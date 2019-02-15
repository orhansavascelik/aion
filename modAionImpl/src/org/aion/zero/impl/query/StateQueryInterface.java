package org.aion.zero.impl.query;

import java.util.Optional;
import org.aion.type.ByteArrayWrapper;
import org.aion.mcf.core.AccountState;
import org.aion.type.api.interfaces.common.Address;

public interface StateQueryInterface {
    Optional<AccountState> getAccountState(Address address, long blockNumber);

    Optional<AccountState> getAccountState(Address address, byte[] blockHash);

    Optional<AccountState> getAccountState(Address address);

    Optional<ByteArrayWrapper> getCode(Address address);
}
