package com.saurav.sauravcaqi.bean

data class RvCityUpdateItem(var city: String? = null, var currentAQI: Double? = null, var past: ArrayList<HistoryItem>? = null, var tLastUpdated: Long? = null)

data class HistoryItem(val aqi:Double?,val t:Long?)