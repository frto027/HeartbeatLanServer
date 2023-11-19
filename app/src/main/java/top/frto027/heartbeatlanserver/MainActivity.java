package top.frto027.heartbeatlanserver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    LinearLayoutCompat bluetoothScrollView;
    BluetoothCardView[] views;
    Handler handler = new Handler();

    Button broadcastToggleBtn;

    final static String HEART_UUID = "00002a37-0000-1000-8000-00805f9b34fb";

    boolean isResume = true;

    @Override
    protected void onPause() {
        super.onPause();
        isResume = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResume = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HeartDeviceServerThread.close();
    }

    class BluetoothCardView {
        BluetoothDevice dev;

        View root = null;

        TextView nameTv;

        Button con_discon_btn;

        boolean connected = false;

        public BluetoothCardView(BluetoothDevice dev) {
            this.dev = dev;
            root = View.inflate(MainActivity.this, R.layout.bluetooth_card, null);
            nameTv = root.findViewById(R.id.dev_name_tv);
            con_discon_btn = root.findViewById(R.id.connect_disconnect_btn);
            con_discon_btn.setOnClickListener((e) -> ToggleStatus());
            Update();
        }

        BluetoothGatt gatt;

        HeartDeviceStatus devStatus = new HeartDeviceStatus();
        class BluetoothGattCb extends BluetoothGattCallback {
            BluetoothGatt gatt;

            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
                super.onCharacteristicChanged(gatt, characteristic, value);
                if (HEART_UUID.equals(characteristic.getUuid().toString())) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    int flag = characteristic.getProperties();

                    int format = -1;
                    if ((flag & 0x01) != 0) {
                        format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    } else {
                        format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    }
                    devStatus.heartRate = characteristic.getIntValue(format, 1);
                    HeartDeviceServerThread.getInstance().informDevStatus(devStatus);
                    TriggerUpdate();
                }

            }
/*
            @Override
            public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
                super.onCharacteristicRead(gatt, characteristic, value, status);
                if (HEART_UUID.equals(characteristic.getUuid())) {
                    handler.post(BluetoothCardView.this::Update);
                    handler.postDelayed(() -> {
                        if (gatt == BluetoothCardView.this.gatt) {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            gatt.readCharacteristic(characteristic);
                        }
                    }, 100);
                }
            }
*/
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    gatt.discoverServices();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (gatt != BluetoothCardView.this.gatt)
                    return;
                for (BluetoothGattService serv : gatt.getServices()) {
                    for (BluetoothGattCharacteristic ch : serv.getCharacteristics()) {
                        if (HEART_UUID.equals(ch.getUuid().toString())) {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                throw new RuntimeException("not work");
                            }
                            gatt.setCharacteristicNotification(ch, true);
                            BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                }
                TriggerUpdate();
            }
        }

        void ToggleStatus() {
            connected = !connected;
            if (connected) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    throw new RuntimeException("no permission");
                }
                BluetoothGattCb cb = new BluetoothGattCb();
                gatt = dev.connectGatt(MainActivity.this, true, cb);
                cb.gatt = gatt;
                //gatt.discoverServices();
            }else{
                gatt.close();
                gatt = null;
            }
            Update();
        }

        void TriggerUpdate(){
            if(isResume)
                handler.post(this::Update);
        }
        void Update(){
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                throw new RuntimeException("permission mess!");
            }
            devStatus.name = dev.getName();
            devStatus.address = dev.getAddress();

            /*
            String services = "\n";
            if(gatt != null){
                for(BluetoothGattService serv : gatt.getServices()){
                    services += "serve:\n";
                    for (BluetoothGattCharacteristic ch :
                            serv.getCharacteristics()) {
                        services += "  " + ch.getUuid().toString() + "\n";
                    }
                }
            }
            */
            //Log.d("BLE_GETT", services);
            //nameTv.setText("Device name: " + name + " heart: " + devStatus.heartRate + " type: " + dev.getType() + "\naddr:" + dev.getAddress() + services);
            nameTv.setText(devStatus.toString());

            if(connected){
                con_discon_btn.setText("断开");
            }else{
                con_discon_btn.setText("连接");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothScrollView = findViewById(R.id.main_activity_scrollview);
        broadcastToggleBtn = findViewById(R.id.broadcast_toggle_btn);

        broadcastToggleBtn.setText("正在匹配新设备，点击停止");
        broadcastToggleBtn.setOnClickListener((e)->{
            HeartDeviceServerThread.enableBroadcast = !HeartDeviceServerThread.enableBroadcast;
            if(HeartDeviceServerThread.enableBroadcast){
                broadcastToggleBtn.setText("正在匹配新设备，点击停止");
            }else{
                broadcastToggleBtn.setText("已停止继续匹配，点击开始");
            }
        });

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }


        HeartDeviceServerThread.getInstance();
        FlushBluetoothDevices();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            HeartDeviceServerThread.getInstance();
            FlushBluetoothDevices();
        }
    }

    private void FlushBluetoothDevices() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Need bluetooth permission to get heartbeat infos.", Toast.LENGTH_LONG).show();
            return;
        }
        Set<BluetoothDevice> deviceSet = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        views = deviceSet.stream()
                .filter((d)->d.getType() == BluetoothDevice.DEVICE_TYPE_LE)
                .map(BluetoothCardView::new)
                .toArray(BluetoothCardView[]::new);
        Arrays.sort(views, (a,b)->{
            /* TODO: more things to do */
            return a.devStatus.name.compareTo(b.devStatus.name);
        });
        bluetoothScrollView.removeAllViews();
        for (BluetoothCardView v :
                views) {
            bluetoothScrollView.addView(v.root);
        }
    }



}