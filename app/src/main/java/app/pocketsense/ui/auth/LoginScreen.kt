package app.pocketsense.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.pocketsense.ui.components.auth.AuthTextField
import app.pocketsense.ui.components.auth.PrimaryButton
import app.pocketsense.ui.components.auth.SocialAuthButton
import kotlinx.coroutines.launch

@Composable
internal fun LoginScreen(
    viewModel: AuthViewModel,
    googleReady: Boolean,
    onGoogleClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val palette = LocalAuthPalette.current
    val loginErrors by viewModel.loginFieldErrors.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val info by viewModel.infoMessage.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(info) {
        val msg = info ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeInfoMessage()
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
            viewModel.resetUiToIdleIfNeeded()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = palette.background,
        contentColor = palette.textPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(innerPadding)
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxHeight * 0.40f)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    AuthLogoMark()
                    Spacer(Modifier.height(32.dp))
                    Text("Sign in", style = MaterialTheme.typography.headlineLarge, color = palette.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Welcome back",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
            }

            AuthTextField(
                label = "Email",
                value = email,
                onValueChange = {
                    email = it
                    viewModel.clearLoginEmailError()
                },
                errorText = loginErrors.email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(16.dp))
            AuthTextField(
                label = "Password",
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearLoginPasswordError()
                },
                errorText = loginErrors.password,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordVisibleChange = { passwordVisible = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.submitLogin(email, password) },
                ),
            )

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "Continue",
                loading = uiState is AuthUiState.Loading,
                onClick = { viewModel.submitLogin(email, password) },
            )

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = palette.border, thickness = 1.dp)
                Text("or", style = MaterialTheme.typography.bodySmall, color = palette.textTertiary)
                HorizontalDivider(modifier = Modifier.weight(1f), color = palette.border, thickness = 1.dp)
            }
            Spacer(Modifier.height(16.dp))

            SocialAuthButton(
                label = "Continue with Google",
                onClick = {
                    if (!googleReady) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Add default_web_client_id in strings.xml (Firebase Web client ID), then rebuild.",
                            )
                        }
                    } else {
                        onGoogleClick()
                    }
                },
                enabled = uiState !is AuthUiState.Loading,
            )

            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
                Text(
                    text = "Sign up",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary,
                    modifier = Modifier.clickable { onNavigateToRegister() },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
