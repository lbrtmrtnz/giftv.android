package com.icenerd.giftv

import android.app.LoaderManager
import android.content.Context
import android.content.Intent
import android.content.Loader
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.icenerd.adapter.RecyclerPageListener
import com.icenerd.giftv.data.loader.GIPHYSearchLoader
import com.icenerd.giftv.data.loader.GIPHYTrendingLoader
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
import java.util.*

class MobileActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor>, ActionMode.Callback {
    companion object {
        private val TAG = "MobileActivity"
        private val LOADER_ID_TRENDING = Random().nextInt()
        private val LOADER_ID_SEARCH = Random().nextInt()
    }

    private var mToolbar: Toolbar? = null
    private var mInputSearch: EditText? = null
    private var mCurrentQuery: String? = null
    private var mRecyclerView: RecyclerView? = null
    private var mRecyclerLoadMoreListener: RecyclerPageListener? = null
    private var mAdapter: GifAdapter? = null

    private var mAboutDialog: AboutDialog? = null
    private var mMobileTVNameDialog: MobileTVNameDialog? = null
    private var mCurrentMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mToolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(mToolbar)
        supportActionBar!!.setTitle("")
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setIcon(R.drawable.ic_launcher_with_text)

        mInputSearch = findViewById(R.id.search_input) as EditText
        mRecyclerView = findViewById(R.id.recycler_view) as RecyclerView
        val manager = GridLayoutManager(mRecyclerView!!.context, 2)
        mRecyclerView!!.layoutManager = manager
        mRecyclerView!!.itemAnimator = DefaultItemAnimator()
        mRecyclerLoadMoreListener = object : RecyclerPageListener(manager) {
            override fun onLoadMore(current_page: Int) {
                if(BuildConfig.DEBUG) Log.d("onLoadMore", "${current_page}")
                if (mCurrentQuery == null) {
                    val intent = Intent(GIPHYService.ACTION_GET_TRENDING, null, this@MobileActivity, GIPHYService::class.java)
                    if (Build.VERSION.SDK_INT >= 21) {
                        GIPHYService.enqueueWork(this@MobileActivity, intent)
                    } else {
                        startService(intent)
                    }
                } else {
                    val intent = Intent(GIPHYService.ACTION_GET_SEARCH, null, this@MobileActivity, GIPHYService::class.java)
                    intent.putExtra("terms", mCurrentQuery)
                    intent.putExtra("offset", current_page * GIPHYService.PAGESIZE_SEARCH)
                    if (Build.VERSION.SDK_INT >= 21) {
                        GIPHYService.enqueueWork(this@MobileActivity, intent)
                    } else {
                        startService(intent)
                    }
                }
            }
        }
        mRecyclerView!!.addOnScrollListener(mRecyclerLoadMoreListener)

        mAdapter = GifAdapter()
        mAdapter?.mListener = object: GifAdapter.OnItemClickListener {
            override fun onItemClick(model: GifModel) {
                if (BuildConfig.DEBUG) Log.d(TAG, "item clicked")
                if (mCurrentMode == null) {
                    // nothing to do
                } else {
                    val count = mAdapter?.selectedCount?:0
                    mCurrentMode!!.title = "${count} GIF selected"
                }
            }
        }
        mAdapter?.mLongListener = object: GifAdapter.OnItemLongClickListener {
            override fun onItemLongClick(model: GifModel) {
                if (BuildConfig.DEBUG) Log.d(TAG, "item long clicked")
                if (mCurrentMode == null) {
                    startSupportActionMode(this@MobileActivity)
                    mAdapter!!.toggle_selection = true
                } else {
                    // nothing to do
                }
            }
        }
        mRecyclerView!!.adapter = mAdapter
        findViewById<View>(R.id.action_search).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(mInputSearch!!.windowToken, 0)
            if (mCurrentMode == null && !mInputSearch!!.text.toString().isEmpty()) {
                ACTION_search(mInputSearch!!.text.toString())
            }
        }
        mInputSearch!!.imeOptions = EditorInfo.IME_ACTION_SEARCH
        mInputSearch!!.setOnEditorActionListener { textView, action_id, _ ->
            if (action_id == EditorInfo.IME_ACTION_SEARCH) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(textView.windowToken, 0)
                if (mCurrentMode == null && !mInputSearch!!.text.toString().isEmpty()) {
                    ACTION_search(mInputSearch!!.text.toString())
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

            mAboutDialog = AboutDialog()
            mAboutDialog!!.show(ft, "about_dialog")
        }
        loaderManager.initLoader(LOADER_ID_TRENDING, Bundle(), this).forceLoad()
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
                run {
                    val fragMan = supportFragmentManager
                    val ft = fragMan.beginTransaction()

                    mAboutDialog = AboutDialog()
                    mAboutDialog!!.show(ft, "about_dialog")
                }
                false
            }
            else -> false
        }
    }

    private fun startMobileTV() {
        val fragMan = supportFragmentManager
        val ft = fragMan.beginTransaction()

        mMobileTVNameDialog = MobileTVNameDialog()
        mMobileTVNameDialog!!.setOnStartMobileTVListener(object : MobileTVNameDialog.OnStartMobileTVListener {
            override fun onStartMobileTV(name: String) {
                startMobileTV(name)
            }
        })
        mMobileTVNameDialog!!.show(ft, "mobile_tv_name_dialog")
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
        mCurrentQuery = query
        loaderManager.restartLoader(LOADER_ID_SEARCH, Bundle(), this).forceLoad()
        mRecyclerLoadMoreListener!!.onRefresh()
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>? {
        if (mCurrentQuery == null) {
            if (id == LOADER_ID_TRENDING) return GIPHYTrendingLoader(this)
        } else {
            if (id == LOADER_ID_SEARCH) return GIPHYSearchLoader(this, mCurrentQuery!!)
        }
        return null
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        mAdapter!!.swapCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mAdapter!!.changeCursor(null)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.actionmode_multi_select, menu)
        mCurrentMode = mode
        mInputSearch!!.isEnabled = false
        if (Build.VERSION.SDK_INT >= 23) {
            mInputSearch!!.setTextColor(getColor(R.color.black_overlay))
        } else {
            mInputSearch!!.setTextColor(ResourcesCompat.getColor(resources, R.color.black_overlay, theme))
        }
        findViewById<View>(R.id.action_search).setBackgroundResource(R.color.colorPrimaryDark)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == R.id.action_send) {
            if (BuildConfig.DEBUG) Log.d(TAG, "action_send")

            val selectedGif = mAdapter?.selectedGIF
            if (selectedGif != null && selectedGif.size > 0) {
                val fragMan = supportFragmentManager
                val dialog = SendChannelDialog()

                dialog.setOnTVSelectedListener(object : SendChannelDialog.OnTVSelectedListener {
                    override fun onTVSelected(model: TCPServiceModel) {
                        mode.finish()
                    }
                })

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
                args.putString("terms", mCurrentQuery)
                dialog.arguments = args
                dialog.show(fragMan, "send_gif_batch")
            }
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        mCurrentMode = null
        mAdapter!!.clearSelections()
        mInputSearch!!.isEnabled = true
        if (Build.VERSION.SDK_INT >= 23) {
            mInputSearch!!.setTextColor(getColor(R.color.colorPrimary))
        } else {
            mInputSearch!!.setTextColor(ResourcesCompat.getColor(resources,R.color.colorPrimary,theme))
        }
        findViewById<View>(R.id.action_search).setBackgroundResource(R.color.colorAccent)
    }
}
