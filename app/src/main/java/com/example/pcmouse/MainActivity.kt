package com.example.pcmouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MouseApp() }
    }
}

fun sendUdp(host: String, port: Int, packet: JSONObject) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val data = packet.toString().toByteArray()
            val address = InetAddress.getByName(host)
            val dp = DatagramPacket(data, data.size, address, port)
            DatagramSocket().use { it.send(dp) }
        } catch (_: Exception) {}
    }
}

@Composable
fun MouseApp() {
    var host by remember { mutableStateOf("192.168.1.100") }
    val port = 5005
    var sensitivity by remember { mutableStateOf(1.5f) }
    var connected by remember { mutableStateOf(false) }

    val darkBg = Color(0xFF111318)
    val surface = Color(0xFF1C2028)
    val accent = Color(0xFF4E9EFF)
    val textPrimary = Color(0xFFE8EAF0)
    val textMuted = Color(0xFF8890A4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            "PC Mouse",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Text(
            "Touchpad controller",
            fontSize = 14.sp,
            color = textMuted
        )

        Spacer(Modifier.height(24.dp))

        // Settings card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PC IP address", fontSize = 12.sp, color = textMuted)
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it; connected = false },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color(0xFF2E3545)
                    ),
                    placeholder = { Text("e.g. 192.168.1.100", color = textMuted) }
                )

                Text("Sensitivity: ${"%.1f".format(sensitivity)}x", fontSize = 12.sp, color = textMuted)
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0.5f..5f,
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                )

                Button(
                    onClick = {
                        val ping = JSONObject().apply { put("action", "move"); put("dx", 0); put("dy", 0) }
                        sendUdp(host, port, ping)
                        connected = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (connected) "✓ Connected" else "Connect", color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Touchpad area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(surface, RoundedCornerShape(20.dp))
                .pointerInput(host, sensitivity) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val packet = JSONObject().apply {
                            put("action", "move")
                            put("dx", dragAmount.x * sensitivity)
                            put("dy", dragAmount.y * sensitivity)
                        }
                        sendUdp(host, port, packet)
                    }
                }
                .pointerInput(host) {
                    detectTapGestures(
                        onTap = {
                            sendUdp(host, port, JSONObject().put("action", "left_click"))
                        },
                        onLongPress = {
                            sendUdp(host, port, JSONObject().put("action", "right_click"))
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Touchpad", fontSize = 16.sp, color = textMuted)
            Text(
                "Tap = left click  •  Hold = right click",
                fontSize = 11.sp,
                color = Color(0xFF4A5263),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Scroll buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { sendUdp(host, port, JSONObject().put("action", "scroll").put("amount", 3)) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = surface),
                shape = RoundedCornerShape(12.dp)
            ) { Text("▲ Scroll up", color = textPrimary) }

            Button(
                onClick = { sendUdp(host, port, JSONObject().put("action", "scroll").put("amount", -3)) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = surface),
                shape = RoundedCornerShape(12.dp)
            ) { Text("▼ Scroll down", color = textPrimary) }
        }

        Spacer(Modifier.height(16.dp))
    }
}
