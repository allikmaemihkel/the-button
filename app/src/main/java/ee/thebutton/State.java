package ee.thebutton;

import android.content.Context;
import android.content.SharedPreferences;

final class State {
    static final String[] ACTIONS = {"Veeb sisse / välja", "Laadi veeb uuesti", "Ava algne link", "Peida nupp"};
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("button", Context.MODE_PRIVATE); }
    static void migrate(Context c) {
        SharedPreferences p=prefs(c);
        if(p.getInt("schema",0)<2)p.edit().putInt("schema",2).putInt("single",0).putInt("double",1).putInt("long",3).apply();
    }
    static void action(Context c, int action) {
        SharedPreferences p = prefs(c);
        switch (action) {
            case 0:
                if(!WebActivity.closeCurrent()) c.startActivity(new android.content.Intent(c,WebActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case 1: WebSession.get(c).reload(); break;
            case 2: WebSession.get(c).loadConfigured(); break;
            case 3:
                WebActivity.closeCurrent();
                p.edit().putBoolean("overlay", false).apply();
                c.stopService(new android.content.Intent(c, ButtonService.class));
                break;
        }
    }
}
