package org.gravitywavetech.framework.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 字典项实体（测试夹具）
 *
 * <p>严格对应 {@code df_api_dict_item} 表结构，仅用于 {@code NativeQueryFragmentImpl}
 * 的单元测试，为 {@code findByNativeSql(sql, DfApiDictItem.class, ...)} 提供结果类型。</p>
 *
 * <pre>
 * CREATE TABLE `df_api_dict_item` (
 *   `id`                              bigint(50)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
 *   `dict_type_code`                  varchar(50) NOT NULL COMMENT '所属字典分类标识',
 *   `dict_label`                      varchar(200) NOT NULL COMMENT '字典名称(展示文本)',
 *   `dict_value`                      varchar(100) NOT NULL COMMENT '字典标识/编码',
 *   `sort`                            int(11)     NULL DEFAULT 0 COMMENT '排序号',
 *   `create_time`                     datetime    NULL DEFAULT CURRENT_TIMESTAMP,
 *   `update_time`                     datetime    NULL DEFAULT CURRENT_TIMESTAMP,
 *   `delete_status`                   varchar(1)  NULL COMMENT '删除标识 Y 删除 N 未删除',
 *   `sort_order`                      bigint(20)  NULL COMMENT '排序',
 *   `use_flag`                        varchar(1)  NULL,
 *   `creator_id`                      varchar(50) NULL,
 *   `updator_id`                      varchar(50) NULL,
 *   `ft_special_type_yj_dict_value`   varchar(50) NULL COMMENT '前端问题分类一级字典值',
 *   `ft_special_type_ej_dict_value`   varchar(50) NULL COMMENT '前端问题分类二级字典值'
 * );
 * </pre>
 */
@Getter
@Setter
@Entity
@Table(name = "df_api_dict_item")
public class DfApiDictItem {

    /** 主键ID */
    @Id
    @Column(name = "id")
    private Long id;

    /** 所属字典分类标识（关联 sys_dict_type.dict_type_code） */
    @Column(name = "dict_type_code", length = 50, nullable = false)
    private String dictTypeCode;

    /** 字典名称（展示文本：信访举报） */
    @Column(name = "dict_label", length = 200, nullable = false)
    private String dictLabel;

    /** 字典标识/编码（010、1、yuandan） */
    @Column(name = "dict_value", length = 100, nullable = false)
    private String dictValue;

    /** 排序号 */
    @Column(name = "sort")
    private Integer sort;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /** 删除标识 Y 删除 N 未删除 */
    @Column(name = "delete_status", length = 1)
    private String deleteStatus;

    /** 排序 */
    @Column(name = "sort_order")
    private Long sortOrder;

    /** 使用标识 */
    @Column(name = "use_flag", length = 1)
    private String useFlag;

    /** 创建人ID */
    @Column(name = "creator_id", length = 50)
    private String creatorId;

    /** 更新人ID */
    @Column(name = "updator_id", length = 50)
    private String updatorId;

    /** 前端问题分类一级字典值 */
    @Column(name = "ft_special_type_yj_dict_value", length = 50)
    private String ftSpecialTypeYjDictValue;

    /** 前端问题分类二级字典值 */
    @Column(name = "ft_special_type_ej_dict_value", length = 50)
    private String ftSpecialTypeEjDictValue;


    @Column(name = "parent_dict_value", length = 100, nullable = false)
    private String parentDictValue;

    @Column(name = "group_dict_value", length = 50, nullable = false)
    private String groupDictValue;
}
