/**
 * Parsers for different file formats and data types
 * <p>
 * The general use-case for any parser is to create objects out of an
 * {@link java.io.InputStream} (e.g. by reading a data file).
 * The objects are packed in a
 * {@link de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle} which,
 * in turn, is used by a
 * {@link de.lmu.ifi.dbs.elki.datasource.DatabaseConnection}-Object
 * to fill a {@link de.lmu.ifi.dbs.elki.database.Database}
 * containing the corresponding objects.
 * <p>
 * By default (i.e., if the user does not specify any specific requests),
 * any {@link de.lmu.ifi.dbs.elki.KDDTask} will
 * use the {@link de.lmu.ifi.dbs.elki.database.StaticArrayDatabase} which,
 * in turn, will use a
 * {@link de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection}
 * and a {@link de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser}
 * to parse a specified data file creating
 * a {@link de.lmu.ifi.dbs.elki.database.StaticArrayDatabase}
 * containing {@link de.lmu.ifi.dbs.elki.data.DoubleVector}-Objects.
 * <p>
 * Thus, the standard procedure to use a data set of a real-valued vector space
 * is to prepare the data set in a file of the following format
 * (as suitable to
 * {@link de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser}):
 * <ul>
 * <li>One point per line, attributes separated by whitespace.</li>
 * <li>Several labels may be given per point. A label must not be parseable as
 * double.</li>
 * <li>Lines starting with &quot;#&quot; will be ignored.</li>
 * <li>An index can be specified to identify an entry to be treated as class
 * label.
 * This index counts all entries (numeric and labels as well) starting with
 * 0.</li>
 * <li>Files can be gzip compressed.</li>
 * </ul>
 * This file format is e.g. also suitable to gnuplot.
 *
 * @opt hide java.io.*
 * @opt hide de.lmu.ifi.dbs.elki.utilities.*
 */
/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.datasource.parser;
