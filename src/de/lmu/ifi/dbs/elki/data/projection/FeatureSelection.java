package de.lmu.ifi.dbs.elki.data.projection;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.datastructures.FeatureVectorAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.SubsetArrayAdapter;

/**
 * Projection class for number vectors.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 * @param <F> Feature type
 */
public class FeatureSelection<V extends FeatureVector<V, F>, F> extends AbstractFeatureSelectionProjection<V, F> {
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
   * @param dim Dimensions
   * @param factory Object factory
   */
  public FeatureSelection(int[] dims, V factory) {
    super(new SubsetArrayAdapter<F, V>(FeatureVectorAdapter.getStatic((V) null), dims));
    this.factory = factory;
    this.dimensionality = dims.length;

    int mindim = 0;
    for(int dim : dims) {
      mindim = Math.max(mindim, dim + 1);
    }
    this.mindim = mindim;
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