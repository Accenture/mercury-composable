import{j as f,T as Wh,_ as co,H as cc,W as Mg}from"./vendor-panels-Cixz1HBJ.js";import{a as Ph,b as Dg,r as T,N as Og,R as Fh,u as kg,B as Rg,c as zg,d as ef,e as Bg}from"./vendor-router-DUFbnzxw.js";import{N as Hg,H as Sh,P as xh,M as Gg,u as Ug,a as Lg,B as Yg,b as qg,C as Xg,c as Jg,i as Ig}from"./vendor-xyflow-k-RwjR-l.js";import{c as Zg,a as Qg,d as uo,J as Sc}from"./vendor-json-view-Djmwb-hd.js";import{M as Vg,r as Kg}from"./vendor-markdown-Cp1IxVgw.js";(function(){const n=document.createElement("link").relList;if(n&&n.supports&&n.supports("modulepreload"))return;for(const p of document.querySelectorAll('link[rel="modulepreload"]'))u(p);new MutationObserver(p=>{for(const h of p)if(h.type==="childList")for(const b of h.addedNodes)b.tagName==="LINK"&&b.rel==="modulepreload"&&u(b)}).observe(document,{childList:!0,subtree:!0});function s(p){const h={};return p.integrity&&(h.integrity=p.integrity),p.referrerPolicy&&(h.referrerPolicy=p.referrerPolicy),p.crossOrigin==="use-credentials"?h.credentials="include":p.crossOrigin==="anonymous"?h.credentials="omit":h.credentials="same-origin",h}function u(p){if(p.ep)return;p.ep=!0;const h=s(p);fetch(p.href,h)}})();var Pr={exports:{}},so={},Fr={exports:{}},ec={};/**
 * @license React
 * scheduler.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var wh;function $g(){return wh||(wh=1,(function(r){function n(D,O){var Q=D.length;D.push(O);e:for(;0<Q;){var P=Q-1>>>1,F=D[P];if(0<p(F,O))D[P]=O,D[Q]=F,Q=P;else break e}}function s(D){return D.length===0?null:D[0]}function u(D){if(D.length===0)return null;var O=D[0],Q=D.pop();if(Q!==O){D[0]=Q;e:for(var P=0,F=D.length,ie=F>>>1;P<ie;){var oe=2*(P+1)-1,ee=D[oe],ye=oe+1,se=D[ye];if(0>p(ee,Q))ye<F&&0>p(se,ee)?(D[P]=se,D[ye]=Q,P=ye):(D[P]=ee,D[oe]=Q,P=oe);else if(ye<F&&0>p(se,Q))D[P]=se,D[ye]=Q,P=ye;else break e}}return O}function p(D,O){var Q=D.sortIndex-O.sortIndex;return Q!==0?Q:D.id-O.id}if(r.unstable_now=void 0,typeof performance=="object"&&typeof performance.now=="function"){var h=performance;r.unstable_now=function(){return h.now()}}else{var b=Date,g=b.now();r.unstable_now=function(){return b.now()-g}}var m=[],y=[],w=1,v=null,x=3,N=!1,E=!1,C=!1,_=!1,j=typeof setTimeout=="function"?setTimeout:null,k=typeof clearTimeout=="function"?clearTimeout:null,H=typeof setImmediate<"u"?setImmediate:null;function L(D){for(var O=s(y);O!==null;){if(O.callback===null)u(y);else if(O.startTime<=D)u(y),O.sortIndex=O.expirationTime,n(m,O);else break;O=s(y)}}function X(D){if(C=!1,L(D),!E)if(s(m)!==null)E=!0,Y||(Y=!0,te());else{var O=s(y);O!==null&&re(X,O.startTime-D)}}var Y=!1,B=-1,I=5,V=-1;function K(){return _?!0:!(r.unstable_now()-V<I)}function $(){if(_=!1,Y){var D=r.unstable_now();V=D;var O=!0;try{e:{E=!1,C&&(C=!1,k(B),B=-1),N=!0;var Q=x;try{t:{for(L(D),v=s(m);v!==null&&!(v.expirationTime>D&&K());){var P=v.callback;if(typeof P=="function"){v.callback=null,x=v.priorityLevel;var F=P(v.expirationTime<=D);if(D=r.unstable_now(),typeof F=="function"){v.callback=F,L(D),O=!0;break t}v===s(m)&&u(m),L(D)}else u(m);v=s(m)}if(v!==null)O=!0;else{var ie=s(y);ie!==null&&re(X,ie.startTime-D),O=!1}}break e}finally{v=null,x=Q,N=!1}O=void 0}}finally{O?te():Y=!1}}}var te;if(typeof H=="function")te=function(){H($)};else if(typeof MessageChannel<"u"){var ue=new MessageChannel,he=ue.port2;ue.port1.onmessage=$,te=function(){he.postMessage(null)}}else te=function(){j($,0)};function re(D,O){B=j(function(){D(r.unstable_now())},O)}r.unstable_IdlePriority=5,r.unstable_ImmediatePriority=1,r.unstable_LowPriority=4,r.unstable_NormalPriority=3,r.unstable_Profiling=null,r.unstable_UserBlockingPriority=2,r.unstable_cancelCallback=function(D){D.callback=null},r.unstable_forceFrameRate=function(D){0>D||125<D?console.error("forceFrameRate takes a positive int between 0 and 125, forcing frame rates higher than 125 fps is not supported"):I=0<D?Math.floor(1e3/D):5},r.unstable_getCurrentPriorityLevel=function(){return x},r.unstable_next=function(D){switch(x){case 1:case 2:case 3:var O=3;break;default:O=x}var Q=x;x=O;try{return D()}finally{x=Q}},r.unstable_requestPaint=function(){_=!0},r.unstable_runWithPriority=function(D,O){switch(D){case 1:case 2:case 3:case 4:case 5:break;default:D=3}var Q=x;x=D;try{return O()}finally{x=Q}},r.unstable_scheduleCallback=function(D,O,Q){var P=r.unstable_now();switch(typeof Q=="object"&&Q!==null?(Q=Q.delay,Q=typeof Q=="number"&&0<Q?P+Q:P):Q=P,D){case 1:var F=-1;break;case 2:F=250;break;case 5:F=1073741823;break;case 4:F=1e4;break;default:F=5e3}return F=Q+F,D={id:w++,callback:O,priorityLevel:D,startTime:Q,expirationTime:F,sortIndex:-1},Q>P?(D.sortIndex=Q,n(y,D),s(m)===null&&D===s(y)&&(C?(k(B),B=-1):C=!0,re(X,Q-P))):(D.sortIndex=F,n(m,D),E||N||(E=!0,Y||(Y=!0,te()))),D},r.unstable_shouldYield=K,r.unstable_wrapCallback=function(D){var O=x;return function(){var Q=x;x=O;try{return D.apply(this,arguments)}finally{x=Q}}}})(ec)),ec}var Th;function Wg(){return Th||(Th=1,Fr.exports=$g()),Fr.exports}/**
 * @license React
 * react-dom-client.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Eh;function Pg(){if(Eh)return so;Eh=1;var r=Wg(),n=Ph(),s=Dg();function u(e){var t="https://react.dev/errors/"+e;if(1<arguments.length){t+="?args[]="+encodeURIComponent(arguments[1]);for(var a=2;a<arguments.length;a++)t+="&args[]="+encodeURIComponent(arguments[a])}return"Minified React error #"+e+"; visit "+t+" for the full message or use the non-minified dev environment for full errors and additional helpful warnings."}function p(e){return!(!e||e.nodeType!==1&&e.nodeType!==9&&e.nodeType!==11)}function h(e){var t=e,a=e;if(e.alternate)for(;t.return;)t=t.return;else{e=t;do t=e,(t.flags&4098)!==0&&(a=t.return),e=t.return;while(e)}return t.tag===3?a:null}function b(e){if(e.tag===13){var t=e.memoizedState;if(t===null&&(e=e.alternate,e!==null&&(t=e.memoizedState)),t!==null)return t.dehydrated}return null}function g(e){if(e.tag===31){var t=e.memoizedState;if(t===null&&(e=e.alternate,e!==null&&(t=e.memoizedState)),t!==null)return t.dehydrated}return null}function m(e){if(h(e)!==e)throw Error(u(188))}function y(e){var t=e.alternate;if(!t){if(t=h(e),t===null)throw Error(u(188));return t!==e?null:e}for(var a=e,l=t;;){var o=a.return;if(o===null)break;var i=o.alternate;if(i===null){if(l=o.return,l!==null){a=l;continue}break}if(o.child===i.child){for(i=o.child;i;){if(i===a)return m(o),e;if(i===l)return m(o),t;i=i.sibling}throw Error(u(188))}if(a.return!==l.return)a=o,l=i;else{for(var c=!1,d=o.child;d;){if(d===a){c=!0,a=o,l=i;break}if(d===l){c=!0,l=o,a=i;break}d=d.sibling}if(!c){for(d=i.child;d;){if(d===a){c=!0,a=i,l=o;break}if(d===l){c=!0,l=i,a=o;break}d=d.sibling}if(!c)throw Error(u(189))}}if(a.alternate!==l)throw Error(u(190))}if(a.tag!==3)throw Error(u(188));return a.stateNode.current===a?e:t}function w(e){var t=e.tag;if(t===5||t===26||t===27||t===6)return e;for(e=e.child;e!==null;){if(t=w(e),t!==null)return t;e=e.sibling}return null}var v=Object.assign,x=Symbol.for("react.element"),N=Symbol.for("react.transitional.element"),E=Symbol.for("react.portal"),C=Symbol.for("react.fragment"),_=Symbol.for("react.strict_mode"),j=Symbol.for("react.profiler"),k=Symbol.for("react.consumer"),H=Symbol.for("react.context"),L=Symbol.for("react.forward_ref"),X=Symbol.for("react.suspense"),Y=Symbol.for("react.suspense_list"),B=Symbol.for("react.memo"),I=Symbol.for("react.lazy"),V=Symbol.for("react.activity"),K=Symbol.for("react.memo_cache_sentinel"),$=Symbol.iterator;function te(e){return e===null||typeof e!="object"?null:(e=$&&e[$]||e["@@iterator"],typeof e=="function"?e:null)}var ue=Symbol.for("react.client.reference");function he(e){if(e==null)return null;if(typeof e=="function")return e.$$typeof===ue?null:e.displayName||e.name||null;if(typeof e=="string")return e;switch(e){case C:return"Fragment";case j:return"Profiler";case _:return"StrictMode";case X:return"Suspense";case Y:return"SuspenseList";case V:return"Activity"}if(typeof e=="object")switch(e.$$typeof){case E:return"Portal";case H:return e.displayName||"Context";case k:return(e._context.displayName||"Context")+".Consumer";case L:var t=e.render;return e=e.displayName,e||(e=t.displayName||t.name||"",e=e!==""?"ForwardRef("+e+")":"ForwardRef"),e;case B:return t=e.displayName||null,t!==null?t:he(e.type)||"Memo";case I:t=e._payload,e=e._init;try{return he(e(t))}catch{}}return null}var re=Array.isArray,D=n.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,O=s.__DOM_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,Q={pending:!1,data:null,method:null,action:null},P=[],F=-1;function ie(e){return{current:e}}function oe(e){0>F||(e.current=P[F],P[F]=null,F--)}function ee(e,t){F++,P[F]=e.current,e.current=t}var ye=ie(null),se=ie(null),ge=ie(null),je=ie(null);function He(e,t){switch(ee(ge,t),ee(se,e),ee(ye,null),t.nodeType){case 9:case 11:e=(e=t.documentElement)&&(e=e.namespaceURI)?Jp(e):0;break;default:if(e=t.tagName,t=t.namespaceURI)t=Jp(t),e=Ip(t,e);else switch(e){case"svg":e=1;break;case"math":e=2;break;default:e=0}}oe(ye),ee(ye,e)}function we(){oe(ye),oe(se),oe(ge)}function ae(e){e.memoizedState!==null&&ee(je,e);var t=ye.current,a=Ip(t,e.type);t!==a&&(ee(se,e),ee(ye,a))}function de(e){se.current===e&&(oe(ye),oe(se)),je.current===e&&(oe(je),ao._currentValue=Q)}var fe,ze;function W(e){if(fe===void 0)try{throw Error()}catch(a){var t=a.stack.trim().match(/\n( *(at )?)/);fe=t&&t[1]||"",ze=-1<a.stack.indexOf(`
    at`)?" (<anonymous>)":-1<a.stack.indexOf("@")?"@unknown:0:0":""}return`
`+fe+e+ze}var be=!1;function Te(e,t){if(!e||be)return"";be=!0;var a=Error.prepareStackTrace;Error.prepareStackTrace=void 0;try{var l={DetermineComponentFrameRoot:function(){try{if(t){var Z=function(){throw Error()};if(Object.defineProperty(Z.prototype,"props",{set:function(){throw Error()}}),typeof Reflect=="object"&&Reflect.construct){try{Reflect.construct(Z,[])}catch(U){var G=U}Reflect.construct(e,[],Z)}else{try{Z.call()}catch(U){G=U}e.call(Z.prototype)}}else{try{throw Error()}catch(U){G=U}(Z=e())&&typeof Z.catch=="function"&&Z.catch(function(){})}}catch(U){if(U&&G&&typeof U.stack=="string")return[U.stack,G.stack]}return[null,null]}};l.DetermineComponentFrameRoot.displayName="DetermineComponentFrameRoot";var o=Object.getOwnPropertyDescriptor(l.DetermineComponentFrameRoot,"name");o&&o.configurable&&Object.defineProperty(l.DetermineComponentFrameRoot,"name",{value:"DetermineComponentFrameRoot"});var i=l.DetermineComponentFrameRoot(),c=i[0],d=i[1];if(c&&d){var S=c.split(`
`),z=d.split(`
`);for(o=l=0;l<S.length&&!S[l].includes("DetermineComponentFrameRoot");)l++;for(;o<z.length&&!z[o].includes("DetermineComponentFrameRoot");)o++;if(l===S.length||o===z.length)for(l=S.length-1,o=z.length-1;1<=l&&0<=o&&S[l]!==z[o];)o--;for(;1<=l&&0<=o;l--,o--)if(S[l]!==z[o]){if(l!==1||o!==1)do if(l--,o--,0>o||S[l]!==z[o]){var q=`
`+S[l].replace(" at new "," at ");return e.displayName&&q.includes("<anonymous>")&&(q=q.replace("<anonymous>",e.displayName)),q}while(1<=l&&0<=o);break}}}finally{be=!1,Error.prepareStackTrace=a}return(a=e?e.displayName||e.name:"")?W(a):""}function me(e,t){switch(e.tag){case 26:case 27:case 5:return W(e.type);case 16:return W("Lazy");case 13:return e.child!==t&&t!==null?W("Suspense Fallback"):W("Suspense");case 19:return W("SuspenseList");case 0:case 15:return Te(e.type,!1);case 11:return Te(e.type.render,!1);case 1:return Te(e.type,!0);case 31:return W("Activity");default:return""}}function Oe(e){try{var t="",a=null;do t+=me(e,a),a=e,e=e.return;while(e);return t}catch(l){return`
Error generating stack: `+l.message+`
`+l.stack}}var Ge=Object.prototype.hasOwnProperty,dt=r.unstable_scheduleCallback,Fe=r.unstable_cancelCallback,ct=r.unstable_shouldYield,sa=r.unstable_requestPaint,ve=r.unstable_now,hn=r.unstable_getCurrentPriorityLevel,fo=r.unstable_ImmediatePriority,mo=r.unstable_UserBlockingPriority,ra=r.unstable_NormalPriority,Bi=r.unstable_LowPriority,go=r.unstable_IdlePriority,Hi=r.log,Gi=r.unstable_setDisableYieldValue,ca=null,xt=null;function on(e){if(typeof Hi=="function"&&Gi(e),xt&&typeof xt.setStrictMode=="function")try{xt.setStrictMode(ca,e)}catch{}}var wt=Math.clz32?Math.clz32:Et,Ui=Math.log,De=Math.LN2;function Et(e){return e>>>=0,e===0?32:31-(Ui(e)/De|0)|0}var et=256,Rn=262144,ja=4194304;function ua(e){var t=e&42;if(t!==0)return t;switch(e&-e){case 1:return 1;case 2:return 2;case 4:return 4;case 8:return 8;case 16:return 16;case 32:return 32;case 64:return 64;case 128:return 128;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:return e&261888;case 262144:case 524288:case 1048576:case 2097152:return e&3932160;case 4194304:case 8388608:case 16777216:case 33554432:return e&62914560;case 67108864:return 67108864;case 134217728:return 134217728;case 268435456:return 268435456;case 536870912:return 536870912;case 1073741824:return 0;default:return e}}function yo(e,t,a){var l=e.pendingLanes;if(l===0)return 0;var o=0,i=e.suspendedLanes,c=e.pingedLanes;e=e.warmLanes;var d=l&134217727;return d!==0?(l=d&~i,l!==0?o=ua(l):(c&=d,c!==0?o=ua(c):a||(a=d&~e,a!==0&&(o=ua(a))))):(d=l&~i,d!==0?o=ua(d):c!==0?o=ua(c):a||(a=l&~e,a!==0&&(o=ua(a)))),o===0?0:t!==0&&t!==o&&(t&i)===0&&(i=o&-o,a=t&-t,i>=a||i===32&&(a&4194048)!==0)?t:o}function gl(e,t){return(e.pendingLanes&~(e.suspendedLanes&~e.pingedLanes)&t)===0}function yf(e,t){switch(e){case 1:case 2:case 4:case 8:case 64:return t+250;case 16:case 32:case 128:case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:return t+5e3;case 4194304:case 8388608:case 16777216:case 33554432:return-1;case 67108864:case 134217728:case 268435456:case 536870912:case 1073741824:return-1;default:return-1}}function Ec(){var e=ja;return ja<<=1,(ja&62914560)===0&&(ja=4194304),e}function Li(e){for(var t=[],a=0;31>a;a++)t.push(e);return t}function yl(e,t){e.pendingLanes|=t,t!==268435456&&(e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0)}function bf(e,t,a,l,o,i){var c=e.pendingLanes;e.pendingLanes=a,e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0,e.expiredLanes&=a,e.entangledLanes&=a,e.errorRecoveryDisabledLanes&=a,e.shellSuspendCounter=0;var d=e.entanglements,S=e.expirationTimes,z=e.hiddenUpdates;for(a=c&~a;0<a;){var q=31-wt(a),Z=1<<q;d[q]=0,S[q]=-1;var G=z[q];if(G!==null)for(z[q]=null,q=0;q<G.length;q++){var U=G[q];U!==null&&(U.lane&=-536870913)}a&=~Z}l!==0&&Ac(e,l,0),i!==0&&o===0&&e.tag!==0&&(e.suspendedLanes|=i&~(c&~t))}function Ac(e,t,a){e.pendingLanes|=t,e.suspendedLanes&=~t;var l=31-wt(t);e.entangledLanes|=t,e.entanglements[l]=e.entanglements[l]|1073741824|a&261930}function Nc(e,t){var a=e.entangledLanes|=t;for(e=e.entanglements;a;){var l=31-wt(a),o=1<<l;o&t|e[l]&t&&(e[l]|=t),a&=~o}}function Cc(e,t){var a=t&-t;return a=(a&42)!==0?1:Yi(a),(a&(e.suspendedLanes|t))!==0?0:a}function Yi(e){switch(e){case 2:e=1;break;case 8:e=4;break;case 32:e=16;break;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:case 4194304:case 8388608:case 16777216:case 33554432:e=128;break;case 268435456:e=134217728;break;default:e=0}return e}function qi(e){return e&=-e,2<e?8<e?(e&134217727)!==0?32:268435456:8:2}function jc(){var e=O.p;return e!==0?e:(e=window.event,e===void 0?32:fh(e.type))}function Mc(e,t){var a=O.p;try{return O.p=e,t()}finally{O.p=a}}var zn=Math.random().toString(36).slice(2),mt="__reactFiber$"+zn,At="__reactProps$"+zn,Ma="__reactContainer$"+zn,Xi="__reactEvents$"+zn,vf="__reactListeners$"+zn,_f="__reactHandles$"+zn,Dc="__reactResources$"+zn,bl="__reactMarker$"+zn;function Ji(e){delete e[mt],delete e[At],delete e[Xi],delete e[vf],delete e[_f]}function Da(e){var t=e[mt];if(t)return t;for(var a=e.parentNode;a;){if(t=a[Ma]||a[mt]){if(a=t.alternate,t.child!==null||a!==null&&a.child!==null)for(e=Pp(e);e!==null;){if(a=e[mt])return a;e=Pp(e)}return t}e=a,a=e.parentNode}return null}function Oa(e){if(e=e[mt]||e[Ma]){var t=e.tag;if(t===5||t===6||t===13||t===31||t===26||t===27||t===3)return e}return null}function vl(e){var t=e.tag;if(t===5||t===26||t===27||t===6)return e.stateNode;throw Error(u(33))}function ka(e){var t=e[Dc];return t||(t=e[Dc]={hoistableStyles:new Map,hoistableScripts:new Map}),t}function pt(e){e[bl]=!0}var Oc=new Set,kc={};function da(e,t){Ra(e,t),Ra(e+"Capture",t)}function Ra(e,t){for(kc[e]=t,e=0;e<t.length;e++)Oc.add(t[e])}var Sf=RegExp("^[:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD][:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD\\-.0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040]*$"),Rc={},zc={};function xf(e){return Ge.call(zc,e)?!0:Ge.call(Rc,e)?!1:Sf.test(e)?zc[e]=!0:(Rc[e]=!0,!1)}function bo(e,t,a){if(xf(t))if(a===null)e.removeAttribute(t);else{switch(typeof a){case"undefined":case"function":case"symbol":e.removeAttribute(t);return;case"boolean":var l=t.toLowerCase().slice(0,5);if(l!=="data-"&&l!=="aria-"){e.removeAttribute(t);return}}e.setAttribute(t,""+a)}}function vo(e,t,a){if(a===null)e.removeAttribute(t);else{switch(typeof a){case"undefined":case"function":case"symbol":case"boolean":e.removeAttribute(t);return}e.setAttribute(t,""+a)}}function fn(e,t,a,l){if(l===null)e.removeAttribute(a);else{switch(typeof l){case"undefined":case"function":case"symbol":case"boolean":e.removeAttribute(a);return}e.setAttributeNS(t,a,""+l)}}function Xt(e){switch(typeof e){case"bigint":case"boolean":case"number":case"string":case"undefined":return e;case"object":return e;default:return""}}function Bc(e){var t=e.type;return(e=e.nodeName)&&e.toLowerCase()==="input"&&(t==="checkbox"||t==="radio")}function wf(e,t,a){var l=Object.getOwnPropertyDescriptor(e.constructor.prototype,t);if(!e.hasOwnProperty(t)&&typeof l<"u"&&typeof l.get=="function"&&typeof l.set=="function"){var o=l.get,i=l.set;return Object.defineProperty(e,t,{configurable:!0,get:function(){return o.call(this)},set:function(c){a=""+c,i.call(this,c)}}),Object.defineProperty(e,t,{enumerable:l.enumerable}),{getValue:function(){return a},setValue:function(c){a=""+c},stopTracking:function(){e._valueTracker=null,delete e[t]}}}}function Ii(e){if(!e._valueTracker){var t=Bc(e)?"checked":"value";e._valueTracker=wf(e,t,""+e[t])}}function Hc(e){if(!e)return!1;var t=e._valueTracker;if(!t)return!0;var a=t.getValue(),l="";return e&&(l=Bc(e)?e.checked?"true":"false":e.value),e=l,e!==a?(t.setValue(e),!0):!1}function _o(e){if(e=e||(typeof document<"u"?document:void 0),typeof e>"u")return null;try{return e.activeElement||e.body}catch{return e.body}}var Tf=/[\n"\\]/g;function Jt(e){return e.replace(Tf,function(t){return"\\"+t.charCodeAt(0).toString(16)+" "})}function Zi(e,t,a,l,o,i,c,d){e.name="",c!=null&&typeof c!="function"&&typeof c!="symbol"&&typeof c!="boolean"?e.type=c:e.removeAttribute("type"),t!=null?c==="number"?(t===0&&e.value===""||e.value!=t)&&(e.value=""+Xt(t)):e.value!==""+Xt(t)&&(e.value=""+Xt(t)):c!=="submit"&&c!=="reset"||e.removeAttribute("value"),t!=null?Qi(e,c,Xt(t)):a!=null?Qi(e,c,Xt(a)):l!=null&&e.removeAttribute("value"),o==null&&i!=null&&(e.defaultChecked=!!i),o!=null&&(e.checked=o&&typeof o!="function"&&typeof o!="symbol"),d!=null&&typeof d!="function"&&typeof d!="symbol"&&typeof d!="boolean"?e.name=""+Xt(d):e.removeAttribute("name")}function Gc(e,t,a,l,o,i,c,d){if(i!=null&&typeof i!="function"&&typeof i!="symbol"&&typeof i!="boolean"&&(e.type=i),t!=null||a!=null){if(!(i!=="submit"&&i!=="reset"||t!=null)){Ii(e);return}a=a!=null?""+Xt(a):"",t=t!=null?""+Xt(t):a,d||t===e.value||(e.value=t),e.defaultValue=t}l=l??o,l=typeof l!="function"&&typeof l!="symbol"&&!!l,e.checked=d?e.checked:!!l,e.defaultChecked=!!l,c!=null&&typeof c!="function"&&typeof c!="symbol"&&typeof c!="boolean"&&(e.name=c),Ii(e)}function Qi(e,t,a){t==="number"&&_o(e.ownerDocument)===e||e.defaultValue===""+a||(e.defaultValue=""+a)}function za(e,t,a,l){if(e=e.options,t){t={};for(var o=0;o<a.length;o++)t["$"+a[o]]=!0;for(a=0;a<e.length;a++)o=t.hasOwnProperty("$"+e[a].value),e[a].selected!==o&&(e[a].selected=o),o&&l&&(e[a].defaultSelected=!0)}else{for(a=""+Xt(a),t=null,o=0;o<e.length;o++){if(e[o].value===a){e[o].selected=!0,l&&(e[o].defaultSelected=!0);return}t!==null||e[o].disabled||(t=e[o])}t!==null&&(t.selected=!0)}}function Uc(e,t,a){if(t!=null&&(t=""+Xt(t),t!==e.value&&(e.value=t),a==null)){e.defaultValue!==t&&(e.defaultValue=t);return}e.defaultValue=a!=null?""+Xt(a):""}function Lc(e,t,a,l){if(t==null){if(l!=null){if(a!=null)throw Error(u(92));if(re(l)){if(1<l.length)throw Error(u(93));l=l[0]}a=l}a==null&&(a=""),t=a}a=Xt(t),e.defaultValue=a,l=e.textContent,l===a&&l!==""&&l!==null&&(e.value=l),Ii(e)}function Ba(e,t){if(t){var a=e.firstChild;if(a&&a===e.lastChild&&a.nodeType===3){a.nodeValue=t;return}}e.textContent=t}var Ef=new Set("animationIterationCount aspectRatio borderImageOutset borderImageSlice borderImageWidth boxFlex boxFlexGroup boxOrdinalGroup columnCount columns flex flexGrow flexPositive flexShrink flexNegative flexOrder gridArea gridRow gridRowEnd gridRowSpan gridRowStart gridColumn gridColumnEnd gridColumnSpan gridColumnStart fontWeight lineClamp lineHeight opacity order orphans scale tabSize widows zIndex zoom fillOpacity floodOpacity stopOpacity strokeDasharray strokeDashoffset strokeMiterlimit strokeOpacity strokeWidth MozAnimationIterationCount MozBoxFlex MozBoxFlexGroup MozLineClamp msAnimationIterationCount msFlex msZoom msFlexGrow msFlexNegative msFlexOrder msFlexPositive msFlexShrink msGridColumn msGridColumnSpan msGridRow msGridRowSpan WebkitAnimationIterationCount WebkitBoxFlex WebKitBoxFlexGroup WebkitBoxOrdinalGroup WebkitColumnCount WebkitColumns WebkitFlex WebkitFlexGrow WebkitFlexPositive WebkitFlexShrink WebkitLineClamp".split(" "));function Yc(e,t,a){var l=t.indexOf("--")===0;a==null||typeof a=="boolean"||a===""?l?e.setProperty(t,""):t==="float"?e.cssFloat="":e[t]="":l?e.setProperty(t,a):typeof a!="number"||a===0||Ef.has(t)?t==="float"?e.cssFloat=a:e[t]=(""+a).trim():e[t]=a+"px"}function qc(e,t,a){if(t!=null&&typeof t!="object")throw Error(u(62));if(e=e.style,a!=null){for(var l in a)!a.hasOwnProperty(l)||t!=null&&t.hasOwnProperty(l)||(l.indexOf("--")===0?e.setProperty(l,""):l==="float"?e.cssFloat="":e[l]="");for(var o in t)l=t[o],t.hasOwnProperty(o)&&a[o]!==l&&Yc(e,o,l)}else for(var i in t)t.hasOwnProperty(i)&&Yc(e,i,t[i])}function Vi(e){if(e.indexOf("-")===-1)return!1;switch(e){case"annotation-xml":case"color-profile":case"font-face":case"font-face-src":case"font-face-uri":case"font-face-format":case"font-face-name":case"missing-glyph":return!1;default:return!0}}var Af=new Map([["acceptCharset","accept-charset"],["htmlFor","for"],["httpEquiv","http-equiv"],["crossOrigin","crossorigin"],["accentHeight","accent-height"],["alignmentBaseline","alignment-baseline"],["arabicForm","arabic-form"],["baselineShift","baseline-shift"],["capHeight","cap-height"],["clipPath","clip-path"],["clipRule","clip-rule"],["colorInterpolation","color-interpolation"],["colorInterpolationFilters","color-interpolation-filters"],["colorProfile","color-profile"],["colorRendering","color-rendering"],["dominantBaseline","dominant-baseline"],["enableBackground","enable-background"],["fillOpacity","fill-opacity"],["fillRule","fill-rule"],["floodColor","flood-color"],["floodOpacity","flood-opacity"],["fontFamily","font-family"],["fontSize","font-size"],["fontSizeAdjust","font-size-adjust"],["fontStretch","font-stretch"],["fontStyle","font-style"],["fontVariant","font-variant"],["fontWeight","font-weight"],["glyphName","glyph-name"],["glyphOrientationHorizontal","glyph-orientation-horizontal"],["glyphOrientationVertical","glyph-orientation-vertical"],["horizAdvX","horiz-adv-x"],["horizOriginX","horiz-origin-x"],["imageRendering","image-rendering"],["letterSpacing","letter-spacing"],["lightingColor","lighting-color"],["markerEnd","marker-end"],["markerMid","marker-mid"],["markerStart","marker-start"],["overlinePosition","overline-position"],["overlineThickness","overline-thickness"],["paintOrder","paint-order"],["panose-1","panose-1"],["pointerEvents","pointer-events"],["renderingIntent","rendering-intent"],["shapeRendering","shape-rendering"],["stopColor","stop-color"],["stopOpacity","stop-opacity"],["strikethroughPosition","strikethrough-position"],["strikethroughThickness","strikethrough-thickness"],["strokeDasharray","stroke-dasharray"],["strokeDashoffset","stroke-dashoffset"],["strokeLinecap","stroke-linecap"],["strokeLinejoin","stroke-linejoin"],["strokeMiterlimit","stroke-miterlimit"],["strokeOpacity","stroke-opacity"],["strokeWidth","stroke-width"],["textAnchor","text-anchor"],["textDecoration","text-decoration"],["textRendering","text-rendering"],["transformOrigin","transform-origin"],["underlinePosition","underline-position"],["underlineThickness","underline-thickness"],["unicodeBidi","unicode-bidi"],["unicodeRange","unicode-range"],["unitsPerEm","units-per-em"],["vAlphabetic","v-alphabetic"],["vHanging","v-hanging"],["vIdeographic","v-ideographic"],["vMathematical","v-mathematical"],["vectorEffect","vector-effect"],["vertAdvY","vert-adv-y"],["vertOriginX","vert-origin-x"],["vertOriginY","vert-origin-y"],["wordSpacing","word-spacing"],["writingMode","writing-mode"],["xmlnsXlink","xmlns:xlink"],["xHeight","x-height"]]),Nf=/^[\u0000-\u001F ]*j[\r\n\t]*a[\r\n\t]*v[\r\n\t]*a[\r\n\t]*s[\r\n\t]*c[\r\n\t]*r[\r\n\t]*i[\r\n\t]*p[\r\n\t]*t[\r\n\t]*:/i;function So(e){return Nf.test(""+e)?"javascript:throw new Error('React has blocked a javascript: URL as a security precaution.')":e}function mn(){}var Ki=null;function $i(e){return e=e.target||e.srcElement||window,e.correspondingUseElement&&(e=e.correspondingUseElement),e.nodeType===3?e.parentNode:e}var Ha=null,Ga=null;function Xc(e){var t=Oa(e);if(t&&(e=t.stateNode)){var a=e[At]||null;e:switch(e=t.stateNode,t.type){case"input":if(Zi(e,a.value,a.defaultValue,a.defaultValue,a.checked,a.defaultChecked,a.type,a.name),t=a.name,a.type==="radio"&&t!=null){for(a=e;a.parentNode;)a=a.parentNode;for(a=a.querySelectorAll('input[name="'+Jt(""+t)+'"][type="radio"]'),t=0;t<a.length;t++){var l=a[t];if(l!==e&&l.form===e.form){var o=l[At]||null;if(!o)throw Error(u(90));Zi(l,o.value,o.defaultValue,o.defaultValue,o.checked,o.defaultChecked,o.type,o.name)}}for(t=0;t<a.length;t++)l=a[t],l.form===e.form&&Hc(l)}break e;case"textarea":Uc(e,a.value,a.defaultValue);break e;case"select":t=a.value,t!=null&&za(e,!!a.multiple,t,!1)}}}var Wi=!1;function Jc(e,t,a){if(Wi)return e(t,a);Wi=!0;try{var l=e(t);return l}finally{if(Wi=!1,(Ha!==null||Ga!==null)&&(ri(),Ha&&(t=Ha,e=Ga,Ga=Ha=null,Xc(t),e)))for(t=0;t<e.length;t++)Xc(e[t])}}function _l(e,t){var a=e.stateNode;if(a===null)return null;var l=a[At]||null;if(l===null)return null;a=l[t];e:switch(t){case"onClick":case"onClickCapture":case"onDoubleClick":case"onDoubleClickCapture":case"onMouseDown":case"onMouseDownCapture":case"onMouseMove":case"onMouseMoveCapture":case"onMouseUp":case"onMouseUpCapture":case"onMouseEnter":(l=!l.disabled)||(e=e.type,l=!(e==="button"||e==="input"||e==="select"||e==="textarea")),e=!l;break e;default:e=!1}if(e)return null;if(a&&typeof a!="function")throw Error(u(231,t,typeof a));return a}var gn=!(typeof window>"u"||typeof window.document>"u"||typeof window.document.createElement>"u"),Pi=!1;if(gn)try{var Sl={};Object.defineProperty(Sl,"passive",{get:function(){Pi=!0}}),window.addEventListener("test",Sl,Sl),window.removeEventListener("test",Sl,Sl)}catch{Pi=!1}var Bn=null,Fi=null,xo=null;function Ic(){if(xo)return xo;var e,t=Fi,a=t.length,l,o="value"in Bn?Bn.value:Bn.textContent,i=o.length;for(e=0;e<a&&t[e]===o[e];e++);var c=a-e;for(l=1;l<=c&&t[a-l]===o[i-l];l++);return xo=o.slice(e,1<l?1-l:void 0)}function wo(e){var t=e.keyCode;return"charCode"in e?(e=e.charCode,e===0&&t===13&&(e=13)):e=t,e===10&&(e=13),32<=e||e===13?e:0}function To(){return!0}function Zc(){return!1}function Nt(e){function t(a,l,o,i,c){this._reactName=a,this._targetInst=o,this.type=l,this.nativeEvent=i,this.target=c,this.currentTarget=null;for(var d in e)e.hasOwnProperty(d)&&(a=e[d],this[d]=a?a(i):i[d]);return this.isDefaultPrevented=(i.defaultPrevented!=null?i.defaultPrevented:i.returnValue===!1)?To:Zc,this.isPropagationStopped=Zc,this}return v(t.prototype,{preventDefault:function(){this.defaultPrevented=!0;var a=this.nativeEvent;a&&(a.preventDefault?a.preventDefault():typeof a.returnValue!="unknown"&&(a.returnValue=!1),this.isDefaultPrevented=To)},stopPropagation:function(){var a=this.nativeEvent;a&&(a.stopPropagation?a.stopPropagation():typeof a.cancelBubble!="unknown"&&(a.cancelBubble=!0),this.isPropagationStopped=To)},persist:function(){},isPersistent:To}),t}var pa={eventPhase:0,bubbles:0,cancelable:0,timeStamp:function(e){return e.timeStamp||Date.now()},defaultPrevented:0,isTrusted:0},Eo=Nt(pa),xl=v({},pa,{view:0,detail:0}),Cf=Nt(xl),es,ts,wl,Ao=v({},xl,{screenX:0,screenY:0,clientX:0,clientY:0,pageX:0,pageY:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,getModifierState:as,button:0,buttons:0,relatedTarget:function(e){return e.relatedTarget===void 0?e.fromElement===e.srcElement?e.toElement:e.fromElement:e.relatedTarget},movementX:function(e){return"movementX"in e?e.movementX:(e!==wl&&(wl&&e.type==="mousemove"?(es=e.screenX-wl.screenX,ts=e.screenY-wl.screenY):ts=es=0,wl=e),es)},movementY:function(e){return"movementY"in e?e.movementY:ts}}),Qc=Nt(Ao),jf=v({},Ao,{dataTransfer:0}),Mf=Nt(jf),Df=v({},xl,{relatedTarget:0}),ns=Nt(Df),Of=v({},pa,{animationName:0,elapsedTime:0,pseudoElement:0}),kf=Nt(Of),Rf=v({},pa,{clipboardData:function(e){return"clipboardData"in e?e.clipboardData:window.clipboardData}}),zf=Nt(Rf),Bf=v({},pa,{data:0}),Vc=Nt(Bf),Hf={Esc:"Escape",Spacebar:" ",Left:"ArrowLeft",Up:"ArrowUp",Right:"ArrowRight",Down:"ArrowDown",Del:"Delete",Win:"OS",Menu:"ContextMenu",Apps:"ContextMenu",Scroll:"ScrollLock",MozPrintableKey:"Unidentified"},Gf={8:"Backspace",9:"Tab",12:"Clear",13:"Enter",16:"Shift",17:"Control",18:"Alt",19:"Pause",20:"CapsLock",27:"Escape",32:" ",33:"PageUp",34:"PageDown",35:"End",36:"Home",37:"ArrowLeft",38:"ArrowUp",39:"ArrowRight",40:"ArrowDown",45:"Insert",46:"Delete",112:"F1",113:"F2",114:"F3",115:"F4",116:"F5",117:"F6",118:"F7",119:"F8",120:"F9",121:"F10",122:"F11",123:"F12",144:"NumLock",145:"ScrollLock",224:"Meta"},Uf={Alt:"altKey",Control:"ctrlKey",Meta:"metaKey",Shift:"shiftKey"};function Lf(e){var t=this.nativeEvent;return t.getModifierState?t.getModifierState(e):(e=Uf[e])?!!t[e]:!1}function as(){return Lf}var Yf=v({},xl,{key:function(e){if(e.key){var t=Hf[e.key]||e.key;if(t!=="Unidentified")return t}return e.type==="keypress"?(e=wo(e),e===13?"Enter":String.fromCharCode(e)):e.type==="keydown"||e.type==="keyup"?Gf[e.keyCode]||"Unidentified":""},code:0,location:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,repeat:0,locale:0,getModifierState:as,charCode:function(e){return e.type==="keypress"?wo(e):0},keyCode:function(e){return e.type==="keydown"||e.type==="keyup"?e.keyCode:0},which:function(e){return e.type==="keypress"?wo(e):e.type==="keydown"||e.type==="keyup"?e.keyCode:0}}),qf=Nt(Yf),Xf=v({},Ao,{pointerId:0,width:0,height:0,pressure:0,tangentialPressure:0,tiltX:0,tiltY:0,twist:0,pointerType:0,isPrimary:0}),Kc=Nt(Xf),Jf=v({},xl,{touches:0,targetTouches:0,changedTouches:0,altKey:0,metaKey:0,ctrlKey:0,shiftKey:0,getModifierState:as}),If=Nt(Jf),Zf=v({},pa,{propertyName:0,elapsedTime:0,pseudoElement:0}),Qf=Nt(Zf),Vf=v({},Ao,{deltaX:function(e){return"deltaX"in e?e.deltaX:"wheelDeltaX"in e?-e.wheelDeltaX:0},deltaY:function(e){return"deltaY"in e?e.deltaY:"wheelDeltaY"in e?-e.wheelDeltaY:"wheelDelta"in e?-e.wheelDelta:0},deltaZ:0,deltaMode:0}),Kf=Nt(Vf),$f=v({},pa,{newState:0,oldState:0}),Wf=Nt($f),Pf=[9,13,27,32],ls=gn&&"CompositionEvent"in window,Tl=null;gn&&"documentMode"in document&&(Tl=document.documentMode);var Ff=gn&&"TextEvent"in window&&!Tl,$c=gn&&(!ls||Tl&&8<Tl&&11>=Tl),Wc=" ",Pc=!1;function Fc(e,t){switch(e){case"keyup":return Pf.indexOf(t.keyCode)!==-1;case"keydown":return t.keyCode!==229;case"keypress":case"mousedown":case"focusout":return!0;default:return!1}}function eu(e){return e=e.detail,typeof e=="object"&&"data"in e?e.data:null}var Ua=!1;function em(e,t){switch(e){case"compositionend":return eu(t);case"keypress":return t.which!==32?null:(Pc=!0,Wc);case"textInput":return e=t.data,e===Wc&&Pc?null:e;default:return null}}function tm(e,t){if(Ua)return e==="compositionend"||!ls&&Fc(e,t)?(e=Ic(),xo=Fi=Bn=null,Ua=!1,e):null;switch(e){case"paste":return null;case"keypress":if(!(t.ctrlKey||t.altKey||t.metaKey)||t.ctrlKey&&t.altKey){if(t.char&&1<t.char.length)return t.char;if(t.which)return String.fromCharCode(t.which)}return null;case"compositionend":return $c&&t.locale!=="ko"?null:t.data;default:return null}}var nm={color:!0,date:!0,datetime:!0,"datetime-local":!0,email:!0,month:!0,number:!0,password:!0,range:!0,search:!0,tel:!0,text:!0,time:!0,url:!0,week:!0};function tu(e){var t=e&&e.nodeName&&e.nodeName.toLowerCase();return t==="input"?!!nm[e.type]:t==="textarea"}function nu(e,t,a,l){Ha?Ga?Ga.push(l):Ga=[l]:Ha=l,t=mi(t,"onChange"),0<t.length&&(a=new Eo("onChange","change",null,a,l),e.push({event:a,listeners:t}))}var El=null,Al=null;function am(e){Gp(e,0)}function No(e){var t=vl(e);if(Hc(t))return e}function au(e,t){if(e==="change")return t}var lu=!1;if(gn){var os;if(gn){var is="oninput"in document;if(!is){var ou=document.createElement("div");ou.setAttribute("oninput","return;"),is=typeof ou.oninput=="function"}os=is}else os=!1;lu=os&&(!document.documentMode||9<document.documentMode)}function iu(){El&&(El.detachEvent("onpropertychange",su),Al=El=null)}function su(e){if(e.propertyName==="value"&&No(Al)){var t=[];nu(t,Al,e,$i(e)),Jc(am,t)}}function lm(e,t,a){e==="focusin"?(iu(),El=t,Al=a,El.attachEvent("onpropertychange",su)):e==="focusout"&&iu()}function om(e){if(e==="selectionchange"||e==="keyup"||e==="keydown")return No(Al)}function im(e,t){if(e==="click")return No(t)}function sm(e,t){if(e==="input"||e==="change")return No(t)}function rm(e,t){return e===t&&(e!==0||1/e===1/t)||e!==e&&t!==t}var Rt=typeof Object.is=="function"?Object.is:rm;function Nl(e,t){if(Rt(e,t))return!0;if(typeof e!="object"||e===null||typeof t!="object"||t===null)return!1;var a=Object.keys(e),l=Object.keys(t);if(a.length!==l.length)return!1;for(l=0;l<a.length;l++){var o=a[l];if(!Ge.call(t,o)||!Rt(e[o],t[o]))return!1}return!0}function ru(e){for(;e&&e.firstChild;)e=e.firstChild;return e}function cu(e,t){var a=ru(e);e=0;for(var l;a;){if(a.nodeType===3){if(l=e+a.textContent.length,e<=t&&l>=t)return{node:a,offset:t-e};e=l}e:{for(;a;){if(a.nextSibling){a=a.nextSibling;break e}a=a.parentNode}a=void 0}a=ru(a)}}function uu(e,t){return e&&t?e===t?!0:e&&e.nodeType===3?!1:t&&t.nodeType===3?uu(e,t.parentNode):"contains"in e?e.contains(t):e.compareDocumentPosition?!!(e.compareDocumentPosition(t)&16):!1:!1}function du(e){e=e!=null&&e.ownerDocument!=null&&e.ownerDocument.defaultView!=null?e.ownerDocument.defaultView:window;for(var t=_o(e.document);t instanceof e.HTMLIFrameElement;){try{var a=typeof t.contentWindow.location.href=="string"}catch{a=!1}if(a)e=t.contentWindow;else break;t=_o(e.document)}return t}function ss(e){var t=e&&e.nodeName&&e.nodeName.toLowerCase();return t&&(t==="input"&&(e.type==="text"||e.type==="search"||e.type==="tel"||e.type==="url"||e.type==="password")||t==="textarea"||e.contentEditable==="true")}var cm=gn&&"documentMode"in document&&11>=document.documentMode,La=null,rs=null,Cl=null,cs=!1;function pu(e,t,a){var l=a.window===a?a.document:a.nodeType===9?a:a.ownerDocument;cs||La==null||La!==_o(l)||(l=La,"selectionStart"in l&&ss(l)?l={start:l.selectionStart,end:l.selectionEnd}:(l=(l.ownerDocument&&l.ownerDocument.defaultView||window).getSelection(),l={anchorNode:l.anchorNode,anchorOffset:l.anchorOffset,focusNode:l.focusNode,focusOffset:l.focusOffset}),Cl&&Nl(Cl,l)||(Cl=l,l=mi(rs,"onSelect"),0<l.length&&(t=new Eo("onSelect","select",null,t,a),e.push({event:t,listeners:l}),t.target=La)))}function ha(e,t){var a={};return a[e.toLowerCase()]=t.toLowerCase(),a["Webkit"+e]="webkit"+t,a["Moz"+e]="moz"+t,a}var Ya={animationend:ha("Animation","AnimationEnd"),animationiteration:ha("Animation","AnimationIteration"),animationstart:ha("Animation","AnimationStart"),transitionrun:ha("Transition","TransitionRun"),transitionstart:ha("Transition","TransitionStart"),transitioncancel:ha("Transition","TransitionCancel"),transitionend:ha("Transition","TransitionEnd")},us={},hu={};gn&&(hu=document.createElement("div").style,"AnimationEvent"in window||(delete Ya.animationend.animation,delete Ya.animationiteration.animation,delete Ya.animationstart.animation),"TransitionEvent"in window||delete Ya.transitionend.transition);function fa(e){if(us[e])return us[e];if(!Ya[e])return e;var t=Ya[e],a;for(a in t)if(t.hasOwnProperty(a)&&a in hu)return us[e]=t[a];return e}var fu=fa("animationend"),mu=fa("animationiteration"),gu=fa("animationstart"),um=fa("transitionrun"),dm=fa("transitionstart"),pm=fa("transitioncancel"),yu=fa("transitionend"),bu=new Map,ds="abort auxClick beforeToggle cancel canPlay canPlayThrough click close contextMenu copy cut drag dragEnd dragEnter dragExit dragLeave dragOver dragStart drop durationChange emptied encrypted ended error gotPointerCapture input invalid keyDown keyPress keyUp load loadedData loadedMetadata loadStart lostPointerCapture mouseDown mouseMove mouseOut mouseOver mouseUp paste pause play playing pointerCancel pointerDown pointerMove pointerOut pointerOver pointerUp progress rateChange reset resize seeked seeking stalled submit suspend timeUpdate touchCancel touchEnd touchStart volumeChange scroll toggle touchMove waiting wheel".split(" ");ds.push("scrollEnd");function en(e,t){bu.set(e,t),da(t,[e])}var Co=typeof reportError=="function"?reportError:function(e){if(typeof window=="object"&&typeof window.ErrorEvent=="function"){var t=new window.ErrorEvent("error",{bubbles:!0,cancelable:!0,message:typeof e=="object"&&e!==null&&typeof e.message=="string"?String(e.message):String(e),error:e});if(!window.dispatchEvent(t))return}else if(typeof process=="object"&&typeof process.emit=="function"){process.emit("uncaughtException",e);return}console.error(e)},It=[],qa=0,ps=0;function jo(){for(var e=qa,t=ps=qa=0;t<e;){var a=It[t];It[t++]=null;var l=It[t];It[t++]=null;var o=It[t];It[t++]=null;var i=It[t];if(It[t++]=null,l!==null&&o!==null){var c=l.pending;c===null?o.next=o:(o.next=c.next,c.next=o),l.pending=o}i!==0&&vu(a,o,i)}}function Mo(e,t,a,l){It[qa++]=e,It[qa++]=t,It[qa++]=a,It[qa++]=l,ps|=l,e.lanes|=l,e=e.alternate,e!==null&&(e.lanes|=l)}function hs(e,t,a,l){return Mo(e,t,a,l),Do(e)}function ma(e,t){return Mo(e,null,null,t),Do(e)}function vu(e,t,a){e.lanes|=a;var l=e.alternate;l!==null&&(l.lanes|=a);for(var o=!1,i=e.return;i!==null;)i.childLanes|=a,l=i.alternate,l!==null&&(l.childLanes|=a),i.tag===22&&(e=i.stateNode,e===null||e._visibility&1||(o=!0)),e=i,i=i.return;return e.tag===3?(i=e.stateNode,o&&t!==null&&(o=31-wt(a),e=i.hiddenUpdates,l=e[o],l===null?e[o]=[t]:l.push(t),t.lane=a|536870912),i):null}function Do(e){if(50<$l)throw $l=0,xr=null,Error(u(185));for(var t=e.return;t!==null;)e=t,t=e.return;return e.tag===3?e.stateNode:null}var Xa={};function hm(e,t,a,l){this.tag=e,this.key=a,this.sibling=this.child=this.return=this.stateNode=this.type=this.elementType=null,this.index=0,this.refCleanup=this.ref=null,this.pendingProps=t,this.dependencies=this.memoizedState=this.updateQueue=this.memoizedProps=null,this.mode=l,this.subtreeFlags=this.flags=0,this.deletions=null,this.childLanes=this.lanes=0,this.alternate=null}function zt(e,t,a,l){return new hm(e,t,a,l)}function fs(e){return e=e.prototype,!(!e||!e.isReactComponent)}function yn(e,t){var a=e.alternate;return a===null?(a=zt(e.tag,t,e.key,e.mode),a.elementType=e.elementType,a.type=e.type,a.stateNode=e.stateNode,a.alternate=e,e.alternate=a):(a.pendingProps=t,a.type=e.type,a.flags=0,a.subtreeFlags=0,a.deletions=null),a.flags=e.flags&65011712,a.childLanes=e.childLanes,a.lanes=e.lanes,a.child=e.child,a.memoizedProps=e.memoizedProps,a.memoizedState=e.memoizedState,a.updateQueue=e.updateQueue,t=e.dependencies,a.dependencies=t===null?null:{lanes:t.lanes,firstContext:t.firstContext},a.sibling=e.sibling,a.index=e.index,a.ref=e.ref,a.refCleanup=e.refCleanup,a}function _u(e,t){e.flags&=65011714;var a=e.alternate;return a===null?(e.childLanes=0,e.lanes=t,e.child=null,e.subtreeFlags=0,e.memoizedProps=null,e.memoizedState=null,e.updateQueue=null,e.dependencies=null,e.stateNode=null):(e.childLanes=a.childLanes,e.lanes=a.lanes,e.child=a.child,e.subtreeFlags=0,e.deletions=null,e.memoizedProps=a.memoizedProps,e.memoizedState=a.memoizedState,e.updateQueue=a.updateQueue,e.type=a.type,t=a.dependencies,e.dependencies=t===null?null:{lanes:t.lanes,firstContext:t.firstContext}),e}function Oo(e,t,a,l,o,i){var c=0;if(l=e,typeof e=="function")fs(e)&&(c=1);else if(typeof e=="string")c=bg(e,a,ye.current)?26:e==="html"||e==="head"||e==="body"?27:5;else e:switch(e){case V:return e=zt(31,a,t,o),e.elementType=V,e.lanes=i,e;case C:return ga(a.children,o,i,t);case _:c=8,o|=24;break;case j:return e=zt(12,a,t,o|2),e.elementType=j,e.lanes=i,e;case X:return e=zt(13,a,t,o),e.elementType=X,e.lanes=i,e;case Y:return e=zt(19,a,t,o),e.elementType=Y,e.lanes=i,e;default:if(typeof e=="object"&&e!==null)switch(e.$$typeof){case H:c=10;break e;case k:c=9;break e;case L:c=11;break e;case B:c=14;break e;case I:c=16,l=null;break e}c=29,a=Error(u(130,e===null?"null":typeof e,"")),l=null}return t=zt(c,a,t,o),t.elementType=e,t.type=l,t.lanes=i,t}function ga(e,t,a,l){return e=zt(7,e,l,t),e.lanes=a,e}function ms(e,t,a){return e=zt(6,e,null,t),e.lanes=a,e}function Su(e){var t=zt(18,null,null,0);return t.stateNode=e,t}function gs(e,t,a){return t=zt(4,e.children!==null?e.children:[],e.key,t),t.lanes=a,t.stateNode={containerInfo:e.containerInfo,pendingChildren:null,implementation:e.implementation},t}var xu=new WeakMap;function Zt(e,t){if(typeof e=="object"&&e!==null){var a=xu.get(e);return a!==void 0?a:(t={value:e,source:t,stack:Oe(t)},xu.set(e,t),t)}return{value:e,source:t,stack:Oe(t)}}var Ja=[],Ia=0,ko=null,jl=0,Qt=[],Vt=0,Hn=null,sn=1,rn="";function bn(e,t){Ja[Ia++]=jl,Ja[Ia++]=ko,ko=e,jl=t}function wu(e,t,a){Qt[Vt++]=sn,Qt[Vt++]=rn,Qt[Vt++]=Hn,Hn=e;var l=sn;e=rn;var o=32-wt(l)-1;l&=~(1<<o),a+=1;var i=32-wt(t)+o;if(30<i){var c=o-o%5;i=(l&(1<<c)-1).toString(32),l>>=c,o-=c,sn=1<<32-wt(t)+o|a<<o|l,rn=i+e}else sn=1<<i|a<<o|l,rn=e}function ys(e){e.return!==null&&(bn(e,1),wu(e,1,0))}function bs(e){for(;e===ko;)ko=Ja[--Ia],Ja[Ia]=null,jl=Ja[--Ia],Ja[Ia]=null;for(;e===Hn;)Hn=Qt[--Vt],Qt[Vt]=null,rn=Qt[--Vt],Qt[Vt]=null,sn=Qt[--Vt],Qt[Vt]=null}function Tu(e,t){Qt[Vt++]=sn,Qt[Vt++]=rn,Qt[Vt++]=Hn,sn=t.id,rn=t.overflow,Hn=e}var gt=null,Qe=null,Me=!1,Gn=null,Kt=!1,vs=Error(u(519));function Un(e){var t=Error(u(418,1<arguments.length&&arguments[1]!==void 0&&arguments[1]?"text":"HTML",""));throw Ml(Zt(t,e)),vs}function Eu(e){var t=e.stateNode,a=e.type,l=e.memoizedProps;switch(t[mt]=e,t[At]=l,a){case"dialog":Ae("cancel",t),Ae("close",t);break;case"iframe":case"object":case"embed":Ae("load",t);break;case"video":case"audio":for(a=0;a<Pl.length;a++)Ae(Pl[a],t);break;case"source":Ae("error",t);break;case"img":case"image":case"link":Ae("error",t),Ae("load",t);break;case"details":Ae("toggle",t);break;case"input":Ae("invalid",t),Gc(t,l.value,l.defaultValue,l.checked,l.defaultChecked,l.type,l.name,!0);break;case"select":Ae("invalid",t);break;case"textarea":Ae("invalid",t),Lc(t,l.value,l.defaultValue,l.children)}a=l.children,typeof a!="string"&&typeof a!="number"&&typeof a!="bigint"||t.textContent===""+a||l.suppressHydrationWarning===!0||qp(t.textContent,a)?(l.popover!=null&&(Ae("beforetoggle",t),Ae("toggle",t)),l.onScroll!=null&&Ae("scroll",t),l.onScrollEnd!=null&&Ae("scrollend",t),l.onClick!=null&&(t.onclick=mn),t=!0):t=!1,t||Un(e,!0)}function Au(e){for(gt=e.return;gt;)switch(gt.tag){case 5:case 31:case 13:Kt=!1;return;case 27:case 3:Kt=!0;return;default:gt=gt.return}}function Za(e){if(e!==gt)return!1;if(!Me)return Au(e),Me=!0,!1;var t=e.tag,a;if((a=t!==3&&t!==27)&&((a=t===5)&&(a=e.type,a=!(a!=="form"&&a!=="button")||Hr(e.type,e.memoizedProps)),a=!a),a&&Qe&&Un(e),Au(e),t===13){if(e=e.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(u(317));Qe=Wp(e)}else if(t===31){if(e=e.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(u(317));Qe=Wp(e)}else t===27?(t=Qe,Fn(e.type)?(e=qr,qr=null,Qe=e):Qe=t):Qe=gt?Wt(e.stateNode.nextSibling):null;return!0}function ya(){Qe=gt=null,Me=!1}function _s(){var e=Gn;return e!==null&&(Dt===null?Dt=e:Dt.push.apply(Dt,e),Gn=null),e}function Ml(e){Gn===null?Gn=[e]:Gn.push(e)}var Ss=ie(null),ba=null,vn=null;function Ln(e,t,a){ee(Ss,t._currentValue),t._currentValue=a}function _n(e){e._currentValue=Ss.current,oe(Ss)}function xs(e,t,a){for(;e!==null;){var l=e.alternate;if((e.childLanes&t)!==t?(e.childLanes|=t,l!==null&&(l.childLanes|=t)):l!==null&&(l.childLanes&t)!==t&&(l.childLanes|=t),e===a)break;e=e.return}}function ws(e,t,a,l){var o=e.child;for(o!==null&&(o.return=e);o!==null;){var i=o.dependencies;if(i!==null){var c=o.child;i=i.firstContext;e:for(;i!==null;){var d=i;i=o;for(var S=0;S<t.length;S++)if(d.context===t[S]){i.lanes|=a,d=i.alternate,d!==null&&(d.lanes|=a),xs(i.return,a,e),l||(c=null);break e}i=d.next}}else if(o.tag===18){if(c=o.return,c===null)throw Error(u(341));c.lanes|=a,i=c.alternate,i!==null&&(i.lanes|=a),xs(c,a,e),c=null}else c=o.child;if(c!==null)c.return=o;else for(c=o;c!==null;){if(c===e){c=null;break}if(o=c.sibling,o!==null){o.return=c.return,c=o;break}c=c.return}o=c}}function Qa(e,t,a,l){e=null;for(var o=t,i=!1;o!==null;){if(!i){if((o.flags&524288)!==0)i=!0;else if((o.flags&262144)!==0)break}if(o.tag===10){var c=o.alternate;if(c===null)throw Error(u(387));if(c=c.memoizedProps,c!==null){var d=o.type;Rt(o.pendingProps.value,c.value)||(e!==null?e.push(d):e=[d])}}else if(o===je.current){if(c=o.alternate,c===null)throw Error(u(387));c.memoizedState.memoizedState!==o.memoizedState.memoizedState&&(e!==null?e.push(ao):e=[ao])}o=o.return}e!==null&&ws(t,e,a,l),t.flags|=262144}function Ro(e){for(e=e.firstContext;e!==null;){if(!Rt(e.context._currentValue,e.memoizedValue))return!0;e=e.next}return!1}function va(e){ba=e,vn=null,e=e.dependencies,e!==null&&(e.firstContext=null)}function yt(e){return Nu(ba,e)}function zo(e,t){return ba===null&&va(e),Nu(e,t)}function Nu(e,t){var a=t._currentValue;if(t={context:t,memoizedValue:a,next:null},vn===null){if(e===null)throw Error(u(308));vn=t,e.dependencies={lanes:0,firstContext:t},e.flags|=524288}else vn=vn.next=t;return a}var fm=typeof AbortController<"u"?AbortController:function(){var e=[],t=this.signal={aborted:!1,addEventListener:function(a,l){e.push(l)}};this.abort=function(){t.aborted=!0,e.forEach(function(a){return a()})}},mm=r.unstable_scheduleCallback,gm=r.unstable_NormalPriority,ot={$$typeof:H,Consumer:null,Provider:null,_currentValue:null,_currentValue2:null,_threadCount:0};function Ts(){return{controller:new fm,data:new Map,refCount:0}}function Dl(e){e.refCount--,e.refCount===0&&mm(gm,function(){e.controller.abort()})}var Ol=null,Es=0,Va=0,Ka=null;function ym(e,t){if(Ol===null){var a=Ol=[];Es=0,Va=Cr(),Ka={status:"pending",value:void 0,then:function(l){a.push(l)}}}return Es++,t.then(Cu,Cu),t}function Cu(){if(--Es===0&&Ol!==null){Ka!==null&&(Ka.status="fulfilled");var e=Ol;Ol=null,Va=0,Ka=null;for(var t=0;t<e.length;t++)(0,e[t])()}}function bm(e,t){var a=[],l={status:"pending",value:null,reason:null,then:function(o){a.push(o)}};return e.then(function(){l.status="fulfilled",l.value=t;for(var o=0;o<a.length;o++)(0,a[o])(t)},function(o){for(l.status="rejected",l.reason=o,o=0;o<a.length;o++)(0,a[o])(void 0)}),l}var ju=D.S;D.S=function(e,t){pp=ve(),typeof t=="object"&&t!==null&&typeof t.then=="function"&&ym(e,t),ju!==null&&ju(e,t)};var _a=ie(null);function As(){var e=_a.current;return e!==null?e:Ie.pooledCache}function Bo(e,t){t===null?ee(_a,_a.current):ee(_a,t.pool)}function Mu(){var e=As();return e===null?null:{parent:ot._currentValue,pool:e}}var $a=Error(u(460)),Ns=Error(u(474)),Ho=Error(u(542)),Go={then:function(){}};function Du(e){return e=e.status,e==="fulfilled"||e==="rejected"}function Ou(e,t,a){switch(a=e[a],a===void 0?e.push(t):a!==t&&(t.then(mn,mn),t=a),t.status){case"fulfilled":return t.value;case"rejected":throw e=t.reason,Ru(e),e;default:if(typeof t.status=="string")t.then(mn,mn);else{if(e=Ie,e!==null&&100<e.shellSuspendCounter)throw Error(u(482));e=t,e.status="pending",e.then(function(l){if(t.status==="pending"){var o=t;o.status="fulfilled",o.value=l}},function(l){if(t.status==="pending"){var o=t;o.status="rejected",o.reason=l}})}switch(t.status){case"fulfilled":return t.value;case"rejected":throw e=t.reason,Ru(e),e}throw xa=t,$a}}function Sa(e){try{var t=e._init;return t(e._payload)}catch(a){throw a!==null&&typeof a=="object"&&typeof a.then=="function"?(xa=a,$a):a}}var xa=null;function ku(){if(xa===null)throw Error(u(459));var e=xa;return xa=null,e}function Ru(e){if(e===$a||e===Ho)throw Error(u(483))}var Wa=null,kl=0;function Uo(e){var t=kl;return kl+=1,Wa===null&&(Wa=[]),Ou(Wa,e,t)}function Rl(e,t){t=t.props.ref,e.ref=t!==void 0?t:null}function Lo(e,t){throw t.$$typeof===x?Error(u(525)):(e=Object.prototype.toString.call(t),Error(u(31,e==="[object Object]"?"object with keys {"+Object.keys(t).join(", ")+"}":e)))}function zu(e){function t(M,A){if(e){var R=M.deletions;R===null?(M.deletions=[A],M.flags|=16):R.push(A)}}function a(M,A){if(!e)return null;for(;A!==null;)t(M,A),A=A.sibling;return null}function l(M){for(var A=new Map;M!==null;)M.key!==null?A.set(M.key,M):A.set(M.index,M),M=M.sibling;return A}function o(M,A){return M=yn(M,A),M.index=0,M.sibling=null,M}function i(M,A,R){return M.index=R,e?(R=M.alternate,R!==null?(R=R.index,R<A?(M.flags|=67108866,A):R):(M.flags|=67108866,A)):(M.flags|=1048576,A)}function c(M){return e&&M.alternate===null&&(M.flags|=67108866),M}function d(M,A,R,J){return A===null||A.tag!==6?(A=ms(R,M.mode,J),A.return=M,A):(A=o(A,R),A.return=M,A)}function S(M,A,R,J){var ce=R.type;return ce===C?q(M,A,R.props.children,J,R.key):A!==null&&(A.elementType===ce||typeof ce=="object"&&ce!==null&&ce.$$typeof===I&&Sa(ce)===A.type)?(A=o(A,R.props),Rl(A,R),A.return=M,A):(A=Oo(R.type,R.key,R.props,null,M.mode,J),Rl(A,R),A.return=M,A)}function z(M,A,R,J){return A===null||A.tag!==4||A.stateNode.containerInfo!==R.containerInfo||A.stateNode.implementation!==R.implementation?(A=gs(R,M.mode,J),A.return=M,A):(A=o(A,R.children||[]),A.return=M,A)}function q(M,A,R,J,ce){return A===null||A.tag!==7?(A=ga(R,M.mode,J,ce),A.return=M,A):(A=o(A,R),A.return=M,A)}function Z(M,A,R){if(typeof A=="string"&&A!==""||typeof A=="number"||typeof A=="bigint")return A=ms(""+A,M.mode,R),A.return=M,A;if(typeof A=="object"&&A!==null){switch(A.$$typeof){case N:return R=Oo(A.type,A.key,A.props,null,M.mode,R),Rl(R,A),R.return=M,R;case E:return A=gs(A,M.mode,R),A.return=M,A;case I:return A=Sa(A),Z(M,A,R)}if(re(A)||te(A))return A=ga(A,M.mode,R,null),A.return=M,A;if(typeof A.then=="function")return Z(M,Uo(A),R);if(A.$$typeof===H)return Z(M,zo(M,A),R);Lo(M,A)}return null}function G(M,A,R,J){var ce=A!==null?A.key:null;if(typeof R=="string"&&R!==""||typeof R=="number"||typeof R=="bigint")return ce!==null?null:d(M,A,""+R,J);if(typeof R=="object"&&R!==null){switch(R.$$typeof){case N:return R.key===ce?S(M,A,R,J):null;case E:return R.key===ce?z(M,A,R,J):null;case I:return R=Sa(R),G(M,A,R,J)}if(re(R)||te(R))return ce!==null?null:q(M,A,R,J,null);if(typeof R.then=="function")return G(M,A,Uo(R),J);if(R.$$typeof===H)return G(M,A,zo(M,R),J);Lo(M,R)}return null}function U(M,A,R,J,ce){if(typeof J=="string"&&J!==""||typeof J=="number"||typeof J=="bigint")return M=M.get(R)||null,d(A,M,""+J,ce);if(typeof J=="object"&&J!==null){switch(J.$$typeof){case N:return M=M.get(J.key===null?R:J.key)||null,S(A,M,J,ce);case E:return M=M.get(J.key===null?R:J.key)||null,z(A,M,J,ce);case I:return J=Sa(J),U(M,A,R,J,ce)}if(re(J)||te(J))return M=M.get(R)||null,q(A,M,J,ce,null);if(typeof J.then=="function")return U(M,A,R,Uo(J),ce);if(J.$$typeof===H)return U(M,A,R,zo(A,J),ce);Lo(A,J)}return null}function ne(M,A,R,J){for(var ce=null,ke=null,le=A,Se=A=0,Ce=null;le!==null&&Se<R.length;Se++){le.index>Se?(Ce=le,le=null):Ce=le.sibling;var Re=G(M,le,R[Se],J);if(Re===null){le===null&&(le=Ce);break}e&&le&&Re.alternate===null&&t(M,le),A=i(Re,A,Se),ke===null?ce=Re:ke.sibling=Re,ke=Re,le=Ce}if(Se===R.length)return a(M,le),Me&&bn(M,Se),ce;if(le===null){for(;Se<R.length;Se++)le=Z(M,R[Se],J),le!==null&&(A=i(le,A,Se),ke===null?ce=le:ke.sibling=le,ke=le);return Me&&bn(M,Se),ce}for(le=l(le);Se<R.length;Se++)Ce=U(le,M,Se,R[Se],J),Ce!==null&&(e&&Ce.alternate!==null&&le.delete(Ce.key===null?Se:Ce.key),A=i(Ce,A,Se),ke===null?ce=Ce:ke.sibling=Ce,ke=Ce);return e&&le.forEach(function(la){return t(M,la)}),Me&&bn(M,Se),ce}function pe(M,A,R,J){if(R==null)throw Error(u(151));for(var ce=null,ke=null,le=A,Se=A=0,Ce=null,Re=R.next();le!==null&&!Re.done;Se++,Re=R.next()){le.index>Se?(Ce=le,le=null):Ce=le.sibling;var la=G(M,le,Re.value,J);if(la===null){le===null&&(le=Ce);break}e&&le&&la.alternate===null&&t(M,le),A=i(la,A,Se),ke===null?ce=la:ke.sibling=la,ke=la,le=Ce}if(Re.done)return a(M,le),Me&&bn(M,Se),ce;if(le===null){for(;!Re.done;Se++,Re=R.next())Re=Z(M,Re.value,J),Re!==null&&(A=i(Re,A,Se),ke===null?ce=Re:ke.sibling=Re,ke=Re);return Me&&bn(M,Se),ce}for(le=l(le);!Re.done;Se++,Re=R.next())Re=U(le,M,Se,Re.value,J),Re!==null&&(e&&Re.alternate!==null&&le.delete(Re.key===null?Se:Re.key),A=i(Re,A,Se),ke===null?ce=Re:ke.sibling=Re,ke=Re);return e&&le.forEach(function(jg){return t(M,jg)}),Me&&bn(M,Se),ce}function Je(M,A,R,J){if(typeof R=="object"&&R!==null&&R.type===C&&R.key===null&&(R=R.props.children),typeof R=="object"&&R!==null){switch(R.$$typeof){case N:e:{for(var ce=R.key;A!==null;){if(A.key===ce){if(ce=R.type,ce===C){if(A.tag===7){a(M,A.sibling),J=o(A,R.props.children),J.return=M,M=J;break e}}else if(A.elementType===ce||typeof ce=="object"&&ce!==null&&ce.$$typeof===I&&Sa(ce)===A.type){a(M,A.sibling),J=o(A,R.props),Rl(J,R),J.return=M,M=J;break e}a(M,A);break}else t(M,A);A=A.sibling}R.type===C?(J=ga(R.props.children,M.mode,J,R.key),J.return=M,M=J):(J=Oo(R.type,R.key,R.props,null,M.mode,J),Rl(J,R),J.return=M,M=J)}return c(M);case E:e:{for(ce=R.key;A!==null;){if(A.key===ce)if(A.tag===4&&A.stateNode.containerInfo===R.containerInfo&&A.stateNode.implementation===R.implementation){a(M,A.sibling),J=o(A,R.children||[]),J.return=M,M=J;break e}else{a(M,A);break}else t(M,A);A=A.sibling}J=gs(R,M.mode,J),J.return=M,M=J}return c(M);case I:return R=Sa(R),Je(M,A,R,J)}if(re(R))return ne(M,A,R,J);if(te(R)){if(ce=te(R),typeof ce!="function")throw Error(u(150));return R=ce.call(R),pe(M,A,R,J)}if(typeof R.then=="function")return Je(M,A,Uo(R),J);if(R.$$typeof===H)return Je(M,A,zo(M,R),J);Lo(M,R)}return typeof R=="string"&&R!==""||typeof R=="number"||typeof R=="bigint"?(R=""+R,A!==null&&A.tag===6?(a(M,A.sibling),J=o(A,R),J.return=M,M=J):(a(M,A),J=ms(R,M.mode,J),J.return=M,M=J),c(M)):a(M,A)}return function(M,A,R,J){try{kl=0;var ce=Je(M,A,R,J);return Wa=null,ce}catch(le){if(le===$a||le===Ho)throw le;var ke=zt(29,le,null,M.mode);return ke.lanes=J,ke.return=M,ke}finally{}}}var wa=zu(!0),Bu=zu(!1),Yn=!1;function Cs(e){e.updateQueue={baseState:e.memoizedState,firstBaseUpdate:null,lastBaseUpdate:null,shared:{pending:null,lanes:0,hiddenCallbacks:null},callbacks:null}}function js(e,t){e=e.updateQueue,t.updateQueue===e&&(t.updateQueue={baseState:e.baseState,firstBaseUpdate:e.firstBaseUpdate,lastBaseUpdate:e.lastBaseUpdate,shared:e.shared,callbacks:null})}function qn(e){return{lane:e,tag:0,payload:null,callback:null,next:null}}function Xn(e,t,a){var l=e.updateQueue;if(l===null)return null;if(l=l.shared,(Be&2)!==0){var o=l.pending;return o===null?t.next=t:(t.next=o.next,o.next=t),l.pending=t,t=Do(e),vu(e,null,a),t}return Mo(e,l,t,a),Do(e)}function zl(e,t,a){if(t=t.updateQueue,t!==null&&(t=t.shared,(a&4194048)!==0)){var l=t.lanes;l&=e.pendingLanes,a|=l,t.lanes=a,Nc(e,a)}}function Ms(e,t){var a=e.updateQueue,l=e.alternate;if(l!==null&&(l=l.updateQueue,a===l)){var o=null,i=null;if(a=a.firstBaseUpdate,a!==null){do{var c={lane:a.lane,tag:a.tag,payload:a.payload,callback:null,next:null};i===null?o=i=c:i=i.next=c,a=a.next}while(a!==null);i===null?o=i=t:i=i.next=t}else o=i=t;a={baseState:l.baseState,firstBaseUpdate:o,lastBaseUpdate:i,shared:l.shared,callbacks:l.callbacks},e.updateQueue=a;return}e=a.lastBaseUpdate,e===null?a.firstBaseUpdate=t:e.next=t,a.lastBaseUpdate=t}var Ds=!1;function Bl(){if(Ds){var e=Ka;if(e!==null)throw e}}function Hl(e,t,a,l){Ds=!1;var o=e.updateQueue;Yn=!1;var i=o.firstBaseUpdate,c=o.lastBaseUpdate,d=o.shared.pending;if(d!==null){o.shared.pending=null;var S=d,z=S.next;S.next=null,c===null?i=z:c.next=z,c=S;var q=e.alternate;q!==null&&(q=q.updateQueue,d=q.lastBaseUpdate,d!==c&&(d===null?q.firstBaseUpdate=z:d.next=z,q.lastBaseUpdate=S))}if(i!==null){var Z=o.baseState;c=0,q=z=S=null,d=i;do{var G=d.lane&-536870913,U=G!==d.lane;if(U?(Ne&G)===G:(l&G)===G){G!==0&&G===Va&&(Ds=!0),q!==null&&(q=q.next={lane:0,tag:d.tag,payload:d.payload,callback:null,next:null});e:{var ne=e,pe=d;G=t;var Je=a;switch(pe.tag){case 1:if(ne=pe.payload,typeof ne=="function"){Z=ne.call(Je,Z,G);break e}Z=ne;break e;case 3:ne.flags=ne.flags&-65537|128;case 0:if(ne=pe.payload,G=typeof ne=="function"?ne.call(Je,Z,G):ne,G==null)break e;Z=v({},Z,G);break e;case 2:Yn=!0}}G=d.callback,G!==null&&(e.flags|=64,U&&(e.flags|=8192),U=o.callbacks,U===null?o.callbacks=[G]:U.push(G))}else U={lane:G,tag:d.tag,payload:d.payload,callback:d.callback,next:null},q===null?(z=q=U,S=Z):q=q.next=U,c|=G;if(d=d.next,d===null){if(d=o.shared.pending,d===null)break;U=d,d=U.next,U.next=null,o.lastBaseUpdate=U,o.shared.pending=null}}while(!0);q===null&&(S=Z),o.baseState=S,o.firstBaseUpdate=z,o.lastBaseUpdate=q,i===null&&(o.shared.lanes=0),Vn|=c,e.lanes=c,e.memoizedState=Z}}function Hu(e,t){if(typeof e!="function")throw Error(u(191,e));e.call(t)}function Gu(e,t){var a=e.callbacks;if(a!==null)for(e.callbacks=null,e=0;e<a.length;e++)Hu(a[e],t)}var Pa=ie(null),Yo=ie(0);function Uu(e,t){e=jn,ee(Yo,e),ee(Pa,t),jn=e|t.baseLanes}function Os(){ee(Yo,jn),ee(Pa,Pa.current)}function ks(){jn=Yo.current,oe(Pa),oe(Yo)}var Bt=ie(null),$t=null;function Jn(e){var t=e.alternate;ee(tt,tt.current&1),ee(Bt,e),$t===null&&(t===null||Pa.current!==null||t.memoizedState!==null)&&($t=e)}function Rs(e){ee(tt,tt.current),ee(Bt,e),$t===null&&($t=e)}function Lu(e){e.tag===22?(ee(tt,tt.current),ee(Bt,e),$t===null&&($t=e)):In()}function In(){ee(tt,tt.current),ee(Bt,Bt.current)}function Ht(e){oe(Bt),$t===e&&($t=null),oe(tt)}var tt=ie(0);function qo(e){for(var t=e;t!==null;){if(t.tag===13){var a=t.memoizedState;if(a!==null&&(a=a.dehydrated,a===null||Lr(a)||Yr(a)))return t}else if(t.tag===19&&(t.memoizedProps.revealOrder==="forwards"||t.memoizedProps.revealOrder==="backwards"||t.memoizedProps.revealOrder==="unstable_legacy-backwards"||t.memoizedProps.revealOrder==="together")){if((t.flags&128)!==0)return t}else if(t.child!==null){t.child.return=t,t=t.child;continue}if(t===e)break;for(;t.sibling===null;){if(t.return===null||t.return===e)return null;t=t.return}t.sibling.return=t.return,t=t.sibling}return null}var Sn=0,_e=null,qe=null,it=null,Xo=!1,Fa=!1,Ta=!1,Jo=0,Gl=0,el=null,vm=0;function We(){throw Error(u(321))}function zs(e,t){if(t===null)return!1;for(var a=0;a<t.length&&a<e.length;a++)if(!Rt(e[a],t[a]))return!1;return!0}function Bs(e,t,a,l,o,i){return Sn=i,_e=t,t.memoizedState=null,t.updateQueue=null,t.lanes=0,D.H=e===null||e.memoizedState===null?wd:Ws,Ta=!1,i=a(l,o),Ta=!1,Fa&&(i=qu(t,a,l,o)),Yu(e),i}function Yu(e){D.H=Yl;var t=qe!==null&&qe.next!==null;if(Sn=0,it=qe=_e=null,Xo=!1,Gl=0,el=null,t)throw Error(u(300));e===null||st||(e=e.dependencies,e!==null&&Ro(e)&&(st=!0))}function qu(e,t,a,l){_e=e;var o=0;do{if(Fa&&(el=null),Gl=0,Fa=!1,25<=o)throw Error(u(301));if(o+=1,it=qe=null,e.updateQueue!=null){var i=e.updateQueue;i.lastEffect=null,i.events=null,i.stores=null,i.memoCache!=null&&(i.memoCache.index=0)}D.H=Td,i=t(a,l)}while(Fa);return i}function _m(){var e=D.H,t=e.useState()[0];return t=typeof t.then=="function"?Ul(t):t,e=e.useState()[0],(qe!==null?qe.memoizedState:null)!==e&&(_e.flags|=1024),t}function Hs(){var e=Jo!==0;return Jo=0,e}function Gs(e,t,a){t.updateQueue=e.updateQueue,t.flags&=-2053,e.lanes&=~a}function Us(e){if(Xo){for(e=e.memoizedState;e!==null;){var t=e.queue;t!==null&&(t.pending=null),e=e.next}Xo=!1}Sn=0,it=qe=_e=null,Fa=!1,Gl=Jo=0,el=null}function Tt(){var e={memoizedState:null,baseState:null,baseQueue:null,queue:null,next:null};return it===null?_e.memoizedState=it=e:it=it.next=e,it}function nt(){if(qe===null){var e=_e.alternate;e=e!==null?e.memoizedState:null}else e=qe.next;var t=it===null?_e.memoizedState:it.next;if(t!==null)it=t,qe=e;else{if(e===null)throw _e.alternate===null?Error(u(467)):Error(u(310));qe=e,e={memoizedState:qe.memoizedState,baseState:qe.baseState,baseQueue:qe.baseQueue,queue:qe.queue,next:null},it===null?_e.memoizedState=it=e:it=it.next=e}return it}function Io(){return{lastEffect:null,events:null,stores:null,memoCache:null}}function Ul(e){var t=Gl;return Gl+=1,el===null&&(el=[]),e=Ou(el,e,t),t=_e,(it===null?t.memoizedState:it.next)===null&&(t=t.alternate,D.H=t===null||t.memoizedState===null?wd:Ws),e}function Zo(e){if(e!==null&&typeof e=="object"){if(typeof e.then=="function")return Ul(e);if(e.$$typeof===H)return yt(e)}throw Error(u(438,String(e)))}function Ls(e){var t=null,a=_e.updateQueue;if(a!==null&&(t=a.memoCache),t==null){var l=_e.alternate;l!==null&&(l=l.updateQueue,l!==null&&(l=l.memoCache,l!=null&&(t={data:l.data.map(function(o){return o.slice()}),index:0})))}if(t==null&&(t={data:[],index:0}),a===null&&(a=Io(),_e.updateQueue=a),a.memoCache=t,a=t.data[t.index],a===void 0)for(a=t.data[t.index]=Array(e),l=0;l<e;l++)a[l]=K;return t.index++,a}function xn(e,t){return typeof t=="function"?t(e):t}function Qo(e){var t=nt();return Ys(t,qe,e)}function Ys(e,t,a){var l=e.queue;if(l===null)throw Error(u(311));l.lastRenderedReducer=a;var o=e.baseQueue,i=l.pending;if(i!==null){if(o!==null){var c=o.next;o.next=i.next,i.next=c}t.baseQueue=o=i,l.pending=null}if(i=e.baseState,o===null)e.memoizedState=i;else{t=o.next;var d=c=null,S=null,z=t,q=!1;do{var Z=z.lane&-536870913;if(Z!==z.lane?(Ne&Z)===Z:(Sn&Z)===Z){var G=z.revertLane;if(G===0)S!==null&&(S=S.next={lane:0,revertLane:0,gesture:null,action:z.action,hasEagerState:z.hasEagerState,eagerState:z.eagerState,next:null}),Z===Va&&(q=!0);else if((Sn&G)===G){z=z.next,G===Va&&(q=!0);continue}else Z={lane:0,revertLane:z.revertLane,gesture:null,action:z.action,hasEagerState:z.hasEagerState,eagerState:z.eagerState,next:null},S===null?(d=S=Z,c=i):S=S.next=Z,_e.lanes|=G,Vn|=G;Z=z.action,Ta&&a(i,Z),i=z.hasEagerState?z.eagerState:a(i,Z)}else G={lane:Z,revertLane:z.revertLane,gesture:z.gesture,action:z.action,hasEagerState:z.hasEagerState,eagerState:z.eagerState,next:null},S===null?(d=S=G,c=i):S=S.next=G,_e.lanes|=Z,Vn|=Z;z=z.next}while(z!==null&&z!==t);if(S===null?c=i:S.next=d,!Rt(i,e.memoizedState)&&(st=!0,q&&(a=Ka,a!==null)))throw a;e.memoizedState=i,e.baseState=c,e.baseQueue=S,l.lastRenderedState=i}return o===null&&(l.lanes=0),[e.memoizedState,l.dispatch]}function qs(e){var t=nt(),a=t.queue;if(a===null)throw Error(u(311));a.lastRenderedReducer=e;var l=a.dispatch,o=a.pending,i=t.memoizedState;if(o!==null){a.pending=null;var c=o=o.next;do i=e(i,c.action),c=c.next;while(c!==o);Rt(i,t.memoizedState)||(st=!0),t.memoizedState=i,t.baseQueue===null&&(t.baseState=i),a.lastRenderedState=i}return[i,l]}function Xu(e,t,a){var l=_e,o=nt(),i=Me;if(i){if(a===void 0)throw Error(u(407));a=a()}else a=t();var c=!Rt((qe||o).memoizedState,a);if(c&&(o.memoizedState=a,st=!0),o=o.queue,Is(Zu.bind(null,l,o,e),[e]),o.getSnapshot!==t||c||it!==null&&it.memoizedState.tag&1){if(l.flags|=2048,tl(9,{destroy:void 0},Iu.bind(null,l,o,a,t),null),Ie===null)throw Error(u(349));i||(Sn&127)!==0||Ju(l,t,a)}return a}function Ju(e,t,a){e.flags|=16384,e={getSnapshot:t,value:a},t=_e.updateQueue,t===null?(t=Io(),_e.updateQueue=t,t.stores=[e]):(a=t.stores,a===null?t.stores=[e]:a.push(e))}function Iu(e,t,a,l){t.value=a,t.getSnapshot=l,Qu(t)&&Vu(e)}function Zu(e,t,a){return a(function(){Qu(t)&&Vu(e)})}function Qu(e){var t=e.getSnapshot;e=e.value;try{var a=t();return!Rt(e,a)}catch{return!0}}function Vu(e){var t=ma(e,2);t!==null&&Ot(t,e,2)}function Xs(e){var t=Tt();if(typeof e=="function"){var a=e;if(e=a(),Ta){on(!0);try{a()}finally{on(!1)}}}return t.memoizedState=t.baseState=e,t.queue={pending:null,lanes:0,dispatch:null,lastRenderedReducer:xn,lastRenderedState:e},t}function Ku(e,t,a,l){return e.baseState=a,Ys(e,qe,typeof l=="function"?l:xn)}function Sm(e,t,a,l,o){if($o(e))throw Error(u(485));if(e=t.action,e!==null){var i={payload:o,action:e,next:null,isTransition:!0,status:"pending",value:null,reason:null,listeners:[],then:function(c){i.listeners.push(c)}};D.T!==null?a(!0):i.isTransition=!1,l(i),a=t.pending,a===null?(i.next=t.pending=i,$u(t,i)):(i.next=a.next,t.pending=a.next=i)}}function $u(e,t){var a=t.action,l=t.payload,o=e.state;if(t.isTransition){var i=D.T,c={};D.T=c;try{var d=a(o,l),S=D.S;S!==null&&S(c,d),Wu(e,t,d)}catch(z){Js(e,t,z)}finally{i!==null&&c.types!==null&&(i.types=c.types),D.T=i}}else try{i=a(o,l),Wu(e,t,i)}catch(z){Js(e,t,z)}}function Wu(e,t,a){a!==null&&typeof a=="object"&&typeof a.then=="function"?a.then(function(l){Pu(e,t,l)},function(l){return Js(e,t,l)}):Pu(e,t,a)}function Pu(e,t,a){t.status="fulfilled",t.value=a,Fu(t),e.state=a,t=e.pending,t!==null&&(a=t.next,a===t?e.pending=null:(a=a.next,t.next=a,$u(e,a)))}function Js(e,t,a){var l=e.pending;if(e.pending=null,l!==null){l=l.next;do t.status="rejected",t.reason=a,Fu(t),t=t.next;while(t!==l)}e.action=null}function Fu(e){e=e.listeners;for(var t=0;t<e.length;t++)(0,e[t])()}function ed(e,t){return t}function td(e,t){if(Me){var a=Ie.formState;if(a!==null){e:{var l=_e;if(Me){if(Qe){t:{for(var o=Qe,i=Kt;o.nodeType!==8;){if(!i){o=null;break t}if(o=Wt(o.nextSibling),o===null){o=null;break t}}i=o.data,o=i==="F!"||i==="F"?o:null}if(o){Qe=Wt(o.nextSibling),l=o.data==="F!";break e}}Un(l)}l=!1}l&&(t=a[0])}}return a=Tt(),a.memoizedState=a.baseState=t,l={pending:null,lanes:0,dispatch:null,lastRenderedReducer:ed,lastRenderedState:t},a.queue=l,a=_d.bind(null,_e,l),l.dispatch=a,l=Xs(!1),i=$s.bind(null,_e,!1,l.queue),l=Tt(),o={state:t,dispatch:null,action:e,pending:null},l.queue=o,a=Sm.bind(null,_e,o,i,a),o.dispatch=a,l.memoizedState=e,[t,a,!1]}function nd(e){var t=nt();return ad(t,qe,e)}function ad(e,t,a){if(t=Ys(e,t,ed)[0],e=Qo(xn)[0],typeof t=="object"&&t!==null&&typeof t.then=="function")try{var l=Ul(t)}catch(c){throw c===$a?Ho:c}else l=t;t=nt();var o=t.queue,i=o.dispatch;return a!==t.memoizedState&&(_e.flags|=2048,tl(9,{destroy:void 0},xm.bind(null,o,a),null)),[l,i,e]}function xm(e,t){e.action=t}function ld(e){var t=nt(),a=qe;if(a!==null)return ad(t,a,e);nt(),t=t.memoizedState,a=nt();var l=a.queue.dispatch;return a.memoizedState=e,[t,l,!1]}function tl(e,t,a,l){return e={tag:e,create:a,deps:l,inst:t,next:null},t=_e.updateQueue,t===null&&(t=Io(),_e.updateQueue=t),a=t.lastEffect,a===null?t.lastEffect=e.next=e:(l=a.next,a.next=e,e.next=l,t.lastEffect=e),e}function od(){return nt().memoizedState}function Vo(e,t,a,l){var o=Tt();_e.flags|=e,o.memoizedState=tl(1|t,{destroy:void 0},a,l===void 0?null:l)}function Ko(e,t,a,l){var o=nt();l=l===void 0?null:l;var i=o.memoizedState.inst;qe!==null&&l!==null&&zs(l,qe.memoizedState.deps)?o.memoizedState=tl(t,i,a,l):(_e.flags|=e,o.memoizedState=tl(1|t,i,a,l))}function id(e,t){Vo(8390656,8,e,t)}function Is(e,t){Ko(2048,8,e,t)}function wm(e){_e.flags|=4;var t=_e.updateQueue;if(t===null)t=Io(),_e.updateQueue=t,t.events=[e];else{var a=t.events;a===null?t.events=[e]:a.push(e)}}function sd(e){var t=nt().memoizedState;return wm({ref:t,nextImpl:e}),function(){if((Be&2)!==0)throw Error(u(440));return t.impl.apply(void 0,arguments)}}function rd(e,t){return Ko(4,2,e,t)}function cd(e,t){return Ko(4,4,e,t)}function ud(e,t){if(typeof t=="function"){e=e();var a=t(e);return function(){typeof a=="function"?a():t(null)}}if(t!=null)return e=e(),t.current=e,function(){t.current=null}}function dd(e,t,a){a=a!=null?a.concat([e]):null,Ko(4,4,ud.bind(null,t,e),a)}function Zs(){}function pd(e,t){var a=nt();t=t===void 0?null:t;var l=a.memoizedState;return t!==null&&zs(t,l[1])?l[0]:(a.memoizedState=[e,t],e)}function hd(e,t){var a=nt();t=t===void 0?null:t;var l=a.memoizedState;if(t!==null&&zs(t,l[1]))return l[0];if(l=e(),Ta){on(!0);try{e()}finally{on(!1)}}return a.memoizedState=[l,t],l}function Qs(e,t,a){return a===void 0||(Sn&1073741824)!==0&&(Ne&261930)===0?e.memoizedState=t:(e.memoizedState=a,e=fp(),_e.lanes|=e,Vn|=e,a)}function fd(e,t,a,l){return Rt(a,t)?a:Pa.current!==null?(e=Qs(e,a,l),Rt(e,t)||(st=!0),e):(Sn&42)===0||(Sn&1073741824)!==0&&(Ne&261930)===0?(st=!0,e.memoizedState=a):(e=fp(),_e.lanes|=e,Vn|=e,t)}function md(e,t,a,l,o){var i=O.p;O.p=i!==0&&8>i?i:8;var c=D.T,d={};D.T=d,$s(e,!1,t,a);try{var S=o(),z=D.S;if(z!==null&&z(d,S),S!==null&&typeof S=="object"&&typeof S.then=="function"){var q=bm(S,l);Ll(e,t,q,Lt(e))}else Ll(e,t,l,Lt(e))}catch(Z){Ll(e,t,{then:function(){},status:"rejected",reason:Z},Lt())}finally{O.p=i,c!==null&&d.types!==null&&(c.types=d.types),D.T=c}}function Tm(){}function Vs(e,t,a,l){if(e.tag!==5)throw Error(u(476));var o=gd(e).queue;md(e,o,t,Q,a===null?Tm:function(){return yd(e),a(l)})}function gd(e){var t=e.memoizedState;if(t!==null)return t;t={memoizedState:Q,baseState:Q,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:xn,lastRenderedState:Q},next:null};var a={};return t.next={memoizedState:a,baseState:a,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:xn,lastRenderedState:a},next:null},e.memoizedState=t,e=e.alternate,e!==null&&(e.memoizedState=t),t}function yd(e){var t=gd(e);t.next===null&&(t=e.alternate.memoizedState),Ll(e,t.next.queue,{},Lt())}function Ks(){return yt(ao)}function bd(){return nt().memoizedState}function vd(){return nt().memoizedState}function Em(e){for(var t=e.return;t!==null;){switch(t.tag){case 24:case 3:var a=Lt();e=qn(a);var l=Xn(t,e,a);l!==null&&(Ot(l,t,a),zl(l,t,a)),t={cache:Ts()},e.payload=t;return}t=t.return}}function Am(e,t,a){var l=Lt();a={lane:l,revertLane:0,gesture:null,action:a,hasEagerState:!1,eagerState:null,next:null},$o(e)?Sd(t,a):(a=hs(e,t,a,l),a!==null&&(Ot(a,e,l),xd(a,t,l)))}function _d(e,t,a){var l=Lt();Ll(e,t,a,l)}function Ll(e,t,a,l){var o={lane:l,revertLane:0,gesture:null,action:a,hasEagerState:!1,eagerState:null,next:null};if($o(e))Sd(t,o);else{var i=e.alternate;if(e.lanes===0&&(i===null||i.lanes===0)&&(i=t.lastRenderedReducer,i!==null))try{var c=t.lastRenderedState,d=i(c,a);if(o.hasEagerState=!0,o.eagerState=d,Rt(d,c))return Mo(e,t,o,0),Ie===null&&jo(),!1}catch{}finally{}if(a=hs(e,t,o,l),a!==null)return Ot(a,e,l),xd(a,t,l),!0}return!1}function $s(e,t,a,l){if(l={lane:2,revertLane:Cr(),gesture:null,action:l,hasEagerState:!1,eagerState:null,next:null},$o(e)){if(t)throw Error(u(479))}else t=hs(e,a,l,2),t!==null&&Ot(t,e,2)}function $o(e){var t=e.alternate;return e===_e||t!==null&&t===_e}function Sd(e,t){Fa=Xo=!0;var a=e.pending;a===null?t.next=t:(t.next=a.next,a.next=t),e.pending=t}function xd(e,t,a){if((a&4194048)!==0){var l=t.lanes;l&=e.pendingLanes,a|=l,t.lanes=a,Nc(e,a)}}var Yl={readContext:yt,use:Zo,useCallback:We,useContext:We,useEffect:We,useImperativeHandle:We,useLayoutEffect:We,useInsertionEffect:We,useMemo:We,useReducer:We,useRef:We,useState:We,useDebugValue:We,useDeferredValue:We,useTransition:We,useSyncExternalStore:We,useId:We,useHostTransitionStatus:We,useFormState:We,useActionState:We,useOptimistic:We,useMemoCache:We,useCacheRefresh:We};Yl.useEffectEvent=We;var wd={readContext:yt,use:Zo,useCallback:function(e,t){return Tt().memoizedState=[e,t===void 0?null:t],e},useContext:yt,useEffect:id,useImperativeHandle:function(e,t,a){a=a!=null?a.concat([e]):null,Vo(4194308,4,ud.bind(null,t,e),a)},useLayoutEffect:function(e,t){return Vo(4194308,4,e,t)},useInsertionEffect:function(e,t){Vo(4,2,e,t)},useMemo:function(e,t){var a=Tt();t=t===void 0?null:t;var l=e();if(Ta){on(!0);try{e()}finally{on(!1)}}return a.memoizedState=[l,t],l},useReducer:function(e,t,a){var l=Tt();if(a!==void 0){var o=a(t);if(Ta){on(!0);try{a(t)}finally{on(!1)}}}else o=t;return l.memoizedState=l.baseState=o,e={pending:null,lanes:0,dispatch:null,lastRenderedReducer:e,lastRenderedState:o},l.queue=e,e=e.dispatch=Am.bind(null,_e,e),[l.memoizedState,e]},useRef:function(e){var t=Tt();return e={current:e},t.memoizedState=e},useState:function(e){e=Xs(e);var t=e.queue,a=_d.bind(null,_e,t);return t.dispatch=a,[e.memoizedState,a]},useDebugValue:Zs,useDeferredValue:function(e,t){var a=Tt();return Qs(a,e,t)},useTransition:function(){var e=Xs(!1);return e=md.bind(null,_e,e.queue,!0,!1),Tt().memoizedState=e,[!1,e]},useSyncExternalStore:function(e,t,a){var l=_e,o=Tt();if(Me){if(a===void 0)throw Error(u(407));a=a()}else{if(a=t(),Ie===null)throw Error(u(349));(Ne&127)!==0||Ju(l,t,a)}o.memoizedState=a;var i={value:a,getSnapshot:t};return o.queue=i,id(Zu.bind(null,l,i,e),[e]),l.flags|=2048,tl(9,{destroy:void 0},Iu.bind(null,l,i,a,t),null),a},useId:function(){var e=Tt(),t=Ie.identifierPrefix;if(Me){var a=rn,l=sn;a=(l&~(1<<32-wt(l)-1)).toString(32)+a,t="_"+t+"R_"+a,a=Jo++,0<a&&(t+="H"+a.toString(32)),t+="_"}else a=vm++,t="_"+t+"r_"+a.toString(32)+"_";return e.memoizedState=t},useHostTransitionStatus:Ks,useFormState:td,useActionState:td,useOptimistic:function(e){var t=Tt();t.memoizedState=t.baseState=e;var a={pending:null,lanes:0,dispatch:null,lastRenderedReducer:null,lastRenderedState:null};return t.queue=a,t=$s.bind(null,_e,!0,a),a.dispatch=t,[e,t]},useMemoCache:Ls,useCacheRefresh:function(){return Tt().memoizedState=Em.bind(null,_e)},useEffectEvent:function(e){var t=Tt(),a={impl:e};return t.memoizedState=a,function(){if((Be&2)!==0)throw Error(u(440));return a.impl.apply(void 0,arguments)}}},Ws={readContext:yt,use:Zo,useCallback:pd,useContext:yt,useEffect:Is,useImperativeHandle:dd,useInsertionEffect:rd,useLayoutEffect:cd,useMemo:hd,useReducer:Qo,useRef:od,useState:function(){return Qo(xn)},useDebugValue:Zs,useDeferredValue:function(e,t){var a=nt();return fd(a,qe.memoizedState,e,t)},useTransition:function(){var e=Qo(xn)[0],t=nt().memoizedState;return[typeof e=="boolean"?e:Ul(e),t]},useSyncExternalStore:Xu,useId:bd,useHostTransitionStatus:Ks,useFormState:nd,useActionState:nd,useOptimistic:function(e,t){var a=nt();return Ku(a,qe,e,t)},useMemoCache:Ls,useCacheRefresh:vd};Ws.useEffectEvent=sd;var Td={readContext:yt,use:Zo,useCallback:pd,useContext:yt,useEffect:Is,useImperativeHandle:dd,useInsertionEffect:rd,useLayoutEffect:cd,useMemo:hd,useReducer:qs,useRef:od,useState:function(){return qs(xn)},useDebugValue:Zs,useDeferredValue:function(e,t){var a=nt();return qe===null?Qs(a,e,t):fd(a,qe.memoizedState,e,t)},useTransition:function(){var e=qs(xn)[0],t=nt().memoizedState;return[typeof e=="boolean"?e:Ul(e),t]},useSyncExternalStore:Xu,useId:bd,useHostTransitionStatus:Ks,useFormState:ld,useActionState:ld,useOptimistic:function(e,t){var a=nt();return qe!==null?Ku(a,qe,e,t):(a.baseState=e,[e,a.queue.dispatch])},useMemoCache:Ls,useCacheRefresh:vd};Td.useEffectEvent=sd;function Ps(e,t,a,l){t=e.memoizedState,a=a(l,t),a=a==null?t:v({},t,a),e.memoizedState=a,e.lanes===0&&(e.updateQueue.baseState=a)}var Fs={enqueueSetState:function(e,t,a){e=e._reactInternals;var l=Lt(),o=qn(l);o.payload=t,a!=null&&(o.callback=a),t=Xn(e,o,l),t!==null&&(Ot(t,e,l),zl(t,e,l))},enqueueReplaceState:function(e,t,a){e=e._reactInternals;var l=Lt(),o=qn(l);o.tag=1,o.payload=t,a!=null&&(o.callback=a),t=Xn(e,o,l),t!==null&&(Ot(t,e,l),zl(t,e,l))},enqueueForceUpdate:function(e,t){e=e._reactInternals;var a=Lt(),l=qn(a);l.tag=2,t!=null&&(l.callback=t),t=Xn(e,l,a),t!==null&&(Ot(t,e,a),zl(t,e,a))}};function Ed(e,t,a,l,o,i,c){return e=e.stateNode,typeof e.shouldComponentUpdate=="function"?e.shouldComponentUpdate(l,i,c):t.prototype&&t.prototype.isPureReactComponent?!Nl(a,l)||!Nl(o,i):!0}function Ad(e,t,a,l){e=t.state,typeof t.componentWillReceiveProps=="function"&&t.componentWillReceiveProps(a,l),typeof t.UNSAFE_componentWillReceiveProps=="function"&&t.UNSAFE_componentWillReceiveProps(a,l),t.state!==e&&Fs.enqueueReplaceState(t,t.state,null)}function Ea(e,t){var a=t;if("ref"in t){a={};for(var l in t)l!=="ref"&&(a[l]=t[l])}if(e=e.defaultProps){a===t&&(a=v({},a));for(var o in e)a[o]===void 0&&(a[o]=e[o])}return a}function Nd(e){Co(e)}function Cd(e){console.error(e)}function jd(e){Co(e)}function Wo(e,t){try{var a=e.onUncaughtError;a(t.value,{componentStack:t.stack})}catch(l){setTimeout(function(){throw l})}}function Md(e,t,a){try{var l=e.onCaughtError;l(a.value,{componentStack:a.stack,errorBoundary:t.tag===1?t.stateNode:null})}catch(o){setTimeout(function(){throw o})}}function er(e,t,a){return a=qn(a),a.tag=3,a.payload={element:null},a.callback=function(){Wo(e,t)},a}function Dd(e){return e=qn(e),e.tag=3,e}function Od(e,t,a,l){var o=a.type.getDerivedStateFromError;if(typeof o=="function"){var i=l.value;e.payload=function(){return o(i)},e.callback=function(){Md(t,a,l)}}var c=a.stateNode;c!==null&&typeof c.componentDidCatch=="function"&&(e.callback=function(){Md(t,a,l),typeof o!="function"&&(Kn===null?Kn=new Set([this]):Kn.add(this));var d=l.stack;this.componentDidCatch(l.value,{componentStack:d!==null?d:""})})}function Nm(e,t,a,l,o){if(a.flags|=32768,l!==null&&typeof l=="object"&&typeof l.then=="function"){if(t=a.alternate,t!==null&&Qa(t,a,o,!0),a=Bt.current,a!==null){switch(a.tag){case 31:case 13:return $t===null?ci():a.alternate===null&&Pe===0&&(Pe=3),a.flags&=-257,a.flags|=65536,a.lanes=o,l===Go?a.flags|=16384:(t=a.updateQueue,t===null?a.updateQueue=new Set([l]):t.add(l),Er(e,l,o)),!1;case 22:return a.flags|=65536,l===Go?a.flags|=16384:(t=a.updateQueue,t===null?(t={transitions:null,markerInstances:null,retryQueue:new Set([l])},a.updateQueue=t):(a=t.retryQueue,a===null?t.retryQueue=new Set([l]):a.add(l)),Er(e,l,o)),!1}throw Error(u(435,a.tag))}return Er(e,l,o),ci(),!1}if(Me)return t=Bt.current,t!==null?((t.flags&65536)===0&&(t.flags|=256),t.flags|=65536,t.lanes=o,l!==vs&&(e=Error(u(422),{cause:l}),Ml(Zt(e,a)))):(l!==vs&&(t=Error(u(423),{cause:l}),Ml(Zt(t,a))),e=e.current.alternate,e.flags|=65536,o&=-o,e.lanes|=o,l=Zt(l,a),o=er(e.stateNode,l,o),Ms(e,o),Pe!==4&&(Pe=2)),!1;var i=Error(u(520),{cause:l});if(i=Zt(i,a),Kl===null?Kl=[i]:Kl.push(i),Pe!==4&&(Pe=2),t===null)return!0;l=Zt(l,a),a=t;do{switch(a.tag){case 3:return a.flags|=65536,e=o&-o,a.lanes|=e,e=er(a.stateNode,l,e),Ms(a,e),!1;case 1:if(t=a.type,i=a.stateNode,(a.flags&128)===0&&(typeof t.getDerivedStateFromError=="function"||i!==null&&typeof i.componentDidCatch=="function"&&(Kn===null||!Kn.has(i))))return a.flags|=65536,o&=-o,a.lanes|=o,o=Dd(o),Od(o,e,a,l),Ms(a,o),!1}a=a.return}while(a!==null);return!1}var tr=Error(u(461)),st=!1;function bt(e,t,a,l){t.child=e===null?Bu(t,null,a,l):wa(t,e.child,a,l)}function kd(e,t,a,l,o){a=a.render;var i=t.ref;if("ref"in l){var c={};for(var d in l)d!=="ref"&&(c[d]=l[d])}else c=l;return va(t),l=Bs(e,t,a,c,i,o),d=Hs(),e!==null&&!st?(Gs(e,t,o),wn(e,t,o)):(Me&&d&&ys(t),t.flags|=1,bt(e,t,l,o),t.child)}function Rd(e,t,a,l,o){if(e===null){var i=a.type;return typeof i=="function"&&!fs(i)&&i.defaultProps===void 0&&a.compare===null?(t.tag=15,t.type=i,zd(e,t,i,l,o)):(e=Oo(a.type,null,l,t,t.mode,o),e.ref=t.ref,e.return=t,t.child=e)}if(i=e.child,!cr(e,o)){var c=i.memoizedProps;if(a=a.compare,a=a!==null?a:Nl,a(c,l)&&e.ref===t.ref)return wn(e,t,o)}return t.flags|=1,e=yn(i,l),e.ref=t.ref,e.return=t,t.child=e}function zd(e,t,a,l,o){if(e!==null){var i=e.memoizedProps;if(Nl(i,l)&&e.ref===t.ref)if(st=!1,t.pendingProps=l=i,cr(e,o))(e.flags&131072)!==0&&(st=!0);else return t.lanes=e.lanes,wn(e,t,o)}return nr(e,t,a,l,o)}function Bd(e,t,a,l){var o=l.children,i=e!==null?e.memoizedState:null;if(e===null&&t.stateNode===null&&(t.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),l.mode==="hidden"){if((t.flags&128)!==0){if(i=i!==null?i.baseLanes|a:a,e!==null){for(l=t.child=e.child,o=0;l!==null;)o=o|l.lanes|l.childLanes,l=l.sibling;l=o&~i}else l=0,t.child=null;return Hd(e,t,i,a,l)}if((a&536870912)!==0)t.memoizedState={baseLanes:0,cachePool:null},e!==null&&Bo(t,i!==null?i.cachePool:null),i!==null?Uu(t,i):Os(),Lu(t);else return l=t.lanes=536870912,Hd(e,t,i!==null?i.baseLanes|a:a,a,l)}else i!==null?(Bo(t,i.cachePool),Uu(t,i),In(),t.memoizedState=null):(e!==null&&Bo(t,null),Os(),In());return bt(e,t,o,a),t.child}function ql(e,t){return e!==null&&e.tag===22||t.stateNode!==null||(t.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),t.sibling}function Hd(e,t,a,l,o){var i=As();return i=i===null?null:{parent:ot._currentValue,pool:i},t.memoizedState={baseLanes:a,cachePool:i},e!==null&&Bo(t,null),Os(),Lu(t),e!==null&&Qa(e,t,l,!0),t.childLanes=o,null}function Po(e,t){return t=ei({mode:t.mode,children:t.children},e.mode),t.ref=e.ref,e.child=t,t.return=e,t}function Gd(e,t,a){return wa(t,e.child,null,a),e=Po(t,t.pendingProps),e.flags|=2,Ht(t),t.memoizedState=null,e}function Cm(e,t,a){var l=t.pendingProps,o=(t.flags&128)!==0;if(t.flags&=-129,e===null){if(Me){if(l.mode==="hidden")return e=Po(t,l),t.lanes=536870912,ql(null,e);if(Rs(t),(e=Qe)?(e=$p(e,Kt),e=e!==null&&e.data==="&"?e:null,e!==null&&(t.memoizedState={dehydrated:e,treeContext:Hn!==null?{id:sn,overflow:rn}:null,retryLane:536870912,hydrationErrors:null},a=Su(e),a.return=t,t.child=a,gt=t,Qe=null)):e=null,e===null)throw Un(t);return t.lanes=536870912,null}return Po(t,l)}var i=e.memoizedState;if(i!==null){var c=i.dehydrated;if(Rs(t),o)if(t.flags&256)t.flags&=-257,t=Gd(e,t,a);else if(t.memoizedState!==null)t.child=e.child,t.flags|=128,t=null;else throw Error(u(558));else if(st||Qa(e,t,a,!1),o=(a&e.childLanes)!==0,st||o){if(l=Ie,l!==null&&(c=Cc(l,a),c!==0&&c!==i.retryLane))throw i.retryLane=c,ma(e,c),Ot(l,e,c),tr;ci(),t=Gd(e,t,a)}else e=i.treeContext,Qe=Wt(c.nextSibling),gt=t,Me=!0,Gn=null,Kt=!1,e!==null&&Tu(t,e),t=Po(t,l),t.flags|=4096;return t}return e=yn(e.child,{mode:l.mode,children:l.children}),e.ref=t.ref,t.child=e,e.return=t,e}function Fo(e,t){var a=t.ref;if(a===null)e!==null&&e.ref!==null&&(t.flags|=4194816);else{if(typeof a!="function"&&typeof a!="object")throw Error(u(284));(e===null||e.ref!==a)&&(t.flags|=4194816)}}function nr(e,t,a,l,o){return va(t),a=Bs(e,t,a,l,void 0,o),l=Hs(),e!==null&&!st?(Gs(e,t,o),wn(e,t,o)):(Me&&l&&ys(t),t.flags|=1,bt(e,t,a,o),t.child)}function Ud(e,t,a,l,o,i){return va(t),t.updateQueue=null,a=qu(t,l,a,o),Yu(e),l=Hs(),e!==null&&!st?(Gs(e,t,i),wn(e,t,i)):(Me&&l&&ys(t),t.flags|=1,bt(e,t,a,i),t.child)}function Ld(e,t,a,l,o){if(va(t),t.stateNode===null){var i=Xa,c=a.contextType;typeof c=="object"&&c!==null&&(i=yt(c)),i=new a(l,i),t.memoizedState=i.state!==null&&i.state!==void 0?i.state:null,i.updater=Fs,t.stateNode=i,i._reactInternals=t,i=t.stateNode,i.props=l,i.state=t.memoizedState,i.refs={},Cs(t),c=a.contextType,i.context=typeof c=="object"&&c!==null?yt(c):Xa,i.state=t.memoizedState,c=a.getDerivedStateFromProps,typeof c=="function"&&(Ps(t,a,c,l),i.state=t.memoizedState),typeof a.getDerivedStateFromProps=="function"||typeof i.getSnapshotBeforeUpdate=="function"||typeof i.UNSAFE_componentWillMount!="function"&&typeof i.componentWillMount!="function"||(c=i.state,typeof i.componentWillMount=="function"&&i.componentWillMount(),typeof i.UNSAFE_componentWillMount=="function"&&i.UNSAFE_componentWillMount(),c!==i.state&&Fs.enqueueReplaceState(i,i.state,null),Hl(t,l,i,o),Bl(),i.state=t.memoizedState),typeof i.componentDidMount=="function"&&(t.flags|=4194308),l=!0}else if(e===null){i=t.stateNode;var d=t.memoizedProps,S=Ea(a,d);i.props=S;var z=i.context,q=a.contextType;c=Xa,typeof q=="object"&&q!==null&&(c=yt(q));var Z=a.getDerivedStateFromProps;q=typeof Z=="function"||typeof i.getSnapshotBeforeUpdate=="function",d=t.pendingProps!==d,q||typeof i.UNSAFE_componentWillReceiveProps!="function"&&typeof i.componentWillReceiveProps!="function"||(d||z!==c)&&Ad(t,i,l,c),Yn=!1;var G=t.memoizedState;i.state=G,Hl(t,l,i,o),Bl(),z=t.memoizedState,d||G!==z||Yn?(typeof Z=="function"&&(Ps(t,a,Z,l),z=t.memoizedState),(S=Yn||Ed(t,a,S,l,G,z,c))?(q||typeof i.UNSAFE_componentWillMount!="function"&&typeof i.componentWillMount!="function"||(typeof i.componentWillMount=="function"&&i.componentWillMount(),typeof i.UNSAFE_componentWillMount=="function"&&i.UNSAFE_componentWillMount()),typeof i.componentDidMount=="function"&&(t.flags|=4194308)):(typeof i.componentDidMount=="function"&&(t.flags|=4194308),t.memoizedProps=l,t.memoizedState=z),i.props=l,i.state=z,i.context=c,l=S):(typeof i.componentDidMount=="function"&&(t.flags|=4194308),l=!1)}else{i=t.stateNode,js(e,t),c=t.memoizedProps,q=Ea(a,c),i.props=q,Z=t.pendingProps,G=i.context,z=a.contextType,S=Xa,typeof z=="object"&&z!==null&&(S=yt(z)),d=a.getDerivedStateFromProps,(z=typeof d=="function"||typeof i.getSnapshotBeforeUpdate=="function")||typeof i.UNSAFE_componentWillReceiveProps!="function"&&typeof i.componentWillReceiveProps!="function"||(c!==Z||G!==S)&&Ad(t,i,l,S),Yn=!1,G=t.memoizedState,i.state=G,Hl(t,l,i,o),Bl();var U=t.memoizedState;c!==Z||G!==U||Yn||e!==null&&e.dependencies!==null&&Ro(e.dependencies)?(typeof d=="function"&&(Ps(t,a,d,l),U=t.memoizedState),(q=Yn||Ed(t,a,q,l,G,U,S)||e!==null&&e.dependencies!==null&&Ro(e.dependencies))?(z||typeof i.UNSAFE_componentWillUpdate!="function"&&typeof i.componentWillUpdate!="function"||(typeof i.componentWillUpdate=="function"&&i.componentWillUpdate(l,U,S),typeof i.UNSAFE_componentWillUpdate=="function"&&i.UNSAFE_componentWillUpdate(l,U,S)),typeof i.componentDidUpdate=="function"&&(t.flags|=4),typeof i.getSnapshotBeforeUpdate=="function"&&(t.flags|=1024)):(typeof i.componentDidUpdate!="function"||c===e.memoizedProps&&G===e.memoizedState||(t.flags|=4),typeof i.getSnapshotBeforeUpdate!="function"||c===e.memoizedProps&&G===e.memoizedState||(t.flags|=1024),t.memoizedProps=l,t.memoizedState=U),i.props=l,i.state=U,i.context=S,l=q):(typeof i.componentDidUpdate!="function"||c===e.memoizedProps&&G===e.memoizedState||(t.flags|=4),typeof i.getSnapshotBeforeUpdate!="function"||c===e.memoizedProps&&G===e.memoizedState||(t.flags|=1024),l=!1)}return i=l,Fo(e,t),l=(t.flags&128)!==0,i||l?(i=t.stateNode,a=l&&typeof a.getDerivedStateFromError!="function"?null:i.render(),t.flags|=1,e!==null&&l?(t.child=wa(t,e.child,null,o),t.child=wa(t,null,a,o)):bt(e,t,a,o),t.memoizedState=i.state,e=t.child):e=wn(e,t,o),e}function Yd(e,t,a,l){return ya(),t.flags|=256,bt(e,t,a,l),t.child}var ar={dehydrated:null,treeContext:null,retryLane:0,hydrationErrors:null};function lr(e){return{baseLanes:e,cachePool:Mu()}}function or(e,t,a){return e=e!==null?e.childLanes&~a:0,t&&(e|=Ut),e}function qd(e,t,a){var l=t.pendingProps,o=!1,i=(t.flags&128)!==0,c;if((c=i)||(c=e!==null&&e.memoizedState===null?!1:(tt.current&2)!==0),c&&(o=!0,t.flags&=-129),c=(t.flags&32)!==0,t.flags&=-33,e===null){if(Me){if(o?Jn(t):In(),(e=Qe)?(e=$p(e,Kt),e=e!==null&&e.data!=="&"?e:null,e!==null&&(t.memoizedState={dehydrated:e,treeContext:Hn!==null?{id:sn,overflow:rn}:null,retryLane:536870912,hydrationErrors:null},a=Su(e),a.return=t,t.child=a,gt=t,Qe=null)):e=null,e===null)throw Un(t);return Yr(e)?t.lanes=32:t.lanes=536870912,null}var d=l.children;return l=l.fallback,o?(In(),o=t.mode,d=ei({mode:"hidden",children:d},o),l=ga(l,o,a,null),d.return=t,l.return=t,d.sibling=l,t.child=d,l=t.child,l.memoizedState=lr(a),l.childLanes=or(e,c,a),t.memoizedState=ar,ql(null,l)):(Jn(t),ir(t,d))}var S=e.memoizedState;if(S!==null&&(d=S.dehydrated,d!==null)){if(i)t.flags&256?(Jn(t),t.flags&=-257,t=sr(e,t,a)):t.memoizedState!==null?(In(),t.child=e.child,t.flags|=128,t=null):(In(),d=l.fallback,o=t.mode,l=ei({mode:"visible",children:l.children},o),d=ga(d,o,a,null),d.flags|=2,l.return=t,d.return=t,l.sibling=d,t.child=l,wa(t,e.child,null,a),l=t.child,l.memoizedState=lr(a),l.childLanes=or(e,c,a),t.memoizedState=ar,t=ql(null,l));else if(Jn(t),Yr(d)){if(c=d.nextSibling&&d.nextSibling.dataset,c)var z=c.dgst;c=z,l=Error(u(419)),l.stack="",l.digest=c,Ml({value:l,source:null,stack:null}),t=sr(e,t,a)}else if(st||Qa(e,t,a,!1),c=(a&e.childLanes)!==0,st||c){if(c=Ie,c!==null&&(l=Cc(c,a),l!==0&&l!==S.retryLane))throw S.retryLane=l,ma(e,l),Ot(c,e,l),tr;Lr(d)||ci(),t=sr(e,t,a)}else Lr(d)?(t.flags|=192,t.child=e.child,t=null):(e=S.treeContext,Qe=Wt(d.nextSibling),gt=t,Me=!0,Gn=null,Kt=!1,e!==null&&Tu(t,e),t=ir(t,l.children),t.flags|=4096);return t}return o?(In(),d=l.fallback,o=t.mode,S=e.child,z=S.sibling,l=yn(S,{mode:"hidden",children:l.children}),l.subtreeFlags=S.subtreeFlags&65011712,z!==null?d=yn(z,d):(d=ga(d,o,a,null),d.flags|=2),d.return=t,l.return=t,l.sibling=d,t.child=l,ql(null,l),l=t.child,d=e.child.memoizedState,d===null?d=lr(a):(o=d.cachePool,o!==null?(S=ot._currentValue,o=o.parent!==S?{parent:S,pool:S}:o):o=Mu(),d={baseLanes:d.baseLanes|a,cachePool:o}),l.memoizedState=d,l.childLanes=or(e,c,a),t.memoizedState=ar,ql(e.child,l)):(Jn(t),a=e.child,e=a.sibling,a=yn(a,{mode:"visible",children:l.children}),a.return=t,a.sibling=null,e!==null&&(c=t.deletions,c===null?(t.deletions=[e],t.flags|=16):c.push(e)),t.child=a,t.memoizedState=null,a)}function ir(e,t){return t=ei({mode:"visible",children:t},e.mode),t.return=e,e.child=t}function ei(e,t){return e=zt(22,e,null,t),e.lanes=0,e}function sr(e,t,a){return wa(t,e.child,null,a),e=ir(t,t.pendingProps.children),e.flags|=2,t.memoizedState=null,e}function Xd(e,t,a){e.lanes|=t;var l=e.alternate;l!==null&&(l.lanes|=t),xs(e.return,t,a)}function rr(e,t,a,l,o,i){var c=e.memoizedState;c===null?e.memoizedState={isBackwards:t,rendering:null,renderingStartTime:0,last:l,tail:a,tailMode:o,treeForkCount:i}:(c.isBackwards=t,c.rendering=null,c.renderingStartTime=0,c.last=l,c.tail=a,c.tailMode=o,c.treeForkCount=i)}function Jd(e,t,a){var l=t.pendingProps,o=l.revealOrder,i=l.tail;l=l.children;var c=tt.current,d=(c&2)!==0;if(d?(c=c&1|2,t.flags|=128):c&=1,ee(tt,c),bt(e,t,l,a),l=Me?jl:0,!d&&e!==null&&(e.flags&128)!==0)e:for(e=t.child;e!==null;){if(e.tag===13)e.memoizedState!==null&&Xd(e,a,t);else if(e.tag===19)Xd(e,a,t);else if(e.child!==null){e.child.return=e,e=e.child;continue}if(e===t)break e;for(;e.sibling===null;){if(e.return===null||e.return===t)break e;e=e.return}e.sibling.return=e.return,e=e.sibling}switch(o){case"forwards":for(a=t.child,o=null;a!==null;)e=a.alternate,e!==null&&qo(e)===null&&(o=a),a=a.sibling;a=o,a===null?(o=t.child,t.child=null):(o=a.sibling,a.sibling=null),rr(t,!1,o,a,i,l);break;case"backwards":case"unstable_legacy-backwards":for(a=null,o=t.child,t.child=null;o!==null;){if(e=o.alternate,e!==null&&qo(e)===null){t.child=o;break}e=o.sibling,o.sibling=a,a=o,o=e}rr(t,!0,a,null,i,l);break;case"together":rr(t,!1,null,null,void 0,l);break;default:t.memoizedState=null}return t.child}function wn(e,t,a){if(e!==null&&(t.dependencies=e.dependencies),Vn|=t.lanes,(a&t.childLanes)===0)if(e!==null){if(Qa(e,t,a,!1),(a&t.childLanes)===0)return null}else return null;if(e!==null&&t.child!==e.child)throw Error(u(153));if(t.child!==null){for(e=t.child,a=yn(e,e.pendingProps),t.child=a,a.return=t;e.sibling!==null;)e=e.sibling,a=a.sibling=yn(e,e.pendingProps),a.return=t;a.sibling=null}return t.child}function cr(e,t){return(e.lanes&t)!==0?!0:(e=e.dependencies,!!(e!==null&&Ro(e)))}function jm(e,t,a){switch(t.tag){case 3:He(t,t.stateNode.containerInfo),Ln(t,ot,e.memoizedState.cache),ya();break;case 27:case 5:ae(t);break;case 4:He(t,t.stateNode.containerInfo);break;case 10:Ln(t,t.type,t.memoizedProps.value);break;case 31:if(t.memoizedState!==null)return t.flags|=128,Rs(t),null;break;case 13:var l=t.memoizedState;if(l!==null)return l.dehydrated!==null?(Jn(t),t.flags|=128,null):(a&t.child.childLanes)!==0?qd(e,t,a):(Jn(t),e=wn(e,t,a),e!==null?e.sibling:null);Jn(t);break;case 19:var o=(e.flags&128)!==0;if(l=(a&t.childLanes)!==0,l||(Qa(e,t,a,!1),l=(a&t.childLanes)!==0),o){if(l)return Jd(e,t,a);t.flags|=128}if(o=t.memoizedState,o!==null&&(o.rendering=null,o.tail=null,o.lastEffect=null),ee(tt,tt.current),l)break;return null;case 22:return t.lanes=0,Bd(e,t,a,t.pendingProps);case 24:Ln(t,ot,e.memoizedState.cache)}return wn(e,t,a)}function Id(e,t,a){if(e!==null)if(e.memoizedProps!==t.pendingProps)st=!0;else{if(!cr(e,a)&&(t.flags&128)===0)return st=!1,jm(e,t,a);st=(e.flags&131072)!==0}else st=!1,Me&&(t.flags&1048576)!==0&&wu(t,jl,t.index);switch(t.lanes=0,t.tag){case 16:e:{var l=t.pendingProps;if(e=Sa(t.elementType),t.type=e,typeof e=="function")fs(e)?(l=Ea(e,l),t.tag=1,t=Ld(null,t,e,l,a)):(t.tag=0,t=nr(null,t,e,l,a));else{if(e!=null){var o=e.$$typeof;if(o===L){t.tag=11,t=kd(null,t,e,l,a);break e}else if(o===B){t.tag=14,t=Rd(null,t,e,l,a);break e}}throw t=he(e)||e,Error(u(306,t,""))}}return t;case 0:return nr(e,t,t.type,t.pendingProps,a);case 1:return l=t.type,o=Ea(l,t.pendingProps),Ld(e,t,l,o,a);case 3:e:{if(He(t,t.stateNode.containerInfo),e===null)throw Error(u(387));l=t.pendingProps;var i=t.memoizedState;o=i.element,js(e,t),Hl(t,l,null,a);var c=t.memoizedState;if(l=c.cache,Ln(t,ot,l),l!==i.cache&&ws(t,[ot],a,!0),Bl(),l=c.element,i.isDehydrated)if(i={element:l,isDehydrated:!1,cache:c.cache},t.updateQueue.baseState=i,t.memoizedState=i,t.flags&256){t=Yd(e,t,l,a);break e}else if(l!==o){o=Zt(Error(u(424)),t),Ml(o),t=Yd(e,t,l,a);break e}else{switch(e=t.stateNode.containerInfo,e.nodeType){case 9:e=e.body;break;default:e=e.nodeName==="HTML"?e.ownerDocument.body:e}for(Qe=Wt(e.firstChild),gt=t,Me=!0,Gn=null,Kt=!0,a=Bu(t,null,l,a),t.child=a;a;)a.flags=a.flags&-3|4096,a=a.sibling}else{if(ya(),l===o){t=wn(e,t,a);break e}bt(e,t,l,a)}t=t.child}return t;case 26:return Fo(e,t),e===null?(a=nh(t.type,null,t.pendingProps,null))?t.memoizedState=a:Me||(a=t.type,e=t.pendingProps,l=gi(ge.current).createElement(a),l[mt]=t,l[At]=e,vt(l,a,e),pt(l),t.stateNode=l):t.memoizedState=nh(t.type,e.memoizedProps,t.pendingProps,e.memoizedState),null;case 27:return ae(t),e===null&&Me&&(l=t.stateNode=Fp(t.type,t.pendingProps,ge.current),gt=t,Kt=!0,o=Qe,Fn(t.type)?(qr=o,Qe=Wt(l.firstChild)):Qe=o),bt(e,t,t.pendingProps.children,a),Fo(e,t),e===null&&(t.flags|=4194304),t.child;case 5:return e===null&&Me&&((o=l=Qe)&&(l=og(l,t.type,t.pendingProps,Kt),l!==null?(t.stateNode=l,gt=t,Qe=Wt(l.firstChild),Kt=!1,o=!0):o=!1),o||Un(t)),ae(t),o=t.type,i=t.pendingProps,c=e!==null?e.memoizedProps:null,l=i.children,Hr(o,i)?l=null:c!==null&&Hr(o,c)&&(t.flags|=32),t.memoizedState!==null&&(o=Bs(e,t,_m,null,null,a),ao._currentValue=o),Fo(e,t),bt(e,t,l,a),t.child;case 6:return e===null&&Me&&((e=a=Qe)&&(a=ig(a,t.pendingProps,Kt),a!==null?(t.stateNode=a,gt=t,Qe=null,e=!0):e=!1),e||Un(t)),null;case 13:return qd(e,t,a);case 4:return He(t,t.stateNode.containerInfo),l=t.pendingProps,e===null?t.child=wa(t,null,l,a):bt(e,t,l,a),t.child;case 11:return kd(e,t,t.type,t.pendingProps,a);case 7:return bt(e,t,t.pendingProps,a),t.child;case 8:return bt(e,t,t.pendingProps.children,a),t.child;case 12:return bt(e,t,t.pendingProps.children,a),t.child;case 10:return l=t.pendingProps,Ln(t,t.type,l.value),bt(e,t,l.children,a),t.child;case 9:return o=t.type._context,l=t.pendingProps.children,va(t),o=yt(o),l=l(o),t.flags|=1,bt(e,t,l,a),t.child;case 14:return Rd(e,t,t.type,t.pendingProps,a);case 15:return zd(e,t,t.type,t.pendingProps,a);case 19:return Jd(e,t,a);case 31:return Cm(e,t,a);case 22:return Bd(e,t,a,t.pendingProps);case 24:return va(t),l=yt(ot),e===null?(o=As(),o===null&&(o=Ie,i=Ts(),o.pooledCache=i,i.refCount++,i!==null&&(o.pooledCacheLanes|=a),o=i),t.memoizedState={parent:l,cache:o},Cs(t),Ln(t,ot,o)):((e.lanes&a)!==0&&(js(e,t),Hl(t,null,null,a),Bl()),o=e.memoizedState,i=t.memoizedState,o.parent!==l?(o={parent:l,cache:l},t.memoizedState=o,t.lanes===0&&(t.memoizedState=t.updateQueue.baseState=o),Ln(t,ot,l)):(l=i.cache,Ln(t,ot,l),l!==o.cache&&ws(t,[ot],a,!0))),bt(e,t,t.pendingProps.children,a),t.child;case 29:throw t.pendingProps}throw Error(u(156,t.tag))}function Tn(e){e.flags|=4}function ur(e,t,a,l,o){if((t=(e.mode&32)!==0)&&(t=!1),t){if(e.flags|=16777216,(o&335544128)===o)if(e.stateNode.complete)e.flags|=8192;else if(bp())e.flags|=8192;else throw xa=Go,Ns}else e.flags&=-16777217}function Zd(e,t){if(t.type!=="stylesheet"||(t.state.loading&4)!==0)e.flags&=-16777217;else if(e.flags|=16777216,!sh(t))if(bp())e.flags|=8192;else throw xa=Go,Ns}function ti(e,t){t!==null&&(e.flags|=4),e.flags&16384&&(t=e.tag!==22?Ec():536870912,e.lanes|=t,ol|=t)}function Xl(e,t){if(!Me)switch(e.tailMode){case"hidden":t=e.tail;for(var a=null;t!==null;)t.alternate!==null&&(a=t),t=t.sibling;a===null?e.tail=null:a.sibling=null;break;case"collapsed":a=e.tail;for(var l=null;a!==null;)a.alternate!==null&&(l=a),a=a.sibling;l===null?t||e.tail===null?e.tail=null:e.tail.sibling=null:l.sibling=null}}function Ve(e){var t=e.alternate!==null&&e.alternate.child===e.child,a=0,l=0;if(t)for(var o=e.child;o!==null;)a|=o.lanes|o.childLanes,l|=o.subtreeFlags&65011712,l|=o.flags&65011712,o.return=e,o=o.sibling;else for(o=e.child;o!==null;)a|=o.lanes|o.childLanes,l|=o.subtreeFlags,l|=o.flags,o.return=e,o=o.sibling;return e.subtreeFlags|=l,e.childLanes=a,t}function Mm(e,t,a){var l=t.pendingProps;switch(bs(t),t.tag){case 16:case 15:case 0:case 11:case 7:case 8:case 12:case 9:case 14:return Ve(t),null;case 1:return Ve(t),null;case 3:return a=t.stateNode,l=null,e!==null&&(l=e.memoizedState.cache),t.memoizedState.cache!==l&&(t.flags|=2048),_n(ot),we(),a.pendingContext&&(a.context=a.pendingContext,a.pendingContext=null),(e===null||e.child===null)&&(Za(t)?Tn(t):e===null||e.memoizedState.isDehydrated&&(t.flags&256)===0||(t.flags|=1024,_s())),Ve(t),null;case 26:var o=t.type,i=t.memoizedState;return e===null?(Tn(t),i!==null?(Ve(t),Zd(t,i)):(Ve(t),ur(t,o,null,l,a))):i?i!==e.memoizedState?(Tn(t),Ve(t),Zd(t,i)):(Ve(t),t.flags&=-16777217):(e=e.memoizedProps,e!==l&&Tn(t),Ve(t),ur(t,o,e,l,a)),null;case 27:if(de(t),a=ge.current,o=t.type,e!==null&&t.stateNode!=null)e.memoizedProps!==l&&Tn(t);else{if(!l){if(t.stateNode===null)throw Error(u(166));return Ve(t),null}e=ye.current,Za(t)?Eu(t):(e=Fp(o,l,a),t.stateNode=e,Tn(t))}return Ve(t),null;case 5:if(de(t),o=t.type,e!==null&&t.stateNode!=null)e.memoizedProps!==l&&Tn(t);else{if(!l){if(t.stateNode===null)throw Error(u(166));return Ve(t),null}if(i=ye.current,Za(t))Eu(t);else{var c=gi(ge.current);switch(i){case 1:i=c.createElementNS("http://www.w3.org/2000/svg",o);break;case 2:i=c.createElementNS("http://www.w3.org/1998/Math/MathML",o);break;default:switch(o){case"svg":i=c.createElementNS("http://www.w3.org/2000/svg",o);break;case"math":i=c.createElementNS("http://www.w3.org/1998/Math/MathML",o);break;case"script":i=c.createElement("div"),i.innerHTML="<script><\/script>",i=i.removeChild(i.firstChild);break;case"select":i=typeof l.is=="string"?c.createElement("select",{is:l.is}):c.createElement("select"),l.multiple?i.multiple=!0:l.size&&(i.size=l.size);break;default:i=typeof l.is=="string"?c.createElement(o,{is:l.is}):c.createElement(o)}}i[mt]=t,i[At]=l;e:for(c=t.child;c!==null;){if(c.tag===5||c.tag===6)i.appendChild(c.stateNode);else if(c.tag!==4&&c.tag!==27&&c.child!==null){c.child.return=c,c=c.child;continue}if(c===t)break e;for(;c.sibling===null;){if(c.return===null||c.return===t)break e;c=c.return}c.sibling.return=c.return,c=c.sibling}t.stateNode=i;e:switch(vt(i,o,l),o){case"button":case"input":case"select":case"textarea":l=!!l.autoFocus;break e;case"img":l=!0;break e;default:l=!1}l&&Tn(t)}}return Ve(t),ur(t,t.type,e===null?null:e.memoizedProps,t.pendingProps,a),null;case 6:if(e&&t.stateNode!=null)e.memoizedProps!==l&&Tn(t);else{if(typeof l!="string"&&t.stateNode===null)throw Error(u(166));if(e=ge.current,Za(t)){if(e=t.stateNode,a=t.memoizedProps,l=null,o=gt,o!==null)switch(o.tag){case 27:case 5:l=o.memoizedProps}e[mt]=t,e=!!(e.nodeValue===a||l!==null&&l.suppressHydrationWarning===!0||qp(e.nodeValue,a)),e||Un(t,!0)}else e=gi(e).createTextNode(l),e[mt]=t,t.stateNode=e}return Ve(t),null;case 31:if(a=t.memoizedState,e===null||e.memoizedState!==null){if(l=Za(t),a!==null){if(e===null){if(!l)throw Error(u(318));if(e=t.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(u(557));e[mt]=t}else ya(),(t.flags&128)===0&&(t.memoizedState=null),t.flags|=4;Ve(t),e=!1}else a=_s(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=a),e=!0;if(!e)return t.flags&256?(Ht(t),t):(Ht(t),null);if((t.flags&128)!==0)throw Error(u(558))}return Ve(t),null;case 13:if(l=t.memoizedState,e===null||e.memoizedState!==null&&e.memoizedState.dehydrated!==null){if(o=Za(t),l!==null&&l.dehydrated!==null){if(e===null){if(!o)throw Error(u(318));if(o=t.memoizedState,o=o!==null?o.dehydrated:null,!o)throw Error(u(317));o[mt]=t}else ya(),(t.flags&128)===0&&(t.memoizedState=null),t.flags|=4;Ve(t),o=!1}else o=_s(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=o),o=!0;if(!o)return t.flags&256?(Ht(t),t):(Ht(t),null)}return Ht(t),(t.flags&128)!==0?(t.lanes=a,t):(a=l!==null,e=e!==null&&e.memoizedState!==null,a&&(l=t.child,o=null,l.alternate!==null&&l.alternate.memoizedState!==null&&l.alternate.memoizedState.cachePool!==null&&(o=l.alternate.memoizedState.cachePool.pool),i=null,l.memoizedState!==null&&l.memoizedState.cachePool!==null&&(i=l.memoizedState.cachePool.pool),i!==o&&(l.flags|=2048)),a!==e&&a&&(t.child.flags|=8192),ti(t,t.updateQueue),Ve(t),null);case 4:return we(),e===null&&Or(t.stateNode.containerInfo),Ve(t),null;case 10:return _n(t.type),Ve(t),null;case 19:if(oe(tt),l=t.memoizedState,l===null)return Ve(t),null;if(o=(t.flags&128)!==0,i=l.rendering,i===null)if(o)Xl(l,!1);else{if(Pe!==0||e!==null&&(e.flags&128)!==0)for(e=t.child;e!==null;){if(i=qo(e),i!==null){for(t.flags|=128,Xl(l,!1),e=i.updateQueue,t.updateQueue=e,ti(t,e),t.subtreeFlags=0,e=a,a=t.child;a!==null;)_u(a,e),a=a.sibling;return ee(tt,tt.current&1|2),Me&&bn(t,l.treeForkCount),t.child}e=e.sibling}l.tail!==null&&ve()>ii&&(t.flags|=128,o=!0,Xl(l,!1),t.lanes=4194304)}else{if(!o)if(e=qo(i),e!==null){if(t.flags|=128,o=!0,e=e.updateQueue,t.updateQueue=e,ti(t,e),Xl(l,!0),l.tail===null&&l.tailMode==="hidden"&&!i.alternate&&!Me)return Ve(t),null}else 2*ve()-l.renderingStartTime>ii&&a!==536870912&&(t.flags|=128,o=!0,Xl(l,!1),t.lanes=4194304);l.isBackwards?(i.sibling=t.child,t.child=i):(e=l.last,e!==null?e.sibling=i:t.child=i,l.last=i)}return l.tail!==null?(e=l.tail,l.rendering=e,l.tail=e.sibling,l.renderingStartTime=ve(),e.sibling=null,a=tt.current,ee(tt,o?a&1|2:a&1),Me&&bn(t,l.treeForkCount),e):(Ve(t),null);case 22:case 23:return Ht(t),ks(),l=t.memoizedState!==null,e!==null?e.memoizedState!==null!==l&&(t.flags|=8192):l&&(t.flags|=8192),l?(a&536870912)!==0&&(t.flags&128)===0&&(Ve(t),t.subtreeFlags&6&&(t.flags|=8192)):Ve(t),a=t.updateQueue,a!==null&&ti(t,a.retryQueue),a=null,e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(a=e.memoizedState.cachePool.pool),l=null,t.memoizedState!==null&&t.memoizedState.cachePool!==null&&(l=t.memoizedState.cachePool.pool),l!==a&&(t.flags|=2048),e!==null&&oe(_a),null;case 24:return a=null,e!==null&&(a=e.memoizedState.cache),t.memoizedState.cache!==a&&(t.flags|=2048),_n(ot),Ve(t),null;case 25:return null;case 30:return null}throw Error(u(156,t.tag))}function Dm(e,t){switch(bs(t),t.tag){case 1:return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 3:return _n(ot),we(),e=t.flags,(e&65536)!==0&&(e&128)===0?(t.flags=e&-65537|128,t):null;case 26:case 27:case 5:return de(t),null;case 31:if(t.memoizedState!==null){if(Ht(t),t.alternate===null)throw Error(u(340));ya()}return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 13:if(Ht(t),e=t.memoizedState,e!==null&&e.dehydrated!==null){if(t.alternate===null)throw Error(u(340));ya()}return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 19:return oe(tt),null;case 4:return we(),null;case 10:return _n(t.type),null;case 22:case 23:return Ht(t),ks(),e!==null&&oe(_a),e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 24:return _n(ot),null;case 25:return null;default:return null}}function Qd(e,t){switch(bs(t),t.tag){case 3:_n(ot),we();break;case 26:case 27:case 5:de(t);break;case 4:we();break;case 31:t.memoizedState!==null&&Ht(t);break;case 13:Ht(t);break;case 19:oe(tt);break;case 10:_n(t.type);break;case 22:case 23:Ht(t),ks(),e!==null&&oe(_a);break;case 24:_n(ot)}}function Jl(e,t){try{var a=t.updateQueue,l=a!==null?a.lastEffect:null;if(l!==null){var o=l.next;a=o;do{if((a.tag&e)===e){l=void 0;var i=a.create,c=a.inst;l=i(),c.destroy=l}a=a.next}while(a!==o)}}catch(d){Le(t,t.return,d)}}function Zn(e,t,a){try{var l=t.updateQueue,o=l!==null?l.lastEffect:null;if(o!==null){var i=o.next;l=i;do{if((l.tag&e)===e){var c=l.inst,d=c.destroy;if(d!==void 0){c.destroy=void 0,o=t;var S=a,z=d;try{z()}catch(q){Le(o,S,q)}}}l=l.next}while(l!==i)}}catch(q){Le(t,t.return,q)}}function Vd(e){var t=e.updateQueue;if(t!==null){var a=e.stateNode;try{Gu(t,a)}catch(l){Le(e,e.return,l)}}}function Kd(e,t,a){a.props=Ea(e.type,e.memoizedProps),a.state=e.memoizedState;try{a.componentWillUnmount()}catch(l){Le(e,t,l)}}function Il(e,t){try{var a=e.ref;if(a!==null){switch(e.tag){case 26:case 27:case 5:var l=e.stateNode;break;case 30:l=e.stateNode;break;default:l=e.stateNode}typeof a=="function"?e.refCleanup=a(l):a.current=l}}catch(o){Le(e,t,o)}}function cn(e,t){var a=e.ref,l=e.refCleanup;if(a!==null)if(typeof l=="function")try{l()}catch(o){Le(e,t,o)}finally{e.refCleanup=null,e=e.alternate,e!=null&&(e.refCleanup=null)}else if(typeof a=="function")try{a(null)}catch(o){Le(e,t,o)}else a.current=null}function $d(e){var t=e.type,a=e.memoizedProps,l=e.stateNode;try{e:switch(t){case"button":case"input":case"select":case"textarea":a.autoFocus&&l.focus();break e;case"img":a.src?l.src=a.src:a.srcSet&&(l.srcset=a.srcSet)}}catch(o){Le(e,e.return,o)}}function dr(e,t,a){try{var l=e.stateNode;Fm(l,e.type,a,t),l[At]=t}catch(o){Le(e,e.return,o)}}function Wd(e){return e.tag===5||e.tag===3||e.tag===26||e.tag===27&&Fn(e.type)||e.tag===4}function pr(e){e:for(;;){for(;e.sibling===null;){if(e.return===null||Wd(e.return))return null;e=e.return}for(e.sibling.return=e.return,e=e.sibling;e.tag!==5&&e.tag!==6&&e.tag!==18;){if(e.tag===27&&Fn(e.type)||e.flags&2||e.child===null||e.tag===4)continue e;e.child.return=e,e=e.child}if(!(e.flags&2))return e.stateNode}}function hr(e,t,a){var l=e.tag;if(l===5||l===6)e=e.stateNode,t?(a.nodeType===9?a.body:a.nodeName==="HTML"?a.ownerDocument.body:a).insertBefore(e,t):(t=a.nodeType===9?a.body:a.nodeName==="HTML"?a.ownerDocument.body:a,t.appendChild(e),a=a._reactRootContainer,a!=null||t.onclick!==null||(t.onclick=mn));else if(l!==4&&(l===27&&Fn(e.type)&&(a=e.stateNode,t=null),e=e.child,e!==null))for(hr(e,t,a),e=e.sibling;e!==null;)hr(e,t,a),e=e.sibling}function ni(e,t,a){var l=e.tag;if(l===5||l===6)e=e.stateNode,t?a.insertBefore(e,t):a.appendChild(e);else if(l!==4&&(l===27&&Fn(e.type)&&(a=e.stateNode),e=e.child,e!==null))for(ni(e,t,a),e=e.sibling;e!==null;)ni(e,t,a),e=e.sibling}function Pd(e){var t=e.stateNode,a=e.memoizedProps;try{for(var l=e.type,o=t.attributes;o.length;)t.removeAttributeNode(o[0]);vt(t,l,a),t[mt]=e,t[At]=a}catch(i){Le(e,e.return,i)}}var En=!1,rt=!1,fr=!1,Fd=typeof WeakSet=="function"?WeakSet:Set,ht=null;function Om(e,t){if(e=e.containerInfo,zr=wi,e=du(e),ss(e)){if("selectionStart"in e)var a={start:e.selectionStart,end:e.selectionEnd};else e:{a=(a=e.ownerDocument)&&a.defaultView||window;var l=a.getSelection&&a.getSelection();if(l&&l.rangeCount!==0){a=l.anchorNode;var o=l.anchorOffset,i=l.focusNode;l=l.focusOffset;try{a.nodeType,i.nodeType}catch{a=null;break e}var c=0,d=-1,S=-1,z=0,q=0,Z=e,G=null;t:for(;;){for(var U;Z!==a||o!==0&&Z.nodeType!==3||(d=c+o),Z!==i||l!==0&&Z.nodeType!==3||(S=c+l),Z.nodeType===3&&(c+=Z.nodeValue.length),(U=Z.firstChild)!==null;)G=Z,Z=U;for(;;){if(Z===e)break t;if(G===a&&++z===o&&(d=c),G===i&&++q===l&&(S=c),(U=Z.nextSibling)!==null)break;Z=G,G=Z.parentNode}Z=U}a=d===-1||S===-1?null:{start:d,end:S}}else a=null}a=a||{start:0,end:0}}else a=null;for(Br={focusedElem:e,selectionRange:a},wi=!1,ht=t;ht!==null;)if(t=ht,e=t.child,(t.subtreeFlags&1028)!==0&&e!==null)e.return=t,ht=e;else for(;ht!==null;){switch(t=ht,i=t.alternate,e=t.flags,t.tag){case 0:if((e&4)!==0&&(e=t.updateQueue,e=e!==null?e.events:null,e!==null))for(a=0;a<e.length;a++)o=e[a],o.ref.impl=o.nextImpl;break;case 11:case 15:break;case 1:if((e&1024)!==0&&i!==null){e=void 0,a=t,o=i.memoizedProps,i=i.memoizedState,l=a.stateNode;try{var ne=Ea(a.type,o);e=l.getSnapshotBeforeUpdate(ne,i),l.__reactInternalSnapshotBeforeUpdate=e}catch(pe){Le(a,a.return,pe)}}break;case 3:if((e&1024)!==0){if(e=t.stateNode.containerInfo,a=e.nodeType,a===9)Ur(e);else if(a===1)switch(e.nodeName){case"HEAD":case"HTML":case"BODY":Ur(e);break;default:e.textContent=""}}break;case 5:case 26:case 27:case 6:case 4:case 17:break;default:if((e&1024)!==0)throw Error(u(163))}if(e=t.sibling,e!==null){e.return=t.return,ht=e;break}ht=t.return}}function ep(e,t,a){var l=a.flags;switch(a.tag){case 0:case 11:case 15:Nn(e,a),l&4&&Jl(5,a);break;case 1:if(Nn(e,a),l&4)if(e=a.stateNode,t===null)try{e.componentDidMount()}catch(c){Le(a,a.return,c)}else{var o=Ea(a.type,t.memoizedProps);t=t.memoizedState;try{e.componentDidUpdate(o,t,e.__reactInternalSnapshotBeforeUpdate)}catch(c){Le(a,a.return,c)}}l&64&&Vd(a),l&512&&Il(a,a.return);break;case 3:if(Nn(e,a),l&64&&(e=a.updateQueue,e!==null)){if(t=null,a.child!==null)switch(a.child.tag){case 27:case 5:t=a.child.stateNode;break;case 1:t=a.child.stateNode}try{Gu(e,t)}catch(c){Le(a,a.return,c)}}break;case 27:t===null&&l&4&&Pd(a);case 26:case 5:Nn(e,a),t===null&&l&4&&$d(a),l&512&&Il(a,a.return);break;case 12:Nn(e,a);break;case 31:Nn(e,a),l&4&&ap(e,a);break;case 13:Nn(e,a),l&4&&lp(e,a),l&64&&(e=a.memoizedState,e!==null&&(e=e.dehydrated,e!==null&&(a=Ym.bind(null,a),sg(e,a))));break;case 22:if(l=a.memoizedState!==null||En,!l){t=t!==null&&t.memoizedState!==null||rt,o=En;var i=rt;En=l,(rt=t)&&!i?Cn(e,a,(a.subtreeFlags&8772)!==0):Nn(e,a),En=o,rt=i}break;case 30:break;default:Nn(e,a)}}function tp(e){var t=e.alternate;t!==null&&(e.alternate=null,tp(t)),e.child=null,e.deletions=null,e.sibling=null,e.tag===5&&(t=e.stateNode,t!==null&&Ji(t)),e.stateNode=null,e.return=null,e.dependencies=null,e.memoizedProps=null,e.memoizedState=null,e.pendingProps=null,e.stateNode=null,e.updateQueue=null}var $e=null,Ct=!1;function An(e,t,a){for(a=a.child;a!==null;)np(e,t,a),a=a.sibling}function np(e,t,a){if(xt&&typeof xt.onCommitFiberUnmount=="function")try{xt.onCommitFiberUnmount(ca,a)}catch{}switch(a.tag){case 26:rt||cn(a,t),An(e,t,a),a.memoizedState?a.memoizedState.count--:a.stateNode&&(a=a.stateNode,a.parentNode.removeChild(a));break;case 27:rt||cn(a,t);var l=$e,o=Ct;Fn(a.type)&&($e=a.stateNode,Ct=!1),An(e,t,a),eo(a.stateNode),$e=l,Ct=o;break;case 5:rt||cn(a,t);case 6:if(l=$e,o=Ct,$e=null,An(e,t,a),$e=l,Ct=o,$e!==null)if(Ct)try{($e.nodeType===9?$e.body:$e.nodeName==="HTML"?$e.ownerDocument.body:$e).removeChild(a.stateNode)}catch(i){Le(a,t,i)}else try{$e.removeChild(a.stateNode)}catch(i){Le(a,t,i)}break;case 18:$e!==null&&(Ct?(e=$e,Vp(e.nodeType===9?e.body:e.nodeName==="HTML"?e.ownerDocument.body:e,a.stateNode),hl(e)):Vp($e,a.stateNode));break;case 4:l=$e,o=Ct,$e=a.stateNode.containerInfo,Ct=!0,An(e,t,a),$e=l,Ct=o;break;case 0:case 11:case 14:case 15:Zn(2,a,t),rt||Zn(4,a,t),An(e,t,a);break;case 1:rt||(cn(a,t),l=a.stateNode,typeof l.componentWillUnmount=="function"&&Kd(a,t,l)),An(e,t,a);break;case 21:An(e,t,a);break;case 22:rt=(l=rt)||a.memoizedState!==null,An(e,t,a),rt=l;break;default:An(e,t,a)}}function ap(e,t){if(t.memoizedState===null&&(e=t.alternate,e!==null&&(e=e.memoizedState,e!==null))){e=e.dehydrated;try{hl(e)}catch(a){Le(t,t.return,a)}}}function lp(e,t){if(t.memoizedState===null&&(e=t.alternate,e!==null&&(e=e.memoizedState,e!==null&&(e=e.dehydrated,e!==null))))try{hl(e)}catch(a){Le(t,t.return,a)}}function km(e){switch(e.tag){case 31:case 13:case 19:var t=e.stateNode;return t===null&&(t=e.stateNode=new Fd),t;case 22:return e=e.stateNode,t=e._retryCache,t===null&&(t=e._retryCache=new Fd),t;default:throw Error(u(435,e.tag))}}function ai(e,t){var a=km(e);t.forEach(function(l){if(!a.has(l)){a.add(l);var o=qm.bind(null,e,l);l.then(o,o)}})}function jt(e,t){var a=t.deletions;if(a!==null)for(var l=0;l<a.length;l++){var o=a[l],i=e,c=t,d=c;e:for(;d!==null;){switch(d.tag){case 27:if(Fn(d.type)){$e=d.stateNode,Ct=!1;break e}break;case 5:$e=d.stateNode,Ct=!1;break e;case 3:case 4:$e=d.stateNode.containerInfo,Ct=!0;break e}d=d.return}if($e===null)throw Error(u(160));np(i,c,o),$e=null,Ct=!1,i=o.alternate,i!==null&&(i.return=null),o.return=null}if(t.subtreeFlags&13886)for(t=t.child;t!==null;)op(t,e),t=t.sibling}var tn=null;function op(e,t){var a=e.alternate,l=e.flags;switch(e.tag){case 0:case 11:case 14:case 15:jt(t,e),Mt(e),l&4&&(Zn(3,e,e.return),Jl(3,e),Zn(5,e,e.return));break;case 1:jt(t,e),Mt(e),l&512&&(rt||a===null||cn(a,a.return)),l&64&&En&&(e=e.updateQueue,e!==null&&(l=e.callbacks,l!==null&&(a=e.shared.hiddenCallbacks,e.shared.hiddenCallbacks=a===null?l:a.concat(l))));break;case 26:var o=tn;if(jt(t,e),Mt(e),l&512&&(rt||a===null||cn(a,a.return)),l&4){var i=a!==null?a.memoizedState:null;if(l=e.memoizedState,a===null)if(l===null)if(e.stateNode===null){e:{l=e.type,a=e.memoizedProps,o=o.ownerDocument||o;t:switch(l){case"title":i=o.getElementsByTagName("title")[0],(!i||i[bl]||i[mt]||i.namespaceURI==="http://www.w3.org/2000/svg"||i.hasAttribute("itemprop"))&&(i=o.createElement(l),o.head.insertBefore(i,o.querySelector("head > title"))),vt(i,l,a),i[mt]=e,pt(i),l=i;break e;case"link":var c=oh("link","href",o).get(l+(a.href||""));if(c){for(var d=0;d<c.length;d++)if(i=c[d],i.getAttribute("href")===(a.href==null||a.href===""?null:a.href)&&i.getAttribute("rel")===(a.rel==null?null:a.rel)&&i.getAttribute("title")===(a.title==null?null:a.title)&&i.getAttribute("crossorigin")===(a.crossOrigin==null?null:a.crossOrigin)){c.splice(d,1);break t}}i=o.createElement(l),vt(i,l,a),o.head.appendChild(i);break;case"meta":if(c=oh("meta","content",o).get(l+(a.content||""))){for(d=0;d<c.length;d++)if(i=c[d],i.getAttribute("content")===(a.content==null?null:""+a.content)&&i.getAttribute("name")===(a.name==null?null:a.name)&&i.getAttribute("property")===(a.property==null?null:a.property)&&i.getAttribute("http-equiv")===(a.httpEquiv==null?null:a.httpEquiv)&&i.getAttribute("charset")===(a.charSet==null?null:a.charSet)){c.splice(d,1);break t}}i=o.createElement(l),vt(i,l,a),o.head.appendChild(i);break;default:throw Error(u(468,l))}i[mt]=e,pt(i),l=i}e.stateNode=l}else ih(o,e.type,e.stateNode);else e.stateNode=lh(o,l,e.memoizedProps);else i!==l?(i===null?a.stateNode!==null&&(a=a.stateNode,a.parentNode.removeChild(a)):i.count--,l===null?ih(o,e.type,e.stateNode):lh(o,l,e.memoizedProps)):l===null&&e.stateNode!==null&&dr(e,e.memoizedProps,a.memoizedProps)}break;case 27:jt(t,e),Mt(e),l&512&&(rt||a===null||cn(a,a.return)),a!==null&&l&4&&dr(e,e.memoizedProps,a.memoizedProps);break;case 5:if(jt(t,e),Mt(e),l&512&&(rt||a===null||cn(a,a.return)),e.flags&32){o=e.stateNode;try{Ba(o,"")}catch(ne){Le(e,e.return,ne)}}l&4&&e.stateNode!=null&&(o=e.memoizedProps,dr(e,o,a!==null?a.memoizedProps:o)),l&1024&&(fr=!0);break;case 6:if(jt(t,e),Mt(e),l&4){if(e.stateNode===null)throw Error(u(162));l=e.memoizedProps,a=e.stateNode;try{a.nodeValue=l}catch(ne){Le(e,e.return,ne)}}break;case 3:if(vi=null,o=tn,tn=yi(t.containerInfo),jt(t,e),tn=o,Mt(e),l&4&&a!==null&&a.memoizedState.isDehydrated)try{hl(t.containerInfo)}catch(ne){Le(e,e.return,ne)}fr&&(fr=!1,ip(e));break;case 4:l=tn,tn=yi(e.stateNode.containerInfo),jt(t,e),Mt(e),tn=l;break;case 12:jt(t,e),Mt(e);break;case 31:jt(t,e),Mt(e),l&4&&(l=e.updateQueue,l!==null&&(e.updateQueue=null,ai(e,l)));break;case 13:jt(t,e),Mt(e),e.child.flags&8192&&e.memoizedState!==null!=(a!==null&&a.memoizedState!==null)&&(oi=ve()),l&4&&(l=e.updateQueue,l!==null&&(e.updateQueue=null,ai(e,l)));break;case 22:o=e.memoizedState!==null;var S=a!==null&&a.memoizedState!==null,z=En,q=rt;if(En=z||o,rt=q||S,jt(t,e),rt=q,En=z,Mt(e),l&8192)e:for(t=e.stateNode,t._visibility=o?t._visibility&-2:t._visibility|1,o&&(a===null||S||En||rt||Aa(e)),a=null,t=e;;){if(t.tag===5||t.tag===26){if(a===null){S=a=t;try{if(i=S.stateNode,o)c=i.style,typeof c.setProperty=="function"?c.setProperty("display","none","important"):c.display="none";else{d=S.stateNode;var Z=S.memoizedProps.style,G=Z!=null&&Z.hasOwnProperty("display")?Z.display:null;d.style.display=G==null||typeof G=="boolean"?"":(""+G).trim()}}catch(ne){Le(S,S.return,ne)}}}else if(t.tag===6){if(a===null){S=t;try{S.stateNode.nodeValue=o?"":S.memoizedProps}catch(ne){Le(S,S.return,ne)}}}else if(t.tag===18){if(a===null){S=t;try{var U=S.stateNode;o?Kp(U,!0):Kp(S.stateNode,!1)}catch(ne){Le(S,S.return,ne)}}}else if((t.tag!==22&&t.tag!==23||t.memoizedState===null||t===e)&&t.child!==null){t.child.return=t,t=t.child;continue}if(t===e)break e;for(;t.sibling===null;){if(t.return===null||t.return===e)break e;a===t&&(a=null),t=t.return}a===t&&(a=null),t.sibling.return=t.return,t=t.sibling}l&4&&(l=e.updateQueue,l!==null&&(a=l.retryQueue,a!==null&&(l.retryQueue=null,ai(e,a))));break;case 19:jt(t,e),Mt(e),l&4&&(l=e.updateQueue,l!==null&&(e.updateQueue=null,ai(e,l)));break;case 30:break;case 21:break;default:jt(t,e),Mt(e)}}function Mt(e){var t=e.flags;if(t&2){try{for(var a,l=e.return;l!==null;){if(Wd(l)){a=l;break}l=l.return}if(a==null)throw Error(u(160));switch(a.tag){case 27:var o=a.stateNode,i=pr(e);ni(e,i,o);break;case 5:var c=a.stateNode;a.flags&32&&(Ba(c,""),a.flags&=-33);var d=pr(e);ni(e,d,c);break;case 3:case 4:var S=a.stateNode.containerInfo,z=pr(e);hr(e,z,S);break;default:throw Error(u(161))}}catch(q){Le(e,e.return,q)}e.flags&=-3}t&4096&&(e.flags&=-4097)}function ip(e){if(e.subtreeFlags&1024)for(e=e.child;e!==null;){var t=e;ip(t),t.tag===5&&t.flags&1024&&t.stateNode.reset(),e=e.sibling}}function Nn(e,t){if(t.subtreeFlags&8772)for(t=t.child;t!==null;)ep(e,t.alternate,t),t=t.sibling}function Aa(e){for(e=e.child;e!==null;){var t=e;switch(t.tag){case 0:case 11:case 14:case 15:Zn(4,t,t.return),Aa(t);break;case 1:cn(t,t.return);var a=t.stateNode;typeof a.componentWillUnmount=="function"&&Kd(t,t.return,a),Aa(t);break;case 27:eo(t.stateNode);case 26:case 5:cn(t,t.return),Aa(t);break;case 22:t.memoizedState===null&&Aa(t);break;case 30:Aa(t);break;default:Aa(t)}e=e.sibling}}function Cn(e,t,a){for(a=a&&(t.subtreeFlags&8772)!==0,t=t.child;t!==null;){var l=t.alternate,o=e,i=t,c=i.flags;switch(i.tag){case 0:case 11:case 15:Cn(o,i,a),Jl(4,i);break;case 1:if(Cn(o,i,a),l=i,o=l.stateNode,typeof o.componentDidMount=="function")try{o.componentDidMount()}catch(z){Le(l,l.return,z)}if(l=i,o=l.updateQueue,o!==null){var d=l.stateNode;try{var S=o.shared.hiddenCallbacks;if(S!==null)for(o.shared.hiddenCallbacks=null,o=0;o<S.length;o++)Hu(S[o],d)}catch(z){Le(l,l.return,z)}}a&&c&64&&Vd(i),Il(i,i.return);break;case 27:Pd(i);case 26:case 5:Cn(o,i,a),a&&l===null&&c&4&&$d(i),Il(i,i.return);break;case 12:Cn(o,i,a);break;case 31:Cn(o,i,a),a&&c&4&&ap(o,i);break;case 13:Cn(o,i,a),a&&c&4&&lp(o,i);break;case 22:i.memoizedState===null&&Cn(o,i,a),Il(i,i.return);break;case 30:break;default:Cn(o,i,a)}t=t.sibling}}function mr(e,t){var a=null;e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(a=e.memoizedState.cachePool.pool),e=null,t.memoizedState!==null&&t.memoizedState.cachePool!==null&&(e=t.memoizedState.cachePool.pool),e!==a&&(e!=null&&e.refCount++,a!=null&&Dl(a))}function gr(e,t){e=null,t.alternate!==null&&(e=t.alternate.memoizedState.cache),t=t.memoizedState.cache,t!==e&&(t.refCount++,e!=null&&Dl(e))}function nn(e,t,a,l){if(t.subtreeFlags&10256)for(t=t.child;t!==null;)sp(e,t,a,l),t=t.sibling}function sp(e,t,a,l){var o=t.flags;switch(t.tag){case 0:case 11:case 15:nn(e,t,a,l),o&2048&&Jl(9,t);break;case 1:nn(e,t,a,l);break;case 3:nn(e,t,a,l),o&2048&&(e=null,t.alternate!==null&&(e=t.alternate.memoizedState.cache),t=t.memoizedState.cache,t!==e&&(t.refCount++,e!=null&&Dl(e)));break;case 12:if(o&2048){nn(e,t,a,l),e=t.stateNode;try{var i=t.memoizedProps,c=i.id,d=i.onPostCommit;typeof d=="function"&&d(c,t.alternate===null?"mount":"update",e.passiveEffectDuration,-0)}catch(S){Le(t,t.return,S)}}else nn(e,t,a,l);break;case 31:nn(e,t,a,l);break;case 13:nn(e,t,a,l);break;case 23:break;case 22:i=t.stateNode,c=t.alternate,t.memoizedState!==null?i._visibility&2?nn(e,t,a,l):Zl(e,t):i._visibility&2?nn(e,t,a,l):(i._visibility|=2,nl(e,t,a,l,(t.subtreeFlags&10256)!==0||!1)),o&2048&&mr(c,t);break;case 24:nn(e,t,a,l),o&2048&&gr(t.alternate,t);break;default:nn(e,t,a,l)}}function nl(e,t,a,l,o){for(o=o&&((t.subtreeFlags&10256)!==0||!1),t=t.child;t!==null;){var i=e,c=t,d=a,S=l,z=c.flags;switch(c.tag){case 0:case 11:case 15:nl(i,c,d,S,o),Jl(8,c);break;case 23:break;case 22:var q=c.stateNode;c.memoizedState!==null?q._visibility&2?nl(i,c,d,S,o):Zl(i,c):(q._visibility|=2,nl(i,c,d,S,o)),o&&z&2048&&mr(c.alternate,c);break;case 24:nl(i,c,d,S,o),o&&z&2048&&gr(c.alternate,c);break;default:nl(i,c,d,S,o)}t=t.sibling}}function Zl(e,t){if(t.subtreeFlags&10256)for(t=t.child;t!==null;){var a=e,l=t,o=l.flags;switch(l.tag){case 22:Zl(a,l),o&2048&&mr(l.alternate,l);break;case 24:Zl(a,l),o&2048&&gr(l.alternate,l);break;default:Zl(a,l)}t=t.sibling}}var Ql=8192;function al(e,t,a){if(e.subtreeFlags&Ql)for(e=e.child;e!==null;)rp(e,t,a),e=e.sibling}function rp(e,t,a){switch(e.tag){case 26:al(e,t,a),e.flags&Ql&&e.memoizedState!==null&&vg(a,tn,e.memoizedState,e.memoizedProps);break;case 5:al(e,t,a);break;case 3:case 4:var l=tn;tn=yi(e.stateNode.containerInfo),al(e,t,a),tn=l;break;case 22:e.memoizedState===null&&(l=e.alternate,l!==null&&l.memoizedState!==null?(l=Ql,Ql=16777216,al(e,t,a),Ql=l):al(e,t,a));break;default:al(e,t,a)}}function cp(e){var t=e.alternate;if(t!==null&&(e=t.child,e!==null)){t.child=null;do t=e.sibling,e.sibling=null,e=t;while(e!==null)}}function Vl(e){var t=e.deletions;if((e.flags&16)!==0){if(t!==null)for(var a=0;a<t.length;a++){var l=t[a];ht=l,dp(l,e)}cp(e)}if(e.subtreeFlags&10256)for(e=e.child;e!==null;)up(e),e=e.sibling}function up(e){switch(e.tag){case 0:case 11:case 15:Vl(e),e.flags&2048&&Zn(9,e,e.return);break;case 3:Vl(e);break;case 12:Vl(e);break;case 22:var t=e.stateNode;e.memoizedState!==null&&t._visibility&2&&(e.return===null||e.return.tag!==13)?(t._visibility&=-3,li(e)):Vl(e);break;default:Vl(e)}}function li(e){var t=e.deletions;if((e.flags&16)!==0){if(t!==null)for(var a=0;a<t.length;a++){var l=t[a];ht=l,dp(l,e)}cp(e)}for(e=e.child;e!==null;){switch(t=e,t.tag){case 0:case 11:case 15:Zn(8,t,t.return),li(t);break;case 22:a=t.stateNode,a._visibility&2&&(a._visibility&=-3,li(t));break;default:li(t)}e=e.sibling}}function dp(e,t){for(;ht!==null;){var a=ht;switch(a.tag){case 0:case 11:case 15:Zn(8,a,t);break;case 23:case 22:if(a.memoizedState!==null&&a.memoizedState.cachePool!==null){var l=a.memoizedState.cachePool.pool;l!=null&&l.refCount++}break;case 24:Dl(a.memoizedState.cache)}if(l=a.child,l!==null)l.return=a,ht=l;else e:for(a=e;ht!==null;){l=ht;var o=l.sibling,i=l.return;if(tp(l),l===a){ht=null;break e}if(o!==null){o.return=i,ht=o;break e}ht=i}}}var Rm={getCacheForType:function(e){var t=yt(ot),a=t.data.get(e);return a===void 0&&(a=e(),t.data.set(e,a)),a},cacheSignal:function(){return yt(ot).controller.signal}},zm=typeof WeakMap=="function"?WeakMap:Map,Be=0,Ie=null,Ee=null,Ne=0,Ue=0,Gt=null,Qn=!1,ll=!1,yr=!1,jn=0,Pe=0,Vn=0,Na=0,br=0,Ut=0,ol=0,Kl=null,Dt=null,vr=!1,oi=0,pp=0,ii=1/0,si=null,Kn=null,ut=0,$n=null,il=null,Mn=0,_r=0,Sr=null,hp=null,$l=0,xr=null;function Lt(){return(Be&2)!==0&&Ne!==0?Ne&-Ne:D.T!==null?Cr():jc()}function fp(){if(Ut===0)if((Ne&536870912)===0||Me){var e=Rn;Rn<<=1,(Rn&3932160)===0&&(Rn=262144),Ut=e}else Ut=536870912;return e=Bt.current,e!==null&&(e.flags|=32),Ut}function Ot(e,t,a){(e===Ie&&(Ue===2||Ue===9)||e.cancelPendingCommit!==null)&&(sl(e,0),Wn(e,Ne,Ut,!1)),yl(e,a),((Be&2)===0||e!==Ie)&&(e===Ie&&((Be&2)===0&&(Na|=a),Pe===4&&Wn(e,Ne,Ut,!1)),un(e))}function mp(e,t,a){if((Be&6)!==0)throw Error(u(327));var l=!a&&(t&127)===0&&(t&e.expiredLanes)===0||gl(e,t),o=l?Gm(e,t):Tr(e,t,!0),i=l;do{if(o===0){ll&&!l&&Wn(e,t,0,!1);break}else{if(a=e.current.alternate,i&&!Bm(a)){o=Tr(e,t,!1),i=!1;continue}if(o===2){if(i=t,e.errorRecoveryDisabledLanes&i)var c=0;else c=e.pendingLanes&-536870913,c=c!==0?c:c&536870912?536870912:0;if(c!==0){t=c;e:{var d=e;o=Kl;var S=d.current.memoizedState.isDehydrated;if(S&&(sl(d,c).flags|=256),c=Tr(d,c,!1),c!==2){if(yr&&!S){d.errorRecoveryDisabledLanes|=i,Na|=i,o=4;break e}i=Dt,Dt=o,i!==null&&(Dt===null?Dt=i:Dt.push.apply(Dt,i))}o=c}if(i=!1,o!==2)continue}}if(o===1){sl(e,0),Wn(e,t,0,!0);break}e:{switch(l=e,i=o,i){case 0:case 1:throw Error(u(345));case 4:if((t&4194048)!==t)break;case 6:Wn(l,t,Ut,!Qn);break e;case 2:Dt=null;break;case 3:case 5:break;default:throw Error(u(329))}if((t&62914560)===t&&(o=oi+300-ve(),10<o)){if(Wn(l,t,Ut,!Qn),yo(l,0,!0)!==0)break e;Mn=t,l.timeoutHandle=Zp(gp.bind(null,l,a,Dt,si,vr,t,Ut,Na,ol,Qn,i,"Throttled",-0,0),o);break e}gp(l,a,Dt,si,vr,t,Ut,Na,ol,Qn,i,null,-0,0)}}break}while(!0);un(e)}function gp(e,t,a,l,o,i,c,d,S,z,q,Z,G,U){if(e.timeoutHandle=-1,Z=t.subtreeFlags,Z&8192||(Z&16785408)===16785408){Z={stylesheets:null,count:0,imgCount:0,imgBytes:0,suspenseyImages:[],waitingForImages:!0,waitingForViewTransition:!1,unsuspend:mn},rp(t,i,Z);var ne=(i&62914560)===i?oi-ve():(i&4194048)===i?pp-ve():0;if(ne=_g(Z,ne),ne!==null){Mn=i,e.cancelPendingCommit=ne(Tp.bind(null,e,t,i,a,l,o,c,d,S,q,Z,null,G,U)),Wn(e,i,c,!z);return}}Tp(e,t,i,a,l,o,c,d,S)}function Bm(e){for(var t=e;;){var a=t.tag;if((a===0||a===11||a===15)&&t.flags&16384&&(a=t.updateQueue,a!==null&&(a=a.stores,a!==null)))for(var l=0;l<a.length;l++){var o=a[l],i=o.getSnapshot;o=o.value;try{if(!Rt(i(),o))return!1}catch{return!1}}if(a=t.child,t.subtreeFlags&16384&&a!==null)a.return=t,t=a;else{if(t===e)break;for(;t.sibling===null;){if(t.return===null||t.return===e)return!0;t=t.return}t.sibling.return=t.return,t=t.sibling}}return!0}function Wn(e,t,a,l){t&=~br,t&=~Na,e.suspendedLanes|=t,e.pingedLanes&=~t,l&&(e.warmLanes|=t),l=e.expirationTimes;for(var o=t;0<o;){var i=31-wt(o),c=1<<i;l[i]=-1,o&=~c}a!==0&&Ac(e,a,t)}function ri(){return(Be&6)===0?(Wl(0),!1):!0}function wr(){if(Ee!==null){if(Ue===0)var e=Ee.return;else e=Ee,vn=ba=null,Us(e),Wa=null,kl=0,e=Ee;for(;e!==null;)Qd(e.alternate,e),e=e.return;Ee=null}}function sl(e,t){var a=e.timeoutHandle;a!==-1&&(e.timeoutHandle=-1,ng(a)),a=e.cancelPendingCommit,a!==null&&(e.cancelPendingCommit=null,a()),Mn=0,wr(),Ie=e,Ee=a=yn(e.current,null),Ne=t,Ue=0,Gt=null,Qn=!1,ll=gl(e,t),yr=!1,ol=Ut=br=Na=Vn=Pe=0,Dt=Kl=null,vr=!1,(t&8)!==0&&(t|=t&32);var l=e.entangledLanes;if(l!==0)for(e=e.entanglements,l&=t;0<l;){var o=31-wt(l),i=1<<o;t|=e[o],l&=~i}return jn=t,jo(),a}function yp(e,t){_e=null,D.H=Yl,t===$a||t===Ho?(t=ku(),Ue=3):t===Ns?(t=ku(),Ue=4):Ue=t===tr?8:t!==null&&typeof t=="object"&&typeof t.then=="function"?6:1,Gt=t,Ee===null&&(Pe=1,Wo(e,Zt(t,e.current)))}function bp(){var e=Bt.current;return e===null?!0:(Ne&4194048)===Ne?$t===null:(Ne&62914560)===Ne||(Ne&536870912)!==0?e===$t:!1}function vp(){var e=D.H;return D.H=Yl,e===null?Yl:e}function _p(){var e=D.A;return D.A=Rm,e}function ci(){Pe=4,Qn||(Ne&4194048)!==Ne&&Bt.current!==null||(ll=!0),(Vn&134217727)===0&&(Na&134217727)===0||Ie===null||Wn(Ie,Ne,Ut,!1)}function Tr(e,t,a){var l=Be;Be|=2;var o=vp(),i=_p();(Ie!==e||Ne!==t)&&(si=null,sl(e,t)),t=!1;var c=Pe;e:do try{if(Ue!==0&&Ee!==null){var d=Ee,S=Gt;switch(Ue){case 8:wr(),c=6;break e;case 3:case 2:case 9:case 6:Bt.current===null&&(t=!0);var z=Ue;if(Ue=0,Gt=null,rl(e,d,S,z),a&&ll){c=0;break e}break;default:z=Ue,Ue=0,Gt=null,rl(e,d,S,z)}}Hm(),c=Pe;break}catch(q){yp(e,q)}while(!0);return t&&e.shellSuspendCounter++,vn=ba=null,Be=l,D.H=o,D.A=i,Ee===null&&(Ie=null,Ne=0,jo()),c}function Hm(){for(;Ee!==null;)Sp(Ee)}function Gm(e,t){var a=Be;Be|=2;var l=vp(),o=_p();Ie!==e||Ne!==t?(si=null,ii=ve()+500,sl(e,t)):ll=gl(e,t);e:do try{if(Ue!==0&&Ee!==null){t=Ee;var i=Gt;t:switch(Ue){case 1:Ue=0,Gt=null,rl(e,t,i,1);break;case 2:case 9:if(Du(i)){Ue=0,Gt=null,xp(t);break}t=function(){Ue!==2&&Ue!==9||Ie!==e||(Ue=7),un(e)},i.then(t,t);break e;case 3:Ue=7;break e;case 4:Ue=5;break e;case 7:Du(i)?(Ue=0,Gt=null,xp(t)):(Ue=0,Gt=null,rl(e,t,i,7));break;case 5:var c=null;switch(Ee.tag){case 26:c=Ee.memoizedState;case 5:case 27:var d=Ee;if(c?sh(c):d.stateNode.complete){Ue=0,Gt=null;var S=d.sibling;if(S!==null)Ee=S;else{var z=d.return;z!==null?(Ee=z,ui(z)):Ee=null}break t}}Ue=0,Gt=null,rl(e,t,i,5);break;case 6:Ue=0,Gt=null,rl(e,t,i,6);break;case 8:wr(),Pe=6;break e;default:throw Error(u(462))}}Um();break}catch(q){yp(e,q)}while(!0);return vn=ba=null,D.H=l,D.A=o,Be=a,Ee!==null?0:(Ie=null,Ne=0,jo(),Pe)}function Um(){for(;Ee!==null&&!ct();)Sp(Ee)}function Sp(e){var t=Id(e.alternate,e,jn);e.memoizedProps=e.pendingProps,t===null?ui(e):Ee=t}function xp(e){var t=e,a=t.alternate;switch(t.tag){case 15:case 0:t=Ud(a,t,t.pendingProps,t.type,void 0,Ne);break;case 11:t=Ud(a,t,t.pendingProps,t.type.render,t.ref,Ne);break;case 5:Us(t);default:Qd(a,t),t=Ee=_u(t,jn),t=Id(a,t,jn)}e.memoizedProps=e.pendingProps,t===null?ui(e):Ee=t}function rl(e,t,a,l){vn=ba=null,Us(t),Wa=null,kl=0;var o=t.return;try{if(Nm(e,o,t,a,Ne)){Pe=1,Wo(e,Zt(a,e.current)),Ee=null;return}}catch(i){if(o!==null)throw Ee=o,i;Pe=1,Wo(e,Zt(a,e.current)),Ee=null;return}t.flags&32768?(Me||l===1?e=!0:ll||(Ne&536870912)!==0?e=!1:(Qn=e=!0,(l===2||l===9||l===3||l===6)&&(l=Bt.current,l!==null&&l.tag===13&&(l.flags|=16384))),wp(t,e)):ui(t)}function ui(e){var t=e;do{if((t.flags&32768)!==0){wp(t,Qn);return}e=t.return;var a=Mm(t.alternate,t,jn);if(a!==null){Ee=a;return}if(t=t.sibling,t!==null){Ee=t;return}Ee=t=e}while(t!==null);Pe===0&&(Pe=5)}function wp(e,t){do{var a=Dm(e.alternate,e);if(a!==null){a.flags&=32767,Ee=a;return}if(a=e.return,a!==null&&(a.flags|=32768,a.subtreeFlags=0,a.deletions=null),!t&&(e=e.sibling,e!==null)){Ee=e;return}Ee=e=a}while(e!==null);Pe=6,Ee=null}function Tp(e,t,a,l,o,i,c,d,S){e.cancelPendingCommit=null;do di();while(ut!==0);if((Be&6)!==0)throw Error(u(327));if(t!==null){if(t===e.current)throw Error(u(177));if(i=t.lanes|t.childLanes,i|=ps,bf(e,a,i,c,d,S),e===Ie&&(Ee=Ie=null,Ne=0),il=t,$n=e,Mn=a,_r=i,Sr=o,hp=l,(t.subtreeFlags&10256)!==0||(t.flags&10256)!==0?(e.callbackNode=null,e.callbackPriority=0,Xm(ra,function(){return jp(),null})):(e.callbackNode=null,e.callbackPriority=0),l=(t.flags&13878)!==0,(t.subtreeFlags&13878)!==0||l){l=D.T,D.T=null,o=O.p,O.p=2,c=Be,Be|=4;try{Om(e,t,a)}finally{Be=c,O.p=o,D.T=l}}ut=1,Ep(),Ap(),Np()}}function Ep(){if(ut===1){ut=0;var e=$n,t=il,a=(t.flags&13878)!==0;if((t.subtreeFlags&13878)!==0||a){a=D.T,D.T=null;var l=O.p;O.p=2;var o=Be;Be|=4;try{op(t,e);var i=Br,c=du(e.containerInfo),d=i.focusedElem,S=i.selectionRange;if(c!==d&&d&&d.ownerDocument&&uu(d.ownerDocument.documentElement,d)){if(S!==null&&ss(d)){var z=S.start,q=S.end;if(q===void 0&&(q=z),"selectionStart"in d)d.selectionStart=z,d.selectionEnd=Math.min(q,d.value.length);else{var Z=d.ownerDocument||document,G=Z&&Z.defaultView||window;if(G.getSelection){var U=G.getSelection(),ne=d.textContent.length,pe=Math.min(S.start,ne),Je=S.end===void 0?pe:Math.min(S.end,ne);!U.extend&&pe>Je&&(c=Je,Je=pe,pe=c);var M=cu(d,pe),A=cu(d,Je);if(M&&A&&(U.rangeCount!==1||U.anchorNode!==M.node||U.anchorOffset!==M.offset||U.focusNode!==A.node||U.focusOffset!==A.offset)){var R=Z.createRange();R.setStart(M.node,M.offset),U.removeAllRanges(),pe>Je?(U.addRange(R),U.extend(A.node,A.offset)):(R.setEnd(A.node,A.offset),U.addRange(R))}}}}for(Z=[],U=d;U=U.parentNode;)U.nodeType===1&&Z.push({element:U,left:U.scrollLeft,top:U.scrollTop});for(typeof d.focus=="function"&&d.focus(),d=0;d<Z.length;d++){var J=Z[d];J.element.scrollLeft=J.left,J.element.scrollTop=J.top}}wi=!!zr,Br=zr=null}finally{Be=o,O.p=l,D.T=a}}e.current=t,ut=2}}function Ap(){if(ut===2){ut=0;var e=$n,t=il,a=(t.flags&8772)!==0;if((t.subtreeFlags&8772)!==0||a){a=D.T,D.T=null;var l=O.p;O.p=2;var o=Be;Be|=4;try{ep(e,t.alternate,t)}finally{Be=o,O.p=l,D.T=a}}ut=3}}function Np(){if(ut===4||ut===3){ut=0,sa();var e=$n,t=il,a=Mn,l=hp;(t.subtreeFlags&10256)!==0||(t.flags&10256)!==0?ut=5:(ut=0,il=$n=null,Cp(e,e.pendingLanes));var o=e.pendingLanes;if(o===0&&(Kn=null),qi(a),t=t.stateNode,xt&&typeof xt.onCommitFiberRoot=="function")try{xt.onCommitFiberRoot(ca,t,void 0,(t.current.flags&128)===128)}catch{}if(l!==null){t=D.T,o=O.p,O.p=2,D.T=null;try{for(var i=e.onRecoverableError,c=0;c<l.length;c++){var d=l[c];i(d.value,{componentStack:d.stack})}}finally{D.T=t,O.p=o}}(Mn&3)!==0&&di(),un(e),o=e.pendingLanes,(a&261930)!==0&&(o&42)!==0?e===xr?$l++:($l=0,xr=e):$l=0,Wl(0)}}function Cp(e,t){(e.pooledCacheLanes&=t)===0&&(t=e.pooledCache,t!=null&&(e.pooledCache=null,Dl(t)))}function di(){return Ep(),Ap(),Np(),jp()}function jp(){if(ut!==5)return!1;var e=$n,t=_r;_r=0;var a=qi(Mn),l=D.T,o=O.p;try{O.p=32>a?32:a,D.T=null,a=Sr,Sr=null;var i=$n,c=Mn;if(ut=0,il=$n=null,Mn=0,(Be&6)!==0)throw Error(u(331));var d=Be;if(Be|=4,up(i.current),sp(i,i.current,c,a),Be=d,Wl(0,!1),xt&&typeof xt.onPostCommitFiberRoot=="function")try{xt.onPostCommitFiberRoot(ca,i)}catch{}return!0}finally{O.p=o,D.T=l,Cp(e,t)}}function Mp(e,t,a){t=Zt(a,t),t=er(e.stateNode,t,2),e=Xn(e,t,2),e!==null&&(yl(e,2),un(e))}function Le(e,t,a){if(e.tag===3)Mp(e,e,a);else for(;t!==null;){if(t.tag===3){Mp(t,e,a);break}else if(t.tag===1){var l=t.stateNode;if(typeof t.type.getDerivedStateFromError=="function"||typeof l.componentDidCatch=="function"&&(Kn===null||!Kn.has(l))){e=Zt(a,e),a=Dd(2),l=Xn(t,a,2),l!==null&&(Od(a,l,t,e),yl(l,2),un(l));break}}t=t.return}}function Er(e,t,a){var l=e.pingCache;if(l===null){l=e.pingCache=new zm;var o=new Set;l.set(t,o)}else o=l.get(t),o===void 0&&(o=new Set,l.set(t,o));o.has(a)||(yr=!0,o.add(a),e=Lm.bind(null,e,t,a),t.then(e,e))}function Lm(e,t,a){var l=e.pingCache;l!==null&&l.delete(t),e.pingedLanes|=e.suspendedLanes&a,e.warmLanes&=~a,Ie===e&&(Ne&a)===a&&(Pe===4||Pe===3&&(Ne&62914560)===Ne&&300>ve()-oi?(Be&2)===0&&sl(e,0):br|=a,ol===Ne&&(ol=0)),un(e)}function Dp(e,t){t===0&&(t=Ec()),e=ma(e,t),e!==null&&(yl(e,t),un(e))}function Ym(e){var t=e.memoizedState,a=0;t!==null&&(a=t.retryLane),Dp(e,a)}function qm(e,t){var a=0;switch(e.tag){case 31:case 13:var l=e.stateNode,o=e.memoizedState;o!==null&&(a=o.retryLane);break;case 19:l=e.stateNode;break;case 22:l=e.stateNode._retryCache;break;default:throw Error(u(314))}l!==null&&l.delete(t),Dp(e,a)}function Xm(e,t){return dt(e,t)}var pi=null,cl=null,Ar=!1,hi=!1,Nr=!1,Pn=0;function un(e){e!==cl&&e.next===null&&(cl===null?pi=cl=e:cl=cl.next=e),hi=!0,Ar||(Ar=!0,Im())}function Wl(e,t){if(!Nr&&hi){Nr=!0;do for(var a=!1,l=pi;l!==null;){if(e!==0){var o=l.pendingLanes;if(o===0)var i=0;else{var c=l.suspendedLanes,d=l.pingedLanes;i=(1<<31-wt(42|e)+1)-1,i&=o&~(c&~d),i=i&201326741?i&201326741|1:i?i|2:0}i!==0&&(a=!0,zp(l,i))}else i=Ne,i=yo(l,l===Ie?i:0,l.cancelPendingCommit!==null||l.timeoutHandle!==-1),(i&3)===0||gl(l,i)||(a=!0,zp(l,i));l=l.next}while(a);Nr=!1}}function Jm(){Op()}function Op(){hi=Ar=!1;var e=0;Pn!==0&&tg()&&(e=Pn);for(var t=ve(),a=null,l=pi;l!==null;){var o=l.next,i=kp(l,t);i===0?(l.next=null,a===null?pi=o:a.next=o,o===null&&(cl=a)):(a=l,(e!==0||(i&3)!==0)&&(hi=!0)),l=o}ut!==0&&ut!==5||Wl(e),Pn!==0&&(Pn=0)}function kp(e,t){for(var a=e.suspendedLanes,l=e.pingedLanes,o=e.expirationTimes,i=e.pendingLanes&-62914561;0<i;){var c=31-wt(i),d=1<<c,S=o[c];S===-1?((d&a)===0||(d&l)!==0)&&(o[c]=yf(d,t)):S<=t&&(e.expiredLanes|=d),i&=~d}if(t=Ie,a=Ne,a=yo(e,e===t?a:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),l=e.callbackNode,a===0||e===t&&(Ue===2||Ue===9)||e.cancelPendingCommit!==null)return l!==null&&l!==null&&Fe(l),e.callbackNode=null,e.callbackPriority=0;if((a&3)===0||gl(e,a)){if(t=a&-a,t===e.callbackPriority)return t;switch(l!==null&&Fe(l),qi(a)){case 2:case 8:a=mo;break;case 32:a=ra;break;case 268435456:a=go;break;default:a=ra}return l=Rp.bind(null,e),a=dt(a,l),e.callbackPriority=t,e.callbackNode=a,t}return l!==null&&l!==null&&Fe(l),e.callbackPriority=2,e.callbackNode=null,2}function Rp(e,t){if(ut!==0&&ut!==5)return e.callbackNode=null,e.callbackPriority=0,null;var a=e.callbackNode;if(di()&&e.callbackNode!==a)return null;var l=Ne;return l=yo(e,e===Ie?l:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),l===0?null:(mp(e,l,t),kp(e,ve()),e.callbackNode!=null&&e.callbackNode===a?Rp.bind(null,e):null)}function zp(e,t){if(di())return null;mp(e,t,!0)}function Im(){ag(function(){(Be&6)!==0?dt(fo,Jm):Op()})}function Cr(){if(Pn===0){var e=Va;e===0&&(e=et,et<<=1,(et&261888)===0&&(et=256)),Pn=e}return Pn}function Bp(e){return e==null||typeof e=="symbol"||typeof e=="boolean"?null:typeof e=="function"?e:So(""+e)}function Hp(e,t){var a=t.ownerDocument.createElement("input");return a.name=t.name,a.value=t.value,e.id&&a.setAttribute("form",e.id),t.parentNode.insertBefore(a,t),e=new FormData(e),a.parentNode.removeChild(a),e}function Zm(e,t,a,l,o){if(t==="submit"&&a&&a.stateNode===o){var i=Bp((o[At]||null).action),c=l.submitter;c&&(t=(t=c[At]||null)?Bp(t.formAction):c.getAttribute("formAction"),t!==null&&(i=t,c=null));var d=new Eo("action","action",null,l,o);e.push({event:d,listeners:[{instance:null,listener:function(){if(l.defaultPrevented){if(Pn!==0){var S=c?Hp(o,c):new FormData(o);Vs(a,{pending:!0,data:S,method:o.method,action:i},null,S)}}else typeof i=="function"&&(d.preventDefault(),S=c?Hp(o,c):new FormData(o),Vs(a,{pending:!0,data:S,method:o.method,action:i},i,S))},currentTarget:o}]})}}for(var jr=0;jr<ds.length;jr++){var Mr=ds[jr],Qm=Mr.toLowerCase(),Vm=Mr[0].toUpperCase()+Mr.slice(1);en(Qm,"on"+Vm)}en(fu,"onAnimationEnd"),en(mu,"onAnimationIteration"),en(gu,"onAnimationStart"),en("dblclick","onDoubleClick"),en("focusin","onFocus"),en("focusout","onBlur"),en(um,"onTransitionRun"),en(dm,"onTransitionStart"),en(pm,"onTransitionCancel"),en(yu,"onTransitionEnd"),Ra("onMouseEnter",["mouseout","mouseover"]),Ra("onMouseLeave",["mouseout","mouseover"]),Ra("onPointerEnter",["pointerout","pointerover"]),Ra("onPointerLeave",["pointerout","pointerover"]),da("onChange","change click focusin focusout input keydown keyup selectionchange".split(" ")),da("onSelect","focusout contextmenu dragend focusin keydown keyup mousedown mouseup selectionchange".split(" ")),da("onBeforeInput",["compositionend","keypress","textInput","paste"]),da("onCompositionEnd","compositionend focusout keydown keypress keyup mousedown".split(" ")),da("onCompositionStart","compositionstart focusout keydown keypress keyup mousedown".split(" ")),da("onCompositionUpdate","compositionupdate focusout keydown keypress keyup mousedown".split(" "));var Pl="abort canplay canplaythrough durationchange emptied encrypted ended error loadeddata loadedmetadata loadstart pause play playing progress ratechange resize seeked seeking stalled suspend timeupdate volumechange waiting".split(" "),Km=new Set("beforetoggle cancel close invalid load scroll scrollend toggle".split(" ").concat(Pl));function Gp(e,t){t=(t&4)!==0;for(var a=0;a<e.length;a++){var l=e[a],o=l.event;l=l.listeners;e:{var i=void 0;if(t)for(var c=l.length-1;0<=c;c--){var d=l[c],S=d.instance,z=d.currentTarget;if(d=d.listener,S!==i&&o.isPropagationStopped())break e;i=d,o.currentTarget=z;try{i(o)}catch(q){Co(q)}o.currentTarget=null,i=S}else for(c=0;c<l.length;c++){if(d=l[c],S=d.instance,z=d.currentTarget,d=d.listener,S!==i&&o.isPropagationStopped())break e;i=d,o.currentTarget=z;try{i(o)}catch(q){Co(q)}o.currentTarget=null,i=S}}}}function Ae(e,t){var a=t[Xi];a===void 0&&(a=t[Xi]=new Set);var l=e+"__bubble";a.has(l)||(Up(t,e,2,!1),a.add(l))}function Dr(e,t,a){var l=0;t&&(l|=4),Up(a,e,l,t)}var fi="_reactListening"+Math.random().toString(36).slice(2);function Or(e){if(!e[fi]){e[fi]=!0,Oc.forEach(function(a){a!=="selectionchange"&&(Km.has(a)||Dr(a,!1,e),Dr(a,!0,e))});var t=e.nodeType===9?e:e.ownerDocument;t===null||t[fi]||(t[fi]=!0,Dr("selectionchange",!1,t))}}function Up(e,t,a,l){switch(fh(t)){case 2:var o=wg;break;case 8:o=Tg;break;default:o=Qr}a=o.bind(null,t,a,e),o=void 0,!Pi||t!=="touchstart"&&t!=="touchmove"&&t!=="wheel"||(o=!0),l?o!==void 0?e.addEventListener(t,a,{capture:!0,passive:o}):e.addEventListener(t,a,!0):o!==void 0?e.addEventListener(t,a,{passive:o}):e.addEventListener(t,a,!1)}function kr(e,t,a,l,o){var i=l;if((t&1)===0&&(t&2)===0&&l!==null)e:for(;;){if(l===null)return;var c=l.tag;if(c===3||c===4){var d=l.stateNode.containerInfo;if(d===o)break;if(c===4)for(c=l.return;c!==null;){var S=c.tag;if((S===3||S===4)&&c.stateNode.containerInfo===o)return;c=c.return}for(;d!==null;){if(c=Da(d),c===null)return;if(S=c.tag,S===5||S===6||S===26||S===27){l=i=c;continue e}d=d.parentNode}}l=l.return}Jc(function(){var z=i,q=$i(a),Z=[];e:{var G=bu.get(e);if(G!==void 0){var U=Eo,ne=e;switch(e){case"keypress":if(wo(a)===0)break e;case"keydown":case"keyup":U=qf;break;case"focusin":ne="focus",U=ns;break;case"focusout":ne="blur",U=ns;break;case"beforeblur":case"afterblur":U=ns;break;case"click":if(a.button===2)break e;case"auxclick":case"dblclick":case"mousedown":case"mousemove":case"mouseup":case"mouseout":case"mouseover":case"contextmenu":U=Qc;break;case"drag":case"dragend":case"dragenter":case"dragexit":case"dragleave":case"dragover":case"dragstart":case"drop":U=Mf;break;case"touchcancel":case"touchend":case"touchmove":case"touchstart":U=If;break;case fu:case mu:case gu:U=kf;break;case yu:U=Qf;break;case"scroll":case"scrollend":U=Cf;break;case"wheel":U=Kf;break;case"copy":case"cut":case"paste":U=zf;break;case"gotpointercapture":case"lostpointercapture":case"pointercancel":case"pointerdown":case"pointermove":case"pointerout":case"pointerover":case"pointerup":U=Kc;break;case"toggle":case"beforetoggle":U=Wf}var pe=(t&4)!==0,Je=!pe&&(e==="scroll"||e==="scrollend"),M=pe?G!==null?G+"Capture":null:G;pe=[];for(var A=z,R;A!==null;){var J=A;if(R=J.stateNode,J=J.tag,J!==5&&J!==26&&J!==27||R===null||M===null||(J=_l(A,M),J!=null&&pe.push(Fl(A,J,R))),Je)break;A=A.return}0<pe.length&&(G=new U(G,ne,null,a,q),Z.push({event:G,listeners:pe}))}}if((t&7)===0){e:{if(G=e==="mouseover"||e==="pointerover",U=e==="mouseout"||e==="pointerout",G&&a!==Ki&&(ne=a.relatedTarget||a.fromElement)&&(Da(ne)||ne[Ma]))break e;if((U||G)&&(G=q.window===q?q:(G=q.ownerDocument)?G.defaultView||G.parentWindow:window,U?(ne=a.relatedTarget||a.toElement,U=z,ne=ne?Da(ne):null,ne!==null&&(Je=h(ne),pe=ne.tag,ne!==Je||pe!==5&&pe!==27&&pe!==6)&&(ne=null)):(U=null,ne=z),U!==ne)){if(pe=Qc,J="onMouseLeave",M="onMouseEnter",A="mouse",(e==="pointerout"||e==="pointerover")&&(pe=Kc,J="onPointerLeave",M="onPointerEnter",A="pointer"),Je=U==null?G:vl(U),R=ne==null?G:vl(ne),G=new pe(J,A+"leave",U,a,q),G.target=Je,G.relatedTarget=R,J=null,Da(q)===z&&(pe=new pe(M,A+"enter",ne,a,q),pe.target=R,pe.relatedTarget=Je,J=pe),Je=J,U&&ne)t:{for(pe=$m,M=U,A=ne,R=0,J=M;J;J=pe(J))R++;J=0;for(var ce=A;ce;ce=pe(ce))J++;for(;0<R-J;)M=pe(M),R--;for(;0<J-R;)A=pe(A),J--;for(;R--;){if(M===A||A!==null&&M===A.alternate){pe=M;break t}M=pe(M),A=pe(A)}pe=null}else pe=null;U!==null&&Lp(Z,G,U,pe,!1),ne!==null&&Je!==null&&Lp(Z,Je,ne,pe,!0)}}e:{if(G=z?vl(z):window,U=G.nodeName&&G.nodeName.toLowerCase(),U==="select"||U==="input"&&G.type==="file")var ke=au;else if(tu(G))if(lu)ke=sm;else{ke=om;var le=lm}else U=G.nodeName,!U||U.toLowerCase()!=="input"||G.type!=="checkbox"&&G.type!=="radio"?z&&Vi(z.elementType)&&(ke=au):ke=im;if(ke&&(ke=ke(e,z))){nu(Z,ke,a,q);break e}le&&le(e,G,z),e==="focusout"&&z&&G.type==="number"&&z.memoizedProps.value!=null&&Qi(G,"number",G.value)}switch(le=z?vl(z):window,e){case"focusin":(tu(le)||le.contentEditable==="true")&&(La=le,rs=z,Cl=null);break;case"focusout":Cl=rs=La=null;break;case"mousedown":cs=!0;break;case"contextmenu":case"mouseup":case"dragend":cs=!1,pu(Z,a,q);break;case"selectionchange":if(cm)break;case"keydown":case"keyup":pu(Z,a,q)}var Se;if(ls)e:{switch(e){case"compositionstart":var Ce="onCompositionStart";break e;case"compositionend":Ce="onCompositionEnd";break e;case"compositionupdate":Ce="onCompositionUpdate";break e}Ce=void 0}else Ua?Fc(e,a)&&(Ce="onCompositionEnd"):e==="keydown"&&a.keyCode===229&&(Ce="onCompositionStart");Ce&&($c&&a.locale!=="ko"&&(Ua||Ce!=="onCompositionStart"?Ce==="onCompositionEnd"&&Ua&&(Se=Ic()):(Bn=q,Fi="value"in Bn?Bn.value:Bn.textContent,Ua=!0)),le=mi(z,Ce),0<le.length&&(Ce=new Vc(Ce,e,null,a,q),Z.push({event:Ce,listeners:le}),Se?Ce.data=Se:(Se=eu(a),Se!==null&&(Ce.data=Se)))),(Se=Ff?em(e,a):tm(e,a))&&(Ce=mi(z,"onBeforeInput"),0<Ce.length&&(le=new Vc("onBeforeInput","beforeinput",null,a,q),Z.push({event:le,listeners:Ce}),le.data=Se)),Zm(Z,e,z,a,q)}Gp(Z,t)})}function Fl(e,t,a){return{instance:e,listener:t,currentTarget:a}}function mi(e,t){for(var a=t+"Capture",l=[];e!==null;){var o=e,i=o.stateNode;if(o=o.tag,o!==5&&o!==26&&o!==27||i===null||(o=_l(e,a),o!=null&&l.unshift(Fl(e,o,i)),o=_l(e,t),o!=null&&l.push(Fl(e,o,i))),e.tag===3)return l;e=e.return}return[]}function $m(e){if(e===null)return null;do e=e.return;while(e&&e.tag!==5&&e.tag!==27);return e||null}function Lp(e,t,a,l,o){for(var i=t._reactName,c=[];a!==null&&a!==l;){var d=a,S=d.alternate,z=d.stateNode;if(d=d.tag,S!==null&&S===l)break;d!==5&&d!==26&&d!==27||z===null||(S=z,o?(z=_l(a,i),z!=null&&c.unshift(Fl(a,z,S))):o||(z=_l(a,i),z!=null&&c.push(Fl(a,z,S)))),a=a.return}c.length!==0&&e.push({event:t,listeners:c})}var Wm=/\r\n?/g,Pm=/\u0000|\uFFFD/g;function Yp(e){return(typeof e=="string"?e:""+e).replace(Wm,`
`).replace(Pm,"")}function qp(e,t){return t=Yp(t),Yp(e)===t}function Xe(e,t,a,l,o,i){switch(a){case"children":typeof l=="string"?t==="body"||t==="textarea"&&l===""||Ba(e,l):(typeof l=="number"||typeof l=="bigint")&&t!=="body"&&Ba(e,""+l);break;case"className":vo(e,"class",l);break;case"tabIndex":vo(e,"tabindex",l);break;case"dir":case"role":case"viewBox":case"width":case"height":vo(e,a,l);break;case"style":qc(e,l,i);break;case"data":if(t!=="object"){vo(e,"data",l);break}case"src":case"href":if(l===""&&(t!=="a"||a!=="href")){e.removeAttribute(a);break}if(l==null||typeof l=="function"||typeof l=="symbol"||typeof l=="boolean"){e.removeAttribute(a);break}l=So(""+l),e.setAttribute(a,l);break;case"action":case"formAction":if(typeof l=="function"){e.setAttribute(a,"javascript:throw new Error('A React form was unexpectedly submitted. If you called form.submit() manually, consider using form.requestSubmit() instead. If you\\'re trying to use event.stopPropagation() in a submit event handler, consider also calling event.preventDefault().')");break}else typeof i=="function"&&(a==="formAction"?(t!=="input"&&Xe(e,t,"name",o.name,o,null),Xe(e,t,"formEncType",o.formEncType,o,null),Xe(e,t,"formMethod",o.formMethod,o,null),Xe(e,t,"formTarget",o.formTarget,o,null)):(Xe(e,t,"encType",o.encType,o,null),Xe(e,t,"method",o.method,o,null),Xe(e,t,"target",o.target,o,null)));if(l==null||typeof l=="symbol"||typeof l=="boolean"){e.removeAttribute(a);break}l=So(""+l),e.setAttribute(a,l);break;case"onClick":l!=null&&(e.onclick=mn);break;case"onScroll":l!=null&&Ae("scroll",e);break;case"onScrollEnd":l!=null&&Ae("scrollend",e);break;case"dangerouslySetInnerHTML":if(l!=null){if(typeof l!="object"||!("__html"in l))throw Error(u(61));if(a=l.__html,a!=null){if(o.children!=null)throw Error(u(60));e.innerHTML=a}}break;case"multiple":e.multiple=l&&typeof l!="function"&&typeof l!="symbol";break;case"muted":e.muted=l&&typeof l!="function"&&typeof l!="symbol";break;case"suppressContentEditableWarning":case"suppressHydrationWarning":case"defaultValue":case"defaultChecked":case"innerHTML":case"ref":break;case"autoFocus":break;case"xlinkHref":if(l==null||typeof l=="function"||typeof l=="boolean"||typeof l=="symbol"){e.removeAttribute("xlink:href");break}a=So(""+l),e.setAttributeNS("http://www.w3.org/1999/xlink","xlink:href",a);break;case"contentEditable":case"spellCheck":case"draggable":case"value":case"autoReverse":case"externalResourcesRequired":case"focusable":case"preserveAlpha":l!=null&&typeof l!="function"&&typeof l!="symbol"?e.setAttribute(a,""+l):e.removeAttribute(a);break;case"inert":case"allowFullScreen":case"async":case"autoPlay":case"controls":case"default":case"defer":case"disabled":case"disablePictureInPicture":case"disableRemotePlayback":case"formNoValidate":case"hidden":case"loop":case"noModule":case"noValidate":case"open":case"playsInline":case"readOnly":case"required":case"reversed":case"scoped":case"seamless":case"itemScope":l&&typeof l!="function"&&typeof l!="symbol"?e.setAttribute(a,""):e.removeAttribute(a);break;case"capture":case"download":l===!0?e.setAttribute(a,""):l!==!1&&l!=null&&typeof l!="function"&&typeof l!="symbol"?e.setAttribute(a,l):e.removeAttribute(a);break;case"cols":case"rows":case"size":case"span":l!=null&&typeof l!="function"&&typeof l!="symbol"&&!isNaN(l)&&1<=l?e.setAttribute(a,l):e.removeAttribute(a);break;case"rowSpan":case"start":l==null||typeof l=="function"||typeof l=="symbol"||isNaN(l)?e.removeAttribute(a):e.setAttribute(a,l);break;case"popover":Ae("beforetoggle",e),Ae("toggle",e),bo(e,"popover",l);break;case"xlinkActuate":fn(e,"http://www.w3.org/1999/xlink","xlink:actuate",l);break;case"xlinkArcrole":fn(e,"http://www.w3.org/1999/xlink","xlink:arcrole",l);break;case"xlinkRole":fn(e,"http://www.w3.org/1999/xlink","xlink:role",l);break;case"xlinkShow":fn(e,"http://www.w3.org/1999/xlink","xlink:show",l);break;case"xlinkTitle":fn(e,"http://www.w3.org/1999/xlink","xlink:title",l);break;case"xlinkType":fn(e,"http://www.w3.org/1999/xlink","xlink:type",l);break;case"xmlBase":fn(e,"http://www.w3.org/XML/1998/namespace","xml:base",l);break;case"xmlLang":fn(e,"http://www.w3.org/XML/1998/namespace","xml:lang",l);break;case"xmlSpace":fn(e,"http://www.w3.org/XML/1998/namespace","xml:space",l);break;case"is":bo(e,"is",l);break;case"innerText":case"textContent":break;default:(!(2<a.length)||a[0]!=="o"&&a[0]!=="O"||a[1]!=="n"&&a[1]!=="N")&&(a=Af.get(a)||a,bo(e,a,l))}}function Rr(e,t,a,l,o,i){switch(a){case"style":qc(e,l,i);break;case"dangerouslySetInnerHTML":if(l!=null){if(typeof l!="object"||!("__html"in l))throw Error(u(61));if(a=l.__html,a!=null){if(o.children!=null)throw Error(u(60));e.innerHTML=a}}break;case"children":typeof l=="string"?Ba(e,l):(typeof l=="number"||typeof l=="bigint")&&Ba(e,""+l);break;case"onScroll":l!=null&&Ae("scroll",e);break;case"onScrollEnd":l!=null&&Ae("scrollend",e);break;case"onClick":l!=null&&(e.onclick=mn);break;case"suppressContentEditableWarning":case"suppressHydrationWarning":case"innerHTML":case"ref":break;case"innerText":case"textContent":break;default:if(!kc.hasOwnProperty(a))e:{if(a[0]==="o"&&a[1]==="n"&&(o=a.endsWith("Capture"),t=a.slice(2,o?a.length-7:void 0),i=e[At]||null,i=i!=null?i[a]:null,typeof i=="function"&&e.removeEventListener(t,i,o),typeof l=="function")){typeof i!="function"&&i!==null&&(a in e?e[a]=null:e.hasAttribute(a)&&e.removeAttribute(a)),e.addEventListener(t,l,o);break e}a in e?e[a]=l:l===!0?e.setAttribute(a,""):bo(e,a,l)}}}function vt(e,t,a){switch(t){case"div":case"span":case"svg":case"path":case"a":case"g":case"p":case"li":break;case"img":Ae("error",e),Ae("load",e);var l=!1,o=!1,i;for(i in a)if(a.hasOwnProperty(i)){var c=a[i];if(c!=null)switch(i){case"src":l=!0;break;case"srcSet":o=!0;break;case"children":case"dangerouslySetInnerHTML":throw Error(u(137,t));default:Xe(e,t,i,c,a,null)}}o&&Xe(e,t,"srcSet",a.srcSet,a,null),l&&Xe(e,t,"src",a.src,a,null);return;case"input":Ae("invalid",e);var d=i=c=o=null,S=null,z=null;for(l in a)if(a.hasOwnProperty(l)){var q=a[l];if(q!=null)switch(l){case"name":o=q;break;case"type":c=q;break;case"checked":S=q;break;case"defaultChecked":z=q;break;case"value":i=q;break;case"defaultValue":d=q;break;case"children":case"dangerouslySetInnerHTML":if(q!=null)throw Error(u(137,t));break;default:Xe(e,t,l,q,a,null)}}Gc(e,i,d,S,z,c,o,!1);return;case"select":Ae("invalid",e),l=c=i=null;for(o in a)if(a.hasOwnProperty(o)&&(d=a[o],d!=null))switch(o){case"value":i=d;break;case"defaultValue":c=d;break;case"multiple":l=d;default:Xe(e,t,o,d,a,null)}t=i,a=c,e.multiple=!!l,t!=null?za(e,!!l,t,!1):a!=null&&za(e,!!l,a,!0);return;case"textarea":Ae("invalid",e),i=o=l=null;for(c in a)if(a.hasOwnProperty(c)&&(d=a[c],d!=null))switch(c){case"value":l=d;break;case"defaultValue":o=d;break;case"children":i=d;break;case"dangerouslySetInnerHTML":if(d!=null)throw Error(u(91));break;default:Xe(e,t,c,d,a,null)}Lc(e,l,o,i);return;case"option":for(S in a)if(a.hasOwnProperty(S)&&(l=a[S],l!=null))switch(S){case"selected":e.selected=l&&typeof l!="function"&&typeof l!="symbol";break;default:Xe(e,t,S,l,a,null)}return;case"dialog":Ae("beforetoggle",e),Ae("toggle",e),Ae("cancel",e),Ae("close",e);break;case"iframe":case"object":Ae("load",e);break;case"video":case"audio":for(l=0;l<Pl.length;l++)Ae(Pl[l],e);break;case"image":Ae("error",e),Ae("load",e);break;case"details":Ae("toggle",e);break;case"embed":case"source":case"link":Ae("error",e),Ae("load",e);case"area":case"base":case"br":case"col":case"hr":case"keygen":case"meta":case"param":case"track":case"wbr":case"menuitem":for(z in a)if(a.hasOwnProperty(z)&&(l=a[z],l!=null))switch(z){case"children":case"dangerouslySetInnerHTML":throw Error(u(137,t));default:Xe(e,t,z,l,a,null)}return;default:if(Vi(t)){for(q in a)a.hasOwnProperty(q)&&(l=a[q],l!==void 0&&Rr(e,t,q,l,a,void 0));return}}for(d in a)a.hasOwnProperty(d)&&(l=a[d],l!=null&&Xe(e,t,d,l,a,null))}function Fm(e,t,a,l){switch(t){case"div":case"span":case"svg":case"path":case"a":case"g":case"p":case"li":break;case"input":var o=null,i=null,c=null,d=null,S=null,z=null,q=null;for(U in a){var Z=a[U];if(a.hasOwnProperty(U)&&Z!=null)switch(U){case"checked":break;case"value":break;case"defaultValue":S=Z;default:l.hasOwnProperty(U)||Xe(e,t,U,null,l,Z)}}for(var G in l){var U=l[G];if(Z=a[G],l.hasOwnProperty(G)&&(U!=null||Z!=null))switch(G){case"type":i=U;break;case"name":o=U;break;case"checked":z=U;break;case"defaultChecked":q=U;break;case"value":c=U;break;case"defaultValue":d=U;break;case"children":case"dangerouslySetInnerHTML":if(U!=null)throw Error(u(137,t));break;default:U!==Z&&Xe(e,t,G,U,l,Z)}}Zi(e,c,d,S,z,q,i,o);return;case"select":U=c=d=G=null;for(i in a)if(S=a[i],a.hasOwnProperty(i)&&S!=null)switch(i){case"value":break;case"multiple":U=S;default:l.hasOwnProperty(i)||Xe(e,t,i,null,l,S)}for(o in l)if(i=l[o],S=a[o],l.hasOwnProperty(o)&&(i!=null||S!=null))switch(o){case"value":G=i;break;case"defaultValue":d=i;break;case"multiple":c=i;default:i!==S&&Xe(e,t,o,i,l,S)}t=d,a=c,l=U,G!=null?za(e,!!a,G,!1):!!l!=!!a&&(t!=null?za(e,!!a,t,!0):za(e,!!a,a?[]:"",!1));return;case"textarea":U=G=null;for(d in a)if(o=a[d],a.hasOwnProperty(d)&&o!=null&&!l.hasOwnProperty(d))switch(d){case"value":break;case"children":break;default:Xe(e,t,d,null,l,o)}for(c in l)if(o=l[c],i=a[c],l.hasOwnProperty(c)&&(o!=null||i!=null))switch(c){case"value":G=o;break;case"defaultValue":U=o;break;case"children":break;case"dangerouslySetInnerHTML":if(o!=null)throw Error(u(91));break;default:o!==i&&Xe(e,t,c,o,l,i)}Uc(e,G,U);return;case"option":for(var ne in a)if(G=a[ne],a.hasOwnProperty(ne)&&G!=null&&!l.hasOwnProperty(ne))switch(ne){case"selected":e.selected=!1;break;default:Xe(e,t,ne,null,l,G)}for(S in l)if(G=l[S],U=a[S],l.hasOwnProperty(S)&&G!==U&&(G!=null||U!=null))switch(S){case"selected":e.selected=G&&typeof G!="function"&&typeof G!="symbol";break;default:Xe(e,t,S,G,l,U)}return;case"img":case"link":case"area":case"base":case"br":case"col":case"embed":case"hr":case"keygen":case"meta":case"param":case"source":case"track":case"wbr":case"menuitem":for(var pe in a)G=a[pe],a.hasOwnProperty(pe)&&G!=null&&!l.hasOwnProperty(pe)&&Xe(e,t,pe,null,l,G);for(z in l)if(G=l[z],U=a[z],l.hasOwnProperty(z)&&G!==U&&(G!=null||U!=null))switch(z){case"children":case"dangerouslySetInnerHTML":if(G!=null)throw Error(u(137,t));break;default:Xe(e,t,z,G,l,U)}return;default:if(Vi(t)){for(var Je in a)G=a[Je],a.hasOwnProperty(Je)&&G!==void 0&&!l.hasOwnProperty(Je)&&Rr(e,t,Je,void 0,l,G);for(q in l)G=l[q],U=a[q],!l.hasOwnProperty(q)||G===U||G===void 0&&U===void 0||Rr(e,t,q,G,l,U);return}}for(var M in a)G=a[M],a.hasOwnProperty(M)&&G!=null&&!l.hasOwnProperty(M)&&Xe(e,t,M,null,l,G);for(Z in l)G=l[Z],U=a[Z],!l.hasOwnProperty(Z)||G===U||G==null&&U==null||Xe(e,t,Z,G,l,U)}function Xp(e){switch(e){case"css":case"script":case"font":case"img":case"image":case"input":case"link":return!0;default:return!1}}function eg(){if(typeof performance.getEntriesByType=="function"){for(var e=0,t=0,a=performance.getEntriesByType("resource"),l=0;l<a.length;l++){var o=a[l],i=o.transferSize,c=o.initiatorType,d=o.duration;if(i&&d&&Xp(c)){for(c=0,d=o.responseEnd,l+=1;l<a.length;l++){var S=a[l],z=S.startTime;if(z>d)break;var q=S.transferSize,Z=S.initiatorType;q&&Xp(Z)&&(S=S.responseEnd,c+=q*(S<d?1:(d-z)/(S-z)))}if(--l,t+=8*(i+c)/(o.duration/1e3),e++,10<e)break}}if(0<e)return t/e/1e6}return navigator.connection&&(e=navigator.connection.downlink,typeof e=="number")?e:5}var zr=null,Br=null;function gi(e){return e.nodeType===9?e:e.ownerDocument}function Jp(e){switch(e){case"http://www.w3.org/2000/svg":return 1;case"http://www.w3.org/1998/Math/MathML":return 2;default:return 0}}function Ip(e,t){if(e===0)switch(t){case"svg":return 1;case"math":return 2;default:return 0}return e===1&&t==="foreignObject"?0:e}function Hr(e,t){return e==="textarea"||e==="noscript"||typeof t.children=="string"||typeof t.children=="number"||typeof t.children=="bigint"||typeof t.dangerouslySetInnerHTML=="object"&&t.dangerouslySetInnerHTML!==null&&t.dangerouslySetInnerHTML.__html!=null}var Gr=null;function tg(){var e=window.event;return e&&e.type==="popstate"?e===Gr?!1:(Gr=e,!0):(Gr=null,!1)}var Zp=typeof setTimeout=="function"?setTimeout:void 0,ng=typeof clearTimeout=="function"?clearTimeout:void 0,Qp=typeof Promise=="function"?Promise:void 0,ag=typeof queueMicrotask=="function"?queueMicrotask:typeof Qp<"u"?function(e){return Qp.resolve(null).then(e).catch(lg)}:Zp;function lg(e){setTimeout(function(){throw e})}function Fn(e){return e==="head"}function Vp(e,t){var a=t,l=0;do{var o=a.nextSibling;if(e.removeChild(a),o&&o.nodeType===8)if(a=o.data,a==="/$"||a==="/&"){if(l===0){e.removeChild(o),hl(t);return}l--}else if(a==="$"||a==="$?"||a==="$~"||a==="$!"||a==="&")l++;else if(a==="html")eo(e.ownerDocument.documentElement);else if(a==="head"){a=e.ownerDocument.head,eo(a);for(var i=a.firstChild;i;){var c=i.nextSibling,d=i.nodeName;i[bl]||d==="SCRIPT"||d==="STYLE"||d==="LINK"&&i.rel.toLowerCase()==="stylesheet"||a.removeChild(i),i=c}}else a==="body"&&eo(e.ownerDocument.body);a=o}while(a);hl(t)}function Kp(e,t){var a=e;e=0;do{var l=a.nextSibling;if(a.nodeType===1?t?(a._stashedDisplay=a.style.display,a.style.display="none"):(a.style.display=a._stashedDisplay||"",a.getAttribute("style")===""&&a.removeAttribute("style")):a.nodeType===3&&(t?(a._stashedText=a.nodeValue,a.nodeValue=""):a.nodeValue=a._stashedText||""),l&&l.nodeType===8)if(a=l.data,a==="/$"){if(e===0)break;e--}else a!=="$"&&a!=="$?"&&a!=="$~"&&a!=="$!"||e++;a=l}while(a)}function Ur(e){var t=e.firstChild;for(t&&t.nodeType===10&&(t=t.nextSibling);t;){var a=t;switch(t=t.nextSibling,a.nodeName){case"HTML":case"HEAD":case"BODY":Ur(a),Ji(a);continue;case"SCRIPT":case"STYLE":continue;case"LINK":if(a.rel.toLowerCase()==="stylesheet")continue}e.removeChild(a)}}function og(e,t,a,l){for(;e.nodeType===1;){var o=a;if(e.nodeName.toLowerCase()!==t.toLowerCase()){if(!l&&(e.nodeName!=="INPUT"||e.type!=="hidden"))break}else if(l){if(!e[bl])switch(t){case"meta":if(!e.hasAttribute("itemprop"))break;return e;case"link":if(i=e.getAttribute("rel"),i==="stylesheet"&&e.hasAttribute("data-precedence"))break;if(i!==o.rel||e.getAttribute("href")!==(o.href==null||o.href===""?null:o.href)||e.getAttribute("crossorigin")!==(o.crossOrigin==null?null:o.crossOrigin)||e.getAttribute("title")!==(o.title==null?null:o.title))break;return e;case"style":if(e.hasAttribute("data-precedence"))break;return e;case"script":if(i=e.getAttribute("src"),(i!==(o.src==null?null:o.src)||e.getAttribute("type")!==(o.type==null?null:o.type)||e.getAttribute("crossorigin")!==(o.crossOrigin==null?null:o.crossOrigin))&&i&&e.hasAttribute("async")&&!e.hasAttribute("itemprop"))break;return e;default:return e}}else if(t==="input"&&e.type==="hidden"){var i=o.name==null?null:""+o.name;if(o.type==="hidden"&&e.getAttribute("name")===i)return e}else return e;if(e=Wt(e.nextSibling),e===null)break}return null}function ig(e,t,a){if(t==="")return null;for(;e.nodeType!==3;)if((e.nodeType!==1||e.nodeName!=="INPUT"||e.type!=="hidden")&&!a||(e=Wt(e.nextSibling),e===null))return null;return e}function $p(e,t){for(;e.nodeType!==8;)if((e.nodeType!==1||e.nodeName!=="INPUT"||e.type!=="hidden")&&!t||(e=Wt(e.nextSibling),e===null))return null;return e}function Lr(e){return e.data==="$?"||e.data==="$~"}function Yr(e){return e.data==="$!"||e.data==="$?"&&e.ownerDocument.readyState!=="loading"}function sg(e,t){var a=e.ownerDocument;if(e.data==="$~")e._reactRetry=t;else if(e.data!=="$?"||a.readyState!=="loading")t();else{var l=function(){t(),a.removeEventListener("DOMContentLoaded",l)};a.addEventListener("DOMContentLoaded",l),e._reactRetry=l}}function Wt(e){for(;e!=null;e=e.nextSibling){var t=e.nodeType;if(t===1||t===3)break;if(t===8){if(t=e.data,t==="$"||t==="$!"||t==="$?"||t==="$~"||t==="&"||t==="F!"||t==="F")break;if(t==="/$"||t==="/&")return null}}return e}var qr=null;function Wp(e){e=e.nextSibling;for(var t=0;e;){if(e.nodeType===8){var a=e.data;if(a==="/$"||a==="/&"){if(t===0)return Wt(e.nextSibling);t--}else a!=="$"&&a!=="$!"&&a!=="$?"&&a!=="$~"&&a!=="&"||t++}e=e.nextSibling}return null}function Pp(e){e=e.previousSibling;for(var t=0;e;){if(e.nodeType===8){var a=e.data;if(a==="$"||a==="$!"||a==="$?"||a==="$~"||a==="&"){if(t===0)return e;t--}else a!=="/$"&&a!=="/&"||t++}e=e.previousSibling}return null}function Fp(e,t,a){switch(t=gi(a),e){case"html":if(e=t.documentElement,!e)throw Error(u(452));return e;case"head":if(e=t.head,!e)throw Error(u(453));return e;case"body":if(e=t.body,!e)throw Error(u(454));return e;default:throw Error(u(451))}}function eo(e){for(var t=e.attributes;t.length;)e.removeAttributeNode(t[0]);Ji(e)}var Pt=new Map,eh=new Set;function yi(e){return typeof e.getRootNode=="function"?e.getRootNode():e.nodeType===9?e:e.ownerDocument}var Dn=O.d;O.d={f:rg,r:cg,D:ug,C:dg,L:pg,m:hg,X:mg,S:fg,M:gg};function rg(){var e=Dn.f(),t=ri();return e||t}function cg(e){var t=Oa(e);t!==null&&t.tag===5&&t.type==="form"?yd(t):Dn.r(e)}var ul=typeof document>"u"?null:document;function th(e,t,a){var l=ul;if(l&&typeof t=="string"&&t){var o=Jt(t);o='link[rel="'+e+'"][href="'+o+'"]',typeof a=="string"&&(o+='[crossorigin="'+a+'"]'),eh.has(o)||(eh.add(o),e={rel:e,crossOrigin:a,href:t},l.querySelector(o)===null&&(t=l.createElement("link"),vt(t,"link",e),pt(t),l.head.appendChild(t)))}}function ug(e){Dn.D(e),th("dns-prefetch",e,null)}function dg(e,t){Dn.C(e,t),th("preconnect",e,t)}function pg(e,t,a){Dn.L(e,t,a);var l=ul;if(l&&e&&t){var o='link[rel="preload"][as="'+Jt(t)+'"]';t==="image"&&a&&a.imageSrcSet?(o+='[imagesrcset="'+Jt(a.imageSrcSet)+'"]',typeof a.imageSizes=="string"&&(o+='[imagesizes="'+Jt(a.imageSizes)+'"]')):o+='[href="'+Jt(e)+'"]';var i=o;switch(t){case"style":i=dl(e);break;case"script":i=pl(e)}Pt.has(i)||(e=v({rel:"preload",href:t==="image"&&a&&a.imageSrcSet?void 0:e,as:t},a),Pt.set(i,e),l.querySelector(o)!==null||t==="style"&&l.querySelector(to(i))||t==="script"&&l.querySelector(no(i))||(t=l.createElement("link"),vt(t,"link",e),pt(t),l.head.appendChild(t)))}}function hg(e,t){Dn.m(e,t);var a=ul;if(a&&e){var l=t&&typeof t.as=="string"?t.as:"script",o='link[rel="modulepreload"][as="'+Jt(l)+'"][href="'+Jt(e)+'"]',i=o;switch(l){case"audioworklet":case"paintworklet":case"serviceworker":case"sharedworker":case"worker":case"script":i=pl(e)}if(!Pt.has(i)&&(e=v({rel:"modulepreload",href:e},t),Pt.set(i,e),a.querySelector(o)===null)){switch(l){case"audioworklet":case"paintworklet":case"serviceworker":case"sharedworker":case"worker":case"script":if(a.querySelector(no(i)))return}l=a.createElement("link"),vt(l,"link",e),pt(l),a.head.appendChild(l)}}}function fg(e,t,a){Dn.S(e,t,a);var l=ul;if(l&&e){var o=ka(l).hoistableStyles,i=dl(e);t=t||"default";var c=o.get(i);if(!c){var d={loading:0,preload:null};if(c=l.querySelector(to(i)))d.loading=5;else{e=v({rel:"stylesheet",href:e,"data-precedence":t},a),(a=Pt.get(i))&&Xr(e,a);var S=c=l.createElement("link");pt(S),vt(S,"link",e),S._p=new Promise(function(z,q){S.onload=z,S.onerror=q}),S.addEventListener("load",function(){d.loading|=1}),S.addEventListener("error",function(){d.loading|=2}),d.loading|=4,bi(c,t,l)}c={type:"stylesheet",instance:c,count:1,state:d},o.set(i,c)}}}function mg(e,t){Dn.X(e,t);var a=ul;if(a&&e){var l=ka(a).hoistableScripts,o=pl(e),i=l.get(o);i||(i=a.querySelector(no(o)),i||(e=v({src:e,async:!0},t),(t=Pt.get(o))&&Jr(e,t),i=a.createElement("script"),pt(i),vt(i,"link",e),a.head.appendChild(i)),i={type:"script",instance:i,count:1,state:null},l.set(o,i))}}function gg(e,t){Dn.M(e,t);var a=ul;if(a&&e){var l=ka(a).hoistableScripts,o=pl(e),i=l.get(o);i||(i=a.querySelector(no(o)),i||(e=v({src:e,async:!0,type:"module"},t),(t=Pt.get(o))&&Jr(e,t),i=a.createElement("script"),pt(i),vt(i,"link",e),a.head.appendChild(i)),i={type:"script",instance:i,count:1,state:null},l.set(o,i))}}function nh(e,t,a,l){var o=(o=ge.current)?yi(o):null;if(!o)throw Error(u(446));switch(e){case"meta":case"title":return null;case"style":return typeof a.precedence=="string"&&typeof a.href=="string"?(t=dl(a.href),a=ka(o).hoistableStyles,l=a.get(t),l||(l={type:"style",instance:null,count:0,state:null},a.set(t,l)),l):{type:"void",instance:null,count:0,state:null};case"link":if(a.rel==="stylesheet"&&typeof a.href=="string"&&typeof a.precedence=="string"){e=dl(a.href);var i=ka(o).hoistableStyles,c=i.get(e);if(c||(o=o.ownerDocument||o,c={type:"stylesheet",instance:null,count:0,state:{loading:0,preload:null}},i.set(e,c),(i=o.querySelector(to(e)))&&!i._p&&(c.instance=i,c.state.loading=5),Pt.has(e)||(a={rel:"preload",as:"style",href:a.href,crossOrigin:a.crossOrigin,integrity:a.integrity,media:a.media,hrefLang:a.hrefLang,referrerPolicy:a.referrerPolicy},Pt.set(e,a),i||yg(o,e,a,c.state))),t&&l===null)throw Error(u(528,""));return c}if(t&&l!==null)throw Error(u(529,""));return null;case"script":return t=a.async,a=a.src,typeof a=="string"&&t&&typeof t!="function"&&typeof t!="symbol"?(t=pl(a),a=ka(o).hoistableScripts,l=a.get(t),l||(l={type:"script",instance:null,count:0,state:null},a.set(t,l)),l):{type:"void",instance:null,count:0,state:null};default:throw Error(u(444,e))}}function dl(e){return'href="'+Jt(e)+'"'}function to(e){return'link[rel="stylesheet"]['+e+"]"}function ah(e){return v({},e,{"data-precedence":e.precedence,precedence:null})}function yg(e,t,a,l){e.querySelector('link[rel="preload"][as="style"]['+t+"]")?l.loading=1:(t=e.createElement("link"),l.preload=t,t.addEventListener("load",function(){return l.loading|=1}),t.addEventListener("error",function(){return l.loading|=2}),vt(t,"link",a),pt(t),e.head.appendChild(t))}function pl(e){return'[src="'+Jt(e)+'"]'}function no(e){return"script[async]"+e}function lh(e,t,a){if(t.count++,t.instance===null)switch(t.type){case"style":var l=e.querySelector('style[data-href~="'+Jt(a.href)+'"]');if(l)return t.instance=l,pt(l),l;var o=v({},a,{"data-href":a.href,"data-precedence":a.precedence,href:null,precedence:null});return l=(e.ownerDocument||e).createElement("style"),pt(l),vt(l,"style",o),bi(l,a.precedence,e),t.instance=l;case"stylesheet":o=dl(a.href);var i=e.querySelector(to(o));if(i)return t.state.loading|=4,t.instance=i,pt(i),i;l=ah(a),(o=Pt.get(o))&&Xr(l,o),i=(e.ownerDocument||e).createElement("link"),pt(i);var c=i;return c._p=new Promise(function(d,S){c.onload=d,c.onerror=S}),vt(i,"link",l),t.state.loading|=4,bi(i,a.precedence,e),t.instance=i;case"script":return i=pl(a.src),(o=e.querySelector(no(i)))?(t.instance=o,pt(o),o):(l=a,(o=Pt.get(i))&&(l=v({},a),Jr(l,o)),e=e.ownerDocument||e,o=e.createElement("script"),pt(o),vt(o,"link",l),e.head.appendChild(o),t.instance=o);case"void":return null;default:throw Error(u(443,t.type))}else t.type==="stylesheet"&&(t.state.loading&4)===0&&(l=t.instance,t.state.loading|=4,bi(l,a.precedence,e));return t.instance}function bi(e,t,a){for(var l=a.querySelectorAll('link[rel="stylesheet"][data-precedence],style[data-precedence]'),o=l.length?l[l.length-1]:null,i=o,c=0;c<l.length;c++){var d=l[c];if(d.dataset.precedence===t)i=d;else if(i!==o)break}i?i.parentNode.insertBefore(e,i.nextSibling):(t=a.nodeType===9?a.head:a,t.insertBefore(e,t.firstChild))}function Xr(e,t){e.crossOrigin==null&&(e.crossOrigin=t.crossOrigin),e.referrerPolicy==null&&(e.referrerPolicy=t.referrerPolicy),e.title==null&&(e.title=t.title)}function Jr(e,t){e.crossOrigin==null&&(e.crossOrigin=t.crossOrigin),e.referrerPolicy==null&&(e.referrerPolicy=t.referrerPolicy),e.integrity==null&&(e.integrity=t.integrity)}var vi=null;function oh(e,t,a){if(vi===null){var l=new Map,o=vi=new Map;o.set(a,l)}else o=vi,l=o.get(a),l||(l=new Map,o.set(a,l));if(l.has(e))return l;for(l.set(e,null),a=a.getElementsByTagName(e),o=0;o<a.length;o++){var i=a[o];if(!(i[bl]||i[mt]||e==="link"&&i.getAttribute("rel")==="stylesheet")&&i.namespaceURI!=="http://www.w3.org/2000/svg"){var c=i.getAttribute(t)||"";c=e+c;var d=l.get(c);d?d.push(i):l.set(c,[i])}}return l}function ih(e,t,a){e=e.ownerDocument||e,e.head.insertBefore(a,t==="title"?e.querySelector("head > title"):null)}function bg(e,t,a){if(a===1||t.itemProp!=null)return!1;switch(e){case"meta":case"title":return!0;case"style":if(typeof t.precedence!="string"||typeof t.href!="string"||t.href==="")break;return!0;case"link":if(typeof t.rel!="string"||typeof t.href!="string"||t.href===""||t.onLoad||t.onError)break;switch(t.rel){case"stylesheet":return e=t.disabled,typeof t.precedence=="string"&&e==null;default:return!0}case"script":if(t.async&&typeof t.async!="function"&&typeof t.async!="symbol"&&!t.onLoad&&!t.onError&&t.src&&typeof t.src=="string")return!0}return!1}function sh(e){return!(e.type==="stylesheet"&&(e.state.loading&3)===0)}function vg(e,t,a,l){if(a.type==="stylesheet"&&(typeof l.media!="string"||matchMedia(l.media).matches!==!1)&&(a.state.loading&4)===0){if(a.instance===null){var o=dl(l.href),i=t.querySelector(to(o));if(i){t=i._p,t!==null&&typeof t=="object"&&typeof t.then=="function"&&(e.count++,e=_i.bind(e),t.then(e,e)),a.state.loading|=4,a.instance=i,pt(i);return}i=t.ownerDocument||t,l=ah(l),(o=Pt.get(o))&&Xr(l,o),i=i.createElement("link"),pt(i);var c=i;c._p=new Promise(function(d,S){c.onload=d,c.onerror=S}),vt(i,"link",l),a.instance=i}e.stylesheets===null&&(e.stylesheets=new Map),e.stylesheets.set(a,t),(t=a.state.preload)&&(a.state.loading&3)===0&&(e.count++,a=_i.bind(e),t.addEventListener("load",a),t.addEventListener("error",a))}}var Ir=0;function _g(e,t){return e.stylesheets&&e.count===0&&xi(e,e.stylesheets),0<e.count||0<e.imgCount?function(a){var l=setTimeout(function(){if(e.stylesheets&&xi(e,e.stylesheets),e.unsuspend){var i=e.unsuspend;e.unsuspend=null,i()}},6e4+t);0<e.imgBytes&&Ir===0&&(Ir=62500*eg());var o=setTimeout(function(){if(e.waitingForImages=!1,e.count===0&&(e.stylesheets&&xi(e,e.stylesheets),e.unsuspend)){var i=e.unsuspend;e.unsuspend=null,i()}},(e.imgBytes>Ir?50:800)+t);return e.unsuspend=a,function(){e.unsuspend=null,clearTimeout(l),clearTimeout(o)}}:null}function _i(){if(this.count--,this.count===0&&(this.imgCount===0||!this.waitingForImages)){if(this.stylesheets)xi(this,this.stylesheets);else if(this.unsuspend){var e=this.unsuspend;this.unsuspend=null,e()}}}var Si=null;function xi(e,t){e.stylesheets=null,e.unsuspend!==null&&(e.count++,Si=new Map,t.forEach(Sg,e),Si=null,_i.call(e))}function Sg(e,t){if(!(t.state.loading&4)){var a=Si.get(e);if(a)var l=a.get(null);else{a=new Map,Si.set(e,a);for(var o=e.querySelectorAll("link[data-precedence],style[data-precedence]"),i=0;i<o.length;i++){var c=o[i];(c.nodeName==="LINK"||c.getAttribute("media")!=="not all")&&(a.set(c.dataset.precedence,c),l=c)}l&&a.set(null,l)}o=t.instance,c=o.getAttribute("data-precedence"),i=a.get(c)||l,i===l&&a.set(null,o),a.set(c,o),this.count++,l=_i.bind(this),o.addEventListener("load",l),o.addEventListener("error",l),i?i.parentNode.insertBefore(o,i.nextSibling):(e=e.nodeType===9?e.head:e,e.insertBefore(o,e.firstChild)),t.state.loading|=4}}var ao={$$typeof:H,Provider:null,Consumer:null,_currentValue:Q,_currentValue2:Q,_threadCount:0};function xg(e,t,a,l,o,i,c,d,S){this.tag=1,this.containerInfo=e,this.pingCache=this.current=this.pendingChildren=null,this.timeoutHandle=-1,this.callbackNode=this.next=this.pendingContext=this.context=this.cancelPendingCommit=null,this.callbackPriority=0,this.expirationTimes=Li(-1),this.entangledLanes=this.shellSuspendCounter=this.errorRecoveryDisabledLanes=this.expiredLanes=this.warmLanes=this.pingedLanes=this.suspendedLanes=this.pendingLanes=0,this.entanglements=Li(0),this.hiddenUpdates=Li(null),this.identifierPrefix=l,this.onUncaughtError=o,this.onCaughtError=i,this.onRecoverableError=c,this.pooledCache=null,this.pooledCacheLanes=0,this.formState=S,this.incompleteTransitions=new Map}function rh(e,t,a,l,o,i,c,d,S,z,q,Z){return e=new xg(e,t,a,c,S,z,q,Z,d),t=1,i===!0&&(t|=24),i=zt(3,null,null,t),e.current=i,i.stateNode=e,t=Ts(),t.refCount++,e.pooledCache=t,t.refCount++,i.memoizedState={element:l,isDehydrated:a,cache:t},Cs(i),e}function ch(e){return e?(e=Xa,e):Xa}function uh(e,t,a,l,o,i){o=ch(o),l.context===null?l.context=o:l.pendingContext=o,l=qn(t),l.payload={element:a},i=i===void 0?null:i,i!==null&&(l.callback=i),a=Xn(e,l,t),a!==null&&(Ot(a,e,t),zl(a,e,t))}function dh(e,t){if(e=e.memoizedState,e!==null&&e.dehydrated!==null){var a=e.retryLane;e.retryLane=a!==0&&a<t?a:t}}function Zr(e,t){dh(e,t),(e=e.alternate)&&dh(e,t)}function ph(e){if(e.tag===13||e.tag===31){var t=ma(e,67108864);t!==null&&Ot(t,e,67108864),Zr(e,67108864)}}function hh(e){if(e.tag===13||e.tag===31){var t=Lt();t=Yi(t);var a=ma(e,t);a!==null&&Ot(a,e,t),Zr(e,t)}}var wi=!0;function wg(e,t,a,l){var o=D.T;D.T=null;var i=O.p;try{O.p=2,Qr(e,t,a,l)}finally{O.p=i,D.T=o}}function Tg(e,t,a,l){var o=D.T;D.T=null;var i=O.p;try{O.p=8,Qr(e,t,a,l)}finally{O.p=i,D.T=o}}function Qr(e,t,a,l){if(wi){var o=Vr(l);if(o===null)kr(e,t,l,Ti,a),mh(e,l);else if(Ag(o,e,t,a,l))l.stopPropagation();else if(mh(e,l),t&4&&-1<Eg.indexOf(e)){for(;o!==null;){var i=Oa(o);if(i!==null)switch(i.tag){case 3:if(i=i.stateNode,i.current.memoizedState.isDehydrated){var c=ua(i.pendingLanes);if(c!==0){var d=i;for(d.pendingLanes|=2,d.entangledLanes|=2;c;){var S=1<<31-wt(c);d.entanglements[1]|=S,c&=~S}un(i),(Be&6)===0&&(ii=ve()+500,Wl(0))}}break;case 31:case 13:d=ma(i,2),d!==null&&Ot(d,i,2),ri(),Zr(i,2)}if(i=Vr(l),i===null&&kr(e,t,l,Ti,a),i===o)break;o=i}o!==null&&l.stopPropagation()}else kr(e,t,l,null,a)}}function Vr(e){return e=$i(e),Kr(e)}var Ti=null;function Kr(e){if(Ti=null,e=Da(e),e!==null){var t=h(e);if(t===null)e=null;else{var a=t.tag;if(a===13){if(e=b(t),e!==null)return e;e=null}else if(a===31){if(e=g(t),e!==null)return e;e=null}else if(a===3){if(t.stateNode.current.memoizedState.isDehydrated)return t.tag===3?t.stateNode.containerInfo:null;e=null}else t!==e&&(e=null)}}return Ti=e,null}function fh(e){switch(e){case"beforetoggle":case"cancel":case"click":case"close":case"contextmenu":case"copy":case"cut":case"auxclick":case"dblclick":case"dragend":case"dragstart":case"drop":case"focusin":case"focusout":case"input":case"invalid":case"keydown":case"keypress":case"keyup":case"mousedown":case"mouseup":case"paste":case"pause":case"play":case"pointercancel":case"pointerdown":case"pointerup":case"ratechange":case"reset":case"resize":case"seeked":case"submit":case"toggle":case"touchcancel":case"touchend":case"touchstart":case"volumechange":case"change":case"selectionchange":case"textInput":case"compositionstart":case"compositionend":case"compositionupdate":case"beforeblur":case"afterblur":case"beforeinput":case"blur":case"fullscreenchange":case"focus":case"hashchange":case"popstate":case"select":case"selectstart":return 2;case"drag":case"dragenter":case"dragexit":case"dragleave":case"dragover":case"mousemove":case"mouseout":case"mouseover":case"pointermove":case"pointerout":case"pointerover":case"scroll":case"touchmove":case"wheel":case"mouseenter":case"mouseleave":case"pointerenter":case"pointerleave":return 8;case"message":switch(hn()){case fo:return 2;case mo:return 8;case ra:case Bi:return 32;case go:return 268435456;default:return 32}default:return 32}}var $r=!1,ea=null,ta=null,na=null,lo=new Map,oo=new Map,aa=[],Eg="mousedown mouseup touchcancel touchend touchstart auxclick dblclick pointercancel pointerdown pointerup dragend dragstart drop compositionend compositionstart keydown keypress keyup input textInput copy cut paste click change contextmenu reset".split(" ");function mh(e,t){switch(e){case"focusin":case"focusout":ea=null;break;case"dragenter":case"dragleave":ta=null;break;case"mouseover":case"mouseout":na=null;break;case"pointerover":case"pointerout":lo.delete(t.pointerId);break;case"gotpointercapture":case"lostpointercapture":oo.delete(t.pointerId)}}function io(e,t,a,l,o,i){return e===null||e.nativeEvent!==i?(e={blockedOn:t,domEventName:a,eventSystemFlags:l,nativeEvent:i,targetContainers:[o]},t!==null&&(t=Oa(t),t!==null&&ph(t)),e):(e.eventSystemFlags|=l,t=e.targetContainers,o!==null&&t.indexOf(o)===-1&&t.push(o),e)}function Ag(e,t,a,l,o){switch(t){case"focusin":return ea=io(ea,e,t,a,l,o),!0;case"dragenter":return ta=io(ta,e,t,a,l,o),!0;case"mouseover":return na=io(na,e,t,a,l,o),!0;case"pointerover":var i=o.pointerId;return lo.set(i,io(lo.get(i)||null,e,t,a,l,o)),!0;case"gotpointercapture":return i=o.pointerId,oo.set(i,io(oo.get(i)||null,e,t,a,l,o)),!0}return!1}function gh(e){var t=Da(e.target);if(t!==null){var a=h(t);if(a!==null){if(t=a.tag,t===13){if(t=b(a),t!==null){e.blockedOn=t,Mc(e.priority,function(){hh(a)});return}}else if(t===31){if(t=g(a),t!==null){e.blockedOn=t,Mc(e.priority,function(){hh(a)});return}}else if(t===3&&a.stateNode.current.memoizedState.isDehydrated){e.blockedOn=a.tag===3?a.stateNode.containerInfo:null;return}}}e.blockedOn=null}function Ei(e){if(e.blockedOn!==null)return!1;for(var t=e.targetContainers;0<t.length;){var a=Vr(e.nativeEvent);if(a===null){a=e.nativeEvent;var l=new a.constructor(a.type,a);Ki=l,a.target.dispatchEvent(l),Ki=null}else return t=Oa(a),t!==null&&ph(t),e.blockedOn=a,!1;t.shift()}return!0}function yh(e,t,a){Ei(e)&&a.delete(t)}function Ng(){$r=!1,ea!==null&&Ei(ea)&&(ea=null),ta!==null&&Ei(ta)&&(ta=null),na!==null&&Ei(na)&&(na=null),lo.forEach(yh),oo.forEach(yh)}function Ai(e,t){e.blockedOn===t&&(e.blockedOn=null,$r||($r=!0,r.unstable_scheduleCallback(r.unstable_NormalPriority,Ng)))}var Ni=null;function bh(e){Ni!==e&&(Ni=e,r.unstable_scheduleCallback(r.unstable_NormalPriority,function(){Ni===e&&(Ni=null);for(var t=0;t<e.length;t+=3){var a=e[t],l=e[t+1],o=e[t+2];if(typeof l!="function"){if(Kr(l||a)===null)continue;break}var i=Oa(a);i!==null&&(e.splice(t,3),t-=3,Vs(i,{pending:!0,data:o,method:a.method,action:l},l,o))}}))}function hl(e){function t(S){return Ai(S,e)}ea!==null&&Ai(ea,e),ta!==null&&Ai(ta,e),na!==null&&Ai(na,e),lo.forEach(t),oo.forEach(t);for(var a=0;a<aa.length;a++){var l=aa[a];l.blockedOn===e&&(l.blockedOn=null)}for(;0<aa.length&&(a=aa[0],a.blockedOn===null);)gh(a),a.blockedOn===null&&aa.shift();if(a=(e.ownerDocument||e).$$reactFormReplay,a!=null)for(l=0;l<a.length;l+=3){var o=a[l],i=a[l+1],c=o[At]||null;if(typeof i=="function")c||bh(a);else if(c){var d=null;if(i&&i.hasAttribute("formAction")){if(o=i,c=i[At]||null)d=c.formAction;else if(Kr(o)!==null)continue}else d=c.action;typeof d=="function"?a[l+1]=d:(a.splice(l,3),l-=3),bh(a)}}}function vh(){function e(i){i.canIntercept&&i.info==="react-transition"&&i.intercept({handler:function(){return new Promise(function(c){return o=c})},focusReset:"manual",scroll:"manual"})}function t(){o!==null&&(o(),o=null),l||setTimeout(a,20)}function a(){if(!l&&!navigation.transition){var i=navigation.currentEntry;i&&i.url!=null&&navigation.navigate(i.url,{state:i.getState(),info:"react-transition",history:"replace"})}}if(typeof navigation=="object"){var l=!1,o=null;return navigation.addEventListener("navigate",e),navigation.addEventListener("navigatesuccess",t),navigation.addEventListener("navigateerror",t),setTimeout(a,100),function(){l=!0,navigation.removeEventListener("navigate",e),navigation.removeEventListener("navigatesuccess",t),navigation.removeEventListener("navigateerror",t),o!==null&&(o(),o=null)}}}function Wr(e){this._internalRoot=e}Ci.prototype.render=Wr.prototype.render=function(e){var t=this._internalRoot;if(t===null)throw Error(u(409));var a=t.current,l=Lt();uh(a,l,e,t,null,null)},Ci.prototype.unmount=Wr.prototype.unmount=function(){var e=this._internalRoot;if(e!==null){this._internalRoot=null;var t=e.containerInfo;uh(e.current,2,null,e,null,null),ri(),t[Ma]=null}};function Ci(e){this._internalRoot=e}Ci.prototype.unstable_scheduleHydration=function(e){if(e){var t=jc();e={blockedOn:null,target:e,priority:t};for(var a=0;a<aa.length&&t!==0&&t<aa[a].priority;a++);aa.splice(a,0,e),a===0&&gh(e)}};var _h=n.version;if(_h!=="19.2.4")throw Error(u(527,_h,"19.2.4"));O.findDOMNode=function(e){var t=e._reactInternals;if(t===void 0)throw typeof e.render=="function"?Error(u(188)):(e=Object.keys(e).join(","),Error(u(268,e)));return e=y(t),e=e!==null?w(e):null,e=e===null?null:e.stateNode,e};var Cg={bundleType:0,version:"19.2.4",rendererPackageName:"react-dom",currentDispatcherRef:D,reconcilerVersion:"19.2.4"};if(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__<"u"){var ji=__REACT_DEVTOOLS_GLOBAL_HOOK__;if(!ji.isDisabled&&ji.supportsFiber)try{ca=ji.inject(Cg),xt=ji}catch{}}return so.createRoot=function(e,t){if(!p(e))throw Error(u(299));var a=!1,l="",o=Nd,i=Cd,c=jd;return t!=null&&(t.unstable_strictMode===!0&&(a=!0),t.identifierPrefix!==void 0&&(l=t.identifierPrefix),t.onUncaughtError!==void 0&&(o=t.onUncaughtError),t.onCaughtError!==void 0&&(i=t.onCaughtError),t.onRecoverableError!==void 0&&(c=t.onRecoverableError)),t=rh(e,1,!1,null,null,a,l,null,o,i,c,vh),e[Ma]=t.current,Or(e),new Wr(t)},so.hydrateRoot=function(e,t,a){if(!p(e))throw Error(u(299));var l=!1,o="",i=Nd,c=Cd,d=jd,S=null;return a!=null&&(a.unstable_strictMode===!0&&(l=!0),a.identifierPrefix!==void 0&&(o=a.identifierPrefix),a.onUncaughtError!==void 0&&(i=a.onUncaughtError),a.onCaughtError!==void 0&&(c=a.onCaughtError),a.onRecoverableError!==void 0&&(d=a.onRecoverableError),a.formState!==void 0&&(S=a.formState)),t=rh(e,1,!0,t,a??null,l,o,S,i,c,d,vh),t.context=ch(null),a=t.current,l=Lt(),l=Yi(l),o=qn(l),o.callback=null,Xn(a,o,l),a=l,t.current.lanes=a,yl(t,a),un(t),e[Ma]=t.current,Or(e),new Ci(t)},so.version="19.2.4",so}var Ah;function Fg(){if(Ah)return Pr.exports;Ah=1;function r(){if(!(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__>"u"||typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE!="function"))try{__REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE(r)}catch(n){console.error(n)}}return r(),Pr.exports=Pg(),Pr.exports}var ey=Fg(),tc={exports:{}},nc={};/**
 * @license React
 * react-compiler-runtime.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Nh;function ty(){if(Nh)return nc;Nh=1;var r=Ph().__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE;return nc.c=function(n){return r.H.useMemoCache(n)},nc}var Ch;function ny(){return Ch||(Ch=1,tc.exports=ty()),tc.exports}var xe=ny();const ay="_wrapper_bt1w8_2",ly="_header_bt1w8_10",oy="_headerActions_bt1w8_21",iy="_title_bt1w8_27",sy="_panelGroup_bt1w8_36",ry="_clipboardToggle_bt1w8_43",cy="_helpToggle_bt1w8_66",uy="_helpButtonWrapper_bt1w8_93",dy="_helpTogglePulsing_bt1w8_97",py="_helpHint_bt1w8_112",hy="_helpHintFading_bt1w8_139",fy="_helpHintKbd_bt1w8_144",my="_resizeHandle_bt1w8_153",kt={wrapper:ay,header:ly,headerActions:oy,title:iy,panelGroup:sy,clipboardToggle:ry,helpToggle:cy,helpButtonWrapper:uy,helpTogglePulsing:dy,helpHint:py,helpHintFading:hy,helpHintKbd:fy,resizeHandle:my},gy=r=>{try{return!new DOMParser().parseFromString(r.trim(),"text/xml").querySelector("parsererror")}catch{return!1}},yy=r=>{try{return JSON.parse(r),!0}catch{return!1}},by=r=>r.trim()?yy(r)?{valid:!0,error:null,type:"json"}:gy(r)?{valid:!0,error:null,type:"xml"}:{valid:!1,error:"Invalid JSON/XML format",type:null}:{valid:!0,error:null,type:null},uc=r=>{try{const n=JSON.parse(r);return JSON.stringify(n,null,2)}catch{return r}},vy=()=>{const r=xe.c(8);let n;r[0]===Symbol.for("react.memo_cache_sentinel")?(n=[],r[0]=n):n=r[0];const[s,u]=T.useState(n),p=T.useRef(0);let h;r[1]===Symbol.for("react.memo_cache_sentinel")?(h=new Set,r[1]=h):h=r[1];const b=T.useRef(h);let g,m;r[2]===Symbol.for("react.memo_cache_sentinel")?(g=()=>()=>{b.current.forEach(clearTimeout)},m=[],r[2]=g,r[3]=m):(g=r[2],m=r[3]),T.useEffect(g,m);let y;r[4]===Symbol.for("react.memo_cache_sentinel")?(y=(E,C)=>{const _=C===void 0?"info":C,j=p.current=p.current+1;u(H=>[...H,{id:j,message:E,type:_}]);const k=setTimeout(()=>{b.current.delete(k),u(H=>H.filter(L=>L.id!==j))},3e3);b.current.add(k)},r[4]=y):y=r[4];const w=y;let v;r[5]===Symbol.for("react.memo_cache_sentinel")?(v=E=>{u(C=>C.filter(_=>_.id!==E))},r[5]=v):v=r[5];const x=v;let N;return r[6]!==s?(N={toasts:s,addToast:w,removeToast:x},r[6]=s,r[7]=N):N=r[7],N},oa=(r,n)=>{const s=T.useCallback(()=>{try{const h=window.localStorage.getItem(r);return h?JSON.parse(h):n}catch{return n}},[r]),[u,p]=T.useState(s);return T.useEffect(()=>{p(s())},[r]),T.useEffect(()=>{try{window.localStorage.setItem(r,JSON.stringify(u))}catch(h){console.error(`Error setting localStorage key "${r}":`,h)}},[r,u]),T.useEffect(()=>{const h=b=>{(b.key===r||b.key===null)&&p(s())};return window.addEventListener("storage",h),()=>window.removeEventListener("storage",h)},[r,s]),T.useEffect(()=>{const h=()=>p(s());return window.addEventListener("focus",h),document.addEventListener("visibilitychange",h),()=>{window.removeEventListener("focus",h),document.removeEventListener("visibilitychange",h)}},[s]),[u,p]},_y=200,jh=50,Sy=8,xy=2e4,On=[{path:"/json-path",label:"JSON-Path",title:"JSON-Path Playground",wsPath:"/ws/json/path",storageKeyPayload:"jsonpath-last-payload",storageKeyHistory:"jsonpath-command-history",storageKeyTab:"jsonpath-right-tab",supportsUpload:!0,tabs:["payload","graph","graph-data"]},{path:"/",label:"Minigraph",title:"Minigraph Playground",wsPath:"/ws/graph/playground",storageKeyPayload:"minigraph-last-payload",storageKeyHistory:"minigraph-command-history",storageKeyTab:"minigraph-right-tab",storageKeySavedGraphs:"minigraph-saved-graphs",storageKeyHelpTopic:"minigraph-help-topic",supportsClipboard:!0,supportsHelp:!0,tabs:["graph","graph-data"]}],Mi={json_simple:JSON.stringify({name:"John Doe",age:30,city:"New York"},null,2),json_nested:JSON.stringify({user:{name:"Jane Smith",profile:{email:"jane@example.com",address:{city:"San Francisco",country:"USA"}}}},null,2),json_array:JSON.stringify([{id:1,name:"Item 1",status:"active"},{id:2,name:"Item 2",status:"pending"},{id:3,name:"Item 3",status:"inactive"}],null,2),xml_simple:`<?xml version="1.0" encoding="UTF-8"?>
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
</items>`};function tf(r){return`ws://${window.location.host}${r}`}function ac(r,n,s,u){const p=r[n]??{phase:"idle",messages:[]},h=[...p.messages,{id:s,raw:u}];return h.length>_y&&h.shift(),{...r,[n]:{...p,messages:h}}}function wy(r,n){const s=r[n.path]??{phase:"idle",messages:[]};switch(n.type){case"CONNECTING":return{...r,[n.path]:{...s,phase:"connecting"}};case"CONNECTED":return ac({...r,[n.path]:{...s,phase:"connected"}},n.path,n.id,n.msg);case"MESSAGE_RECEIVED":return ac(r,n.path,n.id,n.msg);case"DISCONNECTED":return ac({...r,[n.path]:{...s,phase:"idle"}},n.path,n.id,n.msg);case"CONNECT_ERROR":return{...r,[n.path]:{...s,phase:"idle"}};case"CLEAR_MESSAGES":return{...r,[n.path]:{...s,messages:[]}};default:return r}}const nf=T.createContext(null);function Ty({children:r}){const[n,s]=T.useReducer(wy,{}),u=T.useRef({}),p=T.useRef({}),h=T.useRef({});T.useEffect(()=>()=>{Object.entries(u.current).forEach(([B,I])=>{I==null||I.close();const V=p.current[B];V&&clearInterval(V)})},[]);const b=B=>tf(B),g=B=>(h.current[B]=(h.current[B]??0)+1,h.current[B]),m=()=>{const B=new Date().toString(),I=B.indexOf("GMT");return I>0?B.substring(0,I).trim():B},y=(B,I)=>JSON.stringify({type:B,message:I,time:m()}),w=B=>{try{const I=JSON.parse(B);if(I!==null&&typeof I=="object"){const V=I.type;return V==="ping"||V==="pong"}}catch{}return!1},v=T.useCallback((B,I)=>{if(!window.WebSocket){I==null||I("WebSocket not supported by your browser","error");return}const V=u.current[B];if(V&&(V.readyState===WebSocket.OPEN||V.readyState===WebSocket.CONNECTING)){I==null||I("Already connected","error");return}s({type:"CONNECTING",path:B});const K=new WebSocket(b(B));u.current[B]=K,K.onopen=()=>{s({type:"CONNECTED",path:B,id:g(B),msg:y("info","connected")}),I==null||I("Connected to WebSocket","success"),K.send(JSON.stringify({type:"welcome"})),p.current[B]=setInterval(()=>{K.readyState===WebSocket.OPEN&&K.send(y("ping","keep alive"))},xy)},K.onmessage=$=>{w($.data)||s({type:"MESSAGE_RECEIVED",path:B,id:g(B),msg:$.data})},K.onerror=()=>{s({type:"CONNECT_ERROR",path:B})},K.onclose=$=>{const te=p.current[B];te&&(clearInterval(te),p.current[B]=null),s({type:"DISCONNECTED",path:B,id:g(B),msg:y("info",`disconnected - (${$.code}) ${$.reason}`)}),I==null||I("Disconnected from WebSocket","info"),u.current[B]===K&&(u.current[B]=null)}},[]),x=T.useCallback(B=>{const I=u.current[B];I?I.close():s({type:"MESSAGE_RECEIVED",path:B,id:g(B),msg:y("error","already disconnected")})},[]);T.useEffect(()=>(On.forEach(B=>{v(B.wsPath)}),()=>{On.forEach(B=>{const I=u.current[B.wsPath];I&&I.close()})}),[]);const N=T.useCallback((B,I)=>{const V=u.current[B];return V&&V.readyState===WebSocket.OPEN?(V.send(I),!0):!1},[]),E=T.useCallback((B,I)=>{s({type:"MESSAGE_RECEIVED",path:B,id:g(B),msg:I})},[]),C=T.useCallback(B=>{s({type:"CLEAR_MESSAGES",path:B})},[]),[_,j]=T.useState({}),k=T.useCallback((B,I)=>{j(V=>{if(I===null){const K={...V};return delete K[B],K}return{...V,[B]:I}})},[]),H=T.useCallback(B=>_[B]??null,[_]),L=T.useCallback(B=>{const I=_[B]??null;return I!==null&&j(V=>{const K={...V};return delete K[B],K}),I},[_]),X=T.useCallback(B=>n[B]??{phase:"idle",messages:[]},[n]),Y=T.useMemo(()=>({getSlot:X,connect:v,disconnect:x,send:N,appendMessage:E,clearMessages:C,setPendingPayload:k,peekPendingPayload:H,takePendingPayload:L}),[X,v,x,N,E,C,k,H,L]);return f.jsx(nf.Provider,{value:Y,children:r})}function xc(){const r=T.useContext(nf);if(!r)throw new Error("useWebSocketContext must be used inside <WebSocketProvider>");return r}const Ey=r=>{try{const n=JSON.parse(r);return{type:n.type||"info",message:n.message||r,time:n.time,raw:r}}catch{return{type:"raw",message:r,time:null,raw:r}}},Ay=r=>({info:"ℹ️",error:"❌",ping:"🔄",welcome:"👋",raw:""})[r]??"•",po=r=>{try{const n=JSON.parse(r);if(typeof n=="object"&&n!==null)return{isJSON:!0,data:n}}catch{}return{isJSON:!1,data:null}};function Ny(r){if(!r.includes("Graph exported to "))return null;const n=Tc(r);if(!n)return null;const s=n.split("/")[4];return s?{graphName:s,apiPath:n}:null}function Cy(r){return r.includes("Invalid filename")?{reason:"invalid-name"}:r.includes("Expect root node name")?{reason:"root-name-conflict"}:null}function wc(r){const n=po(r);return n.isJSON?(typeof n.data.type=="string",!1):!0}function Tc(r){const n=r.match(/\/api\/graph\/model\/([^\s'"]+)/);return n?n[0]:null}function af(r){return wc(r)?Tc(r)!==null:!1}function lf(r){const n=r.match(/\/api\/json\/content\/([\w-]+)/);return n?n[0]:null}function jy(r){const n=r.match(/Large payload \((\d+)\)\s*->\s*GET\s+(\/api\/inspect\/[^\s]+)/i);if(!n)return null;const s=parseInt(n[1],10),u=n[2],h=`${u.split("/").filter(Boolean).pop()??"payload"}.json`;return{apiPath:u,byteSize:s,filename:h}}function My(r){const n=r.match(/You may upload .*?->\s*POST\s+(\/api\/mock\/[\w-]+)/i);return n?n[1]:null}function Dy(r){if(!r.startsWith("> "))return!1;const n=r.slice(2).trim().toLowerCase();return n==="help"||n.startsWith("help ")?!0:n.startsWith("describe ")?!n.slice(9).trim().startsWith("graph"):!1}function Oy(r){if(!r.startsWith("> ")||!r.slice(2).trimStart().toLowerCase().startsWith("import graph from "))return null;const s=r.slice(2).trimStart().slice(18).trim();return s.length>0?s:null}function ky(r){if(!wc(r)||r.startsWith("> ")||af(r))return null;const n=r.toLowerCase();return n.includes("graph model imported as draft")?"import-graph":n.includes(" -> ")&&n.includes("removed")||n.startsWith("node ")&&(n.includes(" created")||n.includes(" updated")||n.includes(" deleted")||n.includes(" connected to ")||n.includes(" imported from ")||n.includes(" overwritten by node from "))?"node-mutation":null}const Ry={command:"",historyIndex:-1,draftCommand:""};function zy(r,n){switch(n.type){case"SET_COMMAND":return{...r,command:n.value,historyIndex:-1,draftCommand:""};case"CLEAR_COMMAND":return{...r,command:"",historyIndex:-1,draftCommand:""};case"SET_HISTORY_INDEX":return{...r,historyIndex:n.index,command:n.command};case"ENTER_HISTORY":return{...r,historyIndex:0,command:n.command,draftCommand:r.command};case"EXIT_HISTORY":return{...r,historyIndex:-1,command:r.draftCommand,draftCommand:""};default:return r}}function By(r){const n=xe.c(77),{wsPath:s,storageKeyHistory:u,payload:p,addToast:h,bus:b,handleLocalCommand:g}=r,m=xc();let y;n[0]!==m||n[1]!==s?(y=m.getSlot(s),n[0]=m,n[1]=s,n[2]=y):y=n[2];const{phase:w,messages:v}=y,x=w==="connected",N=w==="connecting",[E,C]=T.useReducer(zy,Ry),{command:_,historyIndex:j}=E;let k;n[3]===Symbol.for("react.memo_cache_sentinel")?(k=[],n[3]=k):k=n[3];const[H,L]=oa(u,k),X=T.useRef(null),Y=T.useRef(!1);let B;n[4]===Symbol.for("react.memo_cache_sentinel")?(B=()=>{X.current&&(X.current.scrollTop=X.current.scrollHeight)},n[4]=B):B=n[4];let I;n[5]!==v?(I=[v],n[5]=v,n[6]=I):I=n[6],T.useEffect(B,I);let V;n[7]!==h||n[8]!==m||n[9]!==s?(V=()=>{m.connect(s,h)},n[7]=h,n[8]=m,n[9]=s,n[10]=V):V=n[10];const K=V;let $;n[11]!==m||n[12]!==s?($=()=>{m.disconnect(s)},n[11]=m,n[12]=s,n[13]=$):$=n[13];const te=$;let ue;n[14]!==_||n[15]!==m||n[16]!==g||n[17]!==H||n[18]!==p||n[19]!==w||n[20]!==L||n[21]!==s?(ue=()=>{if(w!=="connected")return;const W=_.trim();if(W.length!==0){if((g==null?void 0:g(W))===!0){H[0]!==W&&L(be=>[W,...be].slice(0,jh)),m.appendMessage(s,"> "+W),C({type:"CLEAR_COMMAND"});return}m.send(s,W),H[0]!==W&&L(be=>[W,...be].slice(0,jh)),W==="load"&&(p.length===0?m.appendMessage(s,"ERROR: please paste JSON/XML payload in input text area"):m.send(s,p)),C({type:"CLEAR_COMMAND"})}},n[14]=_,n[15]=m,n[16]=g,n[17]=H,n[18]=p,n[19]=w,n[20]=L,n[21]=s,n[22]=ue):ue=n[22];const he=ue;let re;n[23]!==H||n[24]!==j?(re=W=>{if(W.key==="ArrowUp"){if(W.preventDefault(),H.length===0)return;if(j===-1)C({type:"ENTER_HISTORY",command:H[0]});else if(j<H.length-1){const be=j+1;C({type:"SET_HISTORY_INDEX",index:be,command:H[be]})}}else if(W.key==="ArrowDown")if(W.preventDefault(),j<=0)j===0&&C({type:"EXIT_HISTORY"});else{const be=j-1;C({type:"SET_HISTORY_INDEX",index:be,command:H[be]})}},n[23]=H,n[24]=j,n[25]=re):re=n[25];const D=re;let O,Q;n[26]!==h||n[27]!==b||n[28]!==m||n[29]!==p||n[30]!==s?(Q=()=>{if(b)return b.on("upload.contentPath",W=>{if(!Y.current)return;if(Y.current=!1,p.length===0){m.appendMessage(s,"ERROR: please paste JSON/XML payload in the input text area");return}let be;try{be=JSON.stringify(JSON.parse(p))}catch{m.appendMessage(s,"ERROR: payload is not valid JSON — cannot upload");return}fetch(W.uploadPath,{method:"POST",headers:{"Content-Type":"application/json"},body:be}).then(Te=>{if(!Te.ok)throw new Error(`HTTP ${Te.status}`);h("Payload uploaded successfully","success")}).catch(Te=>{m.appendMessage(s,`ERROR: upload failed — ${Te.message}`),h(`Upload failed: ${Te.message}`,"error")})})},O=[b,p,s,m,h],n[26]=h,n[27]=b,n[28]=m,n[29]=p,n[30]=s,n[31]=O,n[32]=Q):(O=n[31],Q=n[32]),T.useEffect(Q,O);let P,F;n[33]!==h||n[34]!==b||n[35]!==m||n[36]!==v||n[37]!==p||n[38]!==s?(P=()=>{if(b||!Y.current||v.length===0)return;const W=v[v.length-1].raw,be=lf(W);if(!be)return;if(Y.current=!1,p.length===0){m.appendMessage(s,"ERROR: please paste JSON/XML payload in the input text area");return}let Te;try{Te=JSON.stringify(JSON.parse(p))}catch{m.appendMessage(s,"ERROR: payload is not valid JSON — cannot upload");return}fetch(be,{method:"POST",headers:{"Content-Type":"application/json"},body:Te}).then(me=>{if(!me.ok)throw new Error(`HTTP ${me.status}`);h("Payload uploaded successfully","success")}).catch(me=>{m.appendMessage(s,`ERROR: upload failed — ${me.message}`),h(`Upload failed: ${me.message}`,"error")})},F=[b,v,p,s,m,h],n[33]=h,n[34]=b,n[35]=m,n[36]=v,n[37]=p,n[38]=s,n[39]=P,n[40]=F):(P=n[39],F=n[40]),T.useEffect(P,F);let ie;n[41]!==h||n[42]!==m||n[43]!==p||n[44]!==w||n[45]!==s?(ie=()=>{if(w==="connected"){if(p.length===0){h("Nothing to upload — paste a JSON payload first","error");return}Y.current=!0,m.send(s,"upload")}},n[41]=h,n[42]=m,n[43]=p,n[44]=w,n[45]=s,n[46]=ie):ie=n[46];const oe=ie;let ee;n[47]!==m||n[48]!==w||n[49]!==s?(ee=W=>{w==="connected"&&m.send(s,W)},n[47]=m,n[48]=w,n[49]=s,n[50]=ee):ee=n[50];const ye=ee;let se;n[51]!==h||n[52]!==v?(se=()=>{navigator.clipboard.writeText(v.map(Hy).join(`
`)),h("Console copied to clipboard!","success")},n[51]=h,n[52]=v,n[53]=se):se=n[53];const ge=se;let je;n[54]!==h||n[55]!==m||n[56]!==s?(je=()=>{m.clearMessages(s),h("Console cleared","info")},n[54]=h,n[55]=m,n[56]=s,n[57]=je):je=n[57];const He=je;let we;n[58]!==m||n[59]!==s?(we=W=>{m.appendMessage(s,W)},n[58]=m,n[59]=s,n[60]=we):we=n[60];const ae=we;let de;n[61]===Symbol.for("react.memo_cache_sentinel")?(de=W=>C({type:"SET_COMMAND",value:W}),n[61]=de):de=n[61];const fe=de;let ze;return n[62]!==ae||n[63]!==He||n[64]!==_||n[65]!==K||n[66]!==x||n[67]!==N||n[68]!==ge||n[69]!==te||n[70]!==D||n[71]!==H||n[72]!==v||n[73]!==he||n[74]!==ye||n[75]!==oe?(ze={connected:x,connecting:N,messages:v,command:_,setCommand:fe,connect:K,disconnect:te,sendCommand:he,handleKeyDown:D,consoleRef:X,copyMessages:ge,clearMessages:He,uploadPayload:oe,sendRawText:ye,appendMessage:ae,history:H},n[62]=ae,n[63]=He,n[64]=_,n[65]=K,n[66]=x,n[67]=N,n[68]=ge,n[69]=te,n[70]=D,n[71]=H,n[72]=v,n[73]=he,n[74]=ye,n[75]=oe,n[76]=ze):ze=n[76],ze}function Hy(r){return r.raw}function Gy(r){const n=xe.c(5);let s;n[0]!==r?(s=()=>window.matchMedia(r).matches,n[0]=r,n[1]=s):s=n[1];const[u,p]=T.useState(s);let h,b;return n[2]!==r?(h=()=>{const g=window.matchMedia(r),m=y=>p(y.matches);return g.addEventListener("change",m),()=>g.removeEventListener("change",m)},b=[r],n[2]=r,n[3]=h,n[4]=b):(h=n[3],b=n[4]),T.useEffect(h,b),u}function Mh(r){return typeof r!="object"||r===null?!1:Array.isArray(r.nodes)}function lc(r,n,s){const u=n.includes(s)?s:n[0]??"graph";return typeof r=="string"&&n.includes(r)?r:u}function Uy(r,n,s,u,p){const[h,b]=T.useState(null),[g,m]=oa(p,s),y=lc(g,u,s),[w,v]=T.useState(!1),x=T.useCallback(_=>{m(j=>{const k=lc(j,u,s),H=typeof _=="function"?_(k):_;return lc(H,u,s)})},[m,u,s]);T.useEffect(()=>{g!==y&&m(y)},[g,y,m]);const N=T.useRef(r);T.useEffect(()=>{N.current=r},[r]);const E=T.useRef(null);T.useEffect(()=>{if(!r)return;const _=new AbortController;return b(null),fetch(r,{signal:_.signal}).then(j=>{if(!j.ok)throw new Error(`HTTP ${j.status}`);return j.json()}).then(j=>{Mh(j)&&(b(j),x("graph"))}).catch(j=>{j.name!=="AbortError"&&n(`Graph fetch failed: ${j.message}`,"error")}),()=>{_.abort()}},[r,n]);const C=T.useCallback(()=>{var k;const _=N.current;if(!_)return;(k=E.current)==null||k.abort();const j=new AbortController;E.current=j,v(!0),fetch(_,{signal:j.signal}).then(H=>{if(!H.ok)throw new Error(`HTTP ${H.status}`);return H.json()}).then(H=>{Mh(H)&&b(H),v(!1)}).catch(H=>{H.name!=="AbortError"&&(n(`Graph refresh failed: ${H.message}`,"error"),v(!1))})},[]);return T.useEffect(()=>()=>{var _;(_=E.current)==null||_.abort()},[]),{graphData:h,setGraphData:b,rightTab:y,setRightTab:x,isRefreshing:w,refetchGraph:C}}function Ly(r){const n=xe.c(22),{bus:s,pinnedGraphPath:u,setPinnedGraphPath:p,connected:h,sendRawText:b,addToast:g}=r,m=T.useRef(null),y=T.useRef(!1),w=T.useRef(u),v=T.useRef(h),x=T.useRef(b);let N,E;n[0]!==u?(N=()=>{w.current=u},E=[u],n[0]=u,n[1]=N,n[2]=E):(N=n[1],E=n[2]),T.useEffect(N,E);let C,_;n[3]!==h?(C=()=>{v.current=h},_=[h],n[3]=h,n[4]=C,n[5]=_):(C=n[4],_=n[5]),T.useEffect(C,_);let j,k;n[6]!==b?(j=()=>{x.current=b},k=[b],n[6]=b,n[7]=j,n[8]=k):(j=n[7],k=n[8]),T.useEffect(j,k);let H,L;n[9]!==h?(H=()=>{h||(y.current=!1,m.current!==null&&(clearTimeout(m.current),m.current=null))},L=[h],n[9]=h,n[10]=H,n[11]=L):(H=n[10],L=n[11]),T.useEffect(H,L);let X,Y;n[12]!==s||n[13]!==p?(Y=()=>s.on("graph.link",$=>{y.current&&(y.current=!1,p($.apiPath))}),X=[s,p],n[12]=s,n[13]=p,n[14]=X,n[15]=Y):(X=n[14],Y=n[15]),T.useEffect(Y,X);let B,I;n[16]!==g||n[17]!==s?(B=()=>s.on("graph.mutation",$=>{if(v.current){if($.mutationType==="import-graph"){m.current!==null&&(clearTimeout(m.current),m.current=null),y.current=!0,x.current("describe graph"),g("Graph imported — refreshing view…","info");return}m.current!==null&&clearTimeout(m.current),m.current=setTimeout(()=>{m.current=null,v.current&&(y.current=!0,x.current("describe graph"),g(w.current!==null?"Graph updated — refreshing…":"Graph updated — opening Graph tab…","info"))},300)}}),I=[s,g],n[16]=g,n[17]=s,n[18]=B,n[19]=I):(B=n[18],I=n[19]),T.useEffect(B,I);let V,K;n[20]===Symbol.for("react.memo_cache_sentinel")?(V=()=>()=>{m.current!==null&&clearTimeout(m.current)},K=[],n[20]=V,n[21]=K):(V=n[20],K=n[21]),T.useEffect(V,K)}const Yy=`Connect two nodes together
--------------------------
1. Each connection is directional. Connect A to B is different from B to A.
2. A node must connect to one or more nodes. When a graph has orphan nodes, you cannot export the graph for deployment.

Syntax
------
\`\`\`
connect {node-A} to {node-B} with {relation}
\`\`\`
`,qy=`Create a new node
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
`,Xy=`Data Dictionary
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
`,Jy=`Delete a node, a connection or clear cache
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
`,Iy="Describe graph, node, connection or skill\n-----------------------------------------\n\nSyntax\n------\nShow the structure of the current graph model\n```\ndescribe graph\n```\n\nPrint the structure of a node\n```\ndescribe node {name}\n```\n\nConfirm if there is a connection between node-A and node-B\n```\ndescribe connection {node-A} and {node-B}\n```\n\nSkill description of a specific composable function serving the skill\n```\ndescribe skill {skill.route.name}\n```\n",Zy=`Edit a node
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
`,Qy=`Execute a node with a skill
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
`,Vy=`Export a graph model
--------------------
1. This command exports a graph as a model in JSON format for deployment
2. The name does not require the ".json" extension

Syntax
------
\`\`\`
export graph as {name}
\`\`\`
`,Ky=`Skill: Graph API Fetcher
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

Caution
-------
API fetchers can be chained together to make multiple API calls. 
However, you should design the API chain to be minimalist.

An overly complex chain of API requests would mean slow performance. Just take the minimal set of data that are
required by your application. Don't abuse the flexibility of the API fetcher.
`,$y=`Skill: Graph Data Mapper
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
`,Wy=`Skill: Graph Extension
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
`,Py=`Skill: Graph Island
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
`,Fy=`Skill: Graph Join
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
`,e1=`Skill: Graph JS
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

A node cannot be executed twice
-------------------------------
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
\`\`\`

Execution
---------
Upon successful execution of a "COMPUTE" statement, the result set will be stored in the "result" namespace
of the node. A subsequent "MAPPING" statement can map the key-values in the result set to one or more nodes.

For an "IF" statement, the system will execute a boolean operation.
This process will override the natural graph traversal order and jump to a specific node.
If the function returns "next" after evaluation of all statements, the natural graph traversal order
will be preserved.

Iterative Execution
-------------------
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
`,t1=`Skill: Graph Math
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
\`\`\`

Execution
---------
Upon successful execution of a "COMPUTE" statement, the result set will be stored in the "result" namespace
of the node. A subsequent "MAPPING" statement can map the key-values in the result set to one or more nodes.

For an "IF" statement, the system will execute a boolean operation.
This process will override the natural graph traversal order and jump to a specific node.
If the function returns "next" after evaluation of all statements, the natural graph traversal order
will be preserved.

Iterative Execution
-------------------
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
`,n1=`Import a graph model
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
`,a1=`Inspect state machine
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
`,l1=`Instantiate from a Graph Model
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
`,o1=`List nodes or connections
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
`,i1=`Run a graph instance
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
`,s1=`Display nodes that have been 'seen'
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
`,r1=`Tutorial 1
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
`,c1=`Tutorial 10
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
`,u1=`Tutorial 11
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
`,d1=`Tutorial 2
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
`,p1=`Tutorial 3
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
`,h1=`Tutorial 4
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
`,f1=`Tutorial 5
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
    "person_id": 100
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
`,m1=`Tutorial 6
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
`,g1=`Tutorial 7
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
`,y1=`Tutorial 8
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
`,b1=`Tutorial 9
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
`,v1=`Update a node
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
`,_1=`Upload mock data to current graph instance
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
`,S1=`MiniGraph
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
`,x1=Object.assign({"../../../src/main/resources/help/help connect.md":Yy,"../../../src/main/resources/help/help create.md":qy,"../../../src/main/resources/help/help data-dictionary.md":Xy,"../../../src/main/resources/help/help delete.md":Jy,"../../../src/main/resources/help/help describe.md":Iy,"../../../src/main/resources/help/help edit.md":Zy,"../../../src/main/resources/help/help execute.md":Qy,"../../../src/main/resources/help/help export.md":Vy,"../../../src/main/resources/help/help graph-api-fetcher.md":Ky,"../../../src/main/resources/help/help graph-data-mapper.md":$y,"../../../src/main/resources/help/help graph-extension.md":Wy,"../../../src/main/resources/help/help graph-island.md":Py,"../../../src/main/resources/help/help graph-join.md":Fy,"../../../src/main/resources/help/help graph-js.md":e1,"../../../src/main/resources/help/help graph-math.md":t1,"../../../src/main/resources/help/help import.md":n1,"../../../src/main/resources/help/help inspect.md":a1,"../../../src/main/resources/help/help instantiate.md":l1,"../../../src/main/resources/help/help list.md":o1,"../../../src/main/resources/help/help run.md":i1,"../../../src/main/resources/help/help seen.md":s1,"../../../src/main/resources/help/help tutorial 1.md":r1,"../../../src/main/resources/help/help tutorial 10.md":c1,"../../../src/main/resources/help/help tutorial 11.md":u1,"../../../src/main/resources/help/help tutorial 2.md":d1,"../../../src/main/resources/help/help tutorial 3.md":p1,"../../../src/main/resources/help/help tutorial 4.md":h1,"../../../src/main/resources/help/help tutorial 5.md":f1,"../../../src/main/resources/help/help tutorial 6.md":m1,"../../../src/main/resources/help/help tutorial 7.md":g1,"../../../src/main/resources/help/help tutorial 8.md":y1,"../../../src/main/resources/help/help tutorial 9.md":b1,"../../../src/main/resources/help/help update.md":v1,"../../../src/main/resources/help/help upload.md":_1,"../../../src/main/resources/help/help.md":S1});function w1(r){const n=r.split("/");return(n[n.length-1]??r).replace(/\.md$/,"")}const of=Object.fromEntries(Object.entries(x1).map(([r,n])=>[w1(r),n]));function Ri(r){const n=r===""?"help":`help ${r}`;return of[n]??null}const T1=Object.keys(of).filter(r=>r!=="help").map(r=>r.replace(/^help\s+/,"")).sort(),dc=[{id:"overview",label:"Overview"},{id:"graph-model",label:"Graph Model"},{id:"graph-skills",label:"Graph Skills"},{id:"instance-model",label:"Instance Model"},{id:"tutorials",label:"Tutorials",chipStripLabel:"Chapters"}],E1=new Set(["execute","inspect","instantiate","run","seen","upload"]);function sf(r){return r===""?"overview":r.startsWith("tutorial ")?"tutorials":r.startsWith("graph-")?"graph-skills":E1.has(r)?"instance-model":"graph-model"}function pc(r){if(r==="overview")return[""];const n=T1.filter(s=>sf(s)===r);return r==="tutorials"?[...n].sort((s,u)=>{const p=parseInt(s.replace(/^tutorial\s+/,""),10),h=parseInt(u.replace(/^tutorial\s+/,""),10);return p-h}):n}function A1(r,n){return r===""?"Overview":n==="tutorials"?r.replace(/^tutorial\s+/,""):r}const fl=dc.flatMap(r=>pc(r.id));function rf(r){return r.replace(/^help\s*/i,"").trim().toLowerCase()}function N1(r){const n=xe.c(6),{bus:s,setHelpTopic:u,onTabSwitch:p}=r,h=T.useRef(p);let b;n[0]!==p?(b=()=>{h.current=p},n[0]=p,n[1]=b):b=n[1],T.useEffect(b);let g,m;n[2]!==s||n[3]!==u?(g=()=>s.on("command.helpOrDescribe",y=>{if(!y.commandText.trim().toLowerCase().startsWith("help"))return;const v=rf(y.commandText);Ri(v)!==null&&(u(v),h.current())}),m=[s,u],n[2]=s,n[3]=u,n[4]=g,n[5]=m):(g=n[4],m=n[5]),T.useEffect(g,m)}function C1(r){const n=xe.c(12),{ctx:s,navigate:u,addToast:p,wsPath:h}=r;let b;n[0]===Symbol.for("react.memo_cache_sentinel")?(b=On.find(j1),n[0]=b):b=n[0];const g=b,m=T.useRef(null),y=g==null?void 0:g.wsPath;let w,v;n[1]!==p||n[2]!==s||n[3]!==u?(w=()=>{if(!y||!m.current)return;if(s.getSlot(y).phase==="connected"){const{wsPath:j,json:k}=m.current;m.current=null,s.setPendingPayload(j,k),u(g.path),p("JSON loaded into JSON-Path editor ✓","success")}},v=[y,s,u,p,g],n[1]=p,n[2]=s,n[3]=u,n[4]=w,n[5]=v):(w=n[4],v=n[5]),T.useEffect(w,v);let x;n[6]!==p||n[7]!==s||n[8]!==u?(x=_=>{if(!g)return;const j=s.getSlot(g.wsPath);j.phase==="connected"?(s.setPendingPayload(g.wsPath,_),u(g.path),p("JSON loaded into JSON-Path editor ✓","success")):j.phase==="connecting"?(m.current={wsPath:g.wsPath,json:_},p("Updated pending JSON transfer — latest payload will open when connected","info")):(m.current={wsPath:g.wsPath,json:_},s.connect(g.wsPath,p),p("Connecting to JSON-Path Playground…","info"))},n[6]=p,n[7]=s,n[8]=u,n[9]=x):x=n[9];const N=x,E=g&&h!==g.wsPath?N:void 0;let C;return n[10]!==E?(C={handleSendToJsonPath:E},n[10]=E,n[11]=C):C=n[11],C}function j1(r){return r.tabs.includes("payload")&&r.supportsUpload}function M1(r){const n=xe.c(7),{bus:s,onOpenModal:u,modalOpen:p}=r,h=T.useRef(!1);let b,g;n[0]!==p?(b=()=>{p||(h.current=!1)},g=[p],n[0]=p,n[1]=b,n[2]=g):(b=n[1],g=n[2]),T.useEffect(b,g);let m,y;n[3]!==s||n[4]!==u?(m=()=>s.on("upload.invitation",w=>{h.current||(h.current=!0,u(w.uploadPath))}),y=[s,u],n[3]=s,n[4]=u,n[5]=m,n[6]=y):(m=n[5],y=n[6]),T.useEffect(m,y)}function D1(r){const n=xe.c(17),{bus:s,addToast:u}=r,[p,h]=T.useState(null),b=T.useRef(null);let g;n[0]===Symbol.for("react.memo_cache_sentinel")?(g=new Set,n[0]=g):g=n[0];const[m,y]=T.useState(g);let w;n[1]===Symbol.for("react.memo_cache_sentinel")?(w=B=>{b.current=document.activeElement,h(B)},n[1]=w):w=n[1];const v=w;let x;n[2]===Symbol.for("react.memo_cache_sentinel")?(x=()=>{h(null),setTimeout(()=>{var B;return(B=b.current)==null?void 0:B.focus()},0)},n[2]=x):x=n[2];const N=x;let E;n[3]!==u||n[4]!==p?(E=B=>{y(I=>new Set([...I,p])),h(null),setTimeout(()=>{var I;return(I=b.current)==null?void 0:I.focus()},0),u("Mock data uploaded successfully ✓","success")},n[3]=u,n[4]=p,n[5]=E):E=n[5];const C=E;let _;n[6]!==u?(_=B=>{u(`Upload failed: ${B}`,"error")},n[6]=u,n[7]=_):_=n[7];const j=_;let k;n[8]===Symbol.for("react.memo_cache_sentinel")?(k=()=>{y(new Set)},n[8]=k):k=n[8];const H=k,L=p!==null;let X;n[9]!==s||n[10]!==L?(X={bus:s,onOpenModal:v,modalOpen:L},n[9]=s,n[10]=L,n[11]=X):X=n[11],M1(X);let Y;return n[12]!==j||n[13]!==C||n[14]!==p||n[15]!==m?(Y={modalUploadPath:p,successfulUploadPaths:m,handleOpenUploadModal:v,handleCloseUploadModal:N,handleUploadSuccess:C,handleUploadError:j,resetSuccessfulPaths:H},n[12]=j,n[13]=C,n[14]=p,n[15]=m,n[16]=Y):Y=n[16],Y}function O1(r){const n=xe.c(14),{bus:s,connected:u,appendMessage:p,addToast:h}=r,b=T.useRef(null),g=T.useRef(!1),m=T.useRef(p);let y,w;n[0]!==p?(y=()=>{m.current=p},w=[p],n[0]=p,n[1]=y,n[2]=w):(y=n[1],w=n[2]),T.useEffect(y,w);const v=T.useRef(h);let x,N;n[3]!==h?(x=()=>{v.current=h},N=[h],n[3]=h,n[4]=x,n[5]=N):(x=n[4],N=n[5]),T.useEffect(x,N);let E,C;n[6]!==u?(E=()=>{var L;u||((L=b.current)==null||L.abort(),b.current=null,g.current=!1)},C=[u],n[6]=u,n[7]=E,n[8]=C):(E=n[7],C=n[8]),T.useEffect(E,C);let _,j;n[9]===Symbol.for("react.memo_cache_sentinel")?(_=()=>()=>{var L;(L=b.current)==null||L.abort()},j=[],n[9]=_,n[10]=j):(_=n[9],j=n[10]),T.useEffect(_,j);let k,H;n[11]!==s?(H=()=>s.on("payload.large",L=>{var V;if(g.current)return;const{apiPath:X,byteSize:Y}=L;(V=b.current)==null||V.abort();const B=new AbortController;b.current=B;const I=(Y/1048576).toFixed(2);v.current(`Fetching large payload (${I} MB)…`,"info"),g.current=!0,fetch(X,{signal:B.signal}).then(k1).then(K=>{if(!K.trim())throw new Error("empty response body");let $=K;try{$=JSON.stringify(JSON.parse(K),null,2)}catch{}m.current($),g.current=!1,b.current=null}).catch(K=>{K.name!=="AbortError"&&(g.current=!1,b.current=null,m.current(`ERROR: payload fetch failed — ${K.message}`),v.current(`Payload fetch failed: ${K.message}`,"error"))})}),k=[s],n[11]=s,n[12]=k,n[13]=H):(k=n[12],H=n[13]),T.useEffect(H,k)}function k1(r){if(!r.ok)throw new Error(`HTTP ${r.status}`);return r.text()}function R1(r){const n=xe.c(14);let s;n[0]===Symbol.for("react.memo_cache_sentinel")?(s={},n[0]=s):s=n[0];const[u,p]=oa(r,s);let h;n[1]!==p?(h=E=>{p(C=>({...C,[E]:{name:E,savedAt:new Date().toISOString()}}))},n[1]=p,n[2]=h):h=n[2];const b=h;let g;n[3]!==p?(g=E=>{p(C=>{const _={...C};return delete _[E],_})},n[3]=p,n[4]=g):g=n[4];const m=g;let y;n[5]!==u?(y=E=>Object.prototype.hasOwnProperty.call(u,E),n[5]=u,n[6]=y):y=n[6];const w=y;let v;n[7]!==u?(v=Object.values(u).sort(z1),n[7]=u,n[8]=v):v=n[8];const x=v;let N;return n[9]!==m||n[10]!==w||n[11]!==b||n[12]!==x?(N={savedGraphs:x,saveGraph:b,deleteGraph:m,hasGraph:w},n[9]=m,n[10]=w,n[11]=b,n[12]=x,n[13]=N):N=n[13],N}function z1(r,n){return new Date(n.savedAt).getTime()-new Date(r.savedAt).getTime()}function B1(r,n){const s=xe.c(11),[u,p]=oa(r,1),h=T.useRef(!1),[b,g]=T.useState(null),[m,y]=T.useState(null);let w,v;s[0]!==n?(w=()=>n.on("command.importGraph",k=>{g(k.graphName),y(null)}),v=[n],s[0]=n,s[1]=w,s[2]=v):(w=s[1],v=s[2]),T.useEffect(w,v);let x;s[3]!==u?(x=k=>{y(k),k===`untitled-${u}`&&(h.current=!0)},s[3]=u,s[4]=x):x=s[4];const N=x;let E;s[5]!==p?(E=()=>{g(null),y(null),h.current&&p(H1),h.current=!1},s[5]=p,s[6]=E):E=s[6];const C=E,_=m??b??`untitled-${u}`;let j;return s[7]!==_||s[8]!==C||s[9]!==N?(j={defaultName:_,setLastSavedName:N,resetName:C},s[7]=_,s[8]=C,s[9]=N,s[10]=j):j=s[10],j}function H1(r){return r+1}function G1(r){const n=xe.c(27),{bus:s,connected:u,sendRawText:p,saveGraph:h,setLastSavedName:b,addToast:g}=r,m=T.useRef(null);let y;n[0]!==g||n[1]!==u||n[2]!==p?(y=Y=>{if(!u){g("Save failed: connection required to export graph","error");return}const B=setTimeout(()=>{m.current!==null&&(m.current=null,g("Save failed: export confirmation timed out","error"))},1e4);m.current={graphName:Y,timeoutId:B},p(`export graph as ${Y}`)},n[0]=g,n[1]=u,n[2]=p,n[3]=y):y=n[3];const w=y;let v,x;n[4]!==g||n[5]!==s||n[6]!==h||n[7]!==b?(v=()=>s.on("graph.exported",Y=>{if(m.current===null||Y.graphName!==m.current.graphName)return;clearTimeout(m.current.timeoutId);const B=m.current.graphName;m.current=null,h(B),b(B),g(`Graph saved as "${B}"`,"success")}),x=[s,h,b,g],n[4]=g,n[5]=s,n[6]=h,n[7]=b,n[8]=v,n[9]=x):(v=n[8],x=n[9]),T.useEffect(v,x);let N,E;n[10]!==g||n[11]!==s?(N=()=>s.on("graph.export.failed",Y=>{m.current!==null&&(clearTimeout(m.current.timeoutId),m.current=null,Y.reason==="invalid-name"?g("Save failed: invalid filename (a–z, A–Z, 0–9, hyphen only)","error"):g("Save failed: root node name does not match existing graph","error"))}),E=[s,g],n[10]=g,n[11]=s,n[12]=N,n[13]=E):(N=n[12],E=n[13]),T.useEffect(N,E);let C,_;n[14]!==g||n[15]!==u?(C=()=>{!u&&m.current!==null&&(clearTimeout(m.current.timeoutId),m.current=null,g("Save failed: connection closed before export confirmation","error"))},_=[u,g],n[14]=g,n[15]=u,n[16]=C,n[17]=_):(C=n[16],_=n[17]),T.useEffect(C,_);let j,k;n[18]===Symbol.for("react.memo_cache_sentinel")?(j=()=>()=>{m.current!==null&&clearTimeout(m.current.timeoutId)},k=[],n[18]=j,n[19]=k):(j=n[18],k=n[19]),T.useEffect(j,k);let H;n[20]!==g||n[21]!==u||n[22]!==p?(H=Y=>{u&&(p(`import graph from ${Y}`),g(`Importing graph "${Y}"…`,"info"))},n[20]=g,n[21]=u,n[22]=p,n[23]=H):H=n[23];const L=H;let X;return n[24]!==L||n[25]!==w?(X={handleSaveGraph:w,handleLoadGraph:L},n[24]=L,n[25]=w,n[26]=X):X=n[26],X}const oc=new Map;function U1(r){const n=xe.c(7);let s;n[0]!==r?(s=()=>oc.get(r)??null,n[0]=r,n[1]=s):s=n[1];const[u,p]=T.useState(s);let h;n[2]!==r?(h=m=>{p(m),m===null?oc.delete(r):oc.set(r,m)},n[2]=r,n[3]=h):h=n[3];const b=h;let g;return n[4]!==u||n[5]!==b?(g=[u,b],n[4]=u,n[5]=b,n[6]=g):g=n[6],g}function Dh(r){if(r==null)return"";const n=typeof r=="string"?r:JSON.stringify(r);return n.includes("'''")&&console.warn(`[commandBuilder] Property value contains "'''" which cannot be escaped in the backend grammar. The value may be truncated on paste.`),n.includes(`
`)?`'''
${n}
'''`:n}function L1(r,n){const s=[`${r} node ${n.alias}`];n.types.length>0&&s.push(`with type ${n.types[0]}`);const u=Object.entries(n.properties).filter(([,p])=>p!=null);if(u.length>0){s.push("with properties");for(const[p,h]of u)if(Array.isArray(h))for(const b of h)s.push(`${p}[]=${Dh(b)}`);else s.push(`${p}[]=${Dh(h)}`)}return s.join(`
`)}const Y1="_toastContainer_hhy5k_1",q1="_toast_hhy5k_1",X1="_slideIn_hhy5k_1",J1="_success_hhy5k_36",I1="_error_hhy5k_40",Z1="_info_hhy5k_44",Q1="_toastIcon_hhy5k_48",V1="_toastMessage_hhy5k_53",ro={toastContainer:Y1,toast:q1,slideIn:X1,success:J1,error:I1,info:Z1,toastIcon:Q1,toastMessage:V1},K1=r=>{const n=xe.c(7),{toasts:s,onRemove:u}=r;if(s.length===0)return null;let p;if(n[0]!==u||n[1]!==s){let b;n[3]!==u?(b=g=>f.jsxs("div",{className:`${ro.toast} ${ro[g.type]}`,onClick:()=>u(g.id),children:[f.jsxs("span",{className:ro.toastIcon,children:[g.type==="success"&&"✅",g.type==="error"&&"❌",g.type==="info"&&"ℹ️"]}),f.jsx("span",{className:ro.toastMessage,children:g.message})]},g.id),n[3]=u,n[4]=b):b=n[4],p=s.map(b),n[0]=u,n[1]=s,n[2]=p}else p=n[2];let h;return n[5]!==p?(h=f.jsx("div",{className:ro.toastContainer,children:p}),n[5]=p,n[6]=h):h=n[6],h},$1="_container_9dbh2_3",W1="_trigger_9dbh2_7",P1="_chevron_9dbh2_37",F1="_chevronOpen_9dbh2_43",e0="_dot_9dbh2_49",t0="_dotIdle_9dbh2_56",n0="_dotConnecting_9dbh2_57",a0="_dotConnected_9dbh2_58",l0="_dotPartial_9dbh2_59",o0="_dropdown_9dbh2_65",dn={container:$1,trigger:W1,chevron:P1,chevronOpen:F1,dot:e0,dotIdle:t0,dotConnecting:n0,dotConnected:a0,dotPartial:l0,dropdown:o0};function hc(r){const n=xe.c(23),{label:s,dotStatus:u,children:p}=r,[h,b]=T.useState(!1),g=T.useRef(null);let m,y;n[0]!==h?(m=()=>{if(!h)return;const X=Y=>{g.current&&!g.current.contains(Y.target)&&b(!1)};return document.addEventListener("mousedown",X),()=>document.removeEventListener("mousedown",X)},y=[h],n[0]=h,n[1]=m,n[2]=y):(m=n[1],y=n[2]),T.useEffect(m,y);let w;n[3]===Symbol.for("react.memo_cache_sentinel")?(w=X=>{var Y,B;X.key==="Escape"&&(b(!1),(B=(Y=g.current)==null?void 0:Y.querySelector("button[aria-haspopup]"))==null||B.focus())},n[3]=w):w=n[3];const v=w,x=u==="connected"?dn.dotConnected:u==="connecting"?dn.dotConnecting:u==="partial"?dn.dotPartial:u==="idle"?dn.dotIdle:void 0;let N;n[4]===Symbol.for("react.memo_cache_sentinel")?(N=()=>b(i0),n[4]=N):N=n[4];let E;n[5]!==x||n[6]!==u?(E=u!==void 0&&f.jsx("span",{className:`${dn.dot} ${x??""}`,"aria-hidden":"true"}),n[5]=x,n[6]=u,n[7]=E):E=n[7];let C;n[8]!==s?(C=f.jsx("span",{children:s}),n[8]=s,n[9]=C):C=n[9];const _=`${dn.chevron} ${h?dn.chevronOpen:""}`;let j;n[10]!==_?(j=f.jsx("span",{className:_,"aria-hidden":"true",children:"▾"}),n[10]=_,n[11]=j):j=n[11];let k;n[12]!==h||n[13]!==E||n[14]!==C||n[15]!==j?(k=f.jsxs("button",{className:dn.trigger,onClick:N,"aria-haspopup":"true","aria-expanded":h,children:[E,C,j]}),n[12]=h,n[13]=E,n[14]=C,n[15]=j,n[16]=k):k=n[16];let H;n[17]!==p||n[18]!==h?(H=h&&f.jsx("div",{className:dn.dropdown,role:"menu",children:p}),n[17]=p,n[18]=h,n[19]=H):H=n[19];let L;return n[20]!==H||n[21]!==k?(L=f.jsxs("div",{className:dn.container,ref:g,onKeyDown:v,children:[k,H]}),n[20]=H,n[21]=k,n[22]=L):L=n[22],L}function i0(r){return!r}const s0="_nav_1hfby_3",r0="_menuList_1hfby_11",c0="_menuItem_1hfby_19",u0="_toolRow_1hfby_56",d0="_toolLink_1hfby_67",p0="_toolLinkActive_1hfby_92",h0="_toolDot_1hfby_99",f0="_toolDotIdle_1hfby_106",m0="_toolDotConnecting_1hfby_107",g0="_toolDotConnected_1hfby_108",y0="_connectAllRow_1hfby_112",b0="_connectAllBtn_1hfby_118",v0="_connectAllBtnStop_1hfby_142",_0="_toolConnectBtn_1hfby_154",S0="_toolConnectBtnStop_1hfby_180",x0="_externalIcon_1hfby_192",ft={nav:s0,menuList:r0,menuItem:c0,toolRow:u0,toolLink:d0,toolLinkActive:p0,toolDot:h0,toolDotIdle:f0,toolDotConnecting:m0,toolDotConnected:g0,connectAllRow:y0,connectAllBtn:b0,connectAllBtnStop:v0,toolConnectBtn:_0,toolConnectBtnStop:S0,externalIcon:x0};function w0(r){return r.every(n=>n==="connected")?"connected":r.every(n=>n==="idle")?"idle":r.some(n=>n==="connecting")?"connecting":"partial"}function T0(r){return r==="connected"?"connected":r==="connecting"?"connecting":"idle"}const E0=[{href:"/info",label:"Info"},{href:"/info/lib",label:"Libraries"},{href:"/info/routes",label:"Services"},{href:"/health",label:"Health"},{href:"/env",label:"Environment"},{href:"http://localhost:8085/api/ws/json",label:"Legacy JSON"},{href:"http://localhost:8085/api/ws/graph",label:"Legacy Graph"}];function A0(r){const n=xe.c(27),{addToast:s}=r,u=xc();let p,h,b;if(n[0]!==u){const Y=On.map(B=>u.getSlot(B.wsPath).phase);b=w0(Y),p=Y.every(M0),h=Y.some(j0),n[0]=u,n[1]=p,n[2]=h,n[3]=b}else p=n[1],h=n[2],b=n[3];const g=h;let m;n[4]!==s||n[5]!==u?(m=function(){On.forEach(B=>{u.getSlot(B.wsPath).phase==="idle"&&u.connect(B.wsPath,s)})},n[4]=s,n[5]=u,n[6]=m):m=n[6];const y=m;let w;n[7]!==u?(w=function(){On.forEach(B=>{const{phase:I}=u.getSlot(B.wsPath);(I==="connected"||I==="connecting")&&u.disconnect(B.wsPath)})},n[7]=u,n[8]=w):w=n[8];const v=w,x=`${ft.connectAllBtn} ${p?ft.connectAllBtnStop:""}`,N=p?v:y,E=g?"Connecting…":p?"Disconnect all WebSockets":"Connect all WebSockets",C=g?"Connecting…":p?"Disconnect All":"Connect All";let _;n[9]!==g||n[10]!==x||n[11]!==N||n[12]!==E||n[13]!==C?(_=f.jsx("div",{className:ft.connectAllRow,children:f.jsx("button",{className:x,onClick:N,disabled:g,"aria-label":E,children:C})}),n[9]=g,n[10]=x,n[11]=N,n[12]=E,n[13]=C,n[14]=_):_=n[14];let j;n[15]!==s||n[16]!==u?(j=On.map(Y=>{const{phase:B}=u.getSlot(Y.wsPath),I=T0(B),V=B==="connected",K=B==="connecting",$=I==="connected"?ft.toolDotConnected:I==="connecting"?ft.toolDotConnecting:ft.toolDotIdle;return f.jsxs("li",{role:"none",className:ft.toolRow,children:[f.jsxs(Og,{to:Y.path,role:"menuitem",className:C0,children:[f.jsx("span",{className:`${ft.toolDot} ${$}`,"aria-hidden":"true"}),f.jsx("span",{className:ft.toolLabel,children:Y.label})]}),f.jsx("button",{className:`${ft.toolConnectBtn} ${V?ft.toolConnectBtnStop:""}`,onClick:()=>V||K?u.disconnect(Y.wsPath):u.connect(Y.wsPath,s),disabled:K,"aria-label":K?"Connecting…":V?`Disconnect ${Y.label}`:`Connect ${Y.label}`,title:K?"Connecting…":tf(Y.wsPath),children:K?"…":V?"Stop":"Start"})]},Y.path)}),n[15]=s,n[16]=u,n[17]=j):j=n[17];let k;n[18]!==j?(k=f.jsx("ul",{className:ft.menuList,role:"none",children:j}),n[18]=j,n[19]=k):k=n[19];let H;n[20]!==k||n[21]!==_||n[22]!==b?(H=f.jsxs(hc,{label:"Tools",dotStatus:b,children:[_,k]}),n[20]=k,n[21]=_,n[22]=b,n[23]=H):H=n[23];let L;n[24]===Symbol.for("react.memo_cache_sentinel")?(L=f.jsx(hc,{label:"Quick Links",children:f.jsx("ul",{className:ft.menuList,role:"none",children:E0.map(N0)})}),n[24]=L):L=n[24];let X;return n[25]!==H?(X=f.jsxs("nav",{className:ft.nav,"aria-label":"Main navigation",children:[H,L]}),n[25]=H,n[26]=X):X=n[26],X}function N0(r){return f.jsx("li",{role:"none",children:f.jsxs("a",{href:r.href,role:"menuitem",className:ft.menuItem,target:"_blank",rel:"noopener noreferrer",children:[r.label,f.jsx("span",{className:ft.externalIcon,"aria-hidden":"true",children:"↗"})]})},r.href)}function C0(r){const{isActive:n}=r;return`${ft.toolLink} ${n?ft.toolLinkActive:""}`}function j0(r){return r==="connecting"}function M0(r){return r==="connected"}const D0="_saveBtn_1xd2l_3",O0="_saveForm_1xd2l_33",k0="_saveInput_1xd2l_39",R0="_saveInputWarn_1xd2l_55",z0="_saveWarnLabel_1xd2l_59",B0="_saveActionBtn_1xd2l_65",Ca={saveBtn:D0,saveForm:O0,saveInput:k0,saveInputWarn:R0,saveWarnLabel:z0,saveActionBtn:B0};function H0(r){const n=xe.c(33),{disabled:s,defaultName:u,onSave:p,nameExists:h,connected:b}=r,g=b===void 0?!1:b,[m,y]=T.useState(!1),[w,v]=T.useState(""),x=T.useRef(null);let N;n[0]!==u?(N=()=>{v(u),y(!0)},n[0]=u,n[1]=N):N=n[1];const E=N;let C;n[2]===Symbol.for("react.memo_cache_sentinel")?(C=()=>{y(!1),v("")},n[2]=C):C=n[2];const _=C;let j;n[3]!==p||n[4]!==w?(j=()=>{const K=w.trim();K&&(p(K),y(!1),v(""))},n[3]=p,n[4]=w,n[5]=j):j=n[5];const k=j;let H;n[6]!==k?(H=K=>{K.key==="Enter"&&(K.preventDefault(),k()),K.key==="Escape"&&(K.preventDefault(),_())},n[6]=k,n[7]=H):H=n[7];const L=H;let X,Y;if(n[8]!==m?(X=()=>{var K;m&&((K=x.current)==null||K.focus())},Y=[m],n[8]=m,n[9]=X,n[10]=Y):(X=n[9],Y=n[10]),T.useEffect(X,Y),m){const K=`${Ca.saveInput}${h!=null&&h(w.trim())?` ${Ca.saveInputWarn}`:""}`;let $;n[11]===Symbol.for("react.memo_cache_sentinel")?($=P=>v(P.target.value),n[11]=$):$=n[11];let te;n[12]!==L||n[13]!==w||n[14]!==K?(te=f.jsx("input",{ref:x,className:K,type:"text",value:w,onChange:$,onKeyDown:L,placeholder:"Enter a name…","aria-label":"Graph save name",maxLength:80}),n[12]=L,n[13]=w,n[14]=K,n[15]=te):te=n[15];let ue;n[16]!==h||n[17]!==w?(ue=(h==null?void 0:h(w.trim()))&&f.jsx("span",{className:Ca.saveWarnLabel,role:"status",children:"Overwrite?"}),n[16]=h,n[17]=w,n[18]=ue):ue=n[18];let he;n[19]!==w?(he=w.trim(),n[19]=w,n[20]=he):he=n[20];const re=!he;let D;n[21]!==k||n[22]!==re?(D=f.jsx("button",{className:Ca.saveActionBtn,onClick:k,disabled:re,"aria-label":"Confirm save",children:"✅"}),n[21]=k,n[22]=re,n[23]=D):D=n[23];let O;n[24]===Symbol.for("react.memo_cache_sentinel")?(O=f.jsx("button",{className:Ca.saveActionBtn,onClick:_,"aria-label":"Cancel save",children:"❌"}),n[24]=O):O=n[24];let Q;return n[25]!==te||n[26]!==ue||n[27]!==D?(Q=f.jsxs("div",{className:Ca.saveForm,children:[te,ue,D,O]}),n[25]=te,n[26]=ue,n[27]=D,n[28]=Q):Q=n[28],Q}const B=s||!g,I=s?"No graph loaded":g?"Export graph snapshot to server and save bookmark":"Connect first to save";let V;return n[29]!==E||n[30]!==B||n[31]!==I?(V=f.jsx("button",{className:Ca.saveBtn,onClick:E,disabled:B,title:I,"aria-label":"Save graph snapshot",children:"💾 Save Graph"}),n[29]=E,n[30]=B,n[31]=I,n[32]=V):V=n[32],V}const G0="_empty_tpeii_3",U0="_hint_tpeii_12",L0="_list_tpeii_21",Y0="_row_tpeii_31",q0="_rowInfo_tpeii_50",X0="_rowName_tpeii_58",J0="_rowMeta_tpeii_67",I0="_rowActions_tpeii_78",Z0="_loadBtn_tpeii_84",Q0="_deleteBtn_tpeii_85",pn={empty:G0,hint:U0,list:L0,row:Y0,rowInfo:q0,rowName:X0,rowMeta:J0,rowActions:I0,loadBtn:Z0,deleteBtn:Q0};function V0(r){const n=xe.c(8),{savedGraphs:s,onLoad:u,onDelete:p,connected:h}=r,b=s.length>0?`Load Graph (${s.length})`:"Load Graph";let g;n[0]!==h||n[1]!==p||n[2]!==u||n[3]!==s?(g=s.length===0?f.jsx("p",{className:pn.empty,children:"No saved graphs yet."}):f.jsxs(f.Fragment,{children:[!h&&f.jsx("p",{className:pn.hint,children:"Connect to load a graph"}),f.jsx("ul",{className:pn.list,role:"list",children:s.map(y=>f.jsxs("li",{className:pn.row,children:[f.jsxs("div",{className:pn.rowInfo,children:[f.jsx("span",{className:pn.rowName,title:y.name,children:y.name}),f.jsx("span",{className:pn.rowMeta,children:new Date(y.savedAt).toLocaleString()})]}),f.jsxs("div",{className:pn.rowActions,children:[f.jsx("button",{className:pn.loadBtn,onClick:()=>u(y.name),disabled:!h,title:h?`Run: import graph from ${y.name}`:"Connect to the playground first","aria-label":`Load graph ${y.name}`,children:"Load"}),f.jsx("button",{className:pn.deleteBtn,onClick:()=>p(y.name),title:`Remove "${y.name}" from local storage`,"aria-label":`Delete saved graph ${y.name}`,children:"Delete"})]})]},y.name))})]}),n[0]=h,n[1]=p,n[2]=u,n[3]=s,n[4]=g):g=n[4];let m;return n[5]!==b||n[6]!==g?(m=f.jsx(hc,{label:b,children:g}),n[5]=b,n[6]=g,n[7]=m):m=n[7],m}const K0="_payloadRoot_6u47x_2",$0="_labelRow_6u47x_10",W0="_label_6u47x_10",P0="_payloadControls_6u47x_26",F0="_charCounter_6u47x_32",eb="_typeIndicator_6u47x_38",tb="_validationIcon_6u47x_49",nb="_formatButton_6u47x_53",ab="_uploadButton_6u47x_67",lb="_textarea_6u47x_82",ob="_textareaError_6u47x_107",ib="_errorMessage_6u47x_109",sb="_sampleButtonsRow_6u47x_117",rb="_sampleButtons_6u47x_117",cb="_sampleLabel_6u47x_130",ub="_sampleGroup_6u47x_136",db="_sampleGroupLabel_6u47x_143",pb="_sampleButton_6u47x_117",at={payloadRoot:K0,labelRow:$0,label:W0,payloadControls:P0,charCounter:F0,typeIndicator:eb,validationIcon:tb,formatButton:nb,uploadButton:ab,textarea:lb,textareaError:ob,errorMessage:ib,sampleButtonsRow:sb,sampleButtons:rb,sampleLabel:cb,sampleGroup:ub,sampleGroupLabel:db,sampleButton:pb};function hb(r){const n=xe.c(21),{onLoad:s}=r;let u,p,h,b,g,m;if(n[0]!==s){const v=Object.keys(Mi).filter(gb),x=Object.keys(Mi).filter(mb),N=fb;b=at.sampleButtons,n[7]===Symbol.for("react.memo_cache_sentinel")?(g=f.jsx("span",{className:at.sampleLabel,children:"Quick load:"}),n[7]=g):g=n[7];let E;n[8]===Symbol.for("react.memo_cache_sentinel")?(E=f.jsx("span",{className:at.sampleGroupLabel,children:"JSON:"}),n[8]=E):E=n[8];const C=v.map(_=>f.jsx("button",{className:at.sampleButton,onClick:()=>s(Mi[_]),children:N(_)},_));n[9]!==C?(m=f.jsxs("div",{className:at.sampleGroup,children:[E,C]}),n[9]=C,n[10]=m):m=n[10],u=at.sampleGroup,n[11]===Symbol.for("react.memo_cache_sentinel")?(p=f.jsx("span",{className:at.sampleGroupLabel,children:"XML:"}),n[11]=p):p=n[11],h=x.map(_=>f.jsx("button",{className:at.sampleButton,onClick:()=>s(Mi[_]),children:N(_)},_)),n[0]=s,n[1]=u,n[2]=p,n[3]=h,n[4]=b,n[5]=g,n[6]=m}else u=n[1],p=n[2],h=n[3],b=n[4],g=n[5],m=n[6];let y;n[12]!==u||n[13]!==p||n[14]!==h?(y=f.jsxs("div",{className:u,children:[p,h]}),n[12]=u,n[13]=p,n[14]=h,n[15]=y):y=n[15];let w;return n[16]!==b||n[17]!==g||n[18]!==m||n[19]!==y?(w=f.jsxs("div",{className:b,children:[g,m,y]}),n[16]=b,n[17]=g,n[18]=m,n[19]=y,n[20]=w):w=n[20],w}function fb(r){return r.replace(/^(json|xml)_/,"").replace(/_/g," ")}function mb(r){return r.startsWith("xml_")}function gb(r){return r.startsWith("json_")}function yb(r){const n=xe.c(40),{payload:s,onChange:u,validation:p,onFormat:h,onUpload:b}=r;let g;n[0]===Symbol.for("react.memo_cache_sentinel")?(g=f.jsx("label",{htmlFor:"payload",className:at.label,children:"JSON/XML Payload"}),n[0]=g):g=n[0];let m;n[1]!==s.length?(m=f.jsxs("span",{className:at.charCounter,children:["size: ",s.length]}),n[1]=s.length,n[2]=m):m=n[2];let y;n[3]!==s||n[4]!==p.type?(y=s&&p.type&&f.jsx("span",{className:at.typeIndicator,children:p.type.toUpperCase()}),n[3]=s,n[4]=p.type,n[5]=y):y=n[5];let w;n[6]!==s||n[7]!==p.valid?(w=s&&f.jsx("span",{className:at.validationIcon,children:p.valid?"✅":"❌"}),n[6]=s,n[7]=p.valid,n[8]=w):w=n[8];const v=!s||p.type!=="json",x=p.type==="xml"?"Format only available for JSON":"Format JSON";let N;n[9]!==h||n[10]!==v||n[11]!==x?(N=f.jsx("button",{className:at.formatButton,onClick:h,disabled:v,title:x,children:"Format"}),n[9]=h,n[10]=v,n[11]=x,n[12]=N):N=n[12];let E;n[13]!==b||n[14]!==s||n[15]!==p.type||n[16]!==p.valid?(E=b!==void 0&&f.jsx("button",{className:at.uploadButton,onClick:b,disabled:!s||!p.valid||p.type!=="json",title:"Upload JSON payload to current session via REST",children:"Upload"}),n[13]=b,n[14]=s,n[15]=p.type,n[16]=p.valid,n[17]=E):E=n[17];let C;n[18]!==m||n[19]!==y||n[20]!==w||n[21]!==N||n[22]!==E?(C=f.jsxs("div",{className:at.labelRow,children:[g,f.jsxs("div",{className:at.payloadControls,children:[m,y,w,N,E]})]}),n[18]=m,n[19]=y,n[20]=w,n[21]=N,n[22]=E,n[23]=C):C=n[23];const _=`${at.textarea} ${p.valid?"":at.textareaError}`;let j;n[24]!==u?(j=Y=>u(Y.target.value),n[24]=u,n[25]=j):j=n[25];let k;n[26]!==s||n[27]!==_||n[28]!==j?(k=f.jsx("textarea",{id:"payload",className:_,placeholder:"Paste your JSON/XML payload here",value:s,onChange:j}),n[26]=s,n[27]=_,n[28]=j,n[29]=k):k=n[29];let H;n[30]!==p.error||n[31]!==p.valid?(H=!p.valid&&f.jsx("div",{className:at.errorMessage,children:p.error}),n[30]=p.error,n[31]=p.valid,n[32]=H):H=n[32];let L;n[33]!==u?(L=f.jsx("div",{className:at.sampleButtonsRow,children:f.jsx(hb,{onLoad:u})}),n[33]=u,n[34]=L):L=n[34];let X;return n[35]!==k||n[36]!==H||n[37]!==L||n[38]!==C?(X=f.jsxs("div",{className:at.payloadRoot,children:[C,k,H,L]}),n[35]=k,n[36]=H,n[37]=L,n[38]=C,n[39]=X):X=n[39],X}const bb="_content_138ap_8",vb="_header_138ap_22",_b="_icon_138ap_42",Sb="_alias_138ap_47",xb="_badge_138ap_53",wb="_body_138ap_65",Tb="_row_138ap_70",Eb="_label_138ap_83",Ab="_value_138ap_89",Nb="_edgeHandle_138ap_103",ln={content:bb,header:vb,icon:_b,alias:Sb,badge:xb,body:wb,row:Tb,label:Eb,value:Ab,edgeHandle:Nb},Cb={Root:{icon:"🚀",label:"Root"},End:{icon:"🏁",label:"End"},Fetcher:{icon:"🌐",label:"Fetcher"},mapper:{icon:"🗺️",label:"Mapper"},Math:{icon:"🔢",label:"Math"},JavaScript:{icon:"📜",label:"JavaScript"},Provider:{icon:"🔌",label:"Provider"},Dictionary:{icon:"📖",label:"Dictionary"},Join:{icon:"🔀",label:"Join"},Extension:{icon:"🧩",label:"Extension"},Island:{icon:"🏝️",label:"Island"},Decision:{icon:"❓",label:"Decision"}};function jb(r){return Cb[r]??{icon:"📦",label:r}}function Oh(r){const n=xe.c(7),{label:s,value:u}=r;let p;n[0]!==s?(p=f.jsx("span",{className:ln.label,children:s}),n[0]=s,n[1]=p):p=n[1];let h;n[2]!==u?(h=f.jsx("span",{className:ln.value,title:u,children:u}),n[2]=u,n[3]=h):h=n[3];let b;return n[4]!==p||n[5]!==h?(b=f.jsxs("div",{className:ln.row,children:[p,h]}),n[4]=p,n[5]=h,n[6]=b):b=n[6],b}function Mb(r){const n=xe.c(3),{properties:s}=r;let u,p;if(n[0]!==s){p=Symbol.for("react.early_return_sentinel");e:{const h=Object.entries(s).filter(Ob);if(h.length===0){p=null;break e}u=f.jsx(f.Fragment,{children:h.map(Db)})}n[0]=s,n[1]=u,n[2]=p}else u=n[1],p=n[2];return p!==Symbol.for("react.early_return_sentinel")?p:u}function Db(r){const[n,s]=r;if(Array.isArray(s))return s.map((p,h)=>{const b=typeof p=="string"?p:JSON.stringify(p);return f.jsx(Oh,{label:h===0?n:"",value:b},`${n}-${h}`)});const u=typeof s=="string"?s:JSON.stringify(s);return f.jsx(Oh,{label:n,value:u},n)}function Ob(r){const[,n]=r;return n!=null}function Yt(r){const n=xe.c(35),{data:s,isConnectable:u,selected:p}=r;let h;n[0]!==s.nodeType?(h=jb(s.nodeType),n[0]=s.nodeType,n[1]=h):h=n[1];const b=h;let g;n[2]!==s.minHeight||n[3]!==p?(g=f.jsx(Hg,{minWidth:180,minHeight:s.minHeight,isVisible:p}),n[2]=s.minHeight,n[3]=p,n[4]=g):g=n[4];let m;if(n[5]!==s.targetHandles||n[6]!==u){let j;n[8]!==u?(j=k=>{const{id:H,offset:L}=k;return f.jsx(Sh,{id:H,type:"target",position:xh.Left,isConnectable:u,className:ln.edgeHandle,style:{top:`calc(50% + ${L}px)`}},H)},n[8]=u,n[9]=j):j=n[9],m=s.targetHandles.map(j),n[5]=s.targetHandles,n[6]=u,n[7]=m}else m=n[7];let y;n[10]!==b.icon?(y=f.jsx("span",{className:ln.icon,children:b.icon}),n[10]=b.icon,n[11]=y):y=n[11];let w;n[12]!==s.alias?(w=f.jsx("span",{className:ln.alias,children:s.alias}),n[12]=s.alias,n[13]=w):w=n[13];let v;n[14]!==b.label?(v=f.jsx("span",{className:ln.badge,children:b.label}),n[14]=b.label,n[15]=v):v=n[15];let x;n[16]!==y||n[17]!==w||n[18]!==v?(x=f.jsxs("div",{className:ln.header,children:[y,w,v]}),n[16]=y,n[17]=w,n[18]=v,n[19]=x):x=n[19];let N;n[20]!==s.properties?(N=f.jsx("div",{className:ln.body,children:f.jsx(Mb,{properties:s.properties})}),n[20]=s.properties,n[21]=N):N=n[21];let E;n[22]!==x||n[23]!==N?(E=f.jsxs("div",{className:ln.content,children:[x,N]}),n[22]=x,n[23]=N,n[24]=E):E=n[24];let C;if(n[25]!==s.sourceHandles||n[26]!==u){let j;n[28]!==u?(j=k=>{const{id:H,offset:L}=k;return f.jsx(Sh,{id:H,type:"source",position:xh.Right,isConnectable:u,className:ln.edgeHandle,style:{top:`calc(50% + ${L}px)`}},H)},n[28]=u,n[29]=j):j=n[29],C=s.sourceHandles.map(j),n[25]=s.sourceHandles,n[26]=u,n[27]=C}else C=n[27];let _;return n[30]!==C||n[31]!==g||n[32]!==m||n[33]!==E?(_=f.jsxs(f.Fragment,{children:[g,m,E,C]}),n[30]=C,n[31]=g,n[32]=m,n[33]=E,n[34]=_):_=n[34],_}const kb={Root:Yt,End:Yt,Fetcher:Yt,mapper:Yt,Math:Yt,JavaScript:Yt,Provider:Yt,Dictionary:Yt,Join:Yt,Extension:Yt,Island:Yt,Decision:Yt,default:Yt},Rb="_graphWrapper_191wx_15",zb="_empty_191wx_22",Bb="_emptyIcon_191wx_35",Hb="_refreshingOverlay_191wx_69",Gb="_refreshingSpinner_191wx_85",Ub="_contextMenu_191wx_100",Lb="_contextMenuItem_191wx_110",an={graphWrapper:Rb,empty:zb,emptyIcon:Bb,refreshingOverlay:Hb,refreshingSpinner:Gb,contextMenu:Ub,contextMenuItem:Lb};class Yb extends T.Component{constructor(){super(...arguments),this.state={caughtError:null}}static getDerivedStateFromError(n){return{caughtError:n instanceof Error?n.message:String(n)}}componentDidCatch(n,s){var p,h;const u=n instanceof Error?n.message:String(n);console.error("[GraphView] Render error:",u,s.componentStack),(h=(p=this.props).onRenderError)==null||h.call(p,`Graph render failed: ${u}`)}render(){return this.state.caughtError?f.jsxs("div",{className:an.empty,children:[f.jsx("span",{className:an.emptyIcon,children:"⚠️"}),f.jsx("span",{children:"Graph could not be rendered."}),f.jsx("span",{children:this.state.caughtError})]}):this.props.children}}const cf=240,ho=100,kh=60,qb=120,Xb={boxSizing:"border-box",borderRadius:"8px",borderWidth:"1.5px",borderStyle:"solid",background:"var(--bg-secondary, #1e1e2e)",color:"var(--text-primary, #cdd6f4)",fontSize:"0.75rem",boxShadow:"0 2px 8px rgba(0,0,0,0.45)",overflow:"visible",padding:0},Jb={Root:"#15803d",End:"#dc2626",Fetcher:"#2563eb",mapper:"#ea580c",Math:"#a16207",JavaScript:"#7e22ce",Provider:"#be185d",Dictionary:"#0e7490",Join:"#65a30d",Extension:"#4338ca",Island:"#475569",Decision:"#b45309"},Ib="#6c7086",fc="rgba(148, 163, 184, 0.42)",Zb="var(--bg-secondary)",ki=24,Qb=32,Rh=["#0369a1","#15803d","#b45309","#7e22ce","#b91c1c","#0f766e","#c2410c","#a16207"],Vb={fetch:"#0369a1",details:"#0369a1","ext-call":"#0369a1",mapping:"#b45309",compute:"#b45309",calculate:"#b45309",evaluate:"#b45309",fork:"#7e22ce",join:"#7e22ce",one:"#7e22ce",two:"#6d28d9",three:"#5b21b6",more:"#4c1d95",done:"#15803d",complete:"#15803d",finish:"#15803d",positive:"#15803d",negative:"#b91c1c"};function Kb(r){let n=0;for(let s=0;s<r.length;s++)n=(n<<5)-n+r.charCodeAt(s),n|=0;return Math.abs(n)}function $b(r){if(r.length===0)return fc;const n=r[0].trim().toLowerCase(),s=Vb[n];return s||Rh[Kb(n)%Rh.length]}function zh(r){return`source-${r}`}function Bh(r){return`target-${r}`}function Wb(r,n){return n<=1?0:n===2?r===0?-ki:ki:(r-(n-1)/2)*ki}function Hh(r,n){return Array.from({length:r},(s,u)=>({id:n(u),offset:Wb(u,r)}))}function Pb(r){return r<=1?ho:Math.max(ho,(r-1)*ki+Qb*2)}function Fb(r){const n=Jb[r]??Ib;return{...Xb,borderColor:n,"--node-accent":n}}function ev(r,n,s){var v;const u=new Map,p=new Map;for(const x of r)u.set(x.alias,[]),p.set(x.alias,0);for(const x of n??[])(v=u.get(x.source))==null||v.push(x.target),p.set(x.target,(p.get(x.target)??0)+1);const h=r.filter(x=>p.get(x.alias)===0||x.types.includes("entry_point")).map(x=>x.alias),b=new Map,g=[...h];for(h.forEach(x=>b.set(x,0));g.length>0;){const x=g.shift(),N=b.get(x)??0;for(const E of u.get(x)??[])(!b.has(E)||b.get(E)<=N)&&(b.set(E,N+1),g.push(E))}const m=b.size>0?Math.max(...b.values()):0;for(const x of r)b.has(x.alias)||b.set(x.alias,m+1);const y=new Map;for(const[x,N]of b)y.has(N)||y.set(N,[]),y.get(N).push(x);const w=new Map;for(const[x,N]of y){let C=-(N.reduce((_,j)=>_+(s.get(j)??ho),0)+Math.max(0,N.length-1)*kh)/2;N.forEach(_=>{const j=s.get(_)??ho;w.set(_,{x:x*(cf+qb),y:C}),C+=j+kh})}return w}function tv(r){const n=r.connections??[],s=new Map,u=new Map;for(const w of n)s.set(w.source,(s.get(w.source)??0)+1),u.set(w.target,(u.get(w.target)??0)+1);const p=new Map(r.nodes.map(w=>[w.alias,Pb(Math.max(s.get(w.alias)??0,u.get(w.alias)??0))])),h=ev(r.nodes,n,p),b=r.nodes.map(w=>{const v=s.get(w.alias)??0,x=u.get(w.alias)??0,N=p.get(w.alias)??ho;return{id:w.alias,type:w.types[0]??"default",position:h.get(w.alias)??{x:0,y:0},width:cf,height:N,style:Fb(w.types[0]??"unknown"),data:{alias:w.alias,nodeType:w.types[0]??"unknown",properties:w.properties,sourceHandles:Hh(v,zh),targetHandles:Hh(x,Bh),minHeight:N}}}),g=new Map,m=new Map,y=[];for(const[w,v]of n.entries()){const x=v.relations.map(j=>j.type),N=`${v.source}__${v.target}__${w}`,E=$b(x),C=g.get(v.source)??0,_=m.get(v.target)??0;g.set(v.source,C+1),m.set(v.target,_+1),y.push({id:N,source:v.source,target:v.target,sourceHandle:zh(C),targetHandle:Bh(_),label:x.join(", "),type:"bezier",markerEnd:{type:Gg.ArrowClosed,width:16,height:16,color:fc},style:{stroke:fc,strokeWidth:2},labelStyle:{fill:E,fontSize:10,fontWeight:700},labelBgStyle:{fill:Zb,fillOpacity:.94,stroke:"rgba(15, 23, 42, 0.16)",strokeWidth:1},labelBgPadding:[5,2],labelBgBorderRadius:6,data:{relationTypes:x}})}return{nodes:b,edges:y}}function nv(r,n){return r.nodes.find(s=>s.alias===n)}function av(r,n){return(r.connections??[]).filter(s=>s.source!==s.target&&(s.source===n||s.target===n))}const lv="_toolbar_10vsj_2",ov="_label_10vsj_12",iv="_toolbarActions_10vsj_18",sv="_toolbarButton_10vsj_24",Di={toolbar:lv,label:ov,toolbarActions:iv,toolbarButton:sv};function uf(r){const n=xe.c(19),{graphData:s,onCopySuccess:u,onCopyError:p,extraActions:h}=r;let b;n[0]!==s||n[1]!==p||n[2]!==u?(b=()=>{s&&navigator.clipboard.writeText(JSON.stringify(s,null,2)).then(()=>u==null?void 0:u()).catch(()=>p==null?void 0:p())},n[0]=s,n[1]=p,n[2]=u,n[3]=b):b=n[3];const g=b,m=(s==null?void 0:s.nodes.length)??0;let y;n[4]!==(s==null?void 0:s.connections)?(y=(s==null?void 0:s.connections)??[],n[4]=s==null?void 0:s.connections,n[5]=y):y=n[5];const w=y.length,v=m!==1?"s":"",x=w!==1?"s":"";let N;n[6]!==w||n[7]!==m||n[8]!==v||n[9]!==x?(N=f.jsxs("span",{className:Di.label,children:[m," node",v," · ",w," connection",x]}),n[6]=w,n[7]=m,n[8]=v,n[9]=x,n[10]=N):N=n[10];let E;n[11]!==g?(E=f.jsx("button",{className:Di.toolbarButton,onClick:g,title:"Copy raw graph JSON to clipboard","aria-label":"Copy raw graph JSON to clipboard",children:"📑"}),n[11]=g,n[12]=E):E=n[12];let C;n[13]!==h||n[14]!==E?(C=f.jsxs("div",{className:Di.toolbarActions,children:[h,E]}),n[13]=h,n[14]=E,n[15]=C):C=n[15];let _;return n[16]!==N||n[17]!==C?(_=f.jsxs("div",{className:Di.toolbar,children:[N,C]}),n[16]=N,n[17]=C,n[18]=_):_=n[18],_}const Gh=[],Uh=[];function rv(r){const n=xe.c(65),{graphData:s,onCopySuccess:u,onCopyError:p,onRenderError:h,isRefreshing:b,onClipNode:g}=r,m=b===void 0?!1:b,[y,w]=T.useState(null),v=T.useRef(null);let x,N;n[0]!==y?(x=()=>{if(!y)return;const ae=fe=>{v.current&&!v.current.contains(fe.target)&&w(null)},de=fe=>{fe.key==="Escape"&&w(null)};return document.addEventListener("mousedown",ae),document.addEventListener("keydown",de),()=>{document.removeEventListener("mousedown",ae),document.removeEventListener("keydown",de)}},N=[y],n[0]=y,n[1]=x,n[2]=N):(x=n[1],N=n[2]),T.useEffect(x,N);const E=T.useRef(h);let C,_;n[3]!==h?(C=()=>{E.current=h},_=[h],n[3]=h,n[4]=C,n[5]=_):(C=n[4],_=n[5]),T.useEffect(C,_);let j;e:{if(!s){let ae;n[6]===Symbol.for("react.memo_cache_sentinel")?(ae={nodes:Gh,edges:Uh,transformError:null},n[6]=ae):ae=n[6],j=ae;break e}try{let ae;n[7]!==s?(ae=tv(s),n[7]=s,n[8]=ae):ae=n[8];const de=ae;let fe;n[9]!==de?(fe={...de,transformError:null},n[9]=de,n[10]=fe):fe=n[10],j=fe}catch(ae){const de=ae,fe=de instanceof Error?de.message:String(de);let ze;n[11]===Symbol.for("react.memo_cache_sentinel")?(ze={nodes:Gh,edges:Uh,transformError:fe},n[11]=ze):ze=n[11],j=ze}}const{nodes:k,edges:H,transformError:L}=j;let X,Y;n[12]!==L?(X=()=>{var ae;L&&((ae=E.current)==null||ae.call(E,`Graph render failed: ${L}`))},Y=[L],n[12]=L,n[13]=X,n[14]=Y):(X=n[13],Y=n[14]),T.useEffect(X,Y);let B;n[15]!==s?(B=s?JSON.stringify(s.nodes.map(uv)):"empty",n[15]=s,n[16]=B):B=n[16];const I=B,[V,K,$]=Ug(k),[te,ue,he]=Lg(H);let re,D;if(n[17]!==H||n[18]!==k||n[19]!==ue||n[20]!==K?(re=()=>{K(k),ue(H)},D=[k,H,K,ue],n[17]=H,n[18]=k,n[19]=ue,n[20]=K,n[21]=re,n[22]=D):(re=n[21],D=n[22]),T.useEffect(re,D),L){let ae,de;n[23]===Symbol.for("react.memo_cache_sentinel")?(ae=f.jsx("span",{className:an.emptyIcon,children:"⚠️"}),de=f.jsx("span",{children:"Graph could not be rendered."}),n[23]=ae,n[24]=de):(ae=n[23],de=n[24]);let fe;return n[25]!==L?(fe=f.jsxs("div",{className:an.empty,children:[ae,de,f.jsx("span",{children:L})]}),n[25]=L,n[26]=fe):fe=n[26],fe}if(!s||s.nodes.length===0){let ae,de;n[27]===Symbol.for("react.memo_cache_sentinel")?(ae=f.jsx("span",{className:an.emptyIcon,children:"🕸️"}),de=f.jsx("span",{children:"No graph data yet."}),n[27]=ae,n[28]=de):(ae=n[27],de=n[28]);let fe;n[29]===Symbol.for("react.memo_cache_sentinel")?(fe=f.jsx("strong",{children:"describe graph"}),n[29]=fe):fe=n[29];let ze;return n[30]===Symbol.for("react.memo_cache_sentinel")?(ze=f.jsxs("div",{className:an.empty,children:[ae,de,f.jsxs("span",{children:["Run ",fe," or ",f.jsx("strong",{children:"export graph"})," in the playground."]})]}),n[30]=ze):ze=n[30],ze}let O;n[31]!==s||n[32]!==p||n[33]!==u?(O=f.jsx(uf,{graphData:s,onCopySuccess:u,onCopyError:p}),n[31]=s,n[32]=p,n[33]=u,n[34]=O):O=n[34];let Q;n[35]===Symbol.for("react.memo_cache_sentinel")?(Q={padding:.25},n[35]=Q):Q=n[35];let P;n[36]===Symbol.for("react.memo_cache_sentinel")?(P={hideAttribution:!1},n[36]=P):P=n[36];let F;n[37]!==g?(F=(ae,de)=>{g&&(ae.preventDefault(),w({x:ae.clientX,y:ae.clientY,nodeAlias:de.data.alias}))},n[37]=g,n[38]=F):F=n[38];let ie,oe,ee;n[39]===Symbol.for("react.memo_cache_sentinel")?(ie=()=>w(null),oe=f.jsx(Yg,{variant:qg.Dots,gap:18,size:1,color:"rgba(255,255,255,0.07)"}),ee=f.jsx(Xg,{showInteractive:!1}),n[39]=ie,n[40]=oe,n[41]=ee):(ie=n[39],oe=n[40],ee=n[41]);let ye;n[42]===Symbol.for("react.memo_cache_sentinel")?(ye=f.jsx(Jg,{nodeColor:cv,maskColor:"rgba(0,0,0,0.3)",style:{background:"#fff"}}),n[42]=ye):ye=n[42];let se;n[43]!==te||n[44]!==V||n[45]!==he||n[46]!==$||n[47]!==F?(se=f.jsxs(Ig,{nodes:V,edges:te,onNodesChange:$,onEdgesChange:he,nodeTypes:kb,fitView:!0,fitViewOptions:Q,minZoom:.2,maxZoom:2.5,proOptions:P,onNodeContextMenu:F,onPaneClick:ie,children:[oe,ee,ye]}),n[43]=te,n[44]=V,n[45]=he,n[46]=$,n[47]=F,n[48]=se):se=n[48];let ge;n[49]!==m?(ge=m&&f.jsx("div",{className:an.refreshingOverlay,children:f.jsx("div",{className:an.refreshingSpinner,role:"status","aria-label":"Graph refreshing"})}),n[49]=m,n[50]=ge):ge=n[50];let je;n[51]!==y||n[52]!==s||n[53]!==g?(je=y&&g&&s&&f.jsx("div",{ref:v,className:an.contextMenu,style:{position:"fixed",top:y.y,left:y.x},role:"menu",children:f.jsx("button",{role:"menuitem",autoFocus:!0,className:an.contextMenuItem,onClick:()=>{const ae=nv(s,y.nodeAlias);if(ae){const de=av(s,y.nodeAlias);g(ae,de)}w(null)},children:"Clip to Clipboard"})}),n[51]=y,n[52]=s,n[53]=g,n[54]=je):je=n[54];let He;n[55]!==m||n[56]!==O||n[57]!==se||n[58]!==ge||n[59]!==je?(He=f.jsxs("div",{className:an.graphWrapper,"aria-busy":m,children:[O,se,ge,je]}),n[55]=m,n[56]=O,n[57]=se,n[58]=ge,n[59]=je,n[60]=He):He=n[60];let we;return n[61]!==I||n[62]!==h||n[63]!==He?(we=f.jsx(Yb,{onRenderError:h,children:He},I),n[61]=I,n[62]=h,n[63]=He,n[64]=we):we=n[64],we}function cv(r){return{Root:"#15803d",End:"#dc2626",Fetcher:"#2563eb",mapper:"#ea580c",Math:"#a16207",JavaScript:"#7e22ce",Provider:"#be185d",Dictionary:"#0e7490",Join:"#65a30d",Extension:"#4338ca",Island:"#475569",Decision:"#b45309"}[r.type??""]??"#6c7086"}function uv(r){return r.alias}const dv="_root_1yhjs_2",pv="_empty_1yhjs_10",hv="_emptyIcon_1yhjs_23",fv="_toolbarButton_1yhjs_29 _toolbarButton_10vsj_24",mv="_scrollBody_1yhjs_34",gv="_jsonContainer_1yhjs_45",yv="_jsonLabel_1yhjs_46",bv="_jsonString_1yhjs_47",vv="_jsonNumber_1yhjs_48",_v="_jsonBoolean_1yhjs_49",Sv="_jsonNull_1yhjs_50",qt={root:dv,empty:pv,emptyIcon:hv,toolbarButton:fv,scrollBody:mv,jsonContainer:gv,jsonLabel:yv,jsonString:bv,jsonNumber:vv,jsonBoolean:_v,jsonNull:Sv},xv=Qg,wv=Zg,Tv=r=>r<3,Ev={default:Tv,all:xv,none:wv};function Av(r){const n=xe.c(22),{graphData:s,onCopySuccess:u,onCopyError:p}=r,[h,b]=T.useState("all");if(!s){let L;return n[0]===Symbol.for("react.memo_cache_sentinel")?(L=f.jsx("div",{className:qt.root,children:f.jsxs("div",{className:qt.empty,children:[f.jsx("span",{className:qt.emptyIcon,children:"🕸️"}),f.jsx("span",{children:"No graph data yet."}),f.jsx("span",{children:"Pin a graph-link message in the Console to load the raw data here."})]})}),n[0]=L):L=n[0],L}let g;n[1]===Symbol.for("react.memo_cache_sentinel")?(g=()=>b("all"),n[1]=g):g=n[1];const m=h==="all";let y;n[2]!==m?(y=f.jsx("button",{className:qt.toolbarButton,onClick:g,title:"Expand all nodes","aria-label":"Expand all JSON nodes","aria-pressed":m,children:"➖"}),n[2]=m,n[3]=y):y=n[3];let w;n[4]===Symbol.for("react.memo_cache_sentinel")?(w=()=>b("none"),n[4]=w):w=n[4];const v=h==="none";let x;n[5]!==v?(x=f.jsx("button",{className:qt.toolbarButton,onClick:w,title:"Collapse all nodes","aria-label":"Collapse all JSON nodes","aria-pressed":v,children:"➕"}),n[5]=v,n[6]=x):x=n[6];let N;n[7]!==y||n[8]!==x?(N=f.jsxs(f.Fragment,{children:[y,x]}),n[7]=y,n[8]=x,n[9]=N):N=n[9];let E;n[10]!==s||n[11]!==p||n[12]!==u||n[13]!==N?(E=f.jsx(uf,{graphData:s,onCopySuccess:u,onCopyError:p,extraActions:N}),n[10]=s,n[11]=p,n[12]=u,n[13]=N,n[14]=E):E=n[14];const C=s,_=Ev[h];let j;n[15]===Symbol.for("react.memo_cache_sentinel")?(j={...uo,container:`${uo.container} ${qt.jsonContainer}`,label:qt.jsonLabel,stringValue:qt.jsonString,numberValue:qt.jsonNumber,booleanValue:qt.jsonBoolean,nullValue:qt.jsonNull},n[15]=j):j=n[15];let k;n[16]!==_||n[17]!==C?(k=f.jsx("div",{className:qt.scrollBody,children:f.jsx(Sc,{data:C,shouldExpandNode:_,style:j})}),n[16]=_,n[17]=C,n[18]=k):k=n[18];let H;return n[19]!==k||n[20]!==E?(H=f.jsxs("div",{className:qt.root,children:[E,k]}),n[19]=k,n[20]=E,n[21]=H):H=n[21],H}const Nv="_rightPanel_19vt8_2",Cv="_tabStrip_19vt8_10",jv="_tab_19vt8_10",Mv="_tabActive_19vt8_38",Dv="_tabBadge_19vt8_42",Ov="_tabBody_19vt8_48",kv="_tabBodyHidden_19vt8_57",Rv="_rightPanelGroup_19vt8_62",zv="_verticalResizeHandle_19vt8_70",_t={rightPanel:Nv,tabStrip:Cv,tab:jv,tabActive:Mv,tabBadge:Dv,tabBody:Ov,tabBodyHidden:kv,rightPanelGroup:Rv,verticalResizeHandle:zv},Lh="help-split-percent",ic="help-split-maximized",Bv=45,Hv=98;function Gv({tabs:r,payload:n,onChange:s,validation:u,onFormat:p,onUpload:h,graphData:b,activeTab:g,onTabChange:m,onGraphRenderError:y,onGraphDataCopySuccess:w,onGraphDataCopyError:v,isGraphRefreshing:x,onClipNode:N,helpPanel:E}){const C=T.useId(),_=`${C}-tab-payload`,j=`${C}-tab-graph`,k=`${C}-tab-graph-data`,H=f.jsxs("div",{className:_t.rightPanel,children:[f.jsxs("div",{className:_t.tabStrip,role:"tablist","aria-label":"Right panel tabs",children:[r.includes("payload")&&f.jsx("button",{role:"tab","aria-selected":g==="payload","aria-controls":_,className:`${_t.tab}${g==="payload"?` ${_t.tabActive}`:""}`,onClick:()=>m("payload"),children:"Payload Editor"}),r.includes("graph")&&f.jsxs("button",{role:"tab","aria-selected":g==="graph","aria-controls":j,className:`${_t.tab}${g==="graph"?` ${_t.tabActive}`:""}`,onClick:()=>m("graph"),children:["Graph",b!==null&&f.jsx("span",{className:_t.tabBadge,"aria-label":"Graph data available",children:"🕸️"})]}),r.includes("graph-data")&&f.jsx("button",{role:"tab","aria-selected":g==="graph-data","aria-controls":k,className:`${_t.tab}${g==="graph-data"?` ${_t.tabActive}`:""}`,onClick:()=>m("graph-data"),children:"Graph Data (Raw)"})]}),r.includes("payload")&&f.jsx("div",{role:"tabpanel",id:_,tabIndex:g==="payload"?0:-1,className:`${_t.tabBody}${g!=="payload"?` ${_t.tabBodyHidden}`:""}`,children:f.jsx(yb,{payload:n,onChange:s,validation:u,onFormat:p,onUpload:h})}),r.includes("graph")&&f.jsx("div",{role:"tabpanel",id:j,tabIndex:g==="graph"?0:-1,className:`${_t.tabBody}${g!=="graph"?` ${_t.tabBodyHidden}`:""}`,children:f.jsx(rv,{graphData:b,onRenderError:y,isRefreshing:x,onCopySuccess:w,onCopyError:v,onClipNode:N})}),r.includes("graph-data")&&f.jsx("div",{role:"tabpanel",id:k,tabIndex:g==="graph-data"?0:-1,className:`${_t.tabBody}${g!=="graph-data"?` ${_t.tabBodyHidden}`:""}`,children:f.jsx(Av,{graphData:b,onCopySuccess:w,onCopyError:v})})]}),L=T.useRef(Number(sessionStorage.getItem(Lh))||Bv),X=T.useRef(null),Y=T.useRef(null),[B,I]=T.useState(()=>sessionStorage.getItem(ic)==="1"),V=T.useRef(B),K=T.useCallback(O=>{const Q=O["help-split-help"];if(Q===void 0)return;const P=Q>=Hv;P!==V.current&&(V.current=P,I(P),sessionStorage.setItem(ic,P?"1":"0")),P||(L.current=Q,sessionStorage.setItem(Lh,String(Q)))},[]),$=T.useCallback(()=>{var Q,P,F,ie;const O=!V.current;if(V.current=O,I(O),sessionStorage.setItem(ic,O?"1":"0"),O)(Q=Y.current)==null||Q.resize("0%"),(P=X.current)==null||P.resize("100%");else{const oe=L.current;(F=X.current)==null||F.resize(`${oe}%`),(ie=Y.current)==null||ie.resize(`${100-oe}%`)}},[]),te=!!E;if(T.useEffect(()=>{te&&V.current&&requestAnimationFrame(()=>{var O,Q;(O=Y.current)==null||O.resize("0%"),(Q=X.current)==null||Q.resize("100%")})},[te]),!E)return H;const ue=typeof E=="function"?E($,B):E,re=V.current?100:L.current,D=100-re;return f.jsxs(Wh,{orientation:"vertical",className:_t.rightPanelGroup,onLayoutChanged:K,children:[f.jsx(co,{panelRef:Y,defaultSize:`${D}%`,minSize:"0%",children:H}),f.jsx(cc,{className:_t.verticalResizeHandle,"aria-label":"Resize help panel"}),f.jsx(co,{id:"help-split-help",panelRef:X,defaultSize:`${re}%`,minSize:"15%",children:ue})]})}class Uv extends Fh.Component{constructor(){super(...arguments),this.state={hasError:!1}}static getDerivedStateFromError(){return{hasError:!0}}componentDidCatch(n,s){console.error("[ConsoleErrorBoundary] Failed to render message:",n,s.componentStack)}render(){return this.state.hasError?f.jsx("span",{children:this.props.fallback}):this.props.children}}const Lv=2e3,Yv=(r={})=>{const{onSuccess:n,onError:s}=r,[u,p]=T.useState(!1),h=T.useRef(null);return T.useEffect(()=>()=>{h.current!==null&&clearTimeout(h.current)},[]),{copy:T.useCallback(async g=>{if(!navigator.clipboard)return console.warn("useCopyToClipboard: Clipboard API not available in this browser."),s==null||s(),!1;try{return await navigator.clipboard.writeText(g),p(!0),h.current!==null&&clearTimeout(h.current),h.current=setTimeout(()=>{h.current=null,p(!1)},Lv),n==null||n(),!0}catch(m){return console.error("useCopyToClipboard: Failed to write to clipboard.",m),s==null||s(),!1}},[n,s]),copied:u}},qv="_consoleRoot_1lgp1_2",Xv="_consoleHeader_1lgp1_10",Jv="_consoleTitle_1lgp1_20",Iv="_consoleControls_1lgp1_25",Zv="_controlButton_1lgp1_30",Qv="_console_1lgp1_2",Vv="_emptyConsole_1lgp1_67",Kv="_consoleMessage_1lgp1_80",$v="_consoleMessageActivatable_1lgp1_94",Wv="_consoleMessageGraphLink_1lgp1_104",Pv="_consoleMessageLargePayload_1lgp1_115",Fv="_consoleMessageMockUpload_1lgp1_122",e2="_uploadMockButton_1lgp1_131",t2="_copyButton_1lgp1_172",n2="_copyButtonCopied_1lgp1_225",a2="_sendToJsonPathButton_1lgp1_234",l2="_messageIcon_1lgp1_268",o2="_messageContent_1lgp1_272",i2="_messageText_1lgp1_278",s2="_messageTime_1lgp1_283",r2="_jsonViewWrapper_1lgp1_295",c2="_jsonContainer_1lgp1_301",u2="_jsonLabel_1lgp1_302",d2="_jsonString_1lgp1_303",p2="_jsonNumber_1lgp1_304",h2="_jsonBoolean_1lgp1_305",f2="_jsonNull_1lgp1_306",Ye={consoleRoot:qv,consoleHeader:Xv,consoleTitle:Jv,consoleControls:Iv,controlButton:Zv,console:Qv,emptyConsole:Vv,consoleMessage:Kv,consoleMessageActivatable:$v,consoleMessageGraphLink:Wv,consoleMessageLargePayload:Pv,consoleMessageMockUpload:Fv,uploadMockButton:e2,copyButton:t2,copyButtonCopied:n2,sendToJsonPathButton:a2,messageIcon:l2,messageContent:o2,messageText:i2,messageTime:s2,"messageType-error":"_messageType-error_1lgp1_290","messageType-info":"_messageType-info_1lgp1_291","messageType-welcome":"_messageType-welcome_1lgp1_292",jsonViewWrapper:r2,jsonContainer:c2,jsonLabel:u2,jsonString:d2,jsonNumber:p2,jsonBoolean:h2,jsonNull:f2};function m2(r){var sa;const n=xe.c(77),{message:s,msgId:u,classificationMap:p,onGraphLink:h,onCopyMessage:b,onSendToJsonPath:g,onUploadMockData:m,successfulUploadPaths:y}=r;let w,v,x;n[0]!==s?(v=Ey(s),w=Ay(v.type),x=po(v.message),n[0]=s,n[1]=w,n[2]=v,n[3]=x):(w=n[1],v=n[2],x=n[3]);const N=x;let E,C,_,j,k,H;if(n[4]!==p||n[5]!==u||n[6]!==m||n[7]!==y){const ve=(u!==void 0?p==null?void 0:p.get(u):void 0)??[];C=ve.some(_2),_=ve.some(v2),j=ve.some(b2),k=((sa=ve.find(y2))==null?void 0:sa.uploadPath)??null,E=!!m&&j&&k!==null,H=E&&!!(y!=null&&y.has(k)),n[4]=p,n[5]=u,n[6]=m,n[7]=y,n[8]=E,n[9]=C,n[10]=_,n[11]=j,n[12]=k,n[13]=H}else E=n[8],C=n[9],_=n[10],j=n[11],k=n[12],H=n[13];const L=H,X=!!h&&C&&!j&&!_,Y=!!g&&N.isJSON;let B;n[14]!==b?(B={onSuccess:b},n[14]=b,n[15]=B):B=n[15];const{copy:I,copied:V}=Yv(B);let K;n[16]!==I||n[17]!==s?(K=ve=>{ve.stopPropagation(),I(s)},n[16]=I,n[17]=s,n[18]=K):K=n[18];const $=K;let te;n[19]!==I||n[20]!==s?(te=ve=>{(ve.key==="Enter"||ve.key===" ")&&(ve.preventDefault(),ve.stopPropagation(),I(s))},n[19]=I,n[20]=s,n[21]=te):te=n[21];const ue=te;let he;n[22]!==N.data||n[23]!==N.isJSON||n[24]!==g?(he=ve=>{if(ve.stopPropagation(),!g||!N.isJSON)return;const hn=JSON.stringify(N.data,null,2);g(hn)},n[22]=N.data,n[23]=N.isJSON,n[24]=g,n[25]=he):he=n[25];const re=he;let D;n[26]!==k||n[27]!==m?(D=ve=>{ve.stopPropagation(),!(!m||!k)&&m(k)},n[26]=k,n[27]=m,n[28]=D):D=n[28];const O=D,Q=Ye[`messageType-${v.type}`],P=X?Ye.consoleMessageActivatable:"",F=C?Ye.consoleMessageGraphLink:"",ie=_?Ye.consoleMessageLargePayload:"",oe=j?Ye.consoleMessageMockUpload:"";let ee;n[29]!==F||n[30]!==ie||n[31]!==oe||n[32]!==Q||n[33]!==P?(ee=[Ye.consoleMessage,Q,P,F,ie,oe].filter(Boolean),n[29]=F,n[30]=ie,n[31]=oe,n[32]=Q,n[33]=P,n[34]=ee):ee=n[34];const ye=ee.join(" ");let se;n[35]!==X||n[36]!==h?(se=X?()=>h():void 0,n[35]=X,n[36]=h,n[37]=se):se=n[37];const ge=X?"Click to load graph in Graph View":void 0,je=X?"button":void 0,He=X?0:void 0;let we;n[38]!==X||n[39]!==h?(we=X?ve=>{(ve.key==="Enter"||ve.key===" ")&&(ve.preventDefault(),h())}:void 0,n[38]=X,n[39]=h,n[40]=we):we=n[40];const ae=X?"Load graph in Graph View":void 0,de=j?"⬆️":_?"⬇️":C?"🕸️":w;let fe;n[41]!==de?(fe=f.jsx("span",{className:Ye.messageIcon,children:de}),n[41]=de,n[42]=fe):fe=n[42];let ze;n[43]!==N.data||n[44]!==N.isJSON||n[45]!==v.message||n[46]!==L?(ze=f.jsx("div",{className:Ye.messageContent,children:N.isJSON?f.jsx("div",{className:Ye.jsonViewWrapper,children:f.jsx(Sc,{data:N.data,shouldExpandNode:g2,style:{...uo,container:`${uo.container} ${Ye.jsonContainer}`,label:Ye.jsonLabel,stringValue:Ye.jsonString,numberValue:Ye.jsonNumber,booleanValue:Ye.jsonBoolean,nullValue:Ye.jsonNull}})}):f.jsxs("span",{className:Ye.messageText,children:[v.message,L&&f.jsx("span",{title:"Upload succeeded",children:" ✅"})]})}),n[43]=N.data,n[44]=N.isJSON,n[45]=v.message,n[46]=L,n[47]=ze):ze=n[47];const W=`${Ye.copyButton} ${V?Ye.copyButtonCopied:""}`,be=V?"Copied!":"Copy message",Te=V?"Copied to clipboard":"Copy message to clipboard",me=V?"✅":"📄";let Oe;n[48]!==$||n[49]!==ue||n[50]!==W||n[51]!==be||n[52]!==Te||n[53]!==me?(Oe=f.jsx("button",{className:W,onClick:$,onKeyDown:ue,title:be,"aria-label":Te,tabIndex:0,children:me}),n[48]=$,n[49]=ue,n[50]=W,n[51]=be,n[52]=Te,n[53]=me,n[54]=Oe):Oe=n[54];let Ge;n[55]!==Y||n[56]!==re?(Ge=Y&&f.jsx("button",{className:Ye.sendToJsonPathButton,onClick:re,onKeyDown:ve=>{(ve.key==="Enter"||ve.key===" ")&&re(ve)},title:"Open in JSON-Path Playground","aria-label":"Open this JSON in the JSON-Path Playground",tabIndex:0,children:"➡️"}),n[55]=Y,n[56]=re,n[57]=Ge):Ge=n[57];let dt;n[58]!==E||n[59]!==O?(dt=E&&f.jsx("button",{className:Ye.uploadMockButton,onClick:O,onKeyDown:ve=>{(ve.key==="Enter"||ve.key===" ")&&O(ve)},title:"Re-open upload dialog","aria-label":"Re-open mock data upload dialog",tabIndex:0,children:"⬆️ Upload JSON…"}),n[58]=E,n[59]=O,n[60]=dt):dt=n[60];let Fe;n[61]!==v.time?(Fe=v.time&&f.jsx("span",{className:Ye.messageTime,children:v.time}),n[61]=v.time,n[62]=Fe):Fe=n[62];let ct;return n[63]!==ye||n[64]!==se||n[65]!==ge||n[66]!==je||n[67]!==He||n[68]!==we||n[69]!==ae||n[70]!==fe||n[71]!==ze||n[72]!==Oe||n[73]!==Ge||n[74]!==dt||n[75]!==Fe?(ct=f.jsxs("div",{className:ye,onClick:se,title:ge,role:je,tabIndex:He,onKeyDown:we,"aria-label":ae,children:[fe,ze,Oe,Ge,dt,Fe]}),n[63]=ye,n[64]=se,n[65]=ge,n[66]=je,n[67]=He,n[68]=we,n[69]=ae,n[70]=fe,n[71]=ze,n[72]=Oe,n[73]=Ge,n[74]=dt,n[75]=Fe,n[76]=ct):ct=n[76],ct}function g2(r){return r<1}function y2(r){return r.kind==="upload.invitation"}function b2(r){return r.kind==="upload.invitation"}function v2(r){return r.kind==="payload.large"}function _2(r){return r.kind==="graph.link"}function S2(r){const n=xe.c(32),{messages:s,classificationMap:u,onCopy:p,onClear:h,consoleRef:b,onGraphLinkMessage:g,onCopyMessage:m,onSendToJsonPath:y,onUploadMockData:w,successfulUploadPaths:v}=r;let x;n[0]===Symbol.for("react.memo_cache_sentinel")?(x=f.jsx("span",{className:Ye.consoleTitle,children:"Console Output"}),n[0]=x):x=n[0];let N;n[1]!==p?(N=f.jsx("button",{className:Ye.controlButton,onClick:p,title:"Copy console output","aria-label":"Copy console output to clipboard",children:"📑"}),n[1]=p,n[2]=N):N=n[2];let E;n[3]!==h?(E=f.jsx("button",{className:Ye.controlButton,onClick:h,title:"Clear console","aria-label":"Clear console",children:"🗑️"}),n[3]=h,n[4]=E):E=n[4];let C;n[5]!==N||n[6]!==E?(C=f.jsxs("div",{className:Ye.consoleHeader,children:[x,f.jsxs("div",{className:Ye.consoleControls,children:[N,E]})]}),n[5]=N,n[6]=E,n[7]=C):C=n[7];let _;if(n[8]!==u||n[9]!==s||n[10]!==m||n[11]!==g||n[12]!==y||n[13]!==w||n[14]!==v){let L;n[16]!==u||n[17]!==m||n[18]!==g||n[19]!==y||n[20]!==w||n[21]!==v?(L=X=>f.jsx(Uv,{fallback:X.raw,children:f.jsx(m2,{message:X.raw,msgId:X.id,classificationMap:u,onGraphLink:g?()=>g(X):void 0,onCopyMessage:m,onSendToJsonPath:y,onUploadMockData:w,successfulUploadPaths:v})},X.id),n[16]=u,n[17]=m,n[18]=g,n[19]=y,n[20]=w,n[21]=v,n[22]=L):L=n[22],_=s.map(L),n[8]=u,n[9]=s,n[10]=m,n[11]=g,n[12]=y,n[13]=w,n[14]=v,n[15]=_}else _=n[15];let j;n[23]!==s.length?(j=s.length===0&&f.jsxs("div",{className:Ye.emptyConsole,children:["No messages yet. Use the ",f.jsx("strong",{children:"Start"})," button in the header to connect."]}),n[23]=s.length,n[24]=j):j=n[24];let k;n[25]!==b||n[26]!==_||n[27]!==j?(k=f.jsxs("div",{className:Ye.console,ref:b,role:"log","aria-live":"polite",children:[_,j]}),n[25]=b,n[26]=_,n[27]=j,n[28]=k):k=n[28];let H;return n[29]!==C||n[30]!==k?(H=f.jsxs("div",{className:Ye.consoleRoot,children:[C,k]}),n[29]=C,n[30]=k,n[31]=H):H=n[31],H}const x2="_commandInput_j85f1_2",w2="_labelRow_j85f1_8",T2="_labelGroup_j85f1_16",E2="_label_j85f1_8",A2="_infoWrapper_j85f1_28",N2="_paletteToggle_j85f1_34",C2="_paletteToggleActive_j85f1_66",j2="_popover_j85f1_73",M2="_popoverOpen_j85f1_95",D2="_popoverTitle_j85f1_121",O2="_popoverRow_j85f1_135",k2="_popoverKeyword_j85f1_156",R2="_popoverDesc_j85f1_168",z2="_popoverAlias_j85f1_174",B2="_inputRow_j85f1_181",H2="_inputWrapper_j85f1_187",G2="_textarea_j85f1_197",U2="_sendButton_j85f1_226",L2="_hint_j85f1_243",Y2="_dropup_j85f1_251",q2="_dropupHeader_j85f1_266",X2="_dropupItem_j85f1_282",J2="_dropupItemText_j85f1_305",I2="_matchHighlight_j85f1_313",Z2="_multilineIndicator_j85f1_319",Ke={commandInput:x2,labelRow:w2,labelGroup:T2,label:E2,infoWrapper:A2,paletteToggle:N2,paletteToggleActive:C2,popover:j2,popoverOpen:M2,popoverTitle:D2,popoverRow:O2,popoverKeyword:k2,popoverDesc:R2,popoverAlias:z2,inputRow:B2,inputWrapper:H2,textarea:G2,sendButton:U2,hint:L2,dropup:Y2,dropupHeader:q2,dropupItem:X2,dropupItemText:J2,matchHighlight:I2,multilineIndicator:Z2},Q2=[{keyword:"help",description:"List all help topics, or get help for a specific command",template:"help"},{keyword:"create",description:"Create a new graph node",template:`create node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:"update",description:"Update an existing node",template:`update node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:"edit",description:"Print raw node data ready for editing and re-submitting",template:"edit node {name}"},{keyword:"delete node",description:"Delete a node by name",alias:"clear node",template:"delete node {name}"},{keyword:"delete connection",description:"Delete connection(s) between two nodes",alias:"clear connection",template:"delete connection {nodeA} and {nodeB}"},{keyword:"delete cache",description:"Clear cached API fetcher results",alias:"clear cache",template:"delete cache"},{keyword:"connect",description:"Connect two nodes with a named relation",template:"connect {node-A} to {node-B} with {relation}"},{keyword:"list nodes",description:"List all nodes in the current graph",template:"list nodes"},{keyword:"list connections",description:"List all connections in the current graph",template:"list connections"},{keyword:"describe graph",description:"Describe the current graph model",template:"describe graph"},{keyword:"describe node",description:"Describe a specific node and its connections",template:"describe node {name}"},{keyword:"describe connection",description:"Describe connection(s) between two nodes",template:"describe connection {nodeA} and {nodeB}"},{keyword:"describe skill",description:"Show documentation for a skill by route name",template:"describe skill {skill.route}"},{keyword:"export",description:"Export the graph model to a JSON file",template:"export graph as {name}"},{keyword:"import graph",description:"Import a graph model from a saved file",template:"import graph from {name}"},{keyword:"import node",description:"Import a single node from another saved graph",template:"import node {node-name} from {graph-name}"},{keyword:"instantiate",description:"Create a runnable graph instance with mock input",alias:"start",template:`instantiate graph
{constant} -> input.body.{key}`,multiline:!0},{keyword:"upload mock data",description:"Print the URL to POST a JSON payload as mock input.body",template:"upload mock data"},{keyword:"execute",description:"Execute a single node skill in isolation",template:"execute node {name}"},{keyword:"inspect",description:"Inspect a state-machine variable",template:"inspect {variable_name}"},{keyword:"run",description:"Run the graph instance from root to end",template:"run"}];function V2(r,n){const s=xe.c(22),[u,p]=T.useState(!1),[h,b]=T.useState(-1);let g;if(s[0]!==n||s[1]!==r){e:{const L=n.trimStart();if(L.length===0){let V;s[3]===Symbol.for("react.memo_cache_sentinel")?(V=[],s[3]=V):V=s[3],g=V;break e}const X=L.toLowerCase(),Y=r.filter(V=>V.toLowerCase().startsWith(X)),B=new Set;g=Y.filter(V=>B.has(V)?!1:(B.add(V),!0)).slice(0,Sy)}s[0]=n,s[1]=r,s[2]=g}else g=s[2];const m=g;let y;s[4]===Symbol.for("react.memo_cache_sentinel")?(y=()=>{p(!0),b(-1)},s[4]=y):y=s[4];const w=y;let v;s[5]!==m?(v=L=>{const X=m.length;X!==0&&b(Y=>L===1?Y<0?0:(Y+1)%X:Y<=0?X-1:Y-1)},s[5]=m,s[6]=v):v=s[6];const x=v;let N;s[7]!==m?(N=(L,X)=>{L>=0&&L<m.length&&X(m[L]),p(!1),b(-1)},s[7]=m,s[8]=N):N=s[8];const E=N;let C;s[9]!==E||s[10]!==h||s[11]!==u||s[12]!==m?(C=L=>{if(!u||m.length===0)return;const X=h>=0?h:0;E(X,L)},s[9]=E,s[10]=h,s[11]=u,s[12]=m,s[13]=C):C=s[13];const _=C;let j;s[14]===Symbol.for("react.memo_cache_sentinel")?(j=()=>{p(!1),b(-1)},s[14]=j):j=s[14];const k=j;let H;return s[15]!==E||s[16]!==h||s[17]!==u||s[18]!==x||s[19]!==_||s[20]!==m?(H={suggestions:m,isOpen:u,activeIndex:h,onCommandChange:w,navigate:x,accept:E,onTab:_,dismiss:k},s[15]=E,s[16]=h,s[17]=u,s[18]=x,s[19]=_,s[20]=m,s[21]=H):H=s[21],H}const K2=r=>T.createElement("svg",{xmlns:"http://www.w3.org/2000/svg",viewBox:"0 0 16 16",fill:"none",width:14,height:14,stroke:"currentColor",strokeWidth:1.5,strokeLinecap:"round",strokeLinejoin:"round",...r},T.createElement("polyline",{points:"2,4 6,8 2,12"}),T.createElement("line",{x1:7,y1:12,x2:14,y2:12}));function $2(r){const n=xe.c(70),{command:s,onChange:u,onKeyDown:p,onSend:h,sendDisabled:b,disabled:g,history:m}=r,y=T.useRef(null),w=T.useRef(null),v=T.useRef(null),[x,N]=T.useState(!1);let E,C;n[0]!==x?(E=()=>{if(!x)return;const W=be=>{w.current&&!w.current.contains(be.target)&&N(!1)};return document.addEventListener("mousedown",W),()=>document.removeEventListener("mousedown",W)},C=[x],n[0]=x,n[1]=E,n[2]=C):(E=n[1],C=n[2]),T.useEffect(E,C);const _=V2(m,s);let j;n[3]===Symbol.for("react.memo_cache_sentinel")?(j=()=>{const W=y.current;W&&(W.style.height="auto",W.style.height=`${W.scrollHeight}px`)},n[3]=j):j=n[3];let k;n[4]!==s?(k=[s],n[4]=s,n[5]=k):k=n[5],T.useEffect(j,k);const H=g?"Not connected":"Enter command (Enter to send · Shift+Enter for new line)",L=g?"Enter your test message once it is connected":"Enter to send · Shift+Enter for new line · ↑↓ for history";let X;n[6]!==_||n[7]!==u||n[8]!==p||n[9]!==h?(X=W=>{var be,Te;if(W.key==="Tab"){W.preventDefault(),_.isOpen&&_.suggestions.length>0&&(_.onTab(me=>u(me)),requestAnimationFrame(()=>{const me=y.current;me&&(me.selectionStart=me.selectionEnd=me.value.length)}));return}if(W.key==="Enter"){if(W.shiftKey)return;if(W.preventDefault(),_.isOpen&&_.activeIndex>=0){_.accept(_.activeIndex,me=>u(me)),requestAnimationFrame(()=>{const me=y.current;me&&(me.selectionStart=me.selectionEnd=me.value.length)}),(be=y.current)==null||be.focus();return}h(),(Te=y.current)==null||Te.focus();return}if(W.key==="Escape"){if(_.isOpen){_.dismiss(),W.preventDefault();return}return}if(W.key==="ArrowUp"||W.key==="ArrowDown"){if(_.isOpen&&_.suggestions.length>0){W.preventDefault(),_.navigate(W.key==="ArrowDown"?1:-1);return}const me=y.current;if(me){const{selectionStart:Oe,value:Ge}=me,Fe=!Ge.slice(0,Oe).includes(`
`),ct=!Ge.slice(Oe).includes(`
`);if(!(W.key==="ArrowUp"&&Fe||W.key==="ArrowDown"&&ct))return}p(W),requestAnimationFrame(()=>{const Oe=y.current;Oe&&(Oe.selectionStart=Oe.selectionEnd=Oe.value.length)});return}p(W)},n[6]=_,n[7]=u,n[8]=p,n[9]=h,n[10]=X):X=n[10];const Y=X;let B;n[11]===Symbol.for("react.memo_cache_sentinel")?(B=f.jsx("label",{htmlFor:"command",className:Ke.label,children:"Command"}),n[11]=B):B=n[11];const I=`${Ke.paletteToggle}${x?` ${Ke.paletteToggleActive}`:""}`;let V;n[12]===Symbol.for("react.memo_cache_sentinel")?(V=()=>N(F2),n[12]=V):V=n[12];let K;n[13]!==x?(K=W=>{var be;if(W.key==="ArrowDown"&&x){W.preventDefault();const Te=(be=v.current)==null?void 0:be.querySelector('[role="option"]');Te==null||Te.focus()}},n[13]=x,n[14]=K):K=n[14];let $;n[15]===Symbol.for("react.memo_cache_sentinel")?($=f.jsx(K2,{"aria-hidden":"true",focusable:"false"}),n[15]=$):$=n[15];let te;n[16]!==x||n[17]!==I||n[18]!==K?(te=f.jsx("button",{type:"button",className:I,"aria-label":"Toggle command palette","aria-expanded":x,"aria-controls":"command-palette",onClick:V,onKeyDown:K,title:"Command palette",children:$}),n[16]=x,n[17]=I,n[18]=K,n[19]=te):te=n[19];const ue=`${Ke.popover}${x?` ${Ke.popoverOpen}`:""}`;let he,re;n[20]===Symbol.for("react.memo_cache_sentinel")?(he=W=>{var be,Te;if(W.key==="ArrowDown"||W.key==="ArrowUp"){W.preventDefault();const me=(be=v.current)==null?void 0:be.querySelectorAll('[role="option"]');if(!me||me.length===0)return;const Oe=Array.from(me).indexOf(document.activeElement);W.key==="ArrowDown"?me[Oe<0?0:(Oe+1)%me.length].focus():me[Oe<=0?me.length-1:Oe-1].focus()}else W.key==="Escape"&&(W.preventDefault(),N(!1),(Te=y.current)==null||Te.focus())},re=f.jsx("p",{className:Ke.popoverTitle,children:"Command palette — click to insert"}),n[20]=he,n[21]=re):(he=n[20],re=n[21]);let D;n[22]!==x||n[23]!==u?(D=Q2.map(W=>{const{keyword:be,alias:Te,description:me,template:Oe}=W;return f.jsxs("div",{className:Ke.popoverRow,role:"option","aria-selected":!1,tabIndex:x?0:-1,onMouseDown:P2,onClick:()=>{var Ge;u(Oe),N(!1),(Ge=y.current)==null||Ge.focus()},onKeyDown:Ge=>{var dt;(Ge.key==="Enter"||Ge.key===" ")&&(Ge.preventDefault(),u(Oe),N(!1),(dt=y.current)==null||dt.focus())},children:[f.jsx("span",{className:Ke.popoverKeyword,children:be}),f.jsxs("span",{className:Ke.popoverDesc,children:[me,Te&&f.jsxs("span",{className:Ke.popoverAlias,children:[" · alias: ",Te]})]})]},be)}),n[22]=x,n[23]=u,n[24]=D):D=n[24];let O;n[25]!==ue||n[26]!==D?(O=f.jsxs("div",{id:"command-palette",ref:v,className:ue,role:"listbox","aria-label":"Command palette",onKeyDown:he,children:[re,D]}),n[25]=ue,n[26]=D,n[27]=O):O=n[27];let Q;n[28]!==te||n[29]!==O?(Q=f.jsx("div",{className:Ke.labelRow,children:f.jsxs("div",{className:Ke.labelGroup,children:[B,f.jsxs("span",{ref:w,className:Ke.infoWrapper,children:[te,O]})]})}),n[28]=te,n[29]=O,n[30]=Q):Q=n[30];const P=!(_.isOpen&&_.suggestions.length>0);let F;n[31]===Symbol.for("react.memo_cache_sentinel")?(F=f.jsx("div",{className:Ke.dropupHeader,"aria-hidden":"true",children:"Recent Commands"}),n[31]=F):F=n[31];let ie;n[32]!==_||n[33]!==s||n[34]!==u?(ie=_.isOpen&&_.suggestions.length>0&&_.suggestions.map((W,be)=>{const Te=W.split(`
`)[0],me=W.includes(`
`),Oe=s.trimStart().split(`
`)[0],Ge=Math.min(Oe.length,Te.length),dt=Te.slice(0,Ge),Fe=Te.slice(Ge);return f.jsxs("div",{id:`history-option-${be}`,role:"option","aria-selected":be===_.activeIndex,className:Ke.dropupItem,onMouseDown:W2,onClick:()=>{_.accept(be,ct=>u(ct)),requestAnimationFrame(()=>{const ct=y.current;ct&&(ct.selectionStart=ct.selectionEnd=ct.value.length)})},children:[f.jsxs("span",{className:Ke.dropupItemText,children:[Ge>0&&f.jsx("strong",{className:Ke.matchHighlight,children:dt}),Fe,me?"…":""]}),me&&f.jsx("span",{className:Ke.multilineIndicator,"aria-label":"multi-line command",children:"↵"})]},W)}),n[32]=_,n[33]=s,n[34]=u,n[35]=ie):ie=n[35];let oe;n[36]!==P||n[37]!==ie?(oe=f.jsxs("div",{id:"history-dropup",role:"listbox","aria-label":"Command history suggestions",className:Ke.dropup,hidden:P,children:[F,ie]}),n[36]=P,n[37]=ie,n[38]=oe):oe=n[38];const ee=_.isOpen&&_.suggestions.length>0,ye=_.isOpen&&_.suggestions.length>0&&_.activeIndex>=0?`history-option-${_.activeIndex}`:void 0;let se;n[39]!==_||n[40]!==u?(se=W=>{u(W.target.value),_.onCommandChange()},n[39]=_,n[40]=u,n[41]=se):se=n[41];let ge;n[42]!==_?(ge=()=>_.dismiss(),n[42]=_,n[43]=ge):ge=n[43];let je;n[44]!==s||n[45]!==g||n[46]!==Y||n[47]!==H||n[48]!==ee||n[49]!==ye||n[50]!==se||n[51]!==ge?(je=f.jsx("textarea",{ref:y,id:"command",role:"combobox","aria-expanded":ee,"aria-haspopup":"listbox","aria-controls":"history-dropup","aria-activedescendant":ye,"aria-autocomplete":"list",className:Ke.textarea,rows:1,placeholder:H,value:s,disabled:g,onChange:se,onKeyDown:Y,onBlur:ge,autoComplete:"off",autoCorrect:"off",spellCheck:!1}),n[44]=s,n[45]=g,n[46]=Y,n[47]=H,n[48]=ee,n[49]=ye,n[50]=se,n[51]=ge,n[52]=je):je=n[52];let He;n[53]!==oe||n[54]!==je?(He=f.jsxs("div",{className:Ke.inputWrapper,children:[oe,je]}),n[53]=oe,n[54]=je,n[55]=He):He=n[55];let we;n[56]!==h?(we=()=>{var W;h(),(W=y.current)==null||W.focus()},n[56]=h,n[57]=we):we=n[57];let ae;n[58]!==b||n[59]!==we?(ae=f.jsx("button",{className:Ke.sendButton,onClick:we,disabled:b,"aria-label":"Send command",children:"Send"}),n[58]=b,n[59]=we,n[60]=ae):ae=n[60];let de;n[61]!==He||n[62]!==ae?(de=f.jsxs("div",{className:Ke.inputRow,children:[He,ae]}),n[61]=He,n[62]=ae,n[63]=de):de=n[63];let fe;n[64]!==L?(fe=f.jsx("p",{className:Ke.hint,children:L}),n[64]=L,n[65]=fe):fe=n[65];let ze;return n[66]!==Q||n[67]!==de||n[68]!==fe?(ze=f.jsxs("div",{className:Ke.commandInput,children:[Q,de,fe]}),n[66]=Q,n[67]=de,n[68]=fe,n[69]=ze):ze=n[69],ze}function W2(r){return r.preventDefault()}function P2(r){return r.preventDefault()}function F2(r){return!r}const e_="_root_1ac49_1",t_={root:e_};function n_(r){const n=xe.c(22),{messages:s,classificationMap:u,onCopy:p,onClear:h,consoleRef:b,onGraphLinkMessage:g,onCopyMessage:m,onSendToJsonPath:y,onUploadMockData:w,successfulUploadPaths:v,command:x,onCommandChange:N,onCommandKeyDown:E,onSend:C,sendDisabled:_,inputDisabled:j,commandHistory:k}=r;let H;n[0]!==u||n[1]!==b||n[2]!==s||n[3]!==h||n[4]!==p||n[5]!==m||n[6]!==g||n[7]!==y||n[8]!==w||n[9]!==v?(H=f.jsx(S2,{messages:s,classificationMap:u,onCopy:p,onClear:h,consoleRef:b,onGraphLinkMessage:g,onCopyMessage:m,onSendToJsonPath:y,onUploadMockData:w,successfulUploadPaths:v}),n[0]=u,n[1]=b,n[2]=s,n[3]=h,n[4]=p,n[5]=m,n[6]=g,n[7]=y,n[8]=w,n[9]=v,n[10]=H):H=n[10];let L;n[11]!==x||n[12]!==k||n[13]!==j||n[14]!==N||n[15]!==E||n[16]!==C||n[17]!==_?(L=f.jsx($2,{command:x,onChange:N,onKeyDown:E,onSend:C,disabled:j,sendDisabled:_,history:k}),n[11]=x,n[12]=k,n[13]=j,n[14]=N,n[15]=E,n[16]=C,n[17]=_,n[18]=L):L=n[18];let X;return n[19]!==H||n[20]!==L?(X=f.jsxs("div",{className:t_.root,children:[H,L]}),n[19]=H,n[20]=L,n[21]=X):X=n[21],X}const a_="_dialog_g80bk_4",l_="_modalInner_g80bk_26",o_="_modalHeader_g80bk_34",i_="_modalTitleGroup_g80bk_44",s_="_modalTitle_g80bk_44",r_="_modalPath_g80bk_57",c_="_closeButton_g80bk_64",u_="_modalBody_g80bk_95",d_="_dropZone_g80bk_105",p_="_dropZoneActive_g80bk_127",h_="_dropZoneIcon_g80bk_133",f_="_dropZoneText_g80bk_139",m_="_dropZoneOr_g80bk_152",g_="_browseButton_g80bk_159",y_="_fileInputHidden_g80bk_188",b_="_fileError_g80bk_193",v_="_textareaLabel_g80bk_198",__="_textarea_g80bk_198",S_="_validationError_g80bk_226",x_="_keyboardHint_g80bk_231",w_="_errorBanner_g80bk_236",T_="_modalFooter_g80bk_247",E_="_footerActions_g80bk_257",A_="_formatButton_g80bk_263",N_="_cancelButton_g80bk_264",C_="_uploadButton_g80bk_265",j_="_spinner_g80bk_332",Ze={dialog:a_,modalInner:l_,modalHeader:o_,modalTitleGroup:i_,modalTitle:s_,modalPath:r_,closeButton:c_,modalBody:u_,dropZone:d_,dropZoneActive:p_,dropZoneIcon:h_,dropZoneText:f_,dropZoneOr:m_,browseButton:g_,fileInputHidden:y_,fileError:b_,textareaLabel:v_,textarea:__,validationError:S_,keyboardHint:x_,errorBanner:w_,modalFooter:T_,footerActions:E_,formatButton:A_,cancelButton:N_,uploadButton:C_,spinner:j_};function M_(r){const n=xe.c(9),{uploadPath:s,json:u,onSuccess:p,onError:h}=r,[b,g]=T.useState(!1),m=T.useRef(null);let y;n[0]===Symbol.for("react.memo_cache_sentinel")?(y=()=>{var E;(E=m.current)==null||E.abort(),m.current=null,g(!1)},n[0]=y):y=n[0];const w=y;let v;n[1]!==u||n[2]!==h||n[3]!==p||n[4]!==s?(v=async()=>{var C;(C=m.current)==null||C.abort();const E=new AbortController;m.current=E,g(!0);try{const _=await fetch(s,{method:"POST",headers:{"Content-Type":"application/json"},body:u,signal:E.signal}),j=await _.text();if(!_.ok){g(!1),h(`HTTP ${_.status} — ${j}`);return}g(!1),p(j)}catch(_){const j=_;if(j.name==="AbortError"){g(!1);return}g(!1),h(j.message??"Network error")}},n[1]=u,n[2]=h,n[3]=p,n[4]=s,n[5]=v):v=n[5];const x=v;let N;return n[6]!==b||n[7]!==x?(N={isUploading:b,upload:x,cancel:w},n[6]=b,n[7]=x,n[8]=N):N=n[8],N}var $h;const D_=((($h=navigator.userAgentData)==null?void 0:$h.platform)??navigator.platform).toLowerCase().includes("mac");function O_(r){return new Promise((n,s)=>{const u=new FileReader;u.onload=()=>n(u.result),u.onerror=()=>s(new Error(`Could not read file "${r.name}"`)),u.readAsText(r,"utf-8")})}function k_(r){const n=r.name.toLowerCase().endsWith(".json"),s=r.type==="application/json"||r.type==="text/plain";return!n&&!s?`"${r.name}" does not appear to be a JSON file. Only .json files are accepted.`:null}function R_({uploadPath:r,onSuccess:n,onClose:s,onError:u}){const[p,h]=T.useState(""),[b,g]=T.useState(null),[m,y]=T.useState(null),[w,v]=T.useState(!1),x=T.useRef(null),N=T.useRef(null),E=T.useRef(null),_=po(p).isJSON,j=_&&p.trim()!=="",{isUploading:k,upload:H,cancel:L}=M_({uploadPath:r,json:p,onSuccess:n,onError:O=>{g(O),u(O)}});T.useEffect(()=>{var Q;const O=x.current;if(O)return O.open||O.showModal(),(Q=N.current)==null||Q.focus(),()=>{O.open&&O.close()}},[]);const X=T.useCallback(()=>{L(),s()},[L,s]),Y=T.useCallback(O=>{O.target===x.current&&X()},[X]),B=T.useCallback(O=>{O.preventDefault(),X()},[X]),I=T.useCallback(()=>{g(null),H()},[H]),V=T.useCallback(O=>{O.key==="Enter"&&(O.ctrlKey||O.metaKey)&&(O.preventDefault(),j&&!k&&I())},[j,k,I]),K=T.useCallback(()=>{_&&h(uc(p))},[_,p]),$=T.useCallback(async O=>{var P;y(null),g(null);const Q=k_(O);if(Q){y(Q);return}try{const F=await O_(O);if(!po(F).isJSON){y(`"${O.name}" contains invalid JSON.`);return}h(uc(F)),(P=N.current)==null||P.focus()}catch(F){y(F.message)}},[]),te=T.useCallback(O=>{O.preventDefault(),O.stopPropagation(),w||v(!0)},[w]),ue=T.useCallback(O=>{O.preventDefault(),O.stopPropagation(),(O.currentTarget===O.target||!O.currentTarget.contains(O.relatedTarget))&&v(!1)},[]),he=T.useCallback(O=>{O.preventDefault(),O.stopPropagation(),v(!1);const Q=O.dataTransfer.files[0];Q&&$(Q)},[$]),re=T.useCallback(O=>{var P;const Q=(P=O.target.files)==null?void 0:P[0];Q&&($(Q),O.target.value="")},[$]),D=!_&&p.trim()!=="";return f.jsx("dialog",{ref:x,className:Ze.dialog,"aria-modal":"true","aria-labelledby":"mock-upload-modal-title",onClick:Y,onCancel:B,children:f.jsxs("div",{className:Ze.modalInner,onClick:O=>O.stopPropagation(),children:[f.jsxs("div",{className:Ze.modalHeader,children:[f.jsxs("div",{className:Ze.modalTitleGroup,children:[f.jsx("span",{id:"mock-upload-modal-title",className:Ze.modalTitle,children:"⬆️ Upload Mock Data"}),f.jsx("span",{className:Ze.modalPath,children:r})]}),f.jsx("button",{className:Ze.closeButton,onClick:X,"aria-label":"Close upload modal",title:"Close",disabled:k,children:"✕"})]}),f.jsxs("div",{className:Ze.modalBody,children:[f.jsxs("div",{className:`${Ze.dropZone} ${w?Ze.dropZoneActive:""}`,onDragOver:te,onDragLeave:ue,onDrop:he,"aria-label":"Drop a JSON file here",children:[f.jsx("span",{className:Ze.dropZoneIcon,children:"📂"}),f.jsxs("span",{className:Ze.dropZoneText,children:["Drop a ",f.jsx("code",{children:".json"})," file here"]}),f.jsx("span",{className:Ze.dropZoneOr,children:"— or —"}),f.jsx("input",{ref:E,type:"file",accept:".json,application/json",className:Ze.fileInputHidden,"aria-hidden":"true",tabIndex:-1,onChange:re}),f.jsx("button",{type:"button",className:Ze.browseButton,onClick:()=>{var O;return(O=E.current)==null?void 0:O.click()},disabled:k,"aria-label":"Browse for a JSON file",children:"Browse file…"})]}),m&&f.jsxs("span",{className:Ze.fileError,role:"alert",children:["⚠️ ",m]}),f.jsx("label",{htmlFor:"mock-upload-textarea",className:Ze.textareaLabel,children:"JSON Payload"}),f.jsx("textarea",{id:"mock-upload-textarea",ref:N,className:Ze.textarea,value:p,onChange:O=>{h(O.target.value),y(null)},onKeyDown:V,placeholder:"Paste JSON here, or drop / browse a .json file above",rows:10,spellCheck:!1,"aria-describedby":D?"mock-upload-validation":void 0}),D&&f.jsx("span",{id:"mock-upload-validation",className:Ze.validationError,role:"status",children:"⚠️ Invalid JSON — check syntax"}),f.jsx("span",{className:Ze.keyboardHint,children:D_?"⌘+Enter to upload":"Ctrl+Enter to upload"}),b&&f.jsxs("div",{className:Ze.errorBanner,role:"alert",children:["❌ Upload failed: ",b]})]}),f.jsxs("div",{className:Ze.modalFooter,children:[f.jsx("button",{className:Ze.formatButton,onClick:K,disabled:!_||k,title:"Format JSON","aria-label":"Format JSON",children:"Format"}),f.jsxs("div",{className:Ze.footerActions,children:[f.jsx("button",{className:Ze.cancelButton,onClick:X,disabled:k,children:"Cancel"}),f.jsx("button",{className:Ze.uploadButton,onClick:I,disabled:!j||k,"aria-busy":k,children:k?f.jsxs(f.Fragment,{children:[f.jsx("span",{className:Ze.spinner,"aria-hidden":"true"})," Uploading…"]}):"Upload ▶"})]})]})]})})}const mc=(r,n)=>n.some(s=>r instanceof s);let Yh,qh;function z_(){return Yh||(Yh=[IDBDatabase,IDBObjectStore,IDBIndex,IDBCursor,IDBTransaction])}function B_(){return qh||(qh=[IDBCursor.prototype.advance,IDBCursor.prototype.continue,IDBCursor.prototype.continuePrimaryKey])}const gc=new WeakMap,sc=new WeakMap,zi=new WeakMap;function H_(r){const n=new Promise((s,u)=>{const p=()=>{r.removeEventListener("success",h),r.removeEventListener("error",b)},h=()=>{s(ia(r.result)),p()},b=()=>{u(r.error),p()};r.addEventListener("success",h),r.addEventListener("error",b)});return zi.set(n,r),n}function G_(r){if(gc.has(r))return;const n=new Promise((s,u)=>{const p=()=>{r.removeEventListener("complete",h),r.removeEventListener("error",b),r.removeEventListener("abort",b)},h=()=>{s(),p()},b=()=>{u(r.error||new DOMException("AbortError","AbortError")),p()};r.addEventListener("complete",h),r.addEventListener("error",b),r.addEventListener("abort",b)});gc.set(r,n)}let yc={get(r,n,s){if(r instanceof IDBTransaction){if(n==="done")return gc.get(r);if(n==="store")return s.objectStoreNames[1]?void 0:s.objectStore(s.objectStoreNames[0])}return ia(r[n])},set(r,n,s){return r[n]=s,!0},has(r,n){return r instanceof IDBTransaction&&(n==="done"||n==="store")?!0:n in r}};function df(r){yc=r(yc)}function U_(r){return B_().includes(r)?function(...n){return r.apply(bc(this),n),ia(this.request)}:function(...n){return ia(r.apply(bc(this),n))}}function L_(r){return typeof r=="function"?U_(r):(r instanceof IDBTransaction&&G_(r),mc(r,z_())?new Proxy(r,yc):r)}function ia(r){if(r instanceof IDBRequest)return H_(r);if(sc.has(r))return sc.get(r);const n=L_(r);return n!==r&&(sc.set(r,n),zi.set(n,r)),n}const bc=r=>zi.get(r);function Y_(r,n,{blocked:s,upgrade:u,blocking:p,terminated:h}={}){const b=indexedDB.open(r,n),g=ia(b);return u&&b.addEventListener("upgradeneeded",m=>{u(ia(b.result),m.oldVersion,m.newVersion,ia(b.transaction),m)}),s&&b.addEventListener("blocked",m=>s(m.oldVersion,m.newVersion,m)),g.then(m=>{h&&m.addEventListener("close",()=>h()),p&&m.addEventListener("versionchange",y=>p(y.oldVersion,y.newVersion,y))}).catch(()=>{}),g}function q_(r,{blocked:n}={}){const s=indexedDB.deleteDatabase(r);return n&&s.addEventListener("blocked",u=>n(u.oldVersion,u)),ia(s).then(()=>{})}const X_=["get","getKey","getAll","getAllKeys","count"],J_=["put","add","delete","clear"],rc=new Map;function Xh(r,n){if(!(r instanceof IDBDatabase&&!(n in r)&&typeof n=="string"))return;if(rc.get(n))return rc.get(n);const s=n.replace(/FromIndex$/,""),u=n!==s,p=J_.includes(s);if(!(s in(u?IDBIndex:IDBObjectStore).prototype)||!(p||X_.includes(s)))return;const h=async function(b,...g){const m=this.transaction(b,p?"readwrite":"readonly");let y=m.store;return u&&(y=y.index(g.shift())),(await Promise.all([y[s](...g),p&&m.done]))[0]};return rc.set(n,h),h}df(r=>({...r,get:(n,s,u)=>Xh(n,s)||r.get(n,s,u),has:(n,s)=>!!Xh(n,s)||r.has(n,s)}));const I_=["continue","continuePrimaryKey","advance"],Jh={},vc=new WeakMap,pf=new WeakMap,Z_={get(r,n){if(!I_.includes(n))return r[n];let s=Jh[n];return s||(s=Jh[n]=function(...u){vc.set(this,pf.get(this)[n](...u))}),s}};async function*Q_(...r){let n=this;if(n instanceof IDBCursor||(n=await n.openCursor(...r)),!n)return;n=n;const s=new Proxy(n,Z_);for(pf.set(s,n),zi.set(s,bc(n));n;)yield s,n=await(vc.get(s)||n.continue()),vc.delete(s)}function Ih(r,n){return n===Symbol.asyncIterator&&mc(r,[IDBIndex,IDBObjectStore,IDBCursor])||n==="iterate"&&mc(r,[IDBIndex,IDBObjectStore])}df(r=>({...r,get(n,s,u){return Ih(n,s)?Q_:r.get(n,s,u)},has(n,s){return Ih(n,s)||r.has(n,s)}}));const hf="minigraph-clipboard",V_=1,kn="items";let Oi=null;function Zh(){return Y_(hf,V_,{upgrade(r){r.objectStoreNames.contains(kn)&&r.deleteObjectStore(kn);const n=r.createObjectStore(kn,{keyPath:"id"});n.createIndex("by-alias","node.alias",{unique:!0}),n.createIndex("by-clippedAt","clippedAt")}})}function ml(){return Oi||(Oi=Zh().catch(async r=>(console.warn("[clipboard/db] openDB failed, deleting and recreating:",r),Oi=null,await q_(hf),Zh()))),Oi}async function K_(){return(await(await ml()).getAllFromIndex(kn,"by-clippedAt")).reverse()}async function Qh(r){return(await ml()).getFromIndex(kn,"by-alias",r)}async function $_(r){await(await ml()).add(kn,r)}async function W_(r,n){const u=(await ml()).transaction(kn,"readwrite");await u.store.delete(r),await u.store.add(n),await u.done}async function P_(r){await(await ml()).delete(kn,r)}async function F_(){await(await ml()).clear(kn)}const e3="minigraph-clipboard-sync";function t3(){return new BroadcastChannel(e3)}function n3(r,n){switch(n.type){case"HYDRATE":return{items:n.items,isLoading:!1};case"ITEM_ADDED":return{...r,items:[n.item,...r.items]};case"ITEM_REPLACED":{const s=r.items.filter(u=>u.id!==n.previousId);return{...r,items:[n.item,...s]}}case"ITEM_REMOVED":return{...r,items:r.items.filter(s=>s.id!==n.id)};case"ITEMS_CLEARED":return{...r,items:[]};default:return r}}const ff=T.createContext(null);function a3({children:r}){const[n,s]=T.useReducer(n3,{items:[],isLoading:!0}),u=T.useRef(null);T.useEffect(()=>{K_().then(y=>s({type:"HYDRATE",items:y}))},[]),T.useEffect(()=>{let y;try{y=t3()}catch{return}return u.current=y,y.onmessage=w=>{const v=w.data;switch(v.type){case"item-added":s({type:"ITEM_ADDED",item:v.item});break;case"item-replaced":s({type:"ITEM_REPLACED",item:v.item,previousId:v.previousId});break;case"item-removed":s({type:"ITEM_REMOVED",id:v.id});break;case"items-cleared":s({type:"ITEMS_CLEARED"});break}},()=>{y.close(),u.current=null}},[]);const p=T.useCallback(y=>{var w;(w=u.current)==null||w.postMessage(y)},[]),h=T.useCallback(async(y,w,v)=>{try{const x={id:crypto.randomUUID(),clippedAt:new Date().toISOString(),sourceWsPath:v.sourceWsPath,sourceLabel:v.sourceLabel,node:y,connections:w},N=await Qh(y.alias);if(N)return{status:"duplicate",existingItem:N,pendingItem:x};try{await $_(x)}catch(E){if(E instanceof DOMException&&E.name==="ConstraintError"){const C=await Qh(y.alias);if(C)return{status:"duplicate",existingItem:C,pendingItem:x}}throw E}return s({type:"ITEM_ADDED",item:x}),p({type:"item-added",item:x}),{status:"added"}}catch(x){return{status:"error",message:x instanceof Error?x.message:String(x)}}},[p]),b=T.useCallback(async(y,w)=>{await W_(w,y),s({type:"ITEM_REPLACED",item:y,previousId:w}),p({type:"item-replaced",item:y,previousId:w})},[p]),g=T.useCallback(async y=>{await P_(y),s({type:"ITEM_REMOVED",id:y}),p({type:"item-removed",id:y})},[p]),m=T.useCallback(async()=>{await F_(),s({type:"ITEMS_CLEARED"}),p({type:"items-cleared"})},[p]);return f.jsx(ff.Provider,{value:{items:n.items,isLoading:n.isLoading,clipNode:h,confirmReplace:b,removeItem:g,clearAll:m},children:r})}function mf(){const r=T.useContext(ff);if(!r)throw new Error("useClipboardContext must be used inside <ClipboardProvider>");return r}function gf(r){const n=Date.now(),s=new Date(r).getTime(),u=n-s;if(u<0)return"just now";const p=Math.floor(u/1e3);if(p<60)return"just now";const h=Math.floor(p/60);if(h<60)return`${h} min ago`;const b=Math.floor(h/60);if(b<24)return`${b} hour${b>1?"s":""} ago`;const g=Math.floor(b/24);return g===1?"yesterday":g<30?`${g} days ago`:new Date(r).toLocaleDateString()}const l3="_card_3lfqf_2",o3="_alias_3lfqf_10",i3="_meta_3lfqf_18",s3="_propsLine_3lfqf_24",r3="_timestamp_3lfqf_32",c3="_actions_3lfqf_39",u3="_pasteBtn_3lfqf_45",d3="_inspectBtn_3lfqf_46",p3="_removeBtn_3lfqf_47",Ft={card:l3,alias:o3,meta:i3,propsLine:s3,timestamp:r3,actions:c3,pasteBtn:u3,inspectBtn:d3,removeBtn:p3};function h3(r){const n=xe.c(55),{item:s,connected:u,onPaste:p,onRemove:h,onInspect:b}=r,{node:g,connections:m,clippedAt:y,sourceLabel:w}=s,v=g.types[0]??"—",x=g.properties.skill??"—";let N;n[0]!==g.properties?(N=Object.entries(g.properties).filter(m3).map(f3),n[0]=g.properties,n[1]=N):N=n[1];const E=N,C=E.length>0?E.join(", "):"—";let _;if(n[2]!==m||n[3]!==g.alias){let ee;n[5]!==g.alias?(ee=ye=>ye.source===g.alias,n[5]=g.alias,n[6]=ee):ee=n[6],_=m.filter(ee),n[2]=m,n[3]=g.alias,n[4]=_}else _=n[4];const j=_.length;let k;if(n[7]!==m||n[8]!==g.alias){let ee;n[10]!==g.alias?(ee=ye=>ye.target===g.alias,n[10]=g.alias,n[11]=ee):ee=n[11],k=m.filter(ee),n[7]=m,n[8]=g.alias,n[9]=k}else k=n[9];const H=k.length,L=`${m.length} (${j} out, ${H} in)`;let X;n[12]!==g.alias?(X=f.jsx("div",{className:Ft.alias,children:g.alias}),n[12]=g.alias,n[13]=X):X=n[13];let Y;n[14]!==v?(Y=f.jsxs("div",{className:Ft.meta,children:["Type: ",v]}),n[14]=v,n[15]=Y):Y=n[15];let B;n[16]!==x?(B=f.jsxs("div",{className:Ft.meta,children:["Skill: ",x]}),n[16]=x,n[17]=B):B=n[17];let I;n[18]!==C?(I=f.jsxs("span",{className:Ft.propsLine,children:["Props: ",C]}),n[18]=C,n[19]=I):I=n[19];let V;n[20]!==C||n[21]!==I?(V=f.jsx("div",{className:Ft.meta,title:C,children:I}),n[20]=C,n[21]=I,n[22]=V):V=n[22];let K;n[23]!==L?(K=f.jsxs("div",{className:Ft.meta,children:["Connections: ",L]}),n[23]=L,n[24]=K):K=n[24];let $;n[25]!==y?($=gf(y),n[25]=y,n[26]=$):$=n[26];let te;n[27]!==w||n[28]!==$?(te=f.jsxs("div",{className:Ft.timestamp,children:["Clipped ",$," from ",w]}),n[27]=w,n[28]=$,n[29]=te):te=n[29];let ue;n[30]!==s||n[31]!==p?(ue=()=>p(s),n[30]=s,n[31]=p,n[32]=ue):ue=n[32];const he=!u,re=`Paste node ${g.alias}`;let D;n[33]!==ue||n[34]!==he||n[35]!==re?(D=f.jsx("button",{className:Ft.pasteBtn,onClick:ue,disabled:he,"aria-label":re,children:"Paste"}),n[33]=ue,n[34]=he,n[35]=re,n[36]=D):D=n[36];const O=`Inspect node ${g.alias}`;let Q;n[37]!==b||n[38]!==O?(Q=f.jsx("button",{className:Ft.inspectBtn,onClick:b,"aria-label":O,children:"Describe"}),n[37]=b,n[38]=O,n[39]=Q):Q=n[39];const P=`Remove node ${g.alias} from clipboard`;let F;n[40]!==h||n[41]!==P?(F=f.jsx("button",{className:Ft.removeBtn,onClick:h,"aria-label":P,children:"Remove"}),n[40]=h,n[41]=P,n[42]=F):F=n[42];let ie;n[43]!==D||n[44]!==Q||n[45]!==F?(ie=f.jsxs("div",{className:Ft.actions,children:[D,Q,F]}),n[43]=D,n[44]=Q,n[45]=F,n[46]=ie):ie=n[46];let oe;return n[47]!==te||n[48]!==ie||n[49]!==X||n[50]!==Y||n[51]!==B||n[52]!==V||n[53]!==K?(oe=f.jsxs("div",{className:Ft.card,children:[X,Y,B,V,K,te,ie]}),n[47]=te,n[48]=ie,n[49]=X,n[50]=Y,n[51]=B,n[52]=V,n[53]=K,n[54]=oe):oe=n[54],oe}function f3(r){const[n,s]=r,u=typeof s=="string"?s:JSON.stringify(s);return`${n}=${u&&u.length>30?u.slice(0,30)+"…":u}`}function m3(r){const[n]=r;return n!=="skill"}const g3="_sidebar_nf394_2",y3="_header_nf394_12",b3="_headerTitle_nf394_22",v3="_clearBtn_nf394_29",_3="_itemList_nf394_45",S3="_loading_nf394_55",x3="_emptyState_nf394_65",w3="_emptyIcon_nf394_78",T3="_emptyTitle_nf394_83",E3="_emptyHint_nf394_87",A3="_inspectPanel_nf394_93",N3="_inspectHeader_nf394_101",C3="_inspectClose_nf394_115",j3="_inspectBody_nf394_129",M3="_dialog_nf394_135",D3="_dialogTitle_nf394_150",O3="_dialogBody_nf394_157",k3="_dialogActions_nf394_164",R3="_cancelBtn_nf394_171",z3="_replaceBtn_nf394_185",lt={sidebar:g3,header:y3,headerTitle:b3,clearBtn:v3,itemList:_3,loading:S3,emptyState:x3,emptyIcon:w3,emptyTitle:T3,emptyHint:E3,inspectPanel:A3,inspectHeader:N3,inspectClose:C3,inspectBody:j3,dialog:M3,dialogTitle:D3,dialogBody:O3,dialogActions:k3,cancelBtn:R3,replaceBtn:z3};function B3(){const r=xe.c(1);let n;return r[0]===Symbol.for("react.memo_cache_sentinel")?(n=f.jsxs("div",{className:lt.emptyState,children:[f.jsx("span",{className:lt.emptyIcon,children:"📋"}),f.jsx("span",{className:lt.emptyTitle,children:"No items clipped yet."}),f.jsx("span",{className:lt.emptyHint,children:"Right-click a node in the Graph view to get started."})]}),r[0]=n):n=r[0],n}function H3(r){const n=xe.c(18),{connected:s,onPaste:u}=r,p=mf(),[h,b]=T.useState(null);let g;n[0]===Symbol.for("react.memo_cache_sentinel")?(g=f.jsx("span",{className:lt.headerTitle,children:"Clipboard"}),n[0]=g):g=n[0];let m;n[1]!==p?(m=p.items.length>0&&f.jsx("button",{className:lt.clearBtn,onClick:()=>p.clearAll(),"aria-label":"Clear all clipboard items",children:"Clear"}),n[1]=p,n[2]=m):m=n[2];let y;n[3]!==m?(y=f.jsxs("div",{className:lt.header,children:[g,m]}),n[3]=m,n[4]=y):y=n[4];let w;n[5]!==p||n[6]!==s||n[7]!==(h==null?void 0:h.id)||n[8]!==u?(w=p.isLoading?f.jsx("div",{className:lt.loading,children:"Loading…"}):p.items.length===0?f.jsx(B3,{}):p.items.map(E=>f.jsx(h3,{item:E,connected:s,onPaste:u,onRemove:()=>p.removeItem(E.id),onInspect:()=>b((h==null?void 0:h.id)===E.id?null:E)},E.id)),n[5]=p,n[6]=s,n[7]=h==null?void 0:h.id,n[8]=u,n[9]=w):w=n[9];let v;n[10]!==w?(v=f.jsx("div",{className:lt.itemList,children:w}),n[10]=w,n[11]=v):v=n[11];let x;n[12]!==h?(x=h&&f.jsxs("div",{className:lt.inspectPanel,children:[f.jsxs("div",{className:lt.inspectHeader,children:[f.jsxs("span",{children:["Describe node ",h.node.alias]}),f.jsx("button",{className:lt.inspectClose,onClick:()=>b(null),"aria-label":"Close inspect panel",children:"✕"})]}),f.jsx("div",{className:lt.inspectBody,children:f.jsx(Sc,{data:{node:h.node,connections:h.connections},style:uo})})]}),n[12]=h,n[13]=x):x=n[13];let N;return n[14]!==y||n[15]!==v||n[16]!==x?(N=f.jsxs("div",{className:lt.sidebar,children:[y,v,x]}),n[14]=y,n[15]=v,n[16]=x,n[17]=N):N=n[17],N}const Vh=120,Kh=18,G3=180,U3=650;function L3(r){const{wheelTargetRef:n,scrollRef:s,contentWrapperRef:u,currentIndex:p,totalPages:h,onNavigatePrev:b,onNavigateNext:g}=r,m=T.useRef(0),y=T.useRef(null),w=T.useRef(!1),v=T.useRef(null),x=T.useRef(b),N=T.useRef(g),E=T.useRef(p),C=T.useRef(h);T.useEffect(()=>{x.current=b}),T.useEffect(()=>{N.current=g}),T.useEffect(()=>{E.current=p}),T.useEffect(()=>{C.current=h}),T.useEffect(()=>{v.current!==null&&(clearTimeout(v.current),v.current=null),u.current&&(u.current.style.transition="none",u.current.style.transform="translateY(0)"),m.current=0,y.current=null},[p]),T.useEffect(()=>{const _=n.current;if(!_)return;function j(){m.current=0,y.current=null,u.current&&(u.current.style.transition="transform 0.28s cubic-bezier(0.25, 0.46, 0.45, 0.94)",u.current.style.transform="translateY(0)")}function k(H){if(H.deltaY===0)return;const L=s.current;if(!L)return;const X=L.scrollTop<=0,Y=L.scrollTop+L.clientHeight>=L.scrollHeight-1,B=H.deltaY<0,I=H.deltaY>0,V=X&&B,K=Y&&I;if(!V&&!K){j();return}if(w.current)return;const $=E.current,te=C.current;if(V&&$===0||K&&$===te-1)return;const ue=V?"prev":"next";if(y.current!==null&&y.current!==ue&&j(),y.current=ue,m.current+=Math.abs(H.deltaY),u.current){const he=ue==="prev"?-1:1,re=m.current*(Kh/Vh),D=Math.min(re,Kh)*he;u.current.style.transition="none",u.current.style.transform=`translateY(${D}px)`}if(v.current!==null&&clearTimeout(v.current),v.current=setTimeout(j,G3),m.current>=Vh){v.current!==null&&clearTimeout(v.current);const he=y.current;j(),w.current=!0,he==="prev"?x.current():N.current(),setTimeout(()=>{w.current=!1},U3)}}return _.addEventListener("wheel",k,{passive:!0}),()=>{v.current!==null&&clearTimeout(v.current),_.removeEventListener("wheel",k)}},[])}const Y3="_helpRoot_18tja_2",q3="_categoryNav_18tja_11",X3="_categoryTabScroller_18tja_21",J3="_categoryTab_18tja_21",I3="_categoryTabActive_18tja_71",Z3="_maximizeButton_18tja_78",Q3="_closeButton_18tja_100",V3="_helpBody_18tja_122",K3="_emptyFallback_18tja_130",$3="_helpContent_18tja_147",W3="_topicLink_18tja_226",P3="_helpBodyContent_18tja_271",F3="_chipStrip_18tja_276",eS="_chipStripLabel_18tja_294",tS="_topicChip_18tja_310",nS="_topicChipActive_18tja_338",St={helpRoot:Y3,categoryNav:q3,categoryTabScroller:X3,categoryTab:J3,categoryTabActive:I3,maximizeButton:Z3,closeButton:Q3,helpBody:V3,emptyFallback:K3,helpContent:$3,topicLink:W3,helpBodyContent:P3,chipStrip:F3,chipStripLabel:eS,topicChip:tS,topicChipActive:nS};function _c(r){return typeof r=="string"?r:typeof r=="number"?String(r):Array.isArray(r)?r.map(_c).join(""):Fh.isValidElement(r)?_c(r.props.children):""}function aS(r){if(!r.trim().toLowerCase().startsWith("help "))return null;const u=r.trim().slice(5).replace(/\s*\(.*\)\s*$/,"").trim().toLowerCase();return u.length>0?u:null}function lS(r){var ye;const n=xe.c(53),{activeTopic:s,onNavigate:u,onClose:p,onToggleMaximize:h,isMaximized:b}=r,g=T.useRef(null),m=T.useRef(null),y=T.useRef(null),w=T.useRef(null);let v;n[0]===Symbol.for("react.memo_cache_sentinel")?(v=()=>{g.current&&(g.current.scrollTop=0)},n[0]=v):v=n[0];let x;n[1]!==s?(x=[s],n[1]=s,n[2]=x):x=n[2],T.useEffect(v,x);let N;n[3]===Symbol.for("react.memo_cache_sentinel")?(N=()=>{const se=w.current;if(!se)return;const ge=se.querySelector('[aria-current="step"]');ge&&ge.scrollIntoView({block:"nearest",inline:"nearest",behavior:"smooth"})},n[3]=N):N=n[3];let E;n[4]!==s?(E=[s],n[4]=s,n[5]=E):E=n[5],T.useEffect(N,E);let C;n[6]!==s?(C=sf(s),n[6]=s,n[7]=C):C=n[7];const _=C;let j;n[8]!==_?(j=pc(_),n[8]=_,n[9]=j):j=n[9];const k=j,H=k.length;let L;n[10]!==_?(L=((ye=dc.find(se=>se.id===_))==null?void 0:ye.chipStripLabel)??null,n[10]=_,n[11]=L):L=n[11];const X=L,Y=fl.indexOf(s),B=Y<0?0:Y,I=fl.length;let V,K;n[12]!==u||n[13]!==B?(V=()=>u(fl[B-1]??""),K=()=>u(fl[B+1]??fl[fl.length-1]),n[12]=u,n[13]=B,n[14]=V,n[15]=K):(V=n[14],K=n[15]);let $;n[16]!==B||n[17]!==V||n[18]!==K?($={wheelTargetRef:m,scrollRef:g,contentWrapperRef:y,currentIndex:B,totalPages:I,onNavigatePrev:V,onNavigateNext:K},n[16]=B,n[17]=V,n[18]=K,n[19]=$):$=n[19],L3($);let te;n[20]!==s?(te=Ri(s),n[20]=s,n[21]=te):te=n[21];const ue=te;let he;n[22]!==u?(he=se=>{const{children:ge,...je}=se,He=_c(ge).trim(),we=aS(He);return we!==null&&Ri(we)!==null?f.jsx("li",{...je,children:f.jsx("button",{className:St.topicLink,"aria-label":`Open help topic: ${we}`,onClick:()=>u(we),children:ge})}):f.jsx("li",{...je,children:ge})},n[22]=u,n[23]=he):he=n[23];const re=he;let D;n[24]!==_||n[25]!==u?(D=dc.map(se=>f.jsx("button",{className:[St.categoryTab,se.id===_?St.categoryTabActive:""].join(" ").trim(),"aria-current":se.id===_?"true":void 0,onClick:()=>{const ge=pc(se.id);u(ge[0]??"")},children:se.label},se.id)),n[24]=_,n[25]=u,n[26]=D):D=n[26];let O;n[27]!==D?(O=f.jsx("div",{className:St.categoryTabScroller,children:D}),n[27]=D,n[28]=O):O=n[28];let Q;n[29]!==b||n[30]!==h?(Q=h&&f.jsx("button",{className:St.maximizeButton,onClick:h,"aria-label":b?"Restore help panel":"Maximize help panel",children:b?"⊞":"⛶"}),n[29]=b,n[30]=h,n[31]=Q):Q=n[31];let P;n[32]!==p?(P=p&&f.jsx("button",{className:St.closeButton,onClick:p,"aria-label":"Close help panel",children:"×"}),n[32]=p,n[33]=P):P=n[33];let F;n[34]!==O||n[35]!==Q||n[36]!==P?(F=f.jsxs("nav",{className:St.categoryNav,"aria-label":"Help categories",children:[O,Q,P]}),n[34]=O,n[35]=Q,n[36]=P,n[37]=F):F=n[37];let ie;n[38]!==_||n[39]!==s||n[40]!==k||n[41]!==H||n[42]!==X||n[43]!==u?(ie=H>1&&f.jsxs("div",{className:St.chipStrip,ref:w,children:[X!==null&&f.jsx("span",{className:St.chipStripLabel,children:X}),k.map(se=>{const ge=se===s,je=A1(se,_);return f.jsx("button",{className:[St.topicChip,ge?St.topicChipActive:""].join(" ").trim(),"aria-current":ge?"step":void 0,onClick:()=>u(se),children:je},se)})]}),n[38]=_,n[39]=s,n[40]=k,n[41]=H,n[42]=X,n[43]=u,n[44]=ie):ie=n[44];let oe;n[45]!==s||n[46]!==ue||n[47]!==re?(oe=f.jsx("div",{className:St.helpBody,ref:g,children:f.jsx("div",{className:St.helpBodyContent,ref:y,children:ue===null?f.jsxs("div",{className:St.emptyFallback,children:[f.jsxs("code",{children:["help ",s||""]}),"  not found in the local bundle."]}):f.jsx("div",{className:St.helpContent,children:f.jsx(Vg,{remarkPlugins:[Kg],components:s===""?{li:re}:void 0,children:ue})})})}),n[45]=s,n[46]=ue,n[47]=re,n[48]=oe):oe=n[48];let ee;return n[49]!==F||n[50]!==ie||n[51]!==oe?(ee=f.jsxs("div",{className:St.helpRoot,role:"region","aria-label":"Help browser",ref:m,children:[F,ie,oe]}),n[49]=F,n[50]=ie,n[51]=oe,n[52]=ee):ee=n[52],ee}function oS(r){const n=xe.c(22),{existingItem:s,pendingItem:u,onReplace:p,onCancel:h}=r,b=T.useRef(null);let g,m;n[0]===Symbol.for("react.memo_cache_sentinel")?(g=()=>{const k=b.current;k&&!k.open&&k.showModal()},m=[],n[0]=g,n[1]=m):(g=n[0],m=n[1]),T.useEffect(g,m);let y;n[2]===Symbol.for("react.memo_cache_sentinel")?(y=f.jsx("h2",{id:"duplicate-dialog-title",className:lt.dialogTitle,children:"Duplicate Node"}),n[2]=y):y=n[2];let w;n[3]!==u.node.alias?(w=f.jsxs("strong",{children:['"',u.node.alias,'"']}),n[3]=u.node.alias,n[4]=w):w=n[4];let v;n[5]!==s.clippedAt?(v=gf(s.clippedAt),n[5]=s.clippedAt,n[6]=v):v=n[6];let x;n[7]!==w||n[8]!==v?(x=f.jsxs("p",{className:lt.dialogBody,children:["A clipboard item with alias ",w," already exists (clipped ",v,")."]}),n[7]=w,n[8]=v,n[9]=x):x=n[9];let N;n[10]===Symbol.for("react.memo_cache_sentinel")?(N=f.jsx("p",{className:lt.dialogBody,children:"Replace it with the new snapshot?"}),n[10]=N):N=n[10];let E;n[11]!==h?(E=f.jsx("button",{className:lt.cancelBtn,onClick:h,children:"Cancel"}),n[11]=h,n[12]=E):E=n[12];let C;n[13]!==p?(C=f.jsx("button",{className:lt.replaceBtn,onClick:p,children:"Replace"}),n[13]=p,n[14]=C):C=n[14];let _;n[15]!==E||n[16]!==C?(_=f.jsxs("div",{className:lt.dialogActions,children:[E,C]}),n[15]=E,n[16]=C,n[17]=_):_=n[17];let j;return n[18]!==h||n[19]!==_||n[20]!==x?(j=f.jsxs("dialog",{ref:b,className:lt.dialog,onClose:h,"aria-labelledby":"duplicate-dialog-title",children:[y,x,N,_]}),n[18]=h,n[19]=_,n[20]=x,n[21]=j):j=n[21],j}function iS(r,n){if(!n)return null;const s=r.trim().toLowerCase();if(s!=="help"&&!s.startsWith("help "))return null;const u=rf(r);return Ri(u)!==null?u:null}class sS{constructor(){this.listeners=new Map}on(n,s){const u=n;return this.listeners.has(u)||this.listeners.set(u,new Set),this.listeners.get(u).add(s),()=>{var p;(p=this.listeners.get(u))==null||p.delete(s)}}emit(n){const s=this.listeners.get(n.kind);s&&s.forEach(u=>{try{u(n)}catch(p){console.error(`[ProtocolBus] listener for '${n.kind}' threw:`,p)}})}clear(){this.listeners.clear()}}const rS=new Set(["info","error","ping","welcome"]);function cS(r,n){const s=[],u={msgId:r,raw:n};let p=!1,h=!1,b=!1,g=!1,m=!1;const y=po(n);if(y.isJSON){const _=y.data;if(typeof _.type=="string"){const j=_.type;return s.push({...u,kind:"lifecycle",type:j,knownType:rS.has(j),message:typeof _.message=="string"?_.message:n,time:_.time??null}),s.length>0?s:[{...u,kind:"unclassified"}]}return s.push({...u,kind:"json.response",data:y.data}),s.length>0?s:[{...u,kind:"unclassified"}]}const w=jy(n);w&&(m=!0,s.push({...u,kind:"payload.large",apiPath:w.apiPath,byteSize:w.byteSize,filename:w.filename}));const v=My(n);v&&(b=!0,s.push({...u,kind:"upload.invitation",uploadPath:v}));const x=lf(n);if(x&&(g=!0,s.push({...u,kind:"upload.contentPath",uploadPath:x})),af(n)){h=!0;const _=Tc(n);_&&s.push({...u,kind:"graph.link",apiPath:_})}if(h){const _=Ny(n);_&&s.push({...u,kind:"graph.exported",graphName:_.graphName,apiPath:_.apiPath})}const N=ky(n);N&&s.push({...u,kind:"graph.mutation",mutationType:N}),n.startsWith("> ")&&(p=!0,s.push({...u,kind:"command.echo",commandText:n.slice(2)})),Dy(n)&&s.push({...u,kind:"command.helpOrDescribe",commandText:n.slice(2)});const E=Oy(n);E&&s.push({...u,kind:"command.importGraph",graphName:E});const C=Cy(n);return C&&s.push({...u,kind:"graph.export.failed",reason:C.reason}),!p&&!h&&!b&&!g&&!m&&wc(n)&&s.push({...u,kind:"docs.response",isMarkdown:!0}),s.length===0&&s.push({...u,kind:"unclassified"}),s}function uS(r){const n=xe.c(12),{messages:s,bus:u}=r,p=T.useRef(-1);let h;n[0]!==s?(h=()=>{s.length>0&&(p.current=s[s.length-1].id)},n[0]=s,n[1]=h):h=n[1];let b;n[2]===Symbol.for("react.memo_cache_sentinel")?(b=[],n[2]=b):b=n[2],T.useEffect(h,b);let g;if(n[3]!==s){g=new Map;for(const x of s)g.set(x.id,cS(x.id,x.raw));n[3]=s,n[4]=g}else g=n[4];const m=g;let y,w;n[5]!==u||n[6]!==m||n[7]!==s?(y=()=>{if(s.length===0)return;const x=s.filter(N=>N.id>p.current);if(x.length!==0){p.current=s[s.length-1].id;for(const N of x){const E=m.get(N.id);if(E)for(const C of E)u.emit(C)}}},w=[s,u,m],n[5]=u,n[6]=m,n[7]=s,n[8]=y,n[9]=w):(y=n[8],w=n[9]),T.useEffect(y,w);let v;return n[10]!==m?(v={classificationMap:m},n[10]=m,n[11]=v):v=n[11],v}function dS({config:r}){const{title:n,wsPath:s,storageKeyPayload:u,storageKeyHistory:p,storageKeyTab:h,storageKeySavedGraphs:b,supportsUpload:g,supportsClipboard:m,supportsHelp:y,tabs:w}=r,v=kg(),[x,N]=oa(u,""),E=xc(),[C,_]=T.useState(()=>E.peekPendingPayload(s)),{takePendingPayload:j}=E;T.useEffect(()=>{const De=j(s);De!==null&&_(De)},[j,s]);const k=C??x,H=T.useCallback(De=>{_(null),N(De)},[N]),L=T.useMemo(()=>k?by(k):{valid:!0,error:null,type:null},[k]),{toasts:X,addToast:Y,removeToast:B}=vy(),V=T.useRef(new sS).current,K=T.useCallback(De=>iS(De,y===!0)!==null,[y]),$=By({wsPath:s,storageKeyHistory:p,payload:k,addToast:Y,bus:V,handleLocalCommand:K}),{classificationMap:te}=uS({messages:$.messages,bus:V}),[ue,he]=U1(s),{graphData:re,setGraphData:D,rightTab:O,setRightTab:Q,isRefreshing:P}=Uy(ue,Y,w[0],w,h),{modalUploadPath:F,successfulUploadPaths:ie,handleOpenUploadModal:oe,handleCloseUploadModal:ee,handleUploadSuccess:ye,handleUploadError:se,resetSuccessfulPaths:ge}=D1({bus:V,addToast:Y});Ly({bus:V,pinnedGraphPath:ue,setPinnedGraphPath:he,connected:$.connected,sendRawText:$.sendRawText,addToast:Y});const je=T.useRef(!1);T.useEffect(()=>{je.current&&!$.connected&&(he(null),D(null)),je.current=$.connected},[$.connected,he,D]);const[He,we]=oa(r.storageKeyHelpTopic??"help-topic-fallback",""),[ae,de]=oa("help-panel-open",!1),[fe,ze]=T.useState(()=>!!y&&!ae),[W,be]=T.useState(!1),Te=T.useRef(null),me=T.useCallback(()=>{fe&&(be(!0),Te.current=setTimeout(()=>ze(!1),400))},[fe]);T.useEffect(()=>{if(!fe||W)return;const De=setTimeout(me,3e3);return()=>clearTimeout(De)},[fe,W,me]),T.useEffect(()=>{ae&&fe&&me()},[ae,fe,me]),T.useEffect(()=>()=>{Te.current&&clearTimeout(Te.current)},[]),T.useEffect(()=>{if(!y)return;const De=Et=>{Et.ctrlKey&&Et.key==="`"&&(Et.preventDefault(),de(et=>!et))};return window.addEventListener("keydown",De),()=>window.removeEventListener("keydown",De)},[y,de]),N1({bus:V,setHelpTopic:we,onTabSwitch:y?()=>de(!0):()=>{}}),O1({bus:V,connected:$.connected,appendMessage:$.appendMessage,addToast:Y});const Oe=mf(),[Ge,dt]=oa("clipboard-sidebar-open",!1),[Fe,ct]=T.useState(null),sa=T.useCallback(De=>{const et=(re==null?void 0:re.nodes.some(ja=>ja.alias===De.node.alias))??!1?"update":"create",Rn=L1(et,De.node);$.setCommand(Rn),Y(`${et==="create"?"Create":"Update"} command for "${De.node.alias}" pasted to input`,"info")},[re,$.setCommand,Y]),ve=T.useCallback(async(De,Et)=>{try{const et=await Oe.clipNode(De,Et,{sourceWsPath:s,sourceLabel:r.label});switch(et.status){case"added":Y(`Node "${De.alias}" clipped to clipboard`,"success");break;case"duplicate":ct({pendingItem:et.pendingItem,existingItem:et.existingItem});break;case"error":Y(`Clip failed: ${et.message}`,"error");break}}catch(et){Y(`Clip failed: ${et instanceof Error?et.message:String(et)}`,"error")}},[Oe,s,r.label,Y]),hn=R1(b??""),{defaultName:fo,setLastSavedName:mo,resetName:ra}=B1(b?`${b}-untitled-counter`:"untitled-counter",V),{handleSaveGraph:Bi,handleLoadGraph:go}=G1({bus:V,connected:$.connected,sendRawText:$.sendRawText,saveGraph:hn.saveGraph,setLastSavedName:mo,addToast:Y}),Hi=T.useCallback(De=>{const Et=te.get(De.id),et=Et==null?void 0:Et.find(Rn=>Rn.kind==="graph.link");et&&he(et.apiPath)},[te]),{handleSendToJsonPath:Gi}=C1({ctx:E,navigate:v,addToast:Y,wsPath:s}),ca=Gy("(max-width: 768px)"),{defaultLayout:xt,onLayoutChanged:on}=Mg({id:r.path+"-panel-split",storage:localStorage}),wt=T.useCallback(()=>H(uc(k)),[k]),Ui=T.useCallback(()=>{$.clearMessages(),he(null),D(null),ge(),ra()},[$.clearMessages,D,ge,ra]);return f.jsxs("div",{className:kt.wrapper,children:[f.jsx(K1,{toasts:X,onRemove:B}),F&&f.jsx(R_,{uploadPath:F,onSuccess:ye,onClose:ee,onError:se}),f.jsxs("header",{className:kt.header,children:[f.jsx("h1",{className:kt.title,children:n}),f.jsxs("div",{className:kt.headerActions,children:[b&&f.jsx(H0,{disabled:!re,defaultName:fo,onSave:Bi,nameExists:hn.hasGraph,connected:$.connected}),b&&hn.savedGraphs.length>0&&f.jsx(V0,{savedGraphs:hn.savedGraphs,onLoad:go,onDelete:hn.deleteGraph,connected:$.connected}),m&&f.jsxs("button",{className:kt.clipboardToggle,onClick:()=>dt(De=>!De),"aria-label":Ge?"Close clipboard sidebar":"Open clipboard sidebar","aria-pressed":Ge,children:["Clipboard",Oe.items.length>0?` (${Oe.items.length})`:""]}),f.jsx(A0,{addToast:Y}),y&&f.jsxs("div",{className:kt.helpButtonWrapper,children:[f.jsx("button",{className:`${kt.helpToggle}${fe&&!W?` ${kt.helpTogglePulsing}`:""}`,onClick:()=>de(De=>!De),"aria-label":ae?"Close help panel":"Open help panel","aria-pressed":ae,children:"?"}),fe&&f.jsxs("div",{className:`${kt.helpHint}${W?` ${kt.helpHintFading}`:""}`,onClick:me,role:"status",children:[f.jsx("kbd",{className:kt.helpHintKbd,children:"Ctrl + `"})," to toggle help"]})]})]})]}),Fe&&f.jsx(oS,{existingItem:Fe.existingItem,pendingItem:Fe.pendingItem,onReplace:async()=>{try{await Oe.confirmReplace(Fe.pendingItem,Fe.existingItem.id),ct(null),Y(`Clipboard item "${Fe.pendingItem.node.alias}" replaced`,"success")}catch(De){Y(`Replace failed: ${De instanceof Error?De.message:String(De)}`,"error")}},onCancel:()=>{ct(null),Y("Clip cancelled","info")}}),f.jsxs(Wh,{className:kt.panelGroup,orientation:ca?"vertical":"horizontal",defaultLayout:xt,onLayoutChanged:on,children:[f.jsx(co,{defaultSize:ae||Ge?"50%":"60%",minSize:"25%",children:f.jsx(n_,{messages:$.messages,classificationMap:te,onCopy:$.copyMessages,onClear:Ui,consoleRef:$.consoleRef,command:$.command,onCommandChange:$.setCommand,onCommandKeyDown:$.handleKeyDown,onSend:$.sendCommand,sendDisabled:!$.connected||!$.command.trim(),inputDisabled:!$.connected,commandHistory:$.history,onGraphLinkMessage:Hi,onCopyMessage:()=>Y("Copied to clipboard","success"),onSendToJsonPath:Gi,onUploadMockData:oe,successfulUploadPaths:ie})}),f.jsx(cc,{className:kt.resizeHandle,"aria-label":"Resize panels"}),f.jsx(co,{defaultSize:ae?"50%":Ge?"30%":"40%",minSize:"20%",children:f.jsx(Gv,{tabs:w,payload:k,onChange:H,validation:L,onFormat:wt,onUpload:g?$.uploadPayload:void 0,graphData:re,activeTab:O,onTabChange:Q,onGraphRenderError:De=>Y(De,"error"),onGraphDataCopySuccess:()=>Y("Graph JSON copied to clipboard!","success"),onGraphDataCopyError:()=>Y("Copy failed","error"),isGraphRefreshing:P,onClipNode:m?ve:void 0,helpPanel:y&&ae?(De,Et)=>f.jsx(lS,{activeTopic:He,onNavigate:we,onClose:()=>de(!1),onToggleMaximize:De,isMaximized:Et}):void 0})}),m&&Ge&&f.jsxs(f.Fragment,{children:[f.jsx(cc,{className:kt.resizeHandle,"aria-label":"Resize clipboard"}),f.jsx(co,{defaultSize:"20%",minSize:"10%",maxSize:"40%",children:f.jsx(H3,{connected:$.connected,onPaste:sa})})]})]})]})}function pS(){const r=xe.c(2),n=On[0].path;let s;r[0]===Symbol.for("react.memo_cache_sentinel")?(s=On.map(hS),r[0]=s):s=r[0];let u;return r[1]===Symbol.for("react.memo_cache_sentinel")?(u=f.jsx(Ty,{children:f.jsx(a3,{children:f.jsx(Rg,{children:f.jsxs(zg,{children:[s,f.jsx(ef,{path:"*",element:f.jsx(Bg,{to:n,replace:!0})})]})})})}),r[1]=u):u=r[1],u}function hS(r){return f.jsx(ef,{path:r.path,element:f.jsx(dS,{config:r},r.path)},r.path)}ey.createRoot(document.getElementById("root")).render(f.jsx(T.StrictMode,{children:f.jsx(pS,{})}));
//# sourceMappingURL=index-BwtEcQUJ.js.map
