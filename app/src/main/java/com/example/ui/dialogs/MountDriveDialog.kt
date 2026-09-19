package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.NetworkStorageManager
import com.example.data.model.DriveType
import com.example.ui.components.NothingBadge
import com.example.ui.components.NothingButton
import com.example.ui.components.NothingCard
import com.example.ui.components.nothingTextFieldColors
import com.example.vpn.SubnetUtil
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDark
import com.example.ui.theme.NothingGray
import com.example.ui.theme.NothingGreen
import com.example.ui.theme.NothingLightGray
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingSurface
import com.example.ui.theme.NothingWhite
import com.example.vpn.VpnManager
import com.example.vpn.VpnStatus
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Security
import kotlinx.coroutines.launch

@Composable
fun MountDriveDialog(
    networkManager: NetworkStorageManager,
    onDismiss: () -> Unit,
    onOpenVpn: (() -> Unit)? = null,
    onMount: (name: String, type: DriveType, host: String, port: Int, shareName: String, username: String, pass: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(DriveType.NETWORK_SMB) }
    var driveName by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("445") }
    var shareName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val vpnStatus by VpnManager.status.collectAsState()
    val isVpnConnected = vpnStatus == VpnStatus.CONNECTED
    val isPrivateIp = remember(host) { VpnManager.isPrivateNetworkIp(host) }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var testSuccess by remember { mutableStateOf(false) }
    var copiedCommand by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Auto update defaults on type change
    fun selectType(type: DriveType) {
        selectedType = type
        testStatus = null
        testSuccess = false
        when (type) {
            DriveType.NETWORK_SMB -> {
                if (driveName.isEmpty() || driveName.startsWith("Drive")) driveName = "SMB Share"
                port = "445"
            }
            DriveType.NETWORK_FTP -> {
                if (driveName.isEmpty() || driveName.startsWith("Drive")) driveName = "FTP Server"
                port = "21"
            }
            DriveType.CLOUD_GDRIVE -> {
                driveName = "Google Drive"
            }
            DriveType.CLOUD_DROPBOX -> {
                driveName = "Dropbox Storage"
            }
            DriveType.CLOUD_ONEDRIVE -> {
                driveName = "OneDrive Cloud"
            }
            else -> {}
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        NothingCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MOUNT // EXTERNAL",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "CLOUD & NETWORK STORAGE",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    NothingBadge(
                        text = selectedType.name.replace("_", " "),
                        dotColor = NothingRed
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Storage Type Selector Tabs
                Text(
                    text = "STORAGE PROTOCOL / PROVIDER",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                val types = listOf(
                    DriveType.NETWORK_SMB to "SMB / CIFS",
                    DriveType.NETWORK_FTP to "FTP PROTOCOL",
                    DriveType.CLOUD_GDRIVE to "GOOGLE DRIVE",
                    DriveType.CLOUD_DROPBOX to "DROPBOX",
                    DriveType.CLOUD_ONEDRIVE to "ONEDRIVE"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { (type, label) ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectType(type) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fields
                OutlinedTextField(
                    value = driveName,
                    onValueChange = { driveName = it },
                    label = { Text("MOUNT NAME", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    singleLine = true,
                    colors = nothingTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("mount_name_input")
                )

                if (selectedType == DriveType.NETWORK_SMB || selectedType == DriveType.NETWORK_FTP) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("HOST / IP ADDRESS", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                            placeholder = { Text("192.168.4.13", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                            singleLine = true,
                            colors = nothingTextFieldColors(),
                            modifier = Modifier.weight(1f).testTag("mount_host_input")
                        )

                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("PORT", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = nothingTextFieldColors(),
                            modifier = Modifier.width(90.dp).testTag("mount_port_input")
                        )
                    }

                    // Check and show homelab subnet match
                    val activeVpnRoutes = VpnManager.activeProfile.value?.routes ?: SubnetUtil.DEFAULT_USER_GATEWAYS
                    val matchedSubnet = SubnetUtil.findMatchingSubnet(host, activeVpnRoutes)
                    if (matchedSubnet != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Router, contentDescription = null, tint = NothingGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SUBNET MATCH: ${matchedSubnet.cidrNotation} (Gateway ${matchedSubnet.gatewayOrHostIp})",
                                color = NothingGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (selectedType == DriveType.NETWORK_SMB) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = shareName,
                            onValueChange = { shareName = it },
                            label = { Text("SHARE NAME", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                            placeholder = { Text("e.g. Shared, Media, Public", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                            singleLine = true,
                            colors = nothingTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().testTag("mount_share_input")
                        )

                        // Smart VPN status & connection recommendation
                        Spacer(modifier = Modifier.height(10.dp))
                        if (isVpnConnected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, NothingGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NothingGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "VPN TUNNEL CONNECTED: Internal SMB share is accessible remotely via firewall.",
                                        color = NothingGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, if (isPrivateIp) NothingRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .clickable { onOpenVpn?.invoke() }
                                    .padding(10.dp)
                                    .testTag("smb_vpn_hint_card")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = "VPN",
                                            tint = if (isPrivateIp) NothingRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isPrivateIp) "CONNECTING REMOTELY? (PRIVATE IP DETECTED)" else "REMOTE ACCESS VIA VPN",
                                                color = if (isPrivateIp) NothingRed else MaterialTheme.colorScheme.onSurface,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "Configure /22 LAN subnets or import .ovpn to connect to homelab SMB shares seamlessly.",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                    if (onOpenVpn != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        NothingBadge(
                                            text = "OPEN VPN",
                                            dotColor = NothingRed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("USERNAME (OPTIONAL)", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        singleLine = true,
                        colors = nothingTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("mount_user_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("PASSWORD", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = nothingTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("mount_password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test connection button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NothingButton(
                            text = if (isTesting) "TESTING..." else "TEST CONNECTION",
                            onClick = {
                                if (host.isBlank()) {
                                    testStatus = "Please enter host address"
                                    testSuccess = false
                                    return@NothingButton
                                }
                                isTesting = true
                                testStatus = null
                                coroutineScope.launch {
                                    val p = port.toIntOrNull() ?: if (selectedType == DriveType.NETWORK_SMB) 445 else 21
                                    val res = if (selectedType == DriveType.NETWORK_FTP) {
                                        networkManager.testFtpConnection(host, p, username.ifBlank { "anonymous" }, password)
                                    } else {
                                        networkManager.testSmbConnection(host, p, shareName, username, password)
                                    }
                                    isTesting = false
                                    res.onSuccess {
                                        testStatus = it
                                        testSuccess = true
                                    }.onFailure {
                                        testStatus = it.message ?: "Connection failed"
                                        testSuccess = false
                                    }
                                }
                            },
                            isPrimary = false,
                            enabled = !isTesting,
                            modifier = Modifier.testTag("test_connection_button")
                        )

                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NothingWhite,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    if (testStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (testSuccess) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingBlack)
                                    .border(1.dp, NothingGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = NothingGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = testStatus ?: "",
                                        color = NothingGreen,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        } else {
                            // Rich Interactive Diagnostic Card for SMB / VPN issues
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingBlack)
                                    .border(1.dp, NothingRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(NothingRed)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "DIAGNOSTIC // SMB & VPN ACCESS",
                                            color = NothingRed,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = testStatus ?: "",
                                        color = NothingLightGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 14.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // 1-Click Copy PowerShell Firewall Fix
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(NothingDark)
                                                .border(1.dp, if (copiedCommand) NothingGreen else NothingBorder, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    clipboardManager.setText(
                                                        AnnotatedString("Set-NetFirewallRule -DisplayGroup \"File and Printer Sharing\" -Enabled True")
                                                    )
                                                    copiedCommand = true
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (copiedCommand) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = if (copiedCommand) NothingGreen else NothingWhite,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (copiedCommand) "COPIED POWERSHELL FIX!" else "COPY POWERSHELL FIX",
                                                color = if (copiedCommand) NothingGreen else NothingWhite,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Try NetBIOS Port 139 if port 445 failed
                                        if (port == "445" || port.isBlank()) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(NothingDark)
                                                    .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        port = "139"
                                                        isTesting = true
                                                        testStatus = null
                                                        coroutineScope.launch {
                                                            val res = networkManager.testSmbConnection(host, 139, shareName, username, password)
                                                            isTesting = false
                                                            res.onSuccess {
                                                                testStatus = it
                                                                testSuccess = true
                                                            }.onFailure {
                                                                testStatus = it.message ?: "Connection failed"
                                                                testSuccess = false
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = "Try Port 139",
                                                    tint = NothingWhite,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "TRY PORT 139",
                                                    color = NothingWhite,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Open VPN dialog
                                        if (onOpenVpn != null) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(NothingDark)
                                                    .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                                                    .clickable { onOpenVpn() }
                                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = "VPN",
                                                    tint = NothingRed,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "VPN SETTINGS",
                                                    color = NothingWhite,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Mount in Standby Mode
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(NothingRed)
                                                .clickable {
                                                    val finalName = if (driveName.isBlank()) "SMB ($host)" else driveName
                                                    val p = port.toIntOrNull() ?: 445
                                                    onMount(finalName, selectedType, host, p, shareName, username, password)
                                                    onDismiss()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "MOUNT IN STANDBY MODE",
                                                color = NothingWhite,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Cloud Provider: Email and Password / Token Credentials
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Provider guide notice
                    val guideText = when (selectedType) {
                        DriveType.CLOUD_GDRIVE -> "Google Drive API requires an OAuth Access Token (starts with ya29...) or a 16-character App Password (from Google Account Security)."
                        DriveType.CLOUD_DROPBOX -> "Dropbox requires a Developer Access Token. Generate a token at dropbox.com/developers/apps."
                        DriveType.CLOUD_ONEDRIVE -> "OneDrive requires a Microsoft Graph OAuth Bearer Token."
                        else -> "Enter account credentials to verify and mount."
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(NothingBlack)
                            .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = guideText,
                            color = NothingLightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it
                            testSuccess = false 
                        },
                        label = { Text("ACCOUNT EMAIL", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        placeholder = {
                            val hint = when (selectedType) {
                                DriveType.CLOUD_GDRIVE -> "your.name@gmail.com"
                                DriveType.CLOUD_DROPBOX -> "user@dropbox.com"
                                DriveType.CLOUD_ONEDRIVE -> "user@outlook.com"
                                else -> "email@domain.com"
                            }
                            Text(hint, color = NothingGray, fontSize = 12.sp)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = nothingTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("mount_cloud_email_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it 
                            testSuccess = false
                        },
                        label = { Text("TOKEN / APP PASSWORD", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        placeholder = { 
                            val pHint = when (selectedType) {
                                DriveType.CLOUD_GDRIVE -> "ya29... token or 16-char app password"
                                DriveType.CLOUD_DROPBOX -> "sl.u.A... Access Token"
                                DriveType.CLOUD_ONEDRIVE -> "EwB... Bearer Token"
                                else -> "Access key / Token"
                            }
                            Text(pHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) 
                        },
                        singleLine = true,
                        colors = nothingTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("mount_cloud_password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Online credential verification button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NothingButton(
                            text = if (isTesting) "VERIFYING ONLINE..." else "VERIFY CREDENTIALS ONLINE",
                            onClick = {
                                if (username.isBlank()) {
                                    testStatus = "Please enter your Email ID"
                                    testSuccess = false
                                    return@NothingButton
                                }
                                if (password.isBlank()) {
                                    testStatus = "Please enter your Token or App Password"
                                    testSuccess = false
                                    return@NothingButton
                                }
                                isTesting = true
                                testStatus = null
                                coroutineScope.launch {
                                    val result = networkManager.verifyCloudDrive(selectedType, password, username)
                                    isTesting = false
                                    testSuccess = result.isSuccess
                                    testStatus = if (result.isSuccess) {
                                        "${result.message} • ${result.quotaAvailable}"
                                    } else {
                                        result.message
                                    }
                                }
                            },
                            isPrimary = false,
                            enabled = !isTesting,
                            modifier = Modifier.testTag("verify_cloud_button")
                        )

                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NothingWhite,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    if (testStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (testSuccess) NothingDark else NothingBlack)
                                .border(1.dp, if (testSuccess) NothingGreen else NothingRed, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = testStatus ?: "",
                                color = if (testSuccess) NothingGreen else NothingRed,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    NothingButton(
                        text = "CANCEL",
                        onClick = onDismiss,
                        isPrimary = false,
                        modifier = Modifier.testTag("mount_cancel_button")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    NothingButton(
                        text = if (isTesting) "CONNECTING..." else "MOUNT DRIVE",
                        onClick = {
                            if (!selectedType.isNetworkDrive) {
                                if (username.isBlank()) {
                                    testStatus = "Please enter your account email"
                                    testSuccess = false
                                    return@NothingButton
                                }
                                if (password.isBlank()) {
                                    testStatus = "Please enter your OAuth Token or App Password"
                                    testSuccess = false
                                    return@NothingButton
                                }
                                if (!testSuccess) {
                                    isTesting = true
                                    testStatus = null
                                    coroutineScope.launch {
                                        val result = networkManager.verifyCloudDrive(selectedType, password, username)
                                        isTesting = false
                                        testSuccess = result.isSuccess
                                        if (result.isSuccess) {
                                            testStatus = "${result.message} • ${result.quotaAvailable}"
                                            val finalName = if (driveName.isBlank()) {
                                                when (selectedType) {
                                                    DriveType.CLOUD_GDRIVE -> "${result.accountName ?: "Google Drive"} (${username.substringBefore("@")})"
                                                    DriveType.CLOUD_DROPBOX -> "${result.accountName ?: "Dropbox"} (${username.substringBefore("@")})"
                                                    DriveType.CLOUD_ONEDRIVE -> "${result.accountName ?: "OneDrive"} (${username.substringBefore("@")})"
                                                    else -> "Cloud Drive"
                                                }
                                            } else driveName
                                            onMount(finalName, selectedType, "", 0, "", username, password)
                                            onDismiss()
                                        } else {
                                            testStatus = result.message
                                        }
                                    }
                                    return@NothingButton
                                }
                            }

                            val finalName = if (driveName.isBlank()) {
                                when (selectedType) {
                                    DriveType.NETWORK_SMB -> "SMB ($host)"
                                    DriveType.NETWORK_FTP -> "FTP ($host)"
                                    DriveType.CLOUD_GDRIVE -> "Google Drive ($username)"
                                    DriveType.CLOUD_DROPBOX -> "Dropbox ($username)"
                                    DriveType.CLOUD_ONEDRIVE -> "OneDrive ($username)"
                                    else -> "Remote Drive"
                                }
                            } else driveName

                            val p = port.toIntOrNull() ?: if (selectedType == DriveType.NETWORK_SMB) 445 else 21
                            onMount(finalName, selectedType, host, p, shareName, username, password)
                            onDismiss()
                        },
                        isPrimary = true,
                        enabled = !isTesting,
                        modifier = Modifier.testTag("mount_confirm_button")
                    )
                }
            }
        }
    }
}
