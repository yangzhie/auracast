package com.innovatex.auracast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innovatex.auracast.ui.theme.AlertRed
import com.innovatex.auracast.ui.theme.Ink2
import com.innovatex.auracast.ui.theme.Muted
import com.innovatex.auracast.ui.theme.OnSignalAmber
import com.innovatex.auracast.ui.theme.ReceivingGreen
import com.innovatex.auracast.ui.theme.SignalAmber

data class BandStyle(
    val background: Color,
    val content: Color,
    val borderColor: Color? = null,
    val hollowDot: Boolean = false
)

@Composable
fun StatusBand(
    kicker: String,
    headline: String,
    detail: String,
    style: BandStyle,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (style.borderColor != null) {
                    Modifier.border(1.dp, style.borderColor, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .background(style.background)
            .padding(18.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (style.hollowDot) {
                Box(Modifier.size(9.dp).border(2.dp, style.content, CircleShape))
            } else {
                Box(Modifier.size(9.dp).clip(CircleShape).background(style.content))
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = kicker.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = style.content,
                letterSpacing = 1.6.sp
            )
        }

        Spacer(Modifier.height(9.dp))

        Text(
            text = headline,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = style.content
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = style.content.copy(alpha = 0.92f)
        )
    }
}

object BandStyles {
    val Receiving = BandStyle(ReceivingGreen, Color.White)
    val Searching = BandStyle(SignalAmber, OnSignalAmber)
    val Travelling = BandStyle(Color(0xFFEDEFF6), Ink2, borderColor = Color(0xFFC4CCE4))
    val NoCoverage = BandStyle(Color(0xFFE3E7F1), Ink2, borderColor = Muted, hollowDot = true)
    val Fault = BandStyle(AlertRed, Color.White)
}