package com.example.mycamera;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class MainMenuOfCamera extends LinearLayout implements View.OnClickListener {
    private TextView major,video,photograph,slow_motion,more;


    public MainMenuOfCamera(Context context) {
        super(context);
        Log.d("comeText","abc");

    }

    public MainMenuOfCamera(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LinearLayout.inflate(context,R.layout.main_menu,this);
        init();
        photograph.setSelected(true);
    }

    private void init() {
        major=findViewById(R.id.major);
        video=findViewById(R.id.video);
        photograph=findViewById(R.id.photograph);
        slow_motion=findViewById(R.id.slow_motion);
        more=findViewById(R.id.more);


        major.setOnClickListener(this);
        more.setOnClickListener(this);
        video.setOnClickListener(this);
        photograph.setOnClickListener(this);
        slow_motion.setOnClickListener(this);

    }


    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.major:
                major.setSelected(true);
                video.setSelected(false);
                photograph.setSelected(false);
                slow_motion.setSelected(false);
                more.setSelected(false);
                if(mListener!=null)
                    mListener.OnMainMenuClick(view,major.getWidth()*0);
                break;
            case R.id.video:
                major.setSelected(false);
                video.setSelected(true);
                photograph.setSelected(false);
                slow_motion.setSelected(false);
                more.setSelected(false);
                if(mListener!=null)
                    mListener.OnMainMenuClick(view,major.getWidth()*1);
                break;
            case R.id.photograph:
                major.setSelected(false);
                video.setSelected(false);
                photograph.setSelected(true);
                slow_motion.setSelected(false);
                more.setSelected(false);
                if(mListener!=null)
                    mListener.OnMainMenuClick(view,major.getWidth()*2);
                break;
            case R.id.slow_motion:
                major.setSelected(false);
                video.setSelected(false);
                photograph.setSelected(false);
                slow_motion.setSelected(true);
                more.setSelected(false);
                if(mListener!=null)
                    mListener.OnMainMenuClick(view,major.getWidth()*3);
                break;

            case R.id.more:
                major.setSelected(false);
                video.setSelected(false);
                photograph.setSelected(false);
                slow_motion.setSelected(false);
                more.setSelected(true);
                if(mListener!=null)
                    mListener.OnMainMenuClick(view,major.getWidth()*4);
                break;
        }
    }

    private OnMainMenuClickListener mListener;
    public void setOnMainMenuClickLisener(OnMainMenuClickListener listener){mListener=listener;}
    interface OnMainMenuClickListener{
        public void OnMainMenuClick(View view,int position);
    }

}
