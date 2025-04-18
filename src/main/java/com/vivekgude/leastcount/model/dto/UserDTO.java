package com.vivekgude.leastcount.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDTO {

    @Email(message = "Please provide a valid email address")
    private String emailId;

    @NotBlank(message = "Please provide a username")
    private String name;

    @Size(min = 8)
    private String password;
}
