package com.example.volt_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class HiActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String FIRST_LAUNCH_KEY = "firstLaunch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verifica se é a primeira execução
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!preferences.getBoolean(FIRST_LAUNCH_KEY, true)) {
            // Se não for a primeira, vai direto para LoginActivity
            startLoginActivity();
            return;
        }

        setContentView(R.layout.activity_hi);

        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(v -> {
            // Marca que a atividade já foi exibida
            preferences.edit().putBoolean(FIRST_LAUNCH_KEY, false).apply();
            startLoginActivity();
        });
    }

    private void startLoginActivity() {
        Intent intent = new Intent(HiActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}