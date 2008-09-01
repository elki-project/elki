package de.lmu.ifi.dbs.elki.evaluation.procedure;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluation;
import de.lmu.ifi.dbs.elki.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * An evaluation procedure evaluates a specified algorithm
 * based on a range of pairs of training and test sets.
 * However, test sets may remain empty for certain evaluation scenarios,
 * e.g. for clustering algorithms of some sort.
 * 
 * @author Arthur Zimek
 */
public interface EvaluationProcedure<O extends DatabaseObject,A extends Algorithm<O>,L extends ClassLabel> extends Parameterizable
{
    /**
     * Message to indicate failure to call either {@link #set(Database, Database) set(trainingset, testset)}
     * or {@link #set(Database, Holdout) set(database,holdout)} before calling
     * {@link #evaluate(Algorithm) evaluate(algorithm)}.
     */
    public static final String ILLEGAL_STATE = "EvaluationProcedure has not been properly prepared to perform an evaluation.";
    
    /**
     * Set whether runtime is assessed during evaluation.
     * 
     * 
     * @param time whether or not to assess runtime during evaluation
     */
    public void setTime(boolean time);
    
    /**
     * Set whether to give verbose messages
     * during evaluation.
     * 
     * 
     * @param verbose whether or not to print verbose messages during evaluation
     */
    public void setVerbose(boolean verbose);
    
    /**
     * Sets the specified training and test set.
     * 
     * 
     * @param training the database to train an algorithm
     * @param test the database to test an algorithm
     */
    public void set(Database<O> training, Database<O> test);
     
    /**
     * The given database can be splitted as specified
     * by a certain holdout procedure. The partitions are set
     * as training and test sets for the evaluation procedure.
     * 
     * 
     * @param data the database to prepare holdouts from
     * @param holdout the holdout procedure
     */
    public void set(Database<O> data, Holdout<O,L> holdout);
     
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
    public Evaluation<O,A> evaluate(A algorithm) throws IllegalStateException;
    
    /**
     * Provides a description of the used holdout.
     * 
     * 
     * @return a description of the used holdout
     */
    public String setting();
}
