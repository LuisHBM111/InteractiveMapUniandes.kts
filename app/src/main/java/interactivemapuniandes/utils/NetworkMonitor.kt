package com.uniandes.interactivemapuniandes.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// Cheap one-shot connectivity check. Sprint 3 wants a "no internet" banner.
object NetworkMonitor {
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
