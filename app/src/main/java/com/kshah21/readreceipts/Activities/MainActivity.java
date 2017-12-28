package com.kshah21.readreceipts.Activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.LineData;
import com.kshah21.readreceipts.Adapters.DrawerAdapter;
import com.kshah21.readreceipts.Bookkeeping.Expense;
import com.kshah21.readreceipts.Bookkeeping.RealmWrapper;
import com.kshah21.readreceipts.Fragments.CaptureFragment;
import com.kshah21.readreceipts.OCR.OCR;
import com.kshah21.readreceipts.R;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;


import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements DrawerAdapter.OnItemClickListener {

    private Button cameraButton;
    private Button galleryButton;
    private ImageView receiptView;
    private TextView receiptResult;

    private OCR ocr;
    private RealmWrapper realm;
    private String currentPhotoPath;

    private DrawerLayout drawerLayout;
    private RecyclerView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private CharSequence drawerTitle;
    private CharSequence mainTitle;
    private String[] listOptions;

    private final String DATA_PATH = Environment.getExternalStorageDirectory() + "/ReadReceipts";
    private final String CAM_PERMISSION="android.permission.CAMERA";
    private final String WRITE_PERMISSION="android.permission.WRITE_EXTERNAL_STORAGE";
    private final String READ_PERMISSION="android.permission.READ_EXTERNAL_STORAGE";

    private final Integer GRANTED = PackageManager.PERMISSION_GRANTED;
    private final int REQUEST_ALL_PERMS = 512;
    private final int REQUEST_TAKE_PIC = 1024;
    private final int REQUEST_PICK_PIC = 2048;


    /**
     * Inflate views and setup
     */
    protected void onCreate(Bundle savedInstanceState) {
        //Create and inflate layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Bind fields to respective GUI
        cameraButton = (Button) findViewById(R.id.camera_button);
        galleryButton = (Button) findViewById(R.id.gallery_button);
        receiptView = (ImageView) findViewById(R.id.ocr_image);
        receiptResult = (TextView) findViewById(R.id.ocr_result);

        //Drawer set-up
        drawerTitle = mainTitle = getTitle();
        listOptions = getResources().getStringArray(R.array.drawer_options);
        drawerLayout= (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (RecyclerView) findViewById(R.id.left_drawer);

        //Set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        //Improve performance by indicating the list if fixed size.
        drawerList.setHasFixedSize(true);

        //Set up the drawer's list view with items and click listener
        drawerList.setAdapter(new DrawerAdapter(listOptions,this));
        //Enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        //Set up what happens when action bar gets toggled
        drawerToggle = new ActionBarDrawerToggle(this,drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close){

            public void onDrawerClosed(View view){
                getSupportActionBar().setTitle(mainTitle);
                //Create call to onPrepareOptionsMenu()
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView){
                getSupportActionBar().setTitle(drawerTitle);
                //Create call to onPrepareOptionsMenu()
                invalidateOptionsMenu();
            }
        };

        //Add drawer listener
        drawerLayout.addDrawerListener(drawerToggle);

        //If no previous saved state, load default
        if (savedInstanceState == null) {
            //selectItem(0);
        }
    }

    /**
     * Close RealmDB
     */
    protected void onDestroy() {
        super.onDestroy();
        //realm.close(); // Remember to close Realm when done.
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
     * Inflate options menu
     */
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.drawer_menu, menu);
        return true;
    }

    /**
     * Prepare options menu called on invalidate
     */
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Handle action bar items selection
     */
    public boolean onOptionsItemSelected(MenuItem item){
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Interface for Drawer Items
     */
    @Override
    public void onClick(View view, int position) {
        selectItem(position);
    }

    /**
     * Loads fragment based upon what item in drawer is selected
     */
    private void selectItem(int pos){
        Fragment fragment = CaptureFragment.newInstance(pos);

        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.content_frame,fragment);
        transaction.commit();

        setTitle(listOptions[pos]);
        drawerLayout.closeDrawer(drawerList);
    }

    /**
     * Set the action bar's title based upon current frame or fragment
     */
    public void setTitle(CharSequence title){
        mainTitle = title;
        getSupportActionBar().setTitle(mainTitle);
    }

    /**
     * Ensure drawer's toggle is called after onCreate
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    /**
     * Ensure drawer's toggle is called on config changed
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawerToggle.onConfigurationChanged(newConfig);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
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
     * Obtain picture, set to image view, and send to ocr
     */
    private void setPic(){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds=false;
        Bitmap bitmap= BitmapFactory.decodeFile(currentPhotoPath,options);
        bitmap = fixPic(bitmap);
        receiptView.setImageBitmap(bitmap);

        ocr = new OCR(this);
        new OCR_Task().execute(bitmap);
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

            ocr = new OCR(this);
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

}//End class


