package com.st3.uber.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(name = "admins")
public class Admin extends User{
}
