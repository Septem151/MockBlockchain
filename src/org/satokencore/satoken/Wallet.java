package org.satokencore.satoken;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.bitcoinj.core.Base58;
import static org.satokencore.satoken.StringUtil.*;

public class Wallet {

    //public String hexKeyPair;
    public byte[] masterKey;
    public ArrayList<KeyPair> keys;
    private int highestBalanceIndex;
    private String mnemonicWords;
    public HashMap<String, ECPrivateKey> unspentKeys;

    public transient HashMap<String, TransactionOutput> UTXOs;

    public Wallet() {
        highestBalanceIndex = -1;
        UTXOs = new HashMap<>();
        keys = new ArrayList<>();
        unspentKeys = new HashMap<>();
    }

    public void init() {
        mnemonicWords = generateMnemonic();
        byte[] seed = mnemonicToSeed(mnemonicWords);
        masterKey = seedToMasterKey(seed);
        for (int i = 0; i < 1; i++) {
            String xprv = CKDpriv(masterKey, i, true);
            keys.add(xprvToKeyPair(xprv));
        }
        System.out.println("Seed Phrase: " + mnemonicWords);
    }

    public void init(String mnemonicWords) {
        byte[] seed = mnemonicToSeed(mnemonicWords);
        masterKey = seedToMasterKey(seed);
        for (int i = 0; i < 1; i++) {
            String xprv = CKDpriv(masterKey, i, true);
            keys.add(xprvToKeyPair(xprv));
        }
    }

    private String generateMnemonic() {
        // Generate 128-bit Random Number for Entropy
        SecureRandom sr = new SecureRandom();
        byte[] ENT = new byte[16];
        sr.nextBytes(ENT);

        // Hash the Entropy value
        byte[] HASH = applySha256(ENT);

        // Copy first 4 bits of Hash as Checksum
        boolean[] CS = Arrays.copyOfRange(bytesToBits(HASH), 0, 4);

        // Add Checksum to the end of Entropy bits
        boolean[] ENT_CS = Arrays.copyOf(bytesToBits(ENT), bytesToBits(ENT).length + CS.length);
        System.arraycopy(CS, 0, ENT_CS, bytesToBits(ENT).length, CS.length);

        // Split ENT_CS into groups of 11 bits and creates String array for
        // mnemonicWords
        String mnemonic = "";
        for (int i = 0; i < 12; i++) {
            boolean[] numBits = Arrays.copyOfRange(ENT_CS, i * 11, i * 11 + 11);
            mnemonic += Driver.wordList.get(bitsToInt(numBits));
            if (i < 11) {
                mnemonic += " ";
            }
        }
        return mnemonic;
    }

    private byte[] PBKDF2(String mnemonic, String salt) {
        String fixedSalt = "mnemonic" + salt;
        return PBKDF2(mnemonic, fixedSalt, 2048, 64);
    }

