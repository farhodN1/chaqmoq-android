package com.example.chaqmoq.ui.callWindow

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.chaqmoq.IncomingCallAlert
import com.example.chaqmoq.R
import com.example.chaqmoq.databinding.CallWindowBinding
import com.example.chaqmoq.repos.SocketRepository
import com.example.chaqmoq.repos.WebRTCRepository
import org.json.JSONObject
import com.example.chaqmoq.utils.GlobalVariables.callMaker

class CallWindowFragment : Fragment() {

    private var _binding: CallWindowBinding? = null
    private val binding get() = _binding!!
    private var player: MediaPlayer? = null

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
        _binding = CallWindowBinding.inflate(inflater, container, false)
        val root: View = binding.root
        SocketRepository.onSocketConnection(requireContext());
        WebRTCRepository.initializePeerConnectionFactory(requireContext())
        val incoming = arguments?.getBoolean("incoming") ?: false
        val callType = arguments?.getString("callType")
        val hostId = hostData.getString("nickname", null)
        val targetId = targetData.getString("id", null)
        val targetUserName = targetData.getString("givenName", "Unknown")
        if (callType == "video") {
            WebRTCRepository.initializePeerConnection(requireContext(), binding.remoteView, binding.localView, false)
        } else {
            WebRTCRepository.initializePeerConnection(requireContext(), binding.remoteView, binding.localView, true)
        }
        if (callType == "video") {
            if (!incoming) {
                SocketRepository.socket.emit("videoCall", JSONObject().apply {
                    put("sender", hostId)
                    put("recipient", targetId)
                })
            }

            binding.remoteViewLoading.visibility = View.GONE
            binding.ongoingCall.visibility = View.GONE
            binding.callLayout.visibility = View.VISIBLE
            binding.noAnswer.visibility = View.GONE
            binding.userNameInVideo.text = targetUserName

        } else if (callType == "audio") {
            if (!incoming) {
                SocketRepository.socket.emit("audioCall", JSONObject().apply {
                    put("sender", hostId)
                    put("recipient", targetId)
                })
            }
            binding.callLayout.visibility = View.GONE
            binding.ongoingCall.visibility = View.VISIBLE
            binding.noAnswer.visibility = View.GONE
        }
        var seconds = 0
        val handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {
                if (_binding != null) {
                    seconds += 1
                    val minutes = seconds / 60
                    val secs = seconds % 60
                    binding.status.text = String.format("%02d:%02d", minutes, secs)
                    binding.statusInVideo.text = String.format("%02d:%02d", minutes, secs)
                    handler.postDelayed(this, 1000) // Continue if the binding is valid
                }
            }
        }

        val senderId = callMaker
        if (incoming && senderId !== null) {
            SocketRepository.socket.emit("respond", JSONObject().apply {
                put("sender", senderId)
                put("answer", true)
            })
            handler.post(runnable)
        } else {
            playRingtone()
            SocketRepository.socket.on("respond") {args ->
                Log.d("response", "received")
                stopRingtone()
                if (args.isNotEmpty()) {
                    when (val msg = args[0]) {
                        is Boolean -> {
                            if (msg && hostId !== null && targetId !== null) {
                                WebRTCRepository.createOffer(hostId, targetId)
                                handler.post(runnable)
                            } else {
                                Log.e("msg", "Message is false")
                            }
                        }
                        else -> {
                            Log.e("msg", "Unknown message type: ${msg::class.simpleName}")
                        }
                    }
                } else {
                    Log.e("msg", "No message received")
                }
            }
        }

        binding.endCallButton.setOnClickListener {
            stopRingtone()
            WebRTCRepository.endPeerConnection(binding.localView)
            SocketRepository.socket.emit("endCall", JSONObject().apply {
                put("sender", hostId)
                put("recipient", targetId)
            })
            findNavController().popBackStack()
        }

        binding.handUp.setOnClickListener {
            WebRTCRepository.endPeerConnection(binding.localView)
            Log.d("error", "hostId: $hostId, targetId: $targetId")
            SocketRepository.socket.emit("endCall", JSONObject().apply {
                put("sender", hostId)
                put("recipient", targetId)
            })
            findNavController().popBackStack()
        }

        binding.xBtn.setOnClickListener {
            findNavController().popBackStack()
        }

//        binding.retryBtn.setOnClickListener {
//            val bundle = Bundle().apply {
//                putBoolean("incoming", false)
//            }
//
//            if (callType == "video") {
//                bundle.putString("callType", "video")
//            } else if (callType == "audio call") {
//                bundle.putString("callType", "audio")
//            }
//
//            findNavController().popBackStack()
//            Log.d("navigation", "pop back stack")
//            findNavController().navigate(R.id.nav_call, bundle)
//            Log.d("navigation", "navigate to nav call")
//        }

        binding.muteButton.setOnClickListener {
            WebRTCRepository.toggleMuteAudio()
        }
        binding.micButton.setOnClickListener {
            WebRTCRepository.toggleMuteAudio()
        }

        binding.switchOutput.setOnClickListener {
            toggleIconSpeaker(binding.switchOutput)
        }

        binding.audioOutputButton.setOnClickListener {
            toggleIconSpeaker(binding.audioOutputButton)
        }

        binding.speakerOn.setOnClickListener {
            toggleIconSpeaker(binding.speakerOn)
        }

        binding.switchCameraButton.setOnClickListener {
            WebRTCRepository.switchCamera()
            binding.switchCameraButton.rotation = 180f
        }

        WebRTCRepository.onPCclosed = {
            endCall()
            IncomingCallAlert.isViewAdded = false
        }

        SocketRepository.socket.on("endCall") {
            endCall()
        }

        SocketRepository.socket.on("no-answer") {
            if (binding !== null) {
                Handler(Looper.getMainLooper()).post {
                    binding.ongoingCall.visibility = View.GONE
                    binding.callLayout.visibility = View.GONE
                    binding.noAnswer.visibility = View.VISIBLE
                }
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        stopRingtone()
        player?.stop()
        player?.release()
    }

    override fun onPause() {
        super.onPause()
        stopRingtone()
    }

    fun playRingtone() {
        val ringbackUri = Uri.parse("android.resource://${requireContext().packageName}/raw/ring")
        player = MediaPlayer.create(context, ringbackUri).apply {
            isLooping = true
            start()
        }
    }

    fun endCall() {
        if (_binding !== null) {
            WebRTCRepository.endPeerConnection(binding.localView)
            stopRingtone()
            val inTargetUser: Boolean = findNavController().currentDestination?.id == R.id.target_user
            if (!inTargetUser) {
                Handler(Looper.getMainLooper()).post {
                    findNavController().navigate(R.id.target_user)
                }
            }

        }
    }

    private fun stopRingtone() {
        player?.apply {
            if (isLooping) stop()
            release()
        }
        player = null
    }

    fun toggleIconSpeaker(speakerButton: ImageView) {
        WebRTCRepository.toggleSpeaker(requireContext())
        var isSpeakerOn: Boolean = false
        speakerButton.setOnClickListener {
            isSpeakerOn = !isSpeakerOn

            // Perform scale animation
            speakerButton.animate().scaleX(0.7f).scaleY(0.7f).setDuration(150).withEndAction {
                // Update the drawable after the animation
                val drawableRes = if (isSpeakerOn) {
                    speakerButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
                    R.drawable.speaker_on
                } else {
                    speakerButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)
                    R.drawable.speaker_off
                }
                speakerButton.setImageResource(drawableRes)

                // Restore original size with a smooth animation
                speakerButton.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()
        }
    }
}