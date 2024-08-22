package com.example.musicplayer;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static final int REQUEST_CODE = 1;
    private static final String MY_SORT_PREF = "SortOrder";
    private static final String MUSIC_LAST_PLAYED = "LAST_PLAYED";
    private static final String MUSIC_FILE = "STORED_MUSIC";
    private static final String ARTIST_NAME = "ARTIST_NAME";
    private static final String SONG_NAME = "SONG_NAME";

    public static ArrayList<MusicFiles> musicFiles;
    public static boolean shuffleBoolean = false;
    public static boolean repeatBoolean = false;
    public static ArrayList<MusicFiles> albums = new ArrayList<>();
    public static boolean SHOW_MINI_PLAYER = false;
    public static String PATH_TO_FRAG = null;
    public static String SONG_NAME_TO_FRAG = null;
    public static String ARTIST_TO_FRAG = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
            getSupportActionBar().setTitle("Music");

        }
        setContentView(R.layout.activity_main);
        requestStoragePermissions();

    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void requestPermission(String... permissions) {
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
                break;
            }
        }
        if (allGranted) {
            onPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted();
            } else {
                Toast.makeText(this, "Permission Denied! The app needs storage access to function properly.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onPermissionGranted() {
        musicFiles = getAllAudio(this);
        initViewPager();
    }

    private void initViewPager() {
        ViewPager viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tab);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragments(new SongsFragment(), "Songs");
        viewPagerAdapter.addFragments(new AlbumFragment(), "Albums");
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    public static class ViewPagerAdapter extends FragmentPagerAdapter {

        private final ArrayList<Fragment> fragments;
        private final ArrayList<String> titles;

        public ViewPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.fragments = new ArrayList<>();
            this.titles = new ArrayList<>();
        }

        void addFragments(Fragment fragment, String title) {
            fragments.add(fragment);
            titles.add(title);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

    private ArrayList<MusicFiles> getAllAudio(Context context) {
        SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE);
        String sortOrder = preferences.getString("sorting", "sortByName");

        ArrayList<String> duplicate = new ArrayList<>();
        albums.clear();
        ArrayList<MusicFiles> tempAudioList = new ArrayList<>();
        String order = getSortOrder(sortOrder);
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, order)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    MusicFiles musicFiles = new MusicFiles(
                            cursor.getString(3), // path
                            cursor.getString(1), // title
                            cursor.getString(4), // artist
                            cursor.getString(0), // album
                            cursor.getString(2), // duration
                            cursor.getString(5)  // id
                    );

                    Log.e("Path : " + musicFiles.getPath(), "Album : " + musicFiles.getAlbum());

                    tempAudioList.add(musicFiles);

                    if (!duplicate.contains(musicFiles.getAlbum())) {
                        albums.add(musicFiles);
                        duplicate.add(musicFiles.getAlbum());
                    }
                }
            }
        }

        return tempAudioList;
    }

    private String getSortOrder(String sortOrder) {
        switch (sortOrder) {
            case "sortByDate":
                return MediaStore.MediaColumns.DATE_ADDED + " ASC";
            case "sortBySize":
                return MediaStore.MediaColumns.SIZE + " DESC";
            case "sortByName":
            default:
                return MediaStore.MediaColumns.DISPLAY_NAME + " ASC";
        }
    }

    public static void deleteAudioFile(Context context, String fileId) {
        Uri deleteUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String where = MediaStore.Audio.Media._ID + "=?";
        String[] selectionArgs = new String[]{fileId};
        context.getContentResolver().delete(deleteUri, where, selectionArgs);
    }

    public static void deleteAudioFileForBelowAndroid13(Context context, String filePath) {
        File file = new File(Uri.parse(filePath).getPath());

        if (file.exists() && file.delete()) {
            Log.e("File Deleted", "File: " + filePath);
        } else {
            Log.e(file.exists() ? "File Deletion Failed" : "File Not Found", "File: " + filePath);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        MenuItem menuItem = menu.findItem(R.id.search_option);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String userInput = newText.toLowerCase();
        ArrayList<MusicFiles> filteredFiles = new ArrayList<>();

        for (MusicFiles song : musicFiles) {
            if (song.getTitle().toLowerCase().contains(userInput)) {
                filteredFiles.add(song);
            }
        }

        SongsFragment.musicAdapter.updateList(filteredFiles);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE).edit();
        int itemId = item.getItemId();

        if (itemId == R.id.sort_by_name) {
            editor.putString("sorting", "sortByName");
            editor.apply();
            this.recreate();
        } else if (itemId == R.id.sort_by_date) {
            editor.putString("sorting", "sortByDate");
            editor.apply();
            this.recreate();
        } else if (itemId == R.id.sort_by_size) {
            editor.putString("sorting", "sortBySize");
            editor.apply();
            this.recreate();
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);
        PATH_TO_FRAG = preferences.getString(MUSIC_FILE, null);
        ARTIST_TO_FRAG = preferences.getString(ARTIST_NAME, null);
        SONG_NAME_TO_FRAG = preferences.getString(SONG_NAME, null);

        SHOW_MINI_PLAYER = PATH_TO_FRAG != null;
    }
}
