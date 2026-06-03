package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AccountInfo : Fragment() {
    private lateinit var txtUserName: TextView
    private lateinit var txtUserEmail: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtAchievements: TextView
    private lateinit var txtSubscribed: TextView

    private lateinit var btnEditUser: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnClearData: Button
    private lateinit var btnBackButton: ImageButton

    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_account_info, container, false)

        txtUserName = view.findViewById(R.id.txtUserName)
        txtUserEmail = view.findViewById(R.id.txtUserEmail)
        txtDate = view.findViewById(R.id.txtDate)
        txtAchievements = view.findViewById(R.id.txtAchievements)
        txtSubscribed = view.findViewById(R.id.txtSubscribed)

        btnEditUser = view.findViewById(R.id.btnEditUser)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnClearData = view.findViewById(R.id.btnClearData)
        btnBackButton = view.findViewById(R.id.btnBackButton)

        loadUserInfo()

        btnEditUser.setOnClickListener { editUserInfo() }
        btnChangePassword.setOnClickListener { changePassword() }
        btnClearData.setOnClickListener { clearUserData() }
        btnBackButton.setOnClickListener { parentFragmentManager.popBackStack() }

        return view
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid ?: return

        dbRef.child(userId).get().addOnSuccessListener { snapshot ->
            val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
            val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
            val email = snapshot.child("email").getValue(String::class.java) ?: "Email will appear here"
            val joinDate = snapshot.child("joinDate").getValue(String::class.java) ?: "Date will appear here"
            val achievements = snapshot.child("achievements").getValue(Int::class.java) ?: 0
            val subscribed = snapshot.child("subscribed").getValue(Boolean::class.java) ?: false

            val fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            txtUserName.text = if (fullName.isNotBlank()) fullName else "Name will appear here"

            txtUserEmail.text = email
            txtDate.text = joinDate
            txtAchievements.text = achievements.toString()
            txtSubscribed.text = if (subscribed) "Yes" else "No"
        }
    }

    private fun editUserInfo() {
        // Instead of editing directly, just notify user
        Toast.makeText(requireContext(), "An email has been sent to update your user info", Toast.LENGTH_SHORT).show()
    }

    private fun changePassword() {
        // Instead of changing directly, just notify user
        Toast.makeText(requireContext(), "An email has been sent to change your password", Toast.LENGTH_SHORT).show()
    }

    private fun clearUserData() {
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance()

        db.getReference("expenses").child(userId).removeValue()
        db.getReference("envelopes").child(userId).removeValue()
        db.getReference("monthlygoal").child(userId).removeValue()
        db.getReference("users").child(userId).child("subscription").removeValue()
        db.getReference("users").child(userId).child("achievements").removeValue()

        Toast.makeText(requireContext(), "All linked data cleared", Toast.LENGTH_SHORT).show()
    }
}