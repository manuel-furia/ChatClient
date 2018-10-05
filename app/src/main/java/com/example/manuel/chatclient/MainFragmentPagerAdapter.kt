package com.example.manuel.chatclient

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class MainFragmentPagerAdapter(fragments: List<Fragment>, fm: FragmentManager) : FragmentPagerAdapter(fm){

    val fragments = fragments

    override fun getCount(): Int = fragments.size

    override fun getItem(position: Int): Fragment = fragments[position]


}