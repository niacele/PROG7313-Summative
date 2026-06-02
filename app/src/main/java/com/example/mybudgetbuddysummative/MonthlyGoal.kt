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

        val goalId = db.getReference("monthlyGoals").push().key!!
        val goalMap = mapOf(
            "id" to goalId,
            "userId" to userId,
            "envelopeName" to envelopeName,
            "targetAmount" to amount,
            "targetMonth" to targetMonth
        )

        db.getReference("monthlyGoals").child(userId).child(goalId).setValue(goalMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Monthly goal saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
