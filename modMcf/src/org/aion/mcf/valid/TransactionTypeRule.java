package org.aion.mcf.valid;

import org.aion.type.api.vm.VirtualMachineSpecs;

/**
 * Rules for validating transactions based on allowed types.
 *
 * @author Alexandra Roatis
 */
public class TransactionTypeRule {

    public static boolean isValidFVMTransactionType(byte type) {
        return type == VirtualMachineSpecs.FVM_DEFAULT_TX_TYPE
                || type == VirtualMachineSpecs.FVM_ALLOWED_TX_TYPE;
    }

    public static boolean isValidAVMTransactionType(byte type) {
        return type == VirtualMachineSpecs.AVM_CREATE_CODE;
    }
}
