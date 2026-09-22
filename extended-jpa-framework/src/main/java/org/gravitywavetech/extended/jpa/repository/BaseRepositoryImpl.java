package org.gravitywavetech.extended.jpa.repository;

import org.gravitywavetech.extended.jpa.entity.BaseEntity;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用仓储基类 —— 只负责 CRUD 相关的横切能力。
 *
 * <p>职责边界（与查询能力彻底分离）：</p>
 * <ul>
 *     <li>无 {@code @GeneratedValue} 时的 String 主键 UUID 生成</li>
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
     * 按实体类缓存，避免每次 save 都反射扫描继承链；仅当主键为空且非自动生成时才补 Base58 UUID。</p>
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
            boolean idIsEmpty = (currentId == null)
                    || (currentId instanceof String && !StringUtils.hasLength((String) currentId));

            if (idIsEmpty) {
                IdGenerationStrategy strategy = lookupIdStrategy(entity.getClass());
                if (!strategy.hasGeneratedValue && strategy.idType == String.class) {
                    ((BaseEntity) baseEntity).setId(UuidUtil.base58Uuid());
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
     *     <li>先对每个实体补主键（复用 {@link #save(S)} 的策略）；</li>
     *     <li>批量 {@code persist}，让 Hibernate 把 INSERT 语句攒到 JDBC 连接级 batching；</li>
     *     <li>每 {@code BATCH_SIZE} 个执行一次 {@code flush + clear}，既触发一批 DML 提交，
     *     又控制一级缓存规模；</li>
     *     <li>末尾 {@code flush}，把残留实体落库；</li>
     * </ol>
     *
     * <p>JDBC 层的 batching 由数据源配置决定（Druid / HikariCP 的 {@code jdbcBatchSize}）。
     * 若未开启，本方法退化为"分批 flush"，仍显著优于逐条保存。</p>
     *
     * <p>事务由外层提供（父类 {@code saveAll} 上有 {@code @Transactional}），本方法不做事务管理。</p>
     */
    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        List<S> toSave = new ArrayList<>();
        for (S entity : entities) {
            if (entity == null) {
                throw new IllegalArgumentException("entity must not be null");
            }
            ensureId(entity);
            toSave.add(entity);
        }

        int count = 0;
        for (S entity : toSave) {
            entityManager.persist(entity);
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
     * 与 {@link #save(S)} 保持一致的主键补齐逻辑，抽出以便 {@link #saveAll(Iterable)} 复用。
     */
    @SuppressWarnings("rawtypes")
    private void ensureId(Object entity) {
        if (!(entity instanceof BaseEntity<?> baseEntity)) {
            return;
        }
        Object currentId = baseEntity.getId();
        boolean idIsEmpty = (currentId == null)
                || (currentId instanceof String && !StringUtils.hasLength((String) currentId));
        if (!idIsEmpty) {
            return;
        }
        IdGenerationStrategy strategy = lookupIdStrategy(entity.getClass());
        if (!strategy.hasGeneratedValue && strategy.idType == String.class) {
            ((BaseEntity) baseEntity).setId(UuidUtil.base58Uuid());
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
