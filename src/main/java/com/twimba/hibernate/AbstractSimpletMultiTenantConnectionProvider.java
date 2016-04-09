package com.twimba.hibernate;

import java.util.HashMap;
import java.util.Map;

/**
 * A very simple _demonstration_ on how to extends the
 * AbstractMultiTenantConnectionProvider to be used in a real context. This
 * implementation reads the tenant configuration from a local configuration that
 * has to be correctly initialized before any other operation can occur.
 *
 * @author Luis Santos
 *
 */
public abstract class AbstractSimpletMultiTenantConnectionProvider extends AbstractMultiTenantConnectionProvider {

    private static final long serialVersionUID = 1L;

    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private static final String DRIVER_KEY = "driver";
    private static final String URL_KEY = "url";

    private static Map<String, Map<String, String>> configurations = new HashMap<>();

    @Override
    protected String getTenantDatabaseUsername(String tenantIdentifier) {
        return configurations.get(tenantIdentifier).get(USERNAME_KEY);
    }

    @Override
    protected String getTenantDatabasePassword(String tenantIdentifier) {
        return configurations.get(tenantIdentifier).get(PASSWORD_KEY);
    }

    @Override
    protected String getTenantDatabaseDriver(String tenantIdentifier) {
        return configurations.get(tenantIdentifier).get(DRIVER_KEY);
    }

    @Override
    protected String getTenantDatabaseUrl(String tenantIdentifier) {
        return configurations.get(tenantIdentifier).get(URL_KEY);
    }

    @Override
    protected DatabaseInitializer getDatabaseInitializer() {
        // Nothing will be initialized
        return null;
    }

    public void setTenantDatabaseUsername(String tenantIdentifier, String username) {
        ensureTenantMapExists(tenantIdentifier);
        configurations.get(tenantIdentifier).put(USERNAME_KEY, username);
    }

    public void setTenantDatabasePassword(String tenantIdentifier, String password) {
        ensureTenantMapExists(tenantIdentifier);
        configurations.get(tenantIdentifier).put(PASSWORD_KEY, password);
    }

    public void setTenantDatabaseDriver(String tenantIdentifier, String driver) {
        ensureTenantMapExists(tenantIdentifier);
        configurations.get(tenantIdentifier).put(DRIVER_KEY, driver);
    }

    public void setTenantDatabaseUrl(String tenantIdentifier, String url) {
        ensureTenantMapExists(tenantIdentifier);
        configurations.get(tenantIdentifier).put(URL_KEY, url);
    }

    private void ensureTenantMapExists(String tenantIdentifier) {
        if (!configurations.containsKey(tenantIdentifier)) {
            configurations.put(tenantIdentifier, new HashMap<String, String>());
        }
    }

}
