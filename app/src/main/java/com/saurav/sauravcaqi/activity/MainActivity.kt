package com.saurav.sauravcaqi.activity

import android.graphics.Color
import android.os.Bundle
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
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
import com.google.android.material.snackbar.Snackbar
import com.saurav.sauravcaqi.R
import com.saurav.sauravcaqi.adapter.AqiCityAdapter
import com.saurav.sauravcaqi.bean.HistoryItem
import com.saurav.sauravcaqi.socket.MySocketListener
import com.saurav.sauravcaqi.utils.AQIchartXaxisFormatter
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
    setChartUI()
  }
  
  private fun setChartUI() {
    lineChart.apply {
      // styling black background, white text & cyan (graph+legend).
      
      description = Description().apply {
        textColor = Color.WHITE
      }
      
      xAxis.enableAxisLineDashedLine(15f, 10f, 5f)
      xAxis.isEnabled = true
      xAxis.labelCount = 7
      xAxis.gridColor = Color.WHITE
      xAxis.axisLineColor = Color.WHITE
      xAxis.textColor = Color.WHITE
      xAxis.valueFormatter = AQIchartXaxisFormatter()
      
      axisLeft.apply {
        axisLineColor = Color.WHITE
        titleColor = Color.WHITE
        zeroLineColor = Color.WHITE
        textColor = Color.WHITE
      }
      
      axisRight.apply {
        axisLineColor = Color.WHITE
        titleColor = Color.WHITE
        zeroLineColor = Color.WHITE
        textColor = Color.WHITE
      }
      
      legend.textColor = Color.CYAN
      
      animateXY(200, 200)
      setTouchEnabled(false)
      setPinchZoom(false)
      notifyDataSetChanged()
      invalidate()
    }
  }
  
  private fun listenForChartUpdate(history: ArrayList<HistoryItem>?, city: String) {
    lineChart.apply {
      val now = System.currentTimeMillis() / 1000
      history?.map {
        Entry((now - (it.t ?: now)).toFloat(), it.aqi?.toFloat() ?: 0f)
      }?.let {
        data = LineData(LineDataSet(it, "$city AQI").apply {
          color = Color.CYAN
          valueTextColor = Color.CYAN
          titleColor = Color.CYAN
        }).apply {
          setValueTextColor(Color.WHITE)
          titleColor = Color.WHITE
        }
      }
      
      description = Description().apply {
        text = "Live AQI Details for $city"
      }
      
      notifyDataSetChanged()
      invalidate()
    }
  }
  
  private fun manageChartShowHideAnim() {
    val transition: Transition = Fade()
    transition.duration = 600
    transition.addTarget(card)
    TransitionManager.beginDelayedTransition(findViewById(android.R.id.content), transition)
  }
  
  private fun initRV() {
    adapter.callBack = { _, showChart ->
      manageChartShowHideAnim()
      card.visibility = if (showChart) View.VISIBLE else View.INVISIBLE
    }
    
    adapter.chartValueChangeSubscription = { history, city ->
      listenForChartUpdate(history, city)
    }
    
    rvList.adapter = adapter
    rvList.layoutManager = LinearLayoutManager(this)
  }
  
  private fun initUI() {
    supportActionBar?.hide()
    card.visibility = View.GONE
    loading.visibility = View.VISIBLE
  }
  
  private fun hideLoadingIfVisible() {
    if (loading.visibility == View.VISIBLE)
      loading.visibility = View.GONE
  }
  
  private fun handleSocketDown(){
    listener?.onSocketDown = { // Failed to fetch
      // update UI from failed fetch
      lifecycleScope.launch(Main) {
        // update time stamps of data to old.
        adapter?.updateList(null)
      
        // snackbar for "gone offline".
        if (!(dialog?.isShowing ?: false) && !availInternet() && sn?.duration != Snackbar.LENGTH_INDEFINITE) { // dialog not showing & net off & snackbar not already showing.
          sn = Snackbar.make(findViewById(android.R.id.content), "🔁 Reconnecting: Please check your Internet connection...", Snackbar.LENGTH_INDEFINITE)
          sn?.view?.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.aqi_moderate))
          sn?.show()
        }
      }
    
      // retry establishing network
      lifecycleScope.launch(IO) {
        delay(1000)
        webSocket = client.newWebSocket(request, listener) // re init network
      }
    }
  }
  
  private fun handleSocketLiveAgain(){
    listener?.uponSocketLiveAgain = { // connection revive.
      lifecycleScope.launch(Main) {
        // back online! (when using app & net went off)
        sn?.dismiss()
        if (sn != null) {
          sn = Snackbar.make(findViewById(android.R.id.content), "😍 Back Online!", Snackbar.LENGTH_SHORT)
          sn?.view?.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.aqi_good))
          sn?.show()
        } else {
          // (when started the app & found net off)
          dialog?.cancel()
        }
      }
    }
  }
  
  private fun initialiseWebSocket() {
    request = Request.Builder().url(Constants.WEB_SOCKET_URL).build()
    listener = MySocketListener { data, t ->
      // updating list!
      lifecycleScope.launch(Main) {
        hideLoadingIfVisible()
        adapter?.updateList(data)
      }
    }
    handleSocketDown()
    handleSocketLiveAgain()
    // starting the fun.. :)
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
      setTitle(getString(R.string.pls_turn_on_net))
      setPositiveButton(getString(R.string.retry)) { p0, p1 ->
        initNtwConnection()
      }
      setCancelable(false)
      setFinishOnTouchOutside(false)
      setNeutralButton(getString(R.string.leave_app)) { _, _ ->
        goodbye()
        finish()
      }
      dialog = show()
    }
  }
  
  private fun goodbye() = toast(getString(R.string.goodbye_msg))
  
}