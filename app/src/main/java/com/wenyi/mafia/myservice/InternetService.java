package com.wenyi.mafia.myservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import com.google.gson.Gson;
import com.wenyi.mafia.FirstMsg;
import com.wenyi.mafia.R;
import com.wenyi.mafia.RoomInfo;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class InternetService extends Service {
	public static final int LOGIN = 0,CREATE_ROOM = 1,OUT_ROOM = 2,SKIP = 3,INTO_ROOM = 4,ROOM_LIST = 5,ROOM_CHANGE = 6,CURRENT_CHANGE = 7,
			GET_ROOM = 8,START_GAME = 9,LOGOUT = 10,READY_GAME = 11,SEND_SEAT = 12,SEND_SELF_SEAT = 13,REC_SEAT = 14,START_GAME_ERROR = 15,
			READY_TALK = 16,GAME_SEND_ROLE = 17,ROLE_NIGHT = 18,ALL_NIGHT = 19,GAME_VOTE = 20,CONFIRM_VOTE = 21,TELL_WHO_DEAD = 22,
			ELECTIONEERINGA = 23,CANDIDATE = 24,SPEAK = 25,DAY_VOTE = 26,CHIEF_ELECTED = 27,VOTE_SITUATION = 28,VOTE_INVALID = 29,END_SPEAK = 30,
			ABANDON_ELECTION = 31,GAMING_TALK = 32,WOLF_IDIOCTONIA = 33,TRANSFER_CHIEF = 34,GAME_OVER = 35,INTO_ROOM_ERROR = 36,PASSWORD_ERROR = 37,
			CHANGE_ROOM = 38,ROOM_PASSWORD = 39,FALL_LINE = 40,HUNTER_SKILL = 41,IDIOT_SKILL = 42,CANDIDATE_SPEAK = 43,LOGIN_ERROR = 44,
			CHANGE_GAME_TYPE = 45,SEND_EMOJI = 46;
	public static final String GAME_READY = "1",GAME_UNREADY = "0";
	public static final int RECONNECT_ERROR = 100,CONNECT_FAILED = 200,CONNECT_SUCCESS = 300,CREATE_ROOM_SUCCESS = 400,UPDATE_ROOMLIST = 500,
			ADD_DELETE_ROOM = 600,PLUS_SUBTRACT_CURRENT = 700,GET_SEAT = 800,SET_SEAT = 900,CANNOT_START = 1000,UPDATE_ROOM_TALK = 1100,
			START_GAME_DIALOG = 1200,GAME_START = 1300,GAME_ROLE_ACTION = 1400,GAME_PROCEDURE = 1500,REPLY_VOTE = 1600,WHO_DEAD = 1700,
			ELECTION = 1800,VOTE_CHIEF = 1900,ELECTION_SITUATION = 2000,WHO_SPEAK = 2100,WHO_CHIEF = 2200,VOTE_S = 2300,SPEAK_STOP = 2400,
			ERROR_MSG = 2500,REC_PASSWORD = 2600,ROOM_MSG_CHANGE = 2700,SAME_VOTE = 2800,WHO_ABANDON_ELECTION = 2900,END_GAME = 3000,
			WHO_KILL_SELF = 3100,LOGIN_FAILED = 3200,LOGIN_SUCCESS = 3300,EMOJI_MSG = 3400;
	public static final String serverIP = "123.207.16.217";
//	public static final String serverIP = "192.168.0.139";
	private static final int PORT = 6636;
	private ServerListener serverListener;
	private boolean Connect = false;
	private Socket socket;
    private PrintWriter os;
    private BufferedReader is;
    private Gson gson = new Gson();
    private ServerHandler handler;
	private String nickName;
	private int seatNumber = -1,roleNumber = -1;
	private String password;
	private Runnable connecter;

	public boolean isConnect() {
		return Connect;
	}
	public String getPassword() {
		return password;
	}
	public int getRoleNumber() {
		return roleNumber;
	}
	public String getNickName() {
		return nickName;
	}
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	public int getSeatNumber() {
		return seatNumber;
	}
	public void setSeatNumber(int seatNumber) {
		this.seatNumber = seatNumber;
	}
	public void setServerListener(ServerListener serverListener) {
		this.serverListener = serverListener;
	}
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return new MyServiceBinder();
	}
	public void login(){
		if(Connect) {
			sendFirstLevel(LOGIN, nickName);
		}else {
			connect(true);
//			serverListener.makeToast(getString(R.string.reconnect));
		}
	}
	public void connectServer(){
		if(Connect) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						keepSkip();
						receive();
						stopSelf();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						Connect = false;
						connecter = null;
						handler.sendEmptyMessage(CONNECT_FAILED);
						e.printStackTrace();
					}
				}
				void receive() throws IOException {
					String readInfo;
					FirstMsg recInfo;
					while (Connect){
						readInfo = new String(is.readLine().getBytes("iso-8859-1"),"utf-8");
						recInfo = gson.fromJson(readInfo,FirstMsg.class);
						switch (recInfo.getType()){
							case LOGIN:
								handler.sendEmptyMessage(LOGIN_SUCCESS);
								break;
							case LOGIN_ERROR:
								handler.sendEmptyMessage(LOGIN_FAILED);
								break;
							case LOGOUT:
								return;
							case GET_ROOM:
//								不回传该协议
								break;
							case CREATE_ROOM:
								Message message = handler.obtainMessage(CREATE_ROOM_SUCCESS);
								message.getData().putCharSequence("roomMsg",recInfo.getMsg());
								message.sendToTarget();
								break;
							case INTO_ROOM:
								Message msg0 = handler.obtainMessage(CREATE_ROOM_SUCCESS);
								msg0.getData().putCharSequence("roomMsg",recInfo.getMsg());
								msg0.sendToTarget();
								break;
							case ROOM_LIST:
								String[] roomList = recInfo.getMsg().split("\n");
								Message msg = handler.obtainMessage(UPDATE_ROOMLIST);
								msg.getData().putStringArray("roomList",roomList);
								msg.sendToTarget();
								break;
							case ROOM_CHANGE:
								Message msg2 = handler.obtainMessage(ADD_DELETE_ROOM);
								msg2.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg2.sendToTarget();
								break;
							case CURRENT_CHANGE:
								Message msg3 = handler.obtainMessage(PLUS_SUBTRACT_CURRENT);
								msg3.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg3.sendToTarget();
								break;
							case READY_GAME:
								break;
							case SEND_SEAT:
								String[] seatMsg = recInfo.getMsg().split("\n");
								Message msg4 = handler.obtainMessage(GET_SEAT);
								msg4.getData().putStringArray("seatMsg",seatMsg);
								msg4.sendToTarget();
								break;
							case SEND_SELF_SEAT:
								seatNumber = Integer.parseInt(recInfo.getMsg());
								Message msg5 = handler.obtainMessage(SET_SEAT);
								msg5.arg1 = seatNumber;
								msg5.sendToTarget();
								break;
							case START_GAME_ERROR:
								handler.sendEmptyMessage(CANNOT_START);
								break;
							case READY_TALK:
								Message msg6 = handler.obtainMessage(UPDATE_ROOM_TALK);
								msg6.getData().putCharSequence("roomTalkMsg",recInfo.getMsg());
								msg6.sendToTarget();
								break;
							case START_GAME:
								Message msg18 = handler.obtainMessage(START_GAME_DIALOG);
								msg18.arg1 = Integer.parseInt(recInfo.getMsg());
								msg18.sendToTarget();
								break;
							case GAME_SEND_ROLE:
								roleNumber = Integer.parseInt(recInfo.getMsg());
								Message msg7 = handler.obtainMessage(GAME_START);
								msg7.arg1 = roleNumber;
								msg7.sendToTarget();
								break;
							case ROLE_NIGHT:
								Message msg8 = handler.obtainMessage(GAME_ROLE_ACTION);
								msg8.getData().putCharSequence("value",recInfo.getMsg());
								msg8.sendToTarget();
								break;
							case ALL_NIGHT:
								Message msg9 = handler.obtainMessage(GAME_PROCEDURE);
								msg9.arg1 = Integer.parseInt(recInfo.getMsg());
								msg9.sendToTarget();
								break;
							case CONFIRM_VOTE:
								Message msg10 = handler.obtainMessage(REPLY_VOTE);
								msg10.getData().putCharSequence("reply",recInfo.getMsg());
								msg10.sendToTarget();
								break;
							case TELL_WHO_DEAD:
								String[] deadMsg = recInfo.getMsg().split("\n");
								Message msg11 = handler.obtainMessage(WHO_DEAD);
								if(deadMsg.length == 2){
									msg11.arg1 = Integer.parseInt(deadMsg[0]);
									msg11.arg2 = Integer.parseInt(deadMsg[1]);
								}
								else{
									msg11.arg1 = Integer.parseInt(recInfo.getMsg().trim());
									msg11.arg2 = -1;
								}
								msg11.sendToTarget();
								break;
							case ELECTIONEERINGA:
								handler.sendEmptyMessage(ELECTION);
								break;
							case CANDIDATE:
								Message msg12 = handler.obtainMessage(ELECTION_SITUATION);
								msg12.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg12.sendToTarget();
								break;
							case SPEAK:
								Message msg13 = handler.obtainMessage(WHO_SPEAK);
								msg13.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg13.sendToTarget();
								break;
							case DAY_VOTE:
								handler.sendEmptyMessage(VOTE_CHIEF);
								break;
							case CHIEF_ELECTED:
								//谁当选警长
								Message msg14 = handler.obtainMessage(WHO_CHIEF);
								msg14.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg14.sendToTarget();
								break;
							case VOTE_SITUATION:
								//票型情况
								Message msg15 = handler.obtainMessage(VOTE_S);
								msg15.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg15.sendToTarget();
								break;
							case VOTE_INVALID:
								Message msg21 = handler.obtainMessage(SAME_VOTE);
								msg21.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg21.sendToTarget();
								break;
							case END_SPEAK:
								handler.sendEmptyMessage(SPEAK_STOP);
								break;
							case ABANDON_ELECTION:
								Message msg22 = handler.obtainMessage(WHO_ABANDON_ELECTION);
								msg22.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg22.sendToTarget();
								break;
							case GAMING_TALK:
								//暂时放弃，并入READY_TALK
								break;
							case TRANSFER_CHIEF:
								Message msg24 = handler.obtainMessage(WHO_CHIEF);
								msg24.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg24.sendToTarget();
								break;
							case GAME_OVER:
								Message msg23 = handler.obtainMessage(END_GAME);
								msg23.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg23.sendToTarget();
								break;
							case INTO_ROOM_ERROR:
								Message msg16 = handler.obtainMessage(ERROR_MSG);
								msg16.arg1 = recInfo.getType();
								msg16.sendToTarget();
								break;
							case PASSWORD_ERROR:
								Message msg17 = handler.obtainMessage(ERROR_MSG);
								msg17.arg1 = recInfo.getType();
								msg17.sendToTarget();
								break;
							case ROOM_PASSWORD:
								Message msg19 = handler.obtainMessage(REC_PASSWORD);
								//默认接收
								password = recInfo.getMsg();
								msg19.getData().putCharSequence("addOrDelete",password);
								msg19.sendToTarget();
								break;
							case CHANGE_ROOM:
								Message msg20 = handler.obtainMessage(ROOM_MSG_CHANGE);
								msg20.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg20.sendToTarget();
								break;
							case CANDIDATE_SPEAK:
								Message msg25 = handler.obtainMessage(CANDIDATE_SPEAK);
								msg25.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg25.sendToTarget();
								break;
							case WOLF_IDIOCTONIA:
								//狼人自爆说明
								Message msg26 = handler.obtainMessage(WHO_KILL_SELF);
								msg26.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg26.sendToTarget();
								break;
							case HUNTER_SKILL:
								Message msg27 = handler.obtainMessage(HUNTER_SKILL);
								msg27.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg27.sendToTarget();
								break;
							case IDIOT_SKILL:
								Message msg28 = handler.obtainMessage(IDIOT_SKILL);
								msg28.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg28.sendToTarget();
								break;
							case SEND_EMOJI:
								Message msg29 = handler.obtainMessage(EMOJI_MSG);
								msg29.getData().putCharSequence("addOrDelete",recInfo.getMsg());
								msg29.sendToTarget();
								break;
							default:
								break;
						}
					}
				}
			}).start();
		}else {
			handler.sendEmptyMessage(RECONNECT_ERROR);
		}
	}
	public void keepSkip(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (Connect){
					sendFirstLevel(SKIP,"");
					try {
						Thread.sleep(15*1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	public class MyServiceBinder extends Binder{
        public InternetService getService(){
            return InternetService.this;
        }
    }
	@Override
	public void unbindService(ServiceConnection conn) {
		// TODO Auto-generated method stub
		serverListener = null;
		super.unbindService(conn);
	}
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		handler = new ServerHandler(this);
	}
	static class ServerHandler extends Handler {
		WeakReference<InternetService> weakReference;

		ServerHandler(InternetService service) {
			this.weakReference = new WeakReference<>(service);
		}
		@Override
		public void handleMessage(Message msg) {
			InternetService service = weakReference.get();
			super.handleMessage(msg);
			if(service != null){
				ServerListener serverListener = service.serverListener;
				switch (msg.what){
					case LOGIN_SUCCESS:
						if (serverListener != null)
							serverListener.makeToast(service.getString(R.string.login_success));
						break;
					case RECONNECT_ERROR:
						if (serverListener != null)
							serverListener.makeToast(service.getString(R.string.reconnect_error));
						break;
					case CONNECT_FAILED:
						if (serverListener != null)
							serverListener.makeToast(service.getString(R.string.connect_failed));
						break;
					case CONNECT_SUCCESS:
						if (serverListener != null)
							serverListener.makeToast(service.getString(R.string.connect_success));
						break;
					case LOGIN_FAILED:
						if (serverListener != null)
							serverListener.makeToast(service.getString(R.string.login_error));
						break;
					case CREATE_ROOM_SUCCESS:
						if (serverListener != null)
							serverListener.intoRoom(msg.getData().getString("roomMsg"));
						break;
					case UPDATE_ROOMLIST:
						if (serverListener != null)
							serverListener.getRoomList(msg.getData().getStringArray("roomList"));
						break;
					case GET_SEAT:
						if (serverListener != null)
							serverListener.getSeat(msg.getData().getStringArray("seatMsg"));
						break;
					case SET_SEAT:
						if (serverListener != null) serverListener.setSeat(msg.arg1);
						break;
					case CANNOT_START:
						if (serverListener != null)
							serverListener.makeToast(service.getString(R.string.start_game_error));
						break;
					case UPDATE_ROOM_TALK:
						if (serverListener != null)
							serverListener.upDateRoomTalk(msg.getData().getString("roomTalkMsg"));
						break;
					case START_GAME_DIALOG:
						if(serverListener != null)
							serverListener.makeDialog(msg.arg1);
						break;
					case GAME_START:
						if(serverListener != null)
							serverListener.setRole(msg.arg1);
						break;
					case GAME_PROCEDURE:
						if(serverListener != null)
							serverListener.changeProcedure(msg.arg1);
						break;
					case GAME_ROLE_ACTION:
						if(serverListener != null)
							serverListener.roleAction(msg.getData().getString("value"));
						break;
					case REPLY_VOTE:
						if(serverListener != null)
							serverListener.replyVote(msg.getData().getString("reply"));
						break;
					case WHO_DEAD:
						if(serverListener != null)
							serverListener.tellWhoDead(msg.arg1,msg.arg2);
						break;
					case ELECTION:
						if(serverListener != null)
							serverListener.election();
						break;
					case VOTE_CHIEF:
						if(serverListener != null)
							serverListener.voteElection();
						break;
					case ERROR_MSG:
						if(serverListener != null){
							switch (msg.arg1){
								case INTO_ROOM_ERROR:
									serverListener.makeToast(service.getString(R.string.into_room_error));
									break;
								case PASSWORD_ERROR:
									serverListener.makeToast(service.getString(R.string.room_password_error));
									break;
							}
						}
						break;
					default:
						if (serverListener != null)
							serverListener.upDateRoomList(msg.what, msg.getData().getString("addOrDelete"));
						break;
				}
			}
		}
	}
	public void sendFirstLevel(int type,String msg){
		String str = null;
		try {
			str = new String(gson.toJson(new FirstMsg(type, msg)).getBytes("utf-8"),"iso-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		os.println(str);
		os.flush();
	}
	public void connect(final boolean tag){
		if(connecter == null)
		new Thread(connecter = new Runnable() {
			@Override
			public void run() {
				socket = new Socket();
				SocketAddress socAddress = new InetSocketAddress(serverIP, PORT);
				try {
					socket.connect(socAddress,5000);
					os = new PrintWriter(socket.getOutputStream());
					is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					Connect = true;
					connectServer();
					handler.sendEmptyMessage(CONNECT_SUCCESS);
					if(tag) sendFirstLevel(LOGIN, nickName);
				} catch (IOException e) {
					e.printStackTrace();
					Connect = false;
					connecter = null;
					handler.sendEmptyMessage(CONNECT_FAILED);
				}

			}
		}).start();
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
//		nickName = intent.getStringExtra("nick");
		connect(false);
		return super.onStartCommand(intent, flags, startId);
	}
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		try {
			os.close();
			is.close();
			socket.close();
			Connect = false;
			connecter = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}
	public interface ServerListener{
		void makeToast(String msg);
		void intoRoom(String roomJson);
		void getRoomList(String... msg);
		void getSeat(String... msg);
		void setSeat(int num);
		void setRole(int role);
		void changeProcedure(int procedure);
		void roleAction(String value);
		void upDateRoomList(int what,String json);
		void makeDialog(int roomID);
		void upDateRoomTalk(String msg);
		void replyVote(String reply);
		void tellWhoDead(int... deadSeat);
		void election();
		void voteElection();
	}
}
