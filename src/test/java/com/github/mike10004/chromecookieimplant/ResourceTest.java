package com.github.mike10004.chromecookieimplant;

import io.github.mike10004.crxtool.CrxInventory;
import io.github.mike10004.crxtool.CrxMetadata;
import io.github.mike10004.crxtool.CrxParser;
import io.github.mike10004.crxtool.CrxVersion;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ResourceTest {

    @Test
    public void testPluginBuiltExtensionInClasspath() throws Exception {
        URL resource = getClass().getResource("/chrome-cookie-implant.crx");
        assertNotNull("resource not in test output dir", resource);
    }

    @Test
    public void testExtensionIsCrxVersion3() throws Exception {
        URL resource = getClass().getResource("/chrome-cookie-implant.crx");
        checkState(resource != null, "resource not found");
        CrxInventory inventory;
        try (InputStream in = resource.openStream()) {
            inventory = CrxParser.getDefault().parseInventory(in);
        }
        CrxMetadata metadata = inventory.metadata();
        assertEquals("metadata says version 3", CrxVersion.CRX3, metadata.getCrxVersion());
    }

}
