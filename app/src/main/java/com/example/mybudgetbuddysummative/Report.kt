package com.example.mybudgetbuddysummative

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import java.util.*

class Report : Fragment() {

    private lateinit var btnBackButton: ImageButton
    private lateinit var txtExpenseReport: TextView
    private lateinit var resultsContainer: LinearLayout
    private lateinit var edtStartDate: EditText
    private lateinit var edtEndDate: EditText
    private lateinit var txtTotal: TextView
    private lateinit var btnViewReport: Button

    private lateinit var btnAccountButton: ImageButton
    private lateinit var btnAddExpense: ImageButton
    private lateinit var btnEnvelope: ImageButton

    private lateinit var dbRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)

        // Bind views
        btnBackButton = view.findViewById(R.id.btnBackButton)
        txtExpenseReport = view.findViewById(R.id.txtExpenseReport)
        edtStartDate = view.findViewById(R.id.edtStartDate)
        edtEndDate = view.findViewById(R.id.edtEndDate)
        txtTotal = view.findViewById(R.id.txtTotal)
        btnViewReport = view.findViewById(R.id.btnViewReport)
        btnAccountButton = view.findViewById(R.id.btnAccountButton)
        btnAddExpense = view.findViewById(R.id.btnAddExpense)
        btnEnvelope = view.findViewById(R.id.btnEnvelope)
        resultsContainer = view.findViewById(R.id.resultsContainer)

        // Firebase Realtime Database reference
        dbRef = FirebaseDatabase.getInstance().getReference("expenses")

        // Date pickers
        edtStartDate.setOnClickListener { showDatePicker(edtStartDate) }
        edtEndDate.setOnClickListener { showDatePicker(edtEndDate) }

        // Buttons
        btnBackButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Home())
                .commit()
        }

        btnViewReport.setOnClickListener { viewReport() }

        btnAccountButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Settings())
                .commit()
        }

        btnAddExpense.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Expense())
                .commit()
        }

        btnEnvelope.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Envelope())
                .commit()
        }

        return view
    }

    private fun showDatePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDay)
                val selectedDate = "$selectedYear-$formattedMonth-$formattedDay"
                targetEditText.setText(selectedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun viewReport() {
        val startDate = edtStartDate.text.toString().trim()
        val endDate = edtEndDate.text.toString().trim()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter both a start and end date", Toast.LENGTH_SHORT).show()
            return
        }

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                resultsContainer.removeAllViews()
                var totalAmount = 0.0

                if (!snapshot.exists()) {
                    txtTotal.text = "Total: R0.00"
                    val noResultsText = TextView(requireContext())
                    noResultsText.text = "No expenses found"
                    noResultsText.textSize = 16f
                    noResultsText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    resultsContainer.addView(noResultsText)
                    return
                }

                for (expenseSnap in snapshot.children) {
                    val category = expenseSnap.child("category").getValue(String::class.java) ?: "Unknown"
                    val amount = expenseSnap.child("amount").getValue(Double::class.java) ?: 0.0
                    val date = expenseSnap.child("date").getValue(String::class.java) ?: ""
                    val description = expenseSnap.child("description").getValue(String::class.java) ?: ""
                    val photoUri = expenseSnap.child("photoUri").getValue(String::class.java)

                    // Filter by date range
                    if (date >= startDate && date <= endDate) {
                        val expenseText = TextView(requireContext())
                        expenseText.text =
                            "Category: $category\n" +
                                    "Amount: R${"%.2f".format(amount)}\n" +
                                    "Date: $date\n" +
                                    "Description: $description\n" +
                                    "__________________________________"
                        expenseText.textSize = 16f
                        expenseText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        resultsContainer.addView(expenseText)

                        totalAmount += amount

                        if (photoUri != null) {
                            val imageView = ImageView(requireContext())
                            imageView.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            imageView.adjustViewBounds = true
                            imageView.maxHeight = 800
                            imageView.scaleType = ImageView.ScaleType.FIT_CENTER

                            try {
                                imageView.setImageURI(Uri.parse(photoUri))
                                resultsContainer.addView(imageView)
                            } catch (e: Exception) {
                                val imageErrorText = TextView(requireContext())
                                imageErrorText.text = "Image could not be loaded"
                                imageErrorText.textSize = 14f
                                imageErrorText.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                                resultsContainer.addView(imageErrorText)
                            }
                        }
                    }
                }
                txtTotal.text = "Total: R%.2f".format(totalAmount)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
