package com.ddm.lrplugin;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.LocalSocket;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.widget.Toast;
import com.genymobile.scrcpy.Device;
import com.genymobile.scrcpy.EventController;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Util {
    public static String TAG = "LrPlugin";

    public static byte[] checksum(String paramString1, int paramInt, String paramString2){
        StringBuffer localObject = new StringBuffer();
        if (paramString1 !=null){

            localObject.append(paramString1);
        }
        localObject.append(paramInt);
        localObject.append(paramString2);
        localObject.append("mMcShCsTr");
        byte[] tmp1 = localObject.toString().substring(1, 9).getBytes();
        char[] tmp2 = new char[16];
        tmp2[0] = ((char)48);
        tmp2[1] = ((char)49);
        tmp2[2] = ((char)50);
        tmp2[3] = ((char)51);
        tmp2[4] = ((char)52);
        tmp2[5] = ((char)53);
        tmp2[6] = ((char)54);
        tmp2[7] = ((char)55);
        tmp2[8] = ((char)56);
        tmp2[9] = ((char)57);
        tmp2[10] = ((char)97);
        tmp2[11] = ((char)98);
        tmp2[12] = ((char)99);
        tmp2[13] = ((char)100);
        tmp2[14] = ((char)101);
        tmp2[15] = ((char)102);
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(tmp1);
            byte[] ret = md5.digest();
            int i = ret.length;
            char[] tmp3 = new char[i * 2];
            paramInt = 0;
            int m;
            for (int j = 0; paramInt < i; j = m + 1)
            {
                int k = ret[paramInt];
                m = j + 1;
                tmp3[j] = ((char)tmp2[(k >>> 4 & 0xF)]);
                tmp3[m] = ((char)tmp2[(k & 0xF)]);
                paramInt++;
            }
            String r = new String(tmp3);
            return r.getBytes();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] a ={0};
        return a;
    }
    public static void SendCode(Context ctx,String appPackage,String state,String WXAPPID,String Code,int v){
        Object application = null;
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");

            // Call the currentActivityThread() method to get a reference to the current ActivityThread instance
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);

            // Call the getApplication() method to get a reference to the Application instance
            Method getApplicationMethod = activityThreadClass.getDeclaredMethod("getApplication");
            application = getApplicationMethod.invoke(activityThread);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        // Cast the Application instance to a Context instance
        Context context = (Context) application;
        Log.d("SendCode",appPackage);
        Log.d("SendCode",state);
        Log.d("SendCode",WXAPPID);
        Log.d("SendCode",Code);
        Log.d("SendCode",v+"");
        Intent intent = new Intent();
        String a = null;
        intent.setClassName(appPackage,appPackage+".wxapi.WXEntryActivity");
        intent.putExtra("_message_token",a);
        String _wxapi_sendauth_req_state =state;
        intent.putExtra("_wxapi_sendauth_resp_state",_wxapi_sendauth_req_state);
        intent.putExtra("_wxapi_sendauth_resp_token",Code);
        intent.putExtra("_mmessage_appPackage","com.tencent.mm");
        intent.putExtra("_wxapi_baseresp_transaction",a);
        intent.putExtra("_wxapi_sendauth_resp_lang","zh_CN");
        intent.putExtra("_wxapi_command_type",1);
        intent.putExtra("_mmessage_content",a);
        intent.putExtra("_wxapi_sendauth_resp_country","CN");
        intent.putExtra("wx_token_key","com.tencent.mm.openapi.token");
        intent.putExtra("_wxapi_sendauth_resp_url",WXAPPID+"://oauth?code="+Code+"&state="+_wxapi_sendauth_req_state);
        intent.putExtra("_mmessage_sdkVersion",v);
        intent.putExtra("_wxapi_baseresp_errcode",0);
        intent.putExtra("_wxapi_baseresp_errstr",a);
        intent.putExtra("_wxapi_baseresp_openId",a);
        byte[] b = checksum(a,v,"com.tencent.mm");
        intent.putExtra("_mmessage_checksum",b);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send(); // 触发Activity的启动
        } catch (Throwable e) {
            e.printStackTrace();
            StringWriter stringWriter = new StringWriter();
            String 崩溃堆栈 = "";
            try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
                e.printStackTrace(printWriter); // 将堆栈信息写入 StringWriter
                崩溃堆栈 = stringWriter.toString();
            }catch (Exception ex){
                ex.printStackTrace();
            }

            String 崩溃信息 = e.getMessage();
            Log.d("SendCode", "SendCode: "+崩溃信息+"\r\n"+崩溃堆栈);
        }
