package com.azad.floatingtools.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.azad.floatingtools.R
import com.azad.floatingtools.adapter.AppListAdapter
import com.azad.floatingtools.data.AppItem
import com.azad.floatingtools.service.FloatingWebBubbleService

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // সেকশন ১: Your own apps — ফোনে ইনস্টল থাকা অ্যাপের লিস্ট (এখন ডেমো ডেটা, পরে ফোন থেকে রিয়েল লিস্ট আনা যাবে)
        val yourApps = listOf(
            AppItem(R.mipmap.ic_launcher, "8 Ball Pool"),
            AppItem(R.mipmap.ic_launcher, "Calculator"),
            AppItem(R.mipmap.ic_launcher, "Camera")
        )

        // সেকশন ২: All online apps — জনপ্রিয় অ্যাপের ওয়েব ভার্সন
        val allOnlineApps = listOf(
            AppItem(R.mipmap.ic_launcher, "ChatGPT", webUrl = "https://chat.openai.com"),
            AppItem(R.mipmap.ic_launcher, "Chrome", webUrl = "https://www.google.com"),
            AppItem(R.mipmap.ic_launcher, "Facebook", webUrl = "https://www.facebook.com")
        )

        // সেকশন ৩: Your own web app supported — শুধু যেগুলোর web version আছে ও ফোনে ইনস্টল আছে
        val webSupportedApps = allOnlineApps.filter { it.hasWeb }

        setupSection(view.findViewById(R.id.recyclerYourApps), yourApps)
        setupSection(view.findViewById(R.id.recyclerAllOnline), allOnlineApps)
        setupSection(view.findViewById(R.id.recyclerWebSupported), webSupportedApps)
    }

    private fun setupSection(recyclerView: RecyclerView, items: List<AppItem>) {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = AppListAdapter(items) { app ->
            if (app.hasWeb) openInFloatingWindow(app.name, app.webUrl!!)
        }
    }

    /** ওয়েব ভার্সন থাকা অ্যাপে ক্লিক করলে এটা ফ্লোটিং উইন্ডোতে খুলে দেয় */
    private fun openInFloatingWindow(title: String, url: String) {
        val context = requireContext()
        if (!Settings.canDrawOverlays(context)) {
            // ওভারলে পারমিশন না থাকলে প্রথমে সেটা চাইতে হবে
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            startActivity(intent)
            return
        }
        val serviceIntent = Intent(context, FloatingWebBubbleService::class.java).apply {
            putExtra(FloatingWebBubbleService.EXTRA_URL, url)
            putExtra(FloatingWebBubbleService.EXTRA_TITLE, title)
        }
        context.startService(serviceIntent)
    }
}
