package com.github.mike10004.chromecookieimplant;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;


public class CrxMetadataParserTest {

    private static final String KNOWN_ID = "neiaahbjfbepoclbammdhcailekhmcdm";
    private static final String KNOWN_ID_RESOURCE_PATH = "/known-id.crx";

    @Test
    public void parse() throws Exception {
        File crxWithKnownIdFile = getKnownIDCrxFile();
        CrxMetadataParser parser = new CrxMetadataParser();
        CrxMetadata md = parser.parse(crxWithKnownIdFile);
        System.out.format("parsed %s%n", md);
        assertEquals("id", KNOWN_ID, md.id);
    }

    private static File getKnownIDCrxFile() throws FileNotFoundException, URISyntaxException {
        URL url = CrxMetadataParserTest.class.getResource(KNOWN_ID_RESOURCE_PATH);
        if (url == null) {
            throw new FileNotFoundException("classpath:" + KNOWN_ID_RESOURCE_PATH);
        }
        return new File(url.toURI());
    }
}