module aion.zero {
    requires aion.type.api;
    requires aion.rlp;
    requires aion.crypto;
    requires aion.mcf;
    requires slf4j.api;
    requires org.json;
    requires commons.lang3;
    requires aion.vm.api;

    exports org.aion.zero.api;
    exports org.aion.zero.db;
    exports org.aion.zero.types;
    exports org.aion.zero.exceptions;
}
