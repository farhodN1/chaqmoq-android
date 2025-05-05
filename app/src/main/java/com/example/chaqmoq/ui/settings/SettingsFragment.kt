package com.example.chaqmoq.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.chaqmoq.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        binding.clearConsBtn.setOnClickListener {
            viewModel.clearConversations()
        }

        binding.clearUsersBtn.setOnClickListener {
            viewModel.clearUsers()
        }

        binding.logout.setOnClickListener {
            val prefs = requireActivity().getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

        }

        val root: View = binding.root
        return root
    }

}