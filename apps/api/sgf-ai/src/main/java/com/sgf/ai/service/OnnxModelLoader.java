package com.sgf.ai.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import java.nio.FloatBuffer;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Loads and manages ONNX Runtime sessions for AI model execution.
 */
@Component
public class OnnxModelLoader {

    private static final Logger log = LoggerFactory.getLogger(OnnxModelLoader.class);

    private OrtEnvironment env;
    private OrtSession session;

    @PostConstruct
    public void init() {
        try {
            this.env = OrtEnvironment.getEnvironment();
            ClassPathResource modelResource = new ClassPathResource("models/demand_forecast_v1.onnx");
            
            if (modelResource.exists()) {
                byte[] modelBytes = modelResource.getContentAsByteArray();
                this.session = env.createSession(modelBytes);
                log.info("Successfully loaded ONNX model: demand_forecast_v1.onnx");
            } else {
                log.warn("ONNX model demand_forecast_v1.onnx not found. AI forecasting will fallback to statistical models.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize ONNX Runtime: {}", e.getMessage());
        }
    }

    public boolean isModelLoaded() {
        return session != null;
    }

    public float predict(float[] inputData) {
        if (session == null) return -1.0f;

        try {
            long[] shape = new long[]{1, inputData.length};
            FloatBuffer buffer = FloatBuffer.wrap(inputData);
            OnnxTensor tensor = OnnxTensor.createTensor(env, buffer, shape);
            
            try (OrtSession.Result results = session.run(Collections.singletonMap("input", tensor))) {
                float[][] output = (float[][]) results.get(0).getValue();
                return output[0][0];
            }
        } catch (Exception e) {
            log.error("ONNX inference failed: {}", e.getMessage());
            return -1.0f;
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception e) {
            log.error("Error cleaning up ONNX resources: {}", e.getMessage());
        }
    }
}
