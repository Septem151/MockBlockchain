package org.satokencore.satoken;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.ArrayList;
import org.bitcoinj.core.Base58;

public class StringUtil {

    public static String applySha256(String input) {
        String sha256hex = Hashing.sha256()
                .hashString(input, StandardCharsets.UTF_8)
                .toString();
        return sha256hex;
    }

    public static byte[] applyECDSASig(PrivateKey privKey, String input) {
        Signature dsa;
        byte[] output = new byte[0];
        try {
            dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privKey);
            byte[] strByte = input.getBytes();
            dsa.update(strByte);
            byte[] realSig = dsa.sign();
            output = realSig;
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException e) {
            System.out.println("Error applying ECDSA Sig.");
            throw new RuntimeException(e);
        }
        return output;
    }

    public static boolean verifyECDSASig(PublicKey pubKey, String data, byte[] signature) {
        try {
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(pubKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException e) {
            System.out.println("Error verifying ECDSA Sig.");
            throw new RuntimeException(e);
        }
    }

    public static String getStringFromPubKey(Key key) {
        String keyStr;
        ECPublicKey ecKey = (ECPublicKey) key;
        ECPoint pt = ecKey.getW();
        String sx = adjustTo64(pt.getAffineX().toString(16)).toUpperCase();
        String sy = adjustTo64(pt.getAffineY().toString(16)).toUpperCase();
        keyStr = "04" + sx + sy;
        return keyStr;
    }

    public static String getStringFromPrivKey(ECPrivateKey key) {
        ECPrivateKey epvt = (ECPrivateKey) key;
        String sepvt = adjustTo64(epvt.getS().toString(16)).toUpperCase();
        return sepvt;
    }

    public static String getAddressOfECPubKey(ECPublicKey key) {
        try {
            String bcPub = getStringFromPubKey(key);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] s1 = sha.digest(hexStringToByteArray(bcPub));
            MessageDigest rmd = MessageDigest.getInstance("RipeMD160", "BC");
            byte[] r1 = rmd.digest(s1);
            byte[] r2 = new byte[r1.length + 1];
            r2[0] = 0;
            for (int i = 0; i < r1.length; i++) {
                r2[i + 1] = r1[i];
            }
            byte[] s2 = sha.digest(r2);
            byte[] s3 = sha.digest(s2);

            byte[] a1 = new byte[25];
            for (int i = 0; i < r2.length; i++) {
                a1[i] = r2[i];
            }
            for (int i = 0; i < 4; i++) {
                a1[21 + i] = s3[i];
            }
            String address = Base58.encode(a1);
            return address;
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            System.out.println("Failure generating Address from PubKey.");
            throw new RuntimeException(ex);
        }
    }

    public static String getAddressOfECPrivKey(ECPrivateKey key) {
        try {
            String bcPub = getStringFromPrivKey(key);
            bcPub = "80" + bcPub; // 2
            
            byte[] p1 = hexStringToByteArray(bcPub);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] s1 = sha.digest(p1); // 3
            
            byte[] s2 = sha.digest(s1); // 4

            byte[] checksum = new byte[4]; // 5
            for (int i = 0; i < checksum.length; i++) {
                checksum[i] = s2[i];
            }
            
            byte[] a1 = new byte[p1.length + checksum.length]; // 6
            System.arraycopy(p1, 0, a1, 0, p1.length);
            System.arraycopy(checksum, 0, a1, p1.length, checksum.length);
            
            String address = Base58.encode(a1);
            return address;
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("Failure generating Address from PrivKey.");
            throw new RuntimeException(ex);
        }
    }

    private static String adjustTo64(String s) {
        switch (s.length()) {
            case 62:
                return "00" + s;
            case 63:
                return "0" + s;
            case 64:
                return s;
            default:
                throw new IllegalArgumentException("not a valid key: " + s);
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //Tacks in array of transactions and returns a merkle root.
    public static String getMerkleRoot(ArrayList<Transaction> transactions) {
        int count = transactions.size();
        ArrayList<String> previousTreeLayer = new ArrayList<>();
        for (Transaction transaction : transactions) {
            previousTreeLayer.add(transaction.transactionId);
        }
        ArrayList<String> treeLayer = previousTreeLayer;
        while (count > 1) {
            treeLayer = new ArrayList<>();
            for (int i = 1; i < previousTreeLayer.size(); i++) {
                treeLayer.add(applySha256(previousTreeLayer.get(i - 1) + previousTreeLayer.get(i)));
            }
            count = treeLayer.size();
            previousTreeLayer = treeLayer;
        }
        String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0) : "";
        return merkleRoot;
    }
}
