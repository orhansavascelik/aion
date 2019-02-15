module aion.boot {
    requires aion.crypto;
    requires aion.apiserver;
    requires aion.zero.impl;
    requires aion.log;
    requires aion.evtmgr;
    requires aion.mcf;
    requires slf4j.api;
    requires aion.p2p;
    requires aion.fastvm;
    requires aion.util;
    requires aion.txpool.impl;
    requires aion.type.api;
    requires libnzmq;
    requires aion.vm;

    uses org.aion.evtmgr.EventMgrModule;
    uses org.aion.log.AionLoggerFactory;

    exports org.aion;
}
