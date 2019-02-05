package org.aion.zero.impl.vm.resources;

import org.aion.base.type.AionAddress;
import org.aion.crypto.ECKey;
import org.aion.vm.VirtualMachineProvider;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.impl.StandaloneBlockchain;

public class TransactionHelper {
    private StandaloneBlockchain blockchain;
    private ECKey deployerKey;
    private Address deployer;

    private TransactionHelper() {
        VirtualMachineProvider.initializeAllVirtualMachines();
        StandaloneBlockchain.Bundle bundle = new StandaloneBlockchain.Builder()
            .withDefaultAccounts()
            .withValidatorConfiguration("simple")
            .withAvmEnabled()
            .build();
        this.blockchain = bundle.bc;
        this.deployerKey = bundle.privateKeys.get(0);
        this.deployer = AionAddress.wrap(this.deployerKey.getAddress());
    }

    /**
     * Starts the transaction helper and initializes all virtual machines.
     *
     * Once done with this class, the {@code shutdown()} method must be invoked!
     */
    public static TransactionHelper start() {
        return new TransactionHelper();
    }

    /**
     * Shuts down the virtual machines. This method must be invoked when done with this class.
     */
    public void shutdown() {
        VirtualMachineProvider.shutdownAllVirtualMachines();
        this.blockchain = null;
        this.deployerKey = null;
        this.deployer = null;
    }

}
