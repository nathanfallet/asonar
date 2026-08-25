<#import "layout.ftl" as l>
<@l.page view.layout>
    <section class="page-head">
        <h1>Apps</h1>
        <p class="lede">Choisis une app pour voir sa couverture de mots-clés : où elle rank, où elle ne rank
            pas encore, et l'évolution.</p>
    </section>

    <div class="card">
        <#if (view.apps?size > 0)>
            <table class="kw-table">
                <thead>
                <tr>
                    <th>App</th>
                    <th>Store</th>
                    <th>ID store</th>
                </tr>
                </thead>
                <tbody>
                <#list view.apps as a>
                    <tr>
                        <td class="term"><a class="row-link" href="/apps/${a.id}">${a.name}</a></td>
                        <td><span class="chip">${a.store}</span></td>
                        <td class="muted mono-sm">${a.storeAppId}</td>
                    </tr>
                </#list>
                </tbody>
            </table>
        <#else>
            <div class="empty">
                <p class="empty-title">Aucune app suivie</p>
                <p class="muted">Enregistre une app (via l'API <span class="code">POST /api/apps</span> ou le MCP)
                    pour suivre son ranking.</p>
            </div>
        </#if>
    </div>
</@l.page>
