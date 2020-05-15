package com.trials.samplesocket.network

import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket


class MyServerSocket(private val port: Int) {

    private lateinit var serverSocket: ServerSocket
    private lateinit var job: Job

    fun start() {
        if (::job.isInitialized) if (job.isActive) job.cancel()
        serverSocket = ServerSocket(port)
        val state = "hello from server\n"
        var inputStream: BufferedReader? = null
        var outputStream: PrintWriter? = null
        job = GlobalScope.launch(Dispatchers.IO) {
            try {
                val socket = serverSocket.accept()
                Log.d(MyServerSocket::class.java.simpleName, "connected")
                inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
                outputStream = PrintWriter(socket.getOutputStream(), true)
                this.launch {
                    withContext(Dispatchers.IO) {
                        while (isActive) {
                            val mes = inputStream?.readLine()
                            if (mes == null) {
                                close()
                                break
                            }
                            Log.d(MyServerSocket::class.java.simpleName, "received -> $mes")
                            delay(1000L)
                        }
                    }
                }
                this.launch {
                    withContext(Dispatchers.IO) {
                        while (isActive) {
                            outputStream!!.write(state)
                            outputStream?.flush()
                            Log.d(MyServerSocket::class.java.simpleName, "sent -> $state")
                            delay(1000L)
                        }
                    }
                }
            } catch (e: Exception) {
                close()
            }
        }
    }

    fun close() {
        if (::job.isInitialized) if (job.isActive) job.cancel()
        try {
            if (::serverSocket.isInitialized) serverSocket.close()
        } catch (e: IOException) {
        }
    }
}