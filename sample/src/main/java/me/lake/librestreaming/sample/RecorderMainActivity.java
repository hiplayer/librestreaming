package me.lake.librestreaming.sample;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.AlertDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import me.lake.librestreaming.sample.audiofilter.SetVolumeAudioFilter;
import me.lake.librestreaming.sample.hardfilter.BeautyFaceHardVideoFilter;
import me.lake.librestreaming.sample.ui.AspectTextureView;
import me.lake.librestreaming.client.RESClient;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.core.listener.RESVideoChangeListener;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.Size;
import me.lake.librestreaming.sample.ui.CircleButtonView;


import java.io.File;


import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Environment.DIRECTORY_MOVIES;

/**
 * Created by hukanli on 2017_12_28
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public class RecorderMainActivity extends AppCompatActivity implements RESConnectionListener, TextureView.SurfaceTextureListener, View.OnClickListener, RESVideoChangeListener {
    private static final String TAG = "RES";
    public static final String DIRECTION = "direction";
    public static final String RTMPADDR = "rtmpaddr";
    private static final int REQUEST_GET_ACCOUNT = 112;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int RECORD_REQUEST_CODE = 300;
    private CircleButtonView mCircleButtonView;
    private ImageView mSwitchCamera;
    private ImageView mBeautyFilter;
    protected RESClient resClient;
    protected RESConfig resConfig;
    protected AspectTextureView txv_preview;
    protected Handler mainHander;
    private BeautyFaceHardVideoFilter mBeautyFaceHardVideoFilter;
    protected boolean mIsSurfaceTextureInit = false;
    protected boolean mIsStartRecord = false;
    protected boolean mIsEnableBeautyFilter = false;
    protected int filtermode = RESConfig.FilterMode.HARD;
    protected String senderAddr;
    protected File recordFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent i = getIntent();

        /*
        if (i.getBooleanExtra(DIRECTION, false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        */
        recordFile = new File(getExternalFilesDir(DIRECTORY_MOVIES), "record.mp4");
        senderAddr = recordFile.toString();
        mIsStartRecord = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recorder_main_activity);

        initView();
        checkPermission();
        initResClient();
    }

    private void initView() {
        txv_preview = (AspectTextureView) findViewById(R.id.texture_view);
        txv_preview.setKeepScreenOn(true);
        txv_preview.setSurfaceTextureListener(this);
        mSwitchCamera = (ImageView) findViewById(R.id.switch_camera);
        mSwitchCamera.setOnClickListener(this);
        mBeautyFilter = (ImageView) findViewById(R.id.beauty_filter);
        mBeautyFilter.setOnClickListener(this);
        mCircleButtonView = (CircleButtonView) findViewById(R.id.cbv);
        mCircleButtonView.setOnLongClickListener(new CircleButtonView.OnLongClickListener() {
            @Override
            public void onLongClick() {
                //开始录制
                startVideoRecord();
                Log.e(TAG,"开始录制");
            }

            @Override
            public void onNoMinRecord(int minTime) {
                Toast.makeText(RecorderMainActivity.this, "录制最短时间为"+minTime+"s", Toast.LENGTH_SHORT).show();
                Log.e(TAG,"录制时间太短");
                stopVideoRecord();
            }

            @Override
            public void onRecordFinishedListener() {
                stopVideoRecord();
                Toast.makeText(RecorderMainActivity.this, "录制完成", Toast.LENGTH_SHORT).show();
                Log.e(TAG,"录制完成");
            }
        });

    }

    private void initResClient() {
        resClient = new RESClient();
        resConfig = RESConfig.obtain();
        resConfig.setFilterMode(filtermode);
        resConfig.setTargetVideoSize(new Size(960, 540));
        resConfig.setBitRate(1200 * 1024);
        resConfig.setVideoFPS(25);
        resConfig.setVideoGOP(1);
        resConfig.setRenderingMode(RESConfig.RenderingMode.OpenGLES);
        resConfig.setDefaultCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        int frontDirection, backDirection;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, cameraInfo);
        frontDirection = cameraInfo.orientation;
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
        backDirection = cameraInfo.orientation;
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            resConfig.setFrontCameraDirectionMode((frontDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90) | RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
            resConfig.setBackCameraDirectionMode((backDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_90 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_270));
        } else {
            resConfig.setBackCameraDirectionMode((backDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_0 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_180));
            resConfig.setFrontCameraDirectionMode((frontDirection == 90 ? RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_180 : RESConfig.DirectionMode.FLAG_DIRECTION_ROATATION_0) | RESConfig.DirectionMode.FLAG_DIRECTION_FLIP_HORIZONTAL);
        }

        resConfig.setSenderMode(RESConfig.SenderMode.MPEG4);
        resConfig.setSenderAddr(senderAddr);
        if (!resClient.prepare(resConfig)) {
            resClient = null;
            Log.e(TAG, "prepare,failed!!");
            Toast.makeText(this, "RESClient prepare failed", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Size s = resClient.getVideoSize();
        txv_preview.setAspectRatio(AspectTextureView.MODE_OUTSIDE, ((double) s.getWidth()) / s.getHeight());
        Log.d(TAG, "version=" + resClient.getVertion());
        resClient.setConnectionListener(this);
        resClient.setVideoChangeListener(this);
        mBeautyFaceHardVideoFilter = new BeautyFaceHardVideoFilter(getApplicationContext(), 2);
        resClient.setHardVideoFilter(mBeautyFaceHardVideoFilter);
        mIsEnableBeautyFilter = true;
        mainHander = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            }
        };
        mainHander.sendEmptyMessage(0);

        resClient.setSoftAudioFilter(new SetVolumeAudioFilter());
    }

    private void startVideoRecord() {
        if (resClient != null) {
            stopVideoRecord();
            resClient.startStreaming();
            mIsStartRecord = true;
        }
    }

    private void stopVideoRecord() {
        if (resClient != null) {
            if (mIsStartRecord) {
                resClient.stopStreaming();
            }
            mIsStartRecord = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mainHander != null) {
            mainHander.removeCallbacksAndMessages(null);
        }
        if (resClient != null) {
            if (mIsStartRecord) {
                resClient.stopStreaming();
                mIsStartRecord = false;
            }
            resClient.destroy();
        }
        super.onDestroy();
    }

    protected void reStartWithResolution(int w, int h) {
        /*
        //===========don`t interrupt streaming
        resClient.reSetVideoBitrate(1200*1024);
        resClient.reSetVideoSize(new Size(1280,720));
        //===or======interrupt streaming
        if (mIsStartRecord) {
            resClient.stopStreaming();
        }
        resClient.stopPreview(false);
        resClient.destroy();
        resConfig.setTargetVideoSize(new Size(w, h));
        resClient.prepare(resConfig);
        resClient.startPreview(texture, sw, sh);
        Size s = resClient.getVideoSize();
        txv_preview.setAspectRatio(AspectTextureView.MODE_INSIDE, ((double) s.getWidth()) / s.getHeight());
        if (mIsStartRecord) {
            resClient.startStreaming();
        }
        //===========
        */
    }

    @Override
    public void onOpenConnectionResult(int result) {
        if (result == 0) {
            Log.e(TAG, "server IP = " + resClient.getServerIpAddr());
        }else {
            Toast.makeText(this, "startfailed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWriteError(int errno) {
        if (errno == 9) {
            stopVideoRecord();
            startVideoRecord();
            Toast.makeText(this, "errno==9,restarting", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCloseConnectionResult(int result) {

    }

    protected SurfaceTexture texture;
    protected int sw,sh;

    @Override
    public void onVideoSizeChanged(int width, int height) {
        txv_preview.setAspectRatio(AspectTextureView.MODE_INSIDE, ((double) width) / height);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (resClient != null) {
            resClient.startPreview(surface, width, height);
        }
        texture = surface;
        sw = width;
        sh = height;
        mIsSurfaceTextureInit = true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (resClient != null) {
            resClient.updatePreview(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (resClient != null) {
            resClient.stopPreview(true);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onClick(View v) {
       int id = v.getId();
       if (id == R.id.switch_camera) {
           if (resClient != null) {
               if (mIsStartRecord) {
                   resClient.stopStreaming();
               }
               resClient.swapCamera();
           }
       } else if (id == R.id.beauty_filter) {
            if (resClient != null) {
                if (mIsEnableBeautyFilter) {
                    resClient.setHardVideoFilter(null);
                    mBeautyFilter.setImageDrawable(getResources().getDrawable(R.drawable.beauty_filter_on));
                    mIsEnableBeautyFilter = false;
                } else {
                    resClient.setHardVideoFilter(mBeautyFaceHardVideoFilter);
                    mBeautyFilter.setImageDrawable(getResources().getDrawable(R.drawable.beauty_filter_off));
                    mIsEnableBeautyFilter = true;
                }
            }
       }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), GET_ACCOUNTS);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{GET_ACCOUNTS, CAMERA}, REQUEST_GET_ACCOUNT);
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && cameraAccepted)
                        Toast.makeText(getApplicationContext(), "Permission Granted, Now you can access location data and camera", Toast.LENGTH_LONG).show();
                    else {
                        Toast.makeText(getApplicationContext(), "Permission Denied, You cannot access location data and camera", Toast.LENGTH_LONG).show();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }

                    }
                }

                break;

            case RECORD_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.w(TAG, "Permission has been denied by user");
                } else {
                    Log.w(TAG, "Permission has been granted by user");
                }
                return;
            }
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new  AlertDialog.Builder(RecorderMainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
}