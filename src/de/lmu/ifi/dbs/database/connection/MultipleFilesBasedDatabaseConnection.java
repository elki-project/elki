package de.lmu.ifi.dbs.database.connection;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.parser.*;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyDescription;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.IDPair;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.AssociationID;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Provides a database connection based on multiple files and parsers to be set.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MultipleFilesBasedDatabaseConnection<M extends MetricalObject, O extends MultiRepresentedObject<M>> extends AbstractDatabaseConnection<O> {
  /**
   * Default parser.
   */
  public final static String DEFAULT_PARSER = DoubleVectorLabelParser.class.getName();

  /**
   * Label for parameter parser.
   */
  public final static String PARSER_P = "parser";

  /**
   * Description of parameter parser.
   */
  public final static String PARSER_D = "<classname_1;...;classname_n>a list of parsers to provide a database (default: " + DEFAULT_PARSER + ")";

  /**
   * Label for parameter input.
   */
  public final static String INPUT_P = "in";

  /**
   * Description for parameter input.
   */
  public final static String INPUT_D = "<filename_1;...;filename_n>a list of input files to be parsed.";

  /**
   * The parsers.
   */
  Parser<M>[] parsers;

  /**
   * The input to parse from.
   */
  FileInputStream[] in;

  /**
   * Provides a database connection expecting input from several files.
   */
  @SuppressWarnings("unchecked")
  public MultipleFilesBasedDatabaseConnection() {
    parameterToDescription.put(PARSER_P + OptionHandler.EXPECTS_VALUE, PARSER_D);
    parameterToDescription.put(INPUT_P + OptionHandler.EXPECTS_VALUE, INPUT_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see DatabaseConnection#getDatabase(de.lmu.ifi.dbs.normalization.Normalization)
   */
  @SuppressWarnings("unchecked")
  public Database<O> getDatabase(Normalization<O> normalization) {
    try {
      // parse
      ParsingResult<M> parsingResult = parsers.parse(in);
      // normalize objects
      List<M> objects = normalization != null ?
                        normalization.normalize(parsingResult.getObjects()) :
                        parsingResult.getObjects();
      // transform labels
      List<Map<AssociationID, Object>> labels = transform(parsingResult.getLabels());

      // insert into database
      database.insert(objects, labels);

      // precomputed distances
      if (parsers instanceof DistanceParser) {
        Map<IDPair, Distance> distanceMap = ((DistanceParsingResult<M,Distance>) parsingResult).getDistanceMap();
        DistanceFunction<M, Distance> distanceFunction = ((DistanceParser<M, Distance>) parsers).getDistanceFunction();
        database.addDistancesToCache(distanceMap, (Class<DistanceFunction<M,Distance>>) distanceFunction.getClass());
      }

      return database;
    }
    catch (UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
    catch (NonNumericFeaturesException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Transforms the specified labelList into a map of association id an association object
   * suitable for inserting objects into the database
   *
   * @param labelList the list to be transformes
   * @return a map of association id an association object
   */
  private List<Map<AssociationID, Object>> transform(List<String> labelList) {
    List<Map<AssociationID, Object>> result = new ArrayList<Map<AssociationID, Object>>();

    for (String label : labelList) {
      Map<AssociationID, Object> associationMap = new Hashtable<AssociationID, Object>();

      Object association;
      if (classLabel == null) {
        association = label;
      }
      else {
        try {
          association = Class.forName(classLabel).newInstance();
          ((ClassLabel) association).init(label);
        }
        catch (InstantiationException e) {
          throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e) {
          throw new IllegalStateException(e);
        }
        catch (ClassNotFoundException e) {
          throw new IllegalStateException(e);
        }
      }
      associationMap.put(associationID, association);
      result.add(associationMap);
    }
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("", false));
    description.append('\n');
    description.append("Parsers available within this framework for database connection ");
    description.append(this.getClass().getName());
    description.append(":");
    description.append('\n');
    for (PropertyDescription pd : Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix() + PROPERTY_PARSER)))
    {
      description.append("Class: ");
      description.append(pd.getEntry());
      description.append('\n');
      description.append(pd.getDescription());
      description.append('\n');
    }
    description.append('\n');
    description.append("Databases available within this framework for database connection ");
    description.append(this.getClass().getName());
    description.append(":");
    description.append('\n');
    for (PropertyDescription pd : Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix() + PROPERTY_DATABASE)))
    {
      description.append("Class: ");
      description.append(pd.getEntry());
      description.append('\n');
      description.append(pd.getDescription());
      description.append('\n');
    }
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @SuppressWarnings("unchecked")
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingOptions = super.setParameters(args);

    String parserClasses = optionHandler.getOptionValue(PARSER_P);
    String inputFiles = optionHandler.getOptionValue(INPUT_P);

    if (optionHandler.isSet(PARSER_P)) {
      parsers = Util.instantiate(Parser.class, optionHandler.getOptionValue(PARSER_P));
    }
    return parsers.setParameters(remainingOptions);
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings attributeSettings = result.get(0);
    for (int i = 0; i < parsers.length; i++) {
      attributeSettings.addSetting(PARSER_P + "_" + (i+1), parsers[i].getClass().toString());
      attributeSettings.addSetting(INPUT_P + "_" + (i+1), in[i].toString());
    }

    return result;
  }

  /**
   * Returns the prefix for properties concerning InputStreamDatabaseConnection.
   * Extending classes requiring other properties should overwrite this method
   * to provide another prefix.
   */
  protected String propertyPrefix() {
    return PREFIX;
  }

}
