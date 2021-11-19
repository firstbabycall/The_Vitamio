package com.example.mycamera;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MyDailog.OnItemClickListener, CameraController.OnCameraControllerListener, MainMenuOfCamera.OnMainMenuClickListener {
    private static final int REQUEST_CAMERA_PERMISSION = 0;
    private ImageView mFlashview;
    private LinearLayout major_menu;
    private MainMenuOfCamera indicator;
    private Button flash_cloose;
    private Button flash_open;
    private Button flash_ai;
    private Button flash_all;

    private int minWidth;
    private MyDailog myDailog;
    private float jilu=0;
    private SPUtils spUtils;
    private PhotoButton photoButton;
    private VideoButton videoButton;
    private ImageView changeCamera;
    private ImageView thumbnail;
    private TextView countdown;
    private AutoFitTextureView textureView;
    private int mPhoneOrientation;
    public static final int ORIENTATION_HYSTERESIS = 5;

    private CameraController controller;
    private File mFile;
    private MyOrientationEventListener mMyOrientationEventListener;
    private boolean mIsRecording=false;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private int time;


    private boolean isImageThumbnail=false;

    private Handler handler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            countdown.setText(String.valueOf(time));
            if(time==0) {
                controller.setFilePath(mFile);
                controller.captureStillPicture();
                countdown.setVisibility(View.GONE);
                if(spUtils.getGetBoolean("photo")){
                    setMediaPlay(R.raw.photo_success);
                }
            }
        }
    };
    private boolean mIsMenuOpen=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainMenuOfCamera mainMenuOfCamera=new MainMenuOfCamera(this);
        init();
        Display display = getWindowManager().getDefaultDisplay();
        minWidth=display.getWidth()/5*2;
        Log.d("comeText","getMidde="+minWidth);
        flash_cloose.setSelected(true);
        myDailog=new MyDailog(MainActivity.this,R.style.MyDialog);

        controller=new CameraController(this,textureView,mBackgroundHandler);
        controller.setOnCameraControllerListener(this);

        mMyOrientationEventListener = new MyOrientationEventListener(this);

        spUtils=new SPUtils(this);
    }


    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        mMyOrientationEventListener.enable();
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateThumbnailView();
            }
        }).start();
        if (textureView.isAvailable()) {
            controller.openCamera();
        } else {
            //第二步
            textureView.setSurfaceTextureListener(textturelistener);
        }

    }



    @Override
    protected void onPause() {
        super.onPause();
        if(myDailog.isShowing())
            myDailog.dismiss();
        mMyOrientationEventListener.disable();
        controller.closeSessionDevice();
        if (controller.isRecorderRunning) {
            controller.mMediaRecorder.stop();
            controller.mMediaRecorder.reset();
            mIsRecording = false;
            videoButton.setRestartDraw(true);
            videoButton.invalidate();
        }
    }

    private void setMediaPlay(int path){
        MediaPlayer mediaPlayer =MediaPlayer.create(this,path);
        new  Thread(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.start();
                try {
                    Thread.sleep(1000);
                    mediaPlayer.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }


    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, REQUEST_CAMERA_PERMISSION);
    }



    TextureView.SurfaceTextureListener textturelistener=new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            controller.openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    private void init() {

        indicator=findViewById(R.id.indicator);
        photoButton=findViewById(R.id.put_photo);
        videoButton=findViewById(R.id.put_video);
        textureView=findViewById(R.id.preview);
        changeCamera=findViewById(R.id.change_camera_id);
        thumbnail=findViewById(R.id.thumbnail);
        countdown=findViewById(R.id.countdown);
        mFlashview=findViewById(R.id.mflash_lamp);
        flash_cloose=findViewById(R.id.flash_cloose);
        flash_open=findViewById(R.id.flash_open);
        flash_ai=findViewById(R.id.flash_ai);
        flash_all=findViewById(R.id.flash_all);
        major_menu=findViewById(R.id.major_menu);

        thumbnail.setOnClickListener(this);
        flash_all.setOnClickListener(this);
        flash_cloose.setOnClickListener(this);
        flash_open.setOnClickListener(this);
        flash_ai.setOnClickListener(this);
        mFlashview.setOnClickListener(this);
        changeCamera.setOnClickListener(this);
        photoButton.setOnClickListener(this);
        videoButton.setOnClickListener(this);
        indicator.setOnMainMenuClickLisener(this);
    }

    private void setAnimation(float f) {
        ObjectAnimator objectAnimator=ObjectAnimator.ofFloat(indicator,"translationX",jilu,minWidth-f,minWidth-f);
        objectAnimator.setDuration(1000);
        objectAnimator.start();
        jilu=minWidth-f;
    }

    private void setChangeCameraAnimation() {
        ObjectAnimator rotation=ObjectAnimator.ofFloat(changeCamera,"rotation",0,180);
        rotation.setDuration(1000);
        rotation.start();
    }

    public void oPenMenu() {
        mFlashview.setVisibility(View.GONE);
        doAnimateOpen(flash_cloose, 0);
        doAnimateOpen(flash_open, 1);
        doAnimateOpen(flash_ai, 2);
        doAnimateOpen(flash_all, 3);

    }

    public void closeMenu() {
        mFlashview.setVisibility(View.GONE);
        doAnimateCloose(flash_cloose, 0);
        doAnimateCloose(flash_open, 1);
        doAnimateCloose(flash_ai, 2);
        doAnimateCloose(flash_all, 3);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        flash_cloose.setVisibility(View.GONE);
                        flash_open.setVisibility(View.GONE);
                        flash_ai.setVisibility(View.GONE);
                        flash_all.setVisibility(View.GONE);
                        mFlashview.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();


    }

    private void doAnimateCloose(Button view, int i){
        if (view.getVisibility()==View.VISIBLE){
            view.setVisibility(View.VISIBLE);
        }
        AnimatorSet set = new AnimatorSet();
        Display display = getWindowManager().getDefaultDisplay();
        int translationx =(display.getWidth()/4)*(i);
        set.playTogether(ObjectAnimator.ofFloat(view, "translationX", translationx, 0));
        set.setDuration(300).start();
    }

    private void doAnimateOpen(Button view, int i) {
        if (view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
        AnimatorSet set = new AnimatorSet();
        Display display = getWindowManager().getDefaultDisplay();
        int translationx =(display.getWidth()/4)*(i);
        set.playTogether(ObjectAnimator.ofFloat(view, "translationX", 0, translationx));
        set.setDuration(300).start();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View view) {


        switch(view.getId()){
            case R.id.thumbnail:
                gotoGallery();
                break;
            case R.id.flash_cloose:
                mFlashview.setImageResource(R.drawable.flashoff_selecton);
                flash_cloose.setSelected(true);
                flash_open.setSelected(false);
                flash_ai.setSelected(false);
                flash_all.setSelected(false);
                closeMenu();
                mIsMenuOpen = false;
                controller.changeFlashMode("off");
                Toast.makeText(this,"闪光灯关闭",Toast.LENGTH_SHORT).show();
                break;
            case R.id.flash_open:
                mFlashview.setImageResource(R.drawable.flashon_selecton);
                flash_cloose.setSelected(false);
                flash_open.setSelected(true);
                flash_ai.setSelected(false);
                flash_all.setSelected(false);
                closeMenu();
                mIsMenuOpen = false;
                controller.changeFlashMode("on");
                Toast.makeText(this,"闪光灯打开",Toast.LENGTH_SHORT).show();
                break;
            case R.id.flash_ai:
                mFlashview.setImageResource(R.drawable.flashai_selecton);
                flash_cloose.setSelected(false);
                flash_open.setSelected(false);
                flash_ai.setSelected(true);
                flash_all.setSelected(false);
                closeMenu();
                mIsMenuOpen = false;
                controller.changeFlashMode("auto");
                Toast.makeText(this,"闪光灯自动",Toast.LENGTH_SHORT).show();
                break;
            case R.id.flash_all:
                mFlashview.setImageResource(R.drawable.flashall);
                flash_cloose.setSelected(false);
                flash_open.setSelected(false);
                flash_ai.setSelected(false);
                flash_all.setSelected(true);
                closeMenu();
                mIsMenuOpen = false;
                controller.changeFlashMode("torch");
                Toast.makeText(this,"闪光灯长亮",Toast.LENGTH_SHORT).show();
                break;

            case R.id.mflash_lamp:
                if (!mIsMenuOpen) {
                    mIsMenuOpen = true;
                    oPenMenu();
                } else {
                    mIsMenuOpen = false;
                    closeMenu();
                }
                break;
//            case R.id.major:
//                setAnimation(major.getWidth()*0);
//                major.setSelected(true);
//                video.setSelected(false);
//                photograph.setSelected(false);
//                slow_motion.setSelected(false);
//                more.setSelected(false);
//                major_menu.setVisibility(View.VISIBLE);
//                break;
//            case R.id.video:
//                setAnimation(major.getWidth()*1);
//                major.setSelected(false);
//                video.setSelected(true);
//                photograph.setSelected(false);
//                slow_motion.setSelected(false);
//                more.setSelected(false);
//                videoButton.setVisibility(View.VISIBLE);
//                controller.previewRatioCHange(1.777f);
//                break;
//            case R.id.photograph:
//                setAnimation(major.getWidth()*2);
//                major.setSelected(false);
//                video.setSelected(false);
//                photograph.setSelected(true);
//                slow_motion.setSelected(false);
//                more.setSelected(false);
//                videoButton.setVisibility(View.GONE);
//                controller.previewRatioCHange(1.333f);
//                break;
//            case R.id.slow_motion:
//                setAnimation(major.getWidth()*3);
//                major.setSelected(false);
//                video.setSelected(false);
//                photograph.setSelected(false);
//                slow_motion.setSelected(true);
//                more.setSelected(false);
//                break;
//
//            case R.id.more:
//                setAnimation(major.getWidth()*4);
//                major.setSelected(false);
//                video.setSelected(false);
//                photograph.setSelected(false);
//                slow_motion.setSelected(false);
//                more.setSelected(true);
//                setDaiLog();
//                break;
            case R.id.put_photo:
                mFile=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),"/Camera/"+getShowFileName()+".jpg");

                if (spUtils.getGetBoolean("cameratime")){
                    countdown.setVisibility(View.VISIBLE);
                    setdelay();
                }else{
                    controller.setFilePath(mFile);
                    controller.captureStillPicture();
                    if(spUtils.getGetBoolean("photo")){
                        setMediaPlay(R.raw.photo_success);
                    }
                }


                break;
            case R.id.put_video:
                if(mIsRecording) {
                    //停止录像

                    controller.stopRecord();
                    if(spUtils.getGetBoolean("video")){
                        setMediaPlay(R.raw.recording_stop);
                    }
                    mIsRecording = false;
                    videoButton.setRestartDraw(true);
                    videoButton.invalidate();
                } else {
                    mIsRecording = true;
                    controller.startRecord();
                    if(spUtils.getGetBoolean("video")){
                        setMediaPlay(R.raw.recording_start);
                    }
                    videoButton.setRestartDraw(false);
                    videoButton.invalidate();
                }

                break;
            case R.id.change_camera_id:
                controller.frontBackChangeId();
                setChangeCameraAnimation();
                break;
        }
    }

    private void gotoGallery() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        //缩略图是否生成了
        if (null == mFile) return;
        Uri uri = Uri.fromFile(mFile);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/jpeg");
        startActivity(intent);

    }

    private void setdelay() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (time=3;time>=0;time--) {
                        handler.sendEmptyMessage(0);
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void setDaiLog() {
        myDailog.setCanceledOnTouchOutside(true);
        myDailog.setOnItemClickLisener(this);
        myDailog.show();
    }


    public static String getShowFileName() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        StringBuffer fileName = new StringBuffer();
        fileName.append(calendar.get(Calendar.YEAR));
        if (calendar.get(Calendar.MONTH) + 1 < 10) {
            fileName.append("0");
        }
        fileName.append(calendar.get(Calendar.MONTH) + 1);
        if (calendar.get(Calendar.DAY_OF_MONTH) < 10) {
            fileName.append("0");
        }
        fileName.append(calendar.get(Calendar.DAY_OF_MONTH));
        fileName.append("_");
        if (calendar.get(Calendar.HOUR_OF_DAY) < 10) {
            fileName.append("0");
        }
        fileName.append(calendar.get(Calendar.HOUR_OF_DAY));
        if (calendar.get(Calendar.MINUTE) < 10) {
            fileName.append("0");
        }
        fileName.append(calendar.get(Calendar.MINUTE));
        if (calendar.get(Calendar.SECOND) < 10) {
            fileName.append("0");
        }
        fileName.append(calendar.get
                (Calendar.SECOND));
        return "MIG_" + fileName.toString();
    }




    @Override
    public void OnphotoClick(boolean position) {
        if(position){
            spUtils.setPutBoolean("photo",true);
        }else{
            spUtils.deleteContent("photo");
        }
    }

    @Override
    public void cameraTime(boolean position) {
        if(position){
            spUtils.setPutBoolean("cameratime",true);
        }else{
            spUtils.deleteContent("cameratime");
        }

    }

    @Override
    public void OnvideoClick(boolean position) {
        if(position){
            spUtils.setPutBoolean("video",true);
        }else{
            spUtils.deleteContent("video");
        }
    }


    @Override
    public void onCameraSizeSelectFinish(Size previewSize) {
        textureView.setAspectRatio(previewSize.getHeight(),previewSize.getWidth());
    }

    @Override
    public void onSaveImageFinish(Bitmap bitmap) {
        thumbnail.setImageBitmap(bitmap);
    }

    @Override
    public void OnMainMenuClick(View view,int position) {
        if (major_menu.getVisibility()==View.VISIBLE)
            major_menu.setVisibility(View.GONE);
        switch (view.getId()){

            case R.id.major:
                setAnimation(position);
                major_menu.setVisibility(View.VISIBLE);
                break;
            case R.id.video:
                setAnimation(position);
                videoButton.setVisibility(View.VISIBLE);
                controller.previewRatioCHange(1.777f);
                break;
            case R.id.photograph:
                setAnimation(position);
                videoButton.setVisibility(View.GONE);
                controller.previewRatioCHange(1.333f);
                break;
            case R.id.slow_motion:
                setAnimation(position);
                break;

            case R.id.more:
                setAnimation(position);

                setDaiLog();
                break;
        }
    }

    private class MyOrientationEventListener
            extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN) {
                return;
            }


            mPhoneOrientation = roundOrientation(orientation, mPhoneOrientation);


            controller.setPhoneDeviceDegree(mPhoneOrientation);
        }
    }

    public int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }


    private void updateThumbnailView() {
        Uri targetUrl = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri targetVideoUrl = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver resolver = getContentResolver();
        Cursor imagesCursor = resolver.query(targetUrl, new String[]{
                        MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID}, null, null,
                null);

            if (imagesCursor != null) {
                if (imagesCursor.moveToLast()) {
                    @SuppressLint("Range") long imageId = imagesCursor.getInt(imagesCursor.getColumnIndex(MediaStore.Images.Media._ID));
                    @SuppressLint("Range") String filePathImage = imagesCursor.getString(imagesCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    Log.d("comeOnTest","filePathImage = " + filePathImage);
                    if (filePathImage.contains("DCIM/Camera") && filePathImage.contains(".jpg")) {
                        mFile = new File(filePathImage);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 1;
                        Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(resolver, imageId, MediaStore.Images.Thumbnails.MINI_KIND, options);
                        if (bitmap != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    thumbnail.setImageBitmap(bitmap);
                                }
                            });
                            imagesCursor.close();
                            return;
                        }
                    }
                    imagesCursor.close();
                }else{
                    while (imagesCursor.moveToPrevious()) {
                        @SuppressLint("Range") long imagePreId = imagesCursor.getInt(imagesCursor.getColumnIndex(MediaStore.Images.Media._ID));
                        @SuppressLint("Range") String filePathImage = imagesCursor.getString(imagesCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        if (filePathImage.contains("DCIM/Camera") && filePathImage.contains(".jpg")) {
                            mFile = new File(filePathImage);
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 1;
                            Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(resolver, imagePreId, MediaStore.Images.Thumbnails.MINI_KIND, options);

                            if (bitmap != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        thumbnail.setImageBitmap(bitmap);
                                    }
                                });
                                isImageThumbnail = true;
                                imagesCursor.close();
                                break;
                            }
                        }
                    }

                    if(isImageThumbnail) return;
                }
            }




            Cursor videoCursor = resolver.query(targetVideoUrl, new String[]{
                            MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID}, null, null,
                    null);
            if (videoCursor != null) {
                if (videoCursor.moveToLast()) {
                    @SuppressLint("Range") long videoId = videoCursor.getInt(videoCursor.getColumnIndex(MediaStore.Video.Media._ID));
                    @SuppressLint("Range") String filePathVideo = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Video.Media.DATA));
                    if (filePathVideo.contains("DCIM/Camera") && filePathVideo.contains(".mp4")) {
                        mFile = new File(filePathVideo);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 1;
                        Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(resolver, videoId, MediaStore.Video.Thumbnails.MINI_KIND, options);
                        if (bitmap != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    thumbnail.setImageBitmap(bitmap);
                                }
                            });
                            videoCursor.close();
                            return;
                        }
                    }
                    videoCursor.close();
                }else{
                    while (imagesCursor.moveToPrevious()) {
                        @SuppressLint("Range") long imagePreId = imagesCursor.getInt(imagesCursor.getColumnIndex(MediaStore.Images.Media._ID));
                        @SuppressLint("Range") String filePathImage = imagesCursor.getString(imagesCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        if (filePathImage.contains("DCIM/Camera") && filePathImage.contains(".mp4")) {
                            mFile = new File(filePathImage);
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 1;
                            Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(resolver, imagePreId, MediaStore.Images.Thumbnails.MINI_KIND, options);
                            if (bitmap != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        thumbnail.setImageBitmap(bitmap);
                                    }
                                });
                                isImageThumbnail = true;
                                imagesCursor.close();
                                break;
                            }
                        }
                    }

                    if(isImageThumbnail) return;
                }
            }
        }




}