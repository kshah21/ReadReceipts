package com.kshah21.readreceipts;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button cameraButton;
    private TextView receiptResult;
    private TessBaseAPI tessAPI;
    private String currentPhotoPath;
    private boolean hasCamPermission = false;
    private boolean hasReadPermission = false;
    private boolean hasWritePermission = false;
    private final String DATA_PATH = Environment.getExternalStorageDirectory() + "/Tess";
    private final String TESS_DATA = "/tessdata";
    private final String CAM_PERMISSION="android.permission.CAMERA";
    private final String WRITE_PERMISSION="android.permission.WRITE_EXTERNAL_STORAGE";
    private final String READ_PERMISSION="android.permission.READ_EXTERNAL_STORAGE";
    private final Integer GRANTED = PackageManager.PERMISSION_GRANTED;

    /**
     * Inflate views and setup
     */
    protected void onCreate(Bundle savedInstanceState) {
        //Create and inflate layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Bind fields to respective GUI
        cameraButton = (Button) findViewById(R.id.camera_button);
        receiptResult = (TextView) findViewById(R.id.ocr_return);
    }

    /**
     * Check permissions
     */
    protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT>=23){
            checkPermissions();
        }
    }

    /**
     * Launches Camera Open
     */
    private void launchCamera(){
        String imgPath=DATA_PATH+"/imgs";
        File dir = new File(imgPath);
        if(!dir.exists()){
            dir.mkdirs();
        }
        //Open camera to take picture
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(cameraIntent.resolveActivity(getPackageManager())!=null){
            File photoFile=null;
            try{
                photoFile=createImageFile();
            }
            catch(IOException ex){
                //Error occurred while creating the file
            }
            if(photoFile!=null){
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(cameraIntent,1024);
            }
        }
    }

    /**
     * http://developer.android.com/training/camera/photobasics.html
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        String storageDir = Environment.getExternalStorageDirectory()
                + "/TessOCR";
        File dir = new File(storageDir);
        if (!dir.exists())
            dir.mkdir();

        File image = new File(storageDir + "/" + imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Listener for Camera Button
     */
    View.OnClickListener cameraListener= new View.OnClickListener(){

        public void onClick(View view) {
            //launchCamera();
            Toast.makeText(MainActivity.this,"Camera Button Clicked!!",Toast.LENGTH_LONG).show();
        }
    };

    /**
     * Check/Request multiple permissions
     */
    private void checkPermissions(){
        List<String> permissionList= new ArrayList<String>();
        if(ContextCompat.checkSelfPermission(this,CAM_PERMISSION)!=GRANTED){
            permissionList.add(CAM_PERMISSION);
        }
        if(ContextCompat.checkSelfPermission(this,READ_PERMISSION)!=GRANTED){
            permissionList.add(READ_PERMISSION);
        }
        if(ContextCompat.checkSelfPermission(this,WRITE_PERMISSION)!=GRANTED){
            permissionList.add(WRITE_PERMISSION);
        }
        if(!permissionList.isEmpty()){
            int size = permissionList.size();
            ActivityCompat.requestPermissions(this,permissionList.toArray(new String[size]),1024);
        }
        else{
            cameraButton.setOnClickListener(cameraListener);
        }
    }

    /**
     * Requests permissions from user and sets listener for camera use button
     * Need to figure out how to explain rationale if denied/never ask again
     */
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grantResults){
        switch(code){
            case 1024:{
                Map<String,Integer> perms = new HashMap<String,Integer>();
                perms.put(CAM_PERMISSION,GRANTED);
                perms.put(READ_PERMISSION,GRANTED);
                perms.put(WRITE_PERMISSION,GRANTED);
                if(grantResults.length>0){
                    for(int i=0;i<permissions.length;i++){
                        perms.put(permissions[i],grantResults[i]);
                    }
                    if(perms.get(CAM_PERMISSION)==GRANTED && perms.get(READ_PERMISSION)==GRANTED
                            && perms.get(WRITE_PERMISSION)==GRANTED){
                        cameraButton.setOnClickListener(cameraListener);
                    }
                    else{
                        endApp();
                        /* if(ActivityCompat.shouldShowRequestPermissionRationale(this,CAM_PERMISSION)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this,READ_PERMISSION)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this,WRITE_PERMISSION)){
                            showDialogOK("Camera, Read, and Write Permissions are required for this app. Try again.",
                                    new DialogInterface.OnMultiChoiceClickListener(){
                                        public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                            switch(i){
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    checkPermissions();
                                                    break;
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    //endApp();
                                                    break;
                                            }
                                        }
                                    });
                        }*/
                    }
                }
            }
        }
    }//End onPermissionRequest


    private void showDialogOK(String message, DialogInterface.OnMultiChoiceClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", (DialogInterface.OnClickListener) okListener)
                .setNegativeButton("Cancel", (DialogInterface.OnClickListener) okListener)
                .create()
                .show();
    }

    public void endApp() {
        if (Build.VERSION.SDK_INT >= 21) { //Is the user running Lollipop or above?
            finishAndRemoveTask(); //If yes, run the new fancy function to end the app and remove it from the Task Manager.
        } else {
            finish(); //If not, then just end the app (without removing the task completely).
        }
    }

}//End class
