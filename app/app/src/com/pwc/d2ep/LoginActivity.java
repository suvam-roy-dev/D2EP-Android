package com.pwc.d2ep;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.text.method.CharacterPickerDialog;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.ClientManager;

public class LoginActivity extends Activity {

    private FirebaseAuth mAuth;

    private String TAG = "SF";
    private static final int REQ_ONE_TAP = 2;  // Can be any integer unique to the Activity.
    private boolean showOneTapUI = true;

    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private GoogleSignInClient mGoogleSignInClient;
    private int RC_SIGN_IN = 2512;

    TinyDB tinyDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        tinyDB = new TinyDB(this);
        if (tinyDB.getBoolean("SFIN")){
            finish();
            startActivity(new Intent(this, HostActivity.class));
//            String accountType =
//                    SalesforceSDKManager.getInstance().getAccountType();
//
//            ClientManager.LoginOptions loginOptions =
//                    SalesforceSDKManager.getInstance().getLoginOptions();
//// Get a rest client
//            if (new ClientManager(this, accountType, loginOptions,
//                    SalesforceSDKManager.getInstance().
//                            shouldLogoutWhenTokenRevoked()).getAccount() != null){
//
//            }
        }

        findViewById(R.id.bSF).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                startActivity(new Intent(getApplicationContext(),HostActivity.class));

            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null){
            //Log.d(TAG,account.getDisplayName());
            startActivity(new Intent(this, GUserActivity.class));
            finish();
        }
        //mAuth = FirebaseAuth.getInstance();
//        if mAuth.getCurrentUser(). != null{
//
//        }

//        oneTapClient = Identity.getSignInClient(this);
//        signInRequest = BeginSignInRequest.builder()
//                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
//                        .setSupported(true)
//                        // Your server's client ID, not your Android client ID.
//                        .setServerClientId(getString(R.string.default_web_client_id))
//                        // Only show accounts previously used to sign in.
//                        .setFilterByAuthorizedAccounts(false)
//                        .build())
//                // Automatically sign in when exactly one credential is retrieved.
//                .setAutoSelectEnabled(true)
//                .build();

        findViewById(R.id.bGoogle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
//                oneTapClient.beginSignIn(signInRequest)
//                        .addOnSuccessListener(LoginActivity.this, new OnSuccessListener<BeginSignInResult>() {
//                            @Override
//                            public void onSuccess(BeginSignInResult result) {
//                                try {
//                                    startIntentSenderForResult(
//                                            result.getPendingIntent().getIntentSender(), REQ_ONE_TAP,
//                                            null, 0, 0, 0);
//                                } catch (IntentSender.SendIntentException e) {
//                                    Log.e(TAG, "Couldn't start One Tap UI: " + e.getLocalizedMessage());
//                                }
//                            }
//                        })
//                        .addOnFailureListener(LoginActivity.this, new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                // No saved credentials found. Launch the One Tap sign-up flow, or
//                                // do nothing and continue presenting the signed-out UI.
//                                Log.d(TAG, e.getLocalizedMessage());
//                            }
//                        });


            }
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }

//        switch (requestCode) {
//            case REQ_ONE_TAP:
//                try {
//                    SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
//                    String idToken = credential.getGoogleIdToken();
//                    String username = credential.getDisplayName();
//                    String password = credential.getPassword();
//                    if (idToken !=  null) {
//                        // Got an ID token from Google. Use it to authenticate
//                        // with your backend.
//                        Log.d(TAG, "Got ID token.");
//                        Log.d(TAG, "User Id: "+username);
//
//                    } else if (password != null) {
//                        // Got a saved username and password. Use them to authenticate
//                        // with your backend.
//                        Log.d(TAG, "Got password.");
//                    }
//                } catch (ApiException e) {
//                    // ...
//                }
//                break;
//        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            //Log.d(TAG,account.getDisplayName());
            startActivity(new Intent(this, GUserActivity.class));

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
    }
}