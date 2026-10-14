package com.example.sariManang;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SupplierActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.supplierpage);

        LinearLayout homeBtn = findViewById(R.id.home_button);
        if (homeBtn != null) {
            homeBtn.setOnClickListener(v -> {
                Intent intent = new Intent(SupplierActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }

        CheckBox cbFavorite = findViewById(R.id.cbFavorite);
        if (cbFavorite != null) {
            cbFavorite.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    Toast.makeText(SupplierActivity.this, "Saved to favorites!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SupplierActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                }
            });
        }

        Button btnLogQuality = findViewById(R.id.btnLogQuality);
        if (btnLogQuality != null) {
            btnLogQuality.setOnClickListener(v -> showLogQualityDialog());
        }
    }

    private void showLogQualityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialoglogquality, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitRating);
        btnSubmit.setOnClickListener(v -> {
            Toast.makeText(this, "Rating Submitted!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}