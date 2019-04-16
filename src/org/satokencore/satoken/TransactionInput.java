
package org.satokencore.satoken;

import com.google.gson.annotations.Expose;

public class TransactionInput {
    
    public String transactionOutputId;
    @Expose
    public TransactionOutput UTXO;
    
    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}
