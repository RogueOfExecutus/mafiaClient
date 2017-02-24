package com.wenyi.mafia;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.wenyi.mafia.myrunnable.PeerTest;
import com.wenyi.mafia.myservice.InternetService;
import com.wenyi.mafia.mytools.CustomToast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener,AdapterView.OnItemClickListener,InternetService.ServerListener,
		View.OnTouchListener {
	private final static String[] LOBBY_TYPE = {"0","1"};
	private ActionBar actionBar;
	private Gson gson = new Gson();
	private MyHandler handler;
	public static final int EXIT_CODE = 1,TEST_PEER = 100,TEST_PEER_SUCCESS = 200;
	private int gameType = 0;
	private boolean isExit = false;
	private ListView gameList;
	private TextView lobbyTitle;
	private ViewPager viewPager;
	private ImageView newbie,newbieAudio,proficient;
	private ArrayList<View> views = new ArrayList<>();
	private ServiceConnection sc;
	private InternetService server;
	private boolean isBinding = false;
	private RelativeLayout isLoading;
	private String[] items = {"1","2","3","4","5","6","7"};
	private int[] targetView = {R.id.room_icon,R.id.room_name,R.id.room_field,R.id.room_people,R.id.room_witch_lastword,R.id.room_lock,R.id.room_state};
	private final String[] godNick = {"巫","猎","守","傻","盗","预"};
	private ArrayList<HashMap<String,Object>> list;
	private ArrayList<Integer> roomIDList = new ArrayList<>();
	private SimpleAdapter simpleAdapter;
	private final StringBuilder sb = new StringBuilder();
	private ProgressDialog testingPeer;
	private RelativeLayout waitRoomMsg;
	private CustomToast toast;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		viewPager = (ViewPager) findViewById(R.id.viewpager);
		views.add(getLayoutInflater().inflate(R.layout.gamelobby,null));
		views.add(getLayoutInflater().inflate(R.layout.me,null));
		viewPager.setAdapter(new MyPagerAdapter());
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				makeToast("onPageSelected" + position);
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});
		gameList = (ListView)views.get(0).findViewById(R.id.listView);
		lobbyTitle = (TextView) views.get(0).findViewById(R.id.lobby_title);
		newbie = (ImageView)views.get(0).findViewById(R.id.newbie_lobby);
		newbieAudio = (ImageView)views.get(0).findViewById(R.id.newbie_lobby_audio);
		proficient = (ImageView)views.get(0).findViewById(R.id.proficient_lobby);
		waitRoomMsg = (RelativeLayout) views.get(0).findViewById(R.id.wait_roomMsg);
		isLoading = (RelativeLayout) findViewById(R.id.isLoading);
		newbie.setOnClickListener(this);
		newbieAudio.setOnClickListener(this);
		proficient.setOnClickListener(this);
		newbie.setOnTouchListener(this);
		newbieAudio.setOnTouchListener(this);
		proficient.setOnTouchListener(this);
		actionBar = getActionBar();
		list = new ArrayList<>();
		simpleAdapter = new SimpleAdapter(this,list,R.layout.room_item,items,targetView);
		gameList.setAdapter(simpleAdapter);
		gameList.setOnItemClickListener(this);
		setHandler();
		setTestingPeer();
		if(!isBinding) bindMain();
	}
	public void setTestingPeer(){
		testingPeer = new ProgressDialog(this);
		testingPeer.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		testingPeer.setIndeterminate(false);
		testingPeer.setMessage(getString(R.string.testing_peer));
	}
	public void addList(RoomInfo info,int position){
		int mens = info.getMens();
		HashMap<String,Object> map = new HashMap<>();
		map.put(items[0],mens == 8 ? R.drawable.eight:R.drawable.twelve);
		map.put(items[1],info.getRoomName());
		String[] useRole = info.getRoleMsg().split("\n");
		for (int i=0;i<6;i++){
			if("1".equals(useRole[i])){
				sb.append(godNick[i]);
				sb.append("、");
			}
		}
		sb.append(getString(R.string.how_many_wolf,(info.getWolfs()+1)+""));
		map.put(items[2],sb.toString());
		sb.setLength(0);
		if(info.getGaming() == 0)
			map.put(items[3],getString(R.string.current,info.getCurrent()+"",mens+""));
		else map.put(items[6],R.drawable.room_state_conduct);
		String wMsg,lMsg;
		if(info.getRoleMsg().startsWith("1") ){
			int i = info.getSaveSelfDays();
			if(i>0)
				wMsg = getString(R.string.display_save_self_days,i+"");
			else wMsg = getString(R.string.witch_no_save_self);
		}else wMsg = getString(R.string.no_witch);
		int j = info.getLastWordDays();
		if(j>0)
			lMsg = getString(R.string.display_last_word_days,j+"");
		else lMsg = getString(R.string.no_last_word);
		map.put(items[4],getString(R.string.save_self_last_word,lMsg,wMsg));
		if(info.getHasPassword() == 0)
			map.put(items[5],R.drawable.lock);
		if(position == -1) {
			list.add(map);
			roomIDList.add(info.getRoomID());
		}else list.add(position,map);
	}
	public void unBindMain(){
		roomIDList.clear();
		list.clear();
		unbindService(sc);
		isBinding = false;
	}
	public void bindMain() {
		sc = new ServiceConnection() {

			@Override
			public void onServiceDisconnected(ComponentName name) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				// TODO Auto-generated method stub
				server = ((InternetService.MyServiceBinder) service).getService();
				server.setServerListener(MainActivity.this);
				server.sendFirstLevel(InternetService.GET_ROOM,"");
			}
		};
		Intent serverIntent = new Intent(this, InternetService.class);
		bindService(serverIntent, sc, Context.BIND_AUTO_CREATE);
		isBinding = true;
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if(toast != null && toast.isShowing())
			toast.release();
		if(isBinding) unBindMain();
		super.onPause();
	}
	@Override
	protected void onResume() {
		if(!isBinding) bindMain();
		super.onResume();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
			case R.id.add_game:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
//				builder.setTitle("new men");
				RelativeLayout relativeLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.createroom, null);
				builder.setView(relativeLayout);
				final EditText name = (EditText) relativeLayout.findViewById(R.id.roomName);
				final RadioGroup group = (RadioGroup) relativeLayout.findViewById(R.id.mensGroup);
				final Switch useAudio = (Switch) relativeLayout.findViewById(R.id.using_audio);
				final Spinner godNum = (Spinner) relativeLayout.findViewById(R.id.god_num);
				final Spinner wolfNum = (Spinner) relativeLayout.findViewById(R.id.wolf_num);
				final TextView witchDaysOne = (TextView) relativeLayout.findViewById(R.id.days_one_two);
				final Spinner saveSelfDays = (Spinner) relativeLayout.findViewById(R.id.save_self_days);
				final TextView witchDaysTwo = (TextView) relativeLayout.findViewById(R.id.witch_save_two);
				final Spinner lastWordDays = (Spinner) relativeLayout.findViewById(R.id.last_word_days);
				final EditText roomPassword = (EditText) relativeLayout.findViewById(R.id.room_password);
				final CheckBox usePassword = (CheckBox) relativeLayout.findViewById(R.id.using_password);
				// 根据gameType来决定能否使用语音
				useAudio.setChecked(gameType == 1);
				useAudio.setClickable(false);
				usePassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
						if (b)
							roomPassword.setVisibility(View.VISIBLE);
						else roomPassword.setVisibility(View.GONE);
					}
				});
