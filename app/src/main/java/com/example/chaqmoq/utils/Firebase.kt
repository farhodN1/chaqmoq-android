package com.example.chaqmoq.utils

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import android.net.Uri
import android.util.Log

object Firebase {


    suspend fun uploadFileToFirestore(fileUri: Uri, pathString: String): String? {
        return try {
            val storageReference = FirebaseStorage.getInstance().reference
            val fileReference = storageReference.child(pathString)

            // Upload the file and wait
            fileReference.putFile(fileUri).await()

            // Get the download URL and wait
            val url = fileReference.downloadUrl.await().toString()
            Log.d("FirebaseStorage", "File uploaded successfully. URL: $url")
            url
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Upload failed: $e")
            ""
        }
    }

    fun downloadFileFromFirestore(uri: Uri) {

    }

}