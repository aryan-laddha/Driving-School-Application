package com.example.drivingschool.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor // ✅ adds constructor with all fields
@NoArgsConstructor
public class UserResponseDto {
    private Long id;
    private String name;
    private String username;
    private String contact;
    private String role;
    private String licenseNumber;
    private boolean access;  // primitive boolean
    private boolean deleted; // primitive boolean
}
