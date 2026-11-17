package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OnboardingProfile
import com.example.ui.components.AppLogo
import com.example.ui.components.LiquidThemePillSwitcher
import com.example.ui.components.MinimalIcon
import com.example.ui.components.MinimalIconType
import com.example.ui.components.RecessedFramingBezel
import com.example.ui.components.TactileAlabasterBackground
import com.example.ui.components.TactilePrimaryPillButton
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.InstrumentSerifFamily
import com.example.ui.theme.LiquidTheme
import com.example.ui.theme.SoftCreamInk
import com.example.ui.theme.SoftCreamWhite

private enum class OnboardingStep(val stepIndex: Int, val totalSteps: Int) {
    WIZARD_ROLE(1, 4),
    WIZARD_USE_CASE(2, 4),
    WIZARD_SAVE_HABIT(3, 4),
    WIZARD_WORKFLOW(4, 4),
    GOOGLE_AUTH(5, 4)
}

private enum class EditorialVectorKind {
    TARGET,
    STABILITY,
    FOCUS_WAVES,
    SHIELD
}

private data class WizardOption(
    val id: String,
    val title: String,
    val subtitle: String
)

private val ROLE_OPTIONS = listOf(
    WizardOption(
        id = "developer",
        title = "Developer & Engineer",
        subtitle = "GitHub repositories, architecture notes & API references"
    ),
    WizardOption(
        id = "designer",
        title = "Product & UI Designer",
        subtitle = "Liquid Glass visual references, typography & layout systems"
    ),
    WizardOption(
        id = "researcher",
        title = "Researcher & Student",
        subtitle = "Indexed PDFs, structured study guides & long-form reading"
    ),
    WizardOption(
        id = "founder",
        title = "Founder & Creator",
        subtitle = "AI prompts, product ideas & fast cross-device captures"
    )
)

private val USE_CASE_OPTIONS = listOf(
    WizardOption(
        id = "ai_knowledge",
        title = "AI Knowledge & Prompt Hub",
        subtitle = "Smart tagging, AI organization previews & contextual recall"
    ),
    WizardOption(
        id = "bookmarks_repos",
        title = "Rich Link & Repo Bookmarks",
        subtitle = "Open Graph, YouTube & GitHub thumbnails inside tactile cards"
    ),
    WizardOption(
        id = "local_files",
        title = "Private Local SAF File Vault",
        subtitle = "Reference PDFs and media cleanly on-device without duplication"
    ),
    WizardOption(
        id = "multi_device",
        title = "PC & Mobile Real-Time Sync",
        subtitle = "Beam links and keep your curated library synced everywhere"
    )
)

private val SAVE_HABIT_OPTIONS = listOf(
    WizardOption(
        id = "daily_curator",
        title = "Daily Links & Snippets",
        subtitle = "Rapid captures from browsers, GitHub, YouTube & share sheet"
    ),
    WizardOption(
        id = "deep_research",
        title = "Long-Form Reading & Docs",
        subtitle = "Focused folders for articles, PDFs, papers & reference guides"
    ),
    WizardOption(
        id = "visual_boards",
        title = "Visual & Creative Inspiration",
        subtitle = "Media previews, product teardowns & design system libraries"
    ),
    WizardOption(
        id = "quick_archive",
        title = "Calm Personal Archive",
        subtitle = "Minimal distraction-free vault for timeless bookmarks & notes"
    )
)

