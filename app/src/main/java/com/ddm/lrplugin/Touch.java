package com.ddm.lrplugin;

import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.genymobile.scrcpy.ControlEvent;
import com.genymobile.scrcpy.Device;
import com.genymobile.scrcpy.DisplayInfo;
import com.genymobile.scrcpy.EventController;
import com.genymobile.scrcpy.Ln;
import com.genymobile.scrcpy.Options;
import com.genymobile.scrcpy.Position;
import com.genymobile.scrcpy.wrappers.ServiceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;

public class Touch {
    static EventController mEventController;
    static ServiceManager serviceManager = new ServiceManager();
    static DisplayInfo displayInfo = serviceManager.getDisplayManager().getDisplayInfo();
    static ByteBuffer controlerBuffer = ByteBuffer.allocate(1024);
    public static int 触摸设备ID = 0;
    private static boolean handlerControler(int startPos, int length){

        controlerBuffer.position(0);
        int type = controlerBuffer.get();
        ControlEvent controlEvent = null;
        switch (type) {
            case ControlEvent.TYPE_KEYCODE:
                controlEvent = parseKeycodeControlEvent();
                break;
            case ControlEvent.TYPE_TEXT:
//                controlEvent = parseTextControlEvent();
                break;
            case ControlEvent.TYPE_MOUSE:
//                controlEvent = parseMouseControlEvent();
                break;
            case ControlEvent.TYPE_TOUCH:
                controlEvent = parseMouseTouchEvent();
                break;
            case ControlEvent.TYPE_SCROLL:
                controlEvent = parseScrollControlEvent();
                break;
            case ControlEvent.TYPE_COMMAND:
//                controlEvent = parseCommandControlEvent();
                break;
            default:
                // todo 报错
                Ln.w("Unknown event type: " + type);
                controlEvent = null;
                break;
        }

        if (controlEvent == null) {
            // failure, reset savedPosition
            controlerBuffer.position(0);
        }
        return mEventController.handleEvent(controlEvent);
    }
    public static Device getDevice(int size, int bitRate, int frameRate){
        Options options = new Options();
        options.setBitRate(bitRate);
        options.setMaxSize(size);
        options.setFrameRate(frameRate);
        return new Device(options);
    }
    public static ControlEvent parseKeycodeControlEvent(){
        int action = toUnsigned(controlerBuffer.get());
        int keycode = controlerBuffer.getInt();
        int metaState = controlerBuffer.getInt();
        return ControlEvent.createKeycodeControlEvent(action, keycode, metaState);
    }
    private static ControlEvent parseScrollControlEvent() {
        Position position = readPosition(controlerBuffer);
        int hScroll = controlerBuffer.getInt();
        int vScroll = controlerBuffer.getInt();
        return ControlEvent.createScrollControlEvent(position, hScroll, vScroll);
    }

    public  static ControlEvent parseMouseTouchEvent(){
        int id = toUnsigned(controlerBuffer.get());
        int action = toUnsigned(controlerBuffer.get());
        Position position = readPosition(controlerBuffer);
        return ControlEvent.createMotionTouchEvent(id, action, position);
    }

