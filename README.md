# Harumi Pet Record

寵物健康紀錄管理平台，支援寵物基本資料管理、日常生活紀錄（進食／排便）與健康就醫紀錄，並提供體重、喝水量與進食量圖表。檔案附件（頭像、健康報告 PDF、影片）透過 Cloudflare R2 Signed URL 直傳，不經過後端伺服器。

**Production：** https://app.harumipetrecord.com

---

## 功能特色

- **使用者認證** — 註冊、登入，JWT Bearer Token，各使用者資料完全隔離
- **寵物管理** — 建立、編輯、列表、軟刪除，支援頭像上傳
- **日常生活紀錄** — 每日體重、喝水量、進食紀錄、排便紀錄，每日唯一約束（軟刪除後可重建）
- **數據圖表** — 體重 / 喝水量 / 進食量折線與長條圖，支援自訂日期範圍
- **健康紀錄** — 就醫日期、醫院、醫師、醫療備註，支援多種附件（PDF / 圖片 / 影片）
- **R2 直傳** — 前端壓縮圖片後取得 Signed PUT URL，直接上傳至 Cloudflare R2，後端僅做 metadata 管理

---

## 系統架構

```
瀏覽器（React + Vite）
    │  HTTPS
    ▼
Cloudflare Workers 靜態資產          ← 前端 SPA
app.harumipetrecord.com
    │  HTTPS API
    ▼
Cloudflare Worker 反向代理
api.harumipetrecord.com
    │
    ▼
Google Cloud Run（asia-southeast1）  ← Spring Boot API（scale-to-zero）
    │                    │
    ▼                    ▼
Neon PostgreSQL       Cloudflare R2
（Singapore）          （私有 Bucket）
結構化資料            圖片 / PDF / 影片
```

---

## 技術堆疊

### 後端

| 項目 | 版本 / 技術 |
|---|---|
| 語言 | Java 21 |
| 框架 | Spring Boot 4.1.0-SNAPSHOT |
| 安全性 | Spring Security + JJWT 0.12.6 |
| 資料存取 | Spring Data JPA（Hibernate）|
| 資料庫版控 | Flyway |
| 資料庫 | PostgreSQL 17（Neon）|
| 檔案儲存 | Cloudflare R2（AWS SDK v2 S3-compatible）|
| 建構工具 | Maven Wrapper（mvnw）|

### 前端

| 項目 | 版本 / 技術 |
|---|---|
| 語言 | TypeScript |
| 框架 | React + Vite 8 |
| UI 元件庫 | MUI（Material UI）|
| 路由 | React Router |
| 伺服器狀態 | TanStack Query |
| HTTP | Axios |
| 表單 | React Hook Form + Zod |
| 圖表 | ECharts（echarts-for-react）|
| 圖片壓縮 | browser-image-compression |
| 日期 | dayjs |

---

## 本機開發

### 環境需求

| 工具 | 版本 |
|---|---|
| Java | 21 |
| Node.js | 20 以上 |
| PostgreSQL | 本機執行，供測試與開發用 |
| Maven | 已內建 Wrapper（`backend/mvnw`），不需另外安裝 |

### 第一步：Clone 專案

```bash
git clone <repo-url>
cd pet-record
```

### 第二步：建立後端本機設定

後端使用 `application-local.yaml` 作為本機 profile，此檔案已加入 `.gitignore`，不會提交。

建立 `backend/src/main/resources/application-local.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pet_record
    username: your_db_user
    password: your_db_password

app:
  jwt:
    secret: your-256-bit-base64-secret   # 建議至少 32 bytes 的隨機字串

r2:
  account-id: your-r2-account-id
  access-key-id: your-r2-access-key-id
  secret-access-key: your-r2-secret-access-key
  bucket-name: your-bucket-name
  endpoint: https://your-account-id.r2.cloudflarestorage.com
  upload-expire-minutes: 10
  download-expire-minutes: 15
```

### 第三步：建立前端環境變數

建立 `frontend/.env.development.local`：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

### 第四步：啟動服務

**Windows 一鍵啟動（開啟兩個獨立視窗）：**

```bat
.\start-local-servers.bat
```

**手動啟動後端：**

```bash
cd backend
./mvnw spring-boot:run        # Linux / macOS
.\mvnw.cmd spring-boot:run    # Windows
```

**手動啟動前端：**

```bash
cd frontend
npm install
npm run dev
```

