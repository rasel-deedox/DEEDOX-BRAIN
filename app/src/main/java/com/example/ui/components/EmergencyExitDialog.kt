package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

data class MathChallengeEquation(
    val prompt: String,
    val expectedAnswer: Int
)

object EmergencyExitValidator {
    const val REQUIRED_PHRASE = "I choose to exit focus intentionally"
    const val MIN_REFLECTION_CHARS = 15
    const val MIN_REFLECTION_WORDS = 3

    val REFLECTION_PROMPTS = listOf(
        "1. Why do I need to open this app right now?",
        "2. What specific focus goal am I interrupting?",
        "3. Will this still matter to me in 2 hours?"
    )

    fun generateMathEquation(random: Random = Random.Default): MathChallengeEquation {
        val a = random.nextInt(12, 26)
        val b = random.nextInt(11, 19)
        val c = random.nextInt(18, 65)
        return MathChallengeEquation(
            prompt = "($a × $b) + $c = ?",
            expectedAnswer = (a * b) + c
        )
    }

    fun isReflectionAnswerValid(text: String): Boolean {
        val clean = text.trim()
        if (clean.length < MIN_REFLECTION_CHARS) return false
        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.size >= MIN_REFLECTION_WORDS
    }

    fun isStage1Valid(answer1: String, answer2: String, answer3: String): Boolean {
        return isReflectionAnswerValid(answer1) &&
            isReflectionAnswerValid(answer2) &&
            isReflectionAnswerValid(answer3)
    }

    fun isStage2Valid(rawAnswer: String, expectedAnswer: Int): Boolean {
        return rawAnswer.trim().toIntOrNull() == expectedAnswer
    }

    fun isStage3Valid(
        typedPhrase: String,
        patienceSecondsRemaining: Int,
        confirmedCheckbox: Boolean
    ): Boolean {
        return typedPhrase.trim().equals(REQUIRED_PHRASE, ignoreCase = true) &&
            patienceSecondsRemaining <= 0 &&
            confirmedCheckbox
    }
}

