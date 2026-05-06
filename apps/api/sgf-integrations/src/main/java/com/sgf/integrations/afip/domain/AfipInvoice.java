package com.sgf.integrations.afip.domain;

import com.sgf.core.domain.BaseEntity;
import com.sgf.pos.domain.Sale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "afip_invoices")
public class AfipInvoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @Column(nullable = false)
    private Integer pointOfSale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AfipInvoiceType invoiceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AfipDocumentType customerDocumentType;

    @Column(nullable = false)
    private String customerDocumentNumber;

    @Column(nullable = false)
    private String currencyCode;

    @Column(nullable = false)
    private BigDecimal netAmount;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AfipInvoiceStatus status;

    private Long voucherNumberFrom;
    private Long voucherNumberTo;
    private String cae;
    private LocalDate caeDueDate;
    private String providerReference;
    private String afipResultCode;

    @Column(columnDefinition = "TEXT")
    private String observationsJson;

    @Column(columnDefinition = "TEXT")
    private String errorsJson;

    private String lastErrorCode;

    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(nullable = false)
    private Integer retryCount;

    private OffsetDateTime lastAttemptedAt;
    private OffsetDateTime tokenExpiresAt;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requestJson;

    @Column(columnDefinition = "TEXT")
    private String responseJson;

    private OffsetDateTime authorizedAt;

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public Integer getPointOfSale() {
        return pointOfSale;
    }

    public void setPointOfSale(Integer pointOfSale) {
        this.pointOfSale = pointOfSale;
    }

    public AfipInvoiceType getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(AfipInvoiceType invoiceType) {
        this.invoiceType = invoiceType;
    }

    public AfipDocumentType getCustomerDocumentType() {
        return customerDocumentType;
    }

    public void setCustomerDocumentType(AfipDocumentType customerDocumentType) {
        this.customerDocumentType = customerDocumentType;
    }

    public String getCustomerDocumentNumber() {
        return customerDocumentNumber;
    }

    public void setCustomerDocumentNumber(String customerDocumentNumber) {
        this.customerDocumentNumber = customerDocumentNumber;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public AfipInvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(AfipInvoiceStatus status) {
        this.status = status;
    }

    public Long getVoucherNumberFrom() {
        return voucherNumberFrom;
    }

    public void setVoucherNumberFrom(Long voucherNumberFrom) {
        this.voucherNumberFrom = voucherNumberFrom;
    }

    public Long getVoucherNumberTo() {
        return voucherNumberTo;
    }

    public void setVoucherNumberTo(Long voucherNumberTo) {
        this.voucherNumberTo = voucherNumberTo;
    }

    public String getCae() {
        return cae;
    }

    public void setCae(String cae) {
        this.cae = cae;
    }

    public LocalDate getCaeDueDate() {
        return caeDueDate;
    }

    public void setCaeDueDate(LocalDate caeDueDate) {
        this.caeDueDate = caeDueDate;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public String getAfipResultCode() {
        return afipResultCode;
    }

    public void setAfipResultCode(String afipResultCode) {
        this.afipResultCode = afipResultCode;
    }

    public String getObservationsJson() {
        return observationsJson;
    }

    public void setObservationsJson(String observationsJson) {
        this.observationsJson = observationsJson;
    }

    public String getErrorsJson() {
        return errorsJson;
    }

    public void setErrorsJson(String errorsJson) {
        this.errorsJson = errorsJson;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public OffsetDateTime getLastAttemptedAt() {
        return lastAttemptedAt;
    }

    public void setLastAttemptedAt(OffsetDateTime lastAttemptedAt) {
        this.lastAttemptedAt = lastAttemptedAt;
    }

    public OffsetDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(OffsetDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public OffsetDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(OffsetDateTime authorizedAt) {
        this.authorizedAt = authorizedAt;
    }
}
