const {Builder, By, until} = require('selenium-webdriver');
require('chromedriver');
const fs = require('fs');
const crxData = fs.readFileSync('../../../target/classes/chrome-cookie-implant.crx');
const crxBase64 = new Buffer(crxData).toString('base64');

const driver = new Builder()
    .forBrowser('chrome')
    .withCapabilities({
        'browserName': 'chrome',
        'chromeOptions': {
            extensions: [crxBase64]
        }
    })
    .build();

driver.get('data:text/plain;charset=utf-8,hello%2C%20world');

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
