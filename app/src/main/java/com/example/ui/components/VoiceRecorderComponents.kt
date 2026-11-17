package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.media.VideoThumbnailGenerator
import com.example.data.media.VoicePlaybackState
import com.example.data.media.VoiceRecorderState
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme

@Composable
fun VoiceNoteRecorderPanel(
    recorderState: VoiceRecorderState,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
    onSaveToNote: (title: String) -> Unit,
    onSaveToTodo: (title: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val context = LocalContext.current
    var titleInput by remember { mutableStateOf("") }
    var permissionDeniedNotice by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNotice = false
            onStartRecording()
        } else {
            permissionDeniedNotice = true
        }
    }

    val pulseAlpha = if (recorderState.isRecording && !recorderState.isPaused) {
        val infinite = rememberInfiniteTransition(label = "micPulse")
        val a by infinite.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "micAlpha"
        )
        a
    } else {
        1f
    }

    TactileGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_recorder_panel"),
        cornerRadius = 28.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(if (recorderState.isRecording) CrimsonAccent else palette.accentSoftInk)
                    )
                    Text(
                        text = when {
                            recorderState.isRecording && recorderState.isPaused -> "Recording Paused"
                            recorderState.isRecording -> "Recording Voice Note..."
                            else -> "Voice-to-Note Recorder"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                }
                Text(
                    text = VideoThumbnailGenerator.formatDurationLabel(recorderState.elapsedMs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonAccent
                )
            }

            if (permissionDeniedNotice) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.accentSoftBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Microphone permission is needed to record audio notes. Tap Start Mic to grant access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonAccent,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (!recorderState.error.isNullOrBlank()) {
                Text(
                    text = recorderState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = CrimsonAccent
                )
            }

            // Live Waveform Bars
            val waveBars = recorderState.amplitudes
            val activeBarColor = CrimsonAccent
            val idleBarColor = palette.recessedTrayBg
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.recessedTrayBg)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                val barCount = 36
                val spacing = 4.dp.toPx()
                val totalSpacing = spacing * (barCount - 1)
                val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)
                val maxH = size.height
                val offset = (waveBars.size - barCount).coerceAtLeast(0)
                for (i in 0 until barCount) {
                    val amp = waveBars.getOrNull(i + offset) ?: 0.12f
                    val bh = (amp * maxH).coerceAtLeast(4f)
                    val left = i * (barWidth + spacing)
                    val top = (maxH - bh) / 2f
                    drawRoundRect(
                        color = if (i + offset < waveBars.size) activeBarColor else idleBarColor.copy(alpha = 0.5f),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, bh),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Voice title (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.cardSurfaceTop,
                    unfocusedContainerColor = palette.cardSurfaceTop,
                    focusedBorderColor = CrimsonAccent,
                    unfocusedBorderColor = palette.inputBorder,
                    focusedTextColor = palette.primaryInk,
                    unfocusedTextColor = palette.primaryInk
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_title_input")
            )

            if (!recorderState.isRecording) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TactilePrimaryPillButton(
                        text = "Start Mic",
                        iconType = MinimalIconType.MIC,
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                permissionDeniedNotice = false
                                onStartRecording()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_start_button")
                    )
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.inputBorder),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Close", color = palette.primaryInk)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (recorderState.isPaused) onResumeRecording() else onPauseRecording()
                        },
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.inputBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (recorderState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = if (recorderState.isPaused) "Resume" else "Pause",
                            tint = palette.primaryInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (recorderState.isPaused) "Resume" else "Pause", color = palette.primaryInk)
                    }
                    TextButton(
                        onClick = {
                            onDiscardRecording()
                            onDismiss()
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Discard", color = CrimsonAccent)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveToNote(titleInput.trim())
                            onDismiss()
                        },
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryButtonBg,
                            contentColor = palette.primaryButtonContent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_save_note_button")
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save to Note", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onSaveToTodo(titleInput.trim())
                            onDismiss()
                        },
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonAccent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_save_todo_button")
                    ) {
                        Text("Today's To-Do", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceAudioPlayerCard(
    audioUri: String,
    durationMs: Long,
    playbackState: VoicePlaybackState,
    onTogglePlayback: (String) -> Unit,
    onSeekTo: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val isThisAudio = playbackState.activeUri == audioUri
    val isPlaying = isThisAudio && playbackState.isPlaying
    val effectiveDuration = if (isThisAudio && playbackState.durationMs > 0L) {
        playbackState.durationMs
    } else {
        durationMs.coerceAtLeast(1000L)
    }
    val currentPos = if (isThisAudio) playbackState.positionMs else 0L
    var scrubbingValue by remember(audioUri) { mutableFloatStateOf(-1f) }
    val sliderFraction = if (scrubbingValue >= 0f) {
        scrubbingValue
    } else {
        (currentPos.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.recessedTrayBg,
        border = BorderStroke(1.dp, palette.specularRimOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = { onTogglePlayback(audioUri) },
                shape = CircleShape,
                color = CrimsonAccent,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("audio_play_toggle")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Slider(
                    value = sliderFraction,
                    onValueChange = { scrubbingValue = it },
                    onValueChangeFinished = {
                        if (scrubbingValue >= 0f) {
                            onSeekTo(audioUri, (scrubbingValue * effectiveDuration).toLong())
                            scrubbingValue = -1f
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = CrimsonAccent,
                        activeTrackColor = CrimsonAccent,
                        inactiveTrackColor = palette.barTrack
                    ),
                    modifier = Modifier.height(26.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = VideoThumbnailGenerator.formatDurationLabel(currentPos),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryInk
                    )
                    Text(
                        text = VideoThumbnailGenerator.formatDurationLabel(effectiveDuration),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryInk
                    )
                }
            }
        }
    }
}
