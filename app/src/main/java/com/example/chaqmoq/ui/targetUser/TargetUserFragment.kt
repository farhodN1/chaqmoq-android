package com.example.chaqmoq.ui.targetUser

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chaqmoq.adapter.MessageListAdapter
import com.example.chaqmoq.model.Message
import com.example.chaqmoq.databinding.TargetUserBinding
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class TargetUserFragment : Fragment(){

    private var _binding: TargetUserBinding? = null
    val socket: Socket = IO.socket("http://192.168.78.168:5000")
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
}
