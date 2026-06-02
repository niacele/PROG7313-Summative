package com.example.mybudgetbuddysummative

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class Envelope : Fragment() {
    private lateinit var edtEnvelopeName: EditText
    private lateinit var edtAmount: EditText
    private lateinit var rgRecurring: RadioGroup
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton
    private lateinit var colorPaletteContainer: LinearLayout


    private val dbRef = FirebaseDatabase.getInstance().getReference("envelopes")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var selectedColor: String = "#4A6984"
    private var isRecurring: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_envelope, container, false)

        //typecasting
        edtEnvelopeName = view.findViewById(R.id.edtEnvelopeName)
        edtAmount = view.findViewById(R.id.edtAmount)
        rgRecurring = view.findViewById(R.id.rgRecurring)
        btnSave = view.findViewById(R.id.btnSave)
        colorPaletteContainer = view.findViewById(R.id.colorPaletteContainer)

        // reoccuring
        rgRecurring.setOnCheckedChangeListener { _, checkedId ->
            isRecurring = checkedId == R.id.rbYes
        }

        //colours
        for (i in 0 until colorPaletteContainer.childCount) {
            val child = colorPaletteContainer.getChildAt(i)
            if (child is CheckBox) {
                child.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        // Uncheck others
                        for (j in 0 until colorPaletteContainer.childCount) {
                            val other = colorPaletteContainer.getChildAt(j)
                            if (other is CheckBox && other != buttonView) {
                                other.isChecked = false
                            }
                        }
                        // Save selected color
                        val bg = child.background
                        if (bg != null) {
                            // fallback: use hardcoded hex from XML
                            selectedColor = (child.background as? android.graphics.drawable.ColorDrawable)?.color?.let {
                                String.format("#%06X", 0xFFFFFF and it)
                            } ?: "#4A6984"
                        }
                    }
                }
            }
        }

        // Save button
        btnSave.setOnClickListener {
            saveEnvelope()
        }

        return view
    }

    private fun saveEnvelope() {
        val name = edtEnvelopeName.text.toString().trim()
        val amountText = edtAmount.text.toString().trim()
        val amount = amountText.toDoubleOrNull()

        if (name.isEmpty() || amount == null) {
            Toast.makeText(requireContext(), "Please enter valid name and amount", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("envelopes")
        val envelopeId = dbRef.push().key!!

        //envelope
        val envelopeMap = mapOf(
            "id" to envelopeId,
            "userId" to userId,
            "name" to name,
            "totalAmount" to amount,
            "recurring" to isRecurring,
            "colorHex" to selectedColor
        )

        dbRef.child(userId).child(envelopeId).setValue(envelopeMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Envelope saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}