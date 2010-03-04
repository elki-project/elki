package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * MetricalIndexDatabase is a database implementation which is supported by a
 * metrical index structure.
 * 
 * @author Elke Achtert
 * @param <O> the type of FeatureVector as element of the database
 * @param <D> Distance type
 * @param <N> Node type
 * @param <E> Entry type
 */
public class MetricalIndexDatabase<O extends DatabaseObject, D extends Distance<D>, N extends MetricalNode<N, E>, E extends MTreeEntry<D>> extends IndexDatabase<O> implements Parameterizable {
  /**
   * OptionID for {@link #INDEX_PARAM}
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("metricalindexdb.index", "Metrical index class to use.");

  /**
   * Parameter to specify the metrical index to use.
   * <p>
   * Key: {@code -metricalindexdb.index}
   * </p>
   */
  private final ObjectParameter<MetricalIndex<O, D, N, E>> INDEX_PARAM = new ObjectParameter<MetricalIndex<O, D, N, E>>(INDEX_ID, MetricalIndex.class);

  /**
   * The metrical index storing the data.
   */
  MetricalIndex<O, D, N, E> index;

  /**
   * Store own parameters, for partitioning.
   */
  private Collection<Pair<OptionID, Object>> params;
  
  /**
   * Constructor
   */
  public MetricalIndexDatabase(Parameterization config) {
    super();
    TrackParameters track = new TrackParameters(config);
    if(track.grab(INDEX_PARAM)) {
      index = INDEX_PARAM.instantiateClass(track);
      index.setDatabase(this);
    }
    params = track.getGivenParameters();
  }

  /**
   * Calls the super method and afterwards inserts the specified object into the
   * underlying index structure.
   */
  @Override
  public Integer insert(Pair<O, Associations> objectAndAssociations) throws UnableToComplyException {
    Integer id = super.insert(objectAndAssociations);
    O object = objectAndAssociations.getFirst();
    index.insert(object);
    return id;
  }

  /**
   * Calls the super method and afterwards inserts the specified objects into
   * the underlying index structure. If the option bulk load is enabled and the
   * index structure is empty, a bulk load will be performed. Otherwise the
   * objects will be inserted sequentially.
   */
  @Override
  public void insert(List<Pair<O, Associations>> objectsAndAssociationsList) throws UnableToComplyException {
    for(Pair<O, Associations> objectAndAssociations : objectsAndAssociationsList) {
      super.insert(objectAndAssociations);
    }
    this.index.insert(getObjects(objectsAndAssociationsList));
  }

  @SuppressWarnings("unchecked")
  public <T extends Distance<T>> List<DistanceResultPair<T>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, T> distanceFunction) {
    if(!distanceFunction.getClass().equals(index.getDistanceFunction().getClass())) {
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " + index.getDistanceFunction().getClass());
    }

    List<DistanceResultPair<D>> rangeQuery = index.rangeQuery(get(id), epsilon);

    List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
    for(DistanceResultPair<D> qr : rangeQuery) {
      result.add((DistanceResultPair<T>) qr);
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  public <T extends Distance<T>> List<DistanceResultPair<T>> rangeQuery(Integer id, T epsilon, DistanceFunction<O, T> distanceFunction) {
    if(!distanceFunction.getClass().equals(index.getDistanceFunction().getClass())) {
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " + index.getDistanceFunction().getClass());
    }

    List<DistanceResultPair<D>> rangeQuery = index.rangeQuery(get(id), (D) epsilon);

    List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
    for(DistanceResultPair<D> qr : rangeQuery) {
      result.add((DistanceResultPair<T>) qr);
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  public <T extends Distance<T>> List<DistanceResultPair<T>> kNNQueryForObject(O queryObject, int k, DistanceFunction<O, T> distanceFunction) {
    if(!distanceFunction.getClass().equals(index.getDistanceFunction().getClass())) {
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " + index.getDistanceFunction().getClass());
    }

    List<DistanceResultPair<D>> knnQuery = index.kNNQuery(queryObject, k);

    List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
    for(DistanceResultPair<D> qr : knnQuery) {
      result.add((DistanceResultPair<T>) qr);
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  public <T extends Distance<T>> List<DistanceResultPair<T>> kNNQueryForID(Integer id, int k, DistanceFunction<O, T> distanceFunction) {

    if(!distanceFunction.getClass().equals(index.getDistanceFunction().getClass())) {
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " + index.getDistanceFunction().getClass());
    }

    List<DistanceResultPair<D>> knnQuery = index.kNNQuery(get(id), k);

    List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
    for(DistanceResultPair<D> qr : knnQuery) {
      result.add((DistanceResultPair<T>) qr);
    }

    return result;
  }

  public <T extends Distance<T>> List<List<DistanceResultPair<T>>> bulkKNNQueryForID(@SuppressWarnings("unused") List<Integer> ids, @SuppressWarnings("unused") int k, @SuppressWarnings("unused") DistanceFunction<O, T> distanceFunction) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  @SuppressWarnings("unchecked")
  public <T extends Distance<T>> List<DistanceResultPair<T>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, T> distanceFunction) {
    if(!distanceFunction.getClass().equals(index.getDistanceFunction().getClass())) {
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " + index.getDistanceFunction().getClass() + ", but is " + distanceFunction.getClass());
    }

    List<DistanceResultPair<D>> rknnQuery = index.reverseKNNQuery(get(id), k);

    List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
    for(DistanceResultPair<D> qr : rknnQuery) {
      result.add((DistanceResultPair<T>) qr);
    }

    return result;
  }

  /**
   * Returns a string representation of this database.
   * 
   * @return a string representation of this database.
   */
  @Override
  public String toString() {
    return index.toString();
  }

  /**
   * Returns the index of this database.
   * 
   * @return the index of this database
   */
  @Override
  public MetricalIndex<O, D, N, E> getIndex() {
    return index;
  }

  /**
   * Returns a short description of the database. (Such as: efficiency in space
   * and time, index structure...)
   * 
   * @return a description of the database
   */
  @Override
  public String shortDescription() {
    StringBuffer description = new StringBuffer();
    description.append(this.getClass().getName());
    description.append(" holds all the data in a metrical index structure");
    description.append(" extending ").append(MetricalIndex.class.getName()).append(".\n");
    return description.toString();
  }

  @Override
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return new java.util.Vector<Pair<OptionID, Object>>(this.params);
  }
}