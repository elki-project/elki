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
 */
package de.lmu.ifi.dbs.elki.parser;