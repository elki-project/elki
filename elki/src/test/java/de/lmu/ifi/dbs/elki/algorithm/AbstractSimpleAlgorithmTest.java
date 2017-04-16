/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.InputStreamDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.filter.FixedDBIDsFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Abstract base class useful for testing various algorithms.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public abstract class AbstractSimpleAlgorithmTest {
  /**
   * Base path for unit test files.
   */
  public final static String UNITTEST = "data/testdata/unittests/";

  /**
   * Validate that parameterization succeeded: no parameters left, no
   * parameterization errors.
   *
   * @param config Parameterization to test
   */
  public static void testParameterizationOk(ListParameterization config) {
    if(config.hasUnusedParameters()) {
      fail("Unused parameters: " + config.getRemainingParameters());
    }
    if(config.hasErrors()) {
      config.logAndClearReportedErrors();
      fail("Parameterization errors.");
    }
  }

  /**
   * Generate a simple DoubleVector database from a file.
   *
   * @param filename File to load
   * @param expectedSize Expected size in records
   * @param params Extra parameters
   * @return Database
   */
  public static <T> Database makeSimpleDatabase(String filename, int expectedSize, ListParameterization params, Class<?>[] filters) {
    // Allow loading test data from resources.
    try (InputStream is = open(filename)) {
      if(params == null) {
        params = new ListParameterization();
      }
      // Instantiate filters manually. TODO: redesign
      List<ObjectFilter> filterlist = new ArrayList<>();
      filterlist.add(new FixedDBIDsFilter(1));
      if(filters != null) {
        for(Class<?> filtercls : filters) {
          ObjectFilter filter = ClassGenericsUtil.parameterizeOrAbort(filtercls, params);
          filterlist.add(filter);
        }
      }
      // Setup parser and data loading
      NumberVectorLabelParser<DoubleVector> parser = new NumberVectorLabelParser<>(DoubleVector.FACTORY);
      InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, filterlist, parser);

      // We want to allow the use of indexes via "params"
      params.addParameter(AbstractDatabase.Parameterizer.DATABASE_CONNECTION_ID, dbc);
      Database db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, params);

      testParameterizationOk(params);

      db.initialize();
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

  /**
   * Generate a simple DoubleVector database from a file.
   *
   * @param filename File to load
   * @param expectedSize Expected size in records
   * @return Database
   */
  protected <T> Database makeSimpleDatabase(String filename, int expectedSize) {
    return makeSimpleDatabase(filename, expectedSize, new ListParameterization(), null);
  }
}
