package org.satokencore.satoken;

import com.google.gson.annotations.Expose;
import java.security.PrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;

public class Transaction {

    @Expose
    public String transactionId;

    public ECPublicKey sender;
    public String recipient;

    public int value;

    public byte[] signature;

    public ArrayList<TransactionInput> inputs = new ArrayList<>();

    @Expose
    public ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0;

    public Transaction(ECPublicKey from, String to, int value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
        transactionId = calculateHash();
    }

    private String calculateHash() {
        sequence++;
        return StringUtil.applySha256(
                StringUtil.getStringFromPubKey(sender)
                + recipient
                + Integer.toString(value) + sequence
        );
    }

    public void generateSignature(PrivateKey privKey) {
        String data = StringUtil.getStringFromPubKey(sender)
                + recipient
                + Integer.toString(value);
        signature = StringUtil.applyECDSASig(privKey, data);
    }

    public boolean verifySignature() {
        String data = StringUtil.getStringFromPubKey(sender)
                + recipient
                + Integer.toString(value);
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    public boolean processTransaction() {
        if (verifySignature() == false) {
            System.out.println("Transaction Signature failed to verify.");
            return false;
        }

        // Gather Unspent Transaction Inputs
        inputs.forEach((input) -> {
            input.UTXO = Blockchain.UTXOs.get(input.transactionOutputId);
        });

        // Check transaction is valid
        if (getInputsValue() < Blockchain.minimumTransaction) {
            System.out.println("Transaction Inputs too small: " + getInputsValue());
            return false;
        }

        // Generate TransactionOutputs
        int change = getInputsValue() - value;
        outputs.add(new TransactionOutput(this.recipient, value, transactionId));
        outputs.add(new TransactionOutput(StringUtil.getAddressOfECPubKey(this.sender), change, transactionId));

        // Add Outputs to UTXO List
        outputs.forEach((output) -> {
            Blockchain.UTXOs.put(output.id, output);
        });

        // Remove TransactionInputs from UTXO List
        for (TransactionInput input : inputs) {
            if (input.UTXO == null) {
                continue;
            }
            Blockchain.UTXOs.remove(input.UTXO.id);
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

    public int getOutputsValue() {
        int total = 0;
        for (TransactionOutput output : outputs) {
            total += output.value;
        }
        return total;
    }

}
