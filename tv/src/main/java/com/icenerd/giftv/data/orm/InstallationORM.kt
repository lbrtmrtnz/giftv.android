package com.icenerd.giftv.data.orm

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

import com.icenerd.data.ORM
import com.icenerd.giftv.data.model.InstallationModel


class InstallationORM(db: SQLiteDatabase) : ORM<InstallationModel>(db, TABLE) {
    companion object {
        val TABLE = "installation"

        val COL_UUID = "uuid"

        val COL_BRAND = "brand"
        val COL_MANUFACTURER = "manufacturer"
        val COL_MODEL = "model"
        val COL_SERIAL = "serial"
        val COL_SDK_INT = "sdk_int"
    }

    override fun build(cursor: Cursor): InstallationModel {
        return InstallationModel()
    }
}