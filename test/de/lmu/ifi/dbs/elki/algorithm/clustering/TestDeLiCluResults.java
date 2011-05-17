package de.lmu.ifi.dbs.elki.algorithm.clustering;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu.DeLiCluTreeFactory;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full DeLiClu run, and compares the result with a clustering
 * derived from the data set labels. This test ensures that DeLiClu's
 * performance doesn't unexpectedly drop on this data set (and also ensures that
 * the algorithms work, as a side effect).
 * 
 * @author Katharina Rausch
 * @author Erich Schubert
 */
public class TestDeLiCluResults extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run DeLiClu with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testDeLiCluResults() throws ParameterException {
    ListParameterization indexparams = new ListParameterization();
    // We need a special index for this algorithm:
    indexparams.addParameter(StaticArrayDatabase.INDEX_ID, DeLiCluTreeFactory.class);
    indexparams.addParameter(DeLiCluTreeFactory.PAGE_SIZE_ID, 1000);
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710, indexparams, null);
    
    // Setup actual algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DeLiClu.MINPTS_ID, 18);
    params.addParameter(OPTICSXi.XI_ID, 0.038);
    params.addParameter(OPTICSXi.XIALG_ID, DeLiClu.class);
    OPTICSXi<DoubleDistance> opticsxi = ClassGenericsUtil.parameterizeOrAbort(OPTICSXi.class, params);
    testParameterizationOk(params);

    // run DeLiClu on database
    Clustering<?> clustering = opticsxi.run(db);
    testFMeasure(db, clustering, 0.87406257);
    testClusterSizes(clustering, new int[] { 109, 121, 210, 270 });
  }
}