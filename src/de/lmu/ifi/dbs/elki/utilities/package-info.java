/**
 * <p><b>Utility and helper classes</b> - commonly used data structures, output formatting, exceptions, ...</p>
 * 
 * <p>Specialized utility classes (which often collect static utility methods only) can be found
 * in other places of ELKI as well, as seen below.</p>
 * 
 * <p>Important utility function collections:</p>
 * <ul>
 * <li>Basic and low-level:<ul>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.Util}: Miscellaneous utility functions.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.pairs.PairUtil}: for Pair Comparators.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.logging.LoggingUtil}: simple logging access.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.math.MathUtil}: Mathematics utility functions.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.data.VectorUtil}: Vector and Matrix functions.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil}: Spatial MBR computations (intersection, union etc.).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil}: byte array processing (low-level IO via byte arrays).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.FileUtil}: File and file name utility functions.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil}: Generic classes (instantiation, arrays of arrays, sets that require safe but unchecked casts).</li>
 * </ul></li>
 * <li>Database-related:<ul>
 * <li>{@link de.lmu.ifi.dbs.elki.data.type.TypeUtil}: Data type utility functions and common type definitions.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.QueryUtil}: Database Query API simplifications.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil}: Database ID DBID handling.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil}: Data storage layer (like Maps).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.DatabaseUtil}: database utility functions (centroid etc.).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.distance.DistanceUtil}: distance functions related (min, max for {@link de.lmu.ifi.dbs.elki.distance.distancevalue.Distance}s).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.result.ResultUtil}: result processing functions (e.g. extracting sub-results).</li>
 * </ul></li>
 * <li>Output-related:<ul>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.FormatUtil}: output formatting.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.xml.HTMLUtil}: HTML (with XML DOM) generation.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil}: SVG generation (XML DOM based).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.visualization.batikutil.BatikUtil}: Apache Batik SVG utilities (coordinate transforms screen to canvas).</li>
 * <li>{@link de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil}: Visualizer handling.</li>
 * </ul></li>
 * <li>Specialized:<ul>
 * <li>{@link de.lmu.ifi.dbs.elki.data.images.ImageUtil}: image handling.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil}: Converting iterators and iterables.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil}: Managing parameter settings</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.InspectionUtil}: class and classpath inspection.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.utilities.documentation.DocumentationUtil}: documentation extraction from annotations.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.persistent.PageFileUtil}: reporting page file accesses.</li>
 * <li>{@link de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeUtil}: reporting page file accesses.</li>
 * </ul></li>
 * </ul>
 */
package de.lmu.ifi.dbs.elki.utilities;