(function(browser){

    /**
     * Value class that represents the result of multiple implant attempts.
     * @param status overall status
     * @param implants individual implant
     * @constructor
     */
    function CookieImplantOutput(status, implants) {

        // noinspection JSUnusedGlobalSymbols
        /**
         * See CookieProcessingStatus for possible values.
         */
        this.status = status;
        this.implants = implants || [];
    }

    /**
     * Enumeration of constants representing possible values for the status field of the CookieImplantOutput class.
     */
    const CookieProcessingStatus = {
        not_yet_processed: 'not_yet_processed',
        some_implants_processed: 'some_implants_processed',
        all_implants_processed: 'all_implants_processed',
        values: function () {
            return ['not_yet_processed', 'some_implants_processed', 'all_implants_processed'];
        }
    };

    function createStageArray() {
        const stages = CookieProcessingStatus.values();
        stages.first = stages[0];
        stages.middle = stages[1];
        stages.last = stages[stages.length - 1];
        return stages;
    }

    /**
     * Parses a query string into an object mapping parameter names to values.
     * Inspired by https://stackoverflow.com/questions/901115/how-can-i-get-query-string-values-in-javascript
     */
    function parseQuery() {
        const query = window.location.search.substring(1);
        const vars = query.split("&");
        const obj = {};
        for (let i=0; i<vars.length; i++) {
            if (vars[i]) {
                const pair = vars[i].split("=", 2);
                let list = obj[pair[0]];
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
     * @param {string} implantSeed - the implant seed
     * @param {object} query - the query object, which may specify options of how to interpret the seed
     * @param {function(string,object,err)} [badSeedHandler] function to which bad seeds are passed
     * @returns {object|boolean} - the cookie, or false if the seed could not be parsed
     */
    function parseImplantSeed(implantSeed, query, badSeedHandler) {
        badSeedHandler = badSeedHandler || (() => false);
        let result;
        try {
            result = JSON.parse(implantSeed);
        } catch (err) {
            console.info("failed to interpret cookie seed", implantSeed, query, err);
            result = badSeedHandler(implantSeed, query, err);
        }
        return result;
    }

    /**
     * Sets the inner text of the output element to a string representation
     * of the argument object.
     * @param {CookieImplantOutput} output output object
     */
    function printOutput(output) {
        const outputDiv = document.getElementById('output');
        outputDiv.innerText = JSON.stringify(output, null, 2);
    }

    function isObject(thing) {
        return thing !== null && typeof(thing) === 'object';
    }

    function parseBoolean(value) {
        if (typeof(value) === 'boolean') {
            return value;
        }
        if (typeof(value) === 'string') {
            value = value.toLowerCase();
            return value === 'true' || value === '1';
        }
        if (typeof(value) === 'number') {
            return value !== 0;
        }
        return false;
    }

    function isExpired(cookie, referenceDate) {
        const queryTimeInSeconds = cookie.expirationDate;
        if (typeof(queryTimeInSeconds) === 'number') {
            const referenceTimeInSeconds = referenceDate.getTime() / 1000;
            const expired = queryTimeInSeconds <= referenceTimeInSeconds;
            return expired;
        }
        return false;
    }

    /**
     * Processes implant attempts.
     * @param {object} input parsed query string
     * @param {function(number, number, object, boolean, object, string)} setCookieCallback the cookie callback;
     *      parameters are cookie index (integer), input cookie object (or null), implant success (boolean),
     *      output cookie object (or null if !success), error info (string)
     */
    function processImplants(input, setCookieCallback) {
        let disableExpiryCheck = parseBoolean(input['disable_expiry_check']);
        const cookieImplants = input['implant'] || [];
        const numImplantSeeds = cookieImplants.length;
        const newCookies = cookieImplants
            .map(implantSeed => parseImplantSeed(implantSeed, input));
        const numGoodCookies = newCookies.filter(c => !!c).length;
        console.debug(numGoodCookies, " good cookies among ", numImplantSeeds, " implant seeds");
        newCookies.forEach((newCookie, index) => {
            if (newCookie) {
                try {
                    const now = new Date();
                    if (!disableExpiryCheck && isExpired(newCookie, now)) {
                        setCookieCallback(index, numImplantSeeds, newCookie, false, null, 'ignored:expired');
                    } else {
                        browser.cookies.set(newCookie, outCookie => {
                            if (isObject(outCookie)) {
                                setCookieCallback(index, numImplantSeeds, newCookie, true, outCookie, "OK");
                            } else {
                                console.info("chrome.cookies.set failed", browser.runtime.lastError);
                                const message = (browser.runtime.lastError || {}).message;
                                setCookieCallback(index, numImplantSeeds, newCookie, false, outCookie, message || "failed:cookies_set_failed_without_revealing_why");
                            }
                        });
                    }
                } catch (err) {
                    console.info("chrome.cookies.set threw exception", err);
                    setCookieCallback(index, numImplantSeeds, newCookie, false, null, err.toString());
                }
            } else {
                setCookieCallback(index, numImplantSeeds, null, false, null, 'failed:parse_seed');
            }
        });
        return newCookies.length;
    }

    function createTableCell(value, wrapTextInInput) {
        const cell = document.createElement('td');
        if (wrapTextInInput) {
            const input = document.createElement('input');
            input.type = 'text';
            input.value = value;
            cell.appendChild(input);
        } else {
            cell.innerText = value;
        }
        return cell;
    }

    function setReadableResult(result) {
        const resultDiv = document.getElementById('result');
        if (typeof(result) === 'string') {
            resultDiv.innerHTML = result;
        } else {
            while (resultDiv.lastChild) {
                resultDiv.removeChild(resultDiv.lastChild);
            }
            resultDiv.appendChild(result);
        }
    }

    function createInputTable(query) {
        const inputTable = document.createElement('table');
        inputTable.id = 'input-params';
        let numQueryParams = 0;
        for (const name of Object.keys(query)) {
            const values = query[name];
            values.forEach(value => {
                const nameCell = createTableCell(name);
                const valueCell = createTableCell(value, true);
                const row = document.createElement('tr');
                [nameCell, valueCell].forEach(cell => row.appendChild(cell));
                inputTable.appendChild(row);
                numQueryParams++;
            });
        }
        inputTable.numQueryParams = numQueryParams;
        return inputTable;
    }

    function createResultTableHeader() {
        const resultTableHeader = document.createElement('tr');
        ['#', 'Domain', 'Path', 'Name', 'Value']
            .map(t => createTableCell(t, false))
            .forEach(c => resultTableHeader.appendChild(c));
        return resultTableHeader;
    }

    function createResultTableRow(index, newCookie, success, outCookie, message) {
        const cookieInfoCells = isObject(outCookie)
            ? [createTableCell(index),
                createTableCell(outCookie.domain),
                createTableCell(outCookie.path),
                createTableCell(outCookie.name),
                createTableCell(outCookie.value, true)]
            : [index, '', '', '', ''].map(createTableCell);
        const row = document.createElement('tr');
        cookieInfoCells.forEach(c => row.appendChild(c));
        return row;
    }

    document.addEventListener('DOMContentLoaded', function() {
        const query = parseQuery();
        const STAGES = createStageArray();
        const inputTable = createInputTable(query);
        const inputDiv = document.getElementById('input');
        if (inputTable.numQueryParams > 0) {
            inputDiv.appendChild(inputTable);
        } else {
            inputDiv.innerText = 'No cookie implants requested';
        }
        const resultTable = document.createElement('table');
        resultTable.id = 'implant-results';
        const output = new CookieImplantOutput(STAGES.first);
        printOutput(output);
        let resultTableHeader;
        const numCookies = processImplants(query, (index, numImplantSeeds, newCookie, success, outCookie, s) => {
            console.debug('chrome.cookies.set', index, newCookie, success, outCookie, s);
            output.implants.push({
                'index': index,
                'success': success,
                'savedCookie': outCookie,
                'message': s || null
            });
            const row = createResultTableRow(index, newCookie, success, outCookie, s);
            if (typeof(resultTableHeader) === 'undefined') {
                resultTableHeader = createResultTableHeader();
                resultTable.appendChild(resultTableHeader);
            }
            resultTable.appendChild(row);
            if (output.implants.length === numImplantSeeds) {
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
})(window['browser'] || window['chrome']);
