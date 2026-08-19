<#import "layout.ftl" as l>
<@l.page view.layout>
<section class="page-head">
    <h1>Mots-clés suivis</h1>
    <p class="lede">Indice de popularité (0–100) par mot-clé, dernière mesure relevée.</p>
</section>

<div class="card">
    <#if (view.keywords?size > 0)>
    <table class="kw-table">
        <thead>
        <tr>
            <th>Terme</th>
            <th>Store</th>
            <th>Pays</th>
            <th class="col-pop">Popularité</th>
            <th class="col-when">Mesuré</th>
        </tr>
        </thead>
        <tbody>
        <#list view.keywords as k>
        <tr>
            <td class="term">${k.term}</td>
            <td><span class="chip">${k.store}</span></td>
            <td class="country">${k.country}</td>
            <td class="col-pop">
                <#if k.hasPopularity>
                <div class="bar" title="${k.popularityLabel} / 100">
                    <div class="bar-fill" style="width: ${k.popularityValue}%"></div>
                    <span class="bar-val">${k.popularityLabel}</span>
                </div>
                <#else>
                <span class="muted">—</span>
                </#if>
            </td>
            <td class="col-when muted">${k.capturedAt}</td>
        </tr>
        </#list>
        </tbody>
    </table>
    <#else>
    <div class="empty">
        <p class="empty-title">Aucun mot-clé suivi</p>
        <p class="muted">Ils apparaîtront ici dès qu'un run en aura enregistré.</p>
    </div>
    </#if>
</div>
</@l.page>
