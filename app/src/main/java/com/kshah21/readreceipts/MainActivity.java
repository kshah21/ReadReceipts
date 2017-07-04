package com.kshah21.readreceipts;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContentResolverCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private Button cameraButton;
    private Button galleryButton;
    private ImageView receiptView;
    private TextView receiptResult;
    private TessBaseAPI tessAPI;
    private String currentPhotoPath;
    private Context context;

    private final String TESS_DATA = "/tessdata";
    private final String DATA_PATH = Environment.getExternalStorageDirectory() + "/Receipts";
    private final String CAM_PERMISSION="android.permission.CAMERA";
    private final String WRITE_PERMISSION="android.permission.WRITE_EXTERNAL_STORAGE";
    private final String READ_PERMISSION="android.permission.READ_EXTERNAL_STORAGE";

    private final Integer GRANTED = PackageManager.PERMISSION_GRANTED;
    private final int REQUEST_ALL_PERMS = 512;
    private final int REQUEST_TAKE_PIC = 1024;
    private final int REQUEST_PICK_PIC = 2048;

    private final String TAG = "MainActivity.java";
    private String result;

    /**
     * Inflate views and setup
     */
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("On Create");
        //Create and inflate layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Bind fields to respective GUI
        cameraButton = (Button) findViewById(R.id.camera_button);
        galleryButton = (Button) findViewById(R.id.gallery_button);
        receiptView = (ImageView) findViewById(R.id.ocr_image);
        receiptResult = (TextView) findViewById(R.id.ocr_result);
        context = this;
    }

    /**
     * Check permissions
     */
    protected void onResume(){
        System.out.println("On Resume");
        super.onResume();
        if(Build.VERSION.SDK_INT>=23){
            checkPermissions();
        }
    }

    /**
     * Launches Camera Open
     */
    private void launchCamera(){
        System.out.println("Launch Camera");
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
                Uri photoURI;
                if(Build.VERSION.SDK_INT>=24){
                    photoURI=FileProvider.getUriForFile(this,"com.kshah21.readreceipts.provider",photoFile);
                }
                else{
                    photoURI = Uri.fromFile(photoFile);
                }
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent,REQUEST_TAKE_PIC);
            }
        }
    }

    /**
     * Launches Gallery Open
     */
    private void launchGallery(){
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,REQUEST_PICK_PIC);
    }

    /**
     * Called after returning from Pictures Intents
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        System.out.println("On Result");
        switch (requestCode){
            case REQUEST_TAKE_PIC:{
                if(resultCode== Activity.RESULT_OK){
                    setPic();
                }
                break;
            }
            case REQUEST_PICK_PIC:{
                if(resultCode==Activity.RESULT_OK){
                    Uri imageUri = data.getData();
                    setPic(imageUri);
                }
            }
        }
    }

    /**
     * http://developer.android.com/training/camera/photobasics.html
     */
    private File createImageFile() throws IOException {
        System.out.println("Create Image File");
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        String storageDir = DATA_PATH+"/imgs";
        File dir = new File(storageDir);
        if (!dir.exists())
            dir.mkdir();

        File image = new File(storageDir + "/" + imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        System.out.println(currentPhotoPath);
        return image;
    }

    /**
     * Obtain picture, set to image view, and send to ocr
     */
    private void setPic(){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds=false;
        Bitmap bitmap= BitmapFactory.decodeFile(currentPhotoPath,options);
        bitmap = fixPic(bitmap);
        receiptView.setImageBitmap(bitmap);
        setUpTess();
        OCR(bitmap);
    }

    /**
     * Obtain picture, set to image view, and send to ocr
     */
    private void setPic(Uri imageUri){
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            bitmap.copy(Bitmap.Config.ARGB_8888,true);
            bitmap.setDensity(300);
            receiptView.setImageBitmap(bitmap);
            setUpTess();
            OCR(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Rotates iamge taken by camera if necessary
     */
    private Bitmap fixPic(Bitmap bitmap){
        try {
            ExifInterface exif = new ExifInterface(currentPhotoPath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
            int rotate = 0;
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate=90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate=180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate=270;
                    break;
            }
            System.out.println(rotate);
            if(rotate!=0){
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                Matrix matrix = new Matrix();
                matrix.preRotate(rotate);
                bitmap = Bitmap.createBitmap(bitmap,0,0,width,height,matrix,true);
            }
            return bitmap.copy(Bitmap.Config.ARGB_8888,true);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Copies over tess training data onto phone memory
     */
    private void setUpTess(){
        System.out.println("Set Up Tess");
        try{
            File dir = new File(DATA_PATH + TESS_DATA);
            if(!dir.exists()){
                dir.mkdir();
            }
            String fileList[] = getAssets().list("");
            for(String fileName : fileList){
                String pathToDataFile = DATA_PATH+TESS_DATA+"/"+fileName;
                if(!(new File(pathToDataFile)).exists()){
                    InputStream in = getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte [] buff = new byte[1024];
                    int len ;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Processes provided bitmap and produces text equivalent
     */
    private void OCR(final Bitmap bitmap){
        System.out.println("OCR");
        new OCR_Task().execute(bitmap);

    }

    /**
     * Checks OCR Result for varied regexs which
     * represent Total Amount Spent
     */
    private void obtainTotal(){
        //Will not work if total is preceded by a number
        //Seconday search should be for subtotal + tax!
        //Look for Tender
        //String regex = "(?<![\\w\\d])(?i)total(?![\\w\\d])";
        String regex = "(?<![\\w\\d])(?i)(total|tender)(\\s{0,3})(\\${0,1})(\\d{1,})(\\.{0,1})(\\d{0,2})";
        Pattern pattern = Pattern.compile(regex);
        Matcher match = pattern.matcher(result);

        boolean foundTotal = match.find();
        if(!foundTotal){
            System.out.println("Total Not Found");
            return;
        }
        int index = match.start();
        String total = match.group();
        System.out.println(total);
        receiptResult.setText(total);


    }


    /****************************Utility Functions****************************/

    /**
     * Listener for Camera Button
     */
    View.OnClickListener cameraListener= new View.OnClickListener(){

        public void onClick(View view) {
            Toast.makeText(MainActivity.this,"Camera Button Clicked!!",Toast.LENGTH_LONG).show();
            launchCamera();
        }
    };

    /**
     * Listener for Gallery Button
     */
    View.OnClickListener galleryListener= new View.OnClickListener(){

        public void onClick(View view) {
            Toast.makeText(MainActivity.this,"Camera Button Clicked!!",Toast.LENGTH_LONG).show();
            launchGallery();
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
            ActivityCompat.requestPermissions(this,permissionList.toArray(new String[size]),REQUEST_ALL_PERMS);
        }
        else{
            cameraButton.setOnClickListener(cameraListener);
            galleryButton.setOnClickListener(galleryListener);

        }
    }

    /**
     * Requests permissions from user and sets listener for camera use button
     * Need to figure out how to explain rationale if denied/never ask again
     */
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grantResults)
    {
        switch(code){
            case REQUEST_ALL_PERMS:{
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
                        galleryButton.setOnClickListener(galleryListener);
                    }
                    else{
                    }
                }
            }
        }
    }//End onPermissionRequest

    /**
     * AsyncThread Class which handles OCR processing
     */
    private class OCR_Task extends AsyncTask<Bitmap,String,String> {
        @Override
        protected void onPreExecute() {
            receiptResult.setText("Obtained Image");
        }

        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            String upLetters = "ACBDEFGHIJKLMNOPQRSTUVWXYZ";
            String downLetters="acbcdefghijklmnopqrstuvwxyz";
            String numbers="0123456789";
            String symbols="=*$.- ";
            String whitelist = upLetters + downLetters + numbers + symbols;
            tessAPI = new TessBaseAPI();
            tessAPI.init(DATA_PATH,"eng");
            tessAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,whitelist);
            tessAPI.setImage(bitmaps[0]);
            publishProgress("Processing Image");
            result = tessAPI.getUTF8Text();
            tessAPI.end();
            return result;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            receiptResult.setText(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            receiptResult.setText(result);
            obtainTotal();
        }
    }

}//End class


