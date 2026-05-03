package com.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name="companies")
@Getter
@Setter
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String industry;
    private String description;
    
    @OneToMany(mappedBy = "company")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<User> recruiters;
    public Company() {}

    public Company(String name) {
        this.name = name;
    }
}
