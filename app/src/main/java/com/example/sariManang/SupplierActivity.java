package com.example.sariManang;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SupplierActivity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private LinearLayout supplierListContainer;
    private int supplierCount = 1;

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

        supplierListContainer = findViewById(R.id.supplierListContainer);

        TextView tvAddSupplier = findViewById(R.id.tvAddSupplier);
        if (tvAddSupplier != null) {
            tvAddSupplier.setOnClickListener(v -> addNewSupplierCard("Supplier #" + supplierCount++));
        }
    }

    private void addNewSupplierCard(String name) {
        if (supplierListContainer == null) return;

        View cardView = getLayoutInflater().inflate(R.layout.itemsuppliercard, supplierListContainer, false);

        TextView tvName = cardView.findViewById(R.id.tvSupplierName);
        if (tvName != null) {
            tvName.setText(name);
        }

        CheckBox cbFavorite = cardView.findViewById(R.id.cbFavorite);
        if (cbFavorite != null) {
            cbFavorite.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String status = isChecked ? "Saved to favorites!" : "Removed from favorites";
                Toast.makeText(this, name + ": " + status, Toast.LENGTH_SHORT).show();
            });
        }

        Button btnLogQuality = cardView.findViewById(R.id.btnLogQuality);
        if (btnLogQuality != null) {
            btnLogQuality.setOnClickListener(v -> showLogQualityDialog(name));


        }

        supplierListContainer.addView(cardView);
        Toast.makeText(this, name + " added!", Toast.LENGTH_SHORT).show();
    }

    private void showLogQualityDialog(String supplierName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialoglogquality, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        if (tvTitle != null) {
            tvTitle.setText(supplierName);
        }

        TextView btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        EditText etGoodSupplies = findViewById(R.id.etGoodSupplies);
        EditText etBadSupplies = findViewById(R.id.etBadSupplies);
        EditText etComment = findViewById(R.id.etComment);



        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitRating);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                int goodSupplies = Integer.parseInt(etGoodSupplies.getText().toString());
                int badSupplies = Integer.parseInt(etBadSupplies.getText().toString());
                String comment = etComment.getText().toString();

                Map<String, Object> supplier = new HashMap<>();
                supplier.put("CompanyName", supplierName);
                supplier.put("GoodSupplies", goodSupplies);
                supplier.put("BadSupplies", badSupplies);
                supplier.put("Comment", comment);


                Toast.makeText(this, "Rating Submitted for " + supplierName, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}