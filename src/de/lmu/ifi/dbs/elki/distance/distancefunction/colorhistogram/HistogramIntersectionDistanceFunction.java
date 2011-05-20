package de.lmu.ifi.dbs.elki.distance.distancefunction.colorhistogram;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Intersection distance for color histograms.
 * 
 * According to: M. J. Swain, D. H. Ballard:<br />
 * Color indexing<br />
 * International Journal of Computer Vision, 7(1), 32, 1991
 * 
 * @author Erich Schubert
 */
@Title("Color histogram intersection distance")
@Description("Distance function for color histograms that emphasizes 'strong' bins.")
@Reference(authors = "M. J. Swain, D. H. Ballard", title = "Color Indexing", booktitle = "International Journal of Computer Vision, 7(1), 32, 1991")
public class HistogramIntersectionDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?,?>, DoubleDistance> {
  /**
   * Static instance
   */
  public static final HistogramIntersectionDistanceFunction STATIC = new HistogramIntersectionDistanceFunction();
  
  /**
   * Constructor. No parameters.
   * 
   * @deprecated Use static instance
   */
  @Deprecated
  public HistogramIntersectionDistanceFunction() {
    super();
  }

  @Override
  public DoubleDistance distance(NumberVector<?,?> v1, NumberVector<?,?> v2) {
    final int dim1 = v1.getDimensionality();
    if(dim1 != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors" + "\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString() + "\n" + v1.getDimensionality() + "!=" + v2.getDimensionality());
    }
    double dist = 0;
    double norm1 = 0;
    double norm2 = 0;
    for(int i = 1; i <= dim1; i++) {
      final double val1 = v1.doubleValue(i);
      final double val2 = v2.doubleValue(i);
      dist += Math.min(val1, val2);
      norm1 += val1;
      norm2 += val2;
    }
    dist = 1 - dist / Math.min(norm1, norm2);
    return new DoubleDistance(dist);
  }

  @Override
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public String toString() {
    return "HistogramIntersectionDistance";
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected HistogramIntersectionDistanceFunction makeInstance() {
      return HistogramIntersectionDistanceFunction.STATIC;
    }
  }
}