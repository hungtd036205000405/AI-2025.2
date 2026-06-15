# 🎯 Documentation Index - Chỉ Mục Tài Liệu

## 📚 Danh Sách Tài Liệu Dự Án

Dự án này có **4 tài liệu chính** để giúp bạn hiểu hoàn toàn cách hệ thống vận hành:

### 1. 📖 [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md) - **Tài Liệu Chính**
**Độ dài**: ~300 dòng | **Thời gian đọc**: 20-30 phút

**Nội dung**:
- ✅ Tổng quan dự án
- ✅ Kiến trúc 3 tầng (Frontend, Backend, ML)
- ✅ Cấu trúc thư mục chi tiết
- ✅ Quy trình vận hành từng giai đoạn
- ✅ Hướng dẫn chạy dự án (5 bước)
- ✅ Component chính của backend
- ✅ Luồng dữ liệu chi tiết
- ✅ Troubleshooting guide
- ✅ Performance metrics

**👉 Bắt đầu từ đây nếu**: Bạn muốn hiểu **toàn bộ** dự án từ đầu

---

### 2. ⚡ [QUICK_START.md](./QUICK_START.md) - **Hướng Dẫn Nhanh**
**Độ dài**: ~50 dòng | **Thời gian đọc**: 5 phút

**Nội dung**:
- ✅ 5 bước chạy dự án
- ✅ Useful commands
- ✅ Quick troubleshooting
- ✅ File quan trọng

**👉 Dùng khi**: Bạn chỉ muốn **chạy project ngay lập tức**

---

### 3. 🏗️ [ARCHITECTURE.md](./ARCHITECTURE.md) - **Kiến Trúc & Diagram**
**Độ dài**: ~400 dòng | **Thời gian đọc**: 25-35 phút

**Nội dung**:
- ✅ 6 Diagram chi tiết:
  - Tổng quan kiến trúc 3 tầng
  - Data flow (Preparation → Training → Prediction)
  - Request-Response flow
  - Component class diagram
  - Technology stack
  - Deployment architecture
- ✅ Giải thích từng diagram
- ✅ Technology stack summary

**👉 Dùng khi**: Bạn muốn **hiểu kiến trúc qua hình ảnh/diagram**

---

### 4. 📖 [CODE_STRUCTURE.md](./CODE_STRUCTURE.md) - **Chi Tiết Code**
**Độ dài**: ~600 dòng | **Thời gian đọc**: 40-50 phút

**Nội dung**:
- ✅ Directory map chi tiết từng file
- ✅ Giải thích từng key file:
  - PredictionController.java
  - HousePriceModelService.java
  - DTOs (Request/Response)
  - Python data processing
  - React component
- ✅ Code snippets
- ✅ Testing examples
- ✅ Common tasks

**👉 Dùng khi**: Bạn muốn **sâu vào code, hiểu logic chi tiết**

---

### 5. 📋 [README.md](./README.md) - **Original README**
**Nội dung**:
- ✅ Giới thiệu (Vietnamese)
- ✅ Quy trình khai phá dữ liệu
- ✅ Cấu trúc project
- ✅ Cài đặt & chạy commands

**👉 Giữ cho**: Tham khảo nhanh các commands

---

## 🗺️ Roadmap - Hành Trình Học Tập Gợi Ý

### 🟢 Level 1: Beginner - Muốn chạy project (5-10 phút)
```
QUICK_START.md
    ↓
Chạy theo 5 bước
    ↓
✅ Project chạy thành công
```

### 🟡 Level 2: Intermediate - Muốn hiểu hoạt động (30-40 phút)
```
PROJECT_DOCUMENTATION.md
    ↓ (Đọc: Tổng quan + Quy trình + Hướng dẫn)
ARCHITECTURE.md
    ↓ (Xem các diagram)
✅ Hiểu kiến trúc toàn bộ
```

### 🔴 Level 3: Advanced - Muốn sửa/mở rộng code (60+ phút)
```
CODE_STRUCTURE.md
    ↓ (Chi tiết từng file)
PROJECT_DOCUMENTATION.md
    ↓ (Reference thêm)
Thực hành:
  - Thay đổi model
  - Thêm feature
  - Sửa UI
    ↓
✅ Có khả năng phát triển thêm
```

---

## 🎓 Học Từng Phần

### 📊 **Phần 1: Data Processing (Python)**

