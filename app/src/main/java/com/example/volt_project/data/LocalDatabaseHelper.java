package com.example.volt_project.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.volt_project.data.models.Atividade;
import com.example.volt_project.data.models.Pessoa;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class LocalDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "volt_local.db";
    private static final int DB_VERSION = 1;

    private final Context context; // 👈 guardar o contexto para usar nas SharedPreferences

    public LocalDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS Pessoa (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Nome TEXT," +
                "DataNascimento TEXT," +
                "Genero TEXT," +
                "Altura REAL," +
                "Peso REAL," +
                "Email TEXT UNIQUE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS Atividade (" +
                "Id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Tempo TEXT," +
                "Distancia REAL," +
                "Ritmo REAL," +
                "Pacos INTEGER," +
                "TipoAtividade TEXT," +
                "Calorias INTEGER," +
                "PessoaId INTEGER," +
                "Data TEXT," +
                "FOREIGN KEY (PessoaId) REFERENCES Pessoa(Id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Atividade");
        db.execSQL("DROP TABLE IF EXISTS Pessoa");
        onCreate(db);
    }

    // -------- Pessoa --------
    public void insertOrUpdatePessoa(Pessoa p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("Nome", p.nome);
        v.put("DataNascimento", p.dataNascimento);
        v.put("Genero", p.genero);
        v.put("Altura", p.altura);
        v.put("Peso", p.peso);
        v.put("Email", p.email);

        int rows = db.update("Pessoa", v, "Email = ?", new String[]{p.email});
        if (rows == 0) db.insert("Pessoa", null, v);
    }

    public List<Atividade> getDashboardInfo(String email) {
        List<Atividade> atividades = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        int pessoaId = getPessoaIdByEmail(email);
        if (pessoaId == -1) {
            db.close();
            return atividades; // retorna lista vazia se o user não existir
        }

        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT Id, PessoaId, Data, Tempo, Distancia, Pace, Steps, ActivityType, Calories " +
                            "FROM Atividade WHERE PessoaId = ? ORDER BY Id DESC",
                    new String[]{String.valueOf(pessoaId)}
            );

            if (c.moveToFirst()) {
                do {
                    Atividade a = new Atividade();
                    a.id = c.getInt(c.getColumnIndexOrThrow("Id"));
                    a.pessoaId = c.getInt(c.getColumnIndexOrThrow("PessoaId"));
                    a.data = c.getString(c.getColumnIndexOrThrow("Data"));
                    a.tempo = c.getString(c.getColumnIndexOrThrow("Tempo"));
                    a.distancia = c.getDouble(c.getColumnIndexOrThrow("Distancia"));
                    a.ritmo = c.getDouble(c.getColumnIndexOrThrow("Pace"));
                    a.pacos = c.getInt(c.getColumnIndexOrThrow("Steps"));
                    a.tipoAtividade = c.getString(c.getColumnIndexOrThrow("ActivityType"));
                    a.calorias = c.getInt(c.getColumnIndexOrThrow("Calories"));

                    atividades.add(a);
                } while (c.moveToNext());
            }

        } finally {
            if (c != null) c.close();
            db.close();
        }

        return atividades;
    }

    public Pessoa getPessoaByEmail(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM Pessoa WHERE Email = ?", new String[]{email});
        Pessoa p = null;
        if (c.moveToFirst()) {
            p = new Pessoa();
            p.id = c.getInt(c.getColumnIndexOrThrow("Id"));
            p.nome = c.getString(c.getColumnIndexOrThrow("Nome"));
            p.dataNascimento = c.getString(c.getColumnIndexOrThrow("DataNascimento"));
            p.genero = c.getString(c.getColumnIndexOrThrow("Genero"));
            p.altura = c.getDouble(c.getColumnIndexOrThrow("Altura"));
            p.peso = c.getDouble(c.getColumnIndexOrThrow("Peso"));
            p.email = c.getString(c.getColumnIndexOrThrow("Email"));
        }
        c.close();
        return p;
    }

    // -------- Atividade --------
    /**
     * Novo método: Insere uma Atividade associando-a ao utilizador atual (pelo email guardado nas SharedPreferences)
     */
    public void insertAtividade(Atividade a) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("Tempo", a.tempo);
        v.put("Distancia", a.distancia);
        v.put("Ritmo", a.ritmo);
        v.put("Pacos", a.pacos);
        v.put("TipoAtividade", a.tipoAtividade);
        v.put("Calorias", a.calorias);
        v.put("PessoaId",a.pessoaId);
        v.put("Data", a.data);
        db.insert("Atividade", null, v);


    }

    public Atividade getUltimaAtividade(int pessoaId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM Atividade WHERE PessoaId = ? ORDER BY date(Data) DESC, Id DESC LIMIT 1",
                new String[]{String.valueOf(pessoaId)});
        Atividade a = null;
        if (c.moveToFirst()) {
            a = fromCursor(c);
        }
        c.close();
        return a;
    }

    public List<Atividade> getAllAtividades(String email) {
        int ID = getPessoaIdByEmail(email);
        List<Atividade> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM Atividade WHERE PessoaId = ? ORDER BY datetime(Data) DESC",
                new String[]{String.valueOf(ID)}
        );

        if (c.moveToFirst()) {
            do {
                list.add(fromCursor(c));
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    private Atividade fromCursor(Cursor c) {
        Atividade a = new Atividade();
        a.id = c.getInt(c.getColumnIndexOrThrow("Id"));
        a.tempo = c.getString(c.getColumnIndexOrThrow("Tempo"));
        a.distancia = c.getDouble(c.getColumnIndexOrThrow("Distancia"));
        a.ritmo = c.getDouble(c.getColumnIndexOrThrow("Ritmo"));
        a.pacos = c.getInt(c.getColumnIndexOrThrow("Pacos"));
        a.tipoAtividade = c.getString(c.getColumnIndexOrThrow("TipoAtividade"));
        a.calorias = c.getInt(c.getColumnIndexOrThrow("Calorias"));
        a.pessoaId = c.getInt(c.getColumnIndexOrThrow("PessoaId"));
        a.data = c.getString(c.getColumnIndexOrThrow("Data"));
        return a;
    }

    public double[] getWeeklyStats(int pessoaId) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        String start = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_WEEK, 6);
        String end = sdf.format(cal.getTime());

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT IFNULL(SUM(Pacos),0), IFNULL(SUM(Distancia),0) FROM Atividade " +
                        "WHERE PessoaId = ? AND date(Data) BETWEEN ? AND ?",
                new String[]{String.valueOf(pessoaId), start, end});

        double steps = 0, km = 0;
        if (c.moveToFirst()) {
            steps = c.getDouble(0);
            km = c.getDouble(1);
        }
        c.close();
        return new double[]{steps, km};
    }

    /**
     * Retorna o ID (chave primária) da Pessoa pelo email.
     * Retorna -1 se não for encontrada.
     */
    public int getPessoaIdByEmail(String email) {
        int pessoaId = -1;
        SQLiteDatabase db = getReadableDatabase();

        if (email == null) {
            return -1;
        }

        Cursor c = db.rawQuery("SELECT Id FROM Pessoa WHERE Email = ?", new String[]{email});
        if (c.moveToFirst()) {
            pessoaId = c.getInt(0);
        }
        c.close();
        return pessoaId;
    }

    // 🔹 Delete a Pessoa record and all their activities by email
    public void deletePessoaByEmail(String email) {
        if (email == null) return;

        // 1️⃣ Delete Pessoa itself
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("pessoa", "email = ?", new String[]{email});

        // 2️⃣ Delete related Atividades
        db.delete("atividade", "email = ?", new String[]{email});

        db.close();
    }

    public void clearAllTables() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM pessoa");
        db.execSQL("DELETE FROM atividade");
        db.close();
    }


}
