package com.example.chaqmoq.repos

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.*

object WebRTCRepository {
    private var videoCapturer: CameraVideoCapturer? = null
    private val localVideoSource by lazy { factory.createVideoSource(false) }
    private val localAudioSource by lazy { factory.createAudioSource(MediaConstraints()) }
    lateinit var factory: PeerConnectionFactory
    lateinit var peerConnection: PeerConnection
    var localAudioTrack: AudioTrack? = null
    var localVideoTrack: VideoTrack? = null
    var isAudioOnly: Boolean = false
    val eglBase = EglBase.create()


    fun initializePeerConnectionFactory(context: Context) {

        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
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

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    // Creating a JSON object to represent the ICE candidate
                    val iceCandidate = JSONObject()
                    iceCandidate.put("candidate", it.sdp)
                    iceCandidate.put("sdpMid", it.sdpMid)
                    iceCandidate.put("sdpMLineIndex", it.sdpMLineIndex)
                    // Log the candidate for debugging
                    Log.d("candidate", iceCandidate.toString())

                    // Emit the candidate as a JSON object via the socket
                    SocketRepository.socket.emit("iceCandidate", iceCandidate.toString())
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
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        }) ?: return

        initializeSurfaceView(remoteView)
        initializeSurfaceView(localView)
//        if (!isAudioOnly) {
            startLocalVideo(localView, context)
//        } else startLocalAudio()

    }

    fun createOffer(){
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        TODO("Not yet implemented")
                    }

                    override fun onSetSuccess() {
                        Log.d("on", "create offer")
                        val offerJson = JSONObject()
                        offerJson.put("sdp", desc?.description)
                        offerJson.put("type", "offer")
                        SocketRepository.socket.emit("offer", offerJson)
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
                        val answerJson = JSONObject()
                        answerJson.put("sdp", desc?.description)
                        answerJson.put("type", "answer")
                        SocketRepository.socket.emit("answer", answerJson)
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
            SurfaceTextureHelper.create(Thread.currentThread().name, eglBase.eglBaseContext)
        if (!isAudioOnly) {
            videoCapturer = getVideoCapturer(context)
            videoCapturer?.initialize(
                surfaceTextureHelper,
                surface.context, localVideoSource.capturerObserver
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
    fun startLocalAudio() {
        localVideoTrack = factory.createVideoTrack("local_track", localVideoSource)
        localAudioTrack =
            factory.createAudioTrack("local_track_audio", localAudioSource)
        val localStream = factory.createLocalMediaStream("local_stream")
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
            return  // Exit if peerConnection is not initialized
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
        localVideoTrack?.removeSink(localView) // Remove the video sink (SurfaceViewRenderer)

        // Mute the audio track
        localAudioTrack?.setEnabled(false) // Mute the audio track

        // Close the PeerConnection
        peerConnection.close()

        // Clear the references to the media tracks
        localAudioTrack = null
        localVideoTrack = null

        // Optionally dispose of the PeerConnectionFactory if no longer needed
        factory.dispose() // Clear resources associated with PeerConnectionFactory

        // Optionally dispose of the EglBase if no longer needed
        eglBase.release()
    }



    fun initializeSurfaceView(surface: SurfaceViewRenderer) {
        surface.run {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglBase.eglBaseContext, null)
        }
    }
}