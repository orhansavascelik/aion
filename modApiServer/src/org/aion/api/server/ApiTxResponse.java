package org.aion.api.server;

import org.aion.mcf.blockchain.TxResponse;
import org.aion.type.api.interfaces.common.Address;

public class ApiTxResponse {

    private final TxResponse rsp;

    private byte[] txHash;

    private Address contractAddress;

    // Could just store the exception message string
    private Exception ex;

    ApiTxResponse(TxResponse rsp) {
        this.rsp = rsp;
    }

    ApiTxResponse(TxResponse rsp, byte[] txHash) {
        this.rsp = rsp;
        this.txHash = txHash;
    }

    ApiTxResponse(TxResponse rsp, byte[] txHash, Address contractAddress) {
        this.rsp = rsp;
        this.txHash = txHash;
        this.contractAddress = contractAddress;
    }

    ApiTxResponse(TxResponse rsp, Exception ex) {
        this.rsp = rsp;
        this.ex = ex;
    }

    public TxResponse getType() {
        return rsp;
    }

    public String getMessage() {
        switch (rsp) {
            case SUCCESS:
                return "TransactionExtend sent successfully";
            case INVALID_TX:
                return "Invalid transaction object";
            case INVALID_TX_NRG_PRICE:
                return "Invalid transaction energy price";
            case INVALID_FROM:
                return "Invalid from address provided";
            case INVALID_ACCOUNT:
                return "Account not found, or not unlocked";
            case ALREADY_CACHED:
                return "TransactionExtend is already in the cache";
            case CACHED_NONCE:
                return "TransactionExtend cached due to large nonce";
            case CACHED_POOLMAX:
                return "TransactionExtend cached because the pool is full";
            case REPAID:
                return "TransactionExtend successfully repaid";
            case ALREADY_SEALED:
                return "TransactionExtend has already been sealed in the repo";
            case REPAYTX_POOL_EXCEPTION:
                return "Repaid transaction wasn't found in the pool";
            case REPAYTX_LOWPRICE:
                return "Repaid transaction needs to have a higher energy price";
            case DROPPED:
                return "TransactionExtend dropped";
            case EXCEPTION:
                return ex.getMessage();
            default:
                return "TransactionExtend status unknown";
        }
    }

    public boolean isFail() {
        return rsp.isFail();
    }

    // Should only be called if tx was successfully sent
    public byte[] getTxHash() {
        return txHash;
    }

    public Address getContractAddress() {
        return contractAddress;
    }
}
