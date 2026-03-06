package com.simats.genetics

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.requests.FeedbackCreateRequest
import com.simats.genetics.network.responses.FeedbackCreateResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedbackActivity : AppCompatActivity() {

    private var currentRating = 0
    private var selectedTypeView: View? = null
    private var selectedTypeValue: String = ""

    private lateinit var stars: List<ImageView>
    private lateinit var feedbackInput: EditText
    private lateinit var charCounter: TextView
    private lateinit var submitBtn: MaterialButton
    private lateinit var cancelBtn: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Stars
        stars = listOf(
            findViewById(R.id.star1),
            findViewById(R.id.star2),
            findViewById(R.id.star3),
            findViewById(R.id.star4),
            findViewById(R.id.star5)
        )

        stars.forEachIndexed { index, star ->
            star.setOnClickListener { updateRating(index + 1) }
        }

        // Feedback types (LinearLayouts)
        val typeBug = findViewById<LinearLayout>(R.id.type_bug)
        val typeFeature = findViewById<LinearLayout>(R.id.type_feature)
        val typeImprovement = findViewById<LinearLayout>(R.id.type_improvement)
        val typeCompliment = findViewById<LinearLayout>(R.id.type_compliment)
        val typeOther = findViewById<LinearLayout>(R.id.type_other)

        typeBug.setOnClickListener { selectType(it, "Bug Report") }
        typeFeature.setOnClickListener { selectType(it, "Feature Request") }
        typeImprovement.setOnClickListener { selectType(it, "Improvement") }
        typeCompliment.setOnClickListener { selectType(it, "Compliment") }
        typeOther.setOnClickListener { selectType(it, "Other") }

        feedbackInput = findViewById(R.id.feedback_input)
        charCounter = findViewById(R.id.char_counter)

        feedbackInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                charCounter.text = "$length/500 characters"
                charCounter.setTextColor(if (length > 500) Color.RED else Color.parseColor("#757575"))
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cancelBtn = findViewById(R.id.cancel_button)
        submitBtn = findViewById(R.id.submit_button)

        cancelBtn.setOnClickListener { finish() }

        submitBtn.setOnClickListener {
            val msg = feedbackInput.text.toString().trim()

            if (currentRating == 0) {
                Toast.makeText(this, "Please rate your experience", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedTypeView == null || selectedTypeValue.isEmpty()) {
                Toast.makeText(this, "Please select a feedback type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (msg.isEmpty()) {
                Toast.makeText(this, "Please enter your message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (msg.length > 500) {
                Toast.makeText(this, "Message should be within 500 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitFeedback(currentRating, selectedTypeValue, msg)
        }
    }

    private fun selectType(view: View, value: String) {
        selectedTypeView?.isSelected = false
        view.isSelected = true
        selectedTypeView = view
        selectedTypeValue = value
    }

    private fun updateRating(rating: Int) {
        currentRating = rating
        for (i in 0 until 5) {
            if (i < rating) stars[i].setImageResource(R.drawable.ic_star_filled)
            else stars[i].setImageResource(R.drawable.ic_star_border)
        }
    }

    private fun submitFeedback(rating: Int, feedbackType: String, message: String) {
        submitBtn.isEnabled = false
        submitBtn.alpha = 0.6f

        val req = FeedbackCreateRequest(
            rating = rating,
            feedbackType = feedbackType,
            message = message
        )

        ApiClient.getApi(this)
            .submitFeedback(req)
            .enqueue(object : Callback<FeedbackCreateResponse> {

                override fun onResponse(
                    call: Call<FeedbackCreateResponse>,
                    response: Response<FeedbackCreateResponse>
                ) {
                    submitBtn.isEnabled = true
                    submitBtn.alpha = 1f

                    if (!response.isSuccessful) {
                        Log.e("FEEDBACK", "HTTP ${response.code()} err=${response.errorBody()?.string()}")
                        Toast.makeText(this@FeedbackActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val body = response.body()
                    if (body?.status == true) {
                        Toast.makeText(this@FeedbackActivity, body.message ?: "Thank you!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@FeedbackActivity, FeedbackSuccessActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@FeedbackActivity, body?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FeedbackCreateResponse>, t: Throwable) {
                    submitBtn.isEnabled = true
                    submitBtn.alpha = 1f
                    Log.e("FEEDBACK", "FAIL ${t.message}", t)
                    Toast.makeText(this@FeedbackActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}