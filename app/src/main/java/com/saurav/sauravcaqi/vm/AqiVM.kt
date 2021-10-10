package com.saurav.sauravcaqi.vm

import androidx.lifecycle.ViewModel
import com.saurav.sauravcaqi.bean.AQIItem
import com.saurav.sauravcaqi.socket.MySocketListener
import com.saurav.sauravcaqi.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AqiVM(private val client: OkHttpClient) : ViewModel() {
  private var request: Request? = null
  private var listener: MySocketListener? = null
  private var webSocket: WebSocket? = null
  private var callback = { data: ArrayList<AQIItem>?, t: Long -> }
  private var uponSocketLiveAgain = {}
  private var onSocketDown = {}
  private val TAG = "bharat327"
  
  init {
    request = Request.Builder().url(Constants.WEB_SOCKET_URL).build()
  }
  
  fun initNtw() {
    // callback must be fresh by then.
    listener = MySocketListener(callback)
    listener?.onSocketDown = {
      onSocketDown()
    }
    listener?.uponSocketLiveAgain = {
      uponSocketLiveAgain()
    }
    listener?.attemptRestart = {
      viewModelScope.launch(Dispatchers.IO) {
        delay(1000)
        createNewWebSocket()
      }
    }
    createNewWebSocket()
  }
  
  private fun createNewWebSocket(){
    webSocket = client.newWebSocket(request, listener)
  }
  
  fun setValueListener(cb: (data: ArrayList<AQIItem>?, t: Long) -> Unit) {
    callback = cb
  }
  
  fun setOnSocketDownListener(socketDown: () -> Unit) {
    onSocketDown = socketDown
  }
  
  fun setUponSocketLiveAgainListener(socketAlive: () -> Unit) {
    uponSocketLiveAgain = socketAlive
  }
  
  fun startAppNtwHandling() {
    if (webSocket == null)
      initNtw()
  }
  
}