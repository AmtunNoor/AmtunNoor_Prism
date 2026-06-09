package com.noor.prism

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuAdapter
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MenuAdapter(emptyList()) { menuItem ->
            val intent = Intent(this, WebViewActivity::class.java).apply {
                putExtra("URL", menuItem.url)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        fetchMenuData()
    }

    private fun fetchMenuData() {
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/AmtunNoor/NoorPrism/main/menu.json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to load menu", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    val menuItems: List<MenuItem> = Gson().fromJson(
                        jsonString,
                        object : TypeToken<List<MenuItem>>() {}.type
                    )
                    runOnUiThread {
                        adapter.updateData(menuItems)
                    }
                }
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
