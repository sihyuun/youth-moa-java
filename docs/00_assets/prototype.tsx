
<!DOCTYPE html>
<html data-src-ver="26c5adaa" lang="ko" data-om-id="26c5adaa:0">
<head data-om-id="26c5adaa:1">
<style data-omelette-injected>html,body{background:transparent}</style><script data-omelette-injected>(()=>{var j=["https://claude.ai","https://preview.claude.ai"];var x=true;var S=30000;var f=3000;document.addEventListener("DOMContentLoaded",()=>{const H=window.Babel;const G="om-src-id";if(H&&H.registerPlugin&&!(H.availablePlugins&&H.availablePlugins[G])){H.registerPlugin(G,({types:Y})=>({visitor:{JSXOpeningElement(U,Q){const J=U.node;if(J.name&&J.name.type==="JSXMemberExpression")return;for(let V=0;V<J.attributes.length;V++){const L=J.attributes[V];if(L.type==="JSXAttribute"&&L.name&&L.name.name==="data-om-id")return}const W=Q.file&&Q.file.opts&&Q.file.opts.filename||"?";const Z=J.loc&&J.loc.start;const z=`jsx:${W}:${J.start??0}:${Z?Z.line:0}:${Z?Z.column+1:0}`;J.attributes.push(Y.jsxAttribute(Y.jsxIdentifier("data-om-id"),Y.stringLiteral(z)))}}}))}const k="om-text-extract";let A=false;if(x&&H&&H.registerPlugin&&!(H.availablePlugins&&H.availablePlugins[k])){const Y=window.React;if(Y&&Y.useSyncExternalStore&&Y.createElement){A=true;const U={};const Q=new Set;let J=0;window.__omTextO=U;window.__omTextSet=(z)=>{for(const V in z){const L=z[V];if(L==null)delete U[V];else U[V]=L}J++;Q.forEach((V)=>V())};const W=(z)=>{Q.add(z);return()=>Q.delete(z)};const Z=()=>J;window.__OmT=function z(V){Y.useSyncExternalStore(W,Z,Z);return Y.createElement("span",{"data-om-text":V.i,className:"__om-t"},V.i in U?U[V.i]:V.children)};H.registerPlugin(k,({types:z})=>{const V={style:1,script:1,noscript:1,textarea:1,title:1,option:1,text:1,tspan:1,textPath:1};const L={placeholder:1,alt:1,title:1,label:1,"aria-label":1,"aria-description":1,"aria-placeholder":1};const _=($,C)=>z.logicalExpression("??",z.memberExpression(z.logicalExpression("||",z.memberExpression(z.identifier("window"),z.identifier("__omTextO")),z.objectExpression([])),z.stringLiteral($),true),C);const E=($)=>$.file&&$.file.opts&&$.file.opts.filename||"?";const B=($)=>$.file&&$.file.code||"";const b=($,C)=>{const X=(F)=>F&&F.start!=null&&F.end!=null&&(F.type==="StringLiteral"||F.type==="TemplateLiteral"&&F.expressions.length===0)?[F.start,F.end]:null;const N=X(C);if(N)return N;if(C&&C.type==="Identifier"){const F=$.scope.getBinding(C.name);if(F&&F.constant&&F.path.node.type==="VariableDeclarator"){return X(F.path.node.init)}}return null};const M=($,C,X)=>{const N=$.node;let F=false;const w=(K,q)=>{F=true;return z.jsxElement(z.jsxOpeningElement(z.jsxIdentifier("__OmT"),[z.jsxAttribute(z.jsxIdentifier("i"),z.stringLiteral(K))],false),z.jsxClosingElement(z.jsxIdentifier("__OmT")),[q],false)};const D=N.children.map((K)=>{if(K.type==="JSXText"){if(K.start==null||K.end==null)return K;const q=X.slice(K.start,K.end);const R=q.length-q.trimStart().length;const m=q.length-q.trimEnd().length;if(R+m>=q.length)return K;return w(`txt:${C}:${K.start+R}:${K.end-m}`,K)}if(K.type==="JSXExpressionContainer"){const q=b($,K.expression);if(q)return w(`str:${C}:${q[0]}:${q[1]}`,K)}return K});if(F)N.children=D};return{visitor:{JSXOpeningElement($,C){const X=$.node;const N=E(C);const F={};for(let w=0;w<X.attributes.length;w++){const D=X.attributes[w];if(D.type!=="JSXAttribute"||!D.name)continue;const K=D.name.name;if(!L[K])continue;if(!D.value||D.value.type!=="StringLiteral")continue;if(D.value.start==null||D.value.end==null)continue;const q=`txt:${N}:${D.value.start}:${D.value.end}`;F[K]=q;D.value=z.jsxExpressionContainer(_(q,z.stringLiteral(D.value.value)))}if(Object.keys(F).length){X.attributes.push(z.jsxAttribute(z.jsxIdentifier("data-om-text-attrs"),z.stringLiteral(JSON.stringify(F))))}},JSXElement($,C){const X=$.node.openingElement;const N=X&&X.name&&X.name.name;if(N==="__OmT")return;if(typeof N==="string"&&V[N])return;M($,E(C),B(C))},JSXFragment($,C){M($,E(C),B(C))}}}})}}const u=A?["transform-react-jsx-source",G,k]:["transform-react-jsx-source",G];document.querySelectorAll('script[type="text/babel"],script[type="text/jsx"]').forEach((Y,U)=>{const Q=Y.getAttribute("data-plugins")||"";const J=u.filter((W)=>Q.indexOf(W)<0);if(J.length){Y.setAttribute("data-plugins",Q?`${Q},${J.join(",")}`:J.join(","))}if(!Y.hasAttribute("data-filename")){Y.setAttribute("data-filename",Y.getAttribute("src")||`inline-${U}`)}})});var y=window.parent;if(y&&y!==window){const H=(Q,J)=>{try{y.postMessage({__omelette_log:true,type:Q,data:J},"*")}catch{}};["log","warn","error"].forEach((Q)=>{const J=console[Q];console[Q]=(...W)=>{const Z=W.map((z)=>{try{return typeof z==="object"?JSON.stringify(z):String(z)}catch{try{return String(z)}catch{return"[unstringifiable]"}}}).join(" ");H(Q,Z);J.apply(console,W)}});window.addEventListener("error",(Q)=>{const J=Q.target;if(J&&J!==window&&J.tagName){const W=J.src||J.href||"";H("resource_error",`${J.tagName} failed to load: ${W}`)}else{H("error",`${Q.message||"Unknown error"} at ${Q.filename||"?"}:${Q.lineno||0}:${Q.colno||0}`)}},true);window.addEventListener("unhandledrejection",(Q)=>{const J=Q.reason;const W=J instanceof Error?`${J.message} ${J.stack}`:String(J);H("error",`Unhandled rejection: ${W}`)});const G=()=>{try{y.postMessage({type:"omelette:height",height:document.documentElement.scrollHeight},"*")}catch{}};if(document.readyState==="complete")G();else window.addEventListener("load",G);if(window.ResizeObserver)new ResizeObserver(G).observe(document.documentElement);const k={};let A=0;window.claude={complete(Q){return new Promise((J,W)=>{if(!j.length){W(new Error("claude.complete: no parent origin"));return}const Z=`c${++A}`;const z=()=>setTimeout(()=>{delete k[Z];W(new Error(`claude.complete: no data for ${S/1000}s`))},S);const V=z();k[Z]={resolve:J,reject:W,text:"",t:V,arm:z};try{for(const L of j){y.postMessage({__om_api:true,id:Z,body:Q},L)}}catch(L){clearTimeout(V);delete k[Z];W(L)}})}};const u={};let Y=0;window.omelette={writeFile:(Q,J)=>new Promise((W,Z)=>{if(!j.length){W();return}const z=`f${++Y}`;const V=setTimeout(()=>{delete u[z];W()},f);u[z]={resolve:W,reject:Z,t:V};try{for(const L of j){y.postMessage({__om_file:true,id:z,op:"write",path:Q,content:J},L)}}catch{clearTimeout(V);delete u[z];W()}})};const U=(Q)=>{try{return JSON.stringify(Q)}catch{return JSON.stringify(String(Q))}};window.addEventListener("message",(Q)=>{if(j.indexOf(Q.origin)<0)return;if(Q.source!==y)return;const J=Q.data;if(!J)return;if(J.type==="omelette-preview-refresh"){if(typeof J.token!=="string"||!/^[0-9a-f.-]+(\.(rc|pr-[0-9]+))?(\.[0-9]+\.[0-9a-f]{16})?$/.test(J.token))return;const z=Math.max(1,Math.floor((J.expiresAt||0)-Date.now()/1000));document.cookie="__Host-omelette-preview="+J.token+"; Path=/; Secure; SameSite=None; Partitioned; Max-Age="+z;return}if(J.__om_file_r){const z=u[J.id];if(!z)return;clearTimeout(z.t);delete u[J.id];if(J.ok)z.resolve();else z.reject(new Error(J.error||"write failed"));return}if(J.__om_api_r){const z=k[J.id];if(!z)return;if(J.error!=null){clearTimeout(z.t);delete k[J.id];z.reject(new Error(J.error));return}if(J.chunk!=null){clearTimeout(z.t);z.t=z.arm();z.text+=J.chunk;return}if(J.done){clearTimeout(z.t);delete k[J.id];z.resolve(J.text!=null?J.text:z.text);return}return}if(!J.__om_eval)return;const W=J;const Z=(z)=>{Q.source.postMessage({...z,__om_eval_r:1,id:W.id},Q.origin)};try{const z=(0,eval)(W.code);if(z&&typeof z.then==="function"){z.then((V)=>Z({ok:1,v:U(V)}),(V)=>Z({ok:0,e:String(V)}))}else{Z({ok:1,v:U(z)})}}catch(z){Z({ok:0,e:String(z)})}})}})();
</script>

  <meta charset="UTF-8" data-om-id="26c5adaa:2"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0" data-om-id="26c5adaa:3"/>
  <title>Youth-Moa 인터랙티브 프로토타입</title>
  <link href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css" rel="stylesheet" data-om-id="26c5adaa:4"/>
  <script src="https://unpkg.com/react@18.3.1/umd/react.development.js" integrity="sha384-hD6/rw4ppMLGNu3tX5cjIb+uRZ7UkRJ6BPkLpg4hAu/6onKUg4lLsHAs9EBPT82L" crossorigin="anonymous"></script>
  <script src="https://unpkg.com/react-dom@18.3.1/umd/react-dom.development.js" integrity="sha384-u6aeetuaXnQ38mYT8rp6sbXaQe3NL9t+IBXmnYxwkUI2Hw4bsp2Wvmx4yRQF1uAm" crossorigin="anonymous"></script>
  <script src="https://unpkg.com/@babel/standalone@7.29.0/babel.min.js" integrity="sha384-m08KidiNqLdpJqLq95G/LEi8Qvjl/xUYll3QILypMoQ65QorJ9Lvtp2RXYGBFj1y" crossorigin="anonymous"></script>
  <style>
    *, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: 'Pretendard', -apple-system, sans-serif; background: #0F0F13; min-height: 100vh; }
    img { display: block; }
    ::-webkit-scrollbar { width: 6px; } ::-webkit-scrollbar-thumb { background: #ccc; border-radius: 3px; }
    input[type="date"]::-webkit-calendar-picker-indicator { opacity: 0; cursor: pointer; }
    @keyframes fadeSlideUp { from { opacity:0; transform:translateY(16px); } to { opacity:1; transform:translateY(0); } }
    @keyframes fadeIn { from { opacity:0; } to { opacity:1; } }
    @keyframes slideDown { from { opacity:0; transform:translateY(-8px); } to { opacity:1; transform:translateY(0); } }
    .screen-enter { animation: fadeSlideUp 280ms ease forwards; }
    .overlay-enter { animation: fadeIn 180ms ease forwards; }
    .dropdown-enter { animation: slideDown 180ms ease forwards; }
    .btn-hover { transition: all 150ms ease; cursor: pointer; }
    .btn-hover:hover { filter: brightness(0.92); transform: translateY(-1px); }
    .btn-hover:active { transform: scale(0.97); }
    .card-hover { transition: all 200ms ease; cursor: pointer; }
    .card-hover:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(63,48,233,0.14) !important; border-color: #3F30E9 !important; }
    .nav-link { cursor: pointer; transition: color 120ms; }
    .nav-link:hover { opacity: 0.8; }
    input:focus { outline: none; }
  </style>
</head>
<body data-om-id="26c5adaa:5">
<div id="root" data-om-id="26c5adaa:6"></div>

<script type="text/babel">
const { useState, useEffect, useRef, useCallback } = React;

// ── Design Tokens ──
const T = {
  primary: '#3F30E9', primaryDark: '#3428CF',
  primaryLight: 'oklch(0.93 0.0425 280)', primaryBg: 'oklch(0.96 0.0255 280)',
  secondary: '#F97316', success: '#10B981', successLight: '#D1FAE5',
  warning: '#F59E0B', warningLight: '#FEF3C7', error: '#EF4444',
  // ⚠️ 시맨틱 base 색(secondary/success/warning/error)은 흰 배경 위 본문 텍스트로 직접 사용 시
  // WCAG AA 4.5:1 미달(2.3~3.7:1). 본 코드(src/)에 반영 시 -text 변형을 사용할 것.
  // 근거: boardroom 20260623-06 옵션 B-1, docs/02_design/03_token-policy.md §1 규칙 6.
  secondaryText: '#C2410C', // orange-700, contrast 5.20:1 (AA)
  successText:   '#047857', // emerald-700, contrast 5.50:1 (AA)
  warningText:   '#B45309', // amber-700, contrast 5.05:1 (AA)
  errorText:     '#B91C1C', // red-700, contrast 6.48:1 (AA)
  bg: 'oklch(0.985 0.004 280)', surface: '#FFFFFF',
  text: 'oklch(0.22 0.051 280)', textSec: 'oklch(0.55 0.034 280)', textTri: 'oklch(0.7 0.02 280)',
  border: 'oklch(0.9 0.014 280)', borderLight: 'oklch(0.95 0.007 280)',
  shadow: '0 1px 3px rgba(63,48,233,0.06)', shadowMd: '0 4px 12px rgba(63,48,233,0.07)',
  shadowLg: '0 10px 24px rgba(63,48,233,0.09)',
  radius: 12, tagR: 20, headerH: 68,
};

const IMGS = {
  banner: 'https://images.unsplash.com/photo-1531482615713-2afd69097998?w=1440&h=560&fit=crop',
  pg: ['https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=400&h=280&fit=crop','https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=400&h=280&fit=crop','https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=400&h=280&fit=crop','https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=400&h=280&fit=crop','https://images.unsplash.com/photo-1531482615713-2afd69097998?w=400&h=280&fit=crop','https://images.unsplash.com/photo-1552664730-d307ca884978?w=400&h=280&fit=crop'],
  space: ['https://images.unsplash.com/photo-1497366216548-37526070297c?w=460&h=340&fit=crop','https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=460&h=340&fit=crop','https://images.unsplash.com/photo-1524758631624-e2822e304c36?w=460&h=340&fit=crop'],
};

const PROGRAMS = [
  { id:1,  title:'취업역량 강화 워크숍',      center:'내일스퀘어',      region:'부천시',   status:'진행중', date:'2024-08-01~2024-08-31', cap:30, cat:'취업' },
  { id:2,  title:'청년 창업 아카데미',         center:'상상대로',        region:'수원시',   status:'진행중', date:'2024-08-01~2024-08-31', cap:25, cat:'창업' },
  { id:3,  title:'마음건강 힐링 캠프',         center:'범계역 청년출구', region:'안양시',   status:'진행중', date:'2024-07-01~2024-07-31', cap:20, cat:'힐링' },
  { id:4,  title:'디지털 마케팅 실전반',       center:'원미청정구역',    region:'부천시',   status:'마감',   date:'2024-07-01~2024-07-31', cap:15, cat:'교육' },
  { id:5,  title:'AI 활용 실무 교육',          center:'비행지구',        region:'고양시',   status:'진행중', date:'2024-07-01~2024-07-31', cap:30, cat:'교육' },
  { id:6,  title:'소셜벤처 인큐베이팅',        center:'오름',            region:'용인시',   status:'진행중', date:'2024-07-01~2024-07-31', cap:20, cat:'창업' },
  { id:7,  title:'청년 주거 지원 상담',        center:'두드림',          region:'성남시',   status:'진행중', date:'2024-08-01~2024-08-31', cap:15, cat:'주거' },
  { id:8,  title:'금융 기초 역량 교육',        center:'청춘세가',        region:'안산시',   status:'진행중', date:'2024-08-01~2024-08-31', cap:20, cat:'교육' },
  { id:9,  title:'청년 멘토링 네트워크',       center:'청년벙커',        region:'평택시',   status:'마감',   date:'2024-06-01~2024-06-30', cap:30, cat:'취업' },
  { id:10, title:'창의 아이디어 챌린지',       center:'함께누리',        region:'의정부시', status:'진행중', date:'2024-08-01~2024-08-31', cap:25, cat:'창업' },
  { id:11, title:'ESG 청년 리더십 캠프',       center:'꿈틀',            region:'시흥시',   status:'진행중', date:'2024-07-15~2024-08-15', cap:20, cat:'교육' },
  { id:12, title:'스타트업 네트워킹 데이',     center:'청소년의꿈',      region:'화성시',   status:'진행중', date:'2024-08-01~2024-08-31', cap:15, cat:'창업' },
  { id:13, title:'청년 문화예술 창작 지원',    center:'아이디어팩토리',  region:'광명시',   status:'진행중', date:'2024-08-01~2024-08-31', cap:25, cat:'문화' },
  { id:14, title:'지역사회 문제해결 워크숍',   center:'뉴웨이',          region:'수원시',   status:'마감',   date:'2024-07-01~2024-07-31', cap:15, cat:'교육' },
];
const REGIONS = ['수원시','성남시','부천시','안양시','안산시','고양시','용인시','평택시','의정부시','시흥시','화성시','광명시'];
const CENTERS = ['내일스퀘어','상상대로','범계역 청년출구','원미청정구역','비행지구','오름','두드림','청춘세가','청년벙커','함께누리','꿈틀','청소년의꿈','아이디어팩토리','뉴웨이'];

const NOTICES = [
  { id:1, cat:'행사', title:'제1회 청년의 날 축제 안내', date:'2024.07.30', views:756, pin:true },
  { id:2, cat:'공지', title:'7월 청년센터 프로그램 일정 안내', date:'2024.06.15', views:7178, isNew:true },
  { id:3, cat:'운영', title:'7월 휴관 일정 안내', date:'2024.06.15', views:7756 },
  { id:4, cat:'기타', title:'[경기도] 2024 경기 사회적 경제 박람회', date:'2024.05.15', views:157 },
];

// ── Icon ──
function Icon({ n, size=20, color='currentColor', style:xs, ...rest }) {
  const paths = {
    bell: <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
    search: <><circle cx="11" cy="11" r="8" stroke={color} strokeWidth="1.5" fill="none"/><path d="M21 21l-4.35-4.35" stroke={color} strokeWidth="1.5" strokeLinecap="round"/></>,
    star: <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" stroke={color} strokeWidth="1.5" fill="none" strokeLinejoin="round"/>,
    starFill: <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" fill={color} stroke={color} strokeWidth="1"/>,
    check: <><circle cx="12" cy="12" r="10" stroke={color} strokeWidth="1.5" fill="none"/><path d="M9 12l2 2 4-4" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></>,
    chevR: <path d="M9 18l6-6-6-6" stroke={color} strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
    chevD: <path d="M6 9l6 6 6-6" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
    pin: <><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" stroke={color} strokeWidth="1.5" fill="none"/><circle cx="12" cy="10" r="3" stroke={color} strokeWidth="1.5" fill="none"/></>,
    share: <><circle cx="18" cy="5" r="3" stroke={color} strokeWidth="1.5" fill="none"/><circle cx="6" cy="12" r="3" stroke={color} strokeWidth="1.5" fill="none"/><circle cx="18" cy="19" r="3" stroke={color} strokeWidth="1.5" fill="none"/><path d="M8.59 13.51l6.83 3.98M15.41 6.51l-6.82 3.98" stroke={color} strokeWidth="1.5" fill="none"/></>,
    close: <path d="M18 6L6 18M6 6l12 12" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
    login: <><path d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4M10 17l5-5-5-5M15 12H3" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></>,
    user: <><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke={color} strokeWidth="1.5" fill="none"/><circle cx="12" cy="7" r="4" stroke={color} strokeWidth="1.5" fill="none"/></>,
    download: <><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></>,
    upload: <><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></>,
    filter: <path d="M22 3H2l8 9.46V19l4 2v-8.54L22 3z" stroke={color} strokeWidth="1.5" fill="none" strokeLinejoin="round"/>,
    arrowL: <path d="M19 12H5M12 5l-7 7 7 7" stroke={color} strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
    calendar: <><rect x="3" y="4" width="18" height="18" rx="2" stroke={color} strokeWidth="1.5" fill="none"/><path d="M16 2v4M8 2v4M3 10h18" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round"/></>,
    eye: <><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke={color} strokeWidth="1.5" fill="none"/><circle cx="12" cy="12" r="3" stroke={color} strokeWidth="1.5" fill="none"/></>,
    eyeOff: <><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24M1 1l22 22" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></>,
    file: <><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" stroke={color} strokeWidth="1.5" fill="none" strokeLinejoin="round"/><polyline points="14 2 14 8 20 8" stroke={color} strokeWidth="1.5" fill="none" strokeLinejoin="round"/></>,
    chevU: <path d="M18 15l-6-6-6 6" stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/>,
  };
  return <svg viewBox="0 0 24 24" style={{ width:size, height:size, flexShrink:0, ...xs }} {...rest}>{paths[n]||null}</svg>;
}

// ── Shared Button ──
function Btn({ children, variant='primary', size='m', fullWidth, icon, onClick, disabled, style:xs }) {
  const sizes = { s:{h:34,px:14,fs:13}, m:{h:42,px:20,fs:14}, l:{h:50,px:28,fs:16} };
  const s = sizes[size];
  const vars = {
    primary: { bg:T.primary, fg:'#fff', border:'none' },
    secondary: { bg:T.surface, fg:T.text, border:`1px solid ${T.text}` },
    outline: { bg:'transparent', fg:T.primary, border:`1px solid ${T.primary}` },
    ghost: { bg:'transparent', fg:T.textSec, border:`1px solid ${T.border}` },
    danger: { bg:T.error, fg:'#fff', border:'none' },
  };
  const v = vars[variant]||vars.primary;
  return (
    <div className="btn-hover" onClick={disabled?undefined:onClick} style={{ display:'inline-flex', alignItems:'center', justifyContent:'center', gap:6, height:s.h, padding:`0 ${s.px}px`, borderRadius:8, background:disabled?T.border:v.bg, color:disabled?T.textTri:v.fg, border:v.border, fontSize:s.fs, fontWeight:600, cursor:disabled?'not-allowed':'pointer', width:fullWidth?'100%':'auto', boxSizing:'border-box', userSelect:'none', opacity:disabled?0.7:1, ...xs }}>
      {icon && <Icon n={icon} size={s.fs+2} color={disabled?T.textTri:v.fg}/>}
      {children}
    </div>
  );
}

// ── Badge ──
function Badge({ text, variant='primary' }) {
  const v = { primary:{bg:T.primary,fg:'#fff'}, muted:{bg:T.border,fg:T.textSec}, success:{bg:T.success,fg:'#fff'}, warning:{bg:T.warning,fg:'#fff'}, secondary:{bg:T.secondary,fg:'#fff'} };
  const c = v[variant]||v.primary;
  return <span style={{ display:'inline-flex', alignItems:'center', padding:'3px 10px', borderRadius:T.tagR, background:c.bg, color:c.fg, fontSize:12, fontWeight:600, lineHeight:'18px' }}>{text}</span>;
}

// ── Toast ──
function Toast({ msg, onDone }) {
  useEffect(() => { const t = setTimeout(onDone, 2800); return ()=>clearTimeout(t); }, []);
  return (
    <div style={{ position:'fixed', top:90, left:'50%', transform:'translateX(-50%)', zIndex:1000, display:'flex', alignItems:'center', gap:10, padding:'14px 24px', background:T.surface, borderRadius:12, boxShadow:'0 8px 32px rgba(0,0,0,0.14)', border:`1px solid ${T.borderLight}`, animation:'slideDown 300ms ease forwards' }}>
      <div style={{ width:22, height:22, borderRadius:'50%', background:'#22C55E', display:'flex', alignItems:'center', justifyContent:'center' }}>
        <svg viewBox="0 0 24 24" style={{width:14,height:14}}><path d="M9 12l2 2 4-4" stroke="#fff" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>
      </div>
      <span style={{ fontSize:15, fontWeight:500, color:T.text }}>{msg}</span>
    </div>
  );
}

// ── Modal backdrop ──
function Modal({ children, onClose }) {
  return (
    <div className="overlay-enter" onClick={onClose} style={{ position:'fixed', inset:0, background:'rgba(0,0,0,0.45)', zIndex:500, display:'flex', alignItems:'center', justifyContent:'center' }}>
      <div onClick={e=>e.stopPropagation()}>{children}</div>
    </div>
  );
}

// ── Notification Panel ──
function NotifPanel({ onClose }) {
  const [items, setItems] = React.useState([
    { id:1, title:'프로그램 신청 승인', body:'[프로그램] 프로그램 신청이 완료되었습니다.', time:'2024-07-21', unread:true },
    { id:2, title:'프로그램 신청 취소', body:'[프로그램] 프로그램 신청 취소가 승인되었습니다.', time:'2024-07-21', unread:true },
    { id:3, title:'프로그램 신청 승인', body:'[프로그램] 프로그램 신청이 완료되었습니다.', time:'2024-07-21', unread:false },
    { id:4, title:'프로그램 신청 취소', body:'[프로그램] 프로그램 신청 취소가 승인되었습니다.', time:'2024-07-21', unread:false },
  ]);
  const unreadCount = items.filter(i=>i.unread).length;
  const markAllRead = () => setItems(prev=>prev.map(i=>({...i,unread:false})));
  const clearAll = () => setItems([]);
  const remove = (id) => setItems(prev=>prev.filter(i=>i.id!==id));

  return (
    <div className="dropdown-enter" style={{ width:380, background:T.surface, borderRadius:14, boxShadow:'0 12px 40px rgba(0,0,0,0.14)', border:`1px solid ${T.borderLight}`, overflow:'hidden' }}>
      <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'16px 18px', borderBottom:`1px solid ${T.borderLight}` }}>
        <div style={{ display:'flex', alignItems:'center', gap:6 }}>
          <span style={{ fontSize:16, fontWeight:700, color:T.text }}>알림</span>
          {unreadCount>0 && <span style={{ minWidth:18, height:18, padding:'0 5px', borderRadius:9, background:T.error, color:'#fff', fontSize:11, fontWeight:700, display:'flex', alignItems:'center', justifyContent:'center' }}>{unreadCount}</span>}
        </div>
        <div style={{ display:'flex', alignItems:'center', gap:8 }}>
          <span onClick={markAllRead} className="btn-hover" style={{ fontSize:12, color:T.textSec, cursor:'pointer' }}>모두 읽음</span>
          <span style={{ fontSize:12, color:T.border }}>|</span>
          <span onClick={clearAll} className="btn-hover" style={{ fontSize:12, color:T.textSec, cursor:'pointer' }}>모두 지우기</span>
        </div>
      </div>
      {items.length===0 ? (
        <div style={{ display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', padding:'40px 0', gap:12 }}>
          <Icon n="bell" size={40} color={T.border}/>
          <span style={{ fontSize:14, color:T.textSec }}>아직 도착한 알림이 없어요</span>
        </div>
      ) : (
        items.map(item=>(
          <div key={item.id} style={{ display:'flex', alignItems:'flex-start', gap:12, padding:'14px 16px', borderBottom:`1px solid ${T.borderLight}`, background:item.unread?T.primaryBg:T.surface }}>
            <div style={{ width:36, height:36, borderRadius:'50%', background:item.unread?T.primary:'#E5E7EB', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
              <Icon n="bell" size={16} color={item.unread?'#fff':'#9CA3AF'}/>
            </div>
            <div style={{ flex:1, minWidth:0 }}>
              <div style={{ fontSize:14, color:item.unread?T.text:T.textSec, fontWeight:item.unread?600:400, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{item.title}</div>
              <div style={{ fontSize:13, color:T.textSec, marginTop:2, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{item.body}</div>
              <div style={{ fontSize:12, color:T.textTri, marginTop:4 }}>{item.time}</div>
            </div>
            <div onClick={()=>remove(item.id)} className="btn-hover" style={{ cursor:'pointer', flexShrink:0, marginTop:2 }}>
              <Icon n="close" size={16} color={T.textTri}/>
            </div>
          </div>
        ))
      )}
      <div style={{ padding:'12px 18px', textAlign:'center', cursor:'pointer', fontSize:13, fontWeight:600, color:T.primary }} className="btn-hover">알림 전체보기</div>
    </div>
  );
}

// ── User Menu ──
function UserMenuPanel({ onClose, onLogout, onMyPage }) {
  return (
    <div className="dropdown-enter" style={{ width:220, background:T.surface, borderRadius:14, boxShadow:'0 12px 40px rgba(0,0,0,0.14)', border:`1px solid ${T.borderLight}`, overflow:'hidden' }}>
      <div style={{ display:'flex', alignItems:'center', gap:10, padding:'16px 18px', borderBottom:`1px solid ${T.borderLight}` }}>
        <div style={{ width:38, height:38, borderRadius:'50%', background:T.primaryLight, display:'flex', alignItems:'center', justifyContent:'center' }}>
          <span style={{ fontWeight:700, fontSize:16, color:T.primary }}>박</span>
        </div>
        <div>
          <div style={{ fontSize:15, fontWeight:600, color:T.text }}>박시현님</div>
          <div style={{ fontSize:12, color:T.textTri }}>hyuuun0321@naver.com</div>
        </div>
      </div>
      {[{icon:'calendar',label:'프로그램 신청 내역',action:onMyPage},{icon:'star',label:'즐겨찾기한 프로그램',action:onMyPage},{icon:'user',label:'개인 정보 수정',action:onMyPage}].map(m=>(
        <div key={m.label} onClick={()=>{onClose();m.action?.();}} className="btn-hover" style={{ display:'flex', alignItems:'center', gap:10, padding:'11px 18px', cursor:'pointer' }}>
          <Icon n={m.icon} size={17} color={T.textSec}/>
          <span style={{ fontSize:14, color:T.text }}>{m.label}</span>
        </div>
      ))}
      <div style={{ borderTop:`1px solid ${T.borderLight}` }}>
        <div onClick={()=>{onClose();onLogout();}} className="btn-hover" style={{ display:'flex', alignItems:'center', gap:10, padding:'11px 18px', cursor:'pointer' }}>
          <Icon n="login" size={17} color={T.textTri} style={{transform:'scaleX(-1)'}}/>
          <span style={{ fontSize:14, color:T.textSec }}>로그아웃</span>
        </div>
      </div>
    </div>
  );
}

// ── Header ──
function Header({ screen, go, isLoggedIn, onLoginClick, onLogout, onMyPage, scrolled }) {
  const [showNotif, setShowNotif] = useState(false);
  const [showUser, setShowUser] = useState(false);
  const isHome = screen==='home';
  const transparent = isHome && !scrolled;
  const fg = transparent ? '#fff' : T.text;
  const fgSec = transparent ? 'rgba(255,255,255,0.85)' : T.textSec;
  const nav = [
    { label:'프로그램', screen:'programs' },
    { label:'청년센터', screen:'centers' },
    { label:'공지사항', screen:'notices' },
  ];
  return (
    <div style={{ width:'100%', height:T.headerH, background:transparent?'transparent':T.surface, borderBottom:transparent?'none':`1px solid ${T.border}`, display:'flex', alignItems:'center', padding:'0 80px', boxSizing:'border-box', position:'relative', zIndex:100, transition:'background 300ms ease, border-color 300ms ease, box-shadow 300ms ease', boxShadow:transparent?'none':'0 1px 8px rgba(0,0,0,0.06)' }}>
      {/* Logo */}
      <div style={{ flex:1 }}>
        <div onClick={()=>go('home')} style={{ display:'inline-flex', alignItems:'center', gap:8, cursor:'pointer' }}>
          <img src="assets/logo_symbol.png" alt="" style={{ height:34, width:'auto',
            filter:transparent?'brightness(0) invert(1)':'none', transition:'filter 300ms' }}/>
          <span style={{ fontWeight:800, fontSize:18, letterSpacing:-0.3,
            color:transparent?'#fff':T.primary, transition:'color 300ms' }}>청년모아</span>
        </div>
      </div>
      {/* Nav */}
      <div style={{ display:'flex', gap:36 }}>
        {nav.map(n => (
          <span key={n.label} className="nav-link" onClick={()=>go(n.screen)} style={{ fontSize:15, fontWeight:screen===n.screen?600:400, color:transparent?(screen===n.screen?'#fff':'rgba(255,255,255,0.88)'):(screen===n.screen?T.primary:T.text), paddingBottom:4, borderBottom:screen===n.screen?`2px solid ${transparent?'#fff':T.primary}`:'2px solid transparent' }}>{n.label}</span>
        ))}
      </div>
      {/* Right */}
      <div style={{ flex:1, display:'flex', gap:16, alignItems:'center', justifyContent:'flex-end', position:'relative' }}>
        <Icon n="search" size={20} color={fgSec} style={{cursor:'pointer'}} onClick={()=>go('search')}/>
        {isLoggedIn && (
          <div style={{ position:'relative', display:'flex', alignItems:'center' }}>
            <div style={{ position:'relative', display:'flex', alignItems:'center', cursor:'pointer' }} onClick={()=>{setShowNotif(v=>!v); setShowUser(false);}}>
              <Icon n="bell" size={20} color={fgSec}/>
              <span style={{ position:'absolute', top:-2, right:-2, width:7, height:7, borderRadius:'50%', background:T.error, border:`1.5px solid ${transparent?'transparent':T.surface}` }}/>
            </div>
            {showNotif && (
              <div style={{ position:'absolute', top:34, right:-10, zIndex:200 }}>
                <NotifPanel onClose={()=>setShowNotif(false)}/>
              </div>
            )}
          </div>
        )}
        {isLoggedIn ? (
          <div style={{ position:'relative' }}>
            <div className="btn-hover" onClick={()=>{setShowUser(v=>!v); setShowNotif(false);}} style={{ display:'flex', alignItems:'center', gap:8, cursor:'pointer' }}>
              <div style={{ width:30, height:30, borderRadius:'50%', background:transparent?'rgba(255,255,255,0.25)':T.primaryLight, display:'flex', alignItems:'center', justifyContent:'center' }}>
                <span style={{ fontWeight:700, fontSize:13, color:transparent?'#fff':T.primary }}>박</span>
              </div>
              <span style={{ fontSize:14, fontWeight:500, color:transparent?'#fff':T.text }}>박시현님</span>
              <Icon n="chevD" size={14} color={transparent?'rgba(255,255,255,0.8)':T.textSec}/>
            </div>
            {showUser && (
              <div style={{ position:'absolute', top:40, right:0, zIndex:200 }}>
                <UserMenuPanel onClose={()=>setShowUser(false)} onLogout={onLogout} onMyPage={()=>go('mypage')}/>
              </div>
            )}
          </div>
        ) : (
          <Icon n="login" size={21} color={transparent?'#fff':T.primary} style={{cursor:'pointer'}} onClick={onLoginClick}/>
        )}
      </div>
    </div>
  );
}

// ── Footer ──
function Footer() {
  const links = ['개인정보처리방침','이용약관','이메일주소무단수집거부'];
  const socials = [
    { l:'Instagram', src:'assets/sns_instagram.png' },
    { l:'YouTube',   src:'assets/sns_youtube.png' },
    { l:'KakaoTalk', src:'assets/sns_kakaotalk.png' },
    { l:'Facebook',  src:'assets/sns_facebook.png' },
  ];
  return (
    <div style={{ background:'#F5F5F5', borderTop:`1px solid ${T.border}`, padding:'20px 80px 16px' }}>
      <div style={{ display:'flex', alignItems:'center', gap:40 }}>
        {/* 로고 */}
        <div style={{ display:'inline-flex', alignItems:'center', gap:7, flexShrink:0 }}>
          <img src="assets/logo_symbol.png" alt="" style={{ height:28, width:'auto' }}/>
          <span style={{ fontWeight:800, fontSize:16, letterSpacing:-0.3, color:T.primary }}>청년모아</span>
        </div>
        {/* 링크 + Copyright */}
        <div style={{ flex:1 }}>
          <div style={{ display:'flex', gap:16, marginBottom:6 }}>
            {links.map(l=>(
              <span key={l} style={{ fontSize:11, fontWeight:500, color:'#444', cursor:'pointer' }}>{l}</span>
            ))}
          </div>
          <span style={{ fontSize:11, color:'#888' }}>Copyright © 2024 청년모아 All Rights Reserved</span>
        </div>
        {/* SNS */}
        <div style={{ display:'flex', gap:8, alignItems:'center', flexShrink:0 }}>
          {socials.map(s=>(
            <img key={s.l} src={s.src} alt={s.l} style={{ width:24, height:24, cursor:'pointer' }}/>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── HOME ──
function HomeScreen({ go, isLoggedIn }) {
  const [search, setSearch] = useState('');
  const [fav, setFav] = useState(new Set([1]));
  const [pgPage, setPgPage] = useState(0);
  const totalPgPages = Math.ceil(PROGRAMS.length / 4);
  return (
    <div className="screen-enter" style={{ background:T.bg }}>
      {/* Hero */}
      <div style={{ position:'relative', height:488 + T.headerH, overflow:'hidden', marginTop:-T.headerH }}>
        <img src={IMGS.banner} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', position:'absolute', inset:0 }}/>
        <div style={{ position:'absolute', inset:0, background:'linear-gradient(135deg, oklch(0.34 0.17 280) 0%, oklch(0.46 0.17 280) 45%, oklch(0.66 0.119 280) 100%)' }}/>
        <div style={{ position:'absolute', top:0, left:0, right:0, height:110, background:'linear-gradient(rgba(0,0,0,0.28),transparent)', zIndex:5 }}/>
        <div style={{ position:'absolute', top:T.headerH, left:0, right:0, bottom:0, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', textAlign:'center', padding:'0 40px', zIndex:2 }}>
          <div style={{ display:'inline-flex', padding:'6px 16px', borderRadius:20, background:'rgba(255,255,255,0.2)', backdropFilter:'blur(4px)', marginBottom:16 }}>
            <span style={{ color:'#fff', fontSize:14, fontWeight:500 }}>경기도 청년센터 프로그램 통합 플랫폼</span>
          </div>
          <h1 style={{ color:'#fff', fontSize:42, fontWeight:800, lineHeight:1.3, margin:0 }}>경기도 청년의 내일을<br/>함께 만들어갑니다</h1>
          <p style={{ color:'rgba(255,255,255,0.85)', fontSize:16, marginTop:12 }}>경기도 31개 시·군 청년센터의 프로그램을 한눈에 확인하세요</p>
          <div style={{ display:'flex', alignItems:'center', background:'#fff', borderRadius:T.tagR, padding:'0 6px 0 18px', height:48, width:400, marginTop:24, boxShadow:'0 4px 20px rgba(0,0,0,0.15)' }}>
            <Icon n="search" size={18} color="#94A3B8"/>
            <input value={search} onChange={e=>setSearch(e.target.value)} onKeyDown={e=>e.key==='Enter'&&go('programs')} placeholder="프로그램, 센터명으로 검색" style={{ flex:1, border:'none', outline:'none', fontSize:14, color:T.text, marginLeft:10, background:'transparent' }}/>
            <div className="btn-hover" onClick={()=>go('programs')} style={{ height:38, padding:'0 18px', background:T.primary, borderRadius:T.tagR, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
              <span style={{ color:'#fff', fontSize:13, fontWeight:600 }}>검색</span>
            </div>
          </div>
        </div>
      </div>
      {/* Stats */}
      <div style={{ display:'flex', justifyContent:'center', gap:48, padding:'28px 80px', background:T.surface, borderBottom:`1px solid ${T.borderLight}` }}>
        {[{n:'진행중 프로그램',v:'127'},{n:'참여 청년센터',v:'31'},{n:'누적 참여자',v:'15,420'}].map(s=>(
          <div key={s.n} style={{ display:'flex', alignItems:'center', gap:14 }}>
            <div style={{ width:44, height:44, borderRadius:12, background:T.primaryLight, display:'flex', alignItems:'center', justifyContent:'center' }}>
              <Icon n="check" size={22} color={T.primary}/>
            </div>
            <div>
              <div style={{ fontSize:22, fontWeight:700, color:T.text }}>{s.v}</div>
              <div style={{ fontSize:13, color:T.textSec }}>{s.n}</div>
            </div>
          </div>
        ))}
      </div>
      {/* Programs */}
      <div style={{ padding:'44px 80px 40px' }}>
        <div style={{ marginBottom:24 }}>
          <div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:4 }}>
            <div style={{ fontSize:24, fontWeight:700, color:T.text }}>프로그램</div>
            <div className="btn-hover" onClick={()=>go('programs')}
                 style={{ width:26, height:26, borderRadius:'50%', border:`1px solid ${T.border}`, background:T.surface, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
              <Icon n="chevR" size={14} color={T.textSec}/>
            </div>
          </div>
          <div style={{ fontSize:14, color:T.textSec }}>진행중인 프로그램을 소개해드려요</div>
        </div>
        <div style={{ position:'relative' }}>
          {pgPage > 0 && (
            <div onClick={()=>setPgPage(p=>p-1)} className="btn-hover"
                 style={{ position:'absolute', left:-20, top:'50%', transform:'translateY(-50%)', zIndex:10,
                          width:36, height:36, borderRadius:'50%', background:T.surface,
                          boxShadow:T.shadowMd, border:`1px solid ${T.border}`,
                          display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
              <Icon n="arrowL" size={16} color={T.textSec}/>
            </div>
          )}
          <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:20 }}>
            {PROGRAMS.slice(pgPage*4, pgPage*4+4).map((pg,i)=>(
              <div key={pg.id} className="card-hover" onClick={()=>go('program-detail',{pg})}
                   style={{ borderRadius:T.radius, overflow:'hidden', background:T.surface, boxShadow:T.shadowMd, border:`1px solid ${T.borderLight}` }}>
                <div style={{ width:'100%', height:170, position:'relative', overflow:'hidden', background:'#e5e7eb' }}>
                  <img src={IMGS.pg[(pgPage*4+i)%6]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
                  <div style={{ position:'absolute', top:10, left:10 }}><Badge text={pg.status} variant={pg.status==='마감'?'muted':'primary'}/></div>
                  <div onClick={e=>{e.stopPropagation();setFav(f=>{const n=new Set(f);n.has(pg.id)?n.delete(pg.id):n.add(pg.id);return n;});}} style={{ position:'absolute', top:10, right:10, cursor:'pointer', zIndex:5 }}>
                    <Icon n={fav.has(pg.id)?'starFill':'star'} size={20} color={fav.has(pg.id)?'#F59E0B':'rgba(255,255,255,0.9)'}/>
                  </div>
                </div>
                <div style={{ padding:'12px 14px 14px' }}>
                  <div style={{ fontWeight:600, fontSize:15, color:T.text, marginBottom:8, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{pg.title}</div>
                  <div style={{ fontSize:12, color:T.textSec, marginBottom:6 }}>{pg.center}</div>
                  <div style={{ fontSize:12, color:T.textTri }}>{pg.date.replace('~',' ~ ')}</div>
                </div>
              </div>
            ))}
          </div>
          {pgPage < totalPgPages-1 && (
            <div onClick={()=>setPgPage(p=>p+1)} className="btn-hover"
                 style={{ position:'absolute', right:-20, top:'50%', transform:'translateY(-50%)', zIndex:10,
                          width:36, height:36, borderRadius:'50%', background:T.surface,
                          boxShadow:T.shadowMd, border:`1px solid ${T.border}`,
                          display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
              <div style={{ transform:'scaleX(-1)', display:'flex' }}>
                <Icon n="arrowL" size={16} color={T.textSec}/>
              </div>
            </div>
          )}
        </div>
      </div>
      {/* Notices */}
      <div style={{ padding:'40px 80px 48px', background:T.primaryBg }}>
        <div style={{ marginBottom:24 }}>
          <div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:4 }}>
            <div style={{ fontSize:24, fontWeight:700, color:T.text }}>공지사항</div>
            <div className="btn-hover" onClick={()=>go('notices')}
                 style={{ width:26, height:26, borderRadius:'50%', border:`1px solid ${T.border}`, background:T.surface, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
              <Icon n="chevR" size={14} color={T.textSec}/>
            </div>
          </div>
          <div style={{ fontSize:14, color:T.textSec }}>청년센터 소식을 전해드려요</div>
        </div>
        <div style={{ display:'flex', gap:24 }}>
          <div className="card-hover" onClick={()=>go('notice-detail',{notice:NOTICES[0]})} style={{ width:360, borderRadius:T.radius, overflow:'hidden', background:T.surface, boxShadow:T.shadowMd, flexShrink:0 }}>
            <div style={{ height:180, background:'#e5e7eb', overflow:'hidden' }}>
              <img src="https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=360&h=220&fit=crop" alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
            </div>
            <div style={{ padding:'16px 20px 20px' }}>
              <Badge text="📌 주요" variant="primary"/>
              <div style={{ fontWeight:700, fontSize:17, color:T.text, margin:'8px 0' }}>제1회 청년의 날 축제 안내</div>
              <div style={{ fontSize:13, color:T.textSec }}>2024.07.30 · 조회 756</div>
            </div>
          </div>
          <div style={{ flex:1, background:T.surface, borderRadius:T.radius, boxShadow:T.shadow, overflow:'hidden' }}>
            {NOTICES.slice(1).map((n,i)=>(
              <div key={n.id} onClick={()=>go('notice-detail',{notice:n})} className="card-hover" style={{ display:'flex', alignItems:'center', gap:14, padding:'0 24px', height:64, borderBottom:i<2?`1px solid ${T.borderLight}`:'none', cursor:'pointer' }}>
                <Badge text={n.cat} variant={n.cat==='공지'?'primary':n.cat==='운영'?'success':'muted'}/>
                <span style={{ flex:1, fontSize:14, fontWeight:500, color:T.text, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{n.title}</span>
                <span style={{ fontSize:12, color:T.textTri, flexShrink:0 }}>{n.date}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
      {/* Space Section */}
      <div style={{ padding:'40px 80px 48px' }}>
        <div style={{ display:'flex', alignItems:'flex-end', justifyContent:'space-between', marginBottom:24 }}>
          <div>
            <div style={{ fontSize:24, fontWeight:700, color:T.text }}>공간안내</div>
            <div style={{ fontSize:14, color:T.textSec, marginTop:2 }}>청년센터 공간을 소개해드려요</div>
          </div>
          <Btn variant="outline" size="s" icon="pin" onClick={()=>go('centers')}>지도에서 전체 센터 보기</Btn>
        </div>
        <div style={{ display:'flex', gap:20 }}>
          {IMGS.space.map((src,i)=>(
            <div key={i} className="card-hover" style={{ flex:1, height:240, borderRadius:T.radius, overflow:'hidden', position:'relative' }}>
              <img src={src} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
              <div style={{ position:'absolute', bottom:0, left:0, right:0, padding:'40px 18px 16px', background:'linear-gradient(transparent,rgba(0,0,0,0.6))' }}>
                <span style={{ color:'#fff', fontWeight:600, fontSize:15 }}>{['상상대로','내일스퀘어','비행지구'][i]}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
      <Footer/>
    </div>
  );
}

// ── FILTER MODAL (4.11) ──
function FilterModal({ type, items, applied, onApply, onClose }) {
  const [tempSel, setTempSel] = useState(() => new Set(applied));
  const [query, setQuery] = useState('');
  const visible = items.filter(i => i.label.includes(query));
  const toggleT = val => setTempSel(s => { const n=new Set(s); n.has(val)?n.delete(val):n.add(val); return n; });
  const overlay = (
    <div className="overlay-enter" onClick={onClose}
         style={{ position:'fixed', inset:0, background:'rgba(0,0,0,0.45)', zIndex:1000, display:'flex', alignItems:'center', justifyContent:'center' }}>
      <div className="dropdown-enter" onClick={e=>e.stopPropagation()}
           style={{ width:560, maxHeight:580, background:'#fff', borderRadius:16, display:'flex', flexDirection:'column', overflow:'hidden', boxShadow:'0 20px 60px rgba(0,0,0,0.18)' }}>
        {/* Header */}
        <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'20px 24px 16px', borderBottom:`1px solid ${T.borderLight}` }}>
          <span style={{ fontSize:17, fontWeight:700, color:T.text }}>{type==='region'?'지역':'청년센터'} 전체 보기</span>
          <div onClick={onClose} className="btn-hover" style={{ cursor:'pointer', padding:4, borderRadius:6 }}>
            <Icon n="close" size={20} color={T.textSec}/>
          </div>
        </div>
        {/* Search */}
        <div style={{ padding:'12px 24px', borderBottom:`1px solid ${T.borderLight}` }}>
          <div style={{ display:'flex', alignItems:'center', gap:8, padding:'8px 12px', border:`1px solid ${T.border}`, borderRadius:8 }}>
            <Icon n="search" size={16} color={T.textTri}/>
            <input value={query} onChange={e=>setQuery(e.target.value)}
                   placeholder={type==='region'?'지역 검색':'센터 검색'}
                   style={{ flex:1, border:'none', background:'transparent', fontSize:13, color:T.text, outline:'none' }}/>
          </div>
        </div>
        {/* Grid */}
        <div style={{ flex:1, overflowY:'auto', padding:'16px 24px' }}>
          {visible.length===0
            ? <div style={{ textAlign:'center', padding:'32px 0', fontSize:14, color:T.textTri }}>검색 결과가 없습니다.</div>
            : <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:8 }}>
                {visible.map(item=>(
                  <div key={item.label} onClick={()=>toggleT(item.label)}
                       style={{ display:'flex', alignItems:'center', gap:8, padding:'9px 12px', borderRadius:8, cursor:'pointer', transition:'all 150ms',
                                background:tempSel.has(item.label)?T.primaryBg:'transparent',
                                border:`1px solid ${tempSel.has(item.label)?T.primary:T.borderLight}` }}>
                    <div style={{ width:15, height:15, borderRadius:3, flexShrink:0,
                                  border:`1.5px solid ${tempSel.has(item.label)?T.primary:T.border}`,
                                  background:tempSel.has(item.label)?T.primary:'#fff',
                                  display:'flex', alignItems:'center', justifyContent:'center' }}>
                      {tempSel.has(item.label)&&<svg viewBox="0 0 24 24" style={{width:9,height:9}}><path d="M5 12l5 5L20 7" stroke="#fff" strokeWidth="3" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>}
                    </div>
                    <span style={{ fontSize:12, color:tempSel.has(item.label)?T.primary:T.text, fontWeight:tempSel.has(item.label)?600:400, flex:1 }}>{item.label}</span>
                    <span style={{ fontSize:11, color:T.textTri }}>({item.count})</span>
                  </div>
                ))}
              </div>
          }
        </div>
        {/* Footer */}
        <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', padding:'14px 24px', borderTop:`1px solid ${T.borderLight}` }}>
          <div onClick={()=>setTempSel(new Set())} className="btn-hover nav-link"
               style={{ fontSize:13, color:T.textSec, cursor:'pointer', textDecoration:'underline' }}>선택 초기화</div>
          <div style={{ display:'flex', gap:8 }}>
            <div onClick={onClose} className="btn-hover"
                 style={{ padding:'9px 20px', borderRadius:8, border:`1px solid ${T.border}`, fontSize:13, color:T.textSec, cursor:'pointer', fontWeight:500 }}>취소</div>
            <div onClick={()=>onApply(new Set(tempSel))} className="btn-hover"
                 style={{ padding:'9px 20px', borderRadius:8, background:T.primary, fontSize:13, color:'#fff', cursor:'pointer', fontWeight:600 }}>
              {tempSel.size>0?`${tempSel.size}개 적용`:'적용'}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
  return ReactDOM.createPortal(overlay, document.body);
}

// ── PROGRAM LIST ──
const PG_PAGE = 9;
const SIDE_SHOW = 5;

function ProgramList({ go }) {
  const [selR, setSelR] = useState(new Set());
  const [selC, setSelC] = useState(new Set());
  const [onlyActive, setOnlyActive] = useState(true);
  const [sortBy, setSortBy] = useState('최신순');
  const [sortOpen, setSortOpen] = useState(false);
  const [page, setPage] = useState(1);
  const [fav, setFav] = useState(new Set([1]));
  const [modal, setModal] = useState(null); // null | 'region' | 'center'
  const [regOpen, setRegOpen] = useState(true);
  const [centerOpen, setCenterOpen] = useState(true);
  const [clickedId, setClickedId] = useState(null);
  const [hoveredId, setHoveredId] = useState(null);

  // 지역·센터별 프로그램 건수 (FilterModal 아이템 목록)
  const regionItems = REGIONS.map(r => ({ label:r, count:PROGRAMS.filter(p=>p.region===r).length }));
  const centerItems = CENTERS.map(c => ({ label:c, count:PROGRAMS.filter(p=>p.center===c).length }));

  const resetAll = () => { setSelR(new Set()); setSelC(new Set()); setOnlyActive(true); setPage(1); };
  const hasFilter = selR.size+selC.size > 0 || onlyActive;

  const changeSelR = s => { setSelR(s); setPage(1); };
  const changeSelC = s => { setSelC(s); setPage(1); };
  const changeOnlyActive = v => { setOnlyActive(v); setPage(1); };
  const changeSortBy = v => { setSortBy(v); setPage(1); };

  const toggleSideR = val => changeSelR((s => { const n=new Set(s); n.has(val)?n.delete(val):n.add(val); return n; })(selR));
  const toggleSideC = val => changeSelC((s => { const n=new Set(s); n.has(val)?n.delete(val):n.add(val); return n; })(selC));

  const filtered = PROGRAMS.filter(p => {
    if (selR.size>0 && !selR.has(p.region)) return false;
    if (selC.size>0 && !selC.has(p.center)) return false;
    if (onlyActive && p.status!=='진행중') return false;
    return true;
  }).sort((a,b) => sortBy==='인기순' ? b.cap-a.cap : b.id-a.id);

  const totalPages = Math.max(1, Math.ceil(filtered.length/PG_PAGE));
  const paged = filtered.slice((page-1)*PG_PAGE, page*PG_PAGE);

  const CircleCheck = ({ checked }) => (
    <div style={{ width:18, height:18, borderRadius:'50%', flexShrink:0,
                  border:`1.5px solid ${checked?T.primary:T.border}`,
                  background:checked?T.primary:'#fff',
                  display:'flex', alignItems:'center', justifyContent:'center', transition:'all 150ms' }}>
      <svg viewBox="0 0 24 24" style={{width:10,height:10}}>
        <path d="M5 12l5 5L20 7" stroke={checked?'#fff':'#D1D5DB'} strokeWidth="3" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    </div>
  );

  const sortOpts = ['최신순','인기순','마감임박순'];

  return (
    <div className="screen-enter" style={{ background:T.bg, minHeight:'100vh' }}>
      {/* ─ Filter Modal ─ */}
      {modal && (
        <FilterModal
          type={modal}
          items={modal==='region' ? regionItems : centerItems}
          applied={modal==='region' ? selR : selC}
          onApply={s => { modal==='region' ? changeSelR(s) : changeSelC(s); setModal(null); }}
          onClose={() => setModal(null)}
        />
      )}
      {/* Page Title */}
      <div style={{ padding:'28px 0 24px', textAlign:'center', background:T.surface, borderBottom:`1px solid ${T.borderLight}` }}>
        <h2 style={{ fontSize:28, fontWeight:700, color:T.text }}>프로그램</h2>
      </div>
      <div style={{ display:'flex', padding:'28px 80px 48px', gap:28, alignItems:'flex-start', maxWidth:1440, margin:'0 auto' }}>

        {/* ─ Sidebar (220px) ─ */}
        <div style={{ width:220, flexShrink:0 }}>
          {/* 필터 초기화 버튼 */}
          <div onClick={resetAll} className="btn-hover"
               style={{ display:'flex', alignItems:'center', justifyContent:'space-between',
                        padding:'11px 16px', marginBottom:16, borderRadius:12,
                        background:T.primary, cursor:'pointer', userSelect:'none' }}>
            <span style={{ fontSize:14, fontWeight:700, color:'#fff' }}>필터</span>
            <div style={{ display:'flex', alignItems:'center', gap:5 }}>
              <svg viewBox="0 0 24 24" style={{width:14,height:14,flexShrink:0}} fill="none">
                <path d="M1 4v6h6M23 20v-6h-6" stroke="#fff" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M20.49 9A9 9 0 005.64 5.64L1 10M23 14l-4.64 4.36A9 9 0 013.51 15" stroke="#fff" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              <span style={{ fontSize:13, color:'#fff', fontWeight:500 }}>초기화</span>
            </div>
          </div>

          {/* 지역 섹션 */}
          <div style={{ marginBottom:20 }}>
            <div onClick={()=>setRegOpen(v=>!v)}
                 style={{ display:'flex', alignItems:'center', justifyContent:'space-between',
                          padding:'11px 14px', background:T.surface, borderRadius:12,
                          border:`1px solid ${T.borderLight}`, marginBottom:regOpen?10:0,
                          boxShadow:T.shadow, cursor:'pointer', userSelect:'none' }}>
              <span style={{ fontWeight:700, fontSize:14, color:T.text }}>지역</span>
              <Icon n="chevD" size={16} color={T.textSec} style={{ transform:regOpen?'rotate(180deg)':'rotate(0deg)', transition:'transform 200ms' }}/>
            </div>
            {regOpen && (
              <>
                {REGIONS.slice(0,SIDE_SHOW).map(r=>{
                  const cnt = PROGRAMS.filter(p=>p.region===r).length;
                  return (
                    <div key={r} onClick={()=>toggleSideR(r)}
                         style={{ display:'flex', alignItems:'center', gap:6, marginBottom:8, cursor:'pointer', paddingLeft:16 }}>
                      <CircleCheck checked={selR.has(r)}/>
                      <span style={{ fontSize:13, color:selR.has(r)?T.primary:T.text, fontWeight:selR.has(r)?600:400 }}>{r}</span>
                      <span style={{ fontSize:12, color:T.primary, fontWeight:600, marginLeft:4 }}>{cnt}</span>
                    </div>
                  );
                })}
                <div onClick={()=>setModal('region')} className="btn-hover nav-link"
                     style={{ display:'flex', alignItems:'center', justifyContent:'flex-end', gap:3, marginTop:4, marginBottom:4, fontSize:12, color:T.textSec, cursor:'pointer' }}>
                  지역 더보기 <Icon n="chevR" size={13} color={T.textSec}/>
                </div>
              </>
            )}
          </div>

          {/* 청년센터 섹션 */}
          <div style={{ marginBottom:24 }}>
            <div onClick={()=>setCenterOpen(v=>!v)}
                 style={{ display:'flex', alignItems:'center', justifyContent:'space-between',
                          padding:'11px 14px', background:T.surface, borderRadius:12,
                          border:`1px solid ${T.borderLight}`, marginBottom:centerOpen?10:0,
                          boxShadow:T.shadow, cursor:'pointer', userSelect:'none' }}>
              <span style={{ fontWeight:700, fontSize:14, color:T.text }}>청년센터</span>
              <Icon n="chevD" size={16} color={T.textSec} style={{ transform:centerOpen?'rotate(180deg)':'rotate(0deg)', transition:'transform 200ms' }}/>
            </div>
            {centerOpen && (
              <>
                {CENTERS.slice(0,SIDE_SHOW).map(c=>{
                  const cnt = PROGRAMS.filter(p=>p.center===c).length;
                  return (
                    <div key={c} onClick={()=>toggleSideC(c)}
                         style={{ display:'flex', alignItems:'center', gap:6, marginBottom:8, cursor:'pointer', paddingLeft:16 }}>
                      <CircleCheck checked={selC.has(c)}/>
                      <span style={{ fontSize:13, color:selC.has(c)?T.primary:T.text, fontWeight:selC.has(c)?600:400 }}>{c}</span>
                      <span style={{ fontSize:12, color:T.primary, fontWeight:600, marginLeft:4 }}>{cnt}</span>
                    </div>
                  );
                })}
                <div onClick={()=>setModal('center')} className="btn-hover nav-link"
                     style={{ display:'flex', alignItems:'center', justifyContent:'flex-end', gap:3, marginTop:4, marginBottom:4, fontSize:12, color:T.textSec, cursor:'pointer' }}>
                  청년센터 더보기 <Icon n="chevR" size={13} color={T.textSec}/>
                </div>
              </>
            )}
          </div>
        </div>

        {/* ─ Main ─ */}
        <div style={{ flex:1 }}>
          {/* 정렬 바: 전체 N건 + 진행중만 보기 토글 + 정렬 옵션 */}
          <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:20 }}>
            <span style={{ fontSize:13, color:T.textSec }}>전체 <strong style={{color:T.text}}>{filtered.length}</strong>건</span>
            <div style={{ display:'flex', alignItems:'center', gap:14 }}>
              <label style={{ display:'flex', alignItems:'center', gap:7, cursor:'pointer', userSelect:'none' }}>
                <div onClick={()=>changeOnlyActive(!onlyActive)}
                     style={{ width:36, height:20, borderRadius:10, background:onlyActive?T.primary:T.border, position:'relative', transition:'background 200ms', cursor:'pointer', flexShrink:0 }}>
                  <div style={{ width:16, height:16, borderRadius:'50%', background:'#fff', position:'absolute', top:2, left:onlyActive?18:2, transition:'left 200ms', boxShadow:'0 1px 3px rgba(0,0,0,0.2)' }}/>
                </div>
                <span style={{ fontSize:13, color:T.textSec }}>진행중만 보기</span>
              </label>
              <div style={{ position:'relative' }}>
                <div onClick={()=>setSortOpen(v=>!v)} className="btn-hover"
                     style={{ display:'flex', alignItems:'center', gap:4, padding:'6px 12px', border:`1px solid ${T.border}`, borderRadius:8, fontSize:13, color:T.text, cursor:'pointer', background:T.surface, fontWeight:500 }}>
                  {sortBy}<Icon n="chevD" size={14} color={T.textTri}/>
                </div>
                {sortOpen && (
                  <div className="dropdown-enter" style={{ position:'absolute', right:0, top:'calc(100% + 4px)', background:T.surface, border:`1px solid ${T.border}`, borderRadius:8, boxShadow:T.shadowMd, zIndex:20, overflow:'hidden', minWidth:110 }}>
                    {sortOpts.map(o=>(
                      <div key={o} onClick={()=>{ changeSortBy(o); setSortOpen(false); }}
                           style={{ padding:'9px 14px', fontSize:13, color:sortBy===o?T.primary:T.text, fontWeight:sortBy===o?600:400, cursor:'pointer', background:sortBy===o?T.primaryBg:'transparent' }}>
                        {o}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* 카드 그리드 */}
          {paged.length===0 ? (
            <div style={{ display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', padding:'80px 0' }}>
              <Icon n="filter" size={40} color={T.border}/>
              <div style={{ marginTop:16, fontSize:15, color:T.textSec }}>조건에 맞는 프로그램이 없습니다.</div>
              <div onClick={resetAll} className="btn-hover"
                   style={{ marginTop:12, fontSize:13, color:T.primary, cursor:'pointer', textDecoration:'underline' }}>필터 초기화</div>
            </div>
          ) : (
            <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:16 }}>
              {paged.map(pg=>(
                <div key={pg.id} onClick={()=>go('program-detail',{pg})}
                     style={{ borderRadius:T.radius, overflow:'hidden', background:T.surface, boxShadow:T.shadow, border:`1px solid ${T.borderLight}`, opacity:pg.status==='마감'?0.72:1, cursor:'pointer' }}>
                  <div style={{ width:'100%', height:175, position:'relative', overflow:'hidden', background:'#e5e7eb' }}>
                    <img src={IMGS.pg[(pg.id-1)%6]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
                    <div style={{ position:'absolute', top:10, left:10 }}>
                      <Badge text={pg.status} variant={pg.status==='마감'?'muted':'primary'}/>
                    </div>
                    <div onClick={e=>{e.stopPropagation(); const n=new Set(fav); n.has(pg.id)?n.delete(pg.id):n.add(pg.id); setFav(n);}}
                         style={{ position:'absolute', top:10, right:10, cursor:'pointer' }}>
                      <Icon n={fav.has(pg.id)?'starFill':'star'} size={20} color={fav.has(pg.id)?'#F59E0B':'rgba(255,255,255,0.85)'}/>
                    </div>
                  </div>
                  <div style={{ padding:'12px 14px 14px' }}>
                    <div style={{ fontWeight:600, fontSize:15, color:T.text, marginBottom:8, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{pg.title}</div>
                    <div style={{ fontSize:12, color:T.textSec, marginBottom:6 }}>{pg.center}</div>
                    <div style={{ fontSize:12, color:T.textTri, marginBottom:12 }}>{pg.date.replace('~',' ~ ')}</div>
                    {(() => {
                      const isClosed = pg.status === '마감';
                      const isActive = clickedId === pg.id || hoveredId === pg.id;
                      const btnColor = isClosed ? T.textTri : (isActive ? '#fff' : T.primary);
                      return (
                        <div
                             onMouseEnter={()=>{ if(!isClosed) setHoveredId(pg.id); }}
                             onMouseLeave={()=>setHoveredId(null)}
                             onClick={e => {
                               e.stopPropagation();
                               if (isClosed) return;
                               setClickedId(pg.id);
                               setTimeout(() => { setClickedId(null); go('program-detail', {pg}); }, 180);
                             }}
                             style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:5, height:34,
                                      border:isClosed?'none':(isActive?'none':`1px solid ${T.primary}`),
                                      borderRadius:T.tagR, fontSize:13, fontWeight:600,
                                      color:btnColor,
                                      background:isClosed?T.borderLight:(isActive?T.primary:'transparent'),
                                      cursor:isClosed?'not-allowed':'pointer',
                                      pointerEvents:isClosed?'none':'auto',
                                      transition:'background 150ms ease, color 150ms ease, border 150ms ease' }}>
                          <Icon n="check" size={15} color={btnColor}/>
                          {isClosed?'신청마감':'신청하기'}
                        </div>
                      );
                    })()}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* 페이지네이션 */}
          {totalPages>1 && (
            <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:6, marginTop:32 }}>
              <div onClick={()=>setPage(p=>Math.max(1,p-1))} className="btn-hover"
                   style={{ width:34, height:34, borderRadius:8, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:page>1?'pointer':'default', opacity:page>1?1:0.35 }}>
                <Icon n="arrowL" size={16} color={T.textSec}/>
              </div>
              {Array.from({length:totalPages},(_,i)=>i+1).map(n=>(
                <div key={n} onClick={()=>setPage(n)} className="btn-hover"
                     style={{ width:34, height:34, borderRadius:8, background:page===n?T.primary:'transparent', border:`1px solid ${page===n?T.primary:T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer', fontSize:13, fontWeight:page===n?600:400, color:page===n?'#fff':T.textSec }}>
                  {n}
                </div>
              ))}
              <div onClick={()=>setPage(p=>Math.min(totalPages,p+1))} className="btn-hover"
                   style={{ width:34, height:34, borderRadius:8, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:page<totalPages?'pointer':'default', opacity:page<totalPages?1:0.35, transform:'scaleX(-1)' }}>
                <Icon n="arrowL" size={16} color={T.textSec}/>
              </div>
            </div>
          )}
        </div>
      </div>
      <Footer/>
    </div>
  );
}

// ── PROGRAM DETAIL ──
function ProgramDetail({ go, pg, isLoggedIn, onLoginClick, addToast }) {
  const [fav, setFav] = useState(false);
  return (
    <div className="screen-enter" style={{ background:T.bg, minHeight:'100vh' }}>
      <div style={{ maxWidth:1000, margin:'0 auto', padding:'36px 0 48px' }}>
        <div className="btn-hover" onClick={()=>go('programs')} style={{ display:'inline-flex', alignItems:'center', gap:6, marginBottom:24, cursor:'pointer', color:T.textSec, fontSize:14 }}>
          <Icon n="arrowL" size={18} color={T.textSec}/> 목록으로
        </div>
        <div style={{ display:'flex', gap:32 }}>
          <div style={{ width:340, height:340, borderRadius:T.radius, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}>
            <img src={IMGS.pg[0]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
          </div>
          <div style={{ flex:1 }}>
            <div style={{ display:'flex', gap:8, marginBottom:8 }}>
              <Badge text={pg.status} variant={pg.status==='마감'?'muted':'primary'}/>
              <span style={{ fontSize:13, color:T.textTri }}>{pg.cat}</span>
            </div>
            <h1 style={{ fontSize:26, fontWeight:700, color:T.text, margin:'0 0 4px' }}>{pg.title}</h1>
            <p style={{ fontSize:14, color:T.textSec, margin:'0 0 20px' }}>{pg.center} · {pg.region}</p>
            <div style={{ borderTop:`1px solid ${T.border}` }}>
              {[['신청 기간','2024-07-01 ~ 07-31'],['진행 기간',pg.date],['진행 장소','강의실 A (경기도)'],['모집인원',pg.cap+'명'],['문의처','031-123-4567'],['첨부파일','첨부파일이 없습니다.']].map(([k,v])=>(
                <div key={k} style={{ display:'flex', padding:'11px 0', borderBottom:`1px solid ${T.borderLight}` }}>
                  <span style={{ width:90, fontSize:13, fontWeight:500, color:T.textSec, flexShrink:0 }}>{k}</span>
                  <span style={{ fontSize:13, color:T.text }}>{v}</span>
                </div>
              ))}
            </div>
            <div style={{ display:'flex', gap:10, marginTop:20 }}>
              <div className="btn-hover" onClick={()=>{setFav(v=>!v); addToast(fav?'즐겨찾기 해제되었습니다.':'즐겨찾기에 추가되었습니다.');}} style={{ width:50, height:50, borderRadius:8, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
                <Icon n={fav?'starFill':'star'} size={20} color={fav?'#F59E0B':T.textSec}/>
              </div>
              <div className="btn-hover" onClick={()=>addToast('URL이 클립보드에 복사되었습니다.')} style={{ width:50, height:50, borderRadius:8, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer' }}>
                <Icon n="share" size={18} color={T.textSec}/>
              </div>
              <div className="btn-hover" onClick={()=>{if(!isLoggedIn){onLoginClick();return;} if(pg.status==='마감')return; go('program-apply',{pg});}} style={{ flex:1, height:50, background:pg.status==='마감'?T.border:T.primary, borderRadius:T.tagR, display:'flex', alignItems:'center', justifyContent:'center', gap:6, cursor:pg.status==='마감'?'not-allowed':'pointer' }}>
                <Icon n="check" size={18} color="#fff"/>
                <span style={{ color:'#fff', fontSize:16, fontWeight:600 }}>{pg.status==='마감'?'신청마감':'신청하기'}</span>
              </div>
            </div>
          </div>
        </div>
        <div style={{ height:1, background:T.border, margin:'32px 0' }}/>
        <h3 style={{ fontSize:20, fontWeight:600, color:T.text, marginBottom:16 }}>프로그램 설명</h3>
        <div style={{ width:'100%', height:360, borderRadius:T.radius, background:'#E5E7EB', display:'flex', alignItems:'center', justifyContent:'center', overflow:'hidden', position:'relative' }}>
          <img src={IMGS.pg[1]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover', opacity:0.3 }}/>
          <span style={{ position:'absolute', fontSize:14, color:'#888', fontWeight:500 }}>프로그램 상세 설명 콘텐츠 영역</span>
        </div>
      </div>
      <Footer/>
    </div>
  );
}

// ── PROGRAM APPLY ──
function ProgramApply({ go, pg, addToast }) {
  const [agreed, setAgreed] = useState(false);
  const [reason, setReason] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const handleSubmit = () => {
    if(!agreed) { alert('개인정보 수집 동의가 필요합니다.'); return; }
    go('apply-complete', {pg});
  };
  return (
    <div className="screen-enter" style={{ background:T.bg, minHeight:'100vh' }}>
      <div style={{ maxWidth:700, margin:'0 auto', padding:'36px 0 48px' }}>
        <div className="btn-hover" onClick={()=>go('program-detail',{pg})} style={{ display:'inline-flex', alignItems:'center', gap:6, marginBottom:20, cursor:'pointer', color:T.textSec, fontSize:14 }}>
          <Icon n="arrowL" size={18} color={T.textSec}/> 이전으로
        </div>
        <h2 style={{ fontSize:26, fontWeight:700, color:T.text, textAlign:'center', marginBottom:28 }}>프로그램 신청</h2>
        {/* Summary */}
        <div style={{ display:'flex', gap:16, padding:18, borderRadius:T.radius, background:T.surface, border:`1px solid ${T.border}`, marginBottom:24 }}>
          <div style={{ width:80, height:80, borderRadius:10, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}><img src={IMGS.pg[0]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/></div>
          <div><Badge text="진행중"/><div style={{ fontWeight:600, fontSize:16, color:T.text, margin:'6px 0 2px' }}>{pg.title}</div><div style={{ fontSize:13, color:T.textSec }}>{pg.center} · {pg.date}</div></div>
        </div>
        {/* Section: Applicant */}
        <div style={{ fontWeight:700, fontSize:16, color:T.text, paddingBottom:10, borderBottom:`2px solid ${T.text}`, marginBottom:16 }}>신청자 정보</div>
        <div style={{ display:'flex', flexDirection:'column', gap:14, marginBottom:24 }}>
          {[{label:'이름',val:'박시현'},{label:'핸드폰 번호',val:'010-1234-5678'},{label:'이메일',val:'hyuuun0321@naver.com',note:'이메일을 통해 프로그램 관련 알림을 드립니다.'},{label:'주소',val:'경기도 수원시 팔달구 효원로 1'}].map(f=>(
            <div key={f.label}>
              <span style={{ fontSize:13, fontWeight:500, color:T.textSec, marginBottom:6, display:'block' }}>{f.label}</span>
              <div style={{ height:46, borderRadius:8, border:`1px solid ${T.border}`, padding:'0 14px', display:'flex', alignItems:'center', background:'#F3F4F6', fontSize:14, color:T.textSec }}>{f.val}</div>
              {f.note && <div style={{ fontSize:12, color:T.textTri, marginTop:4 }}>{f.note}</div>}
            </div>
          ))}
        </div>
        {/* Section: Extra */}
        <div style={{ fontWeight:700, fontSize:16, color:T.text, paddingBottom:10, borderBottom:`2px solid ${T.text}`, marginBottom:16 }}>추가 정보</div>
        <div style={{ marginBottom:24 }}>
          <span style={{ fontSize:13, fontWeight:500, color:T.textSec, marginBottom:6, display:'block' }}>지원 동기</span>
          <textarea value={reason} onChange={e=>setReason(e.target.value)} placeholder="지원 동기를 입력해주세요." style={{ width:'100%', height:88, borderRadius:8, border:`1.5px solid ${reason?T.primary:T.border}`, padding:'12px 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, resize:'none', outline:'none', transition:'border 150ms' }}/>
        </div>
        {/* Privacy */}
        <div style={{ fontWeight:700, fontSize:16, color:T.text, paddingBottom:10, borderBottom:`2px solid ${T.text}`, marginBottom:16 }}>개인정보 수집 동의</div>
        <div style={{ padding:14, borderRadius:8, border:`1px solid ${T.border}`, background:T.surface, fontSize:13, color:T.textSec, lineHeight:1.7, maxHeight:130, overflow:'hidden', marginBottom:12 }}>
          개인정보수집 및 초상 이용<br/><br/>1. 개인정보 수집 항목: 성명, 생년월일, 성별, 연락처<br/>2. 이용목적: 강좌신청 예약 접수 및 홍보 목적
        </div>
        <label onClick={()=>setAgreed(v=>!v)} style={{ display:'flex', alignItems:'center', gap:8, marginBottom:28, cursor:'pointer', userSelect:'none' }}>
          <div style={{ width:20, height:20, borderRadius:'50%', border:`2px solid ${agreed?T.primary:T.border}`, display:'flex', alignItems:'center', justifyContent:'center', transition:'all 150ms' }}>
            {agreed && <div style={{ width:10, height:10, borderRadius:'50%', background:T.primary }}/>}
          </div>
          <span style={{ fontSize:14, color:T.text }}>동의합니다</span>
        </label>
        <div style={{ display:'flex', justifyContent:'center' }}>
          <Btn size="l" variant="primary" onClick={handleSubmit} style={{ width:220 }}>신청하기</Btn>
        </div>
      </div>
      <Footer/>
    </div>
  );
}

// ── APPLY COMPLETE ──
function ApplyComplete({ go, pg }) {
  return (
    <div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
      <div style={{ flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', padding:'56px 80px' }}>
        <div style={{ width:88, height:88, borderRadius:'50%', background:'#D1FAE5', display:'flex', alignItems:'center', justifyContent:'center', marginBottom:24 }}>
          <svg viewBox="0 0 24 24" style={{ width:46, height:46 }}>
            <circle cx="12" cy="12" r="10" fill="#10B981"/>
            <path d="M8 12.5l2.5 2.5L16 9.5" stroke="#fff" strokeWidth="2.2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </div>
        <h2 style={{ fontSize:26, fontWeight:700, color:T.text, marginBottom:8 }}>프로그램 신청이 완료되었습니다</h2>
        <p style={{ fontSize:15, color:T.textSec, textAlign:'center', lineHeight:1.7, marginBottom:28, wordBreak:'keep-all' }}>
          신청하신 내용은 <strong style={{color:T.text}}>승인 대기</strong> 상태입니다.<br/>
          결과는 이메일과 마이페이지 &gt; 프로그램 신청 현황에서 확인하세요.
        </p>
        <div style={{ width:520, borderRadius:T.radius, background:T.surface, border:`1px solid ${T.border}`, padding:20, display:'flex', gap:16, marginBottom:28 }}>
          <div style={{ width:80, height:80, borderRadius:8, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}><img src={IMGS.pg[0]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/></div>
          <div style={{ flex:1 }}>
            <span style={{ padding:'2px 10px', borderRadius:T.tagR, background:T.warningLight, color:T.warning, fontSize:12, fontWeight:600 }}>승인 대기</span>
            <div style={{ fontWeight:600, fontSize:16, color:T.text, margin:'6px 0 2px' }}>{pg.title}</div>
            <div style={{ fontSize:13, color:T.textSec }}>{pg.center} · {pg.date}</div>
            <div style={{ fontSize:12, color:T.textTri, marginTop:4 }}>신청일시 2024-07-05 17:11</div>
          </div>
        </div>
        <div style={{ display:'flex', gap:12 }}>
          <Btn size="l" variant="ghost" onClick={()=>go('home')} style={{width:160}}>홈으로</Btn>
          <Btn size="l" variant="primary" onClick={()=>go('mypage')} style={{width:200}}>신청 현황 보기</Btn>
        </div>
      </div>
      <Footer/>
    </div>
  );
}

// ── MYPAGE ──
function MyPage({ go, addToast }) {
  const [tab, setTab] = useState('history');
  const [showCancel, setShowCancel] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelEtcDetail, setCancelEtcDetail] = useState('');
  const [cancelError, setCancelError] = useState(false);
  const [applications, setApplications] = useState([
    { ...PROGRAMS[0], appId:1, appStatus:'승인 대기', appliedAt:'2024.07.05 17:11:31' },
    { ...PROGRAMS[1], appId:2, appStatus:'승인',      appliedAt:'2024.07.03 17:11:31', approvedAt:'2024-07-06 09:05:31', note:'신청 내용이 확인되어 승인 처리되었습니다.' },
    { ...PROGRAMS[2], appId:3, appStatus:'반려',      appliedAt:'2024.07.02 17:11:31', processedAt:'2024-07-04 10:00:00', note:'모집 인원이 초과되어 반려 처리되었습니다.' },
    { ...PROGRAMS[3], appId:4, appStatus:'취소',      appliedAt:'2024.06.28 10:00:00', processedAt:'2024-06-29 15:20:00', note:'단순 변심으로 신청 취소되었습니다.' },
  ]);
  const [cancelTargetId, setCancelTargetId] = useState(null);
  const [histFilter, setHistFilter] = useState('3개월');
  const [selectedApp, setSelectedApp] = useState(null);
  const [favItems] = useState([PROGRAMS[0], PROGRAMS[2], PROGRAMS[4], PROGRAMS[5]]);
  const [favSet, setFavSet] = useState(new Set([0,2,4,5]));
  // profile: step 1 = 비밀번호 재확인, step 2 = 수정 폼
  const [profileStep, setProfileStep] = useState(1);
  const [pwConfirm, setPwConfirm] = useState('');
  const [profileForm, setProfileForm] = useState({ newPw:'', newPwConfirm:'', address:'경기도 수원시 팔달구 효원로 1', addressDetail:'', gender:'여', birth:'1995-03-21' });
  const [showWithdraw, setShowWithdraw] = useState(false);
  const setProfile = (k,v) => setProfileForm(f=>({...f,[k]:v}));
  // 신청 취소: step1=사유선택, step2=최종확인
  const [cancelStep, setCancelStep] = useState(1);
  const doCancel = () => {
    if(!cancelReason){setCancelError(true);return;}
    if(cancelStep===1){ setCancelStep(2); return; }
    setApplications(apps=>apps.filter(a=>a.appId!==cancelTargetId));
    setSelectedApp(null);
    setShowCancel(false); setCancelStep(1); setCancelReason(''); setCancelEtcDetail(''); setCancelError(false); setCancelTargetId(null);
    addToast('취소되었습니다.');
  };
  const statusStyle = { '승인 대기':{bg:T.warningLight,c:T.warning}, '승인':{bg:T.successLight,c:T.success}, '취소':{bg:T.borderLight,c:T.textTri} };
  return (
    <div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
      <div style={{ display:'flex', gap:40, padding:'36px 80px', flex:1, alignItems:'flex-start' }}>
        {/* Sidebar */}
        <div style={{ width:220, flexShrink:0 }}>
          <div style={{ fontSize:18, fontWeight:700, color:T.text, marginBottom:16 }}>반가워요! 박시현님</div>
          <div style={{ display:'flex', border:`1px solid ${T.border}`, borderRadius:T.radius, overflow:'hidden', marginBottom:20 }}>
            {[{label:'진행중인 프로그램',val:'1'},{label:'종료된 프로그램',val:'1'}].map((s,i)=>(
              <div key={s.label} style={{ flex:1, textAlign:'center', padding:'12px 8px', borderLeft:i>0?`1px solid ${T.border}`:'none' }}>
                <div style={{ fontSize:12, color:T.textSec, marginBottom:2 }}>{s.label}</div>
                <div style={{ fontSize:22, fontWeight:700, color:T.text }}>{s.val}</div>
              </div>
            ))}
          </div>
          {[{key:'history',icon:'calendar',label:'프로그램 신청 현황'},{key:'favorites',icon:'star',label:'즐겨찾기한 프로그램'},{key:'profile',icon:'user',label:'개인 정보 수정'}].map(m=>(
            <div key={m.key} onClick={()=>{setTab(m.key); if(m.key==='profile') setProfileStep(1);}} className="btn-hover" style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 12px', borderRadius:8, background:tab===m.key?T.primaryLight:'transparent', cursor:'pointer', marginBottom:2 }}>
              <Icon n={m.icon} size={18} color={tab===m.key?T.primary:T.textSec}/>
              <span style={{ fontSize:14, fontWeight:tab===m.key?600:400, color:tab===m.key?T.primary:T.textSec }}>{m.label}</span>
            </div>
          ))}
        </div>
        {/* Content */}
        <div style={{ flex:1 }}>
          {tab==='history' && (() => {
            const filterMonths = { '3개월':3, '6개월':6, '1년':12 };
            const cutoff = new Date('2024-08-01');
            cutoff.setMonth(cutoff.getMonth() - filterMonths[histFilter]);
            const filtered = applications.filter(app => {
              const d = new Date(app.appliedAt.replace(/\./g,'-').slice(0,10));
              return d >= cutoff;
            });
            // 날짜별 그룹핑
            const groups = filtered.reduce((acc, app) => {
              const key = app.appliedAt;
              if (!acc[key]) acc[key] = [];
              acc[key].push(app);
              return acc;
            }, {});

            const stStyle = {
              '승인 대기': { bg:T.warningLight, c:T.warning },
              '승인':      { bg:T.successLight, c:T.success },
              '반려':      { bg:'#FEE2E2',      c:T.error   },
              '취소':      { bg:T.borderLight,  c:T.textSec },
            };

            return (
              <div>
                <h3 style={{ fontSize:20, fontWeight:600, color:T.text, marginBottom:6 }}>프로그램 신청 내역</h3>
                <div style={{ fontSize:13, color:T.textSec, marginBottom:12 }}>최대 지난 3년간의 프로그램 신청 내역까지 확인할 수 있어요</div>
                <div style={{ height:1, background:T.text, marginBottom:16 }}/>

                {/* 기간 필터 탭 */}
                <div style={{ display:'flex', gap:8, marginBottom:20 }}>
                  {['3개월','6개월','1년','3년'].map(f=>(
                    <div key={f} onClick={()=>{setHistFilter(f);setSelectedApp(null);}} className="btn-hover"
                         style={{ padding:'6px 18px', borderRadius:T.tagR, fontSize:13, fontWeight:600, cursor:'pointer',
                                   background:histFilter===f?T.primary:'transparent',
                                   color:histFilter===f?'#fff':T.textSec,
                                   border:`1px solid ${histFilter===f?T.primary:T.border}`,
                                   transition:'all 150ms' }}>
                      {f}
                    </div>
                  ))}
                </div>

                {filtered.length===0 ? (
                  <div style={{ display:'flex', flexDirection:'column', alignItems:'center', padding:'80px 0', gap:14 }}>
                    <Icon n="calendar" size={64} color={T.border}/>
                    <span style={{ fontSize:15, color:T.textSec }}>신청한 프로그램이 없습니다.</span>
                    <Btn size="m" onClick={()=>go('programs')}>프로그램 보기</Btn>
                  </div>
                ) : (
                  <div style={{ display:'flex', gap:20, alignItems:'flex-start' }}>
                    {/* 목록 */}
                    <div style={{ flex:1, minWidth:0 }}>
                      {Object.entries(groups).map(([date, apps])=>(
                        <div key={date} style={{ marginBottom:20 }}>
                          {/* 날짜 그룹 헤더 */}
                          <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:10 }}>
                            <span style={{ fontSize:13, color:T.textSec, fontWeight:500 }}>{date}</span>
                            <span onClick={()=>setSelectedApp(apps[0])}
                                  style={{ fontSize:12, color:T.primary, cursor:'pointer', display:'flex', alignItems:'center', gap:2 }}
                                  className="btn-hover">
                              신청 상세 <Icon n="chevR" size={13} color={T.primary}/>
                            </span>
                          </div>
                          {apps.map((app,i)=>{
                            const st = stStyle[app.appStatus]||stStyle['취소'];
                            const isSelected = selectedApp?.appId===app.appId;
                            const hasCancel = app.appStatus==='승인 대기' || app.appStatus==='승인';
                            const hasReapply = app.appStatus==='반려';
                            return (
                              <div key={app.appId}
                                   style={{ borderRadius:T.radius, overflow:'hidden',
                                            border:`1px solid ${isSelected?T.primary:T.borderLight}`,
                                            background:T.surface,
                                            marginBottom:10, transition:'border 150ms' }}>
                                {/* 카드 메인 */}
                                <div style={{ display:'flex', alignItems:'flex-start', gap:14, padding:'14px 14px 12px' }}>
                                  <div style={{ width:76, height:76, borderRadius:8, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}>
                                    <img src={IMGS.pg[i%6]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
                                  </div>
                                  <div style={{ flex:1, minWidth:0 }}>
                                    <div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:5 }}>
                                      <span style={{ fontWeight:600, fontSize:14, color:T.text, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{app.title}</span>
                                      <span style={{ flexShrink:0, padding:'2px 9px', borderRadius:T.tagR, background:st.bg, color:st.c, fontSize:11, fontWeight:600 }}>{app.appStatus}</span>
                                    </div>
                                    <div style={{ fontSize:12, color:T.textSec, marginBottom:3 }}>{app.date.replace('~',' ~ ')}</div>
                                    <div style={{ fontSize:11, color:T.textTri }}>신청 일시 {app.appliedAt}</div>
                                  </div>
                                </div>
                                {/* 액션 버튼 */}
                                {(hasCancel || hasReapply) && (
                                  <div style={{ padding:'0 14px 12px', display:'flex', gap:8 }}>
                                    {hasReapply && (
                                      <div className="btn-hover"
                                           style={{ flex:1, display:'flex', alignItems:'center', justifyContent:'center', gap:5,
                                                     height:36, border:`1px solid ${T.border}`, borderRadius:8,
                                                     background:T.surface, cursor:'pointer' }}>
                                        <span style={{ fontSize:13, color:T.textSec }}>⊙</span>
                                        <span style={{ fontSize:13, color:T.textSec, fontWeight:500 }}>재신청</span>
                                      </div>
                                    )}
                                    <div onClick={e=>{e.stopPropagation();setCancelTargetId(app.appId);setCancelStep(1);setShowCancel(true);}}
                                         className="btn-hover"
                                         style={{ flex:1, display:'flex', alignItems:'center', justifyContent:'center', gap:5,
                                                   height:36, border:`1px solid ${T.error}`, borderRadius:8,
                                                   background:T.surface, cursor:'pointer' }}>
                                      <span style={{ fontSize:13, color:T.error }}>⊗</span>
                                      <span style={{ fontSize:13, color:T.error, fontWeight:500 }}>신청 취소</span>
                                    </div>
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      ))}
                    </div>

                    {/* 상세 패널 */}
                    {selectedApp && (
                      <div style={{ width:300, flexShrink:0, borderRadius:T.radius, background:T.surface, border:`1px solid ${T.borderLight}`, padding:20, position:'sticky', top:20 }}>
                        {/* 프로그램 요약 */}
                        <div style={{ display:'flex', gap:12, marginBottom:16, paddingBottom:16, borderBottom:`1px solid ${T.borderLight}` }}>
                          <div style={{ width:56, height:56, borderRadius:8, overflow:'hidden', background:'#e5e7eb', flexShrink:0 }}>
                            <img src={IMGS.pg[applications.findIndex(a=>a.appId===selectedApp.appId)%6]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
                          </div>
                          <div style={{ flex:1, minWidth:0 }}>
                            {(() => { const st=stStyle[selectedApp.appStatus]||stStyle['취소']; return <span style={{ display:'inline-block', padding:'2px 8px', borderRadius:T.tagR, background:st.bg, color:st.c, fontSize:11, fontWeight:600, marginBottom:4 }}>{selectedApp.appStatus}</span>; })()}
                            <div style={{ fontSize:13, fontWeight:600, color:T.text, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{selectedApp.title}</div>
                            <div style={{ fontSize:12, color:T.textSec, marginTop:2 }}>{selectedApp.date}</div>
                          </div>
                        </div>

                        {/* 신청자 정보 */}
                        <div style={{ fontSize:13, fontWeight:600, color:T.text, marginBottom:10 }}>신청자 정보</div>
                        {[['이름','박시현'],['핸드폰 번호','010-1234-5678'],['성별','여'],['생년월일','1998-03-21'],['주소','경기도 수원시 팔달구 효원로 1']].map(([k,v])=>(
                          <div key={k} style={{ display:'flex', gap:8, marginBottom:7 }}>
                            <span style={{ fontSize:12, color:T.textSec, width:72, flexShrink:0 }}>{k}</span>
                            <span style={{ fontSize:12, color:T.text, fontWeight:500 }}>{v}</span>
                          </div>
                        ))}

                        {/* 신청 이력 */}
                        <div style={{ fontSize:13, fontWeight:600, color:T.text, margin:'14px 0 10px', paddingTop:14, borderTop:`1px solid ${T.borderLight}` }}>신청 이력</div>
                        {[
                          ['신청일시', selectedApp.appliedAt],
                          selectedApp.approvedAt ? ['승인일시', selectedApp.approvedAt] : null,
                          selectedApp.processedAt ? ['처리일시', selectedApp.processedAt] : null,
                          selectedApp.note ? ['처리 내용', selectedApp.note] : null,
                        ].filter(Boolean).map(([k,v])=>(
                          <div key={k} style={{ display:'flex', gap:8, marginBottom:7, alignItems:'flex-start' }}>
                            <span style={{ fontSize:12, color:T.textSec, width:72, flexShrink:0 }}>{k}</span>
                            <span style={{ fontSize:12, color:T.text, fontWeight:500, lineHeight:1.5 }}>{v}</span>
                          </div>
                        ))}

                        {selectedApp.appStatus==='승인 대기' && (
                          <Btn size="s" variant="danger" fullWidth style={{ marginTop:16 }}
                               onClick={()=>{setCancelTargetId(selectedApp.appId);setCancelStep(1);setShowCancel(true);}}>
                            프로그램 신청 취소
                          </Btn>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })()}
          {tab==='favorites' && (
            <div>
              <h3 style={{ fontSize:20, fontWeight:600, color:T.text, marginBottom:6 }}>즐겨찾기한 프로그램</h3>
              <div style={{ height:1, background:T.text, marginBottom:20 }}/>
              {favItems.length===0
                ? <div style={{ display:'flex', flexDirection:'column', alignItems:'center', padding:'80px 0', gap:14 }}>
                    <Icon n="star" size={64} color={T.border}/>
                    <span style={{ fontSize:15, color:T.textSec }}>즐겨찾기한 프로그램이 없습니다.</span>
                    <Btn size="m" onClick={()=>go('programs')}>프로그램 보기</Btn>
                  </div>
                : <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:14 }}>
                    {favItems.map((pg,i)=>(
                      <div key={pg.id} onClick={()=>go('program-detail',{pg})} style={{ borderRadius:T.radius, overflow:'hidden', background:T.surface, boxShadow:T.shadow, border:`1px solid ${T.borderLight}`, cursor:'pointer', width:'100%', minWidth:0 }}>
                        <div style={{ width:'100%', height:140, position:'relative', overflow:'hidden', background:'#e5e7eb' }}>
                          <img src={IMGS.pg[i%6]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
                          <div style={{ position:'absolute', top:8, left:8 }}><Badge text={pg.status} variant={pg.status==='마감'?'muted':'primary'}/></div>
                          <div onClick={e=>{ e.stopPropagation(); const n=new Set(favSet); n.has(i)?n.delete(i):n.add(i); setFavSet(n); addToast(favSet.has(i)?'즐겨찾기 해제되었습니다.':'즐겨찾기에 추가되었습니다.'); }}
                               style={{ position:'absolute', top:8, right:8, cursor:'pointer' }}>
                            <Icon n="starFill" size={18} color="#F59E0B"/>
                          </div>
                        </div>
                        <div style={{ padding:'10px 12px 12px' }}>
                          <div style={{ fontWeight:600, fontSize:14, color:T.text, marginBottom:6, whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis' }}>{pg.title}</div>
                          <div style={{ fontSize:12, color:T.textSec, marginBottom:4 }}>{pg.center}</div>
                          <div style={{ fontSize:12, color:T.textTri, marginBottom:10 }}>{pg.date.replace('~',' ~ ')}</div>
                          <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:4, height:32, border:`1px solid ${T.primary}`, borderRadius:T.tagR, fontSize:12, fontWeight:600, color:T.primary }}>
                            <Icon n="check" size={13} color={T.primary}/>신청하기
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>}
            </div>
          )}
          {tab==='profile' && (
            <div>
              <h3 style={{ fontSize:20, fontWeight:600, color:T.text, marginBottom:6 }}>개인 정보 수정</h3>
              <div style={{ height:1, background:T.text, marginBottom:20 }}/>
              {profileStep===1 ? (
                /* Step 1: 비밀번호 재확인 */
                <div style={{ maxWidth:480 }}>
                  <div style={{ fontSize:15, fontWeight:600, color:T.text, marginBottom:6 }}>비밀번호 재확인</div>
                  <div style={{ fontSize:13, color:T.textSec, marginBottom:20, lineHeight:1.6 }}>회원님의 정보를 안전하게 보호하기 위해 비밀번호를 다시 한번 확인해주세요.</div>
                  <div style={{ height:1, background:T.border, marginBottom:20 }}/>
                  <div style={{ display:'grid', gridTemplateColumns:'120px 1fr', gap:'12px 16px', alignItems:'center', marginBottom:28 }}>
                    <span style={{ fontSize:13, fontWeight:500, color:T.textSec }}>아이디</span>
                    <div style={{ height:46, borderRadius:8, border:`1px solid ${T.border}`, padding:'0 14px', display:'flex', alignItems:'center', background:'#F3F4F6', fontSize:14, color:T.textSec }}>hyuuun0321</div>
                    <span style={{ fontSize:13, fontWeight:500, color:T.textSec }}>비밀번호 <span style={{color:T.error}}>*</span></span>
                    <input type="password" value={pwConfirm} onChange={e=>setPwConfirm(e.target.value)} placeholder="현재 비밀번호를 입력해주세요." style={{ height:46, borderRadius:8, border:`1.5px solid ${pwConfirm?T.primary:T.border}`, padding:'0 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none' }}/>
                  </div>
                  <div style={{ display:'flex', justifyContent:'center' }}>
                    <Btn size="m" onClick={()=>{ if(!pwConfirm){alert('비밀번호를 입력해주세요.');return;} setProfileStep(2); }}>확인</Btn>
                  </div>
                </div>
              ) : (
                /* Step 2: 수정 폼 */
                <div style={{ maxWidth:560 }}>
                  <div style={{ display:'grid', gridTemplateColumns:'120px 1fr', gap:'14px 16px', alignItems:'start', marginBottom:24 }}>
                    {/* 아이디 */}
                    <span style={{ fontSize:13, fontWeight:500, color:T.textSec, paddingTop:13 }}>아이디 <span style={{color:T.error}}>*</span></span>
                    <div style={{ height:46, borderRadius:8, border:`1px solid ${T.border}`, padding:'0 14px', display:'flex', alignItems:'center', background:'#F3F4F6', fontSize:14, color:T.textSec }}>hyuuun0321</div>
                    {/* 새 비밀번호 */}
                    <span style={{ fontSize:13, fontWeight:500, color:T.textSec, paddingTop:13 }}>새 비밀번호 <span style={{color:T.error}}>*</span></span>
                    <input type="password" value={profileForm.newPw} onChange={e=>setProfile('newPw',e.target.value)} placeholder="새 비밀번호를 입력해주세요." style={{ height:46, borderRadius:8, border:`1.5px solid ${profileForm.newPw?T.primary:T.border}`, padding:'0 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none' }}/>
                    {/* 새 비밀번호 확인 */}
                    <span style={{ fontSize:13, fontWeight:500, color:T.textSec, paddingTop:13 }}>새 비밀번호 확인 <span style={{color:T.error}}>*</span></span>
                    <div>
                      <input type="password" value={profileForm.newPwConfirm} onChange={e=>setProfile('newPwConfirm',e.target.value)} placeholder="새 비밀번호를 다시 입력해주세요." style={{ width:'100%', height:46, borderRadius:8, border:`1.5px solid ${profileForm.newPwConfirm?(profileForm.newPwConfirm===profileForm.newPw?T.success:T.error):T.border}`, padding:'0 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', boxSizing:'border-box' }}/>
                      {profileForm.newPwConfirm && profileForm.newPwConfirm!==profileForm.newPw && <div style={{ fontSize:12, color:T.error, marginTop:4 }}>비밀번호가 일치하지 않습니다.</div>}
                    </div>
                    {/* 이름 */}
                    <span style={{ fontSize:13, fontWeight:500, color:T.textSec, paddingTop:13 }}>이름 <span style={{color:T.error}}>*</span></span>
                    <div style={{ height:46, borderRadius:8, border:`1px solid ${T.border}`, padding:'0 14px', display:'flex', alignItems:'center', background:'#F3F4F6', fontSize:14, color:T.textSec }}>박시현</div>
                    {/* 핸드폰 */}
                    <span style={{ fontSize:13, fontWeight:500, color:T.textSec, paddingTop:13 }}>핸드폰 번호 <span style={{color:T.error}}>*</span></span>
                    <div style={{ height:46, borderRadius:8, border:`1px solid ${T.border}`, padding:'0 14px', display:'flex', alignItems:'center', background:'#F3F4F6', fontSize:14, color:T.textSec }}>010-1234-5678</div>
                    {/* 주소 */}
                    <span style={{ fontSize:13, fontWeight:500, color:T.textSec, paddingTop:13 }}>주소 <span style={{color:T.error}}>*</span></span>
                    <div style={{ display:'flex', flexDirection:'column', gap:6 }}>
                      <div style={{ display:'flex', gap:8 }}>
                        <div style={{ flex:1, height:46, borderRadius:8, border:`1px solid ${T.border}`, padding:'0 14px', display:'flex', alignItems:'center', background:'#F3F4F6', fontSize:14, color:T.textSec }}>{profileForm.address}</div>
                        <button onClick={()=>addToast('주소 검색은 Kakao API 연동 후 지원됩니다.')}
                          style={{ height:46, padding:'0 16px', borderRadius:8, border:`1.5px solid ${T.primary}`, background:'transparent', fontSize:13, fontWeight:600, color:T.primary, cursor:'pointer', flexShrink:0, fontFamily:'Pretendard' }}>재검색</button>
                      </div>
                      <input value={profileForm.addressDetail} onChange={e=>setProfile('addressDetail',e.target.value)} placeholder="상세주소를 입력해주세요." style={{ height:46, borderRadius:8, border:`1.5px solid ${profileForm.addressDetail?T.primary:T.border}`, padding:'0 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none' }}/>
                    </div>
                    {/* 성별 */}
                    <span style={{ fontSize:13, fontWeight:500, color:T.textSec, paddingTop:13 }}>성별 <span style={{color:T.error}}>*</span></span>
                    <div style={{ display:'flex', alignItems:'center', gap:20, height:46 }}>
                      {['남','여'].map(g=>(
                        <label key={g} onClick={()=>setProfile('gender',g)} style={{ display:'flex', alignItems:'center', gap:7, cursor:'pointer', userSelect:'none' }}>
                          <div style={{ width:18, height:18, borderRadius:'50%', border:`2px solid ${profileForm.gender===g?T.primary:T.border}`, display:'flex', alignItems:'center', justifyContent:'center', transition:'border 150ms' }}>
                            {profileForm.gender===g && <div style={{ width:9, height:9, borderRadius:'50%', background:T.primary }}/>}
                          </div>
                          <span style={{ fontSize:14, color:T.text }}>{g}</span>
                        </label>
                      ))}
                    </div>
                    {/* 생년월일 */}
                    <span style={{ fontSize:13, fontWeight:500, color:T.textSec, paddingTop:13 }}>생년월일 <span style={{color:T.error}}>*</span></span>
                    <div style={{ position:'relative' }}>
                      <input type="date" value={profileForm.birth} onChange={e=>setProfile('birth',e.target.value)}
                        style={{ width:'100%', height:46, borderRadius:8, border:`1.5px solid ${profileForm.birth?T.primary:T.border}`, padding:'0 40px 0 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', appearance:'none', WebkitAppearance:'none', boxSizing:'border-box' }}/>
                      <Icon n="calendar" size={16} color={T.textSec} style={{ position:'absolute', right:14, top:'50%', transform:'translateY(-50%)', pointerEvents:'none' }}/>
                    </div>
                  </div>
                  <div style={{ height:1, background:T.border, marginBottom:20 }}/>
                  <div style={{ display:'flex', gap:12, justifyContent:'center' }}>
                    <Btn size="l" variant="ghost" onClick={()=>setShowWithdraw(true)}>탈퇴하기</Btn>
                    <Btn size="l" variant="primary" onClick={()=>addToast('회원정보가 수정되었습니다.')}>회원정보 수정</Btn>
                  </div>
                </div>
              )}
              {showWithdraw && (
                <Modal onClose={()=>setShowWithdraw(false)}>
                  <div style={{ width:400, padding:'32px 28px 24px', background:T.surface, borderRadius:16, boxShadow:'0 20px 60px rgba(0,0,0,0.15)', textAlign:'center' }}>
                    <h3 style={{ fontSize:19, fontWeight:700, color:T.text, marginBottom:10 }}>정말 탈퇴하시겠습니까?</h3>
                    <p style={{ fontSize:13, color:T.textSec, marginBottom:24, lineHeight:1.6 }}>탈퇴 시 모든 정보가 삭제되며<br/>복구할 수 없습니다.</p>
                    <div style={{ display:'flex', gap:10 }}>
                      <Btn size="m" variant="ghost" fullWidth onClick={()=>setShowWithdraw(false)}>취소</Btn>
                      <Btn size="m" variant="danger" fullWidth onClick={()=>{ setShowWithdraw(false); addToast('탈퇴가 완료되었습니다.'); go('home'); }}>탈퇴하기</Btn>
                    </div>
                  </div>
                </Modal>
              )}
            </div>
          )}
        </div>
      </div>
      {/* Cancel Modal — Step 1: 사유 선택 */}
      {showCancel && cancelStep===1 && (
        <Modal onClose={()=>{setShowCancel(false);setCancelError(false);setCancelReason('');setCancelEtcDetail('');setCancelStep(1);}}>
          <div style={{ width:480, padding:'28px 28px 24px', background:T.surface, borderRadius:16, boxShadow:'0 20px 60px rgba(0,0,0,0.15)' }}>
            <h3 style={{ fontSize:18, fontWeight:700, color:T.text, marginBottom:16 }}>프로그램 신청 취소</h3>
            {/* 프로그램 카드 미리보기 */}
            {(() => {
              const app = applications.find(a=>a.appId===cancelTargetId)||applications[0];
              return (
                <div style={{ display:'flex', alignItems:'center', gap:12, padding:'12px 14px', borderRadius:10, border:`1px solid ${T.border}`, marginBottom:20 }}>
                  <div style={{ width:56, height:56, borderRadius:8, overflow:'hidden', background:'#E5E7EB', flexShrink:0 }}>
                    <img src={IMGS.pg[0]} alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
                  </div>
                  <div style={{ flex:1 }}>
                    <div style={{ fontSize:14, fontWeight:600, color:T.text }}>{app?.title}</div>
                    <div style={{ fontSize:13, color:T.textSec, marginTop:2 }}>2024-07-08 (월) 15:00</div>
                  </div>
                </div>
              );
            })()}
            <span style={{ fontSize:14, fontWeight:600, color:T.text, display:'block', marginBottom:12 }}>취소 사유 선택</span>
            {['단순 변심','신청 정보 변경','기타'].map(r=>(
              <label key={r} onClick={()=>{setCancelReason(r);setCancelError(false);}} style={{ display:'flex', alignItems:'center', gap:10, marginBottom:10, cursor:'pointer', userSelect:'none' }}>
                <div style={{ width:18, height:18, borderRadius:'50%', border:`2px solid ${cancelReason===r?T.primary:T.border}`, display:'flex', alignItems:'center', justifyContent:'center', transition:'border 150ms' }}>
                  {cancelReason===r && <div style={{ width:9, height:9, borderRadius:'50%', background:T.primary }}/>}
                </div>
                <span style={{ fontSize:14, color:T.text }}>{r}</span>
              </label>
            ))}
            {cancelReason==='기타' && (
              <div style={{ marginTop:8, marginLeft:28 }}>
                <textarea
                  value={cancelEtcDetail}
                  onChange={e=>{ if(e.target.value.length<=100) setCancelEtcDetail(e.target.value); }}
                  placeholder="기타 사유를 작성해주세요. (필수)"
                  style={{ width:'100%', height:72, borderRadius:8, border:`1.5px solid ${T.border}`, padding:'10px 12px', fontSize:13, fontFamily:'Pretendard', color:T.text, resize:'none', outline:'none', boxSizing:'border-box' }}
                />
                <div style={{ textAlign:'right', fontSize:12, color:T.textTri, marginTop:2 }}>
                  {cancelEtcDetail.length} / 100
                </div>
              </div>
            )}
            {cancelError && <div style={{ fontSize:12, color:T.error, marginTop:6 }}>필수 선택 항목입니다.</div>}
            <div style={{ display:'flex', gap:10, marginTop:20 }}>
              <Btn size="m" variant="ghost" fullWidth onClick={()=>{setShowCancel(false);setCancelError(false);setCancelReason('');setCancelEtcDetail('');setCancelStep(1);}}>돌아가기</Btn>
              <Btn size="m" variant="primary" fullWidth onClick={doCancel}>확인</Btn>
            </div>
          </div>
        </Modal>
      )}
      {/* Cancel Modal — Step 2: 최종 확인 */}
      {showCancel && cancelStep===2 && (
        <Modal onClose={()=>{setShowCancel(false);setCancelStep(1);setCancelReason('');setCancelEtcDetail('');setCancelError(false);}}>
          <div style={{ width:320, padding:'28px 24px 20px', background:T.surface, borderRadius:16, boxShadow:'0 20px 60px rgba(0,0,0,0.15)', textAlign:'center' }}>
            <div style={{ width:48, height:48, borderRadius:'50%', background:'#FEE2E2', display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 16px' }}>
              <span style={{ fontSize:22, color:T.error, fontWeight:700 }}>!</span>
            </div>
            <p style={{ fontSize:15, fontWeight:600, color:T.text, marginBottom:20 }}>신청 취소하시겠습니까?</p>
            <div style={{ display:'flex', gap:10 }}>
              <Btn size="m" variant="ghost" fullWidth onClick={()=>{setShowCancel(false);setCancelStep(1);setCancelReason('');setCancelEtcDetail('');setCancelError(false);}}>취소</Btn>
              <Btn size="m" variant="danger" fullWidth onClick={doCancel}>확인</Btn>
            </div>
          </div>
        </Modal>
      )}
      <Footer/>
    </div>
  );
}

// ── LOGIN ──
function LoginScreen({ go, onLogin }) {
  const [email, setEmail] = useState('');
  const [pw, setPw] = useState('');
  const [saveId, setSaveId] = useState(false);
  const handle = () => {
    if(!email||!pw){ alert('이메일과 비밀번호를 입력해주세요.'); return; }
    onLogin();
    go('home');
  };
  return (
    <div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
      <div style={{ flex:1, display:'flex', alignItems:'center', justifyContent:'center' }}>
        <div style={{ width:400, textAlign:'center' }}>
          <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:8, marginBottom:20 }}>
            <img src="assets/logo_symbol.png" alt="" style={{ height:30, width:'auto' }}/>
            <span style={{ fontWeight:800, fontSize:20, letterSpacing:-0.3, color:T.primary }}>청년모아</span>
          </div>
          <h2 style={{ fontSize:26, fontWeight:700, color:T.text, marginBottom:24 }}>로그인</h2>
          <div style={{ display:'flex', flexDirection:'column', gap:10, marginBottom:12 }}>
            <input value={email} onChange={e=>setEmail(e.target.value)} placeholder="이메일을 입력해주세요." style={{ height:46, borderRadius:8, border:`1.5px solid ${email?T.primary:T.border}`, padding:'0 16px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', transition:'border 150ms' }}/>
            <input type="password" value={pw} onChange={e=>setPw(e.target.value)} onKeyDown={e=>e.key==='Enter'&&handle()} placeholder="비밀번호를 입력해주세요." style={{ height:46, borderRadius:8, border:`1.5px solid ${pw?T.primary:T.border}`, padding:'0 16px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', transition:'border 150ms' }}/>
          </div>
          <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20 }}>
            <label style={{ display:'flex', alignItems:'center', gap:6, cursor:'pointer', userSelect:'none' }} onClick={()=>setSaveId(v=>!v)}>
              <div style={{ width:16, height:16, borderRadius:4, border:`1.5px solid ${saveId?T.primary:T.border}`, background:saveId?T.primary:'#fff', display:'flex', alignItems:'center', justifyContent:'center', transition:'all 150ms', flexShrink:0 }}>
                {saveId && <svg viewBox="0 0 24 24" style={{width:10,height:10}}><path d="M5 12l5 5L20 7" stroke="#fff" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>}
              </div>
              <span style={{ fontSize:13, color:T.textSec }}>아이디 저장</span>
            </label>
            <div style={{ display:'flex', gap:12, alignItems:'center', fontSize:13 }}>
              <span className="nav-link" style={{ color:T.textSec, cursor:'pointer' }} onClick={()=>go('find-id')}>아이디 찾기</span>
              <span style={{ color:T.borderLight }}>|</span>
              <span className="nav-link" style={{ color:T.textSec, cursor:'pointer' }} onClick={()=>go('find-password')}>비밀번호 찾기</span>
            </div>
          </div>
          <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
            <Btn size="l" fullWidth onClick={handle}>로그인</Btn>
            <Btn size="l" variant="secondary" fullWidth onClick={()=>go('signup')}>회원가입</Btn>
          </div>
        </div>
      </div>
      <Footer/>
    </div>
  );
}

// ── FIND ID ──
function FindIdScreen({ go }) {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [step, setStep] = useState(1); // 1: 입력, 2: 결과
  const [result, setResult] = useState('');
  const handleSubmit = () => {
    if(!name||!phone){ alert('이름과 휴대폰 번호를 입력해주세요.'); return; }
    setResult('user****@naver.com');
    setStep(2);
  };
  const steps = ['가입정보 입력', '아이디 찾기'];
  return (
    <div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
      <div style={{ flex:1, display:'flex', alignItems:'center', justifyContent:'center' }}>
        <div style={{ width:480, textAlign:'center' }}>
          <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:8, marginBottom:20 }}>
            <img src="assets/logo_symbol.png" alt="" style={{ height:30, width:'auto' }}/>
            <span style={{ fontWeight:800, fontSize:20, letterSpacing:-0.3, color:T.primary }}>청년모아</span>
          </div>
          <h2 style={{ fontSize:26, fontWeight:700, color:T.text, marginBottom:24 }}>아이디 찾기</h2>
          {/* Stepper */}
          <div style={{ display:'flex', alignItems:'center', justifyContent:'center', marginBottom:32 }}>
            {steps.map((s, i) => (
              <React.Fragment key={s}>
                <div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:6 }}>
                  <div style={{ width:28, height:28, borderRadius:'50%', background:step>i?T.primary:(step===i+1?T.primary:T.border), display:'flex', alignItems:'center', justifyContent:'center', transition:'background 200ms' }}>
                    {step>i+1
                      ? <svg viewBox="0 0 24 24" style={{width:14,height:14}}><path d="M5 12l5 5L20 7" stroke="#fff" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>
                      : <span style={{ fontSize:12, fontWeight:700, color:'#fff' }}>{i+1}</span>
                    }
                  </div>
                  <span style={{ fontSize:12, fontWeight:step===i+1?600:400, color:step===i+1?T.primary:T.textTri }}>{s}</span>
                </div>
                {i < steps.length-1 && (
                  <div style={{ width:80, height:1.5, background:step>i+1?T.primary:T.borderLight, margin:'0 8px 20px', transition:'background 200ms' }}/>
                )}
              </React.Fragment>
            ))}
          </div>
          {step === 1 ? (
            <>
              <div style={{ display:'flex', flexDirection:'column', gap:10, marginBottom:14 }}>
                <input value={name} onChange={e=>setName(e.target.value)} placeholder="이름을 입력해주세요." style={{ height:46, borderRadius:8, border:`1.5px solid ${name?T.primary:T.border}`, padding:'0 16px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', transition:'border 150ms', boxSizing:'border-box' }}/>
                <input value={phone} onChange={e=>setPhone(e.target.value)} placeholder="휴대폰 번호를 입력해주세요. (- 없이)" style={{ height:46, borderRadius:8, border:`1.5px solid ${phone?T.primary:T.border}`, padding:'0 16px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', transition:'border 150ms', boxSizing:'border-box' }}/>
              </div>
              <Btn size="l" fullWidth onClick={handleSubmit}>확인</Btn>
            </>
          ) : (
            <>
              <div style={{ padding:20, borderRadius:T.radius, background:T.primaryBg, border:`1px solid ${T.primary}`, marginBottom:16, textAlign:'left' }}>
                <div style={{ fontSize:13, color:T.textSec, marginBottom:8 }}>회원님의 아이디</div>
                <div style={{ fontSize:20, fontWeight:700, color:T.primary }}>{result}</div>
              </div>
              <Btn size="l" fullWidth onClick={()=>go('login')}>로그인하기</Btn>
            </>
          )}
          <div style={{ marginTop:16, fontSize:13, display:'flex', justifyContent:'center', gap:12, alignItems:'center' }}>
            <span className="nav-link" onClick={()=>go('login')} style={{ color:T.primary, fontWeight:600, cursor:'pointer' }}>로그인으로 돌아가기</span>
            <span style={{ color:T.borderLight }}>|</span>
            <span className="nav-link" onClick={()=>go('find-password')} style={{ color:T.textSec, cursor:'pointer' }}>비밀번호 찾기</span>
          </div>
        </div>
      </div>
      <Footer/>
    </div>
  );
}

// ── FIND PASSWORD ──
function FindPasswordScreen({ go }) {
  const [email, setEmail] = useState('');
  const [tempPw, setTempPw] = useState('');
  const handleSubmit = () => {
    if(!email){ alert('이메일을 입력해주세요.'); return; }
    setTempPw('Temp#1234');
  };
  return (
    <div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
      <div style={{ flex:1, display:'flex', alignItems:'center', justifyContent:'center' }}>
        <div style={{ width:400, textAlign:'center' }}>
          <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:8, marginBottom:20 }}>
            <img src="assets/logo_symbol.png" alt="" style={{ height:30, width:'auto' }}/>
            <span style={{ fontWeight:800, fontSize:20, letterSpacing:-0.3, color:T.primary }}>청년모아</span>
          </div>
          <h2 style={{ fontSize:26, fontWeight:700, color:T.text, marginBottom:8 }}>비밀번호 찾기</h2>
          <p style={{ fontSize:14, color:T.textSec, marginBottom:28, lineHeight:1.6 }}>가입 시 사용한 이메일 주소를 입력하시면<br/>임시 비밀번호를 발급해드립니다.</p>
          {!tempPw ? (
            <>
              <input value={email} onChange={e=>setEmail(e.target.value)} placeholder="이메일을 입력해주세요." style={{ width:'100%', height:46, borderRadius:8, border:`1.5px solid ${email?T.primary:T.border}`, padding:'0 16px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', transition:'border 150ms', marginBottom:14, boxSizing:'border-box' }}/>
              <Btn size="l" fullWidth onClick={handleSubmit}>임시 비밀번호 발급</Btn>
            </>
          ) : (
            <div style={{ padding:20, borderRadius:T.radius, background:T.primaryBg, border:`1px solid ${T.primary}`, marginBottom:20, textAlign:'left' }}>
              <div style={{ fontSize:13, color:T.textSec, marginBottom:8 }}>발급된 임시 비밀번호</div>
              <div style={{ fontSize:20, fontWeight:700, color:T.primary, letterSpacing:3 }}>{tempPw}</div>
              <div style={{ fontSize:12, color:T.textTri, marginTop:8 }}>로그인 후 반드시 비밀번호를 변경해주세요.</div>
            </div>
          )}
          <div style={{ marginTop:16, fontSize:13, display:'flex', justifyContent:'center', gap:12, alignItems:'center' }}>
            <span className="nav-link" onClick={()=>go('find-id')} style={{ color:T.textSec, cursor:'pointer' }}>아이디 찾기</span>
            <span style={{ color:T.borderLight }}>|</span>
            <span className="nav-link" onClick={()=>go('login')} style={{ color:T.primary, fontWeight:600, cursor:'pointer' }}>로그인으로 돌아가기</span>
          </div>
        </div>
      </div>
      <Footer/>
    </div>
  );
}

// ── SIGNUP ──
function SignupScreen({ go, onLogin }) {
  const [form, setForm] = useState({ email:'', password:'', passwordConfirm:'', name:'', phone:'', gender:'', birthDate:'', zipcode:'', address:'', addressDetail:'' });
  const [emailChecked, setEmailChecked] = useState(false);
  const [showPw, setShowPw] = useState(false);
  const [showPwConfirm, setShowPwConfirm] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [agreePrivacy, setAgreePrivacy] = useState(false);
  const dateRef = useRef(null);
  const set = (k, v) => { setForm(f=>({...f,[k]:v})); if(k==='email') setEmailChecked(false); };
  const checkEmail = () => { if(!form.email) return; setEmailChecked(true); };
  const handleAddressSearch = () => {
    // 추후 Kakao 우편번호 API 연동
    set('zipcode','16488'); set('address','경기 수원시 팔달구 효원로 1');
  };
  const handleSubmit = () => {
    if(!form.email||!form.password||!form.passwordConfirm||!form.name){ alert('필수 항목을 입력해주세요.'); return; }
    if(form.password!==form.passwordConfirm){ alert('비밀번호가 일치하지 않습니다.'); return; }
    if(!emailChecked){ alert('이메일 중복확인을 해주세요.'); return; }
    if(!agreeTerms||!agreePrivacy){ alert('필수 약관에 동의해주세요.'); return; }
    onLogin(); go('home');
  };
  const Lbl = ({text, required}) => (
    <span style={{ fontSize:13, fontWeight:600, color:T.text, display:'block', marginBottom:6 }}>
      {text}{required && <span style={{color:T.error}}> *</span>}
    </span>
  );
  const Inp = ({fkey, placeholder, disabled=false}) => (
    <input value={form[fkey]} onChange={e=>set(fkey,e.target.value)} placeholder={placeholder} disabled={disabled}
      style={{ width:'100%', height:46, borderRadius:8, border:`1.5px solid ${form[fkey]?T.primary:T.border}`, padding:'0 14px', fontSize:14, fontFamily:'Pretendard', color:disabled?T.textTri:T.text, background:disabled?T.bg:'#fff', outline:'none', boxSizing:'border-box', cursor:disabled?'default':'text' }}/>
  );
  const PwInp = ({fkey, show, onToggle, placeholder}) => (
    <div style={{ position:'relative' }}>
      <input type={show?'text':'password'} value={form[fkey]} onChange={e=>set(fkey,e.target.value)} placeholder={placeholder}
        style={{ width:'100%', height:46, borderRadius:8, border:`1.5px solid ${form[fkey]?(fkey==='passwordConfirm'?(form[fkey]===form.password?T.success:T.error):T.primary):T.border}`, padding:'0 44px 0 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none', boxSizing:'border-box' }}/>
      <div onClick={onToggle} style={{ position:'absolute', right:14, top:'50%', transform:'translateY(-50%)', cursor:'pointer', display:'flex' }}>
        <Icon n={show?'eyeOff':'eye'} size={18} color={T.textTri}/>
      </div>
    </div>
  );
  const CheckRow = ({checked, onToggle, label, onView}) => (
    <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between' }}>
      <div style={{ display:'flex', alignItems:'center', gap:8, cursor:'pointer', userSelect:'none', flex:1 }} onClick={onToggle}>
        <div style={{ width:18, height:18, borderRadius:4, border:`1.5px solid ${checked?T.primary:T.border}`, background:checked?T.primary:'#fff', display:'flex', alignItems:'center', justifyContent:'center', transition:'all 150ms', flexShrink:0 }}>
          {checked && <svg viewBox="0 0 24 24" style={{width:11,height:11}}><path d="M5 12l5 5L20 7" stroke="#fff" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>}
        </div>
        <span style={{ fontSize:13, color:T.text }}>{label}</span>
      </div>
      {onView && <span onClick={e=>{e.stopPropagation();onView();}} style={{ fontSize:12, color:T.primary, cursor:'pointer', textDecoration:'underline', flexShrink:0, paddingLeft:8 }}>보기</span>}
    </div>
  );
  const Card = ({children}) => (
    <div style={{ background:T.surface, borderRadius:T.radius, padding:'24px 28px', marginBottom:12, boxShadow:T.shadow }}>
      {children}
    </div>
  );
  const SectionTitle = ({text}) => (
    <div style={{ fontSize:15, fontWeight:700, color:T.text, paddingBottom:14, borderBottom:`1px solid ${T.borderLight}`, marginBottom:18 }}>{text}</div>
  );
  return (
    <div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
      <div style={{ flex:1, display:'flex', justifyContent:'center', padding:'32px 20px 48px' }}>
        <div style={{ width:'100%', maxWidth:640 }}>
          <h2 style={{ fontSize:26, fontWeight:700, color:T.text, textAlign:'center', marginBottom:28 }}>회원가입</h2>
          {/* 계정 정보 */}
          <Card>
            <SectionTitle text="계정 정보"/>
            <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
              <div>
                <Lbl text="이메일" required/>
                <div style={{ display:'flex', gap:8 }}>
                  <input value={form.email} onChange={e=>set('email',e.target.value)} placeholder="이메일을 입력해주세요."
                    style={{ flex:1, height:46, borderRadius:8, border:`1.5px solid ${form.email?T.primary:T.border}`, padding:'0 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, outline:'none' }}/>
                  <Btn size="m" variant={emailChecked?'ghost':'outline'} onClick={checkEmail}>{emailChecked?'확인완료':'중복확인'}</Btn>
                </div>
              </div>
              <div>
                <Lbl text="비밀번호" required/>
                <PwInp fkey="password" show={showPw} onToggle={()=>setShowPw(v=>!v)} placeholder="비밀번호를 입력해주세요. (8자 이상)"/>
              </div>
              <div>
                <Lbl text="비밀번호 확인" required/>
                <PwInp fkey="passwordConfirm" show={showPwConfirm} onToggle={()=>setShowPwConfirm(v=>!v)} placeholder="비밀번호를 다시 입력해주세요."/>
                {form.passwordConfirm && form.passwordConfirm!==form.password && <div style={{ fontSize:12, color:T.error, marginTop:4 }}>비밀번호가 일치하지 않습니다.</div>}
              </div>
            </div>
          </Card>
          {/* 개인 정보 */}
          <Card>
            <SectionTitle text="개인 정보"/>
            <div style={{ display:'flex', flexDirection:'column', gap:16 }}>
              <div>
                <Lbl text="이름" required/>
                <Inp fkey="name" placeholder="이름을 입력해주세요."/>
              </div>
              <div>
                <Lbl text="핸드폰 번호"/>
                <Inp fkey="phone" placeholder="010-0000-0000"/>
              </div>
              <div>
                <Lbl text="주소"/>
                <div style={{ display:'flex', gap:8, marginBottom:8 }}>
                  <input value={form.zipcode} readOnly placeholder="우편번호"
                    style={{ width:140, height:46, borderRadius:8, border:`1.5px solid ${form.zipcode?T.primary:T.border}`, padding:'0 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, background:T.bg, outline:'none', boxSizing:'border-box' }}/>
                  <Btn size="m" variant="outline" onClick={handleAddressSearch}>주소 검색</Btn>
                </div>
                <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
                  <input value={form.address} readOnly placeholder="주소"
                    style={{ width:'100%', height:46, borderRadius:8, border:`1.5px solid ${form.address?T.primary:T.border}`, padding:'0 14px', fontSize:14, fontFamily:'Pretendard', color:T.text, background:T.bg, outline:'none', boxSizing:'border-box' }}/>
                  <Inp fkey="addressDetail" placeholder="상세주소를 입력해주세요."/>
                </div>
              </div>
              <div>
                <Lbl text="성별"/>
                <div style={{ display:'flex', gap:24, height:46, alignItems:'center' }}>
                  {['남','여'].map(g=>(
                    <div key={g} style={{ display:'flex', alignItems:'center', gap:6, cursor:'pointer' }} onClick={()=>set('gender',g)}>
                      <div style={{ width:18, height:18, borderRadius:'50%', border:`1.5px solid ${form.gender===g?T.primary:T.border}`, display:'flex', alignItems:'center', justifyContent:'center', transition:'all 150ms' }}>
                        {form.gender===g && <div style={{ width:8, height:8, borderRadius:'50%', background:T.primary }}/>}
                      </div>
                      <span style={{ fontSize:14, color:T.text }}>{g}</span>
                    </div>
                  ))}
                </div>
              </div>
              <div>
                <Lbl text="생년월일"/>
                <div style={{ position:'relative' }}>
                  <input ref={dateRef} type="date" value={form.birthDate} onChange={e=>set('birthDate',e.target.value)}
                    style={{ width:'100%', height:46, borderRadius:8, border:`1.5px solid ${form.birthDate?T.primary:T.border}`, padding:'0 44px 0 14px', fontSize:14, fontFamily:'Pretendard', color:form.birthDate?T.text:T.textTri, outline:'none', boxSizing:'border-box', appearance:'none', WebkitAppearance:'none' }}/>
                  <div onClick={()=>dateRef.current&&dateRef.current.showPicker&&dateRef.current.showPicker()} style={{ position:'absolute', right:14, top:'50%', transform:'translateY(-50%)', cursor:'pointer', display:'flex' }}>
                    <Icon n="calendar" size={18} color={T.textTri}/>
                  </div>
                </div>
              </div>
            </div>
          </Card>
          {/* 약관 동의 */}
          <Card>
            <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
              <CheckRow checked={agreeTerms} onToggle={()=>setAgreeTerms(v=>!v)} label="이용약관에 동의합니다. (필수)" onView={()=>alert('이용약관 내용입니다.')}/>
              <CheckRow checked={agreePrivacy} onToggle={()=>setAgreePrivacy(v=>!v)} label="개인정보처리방침에 동의합니다. (필수)" onView={()=>alert('개인정보처리방침 내용입니다.')}/>
            </div>
          </Card>
          {/* 버튼 */}
          <div style={{ marginTop:4 }}>
            <Btn size="l" fullWidth onClick={handleSubmit}>가입하기</Btn>
            <div style={{ textAlign:'center', marginTop:16, fontSize:14, color:T.textSec }}>
              이미 계정이 있으신가요?{' '}
              <span onClick={()=>go('login')} style={{ color:T.primary, fontWeight:600, cursor:'pointer', textDecoration:'underline' }}>로그인</span>
            </div>
          </div>
        </div>
      </div>
      <Footer/>
    </div>
  );
}

// ── NOTICES ──
function NoticesScreen({ go }) {
  const catColors = { '행사':T.secondary,'공지':T.primary,'운영':T.success,'기타':T.textTri };
  const cats = ['전체','행사','공지','운영','기타'];
  const [selCat, setSelCat] = useState('전체');
  const [noticePage, setNoticePage] = useState(1);
  const filtered = selCat==='전체' ? NOTICES : NOTICES.filter(n=>n.cat===selCat);
  const totalPages = Math.max(1, Math.ceil(filtered.length/10));
  return (
    <div className="screen-enter" style={{ background:T.surface, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
      <div style={{ padding:'28px 80px 0' }}>
        <h2 style={{ fontSize:28, fontWeight:700, color:T.text, textAlign:'center', marginBottom:20 }}>공지사항</h2>
        {/* 카테고리 탭 */}
        <div style={{ display:'flex', gap:8, justifyContent:'center', marginBottom:20 }}>
          {cats.map(c=>(
            <div key={c} onClick={()=>{setSelCat(c);setNoticePage(1);}} className="btn-hover"
                 style={{ padding:'7px 18px', borderRadius:T.tagR, fontSize:13, fontWeight:600, cursor:'pointer',
                           background:selCat===c?T.primary:'transparent',
                           color:selCat===c?'#fff':T.textSec,
                           border:`1px solid ${selCat===c?T.primary:T.border}`,
                           transition:'all 150ms' }}>
              {c}
            </div>
          ))}
        </div>
      </div>
      <div style={{ padding:'0 80px 48px', flex:1 }}>
        <div style={{ borderTop:`1px solid ${T.text}`, borderBottom:`1px solid ${T.border}` }}>
          <div style={{ display:'grid', gridTemplateColumns:'80px 80px 1fr 120px 80px', padding:'10px 16px', background:T.borderLight, borderBottom:`1px solid ${T.border}` }}>
            {['No','구분','제목','작성일','조회수'].map(h=><span key={h} style={{ fontSize:13, fontWeight:600, color:T.textSec, textAlign:h==='제목'?'left':'center' }}>{h}</span>)}
          </div>
          {filtered.map((n)=>(
            <div key={n.id} className="card-hover" onClick={()=>go('notice-detail',{notice:n})} style={{ display:'grid', gridTemplateColumns:'80px 80px 1fr 120px 80px', padding:'14px 16px', borderBottom:`1px solid ${T.borderLight}`, alignItems:'center', background:n.pin?T.primaryBg:'transparent', cursor:'pointer' }}>
              <span style={{ fontSize:13, color:T.textSec, textAlign:'center' }}>{n.id}</span>
              <div style={{ textAlign:'center' }}><span style={{ display:'inline-block', padding:'2px 10px', borderRadius:T.tagR, background:catColors[n.cat]||T.border, color:'#fff', fontSize:11, fontWeight:600 }}>{n.cat}</span></div>
              <div style={{ display:'flex', alignItems:'center', gap:6 }}>
                {n.pin && <span style={{ fontSize:11, color:T.primary }}>📌</span>}
                <span style={{ fontSize:14, color:T.text, fontWeight:n.pin?600:400 }}>{n.title}</span>
                {n.isNew && <span style={{ display:'inline-flex', alignItems:'center', justifyContent:'center', width:18, height:18, borderRadius:'50%', background:T.primary, color:'#fff', fontSize:10, fontWeight:700, flexShrink:0 }}>N</span>}
              </div>
              <span style={{ fontSize:13, color:T.textTri, textAlign:'center' }}>{n.date}</span>
              <span style={{ fontSize:13, color:T.textTri, textAlign:'center' }}>{n.views.toLocaleString()}</span>
            </div>
          ))}
        </div>
        {totalPages>1 && (
          <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:6, marginTop:28 }}>
            <div onClick={()=>setNoticePage(p=>Math.max(1,p-1))} className="btn-hover"
                 style={{ width:34, height:34, borderRadius:8, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:noticePage>1?'pointer':'default', opacity:noticePage>1?1:0.35 }}>
              <Icon n="arrowL" size={16} color={T.textSec}/>
            </div>
            {Array.from({length:totalPages},(_,i)=>i+1).map(n=>(
              <div key={n} onClick={()=>setNoticePage(n)} className="btn-hover"
                   style={{ width:34, height:34, borderRadius:8, background:noticePage===n?T.primary:'transparent', border:`1px solid ${noticePage===n?T.primary:T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:'pointer', fontSize:13, fontWeight:noticePage===n?600:400, color:noticePage===n?'#fff':T.textSec }}>
                {n}
              </div>
            ))}
            <div onClick={()=>setNoticePage(p=>Math.min(totalPages,p+1))} className="btn-hover"
                 style={{ width:34, height:34, borderRadius:8, border:`1px solid ${T.border}`, display:'flex', alignItems:'center', justifyContent:'center', cursor:noticePage<totalPages?'pointer':'default', opacity:noticePage<totalPages?1:0.35, transform:'scaleX(-1)' }}>
              <Icon n="arrowL" size={16} color={T.textSec}/>
            </div>
          </div>
        )}
      </div>
      <Footer/>
    </div>
  );
}

// ── NOTICE DETAIL ──
function NoticeDetailScreen({ go, notice }) {
  const n = notice || NOTICES[0];
  const idx = NOTICES.findIndex(x=>x.id===n.id);
  const prev = idx>0 ? NOTICES[idx-1] : null;
  const next = idx<NOTICES.length-1 ? NOTICES[idx+1] : null;
  const catColors = { '행사':T.secondary,'공지':T.primary,'운영':T.success,'기타':T.textTri };
  return (
    <div className="screen-enter" style={{ background:T.surface, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
      <div style={{ maxWidth:900, margin:'0 auto', width:'100%', padding:'36px 24px 48px' }}>
        <div className="btn-hover" onClick={()=>go('notices')} style={{ display:'inline-flex', alignItems:'center', gap:6, marginBottom:24, cursor:'pointer', color:T.textSec, fontSize:14 }}>
          <Icon n="arrowL" size={18} color={T.textSec}/> 목록으로
        </div>
        <div style={{ borderTop:`2px solid ${T.text}`, paddingTop:24 }}>
          <div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:12 }}>
            <span style={{ display:'inline-block', padding:'3px 12px', borderRadius:T.tagR, background:catColors[n.cat]||T.border, color:'#fff', fontSize:12, fontWeight:600 }}>{n.cat}</span>
            {n.pin && <span style={{ fontSize:13, color:T.primary, fontWeight:600 }}>📌 고정글</span>}
          </div>
          <h1 style={{ fontSize:24, fontWeight:700, color:T.text, marginBottom:10, lineHeight:1.4 }}>{n.title}</h1>
          <div style={{ display:'flex', gap:16, fontSize:13, color:T.textTri, marginBottom:28, paddingBottom:20, borderBottom:`1px solid ${T.border}` }}>
            <span>{n.date}</span>
            <span>조회 {n.views.toLocaleString()}</span>
          </div>
          <div style={{ width:'100%', height:280, borderRadius:T.radius, overflow:'hidden', marginBottom:28 }}>
            <img src="https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&h=280&fit=crop" alt="" style={{ width:'100%', height:'100%', objectFit:'cover' }}/>
          </div>
          <div style={{ fontSize:15, color:T.text, lineHeight:1.9 }}>
            <p>공지사항 본문 내용이 표시됩니다.</p>
            <p style={{ marginTop:12 }}>실제 서비스에서는 서버에서 받아온 내용이 렌더링됩니다.</p>
          </div>
          {/* 첨부파일 */}
          <div style={{ marginTop:32, paddingTop:20, borderTop:`1px solid ${T.border}` }}>
            <div style={{ fontSize:14, fontWeight:600, color:T.text, marginBottom:10 }}>첨부파일</div>
            <div style={{ display:'flex', alignItems:'center', gap:8, padding:'10px 14px', borderRadius:8, border:`1px solid ${T.borderLight}`, background:T.bg, cursor:'pointer' }} className="btn-hover">
              <Icon n="file" size={16} color={T.textSec}/>
              <span style={{ fontSize:13, color:T.primary, textDecoration:'underline' }}>청년의날_행사안내.pdf</span>
              <span style={{ fontSize:12, color:T.textTri, marginLeft:'auto' }}>1.2MB</span>
            </div>
          </div>
        </div>
        {/* 이전글/다음글 */}
        <div style={{ marginTop:32, border:`1px solid ${T.border}`, borderRadius:T.radius, overflow:'hidden' }}>
          {next && (
            <div onClick={()=>go('notice-detail',{notice:next})} className="card-hover"
                 style={{ display:'flex', alignItems:'center', gap:12, padding:'13px 16px', borderBottom:`1px solid ${T.borderLight}`, cursor:'pointer' }}>
              <Icon n="chevU" size={16} color={T.textSec}/>
              <span style={{ fontSize:12, color:T.textSec, width:40, flexShrink:0 }}>다음글</span>
              <span style={{ fontSize:14, color:T.text }}>{next.title}</span>
            </div>
          )}
          {prev && (
            <div onClick={()=>go('notice-detail',{notice:prev})} className="card-hover"
                 style={{ display:'flex', alignItems:'center', gap:12, padding:'13px 16px', cursor:'pointer' }}>
              <Icon n="chevD" size={16} color={T.textSec}/>
              <span style={{ fontSize:12, color:T.textSec, width:40, flexShrink:0 }}>이전글</span>
              <span style={{ fontSize:14, color:T.text }}>{prev.title}</span>
            </div>
          )}
        </div>
        <div style={{ marginTop:24, display:'flex', justifyContent:'center' }}>
          <Btn variant="ghost" onClick={()=>go('notices')}>목록으로</Btn>
        </div>
      </div>
      <Footer/>
    </div>
  );
}

// ── CENTERS placeholder ──
function CentersScreen() {
  return (
    <div className="screen-enter" style={{ background:T.bg, minHeight:'100vh', display:'flex', flexDirection:'column' }}>
      <div style={{ flex:1, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', gap:16 }}>
        <Icon n="pin" size={64} color={T.border}/>
        <div style={{ fontSize:20, fontWeight:700, color:T.text }}>청년센터 찾기</div>
        <div style={{ fontSize:14, color:T.textSec }}>카카오맵 SDK 연동 후 활성화됩니다</div>
      </div>
      <Footer/>
    </div>
  );
}

// ── MAIN APP ──
function App() {
  const [screen, setScreen] = useState('home');
  const [ctx, setCtx] = useState({});
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [toast, setToast] = useState(null);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const prevScroll = useRef(0);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const go = useCallback((s, c={}) => {
    prevScroll.current = window.scrollY;
    setScreen(s);
    setCtx(c);
    if(s === 'home') setScrolled(false);
    window.scrollTo({ top:0, behavior:'smooth' });
  }, []);

  const addToast = useCallback((msg) => {
    setToast(msg);
  }, []);

  const onLoginClick = () => {
    go('login');
  };

  const noHeader = ['login','find-id','find-password'];
  const showHeader = !noHeader.includes(screen);

  const pages = {
    home: <HomeScreen go={go} isLoggedIn={isLoggedIn}/>,
    programs: <ProgramList go={go}/>,
    'program-detail': <ProgramDetail go={go} pg={ctx.pg||PROGRAMS[0]} isLoggedIn={isLoggedIn} onLoginClick={onLoginClick} addToast={addToast}/>,
    'program-apply': <ProgramApply go={go} pg={ctx.pg||PROGRAMS[0]} addToast={addToast}/>,
    'apply-complete': <ApplyComplete go={go} pg={ctx.pg||PROGRAMS[0]}/>,
    mypage: <MyPage go={go} addToast={addToast}/>,
    notices: <NoticesScreen go={go}/>,
    'notice-detail': <NoticeDetailScreen go={go} notice={ctx.notice||NOTICES[0]}/>,
    centers: <CentersScreen/>,
    login: <LoginScreen go={go} onLogin={()=>setIsLoggedIn(true)}/>,
    signup: <SignupScreen go={go} onLogin={()=>setIsLoggedIn(true)}/>,
    'find-id': <FindIdScreen go={go}/>,
    'find-password': <FindPasswordScreen go={go}/>,
  };

  // Flow indicator
  const FLOW = { home:'홈', programs:'프로그램 목록', 'program-detail':'프로그램 상세', 'program-apply':'프로그램 신청', 'apply-complete':'신청 완료', mypage:'마이페이지', notices:'공지사항', 'notice-detail':'공지사항 상세', centers:'센터 찾기', login:'로그인', signup:'회원가입', 'find-id':'아이디 찾기', 'find-password':'비밀번호 찾기' };

  return (
    <div style={{ minHeight:'100vh', background:'#0F0F13', display:'flex', flexDirection:'column', alignItems:'center' }}>
      {/* Browser chrome simulation */}
      <div style={{ width:'100%', maxWidth:1440, background:T.surface, minHeight:'100vh', position:'relative', boxShadow:'0 0 60px rgba(0,0,0,0.4)' }}>
        {/* Header */}
        {showHeader && (
          <div style={{ position:'sticky', top:0, zIndex:200 }}>
            <Header screen={screen} go={go} isLoggedIn={isLoggedIn} onLoginClick={onLoginClick} scrolled={scrolled} onLogout={()=>{setIsLoggedIn(false);addToast('로그아웃 되었습니다.');go('home');}} onMyPage={()=>go('mypage')}/>
          </div>
        )}
        {/* Page */}
        <div key={screen}>{pages[screen]||pages.home}</div>
      </div>
      {/* Toast */}
      {toast && <Toast msg={toast} onDone={()=>setToast(null)}/>}
      {/* Flow HUD */}
      <div style={{ position:'fixed', bottom:20, right:20, background:'rgba(0,0,0,0.8)', borderRadius:10, padding:'10px 16px', zIndex:9999, backdropFilter:'blur(8px)' }}>
        <div style={{ fontSize:11, color:'rgba(255,255,255,0.5)', marginBottom:4 }}>현재 화면</div>
        <div style={{ fontSize:13, fontWeight:600, color:'#fff' }}>{FLOW[screen]||screen}</div>
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
</script>
</body>
</html>
<script type="application/json" id="__om_srcmap">{"gen":"26c5adaa","entries":[{"id":"26c5adaa:0","tag":"html","openStart":16,"attrEnd":31,"openEnd":32,"closeStart":66058,"closeEnd":66065,"textRuns":[[32,33],[2290,2291],[66057,66058]],"parentId":null,"childIds":["26c5adaa:1","26c5adaa:5"]},{"id":"26c5adaa:1","tag":"head","openStart":33,"attrEnd":38,"openEnd":39,"closeStart":2283,"closeEnd":2290,"textRuns":[[39,42],[65,68],[139,142],[198,201],[325,328],[515,518],[713,716],[903,906],[2282,2283]],"parentId":"26c5adaa:0","childIds":["26c5adaa:2","26c5adaa:3","26c5adaa:4"]},{"id":"26c5adaa:2","tag":"meta","openStart":42,"attrEnd":63,"openEnd":65,"closeEnd":65,"textRuns":[],"parentId":"26c5adaa:1","childIds":[]},{"id":"26c5adaa:3","tag":"meta","openStart":68,"attrEnd":137,"openEnd":139,"closeEnd":139,"textRuns":[],"parentId":"26c5adaa:1","childIds":[]},{"id":"26c5adaa:4","tag":"link","openStart":201,"attrEnd":323,"openEnd":325,"closeEnd":325,"textRuns":[],"parentId":"26c5adaa:1","childIds":[]},{"id":"26c5adaa:5","tag":"body","openStart":2291,"attrEnd":2296,"openEnd":2297,"closeStart":66050,"closeEnd":66057,"textRuns":[[2297,2298],[2319,2321],[66049,66050]],"parentId":"26c5adaa:0","childIds":["26c5adaa:6"]},{"id":"26c5adaa:6","tag":"div","openStart":2298,"attrEnd":2312,"openEnd":2313,"closeStart":2313,"closeEnd":2319,"textRuns":[],"parentId":"26c5adaa:5","childIds":[]}]}</script>