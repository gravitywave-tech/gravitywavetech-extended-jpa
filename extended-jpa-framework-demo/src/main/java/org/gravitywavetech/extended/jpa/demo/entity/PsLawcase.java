package org.gravitywavetech.extended.jpa.demo.entity;//package com.sinosoft.discipline.dangfeng.entity;

import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.Date;

import org.gravitywavetech.extended.jpa.entity.BaseEntity;

@Getter
@Setter
@Entity
@Table(name="ps_lawcase")
public class PsLawcase extends BaseEntity<String> {


    /**
     * 单机版导入的id
     */
    @Column(name = "dj_id", length = 50)
    private String djId;
    /**
     * 上报地区编码(必填)
     */
    @Column(name = "branch_inner_code", length = 25)
    private String branchInnerCode;

    /**
     * 上报地区名称(必填)
     */
    @Column(name = "branch_name", length = 25)
    private String branchName;

    @Column(name = "clue_id", length = 50)
    private String clueId;// 线索（案件）ID
    /**
     * 线索（案件）编码(必填)
     */
    @Column(name = "caseno", length = 50)
    private String caseno;
    /**
     * 创建人姓名(必填)
     */
    @Column(name = "creation_name", length = 50)
    private String creationName;
    /**
     * 创建部门名称(必填)
     */
    @Column(name = "dept_name", length = 50)
    private String deptName;
    /**
     * 创建部门编码
     */
    @Column(name = "inner_code", length = 50)
    private String innerCode;

    /**
     * 审核状态
     */
    @Column(name = "is_approve", length = 18)
    private String isApprove;

    /**
     * 上报月份
     */
    @Column(name = "month", length = 18)
    private String month;
    /**
     * 主要问题线索（简要案情）
     */

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /**
     * 线索（案件）标题(必填)
     */
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    /**
     * 案件（线索）来源【字典值】(必填)
     */
    @Column(name = "clue_source", length = 50)
    private String clueSource;
    /**
     * 是否发生节点  "1":是,"2"否(必填)
     */
    @Column(name = "is_festival", length = 5)
    private String isFestival;
    /**
     * 节点类型【字典值】(必填)
     */
    @Column(name = "festival_type", length = 30)
    private String festivalType;


    /**
     * 上报年份
     */
    @Column(name = "year", length = 18)
    private String year;
    /**
     * 年月=上报年份+“-”+上报月份(必填)
     */
    @Column(name = "tj_date", length = 18)
    private String tjDate;
    /**
     * 草稿 1 才为草稿
     */
    @Column(name = "draft", length = 18)
    private String draft;//
    /**
     * 是否回退  1:是   2:否
     */
    @Column(name = "return_status", length = 5)
    private String returnStatus;

    /**
     * 回退意见
     */
    @Column(name = "return_advice",columnDefinition = "TEXT")
    private String returnAdvice;
    /**
     * 是否上报 0 未上报   1已上报    2 上报后修改
     */
    @Column(name = "is_report", length = 5)
    private String isReport;

    /**
     * 单位编码
     */
    @Column(name = "region_Code", length = 30)
    private String regionCode;
    /**
     * 单位名称
     */
    @Column(name = "region_name", length = 100)
    private String regionName;

    /**
     * 党风数据迁移是否需要解密标识（党风系统数据有部分字段加密，派驻没有）
     */
    @Column(name = "decrypt_flag", length = 5)
    private String decryptFlag;
    /**
     * 是否导入数据1是2否
     */
    @Column(name = "is_import", length = 5)
    private String isImport;
    /**
     * 是否锁定数据 1已锁定    其他是未锁定
     */
    @Column(name = "is_lock", length = 5)
    private String isLock;
    /**
     * 上报时间
     */
    @Column(name = "operate_date")
    private Date operateDate;


    /**
     * 案件（线索）来源时间(必填)
     */
    @Column(name = "ajlysj", length = 50)
    private String ajlysj;
    /**
     * 督办类型
     */
    @Column(name = "supervised_type", length = 50)
    private String supervisedType;

    /**
     * 密级标识(1非密,2秘密,3机密)
     */
    @Column(name = "mjbs", length = 5)
    private String mjbs;
    /**
     * 密级(汉字)
     */
    @Transient
    private String mjbsName;


//    @com.sinosoft.discipline.dangfeng.util.JsonToString(value = "mjbs", label = "mjbsName")
//    private String mjbsJson;

    @Id
    private String id = generateId();
}
