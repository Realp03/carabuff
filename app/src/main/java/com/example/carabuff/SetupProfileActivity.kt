package com.example.carabuff

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SetupProfileActivity : AppCompatActivity() {

    private lateinit var birthdayInput: EditText
    private lateinit var ageInput: EditText

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0

    companion object {
        private const val MIN_AGE = 14
        private const val MAX_AGE = 99
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_profile)

        val nameInput = findViewById<EditText>(R.id.nameInput)
        birthdayInput = findViewById(R.id.birthdayInput)
        ageInput = findViewById(R.id.ageInput)
        val genderSpinner = findViewById<Spinner>(R.id.genderSpinner)

        val nextBtn = findViewById<Button>(R.id.nextBtn)
        val prevBtn = findViewById<Button>(R.id.prevBtn)

        prevBtn.isEnabled = false
        ageInput.isEnabled = false

        // 🔥 SPINNER DATA
        val genderList = arrayOf("Male", "Female", "Other")
        genderSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genderList)

        // 🎂 BIRTHDAY PICKER
        birthdayInput.setOnClickListener {
            showDatePicker()
        }

        // 🚀 NEXT BUTTON
        nextBtn.setOnClickListener {

            val name = nameInput.text.toString().trim()
            val birthday = birthdayInput.text.toString().trim()
            val ageText = ageInput.text.toString().trim()
            val gender = genderSpinner.selectedItem.toString()

            if (name.isEmpty() || birthday.isEmpty() || ageText.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageText.toIntOrNull()
            if (age == null) {
                Toast.makeText(this, "Invalid age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (age < MIN_AGE) {
                Toast.makeText(
                    this,
                    "Minimum age to use this app is $MIN_AGE years old",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (age > MAX_AGE) {
                Toast.makeText(
                    this,
                    "Maximum age allowed is $MAX_AGE years old",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val intent = Intent(this, Step2Activity::class.java)
            intent.putExtra("name", name)
            intent.putExtra("birthday", birthday)
            intent.putExtra("age", age.toString())
            intent.putExtra("gender", gender)

            startActivity(intent)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, y, m, d ->

                selectedYear = y
                selectedMonth = m
                selectedDay = d

                val age = calculateAge(y, m, d)

                if (age < MIN_AGE) {
                    birthdayInput.setText("")
                    ageInput.setText("")
                    Toast.makeText(
                        this,
                        "You must be at least $MIN_AGE years old to use this app",
                        Toast.LENGTH_LONG
                    ).show()
                    return@DatePickerDialog
                }

                if (age > MAX_AGE) {
                    birthdayInput.setText("")
                    ageInput.setText("")
                    Toast.makeText(
                        this,
                        "Maximum age allowed is $MAX_AGE years old",
                        Toast.LENGTH_LONG
                    ).show()
                    return@DatePickerDialog
                }

                val formatted = "${m + 1}/$d/$y"
                birthdayInput.setText(formatted)
                ageInput.setText(age.toString())
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }

    private fun calculateAge(year: Int, month: Int, day: Int): Int {
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - year

        if (today.get(Calendar.MONTH) < month ||
            (today.get(Calendar.MONTH) == month && today.get(Calendar.DAY_OF_MONTH) < day)
        ) {
            age--
        }

        return age
    }
}