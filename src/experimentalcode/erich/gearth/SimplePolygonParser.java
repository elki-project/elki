package experimentalcode.erich.gearth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.parser.AbstractParser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Parser to load polygon data.
 * 
 * @author Erich Schubert
 */
public class SimplePolygonParser extends AbstractParser<PolygonsObject> {
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
  public ParsingResult<PolygonsObject> parse(InputStream in) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;
    List<Pair<PolygonsObject, List<String>>> objectAndLabelsList = new ArrayList<Pair<PolygonsObject, List<String>>>();
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        if(!line.startsWith(COMMENT) && line.length() > 0) {
          Pair<PolygonsObject, List<String>> objectAndLabels = parseLine(line);
          objectAndLabelsList.add(objectAndLabels);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    return new ParsingResult<PolygonsObject>(objectAndLabelsList, PolygonsObject.PROTOTYPE);
  }

  /**
   * Parse a single line.
   * 
   * @param line Line to parse
   * 
   * @return Parsed polygon
   */
  private Pair<PolygonsObject, List<String>> parseLine(String line) {
    List<String> entries = tokenize(line);
    Iterator<String> iter = entries.iterator();

    List<String> labels = new ArrayList<String>();
    List<Polygon> polys = new java.util.Vector<Polygon>(1);

    List<double[]> coords = new ArrayList<double[]>();
    while(iter.hasNext()) {
      String cur = iter.next();
      Matcher m = COORD.matcher(cur);
      if(m.find()) {
        try {
          double c1 = Double.valueOf(m.group(1));
          double c2 = Double.valueOf(m.group(2));
          if (m.group(3) != null) {
            double c3 = Double.valueOf(m.group(3));
            coords.add(new double[] { c1, c2, c3 });
          }
          else {
            coords.add(new double[] { c1, c2 });
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
          coords = new ArrayList<double[]>();
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
    return new Pair<PolygonsObject, List<String>>(new PolygonsObject(polys), labels);
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
  public static class Parameterizer extends AbstractParser.Parameterizer<PolygonsObject> {
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