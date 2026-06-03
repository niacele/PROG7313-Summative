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
    // ✅ Consistent path with AccountInfo
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")

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
        dbRef.child(userId).child("achievements").get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener

            val pink = ContextCompat.getColor(requireContext(), R.color.buddypink)
            val tintList = ColorStateList.valueOf(pink)

            snapshot.children.forEach { child ->
                val key = child.key
                val unlocked = child.getValue(Boolean::class.java) ?: false

                if (unlocked) {
                    when (key) {
                        "firstBudget" -> imgFirstBudget.setImageTintList(tintList)
                        "firstCategory" -> imgFirstCategory.setImageTintList(tintList)
                        "budgeting101" -> imgBudgeting101.setImageTintList(tintList)
                        "budgetBuddies" -> imgBudgetBuddies.setImageTintList(tintList)
                        "savingStreak" -> imgSavingStreak.setImageTintList(tintList)
                        "expenseTracker" -> imgExpenseTracker.setImageTintList(tintList)
                    }
                }
            }
        }
    }

    fun unlockAchievement(key: String) {
        val userId = auth.currentUser?.uid ?: return
        dbRef.child(userId).child("achievements").child(key).setValue(true)
    }
}
