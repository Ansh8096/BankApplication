package net.engineerAnsh.BankApplication.Entity;

import lombok.NonNull;
import org.springframework.stereotype.Component;



public class Transactions {

    @NonNull
    private String transactionId;

    private String transactionDescription;


}