//        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND).addFlags(Intent.FLAG_RECEIVER_NO_ABORT).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent);
        Log.d(TAG, "SendCode: 结束");
    }
    public static void ShareText(Context ctx,String 分享的文本){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, 分享的文本);
        sendIntent.setType("text/plain");
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, Intent.createChooser(sendIntent, "Image"), PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send(); // 触发Activity的启动
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public static void ShareImg(Context ctx,String 分享的图片路径){
        byte[] decodedBytes = Base64.decode(分享的图片路径, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

        // 将 Bitmap 转为 URI
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String path = android.provider.MediaStore.Images.Media.insertImage(
                ctx.getContentResolver(), bitmap, "Image", null);
        Uri imageUri = Uri.parse(path);
        // 创建分享意图
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, Intent.createChooser(shareIntent, "Image"), PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send(); // 触发Activity的启动
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public static void ShareText2(Context ctx,String 分享的文本){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, 分享的文本);
        sendIntent.setType("text/plain");
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, sendIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send(); // 触发Activity的启动
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public static void ShareImg2(Context ctx,String 分享的图片路径){
        byte[] decodedBytes = Base64.decode(分享的图片路径, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

        // 将 Bitmap 转为 URI
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String path = android.provider.MediaStore.Images.Media.insertImage(
                ctx.getContentResolver(), bitmap, "Image", null);
        Uri imageUri = Uri.parse(path);
        // 创建分享意图
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send(); // 触发Activity的启动
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public static void authorization(Context ctx,String APP包名,String state,String code,String appid,String url){
        String TAG = "authorization";
        Log.d(TAG, "authorization: 开始");
        Intent intent  = new Intent();
        intent.setClassName(APP包名,APP包名+".wxapi.WXEntryActivity");
        Bundle bundle = new Bundle();
        bundle.putString("_wxapi_sendauth_resp_state", state);
        bundle.putString("_wxapi_sendauth_resp_token", code);
        String str3 = null;
        bundle.putString("_wxapi_baseresp_transaction", null);
        bundle.putString("_wxapi_sendauth_resp_lang", "zh_CN");
        bundle.putInt("_wxapi_command_type", 1);
        bundle.putString("_wxapi_sendauth_resp_country", "CN");
        bundle.putString("wx_token_key", "com.tencent.mm.openapi.token");
        bundle.putString("_wxapi_sendauth_resp_url", url);
        bundle.putInt("_wxapi_baseresp_errcode", 0);
        bundle.putString("_wxapi_baseresp_errstr", null);
        bundle.putString("_wxapi_baseresp_openId", null);
        intent.putExtras(bundle);

        intent.putExtra("_wxapi_sendauth_resp_auth_result", true);
        intent.putExtra("_mmessage_sdkVersion", 621086464);
        intent.putExtra("_mmessage_appPackage", "com.tencent.mm");
        intent.putExtra("_mmessage_content", str3);
        byte[] cksum = new byte[] { 50, 54, 56, 53, 55, 51, 102, 53, 102, 50, 99, 50, 52, 56, 57, 100, 57, 53, 53, 51, 99, 55, 52, 99, 51, 102, 99, 101, 56, 100, 97, 50 };// a(str3, 620954624, "com.tencent.mm");
        intent.putExtra("_mmessage_checksum", cksum);
        //intent.PutExtra("_message_token", str3);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND).addFlags(Intent.FLAG_RECEIVER_NO_ABORT);
        //ComponentName componentName = new ComponentName(ProjectInfo.Bundleid, ProjectInfo.Bundleid + ".wxapi.WXEntryActivity");
        //intent.SetComponent(componentName);
        ctx.startActivity(intent);
        Log.d(TAG, "authorization: 结束");
    }

    /**
     * 获取CRC32
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static long getFileCRCCode(File file) throws Exception {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
        CRC32 crc32 = new CRC32();
        //CheckedInputStream一种输入流，它还维护正在读取的数据的校验和。 然后可以使用校验和来验证输入数据的完整性。
        CheckedInputStream checkedinputstream = new CheckedInputStream(bufferedInputStream, crc32);
        while (checkedinputstream.read() != -1) {
        }
        checkedinputstream.close();
        bufferedInputStream.close();
        return crc32.getValue();
    }
    /**
     * 压缩成ZIP
     *
     * @param sourceDirectory  源文件夹路径
     * @param zipFile          压缩文件输出地址
     * @param compression      是否压缩文件
     *                         true：
     *                         false：仅复制文件到ZIP包
     * @param keepDirStructure 是否保留原目录结构
     *                         true：保留目录结构
     *                         false：不保留(注意：不保留目录结构可能会出现同名文件,会导致压缩失败)
     * @throws RuntimeException
     */
    public static void zip(String sourceDirectory, String zipFile, boolean compression, boolean keepDirStructure){
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(zipOut)) {
            File sourceFile = new File(sourceDirectory);
            compress(sourceFile, zipOut, sourceFile.getName(), compression, keepDirStructure, bufferedOutputStream);
        } catch (Exception e) {
            //e.printStackTrace();
            Log.d("xxxxxxx",e.getLocalizedMessage());
        }
    }
    public static boolean setScreenPowerMode(int i) {
        IBinder builtInDisplay = SurfaceControl.getBuiltInDisplay();
        if (builtInDisplay != null) {
            return SurfaceControl.setDisplayPowerMode(builtInDisplay, i);
        }
        Log.d("DDM", "设置虚拟息屏失败!");
        return false;
    }
    /**
     * 递归压缩方法
     *
     * @param sourceFile           源文件
     * @param zipOut               压缩后的名称
     * @param name                 zip输出流
     * @param compression          是否压缩文件
     *                             true：
     *                             false：仅复制文件到ZIP包
     * @param keepDirStructure     是否保留原目录结构
     *                             true：保留目录结构
     *                             false：不保留(注意：不保留目录结构可能会出现同名文件,会导致压缩失败)
     * @param bufferedOutputStream
     * @throws Exception
     */


    private static void compress(File sourceFile, ZipOutputStream zipOut, String name, boolean compression,
                                 boolean keepDirStructure, BufferedOutputStream bufferedOutputStream) {
        if (sourceFile.isFile()) {//单文件，直接压缩
            try{
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(sourceFile));
                if (compression) {
                    zipOut.putNextEntry(new ZipEntry(name));
                } else {
                    ZipEntry entry = new ZipEntry(name);
                    //核心，和复制粘贴效果一样，并没有压缩，但速度很快
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(sourceFile.length());
                    //  entry.setCrc(getFileCRCCode(sourceFile));
                    zipOut.putNextEntry(entry);
                }

                int len = 0;
                byte[] data = new byte[8192];
                while ((len = bufferedInputStream.read(data)) != -1) {
                    bufferedOutputStream.write(data, 0, len);
                }
                bufferedInputStream.close();
                bufferedOutputStream.flush();

            }catch (Exception e){
              //  e.printStackTrace();
                Log.d("xxxxxxx",e.getLocalizedMessage());
            }

        } else {//目录或多文件
            File[] listFiles = sourceFile.listFiles();
            if (listFiles == null || listFiles.length == 0) {//目录复制
                // 需要保留原来的文件结构时,需要对空文件夹进行处理
                if (keepDirStructure) {
                    // 空文件夹的处理
                    try {
                        zipOut.putNextEntry(new ZipEntry(name + "/"));
                        // 没有文件，不需要文件的copy
                        zipOut.closeEntry();
                    } catch (IOException e) {
                    //    e.printStackTrace();
                        Log.d("xxxxxxx",e.getLocalizedMessage());
                    }
                }
            } else {
                for (File file : listFiles) {
                    // 判断是否需要保留原来的文件结构
                    if (keepDirStructure) {
                        // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
                        // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
                        compress(file, zipOut, name + "/" + file.getName(), compression, keepDirStructure, bufferedOutputStream);
                    } else {
                        compress(file, zipOut, file.getName(), compression, keepDirStructure, bufferedOutputStream);
                    }

                }
            }
        }
    }
    @TargetApi(Build.VERSION_CODES.M)
    public  static Location getGPS(Context context){
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);        // 默认Android GPS定位实例

        Location location = null;
        // 是否已经授权
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
        {
            location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            //location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);      // 其他应用使用定位更新了定位信息 需要开启GPS
        }
        return location;
        }
    static class ZipCompress
    {
        private String zipFileName;      // 目的地Zip文件
        private String sourceFileName;   //源文件（带压缩的文件或文件夹）

        public ZipCompress(String zipFileName,String sourceFileName)
        {
            this.zipFileName=zipFileName;
            this.sourceFileName=sourceFileName;
        }

        public void zip() throws Exception
        {
            //File zipFile = new File(zipFileName);
            System.out.println("压缩中...");

            //创建zip输出流
            ZipOutputStream out = new ZipOutputStream( new FileOutputStream(zipFileName));

            //创建缓冲输出流
            BufferedOutputStream bos = new BufferedOutputStream(out);

            File sourceFile = new File(sourceFileName);

            //调用函数
            compress(out,bos,sourceFile,sourceFile.getName());

            bos.close();
            out.close();
            System.out.println("压缩完成");

        }

        public void compress(ZipOutputStream out,BufferedOutputStream bos,File sourceFile,String base) throws Exception
        {
            //如果路径为目录（文件夹）
            if(sourceFile.isDirectory())
            {

                //取出文件夹中的文件（或子文件夹）
                File[] flist = sourceFile.listFiles();

                if(flist.length==0)//如果文件夹为空，则只需在目的地zip文件中写入一个目录进入点
                {
                    System.out.println(base+"/");
                    out.putNextEntry(  new ZipEntry(base+"/") );
                }
                else//如果文件夹不为空，则递归调用compress，文件夹中的每一个文件（或文件夹）进行压缩
                {
                    for(int i=0;i<flist.length;i++)
                    {
                        compress(out,bos,flist[i],base+"/"+flist[i].getName());
                    }
                }
            }
            else//如果不是目录（文件夹），即为文件，则先写入目录进入点，之后将文件写入zip文件中
            {
                out.putNextEntry( new ZipEntry(base) );
                FileInputStream fos = new FileInputStream(sourceFile);
                BufferedInputStream bis = new BufferedInputStream(fos);

                int tag;
                System.out.println(base);
                //将源文件写入到zip文件中
                while((tag=bis.read())!=-1)
                {
                    bos.write(tag);
                }
                bis.close();
                fos.close();

            }
        }
    }
    public static int getDeviceId(){
        return Touch.触摸设备ID;
    }
    public static boolean initTouch(){
        int[] deviceIds = InputDevice.getDeviceIds();
        Log.d("点击事件","deviceIds.length: " + deviceIds.length);
        for (int deviceId : deviceIds) {
            InputDevice device = InputDevice.getDevice(deviceId);
            Log.d("点击事件","Touchscreen Device ID: " + device.toString());
            if ((device.getSources() & InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN) {
                // 这是一个触摸屏设备
                Log.d("点击事件","Touchscreen Device ID: " + deviceId);
                Touch.触摸设备ID = deviceId;
                break;
            }
        }
        Device mDevice = Touch.getDevice(896, 2000000, 6);
        Touch.mEventController = new EventController(mDevice);
        return true;
    }

    public static boolean down(int id,int x,int y){
        return Touch.mEventController.injectTouchV2(id,MotionEvent.ACTION_DOWN,x,y);


    }
    public static boolean move(int id,int x,int y){

        return Touch.mEventController.injectTouchV2(id,MotionEvent.ACTION_MOVE,x,y);

    }
    public static boolean up(int id,int x,int y){
        return Touch.mEventController.injectTouchV2(id,MotionEvent.ACTION_UP,x,y);

    }
    public static boolean initTouchServer(){

        int[] deviceIds = InputDevice.getDeviceIds();
        Log.d("点击事件","deviceIds.length: " + deviceIds.length);
        for (int deviceId : deviceIds) {
            InputDevice device = InputDevice.getDevice(deviceId);
            Log.d("点击事件","Touchscreen Device ID: " + device.toString());
            if ((device.getSources() & InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN) {
                // 这是一个触摸屏设备
                Log.d("点击事件","Touchscreen Device ID: " + deviceId);
                Touch.触摸设备ID = deviceId;
                break;
            }
        }
        Device mDevice = Touch.getDevice(896, 2000000, 6);
        Touch.mEventController = new EventController(mDevice);

        SocketThread socketThread = new SocketThread("lockSync");
        socketThread.run();

        HttpService http = new HttpService(8080);
        try {

            http.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("懒人插件","懒人插件运行 pid:"+android.os.Process.myPid()+"++++++++++++");
        try {
            ServerSocket serverSocket = new ServerSocket(9999);
            Log.d("懒人插件","服务端准备就绪，等待客户端连接2222......");
            while (true) {
                Socket socket = serverSocket.accept();
                Log.d("懒人插件","客户端连接成功！");
                InputStream in = socket.getInputStream();
                byte[] b = new byte[1];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while(-1 != (in.read(b))){
                   // Log.d("懒人插件",String.valueOf(b));
                    if (b[0] =='\r'){
                        String cmd = new String(baos.toByteArray());
                        //Log.d("懒人插件",cmd);
                        JSONObject jsonObject = new JSONObject(cmd);
                        String op = jsonObject.getString("op");
                        if (op.equals("close")){
                            socket.shutdownOutput();
                            in.close();
                            socket.close();
                            Log.d("懒人插件","服务关闭");
                            return true;
                        }else{
                            Touch.tcpTouch(jsonObject);
                            baos.reset();
                        }

                        continue;
                    }else{
                        baos.write(b);
                    }
                }
                socket.shutdownOutput();
                in.close();
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void convertToGif(List<String> imagePaths, String outputPath, int delayBetweenFrames) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.setDelay(500);  // ディレイ 500/ms
        encoder.setRepeat(0);   // 0:ループする -1:ループしない
        encoder.start(bos);     // gitデータ生成先ををbosに設定

        try {
            Bitmap bmp1, bmp2, bmp3;

            // ファイルの読み込み
            bmp1 = BitmapFactory.decodeStream(new FileInputStream("/sdcard/target1.png"));
            encoder.addFrame(bmp1);  // gifに追加
            bmp1.recycle();

            bmp2 = BitmapFactory.decodeStream(new FileInputStream("/sdcard/target2.png"));
            encoder.addFrame(bmp2);  // gifに追加
            bmp2.recycle();

            bmp3= BitmapFactory.decodeStream(new FileInputStream("/sdcard/target3.png"));
            encoder.addFrame(bmp3);  // gifに追加
            bmp3.recycle();

        } catch (FileNotFoundException e) {
        }
        encoder.finish();  // 終了
        File filePath = new File("/sdcard", "sample.gif");
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(filePath);
            // bosに生成されたgifデータをファイルに吐き出す
            outputStream.write(bos.toByteArray());
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }
    public static void LRIntent(Context context,String itemId){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setPackage("cn.damai");
        intent.setComponent(new ComponentName("cn.damai", "cn.damai.commonbusiness.seatbiz.sku.qilin.ui.NcovSkuActivity"));
        intent.putExtra("itemId", itemId);
        intent.putExtra("referrer", "damai://projectdetail");
        intent.putExtra("remindTitle", "【北京】2024 五月天 [回到那一天] 25 周年-北京演唱会");
        intent.putExtra("atomSplit", "1");
        intent.putExtra("remindCountDown", 0L);
        intent.putExtra("DMNav_KRequestCodeReferrer", 4129);
        intent.putExtra("project_time", 1716397800277L);
        intent.putExtra("remindSaleTime", 1716373800000L);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
    }
    public static void ConWifi2(Context context,String SSID,String password){
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", SSID);
        wifiConfig.preSharedKey = String.format("\"%s\"", password);

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
// 添加 WiFi 配置
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }
    public static int ConWifi(Context context2,String SSID,String password){
        Context context = null;
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");

            // Call the currentActivityThread() method to get a reference to the current ActivityThread instance
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);

            // Call the getApplication() method to get a reference to the Application instance
            Method getApplicationMethod = activityThreadClass.getDeclaredMethod("getApplication");
            Object application = getApplicationMethod.invoke(activityThread);

            // Cast the Application instance to a Context instance
            context = (Context) application;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        WifiNetworkSpecifier wifiNetworkSpecifier = null;
        NetworkRequest networkRequest = null;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(SSID)
                    .setWpa2Passphrase(password)
                    .build();
            networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(wifiNetworkSpecifier)
                    .build();
            connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    Log.d("lrPlugin","连接成功");
                    // 已成功连接到指定 Wi-Fi
                    connectivityManager.bindProcessToNetwork(network);
                }
                @Override
                public void onLost(Network network) {
                    Log.d("NetworkCallback", "网络丢失");
                }
            });
        }
        return android.os.Build.VERSION.SDK_INT;
    }
//    public byte[] convertAsciiToUtf8(String asciiString) {
//        Charset utf8Charset = Charset.forName("UTF-8");
//        ByteBuffer byteBuffer = utf8Charset.encode(asciiString);
//        return byteBuffer.array();
//    }
    public static String convertAsciiToUtf8(String asciiString) throws UnsupportedEncodingException {
//        String asciiString = MFile.readFile(path);
//        if (asciiString == null || asciiString.isEmpty()) {
//            return ""; // 如果输入为空，返回空字符串
//        }
        // 将 ASCII 字符串转为 UTF-8 字节数组
        byte[] utf8Bytes = new byte[0];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            utf8Bytes = asciiString.getBytes("GB2312");
        }

        // 将 UTF-8 字节数组转回字符串
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new String(utf8Bytes, Charset.forName("UTF-8"));
        }
        return "";
    }
    public static String convertAsciiToUtf8File(String p1,String p2){
        // 输入文件路径（GB2312 编码）
        String inputFilePath = p1;//"/sdcard/Pictures/cong1.txt";
        // 输出文件路径（UTF-8 编码）
        String outputFilePath = p2;//"/sdcard/Pictures/cong1_utf8.txt";
        try (
                // 使用 GB2312 编码读取文件
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath), "GB2312"));
                // 使用 UTF-8 编码写入文件
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath), Charset.forName("UTF-8")));
        ) {
            int line;
            while ((line = reader.read()) != -1) {
                writer.write(line);
            }
            System.out.println("文件已成功从 GB2312 转换为 UTF-8！");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "ok";
    }


    public static int getCoreCount() {
        int coreCount = 0;
        try {
            // 获取 CPU 核心数
            coreCount = Runtime.getRuntime().availableProcessors();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return coreCount;
    }
    /**
     * 获取 CPU 核心数
     * @return CPU 核心数
     */
    public static int getCoresNumbers() {
        // Private Class to display only CPU devices in the directory listing
        class CpuFilter
                implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                // Check if filename is "cpu", followed by a single digit number
                return Pattern.matches("cpu[0-9]+", pathname.getName());
            }
        }
        // CPU 核心数
        int CPU_CORES = 0;
        try {
            // Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            // Return the number of cores ( virtual CPU devices )
            CPU_CORES = files.length;
        } catch (Throwable e) {

        }
        if (CPU_CORES < 1) {
            CPU_CORES = Runtime.getRuntime().availableProcessors();
        }
        if (CPU_CORES < 1) {
            CPU_CORES = 1;
        }
        return CPU_CORES;
    }
    public static  void main(String[] args ){



        if (args[0].equals("-zip")){
            ZipCompress zipCom = new ZipCompress(args[2],args[1]);
            try
            {
                zipCom.zip();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        if (args[0].equals("-ScreenOff")){
            boolean ret = setScreenPowerMode(2);
        }
        if (args[0].equals("-ScreenOn")){
            boolean ret = setScreenPowerMode(0);
        }
        if (args[0].equals("-initTouchServer")){
            boolean ret = initTouchServer();
            System.out.println(ret);
        }

        if (args[0].equals("-SendCode")){
            String appPackage = args[1];
            String state =args[2] ;
            String WXAPPID = args[3];String Code = args[4];
            int v =638058496;
            SendCode(null,appPackage,state,WXAPPID,Code,v);
        }
    }
}
