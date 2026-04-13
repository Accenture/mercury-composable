import{j as h,T as Zp,_ as ro,H as sr,W as Ey}from"./vendor-panels-Cixz1HBJ.js";import{a as Qp,b as wy,r as x,N as Ay,R as Vp,u as Ny,B as Cy,c as jy,d as Kp,e as My}from"./vendor-router-DUFbnzxw.js";import{H as bp,P as _p,N as Dy,u as Oy,a as zy,B as Ry,b as ky,C as By,M as Uy,i as Hy}from"./vendor-xyflow-Bnghg68c.js";import{c as Gy,a as Ly,d as uo,J as vr}from"./vendor-json-view-Djmwb-hd.js";import{M as qy,r as Yy}from"./vendor-markdown-Cp1IxVgw.js";(function(){const n=document.createElement("link").relList;if(n&&n.supports&&n.supports("modulepreload"))return;for(const d of document.querySelectorAll('link[rel="modulepreload"]'))u(d);new MutationObserver(d=>{for(const p of d)if(p.type==="childList")for(const b of p.addedNodes)b.tagName==="LINK"&&b.rel==="modulepreload"&&u(b)}).observe(document,{childList:!0,subtree:!0});function s(d){const p={};return d.integrity&&(p.integrity=d.integrity),d.referrerPolicy&&(p.referrerPolicy=d.referrerPolicy),d.crossOrigin==="use-credentials"?p.credentials="include":d.crossOrigin==="anonymous"?p.credentials="omit":p.credentials="same-origin",p}function u(d){if(d.ep)return;d.ep=!0;const p=s(d);fetch(d.href,p)}})();var $c={exports:{}},so={},Wc={exports:{}},Fc={};/**
 * @license React
 * scheduler.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Sp;function Xy(){return Sp||(Sp=1,(function(c){function n(M,D){var V=M.length;M.push(D);e:for(;0<V;){var F=V-1>>>1,ee=M[F];if(0<d(ee,D))M[F]=D,M[V]=ee,V=F;else break e}}function s(M){return M.length===0?null:M[0]}function u(M){if(M.length===0)return null;var D=M[0],V=M.pop();if(V!==D){M[0]=V;e:for(var F=0,ee=M.length,ce=ee>>>1;F<ce;){var te=2*(F+1)-1,P=M[te],me=te+1,_e=M[me];if(0>d(P,V))me<ee&&0>d(_e,P)?(M[F]=_e,M[me]=V,F=me):(M[F]=P,M[te]=V,F=te);else if(me<ee&&0>d(_e,V))M[F]=_e,M[me]=V,F=me;else break e}}return D}function d(M,D){var V=M.sortIndex-D.sortIndex;return V!==0?V:M.id-D.id}if(c.unstable_now=void 0,typeof performance=="object"&&typeof performance.now=="function"){var p=performance;c.unstable_now=function(){return p.now()}}else{var b=Date,y=b.now();c.unstable_now=function(){return b.now()-y}}var m=[],g=[],A=1,v=null,T=3,N=!1,w=!1,C=!1,S=!1,O=typeof setTimeout=="function"?setTimeout:null,k=typeof clearTimeout=="function"?clearTimeout:null,H=typeof setImmediate<"u"?setImmediate:null;function L(M){for(var D=s(g);D!==null;){if(D.callback===null)u(g);else if(D.startTime<=M)u(g),D.sortIndex=D.expirationTime,n(m,D);else break;D=s(g)}}function Y(M){if(C=!1,L(M),!w)if(s(m)!==null)w=!0,q||(q=!0,ne());else{var D=s(g);D!==null&&ie(Y,D.startTime-M)}}var q=!1,B=-1,J=5,K=-1;function I(){return S?!0:!(c.unstable_now()-K<J)}function $(){if(S=!1,q){var M=c.unstable_now();K=M;var D=!0;try{e:{w=!1,C&&(C=!1,k(B),B=-1),N=!0;var V=T;try{t:{for(L(M),v=s(m);v!==null&&!(v.expirationTime>M&&I());){var F=v.callback;if(typeof F=="function"){v.callback=null,T=v.priorityLevel;var ee=F(v.expirationTime<=M);if(M=c.unstable_now(),typeof ee=="function"){v.callback=ee,L(M),D=!0;break t}v===s(m)&&u(m),L(M)}else u(m);v=s(m)}if(v!==null)D=!0;else{var ce=s(g);ce!==null&&ie(Y,ce.startTime-M),D=!1}}break e}finally{v=null,T=V,N=!1}D=void 0}}finally{D?ne():q=!1}}}var ne;if(typeof H=="function")ne=function(){H($)};else if(typeof MessageChannel<"u"){var ue=new MessageChannel,re=ue.port2;ue.port1.onmessage=$,ne=function(){re.postMessage(null)}}else ne=function(){O($,0)};function ie(M,D){B=O(function(){M(c.unstable_now())},D)}c.unstable_IdlePriority=5,c.unstable_ImmediatePriority=1,c.unstable_LowPriority=4,c.unstable_NormalPriority=3,c.unstable_Profiling=null,c.unstable_UserBlockingPriority=2,c.unstable_cancelCallback=function(M){M.callback=null},c.unstable_forceFrameRate=function(M){0>M||125<M?console.error("forceFrameRate takes a positive int between 0 and 125, forcing frame rates higher than 125 fps is not supported"):J=0<M?Math.floor(1e3/M):5},c.unstable_getCurrentPriorityLevel=function(){return T},c.unstable_next=function(M){switch(T){case 1:case 2:case 3:var D=3;break;default:D=T}var V=T;T=D;try{return M()}finally{T=V}},c.unstable_requestPaint=function(){S=!0},c.unstable_runWithPriority=function(M,D){switch(M){case 1:case 2:case 3:case 4:case 5:break;default:M=3}var V=T;T=M;try{return D()}finally{T=V}},c.unstable_scheduleCallback=function(M,D,V){var F=c.unstable_now();switch(typeof V=="object"&&V!==null?(V=V.delay,V=typeof V=="number"&&0<V?F+V:F):V=F,M){case 1:var ee=-1;break;case 2:ee=250;break;case 5:ee=1073741823;break;case 4:ee=1e4;break;default:ee=5e3}return ee=V+ee,M={id:A++,callback:D,priorityLevel:M,startTime:V,expirationTime:ee,sortIndex:-1},V>F?(M.sortIndex=V,n(g,M),s(m)===null&&M===s(g)&&(C?(k(B),B=-1):C=!0,ie(Y,V-F))):(M.sortIndex=ee,n(m,M),w||N||(w=!0,q||(q=!0,ne()))),M},c.unstable_shouldYield=I,c.unstable_wrapCallback=function(M){var D=T;return function(){var V=T;T=D;try{return M.apply(this,arguments)}finally{T=V}}}})(Fc)),Fc}var xp;function Jy(){return xp||(xp=1,Wc.exports=Xy()),Wc.exports}/**
 * @license React
 * react-dom-client.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Tp;function Zy(){if(Tp)return so;Tp=1;var c=Jy(),n=Qp(),s=wy();function u(e){var t="https://react.dev/errors/"+e;if(1<arguments.length){t+="?args[]="+encodeURIComponent(arguments[1]);for(var a=2;a<arguments.length;a++)t+="&args[]="+encodeURIComponent(arguments[a])}return"Minified React error #"+e+"; visit "+t+" for the full message or use the non-minified dev environment for full errors and additional helpful warnings."}function d(e){return!(!e||e.nodeType!==1&&e.nodeType!==9&&e.nodeType!==11)}function p(e){var t=e,a=e;if(e.alternate)for(;t.return;)t=t.return;else{e=t;do t=e,(t.flags&4098)!==0&&(a=t.return),e=t.return;while(e)}return t.tag===3?a:null}function b(e){if(e.tag===13){var t=e.memoizedState;if(t===null&&(e=e.alternate,e!==null&&(t=e.memoizedState)),t!==null)return t.dehydrated}return null}function y(e){if(e.tag===31){var t=e.memoizedState;if(t===null&&(e=e.alternate,e!==null&&(t=e.memoizedState)),t!==null)return t.dehydrated}return null}function m(e){if(p(e)!==e)throw Error(u(188))}function g(e){var t=e.alternate;if(!t){if(t=p(e),t===null)throw Error(u(188));return t!==e?null:e}for(var a=e,l=t;;){var o=a.return;if(o===null)break;var i=o.alternate;if(i===null){if(l=o.return,l!==null){a=l;continue}break}if(o.child===i.child){for(i=o.child;i;){if(i===a)return m(o),e;if(i===l)return m(o),t;i=i.sibling}throw Error(u(188))}if(a.return!==l.return)a=o,l=i;else{for(var r=!1,f=o.child;f;){if(f===a){r=!0,a=o,l=i;break}if(f===l){r=!0,l=o,a=i;break}f=f.sibling}if(!r){for(f=i.child;f;){if(f===a){r=!0,a=i,l=o;break}if(f===l){r=!0,l=i,a=o;break}f=f.sibling}if(!r)throw Error(u(189))}}if(a.alternate!==l)throw Error(u(190))}if(a.tag!==3)throw Error(u(188));return a.stateNode.current===a?e:t}function A(e){var t=e.tag;if(t===5||t===26||t===27||t===6)return e;for(e=e.child;e!==null;){if(t=A(e),t!==null)return t;e=e.sibling}return null}var v=Object.assign,T=Symbol.for("react.element"),N=Symbol.for("react.transitional.element"),w=Symbol.for("react.portal"),C=Symbol.for("react.fragment"),S=Symbol.for("react.strict_mode"),O=Symbol.for("react.profiler"),k=Symbol.for("react.consumer"),H=Symbol.for("react.context"),L=Symbol.for("react.forward_ref"),Y=Symbol.for("react.suspense"),q=Symbol.for("react.suspense_list"),B=Symbol.for("react.memo"),J=Symbol.for("react.lazy"),K=Symbol.for("react.activity"),I=Symbol.for("react.memo_cache_sentinel"),$=Symbol.iterator;function ne(e){return e===null||typeof e!="object"?null:(e=$&&e[$]||e["@@iterator"],typeof e=="function"?e:null)}var ue=Symbol.for("react.client.reference");function re(e){if(e==null)return null;if(typeof e=="function")return e.$$typeof===ue?null:e.displayName||e.name||null;if(typeof e=="string")return e;switch(e){case C:return"Fragment";case O:return"Profiler";case S:return"StrictMode";case Y:return"Suspense";case q:return"SuspenseList";case K:return"Activity"}if(typeof e=="object")switch(e.$$typeof){case w:return"Portal";case H:return e.displayName||"Context";case k:return(e._context.displayName||"Context")+".Consumer";case L:var t=e.render;return e=e.displayName,e||(e=t.displayName||t.name||"",e=e!==""?"ForwardRef("+e+")":"ForwardRef"),e;case B:return t=e.displayName||null,t!==null?t:re(e.type)||"Memo";case J:t=e._payload,e=e._init;try{return re(e(t))}catch{}}return null}var ie=Array.isArray,M=n.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,D=s.__DOM_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,V={pending:!1,data:null,method:null,action:null},F=[],ee=-1;function ce(e){return{current:e}}function te(e){0>ee||(e.current=F[ee],F[ee]=null,ee--)}function P(e,t){ee++,F[ee]=e.current,e.current=t}var me=ce(null),_e=ce(null),xe=ce(null),Be=ce(null);function qe(e,t){switch(P(xe,t),P(_e,e),P(me,null),t.nodeType){case 9:case 11:e=(e=t.documentElement)&&(e=e.namespaceURI)?Yf(e):0;break;default:if(e=t.tagName,t=t.namespaceURI)t=Yf(t),e=Xf(t,e);else switch(e){case"svg":e=1;break;case"math":e=2;break;default:e=0}}te(me),P(me,e)}function Me(){te(me),te(_e),te(xe)}function le(e){e.memoizedState!==null&&P(Be,e);var t=me.current,a=Xf(t,e.type);t!==a&&(P(_e,e),P(me,a))}function de(e){_e.current===e&&(te(me),te(_e)),Be.current===e&&(te(Be),ao._currentValue=V)}var pe,Re;function W(e){if(pe===void 0)try{throw Error()}catch(a){var t=a.stack.trim().match(/\n( *(at )?)/);pe=t&&t[1]||"",Re=-1<a.stack.indexOf(`
    at`)?" (<anonymous>)":-1<a.stack.indexOf("@")?"@unknown:0:0":""}return`
`+pe+e+Re}var ye=!1;function Te(e,t){if(!e||ye)return"";ye=!0;var a=Error.prepareStackTrace;Error.prepareStackTrace=void 0;try{var l={DetermineComponentFrameRoot:function(){try{if(t){var Q=function(){throw Error()};if(Object.defineProperty(Q.prototype,"props",{set:function(){throw Error()}}),typeof Reflect=="object"&&Reflect.construct){try{Reflect.construct(Q,[])}catch(G){var U=G}Reflect.construct(e,[],Q)}else{try{Q.call()}catch(G){U=G}e.call(Q.prototype)}}else{try{throw Error()}catch(G){U=G}(Q=e())&&typeof Q.catch=="function"&&Q.catch(function(){})}}catch(G){if(G&&U&&typeof G.stack=="string")return[G.stack,U.stack]}return[null,null]}};l.DetermineComponentFrameRoot.displayName="DetermineComponentFrameRoot";var o=Object.getOwnPropertyDescriptor(l.DetermineComponentFrameRoot,"name");o&&o.configurable&&Object.defineProperty(l.DetermineComponentFrameRoot,"name",{value:"DetermineComponentFrameRoot"});var i=l.DetermineComponentFrameRoot(),r=i[0],f=i[1];if(r&&f){var _=r.split(`
`),R=f.split(`
`);for(o=l=0;l<_.length&&!_[l].includes("DetermineComponentFrameRoot");)l++;for(;o<R.length&&!R[o].includes("DetermineComponentFrameRoot");)o++;if(l===_.length||o===R.length)for(l=_.length-1,o=R.length-1;1<=l&&0<=o&&_[l]!==R[o];)o--;for(;1<=l&&0<=o;l--,o--)if(_[l]!==R[o]){if(l!==1||o!==1)do if(l--,o--,0>o||_[l]!==R[o]){var X=`
`+_[l].replace(" at new "," at ");return e.displayName&&X.includes("<anonymous>")&&(X=X.replace("<anonymous>",e.displayName)),X}while(1<=l&&0<=o);break}}}finally{ye=!1,Error.prepareStackTrace=a}return(a=e?e.displayName||e.name:"")?W(a):""}function he(e,t){switch(e.tag){case 26:case 27:case 5:return W(e.type);case 16:return W("Lazy");case 13:return e.child!==t&&t!==null?W("Suspense Fallback"):W("Suspense");case 19:return W("SuspenseList");case 0:case 15:return Te(e.type,!1);case 11:return Te(e.type.render,!1);case 1:return Te(e.type,!0);case 31:return W("Activity");default:return""}}function De(e){try{var t="",a=null;do t+=he(e,a),a=e,e=e.return;while(e);return t}catch(l){return`
Error generating stack: `+l.message+`
`+l.stack}}var Ue=Object.prototype.hasOwnProperty,dt=c.unstable_scheduleCallback,Pe=c.unstable_cancelCallback,rt=c.unstable_shouldYield,sa=c.unstable_requestPaint,ge=c.unstable_now,fn=c.unstable_getCurrentPriorityLevel,po=c.unstable_ImmediatePriority,ho=c.unstable_UserBlockingPriority,ca=c.unstable_NormalPriority,Ri=c.unstable_LowPriority,mo=c.unstable_IdlePriority,ki=c.log,Bi=c.unstable_setDisableYieldValue,ra=null,St=null;function ln(e){if(typeof ki=="function"&&Bi(e),St&&typeof St.setStrictMode=="function")try{St.setStrictMode(ra,e)}catch{}}var xt=Math.clz32?Math.clz32:wt,Ui=Math.log,je=Math.LN2;function wt(e){return e>>>=0,e===0?32:31-(Ui(e)/je|0)|0}var et=256,Rn=262144,ja=4194304;function ua(e){var t=e&42;if(t!==0)return t;switch(e&-e){case 1:return 1;case 2:return 2;case 4:return 4;case 8:return 8;case 16:return 16;case 32:return 32;case 64:return 64;case 128:return 128;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:return e&261888;case 262144:case 524288:case 1048576:case 2097152:return e&3932160;case 4194304:case 8388608:case 16777216:case 33554432:return e&62914560;case 67108864:return 67108864;case 134217728:return 134217728;case 268435456:return 268435456;case 536870912:return 536870912;case 1073741824:return 0;default:return e}}function yo(e,t,a){var l=e.pendingLanes;if(l===0)return 0;var o=0,i=e.suspendedLanes,r=e.pingedLanes;e=e.warmLanes;var f=l&134217727;return f!==0?(l=f&~i,l!==0?o=ua(l):(r&=f,r!==0?o=ua(r):a||(a=f&~e,a!==0&&(o=ua(a))))):(f=l&~i,f!==0?o=ua(f):r!==0?o=ua(r):a||(a=l&~e,a!==0&&(o=ua(a)))),o===0?0:t!==0&&t!==o&&(t&i)===0&&(i=o&-o,a=t&-t,i>=a||i===32&&(a&4194048)!==0)?t:o}function yl(e,t){return(e.pendingLanes&~(e.suspendedLanes&~e.pingedLanes)&t)===0}function dh(e,t){switch(e){case 1:case 2:case 4:case 8:case 64:return t+250;case 16:case 32:case 128:case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:return t+5e3;case 4194304:case 8388608:case 16777216:case 33554432:return-1;case 67108864:case 134217728:case 268435456:case 536870912:case 1073741824:return-1;default:return-1}}function xr(){var e=ja;return ja<<=1,(ja&62914560)===0&&(ja=4194304),e}function Hi(e){for(var t=[],a=0;31>a;a++)t.push(e);return t}function gl(e,t){e.pendingLanes|=t,t!==268435456&&(e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0)}function fh(e,t,a,l,o,i){var r=e.pendingLanes;e.pendingLanes=a,e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0,e.expiredLanes&=a,e.entangledLanes&=a,e.errorRecoveryDisabledLanes&=a,e.shellSuspendCounter=0;var f=e.entanglements,_=e.expirationTimes,R=e.hiddenUpdates;for(a=r&~a;0<a;){var X=31-xt(a),Q=1<<X;f[X]=0,_[X]=-1;var U=R[X];if(U!==null)for(R[X]=null,X=0;X<U.length;X++){var G=U[X];G!==null&&(G.lane&=-536870913)}a&=~Q}l!==0&&Tr(e,l,0),i!==0&&o===0&&e.tag!==0&&(e.suspendedLanes|=i&~(r&~t))}function Tr(e,t,a){e.pendingLanes|=t,e.suspendedLanes&=~t;var l=31-xt(t);e.entangledLanes|=t,e.entanglements[l]=e.entanglements[l]|1073741824|a&261930}function Er(e,t){var a=e.entangledLanes|=t;for(e=e.entanglements;a;){var l=31-xt(a),o=1<<l;o&t|e[l]&t&&(e[l]|=t),a&=~o}}function wr(e,t){var a=t&-t;return a=(a&42)!==0?1:Gi(a),(a&(e.suspendedLanes|t))!==0?0:a}function Gi(e){switch(e){case 2:e=1;break;case 8:e=4;break;case 32:e=16;break;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:case 4194304:case 8388608:case 16777216:case 33554432:e=128;break;case 268435456:e=134217728;break;default:e=0}return e}function Li(e){return e&=-e,2<e?8<e?(e&134217727)!==0?32:268435456:8:2}function Ar(){var e=D.p;return e!==0?e:(e=window.event,e===void 0?32:fp(e.type))}function Nr(e,t){var a=D.p;try{return D.p=e,t()}finally{D.p=a}}var kn=Math.random().toString(36).slice(2),mt="__reactFiber$"+kn,At="__reactProps$"+kn,Ma="__reactContainer$"+kn,qi="__reactEvents$"+kn,ph="__reactListeners$"+kn,hh="__reactHandles$"+kn,Cr="__reactResources$"+kn,vl="__reactMarker$"+kn;function Yi(e){delete e[mt],delete e[At],delete e[qi],delete e[ph],delete e[hh]}function Da(e){var t=e[mt];if(t)return t;for(var a=e.parentNode;a;){if(t=a[Ma]||a[mt]){if(a=t.alternate,t.child!==null||a!==null&&a.child!==null)for(e=$f(e);e!==null;){if(a=e[mt])return a;e=$f(e)}return t}e=a,a=e.parentNode}return null}function Oa(e){if(e=e[mt]||e[Ma]){var t=e.tag;if(t===5||t===6||t===13||t===31||t===26||t===27||t===3)return e}return null}function bl(e){var t=e.tag;if(t===5||t===26||t===27||t===6)return e.stateNode;throw Error(u(33))}function za(e){var t=e[Cr];return t||(t=e[Cr]={hoistableStyles:new Map,hoistableScripts:new Map}),t}function ft(e){e[vl]=!0}var jr=new Set,Mr={};function da(e,t){Ra(e,t),Ra(e+"Capture",t)}function Ra(e,t){for(Mr[e]=t,e=0;e<t.length;e++)jr.add(t[e])}var mh=RegExp("^[:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD][:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD\\-.0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040]*$"),Dr={},Or={};function yh(e){return Ue.call(Or,e)?!0:Ue.call(Dr,e)?!1:mh.test(e)?Or[e]=!0:(Dr[e]=!0,!1)}function go(e,t,a){if(yh(t))if(a===null)e.removeAttribute(t);else{switch(typeof a){case"undefined":case"function":case"symbol":e.removeAttribute(t);return;case"boolean":var l=t.toLowerCase().slice(0,5);if(l!=="data-"&&l!=="aria-"){e.removeAttribute(t);return}}e.setAttribute(t,""+a)}}function vo(e,t,a){if(a===null)e.removeAttribute(t);else{switch(typeof a){case"undefined":case"function":case"symbol":case"boolean":e.removeAttribute(t);return}e.setAttribute(t,""+a)}}function pn(e,t,a,l){if(l===null)e.removeAttribute(a);else{switch(typeof l){case"undefined":case"function":case"symbol":case"boolean":e.removeAttribute(a);return}e.setAttributeNS(t,a,""+l)}}function Xt(e){switch(typeof e){case"bigint":case"boolean":case"number":case"string":case"undefined":return e;case"object":return e;default:return""}}function zr(e){var t=e.type;return(e=e.nodeName)&&e.toLowerCase()==="input"&&(t==="checkbox"||t==="radio")}function gh(e,t,a){var l=Object.getOwnPropertyDescriptor(e.constructor.prototype,t);if(!e.hasOwnProperty(t)&&typeof l<"u"&&typeof l.get=="function"&&typeof l.set=="function"){var o=l.get,i=l.set;return Object.defineProperty(e,t,{configurable:!0,get:function(){return o.call(this)},set:function(r){a=""+r,i.call(this,r)}}),Object.defineProperty(e,t,{enumerable:l.enumerable}),{getValue:function(){return a},setValue:function(r){a=""+r},stopTracking:function(){e._valueTracker=null,delete e[t]}}}}function Xi(e){if(!e._valueTracker){var t=zr(e)?"checked":"value";e._valueTracker=gh(e,t,""+e[t])}}function Rr(e){if(!e)return!1;var t=e._valueTracker;if(!t)return!0;var a=t.getValue(),l="";return e&&(l=zr(e)?e.checked?"true":"false":e.value),e=l,e!==a?(t.setValue(e),!0):!1}function bo(e){if(e=e||(typeof document<"u"?document:void 0),typeof e>"u")return null;try{return e.activeElement||e.body}catch{return e.body}}var vh=/[\n"\\]/g;function Jt(e){return e.replace(vh,function(t){return"\\"+t.charCodeAt(0).toString(16)+" "})}function Ji(e,t,a,l,o,i,r,f){e.name="",r!=null&&typeof r!="function"&&typeof r!="symbol"&&typeof r!="boolean"?e.type=r:e.removeAttribute("type"),t!=null?r==="number"?(t===0&&e.value===""||e.value!=t)&&(e.value=""+Xt(t)):e.value!==""+Xt(t)&&(e.value=""+Xt(t)):r!=="submit"&&r!=="reset"||e.removeAttribute("value"),t!=null?Zi(e,r,Xt(t)):a!=null?Zi(e,r,Xt(a)):l!=null&&e.removeAttribute("value"),o==null&&i!=null&&(e.defaultChecked=!!i),o!=null&&(e.checked=o&&typeof o!="function"&&typeof o!="symbol"),f!=null&&typeof f!="function"&&typeof f!="symbol"&&typeof f!="boolean"?e.name=""+Xt(f):e.removeAttribute("name")}function kr(e,t,a,l,o,i,r,f){if(i!=null&&typeof i!="function"&&typeof i!="symbol"&&typeof i!="boolean"&&(e.type=i),t!=null||a!=null){if(!(i!=="submit"&&i!=="reset"||t!=null)){Xi(e);return}a=a!=null?""+Xt(a):"",t=t!=null?""+Xt(t):a,f||t===e.value||(e.value=t),e.defaultValue=t}l=l??o,l=typeof l!="function"&&typeof l!="symbol"&&!!l,e.checked=f?e.checked:!!l,e.defaultChecked=!!l,r!=null&&typeof r!="function"&&typeof r!="symbol"&&typeof r!="boolean"&&(e.name=r),Xi(e)}function Zi(e,t,a){t==="number"&&bo(e.ownerDocument)===e||e.defaultValue===""+a||(e.defaultValue=""+a)}function ka(e,t,a,l){if(e=e.options,t){t={};for(var o=0;o<a.length;o++)t["$"+a[o]]=!0;for(a=0;a<e.length;a++)o=t.hasOwnProperty("$"+e[a].value),e[a].selected!==o&&(e[a].selected=o),o&&l&&(e[a].defaultSelected=!0)}else{for(a=""+Xt(a),t=null,o=0;o<e.length;o++){if(e[o].value===a){e[o].selected=!0,l&&(e[o].defaultSelected=!0);return}t!==null||e[o].disabled||(t=e[o])}t!==null&&(t.selected=!0)}}function Br(e,t,a){if(t!=null&&(t=""+Xt(t),t!==e.value&&(e.value=t),a==null)){e.defaultValue!==t&&(e.defaultValue=t);return}e.defaultValue=a!=null?""+Xt(a):""}function Ur(e,t,a,l){if(t==null){if(l!=null){if(a!=null)throw Error(u(92));if(ie(l)){if(1<l.length)throw Error(u(93));l=l[0]}a=l}a==null&&(a=""),t=a}a=Xt(t),e.defaultValue=a,l=e.textContent,l===a&&l!==""&&l!==null&&(e.value=l),Xi(e)}function Ba(e,t){if(t){var a=e.firstChild;if(a&&a===e.lastChild&&a.nodeType===3){a.nodeValue=t;return}}e.textContent=t}var bh=new Set("animationIterationCount aspectRatio borderImageOutset borderImageSlice borderImageWidth boxFlex boxFlexGroup boxOrdinalGroup columnCount columns flex flexGrow flexPositive flexShrink flexNegative flexOrder gridArea gridRow gridRowEnd gridRowSpan gridRowStart gridColumn gridColumnEnd gridColumnSpan gridColumnStart fontWeight lineClamp lineHeight opacity order orphans scale tabSize widows zIndex zoom fillOpacity floodOpacity stopOpacity strokeDasharray strokeDashoffset strokeMiterlimit strokeOpacity strokeWidth MozAnimationIterationCount MozBoxFlex MozBoxFlexGroup MozLineClamp msAnimationIterationCount msFlex msZoom msFlexGrow msFlexNegative msFlexOrder msFlexPositive msFlexShrink msGridColumn msGridColumnSpan msGridRow msGridRowSpan WebkitAnimationIterationCount WebkitBoxFlex WebKitBoxFlexGroup WebkitBoxOrdinalGroup WebkitColumnCount WebkitColumns WebkitFlex WebkitFlexGrow WebkitFlexPositive WebkitFlexShrink WebkitLineClamp".split(" "));function Hr(e,t,a){var l=t.indexOf("--")===0;a==null||typeof a=="boolean"||a===""?l?e.setProperty(t,""):t==="float"?e.cssFloat="":e[t]="":l?e.setProperty(t,a):typeof a!="number"||a===0||bh.has(t)?t==="float"?e.cssFloat=a:e[t]=(""+a).trim():e[t]=a+"px"}function Gr(e,t,a){if(t!=null&&typeof t!="object")throw Error(u(62));if(e=e.style,a!=null){for(var l in a)!a.hasOwnProperty(l)||t!=null&&t.hasOwnProperty(l)||(l.indexOf("--")===0?e.setProperty(l,""):l==="float"?e.cssFloat="":e[l]="");for(var o in t)l=t[o],t.hasOwnProperty(o)&&a[o]!==l&&Hr(e,o,l)}else for(var i in t)t.hasOwnProperty(i)&&Hr(e,i,t[i])}function Qi(e){if(e.indexOf("-")===-1)return!1;switch(e){case"annotation-xml":case"color-profile":case"font-face":case"font-face-src":case"font-face-uri":case"font-face-format":case"font-face-name":case"missing-glyph":return!1;default:return!0}}var _h=new Map([["acceptCharset","accept-charset"],["htmlFor","for"],["httpEquiv","http-equiv"],["crossOrigin","crossorigin"],["accentHeight","accent-height"],["alignmentBaseline","alignment-baseline"],["arabicForm","arabic-form"],["baselineShift","baseline-shift"],["capHeight","cap-height"],["clipPath","clip-path"],["clipRule","clip-rule"],["colorInterpolation","color-interpolation"],["colorInterpolationFilters","color-interpolation-filters"],["colorProfile","color-profile"],["colorRendering","color-rendering"],["dominantBaseline","dominant-baseline"],["enableBackground","enable-background"],["fillOpacity","fill-opacity"],["fillRule","fill-rule"],["floodColor","flood-color"],["floodOpacity","flood-opacity"],["fontFamily","font-family"],["fontSize","font-size"],["fontSizeAdjust","font-size-adjust"],["fontStretch","font-stretch"],["fontStyle","font-style"],["fontVariant","font-variant"],["fontWeight","font-weight"],["glyphName","glyph-name"],["glyphOrientationHorizontal","glyph-orientation-horizontal"],["glyphOrientationVertical","glyph-orientation-vertical"],["horizAdvX","horiz-adv-x"],["horizOriginX","horiz-origin-x"],["imageRendering","image-rendering"],["letterSpacing","letter-spacing"],["lightingColor","lighting-color"],["markerEnd","marker-end"],["markerMid","marker-mid"],["markerStart","marker-start"],["overlinePosition","overline-position"],["overlineThickness","overline-thickness"],["paintOrder","paint-order"],["panose-1","panose-1"],["pointerEvents","pointer-events"],["renderingIntent","rendering-intent"],["shapeRendering","shape-rendering"],["stopColor","stop-color"],["stopOpacity","stop-opacity"],["strikethroughPosition","strikethrough-position"],["strikethroughThickness","strikethrough-thickness"],["strokeDasharray","stroke-dasharray"],["strokeDashoffset","stroke-dashoffset"],["strokeLinecap","stroke-linecap"],["strokeLinejoin","stroke-linejoin"],["strokeMiterlimit","stroke-miterlimit"],["strokeOpacity","stroke-opacity"],["strokeWidth","stroke-width"],["textAnchor","text-anchor"],["textDecoration","text-decoration"],["textRendering","text-rendering"],["transformOrigin","transform-origin"],["underlinePosition","underline-position"],["underlineThickness","underline-thickness"],["unicodeBidi","unicode-bidi"],["unicodeRange","unicode-range"],["unitsPerEm","units-per-em"],["vAlphabetic","v-alphabetic"],["vHanging","v-hanging"],["vIdeographic","v-ideographic"],["vMathematical","v-mathematical"],["vectorEffect","vector-effect"],["vertAdvY","vert-adv-y"],["vertOriginX","vert-origin-x"],["vertOriginY","vert-origin-y"],["wordSpacing","word-spacing"],["writingMode","writing-mode"],["xmlnsXlink","xmlns:xlink"],["xHeight","x-height"]]),Sh=/^[\u0000-\u001F ]*j[\r\n\t]*a[\r\n\t]*v[\r\n\t]*a[\r\n\t]*s[\r\n\t]*c[\r\n\t]*r[\r\n\t]*i[\r\n\t]*p[\r\n\t]*t[\r\n\t]*:/i;function _o(e){return Sh.test(""+e)?"javascript:throw new Error('React has blocked a javascript: URL as a security precaution.')":e}function hn(){}var Vi=null;function Ki(e){return e=e.target||e.srcElement||window,e.correspondingUseElement&&(e=e.correspondingUseElement),e.nodeType===3?e.parentNode:e}var Ua=null,Ha=null;function Lr(e){var t=Oa(e);if(t&&(e=t.stateNode)){var a=e[At]||null;e:switch(e=t.stateNode,t.type){case"input":if(Ji(e,a.value,a.defaultValue,a.defaultValue,a.checked,a.defaultChecked,a.type,a.name),t=a.name,a.type==="radio"&&t!=null){for(a=e;a.parentNode;)a=a.parentNode;for(a=a.querySelectorAll('input[name="'+Jt(""+t)+'"][type="radio"]'),t=0;t<a.length;t++){var l=a[t];if(l!==e&&l.form===e.form){var o=l[At]||null;if(!o)throw Error(u(90));Ji(l,o.value,o.defaultValue,o.defaultValue,o.checked,o.defaultChecked,o.type,o.name)}}for(t=0;t<a.length;t++)l=a[t],l.form===e.form&&Rr(l)}break e;case"textarea":Br(e,a.value,a.defaultValue);break e;case"select":t=a.value,t!=null&&ka(e,!!a.multiple,t,!1)}}}var Ii=!1;function qr(e,t,a){if(Ii)return e(t,a);Ii=!0;try{var l=e(t);return l}finally{if(Ii=!1,(Ua!==null||Ha!==null)&&(si(),Ua&&(t=Ua,e=Ha,Ha=Ua=null,Lr(t),e)))for(t=0;t<e.length;t++)Lr(e[t])}}function _l(e,t){var a=e.stateNode;if(a===null)return null;var l=a[At]||null;if(l===null)return null;a=l[t];e:switch(t){case"onClick":case"onClickCapture":case"onDoubleClick":case"onDoubleClickCapture":case"onMouseDown":case"onMouseDownCapture":case"onMouseMove":case"onMouseMoveCapture":case"onMouseUp":case"onMouseUpCapture":case"onMouseEnter":(l=!l.disabled)||(e=e.type,l=!(e==="button"||e==="input"||e==="select"||e==="textarea")),e=!l;break e;default:e=!1}if(e)return null;if(a&&typeof a!="function")throw Error(u(231,t,typeof a));return a}var mn=!(typeof window>"u"||typeof window.document>"u"||typeof window.document.createElement>"u"),$i=!1;if(mn)try{var Sl={};Object.defineProperty(Sl,"passive",{get:function(){$i=!0}}),window.addEventListener("test",Sl,Sl),window.removeEventListener("test",Sl,Sl)}catch{$i=!1}var Bn=null,Wi=null,So=null;function Yr(){if(So)return So;var e,t=Wi,a=t.length,l,o="value"in Bn?Bn.value:Bn.textContent,i=o.length;for(e=0;e<a&&t[e]===o[e];e++);var r=a-e;for(l=1;l<=r&&t[a-l]===o[i-l];l++);return So=o.slice(e,1<l?1-l:void 0)}function xo(e){var t=e.keyCode;return"charCode"in e?(e=e.charCode,e===0&&t===13&&(e=13)):e=t,e===10&&(e=13),32<=e||e===13?e:0}function To(){return!0}function Xr(){return!1}function Nt(e){function t(a,l,o,i,r){this._reactName=a,this._targetInst=o,this.type=l,this.nativeEvent=i,this.target=r,this.currentTarget=null;for(var f in e)e.hasOwnProperty(f)&&(a=e[f],this[f]=a?a(i):i[f]);return this.isDefaultPrevented=(i.defaultPrevented!=null?i.defaultPrevented:i.returnValue===!1)?To:Xr,this.isPropagationStopped=Xr,this}return v(t.prototype,{preventDefault:function(){this.defaultPrevented=!0;var a=this.nativeEvent;a&&(a.preventDefault?a.preventDefault():typeof a.returnValue!="unknown"&&(a.returnValue=!1),this.isDefaultPrevented=To)},stopPropagation:function(){var a=this.nativeEvent;a&&(a.stopPropagation?a.stopPropagation():typeof a.cancelBubble!="unknown"&&(a.cancelBubble=!0),this.isPropagationStopped=To)},persist:function(){},isPersistent:To}),t}var fa={eventPhase:0,bubbles:0,cancelable:0,timeStamp:function(e){return e.timeStamp||Date.now()},defaultPrevented:0,isTrusted:0},Eo=Nt(fa),xl=v({},fa,{view:0,detail:0}),xh=Nt(xl),Fi,Pi,Tl,wo=v({},xl,{screenX:0,screenY:0,clientX:0,clientY:0,pageX:0,pageY:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,getModifierState:ts,button:0,buttons:0,relatedTarget:function(e){return e.relatedTarget===void 0?e.fromElement===e.srcElement?e.toElement:e.fromElement:e.relatedTarget},movementX:function(e){return"movementX"in e?e.movementX:(e!==Tl&&(Tl&&e.type==="mousemove"?(Fi=e.screenX-Tl.screenX,Pi=e.screenY-Tl.screenY):Pi=Fi=0,Tl=e),Fi)},movementY:function(e){return"movementY"in e?e.movementY:Pi}}),Jr=Nt(wo),Th=v({},wo,{dataTransfer:0}),Eh=Nt(Th),wh=v({},xl,{relatedTarget:0}),es=Nt(wh),Ah=v({},fa,{animationName:0,elapsedTime:0,pseudoElement:0}),Nh=Nt(Ah),Ch=v({},fa,{clipboardData:function(e){return"clipboardData"in e?e.clipboardData:window.clipboardData}}),jh=Nt(Ch),Mh=v({},fa,{data:0}),Zr=Nt(Mh),Dh={Esc:"Escape",Spacebar:" ",Left:"ArrowLeft",Up:"ArrowUp",Right:"ArrowRight",Down:"ArrowDown",Del:"Delete",Win:"OS",Menu:"ContextMenu",Apps:"ContextMenu",Scroll:"ScrollLock",MozPrintableKey:"Unidentified"},Oh={8:"Backspace",9:"Tab",12:"Clear",13:"Enter",16:"Shift",17:"Control",18:"Alt",19:"Pause",20:"CapsLock",27:"Escape",32:" ",33:"PageUp",34:"PageDown",35:"End",36:"Home",37:"ArrowLeft",38:"ArrowUp",39:"ArrowRight",40:"ArrowDown",45:"Insert",46:"Delete",112:"F1",113:"F2",114:"F3",115:"F4",116:"F5",117:"F6",118:"F7",119:"F8",120:"F9",121:"F10",122:"F11",123:"F12",144:"NumLock",145:"ScrollLock",224:"Meta"},zh={Alt:"altKey",Control:"ctrlKey",Meta:"metaKey",Shift:"shiftKey"};function Rh(e){var t=this.nativeEvent;return t.getModifierState?t.getModifierState(e):(e=zh[e])?!!t[e]:!1}function ts(){return Rh}var kh=v({},xl,{key:function(e){if(e.key){var t=Dh[e.key]||e.key;if(t!=="Unidentified")return t}return e.type==="keypress"?(e=xo(e),e===13?"Enter":String.fromCharCode(e)):e.type==="keydown"||e.type==="keyup"?Oh[e.keyCode]||"Unidentified":""},code:0,location:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,repeat:0,locale:0,getModifierState:ts,charCode:function(e){return e.type==="keypress"?xo(e):0},keyCode:function(e){return e.type==="keydown"||e.type==="keyup"?e.keyCode:0},which:function(e){return e.type==="keypress"?xo(e):e.type==="keydown"||e.type==="keyup"?e.keyCode:0}}),Bh=Nt(kh),Uh=v({},wo,{pointerId:0,width:0,height:0,pressure:0,tangentialPressure:0,tiltX:0,tiltY:0,twist:0,pointerType:0,isPrimary:0}),Qr=Nt(Uh),Hh=v({},xl,{touches:0,targetTouches:0,changedTouches:0,altKey:0,metaKey:0,ctrlKey:0,shiftKey:0,getModifierState:ts}),Gh=Nt(Hh),Lh=v({},fa,{propertyName:0,elapsedTime:0,pseudoElement:0}),qh=Nt(Lh),Yh=v({},wo,{deltaX:function(e){return"deltaX"in e?e.deltaX:"wheelDeltaX"in e?-e.wheelDeltaX:0},deltaY:function(e){return"deltaY"in e?e.deltaY:"wheelDeltaY"in e?-e.wheelDeltaY:"wheelDelta"in e?-e.wheelDelta:0},deltaZ:0,deltaMode:0}),Xh=Nt(Yh),Jh=v({},fa,{newState:0,oldState:0}),Zh=Nt(Jh),Qh=[9,13,27,32],ns=mn&&"CompositionEvent"in window,El=null;mn&&"documentMode"in document&&(El=document.documentMode);var Vh=mn&&"TextEvent"in window&&!El,Vr=mn&&(!ns||El&&8<El&&11>=El),Kr=" ",Ir=!1;function $r(e,t){switch(e){case"keyup":return Qh.indexOf(t.keyCode)!==-1;case"keydown":return t.keyCode!==229;case"keypress":case"mousedown":case"focusout":return!0;default:return!1}}function Wr(e){return e=e.detail,typeof e=="object"&&"data"in e?e.data:null}var Ga=!1;function Kh(e,t){switch(e){case"compositionend":return Wr(t);case"keypress":return t.which!==32?null:(Ir=!0,Kr);case"textInput":return e=t.data,e===Kr&&Ir?null:e;default:return null}}function Ih(e,t){if(Ga)return e==="compositionend"||!ns&&$r(e,t)?(e=Yr(),So=Wi=Bn=null,Ga=!1,e):null;switch(e){case"paste":return null;case"keypress":if(!(t.ctrlKey||t.altKey||t.metaKey)||t.ctrlKey&&t.altKey){if(t.char&&1<t.char.length)return t.char;if(t.which)return String.fromCharCode(t.which)}return null;case"compositionend":return Vr&&t.locale!=="ko"?null:t.data;default:return null}}var $h={color:!0,date:!0,datetime:!0,"datetime-local":!0,email:!0,month:!0,number:!0,password:!0,range:!0,search:!0,tel:!0,text:!0,time:!0,url:!0,week:!0};function Fr(e){var t=e&&e.nodeName&&e.nodeName.toLowerCase();return t==="input"?!!$h[e.type]:t==="textarea"}function Pr(e,t,a,l){Ua?Ha?Ha.push(l):Ha=[l]:Ua=l,t=hi(t,"onChange"),0<t.length&&(a=new Eo("onChange","change",null,a,l),e.push({event:a,listeners:t}))}var wl=null,Al=null;function Wh(e){Bf(e,0)}function Ao(e){var t=bl(e);if(Rr(t))return e}function eu(e,t){if(e==="change")return t}var tu=!1;if(mn){var as;if(mn){var ls="oninput"in document;if(!ls){var nu=document.createElement("div");nu.setAttribute("oninput","return;"),ls=typeof nu.oninput=="function"}as=ls}else as=!1;tu=as&&(!document.documentMode||9<document.documentMode)}function au(){wl&&(wl.detachEvent("onpropertychange",lu),Al=wl=null)}function lu(e){if(e.propertyName==="value"&&Ao(Al)){var t=[];Pr(t,Al,e,Ki(e)),qr(Wh,t)}}function Fh(e,t,a){e==="focusin"?(au(),wl=t,Al=a,wl.attachEvent("onpropertychange",lu)):e==="focusout"&&au()}function Ph(e){if(e==="selectionchange"||e==="keyup"||e==="keydown")return Ao(Al)}function em(e,t){if(e==="click")return Ao(t)}function tm(e,t){if(e==="input"||e==="change")return Ao(t)}function nm(e,t){return e===t&&(e!==0||1/e===1/t)||e!==e&&t!==t}var Rt=typeof Object.is=="function"?Object.is:nm;function Nl(e,t){if(Rt(e,t))return!0;if(typeof e!="object"||e===null||typeof t!="object"||t===null)return!1;var a=Object.keys(e),l=Object.keys(t);if(a.length!==l.length)return!1;for(l=0;l<a.length;l++){var o=a[l];if(!Ue.call(t,o)||!Rt(e[o],t[o]))return!1}return!0}function ou(e){for(;e&&e.firstChild;)e=e.firstChild;return e}function iu(e,t){var a=ou(e);e=0;for(var l;a;){if(a.nodeType===3){if(l=e+a.textContent.length,e<=t&&l>=t)return{node:a,offset:t-e};e=l}e:{for(;a;){if(a.nextSibling){a=a.nextSibling;break e}a=a.parentNode}a=void 0}a=ou(a)}}function su(e,t){return e&&t?e===t?!0:e&&e.nodeType===3?!1:t&&t.nodeType===3?su(e,t.parentNode):"contains"in e?e.contains(t):e.compareDocumentPosition?!!(e.compareDocumentPosition(t)&16):!1:!1}function cu(e){e=e!=null&&e.ownerDocument!=null&&e.ownerDocument.defaultView!=null?e.ownerDocument.defaultView:window;for(var t=bo(e.document);t instanceof e.HTMLIFrameElement;){try{var a=typeof t.contentWindow.location.href=="string"}catch{a=!1}if(a)e=t.contentWindow;else break;t=bo(e.document)}return t}function os(e){var t=e&&e.nodeName&&e.nodeName.toLowerCase();return t&&(t==="input"&&(e.type==="text"||e.type==="search"||e.type==="tel"||e.type==="url"||e.type==="password")||t==="textarea"||e.contentEditable==="true")}var am=mn&&"documentMode"in document&&11>=document.documentMode,La=null,is=null,Cl=null,ss=!1;function ru(e,t,a){var l=a.window===a?a.document:a.nodeType===9?a:a.ownerDocument;ss||La==null||La!==bo(l)||(l=La,"selectionStart"in l&&os(l)?l={start:l.selectionStart,end:l.selectionEnd}:(l=(l.ownerDocument&&l.ownerDocument.defaultView||window).getSelection(),l={anchorNode:l.anchorNode,anchorOffset:l.anchorOffset,focusNode:l.focusNode,focusOffset:l.focusOffset}),Cl&&Nl(Cl,l)||(Cl=l,l=hi(is,"onSelect"),0<l.length&&(t=new Eo("onSelect","select",null,t,a),e.push({event:t,listeners:l}),t.target=La)))}function pa(e,t){var a={};return a[e.toLowerCase()]=t.toLowerCase(),a["Webkit"+e]="webkit"+t,a["Moz"+e]="moz"+t,a}var qa={animationend:pa("Animation","AnimationEnd"),animationiteration:pa("Animation","AnimationIteration"),animationstart:pa("Animation","AnimationStart"),transitionrun:pa("Transition","TransitionRun"),transitionstart:pa("Transition","TransitionStart"),transitioncancel:pa("Transition","TransitionCancel"),transitionend:pa("Transition","TransitionEnd")},cs={},uu={};mn&&(uu=document.createElement("div").style,"AnimationEvent"in window||(delete qa.animationend.animation,delete qa.animationiteration.animation,delete qa.animationstart.animation),"TransitionEvent"in window||delete qa.transitionend.transition);function ha(e){if(cs[e])return cs[e];if(!qa[e])return e;var t=qa[e],a;for(a in t)if(t.hasOwnProperty(a)&&a in uu)return cs[e]=t[a];return e}var du=ha("animationend"),fu=ha("animationiteration"),pu=ha("animationstart"),lm=ha("transitionrun"),om=ha("transitionstart"),im=ha("transitioncancel"),hu=ha("transitionend"),mu=new Map,rs="abort auxClick beforeToggle cancel canPlay canPlayThrough click close contextMenu copy cut drag dragEnd dragEnter dragExit dragLeave dragOver dragStart drop durationChange emptied encrypted ended error gotPointerCapture input invalid keyDown keyPress keyUp load loadedData loadedMetadata loadStart lostPointerCapture mouseDown mouseMove mouseOut mouseOver mouseUp paste pause play playing pointerCancel pointerDown pointerMove pointerOut pointerOver pointerUp progress rateChange reset resize seeked seeking stalled submit suspend timeUpdate touchCancel touchEnd touchStart volumeChange scroll toggle touchMove waiting wheel".split(" ");rs.push("scrollEnd");function en(e,t){mu.set(e,t),da(t,[e])}var No=typeof reportError=="function"?reportError:function(e){if(typeof window=="object"&&typeof window.ErrorEvent=="function"){var t=new window.ErrorEvent("error",{bubbles:!0,cancelable:!0,message:typeof e=="object"&&e!==null&&typeof e.message=="string"?String(e.message):String(e),error:e});if(!window.dispatchEvent(t))return}else if(typeof process=="object"&&typeof process.emit=="function"){process.emit("uncaughtException",e);return}console.error(e)},Zt=[],Ya=0,us=0;function Co(){for(var e=Ya,t=us=Ya=0;t<e;){var a=Zt[t];Zt[t++]=null;var l=Zt[t];Zt[t++]=null;var o=Zt[t];Zt[t++]=null;var i=Zt[t];if(Zt[t++]=null,l!==null&&o!==null){var r=l.pending;r===null?o.next=o:(o.next=r.next,r.next=o),l.pending=o}i!==0&&yu(a,o,i)}}function jo(e,t,a,l){Zt[Ya++]=e,Zt[Ya++]=t,Zt[Ya++]=a,Zt[Ya++]=l,us|=l,e.lanes|=l,e=e.alternate,e!==null&&(e.lanes|=l)}function ds(e,t,a,l){return jo(e,t,a,l),Mo(e)}function ma(e,t){return jo(e,null,null,t),Mo(e)}function yu(e,t,a){e.lanes|=a;var l=e.alternate;l!==null&&(l.lanes|=a);for(var o=!1,i=e.return;i!==null;)i.childLanes|=a,l=i.alternate,l!==null&&(l.childLanes|=a),i.tag===22&&(e=i.stateNode,e===null||e._visibility&1||(o=!0)),e=i,i=i.return;return e.tag===3?(i=e.stateNode,o&&t!==null&&(o=31-xt(a),e=i.hiddenUpdates,l=e[o],l===null?e[o]=[t]:l.push(t),t.lane=a|536870912),i):null}function Mo(e){if(50<$l)throw $l=0,_c=null,Error(u(185));for(var t=e.return;t!==null;)e=t,t=e.return;return e.tag===3?e.stateNode:null}var Xa={};function sm(e,t,a,l){this.tag=e,this.key=a,this.sibling=this.child=this.return=this.stateNode=this.type=this.elementType=null,this.index=0,this.refCleanup=this.ref=null,this.pendingProps=t,this.dependencies=this.memoizedState=this.updateQueue=this.memoizedProps=null,this.mode=l,this.subtreeFlags=this.flags=0,this.deletions=null,this.childLanes=this.lanes=0,this.alternate=null}function kt(e,t,a,l){return new sm(e,t,a,l)}function fs(e){return e=e.prototype,!(!e||!e.isReactComponent)}function yn(e,t){var a=e.alternate;return a===null?(a=kt(e.tag,t,e.key,e.mode),a.elementType=e.elementType,a.type=e.type,a.stateNode=e.stateNode,a.alternate=e,e.alternate=a):(a.pendingProps=t,a.type=e.type,a.flags=0,a.subtreeFlags=0,a.deletions=null),a.flags=e.flags&65011712,a.childLanes=e.childLanes,a.lanes=e.lanes,a.child=e.child,a.memoizedProps=e.memoizedProps,a.memoizedState=e.memoizedState,a.updateQueue=e.updateQueue,t=e.dependencies,a.dependencies=t===null?null:{lanes:t.lanes,firstContext:t.firstContext},a.sibling=e.sibling,a.index=e.index,a.ref=e.ref,a.refCleanup=e.refCleanup,a}function gu(e,t){e.flags&=65011714;var a=e.alternate;return a===null?(e.childLanes=0,e.lanes=t,e.child=null,e.subtreeFlags=0,e.memoizedProps=null,e.memoizedState=null,e.updateQueue=null,e.dependencies=null,e.stateNode=null):(e.childLanes=a.childLanes,e.lanes=a.lanes,e.child=a.child,e.subtreeFlags=0,e.deletions=null,e.memoizedProps=a.memoizedProps,e.memoizedState=a.memoizedState,e.updateQueue=a.updateQueue,e.type=a.type,t=a.dependencies,e.dependencies=t===null?null:{lanes:t.lanes,firstContext:t.firstContext}),e}function Do(e,t,a,l,o,i){var r=0;if(l=e,typeof e=="function")fs(e)&&(r=1);else if(typeof e=="string")r=fy(e,a,me.current)?26:e==="html"||e==="head"||e==="body"?27:5;else e:switch(e){case K:return e=kt(31,a,t,o),e.elementType=K,e.lanes=i,e;case C:return ya(a.children,o,i,t);case S:r=8,o|=24;break;case O:return e=kt(12,a,t,o|2),e.elementType=O,e.lanes=i,e;case Y:return e=kt(13,a,t,o),e.elementType=Y,e.lanes=i,e;case q:return e=kt(19,a,t,o),e.elementType=q,e.lanes=i,e;default:if(typeof e=="object"&&e!==null)switch(e.$$typeof){case H:r=10;break e;case k:r=9;break e;case L:r=11;break e;case B:r=14;break e;case J:r=16,l=null;break e}r=29,a=Error(u(130,e===null?"null":typeof e,"")),l=null}return t=kt(r,a,t,o),t.elementType=e,t.type=l,t.lanes=i,t}function ya(e,t,a,l){return e=kt(7,e,l,t),e.lanes=a,e}function ps(e,t,a){return e=kt(6,e,null,t),e.lanes=a,e}function vu(e){var t=kt(18,null,null,0);return t.stateNode=e,t}function hs(e,t,a){return t=kt(4,e.children!==null?e.children:[],e.key,t),t.lanes=a,t.stateNode={containerInfo:e.containerInfo,pendingChildren:null,implementation:e.implementation},t}var bu=new WeakMap;function Qt(e,t){if(typeof e=="object"&&e!==null){var a=bu.get(e);return a!==void 0?a:(t={value:e,source:t,stack:De(t)},bu.set(e,t),t)}return{value:e,source:t,stack:De(t)}}var Ja=[],Za=0,Oo=null,jl=0,Vt=[],Kt=0,Un=null,on=1,sn="";function gn(e,t){Ja[Za++]=jl,Ja[Za++]=Oo,Oo=e,jl=t}function _u(e,t,a){Vt[Kt++]=on,Vt[Kt++]=sn,Vt[Kt++]=Un,Un=e;var l=on;e=sn;var o=32-xt(l)-1;l&=~(1<<o),a+=1;var i=32-xt(t)+o;if(30<i){var r=o-o%5;i=(l&(1<<r)-1).toString(32),l>>=r,o-=r,on=1<<32-xt(t)+o|a<<o|l,sn=i+e}else on=1<<i|a<<o|l,sn=e}function ms(e){e.return!==null&&(gn(e,1),_u(e,1,0))}function ys(e){for(;e===Oo;)Oo=Ja[--Za],Ja[Za]=null,jl=Ja[--Za],Ja[Za]=null;for(;e===Un;)Un=Vt[--Kt],Vt[Kt]=null,sn=Vt[--Kt],Vt[Kt]=null,on=Vt[--Kt],Vt[Kt]=null}function Su(e,t){Vt[Kt++]=on,Vt[Kt++]=sn,Vt[Kt++]=Un,on=t.id,sn=t.overflow,Un=e}var yt=null,Ve=null,Ce=!1,Hn=null,It=!1,gs=Error(u(519));function Gn(e){var t=Error(u(418,1<arguments.length&&arguments[1]!==void 0&&arguments[1]?"text":"HTML",""));throw Ml(Qt(t,e)),gs}function xu(e){var t=e.stateNode,a=e.type,l=e.memoizedProps;switch(t[mt]=e,t[At]=l,a){case"dialog":we("cancel",t),we("close",t);break;case"iframe":case"object":case"embed":we("load",t);break;case"video":case"audio":for(a=0;a<Fl.length;a++)we(Fl[a],t);break;case"source":we("error",t);break;case"img":case"image":case"link":we("error",t),we("load",t);break;case"details":we("toggle",t);break;case"input":we("invalid",t),kr(t,l.value,l.defaultValue,l.checked,l.defaultChecked,l.type,l.name,!0);break;case"select":we("invalid",t);break;case"textarea":we("invalid",t),Ur(t,l.value,l.defaultValue,l.children)}a=l.children,typeof a!="string"&&typeof a!="number"&&typeof a!="bigint"||t.textContent===""+a||l.suppressHydrationWarning===!0||Lf(t.textContent,a)?(l.popover!=null&&(we("beforetoggle",t),we("toggle",t)),l.onScroll!=null&&we("scroll",t),l.onScrollEnd!=null&&we("scrollend",t),l.onClick!=null&&(t.onclick=hn),t=!0):t=!1,t||Gn(e,!0)}function Tu(e){for(yt=e.return;yt;)switch(yt.tag){case 5:case 31:case 13:It=!1;return;case 27:case 3:It=!0;return;default:yt=yt.return}}function Qa(e){if(e!==yt)return!1;if(!Ce)return Tu(e),Ce=!0,!1;var t=e.tag,a;if((a=t!==3&&t!==27)&&((a=t===5)&&(a=e.type,a=!(a!=="form"&&a!=="button")||kc(e.type,e.memoizedProps)),a=!a),a&&Ve&&Gn(e),Tu(e),t===13){if(e=e.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(u(317));Ve=If(e)}else if(t===31){if(e=e.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(u(317));Ve=If(e)}else t===27?(t=Ve,Pn(e.type)?(e=Lc,Lc=null,Ve=e):Ve=t):Ve=yt?Wt(e.stateNode.nextSibling):null;return!0}function ga(){Ve=yt=null,Ce=!1}function vs(){var e=Hn;return e!==null&&(Dt===null?Dt=e:Dt.push.apply(Dt,e),Hn=null),e}function Ml(e){Hn===null?Hn=[e]:Hn.push(e)}var bs=ce(null),va=null,vn=null;function Ln(e,t,a){P(bs,t._currentValue),t._currentValue=a}function bn(e){e._currentValue=bs.current,te(bs)}function _s(e,t,a){for(;e!==null;){var l=e.alternate;if((e.childLanes&t)!==t?(e.childLanes|=t,l!==null&&(l.childLanes|=t)):l!==null&&(l.childLanes&t)!==t&&(l.childLanes|=t),e===a)break;e=e.return}}function Ss(e,t,a,l){var o=e.child;for(o!==null&&(o.return=e);o!==null;){var i=o.dependencies;if(i!==null){var r=o.child;i=i.firstContext;e:for(;i!==null;){var f=i;i=o;for(var _=0;_<t.length;_++)if(f.context===t[_]){i.lanes|=a,f=i.alternate,f!==null&&(f.lanes|=a),_s(i.return,a,e),l||(r=null);break e}i=f.next}}else if(o.tag===18){if(r=o.return,r===null)throw Error(u(341));r.lanes|=a,i=r.alternate,i!==null&&(i.lanes|=a),_s(r,a,e),r=null}else r=o.child;if(r!==null)r.return=o;else for(r=o;r!==null;){if(r===e){r=null;break}if(o=r.sibling,o!==null){o.return=r.return,r=o;break}r=r.return}o=r}}function Va(e,t,a,l){e=null;for(var o=t,i=!1;o!==null;){if(!i){if((o.flags&524288)!==0)i=!0;else if((o.flags&262144)!==0)break}if(o.tag===10){var r=o.alternate;if(r===null)throw Error(u(387));if(r=r.memoizedProps,r!==null){var f=o.type;Rt(o.pendingProps.value,r.value)||(e!==null?e.push(f):e=[f])}}else if(o===Be.current){if(r=o.alternate,r===null)throw Error(u(387));r.memoizedState.memoizedState!==o.memoizedState.memoizedState&&(e!==null?e.push(ao):e=[ao])}o=o.return}e!==null&&Ss(t,e,a,l),t.flags|=262144}function zo(e){for(e=e.firstContext;e!==null;){if(!Rt(e.context._currentValue,e.memoizedValue))return!0;e=e.next}return!1}function ba(e){va=e,vn=null,e=e.dependencies,e!==null&&(e.firstContext=null)}function gt(e){return Eu(va,e)}function Ro(e,t){return va===null&&ba(e),Eu(e,t)}function Eu(e,t){var a=t._currentValue;if(t={context:t,memoizedValue:a,next:null},vn===null){if(e===null)throw Error(u(308));vn=t,e.dependencies={lanes:0,firstContext:t},e.flags|=524288}else vn=vn.next=t;return a}var cm=typeof AbortController<"u"?AbortController:function(){var e=[],t=this.signal={aborted:!1,addEventListener:function(a,l){e.push(l)}};this.abort=function(){t.aborted=!0,e.forEach(function(a){return a()})}},rm=c.unstable_scheduleCallback,um=c.unstable_NormalPriority,ot={$$typeof:H,Consumer:null,Provider:null,_currentValue:null,_currentValue2:null,_threadCount:0};function xs(){return{controller:new cm,data:new Map,refCount:0}}function Dl(e){e.refCount--,e.refCount===0&&rm(um,function(){e.controller.abort()})}var Ol=null,Ts=0,Ka=0,Ia=null;function dm(e,t){if(Ol===null){var a=Ol=[];Ts=0,Ka=Ac(),Ia={status:"pending",value:void 0,then:function(l){a.push(l)}}}return Ts++,t.then(wu,wu),t}function wu(){if(--Ts===0&&Ol!==null){Ia!==null&&(Ia.status="fulfilled");var e=Ol;Ol=null,Ka=0,Ia=null;for(var t=0;t<e.length;t++)(0,e[t])()}}function fm(e,t){var a=[],l={status:"pending",value:null,reason:null,then:function(o){a.push(o)}};return e.then(function(){l.status="fulfilled",l.value=t;for(var o=0;o<a.length;o++)(0,a[o])(t)},function(o){for(l.status="rejected",l.reason=o,o=0;o<a.length;o++)(0,a[o])(void 0)}),l}var Au=M.S;M.S=function(e,t){uf=ge(),typeof t=="object"&&t!==null&&typeof t.then=="function"&&dm(e,t),Au!==null&&Au(e,t)};var _a=ce(null);function Es(){var e=_a.current;return e!==null?e:Ze.pooledCache}function ko(e,t){t===null?P(_a,_a.current):P(_a,t.pool)}function Nu(){var e=Es();return e===null?null:{parent:ot._currentValue,pool:e}}var $a=Error(u(460)),ws=Error(u(474)),Bo=Error(u(542)),Uo={then:function(){}};function Cu(e){return e=e.status,e==="fulfilled"||e==="rejected"}function ju(e,t,a){switch(a=e[a],a===void 0?e.push(t):a!==t&&(t.then(hn,hn),t=a),t.status){case"fulfilled":return t.value;case"rejected":throw e=t.reason,Du(e),e;default:if(typeof t.status=="string")t.then(hn,hn);else{if(e=Ze,e!==null&&100<e.shellSuspendCounter)throw Error(u(482));e=t,e.status="pending",e.then(function(l){if(t.status==="pending"){var o=t;o.status="fulfilled",o.value=l}},function(l){if(t.status==="pending"){var o=t;o.status="rejected",o.reason=l}})}switch(t.status){case"fulfilled":return t.value;case"rejected":throw e=t.reason,Du(e),e}throw xa=t,$a}}function Sa(e){try{var t=e._init;return t(e._payload)}catch(a){throw a!==null&&typeof a=="object"&&typeof a.then=="function"?(xa=a,$a):a}}var xa=null;function Mu(){if(xa===null)throw Error(u(459));var e=xa;return xa=null,e}function Du(e){if(e===$a||e===Bo)throw Error(u(483))}var Wa=null,zl=0;function Ho(e){var t=zl;return zl+=1,Wa===null&&(Wa=[]),ju(Wa,e,t)}function Rl(e,t){t=t.props.ref,e.ref=t!==void 0?t:null}function Go(e,t){throw t.$$typeof===T?Error(u(525)):(e=Object.prototype.toString.call(t),Error(u(31,e==="[object Object]"?"object with keys {"+Object.keys(t).join(", ")+"}":e)))}function Ou(e){function t(j,E){if(e){var z=j.deletions;z===null?(j.deletions=[E],j.flags|=16):z.push(E)}}function a(j,E){if(!e)return null;for(;E!==null;)t(j,E),E=E.sibling;return null}function l(j){for(var E=new Map;j!==null;)j.key!==null?E.set(j.key,j):E.set(j.index,j),j=j.sibling;return E}function o(j,E){return j=yn(j,E),j.index=0,j.sibling=null,j}function i(j,E,z){return j.index=z,e?(z=j.alternate,z!==null?(z=z.index,z<E?(j.flags|=67108866,E):z):(j.flags|=67108866,E)):(j.flags|=1048576,E)}function r(j){return e&&j.alternate===null&&(j.flags|=67108866),j}function f(j,E,z,Z){return E===null||E.tag!==6?(E=ps(z,j.mode,Z),E.return=j,E):(E=o(E,z),E.return=j,E)}function _(j,E,z,Z){var se=z.type;return se===C?X(j,E,z.props.children,Z,z.key):E!==null&&(E.elementType===se||typeof se=="object"&&se!==null&&se.$$typeof===J&&Sa(se)===E.type)?(E=o(E,z.props),Rl(E,z),E.return=j,E):(E=Do(z.type,z.key,z.props,null,j.mode,Z),Rl(E,z),E.return=j,E)}function R(j,E,z,Z){return E===null||E.tag!==4||E.stateNode.containerInfo!==z.containerInfo||E.stateNode.implementation!==z.implementation?(E=hs(z,j.mode,Z),E.return=j,E):(E=o(E,z.children||[]),E.return=j,E)}function X(j,E,z,Z,se){return E===null||E.tag!==7?(E=ya(z,j.mode,Z,se),E.return=j,E):(E=o(E,z),E.return=j,E)}function Q(j,E,z){if(typeof E=="string"&&E!==""||typeof E=="number"||typeof E=="bigint")return E=ps(""+E,j.mode,z),E.return=j,E;if(typeof E=="object"&&E!==null){switch(E.$$typeof){case N:return z=Do(E.type,E.key,E.props,null,j.mode,z),Rl(z,E),z.return=j,z;case w:return E=hs(E,j.mode,z),E.return=j,E;case J:return E=Sa(E),Q(j,E,z)}if(ie(E)||ne(E))return E=ya(E,j.mode,z,null),E.return=j,E;if(typeof E.then=="function")return Q(j,Ho(E),z);if(E.$$typeof===H)return Q(j,Ro(j,E),z);Go(j,E)}return null}function U(j,E,z,Z){var se=E!==null?E.key:null;if(typeof z=="string"&&z!==""||typeof z=="number"||typeof z=="bigint")return se!==null?null:f(j,E,""+z,Z);if(typeof z=="object"&&z!==null){switch(z.$$typeof){case N:return z.key===se?_(j,E,z,Z):null;case w:return z.key===se?R(j,E,z,Z):null;case J:return z=Sa(z),U(j,E,z,Z)}if(ie(z)||ne(z))return se!==null?null:X(j,E,z,Z,null);if(typeof z.then=="function")return U(j,E,Ho(z),Z);if(z.$$typeof===H)return U(j,E,Ro(j,z),Z);Go(j,z)}return null}function G(j,E,z,Z,se){if(typeof Z=="string"&&Z!==""||typeof Z=="number"||typeof Z=="bigint")return j=j.get(z)||null,f(E,j,""+Z,se);if(typeof Z=="object"&&Z!==null){switch(Z.$$typeof){case N:return j=j.get(Z.key===null?z:Z.key)||null,_(E,j,Z,se);case w:return j=j.get(Z.key===null?z:Z.key)||null,R(E,j,Z,se);case J:return Z=Sa(Z),G(j,E,z,Z,se)}if(ie(Z)||ne(Z))return j=j.get(z)||null,X(E,j,Z,se,null);if(typeof Z.then=="function")return G(j,E,z,Ho(Z),se);if(Z.$$typeof===H)return G(j,E,z,Ro(E,Z),se);Go(E,Z)}return null}function ae(j,E,z,Z){for(var se=null,Oe=null,oe=E,be=E=0,Ne=null;oe!==null&&be<z.length;be++){oe.index>be?(Ne=oe,oe=null):Ne=oe.sibling;var ze=U(j,oe,z[be],Z);if(ze===null){oe===null&&(oe=Ne);break}e&&oe&&ze.alternate===null&&t(j,oe),E=i(ze,E,be),Oe===null?se=ze:Oe.sibling=ze,Oe=ze,oe=Ne}if(be===z.length)return a(j,oe),Ce&&gn(j,be),se;if(oe===null){for(;be<z.length;be++)oe=Q(j,z[be],Z),oe!==null&&(E=i(oe,E,be),Oe===null?se=oe:Oe.sibling=oe,Oe=oe);return Ce&&gn(j,be),se}for(oe=l(oe);be<z.length;be++)Ne=G(oe,j,be,z[be],Z),Ne!==null&&(e&&Ne.alternate!==null&&oe.delete(Ne.key===null?be:Ne.key),E=i(Ne,E,be),Oe===null?se=Ne:Oe.sibling=Ne,Oe=Ne);return e&&oe.forEach(function(la){return t(j,la)}),Ce&&gn(j,be),se}function fe(j,E,z,Z){if(z==null)throw Error(u(151));for(var se=null,Oe=null,oe=E,be=E=0,Ne=null,ze=z.next();oe!==null&&!ze.done;be++,ze=z.next()){oe.index>be?(Ne=oe,oe=null):Ne=oe.sibling;var la=U(j,oe,ze.value,Z);if(la===null){oe===null&&(oe=Ne);break}e&&oe&&la.alternate===null&&t(j,oe),E=i(la,E,be),Oe===null?se=la:Oe.sibling=la,Oe=la,oe=Ne}if(ze.done)return a(j,oe),Ce&&gn(j,be),se;if(oe===null){for(;!ze.done;be++,ze=z.next())ze=Q(j,ze.value,Z),ze!==null&&(E=i(ze,E,be),Oe===null?se=ze:Oe.sibling=ze,Oe=ze);return Ce&&gn(j,be),se}for(oe=l(oe);!ze.done;be++,ze=z.next())ze=G(oe,j,be,ze.value,Z),ze!==null&&(e&&ze.alternate!==null&&oe.delete(ze.key===null?be:ze.key),E=i(ze,E,be),Oe===null?se=ze:Oe.sibling=ze,Oe=ze);return e&&oe.forEach(function(Ty){return t(j,Ty)}),Ce&&gn(j,be),se}function Je(j,E,z,Z){if(typeof z=="object"&&z!==null&&z.type===C&&z.key===null&&(z=z.props.children),typeof z=="object"&&z!==null){switch(z.$$typeof){case N:e:{for(var se=z.key;E!==null;){if(E.key===se){if(se=z.type,se===C){if(E.tag===7){a(j,E.sibling),Z=o(E,z.props.children),Z.return=j,j=Z;break e}}else if(E.elementType===se||typeof se=="object"&&se!==null&&se.$$typeof===J&&Sa(se)===E.type){a(j,E.sibling),Z=o(E,z.props),Rl(Z,z),Z.return=j,j=Z;break e}a(j,E);break}else t(j,E);E=E.sibling}z.type===C?(Z=ya(z.props.children,j.mode,Z,z.key),Z.return=j,j=Z):(Z=Do(z.type,z.key,z.props,null,j.mode,Z),Rl(Z,z),Z.return=j,j=Z)}return r(j);case w:e:{for(se=z.key;E!==null;){if(E.key===se)if(E.tag===4&&E.stateNode.containerInfo===z.containerInfo&&E.stateNode.implementation===z.implementation){a(j,E.sibling),Z=o(E,z.children||[]),Z.return=j,j=Z;break e}else{a(j,E);break}else t(j,E);E=E.sibling}Z=hs(z,j.mode,Z),Z.return=j,j=Z}return r(j);case J:return z=Sa(z),Je(j,E,z,Z)}if(ie(z))return ae(j,E,z,Z);if(ne(z)){if(se=ne(z),typeof se!="function")throw Error(u(150));return z=se.call(z),fe(j,E,z,Z)}if(typeof z.then=="function")return Je(j,E,Ho(z),Z);if(z.$$typeof===H)return Je(j,E,Ro(j,z),Z);Go(j,z)}return typeof z=="string"&&z!==""||typeof z=="number"||typeof z=="bigint"?(z=""+z,E!==null&&E.tag===6?(a(j,E.sibling),Z=o(E,z),Z.return=j,j=Z):(a(j,E),Z=ps(z,j.mode,Z),Z.return=j,j=Z),r(j)):a(j,E)}return function(j,E,z,Z){try{zl=0;var se=Je(j,E,z,Z);return Wa=null,se}catch(oe){if(oe===$a||oe===Bo)throw oe;var Oe=kt(29,oe,null,j.mode);return Oe.lanes=Z,Oe.return=j,Oe}finally{}}}var Ta=Ou(!0),zu=Ou(!1),qn=!1;function As(e){e.updateQueue={baseState:e.memoizedState,firstBaseUpdate:null,lastBaseUpdate:null,shared:{pending:null,lanes:0,hiddenCallbacks:null},callbacks:null}}function Ns(e,t){e=e.updateQueue,t.updateQueue===e&&(t.updateQueue={baseState:e.baseState,firstBaseUpdate:e.firstBaseUpdate,lastBaseUpdate:e.lastBaseUpdate,shared:e.shared,callbacks:null})}function Yn(e){return{lane:e,tag:0,payload:null,callback:null,next:null}}function Xn(e,t,a){var l=e.updateQueue;if(l===null)return null;if(l=l.shared,(ke&2)!==0){var o=l.pending;return o===null?t.next=t:(t.next=o.next,o.next=t),l.pending=t,t=Mo(e),yu(e,null,a),t}return jo(e,l,t,a),Mo(e)}function kl(e,t,a){if(t=t.updateQueue,t!==null&&(t=t.shared,(a&4194048)!==0)){var l=t.lanes;l&=e.pendingLanes,a|=l,t.lanes=a,Er(e,a)}}function Cs(e,t){var a=e.updateQueue,l=e.alternate;if(l!==null&&(l=l.updateQueue,a===l)){var o=null,i=null;if(a=a.firstBaseUpdate,a!==null){do{var r={lane:a.lane,tag:a.tag,payload:a.payload,callback:null,next:null};i===null?o=i=r:i=i.next=r,a=a.next}while(a!==null);i===null?o=i=t:i=i.next=t}else o=i=t;a={baseState:l.baseState,firstBaseUpdate:o,lastBaseUpdate:i,shared:l.shared,callbacks:l.callbacks},e.updateQueue=a;return}e=a.lastBaseUpdate,e===null?a.firstBaseUpdate=t:e.next=t,a.lastBaseUpdate=t}var js=!1;function Bl(){if(js){var e=Ia;if(e!==null)throw e}}function Ul(e,t,a,l){js=!1;var o=e.updateQueue;qn=!1;var i=o.firstBaseUpdate,r=o.lastBaseUpdate,f=o.shared.pending;if(f!==null){o.shared.pending=null;var _=f,R=_.next;_.next=null,r===null?i=R:r.next=R,r=_;var X=e.alternate;X!==null&&(X=X.updateQueue,f=X.lastBaseUpdate,f!==r&&(f===null?X.firstBaseUpdate=R:f.next=R,X.lastBaseUpdate=_))}if(i!==null){var Q=o.baseState;r=0,X=R=_=null,f=i;do{var U=f.lane&-536870913,G=U!==f.lane;if(G?(Ae&U)===U:(l&U)===U){U!==0&&U===Ka&&(js=!0),X!==null&&(X=X.next={lane:0,tag:f.tag,payload:f.payload,callback:null,next:null});e:{var ae=e,fe=f;U=t;var Je=a;switch(fe.tag){case 1:if(ae=fe.payload,typeof ae=="function"){Q=ae.call(Je,Q,U);break e}Q=ae;break e;case 3:ae.flags=ae.flags&-65537|128;case 0:if(ae=fe.payload,U=typeof ae=="function"?ae.call(Je,Q,U):ae,U==null)break e;Q=v({},Q,U);break e;case 2:qn=!0}}U=f.callback,U!==null&&(e.flags|=64,G&&(e.flags|=8192),G=o.callbacks,G===null?o.callbacks=[U]:G.push(U))}else G={lane:U,tag:f.tag,payload:f.payload,callback:f.callback,next:null},X===null?(R=X=G,_=Q):X=X.next=G,r|=U;if(f=f.next,f===null){if(f=o.shared.pending,f===null)break;G=f,f=G.next,G.next=null,o.lastBaseUpdate=G,o.shared.pending=null}}while(!0);X===null&&(_=Q),o.baseState=_,o.firstBaseUpdate=R,o.lastBaseUpdate=X,i===null&&(o.shared.lanes=0),Kn|=r,e.lanes=r,e.memoizedState=Q}}function Ru(e,t){if(typeof e!="function")throw Error(u(191,e));e.call(t)}function ku(e,t){var a=e.callbacks;if(a!==null)for(e.callbacks=null,e=0;e<a.length;e++)Ru(a[e],t)}var Fa=ce(null),Lo=ce(0);function Bu(e,t){e=Cn,P(Lo,e),P(Fa,t),Cn=e|t.baseLanes}function Ms(){P(Lo,Cn),P(Fa,Fa.current)}function Ds(){Cn=Lo.current,te(Fa),te(Lo)}var Bt=ce(null),$t=null;function Jn(e){var t=e.alternate;P(tt,tt.current&1),P(Bt,e),$t===null&&(t===null||Fa.current!==null||t.memoizedState!==null)&&($t=e)}function Os(e){P(tt,tt.current),P(Bt,e),$t===null&&($t=e)}function Uu(e){e.tag===22?(P(tt,tt.current),P(Bt,e),$t===null&&($t=e)):Zn()}function Zn(){P(tt,tt.current),P(Bt,Bt.current)}function Ut(e){te(Bt),$t===e&&($t=null),te(tt)}var tt=ce(0);function qo(e){for(var t=e;t!==null;){if(t.tag===13){var a=t.memoizedState;if(a!==null&&(a=a.dehydrated,a===null||Hc(a)||Gc(a)))return t}else if(t.tag===19&&(t.memoizedProps.revealOrder==="forwards"||t.memoizedProps.revealOrder==="backwards"||t.memoizedProps.revealOrder==="unstable_legacy-backwards"||t.memoizedProps.revealOrder==="together")){if((t.flags&128)!==0)return t}else if(t.child!==null){t.child.return=t,t=t.child;continue}if(t===e)break;for(;t.sibling===null;){if(t.return===null||t.return===e)return null;t=t.return}t.sibling.return=t.return,t=t.sibling}return null}var _n=0,ve=null,Ye=null,it=null,Yo=!1,Pa=!1,Ea=!1,Xo=0,Hl=0,el=null,pm=0;function We(){throw Error(u(321))}function zs(e,t){if(t===null)return!1;for(var a=0;a<t.length&&a<e.length;a++)if(!Rt(e[a],t[a]))return!1;return!0}function Rs(e,t,a,l,o,i){return _n=i,ve=t,t.memoizedState=null,t.updateQueue=null,t.lanes=0,M.H=e===null||e.memoizedState===null?_d:Is,Ea=!1,i=a(l,o),Ea=!1,Pa&&(i=Gu(t,a,l,o)),Hu(e),i}function Hu(e){M.H=ql;var t=Ye!==null&&Ye.next!==null;if(_n=0,it=Ye=ve=null,Yo=!1,Hl=0,el=null,t)throw Error(u(300));e===null||st||(e=e.dependencies,e!==null&&zo(e)&&(st=!0))}function Gu(e,t,a,l){ve=e;var o=0;do{if(Pa&&(el=null),Hl=0,Pa=!1,25<=o)throw Error(u(301));if(o+=1,it=Ye=null,e.updateQueue!=null){var i=e.updateQueue;i.lastEffect=null,i.events=null,i.stores=null,i.memoCache!=null&&(i.memoCache.index=0)}M.H=Sd,i=t(a,l)}while(Pa);return i}function hm(){var e=M.H,t=e.useState()[0];return t=typeof t.then=="function"?Gl(t):t,e=e.useState()[0],(Ye!==null?Ye.memoizedState:null)!==e&&(ve.flags|=1024),t}function ks(){var e=Xo!==0;return Xo=0,e}function Bs(e,t,a){t.updateQueue=e.updateQueue,t.flags&=-2053,e.lanes&=~a}function Us(e){if(Yo){for(e=e.memoizedState;e!==null;){var t=e.queue;t!==null&&(t.pending=null),e=e.next}Yo=!1}_n=0,it=Ye=ve=null,Pa=!1,Hl=Xo=0,el=null}function Tt(){var e={memoizedState:null,baseState:null,baseQueue:null,queue:null,next:null};return it===null?ve.memoizedState=it=e:it=it.next=e,it}function nt(){if(Ye===null){var e=ve.alternate;e=e!==null?e.memoizedState:null}else e=Ye.next;var t=it===null?ve.memoizedState:it.next;if(t!==null)it=t,Ye=e;else{if(e===null)throw ve.alternate===null?Error(u(467)):Error(u(310));Ye=e,e={memoizedState:Ye.memoizedState,baseState:Ye.baseState,baseQueue:Ye.baseQueue,queue:Ye.queue,next:null},it===null?ve.memoizedState=it=e:it=it.next=e}return it}function Jo(){return{lastEffect:null,events:null,stores:null,memoCache:null}}function Gl(e){var t=Hl;return Hl+=1,el===null&&(el=[]),e=ju(el,e,t),t=ve,(it===null?t.memoizedState:it.next)===null&&(t=t.alternate,M.H=t===null||t.memoizedState===null?_d:Is),e}function Zo(e){if(e!==null&&typeof e=="object"){if(typeof e.then=="function")return Gl(e);if(e.$$typeof===H)return gt(e)}throw Error(u(438,String(e)))}function Hs(e){var t=null,a=ve.updateQueue;if(a!==null&&(t=a.memoCache),t==null){var l=ve.alternate;l!==null&&(l=l.updateQueue,l!==null&&(l=l.memoCache,l!=null&&(t={data:l.data.map(function(o){return o.slice()}),index:0})))}if(t==null&&(t={data:[],index:0}),a===null&&(a=Jo(),ve.updateQueue=a),a.memoCache=t,a=t.data[t.index],a===void 0)for(a=t.data[t.index]=Array(e),l=0;l<e;l++)a[l]=I;return t.index++,a}function Sn(e,t){return typeof t=="function"?t(e):t}function Qo(e){var t=nt();return Gs(t,Ye,e)}function Gs(e,t,a){var l=e.queue;if(l===null)throw Error(u(311));l.lastRenderedReducer=a;var o=e.baseQueue,i=l.pending;if(i!==null){if(o!==null){var r=o.next;o.next=i.next,i.next=r}t.baseQueue=o=i,l.pending=null}if(i=e.baseState,o===null)e.memoizedState=i;else{t=o.next;var f=r=null,_=null,R=t,X=!1;do{var Q=R.lane&-536870913;if(Q!==R.lane?(Ae&Q)===Q:(_n&Q)===Q){var U=R.revertLane;if(U===0)_!==null&&(_=_.next={lane:0,revertLane:0,gesture:null,action:R.action,hasEagerState:R.hasEagerState,eagerState:R.eagerState,next:null}),Q===Ka&&(X=!0);else if((_n&U)===U){R=R.next,U===Ka&&(X=!0);continue}else Q={lane:0,revertLane:R.revertLane,gesture:null,action:R.action,hasEagerState:R.hasEagerState,eagerState:R.eagerState,next:null},_===null?(f=_=Q,r=i):_=_.next=Q,ve.lanes|=U,Kn|=U;Q=R.action,Ea&&a(i,Q),i=R.hasEagerState?R.eagerState:a(i,Q)}else U={lane:Q,revertLane:R.revertLane,gesture:R.gesture,action:R.action,hasEagerState:R.hasEagerState,eagerState:R.eagerState,next:null},_===null?(f=_=U,r=i):_=_.next=U,ve.lanes|=Q,Kn|=Q;R=R.next}while(R!==null&&R!==t);if(_===null?r=i:_.next=f,!Rt(i,e.memoizedState)&&(st=!0,X&&(a=Ia,a!==null)))throw a;e.memoizedState=i,e.baseState=r,e.baseQueue=_,l.lastRenderedState=i}return o===null&&(l.lanes=0),[e.memoizedState,l.dispatch]}function Ls(e){var t=nt(),a=t.queue;if(a===null)throw Error(u(311));a.lastRenderedReducer=e;var l=a.dispatch,o=a.pending,i=t.memoizedState;if(o!==null){a.pending=null;var r=o=o.next;do i=e(i,r.action),r=r.next;while(r!==o);Rt(i,t.memoizedState)||(st=!0),t.memoizedState=i,t.baseQueue===null&&(t.baseState=i),a.lastRenderedState=i}return[i,l]}function Lu(e,t,a){var l=ve,o=nt(),i=Ce;if(i){if(a===void 0)throw Error(u(407));a=a()}else a=t();var r=!Rt((Ye||o).memoizedState,a);if(r&&(o.memoizedState=a,st=!0),o=o.queue,Xs(Xu.bind(null,l,o,e),[e]),o.getSnapshot!==t||r||it!==null&&it.memoizedState.tag&1){if(l.flags|=2048,tl(9,{destroy:void 0},Yu.bind(null,l,o,a,t),null),Ze===null)throw Error(u(349));i||(_n&127)!==0||qu(l,t,a)}return a}function qu(e,t,a){e.flags|=16384,e={getSnapshot:t,value:a},t=ve.updateQueue,t===null?(t=Jo(),ve.updateQueue=t,t.stores=[e]):(a=t.stores,a===null?t.stores=[e]:a.push(e))}function Yu(e,t,a,l){t.value=a,t.getSnapshot=l,Ju(t)&&Zu(e)}function Xu(e,t,a){return a(function(){Ju(t)&&Zu(e)})}function Ju(e){var t=e.getSnapshot;e=e.value;try{var a=t();return!Rt(e,a)}catch{return!0}}function Zu(e){var t=ma(e,2);t!==null&&Ot(t,e,2)}function qs(e){var t=Tt();if(typeof e=="function"){var a=e;if(e=a(),Ea){ln(!0);try{a()}finally{ln(!1)}}}return t.memoizedState=t.baseState=e,t.queue={pending:null,lanes:0,dispatch:null,lastRenderedReducer:Sn,lastRenderedState:e},t}function Qu(e,t,a,l){return e.baseState=a,Gs(e,Ye,typeof l=="function"?l:Sn)}function mm(e,t,a,l,o){if(Io(e))throw Error(u(485));if(e=t.action,e!==null){var i={payload:o,action:e,next:null,isTransition:!0,status:"pending",value:null,reason:null,listeners:[],then:function(r){i.listeners.push(r)}};M.T!==null?a(!0):i.isTransition=!1,l(i),a=t.pending,a===null?(i.next=t.pending=i,Vu(t,i)):(i.next=a.next,t.pending=a.next=i)}}function Vu(e,t){var a=t.action,l=t.payload,o=e.state;if(t.isTransition){var i=M.T,r={};M.T=r;try{var f=a(o,l),_=M.S;_!==null&&_(r,f),Ku(e,t,f)}catch(R){Ys(e,t,R)}finally{i!==null&&r.types!==null&&(i.types=r.types),M.T=i}}else try{i=a(o,l),Ku(e,t,i)}catch(R){Ys(e,t,R)}}function Ku(e,t,a){a!==null&&typeof a=="object"&&typeof a.then=="function"?a.then(function(l){Iu(e,t,l)},function(l){return Ys(e,t,l)}):Iu(e,t,a)}function Iu(e,t,a){t.status="fulfilled",t.value=a,$u(t),e.state=a,t=e.pending,t!==null&&(a=t.next,a===t?e.pending=null:(a=a.next,t.next=a,Vu(e,a)))}function Ys(e,t,a){var l=e.pending;if(e.pending=null,l!==null){l=l.next;do t.status="rejected",t.reason=a,$u(t),t=t.next;while(t!==l)}e.action=null}function $u(e){e=e.listeners;for(var t=0;t<e.length;t++)(0,e[t])()}function Wu(e,t){return t}function Fu(e,t){if(Ce){var a=Ze.formState;if(a!==null){e:{var l=ve;if(Ce){if(Ve){t:{for(var o=Ve,i=It;o.nodeType!==8;){if(!i){o=null;break t}if(o=Wt(o.nextSibling),o===null){o=null;break t}}i=o.data,o=i==="F!"||i==="F"?o:null}if(o){Ve=Wt(o.nextSibling),l=o.data==="F!";break e}}Gn(l)}l=!1}l&&(t=a[0])}}return a=Tt(),a.memoizedState=a.baseState=t,l={pending:null,lanes:0,dispatch:null,lastRenderedReducer:Wu,lastRenderedState:t},a.queue=l,a=gd.bind(null,ve,l),l.dispatch=a,l=qs(!1),i=Ks.bind(null,ve,!1,l.queue),l=Tt(),o={state:t,dispatch:null,action:e,pending:null},l.queue=o,a=mm.bind(null,ve,o,i,a),o.dispatch=a,l.memoizedState=e,[t,a,!1]}function Pu(e){var t=nt();return ed(t,Ye,e)}function ed(e,t,a){if(t=Gs(e,t,Wu)[0],e=Qo(Sn)[0],typeof t=="object"&&t!==null&&typeof t.then=="function")try{var l=Gl(t)}catch(r){throw r===$a?Bo:r}else l=t;t=nt();var o=t.queue,i=o.dispatch;return a!==t.memoizedState&&(ve.flags|=2048,tl(9,{destroy:void 0},ym.bind(null,o,a),null)),[l,i,e]}function ym(e,t){e.action=t}function td(e){var t=nt(),a=Ye;if(a!==null)return ed(t,a,e);nt(),t=t.memoizedState,a=nt();var l=a.queue.dispatch;return a.memoizedState=e,[t,l,!1]}function tl(e,t,a,l){return e={tag:e,create:a,deps:l,inst:t,next:null},t=ve.updateQueue,t===null&&(t=Jo(),ve.updateQueue=t),a=t.lastEffect,a===null?t.lastEffect=e.next=e:(l=a.next,a.next=e,e.next=l,t.lastEffect=e),e}function nd(){return nt().memoizedState}function Vo(e,t,a,l){var o=Tt();ve.flags|=e,o.memoizedState=tl(1|t,{destroy:void 0},a,l===void 0?null:l)}function Ko(e,t,a,l){var o=nt();l=l===void 0?null:l;var i=o.memoizedState.inst;Ye!==null&&l!==null&&zs(l,Ye.memoizedState.deps)?o.memoizedState=tl(t,i,a,l):(ve.flags|=e,o.memoizedState=tl(1|t,i,a,l))}function ad(e,t){Vo(8390656,8,e,t)}function Xs(e,t){Ko(2048,8,e,t)}function gm(e){ve.flags|=4;var t=ve.updateQueue;if(t===null)t=Jo(),ve.updateQueue=t,t.events=[e];else{var a=t.events;a===null?t.events=[e]:a.push(e)}}function ld(e){var t=nt().memoizedState;return gm({ref:t,nextImpl:e}),function(){if((ke&2)!==0)throw Error(u(440));return t.impl.apply(void 0,arguments)}}function od(e,t){return Ko(4,2,e,t)}function id(e,t){return Ko(4,4,e,t)}function sd(e,t){if(typeof t=="function"){e=e();var a=t(e);return function(){typeof a=="function"?a():t(null)}}if(t!=null)return e=e(),t.current=e,function(){t.current=null}}function cd(e,t,a){a=a!=null?a.concat([e]):null,Ko(4,4,sd.bind(null,t,e),a)}function Js(){}function rd(e,t){var a=nt();t=t===void 0?null:t;var l=a.memoizedState;return t!==null&&zs(t,l[1])?l[0]:(a.memoizedState=[e,t],e)}function ud(e,t){var a=nt();t=t===void 0?null:t;var l=a.memoizedState;if(t!==null&&zs(t,l[1]))return l[0];if(l=e(),Ea){ln(!0);try{e()}finally{ln(!1)}}return a.memoizedState=[l,t],l}function Zs(e,t,a){return a===void 0||(_n&1073741824)!==0&&(Ae&261930)===0?e.memoizedState=t:(e.memoizedState=a,e=ff(),ve.lanes|=e,Kn|=e,a)}function dd(e,t,a,l){return Rt(a,t)?a:Fa.current!==null?(e=Zs(e,a,l),Rt(e,t)||(st=!0),e):(_n&42)===0||(_n&1073741824)!==0&&(Ae&261930)===0?(st=!0,e.memoizedState=a):(e=ff(),ve.lanes|=e,Kn|=e,t)}function fd(e,t,a,l,o){var i=D.p;D.p=i!==0&&8>i?i:8;var r=M.T,f={};M.T=f,Ks(e,!1,t,a);try{var _=o(),R=M.S;if(R!==null&&R(f,_),_!==null&&typeof _=="object"&&typeof _.then=="function"){var X=fm(_,l);Ll(e,t,X,Lt(e))}else Ll(e,t,l,Lt(e))}catch(Q){Ll(e,t,{then:function(){},status:"rejected",reason:Q},Lt())}finally{D.p=i,r!==null&&f.types!==null&&(r.types=f.types),M.T=r}}function vm(){}function Qs(e,t,a,l){if(e.tag!==5)throw Error(u(476));var o=pd(e).queue;fd(e,o,t,V,a===null?vm:function(){return hd(e),a(l)})}function pd(e){var t=e.memoizedState;if(t!==null)return t;t={memoizedState:V,baseState:V,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:Sn,lastRenderedState:V},next:null};var a={};return t.next={memoizedState:a,baseState:a,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:Sn,lastRenderedState:a},next:null},e.memoizedState=t,e=e.alternate,e!==null&&(e.memoizedState=t),t}function hd(e){var t=pd(e);t.next===null&&(t=e.alternate.memoizedState),Ll(e,t.next.queue,{},Lt())}function Vs(){return gt(ao)}function md(){return nt().memoizedState}function yd(){return nt().memoizedState}function bm(e){for(var t=e.return;t!==null;){switch(t.tag){case 24:case 3:var a=Lt();e=Yn(a);var l=Xn(t,e,a);l!==null&&(Ot(l,t,a),kl(l,t,a)),t={cache:xs()},e.payload=t;return}t=t.return}}function _m(e,t,a){var l=Lt();a={lane:l,revertLane:0,gesture:null,action:a,hasEagerState:!1,eagerState:null,next:null},Io(e)?vd(t,a):(a=ds(e,t,a,l),a!==null&&(Ot(a,e,l),bd(a,t,l)))}function gd(e,t,a){var l=Lt();Ll(e,t,a,l)}function Ll(e,t,a,l){var o={lane:l,revertLane:0,gesture:null,action:a,hasEagerState:!1,eagerState:null,next:null};if(Io(e))vd(t,o);else{var i=e.alternate;if(e.lanes===0&&(i===null||i.lanes===0)&&(i=t.lastRenderedReducer,i!==null))try{var r=t.lastRenderedState,f=i(r,a);if(o.hasEagerState=!0,o.eagerState=f,Rt(f,r))return jo(e,t,o,0),Ze===null&&Co(),!1}catch{}finally{}if(a=ds(e,t,o,l),a!==null)return Ot(a,e,l),bd(a,t,l),!0}return!1}function Ks(e,t,a,l){if(l={lane:2,revertLane:Ac(),gesture:null,action:l,hasEagerState:!1,eagerState:null,next:null},Io(e)){if(t)throw Error(u(479))}else t=ds(e,a,l,2),t!==null&&Ot(t,e,2)}function Io(e){var t=e.alternate;return e===ve||t!==null&&t===ve}function vd(e,t){Pa=Yo=!0;var a=e.pending;a===null?t.next=t:(t.next=a.next,a.next=t),e.pending=t}function bd(e,t,a){if((a&4194048)!==0){var l=t.lanes;l&=e.pendingLanes,a|=l,t.lanes=a,Er(e,a)}}var ql={readContext:gt,use:Zo,useCallback:We,useContext:We,useEffect:We,useImperativeHandle:We,useLayoutEffect:We,useInsertionEffect:We,useMemo:We,useReducer:We,useRef:We,useState:We,useDebugValue:We,useDeferredValue:We,useTransition:We,useSyncExternalStore:We,useId:We,useHostTransitionStatus:We,useFormState:We,useActionState:We,useOptimistic:We,useMemoCache:We,useCacheRefresh:We};ql.useEffectEvent=We;var _d={readContext:gt,use:Zo,useCallback:function(e,t){return Tt().memoizedState=[e,t===void 0?null:t],e},useContext:gt,useEffect:ad,useImperativeHandle:function(e,t,a){a=a!=null?a.concat([e]):null,Vo(4194308,4,sd.bind(null,t,e),a)},useLayoutEffect:function(e,t){return Vo(4194308,4,e,t)},useInsertionEffect:function(e,t){Vo(4,2,e,t)},useMemo:function(e,t){var a=Tt();t=t===void 0?null:t;var l=e();if(Ea){ln(!0);try{e()}finally{ln(!1)}}return a.memoizedState=[l,t],l},useReducer:function(e,t,a){var l=Tt();if(a!==void 0){var o=a(t);if(Ea){ln(!0);try{a(t)}finally{ln(!1)}}}else o=t;return l.memoizedState=l.baseState=o,e={pending:null,lanes:0,dispatch:null,lastRenderedReducer:e,lastRenderedState:o},l.queue=e,e=e.dispatch=_m.bind(null,ve,e),[l.memoizedState,e]},useRef:function(e){var t=Tt();return e={current:e},t.memoizedState=e},useState:function(e){e=qs(e);var t=e.queue,a=gd.bind(null,ve,t);return t.dispatch=a,[e.memoizedState,a]},useDebugValue:Js,useDeferredValue:function(e,t){var a=Tt();return Zs(a,e,t)},useTransition:function(){var e=qs(!1);return e=fd.bind(null,ve,e.queue,!0,!1),Tt().memoizedState=e,[!1,e]},useSyncExternalStore:function(e,t,a){var l=ve,o=Tt();if(Ce){if(a===void 0)throw Error(u(407));a=a()}else{if(a=t(),Ze===null)throw Error(u(349));(Ae&127)!==0||qu(l,t,a)}o.memoizedState=a;var i={value:a,getSnapshot:t};return o.queue=i,ad(Xu.bind(null,l,i,e),[e]),l.flags|=2048,tl(9,{destroy:void 0},Yu.bind(null,l,i,a,t),null),a},useId:function(){var e=Tt(),t=Ze.identifierPrefix;if(Ce){var a=sn,l=on;a=(l&~(1<<32-xt(l)-1)).toString(32)+a,t="_"+t+"R_"+a,a=Xo++,0<a&&(t+="H"+a.toString(32)),t+="_"}else a=pm++,t="_"+t+"r_"+a.toString(32)+"_";return e.memoizedState=t},useHostTransitionStatus:Vs,useFormState:Fu,useActionState:Fu,useOptimistic:function(e){var t=Tt();t.memoizedState=t.baseState=e;var a={pending:null,lanes:0,dispatch:null,lastRenderedReducer:null,lastRenderedState:null};return t.queue=a,t=Ks.bind(null,ve,!0,a),a.dispatch=t,[e,t]},useMemoCache:Hs,useCacheRefresh:function(){return Tt().memoizedState=bm.bind(null,ve)},useEffectEvent:function(e){var t=Tt(),a={impl:e};return t.memoizedState=a,function(){if((ke&2)!==0)throw Error(u(440));return a.impl.apply(void 0,arguments)}}},Is={readContext:gt,use:Zo,useCallback:rd,useContext:gt,useEffect:Xs,useImperativeHandle:cd,useInsertionEffect:od,useLayoutEffect:id,useMemo:ud,useReducer:Qo,useRef:nd,useState:function(){return Qo(Sn)},useDebugValue:Js,useDeferredValue:function(e,t){var a=nt();return dd(a,Ye.memoizedState,e,t)},useTransition:function(){var e=Qo(Sn)[0],t=nt().memoizedState;return[typeof e=="boolean"?e:Gl(e),t]},useSyncExternalStore:Lu,useId:md,useHostTransitionStatus:Vs,useFormState:Pu,useActionState:Pu,useOptimistic:function(e,t){var a=nt();return Qu(a,Ye,e,t)},useMemoCache:Hs,useCacheRefresh:yd};Is.useEffectEvent=ld;var Sd={readContext:gt,use:Zo,useCallback:rd,useContext:gt,useEffect:Xs,useImperativeHandle:cd,useInsertionEffect:od,useLayoutEffect:id,useMemo:ud,useReducer:Ls,useRef:nd,useState:function(){return Ls(Sn)},useDebugValue:Js,useDeferredValue:function(e,t){var a=nt();return Ye===null?Zs(a,e,t):dd(a,Ye.memoizedState,e,t)},useTransition:function(){var e=Ls(Sn)[0],t=nt().memoizedState;return[typeof e=="boolean"?e:Gl(e),t]},useSyncExternalStore:Lu,useId:md,useHostTransitionStatus:Vs,useFormState:td,useActionState:td,useOptimistic:function(e,t){var a=nt();return Ye!==null?Qu(a,Ye,e,t):(a.baseState=e,[e,a.queue.dispatch])},useMemoCache:Hs,useCacheRefresh:yd};Sd.useEffectEvent=ld;function $s(e,t,a,l){t=e.memoizedState,a=a(l,t),a=a==null?t:v({},t,a),e.memoizedState=a,e.lanes===0&&(e.updateQueue.baseState=a)}var Ws={enqueueSetState:function(e,t,a){e=e._reactInternals;var l=Lt(),o=Yn(l);o.payload=t,a!=null&&(o.callback=a),t=Xn(e,o,l),t!==null&&(Ot(t,e,l),kl(t,e,l))},enqueueReplaceState:function(e,t,a){e=e._reactInternals;var l=Lt(),o=Yn(l);o.tag=1,o.payload=t,a!=null&&(o.callback=a),t=Xn(e,o,l),t!==null&&(Ot(t,e,l),kl(t,e,l))},enqueueForceUpdate:function(e,t){e=e._reactInternals;var a=Lt(),l=Yn(a);l.tag=2,t!=null&&(l.callback=t),t=Xn(e,l,a),t!==null&&(Ot(t,e,a),kl(t,e,a))}};function xd(e,t,a,l,o,i,r){return e=e.stateNode,typeof e.shouldComponentUpdate=="function"?e.shouldComponentUpdate(l,i,r):t.prototype&&t.prototype.isPureReactComponent?!Nl(a,l)||!Nl(o,i):!0}function Td(e,t,a,l){e=t.state,typeof t.componentWillReceiveProps=="function"&&t.componentWillReceiveProps(a,l),typeof t.UNSAFE_componentWillReceiveProps=="function"&&t.UNSAFE_componentWillReceiveProps(a,l),t.state!==e&&Ws.enqueueReplaceState(t,t.state,null)}function wa(e,t){var a=t;if("ref"in t){a={};for(var l in t)l!=="ref"&&(a[l]=t[l])}if(e=e.defaultProps){a===t&&(a=v({},a));for(var o in e)a[o]===void 0&&(a[o]=e[o])}return a}function Ed(e){No(e)}function wd(e){console.error(e)}function Ad(e){No(e)}function $o(e,t){try{var a=e.onUncaughtError;a(t.value,{componentStack:t.stack})}catch(l){setTimeout(function(){throw l})}}function Nd(e,t,a){try{var l=e.onCaughtError;l(a.value,{componentStack:a.stack,errorBoundary:t.tag===1?t.stateNode:null})}catch(o){setTimeout(function(){throw o})}}function Fs(e,t,a){return a=Yn(a),a.tag=3,a.payload={element:null},a.callback=function(){$o(e,t)},a}function Cd(e){return e=Yn(e),e.tag=3,e}function jd(e,t,a,l){var o=a.type.getDerivedStateFromError;if(typeof o=="function"){var i=l.value;e.payload=function(){return o(i)},e.callback=function(){Nd(t,a,l)}}var r=a.stateNode;r!==null&&typeof r.componentDidCatch=="function"&&(e.callback=function(){Nd(t,a,l),typeof o!="function"&&(In===null?In=new Set([this]):In.add(this));var f=l.stack;this.componentDidCatch(l.value,{componentStack:f!==null?f:""})})}function Sm(e,t,a,l,o){if(a.flags|=32768,l!==null&&typeof l=="object"&&typeof l.then=="function"){if(t=a.alternate,t!==null&&Va(t,a,o,!0),a=Bt.current,a!==null){switch(a.tag){case 31:case 13:return $t===null?ci():a.alternate===null&&Fe===0&&(Fe=3),a.flags&=-257,a.flags|=65536,a.lanes=o,l===Uo?a.flags|=16384:(t=a.updateQueue,t===null?a.updateQueue=new Set([l]):t.add(l),Tc(e,l,o)),!1;case 22:return a.flags|=65536,l===Uo?a.flags|=16384:(t=a.updateQueue,t===null?(t={transitions:null,markerInstances:null,retryQueue:new Set([l])},a.updateQueue=t):(a=t.retryQueue,a===null?t.retryQueue=new Set([l]):a.add(l)),Tc(e,l,o)),!1}throw Error(u(435,a.tag))}return Tc(e,l,o),ci(),!1}if(Ce)return t=Bt.current,t!==null?((t.flags&65536)===0&&(t.flags|=256),t.flags|=65536,t.lanes=o,l!==gs&&(e=Error(u(422),{cause:l}),Ml(Qt(e,a)))):(l!==gs&&(t=Error(u(423),{cause:l}),Ml(Qt(t,a))),e=e.current.alternate,e.flags|=65536,o&=-o,e.lanes|=o,l=Qt(l,a),o=Fs(e.stateNode,l,o),Cs(e,o),Fe!==4&&(Fe=2)),!1;var i=Error(u(520),{cause:l});if(i=Qt(i,a),Il===null?Il=[i]:Il.push(i),Fe!==4&&(Fe=2),t===null)return!0;l=Qt(l,a),a=t;do{switch(a.tag){case 3:return a.flags|=65536,e=o&-o,a.lanes|=e,e=Fs(a.stateNode,l,e),Cs(a,e),!1;case 1:if(t=a.type,i=a.stateNode,(a.flags&128)===0&&(typeof t.getDerivedStateFromError=="function"||i!==null&&typeof i.componentDidCatch=="function"&&(In===null||!In.has(i))))return a.flags|=65536,o&=-o,a.lanes|=o,o=Cd(o),jd(o,e,a,l),Cs(a,o),!1}a=a.return}while(a!==null);return!1}var Ps=Error(u(461)),st=!1;function vt(e,t,a,l){t.child=e===null?zu(t,null,a,l):Ta(t,e.child,a,l)}function Md(e,t,a,l,o){a=a.render;var i=t.ref;if("ref"in l){var r={};for(var f in l)f!=="ref"&&(r[f]=l[f])}else r=l;return ba(t),l=Rs(e,t,a,r,i,o),f=ks(),e!==null&&!st?(Bs(e,t,o),xn(e,t,o)):(Ce&&f&&ms(t),t.flags|=1,vt(e,t,l,o),t.child)}function Dd(e,t,a,l,o){if(e===null){var i=a.type;return typeof i=="function"&&!fs(i)&&i.defaultProps===void 0&&a.compare===null?(t.tag=15,t.type=i,Od(e,t,i,l,o)):(e=Do(a.type,null,l,t,t.mode,o),e.ref=t.ref,e.return=t,t.child=e)}if(i=e.child,!sc(e,o)){var r=i.memoizedProps;if(a=a.compare,a=a!==null?a:Nl,a(r,l)&&e.ref===t.ref)return xn(e,t,o)}return t.flags|=1,e=yn(i,l),e.ref=t.ref,e.return=t,t.child=e}function Od(e,t,a,l,o){if(e!==null){var i=e.memoizedProps;if(Nl(i,l)&&e.ref===t.ref)if(st=!1,t.pendingProps=l=i,sc(e,o))(e.flags&131072)!==0&&(st=!0);else return t.lanes=e.lanes,xn(e,t,o)}return ec(e,t,a,l,o)}function zd(e,t,a,l){var o=l.children,i=e!==null?e.memoizedState:null;if(e===null&&t.stateNode===null&&(t.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),l.mode==="hidden"){if((t.flags&128)!==0){if(i=i!==null?i.baseLanes|a:a,e!==null){for(l=t.child=e.child,o=0;l!==null;)o=o|l.lanes|l.childLanes,l=l.sibling;l=o&~i}else l=0,t.child=null;return Rd(e,t,i,a,l)}if((a&536870912)!==0)t.memoizedState={baseLanes:0,cachePool:null},e!==null&&ko(t,i!==null?i.cachePool:null),i!==null?Bu(t,i):Ms(),Uu(t);else return l=t.lanes=536870912,Rd(e,t,i!==null?i.baseLanes|a:a,a,l)}else i!==null?(ko(t,i.cachePool),Bu(t,i),Zn(),t.memoizedState=null):(e!==null&&ko(t,null),Ms(),Zn());return vt(e,t,o,a),t.child}function Yl(e,t){return e!==null&&e.tag===22||t.stateNode!==null||(t.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),t.sibling}function Rd(e,t,a,l,o){var i=Es();return i=i===null?null:{parent:ot._currentValue,pool:i},t.memoizedState={baseLanes:a,cachePool:i},e!==null&&ko(t,null),Ms(),Uu(t),e!==null&&Va(e,t,l,!0),t.childLanes=o,null}function Wo(e,t){return t=Po({mode:t.mode,children:t.children},e.mode),t.ref=e.ref,e.child=t,t.return=e,t}function kd(e,t,a){return Ta(t,e.child,null,a),e=Wo(t,t.pendingProps),e.flags|=2,Ut(t),t.memoizedState=null,e}function xm(e,t,a){var l=t.pendingProps,o=(t.flags&128)!==0;if(t.flags&=-129,e===null){if(Ce){if(l.mode==="hidden")return e=Wo(t,l),t.lanes=536870912,Yl(null,e);if(Os(t),(e=Ve)?(e=Kf(e,It),e=e!==null&&e.data==="&"?e:null,e!==null&&(t.memoizedState={dehydrated:e,treeContext:Un!==null?{id:on,overflow:sn}:null,retryLane:536870912,hydrationErrors:null},a=vu(e),a.return=t,t.child=a,yt=t,Ve=null)):e=null,e===null)throw Gn(t);return t.lanes=536870912,null}return Wo(t,l)}var i=e.memoizedState;if(i!==null){var r=i.dehydrated;if(Os(t),o)if(t.flags&256)t.flags&=-257,t=kd(e,t,a);else if(t.memoizedState!==null)t.child=e.child,t.flags|=128,t=null;else throw Error(u(558));else if(st||Va(e,t,a,!1),o=(a&e.childLanes)!==0,st||o){if(l=Ze,l!==null&&(r=wr(l,a),r!==0&&r!==i.retryLane))throw i.retryLane=r,ma(e,r),Ot(l,e,r),Ps;ci(),t=kd(e,t,a)}else e=i.treeContext,Ve=Wt(r.nextSibling),yt=t,Ce=!0,Hn=null,It=!1,e!==null&&Su(t,e),t=Wo(t,l),t.flags|=4096;return t}return e=yn(e.child,{mode:l.mode,children:l.children}),e.ref=t.ref,t.child=e,e.return=t,e}function Fo(e,t){var a=t.ref;if(a===null)e!==null&&e.ref!==null&&(t.flags|=4194816);else{if(typeof a!="function"&&typeof a!="object")throw Error(u(284));(e===null||e.ref!==a)&&(t.flags|=4194816)}}function ec(e,t,a,l,o){return ba(t),a=Rs(e,t,a,l,void 0,o),l=ks(),e!==null&&!st?(Bs(e,t,o),xn(e,t,o)):(Ce&&l&&ms(t),t.flags|=1,vt(e,t,a,o),t.child)}function Bd(e,t,a,l,o,i){return ba(t),t.updateQueue=null,a=Gu(t,l,a,o),Hu(e),l=ks(),e!==null&&!st?(Bs(e,t,i),xn(e,t,i)):(Ce&&l&&ms(t),t.flags|=1,vt(e,t,a,i),t.child)}function Ud(e,t,a,l,o){if(ba(t),t.stateNode===null){var i=Xa,r=a.contextType;typeof r=="object"&&r!==null&&(i=gt(r)),i=new a(l,i),t.memoizedState=i.state!==null&&i.state!==void 0?i.state:null,i.updater=Ws,t.stateNode=i,i._reactInternals=t,i=t.stateNode,i.props=l,i.state=t.memoizedState,i.refs={},As(t),r=a.contextType,i.context=typeof r=="object"&&r!==null?gt(r):Xa,i.state=t.memoizedState,r=a.getDerivedStateFromProps,typeof r=="function"&&($s(t,a,r,l),i.state=t.memoizedState),typeof a.getDerivedStateFromProps=="function"||typeof i.getSnapshotBeforeUpdate=="function"||typeof i.UNSAFE_componentWillMount!="function"&&typeof i.componentWillMount!="function"||(r=i.state,typeof i.componentWillMount=="function"&&i.componentWillMount(),typeof i.UNSAFE_componentWillMount=="function"&&i.UNSAFE_componentWillMount(),r!==i.state&&Ws.enqueueReplaceState(i,i.state,null),Ul(t,l,i,o),Bl(),i.state=t.memoizedState),typeof i.componentDidMount=="function"&&(t.flags|=4194308),l=!0}else if(e===null){i=t.stateNode;var f=t.memoizedProps,_=wa(a,f);i.props=_;var R=i.context,X=a.contextType;r=Xa,typeof X=="object"&&X!==null&&(r=gt(X));var Q=a.getDerivedStateFromProps;X=typeof Q=="function"||typeof i.getSnapshotBeforeUpdate=="function",f=t.pendingProps!==f,X||typeof i.UNSAFE_componentWillReceiveProps!="function"&&typeof i.componentWillReceiveProps!="function"||(f||R!==r)&&Td(t,i,l,r),qn=!1;var U=t.memoizedState;i.state=U,Ul(t,l,i,o),Bl(),R=t.memoizedState,f||U!==R||qn?(typeof Q=="function"&&($s(t,a,Q,l),R=t.memoizedState),(_=qn||xd(t,a,_,l,U,R,r))?(X||typeof i.UNSAFE_componentWillMount!="function"&&typeof i.componentWillMount!="function"||(typeof i.componentWillMount=="function"&&i.componentWillMount(),typeof i.UNSAFE_componentWillMount=="function"&&i.UNSAFE_componentWillMount()),typeof i.componentDidMount=="function"&&(t.flags|=4194308)):(typeof i.componentDidMount=="function"&&(t.flags|=4194308),t.memoizedProps=l,t.memoizedState=R),i.props=l,i.state=R,i.context=r,l=_):(typeof i.componentDidMount=="function"&&(t.flags|=4194308),l=!1)}else{i=t.stateNode,Ns(e,t),r=t.memoizedProps,X=wa(a,r),i.props=X,Q=t.pendingProps,U=i.context,R=a.contextType,_=Xa,typeof R=="object"&&R!==null&&(_=gt(R)),f=a.getDerivedStateFromProps,(R=typeof f=="function"||typeof i.getSnapshotBeforeUpdate=="function")||typeof i.UNSAFE_componentWillReceiveProps!="function"&&typeof i.componentWillReceiveProps!="function"||(r!==Q||U!==_)&&Td(t,i,l,_),qn=!1,U=t.memoizedState,i.state=U,Ul(t,l,i,o),Bl();var G=t.memoizedState;r!==Q||U!==G||qn||e!==null&&e.dependencies!==null&&zo(e.dependencies)?(typeof f=="function"&&($s(t,a,f,l),G=t.memoizedState),(X=qn||xd(t,a,X,l,U,G,_)||e!==null&&e.dependencies!==null&&zo(e.dependencies))?(R||typeof i.UNSAFE_componentWillUpdate!="function"&&typeof i.componentWillUpdate!="function"||(typeof i.componentWillUpdate=="function"&&i.componentWillUpdate(l,G,_),typeof i.UNSAFE_componentWillUpdate=="function"&&i.UNSAFE_componentWillUpdate(l,G,_)),typeof i.componentDidUpdate=="function"&&(t.flags|=4),typeof i.getSnapshotBeforeUpdate=="function"&&(t.flags|=1024)):(typeof i.componentDidUpdate!="function"||r===e.memoizedProps&&U===e.memoizedState||(t.flags|=4),typeof i.getSnapshotBeforeUpdate!="function"||r===e.memoizedProps&&U===e.memoizedState||(t.flags|=1024),t.memoizedProps=l,t.memoizedState=G),i.props=l,i.state=G,i.context=_,l=X):(typeof i.componentDidUpdate!="function"||r===e.memoizedProps&&U===e.memoizedState||(t.flags|=4),typeof i.getSnapshotBeforeUpdate!="function"||r===e.memoizedProps&&U===e.memoizedState||(t.flags|=1024),l=!1)}return i=l,Fo(e,t),l=(t.flags&128)!==0,i||l?(i=t.stateNode,a=l&&typeof a.getDerivedStateFromError!="function"?null:i.render(),t.flags|=1,e!==null&&l?(t.child=Ta(t,e.child,null,o),t.child=Ta(t,null,a,o)):vt(e,t,a,o),t.memoizedState=i.state,e=t.child):e=xn(e,t,o),e}function Hd(e,t,a,l){return ga(),t.flags|=256,vt(e,t,a,l),t.child}var tc={dehydrated:null,treeContext:null,retryLane:0,hydrationErrors:null};function nc(e){return{baseLanes:e,cachePool:Nu()}}function ac(e,t,a){return e=e!==null?e.childLanes&~a:0,t&&(e|=Gt),e}function Gd(e,t,a){var l=t.pendingProps,o=!1,i=(t.flags&128)!==0,r;if((r=i)||(r=e!==null&&e.memoizedState===null?!1:(tt.current&2)!==0),r&&(o=!0,t.flags&=-129),r=(t.flags&32)!==0,t.flags&=-33,e===null){if(Ce){if(o?Jn(t):Zn(),(e=Ve)?(e=Kf(e,It),e=e!==null&&e.data!=="&"?e:null,e!==null&&(t.memoizedState={dehydrated:e,treeContext:Un!==null?{id:on,overflow:sn}:null,retryLane:536870912,hydrationErrors:null},a=vu(e),a.return=t,t.child=a,yt=t,Ve=null)):e=null,e===null)throw Gn(t);return Gc(e)?t.lanes=32:t.lanes=536870912,null}var f=l.children;return l=l.fallback,o?(Zn(),o=t.mode,f=Po({mode:"hidden",children:f},o),l=ya(l,o,a,null),f.return=t,l.return=t,f.sibling=l,t.child=f,l=t.child,l.memoizedState=nc(a),l.childLanes=ac(e,r,a),t.memoizedState=tc,Yl(null,l)):(Jn(t),lc(t,f))}var _=e.memoizedState;if(_!==null&&(f=_.dehydrated,f!==null)){if(i)t.flags&256?(Jn(t),t.flags&=-257,t=oc(e,t,a)):t.memoizedState!==null?(Zn(),t.child=e.child,t.flags|=128,t=null):(Zn(),f=l.fallback,o=t.mode,l=Po({mode:"visible",children:l.children},o),f=ya(f,o,a,null),f.flags|=2,l.return=t,f.return=t,l.sibling=f,t.child=l,Ta(t,e.child,null,a),l=t.child,l.memoizedState=nc(a),l.childLanes=ac(e,r,a),t.memoizedState=tc,t=Yl(null,l));else if(Jn(t),Gc(f)){if(r=f.nextSibling&&f.nextSibling.dataset,r)var R=r.dgst;r=R,l=Error(u(419)),l.stack="",l.digest=r,Ml({value:l,source:null,stack:null}),t=oc(e,t,a)}else if(st||Va(e,t,a,!1),r=(a&e.childLanes)!==0,st||r){if(r=Ze,r!==null&&(l=wr(r,a),l!==0&&l!==_.retryLane))throw _.retryLane=l,ma(e,l),Ot(r,e,l),Ps;Hc(f)||ci(),t=oc(e,t,a)}else Hc(f)?(t.flags|=192,t.child=e.child,t=null):(e=_.treeContext,Ve=Wt(f.nextSibling),yt=t,Ce=!0,Hn=null,It=!1,e!==null&&Su(t,e),t=lc(t,l.children),t.flags|=4096);return t}return o?(Zn(),f=l.fallback,o=t.mode,_=e.child,R=_.sibling,l=yn(_,{mode:"hidden",children:l.children}),l.subtreeFlags=_.subtreeFlags&65011712,R!==null?f=yn(R,f):(f=ya(f,o,a,null),f.flags|=2),f.return=t,l.return=t,l.sibling=f,t.child=l,Yl(null,l),l=t.child,f=e.child.memoizedState,f===null?f=nc(a):(o=f.cachePool,o!==null?(_=ot._currentValue,o=o.parent!==_?{parent:_,pool:_}:o):o=Nu(),f={baseLanes:f.baseLanes|a,cachePool:o}),l.memoizedState=f,l.childLanes=ac(e,r,a),t.memoizedState=tc,Yl(e.child,l)):(Jn(t),a=e.child,e=a.sibling,a=yn(a,{mode:"visible",children:l.children}),a.return=t,a.sibling=null,e!==null&&(r=t.deletions,r===null?(t.deletions=[e],t.flags|=16):r.push(e)),t.child=a,t.memoizedState=null,a)}function lc(e,t){return t=Po({mode:"visible",children:t},e.mode),t.return=e,e.child=t}function Po(e,t){return e=kt(22,e,null,t),e.lanes=0,e}function oc(e,t,a){return Ta(t,e.child,null,a),e=lc(t,t.pendingProps.children),e.flags|=2,t.memoizedState=null,e}function Ld(e,t,a){e.lanes|=t;var l=e.alternate;l!==null&&(l.lanes|=t),_s(e.return,t,a)}function ic(e,t,a,l,o,i){var r=e.memoizedState;r===null?e.memoizedState={isBackwards:t,rendering:null,renderingStartTime:0,last:l,tail:a,tailMode:o,treeForkCount:i}:(r.isBackwards=t,r.rendering=null,r.renderingStartTime=0,r.last=l,r.tail=a,r.tailMode=o,r.treeForkCount=i)}function qd(e,t,a){var l=t.pendingProps,o=l.revealOrder,i=l.tail;l=l.children;var r=tt.current,f=(r&2)!==0;if(f?(r=r&1|2,t.flags|=128):r&=1,P(tt,r),vt(e,t,l,a),l=Ce?jl:0,!f&&e!==null&&(e.flags&128)!==0)e:for(e=t.child;e!==null;){if(e.tag===13)e.memoizedState!==null&&Ld(e,a,t);else if(e.tag===19)Ld(e,a,t);else if(e.child!==null){e.child.return=e,e=e.child;continue}if(e===t)break e;for(;e.sibling===null;){if(e.return===null||e.return===t)break e;e=e.return}e.sibling.return=e.return,e=e.sibling}switch(o){case"forwards":for(a=t.child,o=null;a!==null;)e=a.alternate,e!==null&&qo(e)===null&&(o=a),a=a.sibling;a=o,a===null?(o=t.child,t.child=null):(o=a.sibling,a.sibling=null),ic(t,!1,o,a,i,l);break;case"backwards":case"unstable_legacy-backwards":for(a=null,o=t.child,t.child=null;o!==null;){if(e=o.alternate,e!==null&&qo(e)===null){t.child=o;break}e=o.sibling,o.sibling=a,a=o,o=e}ic(t,!0,a,null,i,l);break;case"together":ic(t,!1,null,null,void 0,l);break;default:t.memoizedState=null}return t.child}function xn(e,t,a){if(e!==null&&(t.dependencies=e.dependencies),Kn|=t.lanes,(a&t.childLanes)===0)if(e!==null){if(Va(e,t,a,!1),(a&t.childLanes)===0)return null}else return null;if(e!==null&&t.child!==e.child)throw Error(u(153));if(t.child!==null){for(e=t.child,a=yn(e,e.pendingProps),t.child=a,a.return=t;e.sibling!==null;)e=e.sibling,a=a.sibling=yn(e,e.pendingProps),a.return=t;a.sibling=null}return t.child}function sc(e,t){return(e.lanes&t)!==0?!0:(e=e.dependencies,!!(e!==null&&zo(e)))}function Tm(e,t,a){switch(t.tag){case 3:qe(t,t.stateNode.containerInfo),Ln(t,ot,e.memoizedState.cache),ga();break;case 27:case 5:le(t);break;case 4:qe(t,t.stateNode.containerInfo);break;case 10:Ln(t,t.type,t.memoizedProps.value);break;case 31:if(t.memoizedState!==null)return t.flags|=128,Os(t),null;break;case 13:var l=t.memoizedState;if(l!==null)return l.dehydrated!==null?(Jn(t),t.flags|=128,null):(a&t.child.childLanes)!==0?Gd(e,t,a):(Jn(t),e=xn(e,t,a),e!==null?e.sibling:null);Jn(t);break;case 19:var o=(e.flags&128)!==0;if(l=(a&t.childLanes)!==0,l||(Va(e,t,a,!1),l=(a&t.childLanes)!==0),o){if(l)return qd(e,t,a);t.flags|=128}if(o=t.memoizedState,o!==null&&(o.rendering=null,o.tail=null,o.lastEffect=null),P(tt,tt.current),l)break;return null;case 22:return t.lanes=0,zd(e,t,a,t.pendingProps);case 24:Ln(t,ot,e.memoizedState.cache)}return xn(e,t,a)}function Yd(e,t,a){if(e!==null)if(e.memoizedProps!==t.pendingProps)st=!0;else{if(!sc(e,a)&&(t.flags&128)===0)return st=!1,Tm(e,t,a);st=(e.flags&131072)!==0}else st=!1,Ce&&(t.flags&1048576)!==0&&_u(t,jl,t.index);switch(t.lanes=0,t.tag){case 16:e:{var l=t.pendingProps;if(e=Sa(t.elementType),t.type=e,typeof e=="function")fs(e)?(l=wa(e,l),t.tag=1,t=Ud(null,t,e,l,a)):(t.tag=0,t=ec(null,t,e,l,a));else{if(e!=null){var o=e.$$typeof;if(o===L){t.tag=11,t=Md(null,t,e,l,a);break e}else if(o===B){t.tag=14,t=Dd(null,t,e,l,a);break e}}throw t=re(e)||e,Error(u(306,t,""))}}return t;case 0:return ec(e,t,t.type,t.pendingProps,a);case 1:return l=t.type,o=wa(l,t.pendingProps),Ud(e,t,l,o,a);case 3:e:{if(qe(t,t.stateNode.containerInfo),e===null)throw Error(u(387));l=t.pendingProps;var i=t.memoizedState;o=i.element,Ns(e,t),Ul(t,l,null,a);var r=t.memoizedState;if(l=r.cache,Ln(t,ot,l),l!==i.cache&&Ss(t,[ot],a,!0),Bl(),l=r.element,i.isDehydrated)if(i={element:l,isDehydrated:!1,cache:r.cache},t.updateQueue.baseState=i,t.memoizedState=i,t.flags&256){t=Hd(e,t,l,a);break e}else if(l!==o){o=Qt(Error(u(424)),t),Ml(o),t=Hd(e,t,l,a);break e}else{switch(e=t.stateNode.containerInfo,e.nodeType){case 9:e=e.body;break;default:e=e.nodeName==="HTML"?e.ownerDocument.body:e}for(Ve=Wt(e.firstChild),yt=t,Ce=!0,Hn=null,It=!0,a=zu(t,null,l,a),t.child=a;a;)a.flags=a.flags&-3|4096,a=a.sibling}else{if(ga(),l===o){t=xn(e,t,a);break e}vt(e,t,l,a)}t=t.child}return t;case 26:return Fo(e,t),e===null?(a=ep(t.type,null,t.pendingProps,null))?t.memoizedState=a:Ce||(a=t.type,e=t.pendingProps,l=mi(xe.current).createElement(a),l[mt]=t,l[At]=e,bt(l,a,e),ft(l),t.stateNode=l):t.memoizedState=ep(t.type,e.memoizedProps,t.pendingProps,e.memoizedState),null;case 27:return le(t),e===null&&Ce&&(l=t.stateNode=Wf(t.type,t.pendingProps,xe.current),yt=t,It=!0,o=Ve,Pn(t.type)?(Lc=o,Ve=Wt(l.firstChild)):Ve=o),vt(e,t,t.pendingProps.children,a),Fo(e,t),e===null&&(t.flags|=4194304),t.child;case 5:return e===null&&Ce&&((o=l=Ve)&&(l=Pm(l,t.type,t.pendingProps,It),l!==null?(t.stateNode=l,yt=t,Ve=Wt(l.firstChild),It=!1,o=!0):o=!1),o||Gn(t)),le(t),o=t.type,i=t.pendingProps,r=e!==null?e.memoizedProps:null,l=i.children,kc(o,i)?l=null:r!==null&&kc(o,r)&&(t.flags|=32),t.memoizedState!==null&&(o=Rs(e,t,hm,null,null,a),ao._currentValue=o),Fo(e,t),vt(e,t,l,a),t.child;case 6:return e===null&&Ce&&((e=a=Ve)&&(a=ey(a,t.pendingProps,It),a!==null?(t.stateNode=a,yt=t,Ve=null,e=!0):e=!1),e||Gn(t)),null;case 13:return Gd(e,t,a);case 4:return qe(t,t.stateNode.containerInfo),l=t.pendingProps,e===null?t.child=Ta(t,null,l,a):vt(e,t,l,a),t.child;case 11:return Md(e,t,t.type,t.pendingProps,a);case 7:return vt(e,t,t.pendingProps,a),t.child;case 8:return vt(e,t,t.pendingProps.children,a),t.child;case 12:return vt(e,t,t.pendingProps.children,a),t.child;case 10:return l=t.pendingProps,Ln(t,t.type,l.value),vt(e,t,l.children,a),t.child;case 9:return o=t.type._context,l=t.pendingProps.children,ba(t),o=gt(o),l=l(o),t.flags|=1,vt(e,t,l,a),t.child;case 14:return Dd(e,t,t.type,t.pendingProps,a);case 15:return Od(e,t,t.type,t.pendingProps,a);case 19:return qd(e,t,a);case 31:return xm(e,t,a);case 22:return zd(e,t,a,t.pendingProps);case 24:return ba(t),l=gt(ot),e===null?(o=Es(),o===null&&(o=Ze,i=xs(),o.pooledCache=i,i.refCount++,i!==null&&(o.pooledCacheLanes|=a),o=i),t.memoizedState={parent:l,cache:o},As(t),Ln(t,ot,o)):((e.lanes&a)!==0&&(Ns(e,t),Ul(t,null,null,a),Bl()),o=e.memoizedState,i=t.memoizedState,o.parent!==l?(o={parent:l,cache:l},t.memoizedState=o,t.lanes===0&&(t.memoizedState=t.updateQueue.baseState=o),Ln(t,ot,l)):(l=i.cache,Ln(t,ot,l),l!==o.cache&&Ss(t,[ot],a,!0))),vt(e,t,t.pendingProps.children,a),t.child;case 29:throw t.pendingProps}throw Error(u(156,t.tag))}function Tn(e){e.flags|=4}function cc(e,t,a,l,o){if((t=(e.mode&32)!==0)&&(t=!1),t){if(e.flags|=16777216,(o&335544128)===o)if(e.stateNode.complete)e.flags|=8192;else if(yf())e.flags|=8192;else throw xa=Uo,ws}else e.flags&=-16777217}function Xd(e,t){if(t.type!=="stylesheet"||(t.state.loading&4)!==0)e.flags&=-16777217;else if(e.flags|=16777216,!op(t))if(yf())e.flags|=8192;else throw xa=Uo,ws}function ei(e,t){t!==null&&(e.flags|=4),e.flags&16384&&(t=e.tag!==22?xr():536870912,e.lanes|=t,ol|=t)}function Xl(e,t){if(!Ce)switch(e.tailMode){case"hidden":t=e.tail;for(var a=null;t!==null;)t.alternate!==null&&(a=t),t=t.sibling;a===null?e.tail=null:a.sibling=null;break;case"collapsed":a=e.tail;for(var l=null;a!==null;)a.alternate!==null&&(l=a),a=a.sibling;l===null?t||e.tail===null?e.tail=null:e.tail.sibling=null:l.sibling=null}}function Ke(e){var t=e.alternate!==null&&e.alternate.child===e.child,a=0,l=0;if(t)for(var o=e.child;o!==null;)a|=o.lanes|o.childLanes,l|=o.subtreeFlags&65011712,l|=o.flags&65011712,o.return=e,o=o.sibling;else for(o=e.child;o!==null;)a|=o.lanes|o.childLanes,l|=o.subtreeFlags,l|=o.flags,o.return=e,o=o.sibling;return e.subtreeFlags|=l,e.childLanes=a,t}function Em(e,t,a){var l=t.pendingProps;switch(ys(t),t.tag){case 16:case 15:case 0:case 11:case 7:case 8:case 12:case 9:case 14:return Ke(t),null;case 1:return Ke(t),null;case 3:return a=t.stateNode,l=null,e!==null&&(l=e.memoizedState.cache),t.memoizedState.cache!==l&&(t.flags|=2048),bn(ot),Me(),a.pendingContext&&(a.context=a.pendingContext,a.pendingContext=null),(e===null||e.child===null)&&(Qa(t)?Tn(t):e===null||e.memoizedState.isDehydrated&&(t.flags&256)===0||(t.flags|=1024,vs())),Ke(t),null;case 26:var o=t.type,i=t.memoizedState;return e===null?(Tn(t),i!==null?(Ke(t),Xd(t,i)):(Ke(t),cc(t,o,null,l,a))):i?i!==e.memoizedState?(Tn(t),Ke(t),Xd(t,i)):(Ke(t),t.flags&=-16777217):(e=e.memoizedProps,e!==l&&Tn(t),Ke(t),cc(t,o,e,l,a)),null;case 27:if(de(t),a=xe.current,o=t.type,e!==null&&t.stateNode!=null)e.memoizedProps!==l&&Tn(t);else{if(!l){if(t.stateNode===null)throw Error(u(166));return Ke(t),null}e=me.current,Qa(t)?xu(t):(e=Wf(o,l,a),t.stateNode=e,Tn(t))}return Ke(t),null;case 5:if(de(t),o=t.type,e!==null&&t.stateNode!=null)e.memoizedProps!==l&&Tn(t);else{if(!l){if(t.stateNode===null)throw Error(u(166));return Ke(t),null}if(i=me.current,Qa(t))xu(t);else{var r=mi(xe.current);switch(i){case 1:i=r.createElementNS("http://www.w3.org/2000/svg",o);break;case 2:i=r.createElementNS("http://www.w3.org/1998/Math/MathML",o);break;default:switch(o){case"svg":i=r.createElementNS("http://www.w3.org/2000/svg",o);break;case"math":i=r.createElementNS("http://www.w3.org/1998/Math/MathML",o);break;case"script":i=r.createElement("div"),i.innerHTML="<script><\/script>",i=i.removeChild(i.firstChild);break;case"select":i=typeof l.is=="string"?r.createElement("select",{is:l.is}):r.createElement("select"),l.multiple?i.multiple=!0:l.size&&(i.size=l.size);break;default:i=typeof l.is=="string"?r.createElement(o,{is:l.is}):r.createElement(o)}}i[mt]=t,i[At]=l;e:for(r=t.child;r!==null;){if(r.tag===5||r.tag===6)i.appendChild(r.stateNode);else if(r.tag!==4&&r.tag!==27&&r.child!==null){r.child.return=r,r=r.child;continue}if(r===t)break e;for(;r.sibling===null;){if(r.return===null||r.return===t)break e;r=r.return}r.sibling.return=r.return,r=r.sibling}t.stateNode=i;e:switch(bt(i,o,l),o){case"button":case"input":case"select":case"textarea":l=!!l.autoFocus;break e;case"img":l=!0;break e;default:l=!1}l&&Tn(t)}}return Ke(t),cc(t,t.type,e===null?null:e.memoizedProps,t.pendingProps,a),null;case 6:if(e&&t.stateNode!=null)e.memoizedProps!==l&&Tn(t);else{if(typeof l!="string"&&t.stateNode===null)throw Error(u(166));if(e=xe.current,Qa(t)){if(e=t.stateNode,a=t.memoizedProps,l=null,o=yt,o!==null)switch(o.tag){case 27:case 5:l=o.memoizedProps}e[mt]=t,e=!!(e.nodeValue===a||l!==null&&l.suppressHydrationWarning===!0||Lf(e.nodeValue,a)),e||Gn(t,!0)}else e=mi(e).createTextNode(l),e[mt]=t,t.stateNode=e}return Ke(t),null;case 31:if(a=t.memoizedState,e===null||e.memoizedState!==null){if(l=Qa(t),a!==null){if(e===null){if(!l)throw Error(u(318));if(e=t.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(u(557));e[mt]=t}else ga(),(t.flags&128)===0&&(t.memoizedState=null),t.flags|=4;Ke(t),e=!1}else a=vs(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=a),e=!0;if(!e)return t.flags&256?(Ut(t),t):(Ut(t),null);if((t.flags&128)!==0)throw Error(u(558))}return Ke(t),null;case 13:if(l=t.memoizedState,e===null||e.memoizedState!==null&&e.memoizedState.dehydrated!==null){if(o=Qa(t),l!==null&&l.dehydrated!==null){if(e===null){if(!o)throw Error(u(318));if(o=t.memoizedState,o=o!==null?o.dehydrated:null,!o)throw Error(u(317));o[mt]=t}else ga(),(t.flags&128)===0&&(t.memoizedState=null),t.flags|=4;Ke(t),o=!1}else o=vs(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=o),o=!0;if(!o)return t.flags&256?(Ut(t),t):(Ut(t),null)}return Ut(t),(t.flags&128)!==0?(t.lanes=a,t):(a=l!==null,e=e!==null&&e.memoizedState!==null,a&&(l=t.child,o=null,l.alternate!==null&&l.alternate.memoizedState!==null&&l.alternate.memoizedState.cachePool!==null&&(o=l.alternate.memoizedState.cachePool.pool),i=null,l.memoizedState!==null&&l.memoizedState.cachePool!==null&&(i=l.memoizedState.cachePool.pool),i!==o&&(l.flags|=2048)),a!==e&&a&&(t.child.flags|=8192),ei(t,t.updateQueue),Ke(t),null);case 4:return Me(),e===null&&Mc(t.stateNode.containerInfo),Ke(t),null;case 10:return bn(t.type),Ke(t),null;case 19:if(te(tt),l=t.memoizedState,l===null)return Ke(t),null;if(o=(t.flags&128)!==0,i=l.rendering,i===null)if(o)Xl(l,!1);else{if(Fe!==0||e!==null&&(e.flags&128)!==0)for(e=t.child;e!==null;){if(i=qo(e),i!==null){for(t.flags|=128,Xl(l,!1),e=i.updateQueue,t.updateQueue=e,ei(t,e),t.subtreeFlags=0,e=a,a=t.child;a!==null;)gu(a,e),a=a.sibling;return P(tt,tt.current&1|2),Ce&&gn(t,l.treeForkCount),t.child}e=e.sibling}l.tail!==null&&ge()>oi&&(t.flags|=128,o=!0,Xl(l,!1),t.lanes=4194304)}else{if(!o)if(e=qo(i),e!==null){if(t.flags|=128,o=!0,e=e.updateQueue,t.updateQueue=e,ei(t,e),Xl(l,!0),l.tail===null&&l.tailMode==="hidden"&&!i.alternate&&!Ce)return Ke(t),null}else 2*ge()-l.renderingStartTime>oi&&a!==536870912&&(t.flags|=128,o=!0,Xl(l,!1),t.lanes=4194304);l.isBackwards?(i.sibling=t.child,t.child=i):(e=l.last,e!==null?e.sibling=i:t.child=i,l.last=i)}return l.tail!==null?(e=l.tail,l.rendering=e,l.tail=e.sibling,l.renderingStartTime=ge(),e.sibling=null,a=tt.current,P(tt,o?a&1|2:a&1),Ce&&gn(t,l.treeForkCount),e):(Ke(t),null);case 22:case 23:return Ut(t),Ds(),l=t.memoizedState!==null,e!==null?e.memoizedState!==null!==l&&(t.flags|=8192):l&&(t.flags|=8192),l?(a&536870912)!==0&&(t.flags&128)===0&&(Ke(t),t.subtreeFlags&6&&(t.flags|=8192)):Ke(t),a=t.updateQueue,a!==null&&ei(t,a.retryQueue),a=null,e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(a=e.memoizedState.cachePool.pool),l=null,t.memoizedState!==null&&t.memoizedState.cachePool!==null&&(l=t.memoizedState.cachePool.pool),l!==a&&(t.flags|=2048),e!==null&&te(_a),null;case 24:return a=null,e!==null&&(a=e.memoizedState.cache),t.memoizedState.cache!==a&&(t.flags|=2048),bn(ot),Ke(t),null;case 25:return null;case 30:return null}throw Error(u(156,t.tag))}function wm(e,t){switch(ys(t),t.tag){case 1:return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 3:return bn(ot),Me(),e=t.flags,(e&65536)!==0&&(e&128)===0?(t.flags=e&-65537|128,t):null;case 26:case 27:case 5:return de(t),null;case 31:if(t.memoizedState!==null){if(Ut(t),t.alternate===null)throw Error(u(340));ga()}return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 13:if(Ut(t),e=t.memoizedState,e!==null&&e.dehydrated!==null){if(t.alternate===null)throw Error(u(340));ga()}return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 19:return te(tt),null;case 4:return Me(),null;case 10:return bn(t.type),null;case 22:case 23:return Ut(t),Ds(),e!==null&&te(_a),e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 24:return bn(ot),null;case 25:return null;default:return null}}function Jd(e,t){switch(ys(t),t.tag){case 3:bn(ot),Me();break;case 26:case 27:case 5:de(t);break;case 4:Me();break;case 31:t.memoizedState!==null&&Ut(t);break;case 13:Ut(t);break;case 19:te(tt);break;case 10:bn(t.type);break;case 22:case 23:Ut(t),Ds(),e!==null&&te(_a);break;case 24:bn(ot)}}function Jl(e,t){try{var a=t.updateQueue,l=a!==null?a.lastEffect:null;if(l!==null){var o=l.next;a=o;do{if((a.tag&e)===e){l=void 0;var i=a.create,r=a.inst;l=i(),r.destroy=l}a=a.next}while(a!==o)}}catch(f){Ge(t,t.return,f)}}function Qn(e,t,a){try{var l=t.updateQueue,o=l!==null?l.lastEffect:null;if(o!==null){var i=o.next;l=i;do{if((l.tag&e)===e){var r=l.inst,f=r.destroy;if(f!==void 0){r.destroy=void 0,o=t;var _=a,R=f;try{R()}catch(X){Ge(o,_,X)}}}l=l.next}while(l!==i)}}catch(X){Ge(t,t.return,X)}}function Zd(e){var t=e.updateQueue;if(t!==null){var a=e.stateNode;try{ku(t,a)}catch(l){Ge(e,e.return,l)}}}function Qd(e,t,a){a.props=wa(e.type,e.memoizedProps),a.state=e.memoizedState;try{a.componentWillUnmount()}catch(l){Ge(e,t,l)}}function Zl(e,t){try{var a=e.ref;if(a!==null){switch(e.tag){case 26:case 27:case 5:var l=e.stateNode;break;case 30:l=e.stateNode;break;default:l=e.stateNode}typeof a=="function"?e.refCleanup=a(l):a.current=l}}catch(o){Ge(e,t,o)}}function cn(e,t){var a=e.ref,l=e.refCleanup;if(a!==null)if(typeof l=="function")try{l()}catch(o){Ge(e,t,o)}finally{e.refCleanup=null,e=e.alternate,e!=null&&(e.refCleanup=null)}else if(typeof a=="function")try{a(null)}catch(o){Ge(e,t,o)}else a.current=null}function Vd(e){var t=e.type,a=e.memoizedProps,l=e.stateNode;try{e:switch(t){case"button":case"input":case"select":case"textarea":a.autoFocus&&l.focus();break e;case"img":a.src?l.src=a.src:a.srcSet&&(l.srcset=a.srcSet)}}catch(o){Ge(e,e.return,o)}}function rc(e,t,a){try{var l=e.stateNode;Vm(l,e.type,a,t),l[At]=t}catch(o){Ge(e,e.return,o)}}function Kd(e){return e.tag===5||e.tag===3||e.tag===26||e.tag===27&&Pn(e.type)||e.tag===4}function uc(e){e:for(;;){for(;e.sibling===null;){if(e.return===null||Kd(e.return))return null;e=e.return}for(e.sibling.return=e.return,e=e.sibling;e.tag!==5&&e.tag!==6&&e.tag!==18;){if(e.tag===27&&Pn(e.type)||e.flags&2||e.child===null||e.tag===4)continue e;e.child.return=e,e=e.child}if(!(e.flags&2))return e.stateNode}}function dc(e,t,a){var l=e.tag;if(l===5||l===6)e=e.stateNode,t?(a.nodeType===9?a.body:a.nodeName==="HTML"?a.ownerDocument.body:a).insertBefore(e,t):(t=a.nodeType===9?a.body:a.nodeName==="HTML"?a.ownerDocument.body:a,t.appendChild(e),a=a._reactRootContainer,a!=null||t.onclick!==null||(t.onclick=hn));else if(l!==4&&(l===27&&Pn(e.type)&&(a=e.stateNode,t=null),e=e.child,e!==null))for(dc(e,t,a),e=e.sibling;e!==null;)dc(e,t,a),e=e.sibling}function ti(e,t,a){var l=e.tag;if(l===5||l===6)e=e.stateNode,t?a.insertBefore(e,t):a.appendChild(e);else if(l!==4&&(l===27&&Pn(e.type)&&(a=e.stateNode),e=e.child,e!==null))for(ti(e,t,a),e=e.sibling;e!==null;)ti(e,t,a),e=e.sibling}function Id(e){var t=e.stateNode,a=e.memoizedProps;try{for(var l=e.type,o=t.attributes;o.length;)t.removeAttributeNode(o[0]);bt(t,l,a),t[mt]=e,t[At]=a}catch(i){Ge(e,e.return,i)}}var En=!1,ct=!1,fc=!1,$d=typeof WeakSet=="function"?WeakSet:Set,pt=null;function Am(e,t){if(e=e.containerInfo,zc=xi,e=cu(e),os(e)){if("selectionStart"in e)var a={start:e.selectionStart,end:e.selectionEnd};else e:{a=(a=e.ownerDocument)&&a.defaultView||window;var l=a.getSelection&&a.getSelection();if(l&&l.rangeCount!==0){a=l.anchorNode;var o=l.anchorOffset,i=l.focusNode;l=l.focusOffset;try{a.nodeType,i.nodeType}catch{a=null;break e}var r=0,f=-1,_=-1,R=0,X=0,Q=e,U=null;t:for(;;){for(var G;Q!==a||o!==0&&Q.nodeType!==3||(f=r+o),Q!==i||l!==0&&Q.nodeType!==3||(_=r+l),Q.nodeType===3&&(r+=Q.nodeValue.length),(G=Q.firstChild)!==null;)U=Q,Q=G;for(;;){if(Q===e)break t;if(U===a&&++R===o&&(f=r),U===i&&++X===l&&(_=r),(G=Q.nextSibling)!==null)break;Q=U,U=Q.parentNode}Q=G}a=f===-1||_===-1?null:{start:f,end:_}}else a=null}a=a||{start:0,end:0}}else a=null;for(Rc={focusedElem:e,selectionRange:a},xi=!1,pt=t;pt!==null;)if(t=pt,e=t.child,(t.subtreeFlags&1028)!==0&&e!==null)e.return=t,pt=e;else for(;pt!==null;){switch(t=pt,i=t.alternate,e=t.flags,t.tag){case 0:if((e&4)!==0&&(e=t.updateQueue,e=e!==null?e.events:null,e!==null))for(a=0;a<e.length;a++)o=e[a],o.ref.impl=o.nextImpl;break;case 11:case 15:break;case 1:if((e&1024)!==0&&i!==null){e=void 0,a=t,o=i.memoizedProps,i=i.memoizedState,l=a.stateNode;try{var ae=wa(a.type,o);e=l.getSnapshotBeforeUpdate(ae,i),l.__reactInternalSnapshotBeforeUpdate=e}catch(fe){Ge(a,a.return,fe)}}break;case 3:if((e&1024)!==0){if(e=t.stateNode.containerInfo,a=e.nodeType,a===9)Uc(e);else if(a===1)switch(e.nodeName){case"HEAD":case"HTML":case"BODY":Uc(e);break;default:e.textContent=""}}break;case 5:case 26:case 27:case 6:case 4:case 17:break;default:if((e&1024)!==0)throw Error(u(163))}if(e=t.sibling,e!==null){e.return=t.return,pt=e;break}pt=t.return}}function Wd(e,t,a){var l=a.flags;switch(a.tag){case 0:case 11:case 15:An(e,a),l&4&&Jl(5,a);break;case 1:if(An(e,a),l&4)if(e=a.stateNode,t===null)try{e.componentDidMount()}catch(r){Ge(a,a.return,r)}else{var o=wa(a.type,t.memoizedProps);t=t.memoizedState;try{e.componentDidUpdate(o,t,e.__reactInternalSnapshotBeforeUpdate)}catch(r){Ge(a,a.return,r)}}l&64&&Zd(a),l&512&&Zl(a,a.return);break;case 3:if(An(e,a),l&64&&(e=a.updateQueue,e!==null)){if(t=null,a.child!==null)switch(a.child.tag){case 27:case 5:t=a.child.stateNode;break;case 1:t=a.child.stateNode}try{ku(e,t)}catch(r){Ge(a,a.return,r)}}break;case 27:t===null&&l&4&&Id(a);case 26:case 5:An(e,a),t===null&&l&4&&Vd(a),l&512&&Zl(a,a.return);break;case 12:An(e,a);break;case 31:An(e,a),l&4&&ef(e,a);break;case 13:An(e,a),l&4&&tf(e,a),l&64&&(e=a.memoizedState,e!==null&&(e=e.dehydrated,e!==null&&(a=km.bind(null,a),ty(e,a))));break;case 22:if(l=a.memoizedState!==null||En,!l){t=t!==null&&t.memoizedState!==null||ct,o=En;var i=ct;En=l,(ct=t)&&!i?Nn(e,a,(a.subtreeFlags&8772)!==0):An(e,a),En=o,ct=i}break;case 30:break;default:An(e,a)}}function Fd(e){var t=e.alternate;t!==null&&(e.alternate=null,Fd(t)),e.child=null,e.deletions=null,e.sibling=null,e.tag===5&&(t=e.stateNode,t!==null&&Yi(t)),e.stateNode=null,e.return=null,e.dependencies=null,e.memoizedProps=null,e.memoizedState=null,e.pendingProps=null,e.stateNode=null,e.updateQueue=null}var $e=null,Ct=!1;function wn(e,t,a){for(a=a.child;a!==null;)Pd(e,t,a),a=a.sibling}function Pd(e,t,a){if(St&&typeof St.onCommitFiberUnmount=="function")try{St.onCommitFiberUnmount(ra,a)}catch{}switch(a.tag){case 26:ct||cn(a,t),wn(e,t,a),a.memoizedState?a.memoizedState.count--:a.stateNode&&(a=a.stateNode,a.parentNode.removeChild(a));break;case 27:ct||cn(a,t);var l=$e,o=Ct;Pn(a.type)&&($e=a.stateNode,Ct=!1),wn(e,t,a),eo(a.stateNode),$e=l,Ct=o;break;case 5:ct||cn(a,t);case 6:if(l=$e,o=Ct,$e=null,wn(e,t,a),$e=l,Ct=o,$e!==null)if(Ct)try{($e.nodeType===9?$e.body:$e.nodeName==="HTML"?$e.ownerDocument.body:$e).removeChild(a.stateNode)}catch(i){Ge(a,t,i)}else try{$e.removeChild(a.stateNode)}catch(i){Ge(a,t,i)}break;case 18:$e!==null&&(Ct?(e=$e,Qf(e.nodeType===9?e.body:e.nodeName==="HTML"?e.ownerDocument.body:e,a.stateNode),pl(e)):Qf($e,a.stateNode));break;case 4:l=$e,o=Ct,$e=a.stateNode.containerInfo,Ct=!0,wn(e,t,a),$e=l,Ct=o;break;case 0:case 11:case 14:case 15:Qn(2,a,t),ct||Qn(4,a,t),wn(e,t,a);break;case 1:ct||(cn(a,t),l=a.stateNode,typeof l.componentWillUnmount=="function"&&Qd(a,t,l)),wn(e,t,a);break;case 21:wn(e,t,a);break;case 22:ct=(l=ct)||a.memoizedState!==null,wn(e,t,a),ct=l;break;default:wn(e,t,a)}}function ef(e,t){if(t.memoizedState===null&&(e=t.alternate,e!==null&&(e=e.memoizedState,e!==null))){e=e.dehydrated;try{pl(e)}catch(a){Ge(t,t.return,a)}}}function tf(e,t){if(t.memoizedState===null&&(e=t.alternate,e!==null&&(e=e.memoizedState,e!==null&&(e=e.dehydrated,e!==null))))try{pl(e)}catch(a){Ge(t,t.return,a)}}function Nm(e){switch(e.tag){case 31:case 13:case 19:var t=e.stateNode;return t===null&&(t=e.stateNode=new $d),t;case 22:return e=e.stateNode,t=e._retryCache,t===null&&(t=e._retryCache=new $d),t;default:throw Error(u(435,e.tag))}}function ni(e,t){var a=Nm(e);t.forEach(function(l){if(!a.has(l)){a.add(l);var o=Bm.bind(null,e,l);l.then(o,o)}})}function jt(e,t){var a=t.deletions;if(a!==null)for(var l=0;l<a.length;l++){var o=a[l],i=e,r=t,f=r;e:for(;f!==null;){switch(f.tag){case 27:if(Pn(f.type)){$e=f.stateNode,Ct=!1;break e}break;case 5:$e=f.stateNode,Ct=!1;break e;case 3:case 4:$e=f.stateNode.containerInfo,Ct=!0;break e}f=f.return}if($e===null)throw Error(u(160));Pd(i,r,o),$e=null,Ct=!1,i=o.alternate,i!==null&&(i.return=null),o.return=null}if(t.subtreeFlags&13886)for(t=t.child;t!==null;)nf(t,e),t=t.sibling}var tn=null;function nf(e,t){var a=e.alternate,l=e.flags;switch(e.tag){case 0:case 11:case 14:case 15:jt(t,e),Mt(e),l&4&&(Qn(3,e,e.return),Jl(3,e),Qn(5,e,e.return));break;case 1:jt(t,e),Mt(e),l&512&&(ct||a===null||cn(a,a.return)),l&64&&En&&(e=e.updateQueue,e!==null&&(l=e.callbacks,l!==null&&(a=e.shared.hiddenCallbacks,e.shared.hiddenCallbacks=a===null?l:a.concat(l))));break;case 26:var o=tn;if(jt(t,e),Mt(e),l&512&&(ct||a===null||cn(a,a.return)),l&4){var i=a!==null?a.memoizedState:null;if(l=e.memoizedState,a===null)if(l===null)if(e.stateNode===null){e:{l=e.type,a=e.memoizedProps,o=o.ownerDocument||o;t:switch(l){case"title":i=o.getElementsByTagName("title")[0],(!i||i[vl]||i[mt]||i.namespaceURI==="http://www.w3.org/2000/svg"||i.hasAttribute("itemprop"))&&(i=o.createElement(l),o.head.insertBefore(i,o.querySelector("head > title"))),bt(i,l,a),i[mt]=e,ft(i),l=i;break e;case"link":var r=ap("link","href",o).get(l+(a.href||""));if(r){for(var f=0;f<r.length;f++)if(i=r[f],i.getAttribute("href")===(a.href==null||a.href===""?null:a.href)&&i.getAttribute("rel")===(a.rel==null?null:a.rel)&&i.getAttribute("title")===(a.title==null?null:a.title)&&i.getAttribute("crossorigin")===(a.crossOrigin==null?null:a.crossOrigin)){r.splice(f,1);break t}}i=o.createElement(l),bt(i,l,a),o.head.appendChild(i);break;case"meta":if(r=ap("meta","content",o).get(l+(a.content||""))){for(f=0;f<r.length;f++)if(i=r[f],i.getAttribute("content")===(a.content==null?null:""+a.content)&&i.getAttribute("name")===(a.name==null?null:a.name)&&i.getAttribute("property")===(a.property==null?null:a.property)&&i.getAttribute("http-equiv")===(a.httpEquiv==null?null:a.httpEquiv)&&i.getAttribute("charset")===(a.charSet==null?null:a.charSet)){r.splice(f,1);break t}}i=o.createElement(l),bt(i,l,a),o.head.appendChild(i);break;default:throw Error(u(468,l))}i[mt]=e,ft(i),l=i}e.stateNode=l}else lp(o,e.type,e.stateNode);else e.stateNode=np(o,l,e.memoizedProps);else i!==l?(i===null?a.stateNode!==null&&(a=a.stateNode,a.parentNode.removeChild(a)):i.count--,l===null?lp(o,e.type,e.stateNode):np(o,l,e.memoizedProps)):l===null&&e.stateNode!==null&&rc(e,e.memoizedProps,a.memoizedProps)}break;case 27:jt(t,e),Mt(e),l&512&&(ct||a===null||cn(a,a.return)),a!==null&&l&4&&rc(e,e.memoizedProps,a.memoizedProps);break;case 5:if(jt(t,e),Mt(e),l&512&&(ct||a===null||cn(a,a.return)),e.flags&32){o=e.stateNode;try{Ba(o,"")}catch(ae){Ge(e,e.return,ae)}}l&4&&e.stateNode!=null&&(o=e.memoizedProps,rc(e,o,a!==null?a.memoizedProps:o)),l&1024&&(fc=!0);break;case 6:if(jt(t,e),Mt(e),l&4){if(e.stateNode===null)throw Error(u(162));l=e.memoizedProps,a=e.stateNode;try{a.nodeValue=l}catch(ae){Ge(e,e.return,ae)}}break;case 3:if(vi=null,o=tn,tn=yi(t.containerInfo),jt(t,e),tn=o,Mt(e),l&4&&a!==null&&a.memoizedState.isDehydrated)try{pl(t.containerInfo)}catch(ae){Ge(e,e.return,ae)}fc&&(fc=!1,af(e));break;case 4:l=tn,tn=yi(e.stateNode.containerInfo),jt(t,e),Mt(e),tn=l;break;case 12:jt(t,e),Mt(e);break;case 31:jt(t,e),Mt(e),l&4&&(l=e.updateQueue,l!==null&&(e.updateQueue=null,ni(e,l)));break;case 13:jt(t,e),Mt(e),e.child.flags&8192&&e.memoizedState!==null!=(a!==null&&a.memoizedState!==null)&&(li=ge()),l&4&&(l=e.updateQueue,l!==null&&(e.updateQueue=null,ni(e,l)));break;case 22:o=e.memoizedState!==null;var _=a!==null&&a.memoizedState!==null,R=En,X=ct;if(En=R||o,ct=X||_,jt(t,e),ct=X,En=R,Mt(e),l&8192)e:for(t=e.stateNode,t._visibility=o?t._visibility&-2:t._visibility|1,o&&(a===null||_||En||ct||Aa(e)),a=null,t=e;;){if(t.tag===5||t.tag===26){if(a===null){_=a=t;try{if(i=_.stateNode,o)r=i.style,typeof r.setProperty=="function"?r.setProperty("display","none","important"):r.display="none";else{f=_.stateNode;var Q=_.memoizedProps.style,U=Q!=null&&Q.hasOwnProperty("display")?Q.display:null;f.style.display=U==null||typeof U=="boolean"?"":(""+U).trim()}}catch(ae){Ge(_,_.return,ae)}}}else if(t.tag===6){if(a===null){_=t;try{_.stateNode.nodeValue=o?"":_.memoizedProps}catch(ae){Ge(_,_.return,ae)}}}else if(t.tag===18){if(a===null){_=t;try{var G=_.stateNode;o?Vf(G,!0):Vf(_.stateNode,!1)}catch(ae){Ge(_,_.return,ae)}}}else if((t.tag!==22&&t.tag!==23||t.memoizedState===null||t===e)&&t.child!==null){t.child.return=t,t=t.child;continue}if(t===e)break e;for(;t.sibling===null;){if(t.return===null||t.return===e)break e;a===t&&(a=null),t=t.return}a===t&&(a=null),t.sibling.return=t.return,t=t.sibling}l&4&&(l=e.updateQueue,l!==null&&(a=l.retryQueue,a!==null&&(l.retryQueue=null,ni(e,a))));break;case 19:jt(t,e),Mt(e),l&4&&(l=e.updateQueue,l!==null&&(e.updateQueue=null,ni(e,l)));break;case 30:break;case 21:break;default:jt(t,e),Mt(e)}}function Mt(e){var t=e.flags;if(t&2){try{for(var a,l=e.return;l!==null;){if(Kd(l)){a=l;break}l=l.return}if(a==null)throw Error(u(160));switch(a.tag){case 27:var o=a.stateNode,i=uc(e);ti(e,i,o);break;case 5:var r=a.stateNode;a.flags&32&&(Ba(r,""),a.flags&=-33);var f=uc(e);ti(e,f,r);break;case 3:case 4:var _=a.stateNode.containerInfo,R=uc(e);dc(e,R,_);break;default:throw Error(u(161))}}catch(X){Ge(e,e.return,X)}e.flags&=-3}t&4096&&(e.flags&=-4097)}function af(e){if(e.subtreeFlags&1024)for(e=e.child;e!==null;){var t=e;af(t),t.tag===5&&t.flags&1024&&t.stateNode.reset(),e=e.sibling}}function An(e,t){if(t.subtreeFlags&8772)for(t=t.child;t!==null;)Wd(e,t.alternate,t),t=t.sibling}function Aa(e){for(e=e.child;e!==null;){var t=e;switch(t.tag){case 0:case 11:case 14:case 15:Qn(4,t,t.return),Aa(t);break;case 1:cn(t,t.return);var a=t.stateNode;typeof a.componentWillUnmount=="function"&&Qd(t,t.return,a),Aa(t);break;case 27:eo(t.stateNode);case 26:case 5:cn(t,t.return),Aa(t);break;case 22:t.memoizedState===null&&Aa(t);break;case 30:Aa(t);break;default:Aa(t)}e=e.sibling}}function Nn(e,t,a){for(a=a&&(t.subtreeFlags&8772)!==0,t=t.child;t!==null;){var l=t.alternate,o=e,i=t,r=i.flags;switch(i.tag){case 0:case 11:case 15:Nn(o,i,a),Jl(4,i);break;case 1:if(Nn(o,i,a),l=i,o=l.stateNode,typeof o.componentDidMount=="function")try{o.componentDidMount()}catch(R){Ge(l,l.return,R)}if(l=i,o=l.updateQueue,o!==null){var f=l.stateNode;try{var _=o.shared.hiddenCallbacks;if(_!==null)for(o.shared.hiddenCallbacks=null,o=0;o<_.length;o++)Ru(_[o],f)}catch(R){Ge(l,l.return,R)}}a&&r&64&&Zd(i),Zl(i,i.return);break;case 27:Id(i);case 26:case 5:Nn(o,i,a),a&&l===null&&r&4&&Vd(i),Zl(i,i.return);break;case 12:Nn(o,i,a);break;case 31:Nn(o,i,a),a&&r&4&&ef(o,i);break;case 13:Nn(o,i,a),a&&r&4&&tf(o,i);break;case 22:i.memoizedState===null&&Nn(o,i,a),Zl(i,i.return);break;case 30:break;default:Nn(o,i,a)}t=t.sibling}}function pc(e,t){var a=null;e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(a=e.memoizedState.cachePool.pool),e=null,t.memoizedState!==null&&t.memoizedState.cachePool!==null&&(e=t.memoizedState.cachePool.pool),e!==a&&(e!=null&&e.refCount++,a!=null&&Dl(a))}function hc(e,t){e=null,t.alternate!==null&&(e=t.alternate.memoizedState.cache),t=t.memoizedState.cache,t!==e&&(t.refCount++,e!=null&&Dl(e))}function nn(e,t,a,l){if(t.subtreeFlags&10256)for(t=t.child;t!==null;)lf(e,t,a,l),t=t.sibling}function lf(e,t,a,l){var o=t.flags;switch(t.tag){case 0:case 11:case 15:nn(e,t,a,l),o&2048&&Jl(9,t);break;case 1:nn(e,t,a,l);break;case 3:nn(e,t,a,l),o&2048&&(e=null,t.alternate!==null&&(e=t.alternate.memoizedState.cache),t=t.memoizedState.cache,t!==e&&(t.refCount++,e!=null&&Dl(e)));break;case 12:if(o&2048){nn(e,t,a,l),e=t.stateNode;try{var i=t.memoizedProps,r=i.id,f=i.onPostCommit;typeof f=="function"&&f(r,t.alternate===null?"mount":"update",e.passiveEffectDuration,-0)}catch(_){Ge(t,t.return,_)}}else nn(e,t,a,l);break;case 31:nn(e,t,a,l);break;case 13:nn(e,t,a,l);break;case 23:break;case 22:i=t.stateNode,r=t.alternate,t.memoizedState!==null?i._visibility&2?nn(e,t,a,l):Ql(e,t):i._visibility&2?nn(e,t,a,l):(i._visibility|=2,nl(e,t,a,l,(t.subtreeFlags&10256)!==0||!1)),o&2048&&pc(r,t);break;case 24:nn(e,t,a,l),o&2048&&hc(t.alternate,t);break;default:nn(e,t,a,l)}}function nl(e,t,a,l,o){for(o=o&&((t.subtreeFlags&10256)!==0||!1),t=t.child;t!==null;){var i=e,r=t,f=a,_=l,R=r.flags;switch(r.tag){case 0:case 11:case 15:nl(i,r,f,_,o),Jl(8,r);break;case 23:break;case 22:var X=r.stateNode;r.memoizedState!==null?X._visibility&2?nl(i,r,f,_,o):Ql(i,r):(X._visibility|=2,nl(i,r,f,_,o)),o&&R&2048&&pc(r.alternate,r);break;case 24:nl(i,r,f,_,o),o&&R&2048&&hc(r.alternate,r);break;default:nl(i,r,f,_,o)}t=t.sibling}}function Ql(e,t){if(t.subtreeFlags&10256)for(t=t.child;t!==null;){var a=e,l=t,o=l.flags;switch(l.tag){case 22:Ql(a,l),o&2048&&pc(l.alternate,l);break;case 24:Ql(a,l),o&2048&&hc(l.alternate,l);break;default:Ql(a,l)}t=t.sibling}}var Vl=8192;function al(e,t,a){if(e.subtreeFlags&Vl)for(e=e.child;e!==null;)of(e,t,a),e=e.sibling}function of(e,t,a){switch(e.tag){case 26:al(e,t,a),e.flags&Vl&&e.memoizedState!==null&&py(a,tn,e.memoizedState,e.memoizedProps);break;case 5:al(e,t,a);break;case 3:case 4:var l=tn;tn=yi(e.stateNode.containerInfo),al(e,t,a),tn=l;break;case 22:e.memoizedState===null&&(l=e.alternate,l!==null&&l.memoizedState!==null?(l=Vl,Vl=16777216,al(e,t,a),Vl=l):al(e,t,a));break;default:al(e,t,a)}}function sf(e){var t=e.alternate;if(t!==null&&(e=t.child,e!==null)){t.child=null;do t=e.sibling,e.sibling=null,e=t;while(e!==null)}}function Kl(e){var t=e.deletions;if((e.flags&16)!==0){if(t!==null)for(var a=0;a<t.length;a++){var l=t[a];pt=l,rf(l,e)}sf(e)}if(e.subtreeFlags&10256)for(e=e.child;e!==null;)cf(e),e=e.sibling}function cf(e){switch(e.tag){case 0:case 11:case 15:Kl(e),e.flags&2048&&Qn(9,e,e.return);break;case 3:Kl(e);break;case 12:Kl(e);break;case 22:var t=e.stateNode;e.memoizedState!==null&&t._visibility&2&&(e.return===null||e.return.tag!==13)?(t._visibility&=-3,ai(e)):Kl(e);break;default:Kl(e)}}function ai(e){var t=e.deletions;if((e.flags&16)!==0){if(t!==null)for(var a=0;a<t.length;a++){var l=t[a];pt=l,rf(l,e)}sf(e)}for(e=e.child;e!==null;){switch(t=e,t.tag){case 0:case 11:case 15:Qn(8,t,t.return),ai(t);break;case 22:a=t.stateNode,a._visibility&2&&(a._visibility&=-3,ai(t));break;default:ai(t)}e=e.sibling}}function rf(e,t){for(;pt!==null;){var a=pt;switch(a.tag){case 0:case 11:case 15:Qn(8,a,t);break;case 23:case 22:if(a.memoizedState!==null&&a.memoizedState.cachePool!==null){var l=a.memoizedState.cachePool.pool;l!=null&&l.refCount++}break;case 24:Dl(a.memoizedState.cache)}if(l=a.child,l!==null)l.return=a,pt=l;else e:for(a=e;pt!==null;){l=pt;var o=l.sibling,i=l.return;if(Fd(l),l===a){pt=null;break e}if(o!==null){o.return=i,pt=o;break e}pt=i}}}var Cm={getCacheForType:function(e){var t=gt(ot),a=t.data.get(e);return a===void 0&&(a=e(),t.data.set(e,a)),a},cacheSignal:function(){return gt(ot).controller.signal}},jm=typeof WeakMap=="function"?WeakMap:Map,ke=0,Ze=null,Ee=null,Ae=0,He=0,Ht=null,Vn=!1,ll=!1,mc=!1,Cn=0,Fe=0,Kn=0,Na=0,yc=0,Gt=0,ol=0,Il=null,Dt=null,gc=!1,li=0,uf=0,oi=1/0,ii=null,In=null,ut=0,$n=null,il=null,jn=0,vc=0,bc=null,df=null,$l=0,_c=null;function Lt(){return(ke&2)!==0&&Ae!==0?Ae&-Ae:M.T!==null?Ac():Ar()}function ff(){if(Gt===0)if((Ae&536870912)===0||Ce){var e=Rn;Rn<<=1,(Rn&3932160)===0&&(Rn=262144),Gt=e}else Gt=536870912;return e=Bt.current,e!==null&&(e.flags|=32),Gt}function Ot(e,t,a){(e===Ze&&(He===2||He===9)||e.cancelPendingCommit!==null)&&(sl(e,0),Wn(e,Ae,Gt,!1)),gl(e,a),((ke&2)===0||e!==Ze)&&(e===Ze&&((ke&2)===0&&(Na|=a),Fe===4&&Wn(e,Ae,Gt,!1)),rn(e))}function pf(e,t,a){if((ke&6)!==0)throw Error(u(327));var l=!a&&(t&127)===0&&(t&e.expiredLanes)===0||yl(e,t),o=l?Om(e,t):xc(e,t,!0),i=l;do{if(o===0){ll&&!l&&Wn(e,t,0,!1);break}else{if(a=e.current.alternate,i&&!Mm(a)){o=xc(e,t,!1),i=!1;continue}if(o===2){if(i=t,e.errorRecoveryDisabledLanes&i)var r=0;else r=e.pendingLanes&-536870913,r=r!==0?r:r&536870912?536870912:0;if(r!==0){t=r;e:{var f=e;o=Il;var _=f.current.memoizedState.isDehydrated;if(_&&(sl(f,r).flags|=256),r=xc(f,r,!1),r!==2){if(mc&&!_){f.errorRecoveryDisabledLanes|=i,Na|=i,o=4;break e}i=Dt,Dt=o,i!==null&&(Dt===null?Dt=i:Dt.push.apply(Dt,i))}o=r}if(i=!1,o!==2)continue}}if(o===1){sl(e,0),Wn(e,t,0,!0);break}e:{switch(l=e,i=o,i){case 0:case 1:throw Error(u(345));case 4:if((t&4194048)!==t)break;case 6:Wn(l,t,Gt,!Vn);break e;case 2:Dt=null;break;case 3:case 5:break;default:throw Error(u(329))}if((t&62914560)===t&&(o=li+300-ge(),10<o)){if(Wn(l,t,Gt,!Vn),yo(l,0,!0)!==0)break e;jn=t,l.timeoutHandle=Jf(hf.bind(null,l,a,Dt,ii,gc,t,Gt,Na,ol,Vn,i,"Throttled",-0,0),o);break e}hf(l,a,Dt,ii,gc,t,Gt,Na,ol,Vn,i,null,-0,0)}}break}while(!0);rn(e)}function hf(e,t,a,l,o,i,r,f,_,R,X,Q,U,G){if(e.timeoutHandle=-1,Q=t.subtreeFlags,Q&8192||(Q&16785408)===16785408){Q={stylesheets:null,count:0,imgCount:0,imgBytes:0,suspenseyImages:[],waitingForImages:!0,waitingForViewTransition:!1,unsuspend:hn},of(t,i,Q);var ae=(i&62914560)===i?li-ge():(i&4194048)===i?uf-ge():0;if(ae=hy(Q,ae),ae!==null){jn=i,e.cancelPendingCommit=ae(xf.bind(null,e,t,i,a,l,o,r,f,_,X,Q,null,U,G)),Wn(e,i,r,!R);return}}xf(e,t,i,a,l,o,r,f,_)}function Mm(e){for(var t=e;;){var a=t.tag;if((a===0||a===11||a===15)&&t.flags&16384&&(a=t.updateQueue,a!==null&&(a=a.stores,a!==null)))for(var l=0;l<a.length;l++){var o=a[l],i=o.getSnapshot;o=o.value;try{if(!Rt(i(),o))return!1}catch{return!1}}if(a=t.child,t.subtreeFlags&16384&&a!==null)a.return=t,t=a;else{if(t===e)break;for(;t.sibling===null;){if(t.return===null||t.return===e)return!0;t=t.return}t.sibling.return=t.return,t=t.sibling}}return!0}function Wn(e,t,a,l){t&=~yc,t&=~Na,e.suspendedLanes|=t,e.pingedLanes&=~t,l&&(e.warmLanes|=t),l=e.expirationTimes;for(var o=t;0<o;){var i=31-xt(o),r=1<<i;l[i]=-1,o&=~r}a!==0&&Tr(e,a,t)}function si(){return(ke&6)===0?(Wl(0),!1):!0}function Sc(){if(Ee!==null){if(He===0)var e=Ee.return;else e=Ee,vn=va=null,Us(e),Wa=null,zl=0,e=Ee;for(;e!==null;)Jd(e.alternate,e),e=e.return;Ee=null}}function sl(e,t){var a=e.timeoutHandle;a!==-1&&(e.timeoutHandle=-1,$m(a)),a=e.cancelPendingCommit,a!==null&&(e.cancelPendingCommit=null,a()),jn=0,Sc(),Ze=e,Ee=a=yn(e.current,null),Ae=t,He=0,Ht=null,Vn=!1,ll=yl(e,t),mc=!1,ol=Gt=yc=Na=Kn=Fe=0,Dt=Il=null,gc=!1,(t&8)!==0&&(t|=t&32);var l=e.entangledLanes;if(l!==0)for(e=e.entanglements,l&=t;0<l;){var o=31-xt(l),i=1<<o;t|=e[o],l&=~i}return Cn=t,Co(),a}function mf(e,t){ve=null,M.H=ql,t===$a||t===Bo?(t=Mu(),He=3):t===ws?(t=Mu(),He=4):He=t===Ps?8:t!==null&&typeof t=="object"&&typeof t.then=="function"?6:1,Ht=t,Ee===null&&(Fe=1,$o(e,Qt(t,e.current)))}function yf(){var e=Bt.current;return e===null?!0:(Ae&4194048)===Ae?$t===null:(Ae&62914560)===Ae||(Ae&536870912)!==0?e===$t:!1}function gf(){var e=M.H;return M.H=ql,e===null?ql:e}function vf(){var e=M.A;return M.A=Cm,e}function ci(){Fe=4,Vn||(Ae&4194048)!==Ae&&Bt.current!==null||(ll=!0),(Kn&134217727)===0&&(Na&134217727)===0||Ze===null||Wn(Ze,Ae,Gt,!1)}function xc(e,t,a){var l=ke;ke|=2;var o=gf(),i=vf();(Ze!==e||Ae!==t)&&(ii=null,sl(e,t)),t=!1;var r=Fe;e:do try{if(He!==0&&Ee!==null){var f=Ee,_=Ht;switch(He){case 8:Sc(),r=6;break e;case 3:case 2:case 9:case 6:Bt.current===null&&(t=!0);var R=He;if(He=0,Ht=null,cl(e,f,_,R),a&&ll){r=0;break e}break;default:R=He,He=0,Ht=null,cl(e,f,_,R)}}Dm(),r=Fe;break}catch(X){mf(e,X)}while(!0);return t&&e.shellSuspendCounter++,vn=va=null,ke=l,M.H=o,M.A=i,Ee===null&&(Ze=null,Ae=0,Co()),r}function Dm(){for(;Ee!==null;)bf(Ee)}function Om(e,t){var a=ke;ke|=2;var l=gf(),o=vf();Ze!==e||Ae!==t?(ii=null,oi=ge()+500,sl(e,t)):ll=yl(e,t);e:do try{if(He!==0&&Ee!==null){t=Ee;var i=Ht;t:switch(He){case 1:He=0,Ht=null,cl(e,t,i,1);break;case 2:case 9:if(Cu(i)){He=0,Ht=null,_f(t);break}t=function(){He!==2&&He!==9||Ze!==e||(He=7),rn(e)},i.then(t,t);break e;case 3:He=7;break e;case 4:He=5;break e;case 7:Cu(i)?(He=0,Ht=null,_f(t)):(He=0,Ht=null,cl(e,t,i,7));break;case 5:var r=null;switch(Ee.tag){case 26:r=Ee.memoizedState;case 5:case 27:var f=Ee;if(r?op(r):f.stateNode.complete){He=0,Ht=null;var _=f.sibling;if(_!==null)Ee=_;else{var R=f.return;R!==null?(Ee=R,ri(R)):Ee=null}break t}}He=0,Ht=null,cl(e,t,i,5);break;case 6:He=0,Ht=null,cl(e,t,i,6);break;case 8:Sc(),Fe=6;break e;default:throw Error(u(462))}}zm();break}catch(X){mf(e,X)}while(!0);return vn=va=null,M.H=l,M.A=o,ke=a,Ee!==null?0:(Ze=null,Ae=0,Co(),Fe)}function zm(){for(;Ee!==null&&!rt();)bf(Ee)}function bf(e){var t=Yd(e.alternate,e,Cn);e.memoizedProps=e.pendingProps,t===null?ri(e):Ee=t}function _f(e){var t=e,a=t.alternate;switch(t.tag){case 15:case 0:t=Bd(a,t,t.pendingProps,t.type,void 0,Ae);break;case 11:t=Bd(a,t,t.pendingProps,t.type.render,t.ref,Ae);break;case 5:Us(t);default:Jd(a,t),t=Ee=gu(t,Cn),t=Yd(a,t,Cn)}e.memoizedProps=e.pendingProps,t===null?ri(e):Ee=t}function cl(e,t,a,l){vn=va=null,Us(t),Wa=null,zl=0;var o=t.return;try{if(Sm(e,o,t,a,Ae)){Fe=1,$o(e,Qt(a,e.current)),Ee=null;return}}catch(i){if(o!==null)throw Ee=o,i;Fe=1,$o(e,Qt(a,e.current)),Ee=null;return}t.flags&32768?(Ce||l===1?e=!0:ll||(Ae&536870912)!==0?e=!1:(Vn=e=!0,(l===2||l===9||l===3||l===6)&&(l=Bt.current,l!==null&&l.tag===13&&(l.flags|=16384))),Sf(t,e)):ri(t)}function ri(e){var t=e;do{if((t.flags&32768)!==0){Sf(t,Vn);return}e=t.return;var a=Em(t.alternate,t,Cn);if(a!==null){Ee=a;return}if(t=t.sibling,t!==null){Ee=t;return}Ee=t=e}while(t!==null);Fe===0&&(Fe=5)}function Sf(e,t){do{var a=wm(e.alternate,e);if(a!==null){a.flags&=32767,Ee=a;return}if(a=e.return,a!==null&&(a.flags|=32768,a.subtreeFlags=0,a.deletions=null),!t&&(e=e.sibling,e!==null)){Ee=e;return}Ee=e=a}while(e!==null);Fe=6,Ee=null}function xf(e,t,a,l,o,i,r,f,_){e.cancelPendingCommit=null;do ui();while(ut!==0);if((ke&6)!==0)throw Error(u(327));if(t!==null){if(t===e.current)throw Error(u(177));if(i=t.lanes|t.childLanes,i|=us,fh(e,a,i,r,f,_),e===Ze&&(Ee=Ze=null,Ae=0),il=t,$n=e,jn=a,vc=i,bc=o,df=l,(t.subtreeFlags&10256)!==0||(t.flags&10256)!==0?(e.callbackNode=null,e.callbackPriority=0,Um(ca,function(){return Nf(),null})):(e.callbackNode=null,e.callbackPriority=0),l=(t.flags&13878)!==0,(t.subtreeFlags&13878)!==0||l){l=M.T,M.T=null,o=D.p,D.p=2,r=ke,ke|=4;try{Am(e,t,a)}finally{ke=r,D.p=o,M.T=l}}ut=1,Tf(),Ef(),wf()}}function Tf(){if(ut===1){ut=0;var e=$n,t=il,a=(t.flags&13878)!==0;if((t.subtreeFlags&13878)!==0||a){a=M.T,M.T=null;var l=D.p;D.p=2;var o=ke;ke|=4;try{nf(t,e);var i=Rc,r=cu(e.containerInfo),f=i.focusedElem,_=i.selectionRange;if(r!==f&&f&&f.ownerDocument&&su(f.ownerDocument.documentElement,f)){if(_!==null&&os(f)){var R=_.start,X=_.end;if(X===void 0&&(X=R),"selectionStart"in f)f.selectionStart=R,f.selectionEnd=Math.min(X,f.value.length);else{var Q=f.ownerDocument||document,U=Q&&Q.defaultView||window;if(U.getSelection){var G=U.getSelection(),ae=f.textContent.length,fe=Math.min(_.start,ae),Je=_.end===void 0?fe:Math.min(_.end,ae);!G.extend&&fe>Je&&(r=Je,Je=fe,fe=r);var j=iu(f,fe),E=iu(f,Je);if(j&&E&&(G.rangeCount!==1||G.anchorNode!==j.node||G.anchorOffset!==j.offset||G.focusNode!==E.node||G.focusOffset!==E.offset)){var z=Q.createRange();z.setStart(j.node,j.offset),G.removeAllRanges(),fe>Je?(G.addRange(z),G.extend(E.node,E.offset)):(z.setEnd(E.node,E.offset),G.addRange(z))}}}}for(Q=[],G=f;G=G.parentNode;)G.nodeType===1&&Q.push({element:G,left:G.scrollLeft,top:G.scrollTop});for(typeof f.focus=="function"&&f.focus(),f=0;f<Q.length;f++){var Z=Q[f];Z.element.scrollLeft=Z.left,Z.element.scrollTop=Z.top}}xi=!!zc,Rc=zc=null}finally{ke=o,D.p=l,M.T=a}}e.current=t,ut=2}}function Ef(){if(ut===2){ut=0;var e=$n,t=il,a=(t.flags&8772)!==0;if((t.subtreeFlags&8772)!==0||a){a=M.T,M.T=null;var l=D.p;D.p=2;var o=ke;ke|=4;try{Wd(e,t.alternate,t)}finally{ke=o,D.p=l,M.T=a}}ut=3}}function wf(){if(ut===4||ut===3){ut=0,sa();var e=$n,t=il,a=jn,l=df;(t.subtreeFlags&10256)!==0||(t.flags&10256)!==0?ut=5:(ut=0,il=$n=null,Af(e,e.pendingLanes));var o=e.pendingLanes;if(o===0&&(In=null),Li(a),t=t.stateNode,St&&typeof St.onCommitFiberRoot=="function")try{St.onCommitFiberRoot(ra,t,void 0,(t.current.flags&128)===128)}catch{}if(l!==null){t=M.T,o=D.p,D.p=2,M.T=null;try{for(var i=e.onRecoverableError,r=0;r<l.length;r++){var f=l[r];i(f.value,{componentStack:f.stack})}}finally{M.T=t,D.p=o}}(jn&3)!==0&&ui(),rn(e),o=e.pendingLanes,(a&261930)!==0&&(o&42)!==0?e===_c?$l++:($l=0,_c=e):$l=0,Wl(0)}}function Af(e,t){(e.pooledCacheLanes&=t)===0&&(t=e.pooledCache,t!=null&&(e.pooledCache=null,Dl(t)))}function ui(){return Tf(),Ef(),wf(),Nf()}function Nf(){if(ut!==5)return!1;var e=$n,t=vc;vc=0;var a=Li(jn),l=M.T,o=D.p;try{D.p=32>a?32:a,M.T=null,a=bc,bc=null;var i=$n,r=jn;if(ut=0,il=$n=null,jn=0,(ke&6)!==0)throw Error(u(331));var f=ke;if(ke|=4,cf(i.current),lf(i,i.current,r,a),ke=f,Wl(0,!1),St&&typeof St.onPostCommitFiberRoot=="function")try{St.onPostCommitFiberRoot(ra,i)}catch{}return!0}finally{D.p=o,M.T=l,Af(e,t)}}function Cf(e,t,a){t=Qt(a,t),t=Fs(e.stateNode,t,2),e=Xn(e,t,2),e!==null&&(gl(e,2),rn(e))}function Ge(e,t,a){if(e.tag===3)Cf(e,e,a);else for(;t!==null;){if(t.tag===3){Cf(t,e,a);break}else if(t.tag===1){var l=t.stateNode;if(typeof t.type.getDerivedStateFromError=="function"||typeof l.componentDidCatch=="function"&&(In===null||!In.has(l))){e=Qt(a,e),a=Cd(2),l=Xn(t,a,2),l!==null&&(jd(a,l,t,e),gl(l,2),rn(l));break}}t=t.return}}function Tc(e,t,a){var l=e.pingCache;if(l===null){l=e.pingCache=new jm;var o=new Set;l.set(t,o)}else o=l.get(t),o===void 0&&(o=new Set,l.set(t,o));o.has(a)||(mc=!0,o.add(a),e=Rm.bind(null,e,t,a),t.then(e,e))}function Rm(e,t,a){var l=e.pingCache;l!==null&&l.delete(t),e.pingedLanes|=e.suspendedLanes&a,e.warmLanes&=~a,Ze===e&&(Ae&a)===a&&(Fe===4||Fe===3&&(Ae&62914560)===Ae&&300>ge()-li?(ke&2)===0&&sl(e,0):yc|=a,ol===Ae&&(ol=0)),rn(e)}function jf(e,t){t===0&&(t=xr()),e=ma(e,t),e!==null&&(gl(e,t),rn(e))}function km(e){var t=e.memoizedState,a=0;t!==null&&(a=t.retryLane),jf(e,a)}function Bm(e,t){var a=0;switch(e.tag){case 31:case 13:var l=e.stateNode,o=e.memoizedState;o!==null&&(a=o.retryLane);break;case 19:l=e.stateNode;break;case 22:l=e.stateNode._retryCache;break;default:throw Error(u(314))}l!==null&&l.delete(t),jf(e,a)}function Um(e,t){return dt(e,t)}var di=null,rl=null,Ec=!1,fi=!1,wc=!1,Fn=0;function rn(e){e!==rl&&e.next===null&&(rl===null?di=rl=e:rl=rl.next=e),fi=!0,Ec||(Ec=!0,Gm())}function Wl(e,t){if(!wc&&fi){wc=!0;do for(var a=!1,l=di;l!==null;){if(e!==0){var o=l.pendingLanes;if(o===0)var i=0;else{var r=l.suspendedLanes,f=l.pingedLanes;i=(1<<31-xt(42|e)+1)-1,i&=o&~(r&~f),i=i&201326741?i&201326741|1:i?i|2:0}i!==0&&(a=!0,zf(l,i))}else i=Ae,i=yo(l,l===Ze?i:0,l.cancelPendingCommit!==null||l.timeoutHandle!==-1),(i&3)===0||yl(l,i)||(a=!0,zf(l,i));l=l.next}while(a);wc=!1}}function Hm(){Mf()}function Mf(){fi=Ec=!1;var e=0;Fn!==0&&Im()&&(e=Fn);for(var t=ge(),a=null,l=di;l!==null;){var o=l.next,i=Df(l,t);i===0?(l.next=null,a===null?di=o:a.next=o,o===null&&(rl=a)):(a=l,(e!==0||(i&3)!==0)&&(fi=!0)),l=o}ut!==0&&ut!==5||Wl(e),Fn!==0&&(Fn=0)}function Df(e,t){for(var a=e.suspendedLanes,l=e.pingedLanes,o=e.expirationTimes,i=e.pendingLanes&-62914561;0<i;){var r=31-xt(i),f=1<<r,_=o[r];_===-1?((f&a)===0||(f&l)!==0)&&(o[r]=dh(f,t)):_<=t&&(e.expiredLanes|=f),i&=~f}if(t=Ze,a=Ae,a=yo(e,e===t?a:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),l=e.callbackNode,a===0||e===t&&(He===2||He===9)||e.cancelPendingCommit!==null)return l!==null&&l!==null&&Pe(l),e.callbackNode=null,e.callbackPriority=0;if((a&3)===0||yl(e,a)){if(t=a&-a,t===e.callbackPriority)return t;switch(l!==null&&Pe(l),Li(a)){case 2:case 8:a=ho;break;case 32:a=ca;break;case 268435456:a=mo;break;default:a=ca}return l=Of.bind(null,e),a=dt(a,l),e.callbackPriority=t,e.callbackNode=a,t}return l!==null&&l!==null&&Pe(l),e.callbackPriority=2,e.callbackNode=null,2}function Of(e,t){if(ut!==0&&ut!==5)return e.callbackNode=null,e.callbackPriority=0,null;var a=e.callbackNode;if(ui()&&e.callbackNode!==a)return null;var l=Ae;return l=yo(e,e===Ze?l:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),l===0?null:(pf(e,l,t),Df(e,ge()),e.callbackNode!=null&&e.callbackNode===a?Of.bind(null,e):null)}function zf(e,t){if(ui())return null;pf(e,t,!0)}function Gm(){Wm(function(){(ke&6)!==0?dt(po,Hm):Mf()})}function Ac(){if(Fn===0){var e=Ka;e===0&&(e=et,et<<=1,(et&261888)===0&&(et=256)),Fn=e}return Fn}function Rf(e){return e==null||typeof e=="symbol"||typeof e=="boolean"?null:typeof e=="function"?e:_o(""+e)}function kf(e,t){var a=t.ownerDocument.createElement("input");return a.name=t.name,a.value=t.value,e.id&&a.setAttribute("form",e.id),t.parentNode.insertBefore(a,t),e=new FormData(e),a.parentNode.removeChild(a),e}function Lm(e,t,a,l,o){if(t==="submit"&&a&&a.stateNode===o){var i=Rf((o[At]||null).action),r=l.submitter;r&&(t=(t=r[At]||null)?Rf(t.formAction):r.getAttribute("formAction"),t!==null&&(i=t,r=null));var f=new Eo("action","action",null,l,o);e.push({event:f,listeners:[{instance:null,listener:function(){if(l.defaultPrevented){if(Fn!==0){var _=r?kf(o,r):new FormData(o);Qs(a,{pending:!0,data:_,method:o.method,action:i},null,_)}}else typeof i=="function"&&(f.preventDefault(),_=r?kf(o,r):new FormData(o),Qs(a,{pending:!0,data:_,method:o.method,action:i},i,_))},currentTarget:o}]})}}for(var Nc=0;Nc<rs.length;Nc++){var Cc=rs[Nc],qm=Cc.toLowerCase(),Ym=Cc[0].toUpperCase()+Cc.slice(1);en(qm,"on"+Ym)}en(du,"onAnimationEnd"),en(fu,"onAnimationIteration"),en(pu,"onAnimationStart"),en("dblclick","onDoubleClick"),en("focusin","onFocus"),en("focusout","onBlur"),en(lm,"onTransitionRun"),en(om,"onTransitionStart"),en(im,"onTransitionCancel"),en(hu,"onTransitionEnd"),Ra("onMouseEnter",["mouseout","mouseover"]),Ra("onMouseLeave",["mouseout","mouseover"]),Ra("onPointerEnter",["pointerout","pointerover"]),Ra("onPointerLeave",["pointerout","pointerover"]),da("onChange","change click focusin focusout input keydown keyup selectionchange".split(" ")),da("onSelect","focusout contextmenu dragend focusin keydown keyup mousedown mouseup selectionchange".split(" ")),da("onBeforeInput",["compositionend","keypress","textInput","paste"]),da("onCompositionEnd","compositionend focusout keydown keypress keyup mousedown".split(" ")),da("onCompositionStart","compositionstart focusout keydown keypress keyup mousedown".split(" ")),da("onCompositionUpdate","compositionupdate focusout keydown keypress keyup mousedown".split(" "));var Fl="abort canplay canplaythrough durationchange emptied encrypted ended error loadeddata loadedmetadata loadstart pause play playing progress ratechange resize seeked seeking stalled suspend timeupdate volumechange waiting".split(" "),Xm=new Set("beforetoggle cancel close invalid load scroll scrollend toggle".split(" ").concat(Fl));function Bf(e,t){t=(t&4)!==0;for(var a=0;a<e.length;a++){var l=e[a],o=l.event;l=l.listeners;e:{var i=void 0;if(t)for(var r=l.length-1;0<=r;r--){var f=l[r],_=f.instance,R=f.currentTarget;if(f=f.listener,_!==i&&o.isPropagationStopped())break e;i=f,o.currentTarget=R;try{i(o)}catch(X){No(X)}o.currentTarget=null,i=_}else for(r=0;r<l.length;r++){if(f=l[r],_=f.instance,R=f.currentTarget,f=f.listener,_!==i&&o.isPropagationStopped())break e;i=f,o.currentTarget=R;try{i(o)}catch(X){No(X)}o.currentTarget=null,i=_}}}}function we(e,t){var a=t[qi];a===void 0&&(a=t[qi]=new Set);var l=e+"__bubble";a.has(l)||(Uf(t,e,2,!1),a.add(l))}function jc(e,t,a){var l=0;t&&(l|=4),Uf(a,e,l,t)}var pi="_reactListening"+Math.random().toString(36).slice(2);function Mc(e){if(!e[pi]){e[pi]=!0,jr.forEach(function(a){a!=="selectionchange"&&(Xm.has(a)||jc(a,!1,e),jc(a,!0,e))});var t=e.nodeType===9?e:e.ownerDocument;t===null||t[pi]||(t[pi]=!0,jc("selectionchange",!1,t))}}function Uf(e,t,a,l){switch(fp(t)){case 2:var o=gy;break;case 8:o=vy;break;default:o=Zc}a=o.bind(null,t,a,e),o=void 0,!$i||t!=="touchstart"&&t!=="touchmove"&&t!=="wheel"||(o=!0),l?o!==void 0?e.addEventListener(t,a,{capture:!0,passive:o}):e.addEventListener(t,a,!0):o!==void 0?e.addEventListener(t,a,{passive:o}):e.addEventListener(t,a,!1)}function Dc(e,t,a,l,o){var i=l;if((t&1)===0&&(t&2)===0&&l!==null)e:for(;;){if(l===null)return;var r=l.tag;if(r===3||r===4){var f=l.stateNode.containerInfo;if(f===o)break;if(r===4)for(r=l.return;r!==null;){var _=r.tag;if((_===3||_===4)&&r.stateNode.containerInfo===o)return;r=r.return}for(;f!==null;){if(r=Da(f),r===null)return;if(_=r.tag,_===5||_===6||_===26||_===27){l=i=r;continue e}f=f.parentNode}}l=l.return}qr(function(){var R=i,X=Ki(a),Q=[];e:{var U=mu.get(e);if(U!==void 0){var G=Eo,ae=e;switch(e){case"keypress":if(xo(a)===0)break e;case"keydown":case"keyup":G=Bh;break;case"focusin":ae="focus",G=es;break;case"focusout":ae="blur",G=es;break;case"beforeblur":case"afterblur":G=es;break;case"click":if(a.button===2)break e;case"auxclick":case"dblclick":case"mousedown":case"mousemove":case"mouseup":case"mouseout":case"mouseover":case"contextmenu":G=Jr;break;case"drag":case"dragend":case"dragenter":case"dragexit":case"dragleave":case"dragover":case"dragstart":case"drop":G=Eh;break;case"touchcancel":case"touchend":case"touchmove":case"touchstart":G=Gh;break;case du:case fu:case pu:G=Nh;break;case hu:G=qh;break;case"scroll":case"scrollend":G=xh;break;case"wheel":G=Xh;break;case"copy":case"cut":case"paste":G=jh;break;case"gotpointercapture":case"lostpointercapture":case"pointercancel":case"pointerdown":case"pointermove":case"pointerout":case"pointerover":case"pointerup":G=Qr;break;case"toggle":case"beforetoggle":G=Zh}var fe=(t&4)!==0,Je=!fe&&(e==="scroll"||e==="scrollend"),j=fe?U!==null?U+"Capture":null:U;fe=[];for(var E=R,z;E!==null;){var Z=E;if(z=Z.stateNode,Z=Z.tag,Z!==5&&Z!==26&&Z!==27||z===null||j===null||(Z=_l(E,j),Z!=null&&fe.push(Pl(E,Z,z))),Je)break;E=E.return}0<fe.length&&(U=new G(U,ae,null,a,X),Q.push({event:U,listeners:fe}))}}if((t&7)===0){e:{if(U=e==="mouseover"||e==="pointerover",G=e==="mouseout"||e==="pointerout",U&&a!==Vi&&(ae=a.relatedTarget||a.fromElement)&&(Da(ae)||ae[Ma]))break e;if((G||U)&&(U=X.window===X?X:(U=X.ownerDocument)?U.defaultView||U.parentWindow:window,G?(ae=a.relatedTarget||a.toElement,G=R,ae=ae?Da(ae):null,ae!==null&&(Je=p(ae),fe=ae.tag,ae!==Je||fe!==5&&fe!==27&&fe!==6)&&(ae=null)):(G=null,ae=R),G!==ae)){if(fe=Jr,Z="onMouseLeave",j="onMouseEnter",E="mouse",(e==="pointerout"||e==="pointerover")&&(fe=Qr,Z="onPointerLeave",j="onPointerEnter",E="pointer"),Je=G==null?U:bl(G),z=ae==null?U:bl(ae),U=new fe(Z,E+"leave",G,a,X),U.target=Je,U.relatedTarget=z,Z=null,Da(X)===R&&(fe=new fe(j,E+"enter",ae,a,X),fe.target=z,fe.relatedTarget=Je,Z=fe),Je=Z,G&&ae)t:{for(fe=Jm,j=G,E=ae,z=0,Z=j;Z;Z=fe(Z))z++;Z=0;for(var se=E;se;se=fe(se))Z++;for(;0<z-Z;)j=fe(j),z--;for(;0<Z-z;)E=fe(E),Z--;for(;z--;){if(j===E||E!==null&&j===E.alternate){fe=j;break t}j=fe(j),E=fe(E)}fe=null}else fe=null;G!==null&&Hf(Q,U,G,fe,!1),ae!==null&&Je!==null&&Hf(Q,Je,ae,fe,!0)}}e:{if(U=R?bl(R):window,G=U.nodeName&&U.nodeName.toLowerCase(),G==="select"||G==="input"&&U.type==="file")var Oe=eu;else if(Fr(U))if(tu)Oe=tm;else{Oe=Ph;var oe=Fh}else G=U.nodeName,!G||G.toLowerCase()!=="input"||U.type!=="checkbox"&&U.type!=="radio"?R&&Qi(R.elementType)&&(Oe=eu):Oe=em;if(Oe&&(Oe=Oe(e,R))){Pr(Q,Oe,a,X);break e}oe&&oe(e,U,R),e==="focusout"&&R&&U.type==="number"&&R.memoizedProps.value!=null&&Zi(U,"number",U.value)}switch(oe=R?bl(R):window,e){case"focusin":(Fr(oe)||oe.contentEditable==="true")&&(La=oe,is=R,Cl=null);break;case"focusout":Cl=is=La=null;break;case"mousedown":ss=!0;break;case"contextmenu":case"mouseup":case"dragend":ss=!1,ru(Q,a,X);break;case"selectionchange":if(am)break;case"keydown":case"keyup":ru(Q,a,X)}var be;if(ns)e:{switch(e){case"compositionstart":var Ne="onCompositionStart";break e;case"compositionend":Ne="onCompositionEnd";break e;case"compositionupdate":Ne="onCompositionUpdate";break e}Ne=void 0}else Ga?$r(e,a)&&(Ne="onCompositionEnd"):e==="keydown"&&a.keyCode===229&&(Ne="onCompositionStart");Ne&&(Vr&&a.locale!=="ko"&&(Ga||Ne!=="onCompositionStart"?Ne==="onCompositionEnd"&&Ga&&(be=Yr()):(Bn=X,Wi="value"in Bn?Bn.value:Bn.textContent,Ga=!0)),oe=hi(R,Ne),0<oe.length&&(Ne=new Zr(Ne,e,null,a,X),Q.push({event:Ne,listeners:oe}),be?Ne.data=be:(be=Wr(a),be!==null&&(Ne.data=be)))),(be=Vh?Kh(e,a):Ih(e,a))&&(Ne=hi(R,"onBeforeInput"),0<Ne.length&&(oe=new Zr("onBeforeInput","beforeinput",null,a,X),Q.push({event:oe,listeners:Ne}),oe.data=be)),Lm(Q,e,R,a,X)}Bf(Q,t)})}function Pl(e,t,a){return{instance:e,listener:t,currentTarget:a}}function hi(e,t){for(var a=t+"Capture",l=[];e!==null;){var o=e,i=o.stateNode;if(o=o.tag,o!==5&&o!==26&&o!==27||i===null||(o=_l(e,a),o!=null&&l.unshift(Pl(e,o,i)),o=_l(e,t),o!=null&&l.push(Pl(e,o,i))),e.tag===3)return l;e=e.return}return[]}function Jm(e){if(e===null)return null;do e=e.return;while(e&&e.tag!==5&&e.tag!==27);return e||null}function Hf(e,t,a,l,o){for(var i=t._reactName,r=[];a!==null&&a!==l;){var f=a,_=f.alternate,R=f.stateNode;if(f=f.tag,_!==null&&_===l)break;f!==5&&f!==26&&f!==27||R===null||(_=R,o?(R=_l(a,i),R!=null&&r.unshift(Pl(a,R,_))):o||(R=_l(a,i),R!=null&&r.push(Pl(a,R,_)))),a=a.return}r.length!==0&&e.push({event:t,listeners:r})}var Zm=/\r\n?/g,Qm=/\u0000|\uFFFD/g;function Gf(e){return(typeof e=="string"?e:""+e).replace(Zm,`
`).replace(Qm,"")}function Lf(e,t){return t=Gf(t),Gf(e)===t}function Xe(e,t,a,l,o,i){switch(a){case"children":typeof l=="string"?t==="body"||t==="textarea"&&l===""||Ba(e,l):(typeof l=="number"||typeof l=="bigint")&&t!=="body"&&Ba(e,""+l);break;case"className":vo(e,"class",l);break;case"tabIndex":vo(e,"tabindex",l);break;case"dir":case"role":case"viewBox":case"width":case"height":vo(e,a,l);break;case"style":Gr(e,l,i);break;case"data":if(t!=="object"){vo(e,"data",l);break}case"src":case"href":if(l===""&&(t!=="a"||a!=="href")){e.removeAttribute(a);break}if(l==null||typeof l=="function"||typeof l=="symbol"||typeof l=="boolean"){e.removeAttribute(a);break}l=_o(""+l),e.setAttribute(a,l);break;case"action":case"formAction":if(typeof l=="function"){e.setAttribute(a,"javascript:throw new Error('A React form was unexpectedly submitted. If you called form.submit() manually, consider using form.requestSubmit() instead. If you\\'re trying to use event.stopPropagation() in a submit event handler, consider also calling event.preventDefault().')");break}else typeof i=="function"&&(a==="formAction"?(t!=="input"&&Xe(e,t,"name",o.name,o,null),Xe(e,t,"formEncType",o.formEncType,o,null),Xe(e,t,"formMethod",o.formMethod,o,null),Xe(e,t,"formTarget",o.formTarget,o,null)):(Xe(e,t,"encType",o.encType,o,null),Xe(e,t,"method",o.method,o,null),Xe(e,t,"target",o.target,o,null)));if(l==null||typeof l=="symbol"||typeof l=="boolean"){e.removeAttribute(a);break}l=_o(""+l),e.setAttribute(a,l);break;case"onClick":l!=null&&(e.onclick=hn);break;case"onScroll":l!=null&&we("scroll",e);break;case"onScrollEnd":l!=null&&we("scrollend",e);break;case"dangerouslySetInnerHTML":if(l!=null){if(typeof l!="object"||!("__html"in l))throw Error(u(61));if(a=l.__html,a!=null){if(o.children!=null)throw Error(u(60));e.innerHTML=a}}break;case"multiple":e.multiple=l&&typeof l!="function"&&typeof l!="symbol";break;case"muted":e.muted=l&&typeof l!="function"&&typeof l!="symbol";break;case"suppressContentEditableWarning":case"suppressHydrationWarning":case"defaultValue":case"defaultChecked":case"innerHTML":case"ref":break;case"autoFocus":break;case"xlinkHref":if(l==null||typeof l=="function"||typeof l=="boolean"||typeof l=="symbol"){e.removeAttribute("xlink:href");break}a=_o(""+l),e.setAttributeNS("http://www.w3.org/1999/xlink","xlink:href",a);break;case"contentEditable":case"spellCheck":case"draggable":case"value":case"autoReverse":case"externalResourcesRequired":case"focusable":case"preserveAlpha":l!=null&&typeof l!="function"&&typeof l!="symbol"?e.setAttribute(a,""+l):e.removeAttribute(a);break;case"inert":case"allowFullScreen":case"async":case"autoPlay":case"controls":case"default":case"defer":case"disabled":case"disablePictureInPicture":case"disableRemotePlayback":case"formNoValidate":case"hidden":case"loop":case"noModule":case"noValidate":case"open":case"playsInline":case"readOnly":case"required":case"reversed":case"scoped":case"seamless":case"itemScope":l&&typeof l!="function"&&typeof l!="symbol"?e.setAttribute(a,""):e.removeAttribute(a);break;case"capture":case"download":l===!0?e.setAttribute(a,""):l!==!1&&l!=null&&typeof l!="function"&&typeof l!="symbol"?e.setAttribute(a,l):e.removeAttribute(a);break;case"cols":case"rows":case"size":case"span":l!=null&&typeof l!="function"&&typeof l!="symbol"&&!isNaN(l)&&1<=l?e.setAttribute(a,l):e.removeAttribute(a);break;case"rowSpan":case"start":l==null||typeof l=="function"||typeof l=="symbol"||isNaN(l)?e.removeAttribute(a):e.setAttribute(a,l);break;case"popover":we("beforetoggle",e),we("toggle",e),go(e,"popover",l);break;case"xlinkActuate":pn(e,"http://www.w3.org/1999/xlink","xlink:actuate",l);break;case"xlinkArcrole":pn(e,"http://www.w3.org/1999/xlink","xlink:arcrole",l);break;case"xlinkRole":pn(e,"http://www.w3.org/1999/xlink","xlink:role",l);break;case"xlinkShow":pn(e,"http://www.w3.org/1999/xlink","xlink:show",l);break;case"xlinkTitle":pn(e,"http://www.w3.org/1999/xlink","xlink:title",l);break;case"xlinkType":pn(e,"http://www.w3.org/1999/xlink","xlink:type",l);break;case"xmlBase":pn(e,"http://www.w3.org/XML/1998/namespace","xml:base",l);break;case"xmlLang":pn(e,"http://www.w3.org/XML/1998/namespace","xml:lang",l);break;case"xmlSpace":pn(e,"http://www.w3.org/XML/1998/namespace","xml:space",l);break;case"is":go(e,"is",l);break;case"innerText":case"textContent":break;default:(!(2<a.length)||a[0]!=="o"&&a[0]!=="O"||a[1]!=="n"&&a[1]!=="N")&&(a=_h.get(a)||a,go(e,a,l))}}function Oc(e,t,a,l,o,i){switch(a){case"style":Gr(e,l,i);break;case"dangerouslySetInnerHTML":if(l!=null){if(typeof l!="object"||!("__html"in l))throw Error(u(61));if(a=l.__html,a!=null){if(o.children!=null)throw Error(u(60));e.innerHTML=a}}break;case"children":typeof l=="string"?Ba(e,l):(typeof l=="number"||typeof l=="bigint")&&Ba(e,""+l);break;case"onScroll":l!=null&&we("scroll",e);break;case"onScrollEnd":l!=null&&we("scrollend",e);break;case"onClick":l!=null&&(e.onclick=hn);break;case"suppressContentEditableWarning":case"suppressHydrationWarning":case"innerHTML":case"ref":break;case"innerText":case"textContent":break;default:if(!Mr.hasOwnProperty(a))e:{if(a[0]==="o"&&a[1]==="n"&&(o=a.endsWith("Capture"),t=a.slice(2,o?a.length-7:void 0),i=e[At]||null,i=i!=null?i[a]:null,typeof i=="function"&&e.removeEventListener(t,i,o),typeof l=="function")){typeof i!="function"&&i!==null&&(a in e?e[a]=null:e.hasAttribute(a)&&e.removeAttribute(a)),e.addEventListener(t,l,o);break e}a in e?e[a]=l:l===!0?e.setAttribute(a,""):go(e,a,l)}}}function bt(e,t,a){switch(t){case"div":case"span":case"svg":case"path":case"a":case"g":case"p":case"li":break;case"img":we("error",e),we("load",e);var l=!1,o=!1,i;for(i in a)if(a.hasOwnProperty(i)){var r=a[i];if(r!=null)switch(i){case"src":l=!0;break;case"srcSet":o=!0;break;case"children":case"dangerouslySetInnerHTML":throw Error(u(137,t));default:Xe(e,t,i,r,a,null)}}o&&Xe(e,t,"srcSet",a.srcSet,a,null),l&&Xe(e,t,"src",a.src,a,null);return;case"input":we("invalid",e);var f=i=r=o=null,_=null,R=null;for(l in a)if(a.hasOwnProperty(l)){var X=a[l];if(X!=null)switch(l){case"name":o=X;break;case"type":r=X;break;case"checked":_=X;break;case"defaultChecked":R=X;break;case"value":i=X;break;case"defaultValue":f=X;break;case"children":case"dangerouslySetInnerHTML":if(X!=null)throw Error(u(137,t));break;default:Xe(e,t,l,X,a,null)}}kr(e,i,f,_,R,r,o,!1);return;case"select":we("invalid",e),l=r=i=null;for(o in a)if(a.hasOwnProperty(o)&&(f=a[o],f!=null))switch(o){case"value":i=f;break;case"defaultValue":r=f;break;case"multiple":l=f;default:Xe(e,t,o,f,a,null)}t=i,a=r,e.multiple=!!l,t!=null?ka(e,!!l,t,!1):a!=null&&ka(e,!!l,a,!0);return;case"textarea":we("invalid",e),i=o=l=null;for(r in a)if(a.hasOwnProperty(r)&&(f=a[r],f!=null))switch(r){case"value":l=f;break;case"defaultValue":o=f;break;case"children":i=f;break;case"dangerouslySetInnerHTML":if(f!=null)throw Error(u(91));break;default:Xe(e,t,r,f,a,null)}Ur(e,l,o,i);return;case"option":for(_ in a)if(a.hasOwnProperty(_)&&(l=a[_],l!=null))switch(_){case"selected":e.selected=l&&typeof l!="function"&&typeof l!="symbol";break;default:Xe(e,t,_,l,a,null)}return;case"dialog":we("beforetoggle",e),we("toggle",e),we("cancel",e),we("close",e);break;case"iframe":case"object":we("load",e);break;case"video":case"audio":for(l=0;l<Fl.length;l++)we(Fl[l],e);break;case"image":we("error",e),we("load",e);break;case"details":we("toggle",e);break;case"embed":case"source":case"link":we("error",e),we("load",e);case"area":case"base":case"br":case"col":case"hr":case"keygen":case"meta":case"param":case"track":case"wbr":case"menuitem":for(R in a)if(a.hasOwnProperty(R)&&(l=a[R],l!=null))switch(R){case"children":case"dangerouslySetInnerHTML":throw Error(u(137,t));default:Xe(e,t,R,l,a,null)}return;default:if(Qi(t)){for(X in a)a.hasOwnProperty(X)&&(l=a[X],l!==void 0&&Oc(e,t,X,l,a,void 0));return}}for(f in a)a.hasOwnProperty(f)&&(l=a[f],l!=null&&Xe(e,t,f,l,a,null))}function Vm(e,t,a,l){switch(t){case"div":case"span":case"svg":case"path":case"a":case"g":case"p":case"li":break;case"input":var o=null,i=null,r=null,f=null,_=null,R=null,X=null;for(G in a){var Q=a[G];if(a.hasOwnProperty(G)&&Q!=null)switch(G){case"checked":break;case"value":break;case"defaultValue":_=Q;default:l.hasOwnProperty(G)||Xe(e,t,G,null,l,Q)}}for(var U in l){var G=l[U];if(Q=a[U],l.hasOwnProperty(U)&&(G!=null||Q!=null))switch(U){case"type":i=G;break;case"name":o=G;break;case"checked":R=G;break;case"defaultChecked":X=G;break;case"value":r=G;break;case"defaultValue":f=G;break;case"children":case"dangerouslySetInnerHTML":if(G!=null)throw Error(u(137,t));break;default:G!==Q&&Xe(e,t,U,G,l,Q)}}Ji(e,r,f,_,R,X,i,o);return;case"select":G=r=f=U=null;for(i in a)if(_=a[i],a.hasOwnProperty(i)&&_!=null)switch(i){case"value":break;case"multiple":G=_;default:l.hasOwnProperty(i)||Xe(e,t,i,null,l,_)}for(o in l)if(i=l[o],_=a[o],l.hasOwnProperty(o)&&(i!=null||_!=null))switch(o){case"value":U=i;break;case"defaultValue":f=i;break;case"multiple":r=i;default:i!==_&&Xe(e,t,o,i,l,_)}t=f,a=r,l=G,U!=null?ka(e,!!a,U,!1):!!l!=!!a&&(t!=null?ka(e,!!a,t,!0):ka(e,!!a,a?[]:"",!1));return;case"textarea":G=U=null;for(f in a)if(o=a[f],a.hasOwnProperty(f)&&o!=null&&!l.hasOwnProperty(f))switch(f){case"value":break;case"children":break;default:Xe(e,t,f,null,l,o)}for(r in l)if(o=l[r],i=a[r],l.hasOwnProperty(r)&&(o!=null||i!=null))switch(r){case"value":U=o;break;case"defaultValue":G=o;break;case"children":break;case"dangerouslySetInnerHTML":if(o!=null)throw Error(u(91));break;default:o!==i&&Xe(e,t,r,o,l,i)}Br(e,U,G);return;case"option":for(var ae in a)if(U=a[ae],a.hasOwnProperty(ae)&&U!=null&&!l.hasOwnProperty(ae))switch(ae){case"selected":e.selected=!1;break;default:Xe(e,t,ae,null,l,U)}for(_ in l)if(U=l[_],G=a[_],l.hasOwnProperty(_)&&U!==G&&(U!=null||G!=null))switch(_){case"selected":e.selected=U&&typeof U!="function"&&typeof U!="symbol";break;default:Xe(e,t,_,U,l,G)}return;case"img":case"link":case"area":case"base":case"br":case"col":case"embed":case"hr":case"keygen":case"meta":case"param":case"source":case"track":case"wbr":case"menuitem":for(var fe in a)U=a[fe],a.hasOwnProperty(fe)&&U!=null&&!l.hasOwnProperty(fe)&&Xe(e,t,fe,null,l,U);for(R in l)if(U=l[R],G=a[R],l.hasOwnProperty(R)&&U!==G&&(U!=null||G!=null))switch(R){case"children":case"dangerouslySetInnerHTML":if(U!=null)throw Error(u(137,t));break;default:Xe(e,t,R,U,l,G)}return;default:if(Qi(t)){for(var Je in a)U=a[Je],a.hasOwnProperty(Je)&&U!==void 0&&!l.hasOwnProperty(Je)&&Oc(e,t,Je,void 0,l,U);for(X in l)U=l[X],G=a[X],!l.hasOwnProperty(X)||U===G||U===void 0&&G===void 0||Oc(e,t,X,U,l,G);return}}for(var j in a)U=a[j],a.hasOwnProperty(j)&&U!=null&&!l.hasOwnProperty(j)&&Xe(e,t,j,null,l,U);for(Q in l)U=l[Q],G=a[Q],!l.hasOwnProperty(Q)||U===G||U==null&&G==null||Xe(e,t,Q,U,l,G)}function qf(e){switch(e){case"css":case"script":case"font":case"img":case"image":case"input":case"link":return!0;default:return!1}}function Km(){if(typeof performance.getEntriesByType=="function"){for(var e=0,t=0,a=performance.getEntriesByType("resource"),l=0;l<a.length;l++){var o=a[l],i=o.transferSize,r=o.initiatorType,f=o.duration;if(i&&f&&qf(r)){for(r=0,f=o.responseEnd,l+=1;l<a.length;l++){var _=a[l],R=_.startTime;if(R>f)break;var X=_.transferSize,Q=_.initiatorType;X&&qf(Q)&&(_=_.responseEnd,r+=X*(_<f?1:(f-R)/(_-R)))}if(--l,t+=8*(i+r)/(o.duration/1e3),e++,10<e)break}}if(0<e)return t/e/1e6}return navigator.connection&&(e=navigator.connection.downlink,typeof e=="number")?e:5}var zc=null,Rc=null;function mi(e){return e.nodeType===9?e:e.ownerDocument}function Yf(e){switch(e){case"http://www.w3.org/2000/svg":return 1;case"http://www.w3.org/1998/Math/MathML":return 2;default:return 0}}function Xf(e,t){if(e===0)switch(t){case"svg":return 1;case"math":return 2;default:return 0}return e===1&&t==="foreignObject"?0:e}function kc(e,t){return e==="textarea"||e==="noscript"||typeof t.children=="string"||typeof t.children=="number"||typeof t.children=="bigint"||typeof t.dangerouslySetInnerHTML=="object"&&t.dangerouslySetInnerHTML!==null&&t.dangerouslySetInnerHTML.__html!=null}var Bc=null;function Im(){var e=window.event;return e&&e.type==="popstate"?e===Bc?!1:(Bc=e,!0):(Bc=null,!1)}var Jf=typeof setTimeout=="function"?setTimeout:void 0,$m=typeof clearTimeout=="function"?clearTimeout:void 0,Zf=typeof Promise=="function"?Promise:void 0,Wm=typeof queueMicrotask=="function"?queueMicrotask:typeof Zf<"u"?function(e){return Zf.resolve(null).then(e).catch(Fm)}:Jf;function Fm(e){setTimeout(function(){throw e})}function Pn(e){return e==="head"}function Qf(e,t){var a=t,l=0;do{var o=a.nextSibling;if(e.removeChild(a),o&&o.nodeType===8)if(a=o.data,a==="/$"||a==="/&"){if(l===0){e.removeChild(o),pl(t);return}l--}else if(a==="$"||a==="$?"||a==="$~"||a==="$!"||a==="&")l++;else if(a==="html")eo(e.ownerDocument.documentElement);else if(a==="head"){a=e.ownerDocument.head,eo(a);for(var i=a.firstChild;i;){var r=i.nextSibling,f=i.nodeName;i[vl]||f==="SCRIPT"||f==="STYLE"||f==="LINK"&&i.rel.toLowerCase()==="stylesheet"||a.removeChild(i),i=r}}else a==="body"&&eo(e.ownerDocument.body);a=o}while(a);pl(t)}function Vf(e,t){var a=e;e=0;do{var l=a.nextSibling;if(a.nodeType===1?t?(a._stashedDisplay=a.style.display,a.style.display="none"):(a.style.display=a._stashedDisplay||"",a.getAttribute("style")===""&&a.removeAttribute("style")):a.nodeType===3&&(t?(a._stashedText=a.nodeValue,a.nodeValue=""):a.nodeValue=a._stashedText||""),l&&l.nodeType===8)if(a=l.data,a==="/$"){if(e===0)break;e--}else a!=="$"&&a!=="$?"&&a!=="$~"&&a!=="$!"||e++;a=l}while(a)}function Uc(e){var t=e.firstChild;for(t&&t.nodeType===10&&(t=t.nextSibling);t;){var a=t;switch(t=t.nextSibling,a.nodeName){case"HTML":case"HEAD":case"BODY":Uc(a),Yi(a);continue;case"SCRIPT":case"STYLE":continue;case"LINK":if(a.rel.toLowerCase()==="stylesheet")continue}e.removeChild(a)}}function Pm(e,t,a,l){for(;e.nodeType===1;){var o=a;if(e.nodeName.toLowerCase()!==t.toLowerCase()){if(!l&&(e.nodeName!=="INPUT"||e.type!=="hidden"))break}else if(l){if(!e[vl])switch(t){case"meta":if(!e.hasAttribute("itemprop"))break;return e;case"link":if(i=e.getAttribute("rel"),i==="stylesheet"&&e.hasAttribute("data-precedence"))break;if(i!==o.rel||e.getAttribute("href")!==(o.href==null||o.href===""?null:o.href)||e.getAttribute("crossorigin")!==(o.crossOrigin==null?null:o.crossOrigin)||e.getAttribute("title")!==(o.title==null?null:o.title))break;return e;case"style":if(e.hasAttribute("data-precedence"))break;return e;case"script":if(i=e.getAttribute("src"),(i!==(o.src==null?null:o.src)||e.getAttribute("type")!==(o.type==null?null:o.type)||e.getAttribute("crossorigin")!==(o.crossOrigin==null?null:o.crossOrigin))&&i&&e.hasAttribute("async")&&!e.hasAttribute("itemprop"))break;return e;default:return e}}else if(t==="input"&&e.type==="hidden"){var i=o.name==null?null:""+o.name;if(o.type==="hidden"&&e.getAttribute("name")===i)return e}else return e;if(e=Wt(e.nextSibling),e===null)break}return null}function ey(e,t,a){if(t==="")return null;for(;e.nodeType!==3;)if((e.nodeType!==1||e.nodeName!=="INPUT"||e.type!=="hidden")&&!a||(e=Wt(e.nextSibling),e===null))return null;return e}function Kf(e,t){for(;e.nodeType!==8;)if((e.nodeType!==1||e.nodeName!=="INPUT"||e.type!=="hidden")&&!t||(e=Wt(e.nextSibling),e===null))return null;return e}function Hc(e){return e.data==="$?"||e.data==="$~"}function Gc(e){return e.data==="$!"||e.data==="$?"&&e.ownerDocument.readyState!=="loading"}function ty(e,t){var a=e.ownerDocument;if(e.data==="$~")e._reactRetry=t;else if(e.data!=="$?"||a.readyState!=="loading")t();else{var l=function(){t(),a.removeEventListener("DOMContentLoaded",l)};a.addEventListener("DOMContentLoaded",l),e._reactRetry=l}}function Wt(e){for(;e!=null;e=e.nextSibling){var t=e.nodeType;if(t===1||t===3)break;if(t===8){if(t=e.data,t==="$"||t==="$!"||t==="$?"||t==="$~"||t==="&"||t==="F!"||t==="F")break;if(t==="/$"||t==="/&")return null}}return e}var Lc=null;function If(e){e=e.nextSibling;for(var t=0;e;){if(e.nodeType===8){var a=e.data;if(a==="/$"||a==="/&"){if(t===0)return Wt(e.nextSibling);t--}else a!=="$"&&a!=="$!"&&a!=="$?"&&a!=="$~"&&a!=="&"||t++}e=e.nextSibling}return null}function $f(e){e=e.previousSibling;for(var t=0;e;){if(e.nodeType===8){var a=e.data;if(a==="$"||a==="$!"||a==="$?"||a==="$~"||a==="&"){if(t===0)return e;t--}else a!=="/$"&&a!=="/&"||t++}e=e.previousSibling}return null}function Wf(e,t,a){switch(t=mi(a),e){case"html":if(e=t.documentElement,!e)throw Error(u(452));return e;case"head":if(e=t.head,!e)throw Error(u(453));return e;case"body":if(e=t.body,!e)throw Error(u(454));return e;default:throw Error(u(451))}}function eo(e){for(var t=e.attributes;t.length;)e.removeAttributeNode(t[0]);Yi(e)}var Ft=new Map,Ff=new Set;function yi(e){return typeof e.getRootNode=="function"?e.getRootNode():e.nodeType===9?e:e.ownerDocument}var Mn=D.d;D.d={f:ny,r:ay,D:ly,C:oy,L:iy,m:sy,X:ry,S:cy,M:uy};function ny(){var e=Mn.f(),t=si();return e||t}function ay(e){var t=Oa(e);t!==null&&t.tag===5&&t.type==="form"?hd(t):Mn.r(e)}var ul=typeof document>"u"?null:document;function Pf(e,t,a){var l=ul;if(l&&typeof t=="string"&&t){var o=Jt(t);o='link[rel="'+e+'"][href="'+o+'"]',typeof a=="string"&&(o+='[crossorigin="'+a+'"]'),Ff.has(o)||(Ff.add(o),e={rel:e,crossOrigin:a,href:t},l.querySelector(o)===null&&(t=l.createElement("link"),bt(t,"link",e),ft(t),l.head.appendChild(t)))}}function ly(e){Mn.D(e),Pf("dns-prefetch",e,null)}function oy(e,t){Mn.C(e,t),Pf("preconnect",e,t)}function iy(e,t,a){Mn.L(e,t,a);var l=ul;if(l&&e&&t){var o='link[rel="preload"][as="'+Jt(t)+'"]';t==="image"&&a&&a.imageSrcSet?(o+='[imagesrcset="'+Jt(a.imageSrcSet)+'"]',typeof a.imageSizes=="string"&&(o+='[imagesizes="'+Jt(a.imageSizes)+'"]')):o+='[href="'+Jt(e)+'"]';var i=o;switch(t){case"style":i=dl(e);break;case"script":i=fl(e)}Ft.has(i)||(e=v({rel:"preload",href:t==="image"&&a&&a.imageSrcSet?void 0:e,as:t},a),Ft.set(i,e),l.querySelector(o)!==null||t==="style"&&l.querySelector(to(i))||t==="script"&&l.querySelector(no(i))||(t=l.createElement("link"),bt(t,"link",e),ft(t),l.head.appendChild(t)))}}function sy(e,t){Mn.m(e,t);var a=ul;if(a&&e){var l=t&&typeof t.as=="string"?t.as:"script",o='link[rel="modulepreload"][as="'+Jt(l)+'"][href="'+Jt(e)+'"]',i=o;switch(l){case"audioworklet":case"paintworklet":case"serviceworker":case"sharedworker":case"worker":case"script":i=fl(e)}if(!Ft.has(i)&&(e=v({rel:"modulepreload",href:e},t),Ft.set(i,e),a.querySelector(o)===null)){switch(l){case"audioworklet":case"paintworklet":case"serviceworker":case"sharedworker":case"worker":case"script":if(a.querySelector(no(i)))return}l=a.createElement("link"),bt(l,"link",e),ft(l),a.head.appendChild(l)}}}function cy(e,t,a){Mn.S(e,t,a);var l=ul;if(l&&e){var o=za(l).hoistableStyles,i=dl(e);t=t||"default";var r=o.get(i);if(!r){var f={loading:0,preload:null};if(r=l.querySelector(to(i)))f.loading=5;else{e=v({rel:"stylesheet",href:e,"data-precedence":t},a),(a=Ft.get(i))&&qc(e,a);var _=r=l.createElement("link");ft(_),bt(_,"link",e),_._p=new Promise(function(R,X){_.onload=R,_.onerror=X}),_.addEventListener("load",function(){f.loading|=1}),_.addEventListener("error",function(){f.loading|=2}),f.loading|=4,gi(r,t,l)}r={type:"stylesheet",instance:r,count:1,state:f},o.set(i,r)}}}function ry(e,t){Mn.X(e,t);var a=ul;if(a&&e){var l=za(a).hoistableScripts,o=fl(e),i=l.get(o);i||(i=a.querySelector(no(o)),i||(e=v({src:e,async:!0},t),(t=Ft.get(o))&&Yc(e,t),i=a.createElement("script"),ft(i),bt(i,"link",e),a.head.appendChild(i)),i={type:"script",instance:i,count:1,state:null},l.set(o,i))}}function uy(e,t){Mn.M(e,t);var a=ul;if(a&&e){var l=za(a).hoistableScripts,o=fl(e),i=l.get(o);i||(i=a.querySelector(no(o)),i||(e=v({src:e,async:!0,type:"module"},t),(t=Ft.get(o))&&Yc(e,t),i=a.createElement("script"),ft(i),bt(i,"link",e),a.head.appendChild(i)),i={type:"script",instance:i,count:1,state:null},l.set(o,i))}}function ep(e,t,a,l){var o=(o=xe.current)?yi(o):null;if(!o)throw Error(u(446));switch(e){case"meta":case"title":return null;case"style":return typeof a.precedence=="string"&&typeof a.href=="string"?(t=dl(a.href),a=za(o).hoistableStyles,l=a.get(t),l||(l={type:"style",instance:null,count:0,state:null},a.set(t,l)),l):{type:"void",instance:null,count:0,state:null};case"link":if(a.rel==="stylesheet"&&typeof a.href=="string"&&typeof a.precedence=="string"){e=dl(a.href);var i=za(o).hoistableStyles,r=i.get(e);if(r||(o=o.ownerDocument||o,r={type:"stylesheet",instance:null,count:0,state:{loading:0,preload:null}},i.set(e,r),(i=o.querySelector(to(e)))&&!i._p&&(r.instance=i,r.state.loading=5),Ft.has(e)||(a={rel:"preload",as:"style",href:a.href,crossOrigin:a.crossOrigin,integrity:a.integrity,media:a.media,hrefLang:a.hrefLang,referrerPolicy:a.referrerPolicy},Ft.set(e,a),i||dy(o,e,a,r.state))),t&&l===null)throw Error(u(528,""));return r}if(t&&l!==null)throw Error(u(529,""));return null;case"script":return t=a.async,a=a.src,typeof a=="string"&&t&&typeof t!="function"&&typeof t!="symbol"?(t=fl(a),a=za(o).hoistableScripts,l=a.get(t),l||(l={type:"script",instance:null,count:0,state:null},a.set(t,l)),l):{type:"void",instance:null,count:0,state:null};default:throw Error(u(444,e))}}function dl(e){return'href="'+Jt(e)+'"'}function to(e){return'link[rel="stylesheet"]['+e+"]"}function tp(e){return v({},e,{"data-precedence":e.precedence,precedence:null})}function dy(e,t,a,l){e.querySelector('link[rel="preload"][as="style"]['+t+"]")?l.loading=1:(t=e.createElement("link"),l.preload=t,t.addEventListener("load",function(){return l.loading|=1}),t.addEventListener("error",function(){return l.loading|=2}),bt(t,"link",a),ft(t),e.head.appendChild(t))}function fl(e){return'[src="'+Jt(e)+'"]'}function no(e){return"script[async]"+e}function np(e,t,a){if(t.count++,t.instance===null)switch(t.type){case"style":var l=e.querySelector('style[data-href~="'+Jt(a.href)+'"]');if(l)return t.instance=l,ft(l),l;var o=v({},a,{"data-href":a.href,"data-precedence":a.precedence,href:null,precedence:null});return l=(e.ownerDocument||e).createElement("style"),ft(l),bt(l,"style",o),gi(l,a.precedence,e),t.instance=l;case"stylesheet":o=dl(a.href);var i=e.querySelector(to(o));if(i)return t.state.loading|=4,t.instance=i,ft(i),i;l=tp(a),(o=Ft.get(o))&&qc(l,o),i=(e.ownerDocument||e).createElement("link"),ft(i);var r=i;return r._p=new Promise(function(f,_){r.onload=f,r.onerror=_}),bt(i,"link",l),t.state.loading|=4,gi(i,a.precedence,e),t.instance=i;case"script":return i=fl(a.src),(o=e.querySelector(no(i)))?(t.instance=o,ft(o),o):(l=a,(o=Ft.get(i))&&(l=v({},a),Yc(l,o)),e=e.ownerDocument||e,o=e.createElement("script"),ft(o),bt(o,"link",l),e.head.appendChild(o),t.instance=o);case"void":return null;default:throw Error(u(443,t.type))}else t.type==="stylesheet"&&(t.state.loading&4)===0&&(l=t.instance,t.state.loading|=4,gi(l,a.precedence,e));return t.instance}function gi(e,t,a){for(var l=a.querySelectorAll('link[rel="stylesheet"][data-precedence],style[data-precedence]'),o=l.length?l[l.length-1]:null,i=o,r=0;r<l.length;r++){var f=l[r];if(f.dataset.precedence===t)i=f;else if(i!==o)break}i?i.parentNode.insertBefore(e,i.nextSibling):(t=a.nodeType===9?a.head:a,t.insertBefore(e,t.firstChild))}function qc(e,t){e.crossOrigin==null&&(e.crossOrigin=t.crossOrigin),e.referrerPolicy==null&&(e.referrerPolicy=t.referrerPolicy),e.title==null&&(e.title=t.title)}function Yc(e,t){e.crossOrigin==null&&(e.crossOrigin=t.crossOrigin),e.referrerPolicy==null&&(e.referrerPolicy=t.referrerPolicy),e.integrity==null&&(e.integrity=t.integrity)}var vi=null;function ap(e,t,a){if(vi===null){var l=new Map,o=vi=new Map;o.set(a,l)}else o=vi,l=o.get(a),l||(l=new Map,o.set(a,l));if(l.has(e))return l;for(l.set(e,null),a=a.getElementsByTagName(e),o=0;o<a.length;o++){var i=a[o];if(!(i[vl]||i[mt]||e==="link"&&i.getAttribute("rel")==="stylesheet")&&i.namespaceURI!=="http://www.w3.org/2000/svg"){var r=i.getAttribute(t)||"";r=e+r;var f=l.get(r);f?f.push(i):l.set(r,[i])}}return l}function lp(e,t,a){e=e.ownerDocument||e,e.head.insertBefore(a,t==="title"?e.querySelector("head > title"):null)}function fy(e,t,a){if(a===1||t.itemProp!=null)return!1;switch(e){case"meta":case"title":return!0;case"style":if(typeof t.precedence!="string"||typeof t.href!="string"||t.href==="")break;return!0;case"link":if(typeof t.rel!="string"||typeof t.href!="string"||t.href===""||t.onLoad||t.onError)break;switch(t.rel){case"stylesheet":return e=t.disabled,typeof t.precedence=="string"&&e==null;default:return!0}case"script":if(t.async&&typeof t.async!="function"&&typeof t.async!="symbol"&&!t.onLoad&&!t.onError&&t.src&&typeof t.src=="string")return!0}return!1}function op(e){return!(e.type==="stylesheet"&&(e.state.loading&3)===0)}function py(e,t,a,l){if(a.type==="stylesheet"&&(typeof l.media!="string"||matchMedia(l.media).matches!==!1)&&(a.state.loading&4)===0){if(a.instance===null){var o=dl(l.href),i=t.querySelector(to(o));if(i){t=i._p,t!==null&&typeof t=="object"&&typeof t.then=="function"&&(e.count++,e=bi.bind(e),t.then(e,e)),a.state.loading|=4,a.instance=i,ft(i);return}i=t.ownerDocument||t,l=tp(l),(o=Ft.get(o))&&qc(l,o),i=i.createElement("link"),ft(i);var r=i;r._p=new Promise(function(f,_){r.onload=f,r.onerror=_}),bt(i,"link",l),a.instance=i}e.stylesheets===null&&(e.stylesheets=new Map),e.stylesheets.set(a,t),(t=a.state.preload)&&(a.state.loading&3)===0&&(e.count++,a=bi.bind(e),t.addEventListener("load",a),t.addEventListener("error",a))}}var Xc=0;function hy(e,t){return e.stylesheets&&e.count===0&&Si(e,e.stylesheets),0<e.count||0<e.imgCount?function(a){var l=setTimeout(function(){if(e.stylesheets&&Si(e,e.stylesheets),e.unsuspend){var i=e.unsuspend;e.unsuspend=null,i()}},6e4+t);0<e.imgBytes&&Xc===0&&(Xc=62500*Km());var o=setTimeout(function(){if(e.waitingForImages=!1,e.count===0&&(e.stylesheets&&Si(e,e.stylesheets),e.unsuspend)){var i=e.unsuspend;e.unsuspend=null,i()}},(e.imgBytes>Xc?50:800)+t);return e.unsuspend=a,function(){e.unsuspend=null,clearTimeout(l),clearTimeout(o)}}:null}function bi(){if(this.count--,this.count===0&&(this.imgCount===0||!this.waitingForImages)){if(this.stylesheets)Si(this,this.stylesheets);else if(this.unsuspend){var e=this.unsuspend;this.unsuspend=null,e()}}}var _i=null;function Si(e,t){e.stylesheets=null,e.unsuspend!==null&&(e.count++,_i=new Map,t.forEach(my,e),_i=null,bi.call(e))}function my(e,t){if(!(t.state.loading&4)){var a=_i.get(e);if(a)var l=a.get(null);else{a=new Map,_i.set(e,a);for(var o=e.querySelectorAll("link[data-precedence],style[data-precedence]"),i=0;i<o.length;i++){var r=o[i];(r.nodeName==="LINK"||r.getAttribute("media")!=="not all")&&(a.set(r.dataset.precedence,r),l=r)}l&&a.set(null,l)}o=t.instance,r=o.getAttribute("data-precedence"),i=a.get(r)||l,i===l&&a.set(null,o),a.set(r,o),this.count++,l=bi.bind(this),o.addEventListener("load",l),o.addEventListener("error",l),i?i.parentNode.insertBefore(o,i.nextSibling):(e=e.nodeType===9?e.head:e,e.insertBefore(o,e.firstChild)),t.state.loading|=4}}var ao={$$typeof:H,Provider:null,Consumer:null,_currentValue:V,_currentValue2:V,_threadCount:0};function yy(e,t,a,l,o,i,r,f,_){this.tag=1,this.containerInfo=e,this.pingCache=this.current=this.pendingChildren=null,this.timeoutHandle=-1,this.callbackNode=this.next=this.pendingContext=this.context=this.cancelPendingCommit=null,this.callbackPriority=0,this.expirationTimes=Hi(-1),this.entangledLanes=this.shellSuspendCounter=this.errorRecoveryDisabledLanes=this.expiredLanes=this.warmLanes=this.pingedLanes=this.suspendedLanes=this.pendingLanes=0,this.entanglements=Hi(0),this.hiddenUpdates=Hi(null),this.identifierPrefix=l,this.onUncaughtError=o,this.onCaughtError=i,this.onRecoverableError=r,this.pooledCache=null,this.pooledCacheLanes=0,this.formState=_,this.incompleteTransitions=new Map}function ip(e,t,a,l,o,i,r,f,_,R,X,Q){return e=new yy(e,t,a,r,_,R,X,Q,f),t=1,i===!0&&(t|=24),i=kt(3,null,null,t),e.current=i,i.stateNode=e,t=xs(),t.refCount++,e.pooledCache=t,t.refCount++,i.memoizedState={element:l,isDehydrated:a,cache:t},As(i),e}function sp(e){return e?(e=Xa,e):Xa}function cp(e,t,a,l,o,i){o=sp(o),l.context===null?l.context=o:l.pendingContext=o,l=Yn(t),l.payload={element:a},i=i===void 0?null:i,i!==null&&(l.callback=i),a=Xn(e,l,t),a!==null&&(Ot(a,e,t),kl(a,e,t))}function rp(e,t){if(e=e.memoizedState,e!==null&&e.dehydrated!==null){var a=e.retryLane;e.retryLane=a!==0&&a<t?a:t}}function Jc(e,t){rp(e,t),(e=e.alternate)&&rp(e,t)}function up(e){if(e.tag===13||e.tag===31){var t=ma(e,67108864);t!==null&&Ot(t,e,67108864),Jc(e,67108864)}}function dp(e){if(e.tag===13||e.tag===31){var t=Lt();t=Gi(t);var a=ma(e,t);a!==null&&Ot(a,e,t),Jc(e,t)}}var xi=!0;function gy(e,t,a,l){var o=M.T;M.T=null;var i=D.p;try{D.p=2,Zc(e,t,a,l)}finally{D.p=i,M.T=o}}function vy(e,t,a,l){var o=M.T;M.T=null;var i=D.p;try{D.p=8,Zc(e,t,a,l)}finally{D.p=i,M.T=o}}function Zc(e,t,a,l){if(xi){var o=Qc(l);if(o===null)Dc(e,t,l,Ti,a),pp(e,l);else if(_y(o,e,t,a,l))l.stopPropagation();else if(pp(e,l),t&4&&-1<by.indexOf(e)){for(;o!==null;){var i=Oa(o);if(i!==null)switch(i.tag){case 3:if(i=i.stateNode,i.current.memoizedState.isDehydrated){var r=ua(i.pendingLanes);if(r!==0){var f=i;for(f.pendingLanes|=2,f.entangledLanes|=2;r;){var _=1<<31-xt(r);f.entanglements[1]|=_,r&=~_}rn(i),(ke&6)===0&&(oi=ge()+500,Wl(0))}}break;case 31:case 13:f=ma(i,2),f!==null&&Ot(f,i,2),si(),Jc(i,2)}if(i=Qc(l),i===null&&Dc(e,t,l,Ti,a),i===o)break;o=i}o!==null&&l.stopPropagation()}else Dc(e,t,l,null,a)}}function Qc(e){return e=Ki(e),Vc(e)}var Ti=null;function Vc(e){if(Ti=null,e=Da(e),e!==null){var t=p(e);if(t===null)e=null;else{var a=t.tag;if(a===13){if(e=b(t),e!==null)return e;e=null}else if(a===31){if(e=y(t),e!==null)return e;e=null}else if(a===3){if(t.stateNode.current.memoizedState.isDehydrated)return t.tag===3?t.stateNode.containerInfo:null;e=null}else t!==e&&(e=null)}}return Ti=e,null}function fp(e){switch(e){case"beforetoggle":case"cancel":case"click":case"close":case"contextmenu":case"copy":case"cut":case"auxclick":case"dblclick":case"dragend":case"dragstart":case"drop":case"focusin":case"focusout":case"input":case"invalid":case"keydown":case"keypress":case"keyup":case"mousedown":case"mouseup":case"paste":case"pause":case"play":case"pointercancel":case"pointerdown":case"pointerup":case"ratechange":case"reset":case"resize":case"seeked":case"submit":case"toggle":case"touchcancel":case"touchend":case"touchstart":case"volumechange":case"change":case"selectionchange":case"textInput":case"compositionstart":case"compositionend":case"compositionupdate":case"beforeblur":case"afterblur":case"beforeinput":case"blur":case"fullscreenchange":case"focus":case"hashchange":case"popstate":case"select":case"selectstart":return 2;case"drag":case"dragenter":case"dragexit":case"dragleave":case"dragover":case"mousemove":case"mouseout":case"mouseover":case"pointermove":case"pointerout":case"pointerover":case"scroll":case"touchmove":case"wheel":case"mouseenter":case"mouseleave":case"pointerenter":case"pointerleave":return 8;case"message":switch(fn()){case po:return 2;case ho:return 8;case ca:case Ri:return 32;case mo:return 268435456;default:return 32}default:return 32}}var Kc=!1,ea=null,ta=null,na=null,lo=new Map,oo=new Map,aa=[],by="mousedown mouseup touchcancel touchend touchstart auxclick dblclick pointercancel pointerdown pointerup dragend dragstart drop compositionend compositionstart keydown keypress keyup input textInput copy cut paste click change contextmenu reset".split(" ");function pp(e,t){switch(e){case"focusin":case"focusout":ea=null;break;case"dragenter":case"dragleave":ta=null;break;case"mouseover":case"mouseout":na=null;break;case"pointerover":case"pointerout":lo.delete(t.pointerId);break;case"gotpointercapture":case"lostpointercapture":oo.delete(t.pointerId)}}function io(e,t,a,l,o,i){return e===null||e.nativeEvent!==i?(e={blockedOn:t,domEventName:a,eventSystemFlags:l,nativeEvent:i,targetContainers:[o]},t!==null&&(t=Oa(t),t!==null&&up(t)),e):(e.eventSystemFlags|=l,t=e.targetContainers,o!==null&&t.indexOf(o)===-1&&t.push(o),e)}function _y(e,t,a,l,o){switch(t){case"focusin":return ea=io(ea,e,t,a,l,o),!0;case"dragenter":return ta=io(ta,e,t,a,l,o),!0;case"mouseover":return na=io(na,e,t,a,l,o),!0;case"pointerover":var i=o.pointerId;return lo.set(i,io(lo.get(i)||null,e,t,a,l,o)),!0;case"gotpointercapture":return i=o.pointerId,oo.set(i,io(oo.get(i)||null,e,t,a,l,o)),!0}return!1}function hp(e){var t=Da(e.target);if(t!==null){var a=p(t);if(a!==null){if(t=a.tag,t===13){if(t=b(a),t!==null){e.blockedOn=t,Nr(e.priority,function(){dp(a)});return}}else if(t===31){if(t=y(a),t!==null){e.blockedOn=t,Nr(e.priority,function(){dp(a)});return}}else if(t===3&&a.stateNode.current.memoizedState.isDehydrated){e.blockedOn=a.tag===3?a.stateNode.containerInfo:null;return}}}e.blockedOn=null}function Ei(e){if(e.blockedOn!==null)return!1;for(var t=e.targetContainers;0<t.length;){var a=Qc(e.nativeEvent);if(a===null){a=e.nativeEvent;var l=new a.constructor(a.type,a);Vi=l,a.target.dispatchEvent(l),Vi=null}else return t=Oa(a),t!==null&&up(t),e.blockedOn=a,!1;t.shift()}return!0}function mp(e,t,a){Ei(e)&&a.delete(t)}function Sy(){Kc=!1,ea!==null&&Ei(ea)&&(ea=null),ta!==null&&Ei(ta)&&(ta=null),na!==null&&Ei(na)&&(na=null),lo.forEach(mp),oo.forEach(mp)}function wi(e,t){e.blockedOn===t&&(e.blockedOn=null,Kc||(Kc=!0,c.unstable_scheduleCallback(c.unstable_NormalPriority,Sy)))}var Ai=null;function yp(e){Ai!==e&&(Ai=e,c.unstable_scheduleCallback(c.unstable_NormalPriority,function(){Ai===e&&(Ai=null);for(var t=0;t<e.length;t+=3){var a=e[t],l=e[t+1],o=e[t+2];if(typeof l!="function"){if(Vc(l||a)===null)continue;break}var i=Oa(a);i!==null&&(e.splice(t,3),t-=3,Qs(i,{pending:!0,data:o,method:a.method,action:l},l,o))}}))}function pl(e){function t(_){return wi(_,e)}ea!==null&&wi(ea,e),ta!==null&&wi(ta,e),na!==null&&wi(na,e),lo.forEach(t),oo.forEach(t);for(var a=0;a<aa.length;a++){var l=aa[a];l.blockedOn===e&&(l.blockedOn=null)}for(;0<aa.length&&(a=aa[0],a.blockedOn===null);)hp(a),a.blockedOn===null&&aa.shift();if(a=(e.ownerDocument||e).$$reactFormReplay,a!=null)for(l=0;l<a.length;l+=3){var o=a[l],i=a[l+1],r=o[At]||null;if(typeof i=="function")r||yp(a);else if(r){var f=null;if(i&&i.hasAttribute("formAction")){if(o=i,r=i[At]||null)f=r.formAction;else if(Vc(o)!==null)continue}else f=r.action;typeof f=="function"?a[l+1]=f:(a.splice(l,3),l-=3),yp(a)}}}function gp(){function e(i){i.canIntercept&&i.info==="react-transition"&&i.intercept({handler:function(){return new Promise(function(r){return o=r})},focusReset:"manual",scroll:"manual"})}function t(){o!==null&&(o(),o=null),l||setTimeout(a,20)}function a(){if(!l&&!navigation.transition){var i=navigation.currentEntry;i&&i.url!=null&&navigation.navigate(i.url,{state:i.getState(),info:"react-transition",history:"replace"})}}if(typeof navigation=="object"){var l=!1,o=null;return navigation.addEventListener("navigate",e),navigation.addEventListener("navigatesuccess",t),navigation.addEventListener("navigateerror",t),setTimeout(a,100),function(){l=!0,navigation.removeEventListener("navigate",e),navigation.removeEventListener("navigatesuccess",t),navigation.removeEventListener("navigateerror",t),o!==null&&(o(),o=null)}}}function Ic(e){this._internalRoot=e}Ni.prototype.render=Ic.prototype.render=function(e){var t=this._internalRoot;if(t===null)throw Error(u(409));var a=t.current,l=Lt();cp(a,l,e,t,null,null)},Ni.prototype.unmount=Ic.prototype.unmount=function(){var e=this._internalRoot;if(e!==null){this._internalRoot=null;var t=e.containerInfo;cp(e.current,2,null,e,null,null),si(),t[Ma]=null}};function Ni(e){this._internalRoot=e}Ni.prototype.unstable_scheduleHydration=function(e){if(e){var t=Ar();e={blockedOn:null,target:e,priority:t};for(var a=0;a<aa.length&&t!==0&&t<aa[a].priority;a++);aa.splice(a,0,e),a===0&&hp(e)}};var vp=n.version;if(vp!=="19.2.4")throw Error(u(527,vp,"19.2.4"));D.findDOMNode=function(e){var t=e._reactInternals;if(t===void 0)throw typeof e.render=="function"?Error(u(188)):(e=Object.keys(e).join(","),Error(u(268,e)));return e=g(t),e=e!==null?A(e):null,e=e===null?null:e.stateNode,e};var xy={bundleType:0,version:"19.2.4",rendererPackageName:"react-dom",currentDispatcherRef:M,reconcilerVersion:"19.2.4"};if(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__<"u"){var Ci=__REACT_DEVTOOLS_GLOBAL_HOOK__;if(!Ci.isDisabled&&Ci.supportsFiber)try{ra=Ci.inject(xy),St=Ci}catch{}}return so.createRoot=function(e,t){if(!d(e))throw Error(u(299));var a=!1,l="",o=Ed,i=wd,r=Ad;return t!=null&&(t.unstable_strictMode===!0&&(a=!0),t.identifierPrefix!==void 0&&(l=t.identifierPrefix),t.onUncaughtError!==void 0&&(o=t.onUncaughtError),t.onCaughtError!==void 0&&(i=t.onCaughtError),t.onRecoverableError!==void 0&&(r=t.onRecoverableError)),t=ip(e,1,!1,null,null,a,l,null,o,i,r,gp),e[Ma]=t.current,Mc(e),new Ic(t)},so.hydrateRoot=function(e,t,a){if(!d(e))throw Error(u(299));var l=!1,o="",i=Ed,r=wd,f=Ad,_=null;return a!=null&&(a.unstable_strictMode===!0&&(l=!0),a.identifierPrefix!==void 0&&(o=a.identifierPrefix),a.onUncaughtError!==void 0&&(i=a.onUncaughtError),a.onCaughtError!==void 0&&(r=a.onCaughtError),a.onRecoverableError!==void 0&&(f=a.onRecoverableError),a.formState!==void 0&&(_=a.formState)),t=ip(e,1,!0,t,a??null,l,o,_,i,r,f,gp),t.context=sp(null),a=t.current,l=Lt(),l=Gi(l),o=Yn(l),o.callback=null,Xn(a,o,l),a=l,t.current.lanes=a,gl(t,a),rn(t),e[Ma]=t.current,Mc(e),new Ni(t)},so.version="19.2.4",so}var Ep;function Qy(){if(Ep)return $c.exports;Ep=1;function c(){if(!(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__>"u"||typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE!="function"))try{__REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE(c)}catch(n){console.error(n)}}return c(),$c.exports=Zy(),$c.exports}var Vy=Qy(),Pc={exports:{}},er={};/**
 * @license React
 * react-compiler-runtime.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var wp;function Ky(){if(wp)return er;wp=1;var c=Qp().__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE;return er.c=function(n){return c.H.useMemoCache(n)},er}var Ap;function Iy(){return Ap||(Ap=1,Pc.exports=Ky()),Pc.exports}var Se=Iy();const $y="_wrapper_h677m_2",Wy="_header_h677m_10",Fy="_headerActions_h677m_21",Py="_title_h677m_27",eg="_panelGroup_h677m_36",tg="_clipboardToggle_h677m_43",ng="_helpToggle_h677m_66",ag="_helpButtonWrapper_h677m_93",lg="_helpTogglePulsing_h677m_97",og="_helpHint_h677m_112",ig="_helpHintFading_h677m_139",sg="_helpHintKbd_h677m_144",cg="_resizeHandle_h677m_153",zt={wrapper:$y,header:Wy,headerActions:Fy,title:Py,panelGroup:eg,clipboardToggle:tg,helpToggle:ng,helpButtonWrapper:ag,helpTogglePulsing:lg,helpHint:og,helpHintFading:ig,helpHintKbd:sg,resizeHandle:cg},rg=c=>{try{return!new DOMParser().parseFromString(c.trim(),"text/xml").querySelector("parsererror")}catch{return!1}},ug=c=>{try{return JSON.parse(c),!0}catch{return!1}},dg=c=>c.trim()?ug(c)?{valid:!0,error:null,type:"json"}:rg(c)?{valid:!0,error:null,type:"xml"}:{valid:!1,error:"Invalid JSON/XML format",type:null}:{valid:!0,error:null,type:null},cr=c=>{try{const n=JSON.parse(c);return JSON.stringify(n,null,2)}catch{return c}},fg=()=>{const c=Se.c(8);let n;c[0]===Symbol.for("react.memo_cache_sentinel")?(n=[],c[0]=n):n=c[0];const[s,u]=x.useState(n),d=x.useRef(0);let p;c[1]===Symbol.for("react.memo_cache_sentinel")?(p=new Set,c[1]=p):p=c[1];const b=x.useRef(p);let y,m;c[2]===Symbol.for("react.memo_cache_sentinel")?(y=()=>()=>{b.current.forEach(clearTimeout)},m=[],c[2]=y,c[3]=m):(y=c[2],m=c[3]),x.useEffect(y,m);let g;c[4]===Symbol.for("react.memo_cache_sentinel")?(g=(w,C)=>{const S=C===void 0?"info":C,O=d.current=d.current+1;u(H=>[...H,{id:O,message:w,type:S}]);const k=setTimeout(()=>{b.current.delete(k),u(H=>H.filter(L=>L.id!==O))},3e3);b.current.add(k)},c[4]=g):g=c[4];const A=g;let v;c[5]===Symbol.for("react.memo_cache_sentinel")?(v=w=>{u(C=>C.filter(S=>S.id!==w))},c[5]=v):v=c[5];const T=v;let N;return c[6]!==s?(N={toasts:s,addToast:A,removeToast:T},c[6]=s,c[7]=N):N=c[7],N},oa=(c,n)=>{const s=x.useCallback(()=>{try{const p=window.localStorage.getItem(c);return p?JSON.parse(p):n}catch{return n}},[c]),[u,d]=x.useState(s);return x.useEffect(()=>{d(s())},[c]),x.useEffect(()=>{try{window.localStorage.setItem(c,JSON.stringify(u))}catch(p){console.error(`Error setting localStorage key "${c}":`,p)}},[c,u]),x.useEffect(()=>{const p=b=>{(b.key===c||b.key===null)&&d(s())};return window.addEventListener("storage",p),()=>window.removeEventListener("storage",p)},[c,s]),x.useEffect(()=>{const p=()=>d(s());return window.addEventListener("focus",p),document.addEventListener("visibilitychange",p),()=>{window.removeEventListener("focus",p),document.removeEventListener("visibilitychange",p)}},[s]),[u,d]},pg=200,Np=50,hg=8,mg=2e4,On=[{path:"/json-path",label:"JSON-Path",title:"JSON-Path Playground",wsPath:"/ws/json/path",storageKeyPayload:"jsonpath-last-payload",storageKeyHistory:"jsonpath-command-history",storageKeyTab:"jsonpath-right-tab",supportsUpload:!0,tabs:["payload","graph","graph-data"]},{path:"/",label:"Minigraph",title:"Minigraph Playground",wsPath:"/ws/graph/playground",storageKeyPayload:"minigraph-last-payload",storageKeyHistory:"minigraph-command-history",storageKeyTab:"minigraph-right-tab",storageKeySavedGraphs:"minigraph-saved-graphs",storageKeyHelpTopic:"minigraph-help-topic",supportsClipboard:!0,supportsHelp:!0,tabs:["graph","graph-data"]}],ji={json_simple:JSON.stringify({name:"John Doe",age:30,city:"New York"},null,2),json_nested:JSON.stringify({user:{name:"Jane Smith",profile:{email:"jane@example.com",address:{city:"San Francisco",country:"USA"}}}},null,2),json_array:JSON.stringify([{id:1,name:"Item 1",status:"active"},{id:2,name:"Item 2",status:"pending"},{id:3,name:"Item 3",status:"inactive"}],null,2),xml_simple:`<?xml version="1.0" encoding="UTF-8"?>
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
</items>`};function Ip(c){return`ws://${window.location.host}${c}`}function tr(c,n,s,u){const d=c[n]??{phase:"idle",messages:[]},p=[...d.messages,{id:s,raw:u}];return p.length>pg&&p.shift(),{...c,[n]:{...d,messages:p}}}function yg(c,n){const s=c[n.path]??{phase:"idle",messages:[]};switch(n.type){case"CONNECTING":return{...c,[n.path]:{...s,phase:"connecting"}};case"CONNECTED":return tr({...c,[n.path]:{...s,phase:"connected"}},n.path,n.id,n.msg);case"MESSAGE_RECEIVED":return tr(c,n.path,n.id,n.msg);case"DISCONNECTED":return tr({...c,[n.path]:{...s,phase:"idle"}},n.path,n.id,n.msg);case"CONNECT_ERROR":return{...c,[n.path]:{...s,phase:"idle"}};case"CLEAR_MESSAGES":return{...c,[n.path]:{...s,messages:[]}};default:return c}}const $p=x.createContext(null);function gg({children:c}){const[n,s]=x.useReducer(yg,{}),u=x.useRef({}),d=x.useRef({}),p=x.useRef({});x.useEffect(()=>()=>{Object.entries(u.current).forEach(([B,J])=>{J==null||J.close();const K=d.current[B];K&&clearInterval(K)})},[]);const b=B=>Ip(B),y=B=>(p.current[B]=(p.current[B]??0)+1,p.current[B]),m=()=>{const B=new Date().toString(),J=B.indexOf("GMT");return J>0?B.substring(0,J).trim():B},g=(B,J)=>JSON.stringify({type:B,message:J,time:m()}),A=B=>{try{const J=JSON.parse(B);if(J!==null&&typeof J=="object"){const K=J.type;return K==="ping"||K==="pong"}}catch{}return!1},v=x.useCallback((B,J)=>{if(!window.WebSocket){J==null||J("WebSocket not supported by your browser","error");return}const K=u.current[B];if(K&&(K.readyState===WebSocket.OPEN||K.readyState===WebSocket.CONNECTING)){J==null||J("Already connected","error");return}s({type:"CONNECTING",path:B});const I=new WebSocket(b(B));u.current[B]=I,I.onopen=()=>{s({type:"CONNECTED",path:B,id:y(B),msg:g("info","connected")}),J==null||J("Connected to WebSocket","success"),I.send(JSON.stringify({type:"welcome"})),d.current[B]=setInterval(()=>{I.readyState===WebSocket.OPEN&&I.send(g("ping","keep alive"))},mg)},I.onmessage=$=>{A($.data)||s({type:"MESSAGE_RECEIVED",path:B,id:y(B),msg:$.data})},I.onerror=()=>{s({type:"CONNECT_ERROR",path:B})},I.onclose=$=>{const ne=d.current[B];ne&&(clearInterval(ne),d.current[B]=null),s({type:"DISCONNECTED",path:B,id:y(B),msg:g("info",`disconnected - (${$.code}) ${$.reason}`)}),J==null||J("Disconnected from WebSocket","info"),u.current[B]===I&&(u.current[B]=null)}},[]),T=x.useCallback(B=>{const J=u.current[B];J?J.close():s({type:"MESSAGE_RECEIVED",path:B,id:y(B),msg:g("error","already disconnected")})},[]);x.useEffect(()=>(On.forEach(B=>{v(B.wsPath)}),()=>{On.forEach(B=>{const J=u.current[B.wsPath];J&&J.close()})}),[]);const N=x.useCallback((B,J)=>{const K=u.current[B];return K&&K.readyState===WebSocket.OPEN?(K.send(J),!0):!1},[]),w=x.useCallback((B,J)=>{s({type:"MESSAGE_RECEIVED",path:B,id:y(B),msg:J})},[]),C=x.useCallback(B=>{s({type:"CLEAR_MESSAGES",path:B})},[]),[S,O]=x.useState({}),k=x.useCallback((B,J)=>{O(K=>{if(J===null){const I={...K};return delete I[B],I}return{...K,[B]:J}})},[]),H=x.useCallback(B=>S[B]??null,[S]),L=x.useCallback(B=>{const J=S[B]??null;return J!==null&&O(K=>{const I={...K};return delete I[B],I}),J},[S]),Y=x.useCallback(B=>n[B]??{phase:"idle",messages:[]},[n]),q=x.useMemo(()=>({getSlot:Y,connect:v,disconnect:T,send:N,appendMessage:w,clearMessages:C,setPendingPayload:k,peekPendingPayload:H,takePendingPayload:L}),[Y,v,T,N,w,C,k,H,L]);return h.jsx($p.Provider,{value:q,children:c})}function br(){const c=x.useContext($p);if(!c)throw new Error("useWebSocketContext must be used inside <WebSocketProvider>");return c}const vg=c=>{try{const n=JSON.parse(c);return{type:n.type||"info",message:n.message||c,time:n.time,raw:c}}catch{return{type:"raw",message:c,time:null,raw:c}}},bg=c=>({info:"ℹ️",error:"❌",ping:"🔄",welcome:"👋",raw:""})[c]??"•",fo=c=>{try{const n=JSON.parse(c);if(typeof n=="object"&&n!==null)return{isJSON:!0,data:n}}catch{}return{isJSON:!1,data:null}};function _g(c){if(!c.includes("Graph exported to "))return null;const n=Sr(c);if(!n)return null;const s=n.split("/")[4];return s?{graphName:s,apiPath:n}:null}function Sg(c){return c.includes("Invalid filename")?{reason:"invalid-name"}:c.includes("Expect root node name")?{reason:"root-name-conflict"}:null}function _r(c){const n=fo(c);return n.isJSON?(typeof n.data.type=="string",!1):!0}function Sr(c){const n=c.match(/\/api\/graph\/model\/([^\s'"]+)/);return n?n[0]:null}function Wp(c){return _r(c)?Sr(c)!==null:!1}function Fp(c){const n=c.match(/\/api\/json\/content\/([\w-]+)/);return n?n[0]:null}function xg(c){const n=c.match(/Large payload \((\d+)\)\s*->\s*GET\s+(\/api\/inspect\/[^\s]+)/i);if(!n)return null;const s=parseInt(n[1],10),u=n[2],p=`${u.split("/").filter(Boolean).pop()??"payload"}.json`;return{apiPath:u,byteSize:s,filename:p}}function Tg(c){const n=c.match(/You may upload .*?->\s*POST\s+(\/api\/mock\/[\w-]+)/i);return n?n[1]:null}function Eg(c){if(!c.startsWith("> "))return!1;const n=c.slice(2).trim().toLowerCase();return n==="help"||n.startsWith("help ")?!0:n.startsWith("describe ")?!n.slice(9).trim().startsWith("graph"):!1}function wg(c){if(!c.startsWith("> ")||!c.slice(2).trimStart().toLowerCase().startsWith("import graph from "))return null;const s=c.slice(2).trimStart().slice(18).trim();return s.length>0?s:null}function Ag(c){if(!_r(c)||c.startsWith("> ")||Wp(c))return null;const n=c.toLowerCase();return n.includes("graph model imported as draft")?"import-graph":n.includes(" -> ")&&n.includes("removed")||n.startsWith("node ")&&(n.includes(" created")||n.includes(" updated")||n.includes(" deleted")||n.includes(" connected to ")||n.includes(" imported from ")||n.includes(" overwritten by node from "))?"node-mutation":null}const Ng={command:"",historyIndex:-1,draftCommand:""};function Cg(c,n){switch(n.type){case"SET_COMMAND":return{...c,command:n.value,historyIndex:-1,draftCommand:""};case"CLEAR_COMMAND":return{...c,command:"",historyIndex:-1,draftCommand:""};case"SET_HISTORY_INDEX":return{...c,historyIndex:n.index,command:n.command};case"ENTER_HISTORY":return{...c,historyIndex:0,command:n.command,draftCommand:c.command};case"EXIT_HISTORY":return{...c,historyIndex:-1,command:c.draftCommand,draftCommand:""};default:return c}}function jg(c){const n=Se.c(77),{wsPath:s,storageKeyHistory:u,payload:d,addToast:p,bus:b,handleLocalCommand:y}=c,m=br();let g;n[0]!==m||n[1]!==s?(g=m.getSlot(s),n[0]=m,n[1]=s,n[2]=g):g=n[2];const{phase:A,messages:v}=g,T=A==="connected",N=A==="connecting",[w,C]=x.useReducer(Cg,Ng),{command:S,historyIndex:O}=w;let k;n[3]===Symbol.for("react.memo_cache_sentinel")?(k=[],n[3]=k):k=n[3];const[H,L]=oa(u,k),Y=x.useRef(null),q=x.useRef(!1);let B;n[4]===Symbol.for("react.memo_cache_sentinel")?(B=()=>{Y.current&&(Y.current.scrollTop=Y.current.scrollHeight)},n[4]=B):B=n[4];let J;n[5]!==v?(J=[v],n[5]=v,n[6]=J):J=n[6],x.useEffect(B,J);let K;n[7]!==p||n[8]!==m||n[9]!==s?(K=()=>{m.connect(s,p)},n[7]=p,n[8]=m,n[9]=s,n[10]=K):K=n[10];const I=K;let $;n[11]!==m||n[12]!==s?($=()=>{m.disconnect(s)},n[11]=m,n[12]=s,n[13]=$):$=n[13];const ne=$;let ue;n[14]!==S||n[15]!==m||n[16]!==y||n[17]!==H||n[18]!==d||n[19]!==A||n[20]!==L||n[21]!==s?(ue=()=>{if(A!=="connected")return;const W=S.trim();if(W.length!==0){if((y==null?void 0:y(W))===!0){H[0]!==W&&L(ye=>[W,...ye].slice(0,Np)),m.appendMessage(s,"> "+W),C({type:"CLEAR_COMMAND"});return}m.send(s,W),H[0]!==W&&L(ye=>[W,...ye].slice(0,Np)),W==="load"&&(d.length===0?m.appendMessage(s,"ERROR: please paste JSON/XML payload in input text area"):m.send(s,d)),C({type:"CLEAR_COMMAND"})}},n[14]=S,n[15]=m,n[16]=y,n[17]=H,n[18]=d,n[19]=A,n[20]=L,n[21]=s,n[22]=ue):ue=n[22];const re=ue;let ie;n[23]!==H||n[24]!==O?(ie=W=>{if(W.key==="ArrowUp"){if(W.preventDefault(),H.length===0)return;if(O===-1)C({type:"ENTER_HISTORY",command:H[0]});else if(O<H.length-1){const ye=O+1;C({type:"SET_HISTORY_INDEX",index:ye,command:H[ye]})}}else if(W.key==="ArrowDown")if(W.preventDefault(),O<=0)O===0&&C({type:"EXIT_HISTORY"});else{const ye=O-1;C({type:"SET_HISTORY_INDEX",index:ye,command:H[ye]})}},n[23]=H,n[24]=O,n[25]=ie):ie=n[25];const M=ie;let D,V;n[26]!==p||n[27]!==b||n[28]!==m||n[29]!==d||n[30]!==s?(V=()=>{if(b)return b.on("upload.contentPath",W=>{if(!q.current)return;if(q.current=!1,d.length===0){m.appendMessage(s,"ERROR: please paste JSON/XML payload in the input text area");return}let ye;try{ye=JSON.stringify(JSON.parse(d))}catch{m.appendMessage(s,"ERROR: payload is not valid JSON — cannot upload");return}fetch(W.uploadPath,{method:"POST",headers:{"Content-Type":"application/json"},body:ye}).then(Te=>{if(!Te.ok)throw new Error(`HTTP ${Te.status}`);p("Payload uploaded successfully","success")}).catch(Te=>{m.appendMessage(s,`ERROR: upload failed — ${Te.message}`),p(`Upload failed: ${Te.message}`,"error")})})},D=[b,d,s,m,p],n[26]=p,n[27]=b,n[28]=m,n[29]=d,n[30]=s,n[31]=D,n[32]=V):(D=n[31],V=n[32]),x.useEffect(V,D);let F,ee;n[33]!==p||n[34]!==b||n[35]!==m||n[36]!==v||n[37]!==d||n[38]!==s?(F=()=>{if(b||!q.current||v.length===0)return;const W=v[v.length-1].raw,ye=Fp(W);if(!ye)return;if(q.current=!1,d.length===0){m.appendMessage(s,"ERROR: please paste JSON/XML payload in the input text area");return}let Te;try{Te=JSON.stringify(JSON.parse(d))}catch{m.appendMessage(s,"ERROR: payload is not valid JSON — cannot upload");return}fetch(ye,{method:"POST",headers:{"Content-Type":"application/json"},body:Te}).then(he=>{if(!he.ok)throw new Error(`HTTP ${he.status}`);p("Payload uploaded successfully","success")}).catch(he=>{m.appendMessage(s,`ERROR: upload failed — ${he.message}`),p(`Upload failed: ${he.message}`,"error")})},ee=[b,v,d,s,m,p],n[33]=p,n[34]=b,n[35]=m,n[36]=v,n[37]=d,n[38]=s,n[39]=F,n[40]=ee):(F=n[39],ee=n[40]),x.useEffect(F,ee);let ce;n[41]!==p||n[42]!==m||n[43]!==d||n[44]!==A||n[45]!==s?(ce=()=>{if(A==="connected"){if(d.length===0){p("Nothing to upload — paste a JSON payload first","error");return}q.current=!0,m.send(s,"upload")}},n[41]=p,n[42]=m,n[43]=d,n[44]=A,n[45]=s,n[46]=ce):ce=n[46];const te=ce;let P;n[47]!==m||n[48]!==A||n[49]!==s?(P=W=>{A==="connected"&&m.send(s,W)},n[47]=m,n[48]=A,n[49]=s,n[50]=P):P=n[50];const me=P;let _e;n[51]!==p||n[52]!==v?(_e=()=>{navigator.clipboard.writeText(v.map(Mg).join(`
`)),p("Console copied to clipboard!","success")},n[51]=p,n[52]=v,n[53]=_e):_e=n[53];const xe=_e;let Be;n[54]!==p||n[55]!==m||n[56]!==s?(Be=()=>{m.clearMessages(s),p("Console cleared","info")},n[54]=p,n[55]=m,n[56]=s,n[57]=Be):Be=n[57];const qe=Be;let Me;n[58]!==m||n[59]!==s?(Me=W=>{m.appendMessage(s,W)},n[58]=m,n[59]=s,n[60]=Me):Me=n[60];const le=Me;let de;n[61]===Symbol.for("react.memo_cache_sentinel")?(de=W=>C({type:"SET_COMMAND",value:W}),n[61]=de):de=n[61];const pe=de;let Re;return n[62]!==le||n[63]!==qe||n[64]!==S||n[65]!==I||n[66]!==T||n[67]!==N||n[68]!==xe||n[69]!==ne||n[70]!==M||n[71]!==H||n[72]!==v||n[73]!==re||n[74]!==me||n[75]!==te?(Re={connected:T,connecting:N,messages:v,command:S,setCommand:pe,connect:I,disconnect:ne,sendCommand:re,handleKeyDown:M,consoleRef:Y,copyMessages:xe,clearMessages:qe,uploadPayload:te,sendRawText:me,appendMessage:le,history:H},n[62]=le,n[63]=qe,n[64]=S,n[65]=I,n[66]=T,n[67]=N,n[68]=xe,n[69]=ne,n[70]=M,n[71]=H,n[72]=v,n[73]=re,n[74]=me,n[75]=te,n[76]=Re):Re=n[76],Re}function Mg(c){return c.raw}function Dg(c){const n=Se.c(5);let s;n[0]!==c?(s=()=>window.matchMedia(c).matches,n[0]=c,n[1]=s):s=n[1];const[u,d]=x.useState(s);let p,b;return n[2]!==c?(p=()=>{const y=window.matchMedia(c),m=g=>d(g.matches);return y.addEventListener("change",m),()=>y.removeEventListener("change",m)},b=[c],n[2]=c,n[3]=p,n[4]=b):(p=n[3],b=n[4]),x.useEffect(p,b),u}function Cp(c){return typeof c!="object"||c===null?!1:Array.isArray(c.nodes)}function nr(c,n,s){const u=n.includes(s)?s:n[0]??"graph";return typeof c=="string"&&n.includes(c)?c:u}function Og(c,n,s,u,d){const[p,b]=x.useState(null),[y,m]=oa(d,s),g=nr(y,u,s),[A,v]=x.useState(!1),T=x.useCallback(S=>{m(O=>{const k=nr(O,u,s),H=typeof S=="function"?S(k):S;return nr(H,u,s)})},[m,u,s]);x.useEffect(()=>{y!==g&&m(g)},[y,g,m]);const N=x.useRef(c);x.useEffect(()=>{N.current=c},[c]);const w=x.useRef(null);x.useEffect(()=>{if(!c)return;const S=new AbortController;return b(null),fetch(c,{signal:S.signal}).then(O=>{if(!O.ok)throw new Error(`HTTP ${O.status}`);return O.json()}).then(O=>{Cp(O)&&(b(O),T("graph"))}).catch(O=>{O.name!=="AbortError"&&n(`Graph fetch failed: ${O.message}`,"error")}),()=>{S.abort()}},[c,n]);const C=x.useCallback(()=>{var k;const S=N.current;if(!S)return;(k=w.current)==null||k.abort();const O=new AbortController;w.current=O,v(!0),fetch(S,{signal:O.signal}).then(H=>{if(!H.ok)throw new Error(`HTTP ${H.status}`);return H.json()}).then(H=>{Cp(H)&&b(H),v(!1)}).catch(H=>{H.name!=="AbortError"&&(n(`Graph refresh failed: ${H.message}`,"error"),v(!1))})},[]);return x.useEffect(()=>()=>{var S;(S=w.current)==null||S.abort()},[]),{graphData:p,setGraphData:b,rightTab:g,setRightTab:T,isRefreshing:A,refetchGraph:C}}function zg(c){const n=Se.c(22),{bus:s,pinnedGraphPath:u,setPinnedGraphPath:d,connected:p,sendRawText:b,addToast:y}=c,m=x.useRef(null),g=x.useRef(!1),A=x.useRef(u),v=x.useRef(p),T=x.useRef(b);let N,w;n[0]!==u?(N=()=>{A.current=u},w=[u],n[0]=u,n[1]=N,n[2]=w):(N=n[1],w=n[2]),x.useEffect(N,w);let C,S;n[3]!==p?(C=()=>{v.current=p},S=[p],n[3]=p,n[4]=C,n[5]=S):(C=n[4],S=n[5]),x.useEffect(C,S);let O,k;n[6]!==b?(O=()=>{T.current=b},k=[b],n[6]=b,n[7]=O,n[8]=k):(O=n[7],k=n[8]),x.useEffect(O,k);let H,L;n[9]!==p?(H=()=>{p||(g.current=!1,m.current!==null&&(clearTimeout(m.current),m.current=null))},L=[p],n[9]=p,n[10]=H,n[11]=L):(H=n[10],L=n[11]),x.useEffect(H,L);let Y,q;n[12]!==s||n[13]!==d?(q=()=>s.on("graph.link",$=>{g.current&&(g.current=!1,d($.apiPath))}),Y=[s,d],n[12]=s,n[13]=d,n[14]=Y,n[15]=q):(Y=n[14],q=n[15]),x.useEffect(q,Y);let B,J;n[16]!==y||n[17]!==s?(B=()=>s.on("graph.mutation",$=>{if(v.current){if($.mutationType==="import-graph"){m.current!==null&&(clearTimeout(m.current),m.current=null),g.current=!0,T.current("describe graph"),y("Graph imported — refreshing view…","info");return}m.current!==null&&clearTimeout(m.current),m.current=setTimeout(()=>{m.current=null,v.current&&(g.current=!0,T.current("describe graph"),y(A.current!==null?"Graph updated — refreshing…":"Graph updated — opening Graph tab…","info"))},300)}}),J=[s,y],n[16]=y,n[17]=s,n[18]=B,n[19]=J):(B=n[18],J=n[19]),x.useEffect(B,J);let K,I;n[20]===Symbol.for("react.memo_cache_sentinel")?(K=()=>()=>{m.current!==null&&clearTimeout(m.current)},I=[],n[20]=K,n[21]=I):(K=n[20],I=n[21]),x.useEffect(K,I)}const Rg=`Connect two nodes together
--------------------------
1. Each connection is directional. Connect A to B is different from B to A.
2. A node must connect to one or more nodes. When a graph has orphan nodes, you cannot export the graph for deployment.

Syntax
------
\`\`\`
connect {node-A} to {node-B} with {relation}
\`\`\`
`,kg=`Create a new node
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
`,Bg=`Data Dictionary
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
`,Ug=`Delete a node, a connection or clear cache
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
`,Hg="Describe graph, node, connection or skill\n-----------------------------------------\n\nSyntax\n------\nShow the structure of the current graph model\n```\ndescribe graph\n```\n\nPrint the structure of a node\n```\ndescribe node {name}\n```\n\nConfirm if there is a connection between node-A and node-B\n```\ndescribe connection {node-A} and {node-B}\n```\n\nSkill description of a specific composable function serving the skill\n```\ndescribe skill {skill.route.name}\n```\n",Gg=`Edit a node
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
`,Lg=`Execute a node with a skill
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
`,qg=`Export a graph model
--------------------
1. This command exports a graph as a model in JSON format for deployment
2. The name does not require the ".json" extension

Syntax
------
\`\`\`
export graph as {name}
\`\`\`
`,Yg=`Skill: Graph API Fetcher
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
`,Xg=`Skill: Graph Data Mapper
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
`,Jg=`Skill: Graph Extension
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
`,Zg=`Skill: Graph Island
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
`,Qg=`Skill: Graph Join
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
`,Vg=`Skill: Graph JS
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
`,Kg=`Skill: Graph Math
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
`,Ig=`Import a graph model
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
`,$g=`Inspect state machine
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
`,Wg=`Instantiate from a Graph Model
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
`,Fg=`List nodes or connections
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
`,Pg=`Run a graph instance
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
`,e1=`Display nodes that have been 'seen'
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
`,t1=`Tutorial 1
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

Well done. Let's move on to "Tutorial 2".
`,n1=`Tutorial 2
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

In this session, you have completed the following exercise:

1. deploy the graph model 'tutorial-1' and invoke the API that executes the graph model as an instance
2. enhance the graph model from a simple 'hello world' application to an echo program
3. perform a dry-run with mock input to test the response
4. export the updated graph model as 'tutorial-2'
5. deploy 'tutorial-2' graph model
6. test the 'tutorial-2' graph model using a HTTP-POST command with some input payload
`,a1=`Tutorial 3
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
`,l1=`Tutorial 4
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
update node less-than
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
`,o1=`Tutorial 5
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
`,i1=`Tutorial 6
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
`,s1=`Tutorial 7
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
\`\`\`

\`mapping[]\` tells the system to create a data mapping statement in "append mode"
so that the statements will be evaluated in the order that they are provided.

Each data mapping statement has a left-hand-side and right-hand-side separated by the "map to \`->\`" indicator.

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
`,c1=`Update a node
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
`,r1=`Upload mock data to current graph instance
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
`,u1=`MiniGraph
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
`,d1=Object.assign({"../../../src/main/resources/help/help connect.md":Rg,"../../../src/main/resources/help/help create.md":kg,"../../../src/main/resources/help/help data-dictionary.md":Bg,"../../../src/main/resources/help/help delete.md":Ug,"../../../src/main/resources/help/help describe.md":Hg,"../../../src/main/resources/help/help edit.md":Gg,"../../../src/main/resources/help/help execute.md":Lg,"../../../src/main/resources/help/help export.md":qg,"../../../src/main/resources/help/help graph-api-fetcher.md":Yg,"../../../src/main/resources/help/help graph-data-mapper.md":Xg,"../../../src/main/resources/help/help graph-extension.md":Jg,"../../../src/main/resources/help/help graph-island.md":Zg,"../../../src/main/resources/help/help graph-join.md":Qg,"../../../src/main/resources/help/help graph-js.md":Vg,"../../../src/main/resources/help/help graph-math.md":Kg,"../../../src/main/resources/help/help import.md":Ig,"../../../src/main/resources/help/help inspect.md":$g,"../../../src/main/resources/help/help instantiate.md":Wg,"../../../src/main/resources/help/help list.md":Fg,"../../../src/main/resources/help/help run.md":Pg,"../../../src/main/resources/help/help seen.md":e1,"../../../src/main/resources/help/help tutorial 1.md":t1,"../../../src/main/resources/help/help tutorial 2.md":n1,"../../../src/main/resources/help/help tutorial 3.md":a1,"../../../src/main/resources/help/help tutorial 4.md":l1,"../../../src/main/resources/help/help tutorial 5.md":o1,"../../../src/main/resources/help/help tutorial 6.md":i1,"../../../src/main/resources/help/help tutorial 7.md":s1,"../../../src/main/resources/help/help update.md":c1,"../../../src/main/resources/help/help upload.md":r1,"../../../src/main/resources/help/help.md":u1});function f1(c){const n=c.split("/");return(n[n.length-1]??c).replace(/\.md$/,"")}const Pp=Object.fromEntries(Object.entries(d1).map(([c,n])=>[f1(c),n]));function Oi(c){const n=c===""?"help":`help ${c}`;return Pp[n]??null}const p1=Object.keys(Pp).filter(c=>c!=="help").map(c=>c.replace(/^help\s+/,"")).sort(),eh=[{id:"overview",label:"Overview"},{id:"graph-model",label:"Graph Model"},{id:"graph-skills",label:"Graph Skills"},{id:"instance-model",label:"Instance Model"},{id:"tutorials",label:"Tutorials"}],h1=new Set(["execute","inspect","instantiate","run","seen","upload"]);function th(c){return c===""?"overview":c.startsWith("tutorial ")?"tutorials":c.startsWith("graph-")?"graph-skills":h1.has(c)?"instance-model":"graph-model"}function rr(c){return c==="overview"?[""]:p1.filter(n=>th(n)===c)}const hl=eh.flatMap(c=>rr(c.id));function nh(c){return c.replace(/^help\s*/i,"").trim().toLowerCase()}function m1(c){const n=Se.c(6),{bus:s,setHelpTopic:u,onTabSwitch:d}=c,p=x.useRef(d);let b;n[0]!==d?(b=()=>{p.current=d},n[0]=d,n[1]=b):b=n[1],x.useEffect(b);let y,m;n[2]!==s||n[3]!==u?(y=()=>s.on("command.helpOrDescribe",g=>{if(!g.commandText.trim().toLowerCase().startsWith("help"))return;const v=nh(g.commandText);Oi(v)!==null&&(u(v),p.current())}),m=[s,u],n[2]=s,n[3]=u,n[4]=y,n[5]=m):(y=n[4],m=n[5]),x.useEffect(y,m)}function y1(c){const n=Se.c(12),{ctx:s,navigate:u,addToast:d,wsPath:p}=c;let b;n[0]===Symbol.for("react.memo_cache_sentinel")?(b=On.find(g1),n[0]=b):b=n[0];const y=b,m=x.useRef(null),g=y==null?void 0:y.wsPath;let A,v;n[1]!==d||n[2]!==s||n[3]!==u?(A=()=>{if(!g||!m.current)return;if(s.getSlot(g).phase==="connected"){const{wsPath:O,json:k}=m.current;m.current=null,s.setPendingPayload(O,k),u(y.path),d("JSON loaded into JSON-Path editor ✓","success")}},v=[g,s,u,d,y],n[1]=d,n[2]=s,n[3]=u,n[4]=A,n[5]=v):(A=n[4],v=n[5]),x.useEffect(A,v);let T;n[6]!==d||n[7]!==s||n[8]!==u?(T=S=>{if(!y)return;const O=s.getSlot(y.wsPath);O.phase==="connected"?(s.setPendingPayload(y.wsPath,S),u(y.path),d("JSON loaded into JSON-Path editor ✓","success")):O.phase==="connecting"?(m.current={wsPath:y.wsPath,json:S},d("Updated pending JSON transfer — latest payload will open when connected","info")):(m.current={wsPath:y.wsPath,json:S},s.connect(y.wsPath,d),d("Connecting to JSON-Path Playground…","info"))},n[6]=d,n[7]=s,n[8]=u,n[9]=T):T=n[9];const N=T,w=y&&p!==y.wsPath?N:void 0;let C;return n[10]!==w?(C={handleSendToJsonPath:w},n[10]=w,n[11]=C):C=n[11],C}function g1(c){return c.tabs.includes("payload")&&c.supportsUpload}function v1(c){const n=Se.c(7),{bus:s,onOpenModal:u,modalOpen:d}=c,p=x.useRef(!1);let b,y;n[0]!==d?(b=()=>{d||(p.current=!1)},y=[d],n[0]=d,n[1]=b,n[2]=y):(b=n[1],y=n[2]),x.useEffect(b,y);let m,g;n[3]!==s||n[4]!==u?(m=()=>s.on("upload.invitation",A=>{p.current||(p.current=!0,u(A.uploadPath))}),g=[s,u],n[3]=s,n[4]=u,n[5]=m,n[6]=g):(m=n[5],g=n[6]),x.useEffect(m,g)}function b1(c){const n=Se.c(17),{bus:s,addToast:u}=c,[d,p]=x.useState(null),b=x.useRef(null);let y;n[0]===Symbol.for("react.memo_cache_sentinel")?(y=new Set,n[0]=y):y=n[0];const[m,g]=x.useState(y);let A;n[1]===Symbol.for("react.memo_cache_sentinel")?(A=B=>{b.current=document.activeElement,p(B)},n[1]=A):A=n[1];const v=A;let T;n[2]===Symbol.for("react.memo_cache_sentinel")?(T=()=>{p(null),setTimeout(()=>{var B;return(B=b.current)==null?void 0:B.focus()},0)},n[2]=T):T=n[2];const N=T;let w;n[3]!==u||n[4]!==d?(w=B=>{g(J=>new Set([...J,d])),p(null),setTimeout(()=>{var J;return(J=b.current)==null?void 0:J.focus()},0),u("Mock data uploaded successfully ✓","success")},n[3]=u,n[4]=d,n[5]=w):w=n[5];const C=w;let S;n[6]!==u?(S=B=>{u(`Upload failed: ${B}`,"error")},n[6]=u,n[7]=S):S=n[7];const O=S;let k;n[8]===Symbol.for("react.memo_cache_sentinel")?(k=()=>{g(new Set)},n[8]=k):k=n[8];const H=k,L=d!==null;let Y;n[9]!==s||n[10]!==L?(Y={bus:s,onOpenModal:v,modalOpen:L},n[9]=s,n[10]=L,n[11]=Y):Y=n[11],v1(Y);let q;return n[12]!==O||n[13]!==C||n[14]!==d||n[15]!==m?(q={modalUploadPath:d,successfulUploadPaths:m,handleOpenUploadModal:v,handleCloseUploadModal:N,handleUploadSuccess:C,handleUploadError:O,resetSuccessfulPaths:H},n[12]=O,n[13]=C,n[14]=d,n[15]=m,n[16]=q):q=n[16],q}function _1(c){const n=Se.c(14),{bus:s,connected:u,appendMessage:d,addToast:p}=c,b=x.useRef(null),y=x.useRef(!1),m=x.useRef(d);let g,A;n[0]!==d?(g=()=>{m.current=d},A=[d],n[0]=d,n[1]=g,n[2]=A):(g=n[1],A=n[2]),x.useEffect(g,A);const v=x.useRef(p);let T,N;n[3]!==p?(T=()=>{v.current=p},N=[p],n[3]=p,n[4]=T,n[5]=N):(T=n[4],N=n[5]),x.useEffect(T,N);let w,C;n[6]!==u?(w=()=>{var L;u||((L=b.current)==null||L.abort(),b.current=null,y.current=!1)},C=[u],n[6]=u,n[7]=w,n[8]=C):(w=n[7],C=n[8]),x.useEffect(w,C);let S,O;n[9]===Symbol.for("react.memo_cache_sentinel")?(S=()=>()=>{var L;(L=b.current)==null||L.abort()},O=[],n[9]=S,n[10]=O):(S=n[9],O=n[10]),x.useEffect(S,O);let k,H;n[11]!==s?(H=()=>s.on("payload.large",L=>{var K;if(y.current)return;const{apiPath:Y,byteSize:q}=L;(K=b.current)==null||K.abort();const B=new AbortController;b.current=B;const J=(q/1048576).toFixed(2);v.current(`Fetching large payload (${J} MB)…`,"info"),y.current=!0,fetch(Y,{signal:B.signal}).then(S1).then(I=>{if(!I.trim())throw new Error("empty response body");let $=I;try{$=JSON.stringify(JSON.parse(I),null,2)}catch{}m.current($),y.current=!1,b.current=null}).catch(I=>{I.name!=="AbortError"&&(y.current=!1,b.current=null,m.current(`ERROR: payload fetch failed — ${I.message}`),v.current(`Payload fetch failed: ${I.message}`,"error"))})}),k=[s],n[11]=s,n[12]=k,n[13]=H):(k=n[12],H=n[13]),x.useEffect(H,k)}function S1(c){if(!c.ok)throw new Error(`HTTP ${c.status}`);return c.text()}function x1(c){const n=Se.c(14);let s;n[0]===Symbol.for("react.memo_cache_sentinel")?(s={},n[0]=s):s=n[0];const[u,d]=oa(c,s);let p;n[1]!==d?(p=w=>{d(C=>({...C,[w]:{name:w,savedAt:new Date().toISOString()}}))},n[1]=d,n[2]=p):p=n[2];const b=p;let y;n[3]!==d?(y=w=>{d(C=>{const S={...C};return delete S[w],S})},n[3]=d,n[4]=y):y=n[4];const m=y;let g;n[5]!==u?(g=w=>Object.prototype.hasOwnProperty.call(u,w),n[5]=u,n[6]=g):g=n[6];const A=g;let v;n[7]!==u?(v=Object.values(u).sort(T1),n[7]=u,n[8]=v):v=n[8];const T=v;let N;return n[9]!==m||n[10]!==A||n[11]!==b||n[12]!==T?(N={savedGraphs:T,saveGraph:b,deleteGraph:m,hasGraph:A},n[9]=m,n[10]=A,n[11]=b,n[12]=T,n[13]=N):N=n[13],N}function T1(c,n){return new Date(n.savedAt).getTime()-new Date(c.savedAt).getTime()}function E1(c,n){const s=Se.c(11),[u,d]=oa(c,1),p=x.useRef(!1),[b,y]=x.useState(null),[m,g]=x.useState(null);let A,v;s[0]!==n?(A=()=>n.on("command.importGraph",k=>{y(k.graphName),g(null)}),v=[n],s[0]=n,s[1]=A,s[2]=v):(A=s[1],v=s[2]),x.useEffect(A,v);let T;s[3]!==u?(T=k=>{g(k),k===`untitled-${u}`&&(p.current=!0)},s[3]=u,s[4]=T):T=s[4];const N=T;let w;s[5]!==d?(w=()=>{y(null),g(null),p.current&&d(w1),p.current=!1},s[5]=d,s[6]=w):w=s[6];const C=w,S=m??b??`untitled-${u}`;let O;return s[7]!==S||s[8]!==C||s[9]!==N?(O={defaultName:S,setLastSavedName:N,resetName:C},s[7]=S,s[8]=C,s[9]=N,s[10]=O):O=s[10],O}function w1(c){return c+1}function A1(c){const n=Se.c(27),{bus:s,connected:u,sendRawText:d,saveGraph:p,setLastSavedName:b,addToast:y}=c,m=x.useRef(null);let g;n[0]!==y||n[1]!==u||n[2]!==d?(g=q=>{if(!u){y("Save failed: connection required to export graph","error");return}const B=setTimeout(()=>{m.current!==null&&(m.current=null,y("Save failed: export confirmation timed out","error"))},1e4);m.current={graphName:q,timeoutId:B},d(`export graph as ${q}`)},n[0]=y,n[1]=u,n[2]=d,n[3]=g):g=n[3];const A=g;let v,T;n[4]!==y||n[5]!==s||n[6]!==p||n[7]!==b?(v=()=>s.on("graph.exported",q=>{if(m.current===null||q.graphName!==m.current.graphName)return;clearTimeout(m.current.timeoutId);const B=m.current.graphName;m.current=null,p(B),b(B),y(`Graph saved as "${B}"`,"success")}),T=[s,p,b,y],n[4]=y,n[5]=s,n[6]=p,n[7]=b,n[8]=v,n[9]=T):(v=n[8],T=n[9]),x.useEffect(v,T);let N,w;n[10]!==y||n[11]!==s?(N=()=>s.on("graph.export.failed",q=>{m.current!==null&&(clearTimeout(m.current.timeoutId),m.current=null,q.reason==="invalid-name"?y("Save failed: invalid filename (a–z, A–Z, 0–9, hyphen only)","error"):y("Save failed: root node name does not match existing graph","error"))}),w=[s,y],n[10]=y,n[11]=s,n[12]=N,n[13]=w):(N=n[12],w=n[13]),x.useEffect(N,w);let C,S;n[14]!==y||n[15]!==u?(C=()=>{!u&&m.current!==null&&(clearTimeout(m.current.timeoutId),m.current=null,y("Save failed: connection closed before export confirmation","error"))},S=[u,y],n[14]=y,n[15]=u,n[16]=C,n[17]=S):(C=n[16],S=n[17]),x.useEffect(C,S);let O,k;n[18]===Symbol.for("react.memo_cache_sentinel")?(O=()=>()=>{m.current!==null&&clearTimeout(m.current.timeoutId)},k=[],n[18]=O,n[19]=k):(O=n[18],k=n[19]),x.useEffect(O,k);let H;n[20]!==y||n[21]!==u||n[22]!==d?(H=q=>{u&&(d(`import graph from ${q}`),y(`Importing graph "${q}"…`,"info"))},n[20]=y,n[21]=u,n[22]=d,n[23]=H):H=n[23];const L=H;let Y;return n[24]!==L||n[25]!==A?(Y={handleSaveGraph:A,handleLoadGraph:L},n[24]=L,n[25]=A,n[26]=Y):Y=n[26],Y}const ar=new Map;function N1(c){const n=Se.c(7);let s;n[0]!==c?(s=()=>ar.get(c)??null,n[0]=c,n[1]=s):s=n[1];const[u,d]=x.useState(s);let p;n[2]!==c?(p=m=>{d(m),m===null?ar.delete(c):ar.set(c,m)},n[2]=c,n[3]=p):p=n[3];const b=p;let y;return n[4]!==u||n[5]!==b?(y=[u,b],n[4]=u,n[5]=b,n[6]=y):y=n[6],y}function jp(c){if(c==null)return"";const n=typeof c=="string"?c:JSON.stringify(c);return n.includes("'''")&&console.warn(`[commandBuilder] Property value contains "'''" which cannot be escaped in the backend grammar. The value may be truncated on paste.`),n.includes(`
`)?`'''
${n}
'''`:n}function C1(c,n){const s=[`${c} node ${n.alias}`];n.types.length>0&&s.push(`with type ${n.types[0]}`);const u=Object.entries(n.properties).filter(([,d])=>d!=null);if(u.length>0){s.push("with properties");for(const[d,p]of u)if(Array.isArray(p))for(const b of p)s.push(`${d}[]=${jp(b)}`);else s.push(`${d}[]=${jp(p)}`)}return s.join(`
`)}const j1="_toastContainer_1ot2i_1",M1="_toast_1ot2i_1",D1="_slideIn_1ot2i_1",O1="_success_1ot2i_36",z1="_error_1ot2i_40",R1="_info_1ot2i_44",k1="_toastIcon_1ot2i_48",B1="_toastMessage_1ot2i_53",co={toastContainer:j1,toast:M1,slideIn:D1,success:O1,error:z1,info:R1,toastIcon:k1,toastMessage:B1},U1=c=>{const n=Se.c(7),{toasts:s,onRemove:u}=c;if(s.length===0)return null;let d;if(n[0]!==u||n[1]!==s){let b;n[3]!==u?(b=y=>h.jsxs("div",{className:`${co.toast} ${co[y.type]}`,onClick:()=>u(y.id),children:[h.jsxs("span",{className:co.toastIcon,children:[y.type==="success"&&"✅",y.type==="error"&&"❌",y.type==="info"&&"ℹ️"]}),h.jsx("span",{className:co.toastMessage,children:y.message})]},y.id),n[3]=u,n[4]=b):b=n[4],d=s.map(b),n[0]=u,n[1]=s,n[2]=d}else d=n[2];let p;return n[5]!==d?(p=h.jsx("div",{className:co.toastContainer,children:d}),n[5]=d,n[6]=p):p=n[6],p},H1="_container_1pt3n_3",G1="_trigger_1pt3n_7",L1="_chevron_1pt3n_37",q1="_chevronOpen_1pt3n_43",Y1="_dot_1pt3n_49",X1="_dotIdle_1pt3n_56",J1="_dotConnecting_1pt3n_57",Z1="_dotConnected_1pt3n_58",Q1="_dotPartial_1pt3n_59",V1="_dropdown_1pt3n_65",un={container:H1,trigger:G1,chevron:L1,chevronOpen:q1,dot:Y1,dotIdle:X1,dotConnecting:J1,dotConnected:Z1,dotPartial:Q1,dropdown:V1};function ur(c){const n=Se.c(23),{label:s,dotStatus:u,children:d}=c,[p,b]=x.useState(!1),y=x.useRef(null);let m,g;n[0]!==p?(m=()=>{if(!p)return;const Y=q=>{y.current&&!y.current.contains(q.target)&&b(!1)};return document.addEventListener("mousedown",Y),()=>document.removeEventListener("mousedown",Y)},g=[p],n[0]=p,n[1]=m,n[2]=g):(m=n[1],g=n[2]),x.useEffect(m,g);let A;n[3]===Symbol.for("react.memo_cache_sentinel")?(A=Y=>{var q,B;Y.key==="Escape"&&(b(!1),(B=(q=y.current)==null?void 0:q.querySelector("button[aria-haspopup]"))==null||B.focus())},n[3]=A):A=n[3];const v=A,T=u==="connected"?un.dotConnected:u==="connecting"?un.dotConnecting:u==="partial"?un.dotPartial:u==="idle"?un.dotIdle:void 0;let N;n[4]===Symbol.for("react.memo_cache_sentinel")?(N=()=>b(K1),n[4]=N):N=n[4];let w;n[5]!==T||n[6]!==u?(w=u!==void 0&&h.jsx("span",{className:`${un.dot} ${T??""}`,"aria-hidden":"true"}),n[5]=T,n[6]=u,n[7]=w):w=n[7];let C;n[8]!==s?(C=h.jsx("span",{children:s}),n[8]=s,n[9]=C):C=n[9];const S=`${un.chevron} ${p?un.chevronOpen:""}`;let O;n[10]!==S?(O=h.jsx("span",{className:S,"aria-hidden":"true",children:"▾"}),n[10]=S,n[11]=O):O=n[11];let k;n[12]!==p||n[13]!==w||n[14]!==C||n[15]!==O?(k=h.jsxs("button",{className:un.trigger,onClick:N,"aria-haspopup":"true","aria-expanded":p,children:[w,C,O]}),n[12]=p,n[13]=w,n[14]=C,n[15]=O,n[16]=k):k=n[16];let H;n[17]!==d||n[18]!==p?(H=p&&h.jsx("div",{className:un.dropdown,role:"menu",children:d}),n[17]=d,n[18]=p,n[19]=H):H=n[19];let L;return n[20]!==H||n[21]!==k?(L=h.jsxs("div",{className:un.container,ref:y,onKeyDown:v,children:[k,H]}),n[20]=H,n[21]=k,n[22]=L):L=n[22],L}function K1(c){return!c}const I1="_nav_8zfdi_3",$1="_menuList_8zfdi_11",W1="_menuItem_8zfdi_19",F1="_toolRow_8zfdi_51",P1="_toolLink_8zfdi_62",ev="_toolLinkActive_8zfdi_83",tv="_toolDot_8zfdi_90",nv="_toolDotIdle_8zfdi_97",av="_toolDotConnecting_8zfdi_98",lv="_toolDotConnected_8zfdi_99",ov="_connectAllRow_8zfdi_103",iv="_connectAllBtn_8zfdi_109",sv="_connectAllBtnStop_8zfdi_133",cv="_toolConnectBtn_8zfdi_145",rv="_toolConnectBtnStop_8zfdi_171",uv="_externalIcon_8zfdi_183",ht={nav:I1,menuList:$1,menuItem:W1,toolRow:F1,toolLink:P1,toolLinkActive:ev,toolDot:tv,toolDotIdle:nv,toolDotConnecting:av,toolDotConnected:lv,connectAllRow:ov,connectAllBtn:iv,connectAllBtnStop:sv,toolConnectBtn:cv,toolConnectBtnStop:rv,externalIcon:uv};function dv(c){return c.every(n=>n==="connected")?"connected":c.every(n=>n==="idle")?"idle":c.some(n=>n==="connecting")?"connecting":"partial"}function fv(c){return c==="connected"?"connected":c==="connecting"?"connecting":"idle"}const pv=[{href:"/info",label:"Info"},{href:"/info/lib",label:"Libraries"},{href:"/info/routes",label:"Services"},{href:"/health",label:"Health"},{href:"/env",label:"Environment"},{href:"http://localhost:8085/api/ws/json",label:"Legacy JSON"},{href:"http://localhost:8085/api/ws/graph",label:"Legacy Graph"}];function hv(c){const n=Se.c(27),{addToast:s}=c,u=br();let d,p,b;if(n[0]!==u){const q=On.map(B=>u.getSlot(B.wsPath).phase);b=dv(q),d=q.every(vv),p=q.some(gv),n[0]=u,n[1]=d,n[2]=p,n[3]=b}else d=n[1],p=n[2],b=n[3];const y=p;let m;n[4]!==s||n[5]!==u?(m=function(){On.forEach(B=>{u.getSlot(B.wsPath).phase==="idle"&&u.connect(B.wsPath,s)})},n[4]=s,n[5]=u,n[6]=m):m=n[6];const g=m;let A;n[7]!==u?(A=function(){On.forEach(B=>{const{phase:J}=u.getSlot(B.wsPath);(J==="connected"||J==="connecting")&&u.disconnect(B.wsPath)})},n[7]=u,n[8]=A):A=n[8];const v=A,T=`${ht.connectAllBtn} ${d?ht.connectAllBtnStop:""}`,N=d?v:g,w=y?"Connecting…":d?"Disconnect all WebSockets":"Connect all WebSockets",C=y?"Connecting…":d?"Disconnect All":"Connect All";let S;n[9]!==y||n[10]!==T||n[11]!==N||n[12]!==w||n[13]!==C?(S=h.jsx("div",{className:ht.connectAllRow,children:h.jsx("button",{className:T,onClick:N,disabled:y,"aria-label":w,children:C})}),n[9]=y,n[10]=T,n[11]=N,n[12]=w,n[13]=C,n[14]=S):S=n[14];let O;n[15]!==s||n[16]!==u?(O=On.map(q=>{const{phase:B}=u.getSlot(q.wsPath),J=fv(B),K=B==="connected",I=B==="connecting",$=J==="connected"?ht.toolDotConnected:J==="connecting"?ht.toolDotConnecting:ht.toolDotIdle;return h.jsxs("li",{role:"none",className:ht.toolRow,children:[h.jsxs(Ay,{to:q.path,role:"menuitem",className:yv,children:[h.jsx("span",{className:`${ht.toolDot} ${$}`,"aria-hidden":"true"}),h.jsx("span",{className:ht.toolLabel,children:q.label})]}),h.jsx("button",{className:`${ht.toolConnectBtn} ${K?ht.toolConnectBtnStop:""}`,onClick:()=>K||I?u.disconnect(q.wsPath):u.connect(q.wsPath,s),disabled:I,"aria-label":I?"Connecting…":K?`Disconnect ${q.label}`:`Connect ${q.label}`,title:I?"Connecting…":Ip(q.wsPath),children:I?"…":K?"Stop":"Start"})]},q.path)}),n[15]=s,n[16]=u,n[17]=O):O=n[17];let k;n[18]!==O?(k=h.jsx("ul",{className:ht.menuList,role:"none",children:O}),n[18]=O,n[19]=k):k=n[19];let H;n[20]!==k||n[21]!==S||n[22]!==b?(H=h.jsxs(ur,{label:"Tools",dotStatus:b,children:[S,k]}),n[20]=k,n[21]=S,n[22]=b,n[23]=H):H=n[23];let L;n[24]===Symbol.for("react.memo_cache_sentinel")?(L=h.jsx(ur,{label:"Quick Links",children:h.jsx("ul",{className:ht.menuList,role:"none",children:pv.map(mv)})}),n[24]=L):L=n[24];let Y;return n[25]!==H?(Y=h.jsxs("nav",{className:ht.nav,"aria-label":"Main navigation",children:[H,L]}),n[25]=H,n[26]=Y):Y=n[26],Y}function mv(c){return h.jsx("li",{role:"none",children:h.jsxs("a",{href:c.href,role:"menuitem",className:ht.menuItem,target:"_blank",rel:"noopener noreferrer",children:[c.label,h.jsx("span",{className:ht.externalIcon,"aria-hidden":"true",children:"↗"})]})},c.href)}function yv(c){const{isActive:n}=c;return`${ht.toolLink} ${n?ht.toolLinkActive:""}`}function gv(c){return c==="connecting"}function vv(c){return c==="connected"}const bv="_saveBtn_1xd2l_3",_v="_saveForm_1xd2l_33",Sv="_saveInput_1xd2l_39",xv="_saveInputWarn_1xd2l_55",Tv="_saveWarnLabel_1xd2l_59",Ev="_saveActionBtn_1xd2l_65",Ca={saveBtn:bv,saveForm:_v,saveInput:Sv,saveInputWarn:xv,saveWarnLabel:Tv,saveActionBtn:Ev};function wv(c){const n=Se.c(33),{disabled:s,defaultName:u,onSave:d,nameExists:p,connected:b}=c,y=b===void 0?!1:b,[m,g]=x.useState(!1),[A,v]=x.useState(""),T=x.useRef(null);let N;n[0]!==u?(N=()=>{v(u),g(!0)},n[0]=u,n[1]=N):N=n[1];const w=N;let C;n[2]===Symbol.for("react.memo_cache_sentinel")?(C=()=>{g(!1),v("")},n[2]=C):C=n[2];const S=C;let O;n[3]!==d||n[4]!==A?(O=()=>{const I=A.trim();I&&(d(I),g(!1),v(""))},n[3]=d,n[4]=A,n[5]=O):O=n[5];const k=O;let H;n[6]!==k?(H=I=>{I.key==="Enter"&&(I.preventDefault(),k()),I.key==="Escape"&&(I.preventDefault(),S())},n[6]=k,n[7]=H):H=n[7];const L=H;let Y,q;if(n[8]!==m?(Y=()=>{var I;m&&((I=T.current)==null||I.focus())},q=[m],n[8]=m,n[9]=Y,n[10]=q):(Y=n[9],q=n[10]),x.useEffect(Y,q),m){const I=`${Ca.saveInput}${p!=null&&p(A.trim())?` ${Ca.saveInputWarn}`:""}`;let $;n[11]===Symbol.for("react.memo_cache_sentinel")?($=F=>v(F.target.value),n[11]=$):$=n[11];let ne;n[12]!==L||n[13]!==A||n[14]!==I?(ne=h.jsx("input",{ref:T,className:I,type:"text",value:A,onChange:$,onKeyDown:L,placeholder:"Enter a name…","aria-label":"Graph save name",maxLength:80}),n[12]=L,n[13]=A,n[14]=I,n[15]=ne):ne=n[15];let ue;n[16]!==p||n[17]!==A?(ue=(p==null?void 0:p(A.trim()))&&h.jsx("span",{className:Ca.saveWarnLabel,role:"status",children:"Overwrite?"}),n[16]=p,n[17]=A,n[18]=ue):ue=n[18];let re;n[19]!==A?(re=A.trim(),n[19]=A,n[20]=re):re=n[20];const ie=!re;let M;n[21]!==k||n[22]!==ie?(M=h.jsx("button",{className:Ca.saveActionBtn,onClick:k,disabled:ie,"aria-label":"Confirm save",children:"✅"}),n[21]=k,n[22]=ie,n[23]=M):M=n[23];let D;n[24]===Symbol.for("react.memo_cache_sentinel")?(D=h.jsx("button",{className:Ca.saveActionBtn,onClick:S,"aria-label":"Cancel save",children:"❌"}),n[24]=D):D=n[24];let V;return n[25]!==ne||n[26]!==ue||n[27]!==M?(V=h.jsxs("div",{className:Ca.saveForm,children:[ne,ue,M,D]}),n[25]=ne,n[26]=ue,n[27]=M,n[28]=V):V=n[28],V}const B=s||!y,J=s?"No graph loaded":y?"Export graph snapshot to server and save bookmark":"Connect first to save";let K;return n[29]!==w||n[30]!==B||n[31]!==J?(K=h.jsx("button",{className:Ca.saveBtn,onClick:w,disabled:B,title:J,"aria-label":"Save graph snapshot",children:"💾 Save Graph"}),n[29]=w,n[30]=B,n[31]=J,n[32]=K):K=n[32],K}const Av="_empty_d1tzv_3",Nv="_hint_d1tzv_12",Cv="_list_d1tzv_21",jv="_row_d1tzv_31",Mv="_rowInfo_d1tzv_50",Dv="_rowName_d1tzv_58",Ov="_rowMeta_d1tzv_67",zv="_rowActions_d1tzv_78",Rv="_loadBtn_d1tzv_84",kv="_deleteBtn_d1tzv_85",dn={empty:Av,hint:Nv,list:Cv,row:jv,rowInfo:Mv,rowName:Dv,rowMeta:Ov,rowActions:zv,loadBtn:Rv,deleteBtn:kv};function Bv(c){const n=Se.c(8),{savedGraphs:s,onLoad:u,onDelete:d,connected:p}=c,b=s.length>0?`Load Graph (${s.length})`:"Load Graph";let y;n[0]!==p||n[1]!==d||n[2]!==u||n[3]!==s?(y=s.length===0?h.jsx("p",{className:dn.empty,children:"No saved graphs yet."}):h.jsxs(h.Fragment,{children:[!p&&h.jsx("p",{className:dn.hint,children:"Connect to load a graph"}),h.jsx("ul",{className:dn.list,role:"list",children:s.map(g=>h.jsxs("li",{className:dn.row,children:[h.jsxs("div",{className:dn.rowInfo,children:[h.jsx("span",{className:dn.rowName,title:g.name,children:g.name}),h.jsx("span",{className:dn.rowMeta,children:new Date(g.savedAt).toLocaleString()})]}),h.jsxs("div",{className:dn.rowActions,children:[h.jsx("button",{className:dn.loadBtn,onClick:()=>u(g.name),disabled:!p,title:p?`Run: import graph from ${g.name}`:"Connect to the playground first","aria-label":`Load graph ${g.name}`,children:"Load"}),h.jsx("button",{className:dn.deleteBtn,onClick:()=>d(g.name),title:`Remove "${g.name}" from local storage`,"aria-label":`Delete saved graph ${g.name}`,children:"Delete"})]})]},g.name))})]}),n[0]=p,n[1]=d,n[2]=u,n[3]=s,n[4]=y):y=n[4];let m;return n[5]!==b||n[6]!==y?(m=h.jsx(ur,{label:b,children:y}),n[5]=b,n[6]=y,n[7]=m):m=n[7],m}const Uv="_payloadRoot_jsab4_2",Hv="_labelRow_jsab4_10",Gv="_label_jsab4_10",Lv="_payloadControls_jsab4_26",qv="_charCounter_jsab4_32",Yv="_typeIndicator_jsab4_38",Xv="_validationIcon_jsab4_49",Jv="_formatButton_jsab4_53",Zv="_uploadButton_jsab4_67",Qv="_textarea_jsab4_82",Vv="_textareaError_jsab4_107",Kv="_errorMessage_jsab4_109",Iv="_sampleButtonsRow_jsab4_117",$v="_sampleButtons_jsab4_117",Wv="_sampleLabel_jsab4_130",Fv="_sampleGroup_jsab4_136",Pv="_sampleGroupLabel_jsab4_143",e0="_sampleButton_jsab4_117",at={payloadRoot:Uv,labelRow:Hv,label:Gv,payloadControls:Lv,charCounter:qv,typeIndicator:Yv,validationIcon:Xv,formatButton:Jv,uploadButton:Zv,textarea:Qv,textareaError:Vv,errorMessage:Kv,sampleButtonsRow:Iv,sampleButtons:$v,sampleLabel:Wv,sampleGroup:Fv,sampleGroupLabel:Pv,sampleButton:e0};function t0(c){const n=Se.c(21),{onLoad:s}=c;let u,d,p,b,y,m;if(n[0]!==s){const v=Object.keys(ji).filter(l0),T=Object.keys(ji).filter(a0),N=n0;b=at.sampleButtons,n[7]===Symbol.for("react.memo_cache_sentinel")?(y=h.jsx("span",{className:at.sampleLabel,children:"Quick load:"}),n[7]=y):y=n[7];let w;n[8]===Symbol.for("react.memo_cache_sentinel")?(w=h.jsx("span",{className:at.sampleGroupLabel,children:"JSON:"}),n[8]=w):w=n[8];const C=v.map(S=>h.jsx("button",{className:at.sampleButton,onClick:()=>s(ji[S]),children:N(S)},S));n[9]!==C?(m=h.jsxs("div",{className:at.sampleGroup,children:[w,C]}),n[9]=C,n[10]=m):m=n[10],u=at.sampleGroup,n[11]===Symbol.for("react.memo_cache_sentinel")?(d=h.jsx("span",{className:at.sampleGroupLabel,children:"XML:"}),n[11]=d):d=n[11],p=T.map(S=>h.jsx("button",{className:at.sampleButton,onClick:()=>s(ji[S]),children:N(S)},S)),n[0]=s,n[1]=u,n[2]=d,n[3]=p,n[4]=b,n[5]=y,n[6]=m}else u=n[1],d=n[2],p=n[3],b=n[4],y=n[5],m=n[6];let g;n[12]!==u||n[13]!==d||n[14]!==p?(g=h.jsxs("div",{className:u,children:[d,p]}),n[12]=u,n[13]=d,n[14]=p,n[15]=g):g=n[15];let A;return n[16]!==b||n[17]!==y||n[18]!==m||n[19]!==g?(A=h.jsxs("div",{className:b,children:[y,m,g]}),n[16]=b,n[17]=y,n[18]=m,n[19]=g,n[20]=A):A=n[20],A}function n0(c){return c.replace(/^(json|xml)_/,"").replace(/_/g," ")}function a0(c){return c.startsWith("xml_")}function l0(c){return c.startsWith("json_")}function o0(c){const n=Se.c(40),{payload:s,onChange:u,validation:d,onFormat:p,onUpload:b}=c;let y;n[0]===Symbol.for("react.memo_cache_sentinel")?(y=h.jsx("label",{htmlFor:"payload",className:at.label,children:"JSON/XML Payload"}),n[0]=y):y=n[0];let m;n[1]!==s.length?(m=h.jsxs("span",{className:at.charCounter,children:["size: ",s.length]}),n[1]=s.length,n[2]=m):m=n[2];let g;n[3]!==s||n[4]!==d.type?(g=s&&d.type&&h.jsx("span",{className:at.typeIndicator,children:d.type.toUpperCase()}),n[3]=s,n[4]=d.type,n[5]=g):g=n[5];let A;n[6]!==s||n[7]!==d.valid?(A=s&&h.jsx("span",{className:at.validationIcon,children:d.valid?"✅":"❌"}),n[6]=s,n[7]=d.valid,n[8]=A):A=n[8];const v=!s||d.type!=="json",T=d.type==="xml"?"Format only available for JSON":"Format JSON";let N;n[9]!==p||n[10]!==v||n[11]!==T?(N=h.jsx("button",{className:at.formatButton,onClick:p,disabled:v,title:T,children:"Format"}),n[9]=p,n[10]=v,n[11]=T,n[12]=N):N=n[12];let w;n[13]!==b||n[14]!==s||n[15]!==d.type||n[16]!==d.valid?(w=b!==void 0&&h.jsx("button",{className:at.uploadButton,onClick:b,disabled:!s||!d.valid||d.type!=="json",title:"Upload JSON payload to current session via REST",children:"Upload"}),n[13]=b,n[14]=s,n[15]=d.type,n[16]=d.valid,n[17]=w):w=n[17];let C;n[18]!==m||n[19]!==g||n[20]!==A||n[21]!==N||n[22]!==w?(C=h.jsxs("div",{className:at.labelRow,children:[y,h.jsxs("div",{className:at.payloadControls,children:[m,g,A,N,w]})]}),n[18]=m,n[19]=g,n[20]=A,n[21]=N,n[22]=w,n[23]=C):C=n[23];const S=`${at.textarea} ${d.valid?"":at.textareaError}`;let O;n[24]!==u?(O=q=>u(q.target.value),n[24]=u,n[25]=O):O=n[25];let k;n[26]!==s||n[27]!==S||n[28]!==O?(k=h.jsx("textarea",{id:"payload",className:S,placeholder:"Paste your JSON/XML payload here",value:s,onChange:O}),n[26]=s,n[27]=S,n[28]=O,n[29]=k):k=n[29];let H;n[30]!==d.error||n[31]!==d.valid?(H=!d.valid&&h.jsx("div",{className:at.errorMessage,children:d.error}),n[30]=d.error,n[31]=d.valid,n[32]=H):H=n[32];let L;n[33]!==u?(L=h.jsx("div",{className:at.sampleButtonsRow,children:h.jsx(t0,{onLoad:u})}),n[33]=u,n[34]=L):L=n[34];let Y;return n[35]!==k||n[36]!==H||n[37]!==L||n[38]!==C?(Y=h.jsxs("div",{className:at.payloadRoot,children:[C,k,H,L]}),n[35]=k,n[36]=H,n[37]=L,n[38]=C,n[39]=Y):Y=n[39],Y}const i0="_content_7m22c_8",s0="_header_7m22c_22",c0="_icon_7m22c_42",r0="_alias_7m22c_47",u0="_badge_7m22c_53",d0="_body_7m22c_65",f0="_row_7m22c_70",p0="_label_7m22c_83",h0="_value_7m22c_89",Dn={content:i0,header:s0,icon:c0,alias:r0,badge:u0,body:d0,row:f0,label:p0,value:h0},m0={Root:{icon:"🚀",label:"Root"},End:{icon:"🏁",label:"End"},Fetcher:{icon:"🌐",label:"Fetcher"},mapper:{icon:"🗺️",label:"Mapper"},Math:{icon:"🔢",label:"Math"},JavaScript:{icon:"📜",label:"JavaScript"},Provider:{icon:"🔌",label:"Provider"},Dictionary:{icon:"📖",label:"Dictionary"},Join:{icon:"🔀",label:"Join"},Extension:{icon:"🧩",label:"Extension"},Island:{icon:"🏝️",label:"Island"},Decision:{icon:"❓",label:"Decision"}};function y0(c){return m0[c]??{icon:"📦",label:c}}function Mp(c){const n=Se.c(7),{label:s,value:u}=c;let d;n[0]!==s?(d=h.jsx("span",{className:Dn.label,children:s}),n[0]=s,n[1]=d):d=n[1];let p;n[2]!==u?(p=h.jsx("span",{className:Dn.value,title:u,children:u}),n[2]=u,n[3]=p):p=n[3];let b;return n[4]!==d||n[5]!==p?(b=h.jsxs("div",{className:Dn.row,children:[d,p]}),n[4]=d,n[5]=p,n[6]=b):b=n[6],b}function g0(c){const n=Se.c(3),{properties:s}=c;let u,d;if(n[0]!==s){d=Symbol.for("react.early_return_sentinel");e:{const p=Object.entries(s).filter(b0);if(p.length===0){d=null;break e}u=h.jsx(h.Fragment,{children:p.map(v0)})}n[0]=s,n[1]=u,n[2]=d}else u=n[1],d=n[2];return d!==Symbol.for("react.early_return_sentinel")?d:u}function v0(c){const[n,s]=c;if(Array.isArray(s))return s.map((d,p)=>{const b=typeof d=="string"?d:JSON.stringify(d);return h.jsx(Mp,{label:p===0?n:"",value:b},`${n}-${p}`)});const u=typeof s=="string"?s:JSON.stringify(s);return h.jsx(Mp,{label:n,value:u},n)}function b0(c){const[,n]=c;return n!=null}function qt(c){const n=Se.c(28),{data:s,isConnectable:u,selected:d}=c;let p;n[0]!==s.nodeType?(p=y0(s.nodeType),n[0]=s.nodeType,n[1]=p):p=n[1];const b=p;let y;n[2]!==d?(y=h.jsx(Dy,{minWidth:180,minHeight:60,isVisible:d}),n[2]=d,n[3]=y):y=n[3];let m;n[4]!==u?(m=h.jsx(bp,{type:"target",position:_p.Left,isConnectable:u}),n[4]=u,n[5]=m):m=n[5];let g;n[6]!==b.icon?(g=h.jsx("span",{className:Dn.icon,children:b.icon}),n[6]=b.icon,n[7]=g):g=n[7];let A;n[8]!==s.alias?(A=h.jsx("span",{className:Dn.alias,children:s.alias}),n[8]=s.alias,n[9]=A):A=n[9];let v;n[10]!==b.label?(v=h.jsx("span",{className:Dn.badge,children:b.label}),n[10]=b.label,n[11]=v):v=n[11];let T;n[12]!==g||n[13]!==A||n[14]!==v?(T=h.jsxs("div",{className:Dn.header,children:[g,A,v]}),n[12]=g,n[13]=A,n[14]=v,n[15]=T):T=n[15];let N;n[16]!==s.properties?(N=h.jsx("div",{className:Dn.body,children:h.jsx(g0,{properties:s.properties})}),n[16]=s.properties,n[17]=N):N=n[17];let w;n[18]!==T||n[19]!==N?(w=h.jsxs("div",{className:Dn.content,children:[T,N]}),n[18]=T,n[19]=N,n[20]=w):w=n[20];let C;n[21]!==u?(C=h.jsx(bp,{type:"source",position:_p.Right,isConnectable:u}),n[21]=u,n[22]=C):C=n[22];let S;return n[23]!==C||n[24]!==y||n[25]!==m||n[26]!==w?(S=h.jsxs(h.Fragment,{children:[y,m,w,C]}),n[23]=C,n[24]=y,n[25]=m,n[26]=w,n[27]=S):S=n[27],S}const _0={Root:qt,End:qt,Fetcher:qt,mapper:qt,Math:qt,JavaScript:qt,Provider:qt,Dictionary:qt,Join:qt,Extension:qt,Island:qt,Decision:qt,default:qt},S0="_graphWrapper_1tscm_15",x0="_empty_1tscm_22",T0="_emptyIcon_1tscm_35",E0="_refreshingOverlay_1tscm_69",w0="_refreshingSpinner_1tscm_85",A0="_contextMenu_1tscm_100",N0="_contextMenuItem_1tscm_110",an={graphWrapper:S0,empty:x0,emptyIcon:T0,refreshingOverlay:E0,refreshingSpinner:w0,contextMenu:A0,contextMenuItem:N0};class C0 extends x.Component{constructor(){super(...arguments),this.state={caughtError:null}}static getDerivedStateFromError(n){return{caughtError:n instanceof Error?n.message:String(n)}}componentDidCatch(n,s){var d,p;const u=n instanceof Error?n.message:String(n);console.error("[GraphView] Render error:",u,s.componentStack),(p=(d=this.props).onRenderError)==null||p.call(d,`Graph render failed: ${u}`)}render(){return this.state.caughtError?h.jsxs("div",{className:an.empty,children:[h.jsx("span",{className:an.emptyIcon,children:"⚠️"}),h.jsx("span",{children:"Graph could not be rendered."}),h.jsx("span",{children:this.state.caughtError})]}):this.props.children}}const ah=240,dr=100,Dp=60,j0=120,M0={boxSizing:"border-box",borderRadius:"8px",borderWidth:"1.5px",borderStyle:"solid",background:"var(--bg-secondary, #1e1e2e)",color:"var(--text-primary, #cdd6f4)",fontSize:"0.75rem",boxShadow:"0 2px 8px rgba(0,0,0,0.45)",overflow:"visible",padding:0},D0={Root:"#15803d",End:"#dc2626",Fetcher:"#2563eb",mapper:"#ea580c",Math:"#a16207",JavaScript:"#7e22ce",Provider:"#be185d",Dictionary:"#0e7490",Join:"#65a30d",Extension:"#4338ca",Island:"#475569",Decision:"#b45309"},O0="#6c7086";function z0(c){const n=D0[c]??O0;return{...M0,borderColor:n,"--node-accent":n}}function R0(c,n){var A;const s=new Map,u=new Map;for(const v of c)s.set(v.alias,[]),u.set(v.alias,0);for(const v of n??[])(A=s.get(v.source))==null||A.push(v.target),u.set(v.target,(u.get(v.target)??0)+1);const d=c.filter(v=>u.get(v.alias)===0||v.types.includes("entry_point")).map(v=>v.alias),p=new Map,b=[...d];for(d.forEach(v=>p.set(v,0));b.length>0;){const v=b.shift(),T=p.get(v)??0;for(const N of s.get(v)??[])(!p.has(N)||p.get(N)<=T)&&(p.set(N,T+1),b.push(N))}const y=p.size>0?Math.max(...p.values()):0;for(const v of c)p.has(v.alias)||p.set(v.alias,y+1);const m=new Map;for(const[v,T]of p)m.has(T)||m.set(T,[]),m.get(T).push(v);const g=new Map;for(const[v,T]of m){const N=T.length*dr+(T.length-1)*Dp;T.forEach((w,C)=>{g.set(w,{x:v*(ah+j0),y:C*(dr+Dp)-N/2})})}return g}function k0(c){const n=R0(c.nodes,c.connections??[]),s=c.nodes.map(d=>({id:d.alias,type:d.types[0]??"default",position:n.get(d.alias)??{x:0,y:0},width:ah,height:dr,style:z0(d.types[0]??"unknown"),data:{alias:d.alias,nodeType:d.types[0]??"unknown",properties:d.properties}})),u=[];for(const d of c.connections??[]){const p=d.relations.map(y=>y.type),b=`${d.source}__${d.target}`;u.push({id:b,source:d.source,target:d.target,label:p.join(", "),type:"smoothstep",data:{relationTypes:p}})}return{nodes:s,edges:u}}function B0(c,n){return c.nodes.find(s=>s.alias===n)}function U0(c,n){return(c.connections??[]).filter(s=>s.source!==s.target&&(s.source===n||s.target===n))}const H0="_toolbar_13786_2",G0="_label_13786_12",L0="_toolbarActions_13786_18",q0="_toolbarButton_13786_24",Mi={toolbar:H0,label:G0,toolbarActions:L0,toolbarButton:q0};function lh(c){const n=Se.c(19),{graphData:s,onCopySuccess:u,onCopyError:d,extraActions:p}=c;let b;n[0]!==s||n[1]!==d||n[2]!==u?(b=()=>{s&&navigator.clipboard.writeText(JSON.stringify(s,null,2)).then(()=>u==null?void 0:u()).catch(()=>d==null?void 0:d())},n[0]=s,n[1]=d,n[2]=u,n[3]=b):b=n[3];const y=b,m=(s==null?void 0:s.nodes.length)??0;let g;n[4]!==(s==null?void 0:s.connections)?(g=(s==null?void 0:s.connections)??[],n[4]=s==null?void 0:s.connections,n[5]=g):g=n[5];const A=g.length,v=m!==1?"s":"",T=A!==1?"s":"";let N;n[6]!==A||n[7]!==m||n[8]!==v||n[9]!==T?(N=h.jsxs("span",{className:Mi.label,children:[m," node",v," · ",A," connection",T]}),n[6]=A,n[7]=m,n[8]=v,n[9]=T,n[10]=N):N=n[10];let w;n[11]!==y?(w=h.jsx("button",{className:Mi.toolbarButton,onClick:y,title:"Copy raw graph JSON to clipboard","aria-label":"Copy raw graph JSON to clipboard",children:"📑"}),n[11]=y,n[12]=w):w=n[12];let C;n[13]!==p||n[14]!==w?(C=h.jsxs("div",{className:Mi.toolbarActions,children:[p,w]}),n[13]=p,n[14]=w,n[15]=C):C=n[15];let S;return n[16]!==N||n[17]!==C?(S=h.jsxs("div",{className:Mi.toolbar,children:[N,C]}),n[16]=N,n[17]=C,n[18]=S):S=n[18],S}const Op=[],zp=[];function Y0(c){const n=Se.c(65),{graphData:s,onCopySuccess:u,onCopyError:d,onRenderError:p,isRefreshing:b,onClipNode:y}=c,m=b===void 0?!1:b,[g,A]=x.useState(null),v=x.useRef(null);let T,N;n[0]!==g?(T=()=>{if(!g)return;const le=pe=>{v.current&&!v.current.contains(pe.target)&&A(null)},de=pe=>{pe.key==="Escape"&&A(null)};return document.addEventListener("mousedown",le),document.addEventListener("keydown",de),()=>{document.removeEventListener("mousedown",le),document.removeEventListener("keydown",de)}},N=[g],n[0]=g,n[1]=T,n[2]=N):(T=n[1],N=n[2]),x.useEffect(T,N);const w=x.useRef(p);let C,S;n[3]!==p?(C=()=>{w.current=p},S=[p],n[3]=p,n[4]=C,n[5]=S):(C=n[4],S=n[5]),x.useEffect(C,S);let O;e:{if(!s){let le;n[6]===Symbol.for("react.memo_cache_sentinel")?(le={nodes:Op,edges:zp,transformError:null},n[6]=le):le=n[6],O=le;break e}try{let le;n[7]!==s?(le=k0(s),n[7]=s,n[8]=le):le=n[8];const de=le;let pe;n[9]!==de?(pe={...de,transformError:null},n[9]=de,n[10]=pe):pe=n[10],O=pe}catch(le){const de=le,pe=de instanceof Error?de.message:String(de);let Re;n[11]===Symbol.for("react.memo_cache_sentinel")?(Re={nodes:Op,edges:zp,transformError:pe},n[11]=Re):Re=n[11],O=Re}}const{nodes:k,edges:H,transformError:L}=O;let Y,q;n[12]!==L?(Y=()=>{var le;L&&((le=w.current)==null||le.call(w,`Graph render failed: ${L}`))},q=[L],n[12]=L,n[13]=Y,n[14]=q):(Y=n[13],q=n[14]),x.useEffect(Y,q);let B;n[15]!==s?(B=s?JSON.stringify(s.nodes.map(J0)):"empty",n[15]=s,n[16]=B):B=n[16];const J=B,[K,I,$]=Oy(k),[ne,ue,re]=zy(H);let ie,M;if(n[17]!==H||n[18]!==k||n[19]!==ue||n[20]!==I?(ie=()=>{I(k),ue(H)},M=[k,H,I,ue],n[17]=H,n[18]=k,n[19]=ue,n[20]=I,n[21]=ie,n[22]=M):(ie=n[21],M=n[22]),x.useEffect(ie,M),L){let le,de;n[23]===Symbol.for("react.memo_cache_sentinel")?(le=h.jsx("span",{className:an.emptyIcon,children:"⚠️"}),de=h.jsx("span",{children:"Graph could not be rendered."}),n[23]=le,n[24]=de):(le=n[23],de=n[24]);let pe;return n[25]!==L?(pe=h.jsxs("div",{className:an.empty,children:[le,de,h.jsx("span",{children:L})]}),n[25]=L,n[26]=pe):pe=n[26],pe}if(!s||s.nodes.length===0){let le,de;n[27]===Symbol.for("react.memo_cache_sentinel")?(le=h.jsx("span",{className:an.emptyIcon,children:"🕸️"}),de=h.jsx("span",{children:"No graph data yet."}),n[27]=le,n[28]=de):(le=n[27],de=n[28]);let pe;n[29]===Symbol.for("react.memo_cache_sentinel")?(pe=h.jsx("strong",{children:"describe graph"}),n[29]=pe):pe=n[29];let Re;return n[30]===Symbol.for("react.memo_cache_sentinel")?(Re=h.jsxs("div",{className:an.empty,children:[le,de,h.jsxs("span",{children:["Run ",pe," or ",h.jsx("strong",{children:"export graph"})," in the playground."]})]}),n[30]=Re):Re=n[30],Re}let D;n[31]!==s||n[32]!==d||n[33]!==u?(D=h.jsx(lh,{graphData:s,onCopySuccess:u,onCopyError:d}),n[31]=s,n[32]=d,n[33]=u,n[34]=D):D=n[34];let V;n[35]===Symbol.for("react.memo_cache_sentinel")?(V={padding:.25},n[35]=V):V=n[35];let F;n[36]===Symbol.for("react.memo_cache_sentinel")?(F={hideAttribution:!1},n[36]=F):F=n[36];let ee;n[37]!==y?(ee=(le,de)=>{y&&(le.preventDefault(),A({x:le.clientX,y:le.clientY,nodeAlias:de.data.alias}))},n[37]=y,n[38]=ee):ee=n[38];let ce,te,P;n[39]===Symbol.for("react.memo_cache_sentinel")?(ce=()=>A(null),te=h.jsx(Ry,{variant:ky.Dots,gap:18,size:1,color:"rgba(255,255,255,0.07)"}),P=h.jsx(By,{showInteractive:!1}),n[39]=ce,n[40]=te,n[41]=P):(ce=n[39],te=n[40],P=n[41]);let me;n[42]===Symbol.for("react.memo_cache_sentinel")?(me=h.jsx(Uy,{nodeColor:X0,maskColor:"rgba(0,0,0,0.3)",style:{background:"#fff"}}),n[42]=me):me=n[42];let _e;n[43]!==ne||n[44]!==K||n[45]!==re||n[46]!==$||n[47]!==ee?(_e=h.jsxs(Hy,{nodes:K,edges:ne,onNodesChange:$,onEdgesChange:re,nodeTypes:_0,fitView:!0,fitViewOptions:V,minZoom:.2,maxZoom:2.5,proOptions:F,onNodeContextMenu:ee,onPaneClick:ce,children:[te,P,me]}),n[43]=ne,n[44]=K,n[45]=re,n[46]=$,n[47]=ee,n[48]=_e):_e=n[48];let xe;n[49]!==m?(xe=m&&h.jsx("div",{className:an.refreshingOverlay,children:h.jsx("div",{className:an.refreshingSpinner,role:"status","aria-label":"Graph refreshing"})}),n[49]=m,n[50]=xe):xe=n[50];let Be;n[51]!==g||n[52]!==s||n[53]!==y?(Be=g&&y&&s&&h.jsx("div",{ref:v,className:an.contextMenu,style:{position:"fixed",top:g.y,left:g.x},role:"menu",children:h.jsx("button",{role:"menuitem",autoFocus:!0,className:an.contextMenuItem,onClick:()=>{const le=B0(s,g.nodeAlias);if(le){const de=U0(s,g.nodeAlias);y(le,de)}A(null)},children:"Clip to Clipboard"})}),n[51]=g,n[52]=s,n[53]=y,n[54]=Be):Be=n[54];let qe;n[55]!==m||n[56]!==D||n[57]!==_e||n[58]!==xe||n[59]!==Be?(qe=h.jsxs("div",{className:an.graphWrapper,"aria-busy":m,children:[D,_e,xe,Be]}),n[55]=m,n[56]=D,n[57]=_e,n[58]=xe,n[59]=Be,n[60]=qe):qe=n[60];let Me;return n[61]!==J||n[62]!==p||n[63]!==qe?(Me=h.jsx(C0,{onRenderError:p,children:qe},J),n[61]=J,n[62]=p,n[63]=qe,n[64]=Me):Me=n[64],Me}function X0(c){return{Root:"#15803d",End:"#dc2626",Fetcher:"#2563eb",mapper:"#ea580c",Math:"#a16207",JavaScript:"#7e22ce",Provider:"#be185d",Dictionary:"#0e7490",Join:"#65a30d",Extension:"#4338ca",Island:"#475569",Decision:"#b45309"}[c.type??""]??"#6c7086"}function J0(c){return c.alias}const Z0="_root_da4ye_2",Q0="_empty_da4ye_10",V0="_emptyIcon_da4ye_23",K0="_toolbarButton_da4ye_29",I0="_scrollBody_da4ye_57",$0="_jsonContainer_da4ye_68",W0="_jsonLabel_da4ye_69",F0="_jsonString_da4ye_70",P0="_jsonNumber_da4ye_71",eb="_jsonBoolean_da4ye_72",tb="_jsonNull_da4ye_73",Yt={root:Z0,empty:Q0,emptyIcon:V0,toolbarButton:K0,scrollBody:I0,jsonContainer:$0,jsonLabel:W0,jsonString:F0,jsonNumber:P0,jsonBoolean:eb,jsonNull:tb},nb=Ly,ab=Gy,lb=c=>c<3,ob={default:lb,all:nb,none:ab};function ib(c){const n=Se.c(22),{graphData:s,onCopySuccess:u,onCopyError:d}=c,[p,b]=x.useState("all");if(!s){let L;return n[0]===Symbol.for("react.memo_cache_sentinel")?(L=h.jsx("div",{className:Yt.root,children:h.jsxs("div",{className:Yt.empty,children:[h.jsx("span",{className:Yt.emptyIcon,children:"🕸️"}),h.jsx("span",{children:"No graph data yet."}),h.jsx("span",{children:"Pin a graph-link message in the Console to load the raw data here."})]})}),n[0]=L):L=n[0],L}let y;n[1]===Symbol.for("react.memo_cache_sentinel")?(y=()=>b("all"),n[1]=y):y=n[1];const m=p==="all";let g;n[2]!==m?(g=h.jsx("button",{className:Yt.toolbarButton,onClick:y,title:"Expand all nodes","aria-label":"Expand all JSON nodes","aria-pressed":m,children:"➖"}),n[2]=m,n[3]=g):g=n[3];let A;n[4]===Symbol.for("react.memo_cache_sentinel")?(A=()=>b("none"),n[4]=A):A=n[4];const v=p==="none";let T;n[5]!==v?(T=h.jsx("button",{className:Yt.toolbarButton,onClick:A,title:"Collapse all nodes","aria-label":"Collapse all JSON nodes","aria-pressed":v,children:"➕"}),n[5]=v,n[6]=T):T=n[6];let N;n[7]!==g||n[8]!==T?(N=h.jsxs(h.Fragment,{children:[g,T]}),n[7]=g,n[8]=T,n[9]=N):N=n[9];let w;n[10]!==s||n[11]!==d||n[12]!==u||n[13]!==N?(w=h.jsx(lh,{graphData:s,onCopySuccess:u,onCopyError:d,extraActions:N}),n[10]=s,n[11]=d,n[12]=u,n[13]=N,n[14]=w):w=n[14];const C=s,S=ob[p];let O;n[15]===Symbol.for("react.memo_cache_sentinel")?(O={...uo,container:`${uo.container} ${Yt.jsonContainer}`,label:Yt.jsonLabel,stringValue:Yt.jsonString,numberValue:Yt.jsonNumber,booleanValue:Yt.jsonBoolean,nullValue:Yt.jsonNull},n[15]=O):O=n[15];let k;n[16]!==S||n[17]!==C?(k=h.jsx("div",{className:Yt.scrollBody,children:h.jsx(vr,{data:C,shouldExpandNode:S,style:O})}),n[16]=S,n[17]=C,n[18]=k):k=n[18];let H;return n[19]!==k||n[20]!==w?(H=h.jsxs("div",{className:Yt.root,children:[w,k]}),n[19]=k,n[20]=w,n[21]=H):H=n[21],H}const sb="_rightPanel_1ymj3_2",cb="_tabStrip_1ymj3_10",rb="_tab_1ymj3_10",ub="_tabActive_1ymj3_38",db="_tabBadge_1ymj3_42",fb="_tabBody_1ymj3_48",pb="_tabBodyHidden_1ymj3_57",hb="_rightPanelGroup_1ymj3_62",mb="_verticalResizeHandle_1ymj3_70",_t={rightPanel:sb,tabStrip:cb,tab:rb,tabActive:ub,tabBadge:db,tabBody:fb,tabBodyHidden:pb,rightPanelGroup:hb,verticalResizeHandle:mb},Rp="help-split-percent",lr="help-split-maximized",yb=45,gb=98;function vb({tabs:c,payload:n,onChange:s,validation:u,onFormat:d,onUpload:p,graphData:b,activeTab:y,onTabChange:m,onGraphRenderError:g,onGraphDataCopySuccess:A,onGraphDataCopyError:v,isGraphRefreshing:T,onClipNode:N,helpPanel:w}){const C=x.useId(),S=`${C}-tab-payload`,O=`${C}-tab-graph`,k=`${C}-tab-graph-data`,H=h.jsxs("div",{className:_t.rightPanel,children:[h.jsxs("div",{className:_t.tabStrip,role:"tablist","aria-label":"Right panel tabs",children:[c.includes("payload")&&h.jsx("button",{role:"tab","aria-selected":y==="payload","aria-controls":S,className:`${_t.tab}${y==="payload"?` ${_t.tabActive}`:""}`,onClick:()=>m("payload"),children:"Payload Editor"}),c.includes("graph")&&h.jsxs("button",{role:"tab","aria-selected":y==="graph","aria-controls":O,className:`${_t.tab}${y==="graph"?` ${_t.tabActive}`:""}`,onClick:()=>m("graph"),children:["Graph",b!==null&&h.jsx("span",{className:_t.tabBadge,"aria-label":"Graph data available",children:"🕸️"})]}),c.includes("graph-data")&&h.jsx("button",{role:"tab","aria-selected":y==="graph-data","aria-controls":k,className:`${_t.tab}${y==="graph-data"?` ${_t.tabActive}`:""}`,onClick:()=>m("graph-data"),children:"Graph Data (Raw)"})]}),c.includes("payload")&&h.jsx("div",{role:"tabpanel",id:S,tabIndex:y==="payload"?0:-1,className:`${_t.tabBody}${y!=="payload"?` ${_t.tabBodyHidden}`:""}`,children:h.jsx(o0,{payload:n,onChange:s,validation:u,onFormat:d,onUpload:p})}),c.includes("graph")&&h.jsx("div",{role:"tabpanel",id:O,tabIndex:y==="graph"?0:-1,className:`${_t.tabBody}${y!=="graph"?` ${_t.tabBodyHidden}`:""}`,children:h.jsx(Y0,{graphData:b,onRenderError:g,isRefreshing:T,onCopySuccess:A,onCopyError:v,onClipNode:N})}),c.includes("graph-data")&&h.jsx("div",{role:"tabpanel",id:k,tabIndex:y==="graph-data"?0:-1,className:`${_t.tabBody}${y!=="graph-data"?` ${_t.tabBodyHidden}`:""}`,children:h.jsx(ib,{graphData:b,onCopySuccess:A,onCopyError:v})})]}),L=x.useRef(Number(sessionStorage.getItem(Rp))||yb),Y=x.useRef(null),q=x.useRef(null),[B,J]=x.useState(()=>sessionStorage.getItem(lr)==="1"),K=x.useRef(B),I=x.useCallback(D=>{const V=D["help-split-help"];if(V===void 0)return;const F=V>=gb;F!==K.current&&(K.current=F,J(F),sessionStorage.setItem(lr,F?"1":"0")),F||(L.current=V,sessionStorage.setItem(Rp,String(V)))},[]),$=x.useCallback(()=>{var V,F,ee,ce;const D=!K.current;if(K.current=D,J(D),sessionStorage.setItem(lr,D?"1":"0"),D)(V=q.current)==null||V.resize("0%"),(F=Y.current)==null||F.resize("100%");else{const te=L.current;(ee=Y.current)==null||ee.resize(`${te}%`),(ce=q.current)==null||ce.resize(`${100-te}%`)}},[]),ne=!!w;if(x.useEffect(()=>{ne&&K.current&&requestAnimationFrame(()=>{var D,V;(D=q.current)==null||D.resize("0%"),(V=Y.current)==null||V.resize("100%")})},[ne]),!w)return H;const ue=typeof w=="function"?w($,B):w,ie=K.current?100:L.current,M=100-ie;return h.jsxs(Zp,{orientation:"vertical",className:_t.rightPanelGroup,onLayoutChanged:I,children:[h.jsx(ro,{panelRef:q,defaultSize:`${M}%`,minSize:"0%",children:H}),h.jsx(sr,{className:_t.verticalResizeHandle,"aria-label":"Resize help panel"}),h.jsx(ro,{id:"help-split-help",panelRef:Y,defaultSize:`${ie}%`,minSize:"15%",children:ue})]})}class bb extends Vp.Component{constructor(){super(...arguments),this.state={hasError:!1}}static getDerivedStateFromError(){return{hasError:!0}}componentDidCatch(n,s){console.error("[ConsoleErrorBoundary] Failed to render message:",n,s.componentStack)}render(){return this.state.hasError?h.jsx("span",{children:this.props.fallback}):this.props.children}}const _b=2e3,Sb=(c={})=>{const{onSuccess:n,onError:s}=c,[u,d]=x.useState(!1),p=x.useRef(null);return x.useEffect(()=>()=>{p.current!==null&&clearTimeout(p.current)},[]),{copy:x.useCallback(async y=>{if(!navigator.clipboard)return console.warn("useCopyToClipboard: Clipboard API not available in this browser."),s==null||s(),!1;try{return await navigator.clipboard.writeText(y),d(!0),p.current!==null&&clearTimeout(p.current),p.current=setTimeout(()=>{p.current=null,d(!1)},_b),n==null||n(),!0}catch(m){return console.error("useCopyToClipboard: Failed to write to clipboard.",m),s==null||s(),!1}},[n,s]),copied:u}},xb="_consoleRoot_trpoh_2",Tb="_consoleHeader_trpoh_10",Eb="_consoleTitle_trpoh_20",wb="_consoleControls_trpoh_25",Ab="_controlButton_trpoh_30",Nb="_console_trpoh_2",Cb="_emptyConsole_trpoh_67",jb="_consoleMessage_trpoh_80",Mb="_consoleMessageActivatable_trpoh_94",Db="_consoleMessageGraphLink_trpoh_104",Ob="_consoleMessageLargePayload_trpoh_115",zb="_consoleMessageMockUpload_trpoh_122",Rb="_uploadMockButton_trpoh_131",kb="_copyButton_trpoh_172",Bb="_copyButtonCopied_trpoh_225",Ub="_sendToJsonPathButton_trpoh_234",Hb="_messageIcon_trpoh_268",Gb="_messageContent_trpoh_272",Lb="_messageText_trpoh_278",qb="_messageTime_trpoh_283",Yb="_jsonViewWrapper_trpoh_295",Xb="_jsonContainer_trpoh_301",Jb="_jsonLabel_trpoh_302",Zb="_jsonString_trpoh_303",Qb="_jsonNumber_trpoh_304",Vb="_jsonBoolean_trpoh_305",Kb="_jsonNull_trpoh_306",Le={consoleRoot:xb,consoleHeader:Tb,consoleTitle:Eb,consoleControls:wb,controlButton:Ab,console:Nb,emptyConsole:Cb,consoleMessage:jb,consoleMessageActivatable:Mb,consoleMessageGraphLink:Db,consoleMessageLargePayload:Ob,consoleMessageMockUpload:zb,uploadMockButton:Rb,copyButton:kb,copyButtonCopied:Bb,sendToJsonPathButton:Ub,messageIcon:Hb,messageContent:Gb,messageText:Lb,messageTime:qb,"messageType-error":"_messageType-error_trpoh_290","messageType-info":"_messageType-info_trpoh_291","messageType-welcome":"_messageType-welcome_trpoh_292",jsonViewWrapper:Yb,jsonContainer:Xb,jsonLabel:Jb,jsonString:Zb,jsonNumber:Qb,jsonBoolean:Vb,jsonNull:Kb};function Ib(c){var sa;const n=Se.c(77),{message:s,msgId:u,classificationMap:d,onGraphLink:p,onCopyMessage:b,onSendToJsonPath:y,onUploadMockData:m,successfulUploadPaths:g}=c;let A,v,T;n[0]!==s?(v=vg(s),A=bg(v.type),T=fo(v.message),n[0]=s,n[1]=A,n[2]=v,n[3]=T):(A=n[1],v=n[2],T=n[3]);const N=T;let w,C,S,O,k,H;if(n[4]!==d||n[5]!==u||n[6]!==m||n[7]!==g){const ge=(u!==void 0?d==null?void 0:d.get(u):void 0)??[];C=ge.some(e2),S=ge.some(Pb),O=ge.some(Fb),k=((sa=ge.find(Wb))==null?void 0:sa.uploadPath)??null,w=!!m&&O&&k!==null,H=w&&!!(g!=null&&g.has(k)),n[4]=d,n[5]=u,n[6]=m,n[7]=g,n[8]=w,n[9]=C,n[10]=S,n[11]=O,n[12]=k,n[13]=H}else w=n[8],C=n[9],S=n[10],O=n[11],k=n[12],H=n[13];const L=H,Y=!!p&&C&&!O&&!S,q=!!y&&N.isJSON;let B;n[14]!==b?(B={onSuccess:b},n[14]=b,n[15]=B):B=n[15];const{copy:J,copied:K}=Sb(B);let I;n[16]!==J||n[17]!==s?(I=ge=>{ge.stopPropagation(),J(s)},n[16]=J,n[17]=s,n[18]=I):I=n[18];const $=I;let ne;n[19]!==J||n[20]!==s?(ne=ge=>{(ge.key==="Enter"||ge.key===" ")&&(ge.preventDefault(),ge.stopPropagation(),J(s))},n[19]=J,n[20]=s,n[21]=ne):ne=n[21];const ue=ne;let re;n[22]!==N.data||n[23]!==N.isJSON||n[24]!==y?(re=ge=>{if(ge.stopPropagation(),!y||!N.isJSON)return;const fn=JSON.stringify(N.data,null,2);y(fn)},n[22]=N.data,n[23]=N.isJSON,n[24]=y,n[25]=re):re=n[25];const ie=re;let M;n[26]!==k||n[27]!==m?(M=ge=>{ge.stopPropagation(),!(!m||!k)&&m(k)},n[26]=k,n[27]=m,n[28]=M):M=n[28];const D=M,V=Le[`messageType-${v.type}`],F=Y?Le.consoleMessageActivatable:"",ee=C?Le.consoleMessageGraphLink:"",ce=S?Le.consoleMessageLargePayload:"",te=O?Le.consoleMessageMockUpload:"";let P;n[29]!==ee||n[30]!==ce||n[31]!==te||n[32]!==V||n[33]!==F?(P=[Le.consoleMessage,V,F,ee,ce,te].filter(Boolean),n[29]=ee,n[30]=ce,n[31]=te,n[32]=V,n[33]=F,n[34]=P):P=n[34];const me=P.join(" ");let _e;n[35]!==Y||n[36]!==p?(_e=Y?()=>p():void 0,n[35]=Y,n[36]=p,n[37]=_e):_e=n[37];const xe=Y?"Click to load graph in Graph View":void 0,Be=Y?"button":void 0,qe=Y?0:void 0;let Me;n[38]!==Y||n[39]!==p?(Me=Y?ge=>{(ge.key==="Enter"||ge.key===" ")&&(ge.preventDefault(),p())}:void 0,n[38]=Y,n[39]=p,n[40]=Me):Me=n[40];const le=Y?"Load graph in Graph View":void 0,de=O?"⬆️":S?"⬇️":C?"🕸️":A;let pe;n[41]!==de?(pe=h.jsx("span",{className:Le.messageIcon,children:de}),n[41]=de,n[42]=pe):pe=n[42];let Re;n[43]!==N.data||n[44]!==N.isJSON||n[45]!==v.message||n[46]!==L?(Re=h.jsx("div",{className:Le.messageContent,children:N.isJSON?h.jsx("div",{className:Le.jsonViewWrapper,children:h.jsx(vr,{data:N.data,shouldExpandNode:$b,style:{...uo,container:`${uo.container} ${Le.jsonContainer}`,label:Le.jsonLabel,stringValue:Le.jsonString,numberValue:Le.jsonNumber,booleanValue:Le.jsonBoolean,nullValue:Le.jsonNull}})}):h.jsxs("span",{className:Le.messageText,children:[v.message,L&&h.jsx("span",{title:"Upload succeeded",children:" ✅"})]})}),n[43]=N.data,n[44]=N.isJSON,n[45]=v.message,n[46]=L,n[47]=Re):Re=n[47];const W=`${Le.copyButton} ${K?Le.copyButtonCopied:""}`,ye=K?"Copied!":"Copy message",Te=K?"Copied to clipboard":"Copy message to clipboard",he=K?"✅":"📄";let De;n[48]!==$||n[49]!==ue||n[50]!==W||n[51]!==ye||n[52]!==Te||n[53]!==he?(De=h.jsx("button",{className:W,onClick:$,onKeyDown:ue,title:ye,"aria-label":Te,tabIndex:0,children:he}),n[48]=$,n[49]=ue,n[50]=W,n[51]=ye,n[52]=Te,n[53]=he,n[54]=De):De=n[54];let Ue;n[55]!==q||n[56]!==ie?(Ue=q&&h.jsx("button",{className:Le.sendToJsonPathButton,onClick:ie,onKeyDown:ge=>{(ge.key==="Enter"||ge.key===" ")&&ie(ge)},title:"Open in JSON-Path Playground","aria-label":"Open this JSON in the JSON-Path Playground",tabIndex:0,children:"➡️"}),n[55]=q,n[56]=ie,n[57]=Ue):Ue=n[57];let dt;n[58]!==w||n[59]!==D?(dt=w&&h.jsx("button",{className:Le.uploadMockButton,onClick:D,onKeyDown:ge=>{(ge.key==="Enter"||ge.key===" ")&&D(ge)},title:"Re-open upload dialog","aria-label":"Re-open mock data upload dialog",tabIndex:0,children:"⬆️ Upload JSON…"}),n[58]=w,n[59]=D,n[60]=dt):dt=n[60];let Pe;n[61]!==v.time?(Pe=v.time&&h.jsx("span",{className:Le.messageTime,children:v.time}),n[61]=v.time,n[62]=Pe):Pe=n[62];let rt;return n[63]!==me||n[64]!==_e||n[65]!==xe||n[66]!==Be||n[67]!==qe||n[68]!==Me||n[69]!==le||n[70]!==pe||n[71]!==Re||n[72]!==De||n[73]!==Ue||n[74]!==dt||n[75]!==Pe?(rt=h.jsxs("div",{className:me,onClick:_e,title:xe,role:Be,tabIndex:qe,onKeyDown:Me,"aria-label":le,children:[pe,Re,De,Ue,dt,Pe]}),n[63]=me,n[64]=_e,n[65]=xe,n[66]=Be,n[67]=qe,n[68]=Me,n[69]=le,n[70]=pe,n[71]=Re,n[72]=De,n[73]=Ue,n[74]=dt,n[75]=Pe,n[76]=rt):rt=n[76],rt}function $b(c){return c<1}function Wb(c){return c.kind==="upload.invitation"}function Fb(c){return c.kind==="upload.invitation"}function Pb(c){return c.kind==="payload.large"}function e2(c){return c.kind==="graph.link"}function t2(c){const n=Se.c(32),{messages:s,classificationMap:u,onCopy:d,onClear:p,consoleRef:b,onGraphLinkMessage:y,onCopyMessage:m,onSendToJsonPath:g,onUploadMockData:A,successfulUploadPaths:v}=c;let T;n[0]===Symbol.for("react.memo_cache_sentinel")?(T=h.jsx("span",{className:Le.consoleTitle,children:"Console Output"}),n[0]=T):T=n[0];let N;n[1]!==d?(N=h.jsx("button",{className:Le.controlButton,onClick:d,title:"Copy console output","aria-label":"Copy console output to clipboard",children:"📑"}),n[1]=d,n[2]=N):N=n[2];let w;n[3]!==p?(w=h.jsx("button",{className:Le.controlButton,onClick:p,title:"Clear console","aria-label":"Clear console",children:"🗑️"}),n[3]=p,n[4]=w):w=n[4];let C;n[5]!==N||n[6]!==w?(C=h.jsxs("div",{className:Le.consoleHeader,children:[T,h.jsxs("div",{className:Le.consoleControls,children:[N,w]})]}),n[5]=N,n[6]=w,n[7]=C):C=n[7];let S;if(n[8]!==u||n[9]!==s||n[10]!==m||n[11]!==y||n[12]!==g||n[13]!==A||n[14]!==v){let L;n[16]!==u||n[17]!==m||n[18]!==y||n[19]!==g||n[20]!==A||n[21]!==v?(L=Y=>h.jsx(bb,{fallback:Y.raw,children:h.jsx(Ib,{message:Y.raw,msgId:Y.id,classificationMap:u,onGraphLink:y?()=>y(Y):void 0,onCopyMessage:m,onSendToJsonPath:g,onUploadMockData:A,successfulUploadPaths:v})},Y.id),n[16]=u,n[17]=m,n[18]=y,n[19]=g,n[20]=A,n[21]=v,n[22]=L):L=n[22],S=s.map(L),n[8]=u,n[9]=s,n[10]=m,n[11]=y,n[12]=g,n[13]=A,n[14]=v,n[15]=S}else S=n[15];let O;n[23]!==s.length?(O=s.length===0&&h.jsxs("div",{className:Le.emptyConsole,children:["No messages yet. Use the ",h.jsx("strong",{children:"Start"})," button in the header to connect."]}),n[23]=s.length,n[24]=O):O=n[24];let k;n[25]!==b||n[26]!==S||n[27]!==O?(k=h.jsxs("div",{className:Le.console,ref:b,role:"log","aria-live":"polite",children:[S,O]}),n[25]=b,n[26]=S,n[27]=O,n[28]=k):k=n[28];let H;return n[29]!==C||n[30]!==k?(H=h.jsxs("div",{className:Le.consoleRoot,children:[C,k]}),n[29]=C,n[30]=k,n[31]=H):H=n[31],H}const n2="_commandInput_o73qt_2",a2="_labelRow_o73qt_8",l2="_labelGroup_o73qt_16",o2="_label_o73qt_8",i2="_infoWrapper_o73qt_28",s2="_paletteToggle_o73qt_34",c2="_paletteToggleActive_o73qt_60",r2="_popover_o73qt_67",u2="_popoverOpen_o73qt_89",d2="_popoverTitle_o73qt_115",f2="_popoverRow_o73qt_129",p2="_popoverKeyword_o73qt_146",h2="_popoverDesc_o73qt_158",m2="_popoverAlias_o73qt_164",y2="_inputRow_o73qt_171",g2="_inputWrapper_o73qt_177",v2="_textarea_o73qt_187",b2="_sendButton_o73qt_216",_2="_hint_o73qt_233",S2="_dropup_o73qt_241",x2="_dropupHeader_o73qt_256",T2="_dropupItem_o73qt_272",E2="_dropupItemText_o73qt_295",w2="_matchHighlight_o73qt_303",A2="_multilineIndicator_o73qt_309",Ie={commandInput:n2,labelRow:a2,labelGroup:l2,label:o2,infoWrapper:i2,paletteToggle:s2,paletteToggleActive:c2,popover:r2,popoverOpen:u2,popoverTitle:d2,popoverRow:f2,popoverKeyword:p2,popoverDesc:h2,popoverAlias:m2,inputRow:y2,inputWrapper:g2,textarea:v2,sendButton:b2,hint:_2,dropup:S2,dropupHeader:x2,dropupItem:T2,dropupItemText:E2,matchHighlight:w2,multilineIndicator:A2},N2=[{keyword:"help",description:"List all help topics, or get help for a specific command",template:"help"},{keyword:"create",description:"Create a new graph node",template:`create node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:"update",description:"Update an existing node",template:`update node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:"edit",description:"Print raw node data ready for editing and re-submitting",template:"edit node {name}"},{keyword:"delete node",description:"Delete a node by name",alias:"clear node",template:"delete node {name}"},{keyword:"delete connection",description:"Delete connection(s) between two nodes",alias:"clear connection",template:"delete connection {nodeA} and {nodeB}"},{keyword:"delete cache",description:"Clear cached API fetcher results",alias:"clear cache",template:"delete cache"},{keyword:"connect",description:"Connect two nodes with a named relation",template:"connect {node-A} to {node-B} with {relation}"},{keyword:"list nodes",description:"List all nodes in the current graph",template:"list nodes"},{keyword:"list connections",description:"List all connections in the current graph",template:"list connections"},{keyword:"describe graph",description:"Describe the current graph model",template:"describe graph"},{keyword:"describe node",description:"Describe a specific node and its connections",template:"describe node {name}"},{keyword:"describe connection",description:"Describe connection(s) between two nodes",template:"describe connection {nodeA} and {nodeB}"},{keyword:"describe skill",description:"Show documentation for a skill by route name",template:"describe skill {skill.route}"},{keyword:"export",description:"Export the graph model to a JSON file",template:"export graph as {name}"},{keyword:"import graph",description:"Import a graph model from a saved file",template:"import graph from {name}"},{keyword:"import node",description:"Import a single node from another saved graph",template:"import node {node-name} from {graph-name}"},{keyword:"instantiate",description:"Create a runnable graph instance with mock input",alias:"start",template:`instantiate graph
{constant} -> input.body.{key}`,multiline:!0},{keyword:"upload mock data",description:"Print the URL to POST a JSON payload as mock input.body",template:"upload mock data"},{keyword:"execute",description:"Execute a single node skill in isolation",template:"execute node {name}"},{keyword:"inspect",description:"Inspect a state-machine variable",template:"inspect {variable_name}"},{keyword:"run",description:"Run the graph instance from root to end",template:"run"}];function C2(c,n){const s=Se.c(22),[u,d]=x.useState(!1),[p,b]=x.useState(-1);let y;if(s[0]!==n||s[1]!==c){e:{const L=n.trimStart();if(L.length===0){let K;s[3]===Symbol.for("react.memo_cache_sentinel")?(K=[],s[3]=K):K=s[3],y=K;break e}const Y=L.toLowerCase(),q=c.filter(K=>K.toLowerCase().startsWith(Y)),B=new Set;y=q.filter(K=>B.has(K)?!1:(B.add(K),!0)).slice(0,hg)}s[0]=n,s[1]=c,s[2]=y}else y=s[2];const m=y;let g;s[4]===Symbol.for("react.memo_cache_sentinel")?(g=()=>{d(!0),b(-1)},s[4]=g):g=s[4];const A=g;let v;s[5]!==m?(v=L=>{const Y=m.length;Y!==0&&b(q=>L===1?q<0?0:(q+1)%Y:q<=0?Y-1:q-1)},s[5]=m,s[6]=v):v=s[6];const T=v;let N;s[7]!==m?(N=(L,Y)=>{L>=0&&L<m.length&&Y(m[L]),d(!1),b(-1)},s[7]=m,s[8]=N):N=s[8];const w=N;let C;s[9]!==w||s[10]!==p||s[11]!==u||s[12]!==m?(C=L=>{if(!u||m.length===0)return;const Y=p>=0?p:0;w(Y,L)},s[9]=w,s[10]=p,s[11]=u,s[12]=m,s[13]=C):C=s[13];const S=C;let O;s[14]===Symbol.for("react.memo_cache_sentinel")?(O=()=>{d(!1),b(-1)},s[14]=O):O=s[14];const k=O;let H;return s[15]!==w||s[16]!==p||s[17]!==u||s[18]!==T||s[19]!==S||s[20]!==m?(H={suggestions:m,isOpen:u,activeIndex:p,onCommandChange:A,navigate:T,accept:w,onTab:S,dismiss:k},s[15]=w,s[16]=p,s[17]=u,s[18]=T,s[19]=S,s[20]=m,s[21]=H):H=s[21],H}const j2=c=>x.createElement("svg",{xmlns:"http://www.w3.org/2000/svg",viewBox:"0 0 16 16",fill:"none",width:14,height:14,stroke:"currentColor",strokeWidth:1.5,strokeLinecap:"round",strokeLinejoin:"round",...c},x.createElement("polyline",{points:"2,4 6,8 2,12"}),x.createElement("line",{x1:7,y1:12,x2:14,y2:12}));function M2(c){const n=Se.c(70),{command:s,onChange:u,onKeyDown:d,onSend:p,sendDisabled:b,disabled:y,history:m}=c,g=x.useRef(null),A=x.useRef(null),v=x.useRef(null),[T,N]=x.useState(!1);let w,C;n[0]!==T?(w=()=>{if(!T)return;const W=ye=>{A.current&&!A.current.contains(ye.target)&&N(!1)};return document.addEventListener("mousedown",W),()=>document.removeEventListener("mousedown",W)},C=[T],n[0]=T,n[1]=w,n[2]=C):(w=n[1],C=n[2]),x.useEffect(w,C);const S=C2(m,s);let O;n[3]===Symbol.for("react.memo_cache_sentinel")?(O=()=>{const W=g.current;W&&(W.style.height="auto",W.style.height=`${W.scrollHeight}px`)},n[3]=O):O=n[3];let k;n[4]!==s?(k=[s],n[4]=s,n[5]=k):k=n[5],x.useEffect(O,k);const H=y?"Not connected":"Enter command (Enter to send · Shift+Enter for new line)",L=y?"Enter your test message once it is connected":"Enter to send · Shift+Enter for new line · ↑↓ for history";let Y;n[6]!==S||n[7]!==u||n[8]!==d||n[9]!==p?(Y=W=>{var ye,Te;if(W.key==="Tab"){W.preventDefault(),S.isOpen&&S.suggestions.length>0&&(S.onTab(he=>u(he)),requestAnimationFrame(()=>{const he=g.current;he&&(he.selectionStart=he.selectionEnd=he.value.length)}));return}if(W.key==="Enter"){if(W.shiftKey)return;if(W.preventDefault(),S.isOpen&&S.activeIndex>=0){S.accept(S.activeIndex,he=>u(he)),requestAnimationFrame(()=>{const he=g.current;he&&(he.selectionStart=he.selectionEnd=he.value.length)}),(ye=g.current)==null||ye.focus();return}p(),(Te=g.current)==null||Te.focus();return}if(W.key==="Escape"){if(S.isOpen){S.dismiss(),W.preventDefault();return}return}if(W.key==="ArrowUp"||W.key==="ArrowDown"){if(S.isOpen&&S.suggestions.length>0){W.preventDefault(),S.navigate(W.key==="ArrowDown"?1:-1);return}const he=g.current;if(he){const{selectionStart:De,value:Ue}=he,Pe=!Ue.slice(0,De).includes(`
`),rt=!Ue.slice(De).includes(`
`);if(!(W.key==="ArrowUp"&&Pe||W.key==="ArrowDown"&&rt))return}d(W),requestAnimationFrame(()=>{const De=g.current;De&&(De.selectionStart=De.selectionEnd=De.value.length)});return}d(W)},n[6]=S,n[7]=u,n[8]=d,n[9]=p,n[10]=Y):Y=n[10];const q=Y;let B;n[11]===Symbol.for("react.memo_cache_sentinel")?(B=h.jsx("label",{htmlFor:"command",className:Ie.label,children:"Command"}),n[11]=B):B=n[11];const J=`${Ie.paletteToggle}${T?` ${Ie.paletteToggleActive}`:""}`;let K;n[12]===Symbol.for("react.memo_cache_sentinel")?(K=()=>N(z2),n[12]=K):K=n[12];let I;n[13]!==T?(I=W=>{var ye;if(W.key==="ArrowDown"&&T){W.preventDefault();const Te=(ye=v.current)==null?void 0:ye.querySelector('[role="option"]');Te==null||Te.focus()}},n[13]=T,n[14]=I):I=n[14];let $;n[15]===Symbol.for("react.memo_cache_sentinel")?($=h.jsx(j2,{"aria-hidden":"true",focusable:"false"}),n[15]=$):$=n[15];let ne;n[16]!==T||n[17]!==J||n[18]!==I?(ne=h.jsx("button",{type:"button",className:J,"aria-label":"Toggle command palette","aria-expanded":T,"aria-controls":"command-palette",onClick:K,onKeyDown:I,title:"Command palette",children:$}),n[16]=T,n[17]=J,n[18]=I,n[19]=ne):ne=n[19];const ue=`${Ie.popover}${T?` ${Ie.popoverOpen}`:""}`;let re,ie;n[20]===Symbol.for("react.memo_cache_sentinel")?(re=W=>{var ye,Te;if(W.key==="ArrowDown"||W.key==="ArrowUp"){W.preventDefault();const he=(ye=v.current)==null?void 0:ye.querySelectorAll('[role="option"]');if(!he||he.length===0)return;const De=Array.from(he).indexOf(document.activeElement);W.key==="ArrowDown"?he[De<0?0:(De+1)%he.length].focus():he[De<=0?he.length-1:De-1].focus()}else W.key==="Escape"&&(W.preventDefault(),N(!1),(Te=g.current)==null||Te.focus())},ie=h.jsx("p",{className:Ie.popoverTitle,children:"Command palette — click to insert"}),n[20]=re,n[21]=ie):(re=n[20],ie=n[21]);let M;n[22]!==T||n[23]!==u?(M=N2.map(W=>{const{keyword:ye,alias:Te,description:he,template:De}=W;return h.jsxs("div",{className:Ie.popoverRow,role:"option","aria-selected":!1,tabIndex:T?0:-1,onMouseDown:O2,onClick:()=>{var Ue;u(De),N(!1),(Ue=g.current)==null||Ue.focus()},onKeyDown:Ue=>{var dt;(Ue.key==="Enter"||Ue.key===" ")&&(Ue.preventDefault(),u(De),N(!1),(dt=g.current)==null||dt.focus())},children:[h.jsx("span",{className:Ie.popoverKeyword,children:ye}),h.jsxs("span",{className:Ie.popoverDesc,children:[he,Te&&h.jsxs("span",{className:Ie.popoverAlias,children:[" · alias: ",Te]})]})]},ye)}),n[22]=T,n[23]=u,n[24]=M):M=n[24];let D;n[25]!==ue||n[26]!==M?(D=h.jsxs("div",{id:"command-palette",ref:v,className:ue,role:"listbox","aria-label":"Command palette",onKeyDown:re,children:[ie,M]}),n[25]=ue,n[26]=M,n[27]=D):D=n[27];let V;n[28]!==ne||n[29]!==D?(V=h.jsx("div",{className:Ie.labelRow,children:h.jsxs("div",{className:Ie.labelGroup,children:[B,h.jsxs("span",{ref:A,className:Ie.infoWrapper,children:[ne,D]})]})}),n[28]=ne,n[29]=D,n[30]=V):V=n[30];const F=!(S.isOpen&&S.suggestions.length>0);let ee;n[31]===Symbol.for("react.memo_cache_sentinel")?(ee=h.jsx("div",{className:Ie.dropupHeader,"aria-hidden":"true",children:"Recent Commands"}),n[31]=ee):ee=n[31];let ce;n[32]!==S||n[33]!==s||n[34]!==u?(ce=S.isOpen&&S.suggestions.length>0&&S.suggestions.map((W,ye)=>{const Te=W.split(`
`)[0],he=W.includes(`
`),De=s.trimStart().split(`
`)[0],Ue=Math.min(De.length,Te.length),dt=Te.slice(0,Ue),Pe=Te.slice(Ue);return h.jsxs("div",{id:`history-option-${ye}`,role:"option","aria-selected":ye===S.activeIndex,className:Ie.dropupItem,onMouseDown:D2,onClick:()=>{S.accept(ye,rt=>u(rt)),requestAnimationFrame(()=>{const rt=g.current;rt&&(rt.selectionStart=rt.selectionEnd=rt.value.length)})},children:[h.jsxs("span",{className:Ie.dropupItemText,children:[Ue>0&&h.jsx("strong",{className:Ie.matchHighlight,children:dt}),Pe,he?"…":""]}),he&&h.jsx("span",{className:Ie.multilineIndicator,"aria-label":"multi-line command",children:"↵"})]},W)}),n[32]=S,n[33]=s,n[34]=u,n[35]=ce):ce=n[35];let te;n[36]!==F||n[37]!==ce?(te=h.jsxs("div",{id:"history-dropup",role:"listbox","aria-label":"Command history suggestions",className:Ie.dropup,hidden:F,children:[ee,ce]}),n[36]=F,n[37]=ce,n[38]=te):te=n[38];const P=S.isOpen&&S.suggestions.length>0,me=S.isOpen&&S.suggestions.length>0&&S.activeIndex>=0?`history-option-${S.activeIndex}`:void 0;let _e;n[39]!==S||n[40]!==u?(_e=W=>{u(W.target.value),S.onCommandChange()},n[39]=S,n[40]=u,n[41]=_e):_e=n[41];let xe;n[42]!==S?(xe=()=>S.dismiss(),n[42]=S,n[43]=xe):xe=n[43];let Be;n[44]!==s||n[45]!==y||n[46]!==q||n[47]!==H||n[48]!==P||n[49]!==me||n[50]!==_e||n[51]!==xe?(Be=h.jsx("textarea",{ref:g,id:"command",role:"combobox","aria-expanded":P,"aria-haspopup":"listbox","aria-controls":"history-dropup","aria-activedescendant":me,"aria-autocomplete":"list",className:Ie.textarea,rows:1,placeholder:H,value:s,disabled:y,onChange:_e,onKeyDown:q,onBlur:xe,autoComplete:"off",autoCorrect:"off",spellCheck:!1}),n[44]=s,n[45]=y,n[46]=q,n[47]=H,n[48]=P,n[49]=me,n[50]=_e,n[51]=xe,n[52]=Be):Be=n[52];let qe;n[53]!==te||n[54]!==Be?(qe=h.jsxs("div",{className:Ie.inputWrapper,children:[te,Be]}),n[53]=te,n[54]=Be,n[55]=qe):qe=n[55];let Me;n[56]!==p?(Me=()=>{var W;p(),(W=g.current)==null||W.focus()},n[56]=p,n[57]=Me):Me=n[57];let le;n[58]!==b||n[59]!==Me?(le=h.jsx("button",{className:Ie.sendButton,onClick:Me,disabled:b,"aria-label":"Send command",children:"Send"}),n[58]=b,n[59]=Me,n[60]=le):le=n[60];let de;n[61]!==qe||n[62]!==le?(de=h.jsxs("div",{className:Ie.inputRow,children:[qe,le]}),n[61]=qe,n[62]=le,n[63]=de):de=n[63];let pe;n[64]!==L?(pe=h.jsx("p",{className:Ie.hint,children:L}),n[64]=L,n[65]=pe):pe=n[65];let Re;return n[66]!==V||n[67]!==de||n[68]!==pe?(Re=h.jsxs("div",{className:Ie.commandInput,children:[V,de,pe]}),n[66]=V,n[67]=de,n[68]=pe,n[69]=Re):Re=n[69],Re}function D2(c){return c.preventDefault()}function O2(c){return c.preventDefault()}function z2(c){return!c}const R2="_root_1ac49_1",k2={root:R2};function B2(c){const n=Se.c(22),{messages:s,classificationMap:u,onCopy:d,onClear:p,consoleRef:b,onGraphLinkMessage:y,onCopyMessage:m,onSendToJsonPath:g,onUploadMockData:A,successfulUploadPaths:v,command:T,onCommandChange:N,onCommandKeyDown:w,onSend:C,sendDisabled:S,inputDisabled:O,commandHistory:k}=c;let H;n[0]!==u||n[1]!==b||n[2]!==s||n[3]!==p||n[4]!==d||n[5]!==m||n[6]!==y||n[7]!==g||n[8]!==A||n[9]!==v?(H=h.jsx(t2,{messages:s,classificationMap:u,onCopy:d,onClear:p,consoleRef:b,onGraphLinkMessage:y,onCopyMessage:m,onSendToJsonPath:g,onUploadMockData:A,successfulUploadPaths:v}),n[0]=u,n[1]=b,n[2]=s,n[3]=p,n[4]=d,n[5]=m,n[6]=y,n[7]=g,n[8]=A,n[9]=v,n[10]=H):H=n[10];let L;n[11]!==T||n[12]!==k||n[13]!==O||n[14]!==N||n[15]!==w||n[16]!==C||n[17]!==S?(L=h.jsx(M2,{command:T,onChange:N,onKeyDown:w,onSend:C,disabled:O,sendDisabled:S,history:k}),n[11]=T,n[12]=k,n[13]=O,n[14]=N,n[15]=w,n[16]=C,n[17]=S,n[18]=L):L=n[18];let Y;return n[19]!==H||n[20]!==L?(Y=h.jsxs("div",{className:k2.root,children:[H,L]}),n[19]=H,n[20]=L,n[21]=Y):Y=n[21],Y}const U2="_dialog_1ih1o_4",H2="_modalInner_1ih1o_26",G2="_modalHeader_1ih1o_34",L2="_modalTitleGroup_1ih1o_44",q2="_modalTitle_1ih1o_44",Y2="_modalPath_1ih1o_57",X2="_closeButton_1ih1o_64",J2="_modalBody_1ih1o_95",Z2="_dropZone_1ih1o_105",Q2="_dropZoneActive_1ih1o_127",V2="_dropZoneIcon_1ih1o_133",K2="_dropZoneText_1ih1o_139",I2="_dropZoneOr_1ih1o_152",$2="_browseButton_1ih1o_159",W2="_fileInputHidden_1ih1o_188",F2="_fileError_1ih1o_193",P2="_textareaLabel_1ih1o_198",e_="_textarea_1ih1o_198",t_="_validationError_1ih1o_226",n_="_keyboardHint_1ih1o_231",a_="_errorBanner_1ih1o_236",l_="_modalFooter_1ih1o_247",o_="_footerActions_1ih1o_257",i_="_formatButton_1ih1o_263",s_="_cancelButton_1ih1o_264",c_="_uploadButton_1ih1o_265",r_="_spinner_1ih1o_332",Qe={dialog:U2,modalInner:H2,modalHeader:G2,modalTitleGroup:L2,modalTitle:q2,modalPath:Y2,closeButton:X2,modalBody:J2,dropZone:Z2,dropZoneActive:Q2,dropZoneIcon:V2,dropZoneText:K2,dropZoneOr:I2,browseButton:$2,fileInputHidden:W2,fileError:F2,textareaLabel:P2,textarea:e_,validationError:t_,keyboardHint:n_,errorBanner:a_,modalFooter:l_,footerActions:o_,formatButton:i_,cancelButton:s_,uploadButton:c_,spinner:r_};function u_(c){const n=Se.c(9),{uploadPath:s,json:u,onSuccess:d,onError:p}=c,[b,y]=x.useState(!1),m=x.useRef(null);let g;n[0]===Symbol.for("react.memo_cache_sentinel")?(g=()=>{var w;(w=m.current)==null||w.abort(),m.current=null,y(!1)},n[0]=g):g=n[0];const A=g;let v;n[1]!==u||n[2]!==p||n[3]!==d||n[4]!==s?(v=async()=>{var C;(C=m.current)==null||C.abort();const w=new AbortController;m.current=w,y(!0);try{const S=await fetch(s,{method:"POST",headers:{"Content-Type":"application/json"},body:u,signal:w.signal}),O=await S.text();if(!S.ok){y(!1),p(`HTTP ${S.status} — ${O}`);return}y(!1),d(O)}catch(S){const O=S;if(O.name==="AbortError"){y(!1);return}y(!1),p(O.message??"Network error")}},n[1]=u,n[2]=p,n[3]=d,n[4]=s,n[5]=v):v=n[5];const T=v;let N;return n[6]!==b||n[7]!==T?(N={isUploading:b,upload:T,cancel:A},n[6]=b,n[7]=T,n[8]=N):N=n[8],N}var Jp;const d_=(((Jp=navigator.userAgentData)==null?void 0:Jp.platform)??navigator.platform).toLowerCase().includes("mac");function f_(c){return new Promise((n,s)=>{const u=new FileReader;u.onload=()=>n(u.result),u.onerror=()=>s(new Error(`Could not read file "${c.name}"`)),u.readAsText(c,"utf-8")})}function p_(c){const n=c.name.toLowerCase().endsWith(".json"),s=c.type==="application/json"||c.type==="text/plain";return!n&&!s?`"${c.name}" does not appear to be a JSON file. Only .json files are accepted.`:null}function h_({uploadPath:c,onSuccess:n,onClose:s,onError:u}){const[d,p]=x.useState(""),[b,y]=x.useState(null),[m,g]=x.useState(null),[A,v]=x.useState(!1),T=x.useRef(null),N=x.useRef(null),w=x.useRef(null),S=fo(d).isJSON,O=S&&d.trim()!=="",{isUploading:k,upload:H,cancel:L}=u_({uploadPath:c,json:d,onSuccess:n,onError:D=>{y(D),u(D)}});x.useEffect(()=>{var V;const D=T.current;if(D)return D.open||D.showModal(),(V=N.current)==null||V.focus(),()=>{D.open&&D.close()}},[]);const Y=x.useCallback(()=>{L(),s()},[L,s]),q=x.useCallback(D=>{D.target===T.current&&Y()},[Y]),B=x.useCallback(D=>{D.preventDefault(),Y()},[Y]),J=x.useCallback(()=>{y(null),H()},[H]),K=x.useCallback(D=>{D.key==="Enter"&&(D.ctrlKey||D.metaKey)&&(D.preventDefault(),O&&!k&&J())},[O,k,J]),I=x.useCallback(()=>{S&&p(cr(d))},[S,d]),$=x.useCallback(async D=>{var F;g(null),y(null);const V=p_(D);if(V){g(V);return}try{const ee=await f_(D);if(!fo(ee).isJSON){g(`"${D.name}" contains invalid JSON.`);return}p(cr(ee)),(F=N.current)==null||F.focus()}catch(ee){g(ee.message)}},[]),ne=x.useCallback(D=>{D.preventDefault(),D.stopPropagation(),A||v(!0)},[A]),ue=x.useCallback(D=>{D.preventDefault(),D.stopPropagation(),(D.currentTarget===D.target||!D.currentTarget.contains(D.relatedTarget))&&v(!1)},[]),re=x.useCallback(D=>{D.preventDefault(),D.stopPropagation(),v(!1);const V=D.dataTransfer.files[0];V&&$(V)},[$]),ie=x.useCallback(D=>{var F;const V=(F=D.target.files)==null?void 0:F[0];V&&($(V),D.target.value="")},[$]),M=!S&&d.trim()!=="";return h.jsx("dialog",{ref:T,className:Qe.dialog,"aria-modal":"true","aria-labelledby":"mock-upload-modal-title",onClick:q,onCancel:B,children:h.jsxs("div",{className:Qe.modalInner,onClick:D=>D.stopPropagation(),children:[h.jsxs("div",{className:Qe.modalHeader,children:[h.jsxs("div",{className:Qe.modalTitleGroup,children:[h.jsx("span",{id:"mock-upload-modal-title",className:Qe.modalTitle,children:"⬆️ Upload Mock Data"}),h.jsx("span",{className:Qe.modalPath,children:c})]}),h.jsx("button",{className:Qe.closeButton,onClick:Y,"aria-label":"Close upload modal",title:"Close",disabled:k,children:"✕"})]}),h.jsxs("div",{className:Qe.modalBody,children:[h.jsxs("div",{className:`${Qe.dropZone} ${A?Qe.dropZoneActive:""}`,onDragOver:ne,onDragLeave:ue,onDrop:re,"aria-label":"Drop a JSON file here",children:[h.jsx("span",{className:Qe.dropZoneIcon,children:"📂"}),h.jsxs("span",{className:Qe.dropZoneText,children:["Drop a ",h.jsx("code",{children:".json"})," file here"]}),h.jsx("span",{className:Qe.dropZoneOr,children:"— or —"}),h.jsx("input",{ref:w,type:"file",accept:".json,application/json",className:Qe.fileInputHidden,"aria-hidden":"true",tabIndex:-1,onChange:ie}),h.jsx("button",{type:"button",className:Qe.browseButton,onClick:()=>{var D;return(D=w.current)==null?void 0:D.click()},disabled:k,"aria-label":"Browse for a JSON file",children:"Browse file…"})]}),m&&h.jsxs("span",{className:Qe.fileError,role:"alert",children:["⚠️ ",m]}),h.jsx("label",{htmlFor:"mock-upload-textarea",className:Qe.textareaLabel,children:"JSON Payload"}),h.jsx("textarea",{id:"mock-upload-textarea",ref:N,className:Qe.textarea,value:d,onChange:D=>{p(D.target.value),g(null)},onKeyDown:K,placeholder:"Paste JSON here, or drop / browse a .json file above",rows:10,spellCheck:!1,"aria-describedby":M?"mock-upload-validation":void 0}),M&&h.jsx("span",{id:"mock-upload-validation",className:Qe.validationError,role:"status",children:"⚠️ Invalid JSON — check syntax"}),h.jsx("span",{className:Qe.keyboardHint,children:d_?"⌘+Enter to upload":"Ctrl+Enter to upload"}),b&&h.jsxs("div",{className:Qe.errorBanner,role:"alert",children:["❌ Upload failed: ",b]})]}),h.jsxs("div",{className:Qe.modalFooter,children:[h.jsx("button",{className:Qe.formatButton,onClick:I,disabled:!S||k,title:"Format JSON","aria-label":"Format JSON",children:"Format"}),h.jsxs("div",{className:Qe.footerActions,children:[h.jsx("button",{className:Qe.cancelButton,onClick:Y,disabled:k,children:"Cancel"}),h.jsx("button",{className:Qe.uploadButton,onClick:J,disabled:!O||k,"aria-busy":k,children:k?h.jsxs(h.Fragment,{children:[h.jsx("span",{className:Qe.spinner,"aria-hidden":"true"})," Uploading…"]}):"Upload ▶"})]})]})]})})}const fr=(c,n)=>n.some(s=>c instanceof s);let kp,Bp;function m_(){return kp||(kp=[IDBDatabase,IDBObjectStore,IDBIndex,IDBCursor,IDBTransaction])}function y_(){return Bp||(Bp=[IDBCursor.prototype.advance,IDBCursor.prototype.continue,IDBCursor.prototype.continuePrimaryKey])}const pr=new WeakMap,or=new WeakMap,zi=new WeakMap;function g_(c){const n=new Promise((s,u)=>{const d=()=>{c.removeEventListener("success",p),c.removeEventListener("error",b)},p=()=>{s(ia(c.result)),d()},b=()=>{u(c.error),d()};c.addEventListener("success",p),c.addEventListener("error",b)});return zi.set(n,c),n}function v_(c){if(pr.has(c))return;const n=new Promise((s,u)=>{const d=()=>{c.removeEventListener("complete",p),c.removeEventListener("error",b),c.removeEventListener("abort",b)},p=()=>{s(),d()},b=()=>{u(c.error||new DOMException("AbortError","AbortError")),d()};c.addEventListener("complete",p),c.addEventListener("error",b),c.addEventListener("abort",b)});pr.set(c,n)}let hr={get(c,n,s){if(c instanceof IDBTransaction){if(n==="done")return pr.get(c);if(n==="store")return s.objectStoreNames[1]?void 0:s.objectStore(s.objectStoreNames[0])}return ia(c[n])},set(c,n,s){return c[n]=s,!0},has(c,n){return c instanceof IDBTransaction&&(n==="done"||n==="store")?!0:n in c}};function oh(c){hr=c(hr)}function b_(c){return y_().includes(c)?function(...n){return c.apply(mr(this),n),ia(this.request)}:function(...n){return ia(c.apply(mr(this),n))}}function __(c){return typeof c=="function"?b_(c):(c instanceof IDBTransaction&&v_(c),fr(c,m_())?new Proxy(c,hr):c)}function ia(c){if(c instanceof IDBRequest)return g_(c);if(or.has(c))return or.get(c);const n=__(c);return n!==c&&(or.set(c,n),zi.set(n,c)),n}const mr=c=>zi.get(c);function S_(c,n,{blocked:s,upgrade:u,blocking:d,terminated:p}={}){const b=indexedDB.open(c,n),y=ia(b);return u&&b.addEventListener("upgradeneeded",m=>{u(ia(b.result),m.oldVersion,m.newVersion,ia(b.transaction),m)}),s&&b.addEventListener("blocked",m=>s(m.oldVersion,m.newVersion,m)),y.then(m=>{p&&m.addEventListener("close",()=>p()),d&&m.addEventListener("versionchange",g=>d(g.oldVersion,g.newVersion,g))}).catch(()=>{}),y}function x_(c,{blocked:n}={}){const s=indexedDB.deleteDatabase(c);return n&&s.addEventListener("blocked",u=>n(u.oldVersion,u)),ia(s).then(()=>{})}const T_=["get","getKey","getAll","getAllKeys","count"],E_=["put","add","delete","clear"],ir=new Map;function Up(c,n){if(!(c instanceof IDBDatabase&&!(n in c)&&typeof n=="string"))return;if(ir.get(n))return ir.get(n);const s=n.replace(/FromIndex$/,""),u=n!==s,d=E_.includes(s);if(!(s in(u?IDBIndex:IDBObjectStore).prototype)||!(d||T_.includes(s)))return;const p=async function(b,...y){const m=this.transaction(b,d?"readwrite":"readonly");let g=m.store;return u&&(g=g.index(y.shift())),(await Promise.all([g[s](...y),d&&m.done]))[0]};return ir.set(n,p),p}oh(c=>({...c,get:(n,s,u)=>Up(n,s)||c.get(n,s,u),has:(n,s)=>!!Up(n,s)||c.has(n,s)}));const w_=["continue","continuePrimaryKey","advance"],Hp={},yr=new WeakMap,ih=new WeakMap,A_={get(c,n){if(!w_.includes(n))return c[n];let s=Hp[n];return s||(s=Hp[n]=function(...u){yr.set(this,ih.get(this)[n](...u))}),s}};async function*N_(...c){let n=this;if(n instanceof IDBCursor||(n=await n.openCursor(...c)),!n)return;n=n;const s=new Proxy(n,A_);for(ih.set(s,n),zi.set(s,mr(n));n;)yield s,n=await(yr.get(s)||n.continue()),yr.delete(s)}function Gp(c,n){return n===Symbol.asyncIterator&&fr(c,[IDBIndex,IDBObjectStore,IDBCursor])||n==="iterate"&&fr(c,[IDBIndex,IDBObjectStore])}oh(c=>({...c,get(n,s,u){return Gp(n,s)?N_:c.get(n,s,u)},has(n,s){return Gp(n,s)||c.has(n,s)}}));const sh="minigraph-clipboard",C_=1,zn="items";let Di=null;function Lp(){return S_(sh,C_,{upgrade(c){c.objectStoreNames.contains(zn)&&c.deleteObjectStore(zn);const n=c.createObjectStore(zn,{keyPath:"id"});n.createIndex("by-alias","node.alias",{unique:!0}),n.createIndex("by-clippedAt","clippedAt")}})}function ml(){return Di||(Di=Lp().catch(async c=>(console.warn("[clipboard/db] openDB failed, deleting and recreating:",c),Di=null,await x_(sh),Lp()))),Di}async function j_(){return(await(await ml()).getAllFromIndex(zn,"by-clippedAt")).reverse()}async function qp(c){return(await ml()).getFromIndex(zn,"by-alias",c)}async function M_(c){await(await ml()).add(zn,c)}async function D_(c,n){const u=(await ml()).transaction(zn,"readwrite");await u.store.delete(c),await u.store.add(n),await u.done}async function O_(c){await(await ml()).delete(zn,c)}async function z_(){await(await ml()).clear(zn)}const R_="minigraph-clipboard-sync";function k_(){return new BroadcastChannel(R_)}function B_(c,n){switch(n.type){case"HYDRATE":return{items:n.items,isLoading:!1};case"ITEM_ADDED":return{...c,items:[n.item,...c.items]};case"ITEM_REPLACED":{const s=c.items.filter(u=>u.id!==n.previousId);return{...c,items:[n.item,...s]}}case"ITEM_REMOVED":return{...c,items:c.items.filter(s=>s.id!==n.id)};case"ITEMS_CLEARED":return{...c,items:[]};default:return c}}const ch=x.createContext(null);function U_({children:c}){const[n,s]=x.useReducer(B_,{items:[],isLoading:!0}),u=x.useRef(null);x.useEffect(()=>{j_().then(g=>s({type:"HYDRATE",items:g}))},[]),x.useEffect(()=>{let g;try{g=k_()}catch{return}return u.current=g,g.onmessage=A=>{const v=A.data;switch(v.type){case"item-added":s({type:"ITEM_ADDED",item:v.item});break;case"item-replaced":s({type:"ITEM_REPLACED",item:v.item,previousId:v.previousId});break;case"item-removed":s({type:"ITEM_REMOVED",id:v.id});break;case"items-cleared":s({type:"ITEMS_CLEARED"});break}},()=>{g.close(),u.current=null}},[]);const d=x.useCallback(g=>{var A;(A=u.current)==null||A.postMessage(g)},[]),p=x.useCallback(async(g,A,v)=>{try{const T={id:crypto.randomUUID(),clippedAt:new Date().toISOString(),sourceWsPath:v.sourceWsPath,sourceLabel:v.sourceLabel,node:g,connections:A},N=await qp(g.alias);if(N)return{status:"duplicate",existingItem:N,pendingItem:T};try{await M_(T)}catch(w){if(w instanceof DOMException&&w.name==="ConstraintError"){const C=await qp(g.alias);if(C)return{status:"duplicate",existingItem:C,pendingItem:T}}throw w}return s({type:"ITEM_ADDED",item:T}),d({type:"item-added",item:T}),{status:"added"}}catch(T){return{status:"error",message:T instanceof Error?T.message:String(T)}}},[d]),b=x.useCallback(async(g,A)=>{await D_(A,g),s({type:"ITEM_REPLACED",item:g,previousId:A}),d({type:"item-replaced",item:g,previousId:A})},[d]),y=x.useCallback(async g=>{await O_(g),s({type:"ITEM_REMOVED",id:g}),d({type:"item-removed",id:g})},[d]),m=x.useCallback(async()=>{await z_(),s({type:"ITEMS_CLEARED"}),d({type:"items-cleared"})},[d]);return h.jsx(ch.Provider,{value:{items:n.items,isLoading:n.isLoading,clipNode:p,confirmReplace:b,removeItem:y,clearAll:m},children:c})}function rh(){const c=x.useContext(ch);if(!c)throw new Error("useClipboardContext must be used inside <ClipboardProvider>");return c}function uh(c){const n=Date.now(),s=new Date(c).getTime(),u=n-s;if(u<0)return"just now";const d=Math.floor(u/1e3);if(d<60)return"just now";const p=Math.floor(d/60);if(p<60)return`${p} min ago`;const b=Math.floor(p/60);if(b<24)return`${b} hour${b>1?"s":""} ago`;const y=Math.floor(b/24);return y===1?"yesterday":y<30?`${y} days ago`:new Date(c).toLocaleDateString()}const H_="_card_474bo_2",G_="_alias_474bo_10",L_="_meta_474bo_18",q_="_propsLine_474bo_24",Y_="_timestamp_474bo_32",X_="_actions_474bo_39",J_="_pasteBtn_474bo_45",Z_="_inspectBtn_474bo_46",Q_="_removeBtn_474bo_47",Pt={card:H_,alias:G_,meta:L_,propsLine:q_,timestamp:Y_,actions:X_,pasteBtn:J_,inspectBtn:Z_,removeBtn:Q_};function V_(c){const n=Se.c(55),{item:s,connected:u,onPaste:d,onRemove:p,onInspect:b}=c,{node:y,connections:m,clippedAt:g,sourceLabel:A}=s,v=y.types[0]??"—",T=y.properties.skill??"—";let N;n[0]!==y.properties?(N=Object.entries(y.properties).filter(I_).map(K_),n[0]=y.properties,n[1]=N):N=n[1];const w=N,C=w.length>0?w.join(", "):"—";let S;if(n[2]!==m||n[3]!==y.alias){let P;n[5]!==y.alias?(P=me=>me.source===y.alias,n[5]=y.alias,n[6]=P):P=n[6],S=m.filter(P),n[2]=m,n[3]=y.alias,n[4]=S}else S=n[4];const O=S.length;let k;if(n[7]!==m||n[8]!==y.alias){let P;n[10]!==y.alias?(P=me=>me.target===y.alias,n[10]=y.alias,n[11]=P):P=n[11],k=m.filter(P),n[7]=m,n[8]=y.alias,n[9]=k}else k=n[9];const H=k.length,L=`${m.length} (${O} out, ${H} in)`;let Y;n[12]!==y.alias?(Y=h.jsx("div",{className:Pt.alias,children:y.alias}),n[12]=y.alias,n[13]=Y):Y=n[13];let q;n[14]!==v?(q=h.jsxs("div",{className:Pt.meta,children:["Type: ",v]}),n[14]=v,n[15]=q):q=n[15];let B;n[16]!==T?(B=h.jsxs("div",{className:Pt.meta,children:["Skill: ",T]}),n[16]=T,n[17]=B):B=n[17];let J;n[18]!==C?(J=h.jsxs("span",{className:Pt.propsLine,children:["Props: ",C]}),n[18]=C,n[19]=J):J=n[19];let K;n[20]!==C||n[21]!==J?(K=h.jsx("div",{className:Pt.meta,title:C,children:J}),n[20]=C,n[21]=J,n[22]=K):K=n[22];let I;n[23]!==L?(I=h.jsxs("div",{className:Pt.meta,children:["Connections: ",L]}),n[23]=L,n[24]=I):I=n[24];let $;n[25]!==g?($=uh(g),n[25]=g,n[26]=$):$=n[26];let ne;n[27]!==A||n[28]!==$?(ne=h.jsxs("div",{className:Pt.timestamp,children:["Clipped ",$," from ",A]}),n[27]=A,n[28]=$,n[29]=ne):ne=n[29];let ue;n[30]!==s||n[31]!==d?(ue=()=>d(s),n[30]=s,n[31]=d,n[32]=ue):ue=n[32];const re=!u,ie=`Paste node ${y.alias}`;let M;n[33]!==ue||n[34]!==re||n[35]!==ie?(M=h.jsx("button",{className:Pt.pasteBtn,onClick:ue,disabled:re,"aria-label":ie,children:"Paste"}),n[33]=ue,n[34]=re,n[35]=ie,n[36]=M):M=n[36];const D=`Inspect node ${y.alias}`;let V;n[37]!==b||n[38]!==D?(V=h.jsx("button",{className:Pt.inspectBtn,onClick:b,"aria-label":D,children:"Describe"}),n[37]=b,n[38]=D,n[39]=V):V=n[39];const F=`Remove node ${y.alias} from clipboard`;let ee;n[40]!==p||n[41]!==F?(ee=h.jsx("button",{className:Pt.removeBtn,onClick:p,"aria-label":F,children:"Remove"}),n[40]=p,n[41]=F,n[42]=ee):ee=n[42];let ce;n[43]!==M||n[44]!==V||n[45]!==ee?(ce=h.jsxs("div",{className:Pt.actions,children:[M,V,ee]}),n[43]=M,n[44]=V,n[45]=ee,n[46]=ce):ce=n[46];let te;return n[47]!==ne||n[48]!==ce||n[49]!==Y||n[50]!==q||n[51]!==B||n[52]!==K||n[53]!==I?(te=h.jsxs("div",{className:Pt.card,children:[Y,q,B,K,I,ne,ce]}),n[47]=ne,n[48]=ce,n[49]=Y,n[50]=q,n[51]=B,n[52]=K,n[53]=I,n[54]=te):te=n[54],te}function K_(c){const[n,s]=c,u=typeof s=="string"?s:JSON.stringify(s);return`${n}=${u&&u.length>30?u.slice(0,30)+"…":u}`}function I_(c){const[n]=c;return n!=="skill"}const $_="_sidebar_ol0sc_2",W_="_header_ol0sc_12",F_="_headerTitle_ol0sc_22",P_="_clearBtn_ol0sc_29",e3="_itemList_ol0sc_45",t3="_loading_ol0sc_55",n3="_emptyState_ol0sc_65",a3="_emptyIcon_ol0sc_78",l3="_emptyTitle_ol0sc_83",o3="_emptyHint_ol0sc_87",i3="_inspectPanel_ol0sc_93",s3="_inspectHeader_ol0sc_101",c3="_inspectClose_ol0sc_115",r3="_inspectBody_ol0sc_129",u3="_dialog_ol0sc_135",d3="_dialogTitle_ol0sc_150",f3="_dialogBody_ol0sc_157",p3="_dialogActions_ol0sc_164",h3="_cancelBtn_ol0sc_171",m3="_replaceBtn_ol0sc_185",lt={sidebar:$_,header:W_,headerTitle:F_,clearBtn:P_,itemList:e3,loading:t3,emptyState:n3,emptyIcon:a3,emptyTitle:l3,emptyHint:o3,inspectPanel:i3,inspectHeader:s3,inspectClose:c3,inspectBody:r3,dialog:u3,dialogTitle:d3,dialogBody:f3,dialogActions:p3,cancelBtn:h3,replaceBtn:m3};function y3(){const c=Se.c(1);let n;return c[0]===Symbol.for("react.memo_cache_sentinel")?(n=h.jsxs("div",{className:lt.emptyState,children:[h.jsx("span",{className:lt.emptyIcon,children:"📋"}),h.jsx("span",{className:lt.emptyTitle,children:"No items clipped yet."}),h.jsx("span",{className:lt.emptyHint,children:"Right-click a node in the Graph view to get started."})]}),c[0]=n):n=c[0],n}function g3(c){const n=Se.c(18),{connected:s,onPaste:u}=c,d=rh(),[p,b]=x.useState(null);let y;n[0]===Symbol.for("react.memo_cache_sentinel")?(y=h.jsx("span",{className:lt.headerTitle,children:"Clipboard"}),n[0]=y):y=n[0];let m;n[1]!==d?(m=d.items.length>0&&h.jsx("button",{className:lt.clearBtn,onClick:()=>d.clearAll(),"aria-label":"Clear all clipboard items",children:"Clear"}),n[1]=d,n[2]=m):m=n[2];let g;n[3]!==m?(g=h.jsxs("div",{className:lt.header,children:[y,m]}),n[3]=m,n[4]=g):g=n[4];let A;n[5]!==d||n[6]!==s||n[7]!==(p==null?void 0:p.id)||n[8]!==u?(A=d.isLoading?h.jsx("div",{className:lt.loading,children:"Loading…"}):d.items.length===0?h.jsx(y3,{}):d.items.map(w=>h.jsx(V_,{item:w,connected:s,onPaste:u,onRemove:()=>d.removeItem(w.id),onInspect:()=>b((p==null?void 0:p.id)===w.id?null:w)},w.id)),n[5]=d,n[6]=s,n[7]=p==null?void 0:p.id,n[8]=u,n[9]=A):A=n[9];let v;n[10]!==A?(v=h.jsx("div",{className:lt.itemList,children:A}),n[10]=A,n[11]=v):v=n[11];let T;n[12]!==p?(T=p&&h.jsxs("div",{className:lt.inspectPanel,children:[h.jsxs("div",{className:lt.inspectHeader,children:[h.jsxs("span",{children:["Describe node ",p.node.alias]}),h.jsx("button",{className:lt.inspectClose,onClick:()=>b(null),"aria-label":"Close inspect panel",children:"✕"})]}),h.jsx("div",{className:lt.inspectBody,children:h.jsx(vr,{data:{node:p.node,connections:p.connections},style:uo})})]}),n[12]=p,n[13]=T):T=n[13];let N;return n[14]!==g||n[15]!==v||n[16]!==T?(N=h.jsxs("div",{className:lt.sidebar,children:[g,v,T]}),n[14]=g,n[15]=v,n[16]=T,n[17]=N):N=n[17],N}const Yp=120,Xp=18,v3=180,b3=650;function _3(c){const{wheelTargetRef:n,scrollRef:s,contentWrapperRef:u,currentIndex:d,totalPages:p,onNavigatePrev:b,onNavigateNext:y}=c,m=x.useRef(0),g=x.useRef(null),A=x.useRef(!1),v=x.useRef(null),T=x.useRef(b),N=x.useRef(y),w=x.useRef(d),C=x.useRef(p);x.useEffect(()=>{T.current=b}),x.useEffect(()=>{N.current=y}),x.useEffect(()=>{w.current=d}),x.useEffect(()=>{C.current=p}),x.useEffect(()=>{v.current!==null&&(clearTimeout(v.current),v.current=null),u.current&&(u.current.style.transition="none",u.current.style.transform="translateY(0)"),m.current=0,g.current=null},[d]),x.useEffect(()=>{const S=n.current;if(!S)return;function O(){m.current=0,g.current=null,u.current&&(u.current.style.transition="transform 0.28s cubic-bezier(0.25, 0.46, 0.45, 0.94)",u.current.style.transform="translateY(0)")}function k(H){if(H.deltaY===0)return;const L=s.current;if(!L)return;const Y=L.scrollTop<=0,q=L.scrollTop+L.clientHeight>=L.scrollHeight-1,B=H.deltaY<0,J=H.deltaY>0,K=Y&&B,I=q&&J;if(!K&&!I){O();return}if(A.current)return;const $=w.current,ne=C.current;if(K&&$===0||I&&$===ne-1)return;const ue=K?"prev":"next";if(g.current!==null&&g.current!==ue&&O(),g.current=ue,m.current+=Math.abs(H.deltaY),u.current){const re=ue==="prev"?-1:1,ie=m.current*(Xp/Yp),M=Math.min(ie,Xp)*re;u.current.style.transition="none",u.current.style.transform=`translateY(${M}px)`}if(v.current!==null&&clearTimeout(v.current),v.current=setTimeout(O,v3),m.current>=Yp){v.current!==null&&clearTimeout(v.current);const re=g.current;O(),A.current=!0,re==="prev"?T.current():N.current(),setTimeout(()=>{A.current=!1},b3)}}return S.addEventListener("wheel",k,{passive:!0}),()=>{v.current!==null&&clearTimeout(v.current),S.removeEventListener("wheel",k)}},[])}const S3="_helpRoot_ss4ov_2",x3="_categoryNav_ss4ov_11",T3="_categoryTabScroller_ss4ov_21",E3="_categoryTab_ss4ov_21",w3="_categoryTabActive_ss4ov_71",A3="_maximizeButton_ss4ov_78",N3="_closeButton_ss4ov_100",C3="_helpBody_ss4ov_122",j3="_emptyFallback_ss4ov_130",M3="_helpContent_ss4ov_147",D3="_topicLink_ss4ov_226",O3="_helpBodyContent_ss4ov_271",z3="_chipStrip_ss4ov_276",R3="_topicChip_ss4ov_290",k3="_topicChipActive_ss4ov_318",Et={helpRoot:S3,categoryNav:x3,categoryTabScroller:T3,categoryTab:E3,categoryTabActive:w3,maximizeButton:A3,closeButton:N3,helpBody:C3,emptyFallback:j3,helpContent:M3,topicLink:D3,helpBodyContent:O3,chipStrip:z3,topicChip:R3,topicChipActive:k3};function gr(c){return typeof c=="string"?c:typeof c=="number"?String(c):Array.isArray(c)?c.map(gr).join(""):Vp.isValidElement(c)?gr(c.props.children):""}function B3(c){if(!c.trim().toLowerCase().startsWith("help "))return null;const u=c.trim().slice(5).replace(/\s*\(.*\)\s*$/,"").trim().toLowerCase();return u.length>0?u:null}function U3(c){const n=Se.c(49),{activeTopic:s,onNavigate:u,onClose:d,onToggleMaximize:p,isMaximized:b}=c,y=x.useRef(null),m=x.useRef(null),g=x.useRef(null),A=x.useRef(null);let v;n[0]===Symbol.for("react.memo_cache_sentinel")?(v=()=>{y.current&&(y.current.scrollTop=0)},n[0]=v):v=n[0];let T;n[1]!==s?(T=[s],n[1]=s,n[2]=T):T=n[2],x.useEffect(v,T);let N;n[3]===Symbol.for("react.memo_cache_sentinel")?(N=()=>{const te=A.current;if(!te)return;const P=te.querySelector('[aria-current="step"]');P&&P.scrollIntoView({block:"nearest",inline:"nearest",behavior:"smooth"})},n[3]=N):N=n[3];let w;n[4]!==s?(w=[s],n[4]=s,n[5]=w):w=n[5],x.useEffect(N,w);let C;n[6]!==s?(C=th(s),n[6]=s,n[7]=C):C=n[7];const S=C;let O;n[8]!==S?(O=rr(S),n[8]=S,n[9]=O):O=n[9];const k=O,H=k.length,L=hl.indexOf(s),Y=L<0?0:L,q=hl.length;let B,J;n[10]!==u||n[11]!==Y?(B=()=>u(hl[Y-1]??""),J=()=>u(hl[Y+1]??hl[hl.length-1]),n[10]=u,n[11]=Y,n[12]=B,n[13]=J):(B=n[12],J=n[13]);let K;n[14]!==Y||n[15]!==B||n[16]!==J?(K={wheelTargetRef:m,scrollRef:y,contentWrapperRef:g,currentIndex:Y,totalPages:q,onNavigatePrev:B,onNavigateNext:J},n[14]=Y,n[15]=B,n[16]=J,n[17]=K):K=n[17],_3(K);let I;n[18]!==s?(I=Oi(s),n[18]=s,n[19]=I):I=n[19];const $=I;let ne;n[20]!==u?(ne=te=>{const{children:P,...me}=te,_e=gr(P).trim(),xe=B3(_e);return xe!==null&&Oi(xe)!==null?h.jsx("li",{...me,children:h.jsx("button",{className:Et.topicLink,"aria-label":`Open help topic: ${xe}`,onClick:()=>u(xe),children:P})}):h.jsx("li",{...me,children:P})},n[20]=u,n[21]=ne):ne=n[21];const ue=ne;let re;n[22]!==S||n[23]!==u?(re=eh.map(te=>h.jsx("button",{className:[Et.categoryTab,te.id===S?Et.categoryTabActive:""].join(" ").trim(),"aria-current":te.id===S?"true":void 0,onClick:()=>{const P=rr(te.id);u(P[0]??"")},children:te.label},te.id)),n[22]=S,n[23]=u,n[24]=re):re=n[24];let ie;n[25]!==re?(ie=h.jsx("div",{className:Et.categoryTabScroller,children:re}),n[25]=re,n[26]=ie):ie=n[26];let M;n[27]!==b||n[28]!==p?(M=p&&h.jsx("button",{className:Et.maximizeButton,onClick:p,"aria-label":b?"Restore help panel":"Maximize help panel",children:b?"⊞":"⛶"}),n[27]=b,n[28]=p,n[29]=M):M=n[29];let D;n[30]!==d?(D=d&&h.jsx("button",{className:Et.closeButton,onClick:d,"aria-label":"Close help panel",children:"×"}),n[30]=d,n[31]=D):D=n[31];let V;n[32]!==ie||n[33]!==M||n[34]!==D?(V=h.jsxs("nav",{className:Et.categoryNav,"aria-label":"Help categories",children:[ie,M,D]}),n[32]=ie,n[33]=M,n[34]=D,n[35]=V):V=n[35];let F;n[36]!==s||n[37]!==k||n[38]!==H||n[39]!==u?(F=H>1&&h.jsx("div",{className:Et.chipStrip,ref:A,children:k.map(te=>{const P=te===s,me=te===""?"Overview":te;return h.jsx("button",{className:[Et.topicChip,P?Et.topicChipActive:""].join(" ").trim(),"aria-current":P?"step":void 0,onClick:()=>u(te),children:me},te)})}),n[36]=s,n[37]=k,n[38]=H,n[39]=u,n[40]=F):F=n[40];let ee;n[41]!==s||n[42]!==$||n[43]!==ue?(ee=h.jsx("div",{className:Et.helpBody,ref:y,children:h.jsx("div",{className:Et.helpBodyContent,ref:g,children:$===null?h.jsxs("div",{className:Et.emptyFallback,children:[h.jsxs("code",{children:["help ",s||""]}),"  not found in the local bundle."]}):h.jsx("div",{className:Et.helpContent,children:h.jsx(qy,{remarkPlugins:[Yy],components:s===""?{li:ue}:void 0,children:$})})})}),n[41]=s,n[42]=$,n[43]=ue,n[44]=ee):ee=n[44];let ce;return n[45]!==V||n[46]!==F||n[47]!==ee?(ce=h.jsxs("div",{className:Et.helpRoot,role:"region","aria-label":"Help browser",ref:m,children:[V,F,ee]}),n[45]=V,n[46]=F,n[47]=ee,n[48]=ce):ce=n[48],ce}function H3(c){const n=Se.c(22),{existingItem:s,pendingItem:u,onReplace:d,onCancel:p}=c,b=x.useRef(null);let y,m;n[0]===Symbol.for("react.memo_cache_sentinel")?(y=()=>{const k=b.current;k&&!k.open&&k.showModal()},m=[],n[0]=y,n[1]=m):(y=n[0],m=n[1]),x.useEffect(y,m);let g;n[2]===Symbol.for("react.memo_cache_sentinel")?(g=h.jsx("h2",{id:"duplicate-dialog-title",className:lt.dialogTitle,children:"Duplicate Node"}),n[2]=g):g=n[2];let A;n[3]!==u.node.alias?(A=h.jsxs("strong",{children:['"',u.node.alias,'"']}),n[3]=u.node.alias,n[4]=A):A=n[4];let v;n[5]!==s.clippedAt?(v=uh(s.clippedAt),n[5]=s.clippedAt,n[6]=v):v=n[6];let T;n[7]!==A||n[8]!==v?(T=h.jsxs("p",{className:lt.dialogBody,children:["A clipboard item with alias ",A," already exists (clipped ",v,")."]}),n[7]=A,n[8]=v,n[9]=T):T=n[9];let N;n[10]===Symbol.for("react.memo_cache_sentinel")?(N=h.jsx("p",{className:lt.dialogBody,children:"Replace it with the new snapshot?"}),n[10]=N):N=n[10];let w;n[11]!==p?(w=h.jsx("button",{className:lt.cancelBtn,onClick:p,children:"Cancel"}),n[11]=p,n[12]=w):w=n[12];let C;n[13]!==d?(C=h.jsx("button",{className:lt.replaceBtn,onClick:d,children:"Replace"}),n[13]=d,n[14]=C):C=n[14];let S;n[15]!==w||n[16]!==C?(S=h.jsxs("div",{className:lt.dialogActions,children:[w,C]}),n[15]=w,n[16]=C,n[17]=S):S=n[17];let O;return n[18]!==p||n[19]!==S||n[20]!==T?(O=h.jsxs("dialog",{ref:b,className:lt.dialog,onClose:p,"aria-labelledby":"duplicate-dialog-title",children:[g,T,N,S]}),n[18]=p,n[19]=S,n[20]=T,n[21]=O):O=n[21],O}function G3(c,n){if(!n)return null;const s=c.trim().toLowerCase();if(s!=="help"&&!s.startsWith("help "))return null;const u=nh(c);return Oi(u)!==null?u:null}class L3{constructor(){this.listeners=new Map}on(n,s){const u=n;return this.listeners.has(u)||this.listeners.set(u,new Set),this.listeners.get(u).add(s),()=>{var d;(d=this.listeners.get(u))==null||d.delete(s)}}emit(n){const s=this.listeners.get(n.kind);s&&s.forEach(u=>{try{u(n)}catch(d){console.error(`[ProtocolBus] listener for '${n.kind}' threw:`,d)}})}clear(){this.listeners.clear()}}const q3=new Set(["info","error","ping","welcome"]);function Y3(c,n){const s=[],u={msgId:c,raw:n};let d=!1,p=!1,b=!1,y=!1,m=!1;const g=fo(n);if(g.isJSON){const S=g.data;if(typeof S.type=="string"){const O=S.type;return s.push({...u,kind:"lifecycle",type:O,knownType:q3.has(O),message:typeof S.message=="string"?S.message:n,time:S.time??null}),s.length>0?s:[{...u,kind:"unclassified"}]}return s.push({...u,kind:"json.response",data:g.data}),s.length>0?s:[{...u,kind:"unclassified"}]}const A=xg(n);A&&(m=!0,s.push({...u,kind:"payload.large",apiPath:A.apiPath,byteSize:A.byteSize,filename:A.filename}));const v=Tg(n);v&&(b=!0,s.push({...u,kind:"upload.invitation",uploadPath:v}));const T=Fp(n);if(T&&(y=!0,s.push({...u,kind:"upload.contentPath",uploadPath:T})),Wp(n)){p=!0;const S=Sr(n);S&&s.push({...u,kind:"graph.link",apiPath:S})}if(p){const S=_g(n);S&&s.push({...u,kind:"graph.exported",graphName:S.graphName,apiPath:S.apiPath})}const N=Ag(n);N&&s.push({...u,kind:"graph.mutation",mutationType:N}),n.startsWith("> ")&&(d=!0,s.push({...u,kind:"command.echo",commandText:n.slice(2)})),Eg(n)&&s.push({...u,kind:"command.helpOrDescribe",commandText:n.slice(2)});const w=wg(n);w&&s.push({...u,kind:"command.importGraph",graphName:w});const C=Sg(n);return C&&s.push({...u,kind:"graph.export.failed",reason:C.reason}),!d&&!p&&!b&&!y&&!m&&_r(n)&&s.push({...u,kind:"docs.response",isMarkdown:!0}),s.length===0&&s.push({...u,kind:"unclassified"}),s}function X3(c){const n=Se.c(12),{messages:s,bus:u}=c,d=x.useRef(-1);let p;n[0]!==s?(p=()=>{s.length>0&&(d.current=s[s.length-1].id)},n[0]=s,n[1]=p):p=n[1];let b;n[2]===Symbol.for("react.memo_cache_sentinel")?(b=[],n[2]=b):b=n[2],x.useEffect(p,b);let y;if(n[3]!==s){y=new Map;for(const T of s)y.set(T.id,Y3(T.id,T.raw));n[3]=s,n[4]=y}else y=n[4];const m=y;let g,A;n[5]!==u||n[6]!==m||n[7]!==s?(g=()=>{if(s.length===0)return;const T=s.filter(N=>N.id>d.current);if(T.length!==0){d.current=s[s.length-1].id;for(const N of T){const w=m.get(N.id);if(w)for(const C of w)u.emit(C)}}},A=[s,u,m],n[5]=u,n[6]=m,n[7]=s,n[8]=g,n[9]=A):(g=n[8],A=n[9]),x.useEffect(g,A);let v;return n[10]!==m?(v={classificationMap:m},n[10]=m,n[11]=v):v=n[11],v}function J3({config:c}){const{title:n,wsPath:s,storageKeyPayload:u,storageKeyHistory:d,storageKeyTab:p,storageKeySavedGraphs:b,supportsUpload:y,supportsClipboard:m,supportsHelp:g,tabs:A}=c,v=Ny(),[T,N]=oa(u,""),w=br(),[C,S]=x.useState(()=>w.peekPendingPayload(s)),{takePendingPayload:O}=w;x.useEffect(()=>{const je=O(s);je!==null&&S(je)},[O,s]);const k=C??T,H=x.useCallback(je=>{S(null),N(je)},[N]),L=x.useMemo(()=>k?dg(k):{valid:!0,error:null,type:null},[k]),{toasts:Y,addToast:q,removeToast:B}=fg(),K=x.useRef(new L3).current,I=x.useCallback(je=>G3(je,g===!0)!==null,[g]),$=jg({wsPath:s,storageKeyHistory:d,payload:k,addToast:q,bus:K,handleLocalCommand:I}),{classificationMap:ne}=X3({messages:$.messages,bus:K}),[ue,re]=N1(s),{graphData:ie,setGraphData:M,rightTab:D,setRightTab:V,isRefreshing:F}=Og(ue,q,A[0],A,p),{modalUploadPath:ee,successfulUploadPaths:ce,handleOpenUploadModal:te,handleCloseUploadModal:P,handleUploadSuccess:me,handleUploadError:_e,resetSuccessfulPaths:xe}=b1({bus:K,addToast:q});zg({bus:K,pinnedGraphPath:ue,setPinnedGraphPath:re,connected:$.connected,sendRawText:$.sendRawText,addToast:q});const Be=x.useRef(!1);x.useEffect(()=>{Be.current&&!$.connected&&(re(null),M(null)),Be.current=$.connected},[$.connected,re,M]);const[qe,Me]=oa(c.storageKeyHelpTopic??"help-topic-fallback",""),[le,de]=oa("help-panel-open",!1),[pe,Re]=x.useState(()=>!!g&&!le),[W,ye]=x.useState(!1),Te=x.useRef(null),he=x.useCallback(()=>{pe&&(ye(!0),Te.current=setTimeout(()=>Re(!1),400))},[pe]);x.useEffect(()=>{if(!pe||W)return;const je=setTimeout(he,3e3);return()=>clearTimeout(je)},[pe,W,he]),x.useEffect(()=>{le&&pe&&he()},[le,pe,he]),x.useEffect(()=>()=>{Te.current&&clearTimeout(Te.current)},[]),x.useEffect(()=>{if(!g)return;const je=wt=>{wt.ctrlKey&&wt.key==="`"&&(wt.preventDefault(),de(et=>!et))};return window.addEventListener("keydown",je),()=>window.removeEventListener("keydown",je)},[g,de]),m1({bus:K,setHelpTopic:Me,onTabSwitch:g?()=>de(!0):()=>{}}),_1({bus:K,connected:$.connected,appendMessage:$.appendMessage,addToast:q});const De=rh(),[Ue,dt]=oa("clipboard-sidebar-open",!1),[Pe,rt]=x.useState(null),sa=x.useCallback(je=>{const et=(ie==null?void 0:ie.nodes.some(ja=>ja.alias===je.node.alias))??!1?"update":"create",Rn=C1(et,je.node);$.setCommand(Rn),q(`${et==="create"?"Create":"Update"} command for "${je.node.alias}" pasted to input`,"info")},[ie,$.setCommand,q]),ge=x.useCallback(async(je,wt)=>{try{const et=await De.clipNode(je,wt,{sourceWsPath:s,sourceLabel:c.label});switch(et.status){case"added":q(`Node "${je.alias}" clipped to clipboard`,"success");break;case"duplicate":rt({pendingItem:et.pendingItem,existingItem:et.existingItem});break;case"error":q(`Clip failed: ${et.message}`,"error");break}}catch(et){q(`Clip failed: ${et instanceof Error?et.message:String(et)}`,"error")}},[De,s,c.label,q]),fn=x1(b??""),{defaultName:po,setLastSavedName:ho,resetName:ca}=E1(b?`${b}-untitled-counter`:"untitled-counter",K),{handleSaveGraph:Ri,handleLoadGraph:mo}=A1({bus:K,connected:$.connected,sendRawText:$.sendRawText,saveGraph:fn.saveGraph,setLastSavedName:ho,addToast:q}),ki=x.useCallback(je=>{const wt=ne.get(je.id),et=wt==null?void 0:wt.find(Rn=>Rn.kind==="graph.link");et&&re(et.apiPath)},[ne]),{handleSendToJsonPath:Bi}=y1({ctx:w,navigate:v,addToast:q,wsPath:s}),ra=Dg("(max-width: 768px)"),{defaultLayout:St,onLayoutChanged:ln}=Ey({id:c.path+"-panel-split",storage:localStorage}),xt=x.useCallback(()=>H(cr(k)),[k]),Ui=x.useCallback(()=>{$.clearMessages(),re(null),M(null),xe(),ca()},[$.clearMessages,M,xe,ca]);return h.jsxs("div",{className:zt.wrapper,children:[h.jsx(U1,{toasts:Y,onRemove:B}),ee&&h.jsx(h_,{uploadPath:ee,onSuccess:me,onClose:P,onError:_e}),h.jsxs("header",{className:zt.header,children:[h.jsx("h1",{className:zt.title,children:n}),h.jsxs("div",{className:zt.headerActions,children:[b&&h.jsx(wv,{disabled:!ie,defaultName:po,onSave:Ri,nameExists:fn.hasGraph,connected:$.connected}),b&&fn.savedGraphs.length>0&&h.jsx(Bv,{savedGraphs:fn.savedGraphs,onLoad:mo,onDelete:fn.deleteGraph,connected:$.connected}),m&&h.jsxs("button",{className:zt.clipboardToggle,onClick:()=>dt(je=>!je),"aria-label":Ue?"Close clipboard sidebar":"Open clipboard sidebar","aria-pressed":Ue,children:["Clipboard",De.items.length>0?` (${De.items.length})`:""]}),h.jsx(hv,{addToast:q}),g&&h.jsxs("div",{className:zt.helpButtonWrapper,children:[h.jsx("button",{className:`${zt.helpToggle}${pe&&!W?` ${zt.helpTogglePulsing}`:""}`,onClick:()=>de(je=>!je),"aria-label":le?"Close help panel":"Open help panel","aria-pressed":le,children:"?"}),pe&&h.jsxs("div",{className:`${zt.helpHint}${W?` ${zt.helpHintFading}`:""}`,onClick:he,role:"status",children:[h.jsx("kbd",{className:zt.helpHintKbd,children:"Ctrl + `"})," to toggle help"]})]})]})]}),Pe&&h.jsx(H3,{existingItem:Pe.existingItem,pendingItem:Pe.pendingItem,onReplace:async()=>{try{await De.confirmReplace(Pe.pendingItem,Pe.existingItem.id),rt(null),q(`Clipboard item "${Pe.pendingItem.node.alias}" replaced`,"success")}catch(je){q(`Replace failed: ${je instanceof Error?je.message:String(je)}`,"error")}},onCancel:()=>{rt(null),q("Clip cancelled","info")}}),h.jsxs(Zp,{className:zt.panelGroup,orientation:ra?"vertical":"horizontal",defaultLayout:St,onLayoutChanged:ln,children:[h.jsx(ro,{defaultSize:le||Ue?"50%":"60%",minSize:"25%",children:h.jsx(B2,{messages:$.messages,classificationMap:ne,onCopy:$.copyMessages,onClear:Ui,consoleRef:$.consoleRef,command:$.command,onCommandChange:$.setCommand,onCommandKeyDown:$.handleKeyDown,onSend:$.sendCommand,sendDisabled:!$.connected||!$.command.trim(),inputDisabled:!$.connected,commandHistory:$.history,onGraphLinkMessage:ki,onCopyMessage:()=>q("Copied to clipboard","success"),onSendToJsonPath:Bi,onUploadMockData:te,successfulUploadPaths:ce})}),h.jsx(sr,{className:zt.resizeHandle,"aria-label":"Resize panels"}),h.jsx(ro,{defaultSize:le?"50%":Ue?"30%":"40%",minSize:"20%",children:h.jsx(vb,{tabs:A,payload:k,onChange:H,validation:L,onFormat:xt,onUpload:y?$.uploadPayload:void 0,graphData:ie,activeTab:D,onTabChange:V,onGraphRenderError:je=>q(je,"error"),onGraphDataCopySuccess:()=>q("Graph JSON copied to clipboard!","success"),onGraphDataCopyError:()=>q("Copy failed","error"),isGraphRefreshing:F,onClipNode:m?ge:void 0,helpPanel:g&&le?(je,wt)=>h.jsx(U3,{activeTopic:qe,onNavigate:Me,onClose:()=>de(!1),onToggleMaximize:je,isMaximized:wt}):void 0})}),m&&Ue&&h.jsxs(h.Fragment,{children:[h.jsx(sr,{className:zt.resizeHandle,"aria-label":"Resize clipboard"}),h.jsx(ro,{defaultSize:"20%",minSize:"10%",maxSize:"40%",children:h.jsx(g3,{connected:$.connected,onPaste:sa})})]})]})]})}function Z3(){const c=Se.c(2),n=On[0].path;let s;c[0]===Symbol.for("react.memo_cache_sentinel")?(s=On.map(Q3),c[0]=s):s=c[0];let u;return c[1]===Symbol.for("react.memo_cache_sentinel")?(u=h.jsx(gg,{children:h.jsx(U_,{children:h.jsx(Cy,{children:h.jsxs(jy,{children:[s,h.jsx(Kp,{path:"*",element:h.jsx(My,{to:n,replace:!0})})]})})})}),c[1]=u):u=c[1],u}function Q3(c){return h.jsx(Kp,{path:c.path,element:h.jsx(J3,{config:c},c.path)},c.path)}Vy.createRoot(document.getElementById("root")).render(h.jsx(x.StrictMode,{children:h.jsx(Z3,{})}));
//# sourceMappingURL=index-pbrJ9eWn.js.map
