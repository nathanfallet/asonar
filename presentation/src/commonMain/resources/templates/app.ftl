<#import "layout.ftl" as l>
<@l.page view.layout>
    <section class="page-head">
        <a class="back" href="/apps">← Toutes les apps</a>
        <h1>${view.appName}</h1>
        <p class="lede">
            <span class="chip">${view.store}</span>
            <span class="country">${view.storeAppId}</span>
            · Ranké sur <strong>${view.rankedCount}</strong>/${view.totalCount} mots-clés suivis
        </p>
    </section>

    <#assign s = view.summary>
    <div class="stat-grid">
        <div class="card stat">
            <div class="stat-title">Rang moyen</div>
            <div class="stat-main">
                <span class="stat-big mono">${s.avgRankLabel}</span>
                <span class="stat-side"><span class="muted">Meilleur</span> <strong
                            class="mono">${s.bestRankLabel}</strong></span>
                <span class="stat-side"><span class="muted">Pire</span> <strong
                            class="mono">${s.worstRankLabel}</strong></span>
            </div>
        </div>

        <div class="card stat">
            <div class="stat-title">Distribution</div>
            <div class="dist">
                <#list [{"n": s.top5, "l": "TOP 5"}, {"n": s.top25, "l": "TOP 25"}, {"n": s.top100, "l": "TOP 100"}, {"n": s.beyond100, "l": ">100"}] as b>
                    <div class="dist-col">
                        <span class="dist-n mono">${b.n}</span>
                        <span class="dist-bar" style="height: ${(4 + b.n / s.distMax * 34)?round}px"></span>
                        <span class="dist-l">${b.l}</span>
                    </div>
                </#list>
            </div>
        </div>

        <div class="card stat">
            <div class="stat-title">Mouvement</div>
            <div class="move">
                <span class="move-up">↑ ${s.wentUp}</span>
                <span class="move-down">↓ ${s.wentDown}</span>
                <span class="move-flat">= ${s.unchanged}</span>
            </div>
            <div class="move-bar">
                <span class="up" style="width: ${(s.wentUp / s.moveTotal * 100)?round}%"></span>
                <span class="down" style="width: ${(s.wentDown / s.moveTotal * 100)?round}%"></span>
                <span class="flat" style="width: ${(s.unchanged / s.moveTotal * 100)?round}%"></span>
            </div>
        </div>
    </div>

    <#if view.hasChart>
        <div class="card chart-card">
            <div class="card-head">Évolution du rang dans le temps</div>
            <div class="rank-chart" data-chart="${view.chartJson}"></div>
        </div>
    </#if>

    <#if (view.recommendations?size > 0)>
        <div class="card recos-card">
            <div class="card-head">Recommandations <span class="chip">quels mots-clés viser</span></div>
            <table class="kw-table">
                <thead>
                <tr>
                    <th>Mot-clé</th>
                    <th>Pays</th>
                    <th>Verdict</th>
                    <th>Score</th>
                    <th>Pourquoi</th>
                </tr>
                </thead>
                <tbody>
                <#list view.recommendations as o>
                    <tr>
                        <td class="term"><a class="row-link" href="/keywords/${o.keywordId}">${o.term}</a></td>
                        <td class="country">${o.country}</td>
                        <td><span class="verdict verdict-${o.verdictClass}">${o.verdictLabel}</span></td>
                        <td class="mono">${o.scoreLabel}</td>
                        <td class="muted opp-why">${o.comment}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>
    </#if>

    <div class="card">
        <div class="card-head">Couverture de ranking</div>
        <#if (view.rows?size > 0)>
            <table class="kw-table">
                <thead>
                <tr>
                    <th>Mot-clé</th>
                    <th>Pays</th>
                    <th>Popularité</th>
                    <th>Rang</th>
                    <th class="col-spark">Historique</th>
                    <th class="col-when">Mesuré</th>
                </tr>
                </thead>
                <tbody>
                <#list view.rows as r>
                    <tr>
                        <td class="term"><a class="row-link" href="/keywords/${r.keywordId}">${r.term}</a></td>
                        <td class="country">${r.country}</td>
                        <td class="mono">${r.popularityLabel}</td>
                        <td>
                            <#if r.ranked>
                                <span class="rank-pill ranked">${r.rankLabel}</span>
                            <#else>
                                <span class="rank-pill">non ranké</span>
                            </#if>
                        </td>
                        <td class="col-spark">
                            <#if r.sparkPoints?has_content>
                                <svg class="spark" viewBox="0 0 100 24" preserveAspectRatio="none"
                                     aria-hidden="true">
                                    <polyline points="${r.sparkPoints}"/>
                                </svg>
                            <#else>
                                <span class="muted">—</span>
                            </#if>
                        </td>
                        <td class="col-when muted">${r.capturedAt}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
        <#else>
            <div class="empty">
                <p class="muted">Aucun mot-clé suivi sur ce store. Ajoute des mots-clés depuis les Mots-clés.</p>
            </div>
        </#if>
    </div>

    <script src="/js/chart.js"></script>
</@l.page>
