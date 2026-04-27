package com.example.volt_project.dashboard.history;

public class HistoryItem {

    //Propriedades que uma atividade de corrida tem
    public String type;
    public String pace;
    public double speed;
    public double distance;
    public int calories;
    public double avg_speed;
    public String duration;
    public int steps;
    public String date;

    public HistoryItem(String type, String pace, double speed, double distance,
                       int calories, double avg_speed, String duration, int steps, String date) {
        this.type = type;
        this.pace = pace;
        this.speed = speed;
        this.distance = distance;
        this.calories = calories;
        this.avg_speed = avg_speed;
        this.duration = duration;
        this.steps = steps;
        this.date = date;
    }
}