啟動後：

| 服務 | 位址 |
|---|---|
| 前端 | http://localhost:5173 |
| 後端 API | http://localhost:8080 |
| 健康檢查 | http://localhost:8080/actuator/health |

---

## 環境變數說明

### 後端（`application-local.yaml` 或 Cloud Run 環境變數）

| 變數 | 說明 |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL（production 使用；local 設定直接寫在 yaml）|
| `DB_USERNAME` | 資料庫帳號 |
| `DB_PASSWORD` | 資料庫密碼 |
| `JWT_SECRET` | JWT 簽署金鑰（256-bit Base64）|
| `R2_ACCOUNT_ID` | Cloudflare Account ID |
| `R2_ACCESS_KEY_ID` | R2 API Token Access Key |
| `R2_SECRET_ACCESS_KEY` | R2 API Token Secret Key |
| `R2_BUCKET_NAME` | R2 Bucket 名稱 |
| `R2_ENDPOINT` | R2 S3 Endpoint URL |

### 前端

| 變數 | 說明 |
|---|---|
| `VITE_API_BASE_URL` | 後端 API Base URL（例：`https://api.harumipetrecord.com/api`）|

---

## 測試

後端測試使用本機 PostgreSQL `pet_record_test` 資料庫（對應 `application-test.yaml`）。

```bash
cd backend
./mvnw test        # Linux / macOS
.\mvnw.cmd test    # Windows
```

前端型別檢查與 lint：

```bash
cd frontend
npm run build    # 執行 tsc + vite build
npm run lint
```

---

## 資料庫 Migration

Migration 腳本位於 `backend/src/main/resources/db/migration/`，由 Flyway 在啟動時自動執行。

| Migration | 內容 |
|---|---|
| V1 | `users` 資料表 |
| V2 | `pets` 資料表 |
| V3 | `daily_records`、`feeding_records`、`stool_records` |
| V4 | `files`、`upload_sessions`，以及 `pets.avatar_file_id` |
| V5 | `health_records`、`health_record_files` |

---

## 專案結構

```
pet-record/
├── backend/                         # Spring Boot 後端
│   └── src/
│       ├── main/java/com/harumi/petrecord/
│       │   ├── auth/                # 登入、註冊、JWT
│       │   ├── config/              # Security、CORS、JWT 設定
│       │   ├── common/exception/    # 全域錯誤處理
│       │   ├── dailyrecord/         # 日常紀錄（進食、排便、圖表）
│       │   ├── file/                # R2 上傳、upload session、Signed URL
│       │   ├── healthrecord/        # 健康紀錄與附件
│       │   ├── pet/                 # 寵物 CRUD
│       │   ├── security/            # JwtFilter、CurrentUser
│       │   └── user/                # 使用者資料
│       └── main/resources/
│           ├── application.yaml
│           ├── application-local.yaml    # 本機設定（gitignored）
│           └── db/migration/            # Flyway scripts
│
├── frontend/                        # React + Vite 前端
│   └── src/
│       ├── api/                     # Axios API 呼叫
│       ├── components/              # UI 元件（common、pet、daily、health、file）
│       ├── hooks/                   # TanStack Query hooks
│       ├── pages/                   # 頁面元件
│       ├── routes/                  # AppRoutes、ProtectedRoute
│       ├── types/                   # TypeScript 型別
│       └── utils/                   # 檔案上傳、圖片壓縮、年齡計算
│
├── docs/
│   └── pet-record-plan.md           # 完整技術規格文件
│
├── start-local-servers.bat          # Windows 一鍵啟動腳本
└── README.md
```

---

## 部署架構

| 元件 | 服務 | 說明 |
|---|---|---|
| 前端 | Cloudflare Workers 靜態資產 | SPA fallback via `not_found_handling` |
| API 代理 | Cloudflare Worker | `api.harumipetrecord.com` → Cloud Run |
| 後端 | Google Cloud Run（asia-southeast1）| 容器化，scale-to-zero |
| 資料庫 | Neon PostgreSQL（ap-southeast-1）| `main`（prod）/ `dev` 兩個 branch |
| 檔案儲存 | Cloudflare R2 | 私有 Bucket，Signed URL 存取 |
| DNS / SSL | Cloudflare | 自動 HTTPS 憑證 |

詳細規格與設計決策請見 [`docs/pet-record-plan.md`](docs/pet-record-plan.md)。
