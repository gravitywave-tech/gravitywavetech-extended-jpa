package org.gravitywavetech.framework.jpa.repository;

import org.gravitywavetech.framework.jpa.entity.DfApiDictItem;

import java.util.List;

/**
 * DfApiDictItemRepository
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/18
 */
public interface DfApiDictItemRepository extends ExtendedBaseRepository<DfApiDictItem, Long>{
    /** 根据分类编码，查询有效字典项，按sort_order排序 */
    List<DfApiDictItem> findByDictTypeCodeAndDeleteStatusAndUseFlagOrderBySortOrderAsc(
            String dictTypeCode, String deleteStatus, String useFlag);


    /**
     * dictValue IN (dictValueList) AND parentDictValue = parentDictValue
     * @param dictValueList 字典值集合
     * @param parentDictValue 父级字典值
     * @return 字典项列表
     */
    List<DfApiDictItem> findByDictValueInAndParentDictValue(List<String> dictValueList, String parentDictValue);

    /**
     * @description:
     * @author: sinosoft
     * @date: 2026/8/21 17:42
     * @param: dictValue
     * @param: parentDictValue
     * @return: com.sinosoft.discipline.dangfeng.entity.DfApiDictItem
     **/
    public DfApiDictItem findByDictValueAndParentDictValue(String dictValue,String parentDictValue);

    /**
     * @description:
     * @author: sinosoft
     * @date: 2026/8/21 17:42
     * @param: dictTypeCode
     * @param: dictValue
     * @return: com.sinosoft.discipline.dangfeng.entity.DfApiDictItem
     **/
    public DfApiDictItem findByDictTypeCodeAndDictValue(String dictTypeCode,String dictValue);

    /**
     * @description:
     * @author: sinosoft
     * @date: 2026/8/21 17:42
     * @param: dictTypeCode
     * @param: dictValue
     * @return: com.sinosoft.discipline.dangfeng.entity.DfApiDictItem
     **/
    public List<DfApiDictItem> findByDictTypeCodeAndDictValueIn(String dictTypeCode, List<String> dictValues);

    public List<DfApiDictItem> findByDictTypeCodeAndFtSpecialTypeEjDictValueAndGroupDictValue(String dictTypeCode,String FtSpecialTypeEjDictValue,String groupDictValue);

    public List<DfApiDictItem> findByDictTypeCodeAndFtSpecialTypeYjDictValueAndGroupDictValue(String dictTypeCode,String FtSpecialTypeYjDictValue,String groupDictValue);
}
