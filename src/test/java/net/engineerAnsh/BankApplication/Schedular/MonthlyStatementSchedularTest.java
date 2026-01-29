package net.engineerAnsh.BankApplication.Schedular;

import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "JWT_SECRET_KEY=THIS_IS_A_TEST_SECRET_KEY_123456789012345",
        "JWT_TOKEN_TIME=565"
})
@ActiveProfiles("test")
class MonthlyStatementSchedularTest {

    @Autowired
    private MonthlyStatementSchedular statementSchedular;

    private Environment env;

//    @Test
//    void checkMailConfig() {
//        System.out.println("MAIL USER: " + env.getProperty("spring.mail.username"));
//        System.out.println("MAIL PASS EXISTS: " + (env.getProperty("spring.mail.password") != null));
//    }

    @Test
    @Transactional
    public void getStatementTest() throws MessagingException {
//        statementSchedular.sendMonthlyStatementsManually();
    }
}
