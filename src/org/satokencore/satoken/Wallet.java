package org.satokencore.satoken;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.bitcoinj.core.Base58;

public class Wallet {

    public ECPrivateKey privKey;
    public ECPublicKey pubKey;
    private String address, privAddress;
    
    public HashMap<String, TransactionOutput> UTXOs = new HashMap<>();

    public Wallet() {
        generateKeyPair();
    }

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
            keyGen.initialize(ecSpec, random);
            KeyPair keyPair = keyGen.generateKeyPair();
            // Set the public and private keys from the keyPair
            privKey = (ECPrivateKey) keyPair.getPrivate();
            pubKey = (ECPublicKey) keyPair.getPublic();
            address = StringUtil.getAddressOfECPubKey(pubKey);
            privAddress = StringUtil.getAddressOfECPrivKey(privKey);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
    
    public int getBalance() {
        int balance = 0;
        for (Map.Entry<String, TransactionOutput> entry : Blockchain.UTXOs.entrySet()) {
            TransactionOutput UTXO = entry.getValue();
            if (UTXO.isMine(StringUtil.getAddressOfECPubKey(pubKey))) {
                UTXOs.put(UTXO.coinId, UTXO);
                balance += UTXO.value;
            }
        }
        return balance;
    }
    
    public Transaction sendFunds(String recipient, int value) {
        if (getBalance() < value) {
            System.out.println("Not enough funds to send the transaction.");
            return null;
        }
        
        // Creating List of Inputs
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
            TransactionOutput UTXO = entry.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.coinId));
            if (total > value) break;
        }
        
        // Building Transaction
        Transaction transaction = new Transaction(pubKey, recipient, value, inputs);
        transaction.generateSignature(privKey);
        
        inputs.forEach((input) -> {
            UTXOs.remove(input.transactionOutputId);
        });
        
        return transaction;
    }

    public boolean isAddressOfPubKey(String _address) {
        try {
            String bcPub = StringUtil.getStringFromPubKey(pubKey);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] s1 = sha.digest(StringUtil.hexStringToByteArray(bcPub));
            MessageDigest rmd = MessageDigest.getInstance("RipeMD160", "BC");
            byte[] r1 = rmd.digest(s1);
            byte[] r2 = new byte[r1.length + 1];
            r2[0] = 0;
            for (int i = 0; i < r1.length; i++) {
                r2[i + 1] = r1[i];
            }
            byte[] s2 = sha.digest(r2);
            byte[] s3 = sha.digest(s2);
            byte[] decodedAdd = Base58.decode(_address);
            byte[] checksum = Arrays.copyOfRange(decodedAdd, decodedAdd.length - 4, decodedAdd.length);
            for (int i = 0; i < 4; i++) {
                if (s3[i] != checksum[i]) {
                    return false;
                }
            }
            return true;
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            System.out.println("Failed to match Checksums.");
            throw new RuntimeException(e);
        }

    }

    public String getAddress() {
        return address;
    }

    public String getPrivAddress() {
        return privAddress;
    }
}
