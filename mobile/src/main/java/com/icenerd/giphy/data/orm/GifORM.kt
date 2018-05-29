package com.icenerd.giphy.data.orm

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.icenerd.data.ORM
import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.MobileTVActivity
import com.icenerd.giftv.data.GIFTVDB
import com.icenerd.giphy.data.model.GifModel
import java.util.*

class GifORM(db: SQLiteDatabase) : ORM<GifModel>(db, TABLE) {
    companion object {
        val TABLE = "giphy_gif"

        val COL_ID = "id"
        val COL_RATING = "rating"

        val COL_URL_FIXED_HEIGHT = "fixed_height"
        val COL_URL_FIXED_HEIGHT_STILL = "fixed_height_still"
        val COL_URL_FIXED_HEIGHT_DOWNSAMPLED = "fixed_height_downsampled"
        val COL_URL_FIXED_WIDTH = "fixed_width"
        val COL_URL_FIXED_WIDTH_STILL = "fixed_width_still"
        val COL_URL_FIXED_WIDTH_DOWNSAMPLED = "fixed_width_downsampled"
        val COL_URL_FIXED_HEIGHT_SMALL = "fixed_height_small"
        val COL_URL_FIXED_HEIGHT_SMALL_STILL = "fixed_height_small_still"
        val COL_URL_FIXED_WIDTH_SMALL = "fixed_width_small"
        val COL_URL_FIXED_WIDTH_SMALL_STILL = "fixed_width_small_still"
        val COL_URL_DOWNSIZED = "downsized"
        val COL_URL_DOWNSIZED_STILL = "downsized_still"
        val COL_URL_DOWNSIZED_LARGE = "downsized_large"
        val COL_URL_ORIGINAL = "original"
        val COL_URL_ORIGINAL_STILL = "original_still"
        val COL_SIZE = "size"

        val RATING_YOUTH = "y"
        val RATING_GENERAL = "g"
        val RATING_PARENTAL_GUIDANCE = "pg"
        val RATING_PARENTAL_GUIDANCE_13 = "pg-13"
        val RATING_RESTRICTED = "r"
    }

    override fun build(cursor: Cursor): GifModel {
        return GifModel(cursor)
    }

    fun linkSearchGif(terms: String, offset: Int, GifID: String): Boolean {
        var bReturn = false

        val values = ContentValues()
        values.put("terms", terms)
        values.put("offset", offset)
        values.put(GifORM.COL_ID, GifID)

        val dbReturn = DB.insertWithOnConflict(GIFTVDB.TABLE_LINK_SEARCH_GIF, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        if (dbReturn > 0) {
            if (BuildConfig.DEBUG) Log.d(TABLE, "save( ${GIFTVDB.TABLE_LINK_SEARCH_GIF} ) = ${dbReturn}")
            bReturn = true
        } else {
            if (BuildConfig.DEBUG) Log.e(TABLE, "save( ${GIFTVDB.TABLE_LINK_SEARCH_GIF} )")
        }

        return bReturn
    }

    fun deleteSearchLinksAfter(offset: Int): Int {
        val iReturn = DB.delete(GIFTVDB.TABLE_LINK_SEARCH_GIF,"offset > ${offset}",null)

        if (iReturn > 0) {
            if (BuildConfig.DEBUG) Log.d(TABLE, "deleteWhere( ${GIFTVDB.TABLE_LINK_SEARCH_GIF} ) = ${iReturn}")
        } else {
            if (BuildConfig.DEBUG) Log.e(TABLE, "deleteWhere( ${GIFTVDB.TABLE_LINK_SEARCH_GIF} ) = ${iReturn}")
        }

        return iReturn
    }

    fun getSearch(terms: String): Cursor {
        val sqlQuery = "SELECT %s.* FROM %s INNER JOIN %s ON %s.%s LIKE '%s' AND %s.%s = %s.%s GROUP BY %s.%s ORDER BY %s.%s"
            .format(
                GifORM.TABLE, GifORM.TABLE,
                GIFTVDB.TABLE_LINK_SEARCH_GIF,
                GIFTVDB.TABLE_LINK_SEARCH_GIF, "terms", terms,
                GifORM.TABLE, GifORM.COL_ID, GIFTVDB.TABLE_LINK_SEARCH_GIF, GifORM.COL_ID,
                GifORM.TABLE, GifORM.COL_ID,
                GIFTVDB.TABLE_LINK_SEARCH_GIF, "offset"
            )
        return DB.rawQuery(sqlQuery, null)
    }

    fun tv_log(current_millis: Long, terms: String, model: GifModel) {
        val values = ContentValues()
        values.put(GifORM.COL_ID, model._id)
        values.put("created_on", current_millis)
        values.put("terms", terms)
        DB.insert(GIFTVDB.TABLE_LINK_TV_GIF, null, values)
    }

    fun tv_gif_list(): List<GifModel> {
        val sqlQuery = "SELECT %s.* FROM %s INNER JOIN %s ON %s.%s > %s AND %s.%s == %s.%s GROUP BY %s.%s ORDER BY RANDOM() LIMIT %s"
            .format(
                GifORM.TABLE, GifORM.TABLE,
                GIFTVDB.TABLE_LINK_TV_GIF,
                GIFTVDB.TABLE_LINK_TV_GIF, "created_on", System.currentTimeMillis() - 28800000,
                GifORM.TABLE, GifORM.COL_ID, GIFTVDB.TABLE_LINK_TV_GIF, GifORM.COL_ID,
                GifORM.TABLE, GifORM.COL_ID,
                MobileTVActivity.BATCH_SIZE
            )
        val cursor = DB.rawQuery(sqlQuery, null)
        val lstReturn = ArrayList<GifModel>()
        if (cursor.count > 0) {
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                lstReturn.add(build(cursor))
                cursor.moveToNext()
            }
        }
        cursor.close()
        return lstReturn
    }
}
