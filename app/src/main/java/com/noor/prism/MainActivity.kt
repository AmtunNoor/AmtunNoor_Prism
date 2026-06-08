package com.noor.prism

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val menuUrl = "https://amtunnoor.github.io/AmtunNoor_Prism/menu.json"
    private val cacheKey = "cached_menu_json"
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        fetchMenuData()
    }

    private fun fetchMenuData() {
        val request = Request.Builder().url(menuUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { loadCachedDataOrShowError() }
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    saveToCache(json)
                    runOnUiThread { parseAndDisplayJson(json) }
                } ?: runOnUiThread { loadCachedDataOrShowError() }
            }
        })
    }

    private fun parseAndDisplayJson(json: String) {
        try {
            val itemType = object : TypeToken<List<MenuModel>>() {}.type
            val menuItems: List<MenuModel> = gson.fromJson(json, itemType)
            progressBar.visibility = View.GONE
            recyclerView.adapter = MenuAdapter(menuItems) { selectedItem ->
                val intent = Intent(this, WebViewActivity::class.java).apply {
                    putExtra("TARGET_URL", selectedItem.url)
                }
                startActivity(intent)
            }
            recyclerView.requestFocus()
        } catch (e: Exception) {
            loadCachedDataOrShowError()
        }
    }

    private fun saveToCache(json: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        sharedPref.edit().putString(cacheKey, json).apply()
    }

    private fun loadCachedDataOrShowError() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val cachedJson = sharedPref.getString(cacheKey, null)
        if (cachedJson != null) {
            parseAndDisplayJson(cachedJson)
        } else {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Unable to load menu.", Toast.LENGTH_LONG).show()
        }
    }
}
