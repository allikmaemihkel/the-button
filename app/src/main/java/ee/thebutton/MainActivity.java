package ee.thebutton;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

public final class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int BG=0xff080b12,CARD=0xff121824,MUTED=0xff8b97ae,WHITE=0xfff2f4ff,RED=0xffff405c;
    private LinearLayout root,content;
    private SharedPreferences prefs;
    private EditText address;
    private Switch overlaySwitch;
    private boolean syncing;
    private int tab;
    private String draft;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);State.migrate(this);prefs=State.prefs(this);
        tab=saved==null?0:saved.getInt("tab",0);draft=saved==null?prefs.getString("webUrl",""):saved.getString("draft","");
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        setContentView(root);root.requestApplyInsets();prefs.registerOnSharedPreferenceChangeListener(this);render();
    }
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color,int radius,int stroke) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    private TextView text(String value,int size,int color) { TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t; }
    private TextView label(String value) { TextView t=text(value,11,MUTED);t.setLetterSpacing(.16f);return t; }
    private void gap(LinearLayout parent,int height) { parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height))); }
    private LinearLayout card() {
        LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.VERTICAL);view.setPadding(dp(18),dp(18),dp(18),dp(18));view.setBackground(shape(CARD,20,0xff283145));content.addView(view,new LinearLayout.LayoutParams(-1,-2));return view;
    }
    private TextView action(String title,boolean primary,Runnable run) {
        TextView t=text(title,14,WHITE);t.setTypeface(null,Typeface.BOLD);t.setGravity(Gravity.CENTER);t.setPadding(dp(12),dp(16),dp(12),dp(16));t.setBackground(shape(primary?RED:0xff1c2536,14,primary?0:0xff344158));t.setOnClickListener(v->run.run());t.setFocusable(true);return t;
    }
    private void render() {
        if(address!=null)draft=address.getText().toString();address=null;overlaySwitch=null;root.removeAllViews();
        TextView brand=text("●  THE BUTTON                       V 2.0",14,WHITE);brand.setLetterSpacing(.13f);brand.setPadding(dp(24),dp(24),dp(24),dp(20));root.addView(brand);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(24),dp(12),dp(24),dp(28));scroll.addView(content);
        content.addView(label(tab==0?"VEEBILEHT → SINU TAUST":"SINU NUPP. SINU REEGLID."));gap(content,12);
        TextView title=text(tab==0?"Kleebi link.\nAnna taustale elu.":"Üks nupp.\nSinu juhtimine.",32,WHITE);title.setTypeface(null,Typeface.BOLD);content.addView(title);gap(content,12);
        content.addView(text(tab==0?"Sinu lemmikveeb otse telefoni taustal.":"Muuda vajutuste tegevusi ja nupu välimust.",14,MUTED));gap(content,24);
        if(tab==0)home();else controls();
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(16),dp(12),dp(16),dp(12));nav.setBackgroundColor(0xff0e131e);
        for(int i=0;i<2;i++) { final int selected=i;TextView item=action(i==0?"◉  Veebitaust":"⌘  Juhtimine",i==tab,()->{tab=selected;render();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(i>0)lp.leftMargin=dp(12);nav.addView(item,lp); }root.addView(nav);sync();
    }
    private void home() {
        LinearLayout link=card();link.addView(label("01 / VEEBILEHE AADRESS"));gap(link,14);
        address=new EditText(this);address.setTextColor(WHITE);address.setHintTextColor(MUTED);address.setTextSize(16);address.setSingleLine(true);address.setHint("https://sinu-veeb.ee");address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);address.setText(draft);address.setSelectAllOnFocus(true);address.setPadding(dp(12),dp(12),dp(12),dp(12));address.setBackground(shape(BG,12,0xff344158));address.setContentDescription("Veebilehe link");link.addView(address,new LinearLayout.LayoutParams(-1,dp(56)));gap(link,12);
        link.addView(action("Kleebi kopeeritud link",false,()-> {
            ClipboardManager clipboard=getSystemService(ClipboardManager.class);ClipData clip=clipboard.getPrimaryClip();
            if(clip==null||clip.getItemCount()==0) { toast("Kopeeri kõigepealt brauserist veebilehe link.");return; }
            CharSequence value=clip.getItemAt(0).coerceToText(this);address.setText(value==null?"":value);address.setSelection(address.length());
        }));gap(link,12);
        link.addView(action("Salvesta ja proovi veebilehte  ↗",true,()->{if(saveLink())startActivity(new Intent(this,WebActivity.class));}));
        gap(content,18);LinearLayout setup=card();setup.addView(label("02 / MÄÄRA TELEFONI TAUSTAKS"));gap(setup,12);
        setup.addView(text("Veeb töötab avakuva ikoonide taga. Punane nupp avab selle täisekraanil kasutamiseks.",14,MUTED));gap(setup,16);
        setup.addView(action("Määra veebileht taustaks",true,()->{if(saveLink())setWallpaper();}));gap(content,18);
        LinearLayout button=card();overlaySwitch=toggle(button,"Ujuv punane nupp","Ava ja sulge veeb igalt ekraanilt",ButtonService.isRunning,this::setOverlay);
        gap(content,18);content.addView(action("Proovi lingita näidislehte",false,()->{
            draft="";address.setText("");prefs.edit().putString("webUrl","").putBoolean("demoSelected",true).apply();WebSession.get(this).loadConfigured();startActivity(new Intent(this,WebActivity.class));
        }));gap(content,14);content.addView(text("Juhtimisrežiimis saad kerida, linke vajutada ja kirjutada. Ülemine punane nupp viib tagasi. Kui peidad ujuva nupu, saad selle siit uuesti sisse lülitada.",12,MUTED));
    }
    private boolean saveLink() {
        try {
            if(address.getText().toString().trim().isEmpty()&&prefs.getBoolean("demoSelected",false))return true;
            String url=WebAddress.normalize(address.getText().toString());address.setText(url);draft=url;
            prefs.edit().putString("webUrl",url).putBoolean("demoSelected",false).apply();
            getSystemService(InputMethodManager.class).hideSoftInputFromWindow(address.getWindowToken(),0);return true;
        } catch(IllegalArgumentException e) { address.setError(e.getMessage());return false; }
    }
    private void controls() {
        String[] names={"Üks vajutus","Topeltvajutus","Pikk vajutus"},keys={"single","double","long"};int[] defaults={0,1,3};
        for(int i=0;i<3;i++) {
            final String key=keys[i],name=names[i];final int fallback=defaults[i];
            LinearLayout c=card();c.addView(label("0"+(i+1)+" / "+name.toUpperCase(java.util.Locale.ROOT)));gap(c,12);
            c.addView(action(State.ACTIONS[prefs.getInt(key,fallback)]+"  ▾",false,()->new AlertDialog.Builder(this).setTitle(name).setSingleChoiceItems(State.ACTIONS,prefs.getInt(key,fallback),(d,n)->{prefs.edit().putInt(key,n).apply();d.dismiss();render();}).setNegativeButton("Tühista",null).show()));gap(content,14);
        }
        LinearLayout appearance=card();appearance.addView(label("NUPU VÄLIMUS"));gap(appearance,14);slider(appearance,"Suurus","size",48,112,72," dp");slider(appearance,"Nähtavus","opacity",35,100,90,"%");
        gap(content,18);content.addView(text("Lohista ujuvat nuppu sobivasse kohta. Juhtimisvaate ülemine punane nupp sulgeb alati veebi. Mõned veebid piiravad sisseehitatud brausereid. Failide, kaamera ja mikrofoni kasutamiseks ava leht tavalises brauseris.",12,MUTED));
    }
    private interface Toggle { void change(boolean value); }
    private Switch toggle(LinearLayout parent,String title,String subtitle,boolean checked,Toggle change) {
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.addView(text(title,15,WHITE));gap(copy,4);copy.addView(text(subtitle,12,MUTED));row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        Switch sw=new Switch(this);sw.setContentDescription(title);sw.setChecked(checked);row.addView(sw);parent.addView(row);sw.setOnCheckedChangeListener((b,v)->{if(!syncing)change.change(v);});return sw;
    }
    private void slider(LinearLayout parent,String title,String key,int min,int max,int fallback,String unit) {
        TextView t=text(title+" · "+prefs.getInt(key,fallback)+unit,14,WHITE);parent.addView(t);SeekBar seek=new SeekBar(this);seek.setContentDescription(title);seek.setMax(max-min);seek.setProgress(prefs.getInt(key,fallback)-min);parent.addView(seek,new LinearLayout.LayoutParams(-1,dp(48)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int n,boolean user) { t.setText(title+" · "+(n+min)+unit);if(user)prefs.edit().putInt(key,n+min).apply(); }
            public void onStartTrackingTouch(SeekBar s) {}public void onStopTrackingTouch(SeekBar s) {}
        });
    }
    private void setWallpaper() {
        try { startActivity(new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,LiveWallpaper.class))); }
        catch(ActivityNotFoundException e) {
            try {startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));}
            catch(ActivityNotFoundException missing) {toast("Telefonis puudub elava tausta valija. Kontrolli telefoni taustaseadeid.");}
        }
    }
    private void setOverlay(boolean enable) {
        if(!enable) { stopService(new Intent(this,ButtonService.class));prefs.edit().putBoolean("overlay",false).apply();return; }
        if(!Settings.canDrawOverlays(this)) {
            sync();new AlertDialog.Builder(this).setTitle("Luba ujuv punane nupp").setMessage("Luba järgmisel ekraanil teiste rakenduste peal kuvamine. Tule tagasi ja lülita nupp sisse.")
                .setPositiveButton("Ava luba",(d,w)->{try {startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(ActivityNotFoundException e){toast("Ava telefoni seadetes rakenduse eriload.");}}).setNegativeButton("Mitte praegu",null).show();return;
        }
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},41);
        try {startForegroundService(new Intent(this,ButtonService.class));}catch(RuntimeException e){toast("Nuppu ei saanud käivitada. Kontrolli rakenduse õigusi.");sync();}
    }
    private void toast(String value) { Toast.makeText(this,value,Toast.LENGTH_LONG).show(); }
    private void sync() { syncing=true;if(overlaySwitch!=null)overlaySwitch.setChecked(ButtonService.isRunning&&Settings.canDrawOverlays(this));syncing=false; }
    @Override protected void onResume() {super.onResume();if(prefs!=null)sync();}
    @Override public void onSharedPreferenceChanged(SharedPreferences p,String key) {sync();}
    @Override protected void onSaveInstanceState(Bundle out) {super.onSaveInstanceState(out);out.putInt("tab",tab);out.putString("draft",address==null?draft:address.getText().toString());}
    @Override protected void onDestroy() {prefs.unregisterOnSharedPreferenceChangeListener(this);super.onDestroy();}
}
