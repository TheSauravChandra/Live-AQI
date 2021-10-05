package com.saurav.sauravcaqi.adapter

import android.content.Context
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
import com.saurav.sauravcaqi.utils.MyUtils.Companion.roundOffDecimal

class AqiCityAdapter(private val context: Context) : RecyclerView.Adapter<AqiCityAdapter.ViewHolder>() {
  private var list = arrayListOf(RvCityUpdateItem()) // table header item.
  private var recentUpdate = System.currentTimeMillis()
  private val TAG = "bharat"
  
  // UI relevant data
  private val MAX_TIME_SERIES = 30 // items. (~ 2sec)
  
  // city: 105dp width
  
  // for shared animation we need views too, & next page needs data to show.
  private var callBack: ((item: RvCityUpdateItem?, card: AqiCityCardBinding) -> Unit)? = null
  
  fun attachCallback(gc: (item: RvCityUpdateItem?, card: AqiCityCardBinding) -> Unit) {
    this.callBack = gc
  }
  
  fun updateList(incomingSocketYield: ArrayList<AQIItem>?) {
    recentUpdate = System.currentTimeMillis() / 1000
    incomingSocketYield?.let {
      val items = transformList(incomingSocketYield)
      list = items
      notifyDataSetChanged()
    }
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
          list.add(RvCityUpdateItem(item.city, item.aqi, arrayListOf(HistoryItem(item.aqi, recentUpdate)), recentUpdate))
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
        binding.tvLastUpdated.text = "Last Updated"
        binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_150))
      } else {
        data?.run {
          binding.tvCity.text = city ?: ""
          binding.tvCurrentAQI.text = currentAQI?.let { roundOffDecimal(it) }?.toString() ?: ""
          binding.tvLastUpdated.text = MyUtils.lastUpdated(recentUpdate, tLastUpdated)
        }
        
        binding.root.setOnClickListener {
          callBack?.let { it(data, binding) }
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