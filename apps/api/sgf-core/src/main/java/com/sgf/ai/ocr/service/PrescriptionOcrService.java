package com.sgf.ai.ocr.service;

import com.sgf.ai.ocr.dto.OcrResult;
import java.io.InputStream;

/**
 * Contrato del servicio de OCR para recetas médicas.
 */
public interface PrescriptionOcrService {

    /**
     * Procesa una imagen y extrae los datos estructurados de la receta.
     * @param imageStream Stream de la imagen (JPG, PNG, TIFF, PDF).
     * @param filename    Nombre del archivo (para inferir el tipo).
     */
    OcrResult extractFromImage(InputStream imageStream, String filename);
}
