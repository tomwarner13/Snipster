let snips = [];
let socket = null;
let snip = {};
let flask = {};
let socketIsConnected = false;
let changesQueued = false;

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
    if(data.title !== snip.title || data.content !== snip.content) {
        snip = data;
        updateEditorContents(snip.title, snip.content);
    }
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

function updateEditorContents(title, content) {
    flask.updateCode(content);
}

function connectSocket(retryTimeout) {
    //websocket protocol has to be secure/insecure to match http* protocol
    let wsProtocol = "wss://";
    if(location.protocol === "http:") wsProtocol = "ws://";

    socket = new WebSocket(wsProtocol + window.location.host + "/socket/" + snip.id);

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
    fetch("/snips")
        .then(response => {
            return response.json();
        })
        .then(data => {
            snips = data;
            loadExistingSnip(snips[0]);
            connectSocket(1);
            $(() => {
                loadFlask();
                updateEditorContents(snip.title, snip.content);
            });
        });
} else {
    snip = { title: "untitled", content: "welcome! edit me" };
    snips = [snip];
    $(() => { //load flask on document.ready
        loadFlask();
        updateEditorContents(snip.title, snip.content);
    });
}