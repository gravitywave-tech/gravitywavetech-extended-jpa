package org.gravitywavetech.extended.jpa.repository;

import org.gravitywavetech.extended.jpa.demo.entity.DfApiDictItem;
import org.gravitywavetech.extended.jpa.demo.entity.PsLawcase;
import org.gravitywavetech.extended.jpa.repository.ExtendedBaseRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @EnableJpaRepositories} 接线验证。
 *
 * <p>本测试证明 {@link org.gravitywavetech.extended.jpa.demo.config.JpaRepositoryConfig} 已被真正激活，
 * 即：{@link PsLawcaseRepository} 由 {@link ExtendedBaseRepositoryImpl} 承担实现，
 * 而不是 Spring Data 默认的 {@code SimpleJpaRepository}。</p>
 *
 * <p>判定依据分三层，逐层收紧：</p>
 * <ol>
 *     <li>代理背后的 target 类型是否为 {@link ExtendedBaseRepositoryImpl}；</li>
 *     <li>Native / HQL / Criteria 三类查询入口是否都有实现绑定；</li>
 *     <li>Native SQL 是否能端到端跑通（验证 EntityManager 与领域类型已被正确注入基类）。</li>
 * </ol>
 *
 * <p>其中第 2 层是决定性的：{@link ExtendedJpaRepositoryFactory} 对继承
 * {@link ExtendedBaseRepository} 的仓储会<b>跳过</b>查询片段注入，因此这些方法只可能
 * 由 {@code repositoryBaseClass} 提供。若接线缺失，Spring Data 会在启动期直接抛出
 * “No implementation for … was bound”，上下文根本起不来。</p>
 */
@SpringBootTest
@Transactional
class JpaRepositoryWiringTest {

    @Autowired
    private DfApiDictItemRepository repository;

    @Test
    @DisplayName("接线生效：仓储基类为 ExtendedBaseRepositoryImpl 而非 SimpleJpaRepository")
    void repositoryIsBackedByExtendedBaseRepositoryImpl() throws Exception {
        Class<?> targetClass = resolveTargetClass(repository);

        assertThat(ExtendedBaseRepositoryImpl.class.isAssignableFrom(targetClass))
                .as("仓储实现类应为 ExtendedBaseRepositoryImpl，实际为 %s", targetClass.getName())
                .isTrue();
    }

    @Test
    @DisplayName("三类动态查询入口均已绑定实现")
    void allQueryFragmentsAreBound() {
        assertThat(repository.nativeQuery()).isNotNull();
        assertThat(repository.hqlQuery()).isNotNull();
        assertThat(repository.criteriaQuery()).isNotNull();
    }

    @Test
    @DisplayName("Native SQL 端到端可用")
    void nativeSqlRunsEndToEnd() {
        DfApiDictItem entity = new DfApiDictItem();
        entity.setDictLabel("信访举报");
        entity.setDictTypeCode("xsly");
        entity.setDictValue("010");
        entity.setParentDictValue("100");
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleteStatus("N");
        entity.setCreatorId("tomcat");
        entity.setUpdatorId("tocmat");
        entity.setGroupDictValue("xsly");
        repository.saveAndFlush(entity);

        List<Map<String, Object>> rows =
                repository.findByNativeSqlForMap("select count(*) as total_count from df_api_dict_item");

        assertThat(rows).hasSize(1);

        Object count = rows.get(0).values().iterator().next();
        assertThat(count).isInstanceOf(Number.class);
        assertThat(((Number) count).longValue()).isEqualTo(1L);
    }

    /**
     * 取出 AOP 代理背后的真实 target 类型。
     *
     * <p>Spring Data 仓储由 {@code ProxyFactory} 创建，必然实现 {@link Advised}，
     * 因此可直接向下取到 target；取不到时退化为代理自身的用户类型。</p>
     */
    private static Class<?> resolveTargetClass(Object repository) throws Exception {
        if (repository instanceof Advised advised) {
            Object target = advised.getTargetSource().getTarget();
            if (target != null) {
                return ClassUtils.getUserClass(target);
            }
        }
        return ClassUtils.getUserClass(repository);
    }
}
