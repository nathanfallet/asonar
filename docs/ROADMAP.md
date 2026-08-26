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
- Vérifié live : NutriMaxing ranké #50 « quoi manger », #141 « what to eat », pas ranké pizza/cooking
  fever/tacos ; graphe testé avec un historique injecté puis nettoyé.

### 3. Moteur de scoring — LE CERVEAU — ✅ FAIT (Option B)

- Codifie le raccourci `app-aso` : croise **usage-titre/sous-titre du top-10** × **vélocité d'avis 30j**,
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
  parmi les traqués, lesquels valent le coup. Vérifié live : cooking fever → Yes (45), pizza → No (mur), pop-5 →
  Réserve.

### 5. Onglet « Apps » (web) — ✅ en grande partie fait (dans #2)

- Sélecteur d'app + tableau de couverture + **graphe d'historique de rang** : livrés avec #2.
- Reste : afficher les **suggestions** de mots-clés (= sortie du scoring #3) sur cette page.

## Livré récemment

- Session-restore Chrome (`--restore-last-session`) + nettoyage des onglets parasites (`BrowserHolder`).
- Ajout de mot-clé → **refresh auto** (side-effect via le use case).
- **Largeur** du front (conteneur 1400px, grille `minmax(0,…)` — plus de colonnes coupées).
- Colonne **« Avis / 30j »** (vélocité de reviews) sur le détail mot-clé + `ratingsPer30d` dans l'API/MCP,
  avec garde-fou (affiche `—` tant que les snapshots couvrent moins d'1 jour).
