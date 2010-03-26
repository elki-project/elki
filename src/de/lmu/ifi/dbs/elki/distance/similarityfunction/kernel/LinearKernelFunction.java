package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

/**
 * Provides a linear Kernel function that computes a similarity between the two
 * feature vectors V1 and V2 defined by V1^T*V2.
 * 
 * @author Simon Paradies
 * @param <O> vector type
 */
public class LinearKernelFunction<O extends NumberVector<O, ?>> extends AbstractKernelFunction<O, DoubleDistance> {
  /**
   * Provides a linear Kernel function that computes a similarity between the
   * two vectors V1 and V2 defined by V1^T*V2.
   */
  public LinearKernelFunction() {
    super(new DoubleDistance());
  }

  /**
   * Provides a linear Kernel function that computes a similarity between the
   * two feature vectors V1 and V2 definded by V1^T*V2
   * 
   * @param o1 first feature vector
   * @param o2 second feature vector
   * @return the linear kernel similarity between the given two vectors as an
   *         instance of {@link DoubleDistance DoubleDistance}.
   */
  @Override
  public DoubleDistance similarity(final O o1, final O o2) {
    if(o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of Feature-Vectors" + "\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
    }
    double sim = 0;
    for(int i = 1; i <= o1.getDimensionality(); i++) {
      sim += o1.doubleValue(i) * o2.doubleValue(i);
    }
    return new DoubleDistance(sim);
  }

  @Override
  public DoubleDistance distance(final O fv1, final O fv2) {
    return new DoubleDistance(Math.sqrt(similarity(fv1, fv1).doubleValue() + similarity(fv2, fv2).doubleValue() - 2 * similarity(fv1, fv2).doubleValue()));
  }
}