package com.pwc.d2ep;

import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.pwc.d2ep.databinding.ActivityHostBinding;
import com.pwc.d2ep.databinding.NavHeaderHostBinding;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;

public class HostActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityHostBinding binding;
    TinyDB tinyDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tinyDB = new TinyDB(this);
        binding = ActivityHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        binding.bLogOut.setOnClickListener(view -> {
            tinyDB.putBoolean("SFIN",false);
            finish();
            SalesforceSDKManager.getInstance().logout(getParent(),false);
            startActivity(new Intent(this, LoginActivity.class));
        });

        String accountType =
                SalesforceSDKManager.getInstance().getAccountType();

        ClientManager.LoginOptions loginOptions =
                SalesforceSDKManager.getInstance().getLoginOptions();
// Get a rest client
        new ClientManager(HostActivity.this, accountType, loginOptions,
                false).
                getRestClient(HostActivity.this, client -> {
                    if (client == null) {

                        return;
                    }
                    // Cache the returned client
                    //client1 = client;
                    tinyDB.putBoolean("SFIN", true);
                    Log.d("2512", "authenticatedRestClient: "+client.getClientInfo().toString());
                    String userName = client.getClientInfo().displayName;
                    String userEmail = client.getClientInfo().email;
                    ((TextView)binding.navView.getHeaderView(0).findViewById(R.id.tvUserName)).setText(userName);
                    ((TextView)binding.navView.getHeaderView(0).findViewById(R.id.tvUserEMail)).setText(userEmail);

                    //sendRequest("SELECT Name,ID,Status__c,RetailerToVisit__c,VisitStartTime__c FROM Visit__c where OwnerId='"+ownerId+"'");
                }
                );
        setSupportActionBar(binding.appBarHost.toolbar);
        DrawerLayout drawer = binding.drawerLayout;

        NavigationView navigationView = binding.navView;

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_host);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    public void updateStatusBarColor(String color){// Color must be in hexadecimal fromat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(color));
        }
    }



//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.host, menu);
//        return true;
//    }

    @Override
    public boolean onSupportNavigateUp() {


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_host);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @Override
    public void onBackPressed() {


        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //HostActivity.super.onBackPressed();
                        System.exit(0);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }


//    @Override
//    public void onBackPressed() {
////        super.onBackPressed();
//
//    }
}