demo 的 FrameworkApplication 只有 @SpringBootApplication，仓储对象完全由 starter 自动装配。
装配链路

demo/FrameworkApplication.java
@SpringBootApplication          ← 声明 basePackage（org.gravitywavetech.extended.jpa.demo）
│
│ 启动时读 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports                                                                                    
▼
framework/…/autoconfigure/ExtendedJpaAutoConfiguration
@AutoConfiguration(before = DataJpaRepositoriesAutoConfiguration.class)
@ConditionalOnClass / @ConditionalOnProperty / matchIfMissing=true
@Import(ExtendedJpaRepositoriesRegistrar.class)
│
│ 通过 @Import 处理
▼
framework/…/autoconfigure/ExtendedJpaRepositoriesRegistrar
extends AbstractRepositoryConfigurationSourceSupport
getAnnotation()      → @EnableJpaRepositories
getConfiguration()   → ExtendedJpaRepositoriesConfiguration（内部静态类）
getBasePackages()    → 从 AutoConfigurationPackages 拿 demo 的包（不用手写 basePackages）
│
│ 该内部静态类上挂着
▼
@EnableJpaRepositories(
repositoryBaseClass = ExtendedBaseRepositoryImpl.class,
repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean.class
)
class ExtendedJpaRepositoriesConfiguration {}
│
│ Spring Data 启动 Repository 扫描
▼
扫描 org.gravitywavetech.extended.jpa.demo 包下所有 XxxRepository 接口
→ DfApiDictItemRepository (extends ExtendedBaseRepository)
→ PsLawcaseRepository     (extends ExtendedBaseRepository)
│
│ 通过 ExtendedJpaRepositoryFactoryBean 创建代理
▼
ExtendedJpaRepositoryFactory.getRepositoryFragments(metadata)
├─ 检测 repositoryInterface 是否 extends ExtendedBaseRepository → 是则跳过片段注入
└─ 否则按需为 Native/Hql/Criteria 接口片段追加 RepositoryFragment
│
│ repositoryBaseClass 实例化
▼
new ExtendedBaseRepositoryImpl(entityInformation, entityManager)
└─ 组合三个 Fragment 实现（Native/Hql/Criteria），保留泛型，可覆盖 save()
│
│ 由 Spring Data 的 ProxyFactory 包装
▼
业务代码 @Autowired 拿到的最终代理对象
├─ CRUD → ExtendedBaseRepositoryImpl → BaseRepositoryImpl → SimpleJpaRepository
├─ nativeQuery()/findByNativeSql(...)  → NativeQueryFragmentImpl
├─ hqlQuery()/findByHql(...)           → HqlQueryFragmentImpl
└─ criteriaQuery()/findByCriteria(...) → CriteriaQueryFragmentImpl

关键点
new ExtendedBaseRepositoryImpl(entityInformation, entityManager)
└─ 组合三个 Fragment 实现（Native/Hql/Criteria），保留泛型，可覆盖 save()
│
│ 由 Spring Data 的 ProxyFactory 包装
▼
业务代码 @Autowired 拿到的最终代理对象
├─ CRUD → ExtendedBaseRepositoryImpl → BaseRepositoryImpl → SimpleJpaRepository
├─ nativeQuery()/findByNativeSql(...)  → NativeQueryFragmentImpl
├─ hqlQuery()/findByHql(...)           → HqlQueryFragmentImpl
└─ criteriaQuery()/findByCriteria(...) → CriteriaQueryFragmentImpl
关键点

1. @SpringBootApplication 就够：starter 通过 AutoConfigurationPackages 自动继承 base package，所以业务侧不需要 @EnableJpaRepositories 也不需要写 basePackages。这正是 ExtendedJpaRepositoriesRegistrar 存在的意义（写在 CLAUDE.md
   关键约束第 7 条）。
2. 顺序保证：@AutoConfiguration(before = DataJpaRepositoriesAutoConfiguration.class) 让本 starter 先于 Boot 自带的 DataJpaRepositoriesAutoConfiguration 生效；后者类上有 @ConditionalOnMissingBean({JpaRepositoryFactoryBean.class,
   JpaRepositoryConfigExtension.class})，会检测到我们已经注入的 Bean 自动让路，不会重复扫描。这就是 P0 #1 修的那个坑——原来条件写反了，让 Boot 生效而本 starter 失效。
3. 关闭开关：extended.jpa.enabled=false 可整体关闭本 starter，回退到 Boot 默认 JPA 仓储装配。
4. 验证方式：JpaRepositoryWiringTest#repositoryIsBackedByExtendedBaseRepositoryImpl 就是从 AOP 代理背后取真实 target 类型，断言是 ExtendedBaseRepositoryImpl 而不是 SimpleJpaRepository