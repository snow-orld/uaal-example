using System;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using UnityEngine;

public class CameraWrapper : MonoBehaviour
{
    public Material material;

    [DllImport("cameraPlugin")]
    private static extern void SetTextureFromUnity(System.IntPtr texture);

    [DllImport("cameraPlugin")]
    private static extern IntPtr GetRenderEventFunc();
    
    private static int cameraWidth = 1920;
    private static int cameraHeight = 1080;

    private AndroidJavaObject ajo = null;

    private void Start()
    {
        if (Application.platform == RuntimePlatform.Android)
        {
            AndroidJavaClass ajc = new AndroidJavaClass("com.unity.mynativeapp.CameraActivity");
            ajo = ajc.GetStatic<AndroidJavaObject>("mActivity");
            
            InitCamera();
            CreateTextureAndPassToPlugin();
            StartCamera();
        }
    }

    public void InitCamera()
    {
        if (ajo != null)
        {
            ajo.Call("initCamera");
        }
    }

    public void DeInitCamera()
    {
        if (ajo != null)
        {
            ajo.Call("deInitCamera");
        }
    }

    public void StartCamera()
    {
        if (ajo != null)
        {
            ajo.Call("startCamera");
            StartCoroutine("CallPluginAtEndOfFrame");
        }
    }

    public void PauseCamera()
    {
        if (ajo != null)
        {
            ajo.Call("pauseCamera");
            StopCoroutine("CallPluginAtEndOfFrame");
        }
    }

    public void StopCamera()
    {
        if (ajo != null)
        {
            StopCoroutine("CallPluginAtEndOfFrame");
            ajo.Call("stopCamera");
        }
    }

    private IEnumerator CallPluginAtEndOfFrame()
    {
        while (true) {
            // Wait until all frame rendering is done
            yield return new WaitForEndOfFrame ();

            // Issue a plugin event with arbitrary integer identifier.
            // The plugin can distinguish between different
            // things it needs to do based on this ID.
            // For our simple plugin, it does not matter which ID we pass here.
            GL.IssuePluginEvent (GetRenderEventFunc (), 1);
            
            // skip one frame
            yield return new WaitForEndOfFrame ();
        }
    }
    
    private void CreateTextureAndPassToPlugin () {
        Texture2D tex = new Texture2D (cameraWidth, cameraHeight, TextureFormat.RGBA32, false);
        
        // Set point filtering just so we can see the pixels clearly
        tex.filterMode = FilterMode.Point;
        // Call Apply() so it's actually uploaded to the GPU
        tex.Apply();

        material.mainTexture = tex;

        SetTextureFromUnity(tex.GetNativeTexturePtr());

        EnablePreview(true);
    }
    
    public void EnablePreview (bool enable) {
        if (ajo != null) {
            ajo.Call ("enableCameraPreviewUpdater", enable);
        }
    }
    
    void OnGUI()
    {
        GUIStyle style = new GUIStyle("button");
        int screenWidth = Screen.width;
        style.fontSize = 20;
        if (GUI.Button(new Rect(screenWidth - 200, 0, 200, 90), "Start Camera", style)) StartCamera();
        if (GUI.Button(new Rect(screenWidth - 200, 100, 200, 90), "Pause Camera", style)) PauseCamera();
        if (GUI.Button(new Rect(screenWidth - 200, 200, 200, 90), "Stop Camera", style)) StopCamera();
    }
}
