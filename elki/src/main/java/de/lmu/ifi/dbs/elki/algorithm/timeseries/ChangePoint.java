package de.lmu.ifi.dbs.elki.algorithm.timeseries;

public class ChangePoint {

    double index, accuracy;

    public ChangePoint(double index, double accuracy){
        this.index = index;
        this.accuracy = accuracy;
    }

    public StringBuilder appendTo(StringBuilder buf) {
        // TO DO with label????
        return buf.append("[").append(index).append(", ").append(accuracy).append("]");
    }
}
