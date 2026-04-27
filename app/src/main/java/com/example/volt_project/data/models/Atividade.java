package com.example.volt_project.data.models;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class Atividade {
    public int id;

    @PropertyName("time") public String tempo;
    @PropertyName("distance") public double distancia;
    @PropertyName("pace") public double ritmo;
    @PropertyName("steps") public int pacos;
    @PropertyName("activityType") public String tipoAtividade;
    @PropertyName("calories") public int calorias;
    @PropertyName("personId") public int pessoaId;
    @PropertyName("date") public String data;

    public Atividade() {}
}