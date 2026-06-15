# 🏗️ System Architecture - Chi Tiết Kiến Trúc Hệ Thống

## Diagram 1: Tổng Quan Kiến Trúc 3 Tầng

```
┌────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                          │
│                      (Frontend - React)                         │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Components:                                             │  │
│  │  - Input Form (Diện tích, Vị trí, Mô tả)               │  │
│  │  - Submit Button                                        │  │
│  │  - Result Display                                       │  │
│  │  - Error Handling                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Tech: React 18 + Vite + Lucide Icons                          │
└────────────────────────┬────────────────────────────────────────┘
                         │
                    HTTP Request/Response
                    JSON over HTTP REST
                         │
                         ▼
┌────────────────────────────────────────────────────────────────┐
│                    BUSINESS LOGIC LAYER                        │
│                  (Backend - Spring Boot 3.3)                   │
│                                                                 │
│  ┌─ REST Controller ────────────────────────────────────────┐  │
│  │ GET  /api/metadata                                       │  │
│  │ POST /api/predict                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│           │                                                    │
│           ▼                                                    │
│  ┌─ Service Layer ──────────────────────────────────────────┐  │
│  │ HousePriceModelService                                   │  │
│  │ - Load Models                                            │  │
│  │ - Validate Input                                         │  │
│  │ - Call Prediction                                        │  │
│  │ - Format Response                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│           │                                                    │
│           ▼                                                    │
│  ┌─ Model Integration ──────────────────────────────────────┐  │
│  │ - Load .pkl / .pt files                                  │  │
│  │ - PyTorch / Scikit-learn models                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Tech: Java 17 + Spring Boot + Maven                          │
└────────────────────────┬────────────────────────────────────────┘
                         │
                   Model Invocation
                         │
                         ▼
┌────────────────────────────────────────────────────────────────┐
│                    DATA LAYER                                  │
│              (Trained ML Models + Data)                        │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Models:                                                 │  │
│  │  - linear_regression.pkl                                │  │
│  │  - random_forest.pkl                                    │  │
│  │  - xgboost.pkl                                          │  │
│  │  - neural_network.pt                                    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Preprocessors:                                          │  │
│  │  - label_encoders.pkl                                   │  │
│  │  - scalers.pkl                                          │  │
│  │  - feature_mappings.json                                │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Datasets:                                               │  │
│  │  - houseprice_dataset.csv (training data)               │  │
│  │  - standard_dataset.csv (raw data)                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Tech: Python 3.x + Scikit-learn + PyTorch + XGBoost          │
└────────────────────────────────────────────────────────────────┘
```

---

## Diagram 2: Data Flow - Luồng Dữ Liệu

### Giai Đoạn Preparation (Chuẩn Bị)

```
┌──────────────┐
│ Phongtro123  │
│  Website     │
└──────┬───────┘
       │
       │ 1. Crawl HTML
       │    (requests + BeautifulSoup)
       ▼
┌──────────────────────┐
│  Raw Data (HTML)     │
└──────┬───────────────┘
       │
       │ 2. Parse & Extract
       │    (Extract: Giá, Diện tích, Vị trí, ...)
       ▼
┌──────────────────────┐
│ standard_dataset.csv │
│ (Dữ liệu thô)        │
│ Rows: ~1000+         │
└──────┬───────────────┘
       │
       │ 3. Data Cleaning
       │    - Drop NaN
       │    - Type conversion
       │    - Format standardization
       ▼
┌──────────────────────┐
│ Cleaned Data         │
│ Rows: ~900          │
└──────┬───────────────┘
       │
       │ 4. Remove Outliers
       │    (Z-score > 3)
       ▼
┌──────────────────────┐
│ Filtered Data        │
│ Rows: ~800          │
└──────┬───────────────┘
       │
       │ 5. Feature Engineering
       │    - One-hot encoding (Vị trí)
       │    - MinMaxScaler (Diện tích, Giá)
       ▼
┌──────────────────────┐
│houseprice_dataset.csv│
│ (Ready to train)     │
│ Cols: 50+            │
│ Rows: ~800           │
└──────────────────────┘
```

### Giai Đoạn Training (Huấn Luyện)

