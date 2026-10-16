package com.example.sariManang;

import android.content.Intent;
import android.os.Bundle;
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
            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(in);
        });
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
