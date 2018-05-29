package com.icenerd.giftv.data.model

import android.content.ContentValues
import android.database.Cursor

import com.icenerd.data.Model
import com.icenerd.giftv.data.orm.TCPServiceORM

class TCPServiceModel : Model {
    var name: String? = null
    var host: String? = null
    var port: Int = 0
    var id_status: String? = null
    constructor() {}
    constructor(cursor: Cursor) {
        name = getString(cursor, TCPServiceORM.COL_NAME)
        host = getString(cursor, TCPServiceORM.COL_HOST)
        port = getLong(cursor, TCPServiceORM.COL_PORT).toInt()
        id_status = getString(cursor, TCPServiceORM.COL_ID_STATUS)
    }
    override fun getContentValues(): ContentValues {
        val cvalues = ContentValues()
        cvalues.put(TCPServiceORM.COL_NAME, name)
        cvalues.put(TCPServiceORM.COL_HOST, host)
        cvalues.put(TCPServiceORM.COL_PORT, port)
        cvalues.put(TCPServiceORM.COL_ID_STATUS, id_status)
        return cvalues
    }
}