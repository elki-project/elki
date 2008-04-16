package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.database.Database;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CLIQUE<O extends DatabaseObject> extends AbstractAlgorithm<O> {

  /**
	 * Parameter xsi.
	 */
	public static final String XSI_P = "xsi";

  /**
	 * Description for parameter xsi.
	 */
	public static final String XSI_D = "number of intervals (units) in each dimension";

  /**
	 * Parameter tau.
	 */
	public static final String TAU_P = "tau";

  /**
	 * Description for parameter tau.
	 */
	public static final String TAU_D = "threshold for the selectivity of a unit, where the selectivity is" +
                                     "the fraction of total data points contained in this unit";

  /**
	 * Flag for prune.
	 */
	public static final String PRUNE_F = "prune";

  /**
	 * Parameter for mindim.
	 */
	public static final String MINDIM_P = "mindim";

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   * // todo
   */
  public Result<O> getResult() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  public Description getDescription() {
    return new Description("CLIQUE",
                           "Automatic Subspace Clustering of High Dimensional Data for Data Mining Applications",
                           "Grid-based algorithm to find density-connected sets in a database based on the parameters ",
                           "R. Agrawal, J. Gehrke, D. Gunopulos, P. Raghavan: " +
                           "Automatic Subspace Clustering of High Dimensional Data for Data Mining Applications. " +
                           "In Proc. SIGMOD Conference, Seattle, WA, 1998.");
  }

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized
   *                               properly (e.g. the setParameters(String[]) method has been failed
   *                               to be called).
   * // todo
   */
  protected void runInTime(Database<O> database) throws IllegalStateException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
