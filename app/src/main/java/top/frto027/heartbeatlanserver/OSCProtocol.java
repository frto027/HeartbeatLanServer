package top.frto027.heartbeatlanserver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class OSCProtocol extends AppCompatActivity {
    static boolean protocolEnabled = true;

    LinearLayoutCompat oscListView;
    static final Set<OSCProtocol.AddrPort> oscClients = new HashSet<>();
    static class AddrPort{
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oscprotocol);

        setSupportActionBar(findViewById(R.id.my_toolbar));
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark, getTheme()));

        oscListView = findViewById(R.id.osc_listview);
        syncOscLists();
        findViewById(R.id.add_osc_btn).setOnClickListener((e)->{
            //add osc
            new OSCAddressInputDialog().setListener(new OSCAddressInputDialog.Listener() {
                @Override
                public void onOk(String ip, int port) {
                    synchronized (OSCProtocol.oscClients){
                        OSCProtocol.oscClients.add(new OSCProtocol.AddrPort(ip, port));
                        WritePreferences(OSCProtocol.this);
                    }
                    syncOscLists();
                }

                @Override
                public void onCancel() {

                }
            }).show(getSupportFragmentManager(), "oscdialog");
        });
        findViewById(R.id.back_btn).setOnClickListener((a)->{
            finish();
        });
    }


    static class AddrPortTextView extends LinearLayout {
        public interface OnRemoveListener{
            void Remove(AddrPortTextView view);
        }
        AddrPort addrPort;

        OnRemoveListener l;
        @SuppressLint("SetTextI18n")
        AddrPortTextView(Context context, AddrPort addrPort){
            super(context);
            this.addrPort = addrPort;
            View v = inflate(context, R.layout.osc_addr_port_item, this);
            TextView tv = v.findViewById(R.id.addr_port_textview);
            tv.setText(addrPort.address + " " + addrPort.port);
            v.findViewById(R.id.addr_port_remove_btn).setOnClickListener((a)->{
                if(this.l != null)
                    this.l.Remove(this);
            });
        }

        public void setOnRemoveListener(OnRemoveListener l) {
            this.l = l;
        }
    }

    private void syncOscLists(){
        oscListView.removeAllViews();
        synchronized (OSCProtocol.oscClients){
            for(OSCProtocol.AddrPort ip: OSCProtocol.oscClients){
                OSCProtocol.AddrPortTextView tv = new OSCProtocol.AddrPortTextView(this, ip);
                tv.setOnRemoveListener(v -> {
                    OSCProtocol.AddrPort ip1 = v.addrPort;
                    synchronized (OSCProtocol.oscClients){
                        OSCProtocol.oscClients.remove(ip1);
                        WritePreferences(this);
                    }
                    syncOscLists();
                });
                oscListView.addView(tv);
            }
        }
    }

    static void ReadPreferences(Activity activity){
        Gson gson = new Gson();
        OSCProtocol.AddrPort[] list = gson.fromJson(activity.getPreferences(MODE_PRIVATE).getString("oscclients", "[]"), OSCProtocol.AddrPort[].class);
        OSCProtocol.oscClients.clear();
        OSCProtocol.oscClients.addAll(Arrays.asList(list));
        for (OSCProtocol.AddrPort ap :
                OSCProtocol.oscClients) {
            ap.flush();
        }
    }
    static void WritePreferences(Activity activity){
        OSCProtocol.AddrPort[] list = new OSCProtocol.AddrPort[OSCProtocol.oscClients.size()];
        list = OSCProtocol.oscClients.toArray(list);
        String json = new Gson().toJson(list);
        SharedPreferences.Editor editor = activity.getPreferences(MODE_PRIVATE).edit();
        editor.putString("oscclients",json);
        editor.apply();
    }

}