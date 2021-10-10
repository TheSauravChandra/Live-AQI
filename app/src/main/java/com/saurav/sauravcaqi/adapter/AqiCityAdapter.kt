package com.saurav.sauravcaqi.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.saurav.sauravcaqi.R
import com.saurav.sauravcaqi.bean.RvCityUpdateItem
import com.saurav.sauravcaqi.bean.UiBulkData
import com.saurav.sauravcaqi.databinding.AqiCityCardBinding
import com.saurav.sauravcaqi.utils.Constants.MAX_GRADIENT_SERIES
import com.saurav.sauravcaqi.utils.MyUtils
import com.saurav.sauravcaqi.utils.MyUtils.Companion.getAQIcolor
import com.saurav.sauravcaqi.utils.MyUtils.Companion.getAQIemojiMappingRes
import com.saurav.sauravcaqi.utils.MyUtils.Companion.roundOffDecimal
import kotlin.math.min

class AqiCityAdapter(private val context: Context) : RecyclerView.Adapter<AqiCityAdapter.ViewHolder>() {
  private var list = ArrayList<RvCityUpdateItem>() // table header item.
  private var recentUpdate = System.currentTimeMillis() / 1000
  private val TAG = "bharat"
  private var selectedIndex = -1
  
  var callBack: ((item: RvCityUpdateItem?, showChart: Boolean) -> Unit)? = null
  
  fun getSelectedIndex() = selectedIndex
  
  fun updateList(data: UiBulkData) {
    recentUpdate = data.recentUpdate
    list = data.list
    notifyDataSetChanged()
  }
  
  inner class ViewHolder(var binding: AqiCityCardBinding) : RecyclerView.ViewHolder(binding.root) {
    
    fun setData(data: RvCityUpdateItem?) {
      if (position == 0) {
        binding.tvCity.text = context.getString(R.string.city_header)
        binding.tvCurrentAQI.text = context.getString(R.string.current_aqi_header)
        binding.tvCurrentAQI.textSize = 16f
        binding.tvCurrentAQI.background = ContextCompat.getDrawable(context, R.drawable.bg_cell)
        binding.tvLastUpdated.text = context.getString(R.string.last_updated_header)
        binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_150))
        binding.root.setOnClickListener {}
      } else {
        data?.run {
          binding.tvCity.text = city ?: ""
          binding.tvCurrentAQI.text = currentAQI?.let { getAQIemojiMappingRes(it.toInt()) + " " + roundOffDecimal(it) } ?: ""
          binding.tvCurrentAQI.textSize = 20f
          val colors = past?.subList(0, min((past?.size ?: 0), MAX_GRADIENT_SERIES))?.map { it ->
            context getAQIcolor (it.aqi?.toInt() ?: 0)
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
        }
      }
      
    }
  }
  
  fun removeChart() {
    if (selectedIndex != -1)
      notifyItemChanged(selectedIndex)
    selectedIndex = -1
    callBack?.let { it(null, false) }
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