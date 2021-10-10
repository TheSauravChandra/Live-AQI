package com.saurav.sauravcaqi.utils

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.saurav.sauravcaqi.R
import com.saurav.sauravcaqi.bean.AQIItem
import com.saurav.sauravcaqi.bean.HistoryItem
import com.saurav.sauravcaqi.bean.RvCityUpdateItem
import com.saurav.sauravcaqi.utils.Constants.MAX_TIME_SERIES
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MyUtils {
  companion object {
    
    @JvmStatic
    fun Context.toast(msg: String = "") {
      Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
    
    @JvmStatic
    fun Context.availInternet(): Boolean {
      val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val activeNetwork = cm.activeNetworkInfo
      return activeNetwork != null && activeNetwork.isConnected
    }
    
    @JvmStatic
    fun lastUpdated(tNowSec: Long, tLastSec: Long?): String { // in seconds,
      if (tLastSec == null)
        return " --- "
      
      if ((tNowSec - tLastSec) < 60)
        return "A few seconds ago"
      
      if ((tNowSec - tLastSec) > 60 && (tNowSec - tLastSec) < (60 * 2))
        return "A minute ago"
      
      val sdf = SimpleDateFormat("hh:mm a")
      sdf.timeZone = TimeZone.getTimeZone("IST").apply {
        rawOffset = (5 * 60 + 30) * 60 * 1000 // +5:30 GMT Delhi
      }
      
      return sdf.format(Date(tLastSec * 1000))
      
    }
    
    @JvmStatic
    fun roundOffDecimal(number: Double): Double? {
      val df = DecimalFormat("#.##")
      df.roundingMode = RoundingMode.CEILING
      return df.format(number).toDouble()
    }
    
    @JvmStatic
    fun getAQIcolourMappingRes(aqi: Int) = when (aqi) {
      in 0..50 -> R.color.aqi_good
      in 51..100 -> R.color.aqi_satisfactory
      in 101..200 -> R.color.aqi_moderate
      in 201..300 -> R.color.aqi_poor
      in 301..400 -> R.color.aqi_very_poor
      in 401..500 -> R.color.aqi_severe
      else -> R.color.purple_700 // danger!
    }
    
    @JvmStatic
    infix fun Context.getAQIcolor(aqi: Int): Int {
      return ContextCompat.getColor(
        this, getAQIcolourMappingRes(aqi)
      )
    }
    
    @JvmStatic
    fun getAllRelevantColourLines(minY: Float, maxY: Float) = listOf(
      0f to R.color.aqi_good,
      50f to R.color.aqi_good,
      50.1f to R.color.aqi_satisfactory,
      100f to R.color.aqi_satisfactory,
      100.1f to R.color.aqi_moderate,
      200f to R.color.aqi_moderate,
      200.1f to R.color.aqi_poor,
      300f to R.color.aqi_poor,
      300.1f to R.color.aqi_very_poor,
      400f to R.color.aqi_very_poor,
      400.1f to R.color.aqi_severe,
      500f to R.color.aqi_severe
    ).filter { (it.first > (minY - 10) && it.first < (maxY + 10)) }
    // remove filter to allow all, but deviations will squeeze.
    
    @JvmStatic
    fun getAQIemojiMappingRes(aqi: Int) = when (aqi) {
      in 0..50 -> "😍"
      in 51..100 -> "🥳"
      in 101..200 -> "🙂"
      in 201..300 -> "😫"
      in 301..400 -> "🤢"
      in 401..500 -> "☠"
      else -> "👻"
    }
    
    @JvmStatic
    fun transformList(list: ArrayList<RvCityUpdateItem>, items: ArrayList<AQIItem>, recentUpdate: Long): ArrayList<RvCityUpdateItem> {
      items?.forEach { item -> // all incoming are made use of
        var foundIndex = -1
        list.forEachIndexed { index, rvCityUpdateItem ->
          if (rvCityUpdateItem.city == item.city) {
            foundIndex = index
            return@forEachIndexed
          }
        }
        
        when (foundIndex) {
          -1 -> { // new city
            list.add(RvCityUpdateItem(item.city, item.aqi, arrayListOf(HistoryItem(item.aqi, recentUpdate), HistoryItem(item.aqi, recentUpdate - 1)), recentUpdate))
          }
          else -> { // existing city new AQI ping
            list[foundIndex].apply {
              tLastUpdated = recentUpdate
              currentAQI = item.aqi
              past?.add(0, HistoryItem(item.aqi, recentUpdate)) // add at first.
              if (past?.size ?: 0 > MAX_TIME_SERIES) { // when too old,
                past?.remove(past?.last()) // each time pop 1.
              }
            }
          }
        }
      }
      return list
    }
    
  }
}