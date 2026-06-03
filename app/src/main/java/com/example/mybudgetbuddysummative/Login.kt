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
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        edtEmail = view.findViewById(R.id.edtEmail)
        edtPassword = view.findViewById(R.id.edtPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        btnRegisterPage = view.findViewById(R.id.btnRegisterPage)

        auth = FirebaseAuth.getInstance()

        btnLogin.setOnClickListener { loginUser() }

        btnRegisterPage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Register())
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
                    val user = auth.currentUser
                    UserSession.uid = user?.uid
                    UserSession.email = user?.email

                    Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()

                    (activity as? MainActivity)?.enableBottomNav()

                } else {
                    Toast.makeText(requireContext(), "Login failed. Please check your credentials.", Toast.LENGTH_LONG).show()
                }
            }
    }
}
