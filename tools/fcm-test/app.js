// Findear FCM 테스트 페이지. 프레임워크·빌드 없음.
// 상태는 document.body.dataset.fcmState 에 쓴다:
//   config-missing | config-invalid | unsupported | sdk-load-failed | ready
// 설정 확인(1~3)을 통과하기 전에는 Firebase SDK 를 불러오지 않고 서비스 워커도 등록하지 않는다.
// SDK 사용법은 공식 문서(https://firebase.google.com/docs/cloud-messaging/js/client, .../js/receive,
// https://firebase.google.com/docs/web/alt-setup)를 따른다.

const $ = (id) => document.getElementById(id);

const REQUIRED_FIELDS = [
  ["firebaseConfig.apiKey", (c) => c.firebaseConfig && c.firebaseConfig.apiKey],
  ["firebaseConfig.projectId", (c) => c.firebaseConfig && c.firebaseConfig.projectId],
  ["firebaseConfig.messagingSenderId", (c) => c.firebaseConfig && c.firebaseConfig.messagingSenderId],
  ["firebaseConfig.appId", (c) => c.firebaseConfig && c.firebaseConfig.appId],
  ["vapidKey", (c) => c.vapidKey],
];

const MAIN_URL_KEY = "findear-fcm-test.mainUrl";
const DEFAULT_MAIN_URL = "http://localhost:8080"; // .env의 MAIN_HOST_PORT를 바꿨으면 화면에서 그 포트로

$("origin").textContent = location.origin;

// ---------- 상태 ----------

function setState(state, message, detail) {
  document.body.dataset.fcmState = state;
  $("status").textContent = message;
  $("status-detail").textContent = detail || "";
  const ready = state === "ready";
  $("guide").hidden = ready;
  $("steps").hidden = !ready;
  $("log-section").hidden = !ready;
}

function isBlank(value) {
  return typeof value !== "string" || value.trim() === "";
}

// ---------- 로그 ----------

function log(line) {
  const div = document.createElement("div");
  div.textContent = `[${new Date().toLocaleTimeString()}] ${line}`;
  const box = $("log");
  box.appendChild(div);
  box.scrollTop = box.scrollHeight;
}

function shorten(value) {
  const s = String(value);
  return s.length > 8 ? `${s.slice(0, 8)}…(${s.length}자)` : s;
}

// 응답 JSON 안의 토큰류는 앞 몇 글자만 남긴다.
function maskSecrets(value) {
  if (Array.isArray(value)) return value.map(maskSecrets);
  if (value && typeof value === "object") {
    const out = {};
    for (const [k, v] of Object.entries(value)) {
      out[k] = /token/i.test(k) && typeof v === "string" ? shorten(v) : maskSecrets(v);
    }
    return out;
  }
  return value;
}

function summarizeBody(text) {
  try {
    return JSON.stringify(maskSecrets(JSON.parse(text)));
  } catch {
    return text.length > 300 ? `${text.slice(0, 300)}…` : text;
  }
}

// ---------- main 호출 ----------

function mainBase() {
  const raw = $("main-url").value.trim().replace(/\/+$/, "");
  const url = new URL(raw); // 잘못된 값이면 TypeError
  if (url.protocol !== "http:" && url.protocol !== "https:") throw new TypeError("http(s) 주소가 아닙니다");
  return url.origin;
}

async function callMain(method, path, { headers = {}, body } = {}) {
  let base;
  try {
    base = mainBase();
  } catch {
    log("main 주소가 올바르지 않습니다. 예: http://localhost:8080");
    return null;
  }
  const url = base + path;
  const options = { method, headers: { ...headers } };
  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }
  let response;
  try {
    response = await fetch(url, options);
  } catch (e) {
    log(`${method} ${url} 요청 실패: ${e.message}`);
    if (e instanceof TypeError) {
      log("확인: main 주소가 맞는지, main이 떠 있는지, main의 CORS_ALLOWED_ORIGINS에 이 페이지 origin(" + location.origin + ")이 있는지.");
    }
    return null;
  }
  const text = await response.text();
  log(`${method} ${url} → HTTP ${response.status} ${summarizeBody(text)}`);
  let json = null;
  try {
    json = JSON.parse(text);
  } catch {
    // JSON 이 아니면 json 은 null
  }
  return { status: response.status, ok: response.ok, json };
}

