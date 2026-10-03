package com.yunmei.client.ui.screens.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yunmei.client.data.ble.UnlockManager
import com.yunmei.client.data.local.AccountStore
import com.yunmei.client.data.local.LockStore
import com.yunmei.client.data.local.StoredUser
import com.yunmei.client.data.location.LocationProvider
import com.yunmei.client.data.model.Lock
import com.yunmei.client.data.preferences.AppPreferences
import com.yunmei.client.data.preferences.AppSettings
import com.yunmei.client.data.repository.YunMeiException
import com.yunmei.client.data.repository.YunMeiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActionsUiState(
    val locks: List<Lock> = emptyList(),
    val selectedLock: Lock? = null,
    val busy: Boolean = false,
    val progress: Int = 0,
    val message: String? = null,
    val battery: Int? = null,
    val code: String? = null,
)

/**
 * 「开门」功能页：开门 / 打卡 / 获取开门密码。原主页的开门逻辑整体迁移至此。
 */
class ActionsViewModel(
    private val lockStore: LockStore,
    private val accountStore: AccountStore,
    private val repository: YunMeiRepository,
    private val appPreferences: AppPreferences,
    private val unlockManager: UnlockManager,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActionsUiState())
    val uiState: StateFlow<ActionsUiState> = _uiState

    /** 设置页开关（快速连接/自动获取密码/隐藏获取密码/隐藏打卡等）在此统一订阅，跨页生效。 */
    val settings: StateFlow<AppSettings> = appPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    init {
        reloadLocks()
    }

    fun reloadLocks() {
        val locks = lockStore.getAll().toMutableList()
        lockStore.getDefault()?.let { default ->
            locks.removeAll { it.label == default.label }
            locks.add(0, default)
        }
        _uiState.update {
            it.copy(locks = locks, selectedLock = it.selectedLock ?: locks.firstOrNull())
        }
    }

    fun selectLock(lock: Lock) {
        _uiState.update { it.copy(selectedLock = lock, code = null, battery = null, message = null) }
    }

    fun setMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun openDoor(quickConnect: Boolean) {
        val lock = _uiState.value.selectedLock ?: return
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, progress = 0, message = null, code = null, battery = null) }
        unlockManager.openDoor(lock, quickConnect, object : UnlockManager.Listener {
            override fun onProgress(percent: Int, message: String) {
                _uiState.update { it.copy(progress = percent, message = message) }
            }

            override fun onBattery(percent: Int) {
                _uiState.update { it.copy(battery = percent) }
            }

            override fun onSuccess() {
                _uiState.update { it.copy(busy = false, progress = 100, message = "开门完成") }
                // 「开门时同时获取密码」：开门成功后自动拉取开门密码。
                if (settings.value.autoCode) {
                    getCode()
                }
            }

            override fun onFailure(message: String) {
                _uiState.update { it.copy(busy = false, message = message) }
            }
        })
    }

    fun getCode() {
        val lock = _uiState.value.selectedLock ?: return
        val user = accountFor(lock)
        if (user == null) {
            _uiState.update { it.copy(message = "当前门锁的账号未保存") }
            return
        }
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, message = "登录并获取密码", code = null) }
            runCatching { repository.getLockPassword(user, lock) }
                .onSuccess { code ->
                    _uiState.update { it.copy(busy = false, code = code, message = "获取成功") }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            busy = false,
                            message = (error as? YunMeiException)?.message ?: "获取密码失败：${error.message}",
                        )
                    }
                }
        }
    }

    fun sign(useLastLocation: Boolean, saveLocation: Boolean) {
        val lock = _uiState.value.selectedLock ?: return
        val user = accountFor(lock)
        if (user == null) {
            _uiState.update { it.copy(message = "当前门锁的账号未保存") }
            return
        }
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, progress = 0, message = "正在定位", code = null) }

            val location = if (useLastLocation) {
                appPreferences.getLastLocation().takeIf { it.isNotBlank() }
            } else {
                runCatching { locationProvider.getLocation() }.getOrNull()
            }
            if (location.isNullOrBlank()) {
                _uiState.update { it.copy(busy = false, message = "定位失败，请检查权限或定位服务") }
                return@launch
            }
            if (saveLocation) {
                appPreferences.setLastLocation(location)
            }

            _uiState.update { it.copy(progress = 60, message = "正在打卡") }
            runCatching { repository.sign(user, lock, location) }
                .onSuccess { msg ->
                    _uiState.update { it.copy(busy = false, progress = 100, message = msg) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            busy = false,
                            message = (error as? YunMeiException)?.message ?: "打卡失败：${error.message}",
                        )
                    }
                }
        }
    }

    /**
     * 解析门锁关联账号。找不到对应账号时，若设置页开启了
     * 「无对应账号时使用首个账号」，则回退到首个已保存账号。
     */
    private fun accountFor(lock: Lock): StoredUser? {
        accountStore.getByUsernameMd5(lock.usernameMd5)?.let { return it }
        if (settings.value.alwaysCode) {
            accountStore.getAll().firstOrNull()?.let { return it }
        }
        return null
    }
}
