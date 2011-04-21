/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
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
import experimentalcode.frankenb.main.KnnDataMerger;
import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;

/**
 * This class can be used as an index in combination with a precalculated kNN result that
 * {@link KnnDataMerger} provides. The result normally consists of two files - one directory file (.dir)
 * and one data file (.dat)
 * 
 * @author Florian Frankenberger
 */
@Title("Precalculated kNN Neighborhood index")
@Description("Uses a precalculated kNN Neighborhood as index for a given database. Be aware that the index must be generated from the database.")
public class PrecalculatedKnnIndex<O> implements KNNIndex<O> {

  private DynamicBPlusTree<Integer, DistanceList> resultTree;
  private Relation<O> relation;

  public PrecalculatedKnnIndex(Relation<O> relation, DynamicBPlusTree<Integer, DistanceList> resultTree) {
    this.relation = relation;
    this.resultTree = resultTree;
  }
  
  public void setResultTree(DynamicBPlusTree<Integer, DistanceList> resultTree) {
    this.resultTree = resultTree;
  }

  @Override
  public PageFileStatistics getPageFileStatistics() {
    return null;
  }

  @Override
  public void insert(DBID id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void insertAll(DBIDs ids) {
    //throw new UnsupportedOperationException();
  }

  @Override
  public boolean delete(DBID id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteAll(DBIDs ids) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getLongName() {
    return "Precalculated Knn Query";
  }

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
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    return (KNNQuery<O, D>) new PrecalculatedKnnQuery<O>(this.relation, this.resultTree);
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
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    return (KNNQuery<O, D>) new PrecalculatedKnnQuery<O>(this.relation, this.resultTree);
  }

  public static class Factory<O, D extends Distance<D>> implements IndexFactory<O, KNNIndex<O>> {

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
    public PrecalculatedKnnIndex<O> instantiate(Relation<O> database) {
      return new PrecalculatedKnnIndex<O>(database, resultTree);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return TypeUtil.ANY;
    }
  }

  @Override
  public Relation<O> getRelation() {
    return this.relation;
  }
}