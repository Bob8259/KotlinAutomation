package com.example.touch

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class GestureTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "GestureTestActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_gesture_layout)
        
        // 初始化按钮点击事件
        initButtonListeners()
    }
    
    private fun initButtonListeners() {
        // 测试长按并拖动
        findViewById<Button>(R.id.btn_test_long_press_drag).setOnClickListener {
            Toast.makeText(this, "请切换到目标应用，准备执行长按并拖动测试", Toast.LENGTH_LONG).show()
            
            // 延迟执行，给用户时间切换应用
            it.postDelayed({
                TouchGestureTest.testLongPressAndDrag(this)
            }, 3000) // 3秒后执行
        }
        
        // 测试双指捏合
        findViewById<Button>(R.id.btn_test_pinch_in).setOnClickListener {
            Toast.makeText(this, "请切换到目标应用，准备执行双指捏合测试", Toast.LENGTH_LONG).show()
            
            // 延迟执行，给用户时间切换应用
            it.postDelayed({
                TouchGestureTest.testPinchIn(this)
            }, 3000) // 3秒后执行
        }
        
        // 测试双指张开
        findViewById<Button>(R.id.btn_test_pinch_out).setOnClickListener {
            Toast.makeText(this, "请切换到目标应用，准备执行双指张开测试", Toast.LENGTH_LONG).show()
            
            // 延迟执行，给用户时间切换应用
            it.postDelayed({
                TouchGestureTest.testPinchOut(this)
            }, 3000) // 3秒后执行
        }
        
        // 测试连续点击
        findViewById<Button>(R.id.btn_test_taps).setOnClickListener {
            Toast.makeText(this, "请切换到目标应用，准备执行连续点击测试", Toast.LENGTH_LONG).show()
            
            // 延迟执行，给用户时间切换应用
            it.postDelayed({
                TouchGestureTest.testTaps(this)
            }, 3000) // 3秒后执行
        }
        
        // 使用自定义参数测试长按并拖动
        findViewById<Button>(R.id.btn_test_custom).setOnClickListener {
            try {
                // 获取自定义参数
                val startX = findViewById<EditText>(R.id.et_start_x).text.toString().toFloat()
                val startY = findViewById<EditText>(R.id.et_start_y).text.toString().toFloat()
                val endX = findViewById<EditText>(R.id.et_end_x).text.toString().toFloat()
                val endY = findViewById<EditText>(R.id.et_end_y).text.toString().toFloat()
                val longPressTime = findViewById<EditText>(R.id.et_long_press_time).text.toString().toLong()
                val moveTime = findViewById<EditText>(R.id.et_move_time).text.toString().toLong()
                
                Toast.makeText(this, "请切换到目标应用，准备执行自定义长按并拖动测试", Toast.LENGTH_LONG).show()
                
                // 延迟执行，给用户时间切换应用
                it.postDelayed({
                    TouchGestureTest.testLongPressAndDrag(
                        context = this,
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        longPressTime = longPressTime,
                        moveTime = moveTime
                    )
                }, 3000) // 3秒后执行
            } catch (e: Exception) {
                Log.e(TAG, "解析自定义参数时出错: ${e.message}")
                Toast.makeText(this, "参数错误，请检查输入", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // 检查辅助功能服务是否已连接
        if (TouchGestureServiceHolder.instance == null) {
            Toast.makeText(this, "辅助功能服务未连接，请先开启辅助功能", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "辅助功能服务已连接，可以开始测试", Toast.LENGTH_SHORT).show()
        }
    }
} 