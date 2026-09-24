package org.gravitywavetech.extended.jpa.repository;

import org.gravitywavetech.extended.jpa.entity.BaseEntity;
import org.gravitywavetech.extended.jpa.util.SnowflakeUtil;
import org.gravitywavetech.extended.jpa.util.UuidUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用仓储基类 —— 只负责 CRUD 相关的横切能力。
 *
 * <p>职责边界（与查询能力彻底分离）：</p>
 * <ul>
 *     <li>无 {@code @GeneratedValue} 时按 {@code @Id} 字段类型自动补齐主键
 *         （当前内置：{@code String} → Base58 UUID，{@code Long}/{@code long} → 雪花 ID）</li>
 *     <li>绕过父类 ID 强转问题的 persist / merge 分发</li>
 * </ul>
 *
 * <p>审计字段（creatorId / createTime / updatorId / updateTime）由 Spring Data Auditing
 * 通过 {@code AuditingEntityListener} 在 {@code persist / merge} 前自动填充，本类不再重复处理。</p>
 *
 * <p>Native SQL / HQL / Criteria 查询能力由
 * {@link NativeQueryFragment} / {@link HqlQueryFragment} / {@link CriteriaQueryFragment} 提供，
 * 并由 {@link ExtendedBaseRepositoryImpl} 以组合方式委托，本类不感知任何查询构建逻辑。</p>
 *
 * <p>作为 {@code repositoryBaseClass} 使用时通过 {@code JpaEntityInformation} 注入实体类型，
 * 因此能保留完整的 {@code T} 泛型信息 —— 这是 Fragment 接口无法做到的。</p>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author Administrator
 * @version 1.0
 */
@NoRepositoryBean
public class BaseRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {

    protected final EntityManager entityManager;
    protected final Class<T> domainClass;
    /** 自己持有一份 entityInformation，不再依赖父类的包级私有字段 */
    protected final JpaEntityInformation<T, Serializable> entityInformation;

    /**
     * 每个实体类只反射扫描一次，缓存主键策略（是否标注 @GeneratedValue、@Id 字段的实际类型）。
     * Spring Data 的 {@code getIdType()} 在某些泛型场景下会退化为 {@code Object}/{@code Serializable}，
     * 需要用反射结果补齐。
     */
    private static final ConcurrentHashMap<Class<?>, IdGenerationStrategy> ID_STRATEGY_CACHE =
            new ConcurrentHashMap<>();

    /**
     * 主键生成器注册表：按 {@code @Id} 字段的实际类型分发。
     *
     * <p>新增一种 ID 策略只需在此加一行，不必改动 {@link #save(Object)} 主体；
     * 与 {@code SqlCountSupport} 的「唯一入口」原则一致。</p>
     */
    private static final Map<Class<?>, IdGenerator> GENERATORS = Map.of(
            String.class, UuidUtil::base58Uuid,
            Long.class,   SnowflakeUtil::nextId,
            Long.TYPE,    SnowflakeUtil::nextId   // 原生 long 字段
    );

    /** 主键生成器：按 ID 字段类型生成新值。返回 {@code Serializable} 以直接喂给 {@code setId(ID)}。 */
    @FunctionalInterface
    private interface IdGenerator {
        Serializable nextId();
    }

    /**
     * 判断主键是否"未设置"：{@code null} 或空字符串（仅 String 类型适用）。
     * 其他类型（如 {@code Long}）只认 {@code null} 为空——业务如需以 {@code 0} 等哨兵表示未设置，
     * 请自行在保存前显式置空。
     */
    private static boolean isIdEmpty(Object id) {
        if (id == null) {
            return true;
        }
        return id instanceof String s && !StringUtils.hasLength(s);
    }

    /**
     * {@link #saveAll(Iterable)} 每批 flush 的实体数。与数据源 JDBC batching 配合
     * 才能真正把 N 个 INSERT 合并成少数 SQL 语句；单批过大将导致 flush 阶段 SQL 语句堆在内存里。
     */
    private static final int BATCH_SIZE = 50;

    /**
     * 构造函数 - 匹配父类的签名，供 Spring Data 反射调用
     */
    public BaseRepositoryImpl(JpaEntityInformation<T, Serializable> entityInformation,
                              EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
        this.domainClass = entityInformation.getJavaType();
    }

