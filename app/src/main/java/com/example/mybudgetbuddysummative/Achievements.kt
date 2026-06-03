package com.example.mybudgetbuddysummative

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
    private lateinit var btnBackButton: ImageButton

    private val auth = FirebaseAuth.getInstance()
    private val databaseUrl = "https://mybudgetbuddysum-default-rtdb.firebaseio.com/"
    private lateinit var dbRef: com.google.firebase.database.DatabaseReference

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

        btnBackButton = view.findViewById(R.id.btnBackButton)
        btnBackButton.setOnClickListener { parentFragmentManager.popBackStack() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is MainActivity) {
            dbRef = (activity as MainActivity).envelopesRef.root.child("users")
        } else {
            dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users")
        }

        loadAchievements()
    }

    private fun loadAchievements() {
        val userId = auth.currentUser?.uid ?: "test_development_user"

        dbRef.child(userId).child("unlockedAchievements").get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener

            val accentColor = try {
                ContextCompat.getColor(requireContext(), R.color.buddypink)
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#C57B7B") // Fallback Hex matching title branding
            }

            val activeColorTint = ColorStateList.valueOf(accentColor)

            snapshot.children.forEach { child ->
                val key = child.key

                when (key) {
                    "firstBudget" -> imgFirstBudget.imageTintList = activeColorTint
                    "firstCategory" -> imgFirstCategory.imageTintList = activeColorTint
                    "budgeting101" -> imgBudgeting101.imageTintList = activeColorTint
                    "budgetBuddies" -> imgBudgetBuddies.imageTintList = activeColorTint
                    "savingStreak" -> imgSavingStreak.imageTintList = activeColorTint
                    "expenseTracker" -> imgExpenseTracker.imageTintList = activeColorTint
                }
            }
        }.addOnFailureListener { e ->
            Log.e("Achievements", "Failed to load achievements data profile: ${e.message}")
        }
    }

    companion object {
        fun awardAchievement(achievementKey: String) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

            val userTableRef = FirebaseDatabase
                .getInstance("https://firebaseio.com")
                .getReference("users")
                .child(currentUserId)

            // 1. Check if badge is already earned to prevent writing loops
            userTableRef.child("unlockedAchievements").child(achievementKey).get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        val timestampDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())

                        // Mark as true/unlocked along with completion date details
                        userTableRef.child("unlockedAchievements").child(achievementKey).setValue(timestampDate)

                        // 2. Safely read and update total count for Account Info tracking views
                        userTableRef.child("achievements").get().addOnSuccessListener { countSnapshot ->
                            val existingCounterValue = countSnapshot.getValue(Int::class.java) ?: 0
                            userTableRef.child("achievements").setValue(existingCounterValue + 1)
                        }
                    }
                }
        }
    }
}
