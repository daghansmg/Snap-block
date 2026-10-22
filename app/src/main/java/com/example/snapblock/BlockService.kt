package com.example.snapblock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

class BlockService : AccessibilityService() {

    companion object {
        var instance: BlockService? = null
        var logger: ((String) -> Unit)? = null
        @Volatile var running = false
    }

    private data class Icon(val cx: Int, val cy: Int, val w: Int, val h: Int, val isX: Boolean)

    private val exec = Executors.newSingleThreadExecutor()
    private var lastShot = 0L

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

    private fun lum(c: Int) = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000

    @android.annotation.TargetApi(30)
    private fun shot(): Bitmap? {
        if (Build.VERSION.SDK_INT < 30) { log("Android 11 veya üstü gerekli."); return null }
        val wait = 1150 - (System.currentTimeMillis() - lastShot)
        if (wait > 0) Thread.sleep(wait)
        lastShot = System.currentTimeMillis()
        var result: Bitmap? = null
        val latch = CountDownLatch(1)
        takeScreenshot(Display.DEFAULT_DISPLAY, exec, object : TakeScreenshotCallback {
            override fun onSuccess(r: ScreenshotResult) {
                try {
                    val hb = r.hardwareBuffer
                    val bmp = Bitmap.wrapHardwareBuffer(hb, r.colorSpace)
                    result = bmp?.copy(Bitmap.Config.ARGB_8888, false)
                    hb.close()
                } catch (e: Exception) {}
                latch.countDown()
            }
            override fun onFailure(errorCode: Int) { latch.countDown() }
        })
        latch.await(5, TimeUnit.SECONDS)
        return result
    }

    private fun icons(b: Bitmap): List<Icon> {
        val w = b.width; val h = b.height
        val x0 = (w * 0.86).toInt(); val x1 = (w * 0.96).toInt()
        val y0 = (h * 0.12).toInt(); val y1 = (h * 0.76).toInt()
        val bx = (w * 0.80).toInt()
        val out = mutableListOf<Icon>()
        var top = -1; var gap = 0; var l = w; var r = 0; var lastInk = -1

        fun close(bottom: Int) {
            if (top >= 0) {
                val bh = bottom - top; val bw = r - l
                if (bh in 30..110 && bw in 30..110) {
                    val cx = (l + r) / 2; val cy = (top + bottom) / 2
                    val bg = lum(b.getPixel(bx, cy))
                    var sum = 0
                    for (dy in -2..2) for (dx in -2..2) sum += abs(lum(b.getPixel(cx + dx, cy + dy)) - bg)
                    out.add(Icon(cx, cy, bw, bh, sum / 25 > 60))
                }
            }
            top = -1; l = w; r = 0
        }

        for (y in y0..y1) {
            val bg = lum(b.getPixel(bx, y))
            var ink = false
            var x = x0
            while (x <= x1) {
                if (abs(lum(b.getPixel(x, y)) - bg) > 90) { ink = true; if (x < l) l = x; if (x > r) r = x }
                x += 2
            }
            if (ink) { if (top < 0) top = y; lastInk = y; gap = 0 }
            else if (top >= 0) { gap++; if (gap > 3) { close(lastInk); gap = 0 } }
        }
        close(lastInk)
        return out
    }

    private fun sig(b: Bitmap): Long {
        var s = 0L
        for (y in 0 until 40) for (x in 0 until 20) {
            val px = x * b.width / 20
            val py = (0.12 * b.height + y * (0.64 * b.height / 40)).toInt()
            s = s * 31 + lum(b.getPixel(px, py)) / 8
        }
        return s
    }

    private fun headerLum(b: Bitmap) = lum(b.getPixel((b.width * 0.30).toInt(), (b.height * 0.075).toInt()))
    private fun dialogShown(b: Bitmap) = lum(b.getPixel(b.width / 2, (b.height * 0.375).toInt())) > 235

