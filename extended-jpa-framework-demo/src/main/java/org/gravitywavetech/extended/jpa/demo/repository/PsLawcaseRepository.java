package org.gravitywavetech.extended.jpa.demo.repository;

import org.gravitywavetech.extended.jpa.demo.entity.PsLawcase;
import org.gravitywavetech.extended.jpa.repository.ExtendedBaseRepository;

/**
 * {@link ExtendedBaseRepository} 的落地样板仓储。
 *
 * <p>本接口不声明任何方法，仅通过继承即可获得：完整 CRUD、{@code JpaSpecificationExecutor}
 * 以及 Native SQL / HQL / Criteria 三类动态查询能力，用于验证
 * {@code repositoryBaseClass} 接线是否真正生效。</p>
 *
 * @author gravitywavetech
 */
public interface PsLawcaseRepository extends ExtendedBaseRepository<PsLawcase, String> {
}
