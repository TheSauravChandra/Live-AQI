package com.saurav.sauravcaqi.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.saurav.sauravcaqi.R
import com.saurav.sauravcaqi.bean.AQIItem
import com.saurav.sauravcaqi.bean.HistoryItem
import com.saurav.sauravcaqi.bean.RvCityUpdateItem
import com.saurav.sauravcaqi.databinding.AqiCityCardBinding
import com.saurav.sauravcaqi.utils.MyUtils
import com.saurav.sauravcaqi.utils.MyUtils.Companion.getAQIcolor
import com.saurav.sauravcaqi.utils.MyUtils.Companion.roundOffDecimal
import kotlin.math.min

class AqiCityAdapter(private val context: Context) : RecyclerView.Adapter<AqiCityAdapter.ViewHolder>() {
  private var list = arrayListOf(RvCityUpdateItem()) // table header item.
  private var recentUpdate = System.currentTimeMillis() / 1000
  private val TAG = "bharat"
  private var selectedIndex = -1
  private val MAX_TIME_SERIES = 15 // items. (~ 2sec)
  private val MAX_GRADIENT_SERIES = 5 // items. (~ 2sec)
  
  var callBack: ((item: RvCityUpdateItem?, showChart: Boolean) -> Unit)? = null
  var chartValueChangeSubscription: ((history: ArrayList<HistoryItem>?, city: String) -> Unit)? = null
  
  fun updateList(incomingSocketYield: ArrayList<AQIItem>?) {
    recentUpdate = System.currentTimeMillis() / 1000
    incomingSocketYield?.let {
      list = transformList(incomingSocketYield)
    }
    
    if (selectedIndex > -1 && selectedIndex < list.size)
      list[selectedIndex].apply {
        chartValueChangeSubscription?.let { it(past, city ?: "") }
      }
    
    notifyDataSetChanged()
  }
  
  private fun transformList(items: ArrayList<AQIItem>): ArrayList<RvCityUpdateItem> {
    val list = this.list
    
    items?.forEach { item -> // all incoming are made use of
      var foundIndex = -1
      list.forEachIndexed { index, rvCityUpdateItem ->
        if (rvCityUpdateItem.city == item.city) {
          foundIndex = index
          return@forEachIndexed
        }
      }
      
      when (foundIndex) {
        -1 -> {
          list.add(RvCityUpdateItem(item.city, item.aqi, arrayListOf(HistoryItem(item.aqi, recentUpdate), HistoryItem(item.aqi, recentUpdate - 1)), recentUpdate))
        }
        else -> {
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
  
  inner class ViewHolder(var binding: AqiCityCardBinding) : RecyclerView.ViewHolder(binding.root) {
    
    fun setData(data: RvCityUpdateItem?) {
      if (position == 0) {
        binding.tvCity.text = "City"
        binding.tvCurrentAQI.text = "Current AQI"
        binding.tvCurrentAQI.textSize = 16f
        binding.tvCurrentAQI.background = ContextCompat.getDrawable(context, R.drawable.bg_cell)
        binding.tvLastUpdated.text = "Last Updated"
        binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_150))
        binding.root.setOnClickListener {}
      } else {
        data?.run {
          binding.tvCity.text = city ?: ""
          binding.tvCurrentAQI.text = currentAQI?.let { roundOffDecimal(it) }?.toString() ?: ""
          binding.tvCurrentAQI.textSize = 20f
          val colors = past?.subList(0, min((past?.size ?: 0), MAX_GRADIENT_SERIES))?.map{ it ->
            context getAQIcolor (it.aqi?.toInt()?:0)
          }?.toIntArray()
          
          colors?.let {
            binding.tvCurrentAQI.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, it)
          }
          
          binding.tvLastUpdated.text = MyUtils.lastUpdated(recentUpdate, tLastUpdated)
        }
        
        binding.root.setBackgroundColor(ContextCompat.getColor(context, if (selectedIndex == position) R.color.purple_200 else R.color.white))
        
        
        binding.root.setOnClickListener {
          if (selectedIndex != position) {
            if (selectedIndex != -1)
              notifyItemChanged(selectedIndex)
            selectedIndex = position
          } else {
            selectedIndex = -1
          }
          notifyItemChanged(position)
          callBack?.let { it(data, selectedIndex != -1) }
          
          if (selectedIndex > -1)
            list[selectedIndex]?.let {
              chartValueChangeSubscription?.let { it1 -> it1(it?.past, it?.city ?: "") }
            }
        }
      }
      
    }
  }
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = AqiCityCardBinding.inflate(
      LayoutInflater.from(parent.context),
      parent, false
    )
    return ViewHolder(binding)
  }
  
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.setData(list[position])
    holder.binding.executePendingBindings()
  }
  
  override fun getItemCount() = list.size
  
}