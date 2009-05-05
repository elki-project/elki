/**
 * <p>Package collects parser for different file formats and data types.</p>
 * 
 * <p>The general use-case for any parser is to create {@link DatabaseObject}s out of an
 * {@link java.io.InputStream} (e.g. by reading a data file).
 * The {@link DatabaseObject}s are packed in a {@link ParsingResult}-Object which,
 * in turn, is used by a {@link DatabaseConnection}-Object to create a {@link Database}
 * containing the corresponding objects.</p>
 * <p>By default (i.e., if the user does not specify any specific requests), any {@link KDDTask} will
 * use the {@link FileBasedDatabaseConnection} which, in turn, will use the
 * {@link DoubleVectorLabelParser} to parse a specified data file creating
 * a {@link SequentialDatabase} containing {@link DoubleVector}-Objects.</p>
 * 
 * <p>Thus, the standard procedure to use a data set of a real-valued vector space is to prepare the data set
 * in a file of the following format (as suitable to {@link DoubleVectorLabelParser}):
 * <ul>
 *  <li>One point per line, attributes separated by whitespace.</li>
 *  <li>Several labels may be given per point. A label must not be parseable as double.</li>
 *  <li>Lines starting with &quot;#&quot; will be ignored.</li>
 *  <li>An index can be specified to identify an entry to be treated as class label.
 *      This index counts all entries (numeric and labels as well) starting with 0.</li>
 * </ul>
 * This file format is e.g. also suitable to gnuplot.
 * </p>
 * 
 * <p>As an example file following these requirements consider e.g.:
 * <a href="http://www.dbs.ifi.lmu.de/research/KDD/ELKI/datasets/example/exampledata.txt"><exampledata.txt</a>
 * </p>
 */
package de.lmu.ifi.dbs.elki.parser;