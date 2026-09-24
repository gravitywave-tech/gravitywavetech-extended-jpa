# Java代码开发规范（基于Claude Code与Google Java Style Guide）

## 项目概述
本规范为使用Claude Code进行Java开发的团队提供统一、专业的协作标准，基于Google Java Style Guide结合现代Java开发实践制定。适用于所有Java项目，特别是基于微服务架构的复杂系统。核心目标是通过标准化代码风格、开发流程和协作模式，提升团队生产力、代码质量和项目可维护性。

### 技术栈
- **JDK版本**: Java 25 (LTS)
- **构建工具**: Maven
- **核心框架**: Spring Boot 4.1.x
- **核心库**: Lombok, MapStruct, Resilience4j, AWS SDK v2
- **测试框架**: JUnit 5, Mockito, AssertJ, Testcontainers

---

## 核心理念
1. **一致性优于完美**: 在所有代码中保持统一的风格和结构，是团队协作的基石
2. **可读性是第一要务**: 代码是写给人看的，机器只是顺便执行
3. **拥抱现代工具与实践**: 结合AI助手，使用Lombok、Optional、Stream API等现代Java特性简化代码
4. **领域驱动与架构清晰**: 采用DDD和六边形架构，确保代码可扩展性和业务逻辑纯粹性
5. **质量内建与自动化**: 通过高标准测试、自动化审查和CI/CD流程，在合并前保障代码质量

---

## 代码风格规范（基于Google Java Style Guide）

### 1. 源文件基础
#### 文件编码与换行符
- 所有Java源文件必须使用**UTF-8编码**
- 统一使用**LF（\n）**作为行结束符
- 严禁使用制表符（Tab）缩进，必须使用空格
- 禁止除缩进空格外的其他空白字符（垂直制表符、换页符等）

#### 特殊字符处理
- 特殊字符（\b, \t, \n, \f, \r, \", \', \\）必须使用预定义转义序列，不使用八进制或Unicode转义
- 非ASCII字符优先使用实际Unicode字符（如"μs"），仅不可打印字符使用转义序列并添加注释

### 2. 源文件结构
一个标准Java源文件按以下顺序组织，各部分之间用一个空行分隔：

1. **许可证信息**: 文件最顶端（如Apache License 2.0）
2. **包声明**: package语句，不换行，全部小写，遵循反向域名约定（如`com.company.project.module`）
3. **导入语句**:
   - 严禁使用通配符导入（*）
   - 单个导入语句不换行
   - 先静态导入，后非静态导入
   - 每组内按ASCII码顺序排序
4. **类声明**: 每个源文件仅包含一个顶层类，文件名与类名完全一致

### 3. 格式化规范
#### 大括号使用
- 控制结构（if/else/for/do/while）即使只有一条语句也必须使用大括号
- 遵循K&R风格：左大括号与声明同行，右大括号独占一行（后接else/while除外）

#### 缩进与行长度
- **缩进**: 每次进入新代码块缩进**2个空格**
- **行长度**: 每行不超过100个字符，最多不超过120个字符（包声明和导入语句除外）

#### 换行规则
- 换行在运算符（.、+、,）之后，而非之前
- 方法调用链换行时，后续调用缩进2个空格
- 函数参数换行时每个参数独占一行，与第一个参数对齐

#### 空白规范
- 关键字（if/for/catch）与左括号之间加一个空格
- 方法名与左括号之间不加空格
- 二元运算符两侧加空格
- 逗号后加空格，前不加空格
- 类型转换右括号后加空格

### 4. 命名规范
| 元素类型 | 命名风格 | 示例 |
|---------|---------|------|
| 包名 | 全小写，反向域名 | `com.company.ecommerce.order.service` |
| 类、接口、枚举 | PascalCase（大驼峰） | `UserProfile`, `OrderService`, `PaymentStatus` |
| 方法 | camelCase（小驼峰），动词开头 | `getUserName()`, `processOrder()`, `isValid()` |
| 变量、参数 | camelCase，名词/名词短语 | `userName`, `orderList`, `maxRetryCount` |
| 静态常量 | UPPER_SNAKE_CASE（全大写下划线分隔） | `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT_MS` |
| 数据库字段 | snake_case（小写下划线分隔） | `user_id`, `created_at`, `order_status` |
| Java实体字段 | camelCase，通过@Column注解映射数据库字段 | `userId`, `createdAt`, `orderStatus` |

> 说明：接口不使用I前缀；抽象类可使用Abstract作为前缀；布尔方法以is/has开头；测试方法可使用should开头的描述性短语。

