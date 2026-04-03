package com.example.carabuff

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SetupProfileActivity : AppCompatActivity() {

    private lateinit var contentRoot: LinearLayout
    private lateinit var birthdayContainer: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var birthdayInput: EditText
    private lateinit var ageInput: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var prevBtn: Button
    private lateinit var nextBtn: Button

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_profile)
        overridePendingTransition(0, 0)

        contentRoot = findViewById(R.id.setupProfileContent)
        birthdayContainer = findViewById(R.id.birthdayContainer)
        nameInput = findViewById(R.id.nameInput)
        birthdayInput = findViewById(R.id.birthdayInput)
        ageInput = findViewById(R.id.ageInput)
        genderSpinner = findViewById(R.id.genderSpinner)
        prevBtn = findViewById(R.id.prevBtn)
        nextBtn = findViewById(R.id.nextBtn)

        setupGenderSpinner()
        restorePreviousInputs()
        setupBirthdayPickerTriggers()
        showContentEnterAnimation()

        prevBtn.setOnClickListener {
            animateContentExitLeft {
                finish()
                overridePendingTransition(0, 0)
            }
        }

        nextBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val gender = genderSpinner.selectedItem?.toString()?.trim().orEmpty()
            val birthday = birthdayInput.text.toString().trim()
            val ageText = ageInput.text.toString().trim()

            if (name.isEmpty()) {
                nameInput.error = "Please enter your name"
                nameInput.requestFocus()
                return@setOnClickListener
            }

            if (gender.equals("Select Gender", true)) {
                Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (birthday.isEmpty()) {
                Toast.makeText(this, "Please select your birthday", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ageText.isEmpty()) {
                Toast.makeText(this, "Age could not be calculated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageText.toIntOrNull()
            if (age == null) {
                Toast.makeText(this, "Invalid age value", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (age < 14 || age > 99) {
                Toast.makeText(this, "Allowed age is 14 to 99 years old only", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            animateContentExitLeft {
                val intent = Intent(this, Step2Activity::class.java).apply {
                    putExtra("name", name)
                    putExtra("gender", gender)
                    putExtra("birthday", birthday)
                    putExtra("age", age.toString())
                }
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }
    }

    private fun setupGenderSpinner() {
        val genderOptions = arrayOf(
            "Select Gender",
            "Male",
            "Female",
            "Prefer not to say"
        )

        val genderAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            genderOptions
        ) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = TextView(this@SetupProfileActivity)
                textView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                textView.text = genderOptions[position]
                textView.setTextColor(Color.parseColor("#111111"))
                textView.textSize = 15f
                textView.gravity = Gravity.CENTER_VERTICAL
                textView.setPadding(32, 0, 32, 0)
                textView.minHeight = dpToPx(54)
                return textView
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = TextView(this@SetupProfileActivity)

                textView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                textView.text = genderOptions[position]

                textView.setTextColor(Color.parseColor("#FFFFFF")) // text color
                textView.textSize = 15f

                textView.setBackgroundColor(Color.parseColor("#1C3557")) // 🔥 FIXED

                textView.gravity = Gravity.CENTER_VERTICAL
                textView.setPadding(32, 24, 32, 24)

                return textView
            }
        }

        genderSpinner.adapter = genderAdapter
    }

    private fun restorePreviousInputs() {
        val restoredName = intent.getStringExtra("name") ?: ""
        val restoredGender = intent.getStringExtra("gender") ?: ""
        val restoredBirthday = intent.getStringExtra("birthday") ?: ""
        val restoredAge = intent.getStringExtra("age") ?: ""

        if (restoredName.isNotEmpty()) {
            nameInput.setText(restoredName)
        }

        if (restoredBirthday.isNotEmpty()) {
            birthdayInput.setText(restoredBirthday)
        }

        if (restoredAge.isNotEmpty()) {
            ageInput.setText(restoredAge)
        }

        if (restoredGender.isNotEmpty()) {
            val genderOptions = listOf(
                "Select Gender",
                "Male",
                "Female",
                "Prefer not to say"
            )
            val genderIndex = genderOptions.indexOf(restoredGender)
            if (genderIndex >= 0) {
                genderSpinner.setSelection(genderIndex)
            }
        }

        if (restoredBirthday.isNotEmpty()) {
            try {
                val parser = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                val parsedDate = parser.parse(restoredBirthday)

                if (parsedDate != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = parsedDate
                    selectedYear = calendar.get(Calendar.YEAR)
                    selectedMonth = calendar.get(Calendar.MONTH)
                    selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun setupBirthdayPickerTriggers() {
        birthdayContainer.setOnClickListener {
            showWheelDatePicker()
        }

        birthdayInput.setOnClickListener {
            showWheelDatePicker()
        }
    }

    private fun showWheelDatePicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_wheel_date_picker, null)

        val dayPicker = dialogView.findViewById<NumberPicker>(R.id.dayPicker)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.yearPicker)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelDate)
        val btnDone = dialogView.findViewById<Button>(R.id.btnDoneDate)

        val months = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)

        val minYear = currentYear - 99
        val maxYear = currentYear - 14

        val initialYear = if (selectedYear in minYear..maxYear) selectedYear else maxYear
        val initialMonth = if (selectedMonth in 0..11) selectedMonth else 0
        val initialDay = if (selectedDay in 1..31) selectedDay else 1

        dayPicker.minValue = 1
        dayPicker.maxValue = 31
        dayPicker.wrapSelectorWheel = true

        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months
        monthPicker.wrapSelectorWheel = true

        yearPicker.minValue = minYear
        yearPicker.maxValue = maxYear
        yearPicker.wrapSelectorWheel = false

        yearPicker.value = initialYear
        monthPicker.value = initialMonth

        fun updateDayPickerMax() {
            val cal = Calendar.getInstance()
            cal.set(yearPicker.value, monthPicker.value, 1)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val currentDayValue = dayPicker.value
            dayPicker.maxValue = maxDay

            if (currentDayValue > maxDay) {
                dayPicker.value = maxDay
            }
        }

        updateDayPickerMax()
        dayPicker.value = initialDay.coerceAtMost(dayPicker.maxValue)

        monthPicker.setOnValueChangedListener { _, _, _ ->
            updateDayPickerMax()
        }

        yearPicker.setOnValueChangedListener { _, _, _ ->
            updateDayPickerMax()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDone.setOnClickListener {
            selectedDay = dayPicker.value
            selectedMonth = monthPicker.value
            selectedYear = yearPicker.value

            val selectedCalendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }

            val age = calculateAge(selectedYear, selectedMonth, selectedDay)
            if (age < 14 || age > 99) {
                Toast.makeText(
                    this,
                    "Allowed age is 14 to 99 years old only",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val displayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            birthdayInput.setText(displayFormat.format(selectedCalendar.time))
            ageInput.setText(age.toString())

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun calculateAge(year: Int, month: Int, day: Int): Int {
        val today = Calendar.getInstance()
        val birthDate = Calendar.getInstance().apply {
            set(year, month, day)
        }

        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)

        if (today.get(Calendar.MONTH) < birthDate.get(Calendar.MONTH) ||
            (today.get(Calendar.MONTH) == birthDate.get(Calendar.MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH) < birthDate.get(Calendar.DAY_OF_MONTH))
        ) {
            age--
        }

        return age
    }

    private fun showContentEnterAnimation() {
        contentRoot.alpha = 0f
        contentRoot.translationX = 120f

        contentRoot.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(260)
            .start()
    }

    private fun animateContentExitLeft(onEnd: () -> Unit) {
        contentRoot.animate()
            .alpha(0f)
            .translationX(-120f)
            .setDuration(220)
            .withEndAction {
                onEnd()
            }
            .start()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        animateContentExitLeft {
            super.onBackPressed()
            overridePendingTransition(0, 0)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}