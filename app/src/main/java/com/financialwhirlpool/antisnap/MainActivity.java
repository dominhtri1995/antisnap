package com.financialwhirlpool.antisnap;

import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.financialwhirlpool.antisnap.Class.Friend;
import com.financialwhirlpool.antisnap.Class.ImageMessage;
import com.financialwhirlpool.antisnap.Class.UserDb;
import com.financialwhirlpool.antisnap.Fragment.FriendRequestFragment;
import com.financialwhirlpool.antisnap.Fragment.FriendToSendFragment;
import com.financialwhirlpool.antisnap.Fragment.MainFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jakewharton.rxbinding.view.RxView;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    ImageView fullscreen;
    ImageView blackscreen;
    ImageView profile;

    private boolean observableActive;
    private boolean imageRotate;
    List<ImageMessage> imageMessageList;

    private DatabaseReference mDatabase;
    private DatabaseReference messageDatabaseListener;

    String currentPartnerUid = null;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private Realm realm;
    RealmResults<UserDb> realmResultsQuery;

    Uri selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Realm

        RealmConfiguration config = new RealmConfiguration.Builder(MainActivity.this).build();
        realm = Realm.getInstance(config);
        //Firebase
        mAuth = FirebaseAuth.getInstance();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        //Check if sign in
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d("trido", "onAuthStateChanged from Main:signed_in: " + user.getEmail());
                    messageDatabaseListener = mDatabase.child("message").child(user.getUid());


                } else {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            }
        };


        fullscreen = (ImageView) findViewById(R.id.fullscreen);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        blackscreen = (ImageView) findViewById(R.id.blackscreen);

        // Set type logo Anti
        Typeface courgette = Typeface.createFromAsset(getAssets(), "Pacifico.ttf");
        TextView title = (TextView) findViewById(R.id.toolbar_title);
        title.setTypeface(courgette);

        // Drawer Navigation
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = LayoutInflater.from(this).inflate(R.layout.nav_header_main, null);
        navigationView.addHeaderView(header);