    /**
     * 构造函数 - 使用实体类
     */
    @SuppressWarnings("unchecked")
    public BaseRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        this((JpaEntityInformation<T, Serializable>) JpaEntityInformationSupport
                .getEntityInformation(domainClass, entityManager), entityManager);
    }

    /**
     * 重写 save：按需生成主键，并直接走 JPA 的 persist / merge。
     *
     * <p>审计字段（creatorId / createTime / updatorId / updateTime）交由 Spring Data Auditing
     * 自动填充，本方法不再重复处理。</p>
     *
     * <p>主键生成策略（{@code @GeneratedValue} 是否声明、{@code @Id} 字段的实际类型）
     * 按实体类缓存，避免每次 save 都反射扫描继承链；主键为空且非 {@code @GeneratedValue} 时，
     * 按 {@link #GENERATORS} 注册表按类型自动生成（String→Base58 UUID，Long→雪花 ID）。</p>
     */
    @Override
    @Transactional
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <S extends T> S save(S entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }

        if (entity instanceof BaseEntity<?> baseEntity) {
            Object currentId = baseEntity.getId();
            if (isIdEmpty(currentId)) {
                IdGenerationStrategy strategy = lookupIdStrategy(entity.getClass());
                if (!strategy.hasGeneratedValue) {
                    IdGenerator generator = GENERATORS.get(strategy.idType);
                    if (generator == null) {
                        throw new IllegalArgumentException(
                                "主键为空且类型无内置生成器: entity=" + entity.getClass().getName()
                                        + ", idType=" + strategy.idType);
                    }
                    ((BaseEntity) baseEntity).setId(generator.nextId());
                }
                // else: @GeneratedValue 由 JPA 自管，不填充也不报错
            }
        } else {
            // 非 BaseEntity 的实体：仅在非 @GeneratedValue 且主键为空时快速失败，避免 persist 时抛出难懂的 JPA 异常。
            Object currentId = this.entityInformation.getId(entity);
            if (currentId == null) {
                IdGenerationStrategy strategy = lookupIdStrategy(entity.getClass());
                if (!strategy.hasGeneratedValue) {
                    throw new IllegalArgumentException(
                            "实体未继承 BaseEntity，且主键为空、缺少 @GeneratedValue：entity=" + entity.getClass().getName());
                }
            }
        }

        if (this.entityInformation.isNew(entity)) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    /**
     * 批量保存。
     *
     * <p>父类 {@link SimpleJpaRepository#saveAll(Iterable)} 逐条调用 {@code save} 并 flush，
     * 对大数据量导入很慢。本实现按 {@link #BATCH_SIZE} 一批 flush + clear：
     * <ol>
     *     <li>逐条调用 {@link #save(S)}：先做主键补齐，再根据 {@code isNew()} 分派 {@code persist/merge}
     *         —— 与单条 {@code save} 行为完全一致，因此允许混合传入新实体与已加载实体；</li>
     *     <li>每 {@code BATCH_SIZE} 个执行一次 {@code flush + clear}，既触发一批 DML 提交，
     *         又控制一级缓存规模；</li>
     *     <li>末尾 {@code flush}，把残留实体落库。</li>
     * </ol>
     *
     * <p>JDBC 层的 batching 由数据源配置决定（Druid / HikariCP 的 {@code jdbcBatchSize}）。
     * 若未开启，本方法退化为"分批 flush"，仍显著优于逐条保存。</p>
     */
    @Override
    @Transactional
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        List<S> toSave = new ArrayList<>();
        for (S entity : entities) {
            if (entity == null) {
                throw new IllegalArgumentException("entity must not be null");
            }
            toSave.add(save(entity));
        }

        int count = 0;
        for (S entity : toSave) {
            if (++count % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        if (count % BATCH_SIZE != 0) {
            entityManager.flush();
        }
        return toSave;
    }

    /**
     * 与 {@link #save(S)} 一致的主键补齐逻辑，与 save 共享 {@link #GENERATORS} 注册表。
     *
     * <p>预留供后续按业务语义在特定路径复用（如脱离 save 的批量预处理）；当前无调用方。</p>
     */
    @SuppressWarnings("rawtypes")
    private void ensureId(Object entity) {
        if (!(entity instanceof BaseEntity<?> baseEntity)) {
            return;
        }
        Object currentId = baseEntity.getId();
        if (!isIdEmpty(currentId)) {
            return;
        }
        IdGenerationStrategy strategy = lookupIdStrategy(entity.getClass());
        if (strategy.hasGeneratedValue) {
            return; // @GeneratedValue 由 JPA 自管
        }
        IdGenerator generator = GENERATORS.get(strategy.idType);
        if (generator != null) {
            ((BaseEntity) baseEntity).setId(generator.nextId());
        }
    }

    /* ==================== 主键策略缓存 ==================== */

    private static IdGenerationStrategy lookupIdStrategy(Class<?> entityClass) {
        return ID_STRATEGY_CACHE.computeIfAbsent(entityClass, BaseRepositoryImpl::buildIdStrategy);
    }

    private static IdGenerationStrategy buildIdStrategy(Class<?> entityClass) {
        Field idField = findIdField(entityClass);
        boolean hasGeneratedValue = idField != null && idField.isAnnotationPresent(GeneratedValue.class);
        Class<?> idType = idField != null ? idField.getType() : null;
        return new IdGenerationStrategy(hasGeneratedValue, idType);
    }

    private static Field findIdField(Class<?> entityClass) {
        for (Class<?> c = entityClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Id.class)) {
                    return f;
                }
            }
        }
        return null;
    }

    /** 每个实体类的主键生成策略，进程内缓存。 */
    private record IdGenerationStrategy(boolean hasGeneratedValue, Class<?> idType) {
    }
}
