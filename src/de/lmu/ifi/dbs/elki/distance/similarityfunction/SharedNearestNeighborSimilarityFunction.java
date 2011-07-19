package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * SharedNearestNeighborSimilarityFunction with a pattern defined to accept
 * Strings that define a non-negative Integer.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborIndex.Factory
 * @apiviz.uses Instance oneway - - «create»
 * 
 * @param <O> object type
 * @param <D> distance type
 */
// todo arthur comment class
public class SharedNearestNeighborSimilarityFunction<O, D extends Distance<D>> extends AbstractIndexBasedSimilarityFunction<O, SharedNearestNeighborIndex<O>, SetDBIDs, IntegerDistance> {
  /**
   * Constructor.
   * 
   * @param indexFactory Index factory.
   */
  public SharedNearestNeighborSimilarityFunction(SharedNearestNeighborIndex.Factory<O, SharedNearestNeighborIndex<O>> indexFactory) {
    super(indexFactory);
  }

  @Override
  public IntegerDistance getDistanceFactory() {
    return IntegerDistance.FACTORY;
  }

  static protected int countSharedNeighbors(SetDBIDs neighbors1, SetDBIDs neighbors2) {
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
      else if(neighbors2ID.compareTo(neighbors1ID) > 0) {
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

  @SuppressWarnings("unchecked")
  @Override
  public <T extends O> Instance<T, D> instantiate(Relation<T> database) {
    SharedNearestNeighborIndex<O> indexi = indexFactory.instantiate((Relation<O>) database);
    return (Instance<T, D>) new Instance<O, D>((Relation<O>) database, indexi);
  }

  /**
   * TODO: document
   * 
   * @author Erich Schubert
   * 
   * @apiviz.uses SharedNearestNeighborIndex
   * 
   * @param <D>
   */
  public static class Instance<O, D extends Distance<D>> extends AbstractIndexBasedSimilarityFunction.Instance<O, SharedNearestNeighborIndex<O>, SetDBIDs, IntegerDistance> {
    public Instance(Relation<O> database, SharedNearestNeighborIndex<O> preprocessor) {
      super(database, preprocessor);
    }

    @Override
    public IntegerDistance similarity(DBID id1, DBID id2) {
      SetDBIDs neighbors1 = index.getNearestNeighborSet(id1);
      SetDBIDs neighbors2 = index.getNearestNeighborSet(id2);
      return new IntegerDistance(countSharedNeighbors(neighbors1, neighbors2));
    }

    @Override
    public IntegerDistance getDistanceFactory() {
      return IntegerDistance.FACTORY;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractIndexBasedSimilarityFunction.Parameterizer<SharedNearestNeighborIndex.Factory<O, SharedNearestNeighborIndex<O>>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configIndexFactory(config, SharedNearestNeighborIndex.Factory.class, SharedNearestNeighborPreprocessor.Factory.class);
    }

    @Override
    protected SharedNearestNeighborSimilarityFunction<O, D> makeInstance() {
      return new SharedNearestNeighborSimilarityFunction<O, D>(factory);
    }
  }
}