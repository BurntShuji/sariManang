package com.example.sariManang;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.homepage);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout suppliersBtn = findViewById(R.id.suppliers_button);
        if (suppliersBtn != null) {
            suppliersBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SupplierActivity.class);
                startActivity(intent);
            });
        }

        Button btnNewSale = findViewById(R.id.btnNewSale);
        Button btnRecordUtang = findViewById(R.id.btnRecordUtang);
        Button btnAddStock = findViewById(R.id.btnAddStock);

        if (btnNewSale != null) {
            btnNewSale.setOnClickListener(v -> showNewSaleDialog());
        }

        if (btnRecordUtang != null) {
            btnRecordUtang.setOnClickListener(v -> showRecordUtangDialog());
        }

        if (btnAddStock != null) {
            btnAddStock.setOnClickListener(v -> showAddStockDialog());
        }
    }

    private void showNewSaleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialognewsale, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView btnClose = view.findViewById(R.id.btnCloseDialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        Button btnSubmit = view.findViewById(R.id.btnSubmitSale);
        btnSubmit.setOnClickListener(v -> {
            Toast.makeText(this, "Sale Recorded!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showRecordUtangDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogrecordutang, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView btnClose = view.findViewById(R.id.btnCloseDialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        Button btnSubmit = view.findViewById(R.id.btnSubmitUtang);
        btnSubmit.setOnClickListener(v -> {
            Toast.makeText(this, "Utang Recorded!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showAddStockDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialogaddstock, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView btnClose = view.findViewById(R.id.btnCloseDialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        Button btnSubmit = view.findViewById(R.id.btnSubmitStock);
        btnSubmit.setOnClickListener(v -> {
            Toast.makeText(this, "Stock Added!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}