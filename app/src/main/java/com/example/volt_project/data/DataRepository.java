package com.example.volt_project.data;

import android.content.Context;

import com.example.volt_project.data.models.Atividade;
import com.example.volt_project.data.models.Pessoa;

import java.util.List;

public class DataRepository {

    private final LocalDatabaseHelper db;

    public DataRepository(Context ctx) {
        db = new LocalDatabaseHelper(ctx);
    }


    // ---------- Pessoa ----------
    public void savePessoa(Pessoa p) {
        db.insertOrUpdatePessoa(p);
    }

    public Pessoa getPessoaByEmail(String email) {
        return db.getPessoaByEmail(email);
    }

    public List<Atividade> getDashboardInfo(String email){
        return db.getDashboardInfo(email);
    }



    public Atividade getUltimaAtividade(int pessoaId) {
        return db.getUltimaAtividade(pessoaId);
    }

    public List<Atividade> getAllAtividades(String email) {
        return db.getAllAtividades(email);
    }

    public double[] getWeeklyStats(int pessoaId) {
        return db.getWeeklyStats(pessoaId);
    }

    public void insertAtividade(Atividade a){
        db.insertAtividade(a);
    }
    public void deletePessoaByEmail(String email){
        db.deletePessoaByEmail(email);
    }

    public void clearAllTables(){
        db.clearAllTables();
    }

}
