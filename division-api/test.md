# 根因系统测试场景

## 测试场景汇总表

| # | 完整链接 | 错误类型 | 调用深度 | 根因层 |
|---|---------|---------|---------|--------|
| 1 | `http://localhost:8080/api/root-cause/null-pointer` | **NullPointerException** | 3层 | DAO返回null |
| 2 | `http://localhost:8080/api/root-cause/db-duplicate-key` | **BusinessException → DataAccessException** | 2层 | DAO主键冲突 |
| 3 | `http://localhost:8080/api/root-cause/external-timeout` | **BusinessException → ExternalServiceException** | 3层 | 外部风控超时 |
| 4 | `http://localhost:8080/api/root-cause/db-join-error` | **BusinessException → DA → ArrayIndexOutOfBounds** | 3层 | DAO关联查询越界 |
| 5 | `http://localhost:8080/api/root-cause/payment-chain-error` | **BusinessException → Ext → ArithmeticException** | 3层 | 库存计算除零 |
| 6 | `http://localhost:8080/api/root-cause/deep-calc-error` | **BusinessException → ArithmeticException** | 5层 | `step2Transform(y=0)` |
| 7 | `http://localhost:8080/api/root-cause/batch-partial-fail` | **BusinessException** | 2层 | 第2条订单不存在 |
| 8 | `http://localhost:8080/api/root-cause/illegal-state?orderId=ORDER_002` | **IllegalStateException** | 2层 | 状态机校验 |
| 9 | `http://localhost:8080/api/root-cause/db-connection-fail` | **BusinessException → DA → SQLException** | 2层 | DAO SQL连接失败 |
| 10 | `http://localhost:8080/api/root-cause/data-corrupted` | **BusinessException** | 2层 | ORDER_003字段损坏 |
| 11 | `http://localhost:8080/api/root-cause/number-format` | **RuntimeException → NumberFormatException** | 1层 | 解析`"abc123"`失败 |
| 12 | `http://localhost:8080/api/root-cause/unknown-mode` | **RuntimeException → IllegalArgumentException** | 4层 | `step3ApplyMode(MAGIC)` |
| 13 | `http://localhost:8080/api/root-cause/param-out-of-range` | **IllegalArgumentException** | 2层 | score=150超出[0,100] |
| 14 | `http://localhost:8080/api/root-cause/array-index` | **ArrayIndexOutOfBoundsException** | 1层 | 访问data[10] |
| 15 | `http://localhost:8080/api/root-cause/cascading-error` | **BusinessException → ArithmeticException** | 5层 | Reciprocal负数开方 |
| 16 | `http://localhost:8080/api/divide?a=100&b=0` | **IllegalArgumentException** | 2层 | 原除零接口 |

## curl 一键测试所有场景

```bash
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/null-pointer
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/db-duplicate-key
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/external-timeout
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/db-join-error
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/payment-chain-error
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/deep-calc-error
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/batch-partial-fail
curl -s -w "\n%{http_code}\n" 'http://localhost:8080/api/root-cause/illegal-state?orderId=ORDER_002'
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/db-connection-fail
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/data-corrupted
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/number-format
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/unknown-mode
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/param-out-of-range
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/array-index
curl -s -w "\n%{http_code}\n" http://localhost:8080/api/root-cause/cascading-error
curl -s -w "\n%{http_code}\n" 'http://localhost:8080/api/divide?a=100&b=0'
```

## PowerShell 一键测试所有场景

```powershell
$tests = @(
  "/api/root-cause/null-pointer",
  "/api/root-cause/db-duplicate-key",
  "/api/root-cause/external-timeout",
  "/api/root-cause/db-join-error",
  "/api/root-cause/payment-chain-error",
  "/api/root-cause/deep-calc-error",
  "/api/root-cause/batch-partial-fail",
  "/api/root-cause/illegal-state?orderId=ORDER_002",
  "/api/root-cause/db-connection-fail",
  "/api/root-cause/data-corrupted",
  "/api/root-cause/number-format",
  "/api/root-cause/unknown-mode",
  "/api/root-cause/param-out-of-range",
  "/api/root-cause/array-index",
  "/api/root-cause/cascading-error",
  "/api/divide?a=100&b=0"
)
foreach ($t in $tests) {
  try {
    $r = Invoke-WebRequest -Uri "http://localhost:8080$t" -UseBasicParsing -TimeoutSec 3
    Write-Host "OK  $t -> $($r.StatusCode) traceId=$($r.Headers['X-Trace-Id'])"
  } catch {
    Write-Host "ERR $t -> $($_.Exception.Response.StatusCode.value__) traceId=$($_.Exception.Response.Headers['X-Trace-Id'])"
  }
}
```

## 日志文件

`d:/git/myproject/division-api/app.log`

每个错误响应都包含 `X-Trace-Id` 响应头 + `exceptionChain` / `rootCause` 字段，可用于根因分析系统测试。
