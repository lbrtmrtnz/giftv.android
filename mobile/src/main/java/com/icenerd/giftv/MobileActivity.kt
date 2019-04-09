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

    private var xSearchTerms: String? = null // main user event
        set(searchTerms) {
            field = searchTerms
            val searchIntent = Intent(GIPHYService.ACTION_GET_SEARCH, null, this, GIPHYService::class.java)
            searchIntent.putExtra("terms", searchTerms)
            GIPHYService.enqueueWork(this, searchIntent)
            mLoadMoreListener.onRefresh()
        }

    private val inputSearch by lazy { findViewById<EditText>(R.id.input_search) }
    private val actionSearch by lazy { findViewById<View>(R.id.action_search) }
    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.recycler_view) }

    private var mAboutDialog: AboutDialog? = null
    private var mMobileTVNameDialog: MobileTVNameDialog? = null
        set(dialog) {
            dialog?.actionListener = mMobileTVNameDialogActions
            field = dialog
        }
    private var mActionMode: ActionMode? = null

    private val mMobileTVNameDialogActions by lazy {
        object : MobileTVNameDialog.ActionListener {
            override fun onStartMobileTV(name: String) {
                startMobileTV(name)
            }
        }}

    private val mGridLayoutManager by lazy { GridLayoutManager(this, 2) }
    private val mLoadMoreListener by lazy {
        object : RecyclerPageListener(mGridLayoutManager) {
            override fun onLoadMore(current_page: Int) {
                if(BuildConfig.DEBUG) Log.d("onLoadMore", "${current_page}")
                if (xSearchTerms.isNullOrEmpty()) {
                    val intent = Intent(GIPHYService.ACTION_GET_TRENDING, null, this@MobileActivity, GIPHYService::class.java)
                    GIPHYService.enqueueWork(this@MobileActivity, intent)
                } else {
                    val intent = Intent(GIPHYService.ACTION_GET_SEARCH, null, this@MobileActivity, GIPHYService::class.java)
                    intent.putExtra(GIPHYService.EXTRA_TERMS, xSearchTerms)
                    intent.putExtra(GIPHYService.EXTRA_OFFSET, current_page * GIPHYService.PAGESIZE_SEARCH)
                    GIPHYService.enqueueWork(this@MobileActivity, intent)
                }
            }
        }
    }
    private val mGifAdapter by lazy { GifAdapter().also { adapter ->
        adapter.actionListener = object: GifAdapter.ActionListener {
            override fun onItemClick(model: GifModel) {
                if (BuildConfig.DEBUG) Log.d(TAG, "item clicked")
                val count = adapter.selectedCount
                mActionMode?.title = "$count GIF selected"
            }
            override fun onItemLongClick(model: GifModel) {
                if (BuildConfig.DEBUG) Log.d(TAG, "item long clicked")
                if (mActionMode == null) {
                    startSupportActionMode(this@MobileActivity)
                    adapter.toggleSelection = true
                }
            }
        }
    } }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                GIPHYService.UPDATE_TRENDING -> {
                    val cursor = GifORM(GIFTVDB(context).readableDatabase)
                        .getCursorAll(null)
                    mGifAdapter.swapCursor(cursor)
                }
                GIPHYService.UPDATE_SEARCH -> {
                    val cursor = GifORM(GIFTVDB(context).readableDatabase)
                        .getSearch(intent.getStringExtra("terms"))
                    mGifAdapter.swapCursor(cursor)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onStart() {
        super.onStart()

        val intentFilter = IntentFilter()
        intentFilter.addAction(GIPHYService.UPDATE_SEARCH)
        intentFilter.addAction(GIPHYService.UPDATE_TRENDING)
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter)

        val trendingIntent = Intent(GIPHYService.ACTION_GET_TRENDING, null, this, GIPHYService::class.java)
        GIPHYService.enqueueWork(this, trendingIntent)

        recyclerView.apply {
            layoutManager = mGridLayoutManager
            itemAnimator = DefaultItemAnimator()
            adapter = mGifAdapter
            addOnScrollListener(mLoadMoreListener)
        }

        actionSearch.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(inputSearch.windowToken, 0)
            if (mActionMode == null && !inputSearch.text.isNullOrEmpty()) {
                xSearchTerms = inputSearch.text.toString()
            }
        }
        inputSearch.imeOptions = EditorInfo.IME_ACTION_SEARCH
        inputSearch.setOnEditorActionListener { editTextView, action_id, _ ->
            when (action_id) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(editTextView.windowToken, 0)
                    if (mActionMode == null && !inputSearch?.text.isNullOrEmpty()) {
                        xSearchTerms = inputSearch?.text.toString()
                    }
                }
            }
            false
        }

        val sharedPrefs = getSharedPreferences("giftv", Context.MODE_PRIVATE)
        if (!sharedPrefs.contains("tutorial_about")) {
            sharedPrefs.edit().apply {
                putBoolean("tutorial_about", true)
                apply()
            }
            val ft = supportFragmentManager.beginTransaction()
            mAboutDialog = AboutDialog()
            mAboutDialog?.show(ft, "about_dialog")
        }
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
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
                mAboutDialog = AboutDialog()
                mAboutDialog?.show(ft, "about_dialog")
                true
            }

            else -> false
        }
    }

    private fun startMobileTV() {
        mMobileTVNameDialog = MobileTVNameDialog()
        val ft = supportFragmentManager.beginTransaction()
        mMobileTVNameDialog?.show(ft, "mobile_tv_name_dialog")
    }

    private fun startMobileTV(name: String?) {
        if (!name.isNullOrEmpty()) {
            val sharedPref = getSharedPreferences("giftv", Context.MODE_PRIVATE)
            sharedPref.edit().apply {
                putString(MobileTVActivity.EXTRA_NAME, name)
                apply()
            }
            val intent = Intent(this, MobileTVActivity::class.java)
            intent.putExtra(Server.NAME, name)
            intent.putExtra(Server.TYPE, getString(R.string.network_service_type))
            startActivity(intent)
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.actionmode_multi_select, menu)
        mActionMode = mode
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

            val selectedGif = mGifAdapter.selectedGIF
            if (selectedGif.isNotEmpty()) {
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

                val dialog = SendChannelDialog().apply {
                    actionListener = object : SendChannelDialog.ActionListener {
                        override fun onTVSelected(model: TCPServiceModel) {
                            mode.finish()
                        }
                    }
                    arguments = Bundle().apply {
                        putString(GifORM.TABLE, jsonBatch)
                        putString("terms", xSearchTerms)
                    }
                }
                dialog.show(supportFragmentManager, "send_gif_batch")
            }
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        mActionMode = null
        mGifAdapter.clearSelections()
        inputSearch?.isEnabled = true
        inputSearch?.setTextColor(ResourcesCompat.getColor(resources,R.color.colorPrimary,theme))
        actionSearch.setBackgroundResource(R.color.colorAccent)
    }
}
