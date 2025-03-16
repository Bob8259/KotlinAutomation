
# KotlinAutomation

This repository implements **TouchDown**, **TouchMove**, and **TouchUp** functions for **rooted devices**, allowing simultaneous multi-finger simulation.  
For more details, refer to:  
- [Auto.js](https://github.com/openautojs/openautojs)  
- [QScript(按键精灵)](https://zimaoxy.com/docs/qscript/keydown/)  
- [LazyScript(懒人精灵)](http://www.lrappsoft.com/lrhelp/hong-kong-fang-fa/an-zhu-bu-fang.html?h=touchdown)  

## Key Features
- **Kotlin Automation**
- **Multi-finger and Multi-touch Simulation**
- **Root Access Required**
- **Instrumentation & InputManager Integration**

---

## Core Concept

Simulating a real touch is the key challenge in automation. While the **Accessibility Service** allows touch simulations, it requires defining the entire touch sequence **before dispatching events**. 

However, in cases where the entire touch stroke is unknown beforehand, injecting touch events dynamically becomes difficult. Even with root access, **directly injecting gestures into the system is not possible** due to permission restrictions, specifically the `INJECT_EVENTS` permission. 

This repository overcomes this limitation using **InputManager reflection**, allowing dynamic multi-touch simulation.

---
Because I war born in China mainland and currently living in China Hong Kong, so I wrote all the comments in Chinese. But I wrote the readme in English because I hope this could help more people.
The following codes are all **translated by AI**, so they may contain errors. Please refer to the repo for the original code.
## 1. MultiSwipeSimulator Class (Core Implementation)

```kotlin
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

                // First touch point (finger 1) presses down
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

                // Second touch point (finger 2) presses down
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

                // Calculate movement steps
                val steps = (duration / 20).coerceAtLeast(1)
                val stepDuration = duration / steps
                val deltaX1 = (x1End - x1Start).toFloat()
                val deltaY1 = (y1End - y1Start).toFloat()
                val deltaX2 = (x2End - x2Start).toFloat()
                val deltaY2 = (y2End - y2Start).toFloat()

                // Send movement events
                repeat(steps.toInt()) { step ->
                    eventTime += stepDuration
                    val progress = (step + 1f) / steps

                    val currentCoord0 = PointerCoords().apply {
                        x = x1Start + deltaX1 * progress
                        y = y1Start + deltaY1 * progress
                    }
                    val currentCoord1 = PointerCoords().apply {
                        x = x2Start + deltaX2 * progress
                        y = y2Start + deltaY2 * progress
                    }

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

                // Lift second touch point
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

                // Lift first touch point
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
    }
}
```

---

## 2. Executing the Class with Root Access

To run this class on a **rooted Android device**, build an APK and place it in your device storage.  
Then, use `app_process` to execute it. You can use **libsu** or simply `"su -c"`.

```kotlin
fun runAppProcessAsRoot() {
    try {
        // Modify the APK path accordingly!
        val command =
            "app_process -Djava.class.path=/sdcard/10.apk /data/local/tmp com.coc.zkq.MultiSwipeSimulator 200 500 500 500 200 100 500 600 1000"
        val result = Shell.su(command).exec()

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
