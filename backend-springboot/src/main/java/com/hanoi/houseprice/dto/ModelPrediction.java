package com.hanoi.houseprice.dto;

public class ModelPrediction {
    private final double predictedPriceMillion;
    private final double lowerBoundMillion;
    private final double upperBoundMillion;
    private final String unit;
    private final String modelName;

    public ModelPrediction(
            double predictedPriceMillion,
            double lowerBoundMillion,
            double upperBoundMillion,
            String unit,
            String modelName
    ) {
        this.predictedPriceMillion = predictedPriceMillion;
        this.lowerBoundMillion = lowerBoundMillion;
        this.upperBoundMillion = upperBoundMillion;
        this.unit = unit;
        this.modelName = modelName;
    }

    public double getPredictedPriceMillion() {
        return predictedPriceMillion;
    }

    public double getLowerBoundMillion() {
        return lowerBoundMillion;
    }

    public double getUpperBoundMillion() {
        return upperBoundMillion;
    }

    public String getUnit() {
        return unit;
    }

    public String getModelName() {
        return modelName;
    }
}
