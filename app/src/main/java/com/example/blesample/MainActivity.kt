package com.example.blesample

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.blesample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding : ActivityMainBinding

    private var bleConnUtil : BleConnUtil? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        listener()
    }

    private fun init() {
        bleConnUtil = BleConnUtil(this)
    }

    private fun listener() {
        binding.btnBleOn.setOnClickListener(this)
        binding.btnBleOff.setOnClickListener(this)
        binding.btnBleScan.setOnClickListener(this)
        binding.btnBleDisConnect.setOnClickListener(this)
    }

    /**
     * 블루투스 활성화 시키기
     * */
    private fun bluetoothEnable() {
        bleConnUtil?.let {

            if(it.isBluetoothEnable()) {
                //활성화
                toast(getString(R.string.bluetooth_enable))
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, BleConnUtil.BT_REQUEST_ENABLE)
            }
        } ?: {
            toast(getString(R.string.bluetooth_not_device))
        } ()
    }

    private fun toast(msg : String = "") {
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        bleConnUtil?.let {
            it.close()
        }
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            R.id.btnBleOn -> {
                //블루투스 활성화
                bluetoothEnable()
            }
            R.id.btnBleOff -> {
                //블루투스 비활성화
                bleConnUtil?.let {
                    it.bluetoothDisable()
                    toast(getString(R.string.bluetooth_disenable))
                }
            }
            R.id.btnBleScan -> {
                //블루투스 스캔
                bleConnUtil?.let {
                    if(!it.isBluetoothEnable()) {
                        toast(getString(R.string.bluetooth_on))
                        return
                    }
                    it.bluetoothScan(true)
                }
            }
            R.id.btnBleDisConnect -> {
                //블루투스 연결 끊기
                bleConnUtil?.let {
                    it.close()
                }
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            BleConnUtil.BT_REQUEST_ENABLE -> {
                // 블루투스 활성화
                toast(getString(R.string.bluetooth_enable))
            }
        }
    }
}