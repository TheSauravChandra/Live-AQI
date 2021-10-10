package com.saurav.sauravcaqi.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurav.sauravcaqi.bean.AQIItem
import com.saurav.sauravcaqi.bean.RvCityUpdateItem
import com.saurav.sauravcaqi.bean.UiBulkData
import com.saurav.sauravcaqi.socket.MySocketListener
import com.saurav.sauravcaqi.utils.Constants
import com.saurav.sauravcaqi.utils.MyUtils.Companion.transformList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class AqiVM(private val client: OkHttpClient) : ViewModel() {
  private var request: Request? = null
  private var listener: MySocketListener? = null
  private var webSocket: WebSocket? = null
  private var callback = { data: ArrayList<AQIItem>?, t: Long -> }
  private var uponSocketLiveAgain = {}
  private var onSocketDown = {}
  private val TAG = "bharat327"
  
  private var list = arrayListOf(RvCityUpdateItem()) // table header item.
  private var recentUpdate = System.currentTimeMillis() / 1000
  
  // for UI data post, used livedata.. rest are IO stuff so did lambdas.
  private var _allData = MutableLiveData(UiBulkData(ArrayList(), recentUpdate))
  var allData: LiveData<UiBulkData> = _allData
  
  init {
    request = Request.Builder().url(Constants.WEB_SOCKET_URL).build()
    callback = { incomingSocketYield: ArrayList<AQIItem>?, t: Long ->
      recentUpdate = t
      incomingSocketYield?.let {
        list = transformList(list, it, recentUpdate)
      }
      _allData.postValue(UiBulkData(list, recentUpdate))
    }
  }
  
  fun initNtw() {
    // callback must be fresh by then.
    listener = MySocketListener(callback)
    listener?.onSocketDown = {
      // missed data pings
      recentUpdate = System.currentTimeMillis() / 1000
      _allData.postValue(UiBulkData(list, recentUpdate))
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
  
  private fun createNewWebSocket() {
    webSocket = client.newWebSocket(request, listener)
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