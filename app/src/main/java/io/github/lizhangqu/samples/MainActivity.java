package io.github.lizhangqu.samples;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.tencent.tinker.lib.tinker.Tinker;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Tinker.with(getApplicationContext()).cleanPatch();
    }
}
