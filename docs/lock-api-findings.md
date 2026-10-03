# 云莓智能门锁获取问题排查与修复方案

> 交付物对应任务 t1。结论供 t3（门锁兜底与导入实现）直接落地。

## 0. 结论摘要（TL;DR）

1. **根因是数据层「账号未绑定宿舍」，不是客户端参数/鉴权 bug。**
   `/dormuser/getuserlock` 是「登录用户 → 宿舍 → 门锁」的三表关联查询：只有账号在
   学校侧存在 `dormuser`（宿舍住户）记录时才会返回门锁。账号 `13136112254 / schoolNo 001`
   能登录、学校 token 有效（`/lockpassword/getlockpwdbylockno` 能返回锁密码），但它在
   宿舍住户表里没有绑定记录，所以 `getuserlock` 鉴权通过、查询却返回 `[]`。
2. **`getlockpwdbylockno` 与 `getuserlock` 是两个不同粒度的接口：**
   前者按 `lockNo` 全校查锁密码（不依赖宿舍绑定），后者按「用户→宿舍→锁」查锁。
   所以「锁 `E1:C4:5E:97:49:94` 存在 + 密码能取到」与「getuserlock 为空」并不矛盾。
3. **客户端必须兜底，且兜底已具备大半条件：** 登录拿不到锁时仍保存账号，并引导
   「门锁管理 → 扫码添加 / 手动导入」，复用原版 `lock_info` 分享二维码。这部分逻辑
   （`LoginViewModel.proceedWithSchool`、`LockListViewModel.addFromScan`、`Lock.from`）
   已基本就绪，但有一个**会导致打卡失败的回归点**需要 t3 修掉（见 §5.3）。
4. **发现了可用的「替代自动获取」接口族**（见 §4），可作为可选增强，但不应阻塞兜底方案。

---

## 1. 原生客户端接口链路还原

来源：`yunmei_unintelligent-master`（第一优先级参考实现），PWA / YunmeiX / YunMei_Air /
Yunmei-Auth 的实现完全一致或等价。

### 1.1 登录与学校切换

`YunmeiAPI`（原生 Java）完整流程：

1. `POST https://base.yunmeitech.com/login`，表单 `userName` + `userPwd`(密码的 MD5)。
   响应 `{success, msg, o:{token, userId, userTel, realName}}`。
2. `POST https://base.yunmeitech.com/userschool/getbyuserid`，表单 `userId`，
   头带 `token_data` / `token_userId`。返回学校数组，每项含
   `schoolNo`、`school.schoolName`、`school.serverUrl`、`token`（学校侧 token）。
3. `setSchool`：把 baseUrl 切到 `school.serverUrl`，token 换成学校 token。

### 1.2 获取门锁（问题接口）

`POST {serverUrl}/dormuser/getuserlock`，表单 `schoolNo` + `userId`，
头 `token_data`(学校token) + `token_userId` + `tokenUserId`。

返回裸 JSON 数组，每项字段（原项目映射）：

| 服务端字段 | 原项目字段 | 本工程 `Lock` 字段 | 用途 |
| --- | --- | --- | --- |
| `buildName` + `dormNo` | `label` | `label` | 显示名 `楼栋-寝室` |
| `lockNo` | `D_Mac` / `lockNo` | `mac` / `lockNo` | 锁号（实为 MAC 格式，如 `E1:C4:5E:97:49:94`） |
| `lockCharacterUuid` | `D_CHAR` | `writeUuid` | 写特征 UUID（BLE 写开门帧） |
| `lockServiceUuid` | `D_SERV` | `serviceUuid` | 服务 UUID（BLE 扫描/连接） |
| `lockSecret` | `D_SEC` | `secret` | 开门密钥（BLE 帧的 secret） |
| — | `username` | `usernameMd5` | `MD5(username)`，用于找回已保存账号 |

> 注意区分两个「密码」概念：
> - `lockSecret`（`getuserlock` 返回）→ BLE 开门帧里的 secret，**开门靠它**；
> - `lockPwd`（`/lockpassword/getlockpwdbylockno` 返回）→ 服务器下发的开锁密码（数字码），
>   只依赖 `lockNo`，**不依赖宿舍绑定**。
> 因此「密码能取到」≠「门锁能自动拿到」，两者来源不同。

