package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full DiSH run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that DiSH performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestDiSHResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run DiSH with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testDiSHResults() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "subspace-hierarchy.csv", 450);

    ListParameterization params = new ListParameterization();
    params.addParameter(DiSH.EPSILON_ID, 0.005);
    params.addParameter(DiSH.MU_ID, 50);

    // setup algorithm
    DiSH<DoubleVector> dish = ClassGenericsUtil.parameterizeOrAbort(DiSH.class, params);
    testParameterizationOk(params);

    // run DiSH on database
    Clustering<SubspaceModel<DoubleVector>> result = dish.run(db);

    testFMeasureHierarchical(db, result, 0.9991258);
    testClusterSizes(result, new int[] { 51, 199, 200 });
  }

  /**
   * Run DiSH with fixed parameters and compare the result to a golden standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testDiSHSubspaceOverlapping() throws ParameterException {
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-4-5d.ascii", 1100);
  
    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DiSH.EPSILON_ID, 0.1);
    params.addParameter(DiSH.MU_ID, 30);
    DiSH<DoubleVector> dish = ClassGenericsUtil.parameterizeOrAbort(DiSH.class, params);
    testParameterizationOk(params);
  
    // run DiSH on database
    Clustering<SubspaceModel<DoubleVector>> result = dish.run(db);
    testFMeasure(db, result, 0.6376870);
    testClusterSizes(result, new int[] { 33, 52, 72, 109, 172, 314, 348 });
  }
}