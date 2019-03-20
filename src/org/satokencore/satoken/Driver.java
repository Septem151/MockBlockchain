package org.satokencore.satoken;

import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Driver {

    public static final Scanner scan = new Scanner(System.in);
    public static Blockchain blockchain;
    public static File chainDirectory;
    public static File chainFile;

    /* 
     * Adjust these to suit your needs.
     * As of now though, there's no real way to guess which difficulty
     * is a good starting point for your target block time, but
     * setting the targetBlocksMined to a lower number will allow for
     * quicker testing of appropriate difficulty hex values.
     */
    public static String difficulty = "4C7FEF6BA544E18ADE6E72BF2CCA14C4749E0C35F9F6571C617AA3EC172C";
    public static long targetAvgBlockTime = 10000;
    public static int targetBlocksMined = 20;
    public static int blockRewardValue = 500;
    public static int rewardAdjustBlocks = 150;
    public static float rewardAdjust = 0.50f;
    public static int automineBlocks = 100;
    public static final ArrayList<Account> accounts = new ArrayList<>();
    public static int selectedAccount = 0;
    public static boolean running = true;
    public static boolean automine = false;
    public static Block currentBlock;

    public static void main(String[] args) {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }
        accounts.add(new Account());
        difficulty = StringUtil.padDifficulty(difficulty);

        // Create or Retrieve Blockchain
        loadBlockchain();

        printHelp();
        while (running) {
            System.out.print("\n >> ");
            switch (scan.nextLine().toLowerCase()) {
                case "transact":
                    if (currentBlock == null) {
                        currentBlock = new Block(blockchain.getLastBlock().hash);
                    }
                    accounts.get(selectedAccount).createTransaction(currentBlock);
                    System.out.println("Transaction will be included in next block.");
                    break;
                case "mine":
                    beginMining();
                    break;
                case "blockchain":
                    printChain();
                    break;
                case "automine":
                    automine = !automine;
                    System.out.printf("%-22s%s", "Autominer: ", (automine) ? "ON" : "OFF");
                    if (automine) {
                        System.out.print("\nAutomine quantity: ");
                        try {
                            int input = Integer.parseInt(scan.nextLine());
                            if (input <= 0) {
                                throw new NumberFormatException();
                            }
                            automineBlocks = input;
                        } catch (NumberFormatException e) {
                            System.out.println("Only positive, non-zero integers may be entered. Defaulting to " + automineBlocks);
                        }
                    }
                    break;
                case "wallet":
                    printWallet();
                    break;
                case "keys":
                    printKeyInfo();
                    break;
                case "newaccount":
                    newAccount();
                    break;
                case "switchaccount":
                    switchAccount();
                    break;
                case "help":
                    printHelp();
                    break;
                case "exit":
                    running = false;
                    break;
                default:
                    System.out.println("\"help\" for a list of commands.");
                    break;
            }
        }
        System.out.println("Saving Blockchain...");
        saveBlockchain();
    }

    public static void loadBlockchain() {
        System.out.println("Loading Chain Data...");
        chainDirectory = new File("blockchain");
        if (!chainDirectory.exists()) {
            chainDirectory.mkdir();
        }
        chainFile = new File(chainDirectory, "chaindata.json");
        try {
            if (!chainFile.createNewFile()) {
                BufferedReader br = new BufferedReader(new FileReader(chainFile));
                blockchain = new GsonBuilder().registerTypeAdapter(ECPublicKey.class, new InterfaceAdapter<ECPublicKey>())
                        .registerTypeAdapter(ECPrivateKey.class, new InterfaceAdapter<ECPrivateKey>())
                        .create().fromJson(br, Blockchain.class);
                br.close();
            } else {
                System.out.println("Creating new Blockchain...");
                blockchain = new Blockchain();
            }
        } catch (IOException ex) {
            Logger.getLogger(Driver.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Chain Data Loaded.");
    }

    public static void saveBlockchain() {
        System.out.println("Saving Chain Data...");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(chainFile, false));
            bw.write(blockchain.getSaveData());
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(Driver.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Chain Data Saved.");
    }

    public static void newAccount() {
        accounts.add(new Account());
        selectedAccount = accounts.size() - 1;
        System.out.println("Switch to account #" + selectedAccount + ".");
    }

    public static void switchAccount() {
        int prevAccount = selectedAccount;
        boolean confirmed = false;
        while (!confirmed) {
            System.out.print("Account #: ");
            try {
                System.out.println();
                selectedAccount = Integer.parseInt(scan.nextLine());
                if (selectedAccount < accounts.size()) {
                    System.out.println("Switching to account #" + selectedAccount + ".");
                    if (!accounts.get(selectedAccount).signIn()) {
                        selectedAccount = prevAccount;
                    } else {
                        System.out.println("Account unlocked.");
                    }
                    confirmed = true;
                } else {
                    System.out.println("Account does not exist.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Numbers only.");
            }
        }
    }

    public static void printHelp() {
        System.out.println("INTERACTION: ");
        System.out.printf("\t%-22s%s\n", "transact", "Create a transaction.");
        System.out.printf("\t%-22s%s\n", "mine", "Mine new block(s) to the blockchain.");
        System.out.printf("\t%-22s%s\n", "blockchain", "View the blockchain.");
        System.out.printf("\t%-22s%-22s%s\n", "automine", "Mines " + automineBlocks + " blocks.", (automine) ? "ON" : "OFF");

        System.out.println("\nACCOUNTS: ");
        System.out.printf("\t%-22s%s\n", "wallet", "View your wallet.");
        System.out.printf("\t%-22s%s\n", "keys", "View the wallet's Private/Public keypair.");
        System.out.printf("\t%-22s%s\n", "newaccount", "Create an account.");
        System.out.printf("\t%-22s%s\n", "switchaccount", "Switch accounts.");

        System.out.printf("\n\t%-22s%s\n", "help", "View these commands.");
        System.out.printf("\t%-22s%s\n", "exit", "Exit the program (clears all state).");
    }

    public static Wallet getWallet() {
        return accounts.get(selectedAccount).getWallet();
    }

    public static void printKeyInfo() {
        System.out.println(
                System.lineSeparator()
                + "Public Key: " + getWallet().getAddress() + System.lineSeparator()
                + "Private Key: " + getWallet().getPrivAddress()
        );
    }

    public static void printWallet() {
        System.out.println(
                System.lineSeparator()
                + "Account Id: " + selectedAccount + System.lineSeparator()
                + "Address: " + getWallet().getAddress() + System.lineSeparator()
                + "Balance: " + getWallet().getBalance() + " STC"
        );
    }

    public static void printChain() {
        System.out.println(blockchain + System.lineSeparator());
        System.out.println("Chain valid: " + isChainValid());
    }

    public static void beginMining() {
        int automineLeft = automineBlocks;
        long startTime;
        long elapsedTime;
        long avgBlockTime;
        int startIndex = blockchain.size() - 1;
        boolean mining = true;
        while ((automine && automineLeft > 0) || mining) {
            System.out.println("Mining new Block...");
            if (currentBlock == null) {
                addBlock(new Block(blockchain.getBlock(blockchain.size() - 1).hash));
            } else {
                addBlock(currentBlock);
            }
            currentBlock = null;
            automineLeft--;
            if (automineLeft <= 0) {
                mining = !mining;
            }
            if ((blockchain.size() + 1) % targetBlocksMined == 0) {
                startTime = blockchain.getBlock(startIndex).getTimestamp();
                elapsedTime = blockchain.getBlock(blockchain.size() - 1).getTimestamp() - startTime;
                avgBlockTime = elapsedTime / targetBlocksMined;
                System.out.println("Average Block Time: " + avgBlockTime);
                startIndex = blockchain.size() - 1;

                // Adjust difficulty
                BigInteger newDiff = hexToBigInt(difficulty);
                BigInteger prevDiff = hexToBigInt(difficulty);
                double diffAdjust = (double) avgBlockTime / targetAvgBlockTime;
                if (diffAdjust > 1.08) {
                    newDiff = newDiff.divide(BigInteger.valueOf(100));
                    newDiff = newDiff.multiply(BigInteger.valueOf(108));
                } else if (diffAdjust < 0.92) {
                    newDiff = newDiff.divide(BigInteger.valueOf(100));
                    newDiff = newDiff.multiply(BigInteger.valueOf(92));
                }
                difficulty = String.format("%064x", newDiff);
                if (newDiff.compareTo(prevDiff) < 0) {
                    System.out.println("Difficulty has increased to a hash of " + difficulty);
                } else if (newDiff.compareTo(prevDiff) > 0) {
                    System.out.println("Difficulty has decreased to a hash of " + difficulty);
                } else {
                    System.out.println("Difficulty has not changed. Current Difficulty: " + difficulty);
                }
            }

            // Adjust block reward value
            if ((blockchain.size() % rewardAdjustBlocks) == 0) {
                blockRewardValue = (int) (blockRewardValue * rewardAdjust);
            }

            if (automine) {
                continue;
            }
            boolean confirmed = false;
            while (!confirmed) {
                System.out.println("Continue mining? (Y/n)");
                switch (scan.nextLine().toUpperCase()) {
                    case "Y":
                        mining = true;
                        confirmed = true;
                        break;
                    case "N":
                        mining = false;
                        confirmed = true;
                        break;
                    default:
                        System.out.println("Not a recognized response.");
                        confirmed = false;
                        break;
                }
            }
        }

    }

    public static boolean isChainValid() {
        Block prevBlock, curBlock;

        for (int i = 1; i < blockchain.getBlocks().size(); i++) {
            curBlock = blockchain.getBlocks().get(i);
            prevBlock = blockchain.getBlocks().get(i - 1);

            if (!curBlock.hash.equals(curBlock.calculateHash())) {
                System.out.println("Current hashes not equal.");
                return false;
            }

            if (!curBlock.prevHash.equals(prevBlock.hash)) {
                System.out.println("Previous hashes not equal.");
                blockchain.getBlocks().remove(curBlock);
                return false;
            }
        }
        return true;
    }

    public static BigInteger hexToBigInt(String hexStr) {
        BigInteger bigInt = new BigInteger(hexStr, 16);
        return bigInt;
    }

    public static void addBlock(Block block) {
        block.mineBlock(getWallet().getAddress(), difficulty);
        blockchain.addBlock(block);
        System.out.println("Block #" + (blockchain.size() - 1) + " Mined! : " + blockchain.getBlock(blockchain.size() - 1).hash);
    }

}
