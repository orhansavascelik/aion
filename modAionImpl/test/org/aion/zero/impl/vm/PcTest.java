package org.aion.zero.impl.vm;

import static org.aion.crypto.ECKeyFac.ECKeyType.ED25519;
import static org.aion.crypto.HashUtil.H256Type.BLAKE2B_256;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import org.aion.base.db.IRepository;
import org.aion.base.db.IRepositoryCache;
import org.aion.base.type.AionAddress;
import org.aion.base.util.ByteArrayWrapper;
import org.aion.crypto.ECKeyFac;
import org.aion.crypto.HashUtil;
import org.aion.log.AionLoggerFactory;
import org.aion.mcf.core.AccountState;
import org.aion.mcf.core.ImportResult;
import org.aion.mcf.types.AbstractBlockSummary;
import org.aion.mcf.vm.types.DataWord;
import org.aion.mcf.vm.types.DoubleDataWord;
import org.aion.solidity.Compiler;
import org.aion.util.conversions.Hex;
import org.aion.utils.NativeLibrary;
import org.aion.vm.api.interfaces.Address;
import org.aion.zero.db.AionRepositoryCache;
import org.aion.zero.impl.AionHub;
import org.aion.zero.impl.config.CfgAion;
import org.aion.zero.impl.core.IAionBlockchain;
import org.aion.zero.impl.types.AionBlock;
import org.aion.zero.impl.types.AionBlockSummary;
import org.aion.zero.types.AionTransaction;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

public class PcTest {
    private IAionBlockchain blockchain;

    /**
     * Loads the database located at: {aion dir}/modAionImpl/mainnet/database
     */
    @Before
    public void setup() {
        NativeLibrary.checkNativeLibrariesLoaded();
        ECKeyFac.setType(ED25519);
        HashUtil.setType(BLAKE2B_256);
        CfgAion cfg = CfgAion.inst();
        AionLoggerFactory.init(cfg.getLog().getModules(), cfg.getLog().getLogFile(), cfg.getLogPath());
        this.blockchain = AionHub.inst().getBlockchain();
    }

    @Test
    public void checkBestBlockNumber() {
        System.out.println("Best block #: " + AionHub.inst().getBlockchain().getBestBlock().getNumber());
    }

    @Test
    public void test() {
        // Make the transactions.
        AionTransaction transaction1 = makeTransaction1();
        transaction1.setTransactionHash(Hex.decode("5e5adf199f41f3efba2bd6baafdd446990a7a096c2f5297ce9a241231350260a"));

        AionTransaction transaction2 = makeTransaction2();
        transaction2.setTransactionHash(Hex.decode("43133a8e03f3fd399066d5e26a07ff3fe6aeb2d08660f0216cdb789deb9f8484"));

        // Send them off.
        sendTransactionsInBulk(transaction1, transaction2);
    }

