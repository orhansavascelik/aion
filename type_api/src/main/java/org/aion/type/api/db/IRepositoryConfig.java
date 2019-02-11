package org.aion.type.api.db;

import java.util.Properties;

/**
 * Represents a configuration interface accepted that should be accepted by the repository to
 * implement necessary configs
 *
 * @author yao
 */
public interface IRepositoryConfig {

    /** @return absolute path to the DB folder containing files */
    String getDbPath();

    IPruneConfig getPruneConfig();

    IContractDetails contractDetailsImpl();

    Properties getDatabaseConfig(String db_name);
}
