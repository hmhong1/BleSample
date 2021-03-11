package com.example.blesample

import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import androidx.annotation.RequiresApi
import java.util.*


class BleConnUtil(var context: Context) {

    companion object {

        private const val TAG = "BleConnUtil"

        //forResult
        const val BT_REQUEST_ENABLE = 1000 //블루투스 활성화
        const val BT_REQUEST_CONNECT_DEVICE = 1001 //블루투스 기기 검색

        private const val SCAN_HANDLER_NAME = "SCAN_HANDLER_NAME"
        private const val SCAN_PERIOD: Long = 3000

        const val GATT_CONNECTED = "com.example.blesample.GATT_CONNECTED"
        const val GATT_DISCONNECTED = "com.example.blesample.GATT_DISCONNECTED"
        const val GATT_DATA_READ = "com.example.blesample.GATT_DATA_READ"
        const val GATT_DATA_WRITE = "com.example.blesample.GATT_DATA_WRITE"
        const val GATT_SERVICE_DISCOVERED = "com.example.blesample.GATT_SERVICE_DISCOVERED"
    }

    /* 블루투스 GATT 서버 */
    private var bluetoothGatt : BluetoothGatt? = null

    private var bleScanListDialog : BleScanListDialog? = null

    /* 블루투스 스캔시 사용하는 핸들러*/
    private var scanHandler : Handler? = null

    private var scanAlertDialog : AlertDialog? = null

    /* 블루투스 연결 여부 */
    var isConnected = false

    /* 블루투스 스캔여부 */
    var isScanning = false

    var scanDeviceList = ArrayList<BluetoothDevice>()

    init {
        isConnected = false
        isScanning = false

        scanDeviceList.clear()
    }

