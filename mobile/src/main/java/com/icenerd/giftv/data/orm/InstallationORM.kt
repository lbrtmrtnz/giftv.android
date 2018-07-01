package com.icenerd.giftv.data.orm

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

import com.icenerd.data.ORM
import com.icenerd.giftv.data.model.InstallationModel


class InstallationORM(db: SQLiteDatabase) : ORM<InstallationModel>(db, TABLE) {
    companion object {
        const val TABLE = "installation"

        const val COL_UUID = "uuid"

        const val COL_BRAND = "brand"
        const val COL_MANUFACTURER = "manufacturer"
        const val COL_MODEL = "model"
        const val COL_SERIAL = "serial"
        const val COL_SDK_INT = "sdk_int"
    }

    override fun build(cursor: Cursor): InstallationModel {
        return InstallationModel()
    }
}