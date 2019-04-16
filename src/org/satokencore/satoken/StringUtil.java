package org.satokencore.satoken;

import com.google.common.hash.Hashing;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bitcoinj.core.Base58;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;

public class StringUtil {

    public static String applySha256(String input) {
        String sha256hex = Hashing.sha256()
                .hashString(input, StandardCharsets.UTF_8)
                .toString();
        return sha256hex;
    }
    
    public static byte[] applySha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("Sha-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String applyECDSASig(PrivateKey privKey, String input) {
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
        return bytesToHex(output);
    }

    public static boolean verifyECDSASig(String pubHex, String data, String signature) {
        try {
            PublicKey pubKey = pubHexToKey(pubHex);
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(pubKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(hexToBytes(signature));
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
        BigInteger S = key.getS();
        byte[] S_bytes = S.toByteArray();
        String hexS = bytesToHex(S_bytes);
        String sepvt = adjustTo64(hexS).toUpperCase();
        return sepvt;
    }

    public static PublicKey pubHexToKey(String pubHex) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                    hexToBytes(pubHex));
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException ex) {
            Logger.getLogger(StringUtil.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    public static PrivateKey privHexToKey(String privHex) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                    hexToBytes(privHex));
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException ex) {
            Logger.getLogger(StringUtil.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    public static String pubKeyToHex(PublicKey pubKey) {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
                pubKey.getEncoded());
        return bytesToHex(x509EncodedKeySpec.getEncoded());
    }

    public static String getAddressOfPubHex(String pubHex) {
        try {
            ECPublicKey publicKey = (ECPublicKey) pubHexToKey(pubHex);
            String bcPub = getStringFromPubKey(publicKey);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] s1 = sha.digest(hexToBytes(bcPub));
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

            byte[] p1 = hexToBytes(bcPub);
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

    public static String adjustTo64(String s) {
        switch (s.length()) {
            case 62:
                return "00" + s;
            case 63:
                return "0" + s;
            case 64:
                return s;
            default:
                throw new IllegalArgumentException("not a valid key: " + s + "\nLength: " + s.length());
        }
    }

    public static byte[] hexToBytes(String s) {
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

    public static String padDifficulty(String difficulty) {
        String res = "";
        for (int i = difficulty.length(); i < 64; i++) {
            res += "0";
        }
        res += difficulty;
        return res.toLowerCase();
    }

    public static void SaveKeyPair(String path, String saveAs, KeyPair keyPair) throws IOException {
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Store Public Key.
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(
                publicKey.getEncoded());
        FileOutputStream fos = new FileOutputStream(path + "/" + saveAs + "public.key");
        fos.write(x509EncodedKeySpec.getEncoded());
        fos.close();

        // Store Private Key.
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                privateKey.getEncoded());
        fos = new FileOutputStream(path + "/" + saveAs + "private.key");
        fos.write(pkcs8EncodedKeySpec.getEncoded());
        fos.close();
    }

    public static KeyPair LoadKeyPair(String path, String loadAs)
            throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchProviderException {
        // Read Public Key.
        File filePublicKey = new File(path + "/" + loadAs + "public.key");
        FileInputStream fis = new FileInputStream(path + "/" + loadAs + "public.key");
        byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
        fis.read(encodedPublicKey);
        fis.close();

        // Read Private Key.
        File filePrivateKey = new File(path + "/" + loadAs + "private.key");
        fis = new FileInputStream(path + "/" + loadAs + "private.key");
        byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
        fis.read(encodedPrivateKey);
        fis.close();

        // Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }

    public static String dumpKeyPair(KeyPair keyPair) {
        PublicKey pub = keyPair.getPublic();
        PrivateKey priv = keyPair.getPrivate();
        String res = "";
        res += getHexString(pub.getEncoded()) + ",";
        res += getHexString(priv.getEncoded());
        return res;
    }

    public static String getHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static ECPrivateKey getPrivateKey(byte[] privateKeyRaw) {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256k1"));

            ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);
            ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(new BigInteger(privateKeyRaw), ecParameterSpec);

            ECPrivateKey privateKey = (ECPrivateKey) KeyFactory.getInstance("ECDSA").generatePrivate(ecPrivateKeySpec);

            return privateKey;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidParameterSpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static ECPublicKey getPublicKey(ECPrivateKey privKey) {
        try {
            BCECPrivateKey privateKey = (BCECPrivateKey) privKey;
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");

            BigInteger d = privateKey.getD();
            org.bouncycastle.jce.spec.ECParameterSpec ecSpec
                    = privateKey.getParameters();
            org.bouncycastle.math.ec.ECPoint Q = privateKey.getParameters().getG().multiply(d);

            org.bouncycastle.jce.spec.ECPublicKeySpec pubSpec = new org.bouncycastle.jce.spec.ECPublicKeySpec(Q, ecSpec);
            PublicKey publicKeyGenerated = keyFactory.generatePublic(pubSpec);
            return (ECPublicKey) publicKeyGenerated;
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean[] bytesToBits(byte[] data) {
        boolean[] bits = new boolean[data.length * 8];
        for (int i = 0; i < data.length; ++i) {
            for (int j = 0; j < 8; ++j) {
                bits[(i * 8) + j] = (data[i] & (1 << (7 - j))) != 0;
            }
        }
        return bits;
    }

    public static int bitsToInt(boolean[] bits) {
        int n = 0, l = bits.length;
        for (int i = 0; i < l; ++i) {
            n = (n << 1) + (bits[i] ? 1 : 0);
        }
        return n;
    }

    public static byte[] concatBytes(byte[] one, byte[] two) {
        byte[] combined = new byte[one.length + two.length];
        System.arraycopy(one, 0, combined, 0, one.length);
        System.arraycopy(two, 0, combined, one.length, two.length);
        return combined;
    }

    public static String compressPublicKey(ECPublicKey toCompress) {
        BigInteger x = toCompress.getW().getAffineX();
        BigInteger y = toCompress.getW().getAffineY();
        String prefix = y.testBit(0) ? "03" : "02";
        return prefix + StringUtil.adjustTo64(x.toString(16));
    }
    
    public static KeyPair xprvToKeyPair(String xprv) {
        /* Extended Private Key contains data about its:
            Version, 4 Bytes (0x0488ADE4 for Extended Private Keys)
            Private Key, 33 Bytes
            Chain Code, 32 Bytes
            Depth, 1 Byte (0x00 for Master Extended Private Key)
            Fingerprint, 4 Bytes (0x00000000 for Master Extended Private Key)
            Child Number, 4 Bytes (0x00000000 for Master Extended Private Key)
         */
        
        byte[] xprvBytes = Base58.decodeChecked(xprv);
        // First 4 Bytes are not used unless implementing Version Checking
//        byte[] depth = Arrays.copyOfRange(xprvBytes, 4, 5);
//        byte[] fingerprint = Arrays.copyOfRange(xprvBytes, 5, 9);
//        byte[] childNumber = Arrays.copyOfRange(xprvBytes, 9, 13);
//        byte[] chainCode = Arrays.copyOfRange(xprvBytes, 13, 45);
        // For Private Key Bytes, exclude 0x00 Byte (index 45 of payload bytes)
        byte[] privKeyBytes = Arrays.copyOfRange(xprvBytes, 46, 78);
        ECPrivateKey privKey = getPrivateKey(privKeyBytes);
        ECPublicKey pubKey = getPublicKey(privKey);
        return new KeyPair(pubKey, privKey);
    }
}
