<#macro page layout>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${layout.title} · asonar</title>
    <link rel="stylesheet" href="/css/asonar.css">
</head>
<body>
<header class="topbar">
    <a class="brand" href="/">
        <span class="radar" aria-hidden="true"></span>
        <span class="brand-name">asonar</span>
        <span class="brand-tag">ASO radar</span>
    </a>
    <nav class="nav">
        <a href="/"<#if layout.activeNav == "dashboard"> class="active"</#if>>Dashboard</a>
        <a href="/mcp-guide" class="nav-cta<#if layout.activeNav == "mcp"> active</#if>">Connecter à Claude</a>
    </nav>
</header>
<main class="container">
    <#nested>
</main>
<footer class="footer">asonar · instance locale</footer>
</body>
</html>
</#macro>
