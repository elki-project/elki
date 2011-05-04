package de.lmu.ifi.dbs.elki.algorithm;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.filter.FixedDBIDsFilter;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.evaluation.roc.ComputeROCCurve;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Abstract base class useful for testing various algorithms.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractSimpleAlgorithmTest {
  /**
   * Base path for unit test files.
   */
  public final static String UNITTEST = "data/testdata/unittests/";

  /**
   * Notice: this is okay for tests - don't use this for frequently used
   * objects, use a static instance instead!
   */
  protected Logging logger = Logging.getLogger(this.getClass());

  /**
   * Validate that parameterization succeeded: no parameters left, no
   * parameterization errors.
   * 
   * @param config Parameterization to test
   */
  protected void testParameterizationOk(ListParameterization config) {
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
  protected <T> Database makeSimpleDatabase(String filename, int expectedSize, ListParameterization params, Class<?>[] filters) {
    org.junit.Assert.assertTrue("Test data set not found: " + filename, (new File(filename)).exists());
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, filename);
    
    List<Class<?>> filterlist = new ArrayList<Class<?>>();
    filterlist.add(FixedDBIDsFilter.class);
    if (filters != null) {
      for (Class<?> filter : filters) {
        filterlist.add(filter);
      }
    }
    params.addParameter(FileBasedDatabaseConnection.FILTERS_ID, filterlist);
    params.addParameter(FixedDBIDsFilter.IDSTART_ID, 1);
    FileBasedDatabaseConnection dbconn = ClassGenericsUtil.parameterizeOrAbort(FileBasedDatabaseConnection.class, params);

    testParameterizationOk(params);

    Database db = dbconn.getDatabase();
    org.junit.Assert.assertEquals("Database size does not match.", expectedSize, db.size());
    return db;
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

    double score = PairCountingFMeasure.compareClusterings(clustering, rbl, 1.0);
    if(logger.isVerbose()) {
      logger.verbose(this.getClass().getSimpleName() + " score: " + score + " expect: " + expected);
    }
    org.junit.Assert.assertEquals(this.getClass().getSimpleName() + ": Score does not match.", expected, score, 0.0001);
  }

  /**
   * Test the clustering result by comparing the score with an expected value.
   * 
   * @param database Database to test
   * @param clustering Clustering result
   * @param expected Expected score
   */
  protected <O> void testFMeasureHierarchical(Database database, Clustering<?> clustering, double expected) {
    // Run by-label as reference
    ByLabelHierarchicalClustering bylabel = new ByLabelHierarchicalClustering();
    Clustering<Model> rbl = bylabel.run(database);

    double score = PairCountingFMeasure.compareClusterings(clustering, rbl, 1.0, false, true);
    if(logger.isVerbose()) {
      logger.verbose(this.getClass().getSimpleName() + " score: " + score + " expect: " + expected);
    }
    org.junit.Assert.assertEquals(this.getClass().getSimpleName() + ": Score does not match.", expected, score, 0.0001);
  }

  /**
   * Validate the cluster sizes with an expected result.
   * 
   * @param clustering Clustering to test
   * @param expected Expected cluster sizes
   */
  protected void testClusterSizes(Clustering<?> clustering, int[] expected) {
    List<Integer> sizes = new java.util.ArrayList<Integer>();
    for(Cluster<?> cl : clustering.getAllClusters()) {
      sizes.add(cl.size());
    }
    // Sort both
    Collections.sort(sizes);
    Arrays.sort(expected);
    // Report
    // if(logger.isVerbose()) {
    StringBuffer buf = new StringBuffer();
    buf.append("Cluster sizes: [");
    for(int i = 0; i < sizes.size(); i++) {
      if(i > 0) {
        buf.append(", ");
      }
      buf.append(sizes.get(i));
    }
    buf.append("]");
    // }
    // Test
    org.junit.Assert.assertEquals("Number of clusters does not match expectations." + buf.toString(), expected.length, sizes.size());
    for(int i = 0; i < expected.length; i++) {
      org.junit.Assert.assertEquals("Cluster size does not match at position " + i, expected[i], (int) sizes.get(i));
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
    params.addParameter(ComputeROCCurve.POSITIVE_CLASS_NAME_ID, positive);
    ComputeROCCurve rocCurve = ClassGenericsUtil.parameterizeOrAbort(ComputeROCCurve.class, params);

    // Ensure the result has been added to the hierarchy:
    if(db.getHierarchy().getParents(result).size() < 1) {
      db.getHierarchy().add(db, result);
    }

    // Compute ROC and AUC:
    rocCurve.processResult(db, result);
    // Find the ROC results
    Iterator<ComputeROCCurve.ROCResult> iter = ResultUtil.filteredResults(result, ComputeROCCurve.ROCResult.class);
    org.junit.Assert.assertTrue("No ROC result found.", iter.hasNext());
    double auc = iter.next().getAUC();
    org.junit.Assert.assertFalse("More than one ROC result found.", iter.hasNext());
    org.junit.Assert.assertEquals("ROC value does not match.", expected, auc, 0.0001);
  }

  /**
   * Test the outlier score of a single object.
   * 
   * @param result Result object to use
   * @param id Object ID
   * @param expected expected value
   */
  protected void testSingleScore(OutlierResult result, int id, double expected) {
    org.junit.Assert.assertNotNull("No outlier result", result);
    org.junit.Assert.assertNotNull("No score result.", result.getScores());
    final DBID dbid = DBIDUtil.importInteger(id);
    org.junit.Assert.assertNotNull("No result for ID " + id, result.getScores().getValueFor(dbid));
    double actual = result.getScores().getValueFor(dbid);
    org.junit.Assert.assertEquals("Outlier score of object " + id + " doesn't match.", expected, actual, 0.0001);
  }
}