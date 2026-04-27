package com.example.volt_project;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.volt_project.data.LocalDatabaseHelper;
import com.example.volt_project.data.models.Atividade;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class RunningActivity extends AppCompatActivity implements SensorEventListener {

    //Intervalo mínimo pedido ao LocationManager
    private static final int MIN_LOCATION_UPDATE_MS = 1000;
    //Distância mínima em metros para receber atualizações
    private static final float MIN_LOCATION_UPDATE_M = 1f;

    //Comprimento médio de um passo
    //1 Passo = 0.78 metros
    private static final double AVERAGE_STRIDE_LENGTH_METERS = 0.78;
    //Calorias por passo ( 1 passo = 0.04)
    private static final double CALORIES_PER_STEP = 0.04;
    //Helper local para recuperar e inserir dados na base de dados local (SQLite)
    private LocalDatabaseHelper localdb;
    //Variáveis de controlo do mapa
    private MapView map;
    private MyLocationNewOverlay myLocationOverlay;
    private IMapController mapController;
    //Gerir a localização
    private LocationManager locationManager;
    //Variável para guardar a última localização
    private Location latestLocation;

    private LinearLayout cancelBtn;
    private LinearLayout modeBtn;
    private ImageView modeIcon;

    //Distância total estimada
    private double totalDistanceMeters = 0.0;

    // Medem quando foi começado a sessão e tempo acumulado quando em pausa
    private long startTimeMillis = 0L;
    private long pausedOffsetMillis = 0L;

    //Agendar atualizações na UI
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    //Rotina que atualiza a UI a cada segundo
    private Runnable timerRunnable;

    // UI
    private TextView statusGPS;
    private TextView txtMode;
    private TextView tvResult01;
    private TextView tvResult02;
    private TextView tvResult03;
    private TextView tvLabel01;
    private TextView tvLabel02;
    private TextView tvLabel03;
    private LinearLayout toggleActivity;
    private ImageView toggleIcon;
    private LinearLayout statusGpsContainer;
    private LinearLayout stopBtn;

    // Run state
    private boolean isRunning = false;
    private boolean sessionStarted = false;

    // Sensors (steps)
    private SensorManager sensorManager;
    private Sensor stepDetector;
    private Sensor stepCounter;
    private boolean useStepDetector = false;
    //Calcular passos desde o início da sessão
    private long stepBaseline = -1L;
    private int stepsSinceStart = 0;

    // Variáveis calculadas
    private int stepsStored = 0;
    private double caloriesStored = 0.0;
    private double speedKmhStored = 0.0;

    // GPS status (apenas para UI)
    private boolean gpsObtained = false;

    // Enum para os modos de atividade
    private enum ActivityMode {
        //Enumeradores para mudar os modos de atividade, de forma eficiente
        RUNNING(R.drawable.ic_running, "Running", "Time", "Pace", "Kilometers"),
        CYCLING(R.drawable.ic_cycling, "Cycling", "Time", "Speed", "Kilometers"),
        WALKING(R.drawable.ic_walking, "Walking", "Time", "Kilometers", "Calories");

        final int iconRes;
        final String displayName;
        final String label1, label2, label3;
        ActivityMode(int iconRes, String displayName, String label1, String label2, String label3) {
            this.iconRes = iconRes;
            this.displayName = displayName;
            this.label1 = label1;
            this.label2 = label2;
            this.label3 = label3;
        }
    }

    private ActivityMode currentMode = ActivityMode.RUNNING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid user agent
        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());

        setContentView(R.layout.activity_running);
        //Instancia a base de dados local
        localdb = new LocalDatabaseHelper(this);
        initializeViews();
        initializeMap();
        initializeLocationManager();
        initializeSensors();
        initializeTimer();
        setupClickListeners();

        // UI inicial - atualizar conforme o modo
        updateUIForCurrentMode();
    }

    private void initializeViews() {
        // MapView + overlay
        map = findViewById(R.id.map);

        // UI elements
        statusGpsContainer = findViewById(R.id.linearLayout);
        statusGPS = findViewById(R.id.statusGPS);
        tvResult01 = findViewById(R.id.tvResult01);
        tvResult02 = findViewById(R.id.tvResult02);
        tvResult03 = findViewById(R.id.tvResult03);

        // Inicializar os labels
        tvLabel01 = findViewById(R.id.tvLabel01);
        tvLabel02 = findViewById(R.id.tvLabel02);
        tvLabel03 = findViewById(R.id.txLabel03); // Note: no XML é txLabel03, não tvLabel03

        toggleActivity = findViewById(R.id.toggleActivity);
        toggleIcon = findViewById(R.id.imageView10);
        stopBtn = findViewById(R.id.stopActivity);
        cancelBtn = findViewById(R.id.cancelActivity);
        txtMode = findViewById(R.id.txtMode);

        // Inicializar botão de modo
        modeBtn = findViewById(R.id.modeActivity);
        modeIcon = findViewById(R.id.imageView9); // ID do ImageView no modeActivity

        //Modificação do comportamento no botão de Regressar
        OnBackPressedCallback callback =
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        showCancelConfirmationDialog();
                    }
                };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void initializeMap() {
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            mapController = map.getController();
            if (mapController != null) mapController.setZoom(17.0);

            // MyLocation overlay (osmdroid) - apenas visual
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            myLocationOverlay.enableFollowLocation();
            map.getOverlays().add(myLocationOverlay);

            // Verificar se já tem localização
            myLocationOverlay.runOnFirstFix(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateGpsStatus(true);
                        }
                    });
                }
            });
        }
    }

    //Obtém o serviço de localização
    private void initializeLocationManager() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    //Iniciar os sensor de atividade física, calculando os passos
    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            useStepDetector = (stepDetector != null);
        }
    }

    private void initializeTimer() {
        // Runnable do timer (atualiza UI a cada segundo)
        timerRunnable = new Runnable() {
            //Calcula a cada segundo o tempo inical + acumulado desde o inicio da atividade
            //Calcula o tempo em pausa.
            @Override
            public void run() {
                long elapsed = 0L;
                if (sessionStarted) {
                    if (isRunning) {
                        elapsed = pausedOffsetMillis + (SystemClock.elapsedRealtime() - startTimeMillis);
                    } else {
                        elapsed = pausedOffsetMillis;
                    }
                } else {
                    elapsed = 0L;
                }

                // Atualiza tempo (comum a todos os modos)
                long totalSeconds = elapsed / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                if (tvResult01 != null) tvResult01.setText(String.format("%02d:%02d", minutes, seconds));

                // Calcula distância baseada em passos
                totalDistanceMeters = stepsSinceStart * AVERAGE_STRIDE_LENGTH_METERS;
                stepsStored = stepsSinceStart;
                caloriesStored = stepsSinceStart * CALORIES_PER_STEP;

                // Atualiza UI conforme o modo atual
                updateResultsForCurrentMode(elapsed);

                // Velocidade média (km/h) - cálculo comum
                if (sessionStarted && elapsed > 0) {
                    double speedKmh = (totalDistanceMeters / (elapsed / 1000.0)) * 3.6;
                    speedKmhStored = speedKmh;
                }

                //Re-agendamento do timerRunnable
                timerHandler.postDelayed(this, 1000);
            }
        };
    }

    private void setupClickListeners() {
        // Toggle (play/pause/resume)
        if (toggleActivity != null) {
            toggleActivity.setOnClickListener(v -> {
                if (!isRunning) {
                    if (!sessionStarted) startNewSession();
                    else resumeRun();
                } else {
                    pauseRun();
                }
            });
        }

        // Stop button
        if (stopBtn != null) {
            stopBtn.setOnClickListener(v -> showStopConfirmationDialog());
        }

        // Cancel button
        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(v -> showCancelConfirmationDialog());
        }

        // Mode button - alternar entre modos
        if (modeBtn != null) {
            modeBtn.setOnClickListener(v -> switchActivityMode());
        }
    }

    /**
     * Método para alternar entre os modos de atividade
     */
    private void switchActivityMode() {
        // Ciclo: Running -> Cycling -> Walking -> Running
        switch (currentMode) {
            case RUNNING:
                currentMode = ActivityMode.CYCLING;
                break;
            case CYCLING:
                currentMode = ActivityMode.WALKING;
                break;
            case WALKING:
                currentMode = ActivityMode.RUNNING;
                break;
        }

        updateUIForCurrentMode();
    }

    /**
     * Atualiza a UI conforme o modo atual
     */
    private void updateUIForCurrentMode() {
        // Atualizar texto do modo
        if (txtMode != null) {
            txtMode.setText(currentMode.displayName);
        }

        // Atualizar ícone do modo
        if (modeIcon != null) {
            modeIcon.setImageResource(currentMode.iconRes);
        }

        // Atualizar labels
        if (tvLabel01 != null) tvLabel01.setText(currentMode.label1);
        if (tvLabel02 != null) tvLabel02.setText(currentMode.label2);
        if (tvLabel03 != null) tvLabel03.setText(currentMode.label3);

        // Atualizar valores iniciais
        updateInitialUI();
    }

    /**
     * Atualiza os resultados conforme o modo atual
     */
    private void updateResultsForCurrentMode(long elapsed) {
        double km = totalDistanceMeters / 1000.0;
        int calories = (int) Math.round(caloriesStored);

        switch (currentMode) {
            case RUNNING:
                updateRunningMode(elapsed, km);
                break;
            case CYCLING:
                updateCyclingMode(elapsed, km);
                break;
            case WALKING:
                updateWalkingMode(km, calories);
                break;
        }
    }

    private void updateRunningMode(long elapsed, double km) {
        // Pace (min/km)
        //Verifica se há sessão e distância superior a 1 metro e se o tempo passado é superior a 0
        if (sessionStarted && totalDistanceMeters >= 1.0 && elapsed > 0) {
            double timeSecs = elapsed / 1000.0;
            double paceSecPerKm = timeSecs / (totalDistanceMeters / 1000.0);
            int paceMin = (int) (paceSecPerKm / 60.0);
            int paceSec = (int) (paceSecPerKm % 60.0);
            if (tvResult02 != null) tvResult02.setText(String.format("%d:%02d", paceMin, paceSec));
        } else {
            if (tvResult02 != null) tvResult02.setText("--:--");
        }

        // Distância em km
        if (tvResult03 != null) tvResult03.setText(String.format("%.2f", km));
    }

    private void updateCyclingMode(long elapsed, double km) {
        // Velocidade (km/h)
        if (sessionStarted && elapsed > 0) {
            double speedKmh = (totalDistanceMeters / (elapsed / 1000.0)) * 3.6;
            if (tvResult02 != null) tvResult02.setText(String.format("%.2f", speedKmh));
        } else {
            if (tvResult02 != null) tvResult02.setText("0.00");
        }

        // Distância em km
        if (tvResult03 != null) tvResult03.setText(String.format("%.2f", km));
    }

    private void updateWalkingMode(double km, int calories) {
        // Distância em km
        if (tvResult02 != null) tvResult02.setText(String.format("%.2f", km));

        // Calorias
        if (tvResult03 != null) tvResult03.setText(String.format("%d", calories));
    }

    private void updateInitialUI() {
        if (statusGPS != null) statusGPS.setText("Getting GPS Location");
        if (tvResult01 != null) tvResult01.setText("00:00");

        // Inicializar conforme o modo
        switch (currentMode) {
            case RUNNING:
                if (tvResult02 != null) tvResult02.setText("--:--");
                if (tvResult03 != null) tvResult03.setText("0.00");
                break;
            case CYCLING:
                if (tvResult02 != null) tvResult02.setText("0.00");
                if (tvResult03 != null) tvResult03.setText("0.00");
                break;
            case WALKING:
                if (tvResult02 != null) tvResult02.setText("0.00");
                if (tvResult03 != null) tvResult03.setText("0");
                break;
        }
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Activity")
                .setMessage("Do you pretend to cancel your activity? This activity will not be stored in your history.")
                .setPositiveButton("CONFIRM", (dialog, which) -> {
                    // Confirmar cancelamento - voltar para Dashboard
                    returnToDashboard();
                })
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    // Não faz nada, simplesmente fecha o diálogo
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void showStopConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Finish Activity")
                .setMessage("Do you pretend to finish your activity? This activity will be stored in your history.")
                .setPositiveButton("CONFIRM", (dialog, which) -> {
                    // Confirmar término - guardar dados e voltar para Dashboard
                    saveActivityData();
                    returnToDashboard();
                })
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    // Não faz nada, simplesmente fecha o diálogo
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void saveActivityData() {
        // Parar a sessão primeiro para gerar o JSON
        if (sessionStarted) {
            String json = stopSession();
            saveToDatabase(json);
        }
    }

    //Verificar que o GPS foi inicializado
    private void updateGpsStatus(boolean obtained) {
        gpsObtained = obtained;
        if (statusGPS != null) {
            statusGPS.setText(obtained ? "GPS location obtained" : "Getting GPS Location");
        }
        if (obtained) {
            try {
                if (statusGpsContainer != null)
                    ViewCompat.setBackgroundTintList(statusGpsContainer,
                            ContextCompat.getColorStateList(this, R.color.success));
            } catch (Exception ignored) {}
        }
    }

    //Verificação se as permissões de localização de aproximação e precisa foram concedidas
    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    //Verifica se tem a permissão de atividade ativada
    private boolean hasActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void startNewSession() {
        if (!hasActivityRecognitionPermission()) {
            Log.e("RunningActivity", "Missing step counter permission. Finishing activity.");
            finish();
            return;
        }

        pausedOffsetMillis = 0L;
        startTimeMillis = SystemClock.elapsedRealtime();
        totalDistanceMeters = 0.0;
        stepBaseline = -1L;
        stepsSinceStart = 0;
        stepsStored = 0;
        caloriesStored = 0.0;
        speedKmhStored = 0.0;

        sessionStarted = true;
        isRunning = true;

        animateToggleIcon(true);

        startLocationUpdates(); // apenas para visualização
        registerStepSensors();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        timerHandler.post(timerRunnable);
    }

    private void resumeRun() {
        if (!hasActivityRecognitionPermission()) {
            Log.e("RunningActivity", "Missing step counter permission. Finishing activity.");
            finish();
            return;
        }

        animateToggleIcon(true);
        isRunning = true;
        startTimeMillis = SystemClock.elapsedRealtime();

        startLocationUpdates();
        registerStepSensors();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        timerHandler.post(timerRunnable);
    }

    private void pauseRun() {
        animateToggleIcon(false);
        isRunning = false;
        long now = SystemClock.elapsedRealtime();
        pausedOffsetMillis += (now - startTimeMillis);

        stopLocationUpdates();
        unregisterStepSensors();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }

    private String stopSession() {
        if (!sessionStarted) return "";

        if (isRunning) {
            isRunning = false;
            long now = SystemClock.elapsedRealtime();
            pausedOffsetMillis += (now - startTimeMillis);
            stopLocationUpdates();
            unregisterStepSensors();
            if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
        }

        long totalElapsedMs = pausedOffsetMillis;
        long totalSeconds = totalElapsedMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String duration = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        double distanceKm = totalDistanceMeters / 1000.0;
        int calories = (int) Math.round(caloriesStored);
        double avgSpeed = speedKmhStored;

        // Gerar dados específicos por modo
        String pace = "--:--";
        String speed = String.format("%.2f", avgSpeed);

        if (totalDistanceMeters >= 1.0 && totalElapsedMs > 0) {
            double timeSecs = totalElapsedMs / 1000.0;
            double paceSecPerKm = timeSecs / distanceKm;
            int paceMin = (int) (paceSecPerKm / 60.0);
            int paceSec = (int) (paceSecPerKm % 60.0);
            pace = String.format("%d:%02d", paceMin, paceSec);
        }

        String json = "{\n" +
                "  \"type\": \"" + currentMode.displayName.toLowerCase() + "\",\n" +
                "  \"pace\": \"" + pace + "\",\n" +
                "  \"speed\": \"" + speed + "\",\n" +
                "  \"distance\": " + String.format("%.2f", distanceKm) + ",\n" +
                "  \"calories\": " + calories + ",\n" +
                "  \"avg_speed\": " + String.format("%.2f", avgSpeed) + ",\n" +
                "  \"duration\": \"" + duration + "\",\n" +
                "  \"steps\": " + stepsStored + "\n" +
                "}";

        Log.d("RunningActivity", "Session completed:\n" + json);

        // Reset
        sessionStarted = false;
        isRunning = false;
        pausedOffsetMillis = 0L;
        startTimeMillis = 0L;
        totalDistanceMeters = 0.0;
        stepBaseline = -1L;
        stepsSinceStart = 0;
        stepsStored = 0;
        caloriesStored = 0.0;
        speedKmhStored = 0.0;

        // Atualizar UI para valores iniciais
        updateInitialUI();

        try {
            if (toggleIcon != null) toggleIcon.setImageResource(R.drawable.ic_play);
        } catch (Exception ignored) {}

        timerHandler.removeCallbacks(timerRunnable);

        Log.i("RunningActivity", "Session stopped and reset successfully.");

        return json;
    }

    //Obter dados do JSON, processá-los e enviar para a Base de Dados Local
    private void saveToDatabase(String jsonData) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            try {
                JSONObject obj = new JSONObject(jsonData);
                String email = currentUser.getEmail();

                Atividade a = new Atividade();
                a.pessoaId = localdb.getPessoaIdByEmail(email);
                a.tempo = obj.optString("duration");
                a.distancia = obj.optDouble("distance");
                a.ritmo = parsePaceToDouble(obj.optString("pace"));
                a.pacos = obj.optInt("steps");
                a.tipoAtividade = obj.optString("type");
                a.calorias = obj.optInt("calories");
                a.data = java.time.LocalDate.now().toString();

                localdb.insertAtividade(a);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private double parsePaceToDouble(String pace) {
        // converte "5:33" → 5.55 (exemplo)
        try {
            String[] parts = pace.split(":");
            int min = Integer.parseInt(parts[0]);
            int sec = Integer.parseInt(parts[1]);
            return min + sec / 60.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void returnToDashboard() {
        Intent intent = new Intent(RunningActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    //Centra o mapa e mostra a localização
    private void startLocationUpdates() {
        if (!hasLocationPermissions()) return;

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Tentar obter última localização conhecida
                if (locationManager != null) {
                    Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnown == null) {
                        lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }

                    if (lastKnown != null) {
                        latestLocation = lastKnown;
                        updateGpsStatus(true);
                        GeoPoint gp = new GeoPoint(lastKnown.getLatitude(), lastKnown.getLongitude());
                        if (mapController != null) mapController.setCenter(gp);
                    }

                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_LOCATION_UPDATE_MS,
                            MIN_LOCATION_UPDATE_M,
                            locationListener,
                            Looper.getMainLooper()
                    );
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_LOCATION_UPDATE_MS,
                            MIN_LOCATION_UPDATE_M,
                            locationListener,
                            Looper.getMainLooper()
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopLocationUpdates() {
        try {
            if (locationManager != null) locationManager.removeUpdates(locationListener);
        } catch (SecurityException ignored) {
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (location == null) return;

            latestLocation = new Location(location);

            float acc = location.hasAccuracy() ? location.getAccuracy() : Float.MAX_VALUE;
            if (acc > 0 && acc <= 30) {
                updateGpsStatus(true);
            }

            GeoPoint gp = new GeoPoint(location.getLatitude(), location.getLongitude());
            if (mapController != null) mapController.setCenter(gp);
        }

        @Override public void onProviderEnabled(@NonNull String provider) { }
        @Override public void onProviderDisabled(@NonNull String provider) { }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    };

    private void registerStepSensors() {
        if (!hasActivityRecognitionPermission()) return;
        if (sensorManager == null) return;

        if (useStepDetector) {
            sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (stepCounter != null) {
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void unregisterStepSensors() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isRunning) return;

        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            if (event.values != null && event.values.length > 0) {
                stepsSinceStart += (int) event.values[0];
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (stepBaseline < 0) {
                stepBaseline = (long) event.values[0];
            }
            long totalSinceBoot = (long) event.values[0];
            stepsSinceStart = (int) (totalSinceBoot - stepBaseline);
            if (stepsSinceStart < 0) stepsSinceStart = 0;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();

        if (hasLocationPermissions()) {
            if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
            startLocationUpdates();
        }

        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();

        if (isRunning) {
            pauseRun();
        }

        stopLocationUpdates();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        unregisterStepSensors();
        timerHandler.removeCallbacksAndMessages(null);

        // limpar e evitar leaks
        if (map != null) {
            map.getOverlays().clear();
            map.onDetach();
            map = null;
        }
        myLocationOverlay = null;
        mapController = null;
    }

    private void animateToggleIcon(final boolean toPause) {
        if (toggleIcon == null) return;
        toggleIcon.animate().rotationBy(360f).setDuration(300).withEndAction(new Runnable() {
            @Override
            public void run() {
                try {
                    if (toPause) {
                        toggleIcon.setImageResource(R.drawable.ic_pause);
                    } else {
                        toggleIcon.setImageResource(R.drawable.ic_play);
                    }
                    toggleIcon.setRotation(0f);
                } catch (Exception e) {
                    // se drawables não existirem, ignora
                }
            }
        }).start();
    }
}