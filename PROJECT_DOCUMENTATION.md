# 📚 Tài Liệu Dự Án - House Price Prediction

## 🎯 Tổng Quan Dự Án

Đây là hệ thống **dự đoán giá thuê phòng/nhà trọ tại Hà Nội** sử dụng Machine Learning. Dự án kết hợp:
- **Backend Python**: Crawl dữ liệu, xử lý dữ liệu, train mô hình ML
- **Backend Java (Spring Boot)**: API server cung cấp dịch vụ dự đoán
- **Frontend React**: Giao diện người dùng để nhập thông tin và xem kết quả

---

## 🏗️ Kiến Trúc Dự Án

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React)                         │
│              - Giao diện người dùng                         │
│              - Nhập dữ liệu: Diện tích, Vị trí, v.v         │
│              - Hiển thị kết quả dự đoán                     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ HTTP Request
                     ▼
┌─────────────────────────────────────────────────────────────┐
│          Backend - Spring Boot (Java 17)                    │
│              - REST API (/api/predict)                      │
│              - Nhận yêu cầu dự đoán                         │
│              - Gọi mô hình ML                               │
│              - Trả về kết quả                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ Gọi Python Model
                     ▼
┌─────────────────────────────────────────────────────────────┐
│        Python ML Models (Trained & Saved)                   │
│              - Linear Regression                            │
│              - Random Forest                                │
│              - XGBoost                                      │
│              - Neural Network (PyTorch)                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 📂 Cấu Trúc Thư Mục

```
project-root/
├── README.md                          # Hướng dẫn nhanh
├── PROJECT_DOCUMENTATION.md           # Tài liệu này
├── requirements.txt                   # Python dependencies
├── crawling.py                        # Script crawl dữ liệu từ Phongtro123
├── data_mining.ipynb                  # Notebook Jupyter: quy trình ML
├── helllo.py                          # Script train toàn bộ pipeline
├── houseprice_dataset.csv             # Dữ liệu đã xử lý (sẵn sàng train)
├── standard_dataset.csv               # Dữ liệu thô từ crawl
│
├── backend-springboot/                # Spring Boot Backend
│   ├── pom.xml                        # Maven configuration
│   └── src/main/java/com/hanoi/houseprice/
│       ├── HousePriceApplication.java # Entry point
│       ├── controller/
│       │   └── PredictionController.java
│       ├── service/
│       │   └── HousePriceModelService.java
│       ├── dto/
│       │   ├── PredictionRequest.java
│       │   ├── PredictionResponse.java
│       │   └── MetadataResponse.java
│       └── config/
│           └── WebConfig.java
│
└── frontend-react/                    # React Frontend
    ├── package.json                   # npm dependencies
    ├── vite.config.js                 # Vite configuration
    ├── index.html                     # HTML entry point
    └── src/
        ├── main.jsx                   # React app entry
        └── styles.css                 # CSS styles
```

---

## 🔄 Quy Trình Vận Hành

### 1️⃣ Giai Đoạn Chuẩn Bị Dữ Liệu (Python)

**File**: `crawling.py`

```
Phongtro123.com
      ↓
  Crawling (requests + BeautifulSoup)
      ↓
  Standard Dataset (Tiếng Việt)
      ↓
  Data Cleaning
    - Loại bỏ rows có missing values
    - Chuẩn hóa định dạng dữ liệu
      ↓
  Remove Outliers (Z-score)
    - Loại điểm ngoại biên trên Diện tích & Giá
      ↓
  Feature Engineering
    - One-hot encoding cho Vị trí
    - MinMaxScaler chuẩn hóa số liệu
      ↓
  House Price Dataset (Ready to train)
```

**Cột dữ liệu**:
- `Mô tả`: Mô tả phòng (sau encoding thành nhiều cột)
- `Diện tích`: Kích thước phòng (m²)
- `Vị trí`: Quận/Huyện tại Hà Nội (one-hot encoded)
- `Ngày đăng`: Thời gian đăng bài
- `Người đăng`: ID người đăng
- `Giá phòng`: Mục tiêu dự đoán (giá thuê/tháng)

---

### 2️⃣ Giai Đoạn Training Model (Python)

**File**: `data_mining.ipynb` hoặc `helllo.py`

