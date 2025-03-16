package com.example.touch

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
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

class TouchGestureService : AccessibilityService() {

    companion object {
        private const val TAG = "TouchGestureService"
        private const val GESTURE_DURATION = 800L // 增加手势持续时间（毫秒）
        private const val ZOOM_FACTOR = 0.5f // 增加缩放因子
        private const val MAX_RANDOM_OFFSET = 5 // 减小随机偏移量，使手势更精确
        private const val BASE_DISTANCE_FACTOR = 0.15f // 增加基础距离因子
        private const val DEFAULT_MOVE_DURATION = 300L // 默认移动持续时间（毫秒）

        // Android系统限制手势最大持续时间为30秒(30000毫秒)
        private const val LONG_PRESS_DURATION = 29000L // 长按持续时间（29秒，接近但不超过系统限制）
    }

    private lateinit var mainHandler: Handler
    private lateinit var random: Random
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var isLandscape: Boolean = false

    // 存储当前活动的手指位置，键为手指ID，值为当前坐标
    private val activeFingers = ConcurrentHashMap<Int, PointF>()

    // 存储当前活动的手势描述，键为手指ID
    private val activeGestures = ConcurrentHashMap<Int, GestureDescription.StrokeDescription>()
    
    // 存储每个手指ID的手势轨迹列表
    private val fingerStrokes = mutableMapOf<Int, MutableList<GestureDescription.StrokeDescription>>()
    
    // 存储每个手指ID的累积时间偏移
    private val timeOffsets = mutableMapOf<Int, Long>()

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
        // 清除所有活动的手势
        cancelAllGestures()

