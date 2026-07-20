package com.cloudimny.views.setup

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cloudimny.R
import com.cloudimny.models.SshConnectionCredentials
import com.cloudimny.models.validation.validateSshCredentials
import com.cloudimny.models.validation.SshValidationResult.*
import com.cloudimny.views.LoadingFragment

private const val LOADING_FRAGMENT_TAG = "loading"

class SetupCredentialsFragment : Fragment(R.layout.fragment_setup_credentials) {
    private val viewModel: SetupViewModel by activityViewModels()

    private lateinit var addressInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var setupConfirmButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addressInput = view.findViewById(R.id.setup_address_input)
        usernameInput = view.findViewById(R.id.setup_username_input)
        passwordInput = view.findViewById(R.id.setup_password_input)
        setupConfirmButton = view.findViewById(R.id.setup_confirm_button)

        viewModel.errorMessageResId.observe(viewLifecycleOwner) { messageResId ->
            if (messageResId != null) {
                Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_LONG).show()
                hideLoadingFragment()
            }
        }

        setupConfirmButton.setOnClickListener {
            val credentials = SshConnectionCredentials(
                addressInput.text.toString(),
                usernameInput.text.toString(),
                passwordInput.text.toString()
            )

            val errorMessage: String? =
                when (validateSshCredentials(credentials)) {
                    INVALID_ADDRESS ->
                        getString(R.string.invalid_address_message)

                    INVALID_USERNAME ->
                        getString(R.string.invalid_username_message)

                    INVALID_PASSWORD ->
                        getString(R.string.invalid_password_message)

                    VALID ->
                        null
                }

            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.connectAndConfigure(credentials)
            parentFragmentManager.beginTransaction()
                .add(
                    R.id.main,
                    LoadingFragment.newInstance(
                        getString(R.string.setting_server_up),
                        getString(R.string.it_might_take_a_few_minutes)
                    ),
                    LOADING_FRAGMENT_TAG
                ).commit()
        }
    }

    private fun hideLoadingFragment() {
        val loadingFragment = parentFragmentManager.findFragmentByTag(LOADING_FRAGMENT_TAG) ?: return
        parentFragmentManager.beginTransaction()
            .remove(loadingFragment)
            .commit()
    }
}