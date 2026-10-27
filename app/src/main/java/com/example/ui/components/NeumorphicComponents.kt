package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.service.VoiceOrbState
import com.example.tools.PendingToolConfirmation
import com.example.ui.theme.MayaTokens

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = MayaTokens.CardRadius,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseMod = modifier
        .let { if (testTag != null) it.testTag(testTag) else it }
        .shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = MayaTokens.ShadowDark,
            spotColor = MayaTokens.ShadowDark
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(MayaTokens.SurfaceCardElevated, MayaTokens.SurfaceCard)
            )
        )
        .border(1.2.dp, Color.White.copy(alpha = 0.88f), shape)
        .let {
            if (onClick != null) {
                it.minimumInteractiveComponentSize().clickable(onClick = onClick)
            } else it
        }
        .padding(contentPadding)

    Box(modifier = baseMod) {
        content()
    }
}

@Composable
fun MayaPrimaryGradientButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "primary_action_button"
) {
    val shape = RoundedCornerShape(MayaTokens.PillRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag(testTag)
            .shadow(10.dp, shape, spotColor = MayaTokens.PrimaryBlue.copy(alpha = 0.45f))
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF8CC5FF), Color(0xFF3A94FF))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFFBBD6F7), Color(0xFF9AC0F0))
                    )
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.7f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MayaTokens.InkPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MayaTokens.InkPrimary
            )
        }
    }
}

@Composable
fun MayaTopHeader(
    title: String,
    showBackInsteadOfMenu: Boolean = false,
    hasUnreadNotifications: Boolean = true,
    isMicActive: Boolean = false,
    isCameraActive: Boolean = false,
    isAccessibilityActive: Boolean = false,
    onLeftClick: () -> Unit,
    onBellClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeumorphicCircleIconButton(
                    icon = if (showBackInsteadOfMenu) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                    contentDescription = if (showBackInsteadOfMenu) "Wapas jao" else "Menu kholo",
                    onClick = onLeftClick,
                    testTag = if (showBackInsteadOfMenu) "back_button" else "drawer_menu_button"
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                NeumorphicCircleIconButton(
                    icon = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    onClick = onBellClick,
                    testTag = "top_notifications_button"
                )
                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-3).dp, y = 3.dp)
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD946EF))
                    )
                }
            }
        }

        // Part 14 Safety & Privacy Active Hardware/Accessibility Banner
        if (isMicActive || isCameraActive || isAccessibilityActive) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.72f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isMicActive) ActivePrivacyDot("Mic ON", Color(0xFF16A34A))
                if (isCameraActive) ActivePrivacyDot("Cam ON", Color(0xFFEA580C))
                if (isAccessibilityActive) ActivePrivacyDot("Screen Control Ready", MayaTokens.PrimaryBlue)
            }
        }
    }
}

@Composable
private fun ActivePrivacyDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MayaTokens.InkPrimary)
    }
}

