package com.kshah21.readreceipts.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kshah21.readreceipts.Activities.MainActivity;
import com.kshah21.readreceipts.Bookkeeping.RealmWrapper;
import com.kshah21.readreceipts.OCR.OCR;
import com.kshah21.readreceipts.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kunal on 12/28/17.
 */

public class CaptureFragment extends Fragment {

    private Button cameraButton;
    private Button galleryButton;
    private ImageView receiptView;
    private TextView receiptResult;

    private OCR ocr;
    private RealmWrapper realm;
    private String currentPhotoPath;

    private final String DATA_PATH = Environment.getExternalStorageDirectory() + "/ReadReceipts";
    public static final String OPTION_NUMBER = "option_number";

    private final int REQUEST_TAKE_PIC = 1024;
    private final int REQUEST_PICK_PIC = 2048;


    public CaptureFragment(){

    }

    public static Fragment newInstance(int position){
        Fragment fragment = new CaptureFragment();
        Bundle args = new Bundle();
        args.putInt(OPTION_NUMBER,position);
        fragment.setArguments(args);
        return fragment;
    }

    public void onAttach(Context activity){
        super.onAttach(activity);
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.capture_fragment, container, false);

        int pos = getArguments().getInt(OPTION_NUMBER);
        String option = getResources().getStringArray(R.array.drawer_options)[pos];

        cameraButton = (Button) rootView.findViewById(R.id.camera_button);
        galleryButton = (Button) rootView.findViewById(R.id.gallery_button);
        receiptView = (ImageView) rootView.findViewById(R.id.ocr_image);
        receiptResult = (TextView) rootView.findViewById(R.id.ocr_result);

        getActivity().setTitle(option);
        return rootView;
    }

    public void onActivityCreated(Bundle savedState){
        super.onActivityCreated(savedState);
    }

    public void onStart() {
        super.onStart();
    }

    public void onResume() {
        super.onResume();
        if(((MainActivity)getActivity()).getPermissionStatus()){
            cameraButton.setOnClickListener(cameraListener);
            galleryButton.setOnClickListener(galleryListener);
        }
    }

    public void onPause() {
        super.onPause();
    }

    public void onStop() {
        super.onStop();
    }

    public void onDestroyView() {
        super.onDestroyView();
        cameraButton = null;
        galleryButton = null;
        receiptView = null;
        receiptResult = null;
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void onDetach() {
        super.onDetach();
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
        if(cameraIntent.resolveActivity(getActivity().getPackageManager())!=null){
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
                    photoURI= FileProvider.getUriForFile(getActivity(),"com.kshah21.readreceipts.provider",photoFile);
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
     * Called after returning from Pictures Intents
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case REQUEST_TAKE_PIC:{
                if(resultCode== Activity.RESULT_OK){
                    setPic();
                    Toast.makeText(getActivity(),"TOOK PICTURE",Toast.LENGTH_LONG).show();

                }
                break;
            }
            case REQUEST_PICK_PIC:{
                if(resultCode==Activity.RESULT_OK){
                    Uri imageUri = data.getData();
                    setPic(imageUri);
                    Toast.makeText(getActivity(),"PICKED PICTURE",Toast.LENGTH_LONG).show();

                }
            }
        }
    }

    /**
     * Obtain picture, set to image view, and send to ocr
     */
    private void setPic(){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds=false;
        Bitmap bitmap= BitmapFactory.decodeFile(currentPhotoPath,options);
        bitmap = fixPic(bitmap);
        //receiptView.setImageBitmap(bitmap);

        ocr = new OCR(getActivity());
        new OCR_Task().execute(bitmap);
    }

    /**
     * Obtain picture, set to image view, and send to ocr
     */
    private void setPic(Uri imageUri){
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
            bitmap.copy(Bitmap.Config.ARGB_8888,true);
            bitmap.setDensity(300);
            //receiptView.setImageBitmap(bitmap);

            ocr = new OCR(getActivity());
            new OCR_Task().execute(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Rotates image taken by camera if necessary
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


    View.OnClickListener cameraListener= new View.OnClickListener(){

        public void onClick(View view) {
            Toast.makeText(getActivity(),"Camera Button Clicked!!",Toast.LENGTH_LONG).show();
            launchCamera();
        }
    };

    View.OnClickListener galleryListener= new View.OnClickListener(){

        public void onClick(View view) {
            Toast.makeText(getActivity(),"Camera Button Clicked!!",Toast.LENGTH_LONG).show();
            launchGallery();
        }
    };


    private class OCR_Task extends AsyncTask<Bitmap,String,String> {
        @Override
        protected void onPreExecute() {
            receiptResult.setText("Obtained Image");
        }

        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            publishProgress("Processing Image");
            return ocr.doOCR(bitmaps[0]);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            receiptResult.setText(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if(result!=null){
                receiptResult.setText(result);
                realm.createExpense(result);
                receiptResult.setText(realm.queryExpense());
            }
            else{
                receiptResult.setText("OCR FAILED");
            }
        }
    }




}
