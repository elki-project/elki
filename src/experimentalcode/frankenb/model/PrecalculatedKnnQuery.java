/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.integer.IntegerDBID;
import de.lmu.ifi.dbs.elki.database.query.AbstractKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class PrecalculatedKnnQuery<O extends DatabaseObject> extends AbstractKNNQuery<O, DoubleDistance> {

  /**
   * OptionID for {@link #PRECALC_DIR_PARAM}
   */
  public static final OptionID PRECALC_DIR_ID = OptionID.getOrCreateOptionID("precalculation.dir", "");

  /**
   * OptionID for {@link #PRECALC_DIR_PARAM}
   */
  public static final OptionID PRECALC_DAT_ID = OptionID.getOrCreateOptionID("precalculation.dat", "");

  /**
   * Parameter that specifies the name of the precalculated tree's directory file.
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
   * @param config
   */
  public PrecalculatedKnnQuery(Parameterization config) {
    super(config);
    config = config.descend(this);
    
    PRECALC_DIR_PARAM.setShortDescription("Directory file of the precalculated tree");
    PRECALC_DAT_PARAM.setShortDescription("Data file of the precalculated tree");
    
    if (config.grab(PRECALC_DIR_PARAM) && config.grab(PRECALC_DAT_PARAM)) {
      File resultDirectory = PRECALC_DIR_PARAM.getValue();
      File resultData = PRECALC_DAT_PARAM.getValue();

      try {
        resultTree = new DynamicBPlusTree<Integer, DistanceList>(
            resultDirectory,
            resultData,
            new ConstantSizeIntegerSerializer(),
            new DistanceListSerializer()
        );
      } catch (IOException e) {
        throw new RuntimeException("Problem opening the result tree", e);
      }
    }
    
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.database.query.AbstractKNNQuery#instantiate(de.lmu.ifi.dbs.elki.database.Database)
   */
  @Override
  public <T extends O> AbstractKNNQuery.Instance<T, DoubleDistance> instantiate(Database<T> database) {
    Map<Integer, DBID> dbidMap = new HashMap<Integer, DBID>();
    for (DBID dbid : database.getIDs()) {
      dbidMap.put(dbid.getIntegerID(), dbid);
    }
    return new PrecalculatedKnnQueryInstance<T>(resultTree, dbidMap);
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.database.query.AbstractKNNQuery#instantiate(de.lmu.ifi.dbs.elki.database.Database, de.lmu.ifi.dbs.elki.database.query.DistanceQuery)
   */
  @Override
  public <T extends O> AbstractKNNQuery.Instance<T, DoubleDistance> instantiate(Database<T> database, DistanceQuery<T, DoubleDistance> distanceQuery) {
    return instantiate(database);
  }
  
  public static class PrecalculatedKnnQueryInstance<O extends DatabaseObject> extends AbstractKNNQuery.Instance<O, DoubleDistance> {

    private final DynamicBPlusTree<Integer, DistanceList> resultTree;
    private final Map<Integer, DBID> dbidMap;
    
    /**
     * @param database
     * @param distanceQuery
     */
    public PrecalculatedKnnQueryInstance(DynamicBPlusTree<Integer, DistanceList> resultTree, Map<Integer, DBID> dbidMap) {
      super(null, null);
      this.resultTree = resultTree;
      this.dbidMap = dbidMap;
    }

    /* (non-Javadoc)
     * @see de.lmu.ifi.dbs.elki.database.query.AbstractKNNQuery.Instance#get(de.lmu.ifi.dbs.elki.database.ids.DBID)
     */
    @Override
    public List<DistanceResultPair<DoubleDistance>> get(DBID id) {
      try {
        DistanceList distanceList = this.resultTree.get(id.getIntegerID());
        List<DistanceResultPair<DoubleDistance>> list = new ArrayList<DistanceResultPair<DoubleDistance>>();
        if (distanceList == null) {
          throw new RuntimeException("This seems not to be the precalculated result for the given database as the id " + id.getIntegerID() + " is not contained in the precalculated results");
        }
        
        for (Pair<Integer, Double> distance : distanceList) {
          list.add(new DistanceResultPair<DoubleDistance>(new DoubleDistance(distance.second), dbidMap.get(distance.first)));
        }
        
        return list;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
  }


}
