package com.icenerd.giphy.data.model

import android.content.ContentValues
import android.database.Cursor

import com.icenerd.data.NetworkModel
import com.icenerd.giphy.data.orm.GifORM

import org.json.JSONException
import org.json.JSONObject

class GifModel : NetworkModel {

    val _id: String?
    var rating: String? = GifORM.RATING_RESTRICTED
    var fixed_height: String? = null
    var fixed_height_still: String? = null
    var fixed_height_downsampled: String? = null
    var fixed_width: String? = null
    var fixed_width_still: String? = null
    var fixed_width_downsampled: String? = null
    var fixed_height_small: String? = null
    var fixed_height_small_still: String? = null
    var fixed_width_small: String? = null
    var fixed_width_small_still: String? = null
    var downsized: String? = null
    var downsized_still: String? = null
    var downsized_large: String? = null
    var original: String? = null
    var original_still: String? = null
    var size: Long = 0

    private var mHashCode = 0

    constructor(cursor: Cursor) {
        _id = getString(cursor, GifORM.COL_ID)
        if (mHashCode == 0) {
            for (i in 0 until this._id!!.length) {
                mHashCode += Character.getNumericValue(this._id[i])
            }
        }
        rating = getString(cursor, GifORM.COL_RATING)

        fixed_height = getString(cursor, GifORM.COL_URL_FIXED_HEIGHT)
        fixed_height_still = getString(cursor, GifORM.COL_URL_FIXED_HEIGHT_STILL)
        fixed_height_downsampled = getString(cursor, GifORM.COL_URL_FIXED_HEIGHT_DOWNSAMPLED)
        fixed_width = getString(cursor, GifORM.COL_URL_FIXED_WIDTH)
        fixed_width_still = getString(cursor, GifORM.COL_URL_FIXED_WIDTH_STILL)
        fixed_width_downsampled = getString(cursor, GifORM.COL_URL_FIXED_WIDTH_DOWNSAMPLED)
        fixed_height_small = getString(cursor, GifORM.COL_URL_FIXED_HEIGHT_SMALL)
        fixed_height_small_still = getString(cursor, GifORM.COL_URL_FIXED_HEIGHT_SMALL_STILL)
        fixed_width_small = getString(cursor, GifORM.COL_URL_FIXED_WIDTH_SMALL)
        fixed_width_small_still = getString(cursor, GifORM.COL_URL_FIXED_WIDTH_SMALL_STILL)
        downsized = getString(cursor, GifORM.COL_URL_DOWNSIZED)
        downsized_still = getString(cursor, GifORM.COL_URL_DOWNSIZED_STILL)
        downsized_large = getString(cursor, GifORM.COL_URL_DOWNSIZED_LARGE)
        original = getString(cursor, GifORM.COL_URL_ORIGINAL)
        original_still = getString(cursor, GifORM.COL_URL_ORIGINAL_STILL)
        size = getLong(cursor, GifORM.COL_SIZE)
    }

    @Throws(JSONException::class)
    constructor(json: JSONObject) {
        _id = json.getString(GifORM.COL_ID)
        rating = getString(json, GifORM.COL_RATING)
        if (json.has("images")) {
            val jsonImages = json.getJSONObject("images")

            fixed_height = getString(jsonImages.getJSONObject(GifORM.COL_URL_FIXED_HEIGHT), "url")
            fixed_height_still = getString(jsonImages.getJSONObject(GifORM.COL_URL_FIXED_HEIGHT_STILL), "url")
            fixed_height_downsampled = getString(jsonImages.getJSONObject(GifORM.COL_URL_FIXED_HEIGHT_DOWNSAMPLED), "url")
            fixed_width = getString(jsonImages.getJSONObject(GifORM.COL_URL_FIXED_WIDTH), "url")
            fixed_width_still = getString(jsonImages.getJSONObject(GifORM.COL_URL_FIXED_WIDTH_STILL), "url")
            fixed_width_downsampled = getString(jsonImages.getJSONObject(GifORM.COL_URL_FIXED_WIDTH_DOWNSAMPLED), "url")
            fixed_height_small = getString(jsonImages.getJSONObject(GifORM.COL_URL_FIXED_HEIGHT_SMALL), "url")
            fixed_height_small_still = getString(jsonImages.getJSONObject(GifORM.COL_URL_FIXED_HEIGHT_SMALL_STILL), "url")
            fixed_width_small = getString(jsonImages.getJSONObject(GifORM.COL_URL_FIXED_WIDTH_SMALL), "url")
            fixed_width_small_still = getString(jsonImages.getJSONObject(GifORM.COL_URL_FIXED_WIDTH_SMALL_STILL), "url")
            downsized = getString(jsonImages.getJSONObject(GifORM.COL_URL_DOWNSIZED), "url")
            downsized_still = getString(jsonImages.getJSONObject(GifORM.COL_URL_DOWNSIZED_STILL), "url")
            downsized_large = getString(jsonImages.getJSONObject(GifORM.COL_URL_DOWNSIZED_LARGE), "url")
            original = getString(jsonImages.getJSONObject(GifORM.COL_URL_ORIGINAL), "url")
            original_still = getString(jsonImages.getJSONObject(GifORM.COL_URL_ORIGINAL_STILL), "url")
            size = java.lang.Long.parseLong(getString(jsonImages.getJSONObject(GifORM.COL_URL_ORIGINAL), "size")!!)
        }
    }

