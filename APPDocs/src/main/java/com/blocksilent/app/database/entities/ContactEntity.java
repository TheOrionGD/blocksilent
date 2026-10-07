package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "important_contacts")
public class ContactEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String phoneNumber;
    private boolean enabled;

    public ContactEntity(String name, String phoneNumber, boolean enabled) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.enabled = enabled;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
