// Reusable multi-line time chart. No external libs.
//
// Config: a container `<div class="rank-chart" data-chart='{...}'>` whose JSON is
//   { "yInvert": bool,
//     "series": [{ "label", "color", "points": [{ "t": epochMs, "v": num }], "country"?, "badge"? }],
//     "filters": { "country"?: bool, "period"?: [days], "top"?: [rankThresholds] } }
// `yInvert` puts smaller values on top (ranks: #1 at the top). The legend shows each series' `badge`
// (e.g. its current rank) and toggles the line. Any declared `filters` render as controls in the chart
// card header (a `.chart-controls` slot): a country selector, a Top-N segmented control (rank charts),
// and a period (day-window) segmented control — so the same component serves other charts (e.g. a
// keyword's popularity over time) just by declaring different filters.
(function () {
    "use strict";

    var NS = "http://www.w3.org/2000/svg";
    var M = {top: 16, right: 18, bottom: 28, left: 44};
    var W = 820, H = 340, DAY = 86400000;

    function el(name, attrs) {
        var e = document.createElementNS(NS, name);
        if (attrs) for (var k in attrs) e.setAttribute(k, attrs[k]);
        return e;
    }

    function escapeHtml(str) {
        var d = document.createElement("div");
        d.textContent = str == null ? "" : String(str);
        return d.innerHTML;
    }

    function niceStep(range, target) {
        var raw = range / target;
        var mag = Math.pow(10, Math.floor(Math.log10(raw)));
        var norm = raw / mag;
        var step = norm < 1.5 ? 1 : norm < 3 ? 2 : norm < 7 ? 5 : 10;
        return Math.max(1, step * mag);
    }

    function fmtDate(ms) {
        return new Date(ms).toLocaleDateString(undefined, {month: "short", day: "numeric"});
    }

    function periodLabel(days) {
        return days <= 1 ? "24 h" : days + " j";
    }

    // A segmented control. options = [{label, value}]; calls onPick(value) on click.
    function segmented(options, current, onPick) {
        var wrap = document.createElement("div");
        wrap.className = "chart-seg";
        options.forEach(function (o) {
            var b = document.createElement("button");
            b.type = "button";
            b.textContent = o.label;
            if (o.value === current) b.classList.add("on");
            b.addEventListener("click", function () {
                Array.prototype.forEach.call(wrap.children, function (x) { x.classList.remove("on"); });
                b.classList.add("on");
                onPick(o.value);
            });
            wrap.appendChild(b);
        });
        return wrap;
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
        var all = (data.series || []).filter(function (s) { return s.points && s.points.length; });
        if (!all.length) return;
        var invert = data.yInvert === true;
        var filters = data.filters || {};

        // Each series' current value = its newest point (drives the Top-N filter). Latest overall t sets
        // the right edge that period windows are measured back from.
        var tMaxAll = -Infinity;
        all.forEach(function (s) {
            var latest = s.points[0];
            s.points.forEach(function (p) {
                if (p.t > latest.t) latest = p;
                if (p.t > tMaxAll) tMaxAll = p.t;
            });
            s._cur = latest.v;
        });

        // Filter state.
        var hidden = {};
        var legItems = [];
        var fCountry = "";
        var fTop = 0;
        var fDays = (filters.period && filters.period.length) ? Math.max.apply(null, filters.period) : 0;

        var plotW = W - M.left - M.right, plotH = H - M.top - M.bottom;
        var svg = el("svg", {viewBox: "0 0 " + W + " " + H, class: "chart-svg", preserveAspectRatio: "xMidYMid meet"});
        var pointIndex = [];
        var xMin, xMax, yMin, yMax;

        function inWindow(p) { return fDays === 0 || p.t >= tMaxAll - fDays * DAY; }

        // Does a series pass the country / Top-N filters? (the manual legend on/off is separate)
        function inFilter(i) {
            var s = all[i];
            if (fCountry && s.country !== fCountry) return false;
            if (fTop && !(s._cur != null && s._cur <= fTop)) return false;
            return true;
        }

        function visibleIndexes() {
            var out = [];
            all.forEach(function (s, i) { if (!hidden[i] && inFilter(i)) out.push(i); });
            return out;
        }

        // Keep the legend in sync with the filters: a filtered-out series isn't in the chart, so its
        // legend entry (where you toggle lines on/off) shouldn't be either.
        function syncLegend() {
            legItems.forEach(function (item, i) { item.style.display = inFilter(i) ? "" : "none"; });
        }

        function xScale(t) {
            return xMax === xMin ? M.left + plotW / 2 : M.left + (t - xMin) / (xMax - xMin) * plotW;
        }

        function yScale(v) {
            var vv = Math.max(yMin, Math.min(yMax, v)); // clamp so a Top-N zoom keeps lines in the plot
            var f = (vv - yMin) / (yMax - yMin);
            return invert ? M.top + f * plotH : M.top + (1 - f) * plotH;
        }

        var hover = el("circle", {r: 4.5, class: "chart-hover", fill: "none"});
        hover.style.visibility = "hidden";
        var overlay = el("rect", {x: M.left, y: M.top, width: plotW, height: plotH, fill: "transparent"});
        var tip = document.createElement("div");
        tip.className = "chart-tip";
        tip.style.display = "none";

        function redraw() {
            syncLegend();
            while (svg.firstChild) svg.removeChild(svg.firstChild);
            pointIndex.length = 0;
            var vis = visibleIndexes();

            xMin = Infinity; xMax = -Infinity; yMin = Infinity; yMax = -Infinity;
            vis.forEach(function (i) {
                all[i].points.forEach(function (p) {
                    if (!inWindow(p)) return;
                    if (p.t < xMin) xMin = p.t;
                    if (p.t > xMax) xMax = p.t;
                    if (p.v < yMin) yMin = p.v;
                    if (p.v > yMax) yMax = p.v;
                });
            });
            if (!isFinite(xMin)) {
                svg.appendChild(overlay);
                return; // nothing visible in this filter combination
            }
            if (fTop) { yMin = 1; yMax = fTop; }        // zoom the value axis onto the top-N band
            if (yMin === yMax) { yMin -= 1; yMax += 1; }

            // Horizontal gridlines + value labels — always labelling the top and bottom bounds so the
            // best rank (e.g. #1) is shown, not only the round niceStep ticks.
            var gy = el("g", {class: "chart-grid"});
            var seen = {};
            function yLabel(v) {
                var r = Math.round(v);
                if (seen[r]) return;
                seen[r] = true;
                var y = yScale(v);
                gy.appendChild(el("line", {x1: M.left, y1: y, x2: W - M.right, y2: y}));
                var t = el("text", {x: M.left - 8, y: y + 4, class: "chart-axis", "text-anchor": "end"});
                t.textContent = String(r);
                gy.appendChild(t);
            }
            var yStep = niceStep(yMax - yMin, 5);
            for (var v = Math.ceil(yMin / yStep) * yStep; v <= yMax; v += yStep) yLabel(v);
            yLabel(yMin);
            yLabel(yMax);
            svg.appendChild(gy);

            // X axis date labels.
            var gx = el("g", {class: "chart-grid"});
            var ticks = Math.min(6, vis.reduce(function (m, i) {
                return Math.max(m, all[i].points.filter(inWindow).length);
            }, 0));
            for (var k = 0; k < ticks; k++) {
                var tt = xMin + (xMax - xMin) * (ticks === 1 ? 0.5 : k / (ticks - 1));
                var tl = el("text", {x: xScale(tt), y: H - 8, class: "chart-axis", "text-anchor": "middle"});
                tl.textContent = fmtDate(tt);
                gx.appendChild(tl);
            }
            svg.appendChild(gx);

            // One group per visible series (line + points).
            vis.forEach(function (i) {
                var s = all[i];
                var pts = s.points.filter(inWindow).slice().sort(function (a, b) { return a.t - b.t; });
                var g = el("g", {class: "chart-series"});
                if (pts.length > 1) {
                    var d = pts.map(function (p) { return xScale(p.t) + "," + yScale(p.v); }).join(" ");
                    g.appendChild(el("polyline", {points: d, stroke: s.color, fill: "none"}));
                }
                pts.forEach(function (p) {
                    var cx = xScale(p.t), cy = yScale(p.v);
                    g.appendChild(el("circle", {cx: cx, cy: cy, r: 2.5, fill: s.color}));
                    pointIndex.push({x: cx, y: cy, s: s, p: p});
                });
                svg.appendChild(g);
            });

            svg.appendChild(hover);
            svg.appendChild(overlay);
        }

        overlay.addEventListener("mousemove", function (evt) {
            var rect = svg.getBoundingClientRect();
            var mx = (evt.clientX - rect.left) / rect.width * W;
            var my = (evt.clientY - rect.top) / rect.height * H;
            var best = null, bestD = Infinity;
            pointIndex.forEach(function (pt) {
                var d = (pt.x - mx) * (pt.x - mx) + (pt.y - my) * (pt.y - my);
                if (d < bestD) { bestD = d; best = pt; }
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
            tip.innerHTML = '<span class="chart-tip-k"><span class="chart-tip-dot" style="background:' +
                best.s.color + '"></span>' + escapeHtml(best.s.label) + '</span><span class="chart-tip-v">' +
                (invert ? "#" : "") + best.p.v + ' · ' + fmtDate(best.p.t) + '</span>';
            tip.style.display = "block";
            tip.style.left = (best.x / W * rect.width) + "px";
            tip.style.top = (best.y / H * rect.height) + "px";
        });
        overlay.addEventListener("mouseleave", function () {
            hover.style.visibility = "hidden";
            tip.style.display = "none";
        });

        // Legend (label + badge), each toggles its line.
        var legend = document.createElement("div");
        legend.className = "chart-legend";
        all.forEach(function (s, i) {
            var item = document.createElement("button");
            item.type = "button";
            item.className = "chart-leg";
            item.innerHTML = '<span class="chart-leg-dot" style="background:' + s.color + '"></span>' +
                escapeHtml(s.label) +
                (s.badge ? ' <span class="chart-leg-badge">' + escapeHtml(s.badge) + '</span>' : '');
            item.addEventListener("click", function () {
                hidden[i] = !hidden[i];
                item.classList.toggle("off", !!hidden[i]);
                redraw();
            });
            legItems.push(item);
            legend.appendChild(item);
        });

        // Filter controls → the card header slot (falls back to above the chart).
        var card = container.closest ? container.closest(".chart-card") : null;
        var slot = card ? card.querySelector(".chart-controls") : null;
        var controls = slot || document.createElement("div");
        if (!slot) controls.className = "chart-controls";

        if (filters.country) {
            var countries = [];
            all.forEach(function (s) { if (s.country && countries.indexOf(s.country) < 0) countries.push(s.country); });
            if (countries.length > 1) {
                countries.sort();
                var sel = document.createElement("select");
                sel.className = "chart-select";
                sel.appendChild(new Option("Tous les pays", ""));
                countries.forEach(function (c) { sel.appendChild(new Option(c, c)); });
                sel.addEventListener("change", function () { fCountry = sel.value; redraw(); });
                controls.appendChild(sel);
            }
        }
        if (filters.top && filters.top.length) {
            var topOpts = filters.top.map(function (n) { return {label: "Top " + n, value: n}; });
            topOpts.push({label: "Tous", value: 0});
            controls.appendChild(segmented(topOpts, 0, function (val) { fTop = val; redraw(); }));
        }
        if (filters.period && filters.period.length) {
            var periods = filters.period.slice().sort(function (a, b) { return a - b; });
            var perOpts = periods.map(function (n) { return {label: periodLabel(n), value: n}; });
            controls.appendChild(segmented(perOpts, fDays, function (val) { fDays = val; redraw(); }));
        }

        container.appendChild(svg);
        container.appendChild(tip);
        container.appendChild(legend);
        if (!slot && controls.children.length) container.insertBefore(controls, svg);
        redraw();
    }

    function init() {
        var charts = document.querySelectorAll(".rank-chart");
        for (var i = 0; i < charts.length; i++) render(charts[i]);
    }

    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init);
    else init();
})();
