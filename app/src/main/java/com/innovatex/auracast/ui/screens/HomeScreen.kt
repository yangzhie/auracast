package com.innovatex.auracast.ui.screens

import com.innovatex.auracast.R
import com.innovatex.auracast.ui.i18n.appString
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.innovatex.auracast.ui.theme.AppearanceMode
import com.innovatex.auracast.ui.theme.LocalAccessibilitySettings
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPlanJourney: () -> Unit = {},
    onHowItWorks: () -> Unit = {},
    onOpenAccessibility: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val isColourful = LocalAccessibilitySettings.current.appearance == AppearanceMode.COLOURFUL

    val heroBrush: Brush = if (isColourful) {
        Brush.linearGradient(
            colors = listOf(colors.primary, colors.tertiary, colors.secondary)
        )
    } else {
        SolidColor(colors.primary)
    }
    val brandingColor = if (isColourful) {
        colors.secondary
    } else {
        colors.primary
    }
    val accessibilityAccent = if (isColourful) {
        colors.tertiary
    } else {
        colors.onBackground
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(46.dp)
                    .background(brandingColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "A",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isColourful) {
                        colors.onSecondary
                    } else {
                        colors.onPrimary
                    }
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                "AURACAST",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = colors.onBackground
            )
        }

        Spacer(Modifier.height(56.dp))
        Text(
            appString(R.string.home_title),
            fontSize = 43.sp,
            lineHeight = 49.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            color = colors.onBackground
        )
        Spacer(Modifier.height(14.dp))
        Text(
            appString(R.string.home_subtitle),
            fontSize = 17.sp,
            lineHeight = 25.sp,
            color = colors.onBackground
        )
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(205.dp)
                .background(brush = heroBrush, shape = RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    appString(R.string.hero_caption),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = colors.onPrimary
                )
                SoundWave(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.onPrimary
                )
                Text(
                    appString(R.string.hero_footer),
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onPrimary
                )
            }
        }

        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onPlanJourney,
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(appString(R.string.plan_journey), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("→", fontSize = 25.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onOpenAccessibility,
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, accessibilityAccent),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accessibilityAccent)
        ) {
            Text(appString(R.string.accessibility), fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SoundWave(modifier: Modifier = Modifier, color: Color) {
    val waveHeights = listOf(
        18.dp, 32.dp, 50.dp, 72.dp, 90.dp,
        72.dp, 50.dp, 32.dp, 18.dp
    )

    Row(
        modifier = modifier.height(94.dp).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(7.dp)
                    .height(height)
                    .background(color, RoundedCornerShape(100.dp))
            )
        }
    }
}
