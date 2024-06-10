package top.frto027.heartbeatlanserver;

import android.os.Build;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class UDPProtocol extends AppCompatActivity {
    CompoundButton broadcastToggleSwitch;
    CompoundButton enableUdpProtocolSwitch;
    static boolean protocolEnabled = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udpprotocol);
        setSupportActionBar(findViewById(R.id.my_toolbar));
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark, getTheme()));

        boolean isOculusDevice = Build.MODEL.equals("Quest") && Build.MANUFACTURER.equals("Oculus");

        broadcastToggleSwitch = findViewById(R.id.broadcast_toggle_switch);
        broadcastToggleSwitch.setChecked(true);

        ((TextView)findViewById(R.id.protocol_ver_tv)).setText(
                String.format(getText(R.string.protocol_ver_hint).toString(),
                        HeartDeviceServerThread.UdpServerThread.PROTOCOL_VER));

        broadcastToggleSwitch.setText(R.string.pairing_can_be_discovered);
        broadcastToggleSwitch.setOnCheckedChangeListener((b,c)->{
            HeartDeviceServerThread.enableBroadcast = c;
            if(HeartDeviceServerThread.enableBroadcast){
                broadcastToggleSwitch.setText(R.string.pairing_can_be_discovered);
            }else{
                broadcastToggleSwitch.setText(R.string.not_pairing_cannot_discovered);
            }
        });

        enableUdpProtocolSwitch = findViewById(R.id.enable_udp_protocol_switch);
        enableUdpProtocolSwitch.setChecked(protocolEnabled);
        enableUdpProtocolSwitch.setOnCheckedChangeListener((b,c)->{
            protocolEnabled = c;
        });

        CompoundButton localModeSwitch = findViewById(R.id.local_mode_toggle);
        localModeSwitch.setChecked(isOculusDevice);
        HeartDeviceServerThread.LocalhostMode = isOculusDevice;
        localModeSwitch.setOnCheckedChangeListener((v,c)->{
            HeartDeviceServerThread.LocalhostMode = c;
        });
        findViewById(R.id.back_btn).setOnClickListener((a)->{
            finish();
        });

    }
}