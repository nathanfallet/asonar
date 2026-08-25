# asonar — Roadmap

## Vision (l'étoile nord)

asonar n'est pas une collection de features : c'est un **moteur d'analyse ASO piloté par agent**.

La boucle cible :

1. Un agent IA (via **MCP**) balance plein de mots-clés candidats (il est bon pour trouver des termes liés).
2. asonar les **fetch + analyse en background** au rafraîchissement : popularité, qui rank, qui met le terme
   dans son **titre / sous-titre**, vélocité d'avis (30j).
3. asonar **score la pertinence automatiquement** (ce qu'un expert ASO fait à la main), selon les règles du
   skill `app-aso`.
4. Via MCP, l'agent récupère **« ces mots-clés valent le coup, ceux-là non »** + **sur quels mots-clés notre
   app rank / ne rank pas**.
5. L'agent écrit **titre / sous-titre / descriptions** avec les bons mots-clés.

## Chantiers (dans l'ordre)

### 1. Sous-titre (App Store) + short description (Play Store) — FONDATION

Débloque toute l'analyse « qui met le terme dans son titre vs son sous-titre ».

- **Statut :** champ `SearchResultApp.subtitle: String?` ajouté, **`null` pour l'instant**.
- **Source — décision à trancher :**
    - ✅ **Ce que le lookup officiel donne** (`itunes.apple.com/lookup?id={adamId}&country={pays}`, vérifié
      en listant tous les champs) : **titre** (`trackName`) + **description longue** (`description`) + genres,
      releaseNotes, version, ratings… → officiel, zéro scraping. **La description peut se wire dès ce
      chantier.**
    - ❌ **Le SOUS-TITRE n'est dans AUCUNE API** — ni le lookup ni le search (pas de champ `subtitle`/
      `tagline`/`promotionalText`) ; l'App Store Connect API ne couvre que *nos* apps. Or c'est justement le
      champ le plus optimisé mots-clés sur l'App Store.
    - ✅ Le sous-titre est sur la page produit `apps.apple.com/{pays}/app/id{adamId}`, récupérable par
      **simple GET HTTPS** — **pas besoin du browser kdriver** (vérifié : `id911121200` → « Le simulateur de
      Pizzeria »). Play : short description sur la page listing, pareil.
    - ⚠️ Mais ça reste du **scraping** de page produit → **à discuter avant d'implémenter** (règle : API
      officielle on fonce, scraping on en parle d'abord). En attendant `subtitle = null` ; la description via
      lookup, elle, est « API officielle » → OK pour foncer quand on fait ce chantier.
- **Reste à faire une fois validé :** brancher la source + **persister** (colonne sur `TopAppSnapshot` →
  ⚠️ migration : `SchemaUtils.create` n'ALTER pas une table existante).

### 2. Couverture de ranking par app — ROI immédiat (data déjà là)

- Sur tous les mots-clés suivis : **ranké / pas ranké** + historique de rang, par app.
- On stocke déjà les rank-snapshots par (mot-clé, app) → surtout une requête + un tool MCP + l'onglet Apps.

### 3. Moteur de scoring de pertinence — LE CERVEAU

- Tourne **en background au fetch**. Applique les règles `app-aso` (Search Term Value, long-tail gagnable,
  popularité > seuil, compétition, usage du terme en titre/sous-titre…).
- Sortie : **verdict / score de pertinence par mot-clé**.

### 4. Tools MCP de reco

- Expose 2 + 3 pour que l'agent pilote la boucle : **bulk add** de mots-clés → récupère les recommandations
  (« vise ceux-là, laisse tomber ceux-là »).

### 5. Onglet « Apps » (web)

- Sélecteur d'app → mots-clés où on rank, **graphe d'historique** (façon AppFigures), suggestions.
- Secondaire au MCP mais utile pour nous (humains).

## Livré récemment

- Session-restore Chrome (`--restore-last-session`) + nettoyage des onglets parasites (`BrowserHolder`).
- Ajout de mot-clé → **refresh auto** (side-effect via le use case).
- **Largeur** du front (conteneur 1400px, grille `minmax(0,…)` — plus de colonnes coupées).
- Colonne **« Avis / 30j »** (vélocité de reviews) sur le détail mot-clé + `ratingsPer30d` dans l'API/MCP,
  avec garde-fou (affiche `—` tant que les snapshots couvrent moins d'1 jour).
