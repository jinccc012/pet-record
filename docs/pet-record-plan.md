# 寵物紀錄網站完整實作規劃

> 版本：v0.0.1
> 前端：React + Vite + TypeScript
> 後端：Spring Boot
> 資料庫：Neon PostgreSQL Free
> 檔案儲存：Cloudflare R2
> 後端部署：Oracle Cloud Always Free VM
> 前端部署：Cloudflare Pages
> 檔案上傳：前端圖片壓縮 + Signed URL 直傳 R2
> 適用規模：小型站台
> 目標：所有資料雲端保存，並在控制使用量的前提下盡量長期維持免費與穩定

---

## 1. 專案目標
建立一個寵物紀錄網站，讓使用者可以紀錄狗狗或貓貓的基本資料、日常生活紀錄與健康紀錄

主要功能：
- 首頁顯示狗狗與貓貓兩種圖卡
- 點選狗狗或貓貓後可以新增寵物資料
- 首頁下方顯示已建立的寵物卡片
- 寵物卡片顯示頭像、姓名、年齡、動物類型
- 點選寵物卡片後進入編輯頁
- 寵物編輯頁包含三個分頁：
  - 基本資料
  - 日常生活紀錄
  - 健康紀錄
- 日常紀錄需產生圖表
- 健康紀錄需支援 PDF、圖片與影片附件
- 所有結構化資料存放於 Neon PostgreSQL
- 所有圖片、PDF、影片存放於 Cloudflare R2
- 檔案使用 Signed URL 直傳，避免大型檔案經過 Spring Boot 後端
- 圖片上傳前由前端壓縮，降低 R2 容量與流量風險
- JVM、Hibernate、Jackson 統一使用 UTC
- 前端再依使用者本地時區顯示時間
- 核心資料表使用軟刪除，降低誤刪風險
- Oracle VM 的 Spring Boot 啟動參數需限制 JVM heap，避免小規格 VM 被 OOM Killer 終止

---

## 2. 最終架構

```text
使用者瀏覽器
  |
  | React + Vite + TypeScript
  | 前端圖片壓縮
  v
Cloudflare Pages
  |
  | HTTPS API
  v
Oracle Cloud Always Free VM
  |
  | Nginx + Spring Boot API
  | systemd 保持服務常駐
  v
Neon PostgreSQL Free
  |
  | 結構化資料
  | 允許閒置時自動 scale to zero
  v
Cloudflare R2
  |
  | 圖片 / PDF / 影片
  | 私有 bucket
  | 短效 Signed URL 存取
```

---

## 3. 技術總表

| 分類 | 技術 |
|---|---|
| 前端框架 | React |
| 前端語言 | TypeScript |
| 前端建構工具 | Vite |
| UI 元件庫 | MUI |
| 路由 | React Router |
| API 狀態管理 | TanStack Query |
| HTTP 請求 | Axios |
| 表單處理 | React Hook Form |
| 表單驗證 | Zod |
| 日期處理 | dayjs |
| 圖表 | ECharts |
| 圖片壓縮 | browser-image-compression 或 Canvas API |
| 後端框架 | Spring Boot |
| 後端語言 | Java 21 或 Java 17 |
| ORM | Spring Data JPA / Hibernate |
| API 風格 | RESTful API |
| 安全性 | Spring Security + JWT |
| 資料庫 | Neon PostgreSQL Free |
| 檔案儲存 | Cloudflare R2 |
| 檔案上傳 | Signed URL 直傳 |
| 後端部署 | Oracle Cloud Always Free VM |
| 前端部署 | Cloudflare Pages |
| Web Server | Nginx |
| SSL | Cloudflare SSL 或 Let’s Encrypt |
| 後端執行 | systemd service |
| API 文件 | springdoc-openapi / Swagger |
| 資料庫版控 | Flyway |
| 測試 | JUnit 5 + Mockito |
| Log | Logback |
| 備份 | pg_dump + R2 object key 清單 |

---

## 4. 核心設計原則

### 4.1 前端負責

- 頁面呈現
- 表單輸入
- 表單驗證
- 圖片壓縮
- 呼叫 API
- Signed URL 直傳檔案
- 圖表顯示
- JWT Token 保存

### 4.2 後端負責

- 登入驗證
- JWT 簽發與驗證
- 使用者權限檢查
- 產生 R2 Signed Upload URL
- 產生 R2 Signed View URL
- 檔案 metadata 管理
- 寵物資料 CRUD
- 日常紀錄 CRUD
- 健康紀錄 CRUD
- 圖表資料統計

### 4.3 Neon PostgreSQL 負責

- 使用者資料
- 寵物基本資料
- 日常生活紀錄
- 進食紀錄
- 大便紀錄
- 健康紀錄
- 檔案 metadata
- upload session 狀態

### 4.4 Cloudflare R2 負責

- 寵物頭像
- 大便異常圖片
- 健康報告 PDF
- 健康檢查圖片
- 超音波影片

---

## 5. 免費服務選型

### 5.1 Cloudflare Pages

用途：

- 部署 React 前端。
- 提供 HTTPS。
- 支援自訂網域。
- 適合 SPA 網站。

部署設定：

| 項目 | 設定 |
|---|---|
| Build command | `npm run build` |
| Build output directory | `dist` |

SPA fallback：

建立 `public/_redirects`：

```text
/* /index.html 200
```

---

### 5.2 Oracle Cloud Always Free VM

用途：

- 部署 Spring Boot 後端。
- 執行 Nginx。
- 執行 systemd service。
- 負責 JWT、API、R2 Signed URL、資料庫存取。

建議配置：

| 項目 | 建議 |
|---|---|
| VM | Ampere A1 Always Free |
| OS | Ubuntu 22.04 LTS 或 24.04 LTS |
| Java | Temurin JDK 21 |
| Web Server | Nginx |
| 啟動方式 | systemd |

正確維持穩定方式：

