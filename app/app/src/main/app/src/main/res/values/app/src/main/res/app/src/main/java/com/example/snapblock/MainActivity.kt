package com.example.snapblock

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() } }.also { root.addView(it) }

        btn("1) Erişilebilirlik ayarlarını aç (SnapBlock'u etkinleştir)") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btn("2) Tara (teşhis: sağdaki öğeleri listeler)") {
            val s = BlockService.instance
            if (s == null) log("Servis kapalı. Önce 1. adımı yap.") else Thread { s.diagnose() }.start()
        }
        btn("3) BAŞLAT (önce Snapchat > Sohbet ekranını aç)") {
            val s = BlockService.instance
            if (s == null) log("Servis kapalı. Önce 1. adımı yap.") else s.startBlocking()
        }
        btn("DURDUR") { BlockService.running = false; log("Durduruluyor...") }

        logView = TextView(this).apply { textSize = 12f; setTextIsSelectable(true) }
        root.addView(ScrollView(this).apply { addView(logView) })
        setContentView(root)
        BlockService.logger = { log(it) }
    }

    private fun log(s: String) { logView.append(s + "\n") }
}
