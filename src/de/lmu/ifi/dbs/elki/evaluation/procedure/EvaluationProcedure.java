package de.lmu.ifi.dbs.elki.evaluation.procedure;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.EvaluationResult;
import de.lmu.ifi.dbs.elki.evaluation.holdout.Holdout;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * An evaluation procedure evaluates a specified algorithm
 * based on a range of pairs of training and test sets.
 * However, test sets may remain empty for certain evaluation scenarios,
 * e.g. for clustering algorithms of some sort.
 * 
 * @author Arthur Zimek
 */
public interface EvaluationProcedure<O extends DatabaseObject, L extends ClassLabel, A extends Algorithm<O, Result>> extends Parameterizable
{
    /**
     * Message to indicate failure to call either {@link #setTrainingset(Database, Database) set(trainingset, testset)}
     * or have the user specify a holdout before calling {@link #evaluate(Algorithm) evaluate(algorithm)}.
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
     * Evaluates an algorithm.
     * 
     * @param test Test set to run on 
     * @param algorithm the algorithm to evaluate
     * @return the evaluation of the specified algorithm
     * based on the previously specified training and test sets
     * @throws IllegalStateException if a holdout is required to set
     * before calling this method
     */
    public EvaluationResult<O,A> evaluate(Database<O> test, A algorithm) throws IllegalStateException;
}
