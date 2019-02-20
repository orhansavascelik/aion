module aion.txpool.impl {
    requires aion.log;
    requires slf4j.api;
    requires aion.txpool;
    requires aion.util;
    requires core;
    requires aion.vm.api;

    provides org.aion.txpool.ITxPool with
            org.aion.txpool.zero.TxPoolA0;

    exports org.aion.txpool.zero;
}
