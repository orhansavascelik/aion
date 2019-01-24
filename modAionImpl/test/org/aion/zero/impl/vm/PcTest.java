package org.aion.zero.impl.vm;

import static org.aion.crypto.ECKeyFac.ECKeyType.ED25519;
import static org.aion.crypto.HashUtil.H256Type.BLAKE2B_256;

import java.io.IOException;
import java.util.Properties;
import org.aion.crypto.ECKeyFac;
import org.aion.crypto.HashUtil;
import org.aion.log.AionLoggerFactory;
import org.aion.solidity.Compiler;
import org.aion.utils.NativeLibrary;
import org.aion.zero.impl.AionHub;
import org.aion.zero.impl.config.CfgAion;
import org.junit.Before;
import org.junit.Test;

public class PcTest {

    @Before
    public void setup() {
        // Load in the database at: modAionImpl/mainnet/database
        NativeLibrary.checkNativeLibrariesLoaded();

        try {
            Compiler.getInstance().compileHelloAion();
        } catch (IOException e) {
            System.out.println("compiler load failed!");
            throw new ExceptionInInitializerError();
        }

        ECKeyFac.setType(ED25519);
        HashUtil.setType(BLAKE2B_256);

        CfgAion cfg = CfgAion.inst();

        Properties p = cfg.getFork().getProperties();
        p.forEach(
            (k, v) -> {
                System.out.println(
                    "<Protocol name: "
                        + k.toString()
                        + " block#: "
                        + v.toString()
                        + " updated!");
            });

        AionLoggerFactory.init(cfg.getLog().getModules(), cfg.getLog().getLogFile(), cfg.getLogPath());
    }

    @Test
    public void test() {
        System.out.println("Best block #: " + AionHub.inst().getBlockchain().getBestBlock().getNumber());
    }

}
