package org.gravitywavetech.order.domain.model;

import lombok.EqualsAndHashCode;

/**
 * Address
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */

@EqualsAndHashCode
public class Address {
    private final String province;
    private final String city;
    private final String detail;

    public Address(String province, String city, String detail) {
        this.province = province;
        this.city = city;
        this.detail = detail;
    }

    // ==========新增这个静态工厂方法==========
    public static Address of(String province, String city, String detail) {
        return new Address(province, city, detail);
    }

    //getter
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getDetail() { return detail; }
}


