# KotlinAutomation

This repo realized TouchDown, TouchMove, and TouchUp functions for Rooted devices. (本仓库使用Kotlin实现root设备上的多点按下，滑动，抬起操作。可同时模拟多根手指).For the details of them, you may refer to [Auto.js](https://github.com/openautojs/openautojs), [按键精灵](https://zimaoxy.com/docs/qscript/keydown/) or [懒人精灵](http://www.lrappsoft.com/lrhelp/hong-kong-fang-fa/an-zhu-bu-fang.html?h=touchdown)

### Key Words
Kotlin Automation, Multi-finger, Multi touches, Root, Instumentation, InputManager.

### Core part
Simulating a real touch is the key part of automation. You can use the Accessibility service to achieve it, but you need to set the whole stroke before dispatching the touches. What if you do not know the whole stroke before sending it? After trying, I found it was extremely hard. Because even with the root permission, you still can not directly inject the gestures into the system. You will get SecurityExecption saying you do not have INJECT_EVENTS permission. So the core part of this repo is here:

### 1 Leave a Class for us to call later
(I was born in China Mainland and currently, I am living in China Hong Kong. I am too lazy to translate all the comments into English. But I wrote this repo in Eng because I hope this could help more people.)


```
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
```

### 2 Call this class using Root access.
You need to build an apk, and put it inside your device. Then use app_process to call it. I used libsu here, but a simple "su -c" should also be working. Here, the path '/sdcard/10.apk' is where you put your apk file.
'com.coc.zkq' is the package name, and you may need to modify this. 'MultiSwipeSimulator' is the class name. The following numbers are just some parameters.
```
fun runAppProcessAsRoot() {
    try {
        // 使用 libsu 执行命令,记得修改路径 (Remember to modify the path!!)
        val command =
            "app_process -Djava.class.path=/sdcard/10.apk /data/local/tmp com.coc.zkq.MultiSwipeSimulator 200 500 500 500 200 100 500 600 1000"
        val result = Shell.su(command).exec()

        // 输出命令执行结果
        if (result.isSuccess) {
            Log.d("test", "Command executed successfully.")
            result.out.forEach { println(it) }
        } else {
            Log.d("test", "Command failed with exit code ${result.code}.")
            result.err.forEach { println(it) }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```
