package com.icenerd.giftv.data.orm

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log

import com.icenerd.data.ORM
import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.data.model.StatusModel


class StatusORM(db: SQLiteDatabase) : ORM<StatusModel>(db, TABLE) {
    companion object {
        private val TAG = "StatusORM"
        val TABLE = "status"
        val COL_UUID = "uuid"
        val COL_CHANNEL_TYPE = "channel_type"
        val COL_CHANNEL_ID = "channel_id"
        val COL_CREATED_ON = "created_on"
        /*--- CHANNEL_TYPE ---*/
        val CHANNEL_OFF = 0
        val CHANNEL_TWITCH = 1
        val CHANNEL_GIPHY = 2
    }

    override fun build(cursor: Cursor): StatusModel { return StatusModel(cursor) }

    override fun save(model: StatusModel): Boolean {
        val dupeModel = findWhere("${COL_UUID}=\"${model.getUUID()}\" AND ${COL_CREATED_ON}=${model.created_on}")
        if (dupeModel == null) {
            if (BuildConfig.DEBUG) Log.d(TAG, "no duplicate found")
            return super.save(model)
        } else {
            Log.d("StatusORM", "Status already saved")
            return true
        }
    }
}