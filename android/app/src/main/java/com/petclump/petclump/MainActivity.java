package com.petclump.petclump;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

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
import com.petclump.petclump.models.Specie;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    // Google Sign-ins
    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient gClient;

    // Facebook Sign-ins
    private CallbackManager fbManager;

    // UI
    private TextView animalText, uidText;
    private Context c;
    private static final String TAG = "Entry Point";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setup facebook sdk
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_main);
        setupUI();
        setupGoogleLogin();
        setupFacebookLogin();
    }

    private void setupUI(){
        // Init variables
        c = getApplicationContext();
        uidText = findViewById(R.id.uidText);
        animalText = findViewById(R.id.animalText);
        Button pickButton = findViewById(R.id.button_go_upload_photo);
        Button settingsButton = findViewById(R.id.button_settings);
        FirebaseUser cUser = FirebaseAuth.getInstance().getCurrentUser();

        // Show user UID if logged in
        if (cUser != null) { uidText.setText(cUser.getUid()); }

        // Sign out button
        Button mGoogleSignOutBtn = findViewById(R.id.mGoogleSignOut);
        mGoogleSignOutBtn.setOnClickListener(v ->{
            Auth.GoogleSignInApi.signOut(gClient).setResultCallback(status ->
                uidText.setText(R.string.app_name)
            );
            FirebaseAuth.getInstance().signOut();
        });

        // Upload photo activity
        pickButton.setOnClickListener(v -> {
            Intent nextScreen = new Intent(c, UploadPhotoActivity.class);
            startActivity(nextScreen);
        });

        // Setting page activity
        settingsButton.setOnClickListener(v -> {
            Intent nextScreen = new Intent(c, UserInfoActivity.class);
            startActivity(nextScreen);
        });

        // Animal text on screen
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        for (Specie s: Specie.values()) {
                            Thread.sleep(1000);
                            runOnUiThread( ()-> animalText.setText(s.getName(c)));
                        }
                    }
                } catch (InterruptedException e) {
                    Log.d("Interrupted", "run: " + e.getMessage());
                }
            }
        };
        t.start();
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
        mGoogleSignInBtn.setOnClickListener(v -> {
            Intent doSignIn = Auth.GoogleSignInApi.getSignInIntent(gClient);
            startActivityForResult(doSignIn, RC_SIGN_IN);
        });

        OptionalPendingResult<GoogleSignInResult> oResult = Auth.GoogleSignInApi.silentSignIn(gClient);
        if (oResult.isDone()) {
            GoogleSignInResult googleSignInResult = oResult.get();
            handleSignInResult(googleSignInResult);
        } else {
            oResult.setResultCallback(this::handleSignInResult);
        }
        //FirebaseAuth.getInstance().addAuthStateListener();

    }

    /**
     * This method sets up the Facebook login button
     */
    private void setupFacebookLogin(){
        // Initialize Facebook Login button
        fbManager = CallbackManager.Factory.create();
        LoginButton loginButton = findViewById(R.id.facebook_button);
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

            this.uidText.setText(mAuth.getCurrentUser().getUid());
            Log.d(TAG, "handleFBSignInResult: current user id: " + mAuth.getCurrentUser().getUid());
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: Sign in connection failed");
    }

    // Google Sign In Method
    private void GoogleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Google Sign In Result
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // facebook callback
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        // google result
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, an error message would pop up
                Toast.makeText(MainActivity.this ,e.getMessage(), Toast.LENGTH_SHORT);
            }
        }
    }
    // Google authentication with Firebase
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG_Google_LogIn, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG_Google_LogIn, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(MainActivity.this, R.string.User_Signed_In, Toast.LENGTH_SHORT).show();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG_Google_LogIn, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, R.string.Authentication_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
