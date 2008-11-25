package de.lmu.ifi.dbs.elki.database.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.parser.ObjectAndLabels;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

/**
 * Provides a database connection based on multiple files and parsers to be set.
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be provided by the implementing class as element of the supplied database
 */
public class MultipleFileBasedDatabaseConnection<O extends DatabaseObject>
    extends AbstractDatabaseConnection<MultiRepresentedObject<O>> {

    /**
     * OptionID for {@link #PARSERS_PARAM}
     */
    public static final OptionID PARSERS_ID = OptionID.getOrCreateOptionID(
        "multipledbc.parsers",
        "A comma separated list of classnames specifying the parsers to provide a database " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Parser.class) +
            ". If this parameter is not set, " +
            RealVectorLabelParser.class.getName() + " is used as parser for all input files."
    );

    /**
     * Optional parameter to specify the parsers to provide a database,
     * must extend {@link Parser}. If this parameter is not set,
     * {@link RealVectorLabelParser} is used as parser for all input files.
     * <p>Key: {@code -multipledbc.parsers} </p>
     */
    private final ClassListParameter<Parser<O>> PARSERS_PARAM = new ClassListParameter<Parser<O>>(
        PARSERS_ID, Parser.class, true);

    /**
     * Holds the instances of the parsers specified by {@link #PARSERS_PARAM}.
     */
    private List<Parser<O>> parsers;

    /**
     * OptionID for {@link #INPUT_PARAM}
     */
    public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID(
        "multipledbc.in",
        "A comma separated list of the names of the input files to be parsed."
    );

    /**
     * Parameter that specifies the names of the input files to be parsed.
     * <p>Key: {@code -multipledbc.in} </p>
     */
    private final FileListParameter INPUT_PARAM =
        new FileListParameter(INPUT_ID,
            FileListParameter.FilesType.INPUT_FILES);

    /**
     * The input files to parse from as specified by {@link #INPUT_PARAM}.
     */
    private List<FileInputStream> inputStreams;

    /**
     * Provides a database connection expecting input from several files,
     * adding parameters
     * {@link #PARSERS_PARAM}, and {@link #INPUT_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public MultipleFileBasedDatabaseConnection() {
        super();
        forceExternalID = true;
        // parameter parser
        addOption(PARSERS_PARAM);

        // parameter file list
        addOption(INPUT_PARAM);
    }

    public Database<MultiRepresentedObject<O>> getDatabase(
        Normalization<MultiRepresentedObject<O>> normalization) {
        try {
            // number of representations
            final int numberOfRepresentations = inputStreams.size();

            // comparator to sort the ObjectAndLabels lists provided by the
            // parsers.
            Comparator<ObjectAndLabels<O>> comparator = new Comparator<ObjectAndLabels<O>>() {
                public int compare(ObjectAndLabels<O> o1, ObjectAndLabels<O> o2) {
                    String classLabel1 = o1.getLabels().get(classLabelIndex);
                    String classLabel2 = o2.getLabels().get(classLabelIndex);
                    return classLabel1.compareTo(classLabel2);
                }
            };

            // parse
            List<ParsingResult<O>> parsingResults = new ArrayList<ParsingResult<O>>(
                numberOfRepresentations);
            int numberOfObjects = 0;
            for (int r = 0; r < numberOfRepresentations; r++) {
                ParsingResult<O> parsingResult = parsers.get(r).parse(inputStreams.get(r));
                parsingResults.add(parsingResult);
                numberOfObjects = Math.max(parsingResult.getObjectAndLabelList().size(), numberOfObjects);
                // sort the representations according to the external ids
                List<ObjectAndLabels<O>> objectAndLabelsList = parsingResult.getObjectAndLabelList();
                Collections.sort(objectAndLabelsList, comparator);
            }

            // build the multi-represented objects and their labels
            List<ObjectAndLabels<MultiRepresentedObject<O>>> objectAndLabelsList = new ArrayList<ObjectAndLabels<MultiRepresentedObject<O>>>();
            for (int i = 0; i < numberOfObjects; i++) {
                List<O> representations = new ArrayList<O>(numberOfRepresentations);
                List<String> labels = new ArrayList<String>();
                for (int r = 0; r < numberOfRepresentations; r++) {
                    ParsingResult<O> parsingResult = parsingResults.get(r);
                    representations.add(parsingResult.getObjectAndLabelList().get(i).getObject());
                    List<String> rep_labels = parsingResult.getObjectAndLabelList().get(i).getLabels();
                    for (String l : rep_labels) {
                        if (!labels.contains(l))
                            labels.add(l);
                    }
                }
                objectAndLabelsList.add(new ObjectAndLabels<MultiRepresentedObject<O>>(
                    new MultiRepresentedObject<O>(representations), labels));
            }

            // normalize objects and transform labels
            List<SimplePair<MultiRepresentedObject<O>,Associations>> objectAndAssociationList = normalizeAndTransformLabels(objectAndLabelsList, normalization);

            // insert into database
            database.insert(objectAndAssociationList);

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
     * Calls the super method
     * and instantiates {@link #inputStreams} according to the value of parameter
     * {@link #INPUT_PARAM}
     * and {@link #parsers} according to the value of parameter {@link #PARSERS_PARAM} .
     * The remaining parameters are passed to all instances of {@link #parsers}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // input files
        List<File> inputFiles = INPUT_PARAM.getValue();
        inputStreams = new ArrayList<FileInputStream>(inputFiles.size());
        for (File inputFile : inputFiles) {
            try {
                inputStreams.add(new FileInputStream(inputFile));
            }
            catch (FileNotFoundException e) {
                throw new WrongParameterValueException(INPUT_PARAM, inputFiles.toString(), e);
            }
        }

        // parsers
        if (optionHandler.isSet(PARSERS_PARAM)) {
            parsers = PARSERS_PARAM.instantiateClasses();

            if (parsers.size() != inputStreams.size()) {
                throw new WrongParameterValueException("Number of parsers and input files does not match ("
                    + parsers.size() + " != "
                    + inputStreams.size() + ")!");
            }
        }
        else {
            this.parsers = new ArrayList<Parser<O>>(inputStreams.size());
            for (int i = 0; i < inputStreams.size(); i++) {
                try {
                    Parser<O> parser = Util.instantiateGenerics(Parser.class, RealVectorLabelParser.class.getName());
                    this.parsers.add(i, parser);
                }
                catch (UnableToComplyException e) {
                    throw new RuntimeException("This should never happen!");
                }
            }
        }

        // set parameters of parsers
        for (Parser<O> parser : this.parsers) {
            remainingParameters = parser.setParameters(remainingParameters);
        }
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * all instances of {@link #parsers}.
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        for (Parser<O> parser : parsers) {
            attributeSettings.addAll(parser.getAttributeSettings());
        }
        return attributeSettings;
    }
}
