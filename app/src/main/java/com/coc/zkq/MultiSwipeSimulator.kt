package com.coc.zkq

import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import java.lang.Thread.sleep
import java.lang.reflect.Method

class MultiSwipeSimulator {
    companion object {
        @JvmStatic
        fun simulateSingleSwipe(
            startX: Int, startY: Int, endX: Int, endY: Int, duration: Long
        ) {
            try {
                // get InputManager
                val inputManagerClass = Class.forName("android.hardware.input.InputManager")
                val getInstanceMethod: Method = inputManagerClass.getMethod("getInstance")
                val inputManager = getInstanceMethod.invoke(null)


                val injectInputEventMethod: Method = inputManagerClass.getMethod(
                    "injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType
                )


                val downTime = SystemClock.uptimeMillis()
                var eventTime = downTime

                // touch down
//                var downEvent = MotionEvent.obtain(
//                    downTime,
//                    eventTime,
//                    MotionEvent.ACTION_DOWN,
//                    startX.toFloat(),
//                    startY.toFloat(),
//                    0
//                )
//                injectInputEventMethod.invoke(inputManager, downEvent, 0)
//                sleep(1000)
                val downEvent2 = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_DOWN, // Use ACTION_POINTER_DOWN for the second finger
                    startX.toFloat() + 100,
                    startY.toFloat() + 100,
                    0 // Assign a different pointer ID (e.g., 1)
                )
//
                injectInputEventMethod.invoke(inputManager, downEvent2, 0)
                sleep(2000)
                // touch move
                val steps = 100
                val stepDuration = duration / steps
                val stepX = (endX - startX) / steps.toFloat()
                val stepY = (endY - startY) / steps.toFloat()

                for (i in 1 until steps) {
                    eventTime += stepDuration
                    val moveX = startX + stepX * i
                    val moveY = startY + stepY * i

                    // create movement
                    val moveEvent = MotionEvent.obtain(
                        downTime, eventTime, MotionEvent.ACTION_MOVE, moveX, moveY, 0
                    )
                    injectInputEventMethod.invoke(inputManager, moveEvent, 0)
                    moveEvent.recycle()
                    sleep(stepDuration)
                }

                // touch up
                val upEvent = MotionEvent.obtain(
                    downTime, eventTime, MotionEvent.ACTION_UP, endX.toFloat(), endY.toFloat(), 0
                )
                injectInputEventMethod.invoke(inputManager, upEvent, 0)

                // release
//                downEvent.recycle()
                downEvent2.recycle()
                upEvent.recycle()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        @JvmStatic
        fun simulateDualTouchSwipe(
            x1Start: Int, y1Start: Int, x1End: Int, y1End: Int,
            x2Start: Int, y2Start: Int, x2End: Int, y2End: Int,
            duration: Long
        ) {
            try {
                val inputManagerClass = Class.forName("android.hardware.input.InputManager")
                val getInstanceMethod = inputManagerClass.getMethod("getInstance")
                val inputManager = getInstanceMethod.invoke(null)
                val injectInputEventMethod = inputManagerClass.getMethod(
                    "injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType
                )

                val downTime = SystemClock.uptimeMillis()
                var eventTime = downTime

                // 第一个触点按下
                val prop0 = PointerProperties().apply {
                    id = 0
                    toolType = MotionEvent.TOOL_TYPE_FINGER
                }
                val initialCoord0 = PointerCoords().apply {
                    x = x1Start.toFloat()
                    y = y1Start.toFloat()
                }
                val downEvent = MotionEvent.obtain(
                    downTime, eventTime,
                    MotionEvent.ACTION_DOWN, 1,
                    arrayOf(prop0), arrayOf(initialCoord0),
                    0, 0, 1f, 1f, 0, 0,
                    InputDevice.SOURCE_TOUCHSCREEN, 0
                )
                injectInputEventMethod.invoke(inputManager, downEvent, 0)
                downEvent.recycle()

                // 添加第二个触点
                eventTime += 20
                val prop1 = PointerProperties().apply {
                    id = 1
                    toolType = MotionEvent.TOOL_TYPE_FINGER
                }
                val initialCoord1 = PointerCoords().apply {
                    x = x2Start.toFloat()
                    y = y2Start.toFloat()
                }
                val pointerDownAction = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                val pointerDownEvent = MotionEvent.obtain(
                    downTime, eventTime, pointerDownAction, 2,
                    arrayOf(prop0, prop1), arrayOf(initialCoord0, initialCoord1),
                    0, 0, 1f, 1f, 0, 0,
                    InputDevice.SOURCE_TOUCHSCREEN, 0
                )
                injectInputEventMethod.invoke(inputManager, pointerDownEvent, 0)
                pointerDownEvent.recycle()

                // 计算滑动参数
                val steps = (duration / 20).coerceAtLeast(1)
                val stepDuration = duration / steps
                val deltaX1 = (x1End - x1Start).toFloat()
                val deltaY1 = (y1End - y1Start).toFloat()
                val deltaX2 = (x2End - x2Start).toFloat()
                val deltaY2 = (y2End - y2Start).toFloat()

                // 发送连续移动事件
                repeat(steps.toInt()) { step ->
                    eventTime += stepDuration
                    val progress = (step + 1f) / steps

                    // 计算当前坐标
                    val currentCoord0 = PointerCoords().apply {
                        x = x1Start + deltaX1 * progress
                        y = y1Start + deltaY1 * progress
                    }
                    val currentCoord1 = PointerCoords().apply {
                        x = x2Start + deltaX2 * progress
                        y = y2Start + deltaY2 * progress
                    }

                    // 发送移动事件
                    val moveEvent = MotionEvent.obtain(
                        downTime, eventTime,
                        MotionEvent.ACTION_MOVE, 2,
                        arrayOf(prop0, prop1), arrayOf(currentCoord0, currentCoord1),
                        0, 0, 1f, 1f, 0, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0
                    )
                    injectInputEventMethod.invoke(inputManager, moveEvent, 0)
                    moveEvent.recycle()
                    SystemClock.sleep(stepDuration)
                }

                // 抬起第二个触点
                eventTime += stepDuration
                val pointerUpAction = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                val pointerUpEvent = MotionEvent.obtain(
                    downTime, eventTime, pointerUpAction, 2,
                    arrayOf(prop0, prop1), arrayOf(
                        PointerCoords().apply { x = x1End.toFloat(); y = y1End.toFloat() },
                        PointerCoords().apply { x = x2End.toFloat(); y = y2End.toFloat() }
                    ),
                    0, 0, 1f, 1f, 0, 0,
                    InputDevice.SOURCE_TOUCHSCREEN, 0
                )
                injectInputEventMethod.invoke(inputManager, pointerUpEvent, 0)
                pointerUpEvent.recycle()

                // 抬起第一个触点
                eventTime += 20
                val upEvent = MotionEvent.obtain(
                    downTime, eventTime,
                    MotionEvent.ACTION_UP, 1,
                    arrayOf(prop0), arrayOf(
                        PointerCoords().apply { x = x1End.toFloat(); y = y1End.toFloat() }
                    ),
                    0, 0, 1f, 1f, 0, 0,
                    InputDevice.SOURCE_TOUCHSCREEN, 0
                )
                injectInputEventMethod.invoke(inputManager, upEvent, 0)
                upEvent.recycle()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            // 检查参数数量是否正确
            if (args.size != 9) {
                println("Usage: MultiSwipeSimulator startX1 startY1 endX1 endY1 startX2 startY2 endX2 endY2 duration")
                return
            }

            try {
                // 解析参数
                val startX1 = args[0].toInt()
                val startY1 = args[1].toInt()
                val endX1 = args[2].toInt()
                val endY1 = args[3].toInt()

                val startX2 = args[4].toInt()
                val startY2 = args[5].toInt()
                val endX2 = args[6].toInt()
                val endY2 = args[7].toInt()

                val duration = args[8].toLong()
                // 调用 simulateMultiSwipe 方法
                simulateDualTouchSwipe(
                    startX1, startY1, endX1, endY1,
                    startX2, startY2, endX2, endY2,
                    duration
                )
//                simulateDualTouchSwipe(
//                    200, 500, 600, 300, 1000, 500, 700, 300, 1000
//                )
            } catch (e: NumberFormatException) {
                println("Invalid argument format. All arguments must be integers except duration (long).")
                e.printStackTrace()
            } catch (e: Exception) {
                println("An error occurred during execution.")
                e.printStackTrace()
            }
        }
    }
}