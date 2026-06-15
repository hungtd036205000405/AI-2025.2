package com.hanoi.houseprice.dto;

public class ModelEvaluationMetric {
    private final String model;
    private final double mae;
    private final double mse;
    private final double rmse;
    private final double r2;

    public ModelEvaluationMetric(String model, double mae, double mse, double rmse, double r2) {
        this.model = model;
        this.mae = mae;
        this.mse = mse;
        this.rmse = rmse;
        this.r2 = r2;
    }

    public String getModel() {
        return model;
    }

    public double getMae() {
        return mae;
    }

    public double getMse() {
        return mse;
    }

    public double getRmse() {
        return rmse;
    }

    public double getR2() {
        return r2;
    }
}
