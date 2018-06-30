package com.icenerd.giftv.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.data.orm.StatusORM
import com.icenerd.giftv.data.orm.TCPServiceORM
import com.icenerd.giphy.data.orm.GifORM

class GIFTVDB(ctx: Context) : SQLiteOpenHelper(ctx, NAME, null, VERSION) {
    companion object {
        private val NAME = "com.icenerd.giftv"
        private val VERSION = 1

        val TABLE_LINK_SEARCH_GIF = "search_gif"
        val TABLE_LINK_TV_GIF = "tv_gif"
    }
    override fun onCreate(database: SQLiteDatabase) {
        if (BuildConfig.DEBUG) Log.d(NAME, "onCreate()")
        var strQuery: String

        strQuery = "CREATE TABLE IF NOT EXISTS " + TCPServiceORM.TABLE + "(" +
                TCPServiceORM.COL_NAME + " TEXT NOT NULL," +
                TCPServiceORM.COL_HOST + " TEXT NOT NULL," +
                TCPServiceORM.COL_PORT + " INTEGER DEFAULT 0," +
                TCPServiceORM.COL_ID_STATUS + " INTEGER" +
                ");"
        database.execSQL(strQuery)
        if (BuildConfig.DEBUG) Log.d(NAME, "created table " + TCPServiceORM.TABLE)

        strQuery = "CREATE TABLE IF NOT EXISTS " + StatusORM.TABLE + "(" +
                StatusORM.COL_UUID + " TEXT NOT NULL," +
                StatusORM.COL_CHANNEL_TYPE + " TINYINT(1) DEFAULT 0," +
                StatusORM.COL_CHANNEL_ID + " INTEGER," +
                StatusORM.COL_CREATED_ON + " INTEGER NOT NULL" +
                ");"
        database.execSQL(strQuery)
        if (BuildConfig.DEBUG) Log.d(NAME, "created table " + StatusORM.TABLE)

        strQuery = "CREATE TABLE IF NOT EXISTS " + GifORM.TABLE + "(" +
                GifORM.COL_ID + " TEXT PRIMARY KEY," +
                GifORM.COL_RATING + " TEXT NOT NULL," +
                GifORM.COL_URL_FIXED_HEIGHT + " TEXT NOT NULL," +
                GifORM.COL_URL_FIXED_HEIGHT_STILL + " TEXT NOT NULL," +
                GifORM.COL_URL_FIXED_HEIGHT_DOWNSAMPLED + " TEXT NOT NULL," +
                GifORM.COL_URL_FIXED_WIDTH + " TEXT NOT NULL," +
                GifORM.COL_URL_FIXED_WIDTH_STILL + " TEXT NOT NULL," +
                GifORM.COL_URL_FIXED_WIDTH_DOWNSAMPLED + " TEXT NOT NULL," +
                GifORM.COL_URL_FIXED_HEIGHT_SMALL + " TEXT NOT NULL," +
                GifORM.COL_URL_FIXED_HEIGHT_SMALL_STILL + " TEXT NOT NULL," +
                GifORM.COL_URL_FIXED_WIDTH_SMALL + " TEXT NOT NULL," +
                GifORM.COL_URL_FIXED_WIDTH_SMALL_STILL + " TEXT NOT NULL," +
                GifORM.COL_URL_DOWNSIZED + " TEXT NOT NULL," +
                GifORM.COL_URL_DOWNSIZED_STILL + " TEXT NOT NULL," +
                GifORM.COL_URL_DOWNSIZED_LARGE + " TEXT NOT NULL," +
                GifORM.COL_URL_ORIGINAL + " TEXT NOT NULL," +
                GifORM.COL_URL_ORIGINAL_STILL + " TEXT NOT NULL," +
                GifORM.COL_SIZE + " INTEGER NOT NULL" +
                ");"
        database.execSQL(strQuery)
        if (BuildConfig.DEBUG) Log.d(NAME, "created table " + GifORM.TABLE)

        strQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_LINK_SEARCH_GIF + "(" +
                "terms TEXT NOT NULL," +
                "offset INTEGER NOT NULL," +
                GifORM.COL_ID + " TEXT NOT NULL" +
                ");"
        database.execSQL(strQuery)
        if (BuildConfig.DEBUG) Log.d(NAME, "created table " + StatusORM.TABLE)

        strQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_LINK_TV_GIF + "(" +
                GifORM.COL_ID + " TEXT NOT NULL," +
                "terms TEXT," +
                "created_on INTEGER NOT NULL" +
                ");"
        database.execSQL(strQuery)
        if (BuildConfig.DEBUG) Log.d(NAME, "created table " + StatusORM.TABLE)

    }
    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (BuildConfig.DEBUG) Log.w(NAME, "Upgrading from $oldVersion to $newVersion")

        database.execSQL("DROP TABLE IF EXISTS $TABLE_LINK_SEARCH_GIF")
        database.execSQL("DROP TABLE IF EXISTS $TABLE_LINK_TV_GIF")
        database.execSQL("DROP TABLE IF EXISTS " + TCPServiceORM.TABLE)
        database.execSQL("DROP TABLE IF EXISTS " + StatusORM.TABLE)
        database.execSQL("DROP TABLE IF EXISTS " + GifORM.TABLE)
        onCreate(database)
    }
}
