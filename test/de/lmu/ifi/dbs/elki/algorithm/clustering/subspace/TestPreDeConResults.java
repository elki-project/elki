package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full PreDeCon run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that PreDeCon performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 * 
 * @author Erich Schubert
 */
public class TestPreDeConResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run PreDeCon with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testPreDeConResults() throws ParameterException {
    // Additional input parameters
    ListParameterization inp = new ListParameterization();
    inp.addParameter(FileBasedDatabaseConnection.CLASS_LABEL_INDEX_ID, 1);
    Database<DoubleVector> db = makeSimpleDatabase(UNITTEST + "axis-parallel-subspace-clusters-6d.csv.gz", 2500, inp);

    ListParameterization params = new ListParameterization();
    // PreDeCon
    // FIXME: These parameters do NOT work...
    params.addParameter(AbstractProjectedDBSCAN.EPSILON_ID, 50);
    params.addParameter(AbstractProjectedDBSCAN.MINPTS_ID, 50);
    params.addParameter(AbstractProjectedDBSCAN.LAMBDA_ID, 2);

    // setup algorithm
    PreDeCon<DoubleVector> predecon = ClassGenericsUtil.parameterizeOrAbort(PreDeCon.class, params);
    testParameterizationOk(params);

    // run PredeCon on database
    Clustering<Model> result = predecon.run(db);

    // FIXME: find working parameters...
    testFMeasure(db, result, 0.40153);
    testClusterSizes(result, new int[] { 2500 });
  }
}