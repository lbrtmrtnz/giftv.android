package com.icenerd.giftv.adapter

import android.os.Build
import android.text.Html
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.icenerd.giftv.R
import com.icenerd.giftv.data.model.TCPServiceModel

class TCPServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val item_status_title by lazy { itemView.findViewById<TextView>(R.id.item_status_title) }

    fun bind(model: TCPServiceModel, listener: TCPServiceAdapter.ActionListener) {
        item_status_title.text = if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(model.name, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(model.name)
        }
        itemView.setOnClickListener { listener.onItemClick(model) }
    }

}