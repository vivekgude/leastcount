package com.vivekgude.leastcount.model.dao;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users", schema = "public")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email_id", unique = true, nullable = false)
    private String emailId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "password", nullable = false)
    private String password;

    public User(String emailId, String name, String password) {
        this.emailId = emailId;
        this.name = name;
        this.password = password;
    }
}
