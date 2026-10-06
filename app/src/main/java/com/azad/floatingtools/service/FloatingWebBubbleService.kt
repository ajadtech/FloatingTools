package com.azad.floatingtools.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import com.azad.floatingtools.R
import kotlin.math.abs

/**
 * একটা ওয়েবসাইট (যেমন YouTube) ফ্লোটিং উইন্ডোতে খোলে।
 *
 * তিনটা অবস্থা:
 * 1) EXPANDED — বড় উইন্ডো, পুরো WebView দেখা যায়, উপরে মাইনাস/ক্লোজ বাটন
 * 2) MINIMIZED (bubble) — ছোট গোল আইকন, কিন্তু WebView-টা window থেকে কখনো সরানো হয় না, তাই অডিও/ভিডিও চলতেই থাকে
 * 3) CLOSED — WebView সম্পূর্ণ remove, তখনই শুধু অডিও থামবে
 *
 * মূল কৌশল: মিনিমাইজ করার সময় WebView-কে windowManager থেকে removeView() করা হয় না,
 * শুধু তার প্যারেন্ট FrameLayout-এর সাইজ ছোট করে (1dp x 1dp এর কাছাকাছি, GONE না করে VISIBLE রেখে)
 * আর তার উপরে বাবল আইকনটা ওভারলে করে দেখানো হয়। এতে Chromium/WebView মনে করে সে এখনো স্ক্রিনে আছে,
 * তাই background media playback policy ট্রিগার হয় না।
 */
class FloatingWebBubbleService : Service() {

    private lateinit var windowManager: WindowManager

    private var windowView: View? = null       // পুরো উইন্ডো (টাইটেলবার + WebView)
    private var bubbleView: View? = null        // মিনিমাইজড বাবল
    private lateinit var windowParams: WindowManager.LayoutParams
    private lateinit var bubbleParams: WindowManager.LayoutParams

    private lateinit var webView: WebView
    private var isMinimized = false

    private var startUrl = "https://m.youtube.com"

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // যে অ্যাপে ক্লিক করা হয়েছে তার URL এখানে আসে; না থাকলে ডিফল্ট YouTube খুলবে
        intent?.getStringExtra(EXTRA_URL)?.let { startUrl = it }
        val title = intent?.getStringExtra(EXTRA_TITLE)
        if (windowView == null) {
            showExpandedWindow()
            title?.let { windowView?.findViewById<android.widget.TextView>(R.id.txtWindowTitle)?.text = it }
        }
        return START_NOT_STICKY
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }

    // ---------- EXPANDED WINDOW ----------

    private fun showExpandedWindow() {
        windowView = LayoutInflater.from(this).inflate(R.layout.view_floating_web_window, null)

        val webContainer = windowView!!.findViewById<FrameLayout>(R.id.webContainer)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // ব্যাকগ্রাউন্ডে অডিও/ভিডিও চালানোর জন্য জরুরি সেটিং
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            loadUrl(startUrl)
        }
        webContainer.addView(
            webView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            900,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowParams.gravity = Gravity.TOP or Gravity.START
        windowParams.x = 0
        windowParams.y = 200

        windowManager.addView(windowView, windowParams)

        // টাইটেলবার ধরে টেনে নড়ানো
        setupDrag(windowView!!.findViewById(R.id.dragHandle), windowParams, windowView!!)

        // মাইনাস বাটন → শুধু minimize, WebView কখনো remove হবে না
        windowView!!.findViewById<View>(R.id.btnMinimize).setOnClickListener {
            minimizeToBubble()
        }

        // ক্লোজ বাটন → সম্পূর্ণ বন্ধ, এখানেই অডিও থামবে
        windowView!!.findViewById<View>(R.id.btnClose).setOnClickListener {
            closeCompletely()
        }
    }

    // ---------- MINIMIZE (bubble) ----------

    private fun minimizeToBubble() {
        if (isMinimized) return
        isMinimized = true

        // উইন্ডোটা পুরোপুরি সরানো হচ্ছে না — শুধু প্রায়-অদৃশ্য ছোট সাইজে (1x1) নামিয়ে স্ক্রিনের বাইরে রাখা হচ্ছে,
        // যাতে WebView attached থাকে আর অডিও/ভিডিও চলতেই থাকে।
        windowParams.width = 1
        windowParams.height = 1
        windowParams.x = -10
        windowParams.y = -10
        windowManager.updateViewLayout(windowView, windowParams)

        showBubble()
    }

    private fun showBubble() {
        bubbleView = LayoutInflater.from(this).inflate(R.layout.view_floating_bubble, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        bubbleParams.gravity = Gravity.TOP or Gravity.START
        val metrics: DisplayMetrics = resources.displayMetrics
        bubbleParams.x = metrics.widthPixels - 180
        bubbleParams.y = 400

        windowManager.addView(bubbleView, bubbleParams)

        setupDrag(bubbleView!!, bubbleParams, bubbleView!!, onTap = {
            // বাবলে ট্যাপ করলে আবার বড় উইন্ডোতে ফিরিয়ে আনো
            expandFromBubble()
        })
    }

    private fun expandFromBubble() {
        if (!isMinimized) return
        isMinimized = false

        bubbleView?.let { windowManager.removeView(it) }
        bubbleView = null

        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.height = 900
        windowParams.x = 0
        windowParams.y = 200
        windowManager.updateViewLayout(windowView, windowParams)
    }

    // ---------- CLOSE ----------

    private fun closeCompletely() {
        bubbleView?.let { windowManager.removeView(it) }
        windowView?.let { windowManager.removeView(it) }
        stopSelf()
    }

    // ---------- DRAG HELPER (ট্যাপ ও ড্র্যাগ দুটোই হ্যান্ডেল করে) ----------

    private fun setupDrag(
        touchArea: View,
        params: WindowManager.LayoutParams,
        viewToMove: View,
        onTap: (() -> Unit)? = null
    ) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var dragging = false
        val threshold = 15

        touchArea.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (!dragging && (abs(dx) > threshold || abs(dy) > threshold)) dragging = true
                    if (dragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(viewToMove, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        v.performClick()
                        onTap?.invoke()
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        windowView?.let { runCatching { windowManager.removeView(it) } }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
