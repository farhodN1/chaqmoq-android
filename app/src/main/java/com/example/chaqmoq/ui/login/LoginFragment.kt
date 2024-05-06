package com.example.chaqmoq.ui.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.chaqmoq.databinding.ActivityLoginBinding

class LoginFragment : Fragment() {

    private var _binding: ActivityLoginBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val loginViewModel =
            ViewModelProvider(this).get(LoginViewModel::class.java)

        _binding = ActivityLoginBinding.inflate(inflater, container, false)
        val root: View = binding.root
        Log.i("hello", "go sleep")
        val textView: TextView = binding.nameTv
        loginViewModel.text.observe(viewLifecycleOwner) {
            textView.text = "oyhjgk"
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}