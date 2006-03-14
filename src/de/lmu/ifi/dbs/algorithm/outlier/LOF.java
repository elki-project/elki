package de.lmu.ifi.dbs.algorithm.outlier;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.LOFResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.utilities.*;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * LOF provides the algorithm to compute the LOF.
 *
 * @author Peer Kr&ouml;ger (<a
 *         href="mailto:kroegerp@dbs.ifi.lmu.de">kroegerp@dbs.ifi.lmu.de</a>)
 */
public class LOF<O extends DatabaseObject> extends DistanceBasedAlgorithm<O,DoubleDistance> {

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>minpts";

  /**
   * Minimum points.
   */
  protected int minpts;

  /**
   * Keeps the LOFs for each objectID.
   */
  protected IDDoublePair[] resultArray;
  
  /**
   * Provides the result of the algorithm.
   */
  protected LOFResult<O> result;

  /**
   * Sets minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since LOF is a non-abstract
   * class, finally optionHandler is initialized.
   */
  public LOF() {
    super();
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(Database)
   */
  protected void runInTime(Database<O> database) throws IllegalStateException {
    if (isVerbose()) {
      System.out.println();
    }
    try {
    	Progress progress = new Progress(database.size());
    	getDistanceFunction().setDatabase(database, isVerbose());
    	if (isVerbose()) {
    		System.out.println("\n ##### Computing LOFs:");
    	}

    	// compute neighbors of each db object
    	if (isVerbose()) {
    		System.out.println("\n Step 1: computing neighborhoods:");
    	}
    	{
    		int counter = 1;
    		for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
    			Integer id = iter.next();
	    		computeNeighbors(database, id);
	    		if (isVerbose()) {
	    			progress.setProcessed(counter);
	    			System.out.print(Util.status(progress));
	    		}
	    	}
	    	if (isVerbose()) {
	    		System.out.println();
	    	}
    	}

    	// compute local reachability density of each db object
    	if (isVerbose()) {
    		System.out.println("\n Step 2: computing LRDs:");
    	}
    	{
    		int counter = 1;
    		for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
    			Integer id = iter.next();
	    		computeLRD(database, id);
	    		if (isVerbose()) {
	    			progress.setProcessed(counter);
	    			System.out.print(Util.status(progress));
	    		}
	    	}
	    	if (isVerbose()) {
	    		System.out.println();
	    	}
    	}

    	// compute LOF of each db object
    	if (isVerbose()) {
    		System.out.println("\n Step 3: finally, computing LOFs:");
    	}
    	resultArray = new IDDoublePair[database.size()];
    	{
    		int counter = 0;
    		for (Iterator<Integer> iter = database.iterator(); iter.hasNext(); counter++) {
    			Integer id = iter.next();
	    		resultArray[counter] = new IDDoublePair(id,computeLOF(database, id).doubleValue());
	    		if (isVerbose()) {
	    			progress.setProcessed(counter+1);
	    			System.out.print(Util.status(progress));
	    		}
	    	}
	    	if (isVerbose()) {
	    		System.out.println();
	    	}
    	}

    	
    	
    	result = new LOFResult<O>(database,resultArray);
    }
    catch (Exception e) {
    	throw new IllegalStateException(e);
    }

  }

  /**
   * Computes the k-nearest neighbors of a given object in a given database.
   * @param database  the database on which the LRD is computed
   * @param objectID  the object
   */
  public void computeNeighbors(Database<O> database, Integer objectID) {
	  List<QueryResult <DoubleDistance>> neighbors = database.kNNQueryForID(objectID, minpts+1, getDistanceFunction());
	  neighbors.remove(0);
	  database.associate(AssociationID.NEIGHBORS,objectID,neighbors);
  }
  
  /**
   * Computes the local reachability density (LRD) of a given object.
   * 
   * @param database  the database on which the LRD is computed
   * @param objectID  the object
   */
  public void computeLRD(Database<O> database, Integer objectID) {
	  List<QueryResult <DoubleDistance>> neighbors = (List<QueryResult <DoubleDistance>>) database.getAssociation(AssociationID.NEIGHBORS, objectID);

	  double sum = 0;
	  for (QueryResult <DoubleDistance> o : neighbors) {
	      Integer oID = o.getID();
	      double oDist = o.getDistance().getDoubleValue();
	      List<QueryResult <DoubleDistance>> neighborsO = (List<QueryResult <DoubleDistance>>) database.getAssociation(AssociationID.NEIGHBORS, oID);
	      double oCoreDist = neighborsO.get(neighborsO.size()-1).getDistance().getDoubleValue();
	      double oReachDist = Math.max(oCoreDist, oDist);
	      sum = sum + oReachDist;
	  }
	  
	  double lrd = neighbors.size() / sum;
	  database.associate(AssociationID.LRD,objectID,new Double(lrd));
  }
  
  /**
   * computes the LOF value for a given object
   *
   * @param database      the database on which the algorithm is run
   * @param objectID      object the LOF of which is computed
   */
  protected Double computeLOF(Database<O> database, Integer objectID) {
	  List<QueryResult <DoubleDistance>> neighbors = (List<QueryResult <DoubleDistance>>) database.getAssociation(AssociationID.NEIGHBORS, objectID);
	  Double pLRD = (Double) database.getAssociation(AssociationID.LRD,objectID);
	  
	  double sum = 0;
	  for (QueryResult <DoubleDistance> o : neighbors) {
	      Integer oID = o.getID();
	      Double oLRD = (Double) database.getAssociation(AssociationID.LRD,oID);
	      sum = sum + oLRD.doubleValue() / pLRD.doubleValue();
	  }
	  sum = sum / neighbors.size();
	  return new Double(sum);
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("LOF", "Local Outlier Factor", "Algorithm to compute density-based local outlier factors in a database based on the parameters " + "minimumPoints. " + "These two parameters determine a density threshold for clustering.", "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander: " + " LOF: Identifying Density-Based Local Outliers. " + "In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), " + "Dallas, TX, 2000.");
  }

  /**
   * Sets the parameters minpts additionally to the parameters set
   * by the super-class method.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
	  String[] remainingParameters = super.setParameters(args);

    // minpts
    String minptsString = optionHandler.getOptionValue(MINPTS_P);
    try {
      minpts = Integer.parseInt(minptsString);
      if (minpts <= 0)
        throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D);
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D, e);
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
   */
  public Result<O> getResult() {
    return result;
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
   List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(MINPTS_P, Integer.toString(minpts));

    return attributeSettings;
  }

}
