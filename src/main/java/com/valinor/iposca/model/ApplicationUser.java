package com.valinor.iposca.model;

public class ApplicationUser {
    private int userID;
    private String username;
    private String password;
    private String role;
    private String createdAt;

    // Empty constructor
    public ApplicationUser(){
    }

    // Full constructor for when we have all the data at once
    public ApplicationUser(int userID, String username, String password,
                     String role, String createdAt) {
        this.userID = userID;
        this.username = username;
        this.password = password;
        this.role = role;
        this.createdAt = createdAt;
    }


    public int getUserID(){return userID;}

    public void setUserID(int userID){this.userID = userID;}

    public String getUsername(){return username;}

    public void setUsername(String username){this.username = username;}

    public String getPassword(){return password;}

    public void setPassword(String password){this.password = password;}

    public String getRole(){return role;}

    public void setRole(String role){this.role = role;}

    public String getCreatedAt(){return createdAt;}

    public void setCreatedAt(String createdAt){this.createdAt = createdAt;}
}
