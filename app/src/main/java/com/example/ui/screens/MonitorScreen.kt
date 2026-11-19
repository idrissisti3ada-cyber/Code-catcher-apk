package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.MonitoredApp
import com.example.ui.MainUiState
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.InactiveGrey
import com.example.ui.theme.InstagramAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WhatsAppAccent

@Composable
fun MonitorScreen(
    uiState: MainUiState,
    onToggleApp: (MonitoredApp) -> Unit,
    onToggleMasterMonitoring: () -> Unit,
    onOpenSetupGuide: () -> Unit,
    onCopyCodeAgain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header + Offline Privacy Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Hero Status Banner
            MonitoringHeroCard(
                hasAnyAppSelected = uiState.settings.hasAnyAppSelected,
                isMonitoringActive = uiState.settings.isMonitoringActive,
                onToggleMasterMonitoring = onToggleMasterMonitoring
            )

            // Permissions Status Card
            PermissionHealthBanner(
                missingCount = uiState.permissions.missingCount,
                allReady = uiState.permissions.allRecommendedGranted,
                onOpenSetupGuide = onOpenSetupGuide
            )

            // Section Title
            Text(
                text = stringResource(R.string.section_apps_to_watch),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Two Toggle Cards: WhatsApp & Instagram
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppToggleCard(
                        title = stringResource(R.string.whatsapp_title),
                        subtitle = stringResource(R.string.whatsapp_subtitle),
                        packagesLabel = stringResource(R.string.whatsapp_packages),
                        icon = Icons.Filled.Forum,
                        accentColor = WhatsAppAccent,
                        isEnabled = uiState.settings.whatsAppEnabled,
                        testTag = "whatsapp_toggle_card",
                        onToggle = { onToggleApp(MonitoredApp.WHATSAPP) },
                        modifier = Modifier.weight(1f)
                    )
                    AppToggleCard(
                        title = stringResource(R.string.instagram_title),
                        subtitle = stringResource(R.string.instagram_subtitle),
                        packagesLabel = stringResource(R.string.instagram_packages),
                        icon = Icons.Filled.AlternateEmail,
                        accentColor = InstagramAccent,
                        isEnabled = uiState.settings.instagramEnabled,
                        testTag = "instagram_toggle_card",
                        onToggle = { onToggleApp(MonitoredApp.INSTAGRAM) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppToggleCard(
                        title = stringResource(R.string.whatsapp_title),
                        subtitle = stringResource(R.string.whatsapp_subtitle),
                        packagesLabel = stringResource(R.string.whatsapp_packages),
                        icon = Icons.Filled.Forum,
                        accentColor = WhatsAppAccent,
                        isEnabled = uiState.settings.whatsAppEnabled,
                        testTag = "whatsapp_toggle_card",
                        onToggle = { onToggleApp(MonitoredApp.WHATSAPP) }
                    )
                    AppToggleCard(
                        title = stringResource(R.string.instagram_title),
                        subtitle = stringResource(R.string.instagram_subtitle),
                        packagesLabel = stringResource(R.string.instagram_packages),
                        icon = Icons.Filled.AlternateEmail,
                        accentColor = InstagramAccent,
                        isEnabled = uiState.settings.instagramEnabled,
                        testTag = "instagram_toggle_card",
                        onToggle = { onToggleApp(MonitoredApp.INSTAGRAM) }
                    )
                }
            }

            // Latest Caught Code Preview Card
            val latestCode = uiState.history.firstOrNull()
            AnimatedVisibility(visible = latestCode != null) {
                if (latestCode != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LATEST CAUGHT • ${latestCode.sourceApp.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = latestCode.code,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            FilledTonalButton(
                                onClick = { onCopyCodeAgain(latestCode.code) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("latest_copy_again_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_copy_again))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MonitoringHeroCard(
    hasAnyAppSelected: Boolean,
    isMonitoringActive: Boolean,
    onToggleMasterMonitoring: () -> Unit
) {
    val glowColor = when {
        !hasAnyAppSelected -> MaterialTheme.colorScheme.secondary
        isMonitoringActive -> ActiveGreen
        else -> InactiveGrey
    }

    val statusTitle = when {
        !hasAnyAppSelected -> stringResource(R.string.status_monitoring_idle)
        isMonitoringActive -> stringResource(R.string.status_monitoring_on)
        else -> stringResource(R.string.status_monitoring_paused)
    }

    val statusSubtitle = when {
        !hasAnyAppSelected -> stringResource(R.string.status_subtitle_idle)
        isMonitoringActive -> stringResource(R.string.status_subtitle_on)
        else -> stringResource(R.string.status_subtitle_paused)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    glowColor.copy(alpha = 0.45f),
                    RoundedCornerShape(24.dp)
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.22f),
                            radius = size.maxDimension * 0.58f
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.22f),
                        radius = size.maxDimension * 0.58f
                    )
                }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(glowColor.copy(alpha = 0.16f))
                        .border(2.dp, glowColor.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Radar,
                        contentDescription = null,
                        tint = glowColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (hasAnyAppSelected) {
                    Button(
                        onClick = onToggleMasterMonitoring,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("master_monitoring_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMonitoringActive) ActiveGreen else InactiveGrey,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isMonitoringActive) {
                                Icons.Filled.PauseCircle
                            } else {
                                Icons.Filled.PlayCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMonitoringActive) "ON" else "OFF",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionHealthBanner(
    missingCount: Int,
    allReady: Boolean,
    onOpenSetupGuide: () -> Unit
) {
    val accent = if (allReady) ActiveGreen else WarningAmber
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (allReady) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = accent
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.setup_card_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (allReady) {
                        stringResource(R.string.setup_all_ready)
                    } else {
                        stringResource(R.string.setup_missing_count, missingCount)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onOpenSetupGuide,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("open_setup_guide_button")
            ) {
                Text(stringResource(R.string.setup_open_guide))
            }
        }
    }
}

@Composable
private fun AppToggleCard(
    title: String,
    subtitle: String,
    packagesLabel: String,
    icon: ImageVector,
    accentColor: Color,
    isEnabled: Boolean,
    testTag: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isEnabled) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "app_card_border"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isEnabled) {
            accentColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "app_card_bg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onToggle)
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        tonalElevation = if (isEnabled) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, borderColor, RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = if (isEnabled) 0.24f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isEnabled) accentColor.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isEnabled) {
                                stringResource(R.string.state_active)
                            } else {
                                stringResource(R.string.state_off)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = packagesLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.minimumInteractiveComponentSize(),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor
                )
            )
        }
    }
}
