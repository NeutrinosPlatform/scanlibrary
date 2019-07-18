package com.scanlibrary;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.media.ExifInterface;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jhansi on 04/04/15.
 */
public class PickImageFragment extends Fragment implements  OnDialogButtonClickListener {

    public final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 1;
    int camorgal = 0;

    // for security permissions
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final String READ_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String WRITE_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private View view;
    private ImageButton cameraButton;
    private ImageButton galleryButton;
    private Uri fileUri;
    private IScanner scanner;


    // for security permissions
    @ValueConstants.DialogType
    private int mDialogType;
    private String mRequestPermissions = "We are requesting the camera and Gallery permission as it is absolutely necessary for the app to perform it\'s functionality.\nPlease select \"Grant Permission\" to try again and \"Cancel \" to exit the application.";
    private String mRequsetSettings = "You have rejected the camera and Gallery permission for the application. As it is absolutely necessary for the app to perform you need to enable it in the settings of your device.\nPlease select \"Go to settings\" to go to application settings in your device and \"Cancel \" to exit the application.";
    private String mGrantPermissions = "Grant Permissions";
    private String mCancel = "Cancel";
    private String mGoToSettings = "Go To Settings";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof IScanner)) {
            throw new ClassCastException("Activity must implement IScanner");
        }
        this.scanner = (IScanner) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.pick_image_fragment, null);
        init();
        return view;
    }

    private void init() {
        cameraButton = (ImageButton) view.findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new CameraButtonClickListener());
        galleryButton = (ImageButton) view.findViewById(R.id.selectButton);
        galleryButton.setOnClickListener(new GalleryClickListener());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkMultiplePermissions(REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS, getActivity());
        } else {
             // Open your camera here.
             if (isIntentPreferenceSet()) {
                handleIntentPreference();
             } else {
                getActivity().finish();
             }
        }
    }

    public static void openAlertDialog(String message, String positiveBtnText, String negativeBtnText,
                                       final OnDialogButtonClickListener listener,Context mContext) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AlertDialogCustom);
        builder.setPositiveButton(positiveBtnText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                listener.onPositiveButtonClicked();
            }
        });
        builder.setPositiveButton(positiveBtnText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                listener.onNegativeButtonClicked();
            }
        });

        builder.setTitle(mContext.getResources().getString(R.string.app_name));
        builder.setMessage(message);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(false);
        builder.create().show();
    }

    @Override
    public void onPositiveButtonClicked() {
        switch (mDialogType) {
            case ValueConstants.DialogType.DIALOG_DENY:
                checkMultiplePermissions(REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS, getActivity());
                break;
            case ValueConstants.DialogType.DIALOG_NEVER_ASK:
                redirectToAppSettings(getActivity());
                break;
        }
    }

    @Override
    public void onNegativeButtonClicked() {

    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    // Call your camera here.
                    if (isIntentPreferenceSet()) {
                        handleIntentPreference();
                    } else {
                        getActivity().finish();
                    }
                } else {
                    boolean showRationale1 = shouldShowRequestPermissionRationale(CAMERA_PERMISSION);
                    boolean showRationale2 = shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE_PERMISSION);
                    boolean showRationale3 = shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE_PERMISSION);
                    if (showRationale1 && showRationale2 && showRationale3) {
                        //explain to user why we need the permissions
                        mDialogType = ValueConstants.DialogType.DIALOG_DENY;
                        // Show dialog with
                        openAlertDialog(mRequestPermissions, mGrantPermissions, mCancel, this, getActivity());
                    } else {
                        //explain to user why we need the permissions and ask him to go to settings to enable it
                        mDialogType = ValueConstants.DialogType.DIALOG_NEVER_ASK;
                        openAlertDialog(mRequsetSettings, mGoToSettings, mCancel, this, getActivity());
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //check for camera and storage access permissions
    @TargetApi(Build.VERSION_CODES.M)
    private void checkMultiplePermissions(int permissionCode, Context context) {

        String[] PERMISSIONS = {CAMERA_PERMISSION, READ_EXTERNAL_STORAGE_PERMISSION, WRITE_EXTERNAL_STORAGE_PERMISSION};
        if (!hasPermissions(context, PERMISSIONS)) {
            requestPermissions(PERMISSIONS, permissionCode);
        } else {
            // Open your camera here.
            if (isIntentPreferenceSet()) {
                handleIntentPreference();
            } else {
                getActivity().finish();
            }
        }
    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void redirectToAppSettings(Context context) {
        String packageName = context.getPackageName();
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", packageName, null);
        intent.setData(uri);
        try {
            context.startActivity(intent);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearTempImages() {
        try {
            File tempFolder = new File(ScanConstants.IMAGE_PATH);
            for (File f : tempFolder.listFiles())
                f.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleIntentPreference() {
        int preference = getIntentPreference();
        if (preference == ScanConstants.OPEN_CAMERA) {
            openCamera();
        } else if (preference == ScanConstants.OPEN_MEDIA) {
            openMediaContent();
        }
    }

    private boolean isIntentPreferenceSet() {
        int preference = getArguments().getInt(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
        return preference != 0;
    }

    private int getIntentPreference() {
        int preference = getArguments().getInt(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
        return preference;
    }


    private class CameraButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkMultiplePermissions(REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS, getActivity());
            } else {
                // Open your camera here.
                openCamera();
            }
        }
    }

    private class GalleryClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkMultiplePermissions(REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS, getActivity());
            } else {
                // Open your media here.
                openMediaContent();
            }
        }
    }

    public void openMediaContent() {
        camorgal = 1;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, ScanConstants.PICKFILE_REQUEST_CODE);
    }

    public void openCamera() {
        camorgal = 0;
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        File file = createImageFile();
        boolean isDirectoryCreated = file.getParentFile().mkdirs();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String aut =   getActivity().getApplicationContext().getPackageName() + ".com.scanlibrary.provider"; // As defined in Manifest
            Uri tempFileUri = FileProvider.getUriForFile(getActivity().getApplicationContext(),
                    aut, // As defined in Manifest
                    file);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri);
        } else {
            Uri tempFileUri = Uri.fromFile(file);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri);
        }
        startActivityForResult(cameraIntent, ScanConstants.START_CAMERA_REQUEST_CODE);
    }

    private File createImageFile() {
        // clearTempImages();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new
                Date());
        File file = new File(ScanConstants.IMAGE_PATH, "IMG_" + timeStamp +
                ".jpg");
        fileUri = Uri.fromFile(file);
        return file;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("", "onActivityResult" + resultCode);
        Bitmap bitmap = null;
        if (resultCode == Activity.RESULT_OK) {
            try {
                switch (requestCode) {
                    case ScanConstants.START_CAMERA_REQUEST_CODE:
                        bitmap = getBitmap(fileUri);
                        break;

                    case ScanConstants.PICKFILE_REQUEST_CODE:
                        bitmap = getBitmap(data.getData());
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            getActivity().finish();
        }
        if (bitmap != null) {
            
            if ( camorgal == 0 )
            {
            //PickImageFragment.this.getActivity().getContentResolver().notifyChange(fileUri, null);
            File imageFile = new File(fileUri.getPath());
            ExifInterface exif = null;
            
            try
            {
                exif = new ExifInterface(imageFile.getAbsolutePath());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            
            int orientation = 0;
            if (exif != null)
            {
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
            
            int rotate = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }

            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(rotate);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            postImagePick(bitmap);
        }
    }

    protected void postImagePick(Bitmap bitmap) {
        Uri uri = Utils.getUri(getActivity(), bitmap);
        bitmap.recycle();
        scanner.onBitmapSelect(uri);
    }

    private Bitmap getBitmap(Uri selectedimg) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            int quality = getArguments().getInt("quality", 1);
            options.inSampleSize = quality;
        } catch (Exception e) {
            options.inSampleSize = 1;
        }
        AssetFileDescriptor fileDescriptor = null;
        fileDescriptor =
                getActivity().getContentResolver().openAssetFileDescriptor(selectedimg, "r");
        Bitmap original
                = BitmapFactory.decodeFileDescriptor(
                fileDescriptor.getFileDescriptor(), null, options);
        return original;
    }
}
