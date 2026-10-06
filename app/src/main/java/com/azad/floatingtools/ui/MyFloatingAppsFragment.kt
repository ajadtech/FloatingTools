package com.azad.floatingtools.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.azad.floatingtools.R
import com.azad.floatingtools.adapter.AppListAdapter
import com.azad.floatingtools.data.AppItem
import com.azad.floatingtools.service.FloatingWebBubbleService

class MyFloatingAppsFragment : Fragment(R.layout.fragment_my_floating_apps) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // যেসব অ্যাপ ইউজার ফ্লোটিং বারে যোগ করেছে, সেগুলোর লিস্ট (এখন ডেমো ডেটা)
        val myFloatingApps = listOf(
            AppItem(R.mipmap.ic_launcher, "ChatGPT", webUrl = "https://chat.openai.com"),
            AppItem(R.mipmap.ic_launcher, "Chrome", webUrl = "https://www.google.com")
        )

        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerMyFloatingApps)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = AppListAdapter(myFloatingApps) { app ->
            if (app.hasWeb) openInFloatingWindow(app.name, app.webUrl!!)
        }

        view.findViewById<View>(R.id.imgTelegram).setOnClickListener {
            openLink(getString(R.string.telegram_url))
        }
        view.findViewById<View>(R.id.imgFacebook).setOnClickListener {
            openLink(getString(R.string.facebook_url))
        }
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    /** ওয়েব ভার্সন থাকা অ্যাপে ক্লিক করলে ফ্লোটিং উইন্ডোতে খুলে দেয় */
    private fun openInFloatingWindow(title: String, url: String) {
        val context = requireContext()
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
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
