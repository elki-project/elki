package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;

/**
 * Interface Kernel describes the requirements of any kernel function.
 *
 * @author Elke Achtert 
 * @param <O> object type
 * @param <D> distance type
 */
public interface KernelFunction<O extends DatabaseObject, D extends Distance<D>> extends SimilarityFunction<O, D>, DistanceFunction<O, D> {
	//TODO any methods?
}
