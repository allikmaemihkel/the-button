package ee.thebutton;

import android.app.Presentation;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.service.wallpaper.WallpaperService;
import android.view.*;
import android.widget.FrameLayout;

/** Renders an attached, hardware-accelerated WebView to the wallpaper surface. */
public final class LiveWallpaper extends WallpaperService {
    @Override public Engine onCreateEngine() { return new WebEngine(); }
    private final class WebEngine extends Engine {
        private boolean visible,surfaceReady;
        private int width,height;
        private VirtualDisplay display;
        private Presentation presentation;
        @Override public void onVisibilityChanged(boolean value) { visible=value;if(value)openDisplay();else releaseDisplay(); }
        @Override public void onSurfaceChanged(SurfaceHolder holder,int format,int w,int h) {
            super.onSurfaceChanged(holder,format,w,h);releaseDisplay();width=w;height=h;surfaceReady=true;openDisplay();
        }
        private void openDisplay() {
            if(!visible||!surfaceReady||width<=0||height<=0||display!=null)return;
            try {
                display=getSystemService(DisplayManager.class).createVirtualDisplay("The Button wallpaper",width,height,
                    getResources().getDisplayMetrics().densityDpi,getSurfaceHolder().getSurface(),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY|DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION);
                if(display==null)throw new IllegalStateException("Virtual display unavailable");
                presentation=new Presentation(LiveWallpaper.this,display.getDisplay());
                Window window=presentation.getWindow();
                int flags=WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    |WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                window.setFlags(flags,flags);
                window.setBackgroundDrawableResource(android.R.color.black);
                FrameLayout frame=new FrameLayout(presentation.getContext());frame.setBackgroundColor(0xff080b12);
                presentation.setContentView(frame);presentation.show();window.setLayout(-1,-1);
                WebSession.get(LiveWallpaper.this).attach(this,presentation.getContext(),frame,isPreview()?20:10,null);
            } catch(RuntimeException error) {
                releaseDisplay();Canvas c=null;
                try {
                    c=getSurfaceHolder().lockCanvas();
                    if(c!=null) { c.drawColor(0xff080b12);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.WHITE);p.setTextSize(18*getResources().getDisplayMetrics().scaledDensity);c.drawText("Veebitausta ei saanud avada.",24,height/2f,p); }
                } finally { if(c!=null)getSurfaceHolder().unlockCanvasAndPost(c); }
            }
        }
        private void releaseDisplay() {
            WebSession.get(LiveWallpaper.this).detach(this);
            if(presentation!=null) { presentation.dismiss();presentation=null; }
            if(display!=null) { display.release();display=null; }
        }
        @Override public void onSurfaceDestroyed(SurfaceHolder holder) { surfaceReady=false;releaseDisplay();super.onSurfaceDestroyed(holder); }
        @Override public void onDestroy() { releaseDisplay();super.onDestroy(); }
    }
}
