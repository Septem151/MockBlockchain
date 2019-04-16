package org.satokencore.satoken;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.io.IOException;
import java.security.interfaces.ECPrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Blockchain {

    @Expose
    private final ArrayList<Block> blocks;

    public HashMap<String, TransactionOutput> UTXOs;
    public static final int minimumTransaction = 1;
    public static Wallet coinbase = new Wallet();

    public Blockchain() {
        blocks = new ArrayList<>();
        UTXOs = new HashMap<>();
    }

    public void mineGenesisBlock() {
        // Create Raw Genesis TX
        LinkedHashMap<String, Integer> recipients = new LinkedHashMap<>();
        String coinbaseAddress = StringUtil.getAddressOfPubHex(
                StringUtil.pubKeyToHex(
                        coinbase.keys.get(0).getPublic()));
        int startValue = 144001993;
        recipients.put(coinbaseAddress, startValue);
        ECPrivateKey[] privKeys = new ECPrivateKey[] { (ECPrivateKey)coinbase.keys.get(0).getPrivate() };
        Transaction genesisTransaction = new Transaction(recipients, null, null);
        genesisTransaction.transactionId = "0";
        genesisTransaction.utxos.add(new TransactionOutput(coinbaseAddress, startValue, genesisTransaction.transactionId));
        genesisTransaction.generateSignature(privKeys);
        this.UTXOs.put(genesisTransaction.utxos.get(0).coinId, genesisTransaction.utxos.get(0));

        // Mine Genesis Block
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction, this);
        genesis.mineBlock(null, Driver.difficulty, this);
        blocks.add(genesis);
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

    public String getSaveData() throws IOException {
        return new GsonBuilder()
                .create().toJson(this);
    }

    @Override
    public String toString() {
        String blockchainJson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create().toJson(this);
        return blockchainJson;
    }

}
