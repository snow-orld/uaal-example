package com.unity.mynativeapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    boolean isUnityLoaded = false;

    private View mBgView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        handleIntent(getIntent());
    }

    private void AddActivityToWindow() {
        Log.d(TAG, "call CreateForegroundView");
        mBgView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        setIntent(intent);
    }

    void handleIntent(Intent intent) {
        if(intent == null || intent.getExtras() == null) return;

        if(intent.getExtras().containsKey("setColor")){
            View v = findViewById(R.id.button2);
            switch (intent.getExtras().getString("setColor")) {
                case "yellow": v.setBackgroundColor(Color.YELLOW); break;
                case "red": v.setBackgroundColor(Color.RED); break;
                case "blue": v.setBackgroundColor(Color.BLUE); break;
                default: break;
            }
        }
    }

    public void btnLoadUnity(View v) {
        isUnityLoaded = true;
        Intent intent = new Intent(this, MainUnityActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, 1);
    }

    public void btnLoadUnityInLayout(View v) {
        isUnityLoaded = true;
        Intent intent = new Intent(this, AppActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1) isUnityLoaded = false;
    }

    public void unloadUnity(Boolean doShowToast) {
        if(isUnityLoaded) {
            Intent intent = new Intent(this, MainUnityActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("doQuit", true);
            startActivity(intent);
            isUnityLoaded = false;
        }
        else if(doShowToast) showToast("Show Unity First");
    }

    public void btnUnloadUnity(View v) {
        unloadUnity(true);
    }

    public void showToast(String message) {
        CharSequence text = message;
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
        toast.show();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }

    public void btnQuit(View v) {
        moveTaskToBack(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MainActivity", "onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("MainActivity", "onRestart");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MainActivity", "onStart");
    }
}
