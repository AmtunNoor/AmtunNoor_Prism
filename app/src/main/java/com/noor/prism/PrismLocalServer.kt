package com.noor.prism

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.FileInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Small loopback-only HTTP server for the verified Prism snapshot.
 *
 * Audio is deliberately served through a real HTTP socket instead of
 * WebView.shouldInterceptRequest(). Chromium can then perform its normal media
 * range requests and decode each MP3 from beginning to end without Android
 * fabricating a WebResourceResponse.
 */
class PrismLocalServer(private val snapshotManager: PrismSnapshotManager) : Closeable {

    private val running = AtomicBoolean(false)
    private val clients = Executors.newFixedThreadPool(CLIENT_THREADS) { runnable ->
        Thread(runnable, "PrismLocalHttpClient").apply { isDaemon = true }
    }
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null

    @Volatile
    private var port: Int = 0

    val baseUrl: String
        get() = "http://127.0.0.1:$port/"

    fun indexUrl(device: String): String = "${baseUrl}index.html?runtime=android&device=$device"

    @Synchronized
    fun start() {
        if (running.get()) return
        val socket = runCatching {
            ServerSocket(PREFERRED_PORT, BACKLOG, InetAddress.getByName(LOOPBACK))
        }.getOrElse {
            ServerSocket(0, BACKLOG, InetAddress.getByName(LOOPBACK))
        }
        serverSocket = socket
        port = socket.localPort
        running.set(true)
        acceptThread = Thread({ acceptLoop(socket) }, "PrismLocalHttpAccept").apply {
            isDaemon = true
            start()
        }
    }

