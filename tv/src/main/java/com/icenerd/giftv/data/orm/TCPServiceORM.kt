package com.icenerd.giftv.data.orm

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

import com.icenerd.data.ORM
import com.icenerd.giftv.data.model.TCPServiceModel

class TCPServiceORM(db: SQLiteDatabase) : ORM<TCPServiceModel>(db, TABLE) {
    override fun build(cursor: Cursor): TCPServiceModel {
        return TCPServiceModel(cursor)
    }
    companion object {
        val TABLE = "service_info"

        val COL_NAME = "name"
        val COL_HOST = "host"
        val COL_PORT = "port"
        val COL_ID_STATUS = "id_status"
    }
}