package com.saurav.sauravcaqi.utils

import com.github.mikephil.charting.formatter.ValueFormatter

class AQIchartXaxisFormatter : ValueFormatter() {
  override fun getFormattedValue(value: Float): String {
    val x = "-${"%.2f".format(value)}s"
    return x
  }
}