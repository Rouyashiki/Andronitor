package com.anatdx.andronitor.model

/** A field the local user search can match a query against. */
enum class SearchField { REST_ID, SCREEN_NAME, NAME, BIO, LOCATION, LINK }

/** Tri-state filter: no constraint, must be true, or must be false. */
enum class FilterState { ALL, YES, NO }

enum class AccountStatus(val key: String) {
    NORMAL("normal"),
    SUSPENDED("suspended"),
    DEACTIVATED("deactivated"),
    TEMPORARILY_RESTRICTED("temporarily_restricted"),
}

data class SearchFilters(
    val fields: Set<SearchField> = DEFAULT_FIELDS,
    val isFollower: FilterState = FilterState.ALL,
    val isFollowing: FilterState = FilterState.ALL,
    val isVerified: FilterState = FilterState.ALL,
    val isProtected: FilterState = FilterState.ALL,
    val statuses: Set<AccountStatus> = emptySet(),
) {
    /** Filters narrowing the result set, shown as a badge count on the filter button. */
    val activeCount: Int
        get() = listOf(isFollower, isFollowing, isVerified, isProtected).count { it != FilterState.ALL } +
            if (statuses.isNotEmpty() && statuses.size < AccountStatus.entries.size) 1 else 0

    /** An empty selection searches the text fields, matching upstream. */
    val effectiveFields: Set<SearchField>
        get() = if (fields.isEmpty()) FALLBACK_FIELDS else fields

    fun matches(user: TwitterUser, query: String): Boolean {
        if (!matchesState(isVerified, user.isVerified)) return false
        if (!matchesState(isProtected, user.isProtected)) return false
        if (!matchesState(isFollower, user.isFollower)) return false
        if (!matchesState(isFollowing, user.isFollowing)) return false
        if (statuses.isNotEmpty() && statuses.none { it.matches(user) }) return false
        if (query.isEmpty()) return true
        return effectiveFields.any { field -> field.matches(user, query) }
    }

    companion object {
        val DEFAULT_FIELDS = setOf(SearchField.REST_ID, SearchField.SCREEN_NAME, SearchField.NAME)
        private val FALLBACK_FIELDS = setOf(SearchField.SCREEN_NAME, SearchField.NAME, SearchField.BIO)

        private fun matchesState(state: FilterState, value: Boolean): Boolean = when (state) {
            FilterState.ALL -> true
            FilterState.YES -> value
            FilterState.NO -> !value
        }
    }
}

private fun AccountStatus.matches(user: TwitterUser): Boolean = when (this) {
    // Rows written before a status was ever recorded read as normal.
    AccountStatus.NORMAL -> user.status == key || user.status.isEmpty()
    AccountStatus.TEMPORARILY_RESTRICTED -> user.status == key || user.keptIdsStatus == key
    else -> user.status == key
}

private fun SearchField.matches(user: TwitterUser, query: String): Boolean = when (this) {
    // Rest ids are matched exactly — a substring match on a 19-digit id is noise.
    SearchField.REST_ID -> user.restId == query
    SearchField.SCREEN_NAME -> user.screenName.containsIgnoreCase(query)
    SearchField.NAME -> user.name.containsIgnoreCase(query)
    SearchField.BIO -> user.bio.containsIgnoreCase(query)
    SearchField.LOCATION -> user.location.containsIgnoreCase(query)
    SearchField.LINK -> user.link.containsIgnoreCase(query)
}

private fun String?.containsIgnoreCase(query: String): Boolean =
    this != null && contains(query, ignoreCase = true)
