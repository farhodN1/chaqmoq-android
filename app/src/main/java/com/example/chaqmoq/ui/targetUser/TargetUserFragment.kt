package com.example.chaqmoq.ui.targetUser

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
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
import com.example.chaqmoq.databinding.TargetUserBinding
import io.socket.client.IO
import io.socket.client.Socket


class TargetUserFragment : Fragment(){

    private var _binding: TargetUserBinding? = null
    val socket: Socket = IO.socket("http://192.168.41.168:5000")
    private val binding get() = _binding!!
    private lateinit var messageListAdapter: MessageListAdapter
    private val sharedPreferences: SharedPreferences by lazy {
        requireActivity().getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
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
        messageListAdapter = MessageListAdapter(sharedPreferences)
        binding.sendButton.setOnClickListener{sendMessage()}

        socket.connect()
        socket.on(Socket.EVENT_CONNECT) {
            // Handle connection
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            // Handle disconnection
        }

        socket.on("customEvent") { args ->

        }
        socket.emit("eventName", )
        recyclerView.adapter = messageListAdapter

        targetUserViewModel.messageList.observe(viewLifecycleOwner) { messageList ->
            messageListAdapter.submitList(messageList)
        }
        return root
        val socket: Socket = IO.socket("http://192.168.41.168:5000")
        socket.connect()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    fun sendMessage(){
//        socket.emit("private message", )
        Log.i("hi", binding.messageEditText.text.toString())
    }
}
