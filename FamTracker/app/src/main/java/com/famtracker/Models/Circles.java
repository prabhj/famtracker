package com.famtracker.Models;

/**
 * Created by darshan on 03/05/17.
 */

public class Circles {

    private String circleName,description,id;

    public Circles() {
    }

    public Circles(String circleName, String description, String id) {
        this.circleName = circleName;
        this.description = description;
        this.id = id;
    }

    public String getCircleName() {
        return circleName;
    }

    public void setCircleName(String circleName) {
        this.circleName = circleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
