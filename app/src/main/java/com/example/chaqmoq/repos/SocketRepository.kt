package com.example.chaqmoq.repos

import android.content.Context
import android.content.Intent
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

object SocketRepository {
    val socket: Socket = IO.socket("http://192.168.1.7:5000")

    fun onSocketConnection() {
        socket.connect()
        socket.on("iceCandidate") { candidate ->
            Log.d("candidate check", candidate.isNotEmpty().toString())
            if (candidate.isNotEmpty()) {
                val data = candidate[0] as? String
                if (data != null) {
                    Log.d("iceCandidate t2", "$data")
                    try {
                        val candidateJson = JSONObject(data)
                        val iceCandidate = IceCandidate(
                            candidateJson.getString("sdpMid"),
                            candidateJson.getInt("sdpMLineIndex"),
                            candidateJson.getString("candidate")
                        )
                        WebRTCRepository.peerConnection.addIceCandidate(iceCandidate)
                    } catch (e: JSONException) {
                        Log.e("iceCandidate Error", "Failed to parse ICE candidate: ${e.message}")
                    }
                } else {
                    Log.e("iceCandidate Error", "The first element is not a String.")
                }
            } else {
                Log.e("iceCandidate Error", "Received empty candidate array.")
            }
        }

        socket.on("offer") { args ->
            if (args != null && args.isNotEmpty()) {
                val data = args[0].toString()
                Log.d("the offer","data ${args[0]})")
                val offerJson = JSONObject(data)
                val sdp = offerJson.getString("sdp")
                val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
                Log.d("ready", offer.toString())
                WebRTCRepository.peerConnection.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d("offer", "Remote offer set successfully")
                        WebRTCRepository.createAnswer()
                    }
                    override fun onCreateFailure(p0: String?) {
                        Log.e("offer", "error on oncreatefailure: ${p0}")
                    }
                    override fun onSetFailure(p0: String?) {
                        Log.e("offer", "Remote offer setting failed: $p0")
                    }
                }, offer)
            } else {
                Log.e("offer", "Received invalid data")
            }
        }

        socket.on("answer") { args ->
            if (args != null && args.isNotEmpty()) {
                val data = args[0].toString()
                Log.d("the answer","data ${args[0]})")
                val answerJson = JSONObject(data)
                val sdp = answerJson.getString("sdp")
                Log.d("answerJson", sdp.toString())
                    val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                    Log.d("ready", answer.toString())
                    WebRTCRepository.peerConnection.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d("answer", "Remote answer set successfully")
                            WebRTCRepository.createAnswer()
                        }
                        override fun onCreateFailure(p0: String?) {
                            Log.e("answer", "error on oncreatefailure: ${p0}")
                        }
                        override fun onSetFailure(p0: String?) {
                            Log.e("answer", "Remote answer setting failed: $p0")
                        }
                    }, answer)
            } else {
                Log.e("answer", "Received invalid data")
            }
        }
    }
}