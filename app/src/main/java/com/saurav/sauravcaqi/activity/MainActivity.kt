package com.saurav.sauravcaqi.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.snackbar.Snackbar
import com.saurav.sauravcaqi.R
import com.saurav.sauravcaqi.adapter.AqiCityAdapter
import com.saurav.sauravcaqi.bean.HistoryItem
import com.saurav.sauravcaqi.socket.MySocketListener
import com.saurav.sauravcaqi.utils.Constants
import com.saurav.sauravcaqi.utils.MyUtils.Companion.availInternet
import com.saurav.sauravcaqi.utils.MyUtils.Companion.toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class MainActivity : AppCompatActivity() {
  private val client: OkHttpClient by lazy { OkHttpClient() }
  private val TAG = "bharat"
  private val adapter = AqiCityAdapter(this)
  
  private var request: Request? = null
  private var listener: MySocketListener? = null
  private var webSocket: WebSocket? = null
  private var sn: Snackbar? = null
  private var dialog: AlertDialog? = null
  
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
    request = Request.Builder().url(Constants.WEB_SOCKET_URL).build()
    listener = MySocketListener { data, t ->
      lifecycleScope.launch(Main) {
        if (loading.visibility == View.VISIBLE)
          loading.visibility = View.GONE
        
        adapter?.updateList(data)
      }
    }
  
    listener?.onSocketDown = { // Failed to fetch
      lifecycleScope.launch(Main) {
        // update time stamps
        adapter?.updateList(null)
  
        // snackbar check
        if (!(dialog?.isShowing ?: false) && !availInternet() && sn?.duration != Snackbar.LENGTH_INDEFINITE) { // dialog not showing & net off & snackbar not already showing.
          sn = Snackbar.make(findViewById(android.R.id.content), "🔁 Reconnecting: Please check your Internet connection...", Snackbar.LENGTH_INDEFINITE)
          sn?.view?.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.aqi_moderate))
          sn?.show()
        }
      }
  
      lifecycleScope.launch(IO) {
        delay(1000)
        webSocket = client.newWebSocket(request, listener) // re init network
      }
    }
    
    listener?.uponSocketLiveAgain = {
      lifecycleScope.launch(Main) {
        sn?.dismiss()
        if (sn != null) {
          sn = Snackbar.make(findViewById(android.R.id.content), "😍 Back Online!", Snackbar.LENGTH_SHORT)
          sn?.view?.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.aqi_good))
          sn?.show()
        } else {
          dialog?.cancel()
        }
      }
    }
    
    initNtwConnection()
  }
  
  private fun initNtwConnection() {
    if (!availInternet()) {
      askTurnOnInternet()
    }
    
    if (webSocket == null) { // auto polling for net.
      webSocket = client.newWebSocket(request, listener)
    }
  }
  
  private fun askTurnOnInternet() {
    // alert to ask for net: retry or exit
    with(AlertDialog.Builder(this))
    {
      setTitle("Please Turn ON Internet")
      setPositiveButton("Retry") { p0, p1 ->
        initNtwConnection()
      }
      setCancelable(false)
      setFinishOnTouchOutside(false)
      setNeutralButton("Leave App") { _, _ ->
        toast("Developed by SauravC.\n with ❤️ in Bharat(India)\nhttp://sauravc.dx.am/")
        finish()
      }
      dialog = show()
    }
  }
  
}