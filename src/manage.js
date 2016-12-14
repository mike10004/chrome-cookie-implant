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
            list.push(decodeURIComponent(pair[1]));
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

/**
 * @param {function(number, object, boolean, object, string)} setCookieCallback the cookie callback; 
 *      parameters are cookie index (integer), input cookie object (or null), import success (boolean), 
 *      output cookie object (or null if !success), error info (string)
 */
function processImports(input, setCookieCallback) {
    var cookieImports = input['import'] || [];
    var numImportSeeds = cookieImports.length;
    var newCookies = cookieImports
        .map(importSeed => parseImportSeed(importSeed, input));
    var numGoodCookies = newCookies.filter(c => !!c).length;
    console.debug(numGoodCookies + " good cookies among " + numImportSeeds + " import seeds");
    newCookies.forEach((newCookie, index) => {
        if (newCookie) {
            try {
                chrome.cookies.set(newCookie, outCookie => {
                    setCookieCallback(index, numImportSeeds, newCookie, true, outCookie, "OK");
                });
            } catch (err) {
                console.info("chrome.cookies.set failed", err);
                setCookieCallback(index, numImportSeeds, newCookie, false, null, err.toString());
            }
        } else {
            setCookieCallback(index, numImportSeeds, null, false, null, 'seed_parse_failed');
        }
    })
}

function createTableCell(value, wrapTextInInput) {
    var cell = document.createElement('td');
    if (wrapTextInInput) {
        var input = document.createElement('input');
        input.type = 'text';
        input.value = value;
        cell.appendChild(input);
    } else {
        cell.innerText = value;
    }
    return cell;
}

document.addEventListener('DOMContentLoaded', function() {
    var inputTable = document.createElement('table');
    inputTable.id = 'input-params';
    var query = parseQuery();
    for (var name in query) {
        var values = query[name];
        values.forEach(value => {
            var nameCell = createTableCell(name);
            var valueCell = createTableCell(value, true);
            var row = document.createElement('tr');
            [nameCell, valueCell].forEach(cell => row.appendChild(cell));
            inputTable.appendChild(row);
        });
    }
    var inputDiv = document.getElementById('input');
    inputDiv.appendChild(inputTable);
    var resultTable = document.createElement('table');
    resultTable.id = 'import-results';
    var output = {
        imports: []
    };
    var outputDiv = document.getElementById('output');
    processImports(query, (index, numImportSeeds, newCookie, success, outCookie, s) => {
        console.debug('chrome.cookies.set', index, newCookie, success, outCookie, s);
        output.imports.push({
            'index': index,
            'success': success,
            'savedCookie': outCookie,
            'message': s || null
        });
        var cookieInfoCells = [createTableCell(outCookie.domain), createTableCell(outCookie.path), createTableCell(outCookie.name), createTableCell(outCookie.value, true)];
        var row = document.createElement(tag || 'tr');
        cookieInfoCells.forEach(c => row.appendChild(c));
        resultTable.appendChild(row);
        if (output.imports.length == numImportSeeds) {
            outputDiv.innerText = JSON.stringify(output, null, 2);
            var resultDiv = document.getElementById('result');
            resultDiv.appendChild(resultTable);
        }
    });
});