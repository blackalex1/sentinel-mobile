package com.xprox.sentinel.service

import android.content.Context
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

class CaptureProxyServer(
    private val context: Context,
    private val listenPort: Int,
    private val targetSocksPort: Int,
    private val targetSocksUsername: String,
    private val targetSocksToken: String
) {
    companion object {
        private const val TAG = "CaptureProxyServer"
    }

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val executor = Executors.newCachedThreadPool()

    fun start() {
        isRunning = true
        try {
            serverSocket = ServerSocket(listenPort, 50, InetAddress.getByName("127.0.0.1"))
            Log.i(TAG, "CaptureProxyServer started on 127.0.0.1:$listenPort")
            thread(name = "capture-proxy-accept") {
                while (isRunning) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        executor.execute { handleClient(socket) }
                    } catch (e: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start CaptureProxyServer", e)
        }
    }

    private fun handleClient(clientSocket: Socket) {
        try {
            val clientIn = java.io.DataInputStream(clientSocket.getInputStream())
            val clientOut = clientSocket.getOutputStream()

            // 1. SOCKS5 Method Selection Handshake
            val version = clientIn.read()
            if (version != 5) {
                clientSocket.close()
                return
            }
            val numMethods = clientIn.read()
            if (numMethods <= 0) {
                clientSocket.close()
                return
            }
            val methods = ByteArray(numMethods)
            clientIn.readFully(methods)

            // We require Username/Password authentication (0x02) to receive the package name from Xray
            if (!methods.contains(0x02.toByte())) {
                // Fail if client doesn't support Username/Password authentication
                clientOut.write(byteArrayOf(0x05.toByte(), 0xFF.toByte()))
                clientSocket.close()
                return
            }
            clientOut.write(byteArrayOf(0x05.toByte(), 0x02.toByte()))

            // 2. SOCKS5 Authentication
            val authVersion = clientIn.read()
            if (authVersion != 1) {
                clientSocket.close()
                return
            }
            val userLen = clientIn.read()
            if (userLen <= 0) {
                clientSocket.close()
                return
            }
            val userBytes = ByteArray(userLen)
            clientIn.readFully(userBytes)
            val packageName = String(userBytes, Charsets.UTF_8)

            val passLen = clientIn.read()
            if (passLen > 0) {
                val passBytes = ByteArray(passLen)
                clientIn.readFully(passBytes)
            }

            // Accept any password since this is purely a local loopback bridge
            clientOut.write(byteArrayOf(0x01.toByte(), 0x00.toByte()))

            // 3. SOCKS5 Request
            val reqVer = clientIn.read()
            val cmd = clientIn.read()
            clientIn.read() // RSV
            val atyp = clientIn.read()

            if (reqVer != 5 || cmd != 1) { // CONNECT command only
                clientSocket.close()
                return
            }

            val addressBytes: ByteArray
            val destIp: String
            when (atyp) {
                0x01 -> { // IPv4
                    addressBytes = ByteArray(4)
                    clientIn.readFully(addressBytes)
                    destIp = addressBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
                }
                0x03 -> { // Domain name
                    val domainLen = clientIn.read()
                    if (domainLen < 0) {
                        clientSocket.close()
                        return
                    }
                    val domainBytes = ByteArray(domainLen)
                    clientIn.readFully(domainBytes)
                    val domainName = String(domainBytes, Charsets.UTF_8)
                    addressBytes = byteArrayOf(0x03.toByte()) + byteArrayOf(domainLen.toByte()) + domainBytes
                    
                    // Resolve domain to IP for valid PCAP header synthesis
                    destIp = try {
                        InetAddress.getByName(domainName).hostAddress ?: "8.8.8.8"
                    } catch (e: Exception) {
                        "8.8.8.8"
                    }
                }
                0x04 -> { // IPv6
                    addressBytes = ByteArray(16)
                    clientIn.readFully(addressBytes)
                    destIp = "2001:db8::1" // Placeholder IPv6
                }
                else -> {
                    clientSocket.close()
                    return
                }
            }

            val portBytes = ByteArray(2)
            clientIn.readFully(portBytes)
            val destPort = ((portBytes[0].toInt() and 0xFF) shl 8) or (portBytes[1].toInt() and 0xFF)

            // 4. Establish Forward connection to Xray's main SOCKS5 port
            val xraySocket = try {
                Socket("127.0.0.1", targetSocksPort)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to target SOCKS port: $targetSocksPort", e)
                clientSocket.close()
                return
            }

            val xrayIn = java.io.DataInputStream(xraySocket.getInputStream())
            val xrayOut = xraySocket.getOutputStream()

            // Perform handshake with Xray SOCKS inbound
            xrayOut.write(byteArrayOf(0x05.toByte(), 0x01.toByte(), 0x02.toByte())) // version 5, 1 method, username/password auth
            val xrayVer = xrayIn.read()
            val xrayMethod = xrayIn.read()
            if (xrayVer != 5 || xrayMethod != 2) {
                xraySocket.close()
                clientSocket.close()
                return
            }

            // Authenticate with Xray SOCKS inbound
            val uBytes = targetSocksUsername.toByteArray(Charsets.UTF_8)
            val pBytes = targetSocksToken.toByteArray(Charsets.UTF_8)
            xrayOut.write(byteArrayOf(0x01.toByte(), uBytes.size.toByte()) + uBytes + byteArrayOf(pBytes.size.toByte()) + pBytes)
            val authStatusVer = xrayIn.read()
            val authStatus = xrayIn.read()
            if (authStatusVer != 1 || authStatus != 0) {
                xraySocket.close()
                clientSocket.close()
                return
            }

            // Send CONNECT request to Xray SOCKS inbound
            xrayOut.write(byteArrayOf(0x05.toByte(), 0x01.toByte(), 0x00.toByte(), atyp.toByte()) + addressBytes + portBytes)
            val xrayRepVer = xrayIn.read()
            val xrayRep = xrayIn.read()
            xrayIn.read() // RSV
            val xrayRepAtyp = xrayIn.read()
            when (xrayRepAtyp) {
                0x01 -> xrayIn.readFully(ByteArray(4))
                0x03 -> {
                    val len = xrayIn.read()
                    if (len > 0) {
                        xrayIn.readFully(ByteArray(len))
                    }
                }
                0x04 -> xrayIn.readFully(ByteArray(16))
            }
            xrayIn.readFully(ByteArray(2)) // port

            if (xrayRepVer != 5 || xrayRep != 0) {
                xraySocket.close()
                clientOut.write(byteArrayOf(0x05.toByte(), 0x04.toByte(), 0x00.toByte(), 0x01.toByte(), 0, 0, 0, 0, 0, 0))
                clientSocket.close()
                return
            }

            // Send success response to client
            clientOut.write(byteArrayOf(0x05.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0, 0, 0, 0, 0, 0))

            // 5. Setup stateful TCP PCAP stream logs
            val clientPort = clientSocket.port
            val clientSeq = AtomicLong(1000L + (Math.random() * 10000).toLong())
            val targetSeq = AtomicLong(5000L + (Math.random() * 10000).toLong())

            val clientIp = "10.0.0.2" // Simulated tunnel source IP

            // Write simulated SYN packet
            PacketForensics.writeTcpPayloadToPcap(
                context = context,
                packageName = packageName,
                srcIp = clientIp,
                srcPort = clientPort,
                dstIp = destIp,
                dstPort = destPort,
                seq = clientSeq.get(),
                ack = 0,
                flags = 0x02.toByte(), // SYN
                payload = ByteArray(0),
                timestampMs = System.currentTimeMillis()
            )
            clientSeq.incrementAndGet()

            // Write simulated SYN-ACK packet
            PacketForensics.writeTcpPayloadToPcap(
                context = context,
                packageName = packageName,
                srcIp = destIp,
                srcPort = destPort,
                dstIp = clientIp,
                dstPort = clientPort,
                seq = targetSeq.get(),
                ack = clientSeq.get(),
                flags = 0x12.toByte(), // SYN|ACK
                payload = ByteArray(0),
                timestampMs = System.currentTimeMillis()
            )
            targetSeq.incrementAndGet()

            // Write simulated ACK packet
            PacketForensics.writeTcpPayloadToPcap(
                context = context,
                packageName = packageName,
                srcIp = clientIp,
                srcPort = clientPort,
                dstIp = destIp,
                dstPort = destPort,
                seq = clientSeq.get(),
                ack = targetSeq.get(),
                flags = 0x10.toByte(), // ACK
                payload = ByteArray(0),
                timestampMs = System.currentTimeMillis()
            )

            // 6. Start bidirectional copy. One direction runs as a background task in the executor pool,
            // the other runs directly on the current handler thread. This reduces thread count per connection from 3 to 2.
            val future = executor.submit {
                forwardData(
                    xrayIn, clientOut, packageName,
                    destIp, destPort, clientIp, clientPort,
                    targetSeq, clientSeq, isClient = false
                )
            }

            forwardData(
                clientIn, xrayOut, packageName,
                clientIp, clientPort, destIp, destPort,
                clientSeq, targetSeq, isClient = true
            )

            // Wait for the background forwarding task to complete
            try {
                future.get()
            } catch (e: Exception) {
                // Ignore
            }

            // Write simulated FIN packet from client
            PacketForensics.writeTcpPayloadToPcap(
                context = context,
                packageName = packageName,
                srcIp = clientIp,
                srcPort = clientPort,
                dstIp = destIp,
                dstPort = destPort,
                seq = clientSeq.get(),
                ack = targetSeq.get(),
                flags = 0x11.toByte(), // FIN|ACK
                payload = ByteArray(0),
                timestampMs = System.currentTimeMillis()
            )
            clientSeq.incrementAndGet()

            // Write simulated FIN-ACK from target
            PacketForensics.writeTcpPayloadToPcap(
                context = context,
                packageName = packageName,
                srcIp = destIp,
                srcPort = destPort,
                dstIp = clientIp,
                dstPort = clientPort,
                seq = targetSeq.get(),
                ack = clientSeq.get(),
                flags = 0x11.toByte(), // FIN|ACK
                payload = ByteArray(0),
                timestampMs = System.currentTimeMillis()
            )
            targetSeq.incrementAndGet()

            // Write final ACK
            PacketForensics.writeTcpPayloadToPcap(
                context = context,
                packageName = packageName,
                srcIp = clientIp,
                srcPort = clientPort,
                dstIp = destIp,
                dstPort = destPort,
                seq = clientSeq.get(),
                ack = targetSeq.get(),
                flags = 0x10.toByte(), // ACK
                payload = ByteArray(0),
                timestampMs = System.currentTimeMillis()
            )

            xraySocket.close()
            clientSocket.close()
        } catch (e: Exception) {
            try {
                clientSocket.close()
            } catch (ex: Exception) {}
        }
    }

    private fun forwardData(
        input: InputStream,
        output: OutputStream,
        packageName: String,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int,
        mySeq: AtomicLong,
        peerSeq: AtomicLong,
        isClient: Boolean
    ) {
        try {
            val buffer = ByteArray(16384)
            while (isRunning) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                if (bytesRead > 0) {
                    output.write(buffer, 0, bytesRead)
                    output.flush()

                    // Log payload to PCAP using correct current seq/ack numbers
                    val currentSeq = mySeq.get()
                    val currentAck = peerSeq.get()
                    
                    PacketForensics.writeTcpPayloadToPcap(
                        context = context,
                        packageName = packageName,
                        srcIp = srcIp,
                        srcPort = srcPort,
                        dstIp = dstIp,
                        dstPort = dstPort,
                        seq = currentSeq,
                        ack = currentAck,
                        flags = 0x18.toByte(), // PSH|ACK
                        payload = buffer,
                        payloadOffset = 0,
                        payloadLength = bytesRead,
                        timestampMs = System.currentTimeMillis()
                    )
                    
                    mySeq.addAndGet(bytesRead.toLong())
                }
            }
        } catch (e: Exception) {
            // Socket closed or read error, terminate forward loop
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        executor.shutdown()
        // Wait for active forwarding threads to finish so the port is freed
        // before a potential immediate restart of CaptureProxyServer
        try {
            executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
