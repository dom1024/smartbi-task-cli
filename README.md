# smartbi-task-cli

极薄命令行工具：通过 Smartbi **Java SDK** 触发已存在的**计划**（`executeSchedule` / 服务端 `ScheduleSDK.run`）或**内部任务**（`executeTask` / `runTaskById`），用于调度系统或脚本集成。

## 依赖 JAR 来源

从已部署的 **`smartbi.war`** 中复制（路径：`WEB-INF/lib/`），放入本仓库 **`lib/`** 目录：

| 文件 | 说明 |
|------|------|
| `smartbi-SDK.jar` | 客户端 SDK（`ClientConnector`、`ScheduleTaskService`） |
| `smartbi-Common.jar` | SDK 依赖 |
| `smartbi-ScheduleTask.jar` | 与计划任务模块一致的最小拷贝（打包进可执行 jar） |
| `log4j-api-2.18.0-xlibs.jar` | SDK 静态初始化依赖（内含 `smartbixlibs` 重定位 Log4j） |
| `log4j-core-2.18.0-xlibs.jar` | 同上 |
| `log4j-slf4j-impl-2.18.0-xlibs.jar` | 日志实现（与 war 中 xlibs 版本一致） |
| `slf4j-api-2.0.0-xlibs.jar` | SLF4J API |
| `jackson-core-2.13.4-xlibs.jar` | SDK JSON（`smartbixlibs` 重定位 Jackson） |
| `jackson-databind-2.13.4.2-xlibs.jar` | 同上 |
| `jackson-annotations-2.13.4-xlibs.jar` | 同上 |

示例（在包含 `smartbi.war` 的目录执行）：

```bash
mkdir -p lib
unzip -jo smartbi.war WEB-INF/lib/smartbi-SDK.jar \
  WEB-INF/lib/smartbi-Common.jar \
  WEB-INF/lib/smartbi-ScheduleTask.jar \
  WEB-INF/lib/log4j-api-2.18.0-xlibs.jar \
  WEB-INF/lib/log4j-core-2.18.0-xlibs.jar \
  WEB-INF/lib/log4j-slf4j-impl-2.18.0-xlibs.jar \
  WEB-INF/lib/slf4j-api-2.0.0-xlibs.jar \
  WEB-INF/lib/jackson-core-2.13.4-xlibs.jar \
  WEB-INF/lib/jackson-databind-2.13.4.2-xlibs.jar \
  WEB-INF/lib/jackson-annotations-2.13.4-xlibs.jar \
  -d lib
```

若 **`mvn compile`** 或 **`java -jar`** 出现 **`ClassNotFoundException` / `NoClassDefFoundError`**，请根据缺失类名从同一 `WEB-INF/lib` 再拷贝对应 jar 到 `lib/`，并在 **`pom.xml`** 中增加对应的 `system` 依赖（保持最小集，不要整包拷贝 war）。

## 日志与 stdout

本工具在 `src/main/resources/log4j2.xml` 中将 **Root logger 设为 OFF**，避免 Smartbi SDK 向 **stdout** 打印日志，从而保证 **stdout 仅一行 JSON**（满足调度解析）。未捕获异常时，堆栈会先经 **`SensitiveSanitizer`** 再写入 **stderr**；`connector.close()` 失败时同样只向 **stderr** 输出脱敏后的堆栈。

若需要排查连接问题，可暂时删除或修改该 `log4j2.xml` 后重新打包（注意 stdout 可能被日志污染）。

## 构建

要求：**JDK 8 或 11**（与 Smartbi 运行环境一致即可）、**Maven 3.6+**。

```bash
mvn -q clean package
```

可执行（胖）jar 输出路径：

```text
target/smartbi-task-cli.jar
```

## 环境变量

| 变量 | 说明 |
|------|------|
| `SMARTBI_URL` | Smartbi 根地址，例如 `http://host:port/smartbi` |
| `SMARTBI_USERNAME` | 登录用户名 |
| `SMARTBI_PASSWORD` | 登录密码 |

**不要在日志或脚本里回显密码。** 本工具 stdout 仅输出单行 JSON；stderr 仅在异常或 `close` 失败时输出**脱敏后**的堆栈，不会原样打印密码、Cookie、Token。

## 运行

### 首选：按计划 ID 触发（与计划任务界面常见 ID 一致）

**`scheduleId`** 对应 Smartbi **计划**层面的立即执行（`executeSchedule` → `ScheduleSDK.run`）。从「计划任务」界面拿到的 ID 多数情况下是 **`scheduleId`**，请优先使用本命令：

```bash
export SMARTBI_URL='http://localhost:18080/smartbi'
export SMARTBI_USERNAME='admin'
export SMARTBI_PASSWORD='******'

java -jar target/smartbi-task-cli.jar run --schedule-id <scheduleId>
```

