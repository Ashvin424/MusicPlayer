package com.example.musicplayer;

import static androidx.core.view.accessibility.AccessibilityEventCompat.setAction;
import static com.example.musicplayer.AlbumDetailAdapter.albumFiles;
import static com.example.musicplayer.ApplicationClass.ACTION_NEXT;
import static com.example.musicplayer.ApplicationClass.ACTION_PLAY;
import static com.example.musicplayer.ApplicationClass.ACTION_PREVIOUS;
import static com.example.musicplayer.ApplicationClass.CHANNEL_ID_2;
import static com.example.musicplayer.MainActivity.musicFiles;
import static com.example.musicplayer.MainActivity.repeatBoolean;
import static com.example.musicplayer.MainActivity.shuffleBoolean;
import static com.example.musicplayer.MusicAdapter.mFiles;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.NotificationCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection {
    TextView song_name, artist_name,duration_played,duration_total;
    ImageView cover_art,next_btn,prev_btn,back_btn,shuffle_btn,repeat_btn;
    FloatingActionButton play_pause_btn;
    SeekBar seekBar;
    static int position = -1;
    static ArrayList<MusicFiles> listSongs = new ArrayList<>();
    static Uri uri;
    //static MediaPlayer mediaPlayer;
    private Handler handler =new Handler();
    private Thread playThread, nextThread, prevThread;
    MusicService musicService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_player);

        // Change R.id.main to R.id.mContainer to match your XML layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        getIntentMethod();


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser){
                    musicService.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(musicService != null){
                    int mCurrentPosition = musicService.getCurrentPosition()/1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this,1000);
            }
        });
        shuffle_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shuffleBoolean){
                    shuffleBoolean = false;
                    shuffle_btn.setImageResource(R.drawable.ic_shuffle_off);
                }
                else {
                        shuffleBoolean = true;
                        shuffle_btn.setImageResource(R.drawable.ic_shuffle_on);
                }
            }
        });
        repeat_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repeatBoolean){
                    repeatBoolean = false;
                    repeat_btn.setImageResource(R.drawable.ic_repeat_off);
                }
                else{
                    repeatBoolean = true;
                    repeat_btn.setImageResource(R.drawable.ic_repeat_on);
                }
            }
        });
        back_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent,this,BIND_AUTO_CREATE);
        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void prevThreadBtn() {
        prevThread = new Thread(){
            @Override
            public void run() {
                super.run();
                prev_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();
                    }
                });
            }
        };
        prevThread.start();
    }


    public void prevBtnClicked() {
        if (musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size()-1);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = ((position - 1)<0 ? (listSongs.size()-1):(position - 1));
            }
            uri =Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.ic_pause);
            play_pause_btn.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
        }else{
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size()-1);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = ((position - 1)<0 ? (listSongs.size()-1):(position - 1));
            }
            uri = Uri.parse(listSongs.get(position).getPath());

            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.ic_play);
            play_pause_btn.setBackgroundResource(R.drawable.ic_play);
        }
    }

    public void nextBtnClicked() {
        if (musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size()-1);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = (position + 1) % listSongs.size();
            }

            uri =Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.ic_pause);
            play_pause_btn.setBackgroundResource(R.drawable.ic_pause);
            musicService.start();
        }else{
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size()-1);
            } else if (!shuffleBoolean && !repeatBoolean) {
                position = (position + 1) % listSongs.size();
            }
            uri =Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.ic_play);
            play_pause_btn.setBackgroundResource(R.drawable.ic_play);
        }
    }

    private int getRandom(int i) {
        Random random = new Random();
        return random.nextInt(i+1);
    }

    private void nextThreadBtn() {
        nextThread = new Thread(){
            @Override
            public void run() {
                super.run();
                next_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();

    }

    private void playThreadBtn() {
        playThread = new Thread(){
            @Override
            public void run() {
                super.run();
                play_pause_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                    }
                });
            }
        };
        playThread.start();
    }

    public void playPauseBtnClicked() {
        if(musicService.isPlaying()){
            play_pause_btn.setImageResource(R.drawable.ic_play);
            musicService.showNotification(R.drawable.ic_play);
            musicService.pause();
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
        }else{
            musicService.showNotification(R.drawable.ic_pause);
            play_pause_btn.setImageResource(R.drawable.ic_pause);
            musicService.start();
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
        }
    }

    private String formattedTime(int mCurrentPosition) {
        String totalout;
        String seconds = String.valueOf(mCurrentPosition % 60);
        String minutes = String.valueOf(mCurrentPosition / 60);

        // Pad the seconds with a zero if it is a single digit
        if (seconds.length() == 1) {
            seconds = "0" + seconds;
        }

        totalout = minutes + ":" + seconds;
        return totalout;
    }

    private void getIntentMethod() {
        position = getIntent().getIntExtra("position", -1);
        String sender = getIntent().getStringExtra("sender");
        if (sender != null && sender.equals("album_details")) {
            listSongs = albumFiles;
        } else {
            listSongs = mFiles; // Use mFiles for main list or search results
        }

        if (listSongs != null && !listSongs.isEmpty()){
            if (position >= 0 && position < listSongs.size()) {
                play_pause_btn.setImageResource(R.drawable.ic_pause);
                uri = Uri.parse(listSongs.get(position).getPath());
            } else {
                Log.e("PlayerActivity", "Invalid position: " + position);
                // Handle invalid position
            }
        } else {
            Log.e("PlayerActivity", "listSongs is null or empty");
            // Handle empty listSongs
        }

        // Start the music service
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("servicePosition", position);
        startService(intent);
        bindService(intent, this, BIND_AUTO_CREATE); // Bind to the service here

        // Don't call showNotification here, as the service is not yet bound
    }


    private void initViews() {
        song_name = findViewById(R.id.songName);
        song_name.setSelected(true);
        artist_name = findViewById(R.id.song_artist);
        duration_played = findViewById(R.id.duration_played);
        duration_total = findViewById(R.id.duration_total);
        cover_art = findViewById(R.id.cover_art);
        next_btn = findViewById(R.id.next_btn);
        prev_btn = findViewById(R.id.prev_btn);
        back_btn = findViewById(R.id.back_btn);
        shuffle_btn = findViewById(R.id.shuffle_off);
        repeat_btn = findViewById(R.id.repeat_btn);
        play_pause_btn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seek_bar);
    }

    private void metaData(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        try {
            int durationTotal = Integer.parseInt(listSongs.get(position).getDuration()) / 1000;
            duration_total.setText(formattedTime(durationTotal));

            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                ImageAnimation(this, cover_art, bitmap);
                Palette.from(bitmap).generate(palette -> {
                    Palette.Swatch swatch = palette != null ? palette.getDominantSwatch() : null;
                    if (swatch != null) {
                        ImageView gradient = findViewById(R.id.imageViewGradient);
                        RelativeLayout mContainer = findViewById(R.id.mContainer);

                        GradientDrawable gradientDrawable = new GradientDrawable(
                                GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{swatch.getRgb(), Color.TRANSPARENT}
                        );
                        gradient.setBackground(gradientDrawable);

                        GradientDrawable gradientDrawableBg = new GradientDrawable(
                                GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{swatch.getRgb(), swatch.getRgb()}
                        );
                        mContainer.setBackground(gradientDrawableBg);

                        song_name.setTextColor(swatch.getTitleTextColor());
                        artist_name.setTextColor(swatch.getBodyTextColor());
                    } else {
                        applyDefaultBackground();
                    }
                });
            } else {
                applyDefaultBackground();
                Glide.with(this)
                        .asBitmap()
                        .load(R.drawable.img)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(cover_art);
            }
        } catch (RuntimeException e) {
            Log.e("PlayerActivity", "Error retrieving metadata", e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.e("PlayerActivity", "Error releasing MediaMetadataRetriever", e);
            }
        }
    }


    public void ImageAnimation(Context context, ImageView imageView, Bitmap bitmap) {
        // Check if bitmap is null
        if (bitmap == null) {
            return;
        }

        // Define fade out and fade in animations
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);

        // Set listeners to animations
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Optional: handle start of animation
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Load new bitmap with Glide
                Glide.with(context)
                        .asBitmap()
                        .load(bitmap)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true) // Prevent caching for immediate changes
                        .into(imageView);

                // Start fade in animation
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        // Optional: handle start of animation
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // Optional: handle end of animation
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        // Optional: handle animation repeat
                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Optional: handle animation repeat
            }
        });

        // Start fade out animation
        imageView.startAnimation(animOut);
    }


    private void applyDefaultBackground() {
        ImageView gradient = findViewById(R.id.imageViewGradient);
        RelativeLayout mContainer = findViewById(R.id.mContainer);

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Color.BLACK, Color.TRANSPARENT}
        );
        gradient.setBackground(gradientDrawable);

        GradientDrawable gradientDrawableBg = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{Color.BLACK, Color.BLACK}
        );
        mContainer.setBackground(gradientDrawableBg);

        song_name.setTextColor(Color.WHITE);
        artist_name.setTextColor(Color.DKGRAY);
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder) service;
        musicService = myBinder.getService();

        if (musicService != null) {
            musicService.setCallBack(this);
            Toast.makeText(this, "Connected to Music Service", Toast.LENGTH_SHORT).show();

            if (listSongs != null && !listSongs.isEmpty() && position >= 0 && position < listSongs.size()) {
                // Initialize MediaPlayer
                musicService.createMediaPlayer(position);

                // Ensure UI components are not null before using them
                if (seekBar != null) {
                    seekBar.setMax(musicService.getDuration() / 1000);
                }

                // Update UI with song metadata
                if (song_name != null) {
                    song_name.setText(listSongs.get(position).getTitle());
                }
                if (artist_name != null) {
                    artist_name.setText(listSongs.get(position).getArtist());
                }
                if (duration_total != null) {
                    duration_total.setText(formattedTime(musicService.getDuration() / 1000));
                }

                // Handle completion and start playback
                musicService.onCompleted();
                musicService.start();

                // Show notification with updated play/pause button
                musicService.showNotification(R.drawable.ic_pause);
            } else {
                Log.e("PlayerActivity", "Invalid position: " + position + " for list size: " + (listSongs != null ? listSongs.size() : "null"));
                // Handle the error (e.g., show a message to the user or fallback to a default behavior)
                Toast.makeText(this, "Invalid song position or empty song list", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("PlayerActivity", "Music service is null");
            // Handle the case where musicService is null
            Toast.makeText(this, "Music service connection failed", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }
}
