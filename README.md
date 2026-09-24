# gravitywavetech-extended-jpa

一个基于 **Spring Boot 4 / Spring Data JPA** 的 JPA 仓储扩展框架，以 **Spring Boot Starter** 形式提供统一的
**审计字段填充 + String 主键 UUID 生成 + 三类动态查询（Native SQL / HQL / Criteria）** 能力。

业务侧继承 `ExtendedBaseRepository<T, ID>` 一行即可获得：

- 完整 CRUD（覆盖 `save()`，在 `String` 主键为空时自动补 Base58 编码的 UUID）
- 审计字段（`CREATE_TIME / UPDATE_TIME / CREATOR_ID / UPDATOR_ID`）由 Spring Data JPA Auditing 自动填充
- `NativeQueryBuilder<T> nativeQuery()` / `HqlQueryBuilder<T> hqlQuery()` / `CriteriaQueryBuilder<T> criteriaQuery()` 三个泛型安全的动态查询入口
- 全部继承自 `JpaRepository<T, ID>` 和 `JpaSpecificationExecutor<T>` 的原生能力（按方法名查询、Spec 等）

---

## 模块结构

仓库为多模块 Maven 项目，Java 版本要求 **25**：

| 模块 | 说明 |
| --- | --- |
| `extended-jpa-framework` | Starter 库（自动配置 + 仓储扩展 + 查询构建器），可被其他项目依赖 |
| `extended-jpa-framework-demo` | 示例应用（验证 Starter 接线），包含两个实体和仓储 |

---

## 快速开始

```bash
# 首次构建（安装父 POM 与 framework 模块）
./mvnw install -N
./mvnw -pl extended-jpa-framework install -DskipTests

# 启动 demo（dev profile，走 H2 内存库，无需外部 MySQL）
./mvnw spring-boot:run

# 跑测试
./mvnw test
```

Windows 下使用 `mvnw.cmd`。

### 依赖引入

业务项目只需在 `pom.xml` 引入 Starter 即可获得全部能力：

```xml
<dependency>
    <groupId>org.gravitywavetech</groupId>
    <artifactId>extended-jpa-framework</artifactId>
    <version>${project.version}</version>
</dependency>
```

启动类上加 `@SpringBootApplication` 即可，无需 `@EnableJpaRepositories`，也无需手写 `basePackages` —— Starter 会通过 `AutoConfigurationPackages` 自动继承业务包路径。

---

## 设计思路

框架遵循三个原则：**接口组合优先、片段单一职责、泛型完整保留**。整体是三层结构。

```
接口层  ExtendedBaseRepository<T, ID>          （@NoRepositoryBean，纯接口组合）
        ├─ NativeQueryFragment<T>              （NativeQueryBuilder<T> / <R> List<R> / <R> Page<R>）
        ├─ HqlQueryFragment<T>                 （HqlQueryBuilder<T> / List<T> / Page<T>）
        └─ CriteriaQueryFragment<T>            （CriteriaQueryBuilder<T> / Page<T>）

实现层  BaseRepositoryImpl<T, ID>              （CRUD 横切：审计字段、String 主键 UUID 生成、
        └─ ExtendedBaseRepositoryImpl<T, ID>   isNew/persist/merge 分发）
             ├─ NativeQueryFragmentImpl<T>     （(EntityManager, Class<T>) 构造，跨仓储复用）
             ├─ HqlQueryFragmentImpl<T>
             └─ CriteriaQueryFragmentImpl<T>
        SqlCountSupport                         （COUNT 语句生成的唯一实现，唯一入口）

装配层  ExtendedJpaRepositoryFactoryBean        （JpaRepositoryFactoryBean 子类）
        └─ ExtendedJpaRepositoryFactory          （getRepositoryFragments 里按 metadata.getDomainType() 注入片段）
```

### 为什么这样切

- **接口组合而非类继承**：`ExtendedBaseRepository` 只是把三个查询片段接在一起，业务不需要 Native SQL 时可以只继承需要的片段，避免"要么全要、要么全不要"。
- **`<T>` 参数化片段**：`NativeQueryBuilder<?>` 的返回值只能是 `?`，业务侧每次都要强转 —— 这是纯 Fragment 方案最大的缺陷，也是保留 `ExtendedBaseRepository` 基类的原因。基类路径下，`nativeQuery()` 直接返回 `NativeQueryBuilder<DfApiDictItem>`。
- **`repositoryBaseClass` 而不是重写代理**：所有 CRUD 横切逻辑（主键生成、审计、persist/merge 分发）集中在 `BaseRepositoryImpl`，片段实现在构造时共享 `EntityManager` 和 `Class<T>`，跨仓储复用同一份实现代码。

---

## 使用方式

### 1. 实体

