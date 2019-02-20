package org.aion.zero.impl;

import java.math.BigInteger;
import java.util.Map;
import org.aion.types.ByteArrayWrapper;
import org.aion.interfaces.db.RepositoryCache;
import org.aion.types.ByteArrayWrapper;
import org.aion.mcf.vm.types.DataWord;
import org.aion.precompiled.ContractFactory;
import org.aion.types.Address;
import org.aion.zero.impl.db.AionRepositoryImpl;

/** {@link AionHub} functionality where a full instantiation of the class is not desirable. */
public class AionHubUtils {

    public static void buildGenesis(AionGenesis genesis, AionRepositoryImpl repository) {
        // initialization section for network balance contract
        RepositoryCache track = repository.startTracking();

        Address networkBalanceAddress = ContractFactory.getTotalCurrencyContractAddress();
        track.createAccount(networkBalanceAddress);

        for (Map.Entry<Integer, BigInteger> addr : genesis.getNetworkBalances().entrySet()) {
            // assumes only additions are performed in the genesis
            track.addStorageRow(
                    networkBalanceAddress,
                    new DataWord(addr.getKey()).toWrapper(),
                    wrapValueForPut(new DataWord(addr.getValue())));
        }

        for (Address addr : genesis.getPremine().keySet()) {
            track.createAccount(addr);
            track.addBalance(addr, genesis.getPremine().get(addr).getBalance());
        }
        track.flush();

        repository.commitBlock(genesis.getHeader());
        repository.getBlockStore().saveBlock(genesis, genesis.getDifficultyBI(), true);
    }

    private static ByteArrayWrapper wrapValueForPut(DataWord value) {
        return (value.isZero())
                ? value.toWrapper()
                : (ByteArrayWrapper) new ByteArrayWrapper(value.getNoLeadZeroesData());
    }
}