```
House Price Dataset
      ↓
  Train/Test Split (80-20 hoặc 70-30)
      ↓
  Train 4 Models:
    ├── Linear Regression
    ├── Random Forest (100 trees)
    ├── XGBoost
    └── Neural Network (PyTorch)
      ↓
  Evaluate (MAE, MSE, RMSE, R²)
      ↓
  Save Models (pickle/torch)
```

**Metrics**:
- **MAE** (Mean Absolute Error): Sai lệch trung bình tuyệt đối
- **MSE** (Mean Squared Error): Bình phương sai số
- **RMSE** (Root Mean Squared Error): Căn bậc 2 của MSE
- **R²**: Độ giải thích của mô hình (0-1)

---

### 3️⃣ Backend API (Spring Boot)

**Khởi động**:
```bash
mvn clean install
mvn spring-boot:run
# Server chạy tại http://localhost:8080
```

**Endpoints**:

#### GET `/api/metadata`
- **Mục đích**: Lấy thông tin metadata (vị trí, feature names, v.v)
- **Response**:
```json
{
  "locations": ["Ba Đình", "Hai Bà Trưng", ...],
  "features": ["diện_tích", "vị_trí_ba_đình", ...],
  "modelInfo": "XGBoost v2.0"
}
```

#### POST `/api/predict`
- **Mục đích**: Dự đoán giá phòng
- **Request**:
```json
{
  "diện_tích": 30.5,
  "vị_trí": "Ba Đình",
  "ngày_đăng": "2026-06-04",
  "mô_tả": "Phòng đẹp, thoáng mát"
}
```
- **Response**:
```json
{
  "predicted_price": 4500000,
  "confidence": 0.87,
  "model": "XGBoost"
}
```

---

### 4️⃣ Frontend (React + Vite)

**Khởi động**:
```bash
cd frontend-react
npm install
npm run dev
# Frontend chạy tại http://localhost:5173
```

**Chức năng**:
1. Form nhập thông tin phòng
2. Gọi API `/api/predict` từ backend
3. Hiển thị kết quả dự đoán
4. Visualize dữ liệu (nếu có)

---

## 🚀 Hướng Dẫn Chạy Dự Án

### Bước 1: Setup Python Environment

```powershell
# Mở PowerShell tại thư mục project
cd c:\Users\Admin\Documents\Codex\2026-05-04\b-n-l-m-t-senior

# Tạo virtual environment
python -m venv venv

# Kích hoạt environment
venv\Scripts\activate

# Cài dependencies
pip install -r requirements.txt
```

### Bước 2: Crawl & Prepare Data (Optional - nếu cần dữ liệu mới)

```powershell
# Crawl dữ liệu từ Phongtro123 (5 trang)
python crawling.py --max-pages 5 --sleep 1

# Hoặc sử dụng dữ liệu có sẵn
# (standard_dataset.csv và houseprice_dataset.csv đã có)
```

### Bước 3: Train Models

```powershell
# Chạy toàn bộ pipeline training
python helllo.py

# Hoặc dùng Jupyter để xem chi tiết
jupyter notebook data_mining.ipynb
```

### Bước 4: Chạy Backend (Spring Boot)

```powershell
cd backend-springboot

# Build project
mvn clean install

# Chạy server
mvn spring-boot:run

# Server chạy tại: http://localhost:8080
# Health check: http://localhost:8080/api/metadata
```

### Bước 5: Chạy Frontend (React)

**Mở terminal mới** (giữ backend chạy):

```powershell
cd frontend-react
npm install
npm run dev

# Frontend chạy tại: http://localhost:5173
```

### Bước 6: Test Dự Án

```bash
# Frontend sẽ gọi tới API:
http://localhost:8080/api/predict
```

---

## 🔧 Các Component Chính

### Backend Java Classes

| Class | Vai Trò |
|-------|---------|
| `HousePriceApplication` | Entry point của Spring Boot |
| `PredictionController` | REST endpoints (/api/predict, /api/metadata) |
| `HousePriceModelService` | Logic dự đoán, gọi ML models |
| `PredictionRequest` | DTO nhận dữ liệu từ frontend |
| `PredictionResponse` | DTO trả kết quả dự đoán |
| `MetadataResponse` | DTO trả metadata (locations, features) |
| `WebConfig` | Cấu hình CORS, RestTemplate, v.v |

### Python Components

