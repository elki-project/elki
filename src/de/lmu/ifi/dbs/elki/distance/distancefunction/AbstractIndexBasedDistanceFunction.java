package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.database.query.distance.AbstractDatabaseDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for distance functions needing a database index.
 * 
 * @author Elke Achtert
 * 
 * @param <O> the type of object to compute the distances in between
 * @param <I> the type of Index used
 * @param <D> the type of Distance used
 */
public abstract class AbstractIndexBasedDistanceFunction<O, I extends Index, D extends Distance<D>> extends AbstractDatabaseDistanceFunction<O, D> implements IndexBasedDistanceFunction<O, D> {
  /**
   * Parameter to specify the preprocessor to be used.
   * <p>
   * Key: {@code -distancefunction.preprocessor}
   * </p>
   */
  protected IndexFactory<O, I> indexFactory;

  /**
   * Constructor.
   * 
   * @param indexFactory Index factory
   */
  public AbstractIndexBasedDistanceFunction(IndexFactory<O, I> indexFactory) {
    super();
    this.indexFactory = indexFactory;
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   */
  abstract public static class Instance<O, I extends Index, D extends Distance<D>, F extends DistanceFunction<? super O, D>> extends AbstractDatabaseDistanceQuery<O, D> implements IndexBasedDistanceFunction.Instance<O, I, D> {
    /**
     * Index we use
     */
    protected final I index;

    /**
     * Our parent distance function
     */
    protected F parent;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param index Index to use
     * @param parent Parent distance function
     */
    public Instance(Relation<O> database, I index, F parent) {
      super(database);
      this.index = index;
      this.parent = parent;
    }

    @Override
    public I getIndex() {
      return index;
    }

    @Override
    public F getDistanceFunction() {
      return parent;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <F> Factory type
   */
  public static abstract class Parameterizer<F extends IndexFactory<?, ?>> extends AbstractParameterizer {
    /**
     * The index factory we use.
     */
    protected F factory;

    /**
     * Index factory parameter
     * 
     * @param config Parameterization
     * @param restriction Restriction class
     * @param defaultClass Default value
     */
    public void configIndexFactory(Parameterization config, Class<?> restriction, Class<?> defaultClass) {
      ObjectParameter<F> param = new ObjectParameter<F>(INDEX_ID, restriction, defaultClass);
      if(config.grab(param)) {
        factory = param.instantiateClass(config);
      }
    }
  }
}