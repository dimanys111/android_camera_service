package com.prospartan.dttwtsolver.myapplication;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WalkingIconService extends Service {
    public static String phoneNumber="";
    public static String Imia="";
    public static EditText editText;
    public static Button b;
    public static WalkingIconService Ser=null;

    private final String LOG_TAG = "myLogs";
    private WindowManager windowManager;
    private CameraPreview mPreview=null;


    private File dirPik;
    private Camera.AutoFocusCallback myAutoFocusCallback;
    private Camera.PictureCallback myPictureCallback;
    private Camera mCamera = null;
    private int schech = 0;
    private static int CAMERA_ID = 0;
    private MediaRecorder mediaRecorder = null;

    private File dirP;

    AudioManager audioMgr;
    int oldStreamVolume;

    private int[][] matrixA = null;
    private int[][] matrixAOld = null;

    public boolean siem=false;
    public boolean zvon=false;
    public boolean zap=false;
    public boolean pokazBoll=false;

    public static boolean zariad=true;

    private PowerManager.WakeLock wakeLock;

    private NotificationManager mNotificationManager;
    private static final int NOTIFY_ID = 1; // Уникальный индификатор вашего уведомления в пределах класса

    private static WindowManager.LayoutParams paramsF;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                int g=level * 100 / scale;
                if (g<10)
                    zariad=false;
                else
                    zariad=true;
            }
        }
    };

    public void onDestroy() {
        if (wakeLock.isHeld())
            wakeLock.release();
        mNotificationManager.cancel(NOTIFY_ID);
        Ser=null;
        siem =false;
        zap=false;
        releaseCamera();
        releaseSV();
        releaseMediaRecorder();
        //unregisterReceiver(mBatteryInfoReceiver);
        super.onDestroy();
    }

    public void setZvon(String Imia) {
        zvon=!zvon;
        if (zvon)
        {
            File dir = new File(dirPik, "MyPatch");
            dir.mkdirs();

            Calendar c = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String strDate = sdf.format(c.getTime());
            File outFile = new File(dir, Imia+" "+strDate + ".3gpp");

            releaseMediaRecorder();

            creatAudioMediaRecorder(outFile);
        }
        else
        {
            releaseMediaRecorder();
        }
    }

    public void On_Of_StartFoto() {
        siem=!siem;
        if (siem)
        {
            createCamera();
            releaseSV();
            svAddwindowManager();
            wakeLock.acquire(10*60*1000L /*10 minutes*/);
        }
        else
        {
            releaseMediaRecorder();
            releaseCamera();
            releaseSV();
            wakeLock.release();
        }
    }

    private void  nachZapFoto() {
        try {
            File dir = new File(dirPik, "MyPatch");
            dir.mkdirs();

            Calendar c = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String strDate = sdf.format(c.getTime());

            File outFile = new File(dir, strDate + ".3gpp");

            FileOutputStream fos = new FileOutputStream(outFile);

            fos.write(1);
            fos.close();

            long siz = outFile.getFreeSpace();
            if (outFile.exists()) {
                outFile.delete();
            }

            if (siz > 5000000) {
                if (mediaRecorder==null) {
                    outFile = new File(dir, strDate + ".3gpp");
                    releaseMediaRecorder();
                    creatAudioMediaRecorder(outFile);
                }
                if (mCamera != null)
                    mCamera.autoFocus(myAutoFocusCallback);
            } else {
                siem = false;
                b.setText("Начать фоткать");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void  creatAudioMediaRecorder(File outFile) {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(outFile.getAbsolutePath());
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();
    }

    public void onCreate() {
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        CAMERA_ID = intent.getIntExtra("camID", 1);

        Ser=this;

        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE); // Создаем экземпляр менеджера уведомлений
        int icon = android.R.drawable.sym_action_email; // Иконка для уведомления, я решил воспользоваться стандартной иконкой для Email
        CharSequence tickerText = "Кам Зап"; // Подробнее под кодом
        long when = System.currentTimeMillis(); // Выясним системное время

        Context context = getApplicationContext();
        CharSequence contentTitle = "Кам Зап"; // Текст заголовка уведомления при развернутой строке статуса
        CharSequence contentText = "Нажмите для запуска"; //Текст под заголовком уведомления при развернутой строке статуса
        Intent notificationIntent = new Intent(this, MainActivity.class); // Создаем экземпляр Intent
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0); // Подробное описание в UPD к статье
        Notification notification = new Notification.Builder(context)
                .setWhen(when)
                .setTicker(tickerText)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(icon)
                .setContentIntent(contentIntent)
                .build(); // Создаем экземпляр уведомления, и передаем ему наши параметры
         // Передаем в наше уведомление параметры вида при развернутой строке состояния
        mNotificationManager.notify(NOTIFY_ID, notification); // И наконец показываем наше уведомление через менеджер передав его ID

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");

        matrixA = new int[8][8];
        matrixAOld = new int[8][8];

        dirPik = Environment.getExternalStorageDirectory();
        dirP = new File(dirPik, "MyPatch");

        myAutoFocusCallback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                File file = new File(dirP, "1.jpg");
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(1);
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                long siz=file.getFreeSpace();
                if (file.exists()) {
                    file.delete();
                }

                if (siz>5000000 || zariad) {
                    camera.takePicture(null, null, myPictureCallback);
                }
                else
                {
                    releaseMediaRecorder();
                    editText.setText("Место кончилось или заряд");
                    b.setText("Начать фоткать");
                    siem=false;
                }
            }
        };

        myPictureCallback=new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try
                {
                    Bitmap tgtImg = BitmapFactory.decodeByteArray(data,0,data.length);
                    tgtImg = Bitmap.createScaledBitmap(tgtImg,8,8,false);

                    int sumIark=0;
                    int sumDelIark=0;

                    for (int row=0; row<8; row++){
                        for (int col=0; col<8; col++){
                            int rgb = tgtImg.getPixel(col, row);
                            matrixA[col][row]= (int) (0.114*Color.blue(rgb)+0.587*Color.green(rgb)+0.299*Color.red(rgb));
                            sumIark=sumIark+matrixA[col][row];

                            sumDelIark=sumDelIark+Math.abs(matrixA[col][row]-matrixAOld[col][row]);
                        }
                    }

                    for (int row=0; row<8; row++){
                        for (int col=0; col<8; col++){
                            matrixAOld[col][row]=matrixA[col][row];
                        }
                    }

                    if (sumIark>2400 && sumDelIark>500)
                    {
                        schech++;
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String strDate = sdf.format(c.getTime());
                        File photoFile = new File(dirP, strDate+".jpg");
                        editText.setText(String.valueOf(schech));
                        FileOutputStream fos = new FileOutputStream(photoFile);
                        fos.write(data);
                        fos.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (siem) {
                    camera.startPreview();
                    camera.autoFocus(myAutoFocusCallback);
                }
                else
                {
                    camera.startPreview();
                }
            }
        };
        return super.onStartCommand(intent, flags, startId);
    }

    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }
            setCameraDisplayOrientation(mCamera);

            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
                if(siem)
                {
                    nachZapFoto();
                }
                if (zap)
                {
                    nachZapVid();
                }
            } catch (Exception e){
                Log.d(LOG_TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(CAMERA_ID); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void createCamera() {
        releaseCamera();
        mCamera = getCameraInstance();
    }

    private void  svAddwindowManager() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mPreview = new CameraPreview(this, mCamera);
        try {
            mPreview.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = paramsF.x;
                            initialY = paramsF.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            break;
                        case MotionEvent.ACTION_MOVE:
                            paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                            paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(mPreview, paramsF);
                            break;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        paramsF = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        paramsF.gravity = Gravity.TOP | Gravity.START;
        paramsF.x = 0;
        paramsF.y = 100;

        paramsF.height = 100;
        paramsF.width = 100;
        windowManager.addView(mPreview, paramsF);
    }

    public void setSVpok() {
        if (pokazBoll) {
            paramsF.height = 300;
            paramsF.width = 300;
        }
        else
        {
            paramsF.height = 1;
            paramsF.width = 1;
        }

        pokazBoll=!pokazBoll;
        windowManager.updateViewLayout(mPreview, paramsF);
    }

    public void setCameraID(int i) {
        CAMERA_ID =i;

        if (siem) {
            releaseCamera();
            createCamera();
            releaseSV();
            svAddwindowManager();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void setCameraDisplayOrientation(Camera camera) {
        // определяем насколько повернут экран от нормального положения
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result = 0;
        // получаем инфо по камере cameraId
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, info);

        // задняя камера
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            // передняя камера
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        camera.setDisplayOrientation(result);

        Camera.Parameters parameters = camera.getParameters(); //6
        int rotate = (degrees + 90) % 360;
        parameters.setRotation(rotate);
        camera.setParameters(parameters);
    }

    public void On_Of_StartRecord() {
        zap=!zap;
        if (zap)
        {
            createCamera();
            svAddwindowManager();
            wakeLock.acquire(10*60*1000L /*10 minutes*/);
        }
        else
        {
            releaseMediaRecorder();
            releaseCamera();
            releaseSV();
            wakeLock.release();
        }
    }

    private void  nachZapVid() {
        if (mediaRecorder==null) {
            if (prepareVideoRecorder()) {
                audioMgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                assert audioMgr != null;
                oldStreamVolume = audioMgr.getStreamVolume(AudioManager.STREAM_RING);
                audioMgr.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
                mediaRecorder.start();
                audioMgr.setStreamVolume(AudioManager.STREAM_RING, oldStreamVolume, 0);
            } else {
                releaseMediaRecorder();
            }
        }
    }

    private boolean prepareVideoRecorder() {

        mCamera.unlock();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH));
        File dir = new File(dirPik, "MyPatch");
        dir.mkdirs();
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = sdf.format(c.getTime());
        File videoFile = new File(dir, strDate + ".3gp");
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            audioMgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            assert audioMgr != null;
            oldStreamVolume = audioMgr.getStreamVolume(AudioManager.STREAM_RING);
            audioMgr.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            mediaRecorder.stop();
            audioMgr.setStreamVolume(AudioManager.STREAM_RING, oldStreamVolume, 0);
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            if (mCamera!= null)
                mCamera.lock();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void releaseSV() {
        if (mPreview!= null) {
            windowManager.removeView(mPreview);
            mPreview = null;
        }
    }
}