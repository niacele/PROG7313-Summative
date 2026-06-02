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

class Login : Fragment() {

    //global declarations
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegisterPage: Button

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the correct layout file (make sure it's named fragment_login.xml)
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        //typecasting
        edtEmail = view.findViewById(R.id.edtEmail)
        edtPassword = view.findViewById(R.id.edtPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        btnRegisterPage = view.findViewById(R.id.btnRegisterPage)

        // Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Login button
        btnLogin.setOnClickListener {
            loginUser()
        }

        // Navigate to Register fragment
        btnRegisterPage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Register()) // Register must also be a Fragment
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun loginUser() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                    // Navigate to Home fragment
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, Home())
                        .commit()
                } else {
                    Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