    public static Position readPosition(ByteBuffer buffer) {
        int x = toUnsigned(buffer.getShort());
        int y = toUnsigned(buffer.getShort());
        int screenWidth = toUnsigned(buffer.getShort());
        int screenHeight = toUnsigned(buffer.getShort());
        return new Position(x, y, screenWidth, screenHeight);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    public static int toUnsigned(byte value) {
        return value; //& 0xff;
    }
    @SuppressWarnings("checkstyle:MagicNumber")
    public static int toUnsigned(short value) {
        return value; //& 0xffff;
    }

    public static void logExceptionToFile( Throwable throwable) {
        // 获取异常信息
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        String exceptionAsString = stringWriter.toString();

        // 定义文件名和路径
        String fileName = "错误记录.txt";
        File file = new File("/sdcard/", fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 写入文件
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(exceptionAsString.getBytes());
            fos.write("\n".getBytes());
            Log.d(Util.TAG, "Exception written to file: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.d(Util.TAG, "Failed to write exception to file", e);
        }
    }
    public synchronized static boolean HttpTouch( JSONObject jsonObject ){
        //Log.d("HttpTouch",jsonObject.toString());
        boolean 执行结果 = false;
        try {
            String op = jsonObject.getString("op");
            int id = 0;
            int x = 0;
            int y = 0;
            try {
                id = jsonObject.getInt("id");
                x = -1;
                y = -1;
                x = jsonObject.getInt("x");
                y = jsonObject.getInt("y");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            switch (op){
                case "close":
                    System.exit(0);
                    break;
                case "tap":

                    break;
                case "tap1":

                    break;
                case "swipe":

                    break;
                case "touchDown":

                    执行结果=Touch.mEventController.injectTouchV2(id,MotionEvent.ACTION_DOWN,x,y);
                    break;
                case "touchMove":
                    执行结果=Touch.mEventController.injectTouchV2(id,MotionEvent.ACTION_MOVE,x,y);
                    break;
                case "touchUp":
                    执行结果=Touch.mEventController.injectTouchV2(id,MotionEvent.ACTION_UP,x,y);
                    break;
                case "scroll":
                    break;
                case "keyEvent":

                    break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            logExceptionToFile(e);
        }
        return 执行结果;
    }
    public synchronized static boolean tcpTouch( JSONObject jsonObject ){
        int displayW = displayInfo.getSize().getWidth();
        int displayH = displayInfo.getSize().getHeight();
        //Log.d("懒人插件",cmd);
        if (displayW>displayH){
            int t = displayH;
            displayH = displayW;
            displayW = t;
        }
        boolean 执行结果 = false;
        try {

            String op = jsonObject.getString("op");
            int id = 0;
            int x = 0;
            int y = 0;
            try {
                id = jsonObject.getInt("id");
                x = -1;
                y = -1;
                x = jsonObject.getInt("x");
                y = jsonObject.getInt("y");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            switch (op){
                case "close":
                    System.exit(0);
                    break;
                case "tap":
                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 5);
                    controlerBuffer.put((byte) id);
                    controlerBuffer.put((byte) 0);
                    controlerBuffer.put((byte) (x >> 8));
                    controlerBuffer.put((byte) x);
                    controlerBuffer.put((byte) (y >> 8));
                    controlerBuffer.put((byte) y);
                    controlerBuffer.put((byte) (displayW >> 8));
                    controlerBuffer.put((byte) displayW);
                    controlerBuffer.put((byte) (displayH >> 8));
                    controlerBuffer.put((byte) displayH);
                    执行结果 = handlerControler(0, 11);
                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 5);
                    controlerBuffer.put((byte) id);
                    controlerBuffer.put((byte) 1);
                    controlerBuffer.put((byte) (x >> 8));
                    controlerBuffer.put((byte) x);
                    controlerBuffer.put((byte) (y >> 8));
                    controlerBuffer.put((byte) y);
                    controlerBuffer.put((byte) (displayW >> 8));
                    controlerBuffer.put((byte) displayW);
                    controlerBuffer.put((byte) (displayH >> 8));
                    controlerBuffer.put((byte) displayH);
                    执行结果 = handlerControler(0, 11);
                    break;
                case "tap1":
                    int sleepTime = jsonObject.getInt("sleepTime");
                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 5);
                    controlerBuffer.put((byte) id);
                    controlerBuffer.put((byte) 0);
                    controlerBuffer.put((byte) (x >> 8));
                    controlerBuffer.put((byte) x);
                    controlerBuffer.put((byte) (y >> 8));
                    controlerBuffer.put((byte) y);
                    controlerBuffer.put((byte) (displayW >> 8));
                    controlerBuffer.put((byte) displayW);
                    controlerBuffer.put((byte) (displayH >> 8));
                    controlerBuffer.put((byte) displayH);
                    执行结果 = handlerControler(0, 11);
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 5);
                    controlerBuffer.put((byte) id);
                    controlerBuffer.put((byte) 1);
                    controlerBuffer.put((byte) (x >> 8));
                    controlerBuffer.put((byte) x);
                    controlerBuffer.put((byte) (y >> 8));
                    controlerBuffer.put((byte) y);
                    controlerBuffer.put((byte) (displayW >> 8));
                    controlerBuffer.put((byte) displayW);
                    controlerBuffer.put((byte) (displayH >> 8));
                    controlerBuffer.put((byte) displayH);
                    执行结果 = handlerControler(0, 11);
                    break;
                case "swipe":
                    int x2 = jsonObject.getInt("x2");
                    int y2 = jsonObject.getInt("y2");
                    int t = jsonObject.getInt("t");
                    int m = jsonObject.getInt("m");
                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 5);
                    controlerBuffer.put((byte) id);
                    controlerBuffer.put((byte) 0);
                    controlerBuffer.put((byte) (x >> 8));
                    controlerBuffer.put((byte) x);
                    controlerBuffer.put((byte) (y >> 8));
                    controlerBuffer.put((byte) y);
                    controlerBuffer.put((byte) (displayW >> 8));
                    controlerBuffer.put((byte) displayW);
                    controlerBuffer.put((byte) (displayH >> 8));
                    controlerBuffer.put((byte) displayH);
                    执行结果 = handlerControler(0, 11);

                    for (int i = 0; i <= m; i++) {
                        int xx = x + (x2 - x) * i/m;
                        int yy = y + (y2 - y) * i/m;

                        controlerBuffer.clear();
                        controlerBuffer.put((byte) 5);
                        controlerBuffer.put((byte) id);
                        controlerBuffer.put((byte) 2);
                        controlerBuffer.put((byte) (xx >> 8));
                        controlerBuffer.put((byte) xx);
                        controlerBuffer.put((byte) (yy >> 8));
                        controlerBuffer.put((byte) yy);
                        controlerBuffer.put((byte) (displayW >> 8));
                        controlerBuffer.put((byte) displayW);
                        controlerBuffer.put((byte) (displayH >> 8));
                        controlerBuffer.put((byte) displayH);
                        执行结果 = handlerControler(0, 11);
                        //Log.d("懒人插件","移动:"+xx+","+yy);

                    }
                    //Log.d("懒人插件","滑动");
//                    controlerBuffer.clear();
//                    controlerBuffer.put((byte) 5);
//                    controlerBuffer.put((byte) id);
//                    controlerBuffer.put((byte) 2);
//                    controlerBuffer.put((byte) (x2 >> 8));
//                    controlerBuffer.put((byte) x2);
//                    controlerBuffer.put((byte) (y2 >> 8));
//                    controlerBuffer.put((byte) y2);
//                    controlerBuffer.put((byte) (displayW >> 8));
//                    controlerBuffer.put((byte) displayW);
//                    controlerBuffer.put((byte) (displayH >> 8));
//                    controlerBuffer.put((byte) displayH);
//                    handlerControler(0, 11);

                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 5);
                    controlerBuffer.put((byte) id);
                    controlerBuffer.put((byte) 1);
                    controlerBuffer.put((byte) (x2 >> 8));
                    controlerBuffer.put((byte) x2);
                    controlerBuffer.put((byte) (y2 >> 8));
                    controlerBuffer.put((byte) y2);
                    controlerBuffer.put((byte) (displayW >> 8));
                    controlerBuffer.put((byte) displayW);
                    controlerBuffer.put((byte) (displayH >> 8));
                    controlerBuffer.put((byte) displayH);
                    执行结果 = handlerControler(0, 11);
                    break;
                case "touchDown":
                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 5);
                    controlerBuffer.put((byte) id);
                    controlerBuffer.put((byte) 0);
                    controlerBuffer.put((byte) (x >> 8));
                    controlerBuffer.put((byte) x);
                    controlerBuffer.put((byte) (y >> 8));
                    controlerBuffer.put((byte) y);
                    controlerBuffer.put((byte) (displayW >> 8));
                    controlerBuffer.put((byte) displayW);
                    controlerBuffer.put((byte) (displayH >> 8));
                    controlerBuffer.put((byte) displayH);
                    执行结果 =  handlerControler(0, 11);
                    break;
                case "touchMove":
                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 5);
                    controlerBuffer.put((byte) id);
                    controlerBuffer.put((byte) 2);
                    controlerBuffer.put((byte) (x >> 8));
                    controlerBuffer.put((byte) x);
                    controlerBuffer.put((byte) (y >> 8));
                    controlerBuffer.put((byte) y);
                    controlerBuffer.put((byte) (displayW >> 8));
                    controlerBuffer.put((byte) displayW);
                    controlerBuffer.put((byte) (displayH >> 8));
                    controlerBuffer.put((byte) displayH);
                    执行结果 =  handlerControler(0, 11);
                    break;
                case "touchUp":

                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 5);
                    controlerBuffer.put((byte) id);
                    controlerBuffer.put((byte) 1);
                    controlerBuffer.put((byte) (x >> 8));
                    controlerBuffer.put((byte) x);
                    controlerBuffer.put((byte) (y >> 8));
                    controlerBuffer.put((byte) y);
                    controlerBuffer.put((byte) (displayW >> 8));
                    controlerBuffer.put((byte) displayW);
                    controlerBuffer.put((byte) (displayH >> 8));
                    controlerBuffer.put((byte) displayH);
                    执行结果 =   handlerControler(0, 11);
                    break;
                case "scroll":

                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 3);
                    controlerBuffer.put((byte) (x >> 8));
                    controlerBuffer.put((byte) x);
                    controlerBuffer.put((byte) (y >> 8));
                    controlerBuffer.put((byte) y);
                    controlerBuffer.put((byte) (displayW >> 8));
                    controlerBuffer.put((byte) displayW);
                    controlerBuffer.put((byte) (displayH >> 8));
                    controlerBuffer.put((byte) displayH);
                    int hScroll  = jsonObject.getInt("h");
                    int vScroll  = jsonObject.getInt("v");
                    controlerBuffer.putInt(hScroll);
                    controlerBuffer.putInt(vScroll);
                    执行结果 = handlerControler(0, 17);
                    break;
                case "keyEvent":
                    //Log.d("懒人插件", "按键: ");
                    int keyCode = jsonObject.getInt("keyCode");
                    int meta = jsonObject.getInt("meta");
                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 0);
                    controlerBuffer.put( (byte) KeyEvent.ACTION_DOWN);
                    controlerBuffer.putInt(keyCode);
                    controlerBuffer.putInt(meta);
                    执行结果 =  handlerControler(0, 6);
                    controlerBuffer.clear();
                    controlerBuffer.put((byte) 0);
                    controlerBuffer.put( (byte) KeyEvent.ACTION_UP);
                    controlerBuffer.putInt( keyCode);
                    controlerBuffer.putInt(meta);
                    执行结果 =  handlerControler(0, 6);
                    controlerBuffer.clear();
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();

        }
        return 执行结果;
    }

}
