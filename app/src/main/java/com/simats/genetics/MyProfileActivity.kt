package com.simats.genetics

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.simats.genetics.databinding.ActivityMyProfileBinding
import com.simats.genetics.network.ApiClient
import com.simats.genetics.network.responses.MyProfileResponse
import com.simats.genetics.network.responses.ProfileDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyProfileActivity : AppCompatActivity() {

    private var cachedProfile: ProfileDto? = null
    private lateinit var binding: ActivityMyProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.editProfileButton.setOnClickListener {
            val p = cachedProfile
            if (p == null) {
                Toast.makeText(this, "Profile not loaded yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val i = Intent(this, EditProfileActivity::class.java)
            i.putExtra("FULL_NAME", p.fullName ?: "")
            i.putExtra("EMAIL", p.email ?: "")
            i.putExtra("PHONE", p.phone ?: "")
            i.putExtra("DOB", p.dob ?: "")        // keep as YYYY-MM-DD
            i.putExtra("GENDER", p.gender ?: "")
            i.putExtra("AGE", p.age ?: 0)
            startActivity(i)
        }

        loadProfile()
    }

    override fun onResume() {
        super.onResume()
        // Reload after coming back from EditProfileActivity
        loadProfile()
    }

    private fun loadProfile() {
        ApiClient.getApi(this).getMyProfile().enqueue(object : Callback<MyProfileResponse> {

            override fun onResponse(
                call: Call<MyProfileResponse>,
                response: Response<MyProfileResponse>
            ) {
                if (!response.isSuccessful) {
                    Log.e("MY_PROFILE", "HTTP ${response.code()} err=${response.errorBody()?.string()}")
                    Toast.makeText(this@MyProfileActivity, "HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }

                val body = response.body()
                if (body?.status == true && body.profile != null) {
                    cachedProfile = body.profile
                    bindProfile(body.profile)
                } else {
                    Toast.makeText(this@MyProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MyProfileResponse>, t: Throwable) {
                Log.e("MY_PROFILE", "FAIL ${t.message}", t)
                Toast.makeText(this@MyProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun bindProfile(p: ProfileDto) {
        binding.tvName.text = p.fullName ?: "-"

        val role = p.role ?: "User"
        val id = p.id ?: 0
        binding.tvUserId.text = "$role ID: #$id"

        val isFemale = (p.gender ?: "").lowercase() == "female"
        
        if (isFemale) {
            binding.ivProfileIcon.setImageResource(R.drawable.ic_female_avatar)
            binding.ivGenderIcon.setImageResource(R.drawable.ic_female_silhouette)
            binding.profileAvatarContainer.setBackgroundResource(0)
            binding.ivProfileIcon.layoutParams.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            binding.ivProfileIcon.layoutParams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            binding.ivProfileIcon.setPadding(0, 0, 0, 0)
            binding.ivProfileIcon.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            binding.ivProfileIcon.imageTintList = null
            binding.ivGenderIcon.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.text_secondary))
        } else {
            binding.ivProfileIcon.setImageResource(R.drawable.ic_person)
            binding.ivGenderIcon.setImageResource(R.drawable.ic_person)
            binding.profileAvatarContainer.setBackgroundResource(R.drawable.circular_light_blue_background)
            val size60dp = (60 * resources.displayMetrics.density).toInt()
            binding.ivProfileIcon.layoutParams.width = size60dp
            binding.ivProfileIcon.layoutParams.height = size60dp
            binding.ivProfileIcon.setPadding(0, 0, 0, 0)
            binding.ivProfileIcon.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            binding.ivProfileIcon.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.solidblue))
            binding.ivGenderIcon.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.text_secondary))
        }

        binding.tvEmailValue.text = p.email ?: "-"
        binding.tvPhoneValue.text = p.phone ?: "-"

        val dobText = p.dob ?: "-"
        val ageText = if (p.age != null && p.age > 0) " (${p.age} years old)" else ""
        binding.tvDobValue.text = dobText + ageText

        binding.tvGenderValue.text = p.gender ?: "-"
    }
}
