package com.example.chaqmoq.ui.callsFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.chaqmoq.databinding.CallsFragmentBinding

class CallsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = CallsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
}