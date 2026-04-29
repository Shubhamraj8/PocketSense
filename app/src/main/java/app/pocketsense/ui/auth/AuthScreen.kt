package app.pocketsense.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.pocketsense.R
import app.pocketsense.data.DarkModePref
import app.pocketsense.data.auth.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

private object AuthDest {
    const val Login = "auth_login"
    const val Register = "auth_register"
}

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    darkMode: DarkModePref,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkMode != DarkModePref.DARK
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = darkMode != DarkModePref.DARK
    }

    AuthAppTheme(darkMode = darkMode) {
        val navController = rememberNavController()
        val vm: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
        val snackbarHostState = remember { SnackbarHostState() }

        val context = LocalContext.current
        val activity = context as Activity
        val webClientId = stringResource(R.string.default_web_client_id).trim()
        val googleReady = webClientId.isNotBlank()
        val scope = rememberCoroutineScope()

        val googleLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            scope.launch {
                if (result.resultCode != Activity.RESULT_OK) {
                    snackbarHostState.showSnackbar("Google sign-in was cancelled.")
                    return@launch
                }
                val data = result.data
                if (data == null) {
                    snackbarHostState.showSnackbar("Google sign-in returned no data.")
                    return@launch
                }
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken
                    if (idToken.isNullOrBlank()) {
                        snackbarHostState.showSnackbar(
                            "No ID token from Google. Verify Web client ID matches Firebase (OAuth Web client) and rebuild.",
                        )
                        return@launch
                    }
                    vm.loginWithGoogleIdToken(idToken)
                } catch (e: ApiException) {
                    val msg = when (e.statusCode) {
                        12501 -> "Google sign-in cancelled (12501). Re-check SHA-1 in Firebase, Web client ID, and Google provider."
                        10 -> "Google Play Services config error (10). Usually SHA-1 / package name mismatch in Firebase."
                        else -> "Google error ${e.statusCode}: ${e.message}"
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }

        fun launchGooglePicker() {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(activity, gso)
            client.signOut().addOnCompleteListener {
                googleLauncher.launch(client.signInIntent)
            }
        }

        NavHost(
            navController = navController,
            startDestination = AuthDest.Login,
            modifier = modifier,
        ) {
            composable(AuthDest.Login) {
                LoginScreen(
                    viewModel = vm,
                    googleReady = googleReady,
                    onGoogleClick = { launchGooglePicker() },
                    onNavigateToRegister = {
                        vm.clearLoginEmailError()
                        vm.clearLoginPasswordError()
                        navController.navigate(AuthDest.Register)
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
            composable(AuthDest.Register) {
                RegisterScreen(
                    viewModel = vm,
                    googleReady = googleReady,
                    onGoogleClick = { launchGooglePicker() },
                    onNavigateToLogin = {
                        vm.clearAllRegisterFieldErrors()
                        navController.popBackStack()
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }
}

@Composable
internal fun AuthLogoMark(modifier: Modifier = Modifier) {
    // Adaptive launcher XML (mipmap/ic_launcher) cannot be used with painterResource — use
    // vector foreground + same background color as the app icon.
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colorResource(R.color.ic_launcher_background)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
