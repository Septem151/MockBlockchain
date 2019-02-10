
package org.satokencore.satoken;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.util.ArrayList;
import java.util.HashMap;

public class Blockchain {
    
    @Expose
    private final ArrayList<Block> blocks;
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>();
    public static final int minimumTransaction = 1;
    
    public Blockchain() {
        blocks = new ArrayList<>();
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
    
    @Override
    public String toString() {
        String blockchainJson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create().toJson(this);
        return blockchainJson;
    }
    
}
