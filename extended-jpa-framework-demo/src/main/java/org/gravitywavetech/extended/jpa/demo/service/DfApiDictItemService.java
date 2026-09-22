package org.gravitywavetech.extended.jpa.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.extended.jpa.demo.entity.DfApiDictItem;
import org.gravitywavetech.extended.jpa.demo.repository.DfApiDictItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DfApiDictItemService
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/22
 */
@Slf4j
@Service
public class DfApiDictItemService {
    @Autowired
    private DfApiDictItemRepository dfApiDictItemRepository;

    @Transactional
    public void save(DfApiDictItem dfApiDictItem) {
        dfApiDictItemRepository.save(dfApiDictItem);
    }

    @Transactional
    public void saveAll(List<DfApiDictItem> items){
        this.dfApiDictItemRepository.saveAll(items);
    }
}
