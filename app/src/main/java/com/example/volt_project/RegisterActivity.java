package com.example.volt_project;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private PopupWindow passwordPopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPass = findViewById(R.id.etPassword);
        EditText etConfirm = findViewById(R.id.etConfirm);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);
        ImageView ivInfoPassword = findViewById(R.id.ivInfoPassword);

        ivInfoPassword.setOnClickListener(v -> showPasswordRulesPopup(v));

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPass.getText().toString();
            String confirm = etConfirm.getText().toString();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show();
                return;
            }

            String validationError = validatePassword(password);
            if (validationError != null) {
                Toast.makeText(this, validationError, Toast.LENGTH_LONG).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cria utilizador
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(this, "Account created! Welcome " +
                                    (user != null ? user.getEmail() : ""), Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(this, AddInfoActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Error: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private String validatePassword(String password) {
        if (password.length() < 8) {
            return "Password must have at least 8 characters.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number.";
        }
        if (!password.matches(".*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/`~|\\\\].*")) {
            return "Password must contain at least one special character.";
        }
        return null;
    }

    private void showPasswordRulesPopup(View anchorView) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_password_rules, null);

        int widthInDp = 280;
        float scale = getResources().getDisplayMetrics().density;
        int widthInPx = (int) (widthInDp * scale + 0.5f);

        passwordPopup = new PopupWindow(
                popupView,
                widthInPx,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#333333"));
        background.setCornerRadius(20);
        popupView.setBackground(background);
        popupView.setElevation(12f);

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        int iconWidth = anchorView.getWidth();
        int iconHeight = anchorView.getHeight();

        int xOffset = location[0] + iconWidth - widthInPx;
        int yOffset = location[1] + iconHeight + (int) (6 * scale);

        passwordPopup.showAtLocation(anchorView, Gravity.NO_GRAVITY, xOffset, yOffset);

        popupView.setOnTouchListener((v, event) -> {
            if (passwordPopup != null && passwordPopup.isShowing()) {
                passwordPopup.dismiss();
            }
            return true;
        });
    }
}
