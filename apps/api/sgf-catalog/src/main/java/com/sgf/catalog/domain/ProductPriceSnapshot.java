package com.sgf.catalog.domain;

import com.sgf.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "product_price_snapshots")
public class ProductPriceSnapshot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(nullable = false, length = 120)
    private String sourceRecordKey;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal pamiAffiliatePrice;

    private Integer pamiDiscountCode;
    private String pamiDiscountLabel;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceRecordKey() {
        return sourceRecordKey;
    }

    public void setSourceRecordKey(String sourceRecordKey) {
        this.sourceRecordKey = sourceRecordKey;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public BigDecimal getRetailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(BigDecimal retailPrice) {
        this.retailPrice = retailPrice;
    }

    public BigDecimal getPamiAffiliatePrice() {
        return pamiAffiliatePrice;
    }

    public void setPamiAffiliatePrice(BigDecimal pamiAffiliatePrice) {
        this.pamiAffiliatePrice = pamiAffiliatePrice;
    }

    public Integer getPamiDiscountCode() {
        return pamiDiscountCode;
    }

    public void setPamiDiscountCode(Integer pamiDiscountCode) {
        this.pamiDiscountCode = pamiDiscountCode;
    }

    public String getPamiDiscountLabel() {
        return pamiDiscountLabel;
    }

    public void setPamiDiscountLabel(String pamiDiscountLabel) {
        this.pamiDiscountLabel = pamiDiscountLabel;
    }
}
