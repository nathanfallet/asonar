package me.nathanfallet.asonar.client.api.keywords

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import me.nathanfallet.asonar.api.requests.keywords.TrackKeywordRequest
import me.nathanfallet.asonar.api.resources.keywords.KeywordsApi
import me.nathanfallet.asonar.api.responses.keywords.KeywordDetailResponse
import me.nathanfallet.asonar.api.responses.keywords.KeywordResponse
import me.nathanfallet.asonar.api.responses.keywords.KeywordsResponse
import me.nathanfallet.asonar.api.responses.snapshots.PopularitySnapshotsResponse
import me.nathanfallet.asonar.api.responses.snapshots.RankSnapshotsResponse
import me.nathanfallet.asonar.api.responses.snapshots.TopAppSnapshotsResponse

/** [KeywordsApiClient] backed by the shared Ktor [HttpClient]. */
class KeywordsApiClientImpl(
    private val client: HttpClient,
) : KeywordsApiClient {

    override suspend fun getAll(): KeywordsResponse = client.get(KeywordsApi()).body()

    override suspend fun get(id: Long): KeywordDetailResponse =
        client.get(KeywordsApi.Id(id = id)).body()

    override suspend fun track(request: TrackKeywordRequest): KeywordResponse = client
        .post(KeywordsApi()) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        .body()

    override suspend fun delete(id: Long) {
        client.delete(KeywordsApi.Id(id = id))
    }

    override suspend fun refresh(id: Long) {
        client.post(KeywordsApi.Id.Refresh(KeywordsApi.Id(id = id)))
    }

    override suspend fun popularityHistory(id: Long): PopularitySnapshotsResponse =
        client.get(KeywordsApi.Id.Popularity(KeywordsApi.Id(id = id))).body()

    override suspend fun topApps(id: Long): TopAppSnapshotsResponse =
        client.get(KeywordsApi.Id.TopApps(KeywordsApi.Id(id = id))).body()

    override suspend fun ranks(id: Long, appId: Long): RankSnapshotsResponse =
        client.get(KeywordsApi.Id.Ranks(KeywordsApi.Id(id = id), appId)).body()

}
