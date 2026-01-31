package net.engineerAnsh.BankApplication.Dto.PublicDto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleRequest{

    @NotBlank
    private String roleName;
}
