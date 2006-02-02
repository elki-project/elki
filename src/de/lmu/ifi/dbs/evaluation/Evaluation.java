package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.MetricalObject;

import java.io.PrintStream;

/**
 * An evaluation result.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Evaluation<M extends MetricalObject,A extends Algorithm<M>> extends Result<M>
{
    /**
     * Prints the evaluation to the designated print stream.
     * 
     * @param out the print stream where to print the evaluation
     */
    public void outputEvaluationResult(PrintStream out);
}
