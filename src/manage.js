// https://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
function parseQuery() {
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    var obj = {};
    for (var i=0;i<vars.length;i++) {
        if (vars[i]) {
            var pair = vars[i].split("=", 2);
            var list = obj[pair[0]];
            if (typeof(list) === 'undefined') {
                obj[pair[0]] = list = [];
            }
            list.push(pair[1]);
        }
    }
    return obj;
}

/**
 * Parse a cookie seed.
 * @param {string} importSeed - the import seed
 * @param {object} query - the query object, which may specify options of how to interpret the seed
 * @param {function(string,object,err)} - function to which bad seeds are passed
 * @returns {object|boolean} - the cookie, or false if the seed could not be parsed
 */
function parseImportSeed(importSeed, query, badSeedHandler) {
    badSeedHandler = badSeedHandler || (x => false);
    var result;
    try {
        result = JSON.parse(importSeed);
    } catch (err) {
        console.info("failed to interpret cookie seed", importSeed, query, err);
        result = badSeedHandler(importSeed, query, err);
    }
    return result;
}

function processImports(input, setCookieCallback) {
    var cookieImports = input['import'] || [];
    var newCookies = cookieImports
        .map(importSeed => parseImportSeed(importSeed, input))
        .filter(c => !!c);
    if (newCookies.length === cookieImports.length) {
        newCookies.forEach(newCookie => {
            chrome.cookies.set(newCookie, setCookieCallback);
        })
    } else {
        console.info("aborting cookie import due to seed parsing failures on " + (cookieImports.length - newCookies.length) + " seeds");
    }
}

function createTableCell(value) {
    var cell = document.createElement('td');
    cell.innerText = value;
    return cell;
}

function createTableRow(values, tag) {
    var row = document.createElement(tag || 'tr');
    values.map(createTableCell).forEach(cell => row.appendChild(cell));
    return row;
}

document.addEventListener('DOMContentLoaded', function() {
    var inputTable = document.createElement('table');
    inputTable.id = 'input-params';
    var query = parseQuery();
    for (var name in query) {
        inputTable.appendChild(createTableRow([name, query[name]]));
    }
    var inputDiv = document.getElementById('input');
    inputDiv.appendChild(inputTable);
    var outputTable = document.createElement('table');
    outputTable.id = 'import-results';
    processImports(query, s => {
        console.debug('chrome.cookies.set', newCookie, s);
        var cookieInfo = [newCookie.domain, newCookie.path, newCookie.name, newCookie.value];
        outputTable.appendChild(createTableRow(cookieInfo));
    });
    var outputDiv = document.getElementById('output');
    outputDiv.appendChild(outputTable);
});