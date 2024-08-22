package com.example.musicplayer;

import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

public class Album_Details_Activity extends AppCompatActivity {
    RecyclerView recyclerView;
    ImageView album_photo;
    String album_name;
    ArrayList<MusicFiles> album_songs = new ArrayList<>();
    AlbumDetailAdapter albumDetailAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_album_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        recyclerView = findViewById(R.id.recyclerView);
        album_photo = findViewById(R.id.album_photo);
        album_name = getIntent().getExtras().getString("albumName");
        int j =0;
        for(int i=0;i<MainActivity.musicFiles.size();i++){
            if (album_name.equals(MainActivity.musicFiles.get(i).getAlbum())){
                album_songs.add(j,MainActivity.musicFiles.get(i));
                j++;
            }

        }
        byte[] image = getAlbumArt(album_songs.get(0).getPath());
        if(image != null){
            Glide.with(this).asBitmap()
                    .load(image)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(album_photo);
        }
        else{
            Glide.with(this)
                    .load(R.drawable.img)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(album_photo);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((album_songs.size()) > 0){
            albumDetailAdapter = new AlbumDetailAdapter(this , album_songs);
            recyclerView.setAdapter(albumDetailAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        }
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
}