package com.example.mycamera;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;

public class MyDailog extends Dialog implements View.OnClickListener {
    private ImageView photoImage,videoImage,cameraTime,watermark;
    private boolean photo=true;
    private boolean video=true;
    private SPUtils spUtils;
    private Context context;
    private boolean iftimer=true;
    private boolean ifwatermark=true;


    public MyDailog(@NonNull Context context) {
        super(context);
    }

    public MyDailog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        this.context=context;
        setContentView(R.layout.my_dialog);
        init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        spUtils=new SPUtils(context);
        if(spUtils.getGetBoolean("photo")){
            photoImage.setSelected(true);
            photo=false;
        }
        if(spUtils.getGetBoolean("video")){
            videoImage.setSelected(true);
            video=false;
        }
        if(spUtils.getGetBoolean("cameratime")){
            cameraTime.setSelected(true);
            iftimer=false;
        }
        if(spUtils.getGetBoolean("watermark")){
            watermark.setSelected(true);
            ifwatermark=false;
        }
    }

    private void init() {

        photoImage=findViewById(R.id.the_photo);
        videoImage=findViewById(R.id.the_video);
        cameraTime=findViewById(R.id.camera_timer);
        watermark=findViewById(R.id.watermark);

        watermark.setOnClickListener(this);
        cameraTime.setOnClickListener(this);
        photoImage.setOnClickListener(this);
        videoImage.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.the_photo:
                if(photo) {
                    photoImage.setSelected(photo);
                    if(mListener!=null)
                        mListener.OnphotoClick(photo);
                    photo = false;
                }
                else {
                    photoImage.setSelected(photo);
                    if(mListener!=null)
                        mListener.OnphotoClick(photo);
                    photo=true;
                }
                break;
            case R.id.the_video:
                if(video) {
                    videoImage.setSelected(video);
                    if(mListener!=null)
                        mListener.OnvideoClick(video);
                    video = false;
                }
                else {
                    videoImage.setSelected(video);
                    if(mListener!=null)
                        mListener.OnvideoClick(video);
                    video=true;
                }
                break;
            case R.id.camera_timer:
                if(iftimer) {
                    cameraTime.setSelected(iftimer);
                    if(mListener!=null)
                        mListener.cameraTime(iftimer);
                    iftimer = false;
                }
                else {
                    cameraTime.setSelected(iftimer);
                    if(mListener!=null)
                        mListener.cameraTime(iftimer);
                    iftimer=true;
                }
            case R.id.watermark:
                if(ifwatermark) {
                    watermark.setSelected(ifwatermark);
                    spUtils.setPutBoolean("watermark",true);
                    iftimer = false;
                }
                else {
                    watermark.setSelected(ifwatermark);
                    spUtils.deleteContent("watermark");
                    iftimer=true;
                }
                break;

        }
    }

    private OnItemClickListener mListener;
    public void setOnItemClickLisener(OnItemClickListener listener){mListener=listener;}
    interface OnItemClickListener{

        public void OnphotoClick(boolean position);
        public void cameraTime(boolean position);
        public void OnvideoClick(boolean position);

    }
}
