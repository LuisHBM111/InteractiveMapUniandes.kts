package com.uniandes.interactivemapuniandes.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

// Live "sin conexion" banner. Sprint 3 feedback dijo que faltaba esto y que cuando
// aparecia, salia en un lugar que el usuario no podia leer. Lo metemos como overlay
// sobre android.R.id.content, anclado arriba con padding para status bar.
object ConnectivityBanner {

    private const val TAG_ID = 0x7f0bff01 // ID arbitrario pero estable para tag

    fun attach(activity: AppCompatActivity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (root.getTag(TAG_ID) != null) return // Ya estaba enganchado

        val banner = buildBanner(activity)
        root.addView(banner)
        banner.visibility = if (NetworkMonitor.isOnline(activity)) View.GONE else View.VISIBLE

        val cm = activity.getSystemService<ConnectivityManager>() ?: return
        val main = Handler(Looper.getMainLooper())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { main.post { banner.visibility = View.GONE } }
            override fun onLost(network: Network) { main.post { banner.visibility = View.VISIBLE } }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val ok = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                main.post { banner.visibility = if (ok) View.GONE else View.VISIBLE }
            }
        }
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, callback)

        root.setTag(TAG_ID, callback)

        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                runCatching { cm.unregisterNetworkCallback(callback) }
                root.setTag(TAG_ID, null)
            }
        })
    }

    private fun buildBanner(ctx: Context): TextView {
        val px = ctx.resources.displayMetrics.density
        val statusBarTop = (24 * px).toInt() // Aprox status bar height
        val pad = (12 * px).toInt()
        return TextView(ctx).apply {
            text = "Sin conexion - se muestra lo guardado"
            setBackgroundColor(Color.parseColor("#D32F2F"))
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(pad, statusBarTop + pad, pad, pad)
            elevation = 20f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )
        }
    }
}

// Atajo si se necesita desde un Activity comun (no AppCompatActivity).
fun Activity.attachConnectivityBanner() {
    if (this is AppCompatActivity) ConnectivityBanner.attach(this)
}
