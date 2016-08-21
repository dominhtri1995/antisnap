package com.financialwhirlpool.antisnap.Fragment;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.financialwhirlpool.antisnap.Class.Friend;
import com.financialwhirlpool.antisnap.Class.FriendRequest;
import com.financialwhirlpool.antisnap.Class.ImageMessage;
import com.financialwhirlpool.antisnap.Class.UserDb;
import com.financialwhirlpool.antisnap.MainActivity;
import com.financialwhirlpool.antisnap.R;
import com.financialwhirlpool.antisnap.VivzAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment {

    List<Friend> friendList;
    VivzAdapter adapter;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private View snackBarView;

    Realm realm;
    RealmResults<Friend> realmQueryFriend;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        RealmConfiguration config = new RealmConfiguration.Builder(getContext()).build();
        realm = Realm.getInstance(config);

        realmQueryFriend = realm.where(Friend.class).findAll();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        //get preferenced User
        FirebaseUser user = mAuth.getCurrentUser();
        SharedPreferences userUidSharedPreference = getActivity().getPreferences(Context.MODE_PRIVATE);
        String userUid = userUidSharedPreference.getString("uid", null);
        //Load data from Realm if possible
        if (user != null && user.getUid().equals(userUid) && realmQueryFriend.size() > 0) {
            Log.i("trido", "current Friend db size " + realmQueryFriend.size());
            friendList = realmQueryFriend;
            adapter = new VivzAdapter(getContext(), friendList);
            getFriendList();
            getFriendRequest();
        } else if (user != null) {
            realm.beginTransaction();
            realm.delete(Friend.class);
            realm.commitTransaction();
            getFriendList();
            adapter = new VivzAdapter(getContext(), friendList);

            //update SharedPreference userUid
            SharedPreferences.Editor editor = userUidSharedPreference.edit();
            editor.putString("uid", user.getUid());
            editor.commit();
            Log.i("trido", "new Sharepreference uid" + userUidSharedPreference.getString("uid", ""));

            //getFriendRequest
            getFriendRequest();
        }
        //Recycler view
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.friendlist);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        VivzAdapter.DividerItemDecoration itemDecoration = new VivzAdapter.DividerItemDecoration(getContext());
        recyclerView.addItemDecoration(itemDecoration);

        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getActionMasked()) {
                    case MotionEvent.ACTION_UP:
                        Log.i("trido", "Finger up in Recycler Parent");
                        EventBus.getDefault().post(new FingerUpFromParentMessageEvent(""));
                        return false;
                }
                return false;
            }
        });
        snackBarView = rootView;
        return rootView;
    }

    public void getUserList() {
        mDatabase.child("user").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final UserDb userDb = dataSnapshot.getValue(UserDb.class);
                userDb.setUid(dataSnapshot.getKey());

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealmOrUpdate(userDb);
                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getFriendList() {
        friendList = new ArrayList<Friend>();
        final FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Observable<Friend> ob = Observable.create(new Observable.OnSubscribe<Friend>() {
                @Override
                public void call(final Subscriber<? super Friend> subscriber) {
                    mDatabase.child("friendlist").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            final long friendCount = dataSnapshot.getChildrenCount();
                            for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                                final String friendUid = dataSnapshot1.getValue(Friend.class).getUid();
                                mDatabase.child("user").child(friendUid).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(final DataSnapshot dataSnapshot) {
                                        final Friend friend = dataSnapshot.getValue(Friend.class);
                                        friend.setUid(friendUid);

                                        Friend friend1 = realm.where(Friend.class)
                                                .equalTo("uid", friendUid)
                                                .findFirst();
                                        if (friend1 == null) {
                                            realm.executeTransaction(new Realm.Transaction() {
                                                @Override
                                                public void execute(Realm realm) {
                                                    Friend friend1 = realm.copyToRealmOrUpdate(friend);
                                                    Log.i("trido", "get friend " + friend.getName());
                                                    friendList.add(friend1);

                                                }
                                            });
                                        }
                                        if (friendList.size() == friendCount)
                                            subscriber.onCompleted();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            });
            ob.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Friend>() {
                        @Override
                        public void onCompleted() {
                            adapter.notifyDataSetChanged();
                            getMessage();
                            getFriendRequest();
                        }

                        @Override
                        public void onError(Throwable e) {
                        }

                        @Override
                        public void onNext(Friend friend) {
                        }
                    });
        }
    }

    public void getMessage() {
        final FirebaseUser user = mAuth.getCurrentUser();
        mDatabase.child("message").child(user.getUid()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                ImageMessage imageMessage = dataSnapshot.getValue(ImageMessage.class);
                imageMessage.setKeyMessage(dataSnapshot.getKey());

                RealmResults<ImageMessage> queryMessageOld = realm.where(ImageMessage.class).findAll();
                realm.beginTransaction();
                ImageMessage im = realm.copyToRealmOrUpdate(imageMessage);
                RealmResults<ImageMessage> queryMessageNew = realm.where(ImageMessage.class).findAll();
                if (queryMessageOld.size() < queryMessageNew.size()) {
                    for (Friend friend : friendList) {
                        if (friend.getUid().equals(imageMessage.getSender())) {

                            friend.getImageMessageList().add(im);
                            friend.setHasMessage(true);
                            Log.i("trido", "add new message");

                            Log.i("trido", friend.getName() + "has " + friend.getImageMessageList().size());
                            adapter.notifyItemChanged(friendList.indexOf(friend));
                            Snackbar.make(snackBarView, "New Message from " + friend.getName(), Snackbar.LENGTH_SHORT).show();
                        } else {
                            Log.i("trido", "sender not found");
                        }
                    }
                }
                realm.commitTransaction();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                ImageMessage imageMessageDeleted = dataSnapshot.getValue(ImageMessage.class);

                for (Friend friend : friendList) {
                    if (friend.getUid().equals(imageMessageDeleted.getSender()) && friend.getImageMessageList().size() == 0) {
                        realm.beginTransaction();
                        friend.setHasMessage(false);
                        realm.commitTransaction();
                        adapter.notifyItemChanged(friendList.indexOf(friend));
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        getUserList();
    }

    public void getFriendRequest() {
        // Get FriendRequest
        mDatabase.child("friendrequest").child(mAuth.getCurrentUser().getUid()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                FriendRequest friendRequest = dataSnapshot.getValue(FriendRequest.class);
                friendRequest.setKey(dataSnapshot.getKey());
                RealmResults<FriendRequest> requestRealmResults = realm.where(FriendRequest.class).findAll();
                for (FriendRequest friendRequest1 : requestRealmResults) {
                    if (friendRequest.getUid().equals(friendRequest1.getUid())) {
                        return;
                    }
                }
                realm.beginTransaction();
                realm.copyToRealm(friendRequest);
                realm.commitTransaction();

                requestRealmResults = realm.where(FriendRequest.class).findAll();
                Snackbar.make(snackBarView, requestRealmResults.size() +
                        " New Friend Request", Snackbar.LENGTH_SHORT).show();
                requestRealmResults.addChangeListener(new RealmChangeListener<RealmResults<FriendRequest>>() {
                    @Override
                    public void onChange(RealmResults<FriendRequest> element) {
                        EventBus.getDefault().post(new MessageEvent("new friend request"));
                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Subscribe
    public void onEvent(VivzAdapter.FingerDownMessageEvent event) {
        int positionClicked = event.message;
        Friend friendToShow = friendList.get(positionClicked);
        EventBus.getDefault().post(new PassFriendToShow(friendToShow, positionClicked));
    }

    @Subscribe
    public void onEvent(MainActivity.RequestUpdateNumberOfMessageEvent messageEvent) {
        adapter.notifyItemChanged(messageEvent.position);
    }

    public class MessageEvent {
        public final String message;

        public MessageEvent(String message) {
            this.message = message;
        }
    }

    public class PassFriendToShow {
        public final Friend friend;
        public final int position;

        public PassFriendToShow(Friend friend, int position) {
            this.friend = friend;
            this.position = position;
        }
    }

    public class FingerUpFromParentMessageEvent {
        public final String messeage;

        public FingerUpFromParentMessageEvent(String messeage) {
            this.messeage = messeage;
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