// ---------- 진입 판정 ----------

async function init() {
  // ① 설정 파일 없음
  const config = self.FINDEAR_FCM_CONFIG;
  if (!config) {
    setState("config-missing", "설정 파일(firebase-config.js)이 없습니다.",
      "firebase-config.example.js를 firebase-config.js로 복사해 값을 채운 뒤 새로고침하세요. Firebase는 초기화하지 않았습니다.");
    return;
  }

  // ② 필수 값 비어 있음
  const empty = REQUIRED_FIELDS.filter(([, get]) => {
    try {
      return isBlank(get(config));
    } catch {
      return true;
    }
  }).map(([name]) => name);
  if (empty.length > 0) {
    setState("config-invalid", "firebase-config.js에 비어 있는 값이 있습니다.",
      `채워야 할 항목: ${empty.join(", ")}`);
    return;
  }

  // ③ 보안 컨텍스트·브라우저 지원
  const missingApis = [];
  if (!window.isSecureContext) missingApis.push("보안 컨텍스트(https 또는 localhost)");
  if (!("serviceWorker" in navigator)) missingApis.push("serviceWorker");
  if (!("Notification" in window)) missingApis.push("Notification");
  if (!("PushManager" in window)) missingApis.push("PushManager");
  if (missingApis.length > 0) {
    setState("unsupported", "이 브라우저·접속 방식에서는 웹푸시를 쓸 수 없습니다.",
      `없는 항목: ${missingApis.join(", ")}. http://localhost:5500으로 최신 Chrome/Edge/Firefox에서 여세요.`);
    return;
  }

  // ④ 여기서부터 Firebase SDK 를 불러온다
  const version = self.FINDEAR_FIREBASE_SDK_VERSION;
  let appModule;
  let messagingModule;
  try {
    if (isBlank(version)) throw new Error("sdk-version.js가 없거나 버전이 비어 있습니다");
    const base = `https://www.gstatic.com/firebasejs/${version}`;
    appModule = await import(`${base}/firebase-app.js`);
    messagingModule = await import(`${base}/firebase-messaging.js`);
  } catch (e) {
    setState("sdk-load-failed", "Firebase SDK를 불러오지 못했습니다.",
      `${e && e.message ? e.message : e} (네트워크 연결과 SDK 버전 ${version}을 확인하세요)`);
    return;
  }

  let messaging;
  try {
    if (!(await messagingModule.isSupported())) {
      setState("unsupported", "Firebase 메시징이 이 브라우저를 지원하지 않는다고 응답했습니다.",
        "isSupported()가 false입니다. 다른 브라우저나 http://localhost:5500 접속을 확인하세요.");
      return;
    }
    const app = appModule.initializeApp(config.firebaseConfig);
    messaging = messagingModule.getMessaging(app);
  } catch (e) {
    setState("unsupported", "Firebase 초기화에 실패했습니다.", e && e.message ? e.message : String(e));
    return;
  }

  setState("ready", "준비됨 — 아래 단계를 순서대로 진행하세요.");
  startSteps(config, messaging, messagingModule);
}

// ---------- 단계 UI ----------

