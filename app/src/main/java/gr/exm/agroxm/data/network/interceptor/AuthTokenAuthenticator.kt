package gr.exm.agroxm.data.network.interceptor

import com.haroldadmin.cnradapter.NetworkResponse
import gr.exm.agroxm.data.datasource.AuthTokenDataSource
import gr.exm.agroxm.data.datasource.CredentialsDataSource
import gr.exm.agroxm.data.network.AuthService
import gr.exm.agroxm.data.network.Credentials
import gr.exm.agroxm.data.network.LoginBody
import gr.exm.agroxm.data.network.RefreshBody
import gr.exm.agroxm.data.path
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AuthTokenAuthenticator : Authenticator, KoinComponent {

    private val authService: AuthService by inject()
    private val authTokenRepository: AuthTokenDataSource by inject()
    private val credentialsRepository: CredentialsDataSource by inject()

    override fun authenticate(route: Route?, response: Response): Request? {
        // The original request
        val request = response.request

        Timber.d("[${request.path()}] Status: ${response.code}. Invoking authenticator.")

        // Try with refresh token
        runBlocking { authTokenRepository.getAuthToken() }.map { authToken ->
            if (authToken.isRefreshTokenValid()) {
                return refreshAndRetry(request, authToken.refresh)
            }
        }

        Timber.d("[${request.path()}] Invalid refresh token. Trying with credentials.")

        // Try with credentials
        runBlocking { credentialsRepository.getCredentials() }.map { credentials ->
            if (credentials.isValid()) {
                retryWithCredentials(request, credentials)
            }
        }

        // Failed to authenticate
        Timber.d("[${request.path()}] Failed to authenticate with all possible ways.")
        return null
    }

    private fun refreshAndRetry(request: Request, refreshToken: String): Request? {
        Timber.d("[${request.path()}] Trying to refresh token")

        val response = runBlocking {
            authService.refresh(RefreshBody(refreshToken))
        }

        when (response) {
            is NetworkResponse.Success -> {
                Timber.d("[${request.path()}] Token refresh success.")

                // Get token from response body
                val newAuthToken = response.body

                // Proceed with original request, adding token
                return retryWithAccessToken(request, newAuthToken.token)
            }
            else -> Timber.d("[${request.path()}] Token refresh failed")
        }

        Timber.w("[${request.path()}] Could not retry with refresh")

        return null
    }

    private fun retryWithCredentials(
        request: Request,
        credentials: Credentials,
    ): Request? {
        Timber.d("[${request.path()}] Trying to login with credentials")

        val response = runBlocking {
            authService.login(LoginBody(credentials.username, credentials.password))
        }

        when (response) {
            is NetworkResponse.Success -> {
                // Get token from response body
                val newAuthToken = response.body

                Timber.d("[${request.path()}] Login success.")

                // Proceed with original request, adding token
                return retryWithAccessToken(request, newAuthToken.token)
            }
            else -> Timber.w("[${request.path()}] Login failed")
        }

        Timber.d("[${request.path()}] Could not login and retry")

        return null
    }

    private fun retryWithAccessToken(request: Request, accessToken: String): Request {
        Timber.d("[${request.path()}] Retrying with access token")
        return request.newBuilder()
            .header("X-Authorization", "Bearer $accessToken")
            .build()
    }
}