| Module | Vai Trò |
|--------|---------|
| `crawling.py` | Crawl dữ liệu từ web |
| `data_mining.ipynb` | Pipeline ML (cleaning, encoding, training) |
| `helllo.py` | Script train nhanh toàn bộ models |
| Models | `.pkl` files chứa trained models |

---

## 📊 Luồng Dữ Liệu Chi Tiết

### Từ Web → Dataset

```
1. Crawling: Lấy HTML từ Phongtro123.com
2. Parsing: Extract thông tin phòng bằng BeautifulSoup
3. Cleaning:
   - Xóa rows: missing values, giá ≤ 0, diện tích ≤ 0
   - Chuẩn hóa: định dạng, kiểu dữ liệu
4. Remove Outliers: Z-score > 3 trên diện tích & giá
5. Feature Engineering:
   - One-hot encode: Vị trí (quận/huyện)
   - MinMaxScaler: Scale to [0, 1]
6. Output: houseprice_dataset.csv (ready to train)
```

### Từ Dataset → Mô Hình

```
1. Load data từ houseprice_dataset.csv
2. Split: Train (80%) - Test (20%)
3. Train 4 models:
   - Linear: y = w*X + b
   - RandomForest: Ensemble cây quyết định
   - XGBoost: Gradient boosting
   - NeuralNet: PyTorch (Lớp: input → hidden → output)
4. Evaluate: MAE, MSE, RMSE, R²
5. Save models: pickle hoặc torch.save()
```

### Từ Frontend → Prediction

```
1. User nhập dữ liệu form
2. Frontend gửi POST → /api/predict
3. Backend:
   - Parse request
   - Encode dữ liệu (one-hot, scaling)
   - Load model từ disk
   - Chạy prediction
   - Trả kết quả JSON
4. Frontend hiển thị kết quả
```

---

## ⚙️ Cấu Hình & Tuning

### Backend Configuration

**File**: `application.properties`

```properties
# Server port
server.port=8080

# Logging
logging.level.root=INFO
logging.level.com.hanoi.houseprice=DEBUG

# CORS (nếu cần)
server.servlet.context-path=/

# Model path
model.path=/models/
```

### Frontend Environment

**File**: `vite.config.js`

```javascript
// API endpoint
const API_URL = 'http://localhost:8080/api';

// Build optimization
export default {
  build: {
    target: 'ES2020',
    minify: 'terser'
  }
};
```

---

## 🐛 Troubleshooting

| Vấn đề | Giải pháp |
|--------|-----------|
| Backend không start | Kiểm tra port 8080 có bị chiếm không; mvn clean install |
| Frontend 404 /api/predict | Kiểm tra backend chạy không; CORS có enable không |
| Model không tìm thấy | Kiểm tra model path trong code; retrain model |
| Memory error train model | Giảm batch size, dùng dataset nhỏ hơn |
| venv không activate | Chạy: `venv\Scripts\activate.ps1` (PowerShell) |

---

## 📈 Performance & Optimization

### Python
- **Crawling**: ~1-2 phút/5 trang (tuỳ network)
- **Training**: ~5-10 phút (tuỳ model & dataset size)
- **Prediction**: <100ms per request

### Backend
- **Startup**: ~3-5 giây
- **API Response**: <200ms average
- **Memory**: ~512MB - 1GB

### Frontend
- **Load time**: ~2-3 giây
- **Bundle size**: ~300KB (production)

---

## 🔐 Security Notes

1. **Input Validation**: Backend validate dữ liệu từ frontend
2. **CORS**: Chỉ cho phép requests từ localhost:5173 (dev)
3. **Models**: Stored locally, không upload internet
4. **Data Privacy**: Dữ liệu crawl từ website công khai

---

## 📝 Next Steps & Improvements

- [ ] Thêm unit tests
- [ ] Deploy to cloud (Azure, AWS)
- [ ] Caching prediction results
- [ ] Real-time model update
- [ ] Advanced feature engineering
- [ ] A/B testing different models
- [ ] Monitoring & logging
- [ ] CI/CD pipeline

---

## 📞 Contact & Support

- **Python Issues**: Check `data_mining.ipynb` hoặc `crawling.py`
- **Backend Issues**: Check Spring Boot logs
- **Frontend Issues**: Browser DevTools console
- **Dataset Issues**: Verify `houseprice_dataset.csv` format

---

**Cập nhật**: 2026-06-04
