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
import android.widget.LinearLayout
import com.azad.floatingtools.R
import kotlin.math.abs

/**
 * ফোনের উপরে ভাসমান ফ্লোটিং বার দেখায়।
 * - সাধারণ ট্যাপ করলে বারটা খোলে/বন্ধ হয় (আগের মতোই কাজ করবে)
 * - প্রেস করে ধরে রেখে টানলে বারটা স্ক্রিনের যেকোনো জায়গায়, মাঝখানেও আনা যাবে
 */
class FloatingBarService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showFloatingBar()
    }

    private fun showFloatingBar() {
        // আপাতত একটা সাধারণ ছোট বাটন/বার হিসেবে দেখাচ্ছি — আসল লেআউট তোমার পুরনো ডিজাইন অনুযায়ী বসাবে
        floatingView = LayoutInflater.from(this).inflate(R.layout.view_floating_bar, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 300

        windowManager.addView(floatingView, params)
        setupTouchAndDrag(floatingView!!)
    }

    private fun setupTouchAndDrag(view: View) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var isDragging = false
        val dragThreshold = 15 // এর কম নড়াচড়া হলে এটা ট্যাপ হিসেবে গণ্য হবে

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()

                    if (!isDragging && (abs(dx) > dragThreshold || abs(dy) > dragThreshold)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // ট্যাপ — বারটা খোলা/বন্ধ করার আগের লজিক এখানে কল হবে
                        v.performClick()
                        toggleBarExpanded()
                    } else {
                        // ড্র্যাগ শেষ — চাইলে স্ক্রিনের মাঝে/কিনারায় স্ন্যাপ করানো যায়
                        snapToNearestEdgeOrCenter(view)
                    }
                    true
                }
                else -> false
            }
        }
    }

    /** আগে থেকে যেভাবে ট্যাপে বার খোলে/বন্ধ হয়, সেই লজিক এখানে বসবে */
    private fun toggleBarExpanded() {
        // TODO: তোমার পুরনো প্রজেক্টের ট্যাপ-ওপেন লজিক এখানে কল কর
    }

    /** ছেড়ে দেওয়ার পর বারটা স্ক্রিনের মাঝখানে বা কাছের কিনারায় নিয়ে যেতে চাইলে এই ফাংশন ব্যবহার কর */
    private fun snapToNearestEdgeOrCenter(view: View) {
        val metrics: DisplayMetrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenCenterX = screenWidth / 2 - view.width / 2

        // এখানে params.x কে আস্তে আস্তে screenCenterX-এর দিকে নিয়ে যাওয়ার এনিমেশন বসানো যায়
        params.x = screenCenterX
        windowManager.updateViewLayout(view, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
