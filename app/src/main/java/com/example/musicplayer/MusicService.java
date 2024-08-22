package com.example.musicplayer;

import static com.example.musicplayer.ApplicationClass.ACTION_NEXT;
import static com.example.musicplayer.ApplicationClass.ACTION_PLAY;
import static com.example.musicplayer.ApplicationClass.ACTION_PREVIOUS;
import static com.example.musicplayer.ApplicationClass.CHANNEL_ID_2;
import static com.example.musicplayer.PlayerActivity.listSongs;
import static com.example.musicplayer.PlayerActivity.uri;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.security.Provider;
import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {
    MyBinder mBinder = new MyBinder();
    MediaPlayer mediaPlayer;
    ArrayList<MusicFiles> musicFiles = new ArrayList<>();
    private ArrayList<MusicFiles> albumSongs = new ArrayList<>();
    Uri uri;
    int position = -1;
    ActionPlaying actionPlaying;
    MediaSessionCompat mediaSessionCompat;
    public static final String MUSIC_LAST_PLAYED = "LAST_PLAYED"; //this variable use as a key for store value in shared preference
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String ARTIST_NAME = "ARTIST_NAME";
    public static final String SONG_NAME = "SONG_NAME";


    @Override
    public void onCreate() {
        super.onCreate();

        mediaSessionCompat = new MediaSessionCompat(getBaseContext(),"My Audio");

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("Bind","Method");
        return mBinder;
    }



    public class MyBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int myPosition = intent.getIntExtra("position", -1);
            String actionName = intent.getStringExtra("ActionName");

            if (myPosition != -1) {
                playMedia(myPosition);
            }

            if (actionName != null) {
                switch (actionName) {
                    case "playPause":
                        playPauseBtnClicked();
                        break;
                    case "next":
                        nextBtnClicked();
                        break;
                    case "previous":
                        prevBtnClicked();
                        break;
                }
            }
        } else {
            Log.e("MusicService", "onStartCommand: Intent is null");
            stopSelf();
        }
        return START_STICKY;
    }


