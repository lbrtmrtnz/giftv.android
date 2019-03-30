package com.icenerd.giftv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.res.ResourcesCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.icenerd.adapter.RecyclerPageListener
import com.icenerd.giftv.data.GIFTVDB
import com.icenerd.giftv.data.model.TCPServiceModel
import com.icenerd.giftv.fragment.dialog.AboutDialog
import com.icenerd.giftv.fragment.dialog.MobileTVNameDialog
import com.icenerd.giftv.fragment.dialog.SendChannelDialog
import com.icenerd.giftv.net.GIPHYService
import com.icenerd.giftv.net.Server
import com.icenerd.giphy.data.adapter.GifAdapter
import com.icenerd.giphy.data.model.GifModel
import com.icenerd.giphy.data.orm.GifORM
import org.json.JSONArray
import org.json.JSONException

class MobileActivity : AppCompatActivity(), ActionMode.Callback {
    companion object {
        private const val TAG = "MobileActivity"
    }

    private var currentSearchTerms: String? = null

    private val inputSearch by lazy { findViewById<EditText>(R.id.search_input) }
    private val actionSearch by lazy { findViewById<View>(R.id.action_search) }
    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.recycler_view) }

    private var loadMoreListener: RecyclerPageListener? = null
    private val gifAdapter by lazy {
        GifAdapter().apply {

        }
    }

    private var abouDialog: AboutDialog? = null
    private var mobileTVNameDialog: MobileTVNameDialog? = null
    private var actionModeCurrent: ActionMode? = null

    private val localBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                GIPHYService.UPDATE_TRENDING -> {
                    val cursor = GifORM(GIFTVDB(context).readableDatabase).getCursorAll(null)
                    gifAdapter.swapCursor(cursor)
                }
                GIPHYService.UPDATE_SEARCH -> {
                    val cursor = GifORM(GIFTVDB(context).readableDatabase).getSearch(intent.getStringExtra("terms"))
                    gifAdapter.swapCursor(cursor)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val intentFilter = IntentFilter()
        intentFilter.addAction(GIPHYService.UPDATE_SEARCH)
        intentFilter.addAction(GIPHYService.UPDATE_TRENDING)
        LocalBroadcastManager.getInstance(this).registerReceiver(localBroadcastReceiver, intentFilter)

        val trendingIntent = Intent(GIPHYService.ACTION_GET_TRENDING, null, this, GIPHYService::class.java)
        GIPHYService.enqueueWork(this, trendingIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localBroadcastReceiver)
    }

    override fun onStart() {
        super.onStart()

        val manager = GridLayoutManager(recyclerView?.context, 2)
        recyclerView.layoutManager = manager
        recyclerView.itemAnimator = DefaultItemAnimator()
        loadMoreListener = object : RecyclerPageListener(manager) {
            override fun onLoadMore(current_page: Int) {
                if(BuildConfig.DEBUG) Log.d("onLoadMore", "${current_page}")
                if (currentSearchTerms.isNullOrEmpty()) {
                    val intent = Intent(GIPHYService.ACTION_GET_TRENDING, null, this@MobileActivity, GIPHYService::class.java)
                    GIPHYService.enqueueWork(this@MobileActivity, intent)
                } else {
                    val intent = Intent(GIPHYService.ACTION_GET_SEARCH, null, this@MobileActivity, GIPHYService::class.java)
                    intent.putExtra(GIPHYService.EXTRA_TERMS, currentSearchTerms)
                    intent.putExtra(GIPHYService.EXTRA_OFFSET, current_page * GIPHYService.PAGESIZE_SEARCH)
                    GIPHYService.enqueueWork(this@MobileActivity, intent)
                }
            }
        }.also { recyclerView?.addOnScrollListener(it) }

        recyclerView?.adapter = gifAdapter
        gifAdapter.actionListener = object: GifAdapter.ActionListener {
            override fun onItemClick(model: GifModel) {
                if (BuildConfig.DEBUG) Log.d(TAG, "item clicked")
                val count = gifAdapter.selectedCount
                actionModeCurrent?.title = "$count GIF selected"
            }
            override fun onItemLongClick(model: GifModel) {
                if (BuildConfig.DEBUG) Log.d(TAG, "item long clicked")
                if (actionModeCurrent == null) {
                    startSupportActionMode(this@MobileActivity)
                    gifAdapter.toggleSelection = true
                }
            }
        }

        actionSearch.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(inputSearch?.windowToken, 0)
            if (actionModeCurrent == null && !inputSearch?.text.isNullOrEmpty()) {
                ACTION_search(inputSearch?.text.toString())
            }
        }
        inputSearch?.imeOptions = EditorInfo.IME_ACTION_SEARCH
        inputSearch?.setOnEditorActionListener { textView, action_id, _ ->
            when (action_id) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(textView.windowToken, 0)
                    if (actionModeCurrent == null && !inputSearch?.text.isNullOrEmpty()) {
                        ACTION_search(inputSearch?.text.toString())
                    }
                }
            }
            false
        }

        val sharedPrefs = getSharedPreferences("giftv", Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("tutorial_about")) {
            val editor = sharedPrefs.edit()
            editor.putBoolean("tutorial_about", true)
            editor.apply()
            val fragMan = supportFragmentManager
            val ft = fragMan.beginTransaction()

            abouDialog = AboutDialog()
            abouDialog?.show(ft, "about_dialog")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_local_activity -> {
                startMobileTV()
                true
            }

            R.id.action_about -> {
                val ft = supportFragmentManager.beginTransaction()
                abouDialog = AboutDialog()
                abouDialog?.show(ft, "about_dialog")
                true
            }
            else -> false
        }
    }

    private fun startMobileTV() {
        mobileTVNameDialog = MobileTVNameDialog()
        mobileTVNameDialog?.actionListener = object : MobileTVNameDialog.ActionListener {
            override fun onStartMobileTV(name: String) {
                startMobileTV(name)
            }
        }

        val ft = supportFragmentManager.beginTransaction()
        mobileTVNameDialog?.show(ft, "mobile_tv_name_dialog")
    }

    private fun startMobileTV(name: String?) {
        if (name != null && !name.isEmpty()) {
            val sharedPref = getSharedPreferences("giftv", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString(MobileTVActivity.EXTRA_NAME, name)
            editor.apply()
            val intent = Intent(this, MobileTVActivity::class.java)
            intent.putExtra(Server.NAME, name)
            intent.putExtra(Server.TYPE, getString(R.string.network_service_type))
            startActivity(intent)
        }
    }

    private fun ACTION_search(query: String) {
        currentSearchTerms = query
        val searchIntent = Intent(GIPHYService.ACTION_GET_SEARCH, null, this, GIPHYService::class.java)
        searchIntent.putExtra("terms", query)
        GIPHYService.enqueueWork(this, searchIntent)
        loadMoreListener?.onRefresh()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.actionmode_multi_select, menu)
        actionModeCurrent = mode
        inputSearch?.isEnabled = false
        inputSearch?.setTextColor(ResourcesCompat.getColor(resources, R.color.black_overlay, theme))
        actionSearch.setBackgroundResource(R.color.colorPrimaryDark)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == R.id.action_send) {
            if (BuildConfig.DEBUG) Log.d(TAG, "action_send")

            val selectedGif = gifAdapter.selectedGIF
            if (selectedGif.isNotEmpty()) {
                val fragMan = supportFragmentManager
                val dialog = SendChannelDialog()

                dialog.actionListener = object : SendChannelDialog.ActionListener {
                    override fun onTVSelected(model: TCPServiceModel) {
                        mode.finish()
                    }
                }

                var jsonBatch: String? = null
                try {
                    val json = JSONArray()
                    for (i in selectedGif.indices) {
                        json.put(selectedGif[i].getJSONObject())
                    }
                    jsonBatch = json.toString()
                } catch (err: JSONException) {
                    err.printStackTrace()
                }

                val args = Bundle()
                args.putString(GifORM.TABLE, jsonBatch)
                args.putString("terms", currentSearchTerms)
                dialog.arguments = args
                dialog.show(fragMan, "send_gif_batch")
            }
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionModeCurrent = null
        gifAdapter.clearSelections()
        inputSearch?.isEnabled = true
        inputSearch?.setTextColor(ResourcesCompat.getColor(resources,R.color.colorPrimary,theme))
        actionSearch.setBackgroundResource(R.color.colorAccent)
    }
}
