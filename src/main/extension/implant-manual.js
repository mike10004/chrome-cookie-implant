document.addEventListener('DOMContentLoaded', function() {
    var exampleCookie = {
        url: 'https://www.example.com/',
        name: 'foo'
    };
    var cookieJsonTextArea = document.getElementById('cookie-json');
    cookieJsonTextArea.innerText = JSON.stringify(exampleCookie);
    var messageDiv = document.getElementById('message');
    var implantButton = document.getElementById('implant');
    implantButton.onclick = function() {
        var cookiesJson = cookieJsonTextArea.value;
        var cookieOrCookies;
        try {
            cookieOrCookies = JSON.parse(cookiesJson);
        } catch (err) {
            console.info("json parse error", err);
            messageDiv.innerText = 'JSON parse error: ' + err.message;
        }
        console.debug("implant button onclick", cookieOrCookies);
        var cookies = cookieOrCookies;
        if (!Array.isArray(cookieOrCookies)) {
            cookies = [cookieOrCookies];
        }
        var url = 'manage.html?';
        cookies.forEach(cookie => {
            var cookieJson = JSON.stringify(cookie);
            var cookieJsonEncoded = encodeURIComponent(cookieJson);
            url += ('implant=' + cookieJsonEncoded + '&');
        });
        location.href = url;
    };
});