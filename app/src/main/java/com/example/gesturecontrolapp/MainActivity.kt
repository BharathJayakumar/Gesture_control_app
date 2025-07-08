package com.example.gesturecontrolapp
// Required imports for Android and Compose UI elements
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.border
import com.example.gesturecontrolapp.ui.theme.GestureControlAppTheme
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import java.net.ConnectException
import java.net.SocketTimeoutException

// Main Activity - entry point of the app
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GestureControlAppTheme {
                VolumeControlApp()
            }
        }
    }
}

@Composable
fun VolumeControlApp() {
    // UI states for displaying sensor readings and system volume info
    var distance by remember { mutableStateOf("Connecting...") }
    var volumeLevel by remember { mutableStateOf(0) }
    var currentSystemVolume by remember { mutableStateOf(0) }
    var maxSystemVolume by remember { mutableStateOf(100) }
    var isConnected by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Initializing...") }

    // Add hand detection variables
    var isHandDetected by remember { mutableStateOf(false) }
    var handDetectionStatus by remember { mutableStateOf("No hand detected") }
    var lastValidVolume by remember { mutableStateOf(0) }

    // Add debouncing variables
    var lastVolumeChangeTime by remember { mutableStateOf(0L) }
    var lastAppliedVolume by remember { mutableStateOf(-1) }
    var pendingVolume by remember { mutableStateOf(-1) }

    // Add variables for debugging
    var rawSensorData by remember { mutableStateOf("") }
    var parsedDistance by remember { mutableStateOf(0f) }
    var parseStatus by remember { mutableStateOf("") }

    val context = LocalContext.current
    val client = remember {
        HttpClient(OkHttp) {
            install(HttpTimeout)
        }
    }

    // Get AudioManager
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Animated slider value for smooth transitions
    val animatedSliderValue by animateFloatAsState(
        targetValue = if (isHandDetected) volumeLevel / 100f else lastValidVolume / 100f,
        animationSpec = tween(durationMillis = 300),
        label = "sliderAnimation"
    )

    // Pulse animation for hand detection
    val pulseAnimation by animateFloatAsState(
        targetValue = if (isHandDetected) 1f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnimation"
    )

    // Initialize max volume
    LaunchedEffect(Unit) {
        maxSystemVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    // Parse and extract numeric value from sensor response
    fun parseDistanceFromResponse(response: String): Float? {
        return try {
            // Clean the response
            val cleanResponse = response.trim()

            // Try different parsing methods
            when {
                // Case 1: Pure number (e.g., "23.45")
                cleanResponse.matches(Regex("^\\d+\\.?\\d*$")) -> {
                    cleanResponse.toFloat()
                }

                // Case 2: JSON format with capital D (e.g., {"Distance": 23})
                cleanResponse.startsWith("{") && cleanResponse.endsWith("}") -> {
                    // Parse the specific JSON format from your ESP32: {"Distance":23}
                    val distanceRegex = Regex("\"Distance\"\\s*:\\s*(\\d+\\.?\\d*)")
                    val match = distanceRegex.find(cleanResponse)
                    match?.groupValues?.get(1)?.toFloat()
                        ?: run {
                            // Fallback: try lowercase "distance"
                            val fallbackRegex = Regex("\"distance\"\\s*:\\s*(\\d+\\.?\\d*)")
                            val fallbackMatch = fallbackRegex.find(cleanResponse)
                            fallbackMatch?.groupValues?.get(1)?.toFloat()
                        }
                }

                // Case 3: Key-value format (e.g., "distance=23.45")
                cleanResponse.contains("=") -> {
                    val parts = cleanResponse.split("=")
                    if (parts.size == 2) {
                        parts[1].trim().toFloat()
                    } else null
                }

                // Case 4: Extract first number from string (e.g., "Distance: 23.45 cm")
                else -> {
                    val regex = Regex("(\\d+\\.?\\d*)")
                    val match = regex.find(cleanResponse)
                    match?.value?.toFloat()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    // Check if hand is detected based on distance
    fun isHandDetectedByDistance(distance: Float?): Boolean {
        return if (distance != null) {
            // Hand is detected if distance is between 5cm and 80cm
            // Values outside this range are considered "no hand" or "invalid reading"
            distance in 5f..50f
        } else {
            false
        }
    }

    // Convert distance to volume level (0–100) - only when hand is detected
    fun calculateVolume(distance: Float?): Int {
        return if (distance != null && isHandDetectedByDistance(distance)) {
            when {
                distance <= 5f -> 0
                distance >= 50f -> 100
                else -> ((distance - 5f) / 45f * 100).toInt()
            }
        } else {
            lastValidVolume // Return last valid volume if no hand detected
        }
    }

    // Apply volume to system with debouncing
    fun applyVolumeToSystem(volumePercentage: Int) {
        try {
            val targetVolume = (volumePercentage / 100f * maxSystemVolume).toInt()
            val clampedVolume = targetVolume.coerceIn(0, maxSystemVolume)

            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                clampedVolume,
                0 // Remove FLAG_SHOW_UI to prevent UI spam
            )

            // Update current system volume for display
            currentSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            lastAppliedVolume = volumePercentage
            lastVolumeChangeTime = System.currentTimeMillis()

        } catch (e: Exception) {
            connectionStatus = "Volume control error: ${e.message}"
        }
    }

    // Debounced volume application
    LaunchedEffect(pendingVolume) {
        if (pendingVolume != -1) {
            delay(150) // Wait for 150ms before applying

            // Check if the pending volume is still the same (no new changes)
            if (pendingVolume != -1 && pendingVolume != lastAppliedVolume) {
                val volumeToApply = pendingVolume
                pendingVolume = -1
                applyVolumeToSystem(volumeToApply)
            }
        }
    }

    // Fetch sensor data in loop
    LaunchedEffect(Unit) {
        while (true) {
            try {
                connectionStatus = "Connecting to sensor..."
                val response: String = client.get("http://172.20.10.2/sensor") {
                    timeout {
                        requestTimeoutMillis = 3000
                        connectTimeoutMillis = 3000
                        socketTimeoutMillis = 3000
                    }
                }.body()

                val cleanResponse = response.trim()
                rawSensorData = cleanResponse // Store raw data for debugging

                // Parse the distance value
                val distanceValue = parseDistanceFromResponse(cleanResponse)
                if (distanceValue != null) {
                    parsedDistance = distanceValue
                    distance = distanceValue.toString()
                    parseStatus = "✓ Parsed successfully"

                    // Check if hand is detected
                    val handDetected = isHandDetectedByDistance(distanceValue)
                    isHandDetected = handDetected

                    if (handDetected) {
                        handDetectionStatus = "✓ Hand detected"
                        val newVolumeLevel = calculateVolume(distanceValue)

                        // Update volume level for UI immediately
                        volumeLevel = newVolumeLevel
                        lastValidVolume = newVolumeLevel // Store as last valid volume

                        // Only schedule volume change if it's significantly different
                        val currentTime = System.currentTimeMillis()
                        if (kotlin.math.abs(newVolumeLevel - lastAppliedVolume) > 3 &&
                            currentTime - lastVolumeChangeTime > 100) { // Minimum 100ms between changes

                            pendingVolume = newVolumeLevel
                        }
                    } else {
                        handDetectionStatus = "✗ No hand detected (distance: ${distanceValue}cm)"
                        // Don't change volume when no hand is detected
                        volumeLevel = lastValidVolume
                    }

                } else {
                    parseStatus = "✗ Failed to parse: '$cleanResponse'"
                    distance = "Parse Error"
                    isHandDetected = false
                    handDetectionStatus = "✗ Parse error"
                }

                isConnected = true
                connectionStatus = "Connected successfully"

            } catch (e: ConnectException) {
                distance = "No connection"
                isConnected = false
                isHandDetected = false
                connectionStatus = "Cannot reach sensor at 172.20.10.2"
                handDetectionStatus = "✗ Connection failed"
                rawSensorData = "Connection failed"
                parseStatus = "✗ No connection"
            } catch (e: SocketTimeoutException) {
                distance = "Timeout"
                isConnected = false
                isHandDetected = false
                connectionStatus = "Connection timeout"
                handDetectionStatus = "✗ Timeout"
                rawSensorData = "Timeout"
                parseStatus = "✗ Timeout"
            } catch (e: Exception) {
                distance = "Error: ${e.message}"
                isConnected = false
                isHandDetected = false
                connectionStatus = "Error: ${e.javaClass.simpleName}"
                handDetectionStatus = "✗ Error"
                rawSensorData = "Error: ${e.message}"
                parseStatus = "✗ Exception: ${e.javaClass.simpleName}"
            }

            delay(500L) // Increased delay since sensor updates every 2 seconds
        }
    }

    // Periodic system volume sync
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // Every second
            try {
                val actualVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (actualVolume != currentSystemVolume) {
                    currentSystemVolume = actualVolume
                }
            } catch (e: Exception) {
                // Ignore errors in background sync
            }
        }
    }

    // Automotive themed UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF1A1A1A),
                        Color(0xFF0A0A0A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column {
                Text(
                    text = "GESTURE CONTROL",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D4FF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "AUTOMOTIVE AUDIO SYSTEM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Main Control Section
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Central Volume Display with Circular Progress
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    // Circular Progress Background
                    Canvas(
                        modifier = Modifier.size(280.dp)
                    ) {
                        val strokeWidth = 8.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        // Background circle
                        drawCircle(
                            color = Color(0xFF333333),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth)
                        )

                        // Progress arc
                        val sweepAngle = 360f * animatedSliderValue
                        drawArc(
                            color = if (isHandDetected) Color(0xFF00D4FF) else Color(0xFFFF6B35),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth)
                        )
                    }

                    // Volume Display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${(animatedSliderValue * 100).toInt()}",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "VOLUME",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF888888)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Status Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Connection Status
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isConnected) Color(0xFF1B4D3E) else Color(0xFF4D1B1B)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (isConnected) Color(0xFF00FF88) else Color(0xFFFF4444),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Hand Detection Status
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isHandDetected) Color(0xFF1B3A4D) else Color(0xFF4D3A1B)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (isHandDetected) Color(0xFF00D4FF) else Color(0xFFFFAA00),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isHandDetected) "TRACKING" else "SEARCHING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Distance Display
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2D2D2D)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isConnected) "$distance" else "---",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00D4FF)
                            )
                            Text(
                                text = "CM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF888888)
                            )
                        }
                    }
                }
            }

            // Bottom Section
            Column {
                // System Volume Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SYSTEM AUDIO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF888888)
                            )
                            Text(
                                text = "$currentSystemVolume / $maxSystemVolume",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Volume bars visualization
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(10) { index ->
                                val isActive = index < (currentSystemVolume * 10 / maxSystemVolume)
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(if (isActive) (16 + index * 2).dp else 8.dp)
                                        .background(
                                            color = if (isActive) Color(0xFF00D4FF) else Color(0xFF333333),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Instructions
                Text(
                    text = "Place your hand 5-50cm from the sensor • Closer = Quieter • Further = Louder",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}