private val WORKFLOW_OPTIONS = listOf(
    WizardOption(
        id = "starter_folders",
        title = "Curated Starter Folders",
        subtitle = "Ready-made AI, GitHub, Design & Learning vaults beside Inbox"
    ),
    WizardOption(
        id = "minimal_inbox",
        title = "Minimal Universal Inbox",
        subtitle = "Start ultra-clean with Inbox only and organize at your pace"
    ),
    WizardOption(
        id = "deep_sync",
        title = "Cloud-First Auto Sync",
        subtitle = "Instant Google cloud metadata sync & automatic link enrichment"
    ),
    WizardOption(
        id = "local_privacy",
        title = "Offline-First Local Sanctuary",
        subtitle = "Keep everything fast on SQLite and opt into sync whenever ready"
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingAuthScreen(
    isSignedIn: Boolean,
    isWelcomeSyncing: Boolean,
    authError: String?,
    userDisplayName: String,
    userEmail: String?,
    onboardingProfile: OnboardingProfile = OnboardingProfile(),
    themeMode: AppThemeMode,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onExploreDemoClick: () -> Unit,
    onContinueAsGuestClick: () -> Unit = onExploreDemoClick,
    onGoogleSignInClick: () -> Unit,
    onWizardCompleted: (role: String, useCase: String, saveHabit: String, workflow: String) -> Unit = { _, _, _, _ -> },
    onCompleteWizardAndSync: (role: String, useCase: String, saveHabit: String, workflow: String) -> Unit,
    onClearAuthError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    var currentStep by remember(onboardingProfile.wizardCompleted) {
        mutableStateOf(
            if (onboardingProfile.wizardCompleted) OnboardingStep.GOOGLE_AUTH
            else OnboardingStep.WIZARD_ROLE
        )
    }
    var selectedRole by remember(onboardingProfile.role) {
        mutableStateOf(onboardingProfile.role.ifBlank { "developer" })
    }
    var selectedUseCase by remember(onboardingProfile.primaryUseCase) {
        mutableStateOf(onboardingProfile.primaryUseCase.ifBlank { "ai_knowledge" })
    }
    var selectedSaveHabit by remember(onboardingProfile.saveHabit) {
        mutableStateOf(onboardingProfile.saveHabit.ifBlank { "daily_curator" })
    }
    var selectedWorkflow by remember(onboardingProfile.workflowPreset) {
        mutableStateOf(onboardingProfile.workflowPreset.ifBlank { "starter_folders" })
    }

    LaunchedEffect(isSignedIn, currentStep, isWelcomeSyncing) {
        if (isSignedIn && currentStep == OnboardingStep.GOOGLE_AUTH && !isWelcomeSyncing) {
            onCompleteWizardAndSync(selectedRole, selectedUseCase, selectedSaveHabit, selectedWorkflow)
        }
    }

    // Absolute full-screen Liquid Glass canvas with zero unwanted outer margins
    TactileAlabasterBackground(
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = viewportHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Minimal Editorial Header: Brand + Liquid Theme Switcher
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppLogo(
                                size = 42.dp,
                                cornerRadius = 14.dp,
                                testTag = "brand_neural_bookmark_logo"
                            )
                            Column {
                                Text(
                                    text = "Personal AI Library",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryInk
                                )
                                Text(
                                    text = "Editorial Knowledge Vault",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondaryInk
                                )
                            }
                        }

                        if (currentStep.stepIndex in 2..4 && !isWelcomeSyncing) {
                            TextButton(
                                onClick = {
                                    currentStep = when (currentStep) {
                                        OnboardingStep.WIZARD_USE_CASE -> OnboardingStep.WIZARD_ROLE
                                        OnboardingStep.WIZARD_SAVE_HABIT -> OnboardingStep.WIZARD_USE_CASE
                                        OnboardingStep.WIZARD_WORKFLOW -> OnboardingStep.WIZARD_SAVE_HABIT
                                        else -> currentStep
                                    }
                                },
                                modifier = Modifier.testTag("wizard_back_button")
                            ) {
                                Text(
                                    text = "← Back",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.secondaryInk
                                )
                            }
                        }
                    }

                    LiquidThemePillSwitcher(
                        selectedMode = themeMode,
                        onSelectMode = onSelectThemeMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Main Frosted Liquid Glass Card
                FrostedOnboardingGlassContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                ) {
                    if (isWelcomeSyncing) {
                        WelcomeSyncTransitionCard(
                            userDisplayName = userDisplayName,
                            userEmail = userEmail
                        )
                    } else {
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                val forward = targetState.stepIndex >= initialState.stepIndex
                                val slideSpec = spring<IntOffset>(stiffness = 320f, dampingRatio = 0.85f)
                                (slideInHorizontally(slideSpec) { fullW ->
                                    if (forward) fullW / 3 else -fullW / 3
                                } + fadeIn(tween(240))) togetherWith
                                    (slideOutHorizontally(slideSpec) { fullW ->
                                        if (forward) -fullW / 3 else fullW / 3
                                    } + fadeOut(tween(180)))
                            },
                            label = "editorialOnboardingTransition"
                        ) { step ->
                            when (step) {
                                OnboardingStep.WIZARD_ROLE -> {
                                    EditorialWizardStepContent(
                                        categoryTracking = "MASTER YOUR WORKFLOW",
                                        heading = "How do you curate & build?",
                                        vectorKind = EditorialVectorKind.TARGET,
                                        options = ROLE_OPTIONS,
                                        selectedId = selectedRole,
                                        stepNumber = 1,
                                        totalSteps = 4,
                                        nextLabel = "NEXT",
                                        onSelect = { selectedRole = it },
                                        onNext = { currentStep = OnboardingStep.WIZARD_USE_CASE }
                                    )
                                }

                                OnboardingStep.WIZARD_USE_CASE -> {
                                    EditorialWizardStepContent(
                                        categoryTracking = "CONSISTENCY",
                                        heading = "What deserves a calmer home?",
                                        vectorKind = EditorialVectorKind.STABILITY,
                                        options = USE_CASE_OPTIONS,
                                        selectedId = selectedUseCase,
                                        stepNumber = 2,
                                        totalSteps = 4,
                                        nextLabel = "NEXT",
                                        onSelect = { selectedUseCase = it },
                                        onNext = { currentStep = OnboardingStep.WIZARD_SAVE_HABIT }
                                    )
                                }

                                OnboardingStep.WIZARD_SAVE_HABIT -> {
                                    EditorialWizardStepContent(
                                        categoryTracking = "CLARITY & RECALL",
                                        heading = "How do you save daily insights?",
                                        vectorKind = EditorialVectorKind.FOCUS_WAVES,
                                        options = SAVE_HABIT_OPTIONS,
                                        selectedId = selectedSaveHabit,
                                        stepNumber = 3,
                                        totalSteps = 4,
                                        nextLabel = "NEXT",
                                        onSelect = { selectedSaveHabit = it },
                                        onNext = { currentStep = OnboardingStep.WIZARD_WORKFLOW }
                                    )
                                }

                                OnboardingStep.WIZARD_WORKFLOW -> {
                                    EditorialWizardStepContent(
                                        categoryTracking = "VAULT ARCHITECTURE",
                                        heading = "Choose your starting plan",
                                        vectorKind = EditorialVectorKind.SHIELD,
                                        options = WORKFLOW_OPTIONS,
                                        selectedId = selectedWorkflow,
                                        stepNumber = 4,
                                        totalSteps = 4,
                                        nextLabel = "GET STARTED",
                                        onSelect = { selectedWorkflow = it },
                                        onNext = {
                                            onClearAuthError()
                                            onWizardCompleted(
                                                selectedRole,
                                                selectedUseCase,
                                                selectedSaveHabit,
                                                selectedWorkflow
                                            )
                                            currentStep = OnboardingStep.GOOGLE_AUTH
                                        }
                                    )
                                }

                                OnboardingStep.GOOGLE_AUTH -> {
                                    val roleTitle = ROLE_OPTIONS.find { it.id == selectedRole }?.title
                                    val useCaseTitle = USE_CASE_OPTIONS.find { it.id == selectedUseCase }?.title
                                    val habitTitle = SAVE_HABIT_OPTIONS.find { it.id == selectedSaveHabit }?.title
                                    val workflowTitle = WORKFLOW_OPTIONS.find { it.id == selectedWorkflow }?.title
                                    val chosenLabels = listOfNotNull(roleTitle, useCaseTitle, habitTitle, workflowTitle)

                                    GoogleOnlyLiquidLoginCardContent(
                                        isSignedIn = isSignedIn,
                                        userDisplayName = userDisplayName,
                                        userEmail = userEmail,
                                        authError = authError,
                                        chosenLabels = chosenLabels,
                                        onGoogleSignInClick = onGoogleSignInClick,
                                        onContinueSignedInClick = {
                                            onCompleteWizardAndSync(
                                                selectedRole,
                                                selectedUseCase,
                                                selectedSaveHabit,
                                                selectedWorkflow
                                            )
                                        },
                                        onContinueAsGuestClick = onContinueAsGuestClick
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Subtle Offline Demo preview link
                if (!isWelcomeSyncing) {
                    TextButton(
                        onClick = onExploreDemoClick,
                        modifier = Modifier.testTag("onboarding_explore_demo_button")
                    ) {
                        MinimalIcon(
                            type = MinimalIconType.PLAY_SQUIRCLE,
                            active = true,
                            tint = palette.accentSoftInk,
                            cutoutColor = palette.canvasTop,
                            size = 16.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Explore Offline Demo Workspace",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.secondaryInk
                        )
                    }
                } else {
                    Spacer(Modifier.height(44.dp))
                }
            }
        }
    }
}

@Composable
private fun EditorialWizardStepContent(
    categoryTracking: String,
    heading: String,
    vectorKind: EditorialVectorKind,
    options: List<WizardOption>,
    selectedId: String,
    stepNumber: Int,
    totalSteps: Int,
    nextLabel: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    val palette = LiquidTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Visual Card: Clean 28dp squircle container with centered minimal vector illustration
        EditorialIllustrationSquircle(
            kind = vectorKind,
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .testTag("editorial_illustration_card")
        )

        // 2. Editorial Typography: Muted uppercase category tracking + High-contrast serif heading
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = categoryTracking,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = palette.secondaryInk
            )
            Text(
                text = heading,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = InstrumentSerifFamily,
                    fontSize = 31.sp,
                    lineHeight = 36.sp
                ),
                color = palette.primaryInk
            )
        }

        // 3. Interactive Options List: 4 stacked 16dp tactile pills with left text & right '+' / checkmark
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { option ->
                EditorialTactileOptionPill(
                    option = option,
                    selected = selectedId == option.id,
                    onClick = { onSelect(option.id) }
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // 4. Bottom Navigation: Pagination dots (● ○ ○ ○) on left & solid pill [ NEXT ] / [ GET STARTED ] on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorialPaginationDots(
                currentStep = stepNumber,
                totalSteps = totalSteps,
                modifier = Modifier.testTag("wizard_pagination_dots")
            )

            TactilePrimaryPillButton(
                text = nextLabel,
                iconType = if (stepNumber == totalSteps) MinimalIconType.SPARKLE else MinimalIconType.ARROW_UP_RIGHT,
                onClick = onNext,
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 136.dp)
                    .testTag("wizard_next_button")
            )
        }
    }
}

