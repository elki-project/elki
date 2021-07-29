package elki.clustering.dbscan;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.model.Model;
import elki.database.Database;
import elki.utilities.ELKIBuilder;
import org.junit.Test;

public class TBADBSCANTest extends AbstractClusterAlgorithmTest {

  @Test
  public void testTBADBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<Model> result = new ELKIBuilder<TBADBSCAN<DoubleVector>>(TBADBSCAN.class) //
        .with(TBADBSCAN.Par.EPSILON_ID, 0.04) //
        .with(TBADBSCAN.Par.MINPTS_ID, 20) //
        .with(TBADBSCAN.Par.NREFPOINTS_ID, 4) //
        .with(TBADBSCAN.Par.MODE_ID, TBADBSCAN.RefPointMode.KPP) //
        .build().autorun(db);

    assertFMeasure(db, result, 0.996413);
    assertClusterSizes(result, new int[] { 29, 50, 101, 150 });
  }

  @Test
  public void testDBSCANOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<Model> result = new ELKIBuilder<TBADBSCAN<DoubleVector>>(TBADBSCAN.class) //
        .with(TBADBSCAN.Par.EPSILON_ID, 11.5) //
        .with(TBADBSCAN.Par.MINPTS_ID, 120) //
        .with(TBADBSCAN.Par.NREFPOINTS_ID, 4) //
        .with(TBADBSCAN.Par.MODE_ID, TBADBSCAN.RefPointMode.KPP) //
        .build().autorun(db);

    assertFMeasure(db, result, 0.954382);
    assertClusterSizes(result, new int[] { 11, 200, 203, 224 });
  }
}
