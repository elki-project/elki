package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;

/**
 * An evaluation procedure evaluates a specified algorithm
 * based on a range of pairs of training and test sets.
 * However, test sets may remain empty for certain evaluation scenarios,
 * e.g. for clustering algorithms of some sort.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface EvaluationProcedure<M extends MetricalObject,A extends Algorithm<M>>
{
    public void setTime(boolean time);
    
    public void setVerbose(boolean verbose);
    
    /**
     * Sets the specified training and test set.
     * 
     * 
     * @param training the database to train an algorithm
     * @param test the database to test an algorithm
     */
    public void set(Database<M> training, Database<M> test);
     
    /**
     * The given database can be splitted as specified
     * by a certain holdout procedure. The partitions are set
     * as training and test sets for the evaluation procedure.
     * 
     * 
     * @param data the database to prepare holdouts from
     * @param holdout the holdout procedure
     */
    public void set(Database<M> data, Holdout<M> holdout);
     
    /**
     * Evaluates an algorithm.
     * 
     * 
     * @param algorithm the algorithm to evaluate
     * @return the evaluation of the specified algorithm
     * based on the previously specified training and test sets
     * @throws IllegalStateException if a holdout is required to set
     * before calling this method
     */
    public Evaluation<M,A> evaluate(A algorithm) throws IllegalStateException;
    
    /**
     * Provides a description of the used holdout.
     * 
     * 
     * @return a description of the used holdout
     */
    public String setting();
}
