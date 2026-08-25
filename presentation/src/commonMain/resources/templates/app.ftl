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
                <p class="muted">Aucun mot-clé suivi sur ce store. Ajoute des mots-clés depuis le dashboard.</p>
            </div>
        </#if>
    </div>
</@l.page>
