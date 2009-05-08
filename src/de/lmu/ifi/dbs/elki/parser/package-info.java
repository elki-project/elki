/**
 * <p>Parsers for different file formats and data types.</p>
 * 
 * <p>The general use-case for any parser is to create
 * {@link de.lmu.ifi.dbs.elki.data.DatabaseObject}s out of an
 * {@link java.io.InputStream} (e.g. by reading a data file).
 * The {@link de.lmu.ifi.dbs.elki.data.DatabaseObject}s are packed in a
 * {@link de.lmu.ifi.dbs.elki.parser.ParsingResult}-Object which,
 * in turn, is used by a {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection}-Object
 * to create a {@link de.lmu.ifi.dbs.elki.database.Database}
 * containing the corresponding objects.</p>
 * <p>By default (i.e., if the user does not specify any specific requests),
 * any {@link de.lmu.ifi.dbs.elki.KDDTask} will
 * use the {@link de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection} which,
 * in turn, will use the
 * {@link de.lmu.ifi.dbs.elki.parser.DoubleVectorLabelParser}
 * to parse a specified data file creating
 * a {@link de.lmu.ifi.dbs.elki.database.SequentialDatabase}
 * containing {@link de.lmu.ifi.dbs.elki.data.DoubleVector}-Objects.</p>
 * 
 * <p>Thus, the standard procedure to use a data set of a real-valued vector space
 * is to prepare the data set in a file of the following format
 * (as suitable to {@link de.lmu.ifi.dbs.elki.parser.DoubleVectorLabelParser}):
 * <ul>
 *  <li>One point per line, attributes separated by whitespace.</li>
 *  <li>Several labels may be given per point. A label must not be parseable as double.</li>
 *  <li>Lines starting with &quot;#&quot; will be ignored.</li>
 *  <li>An index can be specified to identify an entry to be treated as class label.
 *      This index counts all entries (numeric and labels as well) starting with 0.</li>
 *  <li>Files can be gzip compressed.</li>
 * </ul>
 * This file format is e.g. also suitable to gnuplot.
 * </p>
 * 
 * <p>As an example file following these requirements consider e.g.:
 * <a href="http://www.dbs.ifi.lmu.de/research/KDD/ELKI/datasets/example/exampledata.txt">exampledata.txt</a>
 * </p>
 */
package de.lmu.ifi.dbs.elki.parser;