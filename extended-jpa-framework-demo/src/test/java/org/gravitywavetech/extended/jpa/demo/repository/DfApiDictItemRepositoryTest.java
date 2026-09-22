package org.gravitywavetech.extended.jpa.demo.repository;

import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.extended.jpa.demo.entity.DfApiDictItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DfApiDictItemRepositoryTest
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/8/7
 */
@Slf4j
@SpringBootTest
@Transactional
@Commit
public class DfApiDictItemRepositoryTest {
    @Autowired
    private DfApiDictItemRepository dfApiDictItemRepository;

    @Test
    public void findById(){
        Optional<DfApiDictItem> dfApiDictItemOptional = this.dfApiDictItemRepository.findById(1L);
        if(dfApiDictItemOptional.isPresent()){
            System.err.println(dfApiDictItemOptional.get().getDictLabel());
        }
    }


    @Test
    public void findByDictValueInAndParentDictValue(){
        List<String> list = new ArrayList<>();
        list.add("110");
        list.add("120");
        list.add("130");
        list.add("140");
        String parentDictValue = "0101";
        List<DfApiDictItem> items = dfApiDictItemRepository.findByDictValueInAndParentDictValue(list,parentDictValue);
        for(DfApiDictItem item:items){
            DfApiDictItem newItem = new DfApiDictItem();
            newItem.setDictLabel(item.getDictLabel());
            newItem.setDictTypeCode("wtflej");
            newItem.setDictValue(item.getDictValue());
            newItem.setParentDictValue("100");
            newItem.setCreateTime(LocalDateTime.now());
            newItem.setUpdateTime(LocalDateTime.now());
            newItem.setDeleteStatus(item.getDeleteStatus());
            newItem.setUseFlag(item.getUseFlag());
            newItem.setSortOrder(item.getSortOrder());
            newItem.setCreatorId("tomcat");
            newItem.setUpdatorId("tocmat");
            this.dfApiDictItemRepository.save(newItem);
        }
    }

    @Test
    public void saveCopy(){
        Optional<DfApiDictItem> item = this.dfApiDictItemRepository.findById(557L);
        if(item.isPresent()){
            DfApiDictItem newItem = new DfApiDictItem();
            newItem.setDictLabel(item.get().getDictLabel());
            newItem.setDictTypeCode("wtflej");
            newItem.setDictValue(item.get().getDictValue());
            newItem.setParentDictValue("100");
            newItem.setCreateTime(LocalDateTime.now());
            newItem.setUpdateTime(LocalDateTime.now());
            newItem.setDeleteStatus(item.get().getDeleteStatus());
            newItem.setUseFlag(item.get().getUseFlag());
            newItem.setSortOrder(item.get().getSortOrder());
            newItem.setCreatorId("tomcat");
            newItem.setUpdatorId("tocmat");
            this.dfApiDictItemRepository.save(newItem);
        }
    }

    @Test
    public void save(){
        DfApiDictItem item = new DfApiDictItem();
        item.setDictLabel("信访举报");
        item.setDictTypeCode("xsly");
        item.setDictValue("010");
        item.setParentDictValue("100");
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        item.setDeleteStatus("N");
        item.setCreatorId("tomcat");
        item.setUpdatorId("tocmat");
        item.setGroupDictValue("xsly");
        this.dfApiDictItemRepository.save(item);
        System.err.println("-------item-id:"+item.getId());
    }
}
