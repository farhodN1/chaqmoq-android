package com.example.chaqmoq.ui.home

import android.os.Bundle
import android.util.Log
import com.example.chaqmoq.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chaqmoq.MainActivity
import com.example.chaqmoq.databinding.FragmentHomeBinding
import com.example.chaqmoq.adapter.UserListAdapter

class HomeFragment : Fragment() {
    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var userListAdapter: UserListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
//        val isAuthenticated = MainActivity.sharedPrefs.getBoolean("is_authenticated", false)
//        Log.i("auth", isAuthenticated.toString());
        val recyclerView: RecyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        userListAdapter = UserListAdapter{user ->
            Log.d("HomeFragment", "Clicked user: ${user.username}")
            findNavController().navigate(R.id.target_user)
        }
        recyclerView.adapter = userListAdapter

        homeViewModel.userList.observe(viewLifecycleOwner) { userList ->
            userListAdapter.submitList(userList)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