        // 清除实例
        TouchGestureServiceHolder.setInstance(null)
        return super.onUnbind(intent)
    }

    /**
     * 手指按下事件
     * @param x X坐标
     * @param y Y坐标
     * @param fingerId 手指ID，用于区分不同的手指
     * @param duration 按下持续时间（毫秒）
     * @return 是否成功
     */
    fun touchdown(x: Float, y: Float, fingerId: Int, duration: Long): Boolean {
        Log.d(TAG, "手指按下: ID=$fingerId, 坐标=($x, $y), 持续时间=$duration")

        // 检查坐标是否在屏幕范围内
        if (x < 0 || x > screenWidth || y < 0 || y > screenHeight) {
            Log.e(TAG, "坐标超出屏幕范围")
            return false
        }

        // 如果该手指已经按下，先清除之前的状态
        if (activeFingers.containsKey(fingerId)) {
            Log.d(TAG, "手指ID=$fingerId 已经按下，清除之前的状态")
            // 清除之前的手势
            activeGestures.remove(fingerId)
            fingerStrokes.remove(fingerId)
            timeOffsets.remove(fingerId)
        }

        // 保存手指位置
        activeFingers[fingerId] = PointF(x, y)
        
        try {
            // 创建路径
            val path = Path().apply {
                moveTo(x, y)
            }
            
            // 初始化时间偏移为0
            timeOffsets[fingerId] = 0
            
            // 创建按下手势描述
            val stroke = GestureDescription.StrokeDescription(
                path,
                0, // 开始时间，初始为0
                duration, // 持续时间
                true // 保持按下状态
            )
            
            // 保存手势描述
            activeGestures[fingerId] = stroke
            
            // 初始化或清空该手指的轨迹列表
            fingerStrokes[fingerId] = mutableListOf<GestureDescription.StrokeDescription>().apply {
                add(stroke)
            }
            
            // 更新时间偏移
            timeOffsets[fingerId] = timeOffsets[fingerId]!! + duration
            
            Log.d(TAG, "手指ID=$fingerId 按下手势已创建，持续时间=$duration，累积时间=${timeOffsets[fingerId]}")
            
            // 立即执行按下手势
            val builder = GestureDescription.Builder()
            builder.addStroke(stroke)
            
            val result = dispatchGesture(builder.build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "手指ID=$fingerId 按下手势执行完成")
                    // 注意：这里不清理资源，因为我们需要保持按下状态
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    super.onCancelled(gestureDescription)
                    Log.e(TAG, "手指ID=$fingerId 按下手势被取消")
                    // 手势被取消时也不清理资源，让后续操作决定是否清理
                }
            }, null)
            
            if (result) {
                Log.d(TAG, "手指ID=$fingerId 按下手势开始执行")
                return true
            } else {
                Log.e(TAG, "手指ID=$fingerId 按下手势执行失败")
                // 执行失败时清理资源
                activeFingers.remove(fingerId)
                activeGestures.remove(fingerId)
                fingerStrokes.remove(fingerId)
                timeOffsets.remove(fingerId)
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建按下手势时发生异常: ${e.message}")
            e.printStackTrace()
            
            // 清理资源
            activeFingers.remove(fingerId)
            activeGestures.remove(fingerId)
            fingerStrokes.remove(fingerId)
            timeOffsets.remove(fingerId)
            
            return false
        }
    }

    /**
     * 手指移动事件
     * @param x 目标X坐标
     * @param y 目标Y坐标
     * @param fingerId 手指ID，必须与之前的touchdown使用相同的ID
     * @param duration 移动持续时间（毫秒），默认为300毫秒
     * @return 是否成功
     */
    fun touchmove(
        x: Float,
        y: Float,
        fingerId: Int,
        duration: Long = DEFAULT_MOVE_DURATION
    ): Boolean {
        Log.d(TAG, "手指移动: ID=$fingerId, 目标坐标=($x, $y), 持续时间=$duration")

        // 检查该手指是否已经按下
        val startPoint = activeFingers[fingerId]
        if (startPoint == null) {
            Log.e(TAG, "手指ID=$fingerId 未找到，请先调用touchdown")
            return false
        }

        // 检查坐标是否在屏幕范围内
        if (x < 0 || x > screenWidth || y < 0 || y > screenHeight) {
            Log.e(TAG, "坐标超出屏幕范围")
            return false
        }

        // 更新手指位置
        val endPoint = PointF(x, y)
        activeFingers[fingerId] = endPoint
        Log.d(TAG, "手指移动: 从(${startPoint.x}, ${startPoint.y})到(${endPoint.x}, ${endPoint.y})")

        // 检查是否有该手指的轨迹列表和时间偏移
        if (!fingerStrokes.containsKey(fingerId) || !timeOffsets.containsKey(fingerId)) {
            Log.e(TAG, "手指ID=$fingerId 没有初始化轨迹列表或时间偏移，请先调用touchdown")
            return false
        }

        // 获取当前累积时间偏移
        val currentTimeOffset = timeOffsets[fingerId]!!
        Log.d(TAG, "手指ID=$fingerId 当前累积时间偏移: $currentTimeOffset")

        // 创建路径
        val path = Path().apply {
            moveTo(startPoint.x, startPoint.y)
            Log.d(TAG, "起始点坐标: (${startPoint.x}, ${startPoint.y})")

            // 添加一些曲线，使手势看起来更自然
            val controlX = (startPoint.x + endPoint.x) / 2 + getRandomOffset()
            val controlY = (startPoint.y + endPoint.y) / 2 + getRandomOffset()
            quadTo(controlX, controlY, endPoint.x, endPoint.y)
        }

        // 计算实际持续时间
        val actualDuration = duration.coerceAtLeast(1L)
        
        // 创建手势描述，使用累积时间作为开始时间
        val stroke = GestureDescription.StrokeDescription(
            path,
            0, // 开始时间设为0，因为我们会立即执行
            actualDuration,
            true // 不是抬起事件，保持按下状态
        )
        
        // 添加到该手指的轨迹列表
        fingerStrokes[fingerId]?.add(stroke)
        
        // 更新累积时间偏移
        timeOffsets[fingerId] = currentTimeOffset + actualDuration
        
        Log.d(TAG, "添加移动手势到手指ID=$fingerId 的轨迹列表，当前轨迹数量: ${fingerStrokes[fingerId]?.size}，新的累积时间: ${timeOffsets[fingerId]}")
        
        // 等待按下手势完成后再执行移动手势
        mainHandler.postDelayed({
            // 立即执行移动手势
            val builder = GestureDescription.Builder()
            builder.addStroke(stroke)
            
            val result = dispatchGesture(builder.build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "手指ID=$fingerId 移动手势执行完成")
                    // 注意：这里不清理资源，因为我们需要保持按下状态
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    super.onCancelled(gestureDescription)
                    Log.e(TAG, "手指ID=$fingerId 移动手势被取消")
                    // 手势被取消时也不清理资源，让后续操作决定是否清理
                }
            }, null)
            
            if (result) {
                Log.d(TAG, "手指ID=$fingerId 移动手势开始执行")
            } else {
                Log.e(TAG, "手指ID=$fingerId 移动手势执行失败")
            }
        }, duration) // 等待按下手势完成的时间
        
        return true
    }

    /**
     * 手指抬起事件
     * @param x X坐标
     * @param y Y坐标
     * @param fingerId 手指ID
     * @return 是否成功
     */
    fun touchup(x: Float, y: Float, fingerId: Int): Boolean {
        Log.d(TAG, "手指抬起: ID=$fingerId, 坐标=($x, $y)")

        // 检查该手指是否已经按下
        if (!activeFingers.containsKey(fingerId)) {
            Log.e(TAG, "手指ID=$fingerId 未找到，请先调用touchdown")
            return false
        }

        // 检查是否有该手指的轨迹列表和时间偏移
        if (!fingerStrokes.containsKey(fingerId) || !timeOffsets.containsKey(fingerId)) {
            Log.e(TAG, "手指ID=$fingerId 没有初始化轨迹列表或时间偏移，请先调用touchdown")
            return false
        }

        // 获取当前累积时间偏移
        val currentTimeOffset = timeOffsets[fingerId]!!
        Log.d(TAG, "手指ID=$fingerId 当前累积时间偏移: $currentTimeOffset")

        // 获取起始位置（最后一次记录的位置）
        val startPoint = activeFingers[fingerId]!!
        
        // 更新手指位置到最终位置
        activeFingers[fingerId] = PointF(x, y)

        try {
            // 创建一个从当前位置到目标位置的路径，而不是只在目标位置点一下
            val path = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                lineTo(x, y) // 使用直线连接，确保系统能识别为拖放操作的结束
            }

            // 创建抬起手势描述
            val stroke = GestureDescription.StrokeDescription(
                path,
                0, // 开始时间设为0，因为我们会立即执行
                200, // 持续时间增加到50毫秒，给系统更多时间识别
                false // 是抬起事件
            )
            
            // 更新累积时间偏移
            timeOffsets[fingerId] = currentTimeOffset + 200
            
            Log.d(TAG, "手指ID=$fingerId 抬起手势已创建，累积时间: ${timeOffsets[fingerId]}")
            
            // 等待移动手势完成后再执行抬起手势
            mainHandler.postDelayed({
                // 立即执行抬起手势
                val builder = GestureDescription.Builder()
                builder.addStroke(stroke)
                
                val result = dispatchGesture(builder.build(), object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        super.onCompleted(gestureDescription)
                        Log.d(TAG, "手指ID=$fingerId 抬起手势执行完成")
                        
                        // 手势完成后延迟清理资源，确保系统有足够时间处理拖放操作
                        mainHandler.postDelayed({
                            // 清理资源
                            activeGestures.remove(fingerId)
                            activeFingers.remove(fingerId)
                            fingerStrokes.remove(fingerId)
                            timeOffsets.remove(fingerId)
                            
                            Log.d(TAG, "手指ID=$fingerId 的资源已清理")
                        }, 500) // 延迟500毫秒清理资源
                    }

                    override fun onCancelled(gestureDescription: GestureDescription) {
                        super.onCancelled(gestureDescription)
                        Log.e(TAG, "手指ID=$fingerId 抬起手势被取消")
                        
                        // 手势被取消后也清理资源
                        mainHandler.post {
                            // 清理资源
                            activeGestures.remove(fingerId)
                            activeFingers.remove(fingerId)
                            fingerStrokes.remove(fingerId)
                            timeOffsets.remove(fingerId)
                            
                            Log.e(TAG, "手指ID=$fingerId 的资源已清理（手势被取消）")
                        }
                    }
                }, null)
                
                if (result) {
                    Log.d(TAG, "手指ID=$fingerId 抬起手势开始执行")
                } else {
                    Log.e(TAG, "手指ID=$fingerId 抬起手势执行失败")
                    
                    // 执行失败时立即清理资源
                    activeGestures.remove(fingerId)
                    activeFingers.remove(fingerId)
                    fingerStrokes.remove(fingerId)
                    timeOffsets.remove(fingerId)
                }
            }, 1000) // 等待移动手势完成的时间
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "创建手指ID=$fingerId 的抬起手势时发生异常: ${e.message}")
            e.printStackTrace()
            
            // 发生异常时清理资源
            activeGestures.remove(fingerId)
            activeFingers.remove(fingerId)
            fingerStrokes.remove(fingerId)
            timeOffsets.remove(fingerId)
            
            return false
        }
    }

    /**
     * 执行指定手指ID的手势
     * @param fingerId 手指ID
     * @return 是否成功执行
     */
    fun executeGesture(fingerId: Int): Boolean {
        // 检查该手指ID是否已初始化
        if (!fingerStrokes.containsKey(fingerId)) {
            Log.e(TAG, "手指ID=$fingerId 未初始化，无法执行手势")
            return false
        }

        val strokes = fingerStrokes[fingerId]!!
        if (strokes.isEmpty()) {
            Log.e(TAG, "手指ID=$fingerId 没有轨迹，无法执行手势")
            return false
        }

        Log.d(TAG, "执行手指ID=$fingerId 的手势，轨迹数量: ${strokes.size}")

        // 创建手势描述
        val builder = GestureDescription.Builder()
        for (stroke in strokes) {
            builder.addStroke(stroke)
        }

        // 执行手势
        val result = dispatchGesture(builder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "手指ID=$fingerId 手势执行完成")
                
                // 手势完成后清理资源
                mainHandler.post {
                    // 清空轨迹
                    strokes.clear()
                    
                    // 清理资源
                    activeGestures.remove(fingerId)
                    activeFingers.remove(fingerId)
                    fingerStrokes.remove(fingerId)
                    timeOffsets.remove(fingerId)
                    
                    Log.d(TAG, "手指ID=$fingerId 的资源已清理")
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                Log.e(TAG, "手指ID=$fingerId 手势被取消")
                
                // 手势被取消后也清理资源
                mainHandler.post {
                    // 清空轨迹
                    strokes.clear()
                    
                    // 清理资源
                    activeGestures.remove(fingerId)
                    activeFingers.remove(fingerId)
                    fingerStrokes.remove(fingerId)
                    timeOffsets.remove(fingerId)
                    
                    Log.e(TAG, "手指ID=$fingerId 的资源已清理（手势被取消）")
                }
            }
        }, null)

        if (result) {
            Log.d(TAG, "手指ID=$fingerId 手势开始执行")
        } else {
            Log.e(TAG, "手指ID=$fingerId 手势执行失败")
            
            // 执行失败时立即清理资源
            strokes.clear()
            activeGestures.remove(fingerId)
            activeFingers.remove(fingerId)
            fingerStrokes.remove(fingerId)
            timeOffsets.remove(fingerId)
        }

        return result
    }

    /**
     * 执行所有手指的手势
     * @return 是否成功执行
     */
    private fun executeAllGestures(): Boolean {
        if (fingerStrokes.isEmpty()) {
            Log.e(TAG, "没有任何手指轨迹，无法执行手势")
            return false
        }

        // 创建手势描述
        val builder = GestureDescription.Builder()

        // 添加所有手指的轨迹
        for ((fingerId, strokes) in fingerStrokes) {
            if (strokes.isEmpty()) continue

            for (stroke in strokes) {
                builder.addStroke(stroke)
            }

            // 清空该手指的轨迹
            strokes.clear()
        }

        // 执行手势
        return dispatchGesture(builder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "所有手势执行完成")
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                Log.e(TAG, "手势被取消")
            }
        }, null)
    }


    /**
     * 取消所有活动的手势
     */
    private fun cancelAllGestures() {
        Log.d(TAG, "取消所有活动的手势，共 ${activeGestures.size} 个")

        // 复制一份手指ID列表，避免并发修改异常
        val fingerIds = ArrayList(activeGestures.keys)

        // 逐个取消每个手指的手势
        for (fingerId in fingerIds) {
            cancelGesture(fingerId)
        }

        // 清空集合
        activeGestures.clear()

        // AccessibilityService没有cancelGesture方法，所以不能调用super.cancelGesture()
        Log.d(TAG, "所有手势已取消")
    }
    /**
     * 取消指定手指ID的手势
     * @param fingerId 手指ID
     */
    private fun cancelGesture(fingerId: Int) {
        // 获取该手指的手势描述
//        val stroke = activeGestures.remove(fingerId)
        fingerStrokes[fingerId]?.apply {

            Log.d(TAG, "取消手指ID=$fingerId 的手势 - 开始")

            try {
                // 创建一个新的手势描述，用于取消当前手势
                // 注意：AccessibilityService的cancelGesture()方法会取消所有手势，
                // 所以我们需要通过创建一个新的手势来"替换"当前手势

                // 创建一个与当前手指位置相同的路径
                val point = activeFingers[fingerId] ?: return
                val path = Path().apply {
                    moveTo(point.x, point.y)
                }

                val newStroke = GestureDescription.StrokeDescription(
                    path,
                    0, // 开始时间
                    1, // 持续时间为1毫秒
                    false // 是抬起事件
                )
                add(newStroke)
                Log.d(TAG, "取消手指ID=$fingerId 的手势 - 结束")
            } catch (e: Exception) {
                Log.e(TAG, "取消手指ID=$fingerId 的手势时发生异常: ${e.message}")
                e.printStackTrace()
            }
        }

    }
    /**
     * 执行双指放大手势
     */
    fun performPinchOutGesture() {
        // 更新屏幕尺寸，以适应屏幕旋转
        updateScreenDimensions()

        // 屏幕中心点
        var centerX = screenWidth / 2f
        var centerY = screenHeight / 2f

        // 添加一些随机性，使手势看起来更自然
        centerX += getRandomOffset()
        centerY += getRandomOffset()

        // 计算起始点和结束点
        // 根据屏幕方向选择合适的参考尺寸
        val referenceSize = if (isLandscape) screenHeight else screenWidth
        val startDistance = referenceSize * BASE_DISTANCE_FACTOR
        val endDistance = referenceSize * (BASE_DISTANCE_FACTOR + ZOOM_FACTOR)

        // 第一个手指的起始点和结束点
        val finger1Start = PointF(centerX - startDistance / 2, centerY)
        val finger1End = PointF(centerX - endDistance / 2, centerY)

        // 第二个手指的起始点和结束点
        val finger2Start = PointF(centerX + startDistance / 2, centerY)
        val finger2End = PointF(centerX + endDistance / 2, centerY)

        // 添加一些随机性
        addRandomness(finger1Start)
        addRandomness(finger1End)
        addRandomness(finger2Start)
        addRandomness(finger2End)

        // 创建路径对象，用于轨迹显示
        val finger1Path = createFingerPath(finger1Start, finger1End)
        val finger2Path = createFingerPath(finger2Start, finger2End)

        // 通知轨迹监听器
        GestureTrackListener.notifyGestureTrack(
            finger1Path, finger1Start, finger1End,
            finger2Path, finger2Start, finger2End
        )

        // 执行手势
        performPinchGesture(finger1Start, finger1End, finger2Start, finger2End)

        // 显示提示
        showToast("执行放大手势")
        Log.d(TAG, "放大手势: 从 $startDistance 到 $endDistance")
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

        // 添加一些随机性
        centerX += getRandomOffset()
        centerY += getRandomOffset()

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

        // 添加一些随机性
        addRandomness(finger1Start)
        addRandomness(finger1End)
        addRandomness(finger2Start)
        addRandomness(finger2End)

        // 创建路径对象，用于轨迹显示
        val finger1Path = createFingerPath(finger1Start, finger1End)
        val finger2Path = createFingerPath(finger2Start, finger2End)

        // 通知轨迹监听器
        GestureTrackListener.notifyGestureTrack(
            finger1Path, finger1Start, finger1End,
            finger2Path, finger2Start, finger2End
        )

        // 执行手势
        performPinchGesture(finger1Start, finger1End, finger2Start, finger2End)

        // 显示提示
        showToast("执行缩小手势")
        Log.d(TAG, "缩小手势: 从 $startDistance 到 $endDistance")
    }

    /**
     * 创建手指路径，用于轨迹显示
     */
    private fun createFingerPath(startPoint: PointF, endPoint: PointF): Path {
        return Path().apply {
            moveTo(startPoint.x, startPoint.y)

            // 添加一些曲线，使手势看起来更自然
            val controlX = (startPoint.x + endPoint.x) / 2 + getRandomOffset()
            val controlY = (startPoint.y + endPoint.y) / 2 + getRandomOffset()
            quadTo(controlX, controlY, endPoint.x, endPoint.y)
        }
    }

    /**
     * 执行双指捏合手势
     */
    private fun performPinchGesture(
        finger1Start: PointF,
        finger1End: PointF,
        finger2Start: PointF,
        finger2End: PointF
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