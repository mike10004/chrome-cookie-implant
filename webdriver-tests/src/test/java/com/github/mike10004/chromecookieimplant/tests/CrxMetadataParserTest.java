package com.github.mike10004.chromecookieimplant.tests;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;


public class CrxMetadataParserTest {

    private static final String KNOWN_ID = "neiaahbjfbepoclbammdhcailekhmcdm";

    @Test
    public void parse() throws Exception {
        File crxWithKnownIdFile = new File(getClass().getResource("/known-id.crx").toURI());
        CrxMetadataParser parser = new CrxMetadataParser();
        CrxMetadata md = parser.parse(crxWithKnownIdFile);
        System.out.format("parsed %s%n", md);
        assertEquals("id", KNOWN_ID, md.id);
    }

}