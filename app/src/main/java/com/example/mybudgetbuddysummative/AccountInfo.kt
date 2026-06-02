package com.example.mybudgetbuddysummative

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account_info, container, false)

        txtUserName = view.findViewById(R.id.txtUserName)
        txtUserEmail = view.findViewById(R.id.txtUserEmail)
        txtDate = view.findViewById(R.id.txtDate)
        txtAchievements = view.findViewById(R.id.txtAchievements)
        txtSubscribed = view.findViewById(R.id.txtSubscribed)

        btnEditUser = view.findViewById(R.id.btnEditUser)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnClearData = view.findViewById(R.id.btnClearData)

        loadUserInfo()

        //setonclick listener
        btnEditUser.setOnClickListener {
            editUserInfo()
        }

        btnChangePassword.setOnClickListener {
            changePassword()
        }

        btnClearData.setOnClickListener {
            clearUserData()
        }

        return view
    }

    private fun loadUserInfo() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("users")

        dbRef.child(userId).get().addOnSuccessListener { snapshot ->
            val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
            val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
            val email = snapshot.child("email").getValue(String::class.java) ?: "Email will appear here"
            val joinDate = snapshot.child("joinDate").getValue(String::class.java) ?: "Date will appear here"
            val achievements = snapshot.child("achievements").getValue(Int::class.java) ?: 0
            val subscribed = snapshot.child("subscribed").getValue(Boolean::class.java) ?: false

            txtUserName.text = "$firstName $lastName"
            txtUserEmail.text = email
            txtDate.text = joinDate
            txtAchievements.text = achievements.toString()
            txtSubscribed.text = if (subscribed) "Yes" else "No"
        }
    }

    private fun editUserInfo() {
        val userId = auth.currentUser?.uid ?: return
        val newName = "Updated Name" // you’d get this from a dialog/input
        val newEmail = "updated@email.com"

        val updates = mapOf(
            "name" to newName,
            "email" to newEmail
        )

        dbRef.child(userId).updateChildren(updates).addOnSuccessListener {
            Toast.makeText(requireContext(), "User info updated", Toast.LENGTH_SHORT).show()
            loadUserInfo()
        }
    }

    private fun changePassword() {
        val newPassword = "newPassword123" // you’d get this from a dialog/input
        auth.currentUser?.updatePassword(newPassword)?.addOnSuccessListener {
            Toast.makeText(requireContext(), "Password changed", Toast.LENGTH_SHORT).show()
        }?.addOnFailureListener {
            Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance()

        db.getReference("expenses").child(userId).removeValue()
        db.getReference("envelopes").child(userId).removeValue()
        db.getReference("monthlygoal").child(userId).removeValue()
        db.getReference("subscription").child(userId).removeValue()
        db.getReference("achievements").child(userId).removeValue()

        Toast.makeText(requireContext(), "All linked data cleared", Toast.LENGTH_SHORT).show()
    }
}