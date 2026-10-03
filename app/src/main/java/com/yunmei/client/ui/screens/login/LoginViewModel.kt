package com.yunmei.client.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunmei.client.data.local.AccountStore
import com.yunmei.client.data.local.LockStore
import com.yunmei.client.data.local.StoredUser
import com.yunmei.client.data.model.Lock
import com.yunmei.client.data.model.SchoolEntry
import com.yunmei.client.data.repository.YunMeiException
import com.yunmei.client.data.repository.YunMeiRepository
import com.yunmei.client.data.security.Md5
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val saveAccount: Boolean = true,
    val loading: Boolean = false,
    val message: String? = null,
    val savedUsers: List<StoredUser> = emptyList(),
    val pendingSchools: List<SchoolEntry> = emptyList(),
    val pendingLocks: List<Lock> = emptyList(),
    val done: Boolean = false,
)

class LoginViewModel(
    private val repository: YunMeiRepository,
    private val accountStore: AccountStore,
    private val lockStore: LockStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(savedUsers = accountStore.getAll())
    )
    val uiState: StateFlow<LoginUiState> = _uiState

    private var pendingSchool: SchoolEntry? = null

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }

    fun onSaveAccountChange(value: Boolean) = _uiState.update { it.copy(saveAccount = value) }

    fun useSavedUser(index: Int) {
        val user = _uiState.value.savedUsers.getOrNull(index) ?: return
        _uiState.update { it.copy(username = user.username, password = "") }
    }

    fun login() {
        val state = _uiState.value
        if (state.loading) return
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(message = "请输入账号和密码") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, message = null) }
            runCatching {
                repository.login(state.username, Md5.hex(state.password))
                val schools = repository.fetchSchools()
                if (schools.isEmpty()) throw YunMeiException("账号不属于任何学校")
                if (schools.size == 1) {
                    proceedWithSchool(schools.first())
                } else {
                    _uiState.update { it.copy(loading = false, pendingSchools = schools) }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        message = (error as? YunMeiException)?.message ?: "登录失败：${error.message}",
                    )
                }
            }
        }
    }

    fun selectSchool(school: SchoolEntry) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, message = null) }
            runCatching { proceedWithSchool(school) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            message = (error as? YunMeiException)?.message ?: "获取门锁失败：${error.message}",
                        )
                    }
                }
        }
    }

    fun selectLock(lock: Lock) {
        val school = pendingSchool ?: return
        viewModelScope.launch {
            finishLogin(school, lock)
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private suspend fun proceedWithSchool(school: SchoolEntry) {
        val locks = repository.fetchLocks(school)
        if (locks.isEmpty()) {
            saveAccountIfNeeded()
            _uiState.update {
                it.copy(
                    loading = false,
                    message = "账号已保存，但服务器未返回门锁。请返回后进入“门锁管理”，用“扫码添加”或“手动导入”添加门锁。",
                )
            }
            return
        }
        pendingSchool = school
        if (locks.size == 1) {
            finishLogin(school, locks.first())
        } else {
            _uiState.update { it.copy(loading = false, pendingLocks = locks) }
        }
    }

    private fun saveAccountIfNeeded() {
        val state = _uiState.value
        if (state.saveAccount) {
            accountStore.add(
                StoredUser(
                    username = state.username,
                    usernameMd5 = Md5.hex(state.username),
                    passwordMd5 = Md5.hex(state.password),
                )
            )
        }
    }

    private fun finishLogin(school: SchoolEntry, lock: Lock) {
        saveAccountIfNeeded()
        runCatching { lockStore.add(lock) }
        if (lockStore.getDefault() == null) {
            lockStore.setDefault(lock)
        }
        _uiState.update { it.copy(loading = false, pendingSchools = emptyList(), pendingLocks = emptyList(), done = true) }
    }
}
