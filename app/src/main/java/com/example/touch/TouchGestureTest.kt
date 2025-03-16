package com.example.touch

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.content.Context

/**
 * 触摸手势测试类
 * 提供了一系列测试方法，用于验证TouchGestureService的功能
 */
object TouchGestureTest {
    private const val TAG = "TouchGestureTest"
    private const val DEFAULT_FINGER_ID = 1

    /**
     * 测试长按并拖动场景（如桌面图标拖动）
     * @param context 上下文，用于显示Toast
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 结束X坐标
     * @param endY 结束Y坐标
     * @param longPressTime 长按时间（毫秒）
     * @param moveTime 移动时间（毫秒）
     */
    fun testLongPressAndDrag(
        context: Context,
        startX: Float = 200f,
        startY: Float = 400f,
        endX: Float = 500f,
        endY: Float = 600f,
        longPressTime: Long = 1000,
        moveTime: Long = 500
    ) {
        val service = TouchGestureServiceHolder.instance
        if (service == null) {
            Toast.makeText(context, "辅助功能服务未连接", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "开始测试长按并拖动", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "开始测试长按并拖动")

        // 在主线程外执行，避免阻塞UI
        Thread {
            try {
                // 1. 手指按下
                val fingerId = DEFAULT_FINGER_ID
                Log.d(TAG, "1. 手指按下 ($startX, $startY)")
                service.touchdown(startX, startY, fingerId, longPressTime)
                
                // 2. 等待长按手势完成
                Log.d(TAG, "2. 等待长按手势完成，时间为 $longPressTime ms")
                Thread.sleep(longPressTime + 500) // 额外等待500ms确保手势完成
                
                // 3. 移动到目标位置
                Log.d(TAG, "3. 移动到目标位置 ($endX, $endY)")
                service.touchmove(endX, endY, fingerId, moveTime)
                
                // 4. 等待移动手势完成
                Log.d(TAG, "4. 等待移动手势完成，时间为 $moveTime ms")
                Thread.sleep(moveTime + 500) // 额外等待500ms确保手势完成
                
                // 5. 手指抬起
                Log.d(TAG, "5. 手指抬起")
                service.touchup(endX, endY, fingerId)
                
                // 6. 等待抬起手势完成
                Log.d(TAG, "6. 等待抬起手势完成")
                Thread.sleep(2000) // 增加等待时间，确保抬起手势和资源清理完全完成
                
                // 在主线程显示完成提示
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "长按并拖动测试完成", Toast.LENGTH_SHORT).show()
                }
                Log.d(TAG, "长按并拖动测试完成")
            } catch (e: Exception) {
                Log.e(TAG, "测试过程中发生异常: ${e.message}")
                e.printStackTrace()

                // 在主线程显示错误提示
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    /**
     * 测试多指捏合（缩小）场景
     * @param context 上下文，用于显示Toast
     * @param centerX 中心点X坐标
     * @param centerY 中心点Y坐标
     * @param startDistance 起始距离
     * @param endDistance 结束距离
     * @param duration 持续时间（毫秒）
     */
    fun testPinchIn(
        context: Context,
        centerX: Float = 500f,
        centerY: Float = 800f,
        startDistance: Float = 300f,
        endDistance: Float = 100f,
        duration: Long = 500
    ) {
        val service = TouchGestureServiceHolder.instance
        if (service == null) {
            Toast.makeText(context, "辅助功能服务未连接", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "开始测试双指捏合", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "开始测试双指捏合")

        // 在主线程外执行，避免阻塞UI
        Thread {
            try {
                // 使用手指ID 1和2
                val finger1Id = 1
                val finger2Id = 2

                // 计算起始点
                val finger1StartX = centerX - startDistance / 2
                val finger1StartY = centerY
                val finger2StartX = centerX + startDistance / 2
                val finger2StartY = centerY

                // 计算结束点
                val finger1EndX = centerX - endDistance / 2
                val finger1EndY = centerY
                val finger2EndX = centerX + endDistance / 2
                val finger2EndY = centerY

                // 1. 两个手指按下
                Log.d(TAG, "1. 两个手指按下")
                service.touchdown(finger1StartX, finger1StartY, finger1Id, 100)
                service.touchdown(finger2StartX, finger2StartY, finger2Id, 100)

                // 2. 等待按下手势完成
                Log.d(TAG, "2. 等待按下手势完成")
                Thread.sleep(100 + 500) // 额外等待500ms确保手势完成

                // 3. 两个手指同时移动
                Log.d(TAG, "3. 两个手指同时移动")
                service.touchmove(finger1EndX, finger1EndY, finger1Id, duration)
                service.touchmove(finger2EndX, finger2EndY, finger2Id, duration)

                // 4. 等待移动手势完成
                Log.d(TAG, "4. 等待移动手势完成，时间为 $duration ms")
                Thread.sleep(duration + 500) // 额外等待500ms确保手势完成

                // 5. 两个手指抬起
                Log.d(TAG, "5. 两个手指抬起")
                service.touchup(finger1EndX, finger1EndY, finger1Id)
                service.touchup(finger2EndX, finger2EndY, finger2Id)
                
                // 6. 等待抬起手势完成
                Log.d(TAG, "6. 等待抬起手势完成")
                Thread.sleep(1500) // 等待抬起手势完成，包括延迟执行的时间

                // 在主线程显示完成提示
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "双指捏合测试完成", Toast.LENGTH_SHORT).show()
                }
                Log.d(TAG, "双指捏合测试完成")
            } catch (e: Exception) {
                Log.e(TAG, "测试过程中发生异常: ${e.message}")
                e.printStackTrace()

                // 在主线程显示错误提示
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    /**
     * 测试多指张开（放大）场景
     * @param context 上下文，用于显示Toast
     * @param centerX 中心点X坐标
     * @param centerY 中心点Y坐标
     * @param startDistance 起始距离
     * @param endDistance 结束距离
     * @param duration 持续时间（毫秒）
     */
    fun testPinchOut(
        context: Context,
        centerX: Float = 500f,
        centerY: Float = 800f,
        startDistance: Float = 100f,
        endDistance: Float = 300f,
        duration: Long = 500
    ) {
        val service = TouchGestureServiceHolder.instance
        if (service == null) {
            Toast.makeText(context, "辅助功能服务未连接", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "开始测试双指张开", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "开始测试双指张开")

        // 在主线程外执行，避免阻塞UI
        Thread {
            try {
                // 使用手指ID 1和2
                val finger1Id = 1
                val finger2Id = 2

                // 计算起始点
                val finger1StartX = centerX - startDistance / 2
                val finger1StartY = centerY
                val finger2StartX = centerX + startDistance / 2
                val finger2StartY = centerY

                // 计算结束点
                val finger1EndX = centerX - endDistance / 2
                val finger1EndY = centerY
                val finger2EndX = centerX + endDistance / 2
                val finger2EndY = centerY

                // 1. 两个手指按下
                Log.d(TAG, "1. 两个手指按下")
                service.touchdown(finger1StartX, finger1StartY, finger1Id, 100)
                service.touchdown(finger2StartX, finger2StartY, finger2Id, 100)

                // 2. 等待按下手势完成
                Log.d(TAG, "2. 等待按下手势完成")
                Thread.sleep(100 + 500) // 额外等待500ms确保手势完成

                // 3. 两个手指同时移动
                Log.d(TAG, "3. 两个手指同时移动")
                service.touchmove(finger1EndX, finger1EndY, finger1Id, duration)
                service.touchmove(finger2EndX, finger2EndY, finger2Id, duration)

                // 4. 等待移动手势完成
                Log.d(TAG, "4. 等待移动手势完成，时间为 $duration ms")
                Thread.sleep(duration + 500) // 额外等待500ms确保手势完成

                // 5. 两个手指抬起
                Log.d(TAG, "5. 两个手指抬起")
                service.touchup(finger1EndX, finger1EndY, finger1Id)
                service.touchup(finger2EndX, finger2EndY, finger2Id)
                
                // 6. 等待抬起手势完成
                Log.d(TAG, "6. 等待抬起手势完成")
                Thread.sleep(1500) // 等待抬起手势完成，包括延迟执行的时间

                // 在主线程显示完成提示
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "双指张开测试完成", Toast.LENGTH_SHORT).show()
                }
                Log.d(TAG, "双指张开测试完成")
            } catch (e: Exception) {
                Log.e(TAG, "测试过程中发生异常: ${e.message}")
                e.printStackTrace()

                // 在主线程显示错误提示
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    /**
     * 测试连续点击场景
     * @param context 上下文，用于显示Toast
     * @param x X坐标
     * @param y Y坐标
     * @param count 点击次数
     * @param interval 点击间隔（毫秒）
     */
    fun testTaps(
        context: Context,
        x: Float = 500f,
        y: Float = 800f,
        count: Int = 5,
        interval: Long = 200
    ) {
        val service = TouchGestureServiceHolder.instance
        if (service == null) {
            Toast.makeText(context, "辅助功能服务未连接", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "开始测试连续点击", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "开始测试连续点击")

        // 在主线程外执行，避免阻塞UI
        Thread {
            try {
                val fingerId = DEFAULT_FINGER_ID

                for (i in 1..count) {
                    Log.d(TAG, "执行第 $i 次点击")

                    // 按下
                    service.touchdown(x, y, fingerId, 50)

                    // 等待按下手势完成
                    Log.d(TAG, "等待按下手势完成")
                    Thread.sleep(50 + 200) // 额外等待200ms确保手势完成

                    // 抬起
                    service.touchup(x, y, fingerId)
                    
                    // 等待抬起手势完成
                    Log.d(TAG, "等待抬起手势完成")
                    Thread.sleep(1200) // 等待抬起手势完成，包括延迟执行的时间

                    // 如果不是最后一次点击，等待一段时间再执行下一次
                    if (i < count) {
                        Log.d(TAG, "等待 $interval ms 后执行下一次点击")
                        Thread.sleep(interval)
                    }
                }

                // 在主线程显示完成提示
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "连续点击测试完成", Toast.LENGTH_SHORT).show()
                }
                Log.d(TAG, "连续点击测试完成")
            } catch (e: Exception) {
                Log.e(TAG, "测试过程中发生异常: ${e.message}")
                e.printStackTrace()

                // 在主线程显示错误提示
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}