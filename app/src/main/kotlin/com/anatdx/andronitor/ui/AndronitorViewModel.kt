package com.anatdx.andronitor.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anatdx.andronitor.data.AndronitorRepository
import com.anatdx.andronitor.model.Account
import com.anatdx.andronitor.model.AppSettings
import com.anatdx.andronitor.model.DashboardStats
import com.anatdx.andronitor.model.SyncFetchMode
import com.anatdx.andronitor.model.SyncProgress
import com.anatdx.andronitor.model.TwitterUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab { HOME, SEARCH, ACCOUNTS, SETTINGS }

data class AndronitorUiState(
    val tab: AppTab = AppTab.HOME,
    val accounts: List<Account> = emptyList(),
    val activeAccount: Account? = null,
    val stats: DashboardStats = DashboardStats(),
    val settings: AppSettings = AppSettings(),
    val users: List<TwitterUser> = emptyList(),
    val selectedCategory: String? = null,
    val selectedUser: TwitterUser? = null,
    val searchQuery: String = "",
    val searchResults: List<TwitterUser> = emptyList(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val progress: SyncProgress? = null,
    val error: String? = null,
)

class AndronitorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AndronitorRepository(application)
    private val _state = MutableStateFlow(AndronitorUiState(settings = repository.loadSettings()))
    val state: StateFlow<AndronitorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { loadAccounts() }
    }

    fun selectTab(tab: AppTab) {
        _state.update { it.copy(tab = tab, error = null) }
        if (tab == AppTab.HOME) refreshDashboard()
    }

    fun selectAccount(account: Account) {
        repository.setActiveAccount(account.id)
        _state.update { it.copy(activeAccount = account, selectedCategory = null, users = emptyList()) }
        refreshDashboard()
    }

    fun showCategory(category: String) {
        val account = _state.value.activeAccount ?: return
        _state.update { it.copy(selectedCategory = category, users = emptyList(), isLoading = true) }
        viewModelScope.launch {
            val users = runCatching { repository.usersForCategory(account.id, category) }
                .getOrElse { emptyList() }
            _state.update { it.copy(users = users, isLoading = false) }
        }
    }

    fun clearCategory() = _state.update { it.copy(selectedCategory = null, users = emptyList(), isLoading = false) }

    fun showUser(user: TwitterUser) = _state.update { it.copy(selectedUser = user) }

    fun closeUser() = _state.update { it.copy(selectedUser = null) }

    fun updateSearchQuery(query: String) = _state.update { it.copy(searchQuery = query) }

    fun search() {
        val account = _state.value.activeAccount ?: return
        val query = _state.value.searchQuery.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            val result = runCatching { repository.search(account.id, query, _state.value.settings) }
            _state.update {
                it.copy(
                    searchResults = result.getOrDefault(emptyList()),
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun addAccount(cookie: String) {
        if (cookie.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            val result = runCatching { repository.addAccount(cookie.trim(), _state.value.settings) }
            if (result.isSuccess) loadAccounts() else _state.update { it.copy(isBusy = false, error = result.exceptionOrNull()?.message) }
        }
    }

    fun updateCookie(account: Account, cookie: String) {
        viewModelScope.launch {
            val result = runCatching { repository.updateAccountCookie(account.id, cookie.trim()) }
            if (result.isFailure) _state.update { it.copy(error = result.exceptionOrNull()?.message) }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            runCatching { repository.removeAccount(account.id) }
            loadAccounts()
        }
    }

    fun runSync(mode: SyncFetchMode = _state.value.settings.syncFetchMode) {
        val account = _state.value.activeAccount ?: return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, progress = SyncProgress("Preparing"), error = null) }
            val result = runCatching {
                repository.sync(account, _state.value.settings, mode) { progress ->
                    _state.update { current -> current.copy(progress = progress) }
                }
            }
            _state.update {
                it.copy(
                    isBusy = false,
                    progress = result.exceptionOrNull()?.let { error -> SyncProgress("Failed", message = error.message ?: "Sync failed") }
                        ?: it.progress,
                    error = result.exceptionOrNull()?.message,
                )
            }
            loadAccounts()
            refreshDashboard()
        }
    }

    fun updateSettings(settings: AppSettings) {
        repository.saveSettings(settings)
        _state.update { it.copy(settings = settings) }
    }

    fun refreshQueryIds() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            val result = runCatching { repository.refreshQueryIds(_state.value.settings) }
            result.onSuccess { settings -> _state.update { it.copy(settings = settings) } }
            _state.update { it.copy(isBusy = false, error = result.exceptionOrNull()?.message) }
        }
    }

    fun writeBackup(destination: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            val result = runCatching { repository.writeBackup(destination) }
            _state.update { it.copy(isBusy = false, error = result.exceptionOrNull()?.message) }
        }
    }

    fun restoreBackup(source: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null) }
            val result = runCatching { repository.restoreBackup(source) }
            _state.update { it.copy(isBusy = false, error = result.exceptionOrNull()?.message) }
            if (result.isSuccess) loadAccounts()
        }
    }

    private suspend fun loadAccounts() {
        val accounts = runCatching { repository.loadAccounts() }.getOrDefault(emptyList())
        val storedId = repository.activeAccountId()
        val active = accounts.firstOrNull { it.id == storedId } ?: accounts.firstOrNull()
        if (active != null && active.id != storedId) repository.setActiveAccount(active.id)
        _state.update { it.copy(accounts = accounts, activeAccount = active, isLoading = false, isBusy = false) }
        if (active != null) refreshDashboard()
    }

    private fun refreshDashboard() {
        val account = _state.value.activeAccount ?: return
        viewModelScope.launch {
            val stats = runCatching { repository.dashboard(account.id) }.getOrDefault(DashboardStats())
            _state.update { it.copy(stats = stats) }
        }
    }
}
