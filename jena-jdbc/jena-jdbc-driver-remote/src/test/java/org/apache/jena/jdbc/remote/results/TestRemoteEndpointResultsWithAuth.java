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

package org.apache.jena.jdbc.remote.results;

import java.io.IOException;
import java.net.http.HttpClient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.jena.http.auth.AuthLib;
import org.apache.jena.jdbc.JdbcCompatibility;
import org.apache.jena.jdbc.connections.JenaConnection;
import org.apache.jena.jdbc.remote.FusekiTestAuth;
import org.apache.jena.jdbc.remote.connections.RemoteEndpointConnection;
import org.apache.jena.jdbc.remote.utils.TestJdbcRemoteUtils;
import org.apache.jena.query.Dataset ;
import org.apache.jena.sparql.modify.request.Target ;
import org.apache.jena.sparql.modify.request.UpdateDrop ;
import org.apache.jena.update.Update ;
import org.apache.jena.update.UpdateExecution;
import org.eclipse.jetty.security.SecurityHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * Tests result sets from a remote endpoint
 */
@Ignore
public class TestRemoteEndpointResultsWithAuth extends AbstractRemoteEndpointResultSetTests {

    private static RemoteEndpointConnection connection;

    private static String USER = "test";
    private static String PASSWORD = "letmein";
    private static HttpClient client;

    /**
     * Setup for the tests by allocating a Fuseki instance to work with
     *
     * @throws SQLException
     * @throws IOException
     */
    @BeforeClass
    public static void setup() throws SQLException, IOException {
        SecurityHandler sh = FusekiTestAuth.makeSimpleSecurityHandler("/*", USER, PASSWORD);
        FusekiTestAuth.setupServer(true, sh);
        client = HttpClient.newBuilder().authenticator(AuthLib.authenticator(USER, PASSWORD)).build();

        connection = new RemoteEndpointConnection(FusekiTestAuth.serviceQuery(), FusekiTestAuth.serviceUpdate(), null, null, null, null,
                client, JenaConnection.DEFAULT_HOLDABILITY, JdbcCompatibility.DEFAULT, null, null);
        connection.setJdbcCompatibilityLevel(JdbcCompatibility.HIGH);
    }

    /**
     * Clean up after each test by resetting the Fuseki instance
     */
    @After
    public void cleanupTest() {
        Update clearRequest = new UpdateDrop(Target.ALL) ;
        UpdateExecution.service(FusekiTestAuth.serviceUpdate())
            .update(clearRequest)
            .httpClient(client)
            .execute();
    }

    /**
     * Clean up after tests by de-allocating the Fuseki instance
     *
     * @throws SQLException
     */
    @AfterClass
    public static void cleanup() throws SQLException {
        connection.close();
        FusekiTestAuth.teardownServer();
    }

    @Override
    protected ResultSet createResults(Dataset ds, String query) throws SQLException {
        return createResults(ds, query, ResultSet.TYPE_FORWARD_ONLY);
    }

    @Override
    protected ResultSet createResults(Dataset ds, String query, int resultSetType) throws SQLException {
        TestJdbcRemoteUtils.copyToRemoteDataset(ds, FusekiTestAuth.serviceGSP(), client);
        Statement stmt = connection.createStatement(resultSetType, ResultSet.CONCUR_READ_ONLY);
        return stmt.executeQuery(query);
    }
}
