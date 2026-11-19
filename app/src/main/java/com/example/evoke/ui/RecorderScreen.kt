package com.example.evoke.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.evoke.ui.components.HeaderSection
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.example.animation.LiquidBlobAnimation
import com.example.animation.VoiceRecorderAnimation
import com.example.evoke.ui.theme.OutfitFont
import com.example.evoke.ui.components.ShadowButton
import com.example.evoke.ui.theme.AppTheme
import com.example.evoke.ui.theme.GreenTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import com.example.evoke.R
import com.example.evoke.ui.components.AppLanguageDirection
import com.example.evoke.ui.components.isUrduSelected
import com.example.evoke.voice.RecorderMode
import com.example.evoke.voice.RecorderPhase
import com.example.evoke.voice.RecorderUiState
import com.example.evoke.voice.VoiceConfig
import com.example.evoke.voice.VoiceSessionViewModel
import androidx.compose.ui.text.style.TextDirection

private fun Modifier.blurIfSupported(radius: Dp): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius = radius)
    } else {
        this
    }
}

/**
 * The blur/glow-heavy [VoiceRecorderAnimation] uses Modifier.blur (via its
 * own blurIfSupported helper) and layered translucent gradients that are
 * tuned to look right with real blur behind them. Below API 31 that blur
 * silently becomes a no-op, so those rings would render sharp-edged and
 * flat instead of glassy — it still "works", it just doesn't look right.
 *
 * [LiquidBlobAnimation] was built from the ground up with no blur/RenderEffect
 * dependency at all, so it looks correct and intentional on every version
 * below that cutoff.
 *
 * This flag is the single switch point for that decision — check it once
 * here rather than duplicating the SDK check anywhere else in the screen.
 */
private val supportsBlurAndGlow: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Error text colour that stays readable on the dark card. */
private val MessageErrorColor = Color(0xFFFFB4AB)

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/**
 * Recorder used for:
 * - [RecorderMode.PATIENT_SAMPLE]  (Profile)        — 3 voice samples, script below the recorder
 * - [RecorderMode.PARTNER_SAMPLE]  (Not recognised) — 3 voice samples, script below the recorder
 * - [RecorderMode.CONVERSATION]    (Recognised / Not recognised) — no script, max 30 min
 *
 * The recorder card itself (size, position, animation, button) is the same in
 * every mode; for voice samples the card extends downwards with the script,
 * and the page scrolls.
 *
 * Stateless: the recording itself lives in VoiceSessionViewModel (see AppNavigation).
 *
 * Button by phase: READY "Start recording" → RECORDING "Done" (→ "Save recording?"
 * popup) → STOPPED "Save" → PROCESSING spinner → DONE "Record sample n" /
 * "Try again" / "Done".
 */
