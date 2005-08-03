package de.lmu.ifi.dbs.database;

import java.io.FileInputStream;

import de.lmu.ifi.dbs.parser.Parser;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

/**
 * Provides a file based database connection based on the parser to be set.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FileBasedDatabaseConnection extends InputStreamDatabaseConnection
{

    /**
     * Label for parameter input.
     */
    public final static String INPUT_P = "in";

    /**
     * Description for parameter input.
     */
    public final static String INPUT_D = "<filename>input file to be parsed.";


    /**
     * Provides a file based database connection based on the parser to be set.
     */
    public FileBasedDatabaseConnection()
    {
        super();
        parameterToDescription.put(INPUT_P + OptionHandler.EXPECTS_VALUE, INPUT_D);
        optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
        
    }


    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingOptions = super.setParameters(args);
        if(optionHandler.isSet(PARSER_P))
        {
            try
            {
                parser = (Parser) Class.forName(optionHandler.getOptionValue(PARSER_P)).newInstance();
            }
            catch(Exception e)
            {
                throw new IllegalArgumentException(e);
            }
        }
        try
        {
            String input = optionHandler.getOptionValue(INPUT_P);
            in = new FileInputStream(input);
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException(e);
        }
        return parser.setParameters(remainingOptions);
    }

}
