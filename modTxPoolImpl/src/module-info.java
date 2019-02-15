module aion.txpool.impl {
    requires aion.log;
    requires slf4j.api;
    requires aion.type.api;
    requires aion.type;
    requires aion.txpool;
    requires aion.util;
    requires core;

    provides org.aion.txpool.ITxPool with
            org.aion.txpool.zero.TxPoolA0;

    exports org.aion.txpool.zero;
}