**Muốn học?** → [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md#-giai-đoạn-chuẩn-bị-dữ-liệu-python)

**File liên quan**:
- `crawling.py` - Crawl dữ liệu từ web
- `data_mining.ipynb` - Notebook xử lý dữ liệu & train model
- `helllo.py` - Script train nhanh

**Topics**:
- Web scraping (BeautifulSoup)
- Data cleaning (Pandas)
- Feature engineering (One-hot, MinMaxScaler)
- Outlier detection (Z-score)

---

### ⚙️ **Phần 2: Backend API (Spring Boot)**

**Muốn học?** → [CODE_STRUCTURE.md](./CODE_STRUCTURE.md#2-predictorcontrollerjava---frontend-entry-point)

**File liên quan**:
- `PredictionController.java` - REST endpoints
- `HousePriceModelService.java` - Business logic
- `DTOs/*` - Request/Response objects
- `WebConfig.java` - Configuration

**Topics**:
- REST API design
- Spring Boot basics
- Dependency injection
- Input validation

---

### 🎨 **Phần 3: Frontend (React)**

**Muốn học?** → [CODE_STRUCTURE.md](./CODE_STRUCTURE.md#5-react-frontend---app-component)

**File liên quan**:
- `frontend-react/src/main.jsx` - Entry point
- `frontend-react/src/App.jsx` - Main component (nếu có)
- `frontend-react/src/styles.css` - Styling

**Topics**:
- React hooks (useState, useEffect)
- Fetch API / Axios
- Form handling
- UI/UX

---

### 🤖 **Phần 4: Machine Learning Models**

**Muốn học?** → [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md#-giai-đoạn-training-model-python)

**Models**:
- Linear Regression
- Random Forest
- XGBoost
- Neural Network (PyTorch)

**Topics**:
- Model training
- Hyperparameter tuning
- Model evaluation (MAE, MSE, RMSE, R²)
- Model selection

---

## 🔄 Luồng Dữ Liệu Cấp Cao

```
Crawl Data (Python)
    ↓
Clean & Process (Python + Pandas)
    ↓
Train Models (Python + Scikit-learn/PyTorch)
    ↓
Save Models (.pkl, .pt files)
    ↓
Load Models in Backend (Spring Boot)
    ↓
Receive Request from Frontend (React)
    ↓
Preprocess Input
    ↓
Predict using Model
    ↓
Return Response
    ↓
Display Result in Frontend
```

---

## ❓ FAQ - Câu Hỏi Thường Gặp

### Q: Tôi nên bắt đầu từ tài liệu nào?

**A**: Tùy mục đích của bạn:
- 🚀 Chỉ muốn chạy: **QUICK_START.md**
- 📚 Muốn hiểu toàn bộ: **PROJECT_DOCUMENTATION.md** → **ARCHITECTURE.md**
- 🔧 Muốn sửa code: **CODE_STRUCTURE.md**

---

### Q: Làm sao để chạy project trong 5 phút?

**A**: Xem [QUICK_START.md](./QUICK_START.md) - chỉ 5 bước!

---

### Q: Mô hình dự đoán ở đâu? Làm sao nó hoạt động?

**A**: 
- **Ở đâu**: Python files (`crawling.py`, `data_mining.ipynb`)
- **Hoạt động**: [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md#-giai-đoạn-training-model-python)
- **Chi tiết**: [CODE_STRUCTURE.md](./CODE_STRUCTURE.md#4-data-processing-pipeline-python)

---

### Q: Backend (Spring Boot) nhận request từ đâu?

**A**: 
- **Từ Frontend** (React browser)
- **Request**: POST `/api/predict` + JSON data
- **Chi tiết**: [CODE_STRUCTURE.md](./CODE_STRUCTURE.md#2-predictorcontrollerjava---frontend-entry-point)

---

### Q: Làm sao frontend biết gửi request đến đâu?

**A**: 
- **URL**: `http://localhost:8080/api/predict`
- **Cứng**: Trong React code, hoặc environment variable
- **Xem**: [CODE_STRUCTURE.md](./CODE_STRUCTURE.md#5-react-frontend---app-component)

---

### Q: Dữ liệu đầu vào là gì?

**A**: 
- **Diện tích** (m²)
- **Vị trí** (Quận/Huyện Hà Nội)
- **Ngày đăng**
- **Mô tả**
- **Chi tiết**: [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md#-quy-trình-vận-hành)

---

### Q: Output là gì?

**A**: 
- **Giá dự đoán** (VND/tháng)
- **Confidence** (độ tin cậy 0-1)
- **Model name** (XGBoost, Random Forest, ...)
- **Timestamp**

---

### Q: Làm sao thêm location mới?

**A**: 
1. Crawl dữ liệu từ location đó (`crawling.py`)
2. Re-train models (`data_mining.ipynb`)
3. Update backend metadata
4. Update frontend dropdown
- **Chi tiết**: [CODE_STRUCTURE.md](./CODE_STRUCTURE.md#add-a-new-location-to-dropdown)

---

### Q: Làm sao thay đổi model (e.g., LightGBM)?

**A**:
1. Train model mới (`data_mining.ipynb`)
2. Save model mới
3. Update backend `HousePriceModelService.predict()`
- **Chi tiết**: [CODE_STRUCTURE.md](./CODE_STRUCTURE.md#change-prediction-model)

---

## 📞 Liên Hệ & Support

- **Lỗi chạy**: [QUICK_START.md - Troubleshooting](./QUICK_START.md#-nếu-lỗi)
- **Lỗi hiểu**: [PROJECT_DOCUMENTATION.md - Troubleshooting](./PROJECT_DOCUMENTATION.md#-troubleshooting)
- **Lỗi code**: [CODE_STRUCTURE.md - Code details](./CODE_STRUCTURE.md)
- **Kiến trúc**: [ARCHITECTURE.md - All diagrams](./ARCHITECTURE.md)

---

## 📊 Documentation Statistics

| Tài Liệu | Dòng | Thời Gian | Độ Khó |
|---------|------|----------|--------|
| QUICK_START.md | ~50 | 5 min | ⭐ |
| PROJECT_DOCUMENTATION.md | ~300 | 25 min | ⭐⭐ |
| ARCHITECTURE.md | ~400 | 30 min | ⭐⭐ |
| CODE_STRUCTURE.md | ~600 | 45 min | ⭐⭐⭐ |
| **Total** | **~1350** | **~100 min** | - |

---

## 🎯 Next Steps

- [ ] Đọc [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md)
- [ ] Xem [ARCHITECTURE.md](./ARCHITECTURE.md) diagrams
- [ ] Chạy theo [QUICK_START.md](./QUICK_START.md)
- [ ] Tìm hiểu code ở [CODE_STRUCTURE.md](./CODE_STRUCTURE.md)
- [ ] Thực hành: Thêm feature / Thay model
- [ ] Deploy đến cloud (Azure, AWS)

---

**Created**: 2026-06-04  
**Tài Liệu Hoàn Chỉnh**: ✅ Tất cả 4 file đã được tạo

Enjoy! 🎉

