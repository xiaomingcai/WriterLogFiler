package com.example.writerlogfiler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;

/**
 * @author zmz
 * 抓取系统log
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG ="writerlogfiler" ;
    private final int REQUEST = 1;
    private boolean mVolumeDown=false;
    private boolean mPowerDown=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
       if(event.getKeyCode() == KEYCODE_VOLUME_UP&&event.getAction() ==KeyEvent.ACTION_DOWN){
           mVolumeDown=true;
           Toast.makeText(this,"volume",Toast.LENGTH_LONG).show();
       }else if(event.getKeyCode()==KEYCODE_VOLUME_DOWN&&event.getAction()==KeyEvent.ACTION_DOWN){
           mPowerDown=true;
           Toast.makeText(this,"power",Toast.LENGTH_LONG).show();
       }else if(event.getKeyCode()==KEYCODE_VOLUME_DOWN&&event.getAction()==KeyEvent.ACTION_UP){
           mPowerDown=false;
       }else if(event.getKeyCode()==KEYCODE_VOLUME_UP&&event.getAction()==KeyEvent.ACTION_UP){
           mVolumeDown=false;
       }
       if(mVolumeDown && mPowerDown){
           Toast.makeText(this,"开始请求权限",Toast.LENGTH_LONG).show();
           requestPermissionermissions();
       }

        return super.dispatchKeyEvent(event);
    }

    private void requestPermissionermissions(){
    String[] mRequestPermission = {android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    if (!hasPermissions(this, mRequestPermission)) {
        ActivityCompat.requestPermissions(this, mRequestPermission, REQUEST);
    } else {

        new Thread(new Runnable() {
            @Override
            public void run() {
                writeLogFile();
            }
        }).start();
        }
}
    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        writeLogFile();
                    }
                }).start();

            } else {
                Toast.makeText(this, "The app was not allowed to read your store.", Toast.LENGTH_LONG).show();
            }
        }
    }

@SuppressLint("SimpleDateFormat")
    private void writeLogFile() {
        Log.d(TAG, "开始写log");
        Process process = null;
        DataOutputStream os = null;
        String command = "logcat";
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());

            os.write(command.getBytes());
            os.writeBytes("\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();
            InputStream inputStream = process.getInputStream();
            boolean sdCardExist = Environment.getExternalStorageState().equals(
                    android.os.Environment.MEDIA_MOUNTED);
            File dir;
            if (sdCardExist) {

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
                Date date = new Date(System.currentTimeMillis());
                String tg = simpleDateFormat.format(date);
                dir = new File(Environment.getExternalStorageDirectory().toString()
                        + File.separator + "logcat"+tg+".txt");
                Log.d(TAG, "file=" + dir.getPath());
                if (!dir.exists()) {
                    dir.createNewFile();
                }
                byte[] buffer = new byte[1024];
                int bytesLeft = 5 * 1024 * 1024;
                FileOutputStream fos = new FileOutputStream(dir);
                while (bytesLeft > 0) {
                    int read = inputStream.read(buffer, 0, Math.min(bytesLeft,
                            buffer.length));
                    if (read == -1) {
                        throw new EOFException("Unexpected end of data");
                    }
                    fos.write(buffer, 0, read);
                    bytesLeft -= read;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    Log.d(TAG, "dataOutputStream clean");
                    os.close();
                }

                if (process != null) {
                    process.destroy();
                }

            } catch (IOException e) {
                e.printStackTrace();

            }
        }

    }

}