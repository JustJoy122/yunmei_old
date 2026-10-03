package com.yunmei.client.ui.screens.locks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.yunmei.client.core.di.AppContainer
import com.yunmei.client.data.local.StoredUser
import com.yunmei.client.data.model.Lock
import com.yunmei.client.data.qrcode.QrCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockListScreen(
    container: AppContainer,
) {
    val viewModel: LockListViewModel = viewModel {
        LockListViewModel(container.lockStore, container.accountStore)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var qrLock by remember { mutableStateOf<Lock?>(null) }
    var showManualImport by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        viewModel.addFromScan(result.contents)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "门锁",
                style = MaterialTheme.typography.headlineMedium,
            )

            // 扫码/手动导入入口：包进与设置页一致的标准圆角浅色卡片（模板参数）。
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { scanLauncher.launch(ScanOptions()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("扫码添加门锁")
                    }
                    OutlinedButton(
                        onClick = { showManualImport = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("手动导入门锁")
                    }
                }
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (state.locks.isEmpty()) {
                Text("暂无门锁，请先登录或扫码/手动导入。", style = MaterialTheme.typography.bodyMedium)
            }

            state.locks.forEach { lock ->
                LockRow(
                    lock = lock,
                    isDefault = state.defaultLock?.label == lock.label,
                    onSetDefault = { viewModel.setDefault(lock) },
                    onRemove = { viewModel.remove(lock) },
                    onShare = { qrLock = lock },
                )
            }
        }
    }

    if (showManualImport) {
        ManualImportDialog(
            savedAccounts = state.savedAccounts,
            onConfirm = { label, mac, writeUuid, serviceUuid, secret, schoolNo, lockNo, usernameMd5 ->
                viewModel.addManual(
                    label = label,
                    mac = mac,
                    writeUuid = writeUuid,
                    serviceUuid = serviceUuid,
                    secret = secret,
                    schoolNo = schoolNo,
                    lockNo = lockNo,
                    usernameMd5 = usernameMd5,
                )
                showManualImport = false
            },
            onDismiss = { showManualImport = false },
        )
    }

    qrLock?.let { lock ->
        val bitmap = remember(lock) { QrCode.bitmap(lock.toShareUrl(), 512) }
        AlertDialog(
            onDismissRequest = { qrLock = null },
            title = { Text(lock.label) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "门锁二维码",
                        modifier = Modifier.size(240.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请勿通过互联网发送给不可信的人",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { qrLock = null }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun ManualImportDialog(
    savedAccounts: List<StoredUser>,
    onConfirm: (
        label: String,
        mac: String,
        writeUuid: String,
        serviceUuid: String,
        secret: String,
        schoolNo: String,
        lockNo: String,
        usernameMd5: String,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var writeUuid by remember { mutableStateOf("") }
    var serviceUuid by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var schoolNo by remember { mutableStateOf("") }
    var lockNo by remember { mutableStateOf("") }
    var accountIndex by remember { mutableIntStateOf(0) }
    var accountExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动导入门锁") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("标签（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = mac,
                    onValueChange = { mac = it },
                    label = { Text("锁 MAC / 门锁 ID（可选，默认用门锁 ID）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = writeUuid,
                    onValueChange = { writeUuid = it },
                    label = { Text("写特征 UUID（lockCharacterUuid）*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = serviceUuid,
                    onValueChange = { serviceUuid = it },
                    label = { Text("服务 UUID（lockServiceUuid）*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("门锁密钥（lockSecret）*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = schoolNo,
                    onValueChange = { schoolNo = it },
                    label = { Text("学校编号（schoolNo）*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = lockNo,
                    onValueChange = { lockNo = it },
                    label = { Text("门锁 ID（lockNo）*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("关联账号（用于获取开门密码 / 打卡）", style = MaterialTheme.typography.bodySmall)
                Box {
                    OutlinedButton(
                        onClick = { accountExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(savedAccounts.getOrNull(accountIndex)?.username ?: "无已保存账号")
                    }
                    DropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false },
                    ) {
                        if (savedAccounts.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("无已保存账号") },
                                onClick = { accountExpanded = false },
                            )
                        } else {
                            savedAccounts.forEachIndexed { index, user ->
                                DropdownMenuItem(
                                    text = { Text(user.username) },
                                    onClick = {
                                        accountIndex = index
                                        accountExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                if (savedAccounts.isEmpty()) {
                    Text(
                        text = "提示：暂无已保存账号，导入后获取开门密码/打卡将不可用，请先在登录页保存账号。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        label,
                        mac,
                        writeUuid,
                        serviceUuid,
                        secret,
                        schoolNo,
                        lockNo,
                        savedAccounts.getOrNull(accountIndex)?.usernameMd5.orEmpty(),
                    )
                },
            ) { Text("导入") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun LockRow(
    lock: Lock,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
    onShare: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = lock.label + if (isDefault) "  [默认]" else "",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "锁 UUID：${lock.serviceUuid.ifBlank { "未知" }}",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSetDefault) { Text("设为默认") }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onShare) { Text("二维码") }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onRemove) { Text("删除") }
            }
        }
    }
}
