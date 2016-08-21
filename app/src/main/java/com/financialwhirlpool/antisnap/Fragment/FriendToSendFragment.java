package com.financialwhirlpool.antisnap.Fragment;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.financialwhirlpool.antisnap.Class.Friend;
import com.financialwhirlpool.antisnap.FriendToSendAdapter;
import com.financialwhirlpool.antisnap.MainActivity;
import com.financialwhirlpool.antisnap.R;
import com.financialwhirlpool.antisnap.VivzAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by an vo on 8/10/2016.
 */
public class FriendToSendFragment extends Fragment{
    private Uri uriImage;
    private List<Friend> friendlist;

    Realm realm;
    RealmResults<Friend> realmQueryFriend;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    public FriendToSendFragment(){

    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RealmConfiguration config = new RealmConfiguration.Builder(getContext()).build();
        realm = Realm.getInstance(config);

        mDatabase= FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        realmQueryFriend = realm.where(Friend.class).findAll();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friend_to_send, container, false);

        friendlist=realmQueryFriend;
        FriendToSendAdapter adapter = new FriendToSendAdapter(getContext(),friendlist);
        RecyclerView recyclerView= (RecyclerView) rootView.findViewById(R.id.recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        VivzAdapter.DividerItemDecoration itemDecoration = new VivzAdapter.DividerItemDecoration(getContext());
        recyclerView.addItemDecoration(itemDecoration);
        return rootView;
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
    // Store image + image handling
    private String storeImageToFirebase(Uri mCurrentPhotoUri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inSampleSize = 2; // shrink it down otherwise we will use stupid amounts of memory
        InputStream inputStream = null;


        try {
            inputStream = getActivity().getContentResolver().openInputStream(mCurrentPhotoUri);
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
    @Subscribe
    public void onEvent(MainActivity.UriEvent uri) {
        uriImage =uri.uri;
        Log.i("trido","uri all set");
        try {
            //Bitmap bitmapImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
            //  File myFile = new File(getPath(selectedImage));
            File oldFile = new File(getRealPathFromURI(getContext(), uriImage));
            ExifInterface oldExif = new ExifInterface(oldFile.getAbsolutePath());
            String orientation = oldExif.getAttribute(ExifInterface.TAG_ORIENTATION);
            String imagetoFirebase = storeImageToFirebase(uriImage);
            Log.i("trido", imagetoFirebase.substring(0, 10));
            Map<String, Object> message = new HashMap<>();
            message.put("imageMessage", imagetoFirebase);
            message.put("sender", mAuth.getCurrentUser().getUid());
            message.put("orientation", orientation);

            mDatabase.child("message").child(friendlist.get(uri.position).getUid()).push().setValue(message, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    Toast.makeText(getContext(), "Image sent", Toast.LENGTH_SHORT).show();
                    if (databaseError != null)
                        Log.i("trido", databaseError.getMessage());
                    String path = getRealPathFromURI(getContext(), uriImage);
                    File image = new File(path);
                    MediaScannerConnection.scanFile(getContext(), new String[]{image.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String s, Uri uri) {
                            if (uri != null) {
                                getActivity().getContentResolver().delete(uri,
                                        null, null);
                            }
                        }
                    });
                   // Change back to Main Fragment
                    MainFragment fragment = new MainFragment();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.mainFragment,fragment)
                            .commit();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error. Plz try again!", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
}
