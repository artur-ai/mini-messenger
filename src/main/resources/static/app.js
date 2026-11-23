"use strict";

const MAX_LENGTH = 512;
const POLL_MS = 2000;
const LIST_MS = 5000;
const NICK_COLORS = ["#33ff66", "#00e5ff", "#ffb000", "#ff6ec7", "#b388ff", "#c6ff00", "#ff8a65", "#4dd0e1"];

const state = {
    me: null,
    conversations: [],
    channels: [],
    current: null,
    lastSeq: 0,
    lastDay: null,
    polling: false,
    lag: false,
    pollTimer: null,
    listTimer: null,
    clockTimer: null,
};

const $ = (selector) => document.querySelector(selector);

class ApiError extends Error {
    constructor(message, status) {
        super(message);
        this.status = status;
    }
}

async function api(method, path, body) {
    const options = { method, credentials: "same-origin", headers: {} };
    if (body !== undefined) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }
    const response = await fetch("/api" + path, options);
    const raw = await response.text();
    let data = null;
    if (raw) {
        try {
            data = JSON.parse(raw);
        } catch {
            data = null;
        }
    }
    if (!response.ok) {
        if (response.status === 401 && state.me) {
            loggedOut("session expired, please log in again");
        }
        throw new ApiError(data?.message ?? `HTTP ${response.status}`, response.status);
    }
    return data;
}

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

function nickColor(nick) {
    let hash = 0;
    for (const ch of nick) hash = (hash * 31 + ch.codePointAt(0)) | 0;
    return NICK_COLORS[Math.abs(hash) % NICK_COLORS.length];
}

function pad(n) {
    return String(n).padStart(2, "0");
}

function timeOf(date) {
    return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function dayOf(date) {
    return date.toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "short", year: "numeric" });
}

function titleOf(conversation) {
    if (!conversation) return "";
    return conversation.type === "DIRECT" ? "@" + conversation.title : conversation.title;
}

function codePoints(text) {
    return [...text.trim()].length;
}

function showLogin(message) {
    $("#chat").hidden = true;
    $("#login").hidden = false;
    $("#auth-error").textContent = message ? "-!- " + message : "";
    $("#username").focus();
}

function loggedOut(message) {
    state.me = null;
    state.current = null;
    clearInterval(state.pollTimer);
    clearInterval(state.listTimer);
    clearInterval(state.clockTimer);
    showLogin(message);
}

async function boot() {
    try {
        state.me = await api("GET", "/auth/me");
        await enterChat();
    } catch {
        showLogin();
    }
}

async function enterChat() {
    $("#login").hidden = true;
    $("#chat").hidden = false;
    $("#nick").textContent = state.me.username;
    await refreshLists();
    if (state.conversations.length === 0) {
        await api("POST", "/channels/general/join");
        await refreshLists();
    }
    clearLog();
    system(`welcome to dorm3 relay chat, ${state.me.username}. type /help for commands`);
    if (state.conversations.length > 0) {
        await openConversation(state.conversations[0].id);
    }
    clearInterval(state.listTimer);
    state.listTimer = setInterval(() => refreshLists().catch(() => {}), LIST_MS);
    clearInterval(state.clockTimer);
    state.clockTimer = setInterval(renderStatus, 15000);
    $("#input").focus();
}

async function refreshLists() {
    const [conversations, channels] = await Promise.all([
        api("GET", "/conversations"),
        api("GET", "/channels"),
    ]);
    state.conversations = conversations;
    state.channels = channels;
    if (state.current) {
        state.current = conversations.find((c) => c.id === state.current.id) ?? state.current;
    }
    renderSidebar();
    renderMembers();
    renderHeader();
    renderStatus();
}

function renderSidebar() {
    const sidebar = $("#sidebar");
    sidebar.replaceChildren();
    const joined = state.conversations.filter((c) => c.type === "CHANNEL");
    const direct = state.conversations.filter((c) => c.type === "DIRECT");
    const available = state.channels.filter((c) => !c.joined);

    addSection(sidebar, "CHANNELS", joined, (c) => openConversation(c.id));
    addSection(sidebar, "PRIVATE", direct, (c) => openConversation(c.id));
    addSection(sidebar, "JOIN", available, (c) => joinChannel(c.title), "available");
}

function addSection(parent, title, items, onClick, extraClass) {
    if (items.length === 0) return;
    parent.append(el("div", "section-title", title));
    for (const conversation of items) {
        const button = el("button", "item" + (extraClass ? " " + extraClass : ""), titleOf(conversation));
        button.type = "button";
        if (state.current?.id === conversation.id) button.classList.add("active");
        button.addEventListener("click", () => {
            $("#sidebar").classList.remove("open");
            onClick(conversation);
        });
        parent.append(button);
    }
}

