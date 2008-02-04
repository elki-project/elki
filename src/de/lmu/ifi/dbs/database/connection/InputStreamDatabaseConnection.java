package de.lmu.ifi.dbs.database.connection;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.parser.DistanceParser;
import de.lmu.ifi.dbs.parser.DistanceParsingResult;
import de.lmu.ifi.dbs.parser.Parser;
import de.lmu.ifi.dbs.parser.ParsingResult;
import de.lmu.ifi.dbs.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Provides a database connection expecting input from standard in.
 * 
 * @author Arthur Zimek 
 */
public class InputStreamDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<O> {

	/**
	 * Default parser.
	 */
	public final static String DEFAULT_PARSER = RealVectorLabelParser.class.getName();

	/**
	 * Label for parameter parser.
	 */
	public final static String PARSER_P = "parser";

	/**
	 * Description of parameter parser.
	 */
	public final static String PARSER_D = "a parser to provide a database "
			+ Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Parser.class) + ". Default: " + DEFAULT_PARSER;

	/**
	 * The parser.
	 */
	Parser<O> parser;

	/**
	 * The input to parse from.
	 */
	InputStream in = System.in;

	/**
	 * Provides a database connection expecting input from standard in.
	 */
	@SuppressWarnings("unchecked")
	public InputStreamDatabaseConnection() {

		ClassParameter parser = new ClassParameter(PARSER_P, PARSER_D, Parser.class);
		parser.setDefaultValue(DEFAULT_PARSER);
		optionHandler.put(PARSER_P, parser);
	}

	/**
	 * @see de.lmu.ifi.dbs.database.connection.DatabaseConnection#getDatabase(Normalization)
	 */
	@SuppressWarnings("unchecked")
	public Database<O> getDatabase(Normalization<O> normalization) {
		try {
			if (this.debug) {
				debugFine("*** parse");
			}

			// parse
			ParsingResult<O> parsingResult = parser.parse(in);
			// normalize objects and transform labels
			List<ObjectAndAssociations<O>> objectAndAssociationsList = normalizeAndTransformLabels(parsingResult.getObjectAndLabelList(),
					normalization);

			// add precomputed distances
			if (parser instanceof DistanceParser) {
				Map<Integer, Map<Integer, Distance>> distanceCache = ((DistanceParsingResult) parsingResult)
						.getDistanceCache();
				for (ObjectAndAssociations<O> objectAndAssociations : objectAndAssociationsList) {
					Map<Integer, Distance> distances = distanceCache.remove(objectAndAssociations.getObject().getID());
					objectAndAssociations.addAssociation(AssociationID.CACHED_DISTANCES, distances);
				}
			}

			if (this.debug) {
				debugFine("*** insert");
			}

			// insert into database
			database.insert(objectAndAssociationsList);

			return database;
		} catch (UnableToComplyException e) {
			throw new IllegalStateException(e);
		} catch (NonNumericFeaturesException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
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
		// for (PropertyDescription pd :
		// Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix()
		// + PROPERTY_PARSER)))
		// {
		// description.append("Class: ");
		// description.append(pd.getEntry());
		// description.append('\n');
		// description.append(pd.getDescription());
		// description.append('\n');
		// }
		// description.append('\n');
		// description.append("Databases available within this framework for
		// database connection ");
		// description.append(this.getClass().getName());
		// description.append(":");
		// description.append('\n');
		// for (PropertyDescription pd :
		// Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.getPropertyName(propertyPrefix()
		// + PROPERTY_DATABASE)))
		// {
		// description.append("Class: ");
		// description.append(pd.getEntry());
		// description.append('\n');
		// description.append(pd.getDescription());
		// description.append('\n');
		// }
		return description.toString();
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingOptions = super.setParameters(args);

		String parserClass = optionHandler.getOptionValue(PARSER_P);
		
		try {
			// noinspection unchecked
			parser = Util.instantiate(Parser.class, parserClass);
		} catch (UnableToComplyException e) {
			throw new WrongParameterValueException(PARSER_P, parserClass, PARSER_D);
		}

		remainingOptions = parser.setParameters(remainingOptions);
		setParameters(args, remainingOptions);
		return remainingOptions;
	}

	/**
	 * Returns the parameter setting of the attributes.
	 * 
	 * @return the parameter setting of the attributes
	 */
	public List<AttributeSettings> getAttributeSettings() {
		List<AttributeSettings> attributeSettings = super.getAttributeSettings();
		attributeSettings.addAll(parser.getAttributeSettings());
		return attributeSettings;
	}
}
