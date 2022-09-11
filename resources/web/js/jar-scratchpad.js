let socket = null;
let snip = {};
let socketIsConnected = false;
let changesQueued = false;
let closeAction = () => { };
let shouldClickToNewTab = false;
let jar = {};

import {CodeJar} from 'https://cdn.jsdelivr.net/npm/codejar@3.4.0/codejar.min.js';
import {withLineNumbers} from 'https://cdn.jsdelivr.net/npm/codejar@3.4.0/linenumbers.js';
//import {Prism} from '/js/prism.js';

//import {highlight} from 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.23.0/prism.min.js';
//import {highlightjs} from 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.18.1/highlight.min.js';

//import {Prism} from 'prismjs';
//need to import prism and/or highlight here? don't think they're getting loaded in lol F
//looks like may need to call HighlightElement on update too

const debounce = (callback, wait) => {
  let timeoutId = null;
  return (...args) => {
    window.clearTimeout(timeoutId);
    timeoutId = window.setTimeout(() => {
      callback.apply(null, args);
    }, wait);
  };
}

function loadJar() {
    const editor = document.querySelector('#editor');
    jar = CodeJar(editor, withLineNumbers(Prism.highlightElement));    
    // const logHighlight = (e) => {
    //     console.log("highlighting");
    //     Prism.highlightElement(e);
    // };
    // jar = CodeJar(editor, logHighlight);
    jar.onUpdate((data) => {
        //check if content has actually changed before firing anything
        if(snip.content !== data) {
            snip.content = data;
            editSnipDebounced();
        }
    });
}

function onUpdate(data) {
    //check event type, delegate as necessary
    let change = data.snip;
    switch(data.changetype) {
        case "Created":
            onCreated(change);
            break;
        case "Edited":
            onEdited(change);
            break;
        case "Deleted":
            onDeleted(change);
            break;
        default:
            let message = "unexpected change type: " + data.changetype;
            console.log(message);
            throw message;
    }
}

function onCreated(data) {
    snips["" + data.id] = data;
    addTab(data.id, data.title);
    setDeleteVisibility();
    if(shouldClickToNewTab) {
        $("#snip-btn-" + data.id).click();
        shouldClickToNewTab = false;
    }
}

function onEdited(data) {
    //check if active snip is getting edited, update editor if so
    if(data.id === snip.id) {
        if(data.title !== snip.title || data.content !== snip.content) {
            snip = data;
            updateEditorContents(snip.title, snip.content);
        }
    } else {
        //update in storage
        snips["" + data.id] = data;
    }

    updateTab(data.id, data.title);
}

function onDeleted(data) {
    $(".snip-tab-btn").first().click();
    delete snips["" + data.id];
    deleteTab(data.id);
    setDeleteVisibility();
}

function setDeleteVisibility() {
    if(Object.keys(snips).length > 1) {
        $("#delete-snip-btn").show();
    } else {
        $("#delete-snip-btn").hide();
    }
}

function addTab(id, title) {
    let tabHtml = `<li class="nav-item" id="snip-tab-${id}"><button type="button" class="nav-link snip-tab-btn" id="snip-btn-${id}" data-bs-toggle="tab" data-bs-target="#" onclick="loadActive(${id})"><i class="fas fa-sticky-note me-1"></i><span id="snip-name-${id}">${title}</span></button></li>`;
    $("#create-new-tab").before(tabHtml);
}

function updateTab(id, newTitle) {
    $('#snip-name-' + id).text(newTitle);
}

function deleteTab(id) {
    $("#snip-tab-" + id).remove();
}

function renameDialog() {
    $('.control-element').hide();
    $('.rename-element').show();
    $('#rename-snip-input').val(snip.title);
}

function deleteDialog() {
    if(snips.length === 1) return;
    $('.control-element').hide();
    $('.delete-element').show();
}

function initializeKeyListeners() {
    $('#rename-snip-input').on('keyup', (e) => {
        if (e.key === 'Enter' || e.keyCode === 13) {
            renameSnip();
        }
    });
}

function renameSnip() {
    snip.title = $('#rename-snip-input').val();
    editSnip();
    resetControls();
    updateTab(snip.id, snip.title);
}

function resetControls() {
    $('.control-hidden').hide();
    $('.control-element').show();
}