    /**
     * bluetooth adapter init
     * */
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    /**
     * 블루투스 LE를 지원하는 장비 인지 아닌지 확인 하는 함수
     *
     * false - 블루투스를 지원하지 않음
     */
    private fun checkSupportBluetoothLe(): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }
        return true
    }

    /**
     * 블루투스 활성화 여부
     * */
    fun isBluetoothEnable() : Boolean {
        bluetoothAdapter?.let {
            return it.isEnabled
        }
        return false
    }

    /**
     * 블루투스 비활성화
     * */
    fun bluetoothDisable() {
        var adapter = BluetoothAdapter.getDefaultAdapter()
        if(adapter.isEnabled)
            adapter.disable()
    }

    /**
     * 블루투스 스캔
     * */
    fun bluetoothScan(start: Boolean) {
        if(!checkSupportBluetoothLe())
            return

        bluetoothAdapter?.let {

            if(scanHandler == null)
                scanHandler = Handler(Looper.getMainLooper())

            when(start) {
                true -> {
                    if (isScanning)
                        return

                    scanHandler?.let {
                        it.postDelayed(runnable, SCAN_PERIOD)
                    }

                    scanDeviceList.clear()
                    bluetoothVerScanner(start)
                }
                else -> {
                    bluetoothVerScanner(start)
                }
            }

            isScanning = start
        }
    }

    /**
     * 스캔시 사용되는 runnable
     * */
    private val runnable = {
        isScanning = false
        bluetoothVerScanner(isScanning)
    }

    /**
     * 블루투스 스캔
     * 버전체크 후 실행
     *
     * true - 스캔시작
     * false - 스캔종료
     * */
    private fun bluetoothVerScanner(start: Boolean) {

        bluetoothAdapter?.let { it ->

            if(start) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    it.startLeScan(leScanCallback)
                } else {
                    it.bluetoothLeScanner.startScan(canCallback)
                }
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    it.stopLeScan(leScanCallback)
                } else {
                    it.bluetoothLeScanner.stopScan(canCallback)
                }

                showDeviceListDialog(context, scanDeviceList) { view ->
                    var position = view.tag as Int
                    var device = scanDeviceList[position]

                    device?.let {
                        connect(it)
                    }

                    if(bleScanListDialog != null)
                        bleScanListDialog!!.dismiss()
                }


                scanHandler?.let { h ->
                    h.removeCallbacks(runnable)
                }
            }

            if(start) {
                scanAlertDialogShow()
            } else {
                scanAlertDialogDissom()
            }


        }
    }

    private fun scanAlertDialogShow() {
        scanAlertDialog?.let {
            it.show()
        } ?: {
            var builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.Theme_AppCompat_Light_Dialog_Alert))
            builder.setMessage(context.resources.getString(R.string.bluetooth_searching))
            builder.setCancelable(false)
            scanAlertDialog = builder.create()
            scanAlertDialog!!.show()
        } ()
    }

    private fun scanAlertDialogDissom() {
        scanAlertDialog?.let {
            it.dismiss()
        }
    }

    /**
     * 검색된 디바이스 목록 추가
     * */
    private fun addDevice(device: BluetoothDevice?) {
        device?.let {
            if(!scanDeviceList.contains(it)) {

                var name : String? = it.name
                if(!name.isNullOrEmpty()) {
                    scanDeviceList.add(device)
                }
            }
        }
    }

    /**
     * 안드로이드 21 이하일경우
     * 블루투스 스캔 콜백
     * */
    private val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        addDevice(device)
    }

    /**
     * 안드로이드 21 이상일경우
     * 블루투스 스캔 콜백
     * */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val canCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            addDevice(result?.device)
        }
    }

    /**
     * 검색된 블루투스 목록 팝업 노출
     * */
    private fun showDeviceListDialog(
        cxt: Context,
        scanDeviceList: ArrayList<BluetoothDevice>,
        onClickListener: View.OnClickListener
    ) {
        if(bleScanListDialog == null)
            bleScanListDialog = BleScanListDialog(cxt, scanDeviceList, onClickListener)

        if(bleScanListDialog!!.isShowing)
            bleScanListDialog!!.dismiss()

        bleScanListDialog!!.show()
    }

    /**
     * 선택한 블루투스 연결하기
     * */
    private fun connect(device: BluetoothDevice) {
        if(bluetoothGatt != null) {
            close()
        }

        var searchFilter = IntentFilter()
        searchFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        searchFilter.addAction(GATT_CONNECTED)
        searchFilter.addAction(GATT_DISCONNECTED)
        searchFilter.addAction(GATT_DATA_READ)
        searchFilter.addAction(GATT_DATA_WRITE)
        searchFilter.addAction(GATT_SERVICE_DISCOVERED)
        context.registerReceiver(bluetoothSearchReceiver, searchFilter)

        bluetoothGatt = device.connectGatt(context, false, gattCallback)

    }

    /**
     * 블루투스 닫기
     * */
    fun close() {
        bluetoothGatt?.let {
            it.disconnect()
            it.close()
            bluetoothGatt = null
        }
    }

    /**
     * device connect gatt callback
     * */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            val intentAction: String

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    isConnected = true

                    intentAction = GATT_CONNECTED
                    broadcastUpdate(intentAction)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    isConnected = false

                    intentAction = GATT_DISCONNECTED
                    broadcastUpdate(intentAction)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(GATT_SERVICE_DISCOVERED)
                }
                else -> Log.w(TAG, "onServicesDiscovered not success : $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(GATT_DATA_READ, characteristic)
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic write success")
                characteristic?.let {
                    broadcastUpdate(GATT_DATA_WRITE, it)
                }
            } else {
                Log.d(TAG, "Characteristic write un success, status: $status")
                close()
            }
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        context.sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)
        context.sendBroadcast(intent)
    }

    /**
     * connect 할 때 달아준다
     * */
    private var bluetoothSearchReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d(TAG, "bluetoothSearchReceiver action : $action")

            when (action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val paired =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    paired?.let {
                        var bondState = it.bondState

                        if (bondState == BluetoothDevice.BOND_BONDING) {
                        } else if (bondState == BluetoothDevice.BOND_BONDED) {
                        }
                    }
                }

                GATT_CONNECTED -> {
                    Log.d(TAG, "GATT_CONNECTED")
                }
                GATT_DISCONNECTED -> {
                    Log.d(TAG, "GATT_DISCONNECTED")
                }
                GATT_DATA_READ -> {
                    Log.d(TAG, "GATT_DATA_READ")
                }
                GATT_DATA_WRITE -> {
                    Log.d(TAG, "GATT_DATA_WRITE")
                }
                GATT_SERVICE_DISCOVERED -> {
                    Log.d(TAG, "GATT_SERVICE_DISCOVERED")
                }
            }
        }
    }

}