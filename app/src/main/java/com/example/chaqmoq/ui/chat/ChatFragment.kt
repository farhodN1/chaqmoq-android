package com.example.chaqmoq.ui.chat

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import com.example.chaqmoq.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.Glide
import com.example.chaqmoq.adapter.UserListAdapter
import com.example.chaqmoq.databinding.FragmentChatBinding
import com.example.chaqmoq.repos.DatabaseRepository.saveUsers
import com.example.chaqmoq.utils.GlobalVariables
import com.example.chaqmoq.utils.GlobalVariables.target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var userListAdapter: UserListAdapter
    private lateinit var homeViewModel: ChatViewModel

    private val targetData: SharedPreferences by lazy {
        requireActivity().getSharedPreferences("TargetInfo", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(ChatViewModel::class.java)
        homeViewModel.fetchUsers()
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val root: View = binding.root

        Glide.with(requireContext())
            .load(GlobalVariables.host?.profilePicture)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.roundimage_placeholder)
            .into(binding.hostImage)

        val recyclerView: RecyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        userListAdapter = UserListAdapter { user ->
            Log.d("HomeFragment", "Clicked user: ${user.username}")
            findNavController().navigate(R.id.target_user)
            target = user
            with(targetData.edit()) {
                putString("id", user.id)
                putString("username", user.username)
                putString("email", user.email)
                putString("pictureURL", user.profilePicture)
                putString("socket_id", user.socket_id)
                putString("status", user.status)
                putString("lastSeen", user.lastSeen)
                apply()
            }
        }
        recyclerView.adapter = userListAdapter

        binding.swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.fetchUsers()
        }

        homeViewModel.userList.observe(viewLifecycleOwner) { userList ->
            binding.progressBar.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false
            val filteredList = userList.filter { user ->
                user.id != GlobalVariables.host?.id
            }
            userListAdapter.submitList(filteredList)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    saveUsers(userList)
                } catch (e: Exception) {
                    Log.d("error", "can't save users")
                }

            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
