package com.sgf.ai.ocr.dto;

import java.util.List;

/**
 * Resultado del procesamiento OCR de una receta médica.
 * Los campos son opcionales (null si no se pudo extraer).
 */
public record OcrResult(
        String rawText,               // Texto crudo extraído de la imagen
        String prescriptionId,         // Número de formulario / receta
        String beneficiaryId,          // Nro credencial / DNI del afiliado
        String doctorLicense,          // Matrícula del médico prescriptor
        String doctorName,             // Nombre del médico
        List<OcrMedicationItem> medications, // Medicamentos detectados
        double confidenceScore,        // Confianza del OCR (0.0 a 1.0)
        boolean parsedSuccessfully     // Si el parseo estructurado fue exitoso
) {

    /**
     * Medicamento detectado en la receta.
     */
    public record OcrMedicationItem(
            String rawDescription,  // Texto original
            String detectedName,    // Nombre del medicamento inferido
            Integer quantity        // Cantidad, si se detectó
    ) {}

    /** Resultado vacío ante un fallo total. */
    public static OcrResult failed(String rawText) {
        return new OcrResult(rawText, null, null, null, null, List.of(), 0.0, false);
    }
}
