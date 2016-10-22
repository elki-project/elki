package de.lmu.ifi.dbs.elki.algorithm.timeseries;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.result.ChangePointDetectionResult;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import java.util.*;

/**
 * Off-line multiple change point detection algorithm
 *
 * @author Sebastian Rühl
 */

@Title("Off-line Change Point Detection: Algorithm for detecting change point in time series")
@Description("Detects multiple change points in a time series")
@Alias("de.lmu.ifi.dbs.elki.algorithm.OfflineChangePointDetection")
public class OfflineChangePointDetectionAlgorithm extends AbstractAlgorithm<ChangePointDetectionResult> {

    private int confidence, bootstrapSteps;

    /**
     * Constructor
     *
     * @param confidence Confidence
     * @param bootstrapSteps Steps for bootstrapping
     */
    public OfflineChangePointDetectionAlgorithm(int confidence, int bootstrapSteps) {
        this.confidence = confidence;
        this.bootstrapSteps = bootstrapSteps;
    }

    /**
     * Executes multiple change point detection for every given time series
     *
     * @param relation the relation to process
     * @param labellist label for the distinct time series
     * @return list with all the detected change point for every time series
     */
    public ChangePointDetectionResult run(Relation<DoubleVector> relation, Relation<LabelList> labellist) {

        List<ChangePoints> result = new ArrayList<>();

        for(DBIDIter realtion_iter = relation.getDBIDs().iter(); realtion_iter.valid(); realtion_iter.advance()) {

            result.add(new ChangePoints(
                    multipleChangepointsWithConfidence(relation.get(realtion_iter).getValues())));
        }

        return new ChangePointDetectionResult("Change Point List", "changepoints", result, labellist);
    }

    /**
     * Performs multiple change point detection for a given time series
     * Overloaded method, this method initializes the method and then calls other version with set variables
     *
     * @param values time series
     * @return list of change point for given time series
     */
    private List<ChangePoint> multipleChangepointsWithConfidence(double[] values){
        List<ChangePoint> result = new ArrayList<>();
        result = multipleChangepointsWithConfidence(result, values, 0);
        return result;
    }

    /**
     * Actually performs multiple change point detection for a given time series
     * This method uses a kind of divide and conquer approach
     *
     * @param result list containing detected change point
     * @param values current time series
     * @param tmpArraryStartIndex current position in the time series
     * @return list of change point for given time series
     */

    private List<ChangePoint> multipleChangepointsWithConfidence(List<ChangePoint> result, double[] values, int tmpArraryStartIndex){
        double tmpConf = confidenceLevel(values, bootstrapSteps);
        int tmpMaxPos = tmpArraryStartIndex + getMaximumIndex(likelihoodRatioChangeInMean(values)); // return the detected changepoint

        if(!(tmpConf < confidence || values.length <=3 || (tmpMaxPos - tmpArraryStartIndex + 1 == values.length))){ // cannot split up arrays of size 3, that would make every element a change point
            multipleChangepointsWithConfidence(result
                    , Arrays.copyOfRange(values, 0, tmpMaxPos - tmpArraryStartIndex)
                    , tmpArraryStartIndex);
            multipleChangepointsWithConfidence(result
                    , Arrays.copyOfRange(values, tmpMaxPos - tmpArraryStartIndex + 1, values.length)
                    , tmpMaxPos);
            result.add(new ChangePoint(tmpMaxPos, tmpConf));
        }

        return result;
    }


    /**
     *
     * @param values time series
     * @return log likelihood ratio for every observation
     */
    private double[] likelihoodRatioChangeInMean(double[] values){
        double[] result = new double[values.length];

        // vector containing means for all different vector lengths, last index contains mean over all elements
        double[] meansLeft = new double[values.length];
        Mean currentMeanLeft = new Mean();
        for(int i = 0; i < meansLeft.length; i++){
            currentMeanLeft.put(values[i]);
            meansLeft[i] = currentMeanLeft.getMean();
        }
        // first index contains mean over all elements coming from the other side
        double[] meansRight = new double[values.length];
        Mean currentMeanRight = new Mean();
        for(int i = meansRight.length-1; i >= 0; i--){
            currentMeanRight.put(values[i]);
            meansRight[i] = currentMeanRight.getMean();
        }

        result[0] = -(VMath.sumElements(VMath.square(VMath.minus(values, meansRight[0]))));
        for(int i = 1; i < values.length; i++){
            result[i] = -(  (VMath.squareSum(VMath.minus(Arrays.copyOfRange(values, 0, i), meansLeft[i-1])))
                        +   (VMath.squareSum(VMath.minus(Arrays.copyOfRange(values, i, values.length), meansRight[i])))
                        );
        }

        return result;
    }

