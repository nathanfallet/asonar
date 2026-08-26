# asonar — Roadmap

## Vision (l'étoile nord)

asonar n'est pas une collection de features : c'est un **moteur d'analyse ASO piloté par agent**.

La boucle cible :

1. Un agent IA (via **MCP**) balance plein de mots-clés candidats (il est bon pour trouver des termes liés).
2. asonar les **fetch + analyse en background** au rafraîchissement : popularité, qui rank, qui met le terme
   dans son **titre / sous-titre**, vélocité d'avis (30j).
3. asonar **score la pertinence automatiquement** (ce qu'un expert ASO fait à la main).
4. Via MCP, l'agent récupère **« ces mots-clés valent le coup, ceux-là non »** + **sur quels mots-clés notre
   app rank / ne rank pas**.
5. L'agent écrit **titre / sous-titre / descriptions** avec les bons mots-clés.

## Chantiers (dans l'ordre)

### 1. Sous-titre (App Store) + short description (Play Store) — FONDATION

Débloque toute l'analyse « qui met le terme dans son titre vs son sous-titre ».

- **Statut : App Store = FAIT de bout en bout** (fetch → persiste → affiche), testé live.
    - `AppStoreSubtitleSource` (Ktor `HttpClient`, GET page produit, **zéro browser**, sous-titre **localisé
      par pays** via `/{pays}/app/id{adamId}`, extraction de l'unique `<p class="subtitle …">`, **retry 3×** +
      **`logger.warn` sur échec** pour ne jamais avaler une erreur). Interface `AppSubtitleSource` + DI.
    - **Branché dans le fetch** (`FetchKeywordUseCaseImpl`) avec **fallback intelligent** :
      `app.subtitle ?: subtitleSource.getSubtitle(...)` → si la recherche porte déjà le sous-titre (cas futur
      Play), pas de 2e appel ; sinon (cas iTunes) on scrape la page.
    - **Persisté** : colonne `subtitle` sur `TopAppSnapshots` (⚠️ `ALTER TABLE` manuel sur la base existante,
      `SchemaUtils.create` n'ALTER pas — migration propre toujours à faire). **Exposé** API/MCP
      (`TopAppSnapshotResponse.subtitle`) + **web** (sous le nom de l'app sur le détail mot-clé). UTF-8 OK.
    - Vérifié : top-10 « pizza »/FR → 10/10 sous-titres récupérés + affichés.
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
- **Reste à faire :** (a) **source Play Store** (short description sur la page listing, même principe, portée
  par le search si Play le donne → le fallback la prend en charge sans code en plus) ; (b) une **vraie
  stratégie de migration DB** (aujourd'hui `SchemaUtils.create` + `ALTER` manuel — ça ne scale pas).

### 2. Couverture de ranking par app — ✅ FAIT (vertical + graphe)

- `GetAppKeywordCoverageUseCase` : pour une app, tous les mots-clés suivis sur son store avec le **rang
  courant** (null = pas ranké), la popularité, et **l'historique de rang**. Trié rangés d'abord.
- **API** `GET /api/app-coverage?appId=` + **tool MCP `get_app_coverage`** (le cœur agent-first).
- **Onglet « Apps »** (web) : `/apps` (sélecteur) + `/apps/{id}` = tableau ranké/pas-ranké (pills) +
  **sparkline SVG** du rang par mot-clé (meilleur rang = en haut). ⇒ **le chantier #5 est absorbé ici**
  (moins les suggestions, qui dépendent de #3).
- Vérifié live sur une app suivie (rangs relevés + termes non-rangés) ; graphe testé avec un historique
  injecté puis nettoyé.

### 3. Moteur de scoring — LE CERVEAU — ✅ FAIT (Option B)

- Codifie l'analyse ASO manuelle : croise **usage-titre/sous-titre du top-10** × **vélocité d'avis 30j**,
  pondéré par **NOTRE vélocité vs la leur** (`velocityAdvantage > 1` = on peut les dépasser). **Pertinence
  volontairement hors scope** (gérée par le choix des mots-clés traqués).
- Sortie par mot-clé : **verdict** (Yes / Yes but / No / Réserve / Unknown) + **score 0-100** + breakdown + commentaire.
- **Option B — perf** : les signaux chers (usage-titre, vélocité médiane top-10, nb résultats) sont **précalculés
  au fetch** (table `KeywordSignalSnapshots`) → le scoring à la lecture est cheap **et** on peut re-tuner les
  poids/seuils **sans re-fetch** (`OpportunityScorer` = fonction pure, unit-testée : 7 tests).
- ⏭️ Scale futur : cacher notre vélocité par marché dans l'agrégat + éventuellement matérialiser le score final
  pour trier 10k en SQL.

### 4. Tools MCP de reco — ✅ FAIT

- **`get_keyword_opportunities(appId)`** (MCP) + **`GET /api/keyword-opportunities?appId=`** + carte web
  **« Recommandations »** sur la page Apps (verdict + score + pourquoi, triés). 15 tools MCP au total.
- **Suggérer de NOUVEAUX mots-clés = le job de l'agent** (via MCP, il connaît l'app) — notre moteur dit juste,
  parmi les traqués, lesquels valent le coup.

### 5. Onglet « Apps » (web) — ✅ FAIT (dans #2 + #4)

- Sélecteur d'app + couverture + **graphe multi-lignes** + carte **Recommandations** (verdicts/scores triés).

## Les 5 chantiers sont livrés — reste (backlog)

- **📄 Vraie pagination des mots-clés** (⚠️ à faire proprement) : aujourd'hui la liste de mots-clés est chargée
  **sans limite** (`Pagination(limit = 0)` sur la page web, l'API, le MCP `list_keywords`, opportunités,
  couverture) — un pansement le temps d'avoir des milliers de mots-clés sur une instance locale. À remplacer par
  une vraie pagination, mais **pensée avec le tableau triable/filtrable** : un tri/filtre client-side exige
  toutes les lignes ; une pagination server-side naïve renverrait « la moitié des données » et casserait le tri.
  Donc soit tri+filtre+pagination **tous server-side et cohérents**, soit on garde le chargement complet tant que
  ça tient. Ne pas bâcler (une pagination mal faite = pire que pas de pagination).
- **⚡ Audit perfs SQL / index** (prioritaire, la page app commence à ramer) : passer toutes les requêtes en
  `EXPLAIN`, vérifier les index (les colonnes de filtre/jointure : `keywordId`, `appId`, `capturedAt`, les
  uniques `term+store+country` / `store+storeAppId`), traquer les **N+1** sur `/apps/{id}` (couverture +
  opportunités lisent les snapshots **par mot-clé** → coût linéaire en nb de mots-clés). Symptôme : lenteur qui
  croît avec le nombre de mots-clés suivis. Matérialiser/agréger si besoin.
- **Play Store** (multi-store) : sources Play (search/ranking + popularité + short description). Archi prête.
- **Vraie migration DB** (aujourd'hui `SchemaUtils.create` + `ALTER`/drop manuels).
- **Scale du scoring** : cacher notre vélocité par marché dans l'agrégat + matérialiser le score pour trier 10k en SQL.
- **Description via lookup** (API officielle, gratuite) pour nourrir l'analyse.

## Livré récemment (au-delà des 5)

- **Le cerveau — modèle « force de mur »** : `wallStrength` = pondération **position × usage-titre × notes** du
  top-of-results (un #1 qui n'utilise pas le terme, ou l'utilise avec peu d'avis, = place faible → passable),
    + **notre vélocité vs la leur** (`velocityAdvantage`). Option B (signaux précalculés au fetch → re-tune des
      poids **sans re-fetch**). Calibration figée par tests unitaires.
- **Rafraîchissement — gating par âge** (`FetchKeywordUseCase`) : ranking refetché si `> 1h`, popularité si
  `> 7j`, **gates INDÉPENDANTS** (sur leur propre date, ordre **ranking → popularité**), seuils = constantes.
- **Auto-refresh** (`RefreshAppKeywordsUseCase`) : ouvrir la page app (web `/apps/{id}` fire-and-forget) ou le
  MCP `get_app_coverage` enfile en background les mots-clés **rankés ∪ opportunités (YES/YES_BUT)** — pas No/Réserve,
  pas « la terre entière » ; le gating les skippe s'ils sont frais.
- **Composants front réutilisables** : `chart.js` (graphe multi-lignes hover/légende) + `table.js` (tri au clic +
  filtres par colonne + recherche : `js-table` / `<th class="filter">` / `data-sort`).
- **Sous-titre App Store** (fetch page produit sans browser, retry + log, persisté + affiché + API/MCP).
- Session-restore Chrome, ajout mot-clé → refresh auto, largeur front, colonne « Avis / 30j ».

## Notes de calibration (scoring)

- La popularité `5` est le **plancher** de l'index (terme quasi jamais recherché) → un mot-clé à 5 n'est pas une
  opportunité quelle que soit sa winnability : le garde-fou `pop ≤ 5 → RESERVE` est volontaire. Cibles utiles :
  ~**10-15** pour démarrer, ~**20-30** ensuite. Privilégier les termes **1-2 mots** (les longs tombent au plancher).
- **Même terme, opportunité différente selon le storefront** — un mot-clé peut être un mur sur un marché et une
  brèche sur un autre. Et un terme **en anglais** peut avoir du volume dans un store non-anglophone là où sa
  traduction locale est au plancher (les stores indexent les traductions comme des mots-clés distincts) → tester
  plusieurs pays et les deux langues.
