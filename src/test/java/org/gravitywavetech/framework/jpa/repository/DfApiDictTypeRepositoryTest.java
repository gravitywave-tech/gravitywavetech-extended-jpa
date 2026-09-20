//package org.gravitywavetech.framework.jpa.repository;
//
//import com.sinosoft.discipline.dangfeng.entity.DfApiDictType;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.List;
//
///**
// * DfApiDictTypeRepositoryTest
// *
// * <p></p>
// *
// * @author Administrator
// * @version 1.0
// * @since 2026/8/21
// */
//@Slf4j
//@SpringBootTest
//@RunWith(SpringRunner.class)
//public class DfApiDictTypeRepositoryTest {
//    @Autowired
//    private DfApiDictTypeRepository dictTypeRepository;
//
//    @Test
//    public void findByDeleteStatusAndUseFlagOrderBySortOrderAsc(){
//        List<DfApiDictType> list = dictTypeRepository.findByDeleteStatusAndUseFlagOrderBySortOrderAsc("N","Y");
//        System.err.println("size: "+list.size());
//    }
//}
