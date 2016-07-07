package de.lmu.ifi.dbs.elki.algorithm.timeseries;

public class ChangePoint {

    double index, accuracy;
    String label;

    public ChangePoint(double index, double accuracy, String label){
        this.index = index;
        this.accuracy = accuracy;
        this.label = label;
    }

    public ChangePoint(double index, double accuracy){
        this.index = index;
        this.accuracy = accuracy;
        this.label = null;
    }

    public StringBuilder appendTo(StringBuilder buf) {
        if(label == null) {
            return buf.append("[").append(index).append(", ").append(accuracy).append("]");
        } else {
            return buf.append(label).append(": [").append(index).append(", ").append(accuracy).append("]");
        }
    }
}
