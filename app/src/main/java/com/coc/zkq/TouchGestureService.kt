package com.coc.zkq

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Path
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.util.Random

class TouchGestureService : AccessibilityService() {

    companion object {
        private const val TAG = "TouchGestureService"
        private const val GESTURE_DURATION = 800L // 增加手势持续时间（毫秒）
        private const val ZOOM_FACTOR = 0.5f // 增加缩放因子
        private const val MAX_RANDOM_OFFSET = 5 // 减小随机偏移量，使手势更精确
        private const val BASE_DISTANCE_FACTOR = 0.15f // 增加基础距离因子
    }

    private lateinit var mainHandler: Handler
    private lateinit var random: Random
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var isLandscape: Boolean = false
    private val activeFingers = mutableMapOf<Int, Path>()

    // 存储当前活跃的触摸点
    private val activeTouches = SparseArray<TouchPoint>()

    // 触摸点数据类
    data class TouchPoint(val id: Int, val path: Path, var isActive: Boolean = true)

    private val handler = Handler(Looper.getMainLooper())
    private var currentX = 100f
    private var currentY = 100f
    override fun onCreate() {
        super.onCreate()
        mainHandler = Handler(Looper.getMainLooper())
        random = Random()

        // 获取屏幕尺寸
        updateScreenDimensions()
    }

    private fun updateScreenDimensions() {
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Log.d(TAG, "屏幕尺寸: ${screenWidth}x${screenHeight}, 横屏: $isLandscape")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateScreenDimensions()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // 保存实例到持有者
        TouchGestureServiceHolder.setInstance(this)

        // 显示提示
        showToast("辅助功能服务已连接")
        Log.d(TAG, "辅助功能服务已连接")

        // 检查是否有执行手势的权限
        val info = serviceInfo
        if (info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES != 0) {
            Log.d(TAG, "服务具有执行手势的能力")
        } else {
            Log.e(TAG, "服务不具有执行手势的能力，请检查配置")
            showToast("服务不具有执行手势的能力，请检查配置")
        }

        // 更新屏幕尺寸
        updateScreenDimensions()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 我们不需要处理任何辅助功能事件
    }

    override fun onInterrupt() {
        // 服务中断时的处理
    }

    override fun onUnbind(intent: Intent): Boolean {
        // 清除实例
        TouchGestureServiceHolder.setInstance(null)
        return super.onUnbind(intent)
    }

