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
import com.kshah21.readreceipts.Fragments.OverviewFragment;
import com.kshah21.readreceipts.Fragments.ReceiptsFragment;
import com.kshah21.readreceipts.Fragments.ReportsFragment;
import com.kshah21.readreceipts.Interfaces.FragCommunicator;
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

public class MainActivity extends AppCompatActivity implements DrawerAdapter.OnDrawerItemClickListener, FragCommunicator{

    private DrawerLayout drawerLayout;
    private RecyclerView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private CharSequence drawerTitle;
    private CharSequence mainTitle;
    private String[] listOptions;

    private final String CAM_PERMISSION="android.permission.CAMERA";
    private final String WRITE_PERMISSION="android.permission.WRITE_EXTERNAL_STORAGE";
    private final String READ_PERMISSION="android.permission.READ_EXTERNAL_STORAGE";

    private final Integer GRANTED = PackageManager.PERMISSION_GRANTED;
    private final int REQUEST_ALL_PERMS = 512;

    private boolean permissionsGranted;

    /**
     * Inflate views and setup
     */
    protected void onCreate(Bundle savedInstanceState) {
        //Create and inflate layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            selectItem(0);
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
    public void onDrawerClick(View view, int position) {
        selectItem(position);
    }

    /**
     * Loads fragment based upon what item in drawer is selected
     */
    private void selectItem(int pos){
        Fragment fragment;
        if(pos==0){
            fragment = OverviewFragment.newInstance(pos);
        }
        else if(pos==1){
            fragment = CaptureFragment.newInstance(pos);
        }
        else if(pos==2){
            fragment = ReportsFragment.newInstance(pos);
        }
        else{
            fragment = ReceiptsFragment.newInstance(pos);
        }

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


    public boolean getPermissionStatus(){
        return permissionsGranted;
    }

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
            permissionsGranted = true;
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
                        permissionsGranted = true;
                    }
                    else{
                    }
                }
            }
        }
    }//End onPermissionRequest

}//End class