    @Test
    public void testContractState() {
        IRepository topRepository = AionHub.inst().getRepository();
        IRepositoryCache repositoryChild = topRepository.startTracking();
        IRepositoryCache snapshot = repositoryChild.startTracking();

        IRepositoryCache kernel1 = repositoryChild.startTracking();
        IRepositoryCache child1 = kernel1.startTracking();
        IRepositoryCache grand1 = child1.startTracking();

        Address contract1 = AionAddress.wrap("0x0000000000000000000000000000000000000000000000000000000000000200");
        byte[] key = Hex.decode("fbe129140032622b0237f6da53b32dc5");
        byte[] value = Hex.decode("5e5adf199f41f3efba2bd6baafdd446990a7a096c2f5297ce9a241231350260a");

        child1.incrementNonce(contract1);
        child1.addBalance(contract1, BigInteger.ONE.negate());
        grand1.addStorageRow(contract1, new ByteArrayWrapper(key), new ByteArrayWrapper(value));

        grand1.flush();
        child1.flush();
        ((AionRepositoryCache) kernel1).flushCopyTo(snapshot, false);

        // update
        IRepositoryCache snapTrack = snapshot.startTracking();
        snapTrack.addBalance(contract1, BigInteger.ONE);
        snapTrack.addBalance(AionAddress.wrap("0x0000000000000000000000000000000000000000000000000000000000000000"), BigInteger.TEN);
        snapTrack.flush();

        key = Hex.decode("5ea0f3cb150d3203cd857c9e018034b0");
        value = Hex.decode("43133a8e03f3fd399066d5e26a07ff3fe6aeb2d08660f0216cdb789deb9f8484");

        IRepositoryCache kernel2 = repositoryChild.startTracking();
        IRepositoryCache child2 = kernel2.startTracking();
        IRepositoryCache grand2 = child2.startTracking();

        child2.incrementNonce(contract1);
        child2.addBalance(contract1, BigInteger.TWO.negate());
        grand2.addStorageRow(contract1, new ByteArrayWrapper(key), new ByteArrayWrapper(value));

        grand2.flush();
        child2.flush();
        ((AionRepositoryCache) kernel2).flushCopyTo(snapshot, false);

        // update
        snapTrack = snapshot.startTracking();
        snapTrack.addBalance(contract1, BigInteger.TWO);
        snapTrack.addBalance(AionAddress.wrap("0x0000000000000000000000000000000000000000000000000000000000000000"), BigInteger.TEN.subtract(BigInteger.ONE));
        snapTrack.flush();

        // Now Java Kernel consumes the changes.
        kernel1.flushTo(repositoryChild, false);

        IRepositoryCache repoChildTrack = repositoryChild.startTracking();
        repoChildTrack.addBalance(contract1, BigInteger.ONE);
        repoChildTrack.addBalance(AionAddress.wrap("0x0000000000000000000000000000000000000000000000000000000000000000"), BigInteger.TEN);
        repoChildTrack.flush();

        repositoryChild.flush();

        System.out.println("ROOT = " + Hex.toHexString(topRepository.getRoot()));

        kernel2.flushTo(repositoryChild, false);

        repoChildTrack = repositoryChild.startTracking();
        repoChildTrack.addBalance(contract1, BigInteger.TWO);
        repoChildTrack.addBalance(AionAddress.wrap("0x0000000000000000000000000000000000000000000000000000000000000000"), BigInteger.TEN.subtract(BigInteger.ONE));
        repoChildTrack.flush();

        repositoryChild.flush();

        System.out.println("ROOT = " + Hex.toHexString(topRepository.getRoot()));
    }

    private Address randomAddress() {
        byte[] bytes = RandomUtils.nextBytes(Address.SIZE);
        bytes[0] = 0x0f;
        return AionAddress.wrap(bytes);
    }

    private AbstractBlockSummary sendTransactionsInBulk(AionTransaction... transactions) {
        AionBlock block = this.blockchain.createNewBlock(this.blockchain.getBestBlock(), Arrays.asList(transactions), false);
        return this.blockchain.add(block);
    }

    private AionTransaction makeTransaction1() {
        Address sender = AionAddress.wrap("0xa0eb56d95816b918e727ce7c2a1b4457743c2bc1e9a6816211a49c269c6175c2");
        Address contract = AionAddress.wrap("0x0000000000000000000000000000000000000000000000000000000000000200");
        BigInteger nonce = BigInteger.valueOf(20);
        BigInteger value = BigInteger.ZERO;
        byte[] inputData = getTransaction1inputData();

        return newTransaction(
            nonce,
            sender,
            contract,
            value,
            inputData,
            2_000_000,
            10_000_000_000L,
            (byte) 0x01
        );
    }

    private AionTransaction makeTransaction2() {
        Address sender = AionAddress.wrap("0xa0eb56d95816b918e727ce7c2a1b4457743c2bc1e9a6816211a49c269c6175c2");
        Address contract = AionAddress.wrap("0x0000000000000000000000000000000000000000000000000000000000000200");
        BigInteger nonce = BigInteger.valueOf(21);
        BigInteger value = BigInteger.ZERO;
        byte[] inputData = getTransaction2inputData();

        return newTransaction(
            nonce,
            sender,
            contract,
            value,
            inputData,
            2_000_000,
            10_000_000_000L,
            (byte) 0x01
        );
    }

