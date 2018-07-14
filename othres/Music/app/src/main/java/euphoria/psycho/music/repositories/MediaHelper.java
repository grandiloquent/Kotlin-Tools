package euphoria.psycho.music.repositories;

import android.media.MediaMetadataRetriever;

import java.io.File;

public class MediaHelper {

    public static void renameSong(File filePath, File directory) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(filePath.getAbsolutePath());
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

            if ((artist != null && artist.length() > 0) || (title != null && title.length() > 0)) {
                String fileName = AppHelper.getValidateWindowFileName(artist, ' ') + " - " + AppHelper.getValidateWindowFileName(title, ' ') + "." + AppHelper.getFileExtension(filePath);
                if (!filePath.getName().equals(fileName))
                    filePath.renameTo(new File(directory, fileName));
            }


        }
    }
}
