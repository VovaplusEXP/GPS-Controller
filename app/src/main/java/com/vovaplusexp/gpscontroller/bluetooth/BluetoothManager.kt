package com.vovaplusexp.gpscontroller.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.vovaplusexp.gpscontroller.models.PeerEstimate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class BluetoothManager @Inject constructor(@ApplicationContext context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var peerDataListener: ((PeerEstimate) -> Unit)? = null

    private var serverJob: Job? = null
    private val clientJobs = mutableMapOf<String, Job>()
    private val connectedSockets = mutableMapOf<String, BluetoothSocket>()

    fun setPeerDataListener(listener: (PeerEstimate) -> Unit) {
        peerDataListener = listener
    }

    fun start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Timber.w("Bluetooth is not available or not enabled")
            return
        }
        startServer()
        connectToPairedDevices()
    }

    private fun startServer() {
        serverJob = scope.launch {
            var serverSocket: BluetoothServerSocket? = null
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
                Timber.i("Bluetooth server started")
                while (isActive) {
                    val socket = serverSocket?.accept()
                    socket?.let { manageConnectedSocket(it) }
                }
            } catch (e: IOException) {
                Timber.e(e, "Server socket error")
            } finally {
                serverSocket?.close()
            }
        }
    }

    private fun connectToPairedDevices() {
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            connectToDevice(device)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (clientJobs.containsKey(device.address)) return

        clientJobs[device.address] = scope.launch {
            try {
                val socket = device.createRfcommSocketToServiceRecord(MY_UUID)
                socket.connect()
                manageConnectedSocket(socket)
            } catch (e: IOException) {
                Timber.e("Failed to connect to ${device.name}")
                clientJobs.remove(device.address)
            }
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        val address = socket.remoteDevice.address
        connectedSockets[address] = socket
        Timber.i("Device connected: ${socket.remoteDevice.name}")

        scope.launch {
            try {
                val inputStream = socket.inputStream
                val buffer = ByteArray(PeerSyncProtocol.PACKET_SIZE)
                while (isActive) {
                    val bytes = inputStream.read(buffer)
                    if (bytes == PeerSyncProtocol.PACKET_SIZE) {
                        PeerSyncProtocol().parsePacket(buffer, socket.remoteDevice.address.hashCode().toLong())
                            ?.let { peerDataListener?.invoke(it) }
                    }
                }
            } catch (e: IOException) {
                Timber.e("Socket read error for ${socket.remoteDevice.name}")
            } finally {
                socket.close()
                connectedSockets.remove(address)
                clientJobs.remove(address)
                Timber.i("Device disconnected: ${socket.remoteDevice.name}")
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        clientJobs.values.forEach { it.cancel() }
        connectedSockets.values.forEach { it.close() }
        clientJobs.clear()
        connectedSockets.clear()
        scope.cancel()
        Timber.i("Bluetooth Manager stopped")
    }

    companion object {
        private const val NAME = "GPSController"
        private val MY_UUID: UUID = UUID.fromString("8e7c3c5c-3e3a-4f5f-8c3c-3e3a4f5f8c3c")
    }
}
