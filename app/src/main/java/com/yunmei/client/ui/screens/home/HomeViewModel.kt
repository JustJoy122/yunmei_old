package com.yunmei.client.ui.screens.home

import androidx.lifecycle.ViewModel
import com.yunmei.client.data.local.AccountStore
import com.yunmei.client.data.local.LockStore
import com.yunmei.client.data.local.StoredUser
import com.yunmei.client.data.model.Lock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val locks: List<Lock> = emptyList(),
    val selectedLock: Lock? = null,
    val savedAccounts: List<StoredUser> = emptyList(),
) {
    val isLoggedIn: Boolean get() = savedAccounts.isNotEmpty()
}

/**
 * 主页 = 概览：只负责「登录状态 + 门锁状态」展示，不承载开门/打卡/获取密码等操作
 * （这些已下沉到 [com.yunmei.client.ui.screens.actions.ActionsViewModel]）。
 */
class HomeViewModel(
    private val lockStore: LockStore,
    private val accountStore: AccountStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        reload()
    }

    fun reload() {
        val locks = lockStore.getAll().toMutableList()
        lockStore.getDefault()?.let { default ->
            locks.removeAll { it.label == default.label }
            locks.add(0, default)
        }
        _uiState.update {
            it.copy(
                locks = locks,
                selectedLock = it.selectedLock ?: locks.firstOrNull(),
                savedAccounts = accountStore.getAll(),
            )
        }
    }
}
