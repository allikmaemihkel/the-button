package ee.thebutton;

import android.content.Context;
import android.graphics.*;
import android.view.View;

final class PowerButton extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    PowerButton(Context c) { super(c); setContentDescription("Punane juhtnupp. Lohista asukoha muutmiseks."); setClickable(true); }
    @Override protected void onDraw(Canvas canvas) {
        float x = getWidth()/2f, y = getHeight()/2f, r = Math.min(x,y)*.76f;
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(x,y,r*1.3f,new int[]{0x88ff304f,0x00ff304f},null,Shader.TileMode.CLAMP));
        canvas.drawCircle(x,y,r*1.3f,p);
        p.setShader(new LinearGradient(x,y-r,x,y+r,0xffff596c,0xffc51035,Shader.TileMode.CLAMP));
        canvas.drawCircle(x,y,r,p); p.setShader(null);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); p.setColor(0xffff8291); canvas.drawCircle(x,y,r,p);
        p.setColor(Color.WHITE); p.setStrokeWidth(r*.09f); p.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(new RectF(x-r*.37f,y-r*.37f,x+r*.37f,y+r*.37f),-52,284,false,p);
        canvas.drawLine(x,y-r*.48f,x,y-r*.03f,p); p.setStyle(Paint.Style.FILL);
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
