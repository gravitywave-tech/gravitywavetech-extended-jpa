package org.gravitywavetech.extended.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 实体公共基类。
 *
 * <p>为所有业务实体提供统一的主键契约、审计字段、逻辑删除与排序等公共能力，遵循以下约定：</p>
 * <ul>
 *   <li><b>主键</b>：由子类声明；支持 {@code String}（save 时自动补 Base58 UUID）与
 *       {@code Long}/{@code long}（save 时自动补雪花 ID），也允许业务在构造时预置主键值；</li>
 *   <li><b>审计</b>：{@code CREATE_TIME / UPDATE_TIME / CREATOR_ID / UPDATOR_ID} 由 Spring Data JPA
 *       审计能力自动填充。启用方式：在启动类或配置类上添加 {@code @EnableJpaAuditing}，
 *       并按需提供 {@code AuditorAware}（不提供时审计人字段留空，但不会报错）；</li>
 *   <li><b>逻辑删除</b>：统一使用 {@code DELETE_STATUS} 字段，{@link #DELETE_STATUS_NORMAL 未删除} /
 *       {@link #DELETE_STATUS_DELETED 已删除}；</li>
 *   <li><b>整体实现 {@link Persistable}</b>：由于主键在子类构造时即已生成，不能以“主键是否为空”判断新增，
 *       通过 JPA 生命周期回调明确区分“新增/已持久化”，避免 Spring Data 误用 {@code merge} 而产生多余的 SELECT。</li>
 * </ul>
 *
 * <p>说明：本类不包含乐观锁 {@code @Version} 字段。若对应业务表已存在版本列，请在子类中自行声明，
 * 以避免与实际表结构不一致。</p>
 *
 * @param <ID> 主键类型，必须实现 {@link Serializable}
 * @author gravitywavetech
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity<ID extends Serializable> implements Persistable<ID>, Serializable {

    private static final long serialVersionUID = 1L;

    /** 逻辑删除标识：未删除。 */
    public static final String DELETE_STATUS_NORMAL = "N";

    /** 逻辑删除标识：已删除。 */
    public static final String DELETE_STATUS_DELETED = "Y";

    /** 创建时间 */
    @CreatedDate
    @Column(name = "CREATE_TIME", updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @LastModifiedDate
    @Column(name = "UPDATE_TIME")
    private LocalDateTime updateTime;

    /** 创建人ID */
    @CreatedBy
    @Column(name = "CREATOR_ID", updatable = false, length = 50)
    private String creatorId;

    /** 更新人ID */
    @LastModifiedBy
    @Column(name = "UPDATOR_ID", length = 50)
    private String updatorId;

    /** 逻辑删除标识：{@link #DELETE_STATUS_NORMAL} 未删除，{@link #DELETE_STATUS_DELETED} 已删除 */
    @Column(name = "DELETE_STATUS", length = 1)
    private String deleteStatus = DELETE_STATUS_NORMAL;

    /** 排序号 */
    @Column(name = "SORT_ORDER")
    private Long sortOrder = 0L;

    /**
     * 是否为“新增”状态。
     *
     * <p>默认视为新增；实体被持久化（{@link PrePersist}）或从数据库加载（{@link PostLoad}）后置为 {@code false}。
     * 该标记被 {@link #isNew()} 使用，用于指导 Spring Data 选择 {@code persist} 还是 {@code merge}。</p>
     */
    @Transient
    private boolean isNew = true;

    protected BaseEntity() {
        // JPA 规范要求的无参构造
    }

    /**
     * 使用指定主键构造实体。
     *
     * <p>注意：该构造会回调子类实现的 {@link #setId(Serializable)}，子类应确保其实现不依赖尚未初始化的字段。</p>
     *
     * @param id 主键
     */
    protected BaseEntity(ID id) {
        setId(id);
    }

    /**
     * 持久化前翻转 {@code isNew} 标记，并补齐未被业务显式赋值的字段默认值。
     *
     * <p>注意：本方法<b>不</b>写 {@code createTime / updateTime / creatorId / updatorId}——
     * 这些审计字段完全交由 Spring Data Auditing（{@code @CreatedDate / @LastModifiedDate /
     * @CreatedBy / @LastModifiedBy}）在 {@code @EnableJpaAuditing} 激活后统一填充。
     * 若此处也写入，会与 Auditing 双重赋值，导致 {@code @LastModifiedBy} 等策略在
     * 字段已非 {@code null} 时被跳过。</p>
     */
    @PrePersist
    protected void prePersist() {
        if (this.deleteStatus == null) {
            this.deleteStatus = DELETE_STATUS_NORMAL;
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0L;
        }
        // 持久化之后不再视为新增
        this.isNew = false;
    }

    @PostLoad
    protected void postLoad() {
        this.isNew = false;
    }

    /**
     * 获取主键，由子类实现。
     *
     * @return 主键
     */
    @Override
    public abstract ID getId();

    /**
     * 设置主键，由子类实现。
     *
     * @param id 主键
     */
    public abstract void setId(ID id);

    /**
     * 是否已逻辑删除。
     *
     * @return 已删除返回 {@code true}
     */
    @Transient
    public boolean isDeleted() {
        return DELETE_STATUS_DELETED.equals(this.deleteStatus);
    }

    /**
     * 标记为已逻辑删除。
     */
    public void markDeleted() {
        this.deleteStatus = DELETE_STATUS_DELETED;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getUpdatorId() {
        return updatorId;
    }

    public void setUpdatorId(String updatorId) {
        this.updatorId = updatorId;
    }

    public String getDeleteStatus() {
        return deleteStatus;
    }

    public void setDeleteStatus(String deleteStatus) {
        this.deleteStatus = deleteStatus;
    }

    public Long getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Long sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>默认视为新增，持久化或加载后转为已持久化。</p>
     */
    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        // 借助 Hibernate.getClass 兼容代理对象，并按实体类型比较，避免不同类型、相同主键被判定相等
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        BaseEntity<?> other = (BaseEntity<?>) o;
        return getId() != null && Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s, createTime=%s, updateTime=%s, creatorId=%s, updatorId=%s, deleteStatus=%s, sortOrder=%s}",
                Hibernate.getClass(this).getSimpleName(), getId(), createTime, updateTime,
                creatorId, updatorId, deleteStatus, sortOrder);
    }
}
