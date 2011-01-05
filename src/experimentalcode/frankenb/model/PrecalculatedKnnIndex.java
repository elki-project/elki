/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
@Title("Precalculated kNN Neighborhood index")
@Description("Uses a precalculated kNN Neighborhood as index for a given database. Be aware that the index must fit the database.")
public class PrecalculatedKnnIndex<O extends DatabaseObject> implements KNNIndex<O> {

  private DynamicBPlusTree<Integer, DistanceList> resultTree;

  public PrecalculatedKnnIndex(DynamicBPlusTree<Integer, DistanceList> resultTree) {
    this.resultTree = resultTree;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.lmu.ifi.dbs.elki.index.Index#getPageFileStatistics()
   */
  @Override
  public PageFileStatistics getPageFileStatistics() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.lmu.ifi.dbs.elki.index.Index#insert(de.lmu.ifi.dbs.elki.data.DatabaseObject
   * )
   */
  @Override
  public void insert(O object) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.lmu.ifi.dbs.elki.index.Index#insert(java.util.List)
   */
  @Override
  public void insert(List<O> objects) {
    //throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.lmu.ifi.dbs.elki.index.Index#delete(de.lmu.ifi.dbs.elki.data.DatabaseObject
   * )
   */
  @Override
  public boolean delete(O object) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.lmu.ifi.dbs.elki.index.Index#delete(java.util.List)
   */
  @Override
  public void delete(List<O> objects) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.lmu.ifi.dbs.elki.result.Result#getLongName()
   */
  @Override
  public String getLongName() {
    return "Precalculated Knn Query";
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.lmu.ifi.dbs.elki.result.Result#getShortName()
   */
  @Override
  public String getShortName() {
    return "Precalculated Knn Query";
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.lmu.ifi.dbs.elki.index.KNNIndex#getKNNQuery(de.lmu.ifi.dbs.elki.database
   * .Database, de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction,
   * java.lang.Object[])
   */
  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(Database<O> database, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    return (KNNQuery<O, D>) new PrecalculatedKnnQuery<O>(this.resultTree);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.lmu.ifi.dbs.elki.index.KNNIndex#getKNNQuery(de.lmu.ifi.dbs.elki.database
   * .Database, de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery,
   * java.lang.Object[])
   */
  @SuppressWarnings("unchecked")
  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(Database<O> database, DistanceQuery<O, D> distanceQuery, Object... hints) {
    return (KNNQuery<O, D>) new PrecalculatedKnnQuery<O>(this.resultTree);
  }

  public static class Factory<O extends DatabaseObject, D extends Distance<D>> implements IndexFactory<O, KNNIndex<O>> {

    /**
     * OptionID for {@link #PRECALC_DIR_PARAM}
     */
    public static final OptionID PRECALC_DIR_ID = OptionID.getOrCreateOptionID("precalculation.directoryfile", "");

    /**
     * OptionID for {@link #PRECALC_DIR_PARAM}
     */
    public static final OptionID PRECALC_DAT_ID = OptionID.getOrCreateOptionID("precalculation.datafile", "");

    /**
     * Parameter that specifies the name of the precalculated tree's directory
     * file.
     * <p>
     * Key: {@code -precalculation.dir}
     * </p>
     */
    private final FileParameter PRECALC_DIR_PARAM = new FileParameter(PRECALC_DIR_ID, FileParameter.FileType.INPUT_FILE);

    /**
     * Parameter that specifies the name of the precalculated tree's data file.
     * <p>
     * Key: {@code -precalculation.dir}
     * </p>
     */
    private final FileParameter PRECALC_DAT_PARAM = new FileParameter(PRECALC_DAT_ID, FileParameter.FileType.INPUT_FILE);

    private DynamicBPlusTree<Integer, DistanceList> resultTree;

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      config = config.descend(this);

      PRECALC_DIR_PARAM.setShortDescription("Directory file of the precalculated tree");
      PRECALC_DAT_PARAM.setShortDescription("Data file of the precalculated tree");

      if(config.grab(PRECALC_DIR_PARAM) && config.grab(PRECALC_DAT_PARAM)) {
        File resultDirectory = PRECALC_DIR_PARAM.getValue();
        File resultData = PRECALC_DAT_PARAM.getValue();

        try {
          resultTree = new DynamicBPlusTree<Integer, DistanceList>(
              new BufferedDiskBackedDataStorage(resultDirectory), 
              new BufferedDiskBackedDataStorage(resultData), 
              new ConstantSizeIntegerSerializer(), 
              new DistanceListSerializer()
              );
        }
        catch(IOException e) {
          throw new RuntimeException("Problem opening the result tree", e);
        }
      }
    }

    @Override
    public PrecalculatedKnnIndex<O> instantiate(Database<O> database) {
      return new PrecalculatedKnnIndex<O>(resultTree);
    }

  }
}
