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
 * Sets the inner text of the output element to a string representation
 * of the argument object.
 * @param {MultiImportOutput} output output object
 */
function printOutput(output) {
    var outputDiv = document.getElementById('output');
    outputDiv.innerText = JSON.stringify(output, null, 2);
}

function createStageArray() {
    var stages = [
        'not_yet_processed',
        'some_imports_processed',
        'all_imports_processed'
    ];
    stages.first = stages[0];
    stages.middle = stages[1];
    stages.last = stages[stages.length - 1];
    return stages;
}

function isObject(thing) {
    return typeof(thing) === 'object';
}

/**
 * @param {object} input parsed query string
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
                    if (isObject(outCookie)) {
                        setCookieCallback(index, numImportSeeds, newCookie, true, outCookie, "OK");
                    } else {
                        var message = chrome.runtime.lastError.message;
                        console.info("chrome.cookies.set failed", chrome.runtime.lastError);
                        setCookieCallback(index, numImportSeeds, newCookie, false, outCookie, message);
                    }
                });
            } catch (err) {
                console.info("chrome.cookies.set threw exception", err);
                setCookieCallback(index, numImportSeeds, newCookie, false, null, err.toString());
            }
        } else {
            setCookieCallback(index, numImportSeeds, null, false, null, 'seed_parse_failed');
        }
    });
    return newCookies.length;
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

function MultiImportOutput(status, imports) {
    this.status = status;
    this.imports = imports || [];
}

function setReadableResult(result) {
    var resultDiv = document.getElementById('result');
    if (typeof(result) === 'string') {
        resultDiv.innerHTML = result;
    } else {
        while (resultDiv.lastChild) {
            resultDiv.removeChild(resultDiv.lastChild);
        }
        resultDiv.appendChild(result);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    var STAGES = createStageArray();
    var inputTable = document.createElement('table');
    inputTable.id = 'input-params';
    var query = parseQuery();
    var numQueryParams = 0;
    for (var name in query) {
        var values = query[name];
        values.forEach(value => {
            var nameCell = createTableCell(name);
            var valueCell = createTableCell(value, true);
            var row = document.createElement('tr');
            [nameCell, valueCell].forEach(cell => row.appendChild(cell));
            inputTable.appendChild(row);
            numQueryParams++;
        });
    }
    var inputDiv = document.getElementById('input');
    if (numQueryParams > 0) {
        inputDiv.appendChild(inputTable);
    } else {
        inputDiv.innerText = 'No cookie implants requested';
    }
    var resultTable = document.createElement('table');
    resultTable.id = 'import-results';
    var output = new MultiImportOutput(STAGES.first);
    printOutput(output);
    var resultTableHeader; 
    var numCookies = processImports(query, (index, numImportSeeds, newCookie, success, outCookie, s) => {
        console.debug('chrome.cookies.set', index, newCookie, success, outCookie, s);
        output.imports.push({
            'index': index,
            'success': success,
            'savedCookie': outCookie,
            'message': s || null
        });
        var cookieInfoCells = isObject(outCookie)
                ? [createTableCell(outCookie.domain), 
                    createTableCell(outCookie.path), 
                    createTableCell(outCookie.name), 
                    createTableCell(outCookie.value, true)]
                : ['', '', '', ''].map(createTableCell) ;
        var row = document.createElement('tr');
        cookieInfoCells.forEach(c => row.appendChild(c));
        if (typeof(resultTableHeader) === 'undefined') {
            resultTableHeader = document.createElement('tr');
            ['Domain', 'Path', 'Name', 'Value']
                    .map(t => createTableCell(t, false))
                    .forEach(c => resultTableHeader.appendChild(c));
            resultTable.appendChild(resultTableHeader);
        }
        resultTable.appendChild(row);
        if (output.imports.length == numImportSeeds) {
            output.status = STAGES.last;
            printOutput(output);
            setReadableResult(resultTable);
        } else {
            output.status = STAGES.middle;
            printOutput(output);
        }
    });
    if (numCookies === 0) {
        output.status = STAGES.last;
        printOutput(output);
        setReadableResult('No processing performed');
    }
});
