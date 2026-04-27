package com.example.volt_project;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.volt_project.data.LocalDatabaseHelper;
import com.example.volt_project.data.models.Pessoa;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddInfoActivity extends AppCompatActivity {

    private EditText etFullName, etBirthDate, etWeight, etHeight;
    private Spinner spSex, spWeightUnit, spHeightUnit;
    private MaterialButton btnGo;

    private FirebaseAuth auth;
    private DatabaseReference dbRef;
    private LocalDatabaseHelper localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_info);

        auth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance(
                "https://volt-31ef3-default-rtdb.europe-west1.firebasedatabase.app/"
        ).getReference("users");
        localDb = new LocalDatabaseHelper(this);

        initViews();
        setUpDateInput();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etBirthDate = findViewById(R.id.etAge);
        spSex = findViewById(R.id.spSex);
        spWeightUnit = findViewById(R.id.spWeightUnit);
        spHeightUnit = findViewById(R.id.spHeightUnit);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        btnGo = findViewById(R.id.btnGo);
    }

    private void setUpDateInput() {
        Calendar calendar = Calendar.getInstance();
        etBirthDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                        // força o formato dd/MM/yyyy (ex: 05/11/2025)
                        String date = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                                selectedDay, selectedMonth + 1, selectedYear);
                        etBirthDate.setText(date);
                    },
                    year, month, day);
            datePickerDialog.show();
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> sexAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select sex", "Male", "Female", "Other"}
        );
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSex.setAdapter(sexAdapter);

        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"kg", "lb"}
        );
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWeightUnit.setAdapter(weightAdapter);

        ArrayAdapter<String> heightAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"cm", "ft"}
        );
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHeightUnit.setAdapter(heightAdapter);
    }

    private void setupListeners() {
        btnGo.setOnClickListener(v -> saveUserInfo());
    }

    private void saveUserInfo() {
        String name = etFullName.getText().toString().trim();
        String birthDateStr = etBirthDate.getText().toString().trim();
        String sex = (String) spSex.getSelectedItem();
        String wu = (String) spWeightUnit.getSelectedItem();
        String hu = (String) spHeightUnit.getSelectedItem();
        double w = parse(etWeight.getText().toString());
        double h = parse(etHeight.getText().toString());

        if (name.isEmpty() || birthDateStr.isEmpty() || spSex.getSelectedItemPosition() == 0 || w <= 0 || h <= 0) {
            toast("Please fill all fields correctly");
            return;
        }

        // cálculo simples da idade (sem parse falhar)
        int age = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date birthDate = sdf.parse(birthDateStr);
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);
            Calendar today = Calendar.getInstance();
            age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) age--;
        } catch (Exception ignored) {}

        if (age < 10 || age > 100) {
            toast("Enter a valid birth date (10–100 years old)");
            return;
        }

        if ("lb".equals(wu)) w *= 0.45359237;
        if ("ft".equals(hu)) h *= 30.48;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            toast("User not authenticated");
            return;
        }

        String email = user.getEmail();
        String uid = user.getUid();

        // guarda localmente
        Pessoa pessoa = new Pessoa();
        pessoa.nome = name;
        pessoa.dataNascimento = birthDateStr;
        pessoa.genero = sex;
        pessoa.altura = h;
        pessoa.peso = w;
        pessoa.email = email;
        localDb.insertOrUpdatePessoa(pessoa);

        // guarda na Firebase
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("birthDate", birthDateStr);
        userMap.put("sex", sex);
        userMap.put("height", h);
        userMap.put("weight", w);
        userMap.put("email", email);

        dbRef.child(uid).child("info").setValue(userMap)
                .addOnSuccessListener(unused -> {
                    toast("Saved Successfully!");
                    Intent intent = new Intent(AddInfoActivity.this, RequestPermissionsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> toast("Error saving data: " + e.getMessage()));
    }

    private double parse(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
