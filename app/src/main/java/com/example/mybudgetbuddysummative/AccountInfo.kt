package com.example.mybudgetbuddysummative

import android.os.Bundle
import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val databaseUrl = "https://firebaseio.com"
    private lateinit var dbRef: com.google.firebase.database.DatabaseReference

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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is MainActivity) {
            dbRef = (activity as MainActivity).envelopesRef.root.child("users")
        } else {
            dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users")
        }

        loadUserInfo()

        btnEditUser.setOnClickListener { editUserInfo() }
        btnChangePassword.setOnClickListener { changePassword() }
        btnClearData.setOnClickListener { clearUserData() }
        btnBackButton.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: "test_development_user"

        txtUserEmail.text = currentUser?.email ?: "Email will appear here"
        txtUserName.text = currentUser?.displayName ?: "Name will appear here"
        txtDate.text = "Loading..."

        dbRef.child(userId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val firstName = snapshot.child("firstName").getValue(String::class.java)?.trim() ?: ""
                val lastName = snapshot.child("lastName").getValue(String::class.java)?.trim() ?: ""

                val email = snapshot.child("email").getValue(String::class.java) ?: currentUser?.email ?: "Email will appear here"
                val joinDate = snapshot.child("joinDate").getValue(String::class.java) ?: "Not Available"
                val achievements = snapshot.child("achievements").getValue(Int::class.java) ?: 0
                val subscribed = snapshot.child("subscribed").getValue(Boolean::class.java) ?: false

                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                    val joinedFullName = "$firstName $lastName".trim()
                    txtUserName.text = "$joinedFullName"
                } else {
                    val fallbackName = currentUser?.displayName ?: "Name will appear here"
                    txtUserName.text = "$fallbackName"
                }

                txtUserEmail.text = email
                txtDate.text = joinDate
                txtAchievements.text = if (achievements > 0) achievements.toString() else "Coming Soon!"
                txtSubscribed.text = if (subscribed) "Yes" else "No"
            } else {
                val metadata = currentUser?.metadata
                if (metadata != null && metadata.creationTimestamp != 0L) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    txtDate.text = sdf.format(Date(metadata.creationTimestamp))
                } else {
                    txtDate.text = "Date will appear here"
                }
            }
        }.addOnFailureListener { e ->
            Log.e("AccountSettings", "Failed to reach users database path: ${e.message}")
            Toast.makeText(requireContext(), "Failed to refresh user details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editUserInfo() {
        Toast.makeText(requireContext(), "An email has been sent to update your user info", Toast.LENGTH_SHORT).show()
    }

    private fun changePassword() {
        val email = auth.currentUser?.email
        if (!email.isNullOrEmpty()) {
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Password reset email link sent successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error sending link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "No linked email account discovered.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearUserData() {
        val userId = auth.currentUser?.uid ?: return
        val rootRef = if (activity is MainActivity) (activity as MainActivity).envelopesRef.root else FirebaseDatabase.getInstance(databaseUrl).reference

        Toast.makeText(requireContext(), "Clearing dashboard repositories...", Toast.LENGTH_SHORT).show()

        rootRef.child("expenses").child(userId).removeValue()
        rootRef.child("envelopes").child(userId).removeValue()
        rootRef.child("income").child(userId).removeValue()
        rootRef.child("allocations").child(userId).removeValue()
        rootRef.child("monthlygoal").child(userId).removeValue()

        rootRef.child("users").child(userId).child("subscribed").setValue(false)
        rootRef.child("users").child(userId).child("achievements").setValue(0)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "All linked transactions wiped successfully!", Toast.LENGTH_LONG).show()
                loadUserInfo()
            }
    }
}
