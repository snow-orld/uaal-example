using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class RectTest : MonoBehaviour
{
    public static void DrawRect(Rect rect, Color color, float alpha, GUIContent content = null)
    {
        color.a = alpha;
        var backgroundColor = GUI.backgroundColor;
        GUI.backgroundColor = color;
        GUI.Box(rect, content ?? GUIContent.none, 
            new GUIStyle {normal = new GUIStyleState { background = Texture2D.whiteTexture}});
        GUI.backgroundColor = backgroundColor;
    }

    public static void DrawPosition(Vector2 pos, Color color, float size)
    {
        var rect = new Rect();
        rect.size = new Vector2(size, size);
        rect.center = pos;
        DrawRect(rect, color, 1);
    }
    
    private void OnGUI()
    {
        int padding = 10;
        Vector2 paddingXY = new Vector2(padding, padding);
        Vector2 rectSize = new Vector2(Screen.width - 2 * padding, Screen.height - 2 * padding);
        
        Debug.Log($"ScreenWxH {Screen.width}x{Screen.height}, rectSize {rectSize}");

        Rect r1 = new Rect(paddingXY, rectSize);
        DrawRect(r1, Color.red, 0.5f);

        Vector2[] vertices = r1.GetVerticesByRotation(0);

        // Rect related is all in the same frame: Rect frame (upper left is zero, y downwards)
        Vector2 inPos = new Vector2(20, 20);
        DrawPosition(inPos, Color.yellow, 4);
        Debug.Log($"{r1} {(r1.Contains(inPos) ? "contains":"doesn't contain")} {inPos}");

        Vector2 outPos = new Vector2(5, 5);
        DrawPosition(outPos, Color.black, 4);
        Debug.Log($"{r1} {(r1.Contains(outPos) ? "contains":"doesn't contain")} {outPos}");
    }
}

public static class RectExtension
{
    public static Vector2[] GetVerticesByRotation(this Rect rect, float angle)
    {
        Vector2[] corners = new Vector2[4];
        
        for (int i = 0; i < 4; i++)
        {
            Vector2 point = Rect.NormalizedToPoint(rect, new Vector2(i % 2, i / 2));
            Vector2 center = rect.center;

            if (i < 2)
            {
                RectTest.DrawPosition(point, Color.blue, 10);
            }
            else
            {
                RectTest.DrawPosition(point, Color.cyan, 10);
            }
        }

        return corners;
    }
}
