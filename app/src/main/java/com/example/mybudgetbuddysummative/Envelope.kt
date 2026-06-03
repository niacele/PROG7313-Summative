package com.example.mybudgetbuddysummative

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Envelope : Fragment() {
    private lateinit var edtEnvelopeName: TextInputEditText
    private lateinit var edtAmount: TextInputEditText
    private lateinit var rgRecurring: RadioGroup
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton
    private lateinit var colorPaletteContainer: LinearLayout

    private var selectedColor: String = "#4A6984"
    private var isRecurring: Boolean = false

    private val auth = FirebaseAuth.getInstance()
    private lateinit var dbRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_envelope, container, false)

        edtEnvelopeName = view.findViewById(R.id.edtEnvelopeName)
        edtAmount = view.findViewById(R.id.edtAmount)
        rgRecurring = view.findViewById(R.id.rgRecurring)
        btnSave = view.findViewById(R.id.btnSave)
        btnBack = view.findViewById(R.id.btnBack)
        colorPaletteContainer = view.findViewById(R.id.colorPaletteContainer)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is MainActivity) {
            dbRef = (activity as MainActivity).envelopesRef
        } else {
            val databaseUrl = "https://firebaseio.com"
            dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("envelopes")
        }

        rgRecurring.setOnCheckedChangeListener { _, checkedId ->
            isRecurring = checkedId == R.id.rbYes
        }

        setupColorPaletteLogic()

        btnSave.setOnClickListener {
            saveEnvelope()
        }

        btnBack.setOnClickListener {
            returnToHomeView()
        }
    }

    private fun setupColorPaletteLogic() {
        for (i in 0 until colorPaletteContainer.childCount) {
            val child = colorPaletteContainer.getChildAt(i)
            if (child is CheckBox) {
                // Now safely reads the newly added tag elements from XML
                val hexColor = child.tag?.toString() ?: "#4A6984"
                child.setBackgroundColor(Color.parseColor(hexColor))

                if (hexColor == selectedColor) {
                    child.isChecked = true
                    child.scaleX = 1.1f
                    child.scaleY = 1.1f
                    child.alpha = 1.0f
                } else {
                    child.isChecked = false
                    child.scaleX = 0.9f
                    child.scaleY = 0.9f
                    child.alpha = 0.5f // Fade unselected choices for better visual contrast
                }

                child.setOnClickListener { view ->
                    val clickedBox = view as CheckBox
                    for (j in 0 until colorPaletteContainer.childCount) {
                        val other = colorPaletteContainer.getChildAt(j)
                        if (other is CheckBox) {
                            if (other == clickedBox) {
                                other.isChecked = true
                                other.scaleX = 1.1f
                                other.scaleY = 1.1f
                                other.alpha = 1.0f
                                selectedColor = other.tag?.toString() ?: "#4A6984"
                            } else {
                                other.isChecked = false
                                other.scaleX = 0.9f
                                other.scaleY = 0.9f
                                other.alpha = 0.5f
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveEnvelope() {
        val name = edtEnvelopeName.text.toString().trim()
        val amountText = edtAmount.text.toString().trim()
        val amount = amountText.toDoubleOrNull()

        if (name.isEmpty() || amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid name and positive amount", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: "test_development_user"
        btnSave.isEnabled = false

        val userRootRef = dbRef.root.child("users").child(userId)
        userRootRef.child("subscribed").get().addOnSuccessListener { subSnapshot ->
            val isSubscribed = subSnapshot.getValue(Boolean::class.java) ?: false

            dbRef.child(userId).get().addOnSuccessListener { envelopeSnapshot ->
                val currentEnvelopeCount = envelopeSnapshot.childrenCount

                if (!isSubscribed && currentEnvelopeCount >= 3) {
                    btnSave.isEnabled = true

                    AlertDialog.Builder(requireContext())
                        .setTitle("Premium Limit Reached")
                        .setMessage("Free accounts are limited to 3 custom envelopes. Upgrade to Premium to unlock unlimited budget creation!")
                        .setPositiveButton("Upgrade") { _, _ ->
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, SubscriptionTwo())
                                .addToBackStack(null)
                                .commit()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    executeEnvelopeSave(userId, name, amount)
                }
            }.addOnFailureListener {
                btnSave.isEnabled = true
                executeEnvelopeSave(userId, name, amount)
            }
        }.addOnFailureListener {
            btnSave.isEnabled = true
            executeEnvelopeSave(userId, name, amount)
        }
    }

    private fun executeEnvelopeSave(userId: String, name: String, amount: Double) {
        val envelopeId = dbRef.child(userId).push().key

        if (envelopeId == null) {
            btnSave.isEnabled = true
            Toast.makeText(requireContext(), "Key Generation Error", Toast.LENGTH_SHORT).show()
            return
        }

        val envelopeMap = hashMapOf(
            "id" to envelopeId,
            "userId" to userId,
            "name" to name,
            "totalAmount" to amount,
            "recurring" to isRecurring,
            "colorHex" to selectedColor
        )

        dbRef.child(userId).child(envelopeId).setValue(envelopeMap)
            .addOnCompleteListener { task ->
                btnSave.isEnabled = true
                if (task.isSuccessful) {
                    Achievements.awardAchievement("firstCategory")
                    Toast.makeText(requireContext(), "Envelope Saved Successfully!", Toast.LENGTH_SHORT).show()
                    returnToHomeView()
                } else {
                    val errorException = task.exception
                    Toast.makeText(requireContext(), "Database Error: ${errorException?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun returnToHomeView() {
        if (activity is MainActivity) {
            (activity as MainActivity).bottomNav.selectedItemId = R.id.nav_home
        } else {
            parentFragmentManager.popBackStack()
        }
    }
}