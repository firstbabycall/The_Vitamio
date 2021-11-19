package com.example.mycamera;

import android.Manifest;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

public class CameraController {
    private Context context;
    private AutoFitTextureView textureView;
    private String mCameraId="0";
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mPreviewRequestBuilder;


    private static final String APP_FLASH_MODE_ON = "on";
    private static final String APP_FLASH_MODE_OFF = "off";
    private static final String APP_FLASH_MODE_AUTO = "auto";
    private static final String APP_FLASH_MODE_TORCH = "torch";
    private String mFlashMode = APP_FLASH_MODE_OFF;

    private CameraCaptureSession mCameraSession;
    private CaptureRequest mPreviewRequest;
    private CameraManager mManager;
    private CameraCharacteristics cameraCharacteristics;
    private Size mPreviewSize;
    private Size mCaptureSize;
    private float mTargetRatio= 1.333f;;
    public static final float PREVIEW_SIZE_RATIO_OFFSET = 0.01f;;
    private File mFile;
    private int mPhoneOrientation;
    private int mSensorOrientation;
    private File mVideoPath;
    private Handler mBackgroundHandler;
    public boolean isRecorderRunning;
    private SPUtils spUtils;
    private boolean mStartTapFocus = false;
    private boolean mFlashTakePicture = false;
    private boolean mWaitAFFocus = false;
    private boolean mAePrecapture = false;


    public CameraController(Context context, AutoFitTextureView textureView, Handler backgroundHandler) {
        this.context = context;
        this.textureView = textureView;
        this.mBackgroundHandler=backgroundHandler;
    }



    public void openCamera(){
        mManager=(CameraManager) context.getSystemService(Context.CAMERA_SERVICE);


        try {
            mMediaRecorder=new MediaRecorder();
            if(ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                return;
            }
            mManager.openCamera(mCameraId,stateCallback,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private final CameraDevice.StateCallback stateCallback=new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice=cameraDevice;
            getCameraPreviewCaptureSize();
            if(mListener != null) {
                mListener.onCameraSizeSelectFinish(mPreviewSize);
            }
            startPreView();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

        }
    };

    private void createImageReader() {

         mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(
                mOnImageAvailableListener, mBackgroundHandler);
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener=new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            saveImageToSdcard(imageReader);
        }
    };


