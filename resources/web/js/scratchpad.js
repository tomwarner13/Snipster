let snips = [];
let socket = null;
let snip = {};
let flask = {};

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

fetch("/snips")
    .then(response => {
        console.log("response:");
        console.dir(response);
        return response.json();
    })
    .then(data => {
        console.dir(data);
        snips = data;
        loadExistingSnip(snips[0]);
        connectSocket(1);
        loadFlask();
        updateEditorContents(snip.title, snip.content);
    });

console.log("snips: " + snips);

function editSnip() {
    console.log("sending:");
    console.dir(snip);
    socket.send(JSON.stringify(snip));
}

const editSnipDebounced = debounce(() => editSnip(), 500);

function loadExistingSnip(data) {
    console.log("load existing:");
    console.dir(data);
    snip = data;
}

function updateEditorContents(title, content) {
    flask.updateCode(content);
}

function connectSocket(retryTimeout) {
    socket = new WebSocket("ws://" + window.location.host + "/socket/" + snip.id);

    socket.onerror = function(event) {
        console.log("socket error:"); //TODO more
        console.dir(event);
    };

    socket.onopen = function() { //handle filling snip if exists?
        console.log("socket connected");
    }

    socket.onmessage = function(event) {
        console.log("message received:");
        console.dir(event.data);
        onUpdate(JSON.parse(event.data));
    };

    socket.onclose = function(event) {
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