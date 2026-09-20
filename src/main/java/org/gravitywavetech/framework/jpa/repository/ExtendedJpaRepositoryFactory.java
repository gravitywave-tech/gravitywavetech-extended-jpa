package org.gravitywavetech.framework.jpa.repository;

import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;

import jakarta.persistence.EntityManager;

/**
 * 自定义 {@link JpaRepositoryFactory} —— 查询片段的「按需装配」入口。
 *
 * <p>Spring Data 的命名约定 fragment（如 {@code XxxRepositoryImpl}）是跨仓储共享的单例，
 * 无法携带每个仓储自己的实体类型。因此这里覆写
 * {@link #getRepositoryFragments(RepositoryMetadata)}，在<b>每个仓储代理创建时</b>
 * 用当前仓储的 {@code domainType} 实例化三个查询片段，
 * 使 {@link NativeQueryFragment} / {@link HqlQueryFragment} / {@link CriteriaQueryFragment}
 * 既能独立拆分，又能拿到各自的实体类型。</p>
 *
 * <p><b>与 {@link ExtendedBaseRepositoryImpl} 的分工：</b></p>
 * <ul>
 *     <li>仓储接口继承 {@link ExtendedBaseRepository}：三个查询能力由 {@code repositoryBaseClass}
 *         提供（带泛型、可覆盖 CRUD），本 Factory <b>不会</b>重复注入片段。</li>
 *     <li>仓储接口只写 {@code extends JpaRepository<T, ID>, NativeQueryFragment<T>, ...}：
 *         由本 Factory 注入片段实现，实现按需装配（例如只要 HQL 的仓储不引入 Native/Criteria）。</li>
 * </ul>
 *
 * <p>接线方式：{@code @EnableJpaRepositories(repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean.class)}</p>
 */
public class ExtendedJpaRepositoryFactory extends JpaRepositoryFactory {

    private final EntityManager entityManager;

    public ExtendedJpaRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
    }

    @Override
    protected RepositoryComposition.RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
        RepositoryComposition.RepositoryFragments fragments = super.getRepositoryFragments(metadata);

        // 已继承 ExtendedBaseRepository 的仓储，三个查询能力由 repositoryBaseClass 提供，
        // 这里不再追加片段，避免同一方法出现两个实现来源。
        if (ExtendedBaseRepository.class.isAssignableFrom(metadata.getRepositoryInterface())) {
            return fragments;
        }

        Class<?> domainType = metadata.getDomainType();
        return fragments
                .append(nativeFragment(domainType))
                .append(hqlFragment(domainType))
                .append(criteriaFragment(domainType));
    }

    /**
     * 片段的实体类型只有在仓储代理创建时才可知（{@code Class<?>}），
     * 无法直接实例化参数化类型，这里做一次受控的 raw 转换。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private RepositoryFragment<?> nativeFragment(Class<?> domainType) {
        return RepositoryFragment.implemented(NativeQueryFragment.class,
                new NativeQueryFragmentImpl(entityManager, domainType));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private RepositoryFragment<?> hqlFragment(Class<?> domainType) {
        return RepositoryFragment.implemented(HqlQueryFragment.class,
                new HqlQueryFragmentImpl(entityManager, domainType));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private RepositoryFragment<?> criteriaFragment(Class<?> domainType) {
        return RepositoryFragment.implemented(CriteriaQueryFragment.class,
                new CriteriaQueryFragmentImpl(entityManager, domainType));
    }
}
