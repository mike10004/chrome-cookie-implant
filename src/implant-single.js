document.addEventListener('DOMContentLoaded', function() {
    var cookieJsonTextArea = document.getElementById('cookie-json');
    var messageDiv = document.getElementById('message');
    var implantButton = document.getElementById('implant-single');
    implantButton.onclick = function() {
        var cookieJson = cookieJsonTextArea.value;
        var cookie;
        try {
            cookie = JSON.parse(cookieJson);
        } catch (err) {
            console.info("json parse error", err);
            messageDiv.innerText = 'JSON parse error: ' + err.message;
        }
        console.debug("implant-button onclick", cookie);
        cookieJson = JSON.stringify(cookie);
        var cookieJsonEncoded = encodeURIComponent(cookieJson);
        location.href = 'manage.html?implant=' + cookieJsonEncoded;
    };
});