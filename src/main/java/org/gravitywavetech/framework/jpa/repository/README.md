# JPA 通用查询方案：基类骨架 + 查询片段拆分

## 一、接线（必须且只需一处）

```java
@EnableJpaRepositories(
        basePackages = "com.xxx.repository",
        repositoryBaseClass = ExtendedBaseRepositoryImpl.class,
        repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean.class)
```

## 二、三层结构

```
接口层  ExtendedBaseRepository<T, ID>          ← @NoRepositoryBean，自身不声明任何查询方法，只组合三类片段
        ├─ NativeQueryFragment<T>              ← NativeQueryBuilder<T> / <R> List<R> / <R> Page<R>
        ├─ HqlQueryFragment<T>                 ← HqlQueryBuilder<T> / List<T> / Page<T>
        └─ CriteriaQueryFragment<T>            ← CriteriaQueryBuilder<T> / Page<T>

实现层  BaseRepositoryImpl<T, ID>              ← save() 审计字段填充、String 主键 UUID 生成、isNew/persist/merge 分发
        │                                        （不感知任何查询构建逻辑）
        └─ ExtendedBaseRepositoryImpl<T, ID>   ← repositoryBaseClass：只做委托，持有三个片段实例
             ├─ NativeQueryFragmentImpl<T>     ← (EntityManager, Class<T>) 构造，跨仓储复用
             ├─ HqlQueryFragmentImpl<T>        ← 同上
             └─ CriteriaQueryFragmentImpl<T>   ← 同上
        SqlCountSupport                        ← COUNT 语句生成的唯一实现（唯一入口）

装配层  ExtendedJpaRepositoryFactoryBean       ← @EnableJpaRepositories.repositoryFactoryBeanClass（按需装配时使用）
        └─ ExtendedJpaRepositoryFactory        ← 代理创建时把 domainClass 注入三个片段
```

`SqlCountSupport` 的分工：

| 场景 | 规则 |
| --- | --- |
| Native SQL | `SELECT COUNT(*) FROM (原SQL) t`，不做字符串解析，GROUP BY / DISTINCT / JOIN 均成立 |
| HQL | 剥离 `SELECT`、`ORDER BY` 后重拼；含 GROUP BY / UNION / 子查询时抛异常，要求显式设置 countHql |

## 三、两种装配方式（按需选择）

**方式 1：继承 `ExtendedBaseRepository`（推荐）**

```java
public interface DfApiDictItemRepository extends ExtendedBaseRepository<DfApiDictItem, Long> {
}
```

* 查询能力由 `repositoryBaseClass` 提供，泛型完整（`NativeQueryBuilder<DfApiDictItem>` 等，无需强转）
* 可以按需重写 `save()` 等既有 CRUD 方法
* `ExtendedJpaRepositoryFactory` 不会为这类仓储重复注入片段

**方式 2：只继承需要的片段（按需装配）**

```java
public interface PsLawcaseRepository extends JpaRepository<PsLawcase, String>, HqlQueryFragment<PsLawcase> {
}
```

* 只要 HQL 的仓储不会被动引入 Native/Criteria 能力
* 实现由 `ExtendedJpaRepositoryFactory` 在代理创建时注入

## 四、扩展指引

| 要改什么 | 改哪里 |
| --- | --- |
| 新增一种查询语言（QueryDSL / 存储过程等） | 加一个 `XxxQueryFragment<T>` 接口 + 实现，再在 `ExtendedJpaRepositoryFactory` 里 `append` 一行 |
| 调整 save / 审计 / 主键生成 | 只改 `BaseRepositoryImpl` |
| 调整 COUNT 生成规则 | 只改 `SqlCountSupport` |

## 五、约束与注意

1. **片段接口必须参数化**（`<T>`）。否则返回值只能是 `?`，业务侧必须强转 —— 这是纯 Fragment 方案最大的缺陷。
2. `ExtendedJpaRepositoryFactory` 中 `metadata.getDomainType()` 只有 `Class<?>`，无法直接实例化参数化片段，做了一次受控的 raw 转换并压制告警。
3. 已继承 `ExtendedBaseRepository` 的仓储，其查询能力由基类提供，Factory 不再追加片段，避免同一方法出现两个实现来源。
4. `HqlQueryBuilder` 的聚合表达式入口是 `selectExpression(expression, alias)`，不要命名为 `select`，否则 `select("id", "caseno")` 会被重载解析优先绑定到固定元数方法（固定元数优先于可变参数）。