    fun simulateLongPress(duration: Long) {
        val path = Path()
        path.moveTo(93f,446f)
        val path2 = Path()
        path2.moveTo(580f,84f)
        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, duration))
            .build()

        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                // 手势完成后的操作
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                // 手势取消后的操作
            }
        }, null)
    }


    fun touchDown(id: Int, x: Float, y: Float): Boolean {
        if (activeTouches.get(id) != null) {
            return false // ID已存在
        }

        val path = Path()
        path.moveTo(x, y)

        val touchPoint = TouchPoint(id, path)
        activeTouches.put(id, touchPoint)

        // 执行按下动作
        return performTouch()
    }

    /**
     * 触摸移动
     * @param id 触摸点ID
     * @param x 新的X坐标
     * @param y 新的Y坐标
     * @return 是否成功
     */

    fun touchMove(id: Int, x: Float, y: Float): Boolean {
        val touchPoint = activeTouches.get(id) ?: return false

        touchPoint.path.lineTo(x, y)

        // 执行移动动作
        return performTouch()
    }


    /**
     * 触摸抬起
     * @param id 触摸点ID
     * @return 是否成功
     */

    fun touchUp(id: Int): Boolean {
        val touchPoint = activeTouches.get(id) ?: return false

        touchPoint.isActive = false

        // 执行抬起动作
        val result = performTouch()

        // 动作完成后移除该触摸点
        activeTouches.remove(id)

        return result
    }

    /**
     * 执行当前所有触摸动作
     */

    private fun performTouch(): Boolean {
        val gestureBuilder = GestureDescription.Builder()
        var hasActiveTouch = false

        // 添加所有活跃的触摸点到手势中
        for (i in 0 until activeTouches.size()) {
            val touchPoint = activeTouches.valueAt(i)

            // 对于活跃的触摸点，使用较长的持续时间
            // 对于即将抬起的触摸点，使用较短的持续时间
            val duration = if (touchPoint.isActive) 1000L else 1L

            gestureBuilder.addStroke(
                GestureDescription.StrokeDescription(
                    touchPoint.path, 0, // 开始时间
                    duration
                )
            )

            if (touchPoint.isActive) {
                hasActiveTouch = true
            }
        }

        // 如果没有活跃的触摸点，返回false
        if (!hasActiveTouch && activeTouches.size() == 0) {
            return false
        }

        // 构建手势描述
        val gestureDescription = gestureBuilder.build()

        // 执行手势
        val result = dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                // 手势完成后的操作
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                // 手势被取消后的操作
            }
        }, null)

        return result
    }

    fun performLongPressGesture() {
        // 定义两个点的坐标
        val point1 = PointF(63f, 427f)
        val point2 = PointF(533f, 85f)

        // 定义手势持续时间（10秒）
        val duration = 10_000L

        // 执行手势
        performTwoPointLongPressGesture(point1, point2, duration)
    }

    /**
     * 执行双点长按手势
     */
    private fun performTwoPointLongPressGesture(
        point1: PointF, point2: PointF, duration: Long
    ) {
        try {
            // 创建手势描述
            val gestureBuilder = GestureDescription.Builder()

            // 创建第一个手指的路径（只是一个点）
            val path1 = Path().apply {
                moveTo(point1.x, point1.y)
            }

            // 创建第二个手指的路径（只是一个点）
            val path2 = Path().apply {
                moveTo(point2.x, point2.y)
            }

            // 添加第一个手指的路径
            val stroke1 = GestureDescription.StrokeDescription(
                path1, 0, duration
            )
            gestureBuilder.addStroke(stroke1)

            // 添加第二个手指的路径
            val stroke2 = GestureDescription.StrokeDescription(
                path2, 0, duration
            )
            gestureBuilder.addStroke(stroke2)

            // 执行手势
            val gestureDescription = gestureBuilder.build()
            val result = dispatchGesture(gestureDescription, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "手势执行完成")
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    super.onCancelled(gestureDescription)
                    Log.e(TAG, "手势被取消")
                    // 尝试再次执行
                    mainHandler.postDelayed({
                        Log.d(TAG, "尝试再次执行手势")
                        dispatchGesture(gestureDescription, null, null)
                    }, 500)
                }
            }, null)

            if (!result) {
                Log.e(TAG, "手势执行失败，可能原因：1.权限不足 2.系统限制 3.目标应用不支持")
                showToast("手势执行失败，请检查权限")
            } else {
                Log.d(TAG, "手势开始执行")
            }
        } catch (e: Exception) {
            Log.e(TAG, "执行手势时发生异常: ${e.message}")
            e.printStackTrace()
            showToast("执行手势时发生错误: ${e.message}")
        }
    }

    /**
     * 执行双指缩小手势
     */
    fun performPinchInGesture() {
        // 更新屏幕尺寸，以适应屏幕旋转
        updateScreenDimensions()

        // 屏幕中心点
        var centerX = screenWidth / 2f
        var centerY = screenHeight / 2f

        // 计算起始点和结束点
        // 根据屏幕方向选择合适的参考尺寸
        val referenceSize = if (isLandscape) screenHeight else screenWidth
        val startDistance = referenceSize * (BASE_DISTANCE_FACTOR + ZOOM_FACTOR)
        val endDistance = referenceSize * BASE_DISTANCE_FACTOR

        // 第一个手指的起始点和结束点
        val finger1Start = PointF(centerX - startDistance / 2, centerY)
        val finger1End = PointF(centerX - endDistance / 2, centerY)

        // 第二个手指的起始点和结束点
        val finger2Start = PointF(centerX + startDistance / 2, centerY)
        val finger2End = PointF(centerX + endDistance / 2, centerY)

        // 执行手势
        performPinchGesture(finger1Start, finger1End, finger2Start, finger2End)

        // 显示提示
        showToast("执行缩小手势")
        Log.d(TAG, "缩小手势: 从 $startDistance 到 $endDistance")
    }

    /**
     * 执行双指捏合手势
     */
    private fun performPinchGesture(
        finger1Start: PointF, finger1End: PointF, finger2Start: PointF, finger2End: PointF
    ) {
        try {
            // 创建第一个手指的路径
            val finger1Path = Path().apply {
                moveTo(finger1Start.x, finger1Start.y)

                // 添加一些曲线，使手势看起来更自然
                val controlX1 = (finger1Start.x + finger1End.x) / 2 + getRandomOffset()
                val controlY1 = (finger1Start.y + finger1End.y) / 2 + getRandomOffset()
                quadTo(controlX1, controlY1, finger1End.x, finger1End.y)
            }

            // 创建第二个手指的路径
            val finger2Path = Path().apply {
                moveTo(finger2Start.x, finger2Start.y)

                // 添加一些曲线
                val controlX2 = (finger2Start.x + finger2End.x) / 2 + getRandomOffset()
                val controlY2 = (finger2Start.y + finger2End.y) / 2 + getRandomOffset()
                quadTo(controlX2, controlY2, finger2End.x, finger2End.y)
            }

            // 创建手势描述
            val gestureBuilder = GestureDescription.Builder()

            // 添加第一个手指的路径
            val stroke1 = GestureDescription.StrokeDescription(
                finger1Path, 0, GESTURE_DURATION
            )
            gestureBuilder.addStroke(stroke1)

            // 添加第二个手指的路径
            val stroke2 = GestureDescription.StrokeDescription(
                finger2Path, 0, GESTURE_DURATION
            )
            gestureBuilder.addStroke(stroke2)

            // 执行手势
            val gestureDescription = gestureBuilder.build()
            val result = dispatchGesture(gestureDescription, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "手势执行完成")
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    super.onCancelled(gestureDescription)
                    Log.e(TAG, "手势被取消")
                    // 尝试再次执行
                    mainHandler.postDelayed({
                        Log.d(TAG, "尝试再次执行手势")
                        dispatchGesture(gestureDescription, null, null)
                    }, 500)
                }
            }, null)

            if (!result) {
                Log.e(TAG, "手势执行失败，可能原因：1.权限不足 2.系统限制 3.目标应用不支持")
                showToast("手势执行失败，请检查权限")
            } else {
                Log.d(TAG, "手势开始执行")
            }
        } catch (e: Exception) {
            Log.e(TAG, "执行手势时发生异常: ${e.message}")
            e.printStackTrace()
            showToast("执行手势时发生错误: ${e.message}")
        }
    }

    /**
     * 获取随机偏移量
     */
    private fun getRandomOffset(): Float {
        return random.nextFloat() * MAX_RANDOM_OFFSET * 2 - MAX_RANDOM_OFFSET
    }

    /**
     * 为点添加随机性
     */
    private fun addRandomness(point: PointF) {
        point.x += getRandomOffset()
        point.y += getRandomOffset()
    }

    /**
     * 在主线程显示Toast
     */
    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
} 