function renderMembers() {
    const panel = $("#members");
    panel.replaceChildren();
    if (!state.current) return;
    panel.append(el("div", "section-title", `USERS (${state.current.members.length})`));
    for (const nick of state.current.members) {
        const own = nick === state.me.username;
        const row = el(own ? "div" : "button", own ? "member" : "member item", nick);
        row.style.color = nickColor(nick);
        if (!own) {
            row.type = "button";
            row.title = `private message to ${nick}`;
            row.addEventListener("click", () => COMMANDS.msg.run([nick], nick).catch((e) => error(e.message)));
        }
        panel.append(row);
    }
}

function renderHeader() {
    const c = state.current;
    $("#topic").textContent = c
        ? `${titleOf(c)} | ${c.members.length} users | KAI dorm No.3`
        : "dorm3 relay chat";
}

function renderStatus() {
    const bar = $("#status");
    bar.replaceChildren();
    const parts = [`[${timeOf(new Date())}]`, `[${state.me?.username ?? "-"}]`];
    if (state.current) parts.push(`[${titleOf(state.current)}]`);
    bar.append(document.createTextNode(parts.join(" ")));
    if (state.lag) bar.append(el("span", "lag", " [lag]"));
}

function clearLog() {
    $("#log").replaceChildren();
    state.lastDay = null;
}

function nearBottom() {
    const log = $("#log");
    return log.scrollHeight - log.scrollTop - log.clientHeight < 60;
}

function appendLine(line) {
    const log = $("#log");
    const stick = nearBottom();
    log.append(line);
    if (stick) log.scrollTop = log.scrollHeight;
}

function system(text, kind = "system") {
    const line = el("div", `line ${kind}`);
    line.append(el("span", "time", `[${timeOf(new Date())}] `), el("span", "", "-!- "), el("span", "text", text));
    appendLine(line);
}

function error(text) {
    system("error: " + text, "system error");
}

function messageLine(message, prefix) {
    const date = new Date(message.createdAt);
    const own = message.sender === state.me.username;
    const mentioned = !own && new RegExp(`(^|\\W)@?${state.me.username}(\\W|$)`, "i").test(message.text);
    const line = el("div", "line" + (own ? " own" : "") + (mentioned ? " mention" : ""));
    line.title = date.toLocaleString();
    const nick = el("span", "nick", `<${message.sender}>`);
    nick.style.color = nickColor(message.sender);
    line.append(el("span", "time", `[${prefix ?? timeOf(date)}] `), nick, document.createTextNode(" "), el("span", "text", message.text));
    return line;
}

function appendMessage(message) {
    if (message.seq <= state.lastSeq) return;
    state.lastSeq = message.seq;
    const day = dayOf(new Date(message.createdAt));
    if (state.lastDay && state.lastDay !== day) {
        system(`day changed to ${day}`);
    }
    state.lastDay = day;
    appendLine(messageLine(message));
}

async function openConversation(id) {
    const conversation = state.conversations.find((c) => c.id === id);
    if (!conversation) return;
    state.current = conversation;
    state.lastSeq = 0;
    clearLog();
    renderSidebar();
    renderMembers();
    renderHeader();
    renderStatus();
    system(`now talking in ${titleOf(conversation)}`);
    try {
        const messages = await api("GET", `/conversations/${id}/messages?limit=200`);
        if (state.current?.id !== id) return;
        if (messages.length === 0) system("no messages yet. say hi!");
        messages.forEach(appendMessage);
        $("#log").scrollTop = $("#log").scrollHeight;
    } catch (e) {
        error(e.message);
    }
    startPolling();
    $("#input").focus();
}

function startPolling() {
    clearInterval(state.pollTimer);
    state.pollTimer = setInterval(poll, POLL_MS);
}

async function poll() {
    const current = state.current;
    if (!current || state.polling) return;
    state.polling = true;
    try {
        const messages = await api("GET", `/conversations/${current.id}/messages?after=${state.lastSeq}`);
        if (state.current?.id === current.id) messages.forEach(appendMessage);
        setLag(false);
    } catch (e) {
        if (e.status !== 401) setLag(true);
    } finally {
        state.polling = false;
    }
}

function setLag(lag) {
    if (state.lag !== lag) {
        state.lag = lag;
        renderStatus();
    }
}

async function joinChannel(name) {
    const channel = await api("POST", `/channels/${encodeURIComponent(name.replace(/^#/, ""))}/join`);
    await refreshLists();
    await openConversation(channel.id);
}

