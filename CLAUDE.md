# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目定位

一个 Spring Boot 4 / Spring Data JPA 扩展框架，以 **Spring Boot Starter** 形式提供统一的
「审计字段填充 + String 主键 UUID 生成 + 三类动态查询（Native SQL / HQL / Criteria）」能力。

仓库为多模块 Maven 项目：

| 模块 | 说明 |
| --- | --- |
| `extended-jpa-framework` | Starter 库（自动配置 + 仓储扩展 + 查询构建器） |
| `extended-jpa-framework-demo` | 示例应用（验证 Starter 接线） |

Java 版本要求 **25**（父 `pom.xml` 中 `<java.version>25</java.version>`）。修改 `pom.xml` 前先确认本地 JDK 满足该版本。

## 常用命令

```bash
./mvnw spring-boot:run          # 启动 demo 应用（默认 dev profile，走 H2 内存库）
./mvnw test                     # 跑全部测试
./mvnw test -Dtest=类名#方法名   # 跑单个测试方法
./mvnw -DskipTests package      # 打包
```

Windows 下用 `mvnw.cmd`。demo 测试使用 `extended-jpa-framework-demo/src/test/resources/application.yaml` 覆盖的数据源（纯 H2），不需要外部 MySQL。

首次运行前需先安装父 POM 和 framework 模块：

```bash
./mvnw install -N          # 安装父 POM
./mvnw -pl extended-jpa-framework install -DskipTests   # 安装 framework 模块
```

## 架构总览

框架通过 **自动配置** 接入 Spring Boot 应用，入口在 `extended-jpa-framework/src/main/java/org/gravitywavetech/extended/jpa/autoconfigure/ExtendedJpaAutoConfiguration.java`。

```
autoconfigure/
├── ExtendedJpaAutoConfiguration.java       @AutoConfiguration 入口，@Import Registrar
├── ExtendedJpaAuditingAutoConfiguration.java @EnableJpaAuditing + 默认 AuditorAware
└── ExtendedJpaRepositoriesRegistrar.java    基于 AbstractRepositoryConfigurationSourceSupport，
                                            通过 AutoConfigurationPackages 扫描仓储

resources/META-INF/spring/
└── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

`ExtendedJpaRepositoriesRegistrar` 继承 Spring Boot 的 `AbstractRepositoryConfigurationSourceSupport`，
使用 `AutoConfigurationPackages` 解析仓储扫描路径（而非使用方声明类自身的包），
这是 `@AutoConfiguration` 类上直接使用 `@EnableJpaRepositories` 无法做到的。

### 三层结构

```
接口层  ExtendedBaseRepository<T, ID>          （@NoRepositoryBean，纯接口组合）
        ├─ NativeQueryFragment<T>              （NativeQueryBuilder<T> / <R> List<R> / <R> Page<R>）
        ├─ HqlQueryFragment<T>                 （HqlQueryBuilder<T> / List<T> / Page<T>）
        └─ CriteriaQueryFragment<T>            （CriteriaQueryBuilder<T> / Page<T>）

实现层  BaseRepositoryImpl<T, ID>              （只负责 CRUD 横切：审计字段、String 主键 UUID 生成、isNew/persist/merge 分发）
        └─ ExtendedBaseRepositoryImpl<T, ID>   （repositoryBaseClass；薄层，只做委托，构造三个 Fragment 实现）
             ├─ NativeQueryFragmentImpl<T>     （(EntityManager, Class<T>) 构造，跨仓储复用）
             ├─ HqlQueryFragmentImpl<T>
             └─ CriteriaQueryFragmentImpl<T>
        SqlCountSupport                         （COUNT 语句生成的唯一实现，唯一入口）

装配层  ExtendedJpaRepositoryFactoryBean        （JpaRepositoryFactoryBean 子类）
        └─ ExtendedJpaRepositoryFactory          （getRepositoryFragments 里按 metadata.getDomainType() 注入片段）
