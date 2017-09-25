package com.loften.kweather.util

import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Created by lcw on 2017/9/22.
 */
object HttpUtil {

    fun sendOkHttpRequest(address: String, callback: Callback){
        val client = OkHttpClient()
        val request = Request.Builder().url(address).build()
        client.newCall(request).enqueue(callback)
    }

}