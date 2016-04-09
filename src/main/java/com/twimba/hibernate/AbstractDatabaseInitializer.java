package com.twimba.hibernate;

import java.sql.Connection;
import java.sql.ResultSet;

/**
 * Abstract class to be implemented with the logic to initialize a database on
 * runtime. Useful to be able to add tenants in runtime without any downtime to
 * reconfigure the system.
 *
 * @author Luis Santos
 *
 */
public abstract class AbstractDatabaseInitializer implements DatabaseInitializer {

    /**
     * Checks if a table exists in the database in order to check if it needs to
     * be initialized or not
     */
    @Override
    public boolean databaseNeedsToBeInitialized(Connection conn) {
        boolean needsUpgrade = true;
        try {
            ResultSet rs = null;
            try {
                rs = conn.getMetaData().getTables(null, null, getInitializingTableNameToCheck(), null);
                while (rs.next()) {
                    needsUpgrade = false;
                    break;
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
        } catch (Exception e) {
            needsUpgrade = false;
        }
        return needsUpgrade;
    }

    /**
     * The name of the table to check if the database is initialized or not. Can
     * be anything... Most probably should be a central table to your
     * application that exists **everytime**... Or just a placeholder like the
     * default
     *
     * @return
     */
    protected String getInitializingTableNameToCheck() {
        return "i_exists";
    }

}