```
┌──────────────────────┐
│houseprice_dataset.csv│
└──────┬───────────────┘
       │
       │ 1. Load & Split
       │    Train: 80% (640)
       │    Test:  20% (160)
       ▼
    ┌──────────────────┐
    │ Train Set (640)  │
    └────┬─────────────┘
         │
    ┌────┴──────┬──────────┬──────────┐
    │            │          │          │
    ▼            ▼          ▼          ▼
┌────────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐
│   Linear   │ │ Random   │ │XGBoost │ │ Neural   │
│Regression  │ │ Forest   │ │        │ │ Network  │
└────┬───────┘ └────┬─────┘ └───┬────┘ └────┬─────┘
     │              │           │           │
     │ MAE: 0.85    │ MAE: 0.82 │ MAE: 0.79 │ MAE: 0.81
     │ R²: 0.72     │ R²: 0.75  │ R²: 0.78  │ R²: 0.76
     ▼              ▼           ▼           ▼
 ┌──────────────────────────────────────────┐
 │  2. Evaluate on Test Set (160 samples)   │
 │  Compare: MAE, MSE, RMSE, R²             │
 └──────┬───────────────────────────────────┘
        │
        │ Select best model
        │ (XGBoost - MAE: 0.79)
        ▼
 ┌──────────────────────────────────────────┐
 │  3. Save Models                          │
 │  - linear_regression.pkl                 │
 │  - random_forest.pkl                     │
 │  - xgboost.pkl        ← BEST             │
 │  - neural_network.pt                     │
 └──────────────────────────────────────────┘
```

### Giai Đoạn Prediction (Dự Đoán)

```
Frontend                    Backend                 Models
┌─────────────────┐
│ User Input:     │
│ - Diện tích: 30 │
│ - Vị trí: Ba Đình
│ - Mô tả: ...    │
└────────┬────────┘
         │
         │ POST /api/predict
         │ {data: JSON}
         ▼
         ┌──────────────────────┐
         │ ValidationController │
         └────────┬─────────────┘
                  │ Validate input
                  ▼
         ┌──────────────────────┐
         │ Preprocessing        │
         │ - One-hot encode     │
         │ - MinMaxScale        │
         └────────┬─────────────┘
                  │
                  ▼
         ┌──────────────────────┐
         │ Load Model           │
         │ (xgboost.pkl)        │
         └────────┬─────────────┘
                  │
                  ▼ prediction = model.predict(X)
                         ↓
                  ┌──────────────────────┐
                  │ Predicted Price      │
                  │ Value: 4,500,000 VND │
                  │ Confidence: 0.87     │
                  └────────┬─────────────┘
                           │
         ┌─────────────────┘
         │ Response JSON
         │ {price: 4500000, confidence: 0.87}
         │
         ▼
    ┌──────────────────┐
    │ Display Result   │
    │ 4,500,000 VND    │
    │ (87% confident)  │
    └──────────────────┘
```

---

## Diagram 3: Request-Response Flow

### API: GET /api/metadata

```
Frontend                          Backend
   │                                 │
   ├─ GET /api/metadata              │
   ├─────────────────────────────────>│
   │                          Controller
   │                                 │
   │                          Service
   │                                 │
   │                          Load metadata
   │                            (locations,
   │                             features)
   │                                 │
   │              {                  │
   │                "locations": [   │
   │                  "Ba Đình",      │
   │                  "Hoàn Kiếm",    │
   │                  "Hai Bà Trưng"  │
   │                ],               │
   │                "features": [    │
   │                  "diện_tích",    │
   │                  "vị_trí",       │
   │                  "mô_tả"         │
   │                ],               │
   │                "modelInfo": {   │
   │                  "name":        │
   │                  "XGBoost",      │
   │                  "accuracy": 0.78
   │                }                │
   │              }                  │
   │<─────────────────────────────────┤
   │                                 │
```

### API: POST /api/predict

```
┌─ Request ─────────────────────────────────────────────┐
│ POST http://localhost:8080/api/predict                 │
│ Content-Type: application/json                         │
│                                                        │
│ {                                                      │
│   "diện_tích": 30.5,                                   │
│   "vị_trí": "Ba Đình",                                 │
│   "ngày_đăng": "2026-06-04",                           │
│   "mô_tả": "Phòng đẹp, thoáng"                         │
│ }                                                      │
└────────────────────┬─────────────────────────────────┘
                     │
        Backend Processing:
                     │
        ├─ @PostMapping("/predict")
        ├─ @Valid @RequestBody PredictionRequest
        ├─ Validation (size, format)
        ├─ Feature Encoding:
        │  ├─ One-hot: vị_trí → [0,1,0,...] (Ba Đình)
        │  ├─ Scale: diện_tích 30.5 → 0.42 (MinMax [0,100])
        │  └─ Prepare X = [0.42, 0, 1, 0, ...]
        │
        ├─ Load Model: xgboost.pkl
        ├─ Predict: y = model.predict(X)
        │  └─ Output: 4500000 VND (price)
        │
        └─ Return PredictionResponse
                     │
┌─ Response ────────┴──────────────────────────────────┐
│ HTTP 200 OK                                           │
│ Content-Type: application/json                        │
│                                                       │
│ {                                                     │
│   "predicted_price": 4500000,                         │
│   "price_range": {                                    │
│     "min": 4200000,                                   │
│     "max": 4800000                                    │
│   },                                                 │
│   "confidence": 0.87,                                 │
│   "model_name": "XGBoost",                            │
│   "timestamp": "2026-06-04T10:30:00Z"                 │
│ }                                                     │
└────────────────────────────────────────────────────┘
```

