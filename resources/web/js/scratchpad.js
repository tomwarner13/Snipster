let socket = null;
let snip = {};
let flask = {};
let socketIsConnected = false;
let changesQueued = false;
let closeAction = () => { };

const debounce = (callback, wait) => {
  let timeoutId = null;
  return (...args) => {
    window.clearTimeout(timeoutId);
    timeoutId = window.setTimeout(() => {
      callback.apply(null, args);
    }, wait);
  };
}

function loadFlask() {
    flask = new CodeFlask('.codeflask', { language: 'markup', lineNumbers: true });
    flask.addLanguage('markup', Prism.languages['markup']); //start plaintext? syntax highlighting later?
    flask.onUpdate((data) => {
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
    switch(data.changeType) {
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
            let message = "unexpected change type: " + data.changeType;
            console.log(message);
            throw message;
    }
}

function onCreated(data) {
    //add new snip to storage

    //create new tab with new title
}

function onEdited(data) {
    //check if active snip is getting edited, update flask if so
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
    //remove tab, remove snip from storage

}

function createNewSnip() {
    //TODO this
}

function updateTab(id, newTitle) {

}

function editSnip() {
    if(socketIsConnected) {
        socket.send(JSON.stringify(snip));
    } else {
        console.log("setting storage: " + snip.content);
        localStorage.setItem('content', snip.content);
        changesQueued = true;
    }
}

function deleteSnip(id) {
    //TODO this
}

function loadExistingSnip(data) {
    snip = data;

    //new snip, attempt to overwrite content with local cache if exists
    if(snip.createdDate === snip.modifiedDate) {
        if(!!localStorage.getItem('content')) { //check for not null/empty/undef/etc
            snip.content = localStorage.getItem('content');
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
    snip = snips.find(s => s.id === id);
    updateEditorContents(snip.title, snip.content);
    closeAction = () => connectSocket(1);
    socket.close();
}

function updateEditorContents(title, content) {
    flask.updateCode(content);
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
        //incoming frames are now of type ClientMessage, handle as such
        onUpdate(JSON.parse(event.data));
    };

    socket.onclose = function(event) {
        console.log("onclose called");
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

if(isLoggedIn) {
    loadExistingSnip(Object.keys(snips)[0]); //
    connectSocket(1);
} else {
    snip = { title: "untitled", content: "welcome! edit me" };
    snips = {"0": snip};
}

$(() => { //load flask on document.ready
    loadFlask();
    updateEditorContents(snip.title, snip.content);
});