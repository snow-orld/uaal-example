package com.unity.camera2;

import static android.content.Context.CAMERA_SERVICE;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.RenderScript;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class Camera2 {
    private static final String TAG = "Camera2";

    private static final int PREVIEW_WIDTH = 1920;
    private static final int PREVIEW_HEIGHT = 1080;
    private static final Size PREVIEW_SIZE = new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT);
    private static final int IMAGE_FORMAT = ImageFormat.YUV_420_888;

    private Context mContext;
    private TextureView mTextureView;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private ImageReader mImageReader;
    private Surface mPreviewSurface;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private YuvToRgb mConversionScript;
    private RenderScript mRenderScript;
    private boolean mUpdate;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private static final int ANR_TIMEOUT_SECONDS = 4;

    private enum CameraCaptureState
    {
        STARTED,
        PAUSED,
        STOPPED
    };
    private CameraCaptureState mCaptureState = CameraCaptureState.STOPPED;

    public native void nativeInit();

    public native void nativeRelease();

    public void Init() {
        Log.i(TAG, "call Init, load native lib");
        System.loadLibrary("cameraPlugin");
        nativeInit();
    }

    public void DeInit() {
        Log.i(TAG, "call DeInit, release native lib");
        nativeRelease();
    }

    public Camera2(Context context) {
        mContext = context;

        mRenderScript = RenderScript.create(mContext);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void startCamera() {
        Log.d(TAG, "call startCamera");
        openCamera();
    }

    public void pauseCamera() {
        Log.d(TAG, "call pauseCamera");
        if (mCaptureSession != null) {
            try {
                mCaptureSession.stopRepeating();
                mCaptureState = CameraCaptureState.PAUSED;
            } catch (CameraAccessException e) {
                Log.e(TAG, "call pauseCamera error: CameraAccessException " + e);
                e.printStackTrace();
            }
        }
    }

    public void stopCamera() {
        Log.d(TAG, "call stopCamera");
        try {
            mCameraOpenCloseLock.acquire();

            if (null != mCaptureSession) {
                mCaptureSession.abortCaptures();
                mCaptureSession.close();
                mCaptureSession = null;
                mCaptureState = CameraCaptureState.STOPPED;
            }

            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            Log.e(TAG,"Interrupted while trying to lock camera closing. " + e);
        } catch (CameraAccessException e) {
            Log.e(TAG, "stopCamera error: abort capture failed. CameraAccessException: " + e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    public void closeCamera() {
        Log.d(TAG, "call closeCamera");
        stopCamera();

        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join(ANR_TIMEOUT_SECONDS * 1000);
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            mHandlerThread.interrupt();
            Log.e(TAG, "CloseCamera: Interrupted while waiting for the background camera thread to finish " + e);
            e.printStackTrace();
        }
    }

    public void enablePreviewUpdater(boolean update)
    {
        mUpdate = update;
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) mContext.getSystemService(CAMERA_SERVICE);

        if (mImageReader == null) {
            mImageReader = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT,
                    IMAGE_FORMAT, 2);
            mConversionScript = new YuvToRgb(mRenderScript, PREVIEW_SIZE, 30);
            mConversionScript.setOutputSurface(mImageReader.getSurface());
            mPreviewSurface = mConversionScript.getInputSurface();
        }

        if (mCaptureSession != null) {
            // Start the repeated capture again if we're paused.
            if (mCaptureState == CameraCaptureState.PAUSED) {
                try {
                    Log.d(TAG, "call startCamera: resume paused preview");
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mHandler);
                } catch (CameraAccessException e) {
                    Log.e(TAG, "called startCamera setRepeatingRequest error: CameraAccessException " + e);
                    e.printStackTrace();
                }
            }
        } else {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (mContext.checkSelfPermission(Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "call StartCamera: Camera Permission NOT Granted!");
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    String pickedCamera = getCamera(manager);
                    Log.d(TAG, "call startCamera: Picked Camera " + pickedCamera);
                    if (pickedCamera == null) return;

                    manager.openCamera(pickedCamera, mCameraStateCallback, mHandler);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        mCaptureState = CameraCaptureState.STARTED;
    }

    private void forceCloseCameraDevice() {
        Log.d(TAG, "call forceCloseCameraDevice");
        if (mCaptureSession != null) {
            try {
                mCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                Log.e(TAG, "called forceCloseCameraDevice error: CameraAccessException " + e);
                e.printStackTrace();
            }
            Log.d(TAG, "clear mCaptureSession");
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            Log.d(TAG, "clear mCameraDevice");
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private final CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "called CameraDevice.StateCallback onOpened");
            mCameraDevice = cameraDevice;
            createCaptureRequest();
            createCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "called CameraDevice.StateCallback onDisconnected");
            mCaptureSession = null;
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.e(TAG, "called CameraDevice.StateCallback onError[" + error + "]");
            forceCloseCameraDevice();
        }
    };

    private void createCaptureRequest() {
        try {
            mPreviewRequestBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mPreviewRequestBuilder.addTarget(mPreviewSurface);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mCaptureSession = session;
            try {
                Log.d(TAG, "called CameraCaptureSession.StateCallback onConfigured!");
                session.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "called CameraCaptureSession.StateCallback onConfigured error: CameraAccessException " + e);
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.e(TAG, "called CameraCaptureSession.StateCallback onConfigureFailed");
            forceCloseCameraDevice();
        }
    };

    private void createCaptureSession() {
        try {
            if (mPreviewSurface != null) {
                mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface),
                        mSessionStateCallback, mHandler);
            } else {
                Log.e(TAG, "called createCaptureSession error: failed creating preview surface");
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "called createCaptureSession error: CameraAccessException " + e);
            e.printStackTrace();
        }
    }

    private String getCamera(CameraManager manager) {
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "getCamera error: CameraAccessException: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /*
     *
     * Called from NDK to update the texture in Unity.
     * It is done this way since Unity does not allow Java callbacks for GL.IssuePluginEvent
     *
     */
    private void requestJavaRendering(int texturePointer) {

        if (!mUpdate) {
            return;
        }

        int[] imageBuffer = new int[0];

        if (mConversionScript != null) {
            imageBuffer = mConversionScript.getOutputBuffer();
        }

//        Log.i(TAG, "Request Java Rendering, imageBuffer.length is " + imageBuffer.length + ".");

        if (imageBuffer.length > 1) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturePointer);

            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, PREVIEW_SIZE.getWidth(),
                    PREVIEW_SIZE.getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                    IntBuffer.wrap(imageBuffer));

        }
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        ((Activity)mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
