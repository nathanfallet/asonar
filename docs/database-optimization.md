# Audit perfs base de données

> Audit du 2026-08-27 sur l'instance MySQL par défaut (`asonar`). Méthode : comparaison des index
> déclarés (code) vs réels (`information_schema`), `performance_schema.events_statements_summary_by_digest`
> pour le coût réel des requêtes, `EXPLAIN` sur les requêtes chaudes. À relire/mettre à jour après chaque
> optimisation livrée.

## Étape 1 — Sync schéma / index : ✅ RAS

Les 7 index déclarés dans le code existent **tous** dans la base et sont **bien utilisés** (`EXPLAIN` →
`ref` + index scan, `rows_examined` 1–85 même sur la grosse table) :

| Table | Index (hors PK) | Présent en DB |
|---|---|---|
| `AppRatingSnapshots` | `(store, storeAppId, country, capturedAt)` | ✅ |
| `Keywords` | unique `(term, store, country)` | ✅ |
| `Apps` | unique `(store, storeAppId)` | ✅ |
| `KeywordSignalSnapshots` | `(keywordId, capturedAt)` | ✅ |
| `RankSnapshots` | `(keywordId, appId, capturedAt)` | ✅ |
| `PopularitySnapshots` | `(keywordId, capturedAt)` | ✅ |
| `TopAppSnapshots` | `(keywordId, capturedAt)` | ✅ |

**Aucune désync, rien à ajouter à la main.** Le `SchemaUtils.create` (create-if-not-exists) n'ALTER pas,
mais ici les tables ont été créées avec leurs index → OK. À re-vérifier si on ajoute un index sur une table
existante (là il faudra un `ALTER` manuel, cf. chantier « vraie migration DB »).

## Taille des tables (contexte)

| Table | Lignes | Taille | Croissance |
|---|---|---|---|
| **`AppRatingSnapshots`** | **~336 k** | **54 MB** | **~200 lignes / fetch** ⚠️ |
| `TopAppSnapshots` | ~25 k | 6 MB | 10 / fetch |
| `KeywordSignalSnapshots` | ~3 k | 2.7 MB | 1 / fetch |
| `RankSnapshots` / `Keywords` / `PopularitySnapshots` | 1–3 k | < 0.5 MB | |

## Étape 2 — Le vrai coût (mesuré)

**Constat de fond : ce n'est PAS un problème d'index** (tout est indexé et utilisé). C'est du **volume de
commits** et de **round-trips (N+1)**.

### 🔴 #1 — Une transaction (donc un `fsync`) par insert → ~202 s de `COMMIT`

`COMMIT` = **~591 k appels, avg 342 ms, ~202 s cumulées** — le poste n°1 de loin. Cause : chaque
`repository.create()` ouvre sa propre transaction, et un fetch écrit **~215 lignes** (≈200 ratings + 10
topApps + ranks + popularité + signaux) → **~215 commits par fetch**. Et `innodb_flush_log_at_trx_commit=1`
+ `sync_binlog=1` → **2 `fsync` disque par commit** (d'où les 342 ms).

**Fix** : envelopper **tout le run dans UNE transaction** (`RecordKeywordRunUseCase` + l'écriture des
signaux) → 1 commit/fetch au lieu de ~215, et **`batchInsert`** pour les ~200 ratings → 1 INSERT au lieu de
200. Gain estimé : **~200× moins de commits**. C'est le chantier principal.
_Attention : les repos font chacun leur `suspendTransaction` ; il faut soit ouvrir une transaction englobante
que les repos rejoignent, soit exposer des méthodes batch/`create(list)`._

> ✅ **FAIT** : méthodes `createAll(payloads)` (`batchInsert`) sur les repos rating/topApp/rank ; un run
> écrit via elles → ~215 commits/fetch → ~5, et les ~200 ratings en 1 batch.

### 🔴 #2 — N+1 massif sur les lectures (aggravé par la limite retirée)

Sur ~1340 keywords, chaque page reboucle en requêtes unitaires :

- **`GetKeywordOpportunities`** (le pire) : par mot-clé, `ScoreKeywordOpportunityUseCase` refait **~5
  requêtes**, dont **`appsRepository.get(appId)` (~17 k appels — la MÊME app à chaque tour)** et
  **`keywordsRepository.get(id)` (~25 k appels — alors que le keyword est déjà dans la liste)**.
  → **~6700 requêtes par ouverture de la page opportunités.**
- `getLatestForKeyword` (PopularitySnapshots) ≈ **88 k appels** ; signaux ~17 k ; ranks ~24 k.
- `GetAppKeywordCoverage` : ~2 requêtes × 1340. `ListKeywordOverviews` : 1 × 1340.

**Fix (2 niveaux)** :
- **Quick win** : dans `ScoreKeywordOpportunityUseCase`, **ne plus re-fetch l'app** (la passer en paramètre)
  ni **le keyword** (déjà en main) → **−~42 k requêtes**, quasi gratuit.
- **Vrai fix** : **batch-load** les « derniers » snapshots (popularité / signaux / rangs) pour **tous** les
  keywords en **une requête chacun** (group-by-max / window function) au lieu d'une par keyword → les
  milliers de round-trips deviennent une poignée.

> ✅ **FAIT** : quick win (`ScoreKeywordOpportunityUseCase` reçoit keyword + app, plus de re-fetch) +
> `latestByKeyword()` / `latestByKeywordForApp(appId)` via un **JOIN sur la sous-requête `MAX(captured_at)
> GROUP BY keyword_id`** (EXPLAIN : `Using index for group-by` / scan d'index couvrant + retour `ref` à 1
> ligne par mot-clé → ne lit que le dernier de chaque mot-clé, tient à l'échelle). `ScoreKeyword` devenu pur
> (zéro I/O). Opportunités ~6700 → ~20 requêtes ; couverture ~2680 → ~4 ; liste ~1340 → 2.

### 🟠 #3 — On enregistre les notes des 200 résultats par fetch

`FetchKeyword` fait `appRatings = results.map` sur **tous** les ~200 résultats → 336 k lignes, +200
INSERT/fetch. Or la vélocité ne sert que pour le **top-10** (le mur) + nos apps. Enregistrer la position 150
= pur gaspillage (table ×10, inserts ×10).

**Fix** : n'enregistrer les ratings que pour le **top-N** (≈10–20) + nos apps → table et écritures ÷~10.

### 🟡 #4 — Tuning MySQL (optionnel, complémentaire)

`innodb_flush_log_at_trx_commit=2` (flush 1×/s au lieu d'à chaque commit) diviserait la latence de commit
par ~10 sur une instance locale re-fetchable. Mais **#1 (batching) est meilleur** (réduit le *nombre* de
commits sans sacrifier la durabilité), et c'est une config du conteneur MySQL partagé. À garder en dernier
recours.

## Ordre d'attaque proposé

1. ✅ **#1 batching des writes** (1 transaction + `batchInsert` par fetch) — le plus gros gain, contenu dans le
   fetch pipeline.
2. ✅ **#2 quick win** (ne plus re-fetch app + keyword dans le scoring) — −42 k requêtes.
3. ✅ **#2 vrai fix** (batch-load des derniers snapshots, JOIN group-by) — pages opportunités / couverture
   rapides à l'échelle.
4. ⏳ **#3** (ratings top-N seulement) — stoppe la croissance de la grosse table. **← reste à faire.**

Lié au backlog **« Audit perfs SQL / index »** et **« pagination »** de la [ROADMAP](ROADMAP.md).
