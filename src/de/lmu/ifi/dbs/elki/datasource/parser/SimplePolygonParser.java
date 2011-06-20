package de.lmu.ifi.dbs.elki.datasource.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.data.spatial.PolygonsObject;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Parser to load polygon data (2D and 3D only) from a simple format. One record
 * per line, points separated by whitespace, numbers separated by colons.
 * Multiple polygons components can be separated using
 * {@link #POLYGON_SEPARATOR}.
 * 
 * Unparseable parts will be treated as labels.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has PolygonsObject
 */
public class SimplePolygonParser extends AbstractParser implements Parser {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(SimplePolygonParser.class);

  /**
   * Pattern to catch coordinates
   */
  public static final Pattern COORD = Pattern.compile("^(" + NUMBER_PATTERN + "),\\s*(" + NUMBER_PATTERN + ")(?:,\\s*(" + NUMBER_PATTERN + "))?$");

  /**
   * Polygon separator
   */
  public static final String POLYGON_SEPARATOR = "--";

  /**
   * Constructor.
   * 
   * @param colSep
   * @param quoteChar
   */
  public SimplePolygonParser(Pattern colSep, char quoteChar) {
    super(colSep, quoteChar);
  }

  @Override
  public MultipleObjectsBundle parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;

    List<PolygonsObject> polys = new ArrayList<PolygonsObject>();
    List<LabelList> labels = new ArrayList<LabelList>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          Pair<PolygonsObject, LabelList> objectAndLabels = parseLine(line);
          polys.add(objectAndLabels.first);
          labels.add(objectAndLabels.second);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return MultipleObjectsBundle.makeSimple(TypeUtil.POLYGON_TYPE, polys, TypeUtil.LABELLIST, labels);
  }

  /**
   * Parse a single line.
   * 
   * @param line Line to parse
   * 
   * @return Parsed polygon
   */
  private Pair<PolygonsObject, LabelList> parseLine(String line) {
    List<String> entries = tokenize(line);
    Iterator<String> iter = entries.iterator();

    LabelList labels = new LabelList();
    List<Polygon> polys = new java.util.Vector<Polygon>(1);

    List<Vector> coords = new ArrayList<Vector>();
    while(iter.hasNext()) {
      String cur = iter.next();
      Matcher m = COORD.matcher(cur);
      if(m.find()) {
        try {
          double c1 = Double.valueOf(m.group(1));
          double c2 = Double.valueOf(m.group(2));
          if(m.group(3) != null) {
            double c3 = Double.valueOf(m.group(3));
            coords.add(new Vector(new double[] { c1, c2, c3 }));
          }
          else {
            coords.add(new Vector(new double[] { c1, c2 }));
          }
          continue;
        }
        catch(NumberFormatException e) {
          logger.warning("Looked like a coordinate pair but didn't parse: " + cur);
        }
      }
      // Polygon separator.
      if(cur.equals(POLYGON_SEPARATOR)) {
        if(coords.size() > 0) {
          polys.add(new Polygon(coords));
          coords = new ArrayList<Vector>();
        }
        continue;
      }
      // Label
      labels.add(cur);
    }
    // Complete polygon
    if(coords.size() > 0) {
      polys.add(new Polygon(coords));
    }
    return new Pair<PolygonsObject, LabelList>(new PolygonsObject(polys), labels);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParser.Parameterizer {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected SimplePolygonParser makeInstance() {
      return new SimplePolygonParser(colSep, quoteChar);
    }
  }
}