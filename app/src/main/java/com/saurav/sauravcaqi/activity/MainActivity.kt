package com.saurav.sauravcaqi.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.saurav.sauravcaqi.R
import com.saurav.sauravcaqi.adapter.AqiCityAdapter
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
  
  private fun initRV() {
    adapter?.attachCallback { item, card ->
      Toast.makeText(this,"HISTORY:"+item?.past,Toast.LENGTH_SHORT).show()
    }
    rvList.adapter = adapter
    rvList.layoutManager = LinearLayoutManager(this)
  }
  
  private fun initUI() {
    supportActionBar?.hide()
  }
  
  private fun initialiseWebSocket() {
    val req = Request.Builder().url(Constants.WEB_SOCKET_URL).build()
    val listener = MySocketListener { data, t ->
      lifecycleScope.launch(Main) {
        adapter?.updateList(data)
      }
    }
    val webSocket = client.newWebSocket(req, listener)
  }
}