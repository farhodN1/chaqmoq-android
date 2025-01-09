package com.example.chaqmoq.repos

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

object SocketRepository {
    var ip: String = "http://192.168.1.101:5000"
    val socket: Socket = IO.socket("${ip}")
    var callMaker: String? = null

    fun onSocketConnection(context: Context) {
        val hostId = context.getSharedPreferences("UserInfo", Context.MODE_PRIVATE).getString("nickname", null)
        val targetId = context.getSharedPreferences("TargetInfo", Context.MODE_PRIVATE).getString("id", null)

        socket.connect()

        socket.on("iceCandidate") { candidate ->
            Log.d("candidate check", JSONObject(candidate[0].toString()).toString())
            try {
                val iceCandidate = IceCandidate(
                    JSONObject(candidate[0].toString())["sdpMid"].toString(),
                    JSONObject(candidate[0].toString())["sdpMLineIndex"] as Int,
                    JSONObject(candidate[0].toString())["candidate"].toString()
                )

                WebRTCRepository.peerConnection.addIceCandidate(iceCandidate)
            } catch (e: Exception) {
                Log.e("RTC", e.toString())
            }
        }

        socket.on("offer") { args ->
            if (args != null && args.isNotEmpty()) {
                val data = args[0].toString()
                Log.d("the offer","data ${args[0]})")
                val offer = SessionDescription(SessionDescription.Type.OFFER, data)
                Log.d("signaling state", WebRTCRepository.peerConnection.signalingState().toString())
                if (WebRTCRepository.peerConnection.signalingState() !== PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
                    WebRTCRepository.peerConnection.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d("RTC", "Remote offer set successfully")
                            if (hostId !== null && targetId !== null) {
                                WebRTCRepository.createAnswer(hostId, targetId)
                            }
                        }
                        override fun onCreateFailure(p0: String?) {
                            Log.e("offer", "error on oncreatefailure: ${p0}")
                        }
                        override fun onSetFailure(p0: String?) {
                            Log.e("offer", "Remote offer setting failed: $p0")
                        }
                    }, offer)
                }
            } else {
                Log.e("offer", "Received invalid data")
            }
        }

        socket.on("answer") { args ->
            if (args != null && args.isNotEmpty()) {
                val data = args[0].toString()
                Log.d("the answer","data ${args[0]})")
                    val answer = SessionDescription(SessionDescription.Type.ANSWER, data)
                    Log.d("ready", answer.toString())
                    Log.d("signaling state", WebRTCRepository.peerConnection.signalingState().toString())
                    if (WebRTCRepository.peerConnection.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
                        WebRTCRepository.peerConnection.setRemoteDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                Log.d("RTC", "Remote answer set successfully")
                                if (hostId !== null && targetId !== null) {
                                    WebRTCRepository.createAnswer(hostId, targetId)
                                }
                            }
                            override fun onCreateFailure(p0: String?) {
                                Log.e("answer", "error on oncreatefailure: ${p0}")
                            }
                            override fun onSetFailure(p0: String?) {
                                Log.e("answer", "Remote answer setting failed: $p0")
                            }
                        }, answer)
                    }
            } else {
                Log.e("answer", "Received invalid data")
            }
        }
    }
}