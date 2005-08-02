package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.parser.Parser;
import de.lmu.ifi.dbs.parser.StandardLabelParser;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

/**
 * Provides a file based database connection based on the parser to be set.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FileBasedDatabaseConnection implements DatabaseConnection {
  static {
    String PROPERTIES_FILE = DEFAULT_PACKAGE.replace('.', File.separatorChar) + File.separatorChar + "database.prp";
    try {
      PROPERTIES.load(ClassLoader.getSystemResourceAsStream(PROPERTIES_FILE));
    }
    catch (Exception e) {
      System.err.println("Warning: unable to load properties file " + PROPERTIES_FILE + ".");
    }
  }

  /**
   * Default parser.
   */
  public final static Parser DEFAULT_PARSER = new StandardLabelParser();

  /**
   * Label for parameter parser.
   */
  public final static String PARSER_P = "parser";

  /**
   * Description of parameter parser.
   */
  public final static String PARSER_D = "<classname>a parser to provide a database (default: " + DEFAULT_PARSER.getClass().getName() + ")";

  /**
   * Marker for input from STDIN.
   */
  public final static String STDIN_MARKER = "STDIN";

  /**
   * Label for parameter input.
   */
  public final static String INPUT_P = "in";

  /**
   * Description for parameter input.
   */
  public final static String INPUT_D = "<filename>input file to be parsed or \"" + STDIN_MARKER + "\" for standard input (piping).";

  /**
   * The parser.
   */
  private Parser parser;

  /**
   * The input to parse from.
   */
  private InputStream in;

  /**
   * OptionHandler to handle options.
   */
  private OptionHandler optionHandler;

  /**
   * Provides a file based database connection based on the parser to be set.
   */
  public FileBasedDatabaseConnection() {
    Map<String, String> parameterToDescription = new Hashtable<String, String>();
    parameterToDescription.put(PARSER_P + OptionHandler.EXPECTS_VALUE, PARSER_D);
    parameterToDescription.put(INPUT_P + OptionHandler.EXPECTS_VALUE, INPUT_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
    try {
      parser = DEFAULT_PARSER.getClass().newInstance();
    }
    catch (InstantiationException e) {
      e.printStackTrace();
    }
    catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  /**
   * @see de.lmu.ifi.dbs.database.DatabaseConnection#getDatabase()
   */
  public Database getDatabase() {
    return parser.parse(in);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("", false));
    description.append('\n');
    description.append("Parsers available within this framework:");
    description.append('\n');
    String parsers = PROPERTIES.getProperty(PROPERTY_PARSER);
    String[] parserNames = (parsers == null ? new String[0] : PROPERTY_SEPARATOR.split(parsers));
    for (int i = 0; i < parserNames.length; i++) {
      try {
        String desc = ((Parser) Class.forName(parserNames[i]).newInstance()).description();
        description.append(parserNames[i]);
        description.append('\n');
        description.append(desc);
        description.append('\n');
      }
      catch (InstantiationException e) {
        System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
      }
      catch (IllegalAccessException e) {
        System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
      }
      catch (ClassNotFoundException e) {
        System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
      }
      catch (ClassCastException e) {
        System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
      }
    }

    description.append('\n');
    description.append("Databases available within this framework:");
    description.append('\n');
    String databases = PROPERTIES.getProperty(PROPERTY_DATABASE);
    String[] databaseNames = (databases == null ? new String[0] : PROPERTY_SEPARATOR.split(databases));
    for (int i = 0; i < databaseNames.length; i++) {
      try {
        String desc = ((Database) Class.forName(databaseNames[i]).newInstance()).description();
        description.append(databaseNames[i]);
        description.append('\n');
        description.append(desc);
        description.append('\n');
      }
      catch (InstantiationException e) {
        System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
      }
      catch (IllegalAccessException e) {
        System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
      }
      catch (ClassNotFoundException e) {
        System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
      }
      catch (ClassCastException e) {
        System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
      }
    }

    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingOptions = optionHandler.grabOptions(args);
    if (optionHandler.isSet(PARSER_P)) {
      try {
        parser = (Parser) Class.forName(optionHandler.getOptionValue(PARSER_P)).newInstance();
      }
      catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
    }
    try {
      String input = optionHandler.getOptionValue(INPUT_P);
      if (input.equals(STDIN_MARKER)) {
        in = System.in;
      }
      else {
        in = new FileInputStream(input);
      }
    }
    catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
    return parser.setParameters(remainingOptions);
  }

}
