package com.icenerd.giphy.data.adapter


import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.util.LruCache
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import com.android.volley.toolbox.Volley
import com.icenerd.adapter.CursorRecyclerAdapter
import com.icenerd.giftv.R
import com.icenerd.giftv.ui.GifLoader
import com.icenerd.giphy.data.model.GifModel
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import java.util.*

class GifAdapter() : CursorRecyclerAdapter<GifAdapter.ViewHolder>(null) {
    private val TAG = "GifAdapter"

    var mListener: OnItemClickListener? = null
    var mLongListener: OnItemLongClickListener? = null

    var toggle_selection = false
    private var mRequestQueue: RequestQueue? = null
    private var mImgLoader: ImageLoader? = null
    private var mGifLoader: GifLoader? = null

    private var mAccentColor = 0

    private val mraGIPHYID = HashMap<GifModel, Boolean>()
    val selectedGIF: List<GifModel>
        get() {
            val raGIFID = ArrayList<GifModel>()
            for (key in mraGIPHYID.keys) {
                val value = mraGIPHYID[key]
                if (value!! && !raGIFID.contains(key)) {
                    raGIFID.add(key)
                }
            }
            return raGIFID
        }
    val selectedCount: Int
        get() {
            var count = 0
            val raGIFID = ArrayList<GifModel>()
            for (key in mraGIPHYID.keys) {
                val value = mraGIPHYID[key]
                if (value!! && !raGIFID.contains(key)) {
                    count++
                }
            }
            return count
        }

    interface OnItemClickListener {
        fun onItemClick(model: GifModel)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(model: GifModel)
    }

    fun clearSelections() {
        mraGIPHYID.clear()
        toggle_selection = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.giphy_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        if (mAccentColor == 0) {
            val ctx = recyclerView.context
            if (Build.VERSION.SDK_INT >= 23) {
                mAccentColor = ctx.resources.getColor(R.color.colorAccent, recyclerView.context.theme)
            } else {
                mAccentColor = ResourcesCompat.getColor(ctx.resources, R.color.colorAccent, ctx.theme)
            }
        }

        mRequestQueue = Volley.newRequestQueue(recyclerView.context)
        mImgLoader = ImageLoader(mRequestQueue, object : ImageLoader.ImageCache {
            private val mCache = LruCache<String, Bitmap>(50)
            override fun putBitmap(url: String, bmp: Bitmap) {
                mCache.put(url, bmp)
            }
            override fun getBitmap(url: String): Bitmap? {
                return mCache.get(url)
            }
        })
        mGifLoader = GifLoader(mRequestQueue, object : GifLoader.GifCache {
            private val mCache = LruCache<String, GifDrawable>(20)
            override fun putGif(url: String, gif: GifDrawable) {
                mCache.put(url, gif)
            }

            override fun getGif(url: String): GifDrawable? {
                return mCache.get(url)
            }
        })
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        mRequestQueue?.stop()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        if(item!=null) {
            val gameModel = GifModel(item)
            holder.bind(gameModel, mListener, mLongListener)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val frame_container_gif_item: View
        val container_gif_item: View
        val giphy_item_net_img: NetworkImageView
        val giphy_item_gif: GifImageView
        val progress_bar: ProgressBar

        init {
            frame_container_gif_item = itemView.findViewById(R.id.frame_container_gif_item)
            container_gif_item = itemView.findViewById(R.id.container_gif_item)
            giphy_item_net_img = itemView.findViewById(R.id.giphy_item_net_img) as NetworkImageView
            giphy_item_gif = itemView.findViewById(R.id.giphy_item_gif) as GifImageView
            progress_bar = itemView.findViewById(R.id.progress_bar) as ProgressBar
        }

        fun bind(model: GifModel, listener: OnItemClickListener?, longListener: OnItemLongClickListener?) {
            val drawable = giphy_item_gif.drawable as GifDrawable?
            if (drawable != null && drawable.isPlaying) {
                drawable.stop()
            }

            frame_container_gif_item.setBackgroundColor(if(mraGIPHYID.get(model)?:false) mAccentColor else Color.TRANSPARENT)

            giphy_item_gif.visibility = View.INVISIBLE
            progress_bar.visibility = View.INVISIBLE
            giphy_item_net_img.setImageUrl(model.fixed_width_small_still, mImgLoader)
            container_gif_item.setOnClickListener {
                if (toggle_selection) {
                    mraGIPHYID[model] = !(mraGIPHYID[model]?:false)
                    frame_container_gif_item.setBackgroundColor(if(mraGIPHYID[model]?:false) mAccentColor else Color.TRANSPARENT)
                }
                val container = mGifLoader!![model.fixed_width?:"", object : GifLoader.GifListener {
                    override fun onResponse(response: GifLoader.GifContainer, isImmediate: Boolean) {
                        giphy_item_gif.setImageDrawable(response.gif)
                        val itemDrawable = giphy_item_gif.drawable as GifDrawable?
                        if (itemDrawable == null) {
                            giphy_item_gif.visibility = View.INVISIBLE
                        } else {
                            giphy_item_gif.visibility = View.VISIBLE
                            if (!itemDrawable.isPlaying) itemDrawable.start()
                        }
                        progress_bar.visibility = View.INVISIBLE
                    }

                    override fun onErrorResponse(error: VolleyError) {}
                }]

                giphy_item_gif.setImageDrawable(container.gif)
                val itemDrawable = giphy_item_gif.drawable as GifDrawable?
                if (itemDrawable == null) {
                    giphy_item_gif.visibility = View.INVISIBLE
                    progress_bar.visibility = View.VISIBLE
                } else {
                    giphy_item_gif.visibility = View.VISIBLE
                    if (!itemDrawable.isPlaying) itemDrawable.start()
                }
                listener?.onItemClick(model)
            }
            container_gif_item.setOnLongClickListener {
                if (longListener != null) {
                    longListener.onItemLongClick(model)
                    container_gif_item.callOnClick()
                    true
                } else {
                    false
                }
            }

        }
    }
}
