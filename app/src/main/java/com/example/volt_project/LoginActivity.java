package com.example.volt_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private LinearLayout linearLayout;
    private SharedPreferences sharedPreferences;

    // Chaves para SharedPreferences
    private static final String LOGIN_PREFS = "LoginPrefs";
    private static final String KEY_EMAIL = "saved_email";
    private static final String KEY_PASSWORD = "saved_password";
    private static final String KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(LOGIN_PREFS, MODE_PRIVATE);

        progressBar = findViewById(R.id.progressBar);
        linearLayout = findViewById(R.id.linearLayout);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgot = findViewById(R.id.tvForgot);
        TextView tvRegister = findViewById(R.id.tvRegister);

        // Verifica se deve tentar login automático
        checkAutoLogin();

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            //Validações de Inputs
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pass.isEmpty()) {
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(email, pass, true); // true = salvar credenciais
        });

        tvForgot.setOnClickListener(v -> startActivity(new Intent(this, RecoverEmailActivity.class)));
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void checkAutoLogin() {
        String savedEmail = sharedPreferences.getString(KEY_EMAIL, null);
        String savedPassword = sharedPreferences.getString(KEY_PASSWORD, null);
        boolean autoLoginEnabled = sharedPreferences.getBoolean(KEY_AUTO_LOGIN_ENABLED, true);

        //Verifica se as credenciais estão guardadas no SharedPreferences e se o utilizador consegue
        //se autenticar
        if (savedEmail != null && savedPassword != null && autoLoginEnabled) {
            // Tem credenciais salvas → esconde o layout e faz login automático
            showLoginUI(false);
            progressBar.setVisibility(View.VISIBLE);

            // Preenche os campos (caso o login automático falhe)
            EditText etEmail = findViewById(R.id.etEmail);
            EditText etPassword = findViewById(R.id.etPassword);
            etEmail.setText(savedEmail);
            etPassword.setText(savedPassword);

            performLogin(savedEmail, savedPassword, false); // false = não salvar novamente
        } else {
            // Não tem credenciais salvas → mostra o layout normal
            showLoginUI(true);
            progressBar.setVisibility(View.GONE);
        }
    }

    private void performLogin(String email, String password, boolean saveCredentials) {
        showLoading(true);
        //Fazer autenticação no Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (saveCredentials) {
                            // Salva as credenciais para login automático futuro
                            saveLoginCredentials(email, password);
                        }

                        // Login bem-sucedido
                        startActivity(new Intent(this, RequestPermissionsActivity.class));
                        finish();
                    } else {
                        // Login falhou - mostra a UI de login
                        showLoginUI(true);
                        showLoading(false);

                        if (saveCredentials) {
                            // Remove credenciais salvas se o login falhar
                            clearSavedCredentials();
                        }

                        Toast.makeText(this, "E-mail or password Incorrect", Toast.LENGTH_LONG).show();
                    }
                });
    }

    //Salvar credenciais nas preferências
    private void saveLoginCredentials(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_AUTO_LOGIN_ENABLED, true);
        editor.apply();
    }

    //Limpar as credenciais nas preferências
    private void clearSavedCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_PASSWORD);
        editor.apply();
    }

    //Mostrar UI de Login
    private void showLoginUI(boolean show) {
        if (linearLayout != null) {
            linearLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    //Mostrar Loading
    private void showLoading(boolean show) {
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgot = findViewById(R.id.tvForgot);
        TextView tvRegister = findViewById(R.id.tvRegister);

        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            tvForgot.setEnabled(false);
            tvRegister.setEnabled(false);
            btnLogin.setAlpha(0.5f);
            // Mantém o linearLayout visível durante o loading manual
            if (linearLayout.getVisibility() == View.VISIBLE) {
                linearLayout.setAlpha(0.7f);
            }
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            tvForgot.setEnabled(true);
            tvRegister.setEnabled(true);
            btnLogin.setAlpha(1f);
            linearLayout.setAlpha(1f);
        }
    }
}