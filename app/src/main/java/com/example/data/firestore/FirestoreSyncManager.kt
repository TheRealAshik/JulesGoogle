package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class FirestorePrompt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val prompt: String = "",
    val category: String = "Coding",
    @ServerTimestamp val createdAt: Timestamp? = null
)

class FirestoreSyncManager(private val context: Context) {

    private val db: FirebaseFirestore by lazy {
        val databaseId = try {
            context.getString(R.string.firestore_database_id)
        } catch (e: Exception) {
            "ai-studio-android-jules-9bcdd3c3-0f00-4ccd-bf0c-4db90f2681a4"
        }
        FirebaseFirestore.getInstance(databaseId)
    }

    fun observeSavedPrompts(userId: String): Flow<List<FirestorePrompt>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = db.collection("users").document(userId).collection("saved_prompts")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("FirestoreSyncManager", "Error listening to saved prompts: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(
                    FirestorePrompt::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun savePrompt(userId: String, title: String, prompt: String, category: String = "General"): Boolean {
        if (userId.isBlank()) return false
        return try {
            val docRef = db.collection("users").document(userId).collection("saved_prompts").document()
            val data = mapOf(
                "userId" to userId,
                "title" to title,
                "prompt" to prompt,
                "category" to category,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed to save prompt", e)
            false
        }
    }

    suspend fun deletePrompt(userId: String, promptId: String): Boolean {
        if (userId.isBlank() || promptId.isBlank()) return false
        return try {
            db.collection("users").document(userId)
                .collection("saved_prompts").document(promptId)
                .delete().await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed to delete prompt", e)
            false
        }
    }

    suspend fun syncUserSettings(userId: String, settingsMap: Map<String, Any>): Boolean {
        if (userId.isBlank()) return false
        return try {
            val docRef = db.collection("users").document(userId).collection("settings").document("preferences")
            docRef.set(settingsMap).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed to sync user settings", e)
            false
        }
    }
}
