package com.wenyi.mafia;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.wenyi.mafia.myservice.InternetService;
import com.wenyi.mafia.mytools.CustomToast;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GameRoomActivity extends Activity implements View.OnClickListener,InternetService.ServerListener,View.OnTouchListener {
	private TextView roomIDnum,roomName,roomMsg;
	private ImageView headImage,readyOrStart;
	private EditText talkMsg;
	private ServiceConnection sc;
	private InternetService server;
	private boolean isBinding = false,isRoomMaster = false,isReady = false;
	private RoomInfo roomInfo;
	private Gson gson = new Gson();
	private int[] readyID = {R.id.readyOne,R.id.readyTwo,R.id.readyThree,R.id.readyFour,R.id.readyFive,R.id.readySix,
			R.id.readySeven,R.id.readyEight,R.id.readyNine,R.id.readyTen,R.id.readyEleven,R.id.readyTwelve};
	private final String[] godNick = {"巫","猎","守","傻","盗","预"};
	private ImageView[] readySeat = new ImageView[12];
	private int seatNumber = -1;
	private ArrayAdapter<String> listAdapter;
	private String password;
	private ActionBar title;
	private final StringBuilder sb = new StringBuilder();
	private CustomToast toast;
	private MyHandler handler;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.readyroom);
		handler = new MyHandler(this);
		String json = getIntent().getStringExtra("roomJson");
		roomInfo = gson.fromJson(json,RoomInfo.class);
		title = getActionBar();
		findSeat(roomInfo.getMens());
		roomIDnum = (TextView) findViewById(R.id.roomID_number);
		roomName = (TextView) findViewById(R.id.roomName);
		roomMsg = (TextView) findViewById(R.id.roomMsg);
		headImage = (ImageView) findViewById(R.id.headSculpture);
		readyOrStart = (ImageView)findViewById(R.id.readyOrStart);
		readyOrStart.setOnTouchListener(this);
		findViewById(R.id.sendTalkMsg).setOnClickListener(this);
		readyOrStart.setOnClickListener(this);
		if(roomInfo.getMens() == 8){
			headImage.setImageResource(R.drawable.eight);
			findViewById(R.id.groupFive).setVisibility(View.GONE);
			findViewById(R.id.groupSix).setVisibility(View.GONE);
		}else headImage.setImageResource(R.drawable.twelve);
		talkMsg = (EditText) findViewById(R.id.talkText);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		listAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<String>());
		((ListView) findViewById(R.id.roomTalk)).setAdapter(listAdapter);
		bindReady();
		setHeadMsg();
	}
	public void findSeat(int mens){
		for (int i = 0;i<mens;i++) {
			readySeat[i] = (ImageView) findViewById(readyID[i]);
		}
	}
	public synchronized void setHeadMsg(){
		title.setTitle(roomInfo.getRoomName());
		roomIDnum.setText(getString(R.string.current,roomInfo.getCurrent()+"",roomInfo.getMens()+""));
		String[] useRole = roomInfo.getRoleMsg().split("\n");
		for (int i=0;i<6;i++){
			if("1".equals(useRole[i])){
				sb.append(godNick[i]);
				sb.append("、");
			}
		}
		sb.append(getString(R.string.how_many_wolf,(roomInfo.getWolfs()+1)+""));
		roomName.setText(sb.toString());
		sb.setLength(0);
		String wMsg,lMsg;
		if(roomInfo.getRoleMsg().startsWith("1") ){
			int i = roomInfo.getSaveSelfDays();
			if(i>0)
				wMsg = getString(R.string.display_save_self_days,i+"");
			else wMsg = getString(R.string.witch_no_save_self);
		}else wMsg = getString(R.string.no_witch);
		int j = roomInfo.getLastWordDays();
		if(j>0)
			lMsg = getString(R.string.display_last_word_days,j+"");
		else lMsg = getString(R.string.no_last_word);
		roomMsg.setText(getString(R.string.save_self_last_word,lMsg,wMsg));
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()){
			case R.id.sendTalkMsg:
				if("".equals(talkMsg.getText().toString().trim())){
					makeToast(getString(R.string.empty_talk));
				}else {
					String role;
					if (isRoomMaster) {
						role = "(房主):";
					} else {
						role = ":";
					}
					String msg = server.getNickName() + role + talkMsg.getText().toString();
					server.sendFirstLevel(InternetService.READY_TALK, msg);
					talkMsg.setText("");
					listAdapter.add(msg);
				}
				break;
			case R.id.readyOrStart:
				if(isReady){
					server.sendFirstLevel(InternetService.READY_GAME,InternetService.GAME_UNREADY);
				}else {
					server.sendFirstLevel(InternetService.READY_GAME,InternetService.GAME_READY);
				}
				break;
			default:
				break;
		}
	}

	public void bindReady() {
		sc = new ServiceConnection() {

			@Override
			public void onServiceDisconnected(ComponentName name) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				// TODO Auto-generated method stub
				server = ((InternetService.MyServiceBinder) service).getService();
				server.setServerListener(GameRoomActivity.this);
				seatNumber = server.getSeatNumber();
				server.sendFirstLevel(InternetService.REC_SEAT,String.valueOf(roomInfo.getRoomID()));
				password = server.getPassword();
//				if(seatNumber>=0)readySeat[seatNumber].setImageResource(R.drawable.me);
			}
		};
		Intent server = new Intent(this, InternetService.class);
		bindService(server, sc, Context.BIND_AUTO_CREATE);
		isBinding = true;
	}
	public void unBindReady(){
		unbindService(sc);
		isBinding = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.room_message, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//展示房间信息
		switch (item.getItemId()){
			case R.id.check_room:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
//				builder.setTitle("new men");
				RelativeLayout relativeLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.createroom,null);
				builder.setView(relativeLayout);
				final EditText name = (EditText) relativeLayout.findViewById(R.id.roomName);
				final RadioGroup group = (RadioGroup) relativeLayout.findViewById(R.id.mensGroup);
				final Switch useAudio = (Switch)relativeLayout.findViewById(R.id.using_audio);
				final Spinner godNum = (Spinner) relativeLayout.findViewById(R.id.god_num);
				final Spinner wolfNum = (Spinner) relativeLayout.findViewById(R.id.wolf_num);
				final TextView witchDaysOne = (TextView) relativeLayout.findViewById(R.id.days_one_two);
				final Spinner saveSelfDays = (Spinner) relativeLayout.findViewById(R.id.save_self_days);
				final TextView witchDaysTwo = (TextView) relativeLayout.findViewById(R.id.witch_save_two);
				final Spinner lastWordDays = (Spinner) relativeLayout.findViewById(R.id.last_word_days);
				final EditText roomPassword = (EditText) relativeLayout.findViewById(R.id.room_password);
				final CheckBox usePassword = (CheckBox)relativeLayout.findViewById(R.id.using_password);
				final CheckBox[] useRole = {(CheckBox)relativeLayout.findViewById(R.id.use_witch),(CheckBox)relativeLayout.findViewById(R.id.use_hunter),
						(CheckBox)relativeLayout.findViewById(R.id.use_guard),(CheckBox)relativeLayout.findViewById(R.id.use_idiot),
						(CheckBox)relativeLayout.findViewById(R.id.use_thief),(CheckBox)relativeLayout.findViewById(R.id.use_seer)};
				useRole[0].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
						if(b){
							witchDaysOne.setVisibility(View.VISIBLE);
							saveSelfDays.setVisibility(View.VISIBLE);
							witchDaysTwo.setVisibility(View.VISIBLE);
						}else {
							witchDaysOne.setVisibility(View.GONE);
							saveSelfDays.setVisibility(View.GONE);
							witchDaysTwo.setVisibility(View.GONE);
						}
					}
				});
				name.setText(roomInfo.getRoomName());
				((RadioButton) group.getChildAt(roomInfo.getMens() == 8 ? 0 : 1)).setChecked(true);
				useAudio.setChecked(roomInfo.getUseAudio() == 0);
				godNum.setSelection(roomInfo.getGods());
				wolfNum.setSelection(roomInfo.getWolfs());
				saveSelfDays.setSelection(roomInfo.getSaveSelfDays());
				lastWordDays.setSelection(roomInfo.getLastWordDays());
				String[] roles = roomInfo.getRoleMsg().split("\n");
				for (int i = 0; i < roles.length; i++) {
					useRole[i].setChecked("1".equals(roles[i]));
				}
				group.getChildAt(0).setEnabled(false);
				group.getChildAt(1).setEnabled(false);
				useAudio.setEnabled(false);
				if(isRoomMaster){
					roomPassword.setText(password);
					usePassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
							if(b)
								roomPassword.setVisibility(View.VISIBLE);
							else roomPassword.setVisibility(View.GONE);
						}
					});
					usePassword.setChecked(roomInfo.getHasPassword() == 0);
					builder.setPositiveButton(getString(R.string.change_room), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							StringBuilder sb = new StringBuilder();
							int i, j = group.getChildCount(), k, l, mens, gods, wolfs, selectGods = 0;
							for (i = 0; i < j; i++) {
								if (((RadioButton) group.getChildAt(i)).isChecked()) break;
							}
							k = i % j;
							mens = k == 0 ? 8 : 12;
							for (CheckBox role : useRole) {
								if (role.isChecked()) {
									sb.append("1");
									selectGods++;
								} else {
									sb.append("0");
								}
								sb.append("\n");
							}
							String abc = name.getText().toString().trim();
							if (useAudio.isChecked()) l = 0;
							else l = 1;
							gods = godNum.getSelectedItemPosition();
							wolfs = wolfNum.getSelectedItemPosition();
							if ("".equals(abc)){
								setDialogNotGone(dialog, false);
								GameRoomActivity.this.makeToast(getString(R.string.empty_game_title));
							}else if(selectGods != (gods + 1) || (gods + wolfs) >= (mens - 2)) {
								setDialogNotGone(dialog, false);
								GameRoomActivity.this.makeToast(getString(R.string.game_num_error));
							} else if (usePassword.isChecked() && roomPassword.length() < 3) {
								setDialogNotGone(dialog, false);
								GameRoomActivity.this.makeToast(getString(R.string.password_too_short));
							} else {
								setDialogNotGone(dialog, true);
								RoomInfo info = new RoomInfo(abc, "nick", sb.toString(), mens, l, roomInfo.getRoomID(), roomInfo.getCurrent(), gods, wolfs,
										saveSelfDays.getSelectedItemPosition(), lastWordDays.getSelectedItemPosition(), usePassword.isChecked() ? 0 : 1,0);
								if(!roomInfo.equals(info)) {
									String room = gson.toJson(info);
									server.sendFirstLevel(InternetService.CHANGE_ROOM, room + "\n" + roomPassword.getText().toString());
//									isLoading.setVisibility(View.VISIBLE);
								}
							}
							sb.setLength(0);
						}
						public void setDialogNotGone(DialogInterface dialog, boolean canGone){
							try {
								Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
								field.setAccessible(true);
								field.set(dialog, canGone);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}else {
					name.setEnabled(false);
					godNum.setEnabled(false);
					wolfNum.setEnabled(false);
					witchDaysOne.setEnabled(false);
					saveSelfDays.setEnabled(false);
					witchDaysTwo.setEnabled(false);
					lastWordDays.setEnabled(false);
					roomPassword.setEnabled(false);
					usePassword.setVisibility(View.GONE);
					for (int i = 0; i < roles.length; i++) {
						useRole[i].setEnabled(false);
					}
				}
				builder.create().show();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		if(!isBinding) bindReady();
		super.onResume();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
			server.setSeatNumber(-1);
			server.sendFirstLevel(InternetService.OUT_ROOM,String.valueOf(roomInfo.getRoomID()));
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		if(toast != null && toast.isShowing())
			toast.release();
		if(isBinding) unBindReady();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if(isBinding) unBindReady();
		super.onDestroy();
	}

	public static class MyHandler extends Handler {
		WeakReference<GameRoomActivity> weakReference;
		public MyHandler(GameRoomActivity activity){
			this.weakReference = new WeakReference<>(activity);
		}
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			GameRoomActivity activity = this.weakReference.get();
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
		for(int i=0;i<len;){
			int index = Integer.parseInt(msg[i++]);
			int type = Integer.parseInt(msg[i++]);
			if(index == seatNumber){
				if(type == 0) {
					readySeat[index].setImageResource(R.drawable.me);
					readyOrStart.setImageResource(R.drawable.confirm_ready);
					isReady = false;
					isRoomMaster = false;
				}else if(type == 1) {
					readySeat[index].setImageResource(R.drawable.me_ready);
					readyOrStart.setImageResource(R.drawable.cancel_ready);
					isReady = true;
					isRoomMaster = false;
				}else if(type == 2) {
					readySeat[index].setImageResource(R.drawable.me_master);
					readyOrStart.setImageResource(R.drawable.start_game);
					isRoomMaster = true;
				}
			}else {
				switch (type) {
					case -1:
						readySeat[index].setImageResource(R.drawable.non_seat);
						break;
					case 0:
						readySeat[index].setImageResource(R.drawable.other);
						break;
					case 1:
						readySeat[index].setImageResource(R.drawable.other_ready);
						break;
					case 2:
						readySeat[index].setImageResource(R.drawable.other_master);
						break;
				}
			}
		}
	}

	@Override
	public void setSeat(int num) {
		this.seatNumber = num;
//		readySeat[num].setImageResource(R.drawable.me);
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
//		RoomInfo info = gson.fromJson(json,RoomInfo.class);
		switch (what){
			case InternetService.PLUS_SUBTRACT_CURRENT:
//				this.roomInfo = info;
//				setHeadMsg();
				String[] roomMens = json.split("\n");
				roomInfo.setCurrent(Integer.parseInt(roomMens[1]));
				roomIDnum.setText(getString(R.string.current,roomMens[1],roomMens[2]));
				break;
			case InternetService.REC_PASSWORD:
				password = json;
				break;
			case InternetService.ROOM_MSG_CHANGE:
				if(!isRoomMaster)makeToast(getString(R.string.room_change_msg));
				this.roomInfo = gson.fromJson(json,RoomInfo.class);
				setHeadMsg();
				//更改准备状态
				break;
		}
	}

	@Override
	public void makeDialog(int roomID) {
		Intent intent = new Intent(this,GamingActivity.class);
		intent.putExtra("roomInfo",roomInfo);
		intent.putExtra("seat",seatNumber);
		startActivity(intent);
	}

	@Override
	public void upDateRoomTalk(String msg) {
//		list.add(msg);
//		listAdapter.notifyDataSetChanged();
		listAdapter.add(msg);
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

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		switch (view.getId()){
			case R.id.readyOrStart:
				if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
					readyOrStart.setAlpha(0.3f);
				else if(motionEvent.getAction() == MotionEvent.ACTION_UP)
					readyOrStart.setAlpha(1.0f);
				break;
		}
		return false;
	}
}
