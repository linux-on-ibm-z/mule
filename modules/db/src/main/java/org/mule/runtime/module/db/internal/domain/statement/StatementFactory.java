/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.internal.domain.statement;

import org.mule.runtime.module.db.internal.domain.autogeneratedkey.AutoGeneratedKeyStrategy;
import org.mule.runtime.module.db.internal.domain.connection.DbConnection;
import org.mule.runtime.module.db.internal.domain.query.QueryTemplate;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates JDBC statements for a given connection
 */
public interface StatementFactory
{

    /**
     * Creates a JDBC statement
     *
     * @param connection connection uses to create the statement
     * @param queryTemplate query template that will be execute on the statement
     * @return a statement appropriate to execute a query with the given template
     * @throws SQLException if a database access error occurs or this method is called on a closed connection
     */
    Statement create(DbConnection connection, QueryTemplate queryTemplate) throws SQLException;

    /**
     * Creates a JDBC statement with auto generated keys processing
     *
     * @param connection connection uses to create the statement
     * @param queryTemplate query template that will be execute on the statement
     * @param autoGeneratedKeyStrategy strategy to process auto generated keys
     * @return a statement appropriate to execute a query with the given template
     * @throws SQLException if a database access error occurs or this method is called on a closed connection
     */
    Statement create(DbConnection connection, QueryTemplate queryTemplate, AutoGeneratedKeyStrategy autoGeneratedKeyStrategy) throws SQLException;
}
