package com.innovatex.auracast.ui.screens

import com.innovatex.auracast.R
import com.innovatex.auracast.ui.i18n.appString
import com.innovatex.auracast.ui.i18n.localizedRouteDestination
import com.innovatex.auracast.ui.i18n.localizedCoverageSummary
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.innovatex.auracast.data.SampleData
import com.innovatex.auracast.data.TransitRoute
import com.innovatex.auracast.ui.theme.AppearanceMode
import com.innovatex.auracast.ui.theme.LocalAccessibilitySettings

@Composable
fun RouteScreen(
    modifier: Modifier = Modifier,
    routes: List<TransitRoute> = SampleData.routes,
    onContinue: (TransitRoute) -> Unit = {}
) {
    var selectedId by rememberSaveable {
        mutableStateOf(routes.firstOrNull { it.coveredStopCount > 0 }?.id)
    }
    val selected = routes.firstOrNull { it.id == selectedId }
    val isColourful = LocalAccessibilitySettings.current.appearance == AppearanceMode.COLOURFUL
    val colors = MaterialTheme.colorScheme

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(appString(R.string.route_step), style = MaterialTheme.typography.labelLarge)
            Text(
                appString(R.string.route_select),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                appString(R.string.route_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                routes.forEach { route ->
                    val isSelected = route.id == selectedId
                    val borderWidth = if (isSelected) {
                        2.dp
                    } else {
                        1.dp
                    }
                    val borderColor = if (isSelected && isColourful) {
                        colors.primary
                    } else if (isSelected) {
                        colors.onSurface
                    } else {
                        colors.outlineVariant
                    }
                    val badgeColor = if (isColourful) {
                        if (isSelected) {
                            colors.secondary
                        } else {
                            colors.tertiary
                        }
                    } else {
                        colors.primary
                    }
                    val badgeTextColor = if (isColourful) {
                        if (isSelected) {
                            colors.onSecondary
                        } else {
                            colors.onTertiary
                        }
                    } else {
                        colors.onPrimary
                    }
                    val containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                    val selectionIcon = if (isSelected) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.RadioButtonUnchecked
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { selectedId = route.id }
                            ),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(borderWidth, borderColor),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = badgeColor
                                )
                            ) {
                                Text(
                                    route.routeNumber,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = badgeTextColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    localizedRouteDestination(route),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    localizedCoverageSummary(route),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (route.coveredStopCount == 0) {
                                    Text(
                                        appString(R.string.no_broadcasts_configured),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                selectionIcon,
                                contentDescription = null,
                                tint = if (isSelected && isColourful) {
                                    colors.primary
                                } else {
                                    colors.onSurface
                                },
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Button(
                onClick = {
                    if (selected != null) {
                        onContinue(selected)
                    }
                },
                enabled = selected != null && selected.coveredStopCount > 0,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(appString(R.string.start_journey))
            }
        }
    }
}
