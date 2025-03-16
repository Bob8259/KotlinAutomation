package com.coc.zkq

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.InputEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coc.zkq.ui.theme.ZkqTheme
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.lang.reflect.Method

class MainActivity : ComponentActivity() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        requestOverlayPermission()//直接使用root获取悬浮窗权限，节省用户手动授权的时间
        enableAccessibilityService(this)//直接使用root获取accessibility权限，节省用户手动授权的时间
        setContent {
            ZkqTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ClashOfClansButton(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    fun enableAccessibilityService(context: Context) {
        try {
            val command =
                "settings put secure enabled_accessibility_services " + "${context.packageName}/${TouchGestureService::class.java.name}"
            Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 创建悬浮窗布局
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window_layout, null)

        // 设置悬浮窗参数
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // 设置悬浮窗位置
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 0
        layoutParams.y = 400

        // 添加悬浮窗到窗口管理器
        windowManager.addView(floatingView, layoutParams)

        // 设置悬浮窗的点击事件
        floatingView.setOnClickListener {
            Toast.makeText(this, "Floating Window Clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private val REQUEST_CODE = 1
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!Settings.canDrawOverlays(this)) {
                grantOverlayPermissionWithRoot()
            } else {
                showFloatingWindow()
            }
        }
    }

    private fun grantOverlayPermissionWithRoot() {
        try {
            // 使用 root 权限授予悬浮窗权限
            val command = "pm grant $packageName android.permission.SYSTEM_ALERT_WINDOW"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor() // 等待命令执行完成

            // 检查是否成功授予权限
            if (Settings.canDrawOverlays(this)) {
                // 权限授予成功，显示悬浮窗
                showFloatingWindow()
            } else {
                // 权限授予失败，提示用户
                Toast.makeText(this, "无法通过 root 权限授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                // 回退到正常请求权限的方式
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果 root 权限获取失败，回退到正常请求权限的方式
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // 权限已授予，可以创建悬浮窗
                    showFloatingWindow()
                } else {
                    // 权限未授予，提示用户
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}


@Composable
fun ClashOfClansButton(modifier: Modifier = Modifier) {
    val TAG = "test"
    val isRunning = mutableStateOf(false) // 用于控制任务的状态
    val coroutineScope = rememberCoroutineScope() // 获取协程作用域
    val context = LocalContext.current // 获取当前上下文

    Button(
        onClick = {
            isRunning.value = !isRunning.value // 切换任务状态
            if (isRunning.value) {
                coroutineScope.launch(Dispatchers.IO) { // 使用 Dispatchers.IO 调度到后台线程
                    if (Shell.getShell().isRoot) {
                        startClashOfClans(context)
                        showToast(context, "Waiting for 3 seconds...")
                        delay(3000)
                        // 使用root权限模拟双指捏合，需要确保把apk放到了正确的路径
//                        runAppProcessAsRoot()
                        //使用Accessibility进行双指捏合
                        val temp = TouchGestureServiceHolder.instance
                        temp?.performPinchInGesture()
                        showToast(context, "Pinch to zoom executed")
                    } else {
                        showToast(context, "Root access is not available")
                    }
                    isRunning.value = false // 重置状态
                }
            }
        }, modifier = modifier
    ) {
        Text(if (isRunning.value) "Running..." else "Start Clash of Clans")
    }
    Button(
        onClick = {
            // 输出日志
            Log.d(TAG, "Button clicked!")
            showToast(context, "Button clicked!")
        }, modifier = modifier.padding(top = 56.dp) // 设置按钮的上边距
    ) {
        Text("Log Button")
    }
}

fun runAppProcessAsRoot() {
    try {
        // 使用 libsu 执行命令,记得修改路径
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

fun simulateMultiSwipe(
    startX1: Int,
    startY1: Int,
    endX1: Int,
    endY1: Int,
    startX2: Int,
    startY2: Int,
    endX2: Int,
    endY2: Int,
    duration: Long
) {
    try {
        // 获取 InputManager 实例
        val inputManagerClass = Class.forName("android.hardware.input.InputManager")
        val getInstanceMethod: Method = inputManagerClass.getMethod("getInstance")
        val inputManager = getInstanceMethod.invoke(null)

        // 获取 injectInputEvent 方法
        val injectInputEventMethod: Method = inputManagerClass.getMethod(
            "injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType
        )

        // 创建 MotionEvent 对象
        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime

        // 第一个手指按下事件
        val downEvent1 = MotionEvent.obtain(
            downTime, eventTime, MotionEvent.ACTION_DOWN, startX1.toFloat(), startY1.toFloat(), 0
        )
        injectInputEventMethod.invoke(inputManager, downEvent1, 0)

        // 第二个手指按下事件
        val downEvent2 = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            startX2.toFloat(),
            startY2.toFloat(),
            0
        )
        injectInputEventMethod.invoke(inputManager, downEvent2, 0)

        // 滑动过程中的移动事件
        val steps = 10
        val stepDuration = duration / steps
        val stepX1 = (endX1 - startX1) / steps.toFloat()
        val stepY1 = (endY1 - startY1) / steps.toFloat()
        val stepX2 = (endX2 - startX2) / steps.toFloat()
        val stepY2 = (endY2 - startY2) / steps.toFloat()

        for (i in 1 until steps) {
            eventTime += stepDuration
            val moveX1 = startX1 + stepX1 * i
            val moveY1 = startY1 + stepY1 * i
            val moveX2 = startX2 + stepX2 * i
            val moveY2 = startY2 + stepY2 * i

            // 创建 PointerProperties 和 PointerCoords 对象
            val pointerProperties = arrayOf(PointerProperties().apply {
                id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER
            }, PointerProperties().apply { id = 1; toolType = MotionEvent.TOOL_TYPE_FINGER })
            val pointerCoords = arrayOf(PointerCoords().apply { x = moveX1; y = moveY1 },
                PointerCoords().apply { x = moveX2; y = moveY2 })

            // 创建包含两个手指的移动事件
            val moveEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_MOVE,
                2,
                pointerProperties,
                pointerCoords,
                0,
                0,
                1f,
                1f,
                0,
                0,
                0,
                0
            )
            injectInputEventMethod.invoke(inputManager, moveEvent, 0)
            moveEvent.recycle()
            sleep(stepDuration)
        }

        // 第二个手指抬起事件
        val upEvent2 = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            endX2.toFloat(),
            endY2.toFloat(),
            0
        )
        injectInputEventMethod.invoke(inputManager, upEvent2, 0)

        // 第一个手指抬起事件
        val upEvent1 = MotionEvent.obtain(
            downTime, eventTime, MotionEvent.ACTION_UP, endX1.toFloat(), endY1.toFloat(), 0
        )
        injectInputEventMethod.invoke(inputManager, upEvent1, 0)

        // 释放事件对象
        downEvent1.recycle()
        downEvent2.recycle()
        upEvent1.recycle()
        upEvent2.recycle()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun clickAtPoint(x: Int, y: Int) {
    val mInst = Instrumentation()

    // 创建一个PointerProperties对象，表示触摸事件的属性
    val pProp = PointerProperties().apply {
        id = 0
        toolType = MotionEvent.TOOL_TYPE_FINGER
    }

    // 创建一个PointerCoords对象，表示触摸事件的坐标
    val pCoord = PointerCoords().apply {
        pressure = 1f
        size = 1f
        this.x = x.toFloat()
        this.y = y.toFloat()
    }

    // 模拟按下事件
    var event = MotionEvent.obtain(
        SystemClock.uptimeMillis(),
        SystemClock.uptimeMillis(),
        MotionEvent.ACTION_DOWN,
        1,
        arrayOf(pProp),
        arrayOf(pCoord),
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0
    )
    mInst.sendPointerSync(event)
    sleep(1000)
    // 模拟抬起事件
    event = MotionEvent.obtain(
        SystemClock.uptimeMillis(),
        SystemClock.uptimeMillis(),
        MotionEvent.ACTION_UP,
        1,
        arrayOf(pProp),
        arrayOf(pCoord),
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0
    )
    mInst.sendPointerSync(event)
}

fun pinchOpen(distance: Int) {
    val mInst = Instrumentation()
    var distance = distance
    if (distance > 100) {
        distance = 100
    }

    if (distance < 0) {
        distance = 0
    }


    val center_X: Int = 720 / 2
    val center_Y: Int = 1280 / 2

    val point_x1 = 100 // 这里最好不要硬编码，小屏幕会出问题
    val point_x2 = 600 // 这里最好不要硬编码，小屏幕会出问题


    val pOneStart = PointerCoords()
    pOneStart.pressure = 1f
    pOneStart.x = ((point_x2 + point_x1) / 2).toFloat()
    pOneStart.y = center_Y.toFloat()
    pOneStart.size = 1f

    val pTwoStart = PointerCoords()
    pTwoStart.pressure = 1f
    pTwoStart.x = ((point_x2 + point_x1) / 2).toFloat() + 50
    pTwoStart.y = center_Y.toFloat() + 50
    pTwoStart.size = 1f

    val pThreeStart = PointerCoords()
    pThreeStart.pressure = 1f
    pThreeStart.x = ((point_x2 + point_x1) / 2).toFloat() - 50
    pThreeStart.y = center_Y.toFloat() + 50
    pThreeStart.size = 1f
    val pProp1 = PointerProperties()
    pProp1.id = 0
    pProp1.toolType = MotionEvent.TOOL_TYPE_FINGER

    val pProp2 = PointerProperties()
    pProp2.id = 1
    pProp2.toolType = MotionEvent.TOOL_TYPE_FINGER

    val pProp3 = PointerProperties()
    pProp3.id = 2
    pProp3.toolType = MotionEvent.TOOL_TYPE_FINGER

    val pCordStart = arrayOf(pOneStart, pTwoStart, pThreeStart)
    val pProp = arrayOf(pProp1, pProp2, pProp3)


    var event = MotionEvent.obtain(
        SystemClock.uptimeMillis(),
        SystemClock.uptimeMillis() + 25,
        MotionEvent.ACTION_DOWN,
        1,
        pProp,
        pCordStart,
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0
    )
    mInst.sendPointerSync(event)
    sleep(1000)
    event = MotionEvent.obtain(
        SystemClock.uptimeMillis(),
        SystemClock.uptimeMillis() + 25,
        MotionEvent.ACTION_POINTER_2_DOWN,
        2,
        pProp,
        pCordStart,
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0
    )
    mInst.sendPointerSync(event)
    sleep(1000)
    event = MotionEvent.obtain(
        SystemClock.uptimeMillis(),
        SystemClock.uptimeMillis() + 25,
        MotionEvent.ACTION_POINTER_3_DOWN,
        3,
        pProp,
        pCordStart,
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0
    )
    mInst.sendPointerSync(event)
    sleep(1000)
    // 一共一百步
    for (i in 0 until distance) {
        val pOneTemp = PointerCoords()
        pOneTemp.pressure = 1f
        pOneTemp.x = 50 + (point_x2 + point_x1) / 2 + (i * (point_x2.toFloat() - point_x1) / 200)

        pOneTemp.y = center_Y.toFloat() + i * 5
        pOneTemp.size = 1f
        val pTwoTemp = PointerCoords()
        pTwoTemp.pressure = 1f
        pTwoTemp.x = (point_x2 + point_x1) / 2 - (i * (point_x2.toFloat() - point_x1) / 200)

        pTwoTemp.y = center_Y.toFloat()
        pTwoTemp.size = 1f
        val pCordTemp = arrayOf(pOneTemp, pTwoTemp)


        event = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_MOVE,
            2,
            pProp,
            pCordTemp,
            0,
            0,
            1f,
            1f,
            0,
            0,
            0,
            0
        )
        mInst.sendPointerSync(event)
        sleep(25)
    }


    val pOneEnd = PointerCoords()
    pOneEnd.pressure = 1f
    pOneEnd.x = point_x2.toFloat()
    pOneEnd.y = center_Y.toFloat()
    pOneEnd.size = 1f

    val pTwoEnd = PointerCoords()
    pTwoEnd.pressure = 1f
    pTwoEnd.x = point_x1.toFloat()
    pTwoEnd.y = center_Y.toFloat()
    pTwoEnd.size = 1f
    val pCordEnd = arrayOf(pOneEnd, pTwoEnd)


    event = MotionEvent.obtain(
        SystemClock.uptimeMillis(),
        SystemClock.uptimeMillis() + 25,
        MotionEvent.ACTION_POINTER_2_UP,
        2,
        pProp,
        pCordEnd,
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0
    )
    mInst.sendPointerSync(event)


    event = MotionEvent.obtain(
        SystemClock.uptimeMillis(),
        SystemClock.uptimeMillis() + 25,
        MotionEvent.ACTION_UP,
        1,
        pProp,
        pCordEnd,
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0
    )
    mInst.sendPointerSync(event)
}


private fun startClashOfClans(context: Context) {
    // 使用 libsu 启动 Clash of Clans
    Shell.su("am start -n com.supercell.clashofclans/com.supercell.titan.GameApp").exec()
        .let { result ->
            if (result.isSuccess) {
                showToast(context, "Clash of Clans started successfully")
            } else {
                showToast(context, "Failed to start Clash of Clans: ${result.out}")
            }
        }
}

private fun showToast(context: Context, message: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
fun ClashOfClansButtonPreview() {
    ZkqTheme {
        ClashOfClansButton()
    }
}
