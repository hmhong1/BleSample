package com.example.blesample

import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.blesample.databinding.BleSearchDeviceItemBinding
import com.example.blesample.databinding.DialogBleListBinding
import java.util.*

class BleScanListDialog(context: Context, var scanDeviceList: ArrayList<BluetoothDevice>, private val onClickListener: View.OnClickListener?) : Dialog(context) {

    private lateinit var binding : DialogBleListBinding

    private var adapter : BleScanListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogBleListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        binding.recyclerView.visibility = if(scanDeviceList.size > 0) View.VISIBLE else View.GONE
        binding.txSearchBleFail.visibility = if(scanDeviceList.size > 0) View.GONE else View.VISIBLE

        adapter = BleScanListAdapter(scanDeviceList, onClickListener)

        binding.recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.recyclerView.adapter = adapter

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    inner class BleScanListAdapter(private val list: MutableList<BluetoothDevice>, private val onClickListener: View.OnClickListener?) : RecyclerView.Adapter<BleScanListAdapter.ViewHodler>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHodler {
            var adapterBinding = BleSearchDeviceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHodler(adapterBinding)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: ViewHodler, position: Int) {
            var deviceData = list[position]

            if(deviceData != null) {
                holder.itemViewBinding.txDeviceName.text = deviceData.name
                holder.itemViewBinding.txDeviceAddress.text = deviceData.address

                onClickListener?.let {
                    holder.itemViewBinding.clDeviceItem.tag = position
                    holder.itemViewBinding.clDeviceItem.setOnClickListener(it)
                }
            }
        }

        inner class ViewHodler(var itemViewBinding: BleSearchDeviceItemBinding) : RecyclerView.ViewHolder(itemViewBinding.root)
    }
}