package top.frto027.heartbeatlanserver;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class ConfigHelper {

    Context context;

    ConfigHelper(Context context){
        this.context = context;
        ble_macs = new HashSet<>();
        try{
            FileInputStream fis = context.openFileInput(BLE_MAC_FILE);
            InputStreamReader inputStreamReader =
                    new InputStreamReader(fis, StandardCharsets.UTF_8);
            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                String line = reader.readLine();
                while (line != null) {
                    ble_macs.add(line);
                    line = reader.readLine();
                }
            } catch (IOException ignored) {
            }
        }catch(IOException ignored){
        }
    }
    Set<String> ble_macs = null;

    final static String BLE_MAC_FILE = "ble_dev_list";

    boolean isMacSelected(String mac){
        return ble_macs.contains(mac);
    }

    void setMacSelected(String mac, boolean selected){
        if(selected == isMacSelected(mac))
            return;
        if(selected){
            ble_macs.add(mac);
        }else{
            ble_macs.remove(mac);
        }
        try (FileOutputStream fos = context.openFileOutput(BLE_MAC_FILE, Context.MODE_PRIVATE)) {
            for (String m :
                    ble_macs) {
                fos.write(m.getBytes(Charset.defaultCharset()));
                fos.write('\n');
            }
        } catch (IOException ignored) {
        }
    }
}