@Composable
fun RecordScreen(
    state: RecorderUiState = RecorderUiState(),
    elapsedMs: Long = 0L,
    onBack: () -> Unit = {},
    onStartClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    onNextSampleClick: () -> Unit = {},
    onKeepAnywayClick: () -> Unit = {},
    onFinishClick: () -> Unit = {}
) {
    val appColors = AppTheme.colors
    val context = LocalContext.current
    val iconShadowColor = if (supportsBlurAndGlow) Color.White else Color.Black

    val showScript = state.mode != RecorderMode.CONVERSATION
    val isRecording = state.phase == RecorderPhase.RECORDING
    val maxMs = if (state.mode == RecorderMode.CONVERSATION)
        VoiceConfig.MAX_CONVERSATION_RECORDING_MS
    else
        VoiceConfig.MAX_ENROLL_RECORDING_MS

    // ── microphone permission ────────────────────────────────────────────────
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) onStartClick()
    }
    val startWithPermission: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            permissionDenied = false
            onStartClick()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // While a recording exists, system back asks "Don't save recording?" first.
    BackHandler(enabled = isRecording || state.phase == RecorderPhase.STOPPED) { onBack() }

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // insets already handled at root
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // ── same sizes as the original recorder card ──────────────────────
            val cardWidth = maxWidth * 0.88f
            val cardHeight = minOf(
                cardWidth * 1.50f,
                (maxHeight - 260.dp - 56.dp) * 0.95f
            )
            val voiceAnimationSize = cardWidth * 1.20f
            val blobAnimationSize = cardWidth * 1.00f

            // Calculate spacing
            val headerHeight = 260.dp
            val bottomPadding = 28.dp
            val headerGap = (-25).dp   // gap under the header; raise it if it feels too tight

            Box(
                modifier = Modifier.fillMaxSize()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // voice samples: the card is taller than the screen (script below)
                        .verticalScroll(rememberScrollState())
                ) {

                    // Header space + gap (headerGap may be negative to pull the card up)
                    Spacer(modifier = Modifier.height((headerHeight + headerGap).coerceAtLeast(0.dp)))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-20).dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // ── Recording Card (extends down for the script) ───────
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(cardWidth)
                                .shadow(
                                    elevation = 40.dp,
                                    shape = RoundedCornerShape(62.dp),
                                    ambientColor = Color.DarkGray.copy(alpha = 0.8f),
                                    spotColor = Color.DarkGray.copy(alpha = 0.9f)
                                )
                                .background(
                                    appColors.pagesText,
                                    RoundedCornerShape(62.dp)
                                )
                        ) {

                            // ── Recorder part: exactly the original card ──────
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(cardHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth(1f)
                                            .fillMaxHeight(0.7f)
                                    ) {
                                        // API 31+ gets the blur/glow version; older
                                        // versions get the no-blur liquid blob.
                                        if (supportsBlurAndGlow) {
                                            VoiceRecorderAnimation(
                                                modifier = Modifier.size(voiceAnimationSize),
                                                isAnimating = isRecording
                                            )
                                        } else {
                                            LiquidBlobAnimation(
                                                modifier = Modifier.size(blobAnimationSize),
                                                isAnimating = isRecording
                                            )
                                        }

                                        Box(contentAlignment = Alignment.Center) {
                                            // White shadow - same icon shape, blurred, drawn behind
                                            Icon(
                                                painter = painterResource(id = R.drawable.recording_icon),
                                                contentDescription = null, // decorative
                                                tint = iconShadowColor.copy(alpha = 0.4f),
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .blurIfSupported(6.dp)
                                            )
                                            // Actual microphone icon
                                            Icon(
                                                painter = painterResource(id = R.drawable.recording_icon),
                                                contentDescription = stringResource(R.string.recording),
                                                tint = appColors.popupText,
                                                modifier = Modifier.size(50.dp)
                                            )
                                        }
                                    }
                                }

                                // Status + button, at the original button position
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = cardHeight * 0.10f)
                                ) {
                                    StatusText(
                                        state = state,
                                        elapsedMs = elapsedMs,
                                        maxMs = maxMs,
                                        permissionDenied = permissionDenied,
                                        modifier = Modifier.width(cardWidth * 0.84f)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    ShadowButton(
                                        width = cardWidth * 0.70f,
                                        height = 48.dp,
                                        color = appColors.popupText,
                                        cornerRadius = 24.dp,
                                        shadowColor = Color.White,
                                        onClick = {
                                            when (state.phase) {
                                                RecorderPhase.READY -> startWithPermission()
                                                RecorderPhase.RECORDING -> onDoneClick()
                                                RecorderPhase.STOPPED -> onSaveClick()
                                                RecorderPhase.PROCESSING -> Unit
                                                RecorderPhase.DONE -> when {
                                                    state.canRetry -> onRetryClick()
                                                    state.canRecordAnother -> onNextSampleClick()
                                                    else -> onFinishClick()
                                                }
                                            }
                                        }
                                    ) {
                                        if (state.phase == RecorderPhase.PROCESSING) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Text(
                                                text = buttonText(state),
                                                color = Color.White,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = OutfitFont
                                            )
                                        }
                                    }

                                    // Visitor sample refused as "sounds like the patient":
                                    // the caregiver can keep it if it is a different person.
                                    if (state.phase == RecorderPhase.DONE && state.canKeepAnyway) {
                                        TextButton(onClick = onKeepAnywayClick) {
                                            Text(
                                                text = stringResource(R.string.voice_keep_anyway),
                                                color = appColors.popupText,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = OutfitFont,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // ── Script part (voice samples only) ──────────────
                            if (showScript) {
                                ScriptSection(state = state)
                            }
                        }
                    }

                    // Bottom padding
                    Spacer(modifier = Modifier.height(bottomPadding))
                }

                HeaderSection(
                    title = stringResource(R.string.record),
                    spacing = 68.dp,
                    bottomspace = 40.dp,
                    onBack = { onBack() }
                )
            }
        }
    }
}