    public boolean lenFaceFront() {
        CameraCharacteristics cameraInfo = null;
        try {
            cameraInfo = mManager.getCameraCharacteristics(String.valueOf(mCameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (cameraInfo.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT) {//front camera
            return true;
        }

        return false;
    }

    private void saveImageToSdcard(ImageReader imageReader) {
        Log.d("comeText","save");
        Image image = imageReader.acquireNextImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        if(spUtils.getGetBoolean("watermark"))
            saveWithWaterMark(bytes,image,output);
        else
            saveNoWatermark(image, bytes, output);

    }


    private void saveWithWaterMark(byte[] bytes, Image image,FileOutputStream output) {
        Bitmap bitmapStart = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        Matrix matrix = new Matrix();
        matrix.setRotate(getJpegRotation(Integer.valueOf(mCameraId), mPhoneOrientation));
        if (lenFaceFront()) {
            matrix.postScale(-1, 1);
        }
        Bitmap bitmapSrc = Bitmap.createBitmap(bitmapStart, 0, 0, bitmapStart.getWidth(),
                bitmapStart.getHeight(), matrix, true);


        //Log.d("comeOnTest","bitmapSrc = " + bitmapSrc.getWidth());
        //Log.d("comeOnTest","bitmapSrc = " + bitmapSrc.getHeight());

        Bitmap bitmapNew = Bitmap.createBitmap(bitmapSrc.getWidth(), bitmapSrc.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvasNew = new Canvas(bitmapNew);
        canvasNew.drawBitmap(bitmapSrc, 0, 0, null);

        Paint paintText = new Paint();
        paintText.setColor(Color.RED);
        if (lenFaceFront()) {
            paintText.setTextSize(60);
        } else {
            paintText.setTextSize(150);
        }
        paintText.setDither(true);
        paintText.setFilterBitmap(true);
        Rect rectText = new Rect();
        String drawTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        paintText.getTextBounds(drawTime, 0, drawTime.length(), rectText);
        int beginX = bitmapNew.getWidth() - rectText.width() - 100;
        int beginY = bitmapNew.getHeight() /2;
        canvasNew.drawText(drawTime, beginX, beginY, paintText);

        if (mListener != null)
            mListener.onSaveImageFinish(bitmapNew);
        try {
            output = new FileOutputStream(mFile);
            bitmapNew.compress(Bitmap.CompressFormat.JPEG, 100, output);
            output.flush();
            Uri photouri = Uri.fromFile(mFile);
            context.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photouri));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            image.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void saveNoWatermark(Image image, byte[] bytes, FileOutputStream output) {

        Bitmap showThumbnail = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Matrix matrix = new Matrix();
        matrix.setRotate(getJpegRotation(Integer.valueOf(mCameraId), mPhoneOrientation));
        if (lenFaceFront()) {
            matrix.postScale(-1, 1);
        }
        Bitmap bitmapThumbnail = Bitmap.createBitmap(showThumbnail, 0, 0, showThumbnail.getWidth(), showThumbnail.getHeight(), matrix, true);

        if (mListener != null) {
            mListener.onSaveImageFinish(bitmapThumbnail);
        }

        Bitmap bitmapNew = Bitmap.createBitmap(bitmapThumbnail.getWidth(), bitmapThumbnail.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvasNew = new Canvas(bitmapNew);
        canvasNew.drawBitmap(bitmapThumbnail, 0, 0, null);
        try {
            output = new FileOutputStream(mFile);
            bitmapNew.compress(Bitmap.CompressFormat.JPEG, 100, output);
            output.flush();
//            output.write(bytes);
            //MediaProvider扫描，数据库
            Uri uri = Uri.fromFile(mFile);
            context.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            image.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setFilePath(File file){
        mFile = file;
    }

    public void captureStillPicture() {

        try {

            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());


            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {

                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
                    updateCapture();
                }
            };

            Log.d("comeText","picture");
           captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegRotation(Integer.valueOf(mCameraId), mPhoneOrientation));

           chooseFlashMode(mFlashMode, captureBuilder);
           mCameraSession.capture(captureBuilder.build(), CaptureCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void updateCapture() {
        try {
            mPreviewRequest = mPreviewRequestBuilder.build();
            Log.d("comeOnTest", "updateCapture");
            mCameraSession.capture(mPreviewRequest,
                    mCaptureCallback, mBackgroundHandler);
            //mCaptureSession.captureBurst()
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public int getJpegRotation(int cameraId, int orientation) {
        int rotation = 0;
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            orientation = 0;
        }
        if (cameraId == -1) {
            cameraId = 0;
        }
        CameraCharacteristics cameraInfo = null;
        try {
            cameraInfo = mManager.getCameraCharacteristics(String.valueOf(cameraId));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (cameraInfo.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT) {//front camera
            rotation = (mSensorOrientation - orientation + 360) % 360;
        } else {// back-facing camera
            rotation = (mSensorOrientation + orientation + 360) % 360;
        }
        return rotation;
    }


    private void startPreView() {
        createImageReader();
        SurfaceTexture texture=textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);

        try {
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
//起预览
                    mCameraSession = cameraCaptureSession;
                    setPreviewFrameParams();
                    updatePreview();
                }


                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public void updatePreview() {
        try {
            mPreviewRequest = mPreviewRequestBuilder.build();
            mCameraSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setPreviewFrameParams() {//下发、传参
        /*mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY,//iso
                100);*/
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        //characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        /*mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME,
                100l);//快门打开时间 ns*/
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        //mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        //人脸检测
        //mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, faceDetectModes[faceDetectModes.length - 1]);//设置人脸检测级别
        chooseFlashMode(mFlashMode, mPreviewRequestBuilder);
    }

    public void changeFlashMode(String s) {
        if (mFlashMode.equals(s)) return;
        mFlashMode = s;
        setPreviewFrameParams();
        updatePreview();
    }

    public void chooseFlashMode(String s, CaptureRequest.Builder requestBuilder) {
        Log.d("comeOnTest", "chooseFlashMode s= " + s);
        switch (s) {
            case APP_FLASH_MODE_OFF:
                offFlashMode(requestBuilder);
                break;
            case APP_FLASH_MODE_TORCH:
                torchFlashMode(requestBuilder);
                break;
            case APP_FLASH_MODE_AUTO:
                autoFlashMode(requestBuilder);
                break;
            case APP_FLASH_MODE_ON:
                onFlashMode(requestBuilder);
                break;
        }
    }

    public void torchFlashMode(CaptureRequest.Builder requestBuilder) {
        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
    }

    public void offFlashMode(CaptureRequest.Builder requestBuilder) {
        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
    }

    private void onFlashMode(CaptureRequest.Builder requestBuilder) {
        requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
    }

    private void autoFlashMode(CaptureRequest.Builder requestBuilder) {
        requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    public void process(CaptureResult result) {

        if (mAePrecapture) {
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            if (aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                mAePrecapture = false;
                captureStillPicture();
            }
        }



        if (mWaitAFFocus) {
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
            Log.d("comeOnTest", "afState = " + afState);

            if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                mWaitAFFocus = false;
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    captureStillPicture();
                } else {
                    runPrecaptureSequence();//闪光灯自动拍照
                }
            }
        }

        if (mFlashTakePicture) {
            mFlashTakePicture = false;
            triggerAf();
        }


    }

    private void runPrecaptureSequence() {
        mAePrecapture = true;
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);

            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mCameraSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void triggerAf() {
        mWaitAFFocus = true;
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        updateCapture();
    }

    private void getCameraPreviewCaptureSize() {
        try {
            String[] cameraIdList = mManager.getCameraIdList();
            //0 1, 2,,3
            for (int i = 0; i < cameraIdList.length; i++) {
                Log.d("comeOnTest", "captureRawSensor cameraIdList= " + cameraIdList[i]);
            }

            cameraCharacteristics = mManager.getCameraCharacteristics(mCameraId);//0 1
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        StreamConfigurationMap map = cameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Size[] previewSizeMap = map.getOutputSizes(SurfaceTexture.class);//preview
        Size[] captureSizeMap = map.getOutputSizes(ImageFormat.JPEG);//拍照
        Size[] captureRawSensor = map.getOutputSizes(ImageFormat.RAW_SENSOR);//拍照

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        mPreviewSize = getPreviewSize(previewSizeMap, mTargetRatio, screenWidth);
        mCaptureSize = getPictureSize(mTargetRatio, captureSizeMap);
    }

    public Size getPictureSize(float targetRatio, Size[] mapPicture) {
        Size maxPicSize = new Size(0, 0);
        for (int i = 0; i < mapPicture.length; i++) {
            float ratio = mapPicture[i].getWidth() / (float) mapPicture[i].getHeight();
            if (Math.abs(ratio - targetRatio) > PREVIEW_SIZE_RATIO_OFFSET) {
                continue;
            }
            if (mapPicture[i].getWidth() * mapPicture[i].getHeight() >= maxPicSize.getWidth() * maxPicSize.getHeight()) {
                maxPicSize = mapPicture[i];
            }
        }
        return maxPicSize;
    }
    public Size getPreviewSize(Size[] mapPreview, float targetRatio, int screenWidth) {
        Size previewSize = null;
        int minOffSize = Integer.MAX_VALUE;
        for (int i = 0; i < mapPreview.length; i++) {
            float ratio = mapPreview[i].getWidth() / (float) mapPreview[i].getHeight();
            if (Math.abs(ratio - targetRatio) > PREVIEW_SIZE_RATIO_OFFSET) {
                continue;
            }
            int widthDiff = Math.abs(screenWidth - mapPreview[i].getHeight());
            if (widthDiff <= minOffSize) {
                previewSize = mapPreview[i];
                minOffSize = widthDiff;
            }
        }
        return previewSize;
    }



    public void closeSessionDevice() {
        if (mCameraSession != null) {
            mCameraSession.close();
            mCameraSession = null;
        }

        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }


    public void CloseSesione(){
        if(mCameraSession!=null){
            mCameraSession.close();
            mCameraSession=null;
        }
    }

    public void setPhoneDeviceDegree(int mPhoneOrientation) {

        this.mPhoneOrientation = mPhoneOrientation;
    }


    public void previewRatioCHange(float ratio) {
        mTargetRatio = ratio;
        closeSessionDevice();
        openCamera();
    }


    public MediaRecorder mMediaRecorder;
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUpMediaRecorder() throws IOException {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoPath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(1920,1080);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOrientationHint(getJpegRotation(Integer.valueOf(mCameraId), mPhoneOrientation));
        mMediaRecorder.prepare();
    }


    public void stopRecord() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        isRecorderRunning=false;
        Bitmap bitmapThumbnail= ThumbnailUtils.createVideoThumbnail(mVideoPath.toString(), MediaStore.Images.Thumbnails.MINI_KIND);
        if (mListener != null) {
            mListener.onSaveImageFinish(bitmapThumbnail);
        }

        Uri uri=Uri.fromFile(mVideoPath);
        context.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        startPreView();

    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startRecord() {
        CloseSesione();
        mVideoPath=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),MainActivity.getShowFileName()+".mp4");
        try {
            setUpMediaRecorder();
        } catch (IOException e) {
            e.printStackTrace();
        }
        createImageReader();

        SurfaceTexture texture=textureView.getSurfaceTexture();
        assert texture != null;
        texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        List<Surface> surfaces = new ArrayList<>();

        // Set up Surface for the camera preview
        Surface previewSurface = new Surface(texture);
        surfaces.add(previewSurface);
        mPreviewRequestBuilder.addTarget(previewSurface);
        Surface recorderSurface=mMediaRecorder.getSurface();
        surfaces.add(recorderSurface);
        surfaces.add(mImageReader.getSurface());


        mPreviewRequestBuilder.addTarget(recorderSurface);
        try {
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCameraSession=cameraCaptureSession;
                    try {
                        mCameraSession.setRepeatingRequest(mPreviewRequestBuilder.build(),null,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    mMediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);
            isRecorderRunning=true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    public void frontBackChangeId() {
        closeSessionDevice();
        try {
            cameraCharacteristics = mManager.getCameraCharacteristics(mCameraId);
            Integer integer = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (integer == CameraMetadata.LENS_FACING_BACK) {
                mCameraId = "1";
            } else if (integer == CameraMetadata.LENS_FACING_FRONT) {
                mCameraId = "0";
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        openCamera();
    }


    private OnCameraControllerListener mListener;

    public void setOnCameraControllerListener(OnCameraControllerListener listener) {
        mListener = listener;
    }

    public interface OnCameraControllerListener {
        void onCameraSizeSelectFinish(Size previewSize);
        void onSaveImageFinish(Bitmap bitmap);
    }
}
