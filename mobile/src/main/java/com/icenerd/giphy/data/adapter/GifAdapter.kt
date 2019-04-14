package com.icenerd.giphy.data.adapter


import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.collection.LruCache
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
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
    interface ActionListener {
        fun onItemClick(model: GifModel)
        fun onItemLongClick(model: GifModel)
    }

    var actionListener: ActionListener? = null
    var toggleSelection = false

    private val mraGIPHYID = HashMap<GifModel, Boolean>()
    private var requestQueue: RequestQueue? = null
    private var imageLoader: ImageLoader? = null
    private var gifLoader: GifLoader? = null

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

    fun clearSelections() {
        mraGIPHYID.clear()
        toggleSelection = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.giphy_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        requestQueue = Volley.newRequestQueue(recyclerView.context)
        imageLoader = ImageLoader(requestQueue, object : ImageLoader.ImageCache {
            private val mCache = LruCache<String, Bitmap>(50)
            override fun putBitmap(url: String, bmp: Bitmap) {
                mCache.put(url, bmp)
            }
            override fun getBitmap(url: String): Bitmap? {
                return mCache.get(url)
            }
        })
        gifLoader = GifLoader(requestQueue, object : GifLoader.GifCache {
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
        requestQueue?.stop()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position).let {
            holder.bind(if (it == null) null else GifModel(it), actionListener)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val frameContainerGifItem: View = view.findViewById(R.id.frame_container_gif_item)
        val containerGifItem: View = view.findViewById(R.id.container_gif_item)
        val giphyItemNetImg: NetworkImageView = view.findViewById(R.id.giphy_item_net_img)
        val giphyItemGif: GifImageView = view.findViewById(R.id.giphy_item_gif)
        val progressBar: ProgressBar = view.findViewById(R.id.progress_bar)

        fun bind(gifModel: GifModel?, listener: ActionListener?) {
            val drawable = giphyItemGif.drawable as GifDrawable?
            if (drawable != null && drawable.isPlaying) drawable.stop()

            val ctx = frameContainerGifItem.context
            val accentColor = if (Build.VERSION.SDK_INT >= 23)
                ctx.resources.getColor(R.color.colorAccent, ctx.theme)
            else
                ResourcesCompat.getColor(ctx.resources, R.color.colorAccent, ctx.theme)

            if (gifModel == null) Log.e("GifAdapter", "gifModel IS NULL!")
            frameContainerGifItem.visibility = if (gifModel == null) View.INVISIBLE else View.VISIBLE
            gifModel?.let { model ->
                frameContainerGifItem.setBackgroundColor(if (mraGIPHYID.containsKey(model)) accentColor else Color.TRANSPARENT)

                giphyItemGif.visibility = View.INVISIBLE
                progressBar.visibility = View.INVISIBLE
                giphyItemNetImg.setImageUrl(model.urlStill, imageLoader)
                containerGifItem.setOnClickListener {
                    if (toggleSelection) {
                        mraGIPHYID[model] = !(mraGIPHYID[model] ?: false)
                        frameContainerGifItem.setBackgroundColor(if (mraGIPHYID.containsKey(model)) accentColor else Color.TRANSPARENT)
                    }
                    val container = gifLoader!![model.fixed_width ?: "", object : GifLoader.GifListener {
                        override fun onResponse(response: GifLoader.GifContainer, isImmediate: Boolean) {
                            giphyItemGif.setImageDrawable(response.gif)
                            val itemDrawable = giphyItemGif.drawable as GifDrawable?
                            if (itemDrawable == null) {
                                giphyItemGif.visibility = View.INVISIBLE
                            } else {
                                giphyItemGif.visibility = View.VISIBLE
                                if (!itemDrawable.isPlaying) itemDrawable.start()
                            }
                            progressBar.visibility = View.INVISIBLE
                        }

                        override fun onErrorResponse(error: VolleyError) {}
                    }]

                    giphyItemGif.setImageDrawable(container.gif)
                    val itemDrawable = giphyItemGif.drawable as GifDrawable?
                    if (itemDrawable == null) {
                        giphyItemGif.visibility = View.INVISIBLE
                        progressBar.visibility = View.VISIBLE
                    } else {
                        giphyItemGif.visibility = View.VISIBLE
                        if (!itemDrawable.isPlaying) itemDrawable.start()
                    }
                    listener?.onItemClick(model)
                }
                containerGifItem.setOnLongClickListener {
                    if (listener != null) {
                        listener.onItemLongClick(model)
                        containerGifItem.callOnClick()
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }
}
