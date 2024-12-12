package com.example.chaqmoq.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import com.example.chaqmoq.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.Glide
import com.example.chaqmoq.MainActivity
import com.example.chaqmoq.databinding.FragmentHomeBinding
import com.example.chaqmoq.adapter.UserListAdapter
import com.example.chaqmoq.ui.targetUser.TargetUserViewModel

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var userListAdapter: UserListAdapter

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
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val givenName = hostData.getString("givenName", null)
        val picture = hostData.getString("pictureUrl", null)
        if (givenName !== null) {
            Glide.with(requireContext())
                .load(picture)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.roundimage_placeholder)
                .into(binding.hostImage)
        }
        val recyclerView: RecyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.reloadBtn.setOnClickListener {
            Log.d("reload", "clicked")
            homeViewModel.makeNetworkRequest()
        }
        userListAdapter = UserListAdapter { user ->
            Log.d("HomeFragment", "Clicked user: ${user.username}")
            findNavController().navigate(R.id.target_user)

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
        val hostId = hostData.getString("nickname", null)
        homeViewModel.userList.observe(viewLifecycleOwner) { userList ->
            binding.reloadBtn.visibility = View.GONE
            val filteredList = userList.filter { user ->
                user.id != hostId
            }
            userListAdapter.submitList(filteredList)
        }


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
