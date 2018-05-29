package com.icenerd.data

import android.content.ContentValues
import android.database.Cursor
import android.graphics.Color

import org.json.JSONException
import org.json.JSONObject

abstract class Model {
    companion object {
        val NULL_BOOLEAN = false
        val NULL_COLOR = -1
        val NULL_INT = -1
        val NULL_LONG: Long = -1
        val NULL_STRING = "null"
    }

    abstract fun getContentValues(): ContentValues

    protected fun toColorHex(color: Int): String {
        return if (color != NULL_COLOR) String.format("#%06X", 0xFFFFFF and color) else NULL_STRING
    }

    // Cursor convenience methods
    protected fun getBoolean(cursor: Cursor, COL: String): Boolean {
        return if (cursor.isNull(cursor.getColumnIndex(COL))) false else cursor.getInt(cursor.getColumnIndex(COL)) != 0
    }
    protected fun getColor(cursor: Cursor, COL: String): Int {
        val colorHex = getString(cursor, COL)
        return if (colorHex != null && colorHex.toLowerCase() != NULL_STRING) Color.parseColor(colorHex) else NULL_COLOR
    }
    protected fun getInt(cursor: Cursor, COL: String): Int {
        return if (cursor.isNull(cursor.getColumnIndex(COL))) NULL_INT else cursor.getInt(cursor.getColumnIndex(COL))
    }
    protected fun getLong(cursor: Cursor, COL: String): Long {
        return if (!cursor.isNull(cursor.getColumnIndex(COL))) cursor.getLong(cursor.getColumnIndex(COL)) else NULL_LONG
    }
    protected fun getString(cursor: Cursor, COL: String): String? {
        return if (!cursor.isNull(cursor.getColumnIndex(COL))) cursor.getString(cursor.getColumnIndex(COL)) else null
    }

    // JSON convenience methods
    @Throws(JSONException::class)
    protected fun getBoolean(json: JSONObject, COL: String): Boolean {
        return if (json.has(COL) && !json.isNull(COL)) json.getBoolean(COL) else NULL_BOOLEAN
    }
    @Throws(JSONException::class)
    protected fun getColor(json: JSONObject, COL: String): Int {
        val colorHex = getString(json, COL)
        return if (colorHex != null && colorHex != NULL_STRING) Color.parseColor(colorHex) else NULL_COLOR
    }
    @Throws(JSONException::class)
    protected fun getInt(json: JSONObject, COL: String): Int {
        return if (json.has(COL) && !json.isNull(COL)) json.getInt(COL) else NULL_INT
    }
    @Throws(JSONException::class)
    protected fun getLong(json: JSONObject, COL: String): Long {
        return if (json.has(COL) && !json.isNull(COL)) json.getLong(COL) else NULL_LONG
    }
    @Throws(JSONException::class)
    protected fun getString(json: JSONObject, COL: String): String? {
        return if (json.has(COL) && !json.isNull(COL)) json.getString(COL) else null
    }
}
