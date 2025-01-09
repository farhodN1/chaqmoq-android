package com.example.chaqmoq.ui.targetUser

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chaqmoq.IncomingCallAlert
import com.example.chaqmoq.R
import com.example.chaqmoq.adapter.MessageListAdapter
import com.example.chaqmoq.databinding.TargetUserBinding
import com.example.chaqmoq.repos.SocketRepository
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import org.json.JSONObject
import org.threeten.bp.Instant
import java.nio.ByteBuffer
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs


class TargetUserFragment : Fragment(){
    private var _binding: TargetUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var messageListAdapter: MessageListAdapter
    private var player: MediaPlayer? = null
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var filePath: String
    lateinit var file: String
    var isRecordingStopped: Boolean = true


    private val hostData: SharedPreferences by lazy {
        requireActivity().getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
    }
    private val targetData: SharedPreferences by lazy {
        requireActivity().getSharedPreferences("TargetInfo", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val targetUserViewModel =
            ViewModelProvider(this).get(TargetUserViewModel::class.java)

        val currentUTC = System.currentTimeMillis()

        _binding = TargetUserBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val recyclerView: RecyclerView = binding.recyclerView

        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() == "") {
                    binding.sendButton.visibility = View.GONE
                    binding.voiceMsgBtn.visibility = View.VISIBLE
                } else {
                    binding.sendButton.visibility = View.VISIBLE
                    binding.voiceMsgBtn.visibility = View.GONE
                }
            }
        })
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        messageListAdapter = MessageListAdapter(hostData, requireContext())
        binding.sendButton.setOnClickListener{sendMessage("text")}
        val targetUserName = targetData.getString("username", null)
        val targetUserImage = targetData.getString("pictureURL", null)
        val targetUserLastSeen = targetData.getString("lastSeen", null)
        val targetUserStatus = targetData.getString("status", null)
        binding.username.text = targetUserName
        if (targetUserStatus == "online") {
            binding.status.text = "online"
        } else if (targetUserLastSeen?.toIntOrNull() != null) {
            val timeDifference = currentUTC - targetUserLastSeen.toInt()
            val minutesAgo = timeDifference / (1000 * 60)
            val hoursAgo = timeDifference / (1000 * 60 * 60)
            if (minutesAgo < 60) {
                binding.status.text = "${minutesAgo} ago"
            } else if (hoursAgo < 24) {
                binding.status.text = "${hoursAgo} ago"
            }
        }
        Glide.with(root)
            .load(targetUserImage)
            .placeholder(R.drawable.roundimage_placeholder)
            .apply(RequestOptions.circleCropTransform())
            .into(binding.userImage)
        val targetId = targetData.getString("id", null)
        val hostId = hostData.getString("nickname", null)
        val myString = targetId + hostId
        val conversationId = myString.toList().sorted().joinToString("")
        targetUserViewModel.makeNetworkRequest(conversationId)

        SocketRepository.socket.on("updateMessage") {
            if (_binding !== null) {
                Log.d("update", "message")
                targetUserViewModel.makeNetworkRequest(conversationId)
                val plopUri = Uri.parse("android.resource://${requireContext().packageName}/raw/plop") // this is the line 100
                player = MediaPlayer.create(context, plopUri)
                player?.start()
            }

        }

        recyclerView.adapter = messageListAdapter

        targetUserViewModel.messageList.observe(viewLifecycleOwner) { messageList ->
            Log.d("messageList", messageList.toString())
            messageListAdapter.submitList(messageList)
        }

        binding.backBtn.setOnClickListener {
            view?.findNavController()?.popBackStack()
        }
        binding.videoCallButton.setOnClickListener {
            IncomingCallAlert.isViewAdded = true
            val bundle = Bundle().apply {
                putString("callType", "video")
                putBoolean("incoming", false)
            }
            findNavController().navigate(R.id.nav_call, bundle)
        }
        binding.audioCallButton.setOnClickListener {
            IncomingCallAlert.isViewAdded = true
            Log.d("users", "${hostId}, ${targetId}")
            val bundle = Bundle().apply {
                putString("callType", "audio")
                putBoolean("incoming", false)
            }
            findNavController().navigate(R.id.nav_call, bundle)
        }

        val handler = Handler(Looper.getMainLooper())
        val stopRecorder = Runnable {
            stopRecording()
        }

        binding.voiceMsgBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    handler.removeCallbacks(stopRecorder)
                    if (isRecordingStopped) {
                        startRecording()
                    }
                    binding.voiceMsgBtn.visibility = View.GONE
                    binding.animationCircle.visibility = View.VISIBLE
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.postDelayed(stopRecorder, 500)

                    binding.voiceMsgBtn.visibility = View.VISIBLE
                    binding.animationCircle.visibility = View.GONE
                    true
                }
                else -> false
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun sendMessage(messageType: String? = null, messageInput: String? = null, amps: List<Float>? = null) {
        val targetId = targetData.getString("id", null)
        val hostId = hostData.getString("nickname", null)
        val utcInstant = Instant.now()
        val sender = hostId
        val recipient = targetId
        val message = binding.messageEditText.text.toString()
        val jsonObject = JSONObject()
        jsonObject.put("sender", sender)
        jsonObject.put("recipient", recipient)
        jsonObject.put("sendTime", utcInstant)
        if (messageType == "audio") {
            jsonObject.put("message", messageInput)
            jsonObject.put("message_type", messageType)
            jsonObject.put("amplitudes", Gson().toJson(amps))

        } else {
            jsonObject.put("message", message)
            jsonObject.put("message_type", messageType)
        }
        SocketRepository.socket.emit("private message", jsonObject)
        binding.messageEditText.text = Editable.Factory.getInstance().newEditable("")
    }

    private fun startRecording() {
        isRecordingStopped = false
        file = "voice_message_${System.currentTimeMillis()}.m4a"
        filePath = "${context?.externalCacheDir?.absolutePath}/${file}"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(filePath)
            prepare()
            start()
        }
        Log.d("voice", "Recording started")
