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
- **Découverte — sources restantes** (la source ASA est livrée, voir plus bas) :
    - les **titres / sous-titres / descriptions des concurrents** (les termes qu'ils indexent) — on a déjà en
      base le titre **et** le sous-titre localisé du top-10 de chaque mot-clé suivi (`TopAppSnapshots`), il
      manque la description (lookup iTunes = API officielle) et l'extraction (n-grammes 1-2 mots, stopwords
      FR/EN, dédup vs déjà traqué/déjà candidat) ;
    - les **avis** — les nôtres **et** ceux des concurrents (feed RSS `customerreviews`, officiel) → les mots
      qui reviennent souvent = des idées qu'on n'aurait pas indexées. Question ouverte : le filtrage du bruit.
    - Les deux se branchent en implémentant `KeywordSuggestionSource` (+ `CandidateSource`) : le reste du
      pipeline (candidats, statuts, revue web/MCP) est déjà là et les prendra sans code en plus.
      ⚠️ Elles ne connaissent **pas** le volume (contrairement à ASA) → leurs candidats sortent avec
      `popularity = null` et il faut les fetcher pour savoir s'ils floorent.
- **Play Store** (multi-store) : sources Play (search/ranking + popularité + short description + suggestions
  de mots-clés). L'archi domaine est prête — les interfaces `AppSearchSource`, `KeywordPopularitySource`,
  `AppSubtitleSource`, `KeywordSuggestionSource` et `OpportunityScorer` portent toutes un `store`, et les use
  cases sélectionnent par `filter`/`firstOrNull { it.store == ... }`. Rien n'est couplé à Apple : une source
  Play = une classe (+ une valeur de `CandidateSource` pour les suggestions), zéro changement ailleurs.
    - ⚠️ **PIÈGE DI À CORRIGER LE JOUR OÙ ON AJOUTE LA 2e IMPLÉMENTATION** — c'est *exactement* ce chantier qui
      le déclenche. Ces 5 bindings sont consommés par `getAll()` mais déclarés `single<Interface> { … }` :
      `AppSearchSource` / `AppSubtitleSource` / `KeywordPopularitySource` / `KeywordSuggestionSource`
      (`InfrastructureModule`) et `OpportunityScorer` (`DomainModule`). Deux `single<Interface>` ont la **même
      clé** chez Koin : **la seconde écrase la première, sans exception ni log**, et `getAll()` n'en rend
      qu'une. Ajouter `PlayAppSearchSource` ferait donc **disparaître** le scraper iTunes → le
      `firstOrNull { it.store == APP_STORE }` renvoie null et le fetch cesse silencieusement.
    - **Correctif** (une ligne par binding) : déclarer le type **concret** et binder l'interface —
      `single { ItunesAppSearchSource(get()) } bind AppSearchSource::class`. Les clés diffèrent alors, et
      `getAll()` rend bien les deux. Vérifié en test :
      `single<Interface>` × 2 → `[PLAY_STORE]` (une seule) ; `concret + bind` × 2 → `[PLAY_STORE, APP_STORE]`.
    - **Latent aujourd'hui** : vérifié, aucun type n'est déclaré deux fois dans les 3 modules DI, donc rien
      n'est cassé tant qu'il n'y a qu'une implémentation par interface. À corriger **avec** la première source
      Play, pas avant — et ajouter un test qui verrouille « deux sources déclarées, deux sources vues ».
