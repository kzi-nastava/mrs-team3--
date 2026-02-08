package com.st3.uber.dto.user;

public class UserDto {

    private Long id;
    private String firstName;
    private String lastName;

    public UserDto(Long id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
