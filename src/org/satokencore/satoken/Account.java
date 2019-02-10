package org.satokencore.satoken;

public class Account {

    private String name;
    private String password;
    private Wallet wallet;
    private static int sequence = 0;
    public String id;

    public Account() {
        init();
        id = calculateHash();
    }

    public void init() {
        boolean confirmed = false;
        while (!confirmed) {
            System.out.println("New Account");
            System.out.print("Name: ");
            name = Driver.scan.nextLine();

            System.out.print("Password: ");
            password = Driver.scan.nextLine();

            System.out.print("Repeat Password: ");
            confirmed = (Driver.scan.nextLine().equals(password));

            if (!confirmed) {
                System.out.println("Passwords do not match.");
            } else {
                System.out.println("Proceed to create wallet? (Y/n)");
                switch (Driver.scan.nextLine().toUpperCase()) {
                    case "Y":
                        confirmed = true;
                        break;
                    case "N":
                        confirmed = false;
                        break;
                    default:
                        System.out.println("Not a recognized response.");
                        confirmed = false;
                        break;
                }
            }
            
            System.out.println("Configuring new wallet...");
            wallet = new Wallet();
        }
    }
    
    public boolean signIn() {
        System.out.print("Enter Account password: ");
        if (!password.equals(Driver.scan.nextLine())) {
            System.out.println("\nPasswords do not match.");
            return false;
        }
        return true;
    }
    
    private String calculateHash() {
        sequence++;
        return StringUtil.applySha256(
                name + password + String.valueOf(sequence)
        );
    }

    public void createTransaction(Block block) {
        String receivingAddress = "";
        int value = 0;
        boolean confirmed = false;
        while (!confirmed) {
            System.out.print("Receiving address: ");
            receivingAddress = Driver.scan.nextLine();
            System.out.print("Amount STC: ");
            try {
                value = Integer.parseInt(Driver.scan.nextLine());
                System.out.println("Confirm Transaction (Y/n)");
                switch (Driver.scan.nextLine().toUpperCase()) {
                    case "Y":
                        confirmed = true;
                        break;
                    case "N":
                        confirmed = false;
                        break;
                    default:
                        System.out.println("Not a recognized response.");
                        confirmed = false;
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Not a recognized response.");
            }
        }
        block.addTransaction(wallet.sendFunds(receivingAddress, value));
    }

    public void changeName() {
        System.out.print("Password: ");
        if (!Driver.scan.nextLine().equals(password)) {
            System.out.println("Passwords do not match.");
            return;
        }
        String _name = "";
        boolean confirmed = false;
        while (!confirmed) {
            System.out.print("Name: ");
            _name = Driver.scan.nextLine();
            System.out.println("Is this your name? (Y/n)");
            switch (Driver.scan.nextLine().toUpperCase()) {
                case "Y":
                    confirmed = true;
                    break;
                case "N":
                    confirmed = false;
                    break;
            }
        }
        this.name = _name;
    }

    public void changePassword() {
        System.out.print("Current Password: ");
        if (!Driver.scan.nextLine().equals(password)) {
            System.out.println("Passwords do not match.");
            return;
        }
        String _password = "";
        boolean confirmed = false;
        while (!confirmed) {
            System.out.print("New Password: ");
            _password = Driver.scan.nextLine();
            System.out.print("Repeat Password: ");
            confirmed = (Driver.scan.nextLine().equals(_password));
            if (!confirmed) {
                System.out.println("Passwords do not match.");
            }
        }
        this.password = _password;
    }

    public String getName() {
        return name;
    }

    public Wallet getWallet() {
        return wallet;
    }
}
