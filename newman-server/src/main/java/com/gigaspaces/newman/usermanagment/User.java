package com.gigaspaces.newman.usermanagment;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import java.util.Base64;
import java.util.Objects;

@Entity
public class User {

    @Id
    private String id;
    @Indexed(unique=true)
    private String username;
    private String password;
    private String role;

    public User() {
    }

    public User(String username, String password, String role) {
        this.username = username;
        this.role = role;

        Base64.Encoder encoder = Base64.getEncoder();
        this.password = encoder.encodeToString(password.getBytes());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDecodedPassword() {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decodedBytes = decoder.decode(this.password);
        return new String(decodedBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username) && Objects.equals(password, user.password) && Objects.equals(role, user.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, role);
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password.substring(0,1) + "****" + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