    /*
    PROBABALY REMOVE
    // DOES NOT WORK - REASON NOT YET INVESTIGATED
    private double[] likelihoodRatioChangeInMeanOptimised(double[] values){
        double[] result = new double[values.length];

        // vector containing means for all different vector lengths, last index contains mean over all elements
        double[] meansLeft = new double[values.length];
        Mean currentMeanLeft = new Mean();
        for(int i = 0; i < meansLeft.length; i++){
            currentMeanLeft.put(values[i]);
            meansLeft[i] = currentMeanLeft.getMean();
        }
        // first index contains mean over all elements coming from the other side
        double[] meansRight = new double[values.length];
        Mean currentMeanRight = new Mean();
        for(int i = meansRight.length-1; i >= 0; i--){
            currentMeanRight.put(values[i]);
            meansRight[i] = currentMeanRight.getMean();
        }

        result[0]=0;
        double tmpMeanDif;
        for(int i = 1; i < values.length; i++) {
            tmpMeanDif = meansLeft[i-1] - meansRight[i];
            result[i]= -i*(values.length - i)*tmpMeanDif*tmpMeanDif;
        }

        return result;

    }


    //TRY OUT RESULT HANDLING
    private ChangePoint singleChangepointWithConfidence(double[] values, int bootstrapSteps){
        double conf = confidenceLevel(values, bootstrapSteps);
        double index = getMaximumIndex(likelihoodRatioChangeInMean(values));
        return new ChangePoint(index, conf);
    }

    */

    /**
     * Calculates the confidence for the most probable change point of the given timer series.
     * Confidence is calculated with the help of bootstrapping.
     *
     * @param values time series
     * @param steps steps for bootstrapping
     * @return confidence for most probable change point
     */
    private double confidenceLevel(double[] values, int steps){
        double estimator = getBootstrapEstimator(likelihoodRatioChangeInMean(values));
        int x = 0;
        for(int i=0; i < steps; i++){
            double[] tmpValues = shuffleVector(values);
            double tmpEstimator = getBootstrapEstimator(likelihoodRatioChangeInMean(tmpValues));
            if (tmpEstimator < estimator){
                x += 1;
            }
        }
        return 100 * ((double)x/(double)steps);
    }

    /**
     * Estimator used by bootstrapping
     *
     * @param values time series
     * @return bootstrap estimator
     */
    private double getBootstrapEstimator(double[] values){
        return getMaximum(values)- getMinimum(values);
    }

    /**
     * Shuffles the observations of a time series
     *
     * @param values time series
     * @return shuffled time series
     */
    private double[] shuffleVector(double[] values)
    {
        double[] result= VMath.copy(values);
        Random rnd = new Random();
        for (int i = result.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            double tmp = result[index];
            result[index] = result[i];
            result[i] = tmp;
        }

        return result;
    }

    /**
     * Returns index of maximum value in double array
     *
     * @param values array
     * @return index of maximum value
     */
    private int getMaximumIndex(double[] values){
        int result = 0;
        for (int i = 0; i < values.length; i++){
            if ((values[i] >= values[result])){
                result = i;
            }
        }
        return result;
    }

    /**
     * Returns maximum of a double array
     *
     * @param values array
     * @return maximum value
     */
    private double getMaximum(double[] values){
        double result = values[0];
        for(double value : values){
            if(value > result){
                result = value;
            }
        }
        return result;
    }

    /**
     * Returns minimum of a double array
     *
     * @param values array
     * @return minimum value
     */
    private double getMinimum(double[] values){
        double result = values[0];
        for(double value : values){
            if(value < result){
                result = value;
            }
        }
        return result;
    }

    @Override
    public TypeInformation[] getInputTypeRestriction() {
        return TypeUtil.array(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH, TypeUtil.LABELLIST);
    }

    @Override
    protected Logging getLogger() {
        return null;
    }

    public static class Parameterizer extends AbstractParameterizer {

        public static final OptionID CONFIDENCE_ID = new OptionID("changepointdetection.confidence", //
                "Confidence level for terminating");

        public static final OptionID BOOTSTRAP_ID = new OptionID("changepointdetection.bootstrapsteps", //
                "Steps for bootstrapping");

        private int confidence, bootstrap_steps;

        @Override
        protected void makeOptions(Parameterization config) {
            super.makeOptions(config);
            IntParameter confidence_parameter = new IntParameter(CONFIDENCE_ID) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
            if(config.grab(confidence_parameter)) {
                confidence = confidence_parameter.getValue();
            }
            IntParameter bootstrap_steps_parameter = new IntParameter(BOOTSTRAP_ID) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
            if(config.grab(bootstrap_steps_parameter)) {
                bootstrap_steps = bootstrap_steps_parameter.getValue();
            }
        }

        @Override
        protected OfflineChangePointDetectionAlgorithm makeInstance() {
            return new OfflineChangePointDetectionAlgorithm(confidence, bootstrap_steps);
        }
    }
}
