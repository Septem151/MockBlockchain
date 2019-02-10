
package org.satokencore.satoken;

import com.google.gson.annotations.Expose;

public class TransactionOutput {

    public String id;
    
    public String recipient;
    
    @Expose
    public String address;
    
    @Expose
    public int value;
    
    public String parentTransactionId;

    public TransactionOutput(String recipient, int value, String parentTransactionId) {
        this.recipient = recipient;
        this.address = recipient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = StringUtil.applySha256(recipient
                + Integer.toString(value) + parentTransactionId);
    }
    
    public boolean isMine(String address) {
        return (recipient.equals(address));
    }
}
