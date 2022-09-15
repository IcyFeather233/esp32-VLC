package com.example.polaris.PolarisApp;

import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.yhh.lightlib.AutoFitTextureView;
import com.yhh.lightlib.LightIdCallback;
import com.yhh.lightlib.LightManager;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = MainActivity.class.getSimpleName();

    private Button mBtnTextureView = null;
    private Button mBtnSurfaceView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnTextureView = (Button)findViewById(R.id.btnStart);
        mBtnSurfaceView = (Button)findViewById(R.id.btnStop);

        mBtnTextureView.setOnClickListener(this);
        mBtnSurfaceView.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnStart:
                LightManager lightManager = LightManager.getInstance(this);
                AutoFitTextureView mTextureView = (AutoFitTextureView) findViewById(R.id.textureView);
                lightManager.setmTextureView(mTextureView);


                try {
                    lightManager.startTracking(new LightIdCallback() {
                        @Override
                        public void run(int lightId) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("lightID")
                                    .setMessage("lightID is : " + lightId)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            LightManager lightManager = LightManager.getInstance();
                                            lightManager.stopTracking();
                                        }
                                    })
                                    .show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btnStop:
                lightManager = LightManager.getInstance(this);
                lightManager.stopTracking();
                break;
            default:
                break;
        }
    }
}
