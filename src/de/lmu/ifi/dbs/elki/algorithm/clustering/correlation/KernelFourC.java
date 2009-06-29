package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.KernelBasedLocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.KernelFourCPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Kernel 4C identifies local subgroups of data objects sharing a possible
 * nonlinear correlation. The algorithm is based on a combination of kernel PCA
 * and density-based clustering (DBSCAN).
 * 
 * @author Simon Paradies
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class KernelFourC<V extends RealVector<V, ?>> extends ProjectedDBSCAN<V> {

  public Description getDescription() {
    return new Description("Kernel4C", "Computing Correlation Connected Clusters using Kernels", "(work in progress, stay tuned...)", "n/a");
  }

  @Override
  public Class<?> preprocessorClass() {
    return KernelFourCPreprocessor.class;
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    // todo richtig?
    // locally weighted distance function
    ArrayList<String> locallyWeightedDistanceFunctionParameters = new ArrayList<String>(args);
    OptionUtil.addParameter(locallyWeightedDistanceFunctionParameters, DISTANCE_FUNCTION_ID, KernelBasedLocallyWeightedDistanceFunction.class.getName());

    return super.setParameters(locallyWeightedDistanceFunctionParameters);
  }
}