@Composable
private fun buttonText(state: RecorderUiState): String = when (state.phase) {
    RecorderPhase.READY -> stringResource(R.string.voice_start_recording)
    RecorderPhase.RECORDING -> stringResource(R.string.done_button)
    RecorderPhase.STOPPED -> stringResource(R.string.save_popup)
    RecorderPhase.PROCESSING -> stringResource(R.string.voice_processing)
    RecorderPhase.DONE -> when {
        state.canRetry -> stringResource(R.string.voice_try_again)
        state.canRecordAnother -> stringResource(R.string.voice_record_next, state.currentSample)
        else -> stringResource(R.string.done_button)
    }
}

/** Below the recorder: sample counter, instruction and the text to read aloud. */
@Composable
private fun ScriptSection(state: RecorderUiState) {
    val isUrdu = isUrduSelected()

    // Urdu: right-to-left, with the taller line height the Urdu font needs.
    AppLanguageDirection {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp, end = 26.dp, top = 4.dp, bottom = 46.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            )

            Spacer(modifier = Modifier.height(18.dp))

            // "Voice sample 2 of 3"
            Text(
                text = stringResource(
                    R.string.voice_sample_progress,
                    state.currentSample,
                    VoiceSessionViewModel.SAMPLES_PER_SESSION
                ),
                color = AppTheme.colors.popupText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFont,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    if (state.mode == RecorderMode.PATIENT_SAMPLE)
                        R.string.voice_patient_instruction
                    else
                        R.string.voice_partner_instruction
                ),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 17.sp,
                lineHeight = if (isUrdu) 32.sp else 24.sp,
                fontFamily = OutfitFont,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // The script itself
            val scriptRes = when (state.currentSample) {
                1 -> R.string.voice_sample_script_en
                2 -> R.string.voice_sample_script_ur
                else -> R.string.voice_sample_script_mix
            }
            val scriptIsUrdu = state.currentSample >= 2          // Urdu glyphs need the taller line height
            val scriptDirection = if (state.currentSample == 2) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides scriptDirection) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    if (state.currentSample >= 3) {
                        MixedScript(stringResource(scriptRes))
                    } else {
                        Text(
                            text = stringResource(scriptRes),
                            color = Color.White,
                            fontSize = 22.sp,
                            lineHeight = if (scriptIsUrdu) 44.sp else 34.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = OutfitFont,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusText(
    state: RecorderUiState,
    elapsedMs: Long,
    maxMs: Long,
    permissionDenied: Boolean,
    modifier: Modifier = Modifier
) {
    val message = state.message
    val text: String
    var isError = false

    when {
        permissionDenied && state.phase == RecorderPhase.READY -> {
            text = stringResource(R.string.voice_mic_permission)
            isError = true
        }
        state.phase == RecorderPhase.READY ->
            text = stringResource(
                if (state.mode == RecorderMode.CONVERSATION)
                    R.string.voice_conversation_instruction
                else
                    R.string.voice_press_start
            )
        state.phase == RecorderPhase.RECORDING ->
            text = stringResource(
                R.string.voice_recording_time,
                formatDuration(elapsedMs),
                formatDuration(maxMs)
            )
        state.phase == RecorderPhase.STOPPED ->
            text = stringResource(R.string.voice_recording_stopped, formatDuration(elapsedMs))
        state.phase == RecorderPhase.PROCESSING ->
            text = stringResource(R.string.voice_processing)
        message != null -> {
            text = stringResource(message.res, *message.args.toTypedArray())
            isError = message.isError
        }
        else -> text = ""
    }

    Text(
        text = text,
        color = if (isError) MessageErrorColor else Color.White,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontFamily = OutfitFont,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

/** Mixed Urdu/English script: one sentence per line, RTL, English phrases kept intact. */
@Composable
private fun MixedScript(text: String) {
    val latinRun = Regex("[A-Za-z]+(?:\\s+[A-Za-z]+)*")
    val lines = text.split("۔")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { sentence ->
            // \u2066 = LRI, \u2069 = PDI: each English phrase becomes an isolated LTR block
            latinRun.replace(sentence) { "\u2066${it.value}\u2069" } + "۔"
        }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        lines.forEach { line ->
            Text(
                text = line,
                color = Color.White,
                fontSize = 22.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = OutfitFont,
                textAlign = TextAlign.Right,
                style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RecorderPreview() {
    GreenTheme {
        RecordScreen(state = RecorderUiState(mode = RecorderMode.PATIENT_SAMPLE))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RecorderConversationPreview() {
    GreenTheme {
        RecordScreen(
            state = RecorderUiState(mode = RecorderMode.CONVERSATION, phase = RecorderPhase.RECORDING),
            elapsedMs = 42_000L
        )
    }
}