    private fun acceptLoop(server: ServerSocket) {
        while (running.get()) {
            try {
                val client = server.accept()
                clients.execute { handle(client) }
            } catch (_: SocketException) {
                if (running.get()) continue else break
            } catch (_: Throwable) {
                if (!running.get()) break
            }
        }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = SOCKET_TIMEOUT_MS
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())
            try {
                val requestLine = readAsciiLine(input) ?: return
                val parts = requestLine.split(' ')
                if (parts.size < 2) return
                val method = parts[0].uppercase(Locale.US)
                val target = parts[1]
                val headers = linkedMapOf<String, String>()
                while (true) {
                    val line = readAsciiLine(input) ?: break
                    if (line.isEmpty()) break
                    val separator = line.indexOf(':')
                    if (separator > 0) {
                        headers[line.substring(0, separator).trim().lowercase(Locale.US)] =
                            line.substring(separator + 1).trim()
                    }
                }

                if (method == "OPTIONS") {
                    writeHeaders(output, 204, "No Content", mapOf(
                        "Access-Control-Allow-Origin" to "*",
                        "Access-Control-Allow-Methods" to "GET, HEAD, OPTIONS",
                        "Access-Control-Allow-Headers" to "Range, Content-Type",
                        "Content-Length" to "0"
                    ))
                    output.flush()
                    return
                }
                if (method != "GET" && method != "HEAD") {
                    sendText(output, 405, "Method Not Allowed", "Method not allowed", method == "HEAD")
                    return
                }

                val rawPath = runCatching { URI(target).rawPath }.getOrNull() ?: "/"
                val resolved = snapshotManager.localFileForPath(rawPath)
                if (resolved == null) {
                    sendText(output, 404, "Not Found", "Not found", method == "HEAD")
                    return
                }
                val (file, path) = resolved
                val generated = snapshotManager.bytesForLocalServer(file, path, baseUrl)
                if (generated != null) {
                    sendBytes(output, generated, snapshotManager.mimeTypeFor(path), method == "HEAD")
                    return
                }
                sendFile(output, file, snapshotManager.mimeTypeFor(path), headers["range"], method == "HEAD")
            } catch (_: Throwable) {
                runCatching { sendText(output, 500, "Internal Server Error", "Internal error", false) }
            } finally {
                runCatching { output.flush() }
            }
        }
    }

    private fun sendFile(
        output: BufferedOutputStream,
        file: java.io.File,
        mime: String,
        rangeHeader: String?,
        headOnly: Boolean
    ) {
        val length = file.length()
        when (val range = parseRange(rangeHeader, length)) {
            Range.None -> {
                writeHeaders(output, 200, "OK", commonHeaders(mime) + mapOf(
                    "Accept-Ranges" to "bytes",
                    "Content-Length" to length.toString()
                ))
                if (!headOnly) FileInputStream(file).use { copyExactly(it, output, length) }
            }
            Range.Unsatisfiable -> {
                writeHeaders(output, 416, "Range Not Satisfiable", commonHeaders(mime) + mapOf(
                    "Accept-Ranges" to "bytes",
                    "Content-Range" to "bytes */$length",
                    "Content-Length" to "0"
                ))
            }
            is Range.Valid -> {
                val count = range.end - range.start + 1
                writeHeaders(output, 206, "Partial Content", commonHeaders(mime) + mapOf(
                    "Accept-Ranges" to "bytes",
                    "Content-Range" to "bytes ${range.start}-${range.end}/$length",
                    "Content-Length" to count.toString()
                ))
                if (!headOnly) {
                    FileInputStream(file).use { stream ->
                        skipFully(stream, range.start)
                        copyExactly(stream, output, count)
                    }
                }
            }
        }
    }

    private fun sendBytes(output: BufferedOutputStream, bytes: ByteArray, mime: String, headOnly: Boolean) {
        writeHeaders(output, 200, "OK", commonHeaders(mime) + mapOf("Content-Length" to bytes.size.toString()))
        if (!headOnly) ByteArrayInputStream(bytes).use { copyExactly(it, output, bytes.size.toLong()) }
    }

    private fun sendText(output: BufferedOutputStream, code: Int, reason: String, text: String, headOnly: Boolean) {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        writeHeaders(output, code, reason, commonHeaders("text/plain; charset=utf-8") + mapOf("Content-Length" to bytes.size.toString()))
        if (!headOnly) output.write(bytes)
        output.flush()
    }

    private fun commonHeaders(mime: String): Map<String, String> = linkedMapOf(
        "Content-Type" to mime,
        "Access-Control-Allow-Origin" to "*",
        "Cache-Control" to "no-store",
        "Connection" to "close"
    )

    private fun writeHeaders(output: BufferedOutputStream, code: Int, reason: String, headers: Map<String, String>) {
        val builder = StringBuilder("HTTP/1.1 $code $reason\r\n")
        headers.forEach { (name, value) -> builder.append(name).append(": ").append(value).append("\r\n") }
        builder.append("\r\n")
        output.write(builder.toString().toByteArray(StandardCharsets.US_ASCII))
        output.flush()
    }

    private fun readAsciiLine(input: InputStream): String? {
        val bytes = ArrayList<Byte>(128)
        while (bytes.size < MAX_HEADER_LINE) {
            val value = input.read()
            if (value < 0) return if (bytes.isEmpty()) null else bytes.toByteArray().toString(StandardCharsets.US_ASCII)
            if (value == '\n'.code) break
            if (value != '\r'.code) bytes.add(value.toByte())
        }
        return bytes.toByteArray().toString(StandardCharsets.US_ASCII)
    }

    private sealed interface Range {
        data object None : Range
        data object Unsatisfiable : Range
        data class Valid(val start: Long, val end: Long) : Range
    }

    private fun parseRange(header: String?, length: Long): Range {
        if (header.isNullOrBlank()) return Range.None
        if (length <= 0 || !header.startsWith("bytes=", ignoreCase = true)) return Range.Unsatisfiable
        val value = header.substringAfter('=').substringBefore(',').trim()
        val dash = value.indexOf('-')
        if (dash < 0) return Range.Unsatisfiable
        val left = value.substring(0, dash).trim()
        val right = value.substring(dash + 1).trim()
        return try {
            when {
                left.isEmpty() -> {
                    val suffix = right.toLong()
                    if (suffix <= 0) Range.Unsatisfiable
                    else Range.Valid((length - suffix).coerceAtLeast(0), length - 1)
                }
                else -> {
                    val start = left.toLong()
                    if (start < 0 || start >= length) Range.Unsatisfiable
                    else {
                        val end = if (right.isEmpty()) length - 1 else right.toLong().coerceAtMost(length - 1)
                        if (end < start) Range.Unsatisfiable else Range.Valid(start, end)
                    }
                }
            }
        } catch (_: NumberFormatException) {
            Range.Unsatisfiable
        }
    }

    private fun skipFully(input: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) remaining -= skipped
            else if (input.read() < 0) break else remaining--
        }
    }

    private fun copyExactly(input: InputStream, output: BufferedOutputStream, bytes: Long) {
        val buffer = ByteArray(COPY_BUFFER)
        var remaining = bytes
        while (remaining > 0) {
            val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (count < 0) break
            if (count == 0) continue
            output.write(buffer, 0, count)
            remaining -= count
        }
        output.flush()
    }

    override fun close() {
        if (!running.compareAndSet(true, false)) return
        runCatching { serverSocket?.close() }
        acceptThread?.interrupt()
        clients.shutdownNow()
    }

    companion object {
        private const val LOOPBACK = "127.0.0.1"
        private const val PREFERRED_PORT = 18765
        private const val BACKLOG = 32
        private const val CLIENT_THREADS = 4
        private const val SOCKET_TIMEOUT_MS = 30_000
        private const val MAX_HEADER_LINE = 16_384
        private const val COPY_BUFFER = 64 * 1024
    }
}