    private fun tap(x: Int, y: Int) {
        val p = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 60)).build(), null, null)
    }

    private fun swipeUp() {
        val w = resources.displayMetrics.widthPixels
        val h = resources.displayMetrics.heightPixels
        val p = Path().apply { moveTo(w * 0.5f, h * 0.72f); lineTo(w * 0.5f, h * 0.32f) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 450)).build(), null, null)
    }

    private fun clickNode(n: AccessibilityNodeInfo) {
        var c: AccessibilityNodeInfo? = n
        while (c != null && !c.isClickable) c = c.parent
        if (c != null) { c.performAction(AccessibilityNodeInfo.ACTION_CLICK); return }
        val b = bounds(n)
        tap(b.centerX(), b.centerY())
    }

    private fun exact(root: AccessibilityNodeInfo?, text: String) =
        root?.findAccessibilityNodeInfosByText(text)?.filter { it.text?.toString()?.trim().equals(text, true) } ?: emptyList()

    fun diagnose() {
        log("6 sn içinde Snapchat > Sohbet ekranına geç...")
        Thread.sleep(6000)
        val root = rootInActiveWindow
        log("Paket=${root?.packageName} öğe=${all(root).size}")
        val b = shot()
        if (b == null) { log("Ekran görüntüsü alınamadı. Servisi kapatıp açmayı dene."); return }
        log("Ekran ${b.width}x${b.height}")
        val l = icons(b)
        if (l.isEmpty()) log("Hiç simge bulunamadı.")
        l.forEach { log("y=${it.cy} ${it.w}x${it.h} -> ${if (it.isX) "ÇARPI" else "kamera/diğer"}") }
        log("--- bitti ---")
    }

    fun startBlocking() {
        if (running) { log("Zaten çalışıyor."); return }
        running = true
        Thread { loop() }.start()
    }

    private fun loop() {
        var done = 0; var fails = 0; var same = 0; var shotFails = 0
        log("Başladı. Snapchat Sohbet ekranında kal.")
        while (running) {
            if (rootInActiveWindow?.packageName != "com.snapchat.android") { Thread.sleep(2000); continue }
            val b = shot()
            if (b == null) {
                if (++shotFails >= 4) { log("Ekran görüntüsü alınamıyor, durdum."); break }
                continue
            }
            shotFails = 0

            val x = icons(b).firstOrNull { it.isX }
            if (x == null) {
                swipeUp(); Thread.sleep(1500)
                val b2 = shot()
                if (b2 != null && sig(b2) == sig(b)) {
                    if (++same >= 3) { log("Liste sonu. Engellenen: $done"); break }
                } else same = 0
                continue
            }
            same = 0

            val base = headerLum(b)
            tap(x.cx, x.cy); Thread.sleep(1200)
            val b2 = shot()
            if (b2 == null || base - headerLum(b2) < 30) {
                log("Menü açılmadı.")
                if (++fails >= 3) { log("Üst üste hata, durdum."); break }
                continue
            }

            val n1 = exact(rootInActiveWindow, "Engelle").firstOrNull()
            if (n1 != null) clickNode(n1) else tap((b.width * 0.35).toInt(), (b.height * 0.697).toInt())
            Thread.sleep(1300)

            val b3 = shot()
            val nodeDialog = !rootInActiveWindow?.findAccessibilityNodeInfosByText("emin misin").isNullOrEmpty()
            if (!(nodeDialog || (b3 != null && dialogShown(b3)))) {
                log("Onay ekranı gelmedi.")
                performGlobalAction(GLOBAL_ACTION_BACK)
                if (++fails >= 3) { log("Üst üste hata, durdum."); break }
                Thread.sleep(1500); continue
            }

            val nb = exact(rootInActiveWindow, "Engelle").minByOrNull { bounds(it).top }
            if (nb != null) clickNode(nb) else tap(b.width / 2, (b.height * 0.518).toInt())
            Thread.sleep(1600)

            val b4 = shot()
            if (b4 != null && sig(b4) == sig(b)) {
                log("Engelleme görünmedi.")
                if (++fails >= 3) { log("Üst üste hata, durdum."); break }
            } else {
                done++; fails = 0
                log("Engellendi: $done")
            }
            Thread.sleep(Random.nextLong(15000, 40000))
        }
        running = false
        log("Durdu.")
    }
}
