# 📖 Code Structure & Implementation Guide

## 📁 Directory Map với Giải Thích Chi Tiết

```
project-root/
│
├── 📄 README.md                           # Giới thiệu project (Vietnamese)
├── 📄 PROJECT_DOCUMENTATION.md            # 📚 Tài liệu toàn diện (đang đọc)
├── 📄 QUICK_START.md                      # ⚡ Hướng dẫn 5 bước chạy nhanh
├── 📄 ARCHITECTURE.md                     # 🏗️ Diagram kiến trúc chi tiết
│
├── 📄 requirements.txt                    # Python packages (pip)
│   └─ pandas, numpy, scikit-learn, torch, xgboost, ...
│
├── 🐍 Python Files (Data & ML)
│   ├── crawling.py
│   │   ├─ Crawl HTML từ Phongtro123.com
│   │   ├─ Parse & extract thông tin phòng
│   │   ├─ Clean dữ liệu (remove NaN, invalid values)
│   │   ├─ Remove outliers (Z-score)
│   │   ├─ Encode features (one-hot)
│   │   └─ Output: standard_dataset.csv → houseprice_dataset.csv
│   │
│   ├── data_mining.ipynb
│   │   ├─ Step 1: Load houseprice_dataset.csv
│   │   ├─ Step 2: Train-Test split (80-20)
│   │   ├─ Step 3: Train 4 models
│   │   │  ├─ Linear Regression
│   │   │  ├─ Random Forest (100 trees)
│   │   │  ├─ XGBoost
│   │   │  └─ Neural Network (PyTorch: 2 hidden layers)
│   │   ├─ Step 4: Evaluate (MAE, MSE, RMSE, R²)
│   │   ├─ Step 5: Visualize results
│   │   └─ Output: trained model files (.pkl, .pt)
│   │
│   └── helllo.py
│       ├─ Quick script để train toàn bộ pipeline
│       ├─ Tương đương: Run all cells trong data_mining.ipynb
│       └─ Output: model files
│
├── 📊 Data Files
│   ├── standard_dataset.csv
│   │   ├─ Raw data từ crawling
│   │   ├─ Columns: Mô tả, Diện tích, Vị trí, Ngày đăng, 
│   │   │           Người đăng, Đường link, Giá phòng
│   │   └─ ~1000+ rows (tùy crawl)
│   │
│   └── houseprice_dataset.csv
│       ├─ Processed data (sạch + encoded)
│       ├─ Columns: numeric features + target (price)
│       └─ ~800-900 rows (sau remove outlier)
│
├── 🎯 backend-springboot/
│   ├── pom.xml
│   │   ├─ Project: house-price-backend (v1.0.0)
│   │   ├─ Parent: spring-boot-starter-parent (v3.3.5)
│   │   ├─ Java: 17
│   │   ├─ Dependencies:
│   │   │  ├─ spring-boot-starter-web (REST API)
│   │   │  └─ spring-boot-starter-validation (Input validation)
│   │   └─ Build: Maven plugin
│   │
│   ├── application.properties
│   │   ├─ server.port=8080
│   │   ├─ logging levels
│   │   └─ model paths
│   │
│   └── src/main/java/com/hanoi/houseprice/
│       │
│       ├── 🚀 HousePriceApplication.java
│       │   └─ Entry point: SpringApplication.run()
│       │
│       ├── 🔌 controller/
│       │   └── PredictionController.java
│       │       ├─ @RestController (REST endpoint)
│       │       ├─ GET  /api/metadata
│       │       │   └─ Return: locations, features, modelInfo
│       │       │
│       │       └─ POST /api/predict
│       │           ├─ Input: PredictionRequest (DTO)
│       │           ├─ Validate: @Valid
│       │           ├─ Call: modelService.predict()
│       │           └─ Return: PredictionResponse (DTO)
│       │
│       ├── ⚙️ service/
│       │   └── HousePriceModelService.java
│       │       ├─ Load trained models (.pkl, .pt)
│       │       ├─ Preprocess input (encode, scale)
│       │       ├─ Call model.predict(X)
│       │       ├─ Format output
│       │       └─ Cache? (optional)
│       │
│       ├── 📦 dto/
│       │   ├── PredictionRequest.java
│       │   │   └─ Fields: diện_tích, vị_trí, ngày_đăng, mô_tả
│       │   ├── PredictionResponse.java
│       │   │   └─ Fields: predicted_price, confidence, model_name, timestamp
│       │   └── MetadataResponse.java
│       │       └─ Fields: locations[], features[], modelInfo
│       │
│       └── ⚙️ config/
│           └── WebConfig.java
│               ├─ CORS configuration
│               ├─ RestTemplate beans (if calling external APIs)
│               └─ Other Spring beans
│
└── 🎨 frontend-react/
    ├── package.json
    │   ├─ name: hanoi-house-price-frontend
    │   ├─ scripts: dev, build, preview
    │   └─ deps: react, vite, lucide-react
    │
    ├── vite.config.js
    │   ├─ Dev server config
    │   ├─ API proxy (optional)
    │   └─ Build optimization
    │
    ├── index.html
    │   └─ HTML entry point + <div id="root"></div>
    │
    └── src/
        ├── main.jsx
        │   ├─ React app entry
        │   ├─ CreateRoot (React 18)
        │   └─ Mount to #root
        │
        ├── App.jsx (or main component)
        │   ├─ Form input fields
        │   ├─ State management (useState)
        │   ├─ API calls (fetch)
        │   └─ Display prediction result
        │
        └── styles.css
            └─ CSS styling (form, result, responsive)
```