- 使用 systemd 保持 Spring Boot 常駐。
- 使用 Nginx 反向代理。
- 使用 uptime monitor 監控 API 是否可用。
- 定期 `apt update`。
- 設定 log rotation。
- 定期備份設定檔。

---

### 5.3 Neon PostgreSQL Free

用途：

- 儲存結構化資料。
- 儲存檔案 metadata。
- 儲存 upload session 狀態。

重要策略：

- 允許 Neon 閒置時自動 scale to zero。
- 不使用頻繁 DB Health Check 強制防休眠。
- 接受首次查詢可能有短暫冷啟動。
- API health check 預設不查 DB。

原因：

- Neon Free 有使用量限制。
- 頻繁查 DB 可能消耗免費 compute 額度。
- 小型站台可接受冷啟動延遲。

---

### 5.4 Cloudflare R2

用途：

- 儲存圖片、PDF、影片。
- 使用 S3-compatible API。
- Spring Boot 使用 AWS S3 SDK 操作 R2。
- 前端透過 Signed URL 直傳檔案。

Bucket 原則：

- Bucket 不公開。
- 不讓前端持有 R2 金鑰。
- 所有 object key 由後端產生。
- 所有檔案讀取透過後端授權。
- 私密檔案透過短效 signed GET URL 存取。

快取原則：

| 檔案類型 | 快取策略 |
|---|---|
| 頭像 | 可使用 `Cache-Control: public, max-age=31536000, immutable` |
| 健康報告 PDF | 不公開，不做長期公開快取 |
| 健康圖片 | 不公開，透過 signed GET URL 存取 |
| 超音波影片 | 不公開，透過 signed GET URL 存取 |
| 大便異常圖片 | 視為私密資料，透過 signed GET URL 存取 |

---

## 6. 時區統一策略

醫療與日常紀錄對日期非常敏感，尤其是半夜、跨日或未來有跨時區使用者時。

因此本系統採用：

```text
JVM 預設時區：UTC
Hibernate JDBC time zone：UTC
Jackson JSON 序列化時區：UTC
資料庫 timestamp 基準：UTC
前端顯示：dayjs 轉換為使用者本地時間
```

重要原則：

- `created_at`、`updated_at`、`deleted_at` 使用 UTC。
- `record_date` 代表使用者選擇的日常紀錄日期。
- `visit_date` 代表使用者選擇的就醫日期。
- `feeding_time`、`stool_time` 搭配 `record_date` 使用。
- 不要用 `created_at` 反推日常紀錄日期。

### 6.1 PetRecordApplication.java

```java
package com.example.petrecord;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PetRecordApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(PetRecordApplication.class, args);
    }
}
```

### 6.2 application.yml 時區設定

```yaml
spring:
  jackson:
    time-zone: UTC

  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
```

### 6.3 前端顯示策略

- 系統時間：使用 dayjs 轉換為本地時間顯示。
- 業務日期：`record_date`、`visit_date` 直接依使用者選擇日期顯示。

範例問題：

```text
使用者在台灣 23:30 紀錄大便
如果後端時區處理不一致，可能造成日期被誤判成隔天或前一天
```

---

## 7. 資料儲存原則

### 7.1 結構化資料存在 Neon PostgreSQL

例如：

- 使用者帳號
- 寵物基本資料
- 日常紀錄
- 進食紀錄
- 大便紀錄
- 健康紀錄
- 檔案 metadata
- upload session

### 7.2 非結構化檔案存在 Cloudflare R2

例如：

- 寵物頭像圖片
- 大便異常圖片
- 健康報告 PDF
- 健康檢查圖片
- 超音波影片

### 7.3 禁止事項

- 不要把圖片、PDF、影片存進 PostgreSQL。
- 不要把正式檔案存在 Oracle VM 磁碟。
- 不要讓前端直接操作 R2 金鑰。
- 不要讓前端決定 object key。
- 不要使用永久公開 URL 存取私密健康資料。

---

## 8. Signed URL 直傳設計

### 8.1 完整上傳流程

```text
1. React 選擇檔案。
2. 若是圖片，前端先壓縮。
3. React 呼叫 Spring Boot 建立 upload session 並要求 signed upload URL。
4. Spring Boot 驗證 JWT。
5. Spring Boot 檢查使用者是否擁有 petId。
6. Spring Boot 檢查 relatedId 是否屬於該 petId。
7. Spring Boot 檢查 category 是否合法。
8. Spring Boot 檢查 contentType 是否允許。
9. Spring Boot 檢查 fileSize 是否在限制內。
10. Spring Boot 產生 object key。
11. Spring Boot 建立 upload_sessions PENDING 紀錄。
12. Spring Boot 回傳 signed PUT URL。
13. React 使用 PUT 將檔案直接上傳到 R2。
14. React 呼叫 complete-upload API。
15. Spring Boot 使用 HEAD Object 確認 R2 檔案存在。
16. Spring Boot 建立 files metadata。
17. Spring Boot 將 upload_sessions 狀態改為 COMPLETED。
```

### 8.2 下載 / 預覽流程

```text
1. React 要求查看檔案。
2. Spring Boot 驗證 JWT。
3. Spring Boot 檢查使用者是否擁有 fileId。
4. Spring Boot 產生短效 signed GET URL。
5. React 使用 URL 預覽或下載檔案。
```

### 8.3 為什麼需要 upload session？

Signed URL 直傳可能發生：

```text
前端取得 signed URL。
檔案已上傳到 R2。
但 complete-upload API 失敗。
```

這會造成：

```text
R2 有檔案。
DB 沒有 metadata。
產生孤兒檔案。
```

因此需要 upload session 追蹤狀態。

### 8.4 upload session 狀態

| 狀態 | 說明 |
|---|---|
| PENDING | 已產生 signed URL，但尚未確認上傳完成 |
| COMPLETED | 已完成上傳，且已建立 files metadata |
| FAILED | 上傳失敗或確認失敗 |
| EXPIRED | 超過有效時間未完成 |
| DELETED | 檔案已刪除 |