//				final StringBuilder sb = new StringBuilder();
				final CheckBox[] useRole = {(CheckBox) relativeLayout.findViewById(R.id.use_witch), (CheckBox) relativeLayout.findViewById(R.id.use_hunter),
						(CheckBox) relativeLayout.findViewById(R.id.use_guard), (CheckBox) relativeLayout.findViewById(R.id.use_idiot),
						(CheckBox) relativeLayout.findViewById(R.id.use_thief), (CheckBox) relativeLayout.findViewById(R.id.use_seer)};
				useRole[0].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
						if (b) {
							witchDaysOne.setVisibility(View.VISIBLE);
							saveSelfDays.setVisibility(View.VISIBLE);
							witchDaysTwo.setVisibility(View.VISIBLE);
						} else {
							witchDaysOne.setVisibility(View.GONE);
							saveSelfDays.setVisibility(View.GONE);
							witchDaysTwo.setVisibility(View.GONE);
						}
					}
				});
				String abc = this.getFilesDir() + "/roomCache.info";
				File file = new File(abc);
				if (file.exists()) {
					try {
						InputStream in = openFileInput("roomCache.info");
						byte[] bytes = new byte[256];
//	                	StringBuffer sb = new StringBuffer();
						int len;
						while ((len = in.read(bytes)) != -1) {
							sb.append(new String(bytes, 0, len));
						}
						RoomInfo info = gson.fromJson(sb.toString(), RoomInfo.class);
						name.setText(info.getRoomName());
						((RadioButton) group.getChildAt(info.getMens() == 8 ? 0 : 1)).setChecked(true);
//						useAudio.setChecked(info.getUseAudio() == 0);
						godNum.setSelection(info.getGods());
						wolfNum.setSelection(info.getWolfs());
						usePassword.setChecked(info.getHasPassword() == 0);
						saveSelfDays.setSelection(info.getSaveSelfDays());
						lastWordDays.setSelection(info.getLastWordDays());
						String[] roles = info.getRoleMsg().split("\n");
						for (int i = 0; i < roles.length; i++) {
							useRole[i].setChecked("1".equals(roles[i]));
						}
						in.close();
						sb.setLength(0);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					((RadioButton) group.getChildAt(0)).setChecked(true);
				}
				builder.setPositiveButton(getString(R.string.create), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
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
						if("".equals(abc)){
							setDialogNotGone(dialog, false);
							MainActivity.this.makeToast(getString(R.string.empty_game_title));
						} else if (selectGods != (gods + 1) || (gods + wolfs) >= (mens - 2)) {
							setDialogNotGone(dialog, false);
							MainActivity.this.makeToast(getString(R.string.game_num_error));
						} else if (usePassword.isChecked() && roomPassword.length() < 3) {
							setDialogNotGone(dialog, false);
							MainActivity.this.makeToast(getString(R.string.password_too_short));
						} else {
							setDialogNotGone(dialog, true);
							RoomInfo roomInfo = new RoomInfo(abc, "nick", sb.toString(), mens, l, 0, 1, gods, wolfs,
									saveSelfDays.getSelectedItemPosition(), lastWordDays.getSelectedItemPosition(), usePassword.isChecked() ? 0 : 1,0);
							String info = gson.toJson(roomInfo);
							saveInfo(info);
							server.sendFirstLevel(InternetService.CREATE_ROOM, info + "\n" + roomPassword.getText().toString());
							isLoading.setVisibility(View.VISIBLE);
						}
						sb.setLength(0);
					}

					void setDialogNotGone(DialogInterface dialog, boolean canGone){
						try {
							Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
							field.setAccessible(true);
							field.set(dialog, canGone);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					void saveInfo(String info) {
						try {
							OutputStream out = openFileOutput("roomCache.info", Context.MODE_PRIVATE);
							byte[] bytes = info.getBytes();
							out.write(bytes, 0, bytes.length);
							out.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						try {
							Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
							field.setAccessible(true);
							field.set(dialog, true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				AlertDialog dialog = builder.create();
				dialog.setCanceledOnTouchOutside(false);
				dialog.show();
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
			if(isExit){
				handler.removeMessages(EXIT_CODE);
				server.sendFirstLevel(InternetService.LOGOUT,"");
				sendBroadcast(new Intent("closeAPP"));
//				finish();
			}else {
				isExit = true;
				System.out.println("click exit");
				makeToast(getString(R.string.exit_tips));
				handler.sendEmptyMessageDelayed(EXIT_CODE, 2000);
				return false;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	public void setHandler(){
		handler = new MyHandler(this);
	}
	@Override
	public void makeToast(String msg){
		if(msg == null) return;
		isLoading.setVisibility(View.GONE);
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
		isLoading.setVisibility(View.GONE);
		Intent intent = new Intent(this,GameRoomActivity.class);
		intent.putExtra("roomJson",roomJson);
		if(isBinding) unBindMain();
		startActivity(intent);
	}

	@Override
	public void getRoomList(String... msg) {
		list.clear();
		roomIDList.clear();
		if(msg != null) {
			RoomInfo info;
			for (String json : msg) {
				info = gson.fromJson(json, RoomInfo.class);
				if (info != null) addList(info, -1);
			}
		}
		simpleAdapter.notifyDataSetChanged();
		waitRoomMsg.setVisibility(View.GONE);
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
		switch (what) {
			case InternetService.ADD_DELETE_ROOM:
				RoomInfo info = gson.fromJson(json,RoomInfo.class);
				addList(info, -1);
				simpleAdapter.notifyDataSetChanged();
				break;
			case InternetService.PLUS_SUBTRACT_CURRENT:
				String[] roomMens = json.split("\n");
				int index = roomIDList.indexOf(Integer.parseInt(roomMens[0]));
				if(index == -1) break;
				int current = Integer.parseInt(roomMens[1]);
				if(current>0){
					if(list.get(index).containsKey(items[6])) break;
					list.get(index).put(items[3],getResources().getString(R.string.current,roomMens[1],roomMens[2]));
				}else {
					list.remove(index);
					roomIDList.remove(index);
				}
				simpleAdapter.notifyDataSetChanged();
				break;
			case InternetService.ROOM_MSG_CHANGE:
				RoomInfo upDateInfo = gson.fromJson(json,RoomInfo.class);
				int position = roomIDList.indexOf(upDateInfo.getRoomID());
				if(position != -1) list.remove(position);
				addList(upDateInfo, position);
				simpleAdapter.notifyDataSetChanged();
				//更新list
				break;
		}
	}

	@Override
	public void makeDialog(int roomID) {
		int position = roomIDList.indexOf(roomID);
		if(position == -1) return;
		list.get(position).put(items[6],R.drawable.room_state_conduct);
		list.get(position).remove(items[3]);
		list.add(list.remove(position));
		simpleAdapter.notifyDataSetChanged();
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

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		switch (view.getId()){
			case R.id.newbie_lobby:
				if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
					newbie.setAlpha(0.3f);
				else if(motionEvent.getAction() == MotionEvent.ACTION_UP)
					newbie.setAlpha(1.0f);
				break;
			case R.id.newbie_lobby_audio:
				if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
					newbieAudio.setAlpha(0.3f);
				else if(motionEvent.getAction() == MotionEvent.ACTION_UP)
					newbieAudio.setAlpha(1.0f);
				break;
			case R.id.proficient_lobby:
				if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
					proficient.setAlpha(0.3f);
				else if(motionEvent.getAction() == MotionEvent.ACTION_UP)
					proficient.setAlpha(1.0f);
				break;
		}
		return false;
	}

//	float x,y;
//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		if(event.getAction() == MotionEvent.ACTION_DOWN){
//			x = event.getX();
//			y = event.getY();
//			makeToast("touch"+x);
//		}else if(event.getAction() == MotionEvent.ACTION_UP){
//			if((x - event.getX()) > 200.0f){
//				finish();
//			}
//			x = 0;
//			y = 0;
//		}
//		return super.onTouchEvent(event);
//	}

	class MyPagerAdapter extends PagerAdapter{
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = views.get(position);
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(views.get(position));
        }

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==object;
        }
    }
	public static class MyHandler extends Handler {
	    WeakReference<MainActivity> weakReference;
	    MyHandler(MainActivity activity){
	        this.weakReference = new WeakReference<>(activity);
	    }
	    @Override
	    public void handleMessage(Message msg) {
	        super.handleMessage(msg);
			MainActivity activity = this.weakReference.get();
	        if(activity!=null){
	        	switch (msg.what) {
					case EXIT_CODE:
						activity.isExit = false;
						break;
					case TEST_PEER:
						if(activity.testingPeer.isShowing())
							activity.testingPeer.dismiss();
						activity.makeToast(activity.getString(R.string.test_failed));
						break;
					case TEST_PEER_SUCCESS:
						activity.testSuccess();
						break;
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
	final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
	@Override
	public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode){
			case REQUEST_CODE_ASK_PERMISSIONS:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
					testPeer();
				else makeToast(getString(R.string.refuse_audio));
				break;
		}
	}
	public void testPeer(){
		if(gameType != 1){
			new Thread(new PeerTest().setTestResult(handler)).start();
			testingPeer.show();
			handler.sendEmptyMessageDelayed(TEST_PEER,5*1000);
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.newbie_lobby:
			makeGameType(0);
			break;
		case R.id.newbie_lobby_audio:
			int hasAudioPermission = ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO);
			if(hasAudioPermission != PackageManager.PERMISSION_GRANTED){
				ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.RECORD_AUDIO},
						REQUEST_CODE_ASK_PERMISSIONS);
				break;
			}
			testPeer();
			break;
		case R.id.proficient_lobby:
//			list.clear();
//			simpleAdapter.notifyDataSetChanged();
//			server.sendFirstLevel(InternetService.CHANGE_GAME_TYPE,LOBBY_TYPE[1]);
			makeToast(getString(R.string.wait_for_dev));
			break;
		default:
			break;
		}
	}
	public void testSuccess() {
		if(testingPeer.isShowing()){
			testingPeer.dismiss();
			handler.removeMessages(TEST_PEER);
		}
		makeToast(getString(R.string.test_success));
		makeGameType(1);
	}
	public void makeGameType(int type){
		if(gameType != type){
			waitRoomMsg.setGravity(View.VISIBLE);
			gameType = type;
			changeLobby(gameType);
			list.clear();
			roomIDList.clear();
			simpleAdapter.notifyDataSetChanged();
			server.sendFirstLevel(InternetService.CHANGE_GAME_TYPE, LOBBY_TYPE[type]);
			if(type == 0) lobbyTitle.setText(getString(R.string.newbie_no_audio_title));
			else if(type == 1) lobbyTitle.setText(getString(R.string.newbie_audio_title));
		}
	}
	public void changeLobby(int type){
		if(type == 0){
			newbie.setImageResource(R.drawable.newbie_noaudio);
			newbieAudio.setImageResource(R.drawable.newbie_audio_noselect);
		}else if(type == 1){
			newbie.setImageResource(R.drawable.newbie_noaudio_noselect);
			newbieAudio.setImageResource(R.drawable.newbie_audio);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
//		dialog展示 密码填写
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String msg;
		if(list.get(position).containsKey(items[6])) {
			msg = getString(R.string.room_gaming);
			builder.setMessage(msg);
		} else {
			msg = getString(R.string.into_room_tips);
			builder.setMessage(msg);
			final int roomID = roomIDList.get(position);
			if(list.get(position).containsKey(items[5])){
				RelativeLayout relativeLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.into_password,null);
				builder.setView(relativeLayout);
				final EditText pWord = (EditText) relativeLayout.findViewById(R.id.input_password);
				builder.setPositiveButton(getString(R.string.into_room), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int i) {
						if(pWord.length()<3){
							try {
								Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
								field.setAccessible(true);
								field.set(dialog, false);
								MainActivity.this.makeToast(getString(R.string.password_too_short));
							}catch (Exception e){
								e.printStackTrace();
							}
						}else {
							try {
								Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
								field.setAccessible(true);
								field.set(dialog, true);
							}catch (Exception e){
								e.printStackTrace();
							}
							server.sendFirstLevel(InternetService.INTO_ROOM, roomID + "\n" + pWord.getText().toString());
							isLoading.setVisibility(View.VISIBLE);
						}
					}
				});
			} else {
				builder.setPositiveButton(getString(R.string.into_room), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int i) {
						server.sendFirstLevel(InternetService.INTO_ROOM, roomID + "\n1");
						isLoading.setVisibility(View.VISIBLE);
					}
				});
			}
		}
		builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				try {
					Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
					field.setAccessible(true);
					field.set(dialog, true);
				}catch (Exception e){
					e.printStackTrace();
				}
			}
		});
		builder.create().show();
	}
}