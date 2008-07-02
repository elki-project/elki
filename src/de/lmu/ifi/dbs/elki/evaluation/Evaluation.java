package de.lmu.ifi.dbs.elki.evaluation;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;

import java.io.PrintStream;

/**
 * An evaluation result.
 * 
 * @author Arthur Zimek 
 */
public interface Evaluation<O extends DatabaseObject, A extends Algorithm<O>>
        extends Result<O>
{
    /**
     * Prints the evaluation to the designated print stream.
     * 
     * @param out
     *            the print stream where to print the evaluation
     */
    public void outputEvaluationResult(PrintStream out);
}
