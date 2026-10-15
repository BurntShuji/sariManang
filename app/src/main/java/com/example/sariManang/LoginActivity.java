package com.example.sariManang;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private boolean signUp = false, showPass = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
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
            confirm.setVisibility(signUp ? android.view.View.VISIBLE : android.view.View.GONE);
        });
        btn.setOnClickListener(v -> {
            String u = user.getText().toString().trim(), p = pass.getText().toString();
            if (u.isEmpty()) { user.setError("Required"); return; }
            if (p.isEmpty()) { pass.setError("Required"); return; }
            if (signUp) {
                if (!p.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$")) {
                    pass.setError("Needs 8+ chars with upper, lower, number & special char"); return;
                }
                if (!p.equals(confirm.getText().toString())) { confirm.setError("Passwords don't match"); return; }
            }
            Intent in = new Intent(this, MainActivity.class);
            in.putExtra("name", u.contains("@") ? u.substring(0, u.indexOf('@')) : u);
            startActivity(in);
            finish();
        });
    }
}