### 5. Javadoc规范
- 格式：以`/**`开始，`*/`结束，每行以`*`开头并对齐
- 标签顺序：@param → @return → @throws → @since → @deprecated
- 公共类、接口、方法、公共/受保护字段必须有Javadoc
- 内容要求：简洁摘要开头，说明"是什么/为什么/如何使用"，不重复代码逻辑
- 方法Javadoc需说明功能、参数含义、返回值、异常触发条件

---

## 现代Java编程实践

### 1. 语言特性应用
#### Lombok使用规范
✅ **推荐使用**:
- `@Data`: 简单DTO/VO数据载体
- `@Builder`: 创建复杂对象的构建器模式，类型安全可读性高
- `@RequiredArgsConstructor`: 生成包含final字段和@NonNull字段的构造函数，用于依赖注入和不可变对象

❌ **避免使用**:
- `@AllArgsConstructor`: 易导致兼容性问题，构造函数参数过多难以使用

#### Optional使用原则
- 方法可能不返回结果时，**必须**返回`Optional<T>`替代null
- 调用者必须使用`isPresent()/ifPresent()/orElse()/map()/flatMap()`显式处理
- Optional不作为类字段（不可序列化、增加内存开销），也不用于方法参数

```java
// 推荐写法
public Optional<User> findUserById(Long id) {
    return Optional.ofNullable(userRepository.findById(id));
}
```

#### Stream API最佳实践
- 鼓励使用Stream API进行集合的过滤、转换、排序操作，声明式风格提升可读性
- 优先使用方法引用（如`User::getName`）替代lambda表达式
- 合理组合中间操作和终端操作，注意惰性求值特性
- 简单迭代且性能优先场景保留传统for循环

### 2. 架构与设计原则
#### 领域驱动设计（DDD）
- 划分限界上下文（Bounded Context），定义独立领域模型和通用语言
- 区分实体（有唯一标识）和值对象（无唯一标识）
- 通过聚合根管理聚合内部一致性
- 领域逻辑不属于实体/值对象时封装为领域服务
- 通过仓储（Repository）实现领域模型与数据访问层解耦

#### 六边形架构（端口与适配器）
- 核心业务逻辑位于中心，外部依赖通过端口和适配器交互
- 优势：可测试性高（Mock外部依赖）、可替换性强（更换实现不影响核心逻辑）、边界清晰

#### 事件驱动通信
- 微服务间通过事件驱动实现松耦合
- 服务发布事件，感兴趣的服务订阅处理
- 优势：松耦合、可扩展、容错性高（消息队列缓存事件）
- 推荐框架：Spring Cloud Stream、Apache Kafka

### 3. 错误处理与日志记录
#### 自定义异常
- 根据业务领域创建语义化自定义异常，类名以Exception结尾（如`InsufficientStockException`）
- 提供多构造函数：无参、接收错误消息、接收Throwable cause（用于异常链传递）
- 可预期且调用者应处理的错误使用受检异常；编程错误/不可恢复错误使用非受检异常

#### 全局异常处理
- 使用`@RestControllerAdvice`实现全局异常处理，禁止在Controller中重复编写try-catch
- 通过`@ExceptionHandler`分类型处理异常，统一转换为标准化错误响应
- 集中处理日志记录、告警通知，减少代码冗余，保证响应格式一致

#### 错误响应标准
- 遵循**RFC 7807** (Problem Details for HTTP APIs)标准，包含字段：
  - `type`: 错误类型文档URI
  - `title`: 错误简短摘要
  - `status`: HTTP状态码
  - `detail`: 详细错误说明
  - `instance`: 错误发生的资源URI

```json
{
  "type": "https://api.example.com/errors/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Order with ID 12345 could not be found.",
  "instance": "/api/orders/12345"
}
```

#### 日志记录规范
- 日志框架：统一使用SLF4J作为API，Logback/Log4j2作为实现
- 日志级别正确使用：
  - ERROR: 严重问题，可能导致功能中断
  - WARN: 潜在问题或非预期状态，系统可继续运行
  - INFO: 重要业务事件或系统状态变更
  - DEBUG: 详细调试信息，开发测试环境开启
  - TRACE: 最详细信息，深度排查使用
- 分布式系统必须记录Trace ID/Correlation ID实现链路追踪
- **严禁**在日志中记录敏感信息（密码、信用卡号、个人身份信息等）

---

## 开发流程与团队协作

### 1. 分支管理策略
#### 主分支保护
- 主分支（main/master）代码必须始终处于可部署状态
- 禁止直接推送到主分支，所有变更必须通过PR/MR合并
- 分支保护规则：至少1人审查批准、通过所有CI/CD质量门、合并前保持与主分支同步

