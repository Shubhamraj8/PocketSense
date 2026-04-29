package app.pocketsense.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pocketsense.data.auth.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: FirebaseUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

data class LoginFieldErrors(
    val email: String? = null,
    val password: String? = null,
)

data class RegisterFieldErrors(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _loginFieldErrors = MutableStateFlow(LoginFieldErrors())
    val loginFieldErrors: StateFlow<LoginFieldErrors> = _loginFieldErrors.asStateFlow()

    private val _registerFieldErrors = MutableStateFlow(RegisterFieldErrors())
    val registerFieldErrors: StateFlow<RegisterFieldErrors> = _registerFieldErrors.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    fun consumeInfoMessage() {
        _infoMessage.value = null
    }

    fun resetUiToIdleIfNeeded() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun clearLoginEmailError() {
        _loginFieldErrors.update { it.copy(email = null) }
    }

    fun clearLoginPasswordError() {
        _loginFieldErrors.update { it.copy(password = null) }
    }

    fun clearRegisterNameError() {
        _registerFieldErrors.update { it.copy(name = null) }
    }

    fun clearRegisterEmailError() {
        _registerFieldErrors.update { it.copy(email = null) }
    }

    fun clearRegisterPasswordError() {
        _registerFieldErrors.update { it.copy(password = null) }
    }

    fun clearRegisterConfirmError() {
        _registerFieldErrors.update { it.copy(confirmPassword = null) }
    }

    fun clearAllRegisterFieldErrors() {
        _registerFieldErrors.value = RegisterFieldErrors()
    }

    fun submitLogin(email: String, password: String) {
        val emailErr = validateEmail(email)
        val passErr = validatePassword(password)
        _loginFieldErrors.value = LoginFieldErrors(emailErr, passErr)
        if (emailErr != null || passErr != null) {
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInWithEmail(email, password)
            if (result.isSuccess) {
                val user = authRepository.currentUser()
                if (user != null) {
                    _uiState.value = AuthUiState.Success(user)
                } else {
                    _uiState.value = AuthUiState.Error("Signed in, but user profile is unavailable.")
                }
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Sign-in failed.",
                )
            }
        }
    }

    fun submitRegister(fullName: String, email: String, password: String, confirmPassword: String) {
        val nameErr = validateFullName(fullName)
        val emailErr = validateEmail(email)
        val passErr = validatePassword(password)
        val confirmErr = validateConfirmPassword(password, confirmPassword)
        _registerFieldErrors.value = RegisterFieldErrors(nameErr, emailErr, passErr, confirmErr)
        if (nameErr != null || emailErr != null || passErr != null || confirmErr != null) {
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val signUp = authRepository.signUpWithEmail(email, password)
            if (signUp.isFailure) {
                _uiState.value = AuthUiState.Error(
                    signUp.exceptionOrNull()?.localizedMessage ?: "Could not create account.",
                )
                return@launch
            }
            val profile = authRepository.updateDisplayName(fullName)
            if (profile.isFailure) {
                _infoMessage.value =
                    "Account created, but we could not save your display name. You can update it later in your Google / Firebase profile settings."
            }
            val user = authRepository.currentUser()
            if (user != null) {
                _uiState.value = AuthUiState.Success(user)
            } else {
                _uiState.value = AuthUiState.Error("Account created, but session is unavailable.")
            }
        }
    }

    fun loginWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInWithGoogleIdToken(idToken)
            if (result.isSuccess) {
                val user = authRepository.currentUser()
                if (user != null) {
                    _uiState.value = AuthUiState.Success(user)
                } else {
                    _uiState.value = AuthUiState.Error("Signed in, but user profile is unavailable.")
                }
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Google sign-in failed.",
                )
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val emailErr = validateEmail(email)
        _loginFieldErrors.update { it.copy(email = emailErr) }
        if (emailErr != null) {
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Idle
                _infoMessage.value = "If an account exists for ${email.trim()}, you will receive a reset email shortly."
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Could not send reset email.",
                )
            }
        }
    }

    private fun validateFullName(name: String): String? {
        val t = name.trim()
        if (t.isEmpty()) return "Enter your full name."
        if (t.length < 2) return "Name looks too short."
        return null
    }

    private fun validateEmail(email: String): String? {
        val t = email.trim()
        if (t.isEmpty()) return "Email is required."
        if (!Patterns.EMAIL_ADDRESS.matcher(t).matches()) return "Enter a valid email address."
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.length < 8) return "Use at least 8 characters."
        if (password.none { it.isUpperCase() }) return "Add at least one uppercase letter."
        if (password.none { it.isDigit() }) return "Add at least one number."
        return null
    }

    private fun validateConfirmPassword(password: String, confirm: String): String? {
        if (confirm.isEmpty()) return "Confirm your password."
        if (confirm != password) return "Passwords do not match."
        return null
    }
}
