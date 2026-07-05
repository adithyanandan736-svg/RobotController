package com.robot.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Classic Bluetooth (SPP) connection — works with HC-05/HC-06 modules or an
 * ESP32 running Bluetooth Classic serial. Sends the same JSON command strings
 * used over WiFi, just through a Bluetooth socket instead of HTTP.
 *
 * Note: requires the phone to already be PAIRED with the module in Android's
 * Bluetooth settings before this will connect.
 */
class BluetoothHelper(private val onStatus: (String, Boolean) -> Unit) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun connectToPairedDevice(deviceName: String) {
        executor.execute {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null) {
                    mainHandler.post { onStatus("No Bluetooth adapter on this device", false) }
                    return@execute
                }
                val device: BluetoothDevice? = adapter.bondedDevices.firstOrNull { it.name == deviceName }
                if (device == null) {
                    mainHandler.post { onStatus("Device '$deviceName' not paired yet — pair it in phone Bluetooth settings first", false) }
                    return@execute
                }
                val sock = device.createRfcommSocketToServiceRecord(sppUuid)
                adapter.cancelDiscovery()
                sock.connect()
                socket = sock
                outputStream = sock.outputStream
                mainHandler.post { onStatus("Connected via Bluetooth to $deviceName", true) }
            } catch (e: IOException) {
                mainHandler.post { onStatus("Bluetooth connection failed: ${e.message}", false) }
            }
        }
    }

    fun send(jsonString: String) {
        executor.execute {
            try {
                outputStream?.write((jsonString + "\n").toByteArray(Charsets.UTF_8))
                outputStream?.flush()
            } catch (e: IOException) {
                mainHandler.post { onStatus("Send failed: ${e.message}", false) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun listPairedDeviceNames(): List<String> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices.map { it.name }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            // ignore on close
        }
    }
}
