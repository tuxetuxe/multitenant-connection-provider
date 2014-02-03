package com.twimba.hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import com.twimba.hibernate.exceptions.InvalidTenantConfigurationException;
import com.twimba.hibernate.exceptions.MultiTenantConnectionProviderException;
import com.twimba.hibernate.exceptions.UnknownTenantException;

/**
 * A very simple implementation of the Hibernate MultiTenantConnection Provider.
 * This abstract class should be extended (ie:
 * com.twimba.hibernate.SimpletMultiTenantConnectionProvider) whith logic to
 * actual retreive the correct tenant information. All the configurations should
 * be done in the standard hibernate way, with the tenats only having a
 * different pair of {database URL, username, password, JDBC driver }.
 * 
 * A connection provider to the "base tenant" (the hibernate "any connection")
 * is always present. In a real world scenario you probably don't want to use
 * this database to store any tenant data.
 * 
 * Attention:
 *  You still need a org.hibernate.context.spi.CurrentTenantIdentifierResolver 
 *  configured so that hibernate can resolve the current tenant id !!
 *  
 *  Programmatically this is a sample configuration
 *     HashMap<String, String> params = new HashMap<String, String>();
 *     
 *     params.put(Environment.MULTI_TENANT, MultiTenancyStrategy.DATABASE.name());
 *     params.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, <a sub class of this>);
 *     params.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, <an implementation of the CurrentTenantIdentifierResolver> );
 * 
 * @author Luis Santos
 */
public abstract class AbstractMultiTenantConnectionProvider implements MultiTenantConnectionProvider, Stoppable, ServiceRegistryAwareService {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = Logger.getLogger(MultiTenantConnectionProvider.class);

	@SuppressWarnings("rawtypes")
	private Map cfgSettings;
	private ServiceRegistryImplementor serviceRegistry;

	/**
	 * The base tenant connection provider. This is configured in the standard
	 * Hibernate way
	 */
	private C3P0ConnectionProvider baseConnectionProvider = null;

	/**
	 * A map of all the tenants connection providers to quick and easy access
	 */
	private Map<String, C3P0ConnectionProvider> knownConnectionProviders = new HashMap<String, C3P0ConnectionProvider>();

	/**
	 * A collection of all the tenants for who the database is already
	 * initialized
	 */
	private Set<String> initialziedTentantsDatabases = new HashSet<String>();

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		cfgSettings = serviceRegistry.getService(ConfigurationService.class).getSettings();

