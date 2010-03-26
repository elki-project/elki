package de.lmu.ifi.dbs.elki.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.TrivialAllInOne;
import de.lmu.ifi.dbs.elki.algorithm.clustering.TrivialAllNoise;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Validate {@link PairCountingFMeasure} with respect to its ability to compare
 * data clusterings.
 * 
 * @author Erich Schubert
 */
public class TestPairCountingFMeasure implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  /**
   * Validate {@link PairCountingFMeasure} with respect to its ability to
   * compare data clusterings.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testCompareDatabases() throws ParameterException {
    ListParameterization params = new ListParameterization();
    // Input
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);

    // get database
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>(params);
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertTrue(db.size() == shoulds);

    // run all-in-one
    TrivialAllInOne<DoubleVector> allinone = new TrivialAllInOne<DoubleVector>();
    Clustering<Model> rai = allinone.run(db);

    // run all-in-noise
    TrivialAllNoise<DoubleVector> allinnoise = new TrivialAllNoise<DoubleVector>();
    Clustering<Model> ran = allinnoise.run(db);

    // run by-label
    ByLabelClustering<DoubleVector> bylabel = new ByLabelClustering<DoubleVector>();
    Clustering<?> rbl = bylabel.run(db);

    assertEquals(1.0, PairCountingFMeasure.compareClusterings(rai, rai), Double.MIN_VALUE);
    assertEquals(1.0, PairCountingFMeasure.compareClusterings(ran, ran), Double.MIN_VALUE);
    assertEquals(1.0, PairCountingFMeasure.compareClusterings(rbl, rbl), Double.MIN_VALUE);

    assertEquals(0.009950248756218905, PairCountingFMeasure.compareClusterings(ran, rbl), Double.MIN_VALUE);
    assertEquals(0.0033277870216306157, PairCountingFMeasure.compareClusterings(rai, ran), Double.MIN_VALUE);

    assertEquals(0.5 /* 0.3834296724470135 */, PairCountingFMeasure.compareClusterings(rai, rbl), Double.MIN_VALUE);
  }
}