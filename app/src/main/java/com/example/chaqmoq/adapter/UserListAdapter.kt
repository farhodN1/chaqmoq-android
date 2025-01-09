package com.example.chaqmoq.adapter

import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chaqmoq.R
import com.example.chaqmoq.databinding.UserItemBinding
import com.example.chaqmoq.model.User
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class UserListAdapter(private val onItemClick: (User) -> Unit) : ListAdapter<User, UserListAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    inner class UserViewHolder(private val binding: UserItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            val imageView = itemView.findViewById<ImageView>(R.id.imageViewProfile)
            Glide.with(itemView)
                .load(user.profilePicture)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.circle_background)
                .error(R.drawable.circle_background)
                .into(imageView)
            binding.userNameTextView.text = user.username
            binding.root.setOnClickListener{ onItemClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
