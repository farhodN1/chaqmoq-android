package com.example.chaqmoq.ui.callWindow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.chaqmoq.R
import com.example.chaqmoq.databinding.CallWindowBinding
import com.example.chaqmoq.repos.SocketRepository
import com.example.chaqmoq.repos.WebRTCRepository

class CallWindowFragment : Fragment() {

    private var _binding: CallWindowBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        val slideshowViewModel =
//            ViewModelProvider(this).get(CallWindowViewModel::class.java)
        _binding = CallWindowBinding.inflate(inflater, container, false)
        val root: View = binding.root
        WebRTCRepository.initializePeerConnectionFactory(requireContext())
        val incoming = arguments?.getBoolean("incoming") ?: false
        val callType = arguments?.getString("callType")
//        Log.d("callType", callType.toString())
        if (callType == "video call") {
            WebRTCRepository.initializePeerConnection(requireContext(), binding.remoteView, binding.localView, false)
            binding.remoteViewLoading.visibility = View.GONE
        } else if (callType == "audio call") {
            WebRTCRepository.initializePeerConnection(requireContext(), binding.remoteView, binding.localView, true)
            binding.callLayout.visibility = View.GONE
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
        if (incoming && SocketRepository.socket.connected()) {
            SocketRepository.socket.emit("respond", true)
            handler.post(runnable)
        } else {
            SocketRepository.socket.on("respond") {args ->
                if (args.isNotEmpty()) {
                    val msg = args[0]
                    when (msg) {
                        is Boolean -> {
                            if (msg) {
                                WebRTCRepository.createOffer()
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
            WebRTCRepository.endPeerConnection(binding.localView)
            SocketRepository.socket.emit("endCall")
            findNavController().navigate(R.id.target_user)
        }
        binding.handUp.setOnClickListener {
            WebRTCRepository.endPeerConnection(binding.localView)
            SocketRepository.socket.emit("endCall")
            findNavController().navigate(R.id.target_user)
        }
        SocketRepository.socket.on("endCall") {
            WebRTCRepository.endPeerConnection(binding.localView)
            WebRTCRepository.endPeerConnection(binding.remoteView)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}