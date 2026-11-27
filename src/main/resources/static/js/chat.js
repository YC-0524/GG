// Chat page (US 48): keeps the newest message in view, shows local times,
// counts characters, and checks for new messages every few seconds
const POLL_MS = 4000;

const chat = document.getElementById("chat");
const list = document.getElementById("chat-messages");
const empty = document.getElementById("chat-empty");
const form = document.getElementById("chat-form");
const input = document.getElementById("chat-input");
const count = document.getElementById("chat-count");
const closedNotice = document.getElementById("chat-closed");
const matchId = chat.dataset.matchId;
const maxLength = Number(input.getAttribute("maxlength"));
// English like the rest of the site, in the viewer's own time zone
const timeFormat = new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" });

function showLocalTime(time) {
    const date = new Date(time.getAttribute("datetime"));
    if (!Number.isNaN(date.getTime())) {
        time.textContent = timeFormat.format(date);
    }
}

function scrollToNewest() {
    list.scrollTop = list.scrollHeight;
}

function lastMessageId() {
    const last = list.querySelector("li:last-child");
    return last ? Number(last.dataset.messageId) : 0;
}

function addMessage(message) {
    const item = document.createElement("li");
    item.className = `chat-message ${message.mine ? "mine" : "theirs"}`;
    item.dataset.messageId = message.id;

    const who = document.createElement("span");
    who.className = "visually-hidden";
    who.textContent = message.mine ? "You:" : `${document.querySelector(".chat-header h1").textContent.trim()}:`;

    const bubble = document.createElement("p");
    bubble.className = "chat-bubble";
    bubble.textContent = message.body;

    const time = document.createElement("time");
    time.setAttribute("datetime", message.sentAtIso);
    time.textContent = message.sentAt;
    showLocalTime(time);

    item.append(who, bubble, time);
    list.append(item);
    empty.hidden = true;
}

function closeChat() {
    closedNotice.hidden = false;
    form.querySelectorAll("textarea, button").forEach((element) => { element.disabled = true; });
    clearInterval(poller);
}

async function checkForMessages() {
    try {
        const response = await fetch(`/matches/${matchId}/messages?after=${lastMessageId()}`, {
            credentials: "same-origin",
            headers: { Accept: "application/json" }
        });
        if (response.status === 410 || response.status === 404) {
            closeChat();
            return;
        }
        if (!response.ok) return;
        const messages = await response.json();
        const nearBottom = list.scrollHeight - list.scrollTop - list.clientHeight < 80;
        messages.forEach(addMessage);
        if (messages.length > 0 && nearBottom) scrollToNewest();
    } catch (error) {
        // offline for a moment; try again on the next check
    }
}

function updateCount() {
    count.textContent = `${input.value.length} / ${maxLength}`;
}

list.querySelectorAll("time").forEach(showLocalTime);
scrollToNewest();
updateCount();
input.addEventListener("input", updateCount);

// Enter sends, Shift+Enter adds a new line
input.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
        event.preventDefault();
        if (input.value.trim()) form.requestSubmit();
    }
});

const poller = setInterval(checkForMessages, POLL_MS);
