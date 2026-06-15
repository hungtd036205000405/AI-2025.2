package com.hanoi.houseprice.service;

import com.hanoi.houseprice.dto.MetadataResponse;
import com.hanoi.houseprice.dto.ModelEvaluationMetric;
import com.hanoi.houseprice.dto.ModelPrediction;
import com.hanoi.houseprice.dto.PredictionRequest;
import com.hanoi.houseprice.dto.PredictionResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hanoi.houseprice.dto.BudgetSuggestRequest;
import com.hanoi.houseprice.dto.BudgetSuggestResponse;
import com.hanoi.houseprice.dto.FeatureImportanceResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Service
public class HousePriceModelService {
    private static final String AREA = "Diện tích";
    private static final String PRICE = "Giá phòng";
    private static final String DAY = "Ngày";
    private static final String MONTH = "Tháng";
    private static final String YEAR = "Năm";
    private static final String NUM_BEDROOMS = "Số phòng ngủ";
    private static final String NUM_BATHROOMS = "Số phòng tắm";
    private static final String HAS_AIR_CONDITIONING = "Điều hòa";
    private static final String FURNISHED = "Nội thất";
    private static final String FLOOR = "Tầng";
    private static final String DISTANCE_TO_CENTER = "Khoảng cách";
    private static final List<String> DISPLAY_DISTRICTS = List.of(
            "Ba Đình",
            "Bắc Từ Liêm",
            "Cầu Giấy",
            "Đống Đa",
            "Hai Bà Trưng",
            "Hà Đông",
            "Hoàn Kiếm",
            "Hoàng Mai",
            "Long Biên",
            "Nam Từ Liêm",
            "Tây Hồ",
            "Thanh Xuân",
            "Hà Nội"
    );

    @Value("${app.dataset.path:../houseprice_dataset.csv}")
    private String datasetPath;

    private final List<String> featureNames = new ArrayList<>();
    private final List<String> districts = new ArrayList<>();
    private final Map<String, String> normalizedDistrictToFeature = new HashMap<>();
    private double[] minValues;
    private double[] maxValues;
    private double[] weights;
    private double bias;
    private int trainingRows;
    private double averagePrice;
    private double averageArea;
    private double minPrice;
    private double maxPrice;
    private double residualRmse = 1.5;
    private final List<Map<String, Object>> districtAverages = new ArrayList<>();
    private final List<Map<String, Object>> priceBuckets = new ArrayList<>();
    private final List<Map<String, Object>> areaPricePoints = new ArrayList<>();

    // 4-Model structures
    private final List<ModelEvaluationMetric> metrics = new ArrayList<>();
    private RandomForestRegressor rfModel;
    private GradientBoostingRegressor xgbModel;
    private MLPRegressor nnModel;

    @PostConstruct
    public void train() throws IOException {
        Path path = resolveDatasetPath();
        if (!Files.exists(path)) {
            throw new IOException("Dataset not found: " + path.toAbsolutePath());
        }

        List<Map<String, String>> rows = readCsv(path);
        if (rows.isEmpty()) {
            throw new IOException("Dataset is empty: " + path.toAbsolutePath());
        }

        featureNames.clear();
        for (String column : rows.get(0).keySet()) {
            if (!PRICE.equals(column)) {
                featureNames.add(column);
            }
        }

        buildDistrictList();
        double[][] x = new double[rows.size()][featureNames.size()];
        double[] y = new double[rows.size()];

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Map<String, String> row = rows.get(rowIndex);
            y[rowIndex] = parseDouble(row.get(PRICE));
            for (int featureIndex = 0; featureIndex < featureNames.size(); featureIndex++) {
                x[rowIndex][featureIndex] = parseDouble(row.get(featureNames.get(featureIndex)));
            }
        }

        trainingRows = rows.size();
        averagePrice = mean(y);
        minPrice = min(y);
        maxPrice = max(y);
        averageArea = averageColumn(x, featureNames.indexOf(AREA));
        buildDashboardStats(x, y);

