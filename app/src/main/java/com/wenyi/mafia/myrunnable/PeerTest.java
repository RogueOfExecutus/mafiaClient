package com.wenyi.mafia.myrunnable;

import com.wenyi.mafia.MainActivity;
import com.wenyi.mafia.myservice.InternetService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Created by Administrator on 2016/8/25.
 * 用于测试网络是否能进行P2P通讯
 */
public class PeerTest implements Runnable {

    private MainActivity.MyHandler handler;

    public PeerTest setTestResult(MainActivity.MyHandler handler) {
        this.handler = handler;
        return this;
    }

    @Override
    public void run() {
        try {
            DatagramSocket peer = new DatagramSocket();
            new Thread(new PeerRec(peer)).start();
            DatagramPacket packet = new DatagramPacket(new byte[1],1,new InetSocketAddress(InternetService.serverIP,6789));
            peer.send(packet);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private class PeerRec implements Runnable{
        DatagramSocket peer;
        PeerRec(DatagramSocket peer){
            this.peer = peer;
        }
        @Override
        public void run() {
            int testPort = -1;
            while(true) {
                DatagramPacket p = new DatagramPacket(new byte[128],128);
                try {
                    peer.setSoTimeout(20*1000);
                    peer.receive(p);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                if(p.getPort() == testPort){
                    handler.sendEmptyMessage(200);
                    break;
                }else {
                    if(testPort == -1){
                        testPort = bytesToInt(p.getData(),0);
                        if(testPort != -1)
                            new Thread(new PeerSend(peer,testPort)).start();
                    }
                }
            }
        }
    }

    private class PeerSend implements Runnable{
        int testPort;
        DatagramSocket peer;
        PeerSend( DatagramSocket peer,int testPort){
            this.peer = peer;
            this.testPort = testPort;
        }
        @Override
        public void run() {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[0],0,new InetSocketAddress(InternetService.serverIP,testPort));
                for(int i=0;i<10;i++){
                    peer.send(packet);
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //int与byte数组之间的转换
	private static int bytesToInt(byte[] src, int offset) {
	    int value;
	    value = ((src[offset] & 0xFF)
	            | ((src[offset+1] & 0xFF)<<8)
	            | ((src[offset+2] & 0xFF)<<16)
	            | ((src[offset+3] & 0xFF)<<24));
	    return value;
	}
}