### 8.5 孤兒檔案清理策略

建議：

- 每天執行一次排程。
- 找出超過 24 小時仍為 PENDING 的 upload_sessions。
- 嘗試刪除對應 R2 object。
- 將 upload_sessions 狀態改成 EXPIRED 或 FAILED。

第一版先手動執行清理 API，第二版再做排程。

---

## 9. 前端圖片壓縮策略

### 9.1 需要壓縮的檔案

- 寵物頭像
- 大便異常圖片
- 健康檢查圖片

### 9.2 不建議前端壓縮的檔案

- PDF
- 影片

原因：

- PDF 前端壓縮成本高且效果不一定穩定。
- 影片前端壓縮可能非常耗 CPU，容易造成手機或低階電腦卡住。
- 第一版只限制影片大小，不在前端做影片轉檔。

### 9.3 圖片壓縮建議

| 類型 | 建議最大尺寸 | 建議品質 | 建議大小 |
|---|---:|---:|---:|
| 寵物頭像 | 1024 px | 0.8 | 1 MB 以內 |
| 大便異常圖片 | 1600 px | 0.8 | 2 MB 以內 |
| 健康檢查圖片 | 1600 px | 0.8 | 2 MB 以內 |

### 9.4 前端壓縮套件

```bash
npm install browser-image-compression
```

範例：

```ts
import imageCompression from "browser-image-compression";

export async function compressImage(
  file: File,
  maxSizeMB = 1,
  maxWidthOrHeight = 1600
) {
  return imageCompression(file, {
    maxSizeMB,
    maxWidthOrHeight,
    useWebWorker: true,
    fileType: "image/webp",
  });
}
```

注意：

- 前端壓縮後仍然要由後端檢查 contentType 與 fileSize。
- 不能只相信前端壓縮結果。

---

## 10. 檔案限制建議

| 類型 | 副檔名 | MIME type | 最大大小 |
|---|---|---|---:|
| 寵物頭像 | jpg, jpeg, png, webp | image/jpeg, image/png, image/webp | 5 MB |
| 大便異常圖片 | jpg, jpeg, png, webp | image/jpeg, image/png, image/webp | 10 MB |
| 健康報告 PDF | pdf | application/pdf | 20 MB |
| 健康圖片 | jpg, jpeg, png, webp | image/jpeg, image/png, image/webp | 10 MB |
| 超音波影片 | mp4, mov | video/mp4, video/quicktime | 50 MB 或 100 MB |

若目標是長期免費，第一版建議：

```text
影片上限先設 50 MB。
若使用量穩定，再提高到 100 MB。
```

---

## 11. Cloudflare R2 Bucket 設計

建議 bucket：

```text
pet-record-files
```

Bucket 設定：

- 不公開。
- 不啟用 public bucket access。
- 透過 Spring Boot 產生 signed URL。
- 前端不可直接持有 R2 金鑰。

### 11.1 Object Key 命名規則

```text
avatars/users/{userId}/pets/{petId}/{uuid}.webp

stool-images/users/{userId}/pets/{petId}/daily-records/{dailyRecordId}/{uuid}.webp

health-reports/users/{userId}/pets/{petId}/health-records/{healthRecordId}/reports/{uuid}.pdf

health-images/users/{userId}/pets/{petId}/health-records/{healthRecordId}/images/{uuid}.webp

ultrasound-videos/users/{userId}/pets/{petId}/health-records/{healthRecordId}/videos/{uuid}.{ext}
```

### 11.2 為什麼 object key 要包含 userId？

- 權限檢查更直覺。
- 資料分類清楚。
- 日後匯出使用者資料比較方便。
- 避免不同使用者檔案混在一起。
- 方便清理某使用者資料。

---

## 12. R2 CORS 設定

因為 React 會直接 PUT 到 R2，所以 bucket 需要設定 CORS。

```json
[
  {
    "AllowedOrigins": [
      "http://localhost:5173",
      "https://你的前端網域.pages.dev",
      "https://app.your-domain.com"
    ],
    "AllowedMethods": [
      "GET",
      "PUT",
      "HEAD"
    ],
    "AllowedHeaders": [
      "Content-Type",
      "x-amz-content-sha256",
      "x-amz-date",
      "authorization"
    ],
    "ExposeHeaders": [
      "ETag"
    ],
    "MaxAgeSeconds": 3600
  }
]
```

正式環境建議移除：

```text
http://localhost:5173
```

不要長期設定：

```text
AllowedOrigins: ["*"]
```

---

## 13. 前端頁面規劃

### 13.1 路由

```text
/login
/
 /pets/new?type=DOG
 /pets/new?type=CAT
 /pets/:petId
```

`/pets/:petId` 內部使用 Tabs：

- 基本資料
- 日常生活紀錄
- 健康紀錄

### 13.2 React 專案結構

```text
src
├── api
│   ├── axiosInstance.ts
│   ├── authApi.ts
│   ├── petApi.ts
│   ├── dailyRecordApi.ts
│   ├── healthRecordApi.ts
│   └── fileApi.ts
│
├── components
│   ├── common
│   ├── pet
│   ├── daily
│   └── health
│
├── hooks
│   ├── useAuth.ts
│   ├── usePets.ts
│   ├── useDailyRecords.ts
│   ├── useHealthRecords.ts
│   └── useFileUpload.ts
│
├── layouts
│   └── MainLayout.tsx
│
├── pages
│   ├── LoginPage.tsx
│   ├── HomePage.tsx
│   ├── PetCreatePage.tsx
│   └── PetDetailPage.tsx
│
├── routes
│   └── AppRoutes.tsx
│
├── types
│   ├── auth.ts
│   ├── pet.ts
│   ├── dailyRecord.ts
│   ├── healthRecord.ts
│   ├── file.ts
│   └── upload.ts
│
├── utils
│   ├── dateUtils.ts
│   ├── ageUtils.ts
│   └── imageCompression.ts
│
├── App.tsx
└── main.tsx
```

