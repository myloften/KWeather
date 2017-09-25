package com.loften.kweather

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.loften.kweather.gson.Weather
import com.loften.kweather.service.AutoUpdateService
import com.loften.kweather.util.HttpUtil
import com.loften.kweather.util.Utility
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.aqi.*
import kotlinx.android.synthetic.main.forecast.*
import kotlinx.android.synthetic.main.now.*
import kotlinx.android.synthetic.main.suggestion.*
import kotlinx.android.synthetic.main.title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class WeatherActivity : AppCompatActivity() {

    private var mWeatherId: String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Build.VERSION.SDK_INT >= 21){
            val decorView = window.decorView
            decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor=Color.TRANSPARENT
        }
        setContentView(R.layout.activity_weather)
        init()
    }

    private fun init(){
        val prefs=PreferenceManager.getDefaultSharedPreferences(this)
        val weatherString=prefs.getString("weather", null)
        if(weatherString != null){
            //有缓存时直接解析天气数据
            val weather=Utility.handleWeatherResponse(weatherString)
            mWeatherId=weather!!.basic.id
            showWeatherInfo(weather)
        }else{
            //无缓存时去服务器查询天气
            mWeatherId=intent.getStringExtra("weather_id")
            weather_layout.visibility=View.INVISIBLE
            requestWeather(mWeatherId)
        }
        swipe_refresh.setOnRefreshListener { requestWeather(mWeatherId) }
        nav_button.setOnClickListener{drawer_layout.openDrawer(GravityCompat.START)}
        val bingPic=prefs.getString("bing_pic", null)
        if(bingPic != null){
            Glide.with(this).load(bingPic).into(bing_pic_img)
        }else{
            loadBingPic()
        }
    }

    /**
     * 根据天气id请求城市天气信息。
     */
    fun requestWeather(weatherId: String?) {
        val weatherUrl="http://guolin.tech/api/weather?cityid=$weatherId&key=bc0418b57b2d4918819d3974ac1285d9"
        HttpUtil.sendOkHttpRequest(weatherUrl, object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call?, response: Response) {
                val responseText=response.body()!!.string()
                val weather=Utility.handleWeatherResponse(responseText)
                runOnUiThread {
                    if (weather != null && "ok" == weather.status) {
                        val editor=PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity).edit()
                        editor.putString("weather", responseText)
                        editor.apply()
                        mWeatherId=weather.basic.id
                        showWeatherInfo(weather)
                    } else {
                        Toast.makeText(this@WeatherActivity, "获取天气信息失败", Toast.LENGTH_SHORT).show()
                    }
                    swipe_refresh.isRefreshing=false
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@WeatherActivity, "获取天气信息失败", Toast.LENGTH_SHORT).show()
                    swipe_refresh.isRefreshing=false
                }
            }
        })
        loadBingPic()
    }

    /**
     * 加载必应每日一图
     */
    private fun loadBingPic() {
        val requestBingPic="http://guolin.tech/api/bing_pic"
        HttpUtil.sendOkHttpRequest(requestBingPic, object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call?, response: Response) {
                val bingPic=response.body()!!.string()
                val editor=PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity).edit()
                editor.putString("bing_pic", bingPic)
                editor.apply()
                runOnUiThread { Glide.with(this@WeatherActivity).load(bingPic).into(bing_pic_img) }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }

    /**
     * 处理并展示Weather实体类中的数据。
     */
    private fun showWeatherInfo(weather: Weather?) {
        val cityName=weather!!.basic.city
        val updateTime=weather.basic.update.loc.split(" ")[1]
        val degree=weather.now.tmp + "℃"
        val weatherInfo=weather.now.cond.txt
        title_city.text=cityName
        title_update_time.text=updateTime
        degree_text.text=degree
        weather_info_text.text=weatherInfo
        forecast_layout.removeAllViews()
        for (forecast in weather.daily_forecast){
            val view=LayoutInflater.from(this).inflate(R.layout.forecast_item, forecast_layout, false)
            val dateText=view.findViewById(R.id.date_text) as TextView
            val infoText=view.findViewById(R.id.info_text) as TextView
            val maxText=view.findViewById(R.id.max_text) as TextView
            val minText=view.findViewById(R.id.min_text) as TextView
            dateText.text=forecast.date
            infoText.text=forecast.cond.txt
            maxText.text=forecast.tmp.max
            minText.text=forecast.tmp.min
            forecast_layout.addView(view)
        }
        if(weather.aqi != null){
            aqi_text.text=weather.aqi.city.aqi
            pm25_text.text=weather.aqi.city.pm25
        }
        val comfort="舒适度："+weather.suggestion.comf.txt
        val carWash="洗车指数："+weather.suggestion.cw.txt
        val sport="运行建议："+weather.suggestion.sport.txt
        comfort_text.text=comfort
        car_wash_text.text=carWash
        sport_text.text=sport
        weather_layout.visibility=View.VISIBLE
        val intent=Intent(this, AutoUpdateService::class.java)
        startService(intent)
    }
}