    private AionTransaction newTransaction(BigInteger nonce, Address sender, Address destination, BigInteger value, byte[] data, long energyLimit, long energyPrice, byte vm) {
        return new AionTransaction(nonce.toByteArray(), sender, destination, value.toByteArray(), data, energyLimit, energyPrice, vm);
    }

    private byte[] getTransaction1inputData() {
        return Hex.decode("46d1cc2912401904bda41efdaaec263b80b83ce3a5461ee5b62a180a8d8e5d32c104933300000000000000000000000000000080000000000000000000000000000003100000000000000000000000000"
            + "00005a0000000000000000000000000000006f0000000000000000000000000000007a00000000000000000000000000000085000000000000000000000000000000014882263e1288188e5e9262fe37"
            + "5ffdfec86987f0bb138b2f112e1bc2595d032ce896622c2f5733fa9b93acd581df03a0148334ea7fba4bf27f8b65828cc66122dcbca59c73b1c7a750318833f1681f26e045afe13009fb9df386328260"
            + "b2c22d5c6653f9a4f3abfc88a7feed122873dc9c33d180bfa6c6a8cdb8010a3d397845efeb885321ee91441be1ba5e40033fa938c21fc2658b5a41cf5baadb0435198c64527bb039814e1b1ad87430ab"
            + "c3967da883cf16c4797c444f902c72cee4fad53a995dae484d42f9ce7083dce22353ab05f6ad403438cc787a1bc95c865972da77971b1d68e5cd8ddf5f5c065674618c395fe430fd1b8295c8153a2f18"
            + "5770557fce7b918429660e1a07bd39a1467664d6af13cda8fe37fd97269dbf10f014779fc362db228b7b9cb74fbbdcb9107331622b96ca89d4ec55beed455fa3fb6ac781e602257113ab87ad3de9d470"
            + "d292803accc725869992bcb8dc0c74e9031dbb1aa82de3f9ad0792ae241907dec12d7f4c575cd8ecd18caaa07725d7ba556af0f1a536f80adede41ba54e5d0ef6e3b0dc12b53865104afe7f19135db68"
            + "520e2186e224a3d6340dbb1958a4c5801c97c2dbe31f7b0279f6e7eb38be4ceccb75b46756174faada9b57702253ff448bb2169db1bcff8c47d8b13e590b9385edac27761c9e96fe272f7dd25fe781b7"
            + "0d159b32748f2c65d221b7ceb89ef65bc69ae04898c944f855741ff1e57f2a415342dce05539ad305150453571fc8e7c1b240dc15c8d6780f92aa9f1045d51ef6921bdb7ddd442c556f2b1cf93713ca5"
            + "52ff9c0734898885ae02be9e03d86bcd9e7aa7d49819b5c94ff8b0ffa7ac52461ab8bd2d55083cd82fa2568171f4f63440dd6e37a302c7b6616f238ad0d6e16636deae40000000000000000000000000"
            + "0000014a0bbed969e1f87b965c69efb7755ea251c80004d3f2bc92a1f8d98c704715d7ba04ef69dadb367bcf283e4b7fd5baf0233c4bb2048da3e0549c56e5cc6a2c79ba0bb319709639b58bab6d18c8"
            + "092a99ed14028abd3ff1b533e7179e5066e3697a0cac6f38f43e10a17c373c9609ed00f7c77a75e673fcbfd3cd752c146fc9ba0a036c5469d7706eb8f64bdd6f61ad7838641a010ed66142e745d61763"
            + "07921d3a0c608c4ea7a4f050badde164483fcbc72809c5509978cb39422373fede08bcda0a61a699be9b5324b8822557a8fc8ec40cb7ef35acf4481fd4b2b3b93f381afa0e917cddf3deb69bdd77ff51"
            + "c1207afebbedb1a0522bca3996771d83fbdc5f4a073125b038e0dd7f72af46aebc086abbb98a51924a2bf914be9f5241bbac269a09847f9f3d6a740a3b11e64d7d9bda8bcaa7aecd24d58e2e6c3dd2e7"
            + "ab66adda07956c42e805d05429f2f29ab1222ac602666e51b2ad6e8924feccdd614d5ada03664a8e0a4dce6c7cb0b6e96f29dd1166eae8cbb97d6b0446f784951680444a016d1788068361948f2a3947"
            + "d4ab6c79a29c9bbc9ea9bfd26a692a317c79b65a0f676876c370ca0d817f64cb7692dd7acf7655b0b158f64d076a47b4ed9734aa0fe9dfadb42a75fa193af8d1b2f5392f731f78cdb60b22c7513555c5"
            + "a375670a0dc319a75bab54c91e63fb0d0b00fd81d5fa119afd9e349eabcdc6c3f8ecc59a0e6abc057c2cc496aea18c2dbd7d1f242c25a7223b54838efbc7e3f9a214c29a0814641a6cf9d4e9f2243b96"
            + "fd433451f1569bae42f8e3f0b1eb2ed69717bb9a028e74c687e3bc8b166b8dd461c19f219e0441df5affde6ee3c4992367a6e6ca0c347907f9fbe31e9cb284ca76c3301fa211b21de47340c05d325a4e"
            + "4f9639f00000000000000000000000000000014000000000000000000000002540be400000000000000000000000002540be400000000000000000000000002540be4000000000000000000000000025"
            + "40be400000000000000000000000002540be400000000000000000000000002540be400000000000000000000000002540be400000000000000000000000002540be4000000000000000000000000025"
            + "40be400000000000000000000000002540be400000000000000000000000002540be400000000000000000000000002540be400000000000000000000000002540be4000000000000000000000000025"
            + "40be400000000000000000000000002540be400000000000000000000000002540be400000000000000000000000002540be400000000000000000000000002540be4000000000000000000000000025"
            + "40be400000000000000000000000002540be400000000000000000000000000000000053a77870771305f0e63b94ae7a355f5bde859a2eb49133062ec46555203659945ffd54a38223fb0505d3a03b8c"
            + "5bdc91300302578edf77d2d922cf447d7021d961062eb71a6fa290ae09813ba4c1639d2967ac706bba3abd229b910b314be0f97c1c1f53160283d16cae44cc092b4ad9c7ca2b8501af421e584e665852"
            + "137ce9e5ef28c555c278b49afee3171e5b684365240aeff1a026e3295ea29b505e9a0ff000000000000000000000000000000052da177736d88f959734da17f754d90d53b5c3a8fce63ea0677a4f31ef"
            + "66e55011311bb177c429aaef01953e3245cf9daab9213fdc72dcb9c23da5659d4715f1dfd9ba48574624b25e08e812213f85c33a41349bd130be6773a991866dda3fd725287cf29edd3aaf5a9674b408"
            + "b68301ac88b04948415c6ba3472ede8c72e9bf0ff51b89aafbe6ecedcb9fd20f842618f302b8c676380d52fb7d41809fb10956e00000000000000000000000000000005589016e7308562190cf0cb2f0"
            + "5cb7fb0c3ac30e3edfab13a87ae5381ba7e0a093a2368ca7ed040b64b93ccf053c34bc0493df54d161ccbc879a2548147c3f30d1bc30469755056df97131fea9d250876936826779de98bbd48c3cd51f"
            + "db04b0443bf1b0f763010702132e15316a65339945cd642403f7f3b62376cb7e7bd600a5adaccf8b8b3f2c027cc7b82d289893c524338c0eedf50f446a282353aaed109");
    }

