package com.example.chaqmoq.repos

import android.content.Context
import android.media.AudioManager
import android.util.Log
import org.json.JSONObject
import org.webrtc.*

object WebRTCRepository {
    private var videoCapturer: CameraVideoCapturer? = null
    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null
    lateinit var factory: PeerConnectionFactory
    lateinit var peerConnection: PeerConnection
    var localAudioTrack: AudioTrack? = null
    var localVideoTrack: VideoTrack? = null
    var isAudioOnly: Boolean = false
    private var eglBase: EglBase? = null

    fun initializeEglBase() {
        if (eglBase == null) {
            eglBase = EglBase.create()
        }
    }
    fun initializeSources() {
        localVideoSource = factory.createVideoSource(false)
        localAudioSource = factory.createAudioSource(MediaConstraints())
    }

    var onPCclosed: (() -> Unit)? = null


    fun initializePeerConnectionFactory(context: Context) {

        initializeEglBase()
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase?.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase?.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()
    }

    fun initializePeerConnection(context: Context, remoteView: SurfaceViewRenderer, localView: SurfaceViewRenderer, audioOnly: Boolean) {
        isAudioOnly = audioOnly
        Log.d("isAudioOnly", isAudioOnly.toString())
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        val hostId = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE).getString("nickname", null)
        val targetId = context.getSharedPreferences("TargetInfo", Context.MODE_PRIVATE).getString("id", null)
        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    // Creating a JSON object to represent the ICE candidate
                    val json = JSONObject()
                    json.put("candidate", it.sdp)
                    json.put("sdpMid", it.sdpMid)
                    json.put("sdpMLineIndex", it.sdpMLineIndex)
                    json.put("sender", hostId)
                    json.put("receiver", targetId)
                    Log.d("RTC", "emitting ice candidates")
                    SocketRepository.socket.emit("iceCandidate", json)
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                if (stream == null) {
                    Log.e("WebRTC", "onAddStream: Remote stream is null")
                    return
                }

                // Log details about the incoming media stream
                Log.d("WebRTC", "onAddStream: Received media stream: $stream")

                // Handling video tracks
                val videoTracks = stream.videoTracks
                if (videoTracks.isNotEmpty()) {
                    // Log the video track details
                    Log.d("WebRTC", "onAddStream: Video track count: ${videoTracks.size}")
                    val remoteVideoTrack = videoTracks[0]
                    Log.d("WebRTC", "onAddStream: Remote video track: $remoteVideoTrack")

                    // Add the video track to the remote view (SurfaceViewRenderer)
                    remoteVideoTrack.addSink(remoteView)
                } else {
                    Log.w("WebRTC", "onAddStream: No video tracks found in the remote stream")
                }

