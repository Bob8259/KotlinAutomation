package com.example.touch

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 自定义视图，用于显示手势轨迹
 */
class GestureTrackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 存储手势轨迹
    private val gesturePaths = CopyOnWriteArrayList<GesturePath>()
    
    // 绘制轨迹的画笔
    private val pathPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    
    // 绘制起点和终点的画笔
    private val pointPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // 是否为横屏模式
    private var isLandscape = false
    
    // 清除所有轨迹
    fun clearTracks() {
        gesturePaths.clear()
        invalidate()
    }
    
    // 添加一个手势轨迹
    fun addGesturePath(path: Path, startPoint: PointF, endPoint: PointF, isFirstFinger: Boolean) {
        val color = if (isFirstFinger) Color.RED else Color.GREEN
        gesturePaths.add(GesturePath(path, startPoint, endPoint, color))
        invalidate()
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 检测屏幕方向
        isLandscape = w > h
        Log.d("GestureTrackView", "尺寸变化: ${w}x${h}, 横屏: $isLandscape")
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        Log.d("GestureTrackView", "配置变化: 横屏: $isLandscape")
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制所有手势轨迹
        for (gesturePath in gesturePaths) {
            // 设置轨迹颜色
            pathPaint.color = gesturePath.color
            
            // 绘制轨迹
            canvas.drawPath(gesturePath.path, pathPaint)
            
            // 绘制起点
            pointPaint.color = Color.BLUE
            canvas.drawCircle(gesturePath.startPoint.x, gesturePath.startPoint.y, 10f, pointPaint)
            
            // 绘制终点
            pointPaint.color = Color.RED
            canvas.drawCircle(gesturePath.endPoint.x, gesturePath.endPoint.y, 10f, pointPaint)
        }
    }
    
    // 手势轨迹数据类
    private data class GesturePath(
        val path: Path,
        val startPoint: PointF,
        val endPoint: PointF,
        val color: Int
    )
    
    // 设置轨迹显示时间（毫秒）
    companion object {
        const val TRACK_DISPLAY_DURATION = 3000L
    }
} 