- **Vraie migration DB** (aujourd'hui `SchemaUtils.create` + `ALTER`/drop manuels).
- **Scale du scoring** : cacher notre vélocité par marché dans l'agrégat + matérialiser le score pour trier 10k en SQL.
- **Description via lookup** (API officielle, gratuite) pour nourrir l'analyse.

## Livré récemment (au-delà des 5)

- **Concurrents — rôle sur l'app** : `App.role` = `OWNED` (une app à nous, qu'on optimise) / `COMPETITOR` (une
  app qu'on surveille). Le tracking était **déjà gratuit** — un fetch relève le rang de *toutes* les apps
  enregistrées sur le store et snapshote les avis des ~200 apps vues — donc le rôle ne change pas la collecte :
  il dit ce qu'on en fait (UI, et surtout « extrais des mots-clés des concurrents »). Bout en bout :
  API/MCP (`register_app(role)`, idempotent **sauf** le rôle, qu'un ré-enregistrement corrige) + pills web.
  ⚠️ **`ALTER TABLE Apps ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'OWNED';`** sur une base existante.
- **Découverte de mots-clés — source Apple Search Ads** (le chantier « seule voie pour élargir l'univers à
  volume ») :
    - `AsaKeywordSuggestionSource` rejoue l'opération interne **`getRecommendedKeywordsGql`**
      (`recommendationV2.getRecommendedKeywords`) dans la **même session kdriver authentifiée** que la
      popularité — même pattern d'asset JS, mêmes cookies, zéro auth en plus.
    - **Chaque suggestion arrive avec sa popularité** → un candidat qui floore se jette **avant** de coûter un
      fetch. C'est toute la différence avec les termes qu'on invente (mesuré : 208 combos devinés → 100 % au
      plancher).
    - ⚠️ **Finding : le seed est obligatoire.** `text` est nullable dans le schéma mais inutilisable à null —
      Apple renvoie alors le **top charts du store** (`instagram`, `snapchat`, `tiktok`…), rien à voir avec
      l'app. Une passe sans seed ne renvoie donc rien plutôt que de polluer les candidats.
    - Vérifié live : seed `nutrition`/FR → **59 suggestions**, dont `nutrition tracker` (28), `food tracker`
      (26), `healthy eating` (15), `nutrition app` (13)… et `ernährung` (15) / `essen tracken` (24) **dans le
      storefront FR** — la thèse « terme étranger dans un store non-natif » confirmée par Apple elle-même.
- **Candidats de mots-clés — le pipeline de revue** : table `KeywordCandidates` (mutable, pas de l'historique),
  identité **(app, terme, pays)**, `sources` en **set** (deux sources d'accord = signal de pertinence),
  popularité quand la source la connaît, statut `NEW`/`ADDED`/`DISMISSED`.
    - **L'invariant qui justifie de persister : écarter tient.** Une re-découverte **merge** dans la ligne et ne
      touche **jamais** au statut → un terme écarté ne remonte plus jamais. Vérifié : re-run → 0 créé, 55
      mergés, 3 écartés toujours écartés.
    - Les candidats sont **scopés à l'app** : le même terme peut être une piste pour l'une et du bruit pour
      l'autre, écarter ici ne cache rien là-bas. Un terme **déjà suivi** dans le marché n'est jamais proposé.
    - **Seeds** : par défaut les mots-clés déjà suivis du marché, **les mieux mesurés d'abord** (le voisinage
      d'un terme à volume est là où est le volume), plafonnés — une requête par seed et par marché.
    - Surfaces : `POST/GET /api/keyword-candidates`, `POST /api/keyword-candidates/review`, **3 tools MCP**
      (`discover_keywords`, `list_keyword_candidates`, `review_keyword_candidates` → 18 au total), et la page
      web **`/apps/{id}/candidates`** : cases à cocher → **Ajouter la sélection** / **Écarter la sélection**,
      tri/filtres, plancher (pop ≤ 5) signalé. La passe de découverte tourne en **arrière-plan**
      (fire-and-forget) — elle pilote un vrai navigateur, bien trop long pour un POST de formulaire.

- **Perfs DB** (audit `performance_schema` + `EXPLAIN`, index tous vérifiés OK) : (1) writes d'un fetch en
  **batch/1 transaction** (`createAll`, ~215 commits/fetch → ~5) ; (2) **N+1 lecture tué** — opportunités/
  couverture/liste chargent les « derniers » snapshots en **batch via JOIN `MAX(captured_at) GROUP BY`**
  (scan d'index couvrant/loose + `ref` 1 ligne/mot-clé, tient à l'échelle), `ScoreKeyword` devenu pur ;
  (3) **gating d'enregistrement des notes** (comme le gating du fetch) : une note d'app n'est ré-écrite que si
  le compte a bougé (& ≥ 1h) ou après ≥ 24h → ≥ 1 point/jour sans gonfler la plus grosse table.
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
