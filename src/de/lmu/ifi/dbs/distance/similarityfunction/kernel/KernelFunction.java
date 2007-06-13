package de.lmu.ifi.dbs.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.similarityfunction.SimilarityFunction;

/**
 * Interface Kernel describes the requirements of any kernel function.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface KernelFunction<O extends DatabaseObject, D extends Distance<D>> extends SimilarityFunction<O, D>, DistanceFunction<O, D> {
}
