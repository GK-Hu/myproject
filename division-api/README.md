# Division API

一個簡單的 Spring Boot 除法計算 API。

## 啟動

```bash
cd division-api
mvn spring-boot:run
```

服務預設在 `http://localhost:8080` 啟動。

## API 使用

### 除法計算

```
GET /api/divide?a={被除數}&b={除數}
```

**參數：**

| 參數 | 類型   | 說明     |
|------|--------|----------|
| a    | double | 被除數   |
| b    | double | 除數     |

**成功回應 (200)：**

```json
{
    "dividend": 10.0,
    "divisor": 2.0,
    "result": 5.0
}
```

**錯誤回應 (400)：**

```json
{
    "error": "Invalid input",
    "message": "除數不可為零"
}
```

## 範例

```bash
# 10 / 2 = 5
curl "http://localhost:8080/api/divide?a=10&b=2"

# 7.5 / 2.5 = 3.0
curl "http://localhost:8080/api/divide?a=7.5&b=2.5"

# 除數為 0 會回傳錯誤
curl "http://localhost:8080/api/divide?a=10&b=0"
```
