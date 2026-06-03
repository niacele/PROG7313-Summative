package com.example.mybudgetbuddysummative

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class MonthlyGoal : Fragment() {

    private lateinit var edtGoalAmount: EditText
    private lateinit var edtGoalCategoryDropdown: AutoCompleteTextView
    private lateinit var edtGoalDate: EditText
    private lateinit var btnSaveGoal: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_monthly_goal, container, false)

        // Bind views
        edtGoalAmount = view.findViewById(R.id.edtGoalAmount)
        edtGoalCategoryDropdown = view.findViewById(R.id.edtGoalCategoryDropdown)
        edtGoalDate = view.findViewById(R.id.edtGoalDate)
        btnSaveGoal = view.findViewById(R.id.btnSaveGoal)

        // Load envelopes into dropdown
        loadEnvelopes()

        // Date picker for month/date
        edtGoalDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(),
                { _, year, month, day ->
                    // Save as YYYY-MM format (monthly goal)
                    edtGoalDate.setText("$year-${month + 1}")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Save goal
        btnSaveGoal.setOnClickListener { saveGoal() }

        return view
    }

    private fun loadEnvelopes() {
        val userId = auth.currentUser?.uid ?: return
        val ref = db.getReference("envelopes").child(userId)

        ref.get().addOnSuccessListener { snapshot ->
            val envelopeNames = mutableListOf<String>()
            for (envSnap in snapshot.children) {
                val name = envSnap.child("name").getValue(String::class.java)
                if (name != null) envelopeNames.add(name)
            }

            val adapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_dropdown_item_1line, envelopeNames)
            edtGoalCategoryDropdown.setAdapter(adapter)
        }
    }

    private fun saveGoal() {
        val userId = auth.currentUser?.uid ?: return
        val amount = edtGoalAmount.text.toString().toDoubleOrNull()
        val envelopeName = edtGoalCategoryDropdown.text.toString().trim()
        val targetMonth = edtGoalDate.text.toString().trim()

        if (amount == null || envelopeName.isEmpty() || targetMonth.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val envelopesRef = db.getReference("envelopes").child(userId)
        envelopesRef.get().addOnSuccessListener { snapshot ->
            var envelopeId: String? = null
            for (envSnap in snapshot.children) {
                val name = envSnap.child("name").getValue(String::class.java)
                if (name == envelopeName) {
                    envelopeId = envSnap.key
                    break
                }
            }

            if (envelopeId == null) {
                Toast.makeText(requireContext(), "Envelope not found", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val goalId = db.getReference("monthlyGoals").child(userId).child(envelopeId).push().key!!
            val goalMap = mapOf(
                "id" to goalId,
                "userId" to userId,
                "envelopeId" to envelopeId,
                "envelopeName" to envelopeName,
                "targetAmount" to amount,
                "targetMonth" to targetMonth
            )

            db.getReference("monthlyGoals").child(userId).child(envelopeId).child(goalId).setValue(goalMap)
                .addOnSuccessListener {
                    Achievements.awardAchievement("firstBudget")
                    Toast.makeText(requireContext(), "Monthly goal saved", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
