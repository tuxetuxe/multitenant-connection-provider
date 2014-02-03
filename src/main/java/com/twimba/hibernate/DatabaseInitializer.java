package com.twimba.hibernate;

import java.sql.Connection;

public interface DatabaseInitializer {

	void intializeDatabase(Connection conn);

	boolean databaseNeedsToBeInitialized(Connection conn);
}