@Composable
fun NeumorphicCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .testTag(testTag)
            .shadow(8.dp, CircleShape, spotColor = MayaTokens.ShadowDark)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color.White, MayaTokens.SurfaceCard)))
            .border(1.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MayaTokens.InkPrimary,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
fun MayaComposerPill(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, spotColor = MayaTokens.ShadowDark)
            .clip(shape)
            .background(MayaTokens.SurfaceRecessed.copy(alpha = 0.85f))
            .border(1.dp, Color.White.copy(alpha = 0.85f), shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .testTag("attach_file_button")
                .clickable(onClick = onAttachClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "File ya photo jodo",
                tint = MayaTokens.InkSecondary
            )
        }

        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MayaTokens.InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MayaTokens.InkPrimary,
                unfocusedTextColor = MayaTokens.InkPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("chat_prompt_input")
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .testTag("send_message_button")
                .shadow(5.dp, CircleShape, spotColor = MayaTokens.ShadowDark)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onSendClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Bhejo",
                tint = MayaTokens.InkPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MayaBottomBarWithOrb(
    currentRoute: String,
    orbState: VoiceOrbState,
    language: AppLanguage,
    onNavigate: (String) -> Unit,
    onOrbClick: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (orbState == VoiceOrbState.IDLE) 1.02f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val barShape = RoundedCornerShape(32.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .shadow(12.dp, barShape, spotColor = MayaTokens.ShadowDark)
                .clip(barShape)
                .background(Brush.verticalGradient(listOf(Color.White, MayaTokens.SurfaceCard)))
                .border(1.2.dp, Color.White, barShape)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BottomNavPillItem(
                label = AppStrings.navHome(language),
                icon = Icons.Default.Home,
                selected = currentRoute == "home",
                testTag = "nav_home",
                onClick = { onNavigate("home") }
            )
            BottomNavPillItem(
                label = AppStrings.navScan(language),
                icon = Icons.Default.CenterFocusStrong,
                selected = currentRoute == "scan",
                testTag = "nav_scan",
                onClick = { onNavigate("scan") }
            )
            Spacer(Modifier.width(78.dp)) // Space reserved for floating center Mic Orb
            BottomNavPillItem(
                label = AppStrings.navYaadein(language),
                icon = Icons.Default.Psychology,
                selected = currentRoute == "yaadein",
                testTag = "nav_yaadein",
                onClick = { onNavigate("yaadein") }
            )
            BottomNavPillItem(
                label = AppStrings.navChat(language),
                icon = Icons.AutoMirrored.Filled.Chat,
                selected = currentRoute == "chat",
                testTag = "nav_chat",
                onClick = { onNavigate("chat") }
            )
        }

        // Center Glowing Neumorphic Mic Orb
        val orbColors = when (orbState) {
            VoiceOrbState.IDLE -> listOf(Color(0xFF8BC4FF), Color(0xFF3993FF))
            VoiceOrbState.LISTENING -> listOf(Color(0xFF6EE7B7), Color(0xFF10B981))
            VoiceOrbState.THINKING -> listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))
            VoiceOrbState.SPEAKING -> listOf(Color(0xFFC4B5FD), Color(0xFF7C3AED))
            VoiceOrbState.AWAITING_CONFIRMATION -> listOf(Color(0xFFFCA5A5), Color(0xFFEF4444))
        }

        Box(
            modifier = Modifier
                .offset(y = (-8).dp)
                .size(82.dp)
                .scale(pulseScale)
                .testTag("center_mic_orb")
                .shadow(16.dp, CircleShape, spotColor = orbColors.last().copy(alpha = 0.55f))
                .clip(CircleShape)
                .background(Brush.verticalGradient(orbColors))
                .border(3.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                .clickable(onClick = onOrbClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (orbState == VoiceOrbState.IDLE) Icons.Default.Mic else Icons.Default.Stop,
                    contentDescription = if (orbState == VoiceOrbState.IDLE) "Maya se bolo" else "Maya ko roko",
                    tint = MayaTokens.PrimaryBlueDeep,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomNavPillItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .width(64.dp)
            .height(56.dp)
            .testTag(testTag)
            .clip(pillShape)
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(Color(0xFF9AC9FF), Color(0xFF4CA0FF)))
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MayaTokens.InkPrimary else MayaTokens.InkSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) MayaTokens.InkPrimary else MayaTokens.InkSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun DangerousToolConfirmationBanner(
    pending: PendingToolConfirmation,
    onApprove: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFFF7ED),
        border = BorderStroke(1.5.dp, Color(0xFFFB923C)),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Voice Confirmation Zaroori Hai",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF9A3412)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = pending.promptSpoken,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7C2D12)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = MayaTokens.PrimaryBlue),
                    modifier = Modifier.testTag("confirm_tool_yes")
                ) {
                    Text("Haan, Pakka Karo")
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("confirm_tool_no")
                ) {
                    Text("Nahi, Ruko", color = MayaTokens.DangerRed)
                }
            }
        }
    }
}
