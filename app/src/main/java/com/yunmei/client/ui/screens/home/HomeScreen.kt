package com.yunmei.client.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yunmei.client.core.di.AppContainer
import com.yunmei.client.data.model.Lock

/**
 * 主页/概览：左上大标题 + 状态 banner（登录/门锁状态，可点击）+ 信息卡。
 * 不再出现「当前界面风格」，也不再堆叠开门/打卡按钮（已移入「开门」底栏页）。
 */
@Composable
fun HomeScreen(
    container: AppContainer,
    onNavigateToLogin: () -> Unit,
    onNavigateToLocks: () -> Unit,
    onNavigateToActions: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(container.lockStore, container.accountStore)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.reload() }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 左上大标题 = App 名（首页是顶层 tab 根页，无返回箭头）。
            Text(
                text = "云莓智能",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            StatusBanner(
                isLoggedIn = state.isLoggedIn,
                lock = state.selectedLock,
                onClick = {
                    if (state.isLoggedIn) onNavigateToActions() else onNavigateToLogin()
                },
            )

            LockInfoCard(
                lock = state.selectedLock,
                onClick = onNavigateToLocks,
            )

            AccountInfoCard(
                usernames = state.savedAccounts.map { it.username },
                onClick = onNavigateToLogin,
            )

            // 为底栏预留空间，避免最后一项被遮挡。
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
private fun StatusBanner(
    isLoggedIn: Boolean,
    lock: Lock?,
    onClick: () -> Unit,
) {
    val loggedIn = isLoggedIn
    val hasLock = lock != null
    val healthy = loggedIn && hasLock && lock.isUsable
    val title = when {
        healthy -> "就绪"
        !loggedIn && !hasLock -> "尚未登录 / 未添加门锁"
        !loggedIn -> "未登录"
        !hasLock -> "未添加门锁"
        else -> "门锁不可用"
    }
    val subtitle = buildString {
        append(if (loggedIn) "账号已登录" else "账号未登录")
        append(" · ")
        append(lock?.label?.takeIf { it.isNotBlank() } ?: "无门锁")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (healthy) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (healthy) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (healthy) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (healthy) "点击开锁" else "点击前往",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LockInfoCard(
    lock: Lock?,
    onClick: () -> Unit,
) {
    InfoCard(
        title = "当前门锁",
        icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        onClick = onClick,
    ) {
        if (lock == null) {
            InfoRow("状态", "未添加门锁，点击管理")
        } else {
            InfoRow("名称", lock.label)
            InfoRow("学校编号", lock.schoolNo.ifBlank { "未知" })
            InfoRow("门锁 ID", lock.lockNo.ifBlank { "未知" })
            InfoRow("可用性", if (lock.isUsable) "✓ 可开门" else "× 缺少密钥/UUID")
        }
    }
}

@Composable
private fun AccountInfoCard(
    usernames: List<String>,
    onClick: () -> Unit,
) {
    InfoCard(
        title = "账号",
        icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        onClick = onClick,
    ) {
        if (usernames.isEmpty()) {
            InfoRow("状态", "未登录，点击登录")
        } else {
            InfoRow("已保存", usernames.joinToString("、"))
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
