package com.example.mybudgetbuddysummative

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class Achievements : Fragment() {
    private lateinit var imgFirstBudget: ImageView
    private lateinit var imgFirstCategory: ImageView
    private lateinit var imgBudgeting101: ImageView
    private lateinit var imgBudgetBuddies: ImageView
    private lateinit var imgSavingStreak: ImageView
    private lateinit var imgExpenseTracker: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().getReference("achievements")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_achievements, container, false)

        imgFirstBudget = view.findViewById(R.id.imgFirstBudget)
        imgFirstCategory = view.findViewById(R.id.imgFirstCategory)
        imgBudgeting101 = view.findViewById(R.id.imgBudgeting101)
        imgBudgetBuddies = view.findViewById(R.id.imgBudgetBuddies)
        imgSavingStreak = view.findViewById(R.id.imgSavingStreak)
        imgExpenseTracker = view.findViewById(R.id.imgExpenseTracker)

        loadAchievements()

        return view
    }

    private fun loadAchievements() {
        val userId = auth.currentUser?.uid ?: return
        dbRef.child(userId).get().addOnSuccessListener { snapshot ->
            val achievements = snapshot.value as? Map<String, Boolean> ?: return@addOnSuccessListener

            val pink = ContextCompat.getColor(requireContext(), R.color.buddypink)
            val tintList = ColorStateList.valueOf(pink)

            if (achievements["firstBudget"] == true) {
                imgFirstBudget.setImageTintList(tintList)
            }
            if (achievements["firstCategory"] == true) {
                imgFirstCategory.setImageTintList(tintList)
            }
            if (achievements["budgeting101"] == true) {
                imgBudgeting101.setImageTintList(tintList)
            }
            if (achievements["budgetBuddies"] == true) {
                imgBudgetBuddies.setImageTintList(tintList)
            }
            if (achievements["savingStreak"] == true) {
                imgSavingStreak.setImageTintList(tintList)
            }
            if (achievements["expenseTracker"] == true) {
                imgExpenseTracker.setImageTintList(tintList)
            }
        }
    }

    fun unlockAchievement(key: String) {
        val userId = auth.currentUser?.uid ?: return
        dbRef.child(userId).child(key).setValue(true)
    }
}