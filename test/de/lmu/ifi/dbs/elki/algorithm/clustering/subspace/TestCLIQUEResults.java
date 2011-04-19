package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full CLIQUE run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that CLIQUE performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Elke Achtert
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestCLIQUEResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run CLIQUE with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testCLIQUEResults() throws ParameterException {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-simple.csv", 600);

    ListParameterization params = new ListParameterization();
    params.addParameter(CLIQUE.TAU_ID, "0.1");
    params.addParameter(CLIQUE.XSI_ID, 20);

    // setup algorithm
    CLIQUE<DoubleVector> clique = ClassGenericsUtil.parameterizeOrAbort(CLIQUE.class, params);
    testParameterizationOk(params);

    // run CLIQUE on database
    Clustering<SubspaceModel<DoubleVector>> result = clique.run(db);
    // Run by-label as reference
    ByLabelClustering bylabel = new ByLabelClustering(true, null);
    Clustering<Model> rbl = bylabel.run(db);

    double score = PairCountingFMeasure.compareClusterings(result, rbl, 1.0);
    org.junit.Assert.assertEquals(this.getClass().getSimpleName() + ": Score does not match.", 0.9882, score, 0.0001);
    testClusterSizes(result, new int[] { 200, 200, 216, 400 });
  }

  /**
   * Run CLIQUE with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testCLIQUESubspaceOverlapping() throws ParameterException {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-3-4d.ascii", 850);
  
    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(CLIQUE.TAU_ID, 0.2);
    params.addParameter(CLIQUE.XSI_ID, 6);
    CLIQUE<DoubleVector> clique = ClassGenericsUtil.parameterizeOrAbort(CLIQUE.class, params);
    testParameterizationOk(params);
  
    // run CLIQUE on database
    Clustering<SubspaceModel<DoubleVector>> result = clique.run(db);
    testFMeasure(db, result, 0.433661);
    testClusterSizes(result, new int[] { 255, 409, 458, 458, 480 });
  }
}