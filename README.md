# Data Mining - House Price Prediction

Project khai phá dữ liệu và dự đoán giá thuê phòng/nhà trọ Việt Nam từ [Phongtro123.com](https://phongtro123.com).

## Quy trình khai phá dữ liệu

1. **Data Selection**: crawl dữ liệu từ Phongtro123 bằng `crawling.py`.
2. **Data Cleaning**: làm sạch `Diện tích`, `Vị trí`, `Ngày đăng`, `Giá phòng`.
3. **Remove Outlier**: loại điểm ngoại biên bằng Z-score trên `Diện tích` và `Giá phòng`.
4. **Data Transformation**: one-hot encoding `Vị trí`, chuẩn hóa input bằng `MinMaxScaler`.
5. **Data Mining & Evaluation**: train/test split và đánh giá model bằng MAE, MSE, RMSE, R2.

## Cấu trúc project

- `crawling.py`: crawl dữ liệu, cleaning, encoding, remove outlier.
- `standard_dataset.csv`: dữ liệu thô với cột tiếng Việt: `Mô tả`, `Diện tích`, `Vị trí`, `Ngày đăng`, `Người đăng`, `Đường link`, `Giá phòng`.
- `houseprice_dataset.csv`: dữ liệu đã làm sạch, one-hot encode và sẵn sàng train.
- `data_mining.ipynb`: notebook trình bày quy trình Data Mining.
- `helllo.py`: file main chạy nhanh toàn bộ pipeline training.
- `requirements.txt`: thư viện cần cài.

## Cài đặt

```powershell
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

## Crawl dữ liệu

```powershell
python crawling.py --max-pages 5 --sleep 1
```

Nếu môi trường không có mạng hoặc website đổi HTML, crawler tự tạo sample dataset để code vẫn chạy được.

## Train nhanh

```powershell
python helllo.py
```

Các model được giữ trong project:

- Linear Regression
- Random Forest Regressor
- XGBoost Regressor
- Neural Network Regression bằng PyTorch

Console sẽ tách riêng từng phần thực thi:

- `3.1 Linear Regression`
- `3.2 Random Forest Regressor`
- `3.3 XGBoost Regressor`
- `3.4 Neural Network Regression`

Mỗi phần in dự đoán/metric riêng. Phần cuối mới in bảng so sánh tổng hợp MAE/MSE/RMSE/R2.
