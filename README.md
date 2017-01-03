Chrome Cookie Implant
=====================

This is an extension that allows you to add cookies to your Chrome profile 
with an HTTP GET request. The request URL is formed from the extension's ID
and query parameters that contain cookies in JSON format. (The JSON must be 
URL-encoded, of course.)

This is useful in web testing because the Chrome Extensions API has a more
powerful cookie management API than the Chrome WebDriver.

Building the extension
----------------------

Clone and make:

    $ git clone https://github.com/mike10004/chrome-cookie-implant
    $ cd chrome-cookie-implant
    $ ./make.sh

This prints the name of the `.crx` file created. The name is formed from 
the extension ID, the version, and the `.crx` suffix, for example
`neiaahbjfbepoclbammdhcailekhmcdm-1.1.crx`. 

Using the extension
-------------------

Some example Java code that demonstrates using the extension:

    import io.github.bonigarcia.wdm.ChromeDriverManager;
    import org.apache.http.client.utils.URIBuilder;
    import org.openqa.selenium.chrome.ChromeDriver;
    import org.openqa.selenium.chrome.ChromeOptions;
    
    import java.io.File;
    import java.net.URI;
    
    public class CookieImplantExample {
    
        public static void main(String[] args) throws Exception {
            ChromeDriverManager.getInstance().setup();
            ChromeOptions options = new ChromeOptions();
            String extensionId = "neiaahbjfbepoclbammdhcailekhmcdm";
            options.addExtensions(new File("/home/mike10004/chrome-cookie-implant/" + extensionId + "-1.1.crx"));
            ChromeDriver driver = new ChromeDriver(options);
            String cookieJson = "{" +
                    "\"url\":\"http://www.example.com/\"," +
                    "\"domain\":\".www.example.com\"," +
                    "\"path\":\"/\"," +
                    "\"name\":\"my_cookie_name\"," +
                    "\"value\":" +
                    "\"my_cookie_value\"," +
                    "\"expirationDate\":1513292605.379" +
            "}";
            URI uri = new URIBuilder(URI.create("chrome-extension://" + extensionId + "/manage.html"))
                    .addParameter("import", cookieJson)
                    .build();
            System.out.println(uri);
            driver.get(uri.toString());
        }
    
    }

See [Chrome cookies API](https://developer.chrome.com/extensions/cookies#method-set)
documentation on the properties the cookies can have.

Notes
-----

Icon by Adèle Foucart, [CC 3.0](http://creativecommons.org/licenses/by/3.0/us/)
via [The Noun Project](https://thenounproject.com/term/chocolate-chip-cookie/261714/).

