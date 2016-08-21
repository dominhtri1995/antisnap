package com.financialwhirlpool.antisnap.Fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.financialwhirlpool.antisnap.Class.Friend;
import com.financialwhirlpool.antisnap.Class.FriendRequest;
import com.financialwhirlpool.antisnap.FriendRequestAdapter;
import com.financialwhirlpool.antisnap.R;
import com.financialwhirlpool.antisnap.VivzAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * A simple {@link Fragment} subclass.
 */
public class FriendRequestFragment extends Fragment {
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private Realm realm;
    private RealmResults<FriendRequest> realmResultsFriendRequest;
    private List<Friend> friendList;
    FriendRequestAdapter adapter;
    String currentUid;

    public FriendRequestFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUid =mAuth.getCurrentUser().getUid();

        RealmConfiguration config = new RealmConfiguration.Builder(getContext()).build();
        realm = Realm.getInstance(config);

        realmResultsFriendRequest = realm.where(FriendRequest.class).findAll();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_friend_request, container, false);
        getListOfFriendRequest();

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview);
        adapter = new FriendRequestAdapter(getContext(), friendList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        VivzAdapter.DividerItemDecoration itemDecoration = new VivzAdapter.DividerItemDecoration(getContext());
        recyclerView.addItemDecoration(itemDecoration);

        return rootView;
    }

    public void getListOfFriendRequest() {
        friendList = new ArrayList<Friend>();
        for (FriendRequest friendRequest : realmResultsFriendRequest) {

            mDatabase.child("user").child(friendRequest.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Friend friend = dataSnapshot.getValue(Friend.class);
                    friendList.add(friend);
                    adapter.notifyItemInserted(friendList.size()-1);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
    @Subscribe
    public void onEvent(final FriendRequestAdapter.friendRequestAccepted requestAccepted){
        mDatabase.child("friendrequest").child(currentUid)
                .child(realmResultsFriendRequest.get(requestAccepted.message).getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                            //Add Friend
                            String newFriendUid = dataSnapshot1.getValue(String.class);
                            addFriend(newFriendUid, requestAccepted.message);
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
        //Remove on Server
        mDatabase.child("friendrequest").child(currentUid)
                .child(realmResultsFriendRequest.get(requestAccepted.message).getKey())
                .removeValue();
        // Remove on UI
        friendList.remove(requestAccepted.message);
        adapter.notifyItemRemoved(requestAccepted.message);
    }
    @Subscribe
    public void onEvent(FriendRequestAdapter.friendRequestDeclined requestDeclined){
        realm.beginTransaction();
        realmResultsFriendRequest.deleteFromRealm(requestDeclined.message);
        realm.commitTransaction();
        mDatabase.child("friendrequest").child(currentUid)
                .child(realmResultsFriendRequest.get(requestDeclined.message).getKey())
                .removeValue();

        friendList.remove(requestDeclined.message);
        adapter.notifyItemRemoved(requestDeclined.message);
    }
    public void addFriend(final String uid, final int position){
        Map<String,Object> message = new HashMap<String,Object>();
        message.put("uid",uid);
        mDatabase.child("friendlist").child(currentUid).push().setValue(message, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Toast.makeText(getContext(), "Added", Toast.LENGTH_SHORT).show();
                realm.beginTransaction();
                realmResultsFriendRequest.deleteFromRealm(position);
                realm.commitTransaction();
            }
        });
        mDatabase.child("user").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Friend friend = dataSnapshot.getValue(Friend.class);
                friend.setUid(uid);
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealm(friend);
                    }
                });
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }

}