function createNewSnip() {
    let body = { title: "untitled", content: "edit me" };
    shouldClickToNewTab = true;
    fetch("/snips", {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
    })
    .then(_ => {
        console.log("shouldClick set true");
        shouldClickToNewTab = false;
    })
    .catch(e => {
        console.log("create failed! " + e);
        shouldClickToNewTab = false;
    });
}

function editSnip() {
    if(socketIsConnected) {
        socket.send(JSON.stringify(snip));
    } else {
    //this will need to handle title too
        console.log("setting storage: " + snip.content);
        localStorage.setItem('content', snip.content);
        changesQueued = true;
    }
}

function deleteSnip() {
    fetch("/snips/" + snip.id, {
        method: 'DELETE'
    })
    .then(_ => {
        resetControls();
    })
    .catch(e => console.log("delete failed! " + e));
}

function loadExistingSnip(data) {
    snip = data;

    //new snip, attempt to overwrite content with local cache if exists
    if(snip.createdOn === snip.lastModified) {
        if(!!localStorage.getItem('content')) { //check for not null/empty/undef/etc
            snip.content = localStorage.getItem('content');
            localStorage.removeItem('content'); //we don't want this persisting through logout now
            if(socketIsConnected) {
                socket.send(JSON.stringify(snip));
            } else {
                changesQueued = true;
            }
        }
    }
}

function loadActive(id) {
    if(id === snip.id) return; //current snip already active, no-op
    snip = snips["" + id];
    updateEditorContents(snip.title, snip.content);
    closeAction = () => connectSocket(1); //TODO is this necessary anymore? may be ok to just leave socket connected
    socket.close();
}

function updateEditorContents(title, content) {
    jar.updateCode(content);
}

function connectSocket(retryTimeout) {
    if(socketIsConnected) { console.log("connected already"); return; }
    //websocket protocol has to be secure/insecure to match http* protocol
    let wsProtocol = (location.protocol === "http:") ? "ws://" : "wss://";

    socket = new WebSocket(wsProtocol + window.location.host + "/socket/" + username);

    socket.onerror = function(event) {
        console.log("socket error:"); //TODO more
        console.dir(event);
    };

    socket.onopen = function() { //handle filling snip if exists?
        socketIsConnected = true;
        //if snip updates apply, send them? need to handle if changes on both ends lol
        if(changesQueued) {
            editSnip();
            changesQueued = false;
        }
    }

    socket.onmessage = function(event) {
        onUpdate(JSON.parse(event.data));
    };

    socket.onclose = function(event) {
        socketIsConnected = false;
        var explanation = "";
        if (event.reason && event.reason.length > 0) {
            explanation = event.reason;
        } else {
            explanation = "Reason unknown";
        }
        if(explanation === "OK") {
            closeAction();
            closeAction = () => { };
            return; //socket was closed by client code, run continuation and leave
        }

        console.log("socket closed:" + explanation);
        console.dir(event);
        ohSnap("Connection closed! " + event.code + ": " + explanation, {color: 'red'});
        ohSnap("Retrying in " + retryTimeout + " seconds", {color: 'yellow'});
        let newTimeout = Math.min(10, retryTimeout + 2); //bump up retry timeout by 2 seconds, up to max of 10
        setTimeout(connectSocket(newTimeout), retryTimeout * 1000);
    }
}

const editSnipDebounced = debounce(() => editSnip(), 500);

function fixSignOnWidget() {
    $('.okta-sign-in-header').hide(); //TODO just move to common stylesheet lmao come on
    $('.okta-form-title').hide();
}

window.onbeforeunload = () => {
    if(socketIsConnected) {
        closeAction = () => { };
        socket.close();
    }
}

$(() => { //initialize components on document.ready
    if(isLoggedIn) {
        loadExistingSnip(Object.values(snips)[0]);
        connectSocket(1);
    } else {
        let tempContent = !!localStorage.getItem('content') ? localStorage.getItem('content') : defaultContent;
        snip = { title: "untitled", content: tempContent };
        snips = {"0": snip};
    }

    loadJar();
    if(!isLoggedIn) fixSignOnWidget();
    updateEditorContents(snip.title, snip.content);
    initializeKeyListeners();
});

//attach global functions to window object so DOM elements can call them
window.loadActive = loadActive;
window.createNewSnip = createNewSnip;
window.renameDialog = renameDialog;
window.deleteDialog = deleteDialog;
window.renameSnip = renameSnip;
window.resetControls = resetControls;
window.deleteSnip = deleteSnip;