<#import "layout.ftl" as l>
<@l.page view.layout>
<section class="page-head">
    <h1>Connecter asonar à Claude</h1>
    <p class="lede">asonar expose un serveur <strong>MCP</strong> : Claude peut alors lire tes mots-clés, leur
        popularité et le classement, et gérer ce qui est suivi — directement depuis une conversation.</p>
</section>

<div class="card mcp-endpoint">
    <div class="card-head">Endpoint MCP <span class="chip">Streamable HTTP · local · sans auth</span></div>
    <div class="code-row"><code class="code">${view.mcpUrl}</code></div>
</div>

<div class="steps">
    <div class="card step">
        <div class="step-num">1</div>
        <div class="step-body">
            <h2>Claude Code (CLI)</h2>
            <p class="muted">Une commande, et le serveur est ajouté :</p>
            <pre class="code block">${view.claudeCodeCommand}</pre>
        </div>
    </div>

    <div class="card step">
        <div class="step-num">2</div>
        <div class="step-body">
            <h2>Claude Desktop</h2>
            <p class="muted">Ajoute cette entrée dans <code>claude_desktop_config.json</code>, puis redémarre :</p>
            <pre class="code block">{
  "mcpServers": {
    "asonar": {
      "type": "http",
      "url": "${view.mcpUrl}"
    }
  }
}</pre>
        </div>
    </div>
</div>

<div class="card">
    <div class="card-head">Outils disponibles</div>
    <table class="kw-table">
        <thead><tr><th>Outil</th><th>Ce qu'il fait</th></tr></thead>
        <tbody>
        <tr><td class="term">list_keywords</td><td class="muted">Lister les mots-clés suivis + leur popularité</td></tr>
        <tr><td class="term">get_keyword</td><td class="muted">Détail d'un mot-clé (popularité, top-10, rangs)</td></tr>
        <tr><td class="term">track_keyword</td><td class="muted">Commencer à suivre un mot-clé</td></tr>
        <tr><td class="term">untrack_keyword</td><td class="muted">Arrêter de suivre un mot-clé</td></tr>
        <tr><td class="term">get_keyword_popularity_history</td><td class="muted">Historique de popularité</td></tr>
        <tr><td class="term">get_keyword_top_apps</td><td class="muted">Dernier top des résultats</td></tr>
        <tr><td class="term">get_keyword_ranks</td><td class="muted">Historique de rang d'une de nos apps</td></tr>
        <tr><td class="term">list_apps</td><td class="muted">Lister nos apps suivies</td></tr>
        <tr><td class="term">get_app</td><td class="muted">Détail d'une app</td></tr>
        <tr><td class="term">register_app</td><td class="muted">Enregistrer une app à suivre</td></tr>
        <tr><td class="term">delete_app</td><td class="muted">Retirer une app</td></tr>
        </tbody>
    </table>
</div>
</@l.page>