继承 `BaseEntity<ID>` 即可获得主键契约、审计字段、逻辑删除、排序号，并整体实现 `Persistable<ID>` —— 因为主键在构造时已生成，不能以"主键是否为空"判断新增，通过生命周期回调区分"新增 / 已持久化"，避免 Spring Data 误用 `merge` 产生多余 SELECT。

```java
@Entity
@Table(name = "ps_lawcase")
public class PsLawcase extends BaseEntity<String> {
    @Id
    private String id;

    @Column(name = "case_no")
    private String caseNo;
    // ...
}
```

### 2. 仓储

**推荐方式：继承 `ExtendedBaseRepository<T, ID>`**

```java
public interface PsLawcaseRepository extends ExtendedBaseRepository<PsLawcase, String> {
    // 也可以按需声明按方法名查询
    List<PsLawcase> findByCaseNo(String caseNo);
}
```

**按需方式：只继承需要的片段**

```java
public interface PsLawcaseRepository
        extends JpaRepository<PsLawcase, String>,
                HqlQueryFragment<PsLawcase> {
}
```

`ExtendedJpaRepositoryFactory` 检测到仓储接口已继承 `ExtendedBaseRepository` 时会跳过片段注入，避免同一方法出现两个实现来源。

### 3. 三个查询入口示例

**Native SQL** —— 位置参数 `?1 ?2 ...`，分页交给 Hibernate 方言翻译，不手动拼 `LIMIT`：

```java
NativeQueryBuilder<PsLawcase> builder = repo
    .nativeQuery()
    .select("id", "case_no", "create_time")
    .from("ps_lawcase", "t")
    .eqWithAlias("t", "delete_status", "N")
    .likeWithAlias("t", "case_no", keyword, LikeMode.ANY)
    .orderByWithAlias("t", "create_time", "DESC")
    .limit(offset, pageSize);

List<PsLawcase> rows   = builder.list();
Page<PsLawcase> page   = builder.page(pageable);
List<Map<String,Object>> maps = builder.listForMap();
```

**HQL** —— 字段自动加 `currentAlias.` 前缀（`from("c")` 决定别名）：

```java
Page<PsLawcase> page = repo.hqlQuery()
    .from("c")
    .eq("caseNo", caseNo)
    .isNotNull("deleteStatus")
    .orderBy("createTime", "DESC")
    .page(pageable);
```

**Criteria** —— 用 `BiFunction<CriteriaBuilder, Root<T>, Predicate>` 存"条件构造器"，`count()` 在新 Count Query 上重新生成 Predicate，避免跨 `Root` 复用问题：

```java
Page<PsLawcase> page = repo.findByCriteria(
    repo.criteriaQuery()
        .eq("caseNo", caseNo)
        .desc("createTime"),
    pageable
);
```

### 4. `save()` 语义

- `String` 主键且未显式设置 → 自动补 Base58 编码的 UUID；
- 审计字段完全交由 Spring Data Auditing 处理，框架不再重复赋值；
- 通过 `isNew()` 决定 `persist` 还是 `merge`，避免 `SimpleJpaRepository` 在"主键已生成但未持久化"场景下错误走 `merge`。

---

## 装配链路

```
demo/FrameworkApplication.java
@SpringBootApplication          ← 声明 basePackage
        │
        ▼  读 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
framework/…/autoconfigure/ExtendedJpaAutoConfiguration
@AutoConfiguration(before = DataJpaRepositoriesAutoConfiguration.class)
@ConditionalOnProperty("extended.jpa.enabled", matchIfMissing = true)
@Import(ExtendedJpaRepositoriesRegistrar.class)
        │
        ▼
ExtendedJpaRepositoriesRegistrar
extends AbstractRepositoryConfigurationSupport
getBasePackages() → 从 AutoConfigurationPackages 拿业务包
        │
        ▼  内部配置类挂
@EnableJpaRepositories(
    repositoryBaseClass = ExtendedBaseRepositoryImpl.class,
    repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean.class
)
        │
        ▼  Spring Data 扫描 XxxRepository 接口
ExtendedJpaRepositoryFactory.getRepositoryFragments(metadata)
├─ 继承 ExtendedBaseRepository → 跳过片段注入（由 repositoryBaseClass 提供）
└─ 否则按需为 Native/Hql/Criteria 片段追加 RepositoryFragment
        │
        ▼
new ExtendedBaseRepositoryImpl(entityInformation, entityManager)
├─ 组合三个 Fragment 实现，保留泛型
└─ 由 Spring Data 的 ProxyFactory 包装
        │
        ▼
业务代码 @Autowired 拿到的最终代理
├─ CRUD → ExtendedBaseRepositoryImpl → BaseRepositoryImpl → SimpleJpaRepository
├─ nativeQuery()/findByNativeSql(...)  → NativeQueryFragmentImpl
├─ hqlQuery()/findByHql(...)           → HqlQueryFragmentImpl
└─ criteriaQuery()/findByCriteria(...) → CriteriaQueryFragmentImpl
```

