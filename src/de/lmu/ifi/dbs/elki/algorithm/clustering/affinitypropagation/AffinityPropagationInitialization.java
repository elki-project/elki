package de.lmu.ifi.dbs.elki.algorithm.clustering.affinitypropagation;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Initialization methods for affinity propagation.
 * 
 * @author Erich Schubert
 */
public interface AffinityPropagationInitialization<O> extends Parameterizable {
  /**
   * Quantile to use for the diagonal entries.
   */
  public static final OptionID QUANTILE_ID = new OptionID("ap.quantile", "Quantile to use for diagonal entries.");

  /**
   * Compute the initial similarity matrix.
   * 
   * @param db Database
   * @param relation Data relation
   * @param ids indexed DBIDs
   * @return Similarity matrix
   */
  double[][] getSimilarityMatrix(Database db, Relation<O> relation, ArrayDBIDs ids);

  /**
   * Get the data type information for the similarity computations.
   * 
   * @return Data type
   */
  TypeInformation getInputTypeRestriction();
}