package com.filechooser;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;


import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;

public class FileChooserModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    static final int REQUEST_LAUNCH_FILE_CHOOSER = 1;
    private final ReactApplicationContext mReactContext;
    private Callback mCallback;
    WritableMap response;

    public FileChooserModule(ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addActivityEventListener(this);

        mReactContext = reactContext;
    }

    @Override
    public String getName() {
        return "FileChooser";
    }

    @ReactMethod
    public void show(final ReadableMap options, final Callback callback) {
        Activity currentActivity = getCurrentActivity();
        WritableMap response = Arguments.createMap();

        if (currentActivity == null) {
            response.putString("error", "can't find current Activity");
            callback.invoke(response);
            return;
        }

        if (!permissionsCheck(currentActivity)) {
            response.putBoolean("didRequestPermission", true);
            response.putString("option", "launchFileChooser");
            callback.invoke(response);
            return;
        }

        String mimeType = options.hasKey("mimeType") ? options.getString("mimeType") : "*/*";
        String title = options.hasKey("title") ? options.getString("title") : "Select file to Upload";

        Intent libraryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        libraryIntent.setType(mimeType);
        libraryIntent.addCategory(Intent.CATEGORY_OPENABLE);

        if (libraryIntent.resolveActivity(mReactContext.getPackageManager()) == null) {
            response.putString("error", "Cannot launch file library");
            callback.invoke(response);
            return;
        }

        mCallback = callback;

        try {
            currentActivity.startActivityForResult(
              Intent.createChooser(libraryIntent, title),
              REQUEST_LAUNCH_FILE_CHOOSER
            );
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    // R.N > 33
    public void onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent data) {
        onActivityResult(requestCode, resultCode, data);
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        WritableMap response = Arguments.createMap();

        //robustness code
        if (mCallback == null || requestCode != REQUEST_LAUNCH_FILE_CHOOSER) {
            return;
        }
        // user cancel
        if (resultCode != Activity.RESULT_OK) {
            response.putBoolean("didCancel", true);
            mCallback.invoke(response);
            return;
        }

        Activity currentActivity = getCurrentActivity();
        Uri uri = data.getData();

        if (uri == null) {
            response.putString("error", "uri is null");
            mCallback.invoke(response);
            return;
        }

        response.putString("uri", uri.toString());

        try {
            response.putString("fileName", getFileNameFromUri(currentActivity, uri));
        } catch (Exception ex) {
            response.putString("error", ex.getMessage());
        }

        try {
            response.putDouble("fileSize", getFileSizeFromUri(currentActivity, uri));
        } catch (Exception ex) {
            response.putString("error", ex.getMessage());
        }

        try {
            response.putString("mimeType", currentActivity.getContentResolver().getType(uri));
        } catch (Exception ex) {
            response.putString("error", ex.getMessage());
        }

        mCallback.invoke(response);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                try {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                catch (NumberFormatException ex) {
                    return null;
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                    split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private boolean permissionsCheck(Activity activity) {
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (writePermission != PackageManager.PERMISSION_GRANTED ||
            readPermission != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            ActivityCompat.requestPermissions(activity, PERMISSIONS, 1);
            return false;
        }
        return true;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
        String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
            column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private String getFileFromUri(Activity activity, Uri uri) {
        //If it can't get path of file, file is saved in cache, and obtain path from there
        try {
            String filePath = activity.getCacheDir().toString();
            String fileName = getFileNameFromUri(activity, uri);
            String path = filePath + "/" + fileName;
            if (!fileName.equals("error") && saveFileOnCache(path, activity, uri)) {
                return path;
            } else {
                return "error";
            }
        } catch (Exception e) {
            //Log.d("FileChooserModule", "Error getFileFromStream");
            return "error";
        }
    }

    private String getFileNameFromUri(Activity activity, Uri uri) {
        Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            final int column_index = cursor.getColumnIndexOrThrow("_display_name");
            return cursor.getString(column_index);
        } else {
            return "error";
        }
    }

    private long getFileSizeFromUri(Activity activity, Uri uri) {
        Cursor cursor = activity.getContentResolver().query(uri,
                null, null, null, null);
        cursor.moveToFirst();
        long size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
        cursor.close();
        return size;
    }


    private boolean saveFileOnCache(String path, Activity activity, Uri uri) {
        //Log.d("FileChooserModule", "saveFileOnCache path: "+path);
        try {
            InputStream is = activity.getContentResolver().openInputStream(uri);
            OutputStream stream = new BufferedOutputStream(new FileOutputStream(path));
            byte[] buffer = new byte[1024];
            int len;

            while ((len = is.read(buffer)) != -1)
                stream.write(buffer, 0, len);

            stream.close();

            //Log.d("FileChooserModule", "saveFileOnCache done!");
            return true;

        } catch (Exception e) {
            //Log.d("FileChooserModule", "saveFileOnCache error");
            return false;
        }
    }

    // Required for RN 0.30+ modules than implement ActivityEventListener
    public void onNewIntent(Intent intent) {}

}
