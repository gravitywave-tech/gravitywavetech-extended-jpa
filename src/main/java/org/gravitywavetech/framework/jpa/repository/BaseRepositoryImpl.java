package org.gravitywavetech.framework.jpa.repository;

import org.gravitywavetech.framework.jpa.entity.BaseEntity;
import org.gravitywavetech.framework.util.UuidUtil;
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
     */
    @Override
    @Transactional
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <S extends T> S save(S entity) {
        // ID 处理：仅当 ID 为空且没有 @GeneratedValue 时才干预
        if (entity instanceof BaseEntity<?> baseEntity) {
            Object currentId = baseEntity.getId();
            boolean idIsEmpty = (currentId == null)
                    || (currentId instanceof String && !StringUtils.hasLength((String) currentId));

            if (idIsEmpty && !hasGeneratedValue(entity)) {
                Class<?> idType = this.entityInformation.getIdType();

                // 防御泛型擦除：如果 getIdType() 返回 Object/Serializable，回退到反射查 @Id 字段
                if (idType == Object.class || idType == Serializable.class) {
                    Field idField = getIdField(entity);
                    if (idField != null) {
                        idType = idField.getType();
                    }
                }

                // 仅对 String 类型赋 UUID；Long/Integer 等保持 null，由数据库自增或业务层处理
                if (idType == String.class) {
                    ((BaseEntity) baseEntity).setId(UuidUtil.base58Uuid());
                }
            }
        }

        // 直接走 JPA
        if (this.entityInformation.isNew(entity)) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }

    /* ==================== 辅助方法 ==================== */

    /**
     * 反射检查实体主键字段是否标注了 @GeneratedValue
     */
    private <S extends T> boolean hasGeneratedValue(S entity) {
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return field.isAnnotationPresent(GeneratedValue.class);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    /**
     * 获取实体类中标注了 @Id 的字段
     */
    private <S extends T> Field getIdField(S entity) {
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
