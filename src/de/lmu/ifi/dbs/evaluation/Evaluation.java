package de.lmu.ifi.dbs.evaluation;

import java.io.PrintStream;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DatabaseObject;

/**
 * An evaluation result.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
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
