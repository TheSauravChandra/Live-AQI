package com.saurav.sauravcaqi.socket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saurav.sauravcaqi.bean.AQIItem
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.lang.reflect.Type


class MySocketListener(private val cb: (data: ArrayList<AQIItem>?, t: Long) -> Unit) : WebSocketListener() {
  private val gson = Gson()
  private val TAG = "bharat"
  var uponSocketLiveAgain = {}
  var onSocketDown = {}
  var attemptRestart = {}
  
  private fun sendBackData(data: String?) {
    data?.let {
      val aqiType: Type = object : TypeToken<ArrayList<AQIItem>?>() {}.type
      cb(
        try {
          gson.fromJson(it, aqiType)
        } catch (e: Exception) {
          Log.e(TAG, "Error: " + e.message)
          ArrayList()
        }, System.currentTimeMillis() / 1000
      )
    }
  }
  
  override fun onMessage(webSocket: WebSocket?, text: String?) {
    super.onMessage(webSocket, text)
    /*
    [{"city":"Mumbai","aqi":181.3946178595279},
    {"city":"Delhi","aqi":303.28831346653243},
    {"city":"Kolkata","aqi":201.44973198212693},
    {"city":"Bhubaneswar","aqi":98.68245442085266},
    {"city":"Chennai","aqi":138.89705705011673},
    {"city":"Pune","aqi":223.89340025234293},
    {"city":"Hyderabad","aqi":199.9739088073019},
    {"city":"Jaipur","aqi":141.6453990026219},
    {"city":"Lucknow","aqi":76.1595337060258}]
     */
    sendBackData(text)
  }
  
  override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
    super.onClosed(webSocket, code, reason)
    onSocketDown()
    attemptRestart()
  }
  
  override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
    super.onFailure(webSocket, t, response)
    onSocketDown()
    attemptRestart()
  }
  
  override fun onOpen(webSocket: WebSocket?, response: Response?) {
    super.onOpen(webSocket, response)
    uponSocketLiveAgain()
  }
  
}