---

## 14. 前端主要套件

```bash
npm install @mui/material @emotion/react @emotion/styled
npm install @mui/icons-material
npm install react-router-dom
npm install axios
npm install @tanstack/react-query
npm install react-hook-form zod @hookform/resolvers
npm install dayjs
npm install echarts echarts-for-react
npm install browser-image-compression
```

可選：

```bash
npm install react-dropzone
```

---

## 15. 前端環境變數

`.env.development`

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

`.env.production`

```env
VITE_API_BASE_URL=https://api.your-domain.com/api
```

---

## 16. Axios 設定

```ts
import axios from "axios";

export const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
});

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});
```

---

## 17. React Signed URL 上傳流程範例

```ts
import axios from "axios";
import { axiosInstance } from "./axiosInstance";
import { compressImage } from "../utils/imageCompression";

type UploadCategory =
  | "AVATAR"
  | "STOOL_IMAGE"
  | "HEALTH_REPORT"
  | "HEALTH_IMAGE"
  | "ULTRASOUND_VIDEO";

export async function uploadFileBySignedUrl(params: {
  file: File;
  category: UploadCategory;
  petId: number;
  relatedId?: number;
}) {
  const { category, petId, relatedId } = params;
  let file = params.file;

  const shouldCompress =
    category === "AVATAR" ||
    category === "STOOL_IMAGE" ||
    category === "HEALTH_IMAGE";

  if (shouldCompress) {
    file = await compressImage(
      file,
      category === "AVATAR" ? 1 : 2,
      category === "AVATAR" ? 1024 : 1600
    );
  }

  const prepareRes = await axiosInstance.post("/files/signed-upload-url", {
    petId,
    relatedId,
    category,
    originalFilename: file.name,
    contentType: file.type,
    fileSize: file.size,
  });

  const {
    uploadSessionId,
    uploadUrl,
    objectKey,
    bucketName,
    storedFilename,
  } = prepareRes.data;

  await axios.put(uploadUrl, file, {
    headers: {
      "Content-Type": file.type,
    },
  });

  const completeRes = await axiosInstance.post("/files/complete-upload", {
    uploadSessionId,
    petId,
    relatedId,
    category,
    originalFilename: file.name,
    storedFilename,
    bucketName,
    objectKey,
    contentType: file.type,
    fileSize: file.size,
  });

  return completeRes.data;
}
```

---

## 18. 後端 Spring Boot 專案結構

```text
src/main/java/com/example/petrecord
│
├── config
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   ├── CorsConfig.java
│   └── S3ClientConfig.java
│
├── controller
│   ├── AuthController.java
│   ├── PetController.java
│   ├── DailyRecordController.java
│   ├── HealthRecordController.java
│   └── FileController.java
│
├── service
│   ├── AuthService.java
│   ├── PetService.java
│   ├── DailyRecordService.java
│   ├── HealthRecordService.java
│   ├── FileService.java
│   ├── UploadSessionService.java
│   └── R2StorageService.java
│
├── repository
│   ├── UserRepository.java
│   ├── PetRepository.java
│   ├── DailyRecordRepository.java
│   ├── FeedingRecordRepository.java
│   ├── StoolRecordRepository.java
│   ├── HealthRecordRepository.java
│   ├── FileRepository.java
│   └── UploadSessionRepository.java
│
├── entity
│   ├── User.java
│   ├── Pet.java
│   ├── DailyRecord.java
│   ├── FeedingRecord.java
│   ├── StoolRecord.java
│   ├── HealthRecord.java
│   ├── FileResource.java
│   └── UploadSession.java
│
├── dto
│   ├── request
│   └── response
│
├── enums
│   ├── PetType.java
│   ├── UserRole.java
│   ├── FileCategory.java
│   ├── FileStatus.java
│   └── UploadSessionStatus.java
│
├── exception
│   ├── GlobalExceptionHandler.java
│   └── BusinessException.java
│
└── PetRecordApplication.java
```

---

## 19. 後端 Maven 依賴建議

```xml
<dependencies>
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- AWS S3 SDK，用於 Cloudflare R2 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
    </dependency>

    <!-- OpenAPI / Swagger -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>

    <!-- Lombok，可選 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 20. 後端 application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jackson:
    time-zone: UTC

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
        jdbc:
          time_zone: UTC

  flyway:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-minutes: 1440

r2:
  account-id: ${R2_ACCOUNT_ID}
  access-key-id: ${R2_ACCESS_KEY_ID}
  secret-access-key: ${R2_SECRET_ACCESS_KEY}
  bucket-name: ${R2_BUCKET_NAME}
  endpoint: ${R2_ENDPOINT}
  signed-url:
    upload-expire-minutes: 10
    download-expire-minutes: 15
```

健康檢查策略：

| Health Check | 是否查 DB | 用途 |
|---|---:|---|
| liveness | 否 | 確認 Spring Boot 活著 |
| readiness | 可選 | 需要時才檢查 DB，不要高頻率外部呼叫 |

---

## 21. R2 S3 Client 設定概念

```java
@Configuration
public class S3ClientConfig {

    @Bean
    public S3Client s3Client(
            @Value("${r2.endpoint}") String endpoint,
            @Value("${r2.access-key-id}") String accessKeyId,
            @Value("${r2.secret-access-key}") String secretAccessKey
    ) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                accessKeyId,
                secretAccessKey
        );

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();
    }
}
```

若要產生 presigned URL，另外建立 `S3Presigner`。

---

## 22. 後端 API 規劃

### 22.1 Auth API

```http
POST /api/auth/register
POST /api/auth/login
GET  /api/users/me
```

### 22.2 Pet API

```http
GET    /api/pets
POST   /api/pets
GET    /api/pets/{petId}
PUT    /api/pets/{petId}
DELETE /api/pets/{petId}
```

### 22.3 Daily Record API

