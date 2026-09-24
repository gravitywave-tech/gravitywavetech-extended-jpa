package org.gravitywavetech.order.domain.repository;

import org.gravitywavetech.order.infrastructure.repository.jpa.DfApiDictItem;

import java.util.List;

/**
 * DfApiDictItemRepository
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public interface DfApiDictItemRepository {
    public void save(DfApiDictItem dfApiDictItem);

    public void saveAll(List<DfApiDictItem> items);
}
