
package org.satokencore.satoken;

import com.google.gson.annotations.Expose;

public class TransactionOutput {


    public String coinId;
    
    @Expose
    public String address;
    
    @Expose
    public int value;
    
    public String parentTransactionId;

    public TransactionOutput(String address, int value, String parentTransactionId) {
        this.address = address;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.coinId = StringUtil.applySha256(address
                + Integer.toString(value) + parentTransactionId);
    }
    
    public boolean isMine(String address) {
        return (this.address.equals(address));
    }
}
