package de.lmu.ifi.dbs.elki.data.projection;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.SubsetArrayAdapter;

/**
 * Projection class for number vectors.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <F> Feature type
 */
public class FeatureSelection<V extends FeatureVector<V, F>, F> extends AbstractFeatureSelection<V, F> {
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
  public FeatureSelection(int[] dims, V factory) {
    super(new SubsetArrayAdapter<F, V>(getAdapter(factory), dims));
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
  @SuppressWarnings("unchecked")
  private static <V extends FeatureVector<V, F>, F> ArrayAdapter<F, ? super V> getAdapter(V factory) {
    if(factory instanceof NumberVector) {
      ArrayAdapter<?, ?> ret = ArrayLikeUtil.numberVectorAdapter((NumberVector<?, ?>) factory);
      return (ArrayAdapter<F, ? super V>) ret;
    }
    return ArrayLikeUtil.featureVectorAdapter(factory);
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