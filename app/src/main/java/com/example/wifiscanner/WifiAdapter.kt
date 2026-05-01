package com.example.wifiscanner

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WifiAdapter(private val onItemSelected: (ScanResult) -> Unit) :
    RecyclerView.Adapter<WifiAdapter.WifiViewHolder>() {

    private var wifiList: List<ScanResult> = emptyList()

    fun updateList(newList: List<ScanResult>) {
        wifiList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wifi, parent, false)
        return WifiViewHolder(view)
    }

    override fun onBindViewHolder(holder: WifiViewHolder, position: Int) {
        val result = wifiList[position]
        holder.bind(result)
        holder.itemView.setOnClickListener { onItemSelected(result) }
    }

    override fun getItemCount() = wifiList.size

    class WifiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSsid: TextView = view.findViewById(R.id.tvSsid)
        private val tvDetails: TextView = view.findViewById(R.id.tvDetails)

        fun bind(result: ScanResult) {
            tvSsid.text = if (result.SSID.isNullOrEmpty()) "Hidden Network" else result.SSID
            tvDetails.text = "BSSID: ${result.BSSID} | Signal: ${result.level}dBm"
        }
    }
}