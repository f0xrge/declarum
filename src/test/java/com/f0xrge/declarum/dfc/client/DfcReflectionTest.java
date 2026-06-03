package com.f0xrge.declarum.dfc.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DfcReflectionTest {

    @Test
    void shouldReportCompatibleFallbackMethodWhenPrimaryMethodIsMissing() {
        LegacyTypedObject object = new LegacyTypedObject();

        assertFalse(DfcReflection.hasCompatibleMethod(object, "isAttrNull", "declarum_event"));
        assertTrue(DfcReflection.hasCompatibleMethod(object, "isNull", "declarum_event"));
    }

    @Test
    void shouldInvokeDeclaredMethodsOnProxyLikeObjects() {
        ProxyLikeObject object = new ProxyLikeObject();

        Object value = DfcReflection.invoke(object, "declaredOnly", "declarum_event");

        assertEquals("declared:declarum_event", value);
    }

    private static final class LegacyTypedObject {

        public boolean isNull(String attributeName) {
            return "declarum_event".equals(attributeName);
        }
    }

    private static final class ProxyLikeObject {

        private String declaredOnly(String attributeName) {
            return "declared:" + attributeName;
        }
    }
}
