package com.noor.prism

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuAdapter
    private lateinit var mainRootContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainRootContainer = findViewById(R.id.mainRootContainer)
        recyclerView = findViewById(R.id.recyclerView)
        
        // Renders dashboard tiles horizontally side-by-side across the screen
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        adapter = MenuAdapter(emptyList()) { menuModel ->
            val intent = Intent(this, WebViewActivity::class.java).apply {
                putExtra("URL", menuModel.url)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadLocalMenuData()
    }

    private fun loadLocalMenuData() {
        try {
            val jsonString = assets.open("menu.json").use { inputStream ->
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                String(buffer, Charset.forName("UTF-8"))
            }

            val menuItems: List<MenuModel> = Gson().fromJson(
                jsonString,
                object : TypeToken<List<MenuModel>>() {}.type
            )

            // Update data and trigger visual entry loading animation for the kids
            adapter.updateData(menuItems)
            
            // Playful UI Entry Animation: Dashboard cards slide up gracefully over the rainbow background
            recyclerView.alpha = 0f
            recyclerView.translationY = 50f
            recyclerView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .withEndAction {
                    recyclerView.requestFocus() // Safe focus request after layout animation finishes
                }
                .start()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to parse local menu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
