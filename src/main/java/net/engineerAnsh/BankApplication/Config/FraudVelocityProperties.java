package net.engineerAnsh.BankApplication.Config;

import lombok.Getter;
import lombok.Setter;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fraud")
public class FraudVelocityProperties {

    // Now Spring will load: fraud.velocity-limits
    // into the map.
    private Map<TransactionType, Integer> velocityLimits;

}



// Example map created:-
//  {
//  TRANSFER = 5
//  WITHDRAW = 3
//  DEPOSIT = 10
//  }