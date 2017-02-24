package com.wenyi.mafia.mytools;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.wenyi.mafia.R;

/**
 *
 */
public class CustomToast {

	private Context context;
	private WindowManager wm;
	private long mDuration;
	private View mNextView;
	public static final long LENGTH_SHORT = 1500;
	public static final long LENGTH_LONG = 3000;
	public static final int CANCEL_TOAST = 1234;
	private boolean showing;
	private MyHandler handler;

	public boolean isShowing() {
		return showing;
	}

	public CustomToast(Context context) {
		this.context = context.getApplicationContext();
		wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		handler = new MyHandler(this);
	}

	public static CustomToast makeText(Context context, CharSequence text, long duration) {
		CustomToast result = new CustomToast(context);
		LayoutInflater inflate = (LayoutInflater) context.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflate.inflate(R.layout.transient_notification, null);
		TextView tv = (TextView) v.findViewById(R.id.message);
		tv.setText(text);
		result.mNextView = v;
		result.mDuration = duration;
		return result;
	}

	public static CustomToast makeText(Context context, int resId, long duration) {
		return makeText(context, context.getText(resId), duration);
	}

	public void show() {
		if (mNextView != null) {
			WindowManager.LayoutParams params = new WindowManager.LayoutParams();
			params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
			params.height = WindowManager.LayoutParams.WRAP_CONTENT;
			params.width = WindowManager.LayoutParams.WRAP_CONTENT;
			params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
			params.format = PixelFormat.TRANSLUCENT;
			params.windowAnimations = R.style.Animation_Toast;
			params.y = dip2px(context, 64);
			params.type = WindowManager.LayoutParams.TYPE_TOAST;  
			wm.addView(mNextView, params);
			showing = true;
			handler.sendEmptyMessageDelayed(0, mDuration);
		}
	}
	
	public void cancel() {
		handler.removeMessages(0);
		handler.sendEmptyMessage(0);
	}

	public void release(){
		handler.removeMessages(0);
		wm.removeView(mNextView);
		mNextView = null;
		wm = null;
		showing = false;
	}
	public static int dip2px(Context context, float dipValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dipValue * scale + 0.5f);
	}
	
	static class MyHandler extends Handler{
		WeakReference<CustomToast> weak;
		public MyHandler(CustomToast toast) {
			// TODO Auto-generated constructor stub
			weak = new WeakReference<>(toast);
		}
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			CustomToast toast = weak.get();
			if(toast != null){
				switch (msg.what) {
				case 0:
					if (toast.mNextView != null) {
						toast.wm.removeView(toast.mNextView);
						toast.mNextView = null;
						toast.wm = null;
						toast.showing = false;
					}
					break;

				default:
					break;
				}
			}
		}
	}
}