### 1.3 其它已用接口

- 打卡：`POST {serverUrl}/signrecord/signbyschool`，表单 `schoolNo` + `lockNo` + `location`。
- 开锁密码：`POST {serverUrl}/lockpassword/getlockpwdbylockno`，表单 `lockNo`，响应 `{lockPwd}`。

---

## 2. 根因分析

### 2.1 主因（高置信）：账号无 `dormuser` 绑定

证据链：

1. **未授权探测确认「鉴权门」与「空结果」是两回事。** 直接 `POST`
   `https://xiaoyun.yunmeitech.com/dormuser/getuserlock` 不带 token，返回
   `{"error":true,"msg":"用户验证错误！"}`（HTTP 200）。也就是说：token 无效时是**报错**，
   而任务描述中「登录态下返回 `[]`」说明**鉴权已通过、只是查无数据**。
2. **接口命名语义**：`dormuser`（宿舍住户）+ `getuserlock`（取用户门锁）是典型的
   「用户→宿舍→锁」关联查询。锁 `E1:C4:5E:97:49:94` 存在于学校的锁表（所以
   `getlockpwdbylockno` 能查到密码），但该账号没有对应的宿舍住户记录，JOIN 结果为空。
3. **跨项目旁证**：Yunmei-Auth（PHP，服务端/工具链）里 `dormInfo()` 调用
   `getuserlock` 时**只传 `schoolNo`**（`userId` 走 header `tokenUserId`），说明服务端
   识别用户靠 header 里的 userId，而不是 body 参数——进一步说明空结果不是「漏传 userId」。

### 2.2 已排除的假设

| 假设 | 结论 | 依据 |
| --- | --- | --- |
| 鉴权 header 名写错（`token_data` vs `tokenData`） | 排除 | 同 header 约定下 `getlockpwdbylockno` 能返回密码，说明学校 token 被正确接受 |
| 响应被包装成 `{data:[]}` 导致解析空 | 排除 | 任务明确「返回 `[]`」；且本工程 `extractLocksArray` 已兼容 `data/list/result` 包装 |
| 缺少 `userId` 参数 | 排除（非主因） | 原生与当前客户端都传了 `userId`（body + header），PHP 版只传 header 也能工作 |

### 2.3 一个可能的细分情况（供现场验证）

`dormuser` 表可能以「学生 ID」而非「登录 `o.userId`」为外键。若该账号是家长/管理号、
或学生信息未同步到宿舍住户表，就会出现「能登录、能取锁密码、却取不到锁」的组合。
**验证方法（有 token 时）**：用学校 token 调 §4 里的 `student/getbyloginuser` /
`student/getbyuserid`，看是否能取到 `areaNo/buildNo/dormNo`；再拿这些字段调
`dormuser/getbydorm`。若学生表有宿舍信息而 `dormuser` 表查不到，则实锤是宿舍住户表缺绑定。

---

## 3. 服务器接口探测结果（实测）

对 `https://xiaoyun.yunmeitech.com` 做了无鉴权接口枚举（返回 HTTP 200 且命中鉴权门
`{"error":true,"msg":"用户验证错误！"}` 视为「接口存在」，404 视为不存在）。

**已确认存在的接口：**

| 接口 | 语义推断 | 是否已知被客户端使用 |
| --- | --- | --- |
| `dormuser/getuserlock` | 取当前用户的门锁（问题接口） | 是（原生/当前） |
| `dormuser/getbyloginuser` | 按登录用户取宿舍住户记录 | 否（**新增候选**） |
| `dormuser/getdormbyuser` | 按用户取宿舍 | 否（**新增候选**） |
| `dormuser/getbydorm` | 按宿舍取住户（含锁信息） | 否（**新增候选**） |
| `dormuser/getbybuild` | 按楼栋取住户 | 否 |
| `student/getbyuserid` | 按 userId 取学生（含 areaNo/buildNo/dormNo） | 否（**新增候选**） |
| `student/getbyloginuser` | 按登录用户取学生 | 否（**新增候选**） |
| `student/getbydorm` | 按宿舍取学生 | 否 |
| `student/getbydormloginuser` | 取同寝学生（室友） | 是（Yunmei-Auth `getRoommate`） |
| `roomuser/list` | 房间住户列表 | 否 |
| `lockpassword/getlockpwdbylockno` | 按锁号取开锁密码 | 是 |
| `lockpassword/getlockpwd` | 取开锁密码（别名） | 否 |
| `signrecord/signbyschool` | 按学校打卡 | 是 |

