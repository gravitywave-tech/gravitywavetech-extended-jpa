package org.gravitywavetech.extended.jpa.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.extended.jpa.demo.entity.DfApiDictItem;
import org.gravitywavetech.extended.jpa.demo.repository.DfApiDictItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DfApiDictItemServiceTest
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/22
 */
@Slf4j
@SpringBootTest
public class DfApiDictItemServiceTest {
    @Autowired
    private DfApiDictItemService dfApiDictItemService;

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
        this.dfApiDictItemService.save(item);
        System.err.println("-------item-id:"+item.getId());
    }

    @Test
    public void saveAll(){
        List<DfApiDictItem> items = new ArrayList<>();
        for(int i=0;i<10;i++){
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
            items.add(item);
        }
        this.dfApiDictItemService.saveAll(items);
    }
}