    private byte[] PBKDF2(String password, String salt, int iterations, int keysize) {
        // Keysize represented in bytes
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations,
                    keysize * 8);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            return f.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException(ex);
        }
    }

    private byte[] seedToMasterKey(byte[] seed) {
        return HMACSHA512("Bitcoin seed".getBytes(StandardCharsets.UTF_8), seed);
    }

    private byte[] mnemonicToSeed(String mnemonic) {
        byte[] seed = PBKDF2(mnemonic, "");
        return seed;
    }

    private byte[] HMACSHA512(byte[] secretKey, byte[] message) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey, "HmacSHA512");
            sha512_HMAC.init(secret_key);
            sha512_HMAC.update(message);
            byte[] hash = sha512_HMAC.doFinal();
            return hash;
        } catch (IllegalStateException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String CKDpriv(byte[] masterKey, int i, boolean hardened) {
        byte[] k_par = Arrays.copyOfRange(masterKey, 0, masterKey.length / 2);
        byte[] c_par = Arrays.copyOfRange(masterKey, masterKey.length / 2, masterKey.length);
        i = (hardened) ? 0x80000000 | i : i;
        byte[] ser32_i = hexToBytes(String.format("%08X", i));
        ECPrivateKey tempPriv = getPrivateKey(k_par);
        ECPublicKey tempPub = getPublicKey(tempPriv);
        byte[] data;
        if (hardened) {
            data = concatBytes(new byte[]{0x00}, concatBytes(k_par, ser32_i));
        } else {
            data = concatBytes(hexToBytes(compressPublicKey(tempPub)), ser32_i);
        }
        byte[] I = HMACSHA512(c_par, data);
        byte[] I_L = Arrays.copyOfRange(I, 0, I.length / 2);
        byte[] I_R = Arrays.copyOfRange(I, I.length / 2, I.length);
        byte[] k_i = hexToBytes(tweakAdd(bytesToHex(I_L), bytesToHex(k_par)));
        byte[] c_i = I_R;
        byte[] c_I = concatBytes(k_i, c_i);
        byte[] hash160 = Hash160(hexToBytes(compressPublicKey(tempPub)));
        String fingerprint = bytesToHex(Arrays.copyOfRange(hash160, 0, 4));
        String c_xprv = serializeExtendedPrivateKey("01", fingerprint, bytesToHex(ser32_i), c_I);
        return c_xprv;
    }

    private final BigInteger secp256k1_n = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    public String tweakAdd(String n1, String n2) {
        BigInteger num1 = new BigInteger(n1, 16);
        BigInteger num2 = new BigInteger(n2, 16);
        BigInteger res = num1.add(num2);
        res = res.mod(secp256k1_n);
        return adjustTo64(res.toString(16));
    }

    private String serializeExtendedPrivateKey(String depth, String fingerprint, String childNum, byte[] masterKey) {
        byte[] v = hexToBytes("0488ADE4");
        byte[] d = hexToBytes(depth);
        byte[] fp = hexToBytes(fingerprint);
        byte[] cN = hexToBytes(childNum);
        byte[] cC = Arrays.copyOfRange(masterKey, masterKey.length / 2, masterKey.length);
        byte[] pk = concatBytes(new byte[]{0x00}, Arrays.copyOfRange(masterKey, 0, masterKey.length / 2));
        byte[] payload = concatBytes(v, concatBytes(d, concatBytes(fp, concatBytes(cN, concatBytes(cC, pk)))));
        byte[] checksum = Arrays.copyOfRange(applySha256(applySha256(payload)), 0, 4);
        payload = concatBytes(payload, checksum);
        return Base58.encode(payload);
    }

//    private String serializeExtendedPublicKey(String depth, String fingerprint, String childNum, byte[] masterKey) {
//        byte[] v = hexToBytes("0488B21E");
//        byte[] d = hexToBytes(depth);
//        byte[] fp = hexToBytes(fingerprint);
//        byte[] cN = hexToBytes(childNum);
//        byte[] cC = Arrays.copyOfRange(masterKey, masterKey.length / 2, masterKey.length);
//        ECPrivateKey tempPriv = getPrivateKey(Arrays.copyOfRange(masterKey, 0, masterKey.length));
//        ECPublicKey pubKey = getPublicKey(tempPriv);
//        byte[] pk = hexToBytes(compressPublicKey(pubKey));
//        byte[] payload = concatBytes(v, concatBytes(d, concatBytes(fp, concatBytes(cN, concatBytes(cC, pk)))));
//        byte[] checksum = Arrays.copyOfRange(applySha256(applySha256(payload)), 0, 4);
//        payload = concatBytes(payload, checksum);
//        return Base58.encode(payload);
//    }
    private static byte[] Hash160(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("RipeMD160", "BC");
            return digest.digest(applySha256(data));
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void generateKeyPair() {
        int freshIndex = keys.size();
        String xprv = CKDpriv(masterKey, freshIndex, true);
        keys.add(xprvToKeyPair(xprv));
    }

    public String getChangeKey() {
        String changePubHex;
        if(this == Blockchain.coinbase) {
            changePubHex = pubKeyToHex(
                    keys.get(0).getPublic());
        }else if (highestBalanceIndex != keys.size() - 1) {
            highestBalanceIndex++;
            changePubHex = pubKeyToHex(
                    keys.get(highestBalanceIndex).getPublic());
        } else {
            generateKeyPair();
            changePubHex = pubKeyToHex(
                    keys.get(keys.size() - 1).getPublic());
        }
        return changePubHex;
    }

    public int getBalance(Blockchain blockchain) {
        unspentKeys.clear();
        int balance = 0;
        for (Map.Entry<String, TransactionOutput> entry : blockchain.UTXOs.entrySet()) {
            TransactionOutput UTXO = entry.getValue();
            for (KeyPair keyPair : keys) {
                String address = getAddressOfPubHex(pubKeyToHex(keyPair.getPublic()));
                if (UTXO.isMine(address)) {
                    UTXOs.put(UTXO.coinId, UTXO);
                    balance += UTXO.value;
                    highestBalanceIndex = keys.indexOf(keyPair);
                    unspentKeys.put(UTXO.coinId, (ECPrivateKey) keys.get(highestBalanceIndex).getPrivate());
                }
            }
        }
        return balance;
    }

    public Transaction sendFunds(String recipient, int value, Blockchain blockchain) {
        if (getBalance(blockchain) < value) {
            System.out.println("Not enough funds to send the transaction.");
            return null;
        }

        // Creating List of Inputs and corresponding PrivKeys
        ArrayList<ECPrivateKey> privKeys = new ArrayList<>();
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
            TransactionOutput UTXO = entry.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.coinId));
            privKeys.add(unspentKeys.get(UTXO.coinId));
            if (total > value) {
                break;
            }
        }

        // Create Recipients, only able to send many -> one at this time
        LinkedHashMap<String, Integer> recipients = new LinkedHashMap<>();
        // Is recipient an Address or Public Key?
        if (recipient != null) {
            if (recipient.length() >= 26 && recipient.length() <= 35) {
                recipients.put(recipient, value);
            } else {
                recipients.put(getAddressOfPubHex(recipient), value);
            }
        }

        // Building Transaction
        String changePubHex = (total != value) ? this.getChangeKey() : null;
        Transaction transaction = new Transaction(recipients, inputs, changePubHex);

        // Sign Transaction with Private Keys
        ECPrivateKey[] sigKeys = new ECPrivateKey[privKeys.size()];
        for (int i = 0; i < privKeys.size(); i++) {
            sigKeys[i] = privKeys.get(i);
        }
        transaction.generateSignature(sigKeys);

        // Remove all Inputs Used from internal collection
        inputs.forEach((input) -> {
            UTXOs.remove(input.transactionOutputId);
        });

        return transaction;
    }

