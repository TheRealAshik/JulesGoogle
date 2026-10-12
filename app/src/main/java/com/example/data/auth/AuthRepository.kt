package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data object Guest : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth? = FirebaseApp.getApps(context).firstOrNull()?.let { app ->
        FirebaseAuth.getInstance(app)
    }
    private val credentialManager = CredentialManager.create(context)

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { authInstance ->
            trySend(authInstance.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    fun setGuestMode(isGuest: Boolean) {
        _isGuestMode.value = isGuest
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(
            IllegalStateException(
                "Firebase is not configured. Add app/google-services.json for this app to enable Google Sign-In."
            )
        )

        val webClientIdResourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (webClientIdResourceId == 0) {
            return Result.failure(
                IllegalStateException(
                    "Google Sign-In is not configured: provide google-services.json with a web OAuth client ID."
                )
            )
        }

        return try {
            val serverClientId = context.getString(webClientIdResourceId)

            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    _isGuestMode.value = false
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to sign in with Google."))
                }
            } else {
                Result.failure(Exception("Unexpected credential type returned."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("AuthRepository", "Google Sign-In was cancelled by user.", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth?.signOut()
        _isGuestMode.value = false
    }
}
