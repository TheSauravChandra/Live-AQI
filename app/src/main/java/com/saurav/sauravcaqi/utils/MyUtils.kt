package com.saurav.sauravcaqi.utils

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.saurav.sauravcaqi.R
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MyUtils {
  companion object {
  
    @JvmStatic
    fun Context.toast(msg: String = "") {
      Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
  
    @JvmStatic
    fun Context.availInternet(): Boolean {
      val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val activeNetwork = cm.activeNetworkInfo
      return activeNetwork != null && activeNetwork.isConnected
    }
    
    @JvmStatic
    fun lastUpdated(tNowSec: Long, tLastSec: Long?): String { // in seconds,  val now = System.currentTimeMillis()
      return when {
        tLastSec == null -> { // never updated.
          " --- "
        }
        (tNowSec - tLastSec) < 60 -> {
          "A few seconds ago"
        }
        (tNowSec - tLastSec) > 60 && (tNowSec - tLastSec) < (60 * 2) -> {
          "A minute ago"
        }
        else -> {
          val sdf = SimpleDateFormat("HH:mm:ss a")
          val resultdate = Date(tLastSec * 1000)
          sdf.format(resultdate)
        }
      }
      
    }
    
    @JvmStatic
    fun roundOffDecimal(number: Double): Double? {
      val df = DecimalFormat("#.##")
      df.roundingMode = RoundingMode.CEILING
      return df.format(number).toDouble()
    }
    
  }
}