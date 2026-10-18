package com.example.sariManang;
import static android.content.ContentValues.TAG;

import static java.lang.System.in;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
  private boolean signUp = false, showPass = false;

  FirebaseFirestore db = FirebaseFirestore.getInstance();


  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);

    userIDManager userIDManager = new userIDManager(LoginActivity.this);

    if(userIDManager.getUserID() == null) {
      setContentView(R.layout.loginpage);
      EditText user = findViewById(R.id.etUser), pass = findViewById(R.id.etPassword), confirm = findViewById(R.id.etConfirm);
      TextView title = findViewById(R.id.tvTitle), btn = findViewById(R.id.btnLogin), sw = findViewById(R.id.tvSwitch);

      findViewById(R.id.ivEye).setOnClickListener(v -> {
        showPass = !showPass;
        pass.setTransformationMethod(showPass ? null : PasswordTransformationMethod.getInstance());
        pass.setSelection(pass.length());
      });
      sw.setOnClickListener(v -> {
        signUp = !signUp;
        title.setText(signUp ? "Create your account" : "Welcome!");
        btn.setText(signUp ? "SIGN UP" : "LOG IN");
        sw.setText(signUp ? "Already have an account? Log In" : "Don't have an account? Sign Up");
        user.setText("");
        pass.setText("");
        confirm.setText("");
        confirm.setVisibility(signUp ? android.view.View.VISIBLE : android.view.View.GONE);
      });
      btn.setOnClickListener(v -> {
        String u = user.getText().toString().trim(), p = pass.getText().toString();
        if (u.isEmpty()) {
          user.setError("Required");
          return;
        }
        if (p.isEmpty()) {
          pass.setError("Required");
          return;
        }



        if (signUp) {

          if (!p.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$")) {
            pass.setError("Needs 8+ chars with upper, lower, number & special char");
            return;
          }
          if (!p.equals(confirm.getText().toString())) {
            confirm.setError("Passwords don't match");
            return;
          }
          Map<String, Object> credens = new HashMap<>();
          credens.put("username", u);
          credens.put("password", p);
          String userID = db.collection("user").document().getId();
          db.collection("user").document(userID)
              .set(credens)
              .addOnSuccessListener(aVoid -> {
                Toast.makeText(LoginActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                sw.performClick();
                user.setText("");
                pass.setText("");
                confirm.setText("");
              })
              .addOnFailureListener(e -> Log.w(TAG, "Error writing document", e));

        } else {
          db.collection("user")
              .whereEqualTo("username", u)
              .whereEqualTo("password", p)
              .get()
              .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                  DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                  String documentId = document.getId();

                  Toast.makeText(LoginActivity.this, "Welcome! ID: " + documentId, Toast.LENGTH_SHORT).show();
                  Intent in = new Intent(this, MainActivity.class);
                  in.putExtra("name", u.contains("@") ? u.substring(0, u.indexOf('@')) : u);
                  getSharedPreferences("MyUserPrefs", MODE_PRIVATE)
                      .edit()
                      .putString("userid", documentId)
                      .apply();

                  userIDManager.saveUserId(documentId);

                  in = new Intent(LoginActivity.this, MainActivity.class);
                  in.putExtra("name", u.contains("@") ? u.substring(0, u.indexOf('@')) : u);
                  startActivity(in);
                  finish();

                } else {
                  Toast.makeText(LoginActivity.this, "Incorrect username or password", Toast.LENGTH_SHORT).show();
                }
              });
        }

      });
    }
    else{
      Intent in = new Intent(this, MainActivity.class);
      startActivity(in);
      finish();
    }
  }
}