---

## Diagram 4: Component Class Diagram

```
┌─────────────────────────────────────────────────────────┐
│                HousePriceApplication                    │
│           (@SpringBootApplication)                      │
└────────────────────────┬────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌────────────────┐ ┌────────────────┐ ┌────────────────┐
│ PredictionCtrl │ │  WebConfig     │ │   DTOs         │
└────┬───────────┘ │ (CORS, Config) │ └────────────────┘
     │             └────────────────┘
     │ depends on
     ▼
┌────────────────────────────────┐
│  HousePriceModelService        │
│  ─────────────────────────────  │
│  - metadata(): MetadataResponse │
│  - predict(req): PredictionResp │
└────────────────────────────────┘
     │ uses
     ▼
┌────────────────────────────────┐
│   Python ML Models             │
│   ─────────────────────────────  │
│ - xgboost.pkl                  │
│ - random_forest.pkl            │
│ - linear_regression.pkl        │
│ - neural_network.pt            │
└────────────────────────────────┘
```

---

## Diagram 5: Technology Stack

```
┌──────────────────────────────────────────────────────┐
│                    Frontend Layer                    │
│  React 18.3 + Vite 5.4 + Lucide Icons + CSS3        │
│  ├─ JavaScript (ES2020+)                            │
│  ├─ HTTP Client (fetch API)                         │
│  └─ Component-based Architecture                    │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│                  Backend Layer (API)                 │
│       Spring Boot 3.3 + Spring MVC + Maven          │
│  ├─ Java 17                                         │
│  ├─ REST/JSON API                                   │
│  ├─ Bean Validation (Jakarta)                       │
│  └─ Dependency Injection (Spring)                   │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│              ML/Data Processing Layer                │
│      Python 3.x + Scikit-learn + PyTorch            │
│  ├─ Data Processing:                                │
│  │  ├─ Pandas (manipulation)                        │
│  │  ├─ NumPy (numerical)                            │
│  │  ├─ BeautifulSoup (web scraping)                 │
│  │  └─ Requests (HTTP)                              │
│  │                                                  │
│  ├─ Machine Learning:                               │
│  │  ├─ Scikit-learn (Linear, Random Forest)         │
│  │  ├─ XGBoost (Gradient boosting)                  │
│  │  └─ PyTorch (Neural Networks)                    │
│  │                                                  │
│  └─ Visualization:                                  │
│     ├─ Matplotlib                                   │
│     └─ Seaborn                                      │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│           Build & Environment Tools                  │
│  Frontend: npm, Vite                                 │
│  Backend: Maven 3.x, Java 17                        │
│  Python: pip, virtualenv, Jupyter                   │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│            Data Storage & Models                     │
│  CSV: houseprice_dataset.csv (training)             │
│  Models: .pkl (Scikit), .pt (PyTorch)               │
│  Config: JSON, properties files                      │
└──────────────────────────────────────────────────────┘
```

---

## Diagram 6: Deployment Architecture (Development)

```
Developer Machine
├─ Terminal 1 (Python)
│  └─ venv
│     └─ python helllo.py
│        └─ Generates: *.pkl, *.pt files
│
├─ Terminal 2 (Backend)
│  └─ Java/Maven
│     └─ mvn spring-boot:run
│        └─ Server: http://localhost:8080
│           └─ Loads: *.pkl, *.pt files
│
└─ Terminal 3 (Frontend)
   └─ Node.js/npm
      └─ npm run dev
         └─ Browser: http://localhost:5173
            └─ Calls: http://localhost:8080/api/predict
```

---

## Summary - Tóm Tắt

| Layer | Technology | Responsibility |
|-------|-----------|-----------------|
| **Frontend** | React + Vite | UI, Form, Result Display |
| **Backend** | Spring Boot | API, Orchestration, Business Logic |
| **ML** | Python + Scikit/Torch | Training, Prediction, Data Processing |
| **Data** | CSV + Models | Datasets, Trained Models |

**Flow**: User Input → Frontend → Backend API → ML Model → Prediction → Response → Frontend Display

