
// This function is from the Chrome Extension tutorial at 
// https://developer.chrome.com/extensions/getstarted.
// Copyright (c) 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
/**
 * Get the current URL. 
 * 
 * @param {function(string)} callback - called when the URL of the current tab
 *   is found.
 */
function getCurrentTabUrl(callback) {
    const queryInfo = {
        active: true,
        currentWindow: true
    };

    browser.tabs.query(queryInfo, function(tabs) {
      const tab = tabs[0];
      let url = tab.url;
      console.assert(typeof url === 'string', 'tab.url should be a string');
    callback(url);
  });

}

const renderStatus = console.debug;

function parseHost(url) {
    const a = document.createElement('a');
    a.href = url;
  return a.host;
}

function isManageable(host) {
  return host === 'localhost' || host.indexOf('.') >= 0;
}

document.addEventListener('DOMContentLoaded', function() {
  getCurrentTabUrl(function(url) {
    renderStatus('Popup loaded');
      const manageCurrentLink = document.getElementById('manage-current');
      const host = parseHost(url);
      renderStatus('host', host);
    if (isManageable(host)) {
      manageCurrentLink.href = manageCurrentLink.href + encodeURIComponent(host);
      manageCurrentLink.hidden = false;
    } else {
      renderStatus('host not manageable', host);
    }
  });
});
