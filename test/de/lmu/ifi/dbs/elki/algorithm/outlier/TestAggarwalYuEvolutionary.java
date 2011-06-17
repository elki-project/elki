package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the AggarwalYuEvolutionary algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestAggarwalYuEvolutionary extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testAggarwalYuEvolutionary() {
    Database db = makeSimpleDatabase(UNITTEST + "outlier-3d-3clusters.ascii", 960);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(AggarwalYuEvolutionary.K_ID, 2);
    params.addParameter(AggarwalYuEvolutionary.PHI_ID, 8);
    params.addParameter(AggarwalYuEvolutionary.M_ID, 5);
    params.addParameter(AggarwalYuEvolutionary.SEED_ID, 0);

    // setup Algorithm
    AggarwalYuEvolutionary<DoubleVector> aggarwalYuEvolutionary = ClassGenericsUtil.parameterizeOrAbort(AggarwalYuEvolutionary.class, params);
    testParameterizationOk(params);

    // run AggarwalYuEvolutionary on database
    OutlierResult result = aggarwalYuEvolutionary.run(db);

    testSingleScore(result, 945, 16.6553612449883);
    testAUC(db, "Noise", result, 0.5799537037037);
  }
}