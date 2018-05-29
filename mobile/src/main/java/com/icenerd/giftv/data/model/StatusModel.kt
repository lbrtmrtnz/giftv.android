package com.icenerd.giftv.data.model

import android.content.ContentValues
import android.database.Cursor

import com.icenerd.data.NetworkModel
import com.icenerd.giftv.data.orm.StatusORM

import org.json.JSONException
import org.json.JSONObject

class StatusModel: NetworkModel {

    private var uuid: String?
    var channel_type = StatusORM.CHANNEL_OFF // 0 = CHANNEL_OFF
    var channel_id: Long = 0
    var created_on: Long = 0

    fun getUUID(): String? {
        return uuid
    }

    constructor(UUID: String): super() {
        uuid = UUID
        created_on = System.currentTimeMillis()
    }

    @Throws(JSONException::class)
    constructor(json: JSONObject): super() {
        uuid = getString(json, StatusORM.COL_UUID)
        channel_type = getInt(json, StatusORM.COL_CHANNEL_TYPE)
        channel_id = getLong(json, StatusORM.COL_CHANNEL_ID)
        created_on = getLong(json, StatusORM.COL_CREATED_ON)
    }

    constructor(cursor: Cursor): super() {
        uuid = getString(cursor, StatusORM.COL_UUID)
        channel_type = getInt(cursor, StatusORM.COL_CHANNEL_TYPE)
        channel_id = getLong(cursor, StatusORM.COL_CHANNEL_ID)
        created_on = getLong(cursor, StatusORM.COL_CREATED_ON)
    }

    @Throws(JSONException::class)
    override fun getJSONObject(): JSONObject {
        val json = JSONObject()

        json.put(StatusORM.COL_UUID, jsonPutOrNULL(uuid))
        json.put(StatusORM.COL_CHANNEL_TYPE, if (channel_type == NULL_INT) JSONObject.NULL else channel_type)
        json.put(StatusORM.COL_CHANNEL_ID, if (channel_id == NULL_LONG) JSONObject.NULL else channel_id)
        json.put(StatusORM.COL_CREATED_ON, created_on)

        return json
    }

    override fun getContentValues(): ContentValues {
        val values = ContentValues()

        values.put(StatusORM.COL_UUID, uuid)
        values.put(StatusORM.COL_CHANNEL_TYPE, if (channel_type == NULL_INT) null else channel_type)
        values.put(StatusORM.COL_CHANNEL_ID, if (channel_id == NULL_LONG) null else channel_id)
        values.put(StatusORM.COL_CREATED_ON, created_on)

        return values
    }
}

