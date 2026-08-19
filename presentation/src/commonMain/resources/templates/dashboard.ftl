<#import "layout.ftl" as l>
<@l.page view.layout>
<section class="page-head">
    <h1>Mots-clés suivis</h1>
    <p class="lede">Indice de popularité (0–100) par mot-clé, dernière mesure relevée.</p>
</section>

<form class="add-form" method="post" action="/keywords">
    <input class="in in-term" type="text" name="term" placeholder="Ajouter un mot-clé (ex. tdah repas)" required>
    <select class="in in-store" name="store">
        <option value="APP_STORE">App Store</option>
        <option value="PLAY_STORE">Play Store</option>
    </select>
    <input class="in in-country" type="text" name="country" value="FR" maxlength="2" aria-label="Pays" required>
    <button class="btn" type="submit">Suivre</button>
</form>

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
            <td class="term"><a class="row-link" href="/keywords/${k.id}">${k.term}</a></td>
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
        <p class="muted">Ajoute-en un ci-dessus. Les données de popularité arriveront quand le fetch tournera.</p>
    </div>
    </#if>
</div>
</@l.page>
