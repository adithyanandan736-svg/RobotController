package com.robot.controller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import com.robot.controller.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var robotConnection: RobotConnection? = null
    private var bluetoothHelper: BluetoothHelper? = null
    private var ttsHelper: TtsHelper? = null
    private var useBluetooth = false
    private val statusHandler = Handler(Looper.getMainLooper())
    private var statusPolling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestRuntimePermissions()

        ttsHelper = TtsHelper(this) { ready ->
            if (!ready) {
                Toast.makeText(this, "Voice assistant unavailable on this device", Toast.LENGTH_SHORT).show()
            } else {
                ttsHelper?.speak(introMessage)
            }
        }

        setupConnectionSection()
        setupJoystick()
        setupArmButtons()
        setupHeadButtons()
        setupModeButtons()
        setupChatSection()
        applyPressAnimationToAllButtons(binding.root)
    }

    // Gives every button a satisfying little squash-and-release when tapped,
    // instead of the flat default press state — small touch that makes the
    // whole app feel more alive.
    private fun applyPressAnimationToAllButtons(view: android.view.View) {
        if (view is android.widget.Button) {
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN ->
                        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start()
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
                false // let the normal click still fire
            }
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyPressAnimationToAllButtons(view.getChildAt(i))
            }
        }
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val notGranted = perms.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
        }
    }

    // ---------------- CONNECTION ----------------

    private fun setupConnectionSection() {
        binding.radioConnType.setOnCheckedChangeListener { _, checkedId ->
            useBluetooth = checkedId == binding.radioBluetooth.id
            binding.editIp.hint = if (useBluetooth) "Paired Bluetooth device name" else "Robot IP e.g. 192.168.1.42:5000"
        }

        binding.btnConnect.setOnClickListener {
            val value = binding.editIp.text.toString().trim()
            if (value.isEmpty()) {
                Toast.makeText(this, "Enter an IP address or device name first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (useBluetooth) {
                bluetoothHelper = BluetoothHelper { msg, ok ->
                    binding.txtStatus.text = "Status: $msg"
                }
                bluetoothHelper?.connectToPairedDevice(value)
            } else {
                val base = if (value.startsWith("http")) value else "http://$value"
                robotConnection = RobotConnection(base)
                binding.txtStatus.text = "Status: connecting (WiFi)..."
                robotConnection?.fetchStatus(object : RobotConnection.StatusListener {
                    override fun onSuccess(response: String) {
                        binding.txtStatus.text = "Status: connected (WiFi)"
                        updateBatteryFromStatus(response)
                        startStatusPolling()
                    }
                    override fun onError(message: String) {
                        binding.txtStatus.text = "Status: connection failed — $message"
                    }
                })
            }
        }
    }

    private fun startStatusPolling() {
        if (statusPolling) return
        statusPolling = true
        val poll = object : Runnable {
            override fun run() {
                robotConnection?.fetchStatus(object : RobotConnection.StatusListener {
                    override fun onSuccess(response: String) {
                        updateBatteryFromStatus(response)
                    }
                    override fun onError(message: String) {
                        binding.txtBattery.text = "Battery: —"
                    }
                })
                statusHandler.postDelayed(this, 8000)
            }
        }
        statusHandler.postDelayed(poll, 1000)
    }

    // Only shows a battery figure if the Pi actually reported one — never
    // fabricates or defaults a percentage.
    private fun updateBatteryFromStatus(response: String) {
        try {
            val json = JSONObject(response)
            if (json.has("battery_pct") && !json.isNull("battery_pct")) {
                val pct = json.getInt("battery_pct")
                binding.txtBattery.text = "Battery: $pct%"
            } else {
                binding.txtBattery.text = "Battery: — (Pi not reporting this yet)"
            }
        } catch (e: Exception) {
            binding.txtBattery.text = "Battery: — (no data)"
        }
    }

    private fun send(json: JSONObject) {
        if (useBluetooth) {
            bluetoothHelper?.send(json.toString())
        } else {
            robotConnection?.sendCommand(json)
        }
    }

    // ---------------- MOVEMENT ----------------

    private fun setupJoystick() {
        binding.joystick.listener = object : JoystickView.Listener {
            override fun onMove(x: Float, y: Float) {
                send(RobotConnection.move(x, y))
            }
            override fun onRelease() {
                send(RobotConnection.stop())
            }
        }
    }

    // ---------------- ARMS ----------------

    private fun setupArmButtons() {
        binding.btnLeftArmUp.setOnClickListener { send(RobotConnection.arm("left", "up")) }
        binding.btnLeftArmDown.setOnClickListener { send(RobotConnection.arm("left", "down")) }
        binding.btnRightArmUp.setOnClickListener { send(RobotConnection.arm("right", "up")) }
        binding.btnRightArmDown.setOnClickListener { send(RobotConnection.arm("right", "down")) }
    }

    // ---------------- HEAD ----------------

    private fun setupHeadButtons() {
        binding.btnHeadLeft.setOnClickListener { send(RobotConnection.head("left")) }
        binding.btnHeadCenter.setOnClickListener { send(RobotConnection.head("center")) }
        binding.btnHeadRight.setOnClickListener { send(RobotConnection.head("right")) }
        binding.btnBlink.setOnClickListener { send(RobotConnection.blink()) }
    }

    // ---------------- MODES ----------------

    private fun setupModeButtons() {
        binding.btnModeManual.setOnClickListener { send(RobotConnection.mode("manual")) }
        binding.btnModeObstacle.setOnClickListener { send(RobotConnection.mode("obstacle_avoidance")) }
        binding.btnModeFollow.setOnClickListener { send(RobotConnection.mode("human_following")) }
        binding.btnModeLane.setOnClickListener { send(RobotConnection.mode("lane_following")) }
    }

    // ---------------- AI CHAT ----------------

    // The assistant's identity message — spoken once on launch and shown as
    // the first chat bubble, so the robot introduces itself properly.
    private val introMessage = "Hi! I'm your robot's AI assistant, created by Adithya Nandan — " +
        "a 10th-grade student who's passionate about robotics. I'm here to help you control " +
        "your robot and chat along the way."

    private fun setupChatSection() {
        addChatBubble(introMessage, isUser = false)

        binding.btnChatSend.setOnClickListener {
            val text = binding.editChatInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            addChatBubble(text, isUser = true)
            binding.editChatInput.setText("")
            showTyping(true)

            if (useBluetooth) {
                val json = JSONObject().apply { put("cmd", "chat"); put("text", text) }
                bluetoothHelper?.send(json.toString())
                showTyping(false)
                addChatBubble("(Sent over Bluetooth — reply will show on robot's own output)", isUser = false)
            } else {
                robotConnection?.sendChat(text, object : RobotConnection.StatusListener {
                    override fun onSuccess(response: String) {
                        showTyping(false)
                        val reply = try {
                            JSONObject(response).optString("reply", response)
                        } catch (e: Exception) {
                            response
                        }
                        addChatBubble(reply, isUser = false)
                        if (binding.chkSpeakReplies.isChecked) {
                            ttsHelper?.speak(reply)
                        }
                    }
                    override fun onError(message: String) {
                        showTyping(false)
                        addChatBubble("Error: $message", isUser = false)
                    }
                }) ?: run {
                    showTyping(false)
                    addChatBubble("Not connected — press Connect first", isUser = false)
                }
            }
        }
    }

    private fun showTyping(show: Boolean) {
        binding.txtTypingIndicator.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    // Builds a proper chat bubble view (not just appended text) — right-aligned
    // teal bubble for the user, left-aligned dark bubble for the assistant.
    private fun addChatBubble(text: String, isUser: Boolean) {
        val bubble = android.widget.TextView(this).apply {
            this.text = text
            setTextColor(if (isUser) android.graphics.Color.parseColor("#0A2E28") else resources.getColor(R.color.text_primary, theme))
            textSize = 14f
            setPadding(28, 18, 28, 18)
            background = resources.getDrawable(
                if (isUser) R.drawable.bubble_user_bg else R.drawable.bubble_assistant_bg,
                theme
            )
            alpha = 0f
        }

        val row = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }

        val bubbleParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        bubbleParams.marginStart = if (isUser) 60 else 0
        bubbleParams.marginEnd = if (isUser) 0 else 60
        row.addView(bubble, bubbleParams)

        binding.chatContainer.addView(row)
        bubble.animate().alpha(1f).setDuration(220).start()

        binding.scrollChat.post {
            binding.scrollChat.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsHelper?.shutdown()
        bluetoothHelper?.disconnect()
        statusHandler.removeCallbacksAndMessages(null)
    }
}