#### 分支命名规范
- 新功能开发: `feature/功能描述` (如`feature/user-authentication`)
- 缺陷修复: `bugfix/问题描述` (如`bugfix/login-page-error`)
- 生产热修复: `hotfix/修复描述` (如`hotfix/disable-broken-feature`)
- 实验性工作: `experiment/`或`spike/`前缀
- 合并使用Squash and Merge或Rebase and Merge，保持主分支历史整洁
- 合并完成后及时删除已合并分支

### 2. 代码审查（Code Review）
#### 审查流程
1. 开发者在功能分支完成开发和本地测试
2. 推送分支并创建PR，清晰说明变更目的、主要内容和注意点
3. 自动触发CI/CD流水线，通过所有自动化检查后进入人工审查
4. 至少1名团队成员审查，提出修改意见
5. 开发者根据意见修改，所有审查者批准且CI通过后方可合并

#### 审查标准检查清单
| 类别 | 检查项 |
|------|--------|
| **功能性** | 代码是否正确实现需求？是否存在逻辑错误或边界条件处理不当？ |
| **性能** | 是否存在性能瓶颈？超过100行变更是否进行性能评估？ |
| **安全性** | 是否通过安全扫描？是否存在SQL注入、XSS、硬编码敏感信息等风险？ |
| **可读性与可维护性** | 命名是否清晰？结构是否清楚？是否遵循编码规范？方法长度不超过40行？ |
| **测试** | 是否包含对应单元/集成测试？是否覆盖主要路径和边界条件？覆盖率达标？ |
| **设计** | 是否符合整体架构设计（DDD/六边形）？是否避免代码重复？ |

> 严重问题标记为"必须修复"，建议性改进标记为"可以考虑"。

#### Claude Code审查辅助
Claude Code作为"第一遍过滤器"可执行：
- 自动化规范检查（命名、格式、Lombok使用等）
- 识别潜在逻辑错误、性能问题、安全风险
- 生成结构化审查摘要
- 提供具体改进建议
> 注意：Claude Code审查不能替代人工审查，最终批准决策必须由人类做出。

### 3. 质量门（Quality Gates）
- **测试覆盖率**: 业务逻辑代码单元测试覆盖率≥80%，使用JaCoCo统计，不达标则CI失败
- **安全扫描**: 集成SAST工具（SonarQube等），Critical/High级别漏洞必须修复
- **性能检查**: 性能敏感路径变更需进行性能测试，性能下降超过10%需优化
- **文档检查**: 公共API变更需更新OpenAPI文档，复杂逻辑需补充注释

### 4. CI/CD规范
#### 自动化流水线
每次提交或PR创建时自动触发：
1. 自动化构建（干净环境中编译打包）
2. 自动化测试（单元测试、集成测试、E2E测试，使用Testcontainers管理依赖）
3. 代码质量检查（静态分析、风格检查、安全扫描）

#### 部署模式
根据场景选择部署策略：
- **蓝绿部署**: 零停机，两套相同环境切换流量，快速回滚
- **金丝雀发布**: 先发布给小部分用户验证稳定性，逐步扩大范围
- **滚动更新**: K8s默认策略，逐步替换实例，节省资源

#### 回滚策略
- 自动化回滚：监控关键指标（错误率、响应时间）异常超阈值自动回滚
- 手动回滚：提供简单可靠的手动回滚机制，定期演练

---

## 测试策略

### 1. 测试层次
#### 单元测试
- 测试最小可测试单元（单个方法/类），外部依赖全部Mock
- 框架: JUnit 5 + Mockito
- 遵循FIRST原则: Fast（快速）、Independent（独立）、Repeatable（可重复）、Self-Validating（自验证）、Timely（及时）
- 覆盖率要求≥80%

#### 集成测试
- 验证多个组件/服务协同工作，使用真实外部依赖
- 使用Testcontainers启动一次性容器化服务（PostgreSQL、Redis、Kafka等）
- 测试Repository与数据库交互、服务与外部API通信等

#### 端到端测试
- 从用户角度模拟真实场景，验证整个系统
- 使用Selenium/Cypress模拟浏览器操作
- 优先覆盖核心高频业务路径，集成到CI/CD在预发布环境执行

### 2. 测试规范
所有测试用例遵循**Given-When-Then**结构：
- **Given（前置条件）**: 设置初始状态，创建测试对象，准备测试数据，配置Mock行为
- **When（执行操作）**: 执行被测试的操作或方法
- **Then（验证结果）**: 断言结果是否符合预期，检查返回值、对象状态变化、Mock交互验证

---

## 常用命令
```bash
# 构建项目
mvn clean install

# 运行单元测试
mvn test

# 运行集成测试
mvn verify

# 代码格式检查
mvn checkstyle:check

# 生成覆盖率报告
mvn jacoco:report
```
