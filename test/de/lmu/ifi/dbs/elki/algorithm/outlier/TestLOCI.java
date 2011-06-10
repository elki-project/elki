package de.lmu.ifi.dbs.elki.algorithm.outlier;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Tests the LOCI algorithm.
 * 
 * @author Lucia Cichella
 */
public class TestLOCI extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testLOCI() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);

    // Parameterization
    ListParameterization params = new ListParameterization();
    params.addParameter(LOCI.RMAX_ID, 0.5);

    // setup Algorithm
    LOCI<DoubleVector, DoubleDistance> loci = ClassGenericsUtil.parameterizeOrAbort(LOCI.class, params);
    testParameterizationOk(params);

    // run LOCI on database
    OutlierResult result = loci.run(db);

    testAUC(db, "Noise", result, 0.954444);
    testSingleScore(result, 146, 4.14314916);
  }
}