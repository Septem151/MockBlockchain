package org.satokencore.satoken;

import com.google.gson.annotations.Expose;
import java.util.ArrayList;
import java.util.Date;

public class Block {

    @Expose
    public String hash, prevHash;

    public String merkleRoot;

    @Expose
    private final ArrayList<Transaction> transactions = new ArrayList<>();

    @Expose
    private final long timestamp;

    @Expose
    private int nonce;

    public Block(String prevHash) {
        this.prevHash = prevHash;
        timestamp = new Date().getTime();
        hash = calculateHash();
    }

    public String calculateHash() {
        String calculatedHash = StringUtil.applySha256(
                prevHash
                + Long.toString(timestamp)
                + Integer.toString(nonce)
                + merkleRoot);
        return calculatedHash;
    }

    public void mineBlock(String minerAddress, String difficulty) {
        Transaction blockReward = Driver.coinbase.sendFunds(minerAddress, Driver.blockRewardValue);
        this.addTransaction(blockReward);
        merkleRoot = StringUtil.getMerkleRoot(transactions);
        while (Driver.hexToBigInt(hash).compareTo(Driver.hexToBigInt(difficulty)) > 0) {
            nonce++;
            hash = calculateHash();
        }

    }

    public boolean addTransaction(Transaction transaction) {
        if (transaction == null) {
            return false;
        }
        if (!prevHash.equals("0")) {
            if (!transaction.processTransaction()) {
                System.out.println("Transaction failed to process.");
                return false;
            }
        }
        transactions.add(transaction);
        return true;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public Long getTimestamp() {
        return timestamp;
    }

}
