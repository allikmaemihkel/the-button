package ee.thebutton;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;

public final class ButtonService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    static boolean isRunning;
    private WindowManager manager;
    private WindowManager.LayoutParams layout;
    private PowerButton button;
    private boolean attached;
    private float downX, downY;
    private int originX, originY;
    private boolean dragging;
    private GestureDetector gestures;
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        State.migrate(this);
        if (intent != null && "stop".equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY; }
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("button", "Ujuv juhtnupp", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, ButtonService.class).setAction("stop"), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this,"button").setSmallIcon(R.drawable.ic_button)
                .setContentTitle("The Button on aktiivne").setContentText("Lohista nuppu või ava seadistused.")
                .setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Peida nupp",stop).build()).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(7, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(7, notification);
        if (!attached) showButton();
        return START_NOT_STICKY;
    }
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private void showButton() {
        SharedPreferences p = State.prefs(this);
        manager = getSystemService(WindowManager.class);
        int size = dp(p.getInt("size", 72));
        layout = new WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        layout.gravity = Gravity.TOP | Gravity.LEFT;
        layout.x = p.getInt("x", getResources().getDisplayMetrics().widthPixels - size - dp(12));
        layout.y = p.getInt("y", dp(330));
        clamp();
        button = new PowerButton(this); button.setAlpha(p.getInt("opacity",90)/100f);
        gestures = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapConfirmed(MotionEvent e) { if (!dragging) { button.performClick(); runBinding("single",0); } return true; }
            @Override public boolean onDoubleTap(MotionEvent e) { if (!dragging) runBinding("double",1); return true; }
            @Override public void onLongPress(MotionEvent e) { if (!dragging) runBinding("long",3); }
        });
        button.setOnClickListener(v -> { /* Touch gestures dispatch their configured binding. */ });
        button.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (action == android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) { runBinding("single",0); return true; }
                return super.performAccessibilityAction(host, action, args);
            }
        });
        button.setOnTouchListener((v,e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                downX=e.getRawX(); downY=e.getRawY(); originX=layout.x; originY=layout.y; dragging=false;
            }
            if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dx=e.getRawX()-downX, dy=e.getRawY()-downY;
                if (!dragging && Math.hypot(dx,dy)>ViewConfiguration.get(this).getScaledTouchSlop()) {
                    dragging=true;
                    MotionEvent cancel=MotionEvent.obtain(e); cancel.setAction(MotionEvent.ACTION_CANCEL); gestures.onTouchEvent(cancel); cancel.recycle();
                }
                if (dragging) { layout.x=originX+Math.round(dx); layout.y=originY+Math.round(dy); clamp(); if(attached) manager.updateViewLayout(button,layout); }
            }
            if (!dragging) gestures.onTouchEvent(e);
            if (e.getActionMasked() == MotionEvent.ACTION_UP && dragging) p.edit().putInt("x",layout.x).putInt("y",layout.y).apply();
            return true;
        });
        try {
            manager.addView(button,layout); attached=true; isRunning=true;
            button.setVisibility(WebActivity.isOpen()?View.GONE:View.VISIBLE);
            p.registerOnSharedPreferenceChangeListener(this); p.edit().putBoolean("overlay",true).apply();
        } catch (RuntimeException e) { stopSelf(); }
    }
    private void runBinding(String key,int fallback) { State.action(this,State.prefs(this).getInt(key,fallback)); }
    private void clamp() {
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        layout.x=Math.max(0,Math.min(layout.x,metrics.widthPixels-layout.width));
        layout.y=Math.max(0,Math.min(layout.y,metrics.heightPixels-layout.height-dp(48)));
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration config) { super.onConfigurationChanged(config); if(attached) { clamp(); manager.updateViewLayout(button,layout); } }
    @Override public void onSharedPreferenceChanged(SharedPreferences p,String key) {
        if (!attached) return;
        if ("size".equals(key)) { layout.width=layout.height=dp(p.getInt("size",72)); clamp(); manager.updateViewLayout(button,layout); }
        if ("opacity".equals(key)) button.setAlpha(p.getInt("opacity",90)/100f);
        if ("webActive".equals(key)) button.setVisibility(WebActivity.isOpen()?View.GONE:View.VISIBLE);
    }
    @Override public void onDestroy() {
        isRunning=false;
        if (gestures != null) {
            long now=SystemClock.uptimeMillis();
            MotionEvent cancel=MotionEvent.obtain(now,now,MotionEvent.ACTION_CANCEL,0,0,0);
            gestures.onTouchEvent(cancel); cancel.recycle();
        }
        State.prefs(this).unregisterOnSharedPreferenceChangeListener(this);
        if (attached) { manager.removeView(button); attached=false; }
        State.prefs(this).edit().putBoolean("overlay",false).apply();
        stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
}