@Composable
fun EmergencyExitDialog(
    targetRuleTitle: String,
    onDismiss: () -> Unit,
    onUnlockConfirmed: () -> Unit,
    initialMathEquation: MathChallengeEquation = remember { EmergencyExitValidator.generateMathEquation() },
    patienceDurationSeconds: Int = 25
) {
    val palette = LiquidTheme.palette
    var stage by remember { mutableIntStateOf(1) }

    // Stage 1: Reflection Answers
    var answer1 by remember { mutableStateOf("") }
    var answer2 by remember { mutableStateOf("") }
    var answer3 by remember { mutableStateOf("") }
    var stage1Error by remember { mutableStateOf<String?>(null) }

    // Stage 2: Math Challenge
    var mathEquation by remember { mutableStateOf(initialMathEquation) }
    var mathInput by remember { mutableStateOf("") }
    var stage2Error by remember { mutableStateOf<String?>(null) }

    // Stage 3: 3 Mandatory Micro-Tasks
    var typedPhrase by remember { mutableStateOf("") }
    var patienceRemaining by remember { mutableIntStateOf(patienceDurationSeconds) }
    var confirmedIntent by remember { mutableStateOf(false) }
    var stage3Error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(stage) {
        if (stage == 3 && patienceRemaining > 0) {
            while (patienceRemaining > 0) {
                delay(1_000L)
                patienceRemaining--
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.recessedTrayBg,
        unfocusedContainerColor = palette.recessedTrayBg,
        focusedBorderColor = palette.accentCrimson,
        unfocusedBorderColor = palette.specularRimOutline,
        focusedTextColor = palette.primaryInk,
        unfocusedTextColor = palette.primaryInk
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = palette.secondaryPillBg,
            border = BorderStroke(1.2.dp, palette.specularBorderTop),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .testTag("emergency_exit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MONK MODE OVERRIDE • STAGE $stage OF 3",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonAccent
                        )
                        Text(
                            text = "Emergency Exit Protocol",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryInk
                        )
                        Text(
                            text = "Active Shield: $targetRuleTitle",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryInk
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = palette.accentSoftBg,
                        border = BorderStroke(1.dp, palette.accentCrimson.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MinimalIcon(
                                type = MinimalIconType.LOCK,
                                active = true,
                                tint = CrimsonAccent,
                                size = 18.dp
                            )
                        }
                    }
                }

                // 3-Stage Stepper Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1. Reflect", "2. Math", "3. Confirm").forEachIndexed { idx, label ->
                        val stepNum = idx + 1
                        val active = stage == stepNum
                        val done = stage > stepNum
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = when {
                                active -> CrimsonAccent
                                done -> palette.accentSoftBg
                                else -> palette.recessedTrayBg
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (done) "✓ $label" else label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    active -> Color.White
                                    done -> palette.accentSoftInk
                                    else -> palette.secondaryInk
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                AnimatedContent(targetState = stage, label = "emergency_exit_stage") { currentStage ->
                    when (currentStage) {
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Stage 1: Mindful Accountability",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                                Text(
                                    text = "Answer all 3 reflection prompts thoughtfully (at least 3 words and 15 characters each).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                                OutlinedTextField(
                                    value = answer1,
                                    onValueChange = { answer1 = it; stage1Error = null },
                                    label = { Text(EmergencyExitValidator.REFLECTION_PROMPTS[0]) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = fieldColors,
                                    minLines = 2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("emergency_reflect_1")
                                )
                                OutlinedTextField(
                                    value = answer2,
                                    onValueChange = { answer2 = it; stage1Error = null },
                                    label = { Text(EmergencyExitValidator.REFLECTION_PROMPTS[1]) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = fieldColors,
                                    minLines = 2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("emergency_reflect_2")
                                )
                                OutlinedTextField(
                                    value = answer3,
                                    onValueChange = { answer3 = it; stage1Error = null },
                                    label = { Text(EmergencyExitValidator.REFLECTION_PROMPTS[2]) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = fieldColors,
                                    minLines = 2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("emergency_reflect_3")
                                )
                                if (!stage1Error.isNullOrBlank()) {
                                    Text(stage1Error!!, style = MaterialTheme.typography.bodySmall, color = CrimsonAccent)
                                }
                            }
                        }

                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Stage 2: Cognitive Friction Equation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                                Text(
                                    text = "Solve this exact calculation without guessing to proceed to Stage 3.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryInk
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = palette.recessedTrayBg,
                                    border = BorderStroke(1.dp, palette.accentCrimson.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = mathEquation.prompt,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryInk,
                                        modifier = Modifier
                                            .padding(18.dp)
                                            .testTag("emergency_math_prompt")
                                    )
                                }
                                OutlinedTextField(
                                    value = mathInput,
                                    onValueChange = { mathInput = it.filter { ch -> ch.isDigit() || ch == '-' }; stage2Error = null },
                                    label = { Text("Exact numerical answer") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("emergency_math_input")
                                )
                                if (!stage2Error.isNullOrBlank()) {
                                    Text(stage2Error!!, style = MaterialTheme.typography.bodySmall, color = CrimsonAccent)
                                }
                            }
                        }

                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Stage 3: 3 Mandatory Micro-Tasks",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                                // Micro-task A: Exact confirmation phrase
                                Text(
                                    text = "Task A • Type this exact phrase:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.secondaryInk
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.recessedTrayBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = EmergencyExitValidator.REQUIRED_PHRASE,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.accentSoftInk,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                OutlinedTextField(
                                    value = typedPhrase,
                                    onValueChange = { typedPhrase = it; stage3Error = null },
                                    label = { Text("Confirmation phrase") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("emergency_phrase_input")
                                )

                                // Micro-task B: Patience cooldown timer
                                Text(
                                    text = if (patienceRemaining > 0) {
                                        "Task B • Hold Intention: Wait ${patienceRemaining}s before exit unlocks..."
                                    } else {
                                        "Task B • Patience timer complete ✓"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (patienceRemaining > 0) palette.secondaryInk else palette.accentSoftInk,
                                    modifier = Modifier.testTag("emergency_patience_label")
                                )
                                val denom = patienceDurationSeconds.coerceAtLeast(1).toFloat()
                                LinearProgressIndicator(
                                    progress = { ((denom - patienceRemaining) / denom).coerceIn(0f, 1f) },
                                    color = CrimsonAccent,
                                    trackColor = palette.recessedTrayBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(9999.dp))
                                )

                                // Micro-task C: Final manual acknowledgement toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = confirmedIntent,
                                        onCheckedChange = { confirmedIntent = it; stage3Error = null },
                                        colors = CheckboxDefaults.colors(checkedColor = CrimsonAccent),
                                        modifier = Modifier.testTag("emergency_confirm_checkbox")
                                    )
                                    Text(
                                        text = "Task C • I accept ending this Monk Shield lock intentionally.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.primaryInk
                                    )
                                }

                                if (!stage3Error.isNullOrBlank()) {
                                    Text(stage3Error!!, style = MaterialTheme.typography.bodySmall, color = CrimsonAccent)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, palette.inputBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("emergency_stay_focused_button")
                    ) {
                        Text("Stay Focused", color = palette.primaryInk)
                    }

                    Button(
                        onClick = {
                            when (stage) {
                                1 -> {
                                    if (EmergencyExitValidator.isStage1Valid(answer1, answer2, answer3)) {
                                        stage = 2
                                    } else {
                                        stage1Error = "Complete all 3 reflection prompts (at least 3 words & 15 characters each)."
                                    }
                                }
                                2 -> {
                                    if (EmergencyExitValidator.isStage2Valid(mathInput, mathEquation.expectedAnswer)) {
                                        stage = 3
                                    } else {
                                        stage2Error = "Incorrect calculation. Focus and check your arithmetic."
                                    }
                                }
                                else -> {
                                    if (EmergencyExitValidator.isStage3Valid(typedPhrase, patienceRemaining, confirmedIntent)) {
                                        onUnlockConfirmed()
                                    } else {
                                        stage3Error = when {
                                            patienceRemaining > 0 -> "Wait ${patienceRemaining}s for the patience timer to complete."
                                            !typedPhrase.trim().equals(EmergencyExitValidator.REQUIRED_PHRASE, ignoreCase = true) ->
                                                "Type the exact confirmation phrase above."
                                            else -> "Check Task C confirmation before unlocking."
                                        }
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonAccent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("emergency_next_unlock_button")
                    ) {
                        Text(
                            text = when (stage) {
                                1 -> "Verify Stage 1"
                                2 -> "Verify Stage 2"
                                else -> "Unlock Shield"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
