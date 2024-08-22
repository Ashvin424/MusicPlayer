package com.example.musicplayer;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private static final int REQUEST_PERMISSION = 123; // Arbitrary value for permission request
    private Context mContext;
    static ArrayList<MusicFiles> mFiles;

    MusicAdapter(Context mContext, ArrayList<MusicFiles> mFiles) {
        this.mFiles = mFiles;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.file_name.setText(mFiles.get(position).getTitle());
        byte[] image = getAlbumArt(mFiles.get(position).getPath());
        if (image != null) {
            Glide.with(mContext)
                    .load(image)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .circleCrop()
                    .into(holder.album_art);
        } else {
            Glide.with(mContext)
                    .load(R.drawable.img)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .circleCrop()
                    .into(holder.album_art);
        }
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, PlayerActivity.class);
            intent.putExtra("position", position);
            mContext.startActivity(intent);
        });
        holder.menuMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(mContext, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu, popupMenu.getMenu());
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.delete) {
                    deleteFiles(position, v);
                }
                return true;
            });
        });
    }

    private void deleteFiles(int position, View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                deleteFileFromMediaStore(position, v);
            } else {
                if (mContext instanceof Activity) {
                    ActivityCompat.requestPermissions((Activity) mContext,
                            new String[]{android.Manifest.permission.READ_MEDIA_AUDIO},
                            REQUEST_PERMISSION);
                } else {
                    Snackbar.make(v, "Cannot request permission from this context", Snackbar.LENGTH_LONG).show();
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 & 12
            deleteFileFromMediaStore(position, v);
        } else { // Android 10 and below
            deleteFileForBelowAndroid13(position, v);
        }
    }


    private void deleteFileForBelowAndroid13(int position, View v) {
        // This method handles deletion for Android versions below 13
        String fileId = mFiles.get(position).getId();
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(fileId));

        int rowsDeleted = mContext.getContentResolver().delete(contentUri, null, null);

        if (rowsDeleted > 0) {
            mFiles.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mFiles.size());
            Snackbar.make(v, "File Deleted", Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(v, "File not found in MediaStore", Snackbar.LENGTH_LONG).show();
        }
    }

    private void deleteFileFromMediaStore(int position, View v) {
        String fileId = mFiles.get(position).getId();
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(fileId));

        try {
            int rowsDeleted = mContext.getContentResolver().delete(contentUri, null, null);

            if (rowsDeleted > 0) {
                mFiles.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, mFiles.size());
                Snackbar.make(v, "File Deleted", Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(v, "File not found in MediaStore", Snackbar.LENGTH_LONG).show();
            }
        } catch (SecurityException e) {
            Snackbar.make(v, "Permission Denied: Unable to delete file", Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            Snackbar.make(v, "An error occurred: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }


    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView file_name;
        ImageView album_art, menuMore;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            file_name = itemView.findViewById(R.id.music_file_name);
            file_name.setSelected(true);
            album_art = itemView.findViewById(R.id.music_img);
            menuMore = itemView.findViewById(R.id.more_menu);
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
    void updateList(ArrayList<MusicFiles> musicFilesArrayList) {
        mFiles = new ArrayList<>();
        mFiles.addAll(musicFilesArrayList);
        notifyDataSetChanged();
    }
}
