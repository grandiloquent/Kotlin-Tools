package euphoria.psycho.music;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.*;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import euphoria.psycho.music.repositories.AppHelper;
import euphoria.psycho.music.repositories.MediaHelper;
import euphoria.psycho.music.repositories.MediaPlaybackService;

import java.io.File;
import java.util.List;

public class PlaylistBrowserActivity extends ListActivity {

    private PlaylistListAdapter mAdapter;
    private File mCurrentDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppHelper.setContext(this);
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        AppHelper.requestCommonPermissions(this);
        initializeListView();


    }

    private void initializeListView() {
        ListView listView = getListView();
        registerForContextMenu(listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                File file = new File(mCurrentDirectory, mAdapter.getItem(i));
                if (file.isDirectory()) {
                    refreshListView(file);
                } else {
                    playDirectory(i);
                }
            }
        });
        initializeAdapter();
        listView.setAdapter(mAdapter);
    }

    private void playDirectory(int playPosition) {
        Intent intent = new Intent(this, MediaPlaybackService.class);
        intent.putExtra(MediaPlaybackService.KEY_PLAY_DIRECTORY, mCurrentDirectory.getAbsolutePath());
        intent.putExtra(MediaPlaybackService.KEY_PLAY_POSITION, playPosition);
        startService(intent);
    }

    private void refreshListView(File directory) {

        List<String> files = AppHelper.listAudioFiles(directory, true);

        mCurrentDirectory = directory;

        mAdapter.switchData(files);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_option, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menu_context, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int action = item.getItemId();

        switch (action) {
            case R.id.action_delete:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                File file = new File(mCurrentDirectory, mAdapter.getItem(info.position));
                if (file.exists() && file.isFile()) {
                    file.delete();
                }
                refreshListView(mCurrentDirectory);
                return true;

        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int action = item.getItemId();

        switch (action) {
            case R.id.action_format_song:
                actionFormatSong();
                return true;
            case R.id.action_exit:
                Intent intent = new Intent(this, MediaPlaybackService.class);
                stopService(intent);
                return true;
        }
        return false;
    }

    private void actionFormatSong() {

        List<String> files = AppHelper.listAudioFiles(mCurrentDirectory, false);

        for (String file : files) {

            MediaHelper.renameSong(new File(mCurrentDirectory, file), mCurrentDirectory);
        }
    }

    @Override
    public void onBackPressed() {

        if (mCurrentDirectory.getParentFile() != null) {
            refreshListView(mCurrentDirectory.getParentFile());
        } else
            super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = AppHelper.getDefaultSharedPreferences();

        String directory = sharedPreferences.getString("music_directory", null);
        if (directory == null || !directory.equals(mCurrentDirectory.getAbsolutePath())) {
            sharedPreferences.edit().putString("music_directory", mCurrentDirectory.getAbsolutePath()).commit();
        }
    }

    private List<String> getSongFiles() {


        String directory = AppHelper.getDefaultSharedPreferences().getString("music_directory", AppHelper.getExternalStorageDirectory(null).getAbsolutePath());
        if (directory == null) return null;
        File directoryFile = new File(directory);
        if (!directoryFile.exists()) return null;

        mCurrentDirectory = directoryFile;

//        mCurrentDirectory.listFiles(new FileFilter() {
//            @Override
//            public boolean accept(File file) {
//                if (file.isFile() && file.getName().endsWith("mp3")) {
//                    file.renameTo(new File(file.getAbsolutePath() + ".mp3"));
//                }
//                return false;
//            }
//        });
        return AppHelper.listAudioFiles(directoryFile, true);
    }

    private void initializeAdapter() {
        List<String> files = getSongFiles();

        mAdapter = new PlaylistListAdapter(files, R.layout.track_list_item, this);
    }

    static class ViewHolder {
        TextView textView;
    }

    static class PlaylistListAdapter extends BaseAdapter {

        private final List<String> mItems;
        private final int mLayoutResId;

        private final Context mContext;

        @Override
        public int getCount() {
            return mItems != null ? mItems.size() : 0;
        }

        @Override
        public String getItem(int position) {
            return mItems != null && position < mItems.size() ? mItems.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder = null;
            if (view == null) {
                LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                view = layoutInflater.inflate(mLayoutResId, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.textView = view.findViewById(R.id.line1);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();

            }
            viewHolder.textView.setText(mItems.get(i));

            return view;
        }

        public void switchData(List<String> list) {
            if (mItems == null) return;
            mItems.clear();
            mItems.addAll(list);
            notifyDataSetChanged();
        }

        PlaylistListAdapter(List<String> items, int layoutResId, Context context) {
            mItems = items;
            mLayoutResId = layoutResId;
            mContext = context;
        }
    }

}
