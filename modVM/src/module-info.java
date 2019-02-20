module aion.vm {
    requires aion.vm.api;
    requires aion.mcf;
    requires transitive slf4j.api;
    requires aion.zero;
    requires commons.lang3;
    requires aion.util;
    requires aion.fastvm;
    requires org.aion.avm.core;
    requires aion.precompiled;
    requires com.google.common;

    exports org.aion.vm;
}
