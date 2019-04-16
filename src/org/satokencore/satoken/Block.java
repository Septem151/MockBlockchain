package org.satokencore.satoken;

import com.google.gson.annotations.Expose;
import java.util.ArrayList;
import java.util.Date;

public class Block {

    @Expose
    public String hash, prevHash;

    @Expose
    public String difficulty;

    @Expose
    public String merkleRoot;

    @Expose
    private long timestamp;

    @Expose
    private long nonce;

    @Expose
    private final ArrayList<Transaction> transactions = new ArrayList<>();

    public Block(String prevHash) {
        this.prevHash = prevHash;

        hash = calculateHash();
    }

    public String calculateHash() {
        String calculatedHash = StringUtil.applySha256(
                prevHash
                + Long.toHexString(nonce)
                + merkleRoot);
        return calculatedHash;
    }

    public void mineBlock(String minerPubHex, String difficulty, Blockchain blockchain) {
        Transaction blockReward = Blockchain.coinbase.sendFunds(minerPubHex, Driver.blockRewardValue, blockchain);
        this.addTransaction(blockReward, blockchain);
        merkleRoot = StringUtil.getMerkleRoot(transactions);
        hash = calculateHash();
        this.difficulty = difficulty;
        while (Driver.hexToBigInt(hash).compareTo(Driver.hexToBigInt(difficulty)) > 0) {
            nonce++;
            hash = calculateHash();
        }
        timestamp = new Date().getTime();
    }

    public boolean addTransaction(Transaction transaction, Blockchain blockchain) {
        if (transaction == null) {
            return false;
        }
        if (!prevHash.equals("0")) {
            if (!transaction.processTransaction(blockchain)) {
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
