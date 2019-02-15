package org.aion.type.api.interfaces.common;

import java.io.Serializable;

public interface Wrapper<T> extends Bytesable<T>, Comparable<T>, Serializable {

    byte[] getData();

    Wrapper<T> copy();

    boolean isZero();

    boolean isEmpty();

    byte[] getNoLeadZeroesData();
}
