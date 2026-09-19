package com.example.vpn

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory

/**
 * Custom SocketFactory for JCIFS and network clients that automatically
 * protects sockets from Android VpnService TUN loops using VpnService.protect(socket).
 */
class VpnProtectedSocketFactory : SocketFactory() {

    override fun createSocket(): Socket {
        val socket = Socket()
        AppVpnService.protectSocket(socket)
        return socket
    }

    override fun createSocket(host: String?, port: Int): Socket {
        val socket = Socket()
        AppVpnService.protectSocket(socket)
        socket.connect(InetSocketAddress(host, port), 8000)
        return socket
    }

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
        val socket = Socket()
        AppVpnService.protectSocket(socket)
        if (localHost != null) {
            socket.bind(InetSocketAddress(localHost, localPort))
        }
        socket.connect(InetSocketAddress(host, port), 8000)
        return socket
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        val socket = Socket()
        AppVpnService.protectSocket(socket)
        socket.connect(InetSocketAddress(host, port), 8000)
        return socket
    }

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
        val socket = Socket()
        AppVpnService.protectSocket(socket)
        if (localAddress != null) {
            socket.bind(InetSocketAddress(localAddress, localPort))
        }
        socket.connect(InetSocketAddress(address, port), 8000)
        return socket
    }

    companion object {
        fun create(): SocketFactory = VpnProtectedSocketFactory()
    }
}
