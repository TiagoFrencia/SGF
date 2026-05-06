package com.sgf.modules.integrations.adesfa.domain;

import com.sgf.modules.core.BaseEntity;
import com.sgf.modules.sales.domain.Sale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "adesfa_validations")
public class AdesfaValidation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @Column(nullable = false)
    private String validatorCode;

    @Column(nullable = false)
    private String actionCode;

    @Column(nullable = false)
    private String affiliateNumber;

    @Column(nullable = false)
    private String prescriptionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdesfaValidationStatus status;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private BigDecimal patientAmount;

    @Column(nullable = false)
    private BigDecimal coverageAmount;

    @Column(nullable = false)
    private OffsetDateTime validatedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requestJson;

    @Column(columnDefinition = "TEXT")
    private String responseJson;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private String providerReference;
    private String integrationMode;
    private Integer lastHttpStatus;

    @Column(nullable = false)
    private boolean retryable;

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public String getValidatorCode() {
        return validatorCode;
    }

    public void setValidatorCode(String validatorCode) {
        this.validatorCode = validatorCode;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getAffiliateNumber() {
        return affiliateNumber;
    }

    public void setAffiliateNumber(String affiliateNumber) {
        this.affiliateNumber = affiliateNumber;
    }

    public String getPrescriptionNumber() {
        return prescriptionNumber;
    }

    public void setPrescriptionNumber(String prescriptionNumber) {
        this.prescriptionNumber = prescriptionNumber;
    }

    public AdesfaValidationStatus getStatus() {
        return status;
    }

    public void setStatus(AdesfaValidationStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPatientAmount() {
        return patientAmount;
    }

    public void setPatientAmount(BigDecimal patientAmount) {
        this.patientAmount = patientAmount;
    }

    public BigDecimal getCoverageAmount() {
        return coverageAmount;
    }

    public void setCoverageAmount(BigDecimal coverageAmount) {
        this.coverageAmount = coverageAmount;
    }

    public OffsetDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(OffsetDateTime validatedAt) {
        this.validatedAt = validatedAt;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public String getIntegrationMode() {
        return integrationMode;
    }

    public void setIntegrationMode(String integrationMode) {
        this.integrationMode = integrationMode;
    }

    public Integer getLastHttpStatus() {
        return lastHttpStatus;
    }

    public void setLastHttpStatus(Integer lastHttpStatus) {
        this.lastHttpStatus = lastHttpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }
}