async function sendMessage(text) {
    if (!state.current) {
        error("you are not in a conversation. try /join #general");
        return;
    }
    const message = await api("POST", `/conversations/${state.current.id}/messages`, { text });
    appendMessage(message);
    $("#log").scrollTop = $("#log").scrollHeight;
}

const COMMANDS = {
    help: {
        usage: "/help",
        about: "show this list",
        run: () => {
            system("commands:");
            for (const command of Object.values(COMMANDS)) {
                system(`  ${command.usage.padEnd(30)} ${command.about}`);
            }
            system("  enter = send, shift+enter = new line");
        },
    },
    join: {
        usage: "/join #channel",
        about: "join an existing channel",
        run: async (args) => {
            if (!args[0]) throw new Error("usage: /join #channel");
            await joinChannel(args[0]);
        },
    },
    create: {
        usage: "/create #channel [nick ...]",
        about: "create a channel and invite people",
        run: async (args) => {
            if (!args[0]) throw new Error("usage: /create #channel nick1 nick2");
            const channel = await api("POST", "/channels", { name: args[0], members: args.slice(1) });
            await refreshLists();
            await openConversation(channel.id);
        },
    },
    msg: {
        usage: "/msg nick [text]",
        about: "private conversation with someone",
        run: async (args, rest) => {
            if (!args[0]) throw new Error("usage: /msg nick text");
            const direct = await api("POST", "/direct", { username: args[0] });
            await refreshLists();
            await openConversation(direct.id);
            const text = rest.slice(args[0].length).trim();
            if (text) await sendMessage(text);
        },
    },
    search: {
        usage: "/search text",
        about: "search your messages",
        run: async (args, rest) => {
            const query = rest.trim();
            const results = await api("GET", `/messages/search?q=${encodeURIComponent(query)}`);
            system(`search "${query}": ${results.length} result(s)`);
            for (const message of results) {
                const conversation = state.conversations.find((c) => c.id === message.conversationId);
                const date = new Date(message.createdAt);
                const line = messageLine(message, `${pad(date.getDate())}.${pad(date.getMonth() + 1)} ${timeOf(date)} ${titleOf(conversation)}`);
                appendLine(line);
            }
        },
    },
    users: {
        usage: "/users",
        about: "list everyone registered",
        run: async () => {
            const users = await api("GET", "/users");
            system(`${users.length} users: ${users.map((u) => u.username).join(", ")}`);
        },
    },
    clear: {
        usage: "/clear",
        about: "clear the screen",
        run: () => clearLog(),
    },
    logout: {
        usage: "/logout",
        about: "disconnect",
        run: async () => {
            await api("POST", "/auth/logout");
            loggedOut("disconnected");
        },
    },
};

async function handleInput(raw) {
    if (raw.startsWith("/") && !raw.startsWith("//")) {
        const rest = raw.slice(1).replace(/^\S+/, "");
        const name = raw.slice(1).split(/\s+/)[0].toLowerCase();
        const args = rest.trim().split(/\s+/).filter(Boolean);
        const command = COMMANDS[name];
        if (!command) {
            error(`unknown command /${name}. try /help`);
            return;
        }
        await command.run(args, rest.trim());
        return;
    }
    await sendMessage(raw.startsWith("//") ? raw.slice(1) : raw);
}

function updateCounter() {
    const input = $("#input");
    const count = codePoints(input.value);
    const counter = $("#counter");
    counter.textContent = `${count}/${MAX_LENGTH}`;
    counter.classList.toggle("over", count > MAX_LENGTH);
    input.style.height = "auto";
    input.style.height = input.scrollHeight + "px";
}

$("#auth-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const mode = event.submitter?.value === "register" ? "register" : "login";
    $("#auth-error").textContent = "";
    try {
        state.me = await api("POST", `/auth/${mode}`, {
            username: $("#username").value,
            password: $("#password").value,
        });
        $("#password").value = "";
        await enterChat();
    } catch (e) {
        $("#auth-error").textContent = "-!- " + e.message;
    }
});

$("#input").addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
        event.preventDefault();
        $("#composer").requestSubmit();
    }
});

$("#input").addEventListener("input", updateCounter);

$("#composer").addEventListener("submit", async (event) => {
    event.preventDefault();
    const input = $("#input");
    const raw = input.value;
    input.value = "";
    updateCounter();
    try {
        await handleInput(raw);
    } catch (e) {
        error(e.message);
        if (!raw.startsWith("/")) {
            input.value = raw;
            updateCounter();
        }
    }
});

$("#menu").addEventListener("click", () => $("#sidebar").classList.toggle("open"));

boot();
