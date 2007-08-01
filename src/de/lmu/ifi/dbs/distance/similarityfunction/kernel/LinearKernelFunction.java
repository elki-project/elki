package de.lmu.ifi.dbs.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;

/**
 * Provides a linear Kernel function that computes
   * a similarity between the two feature vectors V1 and V2 definded by V1^T*V2.
 *
 * @author Simon Paradies
 */
public class LinearKernelFunction<O extends FeatureVector<O, ? >> extends AbstractDoubleKernelFunction<O> {

  /**
   * Provides a linear Kernel function that computes
   * a similarity between the two vectors V1 and V2 definded by V1^T*V2.
   */
  public LinearKernelFunction() {
    super();
  }

  /**
   * Provides a linear Kernel function that computes
   * a similarity between the two feature vectors V1 and V2 definded by V1^T*V2
   *
   * @param o1 first feature vector
   * @param o2  second feature vector
   * @return the linear kernel similarity between the given two vectors as an
   *         instance of {@link DoubleDistance DoubleDistance}.
   * @see DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public DoubleDistance similarity(final O o1, final O o2) {
    if (o1.getDimensionality() != o2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of Feature-Vectors" +
                                         "\n  first argument: " + o1.toString() +
                                         "\n  second argument: " + o2.toString());
    }
    double sim = 0;
    for (int i = 1; i <= o1.getDimensionality(); i++) {
      sim += o1.getValue(i).doubleValue() * o2.getValue(i).doubleValue();
    }
    return new DoubleDistance(sim);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  @Override
  public String description() {
    StringBuffer description = new StringBuffer();
		description.append(super.description());
		description.append("Linear Kernel for feature vectors. No parameters required.");
		return description.toString();
  }
}