```http
GET    /api/pets/{petId}/daily-records
POST   /api/pets/{petId}/daily-records
GET    /api/pets/{petId}/daily-records/{recordId}
PUT    /api/pets/{petId}/daily-records/{recordId}
DELETE /api/pets/{petId}/daily-records/{recordId}
```

依日期查詢：

```http
GET /api/pets/{petId}/daily-records?date=2026-05-13
```

### 22.4 Chart API

```http
GET /api/pets/{petId}/daily-records/chart?range=7
GET /api/pets/{petId}/daily-records/chart?range=30
```

Response：

```json
{
  "labels": ["2026-05-01", "2026-05-02"],
  "weight": [5.2, 5.3],
  "waterMl": [300, 280],
  "foodGram": [120, 130]
}
```

### 22.5 Health Record API

```http
GET    /api/pets/{petId}/health-records
POST   /api/pets/{petId}/health-records
GET    /api/pets/{petId}/health-records/{healthRecordId}
PUT    /api/pets/{petId}/health-records/{healthRecordId}
DELETE /api/pets/{petId}/health-records/{healthRecordId}
```

### 22.6 File API

```http
POST   /api/files/signed-upload-url
POST   /api/files/complete-upload
GET    /api/files/{fileId}/signed-view-url
DELETE /api/files/{fileId}
```

---

## 23. File API 詳細設計

### 23.1 要求上傳 URL

```http
POST /api/files/signed-upload-url
```

Request：

```json
{
  "petId": 1,
  "relatedId": 10,
  "category": "HEALTH_REPORT",
  "originalFilename": "blood-report.pdf",
  "contentType": "application/pdf",
  "fileSize": 1048576
}
```

Response：

```json
{
  "uploadSessionId": 200,
  "uploadUrl": "https://...",
  "bucketName": "pet-record-files",
  "objectKey": "health-reports/users/1/pets/1/health-records/10/reports/uuid.pdf",
  "storedFilename": "uuid.pdf",
  "expiresInSeconds": 600
}
```

後端檢查：

- JWT 是否有效。
- 使用者是否擁有 `petId`。
- `category` 是否合法。
- `contentType` 是否允許。
- `fileSize` 是否在限制內。
- `relatedId` 是否屬於該 `petId`。

### 23.2 完成上傳

```http
POST /api/files/complete-upload
```

Request：

```json
{
  "uploadSessionId": 200,
  "petId": 1,
  "relatedId": 10,
  "category": "HEALTH_REPORT",
  "originalFilename": "blood-report.pdf",
  "storedFilename": "uuid.pdf",
  "bucketName": "pet-record-files",
  "objectKey": "health-reports/users/1/pets/1/health-records/10/reports/uuid.pdf",
  "contentType": "application/pdf",
  "fileSize": 1048576
}
```

處理流程：

```text
1. 檢查 uploadSession 是否存在。
2. 檢查 uploadSession 是否屬於目前使用者。
3. 檢查狀態是否為 PENDING。
4. 使用 HEAD Object 確認 R2 object 存在。
5. 建立 files metadata。
6. 建立對應關聯資料，例如 health_record_files。
7. 將 uploadSession 狀態改為 COMPLETED。
```

Response：

```json
{
  "fileId": 100,
  "category": "HEALTH_REPORT",
  "originalFilename": "blood-report.pdf",
  "contentType": "application/pdf",
  "fileSize": 1048576
}
```

### 23.3 取得檔案預覽 URL

```http
GET /api/files/{fileId}/signed-view-url
```

Response：

```json
{
  "url": "https://...",
  "expiresInSeconds": 900
}
```

### 23.4 刪除檔案

```http
DELETE /api/files/{fileId}
```

處理流程：

```text
1. 檢查使用者權限。
2. 刪除 R2 object。
3. 將 files.status 改為 DELETED。
4. 設定 deleted_at。
```

建議使用 soft delete，避免資料關聯錯亂。

---

## 24. 資料庫設計

### 24.1 users

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 24.2 pets

```sql
CREATE TABLE pets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    birth_date DATE,
    avatar_file_id BIGINT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
```

### 24.3 upload_sessions

```sql
CREATE TABLE upload_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    pet_id BIGINT REFERENCES pets(id),
    related_id BIGINT,
    file_category VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    bucket_name VARCHAR(255) NOT NULL,
    object_key TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_upload_sessions_user_id ON upload_sessions(user_id);
CREATE INDEX idx_upload_sessions_status ON upload_sessions(status);
CREATE INDEX idx_upload_sessions_expires_at ON upload_sessions(expires_at);
```

### 24.4 files

```sql
CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    upload_session_id BIGINT REFERENCES upload_sessions(id),
    uploaded_by BIGINT NOT NULL REFERENCES users(id),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    storage_provider VARCHAR(50) NOT NULL,
    bucket_name VARCHAR(255) NOT NULL,
    object_key TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_category VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_files_uploaded_by ON files(uploaded_by);
CREATE INDEX idx_files_category ON files(file_category);
CREATE INDEX idx_files_status ON files(status);
```

### 24.5 daily_records

```sql
CREATE TABLE daily_records (
    id BIGSERIAL PRIMARY KEY,
    pet_id BIGINT NOT NULL REFERENCES pets(id),
    record_date DATE NOT NULL,
    weight_kg NUMERIC(5,2),
    water_ml INTEGER,
    daily_note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uk_daily_record_pet_date UNIQUE (pet_id, record_date)
);
```

### 24.6 feeding_records

