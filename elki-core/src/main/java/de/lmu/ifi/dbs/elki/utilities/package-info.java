/**
 * <b>Utility and helper classes</b> - commonly used data structures, output
 * formatting, exceptions, ...
 * <p>
 * Specialized utility classes (which often collect static utility methods only)
 * can be found in other places of ELKI as well, as seen below.
 * <p>
 * Important utility function collections:
 * <ul>
 * <li>Basic and low-level:
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.Util}: Miscellaneous utility
 * functions.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.logging.LoggingUtil}: simple logging
 * access.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.math.MathUtil}: Mathematics utility
 * functions.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.data.VectorUtil}: Vector and Matrix
 * functions.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil}: Spatial MBR
 * computations (intersection, union etc.).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil}: byte array
 * processing (low-level IO via byte arrays).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.io.FileUtil}: File and file name
 * utility functions.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil}: Generic classes
 * (instantiation, arrays of arrays, sets that require safe but unchecked
 * casts).</li>
 * </ul>
 * </li>
 * <li>Database-related:
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.data.type.TypeUtil}: Data type utility
 * functions and common type definitions.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.QueryUtil}: Database Query API
 * simplifications.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil}: Database ID DBID
 * handling.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil}: Data
 * storage layer (like Maps).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.DatabaseUtil}: database utility
 * functions (centroid etc.).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.result.ResultUtil}: result processing
 * functions (e.g. extracting sub-results).</li>
 * </ul>
 * </li>
 * <li>Output-related:
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.io.FormatUtil}: output
 * formatting.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil}: SVG generation
 * (XML DOM based).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.visualization.batikutil.BatikUtil}: Apache
 * Batik SVG utilities (coordinate transforms screen to canvas).</li>
 * </ul>
 * </li>
 * <li>Specialized:
 * <ul>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil}: Managing
 * parameter settings</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry}: class and
 * classpath inspection.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeUtil}:
 * reporting page file accesses.</li>
 * </ul>
 * </li>
 * </ul>
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
package de.lmu.ifi.dbs.elki.utilities;
