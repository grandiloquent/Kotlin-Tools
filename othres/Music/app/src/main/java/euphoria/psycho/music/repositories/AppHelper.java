package euphoria.psycho.music.repositories;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileFilter;
import java.text.Collator;
import java.util.*;
import java.util.regex.Pattern;

public class AppHelper {

    private static Context sContext;

    public static Context getContext() {
        return sContext;
    }

    public static void setContext(Context context) {
        sContext = context;
    }


    public static SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(sContext);
    }


    public static int requestCommonPermissions(Activity activity) {
        int requestPermissionCode = 101;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.WAKE_LOCK,
                    //Manifest.permission.INTERNET
            }, requestPermissionCode);
        }

        return requestPermissionCode;
    }

    private static final char[] ILLEGAL_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    public static String getValidateWindowFileName(String fileName, char replacer) {

        for (char c : ILLEGAL_CHARACTERS) {
            fileName = fileName.replace(c, replacer);
        }
        return fileName;
    }

    public static String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }

    public static File getExternalStorageDirectory(String fileName) {
        if (fileName == null)
            return Environment.getExternalStorageDirectory();
        else
            return new File(Environment.getExternalStorageDirectory(), fileName);
    }

    public static String getFileName(String filePath) {
        int where = filePath.lastIndexOf('/');
        if (where != -1) {
            String fileName = filePath.substring(where + 1);
            where = fileName.lastIndexOf('.');
            if (where != -1) {
                return fileName.substring(0, where);
            }
            return fileName;
        } else {
            where = filePath.lastIndexOf('\\');
            if (where != -1) {
                return filePath.substring(where + 1);
            }
        }
        return filePath;
    }

    public static List<String> listAudioFiles(File directoryFile, final boolean containsDirectory) {
        final List<String> files = new ArrayList<>();
        final Pattern pattern = Pattern.compile("\\.(?:mp3|ogg|wav|flac)$", Pattern.CASE_INSENSITIVE);

        File[] rawFiles = directoryFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (containsDirectory) {
                    if (file.isDirectory() || (file.isFile() && pattern.matcher(file.getName()).find())) {
                        return true;
                    }
                } else {
                    if (file.isFile() && pattern.matcher(file.getName()).find()) {
                        return true;
                    }
                }

                return false;
            }
        });
        if (rawFiles != null) {
            final Collator collator = Collator.getInstance(Locale.CHINA);

            Arrays.sort(rawFiles, new Comparator<File>() {
                @Override
                public int compare(File file, File t1) {
                    if (containsDirectory) {
                        if ((file.isDirectory() && t1.isDirectory()) || (file.isFile() && t1.isFile())) {
                            return collator.compare(file.getName(), t1.getName());
                        }
                        if (file.isDirectory() && t1.isFile()) return -1;
                        if (file.isFile() && t1.isDirectory()) return 1;
                    } else {
                        return collator.compare(file.getName(), t1.getName());
                    }
                    return 0;
                }
            });

        }

        for (File file : rawFiles) {
            files.add(file.getName());
        }
        return files;
    }
}