```sql
CREATE TABLE feeding_records (
    id BIGSERIAL PRIMARY KEY,
    daily_record_id BIGINT NOT NULL REFERENCES daily_records(id) ON DELETE CASCADE,
    feeding_time TIME NOT NULL,
    food_gram INTEGER,
    condition_text VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 24.7 stool_records

```sql
CREATE TABLE stool_records (
    id BIGSERIAL PRIMARY KEY,
    daily_record_id BIGINT NOT NULL REFERENCES daily_records(id) ON DELETE CASCADE,
    stool_time TIME NOT NULL,
    condition_text VARCHAR(500),
    abnormal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 24.8 stool_record_files

```sql
CREATE TABLE stool_record_files (
    id BIGSERIAL PRIMARY KEY,
    stool_record_id BIGINT NOT NULL REFERENCES stool_records(id) ON DELETE CASCADE,
    file_id BIGINT NOT NULL REFERENCES files(id)
);
```

### 24.9 health_records

```sql
CREATE TABLE health_records (
    id BIGSERIAL PRIMARY KEY,
    pet_id BIGINT NOT NULL REFERENCES pets(id),
    visit_date DATE NOT NULL,
    hospital_name VARCHAR(255),
    doctor_name VARCHAR(255),
    medical_note VARCHAR(5000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
```

### 24.10 health_record_files

```sql
CREATE TABLE health_record_files (
    id BIGSERIAL PRIMARY KEY,
    health_record_id BIGINT NOT NULL REFERENCES health_records(id) ON DELETE CASCADE,
    file_id BIGINT NOT NULL REFERENCES files(id)
);
```

### 24.11 軟刪除查詢原則

核心資料表使用 `deleted_at` 進行軟刪除。

套用資料表：

- `pets`
- `daily_records`
- `health_records`
- `files`

刪除時不直接 `DELETE`，而是：

```sql
UPDATE pets
SET deleted_at = CURRENT_TIMESTAMP
WHERE id = :petId;
```

一般查詢時必須加上：

```sql
WHERE deleted_at IS NULL
```

注意事項：

- 刪除寵物時，第一版建議只標記 `pets.deleted_at`。
- 不要立刻刪除 `daily_records`、`health_records`、`files`。
- 刪除健康紀錄時，第一版建議只標記 `health_records.deleted_at`。
- 附件 `files` 可保留 ACTIVE，待確認後再刪除或改為 DELETED。
- 真正清除資料可做成管理員維護功能。

---

## 25. 圖表設計

圖表資料來源：

- `daily_records.weight_kg`
- `daily_records.water_ml`
- `feeding_records.food_gram` 加總

需要三種圖表：

| 圖表 | 資料來源 | 類型 |
|---|---|---|
| 體重 | `daily_records.weight_kg` | 折線圖 |
| 喝水量 | `daily_records.water_ml` | 折線圖或長條圖 |
| 進食量 | `SUM(feeding_records.food_gram)` | 長條圖 |

時間範圍：

- 近 7 天
- 近 30 天

SQL 查詢概念：

```sql
SELECT
    dr.record_date,
    dr.weight_kg,
    dr.water_ml,
    COALESCE(SUM(fr.food_gram), 0) AS total_food_gram
FROM daily_records dr
LEFT JOIN feeding_records fr ON fr.daily_record_id = dr.id
WHERE dr.pet_id = :petId
  AND dr.record_date BETWEEN :startDate AND :endDate
  AND dr.deleted_at IS NULL
GROUP BY dr.id, dr.record_date, dr.weight_kg, dr.water_ml
ORDER BY dr.record_date;
```

---

## 26. 權限設計

每個 API 都必須檢查：

- 目前登入使用者是否擁有該 `petId`。
- 目前登入使用者是否擁有該 `dailyRecordId`。
- 目前登入使用者是否擁有該 `healthRecordId`。
- 目前登入使用者是否擁有該 `fileId`。
- 目前登入使用者是否擁有該 `uploadSessionId`。

不要只靠前端隱藏按鈕。

後端必須做權限驗證。

---

## 27. 安全性設計

### 27.1 JWT 儲存策略

第一版可先使用 localStorage 儲存 accessToken，實作簡單：

```text
登入成功後回傳 accessToken
前端存 localStorage
每次 API 請求帶 Authorization: Bearer token
```

正式版建議改用 httpOnly Cookie，但必須注意網域限制。

正確正式網域範例：

```text
前端：app.your-domain.com
後端：api.your-domain.com
Cookie Domain：.your-domain.com
```

Cookie 建議設定：

```text
HttpOnly=true
Secure=true
SameSite=Lax
Domain=.your-domain.com
Path=/
```

不建議正式版使用：

```text
前端：xxx.pages.dev
後端：api.your-domain.com
```

原因：

```text
xxx.pages.dev 與 your-domain.com 是不同主網域。
現代瀏覽器會將這種情境視為第三方 Cookie。
Safari 與未來瀏覽器可能阻擋第三方 Cookie 寫入或傳送。
```

因此，如果正式版要使用 httpOnly Cookie：

```text
Cloudflare Pages 必須設定自訂網域。
前端與後端必須共用同一個主網域。
例如 app.your-domain.com + api.your-domain.com。
```

### 27.2 密碼

- 使用 BCrypt。
- 不可明文保存密碼。
- 不可將 `password_hash` 回傳到前端。

### 27.3 檔案安全

後端需要檢查：

- 副檔名
- contentType
- fileSize
- category
- 使用者權限
- relatedId 是否合法
- objectKey 是否由後端產生

不可讓前端自訂 object key。

### 27.4 Signed URL

建議有效時間：

| URL 類型 | 建議有效時間 |
|---|---|
| 上傳 URL | 5 到 10 分鐘 |
| 預覽 URL | 5 到 15 分鐘 |

不要產生永久 URL。

### 27.5 CORS

後端 CORS 只允許：

- `http://localhost:5173`
- `https://你的前端網域.pages.dev`
- `https://app.your-domain.com`

R2 CORS 也只允許上述前端網域。

---

## 28. Oracle VM 部署規劃

### 28.1 VM 安裝項目

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk nginx git unzip
```

### 28.2 建立後端目錄

```bash
sudo mkdir -p /opt/pet-record-api
sudo chown -R $USER:$USER /opt/pet-record-api
```

### 28.3 上傳 jar

```bash
scp target/pet-record-api.jar ubuntu@your-server:/opt/pet-record-api/app.jar
```

### 28.4 建立環境變數檔

```bash
sudo nano /opt/pet-record-api/.env
```

內容：

```env
DB_URL=jdbc:postgresql://your-neon-host/your-db?sslmode=require
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password
JWT_SECRET=your-long-random-secret

R2_ACCOUNT_ID=your-account-id
R2_ACCESS_KEY_ID=your-access-key-id
R2_SECRET_ACCESS_KEY=your-secret-access-key
R2_BUCKET_NAME=pet-record-files
R2_ENDPOINT=https://your-account-id.r2.cloudflarestorage.com
```

### 28.5 systemd service

```ini
[Unit]
Description=Pet Record Spring Boot API
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/opt/pet-record-api
EnvironmentFile=/opt/pet-record-api/.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/pet-record-api/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

儲存為：

```bash
sudo nano /etc/systemd/system/pet-record-api.service
```

啟動：

```bash
sudo systemctl daemon-reload
sudo systemctl enable pet-record-api
sudo systemctl start pet-record-api
sudo systemctl status pet-record-api
```

查看 log：

```bash
journalctl -u pet-record-api -f
```

### 28.6 JVM 記憶體限制建議

Oracle Always Free VM 的規格可能不同。若初期建立的是小規格 VM，Spring Boot 沒有限制 heap 時，可能會因為瞬間記憶體使用過高而被 Linux OOM Killer 終止。

建議 systemd 的 `ExecStart` 明確限制 JVM heap：

```ini
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/pet-record-api/app.jar
```

不同 VM 規格可參考：

| VM RAM | 建議 JVM 參數 |
|---:|---|
| 1 GB | `-Xms256m -Xmx512m` |
| 2 GB | `-Xms256m -Xmx1024m` |
| 4 GB | `-Xms512m -Xmx1536m` |
| 8 GB 以上 | 依實際監控結果調整 |

注意：

- `-Xmx` 不要設定得太接近 VM 總記憶體。
- 要保留空間給 OS、Nginx、JVM Metaspace、thread stack、native memory。
- 若使用 1GB RAM VM，建議從 `-Xmx512m` 開始。

---

## 29. Nginx 設定

```nginx
server {
    listen 80;
    server_name api.your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

若使用 Cloudflare DNS，可使用 Cloudflare SSL。

也可以使用 Let’s Encrypt：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.your-domain.com
```

---

## 30. Cloudflare Pages 部署

### 30.1 建立 React 專案

```bash
npm create vite@latest pet-record-frontend -- --template react-ts
cd pet-record-frontend
npm install
```

### 30.2 Build 設定

```text
Build command:
npm run build

Build output directory:
dist
```

### 30.3 SPA fallback

建立：

```text
public/_redirects
```

內容：

```text
/* /index.html 200
```

### 30.4 正式版自訂網域

如果正式版使用 httpOnly Cookie，Cloudflare Pages 必須設定自訂網域。

建議：

```text
前端：app.your-domain.com
後端：api.your-domain.com
```

不建議正式版長期使用：

```text
前端：xxx.pages.dev
後端：api.your-domain.com
```

---

## 31. 開發順序建議

### 第 1 階段：專案初始化

- 建立 Spring Boot 專案。
- 建立 React 專案。
- 建立 Neon PostgreSQL。
- 建立 Cloudflare R2 bucket。
- 建立 Oracle VM。
- 設定 R2 CORS。

### 第 2 階段：後端基礎

- 設定 DB 連線。
- 設定 UTC 時區策略。
- 設定 Flyway。
- 建立 `users` 表。
- 建立登入 / 註冊。
- 建立 JWT 驗證。
- 建立全域錯誤處理。

### 第 3 階段：寵物基本資料

- 建立 `pets` 表。
- 實作寵物新增。
- 實作寵物列表。
- 實作寵物編輯。
- 實作寵物軟刪除。
- React 完成首頁與寵物基本資料頁。

### 第 4 階段：R2 Signed URL 上傳

- 建立 `upload_sessions` 表。
- 建立 `files` 表。
- 設定 R2 S3 Client。
- 實作 signed upload URL API。
- 實作 complete upload API。
- 實作 signed view URL API。
- React 實作圖片壓縮。
- React 實作頭像直傳 R2。
- 實作 PENDING upload session 清理機制。

### 第 5 階段：日常生活紀錄

- 建立 `daily_records`。
- 建立 `feeding_records`。
- 建立 `stool_records`。
- 建立 `stool_record_files`。
- 實作日常紀錄 CRUD。
- 實作日常紀錄軟刪除。
- 實作大便異常圖片上傳。
- React 完成日常紀錄表單。

### 第 6 階段：圖表

- 建立 chart API。
- 實作近 7 天資料。
- 實作近 30 天資料。
- React 使用 ECharts 顯示圖表。

### 第 7 階段：健康紀錄

- 建立 `health_records`。
- 建立 `health_record_files`。
- 實作健康紀錄 CRUD。
- 實作健康紀錄軟刪除。
- 實作 PDF / 圖片 / 影片上傳。
- React 完成健康紀錄列表與表單。

### 第 8 階段：部署

- Spring Boot 打包 jar。
- 部署到 Oracle VM。
- 設定 JVM heap 限制。
- 設定 systemd。
- 設定 Nginx。
- 部署 React 到 Cloudflare Pages。
- 設定正式網域。
- 設定 CORS。
- 完整測試。

---

## 32. MVP 最小可行版本

第一版建議完成：

- 登入
- 首頁
- 新增寵物
- 寵物列表
- 基本資料編輯
- 寵物軟刪除
- 頭像圖片壓縮
- 頭像 Signed URL 上傳
- 日常紀錄新增 / 編輯 / 軟刪除
- 體重、喝水、進食圖表
- 健康紀錄新增 / 編輯 / 軟刪除
- 健康附件 Signed URL 上傳

第二版再做：

- 管理員後台
- 匯出 PDF
- 回診提醒
- LINE 通知
- 圖片壓縮參數設定
- 影片壓縮或轉檔
- 批次下載
- 資料匯出
- 孤兒檔案自動排程清理

---

## 33. 免費額度風險與控制

### 33.1 Neon PostgreSQL

風險：

- 資料庫容量超過免費額度。
- 頻繁 DB Health Check 消耗 compute 額度。
- 長期大量資料造成查詢變慢。

控制方式：

- 不把檔案放資料庫。
- 不使用頻繁 DB Health Check 防休眠。
- 定期清理測試資料。
- 建立必要索引。
- 文字欄位限制長度。
- 接受冷啟動。

### 33.2 Cloudflare R2

風險：

- 影片太多導致超過免費儲存額度。
- 讀寫操作過多。
- 孤兒檔案累積。

控制方式：

- 限制影片大小。
- 限制單次上傳數量。
- 圖片壓縮。
- 不公開 bucket。
- 使用短效 signed URL。
- 設計 upload_sessions。
- 定期清理 PENDING / EXPIRED 上傳。

### 33.3 Oracle VM

風險：

- 需要自己維護 Linux。
- VM 資源有限。
- Oracle Always Free 資源有時較難建立。
- 過度閒置可能被回收。
- 小規格 VM 可能發生 OOM。

控制方式：

- 使用 systemd 自動重啟。
- 使用 uptime monitor 監控 API。
- 定期 `apt update`。
- 設定 log rotation。
- 限制 JVM heap。
- 只部署後端 API。
- 不要在 VM 裡存正式檔案。
- 不要把 PostgreSQL 裝在 VM 裡當主資料庫。
- 不要用無意義方式消耗 CPU 或流量。

### 33.4 Cloudflare Pages

風險：

- build 次數過多。
- 前端流量異常增加。
- 正式版 Cookie 網域設定錯誤。

控制方式：

- 小型專案通常不用擔心。
- 不要把大型檔案放在前端 repo。
- 不要把私密資料寫進前端環境變數。
- 正式版使用自訂網域，例如 `app.your-domain.com`。

---

## 34. 備份策略

### 34.1 PostgreSQL 備份

```bash
pg_dump "postgresql://user:password@host/db?sslmode=require" > backup.sql
```

建議：

- 每週匯出一次。
- 重要版本開發前匯出一次。
- 備份檔加密後放到本機或 R2。

### 34.2 R2 檔案備份

R2 本身是雲端物件儲存，但若資料很重要，仍建議備份：

- `files` 表
- `upload_sessions` 表
- R2 object key 清單
- 重要健康報告原始檔

---

## 35. 本版新增重點

本版相較於前一版，額外補上以下正式上線前的重要設計：

### 35.1 時區統一

- JVM、Hibernate、Jackson 統一使用 UTC。
- 前端使用 dayjs 轉換為本地時間顯示。
- `record_date`、`visit_date` 作為業務日期，不靠 `created_at` 反推。

### 35.2 軟刪除

- `pets`、`daily_records`、`health_records`、`files` 補上 `deleted_at`。
- 誤刪時可從資料庫手動救回。

### 35.3 Cookie 網域安全

正式版若使用 httpOnly Cookie，前端與後端必須共用同一個主網域。

建議：

```text
app.your-domain.com
api.your-domain.com
```

### 35.4 JVM 記憶體限制

Oracle VM 的 systemd 啟動指令加入：

```text
-Xms256m -Xmx512m
```

降低小規格 VM 發生 OOM 的風險。

---

## 36. 最終定案

本專案最終採用：

- React + Vite + TypeScript
- MUI
- React Router
- TanStack Query
- Axios
- ECharts
- 前端圖片壓縮
- Spring Boot
- Spring Security + JWT
- Spring Data JPA
- Flyway
- Neon PostgreSQL Free
- Cloudflare R2
- R2 Signed URL 直傳
- upload_sessions 防孤兒檔案
- Oracle Cloud Always Free VM
- Cloudflare Pages
- Nginx
- systemd
- UTC 時區統一
- 軟刪除
- JVM heap 限制
- 正式版自訂網域 Cookie 策略

這套方案的優點：

- 長期免費可行性高。
- 保留 Spring Boot。
- 保留 PostgreSQL。
- 檔案在雲端。
- 資料在雲端。
- 支援圖片 / PDF / 影片。
- 支援 Signed URL 直傳。
- 避免大型檔案經過後端。
- 圖片上傳前壓縮，降低容量風險。
- 適合 10 人左右使用。

最大注意事項：

- 不要把檔案存進資料庫。
- 不要把正式檔案存進 VM。
- 不要讓前端決定 object key。
- 不要讓 Signed URL 有效時間太長。
- 不要頻繁打 Neon DB Health Check。
- 不要用無意義資源消耗防 Oracle idle。
- R2 immutable cache 只用在頭像等可快取檔案。
- 健康資料與影片使用短效 signed URL。
- 要控制影片大小。
- 要定期備份資料庫。
- 要清理 PENDING / EXPIRED upload session。
- JVM / Hibernate / Jackson 統一使用 UTC。
- 正式版若使用 httpOnly Cookie，前後端需共用同一個主網域。
- Oracle VM 需限制 JVM heap，避免 OOM。

---

## 37. 官方參考資料

免費額度與限制可能調整，正式上線前請再次確認官方頁面。

- Oracle Cloud Free Tier / Always Free  
  https://www.oracle.com/cloud/free/  
  https://docs.oracle.com/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm

- Cloudflare Pages  
  https://pages.cloudflare.com/

- Cloudflare R2 Pricing  
  https://developers.cloudflare.com/r2/pricing/

- Cloudflare R2 S3-compatible API  
  https://developers.cloudflare.com/r2/

- Neon Pricing / Plans  
  https://neon.com/pricing  
  https://neon.com/docs/introduction/plans

- Spring Boot  
  https://spring.io/projects/spring-boot

- React  
  https://react.dev/

- Vite  
  https://vite.dev/
