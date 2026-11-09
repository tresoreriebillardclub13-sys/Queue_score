package com.example.server

import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {
    /**
     * Obtenir l'adresse IP IPv4 de l'appareil sur le réseau local Wi-Fi / Ethernet
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val hostAddress = addr.hostAddress ?: continue
                        val isIPv4 = hostAddress.indexOf(':') < 0
                        if (isIPv4 && (hostAddress.startsWith("192.168.") || hostAddress.startsWith("10.") || hostAddress.startsWith("172."))) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }
}
