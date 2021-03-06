/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.jdbc.tdb.connections;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.jena.jdbc.JdbcCompatibility;
import org.apache.jena.jdbc.connections.AbstractJenaConnectionTests;
import org.apache.jena.jdbc.connections.DatasetConnection;
import org.apache.jena.jdbc.connections.JenaConnection;
import org.apache.jena.jdbc.utils.TestJdbcUtils;
import org.apache.jena.query.Dataset ;
import org.apache.jena.tdb.TDBFactory ;
import org.apache.jena.tdb.base.file.Location ;
import org.apache.jena.tdb.sys.TDBInternal;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for the {@link DatasetConnection} backed by a purely in-memory testing
 * only TDB dataset
 * 
 */
public class TestTdbDiskConnection extends AbstractJenaConnectionTests {

    /**
     * Temporary directory rule used to guarantee a unique temporary folder for
     * each test method
     */
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    
    /**
     * Cleans up resources used to avoid process memory leaks
     */
    @After
    public void cleanupTest() {
        TDBInternal.expel(Location.create(tempDir.getRoot().getAbsolutePath()), true);
    }

    @Override
    protected JenaConnection getConnection() throws SQLException {
        return new TDBConnection(TDBFactory.createDataset(tempDir.getRoot().getAbsolutePath()),
                ResultSet.HOLD_CURSORS_OVER_COMMIT, JenaConnection.DEFAULT_AUTO_COMMIT, JdbcCompatibility.DEFAULT);
    }

    @Override
    protected JenaConnection getConnection(Dataset ds) throws SQLException {
        Dataset tdb = TDBFactory.createDataset(tempDir.getRoot().getAbsolutePath());
        TestJdbcUtils.copyDataset(ds, tdb, true);
        return new TDBConnection(tdb, ResultSet.HOLD_CURSORS_OVER_COMMIT, JenaConnection.DEFAULT_AUTO_COMMIT,
                JdbcCompatibility.DEFAULT);
    }

}
