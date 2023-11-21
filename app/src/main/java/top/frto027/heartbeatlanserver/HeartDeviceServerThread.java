package top.frto027.heartbeatlanserver;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HeartDeviceServerThread extends Thread{
    private final static String TAG = "HEART_RATE_SERVER";
    private static HeartDeviceServerThread instance;

    public static boolean enableBroadcast = true;
    public static synchronized HeartDeviceServerThread getInstance(){
        if(instance == null){
            instance = new HeartDeviceServerThread();
            instance.start();
        }
        return instance;
    }

    public static synchronized void close(){
        if(instance != null){
            HeartDeviceServerThread i = instance;
            i.mHandler.post(()->{
                i.udpServerThread.interrupt();
                i.udpServerThread.broadcast_socket.close();
                i.mHandler.getLooper().quit();
            });
            instance = null;
        }
    }

    public void informDevStatus(HeartDeviceStatus status){
        mHandler.post(()->{
            try {
                udpServerThread.send(status);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        });
    }

    UdpServerThread udpServerThread = new UdpServerThread();
    class UdpServerThread extends Thread{
        public final static String PROTOCOL_VER = "001";
        private final static String IMHERE_MAGIC = "HeartBeatSenderHere" + PROTOCOL_VER;
        private final static String CLIENT_MAGIC = "HeartBeatRecHere" + PROTOCOL_VER;

        private boolean isMagicWord(DatagramPacket pkt, String magic){
            byte [] m = magic.getBytes();
            if(pkt.getLength() < m.length)
                return false;
            byte [] pktd = pkt.getData();
            for(int i=0;i<m.length;i++){
                if(m[i] != pktd[i])
                    return false;
            }
            return true;
        }

        class Client{
            InetAddress mAddr;
            int mPort;
            Date mLastLive;
        }

        Map<String, Client> clients = new HashMap<>();

        public void send(HeartDeviceStatus deviceStatus) throws IOException {
            if(null == broadcast_socket)
                return;
            byte[] b = deviceStatus.getBytes();
            DatagramPacket pkt = new DatagramPacket(b, b.length);
            for(Client c:clients.values()){
                pkt.setAddress(c.mAddr);
                pkt.setPort(c.mPort);
                broadcast_socket.send(pkt);
            }
        }
        private void handleIncoming(DatagramPacket pkt){
            if(isMagicWord(pkt,CLIENT_MAGIC)){
                InetAddress addr = pkt.getAddress();
                String hash = addr.getHostAddress() + ":" + pkt.getPort();
                if(clients.containsKey(hash)){
                    clients.get(hash).mLastLive = new Date();
                }else{
                    Client c = new Client();
                    c.mLastLive = new Date();
                    c.mAddr = addr;
                    c.mPort = pkt.getPort();
                    clients.put(hash, c);
                }
            }
        }

        private void filterOutdatedClient(){
            LinkedList<String> r = new LinkedList<>();
            Date d = new Date();
            clients.forEach((k,v)->{
                if(d.getTime() - v.mLastLive.getTime() > 1000*60){
                    r.add(k);
                }
            });
            for(String k : r){
                clients.remove(k);
            }
        }
        DatagramSocket broadcast_socket;

        @Override
        public void run() {
            try {
                DatagramPacket in_pkt = new DatagramPacket(new byte[1024], 1024);

                byte [] bs = IMHERE_MAGIC.getBytes();
                DatagramPacket imhere_pkt = new DatagramPacket(bs, bs.length);
                imhere_pkt.setPort(9965);
                imhere_pkt.setAddress(InetAddress.getByName("255.255.255.255"));

                broadcast_socket = new DatagramSocket(new InetSocketAddress("0.0.0.0",0));
                broadcast_socket.setSoTimeout(3000);

                for(;;){
                    filterOutdatedClient();

                    try{
                        try{
                            broadcast_socket.receive(in_pkt);
                            //handle incoming message
                            handleIncoming(in_pkt);
                        } catch (SocketTimeoutException e){
                            //if timeout, send broadcast "I'm here"
                            try{
                                if(enableBroadcast)
                                    broadcast_socket.send(imhere_pkt);
                            }catch (IOException ee){
                                Log.e(TAG, ee.toString());
                            }
                        }catch (IOException e) {
                            Log.e(TAG,e.toString());
                            sleep(1000);
                        }
                    }catch (InterruptedException e){
                        break;
                    }
                }
                broadcast_socket.close();

            } catch (SocketException e) {
                throw new RuntimeException(e);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    };

    Handler mHandler;
    @Override
    public void run() {
        udpServerThread.start();
        Looper.prepare();
        mHandler = new Handler();
        Looper.loop();
    }
}
