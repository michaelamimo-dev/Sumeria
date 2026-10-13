package com.michaelamimo.sumeria.data.local.entity

import org.json.JSONArray

/** Small, dependency-free JSON codec for the simple string array of author names. */
internal object AuthorsJson {
    fun encode(authors: List<String>): String = JSONArray(authors).toString()

    fun decode(json: String): List<String> {
        val array = JSONArray(json)
        return List(array.length()) { index -> array.getString(index) }
    }
}