> 参数名为**推断**（无鉴权无法看到参数校验），建议拿到 token 后实测确认：
> - `student/getbyuserid` 大概率：`schoolNo` + `userId`；
> - `student/getbyloginuser` / `dormuser/getbyloginuser` / `dormuser/getdormbyuser` 大概率：
>   `schoolNo`（用户身份走 header `token_userId`/`tokenUserId`）；
> - `dormuser/getbydorm`：`schoolNo` + `dormNo`（可能还要 `areaNo`/`buildNo`）；
> - `dormuser/getbybuild`：`schoolNo` + `buildNo`。
> 这些接口同时接受 GET 与 POST（Spring 风格），与现有 `@FormUrlEncoded POST` 客户端兼容。

---

## 4. 替代自动获取方案（可选增强）

优先顺序（一旦 `getuserlock` 为空，可尝试下述链路把「宿舍→锁」信息补回来）：

```
方案 A（首选探测链）:
  student/getbyloginuser  (schoolNo)  → 得到 areaNo/buildNo/dormNo
  dormuser/getbydorm      (schoolNo, areaNo, buildNo, dormNo) → 得到 lockNo/lockSecret/lock*Uuid
  ↓ 组装成 Lock，字段与 getuserlock 返回一致

方案 B（单接口直取）:
  dormuser/getbyloginuser (schoolNo)  → 若直接返回 lock* 字段则一步到位
  dormuser/getdormbyuser  (schoolNo)  → 备选
```

**注意：** 若根因是「`dormuser` 表缺绑定」，方案 A/B 的 `dormuser` 相关接口大概率同样为空；
只有 `student/*` 表与 `dormuser` 表数据不同步时，方案 A 才有救。因此**这些只能作为
「锦上添花」的自动补全尝试，不能替代扫码/手动导入兜底**。

---

## 5. 客户端修复/兜底方案（t3 落地依据）

### 5.1 必做：登录空锁兜底（现状已部分实现，需确认）

`LoginViewModel.proceedWithSchool` 已经在 `fetchLocks` 返回空时：
- 调用 `saveAccountIfNeeded()` 保存账号；
- 提示「账号已保存，但服务器未返回门锁。请进入 门锁管理 → 扫码添加」。

**t3 需确认/补强：**
- 文案落地到登录页 UI（确认 `LoginScreen` 已渲染 `message`）。
- 保存账号后应能回到首页并进入门锁管理，而非卡死在登录流程。

### 5.2 必做：扫码 / 手动导入（复用原版分享二维码格式）

二维码格式已明确，`Lock.from` 已能解析，需确认 UI 侧扫码入口与手动输入齐备。

**分享体（base64 前）字段顺序，9 段 `|` 分隔：**

```
label | mac | writeUuid | serviceUuid | secret | usernameMd5 | schoolNo | lockNo | DATA_VER
 0       1        2            3           4         5            6          7         8
```

- `mac` = `lockNo`（原项目在未知真实 MAC 时用 lockNo 顶替，BLE 快速连接失败后回退扫描）；
- `writeUuid` = `lockCharacterUuid`（原 `D_CHAR`）；
- `serviceUuid` = `lockServiceUuid`（原 `D_SERV`）；
- `secret` = `lockSecret`（原 `D_SEC`）；
- `DATA_VER` = `"1.2"`。

