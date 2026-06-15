package com.hanoi.houseprice.dto;

import java.util.List;
import java.util.Map;

public class PredictionResponse {
    private final double predictedPriceMillion;
    private final double lowerBoundMillion;
    private final double upperBoundMillion;
    private final String unit;
    private final String modelName;
    private final int trainingRows;
    private final Map<String, ModelPrediction> predictions;
    private final List<ModelEvaluationMetric> metrics;

    public PredictionResponse(
            double predictedPriceMillion,
            double lowerBoundMillion,
            double upperBoundMillion,
            String unit,
            String modelName,
            int trainingRows,
            Map<String, ModelPrediction> predictions,
            List<ModelEvaluationMetric> metrics
    ) {
        this.predictedPriceMillion = predictedPriceMillion;
        this.lowerBoundMillion = lowerBoundMillion;
        this.upperBoundMillion = upperBoundMillion;
        this.unit = unit;
        this.modelName = modelName;
        this.trainingRows = trainingRows;
        this.predictions = predictions;
        this.metrics = metrics;
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

    public int getTrainingRows() {
        return trainingRows;
    }

    public Map<String, ModelPrediction> getPredictions() {
        return predictions;
    }

    public List<ModelEvaluationMetric> getMetrics() {
        return metrics;
    }
}
