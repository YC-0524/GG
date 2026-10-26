
package com.group17.game.Models;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "login_connections")
public class LoginConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "login_id")
    @NotBlank
    private String loginID;

    @Column(name = "user_id")
    @NotNull
    private long userID;

    @Email
    @NotBlank
    @Column(name = "email")
    private String email;

    @Column(name = "method")
    private LoginMethod method;

    @Column(name = "created")
    private Date created;

    public LoginConnection() {
    }

    // Full constructor
    public LoginConnection(String loginID, long userID,
            String email, LoginMethod method, Date created) {
        this.loginID = loginID;
        this.userID = userID;
        this.email = email;
        this.method = method;
        this.created = created;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLoginID() {
        return loginID;
    }

    public void setLoginID(String loginID) {
        this.loginID = loginID;
    }

    public long getUserID() {
        return userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LoginMethod getMethod() {
        return method;
    }

    public void setMethod(LoginMethod method) {
        this.method = method;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
