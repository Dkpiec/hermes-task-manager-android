package com.hermes.taskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.taskmanager.ui.theme.TodoistRed

@Composable
fun PinScreen(
    correctPin: String,
    onPinSuccess: () -> Unit,
    onBiometricClick: () -> Unit,
    onSwitchUserClick: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TodoistRed,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter Quick PIN",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = "Unlock your Hermes workspace",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 4 Pin Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val filled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> MaterialTheme.colorScheme.error
                                        filled -> TodoistRed
                                        else -> Color(0xFFE5E7EB)
                                    }
                                )
                        )
                    }
                }

                if (isError) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Incorrect PIN. Try again.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 3x4 Numeric Keypad
                val keypad = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("bio", "0", "back")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in keypad) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (key in row) {
                                when (key) {
                                    "bio" -> {
                                        IconButton(
                                            onClick = onBiometricClick,
                                            modifier = Modifier.size(60.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometric",
                                                tint = TodoistRed,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    "back" -> {
                                        IconButton(
                                            onClick = {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    isError = false
                                                }
                                            },
                                            modifier = Modifier.size(60.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Backspace",
                                                tint = Color(0xFF6B7280),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFF3F4F6),
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clickable {
                                                    if (enteredPin.length < 4) {
                                                        enteredPin += key
                                                        isError = false
                                                        if (enteredPin.length == 4) {
                                                            if (enteredPin == correctPin) {
                                                                onPinSuccess()
                                                            } else {
                                                                isError = true
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = key,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF111827)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(onClick = onSwitchUserClick) {
                    Text(
                        text = "Sign in with password",
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
