[![Travis build status](https://img.shields.io/travis/mike10004/chrome-cookie-implant.svg)](https://travis-ci.org/mike10004/chrome-cookie-implant)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.mike10004/chrome-cookie-implant.svg)](https://repo1.maven.org/maven2/com/github/mike10004/chrome-cookie-implant/)

Chrome Cookie Implant
=====================

This is a Chrome extension that allows you to execute an HTTP GET request to
add cookies to your Chrome profile. The request is made to a `chrome-extension://` URL
whose host is the the extension ID and whose query parameters contain URL-encoded 
JSON objects that contain the cookie data.

This is useful in web testing because the Chrome Extensions API for cookie 
management is more powerful than the WebDriver cookies API. See the 
[Chrome cookies API](https://developer.chrome.com/extensions/cookies#method-set)
for a list of properties a cookie can have.

The companion Java client library uses Selenium to make the GET request to 
implant cookies.

Maven
-----

The Java client library (including the extension):

    <dependency>
        <groupId>com.github.mike10004</groupId>
        <artifactId>chrome-cookie-implant</artifactId>
        <version>1.5.9</version>
    </dependency>

If you want just the Chrome extension:

    <dependency>
        <groupId>com.github.mike10004</groupId>
        <artifactId>chrome-cookie-implant</artifactId>
        <version>1.5.9</version>
        <type>crx</type>
    </dependency>

Using the extension
-------------------

You can use the provided Java client library to install the extension and 
implant cookies, or you can use the extension in any language by grabbing the 
CRX artifact, installing it, and making your own HTTP requests. 

### With the Java client library

    import java.io.*;
    import java.util.Collections;
    import org.openqa.selenium.chrome.ChromeDriver;
    import org.openqa.selenium.chrome.ChromeOptions;
    import com.github.mike10004.chromecookieimplant.*;
    
    // ...

    File crxFile = File.createTempFile("chrome-cookie-implant", ".crx");
    ChromeCookieImplanter implanter = new ChromeCookieImplanter();
    try (OutputStream out = new FileOutputStream(crxFile)) {
        implanter.copyCrxTo(out);
    }
    ChromeOptions options = new ChromeOptions();
    options.addExtensions(crxFile);
    ChromeDriver driver = new ChromeDriver(options);
    ChromeCookie cookie = ChromeCookie.builder("https://www.example.com/")
            .name("my_cookie_name")
            .value("my_cookie_value")
            .build();
    CookieImplantResult result = implanter.implant(Collections.singleton(cookie), driver).get(0);
    System.out.println(result);
    driver.quit();

### With Selenium in another language

You can grab the [CRX artifact](https://repo1.maven.org/maven2/com/github/mike10004/chrome-cookie-implant/)
from the Maven repository and provide it to a webdriver in your preferred 
environment. 

The [src/test/js](https://github.com/mike10004/chrome-cookie-implant/tree/master/src/test/java)
directory contains this NodeJS example using the Selenium JavaScript bindings. 
See `package.json` for a declaration of dependencies.

    const {Builder, By, until} = require('selenium-webdriver');
    require('chromedriver');
    const crxData = require('fs').readFileSync('/path/to/chrome-cookie-implant.crx');
    const crxBase64 = new Buffer(crxData).toString('base64');
    const driver = new Builder()
        .forBrowser('chrome')
        .withCapabilities({
            'browserName': 'chrome',
            'chromeOptions': {
                extensions: [crxBase64]
            }
        }).build();
    
    const cookie = {
        url: "https://www.example.com/",
        domain: ".www.example.com",
        path: "/",
        name: "my_cookie_name",
        value: "my_cookie_value",
        expirationDate: (new Date().getTime() / 1000) + (24 * 60 * 60) // expires tomorrow
    };
    const encodedCookieJson = encodeURIComponent(JSON.stringify(cookie));
    const extensionId = "kaoadjmhchcekjlnhdmeennkgjeacdio";
    const cookieImplantUrl = "chrome-extension://" + extensionId + "/manage.html?implant=" + encodedCookieJson;
    driver.get(cookieImplantUrl);
    driver.findElement(By.id('output'))
        .then(outputElement => driver.wait(until.elementTextContains(outputElement, 'all_implants_processed')))
        .then(outputElement => outputElement.getText())
        .then(text => {
            const output = JSON.parse(text);
            const implant = output.implants[0];
            console.log('success', implant.success);
            if (implant.success) {
                console.log('cookie', implant.savedCookie);
            }
            return implant.savedCookie;
        }).then(() => driver.quit());

The Java client library makes things a bit easier because the extension file
is an embedded resource and you don't have to know the extension ID beforehand
in order to construct the appropriate URL.

Acknowledgements
----------------

Icon by Adèle Foucart, [CC 3.0](http://creativecommons.org/licenses/by/3.0/us/)
via [The Noun Project](https://thenounproject.com/term/chocolate-chip-cookie/261714/).
