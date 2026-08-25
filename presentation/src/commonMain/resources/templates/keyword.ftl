<#import "layout.ftl" as l>
<@l.page view.layout>
    <section class="page-head">
        <a class="back" href="/keywords">← Tous les mots-clés</a>
        <h1>${view.term}</h1>
        <p class="lede">
            <span class="chip">${view.store}</span>
            <span class="country">${view.country}</span>
            <#if view.hasPopularity>· Popularité
                <strong>${view.popularityLabel}</strong>/100 · mesuré ${view.capturedAt}</#if>
        </p>
        <form class="head-actions" method="post" action="/keywords/${view.id}/refresh">
            <button class="btn" type="submit">↻ Rafraîchir les données</button>
        </form>
    </section>

    <div class="detail-grid">
        <div class="card">
            <div class="card-head">Top des résultats</div>
            <#if (view.topApps?size > 0)>
                <table class="kw-table">
                    <thead>
                    <tr>
                        <th class="col-rank">#</th>
                        <th>App</th>
                        <th>Notes</th>
                        <th>Moy.</th>
                        <th>Avis / 30j</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list view.topApps as t>
                        <tr>
                            <td class="col-rank"><span class="pos">${t.position}</span></td>
                            <td class="term">${t.appName}
                                <#if t.subtitle?has_content>
                                    <div class="muted app-subtitle">${t.subtitle}</div>
                                </#if>
                                <div class="muted mono-sm">${t.storeAppId}</div>
                            </td>
                            <td class="mono">${t.ratings}</td>
                            <td class="mono">${t.averageRating}<#if t.averageRating != "—"> ★</#if></td>
                            <td class="mono">${t.reviews30d}</td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
            <#else>
                <div class="empty"><p class="muted">Pas encore de top-10 relevé.</p></div>
            </#if>
        </div>

        <div class="card">
            <div class="card-head">Rang de nos apps</div>
            <#if (view.ranks?size > 0)>
                <table class="kw-table">
                    <thead>
                    <tr>
                        <th>App</th>
                        <th>Rang</th>
                        <th>Résultats</th>
                        <th class="col-when">Mesuré</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list view.ranks as r>
                        <tr>
                            <td class="term">${r.appName}</td>
                            <td><strong>${r.rankLabel}</strong></td>
                            <td class="muted">${r.totalResults}</td>
                            <td class="col-when muted">${r.capturedAt}</td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
            <#else>
                <div class="empty"><p class="muted">Aucune de nos apps n'a encore de rang relevé sur ce mot-clé.</p>
                </div>
            </#if>
        </div>
    </div>
</@l.page>
