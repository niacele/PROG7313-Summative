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

class Report : Fragment() {

    private lateinit var edtFilterEnvelopeDropdown: AutoCompleteTextView
    private lateinit var edtStartDate: EditText
    private lateinit var edtEndDate: EditText
    private lateinit var btnViewReport: Button
    private lateinit var resultsContainer: LinearLayout
    private lateinit var txtTotal: TextView
    private lateinit var btnBackButton: ImageButton

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)

        edtFilterEnvelopeDropdown = view.findViewById(R.id.edtFilterEnvelopeDropdown)
        edtStartDate = view.findViewById(R.id.edtStartDate)
        edtEndDate = view.findViewById(R.id.edtEndDate)
        btnViewReport = view.findViewById(R.id.btnViewReport)
        resultsContainer = view.findViewById(R.id.resultsContainer)
        txtTotal = view.findViewById(R.id.txtTotal)
        btnBackButton = view.findViewById(R.id.btnBackButton)

        // Load envelopes into dropdown
        loadEnvelopes()

        // Date pickers
        edtStartDate.setOnClickListener { showDatePicker(edtStartDate) }
        edtEndDate.setOnClickListener { showDatePicker(edtEndDate) }

        // View report
        btnViewReport.setOnClickListener { viewReport() }

        // Back button
        btnBackButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

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
            edtFilterEnvelopeDropdown.setAdapter(adapter)
        }
    }

    private fun showDatePicker(target: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(),
            { _, year, month, day ->
                target.setText("$year-${month + 1}-$day")
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun viewReport() {
        val userId = auth.currentUser?.uid ?: return
        val envelopeFilter = edtFilterEnvelopeDropdown.text.toString().trim()
        val startDateStr = edtStartDate.text.toString().trim()
        val endDateStr = edtEndDate.text.toString().trim()

        if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please select start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        val startDate = sdf.parse(startDateStr) ?: return
        val endDate = sdf.parse(endDateStr) ?: return

        val ref = db.getReference("expenses").child(userId)
        ref.get().addOnSuccessListener { snapshot ->
            resultsContainer.removeAllViews()
            var total = 0.0

            for (envSnap in snapshot.children) {
                val envelopeName = envSnap.key ?: continue
                if (envelopeFilter.isNotEmpty() && envelopeFilter != envelopeName) continue

                for (expenseSnap in envSnap.children) {
                    val dateStr = expenseSnap.child("date").getValue(String::class.java) ?: continue
                    val amount = expenseSnap.child("amount").getValue(Double::class.java) ?: 0.0
                    val desc = expenseSnap.child("description").getValue(String::class.java) ?: ""

                    val expDate = sdf.parse(dateStr) ?: continue
                    if (expDate.after(startDate) && expDate.before(endDate)) {
                        total += amount

                        // Add row to resultsContainer
                        val row = TextView(requireContext())
                        row.text = "$dateStr - $envelopeName: $desc (R%.2f)".format(amount)
                        resultsContainer.addView(row)
                    }
                }
            }

            txtTotal.text = "R%.2f".format(total)
        }
    }
}