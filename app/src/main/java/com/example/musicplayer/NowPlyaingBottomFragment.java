package com.example.musicplayer;

import static android.content.Context.MODE_PRIVATE;
import static com.example.musicplayer.MainActivity.ARTIST_TO_FRAG;
import static com.example.musicplayer.MainActivity.PATH_TO_FRAG;
import static com.example.musicplayer.MainActivity.SHOW_MINI_PLAYER;
import static com.example.musicplayer.MainActivity.SONG_NAME_TO_FRAG;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class NowPlyaingBottomFragment extends Fragment implements ServiceConnection {
    private ImageView nextBtn, bottomArt;
    private TextView artist, songName;
    private FloatingActionButton playPauseBtn;
    private View view;
    private MusicService musicService;

    private static final String MUSIC_LAST_PLAYED = "LAST_PLAYED";
    private static final String MUSIC_FILE = "STORED_MUSIC";
    private static final String ARTIST_NAME = "ARTIST_NAME";
    private static final String SONG_NAME = "SONG_NAME";

    public NowPlyaingBottomFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_now_plyaing_bottom, container, false);

        artist = view.findViewById(R.id.song_artist_miniPlayer);
        songName = view.findViewById(R.id.song_name_miniPlayer);
        bottomArt = view.findViewById(R.id.bottom_album_art);
        nextBtn = view.findViewById(R.id.skip_next_bottom);
        playPauseBtn = view.findViewById(R.id.play_pause_miniPlayer);

        nextBtn.setOnClickListener(v -> handleNextButtonClick());
        playPauseBtn.setOnClickListener(v -> handlePlayPauseButtonClick());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
        bindMusicService();
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindMusicService();
    }

    private void handleNextButtonClick() {
        if (musicService != null) {
            musicService.nextBtnClicked();
            updateSharedPreferences();
            updateUI();
        }
    }

    private void handlePlayPauseButtonClick() {
        if (musicService != null) {
            musicService.playPauseBtnClicked();
            updatePlayPauseButtonIcon();
        }
    }

    private void updateSharedPreferences() {
        if (getActivity() != null) {
            SharedPreferences.Editor editor = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
            if (musicService.position >= 0 && musicService.position < musicService.musicFiles.size()) {
                editor.putString(MUSIC_FILE, musicService.musicFiles.get(musicService.position).getPath());
                editor.putString(ARTIST_NAME, musicService.musicFiles.get(musicService.position).getArtist());
                editor.putString(SONG_NAME, musicService.musicFiles.get(musicService.position).getTitle());
            } else {
                Log.d("NowPlayingBottomFragment", "Invalid position: " + musicService.position);
            }
            editor.apply();
        }
    }

    private void updateUI() {
        SharedPreferences preferences = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
        String path = preferences.getString(MUSIC_FILE, null);
        String artistName = preferences.getString(ARTIST_NAME, null);
        String songNameStr = preferences.getString(SONG_NAME, null);

        if (path != null) {
            SHOW_MINI_PLAYER = true;
            PATH_TO_FRAG = path;
            ARTIST_TO_FRAG = artistName;
            SONG_NAME_TO_FRAG = songNameStr;

            byte[] art = getAlbumArt(PATH_TO_FRAG);
            Glide.with(getContext())
                    .load(art != null ? art : R.drawable.img)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .circleCrop()
                    .into(bottomArt);

            songName.setText(SONG_NAME_TO_FRAG);
            artist.setText(ARTIST_TO_FRAG);
        } else {
            SHOW_MINI_PLAYER = false;
            PATH_TO_FRAG = null;
            ARTIST_TO_FRAG = null;
            SONG_NAME_TO_FRAG = null;
        }
    }

    private void updatePlayPauseButtonIcon() {
        if (musicService.isPlaying()) {
            playPauseBtn.setImageResource(R.drawable.ic_pause);
        } else {
            playPauseBtn.setImageResource(R.drawable.ic_play);
        }
    }

    private void bindMusicService() {
        Intent intent = new Intent(getContext(), MusicService.class);
        if (getContext() != null) {
            getContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindMusicService() {
        if (getContext() != null) {
            getContext().unbindService(this);
        }
    }

    private byte[] getAlbumArt(String uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(uri);
            return retriever.getEmbeddedPicture();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder binder = (MusicService.MyBinder) service;
        musicService = binder.getService();
        updatePlayPauseButtonIcon();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }
}
