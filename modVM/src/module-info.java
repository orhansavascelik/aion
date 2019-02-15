module aion.vm {
    requires aion.type.api;
    requires aion.mcf;
    requires transitive slf4j.api;
    requires aion.zero;
    requires commons.lang3;
    requires aion.vm.api;
    requires aion.util;
    requires aion.fastvm;
    requires org.aion.avm.core;
    requires aion.precompiled;
    requires com.google.common;
    requires aion.type;

    exports org.aion.vm;
}
