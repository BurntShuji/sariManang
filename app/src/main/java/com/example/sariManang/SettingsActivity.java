package com.example.sariManang;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * SettingsActivity handles user account management, store profile configurations,
 * notification reminder toggles, data archiving, and logout actions.
 */
public class SettingsActivity extends AppCompatActivity {

  public static final String PREFS = "sari_settings";
  public static final String KEY_LOW_STOCK = "low_stock_default";
  public static final int DEFAULT_LOW_STOCK = 5;

  public static int getLowStockDefault(Context c) {
    return c.getSharedPreferences(PREFS, MODE_PRIVATE)
        .getInt(KEY_LOW_STOCK, DEFAULT_LOW_STOCK);
  }

    private String accountName = "Manang Owner";
    private String accountEmail = "owner@sarimanang.ph";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.settingspage);

        // Section 1: Toolbar & Navigation setup
        NavigationHelper.setupToolbar(this, "Settings");
        NavigationHelper.setup(this, R.id.navSettings);

        // Section 2: User Account section setup & edit listener
        TextView tvName = findViewById(R.id.tvAccountName);
        TextView tvEmail = findViewById(R.id.tvAccountEmail);
        TextView tvAvatar = findViewById(R.id.tvAccountAvatar);

        if (tvName != null) tvName.setText(accountName);
        if (tvEmail != null) tvEmail.setText(accountEmail);

        TextView btnEdit = findViewById(R.id.btnEditAccount);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> showEditAccountDialog());
        }

        // Section 3: Data Archiving listener
        findViewById(R.id.btnArchive).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Archive old records?")
                .setMessage("Records older than 6 months will be moved to the archive. Estimates will then use recent sales only.")
                .setPositiveButton("Archive", (d, w) -> Toast.makeText(this, "Archived", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Keep", null)
                .show());

        // Section 4: Logout listener
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Intent in = new Intent(this, LoginActivity.class);
            userIDManager userIDManager = new userIDManager(SettingsActivity.this);
            userIDManager.logout();
            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(in);
        });


      TextView btnLowStock = findViewById(R.id.etLowStock);
      if (btnLowStock != null) {
        btnLowStock.setText("Default low-stock alert: " + getLowStockDefault(this) + " units");
        btnLowStock.setOnClickListener(v -> showLowStockDialog(btnLowStock));
      }

      View adminBtn = findViewById(R.id.btnAdmin);
      adminBtn.setVisibility(View.GONE);
      new dbManager().getFromUsers(new userIDManager(this).getUserID(), "username")
          .addOnSuccessListener(name -> {
            if ("Onionn_".equals(name)) adminBtn.setVisibility(View.VISIBLE);
          });

      // Section 5: Admin button
      findViewById(R.id.btnAdmin).setOnClickListener(v ->
          startActivity(new Intent(this, AdminActivity.class)));
    }

  private void showLowStockDialog(TextView label) {
    EditText et = new EditText(this);
    et.setInputType(InputType.TYPE_CLASS_NUMBER);
    et.setText(String.valueOf(getLowStockDefault(this)));

    new AlertDialog.Builder(this)
        .setTitle("Default low-stock alert")
        .setMessage("Items turn red when their quantity is at or below this number.")
        .setView(et)
        .setPositiveButton("Save", (d, w) -> {
          try {
            int v = Integer.parseInt(et.getText().toString().trim());
            getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putInt(KEY_LOW_STOCK, v).apply();
            label.setText("Default low-stock alert: " + v + " units");
          } catch (NumberFormatException ex) {
            Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show();
          }
        })
        .setNegativeButton("Cancel", null)
        .show();
  }

    /** Displays a dialog popup to edit account user name and email. */
    private void showEditAccountDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 20);

        final EditText etName = new EditText(this);
        etName.setHint("Name");
        etName.setText(accountName);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("Email");
        etEmail.setText(accountEmail);

        layout.addView(etName);
        layout.addView(etEmail);

        new AlertDialog.Builder(this)
                .setTitle("Edit Account Profile")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newEmail = etEmail.getText().toString().trim();

                    if (!newName.isEmpty()) {
                        accountName = newName;
                        TextView tvName = findViewById(R.id.tvAccountName);
                        TextView tvAvatar = findViewById(R.id.tvAccountAvatar);
                        if (tvName != null) tvName.setText(accountName);
                        if (tvAvatar != null && !accountName.isEmpty()) {
                            tvAvatar.setText(accountName.substring(0, 1).toUpperCase());
                        }
                    }

                    if (!newEmail.isEmpty()) {
                        accountEmail = newEmail;
                        TextView tvEmail = findViewById(R.id.tvAccountEmail);
                        if (tvEmail != null) tvEmail.setText(accountEmail);
                    }

                    Toast.makeText(this, "Account updated successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
