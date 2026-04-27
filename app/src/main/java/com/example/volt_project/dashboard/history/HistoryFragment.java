package com.example.volt_project.dashboard.history;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.volt_project.R;
import com.example.volt_project.data.DataRepository;
import com.example.volt_project.data.models.Atividade;
import com.example.volt_project.data.models.Pessoa;
import com.example.volt_project.databinding.FragmentHistoryBinding;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryAdapter adapter;
    private final ArrayList<HistoryItem> allItems = new ArrayList<>();

    private DataRepository repo;
    private Pessoa currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        repo = new DataRepository(requireContext());

        // Inicializa lista vazia
        adapter = new HistoryAdapter(requireContext(), new ArrayList<>());
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);

        // Botões de filtro
        final List<MaterialButton> buttons = Arrays.asList(
                binding.fltAll,
                binding.fltRunning,
                binding.fltBycicle,
                binding.fltWalking
        );
        updateButtonStyles(buttons, binding.fltAll);
        for (MaterialButton btn : buttons) {
            btn.setOnClickListener(v -> {
                updateButtonStyles(buttons, btn);
                filterListByButton(btn);
            });
        }

        // Carrega usuário atual (FirebaseAuth) e atividades locais
        loadUserAndAtividades();

        return root;
    }

    // ------------------------------------------------------------------------

    private void loadUserAndAtividades() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null || fbUser.getEmail() == null) {
            Toast.makeText(getContext(), "Utilizador não logado", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = fbUser.getEmail();
        Pessoa local = repo.getPessoaByEmail(email);

        if (local != null) {
            currentUser = local;
        } else {
            // Cria registro mínimo se ainda não existir
            currentUser = new Pessoa();
            currentUser.email = email;
            repo.savePessoa(currentUser);
        }

        loadAtividadesFromSQLite(email);
    }

    // ------------------------------------------------------------------------

    private void loadAtividadesFromSQLite(String email) {
        allItems.clear();

        List<Atividade> atividades = repo.getAllAtividades(email);
        if (atividades.isEmpty()) {
            Toast.makeText(getContext(), "Nenhuma atividade registrada", Toast.LENGTH_SHORT).show();
        }

        for (Atividade a : atividades) {
            allItems.add(mapAtividadeToHistoryItem(a));
        }

        adapter.updateList(new ArrayList<>(allItems));
    }

    // ------------------------------------------------------------------------

    //Mapeamento dos dados recebidos para cada item criado no RecycleView
    private HistoryItem mapAtividadeToHistoryItem(Atividade a) {
        String type = a.tipoAtividade != null ? a.tipoAtividade.toLowerCase() : "";
        String paceStr = a.ritmo > 0 ? String.format("%.2f", a.ritmo) : "";
        double speed = (a.ritmo > 0) ? (60 / a.ritmo) : 0; // opcional: converte ritmo (min/km) em km/h

        return new HistoryItem(
                type,
                paceStr,
                speed,
                a.distancia,
                a.calorias,
                speed,
                a.tempo != null ? a.tempo : "",
                a.pacos,
                a.data != null ? a.data : ""
        );
    }

    // ------------------------------------------------------------------------

    //Atualizar estilos dos botões à medida que forem clicados (clicado, o destacado)
    private void updateButtonStyles(List<MaterialButton> buttons, MaterialButton selected) {
        int colorPrimary = ContextCompat.getColor(requireContext(), R.color.primary);
        int colorBackground = ContextCompat.getColor(requireContext(), R.color.background);
        int colorSelectedText = ContextCompat.getColor(requireContext(), R.color.foreground);
        int colorUnselectedText = colorSelectedText;

        for (MaterialButton btn : buttons) {
            if (btn == selected) {
                btn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                btn.setTypeface(null, Typeface.BOLD);
                btn.setTextColor(colorSelectedText);
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(colorBackground));
                btn.setTypeface(null, Typeface.NORMAL);
                btn.setTextColor(colorUnselectedText);
            }
        }
    }

    //Filtrar pelos botões, através da propriedade de tipo de atividade (type)
    private void filterListByButton(MaterialButton btn) {
        int id = btn.getId();
        ArrayList<HistoryItem> filtered = new ArrayList<>();

        if (id == R.id.fltRunning) {
            for (HistoryItem it : allItems)
                if (it.type.equalsIgnoreCase("running")) filtered.add(it);
        } else if (id == R.id.fltBycicle) {
            for (HistoryItem it : allItems)
                if (it.type.equalsIgnoreCase("cycling") || it.type.equalsIgnoreCase("bike"))
                    filtered.add(it);
        } else if (id == R.id.fltWalking) {
            for (HistoryItem it : allItems)
                if (it.type.equalsIgnoreCase("walking")) filtered.add(it);
        } else {
            filtered.addAll(allItems);
        }

        adapter.updateList(filtered);
    }

    // ------------------------------------------------------------------------

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