---

## 🔍 Key Files Chi Tiết

### 1. PredictionController.java - Frontend Entry Point

```java
@RestController
@RequestMapping("/api")
public class PredictionController {
    private final HousePriceModelService modelService;
    
    // GET /api/metadata
    // Trả về metadata: locations, features
    @GetMapping("/metadata")
    public MetadataResponse metadata() {
        return modelService.metadata();
    }
    
    // POST /api/predict
    // Nhận request, validate, predict
    @PostMapping("/predict")
    public PredictionResponse predict(
        @Valid @RequestBody PredictionRequest request
    ) {
        return modelService.predict(request);
    }
}
```

**Các HTTP Request**:
- `GET http://localhost:8080/api/metadata` → MetadataResponse
- `POST http://localhost:8080/api/predict` + PredictionRequest JSON → PredictionResponse

---

### 2. HousePriceModelService.java - Business Logic

```java
public class HousePriceModelService {
    // Gọi từ Controller
    
    public MetadataResponse metadata() {
        // Trả về:
        // - locations: ["Ba Đình", "Hoàn Kiếm", ...]
        // - features: ["diện_tích", "vị_trí", ...]
        // - modelInfo: {name: "XGBoost", accuracy: 0.78}
    }
    
    public PredictionResponse predict(PredictionRequest request) {
        // 1. Validate request
        // 2. Load model từ disk (xgboost.pkl)
        // 3. Preprocess:
        //    - One-hot encode: request.vị_trí → [0,1,0,...]
        //    - MinMaxScale: request.diện_tích → [0,1]
        // 4. model.predict(X) → price
        // 5. Format response
        //    {
        //      predicted_price: 4500000,
        //      confidence: 0.87,
        //      model_name: "XGBoost",
        //      timestamp: "2026-06-04T10:30:00Z"
        //    }
    }
}
```

---

### 3. DTOs (Data Transfer Objects)

#### PredictionRequest.java
```java
public class PredictionRequest {
    @NotNull
    private Double diện_tích;
    
    @NotBlank
    private String vị_trí;
    
    @NotNull
    private LocalDate ngày_đăng;
    
    private String mô_tả;
    
    // Getters, Setters, Constructors
}
```

#### PredictionResponse.java
```java
public class PredictionResponse {
    private Double predicted_price;      // VND
    private Double confidence;           // 0-1
    private String model_name;           // "XGBoost"
    private LocalDateTime timestamp;
    private PriceRange price_range;      // min, max
}
```

#### MetadataResponse.java
```java
public class MetadataResponse {
    private List<String> locations;      // ["Ba Đình", ...]
    private List<String> features;       // ["diện_tích", ...]
    private ModelInfo modelInfo;         // {name, accuracy, version}
}
```

---

### 4. Data Processing Pipeline (Python)

#### crawling.py - Flow

```python
# 1. Crawl
html = requests.get("https://phongtro123.com/...")
soup = BeautifulSoup(html)
# Extract: price, area, location, description

# 2. Clean
df = pd.read_csv("standard_dataset.csv")
# Remove: NaN, invalid types, negative values

# 3. Remove Outliers
from scipy import stats
z_scores = np.abs(stats.zscore(df[["diện_tích", "giá"]]))
df_cleaned = df[(z_scores < 3).all(axis=1)]

# 4. Feature Engineering
df_encoded = pd.get_dummies(df_cleaned, columns=["vị_trí"])
from sklearn.preprocessing import MinMaxScaler
scaler = MinMaxScaler()
df_scaled = scaler.fit_transform(df_encoded)

# 5. Save
df_scaled.to_csv("houseprice_dataset.csv")
```

#### data_mining.ipynb - Flow

