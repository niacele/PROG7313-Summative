package com.example.mybudgetbuddysummative

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class Expense : Fragment() {

    private lateinit var edtCategory: EditText
    private lateinit var edtAmount: EditText
    private lateinit var edtDescription: EditText
    private lateinit var edtDate: EditText
    private lateinit var btnAddImage: MaterialButton
    private lateinit var btnSave: Button

    private var selectedPhotoUri: String? = null

    private val dbRef = FirebaseDatabase.getInstance().getReference("expenses")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expense, container, false)

        // Bind views
        edtCategory = view.findViewById(R.id.edtCategory)
        edtAmount = view.findViewById(R.id.edtAmount)
        edtDescription = view.findViewById(R.id.edtDescription)
        edtDate = view.findViewById(R.id.edtDate)
        btnAddImage = view.findViewById(R.id.btnAddImage)
        btnSave = view.findViewById(R.id.btnSave)

        // Button listeners
        btnAddImage.setOnClickListener {
            Toast.makeText(requireContext(), "Add a photo", Toast.LENGTH_SHORT).show()
            pickImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            saveExpense()
        }

        edtDate.setOnClickListener {
            showDatePicker()
        }


        return view
    }

    private fun saveExpense() {
        val category = edtCategory.text.toString().trim()
        val amountTxt = edtAmount.text.toString().trim()
        val date = edtDate.text.toString().trim()
        val description = edtDescription.text.toString().trim()

        if (category.isEmpty() || amountTxt.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountTxt.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Create expense object
        val expenseId = dbRef.push().key!!  // unique ID
        val expense = mapOf(
            "id" to expenseId,
            "category" to category,
            "amount" to amount,
            "date" to date,
            "description" to description,
            "photoUri" to selectedPhotoUri
        )

        // Save to Firebase
        dbRef.child(expenseId).setValue(expense)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Expense saved successfully", Toast.LENGTH_SHORT).show()
                clearFields()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save expense: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDatePicker() {
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
                edtDate.setText(selectedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri.toString()
            Toast.makeText(requireContext(), "Photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFields() {
        edtCategory.text.clear()
        edtAmount.text.clear()
        edtDescription.text.clear()
        edtDate.text.clear()
        selectedPhotoUri = null
    }
}
