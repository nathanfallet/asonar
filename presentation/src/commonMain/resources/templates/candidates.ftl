<#import "layout.ftl" as l>
<@l.page view.layout>
    <section class="page-head">
        <a class="back" href="/apps/${view.appId}">← ${view.appName}</a>
        <h1>Découverte de mots-clés</h1>
        <p class="lede">Les sources proposent, <strong>tu tranches</strong>. Rien n'est suivi tant que tu n'as pas
            coché puis ajouté. Un terme écarté ne reviendra plus, même si la découverte le retrouve.</p>
        <p class="lede lede-stats">
            <span class="chip">${view.newCount} à trancher</span>
            <span class="chip">${view.addedCount} ajoutés</span>
            <span class="chip">${view.dismissedCount} écartés</span>
        </p>
    </section>

    <form class="add-form" method="post" action="/apps/${view.appId}/candidates/discover">
        <input class="in in-term" type="text" name="seeds"
               placeholder="Termes de départ (ex. nutrition, meal planner) — vide = tes mieux mesurés">
        <input class="in in-country in-countries" type="text" name="countries" value="${view.countriesValue}"
               placeholder="FR, US" aria-label="Pays">
        <button class="btn" type="submit">Lancer une découverte</button>
    </form>
    <p class="form-hint">Les sources partent d'un terme pour en trouver des voisins : une découverte sans départ
        ne renvoie que le top charts du store. Apple Search Ads rend chaque suggestion <strong>avec sa
            popularité réelle</strong> — d'où l'intérêt par rapport aux termes qu'on invente.</p>

    <div class="card">
        <#if (view.rows?size > 0)>
            <form method="post" action="/apps/${view.appId}/candidates/review">
                <div class="card-head">
                    Candidats à trancher
                    <div class="card-head-actions">
                        <button class="btn btn-sm" type="submit" name="action" value="accept">Ajouter la sélection
                        </button>
                        <button class="btn btn-sm btn-ghost" type="submit" name="action" value="dismiss">Écarter la
                            sélection
                        </button>
                    </div>
                </div>
                <table class="kw-table js-table">
                    <thead>
                    <tr>
                        <th class="nosort col-check"></th>
                        <th>Terme</th>
                        <th class="filter">Pays</th>
                        <th>Popularité</th>
                        <th class="filter">Source</th>
                        <th class="nosort">D'où ça vient</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list view.rows as c>
                        <tr>
                            <td class="col-check"><input type="checkbox" name="candidate" value="${c.id}"
                                                         aria-label="Sélectionner ${c.term}"></td>
                            <td class="term">${c.term}</td>
                            <td class="country">${c.country}</td>
                            <td data-sort="${c.popularitySort}">
                                <span class="mono<#if c.atFloor> muted</#if>">${c.popularityLabel}</span>
                                <#if c.atFloor><span class="chip">plancher</span></#if>
                            </td>
                            <td><#list c.sources as s><span class="chip">${s}</span></#list></td>
                            <td class="muted opp-why">${c.detail}</td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
            </form>
        <#else>
            <div class="empty">
                <p class="empty-title">Aucun candidat à trancher</p>
                <p class="muted">Lance une découverte ci-dessus, ou demande-la à un agent via le MCP
                    (<span class="code">discover_keywords</span>).</p>
            </div>
        </#if>
    </div>
</@l.page>
