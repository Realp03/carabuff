package com.example.carabuff

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnBack: ImageView

    private lateinit var btnChipProtein: Button
    private lateinit var btnChipWeight: Button
    private lateinit var btnChipWorkout: Button
    private lateinit var btnChipCalories: Button
    private lateinit var btnChipHydration: Button

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private var isSending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerChat = findViewById(R.id.recyclerChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)

        btnChipProtein = findViewById(R.id.btnChipProtein)
        btnChipWeight = findViewById(R.id.btnChipWeight)
        btnChipWorkout = findViewById(R.id.btnChipWorkout)
        btnChipCalories = findViewById(R.id.btnChipCalories)
        btnChipHydration = findViewById(R.id.btnChipHydration)

        adapter = ChatAdapter(messages)
        recyclerChat.layoutManager = LinearLayoutManager(this)
        recyclerChat.adapter = adapter

        addBotMessage("Hi! I’m Carabuff 🐃 Ask me about food, workouts, calories, healthy habits, or your progress.")

        selectChip(btnChipProtein)

        btnBack.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("from_navbar", true)
            }
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        btnSend.setOnClickListener {
            sendMessage()
        }

        btnChipProtein.setOnClickListener {
            selectChip(btnChipProtein)
            sendPresetMessage("How can I increase my protein intake?")
        }

        btnChipWeight.setOnClickListener {
            selectChip(btnChipWeight)
            sendPresetMessage("Give me a weight loss plan.")
        }

        btnChipWorkout.setOnClickListener {
            selectChip(btnChipWorkout)
            sendPresetMessage("Suggest a workout for today.")
        }

        btnChipCalories.setOnClickListener {
            selectChip(btnChipCalories)
            sendPresetMessage("How many calories should I eat per day?")
        }

        btnChipHydration.setOnClickListener {
            selectChip(btnChipHydration)
            sendPresetMessage("How much water should I drink daily?")
        }

        etMessage.setOnEditorActionListener { _, actionId, event ->
            val isEnterPressed =
                event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.action == KeyEvent.ACTION_DOWN

            if (actionId == EditorInfo.IME_ACTION_SEND || isEnterPressed) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun selectChip(selected: Button) {
        val chips = listOf(
            btnChipProtein,
            btnChipWeight,
            btnChipWorkout,
            btnChipCalories,
            btnChipHydration
        )

        chips.forEach { chip ->
            chip.setBackgroundResource(R.drawable.bg_chat_chip_default)
            chip.alpha = 0.85f
        }

        selected.setBackgroundResource(R.drawable.bg_chat_chip_selected)
        selected.alpha = 1.0f
    }

    private fun sendPresetMessage(text: String) {
        if (isSending) return
        etMessage.setText(text)
        sendMessage()
    }

    private fun sendMessage() {
        if (isSending) return

        val userText = etMessage.text.toString().trim()
        if (userText.isEmpty()) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        addUserMessage(userText)
        etMessage.setText("")
        addBotMessage("Typing...")

        setSendingState(true)

        CarabuffApi.askCarabuff(
            message = userText,
            userId = userId,
            onSuccess = { reply ->
                runOnUiThread {
                    removeTyping()
                    addBotMessage(reply)
                    setSendingState(false)
                }
            },
            onError = { error ->
                runOnUiThread {
                    removeTyping()
                    addBotMessage("Sorry, may problem sa connection right now 😢")
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    setSendingState(false)
                }
            }
        )
    }

    private fun setSendingState(sending: Boolean) {
        isSending = sending

        btnSend.isEnabled = !sending
        etMessage.isEnabled = !sending

        btnChipProtein.isEnabled = !sending
        btnChipWeight.isEnabled = !sending
        btnChipWorkout.isEnabled = !sending
        btnChipCalories.isEnabled = !sending
        btnChipHydration.isEnabled = !sending

        btnSend.alpha = if (sending) 0.6f else 1.0f
        etMessage.alpha = if (sending) 0.7f else 1.0f

        val chipAlpha = if (sending) 0.6f else 1.0f
        btnChipProtein.alpha = chipAlpha
        btnChipWeight.alpha = chipAlpha
        btnChipWorkout.alpha = chipAlpha
        btnChipCalories.alpha = chipAlpha
        btnChipHydration.alpha = chipAlpha

        if (!sending) {
            val chips = listOf(
                btnChipProtein,
                btnChipWeight,
                btnChipWorkout,
                btnChipCalories,
                btnChipHydration
            )

            val selectedChip = chips.firstOrNull { it.alpha == 1.0f }
            if (selectedChip != null) {
                selectedChip.alpha = 1.0f
            }
        }
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text, true))
        adapter.notifyItemInserted(messages.lastIndex)
        scrollToBottom()
    }

    private fun addBotMessage(text: String) {
        messages.add(ChatMessage(text, false))
        adapter.notifyItemInserted(messages.lastIndex)
        scrollToBottom()
    }

    private fun removeTyping() {
        if (
            messages.isNotEmpty() &&
            messages.last().text == "Typing..." &&
            !messages.last().isUser
        ) {
            val index = messages.lastIndex
            messages.removeAt(index)
            adapter.notifyItemRemoved(index)
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        recyclerChat.post {
            if (messages.isNotEmpty()) {
                recyclerChat.scrollToPosition(messages.lastIndex)
            }
        }
    }
}