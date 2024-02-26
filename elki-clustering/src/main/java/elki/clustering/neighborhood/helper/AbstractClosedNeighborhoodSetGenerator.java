package elki.clustering.neighborhood.helper;

import java.util.ArrayList;
import java.util.List;

import elki.Algorithm;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.PrimitiveDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.statistics.Duration;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * 
 * @author Niklas Strahmann
 */
public abstract class AbstractClosedNeighborhoodSetGenerator<V> implements ClosedNeighborhoodSetGenerator<V> {
  public List<DBIDs> getClosedNeighborhoods(Relation<? extends V> relation) {
    Duration cnsTime = getLogger().newDuration(this.getClass().getName() + ".time").begin();
    // TODO: use ModifiableSetDBIDs?
    WritableDataStore<Boolean> visited = DataStoreFactory.FACTORY.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB, Boolean.class);
    ArrayList<ModifiableDBIDs> connectedComponents = new ArrayList<>();

    for(DBIDIter element = relation.iterDBIDs(); element.valid(); element.advance()) {
      visited.put(element, false);
    }

    int currentComponentIndex = 0;
    for(DBIDIter element = relation.iterDBIDs(); element.valid(); element.advance()) {
      if(visited.get(element)) {
        continue;
      }
      connectedComponents.add(currentComponentIndex, DBIDUtil.newArray());
      DFSaddComponents(element, visited, connectedComponents.get(currentComponentIndex++));
    }

    List<DBIDs> finalComponents = new ArrayList<>(currentComponentIndex);
    for(int i = 0; i < currentComponentIndex; i++) {
      finalComponents.add(connectedComponents.get(i));
    }
    getLogger().statistics(cnsTime.end());
    return finalComponents;
  }

  private void DFSaddComponents(DBIDRef element, WritableDataStore<Boolean> visited, ModifiableDBIDs component) {
    if(!visited.get(element)) {
      visited.put(element, true);
      component.add(element);
      for(DBIDIter iter = getNeighbors(element).iter(); iter.valid(); iter.advance()) {
        DFSaddComponents(iter, visited, component);
      }
    }
  }

  protected abstract DBIDs getNeighbors(DBIDRef element);

  protected abstract Logging getLogger();
  
  /**
   * 
   * @author Niklas Strahmann
   */
  public abstract static class Par<V> implements Parameterizer {
    protected Distance<? super V> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super V>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, PrimitiveDistance.class, SquaredEuclideanDistance.class).grab(config, x -> distance = x);

    }

    @Override
    public abstract AbstractClosedNeighborhoodSetGenerator<V> make();
  }
}
