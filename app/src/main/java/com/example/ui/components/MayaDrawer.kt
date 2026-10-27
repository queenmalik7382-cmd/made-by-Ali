package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.FlavorConfig
import com.example.ui.theme.MayaTokens

data class DrawerMenuEntry(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val showDot: Boolean = false
)

@Composable
fun MayaNavigationDrawerContent(
    assistantName: String,
    currentRoute: String,
    hasUnreadNotifications: Boolean,
    onSelectRoute: (String) -> Unit
) {
    val drawerShape = RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp)
    Column(
        modifier = Modifier
            .width(310.dp)
            .fillMaxHeight()
            .clip(drawerShape)
            .background(Brush.verticalGradient(listOf(MayaTokens.BgTop, MayaTokens.BgBottom)))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Drawer Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1E6BFF), Color(0xFF060F29)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = assistantName,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = assistantName,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Voice AI • ${FlavorConfig.flavorName.uppercase()} Edition",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MayaTokens.InkSecondary
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DrawerSectionTitle("HOME")
            DrawerItemRow(DrawerMenuEntry("home", "Home", Icons.Default.Home), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("routine", "Meri Routine", Icons.Default.EventRepeat), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("yaadein", "Yaadein", Icons.Default.Psychology), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("chat", "Chat", Icons.AutoMirrored.Filled.Chat), currentRoute, onSelectRoute)

            Spacer(Modifier.height(6.dp))
            DrawerSectionTitle("KAAM-KAAJ (WORK)")
            DrawerItemRow(DrawerMenuEntry("youtube_channel", "YouTube Channel Analyst", Icons.Default.PlayCircleFilled), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("markets", "Markets", Icons.AutoMirrored.Filled.TrendingUp), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("marketing", "Marketing", Icons.Default.Storefront), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("documents", "Documents", Icons.Default.Description), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("photos", "Meri photos", Icons.Default.PhotoLibrary), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("coding", "Website / Coding", Icons.Default.Code), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("accessibility_hub", "Accessibility & ADB", Icons.Default.AccessibilityNew), currentRoute, onSelectRoute)

            Spacer(Modifier.height(6.dp))
            DrawerSectionTitle("SYSTEM")
            DrawerItemRow(DrawerMenuEntry("settings", "Settings", Icons.Default.Settings), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("rules", "$assistantName ke rules", Icons.Default.VerifiedUser), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("pc_sync", "PC ⇄ Phone", Icons.AutoMirrored.Filled.CompareArrows), currentRoute, onSelectRoute)

            Spacer(Modifier.height(6.dp))
            DrawerSectionTitle("AUR")
            DrawerItemRow(DrawerMenuEntry("privacy", "Privacy Policy", Icons.Default.Lock), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("about", "About", Icons.Default.Info), currentRoute, onSelectRoute)
            DrawerItemRow(DrawerMenuEntry("upgrade", "Upgrade", Icons.Default.Star), currentRoute, onSelectRoute)
            DrawerItemRow(
                DrawerMenuEntry("notifications", "Notifications", Icons.Default.Notifications, showDot = hasUnreadNotifications),
                currentRoute,
                onSelectRoute
            )
            Spacer(Modifier.height(16.dp))
        }

        // Footer version bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MayaTokens.SurfaceRecessed.copy(alpha = 0.6f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "v5.2.42  •  $assistantName Voice Engine",
                style = MaterialTheme.typography.labelSmall,
                color = MayaTokens.InkSecondary
            )
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MayaTokens.InkSecondary,
        modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun DrawerItemRow(
    entry: DrawerMenuEntry,
    currentRoute: String,
    onSelect: (String) -> Unit
) {
    val selected = currentRoute == entry.route
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("drawer_${entry.route}")
            .shadow(6.dp, shape, spotColor = MayaTokens.ShadowDark)
            .clip(shape)
            .background(
                if (selected) {
                    Brush.horizontalGradient(listOf(Color(0xFF7CB8FF), Color(0xFF3993FF)))
                } else {
                    Brush.verticalGradient(listOf(Color.White, MayaTokens.SurfaceCard))
                }
            )
            .border(1.dp, Color.White, shape)
            .clickable { onSelect(entry.route) }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = entry.label,
            tint = if (selected) MayaTokens.InkPrimary else MayaTokens.PrimaryBlueDeep,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.titleMedium,
            color = MayaTokens.InkPrimary,
            modifier = Modifier.weight(1f)
        )
        if (entry.showDot) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEA580C))
            )
        }
    }
}
