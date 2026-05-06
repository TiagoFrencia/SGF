package com.sgf.integrations.fhir.profiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

/**
 * FHIR PacienteAR — CORE-AR profile for patients.
 *
 * Based on HL7 Argentina CORE-AR implementation guide.
 * Argentine-specific fields:
 * - Tipo y número de documento (DNI/LE/LC/CI/PASAPORTE)
 * - Nacionalidad
 * - Cobertura de Obra Social con código de afiliado
 * - Sexo según DNI (Sistema Nacional de Identificación)
 * - Datos de contacto localizados
 *
 * See: https://www.argentina.gob.ar/salud/renapdis/fhir
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PacienteAR {

    private String resourceType = "Patient";
    private String id;
    private List<Identifier> identifier;
    private List<HumanName> name;
    private String gender; // male | female | other | unknown
    private String birthDate;
    private List<Address> address;
    private List<ContactPoint> telecom;
    private List<Contact> contact;
    private Meta meta;

    // --- Argentine extensions ---

    @JsonProperty("_tipoDocumento")
    private String tipoDocumento; // DNI | LE | LC | CI | PASAPORTE

    @JsonProperty("_nacionalidad")
    private String nacionalidad; // ISO 3166-1 alpha-2 (default: AR)

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Identifier> getIdentifier() { return identifier; }
    public void setIdentifier(List<Identifier> identifier) { this.identifier = identifier; }

    public List<HumanName> getName() { return name; }
    public void setName(List<HumanName> name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public List<Address> getAddress() { return address; }
    public void setAddress(List<Address> address) { this.address = address; }

    public List<ContactPoint> getTelecom() { return telecom; }
    public void setTelecom(List<ContactPoint> telecom) { this.telecom = telecom; }

    public List<Contact> getContact() { return contact; }
    public void setContact(List<Contact> contact) { this.contact = contact; }

    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }

    // --- Nested types ---

    public static class Identifier {
        private String system;
        private String value;
        private CodeableConcept type;
        private Period period;

        public Identifier() {}
        public Identifier(String system, String value) { this.system = system; this.value = value; }

        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public CodeableConcept getType() { return type; }
        public void setType(CodeableConcept type) { this.type = type; }
        public Period getPeriod() { return period; }
        public void setPeriod(Period period) { this.period = period; }
    }

    public static class HumanName {
        private String use; // official | usual | nickname
        private String family;
        private List<String> given;
        private String text;

        public String getUse() { return use; }
        public void setUse(String use) { this.use = use; }
        public String getFamily() { return family; }
        public void setFamily(String family) { this.family = family; }
        public List<String> getGiven() { return given; }
        public void setGiven(List<String> given) { this.given = given; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class Address {
        private String use; // home | work | temp | billing
        private String line;
        private String city;
        private String district; // Partido/Departamento
        private String state;
        private String postalCode;
        private String country; // ISO 3166-1

        public String getUse() { return use; }
        public void setUse(String use) { this.use = use; }
        public String getLine() { return line; }
        public void setLine(String line) { this.line = line; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }

    public static class ContactPoint {
        private String system; // phone | fax | email | url
        private String value;
        private String use; // home | work | mobile

        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getUse() { return use; }
        public void setUse(String use) { this.use = use; }
    }

    public static class Contact {
        private CodeableConcept relationship;
        private HumanName name;
        private List<ContactPoint> telecom;

        public CodeableConcept getRelationship() { return relationship; }
        public void setRelationship(CodeableConcept relationship) { this.relationship = relationship; }
        public HumanName getName() { return name; }
        public void setName(HumanName name) { this.name = name; }
        public List<ContactPoint> getTelecom() { return telecom; }
        public void setTelecom(List<ContactPoint> telecom) { this.telecom = telecom; }
    }

    public static class CodeableConcept {
        private List<Coding> coding;
        private String text;

        public List<Coding> getCoding() { return coding; }
        public void setCoding(List<Coding> coding) { this.coding = coding; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class Coding {
        private String system;
        private String code;
        private String display;

        public Coding() {}
        public Coding(String system, String code, String display) {
            this.system = system; this.code = code; this.display = display;
        }
        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDisplay() { return display; }
        public void setDisplay(String display) { this.display = display; }
    }

    public static class Period {
        private String start;
        private String end;
        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }
        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }

    public static class Meta {
        private String versionId;
        private String lastUpdated;
        private List<String> profile;
        private String source;

        public String getVersionId() { return versionId; }
        public void setVersionId(String versionId) { this.versionId = versionId; }
        public String getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
        public List<String> getProfile() { return profile; }
        public void setProfile(List<String> profile) { this.profile = profile; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}