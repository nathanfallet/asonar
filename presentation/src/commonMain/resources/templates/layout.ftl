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
    <div class="brand">
        <span class="radar" aria-hidden="true"></span>
        <span class="brand-name">asonar</span>
        <span class="brand-tag">ASO radar</span>
    </div>
    <nav class="nav">
        <a href="/" class="active">Dashboard</a>
    </nav>
</header>
<main class="container">
    <#nested>
</main>
<footer class="footer">asonar · instance locale</footer>
</body>
</html>
</#macro>
