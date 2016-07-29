package com.example.sunpeng.audioplaydemo;

import java.io.File;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

/**
 * Created by sunpeng on 2016/7/27.
 */

public class DemoActivity extends Activity implements OnClickListener,
        OnChronometerTickListener, OnSeekBarChangeListener {
    private EditText et_path;
    private Chronometer et_time;
    private SeekBar sb;
    private Button bt_play, bt_pause, bt_replay, bt_stop;
    private MediaPlayer mediaPlayer;
    private TelephonyManager manager;
    private SimpleAudioController audioController;
    /**
     * subtime:点击“续播”到暂停时的间隔的和 beginTime：重新回到播放时的bash值 falgTime：点击“播放”时的值
     * pauseTime：“暂停”时的值
     */
    private long subtime = 0, beginTime = 0, falgTime = 0, pauseTime = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        manager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        manager.listen(new MyListener(), PhoneStateListener.LISTEN_CALL_STATE);
        sb = (SeekBar) this.findViewById(R.id.sb);
        et_path = (EditText) this.findViewById(R.id.et_path);
        et_time = (Chronometer) this.findViewById(R.id.et_time);
        bt_play = (Button) this.findViewById(R.id.play);
        bt_pause = (Button) this.findViewById(R.id.pause);
        bt_replay = (Button) this.findViewById(R.id.replay);
        bt_stop = (Button) this.findViewById(R.id.stop);
        audioController = (SimpleAudioController) findViewById(R.id.audioController);
//        sb.setEnabled(false);
        sb.setOnSeekBarChangeListener(this);
        bt_play.setOnClickListener(this);
        bt_pause.setOnClickListener(this);
        bt_replay.setOnClickListener(this);
        bt_stop.setOnClickListener(this);
        et_time.setOnChronometerTickListener(this);

//        sb.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return true;
//            }
//        });

        audioController.setOnControlButtonClickListener(new SimpleAudioController.OnControlButtonClickListener() {
            @Override
            public void onClick(ImageView imageButton) {
                if(audioController.getProgress()==0){
                    //开始播放
                    String path;
                    falgTime = SystemClock.elapsedRealtime();
                    path = et_path.getText().toString().trim();
                    try {
                        play(path);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    pauseTime = 0;
                    et_time.setBase(falgTime);
                    et_time.start();
                }else{
                    if(audioController.isPlaying){
                        //暂停
                        mediaPlayer.pause();
                        bt_pause.setText("续播");
                        et_time.stop();
                        pauseTime = SystemClock.elapsedRealtime();
                    }else{
                        //继续播放
                        subtime += SystemClock.elapsedRealtime() - pauseTime;
                        mediaPlayer.start();
                        bt_pause.setText("暂停");
                        sb.setEnabled(true);
                        beginTime = falgTime + subtime;
                        et_time.setBase(beginTime);
                        et_time.start();
                    }
                }
            }
        });
    }

    Handler handler = new Handler();
    Runnable updateThread = new Runnable() {
        public void run() {
            // 获得歌曲现在播放位置并设置成播放进度条的值
            if (mediaPlayer != null) {
                sb.setProgress(mediaPlayer.getCurrentPosition());
                audioController.setProgress(mediaPlayer.getCurrentPosition());
                // 每次延迟100毫秒再启动线程
                handler.postDelayed(updateThread, 100);
            }
        }
    };

    public void onClick(View v) {
        String path;
        try {
            switch (v.getId()) {
                case R.id.play:
                    falgTime = SystemClock.elapsedRealtime();
                    path = et_path.getText().toString().trim();
                    play(path);
                    pauseTime = 0;
                    et_time.setBase(falgTime);
                    et_time.start();
                    break;
                case R.id.pause:
                    pause();
                    break;
                case R.id.replay:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.seekTo(0);
                        et_time.setBase(SystemClock.elapsedRealtime());
                        et_time.start();
                    } else {
                        path = et_path.getText().toString().trim();
                        play(path);
                        et_time.setBase(SystemClock.elapsedRealtime());
                        et_time.start();

                    }
                    if ("续播".equals(bt_pause.getText().toString().trim())) {
                        bt_pause.setText("暂停");

                    }
                    falgTime = SystemClock.elapsedRealtime();
                    subtime = 0;
                    break;
                case R.id.stop:
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        mediaPlayer = null;
                        et_time.setBase(SystemClock.elapsedRealtime());
                        et_time.start();
                        et_time.stop();
                        bt_play.setEnabled(true);
                        bt_play.setClickable(true);
                        sb.setProgress(0);
//                        sb.setEnabled(false);
                        falgTime = 0;
                        subtime = 0;

                        audioController.setPlayOver();
                    }
                    break;

            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "文件播放出现异常", Toast.LENGTH_SHORT).show();
        }

    }

    private void pause() {
        // 判断音乐是否在播放
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            // 暂停音乐播放器
            mediaPlayer.pause();
            bt_pause.setText("续播");
//            sb.setEnabled(false);
            et_time.stop();

            pauseTime = SystemClock.elapsedRealtime();
            // System.out.println("1 pauseTime" + pauseTime);
        } else if (mediaPlayer != null
                && "续播".equals(bt_pause.getText().toString())) {
            subtime += SystemClock.elapsedRealtime() - pauseTime;
            // System.out.println("2 subtime:" + subtime);
            mediaPlayer.start();
            bt_pause.setText("暂停");
            sb.setEnabled(true);
            beginTime = falgTime + subtime;
            // System.out.println("3 beginTime" + beginTime);
            et_time.setBase(beginTime);
            et_time.start();
        }
    }

    /**
     * 播放指定地址的音乐文件 .mp3 .wav .amr
     *
     * @param path
     */
    private void play(String path) throws Exception {
        if ("".equals(path)) {
            Toast.makeText(getApplicationContext(), "路径不能为空",Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(path);
        if (file.exists()) {

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            // mediaPlayer.prepare(); // c/c++ 播放器引擎的初始化
            // 同步方法
            // 采用异步的方式
            mediaPlayer.prepareAsync();
            // 为播放器注册
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                public void onPrepared(MediaPlayer mp) {
                    // TODO Auto-generated method stub
                    mediaPlayer.start();
                    bt_play.setEnabled(false);
                    bt_play.setClickable(false);
                    sb.setMax(mediaPlayer.getDuration());
                    audioController.setMax(mediaPlayer.getDuration());
                    Log.i("max",mediaPlayer.getDuration()+"");
                    handler.post(updateThread);
                    sb.setEnabled(true);
                }
            });

            // 注册播放完毕后的监听事件
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                public void onCompletion(MediaPlayer mp) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    bt_play.setEnabled(true);
                    bt_play.setClickable(true);
                    et_time.setBase(SystemClock.elapsedRealtime());
                    et_time.start();
                    et_time.stop();
                    sb.setProgress(0);
//                    sb.setEnabled(false);

                    audioController.setPlayOver();
                }
            });

        } else {
            Toast.makeText(getApplicationContext(), "文件不存在",Toast.LENGTH_SHORT).show();
            return;
        }

    }

    private class MyListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    // 音乐播放器暂停
                    pause();
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // 重新播放音乐
                    pause();
                    break;
            }
        }
    }

    public void onChronometerTick(Chronometer chronometer) {

    }

    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (fromUser == true && mediaPlayer != null) {
            mediaPlayer.seekTo(progress);
            falgTime = SystemClock.elapsedRealtime() - sb.getProgress();
            et_time.setBase(falgTime);
            subtime = 0;
            et_time.start();
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO 自动生成的方法存根
    }
}

