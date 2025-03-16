package com.genymobile.scrcpy;

import android.graphics.Point;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.ddm.lrplugin.Touch;
import com.genymobile.scrcpy.wrappers.InputManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

public class EventController {

    static String TAG = "EventController";
    private final Device device;

    private final KeyCharacterMap charMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);

    private long lastMouseDown;
    private Vector<MotionEvent.PointerProperties> pointerProperties = new Vector<MotionEvent.PointerProperties>();
    private Vector<MotionEvent.PointerCoords> pointerCoords = new Vector<MotionEvent.PointerCoords>();
    public Map 手指按下时间 = new HashMap<Long, Long> ();
    /**
     * 退出标记
     */
    private volatile boolean eventExitFlag = false;

    public EventController(Device device) {
        this.device = device;
    }

    private int getPointer(int id) {
        for (int i = 0; i < pointerProperties.size(); i++) {
            if (id == pointerProperties.get(i).id) {
                return i;
            }
        }

        MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
        props.id = id;
        props.toolType = MotionEvent.TOOL_TYPE_FINGER;
        pointerProperties.addElement(props);

        MotionEvent.PointerCoords  coords = new MotionEvent.PointerCoords();
        coords.orientation = 0;

        double pressure = 0.0;
        double size = 0.0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            pressure = ThreadLocalRandom.current().nextDouble(1.2, 3.5);
            size = ThreadLocalRandom.current().nextInt(135, 250);
        }else{
            Random random = new Random();
            double min = 1.2;
            double max = 3.5;
            pressure = min + (max - min) * random.nextDouble();
            size = 135 + (250 - 135) * random.nextDouble();
        }
        coords.pressure = (float)pressure;
        coords.size = (float)size;
        pointerCoords.addElement(coords);
        return pointerProperties.size() - 1;
    }

    private void releasePointer(int id) {
        int index = -1;
        for (int i = 0; i < pointerProperties.size(); i++) {
            if (id == pointerProperties.get(i).id) {
                index = i;
                break;
            }
        }

        if ( -1 != index) {
            pointerProperties.remove(index);
            pointerCoords.remove(index);
        }
    }
    private double 随机小鼠(){
        Random random = new Random();

        // 随机生成 0 到 6 之间的位数
        int decimalPlaces = random.nextInt(7);

        // 生成 [0, 1) 范围内的随机 double 值
        double randomValue = random.nextDouble();

        // 调整小数位数
        double factor = Math.pow(10, decimalPlaces);
        double result = Math.round(randomValue * factor) / factor;
        return result;
    }
    private void setPointerCoords(int id, Point point) {
        int index = -1;
        for (int i = 0; i < pointerProperties.size(); i++) {
            if (id == pointerProperties.get(i).id) {
                index = i;
                break;
            }
        }

        if ( -1 != index) {
            MotionEvent.PointerCoords coords = pointerCoords.get(index);
            coords.x = point.x+(float)随机小鼠();
            coords.y = point.y+(float)随机小鼠();
        }
    }

    private void setScroll(int id, int hScroll, int vScroll) {
        int index = -1;
        for (int i = 0; i < pointerProperties.size(); i++) {
            if (id == pointerProperties.get(i).id) {
                index = i;
                break;
            }
        }

        if ( -1 != index) {
            MotionEvent.PointerCoords coords = pointerCoords.get(index);
            coords.setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll);
            coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);
        }
    }

    /*
    public void control() {
        // on start, turn screen on
        turnScreenOn();
        Log.d(TAG,"启动事件处理...");
        while (!eventExitFlag) {
            try{
                handleEvent();
            } catch (Exception ex){

            }
        }
        Log.d(TAG,"事件处理__已退出!");
    }
    */

    public  boolean handleEvent(ControlEvent controlEvent){
        boolean 执行结果 =false;
        switch (controlEvent.getType()) {
            case ControlEvent.TYPE_KEYCODE:
                //Log.d("cosmop",controlEvent.getAction()+" "+controlEvent.getKeycode()+ " "+controlEvent.getMetaState());
                执行结果 = injectKeycode(controlEvent.getAction(), controlEvent.getKeycode(), controlEvent.getMetaState());
                //Log.d("cosmop","执行结果injectKeycode:"+exeResult);
                break;
            case ControlEvent.TYPE_TEXT:
                执行结果 = injectText(controlEvent.getText());
                break;
            case ControlEvent.TYPE_MOUSE:
                执行结果 = injectMouse(controlEvent.getAction(), controlEvent.getButtons(), controlEvent.getPosition());
                break;
            case ControlEvent.TYPE_TOUCH:
                执行结果 = injectTouch(controlEvent.getId(), controlEvent.getAction(), controlEvent.getPosition());
                break;
            case ControlEvent.TYPE_SCROLL:
                执行结果 = injectScroll(controlEvent.getPosition(), controlEvent.getHScroll(), controlEvent.getVScroll());
                break;
            case ControlEvent.TYPE_COMMAND:
                执行结果 = executeCommand(controlEvent.getAction());
                break;
            default:
                // do nothing
        }
        return 执行结果;
    }

    /*
    private void handleEvent() throws IOException {
        ControlEvent controlEvent = mTcpConnection.receiveControlEvent();
        switch (controlEvent.getType()) {
            case ControlEvent.TYPE_KEYCODE:
                injectKeycode(controlEvent.getAction(), controlEvent.getKeycode(), controlEvent.getMetaState());
                break;
            case ControlEvent.TYPE_TEXT:
                injectText(controlEvent.getText());
                break;
            case ControlEvent.TYPE_MOUSE:
                injectMouse(controlEvent.getAction(), controlEvent.getButtons(), controlEvent.getPosition());
                break;
            case ControlEvent.TYPE_TOUCH:
                injectTouch(controlEvent.getId(), controlEvent.getAction(), controlEvent.getPosition());
                break;
            case ControlEvent.TYPE_SCROLL:
                injectScroll(controlEvent.getPosition(), controlEvent.getHScroll(), controlEvent.getVScroll());
                break;
            case ControlEvent.TYPE_COMMAND:
                executeCommand(controlEvent.getAction());
                break;
            default:
                // do nothing
        }
    }
    */

    private boolean injectKeycode(int action, int keycode, int metaState) {
        return injectKeyEvent(action, keycode, 0, metaState);
    }

    private boolean injectChar(char c) {
        String decomposed = KeyComposition.decompose(c);
        char[] chars = decomposed != null ? decomposed.toCharArray() : new char[] {c};
        KeyEvent[] events = charMap.getEvents(chars);
        if (events == null) {
            return false;
        }
        for (KeyEvent event : events) {
            if (!injectEvent(event)) {
                return false;
            }
        }
        return true;
    }

    private boolean injectText(String text) {
        for (char c : text.toCharArray()) {
            if (!injectChar(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean injectTouch(int id, int action, Position position) {
        if (action != MotionEvent.ACTION_DOWN
                && action != MotionEvent.ACTION_UP
                && action != MotionEvent.ACTION_MOVE) {
            Ln.w("Unsupported action: " + action);
            return false;
        }
        if (id < 0 || id > 9) {
            Ln.w("Unsupported id[0-9]: " + id);
            return false;
        }
        long 按下时间 = SystemClock.uptimeMillis();
        long 事件时间 = SystemClock.uptimeMillis();

        int index = getPointer(id);
        int convertAction = action;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (1 != pointerProperties.size()) {
                    convertAction = (index << 8) | MotionEvent.ACTION_POINTER_DOWN;
                }
                手指按下时间.put(id,按下时间);
                break;
            case MotionEvent.ACTION_MOVE:
                if (1 != pointerProperties.size()) {
                    convertAction = (index << 8) | convertAction;
                }
                按下时间 = (long) 手指按下时间.get(id);
                break;
            case MotionEvent.ACTION_UP:
                if (1 != pointerProperties.size()) {
                    convertAction = (index << 8) | MotionEvent.ACTION_POINTER_UP;
                }
                按下时间 = (long) 手指按下时间.get(id);
                break;
        }

        Point point = device.getPhysicalPoint(position);
        if (point == null) {
            // ignore event
            return false;
        }

        if (pointerProperties.isEmpty()) {
            // ignore event
            return false;
        }
        setPointerCoords(id, point);
        MotionEvent.PointerProperties[] props = pointerProperties.toArray(new MotionEvent.PointerProperties[pointerProperties.size()]);
        MotionEvent.PointerCoords[] coords = pointerCoords.toArray(new MotionEvent.PointerCoords[pointerCoords.size()]);
        MotionEvent event = MotionEvent.obtain(按下时间,事件时间, convertAction,
                pointerProperties.size(), props, coords, 0, 0, 1f, 1f,  Touch.触摸设备ID, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);

        //Log.d("点击事件","触摸设备ID:"+Touch.触摸设备ID+event.toString());
        if (action == MotionEvent.ACTION_UP) {
            releasePointer(id);
        }

        boolean 执行结果 = injectEvent(event);
        event.recycle();
        return 执行结果;
    }
    public synchronized boolean injectTouchV2(int id, int action, int x, int y) {
        if (action != MotionEvent.ACTION_DOWN
                && action != MotionEvent.ACTION_UP
                && action != MotionEvent.ACTION_MOVE) {
            Ln.w("Unsupported action: " + action);
            return false;
        }
        if (id < 0 || id > 9) {
            Ln.w("Unsupported id[0-9]: " + id);
            return false;
        }

        int index = getPointer(id);
        int convertAction = action;
        long 按下时间 = SystemClock.uptimeMillis();
        long 事件时间 = SystemClock.uptimeMillis();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (1 != pointerProperties.size()) {
                    convertAction = (index << 8) | MotionEvent.ACTION_POINTER_DOWN;
                }
                手指按下时间.put(id,SystemClock.uptimeMillis());
                break;
            case MotionEvent.ACTION_MOVE:
                if (1 != pointerProperties.size()) {
                    convertAction = (index << 8) | convertAction;
                }
                按下时间 = (long) 手指按下时间.get(id);
                break;
            case MotionEvent.ACTION_UP:
                if (1 != pointerProperties.size()) {
                    convertAction = (index << 8) | MotionEvent.ACTION_POINTER_UP;
                }
                按下时间 = (long) 手指按下时间.get(id);
                break;
        }

        //Point point = device.getPhysicalPoint(position);
        Point point = new Point(x,y);
        if (point == null) {
            // ignore event
            return false;
        }


        if (pointerProperties.isEmpty()) {
            // ignore event
            return false;
        }
        setPointerCoords(id, point);
        MotionEvent.PointerProperties[] props = pointerProperties.toArray(new MotionEvent.PointerProperties[pointerProperties.size()]);
        MotionEvent.PointerCoords[] coords = pointerCoords.toArray(new MotionEvent.PointerCoords[pointerCoords.size()]);
        MotionEvent event = MotionEvent.obtain(按下时间,事件时间, convertAction,
                pointerProperties.size(), props, coords, 0, 0, 1f, 1f,  Touch.触摸设备ID, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);

        //Log.d("点击事件","触摸设备ID:"+Touch.触摸设备ID+event.toString());
        if (action == MotionEvent.ACTION_UP) {
            releasePointer(id);
        }

        boolean 执行结果 = injectEvent(event);
        event.recycle();
        return 执行结果;
    }
    private boolean injectMouse(int action, int buttons, Position position) {
        long now = SystemClock.uptimeMillis();
        if (action == MotionEvent.ACTION_DOWN) {
            getPointer(0);
            lastMouseDown = now;
        }
        Point point = device.getPhysicalPoint(position);
        if (point == null) {
            // ignore event
            return false;
        }

        if (pointerProperties.isEmpty()) {
            // ignore event
            return false;
        }
        setPointerCoords(0, point);
        MotionEvent.PointerProperties[] props = pointerProperties.toArray(new MotionEvent.PointerProperties[pointerProperties.size()]);
        MotionEvent.PointerCoords[] coords = pointerCoords.toArray(new MotionEvent.PointerCoords[pointerCoords.size()]);
        MotionEvent event = MotionEvent.obtain(lastMouseDown, now, action,
                pointerProperties.size(), props, coords, 0, buttons, 1f, 1f, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);

        if (action == MotionEvent.ACTION_UP) {
            releasePointer(0);
        }
        return injectEvent(event);
    }

    private boolean injectScroll(Position position, int hScroll, int vScroll) {
        long now = SystemClock.uptimeMillis();
        Point point = device.getPhysicalPoint(position);
        if (point == null) {
            // ignore event
            return false;
        }

        // init
        MotionEvent.PointerProperties[] props = {new MotionEvent.PointerProperties()};
        props[0].id = 0;
        props[0].toolType = MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords[] coords = {new MotionEvent.PointerCoords()};
        coords[0].orientation = 0;
        coords[0].pressure = 1;
        coords[0].size = 1;

        // set data
        coords[0].x = point.x;
        coords[0].y = point.y;
        coords[0].setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll);
        coords[0].setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);

        MotionEvent event = MotionEvent.obtain(lastMouseDown, now, MotionEvent.ACTION_SCROLL, 1, props, coords, 0, 0, 1f, 1f, 2,
                0, InputDevice.SOURCE_MOUSE, 0);
        return injectEvent(event);
    }

    private boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        return injectEvent(event);
    }

    private boolean injectKeycode(int keyCode) {
        return injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode, 0, 0)
                && injectKeyEvent(KeyEvent.ACTION_UP, keyCode, 0, 0);
    }

    public boolean injectEvent(InputEvent event) {
        return device.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC); //同步
       // InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH
        //InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH); //同步
        //InputManager.INJECT_INPUT_EVENT_MODE_ASYNC --异步模式
    }

    private boolean turnScreenOn() {
        return device.isScreenOn() || injectKeycode(KeyEvent.KEYCODE_POWER);
    }

    private boolean pressBackOrTurnScreenOn() {
        int keycode = device.isScreenOn() ? KeyEvent.KEYCODE_BACK : KeyEvent.KEYCODE_POWER;
        return injectKeycode(keycode);
    }

    private boolean executeCommand(int action) {
        switch (action) {
            case ControlEvent.COMMAND_BACK_OR_SCREEN_ON:
                return pressBackOrTurnScreenOn();
            default:
                Ln.w("Unsupported command: " + action);
        }
        return false;
    }

    public void stopEventController(){
        Log.d(TAG,"停止事件处理");
        this.eventExitFlag = true;
    }
}