//    public boolean isAddressOfPubKey(String _address) {
//        try {
//            String bcPub = getStringFromPubKey(this.getPubKey());
//            MessageDigest sha = MessageDigest.getInstance("SHA-256");
//            byte[] s1 = sha.digest(hexToBytes(bcPub));
//            MessageDigest rmd = MessageDigest.getInstance("RipeMD160", "BC");
//            byte[] r1 = rmd.digest(s1);
//            byte[] r2 = new byte[r1.length + 1];
//            r2[0] = 0;
//            for (int i = 0; i < r1.length; i++) {
//                r2[i + 1] = r1[i];
//            }
//            byte[] s2 = sha.digest(r2);
//            byte[] s3 = sha.digest(s2);
//            byte[] decodedAdd = Base58.decode(_address);
//            byte[] checksum = Arrays.copyOfRange(decodedAdd, decodedAdd.length - 4, decodedAdd.length);
//            for (int i = 0; i < 4; i++) {
//                if (s3[i] != checksum[i]) {
//                    return false;
//                }
//            }
//            return true;
//        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
//            System.out.println("Failed to match Checksums.");
//            throw new RuntimeException(e);
//        }
//
//    }
    // ONLY USED BY THE COINBASE WALLET
    public void saveWallet(String path, String saveAs) throws IOException {
        SaveKeyPair(path, saveAs, keys.get(0));
    }
}
