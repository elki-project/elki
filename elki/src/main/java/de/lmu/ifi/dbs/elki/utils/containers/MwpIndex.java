package de.lmu.ifi.dbs.elki.utils.containers;

/**
 * Structure to hold return values in index creation for McdeMwpDependenceMeausre
 */

public class MwpIndex extends RankStruct{
    public double adjusted;
    public double correction;

    public MwpIndex(int index, double adjusted, double correction){
        super(index);
        this.adjusted = adjusted;
        this.correction = correction;
    }

    public MwpIndex(){};
}
