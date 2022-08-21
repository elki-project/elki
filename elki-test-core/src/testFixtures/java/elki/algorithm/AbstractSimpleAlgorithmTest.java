/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.algorithm;

import static org.junit.Assert.*;

import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.datasource.AbstractDatabaseConnection;
import elki.datasource.FileBasedDatabaseConnection;
import elki.datasource.filter.FixedDBIDsFilter;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.optionhandling.parameterization.ListParameterization;

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
    params.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, new FixedDBIDsFilter(1));
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
    Database db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, params //
        .addParameter(FileBasedDatabaseConnection.Par.INPUT_ID, //
            AbstractSimpleAlgorithmTest.class.getClassLoader().getResource(filename)));
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
    if(expectedSize > 0) {
      assertEquals("Database size does not match.", expectedSize, db.getRelation(TypeUtil.ANY).size());
    }
    return db;
  }
}
