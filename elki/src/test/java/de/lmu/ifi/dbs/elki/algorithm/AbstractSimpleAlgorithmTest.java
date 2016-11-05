package de.lmu.ifi.dbs.elki.algorithm;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.InputStreamDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.filter.FixedDBIDsFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser;
import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable;
import de.lmu.ifi.dbs.elki.evaluation.outlier.OutlierROCCurve;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
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
      if (params == null) {
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
      if (expectedSize > 0) {
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

  /**
   * Find a clustering result, fail if there is more than one or none.
   *
   * @param result Base result
   * @return Clustering
   */
  protected Clustering<?> findSingleClustering(Result result) {
    List<Clustering<? extends Model>> clusterresults = ResultUtil.getClusteringResults(result);
    assertTrue("No unique clustering found in result.", clusterresults.size() == 1);
    Clustering<? extends Model> clustering = clusterresults.get(0);
    return clustering;
  }

  /**
   * Test the clustering result by comparing the score with an expected value.
   *
   * @param database Database to test
   * @param clustering Clustering result
   * @param expected Expected score
   */
  protected <O> void testFMeasure(Database database, Clustering<?> clustering, double expected) {
    // Run by-label as reference
    ByLabelClustering bylabel = new ByLabelClustering();
    Clustering<Model> rbl = bylabel.run(database);

    ClusterContingencyTable ct = new ClusterContingencyTable(true, false);
    ct.process(clustering, rbl);
    double score = ct.getPaircount().f1Measure();
    Logging.getLogger(this.getClass()).verbose(this.getClass().getSimpleName() + " score: " + score + " expect: " + expected);
    assertEquals(this.getClass().getSimpleName() + ": Score does not match.", expected, score, 0.0001);
  }

  /**
   * Validate the cluster sizes with an expected result.
   *
   * @param clustering Clustering to test
   * @param expected Expected cluster sizes
   */
  protected void testClusterSizes(Clustering<?> clustering, int[] expected) {
    List<? extends Cluster<?>> clusters = clustering.getAllClusters();
    int[] sizes = new int[clusters.size()];
    for(int i = 0; i < sizes.length; ++i) {
      sizes[i] = clusters.get(i).size();
    }
    // Sort both
    Arrays.sort(sizes);
    Arrays.sort(expected);
    // Test
    assertEquals("Number of clusters does not match expectations. " + FormatUtil.format(sizes), expected.length, sizes.length);
    for(int i = 0; i < expected.length; i++) {
      assertEquals("Cluster size does not match at position " + i + " in " + FormatUtil.format(sizes), expected[i], sizes[i]);
    }
  }

  /**
   * Test the AUC value for an outlier result.
   *
   * @param db Database
   * @param positive Positive class name
   * @param result Outlier result to process
   * @param expected Expected AUC value
   */
  protected void testAUC(Database db, String positive, OutlierResult result, double expected) {
    ListParameterization params = new ListParameterization();
    params.addParameter(OutlierROCCurve.Parameterizer.POSITIVE_CLASS_NAME_ID, positive);
    OutlierROCCurve rocCurve = ClassGenericsUtil.parameterizeOrAbort(OutlierROCCurve.class, params);

    // Ensure the result has been added to the hierarchy:
    ResultHierarchy hier = db.getHierarchy();
    if(hier.numParents(result) < 1) {
      hier.add(db, result);
    }

    // Compute ROC and AUC:
    rocCurve.processNewResult(hier, result);
    // Find the ROC results
    Collection<OutlierROCCurve.ROCResult> rocs = ResultUtil.filterResults(hier, result, OutlierROCCurve.ROCResult.class);
    assertTrue("No ROC result found.", !rocs.isEmpty());
    double auc = rocs.iterator().next().getAUC();
    assertFalse("More than one ROC result found.", rocs.size() > 1);
    assertEquals("ROC value does not match.", expected, auc, 0.0001);
  }

  /**
   * Test the outlier score of a single object.
   *
   * @param result Result object to use
   * @param id Object ID
   * @param expected expected value
   */
  protected void testSingleScore(OutlierResult result, int id, double expected) {
    assertNotNull("No outlier result", result);
    assertNotNull("No score result.", result.getScores());
    final DBID dbid = DBIDUtil.importInteger(id);
    assertNotNull("No result for ID " + id, result.getScores().doubleValue(dbid));
    double actual = result.getScores().doubleValue(dbid);
    assertEquals("Outlier score of object " + id + " doesn't match.", expected, actual, 0.0001);
  }
}
