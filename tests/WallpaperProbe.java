import android.os.IBinder;
import java.lang.reflect.Method;

/** Emulator-only shell probe, never packaged in the application. */
public class WallpaperProbe {
    public static void main(String[] args) throws Exception {
        Object binder=Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"wallpaper");
        Class<?> api=Class.forName("android.app.IWallpaperManager");
        Object manager=Class.forName("android.app.IWallpaperManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,binder);
        for(Method method:api.getMethods())if(method.getName().startsWith("setWallpaperComponent"))System.out.println(method);
        if(args.length>0) {
            Class<?> description=Class.forName("android.app.wallpaper.WallpaperDescription");
            Class<?> builderType=Class.forName("android.app.wallpaper.WallpaperDescription$Builder");
            Object builder=builderType.getConstructor().newInstance();
            android.content.ComponentName component="restore".equals(args[0])
                ? new android.content.ComponentName("com.android.systemui","com.android.systemui.wallpapers.ImageWallpaper")
                : new android.content.ComponentName("ee.thebutton","ee.thebutton.LiveWallpaper");
            builderType.getMethod("setComponent",android.content.ComponentName.class).invoke(builder,component);
            Object value=builderType.getMethod("build").invoke(builder);
            Method setter=api.getMethod("setWallpaperComponentChecked",description,String.class,int.class,int.class);
            setter.invoke(manager,value,"com.android.shell",1,0);
            System.out.println("Wallpaper component applied by emulator shell");
        }
    }
}
