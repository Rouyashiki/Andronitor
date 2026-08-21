package com.anatdx.andronitor.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFiltersTest {
    private val alice = TwitterUser(
        restId = "1779556990500630528",
        name = "Anan",
        screenName = "Anatdx",
        bio = "幸せを願って",
        location = "Tokyo",
        link = "https://anatdx.example",
        isVerified = true,
        isFollower = true,
    )

    @Test
    fun defaultFieldsMatchNameScreenNameAndExactRestId() {
        val filters = SearchFilters()

        assertTrue(filters.matches(alice, "anat"))
        assertTrue(filters.matches(alice, "Anan"))
        assertTrue(filters.matches(alice, "1779556990500630528"))
        // Rest ids match exactly, never as a substring.
        assertFalse(filters.matches(alice, "17795569"))
        // Bio is not a default field.
        assertFalse(filters.matches(alice, "幸せ"))
    }

    @Test
    fun selectedFieldsWidenAndNarrowTheMatch() {
        assertTrue(SearchFilters(fields = setOf(SearchField.BIO)).matches(alice, "幸せ"))
        assertTrue(SearchFilters(fields = setOf(SearchField.LOCATION)).matches(alice, "tokyo"))
        assertTrue(SearchFilters(fields = setOf(SearchField.LINK)).matches(alice, "anatdx.example"))
        assertFalse(SearchFilters(fields = setOf(SearchField.LOCATION)).matches(alice, "Anan"))
    }

    @Test
    fun emptyFieldSelectionFallsBackToTextFields() {
        val filters = SearchFilters(fields = emptySet())

        assertTrue(filters.matches(alice, "幸せ"))
        assertTrue(filters.matches(alice, "Anan"))
        assertFalse(filters.matches(alice, "1779556990500630528"))
    }

    @Test
    fun attributeFiltersApplyWithoutAQuery() {
        assertTrue(SearchFilters(isVerified = FilterState.YES).matches(alice, ""))
        assertFalse(SearchFilters(isVerified = FilterState.NO).matches(alice, ""))
        assertTrue(SearchFilters(isProtected = FilterState.NO).matches(alice, ""))
        assertTrue(SearchFilters(isFollower = FilterState.YES).matches(alice, ""))
        assertFalse(SearchFilters(isFollowing = FilterState.YES).matches(alice, ""))
    }

    @Test
    fun attributeFiltersCombineWithTheQuery() {
        val verifiedOnly = SearchFilters(isVerified = FilterState.YES)
        val unverifiedOnly = SearchFilters(isVerified = FilterState.NO)

        assertTrue(verifiedOnly.matches(alice, "anat"))
        assertFalse(unverifiedOnly.matches(alice, "anat"))
        assertFalse(verifiedOnly.matches(alice, "nobody"))
    }

    @Test
    fun statusFilterMatchesNormalSuspendedAndRestricted() {
        val suspended = alice.copy(status = "suspended")
        val restricted = alice.copy(keptIdsStatus = "temporarily_restricted")

        assertTrue(SearchFilters(statuses = setOf(AccountStatus.NORMAL)).matches(alice, ""))
        assertFalse(SearchFilters(statuses = setOf(AccountStatus.NORMAL)).matches(suspended, ""))
        assertTrue(SearchFilters(statuses = setOf(AccountStatus.SUSPENDED)).matches(suspended, ""))
        // Restriction is recorded on kept_ids_status, not status.
        assertTrue(SearchFilters(statuses = setOf(AccountStatus.TEMPORARILY_RESTRICTED)).matches(restricted, ""))
        assertTrue(
            SearchFilters(statuses = setOf(AccountStatus.NORMAL, AccountStatus.SUSPENDED)).matches(suspended, ""),
        )
    }

    @Test
    fun activeCountIgnoresFieldSelectionAndFullStatusSet() {
        assertEqualsCount(0, SearchFilters())
        assertEqualsCount(0, SearchFilters(fields = setOf(SearchField.BIO)))
        assertEqualsCount(1, SearchFilters(isVerified = FilterState.YES))
        assertEqualsCount(2, SearchFilters(isVerified = FilterState.YES, isFollower = FilterState.NO))
        assertEqualsCount(1, SearchFilters(statuses = setOf(AccountStatus.SUSPENDED)))
        assertEqualsCount(0, SearchFilters(statuses = AccountStatus.entries.toSet()))
    }

    private fun assertEqualsCount(expected: Int, filters: SearchFilters) {
        org.junit.Assert.assertEquals(expected, filters.activeCount)
    }
}