关键：`@EnableJpaRepositories` **不能**直接放在 `@AutoConfiguration` 类上 —— Spring Data 4.1.x 的 `AnnotationRepositoryConfigurationSource.getBasePackages()` 在没有显式 `basePackages` 时只返回声明类自身的包，不会查 `AutoConfigurationPackages`。必须通过 `@Import` 注册自定义 `AbstractRepositoryConfigurationSourceSupport` 子类（见 `ExtendedJpaRepositoriesRegistrar`）。

---

## 运行时配置

- **默认 dev profile**：demo 模块 `application.yaml` 硬编码 `spring.profiles.active=dev`。dev 数据源为 `jdbc:p6spy:h2:mem:...`（通过 p6spy 打印 SQL），`ddl-auto: create-drop`。启动时不需要 MySQL。
- **测试 profile**：`src/test/resources/application.yaml` 用纯 H2（不走 p6spy）。Spring Boot 对 `classpath:/application.yaml` 只加载首个命中，测试期 `target/test-classes` 优先于 `target/classes`，因此测试配置文件会**替换**主配置而不是合并。
- **sit/prod profile**：走 MySQL + Druid + p6spy（`druid-spring-boot-3-starter` 尚未引入，本地开发不用碰这套配置）。
- **H2 关键词白名单**：表列名若用到 H2 保留字（`year / month / day / value / key` 等），需要在 dev/test 两个 URL 里都通过 `NON_KEYWORDS=...` 放行。
- **关闭开关**：`extended.jpa.enabled=false` 可整体关闭本 Starter，回退到 Boot 默认 JPA 仓储装配。

---

## 扩展指引

| 要改什么 | 改哪里 |
| --- | --- |
| 新增一种查询语言（QueryDSL / 存储过程等） | 加 `XxxQueryFragment<T>` 接口 + `XxxQueryFragmentImpl<T>` 实现，再在 `ExtendedJpaRepositoryFactory` 里 `.append(xxxFragment(domainType))` 一行 |
| 调整 `save()` / 审计字段 / 主键生成策略 | 只改 `BaseRepositoryImpl` |
| 调整 COUNT 生成规则 | 只改 `SqlCountSupport`（Native SQL 统一 `SELECT COUNT(*) FROM (原SQL) t`，HQL 剥离 `SELECT` / `ORDER BY` 后重拼，复杂场景要求调用方显式 `countHql(...)`） |
| 调整仓储接线（包路径、基类、工厂类） | 改 `ExtendedJpaAutoConfiguration` / `ExtendedJpaRepositoriesRegistrar` |

---

## 关键约束（改动前必读）

1. **片段接口必须参数化**（`<T>`）。否则返回值只能是 `?`，业务侧必须强转。
2. `ExtendedJpaRepositoryFactory.getRepositoryFragments()` 中 `metadata.getDomainType()` 只有 `Class<?>`，无法直接实例化参数化片段，代码里做了一次受控的 raw 转换并压制告警。改这一段要小心。
3. `HqlQueryBuilder.select("id", "caseno")` **不能**与 `selectExpression(expression, alias)` 冲突。Java 重载解析会优先匹配固定元数方法，`select("id", "caseno")` 会被静默当成"表达式 + 别名"生成 `SELECT id AS caseno`。
4. `CriteriaQueryBuilder` 的 `predicate(Predicate)` / `or(...)` / `and(...)` 会把绑定到当前 `Root` 的外部 Predicate 加入 `externalPredicates`，`count()` 会显式抛 `IllegalStateException`。分页 / 统计请只用内置的 `eq/like/gt/...`。
5. **COUNT 生成规则集中在 `SqlCountSupport`**：Native SQL 统一子查询包装；HQL 剥离后重拼，含 `GROUP BY` / `UNION` / 子查询时抛异常，要求调用方通过 `countHql("SELECT COUNT(*) ...")` 显式设置。
6. 分页统一走 `setFirstResult/setMaxResults`，让 Hibernate 处理方言差异，不要在 SQL 里手写 `LIMIT`。
7. **`@EnableJpaRepositories` 不能直接放在 `@AutoConfiguration` 类上**，必须走 `@Import` 自定义 `AbstractRepositoryConfigurationSourceSupport` 子类。

---

## 参考

- 装配验证测试：`extended-jpa-framework-demo/src/test/java/org/gravitywavetech/extended/jpa/demo/repository/JpaRepositoryWiringTest.java`（如何从 AOP 代理背后取到真实实现类）
- 三个查询入口委托测试：`ExtendedBaseRepositoryImplTest.java`
- 单片段单测：`NativeQueryFragmentImplTest.java` / `HqlQueryFragmentTest.java`