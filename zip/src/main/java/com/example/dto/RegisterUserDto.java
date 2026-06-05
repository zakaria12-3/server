package com.example.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Username is required")
    private String username;

    private String role;

    private String companyName;
    private String companyLegalName;
    private String registrationNumber;
    private String taxIdentifier;
    private String industry;
    private String companySize;
    private String companyAddress;
    private String companyCity;
    private String companyCountry;
    private String companyWebsite;
    private String companyPhone;
    private String companyEmail;
    private String recruiterJobTitle;
    private String recruiterPhone;

}
