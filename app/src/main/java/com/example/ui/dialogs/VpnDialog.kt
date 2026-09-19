package com.example.ui.dialogs

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.NothingBadge
import com.example.ui.components.NothingButton
import com.example.ui.components.NothingCard
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.vpn.FirewallType
import com.example.vpn.VpnAuthType
import com.example.vpn.VpnManager
import com.example.vpn.VpnProfile
import com.example.vpn.VpnRoutingScope
import com.example.vpn.VpnStatus
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

enum class OpenVpnTab(val label: String) {
    PROFILES("PROFILES"),
    LOGS("LOGS"),
    IMPORT("IMPORT / ADD"),
    STATS("STATS")
}

val OpenVpnOrange = Color(0xFFFA6800)
val OpenVpnDarkSlate = Color(0xFF14181E)
val OpenVpnCardBg = Color(0xFF1D232C)

@Composable
fun VpnDialog(
    onDismiss: () -> Unit,
    onNavigateToSmbMount: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        VpnManager.init(context)
    }

    val status by VpnManager.status.collectAsState()
    val stats by VpnManager.stats.collectAsState()
    val activeProfile by VpnManager.activeProfile.collectAsState()
    val savedProfiles by VpnManager.savedProfiles.collectAsState()
    val logs by VpnManager.logs.collectAsState()

    var currentTab by remember { mutableStateOf(OpenVpnTab.PROFILES) }
    var pendingConnectProfile by remember { mutableStateOf<VpnProfile?>(null) }

    // Launcher for Android VpnService.prepare permission intent
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val target = pendingConnectProfile ?: activeProfile
            if (target != null) {
                VpnManager.connect(context, target)
                Toast.makeText(context, "OpenVPN tunnel starting...", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "VPN connection cancelled or denied by system", Toast.LENGTH_SHORT).show()
            VpnManager.addLog("VPN permission was denied or cancelled.")
        }
    }

    // Android 13+ Notification Permission
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val target = pendingConnectProfile ?: activeProfile
        if (target != null) {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPrepareLauncher.launch(prepareIntent)
            } else {
                VpnManager.connect(context, target)
            }
        }
    }

    fun triggerConnect(profile: VpnProfile) {
        pendingConnectProfile = profile

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val hasNotif = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        try {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPrepareLauncher.launch(prepareIntent)
            } else {
                VpnManager.connect(context, profile)
            }
        } catch (e: Exception) {
            VpnManager.connect(context, profile)
        }
    }

    fun triggerDisconnect() {
        VpnManager.disconnect(context)
        Toast.makeText(context, "OpenVPN disconnected", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NothingBlack.copy(alpha = 0.92f))
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            NothingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, NothingBorder, RoundedCornerShape(20.dp))
                    .testTag("openvpn_app_dialog"),
                backgroundColor = OpenVpnDarkSlate
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // OpenVPN App Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(OpenVpnOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "OpenVPN",
                                    tint = NothingWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "OpenVPN",
                                        color = NothingWhite,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Connect",
                                        color = OpenVpnOrange,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    text = "FIREWALL & SECURE TUNNEL • v3.8.2",
                                    color = NothingGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isConnected = status == VpnStatus.CONNECTED
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isConnected) NothingGreen.copy(alpha = 0.18f) else NothingDark)
                                    .border(1.dp, if (isConnected) NothingGreen else NothingBorder, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (status) {
                                                    VpnStatus.CONNECTED -> NothingGreen
                                                    VpnStatus.CONNECTING, VpnStatus.AUTHENTICATING -> OpenVpnOrange
                                                    else -> NothingRed
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (status) {
                                            VpnStatus.CONNECTED -> "CONNECTED"
                                            VpnStatus.CONNECTING -> "CONNECTING"
                                            VpnStatus.AUTHENTICATING -> "AUTHENTICATING"
                                            VpnStatus.DISCONNECTING -> "CLOSING"
                                            VpnStatus.ERROR -> "ERROR"
                                            else -> "DISCONNECTED"
                                        },
                                        color = when (status) {
                                            VpnStatus.CONNECTED -> NothingGreen
                                            VpnStatus.CONNECTING, VpnStatus.AUTHENTICATING -> OpenVpnOrange
                                            else -> NothingLightGray
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp).testTag("openvpn_close_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = NothingGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // OpenVPN Style Navigation Tabs
                    TabRow(
                        selectedTabIndex = currentTab.ordinal,
                        containerColor = NothingBlack,
                        contentColor = OpenVpnOrange,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[currentTab.ordinal]),
                                color = OpenVpnOrange
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                    ) {
                        OpenVpnTab.values().forEach { tab ->
                            Tab(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                text = {
                                    Text(
                                        text = tab.label,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp,
                                        color = if (currentTab == tab) NothingWhite else NothingGray
                                    )
                                },
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Contents
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentTab) {
                            OpenVpnTab.PROFILES -> {
                                OpenVpnProfilesView(
                                    status = status,
                                    stats = stats,
                                    activeProfile = activeProfile,
                                    savedProfiles = savedProfiles,
                                    onConnect = { triggerConnect(it) },
                                    onDisconnect = { triggerDisconnect() },
                                    onSelectProfile = { VpnManager.selectProfile(it) },
                                    onDuplicate = { VpnManager.duplicateProfile(it) },
                                    onDelete = { VpnManager.deleteProfile(it.id) },
                                    onGoToImport = { currentTab = OpenVpnTab.IMPORT },
                                    onGoToLogs = { currentTab = OpenVpnTab.LOGS },
                                    onGoToStats = { currentTab = OpenVpnTab.STATS },
                                    onNavigateToSmb = onNavigateToSmbMount
                                )
                            }
                            OpenVpnTab.LOGS -> {
                                OpenVpnLogsView(
                                    logs = logs,
                                    status = status,
                                    activeProfile = activeProfile,
                                    onClear = { VpnManager.clearLogs() }
                                )
                            }
                            OpenVpnTab.IMPORT -> {
                                OpenVpnImportAddView(
                                    activeProfile = activeProfile,
                                    onSaveAndConnect = { profile ->
                                        VpnManager.saveProfile(profile)
                                        triggerConnect(profile)
                                        currentTab = OpenVpnTab.PROFILES
                                    },
                                    onSaveProfile = { profile ->
                                        VpnManager.saveProfile(profile)
                                        currentTab = OpenVpnTab.PROFILES
                                    }
                                )
                            }
                            OpenVpnTab.STATS -> {
                                OpenVpnStatsView(
                                    status = status,
                                    stats = stats,
                                    activeProfile = activeProfile,
                                    onGoToLogs = { currentTab = OpenVpnTab.LOGS }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: PROFILES VIEW (OpenVPN Connect Signature Home Screen)
// -------------------------------------------------------------

@Composable
fun OpenVpnProfilesView(
    status: VpnStatus,
    stats: com.example.vpn.VpnStats,
    activeProfile: VpnProfile?,
    savedProfiles: List<VpnProfile>,
    onConnect: (VpnProfile) -> Unit,
    onDisconnect: () -> Unit,
    onSelectProfile: (VpnProfile) -> Unit,
    onDuplicate: (VpnProfile) -> Unit,
    onDelete: (VpnProfile) -> Unit,
    onGoToImport: () -> Unit,
    onGoToLogs: () -> Unit,
    onGoToStats: () -> Unit,
    onNavigateToSmb: (() -> Unit)? = null
) {
    val isConnected = status == VpnStatus.CONNECTED
    val isBusy = status == VpnStatus.CONNECTING || status == VpnStatus.AUTHENTICATING

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Connection Hero Card (Classic OpenVPN Switch UI)
        if (activeProfile != null) {
            item {
                val cardBorderColor = if (isConnected) NothingGreen else if (isBusy) OpenVpnOrange else NothingBorder

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(OpenVpnCardBg)
                        .border(1.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("active_vpn_card")
                ) {
                    Column {
                        // Top row with Toggle Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) NothingGreen else if (isBusy) OpenVpnOrange else NothingDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isConnected) Icons.Default.Check else Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (isConnected || isBusy) NothingBlack else NothingLightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = activeProfile.name,
                                        color = NothingWhite,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${activeProfile.serverHost}:${activeProfile.serverPort} • ${activeProfile.proto}",
                                        color = NothingGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Large OpenVPN Toggle Switch
                            Switch(
                                checked = isConnected || isBusy,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        onConnect(activeProfile)
                                    } else {
                                        onDisconnect()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NothingWhite,
                                    checkedTrackColor = if (isConnected) NothingGreen else OpenVpnOrange,
                                    uncheckedThumbColor = NothingGray,
                                    uncheckedTrackColor = NothingBlack
                                ),
                                modifier = Modifier.testTag("openvpn_main_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // TUNNEL ROUTING SCOPE SELECTOR (Whole Phone vs For Drive Only)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingBlack)
                                .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isWholePhone = activeProfile.routingScope == VpnRoutingScope.WHOLE_PHONE
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isWholePhone) NothingGreen.copy(alpha = 0.22f) else NothingDark)
                                    .border(1.dp, if (isWholePhone) NothingGreen else NothingBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        VpnManager.setRoutingScope(VpnRoutingScope.WHOLE_PHONE)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 8.dp)
                                    .testTag("scope_whole_phone_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = if (isWholePhone) NothingGreen else NothingGray,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "WHOLE PHONE",
                                            color = if (isWholePhone) NothingGreen else NothingLightGray,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "All apps & system internet",
                                        color = NothingGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp
                                    )
                                }
                            }

                            val isDriveOnly = activeProfile.routingScope == VpnRoutingScope.DRIVE_ONLY
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDriveOnly) OpenVpnOrange.copy(alpha = 0.22f) else NothingDark)
                                    .border(1.dp, if (isDriveOnly) OpenVpnOrange else NothingBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        VpnManager.setRoutingScope(VpnRoutingScope.DRIVE_ONLY)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 8.dp)
                                    .testTag("scope_drive_only_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Cloud,
                                            contentDescription = null,
                                            tint = if (isDriveOnly) OpenVpnOrange else NothingGray,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "FOR DRIVE ONLY",
                                            color = if (isDriveOnly) OpenVpnOrange else NothingLightGray,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Explorer & Storage mounts only",
                                        color = NothingGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status Info Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingBlack)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = OpenVpnOrange,
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = when (status) {
                                        VpnStatus.CONNECTED -> "ONLINE // [${activeProfile.routingScope.badge}]"
                                        VpnStatus.CONNECTING -> "RESOLVING & CONNECTING..."
                                        VpnStatus.AUTHENTICATING -> "AUTHENTICATING TLS HANDSHAKE..."
                                        VpnStatus.DISCONNECTING -> "TEARING DOWN TUNNEL..."
                                        VpnStatus.ERROR -> "CONNECTION FAILED"
                                        else -> "DISCONNECTED • READY TO CONNECT"
                                    },
                                    color = if (isConnected) NothingGreen else if (isBusy) OpenVpnOrange else NothingLightGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }

                            NothingBadge(
                                text = activeProfile.firewallType.displayName.substringBefore(" "),
                                dotColor = if (isConnected) NothingGreen else NothingGray
                            )
                        }

                        // Live Telemetry strip if connected
                        if (isConnected) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "SPEED (IN / OUT)", color = NothingGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text(text = "↓${stats.formattedDownloadSpeed} / ↑${stats.formattedUploadSpeed}", color = NothingWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(text = "PING", color = NothingGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text(text = "${stats.latencyMs ?: "--"} ms", color = NothingGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(text = "DURATION", color = NothingGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text(text = stats.formattedDuration, color = NothingWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(text = "VIRTUAL IP", color = NothingGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text(text = stats.assignedIp, color = NothingWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Shortcuts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NothingDark)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                        .clickable { onGoToLogs() }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Terminal, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(text = "VIEW LOGS", color = NothingWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NothingDark)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                        .clickable { onGoToStats() }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Speed, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(text = "STATISTICS", color = NothingWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (onNavigateToSmb != null) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NothingGreen.copy(alpha = 0.15f))
                                            .border(1.dp, NothingGreen, RoundedCornerShape(8.dp))
                                            .clickable { onNavigateToSmb() }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Cloud, contentDescription = null, tint = NothingGreen, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(text = "MOUNT SMB", color = NothingGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Profiles List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONFIGURED PROFILES (${savedProfiles.size})",
                    color = NothingLightGray,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OpenVpnOrange.copy(alpha = 0.18f))
                        .border(1.dp, OpenVpnOrange, RoundedCornerShape(8.dp))
                        .clickable { onGoToImport() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("add_profile_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = OpenVpnOrange, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "IMPORT / ADD", color = OpenVpnOrange, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (savedProfiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingDark)
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NothingGray, modifier = Modifier.size(32.dp))
                        Text(
                            text = "NO VPN PROFILES",
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Tap 'IMPORT / ADD' to add an .ovpn configuration file or configure custom firewall profile.",
                            color = NothingGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Saved Profile Cards
        items(savedProfiles, key = { it.id }) { profile ->
            val isCurrentActive = activeProfile?.id == profile.id
            val isThisProfileConnected = isCurrentActive && isConnected
            val isThisProfileBusy = isCurrentActive && isBusy

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCurrentActive) OpenVpnCardBg else NothingDark)
                    .border(
                        1.dp,
                        if (isThisProfileConnected) NothingGreen else if (isCurrentActive) OpenVpnOrange.copy(alpha = 0.6f) else NothingBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectProfile(profile) }
                    .padding(12.dp)
                    .testTag("profile_item_${profile.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        // Switch for each profile
                        Switch(
                            checked = isThisProfileConnected || isThisProfileBusy,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    onSelectProfile(profile)
                                    onConnect(profile)
                                } else {
                                    if (isCurrentActive) onDisconnect()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NothingWhite,
                                checkedTrackColor = if (isThisProfileConnected) NothingGreen else OpenVpnOrange,
                                uncheckedThumbColor = NothingGray,
                                uncheckedTrackColor = NothingBlack
                            ),
                            modifier = Modifier.testTag("profile_switch_${profile.id}")
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.name,
                                    color = NothingWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                if (isCurrentActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isThisProfileConnected) NothingGreen else OpenVpnOrange)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isScopeWholePhone = profile.routingScope == VpnRoutingScope.WHOLE_PHONE
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isScopeWholePhone) NothingGreen.copy(alpha = 0.2f) else OpenVpnOrange.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                        .clickable {
                                            val newScope = if (isScopeWholePhone) VpnRoutingScope.DRIVE_ONLY else VpnRoutingScope.WHOLE_PHONE
                                            VpnManager.saveProfile(profile.copy(routingScope = newScope))
                                        }
                                ) {
                                    Text(
                                        text = if (isScopeWholePhone) "WHOLE PHONE" else "FOR DRIVE ONLY",
                                        color = if (isScopeWholePhone) NothingGreen else OpenVpnOrange,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${profile.serverHost}:${profile.serverPort} • ${profile.proto}",
                                    color = NothingGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp
                                )
                            }
                            if (profile.fileName.isNotBlank()) {
                                Text(
                                    text = "Source: ${profile.fileName}",
                                    color = NothingLightGray.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }

                    // Profile Actions (Duplicate / Delete)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onDuplicate(profile) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = NothingGray, modifier = Modifier.size(15.dp))
                        }
                        IconButton(
                            onClick = { onDelete(profile) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NothingRed.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: LOGS VIEW (OpenVPN Signature Log Field & File Viewer)
// -------------------------------------------------------------

@Composable
fun OpenVpnLogsView(
    logs: List<String>,
    status: VpnStatus,
    activeProfile: VpnProfile?,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf("ALL") }
    var autoScroll by remember { mutableStateOf(true) }

    val filteredLogs = remember(logs, searchQuery, filterCategory) {
        logs.filter { line ->
            val matchesCategory = when (filterCategory) {
                "EVENTS" -> line.contains("EVENT:", ignoreCase = true)
                "TLS/AUTH" -> line.contains("TLS", ignoreCase = true) || line.contains("AUTH", ignoreCase = true) || line.contains("VERIFY", ignoreCase = true)
                "ROUTING" -> line.contains("route", ignoreCase = true) || line.contains("TUN", ignoreCase = true) || line.contains("IP", ignoreCase = true)
                "ERRORS" -> line.contains("Error", ignoreCase = true) || line.contains("Warning", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() || line.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Log File Header Summary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(OpenVpnCardBg)
                .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = OpenVpnOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "openvpn_client.log",
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${logs.size} lines recorded • Profile: ${activeProfile?.name ?: "None"}",
                            color = NothingGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp
                        )
                    }
                }

                // Action Bar: Copy Log, Share/Export, Clear
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy to clipboard
                    IconButton(
                        onClick = {
                            val text = VpnManager.exportLogsToString()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("OpenVPN Log", text))
                            Toast.makeText(context, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp).testTag("copy_logs_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Log", tint = NothingWhite, modifier = Modifier.size(15.dp))
                    }

                    // Share/Export log file
                    IconButton(
                        onClick = {
                            val text = VpnManager.exportLogsToString()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_TITLE, "openvpn_log_export.txt")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export OpenVPN Log"))
                        },
                        modifier = Modifier.size(28.dp).testTag("export_logs_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export Log", tint = NothingWhite, modifier = Modifier.size(15.dp))
                    }

                    // Clear logs
                    IconButton(
                        onClick = { onClear() },
                        modifier = Modifier.size(28.dp).testTag("clear_logs_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Log", tint = NothingRed, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search logs (e.g. EVENT, TLS, route)...", color = NothingGray, fontSize = 10.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NothingGray, modifier = Modifier.size(14.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = NothingGray, modifier = Modifier.size(14.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OpenVpnOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = OpenVpnOrange
            ),
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("log_search_field")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "EVENTS", "TLS/AUTH", "ROUTING", "ERRORS").forEach { cat ->
                val isSelected = filterCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) OpenVpnOrange else NothingDark)
                        .border(1.dp, if (isSelected) OpenVpnOrange else NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { filterCategory = cat }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) NothingBlack else NothingLightGray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // The OpenVPN Terminal Log Field
        SelectionContainer(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingBlack)
                    .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                    .padding(8.dp)
                    .testTag("openvpn_log_terminal")
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (logs.isEmpty()) "No connection logs recorded yet.\nConnect to a VPN profile to stream logs." else "No log entries match your filter.",
                            color = NothingGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredLogs) { line ->
                            val textColor = when {
                                line.contains("EVENT:", ignoreCase = true) -> Color(0xFF4DEEEA)
                                line.contains("CONNECTED", ignoreCase = true) || line.contains("ONLINE", ignoreCase = true) -> NothingGreen
                                line.contains("AUTH_SUCCESS", ignoreCase = true) || line.contains("VERIFY OK", ignoreCase = true) -> Color(0xFFFFCC00)
                                line.contains("Error", ignoreCase = true) || line.contains("FAILED", ignoreCase = true) -> NothingRed
                                line.contains("Warning", ignoreCase = true) -> Color(0xFFFFA000)
                                line.contains("SMB", ignoreCase = true) -> Color(0xFF81D4FA)
                                else -> NothingLightGray
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = line,
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: IMPORT / ADD PROFILE VIEW (OpenVPN Connect File & URL)
// -------------------------------------------------------------

@Composable
fun OpenVpnImportAddView(
    activeProfile: VpnProfile?,
    onSaveAndConnect: (VpnProfile) -> Unit,
    onSaveProfile: (VpnProfile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var importMode by remember { mutableStateOf("FILE") } // "FILE" or "MANUAL"

    // Profile form state
    var profileName by remember { mutableStateOf("") }
    var serverHost by remember { mutableStateOf("") }
    var serverPort by remember { mutableStateOf("1194") }
    var proto by remember { mutableStateOf("UDP") }
    var firewallType by remember { mutableStateOf(FirewallType.SOPHOS) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var routingScope by remember { mutableStateOf(VpnRoutingScope.WHOLE_PHONE) }
    var ovpnFileName by remember { mutableStateOf("") }
    var ovpnContent by remember { mutableStateOf("") }

    var testPingResult by remember { mutableStateOf<String?>(null) }
    var isTestingPing by remember { mutableStateOf(false) }

    // Launcher for picking .ovpn configuration file
    val ovpnPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "client.ovpn"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: "client.ovpn"
                    }
                }

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream))
                    val content = reader.readText()
                    val imported = VpnManager.importFromOvpn(content, fileName, routingScope)
                    profileName = imported.name
                    serverHost = imported.serverHost
                    serverPort = imported.serverPort.toString()
                    proto = imported.proto
                    firewallType = imported.firewallType
                    ovpnFileName = imported.fileName
                    ovpnContent = imported.ovpnContent
                    routingScope = imported.routingScope
                    Toast.makeText(context, "Loaded .ovpn file: $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read .ovpn: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun buildProfile(): VpnProfile {
        return VpnProfile(
            name = profileName.ifBlank { "${firewallType.displayName} VPN" },
            serverHost = serverHost.trim(),
            serverPort = serverPort.toIntOrNull() ?: 1194,
            proto = proto,
            routingScope = routingScope,
            firewallType = firewallType,
            username = username.trim(),
            password = password,
            saveCredentials = true,
            splitTunnel = routingScope == VpnRoutingScope.DRIVE_ONLY,
            fileName = ovpnFileName,
            ovpnContent = ovpnContent,
            routes = listOf("192.168.0.0/16", "10.0.0.0/8", "172.16.0.0/12"),
            assignedIp = "10.8.0.2"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector: FILE or MANUAL
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingBlack)
                    .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (importMode == "FILE") OpenVpnOrange else NothingBlack)
                        .clickable { importMode = "FILE" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = if (importMode == "FILE") NothingBlack else NothingLightGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "IMPORT .OVPN FILE", color = if (importMode == "FILE") NothingBlack else NothingLightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (importMode == "MANUAL") OpenVpnOrange else NothingBlack)
                        .clickable { importMode = "MANUAL" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = if (importMode == "MANUAL") NothingBlack else NothingLightGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "MANUAL CONFIG", color = if (importMode == "MANUAL") NothingBlack else NothingLightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Mode 1: File Drop / Pick Area
        if (importMode == "FILE") {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OpenVpnCardBg)
                        .border(1.5.dp, OpenVpnOrange.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .clickable {
                            try {
                                ovpnPickerLauncher.launch(arrayOf("*/*"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Cannot open file picker", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(20.dp)
                        .testTag("browse_ovpn_file_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(OpenVpnOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = "Open OVPN", tint = OpenVpnOrange, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (ovpnFileName.isNotBlank()) "FILE LOADED: $ovpnFileName" else "TAP TO SELECT .OVPN CONFIGURATION FILE",
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Supports Sophos UTM/XG, pfSense, FortiGate & OpenVPN AS configs",
                            color = NothingGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // Configuration Form (Profile Name, Server, Port, Protocol)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Profile Name
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) },
                    placeholder = { Text("e.g. Office Sophos Firewall", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OpenVpnOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedLabelColor = OpenVpnOrange,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = OpenVpnOrange
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )

                // Server Host and Port
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = serverHost,
                        onValueChange = { serverHost = it },
                        label = { Text("Server Host or IP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) },
                        placeholder = { Text("vpn.example.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenVpnOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedLabelColor = OpenVpnOrange,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = OpenVpnOrange
                        ),
                        modifier = Modifier.weight(2f).testTag("server_host_input")
                    )

                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        label = { Text("Port", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenVpnOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedLabelColor = OpenVpnOrange,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = OpenVpnOrange
                        ),
                        modifier = Modifier.weight(1f).testTag("server_port_input")
                    )
                }

                // Protocol UDP/TCP and Firewall Preset
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Protocol selector
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingBlack)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Row {
                            listOf("UDP", "TCP").forEach { p ->
                                val selected = proto.equals(p, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) OpenVpnOrange else NothingBlack)
                                        .clickable { proto = p }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p,
                                        color = if (selected) NothingBlack else NothingLightGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Firewall Preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingBlack)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = firewallType.displayName,
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Username and Password
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (Optional / Firewall ID)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OpenVpnOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedLabelColor = OpenVpnOrange,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = OpenVpnOrange
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("vpn_username_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OpenVpnOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedLabelColor = OpenVpnOrange,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = OpenVpnOrange
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("vpn_password_input")
                )

                // Tunnel Routing Scope Selector (Whole Phone vs For Drive Only)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingBlack)
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TUNNEL ROUTING SCOPE",
                        color = NothingLightGray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isWholePhone = routingScope == VpnRoutingScope.WHOLE_PHONE
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isWholePhone) NothingGreen.copy(alpha = 0.2f) else NothingDark)
                                .border(1.dp, if (isWholePhone) NothingGreen else NothingBorder, RoundedCornerShape(8.dp))
                                .clickable { routingScope = VpnRoutingScope.WHOLE_PHONE }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = if (isWholePhone) NothingGreen else NothingGray, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "WHOLE PHONE",
                                        color = if (isWholePhone) NothingGreen else NothingLightGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "All apps & system traffic",
                                    color = NothingGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp
                                )
                            }
                        }

                        val isDriveOnly = routingScope == VpnRoutingScope.DRIVE_ONLY
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDriveOnly) OpenVpnOrange.copy(alpha = 0.2f) else NothingDark)
                                .border(1.dp, if (isDriveOnly) OpenVpnOrange else NothingBorder, RoundedCornerShape(8.dp))
                                .clickable { routingScope = VpnRoutingScope.DRIVE_ONLY }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, tint = if (isDriveOnly) OpenVpnOrange else NothingGray, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "FOR DRIVE ONLY",
                                        color = if (isDriveOnly) OpenVpnOrange else NothingLightGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Explorer & Storage mounts only",
                                    color = NothingGray,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }

                // Test Reachability Probe Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingDark)
                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isTestingPing) {
                            scope.launch {
                                isTestingPing = true
                                testPingResult = "Testing socket reachability..."
                                val res = VpnManager.testServerReachability(
                                    host = serverHost,
                                    port = serverPort.toIntOrNull() ?: 1194,
                                    proto = proto
                                )
                                testPingResult = if (res.isSuccess) {
                                    "Handshake reachable in ${res.getOrNull()}ms"
                                } else {
                                    "Direct probe timeout (Tunnel will route securely)"
                                }
                                isTestingPing = false
                            }
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTestingPing) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = OpenVpnOrange, strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = testPingResult ?: "TEST FIREWALL REACHABILITY PING",
                            color = if (testPingResult?.contains("reachable") == true) NothingGreen else OpenVpnOrange,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons: SAVE and SAVE & CONNECT
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NothingButton(
                        text = "SAVE PROFILE",
                        onClick = {
                            val p = buildProfile()
                            onSaveProfile(p)
                            Toast.makeText(context, "Profile Saved", Toast.LENGTH_SHORT).show()
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f).testTag("save_profile_btn")
                    )

                    NothingButton(
                        text = "SAVE & CONNECT",
                        onClick = {
                            val p = buildProfile()
                            onSaveAndConnect(p)
                        },
                        isPrimary = true,
                        modifier = Modifier.weight(1.3f).testTag("save_connect_profile_btn")
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: STATS VIEW (OpenVPN Connection Telemetry & Routes)
// -------------------------------------------------------------

@Composable
fun OpenVpnStatsView(
    status: VpnStatus,
    stats: com.example.vpn.VpnStats,
    activeProfile: VpnProfile?,
    onGoToLogs: () -> Unit
) {
    val isConnected = status == VpnStatus.CONNECTED

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OpenVpnCardBg)
                    .border(1.5.dp, if (isConnected) NothingGreen else NothingBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TUNNEL TELEMETRY",
                            color = NothingWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        NothingBadge(
                            text = if (isConnected) "ACTIVE" else "IDLE",
                            dotColor = if (isConnected) NothingGreen else NothingGray
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "SPEED (IN)", color = NothingGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text(text = stats.formattedDownloadSpeed, color = NothingWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(text = "Total: ${stats.formattedBytesIn}", color = NothingLightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }

                        Column {
                            Text(text = "SPEED (OUT)", color = NothingGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text(text = stats.formattedUploadSpeed, color = NothingWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(text = "Total: ${stats.formattedBytesOut}", color = NothingLightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }

                        Column {
                            Text(text = "LATENCY", color = NothingGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "${stats.latencyMs ?: "--"} ms", color = NothingGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(text = stats.formattedDuration, color = NothingLightGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Technical parameters card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingBlack)
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "OPENVPN CONNECTION PARAMETERS", color = NothingLightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Tunnel Routing Mode", color = NothingGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = if (stats.routingScope == VpnRoutingScope.WHOLE_PHONE) "WHOLE PHONE (Full Device)" else "FOR DRIVE ONLY (App Split)",
                            color = if (stats.routingScope == VpnRoutingScope.WHOLE_PHONE) NothingGreen else OpenVpnOrange,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Assigned Virtual IP", color = NothingGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(text = stats.assignedIp, color = NothingWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Firewall Gateway", color = NothingGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(text = "${stats.serverHost}:${stats.serverPort} (${stats.proto})", color = NothingWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Data Cipher Suite", color = NothingGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(text = "AES-256-GCM / SHA256", color = NothingWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Interface MTU", color = NothingGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(text = "1500 (TCP/UDP frame 1557)", color = NothingWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Active Subnet Routing Table
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingBlack)
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "ROUTED SMB / CORPORATE SUBNETS", color = NothingLightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = "TUN0", color = NothingGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    listOf("192.168.0.0/16 [SMB Shared Drives]", "10.0.0.0/8 [Internal Services]", "172.16.0.0/12 [Corporate VPN Subnet]").forEach { route ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(NothingGreen))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = route, color = NothingWhite, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}
