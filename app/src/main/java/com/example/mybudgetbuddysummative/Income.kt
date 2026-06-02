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

class Income : Fragment() {

    private lateinit var edtIncomeAmount: EditText
    private lateinit var edtIncomeSource: EditText
    private lateinit var edtIncomeDate: EditText
    private lateinit var btnSaveIncome: Button

    private lateinit var edtAllocationAmount: EditText
    private lateinit var edtAllocationEnvelopeDropdown: AutoCompleteTextView
    private lateinit var btnSaveAllocation: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_income, container, false)

        // Bind views
        edtIncomeAmount = view.findViewById(R.id.edtIncomeAmount)
        edtIncomeSource = view.findViewById(R.id.edtIncomeSource)
        edtIncomeDate = view.findViewById(R.id.edtIncomeDate)
        btnSaveIncome = view.findViewById(R.id.btnSaveIncome)

        edtAllocationAmount = view.findViewById(R.id.edtAllocationAmount)
        edtAllocationEnvelopeDropdown = view.findViewById(R.id.edtAllocationEnvelopeDropdown)
        btnSaveAllocation = view.findViewById(R.id.btnSaveAllocation)

        // Date picker for income date
        edtIncomeDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(),
                { _, year, month, day ->
                    edtIncomeDate.setText("$year-${month + 1}-$day")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Load envelopes into dropdown
        loadEnvelopes()

        // Save income
        btnSaveIncome.setOnClickListener { saveIncome() }

        // Save allocation
        btnSaveAllocation.setOnClickListener { saveAllocation() }

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
            edtAllocationEnvelopeDropdown.setAdapter(adapter)
        }
    }

    private fun saveIncome() {
        val userId = auth.currentUser?.uid ?: return
        val amount = edtIncomeAmount.text.toString().toDoubleOrNull()
        val source = edtIncomeSource.text.toString().trim()
        val date = edtIncomeDate.text.toString().trim()

        if (amount == null || source.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val incomeId = db.getReference("income").push().key!!
        val incomeMap = mapOf(
            "id" to incomeId,
            "userId" to userId,
            "amount" to amount,
            "source" to source,
            "date" to date
        )

        db.getReference("income").child(userId).child(incomeId).setValue(incomeMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Income saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAllocation() {
        val userId = auth.currentUser?.uid ?: return
        val amount = edtAllocationAmount.text.toString().toDoubleOrNull()
        val envelopeName = edtAllocationEnvelopeDropdown.text.toString().trim()

        if (amount == null || envelopeName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter amount and select envelope", Toast.LENGTH_SHORT).show()
            return
        }

        val allocationId = db.getReference("allocations").push().key!!
        val allocationMap = mapOf(
            "id" to allocationId,
            "userId" to userId,
            "amount" to amount,
            "envelopeName" to envelopeName,
            "date" to System.currentTimeMillis()
        )

        db.getReference("allocations").child(userId).child(allocationId).setValue(allocationMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Allocation saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