```

### Builder 三兄弟

- `NativeQueryBuilder` — 字符串拼接 Native SQL，位置参数 `?1 ?2 ...`；`LIMIT` 由 `setFirstResult/setMaxResults` 交给 Hibernate 方言翻译，不手动拼。
- `HqlQueryBuilder` — 字符串拼接 HQL；字段自动加 `currentAlias.` 前缀（`from("c")` 决定别名）。
- `CriteriaQueryBuilder` — 用 `BiFunction<CriteriaBuilder, Root<T>, Predicate>` 存储「条件构造器」，而不是绑定到具体 `Root` 的 `Predicate`，因此 `count()` 可以在新的 Count Query 上重新生成 Predicate，彻底避免跨 `Root` 复用问题。

## 业务仓储的两种接线方式

1. **继承 `ExtendedBaseRepository<T, ID>`（推荐）** — 查询能力由 `repositoryBaseClass` 提供，泛型完整（`NativeQueryBuilder<T>` 等，无需强转），也可以按需重写 `save()` 等既有 CRUD 方法。
2. **只继承需要的片段（按需装配）** — 例：`interface PsLawcaseRepository extends JpaRepository<PsLawcase, String>, HqlQueryFragment<PsLawcase>`，只拿 HQL 能力，不引入 Native/Criteria。实现由 `ExtendedJpaRepositoryFactory` 在代理创建时注入。

`ExtendedJpaRepositoryFactory` 检测到仓储接口已继承 `ExtendedBaseRepository` 时会**跳过**片段注入，避免同一方法出现两个实现来源。

## 关键约束（改动前必读）

1. **片段接口必须参数化**（`<T>`）。否则返回值只能是 `?`，业务侧必须强转 —— 这是纯 Fragment 方案最大的缺陷，也是保留基类的原因。
2. `ExtendedJpaRepositoryFactory.getRepositoryFragments()` 中 `metadata.getDomainType()` 只有 `Class<?>`，无法直接实例化参数化片段，代码里做了一次受控的 raw 转换并压制告警。改这一段要小心。
3. `HqlQueryBuilder.select("id", "caseno")` **不能**命名为 `select` 的固定元数重载。已有一个 `selectExpression(expression, alias)` 用于聚合表达式。如果把 `selectExpression` 改名为 `select`，Java 重载解析会优先匹配固定元数方法，`select("id", "caseno")` 会被静默当成「表达式 + 别名」生成 `SELECT id AS caseno`。
4. `CriteriaQueryBuilder` 的 `predicate(Predicate)` / `or(...)` / `and(...)` 会把绑定到当前 `Root` 的外部 Predicate 加入 `externalPredicates`，`count()` 会显式抛 `IllegalStateException`。分页/统计请只用内置的 `eq/like/gt/...`。
5. **COUNT 生成规则集中在 `SqlCountSupport`**：
   - Native SQL：统一 `SELECT COUNT(*) FROM (原SQL) t`，不做字符串解析，GROUP BY / DISTINCT / JOIN / 子查询均成立。
   - HQL：剥离 `SELECT` 和 `ORDER BY` 后重拼；含 GROUP BY / UNION / 子查询时抛 `IllegalStateException`，要求调用方通过 `countHql("SELECT COUNT(*) ...")` 显式设置。
6. 分页统一走 `setFirstResult/setMaxResults`，让 Hibernate 处理方言差异，不要在 SQL 里手写 `LIMIT`。
7. **`@EnableJpaRepositories` 不能直接放在 `@AutoConfiguration` 类上** —— Spring Data 4.1.x 的 `AnnotationRepositoryConfigurationSource.getBasePackages()` 在没有显式 `basePackages` 时只返回声明类自身的包，不会查 `AutoConfigurationPackages`。必须通过 `@Import` 注册自定义 `AbstractRepositoryConfigurationSourceSupport` 子类（见 `ExtendedJpaRepositoriesRegistrar`）。

## 扩展指引

| 要改什么 | 改哪里 |
| --- | --- |
| 新增一种查询语言（QueryDSL / 存储过程等） | 加一个 `XxxQueryFragment<T>` 接口 + `XxxQueryFragmentImpl<T>` 实现，再在 `ExtendedJpaRepositoryFactory` 里 `.append(xxxFragment(domainType))` 一行 |
| 调整 `save()` / 审计字段 / 主键生成策略 | 只改 `BaseRepositoryImpl` |
| 调整 COUNT 生成规则 | 只改 `SqlCountSupport` |
| 调整仓储接线（包路径、基类、工厂类） | 改 `ExtendedJpaAutoConfiguration` / `ExtendedJpaRepositoriesRegistrar` |

## 运行时配置

- **默认 dev profile**：demo 模块 `application.yaml` 硬编码 `spring.profiles.active=dev`。dev 数据源为 `jdbc:p6spy:h2:mem:...`（通过 p6spy 打印 SQL），`ddl-auto: create-drop`。启动时不需要 MySQL。
- **测试 profile**：`src/test/resources/application.yaml` 用纯 H2（不走 p6spy）。注意 Spring Boot 对 `classpath:/application.yaml` 只加载首个命中，测试期 `target/test-classes` 优先于 `target/classes`，因此测试配置文件会**替换**主配置而不是合并。
- **sit/prod profile**：走 `192.168.0.113:3306/jw-v1` 的 MySQL + Druid + p6spy。`druid-spring-boot-3-starter` 依赖尚未引入，`application-sit.yaml` 中 `type: com.alibaba.druid.pool.DruidDataSource` 目前无法真正生效，本地开发不用碰这套配置。
- **H2 关键词白名单**：`ps_lawcase` 表用到了 `year / month / day / hour / minute / second / value / key` 等 H2 保留字作列名，dev/test 数据源 URL 都通过 `NON_KEYWORDS=YEAR,MONTH,...` 放行。加新表若列名碰到 H2 保留字，需要在两个 URL 里都补上。

## 参考

- 框架设计原文：`extended-jpa-framework/src/main/java/org/gravitywavetech/extended/jpa/repository/README.md`
- 接线验证测试：`extended-jpa-framework-demo/src/test/java/org/gravitywavetech/extended/jpa/demo/repository/JpaRepositoryWiringTest.java`（如何从 AOP 代理背后取到真实实现类）