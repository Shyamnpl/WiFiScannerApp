package com.example.wifiscanner

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wifiscanner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiAdapter: WifiAdapter
    private var connectivityManager: ConnectivityManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        setupRecyclerView()
        checkPermissions()

        binding.btnScan.setOnClickListener {
            startWifiScan()
        }
    }

    private fun setupRecyclerView() {
        wifiAdapter = WifiAdapter { scanResult ->
            showPasswordDialog(scanResult)
        }
        binding.rvWifiList.layoutManager = LinearLayoutManager(this)
        binding.rvWifiList.adapter = wifiAdapter
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )
        if (permissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }

    private fun startWifiScan() {
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        val success = wifiManager.startScan()
        if (!success) {
            Toast.makeText(this, "Scan failed. Please wait a moment.", Toast.LENGTH_SHORT).show()
        }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val results = if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                wifiManager.scanResults
            } else {
                emptyList()
            }
            wifiAdapter.updateList(results)
            unregisterReceiver(this)
        }
    }

    private fun showPasswordDialog(scanResult: ScanResult) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Connect to ${scanResult.SSID}")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Enter Password"
        builder.setView(input)

        builder.setPositiveButton("Connect") { _, _ ->
            connectToWifi(scanResult.SSID, input.text.toString())
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun connectToWifi(ssid: String, password: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    connectivityManager?.bindProcessToNetwork(network)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Connected to $ssid", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to connect to $ssid", Toast.LENGTH_LONG).show()
                    }
                }
            }
            connectivityManager?.requestNetwork(request, callback)
        } else {
            Toast.makeText(this, "This app requires Android 10+", Toast.LENGTH_SHORT).show()
        }
    }
}