        // Train/Test Split (80/20) with seed 30 for reproducibility
        int nSamples = rows.size();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < nSamples; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, new Random(30));
        int trainSize = (int) (nSamples * 0.8);
        int testSize = nSamples - trainSize;

        double[][] xTrain = new double[trainSize][featureNames.size()];
        double[] yTrain = new double[trainSize];
        double[][] xTest = new double[testSize][featureNames.size()];
        double[] yTest = new double[testSize];

        for (int i = 0; i < trainSize; i++) {
            int idx = indices.get(i);
            yTrain[i] = y[idx];
            System.arraycopy(x[idx], 0, xTrain[i], 0, featureNames.size());
        }
        for (int i = 0; i < testSize; i++) {
            int idx = indices.get(trainSize + i);
            yTest[i] = y[idx];
            System.arraycopy(x[idx], 0, xTest[i], 0, featureNames.size());
        }

        fitMinMax(xTrain);
        double[][] scaledXTrain = scale(xTrain);
        double[][] scaledXTest = scale(xTest);

        // 1. Train Linear Regression
        fitLinearRegression(scaledXTrain, yTrain);
        double lrRmse = calculateRmse(scaledXTest, yTest, weights, bias);
        double lrMae = calculateMae(scaledXTest, yTest, weights, bias);
        double lrMse = calculateMse(scaledXTest, yTest, weights, bias);
        double lrR2 = calculateR2(scaledXTest, yTest, weights, bias);
        this.residualRmse = lrRmse;

        // 2. Train Random Forest
        rfModel = new RandomForestRegressor(150, 6);
        rfModel.fit(scaledXTrain, yTrain);
        double rfRmse = calculateRfRmse(scaledXTest, yTest, rfModel);
        double rfMae = calculateRfMae(scaledXTest, yTest, rfModel);
        double rfMse = calculateRfMse(scaledXTest, yTest, rfModel);
        double rfR2 = calculateRfR2(scaledXTest, yTest, rfModel);

        // 3. Train Neural Network
        nnModel = new MLPRegressor(featureNames.size());
        nnModel.fit(scaledXTrain, yTrain, 500, 0.01);
        double nnRmse = calculateNnRmse(scaledXTest, yTest, nnModel);
        double nnMae = calculateNnMae(scaledXTest, yTest, nnModel);
        double nnMse = calculateNnMse(scaledXTest, yTest, nnModel);
        double nnR2 = calculateNnR2(scaledXTest, yTest, nnModel);

        // 4. Train Gradient Boosting (XGBoost)
        xgbModel = new GradientBoostingRegressor(150, 0.05, 4);
        xgbModel.fit(scaledXTrain, yTrain);
        double xgbRmse = calculateXgbRmse(scaledXTest, yTest, xgbModel);
        double xgbMae = calculateXgbMae(scaledXTest, yTest, xgbModel);
        double xgbMse = calculateXgbMse(scaledXTest, yTest, xgbModel);
        double xgbR2 = calculateXgbR2(scaledXTest, yTest, xgbModel);

        // Store evaluation metrics in metrics list
        metrics.clear();
        metrics.add(new ModelEvaluationMetric("Random Forest Regressor", round(rfMae), round(rfMse), round(rfRmse), round(rfR2)));
        metrics.add(new ModelEvaluationMetric("Linear Regression", round(lrMae), round(lrMse), round(lrRmse), round(lrR2)));
        metrics.add(new ModelEvaluationMetric("Neural Network Regression", round(nnMae), round(nnMse), round(nnRmse), round(nnR2)));
        metrics.add(new ModelEvaluationMetric("XGBoost Regressor", round(xgbMae), round(xgbMse), round(xgbRmse), round(xgbR2)));
    }

    private Path resolveDatasetPath() throws IOException {
        List<Path> candidates = List.of(
                Path.of(datasetPath).normalize(),
                Path.of("houseprice_dataset.csv").normalize(),
                Path.of("../houseprice_dataset.csv").normalize()
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IOException("Dataset not found. Tried: " + candidates);
    }

    public PredictionResponse predict(PredictionRequest request) {
        double[] rawFeatures = buildFeatureVector(request);
        double[] scaledFeatures = scaleOne(rawFeatures);

        // 1. Linear Regression Prediction
        double lrPrediction = bias;
        for (int i = 0; i < weights.length; i++) {
            lrPrediction += weights[i] * scaledFeatures[i];
        }
        double lrFinal = clamp(lrPrediction, 0.5, 200.0);
        double lrInterval = Math.max(0.8, residualRmse);
        ModelPrediction lrModelPred = new ModelPrediction(
                round(lrFinal),
                round(Math.max(0.5, lrFinal - lrInterval)),
                round(lrFinal + lrInterval),
                "triệu VND/tháng",
                "Linear Regression"
        );

        // 2. Random Forest Prediction
        double rfPrediction = rfModel.predict(scaledFeatures);
        double rfFinal = clamp(rfPrediction, 0.5, 200.0);
        double rfInterval = Math.max(0.8, metrics.isEmpty() ? 1.0 : metrics.get(0).getRmse());
        ModelPrediction rfModelPred = new ModelPrediction(
                round(rfFinal),
                round(Math.max(0.5, rfFinal - rfInterval)),
                round(rfFinal + rfInterval),
                "triệu VND/tháng",
                "Random Forest Regressor"
        );

        // 3. Neural Network Prediction
        double nnPrediction = nnModel.predict(scaledFeatures);
        double nnFinal = clamp(nnPrediction, 0.5, 200.0);
        double nnInterval = Math.max(0.8, metrics.isEmpty() ? 1.2 : metrics.get(2).getRmse());
        ModelPrediction nnModelPred = new ModelPrediction(
                round(nnFinal),
                round(Math.max(0.5, nnFinal - nnInterval)),
                round(nnFinal + nnInterval),
                "triệu VND/tháng",
                "Neural Network Regression"
        );

        // 4. XGBoost Prediction
        double xgbPrediction = xgbModel.predict(scaledFeatures);
        double xgbFinal = clamp(xgbPrediction, 0.5, 200.0);
        double xgbInterval = Math.max(0.8, metrics.isEmpty() ? 1.5 : metrics.get(3).getRmse());
        ModelPrediction xgbModelPred = new ModelPrediction(
                round(xgbFinal),
                round(Math.max(0.5, xgbFinal - xgbInterval)),
                round(xgbFinal + xgbInterval),
                "triệu VND/tháng",
                "XGBoost Regressor"
        );

        Map<String, ModelPrediction> predictions = new LinkedHashMap<>();
        predictions.put("linear", lrModelPred);
        predictions.put("rf", rfModelPred);
        predictions.put("nn", nnModelPred);
        predictions.put("xgboost", xgbModelPred);

        return new PredictionResponse(
                round(lrFinal),
                round(Math.max(0.5, lrFinal - lrInterval)),
                round(lrFinal + lrInterval),
                "triệu VND/tháng",
                "Linear Regression trained from houseprice_dataset.csv",
                trainingRows,
                predictions,
                metrics
        );
    }

    public MetadataResponse metadata() {
        return new MetadataResponse(
                districts,
                trainingRows,
                featureNames.size(),
                round(averagePrice),
                round(averageArea),
                round(minPrice),
                round(maxPrice),
                districtAverages,
                priceBuckets,
                areaPricePoints,
                metrics
        );
    }

    // --- FEATURE IMPORTANCE ---
    private static final Map<String, String> FEATURE_VI_NAMES = new LinkedHashMap<>();
    static {
        FEATURE_VI_NAMES.put("Diện tích", "Diện tích (m²)");
        FEATURE_VI_NAMES.put("Số phòng ngủ", "Số phòng ngủ");
        FEATURE_VI_NAMES.put("Số phòng tắm", "Số phòng tắm");
        FEATURE_VI_NAMES.put("Điều hòa", "Có điều hòa");
        FEATURE_VI_NAMES.put("Nội thất", "Có nội thất");
        FEATURE_VI_NAMES.put("Tầng", "Tầng");
        FEATURE_VI_NAMES.put("Khoảng cách", "Khoảng cách TT");
        FEATURE_VI_NAMES.put("Ngày", "Ngày đăng");
        FEATURE_VI_NAMES.put("Tháng", "Tháng đăng");
        FEATURE_VI_NAMES.put("Năm", "Năm đăng");
        FEATURE_VI_NAMES.put("Ba Đình", "Quận Ba Đình");
        FEATURE_VI_NAMES.put("Bắc Từ Liêm", "Q. Bắc Từ Liêm");
        FEATURE_VI_NAMES.put("Cầu Giấy", "Quận Cầu Giấy");
        FEATURE_VI_NAMES.put("Đống Đa", "Quận Đống Đa");
        FEATURE_VI_NAMES.put("Hai Bà Trưng", "Q. Hai Bà Trưng");
        FEATURE_VI_NAMES.put("Hà Đông", "Quận Hà Đông");
        FEATURE_VI_NAMES.put("Hoàn Kiếm", "Q. Hoàn Kiếm");
        FEATURE_VI_NAMES.put("Hoàng Mai", "Q. Hoàng Mai");
        FEATURE_VI_NAMES.put("Long Biên", "Q. Long Biên");
        FEATURE_VI_NAMES.put("Nam Từ Liêm", "Q. Nam Từ Liêm");
        FEATURE_VI_NAMES.put("Tây Hồ", "Quận Tây Hồ");
        FEATURE_VI_NAMES.put("Thanh Xuân", "Q. Thanh Xuân");
        FEATURE_VI_NAMES.put("Hà Nội", "Hà Nội (khác)");
        FEATURE_VI_NAMES.put("studio", "Loại: Studio");
        FEATURE_VI_NAMES.put("apartment", "Loại: Căn hộ");
        FEATURE_VI_NAMES.put("house", "Loại: Nhà riêng");
    }

    public FeatureImportanceResponse featureImportance() {
        // Linear: use absolute weights as importance
        List<FeatureImportanceResponse.FeatureScore> linearScores = new ArrayList<>();
        if (weights != null) {
            double maxAbs = 0;
            for (double w : weights) maxAbs = Math.max(maxAbs, Math.abs(w));
            if (maxAbs == 0) maxAbs = 1;
            for (int i = 0; i < featureNames.size(); i++) {
                String fn = featureNames.get(i);
                String vi = FEATURE_VI_NAMES.getOrDefault(fn, fn);
                linearScores.add(new FeatureImportanceResponse.FeatureScore(fn, round(Math.abs(weights[i]) / maxAbs), vi));
            }
            linearScores.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));
            if (linearScores.size() > 8) linearScores = linearScores.subList(0, 8);
        }

        // RF / XGBoost: permutation importance via feature variance contribution
        List<FeatureImportanceResponse.FeatureScore> rfScores = computeTreeImportance("rf");
        List<FeatureImportanceResponse.FeatureScore> xgbScores = computeTreeImportance("xgb");
        List<FeatureImportanceResponse.FeatureScore> nnScores = computeNnImportance();

        return new FeatureImportanceResponse(linearScores, rfScores, xgbScores, nnScores);
    }

    private List<FeatureImportanceResponse.FeatureScore> computeTreeImportance(String modelType) {
        List<FeatureImportanceResponse.FeatureScore> scores = new ArrayList<>();
        if (featureNames.isEmpty() || minValues == null) return scores;

        // Use a fixed set of sample points to measure sensitivity
        int n = featureNames.size();
        double[] baseline = new double[n];
        for (int j = 0; j < n; j++) {
            baseline[j] = 0.5; // mid-point of normalized range
        }
        double basePred = modelType.equals("rf") ? rfModel.predict(baseline) : xgbModel.predict(baseline);

        double[] deltas = new double[n];
        for (int j = 0; j < n; j++) {
            double[] perturbed = baseline.clone();
            perturbed[j] = 1.0; // max
            double high = modelType.equals("rf") ? rfModel.predict(perturbed) : xgbModel.predict(perturbed);
            perturbed[j] = 0.0; // min
            double low = modelType.equals("rf") ? rfModel.predict(perturbed) : xgbModel.predict(perturbed);
            deltas[j] = Math.abs(high - low);
        }

        double maxDelta = 0;
        for (double d : deltas) maxDelta = Math.max(maxDelta, d);
        if (maxDelta == 0) maxDelta = 1;

        for (int j = 0; j < n; j++) {
            String fn = featureNames.get(j);
            String vi = FEATURE_VI_NAMES.getOrDefault(fn, fn);
            scores.add(new FeatureImportanceResponse.FeatureScore(fn, round(deltas[j] / maxDelta), vi));
        }
        scores.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));
        if (scores.size() > 8) scores = scores.subList(0, 8);
        return scores;
    }

    private List<FeatureImportanceResponse.FeatureScore> computeNnImportance() {
        List<FeatureImportanceResponse.FeatureScore> scores = new ArrayList<>();
        if (featureNames.isEmpty() || nnModel == null) return scores;

        int n = featureNames.size();
        double[] baseline = new double[n];
        for (int j = 0; j < n; j++) baseline[j] = 0.5;

        double[] deltas = new double[n];
        for (int j = 0; j < n; j++) {
            double[] perturbed = baseline.clone();
            perturbed[j] = 1.0;
            double high = nnModel.predict(perturbed);
            perturbed[j] = 0.0;
            double low = nnModel.predict(perturbed);
            deltas[j] = Math.abs(high - low);
        }

        double maxDelta = 0;
        for (double d : deltas) maxDelta = Math.max(maxDelta, d);
        if (maxDelta == 0) maxDelta = 1;

        for (int j = 0; j < n; j++) {
            String fn = featureNames.get(j);
            String vi = FEATURE_VI_NAMES.getOrDefault(fn, fn);
            scores.add(new FeatureImportanceResponse.FeatureScore(fn, round(deltas[j] / maxDelta), vi));
        }
        scores.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));
        if (scores.size() > 8) scores = scores.subList(0, 8);
        return scores;
    }

    // --- BUDGET SUGGEST ---
    public BudgetSuggestResponse budgetSuggest(BudgetSuggestRequest req) {
        List<BudgetSuggestResponse.DistrictOption> options = new ArrayList<>();

        // District price-per-m² from dataset averages
        Map<String, Double> districtAvgPricePerSqm = new LinkedHashMap<>();
        for (Map<String, Object> da : districtAverages) {
            String district = (String) da.get("district");
            double avgPrice = ((Number) da.get("averagePrice")).doubleValue();
            // Approximate typical area as 25 sqm for price/sqm calc
            districtAvgPricePerSqm.put(district, avgPrice / 25.0);
        }
        // Fallback price-per-sqm if no data
        if (districtAvgPricePerSqm.isEmpty()) {
            districtAvgPricePerSqm.put("Hoàn Kiếm", 0.22);
            districtAvgPricePerSqm.put("Ba Đình", 0.20);
            districtAvgPricePerSqm.put("Tây Hồ", 0.19);
            districtAvgPricePerSqm.put("Cầu Giấy", 0.17);
            districtAvgPricePerSqm.put("Đống Đa", 0.17);
            districtAvgPricePerSqm.put("Hai Bà Trưng", 0.16);
            districtAvgPricePerSqm.put("Thanh Xuân", 0.15);
            districtAvgPricePerSqm.put("Hoàng Mai", 0.13);
            districtAvgPricePerSqm.put("Long Biên", 0.13);
            districtAvgPricePerSqm.put("Hà Đông", 0.12);
            districtAvgPricePerSqm.put("Bắc Từ Liêm", 0.12);
            districtAvgPricePerSqm.put("Nam Từ Liêm", 0.13);
        }

        // Multiplier from amenity choices
        double multiplier = 1.0;
        if (req.getFurnished() == 1) multiplier += 0.15;
        if (req.getHasAirConditioning() == 1) multiplier += 0.10;
        multiplier += (req.getNumBedrooms() - 1) * 0.20;
        if ("apartment".equals(req.getRoomType())) multiplier += 0.15;
        else if ("house".equals(req.getRoomType())) multiplier += 0.35;

        for (Map.Entry<String, Double> entry : districtAvgPricePerSqm.entrySet()) {
            String district = entry.getKey();
            double basePricePerSqm = entry.getValue() * multiplier;
            if (basePricePerSqm <= 0) continue;

            double affordableArea = req.getBudgetMillion() / basePricePerSqm;
            double areaMin = Math.max(10, round(affordableArea * 0.85));
            double areaMax = round(affordableArea * 1.15);

            // Use mid-point area for estimated price
            double midArea = (areaMin + areaMax) / 2.0;
            double estimatedPrice = round(midArea * basePricePerSqm);

            options.add(new BudgetSuggestResponse.DistrictOption(
                    district,
                    areaMin,
                    areaMax,
                    estimatedPrice,
                    round(basePricePerSqm),
                    "" // tag assigned below
            ));
        }

        // Sort by area descending (most area for money = best value)
        options.sort((a, b) -> Double.compare(b.getSuggestedAreaMax(), a.getSuggestedAreaMax()));

        // Assign tags to top 3
        List<BudgetSuggestResponse.DistrictOption> tagged = new ArrayList<>();
        String[] tags = {"🏆 Nhiều diện tích nhất", "⚖️ Cân bằng nhất", "🏙️ Trung tâm nhất"};
        for (int i = 0; i < Math.min(options.size(), 6); i++) {
            BudgetSuggestResponse.DistrictOption opt = options.get(i);
            String tag = i < tags.length ? tags[i] : "";
            tagged.add(new BudgetSuggestResponse.DistrictOption(
                    opt.getDistrict(), opt.getSuggestedAreaMin(), opt.getSuggestedAreaMax(),
                    opt.getEstimatedPrice(), opt.getPricePerSqm(), tag
            ));
        }

        return new BudgetSuggestResponse(tagged, req.getBudgetMillion());
    }

    // Helper functions for evaluation metrics
    private double calculateRmse(double[][] x, double[] y, double[] w, double b) {
        return Math.sqrt(calculateMse(x, y, w, b));
    }

    private double calculateMae(double[][] x, double[] y, double[] w, double b) {
        double total = 0.0;
        for (int i = 0; i < x.length; i++) {
            double pred = b;
            for (int j = 0; j < w.length; j++) {
                pred += w[j] * x[i][j];
            }
            total += Math.abs(pred - y[i]);
        }
        return total / x.length;
    }

    private double calculateMse(double[][] x, double[] y, double[] w, double b) {
        double total = 0.0;
        for (int i = 0; i < x.length; i++) {
            double pred = b;
            for (int j = 0; j < w.length; j++) {
                pred += w[j] * x[i][j];
            }
            total += Math.pow(pred - y[i], 2);
        }
        return total / x.length;
    }

    private double calculateR2(double[][] x, double[] y, double[] w, double b) {
        double meanY = mean(y);
        double ssTot = 0.0;
        double ssRes = 0.0;
        for (int i = 0; i < x.length; i++) {
            double pred = b;
            for (int j = 0; j < w.length; j++) {
                pred += w[j] * x[i][j];
            }
            ssRes += Math.pow(y[i] - pred, 2);
            ssTot += Math.pow(y[i] - meanY, 2);
        }
        return ssTot == 0 ? 0.0 : 1.0 - (ssRes / ssTot);
    }

    private double calculateRfRmse(double[][] x, double[] y, RandomForestRegressor model) {
        return Math.sqrt(calculateRfMse(x, y, model));
    }

    private double calculateRfMae(double[][] x, double[] y, RandomForestRegressor model) {
        double total = 0.0;
        for (int i = 0; i < x.length; i++) {
            total += Math.abs(model.predict(x[i]) - y[i]);
        }
        return total / x.length;
    }

    private double calculateRfMse(double[][] x, double[] y, RandomForestRegressor model) {
        double total = 0.0;
        for (int i = 0; i < x.length; i++) {
            total += Math.pow(model.predict(x[i]) - y[i], 2);
        }
        return total / x.length;
    }

    private double calculateRfR2(double[][] x, double[] y, RandomForestRegressor model) {
        double meanY = mean(y);
        double ssTot = 0.0;
        double ssRes = 0.0;
        for (int i = 0; i < x.length; i++) {
            double pred = model.predict(x[i]);
            ssRes += Math.pow(y[i] - pred, 2);
            ssTot += Math.pow(y[i] - meanY, 2);
        }
        return ssTot == 0 ? 0.0 : 1.0 - (ssRes / ssTot);
    }

    private double calculateNnRmse(double[][] x, double[] y, MLPRegressor model) {
        return Math.sqrt(calculateNnMse(x, y, model));
    }

    private double calculateNnMae(double[][] x, double[] y, MLPRegressor model) {
        double total = 0.0;
        for (int i = 0; i < x.length; i++) {
            total += Math.abs(model.predict(x[i]) - y[i]);
        }
        return total / x.length;
    }

    private double calculateNnMse(double[][] x, double[] y, MLPRegressor model) {
        double total = 0.0;
        for (int i = 0; i < x.length; i++) {
            total += Math.pow(model.predict(x[i]) - y[i], 2);
        }
        return total / x.length;
    }

    private double calculateNnR2(double[][] x, double[] y, MLPRegressor model) {
        double meanY = mean(y);
        double ssTot = 0.0;
        double ssRes = 0.0;
        for (int i = 0; i < x.length; i++) {
            double pred = model.predict(x[i]);
            ssRes += Math.pow(y[i] - pred, 2);
            ssTot += Math.pow(y[i] - meanY, 2);
        }
        return ssTot == 0 ? 0.0 : 1.0 - (ssRes / ssTot);
    }

    private double calculateXgbRmse(double[][] x, double[] y, GradientBoostingRegressor model) {
        return Math.sqrt(calculateXgbMse(x, y, model));
    }

    private double calculateXgbMae(double[][] x, double[] y, GradientBoostingRegressor model) {
        double total = 0.0;
        for (int i = 0; i < x.length; i++) {
            total += Math.abs(model.predict(x[i]) - y[i]);
        }
        return total / x.length;
    }

    private double calculateXgbMse(double[][] x, double[] y, GradientBoostingRegressor model) {
        double total = 0.0;
        for (int i = 0; i < x.length; i++) {
            total += Math.pow(model.predict(x[i]) - y[i], 2);
        }
        return total / x.length;
    }

    private double calculateXgbR2(double[][] x, double[] y, GradientBoostingRegressor model) {
        double meanY = mean(y);
        double ssTot = 0.0;
        double ssRes = 0.0;
        for (int i = 0; i < x.length; i++) {
            double pred = model.predict(x[i]);
            ssRes += Math.pow(y[i] - pred, 2);
            ssTot += Math.pow(y[i] - meanY, 2);
        }
        return ssTot == 0 ? 0.0 : 1.0 - (ssRes / ssTot);
    }

    private void buildDashboardStats(double[][] x, double[] y) {
        districtAverages.clear();
        priceBuckets.clear();
        areaPricePoints.clear();

        Map<String, double[]> districtTotals = new HashMap<>();
        for (int i = 0; i < x.length; i++) {
            String district = detectDistrict(x[i]);
            districtTotals.putIfAbsent(district, new double[]{0.0, 0.0});
            districtTotals.get(district)[0] += y[i];
            districtTotals.get(district)[1] += 1.0;
        }

        districtTotals.entrySet().stream()
                .filter(entry -> entry.getValue()[1] >= 1.0)
                .sorted((a, b) -> Double.compare(
                        b.getValue()[0] / b.getValue()[1],
                        a.getValue()[0] / a.getValue()[1]
                ))
                .limit(8)
                .forEach(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("district", entry.getKey());
                    item.put("averagePrice", round(entry.getValue()[0] / entry.getValue()[1]));
                    item.put("count", (int) entry.getValue()[1]);
                    districtAverages.add(item);
                });

        double[][] bucketDefinitions = {
                {0, 2},
                {2, 4},
                {4, 6},
                {6, 8},
                {8, 10},
                {10, 15},
                {15, 999}
        };
        int[] counts = new int[bucketDefinitions.length];
        for (double price : y) {
            for (int i = 0; i < bucketDefinitions.length; i++) {
                if (price >= bucketDefinitions[i][0] && price < bucketDefinitions[i][1]) {
                    counts[i]++;
                    break;
                }
            }
        }
        for (int i = 0; i < bucketDefinitions.length; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            String label = bucketDefinitions[i][1] > 100 ? "15+" : formatBucket(bucketDefinitions[i][0], bucketDefinitions[i][1]);
            item.put("label", label);
            item.put("count", counts[i]);
            priceBuckets.add(item);
        }

        int areaIndex = featureNames.indexOf(AREA);
        int maxPoints = Math.min(70, x.length);
        for (int i = 0; i < maxPoints; i++) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("area", round(x[i][areaIndex]));
            point.put("price", round(y[i]));
            point.put("district", detectDistrict(x[i]));
            areaPricePoints.add(point);
        }
    }

    private String detectDistrict(double[] row) {
        for (String district : districts) {
            String featureName = normalizedDistrictToFeature.getOrDefault(normalizeLabel(district), district);
            int index = featureNames.indexOf(featureName);
            if (index >= 0 && row[index] >= 0.5) {
                return district;
            }
        }
        return "Hà Nội";
    }

    private String formatBucket(double from, double to) {
        return String.format(Locale.ROOT, "%.0f-%.0f", from, to);
    }

    private void buildDistrictList() {
        districts.clear();
        normalizedDistrictToFeature.clear();
        Map<String, String> displayByNormalized = new HashMap<>();
        for (String displayDistrict : DISPLAY_DISTRICTS) {
            displayByNormalized.put(normalizeLabel(displayDistrict), displayDistrict);
        }
        for (String feature : featureNames) {
            if (!List.of(AREA, DAY, MONTH, YEAR, NUM_BEDROOMS, NUM_BATHROOMS, HAS_AIR_CONDITIONING, FURNISHED, FLOOR, DISTANCE_TO_CENTER).contains(feature)) {
                String normalizedFeature = normalizeLabel(feature);
                if (displayByNormalized.containsKey(normalizedFeature)) {
                    String displayName = displayByNormalized.get(normalizedFeature);
                    if (!districts.contains(displayName)) {
                        districts.add(displayName);
                    }
                    normalizedDistrictToFeature.put(normalizedFeature, feature);
                    normalizedDistrictToFeature.put(normalizeLabel(displayName), feature);
                }
            }
        }
        districts.sort(Comparator.naturalOrder());
    }

    private double[] buildFeatureVector(PredictionRequest request) {
        double[] features = new double[featureNames.size()];
        String districtFeature = normalizedDistrictToFeature.getOrDefault(
                normalizeLabel(request.getDistrict()),
                request.getDistrict()
        );

        for (int i = 0; i < featureNames.size(); i++) {
            String feature = featureNames.get(i);
            if (AREA.equals(feature)) {
                features[i] = request.getArea();
            } else if (DAY.equals(feature)) {
                features[i] = request.getDay();
            } else if (MONTH.equals(feature)) {
                features[i] = request.getMonth();
            } else if (YEAR.equals(feature)) {
                features[i] = request.getYear();
            } else if (NUM_BEDROOMS.equals(feature)) {
                features[i] = request.getNumBedrooms();
            } else if (NUM_BATHROOMS.equals(feature)) {
                features[i] = request.getNumBathrooms();
            } else if (HAS_AIR_CONDITIONING.equals(feature)) {
                features[i] = request.getHasAirConditioning();
            } else if (FURNISHED.equals(feature)) {
                features[i] = request.getFurnished();
            } else if (FLOOR.equals(feature)) {
                features[i] = request.getFloor();
            } else if (DISTANCE_TO_CENTER.equals(feature)) {
                features[i] = request.getDistanceToCenter();
            } else if ("studio".equals(feature)) {
                features[i] = "studio".equalsIgnoreCase(request.getRoomType()) ? 1.0 : 0.0;
            } else if ("apartment".equals(feature)) {
                features[i] = "apartment".equalsIgnoreCase(request.getRoomType()) ? 1.0 : 0.0;
            } else if ("house".equals(feature)) {
                features[i] = "house".equalsIgnoreCase(request.getRoomType()) ? 1.0 : 0.0;
            } else {
                features[i] = feature.equals(districtFeature) ? 1.0 : 0.0;
            }
        }
        return features;
    }

    private void fitMinMax(double[][] x) {
        int featureCount = x[0].length;
        minValues = new double[featureCount];
        maxValues = new double[featureCount];
        for (int j = 0; j < featureCount; j++) {
            minValues[j] = Double.POSITIVE_INFINITY;
            maxValues[j] = Double.NEGATIVE_INFINITY;
            for (double[] row : x) {
                minValues[j] = Math.min(minValues[j], row[j]);
                maxValues[j] = Math.max(maxValues[j], row[j]);
            }
        }
    }

    private double[][] scale(double[][] x) {
        double[][] scaled = new double[x.length][x[0].length];
        for (int i = 0; i < x.length; i++) {
            scaled[i] = scaleOne(x[i]);
        }
        return scaled;
    }

    private double[] scaleOne(double[] row) {
        double[] scaled = new double[row.length];
        for (int j = 0; j < row.length; j++) {
            double range = maxValues[j] - minValues[j];
            scaled[j] = range == 0.0 ? 0.0 : (row[j] - minValues[j]) / range;
        }
        return scaled;
    }

    private void fitLinearRegression(double[][] x, double[] y) {
        int rows = x.length;
        int features = x[0].length;
        weights = new double[features];
        bias = averagePrice;
        double learningRate = 0.03;
        double l2 = 0.0005;

        for (int epoch = 0; epoch < 6000; epoch++) {
            double[] gradientWeights = new double[features];
            double gradientBias = 0.0;

            for (int i = 0; i < rows; i++) {
                double prediction = bias;
                for (int j = 0; j < features; j++) {
                    prediction += weights[j] * x[i][j];
                }
                double error = prediction - y[i];
                gradientBias += error;
                for (int j = 0; j < features; j++) {
                    gradientWeights[j] += error * x[i][j] + l2 * weights[j];
                }
            }

            bias -= learningRate * gradientBias / rows;
            for (int j = 0; j < features; j++) {
                weights[j] -= learningRate * gradientWeights[j] / rows;
            }
        }
    }

    private List<Map<String, String>> readCsv(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return List.of();
        }

        String headerLine = stripBom(lines.get(0));
        List<String> headers = parseCsvLine(headerLine);
        List<Map<String, String>> rows = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> values = parseCsvLine(lines.get(i));
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                row.put(headers.get(j), j < values.size() ? values.get(j) : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String stripBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        return Double.parseDouble(value.trim());
    }

    private static double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return values.length == 0 ? 0.0 : sum / values.length;
    }

    private double min(double[] values) {
        double result = Double.POSITIVE_INFINITY;
        for (double value : values) {
            result = Math.min(result, value);
        }
        return values.length == 0 ? 0.0 : result;
    }

    private double max(double[] values) {
        double result = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            result = Math.max(result, value);
        }
        return values.length == 0 ? 0.0 : result;
    }

    private double averageColumn(double[][] x, int columnIndex) {
        if (columnIndex < 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double[] row : x) {
            sum += row[columnIndex];
        }
        return x.length == 0 ? 0.0 : sum / x.length;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private String normalizeLabel(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("quan ", "")
                .replace("huyen ", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized;
    }

    // Helper regression split methods
    private static boolean allSame(double[] values) {
        if (values.length == 0) return true;
        double first = values[0];
        for (double val : values) {
            if (Math.abs(val - first) > 1e-9) return false;
        }
        return true;
    }

    private static List<Double> getUniqueValues(double[][] x, int f) {
        List<Double> list = new ArrayList<>();
        for (double[] row : x) {
            double val = row[f];
            if (!list.contains(val)) {
                list.add(val);
            }
        }
        return list;
    }

    private static double calculateSplitSse(double[] y, List<Integer> leftIdx, List<Integer> rightIdx) {
        double sumLeft = 0;
        for (int idx : leftIdx) sumLeft += y[idx];
        double meanLeft = sumLeft / leftIdx.size();
        
        double sumRight = 0;
        for (int idx : rightIdx) sumRight += y[idx];
        double meanRight = sumRight / rightIdx.size();

        double sse = 0;
        for (int idx : leftIdx) sse += Math.pow(y[idx] - meanLeft, 2);
        for (int idx : rightIdx) sse += Math.pow(y[idx] - meanRight, 2);
        return sse;
    }

    private static double[] toDoubleArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    // --- ML MODELS IMPLEMENTATION ---

    public static class RegressionTree {
        private static class Node {
            boolean isLeaf;
            double value;
            int splitFeature = -1;
            double splitThreshold;
            Node left;
            Node right;
        }

        private final int maxDepth;
        private final int minSamplesSplit;
        private Node root;

        public RegressionTree(int maxDepth, int minSamplesSplit) {
            this.maxDepth = maxDepth;
            this.minSamplesSplit = minSamplesSplit;
        }

        public void fit(double[][] x, double[] y) {
            root = buildTree(x, y, 0);
        }

        private Node buildTree(double[][] x, double[] y, int depth) {
            Node node = new Node();
            if (x.length == 0) {
                node.isLeaf = true;
                node.value = 0;
                return node;
            }

            double meanY = mean(y);
            if (depth >= maxDepth || x.length < minSamplesSplit || allSame(y)) {
                node.isLeaf = true;
                node.value = meanY;
                return node;
            }

            int bestFeature = -1;
            double bestThreshold = 0;
            double minSse = Double.POSITIVE_INFINITY;
            int nFeatures = x[0].length;

            for (int f = 0; f < nFeatures; f++) {
                List<Double> thresholds = getUniqueValues(x, f);
                for (double t : thresholds) {
                    List<Integer> leftIdx = new ArrayList<>();
                    List<Integer> rightIdx = new ArrayList<>();
                    for (int i = 0; i < x.length; i++) {
                        if (x[i][f] <= t) leftIdx.add(i);
                        else rightIdx.add(i);
                    }

                    if (leftIdx.isEmpty() || rightIdx.isEmpty()) continue;

                    double sse = calculateSplitSse(y, leftIdx, rightIdx);
                    if (sse < minSse) {
                        minSse = sse;
                        bestFeature = f;
                        bestThreshold = t;
                    }
                }
            }

            if (bestFeature == -1) {
                node.isLeaf = true;
                node.value = meanY;
                return node;
            }

            node.splitFeature = bestFeature;
            node.splitThreshold = bestThreshold;

            List<double[]> leftXList = new ArrayList<>();
            List<Double> leftYList = new ArrayList<>();
            List<double[]> rightXList = new ArrayList<>();
            List<Double> rightYList = new ArrayList<>();

            for (int i = 0; i < x.length; i++) {
                if (x[i][bestFeature] <= bestThreshold) {
                    leftXList.add(x[i]);
                    leftYList.add(y[i]);
                } else {
                    rightXList.add(x[i]);
                    rightYList.add(y[i]);
                }
            }

            node.left = buildTree(leftXList.toArray(new double[0][]), toDoubleArray(leftYList), depth + 1);
            node.right = buildTree(rightXList.toArray(new double[0][]), toDoubleArray(rightYList), depth + 1);
            return node;
        }

        public double predict(double[] row) {
            return predictNode(root, row);
        }

        private double predictNode(Node node, double[] row) {
            if (node.isLeaf) return node.value;
            if (row[node.splitFeature] <= node.splitThreshold) {
                return predictNode(node.left, row);
            } else {
                return predictNode(node.right, row);
            }
        }
    }

    public static class RandomForestRegressor {
        private final int nTrees;
        private final int maxDepth;
        private final List<RegressionTree> trees = new ArrayList<>();

        public RandomForestRegressor(int nTrees, int maxDepth) {
            this.nTrees = nTrees;
            this.maxDepth = maxDepth;
        }

        public void fit(double[][] x, double[] y) {
            Random rand = new Random(30);
            int nSamples = x.length;
            for (int t = 0; t < nTrees; t++) {
                double[][] xBoot = new double[nSamples][];
                double[] yBoot = new double[nSamples];
                for (int i = 0; i < nSamples; i++) {
                    int idx = rand.nextInt(nSamples);
                    xBoot[i] = x[idx];
                    yBoot[i] = y[idx];
                }
                RegressionTree tree = new RegressionTree(maxDepth, 2);
                tree.fit(xBoot, yBoot);
                trees.add(tree);
            }
        }

        public double predict(double[] row) {
            double sum = 0;
            for (RegressionTree tree : trees) {
                sum += tree.predict(row);
            }
            return sum / trees.size();
        }
    }

    public static class GradientBoostingRegressor {
        private final int nEstimators;
        private final double learningRate;
        private final int maxDepth;
        private final List<RegressionTree> trees = new ArrayList<>();
        private double basePrediction;

        public GradientBoostingRegressor(int nEstimators, double learningRate, int maxDepth) {
            this.nEstimators = nEstimators;
            this.learningRate = learningRate;
            this.maxDepth = maxDepth;
        }

        public void fit(double[][] x, double[] y) {
            int nSamples = x.length;
            basePrediction = mean(y);

            double[] residuals = new double[nSamples];
            for (int i = 0; i < nSamples; i++) {
                residuals[i] = y[i] - basePrediction;
            }

            for (int t = 0; t < nEstimators; t++) {
                RegressionTree tree = new RegressionTree(maxDepth, 2);
                tree.fit(x, residuals);
                trees.add(tree);

                for (int i = 0; i < nSamples; i++) {
                    residuals[i] -= learningRate * tree.predict(x[i]);
                }
            }
        }

        public double predict(double[] row) {
            double pred = basePrediction;
            for (RegressionTree tree : trees) {
                pred += learningRate * tree.predict(row);
            }
            return pred;
        }
    }

    public static class MLPRegressor {
        private final int inputSize;
        private final int hiddenSize1 = 16;
        private final int hiddenSize2 = 8;

        private double[][] w1;
        private double[] b1;
        private double[][] w2;
        private double[] b2;
        private double[] w3;
        private double b3;

        public MLPRegressor(int inputSize) {
            this.inputSize = inputSize;
            initializeWeights();
        }

        private void initializeWeights() {
            Random rand = new Random(30);
            w1 = new double[hiddenSize1][inputSize];
            b1 = new double[hiddenSize1];
            w2 = new double[hiddenSize2][hiddenSize1];
            b2 = new double[hiddenSize2];
            w3 = new double[hiddenSize2];

            for (int i = 0; i < hiddenSize1; i++) {
                for (int j = 0; j < inputSize; j++) {
                    w1[i][j] = (rand.nextDouble() * 2 - 1) * Math.sqrt(2.0 / inputSize);
                }
            }
            for (int i = 0; i < hiddenSize2; i++) {
                for (int j = 0; j < hiddenSize1; j++) {
                    w2[i][j] = (rand.nextDouble() * 2 - 1) * Math.sqrt(2.0 / hiddenSize1);
                }
            }
            for (int i = 0; i < hiddenSize2; i++) {
                w3[i] = (rand.nextDouble() * 2 - 1) * Math.sqrt(2.0 / hiddenSize2);
            }
            b3 = 0.0;
        }

        public void fit(double[][] x, double[] y, int epochs, double lr) {
            int nSamples = x.length;
            double avgY = mean(y);
            b3 = avgY; // Initialize output bias to target mean

            for (int epoch = 0; epoch < epochs; epoch++) {
                for (int i = 0; i < nSamples; i++) {
                    double[] input = x[i];
                    double target = y[i];

                    double[] z1 = new double[hiddenSize1];
                    double[] a1 = new double[hiddenSize1];
                    for (int h = 0; h < hiddenSize1; h++) {
                        double sum = b1[h];
                        for (int j = 0; j < inputSize; j++) {
                            sum += w1[h][j] * input[j];
                        }
                        z1[h] = sum;
                        a1[h] = Math.max(0, sum);
                    }

                    double[] z2 = new double[hiddenSize2];
                    double[] a2 = new double[hiddenSize2];
                    for (int h = 0; h < hiddenSize2; h++) {
                        double sum = b2[h];
                        for (int j = 0; j < hiddenSize1; j++) {
                            sum += w2[h][j] * a1[j];
                        }
                        z2[h] = sum;
                        a2[h] = Math.max(0, sum);
                    }

                    double pred = b3;
                    for (int h = 0; h < hiddenSize2; h++) {
                        pred += w3[h] * a2[h];
                    }

                    double error = pred - target;

                    double d_b3 = error;
                    double[] d_w3 = new double[hiddenSize2];
                    for (int h = 0; h < hiddenSize2; h++) {
                        d_w3[h] = error * a2[h];
                    }

                    double[] d_a2 = new double[hiddenSize2];
                    for (int h = 0; h < hiddenSize2; h++) {
                        d_a2[h] = error * w3[h];
                    }

                    double[] d_b2 = new double[hiddenSize2];
                    double[][] d_w2 = new double[hiddenSize2][hiddenSize1];
                    for (int h = 0; h < hiddenSize2; h++) {
                        double reluGrad = z2[h] > 0 ? 1 : 0;
                        double d_z2 = d_a2[h] * reluGrad;
                        d_b2[h] = d_z2;
                        for (int j = 0; j < hiddenSize1; j++) {
                            d_w2[h][j] = d_z2 * a1[j];
                        }
                    }

                    double[] d_a1 = new double[hiddenSize1];
                    for (int j = 0; j < hiddenSize1; j++) {
                        double sum = 0.0;
                        for (int h = 0; h < hiddenSize2; h++) {
                            double reluGrad = z2[h] > 0 ? 1 : 0;
                            sum += d_a2[h] * reluGrad * w2[h][j];
                        }
                        d_a1[j] = sum;
                    }

                    double[] d_b1 = new double[hiddenSize1];
                    double[][] d_w1 = new double[hiddenSize1][inputSize];
                    for (int h = 0; h < hiddenSize1; h++) {
                        double reluGrad = z1[h] > 0 ? 1 : 0;
                        double d_z1 = d_a1[h] * reluGrad;
                        d_b1[h] = d_z1;
                        for (int j = 0; j < inputSize; j++) {
                            d_w1[h][j] = d_z1 * input[j];
                        }
                    }

                    b3 -= lr * d_b3;
                    for (int h = 0; h < hiddenSize2; h++) {
                        w3[h] -= lr * d_w3[h];
                    }
                    for (int h = 0; h < hiddenSize2; h++) {
                        b2[h] -= lr * d_b2[h];
                        for (int j = 0; j < hiddenSize1; j++) {
                            w2[h][j] -= lr * d_w2[h][j];
                        }
                    }
                    for (int h = 0; h < hiddenSize1; h++) {
                        b1[h] -= lr * d_b1[h];
                        for (int j = 0; j < inputSize; j++) {
                            w1[h][j] -= lr * d_w1[h][j];
                        }
                    }
                }
            }
        }

        public double predict(double[] row) {
            double[] a1 = new double[hiddenSize1];
            for (int h = 0; h < hiddenSize1; h++) {
                double sum = b1[h];
                for (int j = 0; j < inputSize; j++) {
                    sum += w1[h][j] * row[j];
                }
                a1[h] = Math.max(0, sum);
            }

            double[] a2 = new double[hiddenSize2];
            for (int h = 0; h < hiddenSize2; h++) {
                double sum = b2[h];
                for (int j = 0; j < hiddenSize1; j++) {
                    sum += w2[h][j] * a1[j];
                }
                a2[h] = Math.max(0, sum);
            }

            double pred = b3;
            for (int h = 0; h < hiddenSize2; h++) {
                pred += w3[h] * a2[h];
            }
            return pred;
        }
    }
}