//    public int onStartCommand(Intent intent, int flags, int startId){
//        if (intent != null) {
//            int myPosition = intent.getIntExtra("position", -1);
//            String actionName = intent.getStringExtra("ActionName");
//            if (myPosition != -1) {
//                playMedia(myPosition);
//            }
//            if(actionName != null){
//                switch(actionName){
//                    case "playPause":
//                        if (actionPlaying != null){
//                            Log.e("inside" , "Action");
//                            actionPlaying.playPauseBtnClicked();
//                        }
//                        break;
//                    case "next":
//                        if (actionPlaying != null){
//                            Log.e("inside" , "Action");
//                            actionPlaying.nextBtnClicked();
//                        }
//                        break;
//                    case "previous":
//                        if (actionPlaying != null){
//                            Log.e("inside" , "Action");
//                            actionPlaying.prevBtnClicked();
//                        }
//                        break;
//                }
//            }
//        } else {
//            // Handle the case where intent is null, e.g., log a message or stop the service
//            Log.e("MusicService", "onStartCommand: Intent is null");
//            stopSelf(); // Consider stopping the service if the intent is null
//        }
//        return START_STICKY;
//    }

    private void playMedia(int startPosition) {
        // Stop and release the previous MediaPlayer if it's playing
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();   // Stop the previous song
            }
            mediaPlayer.reset();      // Reset the MediaPlayer to its uninitialized state
            mediaPlayer.release();    // Release the resources of the MediaPlayer
            mediaPlayer = null;       // Nullify the reference
        }

        // Ensure `listSongs` is populated
        position = startPosition;

        // Create and start the new MediaPlayer instance
        if (albumSongs != null && !albumSongs.isEmpty()) {
            uri = Uri.parse(musicFiles.get(position).getPath());
            createMediaPlayer(position);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                onCompleted();  // Set listener for completion of the song
            } else {
                Log.e("MusicService", "MediaPlayer creation failed");
            }
        } else {
            Log.e("MusicService", "No songs available in the list");
        }
    }



    void start(){
        mediaPlayer.start();
    }

    boolean isPlaying(){
        if (mediaPlayer == null) {
            return false;
        }
        return mediaPlayer.isPlaying();
    }

    void stop(){
        mediaPlayer.stop();
    }

    void release(){
        mediaPlayer.release();
    }

    int getDuration(){
        return mediaPlayer.getDuration();
    }

    int getCurrentPosition(){
        return mediaPlayer.getCurrentPosition();
    }

    void seekTo(int position){
        mediaPlayer.seekTo(position);
    }

    public void createMediaPlayer(int positionInner) {
        position = positionInner;
        if (listSongs != null && !listSongs.isEmpty()) {
            if (position >= 0 && position < listSongs.size()) {
                uri = Uri.parse(listSongs.get(position).getPath());

                SharedPreferences.Editor editor = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
                editor.putString(MUSIC_FILE, uri.toString());
                editor.putString(ARTIST_NAME, listSongs.get(position).getArtist());
                editor.putString(SONG_NAME, listSongs.get(position).getTitle());
                editor.apply();

                mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
            } else {
                Log.e("MusicService", "Invalid position: " + position + " for list size: " + listSongs.size());
                // Consider setting position to 0 or a valid index
            }
        } else {
            Log.e("MusicService", "listSongs is null or empty");
            // Handle the case where the list is null or empty, e.g., stop the service or notify the user
        }
    }


    void pause(){
        mediaPlayer.pause();
    }

    void onCompleted(){
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (actionPlaying != null){
            actionPlaying.nextBtnClicked();
            if (mediaPlayer != null){
                createMediaPlayer(position);
                mediaPlayer.start();
                onCompleted();
            }
        }

    }

    void setCallBack(ActionPlaying actionPlaying){
        this.actionPlaying = actionPlaying;

    }

    void showNotification(int playPauseBtn) {
        // Check if position and musicFiles are valid
        if (PlayerActivity.position < 0 || PlayerActivity.position >= PlayerActivity.listSongs.size()) {
            Log.e("MusicService", "Invalid position: " + PlayerActivity.position);
            return;
        }

        // Retrieve the current song based on the position
        MusicFiles currentSong = PlayerActivity.listSongs.get(PlayerActivity.position);

        // Create pending intents for notification actions
        Intent intent = new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0); // No flag for older versions
        }

        Intent prevIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PREVIOUS);
        PendingIntent prevPending = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent pausePending = PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Get album art or default image
        byte[] picture = getAlbumArt(currentSong.getPath());
        Bitmap thumb;
        if (picture != null) {
            thumb = BitmapFactory.decodeByteArray(picture, 0, picture.length);
        } else {
            thumb = BitmapFactory.decodeResource(getResources(), R.drawable.img);
        }

        // Build the notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setSmallIcon(playPauseBtn)
                .setLargeIcon(thumb)
                .setContentTitle(currentSong.getTitle())
                .setContentText(currentSong.getArtist())
                .addAction(R.drawable.ic_skip_previous, "Previous", prevPending)
                .addAction(playPauseBtn, "Pause", pausePending)
                .addAction(R.drawable.ic_skip_next, "Next", nextPending)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionCompat.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .build();

        // Show the notification
        startForeground(1,notification);
    }


    private byte[] getAlbumArt(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(uri);
            byte[] art = retriever.getEmbeddedPicture();
            retriever.release();
            return art;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    void nextBtnClicked() {
        if (albumSongs != null && !albumSongs.isEmpty()) {
            position = (position + 1) % albumSongs.size();
            createMediaPlayer(position);
            mediaPlayer.start();
            onCompleted();
            showNotification(R.drawable.ic_pause);
        }
    }
    void prevBtnClicked() {
        if (albumSongs != null && !albumSongs.isEmpty()) { // Use albumSongs here
            position = (position - 1 < 0 ? albumSongs.size() - 1 : position - 1);
            createMediaPlayer(position);
            mediaPlayer.start();
            onCompleted();
            showNotification(R.drawable.ic_pause);
        }
    }
    void playPauseBtnClicked() {
        if (actionPlaying != null) {
            actionPlaying.playPauseBtnClicked();
        }
    }
    public void setAlbumSongs(ArrayList<MusicFiles> albumSongs) {
        this.albumSongs = albumSongs;
    }
}