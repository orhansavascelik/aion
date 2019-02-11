package org.aion.type.api.vm;

import java.math.BigInteger;
import org.aion.type.api.util.ByteArrayWrapper;

public interface IDataWord {

    byte[] getData();

    byte[] getNoLeadZeroesData();

    BigInteger value();

    IDataWord copy();

    boolean isZero();

    ByteArrayWrapper toWrapper();
}
