package com.institution.idscanner.cloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.institution.idscanner.cloud.auth.AuthManager
import com.institution.idscanner.cloud.data.CloudRepository
import com.institution.idscanner.cloud.data.CloudSettings
import com.institution.idscanner.cloud.ui.AppNav
import com.institution.idscanner.cloud.ui.LoginScreen
import com.institution.idscanner.cloud.ui.theme.StudentIDScannerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var auth: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        auth = AuthManager(this)
        configureFirestore()
        setContent {
            StudentIDScannerTheme {
                val user by auth.currentUser.collectAsState()
                if (user == null) {
                    LoginScreen(auth)
                } else {
                    val uid = user!!.uid
                    // First-login setup runs once per user; the repository
                    // guards it so existing users are never re-seeded.
                    val factory = remember(uid) {
                        val repo = CloudRepository(uid)
                        val settings = CloudSettings(repo)
                        object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                val vm = AppViewModel(repo, settings, auth, this@MainActivity)
                                vm.viewModelScope.launch {
                                    runCatching {
                                        repo.ensureProfile(user!!.email.orEmpty())
                                    }
                                }
                                return vm as T
                            }
                        }
                    }
                    val vm: AppViewModel = viewModel(factory = factory)
                    AppNav(vm)
                }
            }
        }
    }

    /**
     * Enables Firestore's offline persistence (cached reads + queued
     * writes) before any Firestore instance is used. Settings can only be
     * applied once per process, so failures are ignored.
     */
    private fun configureFirestore() {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (_: Exception) {
            // Settings were already applied (e.g. after a process restart
            // path that touched Firestore first). Persistence stays on.
        }
    }
}
