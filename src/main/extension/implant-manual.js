document.addEventListener('DOMContentLoaded', function() {
    const exampleCookie = {
        url: 'https://www.example.com/',
        name: 'foo'
    };
    const cookieJsonTextArea = document.getElementById('cookie-json');
    cookieJsonTextArea.innerText = JSON.stringify(exampleCookie);
    const messageDiv = document.getElementById('message');
    const implantButton = document.getElementById('implant');
    implantButton.onclick = function() {
        const cookiesJson = cookieJsonTextArea.value;
        let cookieOrCookies;
        try {
            cookieOrCookies = JSON.parse(cookiesJson);
        } catch (err) {
            console.info("json parse error", err);
            messageDiv.innerText = 'JSON parse error: ' + err.message;
        }
        console.debug("implant button onclick", cookieOrCookies);
        let cookies = cookieOrCookies;
        if (!Array.isArray(cookieOrCookies)) {
            cookies = [cookieOrCookies];
        }
        let url = 'manage.html?';
        cookies.forEach(cookie => {
            const cookieJson = JSON.stringify(cookie);
            const cookieJsonEncoded = encodeURIComponent(cookieJson);
            url += ('implant=' + cookieJsonEncoded + '&');
        });
        location.href = url;
    };
});