package com.example.volt_project.data.models;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class Pessoa {
    public int id;

    @PropertyName("name") public String nome;
    @PropertyName("birthDate") public String dataNascimento;
    @PropertyName("sex") public String genero;
    @PropertyName("height") public double altura;
    @PropertyName("weight") public double peso;
    @PropertyName("email") public String email;

    public Pessoa() {}
}
