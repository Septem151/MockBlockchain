package org.satokencore.satoken;

import com.google.gson.annotations.Expose;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Transaction {

    @Expose
    public String transactionId;

    //public LinkedHashMap<ECPublicKey, Integer> senders;
    public LinkedHashMap<String, Integer> recipients;

    public int valueTransacted;

    public String[] signatures;

    private ECPublicKey[] pubKeys;
    private String changePubHex;

    @Expose
    public ArrayList<TransactionInput> inputs = new ArrayList<>();

    @Expose
    public ArrayList<TransactionOutput> utxos = new ArrayList<>();

    private static int sequence = 0;

    public Transaction(LinkedHashMap<String, Integer> recipients, ArrayList<TransactionInput> inputs, String changePubHex) {
        if (inputs != null) {
            signatures = new String[inputs.size()];
            pubKeys = new ECPublicKey[inputs.size()];
        } else {
            signatures = new String[1];
            pubKeys = new ECPublicKey[1];
        }
        this.recipients = recipients;
        this.inputs = inputs;
        valueTransacted = 0;
        this.changePubHex = changePubHex;
        transactionId = calculateHash();
    }

    private String calculateHash() {
        sequence++;
        String from = "";
        String to = "";
        if (inputs == null) {
            return "0";
        }
        for (TransactionInput input : inputs) {
            from += String.valueOf(input.transactionOutputId);
        }
        for (Map.Entry<String, Integer> entry : recipients.entrySet()) {
            to += entry.getKey();
            to += String.valueOf(entry.getValue());
            valueTransacted += entry.getValue();
        }
        return StringUtil.applySha256(
                from
                + to
                + Integer.toString(valueTransacted)
                + ((changePubHex != null) ? changePubHex : "")
                + sequence
        );
    }

    public void generateSignature(ECPrivateKey[] privKeys) {
        for (int i = 0; i < privKeys.length; i++) {
            pubKeys[i] = StringUtil.getPublicKey(privKeys[i]);
            signatures[i] = StringUtil.applyECDSASig(privKeys[i], transactionId);
        }
    }

    public boolean verifySignature() {
        for (int i = 0; i < signatures.length; i++) {
            String pubHex = StringUtil.pubKeyToHex(pubKeys[i]);
            if (!StringUtil.verifyECDSASig(pubHex, transactionId, signatures[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean processTransaction(Blockchain blockchain) {
        if (!verifySignature()) {
            System.out.println("Transaction Signature failed to verify.");
            return false;
        }

        // Gather Unspent Transaction Inputs
        inputs.forEach((input) -> {
            input.UTXO = blockchain.UTXOs.get(input.transactionOutputId);
        });

        // Check transaction is valid
        if (getInputsValue() < Blockchain.minimumTransaction) {
            System.out.println("Transaction Inputs too small: " + getInputsValue());
            return false;
        }

        // Generate TransactionOutputs
        int change = getInputsValue() - valueTransacted;
        for (Map.Entry<String, Integer> entry : recipients.entrySet()) {
            utxos.add(new TransactionOutput(entry.getKey(), entry.getValue(), transactionId));
        }
        if (changePubHex != null) {
            utxos.add(new TransactionOutput(StringUtil.getAddressOfPubHex(changePubHex), change, transactionId));
        }

        // Add Outputs to UTXO List
        utxos.forEach((output) -> {
            blockchain.UTXOs.put(output.coinId, output);
        });

        // Remove TransactionInputs from UTXO List
        for (TransactionInput input : inputs) {
            if (input.UTXO == null) {
                continue;
            }
            blockchain.UTXOs.remove(input.UTXO.coinId);
        }

        return true;
    }

    public int getInputsValue() {
        int total = 0;
        for (TransactionInput input : inputs) {
            if (input.UTXO == null) {
                continue;
            }
            total += input.UTXO.value;
        }
        return total;
    }

//    public int getOutputsValue() {
//        int total = 0;
//        for (TransactionOutput utxo : utxos) {
//            total += utxo.value;
//        }
//        return total;
//    }
}
