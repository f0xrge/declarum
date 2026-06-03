package com.f0xrge.declarum.dfc.repository;

import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.client.DfcReflection;
import com.f0xrge.declarum.dfc.session.DfcDocumentumSession;
import com.f0xrge.declarum.dfc.session.DocumentumSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DfcRepositoryObjectOperations implements RepositoryObjectOperations {

    @Override
    public List<RepositoryObjectSnapshot> findByQualification(DocumentumSession session, String dqlQualification) {
        Object dfcSession = requireDfcSession(session);
        String queryText = "select r_object_id from " + requireText(dqlQualification, "dqlQualification is required");
        Object query = DfcReflection.newInstance("com.documentum.fc.client.DfQuery");
        DfcReflection.invoke(query, "setDQL", queryText);

        Object readQuery = DfcReflection.staticField("com.documentum.fc.client.IDfQuery", "DF_READ_QUERY");
        Object collection = DfcReflection.invoke(query, "execute", dfcSession, readQuery);
        try {
            List<RepositoryObjectSnapshot> snapshots = new ArrayList<>();
            while (Boolean.TRUE.equals(DfcReflection.invoke(collection, "next"))) {
                String objectId = DfcReflection.invoke(collection, "getString", "r_object_id").toString();
                snapshots.add(snapshotById(dfcSession, objectId));
            }
            return snapshots;
        } finally {
            DfcReflection.invoke(collection, "close");
        }
    }

    @Override
    public Optional<RepositoryObjectSnapshot> findByPath(DocumentumSession session, String path) {
        Object dfcSession = requireDfcSession(session);
        Object object = DfcReflection.invoke(dfcSession, "getObjectByPath", requireText(path, "path is required"));
        if (object == null) {
            return Optional.empty();
        }
        return Optional.of(toSnapshot(object));
    }

    @Override
    public RepositoryObjectSnapshot create(DocumentumSession session, String objectType, Map<String, Object> attributes, String folderPath) {
        Object dfcSession = requireDfcSession(session);
        Object object = DfcReflection.invoke(dfcSession, "newObject", requireText(objectType, "objectType is required"));
        applyAttributes(object, safeAttributes(attributes));
        if (folderPath != null && !folderPath.isBlank()) {
            DfcReflection.invoke(object, "link", folderPath);
        }
        DfcReflection.invoke(object, "save");
        return toSnapshot(object);
    }

    @Override
    public RepositoryObjectSnapshot updateAttributes(DocumentumSession session, String objectId, Map<String, Object> attributes) {
        Object object = getObjectById(requireDfcSession(session), requireText(objectId, "objectId is required"));
        applyAttributes(object, safeAttributes(attributes));
        DfcReflection.invoke(object, "save");
        return toSnapshot(object);
    }

    @Override
    public void delete(DocumentumSession session, String objectId) {
        Object object = getObjectById(requireDfcSession(session), requireText(objectId, "objectId is required"));
        DfcReflection.invoke(object, "destroy");
    }

    private RepositoryObjectSnapshot snapshotById(Object dfcSession, String objectId) {
        return toSnapshot(getObjectById(dfcSession, objectId));
    }

    private Object getObjectById(Object dfcSession, String objectId) {
        Object dfId = DfcReflection.newInstance("com.documentum.fc.common.DfId", String.class, objectId);
        return DfcReflection.invoke(dfcSession, "getObject", dfId);
    }

    private RepositoryObjectSnapshot toSnapshot(Object object) {
        String objectId = DfcReflection.invoke(DfcReflection.invoke(object, "getObjectId"), "toString").toString();
        String objectType = DfcReflection.invoke(object, "getTypeName").toString();
        return new RepositoryObjectSnapshot(objectId, objectType, readAttributes(object));
    }

    private Map<String, Object> readAttributes(Object object) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        int attributeCount = (Integer) DfcReflection.invoke(object, "getAttrCount");
        for (int index = 0; index < attributeCount; index++) {
            Object attribute = DfcReflection.invoke(object, "getAttr", index);
            String attributeName = DfcReflection.invoke(attribute, "getName").toString();
            if (Boolean.TRUE.equals(DfcReflection.invoke(object, "isAttrRepeating", attributeName))) {
                attributes.put(attributeName, readRepeatingAttribute(object, attributeName));
            } else if (!isAttributeNull(object, attributeName)) {
                attributes.put(attributeName, readScalarAttribute(object, attributeName));
            }
        }
        return attributes;
    }

    private boolean isAttributeNull(Object object, String attributeName) {
        if (DfcReflection.hasCompatibleMethod(object, "isAttrNull", attributeName)) {
            return Boolean.TRUE.equals(DfcReflection.invoke(object, "isAttrNull", attributeName));
        }
        if (DfcReflection.hasCompatibleMethod(object, "isNull", attributeName)) {
            return Boolean.TRUE.equals(DfcReflection.invoke(object, "isNull", attributeName));
        }
        throw new IllegalStateException("Unable to invoke DFC method: isAttrNull or isNull");
    }

    private Object readScalarAttribute(Object object, String attributeName) {
        int dataType = (Integer) DfcReflection.invoke(object, "getAttrDataType", attributeName);
        if (dataType == dfcAttributeType("DM_BOOLEAN")) {
            return DfcReflection.invoke(object, "getBoolean", attributeName);
        }
        if (dataType == dfcAttributeType("DM_INTEGER")) {
            return DfcReflection.invoke(object, "getInt", attributeName);
        }
        if (dataType == dfcAttributeType("DM_DOUBLE")) {
            return DfcReflection.invoke(object, "getDouble", attributeName);
        }
        return DfcReflection.invoke(object, "getString", attributeName);
    }

    private List<Object> readRepeatingAttribute(Object object, String attributeName) {
        int valueCount = (Integer) DfcReflection.invoke(object, "getValueCount", attributeName);
        int dataType = (Integer) DfcReflection.invoke(object, "getAttrDataType", attributeName);
        List<Object> values = new ArrayList<>();
        for (int index = 0; index < valueCount; index++) {
            if (dataType == dfcAttributeType("DM_BOOLEAN")) {
                values.add(DfcReflection.invoke(object, "getRepeatingBoolean", attributeName, index));
            } else if (dataType == dfcAttributeType("DM_INTEGER")) {
                values.add(DfcReflection.invoke(object, "getRepeatingInt", attributeName, index));
            } else if (dataType == dfcAttributeType("DM_DOUBLE")) {
                values.add(DfcReflection.invoke(object, "getRepeatingDouble", attributeName, index));
            } else {
                values.add(DfcReflection.invoke(object, "getRepeatingString", attributeName, index));
            }
        }
        return values;
    }

    private int dfcAttributeType(String fieldName) {
        return (Integer) DfcReflection.staticField("com.documentum.fc.common.IDfAttr", fieldName);
    }

    private void applyAttributes(Object object, Map<String, Object> attributes) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Iterable<?> iterable) {
                setRepeatingAttribute(object, entry.getKey(), iterable);
            } else {
                setScalarAttribute(object, entry.getKey(), value);
            }
        }
    }

    private void setRepeatingAttribute(Object object, String attributeName, Iterable<?> values) {
        DfcReflection.invoke(object, "removeAll", attributeName);
        for (Object value : values) {
            appendScalarAttribute(object, attributeName, value);
        }
    }

    private void setScalarAttribute(Object object, String attributeName, Object value) {
        Objects.requireNonNull(value, "attribute value is required");
        if (value instanceof Boolean booleanValue) {
            DfcReflection.invoke(object, "setBoolean", attributeName, booleanValue);
        } else if (value instanceof Integer integerValue) {
            DfcReflection.invoke(object, "setInt", attributeName, integerValue);
        } else if (value instanceof Long longValue) {
            DfcReflection.invoke(object, "setString", attributeName, longValue.toString());
        } else if (value instanceof Float floatValue) {
            DfcReflection.invoke(object, "setDouble", attributeName, Double.valueOf(floatValue));
        } else if (value instanceof Double doubleValue) {
            DfcReflection.invoke(object, "setDouble", attributeName, doubleValue);
        } else {
            DfcReflection.invoke(object, "setString", attributeName, value.toString());
        }
    }

    private void appendScalarAttribute(Object object, String attributeName, Object value) {
        Objects.requireNonNull(value, "attribute value is required");
        if (value instanceof Boolean booleanValue) {
            DfcReflection.invoke(object, "appendBoolean", attributeName, booleanValue);
        } else if (value instanceof Integer integerValue) {
            DfcReflection.invoke(object, "appendInt", attributeName, integerValue);
        } else if (value instanceof Long longValue) {
            DfcReflection.invoke(object, "appendString", attributeName, longValue.toString());
        } else if (value instanceof Float floatValue) {
            DfcReflection.invoke(object, "appendDouble", attributeName, Double.valueOf(floatValue));
        } else if (value instanceof Double doubleValue) {
            DfcReflection.invoke(object, "appendDouble", attributeName, doubleValue);
        } else {
            DfcReflection.invoke(object, "appendString", attributeName, value.toString());
        }
    }

    private Object requireDfcSession(DocumentumSession session) {
        if (session instanceof DfcDocumentumSession dfcDocumentumSession) {
            return dfcDocumentumSession.getDfcSession();
        }
        throw new IllegalArgumentException("DfcRepositoryObjectOperations requires DfcDocumentumSession");
    }

    private Map<String, Object> safeAttributes(Map<String, Object> attributes) {
        return attributes == null ? Map.of() : attributes;
    }

    private String requireText(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
