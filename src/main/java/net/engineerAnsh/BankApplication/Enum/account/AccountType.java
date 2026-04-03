package net.engineerAnsh.BankApplication.Enum.account;

public enum AccountType {
    CURRENT(true),
    SAVINGS(true),
    CHILD(true),
    LOAN(false);   // User can have multiple loan account...

    private final boolean singlePerUser;

    // When each enum constant is created, this constructor runs...
    // Example: CURRENT(true), means: AccountType.CURRENT.singlePerUser = true ...
    AccountType(boolean singlePerUser) {
        this.singlePerUser = singlePerUser;
    }

    public boolean isSinglePerUser() {
        return singlePerUser;
    }

}
