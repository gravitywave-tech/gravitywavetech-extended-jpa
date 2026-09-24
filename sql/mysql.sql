
CREATE TABLE t_order (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         buyer_id BIGINT NOT NULL,
                         status VARCHAR(32) NOT NULL,
                         total_amount DECIMAL(12,2) NOT NULL,
                         province VARCHAR(32),
                         city VARCHAR(32),
                         detail_address VARCHAR(256)
);

CREATE TABLE t_order_item(
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             product_name VARCHAR(255),
                             quantity INT,
                             unit_price DECIMAL(12,2)
);

CREATE TABLE t_payment (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           order_ref_id BIGINT NOT NULL COMMENT '引用订单id（不属于本表外键！只是引用）',
                           amount DECIMAL(12,2) NOT NULL,
                           payment_method VARCHAR(20) NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           third_party_trade_no VARCHAR(128) NULL COMMENT '第三方支付流水号'
);