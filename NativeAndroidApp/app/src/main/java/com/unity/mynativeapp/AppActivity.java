package com.unity.mynativeapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.unity3d.player.IUnityPlayerLifecycleEvents;
import com.unity3d.player.UnityPlayer;

import java.lang.reflect.Field;

public class AppActivity extends AppCompatActivity implements IUnityPlayerLifecycleEvents {
    private static final String TAG = "AppActivity";
    private View mBgView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private FrameLayout.LayoutParams mViewParams;
    private UnityPlayer mUnityPlayer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        AddUnityToView();
        AddUnityToWindow();

        AddControlsToUnity();
    }

    private void AddUnityToWindow() {
        Log.d(TAG, "call CreateForegroundView");
        mBgView = LayoutInflater.from(this).inflate(R.layout.bgview_unity, null);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        mWindowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT);
        mWindowManager.addView(mBgView, mWindowParams);

        mViewParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mViewParams.gravity = Gravity.BOTTOM;

        try {
            mUnityPlayer = new UnityPlayer(this, this);

//            Field f =  UnityPlayer.class.getDeclaredField("mGlView");
//            f.setAccessible(true);
//            SurfaceView v = (SurfaceView)f.get(mUnityPlayer);
//            mUnityPlayer.removeView(v);
//            v.getHolder().setFormat(PixelFormat.TRANSLUCENT);
//            v.setZOrderOnTop(true);
//            mUnityPlayer.addView(v);
        } catch (Exception e) {
            // if we're unlucky, maybe try again?
            Log.e(TAG,"UnityPlayer Thread Start failed" + e.getMessage());
            e.printStackTrace();
        }

        ((ViewGroup)mBgView).addView(mUnityPlayer, mViewParams);

        mBgView.post(new Runnable() {
            @Override
            public void run() {
                mUnityPlayer.windowFocusChanged(true);
                mUnityPlayer.resume();
            }
        });
    }

    /**
     * 运行以下方法，卡在静态开机画面，没有启动UnityPlayer
     */
    private void AddUnityToView_StuckAtStaticSplash() {
        mBgView = LayoutInflater.from(this).inflate(R.layout.bgview_unity, null);
        setContentView(mBgView);
        mUnityPlayer = new UnityPlayer(this, this);
        ((ViewGroup)mBgView).addView(mUnityPlayer);
    }

    /**
     * 添加mBgView.post，保证Unity在安卓初始化完全完成后调用resume
     * 解决AddUnityToView_StuckAtStaticSplash函数卡在开机画面的问题
     */
    private void AddUnityToView() {
        mBgView = LayoutInflater.from(this).inflate(R.layout.bgview_unity, null);
        setContentView(mBgView);
        mUnityPlayer = new UnityPlayer(this, this);
        ((ViewGroup)mBgView).addView(mUnityPlayer);

        mBgView.post(new Runnable() {
            @Override
            public void run() {
                mUnityPlayer.windowFocusChanged(true);
                mUnityPlayer.resume();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "call onNewIntent");
        handleIntent(intent);
        setIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getExtras() == null) return;

        if (intent.getExtras().containsKey("doQuit"))
            if (mUnityPlayer != null) {
                finish();
            }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "call onRestart");

        SwitchToForeground();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "call onStart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "call onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "call onStop");
    }

    @Override
    protected void onDestroy() {
        mUnityPlayer.destroy();
        super.onDestroy();
        Log.d(TAG, "call onDestroy");
    }

    @Override
    public void onUnityPlayerUnloaded() {
        Log.d(TAG, "call onUnityPlayerUnloaded");
        showMainActivity();
    }

    @Override
    public void onUnityPlayerQuitted() {
        Log.d(TAG, "call onUnityPlayerQuitted");
    }

    private void SwitchToBackground() {
        if (mBgView.isAttachedToWindow()) {
            Log.d(TAG, "call SwitchToBackground");
            mWindowManager.removeView(mBgView);
            moveTaskToBack(true);
        }
    }

    private void SwitchToForeground() {
        if (!mBgView.isAttachedToWindow()) {
            Log.d(TAG, "call SwitchToForeground");
            mWindowManager.addView(mBgView, mWindowParams);
        }
    }

    private void showMainActivity() {
        Log.d(TAG, "showMainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void AddControlsToUnity() {
        int buttonWidth = 200;
        int buttonHeight = 80;

        FrameLayout layout = mUnityPlayer;
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                buttonWidth, buttonHeight
        );
        {
            Button myButton = new Button(this);
            myButton.setText("Switch to BG");
            myButton.setX(0);
            myButton.setY(500);

            myButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SwitchToBackground();
                }
            });

            layout.addView(myButton, buttonParams);
        }

        {
            Button myButton = new Button(this);
            myButton.setText("Unload");
            myButton.setX(200);
            myButton.setY(500);

            myButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mUnityPlayer.unload();
                }
            });
            layout.addView(myButton, buttonParams);
        }

        {
            Button myButton = new Button(this);
            myButton.setText("Finish");
            myButton.setX(400);
            myButton.setY(500);

            myButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
            layout.addView(myButton, buttonParams);
        }
    }
}
