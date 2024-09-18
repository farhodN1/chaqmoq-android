package com.example.chaqmoq.ui.targetUser

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chaqmoq.adapter.MessageListAdapter
import com.google.gson.Gson
import com.example.chaqmoq.databinding.TargetUserBinding
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.*

class TargetUserFragment : Fragment(){
    private val gson = Gson()
    lateinit var factory: PeerConnectionFactory
    lateinit var peerConnection: PeerConnection
    val eglBase = EglBase.create()
    private var _binding: TargetUserBinding? = null
    val socket: Socket = IO.socket("http://192.168.222.115:5000")
    private val binding get() = _binding!!
    private lateinit var messageListAdapter: MessageListAdapter
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
    ): View? {
        val targetUserViewModel =
            ViewModelProvider(this).get(TargetUserViewModel::class.java)

        _binding = TargetUserBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView: RecyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        messageListAdapter = MessageListAdapter(hostData)
        binding.sendButton.setOnClickListener{sendMessage()}

        socket.connect()
        socket.on(Socket.EVENT_CONNECT) {
            Log.i("on connect", "successful")
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            Log.i("disconnect", "server is down")
        }

        socket.on("private message") {
            targetUserViewModel.makeNetworkRequest()
        }
        recyclerView.adapter = messageListAdapter

        targetUserViewModel.messageList.observe(viewLifecycleOwner) { messageList ->
            messageListAdapter.submitList(messageList)
        }
        initializePeerConnectionFactory()
        initializePeerConnection()
        onSocketConnection()
//        createOffer()
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        socket.disconnect()
    }
    fun sendMessage(){
        val targetId = targetData.getString("id", null)
        val targetEmail = targetData.getString("email", null)
        val targetPictureURL = targetData.getString("pictureURL", null)
        val hostId = hostData.getString("nickname", null)
        val sender = hostId
        val recipient = targetId
        val message = binding.messageEditText.text.toString()
        val jsonObject = JSONObject()
        jsonObject.put("sender", sender)
        jsonObject.put("recipient", recipient)
        jsonObject.put("message", message)
        socket.emit("private message", jsonObject)
        binding.messageEditText.text = Editable.Factory.getInstance().newEditable("")
    }
    private fun initializePeerConnectionFactory() {
        // Initialize PeerConnectionFactory with valid context
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()
    }

    private fun initializePeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                Log.d("ice", "candidate")
                val iceCandidate = hashMapOf(
                    "sdpMid" to candidate?.sdpMid,
                    "sdpMLineIndex" to candidate?.sdpMLineIndex,
                    "sdpCandidate" to candidate?.sdp
                )
                Log.d("candidate", iceCandidate.toString())
                socket.emit("iceCandidate", iceCandidate)
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        }) ?: return

        val videoSource = factory.createVideoSource(false)
        val videoTrack = factory.createVideoTrack("videoTrack", videoSource)
        videoTrack.setEnabled(true)
        val audioSource = factory.createAudioSource(MediaConstraints())
        val audioTrack = factory.createAudioTrack("audioTrack", audioSource)
        audioTrack.setEnabled(true)

        peerConnection.addTrack(videoTrack)
        peerConnection.addTrack(audioTrack)
    }
    fun createOffer(){
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
               peerConnection?.setLocalDescription(object : SdpObserver {
                   override fun onCreateSuccess(p0: SessionDescription?) {
                       TODO("Not yet implemented")
                   }

                   override fun onSetSuccess() {
                       val offer = hashMapOf(
                           "sdp" to desc?.description,
                           "type" to desc?.type
                       )
                       socket.emit("offer", offer)
                   }

                   override fun onCreateFailure(p0: String?) {
                       TODO("Not yet implemented")
                   }

                   override fun onSetFailure(p0: String?) {
                       TODO("Not yet implemented")
                   }

               }, desc)
            }
            override fun onSetSuccess() {

            }
            override fun onCreateFailure(error: String?) {
                Log.e("SDP", "Offer creation failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    fun createAnswer() {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }


                    override fun onSetSuccess() {
                        val answer = hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type
                        )
                        socket.emit("answer", answer)
                    }

                    override fun onCreateFailure(p0: String?) {
                    }

                    override fun onSetFailure(p0: String?) {
                    }

                }, desc)
            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }

        }, constraints)
    }

    private fun onSocketConnection() {
        Log.d("socket", "listener")
        socket.on("iceCandidate") { candidate ->
            Log.d("candidate", candidate.toString())
        }
        socket.on("offer"){args ->
            if (args != null && args.isNotEmpty()) {
                // Assuming the first argument in the array is the SDP string
                val sdp = args[0] as? String
                if (sdp != null) {
                    Log.d("offer", "triggered $sdp")
                    val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
                    peerConnection.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d("offer", "Remote offer set successfully")

                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {
                            Log.e("offer", "Remote offer setting failed: $p0")
                        }
                    }, offer)
                } else {
                    Log.e("offer", "Received SDP is not a valid string")
                }
            } else {
                Log.e("offer", "Received invalid data")
            }

        }
        socket.on("answer"){ args ->
            val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, args.toString())

            peerConnection.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(p0: SessionDescription?) {
                    TODO("Not yet implemented")
                }

                override fun onSetSuccess() {
                    TODO("Not yet implemented")
                }

                override fun onCreateFailure(p0: String?) {
                    TODO("Not yet implemented")
                }

                override fun onSetFailure(p0: String?) {
                    TODO("Not yet implemented")
                }
            }, sessionDescription)
        }
        socket.on("iceCandidate"){ args ->
            try {
                val receivingCandidate = gson.fromJson(gson.toJson(args),
                    IceCandidateModel::class.java)
                peerConnection.addIceCandidate(IceCandidate(receivingCandidate.sdpMid,
                    Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),receivingCandidate.sdpCandidate))
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

}

class IceCandidateModel(
    val sdpMid:String,
    val sdpMLineIndex:Double,
    val sdpCandidate:String
)
