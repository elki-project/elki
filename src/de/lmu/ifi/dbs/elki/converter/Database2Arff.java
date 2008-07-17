package de.lmu.ifi.dbs.elki.converter;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.Util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Arthur Zimek
 */
//  TODO arthur comment all
public class Database2Arff<D extends DatabaseObject & WekaObject<W>, W extends WekaAttribute<W>> {

    private static final String SEPARATOR = ",";

    public void convertToArff(Database<D> database, OutputStream stream) {
        PrintStream out = new PrintStream(stream);
        convertToArff(database, out);
        out.close();
    }

    public void convertToArff(Database<D> database, PrintStream out) {
        Map<Integer, Set<W>> nominalValues = new HashMap<Integer, Set<W>>();
        BitSet stringAttributes = new BitSet();
        final int NOT_INITIALIZED = -1;
        int dim = NOT_INITIALIZED;
        for (Iterator<Integer> dbIterator = database.iterator(); dbIterator
            .hasNext();) {
            Integer id = dbIterator.next();
            D databaseObject = database.get(id);
            W[] attributes = databaseObject.getAttributes();
            if (dim == NOT_INITIALIZED) {
                for (int i = 0; i < attributes.length; i++) {
                    if (attributes[i].isNominal()) {
                        nominalValues.put(i, new TreeSet<W>());
                    }
                    stringAttributes.set(i, attributes[i].isString());
                }
                dim = attributes.length;
            }
            else if (dim != attributes.length) {
                throw new IllegalArgumentException(
                    "DatabaseObject with id "
                        + id
                        + " differs in its dimensionality from previous objects.");
            }
            for (Integer i : nominalValues.keySet()) {
                nominalValues.get(i).add(attributes[i]);
            }
        }
        out.print("@relation \"");
        out.print(new Date().toString());
        out.println("\"");
        out.println();
        for (int d = 0; d < dim; d++) {
            out.print("@attribute d");
            out.print(Integer.toString(d + 1));
            out.print(" ");
            if (stringAttributes.get(d)) {
                out.print(WekaAttribute.STRING);
            }
            else if (nominalValues.containsKey(d)) {
                out.print("{");
                Util.print(new ArrayList<W>(nominalValues.get(d)),
                    SEPARATOR, out);
                out.print("}");
            }
            else {
                out.print(WekaAttribute.NUMERIC);
            }
            out.println();
        }
        boolean printLabel = database.isSetForAllObjects(AssociationID.LABEL);
        if (printLabel) {
            out.print("@attribute label ");
            out.print(WekaAttribute.STRING);
            out.println();
        }
        boolean printClass = database.isSetForAllObjects(AssociationID.CLASS);
        if (printClass) {
            out.print("@attribute class ");
            out.print("{");
            Util.print(
                new ArrayList<ClassLabel<?>>(Util.getClassLabels(database)),
                SEPARATOR, out);
            out.print("}");
            out.println();
        }
        out.println();
        out.println("@data");
        for (Iterator<Integer> dbIterator = database.iterator(); dbIterator.hasNext();) {
            Integer id = dbIterator.next();
            D d = database.get(id);
            W[] attributes = d.getAttributes();
            Util.print(Arrays.asList(attributes), SEPARATOR, out);
            if (printLabel) {
                out.print(SEPARATOR);
                out.print("\"");
                out.print(database.getAssociation(AssociationID.LABEL, id));
                out.print("\"");
            }
            if (printClass) {
                out.print(SEPARATOR);
                out.print(database.getAssociation(AssociationID.CLASS, id));
            }
            out.println();
        }
        out.flush();
    }
}
