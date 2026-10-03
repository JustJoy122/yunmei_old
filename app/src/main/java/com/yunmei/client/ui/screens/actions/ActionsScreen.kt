package com.yunmei.client.ui.screens.actions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yunmei.client.core.di.AppContainer

private sealed interface PendingAction {
    data class Open(val quickConnect: Boolean) : PendingAction
    data class Sign(val useLastLocation: Boolean, val saveLocation: Boolean) : PendingAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsScreen(
    container: AppContainer,
) {
    val viewModel: ActionsViewModel = viewModel {
        ActionsViewModel(
            lockStore = container.lockStore,
            accountStore = container.accountStore,
            repository = container.repository,
            appPreferences = container.appPreferences,
            unlockManager = container.unlockManager,
            locationProvider = container.locationProvider,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var lockMenuExpanded by remember { mutableStateOf(false) }
    var showSignDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    LaunchedEffect(Unit) { viewModel.reloadLocks() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.all { it }
        val action = pendingAction
        pendingAction = null
        if (!granted) {
            viewModel.setMessage("权限被拒绝，无法继续")
        } else {
            when (action) {
                is PendingAction.Open -> viewModel.openDoor(action.quickConnect)
                is PendingAction.Sign -> viewModel.sign(action.useLastLocation, action.saveLocation)
                null -> Unit
            }
        }
    }

    fun allGranted(permissions: Array<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    fun blePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }

    fun locationPermissions(): Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    fun requestOpen(quickConnect: Boolean) {
        val permissions = blePermissions()
        if (allGranted(permissions)) {
            viewModel.openDoor(quickConnect)
        } else {
            pendingAction = PendingAction.Open(quickConnect)
            permissionLauncher.launch(permissions)
        }
    }

    fun requestSign(useLastLocation: Boolean, saveLocation: Boolean) {
        showSignDialog = false
        if (useLastLocation) {
            viewModel.sign(useLastLocation = true, saveLocation = saveLocation)
            return
        }
        val permissions = locationPermissions()
        if (allGranted(permissions)) {
            viewModel.sign(useLastLocation = false, saveLocation = saveLocation)
        } else {
            pendingAction = PendingAction.Sign(useLastLocation, saveLocation)
            permissionLauncher.launch(permissions)
        }
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
                text = "开门",
                style = MaterialTheme.typography.headlineMedium,
            )

            // 门锁选择 + 状态信息：包进与设置页一致的标准圆角浅色卡片（模板参数）。
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { lockMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(state.selectedLock?.label ?: "未添加门锁")
                        }
                        DropdownMenu(
                            expanded = lockMenuExpanded,
                            onDismissRequest = { lockMenuExpanded = false },
                        ) {
                            if (state.locks.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("请先登录添加门锁") },
                                    onClick = { lockMenuExpanded = false },
                                )
                            } else {
                                state.locks.forEach { lock ->
                                    DropdownMenuItem(
                                        text = { Text(lock.label) },
                                        onClick = {
                                            viewModel.selectLock(lock)
                                            lockMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (state.selectedLock?.isUsable == false) {
                        Text(
                            text = "当前门锁不可用，请登录或选择其他门锁",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (state.busy) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(state.message ?: "处理中…", style = MaterialTheme.typography.bodySmall)
                    } else {
                        state.message?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    state.battery?.let {
                        Text("电量：$it%", style = MaterialTheme.typography.bodyMedium)
                    }
                    state.code?.let {
                        Text(
                            text = "开门密码：$it",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // 操作按钮：开门 / 打卡 / 获取密码，包进标准圆角浅色卡片（模板参数）。
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { requestOpen(quickConnect = settings.quickConnect) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("开门")
                    }
                    // 「隐藏打卡按钮」「隐藏获取密码按钮」为设置页跨页开关：开启后移除对应按钮。
                    if (!settings.hideSign || !settings.hideCode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (!settings.hideSign) {
                                OutlinedButton(
                                    onClick = { showSignDialog = true },
                                    enabled = !state.busy,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("打卡")
                                }
                            }
                            if (!settings.hideCode) {
                                OutlinedButton(
                                    onClick = { viewModel.getCode() },
                                    enabled = !state.busy,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("获取开门密码")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    if (showSignDialog) {
        AlertDialog(
            onDismissRequest = { showSignDialog = false },
            title = { Text("打卡位置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("请选择本次打卡使用的定位方式。", style = MaterialTheme.typography.bodyMedium)
                    TextButton(
                        onClick = { requestSign(useLastLocation = false, saveLocation = true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("定位并保存") }
                    TextButton(
                        onClick = { requestSign(useLastLocation = false, saveLocation = false) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("仅定位") }
                    TextButton(
                        onClick = { requestSign(useLastLocation = true, saveLocation = false) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("使用上次位置") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSignDialog = false }) { Text("取消") }
            },
        )
    }
}
