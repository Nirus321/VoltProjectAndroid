package com.example.volt_project.dashboard.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.volt_project.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<HistoryItem> items;

    public HistoryAdapter(Context context, ArrayList<HistoryItem> items) {
        this.context = context;
        this.items = items;
    }

    //Lista persistente de dados
    public static class ViewHolder extends RecyclerView.ViewHolder {


        ConstraintLayout mainLayout;
        ImageView imageActivity, magicBtn;
        TextView tvDate, tvKm, tvTime;
        TextView resLbl0, resVal0, resLbl1, resVal1, resLbl2, resVal2;
        boolean isExpanded = false;

        //Associa valores aos itens do XML
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mainLayout = itemView.findViewById(R.id.mainLayout);
            imageActivity = itemView.findViewById(R.id.imageActivity);
            magicBtn = itemView.findViewById(R.id.magicBtn);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvKm = itemView.findViewById(R.id.tvKm);
            tvTime = itemView.findViewById(R.id.tvTime);
            resLbl0 = itemView.findViewById(R.id.resLbl0);
            resVal0 = itemView.findViewById(R.id.resVal0);
            resLbl1 = itemView.findViewById(R.id.resLbl1);
            resVal1 = itemView.findViewById(R.id.resVal1);
            resLbl2 = itemView.findViewById(R.id.resLbl2);
            resVal2 = itemView.findViewById(R.id.resVal2);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }

    //Formatação da data
    private String formatDate(String isoDate) {
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd");
            Date d = iso.parse(isoDate);
            SimpleDateFormat out = new SimpleDateFormat("MMMM d, yyyy");
            return out.format(d);
        } catch (ParseException e) {
            return isoDate;
        }
    }

    //Definição de propriedades colocadas (imagem diferentes, ritmo para as corridas, velocidade para bicicleta)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        holder.tvDate.setText(formatDate(item.date));
        holder.tvKm.setText(String.format("%.2f km", item.distance));
        holder.tvTime.setText(item.duration != null ? item.duration : "");

        String type = item.type != null ? item.type.toLowerCase() : "";
        switch (type) {
            case "running":
                holder.imageActivity.setImageResource(R.drawable.ic_running_p);
                holder.resLbl0.setText("Calories");
                holder.resVal0.setText(String.format("%d kcal", item.calories));
                holder.resLbl1.setText("Pace");
                holder.resVal1.setText(item.pace != null ? item.pace + " /km" : "");
                holder.resLbl2.setText("Steps");
                holder.resVal2.setText(String.format("%d", item.steps));
                break;
            case "cycling":
            case "bike":
            case "biking":
                holder.imageActivity.setImageResource(R.drawable.ic_cycling_p);
                holder.resLbl0.setText("Calories");
                holder.resVal0.setText(String.format("%d kcal", item.calories));
                holder.resLbl1.setText("");
                holder.resVal1.setText("");
                holder.resLbl2.setText("Speed");
                holder.resVal2.setText(String.format("%.2f km/h", item.speed));
                break;
            case "walking":
            case "walk":
                holder.imageActivity.setImageResource(R.drawable.ic_walking_p);
                holder.resLbl0.setText("Calories");
                holder.resVal0.setText(String.format("%d kcal", item.calories));
                holder.resLbl1.setText("Speed");
                holder.resVal1.setText(String.format("%.2f km/h", item.speed));
                holder.resLbl2.setText("Steps");
                holder.resVal2.setText(String.format("%d", item.steps));
                break;
            default:
                holder.imageActivity.setImageResource(R.drawable.ic_running_p);
                holder.resLbl0.setText("Calories");
                holder.resVal0.setText(String.format("%d kcal", item.calories));
                holder.resLbl1.setText("");
                holder.resVal1.setText("");
                holder.resLbl2.setText("");
                holder.resVal2.setText("");
                break;
        }

        // Configurar o clique do magicBtn, expandir para ver mais detalhes
        holder.magicBtn.setOnClickListener(v -> {
            if (holder.isExpanded) {
                // Recolher
                holder.mainLayout.getLayoutParams().height = (int) (150 * v.getResources().getDisplayMetrics().density);
                setDetailsVisibility(holder, View.GONE);
            } else {
                // Expandir
                holder.mainLayout.getLayoutParams().height = (int) (230 * v.getResources().getDisplayMetrics().density);
                setDetailsVisibility(holder, View.VISIBLE);
            }

            // Animação de rotação
            holder.magicBtn.animate()
                    .rotationBy(180f)
                    .setDuration(300)
                    .start();

            holder.isExpanded = !holder.isExpanded;
            holder.mainLayout.requestLayout();
        });

        // Garantir estado inicial
        if (!holder.isExpanded) {
            holder.mainLayout.getLayoutParams().height = (int) (150 * holder.itemView.getResources().getDisplayMetrics().density);
            setDetailsVisibility(holder, View.GONE);
            holder.magicBtn.setRotation(0f);
        }
    }

    //Visibilidade de itens escondidos
    private void setDetailsVisibility(ViewHolder holder, int visibility) {
        holder.resLbl0.setVisibility(visibility);
        holder.resVal0.setVisibility(visibility);
        holder.resLbl1.setVisibility(visibility);
        holder.resVal1.setVisibility(visibility);
        holder.resLbl2.setVisibility(visibility);
        holder.resVal2.setVisibility(visibility);
    }

    //Obter número de itens
    @Override
    public int getItemCount() {
        return items.size();
    }

    //Atualização da lista
    public void updateList(List<HistoryItem> newList) {
        items.clear();
        items.addAll(newList);
        notifyDataSetChanged();
    }
}