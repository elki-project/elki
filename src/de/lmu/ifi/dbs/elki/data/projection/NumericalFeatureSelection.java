package de.lmu.ifi.dbs.elki.data.projection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.SubsetArrayAdapter;

/**
 * Projection class for number vectors.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <N> Number type
 */
public class NumericalFeatureSelection<V extends NumberVector<V, N>, N extends Number> extends AbstractFeatureSelection<V, N> {
  /**
   * Minimum dimensionality required for projection
   */
  private int mindim;

  /**
   * Object factory
   */
  private V factory;

  /**
   * Output dimensionality
   */
  private int dimensionality;

  /**
   * Constructor.
   * 
   * @param dims Dimensions
   * @param factory Object factory
   */
  public NumericalFeatureSelection(int[] dims, V factory) {
    super(new SubsetArrayAdapter<N, V>(getAdapter(factory), dims));
    this.factory = factory;
    this.dimensionality = dims.length;

    int mindim = 0;
    for(int dim : dims) {
      mindim = Math.max(mindim, dim + 1);
    }
    this.mindim = mindim;
  }

  /**
   * Choose the best adapter for this.
   * 
   * @param factory Object factory, for type inference
   * @return Adapter
   */
  private static <V extends NumberVector<V, N>, N extends Number> NumberArrayAdapter<N, ? super V> getAdapter(V factory) {
    return ArrayLikeUtil.numberVectorAdapter(factory);
  }

  @SuppressWarnings("unchecked")
  @Override
  public V project(V data) {
    return factory.newNumberVector(data, (NumberArrayAdapter<N, ? super V>) adapter);
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    @SuppressWarnings("unchecked")
    final Class<V> cls = (Class<V>) factory.getClass();
    return new VectorTypeInformation<V>(cls, dimensionality, dimensionality);
  }

  @Override
  public TypeInformation getInputDataTypeInformation() {
    @SuppressWarnings("unchecked")
    final Class<V> cls = (Class<V>) factory.getClass();
    return new VectorTypeInformation<V>(cls, mindim, Integer.MAX_VALUE);
  }
}