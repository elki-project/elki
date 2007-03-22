package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.distancefunction.AbstractLocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.KernelBasedLocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.KernelFourCPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

/**
 * kernel 4C identifies local subgroups of data objects sharing a possible nonlinear correlation.
 * The algorithm is based on a combination of kernel PCA and density-based clustering (DBSCAN).
 *
 * @author Simon Paradies
 */
public class KernelFourC<O extends RealVector<O,?>> extends ProjectedDBSCAN<O, KernelFourCPreprocessor<? extends AbstractLocallyWeightedDistanceFunction<O,?>>> {

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("Kernel4C", "Computing Correlation Connected Clusters using Kernels",
                           "(work in progress, stay tuned...)",
                           "n/a");
  }


  @Override
  public Class preprocessorClass() {
    return KernelFourCPreprocessor.class;
  }


  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    // locally weighted distance function
    String[] locallyWeightedDistanceFunctionParameters = new String[args.length + 2];
    System.arraycopy(args, 0, locallyWeightedDistanceFunctionParameters, 2,
                     args.length);
    locallyWeightedDistanceFunctionParameters[0] = OptionHandler.OPTION_PREFIX + ProjectedDBSCAN.DISTANCE_FUNCTION_P;
    locallyWeightedDistanceFunctionParameters[1] = KernelBasedLocallyWeightedDistanceFunction.class.getName();

    String[] remainingParameters = super.setParameters(locallyWeightedDistanceFunctionParameters);

    setParameters(args, remainingParameters);

    return remainingParameters;
  }
}
