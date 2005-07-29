package de.lmu.ifi.dbs.algorithm;

import java.io.File;

/**
 * Specifies the requirements to an printable result of some algorithm.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Result
{
    /**
     * Writes the clustering result to the given file.
     * Clustering result implementations, which are likely to
     * provide several clusters are supposed to use the filename
     * as prefix for every file to create and to append a proper suffix.
     * In case of occuring IOExceptions the output is expected
     * to be given at the standard-out. Therefore this behaviour
     * should be also achievable by giving a null-Object as parameter.
     * 
     * @param out file, which designates the location to write the results,
     * or which's name designates the prefix of any locations to write the results,
     * or which could remain null to designate the standard-out as location for output. 
     */
    public void output(File out);
}
