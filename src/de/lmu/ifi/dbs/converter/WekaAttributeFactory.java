package de.lmu.ifi.dbs.converter;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.HierarchicalClassLabel;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public final class WekaAttributeFactory// implements Parameterizable
{
    private Class classLabelClass = HierarchicalClassLabel.class;
    
    public WekaAttribute getAttribute(String value)
    {
        try
        {
            Double dvalue = Double.parseDouble(value);
            return new WekaNumericAttribute(dvalue);
        }
        catch(NumberFormatException e)
        {
            ClassLabel classLabel = Util.instantiate(ClassLabel.class,classLabelClass.getName());
            classLabel.init(value);
            return new WekaNominalAttribute(classLabel);
        }
    }
    
    public WekaAttribute getAttribute(String value, boolean string)
    {
        if(string)
        {
            return new WekaStringAttribute(value);
        }
        else
        {
            return getAttribute(value);
        }
    }
    
    
    
}