//        startAnimation(binding.animationCircle)
    }

    private fun stopRecording() {
        Log.d("it's", "up")
        isRecordingStopped = true
        mediaRecorder.apply {
            stop()
            release()
        }
        val uri = "file:///storage/emulated/0/Android/data/com.example.chaqmoq/cache/${file}"
        val amps = extractWaveformFromAudioFile(filePath)
        Log.d("extracted amps", amps.toString())
        uploadFileToFirestore(Uri.parse(uri), amps)
        Log.d("voice", "Recording saved to $filePath")
    }

    fun uploadFileToFirestore(fileUri: Uri, amps: List<Float>) {
        Log.d("fikeUri", fileUri.toString())
        val storageReference = FirebaseStorage.getInstance().reference

        val fileReference = storageReference.child("VoiceMessages/voice_message_${System.currentTimeMillis()}.m4a")

        fileReference.putFile(fileUri)
            .addOnSuccessListener { taskSnapshot ->
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    Log.d("FirebaseStorage", "File uploaded successfully. URL: $uri")
                    sendMessage("audio", uri.toString(), amps)
                }
            }
            .addOnFailureListener { e ->
                Log.e("error", e.toString())
            }
    }

    fun extractWaveformFromAudioFile(filePath: String): List<Float> {
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)
        val format = extractor.getTrackFormat(0)
        val amplitudes = mutableListOf<Float>()

        // Ensure the selected track is audio
        if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio") == true) {
            extractor.selectTrack(0)

            // Allocate ByteBuffer for reading samples
            val bufferSize = 1024
            val byteBuffer = ByteBuffer.allocate(bufferSize)


            while (true) {
                byteBuffer.clear() // Clear the buffer before reuse
                val read = extractor.readSampleData(byteBuffer, 0)
                if (read < 0) break // End of file

                // Convert ByteBuffer to ByteArray
                val byteArray = ByteArray(read)
                byteBuffer.get(byteArray, 0, read)

                // Calculate the average amplitude from the sample data
                val amplitude = byteArray.map {
                    Log.d("ampByte", it.toString())
                    abs(it.toFloat()) / Byte.MAX_VALUE
                }.average()
                amplitudes.add(amplitude.toFloat())

                extractor.advance()
            }

            extractor.release()
        }
        return amplitudes
    }

    private fun startAnimation(view: View) {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val scale =
                    1.0f + (mediaRecorder.maxAmplitude / 3000.0f) // Scale factor based on amplitude
                Handler(Looper.getMainLooper()).post {
                    view.animate()
                        .scaleX(scale)
                        .scaleY(scale)
                        .setDuration(100)
                        .start()
                }
            }
        }, 0, 100) // Update every 100ms
    }
}
