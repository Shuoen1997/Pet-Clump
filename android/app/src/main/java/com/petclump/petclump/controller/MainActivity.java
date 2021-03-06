package com.petclump.petclump.controller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petclump.petclump.R;
import com.petclump.petclump.models.BaseMessage;
import com.petclump.petclump.models.Chat;
import com.petclump.petclump.models.GPS.MyService;
import com.petclump.petclump.models.MessagingDownloader;
import com.petclump.petclump.models.OwnerProfile;
import com.petclump.petclump.models.PetProfile;
import com.petclump.petclump.models.Specie;
import com.petclump.petclump.models.protocols.ProfileUploader;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    // Google Sign-ins
    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient gClient;
    // Facebook Sign-ins
    private CallbackManager fbManager;
    // UI
    private TextView uidText;
    private Context c;
    private static final String TAG = "Entry Point";
    // GPS
    private double profile_lat = 0.0, profile_lon = 0.0;
    private OwnerProfile owner = OwnerProfile.getInstance();
    private final int GPS_REQUEST_CODE = 99;
    private final int FILE_STORAGE_REQUEST_CODE = 100;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setGPS();
        setupUI();
        setFilePermission();
        setupGoogleLogin();
        setupFacebookLogin();
        checkLoggedIn();
    }

    private void checkLoggedIn(){
        if(FirebaseAuth.getInstance().getCurrentUser() != null){
            Log.d(TAG,"checkLoggedIn:"+FirebaseAuth.getInstance().getCurrentUser().getUid());
            saveGPS(()->{startActivity(new Intent(this, MatchingActivity.class));});

        }
    }
    private void setFilePermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                FILE_STORAGE_REQUEST_CODE);
    }
    private void setGPS(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                GPS_REQUEST_CODE);
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);
        Location l;
        // Go through the location providers starting with GPS, stop as soon
        // as we find one.
        try{
            for (int i=providers.size()-1; i>=0; i--) {
                l = lm.getLastKnownLocation(providers.get(i));
                profile_lat = l.getLatitude();
                profile_lon = l.getLongitude();
                //Toast.makeText(this, l.getLatitude()+","+l.getLongitude(), Toast.LENGTH_SHORT).show();
                if (l != null) break;
            }
        }catch(SecurityException e){
            e.printStackTrace();
            Log.d(TAG,"setGPS failed, permission:"+e);
        } catch (Exception ex) {
            // GPS probably failed
            ex.printStackTrace();
        }
    }

    private void setupUI(){
        // Init variables
        c = getApplicationContext();
        ContextProvider.setContext(getApplicationContext());

    }


    private void setupGoogleLogin(){

        GoogleSignInOptions gos = new GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build();

        gClient = new GoogleApiClient
            .Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gos)
            .build();

        SignInButton mGoogleSignInBtn = findViewById(R.id.googleBtn);
        Button google_button = findViewById(R.id.google_button);
        TextView sign_out = findViewById(R.id.main_sign_out);
        Intent doSignIn = Auth.GoogleSignInApi.getSignInIntent(gClient);
        google_button.setOnClickListener(v->{
            mGoogleSignInBtn.performClick();
        });
        mGoogleSignInBtn.setOnClickListener(v -> {
            startActivityForResult(doSignIn, RC_SIGN_IN);
        });
        sign_out.setOnClickListener(v->{
            Auth.GoogleSignInApi.signOut(gClient).setResultCallback(status ->{
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
                    }
            );
        });

        OptionalPendingResult<GoogleSignInResult> oResult = Auth.GoogleSignInApi.silentSignIn(gClient);
        if (oResult.isDone()) {
            GoogleSignInResult googleSignInResult = oResult.get();
            handleSignInResult(googleSignInResult);
        } else {
            oResult.setResultCallback(this::handleSignInResult);
        }
    }

    /**
     * This method sets up the Facebook login button
     */
    private void setupFacebookLogin(){
        // Initialize Facebook Login button
        fbManager = CallbackManager.Factory.create();
        LoginButton loginButton = findViewById(R.id.facebook_button_real);
        Button facebook_button = findViewById(R.id.facebook_button);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(fbManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult r) { handleSignInResult(r.getAccessToken()); }

            @Override
            public void onCancel() { Log.d(TAG, "facebook:onCancel"); }

            @Override
            public void onError(FacebookException e) { Log.d(TAG, "facebook:onError", e);}
        });

        // auto-login
        LoginManager.getInstance().registerCallback(fbManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult r) { handleSignInResult(r.getAccessToken()); }

            @Override
            public void onCancel() { Log.d(TAG, "facebook:onCancel"); }

            @Override
            public void onError(FacebookException e) { Log.d(TAG, "facebook:onError", e); }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
            return;
        }
        fbManager.onActivityResult(requestCode, resultCode, data);
    }

    private void handleSignInResult(GoogleSignInResult result){
        Log.d(TAG, "handleGoogleResult:" + result);
        if (!result.isSuccess()) { return; }
        GoogleSignInAccount account = result.getSignInAccount();
        if (account == null) { return; }
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuthWithCredential(credential);
    }

    private void handleSignInResult(AccessToken token){
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuthWithCredential(credential);
    }

    /**
     * This method gives the Firebase Authentication instance with the user
     * login credential for our app to identify the user.
     * @param credential An AuthProvider generates the credential
     */
    private void firebaseAuthWithCredential(AuthCredential credential) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {

            if (!task.isSuccessful()) {
                Log.w(TAG, "signInWithCredential:failure", task.getException());
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Log.d(TAG, "handleFBSignInResult: No current user");
                return;
            }

            saveGPS(()->{
                checkLoggedIn();
                Log.d(TAG, "handleFBSignInResult: current user id: " + mAuth.getCurrentUser().getUid());
            });
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: Sign in connection failed");
    }
    // GPS permission setup
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case GPS_REQUEST_CODE:
                // If the permissions aren't set, then return. Otherwise, proceed.
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                                , 10);
                    }
                    Log.d(TAG, "returning program");
                    return;
                }
                else{
                    // Create Intent to reference MyService, start the Service.
                    Log.d(TAG, "starting service");
                    Intent i = new Intent(this, MyService.class);
                    if(i==null)
                        Log.d(TAG, "intent null");
                    else{
                        startService(i);
                    }
                }
                break;
            case FILE_STORAGE_REQUEST_CODE:
                // If the permissions aren't set, then return. Otherwise, proceed.
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE}
                                , 11);
                    }
                    Log.d(TAG, "returning program");
                    return;
                }
                break;
            default:
                break;
        }
    }
    private void saveGPS(ProfileUploader c){
        FirebaseUser cUser = FirebaseAuth.getInstance().getCurrentUser();
        owner.download(cUser.getUid(),()->{
            owner.setLat(profile_lat);
            owner.setLon(profile_lon);
            owner.upload(cUser.getUid(),()->{
                Toast.makeText(this, "GPS change has uploaded.", Toast.LENGTH_SHORT).show();
                c.didCompleteUpload();
            });
        });
    }

}
