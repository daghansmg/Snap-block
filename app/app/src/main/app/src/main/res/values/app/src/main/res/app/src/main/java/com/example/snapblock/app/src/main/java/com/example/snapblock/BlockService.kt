package com.example.snapblock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.random.Random

class BlockService : AccessibilityService() {

    companion object {
        var instance: BlockService? = null
        var logger: ((String) -> Unit)? = null
        @Volatile var running = false
        private val X_WORDS = listOf("kapat", "close", "çarpı", "carpi", "remove", "kaldır", "dismiss", "iptal", "cancel", "clear", "reddet", "reject")
        private val NOT_X = listOf("kamera", "camera")
    }

    override fun onServiceConnected() { instance = this; log("Servis hazır.") }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean { running = false; instance = null; return super.onUnbind(intent) }

    fun log(s: String) { Handler(Looper.getMainLooper()).post { logger?.invoke(s) } }

    private fun all(n: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo> = mutableListOf()): MutableList<AccessibilityNodeInfo> {
        if (n == null) return out
        out.add(n)
        for (i in 0 until n.childCount) all(n.getChild(i), out)
        return out
    }

    private fun bounds(n: AccessibilityNodeInfo) = Rect().also { n.getBoundsInScreen(it) }

    private fun rightNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val w = resources.displayMetrics.widthPixels
        val h = resources.displayMetrics.heightPixels
        return all(root).filter {
            val b = bounds(it)
            it.isVisibleToUser && b.centerX() > w * 0.8 && b.centerY() > h * 0.12 && b.centerY() < h * 0.88 && b.width() > 0
        }
    }

    private fun isX(n: AccessibilityNodeInfo): Boolean {
        val d = (n.contentDescription ?: "").toString().lowercase()
        val t = (n.text ?: "").toString().lowercase()
        val id = (n.viewIdResourceName ?: "").lowercase()
        val s = "$d $t $id"
        if (NOT_X.any { s.contains(it) }) return false
        return d == "x" || t == "x" || X_WORDS.any { s.contains(it) }
    }

    fun diagnose() {
        val root = rootInActiveWindow
        if (root == null) { log("Ekran okunamadı. Snapchat Sohbet ekranını aç."); return }
        log("--- Sağ taraftaki öğeler ---")
        rightNodes(root).forEach {
            val b = bounds(it)
            log("${it.className} | desc=${it.contentDescription} | text=${it.text} | id=${it.viewIdResourceName} | tık=${it.isClickable} | y=${b.centerY()} | X?=${isX(it)}")
        }
        log("--- bitti ---")
    }

    private fun clickNode(n: AccessibilityNodeInfo) {
        var c: AccessibilityNodeInfo? = n
        while (c != null && !c.isClickable) c = c.parent
        if (c != null) { c.performAction(AccessibilityNodeInfo.ACTION_CLICK); return }
        val b = bounds(n)
        val p = Path().apply { moveTo(b.centerX().toFloat(), b.centerY().toFloat()) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 60)).build(), null, null)
    }

    private fun exact(root: AccessibilityNodeInfo?, text: String) =
        root?.findAccessibilityNodeInfosByText(text)?.filter { it.text?.toString()?.trim().equals(text, true) } ?: emptyList()

    private fun scrollDown(root: AccessibilityNodeInfo) {
        all(root).firstOrNull { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun startBlocking() {
        if (running) { log("Zaten çalışıyor."); return }
        running = true
        Thread { loop() }.start()
    }

    private fun loop() {
        var done = 0; var fails = 0; var noX = 0
        log("Başladı. Snapchat Sohbet ekranında kal.")
        while (running) {
            val root = rootInActiveWindow
            if (root == null || root.packageName != "com.snapchat.android") { Thread.sleep(2000); continue }

            val x = rightNodes(root).firstOrNull { isX(it) }
            if (x == null) {
                if (++noX >= 4) { log("Çarpı bulunamadı/kalmadı. Engellenen: $done"); break }
                scrollDown(root); Thread.sleep(1500); continue
            }
            noX = 0

            clickNode(x); Thread.sleep(1300)
            val b1 = exact(rootInActiveWindow, "Engelle").firstOrNull()
            if (b1 == null) {
                log("Menü açılmadı."); performGlobalAction(GLOBAL_ACTION_BACK)
                if (++fails >= 3) { log("Üst üste hata, durdum."); break }
                Thread.sleep(1500); continue
            }
            clickNode(b1); Thread.sleep(1300)

            val r3 = rootInActiveWindow
            val confirmShown = !r3?.findAccessibilityNodeInfosByText("emin misin").isNullOrEmpty()
            val btn = exact(r3, "Engelle").minByOrNull { bounds(it).top }
            if (!confirmShown || btn == null) {
                log("Onay ekranı gelmedi."); performGlobalAction(GLOBAL_ACTION_BACK)
                if (++fails >= 3) { log("Üst üste hata, durdum."); break }
                Thread.sleep(1500); continue
            }
            clickNode(btn)
            done++; fails = 0
            log("Engellendi: $done")
            Thread.sleep(Random.nextLong(15000, 40000))
        }
        running = false
        log("Durdu.")
    }
}
