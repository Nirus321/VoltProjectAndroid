package com.example.volt_project.dashboard.profileandsettings;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.volt_project.HiActivity;
import com.example.volt_project.RecoverEmailActivity;
import com.example.volt_project.data.DataRepository;
import com.example.volt_project.data.FirebaseManager;
import com.example.volt_project.data.models.Atividade;
import com.example.volt_project.data.models.Pessoa;
import com.example.volt_project.databinding.FragmentProfileAndSettingsBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ProfileAndSettingsFragment extends Fragment {

    private FragmentProfileAndSettingsBinding binding;
    private DataRepository repo;
    private FirebaseManager firebaseManager;
    private Pessoa currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileAndSettingsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        repo = new DataRepository(requireContext());
        firebaseManager = new FirebaseManager();

        loadUserAndAtividades();
        loadUserFromFirebase();

        setupSpinners();
        setupDatePicker();
        setupClicks();
//        setupAutoUpdate(); // optional auto-save on typing

        return view;
    }

    // Carrega o utilizador atual e cria se não existir
    private void loadUserAndAtividades() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null || fbUser.getEmail() == null) {
            Toast.makeText(getContext(), "Usuário não logado", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = fbUser.getEmail();
        Pessoa local = repo.getPessoaByEmail(email);

        if (local != null) {
            currentUser = local;
        } else {
            currentUser = new Pessoa();
            currentUser.email = email;
            repo.savePessoa(currentUser);
        }

        loadAtividadesFromSQLite(email);
    }

    private void loadAtividadesFromSQLite(String email) {
        List<Atividade> atividades = repo.getAllAtividades(email);
        System.out.println("Atividades locais carregadas: " + atividades.size());
    }

    // Carrega o utilizador do FireBase e sincroniza com o SQLite + UI
    private void loadUserFromFirebase() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        final String uid = fbUser.getUid();

        firebaseManager.getUserInfo(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                Pessoa p = snap.getValue(Pessoa.class);
                if (p != null && p.email != null) {
                    currentUser = p;
                    repo.savePessoa(p); // Atualizar SQLite

                    // Atualizar campos
                    binding.etFullName.setText(p.nome);
                    binding.etEmail.setText(p.email);
                    binding.etAge.setText(p.dataNascimento);
                    binding.etWeight.setText(String.valueOf(p.peso));
                    binding.etHeight.setText(String.valueOf(p.altura));

                    if (p.genero != null) {
                        switch (p.genero.toLowerCase()) {
                            case "male":
                                binding.spSex.setSelection(1);
                                break;
                            case "female":
                                binding.spSex.setSelection(2);
                                break;
                            default:
                                binding.spSex.setSelection(3);
                                break;
                        }
                    }

                } else {
                    Pessoa local = repo.getPessoaByEmail(fbUser.getEmail());
                    if (local != null) {
                        currentUser = local;
                        binding.etFullName.setText(local.nome);
                        binding.etEmail.setText(local.email);
                        binding.etAge.setText(local.dataNascimento);
                        binding.etWeight.setText(String.valueOf(local.peso));
                        binding.etHeight.setText(String.valueOf(local.altura));
                    } else {
                        toast("No user data found");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> sexAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Select sex", "Male", "Female", "Other"}
        );
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spSex.setAdapter(sexAdapter);

        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"kg", "lb"}
        );
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spWeightUnit.setAdapter(weightAdapter);

        ArrayAdapter<String> heightAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"cm", "ft"}
        );
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spHeightUnit.setAdapter(heightAdapter);

        binding.delForeverBtn.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void setupDatePicker() {
        binding.etAge.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(requireContext(), (datePicker, y, m, d) ->
                    binding.etAge.setText(d + "/" + (m + 1) + "/" + y), year, month, day).show();
        });
    }

    private void setupClicks() {
        binding.btnSaveInfo.setOnClickListener(v -> savePersonalInfo());
        binding.btnSaveNewPassword.setOnClickListener(v -> saveCredentials());
        binding.tvForgot.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), RecoverEmailActivity.class);
            startActivity(intent);
        });

        binding.ivSyncNow.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Sincronizando dados...", Toast.LENGTH_SHORT).show();
            syncAtividades();
        });
    }

    // Salvar informações na Base Local e no Firebase
    private void savePersonalInfo() {
        String name = Objects.requireNonNull(binding.etFullName.getText()).toString().trim();
        String birthDate = Objects.requireNonNull(binding.etAge.getText()).toString().trim();
        String sex = binding.spSex.getSelectedItem().toString();
        String weightStr = Objects.requireNonNull(binding.etWeight.getText()).toString().trim();
        String heightStr = Objects.requireNonNull(binding.etHeight.getText()).toString().trim();
        String wu = (String) binding.spWeightUnit.getSelectedItem();
        String hu = (String) binding.spHeightUnit.getSelectedItem();

        if (name.isEmpty()) {
            toast("Enter your full name");
            return;
        }
        if (birthDate.isEmpty()) {
            toast("Enter your birth date");
            return;
        }
        if (binding.spSex.getSelectedItemPosition() == 0) {
            toast("Select your sex");
            return;
        }
        if (weightStr.isEmpty() || Double.parseDouble(weightStr) <= 0) {
            toast("Enter a valid weight");
            return;
        }
        if (heightStr.isEmpty() || Double.parseDouble(heightStr) <= 0) {
            toast("Enter a valid height");
            return;
        }

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            toast("User not logged in");
            return;
        }

        double weight = Double.parseDouble(weightStr);
        double height = Double.parseDouble(heightStr);

        if ("lb".equals(wu)) weight *= 0.45359237;
        if ("ft".equals(hu)) height *= 30.48;

        if (currentUser == null) currentUser = new Pessoa();
        currentUser.nome = name;
        currentUser.dataNascimento = birthDate;
        currentUser.genero = sex;
        currentUser.peso = weight;
        currentUser.altura = height;
        currentUser.email = fbUser.getEmail();

        // ✅ update SQLite
        repo.savePessoa(currentUser);

        // ✅ update Firebase
        String uid = fbUser.getUid();
        firebaseManager.saveUserInfo(uid, currentUser)
                .addOnSuccessListener(aVoid -> toast("Info saved to Firebase ✅"))
                .addOnFailureListener(e -> toast("Failed to save Firebase: " + e.getMessage()));
    }

    // Salvar credenciais
    private void saveCredentials() {
        String email = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
        String oldPass = Objects.requireNonNull(binding.etCurrentPass.getText()).toString().trim();
        String newPass = Objects.requireNonNull(binding.etNewPass.getText()).toString().trim();

        if (oldPass.isEmpty()) {
            toast("Enter your current password");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Enter a valid email");
            return;
        }

        if (newPass.isEmpty()) {
            toast("Enter your new password");
            return;
        }

        String validationError = validatePassword(newPass);
        if (validationError != null) {
            toast(validationError);
            return;
        }

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null || fbUser.getEmail() == null) {
            toast("User not logged in");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(fbUser.getEmail(), oldPass);

        fbUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    boolean emailChanged = !email.equalsIgnoreCase(fbUser.getEmail());

                    // Atualiza utilizador no Firebase Database
                    fbUser.updatePassword(newPass)
                            .addOnSuccessListener(aVoid1 -> toast("Password Updated"))
                            .addOnFailureListener(e -> toast("Password update failed: " + e.getMessage()));

                    // Verificação do utilizador por e-mail
                    if (emailChanged) {
                        fbUser.verifyBeforeUpdateEmail(email)
                                .addOnSuccessListener(unused -> {
                                    toast("Confirm in your E-mail the Changes");

                                    DatabaseReference dbRef = FirebaseDatabase.getInstance(
                                                    "https://volt-31ef3-default-rtdb.europe-west1.firebasedatabase.app/"
                                            ).getReference("users")
                                            .child(fbUser.getUid())
                                            .child("info")
                                            .child("email");

                                    dbRef.setValue(email)
                                            .addOnSuccessListener(aVoid2 ->
                                                    toast("Database email updated — verify your inbox to confirm."))
                                            .addOnFailureListener(e ->
                                                    toast("Failed to update DB: " + e.getMessage()));
                                })
                                .addOnFailureListener(e ->
                                        toast("Error sending verification email: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> toast("Reauthentication failed: " + e.getMessage()));
    }

    private String validatePassword(String password) {
        if (password.length() < 8) return "Password must have at least 8 characters.";
        if (!password.matches(".*[A-Z].*")) return "Password must contain at least one uppercase letter.";
        if (!password.matches(".*[a-z].*")) return "Password must contain at least one lowercase letter.";
        if (!password.matches(".*\\d.*")) return "Password must contain at least one number.";
        if (!password.matches(".*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/~|\\\\].*"))
            return "Password must contain at least one special character.";
        return null;
    }

    // Sincronizar Firebase e SQLite
    private void syncAtividades() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null || fbUser.getEmail() == null) {
            Toast.makeText(getContext(), "Usuário não logado", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = fbUser.getEmail();
        if (repo == null) repo = new DataRepository(requireContext());
        if (currentUser == null) {
            currentUser = repo.getPessoaByEmail(email);
            if (currentUser == null) {
                currentUser = new Pessoa();
                currentUser.email = email;
                repo.savePessoa(currentUser);
            }
        }

        DatabaseReference usersRef = FirebaseDatabase.getInstance(
                "https://volt-31ef3-default-rtdb.europe-west1.firebasedatabase.app/"
        ).getReference("users");

        usersRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(getContext(), "Erro ao aceder ao Firebase", Toast.LENGTH_SHORT).show();
                return;
            }

            String foundUid = null;
            for (DataSnapshot snap : task.getResult().getChildren()) {
                DataSnapshot info = snap.child("info");
                String emailFirebase = info.child("email").getValue(String.class);
                if (emailFirebase != null && emailFirebase.equalsIgnoreCase(email)) {
                    foundUid = snap.getKey();
                    break;
                }
            }

            if (foundUid == null) {
                foundUid = email.replace(".", "_").replace("@", "_");
                usersRef.child(foundUid).child("info").child("email").setValue(email);
            }

            String uidToUse = foundUid;

            List<Atividade> localAtividades = repo.getAllAtividades(email);
            firebaseManager.getUserActivities(uidToUse).get().addOnCompleteListener(task2 -> {
                if (!task2.isSuccessful() || task2.getResult() == null) {
                    Toast.makeText(getContext(), "Erro ao ler atividades Firebase", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Atividade> remoteAtividades = new ArrayList<>();
                for (DataSnapshot snap : task2.getResult().getChildren()) {
                    Atividade a = snap.getValue(Atividade.class);
                    if (a != null) remoteAtividades.add(a);
                }

                mergeAtividades(repo, firebaseManager, uidToUse, localAtividades, remoteAtividades);
                loadAtividadesFromSQLite(email);
                Toast.makeText(getContext(), "Sincronização concluída!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void mergeAtividades(DataRepository repo, FirebaseManager firebaseManager, String uid,
                                 List<Atividade> localList, List<Atividade> remoteList) {

        Set<String> localKeys = new HashSet<>();
        for (Atividade a : localList) {
            localKeys.add(a.data + "_" + a.tempo + "_" + a.tipoAtividade);
        }

        Set<String> remoteKeys = new HashSet<>();
        for (Atividade a : remoteList) {
            remoteKeys.add(a.data + "_" + a.tempo + "_" + a.tipoAtividade);
        }

        // Puxa novas do Firebase -> adiciona ao SQLite se não existirem
        for (Atividade a : remoteList) {
            String key = a.data + "_" + a.tempo + "_" + a.tipoAtividade;
            if (!localKeys.contains(key)) {
                if (a.pessoaId == 0) {
                    int pessoaId = repo.getPessoaByEmail(currentUser.email).id;
                    a.pessoaId = pessoaId;
                }
                repo.insertAtividade(a);
            }
        }

        // Sobe novas do SQLite -> adiciona ao Firebase se não existirem
        for (Atividade a : localList) {
            String key = a.data + "_" + a.tempo + "_" + a.tipoAtividade;
            if (!remoteKeys.contains(key)) {
                firebaseManager.sendAtividadeToFirebase(uid, a);
            }
        }

        // Caso especial: SQLite vazio → importa tudo do Firebase
        if (localList.isEmpty() && !remoteList.isEmpty()) {
            int pessoaId = repo.getPessoaByEmail(currentUser.email).id;
            for (Atividade a : remoteList) {
                a.pessoaId = pessoaId;
                repo.insertAtividade(a);
            }
        }
    }


    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Do you really wish to delete your account? This will erase all activity and info.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (dialog, which) -> deleteAccountForever())
                .create()
                .show();
    }

    //Eliminar conta, eliminando a conta na autenticação, dados SQLite e Firebase Database, e encerrar sessão
    private void deleteAccountForever() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null || fbUser.getEmail() == null) {
            toast("Usuário não logado");
            return;
        }

        String email = fbUser.getEmail();
        DatabaseReference usersRef = FirebaseDatabase.getInstance(
                "https://volt-31ef3-default-rtdb.europe-west1.firebasedatabase.app/"
        ).getReference("users");

        usersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DataSnapshot snap : task.getResult().getChildren()) {
                    DataSnapshot info = snap.child("info");
                    String emailFirebase = info.child("email").getValue(String.class);
                    if (emailFirebase != null && emailFirebase.equalsIgnoreCase(email)) {
                        usersRef.child(snap.getKey()).removeValue()
                                .addOnSuccessListener(aVoid -> toast("Dados Firebase apagados"))
                                .addOnFailureListener(e -> toast("Erro ao apagar dados Firebase"));
                        break;
                    }
                }
            } else {
                toast("Erro ao verificar Firebase");
            }
        });

        fbUser.delete()
                .addOnSuccessListener(aVoid -> toast("Conta FirebaseAuth apagada"))
                .addOnFailureListener(e -> toast("Erro ao apagar FirebaseAuth: " + e.getMessage()));

        try {
            repo.clearAllTables();
            toast("SQLite limpo com sucesso");
        } catch (Exception e) {
            toast("Erro ao limpar SQLite: " + e.getMessage());
        }

        Intent intent = new Intent(requireContext(), HiActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
