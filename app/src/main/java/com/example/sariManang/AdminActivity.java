package com.example.sariManang;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class AdminActivity extends AppCompatActivity {
  static class User {
    String id, name, email;
    long lastActive;

    User(String id, String name, String email, long l) {
      this.id = id;
      this.name = name;
      this.email = email;
      lastActive = l;
    }
  }
    private final ArrayList<User> all = new ArrayList<>(), shown = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private BaseAdapter adapter;
    private EditText search;
    private dbManager db = new dbManager();
    private userIDManager user;
    private String id;


    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        user = new userIDManager(AdminActivity.this);
        id = user.getUserID();



        setContentView(R.layout.adminpage);
        ((TextView) findViewById(R.id.tbTitle)).setText("Admin");
        findViewById(R.id.tbBack).setOnClickListener(v -> finish());
        search = findViewById(R.id.etSearch);





        ListView lv = findViewById(R.id.lvUsers);
        lv.setEmptyView(findViewById(R.id.tvEmpty));

        //display each user
        adapter = new BaseAdapter() {
            public int getCount() { return shown.size(); }
            public Object getItem(int p) { return shown.get(p); }
            public long getItemId(int p) { return p; }
            public View getView(int p, View v, ViewGroup parent) {
                if (v == null) v = getLayoutInflater().inflate(R.layout.itemuser, parent, false);
                User u = shown.get(p);
                ((TextView) v.findViewById(R.id.tvId)).setText(String.valueOf(u.id));
                ((TextView) v.findViewById(R.id.tvUser)).setText(u.name);
                ((TextView) v.findViewById(R.id.tvEmail)).setText(u.email);
                ((TextView) v.findViewById(R.id.tvLast)).setText(fmt.format(new Date(u.lastActive)));
                v.findViewById(R.id.ivEdit).setOnClickListener(x -> showEdit(u));
                v.findViewById(R.id.ivDelete).setOnClickListener(x -> confirmDelete(u));
                return v;
            }
        };
        // listens sa seach box and refreshes to update and filter list
        lv.setAdapter(adapter);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence a, int x, int y, int z) {}
            public void onTextChanged(CharSequence a, int x, int y, int z) {}
            public void afterTextChanged(Editable e) { applyFilter(); }
        });

      db.getFromUsers(id, "username")
          .addOnSuccessListener(name -> {
            if (!"Onionn_".equals(name)) {
              Toast.makeText(this, "Admins only", Toast.LENGTH_SHORT).show();
              finish();
              return;
            }
            loadUsers(db);
          })
          .addOnFailureListener(e -> finish());
    }

    // gets inputted search, clears and filters list
    private void applyFilter() {
      String q = search.getText().toString().toLowerCase().trim();
      shown.clear();
      for (User u : all) {
        if (u.name.toLowerCase().contains(q) || u.email.toLowerCase().contains(q)) shown.add(u);
      }
      adapter.notifyDataSetChanged();
    }

  // real refresh: re-fetch from Firestore

  // edit user
    private void showEdit(User u) {
        AlertDialog d = new AlertDialog.Builder(this).setView(R.layout.dialogedituser).create();
        d.show();
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        EditText name = d.findViewById(R.id.etName), email = d.findViewById(R.id.etEmail);
        ((TextView) d.findViewById(R.id.tvUserId)).setText("User ID: " + u.id);
        name.setText(u.name);
        email.setText(u.email);
        d.findViewById(R.id.btnCancel).setOnClickListener(v -> d.dismiss());
      d.findViewById(R.id.btnSave).setOnClickListener(v -> {
        String n = name.getText().toString().trim(), e = email.getText().toString().trim();
        if (n.isEmpty()) { name.setError("Required"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) { email.setError("Enter a valid email"); return; }

        db.updateUser(u.id, n, e)
            .addOnSuccessListener(x -> {
              u.name = n;
              u.email = e;
              applyFilter();
              Toast.makeText(this, "User updated", Toast.LENGTH_SHORT).show();
              d.dismiss();
            })
            .addOnFailureListener(err ->
                Toast.makeText(this, "Update failed: " + err.getMessage(), Toast.LENGTH_LONG).show());
      });
    }

    //confirm delete
    private void confirmDelete(User u) {
        new AlertDialog.Builder(this)
                .setTitle("Delete user?")
                .setMessage("Remove " + u.name + " (ID " + u.id + ")? This cannot be undone.")
            .setPositiveButton("Delete", (d, w) -> {
              if (u.id.equals(id)) {
                Toast.makeText(this, "You can't delete your own admin account", Toast.LENGTH_SHORT).show();
                return;
              }
              db.deleteUser(u.id)
                  .addOnSuccessListener(x -> {
                    all.remove(u);
                    applyFilter();
                    Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
                  })
                  .addOnFailureListener(err ->
                      Toast.makeText(this, "Delete failed: " + err.getMessage(), Toast.LENGTH_LONG).show());
            })
                .setNegativeButton("Cancel", null).show();
    }


    private List<User> loadUsers(dbManager db){
      db.getAllUsers()
          .addOnSuccessListener(querySnapshot -> {

            all.clear();

            for (QueryDocumentSnapshot doc : querySnapshot) {

              // Get fields from the document
              String userId = doc.getId();
              String name = doc.getString("username");
              String email = doc.getString("email");

              // Get last active timestamp, if available
              Timestamp timestamp = doc.getTimestamp("lastActive");

              long lastActive = timestamp != null
                  ? timestamp.toDate().getTime()
                  : 0L;

              // Add user to ArrayList
              all.add(new User(
                  userId,
                  name != null ? name : "Unknown",
                  email != null ? email : "No email",
                  lastActive
              ));
            }

            applyFilter();


            // Refresh the ListView after loading users
          })
          .addOnFailureListener(e -> {
            Toast.makeText(
                AdminActivity.this,
                "Failed to load users: " + e.getMessage(),
                Toast.LENGTH_LONG
            ).show();
          });
      return all;
    }
}
