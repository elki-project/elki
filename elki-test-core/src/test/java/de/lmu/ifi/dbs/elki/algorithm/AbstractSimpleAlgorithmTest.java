/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.InputStreamDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.filter.FixedDBIDsFilter;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Abstract base class useful for testing various algorithms.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractSimpleAlgorithmTest {
  /**
   * Base path for unit test files.
   */
  public final static String UNITTEST = "elki/testdata/unittests/";

  /**
   * Generate a simple DoubleVector database from a file.
   *
   * @param filename File to load
   * @param expectedSize Expected size in records
   * @return Database
   */
  public static Database makeSimpleDatabase(String filename, int expectedSize) {
    ListParameterization params = new ListParameterization();
    // Use a fixed DBID - historically, we used 1 indexed - to reduce random
    // variation in results due to different hash codes everywhere.
    params.addParameter(AbstractDatabaseConnection.Parameterizer.FILTERS_ID, new FixedDBIDsFilter(1));
    return makeSimpleDatabase(filename, expectedSize, params);
  }

  /**
   * Generate a simple DoubleVector database from a file.
   *
   * @param filename File to load
   * @param expectedSize Expected size in records
   * @param params Extra parameters
   * @return Database
   */
  public static Database makeSimpleDatabase(String filename, int expectedSize, ListParameterization params) {
    assertNotNull("Params, if given, must not be null.", params);
    // Allow loading test data from resources.
    try (InputStream is = open(filename)) {
      params.addParameter(AbstractDatabase.Parameterizer.DATABASE_CONNECTION_ID, InputStreamDatabaseConnection.class);
      params.addParameter(InputStreamDatabaseConnection.Parameterizer.STREAM_ID, is);
      Database db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, params);

      // Ensure we have no unused parameters:
      if(params.hasUnusedParameters()) {
        fail("Unused parameters: " + params.getRemainingParameters());
      }
      // And report any parameterization errors:
      if(params.hasErrors()) {
        params.logAndClearReportedErrors();
        fail("Parameterization errors.");
      }

      db.initialize();
      // Check the relation has the expected size:
      Relation<?> rel = db.getRelation(TypeUtil.ANY);
      if(expectedSize > 0) {
        assertEquals("Database size does not match.", expectedSize, rel.size());
      }
      return db;
    }
    catch(IOException e) {
      fail("Test data " + filename + " not found.");
      return null; // Not reached.
    }
  }

  /**
   * Open a resource input stream. Use gzip if the name ends with .gz.
   * (Autodetection currently does not work on resource streams.)
   * 
   * @param filename resource name
   * @return Input stream
   * @throws IOException
   */
  public static InputStream open(String filename) throws IOException {
    InputStream is = AbstractSimpleAlgorithmTest.class.getClassLoader().getResourceAsStream(filename);
    if(is == null) {
      throw new IOException("Resource not found: " + filename);
    }
    return filename.endsWith(".gz") ? new GZIPInputStream(is) : is;
  }
}
