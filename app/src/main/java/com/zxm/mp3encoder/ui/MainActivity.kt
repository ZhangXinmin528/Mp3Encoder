package com.zxm.mp3encoder.ui

import android.view.View
import com.zxm.core.base.BaseActivity
import com.zxm.mp3encoder.databinding.ActivityMainBinding
import com.zxm.mp3encoder.encoder.Mp3Encoder


class MainActivity : BaseActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun setLayoutView(): View {
        return binding.root
    }

    override fun initParamsAndValues() {
        val mp3Encoder = Mp3Encoder()
        mp3Encoder.encode()
    }

    companion object{

        init {
            System.loadLibrary("audioencoder")
        }
    }
}
