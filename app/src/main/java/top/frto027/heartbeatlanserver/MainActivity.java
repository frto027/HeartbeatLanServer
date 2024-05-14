package top.frto027.heartbeatlanserver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Process;
import android.text.Html;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    class AddrPort{
        public String address;
        public transient InetAddress addr;
        public int port;

        AddrPort(String addr, int port){
            this.address = addr;
            this.port = port;
            try{
                this.addr = InetAddress.getByName(addr);
            }catch (UnknownHostException e){

            }
        }

        void flush(){
            if(this.addr == null){
                try{
                    this.addr = InetAddress.getByName(address);
                }catch (UnknownHostException e){

                }
            }
        }

        @Override
        public int hashCode() {
            return address.hashCode() ^ port;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return this == obj;
        }
    }
    ConfigHelper configHelper;
    LinearLayoutCompat bluetoothScrollView;
    LinearLayoutCompat oscListView;


    final Set<AddrPort> oscClients = new HashSet<>();
    BluetoothCardView[] views;
    Handler handler = new Handler();

    CompoundButton broadcastToggleSwitch;



    final static String HEART_UUID = "00002a37-0000-1000-8000-00805f9b34fb";

    boolean isResume = true;
    Toast backgroundToast;
    @Override
    protected void onPause() {
        super.onPause();
        backgroundToast.show();
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
        backgroundToast.show();
        //Toast.makeText(this, R.string.app_selfkill,Toast.LENGTH_LONG).show();
        //HeartDeviceServerThread.close();
    }

    class BluetoothCardView {
        BluetoothDevice dev;

        View root = null;

        TextView nameTv, heartTv, macAddrTv;

        CheckBox con_discon_btn;

        boolean connected = false;

        public BluetoothCardView(BluetoothDevice dev) {
            this.dev = dev;
            root = View.inflate(MainActivity.this, R.layout.bluetooth_card, null);
            nameTv = root.findViewById(R.id.dev_name_tv);
            heartTv = root.findViewById(R.id.dev_heart_rate_tv);
            macAddrTv = root.findViewById(R.id.dev_mac_tv);
            con_discon_btn = root.findViewById(R.id.connect_disconnect_toggle);
            if(configHelper.isMacSelected(dev.getAddress()))
            {
                ToggleStatus(true);
                con_discon_btn.setChecked(true);
            }
            else
                con_discon_btn.setChecked(false);
            con_discon_btn.setOnCheckedChangeListener((e,v) ->{
                ToggleStatus(v);
                configHelper.setMacSelected(dev.getAddress(), v);
            });
            Update();
        }

        BluetoothGatt gatt;

        HeartDeviceStatus devStatus = new HeartDeviceStatus();
        class BluetoothGattCb extends BluetoothGattCallback {
            BluetoothGatt gatt;
            /*
            Why we need this variable called useLatestHandleGatt:
                Some device will use the new api : onCharacteristicChanged(gatt, chara, values)
                However, this api is never called in quest 2 device.
                Quest 2 use onCharacteristicChanged(gatt, chara) instead.
                The old api is deprecated in API LEVEL 33, and I'm not sure if it will called in latest device.
                So the stupid variable here to prevent duplicate data.
             */
            boolean useLatestHandleGatt = false;
            private void handleGatt(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic){
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
                    devStatus.flag = flag;
                    HeartDeviceServerThread.getInstance().informDevStatus(devStatus);
                    SendHeartRateOSC(devStatus.heartRate);
                    TriggerUpdate();
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                if(useLatestHandleGatt)
                    return;
                handleGatt(gatt, characteristic);
            }


            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
                super.onCharacteristicChanged(gatt, characteristic, value);
                useLatestHandleGatt = true;
                handleGatt(gatt, characteristic);
            }

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

        void ToggleStatus(boolean new_status) {
            if(connected == new_status)
                return;
            connected = new_status;
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

            nameTv.setText(devStatus.name);
            heartTv.setText(String.format(Locale.CHINA, "%d", devStatus.heartRate));
            macAddrTv.setText(devStatus.address);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean isOculusDevice = Build.MODEL.equals("Quest") && Build.MANUFACTURER.equals("Oculus");
        CompoundButton localModeSwitch = findViewById(R.id.local_mode_toggle);
        localModeSwitch.setChecked(isOculusDevice);
        HeartDeviceServerThread.LocalhostMode = isOculusDevice;
        localModeSwitch.setOnCheckedChangeListener((v,c)->{
            HeartDeviceServerThread.LocalhostMode = c;
        });

        backgroundToast = Toast.makeText(this, R.string.app_background,Toast.LENGTH_SHORT);
        configHelper = new ConfigHelper(this);
        findViewById(R.id.close_app_btn).setOnClickListener((e)-> {
                    moveTaskToBack(true);
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                });
        bluetoothScrollView = findViewById(R.id.main_activity_scrollview);
        broadcastToggleSwitch = findViewById(R.id.broadcast_toggle_switch);
        broadcastToggleSwitch.setChecked(true);

        ((TextView)findViewById(R.id.protocol_ver_tv)).setText(
                String.format(getText(R.string.protocol_ver_hint).toString(),
                        HeartDeviceServerThread.UdpServerThread.PROTOCOL_VER));
        TextView licenseTv = (TextView)findViewById(R.id.license_tv);
        licenseTv.setText(
                Html.fromHtml("<a href='#'>"
                        + getText(R.string.license) + "</a>", Html.FROM_HTML_MODE_LEGACY));
        licenseTv.setOnClickListener((e)->{
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://github.com/frto027/HeartbeatLanServer"));
            startActivity(i);
            /* don't bother user. the link will remove it self once clicked. */
            licenseTv.setOnClickListener(null);
            licenseTv.setText(R.string.license);
        });

        broadcastToggleSwitch.setText(R.string.pairing_can_be_discovered);
        broadcastToggleSwitch.setOnCheckedChangeListener((b,c)->{
            HeartDeviceServerThread.enableBroadcast = c;
            if(HeartDeviceServerThread.enableBroadcast){
                broadcastToggleSwitch.setText(R.string.pairing_can_be_discovered);
            }else{
                broadcastToggleSwitch.setText(R.string.not_pairing_cannot_discovered);
            }
        });

        ReadPreferences();
        oscListView = findViewById(R.id.osc_listview);
        syncOscLists();
        findViewById(R.id.add_osc_btn).setOnClickListener((e)->{
            //add osc
            new OSCAddressInputDialog().setListener(new OSCAddressInputDialog.Listener() {
                @Override
                public void onOk(String ip, int port) {
                    synchronized (oscClients){
                        oscClients.add(new AddrPort(ip, port));
                        WritePreferences();
                    }
                    syncOscLists();
                }

                @Override
                public void onCancel() {

                }
            }).show(getSupportFragmentManager(), "oscdialog");
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);

            return;
        }

        HeartDeviceServerThread.getInstance();
        FlushBluetoothDevices();
    }

    static class AddrPortTextView extends androidx.appcompat.widget.AppCompatTextView{
        AddrPort addrPort;
        @SuppressLint("SetTextI18n")
        AddrPortTextView(Context context, AddrPort addrPort){
            super(context);
            this.addrPort = addrPort;
            setText(addrPort.address + " " + addrPort.port);
            setClickable(true);
            setTextSize(16);
            setPadding(0,8,0,0);
        }
    }

    private void syncOscLists(){
        oscListView.removeAllViews();
        TextView ftv = new TextView(this);
        ftv.setText(R.string.osc_ip_list_title);
        ftv.setTextSize(16);
        oscListView.addView(ftv);
        synchronized (oscClients){
            for(AddrPort ip: oscClients){
                AddrPortTextView tv = new AddrPortTextView(this, ip);
                tv.setOnLongClickListener(v -> {
                    AddrPort ip1 = ((AddrPortTextView)v).addrPort;
                    synchronized (oscClients){
                        oscClients.remove(ip1);
                        WritePreferences();
                    }
                    syncOscLists();
                    return true;
                });
                oscListView.addView(tv);
            }
        }
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

    void ReadPreferences(){
        Gson gson = new Gson();
        AddrPort[] list = gson.fromJson(getPreferences(MODE_PRIVATE).getString("oscclients", "[]"), AddrPort[].class);
        oscClients.clear();
        oscClients.addAll(Arrays.asList(list));
        for (AddrPort ap :
                oscClients) {
            ap.flush();
        }
    }
    void WritePreferences(){
        AddrPort[] list = new AddrPort[oscClients.size()];
        list = oscClients.toArray(list);
        String json = new Gson().toJson(list);
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString("oscclients",json);
        editor.apply();
    }

    DatagramSocket udpsocket_v4, udpsocket_v6;

    private boolean WriteIntHeartrate = true;

    private byte[] MakeOscPackage(int heartrate){
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte[] oscAddr = "/avatar/parameters/Heartrate3".getBytes();
        buffer.put(oscAddr);
        buffer.put((byte)0);
        while(buffer.position() % 4 != 0)
            buffer.put((byte)0);
        if(WriteIntHeartrate){
            buffer.put(",i".getBytes());
            buffer.put(new byte[]{0,0});
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(heartrate);
        }else{
            buffer.put(",f".getBytes());
            buffer.put(new byte[]{0,0});
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putFloat(((float)heartrate)/255);
        }
        return Arrays.copyOf(buffer.array(), buffer.position());
    }
    private void SendHeartRateOSC(int heartrate){
        // as google documented, this happens in a background thread
        byte[] pkg = MakeOscPackage(heartrate);
        DatagramPacket pkt = new DatagramPacket(pkg, 0, pkg.length);
        synchronized (oscClients){
            for(AddrPort ip : oscClients){
                try{
                    InetAddress addr = ip.addr;
                    pkt.setAddress(addr);
                    pkt.setPort(ip.port);

                    if(addr instanceof Inet6Address){
                        if(udpsocket_v6 == null){
                            udpsocket_v6 = new DatagramSocket(new InetSocketAddress("::", 0));
                        }
                        udpsocket_v6.send(pkt);
                    }else if(addr instanceof Inet4Address){
                        if(udpsocket_v4 == null){
                            udpsocket_v4 = new DatagramSocket(new InetSocketAddress("0.0.0.0",0));
                        }
                        udpsocket_v4.send(pkt);
                    }
                } catch (IOException e) {
                }
            }
        }
    }
}