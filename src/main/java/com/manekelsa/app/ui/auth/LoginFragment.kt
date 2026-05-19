package com.manekelsa.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.manekelsa.app.R
import com.manekelsa.app.data.firebase.FirebaseService
import com.manekelsa.app.data.repository.AuthRepository
import com.manekelsa.app.databinding.FragmentLoginBinding
import com.manekelsa.app.util.Resource
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(AuthRepository(FirebaseService())) as T
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.isLoggedIn()) { navigateToDirectory(); return }
        binding.btnContinue.setOnClickListener { viewModel.signInAnonymously() }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is Resource.Loading -> { binding.progressBar.visibility = View.VISIBLE; binding.btnContinue.isEnabled = false }
                        is Resource.Success -> { binding.progressBar.visibility = View.GONE; navigateToDirectory() }
                        is Resource.Error -> { binding.progressBar.visibility = View.GONE; binding.btnContinue.isEnabled = true; Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show() }
                        null -> { binding.progressBar.visibility = View.GONE; binding.btnContinue.isEnabled = true }
                    }
                }
            }
        }
    }

    private fun navigateToDirectory() {
        findNavController().navigate(R.id.action_loginFragment_to_directoryFragment)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