    override fun getContentValues(): ContentValues {
        val cValues = ContentValues()

        cValues.put(GifORM.COL_ID, _id)
        cValues.put(GifORM.COL_RATING, rating)
        cValues.put(GifORM.COL_URL_FIXED_HEIGHT, fixed_height)
        cValues.put(GifORM.COL_URL_FIXED_HEIGHT_STILL, fixed_height_still)
        cValues.put(GifORM.COL_URL_FIXED_HEIGHT_DOWNSAMPLED, fixed_height_downsampled)
        cValues.put(GifORM.COL_URL_FIXED_WIDTH, fixed_width)
        cValues.put(GifORM.COL_URL_FIXED_WIDTH_STILL, fixed_width_still)
        cValues.put(GifORM.COL_URL_FIXED_WIDTH_DOWNSAMPLED, fixed_width_downsampled)
        cValues.put(GifORM.COL_URL_FIXED_HEIGHT_SMALL, fixed_height_small)
        cValues.put(GifORM.COL_URL_FIXED_HEIGHT_SMALL_STILL, fixed_height_small_still)
        cValues.put(GifORM.COL_URL_FIXED_WIDTH_SMALL, fixed_width_small)
        cValues.put(GifORM.COL_URL_FIXED_WIDTH_SMALL_STILL, fixed_width_small_still)
        cValues.put(GifORM.COL_URL_DOWNSIZED, downsized)
        cValues.put(GifORM.COL_URL_DOWNSIZED_STILL, downsized_still)
        cValues.put(GifORM.COL_URL_DOWNSIZED_LARGE, downsized_large)
        cValues.put(GifORM.COL_URL_ORIGINAL, original)
        cValues.put(GifORM.COL_URL_ORIGINAL_STILL, original_still)
        cValues.put(GifORM.COL_SIZE, size)

        return cValues
    }

    @Throws(JSONException::class)
    override fun getJSONObject(): JSONObject {
        val json = JSONObject()

        json.put(GifORM.COL_ID, _id)
        json.put(GifORM.COL_RATING, rating)

        val jsonImages = JSONObject()
        jsonImages.put(GifORM.COL_URL_FIXED_HEIGHT, JSONObject().put("url", fixed_height))
        jsonImages.put(GifORM.COL_URL_FIXED_HEIGHT_STILL, JSONObject().put("url", fixed_height_still))
        jsonImages.put(GifORM.COL_URL_FIXED_HEIGHT_DOWNSAMPLED, JSONObject().put("url", fixed_height_downsampled))
        jsonImages.put(GifORM.COL_URL_FIXED_WIDTH, JSONObject().put("url", fixed_width))
        jsonImages.put(GifORM.COL_URL_FIXED_WIDTH_STILL, JSONObject().put("url", fixed_width_still))
        jsonImages.put(GifORM.COL_URL_FIXED_WIDTH_DOWNSAMPLED, JSONObject().put("url", fixed_width_downsampled))
        jsonImages.put(GifORM.COL_URL_FIXED_HEIGHT_SMALL, JSONObject().put("url", fixed_height_small))
        jsonImages.put(GifORM.COL_URL_FIXED_HEIGHT_SMALL_STILL, JSONObject().put("url", fixed_height_small_still))
        jsonImages.put(GifORM.COL_URL_FIXED_WIDTH_SMALL, JSONObject().put("url", fixed_width_small))
        jsonImages.put(GifORM.COL_URL_FIXED_WIDTH_SMALL_STILL, JSONObject().put("url", fixed_width_small_still))
        jsonImages.put(GifORM.COL_URL_DOWNSIZED, JSONObject().put("url", downsized))
        jsonImages.put(GifORM.COL_URL_DOWNSIZED_STILL, JSONObject().put("url", downsized_still))
        jsonImages.put(GifORM.COL_URL_DOWNSIZED_LARGE, JSONObject().put("url", downsized_large))
        jsonImages.put(GifORM.COL_URL_ORIGINAL, JSONObject().put("url", original).put("size", size.toString()))
        jsonImages.put(GifORM.COL_URL_ORIGINAL_STILL, JSONObject().put("url", original_still))
        json.put("images", jsonImages)

        return json
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is GifModel) return false
        val otherMyClass = other as GifModel?
        return if (otherMyClass!!._id!!.equals(this._id!!, ignoreCase = true)) true else false
    }

    override fun hashCode(): Int {
        if (mHashCode == 0) {
            for (i in 0 until this._id!!.length) {
                mHashCode += Character.getNumericValue(this._id[i])
            }
        }
        return mHashCode
    }
}
