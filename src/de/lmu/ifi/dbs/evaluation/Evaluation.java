package de.lmu.ifi.dbs.evaluation;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.data.MetricalObject;

import java.io.PrintStream;

/**
 * An evaluation result.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Evaluation<M extends MetricalObject,A extends Algorithm<M>>
{
    public void output(PrintStream out);
}
