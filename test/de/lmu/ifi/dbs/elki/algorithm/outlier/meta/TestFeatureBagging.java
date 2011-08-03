package de.lmu.ifi.dbs.elki.algorithm.outlier.meta;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the Feature Bagging algorithm.
 * 
 * @author Erich Schubert
 */
public class TestFeatureBagging extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testFeatureBaggingSum() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LOF.K_ID, 10);
    params.addParameter(FeatureBagging.Parameterizer.NUM_ID, 10);
    params.addParameter(FeatureBagging.Parameterizer.SEED_ID, 1);

    // setup Algorithm
    FeatureBagging fb = ClassGenericsUtil.parameterizeOrAbort(FeatureBagging.class, params);
    testParameterizationOk(params);

    // run AggarwalYuEvolutionary on database
    OutlierResult result = fb.run(db);

    testSingleScore(result, 1293, 11.8295414);
    testAUC(db, "Noise", result, 0.9066106);
  }

  @Test
  public void testFeatureBaggingBreadth() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-axis-subspaces-6d.ascii", 1345);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LOF.K_ID, 10);
    params.addParameter(FeatureBagging.Parameterizer.NUM_ID, 10);
    params.addParameter(FeatureBagging.Parameterizer.SEED_ID, 5);
    params.addFlag(FeatureBagging.Parameterizer.BREADTH_ID);

    // setup Algorithm
    FeatureBagging fb = ClassGenericsUtil.parameterizeOrAbort(FeatureBagging.class, params);
    testParameterizationOk(params);

    // run AggarwalYuEvolutionary on database
    OutlierResult result = fb.run(db);

    testSingleScore(result, 1293, 1.321709879);
    testAUC(db, "Noise", result, 0.88466106);
  }
}