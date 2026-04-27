package com.example.volt_project.data;

import com.example.volt_project.data.models.Atividade;
import com.example.volt_project.data.models.Pessoa;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseManager {

    private final DatabaseReference rootRef = FirebaseDatabase.getInstance(
            "https://volt-31ef3-default-rtdb.europe-west1.firebasedatabase.app/"
    ).getReference("users");

    // 🔹 Returns the node where user info (Pessoa) is stored
    public DatabaseReference getUserInfo(String uid) {
        return rootRef.child(uid).child("info");
    }

    // 🔹 Returns the node where user activities are stored
    public DatabaseReference getUserActivities(String uid) {
        return rootRef.child(uid).child("atividades");
    }

    // 🔹 Save Pessoa to Firebase (and return a Task so you can use addOnSuccessListener / addOnFailureListener)
    public Task<Void> saveUserInfo(String uid, Pessoa p) {
        return getUserInfo(uid).setValue(p);
    }

    // 🔹 Save Pessoa (legacy version without Task)
    public void sendPessoaToFirebase(String uid, Pessoa p) {
        try {
            getUserInfo(uid).setValue(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 Save single Atividade
    public void sendAtividadeToFirebase(String uid, Atividade a) {
        try {
            String key = getUserActivities(uid).push().getKey();
            if (key != null) {
                getUserActivities(uid).child(key).setValue(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
