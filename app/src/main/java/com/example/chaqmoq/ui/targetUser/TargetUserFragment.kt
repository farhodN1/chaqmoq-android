package com.example.chaqmoq.ui.targetUser

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
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
import com.example.chaqmoq.repos.VoiceRecorder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

import org.json.JSONObject
import org.threeten.bp.Instant
import java.io.File
import java.util.Timer
import java.util.TimerTask

class TargetUserFragment : Fragment(){
    private var _binding: TargetUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var messageListAdapter: MessageListAdapter
    private var player: MediaPlayer? = null
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var filePath: String
    lateinit var file: String


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
                Log.d("beforeTextChanged", "onTextChanged")
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("textWatcher", "onTextChanged")

            }

            override fun afterTextChanged(s: Editable?) {
                Log.d("afterTextChanged", "onTextChanged")
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
        Log.d("check hD", hostData.getString("nickname", "shit")!!)
        messageListAdapter = MessageListAdapter(hostData, requireContext())
        binding.sendButton.setOnClickListener{sendMessage()}
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
        SocketRepository.socket.on("private message") {
            targetUserViewModel.makeNetworkRequest(conversationId)
            if (context !== null) {
                val plopUri = Uri.parse("android.resource://${requireContext().packageName}/raw/plop")
                player = MediaPlayer.create(context, plopUri)
                player?.start()
            }

        }

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

        binding.voiceMsgBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecording()
                    binding.voiceMsgBtn.visibility = View.GONE
                    binding.animationCircle.visibility = View.VISIBLE
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopRecording()
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

    fun sendMessage(messageType: String? = null, messageInput: String? = null) {
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
        if (messageInput != null) {
            jsonObject.put("message", messageInput)
        } else {
            jsonObject.put("message", message)
        }

        if (messageType !== null) {
            jsonObject.put("message_type", messageType)
        }
        SocketRepository.socket.emit("private message", jsonObject)
        binding.messageEditText.text = Editable.Factory.getInstance().newEditable("")
    }

    private fun startRecording() {
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
        startAnimation(binding.animationCircle)
    }

    private fun stopRecording() {
        mediaRecorder.apply {
            stop()
            release()
        }
        val uri = "file:///storage/emulated/0/Android/data/com.example.chaqmoq/cache/${file}"
        Log.d("voice", "Recording saved to $filePath")
        uploadFileToFirestore(Uri.parse(uri))
    }

    fun uploadFileToFirestore(fileUri: Uri) {
        Log.d("fikeUri", fileUri.toString())
        val storageReference = FirebaseStorage.getInstance().reference

        val fileReference = storageReference.child("VoiceMessages/voice_message_${System.currentTimeMillis()}.m4a")

        fileReference.putFile(fileUri)
            .addOnSuccessListener { taskSnapshot ->
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    Log.d("FirebaseStorage", "File uploaded successfully. URL: $uri")
                    sendMessage("audio", uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Log.e("error", e.toString())
            }
    }

    private fun startAnimation(view: View) {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val scale =
                    1.0f + (mediaRecorder!!.maxAmplitude / 3000.0f) // Scale factor based on amplitude
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