**链接前缀规则（`Lock.toShareUrl` / `Lock.from` 已一致）：**
- 有 secret：`https://yunmeiui.xypp.cc/#/lock_info/{base64}`；
- 无 secret（NFC 公开体）：`https://yunmeiui.xypp.cc/#/lock_id/{base64}`。

**兼容：** 旧版 4 段格式（无账号信息）补一个 `account` 占位 label，仍可导入，但
`secret` 为空时必须拒绝（`LockListViewModel.addFromScan` 已处理「不含密钥无法添加」）。

**手动导入需收集的最小字段（对标扫码）：**
`label`、`mac`(=lockNo)、`writeUuid`、`serviceUuid`、`secret`、`schoolNo`、`lockNo`；
`usernameMd5` 用当前登录账号的 `MD5(username)` 自动填充。

### 5.3 必做：修复「打卡依赖 getuserlock」的回归（关键）

当前 `YunMeiRepository.sign(user: StoredUser, lock: Lock, location)` 会重新
`fetchLocks(school)` 并断言 `locks.any { it.lockNo == lock.lockNo }`，否则抛
`门锁不存在或已失效`。在「getuserlock 为空、锁为手动导入」的场景下，这一步必然抛错，
**导致导入的锁无法打卡**。

原生 `SignService` 里 `loginAndGetLock()` 的返回值只写进一个从未被校验的
`lockNoConfirm`（历史遗留），实际打卡直接 `signbyschool`，**不依赖 getuserlock**。

**t3 应改为：** `sign(user, lock, location)` 去掉「必须先 fetchLocks 再校验存在」的
前置检查，登录+切学校后直接 `signbyschool`（与原生一致）；或仅在 `getuserlock` 非空时
做校验。打卡接口本身只依赖 `schoolNo + lockNo + location`，不需要门锁列表。

### 5.4 已可用、无需改动的路径

- **BLE 开门**：`UnlockManager` 只用本地 `Lock` 的 `secret/writeUuid/serviceUuid/mac`，
  开门时不再请求服务器，导入的锁只要字段完整即可开门。
- **获取开锁密码**：`getLockPassword(user, lock)` → `getlockpwdbylockno`，只依赖
  `lockNo` + 学校 token，空锁场景可用（用户拿到密码后可用于门锁键盘/管理员开门）。

### 5.5 建议实现顺序（t3）

1. 修 `sign` 的 fetchLocks 前置校验（§5.3）——否则导入的锁打卡必挂。
2. 完善登录空锁兜底与门锁管理扫码/手动导入（§5.1/§5.2）。
3. （可选）在 `fetchLocks` 为空时尝试 §4 方案 A/B 的替代接口自动补全，失败再引导扫码。
4. 全程保持 `assembleDebug` 可编译。

---

## 6. 附：关键源码位置索引

| 主题 | 位置 |
| --- | --- |
| 原生 getuserlock 逻辑 | `yunmei_unintelligent-master/app/src/main/java/cc/xypp/yunmeiui/utils/YunmeiAPI.java` |
| 原生 QR 分享/解析格式 | `yunmei_unintelligent-master/app/src/main/java/cc/xypp/yunmeiui/eneity/Lock.java` |
| 原生打卡（不依赖 getuserlock） | `yunmei_unintelligent-master/app/src/main/java/cc/xypp/yunmeiui/function/SignService.java` |
| PHP 版 dormInfo（只传 schoolNo） | `Yunmei-Auth-main/src/YunmeiAuth.php` |
| 当前接口定义 | `YunMeiAndroid/app/src/main/java/com/yunmei/client/data/network/YunMeiApi.kt` |
| 当前仓库（含 sign 回归点） | `YunMeiAndroid/app/src/main/java/com/yunmei/client/data/repository/YunMeiRepository.kt` |
| 当前登录兜底 | `YunMeiAndroid/app/src/main/java/com/yunmei/client/ui/screens/login/LoginViewModel.kt` |
| 当前扫码导入 | `YunMeiAndroid/app/src/main/java/com/yunmei/client/ui/screens/locks/LockListViewModel.kt` |
| 当前 Lock 模型/二维码 | `YunMeiAndroid/app/src/main/java/com/yunmei/client/data/model/Lock.kt` |
