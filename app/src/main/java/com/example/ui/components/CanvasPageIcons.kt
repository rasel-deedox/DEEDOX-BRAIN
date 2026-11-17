package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.repository.CanvasIconKeys
import com.example.data.repository.WorkspaceBlock
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LiquidTheme

object CanvasPageIcons {
    fun forPageKey(rawKey: String?): ImageVector = when (CanvasIconKeys.normalize(rawKey)) {
        CanvasIconKeys.ROCKET -> Icons.Outlined.RocketLaunch
        CanvasIconKeys.PIN -> Icons.Outlined.PushPin
        CanvasIconKeys.BRAIN -> Icons.Outlined.Psychology
        CanvasIconKeys.GEAR -> Icons.Outlined.Settings
        CanvasIconKeys.CHART -> Icons.Outlined.Insights
        CanvasIconKeys.CHECK -> Icons.Outlined.CheckCircle
        CanvasIconKeys.INVOICE -> Icons.Outlined.ReceiptLong
        CanvasIconKeys.WALLET -> Icons.Outlined.AccountBalanceWallet
        CanvasIconKeys.SHIELD -> Icons.Outlined.Lock
        CanvasIconKeys.MAIL -> Icons.Outlined.Email
        CanvasIconKeys.CLOCK -> Icons.Outlined.AccessTime
        else -> Icons.Outlined.Description
    }

    fun labelForPageKey(rawKey: String?): String = when (CanvasIconKeys.normalize(rawKey)) {
        CanvasIconKeys.ROCKET -> "Launch"
        CanvasIconKeys.PIN -> "Pinned"
        CanvasIconKeys.BRAIN -> "Spec"
        CanvasIconKeys.GEAR -> "Ops"
        CanvasIconKeys.CHART -> "Metrics"
        CanvasIconKeys.CHECK -> "Checklist"
        CanvasIconKeys.INVOICE -> "Invoice"
        CanvasIconKeys.WALLET -> "Budget"
        CanvasIconKeys.SHIELD -> "Vault"
        CanvasIconKeys.MAIL -> "Mail"
        CanvasIconKeys.CLOCK -> "Focus"
        else -> "Page"
    }

    fun forBlockType(type: String): ImageVector = when (type) {
        WorkspaceBlock.TYPE_SUBPAGE -> Icons.Outlined.FolderOpen
        WorkspaceBlock.TYPE_TABLE -> Icons.Outlined.GridOn
        WorkspaceBlock.TYPE_INVOICE -> Icons.Outlined.ReceiptLong
        WorkspaceBlock.TYPE_EMAIL -> Icons.Outlined.Email
        WorkspaceBlock.TYPE_REMINDER -> Icons.Outlined.NotificationsActive
        WorkspaceBlock.TYPE_SCHEDULE -> Icons.Outlined.CalendarMonth
        WorkspaceBlock.TYPE_TIMER -> Icons.Outlined.AccessTime
        WorkspaceBlock.TYPE_VOICE -> Icons.Outlined.Mic
        WorkspaceBlock.TYPE_SECRET -> Icons.Outlined.Lock
        WorkspaceBlock.TYPE_CLIENT -> Icons.Outlined.Payments
        WorkspaceBlock.TYPE_EMBED -> Icons.Outlined.Language
        WorkspaceBlock.TYPE_DRIVE_EMBED -> Icons.Outlined.CloudQueue
        WorkspaceBlock.TYPE_CODE -> Icons.Outlined.Code
        WorkspaceBlock.TYPE_H1, WorkspaceBlock.TYPE_H2, WorkspaceBlock.TYPE_H3 -> Icons.Outlined.Title
        WorkspaceBlock.TYPE_CHECKLIST -> Icons.Outlined.CheckBox
        WorkspaceBlock.TYPE_BULLET -> Icons.Outlined.FormatListBulleted
        WorkspaceBlock.TYPE_TOGGLE -> Icons.Outlined.UnfoldMore
        WorkspaceBlock.TYPE_CALLOUT -> Icons.Outlined.Info
        WorkspaceBlock.TYPE_DIVIDER -> Icons.Outlined.HorizontalRule
        else -> Icons.Outlined.Description
    }

    val aiSparkle: ImageVector = Icons.Outlined.AutoAwesome
}

@Composable
fun CanvasPageIconBadge(
    iconKey: String?,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    iconSize: Dp = 18.dp,
    active: Boolean = false
) {
    val palette = LiquidTheme.palette
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) palette.accentSoftBg else palette.recessedTrayBg,
        border = BorderStroke(1.dp, if (active) CrimsonAccent.copy(alpha = 0.6f) else palette.specularRimOutline),
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
            Icon(
                imageVector = CanvasPageIcons.forPageKey(iconKey),
                contentDescription = CanvasPageIcons.labelForPageKey(iconKey),
                tint = if (active) CrimsonAccent else palette.primaryInk,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun MonochromeVectorIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color = LiquidTheme.palette.primaryInk,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}
