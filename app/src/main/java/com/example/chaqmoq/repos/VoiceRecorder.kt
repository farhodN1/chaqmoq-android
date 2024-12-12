package com.example.chaqmoq.repos

import android.media.MediaRecorder
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: String? = null

    fun startRecording(view: View) {
        outputFile = "${context.externalCacheDir?.absolutePath}/voice_message_${System.currentTimeMillis()}.m4a"
        Log.d("audio", "it's gonna be recorded here ${outputFile}")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile)
            prepare()
            start()
        }

    }

    fun stopRecording(): String? {
        mediaRecorder?.apply {
            stop()
            release()
        }
        Log.d("audio", outputFile.toString())
        return outputFile
    }

    fun uploadAudioWithProgress(filePath: String, onProgress: (Int) -> Unit, onComplete: (String?) -> Unit) {
        val file = Uri.fromFile(File(filePath))
        val storageRef = FirebaseStorage.getInstance().reference
        val audioRef = storageRef.child("audio/${file.lastPathSegment}")

        val uploadTask = audioRef.putFile(file)

        // Track progress
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            onProgress(progress) // Report progress
        }.addOnSuccessListener {
            // Retrieve download URL
            audioRef.downloadUrl.addOnSuccessListener { uri ->
                onComplete(uri.toString()) // Success
            }
        }.addOnFailureListener { exception ->
            exception.printStackTrace()
            onComplete(null) // Handle failure
        }
    }


}
