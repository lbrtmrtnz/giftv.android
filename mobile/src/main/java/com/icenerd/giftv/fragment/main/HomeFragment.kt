package com.icenerd.giftv.fragment.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import com.icenerd.giftv.R

class HomeFragment : Fragment() {
    private var mFragmentView: View? = null
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.title = "Home"
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (savedInstanceState == null) {
            mFragmentView = inflater.inflate(R.layout.fragment_home, container, false)
            setHasOptionsMenu(true)
        }
        return mFragmentView
    }
    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.home, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
}