function startSteps(config, messaging, messagingModule) {
  const session = { accessToken: null, memberId: null, permitted: false, fcmToken: null, saved: false };

  try {
    $("main-url").value = localStorage.getItem(MAIN_URL_KEY) || DEFAULT_MAIN_URL;
  } catch {
    $("main-url").value = DEFAULT_MAIN_URL;
  }

  function refresh() {
    $("btn-login").disabled = false;
    $("btn-permission").disabled = !session.accessToken;
    $("btn-token").disabled = !session.permitted;
    $("btn-copy").disabled = !session.fcmToken;
    $("btn-save").disabled = !session.fcmToken;
    $("btn-send").disabled = !session.saved;
  }

  // 로그인을 다시 하면 이후 단계를 처음부터
  function resetAfterLogin() {
    session.permitted = Notification.permission === "granted";
    session.fcmToken = null;
    session.saved = false;
    $("token-result").hidden = true;
    $("token-result").textContent = "";
  }

  $("btn-login").addEventListener("click", async () => {
    try {
      localStorage.setItem(MAIN_URL_KEY, $("main-url").value.trim());
    } catch {
      // 저장 못 해도 진행
    }
    const res = await callMain("POST", "/members/login", { body: { phoneNumber: $("phone").value.trim() } });
    if (!res) return;
    const result = res.json && res.json.result;
    const accessToken = result && result.accessToken;
    const memberId = result && result.member && result.member.memberId;
    if (!res.ok || !accessToken || memberId == null) {
      $("login-result").textContent = `로그인 실패 (HTTP ${res.status}). main이 local 프로필인지, 시드 회원 전화번호인지 확인하세요.`;
      session.accessToken = null;
      session.memberId = null;
      resetAfterLogin();
      refresh();
      return;
    }
    // JWT 는 메모리에만 둔다 (저장소에 저장하지 않음)
    session.accessToken = accessToken;
    session.memberId = memberId;
    resetAfterLogin();
    $("login-result").textContent = `로그인됨: memberId=${memberId}, accessToken=${shorten(accessToken)}`;
    refresh();
  });

  $("btn-permission").addEventListener("click", async () => {
    const permission = await Notification.requestPermission();
    session.permitted = permission === "granted";
    $("permission-result").textContent = session.permitted
      ? "알림 권한: 허용됨"
      : `알림 권한: ${permission}. 브라우저 주소창의 사이트 설정에서 알림을 허용한 뒤 다시 시도하세요.`;
    log(`Notification.requestPermission() → ${permission}`);
    refresh();
  });

  $("btn-token").addEventListener("click", async () => {
    try {
      const serviceWorkerRegistration = await navigator.serviceWorker.register("./firebase-messaging-sw.js");
      const token = await messagingModule.getToken(messaging, {
        vapidKey: config.vapidKey,
        serviceWorkerRegistration,
      });
      if (!token) {
        log("getToken()이 토큰을 주지 않았습니다. 알림 권한을 확인하세요.");
        return;
      }
      session.fcmToken = token;
      session.saved = false;
      $("token-result").textContent = token;
      $("token-result").hidden = false;
      log(`getToken() 성공: ${shorten(token)}`);
    } catch (e) {
      log(`getToken() 실패: ${e && e.message ? e.message : e}`);
    }
    refresh();
  });

  $("btn-copy").addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText(session.fcmToken);
      log("토큰을 클립보드에 복사했습니다.");
    } catch (e) {
      log(`복사 실패: ${e && e.message ? e.message : e} (토큰 칸을 직접 선택해 복사하세요)`);
    }
  });

  $("btn-save").addEventListener("click", async () => {
    const res = await callMain("POST", "/notification/new", {
      headers: { "access-token": session.accessToken },
      body: { token: session.fcmToken },
    });
    session.saved = !!(res && res.ok);
    if (res && !res.ok) log("토큰 저장 실패. JWT가 만료됐다면 1단계부터 다시 하세요.");
    refresh();
  });

  $("btn-send").addEventListener("click", async () => {
    const res = await callMain("POST", `/alarm/send-fcm/${encodeURIComponent(session.memberId)}`, {
      headers: { "access-token": session.accessToken },
      body: {
        title: $("send-title").value,
        message: $("send-message").value,
        type: $("send-type").value,
      },
    });
    if (res && res.ok) {
      log("요청은 성공했습니다. 서버가 FCM_ENABLED=false이면 발송을 건너뛰므로 main 로그도 확인하세요.");
    }
  });

  // 페이지가 열려 있을 때(포그라운드) 받은 메시지는 알림 대신 여기로 온다.
  messagingModule.onMessage(messaging, (payload) => {
    const n = payload.notification || {};
    const body = n.body || "";
    const i = body.lastIndexOf(":");
    const message = i >= 0 ? body.slice(0, i) : body;
    const type = i >= 0 ? body.slice(i + 1) : "";
    log(`[포그라운드 수신] 제목=${n.title || ""} 메시지=${message} 타입=${type}`);
  });

  refresh();
}

init().catch((e) => {
  setState("sdk-load-failed", "예상하지 못한 오류로 시작하지 못했습니다.", e && e.message ? e.message : String(e));
});
