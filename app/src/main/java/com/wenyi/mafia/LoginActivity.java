package com.wenyi.mafia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.AttributedCharacterIterator;

import com.wenyi.mafia.myservice.InternetService;
import com.wenyi.mafia.mytools.CustomToast;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class LoginActivity extends Activity implements View.OnClickListener,InternetService.ServerListener {
	private EditText nickName;
    private ServiceConnection sc;
    private InternetService server;
    private boolean isBinding = false;
    private RelativeLayout isLogin;
    private CustomToast toast;
    private MyHandler handler;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if("closeAPP".equals(action)) {
                ((Activity) context).finish();
            }
        }
    };
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
        handler = new MyHandler(this);
		nickName = (EditText) findViewById(R.id.nickName);
        isLogin = (RelativeLayout) findViewById(R.id.isLogin);
        findViewById(R.id.loginUp).setOnClickListener(this);
		readInfo();
        if(!isBinding) bindLogin();
	}

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("closeAPP");
        registerReceiver(this.broadcastReceiver, filter);
    }

    @Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.loginUp:
//            int netType = getNetworkType();
//            if(netType == 1) {
                if (!isBinding) bindLogin();
                else if (server != null) {
                    if("".equals(nickName.getText().toString().trim())){
                        makeToast(getString(R.string.empty_nick_name));
                    }else {
                        saveInfo();
                        server.setNickName(nickName.getText().toString().trim());
                        server.login();
                        isLogin.setVisibility(View.VISIBLE);
                    }
                }
//            }else if(netType > 1){
//                makeToast(getString(R.string.not_wifi_tips));
//            }else {
//                makeToast(getString(R.string.no_net_tips));
//            }
			break;
		default:
			break;
		}
	}
    public void bindLogin() {
        sc = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                // TODO Auto-generated method stub
            }
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // TODO Auto-generated method stub
                server = ((InternetService.MyServiceBinder) service).getService();
                server.setServerListener(LoginActivity.this);
                server.setNickName(nickName.getText().toString().trim());
            }
        };
        Intent server = new Intent(this, InternetService.class);
        bindService(server, sc, Context.BIND_AUTO_CREATE);
        isBinding = true;
    }
    public void unBindLogin(){
        unbindService(sc);
        isBinding = false;
    }


    @Override
    protected void onPause() {
        if(toast != null && toast.isShowing())
            toast.release();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        if(isBinding) unBindLogin();
        super.onDestroy();
    }

    public void gotoActivity(){
        isLogin.setVisibility(View.GONE);
//        isLogin.getVisibility()
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        if(isBinding) unBindLogin();
//        finish();
    }
	public void saveInfo(){
        try {
            OutputStream out = openFileOutput("nick.info", Context.MODE_PRIVATE);
            String info = nickName.getText().toString();
            byte[] bytes = info.getBytes();
            out.write(bytes,0,bytes.length);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	public void readInfo(){
		String abc = this.getFilesDir()+"/nick.info";
        File file = new File(abc);
        //检测是否存在缓存文件
        if(file.exists()){
            try {
                InputStream in = openFileInput("nick.info");
//                      InputStream in = getResources().openRawResource(R.raw.Configuration);
                byte[] bytes = new byte[256];
                StringBuilder sb = new StringBuilder();
                int len = -1;
                while((len = in.read(bytes))!=-1){
                    sb.append(new String(bytes,0,len));
                }
                nickName.setText(sb);
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && server.isConnect())
            server.sendFirstLevel(InternetService.LOGOUT,"");
        return super.onKeyDown(keyCode, event);
    }
    /**
     * 获取当前网络类型
     * return 0：没有网络   1：WIFI网络   2：WAP网络    3：NET网络
     */

    public static final int NETTYPE_WIFI = 0x01;
    public static final int NETTYPE_CMWAP = 0x02;
    public static final int NETTYPE_CMNET = 0x03;
    public int getNetworkType() {
        int netType = 0;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return netType;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {
            String extraInfo = networkInfo.getExtraInfo();
            if(extraInfo != null && extraInfo.length() != 0){
                if (extraInfo.toLowerCase().equals("cmnet")) {
                    netType = NETTYPE_CMNET;
                } else {
                    netType = NETTYPE_CMWAP;
                }
            }
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = NETTYPE_WIFI;
        }
        return netType;
    }
    public static class MyHandler extends Handler {
        WeakReference<LoginActivity> weakReference;
        public MyHandler(LoginActivity activity){
            this.weakReference = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LoginActivity activity = this.weakReference.get();
            if(activity!=null){
                switch (msg.what) {
                    case CustomToast.CANCEL_TOAST:
                        if(activity.toast != null && activity.toast.isShowing())
                            activity.toast.cancel();
                        String toastMsg = msg.getData().getString("toast");
                        if(toastMsg != null)
                            activity.makeToast(toastMsg);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void makeToast(String msg) {
        if(msg == null) return;
        if(msg.equals(getString(R.string.login_success))) {
            gotoActivity();
        }else {
            if(!msg.equals(getString(R.string.connect_success)))
                isLogin.setVisibility(View.GONE);
            if(toast == null || !toast.isShowing()) {
                toast = CustomToast.makeText(this, msg, CustomToast.LENGTH_LONG);
                toast.show();
            }else {
                Message message = handler.obtainMessage(CustomToast.CANCEL_TOAST);
                message.getData().putCharSequence("toast",msg);
                handler.sendMessageDelayed(message,500);
            }
        }
    }
    @Override
    public void intoRoom(String roomJson) {

    }

    @Override
    public void getRoomList(String... msg) {

    }

    @Override
    public void getSeat(String... msg) {

    }

    @Override
    public void setSeat(int num) {

    }

    @Override
    public void setRole(int role) {

    }

    @Override
    public void changeProcedure(int procedure) {

    }

    @Override
    public void roleAction(String value) {

    }

    @Override
    public void upDateRoomList(int what, String json) {

    }

    @Override
    public void makeDialog(int roomID) {

    }

    @Override
    public void upDateRoomTalk(String msg) {

    }

    @Override
    public void replyVote(String reply) {

    }

    @Override
    public void tellWhoDead(int... deadSeat) {

    }

    @Override
    public void election() {

    }

    @Override
    public void voteElection() {

    }
}