    private byte[] getTransaction2inputData() {
        return Hex.decode("46d1cc2912401904bda41efdaaec263b80b83ce3a5461ee5b62a180a8d8e5d32c10493330000000000000000000000000000008000000000000000000000000000000190000000000000000000000000"
            + "000002a000000000000000000000000000000330000000000000000000000000000003e00000000000000000000000000000049000000000000000000000000000000008eaa6de2dec3aefb87aaf6e11"
            + "0898ea468e6d54355e2a4d35af63b751821d079f6ef2e2e15acc8680cba677546b08e98696d03037da9de7420915ef598d98a04f6565c536a7c097b1d2fbd2779a3ae99d6f80a74b4aa96b7077fec3eb"
            + "78e7d972de5a76ec3aaaa642fd438fd581112bfcd4b67c5ce2df540052bd0f8882d52cc8b97f6cc9ccb639963e010cdd44609b60afa1255b3f9790808305c5b5ffd1860426642f8976ae895aa43f00c0"
            + "d9345a655344fc885142d4d3723e9e408c7feef46f9e2b0def019ac647a1c153d8c9c331f802cf9a1981f9c00f9a1fb441d2fc9430097b906b606c1043c35e67539bc887e1bcdd61a97c33726c7a5a4b"
            + "b89a038f00000000000000000000000000000008a0c2e5c76e310b1f359d56157256562cc2d32b3c32299f17426bcf9b71398cf0a0392c77a3bc0743fc7e18a068e9ee05d05aa760b7f4bfa70c22d1ec"
            + "f06bb09ca0bc992da6210e894936b9281aa0298e2daa5a40d26ce1078a9de2da2d6be4e2a0ee28a0868e367449d378f425e10cf44810ac69988d679b967584f8e791cc01a092b2dfe8f3d0d61aca190a"
            + "128595167dad376cc3ea9394334c17759714aa54a0e342ff2dd67c6028b361860d5d9ffc68243e8491193a2d03a31c1adc849811a02f157db39d7fe663cab51260a0ac1c25a1c3281491a067bd3ea9f5"
            + "9ed18ec8a0f10d0fbf705e22bf7ff3060181993f147622d7a2a2fb1d047b212e17cddb6a00000000000000000000000000000008000000000000000000000002540be400000000000000000000000002"
            + "540be400000000000000000000000002540be400000000000000000000000002540be400000000000000000000000002540be400000000000000000000000002540be400000000000000000000000002"
            + "540be400000000000000000000000002540be40000000000000000000000000000000005ffd54a38223fb0505d3a03b8c5bdc91300302578edf77d2d922cf447d7021d963a77870771305f0e63b94ae7"
            + "a355f5bde859a2eb49133062ec46555203659945c1c1f53160283d16cae44cc092b4ad9c7ca2b8501af421e584e665852137ce9e5ef28c555c278b49afee3171e5b684365240aeff1a026e3295ea29b5"
            + "05e9a0ff1062eb71a6fa290ae09813ba4c1639d2967ac706bba3abd229b910b314be0f97000000000000000000000000000000052cf1a946b9ce403ea427cc7e968ec1db343552e6aaadbe4780ef579d"
            + "cb0d477b1cc6f5f05185ddb235ef9018d39e21e97ef08f5022f8a7cad23d50d089da165add0d79be1bd28257e41532a26027f6a7a1ff4a6d5eb97794cad778c5bf850436f1fc52f811de68ec2101d99d"
            + "72547d63d965b54099e218e9f9be4dd94d6c3b3ed125b68418a578c23a6b5e0913e5165da5f7318267149a682071e3fd5f73533400000000000000000000000000000005479bab52d2df79b33028c7eb"
            + "c9a24a68e6a48bcc780ef5152c4e83d91d7d5e01e326470fb322d3a496d5fd7ded750ba27fbec274f3c41e7c4a37bd9f353450043cf15ecb3dd6959b33ada7d2ada0a584ca60bcccf0264ff0d427e10e"
            + "8d2bf40b00e833d29fcb9c4da940abf512f91c4888692ef45d3fce84ab70f4527014970493b2f82d5dd5f323a0ceadd893082c7e9afffec9c165d43cf0c3ad41750ac509");
    }

}
