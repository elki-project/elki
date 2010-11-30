package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.preprocessing.SharedNearestNeighborsPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * SharedNearestNeighborSimilarityFunction with a pattern defined to accept
 * Strings that define a non-negative Integer.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has SharedNearestNeighborsPreprocessor
 * @apiviz.has Instance oneway - - produces
 * 
 * @param <O> object type
 * @param <D> distance type
 */
// todo arthur comment class
public class SharedNearestNeighborSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractPreprocessorBasedSimilarityFunction<O, SharedNearestNeighborsPreprocessor<O, D>, TreeSetDBIDs, IntegerDistance> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SharedNearestNeighborSimilarityFunction(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @Override
  public IntegerDistance getDistanceFactory() {
    return IntegerDistance.FACTORY;
  }

  /**
   * @return the name of the default preprocessor, which is
   *         {@link SharedNearestNeighborsPreprocessor}
   */
  @Override
  public Class<?> getDefaultPreprocessorClass() {
    return SharedNearestNeighborsPreprocessor.class;
  }

  @Override
  public String getPreprocessorDescription() {
    return "The Classname of the preprocessor to determine the neighbors of the objects.";
  }

  /**
   * @return the super class for the preprocessor, which is
   *         {@link SharedNearestNeighborsPreprocessor}
   */
  @Override
  public Class<SharedNearestNeighborsPreprocessor<O, D>> getPreprocessorSuperClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(SharedNearestNeighborsPreprocessor.class);
  }

  static protected int countSharedNeighbors(TreeSetDBIDs neighbors1, TreeSetDBIDs neighbors2) {
    int intersection = 0;
    Iterator<DBID> iter1 = neighbors1.iterator();
    Iterator<DBID> iter2 = neighbors2.iterator();
    DBID neighbors1ID = null;
    DBID neighbors2ID = null;
    if(iter1.hasNext()) {
      neighbors1ID = iter1.next();
    }
    if(iter2.hasNext()) {
      neighbors2ID = iter2.next();
    }
    while((iter1.hasNext() || iter2.hasNext()) && neighbors1ID != null && neighbors2ID != null) {
      if(neighbors1ID.equals(neighbors2ID)) {
        intersection++;
        if(iter1.hasNext()) {
          neighbors1ID = iter1.next();
        }
        else {
          neighbors1ID = null;
        }
        if(iter2.hasNext()) {
          neighbors2ID = iter2.next();
        }
        else {
          neighbors2ID = null;
        }
      }
      else if(neighbors2ID.compareTo(neighbors1ID) < 0) {
        if(iter1.hasNext()) {
          neighbors1ID = iter1.next();
        }
        else {
          neighbors1ID = null;
        }
      }
      else // neighbors1ID > neighbors2ID
      {
        if(iter2.hasNext()) {
          neighbors2ID = iter2.next();
        }
        else {
          neighbors2ID = null;
        }
      }
    }
    return intersection;
  }

  @Override
  public Class<? super O> getInputDatatype() {
    return DatabaseObject.class;
  }

  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database) {
    return new Instance<T, D>(database, getPreprocessor().instantiate(database));
  }

  /**
   * TODO: document
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has de.lmu.ifi.dbs.elki.preprocessing.SharedNearestNeighborsPreprocessor.Instance
   * 
   * @param <O>
   * @param <D>
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractPreprocessorBasedSimilarityFunction.Instance<O, SharedNearestNeighborsPreprocessor.Instance<O, D>, TreeSetDBIDs, IntegerDistance> {
    public Instance(Database<O> database, SharedNearestNeighborsPreprocessor.Instance<O, D> preprocessor) {
      super(database, preprocessor);
    }

    @Override
    public IntegerDistance similarity(DBID id1, DBID id2) {
      TreeSetDBIDs neighbors1 = preprocessor.get(id1);
      TreeSetDBIDs neighbors2 = preprocessor.get(id2);
      return new IntegerDistance(countSharedNeighbors(neighbors1, neighbors2));
    }

    @Override
    public IntegerDistance getDistanceFactory() {
      return IntegerDistance.FACTORY;
    }
  }
}