### 兼容：按内部任务 ID 触发

**`taskId`** 为系统内部**任务** ID（`executeTask` → `ScheduleSDK.runTaskById`）。仅当你明确持有内部 `taskId`、或需要与旧脚本对齐时使用：

```bash
java -jar target/smartbi-task-cli.jar run-task --task-id <taskId>
```

### 排错说明

若使用 **`run-task --task-id`** 时服务端出现 **`null pointer`** 等异常，而界面上的 ID 实际是计划维度，请改用 **`run --schedule-id`**。

### 诊断命令（仅排查，勿用于长期调度）

当 **`run --schedule-id`** 显示已提交但后台未见执行时，可用本命令查看与 `ScheduleTaskService.executeSchedule` 等价的 **`ClientConnector.remoteInvoke("ScheduleSDK", "run", …)`** 所返回的 **`InvokeResult`** 原始字段（`isSucceed`、`getResult()`、`getOriginalResult()` 的字符串形式）。**`result` / `originalResult` 会先经 `SensitiveSanitizer` 再写入 stdout**；异常堆栈同样经脱敏后写入 **stderr**。

**不建议**在调度系统、定时任务中长期依赖本命令；正式触发请仍使用 **`run --schedule-id`**。

```bash
java -jar target/smartbi-task-cli.jar debug-run-schedule --schedule-id <scheduleId>
```

成功拿到 `InvokeResult` 时，stdout 单行示例（字段以实际为准）：

```json
{"success":true,"command":"debug-run-schedule","idType":"scheduleId","id":"xxx","isSucceed":true,"result":"...","originalResult":"...","message":"diagnostic complete"}
```

- **`success`**：是否已成功执行到 `remoteInvoke` 并得到 `InvokeResult`（未登录失败、未抛异常）。
- **`isSucceed`**：SDK 对本次远程调用的成功标志（与正式 `run` 使用的判定一致）。
- 退出码：`success` 为 `true` 时 **0**；登录失败或 `remoteInvoke` 抛错时 **1**；参数/环境错误仍为 **2**。

说明：`executeSchedule` / `executeTask` 返回的 `true` 仅表示 **远程调用成功**（`InvokeResult.isSucceed()`），**不保证**报表等业务已全部成功；业务结果请在 Smartbi 后台或任务日志中查看。

## 标准输出（单行 JSON）

成功（计划）：

```json
{"success":true,"idType":"scheduleId","id":"xxx","message":"submitted"}
```

成功（任务）：

```json
{"success":true,"idType":"taskId","id":"xxx","message":"submitted"}
```

失败（同样单行，含 `idType` / `id`，解析失败时可能为空字符串）：

```json
{"success":false,"idType":"scheduleId","id":"xxx","message":"失败原因"}
```

`message` 为英文固定短语或脱敏后的异常摘要。

**`debug-run-schedule`** 另含 **`command`**、**`isSucceed`**、**`result`**、**`originalResult`** 字段，见上文「诊断命令」。

## 退出码

| 码 | 含义 |
|----|------|
| 0 | 已提交：登录成功且 `executeSchedule` 或 `executeTask` 返回 `true`；**或** `debug-run-schedule` 已成功得到 `InvokeResult` |
| 1 | Smartbi 侧失败：登录失败、远程调用返回 `false`，或未捕获异常；**或** `debug-run-schedule` 登录失败 / `remoteInvoke` 抛错 |
| 2 | 参数非法，或必需环境变量缺失 |

## 调度系统调用示例

```bash
#!/usr/bin/env bash
set -euo pipefail
export SMARTBI_URL="http://smartbi.example.com/smartbi"
export SMARTBI_USERNAME="scheduler_user"
export SMARTBI_PASSWORD="${SMARTBI_PASSWORD:?set in secret store}"

# With `set -e`, a failing command inside $(...) would exit the script before `CODE=$?`.
set +e
OUT=$(java -jar /opt/smartbi-task-cli/smartbi-task-cli.jar run --schedule-id "$SCHEDULE_ID")
CODE=$?
set -e
echo "$OUT"
exit "$CODE"
```

解析 JSON 时只读一行 stdout 即可（例如 `jq -c .`）。

## 功能边界

- 仅触发已有计划或任务；不创建、修改、删除、禁用计划或任务。
- **`debug-run-schedule`** 仅用于读取远程调用结果与排障，不作为生产调度接口。
- 不实现 `getTaskLog`、任务列表、Web 服务或 Python 包装。

## 许可证

Smartbi 相关 jar 版权归厂商所有；本仓库示例代码按项目约定使用。
