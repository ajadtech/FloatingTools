package com.azad.floatingtools.data

/**
 * একটা অ্যাপ লিস্ট আইটেমের তথ্য রাখে।
 * iconRes: অ্যাপের আইকন (drawable বা mipmap রিসোর্স আইডি)
 * name: অ্যাপের নাম (যেমন "Chrome")
 * webUrl: এই অ্যাপের ওয়েব ভার্সন থাকলে তার লিংক, না থাকলে null
 * packageName: ফোনে ইনস্টল থাকা অ্যাপ হলে তার প্যাকেজ নেম (Your own apps / web app supported অংশে লাগবে)
 */
data class AppItem(
    val iconRes: Int,
    val name: String,
    val webUrl: String? = null,
    val packageName: String? = null
) {
    val hasWeb: Boolean get() = !webUrl.isNullOrBlank()
}