//        MenuItem item= navigationView.getMenu().findItem(R.id.friend_request);
//        item.setIcon(R.drawable.snapchat);

        final FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            //Fragment
            Fragment fragment = new MainFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.mainFragment, fragment)
                    .commit();

            // Start service for Notification
            Intent messageService = new Intent(MainActivity.this, NewMessageService.class);
            startService(messageService);

            // Add info to Navigation header
            final TextView name = (TextView) header.findViewById(R.id.name);
            TextView email = (TextView) header.findViewById(R.id.email);
            profile = (ImageView) header.findViewById(R.id.profileImage);

            email.setText(user.getEmail());
            name.setTypeface(courgette);

            mDatabase.child("user").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String nameInput = dataSnapshot.child("name").getValue(String.class);
                    name.setText(nameInput);

                    String profileInput = dataSnapshot.child("profile").getValue(String.class);
                    if(profileInput != null) {
                        Bitmap[] bitmap = {convertStringToImage(profileInput)};
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getApplicationContext().getResources(), bitmap[0]);
                        drawable.setCircular(true);
                        profile.setImageDrawable(drawable);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

            RxView.clicks(profile)
                    .throttleFirst(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Void>() {
                        @Override
                        public void call(Void aVoid) {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(intent, 2);
                        }
                    });

        }
        // Fab
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 0);
            }
        });
        Log.i("trido", "onCreate");


    }


    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final FirebaseUser user = mAuth.getCurrentUser();

        if (requestCode == 1) {
            if (resultCode == RESULT_OK && data != null) {
                final Uri selectedImage = data.getData();

                try {
                    //Bitmap bitmapImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    //  File myFile = new File(getPath(selectedImage));
                    File oldFile = new File(getRealPathFromURI(getApplicationContext(), selectedImage));
                    ExifInterface oldExif = new ExifInterface(oldFile.getAbsolutePath());
                    String orientation = oldExif.getAttribute(ExifInterface.TAG_ORIENTATION);

                    String imagetoFirebase = storeImageToFirebase(selectedImage);
                    Log.i("trido", imagetoFirebase.substring(0, 10));
                    Map<String, Object> message = new HashMap<>();
                    message.put("imageMessage", imagetoFirebase);
                    message.put("sender", mAuth.getCurrentUser().getUid());
                    message.put("orientation", orientation);

                    mDatabase.child("message").child(currentPartnerUid).push().setValue(message, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            Toast.makeText(MainActivity.this, "Image sent", Toast.LENGTH_SHORT).show();
                            if (databaseError != null)
                                Log.i("trido", databaseError.getMessage());
                            if (requestCode == 0) {
                                String path = getRealPathFromURI(MainActivity.this, selectedImage);
                                File image = new File(path);
                                MediaScannerConnection.scanFile(MainActivity.this, new String[]{image.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                                    @Override
                                    public void onScanCompleted(String s, Uri uri) {
                                        if (uri != null) {
                                            getContentResolver().delete(uri,
                                                    null, null);
                                        }
                                    }
                                });
                                Log.i("trido", "image deleted in memory " + path);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error. Plz try again!", Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                String imagetoFirebase = storeImageToFirebase(selectedImage);
                Map<String, Object> profileMap = new HashMap<>();
                profileMap.put("profile", imagetoFirebase);
                mDatabase.child("user").child(user.getUid()).updateChildren(profileMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if (databaseError == null)
                            Toast.makeText(MainActivity.this, "Profile Image Updated", Toast.LENGTH_SHORT).show();
                        else {
                            Log.i("trido", databaseError.getMessage());
                        }
                    }
                });

                //update new profile picture
                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getApplicationContext().getResources(), bitmap);
                drawable.setCircular(true);
                profile.setImageDrawable(drawable);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (requestCode == 0 && resultCode == RESULT_OK && data != null) {
            selectedImage = data.getData();
            FriendToSendFragment fragment = new FriendToSendFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainFragment,fragment)
                    .commit();

            Log.i("trido","change fragment");
        }
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // Event bus
    @Subscribe
    public void onEvent(FriendToSendAdapter.FriendToSendClickEvent position){
        EventBus.getDefault().post(new UriEvent(selectedImage,position.message));
    }
    @Subscribe
    public void onEvent(final MainFragment.PassFriendToShow event) {
        final Friend friend = event.friend;
        currentPartnerUid = friend.getUid();
        if (friend.getImageMessageList().size() > 0) {
            String imageMessage = friend.getImageMessageList().get(0).getImageMessage();
            final Bitmap[] bitmap = {convertStringToImage(imageMessage)};

            if (imageRotate == false) {
                Matrix matrix = new Matrix();
                switch (friend.getImageMessageList().get(0).getOrientation()) {
                    case "8":
                        matrix.postRotate(270f);
                        imageRotate = true;
                        break;
                    case "6":
                        matrix.postRotate(90f);
                        imageRotate = true;
                        break;
                    case "3":
                        matrix.postRotate(180f);
                        imageRotate = true;
                        break;
                }
                bitmap[0] = Bitmap.createBitmap(bitmap[0], 0, 0, bitmap[0].getWidth(),
                        bitmap[0].getHeight(), matrix, true);
            }
            fullscreen.setVisibility(View.VISIBLE);
            blackscreen.setVisibility(View.VISIBLE);

            fullscreen.setImageBitmap(bitmap[0]);

            Observable<Long> ob = Observable.timer(10, TimeUnit.SECONDS);
            if (observableActive == false) {
                ob.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Long>() {
                            @Override
                            public void call(Long aLong) {
                                fullscreen.setVisibility(View.INVISIBLE);
                                blackscreen.setVisibility(View.INVISIBLE);
                                bitmap[0] = null;
                                // remove from local Storage
                                String keyMessage = friend.getImageMessageList().get(0).getKeyMessage();

                                realm.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        friend.getImageMessageList().get(0).deleteFromRealm();
                                    }
                                });

                                EventBus.getDefault().post(new RequestUpdateNumberOfMessageEvent(event.position));
                                //delete on Server Firebase
                                messageDatabaseListener.child(keyMessage).removeValue();
                                observableActive = false;
                                imageRotate = false;
                                Log.i("trido", String.valueOf(aLong));
                            }

                        });
                observableActive = true;
            }
        }
    }

    @Subscribe
    public void onEvent(VivzAdapter.FingerUpMessageEvent event) {
        if (fullscreen.getVisibility() == View.VISIBLE) {
            fullscreen.setVisibility(View.INVISIBLE);
            blackscreen.setVisibility(View.INVISIBLE);
            Log.i("trido", "Finger up , image gone");
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 1);
        }
    }

    @Subscribe
    public void onEvent(MainFragment.FingerUpFromParentMessageEvent event) {
        if (fullscreen.getVisibility() == View.VISIBLE) {
            fullscreen.setVisibility(View.INVISIBLE);
            blackscreen.setVisibility(View.INVISIBLE);
        }
    }

    public class RequestUpdateNumberOfMessageEvent {
        public final int position;

        public RequestUpdateNumberOfMessageEvent(int position) {
            this.position = position;
        }
    }
    public class UriEvent{
        public final Uri uri;
        public final int position;
        public UriEvent(Uri uri,int position) {
            this.uri = uri;
            this.position=position;
        }
    }

    // Store image + image handling
    private String storeImageToFirebase(Uri mCurrentPhotoUri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inSampleSize = 2; // shrink it down otherwise we will use stupid amounts of memory
        InputStream inputStream = null;


        try {
            inputStream = getContentResolver().openInputStream(mCurrentPhotoUri);
//            inputStream = new FileInputStream(mCurrentPhotoUri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] bytes = baos.toByteArray();

        String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);

        return base64Image;
    }

    public Bitmap convertStringToImage(String imageFile) {
        byte[] imageAsBytes = Base64.decode(imageFile, Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
        return bmp;
    }

    // Menu Setting + Navigation
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_camera) {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, 0);
        } else if (id == R.id.nav_gallery) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 0);
        } else if (id == R.id.friend_request) {
                Fragment fragment = new FriendRequestFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.mainFragment,fragment)
                        .commit();
        } else if (id == R.id.settings) {
        } else if (id == R.id.my_acount) {

        } else if (id == R.id.sign_out) {
            stopService(new Intent(MainActivity.this, NewMessageService.class));
            Log.i("trido", "stop service");
            FirebaseAuth.getInstance().signOut();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();

        ComponentName cn = new ComponentName(this, MainActivity.class);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(cn));

        searchView.setQueryHint("Search friend by name,email or uid");

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                    fab.setVisibility(View.INVISIBLE);
                    Log.i("trido", "focus");
                } else {
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                    fab.setVisibility(View.VISIBLE);
                    Log.i("trido", "no focus");
                }
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id","uid","name","profile"});

                //query name
                realmResultsQuery= realm.where(UserDb.class)
                        .contains("name",newText, Case.INSENSITIVE)
                        .findAll();

                //query email
                if(newText.contains("@")){
                    realmResultsQuery= realm.where(UserDb.class)
                            .contains("email",newText,Case.INSENSITIVE)
                            .findAll();
                }
                if(realmResultsQuery.size() >0) {
                    for (int i = 0; i < realmResultsQuery.size(); i++) {
                        UserDb userDb = realmResultsQuery.get(i);
                        matrixCursor.addRow(new Object[]{i,userDb.getUid(), userDb.getName(),userDb.getProfile()});
                    }
                }else{
                    matrixCursor.addRow(new Object[]{0,"","No matches",null});
                }

                SuggestionAdapter suggestionAdapter = new SuggestionAdapter(getApplicationContext(),matrixCursor,0);
                searchView.setSuggestionsAdapter(suggestionAdapter);
                return false;
            }
        });
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Map<String,String> friendRequestInfo = new HashMap<String, String>();
                friendRequestInfo.put("uid",mAuth.getCurrentUser().getUid());
                mDatabase.child("friendrequest").child(realmResultsQuery.get(position).getUid()).push().setValue(friendRequestInfo, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if(databaseError ==null){
                            Toast.makeText(MainActivity.this, "Friend request sent", Toast.LENGTH_SHORT).show();
                            MenuItemCompat.collapseActionView(menu.findItem(R.id.search));
                        }else{
                            Toast.makeText(MainActivity.this, "Request failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                Map<String,String> newFriend = new HashMap<String, String>();
                newFriend.put("uid",realmResultsQuery.get(position).getUid());
                mDatabase.child("friendlist").child(mAuth.getCurrentUser().getUid()).push().setValue(newFriend);

                return false;
            }
        });

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction()) ||Intent.ACTION_VIEW.equals(intent.getAction()) ) {
            Log.i("trido","new intent from Suggestion click");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.search) {
            //startActivity(new Intent(MainActivity.this,SearchActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    // On start stop pause
    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        mAuth.addAuthStateListener(mAuthListener);
        super.onStart();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        mAuth.removeAuthStateListener(mAuthListener);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
