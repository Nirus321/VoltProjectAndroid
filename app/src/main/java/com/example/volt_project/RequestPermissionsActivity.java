package com.example.volt_project;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class RequestPermissionsActivity extends AppCompatActivity {

    private static final int REQ_ACTIVITY = 200;
    private static final int REQ_STORAGE = 201;
    private static final int REQ_LOCATION = 202;
    private static final int REQ_BACKGROUND = 203;

    // Estágios sequenciais
    private static final int STAGE_ACTIVITY = 0;
    private static final int STAGE_STORAGE = 1;
    private static final int STAGE_LOCATION = 2;
    private static final int STAGE_BACKGROUND = 3;
    private static final int STAGE_DONE = 4;

    private int currentStage = STAGE_ACTIVITY;
    private Button btnPermissions;

    // SharedPreferences para registar se já pedimos uma permissão antes
    private static final String PREFS = "perm_prefs";
    private static final String KEY_ASKED_ACTIVITY = "asked_activity";
    private static final String KEY_ASKED_LOCATION = "asked_location";
    private static final String KEY_ASKED_BACKGROUND = "asked_background";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_permissions);

        btnPermissions = findViewById(R.id.btnPermissions);

        // Verificar se já tem todas as permissões necessárias
        if (hasAllRequiredPermissions()) {
            goToDashboard();
            return;
        }

        btnPermissions.setOnClickListener(v -> {
            currentStage = STAGE_ACTIVITY;
            requestNextMissingPermission();
        });
    }

    //Verifica se todas as permissões obrigatórias estão concedidas
    private boolean hasAllRequiredPermissions() {
        // Activity Recognition (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        // Location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        return true;
    }

    // Fluxo sequencial para pedir permissões em falta
    private void requestNextMissingPermission() {
        switch (currentStage) {
            case STAGE_ACTIVITY:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                            != PackageManager.PERMISSION_GRANTED) {

                        // Se já perguntámos antes e o sistema diz que não devemos mostrar diálogo
                        if (wasAskedBefore(KEY_ASKED_ACTIVITY) &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACTIVITY_RECOGNITION)) {
                            // Permissão permanentemente negada -> abrir Definições
                            showSettingsDialog("Permissão de reconhecimento bloqueada",
                                    "A permissão de reconhecimento de atividade foi bloqueada. Ative-a nas Definições da app.");
                            return;
                        }

                        // Registar que estamos a pedir pela primeira/segunda vez
                        markAsked(KEY_ASKED_ACTIVITY);

                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                                REQ_ACTIVITY);
                        return;
                    }
                }
                // Já tem ou não precisa → próximo estágio
//                currentStage = STAGE_STORAGE;
                currentStage = STAGE_LOCATION;
                requestNextMissingPermission();
                break;

            case STAGE_LOCATION:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {

                    // Verificar se já perguntámos antes e se o sistema bloqueou o pedido
                    boolean fineAsked = wasAskedBefore(KEY_ASKED_LOCATION);
                    boolean shouldShowFine = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
                    boolean shouldShowCoarse = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);

                    if (fineAsked && !shouldShowFine && !shouldShowCoarse) {
                        // Ambos bloqueados/permanentemente negados -> abrir definições
                        showSettingsDialog("Permissão de localização bloqueada",
                                "As permissões de localização foram bloqueadas. Ative-as nas Definições da app.");
                        return;
                    }

                    markAsked(KEY_ASKED_LOCATION);

                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            REQ_LOCATION);
                    return;
                }
                currentStage = STAGE_BACKGROUND;
                requestNextMissingPermission();
                break;

            case STAGE_BACKGROUND:
                // Background location é OPICIONAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {

                        if (wasAskedBefore(KEY_ASKED_BACKGROUND) &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            // background permanentemente negada -> sugerir definições (opcional)
                            showSettingsDialog("Localização em 2.º plano bloqueada",
                                    "A permissão de localização em 2.º plano foi bloqueada. Pode ativá-la nas Definições da app.");
                            // mesmo que esteja bloqueada, é opcional -> continua para DONE
                            currentStage = STAGE_DONE;
                            requestNextMissingPermission();
                            return;
                        }

                        markAsked(KEY_ASKED_BACKGROUND);

                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                REQ_BACKGROUND);
                        return;
                    }
                }
                currentStage = STAGE_DONE;
                requestNextMissingPermission();
                break;

            case STAGE_DONE:
                // Todas as permissões processadas
                if (hasAllRequiredPermissions()) {
                    goToDashboard();
                } else {
                    Toast.makeText(this, "Permissões obrigatórias em falta. Tente novamente.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    // Processa as respostas das permissões

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0) {
            // Usuário cancelou
            Toast.makeText(this, "Solicitação cancelada", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (requestCode) {
            case REQ_ACTIVITY:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //currentStage = STAGE_STORAGE;
                    currentStage = STAGE_LOCATION;
                } else {
                    // Se permissão negada permanentemente (Don't ask again), shouldShowRequestPermissionRationale retorna false
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACTIVITY_RECOGNITION)) {
                        showSettingsDialog("Permissão de reconhecimento de atividade bloqueada",
                                "A permissão de reconhecimento de atividade foi bloqueada. Ative-a nas Definições da app.");
                        return;
                    }

                    Toast.makeText(this, "Permissão de reconhecimento de atividade é obrigatória", Toast.LENGTH_LONG).show();
                    return;
                }
                break;

            case REQ_LOCATION:
                boolean locationGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        locationGranted = false;
                        break;
                    }
                }
                if (locationGranted) {
                    currentStage = STAGE_BACKGROUND;
                } else {
                    // Se permissão permanentemente negada -> abrir definições
                    boolean anyShouldShow = false;
                    for (String p : permissions) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, p)) {
                            anyShouldShow = true;
                            break;
                        }
                    }
                    if (!anyShouldShow) {
                        showSettingsDialog("Permissões de localização bloqueadas",
                                "As permissões de localização foram bloqueadas. Ative-as nas Definições da app.");
                        return;
                    }

                    Toast.makeText(this, "Permissões de localização são obrigatórias", Toast.LENGTH_LONG).show();
                    return;
                }
                break;

            case REQ_BACKGROUND:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // Background é opcional; se permanentemente negada, sugere definições e segue
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        showSettingsDialog("Localização em segundo plano bloqueada",
                                "A permissão de localização em 2.º plano foi bloqueada. Pode ativá-la nas Definições da app.");
                        // continua, pois é opcional
                    } else {
                        Toast.makeText(this, "Localização em segundo plano não concedida. A app funcionará sem ela.", Toast.LENGTH_SHORT).show();
                    }
                }
                currentStage = STAGE_DONE;
                break;
        }

        // Continuar para próxima permissão
        requestNextMissingPermission();
    }

    //Avança para a DashboardActivity
    private void goToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
        finish(); // Fecha esta activity
    }

    // Helpers para SharedPreferences
    private void markAsked(String key) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean(key, true).apply();
    }

    private boolean wasAskedBefore(String key) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    // Mostra um diálogo que leva o utilizador às definições da aplicação
    private void showSettingsDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Abrir Definições", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    Toast.makeText(this, "Permissão necessária não concedida.", Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }
}
