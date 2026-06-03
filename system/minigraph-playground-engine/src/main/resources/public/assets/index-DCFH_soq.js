import{j as h,T as Mf,_ as Hl,H as zc,W as hy}from"./vendor-panels-Cixz1HBJ.js";import{a as kf,b as fy,r as x,N as my,R as Of,u as gy,B as yy,c as by,d as Rf,e as vy}from"./vendor-router-DUFbnzxw.js";import{N as _y,H as es,P as ts,M as xy,u as Sy,a as wy,i as Ey,B as Ty,b as Ny,C as Ay,c as Cy}from"./vendor-xyflow-k-RwjR-l.js";import{c as jy,a as Dy,d as Ul,J as $c}from"./vendor-json-view-Djmwb-hd.js";import{M as My,r as ky}from"./vendor-markdown-Cp1IxVgw.js";(function(){const t=document.createElement("link").relList;if(t&&t.supports&&t.supports("modulepreload"))return;for(const d of document.querySelectorAll('link[rel="modulepreload"]'))c(d);new MutationObserver(d=>{for(const p of d)if(p.type==="childList")for(const b of p.addedNodes)b.tagName==="LINK"&&b.rel==="modulepreload"&&c(b)}).observe(document,{childList:!0,subtree:!0});function s(d){const p={};return d.integrity&&(p.integrity=d.integrity),d.referrerPolicy&&(p.referrerPolicy=d.referrerPolicy),d.crossOrigin==="use-credentials"?p.credentials="include":d.crossOrigin==="anonymous"?p.credentials="omit":p.credentials="same-origin",p}function c(d){if(d.ep)return;d.ep=!0;const p=s(d);fetch(d.href,p)}})();var bc={exports:{}},Bl={},vc={exports:{}},_c={};/**
 * @license React
 * scheduler.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Zh;function Oy(){return Zh||(Zh=1,(function(r){function t(k,R){var $=k.length;k.push(R);e:for(;0<$;){var ee=$-1>>>1,ae=k[ee];if(0<d(ae,R))k[ee]=R,k[$]=ae,$=ee;else break e}}function s(k){return k.length===0?null:k[0]}function c(k){if(k.length===0)return null;var R=k[0],$=k.pop();if($!==R){k[0]=$;e:for(var ee=0,ae=k.length,le=ae>>>1;ee<le;){var re=2*(ee+1)-1,ie=k[re],de=re+1,oe=k[de];if(0>d(ie,$))de<ae&&0>d(oe,ie)?(k[ee]=oe,k[de]=$,ee=de):(k[ee]=ie,k[re]=$,ee=re);else if(de<ae&&0>d(oe,$))k[ee]=oe,k[de]=$,ee=de;else break e}}return R}function d(k,R){var $=k.sortIndex-R.sortIndex;return $!==0?$:k.id-R.id}if(r.unstable_now=void 0,typeof performance=="object"&&typeof performance.now=="function"){var p=performance;r.unstable_now=function(){return p.now()}}else{var b=Date,g=b.now();r.unstable_now=function(){return b.now()-g}}var f=[],y=[],w=1,v=null,S=3,D=!1,N=!1,E=!1,_=!1,T=typeof setTimeout=="function"?setTimeout:null,C=typeof clearTimeout=="function"?clearTimeout:null,j=typeof setImmediate<"u"?setImmediate:null;function L(k){for(var R=s(y);R!==null;){if(R.callback===null)c(y);else if(R.startTime<=k)c(y),R.sortIndex=R.expirationTime,t(f,R);else break;R=s(y)}}function U(k){if(E=!1,L(k),!N)if(s(f)!==null)N=!0,V||(V=!0,G());else{var R=s(y);R!==null&&ne(U,R.startTime-k)}}var V=!1,M=-1,X=5,P=-1;function z(){return _?!0:!(r.unstable_now()-P<X)}function H(){if(_=!1,V){var k=r.unstable_now();P=k;var R=!0;try{e:{N=!1,E&&(E=!1,C(M),M=-1),D=!0;var $=S;try{t:{for(L(k),v=s(f);v!==null&&!(v.expirationTime>k&&z());){var ee=v.callback;if(typeof ee=="function"){v.callback=null,S=v.priorityLevel;var ae=ee(v.expirationTime<=k);if(k=r.unstable_now(),typeof ae=="function"){v.callback=ae,L(k),R=!0;break t}v===s(f)&&c(f),L(k)}else c(f);v=s(f)}if(v!==null)R=!0;else{var le=s(y);le!==null&&ne(U,le.startTime-k),R=!1}}break e}finally{v=null,S=$,D=!1}R=void 0}}finally{R?G():V=!1}}}var G;if(typeof j=="function")G=function(){j(H)};else if(typeof MessageChannel<"u"){var Q=new MessageChannel,Z=Q.port2;Q.port1.onmessage=H,G=function(){Z.postMessage(null)}}else G=function(){T(H,0)};function ne(k,R){M=T(function(){k(r.unstable_now())},R)}r.unstable_IdlePriority=5,r.unstable_ImmediatePriority=1,r.unstable_LowPriority=4,r.unstable_NormalPriority=3,r.unstable_Profiling=null,r.unstable_UserBlockingPriority=2,r.unstable_cancelCallback=function(k){k.callback=null},r.unstable_forceFrameRate=function(k){0>k||125<k?console.error("forceFrameRate takes a positive int between 0 and 125, forcing frame rates higher than 125 fps is not supported"):X=0<k?Math.floor(1e3/k):5},r.unstable_getCurrentPriorityLevel=function(){return S},r.unstable_next=function(k){switch(S){case 1:case 2:case 3:var R=3;break;default:R=S}var $=S;S=R;try{return k()}finally{S=$}},r.unstable_requestPaint=function(){_=!0},r.unstable_runWithPriority=function(k,R){switch(k){case 1:case 2:case 3:case 4:case 5:break;default:k=3}var $=S;S=k;try{return R()}finally{S=$}},r.unstable_scheduleCallback=function(k,R,$){var ee=r.unstable_now();switch(typeof $=="object"&&$!==null?($=$.delay,$=typeof $=="number"&&0<$?ee+$:ee):$=ee,k){case 1:var ae=-1;break;case 2:ae=250;break;case 5:ae=1073741823;break;case 4:ae=1e4;break;default:ae=5e3}return ae=$+ae,k={id:w++,callback:R,priorityLevel:k,startTime:$,expirationTime:ae,sortIndex:-1},$>ee?(k.sortIndex=$,t(y,k),s(f)===null&&k===s(y)&&(E?(C(M),M=-1):E=!0,ne(U,$-ee))):(k.sortIndex=ae,t(f,k),N||D||(N=!0,V||(V=!0,G()))),k},r.unstable_shouldYield=z,r.unstable_wrapCallback=function(k){var R=S;return function(){var $=S;S=R;try{return k.apply(this,arguments)}finally{S=$}}}})(_c)),_c}var Qh;function Ry(){return Qh||(Qh=1,vc.exports=Oy()),vc.exports}/**
 * @license React
 * react-dom-client.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Kh;function zy(){if(Kh)return Bl;Kh=1;var r=Ry(),t=kf(),s=fy();function c(e){var n="https://react.dev/errors/"+e;if(1<arguments.length){n+="?args[]="+encodeURIComponent(arguments[1]);for(var a=2;a<arguments.length;a++)n+="&args[]="+encodeURIComponent(arguments[a])}return"Minified React error #"+e+"; visit "+n+" for the full message or use the non-minified dev environment for full errors and additional helpful warnings."}function d(e){return!(!e||e.nodeType!==1&&e.nodeType!==9&&e.nodeType!==11)}function p(e){var n=e,a=e;if(e.alternate)for(;n.return;)n=n.return;else{e=n;do n=e,(n.flags&4098)!==0&&(a=n.return),e=n.return;while(e)}return n.tag===3?a:null}function b(e){if(e.tag===13){var n=e.memoizedState;if(n===null&&(e=e.alternate,e!==null&&(n=e.memoizedState)),n!==null)return n.dehydrated}return null}function g(e){if(e.tag===31){var n=e.memoizedState;if(n===null&&(e=e.alternate,e!==null&&(n=e.memoizedState)),n!==null)return n.dehydrated}return null}function f(e){if(p(e)!==e)throw Error(c(188))}function y(e){var n=e.alternate;if(!n){if(n=p(e),n===null)throw Error(c(188));return n!==e?null:e}for(var a=e,o=n;;){var l=a.return;if(l===null)break;var i=l.alternate;if(i===null){if(o=l.return,o!==null){a=o;continue}break}if(l.child===i.child){for(i=l.child;i;){if(i===a)return f(l),e;if(i===o)return f(l),n;i=i.sibling}throw Error(c(188))}if(a.return!==o.return)a=l,o=i;else{for(var u=!1,m=l.child;m;){if(m===a){u=!0,a=l,o=i;break}if(m===o){u=!0,o=l,a=i;break}m=m.sibling}if(!u){for(m=i.child;m;){if(m===a){u=!0,a=i,o=l;break}if(m===o){u=!0,o=i,a=l;break}m=m.sibling}if(!u)throw Error(c(189))}}if(a.alternate!==o)throw Error(c(190))}if(a.tag!==3)throw Error(c(188));return a.stateNode.current===a?e:n}function w(e){var n=e.tag;if(n===5||n===26||n===27||n===6)return e;for(e=e.child;e!==null;){if(n=w(e),n!==null)return n;e=e.sibling}return null}var v=Object.assign,S=Symbol.for("react.element"),D=Symbol.for("react.transitional.element"),N=Symbol.for("react.portal"),E=Symbol.for("react.fragment"),_=Symbol.for("react.strict_mode"),T=Symbol.for("react.profiler"),C=Symbol.for("react.consumer"),j=Symbol.for("react.context"),L=Symbol.for("react.forward_ref"),U=Symbol.for("react.suspense"),V=Symbol.for("react.suspense_list"),M=Symbol.for("react.memo"),X=Symbol.for("react.lazy"),P=Symbol.for("react.activity"),z=Symbol.for("react.memo_cache_sentinel"),H=Symbol.iterator;function G(e){return e===null||typeof e!="object"?null:(e=H&&e[H]||e["@@iterator"],typeof e=="function"?e:null)}var Q=Symbol.for("react.client.reference");function Z(e){if(e==null)return null;if(typeof e=="function")return e.$$typeof===Q?null:e.displayName||e.name||null;if(typeof e=="string")return e;switch(e){case E:return"Fragment";case T:return"Profiler";case _:return"StrictMode";case U:return"Suspense";case V:return"SuspenseList";case P:return"Activity"}if(typeof e=="object")switch(e.$$typeof){case N:return"Portal";case j:return e.displayName||"Context";case C:return(e._context.displayName||"Context")+".Consumer";case L:var n=e.render;return e=e.displayName,e||(e=n.displayName||n.name||"",e=e!==""?"ForwardRef("+e+")":"ForwardRef"),e;case M:return n=e.displayName||null,n!==null?n:Z(e.type)||"Memo";case X:n=e._payload,e=e._init;try{return Z(e(n))}catch{}}return null}var ne=Array.isArray,k=t.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,R=s.__DOM_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,$={pending:!1,data:null,method:null,action:null},ee=[],ae=-1;function le(e){return{current:e}}function re(e){0>ae||(e.current=ee[ae],ee[ae]=null,ae--)}function ie(e,n){ae++,ee[ae]=e.current,e.current=n}var de=le(null),oe=le(null),se=le(null),xe=le(null);function ze(e,n){switch(ie(se,n),ie(oe,e),ie(de,null),n.nodeType){case 9:case 11:e=(e=n.documentElement)&&(e=e.namespaceURI)?gh(e):0;break;default:if(e=n.tagName,n=n.namespaceURI)n=gh(n),e=yh(n,e);else switch(e){case"svg":e=1;break;case"math":e=2;break;default:e=0}}re(de),ie(de,e)}function _e(){re(de),re(oe),re(se)}function Je(e){e.memoizedState!==null&&ie(xe,e);var n=de.current,a=yh(n,e.type);n!==a&&(ie(oe,e),ie(de,a))}function je(e){oe.current===e&&(re(de),re(oe)),xe.current===e&&(re(xe),kl._currentValue=$)}var ke,Me;function te(e){if(ke===void 0)try{throw Error()}catch(a){var n=a.stack.trim().match(/\n( *(at )?)/);ke=n&&n[1]||"",Me=-1<a.stack.indexOf(`
    at`)?" (<anonymous>)":-1<a.stack.indexOf("@")?"@unknown:0:0":""}return`
`+ke+e+Me}var pe=!1;function we(e,n){if(!e||pe)return"";pe=!0;var a=Error.prepareStackTrace;Error.prepareStackTrace=void 0;try{var o={DetermineComponentFrameRoot:function(){try{if(n){var F=function(){throw Error()};if(Object.defineProperty(F.prototype,"props",{set:function(){throw Error()}}),typeof Reflect=="object"&&Reflect.construct){try{Reflect.construct(F,[])}catch(J){var I=J}Reflect.construct(e,[],F)}else{try{F.call()}catch(J){I=J}e.call(F.prototype)}}else{try{throw Error()}catch(J){I=J}(F=e())&&typeof F.catch=="function"&&F.catch(function(){})}}catch(J){if(J&&I&&typeof J.stack=="string")return[J.stack,I.stack]}return[null,null]}};o.DetermineComponentFrameRoot.displayName="DetermineComponentFrameRoot";var l=Object.getOwnPropertyDescriptor(o.DetermineComponentFrameRoot,"name");l&&l.configurable&&Object.defineProperty(o.DetermineComponentFrameRoot,"name",{value:"DetermineComponentFrameRoot"});var i=o.DetermineComponentFrameRoot(),u=i[0],m=i[1];if(u&&m){var A=u.split(`
`),q=m.split(`
`);for(l=o=0;o<A.length&&!A[o].includes("DetermineComponentFrameRoot");)o++;for(;l<q.length&&!q[l].includes("DetermineComponentFrameRoot");)l++;if(o===A.length||l===q.length)for(o=A.length-1,l=q.length-1;1<=o&&0<=l&&A[o]!==q[l];)l--;for(;1<=o&&0<=l;o--,l--)if(A[o]!==q[l]){if(o!==1||l!==1)do if(o--,l--,0>l||A[o]!==q[l]){var K=`
`+A[o].replace(" at new "," at ");return e.displayName&&K.includes("<anonymous>")&&(K=K.replace("<anonymous>",e.displayName)),K}while(1<=o&&0<=l);break}}}finally{pe=!1,Error.prepareStackTrace=a}return(a=e?e.displayName||e.name:"")?te(a):""}function ye(e,n){switch(e.tag){case 26:case 27:case 5:return te(e.type);case 16:return te("Lazy");case 13:return e.child!==n&&n!==null?te("Suspense Fallback"):te("Suspense");case 19:return te("SuspenseList");case 0:case 15:return we(e.type,!1);case 11:return we(e.type.render,!1);case 1:return we(e.type,!0);case 31:return te("Activity");default:return""}}function Ee(e){try{var n="",a=null;do n+=ye(e,a),a=e,e=e.return;while(e);return n}catch(o){return`
Error generating stack: `+o.message+`
`+o.stack}}var Ae=Object.prototype.hasOwnProperty,Ue=r.unstable_scheduleCallback,$e=r.unstable_cancelCallback,Le=r.unstable_shouldYield,ht=r.unstable_requestPaint,ve=r.unstable_now,yn=r.unstable_getCurrentPriorityLevel,bn=r.unstable_ImmediatePriority,Et=r.unstable_UserBlockingPriority,Tt=r.unstable_NormalPriority,Pe=r.unstable_LowPriority,Nt=r.unstable_IdlePriority,It=r.log,Vt=r.unstable_setDisableYieldValue,ft=null,Ye=null;function At(e){if(typeof It=="function"&&Vt(e),Ye&&typeof Ye.setStrictMode=="function")try{Ye.setStrictMode(ft,e)}catch{}}var ct=Math.clz32?Math.clz32:zt,Rt=Math.log,Zt=Math.LN2;function zt(e){return e>>>=0,e===0?32:31-(Rt(e)/Zt|0)|0}var Bt=256,Kt=262144,Gt=4194304;function vt(e){var n=e&42;if(n!==0)return n;switch(e&-e){case 1:return 1;case 2:return 2;case 4:return 4;case 8:return 8;case 16:return 16;case 32:return 32;case 64:return 64;case 128:return 128;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:return e&261888;case 262144:case 524288:case 1048576:case 2097152:return e&3932160;case 4194304:case 8388608:case 16777216:case 33554432:return e&62914560;case 67108864:return 67108864;case 134217728:return 134217728;case 268435456:return 268435456;case 536870912:return 536870912;case 1073741824:return 0;default:return e}}function me(e,n,a){var o=e.pendingLanes;if(o===0)return 0;var l=0,i=e.suspendedLanes,u=e.pingedLanes;e=e.warmLanes;var m=o&134217727;return m!==0?(o=m&~i,o!==0?l=vt(o):(u&=m,u!==0?l=vt(u):a||(a=m&~e,a!==0&&(l=vt(a))))):(m=o&~i,m!==0?l=vt(m):u!==0?l=vt(u):a||(a=o&~e,a!==0&&(l=vt(a)))),l===0?0:n!==0&&n!==l&&(n&i)===0&&(i=l&-l,a=n&-n,i>=a||i===32&&(a&4194048)!==0)?n:l}function De(e,n){return(e.pendingLanes&~(e.suspendedLanes&~e.pingedLanes)&n)===0}function Ve(e,n){switch(e){case 1:case 2:case 4:case 8:case 64:return n+250;case 16:case 32:case 128:case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:return n+5e3;case 4194304:case 8388608:case 16777216:case 33554432:return-1;case 67108864:case 134217728:case 268435456:case 536870912:case 1073741824:return-1;default:return-1}}function ge(){var e=Gt;return Gt<<=1,(Gt&62914560)===0&&(Gt=4194304),e}function ot(e){for(var n=[],a=0;31>a;a++)n.push(e);return n}function at(e,n){e.pendingLanes|=n,n!==268435456&&(e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0)}function jn(e,n,a,o,l,i){var u=e.pendingLanes;e.pendingLanes=a,e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0,e.expiredLanes&=a,e.entangledLanes&=a,e.errorRecoveryDisabledLanes&=a,e.shellSuspendCounter=0;var m=e.entanglements,A=e.expirationTimes,q=e.hiddenUpdates;for(a=u&~a;0<a;){var K=31-ct(a),F=1<<K;m[K]=0,A[K]=-1;var I=q[K];if(I!==null)for(q[K]=null,K=0;K<I.length;K++){var J=I[K];J!==null&&(J.lane&=-536870913)}a&=~F}o!==0&&jt(e,o,0),i!==0&&l===0&&e.tag!==0&&(e.suspendedLanes|=i&~(u&~n))}function jt(e,n,a){e.pendingLanes|=n,e.suspendedLanes&=~n;var o=31-ct(n);e.entangledLanes|=n,e.entanglements[o]=e.entanglements[o]|1073741824|a&261930}function Ln(e,n){var a=e.entangledLanes|=n;for(e=e.entanglements;a;){var o=31-ct(a),l=1<<o;l&n|e[o]&n&&(e[o]|=n),a&=~l}}function Yn(e,n){var a=n&-n;return a=(a&42)!==0?1:ra(a),(a&(e.suspendedLanes|n))!==0?0:a}function ra(e){switch(e){case 2:e=1;break;case 8:e=4;break;case 32:e=16;break;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:case 4194304:case 8388608:case 16777216:case 33554432:e=128;break;case 268435456:e=134217728;break;default:e=0}return e}function he(e){return e&=-e,2<e?8<e?(e&134217727)!==0?32:268435456:8:2}function lt(){var e=R.p;return e!==0?e:(e=window.event,e===void 0?32:Lh(e.type))}function Xt(e,n){var a=R.p;try{return R.p=e,n()}finally{R.p=a}}var ln=Math.random().toString(36).slice(2),Ht="__reactFiber$"+ln,$t="__reactProps$"+ln,Fa="__reactContainer$"+ln,cs="__reactEvents$"+ln,nm="__reactListeners$"+ln,am="__reactHandles$"+ln,au="__reactResources$"+ln,Zo="__reactMarker$"+ln;function us(e){delete e[Ht],delete e[$t],delete e[cs],delete e[nm],delete e[am]}function eo(e){var n=e[Ht];if(n)return n;for(var a=e.parentNode;a;){if(n=a[Fa]||a[Ht]){if(a=n.alternate,n.child!==null||a!==null&&a.child!==null)for(e=Eh(e);e!==null;){if(a=e[Ht])return a;e=Eh(e)}return n}e=a,a=e.parentNode}return null}function to(e){if(e=e[Ht]||e[Fa]){var n=e.tag;if(n===5||n===6||n===13||n===31||n===26||n===27||n===3)return e}return null}function Qo(e){var n=e.tag;if(n===5||n===26||n===27||n===6)return e.stateNode;throw Error(c(33))}function no(e){var n=e[au];return n||(n=e[au]={hoistableStyles:new Map,hoistableScripts:new Map}),n}function Dt(e){e[Zo]=!0}var ou=new Set,lu={};function za(e,n){ao(e,n),ao(e+"Capture",n)}function ao(e,n){for(lu[e]=n,e=0;e<n.length;e++)ou.add(n[e])}var om=RegExp("^[:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD][:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD\\-.0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040]*$"),iu={},su={};function lm(e){return Ae.call(su,e)?!0:Ae.call(iu,e)?!1:om.test(e)?su[e]=!0:(iu[e]=!0,!1)}function ql(e,n,a){if(lm(n))if(a===null)e.removeAttribute(n);else{switch(typeof a){case"undefined":case"function":case"symbol":e.removeAttribute(n);return;case"boolean":var o=n.toLowerCase().slice(0,5);if(o!=="data-"&&o!=="aria-"){e.removeAttribute(n);return}}e.setAttribute(n,""+a)}}function Il(e,n,a){if(a===null)e.removeAttribute(n);else{switch(typeof a){case"undefined":case"function":case"symbol":case"boolean":e.removeAttribute(n);return}e.setAttribute(n,""+a)}}function qn(e,n,a,o){if(o===null)e.removeAttribute(a);else{switch(typeof o){case"undefined":case"function":case"symbol":case"boolean":e.removeAttribute(a);return}e.setAttributeNS(n,a,""+o)}}function vn(e){switch(typeof e){case"bigint":case"boolean":case"number":case"string":case"undefined":return e;case"object":return e;default:return""}}function ru(e){var n=e.type;return(e=e.nodeName)&&e.toLowerCase()==="input"&&(n==="checkbox"||n==="radio")}function im(e,n,a){var o=Object.getOwnPropertyDescriptor(e.constructor.prototype,n);if(!e.hasOwnProperty(n)&&typeof o<"u"&&typeof o.get=="function"&&typeof o.set=="function"){var l=o.get,i=o.set;return Object.defineProperty(e,n,{configurable:!0,get:function(){return l.call(this)},set:function(u){a=""+u,i.call(this,u)}}),Object.defineProperty(e,n,{enumerable:o.enumerable}),{getValue:function(){return a},setValue:function(u){a=""+u},stopTracking:function(){e._valueTracker=null,delete e[n]}}}}function ds(e){if(!e._valueTracker){var n=ru(e)?"checked":"value";e._valueTracker=im(e,n,""+e[n])}}function cu(e){if(!e)return!1;var n=e._valueTracker;if(!n)return!0;var a=n.getValue(),o="";return e&&(o=ru(e)?e.checked?"true":"false":e.value),e=o,e!==a?(n.setValue(e),!0):!1}function Xl(e){if(e=e||(typeof document<"u"?document:void 0),typeof e>"u")return null;try{return e.activeElement||e.body}catch{return e.body}}var sm=/[\n"\\]/g;function _n(e){return e.replace(sm,function(n){return"\\"+n.charCodeAt(0).toString(16)+" "})}function ps(e,n,a,o,l,i,u,m){e.name="",u!=null&&typeof u!="function"&&typeof u!="symbol"&&typeof u!="boolean"?e.type=u:e.removeAttribute("type"),n!=null?u==="number"?(n===0&&e.value===""||e.value!=n)&&(e.value=""+vn(n)):e.value!==""+vn(n)&&(e.value=""+vn(n)):u!=="submit"&&u!=="reset"||e.removeAttribute("value"),n!=null?hs(e,u,vn(n)):a!=null?hs(e,u,vn(a)):o!=null&&e.removeAttribute("value"),l==null&&i!=null&&(e.defaultChecked=!!i),l!=null&&(e.checked=l&&typeof l!="function"&&typeof l!="symbol"),m!=null&&typeof m!="function"&&typeof m!="symbol"&&typeof m!="boolean"?e.name=""+vn(m):e.removeAttribute("name")}function uu(e,n,a,o,l,i,u,m){if(i!=null&&typeof i!="function"&&typeof i!="symbol"&&typeof i!="boolean"&&(e.type=i),n!=null||a!=null){if(!(i!=="submit"&&i!=="reset"||n!=null)){ds(e);return}a=a!=null?""+vn(a):"",n=n!=null?""+vn(n):a,m||n===e.value||(e.value=n),e.defaultValue=n}o=o??l,o=typeof o!="function"&&typeof o!="symbol"&&!!o,e.checked=m?e.checked:!!o,e.defaultChecked=!!o,u!=null&&typeof u!="function"&&typeof u!="symbol"&&typeof u!="boolean"&&(e.name=u),ds(e)}function hs(e,n,a){n==="number"&&Xl(e.ownerDocument)===e||e.defaultValue===""+a||(e.defaultValue=""+a)}function oo(e,n,a,o){if(e=e.options,n){n={};for(var l=0;l<a.length;l++)n["$"+a[l]]=!0;for(a=0;a<e.length;a++)l=n.hasOwnProperty("$"+e[a].value),e[a].selected!==l&&(e[a].selected=l),l&&o&&(e[a].defaultSelected=!0)}else{for(a=""+vn(a),n=null,l=0;l<e.length;l++){if(e[l].value===a){e[l].selected=!0,o&&(e[l].defaultSelected=!0);return}n!==null||e[l].disabled||(n=e[l])}n!==null&&(n.selected=!0)}}function du(e,n,a){if(n!=null&&(n=""+vn(n),n!==e.value&&(e.value=n),a==null)){e.defaultValue!==n&&(e.defaultValue=n);return}e.defaultValue=a!=null?""+vn(a):""}function pu(e,n,a,o){if(n==null){if(o!=null){if(a!=null)throw Error(c(92));if(ne(o)){if(1<o.length)throw Error(c(93));o=o[0]}a=o}a==null&&(a=""),n=a}a=vn(n),e.defaultValue=a,o=e.textContent,o===a&&o!==""&&o!==null&&(e.value=o),ds(e)}function lo(e,n){if(n){var a=e.firstChild;if(a&&a===e.lastChild&&a.nodeType===3){a.nodeValue=n;return}}e.textContent=n}var rm=new Set("animationIterationCount aspectRatio borderImageOutset borderImageSlice borderImageWidth boxFlex boxFlexGroup boxOrdinalGroup columnCount columns flex flexGrow flexPositive flexShrink flexNegative flexOrder gridArea gridRow gridRowEnd gridRowSpan gridRowStart gridColumn gridColumnEnd gridColumnSpan gridColumnStart fontWeight lineClamp lineHeight opacity order orphans scale tabSize widows zIndex zoom fillOpacity floodOpacity stopOpacity strokeDasharray strokeDashoffset strokeMiterlimit strokeOpacity strokeWidth MozAnimationIterationCount MozBoxFlex MozBoxFlexGroup MozLineClamp msAnimationIterationCount msFlex msZoom msFlexGrow msFlexNegative msFlexOrder msFlexPositive msFlexShrink msGridColumn msGridColumnSpan msGridRow msGridRowSpan WebkitAnimationIterationCount WebkitBoxFlex WebKitBoxFlexGroup WebkitBoxOrdinalGroup WebkitColumnCount WebkitColumns WebkitFlex WebkitFlexGrow WebkitFlexPositive WebkitFlexShrink WebkitLineClamp".split(" "));function hu(e,n,a){var o=n.indexOf("--")===0;a==null||typeof a=="boolean"||a===""?o?e.setProperty(n,""):n==="float"?e.cssFloat="":e[n]="":o?e.setProperty(n,a):typeof a!="number"||a===0||rm.has(n)?n==="float"?e.cssFloat=a:e[n]=(""+a).trim():e[n]=a+"px"}function fu(e,n,a){if(n!=null&&typeof n!="object")throw Error(c(62));if(e=e.style,a!=null){for(var o in a)!a.hasOwnProperty(o)||n!=null&&n.hasOwnProperty(o)||(o.indexOf("--")===0?e.setProperty(o,""):o==="float"?e.cssFloat="":e[o]="");for(var l in n)o=n[l],n.hasOwnProperty(l)&&a[l]!==o&&hu(e,l,o)}else for(var i in n)n.hasOwnProperty(i)&&hu(e,i,n[i])}function fs(e){if(e.indexOf("-")===-1)return!1;switch(e){case"annotation-xml":case"color-profile":case"font-face":case"font-face-src":case"font-face-uri":case"font-face-format":case"font-face-name":case"missing-glyph":return!1;default:return!0}}var cm=new Map([["acceptCharset","accept-charset"],["htmlFor","for"],["httpEquiv","http-equiv"],["crossOrigin","crossorigin"],["accentHeight","accent-height"],["alignmentBaseline","alignment-baseline"],["arabicForm","arabic-form"],["baselineShift","baseline-shift"],["capHeight","cap-height"],["clipPath","clip-path"],["clipRule","clip-rule"],["colorInterpolation","color-interpolation"],["colorInterpolationFilters","color-interpolation-filters"],["colorProfile","color-profile"],["colorRendering","color-rendering"],["dominantBaseline","dominant-baseline"],["enableBackground","enable-background"],["fillOpacity","fill-opacity"],["fillRule","fill-rule"],["floodColor","flood-color"],["floodOpacity","flood-opacity"],["fontFamily","font-family"],["fontSize","font-size"],["fontSizeAdjust","font-size-adjust"],["fontStretch","font-stretch"],["fontStyle","font-style"],["fontVariant","font-variant"],["fontWeight","font-weight"],["glyphName","glyph-name"],["glyphOrientationHorizontal","glyph-orientation-horizontal"],["glyphOrientationVertical","glyph-orientation-vertical"],["horizAdvX","horiz-adv-x"],["horizOriginX","horiz-origin-x"],["imageRendering","image-rendering"],["letterSpacing","letter-spacing"],["lightingColor","lighting-color"],["markerEnd","marker-end"],["markerMid","marker-mid"],["markerStart","marker-start"],["overlinePosition","overline-position"],["overlineThickness","overline-thickness"],["paintOrder","paint-order"],["panose-1","panose-1"],["pointerEvents","pointer-events"],["renderingIntent","rendering-intent"],["shapeRendering","shape-rendering"],["stopColor","stop-color"],["stopOpacity","stop-opacity"],["strikethroughPosition","strikethrough-position"],["strikethroughThickness","strikethrough-thickness"],["strokeDasharray","stroke-dasharray"],["strokeDashoffset","stroke-dashoffset"],["strokeLinecap","stroke-linecap"],["strokeLinejoin","stroke-linejoin"],["strokeMiterlimit","stroke-miterlimit"],["strokeOpacity","stroke-opacity"],["strokeWidth","stroke-width"],["textAnchor","text-anchor"],["textDecoration","text-decoration"],["textRendering","text-rendering"],["transformOrigin","transform-origin"],["underlinePosition","underline-position"],["underlineThickness","underline-thickness"],["unicodeBidi","unicode-bidi"],["unicodeRange","unicode-range"],["unitsPerEm","units-per-em"],["vAlphabetic","v-alphabetic"],["vHanging","v-hanging"],["vIdeographic","v-ideographic"],["vMathematical","v-mathematical"],["vectorEffect","vector-effect"],["vertAdvY","vert-adv-y"],["vertOriginX","vert-origin-x"],["vertOriginY","vert-origin-y"],["wordSpacing","word-spacing"],["writingMode","writing-mode"],["xmlnsXlink","xmlns:xlink"],["xHeight","x-height"]]),um=/^[\u0000-\u001F ]*j[\r\n\t]*a[\r\n\t]*v[\r\n\t]*a[\r\n\t]*s[\r\n\t]*c[\r\n\t]*r[\r\n\t]*i[\r\n\t]*p[\r\n\t]*t[\r\n\t]*:/i;function Jl(e){return um.test(""+e)?"javascript:throw new Error('React has blocked a javascript: URL as a security precaution.')":e}function In(){}var ms=null;function gs(e){return e=e.target||e.srcElement||window,e.correspondingUseElement&&(e=e.correspondingUseElement),e.nodeType===3?e.parentNode:e}var io=null,so=null;function mu(e){var n=to(e);if(n&&(e=n.stateNode)){var a=e[$t]||null;e:switch(e=n.stateNode,n.type){case"input":if(ps(e,a.value,a.defaultValue,a.defaultValue,a.checked,a.defaultChecked,a.type,a.name),n=a.name,a.type==="radio"&&n!=null){for(a=e;a.parentNode;)a=a.parentNode;for(a=a.querySelectorAll('input[name="'+_n(""+n)+'"][type="radio"]'),n=0;n<a.length;n++){var o=a[n];if(o!==e&&o.form===e.form){var l=o[$t]||null;if(!l)throw Error(c(90));ps(o,l.value,l.defaultValue,l.defaultValue,l.checked,l.defaultChecked,l.type,l.name)}}for(n=0;n<a.length;n++)o=a[n],o.form===e.form&&cu(o)}break e;case"textarea":du(e,a.value,a.defaultValue);break e;case"select":n=a.value,n!=null&&oo(e,!!a.multiple,n,!1)}}}var ys=!1;function gu(e,n,a){if(ys)return e(n,a);ys=!0;try{var o=e(n);return o}finally{if(ys=!1,(io!==null||so!==null)&&(ki(),io&&(n=io,e=so,so=io=null,mu(n),e)))for(n=0;n<e.length;n++)mu(e[n])}}function Ko(e,n){var a=e.stateNode;if(a===null)return null;var o=a[$t]||null;if(o===null)return null;a=o[n];e:switch(n){case"onClick":case"onClickCapture":case"onDoubleClick":case"onDoubleClickCapture":case"onMouseDown":case"onMouseDownCapture":case"onMouseMove":case"onMouseMoveCapture":case"onMouseUp":case"onMouseUpCapture":case"onMouseEnter":(o=!o.disabled)||(e=e.type,o=!(e==="button"||e==="input"||e==="select"||e==="textarea")),e=!o;break e;default:e=!1}if(e)return null;if(a&&typeof a!="function")throw Error(c(231,n,typeof a));return a}var Xn=!(typeof window>"u"||typeof window.document>"u"||typeof window.document.createElement>"u"),bs=!1;if(Xn)try{var $o={};Object.defineProperty($o,"passive",{get:function(){bs=!0}}),window.addEventListener("test",$o,$o),window.removeEventListener("test",$o,$o)}catch{bs=!1}var ca=null,vs=null,Vl=null;function yu(){if(Vl)return Vl;var e,n=vs,a=n.length,o,l="value"in ca?ca.value:ca.textContent,i=l.length;for(e=0;e<a&&n[e]===l[e];e++);var u=a-e;for(o=1;o<=u&&n[a-o]===l[i-o];o++);return Vl=l.slice(e,1<o?1-o:void 0)}function Zl(e){var n=e.keyCode;return"charCode"in e?(e=e.charCode,e===0&&n===13&&(e=13)):e=n,e===10&&(e=13),32<=e||e===13?e:0}function Ql(){return!0}function bu(){return!1}function Pt(e){function n(a,o,l,i,u){this._reactName=a,this._targetInst=l,this.type=o,this.nativeEvent=i,this.target=u,this.currentTarget=null;for(var m in e)e.hasOwnProperty(m)&&(a=e[m],this[m]=a?a(i):i[m]);return this.isDefaultPrevented=(i.defaultPrevented!=null?i.defaultPrevented:i.returnValue===!1)?Ql:bu,this.isPropagationStopped=bu,this}return v(n.prototype,{preventDefault:function(){this.defaultPrevented=!0;var a=this.nativeEvent;a&&(a.preventDefault?a.preventDefault():typeof a.returnValue!="unknown"&&(a.returnValue=!1),this.isDefaultPrevented=Ql)},stopPropagation:function(){var a=this.nativeEvent;a&&(a.stopPropagation?a.stopPropagation():typeof a.cancelBubble!="unknown"&&(a.cancelBubble=!0),this.isPropagationStopped=Ql)},persist:function(){},isPersistent:Ql}),n}var Ba={eventPhase:0,bubbles:0,cancelable:0,timeStamp:function(e){return e.timeStamp||Date.now()},defaultPrevented:0,isTrusted:0},Kl=Pt(Ba),Po=v({},Ba,{view:0,detail:0}),dm=Pt(Po),_s,xs,Wo,$l=v({},Po,{screenX:0,screenY:0,clientX:0,clientY:0,pageX:0,pageY:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,getModifierState:ws,button:0,buttons:0,relatedTarget:function(e){return e.relatedTarget===void 0?e.fromElement===e.srcElement?e.toElement:e.fromElement:e.relatedTarget},movementX:function(e){return"movementX"in e?e.movementX:(e!==Wo&&(Wo&&e.type==="mousemove"?(_s=e.screenX-Wo.screenX,xs=e.screenY-Wo.screenY):xs=_s=0,Wo=e),_s)},movementY:function(e){return"movementY"in e?e.movementY:xs}}),vu=Pt($l),pm=v({},$l,{dataTransfer:0}),hm=Pt(pm),fm=v({},Po,{relatedTarget:0}),Ss=Pt(fm),mm=v({},Ba,{animationName:0,elapsedTime:0,pseudoElement:0}),gm=Pt(mm),ym=v({},Ba,{clipboardData:function(e){return"clipboardData"in e?e.clipboardData:window.clipboardData}}),bm=Pt(ym),vm=v({},Ba,{data:0}),_u=Pt(vm),_m={Esc:"Escape",Spacebar:" ",Left:"ArrowLeft",Up:"ArrowUp",Right:"ArrowRight",Down:"ArrowDown",Del:"Delete",Win:"OS",Menu:"ContextMenu",Apps:"ContextMenu",Scroll:"ScrollLock",MozPrintableKey:"Unidentified"},xm={8:"Backspace",9:"Tab",12:"Clear",13:"Enter",16:"Shift",17:"Control",18:"Alt",19:"Pause",20:"CapsLock",27:"Escape",32:" ",33:"PageUp",34:"PageDown",35:"End",36:"Home",37:"ArrowLeft",38:"ArrowUp",39:"ArrowRight",40:"ArrowDown",45:"Insert",46:"Delete",112:"F1",113:"F2",114:"F3",115:"F4",116:"F5",117:"F6",118:"F7",119:"F8",120:"F9",121:"F10",122:"F11",123:"F12",144:"NumLock",145:"ScrollLock",224:"Meta"},Sm={Alt:"altKey",Control:"ctrlKey",Meta:"metaKey",Shift:"shiftKey"};function wm(e){var n=this.nativeEvent;return n.getModifierState?n.getModifierState(e):(e=Sm[e])?!!n[e]:!1}function ws(){return wm}var Em=v({},Po,{key:function(e){if(e.key){var n=_m[e.key]||e.key;if(n!=="Unidentified")return n}return e.type==="keypress"?(e=Zl(e),e===13?"Enter":String.fromCharCode(e)):e.type==="keydown"||e.type==="keyup"?xm[e.keyCode]||"Unidentified":""},code:0,location:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,repeat:0,locale:0,getModifierState:ws,charCode:function(e){return e.type==="keypress"?Zl(e):0},keyCode:function(e){return e.type==="keydown"||e.type==="keyup"?e.keyCode:0},which:function(e){return e.type==="keypress"?Zl(e):e.type==="keydown"||e.type==="keyup"?e.keyCode:0}}),Tm=Pt(Em),Nm=v({},$l,{pointerId:0,width:0,height:0,pressure:0,tangentialPressure:0,tiltX:0,tiltY:0,twist:0,pointerType:0,isPrimary:0}),xu=Pt(Nm),Am=v({},Po,{touches:0,targetTouches:0,changedTouches:0,altKey:0,metaKey:0,ctrlKey:0,shiftKey:0,getModifierState:ws}),Cm=Pt(Am),jm=v({},Ba,{propertyName:0,elapsedTime:0,pseudoElement:0}),Dm=Pt(jm),Mm=v({},$l,{deltaX:function(e){return"deltaX"in e?e.deltaX:"wheelDeltaX"in e?-e.wheelDeltaX:0},deltaY:function(e){return"deltaY"in e?e.deltaY:"wheelDeltaY"in e?-e.wheelDeltaY:"wheelDelta"in e?-e.wheelDelta:0},deltaZ:0,deltaMode:0}),km=Pt(Mm),Om=v({},Ba,{newState:0,oldState:0}),Rm=Pt(Om),zm=[9,13,27,32],Es=Xn&&"CompositionEvent"in window,Fo=null;Xn&&"documentMode"in document&&(Fo=document.documentMode);var Bm=Xn&&"TextEvent"in window&&!Fo,Su=Xn&&(!Es||Fo&&8<Fo&&11>=Fo),wu=" ",Eu=!1;function Tu(e,n){switch(e){case"keyup":return zm.indexOf(n.keyCode)!==-1;case"keydown":return n.keyCode!==229;case"keypress":case"mousedown":case"focusout":return!0;default:return!1}}function Nu(e){return e=e.detail,typeof e=="object"&&"data"in e?e.data:null}var ro=!1;function Gm(e,n){switch(e){case"compositionend":return Nu(n);case"keypress":return n.which!==32?null:(Eu=!0,wu);case"textInput":return e=n.data,e===wu&&Eu?null:e;default:return null}}function Hm(e,n){if(ro)return e==="compositionend"||!Es&&Tu(e,n)?(e=yu(),Vl=vs=ca=null,ro=!1,e):null;switch(e){case"paste":return null;case"keypress":if(!(n.ctrlKey||n.altKey||n.metaKey)||n.ctrlKey&&n.altKey){if(n.char&&1<n.char.length)return n.char;if(n.which)return String.fromCharCode(n.which)}return null;case"compositionend":return Su&&n.locale!=="ko"?null:n.data;default:return null}}var Um={color:!0,date:!0,datetime:!0,"datetime-local":!0,email:!0,month:!0,number:!0,password:!0,range:!0,search:!0,tel:!0,text:!0,time:!0,url:!0,week:!0};function Au(e){var n=e&&e.nodeName&&e.nodeName.toLowerCase();return n==="input"?!!Um[e.type]:n==="textarea"}function Cu(e,n,a,o){io?so?so.push(o):so=[o]:io=o,n=Ui(n,"onChange"),0<n.length&&(a=new Kl("onChange","change",null,a,o),e.push({event:a,listeners:n}))}var el=null,tl=null;function Lm(e){uh(e,0)}function Pl(e){var n=Qo(e);if(cu(n))return e}function ju(e,n){if(e==="change")return n}var Du=!1;if(Xn){var Ts;if(Xn){var Ns="oninput"in document;if(!Ns){var Mu=document.createElement("div");Mu.setAttribute("oninput","return;"),Ns=typeof Mu.oninput=="function"}Ts=Ns}else Ts=!1;Du=Ts&&(!document.documentMode||9<document.documentMode)}function ku(){el&&(el.detachEvent("onpropertychange",Ou),tl=el=null)}function Ou(e){if(e.propertyName==="value"&&Pl(tl)){var n=[];Cu(n,tl,e,gs(e)),gu(Lm,n)}}function Ym(e,n,a){e==="focusin"?(ku(),el=n,tl=a,el.attachEvent("onpropertychange",Ou)):e==="focusout"&&ku()}function qm(e){if(e==="selectionchange"||e==="keyup"||e==="keydown")return Pl(tl)}function Im(e,n){if(e==="click")return Pl(n)}function Xm(e,n){if(e==="input"||e==="change")return Pl(n)}function Jm(e,n){return e===n&&(e!==0||1/e===1/n)||e!==e&&n!==n}var sn=typeof Object.is=="function"?Object.is:Jm;function nl(e,n){if(sn(e,n))return!0;if(typeof e!="object"||e===null||typeof n!="object"||n===null)return!1;var a=Object.keys(e),o=Object.keys(n);if(a.length!==o.length)return!1;for(o=0;o<a.length;o++){var l=a[o];if(!Ae.call(n,l)||!sn(e[l],n[l]))return!1}return!0}function Ru(e){for(;e&&e.firstChild;)e=e.firstChild;return e}function zu(e,n){var a=Ru(e);e=0;for(var o;a;){if(a.nodeType===3){if(o=e+a.textContent.length,e<=n&&o>=n)return{node:a,offset:n-e};e=o}e:{for(;a;){if(a.nextSibling){a=a.nextSibling;break e}a=a.parentNode}a=void 0}a=Ru(a)}}function Bu(e,n){return e&&n?e===n?!0:e&&e.nodeType===3?!1:n&&n.nodeType===3?Bu(e,n.parentNode):"contains"in e?e.contains(n):e.compareDocumentPosition?!!(e.compareDocumentPosition(n)&16):!1:!1}function Gu(e){e=e!=null&&e.ownerDocument!=null&&e.ownerDocument.defaultView!=null?e.ownerDocument.defaultView:window;for(var n=Xl(e.document);n instanceof e.HTMLIFrameElement;){try{var a=typeof n.contentWindow.location.href=="string"}catch{a=!1}if(a)e=n.contentWindow;else break;n=Xl(e.document)}return n}function As(e){var n=e&&e.nodeName&&e.nodeName.toLowerCase();return n&&(n==="input"&&(e.type==="text"||e.type==="search"||e.type==="tel"||e.type==="url"||e.type==="password")||n==="textarea"||e.contentEditable==="true")}var Vm=Xn&&"documentMode"in document&&11>=document.documentMode,co=null,Cs=null,al=null,js=!1;function Hu(e,n,a){var o=a.window===a?a.document:a.nodeType===9?a:a.ownerDocument;js||co==null||co!==Xl(o)||(o=co,"selectionStart"in o&&As(o)?o={start:o.selectionStart,end:o.selectionEnd}:(o=(o.ownerDocument&&o.ownerDocument.defaultView||window).getSelection(),o={anchorNode:o.anchorNode,anchorOffset:o.anchorOffset,focusNode:o.focusNode,focusOffset:o.focusOffset}),al&&nl(al,o)||(al=o,o=Ui(Cs,"onSelect"),0<o.length&&(n=new Kl("onSelect","select",null,n,a),e.push({event:n,listeners:o}),n.target=co)))}function Ga(e,n){var a={};return a[e.toLowerCase()]=n.toLowerCase(),a["Webkit"+e]="webkit"+n,a["Moz"+e]="moz"+n,a}var uo={animationend:Ga("Animation","AnimationEnd"),animationiteration:Ga("Animation","AnimationIteration"),animationstart:Ga("Animation","AnimationStart"),transitionrun:Ga("Transition","TransitionRun"),transitionstart:Ga("Transition","TransitionStart"),transitioncancel:Ga("Transition","TransitionCancel"),transitionend:Ga("Transition","TransitionEnd")},Ds={},Uu={};Xn&&(Uu=document.createElement("div").style,"AnimationEvent"in window||(delete uo.animationend.animation,delete uo.animationiteration.animation,delete uo.animationstart.animation),"TransitionEvent"in window||delete uo.transitionend.transition);function Ha(e){if(Ds[e])return Ds[e];if(!uo[e])return e;var n=uo[e],a;for(a in n)if(n.hasOwnProperty(a)&&a in Uu)return Ds[e]=n[a];return e}var Lu=Ha("animationend"),Yu=Ha("animationiteration"),qu=Ha("animationstart"),Zm=Ha("transitionrun"),Qm=Ha("transitionstart"),Km=Ha("transitioncancel"),Iu=Ha("transitionend"),Xu=new Map,Ms="abort auxClick beforeToggle cancel canPlay canPlayThrough click close contextMenu copy cut drag dragEnd dragEnter dragExit dragLeave dragOver dragStart drop durationChange emptied encrypted ended error gotPointerCapture input invalid keyDown keyPress keyUp load loadedData loadedMetadata loadStart lostPointerCapture mouseDown mouseMove mouseOut mouseOver mouseUp paste pause play playing pointerCancel pointerDown pointerMove pointerOut pointerOver pointerUp progress rateChange reset resize seeked seeking stalled submit suspend timeUpdate touchCancel touchEnd touchStart volumeChange scroll toggle touchMove waiting wheel".split(" ");Ms.push("scrollEnd");function Dn(e,n){Xu.set(e,n),za(n,[e])}var Wl=typeof reportError=="function"?reportError:function(e){if(typeof window=="object"&&typeof window.ErrorEvent=="function"){var n=new window.ErrorEvent("error",{bubbles:!0,cancelable:!0,message:typeof e=="object"&&e!==null&&typeof e.message=="string"?String(e.message):String(e),error:e});if(!window.dispatchEvent(n))return}else if(typeof process=="object"&&typeof process.emit=="function"){process.emit("uncaughtException",e);return}console.error(e)},xn=[],po=0,ks=0;function Fl(){for(var e=po,n=ks=po=0;n<e;){var a=xn[n];xn[n++]=null;var o=xn[n];xn[n++]=null;var l=xn[n];xn[n++]=null;var i=xn[n];if(xn[n++]=null,o!==null&&l!==null){var u=o.pending;u===null?l.next=l:(l.next=u.next,u.next=l),o.pending=l}i!==0&&Ju(a,l,i)}}function ei(e,n,a,o){xn[po++]=e,xn[po++]=n,xn[po++]=a,xn[po++]=o,ks|=o,e.lanes|=o,e=e.alternate,e!==null&&(e.lanes|=o)}function Os(e,n,a,o){return ei(e,n,a,o),ti(e)}function Ua(e,n){return ei(e,null,null,n),ti(e)}function Ju(e,n,a){e.lanes|=a;var o=e.alternate;o!==null&&(o.lanes|=a);for(var l=!1,i=e.return;i!==null;)i.childLanes|=a,o=i.alternate,o!==null&&(o.childLanes|=a),i.tag===22&&(e=i.stateNode,e===null||e._visibility&1||(l=!0)),e=i,i=i.return;return e.tag===3?(i=e.stateNode,l&&n!==null&&(l=31-ct(a),e=i.hiddenUpdates,o=e[l],o===null?e[l]=[n]:o.push(n),n.lane=a|536870912),i):null}function ti(e){if(50<Tl)throw Tl=0,qr=null,Error(c(185));for(var n=e.return;n!==null;)e=n,n=e.return;return e.tag===3?e.stateNode:null}var ho={};function $m(e,n,a,o){this.tag=e,this.key=a,this.sibling=this.child=this.return=this.stateNode=this.type=this.elementType=null,this.index=0,this.refCleanup=this.ref=null,this.pendingProps=n,this.dependencies=this.memoizedState=this.updateQueue=this.memoizedProps=null,this.mode=o,this.subtreeFlags=this.flags=0,this.deletions=null,this.childLanes=this.lanes=0,this.alternate=null}function rn(e,n,a,o){return new $m(e,n,a,o)}function Rs(e){return e=e.prototype,!(!e||!e.isReactComponent)}function Jn(e,n){var a=e.alternate;return a===null?(a=rn(e.tag,n,e.key,e.mode),a.elementType=e.elementType,a.type=e.type,a.stateNode=e.stateNode,a.alternate=e,e.alternate=a):(a.pendingProps=n,a.type=e.type,a.flags=0,a.subtreeFlags=0,a.deletions=null),a.flags=e.flags&65011712,a.childLanes=e.childLanes,a.lanes=e.lanes,a.child=e.child,a.memoizedProps=e.memoizedProps,a.memoizedState=e.memoizedState,a.updateQueue=e.updateQueue,n=e.dependencies,a.dependencies=n===null?null:{lanes:n.lanes,firstContext:n.firstContext},a.sibling=e.sibling,a.index=e.index,a.ref=e.ref,a.refCleanup=e.refCleanup,a}function Vu(e,n){e.flags&=65011714;var a=e.alternate;return a===null?(e.childLanes=0,e.lanes=n,e.child=null,e.subtreeFlags=0,e.memoizedProps=null,e.memoizedState=null,e.updateQueue=null,e.dependencies=null,e.stateNode=null):(e.childLanes=a.childLanes,e.lanes=a.lanes,e.child=a.child,e.subtreeFlags=0,e.deletions=null,e.memoizedProps=a.memoizedProps,e.memoizedState=a.memoizedState,e.updateQueue=a.updateQueue,e.type=a.type,n=a.dependencies,e.dependencies=n===null?null:{lanes:n.lanes,firstContext:n.firstContext}),e}function ni(e,n,a,o,l,i){var u=0;if(o=e,typeof e=="function")Rs(e)&&(u=1);else if(typeof e=="string")u=ty(e,a,de.current)?26:e==="html"||e==="head"||e==="body"?27:5;else e:switch(e){case P:return e=rn(31,a,n,l),e.elementType=P,e.lanes=i,e;case E:return La(a.children,l,i,n);case _:u=8,l|=24;break;case T:return e=rn(12,a,n,l|2),e.elementType=T,e.lanes=i,e;case U:return e=rn(13,a,n,l),e.elementType=U,e.lanes=i,e;case V:return e=rn(19,a,n,l),e.elementType=V,e.lanes=i,e;default:if(typeof e=="object"&&e!==null)switch(e.$$typeof){case j:u=10;break e;case C:u=9;break e;case L:u=11;break e;case M:u=14;break e;case X:u=16,o=null;break e}u=29,a=Error(c(130,e===null?"null":typeof e,"")),o=null}return n=rn(u,a,n,l),n.elementType=e,n.type=o,n.lanes=i,n}function La(e,n,a,o){return e=rn(7,e,o,n),e.lanes=a,e}function zs(e,n,a){return e=rn(6,e,null,n),e.lanes=a,e}function Zu(e){var n=rn(18,null,null,0);return n.stateNode=e,n}function Bs(e,n,a){return n=rn(4,e.children!==null?e.children:[],e.key,n),n.lanes=a,n.stateNode={containerInfo:e.containerInfo,pendingChildren:null,implementation:e.implementation},n}var Qu=new WeakMap;function Sn(e,n){if(typeof e=="object"&&e!==null){var a=Qu.get(e);return a!==void 0?a:(n={value:e,source:n,stack:Ee(n)},Qu.set(e,n),n)}return{value:e,source:n,stack:Ee(n)}}var fo=[],mo=0,ai=null,ol=0,wn=[],En=0,ua=null,Rn=1,zn="";function Vn(e,n){fo[mo++]=ol,fo[mo++]=ai,ai=e,ol=n}function Ku(e,n,a){wn[En++]=Rn,wn[En++]=zn,wn[En++]=ua,ua=e;var o=Rn;e=zn;var l=32-ct(o)-1;o&=~(1<<l),a+=1;var i=32-ct(n)+l;if(30<i){var u=l-l%5;i=(o&(1<<u)-1).toString(32),o>>=u,l-=u,Rn=1<<32-ct(n)+l|a<<l|o,zn=i+e}else Rn=1<<i|a<<l|o,zn=e}function Gs(e){e.return!==null&&(Vn(e,1),Ku(e,1,0))}function Hs(e){for(;e===ai;)ai=fo[--mo],fo[mo]=null,ol=fo[--mo],fo[mo]=null;for(;e===ua;)ua=wn[--En],wn[En]=null,zn=wn[--En],wn[En]=null,Rn=wn[--En],wn[En]=null}function $u(e,n){wn[En++]=Rn,wn[En++]=zn,wn[En++]=ua,Rn=n.id,zn=n.overflow,ua=e}var Ut=null,it=null,He=!1,da=null,Tn=!1,Us=Error(c(519));function pa(e){var n=Error(c(418,1<arguments.length&&arguments[1]!==void 0&&arguments[1]?"text":"HTML",""));throw ll(Sn(n,e)),Us}function Pu(e){var n=e.stateNode,a=e.type,o=e.memoizedProps;switch(n[Ht]=e,n[$t]=o,a){case"dialog":Re("cancel",n),Re("close",n);break;case"iframe":case"object":case"embed":Re("load",n);break;case"video":case"audio":for(a=0;a<Al.length;a++)Re(Al[a],n);break;case"source":Re("error",n);break;case"img":case"image":case"link":Re("error",n),Re("load",n);break;case"details":Re("toggle",n);break;case"input":Re("invalid",n),uu(n,o.value,o.defaultValue,o.checked,o.defaultChecked,o.type,o.name,!0);break;case"select":Re("invalid",n);break;case"textarea":Re("invalid",n),pu(n,o.value,o.defaultValue,o.children)}a=o.children,typeof a!="string"&&typeof a!="number"&&typeof a!="bigint"||n.textContent===""+a||o.suppressHydrationWarning===!0||fh(n.textContent,a)?(o.popover!=null&&(Re("beforetoggle",n),Re("toggle",n)),o.onScroll!=null&&Re("scroll",n),o.onScrollEnd!=null&&Re("scrollend",n),o.onClick!=null&&(n.onclick=In),n=!0):n=!1,n||pa(e,!0)}function Wu(e){for(Ut=e.return;Ut;)switch(Ut.tag){case 5:case 31:case 13:Tn=!1;return;case 27:case 3:Tn=!0;return;default:Ut=Ut.return}}function go(e){if(e!==Ut)return!1;if(!He)return Wu(e),He=!0,!1;var n=e.tag,a;if((a=n!==3&&n!==27)&&((a=n===5)&&(a=e.type,a=!(a!=="form"&&a!=="button")||ac(e.type,e.memoizedProps)),a=!a),a&&it&&pa(e),Wu(e),n===13){if(e=e.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(c(317));it=wh(e)}else if(n===31){if(e=e.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(c(317));it=wh(e)}else n===27?(n=it,Na(e.type)?(e=rc,rc=null,it=e):it=n):it=Ut?An(e.stateNode.nextSibling):null;return!0}function Ya(){it=Ut=null,He=!1}function Ls(){var e=da;return e!==null&&(tn===null?tn=e:tn.push.apply(tn,e),da=null),e}function ll(e){da===null?da=[e]:da.push(e)}var Ys=le(null),qa=null,Zn=null;function ha(e,n,a){ie(Ys,n._currentValue),n._currentValue=a}function Qn(e){e._currentValue=Ys.current,re(Ys)}function qs(e,n,a){for(;e!==null;){var o=e.alternate;if((e.childLanes&n)!==n?(e.childLanes|=n,o!==null&&(o.childLanes|=n)):o!==null&&(o.childLanes&n)!==n&&(o.childLanes|=n),e===a)break;e=e.return}}function Is(e,n,a,o){var l=e.child;for(l!==null&&(l.return=e);l!==null;){var i=l.dependencies;if(i!==null){var u=l.child;i=i.firstContext;e:for(;i!==null;){var m=i;i=l;for(var A=0;A<n.length;A++)if(m.context===n[A]){i.lanes|=a,m=i.alternate,m!==null&&(m.lanes|=a),qs(i.return,a,e),o||(u=null);break e}i=m.next}}else if(l.tag===18){if(u=l.return,u===null)throw Error(c(341));u.lanes|=a,i=u.alternate,i!==null&&(i.lanes|=a),qs(u,a,e),u=null}else u=l.child;if(u!==null)u.return=l;else for(u=l;u!==null;){if(u===e){u=null;break}if(l=u.sibling,l!==null){l.return=u.return,u=l;break}u=u.return}l=u}}function yo(e,n,a,o){e=null;for(var l=n,i=!1;l!==null;){if(!i){if((l.flags&524288)!==0)i=!0;else if((l.flags&262144)!==0)break}if(l.tag===10){var u=l.alternate;if(u===null)throw Error(c(387));if(u=u.memoizedProps,u!==null){var m=l.type;sn(l.pendingProps.value,u.value)||(e!==null?e.push(m):e=[m])}}else if(l===xe.current){if(u=l.alternate,u===null)throw Error(c(387));u.memoizedState.memoizedState!==l.memoizedState.memoizedState&&(e!==null?e.push(kl):e=[kl])}l=l.return}e!==null&&Is(n,e,a,o),n.flags|=262144}function oi(e){for(e=e.firstContext;e!==null;){if(!sn(e.context._currentValue,e.memoizedValue))return!0;e=e.next}return!1}function Ia(e){qa=e,Zn=null,e=e.dependencies,e!==null&&(e.firstContext=null)}function Lt(e){return Fu(qa,e)}function li(e,n){return qa===null&&Ia(e),Fu(e,n)}function Fu(e,n){var a=n._currentValue;if(n={context:n,memoizedValue:a,next:null},Zn===null){if(e===null)throw Error(c(308));Zn=n,e.dependencies={lanes:0,firstContext:n},e.flags|=524288}else Zn=Zn.next=n;return a}var Pm=typeof AbortController<"u"?AbortController:function(){var e=[],n=this.signal={aborted:!1,addEventListener:function(a,o){e.push(o)}};this.abort=function(){n.aborted=!0,e.forEach(function(a){return a()})}},Wm=r.unstable_scheduleCallback,Fm=r.unstable_NormalPriority,_t={$$typeof:j,Consumer:null,Provider:null,_currentValue:null,_currentValue2:null,_threadCount:0};function Xs(){return{controller:new Pm,data:new Map,refCount:0}}function il(e){e.refCount--,e.refCount===0&&Wm(Fm,function(){e.controller.abort()})}var sl=null,Js=0,bo=0,vo=null;function eg(e,n){if(sl===null){var a=sl=[];Js=0,bo=Qr(),vo={status:"pending",value:void 0,then:function(o){a.push(o)}}}return Js++,n.then(ed,ed),n}function ed(){if(--Js===0&&sl!==null){vo!==null&&(vo.status="fulfilled");var e=sl;sl=null,bo=0,vo=null;for(var n=0;n<e.length;n++)(0,e[n])()}}function tg(e,n){var a=[],o={status:"pending",value:null,reason:null,then:function(l){a.push(l)}};return e.then(function(){o.status="fulfilled",o.value=n;for(var l=0;l<a.length;l++)(0,a[l])(n)},function(l){for(o.status="rejected",o.reason=l,l=0;l<a.length;l++)(0,a[l])(void 0)}),o}var td=k.S;k.S=function(e,n){Hp=ve(),typeof n=="object"&&n!==null&&typeof n.then=="function"&&eg(e,n),td!==null&&td(e,n)};var Xa=le(null);function Vs(){var e=Xa.current;return e!==null?e:tt.pooledCache}function ii(e,n){n===null?ie(Xa,Xa.current):ie(Xa,n.pool)}function nd(){var e=Vs();return e===null?null:{parent:_t._currentValue,pool:e}}var _o=Error(c(460)),Zs=Error(c(474)),si=Error(c(542)),ri={then:function(){}};function ad(e){return e=e.status,e==="fulfilled"||e==="rejected"}function od(e,n,a){switch(a=e[a],a===void 0?e.push(n):a!==n&&(n.then(In,In),n=a),n.status){case"fulfilled":return n.value;case"rejected":throw e=n.reason,id(e),e;default:if(typeof n.status=="string")n.then(In,In);else{if(e=tt,e!==null&&100<e.shellSuspendCounter)throw Error(c(482));e=n,e.status="pending",e.then(function(o){if(n.status==="pending"){var l=n;l.status="fulfilled",l.value=o}},function(o){if(n.status==="pending"){var l=n;l.status="rejected",l.reason=o}})}switch(n.status){case"fulfilled":return n.value;case"rejected":throw e=n.reason,id(e),e}throw Va=n,_o}}function Ja(e){try{var n=e._init;return n(e._payload)}catch(a){throw a!==null&&typeof a=="object"&&typeof a.then=="function"?(Va=a,_o):a}}var Va=null;function ld(){if(Va===null)throw Error(c(459));var e=Va;return Va=null,e}function id(e){if(e===_o||e===si)throw Error(c(483))}var xo=null,rl=0;function ci(e){var n=rl;return rl+=1,xo===null&&(xo=[]),od(xo,e,n)}function cl(e,n){n=n.props.ref,e.ref=n!==void 0?n:null}function ui(e,n){throw n.$$typeof===S?Error(c(525)):(e=Object.prototype.toString.call(n),Error(c(31,e==="[object Object]"?"object with keys {"+Object.keys(n).join(", ")+"}":e)))}function sd(e){function n(B,O){if(e){var Y=B.deletions;Y===null?(B.deletions=[O],B.flags|=16):Y.push(O)}}function a(B,O){if(!e)return null;for(;O!==null;)n(B,O),O=O.sibling;return null}function o(B){for(var O=new Map;B!==null;)B.key!==null?O.set(B.key,B):O.set(B.index,B),B=B.sibling;return O}function l(B,O){return B=Jn(B,O),B.index=0,B.sibling=null,B}function i(B,O,Y){return B.index=Y,e?(Y=B.alternate,Y!==null?(Y=Y.index,Y<O?(B.flags|=67108866,O):Y):(B.flags|=67108866,O)):(B.flags|=1048576,O)}function u(B){return e&&B.alternate===null&&(B.flags|=67108866),B}function m(B,O,Y,W){return O===null||O.tag!==6?(O=zs(Y,B.mode,W),O.return=B,O):(O=l(O,Y),O.return=B,O)}function A(B,O,Y,W){var fe=Y.type;return fe===E?K(B,O,Y.props.children,W,Y.key):O!==null&&(O.elementType===fe||typeof fe=="object"&&fe!==null&&fe.$$typeof===X&&Ja(fe)===O.type)?(O=l(O,Y.props),cl(O,Y),O.return=B,O):(O=ni(Y.type,Y.key,Y.props,null,B.mode,W),cl(O,Y),O.return=B,O)}function q(B,O,Y,W){return O===null||O.tag!==4||O.stateNode.containerInfo!==Y.containerInfo||O.stateNode.implementation!==Y.implementation?(O=Bs(Y,B.mode,W),O.return=B,O):(O=l(O,Y.children||[]),O.return=B,O)}function K(B,O,Y,W,fe){return O===null||O.tag!==7?(O=La(Y,B.mode,W,fe),O.return=B,O):(O=l(O,Y),O.return=B,O)}function F(B,O,Y){if(typeof O=="string"&&O!==""||typeof O=="number"||typeof O=="bigint")return O=zs(""+O,B.mode,Y),O.return=B,O;if(typeof O=="object"&&O!==null){switch(O.$$typeof){case D:return Y=ni(O.type,O.key,O.props,null,B.mode,Y),cl(Y,O),Y.return=B,Y;case N:return O=Bs(O,B.mode,Y),O.return=B,O;case X:return O=Ja(O),F(B,O,Y)}if(ne(O)||G(O))return O=La(O,B.mode,Y,null),O.return=B,O;if(typeof O.then=="function")return F(B,ci(O),Y);if(O.$$typeof===j)return F(B,li(B,O),Y);ui(B,O)}return null}function I(B,O,Y,W){var fe=O!==null?O.key:null;if(typeof Y=="string"&&Y!==""||typeof Y=="number"||typeof Y=="bigint")return fe!==null?null:m(B,O,""+Y,W);if(typeof Y=="object"&&Y!==null){switch(Y.$$typeof){case D:return Y.key===fe?A(B,O,Y,W):null;case N:return Y.key===fe?q(B,O,Y,W):null;case X:return Y=Ja(Y),I(B,O,Y,W)}if(ne(Y)||G(Y))return fe!==null?null:K(B,O,Y,W,null);if(typeof Y.then=="function")return I(B,O,ci(Y),W);if(Y.$$typeof===j)return I(B,O,li(B,Y),W);ui(B,Y)}return null}function J(B,O,Y,W,fe){if(typeof W=="string"&&W!==""||typeof W=="number"||typeof W=="bigint")return B=B.get(Y)||null,m(O,B,""+W,fe);if(typeof W=="object"&&W!==null){switch(W.$$typeof){case D:return B=B.get(W.key===null?Y:W.key)||null,A(O,B,W,fe);case N:return B=B.get(W.key===null?Y:W.key)||null,q(O,B,W,fe);case X:return W=Ja(W),J(B,O,Y,W,fe)}if(ne(W)||G(W))return B=B.get(Y)||null,K(O,B,W,fe,null);if(typeof W.then=="function")return J(B,O,Y,ci(W),fe);if(W.$$typeof===j)return J(B,O,Y,li(O,W),fe);ui(O,W)}return null}function ce(B,O,Y,W){for(var fe=null,qe=null,ue=O,Ce=O=0,Ge=null;ue!==null&&Ce<Y.length;Ce++){ue.index>Ce?(Ge=ue,ue=null):Ge=ue.sibling;var Ie=I(B,ue,Y[Ce],W);if(Ie===null){ue===null&&(ue=Ge);break}e&&ue&&Ie.alternate===null&&n(B,ue),O=i(Ie,O,Ce),qe===null?fe=Ie:qe.sibling=Ie,qe=Ie,ue=Ge}if(Ce===Y.length)return a(B,ue),He&&Vn(B,Ce),fe;if(ue===null){for(;Ce<Y.length;Ce++)ue=F(B,Y[Ce],W),ue!==null&&(O=i(ue,O,Ce),qe===null?fe=ue:qe.sibling=ue,qe=ue);return He&&Vn(B,Ce),fe}for(ue=o(ue);Ce<Y.length;Ce++)Ge=J(ue,B,Ce,Y[Ce],W),Ge!==null&&(e&&Ge.alternate!==null&&ue.delete(Ge.key===null?Ce:Ge.key),O=i(Ge,O,Ce),qe===null?fe=Ge:qe.sibling=Ge,qe=Ge);return e&&ue.forEach(function(Ma){return n(B,Ma)}),He&&Vn(B,Ce),fe}function be(B,O,Y,W){if(Y==null)throw Error(c(151));for(var fe=null,qe=null,ue=O,Ce=O=0,Ge=null,Ie=Y.next();ue!==null&&!Ie.done;Ce++,Ie=Y.next()){ue.index>Ce?(Ge=ue,ue=null):Ge=ue.sibling;var Ma=I(B,ue,Ie.value,W);if(Ma===null){ue===null&&(ue=Ge);break}e&&ue&&Ma.alternate===null&&n(B,ue),O=i(Ma,O,Ce),qe===null?fe=Ma:qe.sibling=Ma,qe=Ma,ue=Ge}if(Ie.done)return a(B,ue),He&&Vn(B,Ce),fe;if(ue===null){for(;!Ie.done;Ce++,Ie=Y.next())Ie=F(B,Ie.value,W),Ie!==null&&(O=i(Ie,O,Ce),qe===null?fe=Ie:qe.sibling=Ie,qe=Ie);return He&&Vn(B,Ce),fe}for(ue=o(ue);!Ie.done;Ce++,Ie=Y.next())Ie=J(ue,B,Ce,Ie.value,W),Ie!==null&&(e&&Ie.alternate!==null&&ue.delete(Ie.key===null?Ce:Ie.key),O=i(Ie,O,Ce),qe===null?fe=Ie:qe.sibling=Ie,qe=Ie);return e&&ue.forEach(function(py){return n(B,py)}),He&&Vn(B,Ce),fe}function et(B,O,Y,W){if(typeof Y=="object"&&Y!==null&&Y.type===E&&Y.key===null&&(Y=Y.props.children),typeof Y=="object"&&Y!==null){switch(Y.$$typeof){case D:e:{for(var fe=Y.key;O!==null;){if(O.key===fe){if(fe=Y.type,fe===E){if(O.tag===7){a(B,O.sibling),W=l(O,Y.props.children),W.return=B,B=W;break e}}else if(O.elementType===fe||typeof fe=="object"&&fe!==null&&fe.$$typeof===X&&Ja(fe)===O.type){a(B,O.sibling),W=l(O,Y.props),cl(W,Y),W.return=B,B=W;break e}a(B,O);break}else n(B,O);O=O.sibling}Y.type===E?(W=La(Y.props.children,B.mode,W,Y.key),W.return=B,B=W):(W=ni(Y.type,Y.key,Y.props,null,B.mode,W),cl(W,Y),W.return=B,B=W)}return u(B);case N:e:{for(fe=Y.key;O!==null;){if(O.key===fe)if(O.tag===4&&O.stateNode.containerInfo===Y.containerInfo&&O.stateNode.implementation===Y.implementation){a(B,O.sibling),W=l(O,Y.children||[]),W.return=B,B=W;break e}else{a(B,O);break}else n(B,O);O=O.sibling}W=Bs(Y,B.mode,W),W.return=B,B=W}return u(B);case X:return Y=Ja(Y),et(B,O,Y,W)}if(ne(Y))return ce(B,O,Y,W);if(G(Y)){if(fe=G(Y),typeof fe!="function")throw Error(c(150));return Y=fe.call(Y),be(B,O,Y,W)}if(typeof Y.then=="function")return et(B,O,ci(Y),W);if(Y.$$typeof===j)return et(B,O,li(B,Y),W);ui(B,Y)}return typeof Y=="string"&&Y!==""||typeof Y=="number"||typeof Y=="bigint"?(Y=""+Y,O!==null&&O.tag===6?(a(B,O.sibling),W=l(O,Y),W.return=B,B=W):(a(B,O),W=zs(Y,B.mode,W),W.return=B,B=W),u(B)):a(B,O)}return function(B,O,Y,W){try{rl=0;var fe=et(B,O,Y,W);return xo=null,fe}catch(ue){if(ue===_o||ue===si)throw ue;var qe=rn(29,ue,null,B.mode);return qe.lanes=W,qe.return=B,qe}finally{}}}var Za=sd(!0),rd=sd(!1),fa=!1;function Qs(e){e.updateQueue={baseState:e.memoizedState,firstBaseUpdate:null,lastBaseUpdate:null,shared:{pending:null,lanes:0,hiddenCallbacks:null},callbacks:null}}function Ks(e,n){e=e.updateQueue,n.updateQueue===e&&(n.updateQueue={baseState:e.baseState,firstBaseUpdate:e.firstBaseUpdate,lastBaseUpdate:e.lastBaseUpdate,shared:e.shared,callbacks:null})}function ma(e){return{lane:e,tag:0,payload:null,callback:null,next:null}}function ga(e,n,a){var o=e.updateQueue;if(o===null)return null;if(o=o.shared,(Xe&2)!==0){var l=o.pending;return l===null?n.next=n:(n.next=l.next,l.next=n),o.pending=n,n=ti(e),Ju(e,null,a),n}return ei(e,o,n,a),ti(e)}function ul(e,n,a){if(n=n.updateQueue,n!==null&&(n=n.shared,(a&4194048)!==0)){var o=n.lanes;o&=e.pendingLanes,a|=o,n.lanes=a,Ln(e,a)}}function $s(e,n){var a=e.updateQueue,o=e.alternate;if(o!==null&&(o=o.updateQueue,a===o)){var l=null,i=null;if(a=a.firstBaseUpdate,a!==null){do{var u={lane:a.lane,tag:a.tag,payload:a.payload,callback:null,next:null};i===null?l=i=u:i=i.next=u,a=a.next}while(a!==null);i===null?l=i=n:i=i.next=n}else l=i=n;a={baseState:o.baseState,firstBaseUpdate:l,lastBaseUpdate:i,shared:o.shared,callbacks:o.callbacks},e.updateQueue=a;return}e=a.lastBaseUpdate,e===null?a.firstBaseUpdate=n:e.next=n,a.lastBaseUpdate=n}var Ps=!1;function dl(){if(Ps){var e=vo;if(e!==null)throw e}}function pl(e,n,a,o){Ps=!1;var l=e.updateQueue;fa=!1;var i=l.firstBaseUpdate,u=l.lastBaseUpdate,m=l.shared.pending;if(m!==null){l.shared.pending=null;var A=m,q=A.next;A.next=null,u===null?i=q:u.next=q,u=A;var K=e.alternate;K!==null&&(K=K.updateQueue,m=K.lastBaseUpdate,m!==u&&(m===null?K.firstBaseUpdate=q:m.next=q,K.lastBaseUpdate=A))}if(i!==null){var F=l.baseState;u=0,K=q=A=null,m=i;do{var I=m.lane&-536870913,J=I!==m.lane;if(J?(Be&I)===I:(o&I)===I){I!==0&&I===bo&&(Ps=!0),K!==null&&(K=K.next={lane:0,tag:m.tag,payload:m.payload,callback:null,next:null});e:{var ce=e,be=m;I=n;var et=a;switch(be.tag){case 1:if(ce=be.payload,typeof ce=="function"){F=ce.call(et,F,I);break e}F=ce;break e;case 3:ce.flags=ce.flags&-65537|128;case 0:if(ce=be.payload,I=typeof ce=="function"?ce.call(et,F,I):ce,I==null)break e;F=v({},F,I);break e;case 2:fa=!0}}I=m.callback,I!==null&&(e.flags|=64,J&&(e.flags|=8192),J=l.callbacks,J===null?l.callbacks=[I]:J.push(I))}else J={lane:I,tag:m.tag,payload:m.payload,callback:m.callback,next:null},K===null?(q=K=J,A=F):K=K.next=J,u|=I;if(m=m.next,m===null){if(m=l.shared.pending,m===null)break;J=m,m=J.next,J.next=null,l.lastBaseUpdate=J,l.shared.pending=null}}while(!0);K===null&&(A=F),l.baseState=A,l.firstBaseUpdate=q,l.lastBaseUpdate=K,i===null&&(l.shared.lanes=0),xa|=u,e.lanes=u,e.memoizedState=F}}function cd(e,n){if(typeof e!="function")throw Error(c(191,e));e.call(n)}function ud(e,n){var a=e.callbacks;if(a!==null)for(e.callbacks=null,e=0;e<a.length;e++)cd(a[e],n)}var So=le(null),di=le(0);function dd(e,n){e=aa,ie(di,e),ie(So,n),aa=e|n.baseLanes}function Ws(){ie(di,aa),ie(So,So.current)}function Fs(){aa=di.current,re(So),re(di)}var cn=le(null),Nn=null;function ya(e){var n=e.alternate;ie(mt,mt.current&1),ie(cn,e),Nn===null&&(n===null||So.current!==null||n.memoizedState!==null)&&(Nn=e)}function er(e){ie(mt,mt.current),ie(cn,e),Nn===null&&(Nn=e)}function pd(e){e.tag===22?(ie(mt,mt.current),ie(cn,e),Nn===null&&(Nn=e)):ba()}function ba(){ie(mt,mt.current),ie(cn,cn.current)}function un(e){re(cn),Nn===e&&(Nn=null),re(mt)}var mt=le(0);function pi(e){for(var n=e;n!==null;){if(n.tag===13){var a=n.memoizedState;if(a!==null&&(a=a.dehydrated,a===null||ic(a)||sc(a)))return n}else if(n.tag===19&&(n.memoizedProps.revealOrder==="forwards"||n.memoizedProps.revealOrder==="backwards"||n.memoizedProps.revealOrder==="unstable_legacy-backwards"||n.memoizedProps.revealOrder==="together")){if((n.flags&128)!==0)return n}else if(n.child!==null){n.child.return=n,n=n.child;continue}if(n===e)break;for(;n.sibling===null;){if(n.return===null||n.return===e)return null;n=n.return}n.sibling.return=n.return,n=n.sibling}return null}var Kn=0,Te=null,We=null,xt=null,hi=!1,wo=!1,Qa=!1,fi=0,hl=0,Eo=null,ng=0;function dt(){throw Error(c(321))}function tr(e,n){if(n===null)return!1;for(var a=0;a<n.length&&a<e.length;a++)if(!sn(e[a],n[a]))return!1;return!0}function nr(e,n,a,o,l,i){return Kn=i,Te=n,n.memoizedState=null,n.updateQueue=null,n.lanes=0,k.H=e===null||e.memoizedState===null?Kd:yr,Qa=!1,i=a(o,l),Qa=!1,wo&&(i=fd(n,a,o,l)),hd(e),i}function hd(e){k.H=gl;var n=We!==null&&We.next!==null;if(Kn=0,xt=We=Te=null,hi=!1,hl=0,Eo=null,n)throw Error(c(300));e===null||St||(e=e.dependencies,e!==null&&oi(e)&&(St=!0))}function fd(e,n,a,o){Te=e;var l=0;do{if(wo&&(Eo=null),hl=0,wo=!1,25<=l)throw Error(c(301));if(l+=1,xt=We=null,e.updateQueue!=null){var i=e.updateQueue;i.lastEffect=null,i.events=null,i.stores=null,i.memoCache!=null&&(i.memoCache.index=0)}k.H=$d,i=n(a,o)}while(wo);return i}function ag(){var e=k.H,n=e.useState()[0];return n=typeof n.then=="function"?fl(n):n,e=e.useState()[0],(We!==null?We.memoizedState:null)!==e&&(Te.flags|=1024),n}function ar(){var e=fi!==0;return fi=0,e}function or(e,n,a){n.updateQueue=e.updateQueue,n.flags&=-2053,e.lanes&=~a}function lr(e){if(hi){for(e=e.memoizedState;e!==null;){var n=e.queue;n!==null&&(n.pending=null),e=e.next}hi=!1}Kn=0,xt=We=Te=null,wo=!1,hl=fi=0,Eo=null}function Qt(){var e={memoizedState:null,baseState:null,baseQueue:null,queue:null,next:null};return xt===null?Te.memoizedState=xt=e:xt=xt.next=e,xt}function gt(){if(We===null){var e=Te.alternate;e=e!==null?e.memoizedState:null}else e=We.next;var n=xt===null?Te.memoizedState:xt.next;if(n!==null)xt=n,We=e;else{if(e===null)throw Te.alternate===null?Error(c(467)):Error(c(310));We=e,e={memoizedState:We.memoizedState,baseState:We.baseState,baseQueue:We.baseQueue,queue:We.queue,next:null},xt===null?Te.memoizedState=xt=e:xt=xt.next=e}return xt}function mi(){return{lastEffect:null,events:null,stores:null,memoCache:null}}function fl(e){var n=hl;return hl+=1,Eo===null&&(Eo=[]),e=od(Eo,e,n),n=Te,(xt===null?n.memoizedState:xt.next)===null&&(n=n.alternate,k.H=n===null||n.memoizedState===null?Kd:yr),e}function gi(e){if(e!==null&&typeof e=="object"){if(typeof e.then=="function")return fl(e);if(e.$$typeof===j)return Lt(e)}throw Error(c(438,String(e)))}function ir(e){var n=null,a=Te.updateQueue;if(a!==null&&(n=a.memoCache),n==null){var o=Te.alternate;o!==null&&(o=o.updateQueue,o!==null&&(o=o.memoCache,o!=null&&(n={data:o.data.map(function(l){return l.slice()}),index:0})))}if(n==null&&(n={data:[],index:0}),a===null&&(a=mi(),Te.updateQueue=a),a.memoCache=n,a=n.data[n.index],a===void 0)for(a=n.data[n.index]=Array(e),o=0;o<e;o++)a[o]=z;return n.index++,a}function $n(e,n){return typeof n=="function"?n(e):n}function yi(e){var n=gt();return sr(n,We,e)}function sr(e,n,a){var o=e.queue;if(o===null)throw Error(c(311));o.lastRenderedReducer=a;var l=e.baseQueue,i=o.pending;if(i!==null){if(l!==null){var u=l.next;l.next=i.next,i.next=u}n.baseQueue=l=i,o.pending=null}if(i=e.baseState,l===null)e.memoizedState=i;else{n=l.next;var m=u=null,A=null,q=n,K=!1;do{var F=q.lane&-536870913;if(F!==q.lane?(Be&F)===F:(Kn&F)===F){var I=q.revertLane;if(I===0)A!==null&&(A=A.next={lane:0,revertLane:0,gesture:null,action:q.action,hasEagerState:q.hasEagerState,eagerState:q.eagerState,next:null}),F===bo&&(K=!0);else if((Kn&I)===I){q=q.next,I===bo&&(K=!0);continue}else F={lane:0,revertLane:q.revertLane,gesture:null,action:q.action,hasEagerState:q.hasEagerState,eagerState:q.eagerState,next:null},A===null?(m=A=F,u=i):A=A.next=F,Te.lanes|=I,xa|=I;F=q.action,Qa&&a(i,F),i=q.hasEagerState?q.eagerState:a(i,F)}else I={lane:F,revertLane:q.revertLane,gesture:q.gesture,action:q.action,hasEagerState:q.hasEagerState,eagerState:q.eagerState,next:null},A===null?(m=A=I,u=i):A=A.next=I,Te.lanes|=F,xa|=F;q=q.next}while(q!==null&&q!==n);if(A===null?u=i:A.next=m,!sn(i,e.memoizedState)&&(St=!0,K&&(a=vo,a!==null)))throw a;e.memoizedState=i,e.baseState=u,e.baseQueue=A,o.lastRenderedState=i}return l===null&&(o.lanes=0),[e.memoizedState,o.dispatch]}function rr(e){var n=gt(),a=n.queue;if(a===null)throw Error(c(311));a.lastRenderedReducer=e;var o=a.dispatch,l=a.pending,i=n.memoizedState;if(l!==null){a.pending=null;var u=l=l.next;do i=e(i,u.action),u=u.next;while(u!==l);sn(i,n.memoizedState)||(St=!0),n.memoizedState=i,n.baseQueue===null&&(n.baseState=i),a.lastRenderedState=i}return[i,o]}function md(e,n,a){var o=Te,l=gt(),i=He;if(i){if(a===void 0)throw Error(c(407));a=a()}else a=n();var u=!sn((We||l).memoizedState,a);if(u&&(l.memoizedState=a,St=!0),l=l.queue,dr(bd.bind(null,o,l,e),[e]),l.getSnapshot!==n||u||xt!==null&&xt.memoizedState.tag&1){if(o.flags|=2048,To(9,{destroy:void 0},yd.bind(null,o,l,a,n),null),tt===null)throw Error(c(349));i||(Kn&127)!==0||gd(o,n,a)}return a}function gd(e,n,a){e.flags|=16384,e={getSnapshot:n,value:a},n=Te.updateQueue,n===null?(n=mi(),Te.updateQueue=n,n.stores=[e]):(a=n.stores,a===null?n.stores=[e]:a.push(e))}function yd(e,n,a,o){n.value=a,n.getSnapshot=o,vd(n)&&_d(e)}function bd(e,n,a){return a(function(){vd(n)&&_d(e)})}function vd(e){var n=e.getSnapshot;e=e.value;try{var a=n();return!sn(e,a)}catch{return!0}}function _d(e){var n=Ua(e,2);n!==null&&nn(n,e,2)}function cr(e){var n=Qt();if(typeof e=="function"){var a=e;if(e=a(),Qa){At(!0);try{a()}finally{At(!1)}}}return n.memoizedState=n.baseState=e,n.queue={pending:null,lanes:0,dispatch:null,lastRenderedReducer:$n,lastRenderedState:e},n}function xd(e,n,a,o){return e.baseState=a,sr(e,We,typeof o=="function"?o:$n)}function og(e,n,a,o,l){if(_i(e))throw Error(c(485));if(e=n.action,e!==null){var i={payload:l,action:e,next:null,isTransition:!0,status:"pending",value:null,reason:null,listeners:[],then:function(u){i.listeners.push(u)}};k.T!==null?a(!0):i.isTransition=!1,o(i),a=n.pending,a===null?(i.next=n.pending=i,Sd(n,i)):(i.next=a.next,n.pending=a.next=i)}}function Sd(e,n){var a=n.action,o=n.payload,l=e.state;if(n.isTransition){var i=k.T,u={};k.T=u;try{var m=a(l,o),A=k.S;A!==null&&A(u,m),wd(e,n,m)}catch(q){ur(e,n,q)}finally{i!==null&&u.types!==null&&(i.types=u.types),k.T=i}}else try{i=a(l,o),wd(e,n,i)}catch(q){ur(e,n,q)}}function wd(e,n,a){a!==null&&typeof a=="object"&&typeof a.then=="function"?a.then(function(o){Ed(e,n,o)},function(o){return ur(e,n,o)}):Ed(e,n,a)}function Ed(e,n,a){n.status="fulfilled",n.value=a,Td(n),e.state=a,n=e.pending,n!==null&&(a=n.next,a===n?e.pending=null:(a=a.next,n.next=a,Sd(e,a)))}function ur(e,n,a){var o=e.pending;if(e.pending=null,o!==null){o=o.next;do n.status="rejected",n.reason=a,Td(n),n=n.next;while(n!==o)}e.action=null}function Td(e){e=e.listeners;for(var n=0;n<e.length;n++)(0,e[n])()}function Nd(e,n){return n}function Ad(e,n){if(He){var a=tt.formState;if(a!==null){e:{var o=Te;if(He){if(it){t:{for(var l=it,i=Tn;l.nodeType!==8;){if(!i){l=null;break t}if(l=An(l.nextSibling),l===null){l=null;break t}}i=l.data,l=i==="F!"||i==="F"?l:null}if(l){it=An(l.nextSibling),o=l.data==="F!";break e}}pa(o)}o=!1}o&&(n=a[0])}}return a=Qt(),a.memoizedState=a.baseState=n,o={pending:null,lanes:0,dispatch:null,lastRenderedReducer:Nd,lastRenderedState:n},a.queue=o,a=Vd.bind(null,Te,o),o.dispatch=a,o=cr(!1),i=gr.bind(null,Te,!1,o.queue),o=Qt(),l={state:n,dispatch:null,action:e,pending:null},o.queue=l,a=og.bind(null,Te,l,i,a),l.dispatch=a,o.memoizedState=e,[n,a,!1]}function Cd(e){var n=gt();return jd(n,We,e)}function jd(e,n,a){if(n=sr(e,n,Nd)[0],e=yi($n)[0],typeof n=="object"&&n!==null&&typeof n.then=="function")try{var o=fl(n)}catch(u){throw u===_o?si:u}else o=n;n=gt();var l=n.queue,i=l.dispatch;return a!==n.memoizedState&&(Te.flags|=2048,To(9,{destroy:void 0},lg.bind(null,l,a),null)),[o,i,e]}function lg(e,n){e.action=n}function Dd(e){var n=gt(),a=We;if(a!==null)return jd(n,a,e);gt(),n=n.memoizedState,a=gt();var o=a.queue.dispatch;return a.memoizedState=e,[n,o,!1]}function To(e,n,a,o){return e={tag:e,create:a,deps:o,inst:n,next:null},n=Te.updateQueue,n===null&&(n=mi(),Te.updateQueue=n),a=n.lastEffect,a===null?n.lastEffect=e.next=e:(o=a.next,a.next=e,e.next=o,n.lastEffect=e),e}function Md(){return gt().memoizedState}function bi(e,n,a,o){var l=Qt();Te.flags|=e,l.memoizedState=To(1|n,{destroy:void 0},a,o===void 0?null:o)}function vi(e,n,a,o){var l=gt();o=o===void 0?null:o;var i=l.memoizedState.inst;We!==null&&o!==null&&tr(o,We.memoizedState.deps)?l.memoizedState=To(n,i,a,o):(Te.flags|=e,l.memoizedState=To(1|n,i,a,o))}function kd(e,n){bi(8390656,8,e,n)}function dr(e,n){vi(2048,8,e,n)}function ig(e){Te.flags|=4;var n=Te.updateQueue;if(n===null)n=mi(),Te.updateQueue=n,n.events=[e];else{var a=n.events;a===null?n.events=[e]:a.push(e)}}function Od(e){var n=gt().memoizedState;return ig({ref:n,nextImpl:e}),function(){if((Xe&2)!==0)throw Error(c(440));return n.impl.apply(void 0,arguments)}}function Rd(e,n){return vi(4,2,e,n)}function zd(e,n){return vi(4,4,e,n)}function Bd(e,n){if(typeof n=="function"){e=e();var a=n(e);return function(){typeof a=="function"?a():n(null)}}if(n!=null)return e=e(),n.current=e,function(){n.current=null}}function Gd(e,n,a){a=a!=null?a.concat([e]):null,vi(4,4,Bd.bind(null,n,e),a)}function pr(){}function Hd(e,n){var a=gt();n=n===void 0?null:n;var o=a.memoizedState;return n!==null&&tr(n,o[1])?o[0]:(a.memoizedState=[e,n],e)}function Ud(e,n){var a=gt();n=n===void 0?null:n;var o=a.memoizedState;if(n!==null&&tr(n,o[1]))return o[0];if(o=e(),Qa){At(!0);try{e()}finally{At(!1)}}return a.memoizedState=[o,n],o}function hr(e,n,a){return a===void 0||(Kn&1073741824)!==0&&(Be&261930)===0?e.memoizedState=n:(e.memoizedState=a,e=Lp(),Te.lanes|=e,xa|=e,a)}function Ld(e,n,a,o){return sn(a,n)?a:So.current!==null?(e=hr(e,a,o),sn(e,n)||(St=!0),e):(Kn&42)===0||(Kn&1073741824)!==0&&(Be&261930)===0?(St=!0,e.memoizedState=a):(e=Lp(),Te.lanes|=e,xa|=e,n)}function Yd(e,n,a,o,l){var i=R.p;R.p=i!==0&&8>i?i:8;var u=k.T,m={};k.T=m,gr(e,!1,n,a);try{var A=l(),q=k.S;if(q!==null&&q(m,A),A!==null&&typeof A=="object"&&typeof A.then=="function"){var K=tg(A,o);ml(e,n,K,hn(e))}else ml(e,n,o,hn(e))}catch(F){ml(e,n,{then:function(){},status:"rejected",reason:F},hn())}finally{R.p=i,u!==null&&m.types!==null&&(u.types=m.types),k.T=u}}function sg(){}function fr(e,n,a,o){if(e.tag!==5)throw Error(c(476));var l=qd(e).queue;Yd(e,l,n,$,a===null?sg:function(){return Id(e),a(o)})}function qd(e){var n=e.memoizedState;if(n!==null)return n;n={memoizedState:$,baseState:$,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:$n,lastRenderedState:$},next:null};var a={};return n.next={memoizedState:a,baseState:a,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:$n,lastRenderedState:a},next:null},e.memoizedState=n,e=e.alternate,e!==null&&(e.memoizedState=n),n}function Id(e){var n=qd(e);n.next===null&&(n=e.alternate.memoizedState),ml(e,n.next.queue,{},hn())}function mr(){return Lt(kl)}function Xd(){return gt().memoizedState}function Jd(){return gt().memoizedState}function rg(e){for(var n=e.return;n!==null;){switch(n.tag){case 24:case 3:var a=hn();e=ma(a);var o=ga(n,e,a);o!==null&&(nn(o,n,a),ul(o,n,a)),n={cache:Xs()},e.payload=n;return}n=n.return}}function cg(e,n,a){var o=hn();a={lane:o,revertLane:0,gesture:null,action:a,hasEagerState:!1,eagerState:null,next:null},_i(e)?Zd(n,a):(a=Os(e,n,a,o),a!==null&&(nn(a,e,o),Qd(a,n,o)))}function Vd(e,n,a){var o=hn();ml(e,n,a,o)}function ml(e,n,a,o){var l={lane:o,revertLane:0,gesture:null,action:a,hasEagerState:!1,eagerState:null,next:null};if(_i(e))Zd(n,l);else{var i=e.alternate;if(e.lanes===0&&(i===null||i.lanes===0)&&(i=n.lastRenderedReducer,i!==null))try{var u=n.lastRenderedState,m=i(u,a);if(l.hasEagerState=!0,l.eagerState=m,sn(m,u))return ei(e,n,l,0),tt===null&&Fl(),!1}catch{}finally{}if(a=Os(e,n,l,o),a!==null)return nn(a,e,o),Qd(a,n,o),!0}return!1}function gr(e,n,a,o){if(o={lane:2,revertLane:Qr(),gesture:null,action:o,hasEagerState:!1,eagerState:null,next:null},_i(e)){if(n)throw Error(c(479))}else n=Os(e,a,o,2),n!==null&&nn(n,e,2)}function _i(e){var n=e.alternate;return e===Te||n!==null&&n===Te}function Zd(e,n){wo=hi=!0;var a=e.pending;a===null?n.next=n:(n.next=a.next,a.next=n),e.pending=n}function Qd(e,n,a){if((a&4194048)!==0){var o=n.lanes;o&=e.pendingLanes,a|=o,n.lanes=a,Ln(e,a)}}var gl={readContext:Lt,use:gi,useCallback:dt,useContext:dt,useEffect:dt,useImperativeHandle:dt,useLayoutEffect:dt,useInsertionEffect:dt,useMemo:dt,useReducer:dt,useRef:dt,useState:dt,useDebugValue:dt,useDeferredValue:dt,useTransition:dt,useSyncExternalStore:dt,useId:dt,useHostTransitionStatus:dt,useFormState:dt,useActionState:dt,useOptimistic:dt,useMemoCache:dt,useCacheRefresh:dt};gl.useEffectEvent=dt;var Kd={readContext:Lt,use:gi,useCallback:function(e,n){return Qt().memoizedState=[e,n===void 0?null:n],e},useContext:Lt,useEffect:kd,useImperativeHandle:function(e,n,a){a=a!=null?a.concat([e]):null,bi(4194308,4,Bd.bind(null,n,e),a)},useLayoutEffect:function(e,n){return bi(4194308,4,e,n)},useInsertionEffect:function(e,n){bi(4,2,e,n)},useMemo:function(e,n){var a=Qt();n=n===void 0?null:n;var o=e();if(Qa){At(!0);try{e()}finally{At(!1)}}return a.memoizedState=[o,n],o},useReducer:function(e,n,a){var o=Qt();if(a!==void 0){var l=a(n);if(Qa){At(!0);try{a(n)}finally{At(!1)}}}else l=n;return o.memoizedState=o.baseState=l,e={pending:null,lanes:0,dispatch:null,lastRenderedReducer:e,lastRenderedState:l},o.queue=e,e=e.dispatch=cg.bind(null,Te,e),[o.memoizedState,e]},useRef:function(e){var n=Qt();return e={current:e},n.memoizedState=e},useState:function(e){e=cr(e);var n=e.queue,a=Vd.bind(null,Te,n);return n.dispatch=a,[e.memoizedState,a]},useDebugValue:pr,useDeferredValue:function(e,n){var a=Qt();return hr(a,e,n)},useTransition:function(){var e=cr(!1);return e=Yd.bind(null,Te,e.queue,!0,!1),Qt().memoizedState=e,[!1,e]},useSyncExternalStore:function(e,n,a){var o=Te,l=Qt();if(He){if(a===void 0)throw Error(c(407));a=a()}else{if(a=n(),tt===null)throw Error(c(349));(Be&127)!==0||gd(o,n,a)}l.memoizedState=a;var i={value:a,getSnapshot:n};return l.queue=i,kd(bd.bind(null,o,i,e),[e]),o.flags|=2048,To(9,{destroy:void 0},yd.bind(null,o,i,a,n),null),a},useId:function(){var e=Qt(),n=tt.identifierPrefix;if(He){var a=zn,o=Rn;a=(o&~(1<<32-ct(o)-1)).toString(32)+a,n="_"+n+"R_"+a,a=fi++,0<a&&(n+="H"+a.toString(32)),n+="_"}else a=ng++,n="_"+n+"r_"+a.toString(32)+"_";return e.memoizedState=n},useHostTransitionStatus:mr,useFormState:Ad,useActionState:Ad,useOptimistic:function(e){var n=Qt();n.memoizedState=n.baseState=e;var a={pending:null,lanes:0,dispatch:null,lastRenderedReducer:null,lastRenderedState:null};return n.queue=a,n=gr.bind(null,Te,!0,a),a.dispatch=n,[e,n]},useMemoCache:ir,useCacheRefresh:function(){return Qt().memoizedState=rg.bind(null,Te)},useEffectEvent:function(e){var n=Qt(),a={impl:e};return n.memoizedState=a,function(){if((Xe&2)!==0)throw Error(c(440));return a.impl.apply(void 0,arguments)}}},yr={readContext:Lt,use:gi,useCallback:Hd,useContext:Lt,useEffect:dr,useImperativeHandle:Gd,useInsertionEffect:Rd,useLayoutEffect:zd,useMemo:Ud,useReducer:yi,useRef:Md,useState:function(){return yi($n)},useDebugValue:pr,useDeferredValue:function(e,n){var a=gt();return Ld(a,We.memoizedState,e,n)},useTransition:function(){var e=yi($n)[0],n=gt().memoizedState;return[typeof e=="boolean"?e:fl(e),n]},useSyncExternalStore:md,useId:Xd,useHostTransitionStatus:mr,useFormState:Cd,useActionState:Cd,useOptimistic:function(e,n){var a=gt();return xd(a,We,e,n)},useMemoCache:ir,useCacheRefresh:Jd};yr.useEffectEvent=Od;var $d={readContext:Lt,use:gi,useCallback:Hd,useContext:Lt,useEffect:dr,useImperativeHandle:Gd,useInsertionEffect:Rd,useLayoutEffect:zd,useMemo:Ud,useReducer:rr,useRef:Md,useState:function(){return rr($n)},useDebugValue:pr,useDeferredValue:function(e,n){var a=gt();return We===null?hr(a,e,n):Ld(a,We.memoizedState,e,n)},useTransition:function(){var e=rr($n)[0],n=gt().memoizedState;return[typeof e=="boolean"?e:fl(e),n]},useSyncExternalStore:md,useId:Xd,useHostTransitionStatus:mr,useFormState:Dd,useActionState:Dd,useOptimistic:function(e,n){var a=gt();return We!==null?xd(a,We,e,n):(a.baseState=e,[e,a.queue.dispatch])},useMemoCache:ir,useCacheRefresh:Jd};$d.useEffectEvent=Od;function br(e,n,a,o){n=e.memoizedState,a=a(o,n),a=a==null?n:v({},n,a),e.memoizedState=a,e.lanes===0&&(e.updateQueue.baseState=a)}var vr={enqueueSetState:function(e,n,a){e=e._reactInternals;var o=hn(),l=ma(o);l.payload=n,a!=null&&(l.callback=a),n=ga(e,l,o),n!==null&&(nn(n,e,o),ul(n,e,o))},enqueueReplaceState:function(e,n,a){e=e._reactInternals;var o=hn(),l=ma(o);l.tag=1,l.payload=n,a!=null&&(l.callback=a),n=ga(e,l,o),n!==null&&(nn(n,e,o),ul(n,e,o))},enqueueForceUpdate:function(e,n){e=e._reactInternals;var a=hn(),o=ma(a);o.tag=2,n!=null&&(o.callback=n),n=ga(e,o,a),n!==null&&(nn(n,e,a),ul(n,e,a))}};function Pd(e,n,a,o,l,i,u){return e=e.stateNode,typeof e.shouldComponentUpdate=="function"?e.shouldComponentUpdate(o,i,u):n.prototype&&n.prototype.isPureReactComponent?!nl(a,o)||!nl(l,i):!0}function Wd(e,n,a,o){e=n.state,typeof n.componentWillReceiveProps=="function"&&n.componentWillReceiveProps(a,o),typeof n.UNSAFE_componentWillReceiveProps=="function"&&n.UNSAFE_componentWillReceiveProps(a,o),n.state!==e&&vr.enqueueReplaceState(n,n.state,null)}function Ka(e,n){var a=n;if("ref"in n){a={};for(var o in n)o!=="ref"&&(a[o]=n[o])}if(e=e.defaultProps){a===n&&(a=v({},a));for(var l in e)a[l]===void 0&&(a[l]=e[l])}return a}function Fd(e){Wl(e)}function ep(e){console.error(e)}function tp(e){Wl(e)}function xi(e,n){try{var a=e.onUncaughtError;a(n.value,{componentStack:n.stack})}catch(o){setTimeout(function(){throw o})}}function np(e,n,a){try{var o=e.onCaughtError;o(a.value,{componentStack:a.stack,errorBoundary:n.tag===1?n.stateNode:null})}catch(l){setTimeout(function(){throw l})}}function _r(e,n,a){return a=ma(a),a.tag=3,a.payload={element:null},a.callback=function(){xi(e,n)},a}function ap(e){return e=ma(e),e.tag=3,e}function op(e,n,a,o){var l=a.type.getDerivedStateFromError;if(typeof l=="function"){var i=o.value;e.payload=function(){return l(i)},e.callback=function(){np(n,a,o)}}var u=a.stateNode;u!==null&&typeof u.componentDidCatch=="function"&&(e.callback=function(){np(n,a,o),typeof l!="function"&&(Sa===null?Sa=new Set([this]):Sa.add(this));var m=o.stack;this.componentDidCatch(o.value,{componentStack:m!==null?m:""})})}function ug(e,n,a,o,l){if(a.flags|=32768,o!==null&&typeof o=="object"&&typeof o.then=="function"){if(n=a.alternate,n!==null&&yo(n,a,l,!0),a=cn.current,a!==null){switch(a.tag){case 31:case 13:return Nn===null?Oi():a.alternate===null&&pt===0&&(pt=3),a.flags&=-257,a.flags|=65536,a.lanes=l,o===ri?a.flags|=16384:(n=a.updateQueue,n===null?a.updateQueue=new Set([o]):n.add(o),Jr(e,o,l)),!1;case 22:return a.flags|=65536,o===ri?a.flags|=16384:(n=a.updateQueue,n===null?(n={transitions:null,markerInstances:null,retryQueue:new Set([o])},a.updateQueue=n):(a=n.retryQueue,a===null?n.retryQueue=new Set([o]):a.add(o)),Jr(e,o,l)),!1}throw Error(c(435,a.tag))}return Jr(e,o,l),Oi(),!1}if(He)return n=cn.current,n!==null?((n.flags&65536)===0&&(n.flags|=256),n.flags|=65536,n.lanes=l,o!==Us&&(e=Error(c(422),{cause:o}),ll(Sn(e,a)))):(o!==Us&&(n=Error(c(423),{cause:o}),ll(Sn(n,a))),e=e.current.alternate,e.flags|=65536,l&=-l,e.lanes|=l,o=Sn(o,a),l=_r(e.stateNode,o,l),$s(e,l),pt!==4&&(pt=2)),!1;var i=Error(c(520),{cause:o});if(i=Sn(i,a),El===null?El=[i]:El.push(i),pt!==4&&(pt=2),n===null)return!0;o=Sn(o,a),a=n;do{switch(a.tag){case 3:return a.flags|=65536,e=l&-l,a.lanes|=e,e=_r(a.stateNode,o,e),$s(a,e),!1;case 1:if(n=a.type,i=a.stateNode,(a.flags&128)===0&&(typeof n.getDerivedStateFromError=="function"||i!==null&&typeof i.componentDidCatch=="function"&&(Sa===null||!Sa.has(i))))return a.flags|=65536,l&=-l,a.lanes|=l,l=ap(l),op(l,e,a,o),$s(a,l),!1}a=a.return}while(a!==null);return!1}var xr=Error(c(461)),St=!1;function Yt(e,n,a,o){n.child=e===null?rd(n,null,a,o):Za(n,e.child,a,o)}function lp(e,n,a,o,l){a=a.render;var i=n.ref;if("ref"in o){var u={};for(var m in o)m!=="ref"&&(u[m]=o[m])}else u=o;return Ia(n),o=nr(e,n,a,u,i,l),m=ar(),e!==null&&!St?(or(e,n,l),Pn(e,n,l)):(He&&m&&Gs(n),n.flags|=1,Yt(e,n,o,l),n.child)}function ip(e,n,a,o,l){if(e===null){var i=a.type;return typeof i=="function"&&!Rs(i)&&i.defaultProps===void 0&&a.compare===null?(n.tag=15,n.type=i,sp(e,n,i,o,l)):(e=ni(a.type,null,o,n,n.mode,l),e.ref=n.ref,e.return=n,n.child=e)}if(i=e.child,!jr(e,l)){var u=i.memoizedProps;if(a=a.compare,a=a!==null?a:nl,a(u,o)&&e.ref===n.ref)return Pn(e,n,l)}return n.flags|=1,e=Jn(i,o),e.ref=n.ref,e.return=n,n.child=e}function sp(e,n,a,o,l){if(e!==null){var i=e.memoizedProps;if(nl(i,o)&&e.ref===n.ref)if(St=!1,n.pendingProps=o=i,jr(e,l))(e.flags&131072)!==0&&(St=!0);else return n.lanes=e.lanes,Pn(e,n,l)}return Sr(e,n,a,o,l)}function rp(e,n,a,o){var l=o.children,i=e!==null?e.memoizedState:null;if(e===null&&n.stateNode===null&&(n.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),o.mode==="hidden"){if((n.flags&128)!==0){if(i=i!==null?i.baseLanes|a:a,e!==null){for(o=n.child=e.child,l=0;o!==null;)l=l|o.lanes|o.childLanes,o=o.sibling;o=l&~i}else o=0,n.child=null;return cp(e,n,i,a,o)}if((a&536870912)!==0)n.memoizedState={baseLanes:0,cachePool:null},e!==null&&ii(n,i!==null?i.cachePool:null),i!==null?dd(n,i):Ws(),pd(n);else return o=n.lanes=536870912,cp(e,n,i!==null?i.baseLanes|a:a,a,o)}else i!==null?(ii(n,i.cachePool),dd(n,i),ba(),n.memoizedState=null):(e!==null&&ii(n,null),Ws(),ba());return Yt(e,n,l,a),n.child}function yl(e,n){return e!==null&&e.tag===22||n.stateNode!==null||(n.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),n.sibling}function cp(e,n,a,o,l){var i=Vs();return i=i===null?null:{parent:_t._currentValue,pool:i},n.memoizedState={baseLanes:a,cachePool:i},e!==null&&ii(n,null),Ws(),pd(n),e!==null&&yo(e,n,o,!0),n.childLanes=l,null}function Si(e,n){return n=Ei({mode:n.mode,children:n.children},e.mode),n.ref=e.ref,e.child=n,n.return=e,n}function up(e,n,a){return Za(n,e.child,null,a),e=Si(n,n.pendingProps),e.flags|=2,un(n),n.memoizedState=null,e}function dg(e,n,a){var o=n.pendingProps,l=(n.flags&128)!==0;if(n.flags&=-129,e===null){if(He){if(o.mode==="hidden")return e=Si(n,o),n.lanes=536870912,yl(null,e);if(er(n),(e=it)?(e=Sh(e,Tn),e=e!==null&&e.data==="&"?e:null,e!==null&&(n.memoizedState={dehydrated:e,treeContext:ua!==null?{id:Rn,overflow:zn}:null,retryLane:536870912,hydrationErrors:null},a=Zu(e),a.return=n,n.child=a,Ut=n,it=null)):e=null,e===null)throw pa(n);return n.lanes=536870912,null}return Si(n,o)}var i=e.memoizedState;if(i!==null){var u=i.dehydrated;if(er(n),l)if(n.flags&256)n.flags&=-257,n=up(e,n,a);else if(n.memoizedState!==null)n.child=e.child,n.flags|=128,n=null;else throw Error(c(558));else if(St||yo(e,n,a,!1),l=(a&e.childLanes)!==0,St||l){if(o=tt,o!==null&&(u=Yn(o,a),u!==0&&u!==i.retryLane))throw i.retryLane=u,Ua(e,u),nn(o,e,u),xr;Oi(),n=up(e,n,a)}else e=i.treeContext,it=An(u.nextSibling),Ut=n,He=!0,da=null,Tn=!1,e!==null&&$u(n,e),n=Si(n,o),n.flags|=4096;return n}return e=Jn(e.child,{mode:o.mode,children:o.children}),e.ref=n.ref,n.child=e,e.return=n,e}function wi(e,n){var a=n.ref;if(a===null)e!==null&&e.ref!==null&&(n.flags|=4194816);else{if(typeof a!="function"&&typeof a!="object")throw Error(c(284));(e===null||e.ref!==a)&&(n.flags|=4194816)}}function Sr(e,n,a,o,l){return Ia(n),a=nr(e,n,a,o,void 0,l),o=ar(),e!==null&&!St?(or(e,n,l),Pn(e,n,l)):(He&&o&&Gs(n),n.flags|=1,Yt(e,n,a,l),n.child)}function dp(e,n,a,o,l,i){return Ia(n),n.updateQueue=null,a=fd(n,o,a,l),hd(e),o=ar(),e!==null&&!St?(or(e,n,i),Pn(e,n,i)):(He&&o&&Gs(n),n.flags|=1,Yt(e,n,a,i),n.child)}function pp(e,n,a,o,l){if(Ia(n),n.stateNode===null){var i=ho,u=a.contextType;typeof u=="object"&&u!==null&&(i=Lt(u)),i=new a(o,i),n.memoizedState=i.state!==null&&i.state!==void 0?i.state:null,i.updater=vr,n.stateNode=i,i._reactInternals=n,i=n.stateNode,i.props=o,i.state=n.memoizedState,i.refs={},Qs(n),u=a.contextType,i.context=typeof u=="object"&&u!==null?Lt(u):ho,i.state=n.memoizedState,u=a.getDerivedStateFromProps,typeof u=="function"&&(br(n,a,u,o),i.state=n.memoizedState),typeof a.getDerivedStateFromProps=="function"||typeof i.getSnapshotBeforeUpdate=="function"||typeof i.UNSAFE_componentWillMount!="function"&&typeof i.componentWillMount!="function"||(u=i.state,typeof i.componentWillMount=="function"&&i.componentWillMount(),typeof i.UNSAFE_componentWillMount=="function"&&i.UNSAFE_componentWillMount(),u!==i.state&&vr.enqueueReplaceState(i,i.state,null),pl(n,o,i,l),dl(),i.state=n.memoizedState),typeof i.componentDidMount=="function"&&(n.flags|=4194308),o=!0}else if(e===null){i=n.stateNode;var m=n.memoizedProps,A=Ka(a,m);i.props=A;var q=i.context,K=a.contextType;u=ho,typeof K=="object"&&K!==null&&(u=Lt(K));var F=a.getDerivedStateFromProps;K=typeof F=="function"||typeof i.getSnapshotBeforeUpdate=="function",m=n.pendingProps!==m,K||typeof i.UNSAFE_componentWillReceiveProps!="function"&&typeof i.componentWillReceiveProps!="function"||(m||q!==u)&&Wd(n,i,o,u),fa=!1;var I=n.memoizedState;i.state=I,pl(n,o,i,l),dl(),q=n.memoizedState,m||I!==q||fa?(typeof F=="function"&&(br(n,a,F,o),q=n.memoizedState),(A=fa||Pd(n,a,A,o,I,q,u))?(K||typeof i.UNSAFE_componentWillMount!="function"&&typeof i.componentWillMount!="function"||(typeof i.componentWillMount=="function"&&i.componentWillMount(),typeof i.UNSAFE_componentWillMount=="function"&&i.UNSAFE_componentWillMount()),typeof i.componentDidMount=="function"&&(n.flags|=4194308)):(typeof i.componentDidMount=="function"&&(n.flags|=4194308),n.memoizedProps=o,n.memoizedState=q),i.props=o,i.state=q,i.context=u,o=A):(typeof i.componentDidMount=="function"&&(n.flags|=4194308),o=!1)}else{i=n.stateNode,Ks(e,n),u=n.memoizedProps,K=Ka(a,u),i.props=K,F=n.pendingProps,I=i.context,q=a.contextType,A=ho,typeof q=="object"&&q!==null&&(A=Lt(q)),m=a.getDerivedStateFromProps,(q=typeof m=="function"||typeof i.getSnapshotBeforeUpdate=="function")||typeof i.UNSAFE_componentWillReceiveProps!="function"&&typeof i.componentWillReceiveProps!="function"||(u!==F||I!==A)&&Wd(n,i,o,A),fa=!1,I=n.memoizedState,i.state=I,pl(n,o,i,l),dl();var J=n.memoizedState;u!==F||I!==J||fa||e!==null&&e.dependencies!==null&&oi(e.dependencies)?(typeof m=="function"&&(br(n,a,m,o),J=n.memoizedState),(K=fa||Pd(n,a,K,o,I,J,A)||e!==null&&e.dependencies!==null&&oi(e.dependencies))?(q||typeof i.UNSAFE_componentWillUpdate!="function"&&typeof i.componentWillUpdate!="function"||(typeof i.componentWillUpdate=="function"&&i.componentWillUpdate(o,J,A),typeof i.UNSAFE_componentWillUpdate=="function"&&i.UNSAFE_componentWillUpdate(o,J,A)),typeof i.componentDidUpdate=="function"&&(n.flags|=4),typeof i.getSnapshotBeforeUpdate=="function"&&(n.flags|=1024)):(typeof i.componentDidUpdate!="function"||u===e.memoizedProps&&I===e.memoizedState||(n.flags|=4),typeof i.getSnapshotBeforeUpdate!="function"||u===e.memoizedProps&&I===e.memoizedState||(n.flags|=1024),n.memoizedProps=o,n.memoizedState=J),i.props=o,i.state=J,i.context=A,o=K):(typeof i.componentDidUpdate!="function"||u===e.memoizedProps&&I===e.memoizedState||(n.flags|=4),typeof i.getSnapshotBeforeUpdate!="function"||u===e.memoizedProps&&I===e.memoizedState||(n.flags|=1024),o=!1)}return i=o,wi(e,n),o=(n.flags&128)!==0,i||o?(i=n.stateNode,a=o&&typeof a.getDerivedStateFromError!="function"?null:i.render(),n.flags|=1,e!==null&&o?(n.child=Za(n,e.child,null,l),n.child=Za(n,null,a,l)):Yt(e,n,a,l),n.memoizedState=i.state,e=n.child):e=Pn(e,n,l),e}function hp(e,n,a,o){return Ya(),n.flags|=256,Yt(e,n,a,o),n.child}var wr={dehydrated:null,treeContext:null,retryLane:0,hydrationErrors:null};function Er(e){return{baseLanes:e,cachePool:nd()}}function Tr(e,n,a){return e=e!==null?e.childLanes&~a:0,n&&(e|=pn),e}function fp(e,n,a){var o=n.pendingProps,l=!1,i=(n.flags&128)!==0,u;if((u=i)||(u=e!==null&&e.memoizedState===null?!1:(mt.current&2)!==0),u&&(l=!0,n.flags&=-129),u=(n.flags&32)!==0,n.flags&=-33,e===null){if(He){if(l?ya(n):ba(),(e=it)?(e=Sh(e,Tn),e=e!==null&&e.data!=="&"?e:null,e!==null&&(n.memoizedState={dehydrated:e,treeContext:ua!==null?{id:Rn,overflow:zn}:null,retryLane:536870912,hydrationErrors:null},a=Zu(e),a.return=n,n.child=a,Ut=n,it=null)):e=null,e===null)throw pa(n);return sc(e)?n.lanes=32:n.lanes=536870912,null}var m=o.children;return o=o.fallback,l?(ba(),l=n.mode,m=Ei({mode:"hidden",children:m},l),o=La(o,l,a,null),m.return=n,o.return=n,m.sibling=o,n.child=m,o=n.child,o.memoizedState=Er(a),o.childLanes=Tr(e,u,a),n.memoizedState=wr,yl(null,o)):(ya(n),Nr(n,m))}var A=e.memoizedState;if(A!==null&&(m=A.dehydrated,m!==null)){if(i)n.flags&256?(ya(n),n.flags&=-257,n=Ar(e,n,a)):n.memoizedState!==null?(ba(),n.child=e.child,n.flags|=128,n=null):(ba(),m=o.fallback,l=n.mode,o=Ei({mode:"visible",children:o.children},l),m=La(m,l,a,null),m.flags|=2,o.return=n,m.return=n,o.sibling=m,n.child=o,Za(n,e.child,null,a),o=n.child,o.memoizedState=Er(a),o.childLanes=Tr(e,u,a),n.memoizedState=wr,n=yl(null,o));else if(ya(n),sc(m)){if(u=m.nextSibling&&m.nextSibling.dataset,u)var q=u.dgst;u=q,o=Error(c(419)),o.stack="",o.digest=u,ll({value:o,source:null,stack:null}),n=Ar(e,n,a)}else if(St||yo(e,n,a,!1),u=(a&e.childLanes)!==0,St||u){if(u=tt,u!==null&&(o=Yn(u,a),o!==0&&o!==A.retryLane))throw A.retryLane=o,Ua(e,o),nn(u,e,o),xr;ic(m)||Oi(),n=Ar(e,n,a)}else ic(m)?(n.flags|=192,n.child=e.child,n=null):(e=A.treeContext,it=An(m.nextSibling),Ut=n,He=!0,da=null,Tn=!1,e!==null&&$u(n,e),n=Nr(n,o.children),n.flags|=4096);return n}return l?(ba(),m=o.fallback,l=n.mode,A=e.child,q=A.sibling,o=Jn(A,{mode:"hidden",children:o.children}),o.subtreeFlags=A.subtreeFlags&65011712,q!==null?m=Jn(q,m):(m=La(m,l,a,null),m.flags|=2),m.return=n,o.return=n,o.sibling=m,n.child=o,yl(null,o),o=n.child,m=e.child.memoizedState,m===null?m=Er(a):(l=m.cachePool,l!==null?(A=_t._currentValue,l=l.parent!==A?{parent:A,pool:A}:l):l=nd(),m={baseLanes:m.baseLanes|a,cachePool:l}),o.memoizedState=m,o.childLanes=Tr(e,u,a),n.memoizedState=wr,yl(e.child,o)):(ya(n),a=e.child,e=a.sibling,a=Jn(a,{mode:"visible",children:o.children}),a.return=n,a.sibling=null,e!==null&&(u=n.deletions,u===null?(n.deletions=[e],n.flags|=16):u.push(e)),n.child=a,n.memoizedState=null,a)}function Nr(e,n){return n=Ei({mode:"visible",children:n},e.mode),n.return=e,e.child=n}function Ei(e,n){return e=rn(22,e,null,n),e.lanes=0,e}function Ar(e,n,a){return Za(n,e.child,null,a),e=Nr(n,n.pendingProps.children),e.flags|=2,n.memoizedState=null,e}function mp(e,n,a){e.lanes|=n;var o=e.alternate;o!==null&&(o.lanes|=n),qs(e.return,n,a)}function Cr(e,n,a,o,l,i){var u=e.memoizedState;u===null?e.memoizedState={isBackwards:n,rendering:null,renderingStartTime:0,last:o,tail:a,tailMode:l,treeForkCount:i}:(u.isBackwards=n,u.rendering=null,u.renderingStartTime=0,u.last=o,u.tail=a,u.tailMode=l,u.treeForkCount=i)}function gp(e,n,a){var o=n.pendingProps,l=o.revealOrder,i=o.tail;o=o.children;var u=mt.current,m=(u&2)!==0;if(m?(u=u&1|2,n.flags|=128):u&=1,ie(mt,u),Yt(e,n,o,a),o=He?ol:0,!m&&e!==null&&(e.flags&128)!==0)e:for(e=n.child;e!==null;){if(e.tag===13)e.memoizedState!==null&&mp(e,a,n);else if(e.tag===19)mp(e,a,n);else if(e.child!==null){e.child.return=e,e=e.child;continue}if(e===n)break e;for(;e.sibling===null;){if(e.return===null||e.return===n)break e;e=e.return}e.sibling.return=e.return,e=e.sibling}switch(l){case"forwards":for(a=n.child,l=null;a!==null;)e=a.alternate,e!==null&&pi(e)===null&&(l=a),a=a.sibling;a=l,a===null?(l=n.child,n.child=null):(l=a.sibling,a.sibling=null),Cr(n,!1,l,a,i,o);break;case"backwards":case"unstable_legacy-backwards":for(a=null,l=n.child,n.child=null;l!==null;){if(e=l.alternate,e!==null&&pi(e)===null){n.child=l;break}e=l.sibling,l.sibling=a,a=l,l=e}Cr(n,!0,a,null,i,o);break;case"together":Cr(n,!1,null,null,void 0,o);break;default:n.memoizedState=null}return n.child}function Pn(e,n,a){if(e!==null&&(n.dependencies=e.dependencies),xa|=n.lanes,(a&n.childLanes)===0)if(e!==null){if(yo(e,n,a,!1),(a&n.childLanes)===0)return null}else return null;if(e!==null&&n.child!==e.child)throw Error(c(153));if(n.child!==null){for(e=n.child,a=Jn(e,e.pendingProps),n.child=a,a.return=n;e.sibling!==null;)e=e.sibling,a=a.sibling=Jn(e,e.pendingProps),a.return=n;a.sibling=null}return n.child}function jr(e,n){return(e.lanes&n)!==0?!0:(e=e.dependencies,!!(e!==null&&oi(e)))}function pg(e,n,a){switch(n.tag){case 3:ze(n,n.stateNode.containerInfo),ha(n,_t,e.memoizedState.cache),Ya();break;case 27:case 5:Je(n);break;case 4:ze(n,n.stateNode.containerInfo);break;case 10:ha(n,n.type,n.memoizedProps.value);break;case 31:if(n.memoizedState!==null)return n.flags|=128,er(n),null;break;case 13:var o=n.memoizedState;if(o!==null)return o.dehydrated!==null?(ya(n),n.flags|=128,null):(a&n.child.childLanes)!==0?fp(e,n,a):(ya(n),e=Pn(e,n,a),e!==null?e.sibling:null);ya(n);break;case 19:var l=(e.flags&128)!==0;if(o=(a&n.childLanes)!==0,o||(yo(e,n,a,!1),o=(a&n.childLanes)!==0),l){if(o)return gp(e,n,a);n.flags|=128}if(l=n.memoizedState,l!==null&&(l.rendering=null,l.tail=null,l.lastEffect=null),ie(mt,mt.current),o)break;return null;case 22:return n.lanes=0,rp(e,n,a,n.pendingProps);case 24:ha(n,_t,e.memoizedState.cache)}return Pn(e,n,a)}function yp(e,n,a){if(e!==null)if(e.memoizedProps!==n.pendingProps)St=!0;else{if(!jr(e,a)&&(n.flags&128)===0)return St=!1,pg(e,n,a);St=(e.flags&131072)!==0}else St=!1,He&&(n.flags&1048576)!==0&&Ku(n,ol,n.index);switch(n.lanes=0,n.tag){case 16:e:{var o=n.pendingProps;if(e=Ja(n.elementType),n.type=e,typeof e=="function")Rs(e)?(o=Ka(e,o),n.tag=1,n=pp(null,n,e,o,a)):(n.tag=0,n=Sr(null,n,e,o,a));else{if(e!=null){var l=e.$$typeof;if(l===L){n.tag=11,n=lp(null,n,e,o,a);break e}else if(l===M){n.tag=14,n=ip(null,n,e,o,a);break e}}throw n=Z(e)||e,Error(c(306,n,""))}}return n;case 0:return Sr(e,n,n.type,n.pendingProps,a);case 1:return o=n.type,l=Ka(o,n.pendingProps),pp(e,n,o,l,a);case 3:e:{if(ze(n,n.stateNode.containerInfo),e===null)throw Error(c(387));o=n.pendingProps;var i=n.memoizedState;l=i.element,Ks(e,n),pl(n,o,null,a);var u=n.memoizedState;if(o=u.cache,ha(n,_t,o),o!==i.cache&&Is(n,[_t],a,!0),dl(),o=u.element,i.isDehydrated)if(i={element:o,isDehydrated:!1,cache:u.cache},n.updateQueue.baseState=i,n.memoizedState=i,n.flags&256){n=hp(e,n,o,a);break e}else if(o!==l){l=Sn(Error(c(424)),n),ll(l),n=hp(e,n,o,a);break e}else{switch(e=n.stateNode.containerInfo,e.nodeType){case 9:e=e.body;break;default:e=e.nodeName==="HTML"?e.ownerDocument.body:e}for(it=An(e.firstChild),Ut=n,He=!0,da=null,Tn=!0,a=rd(n,null,o,a),n.child=a;a;)a.flags=a.flags&-3|4096,a=a.sibling}else{if(Ya(),o===l){n=Pn(e,n,a);break e}Yt(e,n,o,a)}n=n.child}return n;case 26:return wi(e,n),e===null?(a=Ch(n.type,null,n.pendingProps,null))?n.memoizedState=a:He||(a=n.type,e=n.pendingProps,o=Li(se.current).createElement(a),o[Ht]=n,o[$t]=e,qt(o,a,e),Dt(o),n.stateNode=o):n.memoizedState=Ch(n.type,e.memoizedProps,n.pendingProps,e.memoizedState),null;case 27:return Je(n),e===null&&He&&(o=n.stateNode=Th(n.type,n.pendingProps,se.current),Ut=n,Tn=!0,l=it,Na(n.type)?(rc=l,it=An(o.firstChild)):it=l),Yt(e,n,n.pendingProps.children,a),wi(e,n),e===null&&(n.flags|=4194304),n.child;case 5:return e===null&&He&&((l=o=it)&&(o=qg(o,n.type,n.pendingProps,Tn),o!==null?(n.stateNode=o,Ut=n,it=An(o.firstChild),Tn=!1,l=!0):l=!1),l||pa(n)),Je(n),l=n.type,i=n.pendingProps,u=e!==null?e.memoizedProps:null,o=i.children,ac(l,i)?o=null:u!==null&&ac(l,u)&&(n.flags|=32),n.memoizedState!==null&&(l=nr(e,n,ag,null,null,a),kl._currentValue=l),wi(e,n),Yt(e,n,o,a),n.child;case 6:return e===null&&He&&((e=a=it)&&(a=Ig(a,n.pendingProps,Tn),a!==null?(n.stateNode=a,Ut=n,it=null,e=!0):e=!1),e||pa(n)),null;case 13:return fp(e,n,a);case 4:return ze(n,n.stateNode.containerInfo),o=n.pendingProps,e===null?n.child=Za(n,null,o,a):Yt(e,n,o,a),n.child;case 11:return lp(e,n,n.type,n.pendingProps,a);case 7:return Yt(e,n,n.pendingProps,a),n.child;case 8:return Yt(e,n,n.pendingProps.children,a),n.child;case 12:return Yt(e,n,n.pendingProps.children,a),n.child;case 10:return o=n.pendingProps,ha(n,n.type,o.value),Yt(e,n,o.children,a),n.child;case 9:return l=n.type._context,o=n.pendingProps.children,Ia(n),l=Lt(l),o=o(l),n.flags|=1,Yt(e,n,o,a),n.child;case 14:return ip(e,n,n.type,n.pendingProps,a);case 15:return sp(e,n,n.type,n.pendingProps,a);case 19:return gp(e,n,a);case 31:return dg(e,n,a);case 22:return rp(e,n,a,n.pendingProps);case 24:return Ia(n),o=Lt(_t),e===null?(l=Vs(),l===null&&(l=tt,i=Xs(),l.pooledCache=i,i.refCount++,i!==null&&(l.pooledCacheLanes|=a),l=i),n.memoizedState={parent:o,cache:l},Qs(n),ha(n,_t,l)):((e.lanes&a)!==0&&(Ks(e,n),pl(n,null,null,a),dl()),l=e.memoizedState,i=n.memoizedState,l.parent!==o?(l={parent:o,cache:o},n.memoizedState=l,n.lanes===0&&(n.memoizedState=n.updateQueue.baseState=l),ha(n,_t,o)):(o=i.cache,ha(n,_t,o),o!==l.cache&&Is(n,[_t],a,!0))),Yt(e,n,n.pendingProps.children,a),n.child;case 29:throw n.pendingProps}throw Error(c(156,n.tag))}function Wn(e){e.flags|=4}function Dr(e,n,a,o,l){if((n=(e.mode&32)!==0)&&(n=!1),n){if(e.flags|=16777216,(l&335544128)===l)if(e.stateNode.complete)e.flags|=8192;else if(Xp())e.flags|=8192;else throw Va=ri,Zs}else e.flags&=-16777217}function bp(e,n){if(n.type!=="stylesheet"||(n.state.loading&4)!==0)e.flags&=-16777217;else if(e.flags|=16777216,!Oh(n))if(Xp())e.flags|=8192;else throw Va=ri,Zs}function Ti(e,n){n!==null&&(e.flags|=4),e.flags&16384&&(n=e.tag!==22?ge():536870912,e.lanes|=n,jo|=n)}function bl(e,n){if(!He)switch(e.tailMode){case"hidden":n=e.tail;for(var a=null;n!==null;)n.alternate!==null&&(a=n),n=n.sibling;a===null?e.tail=null:a.sibling=null;break;case"collapsed":a=e.tail;for(var o=null;a!==null;)a.alternate!==null&&(o=a),a=a.sibling;o===null?n||e.tail===null?e.tail=null:e.tail.sibling=null:o.sibling=null}}function st(e){var n=e.alternate!==null&&e.alternate.child===e.child,a=0,o=0;if(n)for(var l=e.child;l!==null;)a|=l.lanes|l.childLanes,o|=l.subtreeFlags&65011712,o|=l.flags&65011712,l.return=e,l=l.sibling;else for(l=e.child;l!==null;)a|=l.lanes|l.childLanes,o|=l.subtreeFlags,o|=l.flags,l.return=e,l=l.sibling;return e.subtreeFlags|=o,e.childLanes=a,n}function hg(e,n,a){var o=n.pendingProps;switch(Hs(n),n.tag){case 16:case 15:case 0:case 11:case 7:case 8:case 12:case 9:case 14:return st(n),null;case 1:return st(n),null;case 3:return a=n.stateNode,o=null,e!==null&&(o=e.memoizedState.cache),n.memoizedState.cache!==o&&(n.flags|=2048),Qn(_t),_e(),a.pendingContext&&(a.context=a.pendingContext,a.pendingContext=null),(e===null||e.child===null)&&(go(n)?Wn(n):e===null||e.memoizedState.isDehydrated&&(n.flags&256)===0||(n.flags|=1024,Ls())),st(n),null;case 26:var l=n.type,i=n.memoizedState;return e===null?(Wn(n),i!==null?(st(n),bp(n,i)):(st(n),Dr(n,l,null,o,a))):i?i!==e.memoizedState?(Wn(n),st(n),bp(n,i)):(st(n),n.flags&=-16777217):(e=e.memoizedProps,e!==o&&Wn(n),st(n),Dr(n,l,e,o,a)),null;case 27:if(je(n),a=se.current,l=n.type,e!==null&&n.stateNode!=null)e.memoizedProps!==o&&Wn(n);else{if(!o){if(n.stateNode===null)throw Error(c(166));return st(n),null}e=de.current,go(n)?Pu(n):(e=Th(l,o,a),n.stateNode=e,Wn(n))}return st(n),null;case 5:if(je(n),l=n.type,e!==null&&n.stateNode!=null)e.memoizedProps!==o&&Wn(n);else{if(!o){if(n.stateNode===null)throw Error(c(166));return st(n),null}if(i=de.current,go(n))Pu(n);else{var u=Li(se.current);switch(i){case 1:i=u.createElementNS("http://www.w3.org/2000/svg",l);break;case 2:i=u.createElementNS("http://www.w3.org/1998/Math/MathML",l);break;default:switch(l){case"svg":i=u.createElementNS("http://www.w3.org/2000/svg",l);break;case"math":i=u.createElementNS("http://www.w3.org/1998/Math/MathML",l);break;case"script":i=u.createElement("div"),i.innerHTML="<script><\/script>",i=i.removeChild(i.firstChild);break;case"select":i=typeof o.is=="string"?u.createElement("select",{is:o.is}):u.createElement("select"),o.multiple?i.multiple=!0:o.size&&(i.size=o.size);break;default:i=typeof o.is=="string"?u.createElement(l,{is:o.is}):u.createElement(l)}}i[Ht]=n,i[$t]=o;e:for(u=n.child;u!==null;){if(u.tag===5||u.tag===6)i.appendChild(u.stateNode);else if(u.tag!==4&&u.tag!==27&&u.child!==null){u.child.return=u,u=u.child;continue}if(u===n)break e;for(;u.sibling===null;){if(u.return===null||u.return===n)break e;u=u.return}u.sibling.return=u.return,u=u.sibling}n.stateNode=i;e:switch(qt(i,l,o),l){case"button":case"input":case"select":case"textarea":o=!!o.autoFocus;break e;case"img":o=!0;break e;default:o=!1}o&&Wn(n)}}return st(n),Dr(n,n.type,e===null?null:e.memoizedProps,n.pendingProps,a),null;case 6:if(e&&n.stateNode!=null)e.memoizedProps!==o&&Wn(n);else{if(typeof o!="string"&&n.stateNode===null)throw Error(c(166));if(e=se.current,go(n)){if(e=n.stateNode,a=n.memoizedProps,o=null,l=Ut,l!==null)switch(l.tag){case 27:case 5:o=l.memoizedProps}e[Ht]=n,e=!!(e.nodeValue===a||o!==null&&o.suppressHydrationWarning===!0||fh(e.nodeValue,a)),e||pa(n,!0)}else e=Li(e).createTextNode(o),e[Ht]=n,n.stateNode=e}return st(n),null;case 31:if(a=n.memoizedState,e===null||e.memoizedState!==null){if(o=go(n),a!==null){if(e===null){if(!o)throw Error(c(318));if(e=n.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(c(557));e[Ht]=n}else Ya(),(n.flags&128)===0&&(n.memoizedState=null),n.flags|=4;st(n),e=!1}else a=Ls(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=a),e=!0;if(!e)return n.flags&256?(un(n),n):(un(n),null);if((n.flags&128)!==0)throw Error(c(558))}return st(n),null;case 13:if(o=n.memoizedState,e===null||e.memoizedState!==null&&e.memoizedState.dehydrated!==null){if(l=go(n),o!==null&&o.dehydrated!==null){if(e===null){if(!l)throw Error(c(318));if(l=n.memoizedState,l=l!==null?l.dehydrated:null,!l)throw Error(c(317));l[Ht]=n}else Ya(),(n.flags&128)===0&&(n.memoizedState=null),n.flags|=4;st(n),l=!1}else l=Ls(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=l),l=!0;if(!l)return n.flags&256?(un(n),n):(un(n),null)}return un(n),(n.flags&128)!==0?(n.lanes=a,n):(a=o!==null,e=e!==null&&e.memoizedState!==null,a&&(o=n.child,l=null,o.alternate!==null&&o.alternate.memoizedState!==null&&o.alternate.memoizedState.cachePool!==null&&(l=o.alternate.memoizedState.cachePool.pool),i=null,o.memoizedState!==null&&o.memoizedState.cachePool!==null&&(i=o.memoizedState.cachePool.pool),i!==l&&(o.flags|=2048)),a!==e&&a&&(n.child.flags|=8192),Ti(n,n.updateQueue),st(n),null);case 4:return _e(),e===null&&Wr(n.stateNode.containerInfo),st(n),null;case 10:return Qn(n.type),st(n),null;case 19:if(re(mt),o=n.memoizedState,o===null)return st(n),null;if(l=(n.flags&128)!==0,i=o.rendering,i===null)if(l)bl(o,!1);else{if(pt!==0||e!==null&&(e.flags&128)!==0)for(e=n.child;e!==null;){if(i=pi(e),i!==null){for(n.flags|=128,bl(o,!1),e=i.updateQueue,n.updateQueue=e,Ti(n,e),n.subtreeFlags=0,e=a,a=n.child;a!==null;)Vu(a,e),a=a.sibling;return ie(mt,mt.current&1|2),He&&Vn(n,o.treeForkCount),n.child}e=e.sibling}o.tail!==null&&ve()>Di&&(n.flags|=128,l=!0,bl(o,!1),n.lanes=4194304)}else{if(!l)if(e=pi(i),e!==null){if(n.flags|=128,l=!0,e=e.updateQueue,n.updateQueue=e,Ti(n,e),bl(o,!0),o.tail===null&&o.tailMode==="hidden"&&!i.alternate&&!He)return st(n),null}else 2*ve()-o.renderingStartTime>Di&&a!==536870912&&(n.flags|=128,l=!0,bl(o,!1),n.lanes=4194304);o.isBackwards?(i.sibling=n.child,n.child=i):(e=o.last,e!==null?e.sibling=i:n.child=i,o.last=i)}return o.tail!==null?(e=o.tail,o.rendering=e,o.tail=e.sibling,o.renderingStartTime=ve(),e.sibling=null,a=mt.current,ie(mt,l?a&1|2:a&1),He&&Vn(n,o.treeForkCount),e):(st(n),null);case 22:case 23:return un(n),Fs(),o=n.memoizedState!==null,e!==null?e.memoizedState!==null!==o&&(n.flags|=8192):o&&(n.flags|=8192),o?(a&536870912)!==0&&(n.flags&128)===0&&(st(n),n.subtreeFlags&6&&(n.flags|=8192)):st(n),a=n.updateQueue,a!==null&&Ti(n,a.retryQueue),a=null,e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(a=e.memoizedState.cachePool.pool),o=null,n.memoizedState!==null&&n.memoizedState.cachePool!==null&&(o=n.memoizedState.cachePool.pool),o!==a&&(n.flags|=2048),e!==null&&re(Xa),null;case 24:return a=null,e!==null&&(a=e.memoizedState.cache),n.memoizedState.cache!==a&&(n.flags|=2048),Qn(_t),st(n),null;case 25:return null;case 30:return null}throw Error(c(156,n.tag))}function fg(e,n){switch(Hs(n),n.tag){case 1:return e=n.flags,e&65536?(n.flags=e&-65537|128,n):null;case 3:return Qn(_t),_e(),e=n.flags,(e&65536)!==0&&(e&128)===0?(n.flags=e&-65537|128,n):null;case 26:case 27:case 5:return je(n),null;case 31:if(n.memoizedState!==null){if(un(n),n.alternate===null)throw Error(c(340));Ya()}return e=n.flags,e&65536?(n.flags=e&-65537|128,n):null;case 13:if(un(n),e=n.memoizedState,e!==null&&e.dehydrated!==null){if(n.alternate===null)throw Error(c(340));Ya()}return e=n.flags,e&65536?(n.flags=e&-65537|128,n):null;case 19:return re(mt),null;case 4:return _e(),null;case 10:return Qn(n.type),null;case 22:case 23:return un(n),Fs(),e!==null&&re(Xa),e=n.flags,e&65536?(n.flags=e&-65537|128,n):null;case 24:return Qn(_t),null;case 25:return null;default:return null}}function vp(e,n){switch(Hs(n),n.tag){case 3:Qn(_t),_e();break;case 26:case 27:case 5:je(n);break;case 4:_e();break;case 31:n.memoizedState!==null&&un(n);break;case 13:un(n);break;case 19:re(mt);break;case 10:Qn(n.type);break;case 22:case 23:un(n),Fs(),e!==null&&re(Xa);break;case 24:Qn(_t)}}function vl(e,n){try{var a=n.updateQueue,o=a!==null?a.lastEffect:null;if(o!==null){var l=o.next;a=l;do{if((a.tag&e)===e){o=void 0;var i=a.create,u=a.inst;o=i(),u.destroy=o}a=a.next}while(a!==l)}}catch(m){Qe(n,n.return,m)}}function va(e,n,a){try{var o=n.updateQueue,l=o!==null?o.lastEffect:null;if(l!==null){var i=l.next;o=i;do{if((o.tag&e)===e){var u=o.inst,m=u.destroy;if(m!==void 0){u.destroy=void 0,l=n;var A=a,q=m;try{q()}catch(K){Qe(l,A,K)}}}o=o.next}while(o!==i)}}catch(K){Qe(n,n.return,K)}}function _p(e){var n=e.updateQueue;if(n!==null){var a=e.stateNode;try{ud(n,a)}catch(o){Qe(e,e.return,o)}}}function xp(e,n,a){a.props=Ka(e.type,e.memoizedProps),a.state=e.memoizedState;try{a.componentWillUnmount()}catch(o){Qe(e,n,o)}}function _l(e,n){try{var a=e.ref;if(a!==null){switch(e.tag){case 26:case 27:case 5:var o=e.stateNode;break;case 30:o=e.stateNode;break;default:o=e.stateNode}typeof a=="function"?e.refCleanup=a(o):a.current=o}}catch(l){Qe(e,n,l)}}function Bn(e,n){var a=e.ref,o=e.refCleanup;if(a!==null)if(typeof o=="function")try{o()}catch(l){Qe(e,n,l)}finally{e.refCleanup=null,e=e.alternate,e!=null&&(e.refCleanup=null)}else if(typeof a=="function")try{a(null)}catch(l){Qe(e,n,l)}else a.current=null}function Sp(e){var n=e.type,a=e.memoizedProps,o=e.stateNode;try{e:switch(n){case"button":case"input":case"select":case"textarea":a.autoFocus&&o.focus();break e;case"img":a.src?o.src=a.src:a.srcSet&&(o.srcset=a.srcSet)}}catch(l){Qe(e,e.return,l)}}function Mr(e,n,a){try{var o=e.stateNode;Bg(o,e.type,a,n),o[$t]=n}catch(l){Qe(e,e.return,l)}}function wp(e){return e.tag===5||e.tag===3||e.tag===26||e.tag===27&&Na(e.type)||e.tag===4}function kr(e){e:for(;;){for(;e.sibling===null;){if(e.return===null||wp(e.return))return null;e=e.return}for(e.sibling.return=e.return,e=e.sibling;e.tag!==5&&e.tag!==6&&e.tag!==18;){if(e.tag===27&&Na(e.type)||e.flags&2||e.child===null||e.tag===4)continue e;e.child.return=e,e=e.child}if(!(e.flags&2))return e.stateNode}}function Or(e,n,a){var o=e.tag;if(o===5||o===6)e=e.stateNode,n?(a.nodeType===9?a.body:a.nodeName==="HTML"?a.ownerDocument.body:a).insertBefore(e,n):(n=a.nodeType===9?a.body:a.nodeName==="HTML"?a.ownerDocument.body:a,n.appendChild(e),a=a._reactRootContainer,a!=null||n.onclick!==null||(n.onclick=In));else if(o!==4&&(o===27&&Na(e.type)&&(a=e.stateNode,n=null),e=e.child,e!==null))for(Or(e,n,a),e=e.sibling;e!==null;)Or(e,n,a),e=e.sibling}function Ni(e,n,a){var o=e.tag;if(o===5||o===6)e=e.stateNode,n?a.insertBefore(e,n):a.appendChild(e);else if(o!==4&&(o===27&&Na(e.type)&&(a=e.stateNode),e=e.child,e!==null))for(Ni(e,n,a),e=e.sibling;e!==null;)Ni(e,n,a),e=e.sibling}function Ep(e){var n=e.stateNode,a=e.memoizedProps;try{for(var o=e.type,l=n.attributes;l.length;)n.removeAttributeNode(l[0]);qt(n,o,a),n[Ht]=e,n[$t]=a}catch(i){Qe(e,e.return,i)}}var Fn=!1,wt=!1,Rr=!1,Tp=typeof WeakSet=="function"?WeakSet:Set,Mt=null;function mg(e,n){if(e=e.containerInfo,tc=Zi,e=Gu(e),As(e)){if("selectionStart"in e)var a={start:e.selectionStart,end:e.selectionEnd};else e:{a=(a=e.ownerDocument)&&a.defaultView||window;var o=a.getSelection&&a.getSelection();if(o&&o.rangeCount!==0){a=o.anchorNode;var l=o.anchorOffset,i=o.focusNode;o=o.focusOffset;try{a.nodeType,i.nodeType}catch{a=null;break e}var u=0,m=-1,A=-1,q=0,K=0,F=e,I=null;t:for(;;){for(var J;F!==a||l!==0&&F.nodeType!==3||(m=u+l),F!==i||o!==0&&F.nodeType!==3||(A=u+o),F.nodeType===3&&(u+=F.nodeValue.length),(J=F.firstChild)!==null;)I=F,F=J;for(;;){if(F===e)break t;if(I===a&&++q===l&&(m=u),I===i&&++K===o&&(A=u),(J=F.nextSibling)!==null)break;F=I,I=F.parentNode}F=J}a=m===-1||A===-1?null:{start:m,end:A}}else a=null}a=a||{start:0,end:0}}else a=null;for(nc={focusedElem:e,selectionRange:a},Zi=!1,Mt=n;Mt!==null;)if(n=Mt,e=n.child,(n.subtreeFlags&1028)!==0&&e!==null)e.return=n,Mt=e;else for(;Mt!==null;){switch(n=Mt,i=n.alternate,e=n.flags,n.tag){case 0:if((e&4)!==0&&(e=n.updateQueue,e=e!==null?e.events:null,e!==null))for(a=0;a<e.length;a++)l=e[a],l.ref.impl=l.nextImpl;break;case 11:case 15:break;case 1:if((e&1024)!==0&&i!==null){e=void 0,a=n,l=i.memoizedProps,i=i.memoizedState,o=a.stateNode;try{var ce=Ka(a.type,l);e=o.getSnapshotBeforeUpdate(ce,i),o.__reactInternalSnapshotBeforeUpdate=e}catch(be){Qe(a,a.return,be)}}break;case 3:if((e&1024)!==0){if(e=n.stateNode.containerInfo,a=e.nodeType,a===9)lc(e);else if(a===1)switch(e.nodeName){case"HEAD":case"HTML":case"BODY":lc(e);break;default:e.textContent=""}}break;case 5:case 26:case 27:case 6:case 4:case 17:break;default:if((e&1024)!==0)throw Error(c(163))}if(e=n.sibling,e!==null){e.return=n.return,Mt=e;break}Mt=n.return}}function Np(e,n,a){var o=a.flags;switch(a.tag){case 0:case 11:case 15:ta(e,a),o&4&&vl(5,a);break;case 1:if(ta(e,a),o&4)if(e=a.stateNode,n===null)try{e.componentDidMount()}catch(u){Qe(a,a.return,u)}else{var l=Ka(a.type,n.memoizedProps);n=n.memoizedState;try{e.componentDidUpdate(l,n,e.__reactInternalSnapshotBeforeUpdate)}catch(u){Qe(a,a.return,u)}}o&64&&_p(a),o&512&&_l(a,a.return);break;case 3:if(ta(e,a),o&64&&(e=a.updateQueue,e!==null)){if(n=null,a.child!==null)switch(a.child.tag){case 27:case 5:n=a.child.stateNode;break;case 1:n=a.child.stateNode}try{ud(e,n)}catch(u){Qe(a,a.return,u)}}break;case 27:n===null&&o&4&&Ep(a);case 26:case 5:ta(e,a),n===null&&o&4&&Sp(a),o&512&&_l(a,a.return);break;case 12:ta(e,a);break;case 31:ta(e,a),o&4&&jp(e,a);break;case 13:ta(e,a),o&4&&Dp(e,a),o&64&&(e=a.memoizedState,e!==null&&(e=e.dehydrated,e!==null&&(a=Eg.bind(null,a),Xg(e,a))));break;case 22:if(o=a.memoizedState!==null||Fn,!o){n=n!==null&&n.memoizedState!==null||wt,l=Fn;var i=wt;Fn=o,(wt=n)&&!i?na(e,a,(a.subtreeFlags&8772)!==0):ta(e,a),Fn=l,wt=i}break;case 30:break;default:ta(e,a)}}function Ap(e){var n=e.alternate;n!==null&&(e.alternate=null,Ap(n)),e.child=null,e.deletions=null,e.sibling=null,e.tag===5&&(n=e.stateNode,n!==null&&us(n)),e.stateNode=null,e.return=null,e.dependencies=null,e.memoizedProps=null,e.memoizedState=null,e.pendingProps=null,e.stateNode=null,e.updateQueue=null}var ut=null,Wt=!1;function ea(e,n,a){for(a=a.child;a!==null;)Cp(e,n,a),a=a.sibling}function Cp(e,n,a){if(Ye&&typeof Ye.onCommitFiberUnmount=="function")try{Ye.onCommitFiberUnmount(ft,a)}catch{}switch(a.tag){case 26:wt||Bn(a,n),ea(e,n,a),a.memoizedState?a.memoizedState.count--:a.stateNode&&(a=a.stateNode,a.parentNode.removeChild(a));break;case 27:wt||Bn(a,n);var o=ut,l=Wt;Na(a.type)&&(ut=a.stateNode,Wt=!1),ea(e,n,a),jl(a.stateNode),ut=o,Wt=l;break;case 5:wt||Bn(a,n);case 6:if(o=ut,l=Wt,ut=null,ea(e,n,a),ut=o,Wt=l,ut!==null)if(Wt)try{(ut.nodeType===9?ut.body:ut.nodeName==="HTML"?ut.ownerDocument.body:ut).removeChild(a.stateNode)}catch(i){Qe(a,n,i)}else try{ut.removeChild(a.stateNode)}catch(i){Qe(a,n,i)}break;case 18:ut!==null&&(Wt?(e=ut,_h(e.nodeType===9?e.body:e.nodeName==="HTML"?e.ownerDocument.body:e,a.stateNode),Go(e)):_h(ut,a.stateNode));break;case 4:o=ut,l=Wt,ut=a.stateNode.containerInfo,Wt=!0,ea(e,n,a),ut=o,Wt=l;break;case 0:case 11:case 14:case 15:va(2,a,n),wt||va(4,a,n),ea(e,n,a);break;case 1:wt||(Bn(a,n),o=a.stateNode,typeof o.componentWillUnmount=="function"&&xp(a,n,o)),ea(e,n,a);break;case 21:ea(e,n,a);break;case 22:wt=(o=wt)||a.memoizedState!==null,ea(e,n,a),wt=o;break;default:ea(e,n,a)}}function jp(e,n){if(n.memoizedState===null&&(e=n.alternate,e!==null&&(e=e.memoizedState,e!==null))){e=e.dehydrated;try{Go(e)}catch(a){Qe(n,n.return,a)}}}function Dp(e,n){if(n.memoizedState===null&&(e=n.alternate,e!==null&&(e=e.memoizedState,e!==null&&(e=e.dehydrated,e!==null))))try{Go(e)}catch(a){Qe(n,n.return,a)}}function gg(e){switch(e.tag){case 31:case 13:case 19:var n=e.stateNode;return n===null&&(n=e.stateNode=new Tp),n;case 22:return e=e.stateNode,n=e._retryCache,n===null&&(n=e._retryCache=new Tp),n;default:throw Error(c(435,e.tag))}}function Ai(e,n){var a=gg(e);n.forEach(function(o){if(!a.has(o)){a.add(o);var l=Tg.bind(null,e,o);o.then(l,l)}})}function Ft(e,n){var a=n.deletions;if(a!==null)for(var o=0;o<a.length;o++){var l=a[o],i=e,u=n,m=u;e:for(;m!==null;){switch(m.tag){case 27:if(Na(m.type)){ut=m.stateNode,Wt=!1;break e}break;case 5:ut=m.stateNode,Wt=!1;break e;case 3:case 4:ut=m.stateNode.containerInfo,Wt=!0;break e}m=m.return}if(ut===null)throw Error(c(160));Cp(i,u,l),ut=null,Wt=!1,i=l.alternate,i!==null&&(i.return=null),l.return=null}if(n.subtreeFlags&13886)for(n=n.child;n!==null;)Mp(n,e),n=n.sibling}var Mn=null;function Mp(e,n){var a=e.alternate,o=e.flags;switch(e.tag){case 0:case 11:case 14:case 15:Ft(n,e),en(e),o&4&&(va(3,e,e.return),vl(3,e),va(5,e,e.return));break;case 1:Ft(n,e),en(e),o&512&&(wt||a===null||Bn(a,a.return)),o&64&&Fn&&(e=e.updateQueue,e!==null&&(o=e.callbacks,o!==null&&(a=e.shared.hiddenCallbacks,e.shared.hiddenCallbacks=a===null?o:a.concat(o))));break;case 26:var l=Mn;if(Ft(n,e),en(e),o&512&&(wt||a===null||Bn(a,a.return)),o&4){var i=a!==null?a.memoizedState:null;if(o=e.memoizedState,a===null)if(o===null)if(e.stateNode===null){e:{o=e.type,a=e.memoizedProps,l=l.ownerDocument||l;t:switch(o){case"title":i=l.getElementsByTagName("title")[0],(!i||i[Zo]||i[Ht]||i.namespaceURI==="http://www.w3.org/2000/svg"||i.hasAttribute("itemprop"))&&(i=l.createElement(o),l.head.insertBefore(i,l.querySelector("head > title"))),qt(i,o,a),i[Ht]=e,Dt(i),o=i;break e;case"link":var u=Mh("link","href",l).get(o+(a.href||""));if(u){for(var m=0;m<u.length;m++)if(i=u[m],i.getAttribute("href")===(a.href==null||a.href===""?null:a.href)&&i.getAttribute("rel")===(a.rel==null?null:a.rel)&&i.getAttribute("title")===(a.title==null?null:a.title)&&i.getAttribute("crossorigin")===(a.crossOrigin==null?null:a.crossOrigin)){u.splice(m,1);break t}}i=l.createElement(o),qt(i,o,a),l.head.appendChild(i);break;case"meta":if(u=Mh("meta","content",l).get(o+(a.content||""))){for(m=0;m<u.length;m++)if(i=u[m],i.getAttribute("content")===(a.content==null?null:""+a.content)&&i.getAttribute("name")===(a.name==null?null:a.name)&&i.getAttribute("property")===(a.property==null?null:a.property)&&i.getAttribute("http-equiv")===(a.httpEquiv==null?null:a.httpEquiv)&&i.getAttribute("charset")===(a.charSet==null?null:a.charSet)){u.splice(m,1);break t}}i=l.createElement(o),qt(i,o,a),l.head.appendChild(i);break;default:throw Error(c(468,o))}i[Ht]=e,Dt(i),o=i}e.stateNode=o}else kh(l,e.type,e.stateNode);else e.stateNode=Dh(l,o,e.memoizedProps);else i!==o?(i===null?a.stateNode!==null&&(a=a.stateNode,a.parentNode.removeChild(a)):i.count--,o===null?kh(l,e.type,e.stateNode):Dh(l,o,e.memoizedProps)):o===null&&e.stateNode!==null&&Mr(e,e.memoizedProps,a.memoizedProps)}break;case 27:Ft(n,e),en(e),o&512&&(wt||a===null||Bn(a,a.return)),a!==null&&o&4&&Mr(e,e.memoizedProps,a.memoizedProps);break;case 5:if(Ft(n,e),en(e),o&512&&(wt||a===null||Bn(a,a.return)),e.flags&32){l=e.stateNode;try{lo(l,"")}catch(ce){Qe(e,e.return,ce)}}o&4&&e.stateNode!=null&&(l=e.memoizedProps,Mr(e,l,a!==null?a.memoizedProps:l)),o&1024&&(Rr=!0);break;case 6:if(Ft(n,e),en(e),o&4){if(e.stateNode===null)throw Error(c(162));o=e.memoizedProps,a=e.stateNode;try{a.nodeValue=o}catch(ce){Qe(e,e.return,ce)}}break;case 3:if(Ii=null,l=Mn,Mn=Yi(n.containerInfo),Ft(n,e),Mn=l,en(e),o&4&&a!==null&&a.memoizedState.isDehydrated)try{Go(n.containerInfo)}catch(ce){Qe(e,e.return,ce)}Rr&&(Rr=!1,kp(e));break;case 4:o=Mn,Mn=Yi(e.stateNode.containerInfo),Ft(n,e),en(e),Mn=o;break;case 12:Ft(n,e),en(e);break;case 31:Ft(n,e),en(e),o&4&&(o=e.updateQueue,o!==null&&(e.updateQueue=null,Ai(e,o)));break;case 13:Ft(n,e),en(e),e.child.flags&8192&&e.memoizedState!==null!=(a!==null&&a.memoizedState!==null)&&(ji=ve()),o&4&&(o=e.updateQueue,o!==null&&(e.updateQueue=null,Ai(e,o)));break;case 22:l=e.memoizedState!==null;var A=a!==null&&a.memoizedState!==null,q=Fn,K=wt;if(Fn=q||l,wt=K||A,Ft(n,e),wt=K,Fn=q,en(e),o&8192)e:for(n=e.stateNode,n._visibility=l?n._visibility&-2:n._visibility|1,l&&(a===null||A||Fn||wt||$a(e)),a=null,n=e;;){if(n.tag===5||n.tag===26){if(a===null){A=a=n;try{if(i=A.stateNode,l)u=i.style,typeof u.setProperty=="function"?u.setProperty("display","none","important"):u.display="none";else{m=A.stateNode;var F=A.memoizedProps.style,I=F!=null&&F.hasOwnProperty("display")?F.display:null;m.style.display=I==null||typeof I=="boolean"?"":(""+I).trim()}}catch(ce){Qe(A,A.return,ce)}}}else if(n.tag===6){if(a===null){A=n;try{A.stateNode.nodeValue=l?"":A.memoizedProps}catch(ce){Qe(A,A.return,ce)}}}else if(n.tag===18){if(a===null){A=n;try{var J=A.stateNode;l?xh(J,!0):xh(A.stateNode,!1)}catch(ce){Qe(A,A.return,ce)}}}else if((n.tag!==22&&n.tag!==23||n.memoizedState===null||n===e)&&n.child!==null){n.child.return=n,n=n.child;continue}if(n===e)break e;for(;n.sibling===null;){if(n.return===null||n.return===e)break e;a===n&&(a=null),n=n.return}a===n&&(a=null),n.sibling.return=n.return,n=n.sibling}o&4&&(o=e.updateQueue,o!==null&&(a=o.retryQueue,a!==null&&(o.retryQueue=null,Ai(e,a))));break;case 19:Ft(n,e),en(e),o&4&&(o=e.updateQueue,o!==null&&(e.updateQueue=null,Ai(e,o)));break;case 30:break;case 21:break;default:Ft(n,e),en(e)}}function en(e){var n=e.flags;if(n&2){try{for(var a,o=e.return;o!==null;){if(wp(o)){a=o;break}o=o.return}if(a==null)throw Error(c(160));switch(a.tag){case 27:var l=a.stateNode,i=kr(e);Ni(e,i,l);break;case 5:var u=a.stateNode;a.flags&32&&(lo(u,""),a.flags&=-33);var m=kr(e);Ni(e,m,u);break;case 3:case 4:var A=a.stateNode.containerInfo,q=kr(e);Or(e,q,A);break;default:throw Error(c(161))}}catch(K){Qe(e,e.return,K)}e.flags&=-3}n&4096&&(e.flags&=-4097)}function kp(e){if(e.subtreeFlags&1024)for(e=e.child;e!==null;){var n=e;kp(n),n.tag===5&&n.flags&1024&&n.stateNode.reset(),e=e.sibling}}function ta(e,n){if(n.subtreeFlags&8772)for(n=n.child;n!==null;)Np(e,n.alternate,n),n=n.sibling}function $a(e){for(e=e.child;e!==null;){var n=e;switch(n.tag){case 0:case 11:case 14:case 15:va(4,n,n.return),$a(n);break;case 1:Bn(n,n.return);var a=n.stateNode;typeof a.componentWillUnmount=="function"&&xp(n,n.return,a),$a(n);break;case 27:jl(n.stateNode);case 26:case 5:Bn(n,n.return),$a(n);break;case 22:n.memoizedState===null&&$a(n);break;case 30:$a(n);break;default:$a(n)}e=e.sibling}}function na(e,n,a){for(a=a&&(n.subtreeFlags&8772)!==0,n=n.child;n!==null;){var o=n.alternate,l=e,i=n,u=i.flags;switch(i.tag){case 0:case 11:case 15:na(l,i,a),vl(4,i);break;case 1:if(na(l,i,a),o=i,l=o.stateNode,typeof l.componentDidMount=="function")try{l.componentDidMount()}catch(q){Qe(o,o.return,q)}if(o=i,l=o.updateQueue,l!==null){var m=o.stateNode;try{var A=l.shared.hiddenCallbacks;if(A!==null)for(l.shared.hiddenCallbacks=null,l=0;l<A.length;l++)cd(A[l],m)}catch(q){Qe(o,o.return,q)}}a&&u&64&&_p(i),_l(i,i.return);break;case 27:Ep(i);case 26:case 5:na(l,i,a),a&&o===null&&u&4&&Sp(i),_l(i,i.return);break;case 12:na(l,i,a);break;case 31:na(l,i,a),a&&u&4&&jp(l,i);break;case 13:na(l,i,a),a&&u&4&&Dp(l,i);break;case 22:i.memoizedState===null&&na(l,i,a),_l(i,i.return);break;case 30:break;default:na(l,i,a)}n=n.sibling}}function zr(e,n){var a=null;e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(a=e.memoizedState.cachePool.pool),e=null,n.memoizedState!==null&&n.memoizedState.cachePool!==null&&(e=n.memoizedState.cachePool.pool),e!==a&&(e!=null&&e.refCount++,a!=null&&il(a))}function Br(e,n){e=null,n.alternate!==null&&(e=n.alternate.memoizedState.cache),n=n.memoizedState.cache,n!==e&&(n.refCount++,e!=null&&il(e))}function kn(e,n,a,o){if(n.subtreeFlags&10256)for(n=n.child;n!==null;)Op(e,n,a,o),n=n.sibling}function Op(e,n,a,o){var l=n.flags;switch(n.tag){case 0:case 11:case 15:kn(e,n,a,o),l&2048&&vl(9,n);break;case 1:kn(e,n,a,o);break;case 3:kn(e,n,a,o),l&2048&&(e=null,n.alternate!==null&&(e=n.alternate.memoizedState.cache),n=n.memoizedState.cache,n!==e&&(n.refCount++,e!=null&&il(e)));break;case 12:if(l&2048){kn(e,n,a,o),e=n.stateNode;try{var i=n.memoizedProps,u=i.id,m=i.onPostCommit;typeof m=="function"&&m(u,n.alternate===null?"mount":"update",e.passiveEffectDuration,-0)}catch(A){Qe(n,n.return,A)}}else kn(e,n,a,o);break;case 31:kn(e,n,a,o);break;case 13:kn(e,n,a,o);break;case 23:break;case 22:i=n.stateNode,u=n.alternate,n.memoizedState!==null?i._visibility&2?kn(e,n,a,o):xl(e,n):i._visibility&2?kn(e,n,a,o):(i._visibility|=2,No(e,n,a,o,(n.subtreeFlags&10256)!==0||!1)),l&2048&&zr(u,n);break;case 24:kn(e,n,a,o),l&2048&&Br(n.alternate,n);break;default:kn(e,n,a,o)}}function No(e,n,a,o,l){for(l=l&&((n.subtreeFlags&10256)!==0||!1),n=n.child;n!==null;){var i=e,u=n,m=a,A=o,q=u.flags;switch(u.tag){case 0:case 11:case 15:No(i,u,m,A,l),vl(8,u);break;case 23:break;case 22:var K=u.stateNode;u.memoizedState!==null?K._visibility&2?No(i,u,m,A,l):xl(i,u):(K._visibility|=2,No(i,u,m,A,l)),l&&q&2048&&zr(u.alternate,u);break;case 24:No(i,u,m,A,l),l&&q&2048&&Br(u.alternate,u);break;default:No(i,u,m,A,l)}n=n.sibling}}function xl(e,n){if(n.subtreeFlags&10256)for(n=n.child;n!==null;){var a=e,o=n,l=o.flags;switch(o.tag){case 22:xl(a,o),l&2048&&zr(o.alternate,o);break;case 24:xl(a,o),l&2048&&Br(o.alternate,o);break;default:xl(a,o)}n=n.sibling}}var Sl=8192;function Ao(e,n,a){if(e.subtreeFlags&Sl)for(e=e.child;e!==null;)Rp(e,n,a),e=e.sibling}function Rp(e,n,a){switch(e.tag){case 26:Ao(e,n,a),e.flags&Sl&&e.memoizedState!==null&&ny(a,Mn,e.memoizedState,e.memoizedProps);break;case 5:Ao(e,n,a);break;case 3:case 4:var o=Mn;Mn=Yi(e.stateNode.containerInfo),Ao(e,n,a),Mn=o;break;case 22:e.memoizedState===null&&(o=e.alternate,o!==null&&o.memoizedState!==null?(o=Sl,Sl=16777216,Ao(e,n,a),Sl=o):Ao(e,n,a));break;default:Ao(e,n,a)}}function zp(e){var n=e.alternate;if(n!==null&&(e=n.child,e!==null)){n.child=null;do n=e.sibling,e.sibling=null,e=n;while(e!==null)}}function wl(e){var n=e.deletions;if((e.flags&16)!==0){if(n!==null)for(var a=0;a<n.length;a++){var o=n[a];Mt=o,Gp(o,e)}zp(e)}if(e.subtreeFlags&10256)for(e=e.child;e!==null;)Bp(e),e=e.sibling}function Bp(e){switch(e.tag){case 0:case 11:case 15:wl(e),e.flags&2048&&va(9,e,e.return);break;case 3:wl(e);break;case 12:wl(e);break;case 22:var n=e.stateNode;e.memoizedState!==null&&n._visibility&2&&(e.return===null||e.return.tag!==13)?(n._visibility&=-3,Ci(e)):wl(e);break;default:wl(e)}}function Ci(e){var n=e.deletions;if((e.flags&16)!==0){if(n!==null)for(var a=0;a<n.length;a++){var o=n[a];Mt=o,Gp(o,e)}zp(e)}for(e=e.child;e!==null;){switch(n=e,n.tag){case 0:case 11:case 15:va(8,n,n.return),Ci(n);break;case 22:a=n.stateNode,a._visibility&2&&(a._visibility&=-3,Ci(n));break;default:Ci(n)}e=e.sibling}}function Gp(e,n){for(;Mt!==null;){var a=Mt;switch(a.tag){case 0:case 11:case 15:va(8,a,n);break;case 23:case 22:if(a.memoizedState!==null&&a.memoizedState.cachePool!==null){var o=a.memoizedState.cachePool.pool;o!=null&&o.refCount++}break;case 24:il(a.memoizedState.cache)}if(o=a.child,o!==null)o.return=a,Mt=o;else e:for(a=e;Mt!==null;){o=Mt;var l=o.sibling,i=o.return;if(Ap(o),o===a){Mt=null;break e}if(l!==null){l.return=i,Mt=l;break e}Mt=i}}}var yg={getCacheForType:function(e){var n=Lt(_t),a=n.data.get(e);return a===void 0&&(a=e(),n.data.set(e,a)),a},cacheSignal:function(){return Lt(_t).controller.signal}},bg=typeof WeakMap=="function"?WeakMap:Map,Xe=0,tt=null,Oe=null,Be=0,Ze=0,dn=null,_a=!1,Co=!1,Gr=!1,aa=0,pt=0,xa=0,Pa=0,Hr=0,pn=0,jo=0,El=null,tn=null,Ur=!1,ji=0,Hp=0,Di=1/0,Mi=null,Sa=null,Ct=0,wa=null,Do=null,oa=0,Lr=0,Yr=null,Up=null,Tl=0,qr=null;function hn(){return(Xe&2)!==0&&Be!==0?Be&-Be:k.T!==null?Qr():lt()}function Lp(){if(pn===0)if((Be&536870912)===0||He){var e=Kt;Kt<<=1,(Kt&3932160)===0&&(Kt=262144),pn=e}else pn=536870912;return e=cn.current,e!==null&&(e.flags|=32),pn}function nn(e,n,a){(e===tt&&(Ze===2||Ze===9)||e.cancelPendingCommit!==null)&&(Mo(e,0),Ea(e,Be,pn,!1)),at(e,a),((Xe&2)===0||e!==tt)&&(e===tt&&((Xe&2)===0&&(Pa|=a),pt===4&&Ea(e,Be,pn,!1)),Gn(e))}function Yp(e,n,a){if((Xe&6)!==0)throw Error(c(327));var o=!a&&(n&127)===0&&(n&e.expiredLanes)===0||De(e,n),l=o?xg(e,n):Xr(e,n,!0),i=o;do{if(l===0){Co&&!o&&Ea(e,n,0,!1);break}else{if(a=e.current.alternate,i&&!vg(a)){l=Xr(e,n,!1),i=!1;continue}if(l===2){if(i=n,e.errorRecoveryDisabledLanes&i)var u=0;else u=e.pendingLanes&-536870913,u=u!==0?u:u&536870912?536870912:0;if(u!==0){n=u;e:{var m=e;l=El;var A=m.current.memoizedState.isDehydrated;if(A&&(Mo(m,u).flags|=256),u=Xr(m,u,!1),u!==2){if(Gr&&!A){m.errorRecoveryDisabledLanes|=i,Pa|=i,l=4;break e}i=tn,tn=l,i!==null&&(tn===null?tn=i:tn.push.apply(tn,i))}l=u}if(i=!1,l!==2)continue}}if(l===1){Mo(e,0),Ea(e,n,0,!0);break}e:{switch(o=e,i=l,i){case 0:case 1:throw Error(c(345));case 4:if((n&4194048)!==n)break;case 6:Ea(o,n,pn,!_a);break e;case 2:tn=null;break;case 3:case 5:break;default:throw Error(c(329))}if((n&62914560)===n&&(l=ji+300-ve(),10<l)){if(Ea(o,n,pn,!_a),me(o,0,!0)!==0)break e;oa=n,o.timeoutHandle=bh(qp.bind(null,o,a,tn,Mi,Ur,n,pn,Pa,jo,_a,i,"Throttled",-0,0),l);break e}qp(o,a,tn,Mi,Ur,n,pn,Pa,jo,_a,i,null,-0,0)}}break}while(!0);Gn(e)}function qp(e,n,a,o,l,i,u,m,A,q,K,F,I,J){if(e.timeoutHandle=-1,F=n.subtreeFlags,F&8192||(F&16785408)===16785408){F={stylesheets:null,count:0,imgCount:0,imgBytes:0,suspenseyImages:[],waitingForImages:!0,waitingForViewTransition:!1,unsuspend:In},Rp(n,i,F);var ce=(i&62914560)===i?ji-ve():(i&4194048)===i?Hp-ve():0;if(ce=ay(F,ce),ce!==null){oa=i,e.cancelPendingCommit=ce($p.bind(null,e,n,i,a,o,l,u,m,A,K,F,null,I,J)),Ea(e,i,u,!q);return}}$p(e,n,i,a,o,l,u,m,A)}function vg(e){for(var n=e;;){var a=n.tag;if((a===0||a===11||a===15)&&n.flags&16384&&(a=n.updateQueue,a!==null&&(a=a.stores,a!==null)))for(var o=0;o<a.length;o++){var l=a[o],i=l.getSnapshot;l=l.value;try{if(!sn(i(),l))return!1}catch{return!1}}if(a=n.child,n.subtreeFlags&16384&&a!==null)a.return=n,n=a;else{if(n===e)break;for(;n.sibling===null;){if(n.return===null||n.return===e)return!0;n=n.return}n.sibling.return=n.return,n=n.sibling}}return!0}function Ea(e,n,a,o){n&=~Hr,n&=~Pa,e.suspendedLanes|=n,e.pingedLanes&=~n,o&&(e.warmLanes|=n),o=e.expirationTimes;for(var l=n;0<l;){var i=31-ct(l),u=1<<i;o[i]=-1,l&=~u}a!==0&&jt(e,a,n)}function ki(){return(Xe&6)===0?(Nl(0),!1):!0}function Ir(){if(Oe!==null){if(Ze===0)var e=Oe.return;else e=Oe,Zn=qa=null,lr(e),xo=null,rl=0,e=Oe;for(;e!==null;)vp(e.alternate,e),e=e.return;Oe=null}}function Mo(e,n){var a=e.timeoutHandle;a!==-1&&(e.timeoutHandle=-1,Ug(a)),a=e.cancelPendingCommit,a!==null&&(e.cancelPendingCommit=null,a()),oa=0,Ir(),tt=e,Oe=a=Jn(e.current,null),Be=n,Ze=0,dn=null,_a=!1,Co=De(e,n),Gr=!1,jo=pn=Hr=Pa=xa=pt=0,tn=El=null,Ur=!1,(n&8)!==0&&(n|=n&32);var o=e.entangledLanes;if(o!==0)for(e=e.entanglements,o&=n;0<o;){var l=31-ct(o),i=1<<l;n|=e[l],o&=~i}return aa=n,Fl(),a}function Ip(e,n){Te=null,k.H=gl,n===_o||n===si?(n=ld(),Ze=3):n===Zs?(n=ld(),Ze=4):Ze=n===xr?8:n!==null&&typeof n=="object"&&typeof n.then=="function"?6:1,dn=n,Oe===null&&(pt=1,xi(e,Sn(n,e.current)))}function Xp(){var e=cn.current;return e===null?!0:(Be&4194048)===Be?Nn===null:(Be&62914560)===Be||(Be&536870912)!==0?e===Nn:!1}function Jp(){var e=k.H;return k.H=gl,e===null?gl:e}function Vp(){var e=k.A;return k.A=yg,e}function Oi(){pt=4,_a||(Be&4194048)!==Be&&cn.current!==null||(Co=!0),(xa&134217727)===0&&(Pa&134217727)===0||tt===null||Ea(tt,Be,pn,!1)}function Xr(e,n,a){var o=Xe;Xe|=2;var l=Jp(),i=Vp();(tt!==e||Be!==n)&&(Mi=null,Mo(e,n)),n=!1;var u=pt;e:do try{if(Ze!==0&&Oe!==null){var m=Oe,A=dn;switch(Ze){case 8:Ir(),u=6;break e;case 3:case 2:case 9:case 6:cn.current===null&&(n=!0);var q=Ze;if(Ze=0,dn=null,ko(e,m,A,q),a&&Co){u=0;break e}break;default:q=Ze,Ze=0,dn=null,ko(e,m,A,q)}}_g(),u=pt;break}catch(K){Ip(e,K)}while(!0);return n&&e.shellSuspendCounter++,Zn=qa=null,Xe=o,k.H=l,k.A=i,Oe===null&&(tt=null,Be=0,Fl()),u}function _g(){for(;Oe!==null;)Zp(Oe)}function xg(e,n){var a=Xe;Xe|=2;var o=Jp(),l=Vp();tt!==e||Be!==n?(Mi=null,Di=ve()+500,Mo(e,n)):Co=De(e,n);e:do try{if(Ze!==0&&Oe!==null){n=Oe;var i=dn;t:switch(Ze){case 1:Ze=0,dn=null,ko(e,n,i,1);break;case 2:case 9:if(ad(i)){Ze=0,dn=null,Qp(n);break}n=function(){Ze!==2&&Ze!==9||tt!==e||(Ze=7),Gn(e)},i.then(n,n);break e;case 3:Ze=7;break e;case 4:Ze=5;break e;case 7:ad(i)?(Ze=0,dn=null,Qp(n)):(Ze=0,dn=null,ko(e,n,i,7));break;case 5:var u=null;switch(Oe.tag){case 26:u=Oe.memoizedState;case 5:case 27:var m=Oe;if(u?Oh(u):m.stateNode.complete){Ze=0,dn=null;var A=m.sibling;if(A!==null)Oe=A;else{var q=m.return;q!==null?(Oe=q,Ri(q)):Oe=null}break t}}Ze=0,dn=null,ko(e,n,i,5);break;case 6:Ze=0,dn=null,ko(e,n,i,6);break;case 8:Ir(),pt=6;break e;default:throw Error(c(462))}}Sg();break}catch(K){Ip(e,K)}while(!0);return Zn=qa=null,k.H=o,k.A=l,Xe=a,Oe!==null?0:(tt=null,Be=0,Fl(),pt)}function Sg(){for(;Oe!==null&&!Le();)Zp(Oe)}function Zp(e){var n=yp(e.alternate,e,aa);e.memoizedProps=e.pendingProps,n===null?Ri(e):Oe=n}function Qp(e){var n=e,a=n.alternate;switch(n.tag){case 15:case 0:n=dp(a,n,n.pendingProps,n.type,void 0,Be);break;case 11:n=dp(a,n,n.pendingProps,n.type.render,n.ref,Be);break;case 5:lr(n);default:vp(a,n),n=Oe=Vu(n,aa),n=yp(a,n,aa)}e.memoizedProps=e.pendingProps,n===null?Ri(e):Oe=n}function ko(e,n,a,o){Zn=qa=null,lr(n),xo=null,rl=0;var l=n.return;try{if(ug(e,l,n,a,Be)){pt=1,xi(e,Sn(a,e.current)),Oe=null;return}}catch(i){if(l!==null)throw Oe=l,i;pt=1,xi(e,Sn(a,e.current)),Oe=null;return}n.flags&32768?(He||o===1?e=!0:Co||(Be&536870912)!==0?e=!1:(_a=e=!0,(o===2||o===9||o===3||o===6)&&(o=cn.current,o!==null&&o.tag===13&&(o.flags|=16384))),Kp(n,e)):Ri(n)}function Ri(e){var n=e;do{if((n.flags&32768)!==0){Kp(n,_a);return}e=n.return;var a=hg(n.alternate,n,aa);if(a!==null){Oe=a;return}if(n=n.sibling,n!==null){Oe=n;return}Oe=n=e}while(n!==null);pt===0&&(pt=5)}function Kp(e,n){do{var a=fg(e.alternate,e);if(a!==null){a.flags&=32767,Oe=a;return}if(a=e.return,a!==null&&(a.flags|=32768,a.subtreeFlags=0,a.deletions=null),!n&&(e=e.sibling,e!==null)){Oe=e;return}Oe=e=a}while(e!==null);pt=6,Oe=null}function $p(e,n,a,o,l,i,u,m,A){e.cancelPendingCommit=null;do zi();while(Ct!==0);if((Xe&6)!==0)throw Error(c(327));if(n!==null){if(n===e.current)throw Error(c(177));if(i=n.lanes|n.childLanes,i|=ks,jn(e,a,i,u,m,A),e===tt&&(Oe=tt=null,Be=0),Do=n,wa=e,oa=a,Lr=i,Yr=l,Up=o,(n.subtreeFlags&10256)!==0||(n.flags&10256)!==0?(e.callbackNode=null,e.callbackPriority=0,Ng(Tt,function(){return th(),null})):(e.callbackNode=null,e.callbackPriority=0),o=(n.flags&13878)!==0,(n.subtreeFlags&13878)!==0||o){o=k.T,k.T=null,l=R.p,R.p=2,u=Xe,Xe|=4;try{mg(e,n,a)}finally{Xe=u,R.p=l,k.T=o}}Ct=1,Pp(),Wp(),Fp()}}function Pp(){if(Ct===1){Ct=0;var e=wa,n=Do,a=(n.flags&13878)!==0;if((n.subtreeFlags&13878)!==0||a){a=k.T,k.T=null;var o=R.p;R.p=2;var l=Xe;Xe|=4;try{Mp(n,e);var i=nc,u=Gu(e.containerInfo),m=i.focusedElem,A=i.selectionRange;if(u!==m&&m&&m.ownerDocument&&Bu(m.ownerDocument.documentElement,m)){if(A!==null&&As(m)){var q=A.start,K=A.end;if(K===void 0&&(K=q),"selectionStart"in m)m.selectionStart=q,m.selectionEnd=Math.min(K,m.value.length);else{var F=m.ownerDocument||document,I=F&&F.defaultView||window;if(I.getSelection){var J=I.getSelection(),ce=m.textContent.length,be=Math.min(A.start,ce),et=A.end===void 0?be:Math.min(A.end,ce);!J.extend&&be>et&&(u=et,et=be,be=u);var B=zu(m,be),O=zu(m,et);if(B&&O&&(J.rangeCount!==1||J.anchorNode!==B.node||J.anchorOffset!==B.offset||J.focusNode!==O.node||J.focusOffset!==O.offset)){var Y=F.createRange();Y.setStart(B.node,B.offset),J.removeAllRanges(),be>et?(J.addRange(Y),J.extend(O.node,O.offset)):(Y.setEnd(O.node,O.offset),J.addRange(Y))}}}}for(F=[],J=m;J=J.parentNode;)J.nodeType===1&&F.push({element:J,left:J.scrollLeft,top:J.scrollTop});for(typeof m.focus=="function"&&m.focus(),m=0;m<F.length;m++){var W=F[m];W.element.scrollLeft=W.left,W.element.scrollTop=W.top}}Zi=!!tc,nc=tc=null}finally{Xe=l,R.p=o,k.T=a}}e.current=n,Ct=2}}function Wp(){if(Ct===2){Ct=0;var e=wa,n=Do,a=(n.flags&8772)!==0;if((n.subtreeFlags&8772)!==0||a){a=k.T,k.T=null;var o=R.p;R.p=2;var l=Xe;Xe|=4;try{Np(e,n.alternate,n)}finally{Xe=l,R.p=o,k.T=a}}Ct=3}}function Fp(){if(Ct===4||Ct===3){Ct=0,ht();var e=wa,n=Do,a=oa,o=Up;(n.subtreeFlags&10256)!==0||(n.flags&10256)!==0?Ct=5:(Ct=0,Do=wa=null,eh(e,e.pendingLanes));var l=e.pendingLanes;if(l===0&&(Sa=null),he(a),n=n.stateNode,Ye&&typeof Ye.onCommitFiberRoot=="function")try{Ye.onCommitFiberRoot(ft,n,void 0,(n.current.flags&128)===128)}catch{}if(o!==null){n=k.T,l=R.p,R.p=2,k.T=null;try{for(var i=e.onRecoverableError,u=0;u<o.length;u++){var m=o[u];i(m.value,{componentStack:m.stack})}}finally{k.T=n,R.p=l}}(oa&3)!==0&&zi(),Gn(e),l=e.pendingLanes,(a&261930)!==0&&(l&42)!==0?e===qr?Tl++:(Tl=0,qr=e):Tl=0,Nl(0)}}function eh(e,n){(e.pooledCacheLanes&=n)===0&&(n=e.pooledCache,n!=null&&(e.pooledCache=null,il(n)))}function zi(){return Pp(),Wp(),Fp(),th()}function th(){if(Ct!==5)return!1;var e=wa,n=Lr;Lr=0;var a=he(oa),o=k.T,l=R.p;try{R.p=32>a?32:a,k.T=null,a=Yr,Yr=null;var i=wa,u=oa;if(Ct=0,Do=wa=null,oa=0,(Xe&6)!==0)throw Error(c(331));var m=Xe;if(Xe|=4,Bp(i.current),Op(i,i.current,u,a),Xe=m,Nl(0,!1),Ye&&typeof Ye.onPostCommitFiberRoot=="function")try{Ye.onPostCommitFiberRoot(ft,i)}catch{}return!0}finally{R.p=l,k.T=o,eh(e,n)}}function nh(e,n,a){n=Sn(a,n),n=_r(e.stateNode,n,2),e=ga(e,n,2),e!==null&&(at(e,2),Gn(e))}function Qe(e,n,a){if(e.tag===3)nh(e,e,a);else for(;n!==null;){if(n.tag===3){nh(n,e,a);break}else if(n.tag===1){var o=n.stateNode;if(typeof n.type.getDerivedStateFromError=="function"||typeof o.componentDidCatch=="function"&&(Sa===null||!Sa.has(o))){e=Sn(a,e),a=ap(2),o=ga(n,a,2),o!==null&&(op(a,o,n,e),at(o,2),Gn(o));break}}n=n.return}}function Jr(e,n,a){var o=e.pingCache;if(o===null){o=e.pingCache=new bg;var l=new Set;o.set(n,l)}else l=o.get(n),l===void 0&&(l=new Set,o.set(n,l));l.has(a)||(Gr=!0,l.add(a),e=wg.bind(null,e,n,a),n.then(e,e))}function wg(e,n,a){var o=e.pingCache;o!==null&&o.delete(n),e.pingedLanes|=e.suspendedLanes&a,e.warmLanes&=~a,tt===e&&(Be&a)===a&&(pt===4||pt===3&&(Be&62914560)===Be&&300>ve()-ji?(Xe&2)===0&&Mo(e,0):Hr|=a,jo===Be&&(jo=0)),Gn(e)}function ah(e,n){n===0&&(n=ge()),e=Ua(e,n),e!==null&&(at(e,n),Gn(e))}function Eg(e){var n=e.memoizedState,a=0;n!==null&&(a=n.retryLane),ah(e,a)}function Tg(e,n){var a=0;switch(e.tag){case 31:case 13:var o=e.stateNode,l=e.memoizedState;l!==null&&(a=l.retryLane);break;case 19:o=e.stateNode;break;case 22:o=e.stateNode._retryCache;break;default:throw Error(c(314))}o!==null&&o.delete(n),ah(e,a)}function Ng(e,n){return Ue(e,n)}var Bi=null,Oo=null,Vr=!1,Gi=!1,Zr=!1,Ta=0;function Gn(e){e!==Oo&&e.next===null&&(Oo===null?Bi=Oo=e:Oo=Oo.next=e),Gi=!0,Vr||(Vr=!0,Cg())}function Nl(e,n){if(!Zr&&Gi){Zr=!0;do for(var a=!1,o=Bi;o!==null;){if(e!==0){var l=o.pendingLanes;if(l===0)var i=0;else{var u=o.suspendedLanes,m=o.pingedLanes;i=(1<<31-ct(42|e)+1)-1,i&=l&~(u&~m),i=i&201326741?i&201326741|1:i?i|2:0}i!==0&&(a=!0,sh(o,i))}else i=Be,i=me(o,o===tt?i:0,o.cancelPendingCommit!==null||o.timeoutHandle!==-1),(i&3)===0||De(o,i)||(a=!0,sh(o,i));o=o.next}while(a);Zr=!1}}function Ag(){oh()}function oh(){Gi=Vr=!1;var e=0;Ta!==0&&Hg()&&(e=Ta);for(var n=ve(),a=null,o=Bi;o!==null;){var l=o.next,i=lh(o,n);i===0?(o.next=null,a===null?Bi=l:a.next=l,l===null&&(Oo=a)):(a=o,(e!==0||(i&3)!==0)&&(Gi=!0)),o=l}Ct!==0&&Ct!==5||Nl(e),Ta!==0&&(Ta=0)}function lh(e,n){for(var a=e.suspendedLanes,o=e.pingedLanes,l=e.expirationTimes,i=e.pendingLanes&-62914561;0<i;){var u=31-ct(i),m=1<<u,A=l[u];A===-1?((m&a)===0||(m&o)!==0)&&(l[u]=Ve(m,n)):A<=n&&(e.expiredLanes|=m),i&=~m}if(n=tt,a=Be,a=me(e,e===n?a:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),o=e.callbackNode,a===0||e===n&&(Ze===2||Ze===9)||e.cancelPendingCommit!==null)return o!==null&&o!==null&&$e(o),e.callbackNode=null,e.callbackPriority=0;if((a&3)===0||De(e,a)){if(n=a&-a,n===e.callbackPriority)return n;switch(o!==null&&$e(o),he(a)){case 2:case 8:a=Et;break;case 32:a=Tt;break;case 268435456:a=Nt;break;default:a=Tt}return o=ih.bind(null,e),a=Ue(a,o),e.callbackPriority=n,e.callbackNode=a,n}return o!==null&&o!==null&&$e(o),e.callbackPriority=2,e.callbackNode=null,2}function ih(e,n){if(Ct!==0&&Ct!==5)return e.callbackNode=null,e.callbackPriority=0,null;var a=e.callbackNode;if(zi()&&e.callbackNode!==a)return null;var o=Be;return o=me(e,e===tt?o:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),o===0?null:(Yp(e,o,n),lh(e,ve()),e.callbackNode!=null&&e.callbackNode===a?ih.bind(null,e):null)}function sh(e,n){if(zi())return null;Yp(e,n,!0)}function Cg(){Lg(function(){(Xe&6)!==0?Ue(bn,Ag):oh()})}function Qr(){if(Ta===0){var e=bo;e===0&&(e=Bt,Bt<<=1,(Bt&261888)===0&&(Bt=256)),Ta=e}return Ta}function rh(e){return e==null||typeof e=="symbol"||typeof e=="boolean"?null:typeof e=="function"?e:Jl(""+e)}function ch(e,n){var a=n.ownerDocument.createElement("input");return a.name=n.name,a.value=n.value,e.id&&a.setAttribute("form",e.id),n.parentNode.insertBefore(a,n),e=new FormData(e),a.parentNode.removeChild(a),e}function jg(e,n,a,o,l){if(n==="submit"&&a&&a.stateNode===l){var i=rh((l[$t]||null).action),u=o.submitter;u&&(n=(n=u[$t]||null)?rh(n.formAction):u.getAttribute("formAction"),n!==null&&(i=n,u=null));var m=new Kl("action","action",null,o,l);e.push({event:m,listeners:[{instance:null,listener:function(){if(o.defaultPrevented){if(Ta!==0){var A=u?ch(l,u):new FormData(l);fr(a,{pending:!0,data:A,method:l.method,action:i},null,A)}}else typeof i=="function"&&(m.preventDefault(),A=u?ch(l,u):new FormData(l),fr(a,{pending:!0,data:A,method:l.method,action:i},i,A))},currentTarget:l}]})}}for(var Kr=0;Kr<Ms.length;Kr++){var $r=Ms[Kr],Dg=$r.toLowerCase(),Mg=$r[0].toUpperCase()+$r.slice(1);Dn(Dg,"on"+Mg)}Dn(Lu,"onAnimationEnd"),Dn(Yu,"onAnimationIteration"),Dn(qu,"onAnimationStart"),Dn("dblclick","onDoubleClick"),Dn("focusin","onFocus"),Dn("focusout","onBlur"),Dn(Zm,"onTransitionRun"),Dn(Qm,"onTransitionStart"),Dn(Km,"onTransitionCancel"),Dn(Iu,"onTransitionEnd"),ao("onMouseEnter",["mouseout","mouseover"]),ao("onMouseLeave",["mouseout","mouseover"]),ao("onPointerEnter",["pointerout","pointerover"]),ao("onPointerLeave",["pointerout","pointerover"]),za("onChange","change click focusin focusout input keydown keyup selectionchange".split(" ")),za("onSelect","focusout contextmenu dragend focusin keydown keyup mousedown mouseup selectionchange".split(" ")),za("onBeforeInput",["compositionend","keypress","textInput","paste"]),za("onCompositionEnd","compositionend focusout keydown keypress keyup mousedown".split(" ")),za("onCompositionStart","compositionstart focusout keydown keypress keyup mousedown".split(" ")),za("onCompositionUpdate","compositionupdate focusout keydown keypress keyup mousedown".split(" "));var Al="abort canplay canplaythrough durationchange emptied encrypted ended error loadeddata loadedmetadata loadstart pause play playing progress ratechange resize seeked seeking stalled suspend timeupdate volumechange waiting".split(" "),kg=new Set("beforetoggle cancel close invalid load scroll scrollend toggle".split(" ").concat(Al));function uh(e,n){n=(n&4)!==0;for(var a=0;a<e.length;a++){var o=e[a],l=o.event;o=o.listeners;e:{var i=void 0;if(n)for(var u=o.length-1;0<=u;u--){var m=o[u],A=m.instance,q=m.currentTarget;if(m=m.listener,A!==i&&l.isPropagationStopped())break e;i=m,l.currentTarget=q;try{i(l)}catch(K){Wl(K)}l.currentTarget=null,i=A}else for(u=0;u<o.length;u++){if(m=o[u],A=m.instance,q=m.currentTarget,m=m.listener,A!==i&&l.isPropagationStopped())break e;i=m,l.currentTarget=q;try{i(l)}catch(K){Wl(K)}l.currentTarget=null,i=A}}}}function Re(e,n){var a=n[cs];a===void 0&&(a=n[cs]=new Set);var o=e+"__bubble";a.has(o)||(dh(n,e,2,!1),a.add(o))}function Pr(e,n,a){var o=0;n&&(o|=4),dh(a,e,o,n)}var Hi="_reactListening"+Math.random().toString(36).slice(2);function Wr(e){if(!e[Hi]){e[Hi]=!0,ou.forEach(function(a){a!=="selectionchange"&&(kg.has(a)||Pr(a,!1,e),Pr(a,!0,e))});var n=e.nodeType===9?e:e.ownerDocument;n===null||n[Hi]||(n[Hi]=!0,Pr("selectionchange",!1,n))}}function dh(e,n,a,o){switch(Lh(n)){case 2:var l=iy;break;case 8:l=sy;break;default:l=hc}a=l.bind(null,n,a,e),l=void 0,!bs||n!=="touchstart"&&n!=="touchmove"&&n!=="wheel"||(l=!0),o?l!==void 0?e.addEventListener(n,a,{capture:!0,passive:l}):e.addEventListener(n,a,!0):l!==void 0?e.addEventListener(n,a,{passive:l}):e.addEventListener(n,a,!1)}function Fr(e,n,a,o,l){var i=o;if((n&1)===0&&(n&2)===0&&o!==null)e:for(;;){if(o===null)return;var u=o.tag;if(u===3||u===4){var m=o.stateNode.containerInfo;if(m===l)break;if(u===4)for(u=o.return;u!==null;){var A=u.tag;if((A===3||A===4)&&u.stateNode.containerInfo===l)return;u=u.return}for(;m!==null;){if(u=eo(m),u===null)return;if(A=u.tag,A===5||A===6||A===26||A===27){o=i=u;continue e}m=m.parentNode}}o=o.return}gu(function(){var q=i,K=gs(a),F=[];e:{var I=Xu.get(e);if(I!==void 0){var J=Kl,ce=e;switch(e){case"keypress":if(Zl(a)===0)break e;case"keydown":case"keyup":J=Tm;break;case"focusin":ce="focus",J=Ss;break;case"focusout":ce="blur",J=Ss;break;case"beforeblur":case"afterblur":J=Ss;break;case"click":if(a.button===2)break e;case"auxclick":case"dblclick":case"mousedown":case"mousemove":case"mouseup":case"mouseout":case"mouseover":case"contextmenu":J=vu;break;case"drag":case"dragend":case"dragenter":case"dragexit":case"dragleave":case"dragover":case"dragstart":case"drop":J=hm;break;case"touchcancel":case"touchend":case"touchmove":case"touchstart":J=Cm;break;case Lu:case Yu:case qu:J=gm;break;case Iu:J=Dm;break;case"scroll":case"scrollend":J=dm;break;case"wheel":J=km;break;case"copy":case"cut":case"paste":J=bm;break;case"gotpointercapture":case"lostpointercapture":case"pointercancel":case"pointerdown":case"pointermove":case"pointerout":case"pointerover":case"pointerup":J=xu;break;case"toggle":case"beforetoggle":J=Rm}var be=(n&4)!==0,et=!be&&(e==="scroll"||e==="scrollend"),B=be?I!==null?I+"Capture":null:I;be=[];for(var O=q,Y;O!==null;){var W=O;if(Y=W.stateNode,W=W.tag,W!==5&&W!==26&&W!==27||Y===null||B===null||(W=Ko(O,B),W!=null&&be.push(Cl(O,W,Y))),et)break;O=O.return}0<be.length&&(I=new J(I,ce,null,a,K),F.push({event:I,listeners:be}))}}if((n&7)===0){e:{if(I=e==="mouseover"||e==="pointerover",J=e==="mouseout"||e==="pointerout",I&&a!==ms&&(ce=a.relatedTarget||a.fromElement)&&(eo(ce)||ce[Fa]))break e;if((J||I)&&(I=K.window===K?K:(I=K.ownerDocument)?I.defaultView||I.parentWindow:window,J?(ce=a.relatedTarget||a.toElement,J=q,ce=ce?eo(ce):null,ce!==null&&(et=p(ce),be=ce.tag,ce!==et||be!==5&&be!==27&&be!==6)&&(ce=null)):(J=null,ce=q),J!==ce)){if(be=vu,W="onMouseLeave",B="onMouseEnter",O="mouse",(e==="pointerout"||e==="pointerover")&&(be=xu,W="onPointerLeave",B="onPointerEnter",O="pointer"),et=J==null?I:Qo(J),Y=ce==null?I:Qo(ce),I=new be(W,O+"leave",J,a,K),I.target=et,I.relatedTarget=Y,W=null,eo(K)===q&&(be=new be(B,O+"enter",ce,a,K),be.target=Y,be.relatedTarget=et,W=be),et=W,J&&ce)t:{for(be=Og,B=J,O=ce,Y=0,W=B;W;W=be(W))Y++;W=0;for(var fe=O;fe;fe=be(fe))W++;for(;0<Y-W;)B=be(B),Y--;for(;0<W-Y;)O=be(O),W--;for(;Y--;){if(B===O||O!==null&&B===O.alternate){be=B;break t}B=be(B),O=be(O)}be=null}else be=null;J!==null&&ph(F,I,J,be,!1),ce!==null&&et!==null&&ph(F,et,ce,be,!0)}}e:{if(I=q?Qo(q):window,J=I.nodeName&&I.nodeName.toLowerCase(),J==="select"||J==="input"&&I.type==="file")var qe=ju;else if(Au(I))if(Du)qe=Xm;else{qe=qm;var ue=Ym}else J=I.nodeName,!J||J.toLowerCase()!=="input"||I.type!=="checkbox"&&I.type!=="radio"?q&&fs(q.elementType)&&(qe=ju):qe=Im;if(qe&&(qe=qe(e,q))){Cu(F,qe,a,K);break e}ue&&ue(e,I,q),e==="focusout"&&q&&I.type==="number"&&q.memoizedProps.value!=null&&hs(I,"number",I.value)}switch(ue=q?Qo(q):window,e){case"focusin":(Au(ue)||ue.contentEditable==="true")&&(co=ue,Cs=q,al=null);break;case"focusout":al=Cs=co=null;break;case"mousedown":js=!0;break;case"contextmenu":case"mouseup":case"dragend":js=!1,Hu(F,a,K);break;case"selectionchange":if(Vm)break;case"keydown":case"keyup":Hu(F,a,K)}var Ce;if(Es)e:{switch(e){case"compositionstart":var Ge="onCompositionStart";break e;case"compositionend":Ge="onCompositionEnd";break e;case"compositionupdate":Ge="onCompositionUpdate";break e}Ge=void 0}else ro?Tu(e,a)&&(Ge="onCompositionEnd"):e==="keydown"&&a.keyCode===229&&(Ge="onCompositionStart");Ge&&(Su&&a.locale!=="ko"&&(ro||Ge!=="onCompositionStart"?Ge==="onCompositionEnd"&&ro&&(Ce=yu()):(ca=K,vs="value"in ca?ca.value:ca.textContent,ro=!0)),ue=Ui(q,Ge),0<ue.length&&(Ge=new _u(Ge,e,null,a,K),F.push({event:Ge,listeners:ue}),Ce?Ge.data=Ce:(Ce=Nu(a),Ce!==null&&(Ge.data=Ce)))),(Ce=Bm?Gm(e,a):Hm(e,a))&&(Ge=Ui(q,"onBeforeInput"),0<Ge.length&&(ue=new _u("onBeforeInput","beforeinput",null,a,K),F.push({event:ue,listeners:Ge}),ue.data=Ce)),jg(F,e,q,a,K)}uh(F,n)})}function Cl(e,n,a){return{instance:e,listener:n,currentTarget:a}}function Ui(e,n){for(var a=n+"Capture",o=[];e!==null;){var l=e,i=l.stateNode;if(l=l.tag,l!==5&&l!==26&&l!==27||i===null||(l=Ko(e,a),l!=null&&o.unshift(Cl(e,l,i)),l=Ko(e,n),l!=null&&o.push(Cl(e,l,i))),e.tag===3)return o;e=e.return}return[]}function Og(e){if(e===null)return null;do e=e.return;while(e&&e.tag!==5&&e.tag!==27);return e||null}function ph(e,n,a,o,l){for(var i=n._reactName,u=[];a!==null&&a!==o;){var m=a,A=m.alternate,q=m.stateNode;if(m=m.tag,A!==null&&A===o)break;m!==5&&m!==26&&m!==27||q===null||(A=q,l?(q=Ko(a,i),q!=null&&u.unshift(Cl(a,q,A))):l||(q=Ko(a,i),q!=null&&u.push(Cl(a,q,A)))),a=a.return}u.length!==0&&e.push({event:n,listeners:u})}var Rg=/\r\n?/g,zg=/\u0000|\uFFFD/g;function hh(e){return(typeof e=="string"?e:""+e).replace(Rg,`
`).replace(zg,"")}function fh(e,n){return n=hh(n),hh(e)===n}function Fe(e,n,a,o,l,i){switch(a){case"children":typeof o=="string"?n==="body"||n==="textarea"&&o===""||lo(e,o):(typeof o=="number"||typeof o=="bigint")&&n!=="body"&&lo(e,""+o);break;case"className":Il(e,"class",o);break;case"tabIndex":Il(e,"tabindex",o);break;case"dir":case"role":case"viewBox":case"width":case"height":Il(e,a,o);break;case"style":fu(e,o,i);break;case"data":if(n!=="object"){Il(e,"data",o);break}case"src":case"href":if(o===""&&(n!=="a"||a!=="href")){e.removeAttribute(a);break}if(o==null||typeof o=="function"||typeof o=="symbol"||typeof o=="boolean"){e.removeAttribute(a);break}o=Jl(""+o),e.setAttribute(a,o);break;case"action":case"formAction":if(typeof o=="function"){e.setAttribute(a,"javascript:throw new Error('A React form was unexpectedly submitted. If you called form.submit() manually, consider using form.requestSubmit() instead. If you\\'re trying to use event.stopPropagation() in a submit event handler, consider also calling event.preventDefault().')");break}else typeof i=="function"&&(a==="formAction"?(n!=="input"&&Fe(e,n,"name",l.name,l,null),Fe(e,n,"formEncType",l.formEncType,l,null),Fe(e,n,"formMethod",l.formMethod,l,null),Fe(e,n,"formTarget",l.formTarget,l,null)):(Fe(e,n,"encType",l.encType,l,null),Fe(e,n,"method",l.method,l,null),Fe(e,n,"target",l.target,l,null)));if(o==null||typeof o=="symbol"||typeof o=="boolean"){e.removeAttribute(a);break}o=Jl(""+o),e.setAttribute(a,o);break;case"onClick":o!=null&&(e.onclick=In);break;case"onScroll":o!=null&&Re("scroll",e);break;case"onScrollEnd":o!=null&&Re("scrollend",e);break;case"dangerouslySetInnerHTML":if(o!=null){if(typeof o!="object"||!("__html"in o))throw Error(c(61));if(a=o.__html,a!=null){if(l.children!=null)throw Error(c(60));e.innerHTML=a}}break;case"multiple":e.multiple=o&&typeof o!="function"&&typeof o!="symbol";break;case"muted":e.muted=o&&typeof o!="function"&&typeof o!="symbol";break;case"suppressContentEditableWarning":case"suppressHydrationWarning":case"defaultValue":case"defaultChecked":case"innerHTML":case"ref":break;case"autoFocus":break;case"xlinkHref":if(o==null||typeof o=="function"||typeof o=="boolean"||typeof o=="symbol"){e.removeAttribute("xlink:href");break}a=Jl(""+o),e.setAttributeNS("http://www.w3.org/1999/xlink","xlink:href",a);break;case"contentEditable":case"spellCheck":case"draggable":case"value":case"autoReverse":case"externalResourcesRequired":case"focusable":case"preserveAlpha":o!=null&&typeof o!="function"&&typeof o!="symbol"?e.setAttribute(a,""+o):e.removeAttribute(a);break;case"inert":case"allowFullScreen":case"async":case"autoPlay":case"controls":case"default":case"defer":case"disabled":case"disablePictureInPicture":case"disableRemotePlayback":case"formNoValidate":case"hidden":case"loop":case"noModule":case"noValidate":case"open":case"playsInline":case"readOnly":case"required":case"reversed":case"scoped":case"seamless":case"itemScope":o&&typeof o!="function"&&typeof o!="symbol"?e.setAttribute(a,""):e.removeAttribute(a);break;case"capture":case"download":o===!0?e.setAttribute(a,""):o!==!1&&o!=null&&typeof o!="function"&&typeof o!="symbol"?e.setAttribute(a,o):e.removeAttribute(a);break;case"cols":case"rows":case"size":case"span":o!=null&&typeof o!="function"&&typeof o!="symbol"&&!isNaN(o)&&1<=o?e.setAttribute(a,o):e.removeAttribute(a);break;case"rowSpan":case"start":o==null||typeof o=="function"||typeof o=="symbol"||isNaN(o)?e.removeAttribute(a):e.setAttribute(a,o);break;case"popover":Re("beforetoggle",e),Re("toggle",e),ql(e,"popover",o);break;case"xlinkActuate":qn(e,"http://www.w3.org/1999/xlink","xlink:actuate",o);break;case"xlinkArcrole":qn(e,"http://www.w3.org/1999/xlink","xlink:arcrole",o);break;case"xlinkRole":qn(e,"http://www.w3.org/1999/xlink","xlink:role",o);break;case"xlinkShow":qn(e,"http://www.w3.org/1999/xlink","xlink:show",o);break;case"xlinkTitle":qn(e,"http://www.w3.org/1999/xlink","xlink:title",o);break;case"xlinkType":qn(e,"http://www.w3.org/1999/xlink","xlink:type",o);break;case"xmlBase":qn(e,"http://www.w3.org/XML/1998/namespace","xml:base",o);break;case"xmlLang":qn(e,"http://www.w3.org/XML/1998/namespace","xml:lang",o);break;case"xmlSpace":qn(e,"http://www.w3.org/XML/1998/namespace","xml:space",o);break;case"is":ql(e,"is",o);break;case"innerText":case"textContent":break;default:(!(2<a.length)||a[0]!=="o"&&a[0]!=="O"||a[1]!=="n"&&a[1]!=="N")&&(a=cm.get(a)||a,ql(e,a,o))}}function ec(e,n,a,o,l,i){switch(a){case"style":fu(e,o,i);break;case"dangerouslySetInnerHTML":if(o!=null){if(typeof o!="object"||!("__html"in o))throw Error(c(61));if(a=o.__html,a!=null){if(l.children!=null)throw Error(c(60));e.innerHTML=a}}break;case"children":typeof o=="string"?lo(e,o):(typeof o=="number"||typeof o=="bigint")&&lo(e,""+o);break;case"onScroll":o!=null&&Re("scroll",e);break;case"onScrollEnd":o!=null&&Re("scrollend",e);break;case"onClick":o!=null&&(e.onclick=In);break;case"suppressContentEditableWarning":case"suppressHydrationWarning":case"innerHTML":case"ref":break;case"innerText":case"textContent":break;default:if(!lu.hasOwnProperty(a))e:{if(a[0]==="o"&&a[1]==="n"&&(l=a.endsWith("Capture"),n=a.slice(2,l?a.length-7:void 0),i=e[$t]||null,i=i!=null?i[a]:null,typeof i=="function"&&e.removeEventListener(n,i,l),typeof o=="function")){typeof i!="function"&&i!==null&&(a in e?e[a]=null:e.hasAttribute(a)&&e.removeAttribute(a)),e.addEventListener(n,o,l);break e}a in e?e[a]=o:o===!0?e.setAttribute(a,""):ql(e,a,o)}}}function qt(e,n,a){switch(n){case"div":case"span":case"svg":case"path":case"a":case"g":case"p":case"li":break;case"img":Re("error",e),Re("load",e);var o=!1,l=!1,i;for(i in a)if(a.hasOwnProperty(i)){var u=a[i];if(u!=null)switch(i){case"src":o=!0;break;case"srcSet":l=!0;break;case"children":case"dangerouslySetInnerHTML":throw Error(c(137,n));default:Fe(e,n,i,u,a,null)}}l&&Fe(e,n,"srcSet",a.srcSet,a,null),o&&Fe(e,n,"src",a.src,a,null);return;case"input":Re("invalid",e);var m=i=u=l=null,A=null,q=null;for(o in a)if(a.hasOwnProperty(o)){var K=a[o];if(K!=null)switch(o){case"name":l=K;break;case"type":u=K;break;case"checked":A=K;break;case"defaultChecked":q=K;break;case"value":i=K;break;case"defaultValue":m=K;break;case"children":case"dangerouslySetInnerHTML":if(K!=null)throw Error(c(137,n));break;default:Fe(e,n,o,K,a,null)}}uu(e,i,m,A,q,u,l,!1);return;case"select":Re("invalid",e),o=u=i=null;for(l in a)if(a.hasOwnProperty(l)&&(m=a[l],m!=null))switch(l){case"value":i=m;break;case"defaultValue":u=m;break;case"multiple":o=m;default:Fe(e,n,l,m,a,null)}n=i,a=u,e.multiple=!!o,n!=null?oo(e,!!o,n,!1):a!=null&&oo(e,!!o,a,!0);return;case"textarea":Re("invalid",e),i=l=o=null;for(u in a)if(a.hasOwnProperty(u)&&(m=a[u],m!=null))switch(u){case"value":o=m;break;case"defaultValue":l=m;break;case"children":i=m;break;case"dangerouslySetInnerHTML":if(m!=null)throw Error(c(91));break;default:Fe(e,n,u,m,a,null)}pu(e,o,l,i);return;case"option":for(A in a)if(a.hasOwnProperty(A)&&(o=a[A],o!=null))switch(A){case"selected":e.selected=o&&typeof o!="function"&&typeof o!="symbol";break;default:Fe(e,n,A,o,a,null)}return;case"dialog":Re("beforetoggle",e),Re("toggle",e),Re("cancel",e),Re("close",e);break;case"iframe":case"object":Re("load",e);break;case"video":case"audio":for(o=0;o<Al.length;o++)Re(Al[o],e);break;case"image":Re("error",e),Re("load",e);break;case"details":Re("toggle",e);break;case"embed":case"source":case"link":Re("error",e),Re("load",e);case"area":case"base":case"br":case"col":case"hr":case"keygen":case"meta":case"param":case"track":case"wbr":case"menuitem":for(q in a)if(a.hasOwnProperty(q)&&(o=a[q],o!=null))switch(q){case"children":case"dangerouslySetInnerHTML":throw Error(c(137,n));default:Fe(e,n,q,o,a,null)}return;default:if(fs(n)){for(K in a)a.hasOwnProperty(K)&&(o=a[K],o!==void 0&&ec(e,n,K,o,a,void 0));return}}for(m in a)a.hasOwnProperty(m)&&(o=a[m],o!=null&&Fe(e,n,m,o,a,null))}function Bg(e,n,a,o){switch(n){case"div":case"span":case"svg":case"path":case"a":case"g":case"p":case"li":break;case"input":var l=null,i=null,u=null,m=null,A=null,q=null,K=null;for(J in a){var F=a[J];if(a.hasOwnProperty(J)&&F!=null)switch(J){case"checked":break;case"value":break;case"defaultValue":A=F;default:o.hasOwnProperty(J)||Fe(e,n,J,null,o,F)}}for(var I in o){var J=o[I];if(F=a[I],o.hasOwnProperty(I)&&(J!=null||F!=null))switch(I){case"type":i=J;break;case"name":l=J;break;case"checked":q=J;break;case"defaultChecked":K=J;break;case"value":u=J;break;case"defaultValue":m=J;break;case"children":case"dangerouslySetInnerHTML":if(J!=null)throw Error(c(137,n));break;default:J!==F&&Fe(e,n,I,J,o,F)}}ps(e,u,m,A,q,K,i,l);return;case"select":J=u=m=I=null;for(i in a)if(A=a[i],a.hasOwnProperty(i)&&A!=null)switch(i){case"value":break;case"multiple":J=A;default:o.hasOwnProperty(i)||Fe(e,n,i,null,o,A)}for(l in o)if(i=o[l],A=a[l],o.hasOwnProperty(l)&&(i!=null||A!=null))switch(l){case"value":I=i;break;case"defaultValue":m=i;break;case"multiple":u=i;default:i!==A&&Fe(e,n,l,i,o,A)}n=m,a=u,o=J,I!=null?oo(e,!!a,I,!1):!!o!=!!a&&(n!=null?oo(e,!!a,n,!0):oo(e,!!a,a?[]:"",!1));return;case"textarea":J=I=null;for(m in a)if(l=a[m],a.hasOwnProperty(m)&&l!=null&&!o.hasOwnProperty(m))switch(m){case"value":break;case"children":break;default:Fe(e,n,m,null,o,l)}for(u in o)if(l=o[u],i=a[u],o.hasOwnProperty(u)&&(l!=null||i!=null))switch(u){case"value":I=l;break;case"defaultValue":J=l;break;case"children":break;case"dangerouslySetInnerHTML":if(l!=null)throw Error(c(91));break;default:l!==i&&Fe(e,n,u,l,o,i)}du(e,I,J);return;case"option":for(var ce in a)if(I=a[ce],a.hasOwnProperty(ce)&&I!=null&&!o.hasOwnProperty(ce))switch(ce){case"selected":e.selected=!1;break;default:Fe(e,n,ce,null,o,I)}for(A in o)if(I=o[A],J=a[A],o.hasOwnProperty(A)&&I!==J&&(I!=null||J!=null))switch(A){case"selected":e.selected=I&&typeof I!="function"&&typeof I!="symbol";break;default:Fe(e,n,A,I,o,J)}return;case"img":case"link":case"area":case"base":case"br":case"col":case"embed":case"hr":case"keygen":case"meta":case"param":case"source":case"track":case"wbr":case"menuitem":for(var be in a)I=a[be],a.hasOwnProperty(be)&&I!=null&&!o.hasOwnProperty(be)&&Fe(e,n,be,null,o,I);for(q in o)if(I=o[q],J=a[q],o.hasOwnProperty(q)&&I!==J&&(I!=null||J!=null))switch(q){case"children":case"dangerouslySetInnerHTML":if(I!=null)throw Error(c(137,n));break;default:Fe(e,n,q,I,o,J)}return;default:if(fs(n)){for(var et in a)I=a[et],a.hasOwnProperty(et)&&I!==void 0&&!o.hasOwnProperty(et)&&ec(e,n,et,void 0,o,I);for(K in o)I=o[K],J=a[K],!o.hasOwnProperty(K)||I===J||I===void 0&&J===void 0||ec(e,n,K,I,o,J);return}}for(var B in a)I=a[B],a.hasOwnProperty(B)&&I!=null&&!o.hasOwnProperty(B)&&Fe(e,n,B,null,o,I);for(F in o)I=o[F],J=a[F],!o.hasOwnProperty(F)||I===J||I==null&&J==null||Fe(e,n,F,I,o,J)}function mh(e){switch(e){case"css":case"script":case"font":case"img":case"image":case"input":case"link":return!0;default:return!1}}function Gg(){if(typeof performance.getEntriesByType=="function"){for(var e=0,n=0,a=performance.getEntriesByType("resource"),o=0;o<a.length;o++){var l=a[o],i=l.transferSize,u=l.initiatorType,m=l.duration;if(i&&m&&mh(u)){for(u=0,m=l.responseEnd,o+=1;o<a.length;o++){var A=a[o],q=A.startTime;if(q>m)break;var K=A.transferSize,F=A.initiatorType;K&&mh(F)&&(A=A.responseEnd,u+=K*(A<m?1:(m-q)/(A-q)))}if(--o,n+=8*(i+u)/(l.duration/1e3),e++,10<e)break}}if(0<e)return n/e/1e6}return navigator.connection&&(e=navigator.connection.downlink,typeof e=="number")?e:5}var tc=null,nc=null;function Li(e){return e.nodeType===9?e:e.ownerDocument}function gh(e){switch(e){case"http://www.w3.org/2000/svg":return 1;case"http://www.w3.org/1998/Math/MathML":return 2;default:return 0}}function yh(e,n){if(e===0)switch(n){case"svg":return 1;case"math":return 2;default:return 0}return e===1&&n==="foreignObject"?0:e}function ac(e,n){return e==="textarea"||e==="noscript"||typeof n.children=="string"||typeof n.children=="number"||typeof n.children=="bigint"||typeof n.dangerouslySetInnerHTML=="object"&&n.dangerouslySetInnerHTML!==null&&n.dangerouslySetInnerHTML.__html!=null}var oc=null;function Hg(){var e=window.event;return e&&e.type==="popstate"?e===oc?!1:(oc=e,!0):(oc=null,!1)}var bh=typeof setTimeout=="function"?setTimeout:void 0,Ug=typeof clearTimeout=="function"?clearTimeout:void 0,vh=typeof Promise=="function"?Promise:void 0,Lg=typeof queueMicrotask=="function"?queueMicrotask:typeof vh<"u"?function(e){return vh.resolve(null).then(e).catch(Yg)}:bh;function Yg(e){setTimeout(function(){throw e})}function Na(e){return e==="head"}function _h(e,n){var a=n,o=0;do{var l=a.nextSibling;if(e.removeChild(a),l&&l.nodeType===8)if(a=l.data,a==="/$"||a==="/&"){if(o===0){e.removeChild(l),Go(n);return}o--}else if(a==="$"||a==="$?"||a==="$~"||a==="$!"||a==="&")o++;else if(a==="html")jl(e.ownerDocument.documentElement);else if(a==="head"){a=e.ownerDocument.head,jl(a);for(var i=a.firstChild;i;){var u=i.nextSibling,m=i.nodeName;i[Zo]||m==="SCRIPT"||m==="STYLE"||m==="LINK"&&i.rel.toLowerCase()==="stylesheet"||a.removeChild(i),i=u}}else a==="body"&&jl(e.ownerDocument.body);a=l}while(a);Go(n)}function xh(e,n){var a=e;e=0;do{var o=a.nextSibling;if(a.nodeType===1?n?(a._stashedDisplay=a.style.display,a.style.display="none"):(a.style.display=a._stashedDisplay||"",a.getAttribute("style")===""&&a.removeAttribute("style")):a.nodeType===3&&(n?(a._stashedText=a.nodeValue,a.nodeValue=""):a.nodeValue=a._stashedText||""),o&&o.nodeType===8)if(a=o.data,a==="/$"){if(e===0)break;e--}else a!=="$"&&a!=="$?"&&a!=="$~"&&a!=="$!"||e++;a=o}while(a)}function lc(e){var n=e.firstChild;for(n&&n.nodeType===10&&(n=n.nextSibling);n;){var a=n;switch(n=n.nextSibling,a.nodeName){case"HTML":case"HEAD":case"BODY":lc(a),us(a);continue;case"SCRIPT":case"STYLE":continue;case"LINK":if(a.rel.toLowerCase()==="stylesheet")continue}e.removeChild(a)}}function qg(e,n,a,o){for(;e.nodeType===1;){var l=a;if(e.nodeName.toLowerCase()!==n.toLowerCase()){if(!o&&(e.nodeName!=="INPUT"||e.type!=="hidden"))break}else if(o){if(!e[Zo])switch(n){case"meta":if(!e.hasAttribute("itemprop"))break;return e;case"link":if(i=e.getAttribute("rel"),i==="stylesheet"&&e.hasAttribute("data-precedence"))break;if(i!==l.rel||e.getAttribute("href")!==(l.href==null||l.href===""?null:l.href)||e.getAttribute("crossorigin")!==(l.crossOrigin==null?null:l.crossOrigin)||e.getAttribute("title")!==(l.title==null?null:l.title))break;return e;case"style":if(e.hasAttribute("data-precedence"))break;return e;case"script":if(i=e.getAttribute("src"),(i!==(l.src==null?null:l.src)||e.getAttribute("type")!==(l.type==null?null:l.type)||e.getAttribute("crossorigin")!==(l.crossOrigin==null?null:l.crossOrigin))&&i&&e.hasAttribute("async")&&!e.hasAttribute("itemprop"))break;return e;default:return e}}else if(n==="input"&&e.type==="hidden"){var i=l.name==null?null:""+l.name;if(l.type==="hidden"&&e.getAttribute("name")===i)return e}else return e;if(e=An(e.nextSibling),e===null)break}return null}function Ig(e,n,a){if(n==="")return null;for(;e.nodeType!==3;)if((e.nodeType!==1||e.nodeName!=="INPUT"||e.type!=="hidden")&&!a||(e=An(e.nextSibling),e===null))return null;return e}function Sh(e,n){for(;e.nodeType!==8;)if((e.nodeType!==1||e.nodeName!=="INPUT"||e.type!=="hidden")&&!n||(e=An(e.nextSibling),e===null))return null;return e}function ic(e){return e.data==="$?"||e.data==="$~"}function sc(e){return e.data==="$!"||e.data==="$?"&&e.ownerDocument.readyState!=="loading"}function Xg(e,n){var a=e.ownerDocument;if(e.data==="$~")e._reactRetry=n;else if(e.data!=="$?"||a.readyState!=="loading")n();else{var o=function(){n(),a.removeEventListener("DOMContentLoaded",o)};a.addEventListener("DOMContentLoaded",o),e._reactRetry=o}}function An(e){for(;e!=null;e=e.nextSibling){var n=e.nodeType;if(n===1||n===3)break;if(n===8){if(n=e.data,n==="$"||n==="$!"||n==="$?"||n==="$~"||n==="&"||n==="F!"||n==="F")break;if(n==="/$"||n==="/&")return null}}return e}var rc=null;function wh(e){e=e.nextSibling;for(var n=0;e;){if(e.nodeType===8){var a=e.data;if(a==="/$"||a==="/&"){if(n===0)return An(e.nextSibling);n--}else a!=="$"&&a!=="$!"&&a!=="$?"&&a!=="$~"&&a!=="&"||n++}e=e.nextSibling}return null}function Eh(e){e=e.previousSibling;for(var n=0;e;){if(e.nodeType===8){var a=e.data;if(a==="$"||a==="$!"||a==="$?"||a==="$~"||a==="&"){if(n===0)return e;n--}else a!=="/$"&&a!=="/&"||n++}e=e.previousSibling}return null}function Th(e,n,a){switch(n=Li(a),e){case"html":if(e=n.documentElement,!e)throw Error(c(452));return e;case"head":if(e=n.head,!e)throw Error(c(453));return e;case"body":if(e=n.body,!e)throw Error(c(454));return e;default:throw Error(c(451))}}function jl(e){for(var n=e.attributes;n.length;)e.removeAttributeNode(n[0]);us(e)}var Cn=new Map,Nh=new Set;function Yi(e){return typeof e.getRootNode=="function"?e.getRootNode():e.nodeType===9?e:e.ownerDocument}var la=R.d;R.d={f:Jg,r:Vg,D:Zg,C:Qg,L:Kg,m:$g,X:Wg,S:Pg,M:Fg};function Jg(){var e=la.f(),n=ki();return e||n}function Vg(e){var n=to(e);n!==null&&n.tag===5&&n.type==="form"?Id(n):la.r(e)}var Ro=typeof document>"u"?null:document;function Ah(e,n,a){var o=Ro;if(o&&typeof n=="string"&&n){var l=_n(n);l='link[rel="'+e+'"][href="'+l+'"]',typeof a=="string"&&(l+='[crossorigin="'+a+'"]'),Nh.has(l)||(Nh.add(l),e={rel:e,crossOrigin:a,href:n},o.querySelector(l)===null&&(n=o.createElement("link"),qt(n,"link",e),Dt(n),o.head.appendChild(n)))}}function Zg(e){la.D(e),Ah("dns-prefetch",e,null)}function Qg(e,n){la.C(e,n),Ah("preconnect",e,n)}function Kg(e,n,a){la.L(e,n,a);var o=Ro;if(o&&e&&n){var l='link[rel="preload"][as="'+_n(n)+'"]';n==="image"&&a&&a.imageSrcSet?(l+='[imagesrcset="'+_n(a.imageSrcSet)+'"]',typeof a.imageSizes=="string"&&(l+='[imagesizes="'+_n(a.imageSizes)+'"]')):l+='[href="'+_n(e)+'"]';var i=l;switch(n){case"style":i=zo(e);break;case"script":i=Bo(e)}Cn.has(i)||(e=v({rel:"preload",href:n==="image"&&a&&a.imageSrcSet?void 0:e,as:n},a),Cn.set(i,e),o.querySelector(l)!==null||n==="style"&&o.querySelector(Dl(i))||n==="script"&&o.querySelector(Ml(i))||(n=o.createElement("link"),qt(n,"link",e),Dt(n),o.head.appendChild(n)))}}function $g(e,n){la.m(e,n);var a=Ro;if(a&&e){var o=n&&typeof n.as=="string"?n.as:"script",l='link[rel="modulepreload"][as="'+_n(o)+'"][href="'+_n(e)+'"]',i=l;switch(o){case"audioworklet":case"paintworklet":case"serviceworker":case"sharedworker":case"worker":case"script":i=Bo(e)}if(!Cn.has(i)&&(e=v({rel:"modulepreload",href:e},n),Cn.set(i,e),a.querySelector(l)===null)){switch(o){case"audioworklet":case"paintworklet":case"serviceworker":case"sharedworker":case"worker":case"script":if(a.querySelector(Ml(i)))return}o=a.createElement("link"),qt(o,"link",e),Dt(o),a.head.appendChild(o)}}}function Pg(e,n,a){la.S(e,n,a);var o=Ro;if(o&&e){var l=no(o).hoistableStyles,i=zo(e);n=n||"default";var u=l.get(i);if(!u){var m={loading:0,preload:null};if(u=o.querySelector(Dl(i)))m.loading=5;else{e=v({rel:"stylesheet",href:e,"data-precedence":n},a),(a=Cn.get(i))&&cc(e,a);var A=u=o.createElement("link");Dt(A),qt(A,"link",e),A._p=new Promise(function(q,K){A.onload=q,A.onerror=K}),A.addEventListener("load",function(){m.loading|=1}),A.addEventListener("error",function(){m.loading|=2}),m.loading|=4,qi(u,n,o)}u={type:"stylesheet",instance:u,count:1,state:m},l.set(i,u)}}}function Wg(e,n){la.X(e,n);var a=Ro;if(a&&e){var o=no(a).hoistableScripts,l=Bo(e),i=o.get(l);i||(i=a.querySelector(Ml(l)),i||(e=v({src:e,async:!0},n),(n=Cn.get(l))&&uc(e,n),i=a.createElement("script"),Dt(i),qt(i,"link",e),a.head.appendChild(i)),i={type:"script",instance:i,count:1,state:null},o.set(l,i))}}function Fg(e,n){la.M(e,n);var a=Ro;if(a&&e){var o=no(a).hoistableScripts,l=Bo(e),i=o.get(l);i||(i=a.querySelector(Ml(l)),i||(e=v({src:e,async:!0,type:"module"},n),(n=Cn.get(l))&&uc(e,n),i=a.createElement("script"),Dt(i),qt(i,"link",e),a.head.appendChild(i)),i={type:"script",instance:i,count:1,state:null},o.set(l,i))}}function Ch(e,n,a,o){var l=(l=se.current)?Yi(l):null;if(!l)throw Error(c(446));switch(e){case"meta":case"title":return null;case"style":return typeof a.precedence=="string"&&typeof a.href=="string"?(n=zo(a.href),a=no(l).hoistableStyles,o=a.get(n),o||(o={type:"style",instance:null,count:0,state:null},a.set(n,o)),o):{type:"void",instance:null,count:0,state:null};case"link":if(a.rel==="stylesheet"&&typeof a.href=="string"&&typeof a.precedence=="string"){e=zo(a.href);var i=no(l).hoistableStyles,u=i.get(e);if(u||(l=l.ownerDocument||l,u={type:"stylesheet",instance:null,count:0,state:{loading:0,preload:null}},i.set(e,u),(i=l.querySelector(Dl(e)))&&!i._p&&(u.instance=i,u.state.loading=5),Cn.has(e)||(a={rel:"preload",as:"style",href:a.href,crossOrigin:a.crossOrigin,integrity:a.integrity,media:a.media,hrefLang:a.hrefLang,referrerPolicy:a.referrerPolicy},Cn.set(e,a),i||ey(l,e,a,u.state))),n&&o===null)throw Error(c(528,""));return u}if(n&&o!==null)throw Error(c(529,""));return null;case"script":return n=a.async,a=a.src,typeof a=="string"&&n&&typeof n!="function"&&typeof n!="symbol"?(n=Bo(a),a=no(l).hoistableScripts,o=a.get(n),o||(o={type:"script",instance:null,count:0,state:null},a.set(n,o)),o):{type:"void",instance:null,count:0,state:null};default:throw Error(c(444,e))}}function zo(e){return'href="'+_n(e)+'"'}function Dl(e){return'link[rel="stylesheet"]['+e+"]"}function jh(e){return v({},e,{"data-precedence":e.precedence,precedence:null})}function ey(e,n,a,o){e.querySelector('link[rel="preload"][as="style"]['+n+"]")?o.loading=1:(n=e.createElement("link"),o.preload=n,n.addEventListener("load",function(){return o.loading|=1}),n.addEventListener("error",function(){return o.loading|=2}),qt(n,"link",a),Dt(n),e.head.appendChild(n))}function Bo(e){return'[src="'+_n(e)+'"]'}function Ml(e){return"script[async]"+e}function Dh(e,n,a){if(n.count++,n.instance===null)switch(n.type){case"style":var o=e.querySelector('style[data-href~="'+_n(a.href)+'"]');if(o)return n.instance=o,Dt(o),o;var l=v({},a,{"data-href":a.href,"data-precedence":a.precedence,href:null,precedence:null});return o=(e.ownerDocument||e).createElement("style"),Dt(o),qt(o,"style",l),qi(o,a.precedence,e),n.instance=o;case"stylesheet":l=zo(a.href);var i=e.querySelector(Dl(l));if(i)return n.state.loading|=4,n.instance=i,Dt(i),i;o=jh(a),(l=Cn.get(l))&&cc(o,l),i=(e.ownerDocument||e).createElement("link"),Dt(i);var u=i;return u._p=new Promise(function(m,A){u.onload=m,u.onerror=A}),qt(i,"link",o),n.state.loading|=4,qi(i,a.precedence,e),n.instance=i;case"script":return i=Bo(a.src),(l=e.querySelector(Ml(i)))?(n.instance=l,Dt(l),l):(o=a,(l=Cn.get(i))&&(o=v({},a),uc(o,l)),e=e.ownerDocument||e,l=e.createElement("script"),Dt(l),qt(l,"link",o),e.head.appendChild(l),n.instance=l);case"void":return null;default:throw Error(c(443,n.type))}else n.type==="stylesheet"&&(n.state.loading&4)===0&&(o=n.instance,n.state.loading|=4,qi(o,a.precedence,e));return n.instance}function qi(e,n,a){for(var o=a.querySelectorAll('link[rel="stylesheet"][data-precedence],style[data-precedence]'),l=o.length?o[o.length-1]:null,i=l,u=0;u<o.length;u++){var m=o[u];if(m.dataset.precedence===n)i=m;else if(i!==l)break}i?i.parentNode.insertBefore(e,i.nextSibling):(n=a.nodeType===9?a.head:a,n.insertBefore(e,n.firstChild))}function cc(e,n){e.crossOrigin==null&&(e.crossOrigin=n.crossOrigin),e.referrerPolicy==null&&(e.referrerPolicy=n.referrerPolicy),e.title==null&&(e.title=n.title)}function uc(e,n){e.crossOrigin==null&&(e.crossOrigin=n.crossOrigin),e.referrerPolicy==null&&(e.referrerPolicy=n.referrerPolicy),e.integrity==null&&(e.integrity=n.integrity)}var Ii=null;function Mh(e,n,a){if(Ii===null){var o=new Map,l=Ii=new Map;l.set(a,o)}else l=Ii,o=l.get(a),o||(o=new Map,l.set(a,o));if(o.has(e))return o;for(o.set(e,null),a=a.getElementsByTagName(e),l=0;l<a.length;l++){var i=a[l];if(!(i[Zo]||i[Ht]||e==="link"&&i.getAttribute("rel")==="stylesheet")&&i.namespaceURI!=="http://www.w3.org/2000/svg"){var u=i.getAttribute(n)||"";u=e+u;var m=o.get(u);m?m.push(i):o.set(u,[i])}}return o}function kh(e,n,a){e=e.ownerDocument||e,e.head.insertBefore(a,n==="title"?e.querySelector("head > title"):null)}function ty(e,n,a){if(a===1||n.itemProp!=null)return!1;switch(e){case"meta":case"title":return!0;case"style":if(typeof n.precedence!="string"||typeof n.href!="string"||n.href==="")break;return!0;case"link":if(typeof n.rel!="string"||typeof n.href!="string"||n.href===""||n.onLoad||n.onError)break;switch(n.rel){case"stylesheet":return e=n.disabled,typeof n.precedence=="string"&&e==null;default:return!0}case"script":if(n.async&&typeof n.async!="function"&&typeof n.async!="symbol"&&!n.onLoad&&!n.onError&&n.src&&typeof n.src=="string")return!0}return!1}function Oh(e){return!(e.type==="stylesheet"&&(e.state.loading&3)===0)}function ny(e,n,a,o){if(a.type==="stylesheet"&&(typeof o.media!="string"||matchMedia(o.media).matches!==!1)&&(a.state.loading&4)===0){if(a.instance===null){var l=zo(o.href),i=n.querySelector(Dl(l));if(i){n=i._p,n!==null&&typeof n=="object"&&typeof n.then=="function"&&(e.count++,e=Xi.bind(e),n.then(e,e)),a.state.loading|=4,a.instance=i,Dt(i);return}i=n.ownerDocument||n,o=jh(o),(l=Cn.get(l))&&cc(o,l),i=i.createElement("link"),Dt(i);var u=i;u._p=new Promise(function(m,A){u.onload=m,u.onerror=A}),qt(i,"link",o),a.instance=i}e.stylesheets===null&&(e.stylesheets=new Map),e.stylesheets.set(a,n),(n=a.state.preload)&&(a.state.loading&3)===0&&(e.count++,a=Xi.bind(e),n.addEventListener("load",a),n.addEventListener("error",a))}}var dc=0;function ay(e,n){return e.stylesheets&&e.count===0&&Vi(e,e.stylesheets),0<e.count||0<e.imgCount?function(a){var o=setTimeout(function(){if(e.stylesheets&&Vi(e,e.stylesheets),e.unsuspend){var i=e.unsuspend;e.unsuspend=null,i()}},6e4+n);0<e.imgBytes&&dc===0&&(dc=62500*Gg());var l=setTimeout(function(){if(e.waitingForImages=!1,e.count===0&&(e.stylesheets&&Vi(e,e.stylesheets),e.unsuspend)){var i=e.unsuspend;e.unsuspend=null,i()}},(e.imgBytes>dc?50:800)+n);return e.unsuspend=a,function(){e.unsuspend=null,clearTimeout(o),clearTimeout(l)}}:null}function Xi(){if(this.count--,this.count===0&&(this.imgCount===0||!this.waitingForImages)){if(this.stylesheets)Vi(this,this.stylesheets);else if(this.unsuspend){var e=this.unsuspend;this.unsuspend=null,e()}}}var Ji=null;function Vi(e,n){e.stylesheets=null,e.unsuspend!==null&&(e.count++,Ji=new Map,n.forEach(oy,e),Ji=null,Xi.call(e))}function oy(e,n){if(!(n.state.loading&4)){var a=Ji.get(e);if(a)var o=a.get(null);else{a=new Map,Ji.set(e,a);for(var l=e.querySelectorAll("link[data-precedence],style[data-precedence]"),i=0;i<l.length;i++){var u=l[i];(u.nodeName==="LINK"||u.getAttribute("media")!=="not all")&&(a.set(u.dataset.precedence,u),o=u)}o&&a.set(null,o)}l=n.instance,u=l.getAttribute("data-precedence"),i=a.get(u)||o,i===o&&a.set(null,l),a.set(u,l),this.count++,o=Xi.bind(this),l.addEventListener("load",o),l.addEventListener("error",o),i?i.parentNode.insertBefore(l,i.nextSibling):(e=e.nodeType===9?e.head:e,e.insertBefore(l,e.firstChild)),n.state.loading|=4}}var kl={$$typeof:j,Provider:null,Consumer:null,_currentValue:$,_currentValue2:$,_threadCount:0};function ly(e,n,a,o,l,i,u,m,A){this.tag=1,this.containerInfo=e,this.pingCache=this.current=this.pendingChildren=null,this.timeoutHandle=-1,this.callbackNode=this.next=this.pendingContext=this.context=this.cancelPendingCommit=null,this.callbackPriority=0,this.expirationTimes=ot(-1),this.entangledLanes=this.shellSuspendCounter=this.errorRecoveryDisabledLanes=this.expiredLanes=this.warmLanes=this.pingedLanes=this.suspendedLanes=this.pendingLanes=0,this.entanglements=ot(0),this.hiddenUpdates=ot(null),this.identifierPrefix=o,this.onUncaughtError=l,this.onCaughtError=i,this.onRecoverableError=u,this.pooledCache=null,this.pooledCacheLanes=0,this.formState=A,this.incompleteTransitions=new Map}function Rh(e,n,a,o,l,i,u,m,A,q,K,F){return e=new ly(e,n,a,u,A,q,K,F,m),n=1,i===!0&&(n|=24),i=rn(3,null,null,n),e.current=i,i.stateNode=e,n=Xs(),n.refCount++,e.pooledCache=n,n.refCount++,i.memoizedState={element:o,isDehydrated:a,cache:n},Qs(i),e}function zh(e){return e?(e=ho,e):ho}function Bh(e,n,a,o,l,i){l=zh(l),o.context===null?o.context=l:o.pendingContext=l,o=ma(n),o.payload={element:a},i=i===void 0?null:i,i!==null&&(o.callback=i),a=ga(e,o,n),a!==null&&(nn(a,e,n),ul(a,e,n))}function Gh(e,n){if(e=e.memoizedState,e!==null&&e.dehydrated!==null){var a=e.retryLane;e.retryLane=a!==0&&a<n?a:n}}function pc(e,n){Gh(e,n),(e=e.alternate)&&Gh(e,n)}function Hh(e){if(e.tag===13||e.tag===31){var n=Ua(e,67108864);n!==null&&nn(n,e,67108864),pc(e,67108864)}}function Uh(e){if(e.tag===13||e.tag===31){var n=hn();n=ra(n);var a=Ua(e,n);a!==null&&nn(a,e,n),pc(e,n)}}var Zi=!0;function iy(e,n,a,o){var l=k.T;k.T=null;var i=R.p;try{R.p=2,hc(e,n,a,o)}finally{R.p=i,k.T=l}}function sy(e,n,a,o){var l=k.T;k.T=null;var i=R.p;try{R.p=8,hc(e,n,a,o)}finally{R.p=i,k.T=l}}function hc(e,n,a,o){if(Zi){var l=fc(o);if(l===null)Fr(e,n,o,Qi,a),Yh(e,o);else if(cy(l,e,n,a,o))o.stopPropagation();else if(Yh(e,o),n&4&&-1<ry.indexOf(e)){for(;l!==null;){var i=to(l);if(i!==null)switch(i.tag){case 3:if(i=i.stateNode,i.current.memoizedState.isDehydrated){var u=vt(i.pendingLanes);if(u!==0){var m=i;for(m.pendingLanes|=2,m.entangledLanes|=2;u;){var A=1<<31-ct(u);m.entanglements[1]|=A,u&=~A}Gn(i),(Xe&6)===0&&(Di=ve()+500,Nl(0))}}break;case 31:case 13:m=Ua(i,2),m!==null&&nn(m,i,2),ki(),pc(i,2)}if(i=fc(o),i===null&&Fr(e,n,o,Qi,a),i===l)break;l=i}l!==null&&o.stopPropagation()}else Fr(e,n,o,null,a)}}function fc(e){return e=gs(e),mc(e)}var Qi=null;function mc(e){if(Qi=null,e=eo(e),e!==null){var n=p(e);if(n===null)e=null;else{var a=n.tag;if(a===13){if(e=b(n),e!==null)return e;e=null}else if(a===31){if(e=g(n),e!==null)return e;e=null}else if(a===3){if(n.stateNode.current.memoizedState.isDehydrated)return n.tag===3?n.stateNode.containerInfo:null;e=null}else n!==e&&(e=null)}}return Qi=e,null}function Lh(e){switch(e){case"beforetoggle":case"cancel":case"click":case"close":case"contextmenu":case"copy":case"cut":case"auxclick":case"dblclick":case"dragend":case"dragstart":case"drop":case"focusin":case"focusout":case"input":case"invalid":case"keydown":case"keypress":case"keyup":case"mousedown":case"mouseup":case"paste":case"pause":case"play":case"pointercancel":case"pointerdown":case"pointerup":case"ratechange":case"reset":case"resize":case"seeked":case"submit":case"toggle":case"touchcancel":case"touchend":case"touchstart":case"volumechange":case"change":case"selectionchange":case"textInput":case"compositionstart":case"compositionend":case"compositionupdate":case"beforeblur":case"afterblur":case"beforeinput":case"blur":case"fullscreenchange":case"focus":case"hashchange":case"popstate":case"select":case"selectstart":return 2;case"drag":case"dragenter":case"dragexit":case"dragleave":case"dragover":case"mousemove":case"mouseout":case"mouseover":case"pointermove":case"pointerout":case"pointerover":case"scroll":case"touchmove":case"wheel":case"mouseenter":case"mouseleave":case"pointerenter":case"pointerleave":return 8;case"message":switch(yn()){case bn:return 2;case Et:return 8;case Tt:case Pe:return 32;case Nt:return 268435456;default:return 32}default:return 32}}var gc=!1,Aa=null,Ca=null,ja=null,Ol=new Map,Rl=new Map,Da=[],ry="mousedown mouseup touchcancel touchend touchstart auxclick dblclick pointercancel pointerdown pointerup dragend dragstart drop compositionend compositionstart keydown keypress keyup input textInput copy cut paste click change contextmenu reset".split(" ");function Yh(e,n){switch(e){case"focusin":case"focusout":Aa=null;break;case"dragenter":case"dragleave":Ca=null;break;case"mouseover":case"mouseout":ja=null;break;case"pointerover":case"pointerout":Ol.delete(n.pointerId);break;case"gotpointercapture":case"lostpointercapture":Rl.delete(n.pointerId)}}function zl(e,n,a,o,l,i){return e===null||e.nativeEvent!==i?(e={blockedOn:n,domEventName:a,eventSystemFlags:o,nativeEvent:i,targetContainers:[l]},n!==null&&(n=to(n),n!==null&&Hh(n)),e):(e.eventSystemFlags|=o,n=e.targetContainers,l!==null&&n.indexOf(l)===-1&&n.push(l),e)}function cy(e,n,a,o,l){switch(n){case"focusin":return Aa=zl(Aa,e,n,a,o,l),!0;case"dragenter":return Ca=zl(Ca,e,n,a,o,l),!0;case"mouseover":return ja=zl(ja,e,n,a,o,l),!0;case"pointerover":var i=l.pointerId;return Ol.set(i,zl(Ol.get(i)||null,e,n,a,o,l)),!0;case"gotpointercapture":return i=l.pointerId,Rl.set(i,zl(Rl.get(i)||null,e,n,a,o,l)),!0}return!1}function qh(e){var n=eo(e.target);if(n!==null){var a=p(n);if(a!==null){if(n=a.tag,n===13){if(n=b(a),n!==null){e.blockedOn=n,Xt(e.priority,function(){Uh(a)});return}}else if(n===31){if(n=g(a),n!==null){e.blockedOn=n,Xt(e.priority,function(){Uh(a)});return}}else if(n===3&&a.stateNode.current.memoizedState.isDehydrated){e.blockedOn=a.tag===3?a.stateNode.containerInfo:null;return}}}e.blockedOn=null}function Ki(e){if(e.blockedOn!==null)return!1;for(var n=e.targetContainers;0<n.length;){var a=fc(e.nativeEvent);if(a===null){a=e.nativeEvent;var o=new a.constructor(a.type,a);ms=o,a.target.dispatchEvent(o),ms=null}else return n=to(a),n!==null&&Hh(n),e.blockedOn=a,!1;n.shift()}return!0}function Ih(e,n,a){Ki(e)&&a.delete(n)}function uy(){gc=!1,Aa!==null&&Ki(Aa)&&(Aa=null),Ca!==null&&Ki(Ca)&&(Ca=null),ja!==null&&Ki(ja)&&(ja=null),Ol.forEach(Ih),Rl.forEach(Ih)}function $i(e,n){e.blockedOn===n&&(e.blockedOn=null,gc||(gc=!0,r.unstable_scheduleCallback(r.unstable_NormalPriority,uy)))}var Pi=null;function Xh(e){Pi!==e&&(Pi=e,r.unstable_scheduleCallback(r.unstable_NormalPriority,function(){Pi===e&&(Pi=null);for(var n=0;n<e.length;n+=3){var a=e[n],o=e[n+1],l=e[n+2];if(typeof o!="function"){if(mc(o||a)===null)continue;break}var i=to(a);i!==null&&(e.splice(n,3),n-=3,fr(i,{pending:!0,data:l,method:a.method,action:o},o,l))}}))}function Go(e){function n(A){return $i(A,e)}Aa!==null&&$i(Aa,e),Ca!==null&&$i(Ca,e),ja!==null&&$i(ja,e),Ol.forEach(n),Rl.forEach(n);for(var a=0;a<Da.length;a++){var o=Da[a];o.blockedOn===e&&(o.blockedOn=null)}for(;0<Da.length&&(a=Da[0],a.blockedOn===null);)qh(a),a.blockedOn===null&&Da.shift();if(a=(e.ownerDocument||e).$$reactFormReplay,a!=null)for(o=0;o<a.length;o+=3){var l=a[o],i=a[o+1],u=l[$t]||null;if(typeof i=="function")u||Xh(a);else if(u){var m=null;if(i&&i.hasAttribute("formAction")){if(l=i,u=i[$t]||null)m=u.formAction;else if(mc(l)!==null)continue}else m=u.action;typeof m=="function"?a[o+1]=m:(a.splice(o,3),o-=3),Xh(a)}}}function Jh(){function e(i){i.canIntercept&&i.info==="react-transition"&&i.intercept({handler:function(){return new Promise(function(u){return l=u})},focusReset:"manual",scroll:"manual"})}function n(){l!==null&&(l(),l=null),o||setTimeout(a,20)}function a(){if(!o&&!navigation.transition){var i=navigation.currentEntry;i&&i.url!=null&&navigation.navigate(i.url,{state:i.getState(),info:"react-transition",history:"replace"})}}if(typeof navigation=="object"){var o=!1,l=null;return navigation.addEventListener("navigate",e),navigation.addEventListener("navigatesuccess",n),navigation.addEventListener("navigateerror",n),setTimeout(a,100),function(){o=!0,navigation.removeEventListener("navigate",e),navigation.removeEventListener("navigatesuccess",n),navigation.removeEventListener("navigateerror",n),l!==null&&(l(),l=null)}}}function yc(e){this._internalRoot=e}Wi.prototype.render=yc.prototype.render=function(e){var n=this._internalRoot;if(n===null)throw Error(c(409));var a=n.current,o=hn();Bh(a,o,e,n,null,null)},Wi.prototype.unmount=yc.prototype.unmount=function(){var e=this._internalRoot;if(e!==null){this._internalRoot=null;var n=e.containerInfo;Bh(e.current,2,null,e,null,null),ki(),n[Fa]=null}};function Wi(e){this._internalRoot=e}Wi.prototype.unstable_scheduleHydration=function(e){if(e){var n=lt();e={blockedOn:null,target:e,priority:n};for(var a=0;a<Da.length&&n!==0&&n<Da[a].priority;a++);Da.splice(a,0,e),a===0&&qh(e)}};var Vh=t.version;if(Vh!=="19.2.4")throw Error(c(527,Vh,"19.2.4"));R.findDOMNode=function(e){var n=e._reactInternals;if(n===void 0)throw typeof e.render=="function"?Error(c(188)):(e=Object.keys(e).join(","),Error(c(268,e)));return e=y(n),e=e!==null?w(e):null,e=e===null?null:e.stateNode,e};var dy={bundleType:0,version:"19.2.4",rendererPackageName:"react-dom",currentDispatcherRef:k,reconcilerVersion:"19.2.4"};if(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__<"u"){var Fi=__REACT_DEVTOOLS_GLOBAL_HOOK__;if(!Fi.isDisabled&&Fi.supportsFiber)try{ft=Fi.inject(dy),Ye=Fi}catch{}}return Bl.createRoot=function(e,n){if(!d(e))throw Error(c(299));var a=!1,o="",l=Fd,i=ep,u=tp;return n!=null&&(n.unstable_strictMode===!0&&(a=!0),n.identifierPrefix!==void 0&&(o=n.identifierPrefix),n.onUncaughtError!==void 0&&(l=n.onUncaughtError),n.onCaughtError!==void 0&&(i=n.onCaughtError),n.onRecoverableError!==void 0&&(u=n.onRecoverableError)),n=Rh(e,1,!1,null,null,a,o,null,l,i,u,Jh),e[Fa]=n.current,Wr(e),new yc(n)},Bl.hydrateRoot=function(e,n,a){if(!d(e))throw Error(c(299));var o=!1,l="",i=Fd,u=ep,m=tp,A=null;return a!=null&&(a.unstable_strictMode===!0&&(o=!0),a.identifierPrefix!==void 0&&(l=a.identifierPrefix),a.onUncaughtError!==void 0&&(i=a.onUncaughtError),a.onCaughtError!==void 0&&(u=a.onCaughtError),a.onRecoverableError!==void 0&&(m=a.onRecoverableError),a.formState!==void 0&&(A=a.formState)),n=Rh(e,1,!0,n,a??null,o,l,A,i,u,m,Jh),n.context=zh(null),a=n.current,o=hn(),o=ra(o),l=ma(o),l.callback=null,ga(a,l,o),a=o,n.current.lanes=a,at(n,a),Gn(n),e[Fa]=n.current,Wr(e),new Wi(n)},Bl.version="19.2.4",Bl}var $h;function By(){if($h)return bc.exports;$h=1;function r(){if(!(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__>"u"||typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE!="function"))try{__REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE(r)}catch(t){console.error(t)}}return r(),bc.exports=zy(),bc.exports}var Gy=By(),xc={exports:{}},Sc={};/**
 * @license React
 * react-compiler-runtime.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Ph;function Hy(){if(Ph)return Sc;Ph=1;var r=kf().__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE;return Sc.c=function(t){return r.H.useMemoCache(t)},Sc}var Wh;function Uy(){return Wh||(Wh=1,xc.exports=Hy()),xc.exports}var Se=Uy();const Ly="_wrapper_bt1w8_2",Yy="_header_bt1w8_10",qy="_headerActions_bt1w8_21",Iy="_title_bt1w8_27",Xy="_panelGroup_bt1w8_36",Jy="_clipboardToggle_bt1w8_43",Vy="_helpToggle_bt1w8_66",Zy="_helpButtonWrapper_bt1w8_93",Qy="_helpTogglePulsing_bt1w8_97",Ky="_helpHint_bt1w8_112",$y="_helpHintFading_bt1w8_139",Py="_helpHintKbd_bt1w8_144",Wy="_resizeHandle_bt1w8_153",an={wrapper:Ly,header:Yy,headerActions:qy,title:Iy,panelGroup:Xy,clipboardToggle:Jy,helpToggle:Vy,helpButtonWrapper:Zy,helpTogglePulsing:Qy,helpHint:Ky,helpHintFading:$y,helpHintKbd:Py,resizeHandle:Wy},Fy=r=>{try{return!new DOMParser().parseFromString(r.trim(),"text/xml").querySelector("parsererror")}catch{return!1}},e1=r=>{try{return JSON.parse(r),!0}catch{return!1}},t1=r=>r.trim()?e1(r)?{valid:!0,error:null,type:"json"}:Fy(r)?{valid:!0,error:null,type:"xml"}:{valid:!1,error:"Invalid JSON/XML format",type:null}:{valid:!0,error:null,type:null},Bc=r=>{try{const t=JSON.parse(r);return JSON.stringify(t,null,2)}catch{return r}},n1=()=>{const r=Se.c(8);let t;r[0]===Symbol.for("react.memo_cache_sentinel")?(t=[],r[0]=t):t=r[0];const[s,c]=x.useState(t),d=x.useRef(0);let p;r[1]===Symbol.for("react.memo_cache_sentinel")?(p=new Set,r[1]=p):p=r[1];const b=x.useRef(p);let g,f;r[2]===Symbol.for("react.memo_cache_sentinel")?(g=()=>()=>{b.current.forEach(clearTimeout)},f=[],r[2]=g,r[3]=f):(g=r[2],f=r[3]),x.useEffect(g,f);let y;r[4]===Symbol.for("react.memo_cache_sentinel")?(y=(N,E)=>{const _=E===void 0?"info":E,T=d.current=d.current+1;c(j=>[...j,{id:T,message:N,type:_}]);const C=setTimeout(()=>{b.current.delete(C),c(j=>j.filter(L=>L.id!==T))},3e3);b.current.add(C)},r[4]=y):y=r[4];const w=y;let v;r[5]===Symbol.for("react.memo_cache_sentinel")?(v=N=>{c(E=>E.filter(_=>_.id!==N))},r[5]=v):v=r[5];const S=v;let D;return r[6]!==s?(D={toasts:s,addToast:w,removeToast:S},r[6]=s,r[7]=D):D=r[7],D},Oa=(r,t)=>{const s=x.useCallback(()=>{try{const p=window.localStorage.getItem(r);return p?JSON.parse(p):t}catch{return t}},[r]),[c,d]=x.useState(s);return x.useEffect(()=>{d(s())},[r]),x.useEffect(()=>{try{window.localStorage.setItem(r,JSON.stringify(c))}catch(p){console.error(`Error setting localStorage key "${r}":`,p)}},[r,c]),x.useEffect(()=>{const p=b=>{(b.key===r||b.key===null)&&d(s())};return window.addEventListener("storage",p),()=>window.removeEventListener("storage",p)},[r,s]),x.useEffect(()=>{const p=()=>d(s());return window.addEventListener("focus",p),document.addEventListener("visibilitychange",p),()=>{window.removeEventListener("focus",p),document.removeEventListener("visibilitychange",p)}},[s]),[c,d]},a1=200,Fh=50,o1=8,l1=63488,i1=2e4,ia=[{path:"/json-path",label:"JSON-Path",title:"JSON-Path Playground",wsPath:"/ws/json/path",storageKeyPayload:"jsonpath-last-payload",storageKeyHistory:"jsonpath-command-history",storageKeyTab:"jsonpath-right-tab",supportsUpload:!0,tabs:["payload","graph","graph-data"]},{path:"/",label:"Minigraph",title:"Minigraph Playground",wsPath:"/ws/graph/playground",storageKeyPayload:"minigraph-last-payload",storageKeyHistory:"minigraph-command-history",storageKeyTab:"minigraph-right-tab",storageKeySavedGraphs:"minigraph-saved-graphs",storageKeyHelpTopic:"minigraph-help-topic",supportsClipboard:!0,supportsHelp:!0,supportsAuthoring:!0,tabs:["graph","graph-data"]}],ns={json_simple:JSON.stringify({name:"John Doe",age:30,city:"New York"},null,2),json_nested:JSON.stringify({user:{name:"Jane Smith",profile:{email:"jane@example.com",address:{city:"San Francisco",country:"USA"}}}},null,2),json_array:JSON.stringify([{id:1,name:"Item 1",status:"active"},{id:2,name:"Item 2",status:"pending"},{id:3,name:"Item 3",status:"inactive"}],null,2),xml_simple:`<?xml version="1.0" encoding="UTF-8"?>
<person>
  <name>John Doe</name>
  <age>30</age>
  <city>New York</city>
</person>`,xml_nested:`<?xml version="1.0" encoding="UTF-8"?>
<user>
  <name>Jane Smith</name>
  <profile>
    <email>jane@example.com</email>
    <address>
      <city>San Francisco</city>
      <country>USA</country>
    </address>
  </profile>
</user>`,xml_array:`<?xml version="1.0" encoding="UTF-8"?>
<items>
  <item>
    <id>1</id>
    <name>Item 1</name>
    <status>active</status>
  </item>
  <item>
    <id>2</id>
    <name>Item 2</name>
    <status>pending</status>
  </item>
  <item>
    <id>3</id>
    <name>Item 3</name>
    <status>inactive</status>
  </item>
</items>`};function zf(r){return`ws://${window.location.host}${r}`}function wc(r,t,s,c){const d=r[t]??{phase:"idle",messages:[]},p=[...d.messages,{id:s,raw:c}];return p.length>a1&&p.shift(),{...r,[t]:{...d,messages:p}}}function s1(r,t){const s=r[t.path]??{phase:"idle",messages:[]};switch(t.type){case"CONNECTING":return{...r,[t.path]:{...s,phase:"connecting"}};case"CONNECTED":return wc({...r,[t.path]:{...s,phase:"connected"}},t.path,t.id,t.msg);case"MESSAGE_RECEIVED":return wc(r,t.path,t.id,t.msg);case"DISCONNECTED":return wc({...r,[t.path]:{...s,phase:"idle"}},t.path,t.id,t.msg);case"CONNECT_ERROR":return{...r,[t.path]:{...s,phase:"idle"}};case"CLEAR_MESSAGES":return{...r,[t.path]:{...s,messages:[]}};default:return r}}const Bf=x.createContext(null);function r1({children:r}){const[t,s]=x.useReducer(s1,{}),c=x.useRef({}),d=x.useRef({}),p=x.useRef({});x.useEffect(()=>()=>{Object.entries(c.current).forEach(([M,X])=>{X==null||X.close();const P=d.current[M];P&&clearInterval(P)})},[]);const b=M=>zf(M),g=M=>(p.current[M]=(p.current[M]??0)+1,p.current[M]),f=()=>{const M=new Date().toString(),X=M.indexOf("GMT");return X>0?M.substring(0,X).trim():M},y=(M,X)=>JSON.stringify({type:M,message:X,time:f()}),w=M=>{try{const X=JSON.parse(M);if(X!==null&&typeof X=="object"){const P=X.type;return P==="ping"||P==="pong"}}catch{}return!1},v=x.useCallback((M,X)=>{if(!window.WebSocket){X==null||X("WebSocket not supported by your browser","error");return}const P=c.current[M];if(P&&(P.readyState===WebSocket.OPEN||P.readyState===WebSocket.CONNECTING)){X==null||X("Already connected","error");return}s({type:"CONNECTING",path:M});const z=new WebSocket(b(M));c.current[M]=z,z.onopen=()=>{s({type:"CONNECTED",path:M,id:g(M),msg:y("info","connected")}),X==null||X("Connected to WebSocket","success"),z.send(JSON.stringify({type:"welcome"})),d.current[M]=setInterval(()=>{z.readyState===WebSocket.OPEN&&z.send(y("ping","keep alive"))},i1)},z.onmessage=H=>{w(H.data)||s({type:"MESSAGE_RECEIVED",path:M,id:g(M),msg:H.data})},z.onerror=()=>{s({type:"CONNECT_ERROR",path:M})},z.onclose=H=>{const G=d.current[M];G&&(clearInterval(G),d.current[M]=null),s({type:"DISCONNECTED",path:M,id:g(M),msg:y("info",`disconnected - (${H.code}) ${H.reason}`)}),X==null||X("Disconnected from WebSocket","info"),c.current[M]===z&&(c.current[M]=null)}},[]),S=x.useCallback(M=>{const X=c.current[M];X?X.close():s({type:"MESSAGE_RECEIVED",path:M,id:g(M),msg:y("error","already disconnected")})},[]);x.useEffect(()=>(ia.forEach(M=>{v(M.wsPath)}),()=>{ia.forEach(M=>{const X=c.current[M.wsPath];X&&X.close()})}),[]);const D=x.useCallback((M,X)=>{const P=c.current[M];return P&&P.readyState===WebSocket.OPEN?(P.send(X),!0):!1},[]),N=x.useCallback((M,X)=>{s({type:"MESSAGE_RECEIVED",path:M,id:g(M),msg:X})},[]),E=x.useCallback(M=>{s({type:"CLEAR_MESSAGES",path:M})},[]),[_,T]=x.useState({}),C=x.useCallback((M,X)=>{T(P=>{if(X===null){const z={...P};return delete z[M],z}return{...P,[M]:X}})},[]),j=x.useCallback(M=>_[M]??null,[_]),L=x.useCallback(M=>{const X=_[M]??null;return X!==null&&T(P=>{const z={...P};return delete z[M],z}),X},[_]),U=x.useCallback(M=>t[M]??{phase:"idle",messages:[]},[t]),V=x.useMemo(()=>({getSlot:U,connect:v,disconnect:S,send:D,appendMessage:N,clearMessages:E,setPendingPayload:C,peekPendingPayload:j,takePendingPayload:L}),[U,v,S,D,N,E,C,j,L]);return h.jsx(Bf.Provider,{value:V,children:r})}function Pc(){const r=x.useContext(Bf);if(!r)throw new Error("useWebSocketContext must be used inside <WebSocketProvider>");return r}const c1=r=>{try{const t=JSON.parse(r);return{type:t.type||"info",message:t.message||r,time:t.time,raw:r}}catch{return{type:"raw",message:r,time:null,raw:r}}},u1=r=>({info:"ℹ️",error:"❌",ping:"🔄",welcome:"👋",raw:""})[r]??"•",Ll=r=>{try{const t=JSON.parse(r);if(typeof t=="object"&&t!==null)return{isJSON:!0,data:t}}catch{}return{isJSON:!1,data:null}};function d1(r){if(!r.includes("Graph exported to "))return null;const t=Fc(r);if(!t)return null;const s=t.split("/")[4];return s?{graphName:s,apiPath:t}:null}function p1(r){return r.includes("Invalid filename")?{reason:"invalid-name"}:r.includes("Expect root node name")?{reason:"root-name-conflict"}:null}function Wc(r){const t=Ll(r);return t.isJSON?(typeof t.data.type=="string",!1):!0}function Fc(r){const t=r.match(/\/api\/graph\/model\/([^\s'"]+)/);return t?t[0]:null}function Gf(r){return Wc(r)?Fc(r)!==null:!1}function Hf(r){const t=r.match(/\/api\/json\/content\/([\w-]+)/);return t?t[0]:null}function h1(r){const t=r.match(/Large payload \((\d+)\)\s*->\s*GET\s+(\/api\/inspect\/[^\s]+)/i);if(!t)return null;const s=parseInt(t[1],10),c=t[2],p=`${c.split("/").filter(Boolean).pop()??"payload"}.json`;return{apiPath:c,byteSize:s,filename:p}}function f1(r){const t=r.match(/You may upload .*?->\s*POST\s+(\/api\/mock\/[\w-]+)/i);return t?t[1]:null}function m1(r){if(!r.startsWith("> "))return!1;const t=r.slice(2).trim().toLowerCase();return t==="help"||t.startsWith("help ")?!0:t.startsWith("describe ")?!t.slice(9).trim().startsWith("graph"):!1}function g1(r){if(!r.startsWith("> ")||!r.slice(2).trimStart().toLowerCase().startsWith("import graph from "))return null;const s=r.slice(2).trimStart().slice(18).trim();return s.length>0?s:null}const y1=/^node ([A-Za-z0-9_-]+) created$/i,b1=/^node ([A-Za-z0-9_-]+) already exists$/i,v1=/^node ([A-Za-z0-9_-]+) updated$/i,_1=/^node ([A-Za-z0-9_-]+) deleted$/i,x1=/^node ([A-Za-z0-9_-]+) not found$/i,S1=/^ERROR: (.+)$/;function w1(r){const t=r.trim();if(t.startsWith("> "))return null;const s=t.match(y1);if(s)return{status:"accepted",action:"create-node",alias:s[1],message:t};const c=t.match(b1);if(c)return{status:"rejected",action:"create-node",alias:c[1],message:t};const d=t.match(v1);if(d)return{status:"accepted",action:"edit-node",alias:d[1],message:t};const p=t.match(_1);if(p)return{status:"accepted",action:"delete-node",alias:p[1],message:t};const b=t.match(x1);return b?{status:"rejected",action:null,alias:b[1],message:t}:t.match(S1)?{status:"error",action:null,alias:null,message:t}:null}function E1(r){if(!Wc(r)||r.startsWith("> ")||Gf(r))return null;const t=r.toLowerCase();return t.includes("graph model imported as draft")?"import-graph":t.includes(" -> ")&&t.includes("removed")||t.startsWith("node ")&&(t.includes(" created")||t.includes(" updated")||t.includes(" deleted")||t.includes(" connected to ")||t.includes(" imported from ")||t.includes(" overwritten by node from "))?"node-mutation":null}const T1={command:"",historyIndex:-1,draftCommand:""};function N1(r,t){switch(t.type){case"SET_COMMAND":return{...r,command:t.value,historyIndex:-1,draftCommand:""};case"CLEAR_COMMAND":return{...r,command:"",historyIndex:-1,draftCommand:""};case"SET_HISTORY_INDEX":return{...r,historyIndex:t.index,command:t.command};case"ENTER_HISTORY":return{...r,historyIndex:0,command:t.command,draftCommand:r.command};case"EXIT_HISTORY":return{...r,historyIndex:-1,command:r.draftCommand,draftCommand:""};default:return r}}function A1(r){const t=Se.c(77),{wsPath:s,storageKeyHistory:c,payload:d,addToast:p,bus:b,handleLocalCommand:g}=r,f=Pc();let y;t[0]!==f||t[1]!==s?(y=f.getSlot(s),t[0]=f,t[1]=s,t[2]=y):y=t[2];const{phase:w,messages:v}=y,S=w==="connected",D=w==="connecting",[N,E]=x.useReducer(N1,T1),{command:_,historyIndex:T}=N;let C;t[3]===Symbol.for("react.memo_cache_sentinel")?(C=[],t[3]=C):C=t[3];const[j,L]=Oa(c,C),U=x.useRef(null),V=x.useRef(!1);let M;t[4]===Symbol.for("react.memo_cache_sentinel")?(M=()=>{U.current&&(U.current.scrollTop=U.current.scrollHeight)},t[4]=M):M=t[4];let X;t[5]!==v?(X=[v],t[5]=v,t[6]=X):X=t[6],x.useEffect(M,X);let P;t[7]!==p||t[8]!==f||t[9]!==s?(P=()=>{f.connect(s,p)},t[7]=p,t[8]=f,t[9]=s,t[10]=P):P=t[10];const z=P;let H;t[11]!==f||t[12]!==s?(H=()=>{f.disconnect(s)},t[11]=f,t[12]=s,t[13]=H):H=t[13];const G=H;let Q;t[14]!==_||t[15]!==f||t[16]!==g||t[17]!==j||t[18]!==d||t[19]!==w||t[20]!==L||t[21]!==s?(Q=()=>{if(w!=="connected")return;const te=_.trim();if(te.length!==0){if((g==null?void 0:g(te))===!0){j[0]!==te&&L(pe=>[te,...pe].slice(0,Fh)),f.appendMessage(s,"> "+te),E({type:"CLEAR_COMMAND"});return}f.send(s,te),j[0]!==te&&L(pe=>[te,...pe].slice(0,Fh)),te==="load"&&(d.length===0?f.appendMessage(s,"ERROR: please paste JSON/XML payload in input text area"):f.send(s,d)),E({type:"CLEAR_COMMAND"})}},t[14]=_,t[15]=f,t[16]=g,t[17]=j,t[18]=d,t[19]=w,t[20]=L,t[21]=s,t[22]=Q):Q=t[22];const Z=Q;let ne;t[23]!==j||t[24]!==T?(ne=te=>{if(te.key==="ArrowUp"){if(te.preventDefault(),j.length===0)return;if(T===-1)E({type:"ENTER_HISTORY",command:j[0]});else if(T<j.length-1){const pe=T+1;E({type:"SET_HISTORY_INDEX",index:pe,command:j[pe]})}}else if(te.key==="ArrowDown")if(te.preventDefault(),T<=0)T===0&&E({type:"EXIT_HISTORY"});else{const pe=T-1;E({type:"SET_HISTORY_INDEX",index:pe,command:j[pe]})}},t[23]=j,t[24]=T,t[25]=ne):ne=t[25];const k=ne;let R,$;t[26]!==p||t[27]!==b||t[28]!==f||t[29]!==d||t[30]!==s?($=()=>{if(b)return b.on("upload.contentPath",te=>{if(!V.current)return;if(V.current=!1,d.length===0){f.appendMessage(s,"ERROR: please paste JSON/XML payload in the input text area");return}let pe;try{pe=JSON.stringify(JSON.parse(d))}catch{f.appendMessage(s,"ERROR: payload is not valid JSON — cannot upload");return}fetch(te.uploadPath,{method:"POST",headers:{"Content-Type":"application/json"},body:pe}).then(we=>{if(!we.ok)throw new Error(`HTTP ${we.status}`);p("Payload uploaded successfully","success")}).catch(we=>{f.appendMessage(s,`ERROR: upload failed — ${we.message}`),p(`Upload failed: ${we.message}`,"error")})})},R=[b,d,s,f,p],t[26]=p,t[27]=b,t[28]=f,t[29]=d,t[30]=s,t[31]=R,t[32]=$):(R=t[31],$=t[32]),x.useEffect($,R);let ee,ae;t[33]!==p||t[34]!==b||t[35]!==f||t[36]!==v||t[37]!==d||t[38]!==s?(ee=()=>{if(b||!V.current||v.length===0)return;const te=v[v.length-1].raw,pe=Hf(te);if(!pe)return;if(V.current=!1,d.length===0){f.appendMessage(s,"ERROR: please paste JSON/XML payload in the input text area");return}let we;try{we=JSON.stringify(JSON.parse(d))}catch{f.appendMessage(s,"ERROR: payload is not valid JSON — cannot upload");return}fetch(pe,{method:"POST",headers:{"Content-Type":"application/json"},body:we}).then(ye=>{if(!ye.ok)throw new Error(`HTTP ${ye.status}`);p("Payload uploaded successfully","success")}).catch(ye=>{f.appendMessage(s,`ERROR: upload failed — ${ye.message}`),p(`Upload failed: ${ye.message}`,"error")})},ae=[b,v,d,s,f,p],t[33]=p,t[34]=b,t[35]=f,t[36]=v,t[37]=d,t[38]=s,t[39]=ee,t[40]=ae):(ee=t[39],ae=t[40]),x.useEffect(ee,ae);let le;t[41]!==p||t[42]!==f||t[43]!==d||t[44]!==w||t[45]!==s?(le=()=>{if(w==="connected"){if(d.length===0){p("Nothing to upload — paste a JSON payload first","error");return}V.current=!0,f.send(s,"upload")}},t[41]=p,t[42]=f,t[43]=d,t[44]=w,t[45]=s,t[46]=le):le=t[46];const re=le;let ie;t[47]!==f||t[48]!==w||t[49]!==s?(ie=te=>w!=="connected"?!1:f.send(s,te),t[47]=f,t[48]=w,t[49]=s,t[50]=ie):ie=t[50];const de=ie;let oe;t[51]!==p||t[52]!==v?(oe=()=>{navigator.clipboard.writeText(v.map(C1).join(`
`)),p("Console copied to clipboard!","success")},t[51]=p,t[52]=v,t[53]=oe):oe=t[53];const se=oe;let xe;t[54]!==p||t[55]!==f||t[56]!==s?(xe=()=>{f.clearMessages(s),p("Console cleared","info")},t[54]=p,t[55]=f,t[56]=s,t[57]=xe):xe=t[57];const ze=xe;let _e;t[58]!==f||t[59]!==s?(_e=te=>{f.appendMessage(s,te)},t[58]=f,t[59]=s,t[60]=_e):_e=t[60];const Je=_e;let je;t[61]===Symbol.for("react.memo_cache_sentinel")?(je=te=>E({type:"SET_COMMAND",value:te}),t[61]=je):je=t[61];const ke=je;let Me;return t[62]!==Je||t[63]!==ze||t[64]!==_||t[65]!==z||t[66]!==S||t[67]!==D||t[68]!==se||t[69]!==G||t[70]!==k||t[71]!==j||t[72]!==v||t[73]!==Z||t[74]!==de||t[75]!==re?(Me={connected:S,connecting:D,messages:v,command:_,setCommand:ke,connect:z,disconnect:G,sendCommand:Z,handleKeyDown:k,consoleRef:U,copyMessages:se,clearMessages:ze,uploadPayload:re,sendRawText:de,appendMessage:Je,history:j},t[62]=Je,t[63]=ze,t[64]=_,t[65]=z,t[66]=S,t[67]=D,t[68]=se,t[69]=G,t[70]=k,t[71]=j,t[72]=v,t[73]=Z,t[74]=de,t[75]=re,t[76]=Me):Me=t[76],Me}function C1(r){return r.raw}function j1(r){const t=Se.c(5);let s;t[0]!==r?(s=()=>window.matchMedia(r).matches,t[0]=r,t[1]=s):s=t[1];const[c,d]=x.useState(s);let p,b;return t[2]!==r?(p=()=>{const g=window.matchMedia(r),f=y=>d(y.matches);return g.addEventListener("change",f),()=>g.removeEventListener("change",f)},b=[r],t[2]=r,t[3]=p,t[4]=b):(p=t[3],b=t[4]),x.useEffect(p,b),c}function ef(r){return typeof r!="object"||r===null?!1:Array.isArray(r.nodes)}function Ec(r,t,s){const c=t.includes(s)?s:t[0]??"graph";return typeof r=="string"&&t.includes(r)?r:c}function D1(r,t,s,c,d){const[p,b]=x.useState(null),[g,f]=Oa(d,s),y=Ec(g,c,s),[w,v]=x.useState(!1),S=x.useCallback(_=>{f(T=>{const C=Ec(T,c,s),j=typeof _=="function"?_(C):_;return Ec(j,c,s)})},[f,c,s]);x.useEffect(()=>{g!==y&&f(y)},[g,y,f]);const D=x.useRef(r);x.useEffect(()=>{D.current=r},[r]);const N=x.useRef(null);x.useEffect(()=>{if(!r)return;const _=new AbortController;return b(null),fetch(r,{signal:_.signal}).then(T=>{if(!T.ok)throw new Error(`HTTP ${T.status}`);return T.json()}).then(T=>{ef(T)&&(b(T),S("graph"))}).catch(T=>{T.name!=="AbortError"&&t(`Graph fetch failed: ${T.message}`,"error")}),()=>{_.abort()}},[r,t]);const E=x.useCallback(()=>{var C;const _=D.current;if(!_)return;(C=N.current)==null||C.abort();const T=new AbortController;N.current=T,v(!0),fetch(_,{signal:T.signal}).then(j=>{if(!j.ok)throw new Error(`HTTP ${j.status}`);return j.json()}).then(j=>{ef(j)&&b(j),v(!1)}).catch(j=>{j.name!=="AbortError"&&(t(`Graph refresh failed: ${j.message}`,"error"),v(!1))})},[]);return x.useEffect(()=>()=>{var _;(_=N.current)==null||_.abort()},[]),{graphData:p,setGraphData:b,rightTab:y,setRightTab:S,isRefreshing:w,refetchGraph:E}}function M1(r){const t=Se.c(22),{bus:s,pinnedGraphPath:c,setPinnedGraphPath:d,connected:p,sendRawText:b,addToast:g}=r,f=x.useRef(null),y=x.useRef(!1),w=x.useRef(c),v=x.useRef(p),S=x.useRef(b);let D,N;t[0]!==c?(D=()=>{w.current=c},N=[c],t[0]=c,t[1]=D,t[2]=N):(D=t[1],N=t[2]),x.useEffect(D,N);let E,_;t[3]!==p?(E=()=>{v.current=p},_=[p],t[3]=p,t[4]=E,t[5]=_):(E=t[4],_=t[5]),x.useEffect(E,_);let T,C;t[6]!==b?(T=()=>{S.current=b},C=[b],t[6]=b,t[7]=T,t[8]=C):(T=t[7],C=t[8]),x.useEffect(T,C);let j,L;t[9]!==p?(j=()=>{p||(y.current=!1,f.current!==null&&(clearTimeout(f.current),f.current=null))},L=[p],t[9]=p,t[10]=j,t[11]=L):(j=t[10],L=t[11]),x.useEffect(j,L);let U,V;t[12]!==s||t[13]!==d?(V=()=>s.on("graph.link",H=>{y.current&&(y.current=!1,d(H.apiPath))}),U=[s,d],t[12]=s,t[13]=d,t[14]=U,t[15]=V):(U=t[14],V=t[15]),x.useEffect(V,U);let M,X;t[16]!==g||t[17]!==s?(M=()=>s.on("graph.mutation",H=>{if(v.current){if(H.mutationType==="import-graph"){f.current!==null&&(clearTimeout(f.current),f.current=null),y.current=!0,S.current("describe graph"),g("Graph imported — refreshing view…","info");return}f.current!==null&&clearTimeout(f.current),f.current=setTimeout(()=>{f.current=null,v.current&&(y.current=!0,S.current("describe graph"),g(w.current!==null?"Graph updated — refreshing…":"Graph updated — opening Graph tab…","info"))},300)}}),X=[s,g],t[16]=g,t[17]=s,t[18]=M,t[19]=X):(M=t[18],X=t[19]),x.useEffect(M,X);let P,z;t[20]===Symbol.for("react.memo_cache_sentinel")?(P=()=>()=>{f.current!==null&&clearTimeout(f.current)},z=[],t[20]=P,t[21]=z):(P=t[20],z=t[21]),x.useEffect(P,z)}const k1=`Connect two nodes together
--------------------------
1. Each connection is directional. Connect A to B is different from B to A.
2. A node must connect to one or more nodes. When a graph has orphan nodes, you cannot export the graph for deployment.

Syntax
------
\`\`\`
connect {node-A} to {node-B} with {relation}
\`\`\`
`,O1=`Create a new node
-----------------
1. Root node must use the name 'root' and end node must use 'end'.
2. Skill is a property with the name 'skill'. A node has zero or one skill.
3. The 'create node' is a multi-line command 
4. Properties are optional for a graph model. If present, they are used as default value. 
5. For each property, you can use the "triple single quotes" to enter a multi-line value if needed. 
6. Node name and type should use lower case characters and hyphen only
7. Type and key-values will be used and validated by the node's skill function if any
8. The key of a property can be a composable key using the dot-bracket format.
   The value may use Event Script's constant syntax.

Syntax
------
\`\`\`
create node {name}
with type {type}
with properties
{key1}={value1}
{key2}={value2}
...
\`\`\`

Best practice
-------------
For root node, we recommend adding a "name" property as the graph name and "purpose" property to describe
the use case as a one-liner.

Example
-------
\`\`\`
create node root
with type Root
with properties
name=helloworld
purpose=Demo graph
...
\`\`\`
`,R1=`Data Dictionary
---------------
Based on the MiniGraph technology, the data dictionary method requires (1) Data Dictionary items,
(2) Data Providers and (3) API Fetchers.

1. You can create a node holding a data dictionary item
2. A data dictionary item presents a data attribute that can be retrieved from a data provider using an API fetcher
3. It has 'input' and 'output' statements to define input parameter(s) and output data mapping respectively
4. Default value is supported using the colon (':') character (see example below)

Syntax
------
\`\`\`
create node {name}
with type Dictionary
with properties
purpose={something about this data dictionary item}
provider={data provider}
input[]={parameter}
output[]={data mapping from response object to result set}
\`\`\`

Example
-------
\`\`\`
create node person-name
with type Dictionary
with properties
purpose=name of a person
provider=mdm-profile
input[]=person_id
input[]=detail:true
output[]=response.profile.name -> result.person_name
\`\`\`

Data dictionary node holds key-values and it does not execute by itself. It is used by an API fetcher node.
Instead, the result set will be saved in the API fetcher node.

One or more data dictionary items can share the same data provider. For example, a complex data structure
is returned by a data provider, a single data dictionary item will get one or more data attributes.
If the same input key-values are applied to the same data provider, the API fetcher will only issue a single
API request.

Data Provider
-------------
1. A data provider is also a node
2. It describes the communication protocol with a target system providing a set of data attributes
3. It has 'url', 'method', 'feature', 'and 'input' statements

Syntax
------
\`\`\`
create node {name}
with type Provider
with properties
purpose={something about this provider if any}
url={url to target system}
method={GET | POST | PUT | PATCH | HEAD, etc.}
feature[]={authentication mechanism, encryption, etc.}
input[]={source -> target}
\`\`\`

Feature
-------
The list contains one of more optional features that an API fetcher using this provider must support.

Two built-in features are \`log-request-headers\` and \`log-response-headers\`. When these features are included, 
the fetcher will log request/response headers into the "header" section of its properties.

Input data mapping
------------------
The input data mapping is designed to do simple mapping with the following restriction:
- The left hand side (source) is limited to parameter of the data dictionary item or constants
- The right hand side (target) is allowed to use the following namespaces:

*Left hand side*

1. Constant
2. Input parameter for a data dictionary
3. Other value that is available in the state machine. e.g. "model." namespace.

*Right hand side*

1. \`body.\` - request body
2. \`header.\` - request header
3. \`query.\` - request query parameter
4. \`path_parameter.\` - URI path parameter

The following two examples illustrate a data provider configuration for a hypothetical profile management system

Example one
-----------
In the first example, it maps the parameter 'person_id' of the data dictionary to the path parameter 'id'.
It also maps the parameter 'detail' of the data dictionary to the query parameter 'id'

\`\`\`
create node mdm-profile
with type Provider
with properties
purpose=MDM profile management system
url=\${HOST}/api/mdm/profile/{id}
method=GET
feature[]=oauth-bearer
input[]=text(application/json) -> header.accept
input[]=person_id -> path_parameter.id
input[]=detail -> query.detail
\`\`\`

Example two
-----------
In the second example, it uses POST method and expects a request body containing the 'person_id' parameter.
Since it is a POST request, it requires the configuration of 'content-type' in the header section.
The 'body.' namespace is used to tell the system to map the input parameter in the API request body.
For some use cases, you may set the input parameter as the whole 'body'.
e.g. setting a string or an array as request body instead of key-values.

The 'feature' statement section contains 'oauth-bearer'. Therefore, you must configure an API fetcher that
supports this feature. Otherwise, the fetcher may throw exception. For demo purpose, we will configure
the 'graph.api.fetcher' that will just print a warning message if the feature is not supported.

Since the MiniGraph Playground system is extensible, you can always write a custom API fetcher to handle
new communication protocols and features.

\`\`\`
create node mdm-profile
with type Provider
with properties
purpose=MDM profile management system
url=\${HOST}/api/mdm/profile
method=POST
feature[]=oauth-bearer
input[]=text(application/json) -> header.accept
input[]=text(application/json) -> header.content-type
input[]=person_id -> body.id
input[]=detail -> query.detail
\`\`\`

API Fetcher
-----------
Data dictionary items are consumed by API fetcher. A built-in API fetcher is called "graph.api.fetcher".

Skill: Graph API Fetcher
------------------------
When a node is configured with this skill of "graph API fetcher", it will make an API call to a backend service
and collect result set into the "result" property of the node. In case of exception, the "status" and "error"
fields will be set to the node's properties and the graph execution will stop.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.api.fetcher"

Setup
-----
To enable this skill for a node, set "skill=graph.api.fetcher" as a property in a node.
It will find out the data provider from a given data dictionary item to make an outgoing API call.

The following are required in the properties of the node:

1. dictionary - this is a list of valid data dictionary node names configured in the same graph model
2. input - one or more data mapping as input parameters to invoke the API call
3. output - one of more data mapping to map result set to another node or the 'output.' namespace

The parameter name in each mapping statement must match that in the data dictionary item.
Otherwise, execution will fail.

The system uses the same syntax of Event Script for data mapping.

Properties
----------
\`\`\`
skill=graph.api.fetcher
dictionary[]={data dictionary item}
input[]={mapping of key-value from input or another node to input parameter(s) of the data dictionary item(s)}
output[]={optional mapping of result set to one or more variables in the 'model.' or 'output.' namespace}
\`\`\`

Optional properties
-------------------
\`\`\`
for_each[]={map a result parameter that is an array into a model variable for iterative API execution}
concurrency={controls parallel API calls for an "iterative API request". Default 3, max 30}
\`\`\`

Dictionary
----------
This list contains one or more data dictionary item (aka 'data attribute')

Feature
-------
This API fetcher supports features configured in a data provider's node.

There are 2 built-in features that are convenience for development and tests:
- log-request-headers
- log-response-headers

When either or both of these features are added to a data provider's node,
the fetcher will log request/response headers into the "header" section
of its properties.

Input/Output Data mapping
-------------------------
source.composite.key -> target.composite.key

For input data mapping, the source can use a key-value from the \`input.\` namespace or another node.
The target can be a key-value in the state machine (\`model.\` namespace) or an input parameter name of the
data dictionary.

For output data mapping, the source can be a key-value from the result set and the target can use
the \`output.\` or \`model.\` namespace.

Output data mapping is optional because you can use another data mapper to map result set of the fetcher
to another node.

Result set
----------
Upon successful execution, the result set will be stored in the "result" parameter in the properties of
the node. A subsequent data mapper can then map the key-values in the result set to one or more nodes.

Example
-------
\`\`\`
create node fetcher-1
with properties
skill=graph.api.fetcher
dictionary[]=person-name
dictionary[]=person-address
dictionary[]=person-accounts
input[]=input.body.person_id -> person_id
output[]=result.person_name -> output.body.name
output[]=result.person_address -> output.body.address
\`\`\`

Iterative API call
------------------
Using the optional \`for_each\` statement, you can tell the API fetcher to do "fork-n-join" of API requests.

A "for_each" statement extracts the next array element from result set of a prior API call into a model variable.
You can then put the model variable in the "left-hand-side" of an input statement. The API fetcher will then
issue multiple API calls using an iterative stream of the model variable.

If your API call needs more than one parameter, you can configure more than one "for_each" statement.

Example
-------
In this example, the "for_each" statement extracts the "person_accounts" from the result of a prior API call
by "fetcher-1" and map the array into an iterative stream of elements using the model variable "account_id".

The concurrency property tells the API fetcher to limit parallelism to avoid overwhelming the target service.
\`\`\`
create node fetcher-2
with properties
skill=graph.api.fetcher
dictionary[]=person-id
dictionary[]=account-id
for_each[]=fetcher-1.result.person_accounts -> model.account_id
concurrency=3
input[]=input.body.person_id -> person_id
input[]=model.account_id -> account_id
output[]=result.person_name -> output.body.name
output[]=result.person_address -> output.body.address
\`\`\`

- The "[]" syntax is used to create and append a list of one or more data mapping entries
- The "->" signature indicates the direction of mapping where the left-hand-side is a source
  and right-hand-side is a target

Caution
-------
API fetchers can be chained together to make multiple API calls.
However, you should design the API chain to be minimalist.

An overly complex chain of API requests would mean slow performance. Just take the minimal set of data that are
required by your application. Don't abuse the flexibility of the API fetcher.
`,z1=`Delete a node, a connection or clear cache
------------------------------------------

Syntax
------
Delete a node
\`\`\`
delete node {name}
\`\`\`

Delete the connections between two nodes if any
\`\`\`
delete connection {nodeA} and {nodeB}
\`\`\`

Clear cache for API fetchers
\`\`\`
clear cache
\`\`\`

Alias
-----
\`clear\` is an alias of \`delete\`
`,B1="Describe graph, node, connection or skill\n-----------------------------------------\n\nSyntax\n------\nShow the structure of the current graph model\n```\ndescribe graph\n```\n\nPrint the structure of a node\n```\ndescribe node {name}\n```\n\nConfirm if there is a connection between node-A and node-B\n```\ndescribe connection {node-A} and {node-B}\n```\n\nSkill description of a specific composable function serving the skill\n```\ndescribe skill {skill.route.name}\n```\n",G1=`Edit a node
-----------
This is a convenience feature to populate an "update node" command with raw input data.

Syntax
------
\`\`\`
edit node {name}
with type {type}
with properties
{key1}={value1}
{key2}={value2}
...
\`\`\`

Example
-------
\`\`\`
edit node demo-node
...
\`\`\`

The above command will print the raw input data of "demo-node" if it exists.
You can then edit the raw input data and submit the update.

Sample output
-------------
\`\`\`
update node demo-node
with type Demo
with properties
hello=world
test='''
this is a sample multiple key-value
line two
line three
'''
good=day
...
\`\`\`
`,H1=`Execute a node with a skill
---------------------------
1. Execution is performed only when the node has a skill
2. The skill property must contain only one skill route
3. The system will invoke the skill providing function
4. Graph traversal is disabled to isolate the execution for functional verification

Syntax
------
\`\`\`
execute node {name}
\`\`\`

Short form
----------
\`\`\`
execute {node-name}
\`\`\`
`,U1=`Export a graph model
--------------------
1. This command exports a graph as a model in JSON format for deployment
2. The name does not require the ".json" extension

Syntax
------
\`\`\`
export graph as {name}
\`\`\`
`,L1=`Skill: Graph API Fetcher
------------------------
When a node is configured with this skill of "graph API fetcher", it will make an API call to a backend service
and collect result set into the "result" property of the node. In case of exception, the "status" and "error"
fields will be set to the node's properties and the graph execution will stop.

Execution will start when the GraphExecutor reaches the node containing this skill.

Pre-requisite
-------------
Please refer to the "data dictionary" documentation using "help data-dictionary" before creating an API fetcher node.

Route name
----------
"graph.api.fetcher"

Setup
-----
To enable this skill for a node, set "skill=graph.api.fetcher" as a property in a node.
It will find out the data provider from a given data dictionary item to make an outgoing API call.

The following are required in the properties of the node:

1. dictionary - this is a list of valid data dictionary node names configured in the same graph model
2. input - one or more data mapping as input parameters to invoke the API call
3. output - one of more data mapping to map result set to another node or the 'output.' namespace

The parameter name in each mapping statement must match that in the data dictionary item.
Otherwise, execution will fail.

The system uses the same syntax of Event Script for data mapping.

Properties
----------
\`\`\`
skill=graph.api.fetcher
dictionary[]={data dictionary item}
input[]={mapping of key-value from input or another node to input parameter(s) of the data dictionary item(s)}
output[]={optional mapping of result set to one or more variables in the 'model.' or 'output.' namespace}
\`\`\`

Optional properties
-------------------
\`\`\`
for_each[]={map an array parameter for iterative API execution}
concurrency={controls parallel API calls for an "iterative API request". Default 3, max 30}
exception={exception-handler-node-name}
\`\`\`

Dictionary
----------
This list contains one or more data dictionary item (aka 'data attribute')

Feature
-------
This API fetcher supports features configured in a data provider's node.

There are 2 built-in features that are convenience for development and tests:
- log-request-headers
- log-response-headers

When either or both of these features are added to a data provider's node, 
the fetcher will log request/response headers into the "header" section
of its properties.

Input/Output Data mapping
-------------------------
source.composite.key -> target.composite.key

For input data mapping, the source can use a key-value from the \`input.\` namespace or another node.
The target can be a key-value in the state machine (\`model.\` namespace) or an input parameter name of the
data dictionary.

For output data mapping, the source can be a key-value from the result set and the target can use
the \`output.\` or \`model.\` namespace.

Output data mapping is optional because you can use another data mapper to map result set of the fetcher
to another node.

Result set
----------
Upon successful execution, the result set will be stored in the "result" parameter in the properties of
the node. A subsequent data mapper can then map the key-values in the result set to one or more nodes.

Example
-------
\`\`\`
create node fetcher-1
with properties
skill=graph.api.fetcher
dictionary[]=person_name
dictionary[]=person_address
dictionary[]=person_accounts
input[]=input.body.person_id -> person_id
output[]=result.person_name -> output.body.name
output[]=result.person_address -> output.body.address
\`\`\`

Iterative API call
------------------
Using the optional \`for_each\` statement, you can tell the API fetcher to do "fork-n-join" of API requests.

A "for_each" statement extracts the next array element from result set of a prior API call into a model variable.
You can then put the model variable in the "left-hand-side" of an input statement. The API fetcher will then
issue multiple API calls using an iterative stream of the model variable.

If your API call needs more than one parameter, you can configure more than one "for_each" statement.

Example
-------
In this example, the "for_each" statement extracts the "person_accounts" from the result of a prior API call
by "fetcher-1" and map the array into an iterative stream of elements using the model variable "account_id".

The concurrency property tells the API fetcher to limit parallelism to avoid overwhelming the target service.
\`\`\`
create node fetcher-2
with properties
skill=graph.api.fetcher
dictionary[]=person_id
dictionary[]=account_id
for_each[]=fetcher-1.result.person_accounts -> model.account_id
concurrency=3
input[]=input.body.person_id -> person_id
input[]=model.account_id -> account_id
output[]=result.person_name -> output.body.name
output[]=result.person_address -> output.body.address
\`\`\`

- The "[]" syntax is used to create and append a list of one or more data mapping entries
- The "->" signature indicates the direction of mapping where the left-hand-side is a source
  and right-hand-side is a target

Custom error handling
---------------------
By default, when an API request fails, the system will abort the graph execution and return the error code
and message to the caller.

If you want to handle the exception in your graph model, you can set the node-name of the error-handler in
the "exception" property to tell the system to traverse to the error-handler node.

To handle an exception, the error-handler node should be a decision-making node using the graph.math or graph.js skill.
It can evaluate the status code and error in the API fetcher node to determine the next step.

Caution
-------
API fetchers can be chained together to make multiple API calls. 
However, you should design the API chain to be minimalist.

An overly complex chain of API requests would mean slow performance. Just take the minimal set of data that are
required by your application. Don't abuse the flexibility of the API fetcher.
`,Y1=`Skill: Graph Data Mapper
------------------------
When a node is configured with this skill of "data mapping", it will execute a set of data mapping entries
to populate data attributes into one or more nodes where each node represents a data entity.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.data.mapper"

Setup
-----
To enable this skill for a node, set "skill=graph.data.mapper" as a property in a node.
One or more data mapping entries can be added to the property "mapping".

Properties
----------
\`\`\`
skill=graph.data.mapper
mapping[]=source -> target
\`\`\`

The system uses the same syntax of Event Script for data mapping.

Execution
---------
Upon successful execution, key-values will be populated to one or more nodes.

Syntax for mapping
------------------
source.composite.key -> target.composite.key

The source composite key can use the following namespaces:
1. "input." namespace to map key-values from the input header or body of an incoming request
2. Node name (aka 'alias') to map key-values of a node's properties
3. "model." namespace for holding intermediate key-values for simple data transformation

The target composite key can use the following namespaces:
1. "output." namespace to map key-values to the result set to be returned as response to the calling party
2. Node name (aka 'alias') to map key-values of a node's properties
3. "model." namespace for holding intermediate key-values for simple data transformation

Example
-------
\`\`\`
create node my-simple-mapper
with properties
skill=graph.data.mapper
mapping[]=input.body.hr_id -> employee.id
mapping[]=input.body.join_date -> employee.join_date
\`\`\`

The "[]" syntax is used to create and append a list of one or more data mapping entries
The "->" signature indicates the direction of mapping where the left-hand-side is source and right-hand-side is target
`,q1=`Skill: Graph Extension
----------------------
When a node is configured with this skill of "graph extension", it will make an API call to another graph model
(or flow) and collect result set into the "result" property of the node. In case of exception, the "status" and
"result.error" fields will be set to the node's properties and the graph execution will stop.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.extension"

Setup
-----
To enable this skill for a node, set "skill=graph.extension" as a property in a node.

The following parameters are required in the properties of the node:

1. extension - this should be a valid graph model name or flow identifier in the same memory space
2. input - this should include one or more data mapping as input parameters to invoke the API call

A flow identifier is prefixed by a flow protocol signature "flow://". e.g. "flow://hello-world".

The system uses the same syntax of Event Script for data mapping.

Properties
----------
\`\`\`
skill=graph.extension
extension=graph-id or flow-id
input[]={mapping of key-value from input or another node to input parameter(s) of the data dictionary item(s)}
output[]={optional mapping of result set to one or more variables in the 'model.' or 'output.' namespace}
\`\`\`

Optional properties
-------------------
\`\`\`
for_each[]={map an array parameter for iterative API execution}
concurrency={controls parallel API calls for an "iterative API request". Default 3, max 30}
exception={error-handler-node-name}
\`\`\`

Result set
----------
Upon successful execution, the result set will be stored in the "result" parameter in the properties of
the node. A subsequent data mapper can then map the key-values in the result set to one or more nodes.

Input Data mapping
------------------
source.composite.key -> target.composite.key

For input data mapping, the source can use a key-value from the \`input.\` namespace or another node.
The target can be a key-value in the state machine (\`model.\` namespace) or an input parameter name of the
data dictionary.

Example
-------
\`\`\`
create node performance-evaluator
with properties
skill=graph.extension
extension=evaluate-sales-performance
input[]=input.body.department_id -> id
output[]=result.sales_performance -> output.body.sales_performance
\`\`\`

Iterative API call
------------------
Using the optional \`for_each\` statement, you can tell the "Extension" skill to do "fork-n-join" of API requests.

A "for_each" statement extracts the next array element from a node result set into a model variable.
You can then put the model variable in the "left-hand-side" of the mapping statement. The skill will then
issue multiple API calls using an iterative stream of the model variable.

If your API call needs more than one parameter, you can configure more than one "for_each" statement.

The concurrency property tells the skill to limit parallelism to avoid overwhelming the target service.

The "[]" syntax is used to create and append a list of one or more data mapping entries
The "->" signature indicates the direction of mapping where the left-hand-side is source and right-hand-side is target

Custom error handling
---------------------
By default, when an API request fails, the system will abort the graph execution and return the error code
and message to the caller.

If you want to handle the exception in your graph model, you can set the node-name of the error-handler in
the "exception" property to tell the system to traverse to the error-handler node.

To handle an exception, the error-handler node should be a decision-making node using the graph.math or graph.js skill.
It can evaluate the status code and error in the API fetcher node to determine the next step.
`,I1=`Skill: Graph Island
-------------------
The purpose of a node with this skill is to tell the system to block graph traversal.

In this way, we can use this node as a connector to data entities and other things that are used to
represent some knowledge. We don't want to system to actively executing the nodes on the "isolated island".

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.island"

Setup
-----
To enable this skill for a node, set "skill=graph.island" as a property in a node.
This node does not require additional properties.

Properties
----------
\`\`\`
skill=graph.island
\`\`\`

Execution
---------
Upon successful execution, a node with this skill will return ".sink" to tell the system
that there is no need for further traversal.
`,X1=`Skill: Graph Join
-----------------
A node with this skill will wait for all connected nodes that join to this node to complete.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.join"

Setup
-----
To enable this skill for a node, set "skill=graph.join" as a property in a node.
This node does not require additional properties.

Properties
----------
\`\`\`
skill=graph.join
\`\`\`

Execution
---------
Upon successful execution, a node with this skill will return "next" if all connected nodes to finish
processing. Otherwise, it will return ".sink" to tell the system that it is not ready.
`,J1=`Skill: Graph JS
---------------
When a node is configured with this skill of "graph js", it will execute a set of simple JavaScript statements
to return result. For example, doing mathematical calculation or boolean operation for decision-making.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.js"

Setup
-----
To enable this skill for a node, set "skill=graph.js" as a property in a node.
One or more statements can be added.

There are 5 types of statements:
1. "IF" statement for decision-making
2. "COMPUTE" statement to evaluate a mathematical formula
3. "MAPPING" statement to do data mapping from a source to a target variable
4. "EXECUTE" statement to execute another node with "graph.js" skill
5. "RESET" statement to reset one or more nodes from the state machine

You can configure one or more statements of these 3 types.

The system will reject execution if the node contains only "MAP" statements
because it is more efficient to use the "graph.data.mapper" skills for mapping
only operations.

Statements are executed orderly.

Properties
----------
\`\`\`
skill=graph.js
statement[]=COMPUTE: variable -> mathematical statement
statement[]=IF: if-then-else statement
statement[]=MAPPING: source -> target
statement[]=EXECUTE: another-node
\`\`\`

Node cannot be executed more than once
--------------------------------------
To avoid unintended looping, the system guarantees that a node, that has been "seen", is not executed again.

The \`reset\` command clears the "seen" status and erases its result from the state machine. This is reserved
for advanced use cases that require executing a node more than once. You should use this feature with care.

The following statement resets the node named "previous-node" so that the graph executor can run this node
again when conditional traversal points to the node.

\`\`\`
statement[]=RESET: previous-node
\`\`\`

Optional properties
-------------------
\`\`\`
for_each[]={map an array parameter for iterative statement execution}
statement[]=BEGIN
statement[]=END
statement[]=NEXT: {next-node-name}
statement[]=DELAY: {milliseconds}
\`\`\`

Execution
---------
Upon successful execution of a "COMPUTE" statement, the result set will be stored in the "result" namespace
of the node. A subsequent "MAPPING" statement can map the key-values in the result set to one or more nodes.

For an "IF" statement, the system will execute a boolean operation.
This process will override the natural graph traversal order and jump to a specific node.
If the function returns "next" after evaluation of all statements, the natural graph traversal order
will be preserved.

Iterative Execution and Begin-End
---------------------------------
Using the optional \`for_each\` statement, you can tell the skill module to execute the statements iteratively.

A "for_each" statement extracts the next array element from another array variable into a model variable.
You can then put the model variable in the "left-hand-side" of an input statement. The module will then
execute the statement block using an iterative stream of the model variable.

You can also use the \`BEGIN\` and \`END\` control statements to select a section of the statements for the
iterative execution based on the "for_each" criteria.

Syntax for COMPUTE statement
----------------------------
It will be a regular JavaScript statement with parameter substitution using the bracket syntax where
the enclosed parameter is a reference to a data attributes in the namespace of "input.", "model." or node name.

When you have more than one JavaScript statement, a subsequent statement can use the result of a prior statement
as its parameters.

Each parameter is wrapped by a set of curly brackets.

Override Graph Traversal
------------------------
Normally the next node is the one or more nodes that this node is connected to.
If you want to tell system to jump to a specific "next-node", you can use the "NEXT:" syntax and put the name
of the node to jump to.

Deferred completion
-------------------
You can add an artificial delay to defer completion of the execution of this node. This is useful to simulate
a slow service for performance test and to pause between retries.

Next and Delay statements
-------------------------
It is a good practice to place the next or delay statement, if any, as last one in the block.
However, the placement does not change the behavior because they will only be processed at the end.

Limitation
----------
This skill is designed to execute a simple inline JavaScript statement that uses standard JavaScript library.
Complex functions and variables are not recommended.

Example
-------
\`\`\`
create node demo-js-runner
with properties
skill=graph.js
statement[]=COMPUTE: amount -> (1 - {input.body.discount}) * {book.price}
\`\`\`

The syntax \`{variable_name}\` is used to resolve the value from the variable into the COMPUTE statement.

Syntax for IF statement
-----------------------
Each IF statement is a multiline command:
\`\`\`
IF: JavaScript-statement
THEN: node-name | next
ELSE: node-name | next
\`\`\`

The "next" keyword tells the system to execute the next statement.

The if-then-else is used to select two options after evaluation of the JavaScript statement.
If the JavaScript statement does not return a boolean value, the following resolution would apply:
1. numeric value - true is positive value and false is negative value
2. text value - "true", "yes", "T", "Y" are positive and all other values are false
3. other value will be converted to a text string first

Example
-------
\`\`\`
statement[]='''
IF: (1 - {input.body.discount}) * {book.price} > 5000
THEN: high-price
ELSE: low-price
\`\`\`

The syntax \`{variable_name}\` is used to resolve the value from the variable into the IF statement.

Syntax for MAPPING statement
----------------------------
MAPPING: source.composite.key -> target.composite.key

The source composite key can use the following namespaces:
1. "input." namespace to map key-values from the input header or body of an incoming request
2. Node name (aka 'alias') to map key-values of a node's properties
3. "model." namespace for holding intermediate key-values for simple data transformation

The target composite key can use the following namespaces:
1. "output." namespace to map key-values to the result set to be returned as response to the calling party
2. Node name (aka 'alias') to map key-values of a node's properties
3. "model." namespace for holding intermediate key-values for simple data transformation

Example
-------
\`\`\`
statment[]=MAPPING: input.body.hr_id -> employee.id
statement[]=MAPPING: input.body.join_date -> employee.join_date
\`\`\`

Note that the MAPPING statement operates exactly in the same way as a data-mapper so there is
no need to use curly braces to wrap around variables.

Syntax for EXECUTE statement
----------------------------
EXECUTE: another-node

Example
-------
\`\`\`
statment[]=EXECUTE: js-3
\`\`\`

The "[]" syntax is used to create and append a list of one or more statements
`,V1=`Skill: Graph Math
-----------------
When a node is configured with this skill of "graph math", it will execute a set of simple math or boolean statements
to return result. For example, doing mathematical calculation or boolean operation for decision-making.

While your math and/or boolean statements use JavaScript syntax, this skill does not support full JavaScript language.
Its capability is limited to simple math and boolean operations.

Examples for math statement: 
- \`COMPUTE: Math.sin(Math.PI / 2) + 1\`
- \`COMPUTE: value -> x ** 2 + 10 * {interest.rate}\`

where "interest" is a node-name and "rate" is a property of the node.
The return value is a floating point number with double precision.

Example for boolean statement: 
- \`IF: {member.age} >= 18\`
The return value is true or false to execute the THEN or ELSE path.

For performance reason, you should use this skill instead of the "graph.js" skill.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.math"

Setup
-----
To enable this skill for a node, set "skill=graph.math" as a property in a node.
One or more statements can be added.

There are 5 types of statements:
1. "IF" statement for decision-making
2. "COMPUTE" statement to evaluate a mathematical formula
3. "MAPPING" statement to do data mapping from a source to a target variable
4. "EXECUTE" statement to execute another node with "graph.math" skill
5. "RESET" statement to reset the state machine for one or more nodes

You can configure one or more statements of these 3 types.

The system will reject execution if the node contains only "MAP" statements
because it is more efficient to use the "graph.data.mapper" skills for mapping
only operations.

Statements are executed orderly.

Properties
----------
\`\`\`
skill=graph.math
statement[]=COMPUTE: variable -> mathematical statement
statement[]=IF: if-then-else statement
statement[]=MAPPING: source -> target
statement[]=EXECUTE: another-node
\`\`\`

Node cannot be executed more than once
--------------------------------------
To avoid unintended looping, the system guarantees that a node, that has been "seen", is not executed again.

The \`reset\` command clears the "seen" status and erases its result from the state machine. This is reserved
for advanced use cases that execute a node more than once. *This optional feature must be used with care*.

The following statement resets the node named "previous-node" so that the graph executor can run this node
again when conditional traversal points to the node.

\`\`\`
statement[]=RESET: previous-node
\`\`\`

Optional properties
-------------------
\`\`\`
for_each[]={map an array parameter for iterative statement execution}
statement[]=BEGIN
statement[]=END
statement[]=NEXT: {next-node-name}
statement[]=DELAY: {milliseconds}
\`\`\`

Execution
---------
Upon successful execution of a "COMPUTE" statement, the result set will be stored in the "result" namespace
of the node. A subsequent "MAPPING" statement can map the key-values in the result set to one or more nodes.

For an "IF" statement, the system will execute a boolean operation.
This process will override the natural graph traversal order and jump to a specific node.
If the function returns "next" after evaluation of all statements, the natural graph traversal order
will be preserved.

Iterative Execution and Begin-End
---------------------------------
Using the optional \`for_each\` statement, you can tell the skill module to execute the statements iteratively.

A "for_each" statement extracts the next array element from another array variable into a model variable.
You can then put the model variable in the "left-hand-side" of an input statement. The module will then
execute the statement block using an iterative stream of the model variable.

You can also use the \`BEGIN\` and \`END\` control statements to select a section of the statements for the
iterative execution based on the "for_each" criteria.

Syntax for COMPUTE statement
----------------------------
It will be a regular JavaScript statement with parameter substitution using the bracket syntax where
the enclosed parameter is a reference to a data attributes in the namespace of "input.", "model." or node name.

When you have more than one JavaScript statement, a subsequent statement can use the result of a prior statement
as its parameters.

Each parameter is wrapped by a set of curly brackets.

Override Graph Traversal
------------------------
Normally the next node is the one or more nodes that this node is connected to.
If you want to tell system to jump to a specific "next-node", you can use the "NEXT:" syntax and put the name
of the node to jump to.

Deferred completion
-------------------
You can add an artificial delay to defer completion of the execution of this node. This is useful to simulate
a slow service for performance test and to pause between retries.

Next and Delay statements
-------------------------
It is a good practice to place the next or delay statement, if any, as last one in the statement block.
However, the placement does not change the behavior because they will only be processed at the end.

Limitation
----------
This skill is designed to execute a simple inline mathematics or boolean operations that use JavaScript syntax.
For simplicity and speed of execution, it does not support variables and functions.

Example
-------
\`\`\`
create node demo-math-runner
with properties
skill=graph.math
statement[]=COMPUTE: amount -> (1 - {input.body.discount}) * {book.price}
\`\`\`

The syntax \`{variable_name}\` is used to resolve the value from the variable into the COMPUTE statement.

Syntax for IF statement
-----------------------
Each IF statement is a multiline command:
\`\`\`
IF: Boolean-operation-statement
THEN: node-name | next
ELSE: node-name | next
\`\`\`

The "next" keyword tells the system to execute the next statement.

The if-then-else is used to select two options after evaluation of the boolean operation statement.

Example
-------
\`\`\`
statement[]='''
IF: (1 - {input.body.discount}) * {book.price} > 5000
THEN: high-price
ELSE: low-price
\`\`\`

The syntax \`{variable_name}\` is used to resolve the value from the variable into the IF statement.

Syntax for MAPPING statement
----------------------------
MAPPING: source.composite.key -> target.composite.key

The source composite key can use the following namespaces:
1. "input." namespace to map key-values from the input header or body of an incoming request
2. Node name (aka 'alias') to map key-values of a node's properties
3. "model." namespace for holding intermediate key-values for simple data transformation

The target composite key can use the following namespaces:
1. "output." namespace to map key-values to the result set to be returned as response to the calling party
2. Node name (aka 'alias') to map key-values of a node's properties
3. "model." namespace for holding intermediate key-values for simple data transformation

Example
-------
\`\`\`
statment[]=MAPPING: input.body.hr_id -> employee.id
statement[]=MAPPING: input.body.join_date -> employee.join_date
\`\`\`

Note that the MAPPING statement operates exactly in the same way as a data-mapper so there is
no need to use curly braces to wrap around variables.

Syntax for EXECUTE statement
----------------------------
EXECUTE: another-node

Example
-------
\`\`\`
statment[]=EXECUTE: math-3
\`\`\`

The "[]" syntax is used to create and append a list of one or more statements
`,Z1=`Import a graph model
--------------------
1. This command imports a graph as a model for review and update
2. The name does not require the ".json" extension

Syntax
------
\`\`\`
import graph from {name}
\`\`\`

Example
-------
\`\`\`
import graph from helloworld
\`\`\`

Import a node from another graph model
--------------------------------------
You can re-use nodes from another graph.

A best practice is to publish some common graph model holding reusable nodes as modules and skills
so that other members can borrow the nodes for use in their own graph models.

Syntax
------
\`\`\`
import node {node-name} from {graph-name}
\`\`\`

Example
-------
\`\`\`
import node fetcher from helloworld
\`\`\`
`,Q1=`Inspect state machine
---------------------
This command inspects the state machine containing properties of nodes, input, output and model namespaces.

Pre-requisite
-------------
A graph instance is created with the "instantiate" command

Syntax
------
\`\`\`
inspect {variable_name}
\`\`\`

Examples
--------
\`\`\`
inspect {input.body.user_id}
inspect {book.price}
inpsect {model.some_variable}
inspect {output.body.some_key}
\`\`\`
`,K1=`Instantiate from a Graph Model
------------------------------
1. This command creates a graph instance with mock input from the current graph model for development and tests
2. You must do this before using "execute", "inspect" and "run" commands
3. The name does not require the ".json" extension
4. You can tell the system to mock one or more constants as input variables
5. The input namespace contains 'body' and 'header'
6. The model namespace is a state machine. It is optional unless you want to emulate some model variables.

Syntax
------
\`\`\`
instantiate graph
{constant} -> input.body.{key}
\`\`\`

Example
-------
\`\`\`
instantiate graph
int(100) -> input.body.profile_id
text(application/json) -> input.header.content-type
text(world) -> model.hello
\`\`\`

Alias
-----
\`start\` is an alias of \`instantiate\`
`,$1=`List nodes or connections
-------------------------
The "list nodes" and "list connections" commands list all the nodes and connections of the current graph model
respectively.

Syntax
------
\`\`\`
list nodes
\`\`\`

\`\`\`
list connections
\`\`\`
`,P1=`Run a graph instance
--------------------
1. This command runs a graph instance from a root node. Using graph traversal, it will execute any node with skill
   configured.
2. Each new instance can only be executed once.
3. You must close the current instance and instantiate a new one for the next "run" command.

Pre-requisite
-------------
A graph instance is created with the "instantiate" command

Syntax
------
\`\`\`
run
\`\`\`
`,W1=`Display nodes that have been 'seen'
-----------------------------------
This command displays the list of nodes that have been seen or executed.

Pre-requisite
-------------
A graph instance is created with the "instantiate" command

Syntax
------
\`\`\`
seen
\`\`\`
`,F1=`Session commands
----------------
1. Display current session 
2. Mirror another session for collaboration with another user

Syntax
------
Display current session
\`\`\`
session
\`\`\`

Subscribe to another session
\`\`\`
session subscribe {session-id}
\`\`\`

Reset as a new session
\`\`\`
session reset
\`\`\`
`,eb=`Tutorial 1
----------
Welcome to the MiniGraph Playground, the self-service user interface for creating amazing applications
using [Active Knowledge Graph](https://accenture.github.io/mercury-composable/guides/CHAPTER-11/)
(*right-click to open new tab*).

Let's get started.

In this session, you will create the simplest application that returns a "hello world" message.

Exercise
--------
If you can see this page, this means you have successfully started the MiniGraph Playground from a browser
and connected to a designer workbench session.

If your session is disconnected, select the "Tools" dropdown in the top-right corner, click MiniGraph's start
and select "MiniGraph".

Create a starting point of a graph
----------------------------------
**Create a root node** that is the starting point for a graph model.
Select multiline and enter the following command in the bottom-right inbox box.

\`\`\`
create node root
with type Root
with properties
purpose=Tutorial one to return a 'hello world' message
\`\`\`

The console displays:

\`\`\`
> create node root...
Graph with 1 node described in /api/graph/model/ws-875677-2/165-1
\`\`\`

A drawing will be shown on the right hand side under the "Graph" tab.

This means a graph with a single node called "root" has been created.

\`ws-875677-2\` is the session ID of the workbench.
\`165-1\` is a random number for the session that you can ignore.

Create an end node
------------------
An end node is the exit point of a graph model.

Enter the following to create an end node.

\`\`\`
create node end
with type End
with properties
skill=graph.data.mapper
mapping[]=text(hello world) -> output.body
\`\`\`

The console displays:

\`\`\`
> create node end...
Graph with 2 nodes described in /api/graph/model/ws-875677-2/061-2
\`\`\`

The "skill=graph.data.mapper" assigns the data mapper function to the end node.
In a data mapper, you can do data mapping. 

The mapping statement \`mapping[]=text(hello world) -> output.body\` tells the
system to map the constant "hello world" to \`output.body\` that is the response
payload when the graph is executed. The \`[]\` syntax means it is a list of statements.

The MiniGraph system uses the same Event Script's data mapping syntax. For more details, please refer to
[Data Mapping Syntax](https://accenture.github.io/mercury-composable/guides/CHAPTER-4/#tasks-and-data-mapping)
(*right-click to open new tab*).

First attempt to run a graph
----------------------------
To run a graph model, you can use the \`instantiate graph\` command.

The console displays:

\`\`\`
> instantiate graph
Graph instance created. Loaded 0 mock entries, model.ttl = 30000 ms
\`\`\`

When you enter "instantiate graph", you ask the system to create an "instance"
from a graph model.

You can now try to run the graph by entering the "run" command.

The console displays:

\`\`\`
> run
Walk to root
\`\`\`

The system will start running the graph from the starting point. i.e. the root node.
However, nothing happens after that.

What is missing?
----------------
Active Knowledge Graph is a "property graph" that contains one or more "active" nodes.
An active node is associated with a "skill" that is backed by a composable function.

The system performs graph traversal from the root node. There is nothing happened
because there are no further nodes to reach after the root node.

Graph traversal will stop when running in the MiniGraph Playground because the graph
model is incomplete without an "end" node.

Connecting nodes
----------------
Please enter the following command to connect the root node to the end node.

\`\`\`
connect root to end with done
\`\`\`

The console displays:

\`\`\`
> connect root to end with done
node root connected to end
Graph with 2 nodes described in /api/graph/model/ws-875677-2/551-3
\`\`\`

The graph model drawing is updated on the right panel.

Running the graph
-----------------
Now you have a graph that has a start and an ending point where one node contains a skill to do something.
i.e. the end node with a data mapping statement.

You can now instantiate the graph again and run it by entering the following commands.

\`\`\`
instantiate graph
run
\`\`\`

The console displays:

\`\`\`
> instantiate graph
Graph instance created. Loaded 0 mock entries, model.ttl = 30000 ms
> run
Walk to root
Walk to end
Executed end with skill graph.data.mapper in 1.736 ms
{
  "output": {
    "body": "hello world"
  }
}
Graph traversal completed in 9 ms
\`\`\`

Congratulations. You have create your first MiniGraph that works.
It returns "hello world" when it runs.

Export the graph
----------------
You may now export the graph so that you can deploy it to production.

Enter the export command below:

\`\`\`
export graph as tutorial-1
\`\`\`

This will export the graph model in JSON format with the name \`tutorial-1\`
in "/tmp/graph/helloworld.json"

The console displays:

\`\`\`
> export graph as tutorial-1
Added name=tutorial-1 to Root node
Graph exported to /tmp/graph/tutorial-1.json
Described in /api/graph/model/tutorial-1/436-4
\`\`\`

Note that the system will add the graph name (i.e. unique "id") to the root node.
This avoids the user from accidentally overwriting an existing graph model.

Help pages
----------
To display more information about each command that you use in this tutorial,
enter the following:

\`\`\`
help create
help connect
help instantiate
help run
help export
\`\`\`

Summary
-------
In this session, you have created the simplest graph model to return a "hello world" message when the graph
API endpoint is called. You have exported the graph model and tested some help pages.

Well done. Let's move on to "Tutorial 2".
`,tb=`Tutorial 10
-----------
In this session, you will create a graph model to use an extension.

Exercise
--------
You will use an existing graph model as an extension. Then create a new graph model to use the extension.

To clear the previous graph session, click the Tools button in the top-right corner and click the "Stop" and "Start"
toggle button. A new graph session will start.

What is a graph extension?
--------------------------
A graph extension is a graph model that is built to serve some logic that can be reused by another graph model.

Import tutorial 3 as an extension
---------------------------------
Enter the following to import tutorial 3. Note that tutorial-3.json is preloaded into the main/resources/graph
folder.

\`\`\`
> import graph from tutorial-3
Graph model not found in /tmp/graph/tutorial-3.json
Found deployed graph model in classpath:/graph
Please export an updated version and re-import to instantiate an instance model
Graph model imported as draft
\`\`\`

Once the graph model is imported, start the graph with mock data.

\`\`\`
start graph
int(100) -> input.body.person_id
\`\`\`

Then do a 'dry-run'

\`\`\`
> run
Walk to root
Walk to fetcher
Executed fetcher with skill graph.api.fetcher in 0.982 ms
Walk to end
{
  "output": {
    "body": {
      "address": "100 World Blvd",
      "name": "Peter"
    }
  }
}
Graph traversal completed in 2 ms
\`\`\`

You see that it fetches data using the input parameter (person_id=100) and return name and address of the person.

Restart playground session
--------------------------
You will clear the current graph session - click the Tools button in the top-right corner and click the "Stop" 
and "Start" toggle button. A new graph session will start.

Create a root node and an end node
----------------------------------
You will create a new graph model with root node and end node.

\`\`\`
create node root
with type Root
with properties
name=tutorial-10
purpose=Demonstrate the use of graph extension
\`\`\`

\`\`\`
create node end
with type End
\`\`\`

Create a node to use an extension
---------------------------------
Enter the following to create an extension node. The skill is 'extension' and the extension is 'tutorial-3'.

The input mapping sets the input parameter(s) to an extension which is also a graph model.
The output mapping sets the result from the extension to the output payload.

\`\`\`
create node extension
with type Extension
with properties
skill=graph.extension
extension=tutorial-3
input[]=input.body.person_id -> person_id
output[]=result -> output.body
\`\`\`

Connect the nodes to complete the graph model
---------------------------------------------

\`\`\`
connect root to extension with run
connect extension to end with finish
\`\`\`

Test the graph model
--------------------
Enter the following to instantiate the graph model with mock input.

\`\`\`
instantiate graph
int(100) -> input.body.person_id
\`\`\`

Then do a 'dry-run'.

\`\`\`
> run
Walk to root
Walk to extension
Executed extension with skill graph.extension in 19.013 ms
Walk to end
{
  "output": {
    "body": {
      "address": "100 World Blvd",
      "name": "Peter"
    }
  }
}
Graph traversal completed in 20 ms
\`\`\`

The input for the current graph instance is mapped as input parameter to the extension 'tutorial-3'.
The result is mapped as output for the graph.

If you inspect the extension node, you will see:

\`\`\`
> inspect extension
{
  "inspect": "extension",
  "outcome": {
    "result": {
      "address": "100 World Blvd",
      "name": "Peter"
    },
    "live": true,
    "target": "tutorial-3",
    "status": 200
  }
}
> inspect output
{
  "inspect": "output",
  "outcome": {
    "body": {
      "address": "100 World Blvd",
      "name": "Peter"
    }
  }
}
\`\`\`

Check the application log
-------------------------
Complete telemetry information is shown in the application log. You will see that 'tutorial-3' is invoked
as an extension and it fetches data from the data provider with the input parameter 'person_id'.

\`\`\`
GraphExtension:202 - Call extension tutorial-3, ttl=30000
GraphApiFetcher:410 - GET http://127.0.0.1:8085/api/mdm/profile/100, with [person_id], ttl=30000
\`\`\`

This is a trivial example to demonstrate that you can call an extension from a graph instance.
A typical use case is that the main graph model would use one or more extensions for API data fetching and perform
decision-making using the retrieved data.

Reusability
-----------
Graph extension promotes reusability. Common use cases can be built using graph models that are available as
"extensions" for another graph model to use.

Export the graph model
----------------------
Now you may save the graph model by exporting it.

\`\`\`
> export graph as tutorial-10
Graph exported to /tmp/graph/tutorial-10.json
Described in /api/graph/model/tutorial-10/286-8
\`\`\`

Deploy the graph model
----------------------
To deploy the graph model, copy "/tmp/graph/tutorial-10.json" to your application's \`main/resources/graph\` folder.
You can then test the deployed model with a curl command.

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-10 \\
  -H "Content-Type: application/json" \\
  -d '{ 
    "person_id": 100
}'
\`\`\`

Summary
-------
In this session, you have created a graph model that uses a graph extension.
`,nb=`Tutorial 11
-----------
In this session, you will create a graph model to use an "event flow" as an extension.

Pre-requisite
-------------
You would need some working knowledge with event script. For more details, please refer to
[Event Script Syntax](https://accenture.github.io/mercury-composable/guides/CHAPTER-4).

Assume you already know how to create an event flow (configuration and composable functions as tasks),
it is easy to use event flow as an extension.

What is a flow extension?
-------------------------
A flow extension is an event flow that is built to serve some logic that can be reused by a graph model.

Import graph model from Tutorial-10
-----------------------------------
In tutorial 10, you have created an extension in a main graph to call another graph.

You will update the graph model in tutorial 10 to call a flow as an extension.

\`\`\`
> import graph from tutorial-10
Graph exported to /tmp/graph/tutorial-11.json
Described in /api/graph/model/tutorial-11/431-3
\`\`\`

Edit the root node
------------------
Enter 'edit node root' and copy-n-paste the content into the inbox box. Change the name and purpose for
tutorial 11.

\`\`\`
update node root
with type Root
with properties
name=tutorial-11
purpose=Demonstrate the use of flow extension
\`\`\`

Edit the extension node
-----------------------
Enter 'edit node extension' and copy-n-paste the content into the inbox box. Update the extension to "flow://flow-11"
and change the input statements to pass "hello" and "message" as parameters. The flow protocol prefix tells the
system to execute the flow with the identifier "flow-11".

\`\`\`
update node extension
with type Extension
with properties
extension=flow://flow-11
input[]=input.body.hello -> hello
input[]=input.body.message -> message
output[]=result -> output.body
skill=graph.extension
\`\`\`

About flow 11
-------------
For your convenience, "flow-11" is preloaded. You can review the configuration files "flows.yaml" and "flow-11.yml"
in the resources folder. The event flow "flow-11" is an echo program. The task "no.op" will echo everything from
the input and pass it as output. Below is an extract of the event flow's first task.

\`\`\`yaml
tasks:
  - input:
      # pass all input parameters as arguments
      - 'input.body -> *'
    process: 'no.op'
    output:
      - 'result -> output.body'
    description: 'echo everything in the input payload'
    execution: end
\`\`\`

Perform a dry-run
-----------------
To test the updated graph model, you can instantiate the graph with the two input "hello" and "message" as follows:

\`\`\`
instantiate graph
text(world) -> input.body.hello
text(this is a good day) -> input.body.message
\`\`\`

Then enter 'run' to execute the graph.

\`\`\`
> start graph...
Graph instance created. Loaded 2 mock entries, model.ttl = 30000 ms
> run
Walk to root
Walk to extension
Executed extension with skill graph.extension in 5.46 ms
Walk to end
{
  "output": {
    "body": {
      "hello": "world",
      "message": "this is a good day"
    }
  }
}
Graph traversal completed in 7 ms
\`\`\`

You can also check the application log. Telemetry and tracing information are shown.

\`\`\`
GraphExtension:202 - Call extension flow://flow-11, ttl=30000
Telemetry:81 - {trace={path=/graph/playground, service=graph.extension...
Telemetry:81 - {trace={path=/graph/playground, service=no.op...
Telemetry:81 - {trace={path=/graph/playground, service=task.executor...
Telemetry:81 - {trace={path=/graph/playground, service=event.script.manager...
\`\`\`

This validates that the event flow instance for "flow-11" was executed by the graph instance for tutorial-11.

Export the graph model
----------------------
Now you may save the graph model by exporting it.

\`\`\`
> export graph as tutorial-11
Graph exported to /tmp/graph/tutorial-11.json
Described in /api/graph/model/tutorial-11/794-6
\`\`\`

Deploy the graph model
----------------------
To deploy the graph model, copy "/tmp/graph/tutorial-11.json" to your application's \`main/resources/graph\` folder.
You can then test the deployed model with a curl command.

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-11 \\
  -H "Content-Type: application/json" \\
  -d '{ 
    "hello": "world",
    "message": "this is a good day"
}'
\`\`\`

Summary
-------
In this session, we have discussed the use of an event flow as an extension to a graph model and
the use of the flow protocol prefix "flow://".

Why extending a graph model with event flow?
--------------------------------------------
While graph extension discussed in tutorial 10 can create sophisticated and powerful graph models,
extending a graph with event flow allows us to do things beyond simple API fetching, data mapping, computation
and decision-making.

With event flow, you can model very complex transaction processing with "pro-code". The combined graph modeling
and event script programming provides the best of both worlds in no-code and pro-code to tackle the most
demanding use cases.
`,ab=`Tutorial 10
-----------
In this session, you will create a graph model with custom error handling.

Exercise
--------
You will import tutorial 3 and add an error-handler node to retry an API failure.

To clear the previous graph session, click the Tools button in the top-right corner and click the "Stop" and "Start"
toggle button. A new graph session will start.

Import tutorial 3 as a template
-------------------------------
Enter the following to import tutorial 3. Note that tutorial-3.json is preloaded into the main/resources/graph
folder.

\`\`\`
> import graph from tutorial-3
Graph model not found in /tmp/graph/tutorial-3.json
Found deployed graph model in classpath:/graph
Please export an updated version and re-import to instantiate an instance model
Graph model imported as draft
\`\`\`

Update the root node
--------------------
Enter the following to update the root node. It assigns the skill "graph.data.mapper" to the node and
maps the input parameter "exception" to the model variable with the same name.

The \`f:defaultValue()\` plugin function sets the variable "model.exception" to false when the input
parameter is not given.

We will use the model.exception parameter to trigger a simulated exception for the mdm-profile service.

\`\`\`
update node root
with type Root
with properties
mapping[]=f:defaultValue(input.body.exception, boolean(false)) -> model.exception
name=tutorial-12
purpose=Demonstrate custom error handling
skill=graph.data.mapper
\`\`\`

Update the dictionary
---------------------
For person-address, you will add the input parameter \`exception:false\` where ":false" is the default value of
the parameter if not given.

\`\`\`
update node person-address
with type Dictionary
with properties
input[]=person_id
input[]=exception:false
output[]=response.profile.address -> result.address
provider=mdm-profile
purpose=address of a person
\`\`\`

and do the same for person-name

\`\`\`
update node person-name
with type Dictionary
with properties
input[]=person_id
input[]=exception:false
output[]=response.profile.name -> result.name
provider=mdm-profile
purpose=name of a person
\`\`\`

Update the data provider
------------------------
You will add the input data mapping \`exception -> header.x-exception\` to the mdm-profile node. The input parameter
"exception" is used to set the HTTP request header "X-Exception".

\`\`\`
update node mdm-profile
with type Provider
with properties
feature[]=log-request-headers
feature[]=log-response-headers
input[]=text(application/json) -> header.accept
input[]=exception -> header.x-exception
input[]=person_id -> path_parameter.id
method=GET
purpose=Master Data Management's profile management endpoint
url=http://127.0.0.1:\${rest.server.port:8080}/api/mdm/profile/{id}
\`\`\`

Update the fetcher node
-----------------------
You will add the input data mapping \`model.exception -> exception\` to set the parameter exception to retrieve
the two data dictionary items (person-name and person-address).

You also add the property \`exception=error-handler\`. This tells the system to route the flow to the "error-handler"
node.

\`\`\`
update node fetcher
with type Fetcher
with properties
dictionary[]=person-name
dictionary[]=person-address
exception=error-handler
input[]=input.body.person_id -> person_id
input[]=model.exception -> exception
output[]=result.name -> output.body.name
output[]=result.address -> output.body.address
skill=graph.api.fetcher
\`\`\`

The mock endpoint contains this:

\`\`\`java
@Override
public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
    if ("true".equals(input.getHeader("x-exception"))) {
        throw new AppException(401, "simulated exception");
    }
    // for simplicity, business logic not shown here
}
\`\`\`

Create Error-Handler node
-------------------------
You will then create the error-handler node that is referenced in the fetcher node above.

When the "exception" property is configured in a fetcher, the system will not abort the graph traversal, it will
route it to the given error handler.

In the handler, you test the "fetcher.status" variable to see if it is HTTP-200. While an error status is always
a value equals or larger than 200, it is a good practice to do simple validation to avoid unintended configuration
error.

If it is not 200, the statement block will execute. The first 2 mapping statements increment the variable
"model.attempts". The next evaluation statement checks if the maximum attempts have reached, it will clear
the simulated exception by routing to the "clear-exception" node.

The "NEXT: fetcher" statement tells the system to connect to the fetcher again. Since a node cannot be executed twice,
you use the "RESET:" command to clear its states so that it can be executed again.

The "DELAY: 50" means that it will pause for 50 milliseconds before the next retry. This is a best practice because
it avoids very rapid retries that may contribute to a side effect called "recovery storm" or 
"unintended denial-of-service attack".

\`\`\`
create node error-handler
with type Decision
with properties
skill=graph.math
statement[]='''
IF: {fetcher.status} == 200
THEN: end
ELSE: next
'''
statement[]=MAPPING: f:defaultValue(model.attempts, int(0)) -> model.attempts
statement[]=MAPPING: f:add(model.attempts, int(1)) -> model.attempts
statement[]='''
IF: {model.attempts} >= 3
THEN: clear-exception
ELSE: next
'''
statement[]=RESET: fetcher, error-handler
statement[]=NEXT: fetcher
statement[]=DELAY: 50
\`\`\`

Create the clear-exception node
-------------------------------
In the clear-exception node, you add statements to set the variable "model.exception" to false so that
the mock service will return normal response instead of an exception. You also clear the "model.attempts" to zero
and reset the fetcher and the clear-exception nodes so that the system can execute them again.

You will then create new connections to complete the exercise.

\`\`\`
create node clear-exception
with type Decision
with properties
skill=graph.math
statement[]=MAPPING: boolean(false) -> model.exception
statement[]=MAPPING: int(0) -> model.attempts
statement[]=RESET: fetcher, clear-exception
\`\`\`

Connections for error-handler and clear-exception nodes
-------------------------------------------------------

\`\`\`
connect error-handler to fetcher with retry
connect clear-exception to fetcher with reset
\`\`\`

Do a dry-run
------------
Enter the following to start the graph with mock input data. You are setting integer of 100 to person_id
and boolean value of "true" to exception in the input payload.

\`\`\`
start graph
int(100) -> input.body.person_id
boolean(true) -> input.body.exception
\`\`\`

Execute the run command

\`\`\`
> run
Walk to root
Executed root with skill graph.data.mapper in 0.231 ms
Walk to fetcher
Walk to dictionary
Executed dictionary with skill graph.island in 0.014 ms
Executed fetcher with skill graph.api.fetcher in 21.83 ms
Walk to error-handler
Executed error-handler with skill graph.math in 52.242 ms
Walk to fetcher
Executed fetcher with skill graph.api.fetcher in 8.025 ms
Walk to error-handler
Executed error-handler with skill graph.math in 51.824 ms
Walk to fetcher
Executed fetcher with skill graph.api.fetcher in 8.264 ms
Walk to error-handler
Executed error-handler with skill graph.math in 51.837 ms
Walk to clear-exception
Executed clear-exception with skill graph.math in 0.132 ms
Walk to fetcher
Executed fetcher with skill graph.api.fetcher in 0.547 ms
Walk to end
{
  "output": {
    "body": {
      "address": "100 World Blvd",
      "name": "Peter"
    }
  }
}
Graph traversal completed in 201 ms
\`\`\`

The graph traversal log shows that the "error-handler" node has been executed for 3 times before
the clear-exception node is executed. After clearing the exception, the mock service returns
a correct result set as "output".

Export the graph model
----------------------
Now you may save the graph model by exporting it.

\`\`\`
> export graph as tutorial-12
Graph exported to /tmp/graph/tutorial-12.json
Described in /api/graph/model/tutorial-12/591-5
\`\`\`

Deploy the graph model
----------------------
To deploy the graph model, copy "/tmp/graph/tutorial-10.json" to your application's \`main/resources/graph\` folder.
You can then test the deployed model with a curl command.

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-12 \\
  -H "Content-Type: application/json" \\
  -d '{ 
    "person_id": 100,
    "exception": true
}'
\`\`\`

Summary
-------
In this session, you have used tutorial-3 as a template and enhanced it with custom error handling.

You have used the keywords "RESET", "NEXT" and "DELAY" to reset the states of the nodes visited, to tell the
graph traversal system to route to a specific node and to introduce an artificial delay to avoid overwhelming
the target service.

IMPORTANT: Graph traversal loops
--------------------------------
The graph traversal system is designed to allow a node to be executed only once.

When using the keyword "RESET: node-name", the "seen" status and all state information are cleared so that the node
can be executed again. This would create a potential endless loop in graph traversal.

Therefore, please pay attention to have some decision logic to stop looping or retries.

As a protection mechanism, the system has a built-in loop detection logic. When a node is executed too frequently,
the graph traversal will be aborted.

The default parameters in \`application.properties\` are 10 visits per second for the same node.

\`\`\`properties
graph.max.loop.interval=1000
graph.node.high.frequency=10
\`\`\`
`,ob=`Tutorial 2
----------
In this session, you will deploy the graph model 'hello world' that you created in tutorial 1.

Exercise
--------
To deploy the graph model from tutorial 1, copy the 'tutorial-1.json' file that was exported earlier.

\`\`\`
cp /tmp/tutorial-1.json ~/sandbox/{your_minigraph_project}/src/main/resources/graph
\`\`\`

The default locations for the temp graph folder and the deployed graph folder are shown in the application.properties
file.

\`\`\`properties
#
# temp graph working location
# (temp graph location must use "file:/" prefix because of READ/WRITE requirements
#
location.graph.temp=file:/tmp/graph
#
# deployed graph model location
# (deployed graph location may use "file:/" or "classpath:/" because it is READ only
#
location.graph.deployed=classpath:/graph
\`\`\`

Invoke the graph API REST endpoint
----------------------------------
The generic graph API endpoint is \`POST /api/graph/{graph_id}\` where 'graph_id' is the name of the graph model.

To make a request to the 'tutorial-1' graph model, please enter the following curl command.

\`\`\`
> curl -X POST http://127.0.0.1:8085/api/graph/tutorial-1
hello world
\`\`\`

It will return 'hello world'.

Since the "hello world" graph model does not require any input parameter, you can also use HTTP-GET to execute
the graph.

\`\`\`
> curl http://127.0.0.1:8085/api/graph/tutorial-1
hello world
\`\`\`

In the application log, you will see the 'telemetry' of the event flow. The HTTP-POST request is received
by the 'http.flow.adapter' that executes a flow called 'graph-executor'.

The Graph Executor creates an instance of the graph, traverses from the "root" node and comes to the "end" node
that contains the "graph.data.mapper" skill. The data mapper sets the output as "hello world" that routes the
result to the "async.http.response" and the curl command receives.

\`\`\`
2026-03-31 15:19:08.052 INFO  org.platformlambda.core.services.Telemetry:81 - 
    {trace={path=POST /api/graph/tutorial-1, service=http.flow.adapter, success=true, 
     origin=20260331aa0d11b425ce44c79f00afa8947885fc, start=2026-03-31T22:19:08.051Z, exec_time=0.12, 
     from=http.request, id=2cc56126d544483abcdbc523f486a232, status=200}}
2026-03-31 15:19:08.055 INFO  org.platformlambda.core.services.Telemetry:81 - 
    {trace={path=POST /api/graph/tutorial-1, service=graph.data.mapper, success=true, 
     origin=20260331aa0d11b425ce44c79f00afa8947885fc, start=2026-03-31T22:19:08.054Z, exec_time=0.074, 
     from=graph.executor, id=2cc56126d544483abcdbc523f486a232, status=200}, annotations={node=end}}
2026-03-31 15:19:08.056 INFO  com.accenture.minigraph.services.GraphHousekeeper:44 - 
    Graph instance 2c1a00d63f7d4ec2b657db4a75021068 for model 'tutorial-1' cleared
2026-03-31 15:19:08.056 INFO  org.platformlambda.core.services.Telemetry:81 - 
    {trace={path=POST /api/graph/tutorial-1, service=task.executor, success=true, 
     origin=20260331aa0d11b425ce44c79f00afa8947885fc, exec_time=4.0, start=2026-03-31T22:19:08.051Z, 
     from=event.script.manager, id=2cc56126d544483abcdbc523f486a232, status=200}, 
     annotations={execution=Run 1 task in 4 ms, tasks=[{spent=3.477, name=graph.executor}], flow=graph-executor}}
2026-03-31 15:19:08.056 INFO  org.platformlambda.core.services.Telemetry:81 - 
    {trace={path=POST /api/graph/tutorial-1, service=async.http.response, success=true, 
    origin=20260331aa0d11b425ce44c79f00afa8947885fc, start=2026-03-31T22:19:08.055Z, exec_time=0.224, 
    from=task.executor, id=2cc56126d544483abcdbc523f486a232, status=200}}
2026-03-31 15:19:08.057 INFO  org.platformlambda.core.services.Telemetry:81 - 
    {trace={path=POST /api/graph/tutorial-1, service=graph.housekeeper, success=true, 
    origin=20260331aa0d11b425ce44c79f00afa8947885fc, start=2026-03-31T22:19:08.056Z, exec_time=0.241, 
    from=task.executor, id=2cc56126d544483abcdbc523f486a232, status=200}}
\`\`\`

Let's enhance the graph model to echo input.

Import the graph model
----------------------
You can import the tutorial-1 graph model like this:

\`\`\`
> import graph from tutorial-1
Graph model imported as draft
\`\`\`

The graph diagram is shown in the right panel under the "Graph" tab.

Edit the nodes
--------------
Enter an "edit node" command to print out the root node content.

\`\`\`
> edit node root
update node root
with type Root
with properties
name=tutorial-1
purpose=Tutorial one to return a 'hello world' message
\`\`\`

You can copy-n-paste the "update node" block into the input box and modify it as:

\`\`\`
update node root
with type Root
with properties
name=tutorial-2
purpose=Tutorial two to echo a user message
\`\`\`

Click enter and you will see:

\`\`\`
> update node root...
node root updated
\`\`\`

Then you will update the end root in the same fashion. Modify its content like this:

\`\`\`
update node end
with type End
with properties
mapping[]=input.body -> output.body
skill=graph.data.mapper
\`\`\`

Perform a Dry-Run
-----------------

To run the updated graph model, you can use the \`instantiate graph\` command with some mock input content.

\`\`\`
> instantiate graph
  text(it works) -> input.body.message
Graph instance created. Loaded 1 mock entry, model.ttl = 30000 ms
\`\`\`

In the above command, you insert the constant value "it works" into the "message" key in the "input.body"
namespace.

Enter "run" to do a dry-run and you will see this:

\`\`\`
> run
Walk to root
Walk to end
Executed end with skill graph.data.mapper in 0.43 ms
{
  "output": {
    "body": {
      "message": "it works"
    }
  }
}
Graph traversal completed in 2 ms
\`\`\`

Export the updated graph model
------------------------------
You may export the updated model graph as "tutorial 2".

\`\`\`
> export graph as tutorial-2
Graph exported to /tmp/graph/tutorial-2.json
Described in /api/graph/model/tutorial-2/235-7
\`\`\`

Deploy the graph model
----------------------
Repeat the deployment step in the beginning of this tutorial and apply it to 'tutorial-2'.

Test the deployed graph model
-----------------------------
Restart your application to load the deployed graphs into memory.

Send the following curl command

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-2 \\
  -H "Content-Type: application/json" \\
  -d '{
    "greeting": "Hello",
    "message": "it is a wonderful day"
  }'
\`\`\`

It will response with:

\`\`\`json
{
  "greeting": "Hello",
  "message": "it is a wonderful day"
}
\`\`\`

Summary
-------
In this session, you have completed the following exercise:

1. deploy the graph model 'tutorial-1' and invoke the API that executes the graph model as an instance
2. enhance the graph model from a simple 'hello world' application to an echo program
3. perform a dry-run with mock input to test the response
4. export the updated graph model as 'tutorial-2'
5. deploy 'tutorial-2' graph model
6. test the 'tutorial-2' graph model using a HTTP-POST command with some input payload
`,lb=`Tutorial 3
----------
In this session, you will learn about the data dictionary method to source data from an external service.

Exercise
--------
You will create a root node, an end node, a data dictionary node, a data provider node and an API fetcher node
as an exercise.

To clear the previous graph session, click the Tools button in the top-right corner and click the "Stop" and "Start"
toggle button. A new graph session will start.

Create root and end nodes
-------------------------
Enter the "create node" command for "root" and "end" nodes first.

\`\`\`
create node root
with type Root
with properties
name=tutorial-3
purpose=Demonstrate data sourcing using the Data Dictionary method
\`\`\`

\`\`\`
create node end
with type End
\`\`\`

Create data dictionary items
----------------------------
A data dictionary describes a "data attribute" and its "data provider". Please enter the following:

\`\`\`
create node person-name
with type Dictionary
with properties
purpose=name of a person
provider=mdm-profile
input[]=person_id
output[]=response.profile.name -> result.name

create node person-address
with type Dictionary
with properties
purpose=address of a person
provider=mdm-profile
input[]=person_id
output[]=response.profile.address -> result.address
\`\`\`

This command create two nodes called "person-name" and "person-address" with a data provider called "mdm-profile".
The input parameter to retrieve these data attribute from the data provider is "person_id".
The output section contains a data mapping statement that maps the response's key-value(s)
as the data dictionary's result set. The "response." and "result." are namespaces that
represent the response key-values from the data provider and the result key-values obtained
with this data dictionary.

In the "person-name" data dictionary, it tells the system to extract the "profile.name" data attribute from
the response's data structure and map it as the key "name".

Create a data provider
----------------------
The data dictionary assigns a data provider "mdm-profile". We will create a node for the
data provider.

\`\`\`
create node mdm-profile
with type Provider
with properties
purpose=Master Data Management's profile management endpoint
url=http://127.0.0.1:\${rest.server.port:8080}/api/mdm/profile/{id}
method=GET
feature[]=log-request-headers
feature[]=log-response-headers
input[]=text(application/json) -> header.accept
input[]=person_id -> path_parameter.id
\`\`\`

The "url" is the REST endpoint of the target service for "mdm-profile".
The \`\${rest.server.port:8080}\` is used to obtain a key-value from the application.properties or environment variable.
The colon syntax is optional. If yes, you can set a default value.

In this example, the url has a path parameter "id".

The "feature" section tells the system to apply pre-processing and/or post-processing of HTTP request/response.
The "log-request-headers" feature will log request headers, if any and the "log-response-headers" feature will
print the HTTP response headers from the target service. These 2 features are for demonstration purpose.
In real-world use case, you may implement an "oauth2-bearer" feature. We will discuss custom feature in a
subsequent tutorial.

The input section tells the system to map HTTP request headers, path parameter, query and/or body key-values.
The namespaces are:

\`\`\`
header.
query.
path_parameter.
body.
\`\`\`

The left hand side of the input mapping is the input parameter(s) from the associated data dictionary.

Create an API fetcher
---------------------
You will create a fetcher node like this:

\`\`\`
create node fetcher
with type Fetcher
with properties
skill=graph.api.fetcher
dictionary[]=person-name
dictionary[]=person-address
input[]=input.body.person_id -> person_id
output[]=result.name -> output.body.name
output[]=result.address -> output.body.address
\`\`\`

After this step, you will see 6 nodes in the graph diagram on the right panel.

Connect the fetcher
-------------------
You will connect the root node to the fetcher node and then connect it to the end node.

\`\`\`
> connect root to fetcher with fetch
node root connected to fetcher
> connect fetcher to end with complete
node fetcher connected to end
\`\`\`

Export the graph model
----------------------
The graph model is complete. Let's export it as 'tutorial-3'.

\`\`\`
> export graph as tutorial-3
Graph exported to /tmp/graph/tutorial-3.json
Described in /api/graph/model/tutorial-3/849-13
\`\`\`

Test the fetcher node
---------------------
Before you do a dry-run, you can test the fetcher alone because it is self-contained. It maps the input parameter
to 'person_id', makes an outgoing HTTP request using the data dictionary and returns the result as "output.body".

First, you can instantiate the graph model and mock the input parameter like this:

\`\`\`
instantiate graph
int(100) -> input.body.person_id
\`\`\`

The system will acknowledge your command as follows:

\`\`\`
> instantiate graph...
Graph instance created. Loaded 1 mock entry, model.ttl = 30000 ms
\`\`\`

Before you test the fetcher, you can check the input and output key-values with the \`inspect\` command:

\`\`\`
> inspect input
{
  "inspect": "input",
  "outcome": {
    "body": {
      "person_id": 100
    }
  }
}
> inspect output
{
  "inspect": "output",
  "outcome": {}
}
\`\`\`

When a graph model is instantiated, the system creates a temporary "state machine" for each graph instance.
The inspect command allows you to check the current key-values in the "state machine".

The above output shows that "person_id" of integer value 100 is stored in the input.body and there is nothing
in the "output.body".

You can now test the fetcher with the "execute" command:

\`\`\`
> execute fetcher
node fetcher run for 0.266 ms with exit path 'next'
\`\`\`

The system shows that fetcher has been executed and it is ready to continue to the next node.

Now you can inspect the "output" in the state machine again.

\`\`\`
> inspect output
{
  "inspect": "output",
  "outcome": {
    "body": {
      "address": "100 World Blvd",
      "name": "Peter"
    }
  }
}
\`\`\`

It shows that the result set contains name and address obtained from the target service correctly.

Dry-Run
-------

We know that the fetcher is configured correctly. You can do a dry-run from the beginning to the end.

You can clear the state machine by instantiating the graph model using the command earlier.

\`\`\`
instantiate graph
int(100) -> input.body.person_id
\`\`\`

\`\`\`
> instantiate graph...
Graph instance created. Loaded 1 mock entry, model.ttl = 30000 ms
\`\`\`

Verify that the output's key-values are cleared when you do \`inspect output\`. Then enter \`run\`.

\`\`\`
> run
Walk to root
Walk to fetcher
Executed fetcher with skill graph.api.fetcher in 14.456 ms
Walk to end
{
  "output": {
    "body": {
      "address": "100 World Blvd",
      "name": "Peter"
    }
  }
}
Graph traversal completed in 15 ms
\`\`\`

List nodes and connections
--------------------------
Before we close this session, let's check the nodes and connections for the graph model 'tutorial-3'.

Enter the \`list nodes\` and \`list connections\` commands:

\`\`\`
> list nodes
root [Root]
fetcher [Fetcher]
mdm-profile [Provider]
person-address [Dictionary]
person-name [Dictionary]
end [End]
> list connections
root -[fetch]-> fetcher
fetcher -[complete]-> end
\`\`\`

Note that data dictionary and data provider nodes do not need to be connected. It is because they are
"configuration" nodes. They are not active nodes that can be executed by themselves. The API fetcher node
uses the configuration given in the data dictionary and data provider to make an external API call.

For more details of the data dictionary method, you may enter "help data-dictionary".

Create an island to hold data dictionary
----------------------------------------
The data dictionary and data provider nodes are not connected. To organize, you can create an "island" node
to hold them.

\`\`\`
create node dictionary
with type Island
with properties
skill=graph.island
\`\`\`

Then you can connect the data dictionary nodes and provider node to it.

\`\`\`
> connect root to dictionary with contains
node root connected to dictionary
> connect dictionary to person-name with data
node dictionary connected to person-name
> connect dictionary to person-address with data
node dictionary connected to person-address
> connect person-name to mdm-profile with provider
node person-name connected to mdm-profile
> connect person-address to mdm-profile with provider
node person-address connected to mdm-profile
> list connections
root -[contains]-> dictionary
root -[fetch]-> fetcher
dictionary -[data]-> person-address
dictionary -[data]-> person-name
person-address -[provider]-> mdm-profile
person-name -[provider]-> mdm-profile
fetcher -[complete]-> end
\`\`\`

The purpose of an "island" node is to isolate sub-graph that does not require execution.
The data dictionary and provider nodes hold configuration for the API fetcher.
They are not executable by themselves.

Connecting data dictionary and provider nodes helps to describe the relationships, but this is not mandatory.

However, for data entities such as person, account and order, defining the directional connections with relationships
is a best practice that we recommend. It is because data entities and relationships represent enterprise knowledge.

To save the updated graph model, you should export it again.

\`\`\`
> export graph as tutorial-3
Graph exported to /tmp/graph/tutorial-3.json
Described in /api/graph/model/tutorial-3/287-4
\`\`\`

Deploy the graph model
----------------------
To deploy, you may copy "/tmp/graph/tutorial-3.json" into your application's main/resources/graph folder and
restart the application. You can use the following curl command to invoke the knowledge graph endpoint.

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-3 \\
  -H "Content-Type: application/json" \\
  -d '{
    "person_id": 100
  }'
\`\`\`

Note that input parameters, if any, must be submitted as a POST request body with content type "application/json".

You will receive the following response:

\`\`\`json
{
  "address": "100 World Blvd",
  "name": "Peter"
}
\`\`\`

If you change the person_id to 10, you will receive an error because the test profile is set to 100.

\`\`\`json
{
  "message": "Profile 10 not found",
  "type": "error",
  "target": "person-name",
  "status": 400
}
\`\`\`

Well done! You have successfully created a graph model that can fetch external data.

API call optimization
---------------------
If you check the application log, you notice that each graph instance makes one HTTP call to
\`http://127.0.0.1:8085/api/mdm/profile/10\` only.

When the target URL and method for multiple data dictionary items and their input parameter(s)
are the same, the system will avoid making redundant API calls.

Therefore, it is important to configure the data dictionary and provider correctly so that
the system will efficiently fetch data.

Summary
-------
In this session, you have configured data dictionary and data provider. You have defined an API fetcher
node to use the data dictionary and data provider to fetch some data. You have deployed the graph model
and made an API request using CURL command.

You have also learnt how to organize data dictionary and provider nodes in an "island" (aka 'subgraph').
`,ib=`Tutorial 4
----------
In this session, you will setup simple mathematics and boolean operations in a graph model to make decision.

Exercise
--------
You will create a root node, an end node, a decision node as an exercise.

To clear the previous graph session, click the Tools button in the top-right corner and click the "Stop" and "Start"
toggle button. A new graph session will start.

Create root and end nodes
-------------------------
Enter the "create node" command for "root" and "end" nodes first.

\`\`\`
create node root
with type Root
with properties
name=tutorial-4
purpose=Demonstrate decision making using mathematics and boolean operations
\`\`\`

Assume there are two input parameters (a and b) and the 'decision' node will add the two numbers,
the end node will echo the input parameters and the sum of the two numbers.

\`\`\`
create node end
with type End
with properties
skill=graph.data.mapper
mapping[]=input.body.a -> output.body.a
mapping[]=input.body.b -> output.body.b
mapping[]=decision.result.c -> output.body.sum
\`\`\`

Create a decision node
----------------------
You may create node with skill 'graph.math' to do decision-making.

\`\`\`
create node decision
with type Decision
with properties
skill=graph.math
statement[]=COMPUTE: c -> {input.body.a} + {input.body.b}
statement[]='''
IF: {input.body.a} >= {input.body.b}
THEN: next
ELSE: less-than
'''
statement[]=MAPPING: text(a >= b) -> output.body.message
statement[]=MAPPING: boolean(false) -> output.body.less_than
\`\`\`

The skill "graph.math" supports statements for:

| Type         | Operation                                                    |
|--------------|--------------------------------------------------------------|
| COMPUTE      | to generate a value (LHS) from a mathematics operation (RHS) |
| IF-THEN-ELSE | to evaluate a condition with a boolean operation             |
| MAPPING      | to perform a data mapping operation                          |
| RESET        | to reset the current state of one or more nodes              |

We will discuss 'reset' feature in a more advanced tutorial chapter later.

You can use the 'triple single quote' syntax to create the IF-THEN-ELSE statement.

The IF statement is a boolean operation.
The THEN is the next step or another node when the IF statement is true.
The ELSE is the next step or another node when the IF statement is false.

Statements are evaluated in order. The 'next' statement refers to the one after the current IF-THEN-ELSE.
In the above example, the next statements are doing data mapping to set output key-values.

Create a node to handle the negative case
-----------------------------------------
Let's create a node called "less-than" to handle the negative case from the decision node.

\`\`\`
create node less-than
with type Reject
with properties
mapping[]=text(a < b) -> output.body.message
mapping[]=boolean(true) -> output.body.less_than
skill=graph.data.mapper
\`\`\`

The curly brace syntax \`{}\` is used to tell the system to get the value from the bracketed key.

A mapping statement does not need the curly brace syntax because it is designed for data mapping only where
the left-hand-side is a constant, an input parameter or a model variable and the right-hand-side is a model
variable or an output variable.

Connect the nodes
-----------------

\`\`\`
connect root to decision with evaluate
connect less-than to end with negative
connect decision to end with positive
\`\`\`

The "less-than" node is invoked by the decision node if "a < b". Therefore, it does not need to connect to the "root".
When it finishes execution, it will hand off to the "end" node. If you do a "list connections" command, you will see:

\`\`\`
> list connections
root -[evaluate]-> decision
decision -[positive]-> end
less-than -[negative]-> end
\`\`\`

You can also use the "describe node" command to see connections:

\`\`\`
> describe node decision
{
  "node": {
    "types": [
      "Decision"
    ],
    "alias": "decision",
    "id": "c9b30d7d8a6c4d49a88b5a9254fe44e2",
    "properties": {
      "skill": "graph.math",
      "statement": [
        "COMPUTE: c -> {input.body.a} + {input.body.b}",
        "IF: {input.body.a} > {input.body.b}
         THEN: next
         ELSE: less-than",        
        "MAPPING: text(a >= b) -> output.body.message",
        "MAPPING: boolean(false) -> output.body.less_than"
      ]
    }
  },
  "from": [
    "root"
  ],
  "to": [
    "end"
  ]
}
\`\`\`

Test positive case
------------------
To test a positive case, you can mock input value and instantiate the graph model. 
Note that "start" is an alias of "instantiate".

\`\`\`
start graph
int(100) -> input.body.a
int(50) -> input.body.b
\`\`\`

Then you can test the graph model with the "run" command:

\`\`\`
> run
Walk to root
Walk to decision
Executed decision with skill graph.math in 0.824 ms
Walk to end
Executed end with skill graph.data.mapper in 0.099 ms
{
  "output": {
    "body": {
      "a": 100,
      "b": 50,
      "less_than": false,
      "sum": 150.0,
      "message": "a >= b"
    }
  }
}
Graph traversal completed in 7 ms
\`\`\`

Test negative case
------------------

\`\`\`
start graph
int(180) -> input.body.a
int(250) -> input.body.b
\`\`\`

When you do a dry-run, it shows the following:

\`\`\`
> run
Walk to root
Walk to decision
Executed decision with skill graph.math in 0.394 ms
Walk to less-than
Executed less-than with skill graph.data.mapper in 0.054 ms
Walk to end
Executed end with skill graph.data.mapper in 0.051 ms
{
  "output": {
    "body": {
      "a": 180,
      "b": 250,
      "less_than": true,
      "sum": 430.0,
      "message": "a < b"
    }
  }
}
Graph traversal completed in 2 ms
\`\`\`

Export the graph model
----------------------
You may save the graph model by exporting it.

\`\`\`
> export graph as tutorial-4
Graph exported to /tmp/graph/tutorial-4.json
Described in /api/graph/model/tutorial-4/804-24
\`\`\`

Deploy the graph model
----------------------
To deploy the graph model, copy "/tmp/graph/tutorial-4.json" to your application's \`main/resources/graph\` folder.
You can then test the deployed model with a curl command.

Summary
-------
In this session, you have created a graph model to add two numbers together, compare the two numbers and return
a decision.

While this is a trivial example, it demonstrates that you can create very useful computation and evaluation
logic using an Active Knowledge Graph that contains just simple mathematics and boolean operation statements.
`,sb=`Tutorial 5
----------
In this session, we will explore parallel processing and sophisticated graph navigation using a node
with the skill 'graph.join'.

Exercise
--------
You will import the graph model from tutorial-3 and update it to fetch two user profiles at the same time.

Import a graph model
--------------------
Enter 'import graph from tutorial-3'

\`\`\`
> import graph from tutorial-3
Graph model not found in /tmp/graph/tutorial-3.json
Found deployed graph model in classpath:/graph
Please export an updated version and re-import to instantiate an instance model
\`\`\`

If you have not exported tutorial-3 earlier, the system will import it from a demo graph.

Examine the graph model
-----------------------
You can examine the graph model with the 'list nodes' and 'list connections' commands.

\`\`\`
> list nodes
root [Root]
fetcher [Fetcher]
mdm-profile [Provider]
person-address [Dictionary]
person-name [Dictionary]
end [End]
> list connections
root -[fetch]-> fetcher
fetcher -[complete]-> end
\`\`\`

Review the fetcher node
-----------------------
Enter 'edit node fetcher' to review the configuration of the node. The system displays the following:

\`\`\`
update node fetcher
with type Fetcher
with properties
dictionary[]=person-name
dictionary[]=person-address
input[]=input.body.person_id -> person_id
output[]=result.name -> output.body.name
output[]=result.address -> output.body.address
skill=graph.api.fetcher
\`\`\`

Create two new fetchers
-----------------------
Assume the use case that we want to fetch two user profiles at the same time. You will create two fetchers
like this:

\`\`\`
create node fetcher-1
with type Fetcher
with properties
dictionary[]=person-name
dictionary[]=person-address
input[]=input.body.person1 -> person_id
output[]=result.name -> model.fetcher-1.name
output[]=result.address -> model.fetcher-1.address
output[]=model.fetcher-1 -> output.body.profile[]
skill=graph.api.fetcher
\`\`\`

\`\`\`
create node fetcher-2
with type Fetcher
with properties
dictionary[]=person-name
dictionary[]=person-address
input[]=input.body.person2 -> person_id
output[]=result.name -> model.fetcher-2.name
output[]=result.address -> model.fetcher-2.address
output[]=model.fetcher-2 -> output.body.profile[]
skill=graph.api.fetcher
\`\`\`

When two skilled nodes are executed in parallel, we must pay attention to avoid one execution stepping
on the memory space of another one. In this case, we can use two temporary variables in the "state machine".

The state machine uses the namespace "model", we therefore use two variables \`model.fetcher-1\` and \`model.fetcher-2\`
to avoid concurrent updates to the same variable.

The final step of output data mapping is the use of array append syntax \`[]\`. This tells the system to append
the map containing name and address to the variable 'profile'.

Due to parallelism, the order of the array is undetermined. If you want to guarantee person1's result go to array
element-0 and person2 to element-1, set the array element index directly. e.g.

\`\`\`
output[]=model.fetcher-1 -> output.body.profile[0]
\`\`\`

\`\`\`
output[]=model.fetcher-2 -> output.body.profile[1]
\`\`\`

Since profile order does not matter in this tutorial, we will use the array append feature \`[]\`.

Create a join node
------------------
You can now create a "join" node like this:

\`\`\`
create node join
with type Join
with properties
skill=graph.join
\`\`\`

Remove the original fetcher node
--------------------------------
Enter 'delete node fetcher' to remove the original fetcher node.

\`\`\`
> delete node fetcher
node fetcher deleted
\`\`\`

After you have deleted the original fetcher, its connections to the root node and end node will be removed too.

Connect the new fetchers
------------------------
Please enter the following to define the graph navigation.

\`\`\`
connect root to fetcher-1 with one
connect root to fetcher-2 with two
connect fetcher-1 to join with join
connect fetcher-2 to join with join
connect join to end with done
\`\`\`

Do a 'list connections' to confirm the setup.

\`\`\`
> list connections
root -[one]-> fetcher-1
root -[two]-> fetcher-2
fetcher-1 -[join]-> join
fetcher-2 -[join]-> join
join -[done]-> end
\`\`\`

Perform a dry-run
-----------------
You may start the graph model with this mock input:

\`\`\`
start graph
int(100) -> input.body.person1
int(200) -> input.body.person2
\`\`\`

Then enter 'run' to execute the graph instance.

\`\`\`
> run
Walk to root
Walk to fetcher-2
Walk to fetcher-1
Executed fetcher-1 with skill graph.api.fetcher in 1.048 ms
Walk to join
Executed fetcher-2 with skill graph.api.fetcher in 0.931 ms
Walk to join
Executed join with skill graph.join in 0.04 ms
Walk to end
Executed join with skill graph.join in 0.017 ms
{
  "output": {
    "body": {
      "profile": [
        {
          "address": "100 World Blvd",
          "name": "Mary"
        },
        {
          "address": "100 World Blvd",
          "name": "Peter"
        }
      ]
    }
  }
}
Graph traversal completed in 6 ms
\`\`\`

If you check the application log, you will see the two fetchers are executed in parallel.

\`\`\`
2026-04-02 16:47:32.633 INFO  com.accenture.minigraph.skills.GraphApiFetcher:410 - 
           GET http://127.0.0.1:8085/api/mdm/profile/100, with [person_id], ttl=30000
2026-04-02 16:47:32.633 INFO  com.accenture.minigraph.skills.GraphApiFetcher:410 - 
           GET http://127.0.0.1:8085/api/mdm/profile/200, with [person_id], ttl=30000
\`\`\`

Create an island to hold data dictionary
----------------------------------------
Just like tutorial 3, you will create an island node to hold the data dictionary and provider nodes.

\`\`\`
create node dictionary
with type Island
with properties
skill=graph.island
\`\`\`

Then you can connect the data dictionary nodes and provider node to it.

\`\`\`
> connect root to dictionary with contains
node root connected to dictionary
> connect dictionary to person-name with data
node dictionary connected to person-name
> connect dictionary to person-address with data
node dictionary connected to person-address
> connect person-name to mdm-profile with provider
node person-name connected to mdm-profile
> connect person-address to mdm-profile with provider
node person-address connected to mdm-profile
> list connections
root -[contains]-> dictionary
root -[one]-> fetcher-1
root -[two]-> fetcher-2
dictionary -[data]-> person-address
dictionary -[data]-> person-name
fetcher-1 -[join]-> join
fetcher-2 -[join]-> join
person-address -[provider]-> mdm-profile
person-name -[provider]-> mdm-profile
join -[done]-> end
\`\`\`

Export the graph model
----------------------
You may save the graph model by exporting it.

\`\`\`
> export graph as tutorial-5
Graph exported to /tmp/graph/tutorial-5.json
Described in /api/graph/model/tutorial-5/920-28
\`\`\`

Deploy the graph model
----------------------
To deploy the graph model, copy "/tmp/graph/tutorial-5.json" to your application's \`main/resources/graph\` folder.
You can then test the deployed model with a curl command.

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-5 \\
  -H "Content-Type: application/json" \\
  -d '{
    "person1": 100,
    "person2": 200
  }'
\`\`\`

Summary
-------
In this session, you have created a graph model that is capable of doing parallel processing. It makes two
API requests to fetch data at the same time. The two nodes then converge into a "join" node before reaching
the "end" node.

The execution of a graph instance is guided by "graph traversal". It will follow the connections that you define
for the nodes. If a node has a skill assigned, the graph executor will run the composable function that provides
the skill. If the node does not have a skill, the graph executor will find the next 'downstream' node from there.
`,rb=`Tutorial 6
----------
In this session, we will create a graph model that would fetch an array list from one service and iterate
the elements in the array to fetch more details from another service. We will examine the use of the
"for_each" keyword.

Exercise
--------
You will import the graph model from tutorial-3 as a template and expand it to handle a multi-step
data fetch use case.

Import a graph model
--------------------
Enter 'import graph from tutorial-3'

\`\`\`
> import graph from tutorial-3
Graph model not found in /tmp/graph/tutorial-3.json
Found deployed graph model in classpath:/graph
Please export an updated version and re-import to instantiate an instance model
\`\`\`

If you have not exported tutorial-3 earlier, the system will import it from a demo graph.

Examine the graph model
-----------------------
You can examine the graph model with the 'list nodes' and 'list connections' commands.

\`\`\`
> list nodes
root [Root]
fetcher [Fetcher]
mdm-profile [Provider]
person-address [Dictionary]
person-name [Dictionary]
end [End]
> list connections
root -[fetch]-> fetcher
fetcher -[complete]-> end
\`\`\`

Create a new data dictionary node
---------------------------------
Enter the following to create a new data dictionary node "person-accounts". This uses the same data provider
"mdm-profile" to retrieve a list of accounts for the user. The list of accounts is an array of account numbers.

\`\`\`
create node person-accounts
with type Dictionary
with properties
input[]=person_id
output[]=response.accounts -> result.account_numbers
provider=mdm-profile
purpose=accounts of a person
\`\`\`

Update the fetcher
------------------
Add the dictionary item "person-accounts" in the original fetcher.

\`\`\`
update node fetcher
with type Fetcher
with properties
dictionary[]=person-name
dictionary[]=person-address
dictionary[]=person-accounts
input[]=input.body.person_id -> person_id
output[]=result.name -> output.body.name
output[]=result.address -> output.body.address
skill=graph.api.fetcher
\`\`\`

Create one more data dictionary node
------------------------------------
Create a data dictionary node "account-details" that is associated with the data provider "account-details-provider"
to retrieve account details based on person_id and account_id.

\`\`\`
create node account-details
with type Dictionary
with properties
input[]=person_id
input[]=account_id
output[]=response.account.details -> result.accounts
provider=account-details-provider
purpose=Account details
\`\`\`

Create a new data provider
--------------------------
Enter the following to create a data provider that retrieves account details.
In the feature section, there are oauth2-bearer, log-request-headers and log-response-headers.
The "oauth2-bearer" is a placeholder and you should implement according to your organization
security guideline. Functionally, it would acquire OAuth2 bearer token from a security authority 
using client-id and secret configured in the deployed environment. It should cache and refresh
the access token as required and insert the "authorization" header in a pre-processing step
for the Graph API Fetcher. The log-request-headers and log-response-headers can be used as
templates to implement your own pre-processing and post-processing features.

\`\`\`
create node account-details-provider
with type Provider
with properties
feature[]=oauth2-bearer
feature[]=log-request-headers
feature[]=log-response-headers
input[]=text(application/json) -> header.accept
input[]=text(application/json) -> header.content-type
input[]=person_id -> body.person_id
input[]=account_id -> body.account_id
method=POST
purpose=Account Management Endpoint
url=http://127.0.0.1:\${rest.server.port}/api/account/details
\`\`\`

Create a second fetcher
-----------------------
You will create a second fetcher as follows. You will apply the \`for_each\` statement to iterate
the array in the fetcher's result set and map each element into "model.account_number".

For each element, the input statement block will be executed to populate the input parameter "account_id".

\`\`\`
create node fetcher-2
with type Fetcher
with properties
dictionary[]=account-details
for_each[]=fetcher.result.account_numbers -> model.account_number
input[]=input.body.person_id -> person_id
input[]=model.account_number -> account_id
output[]=result.accounts -> output.body.accounts
skill=graph.api.fetcher
\`\`\`

Rearrange the connections
-------------------------
You will connect the first fetcher to the second fetcher, delete the original connection between fetcher and
the end node. Then connect the second fetcher to the end node.

Then enter 'list connections' to show the updated connections.

\`\`\`
> connect fetcher to fetcher-2 with details
node fetcher connected to fetcher-2
> delete connection fetcher and end
fetcher -> end removed
> connect fetcher-2 to end with complete
node fetcher-2 connected to end
> list connections
root -[fetch]-> fetcher
fetcher -[details]-> fetcher-2
fetcher-2 -[complete]-> end
\`\`\`

Update the root node
--------------------
Since you are using tutorial-3 graph model as a template, it is a good practice to update the root node
to describe the new purpose of tutorial-6. Enter the following.

\`\`\`
update node root
with type Root
with properties
name=tutorial-6
purpose=Demonstrate multi-step API fetching and the "for_each" method
\`\`\`

Perform a dry-run
-----------------
Enter the following to mock the input parameter of "person_id = 100".

\`\`\`
start graph
int(100) -> input.body.person_id
\`\`\`

Then enter \`run\` to do a dry-run.

You will see the following:

\`\`\`
> start graph...
Graph instance created. Loaded 1 mock entry, model.ttl = 30000 ms
> run
Walk to root
Walk to fetcher
Executed fetcher with skill graph.api.fetcher in 12.085 ms
Walk to fetcher-2
Executed fetcher-2 with skill graph.api.fetcher in 14.326 ms
Walk to end
{
  "output": {
    "body": {
      "address": "100 World Blvd",
      "name": "Peter",
      "accounts": [
        {
          "balance": 25032.13,
          "id": "a101",
          "type": "Saving"
        },
        {
          "balance": 6020.68,
          "id": "b202",
          "type": "Current"
        },
        {
          "balance": 120000.0,
          "id": "c303",
          "type": "C/D"
        },
        {
          "balance": 6000.0,
          "id": "d400",
          "type": "apple"
        },
        {
          "balance": 8200.0,
          "id": "e500",
          "type": "google"
        }
      ]
    }
  }
}
Graph traversal completed in 28 ms
\`\`\`

Parallelism
-----------
When using the "for_each" method, the system will perform parallel API fetching. The default concurrency is 3.
If you want to change this value, set "concurrency" in "fetcher-2" to try.

With concurrency of 3 and there are 5 accounts, the system will perform a batch of 3 and a batch of 2 API requests.
When you changed the concurrency setting, you will see the batch size will be adjusted accordingly.

Create an island to hold data dictionary
----------------------------------------
You will create an island node to organize the data dictionary and provider nodes.

\`\`\`
create node dictionary
with type Island
with properties
skill=graph.island
\`\`\`

Then you can connect the data dictionary nodes and provider node to it.

\`\`\`
> connect root to dictionary with contains
node root connected to dictionary
> connect dictionary to person-name with data
node dictionary connected to person-name
> connect dictionary to person-address with data
node dictionary connected to person-address
> connect dictionary to person-accounts with data
node dictionary connected to person-accounts
> connect person-name to mdm-profile with provider
node person-name connected to mdm-profile
> connect person-address to mdm-profile with provider
node person-address connected to mdm-profile
> connect person-accounts to mdm-profile with provider
node person-accounts connected to mdm-profile
> connect dictionary to account-details with data
node dictionary connected to account-details
> connect account-details to account-details-provider with data
node account-details connected to account-details-provider
> list connections
root -[contains]-> dictionary
root -[fetch]-> fetcher
account-details -[provider]-> account-details-provider
dictionary -[data]-> account-details
dictionary -[data]-> person-accounts
dictionary -[data]-> person-address
dictionary -[data]-> person-name
fetcher -[details]-> fetcher-2
person-accounts -[provider]-> mdm-profile
person-address -[provider]-> mdm-profile
person-name -[provider]-> mdm-profile
fetcher-2 -[complete]-> end
\`\`\`

Export the graph model
----------------------
You may save the graph model by exporting it.

\`\`\`
> export graph as tutorial-6
Graph exported to /tmp/graph/tutorial-6.json
Described in /api/graph/model/tutorial-6/775-18
\`\`\`

Deploy the graph model
----------------------
To deploy the graph model, copy "/tmp/graph/tutorial-6.json" to your application's \`main/resources/graph\` folder.
You can then test the deployed model with a curl command.

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-6 \\
  -H "Content-Type: application/json" \\
  -d '{
    "person_id": 100
  }'
\`\`\`

Summary
-------
In this session, you have created a graph model that performs 2 steps of API fetching. The first one gets the
name, address and list of account numbers. The second one uses the account numbers to fetch the account details
for each account using the "for_each" method.
`,cb=`Tutorial 7
----------
In this session, we will discuss data mapping in more details.

Exercise
--------
You will create a new graph model with to test various data mapping methods.

To clear the previous graph session, click the Tools button in the top-right corner and click the "Stop" and "Start"
toggle button. A new graph session will start.

Create a root node and an end node
----------------------------------
Enter the following to create a root node and an end node

\`\`\`
create node root
with type Root
with properties
name=tutorial-7
purpose=Demonstrate various data mapping methods
\`\`\`

\`\`\`
create node end
with type End
with properties
\`\`\`

Create a data mapper node
-------------------------
Let's try some data mapping methods. Please enter the following:

\`\`\`
create node data-mapper
with type Mapper
with properties
mapping[]=text(world) -> output.body.hello
mapping[]=input.body.profile.name -> output.body.name
mapping[]=model.none -> model.address
mapping[]=input.body.profile.address1 -> model.address[]
mapping[]=input.body.profile.address2 -> model.address[]
mapping[]=model.address -> output.body.address
mapping[]=f:now(text(local)) -> output.body.time
\`\`\`

\`mapping[]\` tells the system to create a data mapping statement in "append mode"
so that the statements will be evaluated in the order that they are provided.

Each data mapping statement has a left-hand-side and right-hand-side separated by the "map to" (\`->\`) indicator.

The value of the left-hand-side will be mapped to the key of the right-hand-side.

The MiniGraph system uses the same Event Script's data mapping syntax. For more details, please refer to
[Data Mapping Syntax](https://accenture.github.io/mercury-composable/guides/CHAPTER-4/#tasks-and-data-mapping)
(*right-click to open new tab*).

*Constant* - 'text(world)' means a constant of "world". \`output.body.\` is the namespace for the output payload
when a graph finishes execution. In this example, the output.body will be populated with "hello=world".

*Input* - \`input.body\` is the namespace for input payload that is provided to a graph instance when it is started.

Assuming the input payload looks like this:

\`\`\`json
{ 
  "profile": {
    "name": "Peter",
    "address1": "100 World Blvd",
    "address2": "New York"
  }
}
\`\`\`

The value "Peter" will be mapped to the "name" field and the address1 and address2 as the first and second element
of an array in "model.address". The \`model.\` namespace refers to a temporal state machine during the execution of 
the graph instance. You can use the model key-values as temporary data buffer for data transformation.

*Output* - the mapping statement \`model.address -> output.body.address\` maps the address array with 2 elements
into the output payload of the graph instance when it finishes execution.

*Idempotent design* - the array append syntax (\`[]\`) would create side effect when the same array key has been used
more than once. For example, during testing, you may execute the same node multiple times. This would create
duplicated entries in the array. To ensure idempotence, you can clear the model array key before you append values.
This is done by mapping an non-existent model key (e.g. \`model.none\`) to the model.address array field.

For this exercise, a better solution would be direct addressing instead of "append" mode:

\`\`\`
mapping[]=input.body.profile.address1 -> model.address[0]
mapping[]=input.body.profile.address2 -> model.address[1]
mapping[]=model.address -> output.body.address
\`\`\`

It achieves the same outcome without using the clear variable method (\`model.none -> model.address\`).

*plugin functions* - the left-hand-side of \`f:now(text(local)) -> output.body.time\` uses the "f:" syntax
to execute a "plugin" function called "now". It takes the constant value of "local" to return a local time stamp.

A number of built-in data mapping plugins are available. Please refer to the Event Script syntax page above for
more details.

Test the data mapper
--------------------
You can test the data mapper before you complete the whole graph model.

Enter the following to instantiate the graph and open a dialog box to enter the mock input data.

\`\`\`
> instantiate graph
Graph instance created. Loaded 0 mock entries, model.ttl = 30000 ms
> upload mock data
Mock data loaded into 'input.body' namespace
\`\`\`

When you enter the "upload mock data" command, an input dialog box will be opened. Please paste the sample
input payload for the "profile" of "Peter" listed above.

To confirm that you have uploaded the mock input. Enter "inspect input".

\`\`\`
> inspect input
{
  "inspect": "input",
  "outcome": {
    "body": {
      "profile": {
        "address2": "New York",
        "address1": "100 World Blvd",
        "name": "Peter"
      }
    }
  }
}
\`\`\`

You can now test the data mapper by "executing" it. Enter "execute data-mapper".

\`\`\`
> execute data-mapper
ERROR: node data-mapper does not have a skill property
\`\`\`

The system rejects the request with an error message telling that the data mapper is missing a skill.

You can update the data-mapper node with the 'edit node data-mapper' command and copy-n-paste the content
to the inbox box for editing. Add "skill=graph.data.mapper" and submit.

\`\`\`
> edit node data-mapper
update node data-mapper
with type Mapper
with properties
mapping[]=text(world) -> output.body.hello
mapping[]=input.body.profile.name -> output.body.name
mapping[]=model.none -> model.address
mapping[]=input.body.profile.address1 -> model.address[]
mapping[]=input.body.profile.address2 -> model.address[]
mapping[]=model.address -> output.body.address
mapping[]=f:now(text(local)) -> output.body.time
skill=graph.data.mapper
\`\`\`

The system will display "node data-mapper updated".

To activate the updated node, you can re-start the graph instance by entering 'instantiate graph' and
'update mock data'. Submit the mock input payload.

Then execute the data-mapper again.

\`\`\`
> execute data-mapper
node data-mapper run for 0.488 ms with exit path 'next'
\`\`\`

The data-mapper runs successfully.

Inspect the model and output
----------------------------
You can inspect the model and the output key-values to see what values are mapped.

\`\`\`
> inspect model
{
  "inspect": "model",
  "outcome": {
    "address": [
      "100 World Blvd",
      "New York"
    ]
  }
}
> inspect output
{
  "inspect": "output",
  "outcome": {
    "body": {
      "address": [
        "100 World Blvd",
        "New York"
      ],
      "name": "Peter",
      "hello": "world",
      "time": "2026-04-11 19:52:22.527"
    }
  }
}
\`\`\`

Connect the nodes to complete the graph model
---------------------------------------------
Enter the two connect commands below.

\`\`\`
> connect root to data-mapper with mapping
node root connected to data-mapper
> connect data-mapper to end with complete
node data-mapper connected to end
\`\`\`

The graph model will be shown in the right panel.

Export the graph model
----------------------
You may save the graph model by exporting it.

\`\`\`
> export graph as tutorial-7
Graph exported to /tmp/graph/tutorial-7.json
Described in /api/graph/model/tutorial-7/152-13
\`\`\`

Deploy the graph model
----------------------
To deploy the graph model, copy "/tmp/graph/tutorial-7.json" to your application's \`main/resources/graph\` folder.
You can then test the deployed model with a curl command.

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-7 \\
  -H "Content-Type: application/json" \\
  -d '{ 
  "profile": {
    "name": "Peter",
    "address1": "100 World Blvd",
    "address2": "New York"
  }
}'
\`\`\`

Summary
-------
In this session, you have created a graph model that data mapping. You used the array append method to transform
the input address1 and address2 into an array. You learnt how to clear model variable using an non-existing variable
\`model.none\`. You also applied the "f:now()" plugin function to return the current time.
`,ub=`Tutorial 8
----------
In this session, we will use JSON-Path search feature to retrieve key-values from input payload.

Exercise
--------
You will import tutorial-7 and replace some data mapping statements with JSON-Path search requests.

To clear the previous graph session, click the Tools button in the top-right corner and click the "Stop" and "Start"
toggle button. A new graph session will start.

Import tutorial-7
-----------------
Enter 'import graph from tutorial-7' first.

\`\`\`
> import graph from tutorial-7
Found deployed graph model in classpath:/graph
Please export an updated version and re-import to instantiate an instance model
Graph model imported as draft
\`\`\`

Input payload
-------------
The account holder "Peter" has 2 accounts.
We will assume the following input payload data structure. You would copy-n-paste this JSON dataset
when using the "upload mock data" dialog box in this tutorial exercise.

\`\`\`json
{ 
  "profile": {
    "name": "Peter",
    "account": [
      {
        "id": "100",
        "amount": 18000.30,
        "description": "Time deposit",
        "type": "C/D"
      },
      {
        "id": "200",
        "amount": 62050.80,
        "description": "Saving account",
        "type": "Saving"
      }
    ]
  }
}
\`\`\`

Edit the data mapper node
-------------------------
Let's try some data mapping methods. Please enter the following:

\`\`\`
update node data-mapper
with type Mapper
with properties
mapping[]=input.body.profile.name -> output.body.name
mapping[]=$.input.body.profile.account[*].type -> model.type
mapping[]=$.input.body.profile.account[*].id -> model.id
mapping[]=$.input.body.profile.account[*].amount -> model.amount
skill=graph.data.mapper
\`\`\`

The above data mapping statements extract the type, id and amount from the account list in the
input payload using JSON-Path search syntax.

Test the data mapper
--------------------
Let's test the data mapper first.

Enter the following to instantiate the graph and open a dialog box to enter the mock input data.

\`\`\`
> instantiate graph
Graph instance created. Loaded 0 mock entries, model.ttl = 30000 ms
> upload mock data
Mock data loaded into 'input.body' namespace
\`\`\`

The first data mapping statement maps the input.body.profile.name into the "name" field of the output body.
The subsequent data mapping statements extract the type, id and amount key-values form the account list and
map them into the model variables type, id and amount accordingly.

When you enter the "upload mock data" command, an input dialog box will be opened. Please paste the sample
input payload listed above.

To confirm that you have uploaded the mock input. Enter "inspect input".

\`\`\`
> inspect input
{
  "inspect": "input",
  "outcome": {
    "body": {
      "profile": {
        "name": "Peter",
        "account": [
          {
            "amount": 18000.3,
            "description": "Time deposit",
            "id": "100",
            "type": "C/D"
          },
          {
            "amount": 62050.8,
            "description": "Saving account",
            "id": "200",
            "type": "Saving"
          }
        ]
      }
    }
  }
}
\`\`\`

You can now test the data mapper by "executing" it. Enter "execute data-mapper".

\`\`\`
> execute data-mapper
node data-mapper run for 0.589 ms with exit path 'next'
\`\`\`

The data-mapper runs successfully.

Inspect the model and output
----------------------------
You can inspect the model and the output key-values to see what values are mapped.

\`\`\`
> inspect model
{
  "inspect": "model",
  "outcome": {
    "amount": [
      18000.3,
      62050.8
    ],
    "id": [
      "100",
      "200"
    ],
    "type": [
      "C/D",
      "Saving"
    ]
  }
}
> inspect output
{
  "inspect": "output",
  "outcome": {
    "body": {
      "name": "Peter"
    }
  }
}
\`\`\`

This confirms that the JSON-Path commands have extracted the key-values from the account list successfully.
However, presenting data in list of key-values in maps is usually not a good schema design. It may be easier
for an application to parse the key-values but it reduces readability for a human operator.

This is just a demo to illustrate that we can use JSON-Path retrieval syntax.

Using the listOfMap plugin
--------------------------
For proper data structure representation, we can use the plugin "f:listOfMap()" to consolidate the map of lists.
You can add a data mapping statement to use the listOfMap plugin like this:

\`\`\`
update node data-mapper
with type Mapper
with properties
mapping[]=input.body.profile.name -> output.body.name
mapping[]=$.input.body.profile.account[*].type -> model.account.type
mapping[]=$.input.body.profile.account[*].id -> model.account.id
mapping[]=$.input.body.profile.account[*].amount -> model.account.amount
mapping[]=f:listOfMap(model.account) -> output.body.account
skill=graph.data.mapper
\`\`\`

Note that you add one level of key called "account" to hold the 3 maps of lists for type, id and amount.
Then you apply the plugin "f:listOfMap()" to consolidate the maps of lists into a list of maps.

When you enter 'inspect model' and 'inspect output', you will see:

\`\`\`
> inspect model
{
  "inspect": "model",
  "outcome": {
    "account": {
      "amount": [
        18000.3,
        62050.8
      ],
      "id": [
        "100",
        "200"
      ],
      "type": [
        "C/D",
        "Saving"
      ]
    }
  }
}
> inspect output
{
  "inspect": "output",
  "outcome": {
    "body": {
      "name": "Peter",
      "account": [
        {
          "amount": 18000.3,
          "id": "100",
          "type": "C/D"
        },
        {
          "amount": 62050.8,
          "id": "200",
          "type": "Saving"
        }
      ]
    }
  }
}
\`\`\`

This illustrates that the \`listOfMap\` plugin can perform simple data transformation.
This is handy when your graph model uses API fetchers to retrieve data from multiple sources.
Without writing code, you can group data from different data structures.

Using the removeKey plugin
--------------------------
For a single data source, it is indeed easier to use the plugin \`f:removeKey()\` to remove one or more keys
from the data structure.

\`\`\`
mapping[]=f:removeKey(input.body.profile.account, text(description)) -> output.body.account
\`\`\`

Let's prove this by editing the data-mapper again. We add a new data mapping statement at the end to map
the alternative solution to the "account2" field in the output payload.

\`\`\`
update node data-mapper
with type Mapper
with properties
mapping[]=input.body.profile.name -> output.body.name
mapping[]=$.input.body.profile.account[*].type -> model.account.type
mapping[]=$.input.body.profile.account[*].id -> model.account.id
mapping[]=$.input.body.profile.account[*].amount -> model.account.amount
mapping[]=f:listOfMap(model.account) -> output.body.account
mapping[]=f:removeKey(input.body.profile.account, text(description)) -> output.body.account2
skill=graph.data.mapper
\`\`\`

You will do 'instantiate graph' and 'upload mock data' with the same input payload.
Then 'execute data-mapper' and 'inspect output' to see the outcome.

\`\`\`
> execute data-mapper
node data-mapper run for 2.826 ms with exit path 'next'
> inspect output
{
  "inspect": "output",
  "outcome": {
    "body": {
      "name": "Peter",
      "account2": [
        {
          "amount": 18000.3,
          "id": "100",
          "type": "C/D"
        },
        {
          "amount": 62050.8,
          "id": "200",
          "type": "Saving"
        }
      ],
      "account": [
        {
          "amount": 18000.3,
          "id": "100",
          "type": "C/D"
        },
        {
          "amount": 62050.8,
          "id": "200",
          "type": "Saving"
        }
      ]
    }
  }
}
\`\`\`

Note that "account" and "account2" have the same key-values and data structure. This confirms that
the "description" key-value has been removed from each map in a list successfully.

Export the graph model
----------------------
As a good practice, you may save the graph model by exporting it.

\`\`\`
> export graph as tutorial-8
Graph exported to /tmp/graph/tutorial-8.json
Described in /api/graph/model/tutorial-8/315-6
\`\`\`

Deploy the graph model
----------------------
To deploy the graph model, copy "/tmp/graph/tutorial-8.json" to your application's \`main/resources/graph\` folder.
You can then test the deployed model with a curl command.

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-8 \\
  -H "Content-Type: application/json" \\
  -d '{ 
  "profile": {
    "name": "Peter",
    "account": [
      {
        "id": "100",
        "amount": 18000.30,
        "description": "Time deposit",
        "type": "C/D"
      },
      {
        "id": "200",
        "amount": 62050.80,
        "description": "Saving account",
        "type": "Saving"
      }
    ]
  }
}'
\`\`\`

Summary
-------
In this session, you have created a graph model that uses JSON-Path retrieval and search features. 
You have applied the plugin "f:listOfMap()" to consolidate maps of lists into a list of maps.
You have also tested the plugin "f:removeKey()" to remove unwanted key-values from a list of maps.

Note that JSON-Path retrieval and search syntax supports value comparison for selective key-value retrieval.
Please refer to JSON-Path syntax on the web for more details.
`,db=`Tutorial 9
----------
In this session, we will discuss the 'reusable module' use case.

Exercise
--------
You will create a reusable module and put it in a common graph model. Then create another graph model and
import the reusable module into the graph model to reuse it.

To clear the previous graph session, click the Tools button in the top-right corner and click the "Stop" and "Start"
toggle button. A new graph session will start.

What is a reusable module?
--------------------------
A module is a node that contains either the graph.js or graph.math skill. For frequently used math formula
or boolean operation, you can save the "common logic" in one or more module nodes and export it as a common graph model.

When you design a new graph model, you can import one or more reusable modules from the common graph model.

This is a best practice for graph modeling of common computation and decision logic so that developers do not need
to re-invent the same logic. This also encourages quality control and governance.

For this tutorial, we will skip the export of the common graph model and focus in creation of a reusable module
and illustration of how to use it in a graph model.

Create a root node and an end node
----------------------------------
Enter the following to create a root node and an end node

\`\`\`
create node root
with type Root
with properties
name=tutorial-9
purpose=Demonstrate use of modules
\`\`\`

\`\`\`
create node end
with type End
\`\`\`

Create a reusable module
-------------------------
You will create a simple "addition" module by adding two numbers and save the result in a variable called "sum".

\`\`\`
create node addition
with type Module
with properties
skill=graph.math
statement[]=COMPUTE: sum -> {model.a} + {model.b}
\`\`\`

Test the module
---------------
Enter the following to start the graph model and set two numbers in variable "a" and "b" in the state machine
"model".

\`\`\`
instantiate graph
int(10) -> model.a
int(20) -> model.b
\`\`\`

You can then test the module using 'execute addition'. 

\`\`\`
> execute addition
node addition run for 0.312 ms with exit path 'next'
\`\`\`

Then you can inspect the node.

\`\`\`
> inspect addition
{
  "inspect": "addition",
  "outcome": {
    "result": {
      "sum": 30.0
    },
    "decision": "next"
  }
}
\`\`\`

You can see the module adds the two numbers and save the result "30.0" into the variable "sum" in the result set
of the node.

Using the new module
--------------------
You will create a new node to use the module.

\`\`\`
create node compute
with type Compute
with properties
skill=graph.math
statement[]=MAPPING: input.body.a -> model.a
statement[]=MAPPING: input.body.b -> model.b
statement[]=EXECUTE: addition
statement[]=MAPPING: compute.result.sum -> output.body.sum
\`\`\`

In this node, it maps the input parameter "a" and "b" into the model variable "a" and "b".
Then it executes the module "addition". The computed result is saved in the "compute" node.
The last statement maps the computed value to the output payload "output.body.sum".

Test the compute node
---------------------
You will instantiate the graph model like this:

\`\`\`
instantiate graph
int(10) -> input.body.a
int(20) -> input.body.b
\`\`\`

Then you enter 'execute compute'. It will invoke the node 'compute' and it maps the input parameters to the model
variables. Then it executes the module "addition" that adds the two model variables together.

Inspect the result
------------------
The result is saved to the variable "sum" under the "compute" node instead of the module "addition".
It is because the compute node is the one that executes the statements.
It just borrows the logic from the module "addition".

\`\`\`
> inspect compute
{
  "inspect": "compute",
  "outcome": {
    "result": {
      "sum": 30.0
    },
    "decision": "next"
  }
}
> inspect model
{
  "inspect": "model",
  "outcome": {
    "a": 10,
    "b": 20
  }
}
> inspect addition
{
  "inspect": "addition",
  "outcome": {}
}
> inspect output
{
  "inspect": "output",
  "outcome": {
    "body": {
      "sum": 30.0
    }
  }
}
\`\`\`

Now the module works as expected.

Connect the nodes
-----------------
You will connect the nodes with the following commands:

\`\`\`
connect root to compute with calculate
connect compute to end with finish
\`\`\`

Test the completed model
------------------------
You will enter the following to test the whole model.

\`\`\`
start graph
int(10) -> input.body.a
int(20) -> input.body.b
\`\`\`

and enter 'run' to do a 'dry-run' from the root to the end node.

\`\`\`
> run
Walk to root
Walk to compute
Executed compute with skill graph.math in 0.387 ms
Walk to end
{
  "output": {
    "body": {
      "sum": 30.0
    }
  }
}
Graph traversal completed in 7 ms
\`\`\`

Check the nodes and connections
-------------------------------
Enter the following to show the nodes and connections

\`\`\`
> list nodes
root [Root]
addition [Module]
compute [Compute]
end [End]
> list connections
root -[calculate]-> compute
compute -[finish]-> end
\`\`\`

Note that the module "addition" does not need to be connected because it is a reusable module. The node that executes
it must be connected so that the graph executor can execute it when the graph traversal starts.

Create an island to hold modules
--------------------------------
You will create an island node to organize one or more module nodes.

\`\`\`
create node modules
with type Island
with properties
skill=graph.island
\`\`\`

Then you can connect the data dictionary nodes and provider node to it.

\`\`\`
> connect root to modules with contains
node root connected to modules
> connect modules to addition with contains
node modules connected to addition
> list connections
root -[calculate]-> compute
root -[contains]-> modules
modules -[contains]-> addition
compute -[finish]-> end
\`\`\`

Export the graph model
----------------------
As a good practice, you may save the graph model by exporting it.

\`\`\`
> export graph as tutorial-9
Graph exported to /tmp/graph/tutorial-9.json
Described in /api/graph/model/tutorial-9/359-15
\`\`\`

Deploy the graph model
----------------------
To deploy the graph model, copy "/tmp/graph/tutorial-9.json" to your application's \`main/resources/graph\` folder.
You can then test the deployed model with a curl command.

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-9 \\
  -H "Content-Type: application/json" \\
  -d '{ 
    "a": 10,
    "b": 20
}'
\`\`\`

Summary
-------
In this session, you have created a graph model that contains a compute node that executes a reusable module.
`,pb=`Update a node
-------------
1. Root node must use the name 'root'
2. Skill is a property with the name 'skill'. A node has zero or one skill.
3. The 'update node' is a multi-line command
4. Properties are optional for a graph model. If present, they are used as default value.
5. For each property, you can use the "triple single quotes" to enter a multi-line value if needed.
6. Node name and type should use lower case characters and hyphen only
7. Type and key-values will be used and validated by the node's skill function if any
8. The key of a property can be a composable key using the dot-bracket format.
   The value may use Event Script's constant syntax.

Syntax
------
\`\`\`
update node {name}
with type={type}
with properties
{key1}={value1}
{key2}={value2}
...
\`\`\`
`,hb=`Upload mock data to current graph instance
------------------------------------------
When the following command is entered, the system will print out a URL for you to upload
a JSON payload to the current graph instance.

Syntax
------
\`\`\`
upload mock data
\`\`\`

Upon receiving a HTTP POST request to the given URL, the JSON request payload will be used
as mock "input.body".

If you want to mock some input headers or the state machine, please use the "instantiate graph" command
before uploading.
`,fb=`MiniGraph
---------
A mini-graph is a property graph that is designed to run entirely in memory.
It is recommended that you limit the number of nodes to less than 750.

Graph Model is used to describe a business use case using graph methodology.
Optionally, you may configure a nodes to have a special skill to react to incoming events.

Instance Model is an instance of a graph model that is used to process a specific business use case
or transaction. It is created when an incoming event arrives. It will map data attributes from input
of a request to properties of one or more nodes.

Execution of an instance model will start from the root node of a graph until it reaches the end node.
Result of the end node will be returned to the calling party.

For a model to be meaningful, you must configure at least one node to have a skill to process the data
attributes of some nodes (aka "data entities"). A skill is a property with the label "skill" and the
value is a composable function route name.

For more information about each feature, try the following help topics.

For graph model
---------------
- help create (node)
- help delete (node, connection or cache)
- help update (node)
- help edit (node)
- help connect (node-A to node-B)
- help list (node or connection)
- help export
- help import (graph or node)
- help describe (graph, node, connection or skill)
- help data-dictionary
- help session (display, subscribe or reset session)

For instance model
------------------
- help instantiate (create an instance from a graph model)
- help upload (mock data)
- help execute (skill of a specific node. Graph traversal is paused to enable functional test in isolation.)
- help inspect (state-machine for properties of nodes, input, output and model namespaces)
- help run (execute a graph instance from a root node to the end node, if any, using graph traversal.)
- help seen (display the nodes that have been seen or executed)

Built-in skills
---------------
1. graph.data.mapper - map data from one node to another
2. graph.math - perform simple math function and boolean operation using native Java
3. graph.js - handle simple math function and boolean operation using a JavaScript engine
4. graph.api.fetcher - make API call to other systems
5. graph.extension - issue API call to another graph model
6. graph.island - this indicates that the node leads to isolated nodes and graph traversal would pause
7. graph.join - a node with this skill will wait for completion of all nodes that connect to it

Tutorials
---------
- help tutorial 1 (your first 'hello world' graph model)
- help tutorial 2 (deploying a graph model)
- help tutorial 3 (data dictionary, provider and API fetcher)
- help tutorial 4 (decision-making using mathematics and boolean operations)
- help tutorial 5 (more sophisticated graph navigation)
- help tutorial 6 (iterative API fetching using the 'for_each' keyword)
- help tutorial 7 (data mapping)
- help tutorial 8 (JSON-Path key-value retrieval and search)
- help tutorial 9 (reusable 'modules')
- help tutorial 10 (graph extension)
- help tutorial 11 (flow extension)
- help tutorial 12 (custom error handling)
`,mb=Object.assign({"../../../src/main/resources/help/help connect.md":k1,"../../../src/main/resources/help/help create.md":O1,"../../../src/main/resources/help/help data-dictionary.md":R1,"../../../src/main/resources/help/help delete.md":z1,"../../../src/main/resources/help/help describe.md":B1,"../../../src/main/resources/help/help edit.md":G1,"../../../src/main/resources/help/help execute.md":H1,"../../../src/main/resources/help/help export.md":U1,"../../../src/main/resources/help/help graph-api-fetcher.md":L1,"../../../src/main/resources/help/help graph-data-mapper.md":Y1,"../../../src/main/resources/help/help graph-extension.md":q1,"../../../src/main/resources/help/help graph-island.md":I1,"../../../src/main/resources/help/help graph-join.md":X1,"../../../src/main/resources/help/help graph-js.md":J1,"../../../src/main/resources/help/help graph-math.md":V1,"../../../src/main/resources/help/help import.md":Z1,"../../../src/main/resources/help/help inspect.md":Q1,"../../../src/main/resources/help/help instantiate.md":K1,"../../../src/main/resources/help/help list.md":$1,"../../../src/main/resources/help/help run.md":P1,"../../../src/main/resources/help/help seen.md":W1,"../../../src/main/resources/help/help session.md":F1,"../../../src/main/resources/help/help tutorial 1.md":eb,"../../../src/main/resources/help/help tutorial 10.md":tb,"../../../src/main/resources/help/help tutorial 11.md":nb,"../../../src/main/resources/help/help tutorial 12.md":ab,"../../../src/main/resources/help/help tutorial 2.md":ob,"../../../src/main/resources/help/help tutorial 3.md":lb,"../../../src/main/resources/help/help tutorial 4.md":ib,"../../../src/main/resources/help/help tutorial 5.md":sb,"../../../src/main/resources/help/help tutorial 6.md":rb,"../../../src/main/resources/help/help tutorial 7.md":cb,"../../../src/main/resources/help/help tutorial 8.md":ub,"../../../src/main/resources/help/help tutorial 9.md":db,"../../../src/main/resources/help/help update.md":pb,"../../../src/main/resources/help/help upload.md":hb,"../../../src/main/resources/help/help.md":fb});function gb(r){const t=r.split("/");return(t[t.length-1]??r).replace(/\.md$/,"")}const Uf=Object.fromEntries(Object.entries(mb).map(([r,t])=>[gb(r),t]));function ss(r){const t=r===""?"help":`help ${r}`;return Uf[t]??null}const yb=Object.keys(Uf).filter(r=>r!=="help").map(r=>r.replace(/^help\s+/,"")).sort(),Gc=[{id:"overview",label:"Overview"},{id:"graph-model",label:"Graph Model"},{id:"graph-skills",label:"Graph Skills"},{id:"instance-model",label:"Instance Model"},{id:"tutorials",label:"Tutorials",chipStripLabel:"Chapters"}],bb=new Set(["execute","inspect","instantiate","run","seen","upload"]);function Lf(r){return r===""?"overview":r.startsWith("tutorial ")?"tutorials":r.startsWith("graph-")?"graph-skills":bb.has(r)?"instance-model":"graph-model"}function Hc(r){if(r==="overview")return[""];const t=yb.filter(s=>Lf(s)===r);return r==="tutorials"?[...t].sort((s,c)=>{const d=parseInt(s.replace(/^tutorial\s+/,""),10),p=parseInt(c.replace(/^tutorial\s+/,""),10);return d-p}):t}function vb(r,t){return r===""?"Overview":t==="tutorials"?r.replace(/^tutorial\s+/,""):r}const Ho=Gc.flatMap(r=>Hc(r.id));function Yf(r){return r.replace(/^help\s*/i,"").trim().toLowerCase()}function _b(r){const t=Se.c(6),{bus:s,setHelpTopic:c,onTabSwitch:d}=r,p=x.useRef(d);let b;t[0]!==d?(b=()=>{p.current=d},t[0]=d,t[1]=b):b=t[1],x.useEffect(b);let g,f;t[2]!==s||t[3]!==c?(g=()=>s.on("command.helpOrDescribe",y=>{if(!y.commandText.trim().toLowerCase().startsWith("help"))return;const v=Yf(y.commandText);ss(v)!==null&&(c(v),p.current())}),f=[s,c],t[2]=s,t[3]=c,t[4]=g,t[5]=f):(g=t[4],f=t[5]),x.useEffect(g,f)}function xb(r){const t=Se.c(12),{ctx:s,navigate:c,addToast:d,wsPath:p}=r;let b;t[0]===Symbol.for("react.memo_cache_sentinel")?(b=ia.find(Sb),t[0]=b):b=t[0];const g=b,f=x.useRef(null),y=g==null?void 0:g.wsPath;let w,v;t[1]!==d||t[2]!==s||t[3]!==c?(w=()=>{if(!y||!f.current)return;if(s.getSlot(y).phase==="connected"){const{wsPath:T,json:C}=f.current;f.current=null,s.setPendingPayload(T,C),c(g.path),d("JSON loaded into JSON-Path editor ✓","success")}},v=[y,s,c,d,g],t[1]=d,t[2]=s,t[3]=c,t[4]=w,t[5]=v):(w=t[4],v=t[5]),x.useEffect(w,v);let S;t[6]!==d||t[7]!==s||t[8]!==c?(S=_=>{if(!g)return;const T=s.getSlot(g.wsPath);T.phase==="connected"?(s.setPendingPayload(g.wsPath,_),c(g.path),d("JSON loaded into JSON-Path editor ✓","success")):T.phase==="connecting"?(f.current={wsPath:g.wsPath,json:_},d("Updated pending JSON transfer — latest payload will open when connected","info")):(f.current={wsPath:g.wsPath,json:_},s.connect(g.wsPath,d),d("Connecting to JSON-Path Playground…","info"))},t[6]=d,t[7]=s,t[8]=c,t[9]=S):S=t[9];const D=S,N=g&&p!==g.wsPath?D:void 0;let E;return t[10]!==N?(E={handleSendToJsonPath:N},t[10]=N,t[11]=E):E=t[11],E}function Sb(r){return r.tabs.includes("payload")&&r.supportsUpload}function wb(r){const t=Se.c(7),{bus:s,onOpenModal:c,modalOpen:d}=r,p=x.useRef(!1);let b,g;t[0]!==d?(b=()=>{d||(p.current=!1)},g=[d],t[0]=d,t[1]=b,t[2]=g):(b=t[1],g=t[2]),x.useEffect(b,g);let f,y;t[3]!==s||t[4]!==c?(f=()=>s.on("upload.invitation",w=>{p.current||(p.current=!0,c(w.uploadPath))}),y=[s,c],t[3]=s,t[4]=c,t[5]=f,t[6]=y):(f=t[5],y=t[6]),x.useEffect(f,y)}function Eb(r){const t=Se.c(17),{bus:s,addToast:c}=r,[d,p]=x.useState(null),b=x.useRef(null);let g;t[0]===Symbol.for("react.memo_cache_sentinel")?(g=new Set,t[0]=g):g=t[0];const[f,y]=x.useState(g);let w;t[1]===Symbol.for("react.memo_cache_sentinel")?(w=M=>{b.current=document.activeElement,p(M)},t[1]=w):w=t[1];const v=w;let S;t[2]===Symbol.for("react.memo_cache_sentinel")?(S=()=>{p(null),setTimeout(()=>{var M;return(M=b.current)==null?void 0:M.focus()},0)},t[2]=S):S=t[2];const D=S;let N;t[3]!==c||t[4]!==d?(N=M=>{y(X=>new Set([...X,d])),p(null),setTimeout(()=>{var X;return(X=b.current)==null?void 0:X.focus()},0),c("Mock data uploaded successfully ✓","success")},t[3]=c,t[4]=d,t[5]=N):N=t[5];const E=N;let _;t[6]!==c?(_=M=>{c(`Upload failed: ${M}`,"error")},t[6]=c,t[7]=_):_=t[7];const T=_;let C;t[8]===Symbol.for("react.memo_cache_sentinel")?(C=()=>{y(new Set)},t[8]=C):C=t[8];const j=C,L=d!==null;let U;t[9]!==s||t[10]!==L?(U={bus:s,onOpenModal:v,modalOpen:L},t[9]=s,t[10]=L,t[11]=U):U=t[11],wb(U);let V;return t[12]!==T||t[13]!==E||t[14]!==d||t[15]!==f?(V={modalUploadPath:d,successfulUploadPaths:f,handleOpenUploadModal:v,handleCloseUploadModal:D,handleUploadSuccess:E,handleUploadError:T,resetSuccessfulPaths:j},t[12]=T,t[13]=E,t[14]=d,t[15]=f,t[16]=V):V=t[16],V}function Tb(r){const t=Se.c(14),{bus:s,connected:c,appendMessage:d,addToast:p}=r,b=x.useRef(null),g=x.useRef(!1),f=x.useRef(d);let y,w;t[0]!==d?(y=()=>{f.current=d},w=[d],t[0]=d,t[1]=y,t[2]=w):(y=t[1],w=t[2]),x.useEffect(y,w);const v=x.useRef(p);let S,D;t[3]!==p?(S=()=>{v.current=p},D=[p],t[3]=p,t[4]=S,t[5]=D):(S=t[4],D=t[5]),x.useEffect(S,D);let N,E;t[6]!==c?(N=()=>{var L;c||((L=b.current)==null||L.abort(),b.current=null,g.current=!1)},E=[c],t[6]=c,t[7]=N,t[8]=E):(N=t[7],E=t[8]),x.useEffect(N,E);let _,T;t[9]===Symbol.for("react.memo_cache_sentinel")?(_=()=>()=>{var L;(L=b.current)==null||L.abort()},T=[],t[9]=_,t[10]=T):(_=t[9],T=t[10]),x.useEffect(_,T);let C,j;t[11]!==s?(j=()=>s.on("payload.large",L=>{var P;if(g.current)return;const{apiPath:U,byteSize:V}=L;(P=b.current)==null||P.abort();const M=new AbortController;b.current=M;const X=(V/1048576).toFixed(2);v.current(`Fetching large payload (${X} MB)…`,"info"),g.current=!0,fetch(U,{signal:M.signal}).then(Nb).then(z=>{if(!z.trim())throw new Error("empty response body");let H=z;try{H=JSON.stringify(JSON.parse(z),null,2)}catch{}f.current(H),g.current=!1,b.current=null}).catch(z=>{z.name!=="AbortError"&&(g.current=!1,b.current=null,f.current(`ERROR: payload fetch failed — ${z.message}`),v.current(`Payload fetch failed: ${z.message}`,"error"))})}),C=[s],t[11]=s,t[12]=C,t[13]=j):(C=t[12],j=t[13]),x.useEffect(j,C)}function Nb(r){if(!r.ok)throw new Error(`HTTP ${r.status}`);return r.text()}function Ab(r){const t=Se.c(14);let s;t[0]===Symbol.for("react.memo_cache_sentinel")?(s={},t[0]=s):s=t[0];const[c,d]=Oa(r,s);let p;t[1]!==d?(p=N=>{d(E=>({...E,[N]:{name:N,savedAt:new Date().toISOString()}}))},t[1]=d,t[2]=p):p=t[2];const b=p;let g;t[3]!==d?(g=N=>{d(E=>{const _={...E};return delete _[N],_})},t[3]=d,t[4]=g):g=t[4];const f=g;let y;t[5]!==c?(y=N=>Object.prototype.hasOwnProperty.call(c,N),t[5]=c,t[6]=y):y=t[6];const w=y;let v;t[7]!==c?(v=Object.values(c).sort(Cb),t[7]=c,t[8]=v):v=t[8];const S=v;let D;return t[9]!==f||t[10]!==w||t[11]!==b||t[12]!==S?(D={savedGraphs:S,saveGraph:b,deleteGraph:f,hasGraph:w},t[9]=f,t[10]=w,t[11]=b,t[12]=S,t[13]=D):D=t[13],D}function Cb(r,t){return new Date(t.savedAt).getTime()-new Date(r.savedAt).getTime()}function jb(r,t){const s=Se.c(11),[c,d]=Oa(r,1),p=x.useRef(!1),[b,g]=x.useState(null),[f,y]=x.useState(null);let w,v;s[0]!==t?(w=()=>t.on("command.importGraph",C=>{g(C.graphName),y(null)}),v=[t],s[0]=t,s[1]=w,s[2]=v):(w=s[1],v=s[2]),x.useEffect(w,v);let S;s[3]!==c?(S=C=>{y(C),C===`untitled-${c}`&&(p.current=!0)},s[3]=c,s[4]=S):S=s[4];const D=S;let N;s[5]!==d?(N=()=>{g(null),y(null),p.current&&d(Db),p.current=!1},s[5]=d,s[6]=N):N=s[6];const E=N,_=f??b??`untitled-${c}`;let T;return s[7]!==_||s[8]!==E||s[9]!==D?(T={defaultName:_,setLastSavedName:D,resetName:E},s[7]=_,s[8]=E,s[9]=D,s[10]=T):T=s[10],T}function Db(r){return r+1}function Mb(r){const t=Se.c(27),{bus:s,connected:c,sendRawText:d,saveGraph:p,setLastSavedName:b,addToast:g}=r,f=x.useRef(null);let y;t[0]!==g||t[1]!==c||t[2]!==d?(y=V=>{if(!c){g("Save failed: connection required to export graph","error");return}const M=setTimeout(()=>{f.current!==null&&(f.current=null,g("Save failed: export confirmation timed out","error"))},1e4);f.current={graphName:V,timeoutId:M},d(`export graph as ${V}`)},t[0]=g,t[1]=c,t[2]=d,t[3]=y):y=t[3];const w=y;let v,S;t[4]!==g||t[5]!==s||t[6]!==p||t[7]!==b?(v=()=>s.on("graph.exported",V=>{if(f.current===null||V.graphName!==f.current.graphName)return;clearTimeout(f.current.timeoutId);const M=f.current.graphName;f.current=null,p(M),b(M),g(`Graph saved as "${M}"`,"success")}),S=[s,p,b,g],t[4]=g,t[5]=s,t[6]=p,t[7]=b,t[8]=v,t[9]=S):(v=t[8],S=t[9]),x.useEffect(v,S);let D,N;t[10]!==g||t[11]!==s?(D=()=>s.on("graph.export.failed",V=>{f.current!==null&&(clearTimeout(f.current.timeoutId),f.current=null,V.reason==="invalid-name"?g("Save failed: invalid filename (a–z, A–Z, 0–9, hyphen only)","error"):g("Save failed: root node name does not match existing graph","error"))}),N=[s,g],t[10]=g,t[11]=s,t[12]=D,t[13]=N):(D=t[12],N=t[13]),x.useEffect(D,N);let E,_;t[14]!==g||t[15]!==c?(E=()=>{!c&&f.current!==null&&(clearTimeout(f.current.timeoutId),f.current=null,g("Save failed: connection closed before export confirmation","error"))},_=[c,g],t[14]=g,t[15]=c,t[16]=E,t[17]=_):(E=t[16],_=t[17]),x.useEffect(E,_);let T,C;t[18]===Symbol.for("react.memo_cache_sentinel")?(T=()=>()=>{f.current!==null&&clearTimeout(f.current.timeoutId)},C=[],t[18]=T,t[19]=C):(T=t[18],C=t[19]),x.useEffect(T,C);let j;t[20]!==g||t[21]!==c||t[22]!==d?(j=V=>{c&&(d(`import graph from ${V}`),g(`Importing graph "${V}"…`,"info"))},t[20]=g,t[21]=c,t[22]=d,t[23]=j):j=t[23];const L=j;let U;return t[24]!==L||t[25]!==w?(U={handleSaveGraph:w,handleLoadGraph:L},t[24]=L,t[25]=w,t[26]=U):U=t[26],U}const Tc=new Map;function kb(r){const t=Se.c(7);let s;t[0]!==r?(s=()=>Tc.get(r)??null,t[0]=r,t[1]=s):s=t[1];const[c,d]=x.useState(s);let p;t[2]!==r?(p=f=>{d(f),f===null?Tc.delete(r):Tc.set(r,f)},t[2]=r,t[3]=p):p=t[3];const b=p;let g;return t[4]!==c||t[5]!==b?(g=[c,b],t[4]=c,t[5]=b,t[6]=g):g=t[6],g}function tf(r){if(r==null)return"";const t=typeof r=="string"?r:JSON.stringify(r);return t.includes("'''")&&console.warn(`[commandBuilder] Property value contains "'''" which cannot be escaped in the backend grammar. The value may be truncated on paste.`),t.includes(`
`)?`'''
${t}
'''`:t}function Ob(r,t){const s=[`${r} node ${t.alias}`];t.types.length>0&&s.push(`with type ${t.types[0]}`);const c=Object.entries(t.properties).filter(([,d])=>d!=null);if(c.length>0){s.push("with properties");for(const[d,p]of c)if(Array.isArray(p))for(const b of p)s.push(`${d}[]=${tf(b)}`);else s.push(`${d}[]=${tf(p)}`)}return s.join(`
`)}function nf(r,t){const s=t!=null&&t.nodes.some(c=>c.alias===r.node.alias)?"update":"create";return{verb:s,command:Ob(s,r.node)}}function Rb(r){return{execute(t){return r(t)}}}const zb="_toastContainer_hhy5k_1",Bb="_toast_hhy5k_1",Gb="_slideIn_hhy5k_1",Hb="_success_hhy5k_36",Ub="_error_hhy5k_40",Lb="_info_hhy5k_44",Yb="_toastIcon_hhy5k_48",qb="_toastMessage_hhy5k_53",Gl={toastContainer:zb,toast:Bb,slideIn:Gb,success:Hb,error:Ub,info:Lb,toastIcon:Yb,toastMessage:qb},Ib=r=>{const t=Se.c(7),{toasts:s,onRemove:c}=r;if(s.length===0)return null;let d;if(t[0]!==c||t[1]!==s){let b;t[3]!==c?(b=g=>h.jsxs("div",{className:`${Gl.toast} ${Gl[g.type]}`,onClick:()=>c(g.id),children:[h.jsxs("span",{className:Gl.toastIcon,children:[g.type==="success"&&"✅",g.type==="error"&&"❌",g.type==="info"&&"ℹ️"]}),h.jsx("span",{className:Gl.toastMessage,children:g.message})]},g.id),t[3]=c,t[4]=b):b=t[4],d=s.map(b),t[0]=c,t[1]=s,t[2]=d}else d=t[2];let p;return t[5]!==d?(p=h.jsx("div",{className:Gl.toastContainer,children:d}),t[5]=d,t[6]=p):p=t[6],p},Xb="_container_9dbh2_3",Jb="_trigger_9dbh2_7",Vb="_chevron_9dbh2_37",Zb="_chevronOpen_9dbh2_43",Qb="_dot_9dbh2_49",Kb="_dotIdle_9dbh2_56",$b="_dotConnecting_9dbh2_57",Pb="_dotConnected_9dbh2_58",Wb="_dotPartial_9dbh2_59",Fb="_dropdown_9dbh2_65",Hn={container:Xb,trigger:Jb,chevron:Vb,chevronOpen:Zb,dot:Qb,dotIdle:Kb,dotConnecting:$b,dotConnected:Pb,dotPartial:Wb,dropdown:Fb};function Uc(r){const t=Se.c(23),{label:s,dotStatus:c,children:d}=r,[p,b]=x.useState(!1),g=x.useRef(null);let f,y;t[0]!==p?(f=()=>{if(!p)return;const U=V=>{g.current&&!g.current.contains(V.target)&&b(!1)};return document.addEventListener("mousedown",U),()=>document.removeEventListener("mousedown",U)},y=[p],t[0]=p,t[1]=f,t[2]=y):(f=t[1],y=t[2]),x.useEffect(f,y);let w;t[3]===Symbol.for("react.memo_cache_sentinel")?(w=U=>{var V,M;U.key==="Escape"&&(b(!1),(M=(V=g.current)==null?void 0:V.querySelector("button[aria-haspopup]"))==null||M.focus())},t[3]=w):w=t[3];const v=w,S=c==="connected"?Hn.dotConnected:c==="connecting"?Hn.dotConnecting:c==="partial"?Hn.dotPartial:c==="idle"?Hn.dotIdle:void 0;let D;t[4]===Symbol.for("react.memo_cache_sentinel")?(D=()=>b(e0),t[4]=D):D=t[4];let N;t[5]!==S||t[6]!==c?(N=c!==void 0&&h.jsx("span",{className:`${Hn.dot} ${S??""}`,"aria-hidden":"true"}),t[5]=S,t[6]=c,t[7]=N):N=t[7];let E;t[8]!==s?(E=h.jsx("span",{children:s}),t[8]=s,t[9]=E):E=t[9];const _=`${Hn.chevron} ${p?Hn.chevronOpen:""}`;let T;t[10]!==_?(T=h.jsx("span",{className:_,"aria-hidden":"true",children:"▾"}),t[10]=_,t[11]=T):T=t[11];let C;t[12]!==p||t[13]!==N||t[14]!==E||t[15]!==T?(C=h.jsxs("button",{className:Hn.trigger,onClick:D,"aria-haspopup":"true","aria-expanded":p,children:[N,E,T]}),t[12]=p,t[13]=N,t[14]=E,t[15]=T,t[16]=C):C=t[16];let j;t[17]!==d||t[18]!==p?(j=p&&h.jsx("div",{className:Hn.dropdown,role:"menu",children:d}),t[17]=d,t[18]=p,t[19]=j):j=t[19];let L;return t[20]!==j||t[21]!==C?(L=h.jsxs("div",{className:Hn.container,ref:g,onKeyDown:v,children:[C,j]}),t[20]=j,t[21]=C,t[22]=L):L=t[22],L}function e0(r){return!r}const t0="_nav_1hfby_3",n0="_menuList_1hfby_11",a0="_menuItem_1hfby_19",o0="_toolRow_1hfby_56",l0="_toolLink_1hfby_67",i0="_toolLinkActive_1hfby_92",s0="_toolDot_1hfby_99",r0="_toolDotIdle_1hfby_106",c0="_toolDotConnecting_1hfby_107",u0="_toolDotConnected_1hfby_108",d0="_connectAllRow_1hfby_112",p0="_connectAllBtn_1hfby_118",h0="_connectAllBtnStop_1hfby_142",f0="_toolConnectBtn_1hfby_154",m0="_toolConnectBtnStop_1hfby_180",g0="_externalIcon_1hfby_192",Ot={nav:t0,menuList:n0,menuItem:a0,toolRow:o0,toolLink:l0,toolLinkActive:i0,toolDot:s0,toolDotIdle:r0,toolDotConnecting:c0,toolDotConnected:u0,connectAllRow:d0,connectAllBtn:p0,connectAllBtnStop:h0,toolConnectBtn:f0,toolConnectBtnStop:m0,externalIcon:g0};function y0(r){return r.every(t=>t==="connected")?"connected":r.every(t=>t==="idle")?"idle":r.some(t=>t==="connecting")?"connecting":"partial"}function b0(r){return r==="connected"?"connected":r==="connecting"?"connecting":"idle"}const v0=[{href:"/info",label:"Info"},{href:"/info/lib",label:"Libraries"},{href:"/info/routes",label:"Services"},{href:"/health",label:"Health"},{href:"/env",label:"Environment"},{href:"http://localhost:8085/api/ws/json",label:"Legacy JSON"},{href:"http://localhost:8085/api/ws/graph",label:"Legacy Graph"}];function _0(r){const t=Se.c(27),{addToast:s}=r,c=Pc();let d,p,b;if(t[0]!==c){const V=ia.map(M=>c.getSlot(M.wsPath).phase);b=y0(V),d=V.every(E0),p=V.some(w0),t[0]=c,t[1]=d,t[2]=p,t[3]=b}else d=t[1],p=t[2],b=t[3];const g=p;let f;t[4]!==s||t[5]!==c?(f=function(){ia.forEach(M=>{c.getSlot(M.wsPath).phase==="idle"&&c.connect(M.wsPath,s)})},t[4]=s,t[5]=c,t[6]=f):f=t[6];const y=f;let w;t[7]!==c?(w=function(){ia.forEach(M=>{const{phase:X}=c.getSlot(M.wsPath);(X==="connected"||X==="connecting")&&c.disconnect(M.wsPath)})},t[7]=c,t[8]=w):w=t[8];const v=w,S=`${Ot.connectAllBtn} ${d?Ot.connectAllBtnStop:""}`,D=d?v:y,N=g?"Connecting…":d?"Disconnect all WebSockets":"Connect all WebSockets",E=g?"Connecting…":d?"Disconnect All":"Connect All";let _;t[9]!==g||t[10]!==S||t[11]!==D||t[12]!==N||t[13]!==E?(_=h.jsx("div",{className:Ot.connectAllRow,children:h.jsx("button",{className:S,onClick:D,disabled:g,"aria-label":N,children:E})}),t[9]=g,t[10]=S,t[11]=D,t[12]=N,t[13]=E,t[14]=_):_=t[14];let T;t[15]!==s||t[16]!==c?(T=ia.map(V=>{const{phase:M}=c.getSlot(V.wsPath),X=b0(M),P=M==="connected",z=M==="connecting",H=X==="connected"?Ot.toolDotConnected:X==="connecting"?Ot.toolDotConnecting:Ot.toolDotIdle;return h.jsxs("li",{role:"none",className:Ot.toolRow,children:[h.jsxs(my,{to:V.path,role:"menuitem",className:S0,children:[h.jsx("span",{className:`${Ot.toolDot} ${H}`,"aria-hidden":"true"}),h.jsx("span",{className:Ot.toolLabel,children:V.label})]}),h.jsx("button",{className:`${Ot.toolConnectBtn} ${P?Ot.toolConnectBtnStop:""}`,onClick:()=>P||z?c.disconnect(V.wsPath):c.connect(V.wsPath,s),disabled:z,"aria-label":z?"Connecting…":P?`Disconnect ${V.label}`:`Connect ${V.label}`,title:z?"Connecting…":zf(V.wsPath),children:z?"…":P?"Stop":"Start"})]},V.path)}),t[15]=s,t[16]=c,t[17]=T):T=t[17];let C;t[18]!==T?(C=h.jsx("ul",{className:Ot.menuList,role:"none",children:T}),t[18]=T,t[19]=C):C=t[19];let j;t[20]!==C||t[21]!==_||t[22]!==b?(j=h.jsxs(Uc,{label:"Tools",dotStatus:b,children:[_,C]}),t[20]=C,t[21]=_,t[22]=b,t[23]=j):j=t[23];let L;t[24]===Symbol.for("react.memo_cache_sentinel")?(L=h.jsx(Uc,{label:"Quick Links",children:h.jsx("ul",{className:Ot.menuList,role:"none",children:v0.map(x0)})}),t[24]=L):L=t[24];let U;return t[25]!==j?(U=h.jsxs("nav",{className:Ot.nav,"aria-label":"Main navigation",children:[j,L]}),t[25]=j,t[26]=U):U=t[26],U}function x0(r){return h.jsx("li",{role:"none",children:h.jsxs("a",{href:r.href,role:"menuitem",className:Ot.menuItem,target:"_blank",rel:"noopener noreferrer",children:[r.label,h.jsx("span",{className:Ot.externalIcon,"aria-hidden":"true",children:"↗"})]})},r.href)}function S0(r){const{isActive:t}=r;return`${Ot.toolLink} ${t?Ot.toolLinkActive:""}`}function w0(r){return r==="connecting"}function E0(r){return r==="connected"}const T0="_saveBtn_1xd2l_3",N0="_saveForm_1xd2l_33",A0="_saveInput_1xd2l_39",C0="_saveInputWarn_1xd2l_55",j0="_saveWarnLabel_1xd2l_59",D0="_saveActionBtn_1xd2l_65",Wa={saveBtn:T0,saveForm:N0,saveInput:A0,saveInputWarn:C0,saveWarnLabel:j0,saveActionBtn:D0};function M0(r){const t=Se.c(33),{disabled:s,defaultName:c,onSave:d,nameExists:p,connected:b}=r,g=b===void 0?!1:b,[f,y]=x.useState(!1),[w,v]=x.useState(""),S=x.useRef(null);let D;t[0]!==c?(D=()=>{v(c),y(!0)},t[0]=c,t[1]=D):D=t[1];const N=D;let E;t[2]===Symbol.for("react.memo_cache_sentinel")?(E=()=>{y(!1),v("")},t[2]=E):E=t[2];const _=E;let T;t[3]!==d||t[4]!==w?(T=()=>{const z=w.trim();z&&(d(z),y(!1),v(""))},t[3]=d,t[4]=w,t[5]=T):T=t[5];const C=T;let j;t[6]!==C?(j=z=>{z.key==="Enter"&&(z.preventDefault(),C()),z.key==="Escape"&&(z.preventDefault(),_())},t[6]=C,t[7]=j):j=t[7];const L=j;let U,V;if(t[8]!==f?(U=()=>{var z;f&&((z=S.current)==null||z.focus())},V=[f],t[8]=f,t[9]=U,t[10]=V):(U=t[9],V=t[10]),x.useEffect(U,V),f){const z=`${Wa.saveInput}${p!=null&&p(w.trim())?` ${Wa.saveInputWarn}`:""}`;let H;t[11]===Symbol.for("react.memo_cache_sentinel")?(H=ee=>v(ee.target.value),t[11]=H):H=t[11];let G;t[12]!==L||t[13]!==w||t[14]!==z?(G=h.jsx("input",{ref:S,className:z,type:"text",value:w,onChange:H,onKeyDown:L,placeholder:"Enter a name…","aria-label":"Graph save name",maxLength:80}),t[12]=L,t[13]=w,t[14]=z,t[15]=G):G=t[15];let Q;t[16]!==p||t[17]!==w?(Q=(p==null?void 0:p(w.trim()))&&h.jsx("span",{className:Wa.saveWarnLabel,role:"status",children:"Overwrite?"}),t[16]=p,t[17]=w,t[18]=Q):Q=t[18];let Z;t[19]!==w?(Z=w.trim(),t[19]=w,t[20]=Z):Z=t[20];const ne=!Z;let k;t[21]!==C||t[22]!==ne?(k=h.jsx("button",{className:Wa.saveActionBtn,onClick:C,disabled:ne,"aria-label":"Confirm save",children:"✅"}),t[21]=C,t[22]=ne,t[23]=k):k=t[23];let R;t[24]===Symbol.for("react.memo_cache_sentinel")?(R=h.jsx("button",{className:Wa.saveActionBtn,onClick:_,"aria-label":"Cancel save",children:"❌"}),t[24]=R):R=t[24];let $;return t[25]!==G||t[26]!==Q||t[27]!==k?($=h.jsxs("div",{className:Wa.saveForm,children:[G,Q,k,R]}),t[25]=G,t[26]=Q,t[27]=k,t[28]=$):$=t[28],$}const M=s||!g,X=s?"No graph loaded":g?"Export graph snapshot to server and save bookmark":"Connect first to save";let P;return t[29]!==N||t[30]!==M||t[31]!==X?(P=h.jsx("button",{className:Wa.saveBtn,onClick:N,disabled:M,title:X,"aria-label":"Save graph snapshot",children:"💾 Save Graph"}),t[29]=N,t[30]=M,t[31]=X,t[32]=P):P=t[32],P}const k0="_empty_tpeii_3",O0="_hint_tpeii_12",R0="_list_tpeii_21",z0="_row_tpeii_31",B0="_rowInfo_tpeii_50",G0="_rowName_tpeii_58",H0="_rowMeta_tpeii_67",U0="_rowActions_tpeii_78",L0="_loadBtn_tpeii_84",Y0="_deleteBtn_tpeii_85",Un={empty:k0,hint:O0,list:R0,row:z0,rowInfo:B0,rowName:G0,rowMeta:H0,rowActions:U0,loadBtn:L0,deleteBtn:Y0};function q0(r){const t=Se.c(8),{savedGraphs:s,onLoad:c,onDelete:d,connected:p}=r,b=s.length>0?`Load Graph (${s.length})`:"Load Graph";let g;t[0]!==p||t[1]!==d||t[2]!==c||t[3]!==s?(g=s.length===0?h.jsx("p",{className:Un.empty,children:"No saved graphs yet."}):h.jsxs(h.Fragment,{children:[!p&&h.jsx("p",{className:Un.hint,children:"Connect to load a graph"}),h.jsx("ul",{className:Un.list,role:"list",children:s.map(y=>h.jsxs("li",{className:Un.row,children:[h.jsxs("div",{className:Un.rowInfo,children:[h.jsx("span",{className:Un.rowName,title:y.name,children:y.name}),h.jsx("span",{className:Un.rowMeta,children:new Date(y.savedAt).toLocaleString()})]}),h.jsxs("div",{className:Un.rowActions,children:[h.jsx("button",{className:Un.loadBtn,onClick:()=>c(y.name),disabled:!p,title:p?`Run: import graph from ${y.name}`:"Connect to the playground first","aria-label":`Load graph ${y.name}`,children:"Load"}),h.jsx("button",{className:Un.deleteBtn,onClick:()=>d(y.name),title:`Remove "${y.name}" from local storage`,"aria-label":`Delete saved graph ${y.name}`,children:"Delete"})]})]},y.name))})]}),t[0]=p,t[1]=d,t[2]=c,t[3]=s,t[4]=g):g=t[4];let f;return t[5]!==b||t[6]!==g?(f=h.jsx(Uc,{label:b,children:g}),t[5]=b,t[6]=g,t[7]=f):f=t[7],f}const I0="_payloadRoot_6u47x_2",X0="_labelRow_6u47x_10",J0="_label_6u47x_10",V0="_payloadControls_6u47x_26",Z0="_charCounter_6u47x_32",Q0="_typeIndicator_6u47x_38",K0="_validationIcon_6u47x_49",$0="_formatButton_6u47x_53",P0="_uploadButton_6u47x_67",W0="_textarea_6u47x_82",F0="_textareaError_6u47x_107",ev="_errorMessage_6u47x_109",tv="_sampleButtonsRow_6u47x_117",nv="_sampleButtons_6u47x_117",av="_sampleLabel_6u47x_130",ov="_sampleGroup_6u47x_136",lv="_sampleGroupLabel_6u47x_143",iv="_sampleButton_6u47x_117",yt={payloadRoot:I0,labelRow:X0,label:J0,payloadControls:V0,charCounter:Z0,typeIndicator:Q0,validationIcon:K0,formatButton:$0,uploadButton:P0,textarea:W0,textareaError:F0,errorMessage:ev,sampleButtonsRow:tv,sampleButtons:nv,sampleLabel:av,sampleGroup:ov,sampleGroupLabel:lv,sampleButton:iv};function sv(r){const t=Se.c(21),{onLoad:s}=r;let c,d,p,b,g,f;if(t[0]!==s){const v=Object.keys(ns).filter(uv),S=Object.keys(ns).filter(cv),D=rv;b=yt.sampleButtons,t[7]===Symbol.for("react.memo_cache_sentinel")?(g=h.jsx("span",{className:yt.sampleLabel,children:"Quick load:"}),t[7]=g):g=t[7];let N;t[8]===Symbol.for("react.memo_cache_sentinel")?(N=h.jsx("span",{className:yt.sampleGroupLabel,children:"JSON:"}),t[8]=N):N=t[8];const E=v.map(_=>h.jsx("button",{className:yt.sampleButton,onClick:()=>s(ns[_]),children:D(_)},_));t[9]!==E?(f=h.jsxs("div",{className:yt.sampleGroup,children:[N,E]}),t[9]=E,t[10]=f):f=t[10],c=yt.sampleGroup,t[11]===Symbol.for("react.memo_cache_sentinel")?(d=h.jsx("span",{className:yt.sampleGroupLabel,children:"XML:"}),t[11]=d):d=t[11],p=S.map(_=>h.jsx("button",{className:yt.sampleButton,onClick:()=>s(ns[_]),children:D(_)},_)),t[0]=s,t[1]=c,t[2]=d,t[3]=p,t[4]=b,t[5]=g,t[6]=f}else c=t[1],d=t[2],p=t[3],b=t[4],g=t[5],f=t[6];let y;t[12]!==c||t[13]!==d||t[14]!==p?(y=h.jsxs("div",{className:c,children:[d,p]}),t[12]=c,t[13]=d,t[14]=p,t[15]=y):y=t[15];let w;return t[16]!==b||t[17]!==g||t[18]!==f||t[19]!==y?(w=h.jsxs("div",{className:b,children:[g,f,y]}),t[16]=b,t[17]=g,t[18]=f,t[19]=y,t[20]=w):w=t[20],w}function rv(r){return r.replace(/^(json|xml)_/,"").replace(/_/g," ")}function cv(r){return r.startsWith("xml_")}function uv(r){return r.startsWith("json_")}function dv(r){const t=Se.c(40),{payload:s,onChange:c,validation:d,onFormat:p,onUpload:b}=r;let g;t[0]===Symbol.for("react.memo_cache_sentinel")?(g=h.jsx("label",{htmlFor:"payload",className:yt.label,children:"JSON/XML Payload"}),t[0]=g):g=t[0];let f;t[1]!==s.length?(f=h.jsxs("span",{className:yt.charCounter,children:["size: ",s.length]}),t[1]=s.length,t[2]=f):f=t[2];let y;t[3]!==s||t[4]!==d.type?(y=s&&d.type&&h.jsx("span",{className:yt.typeIndicator,children:d.type.toUpperCase()}),t[3]=s,t[4]=d.type,t[5]=y):y=t[5];let w;t[6]!==s||t[7]!==d.valid?(w=s&&h.jsx("span",{className:yt.validationIcon,children:d.valid?"✅":"❌"}),t[6]=s,t[7]=d.valid,t[8]=w):w=t[8];const v=!s||d.type!=="json",S=d.type==="xml"?"Format only available for JSON":"Format JSON";let D;t[9]!==p||t[10]!==v||t[11]!==S?(D=h.jsx("button",{className:yt.formatButton,onClick:p,disabled:v,title:S,children:"Format"}),t[9]=p,t[10]=v,t[11]=S,t[12]=D):D=t[12];let N;t[13]!==b||t[14]!==s||t[15]!==d.type||t[16]!==d.valid?(N=b!==void 0&&h.jsx("button",{className:yt.uploadButton,onClick:b,disabled:!s||!d.valid||d.type!=="json",title:"Upload JSON payload to current session via REST",children:"Upload"}),t[13]=b,t[14]=s,t[15]=d.type,t[16]=d.valid,t[17]=N):N=t[17];let E;t[18]!==f||t[19]!==y||t[20]!==w||t[21]!==D||t[22]!==N?(E=h.jsxs("div",{className:yt.labelRow,children:[g,h.jsxs("div",{className:yt.payloadControls,children:[f,y,w,D,N]})]}),t[18]=f,t[19]=y,t[20]=w,t[21]=D,t[22]=N,t[23]=E):E=t[23];const _=`${yt.textarea} ${d.valid?"":yt.textareaError}`;let T;t[24]!==c?(T=V=>c(V.target.value),t[24]=c,t[25]=T):T=t[25];let C;t[26]!==s||t[27]!==_||t[28]!==T?(C=h.jsx("textarea",{id:"payload",className:_,placeholder:"Paste your JSON/XML payload here",value:s,onChange:T}),t[26]=s,t[27]=_,t[28]=T,t[29]=C):C=t[29];let j;t[30]!==d.error||t[31]!==d.valid?(j=!d.valid&&h.jsx("div",{className:yt.errorMessage,children:d.error}),t[30]=d.error,t[31]=d.valid,t[32]=j):j=t[32];let L;t[33]!==c?(L=h.jsx("div",{className:yt.sampleButtonsRow,children:h.jsx(sv,{onLoad:c})}),t[33]=c,t[34]=L):L=t[34];let U;return t[35]!==C||t[36]!==j||t[37]!==L||t[38]!==E?(U=h.jsxs("div",{className:yt.payloadRoot,children:[E,C,j,L]}),t[35]=C,t[36]=j,t[37]=L,t[38]=E,t[39]=U):U=t[39],U}const pv={Root:{icon:"🚀",label:"Root"},End:{icon:"🏁",label:"End"},Fetcher:{icon:"🌐",label:"Fetcher"},mapper:{icon:"🗺️",label:"Mapper"},Math:{icon:"🔢",label:"Math"},JavaScript:{icon:"📜",label:"JavaScript"},Provider:{icon:"🔌",label:"Provider"},Dictionary:{icon:"📖",label:"Dictionary"},Join:{icon:"🔀",label:"Join"},Extension:{icon:"🧩",label:"Extension"},Island:{icon:"🏝️",label:"Island"},Decision:{icon:"❓",label:"Decision"}},hv={boxSizing:"border-box",borderRadius:"8px",borderWidth:"1.5px",borderStyle:"solid",background:"var(--bg-secondary, #1e1e2e)",color:"var(--text-primary, #cdd6f4)",fontSize:"0.75rem",boxShadow:"0 2px 8px rgba(0,0,0,0.45)",overflow:"visible",padding:0},fv={Root:"#15803d",End:"#dc2626",Fetcher:"#2563eb",mapper:"#ea580c",Math:"#a16207",JavaScript:"#7e22ce",Provider:"#be185d",Dictionary:"#0e7490",Join:"#65a30d",Extension:"#4338ca",Island:"#475569",Decision:"#b45309"},mv="#6c7086";function gv(r){return pv[r]??{icon:"📦",label:r}}function qf(r){const t=fv[r]??mv;return{...hv,borderColor:t,"--node-accent":t}}const yv="_content_138ap_8",bv="_header_138ap_22",vv="_icon_138ap_42",_v="_alias_138ap_47",xv="_badge_138ap_53",Sv="_body_138ap_65",wv="_row_138ap_70",Ev="_label_138ap_83",Tv="_value_138ap_89",Nv="_edgeHandle_138ap_103",gn={content:yv,header:bv,icon:vv,alias:_v,badge:xv,body:Sv,row:wv,label:Ev,value:Tv,edgeHandle:Nv};function af(r){const t=Se.c(7),{label:s,value:c}=r;let d;t[0]!==s?(d=h.jsx("span",{className:gn.label,children:s}),t[0]=s,t[1]=d):d=t[1];let p;t[2]!==c?(p=h.jsx("span",{className:gn.value,title:c,children:c}),t[2]=c,t[3]=p):p=t[3];let b;return t[4]!==d||t[5]!==p?(b=h.jsxs("div",{className:gn.row,children:[d,p]}),t[4]=d,t[5]=p,t[6]=b):b=t[6],b}function Av(r){const t=Se.c(3),{properties:s}=r;let c,d;if(t[0]!==s){d=Symbol.for("react.early_return_sentinel");e:{const p=Object.entries(s).filter(jv);if(p.length===0){d=null;break e}c=h.jsx(h.Fragment,{children:p.map(Cv)})}t[0]=s,t[1]=c,t[2]=d}else c=t[1],d=t[2];return d!==Symbol.for("react.early_return_sentinel")?d:c}function Cv(r){const[t,s]=r;if(Array.isArray(s))return s.map((d,p)=>{const b=typeof d=="string"?d:JSON.stringify(d);return h.jsx(af,{label:p===0?t:"",value:b},`${t}-${p}`)});const c=typeof s=="string"?s:JSON.stringify(s);return h.jsx(af,{label:t,value:c},t)}function jv(r){const[,t]=r;return t!=null}function If(r){const t=Se.c(17),{alias:s,nodeType:c,properties:d}=r;let p;t[0]!==c?(p=gv(c),t[0]=c,t[1]=p):p=t[1];const b=p;let g;t[2]!==b.icon?(g=h.jsx("span",{className:gn.icon,children:b.icon}),t[2]=b.icon,t[3]=g):g=t[3];let f;t[4]!==s?(f=h.jsx("span",{className:gn.alias,children:s}),t[4]=s,t[5]=f):f=t[5];let y;t[6]!==b.label?(y=h.jsx("span",{className:gn.badge,children:b.label}),t[6]=b.label,t[7]=y):y=t[7];let w;t[8]!==g||t[9]!==f||t[10]!==y?(w=h.jsxs("div",{className:gn.header,children:[g,f,y]}),t[8]=g,t[9]=f,t[10]=y,t[11]=w):w=t[11];let v;t[12]!==d?(v=h.jsx("div",{className:gn.body,children:h.jsx(Av,{properties:d})}),t[12]=d,t[13]=v):v=t[13];let S;return t[14]!==w||t[15]!==v?(S=h.jsx(x.Fragment,{children:h.jsxs("div",{className:gn.content,children:[w,v]})}),t[14]=w,t[15]=v,t[16]=S):S=t[16],S}function fn(r){const t=Se.c(34),{data:s,isConnectable:c,selected:d}=r;let p;t[0]!==s.minHeight||t[1]!==d?(p=h.jsx(_y,{minWidth:180,minHeight:s.minHeight,isVisible:d}),t[0]=s.minHeight,t[1]=d,t[2]=p):p=t[2];let b;if(t[3]!==s.targetHandles||t[4]!==c){let S;t[6]!==c?(S=D=>{const{id:N,offset:E}=D;return h.jsx(es,{id:N,type:"target",position:ts.Left,isConnectable:c,className:gn.edgeHandle,style:{top:`calc(50% + ${E}px)`}},N)},t[6]=c,t[7]=S):S=t[7],b=s.targetHandles.map(S),t[3]=s.targetHandles,t[4]=c,t[5]=b}else b=t[5];let g;if(t[8]!==s.backSourceHandles||t[9]!==c){let S;t[11]!==c?(S=D=>{const{id:N,offset:E}=D;return h.jsx(es,{id:N,type:"source",position:ts.Left,isConnectable:c,className:gn.edgeHandle,style:{top:`calc(50% + ${E}px)`}},N)},t[11]=c,t[12]=S):S=t[12],g=s.backSourceHandles.map(S),t[8]=s.backSourceHandles,t[9]=c,t[10]=g}else g=t[10];let f;t[13]!==s.alias||t[14]!==s.nodeType||t[15]!==s.properties?(f=h.jsx(If,{alias:s.alias,nodeType:s.nodeType,properties:s.properties}),t[13]=s.alias,t[14]=s.nodeType,t[15]=s.properties,t[16]=f):f=t[16];let y;if(t[17]!==s.sourceHandles||t[18]!==c){let S;t[20]!==c?(S=D=>{const{id:N,offset:E}=D;return h.jsx(es,{id:N,type:"source",position:ts.Right,isConnectable:c,className:gn.edgeHandle,style:{top:`calc(50% + ${E}px)`}},N)},t[20]=c,t[21]=S):S=t[21],y=s.sourceHandles.map(S),t[17]=s.sourceHandles,t[18]=c,t[19]=y}else y=t[19];let w;if(t[22]!==s.backTargetHandles||t[23]!==c){let S;t[25]!==c?(S=D=>{const{id:N,offset:E}=D;return h.jsx(es,{id:N,type:"target",position:ts.Right,isConnectable:c,className:gn.edgeHandle,style:{top:`calc(50% + ${E}px)`}},N)},t[25]=c,t[26]=S):S=t[26],w=s.backTargetHandles.map(S),t[22]=s.backTargetHandles,t[23]=c,t[24]=w}else w=t[24];let v;return t[27]!==p||t[28]!==b||t[29]!==g||t[30]!==f||t[31]!==y||t[32]!==w?(v=h.jsxs(h.Fragment,{children:[p,b,g,f,y,w]}),t[27]=p,t[28]=b,t[29]=g,t[30]=f,t[31]=y,t[32]=w,t[33]=v):v=t[33],v}const Dv={Root:fn,End:fn,Fetcher:fn,mapper:fn,Math:fn,JavaScript:fn,Provider:fn,Dictionary:fn,Join:fn,Extension:fn,Island:fn,Decision:fn,default:fn},Mv="_graphWrapper_zglpq_15",kv="_graphSurface_zglpq_24",Ov="_empty_zglpq_30",Rv="_emptyIcon_zglpq_43",zv="_emptyCreateButton_zglpq_48",Bv="_emptyHint_zglpq_70",Gv="_refreshingOverlay_zglpq_104",Hv="_clipboardDropOverlay_zglpq_116",Uv="_clipboardDropMessage_zglpq_129",Lv="_refreshingSpinner_zglpq_144",on={graphWrapper:Mv,graphSurface:kv,empty:Ov,emptyIcon:Rv,emptyCreateButton:zv,emptyHint:Bv,refreshingOverlay:Gv,clipboardDropOverlay:Hv,clipboardDropMessage:Uv,refreshingSpinner:Lv};class Yv extends x.Component{constructor(){super(...arguments),this.state={caughtError:null}}static getDerivedStateFromError(t){return{caughtError:t instanceof Error?t.message:String(t)}}componentDidCatch(t,s){var d,p;const c=t instanceof Error?t.message:String(t);console.error("[GraphView] Render error:",c,s.componentStack),(p=(d=this.props).onRenderError)==null||p.call(d,`Graph render failed: ${c}`)}render(){return this.state.caughtError?h.jsxs("div",{className:on.empty,children:[h.jsx("span",{className:on.emptyIcon,children:"⚠️"}),h.jsx("span",{children:"Graph could not be rendered."}),h.jsx("span",{children:this.state.caughtError})]}):this.props.children}}const ls=240,Io=100,of=60,lf=120,qv=360,Iv=120,Xv=80,Lc="rgba(148, 163, 184, 0.42)",Jv="var(--bg-secondary)",is=24,Vv=32,sf=["#0369a1","#15803d","#b45309","#7e22ce","#b91c1c","#0f766e","#c2410c","#a16207"],Zv={fetch:"#0369a1",details:"#0369a1","ext-call":"#0369a1",mapping:"#b45309",compute:"#b45309",calculate:"#b45309",evaluate:"#b45309",fork:"#7e22ce",join:"#7e22ce",one:"#7e22ce",two:"#6d28d9",three:"#5b21b6",more:"#4c1d95",done:"#15803d",complete:"#15803d",finish:"#15803d",positive:"#15803d",negative:"#b91c1c"};function Qv(r){let t=0;for(let s=0;s<r.length;s++)t=(t<<5)-t+r.charCodeAt(s),t|=0;return Math.abs(t)}function Kv(r){if(r.length===0)return Lc;const t=r[0].trim().toLowerCase(),s=Zv[t];return s||sf[Qv(t)%sf.length]}function $v(r){return`source-${r}`}function Pv(r){return`target-${r}`}function Wv(r){return`back-source-${r}`}function Fv(r){return`back-target-${r}`}function rf(r,t){return t<=1?0:t===2?r===0?-is:is:(r-(t-1)/2)*is}function cf(r){return r<=1?Io:Math.max(Io,(r-1)*is+Vv*2)}const e2=new Set(["graph.math","graph.js"]),uf=["Dictionary","Provider","Module","Entity"],Nc={ROOT_TREE:0,DEFAULT_TREE:1,END_TREE:2};function Ac(r){return r.alias.toLowerCase()==="root"||r.types.includes("Root")||r.types.includes("entry_point")}function t2(r){return r.alias.toLowerCase()==="end"||r.types.includes("End")}function df(r){return r.hasRoot?Nc.ROOT_TREE:r.hasEnd?Nc.END_TREE:Nc.DEFAULT_TREE}function n2(r,t){const s=df(r)-df(t);return s!==0?s:r.sortKey.localeCompare(t.sortKey)}function a2(r,t){if(t.has(r.alias))return"flow";const c=r.types[0]??"",d=typeof r.properties.skill=="string"?r.properties.skill:void 0;return c==="Dictionary"?"Dictionary":c==="Provider"?"Provider":d&&e2.has(d)?"Module":d?"__unknown__":"Entity"}function o2(r,t,s){var M,X,P;const c=new Set;for(const z of t??[])c.add(z.source),c.add(z.target);const d=[],p=[],b=new Map;for(const z of r){const H=a2(z,c);b.set(z.alias,H),H==="flow"?d.push(z):p.push(z)}const g=new Set(d.map(z=>z.alias)),f=new Map(d.map(z=>[z.alias,z])),y=new Map,w=new Map,v=new Map;for(const z of d)y.set(z.alias,[]),w.set(z.alias,new Set),v.set(z.alias,0);for(const z of t??[])!g.has(z.source)||!g.has(z.target)||((M=y.get(z.source))==null||M.push(z.target),(X=w.get(z.source))==null||X.add(z.target),(P=w.get(z.target))==null||P.add(z.source),v.set(z.target,(v.get(z.target)??0)+1));const S=d.filter(z=>v.get(z.alias)===0||z.types.includes("entry_point")||Ac(z)).map(z=>z.alias),D=new Set;{let z=function(ne){if(Z.get(ne)!==H)return;Z.set(ne,G);const k=[{node:ne,childIdx:0}];for(;k.length>0;){const R=k[k.length-1],$=y.get(R.node)??[];if(R.childIdx>=$.length){Z.set(R.node,Q),k.pop();continue}const ee=$[R.childIdx++],ae=Z.get(ee);ae===G?D.add(`${R.node}	${ee}`):ae===H&&(Z.set(ee,G),k.push({node:ee,childIdx:0}))}};const H=0,G=1,Q=2,Z=new Map;for(const ne of d)Z.set(ne.alias,0);for(const ne of S)z(ne);for(const ne of d)z(ne.alias)}const N=[],E=new Set;for(const z of Array.from(g).sort()){if(E.has(z))continue;const H=[],G=[z];for(E.add(z);G.length>0;){const Z=G.pop();H.push(Z);for(const ne of w.get(Z)??[])E.has(ne)||(E.add(ne),G.push(ne))}H.sort();const Q=H.map(Z=>f.get(Z)).filter(Z=>!!Z);N.push({aliases:H,nodes:Q,hasRoot:Q.some(Ac),hasEnd:Q.some(t2),sortKey:H[0]??""})}N.sort(n2);const _=new Map,T=new Map;let C=0,j=0;for(const z of N){const H=new Set(z.aliases),G=z.nodes.filter(ee=>v.get(ee.alias)===0||ee.types.includes("entry_point")||Ac(ee)).map(ee=>ee.alias).sort();G.length===0&&z.aliases.length>0&&G.push(z.aliases[0]);const Q=new Map,Z=[...G];for(G.forEach(ee=>Q.set(ee,0));Z.length>0;){const ee=Z.shift(),ae=Q.get(ee)??0;for(const le of y.get(ee)??[])H.has(le)&&(D.has(`${ee}	${le}`)||(!Q.has(le)||Q.get(le)<=ae)&&(Q.set(le,ae+1),Z.push(le)))}const ne=Q.size>0?Math.max(...Q.values()):0;for(const ee of z.aliases)Q.has(ee)||Q.set(ee,ne+1);const k=new Map;for(const[ee,ae]of Q)k.has(ae)||k.set(ae,[]),k.get(ae).push(ee);let R=j;for(const[ee,ae]of[...k].sort(([le],[re])=>le-re)){const le=ae.slice().sort();let ie=-(le.reduce((se,xe)=>se+(s.get(xe)??Io),0)+Math.max(0,le.length-1)*of)/2;const de=C+ee,oe=j+ee*(ls+lf);R=Math.max(R,oe),le.forEach(se=>{const xe=s.get(se)??Io;_.set(se,de),T.set(se,{x:oe,y:ie}),ie+=xe+of})}const $=Q.size>0?Math.max(...Q.values()):0;C+=$+1,j=R+ls+qv}let L=0;for(const[z,H]of T)L=Math.max(L,H.y+(s.get(z)??Io));let U=L+(T.size>0?Iv:0);const V=new Map;for(const z of uf)V.set(z,[]);V.set("__unknown__",[]);for(const z of p){const H=b.get(z.alias);V.get(H).push(z.alias)}for(const z of[...uf,"__unknown__"]){const H=(V.get(z)??[]).slice().sort();if(H.length===0)continue;const G=0,Q=H.reduce((Z,ne)=>Math.max(Z,s.get(ne)??Io),0);H.forEach((Z,ne)=>{T.set(Z,{x:G+ne*(ls+lf),y:U})}),U+=Q+Xv}return{positions:T,levelOf:_}}function l2(r){const t=r.connections??[],s=new Map,c=new Map;for(const E of t)s.set(E.source,(s.get(E.source)??0)+1),c.set(E.target,(c.get(E.target)??0)+1);const d=new Map(r.nodes.map(E=>[E.alias,cf(Math.max(s.get(E.alias)??0,c.get(E.alias)??0))])),{positions:p,levelOf:b}=o2(r.nodes,t,d),g=new Set;for(const[E,_]of t.entries()){const T=b.get(_.source),C=b.get(_.target);T!==void 0&&C!==void 0&&T>=C&&g.add(E)}const f=new Map,y=new Map;for(const E of r.nodes)f.set(E.alias,[]),y.set(E.alias,[]);for(const[E,_]of t.entries())g.has(E)?(y.get(_.source).push({connIndex:E,peerAlias:_.target,isBack:!0}),f.get(_.target).push({connIndex:E,peerAlias:_.source,isBack:!0})):(f.get(_.source).push({connIndex:E,peerAlias:_.target,isBack:!1}),y.get(_.target).push({connIndex:E,peerAlias:_.source,isBack:!1}));const w=E=>{var _;return((_=p.get(E))==null?void 0:_.y)??0};for(const E of f.values())E.sort((_,T)=>w(_.peerAlias)-w(T.peerAlias));for(const E of y.values())E.sort((_,T)=>w(_.peerAlias)-w(T.peerAlias));const v=new Map,S=new Map,D=r.nodes.map(E=>{const _=f.get(E.alias)??[],T=y.get(E.alias)??[],C=cf(Math.max(_.length,T.length)),j=[],L=[];let U=0,V=0;for(let H=0;H<_.length;H++){const G=_[H],Q=rf(H,_.length);if(G.isBack){const Z=Fv(V++);L.push({id:Z,offset:Q}),S.set(G.connIndex,Z)}else{const Z=$v(U++);j.push({id:Z,offset:Q}),v.set(G.connIndex,Z)}}const M=[],X=[];let P=0,z=0;for(let H=0;H<T.length;H++){const G=T[H],Q=rf(H,T.length);if(G.isBack){const Z=Wv(z++);X.push({id:Z,offset:Q}),v.set(G.connIndex,Z)}else{const Z=Pv(P++);M.push({id:Z,offset:Q}),S.set(G.connIndex,Z)}}return{id:E.alias,type:E.types[0]??"default",position:p.get(E.alias)??{x:0,y:0},width:ls,height:C,style:qf(E.types[0]??"unknown"),data:{alias:E.alias,nodeType:E.types[0]??"unknown",properties:E.properties,sourceHandles:j,targetHandles:M,backSourceHandles:X,backTargetHandles:L,minHeight:C}}}),N=[];for(const[E,_]of t.entries()){const T=_.relations.map(L=>L.type),C=`${_.source}__${_.target}__${E}`,j=Kv(T);N.push({id:C,source:_.source,target:_.target,sourceHandle:v.get(E),targetHandle:S.get(E),label:T.join(", "),type:"bezier",markerEnd:{type:xy.ArrowClosed,width:16,height:16,color:Lc},style:{stroke:Lc,strokeWidth:2},labelStyle:{fill:j,fontSize:10,fontWeight:700},labelBgStyle:{fill:Jv,fillOpacity:.94,stroke:"rgba(15, 23, 42, 0.16)",strokeWidth:1},labelBgPadding:[5,2],labelBgBorderRadius:6,data:{relationTypes:T}})}return{nodes:D,edges:N}}const eu="application/x-minigraph-clipboard-item";function as(r){return r.includes(eu)}function i2(r,t){r.effectAllowed="copy",r.setData(eu,t)}function s2(r){const t=(r==null?void 0:r.getData(eu))??"";return t.trim()?t:null}function r2(r,t){return r.nodes.find(s=>s.alias===t)}function c2(r,t){return(r.connections??[]).filter(s=>s.source!==s.target&&(s.source===t||s.target===t))}const u2="_toolbar_117v8_2",d2="_nameGroup_117v8_13",p2="_graphName_117v8_20",h2="_stats_117v8_29",f2="_toolbarActions_117v8_49",m2="_toolbarButton_117v8_55",Uo={toolbar:u2,nameGroup:d2,graphName:p2,stats:h2,toolbarActions:f2,toolbarButton:m2};function Xf(r){const t=Se.c(24),{graphData:s,graphName:c,onCopySuccess:d,onCopyError:p,extraActions:b}=r;let g;t[0]!==s||t[1]!==p||t[2]!==d?(g=()=>{s&&navigator.clipboard.writeText(JSON.stringify(s,null,2)).then(()=>d==null?void 0:d()).catch(()=>p==null?void 0:p())},t[0]=s,t[1]=p,t[2]=d,t[3]=g):g=t[3];const f=g,y=(s==null?void 0:s.nodes.length)??0;let w;t[4]!==(s==null?void 0:s.connections)?(w=(s==null?void 0:s.connections)??[],t[4]=s==null?void 0:s.connections,t[5]=w):w=t[5];const v=w.length,S=c??"Untitled";let D;t[6]!==S?(D=h.jsx("span",{className:Uo.graphName,children:S}),t[6]=S,t[7]=D):D=t[7];const N=y!==1?"s":"",E=v!==1?"s":"";let _;t[8]!==v||t[9]!==y||t[10]!==N||t[11]!==E?(_=h.jsxs("span",{className:Uo.stats,children:[y," node",N," · ",v," connection",E]}),t[8]=v,t[9]=y,t[10]=N,t[11]=E,t[12]=_):_=t[12];let T;t[13]!==D||t[14]!==_?(T=h.jsxs("div",{className:Uo.nameGroup,children:[D,_]}),t[13]=D,t[14]=_,t[15]=T):T=t[15];let C;t[16]!==f?(C=h.jsx("button",{className:Uo.toolbarButton,onClick:f,title:"Copy raw graph JSON to clipboard","aria-label":"Copy raw graph JSON to clipboard",children:"📑"}),t[16]=f,t[17]=C):C=t[17];let j;t[18]!==b||t[19]!==C?(j=h.jsxs("div",{className:Uo.toolbarActions,children:[b,C]}),t[18]=b,t[19]=C,t[20]=j):j=t[20];let L;return t[21]!==j||t[22]!==T?(L=h.jsxs("div",{className:Uo.toolbar,children:[T,j]}),t[21]=j,t[22]=T,t[23]=L):L=t[23],L}const g2="_menu_13qxg_1",y2="_menuItem_13qxg_12",pf={menu:g2,menuItem:y2};function b2(r){const t=Se.c(17),{open:s,x:c,y:d,canCreateNode:p,onCreateNode:b,onClose:g}=r,f=x.useRef(null),y=x.useRef(null);let w,v;if(t[0]!==g||t[1]!==s?(w=()=>{var j;if(!s)return;(j=y.current)==null||j.focus();const T=L=>{f.current&&!f.current.contains(L.target)&&g()},C=L=>{L.key==="Escape"&&(L.preventDefault(),g())};return document.addEventListener("pointerdown",T),document.addEventListener("keydown",C),()=>{document.removeEventListener("pointerdown",T),document.removeEventListener("keydown",C)}},v=[s,g],t[0]=g,t[1]=s,t[2]=w,t[3]=v):(w=t[2],v=t[3]),x.useEffect(w,v),!s)return null;let S;t[4]!==c||t[5]!==d?(S={left:c,top:d},t[4]=c,t[5]=d,t[6]=S):S=t[6];const D=!p;let N;t[7]!==p||t[8]!==g||t[9]!==b?(N=()=>{p&&(b(),g())},t[7]=p,t[8]=g,t[9]=b,t[10]=N):N=t[10];let E;t[11]!==D||t[12]!==N?(E=h.jsx("button",{ref:y,role:"menuitem",type:"button",className:pf.menuItem,disabled:D,onClick:N,children:"Create Node"}),t[11]=D,t[12]=N,t[13]=E):E=t[13];let _;return t[14]!==S||t[15]!==E?(_=h.jsx("div",{ref:f,className:pf.menu,style:S,role:"menu","aria-label":"Graph actions",children:E}),t[14]=S,t[15]=E,t[16]=_):_=t[16],_}const v2="_menu_1trgd_1",_2="_menuItem_1trgd_12",x2="_dangerItem_1trgd_38",S2="_confirmation_1trgd_51",w2="_confirmationText_1trgd_57",E2="_confirmationActions_1trgd_65",On={menu:v2,menuItem:_2,dangerItem:x2,confirmation:S2,confirmationText:w2,confirmationActions:E2},Lo=8;function T2(r){const t=Se.c(48),{open:s,x:c,y:d,nodeAlias:p,canClipNode:b,canEditNode:g,canDeleteNode:f,onClipNode:y,onEditNode:w,onDeleteNode:v,onClose:S}=r,[D,N]=x.useState(!1);let E;t[0]!==c||t[1]!==d?(E={left:c,top:d},t[0]=c,t[1]=d,t[2]=E):E=t[2];const[_,T]=x.useState(E),C=x.useRef(null),j=x.useRef(null),L=x.useRef(null),U=b||g||f;let V;t[3]!==s?(V=()=>{s&&N(!1)},t[3]=s,t[4]=V):V=t[4];let M;t[5]!==p||t[6]!==s||t[7]!==c||t[8]!==d?(M=[p,s,c,d],t[5]=p,t[6]=s,t[7]=c,t[8]=d,t[9]=M):M=t[9],x.useLayoutEffect(V,M);let X;t[10]!==s||t[11]!==c||t[12]!==d?(X=()=>{if(!s)return;const $=C.current;if(!$){T({left:c,top:d});return}const ee=$.getBoundingClientRect(),ae=Math.max(Lo,window.innerWidth-ee.width-Lo),le=Math.max(Lo,window.innerHeight-ee.height-Lo);T({left:Math.min(Math.max(c,Lo),ae),top:Math.min(Math.max(d,Lo),le)})},t[10]=s,t[11]=c,t[12]=d,t[13]=X):X=t[13];let P;t[14]!==b||t[15]!==f||t[16]!==g||t[17]!==D||t[18]!==p||t[19]!==s||t[20]!==c||t[21]!==d?(P=[b,f,g,D,p,s,c,d],t[14]=b,t[15]=f,t[16]=g,t[17]=D,t[18]=p,t[19]=s,t[20]=c,t[21]=d,t[22]=P):P=t[22],x.useLayoutEffect(X,P);let z,H;t[23]!==D||t[24]!==s?(z=()=>{var $,ee;if(!s){N(!1);return}D?($=L.current)==null||$.focus():(ee=j.current)==null||ee.focus()},H=[D,s],t[23]=D,t[24]=s,t[25]=z,t[26]=H):(z=t[25],H=t[26]),x.useEffect(z,H);let G,Q;if(t[27]!==S||t[28]!==s?(G=()=>{if(!s)return;const $=le=>{C.current&&!C.current.contains(le.target)&&S()},ee=le=>{le.key==="Escape"&&(le.preventDefault(),S())},ae=()=>S();return document.addEventListener("pointerdown",$),document.addEventListener("keydown",ee),window.addEventListener("scroll",ae,!0),window.addEventListener("resize",ae),()=>{document.removeEventListener("pointerdown",$),document.removeEventListener("keydown",ee),window.removeEventListener("scroll",ae,!0),window.removeEventListener("resize",ae)}},Q=[S,s],t[27]=S,t[28]=s,t[29]=G,t[30]=Q):(G=t[29],Q=t[30]),x.useEffect(G,Q),!s||!U)return null;let Z;t[31]!==_.left||t[32]!==_.top?(Z={left:_.left,top:_.top},t[31]=_.left,t[32]=_.top,t[33]=Z):Z=t[33];const ne=`Node actions for ${p}`;let k;t[34]!==b||t[35]!==f||t[36]!==g||t[37]!==D||t[38]!==p||t[39]!==y||t[40]!==S||t[41]!==v||t[42]!==w?(k=D?h.jsxs("div",{className:On.confirmation,role:"group","aria-label":`Confirm delete ${p}`,children:[h.jsxs("div",{className:On.confirmationText,children:['Delete "',p,'"?']}),h.jsxs("div",{className:On.confirmationActions,children:[h.jsx("button",{ref:L,type:"button",className:`${On.menuItem} ${On.dangerItem}`,onClick:()=>{v(),S()},children:"Delete"}),h.jsx("button",{type:"button",className:On.menuItem,onClick:()=>N(!1),children:"Cancel"})]})]}):h.jsxs(h.Fragment,{children:[b&&h.jsx("button",{ref:j,role:"menuitem",type:"button",className:On.menuItem,onClick:()=>{y(),S()},children:"Clip to Workspace"}),g&&h.jsx("button",{ref:b?void 0:j,role:"menuitem",type:"button",className:On.menuItem,onClick:()=>{w(),S()},children:"Edit Node"}),f&&h.jsx("button",{ref:!b&&!g?j:void 0,role:"menuitem",type:"button",className:`${On.menuItem} ${On.dangerItem}`,onClick:()=>N(!0),children:"Delete Node"})]}),t[34]=b,t[35]=f,t[36]=g,t[37]=D,t[38]=p,t[39]=y,t[40]=S,t[41]=v,t[42]=w,t[43]=k):k=t[43];let R;return t[44]!==Z||t[45]!==ne||t[46]!==k?(R=h.jsx("div",{ref:C,className:On.menu,style:Z,role:"menu","aria-label":ne,children:k}),t[44]=Z,t[45]=ne,t[46]=k,t[47]=R):R=t[47],R}const hf=[],ff=[];function N2(r){const t=Se.c(110),{graphData:s,graphName:c,onCopySuccess:d,onCopyError:p,onRenderError:b,isRefreshing:g,onClipNode:f,onClipboardDrop:y,isConnected:w,supportsAuthoring:v,onCreateNode:S,onEditNode:D,onDeleteNode:N}=r,E=g===void 0?!1:g,_=v===void 0?!1:v,[T,C]=x.useState(null),[j,L]=x.useState(null),[U,V]=x.useState(!1),M=x.useRef(0),X=!!(_&&S&&w),P=!!f,z=!!(_&&D&&w),H=!!(_&&N&&w),G=P||z||H,Q=!!(y&&w);let Z;t[0]===Symbol.for("react.memo_cache_sentinel")?(Z=()=>{M.current=0,V(!1)},t[0]=Z):Z=t[0];const ne=Z;let k,R;t[1]!==j?(k=()=>{if(!j)return;const he=Xt=>{Xt.key==="Escape"&&L(null)},lt=()=>L(null);return document.addEventListener("keydown",he),window.addEventListener("scroll",lt,!0),window.addEventListener("resize",lt),()=>{document.removeEventListener("keydown",he),window.removeEventListener("scroll",lt,!0),window.removeEventListener("resize",lt)}},R=[j],t[1]=j,t[2]=k,t[3]=R):(k=t[2],R=t[3]),x.useEffect(k,R);let $,ee;t[4]===Symbol.for("react.memo_cache_sentinel")?($=()=>{const he=()=>ne();return window.addEventListener("dragend",he),window.addEventListener("drop",he),()=>{window.removeEventListener("dragend",he),window.removeEventListener("drop",he),ne()}},ee=[ne],t[4]=$,t[5]=ee):($=t[4],ee=t[5]),x.useEffect($,ee);const ae=x.useRef(b);let le,re;t[6]!==b?(le=()=>{ae.current=b},re=[b],t[6]=b,t[7]=le,t[8]=re):(le=t[7],re=t[8]),x.useEffect(le,re);let ie;e:{if(!s){let he;t[9]===Symbol.for("react.memo_cache_sentinel")?(he={nodes:hf,edges:ff,transformError:null},t[9]=he):he=t[9],ie=he;break e}try{let he;t[10]!==s?(he=l2(s),t[10]=s,t[11]=he):he=t[11];const lt=he;let Xt;t[12]!==lt?(Xt={...lt,transformError:null},t[12]=lt,t[13]=Xt):Xt=t[13],ie=Xt}catch(he){const lt=he,Xt=lt instanceof Error?lt.message:String(lt);let ln;t[14]===Symbol.for("react.memo_cache_sentinel")?(ln={nodes:hf,edges:ff,transformError:Xt},t[14]=ln):ln=t[14],ie=ln}}const{nodes:de,edges:oe,transformError:se}=ie;let xe,ze;t[15]!==se?(xe=()=>{var he;se&&((he=ae.current)==null||he.call(ae,`Graph render failed: ${se}`))},ze=[se],t[15]=se,t[16]=xe,t[17]=ze):(xe=t[16],ze=t[17]),x.useEffect(xe,ze);let _e;t[18]!==s?(_e=s?JSON.stringify(s.nodes.map(C2)):"empty",t[18]=s,t[19]=_e):_e=t[19];const Je=_e,[je,ke,Me]=Sy(de),[te,pe,we]=wy(oe);let ye,Ee;t[20]!==oe||t[21]!==de||t[22]!==pe||t[23]!==ke?(ye=()=>{ke(de),pe(oe)},Ee=[de,oe,ke,pe],t[20]=oe,t[21]=de,t[22]=pe,t[23]=ke,t[24]=ye,t[25]=Ee):(ye=t[24],Ee=t[25]),x.useEffect(ye,Ee);let Ae;t[26]!==Q?(Ae=he=>{Q&&as(Array.from(he.dataTransfer.types))&&(he.preventDefault(),M.current=M.current+1,V(!0))},t[26]=Q,t[27]=Ae):Ae=t[27];const Ue=Ae;let $e;t[28]!==Q?($e=he=>{Q&&as(Array.from(he.dataTransfer.types))&&(he.preventDefault(),he.dataTransfer.dropEffect="copy",V(!0))},t[28]=Q,t[29]=$e):$e=t[29];const Le=$e;let ht;t[30]===Symbol.for("react.memo_cache_sentinel")?(ht=he=>{as(Array.from(he.dataTransfer.types))&&(M.current=Math.max(0,M.current-1),M.current===0&&V(!1))},t[30]=ht):ht=t[30];const ve=ht;let yn;t[31]!==Q||t[32]!==y?(yn=he=>{if(!Q||!as(Array.from(he.dataTransfer.types)))return;he.preventDefault();const lt=s2(he.dataTransfer);ne(),lt&&(y==null||y(lt))},t[31]=Q,t[32]=y,t[33]=yn):yn=t[33];const bn=yn,Et=!!(s&&s.nodes.length>0);let Tt;t[34]!==T||t[35]!==s?(Tt=T&&s?r2(s,T.nodeAlias):null,t[34]=T,t[35]=s,t[36]=Tt):Tt=t[36];const Pe=Tt;if(se){let he,lt;t[37]===Symbol.for("react.memo_cache_sentinel")?(he=h.jsx("span",{className:on.emptyIcon,children:"⚠️"}),lt=h.jsx("span",{children:"Graph could not be rendered."}),t[37]=he,t[38]=lt):(he=t[37],lt=t[38]);let Xt;return t[39]!==se?(Xt=h.jsxs("div",{className:on.empty,children:[he,lt,h.jsx("span",{children:se})]}),t[39]=se,t[40]=Xt):Xt=t[40],Xt}let Nt;t[41]!==s||t[42]!==c||t[43]!==Et||t[44]!==p||t[45]!==d?(Nt=Et&&s&&h.jsx(Xf,{graphData:s,graphName:c,onCopySuccess:d,onCopyError:p}),t[41]=s,t[42]=c,t[43]=Et,t[44]=p,t[45]=d,t[46]=Nt):Nt=t[46];let It;t[47]!==X||t[48]!==G||t[49]!==te||t[50]!==Et||t[51]!==w||t[52]!==je||t[53]!==S||t[54]!==we||t[55]!==Me||t[56]!==_?(It=Et?h.jsxs(Ey,{nodes:je,edges:te,onNodesChange:Me,onEdgesChange:we,nodeTypes:Dv,fitView:!0,fitViewOptions:{padding:.25},minZoom:.2,maxZoom:2.5,proOptions:{hideAttribution:!1},onNodeContextMenu:(he,lt)=>{he.preventDefault(),he.stopPropagation(),L(null),G&&C({x:he.clientX,y:he.clientY,nodeAlias:lt.data.alias})},onPaneContextMenu:he=>{he.preventDefault(),X&&(C(null),L({x:he.clientX,y:he.clientY}))},onPaneClick:()=>{C(null),L(null)},children:[h.jsx(Ty,{variant:Ny.Dots,gap:18,size:1,color:"rgba(255,255,255,0.07)"}),h.jsx(Ay,{showInteractive:!1}),h.jsx(Cy,{nodeColor:A2,maskColor:"rgba(0,0,0,0.3)",style:{background:"#fff"}})]}):h.jsxs("div",{className:on.empty,children:[h.jsx("span",{className:on.emptyIcon,children:"🕸️"}),h.jsx("span",{children:"No graph data yet."}),h.jsxs("span",{children:["Run ",h.jsx("strong",{children:"describe graph"})," or ",h.jsx("strong",{children:"export graph"})," in the playground."]}),_&&S&&h.jsxs(h.Fragment,{children:[h.jsx("button",{type:"button",className:on.emptyCreateButton,disabled:!w,onClick:()=>S("empty-graph"),children:"Create Node"}),!w&&h.jsx("span",{className:on.emptyHint,children:"Connect WebSocket to create a node."})]})]}),t[47]=X,t[48]=G,t[49]=te,t[50]=Et,t[51]=w,t[52]=je,t[53]=S,t[54]=we,t[55]=Me,t[56]=_,t[57]=It):It=t[57];let Vt;t[58]!==E?(Vt=E&&h.jsx("div",{className:on.refreshingOverlay,children:h.jsx("div",{className:on.refreshingSpinner,role:"status","aria-label":"Graph refreshing"})}),t[58]=E,t[59]=Vt):Vt=t[59];let ft;t[60]!==U?(ft=U&&h.jsx("div",{className:on.clipboardDropOverlay,children:h.jsx("div",{className:on.clipboardDropMessage,children:"Drop to paste workspace node"})}),t[60]=U,t[61]=ft):ft=t[61];const Ye=j!==null,At=(j==null?void 0:j.x)??0,ct=(j==null?void 0:j.y)??0;let Rt;t[62]!==S?(Rt=()=>S==null?void 0:S("pane-context-menu"),t[62]=S,t[63]=Rt):Rt=t[63];let Zt;t[64]===Symbol.for("react.memo_cache_sentinel")?(Zt=()=>L(null),t[64]=Zt):Zt=t[64];let zt;t[65]!==X||t[66]!==Ye||t[67]!==At||t[68]!==ct||t[69]!==Rt?(zt=h.jsx(b2,{open:Ye,x:At,y:ct,canCreateNode:X,onCreateNode:Rt,onClose:Zt}),t[65]=X,t[66]=Ye,t[67]=At,t[68]=ct,t[69]=Rt,t[70]=zt):zt=t[70];const Bt=T!==null&&Pe!==null&&G,Kt=(T==null?void 0:T.x)??0,Gt=(T==null?void 0:T.y)??0,vt=(T==null?void 0:T.nodeAlias)??"",me=P&&Pe!==null,De=z&&Pe!==null,Ve=H&&Pe!==null;let ge;t[71]!==Pe||t[72]!==s||t[73]!==f?(ge=()=>{if(!Pe||!s)return;const he=c2(s,Pe.alias);f==null||f(Pe,he)},t[71]=Pe,t[72]=s,t[73]=f,t[74]=ge):ge=t[74];let ot;t[75]!==Pe||t[76]!==D?(ot=()=>{Pe&&(D==null||D(Pe))},t[75]=Pe,t[76]=D,t[77]=ot):ot=t[77];let at;t[78]!==Pe||t[79]!==N?(at=()=>{Pe&&(N==null||N(Pe))},t[78]=Pe,t[79]=N,t[80]=at):at=t[80];let jn;t[81]===Symbol.for("react.memo_cache_sentinel")?(jn=()=>C(null),t[81]=jn):jn=t[81];let jt;t[82]!==Bt||t[83]!==Kt||t[84]!==Gt||t[85]!==vt||t[86]!==me||t[87]!==De||t[88]!==Ve||t[89]!==ge||t[90]!==ot||t[91]!==at?(jt=h.jsx(T2,{open:Bt,x:Kt,y:Gt,nodeAlias:vt,canClipNode:me,canEditNode:De,canDeleteNode:Ve,onClipNode:ge,onEditNode:ot,onDeleteNode:at,onClose:jn}),t[82]=Bt,t[83]=Kt,t[84]=Gt,t[85]=vt,t[86]=me,t[87]=De,t[88]=Ve,t[89]=ge,t[90]=ot,t[91]=at,t[92]=jt):jt=t[92];let Ln;t[93]!==Ue||t[94]!==Le||t[95]!==bn||t[96]!==It||t[97]!==Vt||t[98]!==ft||t[99]!==zt||t[100]!==jt?(Ln=h.jsxs("div",{className:on.graphSurface,onDragEnter:Ue,onDragOver:Le,onDragLeave:ve,onDrop:bn,children:[It,Vt,ft,zt,jt]}),t[93]=Ue,t[94]=Le,t[95]=bn,t[96]=It,t[97]=Vt,t[98]=ft,t[99]=zt,t[100]=jt,t[101]=Ln):Ln=t[101];let Yn;t[102]!==E||t[103]!==Nt||t[104]!==Ln?(Yn=h.jsxs("div",{className:on.graphWrapper,"aria-busy":E,children:[Nt,Ln]}),t[102]=E,t[103]=Nt,t[104]=Ln,t[105]=Yn):Yn=t[105];let ra;return t[106]!==Je||t[107]!==b||t[108]!==Yn?(ra=h.jsx(Yv,{onRenderError:b,children:Yn},Je),t[106]=Je,t[107]=b,t[108]=Yn,t[109]=ra):ra=t[109],ra}function A2(r){return{Root:"#15803d",End:"#dc2626",Fetcher:"#2563eb",mapper:"#ea580c",Math:"#a16207",JavaScript:"#7e22ce",Provider:"#be185d",Dictionary:"#0e7490",Join:"#65a30d",Extension:"#4338ca",Island:"#475569",Decision:"#b45309"}[r.type??""]??"#6c7086"}function C2(r){return r.alias}const j2="_root_1yhjs_2",D2="_empty_1yhjs_10",M2="_emptyIcon_1yhjs_23",k2="_toolbarButton_1yhjs_29 _toolbarButton_117v8_55",O2="_scrollBody_1yhjs_34",R2="_jsonContainer_1yhjs_45",z2="_jsonLabel_1yhjs_46",B2="_jsonString_1yhjs_47",G2="_jsonNumber_1yhjs_48",H2="_jsonBoolean_1yhjs_49",U2="_jsonNull_1yhjs_50",mn={root:j2,empty:D2,emptyIcon:M2,toolbarButton:k2,scrollBody:O2,jsonContainer:R2,jsonLabel:z2,jsonString:B2,jsonNumber:G2,jsonBoolean:H2,jsonNull:U2},L2=Dy,Y2=jy,q2=r=>r<3,I2={default:q2,all:L2,none:Y2};function X2(r){const t=Se.c(23),{graphData:s,graphName:c,onCopySuccess:d,onCopyError:p}=r,[b,g]=x.useState("all");if(!s){let U;return t[0]===Symbol.for("react.memo_cache_sentinel")?(U=h.jsx("div",{className:mn.root,children:h.jsxs("div",{className:mn.empty,children:[h.jsx("span",{className:mn.emptyIcon,children:"🕸️"}),h.jsx("span",{children:"No graph data yet."}),h.jsx("span",{children:"Pin a graph-link message in the Console to load the raw data here."})]})}),t[0]=U):U=t[0],U}let f;t[1]===Symbol.for("react.memo_cache_sentinel")?(f=()=>g("all"),t[1]=f):f=t[1];const y=b==="all";let w;t[2]!==y?(w=h.jsx("button",{className:mn.toolbarButton,onClick:f,title:"Expand all nodes","aria-label":"Expand all JSON nodes","aria-pressed":y,children:"➖"}),t[2]=y,t[3]=w):w=t[3];let v;t[4]===Symbol.for("react.memo_cache_sentinel")?(v=()=>g("none"),t[4]=v):v=t[4];const S=b==="none";let D;t[5]!==S?(D=h.jsx("button",{className:mn.toolbarButton,onClick:v,title:"Collapse all nodes","aria-label":"Collapse all JSON nodes","aria-pressed":S,children:"➕"}),t[5]=S,t[6]=D):D=t[6];let N;t[7]!==w||t[8]!==D?(N=h.jsxs(h.Fragment,{children:[w,D]}),t[7]=w,t[8]=D,t[9]=N):N=t[9];let E;t[10]!==s||t[11]!==c||t[12]!==p||t[13]!==d||t[14]!==N?(E=h.jsx(Xf,{graphData:s,graphName:c,onCopySuccess:d,onCopyError:p,extraActions:N}),t[10]=s,t[11]=c,t[12]=p,t[13]=d,t[14]=N,t[15]=E):E=t[15];const _=s,T=I2[b];let C;t[16]===Symbol.for("react.memo_cache_sentinel")?(C={...Ul,container:`${Ul.container} ${mn.jsonContainer}`,label:mn.jsonLabel,stringValue:mn.jsonString,numberValue:mn.jsonNumber,booleanValue:mn.jsonBoolean,nullValue:mn.jsonNull},t[16]=C):C=t[16];let j;t[17]!==T||t[18]!==_?(j=h.jsx("div",{className:mn.scrollBody,children:h.jsx($c,{data:_,shouldExpandNode:T,style:C})}),t[17]=T,t[18]=_,t[19]=j):j=t[19];let L;return t[20]!==j||t[21]!==E?(L=h.jsxs("div",{className:mn.root,children:[E,j]}),t[20]=j,t[21]=E,t[22]=L):L=t[22],L}const J2="_rightPanel_1xiht_2",V2="_tabStrip_1xiht_10",Z2="_tab_1xiht_10",Q2="_tabActive_1xiht_38",K2="_tabBadge_1xiht_42",$2="_tabBody_1xiht_48",P2="_tabBodyHidden_1xiht_57",W2="_graphContent_1xiht_61",F2="_rightPanelGroup_1xiht_68",e_="_verticalResizeHandle_1xiht_76",kt={rightPanel:J2,tabStrip:V2,tab:Z2,tabActive:Q2,tabBadge:K2,tabBody:$2,tabBodyHidden:P2,graphContent:W2,rightPanelGroup:F2,verticalResizeHandle:e_},mf="help-split-percent",Cc="help-split-maximized",t_=45,n_=98;function a_({tabs:r,payload:t,onChange:s,validation:c,onFormat:d,onUpload:p,graphData:b,graphName:g,activeTab:f,onTabChange:y,onGraphRenderError:w,onGraphDataCopySuccess:v,onGraphDataCopyError:S,isGraphRefreshing:D,onClipNode:N,onClipboardDrop:E,isConnected:_,supportsAuthoring:T,onCreateNode:C,onEditNode:j,onDeleteNode:L,helpPanel:U}){const V=x.useId(),M=`${V}-tab-payload`,X=`${V}-tab-graph`,P=`${V}-tab-graph-data`,z=h.jsxs("div",{className:kt.rightPanel,children:[h.jsxs("div",{className:kt.tabStrip,role:"tablist","aria-label":"Right panel tabs",children:[r.includes("payload")&&h.jsx("button",{role:"tab","aria-selected":f==="payload","aria-controls":M,className:`${kt.tab}${f==="payload"?` ${kt.tabActive}`:""}`,onClick:()=>y("payload"),children:"Payload Editor"}),r.includes("graph")&&h.jsxs("button",{role:"tab","aria-selected":f==="graph","aria-controls":X,className:`${kt.tab}${f==="graph"?` ${kt.tabActive}`:""}`,onClick:()=>y("graph"),children:["Graph",b!==null&&h.jsx("span",{className:kt.tabBadge,"aria-label":"Graph data available",children:"🕸️"})]}),r.includes("graph-data")&&h.jsx("button",{role:"tab","aria-selected":f==="graph-data","aria-controls":P,className:`${kt.tab}${f==="graph-data"?` ${kt.tabActive}`:""}`,onClick:()=>y("graph-data"),children:"Graph Data (Raw)"})]}),r.includes("payload")&&h.jsx("div",{role:"tabpanel",id:M,tabIndex:f==="payload"?0:-1,className:`${kt.tabBody}${f!=="payload"?` ${kt.tabBodyHidden}`:""}`,children:h.jsx(dv,{payload:t,onChange:s,validation:c,onFormat:d,onUpload:p})}),r.includes("graph")&&h.jsx("div",{role:"tabpanel",id:X,tabIndex:f==="graph"?0:-1,className:`${kt.tabBody}${f!=="graph"?` ${kt.tabBodyHidden}`:""}`,children:h.jsx("div",{className:kt.graphContent,children:h.jsx(N2,{graphData:b,graphName:g,onRenderError:w,isRefreshing:D,onCopySuccess:v,onCopyError:S,onClipNode:N,onClipboardDrop:E,isConnected:_,supportsAuthoring:T,onCreateNode:C,onEditNode:j,onDeleteNode:L})})}),r.includes("graph-data")&&h.jsx("div",{role:"tabpanel",id:P,tabIndex:f==="graph-data"?0:-1,className:`${kt.tabBody}${f!=="graph-data"?` ${kt.tabBodyHidden}`:""}`,children:h.jsx(X2,{graphData:b,graphName:g,onCopySuccess:v,onCopyError:S})})]}),H=x.useRef(Number(sessionStorage.getItem(mf))||t_),G=x.useRef(null),Q=x.useRef(null),[Z,ne]=x.useState(()=>sessionStorage.getItem(Cc)==="1"),k=x.useRef(Z),R=x.useCallback(de=>{const oe=de["help-split-help"];if(oe===void 0)return;const se=oe>=n_;se!==k.current&&(k.current=se,ne(se),sessionStorage.setItem(Cc,se?"1":"0")),se||(H.current=oe,sessionStorage.setItem(mf,String(oe)))},[]),$=x.useCallback(()=>{var oe,se,xe,ze;const de=!k.current;if(k.current=de,ne(de),sessionStorage.setItem(Cc,de?"1":"0"),de)(oe=Q.current)==null||oe.resize("0%"),(se=G.current)==null||se.resize("100%");else{const _e=H.current;(xe=G.current)==null||xe.resize(`${_e}%`),(ze=Q.current)==null||ze.resize(`${100-_e}%`)}},[]),ee=!!U;if(x.useEffect(()=>{ee&&k.current&&requestAnimationFrame(()=>{var de,oe;(de=Q.current)==null||de.resize("0%"),(oe=G.current)==null||oe.resize("100%")})},[ee]),!U)return z;const ae=typeof U=="function"?U($,Z):U,re=k.current?100:H.current,ie=100-re;return h.jsxs(Mf,{orientation:"vertical",className:kt.rightPanelGroup,onLayoutChanged:R,children:[h.jsx(Hl,{panelRef:Q,defaultSize:`${ie}%`,minSize:"0%",children:z}),h.jsx(zc,{className:kt.verticalResizeHandle,"aria-label":"Resize help panel"}),h.jsx(Hl,{id:"help-split-help",panelRef:G,defaultSize:`${re}%`,minSize:"15%",children:ae})]})}class o_ extends Of.Component{constructor(){super(...arguments),this.state={hasError:!1}}static getDerivedStateFromError(){return{hasError:!0}}componentDidCatch(t,s){console.error("[ConsoleErrorBoundary] Failed to render message:",t,s.componentStack)}render(){return this.state.hasError?h.jsx("span",{children:this.props.fallback}):this.props.children}}const l_=2e3,i_=(r={})=>{const{onSuccess:t,onError:s}=r,[c,d]=x.useState(!1),p=x.useRef(null);return x.useEffect(()=>()=>{p.current!==null&&clearTimeout(p.current)},[]),{copy:x.useCallback(async g=>{if(!navigator.clipboard)return console.warn("useCopyToClipboard: Clipboard API not available in this browser."),s==null||s(),!1;try{return await navigator.clipboard.writeText(g),d(!0),p.current!==null&&clearTimeout(p.current),p.current=setTimeout(()=>{p.current=null,d(!1)},l_),t==null||t(),!0}catch(f){return console.error("useCopyToClipboard: Failed to write to clipboard.",f),s==null||s(),!1}},[t,s]),copied:c}},s_="_consoleRoot_1lgp1_2",r_="_consoleHeader_1lgp1_10",c_="_consoleTitle_1lgp1_20",u_="_consoleControls_1lgp1_25",d_="_controlButton_1lgp1_30",p_="_console_1lgp1_2",h_="_emptyConsole_1lgp1_67",f_="_consoleMessage_1lgp1_80",m_="_consoleMessageActivatable_1lgp1_94",g_="_consoleMessageGraphLink_1lgp1_104",y_="_consoleMessageLargePayload_1lgp1_115",b_="_consoleMessageMockUpload_1lgp1_122",v_="_uploadMockButton_1lgp1_131",__="_copyButton_1lgp1_172",x_="_copyButtonCopied_1lgp1_225",S_="_sendToJsonPathButton_1lgp1_234",w_="_messageIcon_1lgp1_268",E_="_messageContent_1lgp1_272",T_="_messageText_1lgp1_278",N_="_messageTime_1lgp1_283",A_="_jsonViewWrapper_1lgp1_295",C_="_jsonContainer_1lgp1_301",j_="_jsonLabel_1lgp1_302",D_="_jsonString_1lgp1_303",M_="_jsonNumber_1lgp1_304",k_="_jsonBoolean_1lgp1_305",O_="_jsonNull_1lgp1_306",Ke={consoleRoot:s_,consoleHeader:r_,consoleTitle:c_,consoleControls:u_,controlButton:d_,console:p_,emptyConsole:h_,consoleMessage:f_,consoleMessageActivatable:m_,consoleMessageGraphLink:g_,consoleMessageLargePayload:y_,consoleMessageMockUpload:b_,uploadMockButton:v_,copyButton:__,copyButtonCopied:x_,sendToJsonPathButton:S_,messageIcon:w_,messageContent:E_,messageText:T_,messageTime:N_,"messageType-error":"_messageType-error_1lgp1_290","messageType-info":"_messageType-info_1lgp1_291","messageType-welcome":"_messageType-welcome_1lgp1_292",jsonViewWrapper:A_,jsonContainer:C_,jsonLabel:j_,jsonString:D_,jsonNumber:M_,jsonBoolean:k_,jsonNull:O_};function R_(r){var ht;const t=Se.c(77),{message:s,msgId:c,classificationMap:d,onGraphLink:p,onCopyMessage:b,onSendToJsonPath:g,onUploadMockData:f,successfulUploadPaths:y}=r;let w,v,S;t[0]!==s?(v=c1(s),w=u1(v.type),S=Ll(v.message),t[0]=s,t[1]=w,t[2]=v,t[3]=S):(w=t[1],v=t[2],S=t[3]);const D=S;let N,E,_,T,C,j;if(t[4]!==d||t[5]!==c||t[6]!==f||t[7]!==y){const ve=(c!==void 0?d==null?void 0:d.get(c):void 0)??[];E=ve.some(U_),_=ve.some(H_),T=ve.some(G_),C=((ht=ve.find(B_))==null?void 0:ht.uploadPath)??null,N=!!f&&T&&C!==null,j=N&&!!(y!=null&&y.has(C)),t[4]=d,t[5]=c,t[6]=f,t[7]=y,t[8]=N,t[9]=E,t[10]=_,t[11]=T,t[12]=C,t[13]=j}else N=t[8],E=t[9],_=t[10],T=t[11],C=t[12],j=t[13];const L=j,U=!!p&&E&&!T&&!_,V=!!g&&D.isJSON;let M;t[14]!==b?(M={onSuccess:b},t[14]=b,t[15]=M):M=t[15];const{copy:X,copied:P}=i_(M);let z;t[16]!==X||t[17]!==s?(z=ve=>{ve.stopPropagation(),X(s)},t[16]=X,t[17]=s,t[18]=z):z=t[18];const H=z;let G;t[19]!==X||t[20]!==s?(G=ve=>{(ve.key==="Enter"||ve.key===" ")&&(ve.preventDefault(),ve.stopPropagation(),X(s))},t[19]=X,t[20]=s,t[21]=G):G=t[21];const Q=G;let Z;t[22]!==D.data||t[23]!==D.isJSON||t[24]!==g?(Z=ve=>{if(ve.stopPropagation(),!g||!D.isJSON)return;const yn=JSON.stringify(D.data,null,2);g(yn)},t[22]=D.data,t[23]=D.isJSON,t[24]=g,t[25]=Z):Z=t[25];const ne=Z;let k;t[26]!==C||t[27]!==f?(k=ve=>{ve.stopPropagation(),!(!f||!C)&&f(C)},t[26]=C,t[27]=f,t[28]=k):k=t[28];const R=k,$=Ke[`messageType-${v.type}`],ee=U?Ke.consoleMessageActivatable:"",ae=E?Ke.consoleMessageGraphLink:"",le=_?Ke.consoleMessageLargePayload:"",re=T?Ke.consoleMessageMockUpload:"";let ie;t[29]!==ae||t[30]!==le||t[31]!==re||t[32]!==$||t[33]!==ee?(ie=[Ke.consoleMessage,$,ee,ae,le,re].filter(Boolean),t[29]=ae,t[30]=le,t[31]=re,t[32]=$,t[33]=ee,t[34]=ie):ie=t[34];const de=ie.join(" ");let oe;t[35]!==U||t[36]!==p?(oe=U?()=>p():void 0,t[35]=U,t[36]=p,t[37]=oe):oe=t[37];const se=U?"Click to load graph in Graph View":void 0,xe=U?"button":void 0,ze=U?0:void 0;let _e;t[38]!==U||t[39]!==p?(_e=U?ve=>{(ve.key==="Enter"||ve.key===" ")&&(ve.preventDefault(),p())}:void 0,t[38]=U,t[39]=p,t[40]=_e):_e=t[40];const Je=U?"Load graph in Graph View":void 0,je=T?"⬆️":_?"⬇️":E?"🕸️":w;let ke;t[41]!==je?(ke=h.jsx("span",{className:Ke.messageIcon,children:je}),t[41]=je,t[42]=ke):ke=t[42];let Me;t[43]!==D.data||t[44]!==D.isJSON||t[45]!==v.message||t[46]!==L?(Me=h.jsx("div",{className:Ke.messageContent,children:D.isJSON?h.jsx("div",{className:Ke.jsonViewWrapper,children:h.jsx($c,{data:D.data,shouldExpandNode:z_,style:{...Ul,container:`${Ul.container} ${Ke.jsonContainer}`,label:Ke.jsonLabel,stringValue:Ke.jsonString,numberValue:Ke.jsonNumber,booleanValue:Ke.jsonBoolean,nullValue:Ke.jsonNull}})}):h.jsxs("span",{className:Ke.messageText,children:[v.message,L&&h.jsx("span",{title:"Upload succeeded",children:" ✅"})]})}),t[43]=D.data,t[44]=D.isJSON,t[45]=v.message,t[46]=L,t[47]=Me):Me=t[47];const te=`${Ke.copyButton} ${P?Ke.copyButtonCopied:""}`,pe=P?"Copied!":"Copy message",we=P?"Copied to clipboard":"Copy message to clipboard",ye=P?"✅":"📄";let Ee;t[48]!==H||t[49]!==Q||t[50]!==te||t[51]!==pe||t[52]!==we||t[53]!==ye?(Ee=h.jsx("button",{className:te,onClick:H,onKeyDown:Q,title:pe,"aria-label":we,tabIndex:0,children:ye}),t[48]=H,t[49]=Q,t[50]=te,t[51]=pe,t[52]=we,t[53]=ye,t[54]=Ee):Ee=t[54];let Ae;t[55]!==V||t[56]!==ne?(Ae=V&&h.jsx("button",{className:Ke.sendToJsonPathButton,onClick:ne,onKeyDown:ve=>{(ve.key==="Enter"||ve.key===" ")&&ne(ve)},title:"Open in JSON-Path Playground","aria-label":"Open this JSON in the JSON-Path Playground",tabIndex:0,children:"➡️"}),t[55]=V,t[56]=ne,t[57]=Ae):Ae=t[57];let Ue;t[58]!==N||t[59]!==R?(Ue=N&&h.jsx("button",{className:Ke.uploadMockButton,onClick:R,onKeyDown:ve=>{(ve.key==="Enter"||ve.key===" ")&&R(ve)},title:"Re-open upload dialog","aria-label":"Re-open mock data upload dialog",tabIndex:0,children:"⬆️ Upload JSON…"}),t[58]=N,t[59]=R,t[60]=Ue):Ue=t[60];let $e;t[61]!==v.time?($e=v.time&&h.jsx("span",{className:Ke.messageTime,children:v.time}),t[61]=v.time,t[62]=$e):$e=t[62];let Le;return t[63]!==de||t[64]!==oe||t[65]!==se||t[66]!==xe||t[67]!==ze||t[68]!==_e||t[69]!==Je||t[70]!==ke||t[71]!==Me||t[72]!==Ee||t[73]!==Ae||t[74]!==Ue||t[75]!==$e?(Le=h.jsxs("div",{className:de,onClick:oe,title:se,role:xe,tabIndex:ze,onKeyDown:_e,"aria-label":Je,children:[ke,Me,Ee,Ae,Ue,$e]}),t[63]=de,t[64]=oe,t[65]=se,t[66]=xe,t[67]=ze,t[68]=_e,t[69]=Je,t[70]=ke,t[71]=Me,t[72]=Ee,t[73]=Ae,t[74]=Ue,t[75]=$e,t[76]=Le):Le=t[76],Le}function z_(r){return r<1}function B_(r){return r.kind==="upload.invitation"}function G_(r){return r.kind==="upload.invitation"}function H_(r){return r.kind==="payload.large"}function U_(r){return r.kind==="graph.link"}function L_(r){const t=Se.c(32),{messages:s,classificationMap:c,onCopy:d,onClear:p,consoleRef:b,onGraphLinkMessage:g,onCopyMessage:f,onSendToJsonPath:y,onUploadMockData:w,successfulUploadPaths:v}=r;let S;t[0]===Symbol.for("react.memo_cache_sentinel")?(S=h.jsx("span",{className:Ke.consoleTitle,children:"Console Output"}),t[0]=S):S=t[0];let D;t[1]!==d?(D=h.jsx("button",{className:Ke.controlButton,onClick:d,title:"Copy console output","aria-label":"Copy console output to clipboard",children:"📑"}),t[1]=d,t[2]=D):D=t[2];let N;t[3]!==p?(N=h.jsx("button",{className:Ke.controlButton,onClick:p,title:"Clear console","aria-label":"Clear console",children:"🗑️"}),t[3]=p,t[4]=N):N=t[4];let E;t[5]!==D||t[6]!==N?(E=h.jsxs("div",{className:Ke.consoleHeader,children:[S,h.jsxs("div",{className:Ke.consoleControls,children:[D,N]})]}),t[5]=D,t[6]=N,t[7]=E):E=t[7];let _;if(t[8]!==c||t[9]!==s||t[10]!==f||t[11]!==g||t[12]!==y||t[13]!==w||t[14]!==v){let L;t[16]!==c||t[17]!==f||t[18]!==g||t[19]!==y||t[20]!==w||t[21]!==v?(L=U=>h.jsx(o_,{fallback:U.raw,children:h.jsx(R_,{message:U.raw,msgId:U.id,classificationMap:c,onGraphLink:g?()=>g(U):void 0,onCopyMessage:f,onSendToJsonPath:y,onUploadMockData:w,successfulUploadPaths:v})},U.id),t[16]=c,t[17]=f,t[18]=g,t[19]=y,t[20]=w,t[21]=v,t[22]=L):L=t[22],_=s.map(L),t[8]=c,t[9]=s,t[10]=f,t[11]=g,t[12]=y,t[13]=w,t[14]=v,t[15]=_}else _=t[15];let T;t[23]!==s.length?(T=s.length===0&&h.jsxs("div",{className:Ke.emptyConsole,children:["No messages yet. Use the ",h.jsx("strong",{children:"Start"})," button in the header to connect."]}),t[23]=s.length,t[24]=T):T=t[24];let C;t[25]!==b||t[26]!==_||t[27]!==T?(C=h.jsxs("div",{className:Ke.console,ref:b,role:"log","aria-live":"polite",children:[_,T]}),t[25]=b,t[26]=_,t[27]=T,t[28]=C):C=t[28];let j;return t[29]!==E||t[30]!==C?(j=h.jsxs("div",{className:Ke.consoleRoot,children:[E,C]}),t[29]=E,t[30]=C,t[31]=j):j=t[31],j}const Y_="_commandInput_j85f1_2",q_="_labelRow_j85f1_8",I_="_labelGroup_j85f1_16",X_="_label_j85f1_8",J_="_infoWrapper_j85f1_28",V_="_paletteToggle_j85f1_34",Z_="_paletteToggleActive_j85f1_66",Q_="_popover_j85f1_73",K_="_popoverOpen_j85f1_95",$_="_popoverTitle_j85f1_121",P_="_popoverRow_j85f1_135",W_="_popoverKeyword_j85f1_156",F_="_popoverDesc_j85f1_168",ex="_popoverAlias_j85f1_174",tx="_inputRow_j85f1_181",nx="_inputWrapper_j85f1_187",ax="_textarea_j85f1_197",ox="_sendButton_j85f1_226",lx="_hint_j85f1_243",ix="_dropup_j85f1_251",sx="_dropupHeader_j85f1_266",rx="_dropupItem_j85f1_282",cx="_dropupItemText_j85f1_305",ux="_matchHighlight_j85f1_313",dx="_multilineIndicator_j85f1_319",rt={commandInput:Y_,labelRow:q_,labelGroup:I_,label:X_,infoWrapper:J_,paletteToggle:V_,paletteToggleActive:Z_,popover:Q_,popoverOpen:K_,popoverTitle:$_,popoverRow:P_,popoverKeyword:W_,popoverDesc:F_,popoverAlias:ex,inputRow:tx,inputWrapper:nx,textarea:ax,sendButton:ox,hint:lx,dropup:ix,dropupHeader:sx,dropupItem:rx,dropupItemText:cx,matchHighlight:ux,multilineIndicator:dx},px=[{keyword:"help",description:"List all help topics, or get help for a specific command",template:"help"},{keyword:"create",description:"Create a new graph node",template:`create node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:"update",description:"Update an existing node",template:`update node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:"edit",description:"Print raw node data ready for editing and re-submitting",template:"edit node {name}"},{keyword:"delete node",description:"Delete a node by name",alias:"clear node",template:"delete node {name}"},{keyword:"delete connection",description:"Delete connection(s) between two nodes",alias:"clear connection",template:"delete connection {nodeA} and {nodeB}"},{keyword:"delete cache",description:"Clear cached API fetcher results",alias:"clear cache",template:"delete cache"},{keyword:"connect",description:"Connect two nodes with a named relation",template:"connect {node-A} to {node-B} with {relation}"},{keyword:"list nodes",description:"List all nodes in the current graph",template:"list nodes"},{keyword:"list connections",description:"List all connections in the current graph",template:"list connections"},{keyword:"describe graph",description:"Describe the current graph model",template:"describe graph"},{keyword:"describe node",description:"Describe a specific node and its connections",template:"describe node {name}"},{keyword:"describe connection",description:"Describe connection(s) between two nodes",template:"describe connection {nodeA} and {nodeB}"},{keyword:"describe skill",description:"Show documentation for a skill by route name",template:"describe skill {skill.route}"},{keyword:"export",description:"Export the graph model to a JSON file",template:"export graph as {name}"},{keyword:"import graph",description:"Import a graph model from a saved file",template:"import graph from {name}"},{keyword:"import node",description:"Import a single node from another saved graph",template:"import node {node-name} from {graph-name}"},{keyword:"instantiate",description:"Create a runnable graph instance with mock input",alias:"start",template:`instantiate graph
{constant} -> input.body.{key}`,multiline:!0},{keyword:"upload mock data",description:"Print the URL to POST a JSON payload as mock input.body",template:"upload mock data"},{keyword:"execute",description:"Execute a single node skill in isolation",template:"execute node {name}"},{keyword:"inspect",description:"Inspect a state-machine variable",template:"inspect {variable_name}"},{keyword:"run",description:"Run the graph instance from root to end",template:"run"}];function hx(r,t){const s=Se.c(22),[c,d]=x.useState(!1),[p,b]=x.useState(-1);let g;if(s[0]!==t||s[1]!==r){e:{const L=t.trimStart();if(L.length===0){let P;s[3]===Symbol.for("react.memo_cache_sentinel")?(P=[],s[3]=P):P=s[3],g=P;break e}const U=L.toLowerCase(),V=r.filter(P=>P.toLowerCase().startsWith(U)),M=new Set;g=V.filter(P=>M.has(P)?!1:(M.add(P),!0)).slice(0,o1)}s[0]=t,s[1]=r,s[2]=g}else g=s[2];const f=g;let y;s[4]===Symbol.for("react.memo_cache_sentinel")?(y=()=>{d(!0),b(-1)},s[4]=y):y=s[4];const w=y;let v;s[5]!==f?(v=L=>{const U=f.length;U!==0&&b(V=>L===1?V<0?0:(V+1)%U:V<=0?U-1:V-1)},s[5]=f,s[6]=v):v=s[6];const S=v;let D;s[7]!==f?(D=(L,U)=>{L>=0&&L<f.length&&U(f[L]),d(!1),b(-1)},s[7]=f,s[8]=D):D=s[8];const N=D;let E;s[9]!==N||s[10]!==p||s[11]!==c||s[12]!==f?(E=L=>{if(!c||f.length===0)return;const U=p>=0?p:0;N(U,L)},s[9]=N,s[10]=p,s[11]=c,s[12]=f,s[13]=E):E=s[13];const _=E;let T;s[14]===Symbol.for("react.memo_cache_sentinel")?(T=()=>{d(!1),b(-1)},s[14]=T):T=s[14];const C=T;let j;return s[15]!==N||s[16]!==p||s[17]!==c||s[18]!==S||s[19]!==_||s[20]!==f?(j={suggestions:f,isOpen:c,activeIndex:p,onCommandChange:w,navigate:S,accept:N,onTab:_,dismiss:C},s[15]=N,s[16]=p,s[17]=c,s[18]=S,s[19]=_,s[20]=f,s[21]=j):j=s[21],j}const fx=r=>x.createElement("svg",{xmlns:"http://www.w3.org/2000/svg",viewBox:"0 0 16 16",fill:"none",width:14,height:14,stroke:"currentColor",strokeWidth:1.5,strokeLinecap:"round",strokeLinejoin:"round",...r},x.createElement("polyline",{points:"2,4 6,8 2,12"}),x.createElement("line",{x1:7,y1:12,x2:14,y2:12}));function mx(r){const t=Se.c(70),{command:s,onChange:c,onKeyDown:d,onSend:p,sendDisabled:b,disabled:g,history:f}=r,y=x.useRef(null),w=x.useRef(null),v=x.useRef(null),[S,D]=x.useState(!1);let N,E;t[0]!==S?(N=()=>{if(!S)return;const te=pe=>{w.current&&!w.current.contains(pe.target)&&D(!1)};return document.addEventListener("mousedown",te),()=>document.removeEventListener("mousedown",te)},E=[S],t[0]=S,t[1]=N,t[2]=E):(N=t[1],E=t[2]),x.useEffect(N,E);const _=hx(f,s);let T;t[3]===Symbol.for("react.memo_cache_sentinel")?(T=()=>{const te=y.current;te&&(te.style.height="auto",te.style.height=`${te.scrollHeight}px`)},t[3]=T):T=t[3];let C;t[4]!==s?(C=[s],t[4]=s,t[5]=C):C=t[5],x.useEffect(T,C);const j=g?"Not connected":"Enter command (Enter to send · Shift+Enter for new line)",L=g?"Enter your test message once it is connected":"Enter to send · Shift+Enter for new line · ↑↓ for history";let U;t[6]!==_||t[7]!==c||t[8]!==d||t[9]!==p?(U=te=>{var pe,we;if(te.key==="Tab"){te.preventDefault(),_.isOpen&&_.suggestions.length>0&&(_.onTab(ye=>c(ye)),requestAnimationFrame(()=>{const ye=y.current;ye&&(ye.selectionStart=ye.selectionEnd=ye.value.length)}));return}if(te.key==="Enter"){if(te.shiftKey)return;if(te.preventDefault(),_.isOpen&&_.activeIndex>=0){_.accept(_.activeIndex,ye=>c(ye)),requestAnimationFrame(()=>{const ye=y.current;ye&&(ye.selectionStart=ye.selectionEnd=ye.value.length)}),(pe=y.current)==null||pe.focus();return}p(),(we=y.current)==null||we.focus();return}if(te.key==="Escape"){if(_.isOpen){_.dismiss(),te.preventDefault();return}return}if(te.key==="ArrowUp"||te.key==="ArrowDown"){if(_.isOpen&&_.suggestions.length>0){te.preventDefault(),_.navigate(te.key==="ArrowDown"?1:-1);return}const ye=y.current;if(ye){const{selectionStart:Ee,value:Ae}=ye,$e=!Ae.slice(0,Ee).includes(`
`),Le=!Ae.slice(Ee).includes(`
`);if(!(te.key==="ArrowUp"&&$e||te.key==="ArrowDown"&&Le))return}d(te),requestAnimationFrame(()=>{const Ee=y.current;Ee&&(Ee.selectionStart=Ee.selectionEnd=Ee.value.length)});return}d(te)},t[6]=_,t[7]=c,t[8]=d,t[9]=p,t[10]=U):U=t[10];const V=U;let M;t[11]===Symbol.for("react.memo_cache_sentinel")?(M=h.jsx("label",{htmlFor:"command",className:rt.label,children:"Command"}),t[11]=M):M=t[11];const X=`${rt.paletteToggle}${S?` ${rt.paletteToggleActive}`:""}`;let P;t[12]===Symbol.for("react.memo_cache_sentinel")?(P=()=>D(bx),t[12]=P):P=t[12];let z;t[13]!==S?(z=te=>{var pe;if(te.key==="ArrowDown"&&S){te.preventDefault();const we=(pe=v.current)==null?void 0:pe.querySelector('[role="option"]');we==null||we.focus()}},t[13]=S,t[14]=z):z=t[14];let H;t[15]===Symbol.for("react.memo_cache_sentinel")?(H=h.jsx(fx,{"aria-hidden":"true",focusable:"false"}),t[15]=H):H=t[15];let G;t[16]!==S||t[17]!==X||t[18]!==z?(G=h.jsx("button",{type:"button",className:X,"aria-label":"Toggle command palette","aria-expanded":S,"aria-controls":"command-palette",onClick:P,onKeyDown:z,title:"Command palette",children:H}),t[16]=S,t[17]=X,t[18]=z,t[19]=G):G=t[19];const Q=`${rt.popover}${S?` ${rt.popoverOpen}`:""}`;let Z,ne;t[20]===Symbol.for("react.memo_cache_sentinel")?(Z=te=>{var pe,we;if(te.key==="ArrowDown"||te.key==="ArrowUp"){te.preventDefault();const ye=(pe=v.current)==null?void 0:pe.querySelectorAll('[role="option"]');if(!ye||ye.length===0)return;const Ee=Array.from(ye).indexOf(document.activeElement);te.key==="ArrowDown"?ye[Ee<0?0:(Ee+1)%ye.length].focus():ye[Ee<=0?ye.length-1:Ee-1].focus()}else te.key==="Escape"&&(te.preventDefault(),D(!1),(we=y.current)==null||we.focus())},ne=h.jsx("p",{className:rt.popoverTitle,children:"Command palette — click to insert"}),t[20]=Z,t[21]=ne):(Z=t[20],ne=t[21]);let k;t[22]!==S||t[23]!==c?(k=px.map(te=>{const{keyword:pe,alias:we,description:ye,template:Ee}=te;return h.jsxs("div",{className:rt.popoverRow,role:"option","aria-selected":!1,tabIndex:S?0:-1,onMouseDown:yx,onClick:()=>{var Ae;c(Ee),D(!1),(Ae=y.current)==null||Ae.focus()},onKeyDown:Ae=>{var Ue;(Ae.key==="Enter"||Ae.key===" ")&&(Ae.preventDefault(),c(Ee),D(!1),(Ue=y.current)==null||Ue.focus())},children:[h.jsx("span",{className:rt.popoverKeyword,children:pe}),h.jsxs("span",{className:rt.popoverDesc,children:[ye,we&&h.jsxs("span",{className:rt.popoverAlias,children:[" · alias: ",we]})]})]},pe)}),t[22]=S,t[23]=c,t[24]=k):k=t[24];let R;t[25]!==Q||t[26]!==k?(R=h.jsxs("div",{id:"command-palette",ref:v,className:Q,role:"listbox","aria-label":"Command palette",onKeyDown:Z,children:[ne,k]}),t[25]=Q,t[26]=k,t[27]=R):R=t[27];let $;t[28]!==G||t[29]!==R?($=h.jsx("div",{className:rt.labelRow,children:h.jsxs("div",{className:rt.labelGroup,children:[M,h.jsxs("span",{ref:w,className:rt.infoWrapper,children:[G,R]})]})}),t[28]=G,t[29]=R,t[30]=$):$=t[30];const ee=!(_.isOpen&&_.suggestions.length>0);let ae;t[31]===Symbol.for("react.memo_cache_sentinel")?(ae=h.jsx("div",{className:rt.dropupHeader,"aria-hidden":"true",children:"Recent Commands"}),t[31]=ae):ae=t[31];let le;t[32]!==_||t[33]!==s||t[34]!==c?(le=_.isOpen&&_.suggestions.length>0&&_.suggestions.map((te,pe)=>{const we=te.split(`
`)[0],ye=te.includes(`
`),Ee=s.trimStart().split(`
`)[0],Ae=Math.min(Ee.length,we.length),Ue=we.slice(0,Ae),$e=we.slice(Ae);return h.jsxs("div",{id:`history-option-${pe}`,role:"option","aria-selected":pe===_.activeIndex,className:rt.dropupItem,onMouseDown:gx,onClick:()=>{_.accept(pe,Le=>c(Le)),requestAnimationFrame(()=>{const Le=y.current;Le&&(Le.selectionStart=Le.selectionEnd=Le.value.length)})},children:[h.jsxs("span",{className:rt.dropupItemText,children:[Ae>0&&h.jsx("strong",{className:rt.matchHighlight,children:Ue}),$e,ye?"…":""]}),ye&&h.jsx("span",{className:rt.multilineIndicator,"aria-label":"multi-line command",children:"↵"})]},te)}),t[32]=_,t[33]=s,t[34]=c,t[35]=le):le=t[35];let re;t[36]!==ee||t[37]!==le?(re=h.jsxs("div",{id:"history-dropup",role:"listbox","aria-label":"Command history suggestions",className:rt.dropup,hidden:ee,children:[ae,le]}),t[36]=ee,t[37]=le,t[38]=re):re=t[38];const ie=_.isOpen&&_.suggestions.length>0,de=_.isOpen&&_.suggestions.length>0&&_.activeIndex>=0?`history-option-${_.activeIndex}`:void 0;let oe;t[39]!==_||t[40]!==c?(oe=te=>{c(te.target.value),_.onCommandChange()},t[39]=_,t[40]=c,t[41]=oe):oe=t[41];let se;t[42]!==_?(se=()=>_.dismiss(),t[42]=_,t[43]=se):se=t[43];let xe;t[44]!==s||t[45]!==g||t[46]!==V||t[47]!==j||t[48]!==ie||t[49]!==de||t[50]!==oe||t[51]!==se?(xe=h.jsx("textarea",{ref:y,id:"command",role:"combobox","aria-expanded":ie,"aria-haspopup":"listbox","aria-controls":"history-dropup","aria-activedescendant":de,"aria-autocomplete":"list",className:rt.textarea,rows:1,placeholder:j,value:s,disabled:g,onChange:oe,onKeyDown:V,onBlur:se,autoComplete:"off",autoCorrect:"off",spellCheck:!1}),t[44]=s,t[45]=g,t[46]=V,t[47]=j,t[48]=ie,t[49]=de,t[50]=oe,t[51]=se,t[52]=xe):xe=t[52];let ze;t[53]!==re||t[54]!==xe?(ze=h.jsxs("div",{className:rt.inputWrapper,children:[re,xe]}),t[53]=re,t[54]=xe,t[55]=ze):ze=t[55];let _e;t[56]!==p?(_e=()=>{var te;p(),(te=y.current)==null||te.focus()},t[56]=p,t[57]=_e):_e=t[57];let Je;t[58]!==b||t[59]!==_e?(Je=h.jsx("button",{className:rt.sendButton,onClick:_e,disabled:b,"aria-label":"Send command",children:"Send"}),t[58]=b,t[59]=_e,t[60]=Je):Je=t[60];let je;t[61]!==ze||t[62]!==Je?(je=h.jsxs("div",{className:rt.inputRow,children:[ze,Je]}),t[61]=ze,t[62]=Je,t[63]=je):je=t[63];let ke;t[64]!==L?(ke=h.jsx("p",{className:rt.hint,children:L}),t[64]=L,t[65]=ke):ke=t[65];let Me;return t[66]!==$||t[67]!==je||t[68]!==ke?(Me=h.jsxs("div",{className:rt.commandInput,children:[$,je,ke]}),t[66]=$,t[67]=je,t[68]=ke,t[69]=Me):Me=t[69],Me}function gx(r){return r.preventDefault()}function yx(r){return r.preventDefault()}function bx(r){return!r}const vx="_root_1ac49_1",_x={root:vx};function xx(r){const t=Se.c(22),{messages:s,classificationMap:c,onCopy:d,onClear:p,consoleRef:b,onGraphLinkMessage:g,onCopyMessage:f,onSendToJsonPath:y,onUploadMockData:w,successfulUploadPaths:v,command:S,onCommandChange:D,onCommandKeyDown:N,onSend:E,sendDisabled:_,inputDisabled:T,commandHistory:C}=r;let j;t[0]!==c||t[1]!==b||t[2]!==s||t[3]!==p||t[4]!==d||t[5]!==f||t[6]!==g||t[7]!==y||t[8]!==w||t[9]!==v?(j=h.jsx(L_,{messages:s,classificationMap:c,onCopy:d,onClear:p,consoleRef:b,onGraphLinkMessage:g,onCopyMessage:f,onSendToJsonPath:y,onUploadMockData:w,successfulUploadPaths:v}),t[0]=c,t[1]=b,t[2]=s,t[3]=p,t[4]=d,t[5]=f,t[6]=g,t[7]=y,t[8]=w,t[9]=v,t[10]=j):j=t[10];let L;t[11]!==S||t[12]!==C||t[13]!==T||t[14]!==D||t[15]!==N||t[16]!==E||t[17]!==_?(L=h.jsx(mx,{command:S,onChange:D,onKeyDown:N,onSend:E,disabled:T,sendDisabled:_,history:C}),t[11]=S,t[12]=C,t[13]=T,t[14]=D,t[15]=N,t[16]=E,t[17]=_,t[18]=L):L=t[18];let U;return t[19]!==j||t[20]!==L?(U=h.jsxs("div",{className:_x.root,children:[j,L]}),t[19]=j,t[20]=L,t[21]=U):U=t[21],U}const Sx="_dialog_g80bk_4",wx="_modalInner_g80bk_26",Ex="_modalHeader_g80bk_34",Tx="_modalTitleGroup_g80bk_44",Nx="_modalTitle_g80bk_44",Ax="_modalPath_g80bk_57",Cx="_closeButton_g80bk_64",jx="_modalBody_g80bk_95",Dx="_dropZone_g80bk_105",Mx="_dropZoneActive_g80bk_127",kx="_dropZoneIcon_g80bk_133",Ox="_dropZoneText_g80bk_139",Rx="_dropZoneOr_g80bk_152",zx="_browseButton_g80bk_159",Bx="_fileInputHidden_g80bk_188",Gx="_fileError_g80bk_193",Hx="_textareaLabel_g80bk_198",Ux="_textarea_g80bk_198",Lx="_validationError_g80bk_226",Yx="_keyboardHint_g80bk_231",qx="_errorBanner_g80bk_236",Ix="_modalFooter_g80bk_247",Xx="_footerActions_g80bk_257",Jx="_formatButton_g80bk_263",Vx="_cancelButton_g80bk_264",Zx="_uploadButton_g80bk_265",Qx="_spinner_g80bk_332",nt={dialog:Sx,modalInner:wx,modalHeader:Ex,modalTitleGroup:Tx,modalTitle:Nx,modalPath:Ax,closeButton:Cx,modalBody:jx,dropZone:Dx,dropZoneActive:Mx,dropZoneIcon:kx,dropZoneText:Ox,dropZoneOr:Rx,browseButton:zx,fileInputHidden:Bx,fileError:Gx,textareaLabel:Hx,textarea:Ux,validationError:Lx,keyboardHint:Yx,errorBanner:qx,modalFooter:Ix,footerActions:Xx,formatButton:Jx,cancelButton:Vx,uploadButton:Zx,spinner:Qx};function Kx(r){const t=Se.c(9),{uploadPath:s,json:c,onSuccess:d,onError:p}=r,[b,g]=x.useState(!1),f=x.useRef(null);let y;t[0]===Symbol.for("react.memo_cache_sentinel")?(y=()=>{var N;(N=f.current)==null||N.abort(),f.current=null,g(!1)},t[0]=y):y=t[0];const w=y;let v;t[1]!==c||t[2]!==p||t[3]!==d||t[4]!==s?(v=async()=>{var E;(E=f.current)==null||E.abort();const N=new AbortController;f.current=N,g(!0);try{const _=await fetch(s,{method:"POST",headers:{"Content-Type":"application/json"},body:c,signal:N.signal}),T=await _.text();if(!_.ok){g(!1),p(`HTTP ${_.status} — ${T}`);return}g(!1),d(T)}catch(_){const T=_;if(T.name==="AbortError"){g(!1);return}g(!1),p(T.message??"Network error")}},t[1]=c,t[2]=p,t[3]=d,t[4]=s,t[5]=v):v=t[5];const S=v;let D;return t[6]!==b||t[7]!==S?(D={isUploading:b,upload:S,cancel:w},t[6]=b,t[7]=S,t[8]=D):D=t[8],D}var Df;const $x=(((Df=navigator.userAgentData)==null?void 0:Df.platform)??navigator.platform).toLowerCase().includes("mac");function Px(r){return new Promise((t,s)=>{const c=new FileReader;c.onload=()=>t(c.result),c.onerror=()=>s(new Error(`Could not read file "${r.name}"`)),c.readAsText(r,"utf-8")})}function Wx(r){const t=r.name.toLowerCase().endsWith(".json"),s=r.type==="application/json"||r.type==="text/plain";return!t&&!s?`"${r.name}" does not appear to be a JSON file. Only .json files are accepted.`:null}function Fx({uploadPath:r,onSuccess:t,onClose:s,onError:c}){const[d,p]=x.useState(""),[b,g]=x.useState(null),[f,y]=x.useState(null),[w,v]=x.useState(!1),S=x.useRef(null),D=x.useRef(null),N=x.useRef(null),_=Ll(d).isJSON,T=_&&d.trim()!=="",{isUploading:C,upload:j,cancel:L}=Kx({uploadPath:r,json:d,onSuccess:t,onError:R=>{g(R),c(R)}});x.useEffect(()=>{var $;const R=S.current;if(R)return R.open||R.showModal(),($=D.current)==null||$.focus(),()=>{R.open&&R.close()}},[]);const U=x.useCallback(()=>{L(),s()},[L,s]),V=x.useCallback(R=>{R.target===S.current&&U()},[U]),M=x.useCallback(R=>{R.preventDefault(),U()},[U]),X=x.useCallback(()=>{g(null),j()},[j]),P=x.useCallback(R=>{R.key==="Enter"&&(R.ctrlKey||R.metaKey)&&(R.preventDefault(),T&&!C&&X())},[T,C,X]),z=x.useCallback(()=>{_&&p(Bc(d))},[_,d]),H=x.useCallback(async R=>{var ee;y(null),g(null);const $=Wx(R);if($){y($);return}try{const ae=await Px(R);if(!Ll(ae).isJSON){y(`"${R.name}" contains invalid JSON.`);return}p(Bc(ae)),(ee=D.current)==null||ee.focus()}catch(ae){y(ae.message)}},[]),G=x.useCallback(R=>{R.preventDefault(),R.stopPropagation(),w||v(!0)},[w]),Q=x.useCallback(R=>{R.preventDefault(),R.stopPropagation(),(R.currentTarget===R.target||!R.currentTarget.contains(R.relatedTarget))&&v(!1)},[]),Z=x.useCallback(R=>{R.preventDefault(),R.stopPropagation(),v(!1);const $=R.dataTransfer.files[0];$&&H($)},[H]),ne=x.useCallback(R=>{var ee;const $=(ee=R.target.files)==null?void 0:ee[0];$&&(H($),R.target.value="")},[H]),k=!_&&d.trim()!=="";return h.jsx("dialog",{ref:S,className:nt.dialog,"aria-modal":"true","aria-labelledby":"mock-upload-modal-title",onClick:V,onCancel:M,children:h.jsxs("div",{className:nt.modalInner,onClick:R=>R.stopPropagation(),children:[h.jsxs("div",{className:nt.modalHeader,children:[h.jsxs("div",{className:nt.modalTitleGroup,children:[h.jsx("span",{id:"mock-upload-modal-title",className:nt.modalTitle,children:"⬆️ Upload Mock Data"}),h.jsx("span",{className:nt.modalPath,children:r})]}),h.jsx("button",{className:nt.closeButton,onClick:U,"aria-label":"Close upload modal",title:"Close",disabled:C,children:"✕"})]}),h.jsxs("div",{className:nt.modalBody,children:[h.jsxs("div",{className:`${nt.dropZone} ${w?nt.dropZoneActive:""}`,onDragOver:G,onDragLeave:Q,onDrop:Z,"aria-label":"Drop a JSON file here",children:[h.jsx("span",{className:nt.dropZoneIcon,children:"📂"}),h.jsxs("span",{className:nt.dropZoneText,children:["Drop a ",h.jsx("code",{children:".json"})," file here"]}),h.jsx("span",{className:nt.dropZoneOr,children:"— or —"}),h.jsx("input",{ref:N,type:"file",accept:".json,application/json",className:nt.fileInputHidden,"aria-hidden":"true",tabIndex:-1,onChange:ne}),h.jsx("button",{type:"button",className:nt.browseButton,onClick:()=>{var R;return(R=N.current)==null?void 0:R.click()},disabled:C,"aria-label":"Browse for a JSON file",children:"Browse file…"})]}),f&&h.jsxs("span",{className:nt.fileError,role:"alert",children:["⚠️ ",f]}),h.jsx("label",{htmlFor:"mock-upload-textarea",className:nt.textareaLabel,children:"JSON Payload"}),h.jsx("textarea",{id:"mock-upload-textarea",ref:D,className:nt.textarea,value:d,onChange:R=>{p(R.target.value),y(null)},onKeyDown:P,placeholder:"Paste JSON here, or drop / browse a .json file above",rows:10,spellCheck:!1,"aria-describedby":k?"mock-upload-validation":void 0}),k&&h.jsx("span",{id:"mock-upload-validation",className:nt.validationError,role:"status",children:"⚠️ Invalid JSON — check syntax"}),h.jsx("span",{className:nt.keyboardHint,children:$x?"⌘+Enter to upload":"Ctrl+Enter to upload"}),b&&h.jsxs("div",{className:nt.errorBanner,role:"alert",children:["❌ Upload failed: ",b]})]}),h.jsxs("div",{className:nt.modalFooter,children:[h.jsx("button",{className:nt.formatButton,onClick:z,disabled:!_||C,title:"Format JSON","aria-label":"Format JSON",children:"Format"}),h.jsxs("div",{className:nt.footerActions,children:[h.jsx("button",{className:nt.cancelButton,onClick:U,disabled:C,children:"Cancel"}),h.jsx("button",{className:nt.uploadButton,onClick:X,disabled:!T||C,"aria-busy":C,children:C?h.jsxs(h.Fragment,{children:[h.jsx("span",{className:nt.spinner,"aria-hidden":"true"})," Uploading…"]}):"Upload ▶"})]})]})]})})}const Jo=/^[A-Za-z0-9_-]+$/,e3=/^[A-Za-z0-9_-]+(?:\[(?:0|[1-9]\d*)\])*$/,t3=new Set(["input","output","model","response","result","parameter","none","next","api","error"]);function Xo(r,t){return`properties.${r}.${t}`}function Jf(r){return r.split(".").every(t=>e3.test(t))}function n3(r,t){return t==="edit"?Jf(r):Jo.test(r)}function tu(r,t={}){var g,f;const s={},c=t.mode??"create",d=r.alias.trim(),p=((g=t.originalAlias)==null?void 0:g.trim())??"",b=r.nodeType.trim();c==="edit"?p?Jo.test(p)||(s.alias="Use only letters, numbers, underscore, and hyphen."):s.alias="Original alias is required.":d?Jo.test(d)?t3.has(d.toLowerCase())?s.alias=`"${d}" is reserved.`:(f=t.graphData)!=null&&f.nodes.some(y=>y.alias.toLowerCase()===d.toLowerCase())&&(s.alias=`Node "${d}" already exists in the current graph.`):s.alias="Use only letters, numbers, underscore, and hyphen.":s.alias="Alias is required.",b&&!Jo.test(b)&&(s.nodeType="Use only letters, numbers, underscore, and hyphen.");for(const y of r.properties){const w=y.key.trim(),v=y.value.trim();!w&&!v||(!w&&v?s[Xo(y.id,"key")]="Property key is required when value is present.":n3(w,c)||(s[Xo(y.id,"key")]=c==="edit"?"Use a property name or dot/bracket path, for example mapping[0] or config.value.":"Use only letters, numbers, underscore, and hyphen."),c==="create"&&(v.includes("\r")||v.includes(`
`))?s[Xo(y.id,"value")]="Property value must be a single line.":v.includes("'''")&&(s[Xo(y.id,"value")]="Property value cannot contain '''."))}return{valid:Object.keys(s).length===0,errors:s}}function Vf(r,t={}){const s={},c=r.trim();return c?Jo.test(c)?t.graphData&&!t.graphData.nodes.some(d=>d.alias.toLowerCase()===c.toLowerCase())&&(s.alias=`Node "${c}" is no longer available in the current graph.`):s.alias="Use only letters, numbers, underscore, and hyphen.":s.alias="Alias is required.",{valid:Object.keys(s).length===0,errors:s}}function a3(r){return r.length<=l1?{valid:!0,errors:{}}:{valid:!1,errors:{command:"The node command is too large. Shorten property values before submitting."}}}let gf=0;function Yl(r="",t=""){return gf+=1,{id:`property-row-${gf}`,key:r,value:t}}function o3(r){return{alias:r==="empty-graph"?"root":"",nodeType:r==="empty-graph"?"Root":"",properties:[Yl()],source:r}}const jc="This node contains data that cannot be safely represented in the edit form. Use the console edit command for this node.";function l3(r){return typeof r=="object"&&r!==null&&!Array.isArray(r)}function i3(r){return r===null?"null":String(r)}function Yc(r,t,s){if(!Jf(r))return!1;if(Array.isArray(t))return t.length===0?!1:t.every((d,p)=>Yc(`${r}[${p}]`,d,s));if(l3(t)){const d=Object.entries(t);return d.length===0?!1:d.every(([p,b])=>Yc(`${r}.${p}`,b,s))}const c=i3(t);return c.includes("'''")?!1:(s.push(Yl(r,c)),!0)}function s3(r){if(!Jo.test(r.alias))return{valid:!1,formState:null,message:jc};if(r.types.length>1)return{valid:!1,formState:null,message:jc};const t=Object.entries(r.properties),s=[];for(const[c,d]of t)if(!Yc(c,d,s))return{valid:!1,formState:null,message:jc};return{valid:!0,formState:{alias:r.alias,nodeType:r.types[0]??"",properties:s.length>0?s:[Yl()],source:"edit-node"},message:null}}const qc=r=>x.createElement("svg",{xmlns:"http://www.w3.org/2000/svg",viewBox:"0 0 16 16",fill:"none",width:16,height:16,stroke:"currentColor",strokeWidth:1.8,strokeLinecap:"round",strokeLinejoin:"round",...r},x.createElement("line",{x1:4.75,y1:4.75,x2:11.25,y2:11.25}),x.createElement("line",{x1:11.25,y1:4.75,x2:4.75,y2:11.25})),r3="_overlay_37wtf_1",c3="_panel_37wtf_21",u3="_form_37wtf_34",d3="_header_37wtf_41",p3="_title_37wtf_50",h3="_iconButton_37wtf_57",f3="_removeButton_37wtf_58",m3="_buttonIcon_37wtf_93",g3="_body_37wtf_151",y3="_field_37wtf_161",b3="_propertyField_37wtf_162",v3="_label_37wtf_169",_3="_input_37wtf_176",x3="_textarea_37wtf_189",S3="_properties_37wtf_213",w3="_propertiesHeader_37wtf_229",E3="_sectionTitle_37wtf_236",T3="_propertyRows_37wtf_242",N3="_propertyActions_37wtf_248",A3="_propertyRow_37wtf_242",C3="_message_37wtf_266",j3="_warningMessage_37wtf_267",D3="_errorMessage_37wtf_268",M3="_errorText_37wtf_293",k3="_footer_37wtf_298",O3="_primaryButton_37wtf_308",R3="_secondaryButton_37wtf_309",z3="_addPropertyButton_37wtf_341",Ne={overlay:r3,panel:c3,form:u3,header:d3,title:p3,iconButton:h3,removeButton:f3,buttonIcon:m3,body:g3,field:y3,propertyField:b3,label:v3,input:_3,textarea:x3,properties:S3,propertiesHeader:w3,sectionTitle:E3,propertyRows:T3,propertyActions:N3,propertyRow:A3,message:C3,warningMessage:j3,errorMessage:D3,errorText:M3,footer:k3,primaryButton:O3,secondaryButton:R3,addPropertyButton:z3},B3=2,G3=8,H3=42;function U3(r){const t=r.split(`
`).reduce((s,c)=>s+Math.max(1,Math.ceil(c.length/H3)),0);return Math.min(Math.max(t,B3),G3)}function L3(r){const t=Se.c(120),{open:s,mode:c,aliasReadOnly:d,formState:p,phase:b,lockReason:g,serverMessage:f,validationErrors:y,onFormStateChange:w,onSubmit:v,onClose:S}=r,D=x.useRef(null),N=x.useRef(null);let E;t[0]===Symbol.for("react.memo_cache_sentinel")?(E=new Map,t[0]=E):E=t[0];const _=x.useRef(E),T=x.useRef(null),C=c==="edit",j=b==="sending",L=g==="disconnected",U=j||L,V=C?"Edit Node":"Create Node",M=C?"Close edit node dialog":"Close create node dialog",X=C?"Save Changes":"Create Node",P=C?"Saving...":"Creating...",z=C?"Connection disconnected. Refresh the page and edit the node again after the app reconnects.":"Connection disconnected. Refresh the page and create the node again after the app reconnects.";let H,G;t[1]!==C||t[2]!==S||t[3]!==s||t[4]!==j?(H=()=>{var ot,at;if(!s)return;C?(ot=N.current)==null||ot.focus():(at=D.current)==null||at.focus();const ge=jn=>{jn.key==="Escape"&&(jn.preventDefault(),j||S())};return document.addEventListener("keydown",ge),()=>{document.removeEventListener("keydown",ge)}},G=[C,S,s,j],t[1]=C,t[2]=S,t[3]=s,t[4]=j,t[5]=H,t[6]=G):(H=t[5],G=t[6]),x.useEffect(H,G);let Q;t[7]===Symbol.for("react.memo_cache_sentinel")?(Q=()=>{const ge=T.current;if(!ge)return;const ot=_.current.get(ge);ot&&(ot.focus(),T.current=null)},t[7]=Q):Q=t[7];let Z;t[8]!==p.properties?(Z=[p.properties],t[8]=p.properties,t[9]=Z):Z=t[9],x.useEffect(Q,Z);const ne=I3;let k;t[10]!==S||t[11]!==j?(k=ge=>{ge.preventDefault(),ge.stopPropagation(),j||S()},t[10]=S,t[11]=j,t[12]=k):k=t[12];const R=k,$=q3;let ee;t[13]!==U||t[14]!==v?(ee=ge=>{ge.preventDefault(),!U&&v()},t[13]=U,t[14]=v,t[15]=ee):ee=t[15];const ae=ee;let le;t[16]!==p||t[17]!==w?(le=ge=>{w({...p,...ge})},t[16]=p,t[17]=w,t[18]=le):le=t[18];const re=le;let ie;t[19]!==p||t[20]!==w?(ie=(ge,ot)=>{w({...p,properties:p.properties.map(at=>at.id===ge?{...at,...ot}:at)})},t[19]=p,t[20]=w,t[21]=ie):ie=t[21];const de=ie;let oe;t[22]!==p||t[23]!==w?(oe=()=>{const ge=Yl();T.current=ge.id,w({...p,properties:[...p.properties,ge]})},t[22]=p,t[23]=w,t[24]=oe):oe=t[24];const se=oe;let xe;t[25]!==p||t[26]!==w?(xe=ge=>{const ot=p.properties.filter(at=>at.id!==ge);w({...p,properties:ot.length>0?ot:[Yl()]})},t[25]=p,t[26]=w,t[27]=xe):xe=t[27];const ze=xe;if(!s)return null;let _e;t[28]!==V?(_e=h.jsx("div",{children:h.jsx("h2",{id:"node-dialog-title",className:Ne.title,children:V})}),t[28]=V,t[29]=_e):_e=t[29];let Je;t[30]===Symbol.for("react.memo_cache_sentinel")?(Je=h.jsx(qc,{className:Ne.buttonIcon,"aria-hidden":"true",focusable:"false"}),t[30]=Je):Je=t[30];let je;t[31]!==M||t[32]!==S||t[33]!==j?(je=h.jsx("button",{type:"button",className:Ne.iconButton,"aria-label":M,onClick:S,disabled:j,children:Je}),t[31]=M,t[32]=S,t[33]=j,t[34]=je):je=t[34];let ke;t[35]!==_e||t[36]!==je?(ke=h.jsxs("header",{className:Ne.header,children:[_e,je]}),t[35]=_e,t[36]=je,t[37]=ke):ke=t[37];let Me;t[38]!==L||t[39]!==f?(Me=f&&!L&&h.jsx("div",{className:Ne.message,role:"status",children:f}),t[38]=L,t[39]=f,t[40]=Me):Me=t[40];let te;t[41]!==y.command?(te=y.command&&h.jsx("div",{className:Ne.errorMessage,role:"alert",children:y.command}),t[41]=y.command,t[42]=te):te=t[42];let pe;t[43]!==L||t[44]!==z||t[45]!==f?(pe=L&&h.jsx("div",{className:Ne.warningMessage,role:"status",children:f??z}),t[43]=L,t[44]=z,t[45]=f,t[46]=pe):pe=t[46];let we;t[47]===Symbol.for("react.memo_cache_sentinel")?(we=h.jsx("span",{className:Ne.label,children:"Alias"}),t[47]=we):we=t[47];const ye=p.alias,Ee=!!y.alias,Ae=y.alias?"node-alias-error":void 0;let Ue;t[48]!==re?(Ue=ge=>re({alias:ge.target.value}),t[48]=re,t[49]=Ue):Ue=t[49];let $e;t[50]!==d||t[51]!==U||t[52]!==p.alias||t[53]!==Ee||t[54]!==Ae||t[55]!==Ue?($e=h.jsx("input",{ref:D,className:Ne.input,value:ye,disabled:U,readOnly:d,"aria-invalid":Ee,"aria-describedby":Ae,onChange:Ue}),t[50]=d,t[51]=U,t[52]=p.alias,t[53]=Ee,t[54]=Ae,t[55]=Ue,t[56]=$e):$e=t[56];let Le;t[57]!==y.alias?(Le=y.alias&&h.jsx("span",{id:"node-alias-error",className:Ne.errorText,children:y.alias}),t[57]=y.alias,t[58]=Le):Le=t[58];let ht;t[59]!==$e||t[60]!==Le?(ht=h.jsxs("label",{className:Ne.field,children:[we,$e,Le]}),t[59]=$e,t[60]=Le,t[61]=ht):ht=t[61];let ve;t[62]===Symbol.for("react.memo_cache_sentinel")?(ve=h.jsx("span",{className:Ne.label,children:"Node Type"}),t[62]=ve):ve=t[62];const yn=p.nodeType,bn=!!y.nodeType,Et=y.nodeType?"node-type-error":void 0;let Tt;t[63]!==re?(Tt=ge=>re({nodeType:ge.target.value}),t[63]=re,t[64]=Tt):Tt=t[64];let Pe;t[65]!==U||t[66]!==p.nodeType||t[67]!==bn||t[68]!==Et||t[69]!==Tt?(Pe=h.jsx("input",{ref:N,className:Ne.input,value:yn,disabled:U,"aria-invalid":bn,"aria-describedby":Et,onChange:Tt}),t[65]=U,t[66]=p.nodeType,t[67]=bn,t[68]=Et,t[69]=Tt,t[70]=Pe):Pe=t[70];let Nt;t[71]!==y.nodeType?(Nt=y.nodeType&&h.jsx("span",{id:"node-type-error",className:Ne.errorText,children:y.nodeType}),t[71]=y.nodeType,t[72]=Nt):Nt=t[72];let It;t[73]!==Pe||t[74]!==Nt?(It=h.jsxs("label",{className:Ne.field,children:[ve,Pe,Nt]}),t[73]=Pe,t[74]=Nt,t[75]=It):It=t[75];let Vt;t[76]===Symbol.for("react.memo_cache_sentinel")?(Vt=h.jsx("div",{className:Ne.propertiesHeader,children:h.jsx("h3",{id:"node-properties-title",className:Ne.sectionTitle,children:"Properties"})}),t[76]=Vt):Vt=t[76];let ft;t[77]!==U||t[78]!==C||t[79]!==p.properties||t[80]!==ze||t[81]!==de||t[82]!==y?(ft=p.properties.map(ge=>{const ot=y[Xo(ge.id,"key")],at=y[Xo(ge.id,"value")],jn=U3(ge.value);return h.jsxs("div",{className:Ne.propertyRow,children:[h.jsxs("label",{className:Ne.propertyField,children:[h.jsx("span",{className:Ne.label,children:"Key"}),h.jsx("input",{ref:jt=>{jt?_.current.set(ge.id,jt):_.current.delete(ge.id)},className:Ne.input,value:ge.key,disabled:U,"aria-invalid":!!ot,onChange:jt=>de(ge.id,{key:jt.target.value})}),ot&&h.jsx("span",{className:Ne.errorText,children:ot})]}),h.jsxs("label",{className:Ne.propertyField,children:[h.jsx("span",{className:Ne.label,children:"Value"}),C?h.jsx("textarea",{className:`${Ne.input} ${Ne.textarea}`,value:ge.value,disabled:U,rows:jn,"aria-invalid":!!at,onChange:jt=>de(ge.id,{value:jt.target.value})}):h.jsx("input",{className:Ne.input,value:ge.value,disabled:U,"aria-invalid":!!at,onChange:jt=>de(ge.id,{value:jt.target.value})}),at&&h.jsx("span",{className:Ne.errorText,children:at})]}),h.jsx("button",{type:"button",className:Ne.removeButton,"aria-label":"Remove property",disabled:U,onClick:()=>ze(ge.id),children:h.jsx(qc,{className:Ne.buttonIcon,"aria-hidden":"true",focusable:"false"})})]},ge.id)}),t[77]=U,t[78]=C,t[79]=p.properties,t[80]=ze,t[81]=de,t[82]=y,t[83]=ft):ft=t[83];let Ye;t[84]!==ft?(Ye=h.jsx("div",{className:Ne.propertyRows,children:ft}),t[84]=ft,t[85]=Ye):Ye=t[85];let At,ct;t[86]===Symbol.for("react.memo_cache_sentinel")?(At=h.jsx("span",{"aria-hidden":"true",children:"+"}),ct=h.jsx("span",{children:"Add Property"}),t[86]=At,t[87]=ct):(At=t[86],ct=t[87]);let Rt;t[88]!==se||t[89]!==U?(Rt=h.jsx("div",{className:Ne.propertyActions,children:h.jsxs("button",{type:"button",className:`${Ne.secondaryButton} ${Ne.addPropertyButton}`,disabled:U,onClick:se,children:[At,ct]})}),t[88]=se,t[89]=U,t[90]=Rt):Rt=t[90];let Zt;t[91]!==Ye||t[92]!==Rt?(Zt=h.jsxs("section",{className:Ne.properties,"aria-labelledby":"node-properties-title",children:[Vt,Ye,Rt]}),t[91]=Ye,t[92]=Rt,t[93]=Zt):Zt=t[93];let zt;t[94]!==Me||t[95]!==te||t[96]!==pe||t[97]!==ht||t[98]!==It||t[99]!==Zt?(zt=h.jsxs("div",{className:Ne.body,children:[Me,te,pe,ht,It,Zt]}),t[94]=Me,t[95]=te,t[96]=pe,t[97]=ht,t[98]=It,t[99]=Zt,t[100]=zt):zt=t[100];let Bt;t[101]!==S||t[102]!==j?(Bt=h.jsx("button",{type:"button",className:Ne.secondaryButton,onClick:S,disabled:j,children:"Cancel"}),t[101]=S,t[102]=j,t[103]=Bt):Bt=t[103];const Kt=j?P:X;let Gt;t[104]!==U||t[105]!==Kt?(Gt=h.jsx("button",{type:"submit",className:Ne.primaryButton,disabled:U,children:Kt}),t[104]=U,t[105]=Kt,t[106]=Gt):Gt=t[106];let vt;t[107]!==Bt||t[108]!==Gt?(vt=h.jsxs("footer",{className:Ne.footer,children:[Bt,Gt]}),t[107]=Bt,t[108]=Gt,t[109]=vt):vt=t[109];let me;t[110]!==ae||t[111]!==zt||t[112]!==vt?(me=h.jsxs("form",{className:Ne.form,onSubmit:ae,children:[zt,vt]}),t[110]=ae,t[111]=zt,t[112]=vt,t[113]=me):me=t[113];let De;t[114]!==ke||t[115]!==me?(De=h.jsxs("div",{className:Ne.panel,role:"dialog","aria-modal":"true","aria-labelledby":"node-dialog-title",onPointerDown:$,onClick:Y3,children:[ke,me]}),t[114]=ke,t[115]=me,t[116]=De):De=t[116];let Ve;return t[117]!==R||t[118]!==De?(Ve=h.jsx("div",{className:Ne.overlay,onPointerDown:ne,onClick:R,children:De}),t[117]=R,t[118]=De,t[119]=Ve):Ve=t[119],Ve}function Y3(r){return r.stopPropagation()}function q3(r){r.stopPropagation()}function I3(r){r.preventDefault(),r.stopPropagation()}function X3(r){const t=Se.c(11),{state:s,validationErrors:c,onFormStateChange:d,onSubmit:p,onClose:b}=r;if(s.status==="closed")return null;const g=s.phase==="sending"?"sending":s.connectionLost?"disconnected":null,f=s.action==="edit-node"?"edit":"create",y=s.action==="edit-node";let w;return t[0]!==g||t[1]!==b||t[2]!==d||t[3]!==p||t[4]!==s.formState||t[5]!==s.phase||t[6]!==s.serverMessage||t[7]!==f||t[8]!==y||t[9]!==c?(w=h.jsx(L3,{open:!0,mode:f,aliasReadOnly:y,formState:s.formState,phase:s.phase,lockReason:g,serverMessage:s.serverMessage,validationErrors:c,onFormStateChange:d,onSubmit:p,onClose:b}),t[0]=g,t[1]=b,t[2]=d,t[3]=p,t[4]=s.formState,t[5]=s.phase,t[6]=s.serverMessage,t[7]=f,t[8]=y,t[9]=c,t[10]=w):w=t[10],w}function Zf(r,t=!1){return r.properties.map(s=>({key:s.key.trim(),value:t?s.value.replace(/\r\n/g,`
`).replace(/\r/g,`
`):s.value.trim()})).filter(s=>s.key||s.value.trim())}function nu(r){const t=a3(r);if(!t.valid)throw new Error(t.errors.command)}function Qf(r,t,s){if(s.includes(`
`)){r.push(`${t}='''`),r.push(s),r.push("'''");return}r.push(`${t}=${s}`)}function J3(r){const t=tu(r);if(!t.valid)throw new Error(Object.values(t.errors)[0]??"Invalid node form state.");const s=r.alias.trim(),c=r.nodeType.trim(),d=Zf(r),p=[`create node ${s}`];if(c&&p.push(`with type ${c}`),d.length>0){p.push("with properties");for(const g of d)Qf(p,g.key,g.value)}const b=p.join(`
`);return nu(b),b}function V3(r,t){const s=t.trim(),c=tu(r,{mode:"edit",originalAlias:s});if(!c.valid)throw new Error(Object.values(c.errors)[0]??"Invalid node form state.");const d=r.nodeType.trim(),p=Zf(r,!0),b=[`update node ${s}`];if(d&&b.push(`with type ${d}`),p.length>0){b.push("with properties");for(const f of p)Qf(b,f.key,f.value)}const g=b.join(`
`);return nu(g),g}function Z3(r,t={}){const s=r.trim(),c=Vf(s,t);if(!c.valid)throw new Error(Object.values(c.errors)[0]??"Invalid node alias.");const d=`delete node ${s}`;return nu(d),d}const Q3=1e4,Dc="A node action is already pending. Wait for it to finish before starting another.",K3="Could not send the create-node command because the WebSocket is not open. The form values remain in this dialog.",$3="Could not send the edit-node command because the WebSocket is not open. Your changes remain in this dialog.",Ic="Could not send the delete-node command because the WebSocket is not open.",P3="This node is no longer available in the current graph.",W3="Connection disconnected. Refresh the page and create the node again after the app reconnects.",Kf="Connection disconnected. Refresh the page and edit the node again after the app reconnects.",yf="Connection disconnected while the node action was pending. The outcome is unknown. Refresh the page and check the graph before trying again.",Yo={status:"closed",pendingSubmit:null,serverMessage:null};function qo(r){return r.pendingSubmit}function bf(r){return r==="edit-node"?$3:r==="delete-node"?Ic:K3}function vf(r){return`The ${r} command was sent, but no backend result was observed yet. The outcome is unknown.`}function F3(r){return r==="edit-node"?Kf:W3}function eS(r,t){return(r==null?void 0:r.trim().toLowerCase())===t.trim().toLowerCase()}function tS(r,t){return(r==null?void 0:r.nodes.find(s=>s.alias.toLowerCase()===t.toLowerCase()))??null}function nS(r,t){return r.status==="error"?!0:eS(r.alias,t.alias)?r.action===null||r.action===t.action:!1}function aS({bus:r,connected:t,graphData:s,executor:c,timeoutMs:d=Q3,onAccepted:p,onUserMessage:b}){const[g,f]=x.useState(Yo),[y,w]=x.useState({}),v=x.useRef(g),S=x.useRef(null),D=x.useRef(t),N=x.useRef(s),E=x.useRef(p),_=x.useRef(b);x.useEffect(()=>{v.current=g},[g]),x.useEffect(()=>{N.current=s},[s]),x.useEffect(()=>{E.current=p},[p]),x.useEffect(()=>{_.current=b},[b]);const T=x.useCallback((H,G="error")=>{var Q;(Q=_.current)==null||Q.call(_,H,G)},[]),C=x.useCallback(H=>{v.current=H,f(H)},[]),j=x.useCallback(()=>{S.current!==null&&(clearTimeout(S.current),S.current=null)},[]),L=x.useCallback(()=>{j(),S.current=setTimeout(()=>{const H=v.current,G=qo(H);G&&(H.status==="open"?C({...H,phase:"editing",pendingSubmit:null,serverMessage:vf(G.action)}):(C(Yo),T(vf(G.action),"error")),S.current=null)},d)},[j,T,C,d]),U=x.useCallback(H=>{if(!t)return;if(qo(v.current)){T(Dc,"error");return}const G=o3(H);w({}),C({status:"open",action:"create-node",phase:"editing",formState:G,originalAlias:null,pendingSubmit:null,serverMessage:null,connectionLost:!1})},[t,T,C]),V=x.useCallback(H=>{if(!t){T(Kf,"error");return}if(qo(v.current)){T(Dc,"error");return}const G=tS(N.current,H.alias);if(!G){T(P3,"error");return}const Q=s3(G);if(!Q.valid||!Q.formState){T(Q.message??"This node cannot be edited in the UI.","error");return}w({}),C({status:"open",action:"edit-node",phase:"editing",formState:Q.formState,originalAlias:G.alias,pendingSubmit:null,serverMessage:null,connectionLost:!1})},[t,T,C]),M=x.useCallback(H=>{if(!t){T(Ic,"error");return}if(qo(v.current)){T(Dc,"error");return}const G=Vf(H.alias,{graphData:N.current});if(!G.valid){T(Object.values(G.errors)[0]??"Invalid node alias.","error");return}let Q;try{Q=Z3(H.alias,{graphData:N.current})}catch(k){T(k instanceof Error?k.message:String(k),"error");return}if(!c.execute(Q)){T(Ic,"error");return}const ne={action:"delete-node",alias:H.alias.trim(),command:Q,sentAt:new Date().toISOString()};w({}),C({status:"closed",pendingSubmit:ne,serverMessage:null}),L()},[t,c,T,C,L]),X=x.useCallback(H=>{const G=v.current;G.status==="open"&&(G.phase==="sending"||G.connectionLost||(w({}),C({...G,formState:H,pendingSubmit:null,serverMessage:null,connectionLost:!1})))},[C]),P=x.useCallback(()=>{var $;const H=v.current;if(H.status!=="open"||H.phase==="sending"||H.connectionLost)return;const G=H.action;if(!t){C({...H,serverMessage:bf(G)});return}const Q=tu(H.formState,G==="edit-node"?{mode:"edit",originalAlias:H.originalAlias}:{graphData:N.current});if(!Q.valid){w(Q.errors);return}let Z,ne;try{G==="edit-node"?(ne=(($=H.originalAlias)==null?void 0:$.trim())??"",Z=V3(H.formState,ne)):(ne=H.formState.alias.trim(),Z=J3(H.formState))}catch(ee){w({command:ee instanceof Error?ee.message:String(ee)});return}if(!c.execute(Z)){C({...H,phase:"editing",pendingSubmit:null,serverMessage:bf(G)});return}const R={action:G,alias:ne,command:Z,sentAt:new Date().toISOString()};w({}),C({...H,phase:"sending",pendingSubmit:R,serverMessage:null,connectionLost:!1}),L()},[t,c,C,L]),z=x.useCallback(()=>{const H=v.current;H.status==="open"&&H.phase!=="sending"&&(j(),w({}),C(Yo))},[j,C]);return x.useEffect(()=>r.on("minigraph.nodeAction.textResult",H=>{var Z;const G=v.current,Q=qo(G);if(!(!Q||!nS(H,Q))){if(j(),H.status==="accepted"){w({}),C(Yo),(Z=E.current)==null||Z.call(E,{status:H.status,action:H.action,alias:H.alias,message:H.message});return}G.status==="open"?C({...G,phase:"editing",pendingSubmit:null,serverMessage:H.status==="error"?`Backend returned an error while this submit was pending: ${H.message}`:H.message}):(C(Yo),T(H.message,"error"))}}),[r,j,T,C]),x.useEffect(()=>{if(D.current&&!t){const H=v.current,G=qo(H);if(H.status==="open"){j();const Q=G?yf:F3(H.action);C({...H,phase:"editing",pendingSubmit:null,serverMessage:Q,connectionLost:!0})}else G&&(j(),C(Yo),T(yf,"error"))}D.current=t},[j,t,T,C]),x.useEffect(()=>()=>{j()},[j]),{state:g,validationErrors:y,openCreateNode:U,openEditNode:V,deleteNode:M,updateFormState:X,submit:P,close:z}}const Xc=(r,t)=>t.some(s=>r instanceof s);let _f,xf;function oS(){return _f||(_f=[IDBDatabase,IDBObjectStore,IDBIndex,IDBCursor,IDBTransaction])}function lS(){return xf||(xf=[IDBCursor.prototype.advance,IDBCursor.prototype.continue,IDBCursor.prototype.continuePrimaryKey])}const Jc=new WeakMap,Mc=new WeakMap,rs=new WeakMap;function iS(r){const t=new Promise((s,c)=>{const d=()=>{r.removeEventListener("success",p),r.removeEventListener("error",b)},p=()=>{s(Ra(r.result)),d()},b=()=>{c(r.error),d()};r.addEventListener("success",p),r.addEventListener("error",b)});return rs.set(t,r),t}function sS(r){if(Jc.has(r))return;const t=new Promise((s,c)=>{const d=()=>{r.removeEventListener("complete",p),r.removeEventListener("error",b),r.removeEventListener("abort",b)},p=()=>{s(),d()},b=()=>{c(r.error||new DOMException("AbortError","AbortError")),d()};r.addEventListener("complete",p),r.addEventListener("error",b),r.addEventListener("abort",b)});Jc.set(r,t)}let Vc={get(r,t,s){if(r instanceof IDBTransaction){if(t==="done")return Jc.get(r);if(t==="store")return s.objectStoreNames[1]?void 0:s.objectStore(s.objectStoreNames[0])}return Ra(r[t])},set(r,t,s){return r[t]=s,!0},has(r,t){return r instanceof IDBTransaction&&(t==="done"||t==="store")?!0:t in r}};function $f(r){Vc=r(Vc)}function rS(r){return lS().includes(r)?function(...t){return r.apply(Zc(this),t),Ra(this.request)}:function(...t){return Ra(r.apply(Zc(this),t))}}function cS(r){return typeof r=="function"?rS(r):(r instanceof IDBTransaction&&sS(r),Xc(r,oS())?new Proxy(r,Vc):r)}function Ra(r){if(r instanceof IDBRequest)return iS(r);if(Mc.has(r))return Mc.get(r);const t=cS(r);return t!==r&&(Mc.set(r,t),rs.set(t,r)),t}const Zc=r=>rs.get(r);function uS(r,t,{blocked:s,upgrade:c,blocking:d,terminated:p}={}){const b=indexedDB.open(r,t),g=Ra(b);return c&&b.addEventListener("upgradeneeded",f=>{c(Ra(b.result),f.oldVersion,f.newVersion,Ra(b.transaction),f)}),s&&b.addEventListener("blocked",f=>s(f.oldVersion,f.newVersion,f)),g.then(f=>{p&&f.addEventListener("close",()=>p()),d&&f.addEventListener("versionchange",y=>d(y.oldVersion,y.newVersion,y))}).catch(()=>{}),g}function dS(r,{blocked:t}={}){const s=indexedDB.deleteDatabase(r);return t&&s.addEventListener("blocked",c=>t(c.oldVersion,c)),Ra(s).then(()=>{})}const pS=["get","getKey","getAll","getAllKeys","count"],hS=["put","add","delete","clear"],kc=new Map;function Sf(r,t){if(!(r instanceof IDBDatabase&&!(t in r)&&typeof t=="string"))return;if(kc.get(t))return kc.get(t);const s=t.replace(/FromIndex$/,""),c=t!==s,d=hS.includes(s);if(!(s in(c?IDBIndex:IDBObjectStore).prototype)||!(d||pS.includes(s)))return;const p=async function(b,...g){const f=this.transaction(b,d?"readwrite":"readonly");let y=f.store;return c&&(y=y.index(g.shift())),(await Promise.all([y[s](...g),d&&f.done]))[0]};return kc.set(t,p),p}$f(r=>({...r,get:(t,s,c)=>Sf(t,s)||r.get(t,s,c),has:(t,s)=>!!Sf(t,s)||r.has(t,s)}));const fS=["continue","continuePrimaryKey","advance"],wf={},Qc=new WeakMap,Pf=new WeakMap,mS={get(r,t){if(!fS.includes(t))return r[t];let s=wf[t];return s||(s=wf[t]=function(...c){Qc.set(this,Pf.get(this)[t](...c))}),s}};async function*gS(...r){let t=this;if(t instanceof IDBCursor||(t=await t.openCursor(...r)),!t)return;t=t;const s=new Proxy(t,mS);for(Pf.set(s,t),rs.set(s,Zc(t));t;)yield s,t=await(Qc.get(s)||t.continue()),Qc.delete(s)}function Ef(r,t){return t===Symbol.asyncIterator&&Xc(r,[IDBIndex,IDBObjectStore,IDBCursor])||t==="iterate"&&Xc(r,[IDBIndex,IDBObjectStore])}$f(r=>({...r,get(t,s,c){return Ef(t,s)?gS:r.get(t,s,c)},has(t,s){return Ef(t,s)||r.has(t,s)}}));const Wf="minigraph-clipboard",yS=1,sa="items";let os=null;function Tf(){return uS(Wf,yS,{upgrade(r){r.objectStoreNames.contains(sa)&&r.deleteObjectStore(sa);const t=r.createObjectStore(sa,{keyPath:"id"});t.createIndex("by-alias","node.alias",{unique:!0}),t.createIndex("by-clippedAt","clippedAt")}})}function Vo(){return os||(os=Tf().catch(async r=>(console.warn("[clipboard/db] openDB failed, deleting and recreating:",r),os=null,await dS(Wf),Tf()))),os}async function bS(){return(await(await Vo()).getAllFromIndex(sa,"by-clippedAt")).reverse()}async function Nf(r){return(await Vo()).getFromIndex(sa,"by-alias",r)}async function vS(r){await(await Vo()).add(sa,r)}async function _S(r,t){const c=(await Vo()).transaction(sa,"readwrite");await c.store.delete(r),await c.store.add(t),await c.done}async function xS(r){await(await Vo()).delete(sa,r)}async function SS(){await(await Vo()).clear(sa)}const wS="minigraph-clipboard-sync";function ES(){return new BroadcastChannel(wS)}function TS(r,t){switch(t.type){case"HYDRATE":return{items:t.items,isLoading:!1};case"ITEM_ADDED":return{...r,items:[t.item,...r.items]};case"ITEM_REPLACED":{const s=r.items.filter(c=>c.id!==t.previousId);return{...r,items:[t.item,...s]}}case"ITEM_REMOVED":return{...r,items:r.items.filter(s=>s.id!==t.id)};case"ITEMS_CLEARED":return{...r,items:[]};default:return r}}const Ff=x.createContext(null);function NS({children:r}){const[t,s]=x.useReducer(TS,{items:[],isLoading:!0}),c=x.useRef(null);x.useEffect(()=>{bS().then(y=>s({type:"HYDRATE",items:y}))},[]),x.useEffect(()=>{let y;try{y=ES()}catch{return}return c.current=y,y.onmessage=w=>{const v=w.data;switch(v.type){case"item-added":s({type:"ITEM_ADDED",item:v.item});break;case"item-replaced":s({type:"ITEM_REPLACED",item:v.item,previousId:v.previousId});break;case"item-removed":s({type:"ITEM_REMOVED",id:v.id});break;case"items-cleared":s({type:"ITEMS_CLEARED"});break}},()=>{y.close(),c.current=null}},[]);const d=x.useCallback(y=>{var w;(w=c.current)==null||w.postMessage(y)},[]),p=x.useCallback(async(y,w,v)=>{try{const S={id:crypto.randomUUID(),clippedAt:new Date().toISOString(),sourceWsPath:v.sourceWsPath,sourceLabel:v.sourceLabel,node:y,connections:w},D=await Nf(y.alias);if(D)return{status:"duplicate",existingItem:D,pendingItem:S};try{await vS(S)}catch(N){if(N instanceof DOMException&&N.name==="ConstraintError"){const E=await Nf(y.alias);if(E)return{status:"duplicate",existingItem:E,pendingItem:S}}throw N}return s({type:"ITEM_ADDED",item:S}),d({type:"item-added",item:S}),{status:"added"}}catch(S){return{status:"error",message:S instanceof Error?S.message:String(S)}}},[d]),b=x.useCallback(async(y,w)=>{await _S(w,y),s({type:"ITEM_REPLACED",item:y,previousId:w}),d({type:"item-replaced",item:y,previousId:w})},[d]),g=x.useCallback(async y=>{await xS(y),s({type:"ITEM_REMOVED",id:y}),d({type:"item-removed",id:y})},[d]),f=x.useCallback(async()=>{await SS(),s({type:"ITEMS_CLEARED"}),d({type:"items-cleared"})},[d]);return h.jsx(Ff.Provider,{value:{items:t.items,isLoading:t.isLoading,clipNode:p,confirmReplace:b,removeItem:g,clearAll:f},children:r})}function em(){const r=x.useContext(Ff);if(!r)throw new Error("useClipboardContext must be used inside <ClipboardProvider>");return r}function tm(r){const t=Date.now(),s=new Date(r).getTime(),c=t-s;if(c<0)return"just now";const d=Math.floor(c/1e3);if(d<60)return"just now";const p=Math.floor(d/60);if(p<60)return`${p} min ago`;const b=Math.floor(p/60);if(b<24)return`${b} hour${b>1?"s":""} ago`;const g=Math.floor(b/24);return g===1?"yesterday":g<30?`${g} days ago`:new Date(r).toLocaleDateString()}const AS="_item_1rbm8_1",CS="_previewFrame_1rbm8_13",jS="_preview_1rbm8_13",DS="_previewShell_1rbm8_25",MS="_metaBlock_1rbm8_29",kS="_timestamp_1rbm8_35",OS="_removeChrome_1rbm8_40",RS="_removeIcon_1rbm8_68",ka={item:AS,previewFrame:CS,preview:jS,previewShell:DS,metaBlock:MS,timestamp:kS,removeChrome:OS,removeIcon:RS};function zS(r){const t=Se.c(43),{item:s,onRemove:c,onOpenMenu:d,onCloseMenu:p}=r,{node:b,clippedAt:g,sourceLabel:f}=s;let y;t[0]!==s.id||t[1]!==p?(y=Z=>{p(),i2(Z.dataTransfer,s.id)},t[0]=s.id,t[1]=p,t[2]=y):y=t[2];const w=y;let v;t[3]!==s.id||t[4]!==d?(v=Z=>{Z.preventDefault(),d(s.id,Z.clientX,Z.clientY)},t[3]=s.id,t[4]=d,t[5]=v):v=t[5];const S=v;let D;t[6]!==s.id||t[7]!==d?(D=Z=>{if(Z.key==="ContextMenu"||Z.key==="F10"&&Z.shiftKey){Z.preventDefault();const ne=Z.currentTarget.getBoundingClientRect();d(s.id,Math.round(ne.left+8),Math.round(ne.top+8))}},t[6]=s.id,t[7]=d,t[8]=D):D=t[8];const N=D,E=`Remove node ${b.alias} from clipboard`;let _;t[9]!==s.id||t[10]!==p||t[11]!==c?(_=Z=>{Z.stopPropagation(),p(),c(s.id)},t[9]=s.id,t[10]=p,t[11]=c,t[12]=_):_=t[12];let T;t[13]===Symbol.for("react.memo_cache_sentinel")?(T=h.jsx(qc,{className:ka.removeIcon,"aria-hidden":"true",focusable:"false"}),t[13]=T):T=t[13];let C;t[14]!==E||t[15]!==_?(C=h.jsx("button",{type:"button",className:ka.removeChrome,draggable:!1,"aria-label":E,onClick:_,children:T}),t[14]=E,t[15]=_,t[16]=C):C=t[16];const j=`Drag node ${b.alias} into the graph to paste`,L=b.types[0]??"unknown";let U;t[17]!==L?(U=qf(L),t[17]=L,t[18]=U):U=t[18];const V=b.types[0]??"unknown";let M;t[19]!==b.alias||t[20]!==b.properties||t[21]!==V?(M=h.jsx(If,{alias:b.alias,nodeType:V,properties:b.properties}),t[19]=b.alias,t[20]=b.properties,t[21]=V,t[22]=M):M=t[22];let X;t[23]!==U||t[24]!==M?(X=h.jsx("div",{className:ka.previewShell,style:U,children:M}),t[23]=U,t[24]=M,t[25]=X):X=t[25];let P;t[26]!==S||t[27]!==w||t[28]!==N||t[29]!==X||t[30]!==j?(P=h.jsx("div",{className:ka.preview,role:"group",draggable:!0,onDragStart:w,onContextMenu:S,onKeyDown:N,tabIndex:0,"aria-label":j,children:X}),t[26]=S,t[27]=w,t[28]=N,t[29]=X,t[30]=j,t[31]=P):P=t[31];let z;t[32]!==P||t[33]!==C?(z=h.jsxs("div",{className:ka.previewFrame,children:[C,P]}),t[32]=P,t[33]=C,t[34]=z):z=t[34];let H;t[35]!==g?(H=tm(g),t[35]=g,t[36]=H):H=t[36];let G;t[37]!==f||t[38]!==H?(G=h.jsx("div",{className:ka.metaBlock,children:h.jsxs("div",{className:ka.timestamp,children:["Clipped ",H," from ",f]})}),t[37]=f,t[38]=H,t[39]=G):G=t[39];let Q;return t[40]!==z||t[41]!==G?(Q=h.jsxs("div",{className:ka.item,children:[z,G]}),t[40]=z,t[41]=G,t[42]=Q):Q=t[42],Q}const BS="_menu_164vh_1",GS="_menuItem_164vh_12",Oc={menu:BS,menuItem:GS},Rc=16;function Af(r,t,s){const c=Rc,d=Math.max(Rc,s-t-Rc);return Math.min(Math.max(r,c),d)}function HS(r){const t=Se.c(28),{open:s,x:c,y:d,canPasteToInput:p,onPasteToInput:b,onInspect:g,onClose:f}=r,y=x.useRef(null),w=x.useRef(null),v=x.useRef(null);let S;t[0]!==c||t[1]!==d?(S={left:c,top:d},t[0]=c,t[1]=d,t[2]=S):S=t[2];const[D,N]=x.useState(S);let E,_;t[3]!==s||t[4]!==c||t[5]!==d?(E=()=>{if(!s||!y.current)return;const P=y.current.getBoundingClientRect();N({left:Af(c,P.width,window.innerWidth),top:Af(d,P.height,window.innerHeight)})},_=[s,c,d],t[3]=s,t[4]=c,t[5]=d,t[6]=E,t[7]=_):(E=t[6],_=t[7]),x.useLayoutEffect(E,_);let T,C;if(t[8]!==p||t[9]!==f||t[10]!==s?(T=()=>{var G,Q;if(!s)return;p?(G=w.current)==null||G.focus():(Q=v.current)==null||Q.focus();const P=Z=>{y.current&&!y.current.contains(Z.target)&&f()},z=Z=>{Z.key==="Escape"&&(Z.preventDefault(),f())},H=()=>f();return document.addEventListener("pointerdown",P),document.addEventListener("keydown",z),window.addEventListener("scroll",H,!0),window.addEventListener("resize",H),()=>{document.removeEventListener("pointerdown",P),document.removeEventListener("keydown",z),window.removeEventListener("scroll",H,!0),window.removeEventListener("resize",H)}},C=[s,p,f],t[8]=p,t[9]=f,t[10]=s,t[11]=T,t[12]=C):(T=t[11],C=t[12]),x.useEffect(T,C),!s)return null;let j;t[13]!==D.left||t[14]!==D.top?(j={left:D.left,top:D.top},t[13]=D.left,t[14]=D.top,t[15]=j):j=t[15];const L=!p;let U;t[16]!==p||t[17]!==b?(U=()=>{p&&b()},t[16]=p,t[17]=b,t[18]=U):U=t[18];let V;t[19]!==L||t[20]!==U?(V=h.jsx("button",{ref:w,role:"menuitem",type:"button",className:Oc.menuItem,disabled:L,onClick:U,children:"Paste to Input"}),t[19]=L,t[20]=U,t[21]=V):V=t[21];let M;t[22]!==g?(M=h.jsx("button",{ref:v,role:"menuitem",type:"button",className:Oc.menuItem,onClick:g,children:"Inspect"}),t[22]=g,t[23]=M):M=t[23];let X;return t[24]!==M||t[25]!==j||t[26]!==V?(X=h.jsxs("div",{ref:y,className:Oc.menu,style:j,role:"menu","aria-label":"Clipboard item actions",children:[V,M]}),t[24]=M,t[25]=j,t[26]=V,t[27]=X):X=t[27],X}const US="_sidebar_nf394_2",LS="_header_nf394_12",YS="_headerTitle_nf394_22",qS="_clearBtn_nf394_29",IS="_itemList_nf394_45",XS="_loading_nf394_55",JS="_emptyState_nf394_65",VS="_emptyIcon_nf394_78",ZS="_emptyTitle_nf394_83",QS="_emptyHint_nf394_87",KS="_inspectPanel_nf394_93",$S="_inspectHeader_nf394_101",PS="_inspectClose_nf394_115",WS="_inspectBody_nf394_129",FS="_dialog_nf394_135",ew="_dialogTitle_nf394_150",tw="_dialogBody_nf394_157",nw="_dialogActions_nf394_164",aw="_cancelBtn_nf394_171",ow="_replaceBtn_nf394_185",bt={sidebar:US,header:LS,headerTitle:YS,clearBtn:qS,itemList:IS,loading:XS,emptyState:JS,emptyIcon:VS,emptyTitle:ZS,emptyHint:QS,inspectPanel:KS,inspectHeader:$S,inspectClose:PS,inspectBody:WS,dialog:FS,dialogTitle:ew,dialogBody:tw,dialogActions:nw,cancelBtn:aw,replaceBtn:ow};function lw(){const r=Se.c(1);let t;return r[0]===Symbol.for("react.memo_cache_sentinel")?(t=h.jsxs("div",{className:bt.emptyState,children:[h.jsx("span",{className:bt.emptyIcon,children:"📋"}),h.jsx("span",{className:bt.emptyTitle,children:"No items clipped yet."}),h.jsx("span",{className:bt.emptyHint,children:"Right-click a node in the Graph view to get started."})]}),r[0]=t):t=r[0],t}function iw(r){const t=Se.c(41),{connected:s,onPasteToInput:c}=r,d=em(),[p,b]=x.useState(null),[g,f]=x.useState(null);let y;t[0]===Symbol.for("react.memo_cache_sentinel")?(y=(R,$,ee)=>{f({itemId:R,x:$,y:ee})},t[0]=y):y=t[0];const w=y;let v;t[1]===Symbol.for("react.memo_cache_sentinel")?(v=()=>{f(null)},t[1]=v):v=t[1];const S=v;let D;t[2]!==c?(D=R=>{S(),c(R)},t[2]=c,t[3]=D):D=t[3];const N=D;let E;t[4]===Symbol.for("react.memo_cache_sentinel")?(E=R=>{S(),b($=>($==null?void 0:$.id)===R.id?null:R)},t[4]=E):E=t[4];const _=E;let T;t[5]!==d?(T=R=>{S(),b($=>($==null?void 0:$.id)===R?null:$),d.removeItem(R)},t[5]=d,t[6]=T):T=t[6];const C=T;let j;t[7]!==d?(j=()=>{S(),b(null),d.clearAll()},t[7]=d,t[8]=j):j=t[8];const L=j;let U,V;t[9]!==g||t[10]!==d.items||t[11]!==p?(U=()=>{const R=new Set(d.items.map(sw));g&&!R.has(g.itemId)&&f(null),p&&!R.has(p.id)&&b(null)},V=[d.items,g,p],t[9]=g,t[10]=d.items,t[11]=p,t[12]=U,t[13]=V):(U=t[12],V=t[13]),x.useEffect(U,V);let M;t[14]!==g||t[15]!==d.items?(M=g?d.items.find(R=>R.id===g.itemId)??null:null,t[14]=g,t[15]=d.items,t[16]=M):M=t[16];const X=M;let P;t[17]===Symbol.for("react.memo_cache_sentinel")?(P=h.jsx("span",{className:bt.headerTitle,children:"Workspace"}),t[17]=P):P=t[17];let z;t[18]!==d.items.length||t[19]!==L?(z=d.items.length>0&&h.jsx("button",{className:bt.clearBtn,onClick:L,"aria-label":"Clear all workspace items",children:"Clear"}),t[18]=d.items.length,t[19]=L,t[20]=z):z=t[20];let H;t[21]!==z?(H=h.jsxs("div",{className:bt.header,children:[P,z]}),t[21]=z,t[22]=H):H=t[22];let G;t[23]!==d.isLoading||t[24]!==d.items||t[25]!==C?(G=d.isLoading?h.jsx("div",{className:bt.loading,children:"Loading…"}):d.items.length===0?h.jsx(lw,{}):d.items.map(R=>h.jsx(zS,{item:R,onRemove:C,onOpenMenu:w,onCloseMenu:S},R.id)),t[23]=d.isLoading,t[24]=d.items,t[25]=C,t[26]=G):G=t[26];let Q;t[27]!==G?(Q=h.jsx("div",{className:bt.itemList,children:G}),t[27]=G,t[28]=Q):Q=t[28];let Z;t[29]!==p?(Z=p&&h.jsxs("div",{className:bt.inspectPanel,children:[h.jsxs("div",{className:bt.inspectHeader,children:[h.jsxs("span",{children:["Inspect node ",p.node.alias]}),h.jsx("button",{className:bt.inspectClose,onClick:()=>b(null),"aria-label":"Close inspect panel",children:"✕"})]}),h.jsx("div",{className:bt.inspectBody,children:h.jsx($c,{data:{node:p.node,connections:p.connections},style:Ul})})]}),t[29]=p,t[30]=Z):Z=t[30];let ne;t[31]!==g||t[32]!==X||t[33]!==s||t[34]!==N?(ne=g&&X&&h.jsx(HS,{open:!0,x:g.x,y:g.y,canPasteToInput:s,onPasteToInput:()=>N(X),onInspect:()=>_(X),onClose:S}),t[31]=g,t[32]=X,t[33]=s,t[34]=N,t[35]=ne):ne=t[35];let k;return t[36]!==H||t[37]!==Q||t[38]!==Z||t[39]!==ne?(k=h.jsxs("div",{className:bt.sidebar,children:[H,Q,Z,ne]}),t[36]=H,t[37]=Q,t[38]=Z,t[39]=ne,t[40]=k):k=t[40],k}function sw(r){return r.id}const Cf=120,jf=18,rw=180,cw=650;function uw(r){const{wheelTargetRef:t,scrollRef:s,contentWrapperRef:c,currentIndex:d,totalPages:p,onNavigatePrev:b,onNavigateNext:g}=r,f=x.useRef(0),y=x.useRef(null),w=x.useRef(!1),v=x.useRef(null),S=x.useRef(b),D=x.useRef(g),N=x.useRef(d),E=x.useRef(p);x.useEffect(()=>{S.current=b}),x.useEffect(()=>{D.current=g}),x.useEffect(()=>{N.current=d}),x.useEffect(()=>{E.current=p}),x.useEffect(()=>{v.current!==null&&(clearTimeout(v.current),v.current=null),c.current&&(c.current.style.transition="none",c.current.style.transform="translateY(0)"),f.current=0,y.current=null},[d]),x.useEffect(()=>{const _=t.current;if(!_)return;function T(){f.current=0,y.current=null,c.current&&(c.current.style.transition="transform 0.28s cubic-bezier(0.25, 0.46, 0.45, 0.94)",c.current.style.transform="translateY(0)")}function C(j){if(j.deltaY===0)return;const L=s.current;if(!L)return;const U=L.scrollTop<=0,V=L.scrollTop+L.clientHeight>=L.scrollHeight-1,M=j.deltaY<0,X=j.deltaY>0,P=U&&M,z=V&&X;if(!P&&!z){T();return}if(w.current)return;const H=N.current,G=E.current;if(P&&H===0||z&&H===G-1)return;const Q=P?"prev":"next";if(y.current!==null&&y.current!==Q&&T(),y.current=Q,f.current+=Math.abs(j.deltaY),c.current){const Z=Q==="prev"?-1:1,ne=f.current*(jf/Cf),k=Math.min(ne,jf)*Z;c.current.style.transition="none",c.current.style.transform=`translateY(${k}px)`}if(v.current!==null&&clearTimeout(v.current),v.current=setTimeout(T,rw),f.current>=Cf){v.current!==null&&clearTimeout(v.current);const Z=y.current;T(),w.current=!0,Z==="prev"?S.current():D.current(),setTimeout(()=>{w.current=!1},cw)}}return _.addEventListener("wheel",C,{passive:!0}),()=>{v.current!==null&&clearTimeout(v.current),_.removeEventListener("wheel",C)}},[])}const dw="_helpRoot_18tja_2",pw="_categoryNav_18tja_11",hw="_categoryTabScroller_18tja_21",fw="_categoryTab_18tja_21",mw="_categoryTabActive_18tja_71",gw="_maximizeButton_18tja_78",yw="_closeButton_18tja_100",bw="_helpBody_18tja_122",vw="_emptyFallback_18tja_130",_w="_helpContent_18tja_147",xw="_topicLink_18tja_226",Sw="_helpBodyContent_18tja_271",ww="_chipStrip_18tja_276",Ew="_chipStripLabel_18tja_294",Tw="_topicChip_18tja_310",Nw="_topicChipActive_18tja_338",Jt={helpRoot:dw,categoryNav:pw,categoryTabScroller:hw,categoryTab:fw,categoryTabActive:mw,maximizeButton:gw,closeButton:yw,helpBody:bw,emptyFallback:vw,helpContent:_w,topicLink:xw,helpBodyContent:Sw,chipStrip:ww,chipStripLabel:Ew,topicChip:Tw,topicChipActive:Nw};function Kc(r){return typeof r=="string"?r:typeof r=="number"?String(r):Array.isArray(r)?r.map(Kc).join(""):Of.isValidElement(r)?Kc(r.props.children):""}function Aw(r){if(!r.trim().toLowerCase().startsWith("help "))return null;const c=r.trim().slice(5).replace(/\s*\(.*\)\s*$/,"").trim().toLowerCase();return c.length>0?c:null}function Cw(r){var de;const t=Se.c(53),{activeTopic:s,onNavigate:c,onClose:d,onToggleMaximize:p,isMaximized:b}=r,g=x.useRef(null),f=x.useRef(null),y=x.useRef(null),w=x.useRef(null);let v;t[0]===Symbol.for("react.memo_cache_sentinel")?(v=()=>{g.current&&(g.current.scrollTop=0)},t[0]=v):v=t[0];let S;t[1]!==s?(S=[s],t[1]=s,t[2]=S):S=t[2],x.useEffect(v,S);let D;t[3]===Symbol.for("react.memo_cache_sentinel")?(D=()=>{const oe=w.current;if(!oe)return;const se=oe.querySelector('[aria-current="step"]');se&&se.scrollIntoView({block:"nearest",inline:"nearest",behavior:"smooth"})},t[3]=D):D=t[3];let N;t[4]!==s?(N=[s],t[4]=s,t[5]=N):N=t[5],x.useEffect(D,N);let E;t[6]!==s?(E=Lf(s),t[6]=s,t[7]=E):E=t[7];const _=E;let T;t[8]!==_?(T=Hc(_),t[8]=_,t[9]=T):T=t[9];const C=T,j=C.length;let L;t[10]!==_?(L=((de=Gc.find(oe=>oe.id===_))==null?void 0:de.chipStripLabel)??null,t[10]=_,t[11]=L):L=t[11];const U=L,V=Ho.indexOf(s),M=V<0?0:V,X=Ho.length;let P,z;t[12]!==c||t[13]!==M?(P=()=>c(Ho[M-1]??""),z=()=>c(Ho[M+1]??Ho[Ho.length-1]),t[12]=c,t[13]=M,t[14]=P,t[15]=z):(P=t[14],z=t[15]);let H;t[16]!==M||t[17]!==P||t[18]!==z?(H={wheelTargetRef:f,scrollRef:g,contentWrapperRef:y,currentIndex:M,totalPages:X,onNavigatePrev:P,onNavigateNext:z},t[16]=M,t[17]=P,t[18]=z,t[19]=H):H=t[19],uw(H);let G;t[20]!==s?(G=ss(s),t[20]=s,t[21]=G):G=t[21];const Q=G;let Z;t[22]!==c?(Z=oe=>{const{children:se,...xe}=oe,ze=Kc(se).trim(),_e=Aw(ze);return _e!==null&&ss(_e)!==null?h.jsx("li",{...xe,children:h.jsx("button",{className:Jt.topicLink,"aria-label":`Open help topic: ${_e}`,onClick:()=>c(_e),children:se})}):h.jsx("li",{...xe,children:se})},t[22]=c,t[23]=Z):Z=t[23];const ne=Z;let k;t[24]!==_||t[25]!==c?(k=Gc.map(oe=>h.jsx("button",{className:[Jt.categoryTab,oe.id===_?Jt.categoryTabActive:""].join(" ").trim(),"aria-current":oe.id===_?"true":void 0,onClick:()=>{const se=Hc(oe.id);c(se[0]??"")},children:oe.label},oe.id)),t[24]=_,t[25]=c,t[26]=k):k=t[26];let R;t[27]!==k?(R=h.jsx("div",{className:Jt.categoryTabScroller,children:k}),t[27]=k,t[28]=R):R=t[28];let $;t[29]!==b||t[30]!==p?($=p&&h.jsx("button",{className:Jt.maximizeButton,onClick:p,"aria-label":b?"Restore help panel":"Maximize help panel",children:b?"⊞":"⛶"}),t[29]=b,t[30]=p,t[31]=$):$=t[31];let ee;t[32]!==d?(ee=d&&h.jsx("button",{className:Jt.closeButton,onClick:d,"aria-label":"Close help panel",children:"×"}),t[32]=d,t[33]=ee):ee=t[33];let ae;t[34]!==R||t[35]!==$||t[36]!==ee?(ae=h.jsxs("nav",{className:Jt.categoryNav,"aria-label":"Help categories",children:[R,$,ee]}),t[34]=R,t[35]=$,t[36]=ee,t[37]=ae):ae=t[37];let le;t[38]!==_||t[39]!==s||t[40]!==C||t[41]!==j||t[42]!==U||t[43]!==c?(le=j>1&&h.jsxs("div",{className:Jt.chipStrip,ref:w,children:[U!==null&&h.jsx("span",{className:Jt.chipStripLabel,children:U}),C.map(oe=>{const se=oe===s,xe=vb(oe,_);return h.jsx("button",{className:[Jt.topicChip,se?Jt.topicChipActive:""].join(" ").trim(),"aria-current":se?"step":void 0,onClick:()=>c(oe),children:xe},oe)})]}),t[38]=_,t[39]=s,t[40]=C,t[41]=j,t[42]=U,t[43]=c,t[44]=le):le=t[44];let re;t[45]!==s||t[46]!==Q||t[47]!==ne?(re=h.jsx("div",{className:Jt.helpBody,ref:g,children:h.jsx("div",{className:Jt.helpBodyContent,ref:y,children:Q===null?h.jsxs("div",{className:Jt.emptyFallback,children:[h.jsxs("code",{children:["help ",s||""]}),"  not found in the local bundle."]}):h.jsx("div",{className:Jt.helpContent,children:h.jsx(My,{remarkPlugins:[ky],components:s===""?{li:ne}:void 0,children:Q})})})}),t[45]=s,t[46]=Q,t[47]=ne,t[48]=re):re=t[48];let ie;return t[49]!==ae||t[50]!==le||t[51]!==re?(ie=h.jsxs("div",{className:Jt.helpRoot,role:"region","aria-label":"Help browser",ref:f,children:[ae,le,re]}),t[49]=ae,t[50]=le,t[51]=re,t[52]=ie):ie=t[52],ie}function jw(r){const t=Se.c(22),{existingItem:s,pendingItem:c,onReplace:d,onCancel:p}=r,b=x.useRef(null);let g,f;t[0]===Symbol.for("react.memo_cache_sentinel")?(g=()=>{const C=b.current;C&&!C.open&&C.showModal()},f=[],t[0]=g,t[1]=f):(g=t[0],f=t[1]),x.useEffect(g,f);let y;t[2]===Symbol.for("react.memo_cache_sentinel")?(y=h.jsx("h2",{id:"duplicate-dialog-title",className:bt.dialogTitle,children:"Duplicate Node"}),t[2]=y):y=t[2];let w;t[3]!==c.node.alias?(w=h.jsxs("strong",{children:['"',c.node.alias,'"']}),t[3]=c.node.alias,t[4]=w):w=t[4];let v;t[5]!==s.clippedAt?(v=tm(s.clippedAt),t[5]=s.clippedAt,t[6]=v):v=t[6];let S;t[7]!==w||t[8]!==v?(S=h.jsxs("p",{className:bt.dialogBody,children:["A clipboard item with alias ",w," already exists (clipped ",v,")."]}),t[7]=w,t[8]=v,t[9]=S):S=t[9];let D;t[10]===Symbol.for("react.memo_cache_sentinel")?(D=h.jsx("p",{className:bt.dialogBody,children:"Replace it with the new snapshot?"}),t[10]=D):D=t[10];let N;t[11]!==p?(N=h.jsx("button",{className:bt.cancelBtn,onClick:p,children:"Cancel"}),t[11]=p,t[12]=N):N=t[12];let E;t[13]!==d?(E=h.jsx("button",{className:bt.replaceBtn,onClick:d,children:"Replace"}),t[13]=d,t[14]=E):E=t[14];let _;t[15]!==N||t[16]!==E?(_=h.jsxs("div",{className:bt.dialogActions,children:[N,E]}),t[15]=N,t[16]=E,t[17]=_):_=t[17];let T;return t[18]!==p||t[19]!==_||t[20]!==S?(T=h.jsxs("dialog",{ref:b,className:bt.dialog,onClose:p,"aria-labelledby":"duplicate-dialog-title",children:[y,S,D,_]}),t[18]=p,t[19]=_,t[20]=S,t[21]=T):T=t[21],T}function Dw(r,t){if(!t)return null;const s=r.trim().toLowerCase();if(s!=="help"&&!s.startsWith("help "))return null;const c=Yf(r);return ss(c)!==null?c:null}class Mw{constructor(){this.listeners=new Map}on(t,s){const c=t;return this.listeners.has(c)||this.listeners.set(c,new Set),this.listeners.get(c).add(s),()=>{var d;(d=this.listeners.get(c))==null||d.delete(s)}}emit(t){const s=this.listeners.get(t.kind);s&&s.forEach(c=>{try{c(t)}catch(d){console.error(`[ProtocolBus] listener for '${t.kind}' threw:`,d)}})}clear(){this.listeners.clear()}}const kw=new Set(["info","error","ping","welcome"]);function Ow(r,t){const s=[],c={msgId:r,raw:t};let d=!1,p=!1,b=!1,g=!1,f=!1;const y=Ll(t);if(y.isJSON){const T=y.data;if(typeof T.type=="string"){const C=T.type;return s.push({...c,kind:"lifecycle",type:C,knownType:kw.has(C),message:typeof T.message=="string"?T.message:t,time:T.time??null}),s.length>0?s:[{...c,kind:"unclassified"}]}return s.push({...c,kind:"json.response",data:y.data}),s.length>0?s:[{...c,kind:"unclassified"}]}const w=h1(t);w&&(f=!0,s.push({...c,kind:"payload.large",apiPath:w.apiPath,byteSize:w.byteSize,filename:w.filename}));const v=f1(t);v&&(b=!0,s.push({...c,kind:"upload.invitation",uploadPath:v}));const S=Hf(t);if(S&&(g=!0,s.push({...c,kind:"upload.contentPath",uploadPath:S})),Gf(t)){p=!0;const T=Fc(t);T&&s.push({...c,kind:"graph.link",apiPath:T})}if(p){const T=d1(t);T&&s.push({...c,kind:"graph.exported",graphName:T.graphName,apiPath:T.apiPath})}const D=E1(t);D&&s.push({...c,kind:"graph.mutation",mutationType:D});const N=w1(t);N&&s.push({...c,kind:"minigraph.nodeAction.textResult",status:N.status,action:N.action,alias:N.alias,message:N.message}),N&&(N.action==="create-node"||N.status==="error")&&s.push({...c,kind:"minigraph.createNode.textResult",status:N.status,alias:N.alias,message:N.message}),t.startsWith("> ")&&(d=!0,s.push({...c,kind:"command.echo",commandText:t.slice(2)})),m1(t)&&s.push({...c,kind:"command.helpOrDescribe",commandText:t.slice(2)});const E=g1(t);E&&s.push({...c,kind:"command.importGraph",graphName:E});const _=p1(t);return _&&s.push({...c,kind:"graph.export.failed",reason:_.reason}),!d&&!p&&!b&&!g&&!f&&Wc(t)&&s.push({...c,kind:"docs.response",isMarkdown:!0}),s.length===0&&s.push({...c,kind:"unclassified"}),s}function Rw(r){const t=Se.c(12),{messages:s,bus:c}=r,d=x.useRef(-1);let p;t[0]!==s?(p=()=>{s.length>0&&(d.current=s[s.length-1].id)},t[0]=s,t[1]=p):p=t[1];let b;t[2]===Symbol.for("react.memo_cache_sentinel")?(b=[],t[2]=b):b=t[2],x.useEffect(p,b);let g;if(t[3]!==s){g=new Map;for(const S of s)g.set(S.id,Ow(S.id,S.raw));t[3]=s,t[4]=g}else g=t[4];const f=g;let y,w;t[5]!==c||t[6]!==f||t[7]!==s?(y=()=>{if(s.length===0)return;const S=s.filter(D=>D.id>d.current);if(S.length!==0){d.current=s[s.length-1].id;for(const D of S){const N=f.get(D.id);if(N)for(const E of N)c.emit(E)}}},w=[s,c,f],t[5]=c,t[6]=f,t[7]=s,t[8]=y,t[9]=w):(y=t[8],w=t[9]),x.useEffect(y,w);let v;return t[10]!==f?(v={classificationMap:f},t[10]=f,t[11]=v):v=t[11],v}function zw({config:r}){const{title:t,wsPath:s,storageKeyPayload:c,storageKeyHistory:d,storageKeyTab:p,storageKeySavedGraphs:b,supportsUpload:g,supportsClipboard:f,supportsHelp:y,supportsAuthoring:w,tabs:v}=r,S=gy(),[D,N]=Oa(c,""),E=Pc(),[_,T]=x.useState(()=>E.peekPendingPayload(s)),{takePendingPayload:C}=E;x.useEffect(()=>{const me=C(s);me!==null&&T(me)},[C,s]);const j=_??D,L=x.useCallback(me=>{T(null),N(me)},[N]),U=x.useMemo(()=>j?t1(j):{valid:!0,error:null,type:null},[j]),{toasts:V,addToast:M,removeToast:X}=n1(),z=x.useRef(new Mw).current,H=x.useCallback(me=>Dw(me,y===!0)!==null,[y]),G=A1({wsPath:s,storageKeyHistory:d,payload:j,addToast:M,bus:z,handleLocalCommand:H}),{classificationMap:Q}=Rw({messages:G.messages,bus:z}),[Z,ne]=kb(s),{graphData:k,setGraphData:R,rightTab:$,setRightTab:ee,isRefreshing:ae}=D1(Z,M,v[0],v,p),{modalUploadPath:le,successfulUploadPaths:re,handleOpenUploadModal:ie,handleCloseUploadModal:de,handleUploadSuccess:oe,handleUploadError:se,resetSuccessfulPaths:xe}=Eb({bus:z,addToast:M});M1({bus:z,pinnedGraphPath:Z,setPinnedGraphPath:ne,connected:G.connected,sendRawText:G.sendRawText,addToast:M});const ze=x.useRef(!1);x.useEffect(()=>{ze.current&&!G.connected&&(ne(null),R(null)),ze.current=G.connected},[G.connected,ne,R]);const[_e,Je]=Oa(r.storageKeyHelpTopic??"help-topic-fallback",""),[je,ke]=Oa("help-panel-open",!1),[Me,te]=x.useState(()=>!!y&&!je),[pe,we]=x.useState(!1),ye=x.useRef(null),Ee=x.useCallback(()=>{Me&&(we(!0),ye.current=setTimeout(()=>te(!1),400))},[Me]);x.useEffect(()=>{if(!Me||pe)return;const me=setTimeout(Ee,3e3);return()=>clearTimeout(me)},[Me,pe,Ee]),x.useEffect(()=>{je&&Me&&Ee()},[je,Me,Ee]),x.useEffect(()=>()=>{ye.current&&clearTimeout(ye.current)},[]),x.useEffect(()=>{if(!y)return;const me=De=>{De.ctrlKey&&De.key==="`"&&(De.preventDefault(),ke(Ve=>!Ve))};return window.addEventListener("keydown",me),()=>window.removeEventListener("keydown",me)},[y,ke]),_b({bus:z,setHelpTopic:Je,onTabSwitch:y?()=>ke(!0):()=>{}}),Tb({bus:z,connected:G.connected,appendMessage:G.appendMessage,addToast:M});const Ae=em(),[Ue,$e]=Oa("clipboard-sidebar-open",!1),[Le,ht]=x.useState(null),ve=x.useCallback(me=>{const De=nf(me,k);G.setCommand(De.command),M(`${De.verb==="create"?"Create":"Update"} command for "${me.node.alias}" pasted to input`,"info")},[k,G.setCommand,M]),yn=x.useCallback(me=>{const De=Ae.items.find(ge=>ge.id===me);if(!De){M("Clipboard item is no longer available. It may have been removed in another tab.","error");return}const Ve=nf(De,k);if(!G.sendRawText(Ve.command)){M("Could not send clipboard paste command because the WebSocket is not open.","error");return}M(`Clipboard node "${De.node.alias}" sent as ${Ve.verb}. Waiting for backend response.`,"info")},[Ae.items,k,G.sendRawText,M]),bn=x.useCallback(async(me,De)=>{try{const Ve=await Ae.clipNode(me,De,{sourceWsPath:s,sourceLabel:r.label});switch(Ve.status){case"added":M(`Node "${me.alias}" clipped to workspace`,"success");break;case"duplicate":ht({pendingItem:Ve.pendingItem,existingItem:Ve.existingItem});break;case"error":M(`Clip failed: ${Ve.message}`,"error");break}}catch(Ve){M(`Clip failed: ${Ve instanceof Error?Ve.message:String(Ve)}`,"error")}},[Ae,s,r.label,M]),Et=Ab(b??""),{defaultName:Tt,setLastSavedName:Pe,resetName:Nt}=jb(b?`${b}-untitled-counter`:"untitled-counter",z),Vt=x.useMemo(()=>{var Ve;const me=k==null?void 0:k.nodes.find(ge=>ge.types.includes("Root")),De=typeof((Ve=me==null?void 0:me.properties)==null?void 0:Ve.name)=="string"?me.properties.name:void 0;return De!=null&&De.trim()?De:null},[k])??Tt,ft=x.useMemo(()=>Rb(G.sendRawText),[G.sendRawText]),Ye=aS({bus:z,connected:G.connected,graphData:k,executor:ft,onUserMessage:M}),{handleSaveGraph:At,handleLoadGraph:ct}=Mb({bus:z,connected:G.connected,sendRawText:G.sendRawText,saveGraph:Et.saveGraph,setLastSavedName:Pe,addToast:M}),Rt=x.useCallback(me=>{const De=Q.get(me.id),Ve=De==null?void 0:De.find(ge=>ge.kind==="graph.link");Ve&&ne(Ve.apiPath)},[Q]),{handleSendToJsonPath:Zt}=xb({ctx:E,navigate:S,addToast:M,wsPath:s}),zt=j1("(max-width: 768px)"),{defaultLayout:Bt,onLayoutChanged:Kt}=hy({id:r.path+"-panel-split",storage:localStorage}),Gt=x.useCallback(()=>L(Bc(j)),[j]),vt=x.useCallback(()=>{G.clearMessages(),ne(null),R(null),xe(),Nt()},[G.clearMessages,R,xe,Nt]);return h.jsxs("div",{className:an.wrapper,children:[h.jsx(Ib,{toasts:V,onRemove:X}),le&&h.jsx(Fx,{uploadPath:le,onSuccess:oe,onClose:de,onError:se}),w&&h.jsx(X3,{state:Ye.state,validationErrors:Ye.validationErrors,onFormStateChange:Ye.updateFormState,onSubmit:Ye.submit,onClose:Ye.close}),h.jsxs("header",{className:an.header,children:[h.jsx("h1",{className:an.title,children:t}),h.jsxs("div",{className:an.headerActions,children:[b&&h.jsx(M0,{disabled:!k,defaultName:Tt,onSave:At,nameExists:Et.hasGraph,connected:G.connected}),b&&Et.savedGraphs.length>0&&h.jsx(q0,{savedGraphs:Et.savedGraphs,onLoad:ct,onDelete:Et.deleteGraph,connected:G.connected}),f&&h.jsxs("button",{className:an.clipboardToggle,onClick:()=>$e(me=>!me),"aria-label":Ue?"Close workspace sidebar":"Open workspace sidebar","aria-pressed":Ue,children:["Workspace",Ae.items.length>0?` (${Ae.items.length})`:""]}),h.jsx(_0,{addToast:M}),y&&h.jsxs("div",{className:an.helpButtonWrapper,children:[h.jsx("button",{className:`${an.helpToggle}${Me&&!pe?` ${an.helpTogglePulsing}`:""}`,onClick:()=>ke(me=>!me),"aria-label":je?"Close help panel":"Open help panel","aria-pressed":je,children:"?"}),Me&&h.jsxs("div",{className:`${an.helpHint}${pe?` ${an.helpHintFading}`:""}`,onClick:Ee,role:"status",children:[h.jsx("kbd",{className:an.helpHintKbd,children:"Ctrl + `"})," to toggle help"]})]})]})]}),Le&&h.jsx(jw,{existingItem:Le.existingItem,pendingItem:Le.pendingItem,onReplace:async()=>{try{await Ae.confirmReplace(Le.pendingItem,Le.existingItem.id),ht(null),M(`Clipboard item "${Le.pendingItem.node.alias}" replaced`,"success")}catch(me){M(`Replace failed: ${me instanceof Error?me.message:String(me)}`,"error")}},onCancel:()=>{ht(null),M("Clip cancelled","info")}}),h.jsxs(Mf,{className:an.panelGroup,orientation:zt?"vertical":"horizontal",defaultLayout:Bt,onLayoutChanged:Kt,children:[h.jsx(Hl,{defaultSize:je||Ue?"50%":"60%",minSize:"25%",children:h.jsx(xx,{messages:G.messages,classificationMap:Q,onCopy:G.copyMessages,onClear:vt,consoleRef:G.consoleRef,command:G.command,onCommandChange:G.setCommand,onCommandKeyDown:G.handleKeyDown,onSend:G.sendCommand,sendDisabled:!G.connected||!G.command.trim(),inputDisabled:!G.connected,commandHistory:G.history,onGraphLinkMessage:Rt,onCopyMessage:()=>M("Copied to clipboard","success"),onSendToJsonPath:Zt,onUploadMockData:ie,successfulUploadPaths:re})}),h.jsx(zc,{className:an.resizeHandle,"aria-label":"Resize panels"}),h.jsx(Hl,{defaultSize:je?"50%":Ue?"30%":"40%",minSize:"20%",children:h.jsx(a_,{tabs:v,payload:j,onChange:L,validation:U,onFormat:Gt,onUpload:g?G.uploadPayload:void 0,graphData:k,graphName:Vt,activeTab:$,onTabChange:ee,onGraphRenderError:me=>M(me,"error"),onGraphDataCopySuccess:()=>M("Graph JSON copied to clipboard!","success"),onGraphDataCopyError:()=>M("Copy failed","error"),isGraphRefreshing:ae,onClipNode:f?bn:void 0,onClipboardDrop:f?yn:void 0,isConnected:G.connected,supportsAuthoring:w,onCreateNode:w?Ye.openCreateNode:void 0,onEditNode:w?Ye.openEditNode:void 0,onDeleteNode:w?Ye.deleteNode:void 0,helpPanel:y&&je?(me,De)=>h.jsx(Cw,{activeTopic:_e,onNavigate:Je,onClose:()=>ke(!1),onToggleMaximize:me,isMaximized:De}):void 0})}),f&&Ue&&h.jsxs(h.Fragment,{children:[h.jsx(zc,{className:an.resizeHandle,"aria-label":"Resize clipboard"}),h.jsx(Hl,{defaultSize:"20%",minSize:"10%",maxSize:"40%",children:h.jsx(iw,{connected:G.connected,onPasteToInput:ve})})]})]})]})}function Bw(){const r=Se.c(2),t=ia[0].path;let s;r[0]===Symbol.for("react.memo_cache_sentinel")?(s=ia.map(Gw),r[0]=s):s=r[0];let c;return r[1]===Symbol.for("react.memo_cache_sentinel")?(c=h.jsx(r1,{children:h.jsx(NS,{children:h.jsx(yy,{children:h.jsxs(by,{children:[s,h.jsx(Rf,{path:"*",element:h.jsx(vy,{to:t,replace:!0})})]})})})}),r[1]=c):c=r[1],c}function Gw(r){return h.jsx(Rf,{path:r.path,element:h.jsx(zw,{config:r},r.path)},r.path)}Gy.createRoot(document.getElementById("root")).render(h.jsx(x.StrictMode,{children:h.jsx(Bw,{})}));
//# sourceMappingURL=index-DCFH_soq.js.map
