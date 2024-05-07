package com.example.chaqmoq.adapter

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chaqmoq.databinding.MessageItemBinding
import com.example.chaqmoq.model.Message

class MessageListAdapter(private val userData: SharedPreferences) :
    ListAdapter<Message, MessageListAdapter.MessageViewHolder>(MessageDiffCallback()) {

    private lateinit var recyclerView: RecyclerView

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = MessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message, userData)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun submitList(list: List<Message>?) {
        super.submitList(list)
        // Scroll to the bottom after the list is updated
        recyclerView.post {
            recyclerView.scrollToPosition(itemCount - 1)
        }
    }

    class MessageViewHolder(private val binding: MessageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message, userData: SharedPreferences) {
            val hostId = userData.getString("nickname", null)
            binding.message = message
            binding.hostId = hostId
            binding.executePendingBindings()
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}

