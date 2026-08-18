package com.cloudimny.views.setup

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cloudimny.AppPreferences
import com.cloudimny.R
import com.cloudimny.models.SshConnectionCredentials
import com.cloudimny.models.validation.SshValidationResult.INVALID_ADDRESS
import com.cloudimny.models.validation.SshValidationResult.INVALID_PASSWORD
import com.cloudimny.models.validation.SshValidationResult.INVALID_USERNAME
import com.cloudimny.models.validation.SshValidationResult.VALID
import com.cloudimny.models.validation.validateSshCredentials
import com.cloudimny.views.LoadingFragment

private const val LOADING_FRAGMENT_TAG = "loading"

class SetupCredentialsFragment : Fragment(R.layout.fragment_setup_credentials) {
    private val viewModel: SetupViewModel by activityViewModels()

    private lateinit var addressInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var setupConfirmButton: Button
    private lateinit var setupImportButton: Button

    // без фильтра по типу: экспорт уходит через ACTION_SEND, и каким расширением его
    // сохранит принимающее приложение — не наше дело; содержимое всё равно проверяется
    private val pickServerFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importServer(uri)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addressInput = view.findViewById(R.id.setup_address_input)
        usernameInput = view.findViewById(R.id.setup_username_input)
        passwordInput = view.findViewById(R.id.setup_password_input)
        setupConfirmButton = view.findViewById(R.id.setup_confirm_button)
        setupImportButton = view.findViewById(R.id.setup_import_button)

        setupImportButton.setOnClickListener {
            pickServerFile.launch(arrayOf("*/*"))
        }

        viewModel.errorMessageResId.observe(viewLifecycleOwner) { messageResId ->
            if (messageResId != null) {
                Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_LONG).show()
                hideLoadingFragment()
            }
        }

        viewModel.completed.observe(viewLifecycleOwner) { completed ->
            if (completed) {
                AppPreferences.setAuthorized(requireContext(), true)

                val intent = requireActivity().intent
                requireActivity().finish()
                startActivity(intent)
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
        val loadingFragment =
            parentFragmentManager.findFragmentByTag(LOADING_FRAGMENT_TAG) ?: return
        parentFragmentManager.beginTransaction()
            .remove(loadingFragment)
            .commit()
    }


}