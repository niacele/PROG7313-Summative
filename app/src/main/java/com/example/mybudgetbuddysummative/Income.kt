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
import java.text.SimpleDateFormat
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

    private val databaseUrl = "https://mybudgetbuddysum-default-rtdb.firebaseio.com/"
    private val db = FirebaseDatabase.getInstance(databaseUrl)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_income, container, false)

        edtIncomeAmount = view.findViewById(R.id.edtIncomeAmount)
        edtIncomeSource = view.findViewById(R.id.edtIncomeSource)
        edtIncomeDate = view.findViewById(R.id.edtIncomeDate)
        btnSaveIncome = view.findViewById(R.id.btnSaveIncome)

        edtAllocationAmount = view.findViewById(R.id.edtAllocationAmount)
        edtAllocationEnvelopeDropdown = view.findViewById(R.id.edtAllocationEnvelopeDropdown)
        btnSaveAllocation = view.findViewById(R.id.btnSaveAllocation)

        edtIncomeDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(),
                { _, year, month, day ->
                    // UNIFIED FIX: Format with zero padding matching yyyy-MM-dd schema properties
                    val formattedMonth = String.format("%02d", month + 1)
                    val formattedDay = String.format("%02d", day)
                    edtIncomeDate.setText("$year-$formattedMonth-$formattedDay")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        loadEnvelopes()

        btnSaveIncome.setOnClickListener { saveIncome() }
        btnSaveAllocation.setOnClickListener { saveAllocation() }

        return view
    }

    private fun loadEnvelopes() {
        val userId = auth.currentUser?.uid ?: "test_development_user"
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
        val userId = auth.currentUser?.uid ?: "test_development_user"
        val amount = edtIncomeAmount.text.toString().toDoubleOrNull()
        val source = edtIncomeSource.text.toString().trim()
        val date = edtIncomeDate.text.toString().trim()

        if (amount == null || amount <= 0 || source.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields with positive values", Toast.LENGTH_SHORT).show()
            return
        }

        val incomeId = db.getReference("income").child(userId).push().key!!
        val incomeMap = mapOf(
            "id" to incomeId,
            "userId" to userId,
            "amount" to amount,
            "source" to source,
            "date" to date
        )

        db.getReference("income").child(userId).child(incomeId).setValue(incomeMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Income saved successfully!", Toast.LENGTH_SHORT).show()
                edtIncomeAmount.text.clear()
                edtIncomeSource.text.clear()
                edtIncomeDate.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAllocation() {
        val userId = auth.currentUser?.uid ?: "test_development_user"
        val amount = edtAllocationAmount.text.toString().toDoubleOrNull()
        val envelopeName = edtAllocationEnvelopeDropdown.text.toString().trim()

        if (amount == null || amount <= 0 || envelopeName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter positive amount and select envelope", Toast.LENGTH_SHORT).show()
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

            val allocationId = db.getReference("allocations").child(userId).child(envelopeId).push().key!!

            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val allocationMap = mapOf(
                "id" to allocationId,
                "userId" to userId,
                "amount" to amount,
                "source" to envelopeName,
                "date" to dateStr
            )

            db.getReference("allocations").child(userId).child(envelopeId).child(allocationId).setValue(allocationMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Allocation saved successfully!", Toast.LENGTH_SHORT).show()

                    // Reset field variables using zero-filtering methods safely
                    edtAllocationAmount.text.clear()
                    edtAllocationEnvelopeDropdown.setText("", false)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}