@Composable
private fun EditorialIllustrationSquircle(
    kind: EditorialVectorKind,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    val squircleShape = remember { RoundedCornerShape(28.dp) }
    val bgBrush = remember(palette.recessedTrayBg, palette.cardSurfaceTop) {
        Brush.verticalGradient(
            listOf(
                palette.recessedTrayBg.copy(alpha = 0.92f),
                palette.cardSurfaceTop.copy(alpha = 0.56f)
            )
        )
    }
    val strokeInk = palette.primaryInk
    val mutedInk = palette.secondaryInk.copy(alpha = 0.48f)
    val accentInk = palette.accentCrimson

    Box(
        modifier = modifier
            .clip(squircleShape)
            .background(bgBrush)
            .border(1.dp, palette.specularRimOutline, squircleShape)
            .drawWithCache {
                val inset = 22.dp.toPx()
                val start = Offset(inset, 1.dp.toPx())
                val end = Offset((size.width - inset).coerceAtLeast(start.x), 1.dp.toPx())
                val sw = 1.2.dp.toPx()
                onDrawBehind {
                    drawLine(
                        color = palette.specularBorderTop,
                        start = start,
                        end = end,
                        strokeWidth = sw,
                        cap = StrokeCap.Round
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(84.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val primaryStroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val subtleStroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

            when (kind) {
                EditorialVectorKind.TARGET -> {
                    drawCircle(color = mutedInk, radius = w * 0.42f, center = Offset(cx, cy), style = subtleStroke)
                    drawCircle(color = strokeInk, radius = w * 0.27f, center = Offset(cx, cy), style = primaryStroke)
                    drawLine(
                        color = mutedInk,
                        start = Offset(cx, h * 0.04f),
                        end = Offset(cx, h * 0.22f),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = mutedInk,
                        start = Offset(cx, h * 0.78f),
                        end = Offset(cx, h * 0.96f),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = mutedInk,
                        start = Offset(w * 0.04f, cy),
                        end = Offset(w * 0.22f, cy),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = mutedInk,
                        start = Offset(w * 0.78f, cy),
                        end = Offset(w * 0.96f, cy),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = accentInk, radius = w * 0.075f, center = Offset(cx, cy))
                }

                EditorialVectorKind.STABILITY -> {
                    // Balanced editorial pillars + horizon baseline
                    drawLine(
                        color = mutedInk,
                        start = Offset(w * 0.12f, h * 0.78f),
                        end = Offset(w * 0.88f, h * 0.78f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawRoundRect(
                        color = strokeInk,
                        topLeft = Offset(w * 0.22f, h * 0.44f),
                        size = Size(w * 0.16f, h * 0.34f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                        style = primaryStroke
                    )
                    drawRoundRect(
                        color = strokeInk,
                        topLeft = Offset(w * 0.42f, h * 0.22f),
                        size = Size(w * 0.16f, h * 0.56f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                        style = primaryStroke
                    )
                    drawRoundRect(
                        color = mutedInk,
                        topLeft = Offset(w * 0.62f, h * 0.36f),
                        size = Size(w * 0.16f, h * 0.42f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                        style = subtleStroke
                    )
                    drawCircle(color = accentInk, radius = w * 0.055f, center = Offset(cx, h * 0.14f))
                }

                EditorialVectorKind.FOCUS_WAVES -> {
                    // Calm concentric ripple waves for clarity & recall
                    val waveOffsets = listOf(0.32f, 0.50f, 0.68f)
                    waveOffsets.forEachIndexed { idx, yFactor ->
                        val y = h * yFactor
                        val amp = h * (0.09f - idx * 0.015f)
                        val wavePath = Path().apply {
                            moveTo(w * 0.14f, y)
                            cubicTo(
                                w * 0.32f, y - amp,
                                w * 0.44f, y + amp,
                                cx, y
                            )
                            cubicTo(
                                w * 0.58f, y - amp,
                                w * 0.70f, y + amp,
                                w * 0.86f, y
                            )
                        }
                        drawPath(
                            path = wavePath,
                            color = if (idx == 1) strokeInk else mutedInk,
                            style = if (idx == 1) primaryStroke else subtleStroke
                        )
                    }
                    drawCircle(color = accentInk, radius = w * 0.055f, center = Offset(cx, cy))
                }

                EditorialVectorKind.SHIELD -> {
                    // Geometric editorial vault shield
                    val shieldPath = Path().apply {
                        moveTo(cx, h * 0.12f)
                        lineTo(w * 0.80f, h * 0.24f)
                        lineTo(w * 0.80f, h * 0.50f)
                        cubicTo(
                            w * 0.80f, h * 0.72f,
                            w * 0.62f, h * 0.84f,
                            cx, h * 0.90f
                        )
                        cubicTo(
                            w * 0.38f, h * 0.84f,
                            w * 0.20f, h * 0.72f,
                            w * 0.20f, h * 0.50f
                        )
                        lineTo(w * 0.20f, h * 0.24f)
                        close()
                    }
                    drawPath(path = shieldPath, color = strokeInk, style = primaryStroke)
                    drawCircle(color = mutedInk, radius = w * 0.14f, center = Offset(cx, cy), style = subtleStroke)
                    drawCircle(color = accentInk, radius = w * 0.055f, center = Offset(cx, cy))
                }
            }
        }
    }
}

@Composable
private fun EditorialTactileOptionPill(
    option: WizardOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LiquidTheme.palette
    val pillShape = remember { RoundedCornerShape(16.dp) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(stiffness = 460f, dampingRatio = 0.74f),
        label = "editorialOptionScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) palette.accentSoftBg else palette.recessedTrayBg,
        label = "editorialOptionBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) palette.accentCrimson else palette.specularRimOutline,
        label = "editorialOptionBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (selected) 8.dp else 0.dp,
                shape = pillShape,
                ambientColor = palette.accentCrimson.copy(alpha = 0.28f),
                spotColor = palette.accentCrimson.copy(alpha = 0.28f)
            )
            .clip(pillShape)
            .background(bgColor)
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                color = borderColor,
                shape = pillShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag("wizard_option_${option.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.primaryInk
            )
            Text(
                text = option.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right-aligned '+' stroke icon (unselected) or filled checkmark circle (selected)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (selected) palette.accentCrimson else palette.frostedPillBg)
                .border(
                    width = 1.dp,
                    color = if (selected) palette.accentCrimson else palette.specularRimOutline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                MinimalIcon(
                    type = MinimalIconType.CHECK,
                    active = true,
                    tint = Color.White,
                    size = 15.dp
                )
            } else {
                Canvas(modifier = Modifier.size(12.dp)) {
                    val strokeW = 1.6.dp.toPx()
                    val half = size.width / 2f
                    drawLine(
                        color = palette.secondaryInk,
                        start = Offset(half, 0f),
                        end = Offset(half, size.height),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = palette.secondaryInk,
                        start = Offset(0f, half),
                        end = Offset(size.width, half),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorialPaginationDots(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val palette = LiquidTheme.palette
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (idx in 1..totalSteps) {
            val active = idx == currentStep
            val completed = idx < currentStep
            val dotWidth by animateDpAsState(
                targetValue = if (active) 24.dp else 8.dp,
                animationSpec = spring(stiffness = 420f, dampingRatio = 0.78f),
                label = "paginationDotWidth"
            )
            val dotColor by animateColorAsState(
                targetValue = when {
                    active -> palette.primaryInk
                    completed -> palette.accentCrimson.copy(alpha = 0.75f)
                    else -> palette.secondaryInk.copy(alpha = 0.28f)
                },
                label = "paginationDotColor"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoogleOnlyLiquidLoginCardContent(
    isSignedIn: Boolean,
    userDisplayName: String,
    userEmail: String?,
    authError: String?,
    chosenLabels: List<String>,
    onGoogleSignInClick: () -> Unit,
    onContinueSignedInClick: () -> Unit,
    onContinueAsGuestClick: () -> Unit
) {
    val palette = LiquidTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("google_login_page"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AppLogo(
                size = 86.dp,
                cornerRadius = 24.dp,
                testTag = "google_login_app_logo"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "GOOGLE CLOUD ACCOUNT",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = palette.accentSoftInk
            )
            Text(
                text = if (isSignedIn) "Welcome back, ${userDisplayName.ifBlank { "Creator" }}" else "Sign in with Google",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = InstrumentSerifFamily,
                    fontSize = 32.sp,
                    lineHeight = 36.sp
                ),
                color = palette.primaryInk
            )
            Text(
                text = if (isSignedIn) {
                    "Connected as ${userEmail ?: "Google Account"}. Sync your configured vault and enter your workspace."
                } else {
                    "One-tap Google sign-in creates or unlocks your encrypted AI Library and syncs bookmarks, folders & themes seamlessly."
                },
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryInk
            )
        }

        if (chosenLabels.isNotEmpty()) {
            RecessedFramingBezel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                innerPadding = PaddingValues(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your Tailored Blueprint",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryInk
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chosenLabels.forEach { label ->
                            BlueprintChip(label)
                        }
                    }
                }
            }
        }

        if (!authError.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CrimsonAccent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, CrimsonAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = authError,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.primaryInk,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        if (isSignedIn) {
            TactilePrimaryPillButton(
                text = "Sync & Enter Workspace",
                iconType = MinimalIconType.CLOUD_SYNC,
                onClick = onContinueSignedInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_enter_signed_in_button")
            )
        } else {
            GoogleSignInLiquidPillButton(
                text = "Continue with Google",
                onClick = onGoogleSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_google_signin_button")
            )

            OutlinedButton(
                onClick = onContinueAsGuestClick,
                shape = RoundedCornerShape(9999.dp),
                border = BorderStroke(1.dp, palette.specularRimOutline),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.recessedTrayBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboarding_guest_button")
            ) {
                Text(
                    text = "Continue as Guest (Local Vault)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.primaryInk
                )
            }
        }

        Text(
            text = "Protected by Google OAuth • Returning users skip onboarding automatically.",
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondaryInk
        )
    }
}

@Composable
private fun WelcomeSyncTransitionCard(
    userDisplayName: String,
    userEmail: String?
) {
    val palette = LiquidTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 38.dp)
            .testTag("welcome_sync_loading_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppLogo(
                size = 82.dp,
                cornerRadius = 24.dp,
                testTag = "welcome_sync_app_logo"
            )
            CircularProgressIndicator(
                modifier = Modifier.size(94.dp),
                strokeWidth = 2.2.dp,
                color = palette.accentCrimson.copy(alpha = 0.72f)
            )
        }
        Text(
            text = "Welcome, ${userDisplayName.ifBlank { "Creator" }}!",
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = InstrumentSerifFamily,
                fontSize = 30.sp
            ),
            color = palette.primaryInk
        )
        Text(
            text = "Syncing your Google profile, workspace blueprint & cloud vault${userEmail?.let { " ($it)" }.orEmpty()}...",
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryInk
        )
    }
}

@Composable
private fun BlueprintChip(text: String) {
    val palette = LiquidTheme.palette
    Surface(
        shape = RoundedCornerShape(9999.dp),
        color = palette.frostedPillBg,
        border = BorderStroke(1.dp, palette.specularRimOutline)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = palette.primaryInk,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun FrostedOnboardingGlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LiquidTheme.palette
    val cardShape = remember { RoundedCornerShape(36.dp) }
    val glassFill = remember(palette.cardSurfaceTop, palette.cardSurfaceBottom) {
        Brush.verticalGradient(listOf(palette.cardSurfaceTop, palette.cardSurfaceBottom))
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (palette.isDark) 26.dp else 20.dp,
                shape = cardShape,
                ambientColor = if (palette.isDark) Color(0x90000000) else Color(0x1E2D261E),
                spotColor = if (palette.isDark) Color(0x90000000) else Color(0x1E2D261E)
            )
            .clip(cardShape)
            .background(glassFill)
            .border(1.dp, palette.specularRimOutline, cardShape)
            .drawWithCache {
                val inset = 30.dp.toPx()
                val start = Offset(inset, 1.dp.toPx())
                val end = Offset((size.width - inset).coerceAtLeast(start.x), 1.dp.toPx())
                val sw = 1.4.dp.toPx()
                onDrawBehind {
                    drawLine(
                        color = palette.specularBorderTop,
                        start = start,
                        end = end,
                        strokeWidth = sw,
                        cap = StrokeCap.Round
                    )
                }
            }
    ) {
        content()
    }
}

/**
 * Reusable Liquid Glass Google-Only Sign-In Pill Button with 4-color Google 'G' mark
 * and smooth spring scale feedback.
 */
@Composable
fun GoogleSignInLiquidPillButton(
    text: String = "Continue with Google",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LiquidTheme.palette
    val shape = remember { RoundedCornerShape(9999.dp) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(stiffness = 480f, dampingRatio = 0.72f),
        label = "googlePillScale"
    )
    val bgBrush = remember(enabled, palette.isDark, palette.frostedPillBg, palette.primaryButtonBg) {
        if (palette.isDark) {
            Brush.horizontalGradient(listOf(SoftCreamWhite, Color(0xFFE3DDD3)))
        } else {
            Brush.horizontalGradient(listOf(palette.primaryButtonBg, Color(0xFF2E2A26)))
        }
    }
    val contentColor = if (palette.isDark) SoftCreamInk else palette.primaryButtonContent

    Box(
        modifier = modifier
            .height(52.dp)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = shape,
                ambientColor = if (palette.isDark) CrimsonAccent.copy(alpha = 0.24f) else Color(0x28000000),
                spotColor = if (palette.isDark) CrimsonAccent.copy(alpha = 0.24f) else Color(0x28000000)
            )
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, palette.specularBorderTop.copy(alpha = 0.7f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                GoogleBrandMark(size = 17.dp)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GoogleBrandMark(size: Dp = 18.dp) {
    Spacer(
        modifier = Modifier
            .size(size)
            .drawWithCache {
                val u = this.size.width / 24f
                val strokeWidth = 3.6f * u
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                val arcRadius = 8.4f * u
                val topLeft = Offset(12f * u - arcRadius, 12f * u - arcRadius)
                val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                val barStart = Offset(12f * u, 12f * u)
                val barEnd = Offset(20.6f * u, 12f * u)
                val blue = Color(0xFF4285F4)
                val red = Color(0xFFEA4335)
                val yellow = Color(0xFFFBBC05)
                val green = Color(0xFF34A853)

                onDrawBehind {
                    drawArc(
                        color = red,
                        startAngle = -142f,
                        sweepAngle = 98f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                    drawArc(
                        color = yellow,
                        startAngle = 148f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                    drawArc(
                        color = green,
                        startAngle = 38f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                    drawArc(
                        color = blue,
                        startAngle = -5f,
                        sweepAngle = 45f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                    drawLine(
                        color = blue,
                        start = barStart,
                        end = barEnd,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Butt
                    )
                }
            }
    )
}
