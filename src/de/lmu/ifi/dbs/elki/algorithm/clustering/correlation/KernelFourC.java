package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.KernelBasedLocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.KernelFourCPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Kernel 4C identifies local subgroups of data objects sharing a possible nonlinear correlation.
 * The algorithm is based on a combination of kernel PCA and density-based clustering (DBSCAN).
 *
 * @author Simon Paradies
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class KernelFourC<V extends RealVector<V, ?>> extends ProjectedDBSCAN<V> {

    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("Kernel4C", "Computing Correlation Connected Clusters using Kernels",
            "(work in progress, stay tuned...)",
            "n/a");
    }

    /**
     * @see ProjectedDBSCAN#preprocessorClass()
     */
    public Class<KernelFourCPreprocessor> preprocessorClass() {
        return KernelFourCPreprocessor.class;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        // locally weighted distance function
        String[] locallyWeightedDistanceFunctionParameters = new String[args.length];
        System.arraycopy(args, 0, locallyWeightedDistanceFunctionParameters, 0, args.length);
        Util.addParameter(locallyWeightedDistanceFunctionParameters,
            DISTANCEFUNCTION_ID,
            KernelBasedLocallyWeightedDistanceFunction.class.getName());

        return super.setParameters(locallyWeightedDistanceFunctionParameters);
    }
}
