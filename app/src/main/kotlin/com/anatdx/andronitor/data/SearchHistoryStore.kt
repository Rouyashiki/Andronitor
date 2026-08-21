package com.anatdx.andronitor.data

import android.content.Context
import org.json.JSONArray

/** Recent search terms, most recent first. */
class SearchHistoryStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("andronitor_search_history", Context.MODE_PRIVATE)

    fun load(): List<String> {
        val raw = preferences.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
        }.getOrDefault(emptyList())
    }

    fun add(term: String): List<String> {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return load()
        val updated = (listOf(trimmed) + load().filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(MAX_TERMS)
        return save(updated)
    }

    fun remove(term: String): List<String> = save(load().filterNot { it == term })

    fun clear(): List<String> = save(emptyList())

    private fun save(terms: List<String>): List<String> {
        preferences.edit().putString(KEY, JSONArray(terms).toString()).apply()
        return terms
    }

    companion object {
        private const val KEY = "recent_searches"
        private const val MAX_TERMS = 10
    }
}
