# 🚫 [DEPRECATED] 台灣股市模擬交易 App (Stock Simulator)

> [!CAUTION]
> ### ⚠️ 本專案已廢除 / THIS REPOSITORY IS DEPRECATED ⚠️
> **本專案（台灣股市模擬交易 App）目前已廢除並停止維護與更新。**  
> **This repository is no longer maintained or updated.**

---

## 📖 專案簡介 (Project Overview)

本專案為基於 **Android Jetpack Compose** 開發之 **台灣股市模擬交易與即時行情系統**，提供全台股即時報價、分時走勢、K 線技術圖表與虛擬資產模擬下單交易功能。

### 🛠️ 主要功能回顧 (Features Overview)
- 📈 **台灣大盤與全台股即時行情**：對接 TWSE MIS 官方即時 API，提供零時差指數與個股即時成交價。
- 📊 **互動式圖表**：包含盤中雙色分時走勢圖（即時對齊開/高/低/收）與專業多天期 K 線圖。
- ⚡ **最佳五檔（Order Book）**：提供個股委買與委賣前五檔即時價量顯示。
- 💰 **虛擬資產與模擬下單**：支援限價單/市價單買賣模擬、交易時間限制邏輯與 VIP 模擬流程。
- 🔍 **全台股即時搜尋**：支援代號與名稱開頭優先度智慧搜尋。

---

## 🛠️ 技術棧 (Tech Stack)
- **UI 框架**：Jetpack Compose (Material 3)
- **架構模式**：MVVM + Clean Architecture / StateFlow
- **網路層**：Retrofit 2 + Gson (TWSE MIS API, FinMind API)
- **本地資料庫**：Room Database
- **非同步並化**：Kotlin Coroutines + Flow

---

## 📌 備註 (Notes)
如需參考程式碼，請留意本專案已停止維護。
