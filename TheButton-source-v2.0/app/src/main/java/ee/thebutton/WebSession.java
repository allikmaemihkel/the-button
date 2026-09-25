package ee.thebutton;

import android.content.*;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.*;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.FrameLayout;
import java.util.ArrayList;

/** Moves one WebView between wallpaper and input Activity, preserving DOM and scroll. */
final class WebSession implements SharedPreferences.OnSharedPreferenceChangeListener {
    interface Status { void update(String address,String message); }
    private static WebSession instance;
    static WebSession get(Context c) {
        if(instance==null)instance=new WebSession(c.getApplicationContext());
        return instance;
    }
    private static final class Host {
        final Object owner;final Context context;final FrameLayout frame;final int priority;final Status status;
        Host(Object o,Context c,FrameLayout f,int p,Status s) { owner=o;context=c;frame=f;priority=p;status=s; }
    }
    private final Context app;
    private final ArrayList<Host> hosts=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private MutableContextWrapper context;
    private WebView web;
    private Host current;
    private String address="Näidisleht",message="";
    private boolean failed;
    private Bundle saved;
    private final Runnable releaseIdle=()-> {
        if(current!=null||web==null)return;
        saved=new Bundle();web.saveState(saved);web.destroy();web=null;context=null;
    };
    private WebSession(Context app) { this.app=app;State.prefs(app).registerOnSharedPreferenceChangeListener(this); }
    void attach(Object owner,Context c,FrameLayout frame,int priority,Status status) {
        hosts.removeIf(h->h.owner==owner);hosts.add(new Host(owner,c,frame,priority,status));select();
    }
    void detach(Object owner) { hosts.removeIf(h->h.owner==owner);select(); }
    private void select() {
        Host next=null;
        for(Host h:hosts)if(next==null||h.priority>=next.priority)next=h;
        if(next==current)return;
        handler.removeCallbacks(releaseIdle);
        if(web!=null&&web.getParent()!=null)((ViewGroup)web.getParent()).removeView(web);
        current=next;
        if(next==null) {
            if(web!=null) { web.clearFocus();web.onPause();web.pauseTimers();context.setBaseContext(app); }
            handler.postDelayed(releaseIdle,120000);return;
        }
        ensureWeb(next.context);context.setBaseContext(next.context);
        next.frame.addView(web,new FrameLayout.LayoutParams(-1,-1));
        web.onResume();web.resumeTimers();report();
    }
    @SuppressWarnings("deprecation")
    private void ensureWeb(Context hostContext) {
        if(web!=null)return;
        context=new MutableContextWrapper(hostContext);web=new WebView(context);web.setBackgroundColor(0xff080b12);
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);settings.setUseWideViewPort(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view,int progress) {
                if(progress<100&&!failed) { message="Laadimine "+progress+"%";report(); }
            }
            @Override public void onPermissionRequest(PermissionRequest request) { request.deny(); }
            @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb) { cb.invoke(origin,false,false); }
            @Override public boolean onShowFileChooser(WebView view,ValueCallback<android.net.Uri[]> cb,FileChooserParams params) {
                cb.onReceiveValue(null);message="Faili lisamiseks ava veebileht tavalises brauseris.";report();return true;
            }
        });
        web.setDownloadListener((url,agent,disposition,mime,length)-> { message="Failide allalaadimiseks ava link tavalises brauseris.";report(); });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request) {
                if(WebAddress.allowed(request.getUrl().toString()))return false;
                message="See link ei ole veebiaadress (http/https).";report();return true;
            }
            @Override public void onPageStarted(WebView view,String url,Bitmap icon) { failed=false;address=url;message="Laadimine…";report(); }
            @Override public void onPageFinished(WebView view,String url) {
                address=url;if(!failed&&message.startsWith("Laadimine"))message="";
                CookieManager.getInstance().flush();report();
            }
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error) {
                if(request.isForMainFrame()) { failed=true;message="Lehte ei saanud laadida. Kontrolli internetti või linki; proovi ↻.";report(); }
            }
            @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,WebResourceResponse response) {
                if(request.isForMainFrame()) { failed=true;message="Veebiserveri vastus: "+response.getStatusCode()+". Proovi ↻.";report(); }
            }
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error) {
                handler.cancel();failed=true;message="Veebilehe turvasertifikaat ei ole kehtiv.";report();
            }
            @Override public boolean onRenderProcessGone(WebView view,RenderProcessGoneDetail detail) {
                if(view.getParent()!=null)((ViewGroup)view.getParent()).removeView(view);
                view.destroy();web=null;saved=null;
                message="Veebimootor peatus. Vajuta ↻, et leht uuesti avada.";report();return true;
            }
        });
        if(saved==null||web.restoreState(saved)==null)loadConfigured();
        saved=null;
    }
    void loadConfigured() {
        saved=null;
        if(web==null) { restartIfVisible();return; }
        String url=State.prefs(app).getString("webUrl","");message="Laadimine…";
        if(url.isEmpty())web.loadDataWithBaseURL("https://thebutton.invalid/",DEMO,"text/html","UTF-8",null);
        else if(WebAddress.allowed(url))web.loadUrl(url);
        report();
    }
    void reload() { if(web==null)restartIfVisible();else if("https://thebutton.invalid/".equals(web.getUrl()))loadConfigured();else web.reload(); }
    private void restartIfVisible() { if(current!=null&&web==null) { current=null;select(); } }
    boolean back() { if(web!=null&&web.canGoBack()) { web.goBack();return true; }return false; }
    void clearFocus() { if(web!=null)web.clearFocus(); }
    private void report() { if(current!=null&&current.status!=null)current.status.update(address,message); }
    @Override public void onSharedPreferenceChanged(SharedPreferences p,String key) { if("webUrl".equals(key))loadConfigured(); }
    private static final String DEMO="<!doctype html><html lang='et'><meta name='viewport' content='width=device-width,initial-scale=1'><style>"
        +"*{box-sizing:border-box}body{margin:0;background:#080b18;color:#eff3ff;font:18px system-ui;padding:12vh 28px;min-height:170vh;background-image:radial-gradient(ellipse at 60% 25%,#352064,transparent 60%)}"
        +"small{letter-spacing:4px;color:#ff6077}h1{font-size:48px;line-height:1.1}p{color:#aebbd2;line-height:1.6}input,button{padding:16px;border:1px solid #536080;border-radius:16px;background:#18203a;color:white;font:inherit;width:100%;margin:8px 0}button{background:#ef3553}.orb{width:140px;height:140px;border:2px solid #ff526c;border-radius:50%;box-shadow:0 0 80px #9d3bfd77;margin:40px auto;animation:pulse 4s infinite alternate}@keyframes pulse{to{transform:scale(1.2);box-shadow:0 0 110px #ff345599}}"
        +"</style><small>THE BUTTON / VEEBITAUST</small><h1>Sinu veeb.<br>Sinu ekraan.</h1><div class='orb'></div><p>See on võrguühenduseta näidis. Kleebi äpi avalehele enda veebilehe link.</p><input placeholder='Proovi siia kirjutada' aria-label='Proovi kirjutamist'><button onclick=\"this.textContent='Töötab! '+new Date().toLocaleTimeString()\">Proovi vajutust</button><p style='margin-top:30vh'>Ka kerimine töötab. Punane nupp sulgeb juhtimise ja jätab lehe taustaks.</p></html>";
}
