"""
Main script: run the data-mining workflow quickly from console.

Run:
    python helllo.py
"""

from __future__ import annotations

import os
import sys
import warnings

import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MinMaxScaler
from torch.optim import SGD
from torch.utils.data import DataLoader, Dataset

from crawling import CLEAN_OUTPUT, RAW_OUTPUT, prepare_model_dataset, save_datasets

warnings.filterwarnings("ignore")
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


def load_dataset() -> pd.DataFrame:
    """Load model-ready data, or build it from raw/crawler data."""
    if os.path.exists(CLEAN_OUTPUT):
        df = pd.read_csv(CLEAN_OUTPUT)
        if {"Diện tích", "Giá phòng"}.issubset(df.columns) and len(df) >= 30:
            return df

    if os.path.exists(RAW_OUTPUT):
        raw_df = pd.read_csv(RAW_OUTPUT)
        if {"Diện tích", "Giá phòng", "Vị trí", "Ngày đăng"}.issubset(raw_df.columns):
            df = prepare_model_dataset(raw_df)
            if len(df) >= 30:
                df.to_csv(CLEAN_OUTPUT, index=False, encoding="utf-8-sig")
                return df

    _, df = save_datasets(max_pages=3, sleep_seconds=0.5)
    return df


def rmse_score(y_true, y_pred) -> float:
    return float(np.sqrt(mean_squared_error(y_true, y_pred)))


def evaluate_model(name: str, y_true, y_pred) -> dict:
    return {
        "Model": name,
        "MAE": mean_absolute_error(y_true, y_pred),
        "MSE": mean_squared_error(y_true, y_pred),
        "RMSE": rmse_score(y_true, y_pred),
        "R2": r2_score(y_true, y_pred),
    }


def print_section(title: str) -> None:
    print("\n" + "=" * 70)
    print(title)
    print("=" * 70)


def print_result(result: dict) -> None:
    print(f"MAE  = {result['MAE']:.4f}")
    print(f"MSE  = {result['MSE']:.4f}")
    print(f"RMSE = {result['RMSE']:.4f}")
    print(f"R2   = {result['R2']:.4f}")


class HousePriceDataset(Dataset):
    def __init__(self, X_data, y_data):
        self.X_data = torch.FloatTensor(X_data)
        self.y_data = torch.FloatTensor(y_data).view(-1, 1)

    def __getitem__(self, index):
        return self.X_data[index], self.y_data[index]

    def __len__(self):
        return len(self.X_data)


class NeuralNetworkRegression(nn.Module):
    def __init__(self, input_size: int):
        super().__init__()
        self.fc1 = nn.Linear(input_size, 32)
        self.fc2 = nn.Linear(32, 16)
        self.fc3 = nn.Linear(16, 1)

    def forward(self, x):
        x = torch.relu(self.fc1(x))
        x = torch.relu(self.fc2(x))
        return self.fc3(x)


def train_neural_network(X_train, X_test, y_train, y_test) -> dict:
    device = "cuda" if torch.cuda.is_available() else "cpu"
    train_data = HousePriceDataset(X_train, y_train)
    train_loader = DataLoader(dataset=train_data, batch_size=128, shuffle=True)

    model = NeuralNetworkRegression(input_size=X_train.shape[1]).to(device)
    criterion = nn.MSELoss()
    optimizer = SGD(model.parameters(), lr=0.01)

    model.train()
    for epoch in range(80):
        running_loss = 0.0
        for batch_idx, (samples, labels) in enumerate(train_loader):
            samples = samples.to(device)
            labels = labels.to(device)

            optimizer.zero_grad()
            outputs = model(samples)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            running_loss += loss.item()

        if (epoch + 1) % 20 == 0:
            print(f"Neural Network - Epoch {epoch + 1}, Loss: {running_loss / (batch_idx + 1):.4f}")

    model.eval()
    with torch.no_grad():
        test_tensor = torch.FloatTensor(X_test).to(device)
        predictions = model(test_tensor).cpu().numpy().ravel()

    return evaluate_model("Neural Network Regression", y_test, predictions)


def train_xgboost(X_train, X_test, y_train, y_test) -> dict | None:
    try:
        from xgboost import XGBRegressor
    except ImportError:
        print("Skip XGBoost: xgboost is not installed.")
        return None

    model = XGBRegressor(
        n_estimators=150,
        learning_rate=0.05,
        max_depth=4,
        subsample=0.9,
        colsample_bytree=0.9,
        objective="reg:squarederror",
        random_state=30,
    )
    model.fit(X_train, y_train)
    predictions = model.predict(X_test)
    print("Giá nhà được dự đoán bởi XGBoost:")
    print(predictions[:20])
    return evaluate_model("XGBoost Regressor", y_test, predictions)


def main() -> None:
    df = load_dataset()
    print_section("1. DATA SELECTION + CLEANING")
    print("Tập dữ liệu sau khi được làm sạch:")
    print(df.head())
    print(f"Số dòng: {len(df)} | Số thuộc tính: {len(df.columns)}")

    X = np.array(df.drop(columns=["Giá phòng"]))
    y = np.array(df["Giá phòng"])

    print_section("2. DATA TRANSFORMATION")
    scaler = MinMaxScaler()
    X = scaler.fit_transform(X)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=30)
    print(f"Train rows: {len(X_train)} | Test rows: {len(X_test)}")
    print(f"Số feature đầu vào: {X_train.shape[1]}")

    results = []

    print_section("3.1 LINEAR REGRESSION")
    linear_model = LinearRegression()
    linear_model.fit(X_train, y_train)
    y_predict = linear_model.predict(X_test)
    print("Giá nhà được dự đoán bởi Linear Regression:")
    print(y_predict[:20])
    linear_result = evaluate_model("Linear Regression", y_test, y_predict)
    print_result(linear_result)
    results.append(linear_result)

    print_section("3.2 RANDOM FOREST REGRESSOR")
    rf_model = RandomForestRegressor(n_estimators=150, random_state=30, min_samples_leaf=2)
    rf_model.fit(X_train, y_train)
    rf_predict = rf_model.predict(X_test)
    print("Giá nhà được dự đoán bởi Random Forest:")
    print(rf_predict[:20])
    rf_result = evaluate_model("Random Forest Regressor", y_test, rf_predict)
    print_result(rf_result)
    results.append(rf_result)

    print_section("3.3 XGBOOST REGRESSOR")
    xgb_result = train_xgboost(X_train, X_test, y_train, y_test)
    if xgb_result:
        print_result(xgb_result)
        results.append(xgb_result)

    print_section("3.4 NEURAL NETWORK REGRESSION")
    nn_result = train_neural_network(X_train, X_test, y_train, y_test)
    print_result(nn_result)
    results.append(nn_result)

    result_df = pd.DataFrame(results).sort_values("RMSE")
    print_section("4. SO SÁNH KẾT QUẢ CUỐI CÙNG")
    print(result_df.to_string(index=False, float_format=lambda value: f"{value:.4f}"))


if __name__ == "__main__":
    main()
