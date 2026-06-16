package com.wbs.project.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JpstnRoleMappingTest {

    @Test
    void inferRoleCode_ba_returnsDeptProjectManager() {
        assertEquals("dept-project-manager", JpstnRoleMapping.inferRoleCode("BA"));
    }

    @Test
    void inferRoleCode_bf_returnsProjectManager() {
        assertEquals("project-manager", JpstnRoleMapping.inferRoleCode("BF"));
    }

    @Test
    void inferRoleCode_unknownCd_returnsNull() {
        assertNull(JpstnRoleMapping.inferRoleCode("X1"));
        assertNull(JpstnRoleMapping.inferRoleCode(""));
        assertNull(JpstnRoleMapping.inferRoleCode(null));
    }

    @Test
    void describe_ba_returnsReadableText() {
        String text = JpstnRoleMapping.describe("BA");
        assertNotNull(text);
        assertTrue(text.contains("BA"));
        assertTrue(text.contains("部门项目负责人"));
    }

    @Test
    void describe_unknownCd_returnsNull() {
        assertNull(JpstnRoleMapping.describe("X1"));
        assertNull(JpstnRoleMapping.describe(null));
    }
}