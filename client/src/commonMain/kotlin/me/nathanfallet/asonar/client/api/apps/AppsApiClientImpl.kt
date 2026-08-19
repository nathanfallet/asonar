package me.nathanfallet.asonar.client.api.apps

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import me.nathanfallet.asonar.api.requests.apps.RegisterAppRequest
import me.nathanfallet.asonar.api.resources.apps.AppsApi
import me.nathanfallet.asonar.api.responses.apps.AppResponse
import me.nathanfallet.asonar.api.responses.apps.AppsResponse

/** [AppsApiClient] backed by the shared Ktor [HttpClient]. */
class AppsApiClientImpl(
    private val client: HttpClient,
) : AppsApiClient {

    override suspend fun getAll(): AppsResponse = client.get(AppsApi()).body()

    override suspend fun get(id: Long): AppResponse = client.get(AppsApi.Id(id = id)).body()

    override suspend fun register(request: RegisterAppRequest): AppResponse = client
        .post(AppsApi()) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        .body()

    override suspend fun delete(id: Long) {
        client.delete(AppsApi.Id(id = id))
    }

}
