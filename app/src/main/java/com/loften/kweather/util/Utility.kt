package com.loften.kweather.util

import android.text.TextUtils
import com.google.gson.Gson
import com.loften.kweather.db.City
import com.loften.kweather.db.County
import com.loften.kweather.db.Province
import com.loften.kweather.gson.Weather
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by lcw on 2017/9/22.
 */
object Utility {

    /**
     * 解析和处理服务器返回的省级数据
     */
    fun handleProvinceResponse(response: String): Boolean {
        if (!TextUtils.isEmpty(response)) {
            try {
                val allProvinces=JSONArray(response)
                for (i in 0 until  allProvinces.length()) {
                    val provinceObject = allProvinces.getJSONObject(i)
                    val province = Province(provinceName = provinceObject.getString("name"),
                            provinceCode = provinceObject.getInt("id"))
                    province.save()
                }
                return true
            }catch (e: JSONException){
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    fun handleCityResponse(response: String, provinceId: Int): Boolean{
        if(!TextUtils.isEmpty(response)){
            try {
                val allCities = JSONArray(response)
                for (i in 0 until  allCities.length()){
                    val cityObject = allCities.getJSONObject(i)
                    val city = City(cityName = cityObject.getString("name"),
                            cityCode = cityObject.getInt("id"), provinceId = provinceId)
                    city.save()
                }
                return true
            }catch (e: JSONException){
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    fun handleCountyResponse(response: String, cityId: Int): Boolean{
        if(!TextUtils.isEmpty(response)){
            try {
                val allCounties = JSONArray(response)
                for (i in 0 until allCounties.length()){
                    val countyObject = allCounties.getJSONObject(i)
                    val county = County(countyName = countyObject.getString("name"),
                            weatherId = countyObject.getString("weather_id"),
                            cityId = cityId)
                    county.save()
                }
                return true
            }catch (e: JSONException){
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * 将返回的JSON数据解析成Weather实体类
     */
    fun handleWeatherResponse(response: String): Weather? {
        try {
            val jsonObject = JSONObject(response)
            val jsonArray = jsonObject.getJSONArray("HeWeather")
            val weatherContent = jsonArray.getJSONObject(0).toString()
            return Gson().fromJson(weatherContent, Weather::class.java)
        }catch (e: Exception){
            e.printStackTrace()
        }
        return null
    }
}