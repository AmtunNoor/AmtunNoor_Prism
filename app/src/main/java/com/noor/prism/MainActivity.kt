package com.noor.prism

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuAdapter
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        adapter = MenuAdapter(emptyList()) { menuModel ->
            val intent = Intent(this, WebViewActivity::class.java).apply {
                putExtra("URL", menuModel.url)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        syncAndLoadDashboard()
    }

    private fun syncAndLoadDashboard() {
        val masterMenuUrl = "https://raw.githubusercontent.com/AmtunNoor/Quran/main/menu.json"
        val request = Request.Builder().url(masterMenuUrl).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { loadOfflineFallback() }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    try {
                        val remoteData: List<RemoteMenuBlueprint> = Gson().fromJson(
                            jsonString,
                            object : TypeToken<List<RemoteMenuBlueprint>>(){}.type
                        )

                        // Bulk download every single page and audio file to the local disk safely
                        for (blueprint in remoteData) {
                            blueprint.syncFiles?.forEach { fileInfo ->
                                downloadFile(fileInfo.url, fileInfo.path)
                            }
                        }

                        runOnUiThread {
                            // Map to internal paths for our WebView to consume safely offline
                            val dashboardItems = remoteData.map { MenuModel(it.title, it.mainUrl) }
                            displayMenu(dashboardItems)
                        }
                    } catch (e: Exception) {
                        runOnUiThread { loadOfflineFallback() }
                    }
                }
            }
        })
    }

    private fun downloadFile(url: String, relativePath: String) {
        val targetFile = File(filesDir, relativePath)
        if (targetFile.exists()) return // Skip downloading if the file is already safely cached

        targetFile.parentFile?.mkdirs()
        val request = Request.Builder().url(url).build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        })
    }

    private fun displayMenu(items: List<MenuModel>) {
        adapter.updateData(items)
        val alphaIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in).apply { duration = 700 }
        recyclerView.layoutAnimation = LayoutAnimationController(alphaIn, 0.25f)
        recyclerView.startLayoutAnimation()
        recyclerView.requestFocus()
    }

    private fun loadOfflineFallback() {
        Toast.makeText(this, "Running completely offline", Toast.LENGTH_SHORT).show()
        val fallbackList = listOf(
            MenuModel("Quran", "file:///data/user/0/com.noor.prism/files/quran/index.html?id=A1"),
            MenuModel("Salah", "file:///data/user/0/com.noor.prism/files/salah_index.html")
        )
        displayMenu(fallbackList)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

data class RemoteMenuBlueprint(
    val title: String,
    val mainUrl: String,
    val syncFiles: List<SyncFileItem>?
)

data class SyncFileItem(
    val url: String,
    val path: String
)
