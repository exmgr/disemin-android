package gr.exm.agroxm.ui.splash

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import gr.exm.agroxm.ui.Navigator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SplashActivity : AppCompatActivity(), KoinComponent {

    private val model: SplashViewModel by viewModels()
    private val navigator: Navigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.isLoggedIn().observe(this) { result ->
            result.mapLeft {
                Timber.d("Not logged in.")
                navigator.showLogin(this)
            }.map { username ->
                Timber.d("Already logged in as $username")
                navigator.showHome(this)
            }
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
