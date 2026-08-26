// Reusable table sort + filter. No external libs.
//
// Opt in per table: <table class="kw-table js-table"> … </table>
//  - Every <th> becomes click-to-sort (asc/desc) unless it has class="nosort".
//  - <th class="filter"> also gets a dropdown of that column's distinct values.
//  - A text search box filters all rows.
//  - Sort value = a cell's data-sort attribute when present (use it for pills, bars, dates,
//    "—"/"non ranké"), else the cell text (numeric when every value parses as a number).
(function () {
    "use strict";

    // Filtering keys off the visible text (categories like "Yes" / "FR"); sorting prefers a hidden
    // data-sort value when present (so pills/bars/dates/"—" order correctly), else the text.
    function textValue(row, i) {
        var td = row.cells[i];
        return td ? td.textContent.trim() : "";
    }

    function sortValue(row, i) {
        var td = row.cells[i];
        if (!td) return "";
        var d = td.getAttribute("data-sort");
        return d !== null ? d : td.textContent.trim();
    }

    function enhance(table) {
        var thead = table.tHead, tbody = table.tBodies[0];
        if (!thead || !tbody) return;
        var headers = Array.prototype.slice.call(thead.rows[0].cells);
        var rows = function () {
            return Array.prototype.slice.call(tbody.rows);
        };

        // --- Toolbar (search + per-column dropdowns) ---
        var bar = document.createElement("div");
        bar.className = "table-tools";
        var search = document.createElement("input");
        search.type = "search";
        search.className = "in table-search";
        search.placeholder = "Filtrer…";
        bar.appendChild(search);

        var selects = []; // {index, el}
        headers.forEach(function (th, i) {
            if (!th.classList.contains("filter")) return;
            var values = {};
            rows().forEach(function (r) {
                values[textValue(r, i)] = true;
            });
            var sel = document.createElement("select");
            sel.className = "in table-filter";
            var all = document.createElement("option");
            all.value = "";
            all.textContent = th.textContent.trim() + " : tous";
            sel.appendChild(all);
            Object.keys(values).sort().forEach(function (v) {
                if (v === "") return;
                var o = document.createElement("option");
                o.value = v;
                o.textContent = v;
                sel.appendChild(o);
            });
            selects.push({index: i, el: sel});
            bar.appendChild(sel);
        });
        table.parentNode.insertBefore(bar, table);

        function applyFilters() {
            var q = search.value.trim().toLowerCase();
            rows().forEach(function (r) {
                var ok = q === "" || r.textContent.toLowerCase().indexOf(q) !== -1;
                if (ok) {
                    for (var k = 0; k < selects.length; k++) {
                        var want = selects[k].el.value;
                        if (want !== "" && textValue(r, selects[k].index) !== want) {
                            ok = false;
                            break;
                        }
                    }
                }
                r.style.display = ok ? "" : "none";
            });
        }

        search.addEventListener("input", applyFilters);
        selects.forEach(function (s) {
            s.el.addEventListener("change", applyFilters);
        });

        // --- Sorting ---
        var sortCol = -1, sortDir = 1;
        headers.forEach(function (th, i) {
            if (th.classList.contains("nosort")) return;
            th.classList.add("th-sort");
            th.addEventListener("click", function () {
                sortDir = sortCol === i ? -sortDir : 1;
                sortCol = i;
                var rs = rows();
                var numeric = rs.every(function (r) {
                    var v = sortValue(r, i);
                    return v === "" || !isNaN(parseFloat(v));
                });
                rs.sort(function (a, b) {
                    var va = sortValue(a, i), vb = sortValue(b, i);
                    if (numeric) return ((parseFloat(va) || 0) - (parseFloat(vb) || 0)) * sortDir;
                    return va.localeCompare(vb, undefined, {numeric: true}) * sortDir;
                });
                rs.forEach(function (r) {
                    tbody.appendChild(r);
                });
                headers.forEach(function (h) {
                    h.classList.remove("sort-asc", "sort-desc");
                });
                th.classList.add(sortDir > 0 ? "sort-asc" : "sort-desc");
            });
        });
    }

    function init() {
        var tables = document.querySelectorAll("table.js-table");
        for (var i = 0; i < tables.length; i++) enhance(tables[i]);
    }

    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init);
    else init();
})();
