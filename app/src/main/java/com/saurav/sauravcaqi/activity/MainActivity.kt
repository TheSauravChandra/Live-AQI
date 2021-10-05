package com.saurav.sauravcaqi.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.saurav.sauravcaqi.R
import com.saurav.sauravcaqi.adapter.AqiCityAdapter
import com.saurav.sauravcaqi.bean.HistoryItem
import com.saurav.sauravcaqi.socket.MySocketListener
import com.saurav.sauravcaqi.utils.Constants
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : AppCompatActivity() {
  private val client: OkHttpClient by lazy { OkHttpClient() }
  private val TAG = "bharat"
  private val adapter = AqiCityAdapter(this)
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    initUI()
    initRV()
    initialiseWebSocket()
  }
  
  private fun listenForChart(history: ArrayList<HistoryItem>?, city: String) {
    lineChart.apply {
      val now = System.currentTimeMillis() / 1000
      history?.map {
        Entry((now - (it.t ?: now)).toFloat(), it.aqi?.toFloat() ?: 0f)
      }?.let {
        data = LineData(LineDataSet(it, "$city AQI"))
      }
      description = Description().apply {
        text = "Live AQI Details for $city"
      }
      setPinchZoom(false)
      invalidate()
    }
  }
  
  private fun initRV() {
    adapter.callBack = { item, showChart ->
      lineChart.visibility = if (showChart) View.VISIBLE else View.INVISIBLE
    }
    
    adapter.subscription = { history, city ->
      listenForChart(history, city)
    }
    
    rvList.adapter = adapter
    rvList.layoutManager = LinearLayoutManager(this)
  }
  
  private fun initUI() {
    supportActionBar?.hide()
    lineChart.visibility = View.GONE
    loading.visibility = View.VISIBLE
  }
  
  private fun initialiseWebSocket() {
    val req = Request.Builder().url(Constants.WEB_SOCKET_URL).build()
    val listener = MySocketListener { data, t ->
      lifecycleScope.launch(Main) {
        if(loading.visibility == View.VISIBLE)
          loading.visibility = View.GONE
        adapter?.updateList(data)
      }
    }
    val webSocket = client.newWebSocket(req, listener)
  }
}