package com.yunmei.client.ui.screens.locks

import androidx.lifecycle.ViewModel
import com.yunmei.client.data.local.AccountStore
import com.yunmei.client.data.local.LockStore
import com.yunmei.client.data.local.StoredUser
import com.yunmei.client.data.model.Lock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class LockListUiState(
    val locks: List<Lock> = emptyList(),
    val defaultLock: Lock? = null,
    val message: String? = null,
    /** 已保存账号的 usernameMd5 列表，用于手动导入时把门锁关联到账号。 */
    val savedAccounts: List<StoredUser> = emptyList(),
)

class LockListViewModel(
    private val lockStore: LockStore,
    private val accountStore: AccountStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockListUiState())
    val uiState: StateFlow<LockListUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                locks = lockStore.getAll(),
                defaultLock = lockStore.getDefault(),
                savedAccounts = accountStore.getAll(),
            )
        }
    }

    fun setDefault(lock: Lock) {
        lockStore.setDefault(lock)
        refresh()
    }

    fun remove(lock: Lock) {
        lockStore.remove(lock.label)
        refresh()
    }

    fun addFromScan(content: String?) {
        if (content.isNullOrBlank()) {
            _uiState.update { it.copy(message = "未扫描到内容") }
            return
        }
        val lock = Lock.from(content)
        if (lock == null) {
            _uiState.update { it.copy(message = "二维码无法识别") }
            return
        }
        if (lock.secret.isBlank()) {
            _uiState.update { it.copy(message = "该二维码不含门锁密钥，无法添加") }
            return
        }
        addLock(lock)
    }

    /**
     * 手动导入门锁。字段与原版 `lock_info` 分享二维码一致：
     * label/mac/writeUuid/serviceUuid/secret/schoolNo/lockNo，usernameMd5 关联到所选账号。
     */
    fun addManual(
        label: String,
        mac: String,
        writeUuid: String,
        serviceUuid: String,
        secret: String,
        schoolNo: String,
        lockNo: String,
        usernameMd5: String,
    ) {
        if (writeUuid.isBlank() || serviceUuid.isBlank()) {
            _uiState.update { it.copy(message = "写特征 UUID 与服务 UUID 不能为空") }
            return
        }
        if (secret.isBlank()) {
            _uiState.update { it.copy(message = "门锁密钥不能为空，无法开门") }
            return
        }
        if (schoolNo.isBlank() || lockNo.isBlank()) {
            _uiState.update { it.copy(message = "学校编号与门锁 ID 不能为空") }
            return
        }
        val lock = Lock(
            label = label.trim().ifBlank { "手动门锁-${lockNo.trim().takeLast(5)}" },
            mac = mac.trim().ifBlank { lockNo.trim() },
            writeUuid = writeUuid.trim(),
            serviceUuid = serviceUuid.trim(),
            secret = secret.trim(),
            usernameMd5 = usernameMd5,
            schoolNo = schoolNo.trim(),
            lockNo = lockNo.trim(),
        )
        addLock(lock)
    }

    private fun addLock(lock: Lock) {
        runCatching { lockStore.add(lock) }
            .onSuccess {
                refresh()
                _uiState.update { it.copy(message = "已添加：${lock.label}") }
            }
            .onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "添加失败") }
            }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
