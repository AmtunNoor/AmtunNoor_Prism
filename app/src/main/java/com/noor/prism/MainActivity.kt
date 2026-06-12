package com.noor.prism

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        // Renders your beautiful dashboard cards horizontally across the screen
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        adapter = MenuAdapter(emptyList()) { menuModel ->
            try {
                // BYPASSES BUGGY WEBVIEW SANDBOX: Launches the link directly into the working browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(menuModel.url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No browser found to open link", Toast.LENGTH_SHORT).show()
            }
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

            adapter.updateData(menuItems)
            recyclerView.requestFocus()

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
