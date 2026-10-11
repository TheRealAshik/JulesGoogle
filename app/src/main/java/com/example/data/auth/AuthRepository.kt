package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
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

    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager = CredentialManager.create(context)

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun setGuestMode(isGuest: Boolean) {
        _isGuestMode.value = isGuest
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        return try {
            val serverClientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                // Fallback to client ID if not generated
                "999615440042-cnss453frc0cd5ckosieaau71plp2fh5.apps.googleusercontent.com"
            }

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
                val authResult = auth.signInWithCredential(authCredential).await()
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
        auth.signOut()
        _isGuestMode.value = false
    }
}
