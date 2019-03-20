package org.satokencore.satoken;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.HashMap;

public class Blockchain {

    @Expose
    private final ArrayList<Block> blocks;
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>();
    public static final int minimumTransaction = 1;
    public static Wallet coinbase;

    public Blockchain() {
        blocks = new ArrayList<>();
        coinbase = new Wallet();
        // Create Raw Genesis TX
        Transaction genesisTransaction = new Transaction(coinbase.pubKey, coinbase.getAddress(), 2000000000, null);
        genesisTransaction.generateSignature(coinbase.privKey);
        genesisTransaction.transactionId = "0";
        genesisTransaction.utxos.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId));
        Blockchain.UTXOs.put(genesisTransaction.utxos.get(0).coinId, genesisTransaction.utxos.get(0));

        // Mine Genesis Block
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        this.addBlock(genesis);
    }

    public void addBlock(Block block) {
        blocks.add(block);
    }

    public ArrayList<Block> getBlocks() {
        return blocks;
    }

    public Block getBlock(int index) {
        return blocks.get(index);
    }

    public Block getLastBlock() {
        return blocks.get(blocks.size() - 1);
    }

    public int size() {
        return blocks.size();
    }

    public String getSaveData() {
        return new GsonBuilder().registerTypeAdapter(ECPublicKey.class, new InterfaceAdapter<ECPublicKey>())
                .registerTypeAdapter(ECPrivateKey.class, new InterfaceAdapter<ECPrivateKey>())
                .create().toJson(this);
    }

    @Override
    public String toString() {
        String blockchainJson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create().toJson(this);
        return blockchainJson;
    }

}
