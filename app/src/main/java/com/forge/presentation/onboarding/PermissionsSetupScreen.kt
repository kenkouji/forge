package com.forge.presentation.onboarding

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeGlassSurface
import com.forge.core.designsystem.component.ForgeGlassVariant
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeTheme

@Composable
fun PermissionsSetupScreen(
    healthConnectGranted: Boolean,
    notificationsGranted: Boolean,
    cameraGranted: Boolean,
    onRequestHealthConnect: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestCamera: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "System Telemetry & Permissions",
            color = ForgeTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "FORGE requests only what it genuinely needs to power your training, physique chronicle, and biological telemetry. If any permission is declined, FORGE continues operating without generating fake values.",
            color = ForgeTheme.colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionTile(
                icon = Icons.Default.DirectionsWalk,
                title = "Health Connect",
                description = "Synchronizes verified daily steps, active burn, and body weight. Optional — FORGE never estimates fake step telemetry if disconnected.",
                isGranted = healthConnectGranted,
                actionLabel = if (healthConnectGranted) "Connected" else "Connect",
                onAction = onRequestHealthConnect
            )

            PermissionTile(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "Sends scheduled workout alarms, pre-workout focal reminders, and PR celebrations. Configurable by category in settings.",
                isGranted = notificationsGranted,
                actionLabel = if (notificationsGranted) "Enabled" else "Allow",
                onAction = onRequestNotifications
            )

            PermissionTile(
                icon = Icons.Default.CameraAlt,
                title = "Camera & Media Access",
                description = "Used solely to capture milestone physique check-ins. Photos are stored encrypted at rest with Android Keystore AES-256-GCM.",
                isGranted = cameraGranted,
                actionLabel = if (cameraGranted) "Authorized" else "Authorize",
                onAction = onRequestCamera
            )
        }
    }
}

@Composable
private fun PermissionTile(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    ForgeGlassSurface(
        variant = if (isGranted) ForgeGlassVariant.STRONG else ForgeGlassVariant.SUBTLE,
        borderColor = if (isGranted) ForgeColors.CyanPrimary.copy(alpha = 0.6f) else ForgeColors.GlassStroke,
        onClick = onAction,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) ForgeColors.CyanPrimary.copy(alpha = 0.2f)
                        else ForgeColors.GlassFillSubtle
                    )
                    .border(
                        1.dp,
                        if (isGranted) ForgeColors.CyanPrimary else ForgeColors.GlassStroke,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) ForgeColors.CyanPrimary else ForgeTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isGranted) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = ForgeColors.CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isGranted) ForgeColors.CyanPrimary.copy(alpha = 0.15f)
                        else ForgeColors.GlassFillStrong
                    )
                    .border(
                        1.dp,
                        if (isGranted) ForgeColors.CyanPrimary.copy(alpha = 0.4f)
                        else ForgeColors.GlassStroke,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = if (isGranted) ForgeColors.CyanPrimary else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
