package net.engineerAnsh.BankApplication.Dto.StatementEmailContent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatementEmailContent {

    private String subject;
    private String body;
    private String fileName;

}
