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
    <div class="card-head">Outils disponibles <span class="chip">${view.tools?size} · à jour</span></div>
    <table class="kw-table">
        <thead><tr><th>Outil</th><th>Ce qu'il fait</th></tr></thead>
        <tbody>
        <#list view.tools as t>
            <tr>
                <td class="term mono-sm">${t.name}</td>
                <td class="muted">${t.description}</td>
            </tr>
        </#list>
        </tbody>
    </table>
</div>
</@l.page>
