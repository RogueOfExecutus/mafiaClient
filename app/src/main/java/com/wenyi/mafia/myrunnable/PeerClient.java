package com.wenyi.mafia.myrunnable;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import com.wenyi.mafia.Speex;
import com.wenyi.mafia.myservice.InternetService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Vector;

/**
 * Created by Administrator on 2016/7/14.
 */
public class PeerClient implements Runnable {
    private String localIP,inetIP;
    private int roomID;
    private SocketAddress server;
    private AudioRecord audioRecord;
    private Vector<HashMap<String,String>> list = new Vector<>();
    private Vector<SocketAddress> linkList = new Vector<>();
    private DatagramSocket client;
    private final String[] item = {"1","2","3","4"};
    private boolean isGaming,isRecording = false;
    public PeerClient(int roomID,String localIP){
        this.roomID = roomID;
        this.localIP = localIP;
        System.out.println(localIP+"localIP");
    }
    @Override
    public void run() {
        try {
            isGaming = true;
            server = new InetSocketAddress(InternetService.serverIP,6789);
            client = new DatagramSocket();
            new Thread(new ReceiveAndPlay()).start();
            linkToServer();
            skip();
            Speex speex = new Speex();
            speex.init();
            int FrameSize = speex.getFrameSize();
            while (isGaming) {
                startCord(true);
                if(makeRecorder()) {
                    while (isRecording) {
                        short[] audiodata = new short[FrameSize];
                        byte[] processedData = new byte[21];
                        processedData[0] = '0';
                        int dataSize = 1;
                        int rlen = audioRecord.read(audiodata, 0, FrameSize);
                        byte[] rec = new byte[20];
                        int elen = speex.encode(audiodata, 0, rec, rlen);
                        System.arraycopy(rec, 0, processedData, dataSize, elen);
                        dataSize = ++elen;
                        DatagramPacket p = new DatagramPacket(processedData, dataSize);
                        sendToAllPeer(p);
                    }
                    endRecorder();
                }
            }
            speex.close();
            client.close();
            System.out.println("----send end----");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //
    public synchronized void startCord(boolean tag) throws InterruptedException {
        if(tag)
            this.wait();
        else {
            isRecording = isGaming;
            this.notify();
        }
    }
    //
    public void endCord(){
        isRecording = false;
    }
    public void endGame() throws InterruptedException {
        isGaming = false;
        startCord(false);
    }
    private void sendToAllPeer(DatagramPacket p) throws IOException {
        for (SocketAddress peer:linkList){
            p.setSocketAddress(peer);
            client.send(p);
        }
    }
    private void sendToAll(DatagramPacket p) throws IOException {
        for (HashMap<String, String> map : list) {
            if (map.get(item[0]).equals(inetIP)) {
                if (!map.get(item[2]).equals(localIP)) {
                    p.setSocketAddress(new InetSocketAddress(map.get(item[2]),  Integer.parseInt(map.get(item[3]))));
                    client.send(p);
                }
            } else {
                p.setSocketAddress(new InetSocketAddress(map.get(item[0]), Integer.parseInt(map.get(item[1]))));
                client.send(p);
            }
        }
    }
    private boolean makeRecorder() {
        // 获得缓冲区字节大小
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        // 创建AudioRecord对象
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
        try {
            audioRecord.startRecording();
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    private void endRecorder(){
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
    }
    private void linkToServer() throws Exception {
        String ip = roomID + "\n" + localIP + "\n" + client.getLocalPort() + "\n0";
        byte[] linkMsg = ip.getBytes();
        client.send(new DatagramPacket(linkMsg,linkMsg.length,server));
    }
    private void skip(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //心跳，发送给服务器和其他peer
                while (isGaming){
                    try {
                        Thread.sleep(5*1000);
                        if(client.isClosed()){
                            if(isGaming)
                                continue;
                            else break;
                        }
                        client.send(new DatagramPacket(new byte[0], 0, server));
                        DatagramPacket p = new DatagramPacket(new byte[0], 0);
                        sendToAll(p);
                    }catch (Exception e){
                        isGaming = false;
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private class ReceiveAndPlay implements Runnable {
        //    Context context;
        private AudioTrack trackplayer;
        @Override
        public void run() {
            makePlayer();
            trackplayer.play();
            byte[] buf = new byte[1024];
            Speex speex = new Speex();
            speex.init();
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (isGaming){
                try {
                    client.receive(packet);
                    SocketAddress socketAddress = packet.getSocketAddress();
                    if(!socketAddress.equals(server) && !linkList.contains(socketAddress))
                        linkList.add(socketAddress);
                    System.out.println(packet.getAddress()+"----"+packet.getPort()+"-----rec");
                    if(packet.getLength() == 0)
                        continue;
                    byte[] rec = packet.getData();
                    if(rec[0]=='0'){
                        byte[] processedData = new byte[20];
                        short[] playDate = new short[256];
                        System.arraycopy(rec, 1, processedData, 0, 20);
                        int dlen = speex.decode(processedData,playDate,20);
                        trackplayer.write(playDate,0,dlen);
                    }else {
                        String[] msg = new String(rec).trim().split("\\|/");
                        if(msg.length == 2){
                            inetIP = msg[1].split("\n",2)[0];
//                            inetPort = Integer.parseInt(msg[1]);
//                            localIP = msg[2];
//                            localPort = Integer.parseInt(msg[3]);
                        }else {
                            for (int i = list.size()+1; i < msg.length; i++) {
                                String[] ipItem = msg[i].split("\n");
                                HashMap<String, String> map = new HashMap<>();
                                map.put(item[0], ipItem[0]);
                                map.put(item[1], ipItem[1]);
                                map.put(item[2], ipItem[2]);
                                map.put(item[3], ipItem[3]);
                                list.add(map);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            trackplayer.stop();
            trackplayer.release();
            speex.close();
            System.out.println("----rec end----");
        }
        private void makePlayer(){
            //根据采样率，采样精度，单双声道来得到frame的大小。
            int bufsize = AudioTrack.getMinBufferSize(8000,//每秒8K个点
                    AudioFormat.CHANNEL_OUT_STEREO,//双声道
                    AudioFormat.ENCODING_PCM_16BIT);//一个采样点16比特-2个字节
            //注意，按照数字音频的知识，这个算出来的是一秒钟buffer的大小。
            //创建AudioTrack
            trackplayer = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufsize,
                    AudioTrack.MODE_STREAM);
        }
    }

}
