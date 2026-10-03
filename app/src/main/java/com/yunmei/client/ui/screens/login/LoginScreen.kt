package com.yunmei.client.ui.screens.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yunmei.client.core.di.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val viewModel: LoginViewModel = viewModel {
        LoginViewModel(container.repository, container.accountStore, container.lockStore)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSavedUsers by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) {
        if (state.done) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录云莓智能") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("账号") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onSaveAccountChange(!state.saveAccount) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.saveAccount,
                    onCheckedChange = viewModel::onSaveAccountChange,
                )
                Text("保存账号（密码以 MD5 形式加密存储）", style = MaterialTheme.typography.bodyMedium)
            }

            if (state.savedUsers.isNotEmpty()) {
                OutlinedButton(onClick = { showSavedUsers = true }) {
                    Text("选择已保存账号")
                }
            }

            Button(
                onClick = viewModel::login,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("登录中…")
                } else {
                    Text("登录并获取门锁")
                }
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (showSavedUsers) {
        AlertDialog(
            onDismissRequest = { showSavedUsers = false },
            title = { Text("选择已保存账号") },
            text = {
                Column {
                    state.savedUsers.forEachIndexed { index, user ->
                        Text(
                            text = user.username,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.useSavedUser(index)
                                    showSavedUsers = false
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSavedUsers = false }) { Text("取消") }
            },
        )
    }

    if (state.pendingSchools.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("选择学校") },
            text = {
                Column {
                    state.pendingSchools.forEach { school ->
                        Text(
                            text = school.school?.schoolName ?: school.schoolNo.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectSchool(school) }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    if (state.pendingLocks.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("选择门锁") },
            text = {
                Column {
                    state.pendingLocks.forEach { lock ->
                        Text(
                            text = lock.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectLock(lock) }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }
}
