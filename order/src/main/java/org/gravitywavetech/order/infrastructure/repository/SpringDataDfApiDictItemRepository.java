package org.gravitywavetech.order.infrastructure.repository;

import org.gravitywavetech.order.domain.repository.DfApiDictItemRepository;
import org.gravitywavetech.order.infrastructure.repository.jpa.DfApiDictItem;
import org.gravitywavetech.order.infrastructure.repository.jpa.DfApiDictItemJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SpringDataDfApiDictItemRepository
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@Component
public class SpringDataDfApiDictItemRepository implements DfApiDictItemRepository {
    @Autowired
    private DfApiDictItemJpaRepository dfApiDictItemJpaRepository;

    @Override
    public void save(DfApiDictItem dfApiDictItem) {
        dfApiDictItemJpaRepository.save(dfApiDictItem);
    }

    @Override
    public void saveAll(List<DfApiDictItem> items) {
        dfApiDictItemJpaRepository.saveAll(items);
    }
}
