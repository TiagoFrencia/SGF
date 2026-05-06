package com.sgf.integrations.vademecum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Suggests generic substitutes for branded pharmaceutical products.
 *
 * Argentine law (Ley 25.649 — Prescripción por Nombre Genérico) mandates that
 * prescriptions must use the generic drug name (DCI: Denominación Común Internacional),
 * and pharmacists must offer the patient the option to choose among available brands
 * and generics with the same active ingredient, form, and concentration.
 *
 * This service:
 * - Finds same-IFA products across AlfaBeta, Kairos, and local catalog
 * - Ranks by price (lowest first) and bioequivalence status
 * - Respects ANMAT bioequivalence certification
 * - Complies with Ley 25.649 dispensing obligations
 */
@Service
public class GenericSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(GenericSuggestionService.class);

    private final AlfaBetaConnector alfaBetaConnector;
    private final KairosConnector kairosConnector;

    /**
     * In-memory index: activeIngredient → list of GTINs.
     * Rebuilt daily during vademecum sync.
     */
    private final ConcurrentHashMap<String, List<String>> ifaIndex = new ConcurrentHashMap<>();

    public GenericSuggestionService(AlfaBetaConnector alfaBetaConnector,
                                     KairosConnector kairosConnector) {
        this.alfaBetaConnector = alfaBetaConnector;
        this.kairosConnector = kairosConnector;
    }

    /**
     * Find generic alternatives for a given product GTIN.
     * Matches by active ingredient, pharmaceutical form, and concentration.
     *
     * @param prescribedGtin The GTIN of the prescribed (or brand) product
     * @return Ranked list of alternatives, cheapest first
     */
    public SuggestionResult findAlternatives(String prescribedGtin) {
        // Get prescribed product details
        var alfabeta = alfaBetaConnector.findByGtin(prescribedGtin);
        var kairos = kairosConnector.findByGtin(prescribedGtin);

        String activeIngredient = null;
        String pharmaceuticalForm = null;
        String concentration = null;
        String prescribedName = prescribedGtin;

        if (alfabeta.isPresent()) {
            var p = alfabeta.get();
            activeIngredient = p.activeIngredient();
            pharmaceuticalForm = p.pharmaceuticalForm();
            concentration = p.concentration();
            prescribedName = p.commercialName();
        }
        if ((activeIngredient == null || concentration == null) && kairos.isPresent()) {
            var p = kairos.get();
            if (activeIngredient == null) activeIngredient = p.activeIngredient();
            if (concentration == null) concentration = p.concentration();
            if (pharmaceuticalForm == null) pharmaceuticalForm = p.pharmaceuticalForm();
        }

        if (activeIngredient == null) {
            return new SuggestionResult(prescribedGtin, prescribedName, List.of(),
                    "No se pudo determinar el principio activo para búsqueda de genéricos");
        }

        // Search both sources
        List<GenericAlternative> alternatives = new ArrayList<>();

        // AlfaBeta search by IFA
        List<AlfaBetaConnector.AlfaBetaProduct> alfaResults =
                alfaBetaConnector.findByActiveIngredient(activeIngredient, 20);
        for (var p : alfaResults) {
            if (p.gtin().equals(prescribedGtin)) continue;
            if (differentFormOrConcentration(p, pharmaceuticalForm, concentration)) continue;
            alternatives.add(new GenericAlternative(
                    p.gtin(), p.commercialName(), p.laboratory(),
                    null, // price from Kairos
                    p.pharmaceuticalForm(), p.concentration(),
                    false, // bioequivalence status from Kairos
                    "AlfaBeta"
            ));
        }

        // Kairos bioequivalent alternatives
        List<KairosConnector.KairosProduct> bioResults =
                kairosConnector.getBioequivalentAlternatives(prescribedGtin, 20);
        for (var p : bioResults) {
            if (p.gtin().equals(prescribedGtin)) continue;
            // Merge or add
            boolean merged = false;
            for (int i = 0; i < alternatives.size(); i++) {
                if (alternatives.get(i).gtin().equals(p.gtin())) {
                    alternatives.set(i, new GenericAlternative(
                            p.gtin(), p.commercialName(), p.laboratory(),
                            p.retailPrice(), p.pharmaceuticalForm(), p.concentration(),
                            p.isBioequivalent(), "AlfaBeta+Kairos"
                    ));
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                alternatives.add(new GenericAlternative(
                        p.gtin(), p.commercialName(), p.laboratory(),
                        p.retailPrice(), p.pharmaceuticalForm(), p.concentration(),
                        p.isBioequivalent(), "Kairos"
                ));
            }
        }

        // Sort: bioequivalent first, then cheapest
        alternatives.sort(Comparator
                .comparing(GenericAlternative::isBioequivalent).reversed()
                .thenComparing(a -> {
                    if (a.retailPrice() == null) return BigDecimal.valueOf(Double.MAX_VALUE);
                    try {
                        return new BigDecimal(a.retailPrice());
                    } catch (Exception e) {
                        return BigDecimal.valueOf(Double.MAX_VALUE);
                    }
                })
        );

        return new SuggestionResult(
                prescribedGtin, prescribedName, alternatives,
                alternatives.isEmpty()
                        ? "No se encontraron alternativas genéricas para " + activeIngredient
                        : "Se encontraron " + alternatives.size() + " alternativas"
        );
    }

    /**
     * Find cheapest alternative for a given active ingredient.
     */
    public GenericAlternative findCheapest(String activeIngredient) {
        List<AlfaBetaConnector.AlfaBetaProduct> results =
                alfaBetaConnector.findByActiveIngredient(activeIngredient, 10);
        if (results.isEmpty()) return null;

        return results.stream()
                .filter(p -> p.gtin() != null)
                .map(p -> {
                    // Try to get pricing from Kairos
                    var kairos = kairosConnector.findByGtin(p.gtin());
                    String price = kairos.map(KairosConnector.KairosProduct::retailPrice).orElse(null);
                    boolean bioeq = kairos.map(KairosConnector.KairosProduct::isBioequivalent).orElse(false);
                    return new GenericAlternative(
                            p.gtin(), p.commercialName(), p.laboratory(),
                            price, p.pharmaceuticalForm(), p.concentration(),
                            bioeq, "AlfaBeta+Kairos"
                    );
                })
                .min(Comparator.comparing(a -> {
                    if (a.retailPrice() == null) return BigDecimal.valueOf(Double.MAX_VALUE);
                    try {
                        return new BigDecimal(a.retailPrice());
                    } catch (Exception e) {
                        return BigDecimal.valueOf(Double.MAX_VALUE);
                    }
                }))
                .orElse(null);
    }

    /**
     * Check if two products are therapeutically equivalent (same IFA, form, concentration).
     */
    public boolean areTherapeuticallyEquivalent(String gtin1, String gtin2) {
        var p1 = alfaBetaConnector.findByGtin(gtin1);
        var p2 = alfaBetaConnector.findByGtin(gtin2);

        if (p1.isEmpty() || p2.isEmpty()) return false;

        var a = p1.get();
        var b = p2.get();

        return sameOrNull(a.activeIngredient(), b.activeIngredient())
                && sameOrNull(a.pharmaceuticalForm(), b.pharmaceuticalForm())
                && sameOrNull(a.concentration(), b.concentration());
    }

    /**
     * Rebuild the in-memory IFA → GTIN index.
     * Called during scheduled vademecum sync.
     */
    public void rebuildIndex(Map<String, List<String>> newIndex) {
        ifaIndex.clear();
        ifaIndex.putAll(newIndex);
        log.info("Generic suggestion index rebuilt: {} IFAs indexed", ifaIndex.size());
    }

    private boolean differentFormOrConcentration(AlfaBetaConnector.AlfaBetaProduct p,
                                                  String form, String concentration) {
        if (form != null && p.pharmaceuticalForm() != null
                && !p.pharmaceuticalForm().equalsIgnoreCase(form)) {
            return true;
        }
        if (concentration != null && p.concentration() != null
                && !p.concentration().equalsIgnoreCase(concentration)) {
            return true;
        }
        return false;
    }

    private boolean sameOrNull(String a, String b) {
        if (a == null || b == null) return true;
        return a.equalsIgnoreCase(b);
    }

    // --- Types ---

    /**
     * A generic or branded alternative to the prescribed product.
     */
    public record GenericAlternative(
            String gtin,
            String commercialName,
            String laboratory,
            String retailPrice,
            String pharmaceuticalForm,
            String concentration,
            boolean isBioequivalent,
            String source
    ) {
        public String priceLabel() {
            if (retailPrice == null) return "N/D";
            return "$" + retailPrice;
        }

        public String bioequivalenceLabel() {
            return isBioequivalent ? "Bioequivalente ✓" : "—";
        }
    }

    /**
     * Complete suggestion result for a product.
     */
    public record SuggestionResult(
            String prescribedGtin,
            String prescribedName,
            List<GenericAlternative> alternatives,
            String message
    ) {
        public boolean hasAlternatives() {
            return !alternatives.isEmpty();
        }

        public GenericAlternative cheapest() {
            return alternatives.isEmpty() ? null : alternatives.get(0);
        }

        public List<GenericAlternative> bioequivalentOnly() {
            return alternatives.stream()
                    .filter(GenericAlternative::isBioequivalent)
                    .toList();
        }
    }
}