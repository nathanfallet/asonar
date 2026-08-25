// Reusable multi-line time chart. No external libs.
//
// Usage: a container `<div class="rank-chart" data-invert-y="true">` holding
//   `<script type="application/json" class="chart-data">{ "yInvert": true, "series": [ ... ] }</script>`
// where each series is { "label": "...", "color": "#...", "points": [ { "t": <epochMillis>, "v": <number> } ] }.
// Renders an SVG with one line per series, a hover tooltip (nearest point), and a clickable legend to
// toggle series. `yInvert` puts smaller values on top (ranks: #1 at the top).
(function () {
    "use strict";

    var NS = "http://www.w3.org/2000/svg";
    var M = {top: 16, right: 18, bottom: 28, left: 40};
    var W = 820, H = 340;

    function el(name, attrs) {
        var e = document.createElementNS(NS, name);
        if (attrs) for (var k in attrs) e.setAttribute(k, attrs[k]);
        return e;
    }

    function niceStep(range, target) {
        var raw = range / target;
        var mag = Math.pow(10, Math.floor(Math.log10(raw)));
        var norm = raw / mag;
        var step = norm < 1.5 ? 1 : norm < 3 ? 2 : norm < 7 ? 5 : 10;
        return Math.max(1, step * mag);
    }

    function fmtDate(ms) {
        var d = new Date(ms);
        return d.toLocaleDateString(undefined, {month: "short", day: "numeric"});
    }

    function render(container) {
        var raw = container.getAttribute("data-chart");
        if (!raw) return;
        var data;
        try {
            data = JSON.parse(raw);
        } catch (e) {
            return;
        }
        var series = (data.series || []).filter(function (s) {
            return s.points && s.points.length;
        });
        if (!series.length) return;
        var invert = data.yInvert === true;

        // Bounds across all series.
        var xMin = Infinity, xMax = -Infinity, yMin = Infinity, yMax = -Infinity;
        series.forEach(function (s) {
            s.points.forEach(function (p) {
                if (p.t < xMin) xMin = p.t;
                if (p.t > xMax) xMax = p.t;
                if (p.v < yMin) yMin = p.v;
                if (p.v > yMax) yMax = p.v;
            });
        });
        // Pad the value axis a touch so lines don't hug the edges.
        if (yMin === yMax) {
            yMin -= 1;
            yMax += 1;
        }
        var plotW = W - M.left - M.right, plotH = H - M.top - M.bottom;

        function xScale(t) {
            return xMax === xMin ? M.left + plotW / 2 : M.left + (t - xMin) / (xMax - xMin) * plotW;
        }

        function yScale(v) {
            var f = (v - yMin) / (yMax - yMin);       // 0 at yMin, 1 at yMax
            if (invert) return M.top + f * plotH;      // small value (good rank) → small y → top
            return M.top + (1 - f) * plotH;
        }

        var svg = el("svg", {viewBox: "0 0 " + W + " " + H, class: "chart-svg", preserveAspectRatio: "xMidYMid meet"});

        // Horizontal gridlines + value labels.
        var yStep = niceStep(yMax - yMin, 5);
        var gy = el("g", {class: "chart-grid"});
        for (var v = Math.ceil(yMin / yStep) * yStep; v <= yMax; v += yStep) {
            var y = yScale(v);
            gy.appendChild(el("line", {x1: M.left, y1: y, x2: W - M.right, y2: y}));
            var lbl = el("text", {x: M.left - 8, y: y + 4, class: "chart-axis", "text-anchor": "end"});
            lbl.textContent = String(v);
            gy.appendChild(lbl);
        }
        svg.appendChild(gy);

        // X axis date labels.
        var gx = el("g", {class: "chart-grid"});
        var ticks = Math.min(6, series.reduce(function (m, s) {
            return Math.max(m, s.points.length);
        }, 0));
        for (var i = 0; i < ticks; i++) {
            var t = xMin + (xMax - xMin) * (ticks === 1 ? 0.5 : i / (ticks - 1));
            var tx = xScale(t);
            var tl = el("text", {x: tx, y: H - 8, class: "chart-axis", "text-anchor": "middle"});
            tl.textContent = fmtDate(t);
            gx.appendChild(tl);
        }
        svg.appendChild(gx);

        // One group per series (line + points).
        var hidden = {};
        var pointIndex = []; // {sx, sy, series, p} for hover hit-testing
        var seriesGroups = [];

        function draw() {
            pointIndex.length = 0;
            seriesGroups.forEach(function (g) {
                g.remove();
            });
            seriesGroups.length = 0;
            series.forEach(function (s, si) {
                if (hidden[si]) return;
                var g = el("g", {class: "chart-series"});
                var pts = s.points.slice().sort(function (a, b) {
                    return a.t - b.t;
                });
                if (pts.length > 1) {
                    var d = pts.map(function (p) {
                        return xScale(p.t) + "," + yScale(p.v);
                    }).join(" ");
                    g.appendChild(el("polyline", {points: d, stroke: s.color, fill: "none"}));
                }
                pts.forEach(function (p) {
                    var cx = xScale(p.t), cy = yScale(p.v);
                    g.appendChild(el("circle", {cx: cx, cy: cy, r: 2.5, fill: s.color}));
                    pointIndex.push({x: cx, y: cy, s: s, p: p});
                });
                svg.appendChild(g);
                seriesGroups.push(g);
            });
        }

        draw();

        // Hover: highlight nearest point + tooltip.
        var hover = el("circle", {r: 4.5, class: "chart-hover", fill: "none"});
        hover.style.visibility = "hidden";
        svg.appendChild(hover);
        var overlay = el("rect", {x: M.left, y: M.top, width: plotW, height: plotH, fill: "transparent"});
        svg.appendChild(overlay);

        var tip = document.createElement("div");
        tip.className = "chart-tip";
        tip.style.display = "none";
        container.appendChild(tip);

        function onMove(evt) {
            var rect = svg.getBoundingClientRect();
            var mx = (evt.clientX - rect.left) / rect.width * W;
            var my = (evt.clientY - rect.top) / rect.height * H;
            var best = null, bestD = Infinity;
            pointIndex.forEach(function (pt) {
                var d = (pt.x - mx) * (pt.x - mx) + (pt.y - my) * (pt.y - my);
                if (d < bestD) {
                    bestD = d;
                    best = pt;
                }
            });
            if (!best || bestD > 900) {
                hover.style.visibility = "hidden";
                tip.style.display = "none";
                return;
            }
            hover.setAttribute("cx", best.x);
            hover.setAttribute("cy", best.y);
            hover.setAttribute("fill", best.s.color);
            hover.style.visibility = "visible";
            tip.innerHTML = '<span class="chart-tip-k"><span class="chart-tip-dot" style="background:' + best.s.color +
                '"></span>' + best.s.label + '</span><span class="chart-tip-v">#' + best.p.v + ' · ' + fmtDate(best.p.t) + '</span>';
            tip.style.display = "block";
            tip.style.left = (best.x / W * rect.width) + "px";
            tip.style.top = (best.y / H * rect.height) + "px";
        }

        overlay.addEventListener("mousemove", onMove);
        overlay.addEventListener("mouseleave", function () {
            hover.style.visibility = "hidden";
            tip.style.display = "none";
        });

        // Legend with toggles.
        var legend = document.createElement("div");
        legend.className = "chart-legend";
        series.forEach(function (s, si) {
            var item = document.createElement("button");
            item.type = "button";
            item.className = "chart-leg";
            item.innerHTML = '<span class="chart-leg-dot" style="background:' + s.color + '"></span>' + s.label;
            item.addEventListener("click", function () {
                hidden[si] = !hidden[si];
                item.classList.toggle("off", !!hidden[si]);
                draw();
                svg.appendChild(hover);
                svg.appendChild(overlay);
            });
            legend.appendChild(item);
        });

        container.appendChild(svg);
        container.appendChild(legend);
    }

    function init() {
        var charts = document.querySelectorAll(".rank-chart");
        for (var i = 0; i < charts.length; i++) render(charts[i]);
    }

    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init);
    else init();
})();
