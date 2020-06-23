package com.example.whaledidyougo.ui.notification;

public class Caretaker {
    private int id;
    private String name, phone;

    public Caretaker(int id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }
}
