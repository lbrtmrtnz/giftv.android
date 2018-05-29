package com.icenerd.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.icenerd.giftv.BuildConfig
import java.util.*

/***
 * Object Relational Mapping, one of these should exist per model that will be stored locally
 * @param <T> The model being mapped out
 **/
abstract class ORM<T : Model>
/**
 * @param db database for operations. *important!* open and close are done outside of this class
 * @param strTableName      name of the table in the DB for these operations.
 * *                        The class extending this should set the table name in constructor and
 * *                        provide a constructor with at least ( db ), exclusive of a table name
 */
protected constructor(protected val DB: SQLiteDatabase, private val mTableName: String) {
    companion object { private val TAG = "ORM" }

    /**
     * @param cursor database entry to be converted into our data model
     * *
     * @return data model
     */
    abstract protected fun build(cursor: Cursor): T

    /**
     * @param whereCond condition for searching tables ie( "COLUMN_WITH_DATA > 10" ), null fails
     * *
     * @return the first match for the condition
     */
    fun findWhere(whereCond: String): T? {
        var model: T? = null
        val sqlQuery = "SELECT * FROM $mTableName WHERE $whereCond"
        val cursor = DB.rawQuery(sqlQuery, null)
        if( cursor.count > 0 ) {
            cursor.moveToFirst()
            model = build(cursor)
        }
        if (BuildConfig.DEBUG) Log.d( TAG, "findWhere( $whereCond ) found ${cursor?.count}" )
        cursor?.close()
        return model
    }

    /**
     * @param whereCond condition for searching tables ie( "COLUMN_WITH_DATA > 10" ), null gets all
     * *
     * @return cursor with results
     */
    fun getCursorAll(whereCond: String?): Cursor {
        var sqlQuery = "SELECT * from $mTableName"
        if( whereCond != null && !whereCond.isEmpty() ) sqlQuery += " WHERE $whereCond"
        val cursorReturn = DB.rawQuery(sqlQuery, null)
        cursorReturn.moveToFirst()
        return cursorReturn
    }

    /**
     * @return convenience getCursorAll(null)
     */
    val cursorAll: Cursor get() = getCursorAll(null)

    /**
     * @param whereCond conditions for searching tables ie( "COLUMN_WITH_DATA > 10" ), null gets all
     * *
     * @return list of models that meet whereCond.
     */
    fun getAll(whereCond: String?): List<T> {
        val raModels = ArrayList<T>()
        val cursor = getCursorAll(whereCond)
        val count = cursor.count
        if( count > 0 ) {
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                raModels.add(build(cursor))
                cursor.moveToNext()
            }
            cursor.close()
        }

        if (BuildConfig.DEBUG) Log.d( TAG, "getAll( $whereCond ) found $count" )
        return raModels
    }

    /**
     * @return convenience getAll(null)
     */
    val all: List<T> get() = getAll(null)

    /**
     * @param raModels list of models to call saveAll( T model ) on
     * *
     * @return number of tables affected
     */
    fun saveAll(raModels: List<T>): Int {
        var iReturn = 0
        for( model in raModels ) {
            iReturn += saveAll(model)
        }
        return iReturn
    }

    /**
     * @param model model to save locally
     * *
     * @return number of tables saved successfully. This is different than save(T model) in that
     * * you may override this on a model whose save operation must also save another table.
     */
    fun saveAll(model: T): Int {
        return if(save(model)) 1 else 0
    }

    /**
     * @param raModels list of models to call save( T model )
     * *
     * @return success/fail, will return false on 1 or more failures.
     */
    open fun save(raModels: List<T>): Boolean {
        var bReturn = true

        for( model in raModels ) {
            if(!save(model)) bReturn = false
        }

        return bReturn
    }

    /**
     * @param model the model to save locally
     * *
     * @return success/fail
     */
    open fun save(model: T): Boolean {
        val values = model.getContentValues()
        val row_id = DB.insertWithOnConflict(mTableName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        if (BuildConfig.DEBUG) Log.d(TAG, "save( $mTableName ) = $row_id" )
        return row_id > 0
    }

    /**
     * @param whereCond conditions for deletions. ie( "PRIMARY_KEY = 52" )
     * *
     * @return number of rows deleted
     */
    fun deleteWhere(whereCond: String?): Int {
        val iReturn = DB.delete(mTableName, whereCond, null)
        if( BuildConfig.DEBUG ) Log.d(TAG, "deleteWhere( $mTableName, $whereCond ) = $iReturn")
        return iReturn
    }

}