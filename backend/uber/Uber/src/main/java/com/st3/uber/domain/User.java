package com.st3.uber.domain;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role")
@Table(name = "users")

public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;
    @Column(nullable = false, unique = true)
    @Setter
    private String email;

    @Column(nullable = false)
    @Setter
    private String password;

    @Column(nullable = false)
    @Setter
    private String name;

    @Column(nullable = false)
    @Setter
    private String surname;

    @Column(nullable = false)
    @Setter
    private String phoneNumber;

    @Column(nullable = false)
    @Setter
    private String address;

    @Setter
    private String profileImage;

    @Column(nullable = false)
    @Setter
    private boolean blocked = false;

    @Setter
    private String blockReason;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter
    private List<Notification> notifications = new ArrayList<>();
}
