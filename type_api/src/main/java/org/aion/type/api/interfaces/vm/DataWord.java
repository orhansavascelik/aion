package org.aion.type.api.interfaces.vm;

import java.math.BigInteger;
import org.aion.type.api.interfaces.common.Wrapper;

public interface DataWord {

    byte[] getData();

    byte[] getNoLeadZeroesData();

    BigInteger value();

    DataWord copy();

    boolean isZero();

    Wrapper toWrapper();
}
