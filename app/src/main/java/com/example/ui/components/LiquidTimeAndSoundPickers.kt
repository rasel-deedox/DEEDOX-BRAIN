package com.example.ui.components

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    title: String = "Select Exact Time",
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LiquidTheme.palette
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = false
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .testTag("liquid_time_picker_dialog"),
            shape = RoundedCornerShape(30.dp),
            color = palette.secondaryPillBg,
            border = BorderStroke(1.2.dp, palette.specularBorderTop),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryInk
                )

                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = palette.recessedTrayBg,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = palette.primaryInk,
                        selectorColor = CrimsonAccent,
                        containerColor = palette.secondaryPillBg,
                        periodSelectorBorderColor = palette.specularRimOutline,
                        periodSelectorSelectedContainerColor = CrimsonAccent,
                        periodSelectorUnselectedContainerColor = palette.recessedTrayBg,
                        periodSelectorSelectedContentColor = Color.White,
                        periodSelectorUnselectedContentColor = palette.secondaryInk,
                        timeSelectorSelectedContainerColor = palette.accentSoftBg,
                        timeSelectorUnselectedContainerColor = palette.recessedTrayBg,
                        timeSelectorSelectedContentColor = CrimsonAccent,
                        timeSelectorUnselectedContentColor = palette.primaryInk
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.specularRimOutline),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", color = palette.primaryInk)
                    }
                    TactilePrimaryPillButton(
                        text = "Set Time",
                        onClick = {
                            onConfirm(state.hour, state.minute)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_time_picker_button")
                    )
                }
            }
        }
    }
}

fun formatTime12h(hour24: Int, minute: Int): String {
    val h24 = hour24.coerceIn(0, 23)
    val m = minute.coerceIn(0, 59)
    val amPm = if (h24 < 12) "AM" else "PM"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, m, amPm)
}

object RingtonePreviewPlayer {
    private var activeRingtone: Ringtone? = null

    fun playPreview(context: Context, uriString: String?): Boolean {
        stop()
        val targetUri = uriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return false
        return runCatching {
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, targetUri) ?: return false
            activeRingtone = ringtone
            ringtone.play()
            true
        }.getOrDefault(false)
    }

    fun stop() {
        runCatching {
            activeRingtone?.let {
                if (it.isPlaying) it.stop()
            }
        }
        activeRingtone = null
    }
}
