package de.lmu.ifi.dbs.elki.test.evaluation;

import static org.junit.Assert.*;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.TrivialAllInOne;
import de.lmu.ifi.dbs.elki.algorithm.clustering.TrivialAllNoise;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.PairCountingFMeasure;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

public class TestPairCountingFMeasure {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/hierarchical-3d2d1d.csv";
  // size of the data set
  int shoulds = 600;

  @Test
  public void testCompareDatabases() throws ParameterException {
    FileBasedDatabaseConnection<DoubleVector> dbconn = new FileBasedDatabaseConnection<DoubleVector>();

    String[] inputparams = new String[0];
    // Set up database input file:
    inputparams = Util.addParameter(inputparams,
        FileBasedDatabaseConnection.INPUT_ID, dataset);
    inputparams = dbconn.setParameters(inputparams);
    // get database
    Database<DoubleVector> db = dbconn.getDatabase(null);

    // verify data set size.
    assertTrue(db.size() == shoulds);
    
    // run all-in-one
    TrivialAllInOne<DoubleVector> allinone = new TrivialAllInOne<DoubleVector>();
    allinone.run(db);
    Clustering<Cluster<Model>> rai = allinone.getResult();
    
    // run all-in-noise
    TrivialAllNoise<DoubleVector> allinnoise = new TrivialAllNoise<DoubleVector>();
    allinnoise.run(db);
    Clustering<Cluster<Model>> ran = allinnoise.getResult();
    
    // run by-label
    ByLabelClustering<DoubleVector> bylabel = new ByLabelClustering<DoubleVector>();
    bylabel.run(db);
    Clustering<Cluster<Model>> rbl = bylabel.getResult();
    
    assertEquals(1.0, PairCountingFMeasure.compareDatabases(rai, rai), Double.MIN_VALUE);
    assertEquals(1.0, PairCountingFMeasure.compareDatabases(ran, ran), Double.MIN_VALUE);
    assertEquals(1.0, PairCountingFMeasure.compareDatabases(rbl, rbl), Double.MIN_VALUE);
    
    assertEquals(0.009950248756218905, PairCountingFMeasure.compareDatabases(ran, rbl), Double.MIN_VALUE);
    assertEquals(0.0033277870216306157, PairCountingFMeasure.compareDatabases(rai, ran), Double.MIN_VALUE);

    assertEquals(0.5 /* 0.3834296724470135 */, PairCountingFMeasure.compareDatabases(rai, rbl), Double.MIN_VALUE);
}

}