```python
# Load
X = df[features]
y = df["giá_phòng"]

# Split
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)

# Train 4 models
from sklearn.linear_model import LinearRegression
from sklearn.ensemble import RandomForestRegressor
import xgboost as xgb
import torch

model_lr = LinearRegression().fit(X_train, y_train)
model_rf = RandomForestRegressor().fit(X_train, y_train)
model_xgb = xgb.XGBRegressor().fit(X_train, y_train)
model_nn = NeuralNetwork(...).fit(X_train, y_train)

# Evaluate
for model in [model_lr, model_rf, model_xgb, model_nn]:
    y_pred = model.predict(X_test)
    print(f"MAE: {mean_absolute_error(y_test, y_pred)}")
    print(f"R²: {r2_score(y_test, y_pred)}")

# Save
pickle.dump(model_xgb, "xgboost.pkl")
torch.save(model_nn.state_dict(), "neural_network.pt")
```

---

### 5. React Frontend - App Component

```jsx
// App.jsx
import React, { useState } from 'react';
import axios from 'axios';

export default function App() {
    const [formData, setFormData] = useState({
        diện_tích: '',
        vị_trí: '',
        ngày_đăng: '',
        mô_tả: ''
    });
    
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(false);
    
    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        
        try {
            const response = await fetch(
                'http://localhost:8080/api/predict',
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData)
                }
            );
            
            const data = await response.json();
            setResult({
                price: data.predicted_price,
                confidence: data.confidence
            });
        } catch (error) {
            console.error('Error:', error);
        } finally {
            setLoading(false);
        }
    };
    
    return (
        <div className="container">
            <h1>🏠 House Price Predictor</h1>
            
            <form onSubmit={handleSubmit}>
                <input
                    type="number"
                    placeholder="Diện tích (m²)"
                    value={formData.diện_tích}
                    onChange={(e) => setFormData({
                        ...formData,
                        diện_tích: e.target.value
                    })}
                />
                
                <select
                    value={formData.vị_trí}
                    onChange={(e) => setFormData({
                        ...formData,
                        vị_trí: e.target.value
                    })}
                >
                    <option>-- Chọn Vị Trí --</option>
                    <option>Ba Đình</option>
                    <option>Hoàn Kiếm</option>
                    {/* ... more locations ... */}
                </select>
                
                <button type="submit" disabled={loading}>
                    {loading ? 'Predicting...' : 'Predict'}
                </button>
            </form>
            
            {result && (
                <div className="result">
                    <h2>Predicted Price: {result.price.toLocaleString()} VND</h2>
                    <p>Confidence: {(result.confidence * 100).toFixed(2)}%</p>
                </div>
            )}
        </div>
    );
}
```

---

## 🔗 Relationship Diagram

```
Frontend (React)
    ↓ fetch("/api/predict")
Backend (Spring Boot)
    ├─ PredictionController
    │   ├─ @PostMapping("/predict")
    │   └─ calls: modelService.predict()
    │
    └─ HousePriceModelService
        ├─ Load model.pkl
        ├─ Preprocess input
        ├─ model.predict(X)
        └─ Return response

Data Layer (Python models)
    ├─ xgboost.pkl
    ├─ random_forest.pkl
    ├─ linear_regression.pkl
    └─ neural_network.pt
```

---

## 📝 Common Tasks

### Add a new location to dropdown

1. **Python**: Crawl new data từ location đó
2. **Training**: Re-train models với new location
3. **Backend**: Update MetadataResponse.locations list
4. **Frontend**: Add `<option>` in select

### Change prediction model

1. **Python**: Train different model (e.g., LightGBM)
2. **Backend**: Update HousePriceModelService.predict() to load new model
3. **Test**: Verify response

### Add new features (e.g., distance to bus stop)

1. **Python**: Add column in houseprice_dataset.csv
2. **Training**: Re-train with new feature
3. **Backend**: Update DTOs + preprocessing
4. **Frontend**: Add input field

---

## 🧪 Testing

### Unit Test Example (Backend)

```java
@SpringBootTest
class PredictionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testPredictEndpoint() throws Exception {
        String request = """
        {
            "diện_tích": 30.5,
            "vị_trí": "Ba Đình"
        }
        """;
        
        mockMvc.perform(post("/api/predict")
                .contentType(APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.predicted_price").exists());
    }
}
```

### Integration Test (Backend + Python Model)

```python
# Test từ Python side
import requests

response = requests.post(
    "http://localhost:8080/api/predict",
    json={
        "diện_tích": 30.5,
        "vị_trí": "Ba Đình"
    }
)

assert response.status_code == 200
assert response.json()["predicted_price"] > 0
```

---

## 📊 Data Flow Summary

```
Website (Phongtro123)
    ↓ crawling.py
standard_dataset.csv (raw)
    ↓ Data Cleaning + Feature Engineering
houseprice_dataset.csv (processed)
    ↓ data_mining.ipynb / helllo.py
Models: xgboost.pkl, random_forest.pkl, ...
    ↓ (Loaded by Backend)
Spring Boot Application
    ↓ (Called from)
React Frontend
    ↓ (User Input)
Prediction Result
    ↓ (Display)
Browser
```

---

**Created**: 2026-06-04
**Last Updated**: 2026-06-04

