package com.saurav.sauravcaqi.activity

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import com.saurav.sauravcaqi.R
import com.saurav.sauravcaqi.adapter.AqiCityAdapter
import com.saurav.sauravcaqi.bean.HistoryItem
import com.saurav.sauravcaqi.utils.AQIchartXaxisFormatter
import com.saurav.sauravcaqi.utils.MyUtils.Companion.availInternet
import com.saurav.sauravcaqi.utils.MyUtils.Companion.getAllRelevantColourLines
import com.saurav.sauravcaqi.utils.MyUtils.Companion.toast
import com.saurav.sauravcaqi.vm.AqiVM
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity() {
  private val TAG = "bharat"
  private val adapter: AqiCityAdapter by inject()
  private val viewModel: AqiVM by viewModel()
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
      xAxis.decorXAxis()
      axisLeft.decorYAxis()
      axisRight.decorYAxis()
      legend.textColor = Color.CYAN
      animateXY(200, 200)
      setTouchEnabled(false)
      setPinchZoom(false)
      notifyDataSetChanged()
      invalidate()
    }
  }
  
  private fun YAxis.decorYAxis() {
    axisLineColor = Color.WHITE
    titleColor = Color.WHITE
    zeroLineColor = Color.WHITE
    textColor = Color.WHITE
  }
  
  private fun XAxis.decorXAxis() {
    enableAxisLineDashedLine(15f, 10f, 5f)
    isEnabled = true
    labelCount = 7
    gridColor = Color.BLACK
    axisLineColor = Color.WHITE
    textColor = Color.WHITE
    valueFormatter = AQIchartXaxisFormatter()
  }
  
  private fun getChartGraph(history: ArrayList<HistoryItem>?, city: String, now: Long) =
    LineDataSet(history?.map { Entry((now - (it.t ?: now)).toFloat(), it.aqi?.toFloat() ?: 0f) } ?: emptyList(), "$city AQI")
      .apply {
        color = Color.CYAN
        valueTextColor = Color.CYAN
        titleColor = Color.CYAN
      }
  
  private fun getAQIcolorHorizontalLines(startX: Float, endX: Float, minY: Float, maxY: Float) =
    getAllRelevantColourLines(minY, maxY).map { pair -> // 1:aqi,2:color
      LineDataSet(listOf(Entry(startX, pair.first), Entry(endX, pair.first)), null).apply {
        ContextCompat.getColor(this@MainActivity, pair.second).let {
          color = it
          valueTextColor = it
          titleColor = it
        }
      }
    }.toTypedArray()
  
  private fun listenForChartUpdate(history: ArrayList<HistoryItem>?, city: String) {
    lineChart.apply {
      val now = System.currentTimeMillis() / 1000
      val startX = 0f
      val endX = history?.last().let { (now - (it?.t ?: now)).toFloat() } ?: 0f
      
      val maxY = history?.maxByOrNull { it.aqi ?: 0.0 }?.aqi?.toFloat() ?: 0f
      val minY = history?.minByOrNull { it.aqi ?: 0.0 }?.aqi?.toFloat() ?: 0f
      
      data = LineData(
        getChartGraph(history, city, now),
        *(getAQIcolorHorizontalLines(startX, endX, minY, maxY))
      ).apply {
        setValueTextColor(Color.WHITE)
        titleColor = Color.WHITE
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
      // USER CITY CLICK(non cyclic event): select / deselect - manually / programatically
      manageChartShowHideAnim()
      card.visibility = if (showChart) View.VISIBLE else View.INVISIBLE
      card.updateLayoutParams<ConstraintLayout.LayoutParams> {
        verticalWeight = if (showChart) 1f else 0f
      }
      
      // a tick / clock pulse back to the chart, on user adapter interact, non cyclic.. data still going via VM
      viewModel.allData.value?.let { data ->
        // send data to share.
        if (adapter.getSelectedIndex() > -1 && adapter.getSelectedIndex() < data.list.size)
          data.list[adapter.getSelectedIndex()].apply {
            listenForChartUpdate(past, city ?: "")
          }
      }
    }
    
    rvList.adapter = adapter
    rvList.layoutManager = LinearLayoutManager(this)
  }
  
  private fun initUI() {
    supportActionBar?.hide()
    card.visibility = View.GONE
    card.updateLayoutParams<ConstraintLayout.LayoutParams> {
      verticalWeight = 0f
    }
    loading.visibility = View.VISIBLE
    
    vSpectrum.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
      intArrayOf(
        R.color.aqi_good,
        R.color.aqi_satisfactory,
        R.color.aqi_moderate,
        R.color.aqi_poor,
        R.color.aqi_very_poor,
        R.color.aqi_severe
      ).map { ContextCompat.getColor(this, it) }.toIntArray()
    )
    
    card.setOnClickListener {
      rvList.updateLayoutParams<ConstraintLayout.LayoutParams> {
        verticalWeight = if (verticalWeight == 0.5f) 2f else 0.5f
      }
    }
    
    tvTitle.setOnClickListener {
      showInfo()
    }
    
  }
  
  private fun hideLoadingIfVisible() {
    if (loading.visibility == View.VISIBLE)
      loading.visibility = View.GONE
  }
  
  private fun handleSocketDown() {
    viewModel.setOnSocketDownListener { // Failed to fetch
      // update UI from failed fetch
      lifecycleScope.launch(Main) {
        snackbarGoneOffline()
      }
    }
  }
  
  private fun snackbarGoneOffline() {
    // dialog not showing & net off & snackbar not already showing.
    if (!(dialog?.isShowing ?: false) && !availInternet() && sn?.duration != Snackbar.LENGTH_INDEFINITE) {
      sn = Snackbar.make(findViewById(android.R.id.content), getString(R.string.reconnecting), Snackbar.LENGTH_INDEFINITE)
      sn?.view?.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.aqi_moderate))
      sn?.show()
    }
  }
  
  private fun snackbarBackOnline() {
    sn = Snackbar.make(findViewById(android.R.id.content), getString(R.string.back_online), Snackbar.LENGTH_SHORT)
    sn?.view?.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.aqi_good))
    sn?.show()
  }
  
  private fun handleSocketLiveAgain() {
    viewModel.setUponSocketLiveAgainListener { // connection revive.
      lifecycleScope.launch(Main) {
        sn?.dismiss() // dismiss offline snackbar, if present (while running app if went offline)
        if (sn != null) {
          // back online! (when using app & net went off)
          snackbarBackOnline()
        } else {
          // (when started the app & found net off)
          dialog?.cancel()
        }
      }
    }
  }
  
  override fun onResume() {
    super.onResume()
    viewModel.checkAndRestartIfKilled()
  }
  
  private fun initialiseWebSocket() {
    viewModel.allData.observe(this@MainActivity, { data ->
      hideLoadingIfVisible()
      adapter?.updateList(data)
      // send data to share to chart, VM->chart, with adapter select interception & routing data stream. cyclic.
      if (adapter.getSelectedIndex() > -1 && adapter.getSelectedIndex() < data.list.size)
        data.list[adapter.getSelectedIndex()].apply {
          listenForChartUpdate(past, city ?: "")
        }
    })
    handleSocketDown()
    handleSocketLiveAgain()
    // starting the fun.. :)
    initNtwConnection()
  }
  
  private fun initNtwConnection() {
    if (!availInternet()) {
      askTurnOnInternet()
    }
    viewModel.startAppNtwHandling()
  }
  
  private fun askTurnOnInternet() {
    // alert to ask for net: retry or exit
    with(AlertDialog.Builder(this))
    {
      setTitle(getString(R.string.pls_turn_on_net))
      setPositiveButton(getString(R.string.retry)) { _, _ ->
        initNtwConnection()
      }
      setCancelable(false)
      setFinishOnTouchOutside(false)
      setNeutralButton(getString(R.string.leave_app)) { _, _ ->
        acknowledgement()
        finish()
      }
      dialog = show()
    }
  }
  
  private fun acknowledgement() = toast(getString(R.string.goodbye_msg))
  
  private fun showInfo() {
    // alert to ask for net: retry or exit
    with(AlertDialog.Builder(this))
    {
      setTitle(getString(R.string.info_title))
      setMessage(getString(R.string.info_msg))
      setPositiveButton(getString(R.string.sure)) { _, _ ->
        acknowledgement()
      }
      setCancelable(true)
      setFinishOnTouchOutside(false)
      show()
    }
  }
  
  override fun onBackPressed() {
    if (card.isVisible) {
      rvList.updateLayoutParams<ConstraintLayout.LayoutParams> {
        if (verticalWeight == 0.5f) {
          verticalWeight = 2f
        } else {
          adapter.removeChart()
        }
      }
    } else {
      acknowledgement()
      super.onBackPressed()
    }
    
  }
  
}