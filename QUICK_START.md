# ⚡ Quick Start Guide - Hướng Dẫn Nhanh

## 🎯 Chạy dự án trong 5 bước

### 1. Setup Python

```powershell
cd c:\Users\Admin\Documents\Codex\2026-05-04\b-n-l-m-t-senior
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

### 2. Train Models (Nếu chưa có)

```powershell
python helllo.py
# Hoặc: jupyter notebook data_mining.ipynb
```

### 3. Chạy Backend Spring Boot

```powershell
cd backend-springboot
mvn clean install
mvn spring-boot:run
# Chờ: Started HousePriceApplication in ... seconds
```

**Kiểm tra backend**:
```
http://localhost:8080/api/metadata
```

### 4. Chạy Frontend (Terminal mới)

```powershell
cd frontend-react
npm install
npm run dev
```

**Mở browser**:
```
http://localhost:5173
```

### 5. Test Dự Đoán

Nhập thông tin phòng → Click "Predict" → Xem kết quả

---

## 📂 File Quan Trọng

| File | Mục đích |
|------|----------|
| `houseprice_dataset.csv` | Dữ liệu sẵn sàng train |
| `helllo.py` | Train models nhanh |
| `backend-springboot/pom.xml` | Backend config |
| `frontend-react/package.json` | Frontend config |

---

## 🔧 Useful Commands

```powershell
# Python
python helllo.py                          # Train all models
python crawling.py --max-pages 5          # Crawl new data

# Backend
mvn clean                                 # Clean build
mvn test                                  # Run tests
mvn spring-boot:run                       # Run server

# Frontend
npm install                               # Install packages
npm run dev                                # Start dev server
npm run build                              # Build for production
```

---

## ❌ Nếu Lỗi

**Backend không start?**
- Kiểm tra port 8080: `netstat -ano | findstr :8080`
- Giết process: `taskkill /PID <PID> /F`

**Frontend 404?**
- Kiểm tra backend chạy: `http://localhost:8080/api/metadata`
- Refresh browser

**Python import error?**
- Kiểm tra venv activate: `(venv)` phải hiện ở terminal
- Reinstall: `pip install -r requirements.txt --force-reinstall`

---

💡 **Tài liệu chi tiết**: Xem `PROJECT_DOCUMENTATION.md`
