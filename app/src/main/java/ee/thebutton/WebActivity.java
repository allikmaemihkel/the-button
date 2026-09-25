package ee.thebutton;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.lang.ref.WeakReference;

/** A focusable window is required for keyboard input; the launcher keeps normal input in wallpaper mode. */
public final class WebActivity extends Activity {
    private static WeakReference<WebActivity> current=new WeakReference<>(null);
    private FrameLayout page;
    private TextView status;
    static boolean isOpen() { return current.get()!=null; }
    static boolean closeCurrent() {
        WebActivity activity=current.get();
        if(activity==null)return false;
        activity.finish();return true;
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff080b12);
        root.setOnApplyWindowInsetsListener((v,insets)-> {
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
        });
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),0,dp(4),0);
        TextView back=control("‹","Eelmine leht");back.setOnClickListener(v->{if(!WebSession.get(this).back())finish();});bar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(56)));
        status=new TextView(this);status.setTextSize(11);status.setTextColor(0xffcad4e8);status.setMaxLines(3);bar.addView(status,new LinearLayout.LayoutParams(0,-2,1));
        TextView refresh=control("↻","Laadi leht uuesti");refresh.setOnClickListener(v->WebSession.get(this).reload());bar.addView(refresh,new LinearLayout.LayoutParams(dp(48),dp(56)));
        PowerButton close=new PowerButton(this);close.setContentDescription("Lülita veebijuhtimine välja ja naase taustale");close.setOnClickListener(v->finish());bar.addView(close,new LinearLayout.LayoutParams(dp(60),dp(60)));
        root.addView(bar);page=new FrameLayout(this);root.addView(page,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);root.requestApplyInsets();
    }
    private TextView control(String title,String description) {
        TextView view=new TextView(this);view.setText(title);view.setContentDescription(description);view.setTextColor(0xffeaf0ff);view.setTextSize(26);view.setGravity(Gravity.CENTER);return view;
    }
    @Override protected void onResume() {
        super.onResume();current=new WeakReference<>(this);
        State.prefs(this).edit().putBoolean("webActive",true).apply();
        try { WebSession.get(this).attach(this,this,page,100,(address,message)->status.setText(address+(message.isEmpty()?"":"\n"+message))); }
        catch(RuntimeException error) { Toast.makeText(this,"Veebimootorit ei saanud avada. Kontrolli Android System WebView rakendust.",Toast.LENGTH_LONG).show();finish(); }
    }
    @Override protected void onPause() {
        if(current.get()==this)current.clear();
        InputMethodManager keyboard=getSystemService(InputMethodManager.class);
        keyboard.hideSoftInputFromWindow(page.getWindowToken(),0);
        WebSession.get(this).clearFocus();WebSession.get(this).detach(this);
        State.prefs(this).edit().putBoolean("webActive",false).apply();super.onPause();
    }
    @Override protected void onStop() { super.onStop();if(!isChangingConfigurations())finish(); }
    @Override protected void onDestroy() { WebSession.get(this).detach(this);super.onDestroy(); }
    @Override public void onBackPressed() { if(!WebSession.get(this).back())super.onBackPressed(); }
}
