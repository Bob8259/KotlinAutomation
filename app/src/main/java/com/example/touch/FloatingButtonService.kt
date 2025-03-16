package com.example.touch

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Path
import android.graphics.PointF
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import java.util.concurrent.atomic.AtomicBoolean

class FloatingButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var sharedPreferences: SharedPreferences
    
    // 轨迹显示悬浮窗
    private lateinit var trackView: View
    private lateinit var trackParams: WindowManager.LayoutParams
    private lateinit var gestureTrackView: GestureTrackView
    
    // 控制按钮悬浮窗
    private lateinit var controlView: View
    private lateinit var controlParams: WindowManager.LayoutParams
    private lateinit var mainButton: Button
    private lateinit var expandedLayout: LinearLayout
    private lateinit var startAutoGestureButton: Button  // 启动自动手势按钮
    private lateinit var stopAutoGestureButton: Button   // 停止自动手势按钮
    private lateinit var clearTracksButton: Button
    private lateinit var settingsButton: Button          // 添加设置按钮
    
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    
    private var isExpanded = false
    
    // 屏幕尺寸
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var isLandscape: Boolean = false
    
    // 自动手势间隔时间（毫秒）
    private var autoGestureInterval = DEFAULT_AUTO_GESTURE_INTERVAL
    
    private val handler = Handler(Looper.getMainLooper())
    
    // 定时任务相关
    private val isAutoGestureRunning = AtomicBoolean(false)
    private val autoGestureRunnable = object : Runnable {
        private var isZoomIn = true
        
        override fun run() {
            if (isAutoGestureRunning.get()) {
                // 执行手势
                if (isZoomIn) {
                    performZoomIn()
                } else {
                    performZoomOut()
                }
                
                // 切换下一次执行的手势类型
                isZoomIn = !isZoomIn
                
                // 设置下一次执行的延迟时间
                handler.postDelayed(this, autoGestureInterval)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        
        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 加载自动手势间隔时间
        autoGestureInterval = sharedPreferences.getLong(KEY_AUTO_GESTURE_INTERVAL, DEFAULT_AUTO_GESTURE_INTERVAL)
        
        // 初始化WindowManager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 获取屏幕尺寸
        updateScreenDimensions()
        
        // 创建轨迹显示悬浮窗
        createTrackView()
        
        // 创建控制按钮悬浮窗
        createControlView()
        
        // 设置悬浮窗参数
        setupLayoutParams()
        
        // 添加悬浮窗到屏幕
        windowManager.addView(trackView, trackParams)
        windowManager.addView(controlView, controlParams)
        
        // 注册手势轨迹监听器
        registerGestureTrackListener()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // 更新屏幕尺寸
        updateScreenDimensions()
        
        // 更新悬浮窗位置
        updateFloatingWindowsLayout()
    }
    
    private fun updateScreenDimensions() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        
        Log.d(TAG, "屏幕尺寸: ${screenWidth}x${screenHeight}, 横屏: $isLandscape")
    }
    
    private fun updateFloatingWindowsLayout() {
        // 更新轨迹显示悬浮窗位置
        if (isLandscape) {
            // 横屏模式下，将轨迹显示窗口放在屏幕底部
            trackParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        } else {
            // 竖屏模式下，将轨迹显示窗口放在屏幕中央
            trackParams.gravity = Gravity.CENTER
        }
        
        // 确保控制按钮不会超出屏幕边界
        if (controlParams.x > screenWidth - 150) {
            controlParams.x = screenWidth - 150
        }
        if (controlParams.y > screenHeight - 150) {
            controlParams.y = screenHeight - 150
        }
        
        // 更新悬浮窗位置
        if (::trackView.isInitialized && ::windowManager.isInitialized) {
            windowManager.updateViewLayout(trackView, trackParams)
        }
        if (::controlView.isInitialized && ::windowManager.isInitialized) {
            windowManager.updateViewLayout(controlView, controlParams)
        }
    }

    private fun createTrackView() {
        // 加载轨迹显示布局
        trackView = LayoutInflater.from(this).inflate(R.layout.track_view_layout, null)
        
        // 初始化轨迹显示视图
        gestureTrackView = trackView.findViewById(R.id.gesture_track_view)
    }
    
    private fun createControlView() {
        // 加载控制按钮布局
        controlView = LayoutInflater.from(this).inflate(R.layout.control_view_layout, null)
        
        // 初始化控制按钮视图
        mainButton = controlView.findViewById(R.id.main_button)
        expandedLayout = controlView.findViewById(R.id.expanded_layout)
        
        // 修改按钮引用和功能
        startAutoGestureButton = controlView.findViewById(R.id.zoom_in_button)
        startAutoGestureButton.text = getString(R.string.zoom_in)
        
        stopAutoGestureButton = controlView.findViewById(R.id.zoom_out_button)
        stopAutoGestureButton.text = getString(R.string.zoom_out)
        
        clearTracksButton = controlView.findViewById(R.id.clear_tracks_button)
        
        // 添加设置按钮
        settingsButton = controlView.findViewById(R.id.settings_button)
        if (settingsButton == null) {
            // 如果布局中没有设置按钮，则动态添加一个
            settingsButton = Button(this)
            settingsButton.text = getString(R.string.settings)
            settingsButton.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            (expandedLayout as LinearLayout).addView(settingsButton)
        }
        
        // 设置主按钮点击事件
        mainButton.setOnClickListener { toggleExpandedView() }
        
        // 设置拖动事件
        mainButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = controlParams.x.toFloat()
                    initialY = controlParams.y.toFloat()
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    // 计算移动的距离
                    controlParams.x = (initialX + (event.rawX - initialTouchX)).toInt()
                    controlParams.y = (initialY + (event.rawY - initialTouchY)).toInt()
                    
                    // 确保不超出屏幕边界
                    if (controlParams.x < 0) controlParams.x = 0
                    if (controlParams.y < 0) controlParams.y = 0
                    if (controlParams.x > screenWidth - 150) controlParams.x = screenWidth - 150
                    if (controlParams.y > screenHeight - 150) controlParams.y = screenHeight - 150
                    
                    // 更新悬浮窗位置
                    windowManager.updateViewLayout(controlView, controlParams)
                    true
                }
                else -> false
            }
        }
        
        // 设置启动自动手势按钮点击事件
        startAutoGestureButton.setOnClickListener {
            startAutoGesture()
            toggleExpandedView()
        }
        
        // 设置停止自动手势按钮点击事件
        stopAutoGestureButton.setOnClickListener {
            stopAutoGesture()
            toggleExpandedView()
        }
        
        // 设置清除轨迹按钮点击事件
        clearTracksButton.setOnClickListener {
            gestureTrackView.clearTracks()
            toggleExpandedView()
        }
        
        // 设置设置按钮点击事件
        settingsButton.setOnClickListener {
            showSettingsDialog()
            toggleExpandedView()
        }
    }

    private fun setupLayoutParams() {
        // 设置轨迹显示悬浮窗参数
        trackParams = WindowManager.LayoutParams().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                type = WindowManager.LayoutParams.TYPE_PHONE
            }
            
            format = PixelFormat.TRANSLUCENT
            
            // 添加FLAG_NOT_TOUCHABLE标志，使悬浮窗不拦截触摸事件
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            
            // 根据屏幕方向设置不同的位置
            gravity = if (isLandscape) {
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            } else {
                Gravity.CENTER
            }
            
            // 设置为全屏大小，以便显示手势轨迹
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            
            // 初始位置
            x = 0
            y = 0
        }
        
        // 设置控制按钮悬浮窗参数
        controlParams = WindowManager.LayoutParams().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                type = WindowManager.LayoutParams.TYPE_PHONE
            }
            
            format = PixelFormat.TRANSLUCENT
            
            // 控制按钮需要接收触摸事件，所以不设置FLAG_NOT_TOUCHABLE
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            
            gravity = Gravity.TOP or Gravity.START
            
            // 设置为包裹内容大小
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            
            // 初始位置，放在右上角
            x = screenWidth - 150
            y = 100
        }
    }

    private fun toggleExpandedView() {
        isExpanded = !isExpanded
        expandedLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }
    
    // 显示设置对话框
    private fun showSettingsDialog() {
        val context = this
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText((autoGestureInterval / 1000).toString())
        
        // 使用系统的 Dialog 而不是 AppCompat 的 AlertDialog
        val dialog = android.app.AlertDialog.Builder(context)
            .setTitle(getString(R.string.settings))
            .setMessage(getString(R.string.auto_gesture_interval_setting))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    val seconds = input.text.toString().toInt()
                    if (seconds >= 1) {
                        autoGestureInterval = seconds * 1000L
                        
                        // 保存设置
                        sharedPreferences.edit()
                            .putLong(KEY_AUTO_GESTURE_INTERVAL, autoGestureInterval)
                            .apply()
                        
                        Toast.makeText(
                            context,
                            getString(R.string.auto_gesture_interval_set, seconds),
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // 如果自动手势正在运行，重新启动以应用新的间隔时间
                        if (isAutoGestureRunning.get()) {
                            stopAutoGesture()
                            startAutoGesture()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.auto_gesture_interval_invalid),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(
                        context,
                        getString(R.string.auto_gesture_interval_invalid),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            
        // 确保对话框可以显示在其他窗口之上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
        }
        
        dialog.show()
    }
    
    // 启动自动手势
    private fun startAutoGesture() {
        val gestureService = TouchGestureServiceHolder.instance
        if (gestureService != null) {
            if (!isAutoGestureRunning.get()) {
                isAutoGestureRunning.set(true)
                
                // 立即执行一次放大手势
                performZoomIn()
                
                // 设定时间后执行下一次手势（缩小）
                handler.postDelayed(autoGestureRunnable, autoGestureInterval)
                
                Toast.makeText(this, getString(R.string.auto_gesture_started), Toast.LENGTH_SHORT).show()
                Log.d(TAG, "自动手势已启动，间隔时间：${autoGestureInterval}毫秒")
            } else {
                Toast.makeText(this, getString(R.string.auto_gesture_running), Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e(TAG, "辅助功能服务未连接")
            Toast.makeText(this, getString(R.string.accessibility_service_not_connected), Toast.LENGTH_SHORT).show()
            
            // 尝试通过Intent启动服务
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
    
    // 停止自动手势
    private fun stopAutoGesture() {
        if (isAutoGestureRunning.get()) {
            isAutoGestureRunning.set(false)
            handler.removeCallbacks(autoGestureRunnable)
            Toast.makeText(this, getString(R.string.auto_gesture_stopped), Toast.LENGTH_SHORT).show()
            Log.d(TAG, "自动手势已停止")
        } else {
            Toast.makeText(this, getString(R.string.auto_gesture_not_running), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performZoomIn() {
        // 直接调用辅助功能服务的方法
        val gestureService = TouchGestureServiceHolder.instance
        if (gestureService != null) {
            Log.d(TAG, "调用放大手势")
            gestureService.performPinchOutGesture()
        } else {
            Log.e(TAG, "辅助功能服务未连接")
            Toast.makeText(this, getString(R.string.accessibility_service_not_connected), Toast.LENGTH_SHORT).show()
            
            // 尝试通过Intent启动服务
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun performZoomOut() {
        // 直接调用辅助功能服务的方法
        val gestureService = TouchGestureServiceHolder.instance
        if (gestureService != null) {
            Log.d(TAG, "调用缩小手势")
            gestureService.performPinchInGesture()
        } else {
            Log.e(TAG, "辅助功能服务未连接")
            Toast.makeText(this, getString(R.string.accessibility_service_not_connected), Toast.LENGTH_SHORT).show()
            
            // 尝试通过Intent启动服务
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
    
    private fun registerGestureTrackListener() {
        // 注册手势轨迹监听器
        GestureTrackListener.setListener(object : GestureTrackListener.OnGestureTrackListener {
            override fun onGestureTrack(
                finger1Path: Path, 
                finger1Start: PointF, 
                finger1End: PointF,
                finger2Path: Path, 
                finger2Start: PointF, 
                finger2End: PointF
            ) {
                // 在UI线程中更新轨迹
                handler.post {
                    // 添加第一个手指的轨迹
                    gestureTrackView.addGesturePath(finger1Path, finger1Start, finger1End, true)
                    
                    // 添加第二个手指的轨迹
                    gestureTrackView.addGesturePath(finger2Path, finger2Start, finger2End, false)
                    
                    // 设置一段时间后自动清除轨迹
                    handler.postDelayed({
                        gestureTrackView.clearTracks()
                    }, GestureTrackView.TRACK_DISPLAY_DURATION)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止自动手势
        stopAutoGesture()
        
        // 取消注册手势轨迹监听器
        GestureTrackListener.setListener(null)
        
        // 移除所有悬浮窗
        if (::trackView.isInitialized && ::windowManager.isInitialized) {
            windowManager.removeView(trackView)
        }
        if (::controlView.isInitialized && ::windowManager.isInitialized) {
            windowManager.removeView(controlView)
        }
    }
    
    companion object {
        private const val TAG = "FloatingButtonService"
        private const val DEFAULT_AUTO_GESTURE_INTERVAL = 10000L // 默认10秒
        private const val PREFS_NAME = "TouchGesturePrefs"
        private const val KEY_AUTO_GESTURE_INTERVAL = "auto_gesture_interval"
    }
}


/**
 * 手势轨迹监听器
 */
object GestureTrackListener {
    private var listener: OnGestureTrackListener? = null
    
    fun setListener(listener: OnGestureTrackListener?) {
        this.listener = listener
    }
    
    fun notifyGestureTrack(
        finger1Path: Path, 
        finger1Start: PointF, 
        finger1End: PointF,
        finger2Path: Path, 
        finger2Start: PointF, 
        finger2End: PointF
    ) {
        listener?.onGestureTrack(
            finger1Path, finger1Start, finger1End,
            finger2Path, finger2Start, finger2End
        )
    }
    
    interface OnGestureTrackListener {
        fun onGestureTrack(
            finger1Path: Path, 
            finger1Start: PointF, 
            finger1End: PointF,
            finger2Path: Path, 
            finger2Start: PointF, 
            finger2End: PointF
        )
    }
} 