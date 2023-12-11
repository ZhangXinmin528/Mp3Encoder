package com.zxm.core.base

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity


/**
 * Created by ZhangXinmin on 2020/7/19.
 * Copyright (c) 2020 . All rights reserved.
 * Base activity for common usage.
 */
abstract class BaseActivity : AppCompatActivity() {
    protected val sTAG = this.javaClass.simpleName
    protected val TAG = this.javaClass.simpleName

    protected var mContext: Context? = null

    abstract fun setLayoutView(): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(setLayoutView())

        mContext = this

        initParamsAndValues()

        initViews()

    }

    protected open fun initParamsAndValues() {

    }

    protected open fun initViews() {

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    protected fun jumpActivity(clazz: Class<*>) {
        val intent = Intent(mContext, clazz)
        startActivity(intent)
    }

    protected fun jumpActivity(bundle: Bundle, clazz: Class<*>) {
        val intent = Intent(mContext, clazz)
        intent.putExtras(bundle)
        startActivity(intent)
    }


}