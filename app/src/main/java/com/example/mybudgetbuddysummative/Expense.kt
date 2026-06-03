package com.example.mybudgetbuddysummative

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class Expense : Fragment() {

    private lateinit var edtCategory: AutoCompleteTextView
    private lateinit var edtAmount: TextInputEditText
    private lateinit var edtDescription: TextInputEditText
    private lateinit var edtDate: TextInputEditText
    private lateinit var btnAddImage: MaterialButton
    private lateinit var btnSave: Button
    private lateinit var btnBackButton: ImageButton
    private lateinit var btnAddIncome: Button

    private var selectedPhotoUri: String? = null

    private val auth = FirebaseAuth.getInstance()
    private lateinit var expensesRef: com.google.firebase.database.DatabaseReference
    private lateinit var envelopesRef: com.google.firebase.database.DatabaseReference

    private val envelopeNamesList = ArrayList<String>()
    private lateinit var dropdownAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expense, container, false)

        edtCategory = view.findViewById(R.id.edtCategory)
        edtAmount = view.findViewById(R.id.edtAmount)
        edtDescription = view.findViewById(R.id.edtDescription)
        edtDate = view.findViewById(R.id.edtDate)
        btnAddImage = view.findViewById(R.id.btnAddImage)
        btnSave = view.findViewById(R.id.btnSave)
        btnBackButton = view.findViewById(R.id.btnBackButton)
        btnAddIncome = view.findViewById(R.id.btnAddIncome)

        dropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, envelopeNamesList)
        edtCategory.setAdapter(dropdownAdapter)

        btnAddImage.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Media Library...", Toast.LENGTH_SHORT).show()
            pickImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            saveExpense()
        }

        edtDate.setOnClickListener {
            showDatePicker()
        }

        btnBackButton.setOnClickListener {
            returnToHomeView()
        }

        btnAddIncome.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Income())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is MainActivity) {
            val mainAct = activity as MainActivity
            envelopesRef = mainAct.envelopesRef
            expensesRef = mainAct.envelopesRef.root.child("expenses")
        } else {
            val databaseUrl = "https://firebaseio.com"
            val databaseInstance = FirebaseDatabase.getInstance(databaseUrl)
            envelopesRef = databaseInstance.getReference("envelopes")
            expensesRef = databaseInstance.getReference("expenses")
        }

        loadUserEnvelopesIntoDropdown()
    }

    private fun loadUserEnvelopesIntoDropdown() {
        val userId = auth.currentUser?.uid ?: "test_development_user"

        envelopesRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                envelopeNamesList.clear()
                if (snapshot.exists()) {
                    for (envelopeSnapshot in snapshot.children) {
                        val envelopeName = envelopeSnapshot.child("name").getValue(String::class.java)
                        if (envelopeName != null) {
                            envelopeNamesList.add(envelopeName)
                        }
                    }
                }
                dropdownAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ExpenseDebug", "Realtime data pipeline failed: ${error.message}")
            }
        })
    }

    private fun saveExpense() {
        val chosenEnvelope = edtCategory.text.toString().trim()
        val amountTxt = edtAmount.text.toString().trim()
        val date = edtDate.text.toString().trim()
        val description = edtDescription.text.toString().trim()

        if (chosenEnvelope.isEmpty() || amountTxt.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please select an envelope and complete all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountTxt.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid positive amount", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: "test_development_user"
        btnSave.isEnabled = false

        val userRootRef = envelopesRef.root.child("users").child(userId)
        userRootRef.child("subscribed").get().addOnSuccessListener { subSnapshot ->
            val isSubscribed = subSnapshot.getValue(Boolean::class.java) ?: false

            if (!isSubscribed && envelopeNamesList.size > 3) {
                btnSave.isEnabled = true
                AlertDialog.Builder(requireContext())
                    .setTitle("Premium Required")
                    .setMessage("Free accounts are limited to a maximum of 3 custom budget categories. Upgrade to Premium to manage unlimited envelopes!")
                    .setPositiveButton("Upgrade") { _, _ ->
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, SubscriptionTwo())
                            .addToBackStack(null)
                            .commit()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                executeExpenseSave(userId, chosenEnvelope, amount, date, description)
            }
        }.addOnFailureListener {
            btnSave.isEnabled = true
            executeExpenseSave(userId, chosenEnvelope, amount, date, description)
        }
    }

    private fun executeExpenseSave(userId: String, chosenEnvelope: String, amount: Double, date: String, description: String) {
        val expenseId = expensesRef.child(userId).push().key!!

        val expenseMap = mapOf(
            "id" to expenseId,
            "userId" to userId,
            "envelope" to chosenEnvelope,
            "amount" to amount,
            "date" to date,
            "description" to description,
            "photoUri" to selectedPhotoUri
        )

        expensesRef.child(userId).child(expenseId).setValue(expenseMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                Achievements.awardAchievement("firstBudget")
                Achievements.awardAchievement("expenseTracker")
                clearFields()
                btnSave.isEnabled = true
                returnToHomeView()
            }
            .addOnFailureListener { e ->
                btnSave.isEnabled = true
                Toast.makeText(requireContext(), "Failed to save record: ${e.message}", Toast.LENGTH_LONG).show()
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
            try {
                // FIXED REMEDY: Force Android to grant permanent, persistent read access rights over this local file
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)

                selectedPhotoUri = uri.toString()
                Toast.makeText(requireContext(), "Image receipt reference attached permanently!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Slicing edge-case fallback if persistable tokens aren't available on a legacy test device
                selectedPhotoUri = uri.toString()
                Log.w("ExpenseDebug", "Persistable URI check skipped on this architecture: ${e.message}")
            }
        }
    }

    private fun clearFields() {
        edtCategory.setText("", false)
        edtAmount.text?.clear()
        edtDescription.text?.clear()
        edtDate.text?.clear()
        selectedPhotoUri = null
    }

    private fun returnToHomeView() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, Home())
            .commitAllowingStateLoss()

        if (activity is MainActivity) {
            val mainAct = activity as MainActivity
            mainAct.bottomNav.menu.findItem(R.id.nav_home).isChecked = true
        }
    }
}