                // You can also handle audio tracks here if needed
                val audioTracks = stream.audioTracks
                if (audioTracks.isNotEmpty()) {
                    Log.d("WebRTC", "onAddStream: Audio track count: ${audioTracks.size}")
                    val remoteAudioTrack = audioTracks[0]
                    // You can choose to log or use the audio track
                    Log.d("WebRTC", "onAddStream: Remote audio track: $remoteAudioTrack")
                } else {
                    Log.w("WebRTC", "onAddStream: No audio tracks found in the remote stream")
                }
            }
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<MediaStream>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                when (state) {
                    PeerConnection.IceConnectionState.CLOSED -> {
                        Log.d("PeerConnection", "Connection is closed")
                        onPCclosed?.invoke()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        Log.d("PeerConnection", "Connection is disconnected")
                        onPCclosed?.invoke()
                    }
                    else -> {
                        Log.d("PeerConnection", "ICE state changed: $state")
                    }
                }
            }
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        }) ?: return

        initializeSurfaceView(remoteView)
        initializeSurfaceView(localView)
        initializeSources()
        startLocalVideo(localView, context)


    }

    fun createOffer(hostId: String, targetId: String){

        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        TODO("Not yet implemented")
                    }

                    override fun onSetSuccess() {
                        Log.d("RTC", "emitting offer")
                        val json = JSONObject()
                        json.put("sdp", desc?.description)
                        json.put("sender", hostId)
                        json.put("receiver", targetId)

                        SocketRepository.socket.emit("offer", json)
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

    fun createAnswer(hostId: String, targetId: String) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }

                    override fun onSetSuccess() {
                        val json = JSONObject()
                        json.put("sdp", desc?.description)
                        json.put("sender", hostId)
                        json.put("receiver", targetId)

                        Log.d("RTC", "emitting answer")
                        SocketRepository.socket.emit("answer", json)
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

    fun startLocalVideo(surface: SurfaceViewRenderer, context: Context) {
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, eglBase?.eglBaseContext)
        if (!isAudioOnly) {
            videoCapturer = getVideoCapturer(context)
            videoCapturer?.initialize(
                surfaceTextureHelper,
                surface.context, localVideoSource?.capturerObserver
            )
            videoCapturer?.startCapture(320, 240, 30)
        }

        localVideoTrack = factory.createVideoTrack("local_track", localVideoSource)
        localVideoTrack?.addSink(surface)
        localAudioTrack =
            factory.createAudioTrack("local_track_audio", localAudioSource)
        val localStream = factory.createLocalMediaStream("local_stream")
        localStream.addTrack(localAudioTrack)
        localStream.addTrack(localVideoTrack)

        peerConnection.addStream(localStream)

    }

    private fun getVideoCapturer(context: Context): CameraVideoCapturer {
        return Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw
            IllegalStateException()
        }
    }

    fun endPeerConnection(localView: SurfaceViewRenderer) {
        // Check if peerConnection is initialized
        if (!::peerConnection.isInitialized) {
            Log.e("WebRTC", "peerConnection is not initialized.")
            return
        }

        // Avoid multiple invocations
        if (peerConnection.iceConnectionState() == PeerConnection.IceConnectionState.CLOSED) {
            Log.d("WebRTC", "Peer connection already closed.")
            return
        }

        // Stop the video capture (if initialized)
        videoCapturer?.let {
            try {
                it.stopCapture()  // Stop the camera capture
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            it.dispose()  // Release resources
            videoCapturer = null  // Clear reference
        }

        // Stop the video track
        localVideoTrack?.setEnabled(false) // Disable the video track
        localVideoTrack?.removeSink(localView) // Remove the video sink

        // Mute the audio track
        localAudioTrack?.setEnabled(false) // Mute the audio track

        // Close the PeerConnection
        peerConnection.close()
        peerConnection.dispose()

        localAudioSource?.dispose()
        localVideoSource?.dispose()

        // Clear references to media tracks
        localAudioTrack = null
        localVideoTrack = null


        // Dispose of the PeerConnectionFactory and EGL base
        factory.dispose() // Clear resources
        localView.release()

        eglBase?.release()
        eglBase = null
    }

    fun initializeSurfaceView(surface: SurfaceViewRenderer) {
        surface.run {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglBase?.eglBaseContext, null)
        }
    }

    fun switchCamera() {
        videoCapturer?.let {
            if (it is CameraVideoCapturer) {
                it.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                    override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                        Log.d("WebRTC", "Camera switched to ${if (isFrontCamera) "front" else "back"}")
                    }

                    override fun onCameraSwitchError(errorDescription: String?) {
                        Log.e("WebRTC", "Error switching camera: $errorDescription")
                    }
                })
            } else {
                Log.e("WebRTC", "Video capturer is not an instance of CameraVideoCapturer")
            }
        }
    }

    fun toggleMuteAudio() {
        localAudioTrack?.let {
            val isEnabled = it.enabled()
            it.setEnabled(!isEnabled)
            Log.d("WebRTC", "Audio is now ${if (isEnabled) "muted" else "unmuted"}")
        }
    }

    fun toggleSpeaker(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Toggle speakerphone
        val isCurrentlyOn = audioManager.isSpeakerphoneOn
        audioManager.isSpeakerphoneOn = !isCurrentlyOn

        // Ensure the audio mode is set correctly
        audioManager.mode = if (!isCurrentlyOn) {
            AudioManager.MODE_IN_COMMUNICATION // Turn speakerphone ON
        } else {
            AudioManager.MODE_NORMAL // Turn speakerphone OFF
        }

        Log.d("WebRTC", "Speakerphone toggled: ${!isCurrentlyOn}")
    }


}