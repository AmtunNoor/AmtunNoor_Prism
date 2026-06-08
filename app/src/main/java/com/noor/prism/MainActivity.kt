package com.noor.prism

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.noor.prism.databinding.ActivityMainBinding
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val client = OkHttpClient()
    private val gson = Gson()
    private val menuUrl = "https://amtunnoor.github.io/AmtunNoor_Prism/menu.json"
    private val cacheKey = "cached_menu_json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup a TV grid with 4 items across rows
        binding.recyclerView.layoutManager = GridLayoutManager(this, 4)

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
                    // Cache item instantly for offline resiliency
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

            binding.progressBar.visibility = View.GONE
            binding.recyclerView.adapter = MenuAdapter(menuItems) { selectedItem ->
                val intent = Intent(this@MainActivity, WebViewActivity::class.java).apply {
                    putExtra("TARGET_URL", selectedItem.url)
                }
                startActivity(intent)
            }
            // Request default focus to prevent remote layout lockup
            binding.recyclerView.requestFocus()
        } catch (e: Exception) {
            loadCachedDataOrShowError()
        }
    }

    private fun saveToCache(json: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(cacheKey, json)
            apply()
        }
    }

    private fun loadCachedDataOrShowError() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val cachedJson = sharedPref.getString(cacheKey, null)

        if (cachedJson != null) {
            parseAndDisplayJson(cachedJson)
            Toast.makeText(this, "Offline Mode: Loaded from Cache", Toast.LENGTH_LONG).show()
        } else {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, getString(R.string.error_loading), Toast.LENGTH_LONG).show()
        }
    }
}
