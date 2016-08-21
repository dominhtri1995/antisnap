package com.financialwhirlpool.antisnap;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jakewharton.rxbinding.view.RxView;

import java.util.HashMap;
import java.util.Map;

import rx.functions.Action1;

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase= FirebaseDatabase.getInstance().getReference();

        Typeface courgette = Typeface.createFromAsset(getAssets(), "Pacifico.ttf");
        TextView title = (TextView) findViewById(R.id.title);
        TextView welcome = (TextView) findViewById(R.id.welcome);
        TextView signin = (TextView) findViewById(R.id.signin);
        title.setTypeface(courgette);
        welcome.setTypeface(courgette);

        Button button = (Button) findViewById(R.id.register);
        final EditText emailInput = (EditText) findViewById(R.id.input_email);
        final EditText pwdInput = (EditText) findViewById(R.id.input_password);
        final EditText name = (EditText) findViewById(R.id.name);
        RadioGroup gender= (RadioGroup) findViewById(R.id.gender);
        int id = gender.getCheckedRadioButtonId();
        final RadioButton radioButton = (RadioButton) findViewById(id);

        RxView.clicks(button)
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        mAuth.createUserWithEmailAndPassword(String.valueOf(emailInput.getText()),
                                String.valueOf(pwdInput.getText()))
                                .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if(!task.isSuccessful()){
                                            Toast.makeText(SignUpActivity.this, "Sign up failed!", Toast.LENGTH_SHORT).show();
                                        }else{
                                            createUser(String.valueOf(emailInput.getText()),
                                                    String.valueOf(name.getText()),
                                                    String.valueOf(radioButton.getText()));
                                        }
                                    }
                                });
                    }
                });

        RxView.clicks(signin)
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                        startActivity(intent);
                    }
                });
        //Firebase

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
            }
        };
    }
    public void createUser(String email, String name, String gender){
        Map<String,Object> user = new HashMap<String,Object>();
        user.put("email",email);
        user.put("name",name);
        user.put("gender",gender);
        mDatabase.child("user").child(mAuth.getCurrentUser().getUid()).setValue(user);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
