package de.lmu.ifi.dbs.properties;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides management of properties.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public final class Properties
{
    /**
     * The pattern to split for separate entries in a property string, which is
     * a &quot;,&quot;.
     */
    public static final Pattern PROPERTY_SEPARATOR = Pattern.compile(",");

    /**
     * The Properties for the KDDFramework.
     */
    public static final Properties KDD_FRAMEWORK_PROPERTIES = new Properties(Properties.class.getPackage().getName().replace('.', File.separatorChar) + File.separatorChar + "KDDFramework.prp");

    /**
     * Stores the properties as defined by a property-file.
     */
    private final java.util.Properties PROPERTIES;

    /**
     * Provides the properties as defined in the designated file.
     * 
     * @param filename name of a file to provide property-definitions.
     */
    private Properties(String filename)
    {
        this.PROPERTIES = new java.util.Properties();
        try
        {
            PROPERTIES.load(ClassLoader.getSystemResourceAsStream(filename));
        }
        catch(Exception e)
        {
            System.err.println("Warning: unable to load properties file " + filename + ".");
        }
    }

    /**
     * Provides the entries (separated by {@link #PROPERTY_SEPARATOR PROPERTY_SEPARATOR})
     * for a specified PropertyName.
     * 
     * @param propertyName the PropertyName of the property to retrieve
     * @return the entries (separated by {@link #PROPERTY_SEPARATOR PROPERTY_SEPARATOR})
     * for the specified PropertyName
     */
    public String[] getProperty(PropertyName propertyName)
    {
        String property = propertyName == null ? null : PROPERTIES.getProperty(propertyName.getName());
        return property == null ? new String[0] : PROPERTY_SEPARATOR.split(property);
    }

    /**
     * Returns an array of PropertyDescription for all entries
     * for the given PropertyName.
     * 
     * @param propertyName the Propertyname of the property to retrieve
     * @return PropertyDescriptins for all entries of the given PropertyName
     */
    public PropertyDescription[] getProperties(PropertyName propertyName)
    {
        String[] entries = getProperty(propertyName);
        List<PropertyDescription> result = new ArrayList<PropertyDescription>();
        for(String entry : entries)
        {
            try
            {
                String desc = "";
                Object propertyInstance = propertyName.getClass().cast(Class.forName(entry).newInstance());
                if(propertyInstance instanceof Algorithm)
                {
                    // TODO: description -- check whether this provides the desired result
                    desc = ((Algorithm) propertyInstance).getDescription().toString();
                }
                else if(propertyInstance instanceof Parameterizable)
                {
                    desc = ((Parameterizable) propertyInstance).description();
                }
                result.add(new PropertyDescription(entry,desc));
            }
            catch(InstantiationException e)
            {
                System.err.println("Invalid classname \"" + entry + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getClass().getName() + "\" in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Invalid classname \"" + entry + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getClass().getName() + "\" in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassNotFoundException e)
            {
                System.err.println("Invalid classname \"" + entry + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getClass().getName() + "\" in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
        }
        PropertyDescription[] propertyDescription = new PropertyDescription[result.size()];
        result.toArray(propertyDescription);
        return propertyDescription;
        
    }

}
