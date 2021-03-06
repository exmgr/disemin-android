package gr.exm.agroxm.data.network.interceptor

import arrow.core.getOrHandle
import gr.exm.agroxm.data.datasource.AuthTokenDataSource
import gr.exm.agroxm.data.network.interceptor.ApiRequestInterceptor.Companion.AUTH_HEADER
import gr.exm.agroxm.data.path
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * {@see okhttp3.Interceptor} that adds Authorization header to the request,
 * using a stored JWT token with Bearer schema.
 */
class ApiRequestInterceptor : Interceptor, KoinComponent {

    companion object {
        const val AUTH_HEADER = "X-Authorization"
    }

    private val authTokenDataSource: AuthTokenDataSource by inject()

    override fun intercept(chain: Interceptor.Chain): Response {
        // Original request
        val request: Request = chain.request()

        // Add auth header using the auth token with Bearer schema
        Timber.d("[${request.path()}] Adding Authorization header.")
        return chain.proceed(request.signedRequest(authTokenDataSource))
    }
}

private fun Request.signedRequest(authTokenRepository: AuthTokenDataSource): Request {
    return runCatching {
        // Get stored auth token
        val authToken = runBlocking {
            authTokenRepository.getAuthToken()
        }.getOrHandle {
            throw it
        }

        // If token is invalid, log, but proceed anyway
        if (!authToken.isAccessTokenValid()) {
            Timber.w("[${this.path()}] Invalid token ${authToken.access}.")
        }

        // Return a new request, adding auth header with access token
        return this.newBuilder()
            .header(AUTH_HEADER, "Bearer ${authToken.access}")
            .build()

    }.getOrElse {
        Timber.w(it, "[${this.path()}] Could not get token. Adding empty Auth header.")

        // Return a new request, adding empty auth header
        this.newBuilder()
            .header(AUTH_HEADER, "")
            .build()
    }
}
