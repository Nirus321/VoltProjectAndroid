package com.example.volt_project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.volt_project.databinding.ActivityDashboardBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    //Menu Principal Lateral com as opções
    private NavigationView navViewMain;
    //Menu Lateral
    private DrawerLayout drawer;
    //Objeto que gera a navegação entre fragmentos
    private NavController navController;
    //Verificar último elemento selecionado
    private MenuItem lastSelectedMenuItem;
    private static final String KEY_EMAIL = "saved_email";
    private static final String KEY_PASSWORD = "saved_password";
    private static final String KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityDashboardBinding binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //Toolbar como barra de ação
        setSupportActionBar(binding.appBarDashboard.toolbar);

        drawer = binding.drawerLayout;
        navViewMain = binding.navViewMain;
        NavigationView navViewBottom = binding.navViewBottom;

        //Define os destinos dos fragmentos
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dashboard, R.id.nav_history, R.id.nav_profile_and_settings)
                .setOpenableLayout(drawer)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_dashboard);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        // Configurar manualmente a navegação sem usar NavigationUI para o NavigationView
        setupCustomNavigation();

        //Define comportamento do menu inferior
        navViewBottom.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_sign_out) {
                handleSignOut();
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
            return false;
        });

        // Definir o item inicial como selecionado
        if (savedInstanceState == null) {
            setSelectedMenuItem(R.id.nav_dashboard);
        }
    }


    private void setupCustomNavigation() {
        navViewMain.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            drawer.closeDrawer(GravityCompat.START);

            //Abre a RunningActivity
            if (id == R.id.nav_start_exercise) {
                // Redirecionar para a Activity de exercício SEM marcar como selecionado
                Intent intent = new Intent(DashboardActivity.this, RunningActivity.class);
                startActivity(intent);
                return true;
            } else {
                // Para fragments, navegar e marcar como selecionado
                try {
                    navController.navigate(id);
                    setSelectedMenuItem(id);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        // Listener para atualizar a seleção quando navegar pelos fragments
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();

            // Só atualiza a seleção se for um destino válido no menu
            if (destinationId == R.id.nav_dashboard ||
                    destinationId == R.id.nav_history ||
                    destinationId == R.id.nav_profile_and_settings) {
                setSelectedMenuItem(destinationId);
            }
        });
    }

    //Atualiza visualmente o item selecionado
    private void setSelectedMenuItem(int menuItemId) {
        Menu menu = navViewMain.getMenu();

        // Desselecionar todos os itens primeiro
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setChecked(false);
        }

        // Selecionar apenas o item especificado
        MenuItem selectedItem = menu.findItem(menuItemId);
        if (selectedItem != null) {
            selectedItem.setChecked(true);
            lastSelectedMenuItem = selectedItem;
        }
    }

    //Processo de Log Out
    private void handleSignOut() {
        // 1. Fazer sign out do Firebase Auth
        FirebaseAuth.getInstance().signOut();

        // 2. Desativar o login automático nas SharedPreferences
        SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        disableAutoLogin(loginPrefs);

        // 3. Redirecionar para a LoginActivity e limpar a back stack
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public static void disableAutoLogin(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_PASSWORD);
        editor.putBoolean(KEY_AUTO_LOGIN_ENABLED, false);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    //Quando retornar à DashboardActivity, retorna ao item anterior, antes de ter ido
    //para a RunnignActivity
    @Override
    protected void onResume() {
        super.onResume();
        // Quando voltar da RunningActivity, restaurar a seleção anterior
        if (lastSelectedMenuItem != null) {
            lastSelectedMenuItem.setChecked(true);
        } else {
            setSelectedMenuItem(R.id.nav_dashboard);
        }
    }
}