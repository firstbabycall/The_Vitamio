package com.example.mycamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

public class VideoButton extends View {
    private Paint mPaint;
    private boolean whetherRecorder=true;
    private Rect rect;
    public VideoButton(Context context) {
        super(context);
        init();
    }

    public VideoButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    private void init() {
        mPaint=new Paint();
        mPaint.setStrokeWidth(10);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL);


    }
    public void setRestartDraw(boolean position){
        whetherRecorder=position;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width=getWidth();
        int height=getHeight();
        int radius=Math.min(width,height)/2;
        if (whetherRecorder){
            canvas.drawCircle(width/2,height/2,radius-16,mPaint);
        }else{
            canvas.drawRect(40,40,width-40,height-40,mPaint);
        }
    }
}
