package net.engineerAnsh.BankApplication.Dto.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleRequest{

    @NotBlank
    private String roleName;
}
