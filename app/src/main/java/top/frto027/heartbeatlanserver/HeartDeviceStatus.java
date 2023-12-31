package top.frto027.heartbeatlanserver;

import androidx.annotation.NonNull;

import java.net.DatagramPacket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HeartDeviceStatus {
    public String name, address;
    public int heartRate;
    public int flag;
    public boolean isSensorContactSupport(){
        return (flag & 0x2) != 0;
    }

    public boolean isSensorContactStatus() {
        return (flag & 0x4) != 0;
    }

    public boolean isEnergyExpended() {
        return (flag & 0x8) != 0;
    }
    public boolean isRRInterval(){
        //?
        return (flag & 0x10) != 0;
    }
    @NonNull
    @Override
    public String toString() {
        return "DevName: " + name + " HeartRate: " + heartRate + "\n addr: " + address;
    }

    public byte[] getBytes(){
        byte[] namebts = name.getBytes();
        byte[] addrbts = address.getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(namebts.length + 1 + addrbts.length + 1 + 4);
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.put(namebts);
        buffer.put((byte)0);
        buffer.put(addrbts);
        buffer.put((byte)0);
        buffer.putInt(heartRate);
        return buffer.array();
    }
}
