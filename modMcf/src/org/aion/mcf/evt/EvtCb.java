package org.aion.mcf.evt;

@FunctionalInterface
public interface EvtCb<T> {
    void call(T t);
}
