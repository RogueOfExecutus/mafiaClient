package com.wenyi.mafia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wenyi.mafia.myrunnable.PeerClient;
import com.wenyi.mafia.myservice.InternetService;
import com.wenyi.mafia.mytools.CustomToast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamingActivity extends Activity implements View.OnClickListener,PopupMenu.OnMenuItemClickListener,InternetService.ServerListener,
		AdapterView.OnItemClickListener{
	public static final int VILLAGER = 1,IDIOT = 10,THIEF = 11,WOLF = 2,SEER = 30,HUNTER = 31,WITCH = 32,GUARD = 33;
	public static final int NIGHT_START = 100,NIGHT_THIEF = 200,NIGHT_GUARD = 300,NIGHT_SEER = 400,NIGHT_WOLF = 500,NIGHT_WITCH = 600,
			DAY_START_CHIEF = 700,DAY_START_MEET = 800;
	public static final int COUNT_VOTE_TIME = 100,COUNT_TALK_TIME = 200,COUNT_WOLF_TIME = 300;
	private LinearLayout[] seatGroup = new LinearLayout[4];
	private RelativeLayout[] seat;
	private final int[] seatGroupId = {R.id.linear_one,R.id.linear_two,R.id.linear_three,R.id.linear_four};
	private final int[] seatId = {R.id.number_one,R.id.number_two,R.id.number_three,R.id.number_four,
			R.id.number_five,R.id.number_six,R.id.number_seven,R.id.number_eight,
			R.id.number_nine,R.id.number_ten,R.id.number_eleven,R.id.number_twelve,
			R.id.number_thirteen,R.id.number_fourteen,R.id.number_fifteen,R.id.number_sixteen};
	private final int[] emojiId = {R.id.oneEmoji,R.id.twoEmoji,R.id.threeEmoji,R.id.fourEmoji,
			R.id.fiveEmoji,R.id.sixEmoji,R.id.sevenEmoji,R.id.eightEmoji};
	private final int[] emojiResources = {R.drawable.emoji_one,R.drawable.emoji_two,R.drawable.emoji_three,R.drawable.emoji_four,
			R.drawable.emoji_five,R.drawable.emoji_six,R.drawable.emoji_seven,R.drawable.emoji_eight};
	private final int[] seatNum = {R.drawable.seat_one,R.drawable.seat_two,R.drawable.seat_three,R.drawable.seat_four,
			R.drawable.seat_five,R.drawable.seat_six,R.drawable.seat_seven,R.drawable.seat_eight,R.drawable.seat_nine,
			R.drawable.seat_ten,R.drawable.seat_eleven,R.drawable.seat_twelve};
	private int num,voteNumber,abstainedNumber = -1,daysSelectItem,lastDays,voteTime,guardTag = -1,speaking = -1,chiefIndex = -1;
	private EditText message;
	private ImageView sendEmoji,killSelf,endSpeak,abandonElection;
	private HashMap<Integer,String> allRoleMap = new HashMap<>();
	private RoomInfo roomInfo;
	private int seatNumber,roleNumber,gameProcedure;
	private ServiceConnection sc;
	private InternetService server;
	private boolean isBinding = false,isVoteTime = false,isTalkTime = false,hasMedicine = true,hasPoison = true,idiotSkill = true;
	private Button confirm,abandon;
	private daysDisplayAdapter daysAdapter;
	private List<Map<String, Object>> list = new ArrayList<>();
	private ListView gameMsg;
	private List<ArrayAdapter<String>> daysMsg = new ArrayList<>();
	private TextView voteNumDisplay,countTime,actionTips;
	private GamingHandler handler;
	private List<Integer> confirmRoleSeat = new ArrayList<>(),electionChiefSeat = new ArrayList<>(),deathPlayerSeat = new ArrayList<>();
	private PeerClient speech;
	private CustomToast toast;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gaming);
		message = (EditText) findViewById(R.id.msg);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		for (int i = 0; i < 4; i++) {
			seatGroup[i] = (LinearLayout)findViewById(seatGroupId[i]);
		}
		HashMap<String,Object> map = new HashMap<>();
		map.put("days",getResources().getString(R.string.days_first));
		list.add(map);
		daysAdapter = new daysDisplayAdapter();
		ListView daysSelector = (ListView) findViewById(R.id.days_selector);
		daysSelector.setAdapter(daysAdapter);
		daysSelector.setOnItemClickListener(this);
		actionTips = (TextView) findViewById(R.id.actionName);
		gameMsg = (ListView) findViewById(R.id.game_msg);
		voteNumDisplay = (TextView) findViewById(R.id.voteNum);
		countTime = (TextView) findViewById(R.id.countTime);
		sendEmoji = (ImageView) findViewById(R.id.select_emoji);
		killSelf = (ImageView) findViewById(R.id.kill_self);
		endSpeak = (ImageView) findViewById(R.id.end_speak);
		abandonElection = (ImageView) findViewById(R.id.abandon_election);
		sendEmoji.setOnClickListener(this);
		killSelf.setOnClickListener(this);
		endSpeak.setOnClickListener(this);
		abandonElection.setOnClickListener(this);
		setDays();
		roomInfo = getIntent().getParcelableExtra("roomInfo");
		seatNumber = getIntent().getIntExtra("seat",-1);
		if(roomInfo.getUseAudio() == 0) {
			speech = new PeerClient(roomInfo.getRoomID(), getLocalIpAddress());
			new Thread(speech).start();
		}
		num = roomInfo.getMens();
		if(num != -1) setGame(num);
		if(num < 9) seatGroup[2].setVisibility(View.GONE);
		if(num < 13) seatGroup[3].setVisibility(View.GONE);
		if(!isBinding) bindGaming();
		confirm = (Button)findViewById(R.id.confirm);
		abandon = (Button)findViewById(R.id.abandon);
		confirm.setOnClickListener(this);
		abandon.setOnClickListener(this);
		findViewById(R.id.sendGameMsg).setOnClickListener(this);
		findViewById(R.id.select_emoji).setOnClickListener(this);
		writeMap();
		addGameMsgList();
		handler = new GamingHandler(this);
	}

	public String getLocalIpAddress() {
		//获取wifi服务
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		//判断wifi是否开启
//        if (!wifiManager.isWifiEnabled()) {
//            wifiManager.setWifiEnabled(true);
//        }
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		return intToIp(ipAddress);
	}
	public String intToIp(int i) {

		return (i & 0xFF ) + "." +
				((i >> 8 ) & 0xFF) + "." +
				((i >> 16 ) & 0xFF) + "." +
				( i >> 24 & 0xFF) ;
	}
	public void addGameMsgList(){
		lastDays = daysMsg.size();
		daysSelectItem = daysMsg.size();
		ArrayAdapter<String> msgAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<String>());
		daysMsg.add(msgAdapter);
		gameMsg.setAdapter(daysMsg.get(lastDays));
		if(lastDays != 0) addDays(daysSelectItem);
		else daysMsg.get(lastDays).add(getString(R.string.start_game));
	}
	public void addDays(int days){
		HashMap<String,Object> map = new HashMap<>();
		map.put("days",getResources().getString(R.string.days_count,days+""));
		list.add(map);
		daysAdapter.notifyDataSetInvalidated();
	}
	public void writeMap(){
		allRoleMap.put(VILLAGER,getResources().getString(R.string.villager));
		allRoleMap.put(IDIOT,getResources().getString(R.string.idiot));
		allRoleMap.put(THIEF,getResources().getString(R.string.thief));
		allRoleMap.put(WOLF,getResources().getString(R.string.wolf));
		allRoleMap.put(SEER,getResources().getString(R.string.seer));
		allRoleMap.put(HUNTER,getResources().getString(R.string.hunter));
		allRoleMap.put(WITCH,getResources().getString(R.string.witch));
		allRoleMap.put(GUARD,getResources().getString(R.string.guard));
	}
	public void setGame(int num){
		seat = new RelativeLayout[num];
		for (int i = 0; i < num; i++) {
			seat[i] = (RelativeLayout) findViewById(seatId[i]);
			((ImageView)seat[i].findViewById(R.id.dead_way)).setImageResource(seatNum[i]);
			seat[i].setOnClickListener(this);
			if(seatNumber == i) ((ImageView)seat[i].findViewById(R.id.seat_num)).setImageResource(R.drawable.me);
		}
	}
	public void bindGaming() {
		sc = new ServiceConnection() {

			@Override
			public void onServiceDisconnected(ComponentName name) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				// TODO Auto-generated method stub
				server = ((InternetService.MyServiceBinder) service).getService();
				server.setServerListener(GamingActivity.this);
				roleNumber = server.getRoleNumber();
				if(roleNumber != -1) {
					setAllRoleImage(seatNumber,roleNumber);
					confirmRoleSeat.add(seatNumber);
				}
				//更换自己身份图片
			}
		};
		Intent server = new Intent(this, InternetService.class);
		bindService(server, sc, Context.BIND_AUTO_CREATE);
		isBinding = true;
	}
	public void unBindGaming(){
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
		if(isBinding)
			unBindGaming();
		try {
			if(roomInfo.getUseAudio() == 0)
				speech.endGame();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()){
			case R.id.confirm:
				if(isVoteTime) {
					if(gameProcedure == NIGHT_GUARD && voteNumber == guardTag)
						Toast.makeText(this,getString(R.string.night_guard_same),Toast.LENGTH_LONG).show();
					else if(voteNumber == -1) makeToast(getString(R.string.please_select));
					else if(gameProcedure == DAY_START_CHIEF && electionChiefSeat.contains(seatNumber))
						makeToast(getString(R.string.chief_not_vote));
					else voteDialog(voteNumber, true, InternetService.GAME_VOTE);
				}
				break;
			case R.id.abandon:
				if(isVoteTime) {
					if(gameProcedure == DAY_START_CHIEF && electionChiefSeat.contains(seatNumber))
						makeToast(getString(R.string.chief_not_vote));
					else voteDialog(abstainedNumber, false, InternetService.GAME_VOTE);
				}
				break;
			case R.id.sendGameMsg:
				if(isTalkTime){
					if("".equals(message.getText().toString().trim())){
						makeToast(getString(R.string.empty_talk));
					}else {
						String msg = getString(R.string.gaming_talk, server.getNickName(),
								(seatNumber + 1)+"", message.getText().toString());
						server.sendFirstLevel(InternetService.READY_TALK, msg);
						message.setText("");
						if (gameProcedure != NIGHT_WOLF)
							daysMsg.get(lastDays).add(msg);
					}
				}else{
					makeToast(getString(R.string.not_talk_time));
				}
				break;
			case R.id.select_emoji:
				sendEmojiDialog();
				break;
			case R.id.kill_self:
				showTips(getString(R.string.kill_self),0);
				//show Dialog
				break;
			case R.id.end_speak:
				showTips(getString(R.string.end_speak),1);
				//show Dialog
				break;
			case R.id.abandon_election:
				showTips(getString(R.string.abandon_election),2);
				//show Dialog
				//加入判断是否GONE
				break;
			default:
				for (int i = 0; i < num; i++){
					if(v == seat[i] && gameProcedure != NIGHT_WITCH){
						voteNumber = i;
						if(isVoteTime){
							if(!(gameProcedure == DAY_START_CHIEF && !electionChiefSeat.contains(i)))
								voteNumDisplay.setText(String.valueOf(voteNumber + 1));
							else if(!electionChiefSeat.isEmpty()) {
								voteNumber = -1;
								voteNumDisplay.setText("");
							}
						}else if(!confirmRoleSeat.contains(i)){
							//预言家验人后不再弹出该菜单
							PopupMenu PM = new PopupMenu(GamingActivity.this, v);
							PM.inflate(R.menu.mark);
							PM.setOnMenuItemClickListener(this);
							PM.show();
						}
						break;
					}
				}
				break;
		}
	}
	public void sendEmojiDialog(){
		ImageView[] emoji = new ImageView[8];
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.all_emoji_layout,null);
		builder.setView(linearLayout);
		final AlertDialog dialog = builder.create();
		for(int i=0;i<8;i++){
			emoji[i] = (ImageView) linearLayout.findViewById(emojiId[i]);
			final int num = i;
			emoji[i].setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					server.sendFirstLevel(InternetService.SEND_EMOJI,seatNumber+"\n"+num);
					showEmoji(seatNumber,num);
					dialog.dismiss();
				}
			});
		}
		dialog.show();
	}
	public void showEmoji(int who,int emojiNum){
		if(who < 0 && who >= roomInfo.getMens()) return;
		((ImageView)seat[who].findViewById(R.id.seat_num)).setImageResource(emojiResources[emojiNum]);
		handler.removeMessages(who);
		handler.sendEmptyMessageDelayed(who,3000);
	}

	public void showTips(String msg,final int type){
		//狼人自爆、发言完毕、退水的提示
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg);
		builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				switch (type){
					case 0:
						server.sendFirstLevel(InternetService.WOLF_IDIOCTONIA,"");
						break;
					case 1:
						stopSpeak();
						break;
					case 2:
						server.sendFirstLevel(InternetService.ABANDON_ELECTION,"");
						stopSpeak();
						abandonElection.setVisibility(View.GONE);
						break;
					case 3:
//						finish();
						server.setSeatNumber(-1);
						server.sendFirstLevel(InternetService.OUT_ROOM,String.valueOf(roomInfo.getRoomID()));
						startActivity(new Intent(GamingActivity.this,MainActivity.class));
						break;
				}
			}
		});
		builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {

			}
		});
		builder.create().show();
	}
	public void voteDialog(int vote, final boolean tag, final int voteType){
		final int voteTemp = vote;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if(gameProcedure == NIGHT_THIEF) {
			builder.setMessage(getResources().getString(R.string.vote_tips, allRoleMap.get(vote)));
		}else if(gameProcedure == DAY_START_CHIEF && electionChiefSeat.isEmpty()){
			builder.setMessage(tag ? getString(R.string.election_chief) : getString(R.string.abstained_chief));
//			else if(electionChiefSeat.contains(seatNumber)) builder.setMessage(tag ? getString(R.string.end_speak) : getString(R.string.abandon_election));
		}else if(tag){
			String tips = (vote+1) + getString(R.string.vote_part_two);
			builder.setMessage(getResources().getString(R.string.vote_tips,tips));
		}else {
			builder.setMessage(getString(R.string.abstained_tips));
		}
		builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				if (voteAction(voteTemp, true, voteType)) {
					if (gameProcedure == NIGHT_GUARD) guardTag = voteTemp;
					if (gameProcedure == NIGHT_WITCH && tag) hasMedicine = false;
					if (gameProcedure == (NIGHT_WITCH + 50) && tag) hasPoison = false;
				}
				voteNumDisplay.setText("");
			}
		});
		builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {

			}
		});
		builder.create().show();
	}
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
			case R.id.unknow_id:
				((ImageView) seat[voteNumber].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_unknow);
				break;
			case R.id.seer_id:
				((ImageView) seat[voteNumber].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_seer);
				break;
			case R.id.hunter_id:
				((ImageView) seat[voteNumber].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_hunter);
				break;
			case R.id.witch_id:
				((ImageView) seat[voteNumber].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_witch);
				break;
			case R.id.guard_id:
				((ImageView) seat[voteNumber].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_guard);
				break;
			case R.id.idiot_id:
				((ImageView) seat[voteNumber].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_idiot);
				break;
			case R.id.wolf_id:
				((ImageView) seat[voteNumber].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_wolf);
				break;
			case R.id.villager_id:
				((ImageView) seat[voteNumber].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_villager);
				break;
			default:
				break;
		}
		return false;
	}
	public void setAllRoleImage(int seatNum,int role){
		switch (role){
			case SEER:
				((ImageView) seat[seatNum].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_seer);
				break;
			case HUNTER:
				((ImageView) seat[seatNum].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_hunter);
				break;
			case WITCH:
				((ImageView) seat[seatNum].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_witch);
				break;
			case GUARD:
				((ImageView) seat[seatNum].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_guard);
				break;
			case IDIOT:
				((ImageView) seat[seatNum].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_idiot);
				break;
			case WOLF:
				((ImageView) seat[seatNum].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_wolf);
				break;
			case VILLAGER:
				((ImageView) seat[seatNum].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_villager);
				break;
			case THIEF:
				((ImageView) seat[seatNum].findViewById(R.id.id_mark)).setImageResource(R.drawable.id_thief);
				break;
			default:
				break;
		}
	}

	static class GamingHandler extends Handler {
		WeakReference<GamingActivity> weakReference;
		public GamingHandler(GamingActivity activity){
			this.weakReference = new WeakReference<>(activity);
		}
		@Override
		public void handleMessage(Message msg) {
			GamingActivity activity = weakReference.get();
			super.handleMessage(msg);
			if(activity!=null){
				switch (msg.what) {
					case COUNT_VOTE_TIME:
						activity.countTime.setTextColor(Color.RED);
						if(activity.voteTime>0) {
							activity.countTime.setText(activity.getString(R.string.count_time, (activity.voteTime--)+""));
							activity.handler.sendEmptyMessageDelayed(COUNT_VOTE_TIME, 1000);
						}
						else {
							activity.voteAction(activity.abstainedNumber,false,InternetService.GAME_VOTE);
							activity.setDays();
							activity.voteNumDisplay.setText("");
						}
						break;
					case COUNT_TALK_TIME:
						activity.countTime.setTextColor(Color.RED);
						if(activity.voteTime>0) {
							activity.countTime.setText(activity.getString(R.string.count_time, (activity.voteTime--)+""));
							activity.handler.sendEmptyMessageDelayed(COUNT_TALK_TIME, 1000);
						}else {
							activity.stopSpeak();
							activity.setDays();
						}
						break;
					case COUNT_WOLF_TIME:
						activity.countTime.setTextColor(Color.RED);
						if(activity.voteTime>0) {
							activity.countTime.setText(activity.getString(R.string.count_time, (activity.voteTime--)+""));
							activity.handler.sendEmptyMessageDelayed(COUNT_WOLF_TIME, 1000);
						}else {
							activity.setVoteButton(50);
						}
						break;
					case CustomToast.CANCEL_TOAST:
						if(activity.toast != null && activity.toast.isShowing())
							activity.toast.cancel();
						String toastMsg = msg.getData().getString("toast");
						if(toastMsg != null)
							activity.makeToast(toastMsg);
						break;
					default:
						if(msg.what >= 0 && msg.what < activity.roomInfo.getMens())
							((ImageView)activity.seat[msg.what].findViewById(R.id.seat_num)).setImageResource(
									msg.what == activity.seatNumber?R.drawable.me:R.drawable.other);
						break;
				}
			}
		}
	}
	public void stopSpeak(){
		if(isTalkTime){
			if(roomInfo.getUseAudio() == 0)
				speech.endCord();
			voteTime = 0;
			server.sendFirstLevel(InternetService.END_SPEAK,"");
			isTalkTime = false;
			endSpeak.setVisibility(View.GONE);
		}
	}
	public boolean voteAction(int vote,boolean isManual,int voteType){
		if(isVoteTime) {
			server.sendFirstLevel(voteType, String.valueOf(vote));
			voteTime = 0;
			isVoteTime = false;
			return true;
		}else if(isManual){
			makeToast(getString(R.string.vote_time_out));
		}
		return false;
	}

	public void setDays() {
		countTime.setTextColor(Color.DKGRAY);
		if (lastDays == 0)
			countTime.setText(getString(R.string.days_first));
		else countTime.setText(getString(R.string.days_count, lastDays+""));
	}
	@Override
	public void makeToast(String msg) {
		if(msg == null) return;
		if(toast == null || !toast.isShowing()) {
			toast = CustomToast.makeText(this, msg, CustomToast.LENGTH_LONG);
			toast.show();
		}else {
			Message message = handler.obtainMessage(CustomToast.CANCEL_TOAST);
			message.getData().putCharSequence("toast",msg);
			handler.sendMessageDelayed(message,500);
		}
		if(getString(R.string.connect_failed).equals(msg))
			startActivity(new Intent(this,LoginActivity.class));
	}

	@Override
	public void intoRoom(String roomJson) {

	}

	@Override
	public void getRoomList(String... msg) {

	}

	@Override
	public void getSeat(String... msg) {
		int len = msg.length;
		if(len < 2) return;
		for(int i=0;i<len;) {
			int index = Integer.parseInt(msg[i++]);
			int type = Integer.parseInt(msg[i++]);
			if(type == -1){
				seat[index].findViewById(R.id.isOnLine).setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void setSeat(int num) {

	}

	@Override
	public void setRole(int role) {
		this.roleNumber = role;
		setAllRoleImage(seatNumber,role);
//		confirm.setText(getString(R.string.confirm));
//		abandon.setText(getString(R.string.abstained));
		daysMsg.get(lastDays).add(getString(R.string.role_get,allRoleMap.get(role)));
		//更换自己身份图片
	}
	@Override
	public void changeProcedure(int procedure) {
		if(gameProcedure == NIGHT_THIEF){
			//重置
			confirm.setText(getString(R.string.confirm));
			abandon.setText(getString(R.string.abstained));
			abstainedNumber = -1;
		}
		this.gameProcedure = procedure;
		switch (procedure){
			case NIGHT_START:
				daysMsg.get(lastDays).add(getString(R.string.night_start));
				break;
			case NIGHT_THIEF:
				daysMsg.get(lastDays).add(getString(R.string.night_thief_one));
				break;
			case NIGHT_GUARD:
				daysMsg.get(lastDays).add(getString(R.string.night_guard_one));
				break;
			case NIGHT_SEER:
				daysMsg.get(lastDays).add(getString(R.string.night_seer_one));
				break;
			case NIGHT_WOLF:
				daysMsg.get(lastDays).add(getString(R.string.night_wolf_one));
				break;
			case NIGHT_WITCH:
				isTalkTime = false;
				if(this.roleNumber != WITCH || !this.hasMedicine) {
					daysMsg.get(lastDays).add(getString(R.string.night_witch_one));
					daysMsg.get(lastDays).add(getString(R.string.night_witch_two));
				}
				break;
			case NIGHT_WITCH + 50:
				daysMsg.get(lastDays).add(getString(R.string.night_witch_three));
				break;
			case DAY_START_CHIEF:
				addGameMsgList();
				daysMsg.get(lastDays).add(getString(R.string.day_start));
				break;
			case DAY_START_MEET:
				if(roleNumber == WOLF)
					killSelf.setVisibility(View.GONE);
				break;
			case DAY_START_MEET+50:
				if(roleNumber == WOLF)
					killSelf.setVisibility(View.VISIBLE);
				break;
			default:
				break;
		}
	}
	@Override
	public void roleAction(String value) {
		switch (this.gameProcedure){
			case NIGHT_START:
				break;
			case NIGHT_THIEF:
				String[] roleSelect = value.split("\n");
				voteNumber = Integer.parseInt(roleSelect[0]);
				abstainedNumber = Integer.parseInt(roleSelect[1]);
				daysMsg.get(lastDays).add(getString(R.string.night_thief_two,allRoleMap.get(voteNumber),allRoleMap.get(abstainedNumber)));
				if(voteNumber == WOLF || abstainedNumber == WOLF){
					daysMsg.get(lastDays).add(getString(R.string.night_thief_wolf));
					server.sendFirstLevel(InternetService.GAME_VOTE,String.valueOf(WOLF));
				}else if(voteNumber == abstainedNumber){
					daysMsg.get(lastDays).add(getString(R.string.night_thief_same));
					server.sendFirstLevel(InternetService.GAME_VOTE,String.valueOf(voteNumber));
				}else {
					confirm.setText(allRoleMap.get(voteNumber));
					abandon.setText(allRoleMap.get(abstainedNumber));
					setVoteButton(30);
					voteNumber = Integer.parseInt(roleSelect[0]);
				}
				break;
			case NIGHT_GUARD:
				setVoteButton(30);
				break;
			case NIGHT_SEER:
				setVoteButton(30);
				break;
			case NIGHT_WOLF:
				String[] partner = value.split("\n");
				for (String other:partner) {
					setAllRoleImage(Integer.parseInt(other),WOLF);
					confirmRoleSeat.add(Integer.parseInt(other));
				}
				isTalkTime = true;
				voteTime = 20;
				daysMsg.get(lastDays).add(getString(R.string.night_wolf_ready));
				handler.sendEmptyMessage(COUNT_WOLF_TIME);
//				setVoteButton(120);
				break;
			//女巫解药毒药分开两个时间点
			case NIGHT_WITCH + 50:
				if(hasPoison) setVoteButton(30);
				else daysMsg.get(lastDays).add(getString(R.string.night_witch_no_poison));
				break;
		}
	}
	public void setVoteButton(int time){
		voteNumDisplay.setText("");
		voteNumber = -1;
		isVoteTime = true;
		voteTime = time;
		daysMsg.get(lastDays).add(getString(R.string.select_time,time+""));
		handler.sendEmptyMessage(COUNT_VOTE_TIME);
	}
	public void setCandidateBG(int who){
		//改变颜色以显示谁竞选警长，后续增加
//		seat[who].setBackgroundColor(Color.);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			showTips(getString(R.string.end_game_tips),3);
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void upDateRoomList(int what, String json) {
//		daysMsg.get(last).add();
		switch (what){
			case InternetService.ELECTION_SITUATION:
				if("non".equals(json)){
					daysMsg.get(lastDays).add(getString(R.string.day_start_chief_non));
				}else {
					StringBuilder sb = new StringBuilder();
					String[] candidate = json.split("\n");
					if(candidate.length == 1){
						int chief = Integer.parseInt(candidate[0]);
						daysMsg.get(lastDays).add(getString(R.string.day_start_chief_two, getString(R.string.number, (chief + 1)+"")));
						//只有一人竞选，自动当选
						daysMsg.get(lastDays).add(getString(R.string.only_one_election));
						setChief(chief);
					}else if(candidate.length == roomInfo.getMens()){
						daysMsg.get(lastDays).add(getString(R.string.all_player_election));
					}else {
						for (String can : candidate) {
							int num = Integer.parseInt(can);
							sb.append(getString(R.string.number, (num + 1)+""));
							if(!electionChiefSeat.contains(num))
								electionChiefSeat.add(num);
						}
						daysMsg.get(lastDays).add(getString(R.string.day_start_chief_two, sb.toString()));
					}
				}
				break;
			case InternetService.WHO_SPEAK:
				String[] speakMsg = json.split("\n");
				speaking = Integer.parseInt(speakMsg[0]);
				//说明是谁发言
				if("0".equals(speakMsg[1]))
					daysMsg.get(lastDays).add(getString(R.string.who_speak,(speaking+1)+""));
				else if("1".equals(speakMsg[1]))
					daysMsg.get(lastDays).add(getString(R.string.who_last_word,(speaking+1)+""));
				if(seatNumber == speaking){
					//所有发言都在这
					try {
						//唤醒录音
						if(roomInfo.getUseAudio() == 0)
							speech.startCord(false);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					daysMsg.get(lastDays).add(getString(R.string.your_turn));
					isTalkTime = true;
					endSpeak.setVisibility(View.VISIBLE);
					voteTime = 120;
					handler.sendEmptyMessage(COUNT_TALK_TIME);
				}
				break;
			case InternetService.SPEAK_STOP:
				daysMsg.get(lastDays).add(getString(R.string.speak_stop,getString(R.string.number,(speaking+1)+"")));
				speaking = -1;
				break;
			case InternetService.SAME_VOTE:
				if("non".equals(json))
					daysMsg.get(lastDays).add(getString(R.string.no_one_vote));
				else if("same".equals(json))
					daysMsg.get(lastDays).add(getString(R.string.same_vote_again));
				if(gameProcedure == DAY_START_CHIEF)
					daysMsg.get(lastDays).add(getString(R.string.no_vote_no_chief));
				else if(gameProcedure == DAY_START_MEET)
					daysMsg.get(lastDays).add(getString(R.string.no_vote_no_death));
				break;
			case InternetService.VOTE_S:
				StringBuilder sb1 = new StringBuilder();
				String[] voteSituation = json.split("\n");
				for (int i = 1;i<voteSituation.length;i++){
					sb1.append(getString(R.string.number,(Integer.parseInt(voteSituation[i])+1)+""));
				}
				daysMsg.get(lastDays).add(getString(R.string.vote_situation,(Integer.parseInt(voteSituation[0])+1)+"",sb1.toString()));
				sb1.setLength(0);
				break;
			case InternetService.WHO_CHIEF:
				if(json.length()>0)
					setChief(Integer.parseInt(json));
				else {
					daysMsg.get(lastDays).add(getString(R.string.please_transfer_chief));
					if (chiefIndex == seatNumber)
						setVoteButton(20);
				}
				break;
			case InternetService.WHO_ABANDON_ELECTION:
				daysMsg.get(lastDays).add(getString(R.string.who_abandon_election,(Integer.parseInt(json)+1)+""));
				if(!electionChiefSeat.contains(seatNumber))
					electionChiefSeat.remove(Integer.valueOf(json));
				break;
			case InternetService.END_GAME:
				String[] gameSituation = json.split("\n");
				daysMsg.get(lastDays).add(getString(R.string.game_over,allRoleMap.get(Integer.parseInt(gameSituation[0]))));
				for (int i = 0;i<roomInfo.getMens();){
					seat[i].setClickable(false);
					setAllRoleImage(i,Integer.parseInt(gameSituation[++i]));
				}
				try {
					if(roomInfo.getUseAudio() == 0)
						speech.endGame();
					isTalkTime = true;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			case InternetService.CANDIDATE_SPEAK:
				if(electionChiefSeat.contains(seatNumber)) {
					int a = Integer.parseInt(json);
					if (a == 0) {
						abandonElection.setVisibility(View.GONE);
					}else if (a == 1) {
						abandonElection.setVisibility(View.VISIBLE);
					}
				}
				break;
			case InternetService.WHO_KILL_SELF:
				int wolf = Integer.parseInt(json);
				daysMsg.get(lastDays).add(getString(R.string.wolf_kill_self,(wolf+1)+""));
				if(roleNumber == WOLF)
					killSelf.setVisibility(View.GONE);
				setAllRoleImage(wolf,WOLF);
				confirmRoleSeat.add(wolf);
				if(speaking == seatNumber)
					stopSpeak();
				//死法自爆
				break;
			case InternetService.HUNTER_SKILL:
				String[] hunterSkill = json.split("\n");
				int hunterNum = Integer.parseInt(hunterSkill[0]);
				if(hunterSkill.length == 1){
					daysMsg.get(lastDays).add(getString(R.string.hunter_skill_one,(hunterNum+1)+""));
					setAllRoleImage(hunterNum,HUNTER);
					confirmRoleSeat.add(hunterNum);
					if(roleNumber == HUNTER){
						//猎人技能选择
						setVoteButton(15);
					}
				}else {
					int hunterShoot = Integer.parseInt(hunterSkill[1]);
					if(hunterShoot == -1)
						daysMsg.get(lastDays).add(getString(R.string.hunter_skill_abandon, hunterNum+""));
					else {
						daysMsg.get(lastDays).add(getString(R.string.hunter_skill_two, (hunterNum + 1)+"", (hunterShoot + 1)+""));
						markDeath(hunterShoot);
					}
					//
				}
				break;
			case InternetService.IDIOT_SKILL:
				int idiotNum = Integer.parseInt(json);
				if(roleNumber == IDIOT)
					idiotSkill = false;
				daysMsg.get(lastDays).add(getString(R.string.idiot_skill,(idiotNum+1)+""));
				setAllRoleImage(idiotNum,IDIOT);
				deathPlayerSeat.remove((Integer)idiotNum);
				seat[idiotNum].setClickable(true);
				((ImageView)seat[idiotNum].findViewById(R.id.dead_way)).setImageResource(R.drawable.idiot_exile);
				confirmRoleSeat.add(idiotNum);
				break;
			case InternetService.EMOJI_MSG:
				String[] emojiMsg = json.split("\n");
				showEmoji(Integer.parseInt(emojiMsg[0]), Integer.parseInt(emojiMsg[1]));
				break;
		}
	}
	public void setChief(int chief){
		if(chiefIndex != -1)
			seat[chiefIndex].findViewById(R.id.isChief).setVisibility(View.GONE);
		chiefIndex = chief;
		if(chiefIndex == -1)
			daysMsg.get(lastDays).add(getString(R.string.no_chief));
		else {
			seat[chiefIndex].findViewById(R.id.isChief).setVisibility(View.VISIBLE);
			daysMsg.get(lastDays).add(getString(R.string.chief_con, getString(R.string.number, (chief + 1)+"")));
		}
	}
	@Override
	public void makeDialog(int roomID) {

	}

	@Override
	public void upDateRoomTalk(String msg) {
		daysMsg.get(lastDays).add(msg);
	}

	@Override
	public void replyVote(String re) {
		int reply = -1;
		if(roleNumber != SEER && roleNumber != WITCH) reply = Integer.parseInt(re);
		switch (roleNumber){
			case GUARD:
				if(reply == -1)
					daysMsg.get(lastDays).add(getString(R.string.night_guard_abandon));
				else daysMsg.get(lastDays).add(getString(R.string.night_guard_two, (++reply)+""));
				break;
			case SEER:
				String[] checkMsg = re.split("\n");
				reply = Integer.parseInt(checkMsg[0]);
				if(reply == -1)
					daysMsg.get(lastDays).add(getString(R.string.night_seer_abandon));
				else {
					int s = Integer.parseInt(checkMsg[1]);
					daysMsg.get(lastDays).add(getString(R.string.night_seer_two, allRoleMap.get(reply)));
					//更换验人身份图标，并不允许再次更换
					setAllRoleImage(s,reply);
					confirmRoleSeat.add(s);
				}
				break;
			case WOLF:
				if(reply == -1)
					daysMsg.get(lastDays).add(getString(R.string.night_wolf_abandon));
				else daysMsg.get(lastDays).add(getString(R.string.night_wolf_two, (reply+1)+""));
				isTalkTime = false;
				break;
			case WITCH:
				String[] witchMsg = re.split("\n");
				reply = Integer.parseInt(witchMsg[0]);
				int p = Integer.parseInt(witchMsg[1]);
				String[] msg = new String[2];
				if(reply == -1)
					msg[0] = getString(R.string.night_witch_m_abandon);
				else msg[0] = getString(R.string.night_witch_m,(reply+1)+"");
				if(p == -1)
					msg[1] = getString(R.string.night_witch_p_abandon);
				else msg[1] = getString(R.string.night_witch_p,(p+1)+"");
				daysMsg.get(lastDays).add(getString(R.string.night_witch_reply,msg[0],msg[1]));
				break;
		}
	}
	public void markDeath(int who){
		deathPlayerSeat.add(who);
		((ImageView)seat[who].findViewById(R.id.dead_way)).setImageResource(R.drawable.deathmark);
		if(seatNumber == who){
			//关闭所有操作
			sendEmoji.setClickable(false);
		}
	}
	@Override
	public void tellWhoDead(int... deadSeat) {
		//标志谁死，非女巫时期死后位置不可再点击
		if (gameProcedure == NIGHT_WITCH) {
			voteNumber = deadSeat[0];
			if (voteNumber == -1) {
				daysMsg.get(lastDays).add(getString(R.string.night_witch_one_p));
				server.sendFirstLevel(InternetService.GAME_VOTE, String.valueOf(voteNumber));
			} else {
				daysMsg.get(lastDays).add(getString(R.string.night_witch_one_s, (deadSeat[0]+1)+""));
				//判断是否能自救
				if(voteNumber != seatNumber || roomInfo.getSaveSelfDays() > lastDays) {
					daysMsg.get(lastDays).add(getString(R.string.night_witch_two));
					setVoteButton(30);
					voteNumber = deadSeat[0];
				}else daysMsg.get(lastDays).add(getString(R.string.night_witch_cannot));
			}
		}else {
			//加上被放逐判断
			if (deadSeat[1] == -1) {
				if(deadSeat[0] == -1)
					daysMsg.get(lastDays).add(getString(R.string.piece_night));
				else {
					daysMsg.get(lastDays).add(getString(R.string.single_die, (deadSeat[0] + 1)+""));
					seat[deadSeat[0]].setClickable(false);
					markDeath(deadSeat[0]);
				}
			}else if(deadSeat[1] == -2){
				if(deadSeat[0] == -1)
					daysMsg.get(lastDays).add(getString(R.string.vote_no_die));
				else {
					daysMsg.get(lastDays).add(getString(R.string.vote_die, (deadSeat[0] + 1)+""));
					seat[deadSeat[0]].setClickable(false);
					markDeath(deadSeat[0]);
				}
			}else  {
				daysMsg.get(lastDays).add(getString(R.string.double_die, (deadSeat[0] + 1)+"", (deadSeat[1] + 1)+""));
				seat[deadSeat[0]].setClickable(false);
				seat[deadSeat[1]].setClickable(false);
				markDeath(deadSeat[0]);
				markDeath(deadSeat[1]);
			}
			//天亮
		}
	}

	@Override
	public void election() {
		daysMsg.get(lastDays).add(getString(R.string.day_start_chief_one));
		setVoteButton(20);
		voteNumber = seatNumber;
	}

	@Override
	public void voteElection() {
		if (gameProcedure == DAY_START_CHIEF) {
			daysMsg.get(lastDays).add(getString(R.string.vote_election));
			if (!electionChiefSeat.contains(seatNumber) && !deathPlayerSeat.contains(seatNumber) && idiotSkill)
				setVoteButton(20);
		} else if (gameProcedure == DAY_START_MEET) {
			daysMsg.get(lastDays).add(getString(R.string.vote_to_die));
			if (!deathPlayerSeat.contains(seatNumber) && idiotSkill)
				setVoteButton(20);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		daysSelectItem = i;
		gameMsg.setAdapter(daysMsg.get(i));
		daysAdapter.notifyDataSetInvalidated();
//		daysAdapter.notifyDataSetChanged();
	}

	class daysDisplayAdapter extends BaseAdapter {
		public daysDisplayAdapter() {
			// TODO Auto-generated constructor stub
		}
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ViewHolder holder;
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(GamingActivity.this);
				convertView = inflater.inflate(R.layout.days_item, null);
				holder = new ViewHolder();
				holder.textView = (TextView) convertView.findViewById(R.id.days_display);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.textView.setText((String) list.get(position).get("days"));
			if (position == daysSelectItem)
				holder.textView.setTextColor(Color.BLUE);
			else holder.textView.setTextColor(Color.DKGRAY);
			return convertView;
		}
	}
	final class ViewHolder{
		TextView textView;
	}
}