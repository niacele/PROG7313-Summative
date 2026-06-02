package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class Register : Fragment() {

    private lateinit var edtFirstName: EditText
    private lateinit var edtLastName: EditText
    private lateinit var edtEmailRegister: EditText
    private lateinit var edtPasswordReg: EditText
    private lateinit var edtConfirmPasswordReg: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnRegisterPage: Button

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        // Bind views
        edtFirstName = view.findViewById(R.id.edtFirstName)
        edtLastName = view.findViewById(R.id.edtLastName)
        edtEmailRegister = view.findViewById(R.id.edtEmailRegister)
        edtPasswordReg = view.findViewById(R.id.edtPasswordReg)
        edtConfirmPasswordReg = view.findViewById(R.id.edtConfirmPasswordReg)
        btnRegister = view.findViewById(R.id.btnRegister)
        btnRegisterPage = view.findViewById(R.id.btnRegisterPage)

        // Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Register button
        btnRegister.setOnClickListener {
            registerUser()
        }

        // Navigate back to Login fragment
        btnRegisterPage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Login())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun registerUser() {
        val firstName = edtFirstName.text.toString().trim()
        val lastName = edtLastName.text.toString().trim()
        val email = edtEmailRegister.text.toString().trim()
        val password = edtPasswordReg.text.toString().trim()
        val confirmPassword = edtConfirmPasswordReg.text.toString().trim()

        // Validation
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase registration
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "User registered successfully", Toast.LENGTH_SHORT).show()
                    clearFields()
                    //go to log in
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Login())
                        .commit()
                } else {
                    Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun clearFields() {
        edtFirstName.text.clear()
        edtLastName.text.clear()
        edtEmailRegister.text.clear()
        edtPasswordReg.text.clear()
        edtConfirmPasswordReg.text.clear()
    }
}
