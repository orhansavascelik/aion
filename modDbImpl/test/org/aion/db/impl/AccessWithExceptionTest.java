package org.aion.db.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.aion.db.impl.DatabaseFactory.Props.DB_NAME;

import com.google.common.truth.Truth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.aion.type.api.db.IByteArrayKeyValueDatabase;
import org.aion.db.utils.FileUtils;
import org.aion.log.AionLoggerFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class AccessWithExceptionTest {
    private static final boolean VERBOSE = false;

    @BeforeClass
    public static void setup() {
        // logging to see errors
        Map<String, String> cfg = new HashMap<>();
        cfg.put("DB", "WARN");

        AionLoggerFactory.init(cfg);
    }

    @AfterClass
    public static void teardown() {
        // clean out the tmp directory
        Truth.assertThat(FileUtils.deleteRecursively(DatabaseTestUtils.testDir)).isTrue();
        Truth.assertThat(DatabaseTestUtils.testDir.mkdirs()).isTrue();
    }

    @Before
    public void deleteFromDisk() {
        // clean out the tmp directory
        assertThat(FileUtils.deleteRecursively(DatabaseTestUtils.testDir)).isTrue();
        Truth.assertThat(DatabaseTestUtils.testDir.mkdirs()).isTrue();
    }

    /** @return parameters for testing {@link #} */
    @SuppressWarnings("unused")
    private static Object databaseInstanceDefinitions() {
        return DatabaseTestUtils.databaseInstanceDefinitions();
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testIsEmptyWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt isEmpty on closed db
        db.isEmpty();
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testKeysWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt keys on closed db
        db.keys();
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testGetWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt get on closed db
        db.get(DatabaseTestUtils.randomBytes(32));
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testPutWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt put on closed db
        db.put(DatabaseTestUtils.randomBytes(32), DatabaseTestUtils.randomBytes(32));
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testDeleteWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt delete on closed db
        db.delete(DatabaseTestUtils.randomBytes(32));
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testPutToBatchWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt put on closed db
        db.putToBatch(DatabaseTestUtils.randomBytes(32), DatabaseTestUtils.randomBytes(32));
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testDeleteInBatchWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt delete on closed db
        db.deleteInBatch(DatabaseTestUtils.randomBytes(32));
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testPutBatchWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        Map<byte[], byte[]> map = new HashMap<>();
        map.put(DatabaseTestUtils.randomBytes(32), DatabaseTestUtils.randomBytes(32));
        map.put(DatabaseTestUtils.randomBytes(32), DatabaseTestUtils.randomBytes(32));
        map.put(DatabaseTestUtils.randomBytes(32), DatabaseTestUtils.randomBytes(32));

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt putBatch on closed db
        db.putBatch(map);
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testDeleteBatchWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        List<byte[]> list = new ArrayList<>();
        list.add(DatabaseTestUtils.randomBytes(32));
        list.add(DatabaseTestUtils.randomBytes(32));
        list.add(DatabaseTestUtils.randomBytes(32));

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt deleteBatch on closed db
        db.deleteBatch(list);
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testCommitWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // TODO: differentiate between not supported and closed
        // attempt commit on closed db
        db.commit();
    }

    @Test(expected = RuntimeException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testSizeWithClosedDatabase(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.isOpen()).isFalse();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt approximateSize on closed db
        db.approximateSize();
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testGetWithNullKey(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.open()).isTrue();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt get with null key
        db.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testPutWithNullKey(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.open()).isTrue();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt put with null key
        db.put(null, DatabaseTestUtils.randomBytes(32));
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testPutWithNullValue(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.open()).isTrue();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt put with null key
        db.put(DatabaseTestUtils.randomBytes(32), null);
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testPutToBatchWithNullKey(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.open()).isTrue();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt put with null key
        db.putToBatch(null, DatabaseTestUtils.randomBytes(32));
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testPutToBatchWithNullValue(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.open()).isTrue();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt put with null key
        db.putToBatch(DatabaseTestUtils.randomBytes(32), null);
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testDeleteWithNullKey(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.open()).isTrue();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt delete with null key
        db.delete(null);
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testDeleteInBatchWithNullKey(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.open()).isTrue();

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt delete with null key
        db.deleteInBatch(null);
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testPutBatchWithNullKey(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.open()).isTrue();

        Map<byte[], byte[]> map = new HashMap<>();
        map.put(DatabaseTestUtils.randomBytes(32), DatabaseTestUtils.randomBytes(32));
        map.put(DatabaseTestUtils.randomBytes(32), DatabaseTestUtils.randomBytes(32));
        map.put(null, DatabaseTestUtils.randomBytes(32));

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt putBatch on closed db
        db.putBatch(map);
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testPutBatchWithNullValue(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.open()).isTrue();

        Map<byte[], byte[]> map = new HashMap<>();
        map.put(DatabaseTestUtils.randomBytes(32), DatabaseTestUtils.randomBytes(32));
        map.put(DatabaseTestUtils.randomBytes(32), DatabaseTestUtils.randomBytes(32));
        map.put(DatabaseTestUtils.randomBytes(32), null);

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt putBatch on closed db
        db.putBatch(map);
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters(method = "databaseInstanceDefinitions")
    public void testDeleteBatchWithNullKey(Properties dbDef) {
        // create database
        dbDef.setProperty(DB_NAME, DatabaseTestUtils.dbName + DatabaseTestUtils.getNext());
        IByteArrayKeyValueDatabase db = DatabaseFactory.connect(dbDef);
        assertThat(db.open()).isTrue();

        List<byte[]> list = new ArrayList<>();
        list.add(DatabaseTestUtils.randomBytes(32));
        list.add(DatabaseTestUtils.randomBytes(32));
        list.add(null);

        if (VERBOSE) {
            System.out.println(db.toString());
        }

        // attempt deleteBatch on closed db
        db.deleteBatch(list);
    }
}
