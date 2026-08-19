package com.innovatex.auracast.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.innovatex.auracast.data.SampleData
import com.innovatex.auracast.ui.theme.Ink
import com.innovatex.auracast.ui.theme.OnSignalAmber
import com.innovatex.auracast.ui.theme.SignalAmber

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPlanJourney: () -> Unit = {},
    onHowItWorks: () -> Unit = {}
) {
    val fittedStops = remember {
        SampleData.routes
            .flatMap { it.stops }
            .filter { it.hasAuracast }
            .distinctBy { it.id }
            .size
    }
    val liveRoutes = remember {
        SampleData.routes.count { it.coveredStopCount > 0 }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        Column(modifier = modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
            ) {
                Spacer(Modifier.height(28.dp))

                BroadcastMark(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(190.dp)
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "AURACAST COMPANION",
                    style = MaterialTheme.typography.labelMedium,
                    color = SignalAmber,
                    letterSpacing = 2.4.sp
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Never miss\nyour stop.",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 42.sp
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Announcements go straight to your hearing aids, " +
                            "switching stop to stop on their own. No scanning, no tapping.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.66f)
                )

                Spacer(Modifier.height(30.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    HeroStat(value = fittedStops.toString(), label = "stops fitted")
                    HeroStat(value = liveRoutes.toString(), label = "routes live")
                }

                Spacer(Modifier.height(28.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp)) {
                Button(
                    onClick = onPlanJourney,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalAmber,
                        contentColor = OnSignalAmber
                    )
                ) {
                    Text(
                        text = "Plan a journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onHowItWorks,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.85f)
                    )
                ) {
                    Text("How this works")
                }
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun BroadcastMark(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motionEnabled = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) != 0f
    }

    val transition = rememberInfiniteTransition(label = "broadcast")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val phase = if (motionEnabled) animatedPhase else 0.45f

    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val maxRadius = size.minDimension / 2f

        listOf(0f, 0.34f, 0.67f).forEach { offset ->
            val p = (phase + offset) % 1f
            val radius = lerp(maxRadius * 0.2f, maxRadius, p)
            val alpha = (1f - p) * 0.6f

            drawCircle(
                color = SignalAmber.copy(alpha = alpha),
                radius = radius,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        drawCircle(color = SignalAmber, radius = maxRadius * 0.12f)
    }
}