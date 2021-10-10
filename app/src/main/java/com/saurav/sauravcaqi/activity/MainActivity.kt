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
import androidx.core.view.updateLayoutParams
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
import com.saurav.sauravcaqi.utils.AQIchartXaxisFormatter
import com.saurav.sauravcaqi.utils.MyUtils.Companion.availInternet
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
          verticalWeight = if(verticalWeight==0.5f) 2f else 0.5f
      }
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
        // update time stamps of data to old.
        adapter?.updateList(null)
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
  
  private fun initialiseWebSocket() {
    viewModel.setValueListener { data, t ->
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
        goodbye()
        finish()
      }
      dialog = show()
    }
  }
  
  private fun goodbye() = toast(getString(R.string.goodbye_msg))
  
}