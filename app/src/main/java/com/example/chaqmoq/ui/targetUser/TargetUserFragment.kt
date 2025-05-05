package com.example.chaqmoq.ui.targetUser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.example.chaqmoq.model.Message
import com.example.chaqmoq.repos.DatabaseRepository.saveMessage
import com.example.chaqmoq.repos.DatabaseRepository.saveMessages
import com.example.chaqmoq.repos.SocketRepository
import com.example.chaqmoq.utils.Firebase.uploadFileToFirestore
import com.example.chaqmoq.utils.GlobalVariables
import com.example.chaqmoq.utils.GlobalVariables.target
import com.example.news.utils.NetworkUtils.isInternetAvailable
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.threeten.bp.Instant
import java.nio.ByteBuffer
import java.util.UUID
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
    val animationHandler = Handler(Looper.getMainLooper())
    var isMediaRecorderRunning = false
    private lateinit var targetUserViewModel: TargetUserViewModel
    private val REQUEST_CODE_OPEN_DOCUMENT = 1001
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            // Handle the file URI
            sendMessage("file", uri)
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TargetUserBinding.inflate(inflater, container, false)
        targetUserViewModel = ViewModelProvider(this).get(TargetUserViewModel::class.java)
        targetUserViewModel.fetchMessages(generateConId(target?.id!!, GlobalVariables.host?.id!!), requireContext())
        val root: View = binding.root
        val recyclerView: RecyclerView = binding.recyclerView

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        messageListAdapter = MessageListAdapter(GlobalVariables.host!!, requireContext())
        binding.sendButton.setOnClickListener{sendMessage("text")}

        binding.username.text = target?.username
        setStatusAndTime(target?.lastSeen, target?.status, binding.status)

        Glide.with(root)
            .load(target?.profilePicture)
            .placeholder(R.drawable.roundimage_placeholder)
            .apply(RequestOptions.circleCropTransform())
            .into(binding.userImage)

        val handler = Handler(Looper.getMainLooper())
        val stopRecorder = Runnable {
            stopRecording()
        }

        SocketRepository.socket.on("updateMessage") {
            if (_binding !== null) {
                Log.d("update", "message")
                updateMessages()
            }
        }

        recyclerView.adapter = messageListAdapter


        targetUserViewModel.messageList.observe(viewLifecycleOwner) { messageList ->
            Log.d("messages", messageList.toString())
            binding.progressBar.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false
            lifecycleScope.launch(Dispatchers.IO) {
                Log.d("messages", messageList.toString())
                saveMessages(messageList)
            }
            messageListAdapter.submitList(messageList)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            targetUserViewModel.fetchMessages(generateConId(target?.id!!, GlobalVariables.host?.id!!), requireContext())
        }
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
            Log.d("users", "${GlobalVariables.host?.id}, ${target?.id}")
            val bundle = Bundle().apply {
                putString("callType", "audio")
                putBoolean("incoming", false)
            }
            findNavController().navigate(R.id.nav_call, bundle)
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
                    binding.iconInCircle.visibility = View.VISIBLE
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.postDelayed(stopRecorder, 500)

                    binding.voiceMsgBtn.visibility = View.VISIBLE
                    binding.animationCircle.visibility = View.GONE
                    binding.iconInCircle.visibility = View.GONE
                    true
                }
                else -> false
            }
        }
        binding.sendFileBtn.setOnClickListener { pickFile() }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun sendMessage(messageType: String, fileUri: Uri? = null) {
        val utcInstant = Instant.now()
        val messageExt = binding.messageEditText.text.toString()
        lifecycleScope.launch(Dispatchers.IO) {
            if (isInternetAvailable(requireContext())) {
                val jsonObject = JSONObject()
                jsonObject.put("sender", GlobalVariables.host?.id)
                jsonObject.put("recipient", target?.id)
                jsonObject.put("sendTime", utcInstant)
                jsonObject.put("id", UUID.randomUUID().toString())
                if (messageType == "audio") {
                    val amps = extractWaveformFromAudioFile(filePath)
                    val uri = "file:///storage/emulated/0/Android/data/com.example.chaqmoq/cache/${file}"
                    val pathString = "VoiceMessages/voice_message_${System.currentTimeMillis()}.m4a"
                    val cloudPath = uploadFileToFirestore(Uri.parse(uri), pathString)
                    Log.d("cloadPath", cloudPath!!)
                    jsonObject.put("message", cloudPath)
                    jsonObject.put("message_type", messageType)
                    jsonObject.put("amplitudes", Gson().toJson(amps))
                } else if (messageType == "text") {
                    jsonObject.put("message", messageExt)
                    jsonObject.put("message_type", messageType)
                } else if (messageType == "file") {
                    val pathString = "Files/file_${System.currentTimeMillis()}.m4a"
                    val cloudPath = uploadFileToFirestore(fileUri!!, pathString)
                    jsonObject.put("message", cloudPath)
                    jsonObject.put("message_type", messageType)
                }
                SocketRepository.socket.emit("private message", jsonObject)
            } else {
                if (messageType == "audio") {
                    val amps = extractWaveformFromAudioFile(filePath)
                    val message = Message(conId = generateConId(GlobalVariables.host?.id!!, target?.id!!), message = filePath, message_type = "audio", amplitudes = amps.toString(), receiver_id = target?.id!!, sender_id = GlobalVariables.host?.id, send_time = utcInstant.toString(), status = "pending")
                    saveMessage(message)
                    updateMessages()
                } else if (messageType == "text") {
                    val message = Message(conId = generateConId(GlobalVariables.host?.id!!, target?.id!!), message = messageExt, message_type = "text", receiver_id = target?.id!!, sender_id = GlobalVariables.host?.id, send_time = utcInstant.toString(), status = "pending")
                    saveMessage(message)
                    updateMessages()
                } else if (messageType == "file") {
                    val message = Message(conId = generateConId(GlobalVariables.host?.id!!, target?.id!!), message = fileUri.toString(), message_type = "audio", receiver_id = target?.id!!, sender_id = GlobalVariables.host?.id, send_time = utcInstant.toString(), status = "pending")
                    saveMessage(message)
                    updateMessages()
                }
            }
        }

        binding.messageEditText.text = Editable.Factory.getInstance().newEditable("")
    }

    private fun startRecording() {
        animationHandler.removeCallbacksAndMessages(null)
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
            isMediaRecorderRunning = true
        }
        startAnimation(binding.animationCircle)
    }

    private fun stopRecording() {
        isRecordingStopped = true
        mediaRecorder.apply {
            stop()
            release()
            isMediaRecorderRunning = false
        }
        sendMessage("audio")
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
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (isMediaRecorderRunning) {
                    val scale = 1.0f + (mediaRecorder.maxAmplitude / 3000.0f)
                    view.animate()
                        .scaleX(scale)
                        .scaleY(scale)
                        .setDuration(100)
                        .start()
                    binding.voiceMsgBtn.bringToFront()

                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.post(runnable)
    }

    fun setStatusAndTime(lastSeen: String?, status: String?, statusView: TextView) {
        val currentUTC = System.currentTimeMillis()
        if (status == "online") {
            statusView.text = "online"
        } else if (lastSeen?.toLong() != null) {
            val timeDifference = currentUTC - lastSeen.toLong()
            val minutesAgo = timeDifference / (1000 * 60)
            val hoursAgo = timeDifference / (1000 * 60 * 60)
            if (minutesAgo < 60) {
                statusView.text = "${minutesAgo} ago"
            } else if (hoursAgo < 24) {
                statusView.text = "${hoursAgo} ago"
            }
        }
    }

    fun generateConId(str1: String, str2: String): String {
        val myString = str1 + str2
        val conversationId = myString.toList().sorted().joinToString("")
        return conversationId
    }

    fun updateMessages() {
        targetUserViewModel.fetchMessages(generateConId(target?.id!!, GlobalVariables.host?.id!!), requireContext())
        val plopUri = Uri.parse("android.resource://${requireContext().packageName}/raw/plop")
        player = MediaPlayer.create(context, plopUri)
        player?.start()
    }

    fun pickFile() {
        filePickerLauncher.launch(arrayOf("*/*"))
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT)
    }
}
