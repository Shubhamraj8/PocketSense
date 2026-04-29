package app.pocketsense.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun observeAuthUser(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            trySend(fbAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun currentUser(): FirebaseUser? = auth.currentUser

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        suspendCoroutine { cont ->
            auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) cont.resume(Result.success(Unit))
                    else cont.resume(Result.failure(task.exception ?: RuntimeException("Sign-in failed")))
                }
        }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> =
        suspendCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) cont.resume(Result.success(Unit))
                    else cont.resume(Result.failure(task.exception ?: RuntimeException("Sign-up failed")))
                }
        }

    suspend fun updateDisplayName(displayName: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        return suspendCoroutine { cont ->
            val request = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build()
            user.updateProfile(request)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) cont.resume(Result.success(Unit))
                    else cont.resume(Result.failure(task.exception ?: RuntimeException("Profile update failed")))
                }
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        suspendCoroutine { cont ->
            auth.sendPasswordResetEmail(email.trim())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) cont.resume(Result.success(Unit))
                    else cont.resume(Result.failure(task.exception ?: RuntimeException("Password reset failed")))
                }
        }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> =
        suspendCoroutine { cont ->
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) cont.resume(Result.success(Unit))
                    else cont.resume(Result.failure(task.exception ?: RuntimeException("Google sign-in failed")))
                }
        }

    fun signOut() {
        auth.signOut()
    }
}