		String baseTenantIdentifier = getBaseTenantIdentifier();
		baseConnectionProvider = createConnectionProvider(baseTenantIdentifier);
		try {
			Connection baseTenantConnection = getConnection(baseTenantIdentifier);
			if (databaseNeedsToBeInitialized(baseTenantIdentifier, baseTenantConnection)) {
				upgradeDatabase(baseTenantIdentifier, baseTenantConnection);
			} else {
				initialziedTentantsDatabases.add(baseTenantIdentifier);
			}
		} catch (SQLException e) {
			throw new MultiTenantConnectionProviderException("Unable to get a connection to the base tenant. Check your configuration.", e);
		}
	}

	@Override
	public Connection getAnyConnection() throws SQLException {
		return baseConnectionProvider.getConnection();
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		baseConnectionProvider.closeConnection(connection);
	}

	@Override
	public Connection getConnection(String tenantIdentifier) throws SQLException {

		if (getBaseTenantIdentifier().equals(tenantIdentifier)) {
			return baseConnectionProvider.getConnection();
		}

		C3P0ConnectionProvider provider = null;
		if (knownConnectionProviders.containsKey(tenantIdentifier)) {
			provider = knownConnectionProviders.get(tenantIdentifier);
		} else {
			provider = createConnectionProvider(tenantIdentifier);
			knownConnectionProviders.put(tenantIdentifier, provider);
		}
		Connection conn = provider.getConnection();

		if (databaseNeedsToBeInitialized(tenantIdentifier, conn)) {
			upgradeDatabase(tenantIdentifier, conn);
		}

		return conn;
	}

	@Override
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
		C3P0ConnectionProvider provider = null;
		if (getBaseTenantIdentifier().equals(tenantIdentifier)) {
			provider = baseConnectionProvider;
		} else {
			if (!knownConnectionProviders.containsKey(tenantIdentifier)) {
				throw new HibernateException("There is no connection provider for tenant '" + tenantIdentifier + "' and we are trying to close a connection here?!?");
			} else {
				provider = knownConnectionProviders.get(tenantIdentifier);
			}
		}
		provider.closeConnection(connection);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean isUnwrappableAs(Class unwrapType) {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		return null;
	}

	@Override
	public void stop() {
		baseConnectionProvider.stop();
		for (C3P0ConnectionProvider provider : knownConnectionProviders.values()) {
			provider.stop();
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return baseConnectionProvider.supportsAggressiveRelease();
	}

	private C3P0ConnectionProvider createConnectionProvider(String tenantIdentifier) {
		C3P0ConnectionProvider provider = getConnectionProvider();
		provider.injectServices(serviceRegistry);
		provider.configure(getSettingsForTenant(cfgSettings, tenantIdentifier));

		return provider;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map getSettingsForTenant(Map cfgSettings, String tenantIdentifier) {
		Map tenantSettings = new HashMap();
		tenantSettings.putAll(cfgSettings);

		String url = getTenantDatabaseUrl(tenantIdentifier);
		String driver = getTenantDatabaseDriver(tenantIdentifier);
		String password = getTenantDatabasePassword(tenantIdentifier);
		String username = getTenantDatabaseUsername(tenantIdentifier);

		if (StringUtils.isBlank(url)) {
			throw new UnknownTenantException("The tenant '" + tenantIdentifier + "' is not known");
		}

		if (StringUtils.isBlank(driver) || StringUtils.isBlank(password) || StringUtils.isBlank(username)) {
			throw new InvalidTenantConfigurationException("The tenant '" + tenantIdentifier + "' is not known");
		}

		tenantSettings.remove(Environment.DRIVER);
		tenantSettings.put(Environment.DRIVER, driver);
		// TODO For now only a single dialect is supported for all the tenants
		tenantSettings.remove(Environment.URL);
		tenantSettings.put(Environment.URL, url);
		tenantSettings.remove(Environment.PASS);
		tenantSettings.put(Environment.PASS, password);
		tenantSettings.remove(Environment.USER);
		tenantSettings.put(Environment.USER, username);

		return tenantSettings;
	}

	private void upgradeDatabase(String tenantIdentifier, Connection conn) {
		try {
			DatabaseInitializer databaseInitializer = getDatabaseInitializer();

			if (databaseInitializer != null) {
				LOG.warn("No database initializer defined. Skipping...");
			}

			databaseInitializer.intializeDatabase(conn);

			initialziedTentantsDatabases.add(tenantIdentifier);

		} catch (Exception e) {
			LOG.warn("Unable to initialize the databse for ´" + tenantIdentifier + "´: " + e.getMessage(), e);
		}
	}

	private boolean databaseNeedsToBeInitialized(String tenantIdentifier, Connection conn) {
		boolean needsUpgrade = true;
		if (!initialziedTentantsDatabases.contains(tenantIdentifier)) {
			needsUpgrade = getDatabaseInitializer().databaseNeedsToBeInitialized(conn);
		} else {
			needsUpgrade = false;
		}
		return needsUpgrade;
	}

	/**
	 * The identifier of the base tenant. Overwrite if you need it to be
	 * different than the default: _baseTenant
	 * 
	 * @return
	 */
	protected String getBaseTenantIdentifier() {
		return "_baseTenant";
	}

	/**
	 * A way to create a specific connection Provider
	 * 
	 * @return
	 */
	protected abstract C3P0ConnectionProvider getConnectionProvider();

	/**
	 * Given a tenant return the appropriate username.
	 */
	protected abstract String getTenantDatabaseUsername(String tenantIdentifier);

	/**
	 * Given a tenant return the appropriate password.
	 */
	protected abstract String getTenantDatabasePassword(String tenantIdentifier);

	/**
	 * Given a tenant return the appropriate driver.
	 */
	protected abstract String getTenantDatabaseDriver(String tenantIdentifier);

	/**
	 * Given a tenant return the appropriate database URL.
	 */
	protected abstract String getTenantDatabaseUrl(String tenantIdentifier);

	/**
	 * If you require that the tenant database is initialized by you custom
	 * initializer, return here a new instance of it.
	 * 
	 * @return
	 */
	protected abstract DatabaseInitializer getDatabaseInitializer();

}
