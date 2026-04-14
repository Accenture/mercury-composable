import{j as h,T as Zf,_ as co,H as sc,W as wy}from"./vendor-panels-Cixz1HBJ.js";import{a as Qf,b as Ey,r as x,N as Ny,R as Vf,u as Ay,B as Cy,c as jy,d as If,e as My}from"./vendor-router-DUFbnzxw.js";import{H as bf,P as _f,N as Dy,u as Oy,a as zy,B as ky,b as Ry,C as By,M as Uy,i as Gy}from"./vendor-xyflow-Bnghg68c.js";import{c as Hy,a as Ly,d as uo,J as vc}from"./vendor-json-view-Djmwb-hd.js";import{M as qy,r as Yy}from"./vendor-markdown-Cp1IxVgw.js";(function(){const n=document.createElement("link").relList;if(n&&n.supports&&n.supports("modulepreload"))return;for(const d of document.querySelectorAll('link[rel="modulepreload"]'))u(d);new MutationObserver(d=>{for(const f of d)if(f.type==="childList")for(const b of f.addedNodes)b.tagName==="LINK"&&b.rel==="modulepreload"&&u(b)}).observe(document,{childList:!0,subtree:!0});function s(d){const f={};return d.integrity&&(f.integrity=d.integrity),d.referrerPolicy&&(f.referrerPolicy=d.referrerPolicy),d.crossOrigin==="use-credentials"?f.credentials="include":d.crossOrigin==="anonymous"?f.credentials="omit":f.credentials="same-origin",f}function u(d){if(d.ep)return;d.ep=!0;const f=s(d);fetch(d.href,f)}})();var $r={exports:{}},so={},Wr={exports:{}},Pr={};/**
 * @license React
 * scheduler.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Sf;function Xy(){return Sf||(Sf=1,(function(r){function n(M,D){var V=M.length;M.push(D);e:for(;0<V;){var P=V-1>>>1,ee=M[P];if(0<d(ee,D))M[P]=D,M[V]=ee,V=P;else break e}}function s(M){return M.length===0?null:M[0]}function u(M){if(M.length===0)return null;var D=M[0],V=M.pop();if(V!==D){M[0]=V;e:for(var P=0,ee=M.length,re=ee>>>1;P<re;){var te=2*(P+1)-1,F=M[te],me=te+1,_e=M[me];if(0>d(F,V))me<ee&&0>d(_e,F)?(M[P]=_e,M[me]=V,P=me):(M[P]=F,M[te]=V,P=te);else if(me<ee&&0>d(_e,V))M[P]=_e,M[me]=V,P=me;else break e}}return D}function d(M,D){var V=M.sortIndex-D.sortIndex;return V!==0?V:M.id-D.id}if(r.unstable_now=void 0,typeof performance=="object"&&typeof performance.now=="function"){var f=performance;r.unstable_now=function(){return f.now()}}else{var b=Date,y=b.now();r.unstable_now=function(){return b.now()-y}}var m=[],g=[],N=1,v=null,T=3,A=!1,E=!1,C=!1,S=!1,O=typeof setTimeout=="function"?setTimeout:null,R=typeof clearTimeout=="function"?clearTimeout:null,G=typeof setImmediate<"u"?setImmediate:null;function L(M){for(var D=s(g);D!==null;){if(D.callback===null)u(g);else if(D.startTime<=M)u(g),D.sortIndex=D.expirationTime,n(m,D);else break;D=s(g)}}function Y(M){if(C=!1,L(M),!E)if(s(m)!==null)E=!0,q||(q=!0,ne());else{var D=s(g);D!==null&&ie(Y,D.startTime-M)}}var q=!1,B=-1,J=5,I=-1;function K(){return S?!0:!(r.unstable_now()-I<J)}function $(){if(S=!1,q){var M=r.unstable_now();I=M;var D=!0;try{e:{E=!1,C&&(C=!1,R(B),B=-1),A=!0;var V=T;try{t:{for(L(M),v=s(m);v!==null&&!(v.expirationTime>M&&K());){var P=v.callback;if(typeof P=="function"){v.callback=null,T=v.priorityLevel;var ee=P(v.expirationTime<=M);if(M=r.unstable_now(),typeof ee=="function"){v.callback=ee,L(M),D=!0;break t}v===s(m)&&u(m),L(M)}else u(m);v=s(m)}if(v!==null)D=!0;else{var re=s(g);re!==null&&ie(Y,re.startTime-M),D=!1}}break e}finally{v=null,T=V,A=!1}D=void 0}}finally{D?ne():q=!1}}}var ne;if(typeof G=="function")ne=function(){G($)};else if(typeof MessageChannel<"u"){var ue=new MessageChannel,ce=ue.port2;ue.port1.onmessage=$,ne=function(){ce.postMessage(null)}}else ne=function(){O($,0)};function ie(M,D){B=O(function(){M(r.unstable_now())},D)}r.unstable_IdlePriority=5,r.unstable_ImmediatePriority=1,r.unstable_LowPriority=4,r.unstable_NormalPriority=3,r.unstable_Profiling=null,r.unstable_UserBlockingPriority=2,r.unstable_cancelCallback=function(M){M.callback=null},r.unstable_forceFrameRate=function(M){0>M||125<M?console.error("forceFrameRate takes a positive int between 0 and 125, forcing frame rates higher than 125 fps is not supported"):J=0<M?Math.floor(1e3/M):5},r.unstable_getCurrentPriorityLevel=function(){return T},r.unstable_next=function(M){switch(T){case 1:case 2:case 3:var D=3;break;default:D=T}var V=T;T=D;try{return M()}finally{T=V}},r.unstable_requestPaint=function(){S=!0},r.unstable_runWithPriority=function(M,D){switch(M){case 1:case 2:case 3:case 4:case 5:break;default:M=3}var V=T;T=M;try{return D()}finally{T=V}},r.unstable_scheduleCallback=function(M,D,V){var P=r.unstable_now();switch(typeof V=="object"&&V!==null?(V=V.delay,V=typeof V=="number"&&0<V?P+V:P):V=P,M){case 1:var ee=-1;break;case 2:ee=250;break;case 5:ee=1073741823;break;case 4:ee=1e4;break;default:ee=5e3}return ee=V+ee,M={id:N++,callback:D,priorityLevel:M,startTime:V,expirationTime:ee,sortIndex:-1},V>P?(M.sortIndex=V,n(g,M),s(m)===null&&M===s(g)&&(C?(R(B),B=-1):C=!0,ie(Y,V-P))):(M.sortIndex=ee,n(m,M),E||A||(E=!0,q||(q=!0,ne()))),M},r.unstable_shouldYield=K,r.unstable_wrapCallback=function(M){var D=T;return function(){var V=T;T=D;try{return M.apply(this,arguments)}finally{T=V}}}})(Pr)),Pr}var xf;function Jy(){return xf||(xf=1,Wr.exports=Xy()),Wr.exports}/**
 * @license React
 * react-dom-client.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Tf;function Zy(){if(Tf)return so;Tf=1;var r=Jy(),n=Qf(),s=Ey();function u(e){var t="https://react.dev/errors/"+e;if(1<arguments.length){t+="?args[]="+encodeURIComponent(arguments[1]);for(var a=2;a<arguments.length;a++)t+="&args[]="+encodeURIComponent(arguments[a])}return"Minified React error #"+e+"; visit "+t+" for the full message or use the non-minified dev environment for full errors and additional helpful warnings."}function d(e){return!(!e||e.nodeType!==1&&e.nodeType!==9&&e.nodeType!==11)}function f(e){var t=e,a=e;if(e.alternate)for(;t.return;)t=t.return;else{e=t;do t=e,(t.flags&4098)!==0&&(a=t.return),e=t.return;while(e)}return t.tag===3?a:null}function b(e){if(e.tag===13){var t=e.memoizedState;if(t===null&&(e=e.alternate,e!==null&&(t=e.memoizedState)),t!==null)return t.dehydrated}return null}function y(e){if(e.tag===31){var t=e.memoizedState;if(t===null&&(e=e.alternate,e!==null&&(t=e.memoizedState)),t!==null)return t.dehydrated}return null}function m(e){if(f(e)!==e)throw Error(u(188))}function g(e){var t=e.alternate;if(!t){if(t=f(e),t===null)throw Error(u(188));return t!==e?null:e}for(var a=e,l=t;;){var o=a.return;if(o===null)break;var i=o.alternate;if(i===null){if(l=o.return,l!==null){a=l;continue}break}if(o.child===i.child){for(i=o.child;i;){if(i===a)return m(o),e;if(i===l)return m(o),t;i=i.sibling}throw Error(u(188))}if(a.return!==l.return)a=o,l=i;else{for(var c=!1,p=o.child;p;){if(p===a){c=!0,a=o,l=i;break}if(p===l){c=!0,l=o,a=i;break}p=p.sibling}if(!c){for(p=i.child;p;){if(p===a){c=!0,a=i,l=o;break}if(p===l){c=!0,l=i,a=o;break}p=p.sibling}if(!c)throw Error(u(189))}}if(a.alternate!==l)throw Error(u(190))}if(a.tag!==3)throw Error(u(188));return a.stateNode.current===a?e:t}function N(e){var t=e.tag;if(t===5||t===26||t===27||t===6)return e;for(e=e.child;e!==null;){if(t=N(e),t!==null)return t;e=e.sibling}return null}var v=Object.assign,T=Symbol.for("react.element"),A=Symbol.for("react.transitional.element"),E=Symbol.for("react.portal"),C=Symbol.for("react.fragment"),S=Symbol.for("react.strict_mode"),O=Symbol.for("react.profiler"),R=Symbol.for("react.consumer"),G=Symbol.for("react.context"),L=Symbol.for("react.forward_ref"),Y=Symbol.for("react.suspense"),q=Symbol.for("react.suspense_list"),B=Symbol.for("react.memo"),J=Symbol.for("react.lazy"),I=Symbol.for("react.activity"),K=Symbol.for("react.memo_cache_sentinel"),$=Symbol.iterator;function ne(e){return e===null||typeof e!="object"?null:(e=$&&e[$]||e["@@iterator"],typeof e=="function"?e:null)}var ue=Symbol.for("react.client.reference");function ce(e){if(e==null)return null;if(typeof e=="function")return e.$$typeof===ue?null:e.displayName||e.name||null;if(typeof e=="string")return e;switch(e){case C:return"Fragment";case O:return"Profiler";case S:return"StrictMode";case Y:return"Suspense";case q:return"SuspenseList";case I:return"Activity"}if(typeof e=="object")switch(e.$$typeof){case E:return"Portal";case G:return e.displayName||"Context";case R:return(e._context.displayName||"Context")+".Consumer";case L:var t=e.render;return e=e.displayName,e||(e=t.displayName||t.name||"",e=e!==""?"ForwardRef("+e+")":"ForwardRef"),e;case B:return t=e.displayName||null,t!==null?t:ce(e.type)||"Memo";case J:t=e._payload,e=e._init;try{return ce(e(t))}catch{}}return null}var ie=Array.isArray,M=n.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,D=s.__DOM_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,V={pending:!1,data:null,method:null,action:null},P=[],ee=-1;function re(e){return{current:e}}function te(e){0>ee||(e.current=P[ee],P[ee]=null,ee--)}function F(e,t){ee++,P[ee]=e.current,e.current=t}var me=re(null),_e=re(null),xe=re(null),Be=re(null);function qe(e,t){switch(F(xe,t),F(_e,e),F(me,null),t.nodeType){case 9:case 11:e=(e=t.documentElement)&&(e=e.namespaceURI)?qp(e):0;break;default:if(e=t.tagName,t=t.namespaceURI)t=qp(t),e=Yp(t,e);else switch(e){case"svg":e=1;break;case"math":e=2;break;default:e=0}}te(me),F(me,e)}function Me(){te(me),te(_e),te(xe)}function le(e){e.memoizedState!==null&&F(Be,e);var t=me.current,a=Yp(t,e.type);t!==a&&(F(_e,e),F(me,a))}function de(e){_e.current===e&&(te(me),te(_e)),Be.current===e&&(te(Be),ao._currentValue=V)}var fe,ke;function W(e){if(fe===void 0)try{throw Error()}catch(a){var t=a.stack.trim().match(/\n( *(at )?)/);fe=t&&t[1]||"",ke=-1<a.stack.indexOf(`
    at`)?" (<anonymous>)":-1<a.stack.indexOf("@")?"@unknown:0:0":""}return`
`+fe+e+ke}var ye=!1;function Te(e,t){if(!e||ye)return"";ye=!0;var a=Error.prepareStackTrace;Error.prepareStackTrace=void 0;try{var l={DetermineComponentFrameRoot:function(){try{if(t){var Q=function(){throw Error()};if(Object.defineProperty(Q.prototype,"props",{set:function(){throw Error()}}),typeof Reflect=="object"&&Reflect.construct){try{Reflect.construct(Q,[])}catch(H){var U=H}Reflect.construct(e,[],Q)}else{try{Q.call()}catch(H){U=H}e.call(Q.prototype)}}else{try{throw Error()}catch(H){U=H}(Q=e())&&typeof Q.catch=="function"&&Q.catch(function(){})}}catch(H){if(H&&U&&typeof H.stack=="string")return[H.stack,U.stack]}return[null,null]}};l.DetermineComponentFrameRoot.displayName="DetermineComponentFrameRoot";var o=Object.getOwnPropertyDescriptor(l.DetermineComponentFrameRoot,"name");o&&o.configurable&&Object.defineProperty(l.DetermineComponentFrameRoot,"name",{value:"DetermineComponentFrameRoot"});var i=l.DetermineComponentFrameRoot(),c=i[0],p=i[1];if(c&&p){var _=c.split(`
`),k=p.split(`
`);for(o=l=0;l<_.length&&!_[l].includes("DetermineComponentFrameRoot");)l++;for(;o<k.length&&!k[o].includes("DetermineComponentFrameRoot");)o++;if(l===_.length||o===k.length)for(l=_.length-1,o=k.length-1;1<=l&&0<=o&&_[l]!==k[o];)o--;for(;1<=l&&0<=o;l--,o--)if(_[l]!==k[o]){if(l!==1||o!==1)do if(l--,o--,0>o||_[l]!==k[o]){var X=`
`+_[l].replace(" at new "," at ");return e.displayName&&X.includes("<anonymous>")&&(X=X.replace("<anonymous>",e.displayName)),X}while(1<=l&&0<=o);break}}}finally{ye=!1,Error.prepareStackTrace=a}return(a=e?e.displayName||e.name:"")?W(a):""}function he(e,t){switch(e.tag){case 26:case 27:case 5:return W(e.type);case 16:return W("Lazy");case 13:return e.child!==t&&t!==null?W("Suspense Fallback"):W("Suspense");case 19:return W("SuspenseList");case 0:case 15:return Te(e.type,!1);case 11:return Te(e.type.render,!1);case 1:return Te(e.type,!0);case 31:return W("Activity");default:return""}}function De(e){try{var t="",a=null;do t+=he(e,a),a=e,e=e.return;while(e);return t}catch(l){return`
Error generating stack: `+l.message+`
`+l.stack}}var Ue=Object.prototype.hasOwnProperty,dt=r.unstable_scheduleCallback,Fe=r.unstable_cancelCallback,ct=r.unstable_shouldYield,sa=r.unstable_requestPaint,ge=r.unstable_now,pn=r.unstable_getCurrentPriorityLevel,fo=r.unstable_ImmediatePriority,ho=r.unstable_UserBlockingPriority,ra=r.unstable_NormalPriority,ki=r.unstable_LowPriority,mo=r.unstable_IdlePriority,Ri=r.log,Bi=r.unstable_setDisableYieldValue,ca=null,St=null;function ln(e){if(typeof Ri=="function"&&Bi(e),St&&typeof St.setStrictMode=="function")try{St.setStrictMode(ca,e)}catch{}}var xt=Math.clz32?Math.clz32:Et,Ui=Math.log,je=Math.LN2;function Et(e){return e>>>=0,e===0?32:31-(Ui(e)/je|0)|0}var et=256,kn=262144,ja=4194304;function ua(e){var t=e&42;if(t!==0)return t;switch(e&-e){case 1:return 1;case 2:return 2;case 4:return 4;case 8:return 8;case 16:return 16;case 32:return 32;case 64:return 64;case 128:return 128;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:return e&261888;case 262144:case 524288:case 1048576:case 2097152:return e&3932160;case 4194304:case 8388608:case 16777216:case 33554432:return e&62914560;case 67108864:return 67108864;case 134217728:return 134217728;case 268435456:return 268435456;case 536870912:return 536870912;case 1073741824:return 0;default:return e}}function yo(e,t,a){var l=e.pendingLanes;if(l===0)return 0;var o=0,i=e.suspendedLanes,c=e.pingedLanes;e=e.warmLanes;var p=l&134217727;return p!==0?(l=p&~i,l!==0?o=ua(l):(c&=p,c!==0?o=ua(c):a||(a=p&~e,a!==0&&(o=ua(a))))):(p=l&~i,p!==0?o=ua(p):c!==0?o=ua(c):a||(a=l&~e,a!==0&&(o=ua(a)))),o===0?0:t!==0&&t!==o&&(t&i)===0&&(i=o&-o,a=t&-t,i>=a||i===32&&(a&4194048)!==0)?t:o}function yl(e,t){return(e.pendingLanes&~(e.suspendedLanes&~e.pingedLanes)&t)===0}function dh(e,t){switch(e){case 1:case 2:case 4:case 8:case 64:return t+250;case 16:case 32:case 128:case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:return t+5e3;case 4194304:case 8388608:case 16777216:case 33554432:return-1;case 67108864:case 134217728:case 268435456:case 536870912:case 1073741824:return-1;default:return-1}}function xc(){var e=ja;return ja<<=1,(ja&62914560)===0&&(ja=4194304),e}function Gi(e){for(var t=[],a=0;31>a;a++)t.push(e);return t}function gl(e,t){e.pendingLanes|=t,t!==268435456&&(e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0)}function ph(e,t,a,l,o,i){var c=e.pendingLanes;e.pendingLanes=a,e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0,e.expiredLanes&=a,e.entangledLanes&=a,e.errorRecoveryDisabledLanes&=a,e.shellSuspendCounter=0;var p=e.entanglements,_=e.expirationTimes,k=e.hiddenUpdates;for(a=c&~a;0<a;){var X=31-xt(a),Q=1<<X;p[X]=0,_[X]=-1;var U=k[X];if(U!==null)for(k[X]=null,X=0;X<U.length;X++){var H=U[X];H!==null&&(H.lane&=-536870913)}a&=~Q}l!==0&&Tc(e,l,0),i!==0&&o===0&&e.tag!==0&&(e.suspendedLanes|=i&~(c&~t))}function Tc(e,t,a){e.pendingLanes|=t,e.suspendedLanes&=~t;var l=31-xt(t);e.entangledLanes|=t,e.entanglements[l]=e.entanglements[l]|1073741824|a&261930}function wc(e,t){var a=e.entangledLanes|=t;for(e=e.entanglements;a;){var l=31-xt(a),o=1<<l;o&t|e[l]&t&&(e[l]|=t),a&=~o}}function Ec(e,t){var a=t&-t;return a=(a&42)!==0?1:Hi(a),(a&(e.suspendedLanes|t))!==0?0:a}function Hi(e){switch(e){case 2:e=1;break;case 8:e=4;break;case 32:e=16;break;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:case 4194304:case 8388608:case 16777216:case 33554432:e=128;break;case 268435456:e=134217728;break;default:e=0}return e}function Li(e){return e&=-e,2<e?8<e?(e&134217727)!==0?32:268435456:8:2}function Nc(){var e=D.p;return e!==0?e:(e=window.event,e===void 0?32:pf(e.type))}function Ac(e,t){var a=D.p;try{return D.p=e,t()}finally{D.p=a}}var Rn=Math.random().toString(36).slice(2),mt="__reactFiber$"+Rn,Nt="__reactProps$"+Rn,Ma="__reactContainer$"+Rn,qi="__reactEvents$"+Rn,fh="__reactListeners$"+Rn,hh="__reactHandles$"+Rn,Cc="__reactResources$"+Rn,vl="__reactMarker$"+Rn;function Yi(e){delete e[mt],delete e[Nt],delete e[qi],delete e[fh],delete e[hh]}function Da(e){var t=e[mt];if(t)return t;for(var a=e.parentNode;a;){if(t=a[Ma]||a[mt]){if(a=t.alternate,t.child!==null||a!==null&&a.child!==null)for(e=Kp(e);e!==null;){if(a=e[mt])return a;e=Kp(e)}return t}e=a,a=e.parentNode}return null}function Oa(e){if(e=e[mt]||e[Ma]){var t=e.tag;if(t===5||t===6||t===13||t===31||t===26||t===27||t===3)return e}return null}function bl(e){var t=e.tag;if(t===5||t===26||t===27||t===6)return e.stateNode;throw Error(u(33))}function za(e){var t=e[Cc];return t||(t=e[Cc]={hoistableStyles:new Map,hoistableScripts:new Map}),t}function pt(e){e[vl]=!0}var jc=new Set,Mc={};function da(e,t){ka(e,t),ka(e+"Capture",t)}function ka(e,t){for(Mc[e]=t,e=0;e<t.length;e++)jc.add(t[e])}var mh=RegExp("^[:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD][:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD\\-.0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040]*$"),Dc={},Oc={};function yh(e){return Ue.call(Oc,e)?!0:Ue.call(Dc,e)?!1:mh.test(e)?Oc[e]=!0:(Dc[e]=!0,!1)}function go(e,t,a){if(yh(t))if(a===null)e.removeAttribute(t);else{switch(typeof a){case"undefined":case"function":case"symbol":e.removeAttribute(t);return;case"boolean":var l=t.toLowerCase().slice(0,5);if(l!=="data-"&&l!=="aria-"){e.removeAttribute(t);return}}e.setAttribute(t,""+a)}}function vo(e,t,a){if(a===null)e.removeAttribute(t);else{switch(typeof a){case"undefined":case"function":case"symbol":case"boolean":e.removeAttribute(t);return}e.setAttribute(t,""+a)}}function fn(e,t,a,l){if(l===null)e.removeAttribute(a);else{switch(typeof l){case"undefined":case"function":case"symbol":case"boolean":e.removeAttribute(a);return}e.setAttributeNS(t,a,""+l)}}function Xt(e){switch(typeof e){case"bigint":case"boolean":case"number":case"string":case"undefined":return e;case"object":return e;default:return""}}function zc(e){var t=e.type;return(e=e.nodeName)&&e.toLowerCase()==="input"&&(t==="checkbox"||t==="radio")}function gh(e,t,a){var l=Object.getOwnPropertyDescriptor(e.constructor.prototype,t);if(!e.hasOwnProperty(t)&&typeof l<"u"&&typeof l.get=="function"&&typeof l.set=="function"){var o=l.get,i=l.set;return Object.defineProperty(e,t,{configurable:!0,get:function(){return o.call(this)},set:function(c){a=""+c,i.call(this,c)}}),Object.defineProperty(e,t,{enumerable:l.enumerable}),{getValue:function(){return a},setValue:function(c){a=""+c},stopTracking:function(){e._valueTracker=null,delete e[t]}}}}function Xi(e){if(!e._valueTracker){var t=zc(e)?"checked":"value";e._valueTracker=gh(e,t,""+e[t])}}function kc(e){if(!e)return!1;var t=e._valueTracker;if(!t)return!0;var a=t.getValue(),l="";return e&&(l=zc(e)?e.checked?"true":"false":e.value),e=l,e!==a?(t.setValue(e),!0):!1}function bo(e){if(e=e||(typeof document<"u"?document:void 0),typeof e>"u")return null;try{return e.activeElement||e.body}catch{return e.body}}var vh=/[\n"\\]/g;function Jt(e){return e.replace(vh,function(t){return"\\"+t.charCodeAt(0).toString(16)+" "})}function Ji(e,t,a,l,o,i,c,p){e.name="",c!=null&&typeof c!="function"&&typeof c!="symbol"&&typeof c!="boolean"?e.type=c:e.removeAttribute("type"),t!=null?c==="number"?(t===0&&e.value===""||e.value!=t)&&(e.value=""+Xt(t)):e.value!==""+Xt(t)&&(e.value=""+Xt(t)):c!=="submit"&&c!=="reset"||e.removeAttribute("value"),t!=null?Zi(e,c,Xt(t)):a!=null?Zi(e,c,Xt(a)):l!=null&&e.removeAttribute("value"),o==null&&i!=null&&(e.defaultChecked=!!i),o!=null&&(e.checked=o&&typeof o!="function"&&typeof o!="symbol"),p!=null&&typeof p!="function"&&typeof p!="symbol"&&typeof p!="boolean"?e.name=""+Xt(p):e.removeAttribute("name")}function Rc(e,t,a,l,o,i,c,p){if(i!=null&&typeof i!="function"&&typeof i!="symbol"&&typeof i!="boolean"&&(e.type=i),t!=null||a!=null){if(!(i!=="submit"&&i!=="reset"||t!=null)){Xi(e);return}a=a!=null?""+Xt(a):"",t=t!=null?""+Xt(t):a,p||t===e.value||(e.value=t),e.defaultValue=t}l=l??o,l=typeof l!="function"&&typeof l!="symbol"&&!!l,e.checked=p?e.checked:!!l,e.defaultChecked=!!l,c!=null&&typeof c!="function"&&typeof c!="symbol"&&typeof c!="boolean"&&(e.name=c),Xi(e)}function Zi(e,t,a){t==="number"&&bo(e.ownerDocument)===e||e.defaultValue===""+a||(e.defaultValue=""+a)}function Ra(e,t,a,l){if(e=e.options,t){t={};for(var o=0;o<a.length;o++)t["$"+a[o]]=!0;for(a=0;a<e.length;a++)o=t.hasOwnProperty("$"+e[a].value),e[a].selected!==o&&(e[a].selected=o),o&&l&&(e[a].defaultSelected=!0)}else{for(a=""+Xt(a),t=null,o=0;o<e.length;o++){if(e[o].value===a){e[o].selected=!0,l&&(e[o].defaultSelected=!0);return}t!==null||e[o].disabled||(t=e[o])}t!==null&&(t.selected=!0)}}function Bc(e,t,a){if(t!=null&&(t=""+Xt(t),t!==e.value&&(e.value=t),a==null)){e.defaultValue!==t&&(e.defaultValue=t);return}e.defaultValue=a!=null?""+Xt(a):""}function Uc(e,t,a,l){if(t==null){if(l!=null){if(a!=null)throw Error(u(92));if(ie(l)){if(1<l.length)throw Error(u(93));l=l[0]}a=l}a==null&&(a=""),t=a}a=Xt(t),e.defaultValue=a,l=e.textContent,l===a&&l!==""&&l!==null&&(e.value=l),Xi(e)}function Ba(e,t){if(t){var a=e.firstChild;if(a&&a===e.lastChild&&a.nodeType===3){a.nodeValue=t;return}}e.textContent=t}var bh=new Set("animationIterationCount aspectRatio borderImageOutset borderImageSlice borderImageWidth boxFlex boxFlexGroup boxOrdinalGroup columnCount columns flex flexGrow flexPositive flexShrink flexNegative flexOrder gridArea gridRow gridRowEnd gridRowSpan gridRowStart gridColumn gridColumnEnd gridColumnSpan gridColumnStart fontWeight lineClamp lineHeight opacity order orphans scale tabSize widows zIndex zoom fillOpacity floodOpacity stopOpacity strokeDasharray strokeDashoffset strokeMiterlimit strokeOpacity strokeWidth MozAnimationIterationCount MozBoxFlex MozBoxFlexGroup MozLineClamp msAnimationIterationCount msFlex msZoom msFlexGrow msFlexNegative msFlexOrder msFlexPositive msFlexShrink msGridColumn msGridColumnSpan msGridRow msGridRowSpan WebkitAnimationIterationCount WebkitBoxFlex WebKitBoxFlexGroup WebkitBoxOrdinalGroup WebkitColumnCount WebkitColumns WebkitFlex WebkitFlexGrow WebkitFlexPositive WebkitFlexShrink WebkitLineClamp".split(" "));function Gc(e,t,a){var l=t.indexOf("--")===0;a==null||typeof a=="boolean"||a===""?l?e.setProperty(t,""):t==="float"?e.cssFloat="":e[t]="":l?e.setProperty(t,a):typeof a!="number"||a===0||bh.has(t)?t==="float"?e.cssFloat=a:e[t]=(""+a).trim():e[t]=a+"px"}function Hc(e,t,a){if(t!=null&&typeof t!="object")throw Error(u(62));if(e=e.style,a!=null){for(var l in a)!a.hasOwnProperty(l)||t!=null&&t.hasOwnProperty(l)||(l.indexOf("--")===0?e.setProperty(l,""):l==="float"?e.cssFloat="":e[l]="");for(var o in t)l=t[o],t.hasOwnProperty(o)&&a[o]!==l&&Gc(e,o,l)}else for(var i in t)t.hasOwnProperty(i)&&Gc(e,i,t[i])}function Qi(e){if(e.indexOf("-")===-1)return!1;switch(e){case"annotation-xml":case"color-profile":case"font-face":case"font-face-src":case"font-face-uri":case"font-face-format":case"font-face-name":case"missing-glyph":return!1;default:return!0}}var _h=new Map([["acceptCharset","accept-charset"],["htmlFor","for"],["httpEquiv","http-equiv"],["crossOrigin","crossorigin"],["accentHeight","accent-height"],["alignmentBaseline","alignment-baseline"],["arabicForm","arabic-form"],["baselineShift","baseline-shift"],["capHeight","cap-height"],["clipPath","clip-path"],["clipRule","clip-rule"],["colorInterpolation","color-interpolation"],["colorInterpolationFilters","color-interpolation-filters"],["colorProfile","color-profile"],["colorRendering","color-rendering"],["dominantBaseline","dominant-baseline"],["enableBackground","enable-background"],["fillOpacity","fill-opacity"],["fillRule","fill-rule"],["floodColor","flood-color"],["floodOpacity","flood-opacity"],["fontFamily","font-family"],["fontSize","font-size"],["fontSizeAdjust","font-size-adjust"],["fontStretch","font-stretch"],["fontStyle","font-style"],["fontVariant","font-variant"],["fontWeight","font-weight"],["glyphName","glyph-name"],["glyphOrientationHorizontal","glyph-orientation-horizontal"],["glyphOrientationVertical","glyph-orientation-vertical"],["horizAdvX","horiz-adv-x"],["horizOriginX","horiz-origin-x"],["imageRendering","image-rendering"],["letterSpacing","letter-spacing"],["lightingColor","lighting-color"],["markerEnd","marker-end"],["markerMid","marker-mid"],["markerStart","marker-start"],["overlinePosition","overline-position"],["overlineThickness","overline-thickness"],["paintOrder","paint-order"],["panose-1","panose-1"],["pointerEvents","pointer-events"],["renderingIntent","rendering-intent"],["shapeRendering","shape-rendering"],["stopColor","stop-color"],["stopOpacity","stop-opacity"],["strikethroughPosition","strikethrough-position"],["strikethroughThickness","strikethrough-thickness"],["strokeDasharray","stroke-dasharray"],["strokeDashoffset","stroke-dashoffset"],["strokeLinecap","stroke-linecap"],["strokeLinejoin","stroke-linejoin"],["strokeMiterlimit","stroke-miterlimit"],["strokeOpacity","stroke-opacity"],["strokeWidth","stroke-width"],["textAnchor","text-anchor"],["textDecoration","text-decoration"],["textRendering","text-rendering"],["transformOrigin","transform-origin"],["underlinePosition","underline-position"],["underlineThickness","underline-thickness"],["unicodeBidi","unicode-bidi"],["unicodeRange","unicode-range"],["unitsPerEm","units-per-em"],["vAlphabetic","v-alphabetic"],["vHanging","v-hanging"],["vIdeographic","v-ideographic"],["vMathematical","v-mathematical"],["vectorEffect","vector-effect"],["vertAdvY","vert-adv-y"],["vertOriginX","vert-origin-x"],["vertOriginY","vert-origin-y"],["wordSpacing","word-spacing"],["writingMode","writing-mode"],["xmlnsXlink","xmlns:xlink"],["xHeight","x-height"]]),Sh=/^[\u0000-\u001F ]*j[\r\n\t]*a[\r\n\t]*v[\r\n\t]*a[\r\n\t]*s[\r\n\t]*c[\r\n\t]*r[\r\n\t]*i[\r\n\t]*p[\r\n\t]*t[\r\n\t]*:/i;function _o(e){return Sh.test(""+e)?"javascript:throw new Error('React has blocked a javascript: URL as a security precaution.')":e}function hn(){}var Vi=null;function Ii(e){return e=e.target||e.srcElement||window,e.correspondingUseElement&&(e=e.correspondingUseElement),e.nodeType===3?e.parentNode:e}var Ua=null,Ga=null;function Lc(e){var t=Oa(e);if(t&&(e=t.stateNode)){var a=e[Nt]||null;e:switch(e=t.stateNode,t.type){case"input":if(Ji(e,a.value,a.defaultValue,a.defaultValue,a.checked,a.defaultChecked,a.type,a.name),t=a.name,a.type==="radio"&&t!=null){for(a=e;a.parentNode;)a=a.parentNode;for(a=a.querySelectorAll('input[name="'+Jt(""+t)+'"][type="radio"]'),t=0;t<a.length;t++){var l=a[t];if(l!==e&&l.form===e.form){var o=l[Nt]||null;if(!o)throw Error(u(90));Ji(l,o.value,o.defaultValue,o.defaultValue,o.checked,o.defaultChecked,o.type,o.name)}}for(t=0;t<a.length;t++)l=a[t],l.form===e.form&&kc(l)}break e;case"textarea":Bc(e,a.value,a.defaultValue);break e;case"select":t=a.value,t!=null&&Ra(e,!!a.multiple,t,!1)}}}var Ki=!1;function qc(e,t,a){if(Ki)return e(t,a);Ki=!0;try{var l=e(t);return l}finally{if(Ki=!1,(Ua!==null||Ga!==null)&&(si(),Ua&&(t=Ua,e=Ga,Ga=Ua=null,Lc(t),e)))for(t=0;t<e.length;t++)Lc(e[t])}}function _l(e,t){var a=e.stateNode;if(a===null)return null;var l=a[Nt]||null;if(l===null)return null;a=l[t];e:switch(t){case"onClick":case"onClickCapture":case"onDoubleClick":case"onDoubleClickCapture":case"onMouseDown":case"onMouseDownCapture":case"onMouseMove":case"onMouseMoveCapture":case"onMouseUp":case"onMouseUpCapture":case"onMouseEnter":(l=!l.disabled)||(e=e.type,l=!(e==="button"||e==="input"||e==="select"||e==="textarea")),e=!l;break e;default:e=!1}if(e)return null;if(a&&typeof a!="function")throw Error(u(231,t,typeof a));return a}var mn=!(typeof window>"u"||typeof window.document>"u"||typeof window.document.createElement>"u"),$i=!1;if(mn)try{var Sl={};Object.defineProperty(Sl,"passive",{get:function(){$i=!0}}),window.addEventListener("test",Sl,Sl),window.removeEventListener("test",Sl,Sl)}catch{$i=!1}var Bn=null,Wi=null,So=null;function Yc(){if(So)return So;var e,t=Wi,a=t.length,l,o="value"in Bn?Bn.value:Bn.textContent,i=o.length;for(e=0;e<a&&t[e]===o[e];e++);var c=a-e;for(l=1;l<=c&&t[a-l]===o[i-l];l++);return So=o.slice(e,1<l?1-l:void 0)}function xo(e){var t=e.keyCode;return"charCode"in e?(e=e.charCode,e===0&&t===13&&(e=13)):e=t,e===10&&(e=13),32<=e||e===13?e:0}function To(){return!0}function Xc(){return!1}function At(e){function t(a,l,o,i,c){this._reactName=a,this._targetInst=o,this.type=l,this.nativeEvent=i,this.target=c,this.currentTarget=null;for(var p in e)e.hasOwnProperty(p)&&(a=e[p],this[p]=a?a(i):i[p]);return this.isDefaultPrevented=(i.defaultPrevented!=null?i.defaultPrevented:i.returnValue===!1)?To:Xc,this.isPropagationStopped=Xc,this}return v(t.prototype,{preventDefault:function(){this.defaultPrevented=!0;var a=this.nativeEvent;a&&(a.preventDefault?a.preventDefault():typeof a.returnValue!="unknown"&&(a.returnValue=!1),this.isDefaultPrevented=To)},stopPropagation:function(){var a=this.nativeEvent;a&&(a.stopPropagation?a.stopPropagation():typeof a.cancelBubble!="unknown"&&(a.cancelBubble=!0),this.isPropagationStopped=To)},persist:function(){},isPersistent:To}),t}var pa={eventPhase:0,bubbles:0,cancelable:0,timeStamp:function(e){return e.timeStamp||Date.now()},defaultPrevented:0,isTrusted:0},wo=At(pa),xl=v({},pa,{view:0,detail:0}),xh=At(xl),Pi,Fi,Tl,Eo=v({},xl,{screenX:0,screenY:0,clientX:0,clientY:0,pageX:0,pageY:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,getModifierState:ts,button:0,buttons:0,relatedTarget:function(e){return e.relatedTarget===void 0?e.fromElement===e.srcElement?e.toElement:e.fromElement:e.relatedTarget},movementX:function(e){return"movementX"in e?e.movementX:(e!==Tl&&(Tl&&e.type==="mousemove"?(Pi=e.screenX-Tl.screenX,Fi=e.screenY-Tl.screenY):Fi=Pi=0,Tl=e),Pi)},movementY:function(e){return"movementY"in e?e.movementY:Fi}}),Jc=At(Eo),Th=v({},Eo,{dataTransfer:0}),wh=At(Th),Eh=v({},xl,{relatedTarget:0}),es=At(Eh),Nh=v({},pa,{animationName:0,elapsedTime:0,pseudoElement:0}),Ah=At(Nh),Ch=v({},pa,{clipboardData:function(e){return"clipboardData"in e?e.clipboardData:window.clipboardData}}),jh=At(Ch),Mh=v({},pa,{data:0}),Zc=At(Mh),Dh={Esc:"Escape",Spacebar:" ",Left:"ArrowLeft",Up:"ArrowUp",Right:"ArrowRight",Down:"ArrowDown",Del:"Delete",Win:"OS",Menu:"ContextMenu",Apps:"ContextMenu",Scroll:"ScrollLock",MozPrintableKey:"Unidentified"},Oh={8:"Backspace",9:"Tab",12:"Clear",13:"Enter",16:"Shift",17:"Control",18:"Alt",19:"Pause",20:"CapsLock",27:"Escape",32:" ",33:"PageUp",34:"PageDown",35:"End",36:"Home",37:"ArrowLeft",38:"ArrowUp",39:"ArrowRight",40:"ArrowDown",45:"Insert",46:"Delete",112:"F1",113:"F2",114:"F3",115:"F4",116:"F5",117:"F6",118:"F7",119:"F8",120:"F9",121:"F10",122:"F11",123:"F12",144:"NumLock",145:"ScrollLock",224:"Meta"},zh={Alt:"altKey",Control:"ctrlKey",Meta:"metaKey",Shift:"shiftKey"};function kh(e){var t=this.nativeEvent;return t.getModifierState?t.getModifierState(e):(e=zh[e])?!!t[e]:!1}function ts(){return kh}var Rh=v({},xl,{key:function(e){if(e.key){var t=Dh[e.key]||e.key;if(t!=="Unidentified")return t}return e.type==="keypress"?(e=xo(e),e===13?"Enter":String.fromCharCode(e)):e.type==="keydown"||e.type==="keyup"?Oh[e.keyCode]||"Unidentified":""},code:0,location:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,repeat:0,locale:0,getModifierState:ts,charCode:function(e){return e.type==="keypress"?xo(e):0},keyCode:function(e){return e.type==="keydown"||e.type==="keyup"?e.keyCode:0},which:function(e){return e.type==="keypress"?xo(e):e.type==="keydown"||e.type==="keyup"?e.keyCode:0}}),Bh=At(Rh),Uh=v({},Eo,{pointerId:0,width:0,height:0,pressure:0,tangentialPressure:0,tiltX:0,tiltY:0,twist:0,pointerType:0,isPrimary:0}),Qc=At(Uh),Gh=v({},xl,{touches:0,targetTouches:0,changedTouches:0,altKey:0,metaKey:0,ctrlKey:0,shiftKey:0,getModifierState:ts}),Hh=At(Gh),Lh=v({},pa,{propertyName:0,elapsedTime:0,pseudoElement:0}),qh=At(Lh),Yh=v({},Eo,{deltaX:function(e){return"deltaX"in e?e.deltaX:"wheelDeltaX"in e?-e.wheelDeltaX:0},deltaY:function(e){return"deltaY"in e?e.deltaY:"wheelDeltaY"in e?-e.wheelDeltaY:"wheelDelta"in e?-e.wheelDelta:0},deltaZ:0,deltaMode:0}),Xh=At(Yh),Jh=v({},pa,{newState:0,oldState:0}),Zh=At(Jh),Qh=[9,13,27,32],ns=mn&&"CompositionEvent"in window,wl=null;mn&&"documentMode"in document&&(wl=document.documentMode);var Vh=mn&&"TextEvent"in window&&!wl,Vc=mn&&(!ns||wl&&8<wl&&11>=wl),Ic=" ",Kc=!1;function $c(e,t){switch(e){case"keyup":return Qh.indexOf(t.keyCode)!==-1;case"keydown":return t.keyCode!==229;case"keypress":case"mousedown":case"focusout":return!0;default:return!1}}function Wc(e){return e=e.detail,typeof e=="object"&&"data"in e?e.data:null}var Ha=!1;function Ih(e,t){switch(e){case"compositionend":return Wc(t);case"keypress":return t.which!==32?null:(Kc=!0,Ic);case"textInput":return e=t.data,e===Ic&&Kc?null:e;default:return null}}function Kh(e,t){if(Ha)return e==="compositionend"||!ns&&$c(e,t)?(e=Yc(),So=Wi=Bn=null,Ha=!1,e):null;switch(e){case"paste":return null;case"keypress":if(!(t.ctrlKey||t.altKey||t.metaKey)||t.ctrlKey&&t.altKey){if(t.char&&1<t.char.length)return t.char;if(t.which)return String.fromCharCode(t.which)}return null;case"compositionend":return Vc&&t.locale!=="ko"?null:t.data;default:return null}}var $h={color:!0,date:!0,datetime:!0,"datetime-local":!0,email:!0,month:!0,number:!0,password:!0,range:!0,search:!0,tel:!0,text:!0,time:!0,url:!0,week:!0};function Pc(e){var t=e&&e.nodeName&&e.nodeName.toLowerCase();return t==="input"?!!$h[e.type]:t==="textarea"}function Fc(e,t,a,l){Ua?Ga?Ga.push(l):Ga=[l]:Ua=l,t=hi(t,"onChange"),0<t.length&&(a=new wo("onChange","change",null,a,l),e.push({event:a,listeners:t}))}var El=null,Nl=null;function Wh(e){Rp(e,0)}function No(e){var t=bl(e);if(kc(t))return e}function eu(e,t){if(e==="change")return t}var tu=!1;if(mn){var as;if(mn){var ls="oninput"in document;if(!ls){var nu=document.createElement("div");nu.setAttribute("oninput","return;"),ls=typeof nu.oninput=="function"}as=ls}else as=!1;tu=as&&(!document.documentMode||9<document.documentMode)}function au(){El&&(El.detachEvent("onpropertychange",lu),Nl=El=null)}function lu(e){if(e.propertyName==="value"&&No(Nl)){var t=[];Fc(t,Nl,e,Ii(e)),qc(Wh,t)}}function Ph(e,t,a){e==="focusin"?(au(),El=t,Nl=a,El.attachEvent("onpropertychange",lu)):e==="focusout"&&au()}function Fh(e){if(e==="selectionchange"||e==="keyup"||e==="keydown")return No(Nl)}function em(e,t){if(e==="click")return No(t)}function tm(e,t){if(e==="input"||e==="change")return No(t)}function nm(e,t){return e===t&&(e!==0||1/e===1/t)||e!==e&&t!==t}var kt=typeof Object.is=="function"?Object.is:nm;function Al(e,t){if(kt(e,t))return!0;if(typeof e!="object"||e===null||typeof t!="object"||t===null)return!1;var a=Object.keys(e),l=Object.keys(t);if(a.length!==l.length)return!1;for(l=0;l<a.length;l++){var o=a[l];if(!Ue.call(t,o)||!kt(e[o],t[o]))return!1}return!0}function ou(e){for(;e&&e.firstChild;)e=e.firstChild;return e}function iu(e,t){var a=ou(e);e=0;for(var l;a;){if(a.nodeType===3){if(l=e+a.textContent.length,e<=t&&l>=t)return{node:a,offset:t-e};e=l}e:{for(;a;){if(a.nextSibling){a=a.nextSibling;break e}a=a.parentNode}a=void 0}a=ou(a)}}function su(e,t){return e&&t?e===t?!0:e&&e.nodeType===3?!1:t&&t.nodeType===3?su(e,t.parentNode):"contains"in e?e.contains(t):e.compareDocumentPosition?!!(e.compareDocumentPosition(t)&16):!1:!1}function ru(e){e=e!=null&&e.ownerDocument!=null&&e.ownerDocument.defaultView!=null?e.ownerDocument.defaultView:window;for(var t=bo(e.document);t instanceof e.HTMLIFrameElement;){try{var a=typeof t.contentWindow.location.href=="string"}catch{a=!1}if(a)e=t.contentWindow;else break;t=bo(e.document)}return t}function os(e){var t=e&&e.nodeName&&e.nodeName.toLowerCase();return t&&(t==="input"&&(e.type==="text"||e.type==="search"||e.type==="tel"||e.type==="url"||e.type==="password")||t==="textarea"||e.contentEditable==="true")}var am=mn&&"documentMode"in document&&11>=document.documentMode,La=null,is=null,Cl=null,ss=!1;function cu(e,t,a){var l=a.window===a?a.document:a.nodeType===9?a:a.ownerDocument;ss||La==null||La!==bo(l)||(l=La,"selectionStart"in l&&os(l)?l={start:l.selectionStart,end:l.selectionEnd}:(l=(l.ownerDocument&&l.ownerDocument.defaultView||window).getSelection(),l={anchorNode:l.anchorNode,anchorOffset:l.anchorOffset,focusNode:l.focusNode,focusOffset:l.focusOffset}),Cl&&Al(Cl,l)||(Cl=l,l=hi(is,"onSelect"),0<l.length&&(t=new wo("onSelect","select",null,t,a),e.push({event:t,listeners:l}),t.target=La)))}function fa(e,t){var a={};return a[e.toLowerCase()]=t.toLowerCase(),a["Webkit"+e]="webkit"+t,a["Moz"+e]="moz"+t,a}var qa={animationend:fa("Animation","AnimationEnd"),animationiteration:fa("Animation","AnimationIteration"),animationstart:fa("Animation","AnimationStart"),transitionrun:fa("Transition","TransitionRun"),transitionstart:fa("Transition","TransitionStart"),transitioncancel:fa("Transition","TransitionCancel"),transitionend:fa("Transition","TransitionEnd")},rs={},uu={};mn&&(uu=document.createElement("div").style,"AnimationEvent"in window||(delete qa.animationend.animation,delete qa.animationiteration.animation,delete qa.animationstart.animation),"TransitionEvent"in window||delete qa.transitionend.transition);function ha(e){if(rs[e])return rs[e];if(!qa[e])return e;var t=qa[e],a;for(a in t)if(t.hasOwnProperty(a)&&a in uu)return rs[e]=t[a];return e}var du=ha("animationend"),pu=ha("animationiteration"),fu=ha("animationstart"),lm=ha("transitionrun"),om=ha("transitionstart"),im=ha("transitioncancel"),hu=ha("transitionend"),mu=new Map,cs="abort auxClick beforeToggle cancel canPlay canPlayThrough click close contextMenu copy cut drag dragEnd dragEnter dragExit dragLeave dragOver dragStart drop durationChange emptied encrypted ended error gotPointerCapture input invalid keyDown keyPress keyUp load loadedData loadedMetadata loadStart lostPointerCapture mouseDown mouseMove mouseOut mouseOver mouseUp paste pause play playing pointerCancel pointerDown pointerMove pointerOut pointerOver pointerUp progress rateChange reset resize seeked seeking stalled submit suspend timeUpdate touchCancel touchEnd touchStart volumeChange scroll toggle touchMove waiting wheel".split(" ");cs.push("scrollEnd");function en(e,t){mu.set(e,t),da(t,[e])}var Ao=typeof reportError=="function"?reportError:function(e){if(typeof window=="object"&&typeof window.ErrorEvent=="function"){var t=new window.ErrorEvent("error",{bubbles:!0,cancelable:!0,message:typeof e=="object"&&e!==null&&typeof e.message=="string"?String(e.message):String(e),error:e});if(!window.dispatchEvent(t))return}else if(typeof process=="object"&&typeof process.emit=="function"){process.emit("uncaughtException",e);return}console.error(e)},Zt=[],Ya=0,us=0;function Co(){for(var e=Ya,t=us=Ya=0;t<e;){var a=Zt[t];Zt[t++]=null;var l=Zt[t];Zt[t++]=null;var o=Zt[t];Zt[t++]=null;var i=Zt[t];if(Zt[t++]=null,l!==null&&o!==null){var c=l.pending;c===null?o.next=o:(o.next=c.next,c.next=o),l.pending=o}i!==0&&yu(a,o,i)}}function jo(e,t,a,l){Zt[Ya++]=e,Zt[Ya++]=t,Zt[Ya++]=a,Zt[Ya++]=l,us|=l,e.lanes|=l,e=e.alternate,e!==null&&(e.lanes|=l)}function ds(e,t,a,l){return jo(e,t,a,l),Mo(e)}function ma(e,t){return jo(e,null,null,t),Mo(e)}function yu(e,t,a){e.lanes|=a;var l=e.alternate;l!==null&&(l.lanes|=a);for(var o=!1,i=e.return;i!==null;)i.childLanes|=a,l=i.alternate,l!==null&&(l.childLanes|=a),i.tag===22&&(e=i.stateNode,e===null||e._visibility&1||(o=!0)),e=i,i=i.return;return e.tag===3?(i=e.stateNode,o&&t!==null&&(o=31-xt(a),e=i.hiddenUpdates,l=e[o],l===null?e[o]=[t]:l.push(t),t.lane=a|536870912),i):null}function Mo(e){if(50<$l)throw $l=0,_r=null,Error(u(185));for(var t=e.return;t!==null;)e=t,t=e.return;return e.tag===3?e.stateNode:null}var Xa={};function sm(e,t,a,l){this.tag=e,this.key=a,this.sibling=this.child=this.return=this.stateNode=this.type=this.elementType=null,this.index=0,this.refCleanup=this.ref=null,this.pendingProps=t,this.dependencies=this.memoizedState=this.updateQueue=this.memoizedProps=null,this.mode=l,this.subtreeFlags=this.flags=0,this.deletions=null,this.childLanes=this.lanes=0,this.alternate=null}function Rt(e,t,a,l){return new sm(e,t,a,l)}function ps(e){return e=e.prototype,!(!e||!e.isReactComponent)}function yn(e,t){var a=e.alternate;return a===null?(a=Rt(e.tag,t,e.key,e.mode),a.elementType=e.elementType,a.type=e.type,a.stateNode=e.stateNode,a.alternate=e,e.alternate=a):(a.pendingProps=t,a.type=e.type,a.flags=0,a.subtreeFlags=0,a.deletions=null),a.flags=e.flags&65011712,a.childLanes=e.childLanes,a.lanes=e.lanes,a.child=e.child,a.memoizedProps=e.memoizedProps,a.memoizedState=e.memoizedState,a.updateQueue=e.updateQueue,t=e.dependencies,a.dependencies=t===null?null:{lanes:t.lanes,firstContext:t.firstContext},a.sibling=e.sibling,a.index=e.index,a.ref=e.ref,a.refCleanup=e.refCleanup,a}function gu(e,t){e.flags&=65011714;var a=e.alternate;return a===null?(e.childLanes=0,e.lanes=t,e.child=null,e.subtreeFlags=0,e.memoizedProps=null,e.memoizedState=null,e.updateQueue=null,e.dependencies=null,e.stateNode=null):(e.childLanes=a.childLanes,e.lanes=a.lanes,e.child=a.child,e.subtreeFlags=0,e.deletions=null,e.memoizedProps=a.memoizedProps,e.memoizedState=a.memoizedState,e.updateQueue=a.updateQueue,e.type=a.type,t=a.dependencies,e.dependencies=t===null?null:{lanes:t.lanes,firstContext:t.firstContext}),e}function Do(e,t,a,l,o,i){var c=0;if(l=e,typeof e=="function")ps(e)&&(c=1);else if(typeof e=="string")c=py(e,a,me.current)?26:e==="html"||e==="head"||e==="body"?27:5;else e:switch(e){case I:return e=Rt(31,a,t,o),e.elementType=I,e.lanes=i,e;case C:return ya(a.children,o,i,t);case S:c=8,o|=24;break;case O:return e=Rt(12,a,t,o|2),e.elementType=O,e.lanes=i,e;case Y:return e=Rt(13,a,t,o),e.elementType=Y,e.lanes=i,e;case q:return e=Rt(19,a,t,o),e.elementType=q,e.lanes=i,e;default:if(typeof e=="object"&&e!==null)switch(e.$$typeof){case G:c=10;break e;case R:c=9;break e;case L:c=11;break e;case B:c=14;break e;case J:c=16,l=null;break e}c=29,a=Error(u(130,e===null?"null":typeof e,"")),l=null}return t=Rt(c,a,t,o),t.elementType=e,t.type=l,t.lanes=i,t}function ya(e,t,a,l){return e=Rt(7,e,l,t),e.lanes=a,e}function fs(e,t,a){return e=Rt(6,e,null,t),e.lanes=a,e}function vu(e){var t=Rt(18,null,null,0);return t.stateNode=e,t}function hs(e,t,a){return t=Rt(4,e.children!==null?e.children:[],e.key,t),t.lanes=a,t.stateNode={containerInfo:e.containerInfo,pendingChildren:null,implementation:e.implementation},t}var bu=new WeakMap;function Qt(e,t){if(typeof e=="object"&&e!==null){var a=bu.get(e);return a!==void 0?a:(t={value:e,source:t,stack:De(t)},bu.set(e,t),t)}return{value:e,source:t,stack:De(t)}}var Ja=[],Za=0,Oo=null,jl=0,Vt=[],It=0,Un=null,on=1,sn="";function gn(e,t){Ja[Za++]=jl,Ja[Za++]=Oo,Oo=e,jl=t}function _u(e,t,a){Vt[It++]=on,Vt[It++]=sn,Vt[It++]=Un,Un=e;var l=on;e=sn;var o=32-xt(l)-1;l&=~(1<<o),a+=1;var i=32-xt(t)+o;if(30<i){var c=o-o%5;i=(l&(1<<c)-1).toString(32),l>>=c,o-=c,on=1<<32-xt(t)+o|a<<o|l,sn=i+e}else on=1<<i|a<<o|l,sn=e}function ms(e){e.return!==null&&(gn(e,1),_u(e,1,0))}function ys(e){for(;e===Oo;)Oo=Ja[--Za],Ja[Za]=null,jl=Ja[--Za],Ja[Za]=null;for(;e===Un;)Un=Vt[--It],Vt[It]=null,sn=Vt[--It],Vt[It]=null,on=Vt[--It],Vt[It]=null}function Su(e,t){Vt[It++]=on,Vt[It++]=sn,Vt[It++]=Un,on=t.id,sn=t.overflow,Un=e}var yt=null,Ve=null,Ce=!1,Gn=null,Kt=!1,gs=Error(u(519));function Hn(e){var t=Error(u(418,1<arguments.length&&arguments[1]!==void 0&&arguments[1]?"text":"HTML",""));throw Ml(Qt(t,e)),gs}function xu(e){var t=e.stateNode,a=e.type,l=e.memoizedProps;switch(t[mt]=e,t[Nt]=l,a){case"dialog":Ee("cancel",t),Ee("close",t);break;case"iframe":case"object":case"embed":Ee("load",t);break;case"video":case"audio":for(a=0;a<Pl.length;a++)Ee(Pl[a],t);break;case"source":Ee("error",t);break;case"img":case"image":case"link":Ee("error",t),Ee("load",t);break;case"details":Ee("toggle",t);break;case"input":Ee("invalid",t),Rc(t,l.value,l.defaultValue,l.checked,l.defaultChecked,l.type,l.name,!0);break;case"select":Ee("invalid",t);break;case"textarea":Ee("invalid",t),Uc(t,l.value,l.defaultValue,l.children)}a=l.children,typeof a!="string"&&typeof a!="number"&&typeof a!="bigint"||t.textContent===""+a||l.suppressHydrationWarning===!0||Hp(t.textContent,a)?(l.popover!=null&&(Ee("beforetoggle",t),Ee("toggle",t)),l.onScroll!=null&&Ee("scroll",t),l.onScrollEnd!=null&&Ee("scrollend",t),l.onClick!=null&&(t.onclick=hn),t=!0):t=!1,t||Hn(e,!0)}function Tu(e){for(yt=e.return;yt;)switch(yt.tag){case 5:case 31:case 13:Kt=!1;return;case 27:case 3:Kt=!0;return;default:yt=yt.return}}function Qa(e){if(e!==yt)return!1;if(!Ce)return Tu(e),Ce=!0,!1;var t=e.tag,a;if((a=t!==3&&t!==27)&&((a=t===5)&&(a=e.type,a=!(a!=="form"&&a!=="button")||Rr(e.type,e.memoizedProps)),a=!a),a&&Ve&&Hn(e),Tu(e),t===13){if(e=e.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(u(317));Ve=Ip(e)}else if(t===31){if(e=e.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(u(317));Ve=Ip(e)}else t===27?(t=Ve,Fn(e.type)?(e=Lr,Lr=null,Ve=e):Ve=t):Ve=yt?Wt(e.stateNode.nextSibling):null;return!0}function ga(){Ve=yt=null,Ce=!1}function vs(){var e=Gn;return e!==null&&(Dt===null?Dt=e:Dt.push.apply(Dt,e),Gn=null),e}function Ml(e){Gn===null?Gn=[e]:Gn.push(e)}var bs=re(null),va=null,vn=null;function Ln(e,t,a){F(bs,t._currentValue),t._currentValue=a}function bn(e){e._currentValue=bs.current,te(bs)}function _s(e,t,a){for(;e!==null;){var l=e.alternate;if((e.childLanes&t)!==t?(e.childLanes|=t,l!==null&&(l.childLanes|=t)):l!==null&&(l.childLanes&t)!==t&&(l.childLanes|=t),e===a)break;e=e.return}}function Ss(e,t,a,l){var o=e.child;for(o!==null&&(o.return=e);o!==null;){var i=o.dependencies;if(i!==null){var c=o.child;i=i.firstContext;e:for(;i!==null;){var p=i;i=o;for(var _=0;_<t.length;_++)if(p.context===t[_]){i.lanes|=a,p=i.alternate,p!==null&&(p.lanes|=a),_s(i.return,a,e),l||(c=null);break e}i=p.next}}else if(o.tag===18){if(c=o.return,c===null)throw Error(u(341));c.lanes|=a,i=c.alternate,i!==null&&(i.lanes|=a),_s(c,a,e),c=null}else c=o.child;if(c!==null)c.return=o;else for(c=o;c!==null;){if(c===e){c=null;break}if(o=c.sibling,o!==null){o.return=c.return,c=o;break}c=c.return}o=c}}function Va(e,t,a,l){e=null;for(var o=t,i=!1;o!==null;){if(!i){if((o.flags&524288)!==0)i=!0;else if((o.flags&262144)!==0)break}if(o.tag===10){var c=o.alternate;if(c===null)throw Error(u(387));if(c=c.memoizedProps,c!==null){var p=o.type;kt(o.pendingProps.value,c.value)||(e!==null?e.push(p):e=[p])}}else if(o===Be.current){if(c=o.alternate,c===null)throw Error(u(387));c.memoizedState.memoizedState!==o.memoizedState.memoizedState&&(e!==null?e.push(ao):e=[ao])}o=o.return}e!==null&&Ss(t,e,a,l),t.flags|=262144}function zo(e){for(e=e.firstContext;e!==null;){if(!kt(e.context._currentValue,e.memoizedValue))return!0;e=e.next}return!1}function ba(e){va=e,vn=null,e=e.dependencies,e!==null&&(e.firstContext=null)}function gt(e){return wu(va,e)}function ko(e,t){return va===null&&ba(e),wu(e,t)}function wu(e,t){var a=t._currentValue;if(t={context:t,memoizedValue:a,next:null},vn===null){if(e===null)throw Error(u(308));vn=t,e.dependencies={lanes:0,firstContext:t},e.flags|=524288}else vn=vn.next=t;return a}var rm=typeof AbortController<"u"?AbortController:function(){var e=[],t=this.signal={aborted:!1,addEventListener:function(a,l){e.push(l)}};this.abort=function(){t.aborted=!0,e.forEach(function(a){return a()})}},cm=r.unstable_scheduleCallback,um=r.unstable_NormalPriority,ot={$$typeof:G,Consumer:null,Provider:null,_currentValue:null,_currentValue2:null,_threadCount:0};function xs(){return{controller:new rm,data:new Map,refCount:0}}function Dl(e){e.refCount--,e.refCount===0&&cm(um,function(){e.controller.abort()})}var Ol=null,Ts=0,Ia=0,Ka=null;function dm(e,t){if(Ol===null){var a=Ol=[];Ts=0,Ia=Nr(),Ka={status:"pending",value:void 0,then:function(l){a.push(l)}}}return Ts++,t.then(Eu,Eu),t}function Eu(){if(--Ts===0&&Ol!==null){Ka!==null&&(Ka.status="fulfilled");var e=Ol;Ol=null,Ia=0,Ka=null;for(var t=0;t<e.length;t++)(0,e[t])()}}function pm(e,t){var a=[],l={status:"pending",value:null,reason:null,then:function(o){a.push(o)}};return e.then(function(){l.status="fulfilled",l.value=t;for(var o=0;o<a.length;o++)(0,a[o])(t)},function(o){for(l.status="rejected",l.reason=o,o=0;o<a.length;o++)(0,a[o])(void 0)}),l}var Nu=M.S;M.S=function(e,t){cp=ge(),typeof t=="object"&&t!==null&&typeof t.then=="function"&&dm(e,t),Nu!==null&&Nu(e,t)};var _a=re(null);function ws(){var e=_a.current;return e!==null?e:Ze.pooledCache}function Ro(e,t){t===null?F(_a,_a.current):F(_a,t.pool)}function Au(){var e=ws();return e===null?null:{parent:ot._currentValue,pool:e}}var $a=Error(u(460)),Es=Error(u(474)),Bo=Error(u(542)),Uo={then:function(){}};function Cu(e){return e=e.status,e==="fulfilled"||e==="rejected"}function ju(e,t,a){switch(a=e[a],a===void 0?e.push(t):a!==t&&(t.then(hn,hn),t=a),t.status){case"fulfilled":return t.value;case"rejected":throw e=t.reason,Du(e),e;default:if(typeof t.status=="string")t.then(hn,hn);else{if(e=Ze,e!==null&&100<e.shellSuspendCounter)throw Error(u(482));e=t,e.status="pending",e.then(function(l){if(t.status==="pending"){var o=t;o.status="fulfilled",o.value=l}},function(l){if(t.status==="pending"){var o=t;o.status="rejected",o.reason=l}})}switch(t.status){case"fulfilled":return t.value;case"rejected":throw e=t.reason,Du(e),e}throw xa=t,$a}}function Sa(e){try{var t=e._init;return t(e._payload)}catch(a){throw a!==null&&typeof a=="object"&&typeof a.then=="function"?(xa=a,$a):a}}var xa=null;function Mu(){if(xa===null)throw Error(u(459));var e=xa;return xa=null,e}function Du(e){if(e===$a||e===Bo)throw Error(u(483))}var Wa=null,zl=0;function Go(e){var t=zl;return zl+=1,Wa===null&&(Wa=[]),ju(Wa,e,t)}function kl(e,t){t=t.props.ref,e.ref=t!==void 0?t:null}function Ho(e,t){throw t.$$typeof===T?Error(u(525)):(e=Object.prototype.toString.call(t),Error(u(31,e==="[object Object]"?"object with keys {"+Object.keys(t).join(", ")+"}":e)))}function Ou(e){function t(j,w){if(e){var z=j.deletions;z===null?(j.deletions=[w],j.flags|=16):z.push(w)}}function a(j,w){if(!e)return null;for(;w!==null;)t(j,w),w=w.sibling;return null}function l(j){for(var w=new Map;j!==null;)j.key!==null?w.set(j.key,j):w.set(j.index,j),j=j.sibling;return w}function o(j,w){return j=yn(j,w),j.index=0,j.sibling=null,j}function i(j,w,z){return j.index=z,e?(z=j.alternate,z!==null?(z=z.index,z<w?(j.flags|=67108866,w):z):(j.flags|=67108866,w)):(j.flags|=1048576,w)}function c(j){return e&&j.alternate===null&&(j.flags|=67108866),j}function p(j,w,z,Z){return w===null||w.tag!==6?(w=fs(z,j.mode,Z),w.return=j,w):(w=o(w,z),w.return=j,w)}function _(j,w,z,Z){var se=z.type;return se===C?X(j,w,z.props.children,Z,z.key):w!==null&&(w.elementType===se||typeof se=="object"&&se!==null&&se.$$typeof===J&&Sa(se)===w.type)?(w=o(w,z.props),kl(w,z),w.return=j,w):(w=Do(z.type,z.key,z.props,null,j.mode,Z),kl(w,z),w.return=j,w)}function k(j,w,z,Z){return w===null||w.tag!==4||w.stateNode.containerInfo!==z.containerInfo||w.stateNode.implementation!==z.implementation?(w=hs(z,j.mode,Z),w.return=j,w):(w=o(w,z.children||[]),w.return=j,w)}function X(j,w,z,Z,se){return w===null||w.tag!==7?(w=ya(z,j.mode,Z,se),w.return=j,w):(w=o(w,z),w.return=j,w)}function Q(j,w,z){if(typeof w=="string"&&w!==""||typeof w=="number"||typeof w=="bigint")return w=fs(""+w,j.mode,z),w.return=j,w;if(typeof w=="object"&&w!==null){switch(w.$$typeof){case A:return z=Do(w.type,w.key,w.props,null,j.mode,z),kl(z,w),z.return=j,z;case E:return w=hs(w,j.mode,z),w.return=j,w;case J:return w=Sa(w),Q(j,w,z)}if(ie(w)||ne(w))return w=ya(w,j.mode,z,null),w.return=j,w;if(typeof w.then=="function")return Q(j,Go(w),z);if(w.$$typeof===G)return Q(j,ko(j,w),z);Ho(j,w)}return null}function U(j,w,z,Z){var se=w!==null?w.key:null;if(typeof z=="string"&&z!==""||typeof z=="number"||typeof z=="bigint")return se!==null?null:p(j,w,""+z,Z);if(typeof z=="object"&&z!==null){switch(z.$$typeof){case A:return z.key===se?_(j,w,z,Z):null;case E:return z.key===se?k(j,w,z,Z):null;case J:return z=Sa(z),U(j,w,z,Z)}if(ie(z)||ne(z))return se!==null?null:X(j,w,z,Z,null);if(typeof z.then=="function")return U(j,w,Go(z),Z);if(z.$$typeof===G)return U(j,w,ko(j,z),Z);Ho(j,z)}return null}function H(j,w,z,Z,se){if(typeof Z=="string"&&Z!==""||typeof Z=="number"||typeof Z=="bigint")return j=j.get(z)||null,p(w,j,""+Z,se);if(typeof Z=="object"&&Z!==null){switch(Z.$$typeof){case A:return j=j.get(Z.key===null?z:Z.key)||null,_(w,j,Z,se);case E:return j=j.get(Z.key===null?z:Z.key)||null,k(w,j,Z,se);case J:return Z=Sa(Z),H(j,w,z,Z,se)}if(ie(Z)||ne(Z))return j=j.get(z)||null,X(w,j,Z,se,null);if(typeof Z.then=="function")return H(j,w,z,Go(Z),se);if(Z.$$typeof===G)return H(j,w,z,ko(w,Z),se);Ho(w,Z)}return null}function ae(j,w,z,Z){for(var se=null,Oe=null,oe=w,be=w=0,Ae=null;oe!==null&&be<z.length;be++){oe.index>be?(Ae=oe,oe=null):Ae=oe.sibling;var ze=U(j,oe,z[be],Z);if(ze===null){oe===null&&(oe=Ae);break}e&&oe&&ze.alternate===null&&t(j,oe),w=i(ze,w,be),Oe===null?se=ze:Oe.sibling=ze,Oe=ze,oe=Ae}if(be===z.length)return a(j,oe),Ce&&gn(j,be),se;if(oe===null){for(;be<z.length;be++)oe=Q(j,z[be],Z),oe!==null&&(w=i(oe,w,be),Oe===null?se=oe:Oe.sibling=oe,Oe=oe);return Ce&&gn(j,be),se}for(oe=l(oe);be<z.length;be++)Ae=H(oe,j,be,z[be],Z),Ae!==null&&(e&&Ae.alternate!==null&&oe.delete(Ae.key===null?be:Ae.key),w=i(Ae,w,be),Oe===null?se=Ae:Oe.sibling=Ae,Oe=Ae);return e&&oe.forEach(function(la){return t(j,la)}),Ce&&gn(j,be),se}function pe(j,w,z,Z){if(z==null)throw Error(u(151));for(var se=null,Oe=null,oe=w,be=w=0,Ae=null,ze=z.next();oe!==null&&!ze.done;be++,ze=z.next()){oe.index>be?(Ae=oe,oe=null):Ae=oe.sibling;var la=U(j,oe,ze.value,Z);if(la===null){oe===null&&(oe=Ae);break}e&&oe&&la.alternate===null&&t(j,oe),w=i(la,w,be),Oe===null?se=la:Oe.sibling=la,Oe=la,oe=Ae}if(ze.done)return a(j,oe),Ce&&gn(j,be),se;if(oe===null){for(;!ze.done;be++,ze=z.next())ze=Q(j,ze.value,Z),ze!==null&&(w=i(ze,w,be),Oe===null?se=ze:Oe.sibling=ze,Oe=ze);return Ce&&gn(j,be),se}for(oe=l(oe);!ze.done;be++,ze=z.next())ze=H(oe,j,be,ze.value,Z),ze!==null&&(e&&ze.alternate!==null&&oe.delete(ze.key===null?be:ze.key),w=i(ze,w,be),Oe===null?se=ze:Oe.sibling=ze,Oe=ze);return e&&oe.forEach(function(Ty){return t(j,Ty)}),Ce&&gn(j,be),se}function Je(j,w,z,Z){if(typeof z=="object"&&z!==null&&z.type===C&&z.key===null&&(z=z.props.children),typeof z=="object"&&z!==null){switch(z.$$typeof){case A:e:{for(var se=z.key;w!==null;){if(w.key===se){if(se=z.type,se===C){if(w.tag===7){a(j,w.sibling),Z=o(w,z.props.children),Z.return=j,j=Z;break e}}else if(w.elementType===se||typeof se=="object"&&se!==null&&se.$$typeof===J&&Sa(se)===w.type){a(j,w.sibling),Z=o(w,z.props),kl(Z,z),Z.return=j,j=Z;break e}a(j,w);break}else t(j,w);w=w.sibling}z.type===C?(Z=ya(z.props.children,j.mode,Z,z.key),Z.return=j,j=Z):(Z=Do(z.type,z.key,z.props,null,j.mode,Z),kl(Z,z),Z.return=j,j=Z)}return c(j);case E:e:{for(se=z.key;w!==null;){if(w.key===se)if(w.tag===4&&w.stateNode.containerInfo===z.containerInfo&&w.stateNode.implementation===z.implementation){a(j,w.sibling),Z=o(w,z.children||[]),Z.return=j,j=Z;break e}else{a(j,w);break}else t(j,w);w=w.sibling}Z=hs(z,j.mode,Z),Z.return=j,j=Z}return c(j);case J:return z=Sa(z),Je(j,w,z,Z)}if(ie(z))return ae(j,w,z,Z);if(ne(z)){if(se=ne(z),typeof se!="function")throw Error(u(150));return z=se.call(z),pe(j,w,z,Z)}if(typeof z.then=="function")return Je(j,w,Go(z),Z);if(z.$$typeof===G)return Je(j,w,ko(j,z),Z);Ho(j,z)}return typeof z=="string"&&z!==""||typeof z=="number"||typeof z=="bigint"?(z=""+z,w!==null&&w.tag===6?(a(j,w.sibling),Z=o(w,z),Z.return=j,j=Z):(a(j,w),Z=fs(z,j.mode,Z),Z.return=j,j=Z),c(j)):a(j,w)}return function(j,w,z,Z){try{zl=0;var se=Je(j,w,z,Z);return Wa=null,se}catch(oe){if(oe===$a||oe===Bo)throw oe;var Oe=Rt(29,oe,null,j.mode);return Oe.lanes=Z,Oe.return=j,Oe}finally{}}}var Ta=Ou(!0),zu=Ou(!1),qn=!1;function Ns(e){e.updateQueue={baseState:e.memoizedState,firstBaseUpdate:null,lastBaseUpdate:null,shared:{pending:null,lanes:0,hiddenCallbacks:null},callbacks:null}}function As(e,t){e=e.updateQueue,t.updateQueue===e&&(t.updateQueue={baseState:e.baseState,firstBaseUpdate:e.firstBaseUpdate,lastBaseUpdate:e.lastBaseUpdate,shared:e.shared,callbacks:null})}function Yn(e){return{lane:e,tag:0,payload:null,callback:null,next:null}}function Xn(e,t,a){var l=e.updateQueue;if(l===null)return null;if(l=l.shared,(Re&2)!==0){var o=l.pending;return o===null?t.next=t:(t.next=o.next,o.next=t),l.pending=t,t=Mo(e),yu(e,null,a),t}return jo(e,l,t,a),Mo(e)}function Rl(e,t,a){if(t=t.updateQueue,t!==null&&(t=t.shared,(a&4194048)!==0)){var l=t.lanes;l&=e.pendingLanes,a|=l,t.lanes=a,wc(e,a)}}function Cs(e,t){var a=e.updateQueue,l=e.alternate;if(l!==null&&(l=l.updateQueue,a===l)){var o=null,i=null;if(a=a.firstBaseUpdate,a!==null){do{var c={lane:a.lane,tag:a.tag,payload:a.payload,callback:null,next:null};i===null?o=i=c:i=i.next=c,a=a.next}while(a!==null);i===null?o=i=t:i=i.next=t}else o=i=t;a={baseState:l.baseState,firstBaseUpdate:o,lastBaseUpdate:i,shared:l.shared,callbacks:l.callbacks},e.updateQueue=a;return}e=a.lastBaseUpdate,e===null?a.firstBaseUpdate=t:e.next=t,a.lastBaseUpdate=t}var js=!1;function Bl(){if(js){var e=Ka;if(e!==null)throw e}}function Ul(e,t,a,l){js=!1;var o=e.updateQueue;qn=!1;var i=o.firstBaseUpdate,c=o.lastBaseUpdate,p=o.shared.pending;if(p!==null){o.shared.pending=null;var _=p,k=_.next;_.next=null,c===null?i=k:c.next=k,c=_;var X=e.alternate;X!==null&&(X=X.updateQueue,p=X.lastBaseUpdate,p!==c&&(p===null?X.firstBaseUpdate=k:p.next=k,X.lastBaseUpdate=_))}if(i!==null){var Q=o.baseState;c=0,X=k=_=null,p=i;do{var U=p.lane&-536870913,H=U!==p.lane;if(H?(Ne&U)===U:(l&U)===U){U!==0&&U===Ia&&(js=!0),X!==null&&(X=X.next={lane:0,tag:p.tag,payload:p.payload,callback:null,next:null});e:{var ae=e,pe=p;U=t;var Je=a;switch(pe.tag){case 1:if(ae=pe.payload,typeof ae=="function"){Q=ae.call(Je,Q,U);break e}Q=ae;break e;case 3:ae.flags=ae.flags&-65537|128;case 0:if(ae=pe.payload,U=typeof ae=="function"?ae.call(Je,Q,U):ae,U==null)break e;Q=v({},Q,U);break e;case 2:qn=!0}}U=p.callback,U!==null&&(e.flags|=64,H&&(e.flags|=8192),H=o.callbacks,H===null?o.callbacks=[U]:H.push(U))}else H={lane:U,tag:p.tag,payload:p.payload,callback:p.callback,next:null},X===null?(k=X=H,_=Q):X=X.next=H,c|=U;if(p=p.next,p===null){if(p=o.shared.pending,p===null)break;H=p,p=H.next,H.next=null,o.lastBaseUpdate=H,o.shared.pending=null}}while(!0);X===null&&(_=Q),o.baseState=_,o.firstBaseUpdate=k,o.lastBaseUpdate=X,i===null&&(o.shared.lanes=0),In|=c,e.lanes=c,e.memoizedState=Q}}function ku(e,t){if(typeof e!="function")throw Error(u(191,e));e.call(t)}function Ru(e,t){var a=e.callbacks;if(a!==null)for(e.callbacks=null,e=0;e<a.length;e++)ku(a[e],t)}var Pa=re(null),Lo=re(0);function Bu(e,t){e=Cn,F(Lo,e),F(Pa,t),Cn=e|t.baseLanes}function Ms(){F(Lo,Cn),F(Pa,Pa.current)}function Ds(){Cn=Lo.current,te(Pa),te(Lo)}var Bt=re(null),$t=null;function Jn(e){var t=e.alternate;F(tt,tt.current&1),F(Bt,e),$t===null&&(t===null||Pa.current!==null||t.memoizedState!==null)&&($t=e)}function Os(e){F(tt,tt.current),F(Bt,e),$t===null&&($t=e)}function Uu(e){e.tag===22?(F(tt,tt.current),F(Bt,e),$t===null&&($t=e)):Zn()}function Zn(){F(tt,tt.current),F(Bt,Bt.current)}function Ut(e){te(Bt),$t===e&&($t=null),te(tt)}var tt=re(0);function qo(e){for(var t=e;t!==null;){if(t.tag===13){var a=t.memoizedState;if(a!==null&&(a=a.dehydrated,a===null||Gr(a)||Hr(a)))return t}else if(t.tag===19&&(t.memoizedProps.revealOrder==="forwards"||t.memoizedProps.revealOrder==="backwards"||t.memoizedProps.revealOrder==="unstable_legacy-backwards"||t.memoizedProps.revealOrder==="together")){if((t.flags&128)!==0)return t}else if(t.child!==null){t.child.return=t,t=t.child;continue}if(t===e)break;for(;t.sibling===null;){if(t.return===null||t.return===e)return null;t=t.return}t.sibling.return=t.return,t=t.sibling}return null}var _n=0,ve=null,Ye=null,it=null,Yo=!1,Fa=!1,wa=!1,Xo=0,Gl=0,el=null,fm=0;function We(){throw Error(u(321))}function zs(e,t){if(t===null)return!1;for(var a=0;a<t.length&&a<e.length;a++)if(!kt(e[a],t[a]))return!1;return!0}function ks(e,t,a,l,o,i){return _n=i,ve=t,t.memoizedState=null,t.updateQueue=null,t.lanes=0,M.H=e===null||e.memoizedState===null?_d:Ks,wa=!1,i=a(l,o),wa=!1,Fa&&(i=Hu(t,a,l,o)),Gu(e),i}function Gu(e){M.H=ql;var t=Ye!==null&&Ye.next!==null;if(_n=0,it=Ye=ve=null,Yo=!1,Gl=0,el=null,t)throw Error(u(300));e===null||st||(e=e.dependencies,e!==null&&zo(e)&&(st=!0))}function Hu(e,t,a,l){ve=e;var o=0;do{if(Fa&&(el=null),Gl=0,Fa=!1,25<=o)throw Error(u(301));if(o+=1,it=Ye=null,e.updateQueue!=null){var i=e.updateQueue;i.lastEffect=null,i.events=null,i.stores=null,i.memoCache!=null&&(i.memoCache.index=0)}M.H=Sd,i=t(a,l)}while(Fa);return i}function hm(){var e=M.H,t=e.useState()[0];return t=typeof t.then=="function"?Hl(t):t,e=e.useState()[0],(Ye!==null?Ye.memoizedState:null)!==e&&(ve.flags|=1024),t}function Rs(){var e=Xo!==0;return Xo=0,e}function Bs(e,t,a){t.updateQueue=e.updateQueue,t.flags&=-2053,e.lanes&=~a}function Us(e){if(Yo){for(e=e.memoizedState;e!==null;){var t=e.queue;t!==null&&(t.pending=null),e=e.next}Yo=!1}_n=0,it=Ye=ve=null,Fa=!1,Gl=Xo=0,el=null}function Tt(){var e={memoizedState:null,baseState:null,baseQueue:null,queue:null,next:null};return it===null?ve.memoizedState=it=e:it=it.next=e,it}function nt(){if(Ye===null){var e=ve.alternate;e=e!==null?e.memoizedState:null}else e=Ye.next;var t=it===null?ve.memoizedState:it.next;if(t!==null)it=t,Ye=e;else{if(e===null)throw ve.alternate===null?Error(u(467)):Error(u(310));Ye=e,e={memoizedState:Ye.memoizedState,baseState:Ye.baseState,baseQueue:Ye.baseQueue,queue:Ye.queue,next:null},it===null?ve.memoizedState=it=e:it=it.next=e}return it}function Jo(){return{lastEffect:null,events:null,stores:null,memoCache:null}}function Hl(e){var t=Gl;return Gl+=1,el===null&&(el=[]),e=ju(el,e,t),t=ve,(it===null?t.memoizedState:it.next)===null&&(t=t.alternate,M.H=t===null||t.memoizedState===null?_d:Ks),e}function Zo(e){if(e!==null&&typeof e=="object"){if(typeof e.then=="function")return Hl(e);if(e.$$typeof===G)return gt(e)}throw Error(u(438,String(e)))}function Gs(e){var t=null,a=ve.updateQueue;if(a!==null&&(t=a.memoCache),t==null){var l=ve.alternate;l!==null&&(l=l.updateQueue,l!==null&&(l=l.memoCache,l!=null&&(t={data:l.data.map(function(o){return o.slice()}),index:0})))}if(t==null&&(t={data:[],index:0}),a===null&&(a=Jo(),ve.updateQueue=a),a.memoCache=t,a=t.data[t.index],a===void 0)for(a=t.data[t.index]=Array(e),l=0;l<e;l++)a[l]=K;return t.index++,a}function Sn(e,t){return typeof t=="function"?t(e):t}function Qo(e){var t=nt();return Hs(t,Ye,e)}function Hs(e,t,a){var l=e.queue;if(l===null)throw Error(u(311));l.lastRenderedReducer=a;var o=e.baseQueue,i=l.pending;if(i!==null){if(o!==null){var c=o.next;o.next=i.next,i.next=c}t.baseQueue=o=i,l.pending=null}if(i=e.baseState,o===null)e.memoizedState=i;else{t=o.next;var p=c=null,_=null,k=t,X=!1;do{var Q=k.lane&-536870913;if(Q!==k.lane?(Ne&Q)===Q:(_n&Q)===Q){var U=k.revertLane;if(U===0)_!==null&&(_=_.next={lane:0,revertLane:0,gesture:null,action:k.action,hasEagerState:k.hasEagerState,eagerState:k.eagerState,next:null}),Q===Ia&&(X=!0);else if((_n&U)===U){k=k.next,U===Ia&&(X=!0);continue}else Q={lane:0,revertLane:k.revertLane,gesture:null,action:k.action,hasEagerState:k.hasEagerState,eagerState:k.eagerState,next:null},_===null?(p=_=Q,c=i):_=_.next=Q,ve.lanes|=U,In|=U;Q=k.action,wa&&a(i,Q),i=k.hasEagerState?k.eagerState:a(i,Q)}else U={lane:Q,revertLane:k.revertLane,gesture:k.gesture,action:k.action,hasEagerState:k.hasEagerState,eagerState:k.eagerState,next:null},_===null?(p=_=U,c=i):_=_.next=U,ve.lanes|=Q,In|=Q;k=k.next}while(k!==null&&k!==t);if(_===null?c=i:_.next=p,!kt(i,e.memoizedState)&&(st=!0,X&&(a=Ka,a!==null)))throw a;e.memoizedState=i,e.baseState=c,e.baseQueue=_,l.lastRenderedState=i}return o===null&&(l.lanes=0),[e.memoizedState,l.dispatch]}function Ls(e){var t=nt(),a=t.queue;if(a===null)throw Error(u(311));a.lastRenderedReducer=e;var l=a.dispatch,o=a.pending,i=t.memoizedState;if(o!==null){a.pending=null;var c=o=o.next;do i=e(i,c.action),c=c.next;while(c!==o);kt(i,t.memoizedState)||(st=!0),t.memoizedState=i,t.baseQueue===null&&(t.baseState=i),a.lastRenderedState=i}return[i,l]}function Lu(e,t,a){var l=ve,o=nt(),i=Ce;if(i){if(a===void 0)throw Error(u(407));a=a()}else a=t();var c=!kt((Ye||o).memoizedState,a);if(c&&(o.memoizedState=a,st=!0),o=o.queue,Xs(Xu.bind(null,l,o,e),[e]),o.getSnapshot!==t||c||it!==null&&it.memoizedState.tag&1){if(l.flags|=2048,tl(9,{destroy:void 0},Yu.bind(null,l,o,a,t),null),Ze===null)throw Error(u(349));i||(_n&127)!==0||qu(l,t,a)}return a}function qu(e,t,a){e.flags|=16384,e={getSnapshot:t,value:a},t=ve.updateQueue,t===null?(t=Jo(),ve.updateQueue=t,t.stores=[e]):(a=t.stores,a===null?t.stores=[e]:a.push(e))}function Yu(e,t,a,l){t.value=a,t.getSnapshot=l,Ju(t)&&Zu(e)}function Xu(e,t,a){return a(function(){Ju(t)&&Zu(e)})}function Ju(e){var t=e.getSnapshot;e=e.value;try{var a=t();return!kt(e,a)}catch{return!0}}function Zu(e){var t=ma(e,2);t!==null&&Ot(t,e,2)}function qs(e){var t=Tt();if(typeof e=="function"){var a=e;if(e=a(),wa){ln(!0);try{a()}finally{ln(!1)}}}return t.memoizedState=t.baseState=e,t.queue={pending:null,lanes:0,dispatch:null,lastRenderedReducer:Sn,lastRenderedState:e},t}function Qu(e,t,a,l){return e.baseState=a,Hs(e,Ye,typeof l=="function"?l:Sn)}function mm(e,t,a,l,o){if(Ko(e))throw Error(u(485));if(e=t.action,e!==null){var i={payload:o,action:e,next:null,isTransition:!0,status:"pending",value:null,reason:null,listeners:[],then:function(c){i.listeners.push(c)}};M.T!==null?a(!0):i.isTransition=!1,l(i),a=t.pending,a===null?(i.next=t.pending=i,Vu(t,i)):(i.next=a.next,t.pending=a.next=i)}}function Vu(e,t){var a=t.action,l=t.payload,o=e.state;if(t.isTransition){var i=M.T,c={};M.T=c;try{var p=a(o,l),_=M.S;_!==null&&_(c,p),Iu(e,t,p)}catch(k){Ys(e,t,k)}finally{i!==null&&c.types!==null&&(i.types=c.types),M.T=i}}else try{i=a(o,l),Iu(e,t,i)}catch(k){Ys(e,t,k)}}function Iu(e,t,a){a!==null&&typeof a=="object"&&typeof a.then=="function"?a.then(function(l){Ku(e,t,l)},function(l){return Ys(e,t,l)}):Ku(e,t,a)}function Ku(e,t,a){t.status="fulfilled",t.value=a,$u(t),e.state=a,t=e.pending,t!==null&&(a=t.next,a===t?e.pending=null:(a=a.next,t.next=a,Vu(e,a)))}function Ys(e,t,a){var l=e.pending;if(e.pending=null,l!==null){l=l.next;do t.status="rejected",t.reason=a,$u(t),t=t.next;while(t!==l)}e.action=null}function $u(e){e=e.listeners;for(var t=0;t<e.length;t++)(0,e[t])()}function Wu(e,t){return t}function Pu(e,t){if(Ce){var a=Ze.formState;if(a!==null){e:{var l=ve;if(Ce){if(Ve){t:{for(var o=Ve,i=Kt;o.nodeType!==8;){if(!i){o=null;break t}if(o=Wt(o.nextSibling),o===null){o=null;break t}}i=o.data,o=i==="F!"||i==="F"?o:null}if(o){Ve=Wt(o.nextSibling),l=o.data==="F!";break e}}Hn(l)}l=!1}l&&(t=a[0])}}return a=Tt(),a.memoizedState=a.baseState=t,l={pending:null,lanes:0,dispatch:null,lastRenderedReducer:Wu,lastRenderedState:t},a.queue=l,a=gd.bind(null,ve,l),l.dispatch=a,l=qs(!1),i=Is.bind(null,ve,!1,l.queue),l=Tt(),o={state:t,dispatch:null,action:e,pending:null},l.queue=o,a=mm.bind(null,ve,o,i,a),o.dispatch=a,l.memoizedState=e,[t,a,!1]}function Fu(e){var t=nt();return ed(t,Ye,e)}function ed(e,t,a){if(t=Hs(e,t,Wu)[0],e=Qo(Sn)[0],typeof t=="object"&&t!==null&&typeof t.then=="function")try{var l=Hl(t)}catch(c){throw c===$a?Bo:c}else l=t;t=nt();var o=t.queue,i=o.dispatch;return a!==t.memoizedState&&(ve.flags|=2048,tl(9,{destroy:void 0},ym.bind(null,o,a),null)),[l,i,e]}function ym(e,t){e.action=t}function td(e){var t=nt(),a=Ye;if(a!==null)return ed(t,a,e);nt(),t=t.memoizedState,a=nt();var l=a.queue.dispatch;return a.memoizedState=e,[t,l,!1]}function tl(e,t,a,l){return e={tag:e,create:a,deps:l,inst:t,next:null},t=ve.updateQueue,t===null&&(t=Jo(),ve.updateQueue=t),a=t.lastEffect,a===null?t.lastEffect=e.next=e:(l=a.next,a.next=e,e.next=l,t.lastEffect=e),e}function nd(){return nt().memoizedState}function Vo(e,t,a,l){var o=Tt();ve.flags|=e,o.memoizedState=tl(1|t,{destroy:void 0},a,l===void 0?null:l)}function Io(e,t,a,l){var o=nt();l=l===void 0?null:l;var i=o.memoizedState.inst;Ye!==null&&l!==null&&zs(l,Ye.memoizedState.deps)?o.memoizedState=tl(t,i,a,l):(ve.flags|=e,o.memoizedState=tl(1|t,i,a,l))}function ad(e,t){Vo(8390656,8,e,t)}function Xs(e,t){Io(2048,8,e,t)}function gm(e){ve.flags|=4;var t=ve.updateQueue;if(t===null)t=Jo(),ve.updateQueue=t,t.events=[e];else{var a=t.events;a===null?t.events=[e]:a.push(e)}}function ld(e){var t=nt().memoizedState;return gm({ref:t,nextImpl:e}),function(){if((Re&2)!==0)throw Error(u(440));return t.impl.apply(void 0,arguments)}}function od(e,t){return Io(4,2,e,t)}function id(e,t){return Io(4,4,e,t)}function sd(e,t){if(typeof t=="function"){e=e();var a=t(e);return function(){typeof a=="function"?a():t(null)}}if(t!=null)return e=e(),t.current=e,function(){t.current=null}}function rd(e,t,a){a=a!=null?a.concat([e]):null,Io(4,4,sd.bind(null,t,e),a)}function Js(){}function cd(e,t){var a=nt();t=t===void 0?null:t;var l=a.memoizedState;return t!==null&&zs(t,l[1])?l[0]:(a.memoizedState=[e,t],e)}function ud(e,t){var a=nt();t=t===void 0?null:t;var l=a.memoizedState;if(t!==null&&zs(t,l[1]))return l[0];if(l=e(),wa){ln(!0);try{e()}finally{ln(!1)}}return a.memoizedState=[l,t],l}function Zs(e,t,a){return a===void 0||(_n&1073741824)!==0&&(Ne&261930)===0?e.memoizedState=t:(e.memoizedState=a,e=dp(),ve.lanes|=e,In|=e,a)}function dd(e,t,a,l){return kt(a,t)?a:Pa.current!==null?(e=Zs(e,a,l),kt(e,t)||(st=!0),e):(_n&42)===0||(_n&1073741824)!==0&&(Ne&261930)===0?(st=!0,e.memoizedState=a):(e=dp(),ve.lanes|=e,In|=e,t)}function pd(e,t,a,l,o){var i=D.p;D.p=i!==0&&8>i?i:8;var c=M.T,p={};M.T=p,Is(e,!1,t,a);try{var _=o(),k=M.S;if(k!==null&&k(p,_),_!==null&&typeof _=="object"&&typeof _.then=="function"){var X=pm(_,l);Ll(e,t,X,Lt(e))}else Ll(e,t,l,Lt(e))}catch(Q){Ll(e,t,{then:function(){},status:"rejected",reason:Q},Lt())}finally{D.p=i,c!==null&&p.types!==null&&(c.types=p.types),M.T=c}}function vm(){}function Qs(e,t,a,l){if(e.tag!==5)throw Error(u(476));var o=fd(e).queue;pd(e,o,t,V,a===null?vm:function(){return hd(e),a(l)})}function fd(e){var t=e.memoizedState;if(t!==null)return t;t={memoizedState:V,baseState:V,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:Sn,lastRenderedState:V},next:null};var a={};return t.next={memoizedState:a,baseState:a,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:Sn,lastRenderedState:a},next:null},e.memoizedState=t,e=e.alternate,e!==null&&(e.memoizedState=t),t}function hd(e){var t=fd(e);t.next===null&&(t=e.alternate.memoizedState),Ll(e,t.next.queue,{},Lt())}function Vs(){return gt(ao)}function md(){return nt().memoizedState}function yd(){return nt().memoizedState}function bm(e){for(var t=e.return;t!==null;){switch(t.tag){case 24:case 3:var a=Lt();e=Yn(a);var l=Xn(t,e,a);l!==null&&(Ot(l,t,a),Rl(l,t,a)),t={cache:xs()},e.payload=t;return}t=t.return}}function _m(e,t,a){var l=Lt();a={lane:l,revertLane:0,gesture:null,action:a,hasEagerState:!1,eagerState:null,next:null},Ko(e)?vd(t,a):(a=ds(e,t,a,l),a!==null&&(Ot(a,e,l),bd(a,t,l)))}function gd(e,t,a){var l=Lt();Ll(e,t,a,l)}function Ll(e,t,a,l){var o={lane:l,revertLane:0,gesture:null,action:a,hasEagerState:!1,eagerState:null,next:null};if(Ko(e))vd(t,o);else{var i=e.alternate;if(e.lanes===0&&(i===null||i.lanes===0)&&(i=t.lastRenderedReducer,i!==null))try{var c=t.lastRenderedState,p=i(c,a);if(o.hasEagerState=!0,o.eagerState=p,kt(p,c))return jo(e,t,o,0),Ze===null&&Co(),!1}catch{}finally{}if(a=ds(e,t,o,l),a!==null)return Ot(a,e,l),bd(a,t,l),!0}return!1}function Is(e,t,a,l){if(l={lane:2,revertLane:Nr(),gesture:null,action:l,hasEagerState:!1,eagerState:null,next:null},Ko(e)){if(t)throw Error(u(479))}else t=ds(e,a,l,2),t!==null&&Ot(t,e,2)}function Ko(e){var t=e.alternate;return e===ve||t!==null&&t===ve}function vd(e,t){Fa=Yo=!0;var a=e.pending;a===null?t.next=t:(t.next=a.next,a.next=t),e.pending=t}function bd(e,t,a){if((a&4194048)!==0){var l=t.lanes;l&=e.pendingLanes,a|=l,t.lanes=a,wc(e,a)}}var ql={readContext:gt,use:Zo,useCallback:We,useContext:We,useEffect:We,useImperativeHandle:We,useLayoutEffect:We,useInsertionEffect:We,useMemo:We,useReducer:We,useRef:We,useState:We,useDebugValue:We,useDeferredValue:We,useTransition:We,useSyncExternalStore:We,useId:We,useHostTransitionStatus:We,useFormState:We,useActionState:We,useOptimistic:We,useMemoCache:We,useCacheRefresh:We};ql.useEffectEvent=We;var _d={readContext:gt,use:Zo,useCallback:function(e,t){return Tt().memoizedState=[e,t===void 0?null:t],e},useContext:gt,useEffect:ad,useImperativeHandle:function(e,t,a){a=a!=null?a.concat([e]):null,Vo(4194308,4,sd.bind(null,t,e),a)},useLayoutEffect:function(e,t){return Vo(4194308,4,e,t)},useInsertionEffect:function(e,t){Vo(4,2,e,t)},useMemo:function(e,t){var a=Tt();t=t===void 0?null:t;var l=e();if(wa){ln(!0);try{e()}finally{ln(!1)}}return a.memoizedState=[l,t],l},useReducer:function(e,t,a){var l=Tt();if(a!==void 0){var o=a(t);if(wa){ln(!0);try{a(t)}finally{ln(!1)}}}else o=t;return l.memoizedState=l.baseState=o,e={pending:null,lanes:0,dispatch:null,lastRenderedReducer:e,lastRenderedState:o},l.queue=e,e=e.dispatch=_m.bind(null,ve,e),[l.memoizedState,e]},useRef:function(e){var t=Tt();return e={current:e},t.memoizedState=e},useState:function(e){e=qs(e);var t=e.queue,a=gd.bind(null,ve,t);return t.dispatch=a,[e.memoizedState,a]},useDebugValue:Js,useDeferredValue:function(e,t){var a=Tt();return Zs(a,e,t)},useTransition:function(){var e=qs(!1);return e=pd.bind(null,ve,e.queue,!0,!1),Tt().memoizedState=e,[!1,e]},useSyncExternalStore:function(e,t,a){var l=ve,o=Tt();if(Ce){if(a===void 0)throw Error(u(407));a=a()}else{if(a=t(),Ze===null)throw Error(u(349));(Ne&127)!==0||qu(l,t,a)}o.memoizedState=a;var i={value:a,getSnapshot:t};return o.queue=i,ad(Xu.bind(null,l,i,e),[e]),l.flags|=2048,tl(9,{destroy:void 0},Yu.bind(null,l,i,a,t),null),a},useId:function(){var e=Tt(),t=Ze.identifierPrefix;if(Ce){var a=sn,l=on;a=(l&~(1<<32-xt(l)-1)).toString(32)+a,t="_"+t+"R_"+a,a=Xo++,0<a&&(t+="H"+a.toString(32)),t+="_"}else a=fm++,t="_"+t+"r_"+a.toString(32)+"_";return e.memoizedState=t},useHostTransitionStatus:Vs,useFormState:Pu,useActionState:Pu,useOptimistic:function(e){var t=Tt();t.memoizedState=t.baseState=e;var a={pending:null,lanes:0,dispatch:null,lastRenderedReducer:null,lastRenderedState:null};return t.queue=a,t=Is.bind(null,ve,!0,a),a.dispatch=t,[e,t]},useMemoCache:Gs,useCacheRefresh:function(){return Tt().memoizedState=bm.bind(null,ve)},useEffectEvent:function(e){var t=Tt(),a={impl:e};return t.memoizedState=a,function(){if((Re&2)!==0)throw Error(u(440));return a.impl.apply(void 0,arguments)}}},Ks={readContext:gt,use:Zo,useCallback:cd,useContext:gt,useEffect:Xs,useImperativeHandle:rd,useInsertionEffect:od,useLayoutEffect:id,useMemo:ud,useReducer:Qo,useRef:nd,useState:function(){return Qo(Sn)},useDebugValue:Js,useDeferredValue:function(e,t){var a=nt();return dd(a,Ye.memoizedState,e,t)},useTransition:function(){var e=Qo(Sn)[0],t=nt().memoizedState;return[typeof e=="boolean"?e:Hl(e),t]},useSyncExternalStore:Lu,useId:md,useHostTransitionStatus:Vs,useFormState:Fu,useActionState:Fu,useOptimistic:function(e,t){var a=nt();return Qu(a,Ye,e,t)},useMemoCache:Gs,useCacheRefresh:yd};Ks.useEffectEvent=ld;var Sd={readContext:gt,use:Zo,useCallback:cd,useContext:gt,useEffect:Xs,useImperativeHandle:rd,useInsertionEffect:od,useLayoutEffect:id,useMemo:ud,useReducer:Ls,useRef:nd,useState:function(){return Ls(Sn)},useDebugValue:Js,useDeferredValue:function(e,t){var a=nt();return Ye===null?Zs(a,e,t):dd(a,Ye.memoizedState,e,t)},useTransition:function(){var e=Ls(Sn)[0],t=nt().memoizedState;return[typeof e=="boolean"?e:Hl(e),t]},useSyncExternalStore:Lu,useId:md,useHostTransitionStatus:Vs,useFormState:td,useActionState:td,useOptimistic:function(e,t){var a=nt();return Ye!==null?Qu(a,Ye,e,t):(a.baseState=e,[e,a.queue.dispatch])},useMemoCache:Gs,useCacheRefresh:yd};Sd.useEffectEvent=ld;function $s(e,t,a,l){t=e.memoizedState,a=a(l,t),a=a==null?t:v({},t,a),e.memoizedState=a,e.lanes===0&&(e.updateQueue.baseState=a)}var Ws={enqueueSetState:function(e,t,a){e=e._reactInternals;var l=Lt(),o=Yn(l);o.payload=t,a!=null&&(o.callback=a),t=Xn(e,o,l),t!==null&&(Ot(t,e,l),Rl(t,e,l))},enqueueReplaceState:function(e,t,a){e=e._reactInternals;var l=Lt(),o=Yn(l);o.tag=1,o.payload=t,a!=null&&(o.callback=a),t=Xn(e,o,l),t!==null&&(Ot(t,e,l),Rl(t,e,l))},enqueueForceUpdate:function(e,t){e=e._reactInternals;var a=Lt(),l=Yn(a);l.tag=2,t!=null&&(l.callback=t),t=Xn(e,l,a),t!==null&&(Ot(t,e,a),Rl(t,e,a))}};function xd(e,t,a,l,o,i,c){return e=e.stateNode,typeof e.shouldComponentUpdate=="function"?e.shouldComponentUpdate(l,i,c):t.prototype&&t.prototype.isPureReactComponent?!Al(a,l)||!Al(o,i):!0}function Td(e,t,a,l){e=t.state,typeof t.componentWillReceiveProps=="function"&&t.componentWillReceiveProps(a,l),typeof t.UNSAFE_componentWillReceiveProps=="function"&&t.UNSAFE_componentWillReceiveProps(a,l),t.state!==e&&Ws.enqueueReplaceState(t,t.state,null)}function Ea(e,t){var a=t;if("ref"in t){a={};for(var l in t)l!=="ref"&&(a[l]=t[l])}if(e=e.defaultProps){a===t&&(a=v({},a));for(var o in e)a[o]===void 0&&(a[o]=e[o])}return a}function wd(e){Ao(e)}function Ed(e){console.error(e)}function Nd(e){Ao(e)}function $o(e,t){try{var a=e.onUncaughtError;a(t.value,{componentStack:t.stack})}catch(l){setTimeout(function(){throw l})}}function Ad(e,t,a){try{var l=e.onCaughtError;l(a.value,{componentStack:a.stack,errorBoundary:t.tag===1?t.stateNode:null})}catch(o){setTimeout(function(){throw o})}}function Ps(e,t,a){return a=Yn(a),a.tag=3,a.payload={element:null},a.callback=function(){$o(e,t)},a}function Cd(e){return e=Yn(e),e.tag=3,e}function jd(e,t,a,l){var o=a.type.getDerivedStateFromError;if(typeof o=="function"){var i=l.value;e.payload=function(){return o(i)},e.callback=function(){Ad(t,a,l)}}var c=a.stateNode;c!==null&&typeof c.componentDidCatch=="function"&&(e.callback=function(){Ad(t,a,l),typeof o!="function"&&(Kn===null?Kn=new Set([this]):Kn.add(this));var p=l.stack;this.componentDidCatch(l.value,{componentStack:p!==null?p:""})})}function Sm(e,t,a,l,o){if(a.flags|=32768,l!==null&&typeof l=="object"&&typeof l.then=="function"){if(t=a.alternate,t!==null&&Va(t,a,o,!0),a=Bt.current,a!==null){switch(a.tag){case 31:case 13:return $t===null?ri():a.alternate===null&&Pe===0&&(Pe=3),a.flags&=-257,a.flags|=65536,a.lanes=o,l===Uo?a.flags|=16384:(t=a.updateQueue,t===null?a.updateQueue=new Set([l]):t.add(l),Tr(e,l,o)),!1;case 22:return a.flags|=65536,l===Uo?a.flags|=16384:(t=a.updateQueue,t===null?(t={transitions:null,markerInstances:null,retryQueue:new Set([l])},a.updateQueue=t):(a=t.retryQueue,a===null?t.retryQueue=new Set([l]):a.add(l)),Tr(e,l,o)),!1}throw Error(u(435,a.tag))}return Tr(e,l,o),ri(),!1}if(Ce)return t=Bt.current,t!==null?((t.flags&65536)===0&&(t.flags|=256),t.flags|=65536,t.lanes=o,l!==gs&&(e=Error(u(422),{cause:l}),Ml(Qt(e,a)))):(l!==gs&&(t=Error(u(423),{cause:l}),Ml(Qt(t,a))),e=e.current.alternate,e.flags|=65536,o&=-o,e.lanes|=o,l=Qt(l,a),o=Ps(e.stateNode,l,o),Cs(e,o),Pe!==4&&(Pe=2)),!1;var i=Error(u(520),{cause:l});if(i=Qt(i,a),Kl===null?Kl=[i]:Kl.push(i),Pe!==4&&(Pe=2),t===null)return!0;l=Qt(l,a),a=t;do{switch(a.tag){case 3:return a.flags|=65536,e=o&-o,a.lanes|=e,e=Ps(a.stateNode,l,e),Cs(a,e),!1;case 1:if(t=a.type,i=a.stateNode,(a.flags&128)===0&&(typeof t.getDerivedStateFromError=="function"||i!==null&&typeof i.componentDidCatch=="function"&&(Kn===null||!Kn.has(i))))return a.flags|=65536,o&=-o,a.lanes|=o,o=Cd(o),jd(o,e,a,l),Cs(a,o),!1}a=a.return}while(a!==null);return!1}var Fs=Error(u(461)),st=!1;function vt(e,t,a,l){t.child=e===null?zu(t,null,a,l):Ta(t,e.child,a,l)}function Md(e,t,a,l,o){a=a.render;var i=t.ref;if("ref"in l){var c={};for(var p in l)p!=="ref"&&(c[p]=l[p])}else c=l;return ba(t),l=ks(e,t,a,c,i,o),p=Rs(),e!==null&&!st?(Bs(e,t,o),xn(e,t,o)):(Ce&&p&&ms(t),t.flags|=1,vt(e,t,l,o),t.child)}function Dd(e,t,a,l,o){if(e===null){var i=a.type;return typeof i=="function"&&!ps(i)&&i.defaultProps===void 0&&a.compare===null?(t.tag=15,t.type=i,Od(e,t,i,l,o)):(e=Do(a.type,null,l,t,t.mode,o),e.ref=t.ref,e.return=t,t.child=e)}if(i=e.child,!sr(e,o)){var c=i.memoizedProps;if(a=a.compare,a=a!==null?a:Al,a(c,l)&&e.ref===t.ref)return xn(e,t,o)}return t.flags|=1,e=yn(i,l),e.ref=t.ref,e.return=t,t.child=e}function Od(e,t,a,l,o){if(e!==null){var i=e.memoizedProps;if(Al(i,l)&&e.ref===t.ref)if(st=!1,t.pendingProps=l=i,sr(e,o))(e.flags&131072)!==0&&(st=!0);else return t.lanes=e.lanes,xn(e,t,o)}return er(e,t,a,l,o)}function zd(e,t,a,l){var o=l.children,i=e!==null?e.memoizedState:null;if(e===null&&t.stateNode===null&&(t.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),l.mode==="hidden"){if((t.flags&128)!==0){if(i=i!==null?i.baseLanes|a:a,e!==null){for(l=t.child=e.child,o=0;l!==null;)o=o|l.lanes|l.childLanes,l=l.sibling;l=o&~i}else l=0,t.child=null;return kd(e,t,i,a,l)}if((a&536870912)!==0)t.memoizedState={baseLanes:0,cachePool:null},e!==null&&Ro(t,i!==null?i.cachePool:null),i!==null?Bu(t,i):Ms(),Uu(t);else return l=t.lanes=536870912,kd(e,t,i!==null?i.baseLanes|a:a,a,l)}else i!==null?(Ro(t,i.cachePool),Bu(t,i),Zn(),t.memoizedState=null):(e!==null&&Ro(t,null),Ms(),Zn());return vt(e,t,o,a),t.child}function Yl(e,t){return e!==null&&e.tag===22||t.stateNode!==null||(t.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),t.sibling}function kd(e,t,a,l,o){var i=ws();return i=i===null?null:{parent:ot._currentValue,pool:i},t.memoizedState={baseLanes:a,cachePool:i},e!==null&&Ro(t,null),Ms(),Uu(t),e!==null&&Va(e,t,l,!0),t.childLanes=o,null}function Wo(e,t){return t=Fo({mode:t.mode,children:t.children},e.mode),t.ref=e.ref,e.child=t,t.return=e,t}function Rd(e,t,a){return Ta(t,e.child,null,a),e=Wo(t,t.pendingProps),e.flags|=2,Ut(t),t.memoizedState=null,e}function xm(e,t,a){var l=t.pendingProps,o=(t.flags&128)!==0;if(t.flags&=-129,e===null){if(Ce){if(l.mode==="hidden")return e=Wo(t,l),t.lanes=536870912,Yl(null,e);if(Os(t),(e=Ve)?(e=Vp(e,Kt),e=e!==null&&e.data==="&"?e:null,e!==null&&(t.memoizedState={dehydrated:e,treeContext:Un!==null?{id:on,overflow:sn}:null,retryLane:536870912,hydrationErrors:null},a=vu(e),a.return=t,t.child=a,yt=t,Ve=null)):e=null,e===null)throw Hn(t);return t.lanes=536870912,null}return Wo(t,l)}var i=e.memoizedState;if(i!==null){var c=i.dehydrated;if(Os(t),o)if(t.flags&256)t.flags&=-257,t=Rd(e,t,a);else if(t.memoizedState!==null)t.child=e.child,t.flags|=128,t=null;else throw Error(u(558));else if(st||Va(e,t,a,!1),o=(a&e.childLanes)!==0,st||o){if(l=Ze,l!==null&&(c=Ec(l,a),c!==0&&c!==i.retryLane))throw i.retryLane=c,ma(e,c),Ot(l,e,c),Fs;ri(),t=Rd(e,t,a)}else e=i.treeContext,Ve=Wt(c.nextSibling),yt=t,Ce=!0,Gn=null,Kt=!1,e!==null&&Su(t,e),t=Wo(t,l),t.flags|=4096;return t}return e=yn(e.child,{mode:l.mode,children:l.children}),e.ref=t.ref,t.child=e,e.return=t,e}function Po(e,t){var a=t.ref;if(a===null)e!==null&&e.ref!==null&&(t.flags|=4194816);else{if(typeof a!="function"&&typeof a!="object")throw Error(u(284));(e===null||e.ref!==a)&&(t.flags|=4194816)}}function er(e,t,a,l,o){return ba(t),a=ks(e,t,a,l,void 0,o),l=Rs(),e!==null&&!st?(Bs(e,t,o),xn(e,t,o)):(Ce&&l&&ms(t),t.flags|=1,vt(e,t,a,o),t.child)}function Bd(e,t,a,l,o,i){return ba(t),t.updateQueue=null,a=Hu(t,l,a,o),Gu(e),l=Rs(),e!==null&&!st?(Bs(e,t,i),xn(e,t,i)):(Ce&&l&&ms(t),t.flags|=1,vt(e,t,a,i),t.child)}function Ud(e,t,a,l,o){if(ba(t),t.stateNode===null){var i=Xa,c=a.contextType;typeof c=="object"&&c!==null&&(i=gt(c)),i=new a(l,i),t.memoizedState=i.state!==null&&i.state!==void 0?i.state:null,i.updater=Ws,t.stateNode=i,i._reactInternals=t,i=t.stateNode,i.props=l,i.state=t.memoizedState,i.refs={},Ns(t),c=a.contextType,i.context=typeof c=="object"&&c!==null?gt(c):Xa,i.state=t.memoizedState,c=a.getDerivedStateFromProps,typeof c=="function"&&($s(t,a,c,l),i.state=t.memoizedState),typeof a.getDerivedStateFromProps=="function"||typeof i.getSnapshotBeforeUpdate=="function"||typeof i.UNSAFE_componentWillMount!="function"&&typeof i.componentWillMount!="function"||(c=i.state,typeof i.componentWillMount=="function"&&i.componentWillMount(),typeof i.UNSAFE_componentWillMount=="function"&&i.UNSAFE_componentWillMount(),c!==i.state&&Ws.enqueueReplaceState(i,i.state,null),Ul(t,l,i,o),Bl(),i.state=t.memoizedState),typeof i.componentDidMount=="function"&&(t.flags|=4194308),l=!0}else if(e===null){i=t.stateNode;var p=t.memoizedProps,_=Ea(a,p);i.props=_;var k=i.context,X=a.contextType;c=Xa,typeof X=="object"&&X!==null&&(c=gt(X));var Q=a.getDerivedStateFromProps;X=typeof Q=="function"||typeof i.getSnapshotBeforeUpdate=="function",p=t.pendingProps!==p,X||typeof i.UNSAFE_componentWillReceiveProps!="function"&&typeof i.componentWillReceiveProps!="function"||(p||k!==c)&&Td(t,i,l,c),qn=!1;var U=t.memoizedState;i.state=U,Ul(t,l,i,o),Bl(),k=t.memoizedState,p||U!==k||qn?(typeof Q=="function"&&($s(t,a,Q,l),k=t.memoizedState),(_=qn||xd(t,a,_,l,U,k,c))?(X||typeof i.UNSAFE_componentWillMount!="function"&&typeof i.componentWillMount!="function"||(typeof i.componentWillMount=="function"&&i.componentWillMount(),typeof i.UNSAFE_componentWillMount=="function"&&i.UNSAFE_componentWillMount()),typeof i.componentDidMount=="function"&&(t.flags|=4194308)):(typeof i.componentDidMount=="function"&&(t.flags|=4194308),t.memoizedProps=l,t.memoizedState=k),i.props=l,i.state=k,i.context=c,l=_):(typeof i.componentDidMount=="function"&&(t.flags|=4194308),l=!1)}else{i=t.stateNode,As(e,t),c=t.memoizedProps,X=Ea(a,c),i.props=X,Q=t.pendingProps,U=i.context,k=a.contextType,_=Xa,typeof k=="object"&&k!==null&&(_=gt(k)),p=a.getDerivedStateFromProps,(k=typeof p=="function"||typeof i.getSnapshotBeforeUpdate=="function")||typeof i.UNSAFE_componentWillReceiveProps!="function"&&typeof i.componentWillReceiveProps!="function"||(c!==Q||U!==_)&&Td(t,i,l,_),qn=!1,U=t.memoizedState,i.state=U,Ul(t,l,i,o),Bl();var H=t.memoizedState;c!==Q||U!==H||qn||e!==null&&e.dependencies!==null&&zo(e.dependencies)?(typeof p=="function"&&($s(t,a,p,l),H=t.memoizedState),(X=qn||xd(t,a,X,l,U,H,_)||e!==null&&e.dependencies!==null&&zo(e.dependencies))?(k||typeof i.UNSAFE_componentWillUpdate!="function"&&typeof i.componentWillUpdate!="function"||(typeof i.componentWillUpdate=="function"&&i.componentWillUpdate(l,H,_),typeof i.UNSAFE_componentWillUpdate=="function"&&i.UNSAFE_componentWillUpdate(l,H,_)),typeof i.componentDidUpdate=="function"&&(t.flags|=4),typeof i.getSnapshotBeforeUpdate=="function"&&(t.flags|=1024)):(typeof i.componentDidUpdate!="function"||c===e.memoizedProps&&U===e.memoizedState||(t.flags|=4),typeof i.getSnapshotBeforeUpdate!="function"||c===e.memoizedProps&&U===e.memoizedState||(t.flags|=1024),t.memoizedProps=l,t.memoizedState=H),i.props=l,i.state=H,i.context=_,l=X):(typeof i.componentDidUpdate!="function"||c===e.memoizedProps&&U===e.memoizedState||(t.flags|=4),typeof i.getSnapshotBeforeUpdate!="function"||c===e.memoizedProps&&U===e.memoizedState||(t.flags|=1024),l=!1)}return i=l,Po(e,t),l=(t.flags&128)!==0,i||l?(i=t.stateNode,a=l&&typeof a.getDerivedStateFromError!="function"?null:i.render(),t.flags|=1,e!==null&&l?(t.child=Ta(t,e.child,null,o),t.child=Ta(t,null,a,o)):vt(e,t,a,o),t.memoizedState=i.state,e=t.child):e=xn(e,t,o),e}function Gd(e,t,a,l){return ga(),t.flags|=256,vt(e,t,a,l),t.child}var tr={dehydrated:null,treeContext:null,retryLane:0,hydrationErrors:null};function nr(e){return{baseLanes:e,cachePool:Au()}}function ar(e,t,a){return e=e!==null?e.childLanes&~a:0,t&&(e|=Ht),e}function Hd(e,t,a){var l=t.pendingProps,o=!1,i=(t.flags&128)!==0,c;if((c=i)||(c=e!==null&&e.memoizedState===null?!1:(tt.current&2)!==0),c&&(o=!0,t.flags&=-129),c=(t.flags&32)!==0,t.flags&=-33,e===null){if(Ce){if(o?Jn(t):Zn(),(e=Ve)?(e=Vp(e,Kt),e=e!==null&&e.data!=="&"?e:null,e!==null&&(t.memoizedState={dehydrated:e,treeContext:Un!==null?{id:on,overflow:sn}:null,retryLane:536870912,hydrationErrors:null},a=vu(e),a.return=t,t.child=a,yt=t,Ve=null)):e=null,e===null)throw Hn(t);return Hr(e)?t.lanes=32:t.lanes=536870912,null}var p=l.children;return l=l.fallback,o?(Zn(),o=t.mode,p=Fo({mode:"hidden",children:p},o),l=ya(l,o,a,null),p.return=t,l.return=t,p.sibling=l,t.child=p,l=t.child,l.memoizedState=nr(a),l.childLanes=ar(e,c,a),t.memoizedState=tr,Yl(null,l)):(Jn(t),lr(t,p))}var _=e.memoizedState;if(_!==null&&(p=_.dehydrated,p!==null)){if(i)t.flags&256?(Jn(t),t.flags&=-257,t=or(e,t,a)):t.memoizedState!==null?(Zn(),t.child=e.child,t.flags|=128,t=null):(Zn(),p=l.fallback,o=t.mode,l=Fo({mode:"visible",children:l.children},o),p=ya(p,o,a,null),p.flags|=2,l.return=t,p.return=t,l.sibling=p,t.child=l,Ta(t,e.child,null,a),l=t.child,l.memoizedState=nr(a),l.childLanes=ar(e,c,a),t.memoizedState=tr,t=Yl(null,l));else if(Jn(t),Hr(p)){if(c=p.nextSibling&&p.nextSibling.dataset,c)var k=c.dgst;c=k,l=Error(u(419)),l.stack="",l.digest=c,Ml({value:l,source:null,stack:null}),t=or(e,t,a)}else if(st||Va(e,t,a,!1),c=(a&e.childLanes)!==0,st||c){if(c=Ze,c!==null&&(l=Ec(c,a),l!==0&&l!==_.retryLane))throw _.retryLane=l,ma(e,l),Ot(c,e,l),Fs;Gr(p)||ri(),t=or(e,t,a)}else Gr(p)?(t.flags|=192,t.child=e.child,t=null):(e=_.treeContext,Ve=Wt(p.nextSibling),yt=t,Ce=!0,Gn=null,Kt=!1,e!==null&&Su(t,e),t=lr(t,l.children),t.flags|=4096);return t}return o?(Zn(),p=l.fallback,o=t.mode,_=e.child,k=_.sibling,l=yn(_,{mode:"hidden",children:l.children}),l.subtreeFlags=_.subtreeFlags&65011712,k!==null?p=yn(k,p):(p=ya(p,o,a,null),p.flags|=2),p.return=t,l.return=t,l.sibling=p,t.child=l,Yl(null,l),l=t.child,p=e.child.memoizedState,p===null?p=nr(a):(o=p.cachePool,o!==null?(_=ot._currentValue,o=o.parent!==_?{parent:_,pool:_}:o):o=Au(),p={baseLanes:p.baseLanes|a,cachePool:o}),l.memoizedState=p,l.childLanes=ar(e,c,a),t.memoizedState=tr,Yl(e.child,l)):(Jn(t),a=e.child,e=a.sibling,a=yn(a,{mode:"visible",children:l.children}),a.return=t,a.sibling=null,e!==null&&(c=t.deletions,c===null?(t.deletions=[e],t.flags|=16):c.push(e)),t.child=a,t.memoizedState=null,a)}function lr(e,t){return t=Fo({mode:"visible",children:t},e.mode),t.return=e,e.child=t}function Fo(e,t){return e=Rt(22,e,null,t),e.lanes=0,e}function or(e,t,a){return Ta(t,e.child,null,a),e=lr(t,t.pendingProps.children),e.flags|=2,t.memoizedState=null,e}function Ld(e,t,a){e.lanes|=t;var l=e.alternate;l!==null&&(l.lanes|=t),_s(e.return,t,a)}function ir(e,t,a,l,o,i){var c=e.memoizedState;c===null?e.memoizedState={isBackwards:t,rendering:null,renderingStartTime:0,last:l,tail:a,tailMode:o,treeForkCount:i}:(c.isBackwards=t,c.rendering=null,c.renderingStartTime=0,c.last=l,c.tail=a,c.tailMode=o,c.treeForkCount=i)}function qd(e,t,a){var l=t.pendingProps,o=l.revealOrder,i=l.tail;l=l.children;var c=tt.current,p=(c&2)!==0;if(p?(c=c&1|2,t.flags|=128):c&=1,F(tt,c),vt(e,t,l,a),l=Ce?jl:0,!p&&e!==null&&(e.flags&128)!==0)e:for(e=t.child;e!==null;){if(e.tag===13)e.memoizedState!==null&&Ld(e,a,t);else if(e.tag===19)Ld(e,a,t);else if(e.child!==null){e.child.return=e,e=e.child;continue}if(e===t)break e;for(;e.sibling===null;){if(e.return===null||e.return===t)break e;e=e.return}e.sibling.return=e.return,e=e.sibling}switch(o){case"forwards":for(a=t.child,o=null;a!==null;)e=a.alternate,e!==null&&qo(e)===null&&(o=a),a=a.sibling;a=o,a===null?(o=t.child,t.child=null):(o=a.sibling,a.sibling=null),ir(t,!1,o,a,i,l);break;case"backwards":case"unstable_legacy-backwards":for(a=null,o=t.child,t.child=null;o!==null;){if(e=o.alternate,e!==null&&qo(e)===null){t.child=o;break}e=o.sibling,o.sibling=a,a=o,o=e}ir(t,!0,a,null,i,l);break;case"together":ir(t,!1,null,null,void 0,l);break;default:t.memoizedState=null}return t.child}function xn(e,t,a){if(e!==null&&(t.dependencies=e.dependencies),In|=t.lanes,(a&t.childLanes)===0)if(e!==null){if(Va(e,t,a,!1),(a&t.childLanes)===0)return null}else return null;if(e!==null&&t.child!==e.child)throw Error(u(153));if(t.child!==null){for(e=t.child,a=yn(e,e.pendingProps),t.child=a,a.return=t;e.sibling!==null;)e=e.sibling,a=a.sibling=yn(e,e.pendingProps),a.return=t;a.sibling=null}return t.child}function sr(e,t){return(e.lanes&t)!==0?!0:(e=e.dependencies,!!(e!==null&&zo(e)))}function Tm(e,t,a){switch(t.tag){case 3:qe(t,t.stateNode.containerInfo),Ln(t,ot,e.memoizedState.cache),ga();break;case 27:case 5:le(t);break;case 4:qe(t,t.stateNode.containerInfo);break;case 10:Ln(t,t.type,t.memoizedProps.value);break;case 31:if(t.memoizedState!==null)return t.flags|=128,Os(t),null;break;case 13:var l=t.memoizedState;if(l!==null)return l.dehydrated!==null?(Jn(t),t.flags|=128,null):(a&t.child.childLanes)!==0?Hd(e,t,a):(Jn(t),e=xn(e,t,a),e!==null?e.sibling:null);Jn(t);break;case 19:var o=(e.flags&128)!==0;if(l=(a&t.childLanes)!==0,l||(Va(e,t,a,!1),l=(a&t.childLanes)!==0),o){if(l)return qd(e,t,a);t.flags|=128}if(o=t.memoizedState,o!==null&&(o.rendering=null,o.tail=null,o.lastEffect=null),F(tt,tt.current),l)break;return null;case 22:return t.lanes=0,zd(e,t,a,t.pendingProps);case 24:Ln(t,ot,e.memoizedState.cache)}return xn(e,t,a)}function Yd(e,t,a){if(e!==null)if(e.memoizedProps!==t.pendingProps)st=!0;else{if(!sr(e,a)&&(t.flags&128)===0)return st=!1,Tm(e,t,a);st=(e.flags&131072)!==0}else st=!1,Ce&&(t.flags&1048576)!==0&&_u(t,jl,t.index);switch(t.lanes=0,t.tag){case 16:e:{var l=t.pendingProps;if(e=Sa(t.elementType),t.type=e,typeof e=="function")ps(e)?(l=Ea(e,l),t.tag=1,t=Ud(null,t,e,l,a)):(t.tag=0,t=er(null,t,e,l,a));else{if(e!=null){var o=e.$$typeof;if(o===L){t.tag=11,t=Md(null,t,e,l,a);break e}else if(o===B){t.tag=14,t=Dd(null,t,e,l,a);break e}}throw t=ce(e)||e,Error(u(306,t,""))}}return t;case 0:return er(e,t,t.type,t.pendingProps,a);case 1:return l=t.type,o=Ea(l,t.pendingProps),Ud(e,t,l,o,a);case 3:e:{if(qe(t,t.stateNode.containerInfo),e===null)throw Error(u(387));l=t.pendingProps;var i=t.memoizedState;o=i.element,As(e,t),Ul(t,l,null,a);var c=t.memoizedState;if(l=c.cache,Ln(t,ot,l),l!==i.cache&&Ss(t,[ot],a,!0),Bl(),l=c.element,i.isDehydrated)if(i={element:l,isDehydrated:!1,cache:c.cache},t.updateQueue.baseState=i,t.memoizedState=i,t.flags&256){t=Gd(e,t,l,a);break e}else if(l!==o){o=Qt(Error(u(424)),t),Ml(o),t=Gd(e,t,l,a);break e}else{switch(e=t.stateNode.containerInfo,e.nodeType){case 9:e=e.body;break;default:e=e.nodeName==="HTML"?e.ownerDocument.body:e}for(Ve=Wt(e.firstChild),yt=t,Ce=!0,Gn=null,Kt=!0,a=zu(t,null,l,a),t.child=a;a;)a.flags=a.flags&-3|4096,a=a.sibling}else{if(ga(),l===o){t=xn(e,t,a);break e}vt(e,t,l,a)}t=t.child}return t;case 26:return Po(e,t),e===null?(a=Fp(t.type,null,t.pendingProps,null))?t.memoizedState=a:Ce||(a=t.type,e=t.pendingProps,l=mi(xe.current).createElement(a),l[mt]=t,l[Nt]=e,bt(l,a,e),pt(l),t.stateNode=l):t.memoizedState=Fp(t.type,e.memoizedProps,t.pendingProps,e.memoizedState),null;case 27:return le(t),e===null&&Ce&&(l=t.stateNode=$p(t.type,t.pendingProps,xe.current),yt=t,Kt=!0,o=Ve,Fn(t.type)?(Lr=o,Ve=Wt(l.firstChild)):Ve=o),vt(e,t,t.pendingProps.children,a),Po(e,t),e===null&&(t.flags|=4194304),t.child;case 5:return e===null&&Ce&&((o=l=Ve)&&(l=Fm(l,t.type,t.pendingProps,Kt),l!==null?(t.stateNode=l,yt=t,Ve=Wt(l.firstChild),Kt=!1,o=!0):o=!1),o||Hn(t)),le(t),o=t.type,i=t.pendingProps,c=e!==null?e.memoizedProps:null,l=i.children,Rr(o,i)?l=null:c!==null&&Rr(o,c)&&(t.flags|=32),t.memoizedState!==null&&(o=ks(e,t,hm,null,null,a),ao._currentValue=o),Po(e,t),vt(e,t,l,a),t.child;case 6:return e===null&&Ce&&((e=a=Ve)&&(a=ey(a,t.pendingProps,Kt),a!==null?(t.stateNode=a,yt=t,Ve=null,e=!0):e=!1),e||Hn(t)),null;case 13:return Hd(e,t,a);case 4:return qe(t,t.stateNode.containerInfo),l=t.pendingProps,e===null?t.child=Ta(t,null,l,a):vt(e,t,l,a),t.child;case 11:return Md(e,t,t.type,t.pendingProps,a);case 7:return vt(e,t,t.pendingProps,a),t.child;case 8:return vt(e,t,t.pendingProps.children,a),t.child;case 12:return vt(e,t,t.pendingProps.children,a),t.child;case 10:return l=t.pendingProps,Ln(t,t.type,l.value),vt(e,t,l.children,a),t.child;case 9:return o=t.type._context,l=t.pendingProps.children,ba(t),o=gt(o),l=l(o),t.flags|=1,vt(e,t,l,a),t.child;case 14:return Dd(e,t,t.type,t.pendingProps,a);case 15:return Od(e,t,t.type,t.pendingProps,a);case 19:return qd(e,t,a);case 31:return xm(e,t,a);case 22:return zd(e,t,a,t.pendingProps);case 24:return ba(t),l=gt(ot),e===null?(o=ws(),o===null&&(o=Ze,i=xs(),o.pooledCache=i,i.refCount++,i!==null&&(o.pooledCacheLanes|=a),o=i),t.memoizedState={parent:l,cache:o},Ns(t),Ln(t,ot,o)):((e.lanes&a)!==0&&(As(e,t),Ul(t,null,null,a),Bl()),o=e.memoizedState,i=t.memoizedState,o.parent!==l?(o={parent:l,cache:l},t.memoizedState=o,t.lanes===0&&(t.memoizedState=t.updateQueue.baseState=o),Ln(t,ot,l)):(l=i.cache,Ln(t,ot,l),l!==o.cache&&Ss(t,[ot],a,!0))),vt(e,t,t.pendingProps.children,a),t.child;case 29:throw t.pendingProps}throw Error(u(156,t.tag))}function Tn(e){e.flags|=4}function rr(e,t,a,l,o){if((t=(e.mode&32)!==0)&&(t=!1),t){if(e.flags|=16777216,(o&335544128)===o)if(e.stateNode.complete)e.flags|=8192;else if(mp())e.flags|=8192;else throw xa=Uo,Es}else e.flags&=-16777217}function Xd(e,t){if(t.type!=="stylesheet"||(t.state.loading&4)!==0)e.flags&=-16777217;else if(e.flags|=16777216,!lf(t))if(mp())e.flags|=8192;else throw xa=Uo,Es}function ei(e,t){t!==null&&(e.flags|=4),e.flags&16384&&(t=e.tag!==22?xc():536870912,e.lanes|=t,ol|=t)}function Xl(e,t){if(!Ce)switch(e.tailMode){case"hidden":t=e.tail;for(var a=null;t!==null;)t.alternate!==null&&(a=t),t=t.sibling;a===null?e.tail=null:a.sibling=null;break;case"collapsed":a=e.tail;for(var l=null;a!==null;)a.alternate!==null&&(l=a),a=a.sibling;l===null?t||e.tail===null?e.tail=null:e.tail.sibling=null:l.sibling=null}}function Ie(e){var t=e.alternate!==null&&e.alternate.child===e.child,a=0,l=0;if(t)for(var o=e.child;o!==null;)a|=o.lanes|o.childLanes,l|=o.subtreeFlags&65011712,l|=o.flags&65011712,o.return=e,o=o.sibling;else for(o=e.child;o!==null;)a|=o.lanes|o.childLanes,l|=o.subtreeFlags,l|=o.flags,o.return=e,o=o.sibling;return e.subtreeFlags|=l,e.childLanes=a,t}function wm(e,t,a){var l=t.pendingProps;switch(ys(t),t.tag){case 16:case 15:case 0:case 11:case 7:case 8:case 12:case 9:case 14:return Ie(t),null;case 1:return Ie(t),null;case 3:return a=t.stateNode,l=null,e!==null&&(l=e.memoizedState.cache),t.memoizedState.cache!==l&&(t.flags|=2048),bn(ot),Me(),a.pendingContext&&(a.context=a.pendingContext,a.pendingContext=null),(e===null||e.child===null)&&(Qa(t)?Tn(t):e===null||e.memoizedState.isDehydrated&&(t.flags&256)===0||(t.flags|=1024,vs())),Ie(t),null;case 26:var o=t.type,i=t.memoizedState;return e===null?(Tn(t),i!==null?(Ie(t),Xd(t,i)):(Ie(t),rr(t,o,null,l,a))):i?i!==e.memoizedState?(Tn(t),Ie(t),Xd(t,i)):(Ie(t),t.flags&=-16777217):(e=e.memoizedProps,e!==l&&Tn(t),Ie(t),rr(t,o,e,l,a)),null;case 27:if(de(t),a=xe.current,o=t.type,e!==null&&t.stateNode!=null)e.memoizedProps!==l&&Tn(t);else{if(!l){if(t.stateNode===null)throw Error(u(166));return Ie(t),null}e=me.current,Qa(t)?xu(t):(e=$p(o,l,a),t.stateNode=e,Tn(t))}return Ie(t),null;case 5:if(de(t),o=t.type,e!==null&&t.stateNode!=null)e.memoizedProps!==l&&Tn(t);else{if(!l){if(t.stateNode===null)throw Error(u(166));return Ie(t),null}if(i=me.current,Qa(t))xu(t);else{var c=mi(xe.current);switch(i){case 1:i=c.createElementNS("http://www.w3.org/2000/svg",o);break;case 2:i=c.createElementNS("http://www.w3.org/1998/Math/MathML",o);break;default:switch(o){case"svg":i=c.createElementNS("http://www.w3.org/2000/svg",o);break;case"math":i=c.createElementNS("http://www.w3.org/1998/Math/MathML",o);break;case"script":i=c.createElement("div"),i.innerHTML="<script><\/script>",i=i.removeChild(i.firstChild);break;case"select":i=typeof l.is=="string"?c.createElement("select",{is:l.is}):c.createElement("select"),l.multiple?i.multiple=!0:l.size&&(i.size=l.size);break;default:i=typeof l.is=="string"?c.createElement(o,{is:l.is}):c.createElement(o)}}i[mt]=t,i[Nt]=l;e:for(c=t.child;c!==null;){if(c.tag===5||c.tag===6)i.appendChild(c.stateNode);else if(c.tag!==4&&c.tag!==27&&c.child!==null){c.child.return=c,c=c.child;continue}if(c===t)break e;for(;c.sibling===null;){if(c.return===null||c.return===t)break e;c=c.return}c.sibling.return=c.return,c=c.sibling}t.stateNode=i;e:switch(bt(i,o,l),o){case"button":case"input":case"select":case"textarea":l=!!l.autoFocus;break e;case"img":l=!0;break e;default:l=!1}l&&Tn(t)}}return Ie(t),rr(t,t.type,e===null?null:e.memoizedProps,t.pendingProps,a),null;case 6:if(e&&t.stateNode!=null)e.memoizedProps!==l&&Tn(t);else{if(typeof l!="string"&&t.stateNode===null)throw Error(u(166));if(e=xe.current,Qa(t)){if(e=t.stateNode,a=t.memoizedProps,l=null,o=yt,o!==null)switch(o.tag){case 27:case 5:l=o.memoizedProps}e[mt]=t,e=!!(e.nodeValue===a||l!==null&&l.suppressHydrationWarning===!0||Hp(e.nodeValue,a)),e||Hn(t,!0)}else e=mi(e).createTextNode(l),e[mt]=t,t.stateNode=e}return Ie(t),null;case 31:if(a=t.memoizedState,e===null||e.memoizedState!==null){if(l=Qa(t),a!==null){if(e===null){if(!l)throw Error(u(318));if(e=t.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(u(557));e[mt]=t}else ga(),(t.flags&128)===0&&(t.memoizedState=null),t.flags|=4;Ie(t),e=!1}else a=vs(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=a),e=!0;if(!e)return t.flags&256?(Ut(t),t):(Ut(t),null);if((t.flags&128)!==0)throw Error(u(558))}return Ie(t),null;case 13:if(l=t.memoizedState,e===null||e.memoizedState!==null&&e.memoizedState.dehydrated!==null){if(o=Qa(t),l!==null&&l.dehydrated!==null){if(e===null){if(!o)throw Error(u(318));if(o=t.memoizedState,o=o!==null?o.dehydrated:null,!o)throw Error(u(317));o[mt]=t}else ga(),(t.flags&128)===0&&(t.memoizedState=null),t.flags|=4;Ie(t),o=!1}else o=vs(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=o),o=!0;if(!o)return t.flags&256?(Ut(t),t):(Ut(t),null)}return Ut(t),(t.flags&128)!==0?(t.lanes=a,t):(a=l!==null,e=e!==null&&e.memoizedState!==null,a&&(l=t.child,o=null,l.alternate!==null&&l.alternate.memoizedState!==null&&l.alternate.memoizedState.cachePool!==null&&(o=l.alternate.memoizedState.cachePool.pool),i=null,l.memoizedState!==null&&l.memoizedState.cachePool!==null&&(i=l.memoizedState.cachePool.pool),i!==o&&(l.flags|=2048)),a!==e&&a&&(t.child.flags|=8192),ei(t,t.updateQueue),Ie(t),null);case 4:return Me(),e===null&&Mr(t.stateNode.containerInfo),Ie(t),null;case 10:return bn(t.type),Ie(t),null;case 19:if(te(tt),l=t.memoizedState,l===null)return Ie(t),null;if(o=(t.flags&128)!==0,i=l.rendering,i===null)if(o)Xl(l,!1);else{if(Pe!==0||e!==null&&(e.flags&128)!==0)for(e=t.child;e!==null;){if(i=qo(e),i!==null){for(t.flags|=128,Xl(l,!1),e=i.updateQueue,t.updateQueue=e,ei(t,e),t.subtreeFlags=0,e=a,a=t.child;a!==null;)gu(a,e),a=a.sibling;return F(tt,tt.current&1|2),Ce&&gn(t,l.treeForkCount),t.child}e=e.sibling}l.tail!==null&&ge()>oi&&(t.flags|=128,o=!0,Xl(l,!1),t.lanes=4194304)}else{if(!o)if(e=qo(i),e!==null){if(t.flags|=128,o=!0,e=e.updateQueue,t.updateQueue=e,ei(t,e),Xl(l,!0),l.tail===null&&l.tailMode==="hidden"&&!i.alternate&&!Ce)return Ie(t),null}else 2*ge()-l.renderingStartTime>oi&&a!==536870912&&(t.flags|=128,o=!0,Xl(l,!1),t.lanes=4194304);l.isBackwards?(i.sibling=t.child,t.child=i):(e=l.last,e!==null?e.sibling=i:t.child=i,l.last=i)}return l.tail!==null?(e=l.tail,l.rendering=e,l.tail=e.sibling,l.renderingStartTime=ge(),e.sibling=null,a=tt.current,F(tt,o?a&1|2:a&1),Ce&&gn(t,l.treeForkCount),e):(Ie(t),null);case 22:case 23:return Ut(t),Ds(),l=t.memoizedState!==null,e!==null?e.memoizedState!==null!==l&&(t.flags|=8192):l&&(t.flags|=8192),l?(a&536870912)!==0&&(t.flags&128)===0&&(Ie(t),t.subtreeFlags&6&&(t.flags|=8192)):Ie(t),a=t.updateQueue,a!==null&&ei(t,a.retryQueue),a=null,e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(a=e.memoizedState.cachePool.pool),l=null,t.memoizedState!==null&&t.memoizedState.cachePool!==null&&(l=t.memoizedState.cachePool.pool),l!==a&&(t.flags|=2048),e!==null&&te(_a),null;case 24:return a=null,e!==null&&(a=e.memoizedState.cache),t.memoizedState.cache!==a&&(t.flags|=2048),bn(ot),Ie(t),null;case 25:return null;case 30:return null}throw Error(u(156,t.tag))}function Em(e,t){switch(ys(t),t.tag){case 1:return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 3:return bn(ot),Me(),e=t.flags,(e&65536)!==0&&(e&128)===0?(t.flags=e&-65537|128,t):null;case 26:case 27:case 5:return de(t),null;case 31:if(t.memoizedState!==null){if(Ut(t),t.alternate===null)throw Error(u(340));ga()}return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 13:if(Ut(t),e=t.memoizedState,e!==null&&e.dehydrated!==null){if(t.alternate===null)throw Error(u(340));ga()}return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 19:return te(tt),null;case 4:return Me(),null;case 10:return bn(t.type),null;case 22:case 23:return Ut(t),Ds(),e!==null&&te(_a),e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 24:return bn(ot),null;case 25:return null;default:return null}}function Jd(e,t){switch(ys(t),t.tag){case 3:bn(ot),Me();break;case 26:case 27:case 5:de(t);break;case 4:Me();break;case 31:t.memoizedState!==null&&Ut(t);break;case 13:Ut(t);break;case 19:te(tt);break;case 10:bn(t.type);break;case 22:case 23:Ut(t),Ds(),e!==null&&te(_a);break;case 24:bn(ot)}}function Jl(e,t){try{var a=t.updateQueue,l=a!==null?a.lastEffect:null;if(l!==null){var o=l.next;a=o;do{if((a.tag&e)===e){l=void 0;var i=a.create,c=a.inst;l=i(),c.destroy=l}a=a.next}while(a!==o)}}catch(p){He(t,t.return,p)}}function Qn(e,t,a){try{var l=t.updateQueue,o=l!==null?l.lastEffect:null;if(o!==null){var i=o.next;l=i;do{if((l.tag&e)===e){var c=l.inst,p=c.destroy;if(p!==void 0){c.destroy=void 0,o=t;var _=a,k=p;try{k()}catch(X){He(o,_,X)}}}l=l.next}while(l!==i)}}catch(X){He(t,t.return,X)}}function Zd(e){var t=e.updateQueue;if(t!==null){var a=e.stateNode;try{Ru(t,a)}catch(l){He(e,e.return,l)}}}function Qd(e,t,a){a.props=Ea(e.type,e.memoizedProps),a.state=e.memoizedState;try{a.componentWillUnmount()}catch(l){He(e,t,l)}}function Zl(e,t){try{var a=e.ref;if(a!==null){switch(e.tag){case 26:case 27:case 5:var l=e.stateNode;break;case 30:l=e.stateNode;break;default:l=e.stateNode}typeof a=="function"?e.refCleanup=a(l):a.current=l}}catch(o){He(e,t,o)}}function rn(e,t){var a=e.ref,l=e.refCleanup;if(a!==null)if(typeof l=="function")try{l()}catch(o){He(e,t,o)}finally{e.refCleanup=null,e=e.alternate,e!=null&&(e.refCleanup=null)}else if(typeof a=="function")try{a(null)}catch(o){He(e,t,o)}else a.current=null}function Vd(e){var t=e.type,a=e.memoizedProps,l=e.stateNode;try{e:switch(t){case"button":case"input":case"select":case"textarea":a.autoFocus&&l.focus();break e;case"img":a.src?l.src=a.src:a.srcSet&&(l.srcset=a.srcSet)}}catch(o){He(e,e.return,o)}}function cr(e,t,a){try{var l=e.stateNode;Vm(l,e.type,a,t),l[Nt]=t}catch(o){He(e,e.return,o)}}function Id(e){return e.tag===5||e.tag===3||e.tag===26||e.tag===27&&Fn(e.type)||e.tag===4}function ur(e){e:for(;;){for(;e.sibling===null;){if(e.return===null||Id(e.return))return null;e=e.return}for(e.sibling.return=e.return,e=e.sibling;e.tag!==5&&e.tag!==6&&e.tag!==18;){if(e.tag===27&&Fn(e.type)||e.flags&2||e.child===null||e.tag===4)continue e;e.child.return=e,e=e.child}if(!(e.flags&2))return e.stateNode}}function dr(e,t,a){var l=e.tag;if(l===5||l===6)e=e.stateNode,t?(a.nodeType===9?a.body:a.nodeName==="HTML"?a.ownerDocument.body:a).insertBefore(e,t):(t=a.nodeType===9?a.body:a.nodeName==="HTML"?a.ownerDocument.body:a,t.appendChild(e),a=a._reactRootContainer,a!=null||t.onclick!==null||(t.onclick=hn));else if(l!==4&&(l===27&&Fn(e.type)&&(a=e.stateNode,t=null),e=e.child,e!==null))for(dr(e,t,a),e=e.sibling;e!==null;)dr(e,t,a),e=e.sibling}function ti(e,t,a){var l=e.tag;if(l===5||l===6)e=e.stateNode,t?a.insertBefore(e,t):a.appendChild(e);else if(l!==4&&(l===27&&Fn(e.type)&&(a=e.stateNode),e=e.child,e!==null))for(ti(e,t,a),e=e.sibling;e!==null;)ti(e,t,a),e=e.sibling}function Kd(e){var t=e.stateNode,a=e.memoizedProps;try{for(var l=e.type,o=t.attributes;o.length;)t.removeAttributeNode(o[0]);bt(t,l,a),t[mt]=e,t[Nt]=a}catch(i){He(e,e.return,i)}}var wn=!1,rt=!1,pr=!1,$d=typeof WeakSet=="function"?WeakSet:Set,ft=null;function Nm(e,t){if(e=e.containerInfo,zr=xi,e=ru(e),os(e)){if("selectionStart"in e)var a={start:e.selectionStart,end:e.selectionEnd};else e:{a=(a=e.ownerDocument)&&a.defaultView||window;var l=a.getSelection&&a.getSelection();if(l&&l.rangeCount!==0){a=l.anchorNode;var o=l.anchorOffset,i=l.focusNode;l=l.focusOffset;try{a.nodeType,i.nodeType}catch{a=null;break e}var c=0,p=-1,_=-1,k=0,X=0,Q=e,U=null;t:for(;;){for(var H;Q!==a||o!==0&&Q.nodeType!==3||(p=c+o),Q!==i||l!==0&&Q.nodeType!==3||(_=c+l),Q.nodeType===3&&(c+=Q.nodeValue.length),(H=Q.firstChild)!==null;)U=Q,Q=H;for(;;){if(Q===e)break t;if(U===a&&++k===o&&(p=c),U===i&&++X===l&&(_=c),(H=Q.nextSibling)!==null)break;Q=U,U=Q.parentNode}Q=H}a=p===-1||_===-1?null:{start:p,end:_}}else a=null}a=a||{start:0,end:0}}else a=null;for(kr={focusedElem:e,selectionRange:a},xi=!1,ft=t;ft!==null;)if(t=ft,e=t.child,(t.subtreeFlags&1028)!==0&&e!==null)e.return=t,ft=e;else for(;ft!==null;){switch(t=ft,i=t.alternate,e=t.flags,t.tag){case 0:if((e&4)!==0&&(e=t.updateQueue,e=e!==null?e.events:null,e!==null))for(a=0;a<e.length;a++)o=e[a],o.ref.impl=o.nextImpl;break;case 11:case 15:break;case 1:if((e&1024)!==0&&i!==null){e=void 0,a=t,o=i.memoizedProps,i=i.memoizedState,l=a.stateNode;try{var ae=Ea(a.type,o);e=l.getSnapshotBeforeUpdate(ae,i),l.__reactInternalSnapshotBeforeUpdate=e}catch(pe){He(a,a.return,pe)}}break;case 3:if((e&1024)!==0){if(e=t.stateNode.containerInfo,a=e.nodeType,a===9)Ur(e);else if(a===1)switch(e.nodeName){case"HEAD":case"HTML":case"BODY":Ur(e);break;default:e.textContent=""}}break;case 5:case 26:case 27:case 6:case 4:case 17:break;default:if((e&1024)!==0)throw Error(u(163))}if(e=t.sibling,e!==null){e.return=t.return,ft=e;break}ft=t.return}}function Wd(e,t,a){var l=a.flags;switch(a.tag){case 0:case 11:case 15:Nn(e,a),l&4&&Jl(5,a);break;case 1:if(Nn(e,a),l&4)if(e=a.stateNode,t===null)try{e.componentDidMount()}catch(c){He(a,a.return,c)}else{var o=Ea(a.type,t.memoizedProps);t=t.memoizedState;try{e.componentDidUpdate(o,t,e.__reactInternalSnapshotBeforeUpdate)}catch(c){He(a,a.return,c)}}l&64&&Zd(a),l&512&&Zl(a,a.return);break;case 3:if(Nn(e,a),l&64&&(e=a.updateQueue,e!==null)){if(t=null,a.child!==null)switch(a.child.tag){case 27:case 5:t=a.child.stateNode;break;case 1:t=a.child.stateNode}try{Ru(e,t)}catch(c){He(a,a.return,c)}}break;case 27:t===null&&l&4&&Kd(a);case 26:case 5:Nn(e,a),t===null&&l&4&&Vd(a),l&512&&Zl(a,a.return);break;case 12:Nn(e,a);break;case 31:Nn(e,a),l&4&&ep(e,a);break;case 13:Nn(e,a),l&4&&tp(e,a),l&64&&(e=a.memoizedState,e!==null&&(e=e.dehydrated,e!==null&&(a=Rm.bind(null,a),ty(e,a))));break;case 22:if(l=a.memoizedState!==null||wn,!l){t=t!==null&&t.memoizedState!==null||rt,o=wn;var i=rt;wn=l,(rt=t)&&!i?An(e,a,(a.subtreeFlags&8772)!==0):Nn(e,a),wn=o,rt=i}break;case 30:break;default:Nn(e,a)}}function Pd(e){var t=e.alternate;t!==null&&(e.alternate=null,Pd(t)),e.child=null,e.deletions=null,e.sibling=null,e.tag===5&&(t=e.stateNode,t!==null&&Yi(t)),e.stateNode=null,e.return=null,e.dependencies=null,e.memoizedProps=null,e.memoizedState=null,e.pendingProps=null,e.stateNode=null,e.updateQueue=null}var $e=null,Ct=!1;function En(e,t,a){for(a=a.child;a!==null;)Fd(e,t,a),a=a.sibling}function Fd(e,t,a){if(St&&typeof St.onCommitFiberUnmount=="function")try{St.onCommitFiberUnmount(ca,a)}catch{}switch(a.tag){case 26:rt||rn(a,t),En(e,t,a),a.memoizedState?a.memoizedState.count--:a.stateNode&&(a=a.stateNode,a.parentNode.removeChild(a));break;case 27:rt||rn(a,t);var l=$e,o=Ct;Fn(a.type)&&($e=a.stateNode,Ct=!1),En(e,t,a),eo(a.stateNode),$e=l,Ct=o;break;case 5:rt||rn(a,t);case 6:if(l=$e,o=Ct,$e=null,En(e,t,a),$e=l,Ct=o,$e!==null)if(Ct)try{($e.nodeType===9?$e.body:$e.nodeName==="HTML"?$e.ownerDocument.body:$e).removeChild(a.stateNode)}catch(i){He(a,t,i)}else try{$e.removeChild(a.stateNode)}catch(i){He(a,t,i)}break;case 18:$e!==null&&(Ct?(e=$e,Zp(e.nodeType===9?e.body:e.nodeName==="HTML"?e.ownerDocument.body:e,a.stateNode),fl(e)):Zp($e,a.stateNode));break;case 4:l=$e,o=Ct,$e=a.stateNode.containerInfo,Ct=!0,En(e,t,a),$e=l,Ct=o;break;case 0:case 11:case 14:case 15:Qn(2,a,t),rt||Qn(4,a,t),En(e,t,a);break;case 1:rt||(rn(a,t),l=a.stateNode,typeof l.componentWillUnmount=="function"&&Qd(a,t,l)),En(e,t,a);break;case 21:En(e,t,a);break;case 22:rt=(l=rt)||a.memoizedState!==null,En(e,t,a),rt=l;break;default:En(e,t,a)}}function ep(e,t){if(t.memoizedState===null&&(e=t.alternate,e!==null&&(e=e.memoizedState,e!==null))){e=e.dehydrated;try{fl(e)}catch(a){He(t,t.return,a)}}}function tp(e,t){if(t.memoizedState===null&&(e=t.alternate,e!==null&&(e=e.memoizedState,e!==null&&(e=e.dehydrated,e!==null))))try{fl(e)}catch(a){He(t,t.return,a)}}function Am(e){switch(e.tag){case 31:case 13:case 19:var t=e.stateNode;return t===null&&(t=e.stateNode=new $d),t;case 22:return e=e.stateNode,t=e._retryCache,t===null&&(t=e._retryCache=new $d),t;default:throw Error(u(435,e.tag))}}function ni(e,t){var a=Am(e);t.forEach(function(l){if(!a.has(l)){a.add(l);var o=Bm.bind(null,e,l);l.then(o,o)}})}function jt(e,t){var a=t.deletions;if(a!==null)for(var l=0;l<a.length;l++){var o=a[l],i=e,c=t,p=c;e:for(;p!==null;){switch(p.tag){case 27:if(Fn(p.type)){$e=p.stateNode,Ct=!1;break e}break;case 5:$e=p.stateNode,Ct=!1;break e;case 3:case 4:$e=p.stateNode.containerInfo,Ct=!0;break e}p=p.return}if($e===null)throw Error(u(160));Fd(i,c,o),$e=null,Ct=!1,i=o.alternate,i!==null&&(i.return=null),o.return=null}if(t.subtreeFlags&13886)for(t=t.child;t!==null;)np(t,e),t=t.sibling}var tn=null;function np(e,t){var a=e.alternate,l=e.flags;switch(e.tag){case 0:case 11:case 14:case 15:jt(t,e),Mt(e),l&4&&(Qn(3,e,e.return),Jl(3,e),Qn(5,e,e.return));break;case 1:jt(t,e),Mt(e),l&512&&(rt||a===null||rn(a,a.return)),l&64&&wn&&(e=e.updateQueue,e!==null&&(l=e.callbacks,l!==null&&(a=e.shared.hiddenCallbacks,e.shared.hiddenCallbacks=a===null?l:a.concat(l))));break;case 26:var o=tn;if(jt(t,e),Mt(e),l&512&&(rt||a===null||rn(a,a.return)),l&4){var i=a!==null?a.memoizedState:null;if(l=e.memoizedState,a===null)if(l===null)if(e.stateNode===null){e:{l=e.type,a=e.memoizedProps,o=o.ownerDocument||o;t:switch(l){case"title":i=o.getElementsByTagName("title")[0],(!i||i[vl]||i[mt]||i.namespaceURI==="http://www.w3.org/2000/svg"||i.hasAttribute("itemprop"))&&(i=o.createElement(l),o.head.insertBefore(i,o.querySelector("head > title"))),bt(i,l,a),i[mt]=e,pt(i),l=i;break e;case"link":var c=nf("link","href",o).get(l+(a.href||""));if(c){for(var p=0;p<c.length;p++)if(i=c[p],i.getAttribute("href")===(a.href==null||a.href===""?null:a.href)&&i.getAttribute("rel")===(a.rel==null?null:a.rel)&&i.getAttribute("title")===(a.title==null?null:a.title)&&i.getAttribute("crossorigin")===(a.crossOrigin==null?null:a.crossOrigin)){c.splice(p,1);break t}}i=o.createElement(l),bt(i,l,a),o.head.appendChild(i);break;case"meta":if(c=nf("meta","content",o).get(l+(a.content||""))){for(p=0;p<c.length;p++)if(i=c[p],i.getAttribute("content")===(a.content==null?null:""+a.content)&&i.getAttribute("name")===(a.name==null?null:a.name)&&i.getAttribute("property")===(a.property==null?null:a.property)&&i.getAttribute("http-equiv")===(a.httpEquiv==null?null:a.httpEquiv)&&i.getAttribute("charset")===(a.charSet==null?null:a.charSet)){c.splice(p,1);break t}}i=o.createElement(l),bt(i,l,a),o.head.appendChild(i);break;default:throw Error(u(468,l))}i[mt]=e,pt(i),l=i}e.stateNode=l}else af(o,e.type,e.stateNode);else e.stateNode=tf(o,l,e.memoizedProps);else i!==l?(i===null?a.stateNode!==null&&(a=a.stateNode,a.parentNode.removeChild(a)):i.count--,l===null?af(o,e.type,e.stateNode):tf(o,l,e.memoizedProps)):l===null&&e.stateNode!==null&&cr(e,e.memoizedProps,a.memoizedProps)}break;case 27:jt(t,e),Mt(e),l&512&&(rt||a===null||rn(a,a.return)),a!==null&&l&4&&cr(e,e.memoizedProps,a.memoizedProps);break;case 5:if(jt(t,e),Mt(e),l&512&&(rt||a===null||rn(a,a.return)),e.flags&32){o=e.stateNode;try{Ba(o,"")}catch(ae){He(e,e.return,ae)}}l&4&&e.stateNode!=null&&(o=e.memoizedProps,cr(e,o,a!==null?a.memoizedProps:o)),l&1024&&(pr=!0);break;case 6:if(jt(t,e),Mt(e),l&4){if(e.stateNode===null)throw Error(u(162));l=e.memoizedProps,a=e.stateNode;try{a.nodeValue=l}catch(ae){He(e,e.return,ae)}}break;case 3:if(vi=null,o=tn,tn=yi(t.containerInfo),jt(t,e),tn=o,Mt(e),l&4&&a!==null&&a.memoizedState.isDehydrated)try{fl(t.containerInfo)}catch(ae){He(e,e.return,ae)}pr&&(pr=!1,ap(e));break;case 4:l=tn,tn=yi(e.stateNode.containerInfo),jt(t,e),Mt(e),tn=l;break;case 12:jt(t,e),Mt(e);break;case 31:jt(t,e),Mt(e),l&4&&(l=e.updateQueue,l!==null&&(e.updateQueue=null,ni(e,l)));break;case 13:jt(t,e),Mt(e),e.child.flags&8192&&e.memoizedState!==null!=(a!==null&&a.memoizedState!==null)&&(li=ge()),l&4&&(l=e.updateQueue,l!==null&&(e.updateQueue=null,ni(e,l)));break;case 22:o=e.memoizedState!==null;var _=a!==null&&a.memoizedState!==null,k=wn,X=rt;if(wn=k||o,rt=X||_,jt(t,e),rt=X,wn=k,Mt(e),l&8192)e:for(t=e.stateNode,t._visibility=o?t._visibility&-2:t._visibility|1,o&&(a===null||_||wn||rt||Na(e)),a=null,t=e;;){if(t.tag===5||t.tag===26){if(a===null){_=a=t;try{if(i=_.stateNode,o)c=i.style,typeof c.setProperty=="function"?c.setProperty("display","none","important"):c.display="none";else{p=_.stateNode;var Q=_.memoizedProps.style,U=Q!=null&&Q.hasOwnProperty("display")?Q.display:null;p.style.display=U==null||typeof U=="boolean"?"":(""+U).trim()}}catch(ae){He(_,_.return,ae)}}}else if(t.tag===6){if(a===null){_=t;try{_.stateNode.nodeValue=o?"":_.memoizedProps}catch(ae){He(_,_.return,ae)}}}else if(t.tag===18){if(a===null){_=t;try{var H=_.stateNode;o?Qp(H,!0):Qp(_.stateNode,!1)}catch(ae){He(_,_.return,ae)}}}else if((t.tag!==22&&t.tag!==23||t.memoizedState===null||t===e)&&t.child!==null){t.child.return=t,t=t.child;continue}if(t===e)break e;for(;t.sibling===null;){if(t.return===null||t.return===e)break e;a===t&&(a=null),t=t.return}a===t&&(a=null),t.sibling.return=t.return,t=t.sibling}l&4&&(l=e.updateQueue,l!==null&&(a=l.retryQueue,a!==null&&(l.retryQueue=null,ni(e,a))));break;case 19:jt(t,e),Mt(e),l&4&&(l=e.updateQueue,l!==null&&(e.updateQueue=null,ni(e,l)));break;case 30:break;case 21:break;default:jt(t,e),Mt(e)}}function Mt(e){var t=e.flags;if(t&2){try{for(var a,l=e.return;l!==null;){if(Id(l)){a=l;break}l=l.return}if(a==null)throw Error(u(160));switch(a.tag){case 27:var o=a.stateNode,i=ur(e);ti(e,i,o);break;case 5:var c=a.stateNode;a.flags&32&&(Ba(c,""),a.flags&=-33);var p=ur(e);ti(e,p,c);break;case 3:case 4:var _=a.stateNode.containerInfo,k=ur(e);dr(e,k,_);break;default:throw Error(u(161))}}catch(X){He(e,e.return,X)}e.flags&=-3}t&4096&&(e.flags&=-4097)}function ap(e){if(e.subtreeFlags&1024)for(e=e.child;e!==null;){var t=e;ap(t),t.tag===5&&t.flags&1024&&t.stateNode.reset(),e=e.sibling}}function Nn(e,t){if(t.subtreeFlags&8772)for(t=t.child;t!==null;)Wd(e,t.alternate,t),t=t.sibling}function Na(e){for(e=e.child;e!==null;){var t=e;switch(t.tag){case 0:case 11:case 14:case 15:Qn(4,t,t.return),Na(t);break;case 1:rn(t,t.return);var a=t.stateNode;typeof a.componentWillUnmount=="function"&&Qd(t,t.return,a),Na(t);break;case 27:eo(t.stateNode);case 26:case 5:rn(t,t.return),Na(t);break;case 22:t.memoizedState===null&&Na(t);break;case 30:Na(t);break;default:Na(t)}e=e.sibling}}function An(e,t,a){for(a=a&&(t.subtreeFlags&8772)!==0,t=t.child;t!==null;){var l=t.alternate,o=e,i=t,c=i.flags;switch(i.tag){case 0:case 11:case 15:An(o,i,a),Jl(4,i);break;case 1:if(An(o,i,a),l=i,o=l.stateNode,typeof o.componentDidMount=="function")try{o.componentDidMount()}catch(k){He(l,l.return,k)}if(l=i,o=l.updateQueue,o!==null){var p=l.stateNode;try{var _=o.shared.hiddenCallbacks;if(_!==null)for(o.shared.hiddenCallbacks=null,o=0;o<_.length;o++)ku(_[o],p)}catch(k){He(l,l.return,k)}}a&&c&64&&Zd(i),Zl(i,i.return);break;case 27:Kd(i);case 26:case 5:An(o,i,a),a&&l===null&&c&4&&Vd(i),Zl(i,i.return);break;case 12:An(o,i,a);break;case 31:An(o,i,a),a&&c&4&&ep(o,i);break;case 13:An(o,i,a),a&&c&4&&tp(o,i);break;case 22:i.memoizedState===null&&An(o,i,a),Zl(i,i.return);break;case 30:break;default:An(o,i,a)}t=t.sibling}}function fr(e,t){var a=null;e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(a=e.memoizedState.cachePool.pool),e=null,t.memoizedState!==null&&t.memoizedState.cachePool!==null&&(e=t.memoizedState.cachePool.pool),e!==a&&(e!=null&&e.refCount++,a!=null&&Dl(a))}function hr(e,t){e=null,t.alternate!==null&&(e=t.alternate.memoizedState.cache),t=t.memoizedState.cache,t!==e&&(t.refCount++,e!=null&&Dl(e))}function nn(e,t,a,l){if(t.subtreeFlags&10256)for(t=t.child;t!==null;)lp(e,t,a,l),t=t.sibling}function lp(e,t,a,l){var o=t.flags;switch(t.tag){case 0:case 11:case 15:nn(e,t,a,l),o&2048&&Jl(9,t);break;case 1:nn(e,t,a,l);break;case 3:nn(e,t,a,l),o&2048&&(e=null,t.alternate!==null&&(e=t.alternate.memoizedState.cache),t=t.memoizedState.cache,t!==e&&(t.refCount++,e!=null&&Dl(e)));break;case 12:if(o&2048){nn(e,t,a,l),e=t.stateNode;try{var i=t.memoizedProps,c=i.id,p=i.onPostCommit;typeof p=="function"&&p(c,t.alternate===null?"mount":"update",e.passiveEffectDuration,-0)}catch(_){He(t,t.return,_)}}else nn(e,t,a,l);break;case 31:nn(e,t,a,l);break;case 13:nn(e,t,a,l);break;case 23:break;case 22:i=t.stateNode,c=t.alternate,t.memoizedState!==null?i._visibility&2?nn(e,t,a,l):Ql(e,t):i._visibility&2?nn(e,t,a,l):(i._visibility|=2,nl(e,t,a,l,(t.subtreeFlags&10256)!==0||!1)),o&2048&&fr(c,t);break;case 24:nn(e,t,a,l),o&2048&&hr(t.alternate,t);break;default:nn(e,t,a,l)}}function nl(e,t,a,l,o){for(o=o&&((t.subtreeFlags&10256)!==0||!1),t=t.child;t!==null;){var i=e,c=t,p=a,_=l,k=c.flags;switch(c.tag){case 0:case 11:case 15:nl(i,c,p,_,o),Jl(8,c);break;case 23:break;case 22:var X=c.stateNode;c.memoizedState!==null?X._visibility&2?nl(i,c,p,_,o):Ql(i,c):(X._visibility|=2,nl(i,c,p,_,o)),o&&k&2048&&fr(c.alternate,c);break;case 24:nl(i,c,p,_,o),o&&k&2048&&hr(c.alternate,c);break;default:nl(i,c,p,_,o)}t=t.sibling}}function Ql(e,t){if(t.subtreeFlags&10256)for(t=t.child;t!==null;){var a=e,l=t,o=l.flags;switch(l.tag){case 22:Ql(a,l),o&2048&&fr(l.alternate,l);break;case 24:Ql(a,l),o&2048&&hr(l.alternate,l);break;default:Ql(a,l)}t=t.sibling}}var Vl=8192;function al(e,t,a){if(e.subtreeFlags&Vl)for(e=e.child;e!==null;)op(e,t,a),e=e.sibling}function op(e,t,a){switch(e.tag){case 26:al(e,t,a),e.flags&Vl&&e.memoizedState!==null&&fy(a,tn,e.memoizedState,e.memoizedProps);break;case 5:al(e,t,a);break;case 3:case 4:var l=tn;tn=yi(e.stateNode.containerInfo),al(e,t,a),tn=l;break;case 22:e.memoizedState===null&&(l=e.alternate,l!==null&&l.memoizedState!==null?(l=Vl,Vl=16777216,al(e,t,a),Vl=l):al(e,t,a));break;default:al(e,t,a)}}function ip(e){var t=e.alternate;if(t!==null&&(e=t.child,e!==null)){t.child=null;do t=e.sibling,e.sibling=null,e=t;while(e!==null)}}function Il(e){var t=e.deletions;if((e.flags&16)!==0){if(t!==null)for(var a=0;a<t.length;a++){var l=t[a];ft=l,rp(l,e)}ip(e)}if(e.subtreeFlags&10256)for(e=e.child;e!==null;)sp(e),e=e.sibling}function sp(e){switch(e.tag){case 0:case 11:case 15:Il(e),e.flags&2048&&Qn(9,e,e.return);break;case 3:Il(e);break;case 12:Il(e);break;case 22:var t=e.stateNode;e.memoizedState!==null&&t._visibility&2&&(e.return===null||e.return.tag!==13)?(t._visibility&=-3,ai(e)):Il(e);break;default:Il(e)}}function ai(e){var t=e.deletions;if((e.flags&16)!==0){if(t!==null)for(var a=0;a<t.length;a++){var l=t[a];ft=l,rp(l,e)}ip(e)}for(e=e.child;e!==null;){switch(t=e,t.tag){case 0:case 11:case 15:Qn(8,t,t.return),ai(t);break;case 22:a=t.stateNode,a._visibility&2&&(a._visibility&=-3,ai(t));break;default:ai(t)}e=e.sibling}}function rp(e,t){for(;ft!==null;){var a=ft;switch(a.tag){case 0:case 11:case 15:Qn(8,a,t);break;case 23:case 22:if(a.memoizedState!==null&&a.memoizedState.cachePool!==null){var l=a.memoizedState.cachePool.pool;l!=null&&l.refCount++}break;case 24:Dl(a.memoizedState.cache)}if(l=a.child,l!==null)l.return=a,ft=l;else e:for(a=e;ft!==null;){l=ft;var o=l.sibling,i=l.return;if(Pd(l),l===a){ft=null;break e}if(o!==null){o.return=i,ft=o;break e}ft=i}}}var Cm={getCacheForType:function(e){var t=gt(ot),a=t.data.get(e);return a===void 0&&(a=e(),t.data.set(e,a)),a},cacheSignal:function(){return gt(ot).controller.signal}},jm=typeof WeakMap=="function"?WeakMap:Map,Re=0,Ze=null,we=null,Ne=0,Ge=0,Gt=null,Vn=!1,ll=!1,mr=!1,Cn=0,Pe=0,In=0,Aa=0,yr=0,Ht=0,ol=0,Kl=null,Dt=null,gr=!1,li=0,cp=0,oi=1/0,ii=null,Kn=null,ut=0,$n=null,il=null,jn=0,vr=0,br=null,up=null,$l=0,_r=null;function Lt(){return(Re&2)!==0&&Ne!==0?Ne&-Ne:M.T!==null?Nr():Nc()}function dp(){if(Ht===0)if((Ne&536870912)===0||Ce){var e=kn;kn<<=1,(kn&3932160)===0&&(kn=262144),Ht=e}else Ht=536870912;return e=Bt.current,e!==null&&(e.flags|=32),Ht}function Ot(e,t,a){(e===Ze&&(Ge===2||Ge===9)||e.cancelPendingCommit!==null)&&(sl(e,0),Wn(e,Ne,Ht,!1)),gl(e,a),((Re&2)===0||e!==Ze)&&(e===Ze&&((Re&2)===0&&(Aa|=a),Pe===4&&Wn(e,Ne,Ht,!1)),cn(e))}function pp(e,t,a){if((Re&6)!==0)throw Error(u(327));var l=!a&&(t&127)===0&&(t&e.expiredLanes)===0||yl(e,t),o=l?Om(e,t):xr(e,t,!0),i=l;do{if(o===0){ll&&!l&&Wn(e,t,0,!1);break}else{if(a=e.current.alternate,i&&!Mm(a)){o=xr(e,t,!1),i=!1;continue}if(o===2){if(i=t,e.errorRecoveryDisabledLanes&i)var c=0;else c=e.pendingLanes&-536870913,c=c!==0?c:c&536870912?536870912:0;if(c!==0){t=c;e:{var p=e;o=Kl;var _=p.current.memoizedState.isDehydrated;if(_&&(sl(p,c).flags|=256),c=xr(p,c,!1),c!==2){if(mr&&!_){p.errorRecoveryDisabledLanes|=i,Aa|=i,o=4;break e}i=Dt,Dt=o,i!==null&&(Dt===null?Dt=i:Dt.push.apply(Dt,i))}o=c}if(i=!1,o!==2)continue}}if(o===1){sl(e,0),Wn(e,t,0,!0);break}e:{switch(l=e,i=o,i){case 0:case 1:throw Error(u(345));case 4:if((t&4194048)!==t)break;case 6:Wn(l,t,Ht,!Vn);break e;case 2:Dt=null;break;case 3:case 5:break;default:throw Error(u(329))}if((t&62914560)===t&&(o=li+300-ge(),10<o)){if(Wn(l,t,Ht,!Vn),yo(l,0,!0)!==0)break e;jn=t,l.timeoutHandle=Xp(fp.bind(null,l,a,Dt,ii,gr,t,Ht,Aa,ol,Vn,i,"Throttled",-0,0),o);break e}fp(l,a,Dt,ii,gr,t,Ht,Aa,ol,Vn,i,null,-0,0)}}break}while(!0);cn(e)}function fp(e,t,a,l,o,i,c,p,_,k,X,Q,U,H){if(e.timeoutHandle=-1,Q=t.subtreeFlags,Q&8192||(Q&16785408)===16785408){Q={stylesheets:null,count:0,imgCount:0,imgBytes:0,suspenseyImages:[],waitingForImages:!0,waitingForViewTransition:!1,unsuspend:hn},op(t,i,Q);var ae=(i&62914560)===i?li-ge():(i&4194048)===i?cp-ge():0;if(ae=hy(Q,ae),ae!==null){jn=i,e.cancelPendingCommit=ae(Sp.bind(null,e,t,i,a,l,o,c,p,_,X,Q,null,U,H)),Wn(e,i,c,!k);return}}Sp(e,t,i,a,l,o,c,p,_)}function Mm(e){for(var t=e;;){var a=t.tag;if((a===0||a===11||a===15)&&t.flags&16384&&(a=t.updateQueue,a!==null&&(a=a.stores,a!==null)))for(var l=0;l<a.length;l++){var o=a[l],i=o.getSnapshot;o=o.value;try{if(!kt(i(),o))return!1}catch{return!1}}if(a=t.child,t.subtreeFlags&16384&&a!==null)a.return=t,t=a;else{if(t===e)break;for(;t.sibling===null;){if(t.return===null||t.return===e)return!0;t=t.return}t.sibling.return=t.return,t=t.sibling}}return!0}function Wn(e,t,a,l){t&=~yr,t&=~Aa,e.suspendedLanes|=t,e.pingedLanes&=~t,l&&(e.warmLanes|=t),l=e.expirationTimes;for(var o=t;0<o;){var i=31-xt(o),c=1<<i;l[i]=-1,o&=~c}a!==0&&Tc(e,a,t)}function si(){return(Re&6)===0?(Wl(0),!1):!0}function Sr(){if(we!==null){if(Ge===0)var e=we.return;else e=we,vn=va=null,Us(e),Wa=null,zl=0,e=we;for(;e!==null;)Jd(e.alternate,e),e=e.return;we=null}}function sl(e,t){var a=e.timeoutHandle;a!==-1&&(e.timeoutHandle=-1,$m(a)),a=e.cancelPendingCommit,a!==null&&(e.cancelPendingCommit=null,a()),jn=0,Sr(),Ze=e,we=a=yn(e.current,null),Ne=t,Ge=0,Gt=null,Vn=!1,ll=yl(e,t),mr=!1,ol=Ht=yr=Aa=In=Pe=0,Dt=Kl=null,gr=!1,(t&8)!==0&&(t|=t&32);var l=e.entangledLanes;if(l!==0)for(e=e.entanglements,l&=t;0<l;){var o=31-xt(l),i=1<<o;t|=e[o],l&=~i}return Cn=t,Co(),a}function hp(e,t){ve=null,M.H=ql,t===$a||t===Bo?(t=Mu(),Ge=3):t===Es?(t=Mu(),Ge=4):Ge=t===Fs?8:t!==null&&typeof t=="object"&&typeof t.then=="function"?6:1,Gt=t,we===null&&(Pe=1,$o(e,Qt(t,e.current)))}function mp(){var e=Bt.current;return e===null?!0:(Ne&4194048)===Ne?$t===null:(Ne&62914560)===Ne||(Ne&536870912)!==0?e===$t:!1}function yp(){var e=M.H;return M.H=ql,e===null?ql:e}function gp(){var e=M.A;return M.A=Cm,e}function ri(){Pe=4,Vn||(Ne&4194048)!==Ne&&Bt.current!==null||(ll=!0),(In&134217727)===0&&(Aa&134217727)===0||Ze===null||Wn(Ze,Ne,Ht,!1)}function xr(e,t,a){var l=Re;Re|=2;var o=yp(),i=gp();(Ze!==e||Ne!==t)&&(ii=null,sl(e,t)),t=!1;var c=Pe;e:do try{if(Ge!==0&&we!==null){var p=we,_=Gt;switch(Ge){case 8:Sr(),c=6;break e;case 3:case 2:case 9:case 6:Bt.current===null&&(t=!0);var k=Ge;if(Ge=0,Gt=null,rl(e,p,_,k),a&&ll){c=0;break e}break;default:k=Ge,Ge=0,Gt=null,rl(e,p,_,k)}}Dm(),c=Pe;break}catch(X){hp(e,X)}while(!0);return t&&e.shellSuspendCounter++,vn=va=null,Re=l,M.H=o,M.A=i,we===null&&(Ze=null,Ne=0,Co()),c}function Dm(){for(;we!==null;)vp(we)}function Om(e,t){var a=Re;Re|=2;var l=yp(),o=gp();Ze!==e||Ne!==t?(ii=null,oi=ge()+500,sl(e,t)):ll=yl(e,t);e:do try{if(Ge!==0&&we!==null){t=we;var i=Gt;t:switch(Ge){case 1:Ge=0,Gt=null,rl(e,t,i,1);break;case 2:case 9:if(Cu(i)){Ge=0,Gt=null,bp(t);break}t=function(){Ge!==2&&Ge!==9||Ze!==e||(Ge=7),cn(e)},i.then(t,t);break e;case 3:Ge=7;break e;case 4:Ge=5;break e;case 7:Cu(i)?(Ge=0,Gt=null,bp(t)):(Ge=0,Gt=null,rl(e,t,i,7));break;case 5:var c=null;switch(we.tag){case 26:c=we.memoizedState;case 5:case 27:var p=we;if(c?lf(c):p.stateNode.complete){Ge=0,Gt=null;var _=p.sibling;if(_!==null)we=_;else{var k=p.return;k!==null?(we=k,ci(k)):we=null}break t}}Ge=0,Gt=null,rl(e,t,i,5);break;case 6:Ge=0,Gt=null,rl(e,t,i,6);break;case 8:Sr(),Pe=6;break e;default:throw Error(u(462))}}zm();break}catch(X){hp(e,X)}while(!0);return vn=va=null,M.H=l,M.A=o,Re=a,we!==null?0:(Ze=null,Ne=0,Co(),Pe)}function zm(){for(;we!==null&&!ct();)vp(we)}function vp(e){var t=Yd(e.alternate,e,Cn);e.memoizedProps=e.pendingProps,t===null?ci(e):we=t}function bp(e){var t=e,a=t.alternate;switch(t.tag){case 15:case 0:t=Bd(a,t,t.pendingProps,t.type,void 0,Ne);break;case 11:t=Bd(a,t,t.pendingProps,t.type.render,t.ref,Ne);break;case 5:Us(t);default:Jd(a,t),t=we=gu(t,Cn),t=Yd(a,t,Cn)}e.memoizedProps=e.pendingProps,t===null?ci(e):we=t}function rl(e,t,a,l){vn=va=null,Us(t),Wa=null,zl=0;var o=t.return;try{if(Sm(e,o,t,a,Ne)){Pe=1,$o(e,Qt(a,e.current)),we=null;return}}catch(i){if(o!==null)throw we=o,i;Pe=1,$o(e,Qt(a,e.current)),we=null;return}t.flags&32768?(Ce||l===1?e=!0:ll||(Ne&536870912)!==0?e=!1:(Vn=e=!0,(l===2||l===9||l===3||l===6)&&(l=Bt.current,l!==null&&l.tag===13&&(l.flags|=16384))),_p(t,e)):ci(t)}function ci(e){var t=e;do{if((t.flags&32768)!==0){_p(t,Vn);return}e=t.return;var a=wm(t.alternate,t,Cn);if(a!==null){we=a;return}if(t=t.sibling,t!==null){we=t;return}we=t=e}while(t!==null);Pe===0&&(Pe=5)}function _p(e,t){do{var a=Em(e.alternate,e);if(a!==null){a.flags&=32767,we=a;return}if(a=e.return,a!==null&&(a.flags|=32768,a.subtreeFlags=0,a.deletions=null),!t&&(e=e.sibling,e!==null)){we=e;return}we=e=a}while(e!==null);Pe=6,we=null}function Sp(e,t,a,l,o,i,c,p,_){e.cancelPendingCommit=null;do ui();while(ut!==0);if((Re&6)!==0)throw Error(u(327));if(t!==null){if(t===e.current)throw Error(u(177));if(i=t.lanes|t.childLanes,i|=us,ph(e,a,i,c,p,_),e===Ze&&(we=Ze=null,Ne=0),il=t,$n=e,jn=a,vr=i,br=o,up=l,(t.subtreeFlags&10256)!==0||(t.flags&10256)!==0?(e.callbackNode=null,e.callbackPriority=0,Um(ra,function(){return Np(),null})):(e.callbackNode=null,e.callbackPriority=0),l=(t.flags&13878)!==0,(t.subtreeFlags&13878)!==0||l){l=M.T,M.T=null,o=D.p,D.p=2,c=Re,Re|=4;try{Nm(e,t,a)}finally{Re=c,D.p=o,M.T=l}}ut=1,xp(),Tp(),wp()}}function xp(){if(ut===1){ut=0;var e=$n,t=il,a=(t.flags&13878)!==0;if((t.subtreeFlags&13878)!==0||a){a=M.T,M.T=null;var l=D.p;D.p=2;var o=Re;Re|=4;try{np(t,e);var i=kr,c=ru(e.containerInfo),p=i.focusedElem,_=i.selectionRange;if(c!==p&&p&&p.ownerDocument&&su(p.ownerDocument.documentElement,p)){if(_!==null&&os(p)){var k=_.start,X=_.end;if(X===void 0&&(X=k),"selectionStart"in p)p.selectionStart=k,p.selectionEnd=Math.min(X,p.value.length);else{var Q=p.ownerDocument||document,U=Q&&Q.defaultView||window;if(U.getSelection){var H=U.getSelection(),ae=p.textContent.length,pe=Math.min(_.start,ae),Je=_.end===void 0?pe:Math.min(_.end,ae);!H.extend&&pe>Je&&(c=Je,Je=pe,pe=c);var j=iu(p,pe),w=iu(p,Je);if(j&&w&&(H.rangeCount!==1||H.anchorNode!==j.node||H.anchorOffset!==j.offset||H.focusNode!==w.node||H.focusOffset!==w.offset)){var z=Q.createRange();z.setStart(j.node,j.offset),H.removeAllRanges(),pe>Je?(H.addRange(z),H.extend(w.node,w.offset)):(z.setEnd(w.node,w.offset),H.addRange(z))}}}}for(Q=[],H=p;H=H.parentNode;)H.nodeType===1&&Q.push({element:H,left:H.scrollLeft,top:H.scrollTop});for(typeof p.focus=="function"&&p.focus(),p=0;p<Q.length;p++){var Z=Q[p];Z.element.scrollLeft=Z.left,Z.element.scrollTop=Z.top}}xi=!!zr,kr=zr=null}finally{Re=o,D.p=l,M.T=a}}e.current=t,ut=2}}function Tp(){if(ut===2){ut=0;var e=$n,t=il,a=(t.flags&8772)!==0;if((t.subtreeFlags&8772)!==0||a){a=M.T,M.T=null;var l=D.p;D.p=2;var o=Re;Re|=4;try{Wd(e,t.alternate,t)}finally{Re=o,D.p=l,M.T=a}}ut=3}}function wp(){if(ut===4||ut===3){ut=0,sa();var e=$n,t=il,a=jn,l=up;(t.subtreeFlags&10256)!==0||(t.flags&10256)!==0?ut=5:(ut=0,il=$n=null,Ep(e,e.pendingLanes));var o=e.pendingLanes;if(o===0&&(Kn=null),Li(a),t=t.stateNode,St&&typeof St.onCommitFiberRoot=="function")try{St.onCommitFiberRoot(ca,t,void 0,(t.current.flags&128)===128)}catch{}if(l!==null){t=M.T,o=D.p,D.p=2,M.T=null;try{for(var i=e.onRecoverableError,c=0;c<l.length;c++){var p=l[c];i(p.value,{componentStack:p.stack})}}finally{M.T=t,D.p=o}}(jn&3)!==0&&ui(),cn(e),o=e.pendingLanes,(a&261930)!==0&&(o&42)!==0?e===_r?$l++:($l=0,_r=e):$l=0,Wl(0)}}function Ep(e,t){(e.pooledCacheLanes&=t)===0&&(t=e.pooledCache,t!=null&&(e.pooledCache=null,Dl(t)))}function ui(){return xp(),Tp(),wp(),Np()}function Np(){if(ut!==5)return!1;var e=$n,t=vr;vr=0;var a=Li(jn),l=M.T,o=D.p;try{D.p=32>a?32:a,M.T=null,a=br,br=null;var i=$n,c=jn;if(ut=0,il=$n=null,jn=0,(Re&6)!==0)throw Error(u(331));var p=Re;if(Re|=4,sp(i.current),lp(i,i.current,c,a),Re=p,Wl(0,!1),St&&typeof St.onPostCommitFiberRoot=="function")try{St.onPostCommitFiberRoot(ca,i)}catch{}return!0}finally{D.p=o,M.T=l,Ep(e,t)}}function Ap(e,t,a){t=Qt(a,t),t=Ps(e.stateNode,t,2),e=Xn(e,t,2),e!==null&&(gl(e,2),cn(e))}function He(e,t,a){if(e.tag===3)Ap(e,e,a);else for(;t!==null;){if(t.tag===3){Ap(t,e,a);break}else if(t.tag===1){var l=t.stateNode;if(typeof t.type.getDerivedStateFromError=="function"||typeof l.componentDidCatch=="function"&&(Kn===null||!Kn.has(l))){e=Qt(a,e),a=Cd(2),l=Xn(t,a,2),l!==null&&(jd(a,l,t,e),gl(l,2),cn(l));break}}t=t.return}}function Tr(e,t,a){var l=e.pingCache;if(l===null){l=e.pingCache=new jm;var o=new Set;l.set(t,o)}else o=l.get(t),o===void 0&&(o=new Set,l.set(t,o));o.has(a)||(mr=!0,o.add(a),e=km.bind(null,e,t,a),t.then(e,e))}function km(e,t,a){var l=e.pingCache;l!==null&&l.delete(t),e.pingedLanes|=e.suspendedLanes&a,e.warmLanes&=~a,Ze===e&&(Ne&a)===a&&(Pe===4||Pe===3&&(Ne&62914560)===Ne&&300>ge()-li?(Re&2)===0&&sl(e,0):yr|=a,ol===Ne&&(ol=0)),cn(e)}function Cp(e,t){t===0&&(t=xc()),e=ma(e,t),e!==null&&(gl(e,t),cn(e))}function Rm(e){var t=e.memoizedState,a=0;t!==null&&(a=t.retryLane),Cp(e,a)}function Bm(e,t){var a=0;switch(e.tag){case 31:case 13:var l=e.stateNode,o=e.memoizedState;o!==null&&(a=o.retryLane);break;case 19:l=e.stateNode;break;case 22:l=e.stateNode._retryCache;break;default:throw Error(u(314))}l!==null&&l.delete(t),Cp(e,a)}function Um(e,t){return dt(e,t)}var di=null,cl=null,wr=!1,pi=!1,Er=!1,Pn=0;function cn(e){e!==cl&&e.next===null&&(cl===null?di=cl=e:cl=cl.next=e),pi=!0,wr||(wr=!0,Hm())}function Wl(e,t){if(!Er&&pi){Er=!0;do for(var a=!1,l=di;l!==null;){if(e!==0){var o=l.pendingLanes;if(o===0)var i=0;else{var c=l.suspendedLanes,p=l.pingedLanes;i=(1<<31-xt(42|e)+1)-1,i&=o&~(c&~p),i=i&201326741?i&201326741|1:i?i|2:0}i!==0&&(a=!0,Op(l,i))}else i=Ne,i=yo(l,l===Ze?i:0,l.cancelPendingCommit!==null||l.timeoutHandle!==-1),(i&3)===0||yl(l,i)||(a=!0,Op(l,i));l=l.next}while(a);Er=!1}}function Gm(){jp()}function jp(){pi=wr=!1;var e=0;Pn!==0&&Km()&&(e=Pn);for(var t=ge(),a=null,l=di;l!==null;){var o=l.next,i=Mp(l,t);i===0?(l.next=null,a===null?di=o:a.next=o,o===null&&(cl=a)):(a=l,(e!==0||(i&3)!==0)&&(pi=!0)),l=o}ut!==0&&ut!==5||Wl(e),Pn!==0&&(Pn=0)}function Mp(e,t){for(var a=e.suspendedLanes,l=e.pingedLanes,o=e.expirationTimes,i=e.pendingLanes&-62914561;0<i;){var c=31-xt(i),p=1<<c,_=o[c];_===-1?((p&a)===0||(p&l)!==0)&&(o[c]=dh(p,t)):_<=t&&(e.expiredLanes|=p),i&=~p}if(t=Ze,a=Ne,a=yo(e,e===t?a:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),l=e.callbackNode,a===0||e===t&&(Ge===2||Ge===9)||e.cancelPendingCommit!==null)return l!==null&&l!==null&&Fe(l),e.callbackNode=null,e.callbackPriority=0;if((a&3)===0||yl(e,a)){if(t=a&-a,t===e.callbackPriority)return t;switch(l!==null&&Fe(l),Li(a)){case 2:case 8:a=ho;break;case 32:a=ra;break;case 268435456:a=mo;break;default:a=ra}return l=Dp.bind(null,e),a=dt(a,l),e.callbackPriority=t,e.callbackNode=a,t}return l!==null&&l!==null&&Fe(l),e.callbackPriority=2,e.callbackNode=null,2}function Dp(e,t){if(ut!==0&&ut!==5)return e.callbackNode=null,e.callbackPriority=0,null;var a=e.callbackNode;if(ui()&&e.callbackNode!==a)return null;var l=Ne;return l=yo(e,e===Ze?l:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),l===0?null:(pp(e,l,t),Mp(e,ge()),e.callbackNode!=null&&e.callbackNode===a?Dp.bind(null,e):null)}function Op(e,t){if(ui())return null;pp(e,t,!0)}function Hm(){Wm(function(){(Re&6)!==0?dt(fo,Gm):jp()})}function Nr(){if(Pn===0){var e=Ia;e===0&&(e=et,et<<=1,(et&261888)===0&&(et=256)),Pn=e}return Pn}function zp(e){return e==null||typeof e=="symbol"||typeof e=="boolean"?null:typeof e=="function"?e:_o(""+e)}function kp(e,t){var a=t.ownerDocument.createElement("input");return a.name=t.name,a.value=t.value,e.id&&a.setAttribute("form",e.id),t.parentNode.insertBefore(a,t),e=new FormData(e),a.parentNode.removeChild(a),e}function Lm(e,t,a,l,o){if(t==="submit"&&a&&a.stateNode===o){var i=zp((o[Nt]||null).action),c=l.submitter;c&&(t=(t=c[Nt]||null)?zp(t.formAction):c.getAttribute("formAction"),t!==null&&(i=t,c=null));var p=new wo("action","action",null,l,o);e.push({event:p,listeners:[{instance:null,listener:function(){if(l.defaultPrevented){if(Pn!==0){var _=c?kp(o,c):new FormData(o);Qs(a,{pending:!0,data:_,method:o.method,action:i},null,_)}}else typeof i=="function"&&(p.preventDefault(),_=c?kp(o,c):new FormData(o),Qs(a,{pending:!0,data:_,method:o.method,action:i},i,_))},currentTarget:o}]})}}for(var Ar=0;Ar<cs.length;Ar++){var Cr=cs[Ar],qm=Cr.toLowerCase(),Ym=Cr[0].toUpperCase()+Cr.slice(1);en(qm,"on"+Ym)}en(du,"onAnimationEnd"),en(pu,"onAnimationIteration"),en(fu,"onAnimationStart"),en("dblclick","onDoubleClick"),en("focusin","onFocus"),en("focusout","onBlur"),en(lm,"onTransitionRun"),en(om,"onTransitionStart"),en(im,"onTransitionCancel"),en(hu,"onTransitionEnd"),ka("onMouseEnter",["mouseout","mouseover"]),ka("onMouseLeave",["mouseout","mouseover"]),ka("onPointerEnter",["pointerout","pointerover"]),ka("onPointerLeave",["pointerout","pointerover"]),da("onChange","change click focusin focusout input keydown keyup selectionchange".split(" ")),da("onSelect","focusout contextmenu dragend focusin keydown keyup mousedown mouseup selectionchange".split(" ")),da("onBeforeInput",["compositionend","keypress","textInput","paste"]),da("onCompositionEnd","compositionend focusout keydown keypress keyup mousedown".split(" ")),da("onCompositionStart","compositionstart focusout keydown keypress keyup mousedown".split(" ")),da("onCompositionUpdate","compositionupdate focusout keydown keypress keyup mousedown".split(" "));var Pl="abort canplay canplaythrough durationchange emptied encrypted ended error loadeddata loadedmetadata loadstart pause play playing progress ratechange resize seeked seeking stalled suspend timeupdate volumechange waiting".split(" "),Xm=new Set("beforetoggle cancel close invalid load scroll scrollend toggle".split(" ").concat(Pl));function Rp(e,t){t=(t&4)!==0;for(var a=0;a<e.length;a++){var l=e[a],o=l.event;l=l.listeners;e:{var i=void 0;if(t)for(var c=l.length-1;0<=c;c--){var p=l[c],_=p.instance,k=p.currentTarget;if(p=p.listener,_!==i&&o.isPropagationStopped())break e;i=p,o.currentTarget=k;try{i(o)}catch(X){Ao(X)}o.currentTarget=null,i=_}else for(c=0;c<l.length;c++){if(p=l[c],_=p.instance,k=p.currentTarget,p=p.listener,_!==i&&o.isPropagationStopped())break e;i=p,o.currentTarget=k;try{i(o)}catch(X){Ao(X)}o.currentTarget=null,i=_}}}}function Ee(e,t){var a=t[qi];a===void 0&&(a=t[qi]=new Set);var l=e+"__bubble";a.has(l)||(Bp(t,e,2,!1),a.add(l))}function jr(e,t,a){var l=0;t&&(l|=4),Bp(a,e,l,t)}var fi="_reactListening"+Math.random().toString(36).slice(2);function Mr(e){if(!e[fi]){e[fi]=!0,jc.forEach(function(a){a!=="selectionchange"&&(Xm.has(a)||jr(a,!1,e),jr(a,!0,e))});var t=e.nodeType===9?e:e.ownerDocument;t===null||t[fi]||(t[fi]=!0,jr("selectionchange",!1,t))}}function Bp(e,t,a,l){switch(pf(t)){case 2:var o=gy;break;case 8:o=vy;break;default:o=Zr}a=o.bind(null,t,a,e),o=void 0,!$i||t!=="touchstart"&&t!=="touchmove"&&t!=="wheel"||(o=!0),l?o!==void 0?e.addEventListener(t,a,{capture:!0,passive:o}):e.addEventListener(t,a,!0):o!==void 0?e.addEventListener(t,a,{passive:o}):e.addEventListener(t,a,!1)}function Dr(e,t,a,l,o){var i=l;if((t&1)===0&&(t&2)===0&&l!==null)e:for(;;){if(l===null)return;var c=l.tag;if(c===3||c===4){var p=l.stateNode.containerInfo;if(p===o)break;if(c===4)for(c=l.return;c!==null;){var _=c.tag;if((_===3||_===4)&&c.stateNode.containerInfo===o)return;c=c.return}for(;p!==null;){if(c=Da(p),c===null)return;if(_=c.tag,_===5||_===6||_===26||_===27){l=i=c;continue e}p=p.parentNode}}l=l.return}qc(function(){var k=i,X=Ii(a),Q=[];e:{var U=mu.get(e);if(U!==void 0){var H=wo,ae=e;switch(e){case"keypress":if(xo(a)===0)break e;case"keydown":case"keyup":H=Bh;break;case"focusin":ae="focus",H=es;break;case"focusout":ae="blur",H=es;break;case"beforeblur":case"afterblur":H=es;break;case"click":if(a.button===2)break e;case"auxclick":case"dblclick":case"mousedown":case"mousemove":case"mouseup":case"mouseout":case"mouseover":case"contextmenu":H=Jc;break;case"drag":case"dragend":case"dragenter":case"dragexit":case"dragleave":case"dragover":case"dragstart":case"drop":H=wh;break;case"touchcancel":case"touchend":case"touchmove":case"touchstart":H=Hh;break;case du:case pu:case fu:H=Ah;break;case hu:H=qh;break;case"scroll":case"scrollend":H=xh;break;case"wheel":H=Xh;break;case"copy":case"cut":case"paste":H=jh;break;case"gotpointercapture":case"lostpointercapture":case"pointercancel":case"pointerdown":case"pointermove":case"pointerout":case"pointerover":case"pointerup":H=Qc;break;case"toggle":case"beforetoggle":H=Zh}var pe=(t&4)!==0,Je=!pe&&(e==="scroll"||e==="scrollend"),j=pe?U!==null?U+"Capture":null:U;pe=[];for(var w=k,z;w!==null;){var Z=w;if(z=Z.stateNode,Z=Z.tag,Z!==5&&Z!==26&&Z!==27||z===null||j===null||(Z=_l(w,j),Z!=null&&pe.push(Fl(w,Z,z))),Je)break;w=w.return}0<pe.length&&(U=new H(U,ae,null,a,X),Q.push({event:U,listeners:pe}))}}if((t&7)===0){e:{if(U=e==="mouseover"||e==="pointerover",H=e==="mouseout"||e==="pointerout",U&&a!==Vi&&(ae=a.relatedTarget||a.fromElement)&&(Da(ae)||ae[Ma]))break e;if((H||U)&&(U=X.window===X?X:(U=X.ownerDocument)?U.defaultView||U.parentWindow:window,H?(ae=a.relatedTarget||a.toElement,H=k,ae=ae?Da(ae):null,ae!==null&&(Je=f(ae),pe=ae.tag,ae!==Je||pe!==5&&pe!==27&&pe!==6)&&(ae=null)):(H=null,ae=k),H!==ae)){if(pe=Jc,Z="onMouseLeave",j="onMouseEnter",w="mouse",(e==="pointerout"||e==="pointerover")&&(pe=Qc,Z="onPointerLeave",j="onPointerEnter",w="pointer"),Je=H==null?U:bl(H),z=ae==null?U:bl(ae),U=new pe(Z,w+"leave",H,a,X),U.target=Je,U.relatedTarget=z,Z=null,Da(X)===k&&(pe=new pe(j,w+"enter",ae,a,X),pe.target=z,pe.relatedTarget=Je,Z=pe),Je=Z,H&&ae)t:{for(pe=Jm,j=H,w=ae,z=0,Z=j;Z;Z=pe(Z))z++;Z=0;for(var se=w;se;se=pe(se))Z++;for(;0<z-Z;)j=pe(j),z--;for(;0<Z-z;)w=pe(w),Z--;for(;z--;){if(j===w||w!==null&&j===w.alternate){pe=j;break t}j=pe(j),w=pe(w)}pe=null}else pe=null;H!==null&&Up(Q,U,H,pe,!1),ae!==null&&Je!==null&&Up(Q,Je,ae,pe,!0)}}e:{if(U=k?bl(k):window,H=U.nodeName&&U.nodeName.toLowerCase(),H==="select"||H==="input"&&U.type==="file")var Oe=eu;else if(Pc(U))if(tu)Oe=tm;else{Oe=Fh;var oe=Ph}else H=U.nodeName,!H||H.toLowerCase()!=="input"||U.type!=="checkbox"&&U.type!=="radio"?k&&Qi(k.elementType)&&(Oe=eu):Oe=em;if(Oe&&(Oe=Oe(e,k))){Fc(Q,Oe,a,X);break e}oe&&oe(e,U,k),e==="focusout"&&k&&U.type==="number"&&k.memoizedProps.value!=null&&Zi(U,"number",U.value)}switch(oe=k?bl(k):window,e){case"focusin":(Pc(oe)||oe.contentEditable==="true")&&(La=oe,is=k,Cl=null);break;case"focusout":Cl=is=La=null;break;case"mousedown":ss=!0;break;case"contextmenu":case"mouseup":case"dragend":ss=!1,cu(Q,a,X);break;case"selectionchange":if(am)break;case"keydown":case"keyup":cu(Q,a,X)}var be;if(ns)e:{switch(e){case"compositionstart":var Ae="onCompositionStart";break e;case"compositionend":Ae="onCompositionEnd";break e;case"compositionupdate":Ae="onCompositionUpdate";break e}Ae=void 0}else Ha?$c(e,a)&&(Ae="onCompositionEnd"):e==="keydown"&&a.keyCode===229&&(Ae="onCompositionStart");Ae&&(Vc&&a.locale!=="ko"&&(Ha||Ae!=="onCompositionStart"?Ae==="onCompositionEnd"&&Ha&&(be=Yc()):(Bn=X,Wi="value"in Bn?Bn.value:Bn.textContent,Ha=!0)),oe=hi(k,Ae),0<oe.length&&(Ae=new Zc(Ae,e,null,a,X),Q.push({event:Ae,listeners:oe}),be?Ae.data=be:(be=Wc(a),be!==null&&(Ae.data=be)))),(be=Vh?Ih(e,a):Kh(e,a))&&(Ae=hi(k,"onBeforeInput"),0<Ae.length&&(oe=new Zc("onBeforeInput","beforeinput",null,a,X),Q.push({event:oe,listeners:Ae}),oe.data=be)),Lm(Q,e,k,a,X)}Rp(Q,t)})}function Fl(e,t,a){return{instance:e,listener:t,currentTarget:a}}function hi(e,t){for(var a=t+"Capture",l=[];e!==null;){var o=e,i=o.stateNode;if(o=o.tag,o!==5&&o!==26&&o!==27||i===null||(o=_l(e,a),o!=null&&l.unshift(Fl(e,o,i)),o=_l(e,t),o!=null&&l.push(Fl(e,o,i))),e.tag===3)return l;e=e.return}return[]}function Jm(e){if(e===null)return null;do e=e.return;while(e&&e.tag!==5&&e.tag!==27);return e||null}function Up(e,t,a,l,o){for(var i=t._reactName,c=[];a!==null&&a!==l;){var p=a,_=p.alternate,k=p.stateNode;if(p=p.tag,_!==null&&_===l)break;p!==5&&p!==26&&p!==27||k===null||(_=k,o?(k=_l(a,i),k!=null&&c.unshift(Fl(a,k,_))):o||(k=_l(a,i),k!=null&&c.push(Fl(a,k,_)))),a=a.return}c.length!==0&&e.push({event:t,listeners:c})}var Zm=/\r\n?/g,Qm=/\u0000|\uFFFD/g;function Gp(e){return(typeof e=="string"?e:""+e).replace(Zm,`
`).replace(Qm,"")}function Hp(e,t){return t=Gp(t),Gp(e)===t}function Xe(e,t,a,l,o,i){switch(a){case"children":typeof l=="string"?t==="body"||t==="textarea"&&l===""||Ba(e,l):(typeof l=="number"||typeof l=="bigint")&&t!=="body"&&Ba(e,""+l);break;case"className":vo(e,"class",l);break;case"tabIndex":vo(e,"tabindex",l);break;case"dir":case"role":case"viewBox":case"width":case"height":vo(e,a,l);break;case"style":Hc(e,l,i);break;case"data":if(t!=="object"){vo(e,"data",l);break}case"src":case"href":if(l===""&&(t!=="a"||a!=="href")){e.removeAttribute(a);break}if(l==null||typeof l=="function"||typeof l=="symbol"||typeof l=="boolean"){e.removeAttribute(a);break}l=_o(""+l),e.setAttribute(a,l);break;case"action":case"formAction":if(typeof l=="function"){e.setAttribute(a,"javascript:throw new Error('A React form was unexpectedly submitted. If you called form.submit() manually, consider using form.requestSubmit() instead. If you\\'re trying to use event.stopPropagation() in a submit event handler, consider also calling event.preventDefault().')");break}else typeof i=="function"&&(a==="formAction"?(t!=="input"&&Xe(e,t,"name",o.name,o,null),Xe(e,t,"formEncType",o.formEncType,o,null),Xe(e,t,"formMethod",o.formMethod,o,null),Xe(e,t,"formTarget",o.formTarget,o,null)):(Xe(e,t,"encType",o.encType,o,null),Xe(e,t,"method",o.method,o,null),Xe(e,t,"target",o.target,o,null)));if(l==null||typeof l=="symbol"||typeof l=="boolean"){e.removeAttribute(a);break}l=_o(""+l),e.setAttribute(a,l);break;case"onClick":l!=null&&(e.onclick=hn);break;case"onScroll":l!=null&&Ee("scroll",e);break;case"onScrollEnd":l!=null&&Ee("scrollend",e);break;case"dangerouslySetInnerHTML":if(l!=null){if(typeof l!="object"||!("__html"in l))throw Error(u(61));if(a=l.__html,a!=null){if(o.children!=null)throw Error(u(60));e.innerHTML=a}}break;case"multiple":e.multiple=l&&typeof l!="function"&&typeof l!="symbol";break;case"muted":e.muted=l&&typeof l!="function"&&typeof l!="symbol";break;case"suppressContentEditableWarning":case"suppressHydrationWarning":case"defaultValue":case"defaultChecked":case"innerHTML":case"ref":break;case"autoFocus":break;case"xlinkHref":if(l==null||typeof l=="function"||typeof l=="boolean"||typeof l=="symbol"){e.removeAttribute("xlink:href");break}a=_o(""+l),e.setAttributeNS("http://www.w3.org/1999/xlink","xlink:href",a);break;case"contentEditable":case"spellCheck":case"draggable":case"value":case"autoReverse":case"externalResourcesRequired":case"focusable":case"preserveAlpha":l!=null&&typeof l!="function"&&typeof l!="symbol"?e.setAttribute(a,""+l):e.removeAttribute(a);break;case"inert":case"allowFullScreen":case"async":case"autoPlay":case"controls":case"default":case"defer":case"disabled":case"disablePictureInPicture":case"disableRemotePlayback":case"formNoValidate":case"hidden":case"loop":case"noModule":case"noValidate":case"open":case"playsInline":case"readOnly":case"required":case"reversed":case"scoped":case"seamless":case"itemScope":l&&typeof l!="function"&&typeof l!="symbol"?e.setAttribute(a,""):e.removeAttribute(a);break;case"capture":case"download":l===!0?e.setAttribute(a,""):l!==!1&&l!=null&&typeof l!="function"&&typeof l!="symbol"?e.setAttribute(a,l):e.removeAttribute(a);break;case"cols":case"rows":case"size":case"span":l!=null&&typeof l!="function"&&typeof l!="symbol"&&!isNaN(l)&&1<=l?e.setAttribute(a,l):e.removeAttribute(a);break;case"rowSpan":case"start":l==null||typeof l=="function"||typeof l=="symbol"||isNaN(l)?e.removeAttribute(a):e.setAttribute(a,l);break;case"popover":Ee("beforetoggle",e),Ee("toggle",e),go(e,"popover",l);break;case"xlinkActuate":fn(e,"http://www.w3.org/1999/xlink","xlink:actuate",l);break;case"xlinkArcrole":fn(e,"http://www.w3.org/1999/xlink","xlink:arcrole",l);break;case"xlinkRole":fn(e,"http://www.w3.org/1999/xlink","xlink:role",l);break;case"xlinkShow":fn(e,"http://www.w3.org/1999/xlink","xlink:show",l);break;case"xlinkTitle":fn(e,"http://www.w3.org/1999/xlink","xlink:title",l);break;case"xlinkType":fn(e,"http://www.w3.org/1999/xlink","xlink:type",l);break;case"xmlBase":fn(e,"http://www.w3.org/XML/1998/namespace","xml:base",l);break;case"xmlLang":fn(e,"http://www.w3.org/XML/1998/namespace","xml:lang",l);break;case"xmlSpace":fn(e,"http://www.w3.org/XML/1998/namespace","xml:space",l);break;case"is":go(e,"is",l);break;case"innerText":case"textContent":break;default:(!(2<a.length)||a[0]!=="o"&&a[0]!=="O"||a[1]!=="n"&&a[1]!=="N")&&(a=_h.get(a)||a,go(e,a,l))}}function Or(e,t,a,l,o,i){switch(a){case"style":Hc(e,l,i);break;case"dangerouslySetInnerHTML":if(l!=null){if(typeof l!="object"||!("__html"in l))throw Error(u(61));if(a=l.__html,a!=null){if(o.children!=null)throw Error(u(60));e.innerHTML=a}}break;case"children":typeof l=="string"?Ba(e,l):(typeof l=="number"||typeof l=="bigint")&&Ba(e,""+l);break;case"onScroll":l!=null&&Ee("scroll",e);break;case"onScrollEnd":l!=null&&Ee("scrollend",e);break;case"onClick":l!=null&&(e.onclick=hn);break;case"suppressContentEditableWarning":case"suppressHydrationWarning":case"innerHTML":case"ref":break;case"innerText":case"textContent":break;default:if(!Mc.hasOwnProperty(a))e:{if(a[0]==="o"&&a[1]==="n"&&(o=a.endsWith("Capture"),t=a.slice(2,o?a.length-7:void 0),i=e[Nt]||null,i=i!=null?i[a]:null,typeof i=="function"&&e.removeEventListener(t,i,o),typeof l=="function")){typeof i!="function"&&i!==null&&(a in e?e[a]=null:e.hasAttribute(a)&&e.removeAttribute(a)),e.addEventListener(t,l,o);break e}a in e?e[a]=l:l===!0?e.setAttribute(a,""):go(e,a,l)}}}function bt(e,t,a){switch(t){case"div":case"span":case"svg":case"path":case"a":case"g":case"p":case"li":break;case"img":Ee("error",e),Ee("load",e);var l=!1,o=!1,i;for(i in a)if(a.hasOwnProperty(i)){var c=a[i];if(c!=null)switch(i){case"src":l=!0;break;case"srcSet":o=!0;break;case"children":case"dangerouslySetInnerHTML":throw Error(u(137,t));default:Xe(e,t,i,c,a,null)}}o&&Xe(e,t,"srcSet",a.srcSet,a,null),l&&Xe(e,t,"src",a.src,a,null);return;case"input":Ee("invalid",e);var p=i=c=o=null,_=null,k=null;for(l in a)if(a.hasOwnProperty(l)){var X=a[l];if(X!=null)switch(l){case"name":o=X;break;case"type":c=X;break;case"checked":_=X;break;case"defaultChecked":k=X;break;case"value":i=X;break;case"defaultValue":p=X;break;case"children":case"dangerouslySetInnerHTML":if(X!=null)throw Error(u(137,t));break;default:Xe(e,t,l,X,a,null)}}Rc(e,i,p,_,k,c,o,!1);return;case"select":Ee("invalid",e),l=c=i=null;for(o in a)if(a.hasOwnProperty(o)&&(p=a[o],p!=null))switch(o){case"value":i=p;break;case"defaultValue":c=p;break;case"multiple":l=p;default:Xe(e,t,o,p,a,null)}t=i,a=c,e.multiple=!!l,t!=null?Ra(e,!!l,t,!1):a!=null&&Ra(e,!!l,a,!0);return;case"textarea":Ee("invalid",e),i=o=l=null;for(c in a)if(a.hasOwnProperty(c)&&(p=a[c],p!=null))switch(c){case"value":l=p;break;case"defaultValue":o=p;break;case"children":i=p;break;case"dangerouslySetInnerHTML":if(p!=null)throw Error(u(91));break;default:Xe(e,t,c,p,a,null)}Uc(e,l,o,i);return;case"option":for(_ in a)if(a.hasOwnProperty(_)&&(l=a[_],l!=null))switch(_){case"selected":e.selected=l&&typeof l!="function"&&typeof l!="symbol";break;default:Xe(e,t,_,l,a,null)}return;case"dialog":Ee("beforetoggle",e),Ee("toggle",e),Ee("cancel",e),Ee("close",e);break;case"iframe":case"object":Ee("load",e);break;case"video":case"audio":for(l=0;l<Pl.length;l++)Ee(Pl[l],e);break;case"image":Ee("error",e),Ee("load",e);break;case"details":Ee("toggle",e);break;case"embed":case"source":case"link":Ee("error",e),Ee("load",e);case"area":case"base":case"br":case"col":case"hr":case"keygen":case"meta":case"param":case"track":case"wbr":case"menuitem":for(k in a)if(a.hasOwnProperty(k)&&(l=a[k],l!=null))switch(k){case"children":case"dangerouslySetInnerHTML":throw Error(u(137,t));default:Xe(e,t,k,l,a,null)}return;default:if(Qi(t)){for(X in a)a.hasOwnProperty(X)&&(l=a[X],l!==void 0&&Or(e,t,X,l,a,void 0));return}}for(p in a)a.hasOwnProperty(p)&&(l=a[p],l!=null&&Xe(e,t,p,l,a,null))}function Vm(e,t,a,l){switch(t){case"div":case"span":case"svg":case"path":case"a":case"g":case"p":case"li":break;case"input":var o=null,i=null,c=null,p=null,_=null,k=null,X=null;for(H in a){var Q=a[H];if(a.hasOwnProperty(H)&&Q!=null)switch(H){case"checked":break;case"value":break;case"defaultValue":_=Q;default:l.hasOwnProperty(H)||Xe(e,t,H,null,l,Q)}}for(var U in l){var H=l[U];if(Q=a[U],l.hasOwnProperty(U)&&(H!=null||Q!=null))switch(U){case"type":i=H;break;case"name":o=H;break;case"checked":k=H;break;case"defaultChecked":X=H;break;case"value":c=H;break;case"defaultValue":p=H;break;case"children":case"dangerouslySetInnerHTML":if(H!=null)throw Error(u(137,t));break;default:H!==Q&&Xe(e,t,U,H,l,Q)}}Ji(e,c,p,_,k,X,i,o);return;case"select":H=c=p=U=null;for(i in a)if(_=a[i],a.hasOwnProperty(i)&&_!=null)switch(i){case"value":break;case"multiple":H=_;default:l.hasOwnProperty(i)||Xe(e,t,i,null,l,_)}for(o in l)if(i=l[o],_=a[o],l.hasOwnProperty(o)&&(i!=null||_!=null))switch(o){case"value":U=i;break;case"defaultValue":p=i;break;case"multiple":c=i;default:i!==_&&Xe(e,t,o,i,l,_)}t=p,a=c,l=H,U!=null?Ra(e,!!a,U,!1):!!l!=!!a&&(t!=null?Ra(e,!!a,t,!0):Ra(e,!!a,a?[]:"",!1));return;case"textarea":H=U=null;for(p in a)if(o=a[p],a.hasOwnProperty(p)&&o!=null&&!l.hasOwnProperty(p))switch(p){case"value":break;case"children":break;default:Xe(e,t,p,null,l,o)}for(c in l)if(o=l[c],i=a[c],l.hasOwnProperty(c)&&(o!=null||i!=null))switch(c){case"value":U=o;break;case"defaultValue":H=o;break;case"children":break;case"dangerouslySetInnerHTML":if(o!=null)throw Error(u(91));break;default:o!==i&&Xe(e,t,c,o,l,i)}Bc(e,U,H);return;case"option":for(var ae in a)if(U=a[ae],a.hasOwnProperty(ae)&&U!=null&&!l.hasOwnProperty(ae))switch(ae){case"selected":e.selected=!1;break;default:Xe(e,t,ae,null,l,U)}for(_ in l)if(U=l[_],H=a[_],l.hasOwnProperty(_)&&U!==H&&(U!=null||H!=null))switch(_){case"selected":e.selected=U&&typeof U!="function"&&typeof U!="symbol";break;default:Xe(e,t,_,U,l,H)}return;case"img":case"link":case"area":case"base":case"br":case"col":case"embed":case"hr":case"keygen":case"meta":case"param":case"source":case"track":case"wbr":case"menuitem":for(var pe in a)U=a[pe],a.hasOwnProperty(pe)&&U!=null&&!l.hasOwnProperty(pe)&&Xe(e,t,pe,null,l,U);for(k in l)if(U=l[k],H=a[k],l.hasOwnProperty(k)&&U!==H&&(U!=null||H!=null))switch(k){case"children":case"dangerouslySetInnerHTML":if(U!=null)throw Error(u(137,t));break;default:Xe(e,t,k,U,l,H)}return;default:if(Qi(t)){for(var Je in a)U=a[Je],a.hasOwnProperty(Je)&&U!==void 0&&!l.hasOwnProperty(Je)&&Or(e,t,Je,void 0,l,U);for(X in l)U=l[X],H=a[X],!l.hasOwnProperty(X)||U===H||U===void 0&&H===void 0||Or(e,t,X,U,l,H);return}}for(var j in a)U=a[j],a.hasOwnProperty(j)&&U!=null&&!l.hasOwnProperty(j)&&Xe(e,t,j,null,l,U);for(Q in l)U=l[Q],H=a[Q],!l.hasOwnProperty(Q)||U===H||U==null&&H==null||Xe(e,t,Q,U,l,H)}function Lp(e){switch(e){case"css":case"script":case"font":case"img":case"image":case"input":case"link":return!0;default:return!1}}function Im(){if(typeof performance.getEntriesByType=="function"){for(var e=0,t=0,a=performance.getEntriesByType("resource"),l=0;l<a.length;l++){var o=a[l],i=o.transferSize,c=o.initiatorType,p=o.duration;if(i&&p&&Lp(c)){for(c=0,p=o.responseEnd,l+=1;l<a.length;l++){var _=a[l],k=_.startTime;if(k>p)break;var X=_.transferSize,Q=_.initiatorType;X&&Lp(Q)&&(_=_.responseEnd,c+=X*(_<p?1:(p-k)/(_-k)))}if(--l,t+=8*(i+c)/(o.duration/1e3),e++,10<e)break}}if(0<e)return t/e/1e6}return navigator.connection&&(e=navigator.connection.downlink,typeof e=="number")?e:5}var zr=null,kr=null;function mi(e){return e.nodeType===9?e:e.ownerDocument}function qp(e){switch(e){case"http://www.w3.org/2000/svg":return 1;case"http://www.w3.org/1998/Math/MathML":return 2;default:return 0}}function Yp(e,t){if(e===0)switch(t){case"svg":return 1;case"math":return 2;default:return 0}return e===1&&t==="foreignObject"?0:e}function Rr(e,t){return e==="textarea"||e==="noscript"||typeof t.children=="string"||typeof t.children=="number"||typeof t.children=="bigint"||typeof t.dangerouslySetInnerHTML=="object"&&t.dangerouslySetInnerHTML!==null&&t.dangerouslySetInnerHTML.__html!=null}var Br=null;function Km(){var e=window.event;return e&&e.type==="popstate"?e===Br?!1:(Br=e,!0):(Br=null,!1)}var Xp=typeof setTimeout=="function"?setTimeout:void 0,$m=typeof clearTimeout=="function"?clearTimeout:void 0,Jp=typeof Promise=="function"?Promise:void 0,Wm=typeof queueMicrotask=="function"?queueMicrotask:typeof Jp<"u"?function(e){return Jp.resolve(null).then(e).catch(Pm)}:Xp;function Pm(e){setTimeout(function(){throw e})}function Fn(e){return e==="head"}function Zp(e,t){var a=t,l=0;do{var o=a.nextSibling;if(e.removeChild(a),o&&o.nodeType===8)if(a=o.data,a==="/$"||a==="/&"){if(l===0){e.removeChild(o),fl(t);return}l--}else if(a==="$"||a==="$?"||a==="$~"||a==="$!"||a==="&")l++;else if(a==="html")eo(e.ownerDocument.documentElement);else if(a==="head"){a=e.ownerDocument.head,eo(a);for(var i=a.firstChild;i;){var c=i.nextSibling,p=i.nodeName;i[vl]||p==="SCRIPT"||p==="STYLE"||p==="LINK"&&i.rel.toLowerCase()==="stylesheet"||a.removeChild(i),i=c}}else a==="body"&&eo(e.ownerDocument.body);a=o}while(a);fl(t)}function Qp(e,t){var a=e;e=0;do{var l=a.nextSibling;if(a.nodeType===1?t?(a._stashedDisplay=a.style.display,a.style.display="none"):(a.style.display=a._stashedDisplay||"",a.getAttribute("style")===""&&a.removeAttribute("style")):a.nodeType===3&&(t?(a._stashedText=a.nodeValue,a.nodeValue=""):a.nodeValue=a._stashedText||""),l&&l.nodeType===8)if(a=l.data,a==="/$"){if(e===0)break;e--}else a!=="$"&&a!=="$?"&&a!=="$~"&&a!=="$!"||e++;a=l}while(a)}function Ur(e){var t=e.firstChild;for(t&&t.nodeType===10&&(t=t.nextSibling);t;){var a=t;switch(t=t.nextSibling,a.nodeName){case"HTML":case"HEAD":case"BODY":Ur(a),Yi(a);continue;case"SCRIPT":case"STYLE":continue;case"LINK":if(a.rel.toLowerCase()==="stylesheet")continue}e.removeChild(a)}}function Fm(e,t,a,l){for(;e.nodeType===1;){var o=a;if(e.nodeName.toLowerCase()!==t.toLowerCase()){if(!l&&(e.nodeName!=="INPUT"||e.type!=="hidden"))break}else if(l){if(!e[vl])switch(t){case"meta":if(!e.hasAttribute("itemprop"))break;return e;case"link":if(i=e.getAttribute("rel"),i==="stylesheet"&&e.hasAttribute("data-precedence"))break;if(i!==o.rel||e.getAttribute("href")!==(o.href==null||o.href===""?null:o.href)||e.getAttribute("crossorigin")!==(o.crossOrigin==null?null:o.crossOrigin)||e.getAttribute("title")!==(o.title==null?null:o.title))break;return e;case"style":if(e.hasAttribute("data-precedence"))break;return e;case"script":if(i=e.getAttribute("src"),(i!==(o.src==null?null:o.src)||e.getAttribute("type")!==(o.type==null?null:o.type)||e.getAttribute("crossorigin")!==(o.crossOrigin==null?null:o.crossOrigin))&&i&&e.hasAttribute("async")&&!e.hasAttribute("itemprop"))break;return e;default:return e}}else if(t==="input"&&e.type==="hidden"){var i=o.name==null?null:""+o.name;if(o.type==="hidden"&&e.getAttribute("name")===i)return e}else return e;if(e=Wt(e.nextSibling),e===null)break}return null}function ey(e,t,a){if(t==="")return null;for(;e.nodeType!==3;)if((e.nodeType!==1||e.nodeName!=="INPUT"||e.type!=="hidden")&&!a||(e=Wt(e.nextSibling),e===null))return null;return e}function Vp(e,t){for(;e.nodeType!==8;)if((e.nodeType!==1||e.nodeName!=="INPUT"||e.type!=="hidden")&&!t||(e=Wt(e.nextSibling),e===null))return null;return e}function Gr(e){return e.data==="$?"||e.data==="$~"}function Hr(e){return e.data==="$!"||e.data==="$?"&&e.ownerDocument.readyState!=="loading"}function ty(e,t){var a=e.ownerDocument;if(e.data==="$~")e._reactRetry=t;else if(e.data!=="$?"||a.readyState!=="loading")t();else{var l=function(){t(),a.removeEventListener("DOMContentLoaded",l)};a.addEventListener("DOMContentLoaded",l),e._reactRetry=l}}function Wt(e){for(;e!=null;e=e.nextSibling){var t=e.nodeType;if(t===1||t===3)break;if(t===8){if(t=e.data,t==="$"||t==="$!"||t==="$?"||t==="$~"||t==="&"||t==="F!"||t==="F")break;if(t==="/$"||t==="/&")return null}}return e}var Lr=null;function Ip(e){e=e.nextSibling;for(var t=0;e;){if(e.nodeType===8){var a=e.data;if(a==="/$"||a==="/&"){if(t===0)return Wt(e.nextSibling);t--}else a!=="$"&&a!=="$!"&&a!=="$?"&&a!=="$~"&&a!=="&"||t++}e=e.nextSibling}return null}function Kp(e){e=e.previousSibling;for(var t=0;e;){if(e.nodeType===8){var a=e.data;if(a==="$"||a==="$!"||a==="$?"||a==="$~"||a==="&"){if(t===0)return e;t--}else a!=="/$"&&a!=="/&"||t++}e=e.previousSibling}return null}function $p(e,t,a){switch(t=mi(a),e){case"html":if(e=t.documentElement,!e)throw Error(u(452));return e;case"head":if(e=t.head,!e)throw Error(u(453));return e;case"body":if(e=t.body,!e)throw Error(u(454));return e;default:throw Error(u(451))}}function eo(e){for(var t=e.attributes;t.length;)e.removeAttributeNode(t[0]);Yi(e)}var Pt=new Map,Wp=new Set;function yi(e){return typeof e.getRootNode=="function"?e.getRootNode():e.nodeType===9?e:e.ownerDocument}var Mn=D.d;D.d={f:ny,r:ay,D:ly,C:oy,L:iy,m:sy,X:cy,S:ry,M:uy};function ny(){var e=Mn.f(),t=si();return e||t}function ay(e){var t=Oa(e);t!==null&&t.tag===5&&t.type==="form"?hd(t):Mn.r(e)}var ul=typeof document>"u"?null:document;function Pp(e,t,a){var l=ul;if(l&&typeof t=="string"&&t){var o=Jt(t);o='link[rel="'+e+'"][href="'+o+'"]',typeof a=="string"&&(o+='[crossorigin="'+a+'"]'),Wp.has(o)||(Wp.add(o),e={rel:e,crossOrigin:a,href:t},l.querySelector(o)===null&&(t=l.createElement("link"),bt(t,"link",e),pt(t),l.head.appendChild(t)))}}function ly(e){Mn.D(e),Pp("dns-prefetch",e,null)}function oy(e,t){Mn.C(e,t),Pp("preconnect",e,t)}function iy(e,t,a){Mn.L(e,t,a);var l=ul;if(l&&e&&t){var o='link[rel="preload"][as="'+Jt(t)+'"]';t==="image"&&a&&a.imageSrcSet?(o+='[imagesrcset="'+Jt(a.imageSrcSet)+'"]',typeof a.imageSizes=="string"&&(o+='[imagesizes="'+Jt(a.imageSizes)+'"]')):o+='[href="'+Jt(e)+'"]';var i=o;switch(t){case"style":i=dl(e);break;case"script":i=pl(e)}Pt.has(i)||(e=v({rel:"preload",href:t==="image"&&a&&a.imageSrcSet?void 0:e,as:t},a),Pt.set(i,e),l.querySelector(o)!==null||t==="style"&&l.querySelector(to(i))||t==="script"&&l.querySelector(no(i))||(t=l.createElement("link"),bt(t,"link",e),pt(t),l.head.appendChild(t)))}}function sy(e,t){Mn.m(e,t);var a=ul;if(a&&e){var l=t&&typeof t.as=="string"?t.as:"script",o='link[rel="modulepreload"][as="'+Jt(l)+'"][href="'+Jt(e)+'"]',i=o;switch(l){case"audioworklet":case"paintworklet":case"serviceworker":case"sharedworker":case"worker":case"script":i=pl(e)}if(!Pt.has(i)&&(e=v({rel:"modulepreload",href:e},t),Pt.set(i,e),a.querySelector(o)===null)){switch(l){case"audioworklet":case"paintworklet":case"serviceworker":case"sharedworker":case"worker":case"script":if(a.querySelector(no(i)))return}l=a.createElement("link"),bt(l,"link",e),pt(l),a.head.appendChild(l)}}}function ry(e,t,a){Mn.S(e,t,a);var l=ul;if(l&&e){var o=za(l).hoistableStyles,i=dl(e);t=t||"default";var c=o.get(i);if(!c){var p={loading:0,preload:null};if(c=l.querySelector(to(i)))p.loading=5;else{e=v({rel:"stylesheet",href:e,"data-precedence":t},a),(a=Pt.get(i))&&qr(e,a);var _=c=l.createElement("link");pt(_),bt(_,"link",e),_._p=new Promise(function(k,X){_.onload=k,_.onerror=X}),_.addEventListener("load",function(){p.loading|=1}),_.addEventListener("error",function(){p.loading|=2}),p.loading|=4,gi(c,t,l)}c={type:"stylesheet",instance:c,count:1,state:p},o.set(i,c)}}}function cy(e,t){Mn.X(e,t);var a=ul;if(a&&e){var l=za(a).hoistableScripts,o=pl(e),i=l.get(o);i||(i=a.querySelector(no(o)),i||(e=v({src:e,async:!0},t),(t=Pt.get(o))&&Yr(e,t),i=a.createElement("script"),pt(i),bt(i,"link",e),a.head.appendChild(i)),i={type:"script",instance:i,count:1,state:null},l.set(o,i))}}function uy(e,t){Mn.M(e,t);var a=ul;if(a&&e){var l=za(a).hoistableScripts,o=pl(e),i=l.get(o);i||(i=a.querySelector(no(o)),i||(e=v({src:e,async:!0,type:"module"},t),(t=Pt.get(o))&&Yr(e,t),i=a.createElement("script"),pt(i),bt(i,"link",e),a.head.appendChild(i)),i={type:"script",instance:i,count:1,state:null},l.set(o,i))}}function Fp(e,t,a,l){var o=(o=xe.current)?yi(o):null;if(!o)throw Error(u(446));switch(e){case"meta":case"title":return null;case"style":return typeof a.precedence=="string"&&typeof a.href=="string"?(t=dl(a.href),a=za(o).hoistableStyles,l=a.get(t),l||(l={type:"style",instance:null,count:0,state:null},a.set(t,l)),l):{type:"void",instance:null,count:0,state:null};case"link":if(a.rel==="stylesheet"&&typeof a.href=="string"&&typeof a.precedence=="string"){e=dl(a.href);var i=za(o).hoistableStyles,c=i.get(e);if(c||(o=o.ownerDocument||o,c={type:"stylesheet",instance:null,count:0,state:{loading:0,preload:null}},i.set(e,c),(i=o.querySelector(to(e)))&&!i._p&&(c.instance=i,c.state.loading=5),Pt.has(e)||(a={rel:"preload",as:"style",href:a.href,crossOrigin:a.crossOrigin,integrity:a.integrity,media:a.media,hrefLang:a.hrefLang,referrerPolicy:a.referrerPolicy},Pt.set(e,a),i||dy(o,e,a,c.state))),t&&l===null)throw Error(u(528,""));return c}if(t&&l!==null)throw Error(u(529,""));return null;case"script":return t=a.async,a=a.src,typeof a=="string"&&t&&typeof t!="function"&&typeof t!="symbol"?(t=pl(a),a=za(o).hoistableScripts,l=a.get(t),l||(l={type:"script",instance:null,count:0,state:null},a.set(t,l)),l):{type:"void",instance:null,count:0,state:null};default:throw Error(u(444,e))}}function dl(e){return'href="'+Jt(e)+'"'}function to(e){return'link[rel="stylesheet"]['+e+"]"}function ef(e){return v({},e,{"data-precedence":e.precedence,precedence:null})}function dy(e,t,a,l){e.querySelector('link[rel="preload"][as="style"]['+t+"]")?l.loading=1:(t=e.createElement("link"),l.preload=t,t.addEventListener("load",function(){return l.loading|=1}),t.addEventListener("error",function(){return l.loading|=2}),bt(t,"link",a),pt(t),e.head.appendChild(t))}function pl(e){return'[src="'+Jt(e)+'"]'}function no(e){return"script[async]"+e}function tf(e,t,a){if(t.count++,t.instance===null)switch(t.type){case"style":var l=e.querySelector('style[data-href~="'+Jt(a.href)+'"]');if(l)return t.instance=l,pt(l),l;var o=v({},a,{"data-href":a.href,"data-precedence":a.precedence,href:null,precedence:null});return l=(e.ownerDocument||e).createElement("style"),pt(l),bt(l,"style",o),gi(l,a.precedence,e),t.instance=l;case"stylesheet":o=dl(a.href);var i=e.querySelector(to(o));if(i)return t.state.loading|=4,t.instance=i,pt(i),i;l=ef(a),(o=Pt.get(o))&&qr(l,o),i=(e.ownerDocument||e).createElement("link"),pt(i);var c=i;return c._p=new Promise(function(p,_){c.onload=p,c.onerror=_}),bt(i,"link",l),t.state.loading|=4,gi(i,a.precedence,e),t.instance=i;case"script":return i=pl(a.src),(o=e.querySelector(no(i)))?(t.instance=o,pt(o),o):(l=a,(o=Pt.get(i))&&(l=v({},a),Yr(l,o)),e=e.ownerDocument||e,o=e.createElement("script"),pt(o),bt(o,"link",l),e.head.appendChild(o),t.instance=o);case"void":return null;default:throw Error(u(443,t.type))}else t.type==="stylesheet"&&(t.state.loading&4)===0&&(l=t.instance,t.state.loading|=4,gi(l,a.precedence,e));return t.instance}function gi(e,t,a){for(var l=a.querySelectorAll('link[rel="stylesheet"][data-precedence],style[data-precedence]'),o=l.length?l[l.length-1]:null,i=o,c=0;c<l.length;c++){var p=l[c];if(p.dataset.precedence===t)i=p;else if(i!==o)break}i?i.parentNode.insertBefore(e,i.nextSibling):(t=a.nodeType===9?a.head:a,t.insertBefore(e,t.firstChild))}function qr(e,t){e.crossOrigin==null&&(e.crossOrigin=t.crossOrigin),e.referrerPolicy==null&&(e.referrerPolicy=t.referrerPolicy),e.title==null&&(e.title=t.title)}function Yr(e,t){e.crossOrigin==null&&(e.crossOrigin=t.crossOrigin),e.referrerPolicy==null&&(e.referrerPolicy=t.referrerPolicy),e.integrity==null&&(e.integrity=t.integrity)}var vi=null;function nf(e,t,a){if(vi===null){var l=new Map,o=vi=new Map;o.set(a,l)}else o=vi,l=o.get(a),l||(l=new Map,o.set(a,l));if(l.has(e))return l;for(l.set(e,null),a=a.getElementsByTagName(e),o=0;o<a.length;o++){var i=a[o];if(!(i[vl]||i[mt]||e==="link"&&i.getAttribute("rel")==="stylesheet")&&i.namespaceURI!=="http://www.w3.org/2000/svg"){var c=i.getAttribute(t)||"";c=e+c;var p=l.get(c);p?p.push(i):l.set(c,[i])}}return l}function af(e,t,a){e=e.ownerDocument||e,e.head.insertBefore(a,t==="title"?e.querySelector("head > title"):null)}function py(e,t,a){if(a===1||t.itemProp!=null)return!1;switch(e){case"meta":case"title":return!0;case"style":if(typeof t.precedence!="string"||typeof t.href!="string"||t.href==="")break;return!0;case"link":if(typeof t.rel!="string"||typeof t.href!="string"||t.href===""||t.onLoad||t.onError)break;switch(t.rel){case"stylesheet":return e=t.disabled,typeof t.precedence=="string"&&e==null;default:return!0}case"script":if(t.async&&typeof t.async!="function"&&typeof t.async!="symbol"&&!t.onLoad&&!t.onError&&t.src&&typeof t.src=="string")return!0}return!1}function lf(e){return!(e.type==="stylesheet"&&(e.state.loading&3)===0)}function fy(e,t,a,l){if(a.type==="stylesheet"&&(typeof l.media!="string"||matchMedia(l.media).matches!==!1)&&(a.state.loading&4)===0){if(a.instance===null){var o=dl(l.href),i=t.querySelector(to(o));if(i){t=i._p,t!==null&&typeof t=="object"&&typeof t.then=="function"&&(e.count++,e=bi.bind(e),t.then(e,e)),a.state.loading|=4,a.instance=i,pt(i);return}i=t.ownerDocument||t,l=ef(l),(o=Pt.get(o))&&qr(l,o),i=i.createElement("link"),pt(i);var c=i;c._p=new Promise(function(p,_){c.onload=p,c.onerror=_}),bt(i,"link",l),a.instance=i}e.stylesheets===null&&(e.stylesheets=new Map),e.stylesheets.set(a,t),(t=a.state.preload)&&(a.state.loading&3)===0&&(e.count++,a=bi.bind(e),t.addEventListener("load",a),t.addEventListener("error",a))}}var Xr=0;function hy(e,t){return e.stylesheets&&e.count===0&&Si(e,e.stylesheets),0<e.count||0<e.imgCount?function(a){var l=setTimeout(function(){if(e.stylesheets&&Si(e,e.stylesheets),e.unsuspend){var i=e.unsuspend;e.unsuspend=null,i()}},6e4+t);0<e.imgBytes&&Xr===0&&(Xr=62500*Im());var o=setTimeout(function(){if(e.waitingForImages=!1,e.count===0&&(e.stylesheets&&Si(e,e.stylesheets),e.unsuspend)){var i=e.unsuspend;e.unsuspend=null,i()}},(e.imgBytes>Xr?50:800)+t);return e.unsuspend=a,function(){e.unsuspend=null,clearTimeout(l),clearTimeout(o)}}:null}function bi(){if(this.count--,this.count===0&&(this.imgCount===0||!this.waitingForImages)){if(this.stylesheets)Si(this,this.stylesheets);else if(this.unsuspend){var e=this.unsuspend;this.unsuspend=null,e()}}}var _i=null;function Si(e,t){e.stylesheets=null,e.unsuspend!==null&&(e.count++,_i=new Map,t.forEach(my,e),_i=null,bi.call(e))}function my(e,t){if(!(t.state.loading&4)){var a=_i.get(e);if(a)var l=a.get(null);else{a=new Map,_i.set(e,a);for(var o=e.querySelectorAll("link[data-precedence],style[data-precedence]"),i=0;i<o.length;i++){var c=o[i];(c.nodeName==="LINK"||c.getAttribute("media")!=="not all")&&(a.set(c.dataset.precedence,c),l=c)}l&&a.set(null,l)}o=t.instance,c=o.getAttribute("data-precedence"),i=a.get(c)||l,i===l&&a.set(null,o),a.set(c,o),this.count++,l=bi.bind(this),o.addEventListener("load",l),o.addEventListener("error",l),i?i.parentNode.insertBefore(o,i.nextSibling):(e=e.nodeType===9?e.head:e,e.insertBefore(o,e.firstChild)),t.state.loading|=4}}var ao={$$typeof:G,Provider:null,Consumer:null,_currentValue:V,_currentValue2:V,_threadCount:0};function yy(e,t,a,l,o,i,c,p,_){this.tag=1,this.containerInfo=e,this.pingCache=this.current=this.pendingChildren=null,this.timeoutHandle=-1,this.callbackNode=this.next=this.pendingContext=this.context=this.cancelPendingCommit=null,this.callbackPriority=0,this.expirationTimes=Gi(-1),this.entangledLanes=this.shellSuspendCounter=this.errorRecoveryDisabledLanes=this.expiredLanes=this.warmLanes=this.pingedLanes=this.suspendedLanes=this.pendingLanes=0,this.entanglements=Gi(0),this.hiddenUpdates=Gi(null),this.identifierPrefix=l,this.onUncaughtError=o,this.onCaughtError=i,this.onRecoverableError=c,this.pooledCache=null,this.pooledCacheLanes=0,this.formState=_,this.incompleteTransitions=new Map}function of(e,t,a,l,o,i,c,p,_,k,X,Q){return e=new yy(e,t,a,c,_,k,X,Q,p),t=1,i===!0&&(t|=24),i=Rt(3,null,null,t),e.current=i,i.stateNode=e,t=xs(),t.refCount++,e.pooledCache=t,t.refCount++,i.memoizedState={element:l,isDehydrated:a,cache:t},Ns(i),e}function sf(e){return e?(e=Xa,e):Xa}function rf(e,t,a,l,o,i){o=sf(o),l.context===null?l.context=o:l.pendingContext=o,l=Yn(t),l.payload={element:a},i=i===void 0?null:i,i!==null&&(l.callback=i),a=Xn(e,l,t),a!==null&&(Ot(a,e,t),Rl(a,e,t))}function cf(e,t){if(e=e.memoizedState,e!==null&&e.dehydrated!==null){var a=e.retryLane;e.retryLane=a!==0&&a<t?a:t}}function Jr(e,t){cf(e,t),(e=e.alternate)&&cf(e,t)}function uf(e){if(e.tag===13||e.tag===31){var t=ma(e,67108864);t!==null&&Ot(t,e,67108864),Jr(e,67108864)}}function df(e){if(e.tag===13||e.tag===31){var t=Lt();t=Hi(t);var a=ma(e,t);a!==null&&Ot(a,e,t),Jr(e,t)}}var xi=!0;function gy(e,t,a,l){var o=M.T;M.T=null;var i=D.p;try{D.p=2,Zr(e,t,a,l)}finally{D.p=i,M.T=o}}function vy(e,t,a,l){var o=M.T;M.T=null;var i=D.p;try{D.p=8,Zr(e,t,a,l)}finally{D.p=i,M.T=o}}function Zr(e,t,a,l){if(xi){var o=Qr(l);if(o===null)Dr(e,t,l,Ti,a),ff(e,l);else if(_y(o,e,t,a,l))l.stopPropagation();else if(ff(e,l),t&4&&-1<by.indexOf(e)){for(;o!==null;){var i=Oa(o);if(i!==null)switch(i.tag){case 3:if(i=i.stateNode,i.current.memoizedState.isDehydrated){var c=ua(i.pendingLanes);if(c!==0){var p=i;for(p.pendingLanes|=2,p.entangledLanes|=2;c;){var _=1<<31-xt(c);p.entanglements[1]|=_,c&=~_}cn(i),(Re&6)===0&&(oi=ge()+500,Wl(0))}}break;case 31:case 13:p=ma(i,2),p!==null&&Ot(p,i,2),si(),Jr(i,2)}if(i=Qr(l),i===null&&Dr(e,t,l,Ti,a),i===o)break;o=i}o!==null&&l.stopPropagation()}else Dr(e,t,l,null,a)}}function Qr(e){return e=Ii(e),Vr(e)}var Ti=null;function Vr(e){if(Ti=null,e=Da(e),e!==null){var t=f(e);if(t===null)e=null;else{var a=t.tag;if(a===13){if(e=b(t),e!==null)return e;e=null}else if(a===31){if(e=y(t),e!==null)return e;e=null}else if(a===3){if(t.stateNode.current.memoizedState.isDehydrated)return t.tag===3?t.stateNode.containerInfo:null;e=null}else t!==e&&(e=null)}}return Ti=e,null}function pf(e){switch(e){case"beforetoggle":case"cancel":case"click":case"close":case"contextmenu":case"copy":case"cut":case"auxclick":case"dblclick":case"dragend":case"dragstart":case"drop":case"focusin":case"focusout":case"input":case"invalid":case"keydown":case"keypress":case"keyup":case"mousedown":case"mouseup":case"paste":case"pause":case"play":case"pointercancel":case"pointerdown":case"pointerup":case"ratechange":case"reset":case"resize":case"seeked":case"submit":case"toggle":case"touchcancel":case"touchend":case"touchstart":case"volumechange":case"change":case"selectionchange":case"textInput":case"compositionstart":case"compositionend":case"compositionupdate":case"beforeblur":case"afterblur":case"beforeinput":case"blur":case"fullscreenchange":case"focus":case"hashchange":case"popstate":case"select":case"selectstart":return 2;case"drag":case"dragenter":case"dragexit":case"dragleave":case"dragover":case"mousemove":case"mouseout":case"mouseover":case"pointermove":case"pointerout":case"pointerover":case"scroll":case"touchmove":case"wheel":case"mouseenter":case"mouseleave":case"pointerenter":case"pointerleave":return 8;case"message":switch(pn()){case fo:return 2;case ho:return 8;case ra:case ki:return 32;case mo:return 268435456;default:return 32}default:return 32}}var Ir=!1,ea=null,ta=null,na=null,lo=new Map,oo=new Map,aa=[],by="mousedown mouseup touchcancel touchend touchstart auxclick dblclick pointercancel pointerdown pointerup dragend dragstart drop compositionend compositionstart keydown keypress keyup input textInput copy cut paste click change contextmenu reset".split(" ");function ff(e,t){switch(e){case"focusin":case"focusout":ea=null;break;case"dragenter":case"dragleave":ta=null;break;case"mouseover":case"mouseout":na=null;break;case"pointerover":case"pointerout":lo.delete(t.pointerId);break;case"gotpointercapture":case"lostpointercapture":oo.delete(t.pointerId)}}function io(e,t,a,l,o,i){return e===null||e.nativeEvent!==i?(e={blockedOn:t,domEventName:a,eventSystemFlags:l,nativeEvent:i,targetContainers:[o]},t!==null&&(t=Oa(t),t!==null&&uf(t)),e):(e.eventSystemFlags|=l,t=e.targetContainers,o!==null&&t.indexOf(o)===-1&&t.push(o),e)}function _y(e,t,a,l,o){switch(t){case"focusin":return ea=io(ea,e,t,a,l,o),!0;case"dragenter":return ta=io(ta,e,t,a,l,o),!0;case"mouseover":return na=io(na,e,t,a,l,o),!0;case"pointerover":var i=o.pointerId;return lo.set(i,io(lo.get(i)||null,e,t,a,l,o)),!0;case"gotpointercapture":return i=o.pointerId,oo.set(i,io(oo.get(i)||null,e,t,a,l,o)),!0}return!1}function hf(e){var t=Da(e.target);if(t!==null){var a=f(t);if(a!==null){if(t=a.tag,t===13){if(t=b(a),t!==null){e.blockedOn=t,Ac(e.priority,function(){df(a)});return}}else if(t===31){if(t=y(a),t!==null){e.blockedOn=t,Ac(e.priority,function(){df(a)});return}}else if(t===3&&a.stateNode.current.memoizedState.isDehydrated){e.blockedOn=a.tag===3?a.stateNode.containerInfo:null;return}}}e.blockedOn=null}function wi(e){if(e.blockedOn!==null)return!1;for(var t=e.targetContainers;0<t.length;){var a=Qr(e.nativeEvent);if(a===null){a=e.nativeEvent;var l=new a.constructor(a.type,a);Vi=l,a.target.dispatchEvent(l),Vi=null}else return t=Oa(a),t!==null&&uf(t),e.blockedOn=a,!1;t.shift()}return!0}function mf(e,t,a){wi(e)&&a.delete(t)}function Sy(){Ir=!1,ea!==null&&wi(ea)&&(ea=null),ta!==null&&wi(ta)&&(ta=null),na!==null&&wi(na)&&(na=null),lo.forEach(mf),oo.forEach(mf)}function Ei(e,t){e.blockedOn===t&&(e.blockedOn=null,Ir||(Ir=!0,r.unstable_scheduleCallback(r.unstable_NormalPriority,Sy)))}var Ni=null;function yf(e){Ni!==e&&(Ni=e,r.unstable_scheduleCallback(r.unstable_NormalPriority,function(){Ni===e&&(Ni=null);for(var t=0;t<e.length;t+=3){var a=e[t],l=e[t+1],o=e[t+2];if(typeof l!="function"){if(Vr(l||a)===null)continue;break}var i=Oa(a);i!==null&&(e.splice(t,3),t-=3,Qs(i,{pending:!0,data:o,method:a.method,action:l},l,o))}}))}function fl(e){function t(_){return Ei(_,e)}ea!==null&&Ei(ea,e),ta!==null&&Ei(ta,e),na!==null&&Ei(na,e),lo.forEach(t),oo.forEach(t);for(var a=0;a<aa.length;a++){var l=aa[a];l.blockedOn===e&&(l.blockedOn=null)}for(;0<aa.length&&(a=aa[0],a.blockedOn===null);)hf(a),a.blockedOn===null&&aa.shift();if(a=(e.ownerDocument||e).$$reactFormReplay,a!=null)for(l=0;l<a.length;l+=3){var o=a[l],i=a[l+1],c=o[Nt]||null;if(typeof i=="function")c||yf(a);else if(c){var p=null;if(i&&i.hasAttribute("formAction")){if(o=i,c=i[Nt]||null)p=c.formAction;else if(Vr(o)!==null)continue}else p=c.action;typeof p=="function"?a[l+1]=p:(a.splice(l,3),l-=3),yf(a)}}}function gf(){function e(i){i.canIntercept&&i.info==="react-transition"&&i.intercept({handler:function(){return new Promise(function(c){return o=c})},focusReset:"manual",scroll:"manual"})}function t(){o!==null&&(o(),o=null),l||setTimeout(a,20)}function a(){if(!l&&!navigation.transition){var i=navigation.currentEntry;i&&i.url!=null&&navigation.navigate(i.url,{state:i.getState(),info:"react-transition",history:"replace"})}}if(typeof navigation=="object"){var l=!1,o=null;return navigation.addEventListener("navigate",e),navigation.addEventListener("navigatesuccess",t),navigation.addEventListener("navigateerror",t),setTimeout(a,100),function(){l=!0,navigation.removeEventListener("navigate",e),navigation.removeEventListener("navigatesuccess",t),navigation.removeEventListener("navigateerror",t),o!==null&&(o(),o=null)}}}function Kr(e){this._internalRoot=e}Ai.prototype.render=Kr.prototype.render=function(e){var t=this._internalRoot;if(t===null)throw Error(u(409));var a=t.current,l=Lt();rf(a,l,e,t,null,null)},Ai.prototype.unmount=Kr.prototype.unmount=function(){var e=this._internalRoot;if(e!==null){this._internalRoot=null;var t=e.containerInfo;rf(e.current,2,null,e,null,null),si(),t[Ma]=null}};function Ai(e){this._internalRoot=e}Ai.prototype.unstable_scheduleHydration=function(e){if(e){var t=Nc();e={blockedOn:null,target:e,priority:t};for(var a=0;a<aa.length&&t!==0&&t<aa[a].priority;a++);aa.splice(a,0,e),a===0&&hf(e)}};var vf=n.version;if(vf!=="19.2.4")throw Error(u(527,vf,"19.2.4"));D.findDOMNode=function(e){var t=e._reactInternals;if(t===void 0)throw typeof e.render=="function"?Error(u(188)):(e=Object.keys(e).join(","),Error(u(268,e)));return e=g(t),e=e!==null?N(e):null,e=e===null?null:e.stateNode,e};var xy={bundleType:0,version:"19.2.4",rendererPackageName:"react-dom",currentDispatcherRef:M,reconcilerVersion:"19.2.4"};if(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__<"u"){var Ci=__REACT_DEVTOOLS_GLOBAL_HOOK__;if(!Ci.isDisabled&&Ci.supportsFiber)try{ca=Ci.inject(xy),St=Ci}catch{}}return so.createRoot=function(e,t){if(!d(e))throw Error(u(299));var a=!1,l="",o=wd,i=Ed,c=Nd;return t!=null&&(t.unstable_strictMode===!0&&(a=!0),t.identifierPrefix!==void 0&&(l=t.identifierPrefix),t.onUncaughtError!==void 0&&(o=t.onUncaughtError),t.onCaughtError!==void 0&&(i=t.onCaughtError),t.onRecoverableError!==void 0&&(c=t.onRecoverableError)),t=of(e,1,!1,null,null,a,l,null,o,i,c,gf),e[Ma]=t.current,Mr(e),new Kr(t)},so.hydrateRoot=function(e,t,a){if(!d(e))throw Error(u(299));var l=!1,o="",i=wd,c=Ed,p=Nd,_=null;return a!=null&&(a.unstable_strictMode===!0&&(l=!0),a.identifierPrefix!==void 0&&(o=a.identifierPrefix),a.onUncaughtError!==void 0&&(i=a.onUncaughtError),a.onCaughtError!==void 0&&(c=a.onCaughtError),a.onRecoverableError!==void 0&&(p=a.onRecoverableError),a.formState!==void 0&&(_=a.formState)),t=of(e,1,!0,t,a??null,l,o,_,i,c,p,gf),t.context=sf(null),a=t.current,l=Lt(),l=Hi(l),o=Yn(l),o.callback=null,Xn(a,o,l),a=l,t.current.lanes=a,gl(t,a),cn(t),e[Ma]=t.current,Mr(e),new Ai(t)},so.version="19.2.4",so}var wf;function Qy(){if(wf)return $r.exports;wf=1;function r(){if(!(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__>"u"||typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE!="function"))try{__REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE(r)}catch(n){console.error(n)}}return r(),$r.exports=Zy(),$r.exports}var Vy=Qy(),Fr={exports:{}},ec={};/**
 * @license React
 * react-compiler-runtime.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Ef;function Iy(){if(Ef)return ec;Ef=1;var r=Qf().__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE;return ec.c=function(n){return r.H.useMemoCache(n)},ec}var Nf;function Ky(){return Nf||(Nf=1,Fr.exports=Iy()),Fr.exports}var Se=Ky();const $y="_wrapper_h677m_2",Wy="_header_h677m_10",Py="_headerActions_h677m_21",Fy="_title_h677m_27",eg="_panelGroup_h677m_36",tg="_clipboardToggle_h677m_43",ng="_helpToggle_h677m_66",ag="_helpButtonWrapper_h677m_93",lg="_helpTogglePulsing_h677m_97",og="_helpHint_h677m_112",ig="_helpHintFading_h677m_139",sg="_helpHintKbd_h677m_144",rg="_resizeHandle_h677m_153",zt={wrapper:$y,header:Wy,headerActions:Py,title:Fy,panelGroup:eg,clipboardToggle:tg,helpToggle:ng,helpButtonWrapper:ag,helpTogglePulsing:lg,helpHint:og,helpHintFading:ig,helpHintKbd:sg,resizeHandle:rg},cg=r=>{try{return!new DOMParser().parseFromString(r.trim(),"text/xml").querySelector("parsererror")}catch{return!1}},ug=r=>{try{return JSON.parse(r),!0}catch{return!1}},dg=r=>r.trim()?ug(r)?{valid:!0,error:null,type:"json"}:cg(r)?{valid:!0,error:null,type:"xml"}:{valid:!1,error:"Invalid JSON/XML format",type:null}:{valid:!0,error:null,type:null},rc=r=>{try{const n=JSON.parse(r);return JSON.stringify(n,null,2)}catch{return r}},pg=()=>{const r=Se.c(8);let n;r[0]===Symbol.for("react.memo_cache_sentinel")?(n=[],r[0]=n):n=r[0];const[s,u]=x.useState(n),d=x.useRef(0);let f;r[1]===Symbol.for("react.memo_cache_sentinel")?(f=new Set,r[1]=f):f=r[1];const b=x.useRef(f);let y,m;r[2]===Symbol.for("react.memo_cache_sentinel")?(y=()=>()=>{b.current.forEach(clearTimeout)},m=[],r[2]=y,r[3]=m):(y=r[2],m=r[3]),x.useEffect(y,m);let g;r[4]===Symbol.for("react.memo_cache_sentinel")?(g=(E,C)=>{const S=C===void 0?"info":C,O=d.current=d.current+1;u(G=>[...G,{id:O,message:E,type:S}]);const R=setTimeout(()=>{b.current.delete(R),u(G=>G.filter(L=>L.id!==O))},3e3);b.current.add(R)},r[4]=g):g=r[4];const N=g;let v;r[5]===Symbol.for("react.memo_cache_sentinel")?(v=E=>{u(C=>C.filter(S=>S.id!==E))},r[5]=v):v=r[5];const T=v;let A;return r[6]!==s?(A={toasts:s,addToast:N,removeToast:T},r[6]=s,r[7]=A):A=r[7],A},oa=(r,n)=>{const s=x.useCallback(()=>{try{const f=window.localStorage.getItem(r);return f?JSON.parse(f):n}catch{return n}},[r]),[u,d]=x.useState(s);return x.useEffect(()=>{d(s())},[r]),x.useEffect(()=>{try{window.localStorage.setItem(r,JSON.stringify(u))}catch(f){console.error(`Error setting localStorage key "${r}":`,f)}},[r,u]),x.useEffect(()=>{const f=b=>{(b.key===r||b.key===null)&&d(s())};return window.addEventListener("storage",f),()=>window.removeEventListener("storage",f)},[r,s]),x.useEffect(()=>{const f=()=>d(s());return window.addEventListener("focus",f),document.addEventListener("visibilitychange",f),()=>{window.removeEventListener("focus",f),document.removeEventListener("visibilitychange",f)}},[s]),[u,d]},fg=200,Af=50,hg=8,mg=2e4,On=[{path:"/json-path",label:"JSON-Path",title:"JSON-Path Playground",wsPath:"/ws/json/path",storageKeyPayload:"jsonpath-last-payload",storageKeyHistory:"jsonpath-command-history",storageKeyTab:"jsonpath-right-tab",supportsUpload:!0,tabs:["payload","graph","graph-data"]},{path:"/",label:"Minigraph",title:"Minigraph Playground",wsPath:"/ws/graph/playground",storageKeyPayload:"minigraph-last-payload",storageKeyHistory:"minigraph-command-history",storageKeyTab:"minigraph-right-tab",storageKeySavedGraphs:"minigraph-saved-graphs",storageKeyHelpTopic:"minigraph-help-topic",supportsClipboard:!0,supportsHelp:!0,tabs:["graph","graph-data"]}],ji={json_simple:JSON.stringify({name:"John Doe",age:30,city:"New York"},null,2),json_nested:JSON.stringify({user:{name:"Jane Smith",profile:{email:"jane@example.com",address:{city:"San Francisco",country:"USA"}}}},null,2),json_array:JSON.stringify([{id:1,name:"Item 1",status:"active"},{id:2,name:"Item 2",status:"pending"},{id:3,name:"Item 3",status:"inactive"}],null,2),xml_simple:`<?xml version="1.0" encoding="UTF-8"?>
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
</items>`};function Kf(r){return`ws://${window.location.host}${r}`}function tc(r,n,s,u){const d=r[n]??{phase:"idle",messages:[]},f=[...d.messages,{id:s,raw:u}];return f.length>fg&&f.shift(),{...r,[n]:{...d,messages:f}}}function yg(r,n){const s=r[n.path]??{phase:"idle",messages:[]};switch(n.type){case"CONNECTING":return{...r,[n.path]:{...s,phase:"connecting"}};case"CONNECTED":return tc({...r,[n.path]:{...s,phase:"connected"}},n.path,n.id,n.msg);case"MESSAGE_RECEIVED":return tc(r,n.path,n.id,n.msg);case"DISCONNECTED":return tc({...r,[n.path]:{...s,phase:"idle"}},n.path,n.id,n.msg);case"CONNECT_ERROR":return{...r,[n.path]:{...s,phase:"idle"}};case"CLEAR_MESSAGES":return{...r,[n.path]:{...s,messages:[]}};default:return r}}const $f=x.createContext(null);function gg({children:r}){const[n,s]=x.useReducer(yg,{}),u=x.useRef({}),d=x.useRef({}),f=x.useRef({});x.useEffect(()=>()=>{Object.entries(u.current).forEach(([B,J])=>{J==null||J.close();const I=d.current[B];I&&clearInterval(I)})},[]);const b=B=>Kf(B),y=B=>(f.current[B]=(f.current[B]??0)+1,f.current[B]),m=()=>{const B=new Date().toString(),J=B.indexOf("GMT");return J>0?B.substring(0,J).trim():B},g=(B,J)=>JSON.stringify({type:B,message:J,time:m()}),N=B=>{try{const J=JSON.parse(B);if(J!==null&&typeof J=="object"){const I=J.type;return I==="ping"||I==="pong"}}catch{}return!1},v=x.useCallback((B,J)=>{if(!window.WebSocket){J==null||J("WebSocket not supported by your browser","error");return}const I=u.current[B];if(I&&(I.readyState===WebSocket.OPEN||I.readyState===WebSocket.CONNECTING)){J==null||J("Already connected","error");return}s({type:"CONNECTING",path:B});const K=new WebSocket(b(B));u.current[B]=K,K.onopen=()=>{s({type:"CONNECTED",path:B,id:y(B),msg:g("info","connected")}),J==null||J("Connected to WebSocket","success"),K.send(JSON.stringify({type:"welcome"})),d.current[B]=setInterval(()=>{K.readyState===WebSocket.OPEN&&K.send(g("ping","keep alive"))},mg)},K.onmessage=$=>{N($.data)||s({type:"MESSAGE_RECEIVED",path:B,id:y(B),msg:$.data})},K.onerror=()=>{s({type:"CONNECT_ERROR",path:B})},K.onclose=$=>{const ne=d.current[B];ne&&(clearInterval(ne),d.current[B]=null),s({type:"DISCONNECTED",path:B,id:y(B),msg:g("info",`disconnected - (${$.code}) ${$.reason}`)}),J==null||J("Disconnected from WebSocket","info"),u.current[B]===K&&(u.current[B]=null)}},[]),T=x.useCallback(B=>{const J=u.current[B];J?J.close():s({type:"MESSAGE_RECEIVED",path:B,id:y(B),msg:g("error","already disconnected")})},[]);x.useEffect(()=>(On.forEach(B=>{v(B.wsPath)}),()=>{On.forEach(B=>{const J=u.current[B.wsPath];J&&J.close()})}),[]);const A=x.useCallback((B,J)=>{const I=u.current[B];return I&&I.readyState===WebSocket.OPEN?(I.send(J),!0):!1},[]),E=x.useCallback((B,J)=>{s({type:"MESSAGE_RECEIVED",path:B,id:y(B),msg:J})},[]),C=x.useCallback(B=>{s({type:"CLEAR_MESSAGES",path:B})},[]),[S,O]=x.useState({}),R=x.useCallback((B,J)=>{O(I=>{if(J===null){const K={...I};return delete K[B],K}return{...I,[B]:J}})},[]),G=x.useCallback(B=>S[B]??null,[S]),L=x.useCallback(B=>{const J=S[B]??null;return J!==null&&O(I=>{const K={...I};return delete K[B],K}),J},[S]),Y=x.useCallback(B=>n[B]??{phase:"idle",messages:[]},[n]),q=x.useMemo(()=>({getSlot:Y,connect:v,disconnect:T,send:A,appendMessage:E,clearMessages:C,setPendingPayload:R,peekPendingPayload:G,takePendingPayload:L}),[Y,v,T,A,E,C,R,G,L]);return h.jsx($f.Provider,{value:q,children:r})}function bc(){const r=x.useContext($f);if(!r)throw new Error("useWebSocketContext must be used inside <WebSocketProvider>");return r}const vg=r=>{try{const n=JSON.parse(r);return{type:n.type||"info",message:n.message||r,time:n.time,raw:r}}catch{return{type:"raw",message:r,time:null,raw:r}}},bg=r=>({info:"ℹ️",error:"❌",ping:"🔄",welcome:"👋",raw:""})[r]??"•",po=r=>{try{const n=JSON.parse(r);if(typeof n=="object"&&n!==null)return{isJSON:!0,data:n}}catch{}return{isJSON:!1,data:null}};function _g(r){if(!r.includes("Graph exported to "))return null;const n=Sc(r);if(!n)return null;const s=n.split("/")[4];return s?{graphName:s,apiPath:n}:null}function Sg(r){return r.includes("Invalid filename")?{reason:"invalid-name"}:r.includes("Expect root node name")?{reason:"root-name-conflict"}:null}function _c(r){const n=po(r);return n.isJSON?(typeof n.data.type=="string",!1):!0}function Sc(r){const n=r.match(/\/api\/graph\/model\/([^\s'"]+)/);return n?n[0]:null}function Wf(r){return _c(r)?Sc(r)!==null:!1}function Pf(r){const n=r.match(/\/api\/json\/content\/([\w-]+)/);return n?n[0]:null}function xg(r){const n=r.match(/Large payload \((\d+)\)\s*->\s*GET\s+(\/api\/inspect\/[^\s]+)/i);if(!n)return null;const s=parseInt(n[1],10),u=n[2],f=`${u.split("/").filter(Boolean).pop()??"payload"}.json`;return{apiPath:u,byteSize:s,filename:f}}function Tg(r){const n=r.match(/You may upload .*?->\s*POST\s+(\/api\/mock\/[\w-]+)/i);return n?n[1]:null}function wg(r){if(!r.startsWith("> "))return!1;const n=r.slice(2).trim().toLowerCase();return n==="help"||n.startsWith("help ")?!0:n.startsWith("describe ")?!n.slice(9).trim().startsWith("graph"):!1}function Eg(r){if(!r.startsWith("> ")||!r.slice(2).trimStart().toLowerCase().startsWith("import graph from "))return null;const s=r.slice(2).trimStart().slice(18).trim();return s.length>0?s:null}function Ng(r){if(!_c(r)||r.startsWith("> ")||Wf(r))return null;const n=r.toLowerCase();return n.includes("graph model imported as draft")?"import-graph":n.includes(" -> ")&&n.includes("removed")||n.startsWith("node ")&&(n.includes(" created")||n.includes(" updated")||n.includes(" deleted")||n.includes(" connected to ")||n.includes(" imported from ")||n.includes(" overwritten by node from "))?"node-mutation":null}const Ag={command:"",historyIndex:-1,draftCommand:""};function Cg(r,n){switch(n.type){case"SET_COMMAND":return{...r,command:n.value,historyIndex:-1,draftCommand:""};case"CLEAR_COMMAND":return{...r,command:"",historyIndex:-1,draftCommand:""};case"SET_HISTORY_INDEX":return{...r,historyIndex:n.index,command:n.command};case"ENTER_HISTORY":return{...r,historyIndex:0,command:n.command,draftCommand:r.command};case"EXIT_HISTORY":return{...r,historyIndex:-1,command:r.draftCommand,draftCommand:""};default:return r}}function jg(r){const n=Se.c(77),{wsPath:s,storageKeyHistory:u,payload:d,addToast:f,bus:b,handleLocalCommand:y}=r,m=bc();let g;n[0]!==m||n[1]!==s?(g=m.getSlot(s),n[0]=m,n[1]=s,n[2]=g):g=n[2];const{phase:N,messages:v}=g,T=N==="connected",A=N==="connecting",[E,C]=x.useReducer(Cg,Ag),{command:S,historyIndex:O}=E;let R;n[3]===Symbol.for("react.memo_cache_sentinel")?(R=[],n[3]=R):R=n[3];const[G,L]=oa(u,R),Y=x.useRef(null),q=x.useRef(!1);let B;n[4]===Symbol.for("react.memo_cache_sentinel")?(B=()=>{Y.current&&(Y.current.scrollTop=Y.current.scrollHeight)},n[4]=B):B=n[4];let J;n[5]!==v?(J=[v],n[5]=v,n[6]=J):J=n[6],x.useEffect(B,J);let I;n[7]!==f||n[8]!==m||n[9]!==s?(I=()=>{m.connect(s,f)},n[7]=f,n[8]=m,n[9]=s,n[10]=I):I=n[10];const K=I;let $;n[11]!==m||n[12]!==s?($=()=>{m.disconnect(s)},n[11]=m,n[12]=s,n[13]=$):$=n[13];const ne=$;let ue;n[14]!==S||n[15]!==m||n[16]!==y||n[17]!==G||n[18]!==d||n[19]!==N||n[20]!==L||n[21]!==s?(ue=()=>{if(N!=="connected")return;const W=S.trim();if(W.length!==0){if((y==null?void 0:y(W))===!0){G[0]!==W&&L(ye=>[W,...ye].slice(0,Af)),m.appendMessage(s,"> "+W),C({type:"CLEAR_COMMAND"});return}m.send(s,W),G[0]!==W&&L(ye=>[W,...ye].slice(0,Af)),W==="load"&&(d.length===0?m.appendMessage(s,"ERROR: please paste JSON/XML payload in input text area"):m.send(s,d)),C({type:"CLEAR_COMMAND"})}},n[14]=S,n[15]=m,n[16]=y,n[17]=G,n[18]=d,n[19]=N,n[20]=L,n[21]=s,n[22]=ue):ue=n[22];const ce=ue;let ie;n[23]!==G||n[24]!==O?(ie=W=>{if(W.key==="ArrowUp"){if(W.preventDefault(),G.length===0)return;if(O===-1)C({type:"ENTER_HISTORY",command:G[0]});else if(O<G.length-1){const ye=O+1;C({type:"SET_HISTORY_INDEX",index:ye,command:G[ye]})}}else if(W.key==="ArrowDown")if(W.preventDefault(),O<=0)O===0&&C({type:"EXIT_HISTORY"});else{const ye=O-1;C({type:"SET_HISTORY_INDEX",index:ye,command:G[ye]})}},n[23]=G,n[24]=O,n[25]=ie):ie=n[25];const M=ie;let D,V;n[26]!==f||n[27]!==b||n[28]!==m||n[29]!==d||n[30]!==s?(V=()=>{if(b)return b.on("upload.contentPath",W=>{if(!q.current)return;if(q.current=!1,d.length===0){m.appendMessage(s,"ERROR: please paste JSON/XML payload in the input text area");return}let ye;try{ye=JSON.stringify(JSON.parse(d))}catch{m.appendMessage(s,"ERROR: payload is not valid JSON — cannot upload");return}fetch(W.uploadPath,{method:"POST",headers:{"Content-Type":"application/json"},body:ye}).then(Te=>{if(!Te.ok)throw new Error(`HTTP ${Te.status}`);f("Payload uploaded successfully","success")}).catch(Te=>{m.appendMessage(s,`ERROR: upload failed — ${Te.message}`),f(`Upload failed: ${Te.message}`,"error")})})},D=[b,d,s,m,f],n[26]=f,n[27]=b,n[28]=m,n[29]=d,n[30]=s,n[31]=D,n[32]=V):(D=n[31],V=n[32]),x.useEffect(V,D);let P,ee;n[33]!==f||n[34]!==b||n[35]!==m||n[36]!==v||n[37]!==d||n[38]!==s?(P=()=>{if(b||!q.current||v.length===0)return;const W=v[v.length-1].raw,ye=Pf(W);if(!ye)return;if(q.current=!1,d.length===0){m.appendMessage(s,"ERROR: please paste JSON/XML payload in the input text area");return}let Te;try{Te=JSON.stringify(JSON.parse(d))}catch{m.appendMessage(s,"ERROR: payload is not valid JSON — cannot upload");return}fetch(ye,{method:"POST",headers:{"Content-Type":"application/json"},body:Te}).then(he=>{if(!he.ok)throw new Error(`HTTP ${he.status}`);f("Payload uploaded successfully","success")}).catch(he=>{m.appendMessage(s,`ERROR: upload failed — ${he.message}`),f(`Upload failed: ${he.message}`,"error")})},ee=[b,v,d,s,m,f],n[33]=f,n[34]=b,n[35]=m,n[36]=v,n[37]=d,n[38]=s,n[39]=P,n[40]=ee):(P=n[39],ee=n[40]),x.useEffect(P,ee);let re;n[41]!==f||n[42]!==m||n[43]!==d||n[44]!==N||n[45]!==s?(re=()=>{if(N==="connected"){if(d.length===0){f("Nothing to upload — paste a JSON payload first","error");return}q.current=!0,m.send(s,"upload")}},n[41]=f,n[42]=m,n[43]=d,n[44]=N,n[45]=s,n[46]=re):re=n[46];const te=re;let F;n[47]!==m||n[48]!==N||n[49]!==s?(F=W=>{N==="connected"&&m.send(s,W)},n[47]=m,n[48]=N,n[49]=s,n[50]=F):F=n[50];const me=F;let _e;n[51]!==f||n[52]!==v?(_e=()=>{navigator.clipboard.writeText(v.map(Mg).join(`
`)),f("Console copied to clipboard!","success")},n[51]=f,n[52]=v,n[53]=_e):_e=n[53];const xe=_e;let Be;n[54]!==f||n[55]!==m||n[56]!==s?(Be=()=>{m.clearMessages(s),f("Console cleared","info")},n[54]=f,n[55]=m,n[56]=s,n[57]=Be):Be=n[57];const qe=Be;let Me;n[58]!==m||n[59]!==s?(Me=W=>{m.appendMessage(s,W)},n[58]=m,n[59]=s,n[60]=Me):Me=n[60];const le=Me;let de;n[61]===Symbol.for("react.memo_cache_sentinel")?(de=W=>C({type:"SET_COMMAND",value:W}),n[61]=de):de=n[61];const fe=de;let ke;return n[62]!==le||n[63]!==qe||n[64]!==S||n[65]!==K||n[66]!==T||n[67]!==A||n[68]!==xe||n[69]!==ne||n[70]!==M||n[71]!==G||n[72]!==v||n[73]!==ce||n[74]!==me||n[75]!==te?(ke={connected:T,connecting:A,messages:v,command:S,setCommand:fe,connect:K,disconnect:ne,sendCommand:ce,handleKeyDown:M,consoleRef:Y,copyMessages:xe,clearMessages:qe,uploadPayload:te,sendRawText:me,appendMessage:le,history:G},n[62]=le,n[63]=qe,n[64]=S,n[65]=K,n[66]=T,n[67]=A,n[68]=xe,n[69]=ne,n[70]=M,n[71]=G,n[72]=v,n[73]=ce,n[74]=me,n[75]=te,n[76]=ke):ke=n[76],ke}function Mg(r){return r.raw}function Dg(r){const n=Se.c(5);let s;n[0]!==r?(s=()=>window.matchMedia(r).matches,n[0]=r,n[1]=s):s=n[1];const[u,d]=x.useState(s);let f,b;return n[2]!==r?(f=()=>{const y=window.matchMedia(r),m=g=>d(g.matches);return y.addEventListener("change",m),()=>y.removeEventListener("change",m)},b=[r],n[2]=r,n[3]=f,n[4]=b):(f=n[3],b=n[4]),x.useEffect(f,b),u}function Cf(r){return typeof r!="object"||r===null?!1:Array.isArray(r.nodes)}function nc(r,n,s){const u=n.includes(s)?s:n[0]??"graph";return typeof r=="string"&&n.includes(r)?r:u}function Og(r,n,s,u,d){const[f,b]=x.useState(null),[y,m]=oa(d,s),g=nc(y,u,s),[N,v]=x.useState(!1),T=x.useCallback(S=>{m(O=>{const R=nc(O,u,s),G=typeof S=="function"?S(R):S;return nc(G,u,s)})},[m,u,s]);x.useEffect(()=>{y!==g&&m(g)},[y,g,m]);const A=x.useRef(r);x.useEffect(()=>{A.current=r},[r]);const E=x.useRef(null);x.useEffect(()=>{if(!r)return;const S=new AbortController;return b(null),fetch(r,{signal:S.signal}).then(O=>{if(!O.ok)throw new Error(`HTTP ${O.status}`);return O.json()}).then(O=>{Cf(O)&&(b(O),T("graph"))}).catch(O=>{O.name!=="AbortError"&&n(`Graph fetch failed: ${O.message}`,"error")}),()=>{S.abort()}},[r,n]);const C=x.useCallback(()=>{var R;const S=A.current;if(!S)return;(R=E.current)==null||R.abort();const O=new AbortController;E.current=O,v(!0),fetch(S,{signal:O.signal}).then(G=>{if(!G.ok)throw new Error(`HTTP ${G.status}`);return G.json()}).then(G=>{Cf(G)&&b(G),v(!1)}).catch(G=>{G.name!=="AbortError"&&(n(`Graph refresh failed: ${G.message}`,"error"),v(!1))})},[]);return x.useEffect(()=>()=>{var S;(S=E.current)==null||S.abort()},[]),{graphData:f,setGraphData:b,rightTab:g,setRightTab:T,isRefreshing:N,refetchGraph:C}}function zg(r){const n=Se.c(22),{bus:s,pinnedGraphPath:u,setPinnedGraphPath:d,connected:f,sendRawText:b,addToast:y}=r,m=x.useRef(null),g=x.useRef(!1),N=x.useRef(u),v=x.useRef(f),T=x.useRef(b);let A,E;n[0]!==u?(A=()=>{N.current=u},E=[u],n[0]=u,n[1]=A,n[2]=E):(A=n[1],E=n[2]),x.useEffect(A,E);let C,S;n[3]!==f?(C=()=>{v.current=f},S=[f],n[3]=f,n[4]=C,n[5]=S):(C=n[4],S=n[5]),x.useEffect(C,S);let O,R;n[6]!==b?(O=()=>{T.current=b},R=[b],n[6]=b,n[7]=O,n[8]=R):(O=n[7],R=n[8]),x.useEffect(O,R);let G,L;n[9]!==f?(G=()=>{f||(g.current=!1,m.current!==null&&(clearTimeout(m.current),m.current=null))},L=[f],n[9]=f,n[10]=G,n[11]=L):(G=n[10],L=n[11]),x.useEffect(G,L);let Y,q;n[12]!==s||n[13]!==d?(q=()=>s.on("graph.link",$=>{g.current&&(g.current=!1,d($.apiPath))}),Y=[s,d],n[12]=s,n[13]=d,n[14]=Y,n[15]=q):(Y=n[14],q=n[15]),x.useEffect(q,Y);let B,J;n[16]!==y||n[17]!==s?(B=()=>s.on("graph.mutation",$=>{if(v.current){if($.mutationType==="import-graph"){m.current!==null&&(clearTimeout(m.current),m.current=null),g.current=!0,T.current("describe graph"),y("Graph imported — refreshing view…","info");return}m.current!==null&&clearTimeout(m.current),m.current=setTimeout(()=>{m.current=null,v.current&&(g.current=!0,T.current("describe graph"),y(N.current!==null?"Graph updated — refreshing…":"Graph updated — opening Graph tab…","info"))},300)}}),J=[s,y],n[16]=y,n[17]=s,n[18]=B,n[19]=J):(B=n[18],J=n[19]),x.useEffect(B,J);let I,K;n[20]===Symbol.for("react.memo_cache_sentinel")?(I=()=>()=>{m.current!==null&&clearTimeout(m.current)},K=[],n[20]=I,n[21]=K):(I=n[20],K=n[21]),x.useEffect(I,K)}const kg=`Connect two nodes together
--------------------------
1. Each connection is directional. Connect A to B is different from B to A.
2. A node must connect to one or more nodes. When a graph has orphan nodes, you cannot export the graph for deployment.

Syntax
------
\`\`\`
connect {node-A} to {node-B} with {relation}
\`\`\`
`,Rg=`Create a new node
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
`,Gg="Describe graph, node, connection or skill\n-----------------------------------------\n\nSyntax\n------\nShow the structure of the current graph model\n```\ndescribe graph\n```\n\nPrint the structure of a node\n```\ndescribe node {name}\n```\n\nConfirm if there is a connection between node-A and node-B\n```\ndescribe connection {node-A} and {node-B}\n```\n\nSkill description of a specific composable function serving the skill\n```\ndescribe skill {skill.route.name}\n```\n",Hg=`Edit a node
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
`,Ig=`Skill: Graph Math
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
`,Kg=`Import a graph model
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
`,Pg=`List nodes or connections
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
`,Fg=`Run a graph instance
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
`,n1=`Tutorial 10
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
To deploy the graph model, copy "/tmp/graph/tutorial-8.json" to your application's \`main/resources/graph\` folder.
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
`,a1=`Tutorial 11
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

Create a node to use an extension
---------------------------------
Enter the following to create an extension node. The skill is 'extension' and the extension is 'flow://my-flow'.

The input mapping sets the input parameter(s) to an extension which is also a graph model.
The output mapping sets the result from the extension to the output payload.

\`\`\`
create node extension
with type Extension
with properties
skill=graph.extension
extension=flow://my-flow
input[]=input.body.person_id -> person_id
output[]=result -> output.body
\`\`\`

*Note*: the flow://my-flow and the input parameters are placeholder examples.

Using event flow as an extension is similar to using a graph model as an extension.
The only difference is the use of the protocol prefix \`flow://\` followed by a flow-id.

Since this is an advanced topic, model answer is not provided. You will try this as an exercise.

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
`,l1=`Tutorial 2
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
`,o1=`Tutorial 3
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
`,i1=`Tutorial 4
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
`,s1=`Tutorial 5
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
`,r1=`Tutorial 6
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
`,c1=`Tutorial 7
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
`,u1=`Tutorial 8
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
`,d1=`Tutorial 9
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
To deploy the graph model, copy "/tmp/graph/tutorial-8.json" to your application's \`main/resources/graph\` folder.
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
`,p1=`Update a node
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
`,f1=`Upload mock data to current graph instance
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
`,h1=`MiniGraph
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
`,m1=Object.assign({"../../../src/main/resources/help/help connect.md":kg,"../../../src/main/resources/help/help create.md":Rg,"../../../src/main/resources/help/help data-dictionary.md":Bg,"../../../src/main/resources/help/help delete.md":Ug,"../../../src/main/resources/help/help describe.md":Gg,"../../../src/main/resources/help/help edit.md":Hg,"../../../src/main/resources/help/help execute.md":Lg,"../../../src/main/resources/help/help export.md":qg,"../../../src/main/resources/help/help graph-api-fetcher.md":Yg,"../../../src/main/resources/help/help graph-data-mapper.md":Xg,"../../../src/main/resources/help/help graph-extension.md":Jg,"../../../src/main/resources/help/help graph-island.md":Zg,"../../../src/main/resources/help/help graph-join.md":Qg,"../../../src/main/resources/help/help graph-js.md":Vg,"../../../src/main/resources/help/help graph-math.md":Ig,"../../../src/main/resources/help/help import.md":Kg,"../../../src/main/resources/help/help inspect.md":$g,"../../../src/main/resources/help/help instantiate.md":Wg,"../../../src/main/resources/help/help list.md":Pg,"../../../src/main/resources/help/help run.md":Fg,"../../../src/main/resources/help/help seen.md":e1,"../../../src/main/resources/help/help tutorial 1.md":t1,"../../../src/main/resources/help/help tutorial 10.md":n1,"../../../src/main/resources/help/help tutorial 11.md":a1,"../../../src/main/resources/help/help tutorial 2.md":l1,"../../../src/main/resources/help/help tutorial 3.md":o1,"../../../src/main/resources/help/help tutorial 4.md":i1,"../../../src/main/resources/help/help tutorial 5.md":s1,"../../../src/main/resources/help/help tutorial 6.md":r1,"../../../src/main/resources/help/help tutorial 7.md":c1,"../../../src/main/resources/help/help tutorial 8.md":u1,"../../../src/main/resources/help/help tutorial 9.md":d1,"../../../src/main/resources/help/help update.md":p1,"../../../src/main/resources/help/help upload.md":f1,"../../../src/main/resources/help/help.md":h1});function y1(r){const n=r.split("/");return(n[n.length-1]??r).replace(/\.md$/,"")}const Ff=Object.fromEntries(Object.entries(m1).map(([r,n])=>[y1(r),n]));function Oi(r){const n=r===""?"help":`help ${r}`;return Ff[n]??null}const g1=Object.keys(Ff).filter(r=>r!=="help").map(r=>r.replace(/^help\s+/,"")).sort(),eh=[{id:"overview",label:"Overview"},{id:"graph-model",label:"Graph Model"},{id:"graph-skills",label:"Graph Skills"},{id:"instance-model",label:"Instance Model"},{id:"tutorials",label:"Tutorials"}],v1=new Set(["execute","inspect","instantiate","run","seen","upload"]);function th(r){return r===""?"overview":r.startsWith("tutorial ")?"tutorials":r.startsWith("graph-")?"graph-skills":v1.has(r)?"instance-model":"graph-model"}function cc(r){return r==="overview"?[""]:g1.filter(n=>th(n)===r)}const hl=eh.flatMap(r=>cc(r.id));function nh(r){return r.replace(/^help\s*/i,"").trim().toLowerCase()}function b1(r){const n=Se.c(6),{bus:s,setHelpTopic:u,onTabSwitch:d}=r,f=x.useRef(d);let b;n[0]!==d?(b=()=>{f.current=d},n[0]=d,n[1]=b):b=n[1],x.useEffect(b);let y,m;n[2]!==s||n[3]!==u?(y=()=>s.on("command.helpOrDescribe",g=>{if(!g.commandText.trim().toLowerCase().startsWith("help"))return;const v=nh(g.commandText);Oi(v)!==null&&(u(v),f.current())}),m=[s,u],n[2]=s,n[3]=u,n[4]=y,n[5]=m):(y=n[4],m=n[5]),x.useEffect(y,m)}function _1(r){const n=Se.c(12),{ctx:s,navigate:u,addToast:d,wsPath:f}=r;let b;n[0]===Symbol.for("react.memo_cache_sentinel")?(b=On.find(S1),n[0]=b):b=n[0];const y=b,m=x.useRef(null),g=y==null?void 0:y.wsPath;let N,v;n[1]!==d||n[2]!==s||n[3]!==u?(N=()=>{if(!g||!m.current)return;if(s.getSlot(g).phase==="connected"){const{wsPath:O,json:R}=m.current;m.current=null,s.setPendingPayload(O,R),u(y.path),d("JSON loaded into JSON-Path editor ✓","success")}},v=[g,s,u,d,y],n[1]=d,n[2]=s,n[3]=u,n[4]=N,n[5]=v):(N=n[4],v=n[5]),x.useEffect(N,v);let T;n[6]!==d||n[7]!==s||n[8]!==u?(T=S=>{if(!y)return;const O=s.getSlot(y.wsPath);O.phase==="connected"?(s.setPendingPayload(y.wsPath,S),u(y.path),d("JSON loaded into JSON-Path editor ✓","success")):O.phase==="connecting"?(m.current={wsPath:y.wsPath,json:S},d("Updated pending JSON transfer — latest payload will open when connected","info")):(m.current={wsPath:y.wsPath,json:S},s.connect(y.wsPath,d),d("Connecting to JSON-Path Playground…","info"))},n[6]=d,n[7]=s,n[8]=u,n[9]=T):T=n[9];const A=T,E=y&&f!==y.wsPath?A:void 0;let C;return n[10]!==E?(C={handleSendToJsonPath:E},n[10]=E,n[11]=C):C=n[11],C}function S1(r){return r.tabs.includes("payload")&&r.supportsUpload}function x1(r){const n=Se.c(7),{bus:s,onOpenModal:u,modalOpen:d}=r,f=x.useRef(!1);let b,y;n[0]!==d?(b=()=>{d||(f.current=!1)},y=[d],n[0]=d,n[1]=b,n[2]=y):(b=n[1],y=n[2]),x.useEffect(b,y);let m,g;n[3]!==s||n[4]!==u?(m=()=>s.on("upload.invitation",N=>{f.current||(f.current=!0,u(N.uploadPath))}),g=[s,u],n[3]=s,n[4]=u,n[5]=m,n[6]=g):(m=n[5],g=n[6]),x.useEffect(m,g)}function T1(r){const n=Se.c(17),{bus:s,addToast:u}=r,[d,f]=x.useState(null),b=x.useRef(null);let y;n[0]===Symbol.for("react.memo_cache_sentinel")?(y=new Set,n[0]=y):y=n[0];const[m,g]=x.useState(y);let N;n[1]===Symbol.for("react.memo_cache_sentinel")?(N=B=>{b.current=document.activeElement,f(B)},n[1]=N):N=n[1];const v=N;let T;n[2]===Symbol.for("react.memo_cache_sentinel")?(T=()=>{f(null),setTimeout(()=>{var B;return(B=b.current)==null?void 0:B.focus()},0)},n[2]=T):T=n[2];const A=T;let E;n[3]!==u||n[4]!==d?(E=B=>{g(J=>new Set([...J,d])),f(null),setTimeout(()=>{var J;return(J=b.current)==null?void 0:J.focus()},0),u("Mock data uploaded successfully ✓","success")},n[3]=u,n[4]=d,n[5]=E):E=n[5];const C=E;let S;n[6]!==u?(S=B=>{u(`Upload failed: ${B}`,"error")},n[6]=u,n[7]=S):S=n[7];const O=S;let R;n[8]===Symbol.for("react.memo_cache_sentinel")?(R=()=>{g(new Set)},n[8]=R):R=n[8];const G=R,L=d!==null;let Y;n[9]!==s||n[10]!==L?(Y={bus:s,onOpenModal:v,modalOpen:L},n[9]=s,n[10]=L,n[11]=Y):Y=n[11],x1(Y);let q;return n[12]!==O||n[13]!==C||n[14]!==d||n[15]!==m?(q={modalUploadPath:d,successfulUploadPaths:m,handleOpenUploadModal:v,handleCloseUploadModal:A,handleUploadSuccess:C,handleUploadError:O,resetSuccessfulPaths:G},n[12]=O,n[13]=C,n[14]=d,n[15]=m,n[16]=q):q=n[16],q}function w1(r){const n=Se.c(14),{bus:s,connected:u,appendMessage:d,addToast:f}=r,b=x.useRef(null),y=x.useRef(!1),m=x.useRef(d);let g,N;n[0]!==d?(g=()=>{m.current=d},N=[d],n[0]=d,n[1]=g,n[2]=N):(g=n[1],N=n[2]),x.useEffect(g,N);const v=x.useRef(f);let T,A;n[3]!==f?(T=()=>{v.current=f},A=[f],n[3]=f,n[4]=T,n[5]=A):(T=n[4],A=n[5]),x.useEffect(T,A);let E,C;n[6]!==u?(E=()=>{var L;u||((L=b.current)==null||L.abort(),b.current=null,y.current=!1)},C=[u],n[6]=u,n[7]=E,n[8]=C):(E=n[7],C=n[8]),x.useEffect(E,C);let S,O;n[9]===Symbol.for("react.memo_cache_sentinel")?(S=()=>()=>{var L;(L=b.current)==null||L.abort()},O=[],n[9]=S,n[10]=O):(S=n[9],O=n[10]),x.useEffect(S,O);let R,G;n[11]!==s?(G=()=>s.on("payload.large",L=>{var I;if(y.current)return;const{apiPath:Y,byteSize:q}=L;(I=b.current)==null||I.abort();const B=new AbortController;b.current=B;const J=(q/1048576).toFixed(2);v.current(`Fetching large payload (${J} MB)…`,"info"),y.current=!0,fetch(Y,{signal:B.signal}).then(E1).then(K=>{if(!K.trim())throw new Error("empty response body");let $=K;try{$=JSON.stringify(JSON.parse(K),null,2)}catch{}m.current($),y.current=!1,b.current=null}).catch(K=>{K.name!=="AbortError"&&(y.current=!1,b.current=null,m.current(`ERROR: payload fetch failed — ${K.message}`),v.current(`Payload fetch failed: ${K.message}`,"error"))})}),R=[s],n[11]=s,n[12]=R,n[13]=G):(R=n[12],G=n[13]),x.useEffect(G,R)}function E1(r){if(!r.ok)throw new Error(`HTTP ${r.status}`);return r.text()}function N1(r){const n=Se.c(14);let s;n[0]===Symbol.for("react.memo_cache_sentinel")?(s={},n[0]=s):s=n[0];const[u,d]=oa(r,s);let f;n[1]!==d?(f=E=>{d(C=>({...C,[E]:{name:E,savedAt:new Date().toISOString()}}))},n[1]=d,n[2]=f):f=n[2];const b=f;let y;n[3]!==d?(y=E=>{d(C=>{const S={...C};return delete S[E],S})},n[3]=d,n[4]=y):y=n[4];const m=y;let g;n[5]!==u?(g=E=>Object.prototype.hasOwnProperty.call(u,E),n[5]=u,n[6]=g):g=n[6];const N=g;let v;n[7]!==u?(v=Object.values(u).sort(A1),n[7]=u,n[8]=v):v=n[8];const T=v;let A;return n[9]!==m||n[10]!==N||n[11]!==b||n[12]!==T?(A={savedGraphs:T,saveGraph:b,deleteGraph:m,hasGraph:N},n[9]=m,n[10]=N,n[11]=b,n[12]=T,n[13]=A):A=n[13],A}function A1(r,n){return new Date(n.savedAt).getTime()-new Date(r.savedAt).getTime()}function C1(r,n){const s=Se.c(11),[u,d]=oa(r,1),f=x.useRef(!1),[b,y]=x.useState(null),[m,g]=x.useState(null);let N,v;s[0]!==n?(N=()=>n.on("command.importGraph",R=>{y(R.graphName),g(null)}),v=[n],s[0]=n,s[1]=N,s[2]=v):(N=s[1],v=s[2]),x.useEffect(N,v);let T;s[3]!==u?(T=R=>{g(R),R===`untitled-${u}`&&(f.current=!0)},s[3]=u,s[4]=T):T=s[4];const A=T;let E;s[5]!==d?(E=()=>{y(null),g(null),f.current&&d(j1),f.current=!1},s[5]=d,s[6]=E):E=s[6];const C=E,S=m??b??`untitled-${u}`;let O;return s[7]!==S||s[8]!==C||s[9]!==A?(O={defaultName:S,setLastSavedName:A,resetName:C},s[7]=S,s[8]=C,s[9]=A,s[10]=O):O=s[10],O}function j1(r){return r+1}function M1(r){const n=Se.c(27),{bus:s,connected:u,sendRawText:d,saveGraph:f,setLastSavedName:b,addToast:y}=r,m=x.useRef(null);let g;n[0]!==y||n[1]!==u||n[2]!==d?(g=q=>{if(!u){y("Save failed: connection required to export graph","error");return}const B=setTimeout(()=>{m.current!==null&&(m.current=null,y("Save failed: export confirmation timed out","error"))},1e4);m.current={graphName:q,timeoutId:B},d(`export graph as ${q}`)},n[0]=y,n[1]=u,n[2]=d,n[3]=g):g=n[3];const N=g;let v,T;n[4]!==y||n[5]!==s||n[6]!==f||n[7]!==b?(v=()=>s.on("graph.exported",q=>{if(m.current===null||q.graphName!==m.current.graphName)return;clearTimeout(m.current.timeoutId);const B=m.current.graphName;m.current=null,f(B),b(B),y(`Graph saved as "${B}"`,"success")}),T=[s,f,b,y],n[4]=y,n[5]=s,n[6]=f,n[7]=b,n[8]=v,n[9]=T):(v=n[8],T=n[9]),x.useEffect(v,T);let A,E;n[10]!==y||n[11]!==s?(A=()=>s.on("graph.export.failed",q=>{m.current!==null&&(clearTimeout(m.current.timeoutId),m.current=null,q.reason==="invalid-name"?y("Save failed: invalid filename (a–z, A–Z, 0–9, hyphen only)","error"):y("Save failed: root node name does not match existing graph","error"))}),E=[s,y],n[10]=y,n[11]=s,n[12]=A,n[13]=E):(A=n[12],E=n[13]),x.useEffect(A,E);let C,S;n[14]!==y||n[15]!==u?(C=()=>{!u&&m.current!==null&&(clearTimeout(m.current.timeoutId),m.current=null,y("Save failed: connection closed before export confirmation","error"))},S=[u,y],n[14]=y,n[15]=u,n[16]=C,n[17]=S):(C=n[16],S=n[17]),x.useEffect(C,S);let O,R;n[18]===Symbol.for("react.memo_cache_sentinel")?(O=()=>()=>{m.current!==null&&clearTimeout(m.current.timeoutId)},R=[],n[18]=O,n[19]=R):(O=n[18],R=n[19]),x.useEffect(O,R);let G;n[20]!==y||n[21]!==u||n[22]!==d?(G=q=>{u&&(d(`import graph from ${q}`),y(`Importing graph "${q}"…`,"info"))},n[20]=y,n[21]=u,n[22]=d,n[23]=G):G=n[23];const L=G;let Y;return n[24]!==L||n[25]!==N?(Y={handleSaveGraph:N,handleLoadGraph:L},n[24]=L,n[25]=N,n[26]=Y):Y=n[26],Y}const ac=new Map;function D1(r){const n=Se.c(7);let s;n[0]!==r?(s=()=>ac.get(r)??null,n[0]=r,n[1]=s):s=n[1];const[u,d]=x.useState(s);let f;n[2]!==r?(f=m=>{d(m),m===null?ac.delete(r):ac.set(r,m)},n[2]=r,n[3]=f):f=n[3];const b=f;let y;return n[4]!==u||n[5]!==b?(y=[u,b],n[4]=u,n[5]=b,n[6]=y):y=n[6],y}function jf(r){if(r==null)return"";const n=typeof r=="string"?r:JSON.stringify(r);return n.includes("'''")&&console.warn(`[commandBuilder] Property value contains "'''" which cannot be escaped in the backend grammar. The value may be truncated on paste.`),n.includes(`
`)?`'''
${n}
'''`:n}function O1(r,n){const s=[`${r} node ${n.alias}`];n.types.length>0&&s.push(`with type ${n.types[0]}`);const u=Object.entries(n.properties).filter(([,d])=>d!=null);if(u.length>0){s.push("with properties");for(const[d,f]of u)if(Array.isArray(f))for(const b of f)s.push(`${d}[]=${jf(b)}`);else s.push(`${d}[]=${jf(f)}`)}return s.join(`
`)}const z1="_toastContainer_1ot2i_1",k1="_toast_1ot2i_1",R1="_slideIn_1ot2i_1",B1="_success_1ot2i_36",U1="_error_1ot2i_40",G1="_info_1ot2i_44",H1="_toastIcon_1ot2i_48",L1="_toastMessage_1ot2i_53",ro={toastContainer:z1,toast:k1,slideIn:R1,success:B1,error:U1,info:G1,toastIcon:H1,toastMessage:L1},q1=r=>{const n=Se.c(7),{toasts:s,onRemove:u}=r;if(s.length===0)return null;let d;if(n[0]!==u||n[1]!==s){let b;n[3]!==u?(b=y=>h.jsxs("div",{className:`${ro.toast} ${ro[y.type]}`,onClick:()=>u(y.id),children:[h.jsxs("span",{className:ro.toastIcon,children:[y.type==="success"&&"✅",y.type==="error"&&"❌",y.type==="info"&&"ℹ️"]}),h.jsx("span",{className:ro.toastMessage,children:y.message})]},y.id),n[3]=u,n[4]=b):b=n[4],d=s.map(b),n[0]=u,n[1]=s,n[2]=d}else d=n[2];let f;return n[5]!==d?(f=h.jsx("div",{className:ro.toastContainer,children:d}),n[5]=d,n[6]=f):f=n[6],f},Y1="_container_1pt3n_3",X1="_trigger_1pt3n_7",J1="_chevron_1pt3n_37",Z1="_chevronOpen_1pt3n_43",Q1="_dot_1pt3n_49",V1="_dotIdle_1pt3n_56",I1="_dotConnecting_1pt3n_57",K1="_dotConnected_1pt3n_58",$1="_dotPartial_1pt3n_59",W1="_dropdown_1pt3n_65",un={container:Y1,trigger:X1,chevron:J1,chevronOpen:Z1,dot:Q1,dotIdle:V1,dotConnecting:I1,dotConnected:K1,dotPartial:$1,dropdown:W1};function uc(r){const n=Se.c(23),{label:s,dotStatus:u,children:d}=r,[f,b]=x.useState(!1),y=x.useRef(null);let m,g;n[0]!==f?(m=()=>{if(!f)return;const Y=q=>{y.current&&!y.current.contains(q.target)&&b(!1)};return document.addEventListener("mousedown",Y),()=>document.removeEventListener("mousedown",Y)},g=[f],n[0]=f,n[1]=m,n[2]=g):(m=n[1],g=n[2]),x.useEffect(m,g);let N;n[3]===Symbol.for("react.memo_cache_sentinel")?(N=Y=>{var q,B;Y.key==="Escape"&&(b(!1),(B=(q=y.current)==null?void 0:q.querySelector("button[aria-haspopup]"))==null||B.focus())},n[3]=N):N=n[3];const v=N,T=u==="connected"?un.dotConnected:u==="connecting"?un.dotConnecting:u==="partial"?un.dotPartial:u==="idle"?un.dotIdle:void 0;let A;n[4]===Symbol.for("react.memo_cache_sentinel")?(A=()=>b(P1),n[4]=A):A=n[4];let E;n[5]!==T||n[6]!==u?(E=u!==void 0&&h.jsx("span",{className:`${un.dot} ${T??""}`,"aria-hidden":"true"}),n[5]=T,n[6]=u,n[7]=E):E=n[7];let C;n[8]!==s?(C=h.jsx("span",{children:s}),n[8]=s,n[9]=C):C=n[9];const S=`${un.chevron} ${f?un.chevronOpen:""}`;let O;n[10]!==S?(O=h.jsx("span",{className:S,"aria-hidden":"true",children:"▾"}),n[10]=S,n[11]=O):O=n[11];let R;n[12]!==f||n[13]!==E||n[14]!==C||n[15]!==O?(R=h.jsxs("button",{className:un.trigger,onClick:A,"aria-haspopup":"true","aria-expanded":f,children:[E,C,O]}),n[12]=f,n[13]=E,n[14]=C,n[15]=O,n[16]=R):R=n[16];let G;n[17]!==d||n[18]!==f?(G=f&&h.jsx("div",{className:un.dropdown,role:"menu",children:d}),n[17]=d,n[18]=f,n[19]=G):G=n[19];let L;return n[20]!==G||n[21]!==R?(L=h.jsxs("div",{className:un.container,ref:y,onKeyDown:v,children:[R,G]}),n[20]=G,n[21]=R,n[22]=L):L=n[22],L}function P1(r){return!r}const F1="_nav_8zfdi_3",e0="_menuList_8zfdi_11",t0="_menuItem_8zfdi_19",n0="_toolRow_8zfdi_51",a0="_toolLink_8zfdi_62",l0="_toolLinkActive_8zfdi_83",o0="_toolDot_8zfdi_90",i0="_toolDotIdle_8zfdi_97",s0="_toolDotConnecting_8zfdi_98",r0="_toolDotConnected_8zfdi_99",c0="_connectAllRow_8zfdi_103",u0="_connectAllBtn_8zfdi_109",d0="_connectAllBtnStop_8zfdi_133",p0="_toolConnectBtn_8zfdi_145",f0="_toolConnectBtnStop_8zfdi_171",h0="_externalIcon_8zfdi_183",ht={nav:F1,menuList:e0,menuItem:t0,toolRow:n0,toolLink:a0,toolLinkActive:l0,toolDot:o0,toolDotIdle:i0,toolDotConnecting:s0,toolDotConnected:r0,connectAllRow:c0,connectAllBtn:u0,connectAllBtnStop:d0,toolConnectBtn:p0,toolConnectBtnStop:f0,externalIcon:h0};function m0(r){return r.every(n=>n==="connected")?"connected":r.every(n=>n==="idle")?"idle":r.some(n=>n==="connecting")?"connecting":"partial"}function y0(r){return r==="connected"?"connected":r==="connecting"?"connecting":"idle"}const g0=[{href:"/info",label:"Info"},{href:"/info/lib",label:"Libraries"},{href:"/info/routes",label:"Services"},{href:"/health",label:"Health"},{href:"/env",label:"Environment"},{href:"http://localhost:8085/api/ws/json",label:"Legacy JSON"},{href:"http://localhost:8085/api/ws/graph",label:"Legacy Graph"}];function v0(r){const n=Se.c(27),{addToast:s}=r,u=bc();let d,f,b;if(n[0]!==u){const q=On.map(B=>u.getSlot(B.wsPath).phase);b=m0(q),d=q.every(x0),f=q.some(S0),n[0]=u,n[1]=d,n[2]=f,n[3]=b}else d=n[1],f=n[2],b=n[3];const y=f;let m;n[4]!==s||n[5]!==u?(m=function(){On.forEach(B=>{u.getSlot(B.wsPath).phase==="idle"&&u.connect(B.wsPath,s)})},n[4]=s,n[5]=u,n[6]=m):m=n[6];const g=m;let N;n[7]!==u?(N=function(){On.forEach(B=>{const{phase:J}=u.getSlot(B.wsPath);(J==="connected"||J==="connecting")&&u.disconnect(B.wsPath)})},n[7]=u,n[8]=N):N=n[8];const v=N,T=`${ht.connectAllBtn} ${d?ht.connectAllBtnStop:""}`,A=d?v:g,E=y?"Connecting…":d?"Disconnect all WebSockets":"Connect all WebSockets",C=y?"Connecting…":d?"Disconnect All":"Connect All";let S;n[9]!==y||n[10]!==T||n[11]!==A||n[12]!==E||n[13]!==C?(S=h.jsx("div",{className:ht.connectAllRow,children:h.jsx("button",{className:T,onClick:A,disabled:y,"aria-label":E,children:C})}),n[9]=y,n[10]=T,n[11]=A,n[12]=E,n[13]=C,n[14]=S):S=n[14];let O;n[15]!==s||n[16]!==u?(O=On.map(q=>{const{phase:B}=u.getSlot(q.wsPath),J=y0(B),I=B==="connected",K=B==="connecting",$=J==="connected"?ht.toolDotConnected:J==="connecting"?ht.toolDotConnecting:ht.toolDotIdle;return h.jsxs("li",{role:"none",className:ht.toolRow,children:[h.jsxs(Ny,{to:q.path,role:"menuitem",className:_0,children:[h.jsx("span",{className:`${ht.toolDot} ${$}`,"aria-hidden":"true"}),h.jsx("span",{className:ht.toolLabel,children:q.label})]}),h.jsx("button",{className:`${ht.toolConnectBtn} ${I?ht.toolConnectBtnStop:""}`,onClick:()=>I||K?u.disconnect(q.wsPath):u.connect(q.wsPath,s),disabled:K,"aria-label":K?"Connecting…":I?`Disconnect ${q.label}`:`Connect ${q.label}`,title:K?"Connecting…":Kf(q.wsPath),children:K?"…":I?"Stop":"Start"})]},q.path)}),n[15]=s,n[16]=u,n[17]=O):O=n[17];let R;n[18]!==O?(R=h.jsx("ul",{className:ht.menuList,role:"none",children:O}),n[18]=O,n[19]=R):R=n[19];let G;n[20]!==R||n[21]!==S||n[22]!==b?(G=h.jsxs(uc,{label:"Tools",dotStatus:b,children:[S,R]}),n[20]=R,n[21]=S,n[22]=b,n[23]=G):G=n[23];let L;n[24]===Symbol.for("react.memo_cache_sentinel")?(L=h.jsx(uc,{label:"Quick Links",children:h.jsx("ul",{className:ht.menuList,role:"none",children:g0.map(b0)})}),n[24]=L):L=n[24];let Y;return n[25]!==G?(Y=h.jsxs("nav",{className:ht.nav,"aria-label":"Main navigation",children:[G,L]}),n[25]=G,n[26]=Y):Y=n[26],Y}function b0(r){return h.jsx("li",{role:"none",children:h.jsxs("a",{href:r.href,role:"menuitem",className:ht.menuItem,target:"_blank",rel:"noopener noreferrer",children:[r.label,h.jsx("span",{className:ht.externalIcon,"aria-hidden":"true",children:"↗"})]})},r.href)}function _0(r){const{isActive:n}=r;return`${ht.toolLink} ${n?ht.toolLinkActive:""}`}function S0(r){return r==="connecting"}function x0(r){return r==="connected"}const T0="_saveBtn_1xd2l_3",w0="_saveForm_1xd2l_33",E0="_saveInput_1xd2l_39",N0="_saveInputWarn_1xd2l_55",A0="_saveWarnLabel_1xd2l_59",C0="_saveActionBtn_1xd2l_65",Ca={saveBtn:T0,saveForm:w0,saveInput:E0,saveInputWarn:N0,saveWarnLabel:A0,saveActionBtn:C0};function j0(r){const n=Se.c(33),{disabled:s,defaultName:u,onSave:d,nameExists:f,connected:b}=r,y=b===void 0?!1:b,[m,g]=x.useState(!1),[N,v]=x.useState(""),T=x.useRef(null);let A;n[0]!==u?(A=()=>{v(u),g(!0)},n[0]=u,n[1]=A):A=n[1];const E=A;let C;n[2]===Symbol.for("react.memo_cache_sentinel")?(C=()=>{g(!1),v("")},n[2]=C):C=n[2];const S=C;let O;n[3]!==d||n[4]!==N?(O=()=>{const K=N.trim();K&&(d(K),g(!1),v(""))},n[3]=d,n[4]=N,n[5]=O):O=n[5];const R=O;let G;n[6]!==R?(G=K=>{K.key==="Enter"&&(K.preventDefault(),R()),K.key==="Escape"&&(K.preventDefault(),S())},n[6]=R,n[7]=G):G=n[7];const L=G;let Y,q;if(n[8]!==m?(Y=()=>{var K;m&&((K=T.current)==null||K.focus())},q=[m],n[8]=m,n[9]=Y,n[10]=q):(Y=n[9],q=n[10]),x.useEffect(Y,q),m){const K=`${Ca.saveInput}${f!=null&&f(N.trim())?` ${Ca.saveInputWarn}`:""}`;let $;n[11]===Symbol.for("react.memo_cache_sentinel")?($=P=>v(P.target.value),n[11]=$):$=n[11];let ne;n[12]!==L||n[13]!==N||n[14]!==K?(ne=h.jsx("input",{ref:T,className:K,type:"text",value:N,onChange:$,onKeyDown:L,placeholder:"Enter a name…","aria-label":"Graph save name",maxLength:80}),n[12]=L,n[13]=N,n[14]=K,n[15]=ne):ne=n[15];let ue;n[16]!==f||n[17]!==N?(ue=(f==null?void 0:f(N.trim()))&&h.jsx("span",{className:Ca.saveWarnLabel,role:"status",children:"Overwrite?"}),n[16]=f,n[17]=N,n[18]=ue):ue=n[18];let ce;n[19]!==N?(ce=N.trim(),n[19]=N,n[20]=ce):ce=n[20];const ie=!ce;let M;n[21]!==R||n[22]!==ie?(M=h.jsx("button",{className:Ca.saveActionBtn,onClick:R,disabled:ie,"aria-label":"Confirm save",children:"✅"}),n[21]=R,n[22]=ie,n[23]=M):M=n[23];let D;n[24]===Symbol.for("react.memo_cache_sentinel")?(D=h.jsx("button",{className:Ca.saveActionBtn,onClick:S,"aria-label":"Cancel save",children:"❌"}),n[24]=D):D=n[24];let V;return n[25]!==ne||n[26]!==ue||n[27]!==M?(V=h.jsxs("div",{className:Ca.saveForm,children:[ne,ue,M,D]}),n[25]=ne,n[26]=ue,n[27]=M,n[28]=V):V=n[28],V}const B=s||!y,J=s?"No graph loaded":y?"Export graph snapshot to server and save bookmark":"Connect first to save";let I;return n[29]!==E||n[30]!==B||n[31]!==J?(I=h.jsx("button",{className:Ca.saveBtn,onClick:E,disabled:B,title:J,"aria-label":"Save graph snapshot",children:"💾 Save Graph"}),n[29]=E,n[30]=B,n[31]=J,n[32]=I):I=n[32],I}const M0="_empty_d1tzv_3",D0="_hint_d1tzv_12",O0="_list_d1tzv_21",z0="_row_d1tzv_31",k0="_rowInfo_d1tzv_50",R0="_rowName_d1tzv_58",B0="_rowMeta_d1tzv_67",U0="_rowActions_d1tzv_78",G0="_loadBtn_d1tzv_84",H0="_deleteBtn_d1tzv_85",dn={empty:M0,hint:D0,list:O0,row:z0,rowInfo:k0,rowName:R0,rowMeta:B0,rowActions:U0,loadBtn:G0,deleteBtn:H0};function L0(r){const n=Se.c(8),{savedGraphs:s,onLoad:u,onDelete:d,connected:f}=r,b=s.length>0?`Load Graph (${s.length})`:"Load Graph";let y;n[0]!==f||n[1]!==d||n[2]!==u||n[3]!==s?(y=s.length===0?h.jsx("p",{className:dn.empty,children:"No saved graphs yet."}):h.jsxs(h.Fragment,{children:[!f&&h.jsx("p",{className:dn.hint,children:"Connect to load a graph"}),h.jsx("ul",{className:dn.list,role:"list",children:s.map(g=>h.jsxs("li",{className:dn.row,children:[h.jsxs("div",{className:dn.rowInfo,children:[h.jsx("span",{className:dn.rowName,title:g.name,children:g.name}),h.jsx("span",{className:dn.rowMeta,children:new Date(g.savedAt).toLocaleString()})]}),h.jsxs("div",{className:dn.rowActions,children:[h.jsx("button",{className:dn.loadBtn,onClick:()=>u(g.name),disabled:!f,title:f?`Run: import graph from ${g.name}`:"Connect to the playground first","aria-label":`Load graph ${g.name}`,children:"Load"}),h.jsx("button",{className:dn.deleteBtn,onClick:()=>d(g.name),title:`Remove "${g.name}" from local storage`,"aria-label":`Delete saved graph ${g.name}`,children:"Delete"})]})]},g.name))})]}),n[0]=f,n[1]=d,n[2]=u,n[3]=s,n[4]=y):y=n[4];let m;return n[5]!==b||n[6]!==y?(m=h.jsx(uc,{label:b,children:y}),n[5]=b,n[6]=y,n[7]=m):m=n[7],m}const q0="_payloadRoot_jsab4_2",Y0="_labelRow_jsab4_10",X0="_label_jsab4_10",J0="_payloadControls_jsab4_26",Z0="_charCounter_jsab4_32",Q0="_typeIndicator_jsab4_38",V0="_validationIcon_jsab4_49",I0="_formatButton_jsab4_53",K0="_uploadButton_jsab4_67",$0="_textarea_jsab4_82",W0="_textareaError_jsab4_107",P0="_errorMessage_jsab4_109",F0="_sampleButtonsRow_jsab4_117",ev="_sampleButtons_jsab4_117",tv="_sampleLabel_jsab4_130",nv="_sampleGroup_jsab4_136",av="_sampleGroupLabel_jsab4_143",lv="_sampleButton_jsab4_117",at={payloadRoot:q0,labelRow:Y0,label:X0,payloadControls:J0,charCounter:Z0,typeIndicator:Q0,validationIcon:V0,formatButton:I0,uploadButton:K0,textarea:$0,textareaError:W0,errorMessage:P0,sampleButtonsRow:F0,sampleButtons:ev,sampleLabel:tv,sampleGroup:nv,sampleGroupLabel:av,sampleButton:lv};function ov(r){const n=Se.c(21),{onLoad:s}=r;let u,d,f,b,y,m;if(n[0]!==s){const v=Object.keys(ji).filter(rv),T=Object.keys(ji).filter(sv),A=iv;b=at.sampleButtons,n[7]===Symbol.for("react.memo_cache_sentinel")?(y=h.jsx("span",{className:at.sampleLabel,children:"Quick load:"}),n[7]=y):y=n[7];let E;n[8]===Symbol.for("react.memo_cache_sentinel")?(E=h.jsx("span",{className:at.sampleGroupLabel,children:"JSON:"}),n[8]=E):E=n[8];const C=v.map(S=>h.jsx("button",{className:at.sampleButton,onClick:()=>s(ji[S]),children:A(S)},S));n[9]!==C?(m=h.jsxs("div",{className:at.sampleGroup,children:[E,C]}),n[9]=C,n[10]=m):m=n[10],u=at.sampleGroup,n[11]===Symbol.for("react.memo_cache_sentinel")?(d=h.jsx("span",{className:at.sampleGroupLabel,children:"XML:"}),n[11]=d):d=n[11],f=T.map(S=>h.jsx("button",{className:at.sampleButton,onClick:()=>s(ji[S]),children:A(S)},S)),n[0]=s,n[1]=u,n[2]=d,n[3]=f,n[4]=b,n[5]=y,n[6]=m}else u=n[1],d=n[2],f=n[3],b=n[4],y=n[5],m=n[6];let g;n[12]!==u||n[13]!==d||n[14]!==f?(g=h.jsxs("div",{className:u,children:[d,f]}),n[12]=u,n[13]=d,n[14]=f,n[15]=g):g=n[15];let N;return n[16]!==b||n[17]!==y||n[18]!==m||n[19]!==g?(N=h.jsxs("div",{className:b,children:[y,m,g]}),n[16]=b,n[17]=y,n[18]=m,n[19]=g,n[20]=N):N=n[20],N}function iv(r){return r.replace(/^(json|xml)_/,"").replace(/_/g," ")}function sv(r){return r.startsWith("xml_")}function rv(r){return r.startsWith("json_")}function cv(r){const n=Se.c(40),{payload:s,onChange:u,validation:d,onFormat:f,onUpload:b}=r;let y;n[0]===Symbol.for("react.memo_cache_sentinel")?(y=h.jsx("label",{htmlFor:"payload",className:at.label,children:"JSON/XML Payload"}),n[0]=y):y=n[0];let m;n[1]!==s.length?(m=h.jsxs("span",{className:at.charCounter,children:["size: ",s.length]}),n[1]=s.length,n[2]=m):m=n[2];let g;n[3]!==s||n[4]!==d.type?(g=s&&d.type&&h.jsx("span",{className:at.typeIndicator,children:d.type.toUpperCase()}),n[3]=s,n[4]=d.type,n[5]=g):g=n[5];let N;n[6]!==s||n[7]!==d.valid?(N=s&&h.jsx("span",{className:at.validationIcon,children:d.valid?"✅":"❌"}),n[6]=s,n[7]=d.valid,n[8]=N):N=n[8];const v=!s||d.type!=="json",T=d.type==="xml"?"Format only available for JSON":"Format JSON";let A;n[9]!==f||n[10]!==v||n[11]!==T?(A=h.jsx("button",{className:at.formatButton,onClick:f,disabled:v,title:T,children:"Format"}),n[9]=f,n[10]=v,n[11]=T,n[12]=A):A=n[12];let E;n[13]!==b||n[14]!==s||n[15]!==d.type||n[16]!==d.valid?(E=b!==void 0&&h.jsx("button",{className:at.uploadButton,onClick:b,disabled:!s||!d.valid||d.type!=="json",title:"Upload JSON payload to current session via REST",children:"Upload"}),n[13]=b,n[14]=s,n[15]=d.type,n[16]=d.valid,n[17]=E):E=n[17];let C;n[18]!==m||n[19]!==g||n[20]!==N||n[21]!==A||n[22]!==E?(C=h.jsxs("div",{className:at.labelRow,children:[y,h.jsxs("div",{className:at.payloadControls,children:[m,g,N,A,E]})]}),n[18]=m,n[19]=g,n[20]=N,n[21]=A,n[22]=E,n[23]=C):C=n[23];const S=`${at.textarea} ${d.valid?"":at.textareaError}`;let O;n[24]!==u?(O=q=>u(q.target.value),n[24]=u,n[25]=O):O=n[25];let R;n[26]!==s||n[27]!==S||n[28]!==O?(R=h.jsx("textarea",{id:"payload",className:S,placeholder:"Paste your JSON/XML payload here",value:s,onChange:O}),n[26]=s,n[27]=S,n[28]=O,n[29]=R):R=n[29];let G;n[30]!==d.error||n[31]!==d.valid?(G=!d.valid&&h.jsx("div",{className:at.errorMessage,children:d.error}),n[30]=d.error,n[31]=d.valid,n[32]=G):G=n[32];let L;n[33]!==u?(L=h.jsx("div",{className:at.sampleButtonsRow,children:h.jsx(ov,{onLoad:u})}),n[33]=u,n[34]=L):L=n[34];let Y;return n[35]!==R||n[36]!==G||n[37]!==L||n[38]!==C?(Y=h.jsxs("div",{className:at.payloadRoot,children:[C,R,G,L]}),n[35]=R,n[36]=G,n[37]=L,n[38]=C,n[39]=Y):Y=n[39],Y}const uv="_content_7m22c_8",dv="_header_7m22c_22",pv="_icon_7m22c_42",fv="_alias_7m22c_47",hv="_badge_7m22c_53",mv="_body_7m22c_65",yv="_row_7m22c_70",gv="_label_7m22c_83",vv="_value_7m22c_89",Dn={content:uv,header:dv,icon:pv,alias:fv,badge:hv,body:mv,row:yv,label:gv,value:vv},bv={Root:{icon:"🚀",label:"Root"},End:{icon:"🏁",label:"End"},Fetcher:{icon:"🌐",label:"Fetcher"},mapper:{icon:"🗺️",label:"Mapper"},Math:{icon:"🔢",label:"Math"},JavaScript:{icon:"📜",label:"JavaScript"},Provider:{icon:"🔌",label:"Provider"},Dictionary:{icon:"📖",label:"Dictionary"},Join:{icon:"🔀",label:"Join"},Extension:{icon:"🧩",label:"Extension"},Island:{icon:"🏝️",label:"Island"},Decision:{icon:"❓",label:"Decision"}};function _v(r){return bv[r]??{icon:"📦",label:r}}function Mf(r){const n=Se.c(7),{label:s,value:u}=r;let d;n[0]!==s?(d=h.jsx("span",{className:Dn.label,children:s}),n[0]=s,n[1]=d):d=n[1];let f;n[2]!==u?(f=h.jsx("span",{className:Dn.value,title:u,children:u}),n[2]=u,n[3]=f):f=n[3];let b;return n[4]!==d||n[5]!==f?(b=h.jsxs("div",{className:Dn.row,children:[d,f]}),n[4]=d,n[5]=f,n[6]=b):b=n[6],b}function Sv(r){const n=Se.c(3),{properties:s}=r;let u,d;if(n[0]!==s){d=Symbol.for("react.early_return_sentinel");e:{const f=Object.entries(s).filter(Tv);if(f.length===0){d=null;break e}u=h.jsx(h.Fragment,{children:f.map(xv)})}n[0]=s,n[1]=u,n[2]=d}else u=n[1],d=n[2];return d!==Symbol.for("react.early_return_sentinel")?d:u}function xv(r){const[n,s]=r;if(Array.isArray(s))return s.map((d,f)=>{const b=typeof d=="string"?d:JSON.stringify(d);return h.jsx(Mf,{label:f===0?n:"",value:b},`${n}-${f}`)});const u=typeof s=="string"?s:JSON.stringify(s);return h.jsx(Mf,{label:n,value:u},n)}function Tv(r){const[,n]=r;return n!=null}function qt(r){const n=Se.c(28),{data:s,isConnectable:u,selected:d}=r;let f;n[0]!==s.nodeType?(f=_v(s.nodeType),n[0]=s.nodeType,n[1]=f):f=n[1];const b=f;let y;n[2]!==d?(y=h.jsx(Dy,{minWidth:180,minHeight:60,isVisible:d}),n[2]=d,n[3]=y):y=n[3];let m;n[4]!==u?(m=h.jsx(bf,{type:"target",position:_f.Left,isConnectable:u}),n[4]=u,n[5]=m):m=n[5];let g;n[6]!==b.icon?(g=h.jsx("span",{className:Dn.icon,children:b.icon}),n[6]=b.icon,n[7]=g):g=n[7];let N;n[8]!==s.alias?(N=h.jsx("span",{className:Dn.alias,children:s.alias}),n[8]=s.alias,n[9]=N):N=n[9];let v;n[10]!==b.label?(v=h.jsx("span",{className:Dn.badge,children:b.label}),n[10]=b.label,n[11]=v):v=n[11];let T;n[12]!==g||n[13]!==N||n[14]!==v?(T=h.jsxs("div",{className:Dn.header,children:[g,N,v]}),n[12]=g,n[13]=N,n[14]=v,n[15]=T):T=n[15];let A;n[16]!==s.properties?(A=h.jsx("div",{className:Dn.body,children:h.jsx(Sv,{properties:s.properties})}),n[16]=s.properties,n[17]=A):A=n[17];let E;n[18]!==T||n[19]!==A?(E=h.jsxs("div",{className:Dn.content,children:[T,A]}),n[18]=T,n[19]=A,n[20]=E):E=n[20];let C;n[21]!==u?(C=h.jsx(bf,{type:"source",position:_f.Right,isConnectable:u}),n[21]=u,n[22]=C):C=n[22];let S;return n[23]!==C||n[24]!==y||n[25]!==m||n[26]!==E?(S=h.jsxs(h.Fragment,{children:[y,m,E,C]}),n[23]=C,n[24]=y,n[25]=m,n[26]=E,n[27]=S):S=n[27],S}const wv={Root:qt,End:qt,Fetcher:qt,mapper:qt,Math:qt,JavaScript:qt,Provider:qt,Dictionary:qt,Join:qt,Extension:qt,Island:qt,Decision:qt,default:qt},Ev="_graphWrapper_1tscm_15",Nv="_empty_1tscm_22",Av="_emptyIcon_1tscm_35",Cv="_refreshingOverlay_1tscm_69",jv="_refreshingSpinner_1tscm_85",Mv="_contextMenu_1tscm_100",Dv="_contextMenuItem_1tscm_110",an={graphWrapper:Ev,empty:Nv,emptyIcon:Av,refreshingOverlay:Cv,refreshingSpinner:jv,contextMenu:Mv,contextMenuItem:Dv};class Ov extends x.Component{constructor(){super(...arguments),this.state={caughtError:null}}static getDerivedStateFromError(n){return{caughtError:n instanceof Error?n.message:String(n)}}componentDidCatch(n,s){var d,f;const u=n instanceof Error?n.message:String(n);console.error("[GraphView] Render error:",u,s.componentStack),(f=(d=this.props).onRenderError)==null||f.call(d,`Graph render failed: ${u}`)}render(){return this.state.caughtError?h.jsxs("div",{className:an.empty,children:[h.jsx("span",{className:an.emptyIcon,children:"⚠️"}),h.jsx("span",{children:"Graph could not be rendered."}),h.jsx("span",{children:this.state.caughtError})]}):this.props.children}}const ah=240,dc=100,Df=60,zv=120,kv={boxSizing:"border-box",borderRadius:"8px",borderWidth:"1.5px",borderStyle:"solid",background:"var(--bg-secondary, #1e1e2e)",color:"var(--text-primary, #cdd6f4)",fontSize:"0.75rem",boxShadow:"0 2px 8px rgba(0,0,0,0.45)",overflow:"visible",padding:0},Rv={Root:"#15803d",End:"#dc2626",Fetcher:"#2563eb",mapper:"#ea580c",Math:"#a16207",JavaScript:"#7e22ce",Provider:"#be185d",Dictionary:"#0e7490",Join:"#65a30d",Extension:"#4338ca",Island:"#475569",Decision:"#b45309"},Bv="#6c7086";function Uv(r){const n=Rv[r]??Bv;return{...kv,borderColor:n,"--node-accent":n}}function Gv(r,n){var N;const s=new Map,u=new Map;for(const v of r)s.set(v.alias,[]),u.set(v.alias,0);for(const v of n??[])(N=s.get(v.source))==null||N.push(v.target),u.set(v.target,(u.get(v.target)??0)+1);const d=r.filter(v=>u.get(v.alias)===0||v.types.includes("entry_point")).map(v=>v.alias),f=new Map,b=[...d];for(d.forEach(v=>f.set(v,0));b.length>0;){const v=b.shift(),T=f.get(v)??0;for(const A of s.get(v)??[])(!f.has(A)||f.get(A)<=T)&&(f.set(A,T+1),b.push(A))}const y=f.size>0?Math.max(...f.values()):0;for(const v of r)f.has(v.alias)||f.set(v.alias,y+1);const m=new Map;for(const[v,T]of f)m.has(T)||m.set(T,[]),m.get(T).push(v);const g=new Map;for(const[v,T]of m){const A=T.length*dc+(T.length-1)*Df;T.forEach((E,C)=>{g.set(E,{x:v*(ah+zv),y:C*(dc+Df)-A/2})})}return g}function Hv(r){const n=Gv(r.nodes,r.connections??[]),s=r.nodes.map(d=>({id:d.alias,type:d.types[0]??"default",position:n.get(d.alias)??{x:0,y:0},width:ah,height:dc,style:Uv(d.types[0]??"unknown"),data:{alias:d.alias,nodeType:d.types[0]??"unknown",properties:d.properties}})),u=[];for(const d of r.connections??[]){const f=d.relations.map(y=>y.type),b=`${d.source}__${d.target}`;u.push({id:b,source:d.source,target:d.target,label:f.join(", "),type:"smoothstep",data:{relationTypes:f}})}return{nodes:s,edges:u}}function Lv(r,n){return r.nodes.find(s=>s.alias===n)}function qv(r,n){return(r.connections??[]).filter(s=>s.source!==s.target&&(s.source===n||s.target===n))}const Yv="_toolbar_13786_2",Xv="_label_13786_12",Jv="_toolbarActions_13786_18",Zv="_toolbarButton_13786_24",Mi={toolbar:Yv,label:Xv,toolbarActions:Jv,toolbarButton:Zv};function lh(r){const n=Se.c(19),{graphData:s,onCopySuccess:u,onCopyError:d,extraActions:f}=r;let b;n[0]!==s||n[1]!==d||n[2]!==u?(b=()=>{s&&navigator.clipboard.writeText(JSON.stringify(s,null,2)).then(()=>u==null?void 0:u()).catch(()=>d==null?void 0:d())},n[0]=s,n[1]=d,n[2]=u,n[3]=b):b=n[3];const y=b,m=(s==null?void 0:s.nodes.length)??0;let g;n[4]!==(s==null?void 0:s.connections)?(g=(s==null?void 0:s.connections)??[],n[4]=s==null?void 0:s.connections,n[5]=g):g=n[5];const N=g.length,v=m!==1?"s":"",T=N!==1?"s":"";let A;n[6]!==N||n[7]!==m||n[8]!==v||n[9]!==T?(A=h.jsxs("span",{className:Mi.label,children:[m," node",v," · ",N," connection",T]}),n[6]=N,n[7]=m,n[8]=v,n[9]=T,n[10]=A):A=n[10];let E;n[11]!==y?(E=h.jsx("button",{className:Mi.toolbarButton,onClick:y,title:"Copy raw graph JSON to clipboard","aria-label":"Copy raw graph JSON to clipboard",children:"📑"}),n[11]=y,n[12]=E):E=n[12];let C;n[13]!==f||n[14]!==E?(C=h.jsxs("div",{className:Mi.toolbarActions,children:[f,E]}),n[13]=f,n[14]=E,n[15]=C):C=n[15];let S;return n[16]!==A||n[17]!==C?(S=h.jsxs("div",{className:Mi.toolbar,children:[A,C]}),n[16]=A,n[17]=C,n[18]=S):S=n[18],S}const Of=[],zf=[];function Qv(r){const n=Se.c(65),{graphData:s,onCopySuccess:u,onCopyError:d,onRenderError:f,isRefreshing:b,onClipNode:y}=r,m=b===void 0?!1:b,[g,N]=x.useState(null),v=x.useRef(null);let T,A;n[0]!==g?(T=()=>{if(!g)return;const le=fe=>{v.current&&!v.current.contains(fe.target)&&N(null)},de=fe=>{fe.key==="Escape"&&N(null)};return document.addEventListener("mousedown",le),document.addEventListener("keydown",de),()=>{document.removeEventListener("mousedown",le),document.removeEventListener("keydown",de)}},A=[g],n[0]=g,n[1]=T,n[2]=A):(T=n[1],A=n[2]),x.useEffect(T,A);const E=x.useRef(f);let C,S;n[3]!==f?(C=()=>{E.current=f},S=[f],n[3]=f,n[4]=C,n[5]=S):(C=n[4],S=n[5]),x.useEffect(C,S);let O;e:{if(!s){let le;n[6]===Symbol.for("react.memo_cache_sentinel")?(le={nodes:Of,edges:zf,transformError:null},n[6]=le):le=n[6],O=le;break e}try{let le;n[7]!==s?(le=Hv(s),n[7]=s,n[8]=le):le=n[8];const de=le;let fe;n[9]!==de?(fe={...de,transformError:null},n[9]=de,n[10]=fe):fe=n[10],O=fe}catch(le){const de=le,fe=de instanceof Error?de.message:String(de);let ke;n[11]===Symbol.for("react.memo_cache_sentinel")?(ke={nodes:Of,edges:zf,transformError:fe},n[11]=ke):ke=n[11],O=ke}}const{nodes:R,edges:G,transformError:L}=O;let Y,q;n[12]!==L?(Y=()=>{var le;L&&((le=E.current)==null||le.call(E,`Graph render failed: ${L}`))},q=[L],n[12]=L,n[13]=Y,n[14]=q):(Y=n[13],q=n[14]),x.useEffect(Y,q);let B;n[15]!==s?(B=s?JSON.stringify(s.nodes.map(Iv)):"empty",n[15]=s,n[16]=B):B=n[16];const J=B,[I,K,$]=Oy(R),[ne,ue,ce]=zy(G);let ie,M;if(n[17]!==G||n[18]!==R||n[19]!==ue||n[20]!==K?(ie=()=>{K(R),ue(G)},M=[R,G,K,ue],n[17]=G,n[18]=R,n[19]=ue,n[20]=K,n[21]=ie,n[22]=M):(ie=n[21],M=n[22]),x.useEffect(ie,M),L){let le,de;n[23]===Symbol.for("react.memo_cache_sentinel")?(le=h.jsx("span",{className:an.emptyIcon,children:"⚠️"}),de=h.jsx("span",{children:"Graph could not be rendered."}),n[23]=le,n[24]=de):(le=n[23],de=n[24]);let fe;return n[25]!==L?(fe=h.jsxs("div",{className:an.empty,children:[le,de,h.jsx("span",{children:L})]}),n[25]=L,n[26]=fe):fe=n[26],fe}if(!s||s.nodes.length===0){let le,de;n[27]===Symbol.for("react.memo_cache_sentinel")?(le=h.jsx("span",{className:an.emptyIcon,children:"🕸️"}),de=h.jsx("span",{children:"No graph data yet."}),n[27]=le,n[28]=de):(le=n[27],de=n[28]);let fe;n[29]===Symbol.for("react.memo_cache_sentinel")?(fe=h.jsx("strong",{children:"describe graph"}),n[29]=fe):fe=n[29];let ke;return n[30]===Symbol.for("react.memo_cache_sentinel")?(ke=h.jsxs("div",{className:an.empty,children:[le,de,h.jsxs("span",{children:["Run ",fe," or ",h.jsx("strong",{children:"export graph"})," in the playground."]})]}),n[30]=ke):ke=n[30],ke}let D;n[31]!==s||n[32]!==d||n[33]!==u?(D=h.jsx(lh,{graphData:s,onCopySuccess:u,onCopyError:d}),n[31]=s,n[32]=d,n[33]=u,n[34]=D):D=n[34];let V;n[35]===Symbol.for("react.memo_cache_sentinel")?(V={padding:.25},n[35]=V):V=n[35];let P;n[36]===Symbol.for("react.memo_cache_sentinel")?(P={hideAttribution:!1},n[36]=P):P=n[36];let ee;n[37]!==y?(ee=(le,de)=>{y&&(le.preventDefault(),N({x:le.clientX,y:le.clientY,nodeAlias:de.data.alias}))},n[37]=y,n[38]=ee):ee=n[38];let re,te,F;n[39]===Symbol.for("react.memo_cache_sentinel")?(re=()=>N(null),te=h.jsx(ky,{variant:Ry.Dots,gap:18,size:1,color:"rgba(255,255,255,0.07)"}),F=h.jsx(By,{showInteractive:!1}),n[39]=re,n[40]=te,n[41]=F):(re=n[39],te=n[40],F=n[41]);let me;n[42]===Symbol.for("react.memo_cache_sentinel")?(me=h.jsx(Uy,{nodeColor:Vv,maskColor:"rgba(0,0,0,0.3)",style:{background:"#fff"}}),n[42]=me):me=n[42];let _e;n[43]!==ne||n[44]!==I||n[45]!==ce||n[46]!==$||n[47]!==ee?(_e=h.jsxs(Gy,{nodes:I,edges:ne,onNodesChange:$,onEdgesChange:ce,nodeTypes:wv,fitView:!0,fitViewOptions:V,minZoom:.2,maxZoom:2.5,proOptions:P,onNodeContextMenu:ee,onPaneClick:re,children:[te,F,me]}),n[43]=ne,n[44]=I,n[45]=ce,n[46]=$,n[47]=ee,n[48]=_e):_e=n[48];let xe;n[49]!==m?(xe=m&&h.jsx("div",{className:an.refreshingOverlay,children:h.jsx("div",{className:an.refreshingSpinner,role:"status","aria-label":"Graph refreshing"})}),n[49]=m,n[50]=xe):xe=n[50];let Be;n[51]!==g||n[52]!==s||n[53]!==y?(Be=g&&y&&s&&h.jsx("div",{ref:v,className:an.contextMenu,style:{position:"fixed",top:g.y,left:g.x},role:"menu",children:h.jsx("button",{role:"menuitem",autoFocus:!0,className:an.contextMenuItem,onClick:()=>{const le=Lv(s,g.nodeAlias);if(le){const de=qv(s,g.nodeAlias);y(le,de)}N(null)},children:"Clip to Clipboard"})}),n[51]=g,n[52]=s,n[53]=y,n[54]=Be):Be=n[54];let qe;n[55]!==m||n[56]!==D||n[57]!==_e||n[58]!==xe||n[59]!==Be?(qe=h.jsxs("div",{className:an.graphWrapper,"aria-busy":m,children:[D,_e,xe,Be]}),n[55]=m,n[56]=D,n[57]=_e,n[58]=xe,n[59]=Be,n[60]=qe):qe=n[60];let Me;return n[61]!==J||n[62]!==f||n[63]!==qe?(Me=h.jsx(Ov,{onRenderError:f,children:qe},J),n[61]=J,n[62]=f,n[63]=qe,n[64]=Me):Me=n[64],Me}function Vv(r){return{Root:"#15803d",End:"#dc2626",Fetcher:"#2563eb",mapper:"#ea580c",Math:"#a16207",JavaScript:"#7e22ce",Provider:"#be185d",Dictionary:"#0e7490",Join:"#65a30d",Extension:"#4338ca",Island:"#475569",Decision:"#b45309"}[r.type??""]??"#6c7086"}function Iv(r){return r.alias}const Kv="_root_da4ye_2",$v="_empty_da4ye_10",Wv="_emptyIcon_da4ye_23",Pv="_toolbarButton_da4ye_29",Fv="_scrollBody_da4ye_57",eb="_jsonContainer_da4ye_68",tb="_jsonLabel_da4ye_69",nb="_jsonString_da4ye_70",ab="_jsonNumber_da4ye_71",lb="_jsonBoolean_da4ye_72",ob="_jsonNull_da4ye_73",Yt={root:Kv,empty:$v,emptyIcon:Wv,toolbarButton:Pv,scrollBody:Fv,jsonContainer:eb,jsonLabel:tb,jsonString:nb,jsonNumber:ab,jsonBoolean:lb,jsonNull:ob},ib=Ly,sb=Hy,rb=r=>r<3,cb={default:rb,all:ib,none:sb};function ub(r){const n=Se.c(22),{graphData:s,onCopySuccess:u,onCopyError:d}=r,[f,b]=x.useState("all");if(!s){let L;return n[0]===Symbol.for("react.memo_cache_sentinel")?(L=h.jsx("div",{className:Yt.root,children:h.jsxs("div",{className:Yt.empty,children:[h.jsx("span",{className:Yt.emptyIcon,children:"🕸️"}),h.jsx("span",{children:"No graph data yet."}),h.jsx("span",{children:"Pin a graph-link message in the Console to load the raw data here."})]})}),n[0]=L):L=n[0],L}let y;n[1]===Symbol.for("react.memo_cache_sentinel")?(y=()=>b("all"),n[1]=y):y=n[1];const m=f==="all";let g;n[2]!==m?(g=h.jsx("button",{className:Yt.toolbarButton,onClick:y,title:"Expand all nodes","aria-label":"Expand all JSON nodes","aria-pressed":m,children:"➖"}),n[2]=m,n[3]=g):g=n[3];let N;n[4]===Symbol.for("react.memo_cache_sentinel")?(N=()=>b("none"),n[4]=N):N=n[4];const v=f==="none";let T;n[5]!==v?(T=h.jsx("button",{className:Yt.toolbarButton,onClick:N,title:"Collapse all nodes","aria-label":"Collapse all JSON nodes","aria-pressed":v,children:"➕"}),n[5]=v,n[6]=T):T=n[6];let A;n[7]!==g||n[8]!==T?(A=h.jsxs(h.Fragment,{children:[g,T]}),n[7]=g,n[8]=T,n[9]=A):A=n[9];let E;n[10]!==s||n[11]!==d||n[12]!==u||n[13]!==A?(E=h.jsx(lh,{graphData:s,onCopySuccess:u,onCopyError:d,extraActions:A}),n[10]=s,n[11]=d,n[12]=u,n[13]=A,n[14]=E):E=n[14];const C=s,S=cb[f];let O;n[15]===Symbol.for("react.memo_cache_sentinel")?(O={...uo,container:`${uo.container} ${Yt.jsonContainer}`,label:Yt.jsonLabel,stringValue:Yt.jsonString,numberValue:Yt.jsonNumber,booleanValue:Yt.jsonBoolean,nullValue:Yt.jsonNull},n[15]=O):O=n[15];let R;n[16]!==S||n[17]!==C?(R=h.jsx("div",{className:Yt.scrollBody,children:h.jsx(vc,{data:C,shouldExpandNode:S,style:O})}),n[16]=S,n[17]=C,n[18]=R):R=n[18];let G;return n[19]!==R||n[20]!==E?(G=h.jsxs("div",{className:Yt.root,children:[E,R]}),n[19]=R,n[20]=E,n[21]=G):G=n[21],G}const db="_rightPanel_1ymj3_2",pb="_tabStrip_1ymj3_10",fb="_tab_1ymj3_10",hb="_tabActive_1ymj3_38",mb="_tabBadge_1ymj3_42",yb="_tabBody_1ymj3_48",gb="_tabBodyHidden_1ymj3_57",vb="_rightPanelGroup_1ymj3_62",bb="_verticalResizeHandle_1ymj3_70",_t={rightPanel:db,tabStrip:pb,tab:fb,tabActive:hb,tabBadge:mb,tabBody:yb,tabBodyHidden:gb,rightPanelGroup:vb,verticalResizeHandle:bb},kf="help-split-percent",lc="help-split-maximized",_b=45,Sb=98;function xb({tabs:r,payload:n,onChange:s,validation:u,onFormat:d,onUpload:f,graphData:b,activeTab:y,onTabChange:m,onGraphRenderError:g,onGraphDataCopySuccess:N,onGraphDataCopyError:v,isGraphRefreshing:T,onClipNode:A,helpPanel:E}){const C=x.useId(),S=`${C}-tab-payload`,O=`${C}-tab-graph`,R=`${C}-tab-graph-data`,G=h.jsxs("div",{className:_t.rightPanel,children:[h.jsxs("div",{className:_t.tabStrip,role:"tablist","aria-label":"Right panel tabs",children:[r.includes("payload")&&h.jsx("button",{role:"tab","aria-selected":y==="payload","aria-controls":S,className:`${_t.tab}${y==="payload"?` ${_t.tabActive}`:""}`,onClick:()=>m("payload"),children:"Payload Editor"}),r.includes("graph")&&h.jsxs("button",{role:"tab","aria-selected":y==="graph","aria-controls":O,className:`${_t.tab}${y==="graph"?` ${_t.tabActive}`:""}`,onClick:()=>m("graph"),children:["Graph",b!==null&&h.jsx("span",{className:_t.tabBadge,"aria-label":"Graph data available",children:"🕸️"})]}),r.includes("graph-data")&&h.jsx("button",{role:"tab","aria-selected":y==="graph-data","aria-controls":R,className:`${_t.tab}${y==="graph-data"?` ${_t.tabActive}`:""}`,onClick:()=>m("graph-data"),children:"Graph Data (Raw)"})]}),r.includes("payload")&&h.jsx("div",{role:"tabpanel",id:S,tabIndex:y==="payload"?0:-1,className:`${_t.tabBody}${y!=="payload"?` ${_t.tabBodyHidden}`:""}`,children:h.jsx(cv,{payload:n,onChange:s,validation:u,onFormat:d,onUpload:f})}),r.includes("graph")&&h.jsx("div",{role:"tabpanel",id:O,tabIndex:y==="graph"?0:-1,className:`${_t.tabBody}${y!=="graph"?` ${_t.tabBodyHidden}`:""}`,children:h.jsx(Qv,{graphData:b,onRenderError:g,isRefreshing:T,onCopySuccess:N,onCopyError:v,onClipNode:A})}),r.includes("graph-data")&&h.jsx("div",{role:"tabpanel",id:R,tabIndex:y==="graph-data"?0:-1,className:`${_t.tabBody}${y!=="graph-data"?` ${_t.tabBodyHidden}`:""}`,children:h.jsx(ub,{graphData:b,onCopySuccess:N,onCopyError:v})})]}),L=x.useRef(Number(sessionStorage.getItem(kf))||_b),Y=x.useRef(null),q=x.useRef(null),[B,J]=x.useState(()=>sessionStorage.getItem(lc)==="1"),I=x.useRef(B),K=x.useCallback(D=>{const V=D["help-split-help"];if(V===void 0)return;const P=V>=Sb;P!==I.current&&(I.current=P,J(P),sessionStorage.setItem(lc,P?"1":"0")),P||(L.current=V,sessionStorage.setItem(kf,String(V)))},[]),$=x.useCallback(()=>{var V,P,ee,re;const D=!I.current;if(I.current=D,J(D),sessionStorage.setItem(lc,D?"1":"0"),D)(V=q.current)==null||V.resize("0%"),(P=Y.current)==null||P.resize("100%");else{const te=L.current;(ee=Y.current)==null||ee.resize(`${te}%`),(re=q.current)==null||re.resize(`${100-te}%`)}},[]),ne=!!E;if(x.useEffect(()=>{ne&&I.current&&requestAnimationFrame(()=>{var D,V;(D=q.current)==null||D.resize("0%"),(V=Y.current)==null||V.resize("100%")})},[ne]),!E)return G;const ue=typeof E=="function"?E($,B):E,ie=I.current?100:L.current,M=100-ie;return h.jsxs(Zf,{orientation:"vertical",className:_t.rightPanelGroup,onLayoutChanged:K,children:[h.jsx(co,{panelRef:q,defaultSize:`${M}%`,minSize:"0%",children:G}),h.jsx(sc,{className:_t.verticalResizeHandle,"aria-label":"Resize help panel"}),h.jsx(co,{id:"help-split-help",panelRef:Y,defaultSize:`${ie}%`,minSize:"15%",children:ue})]})}class Tb extends Vf.Component{constructor(){super(...arguments),this.state={hasError:!1}}static getDerivedStateFromError(){return{hasError:!0}}componentDidCatch(n,s){console.error("[ConsoleErrorBoundary] Failed to render message:",n,s.componentStack)}render(){return this.state.hasError?h.jsx("span",{children:this.props.fallback}):this.props.children}}const wb=2e3,Eb=(r={})=>{const{onSuccess:n,onError:s}=r,[u,d]=x.useState(!1),f=x.useRef(null);return x.useEffect(()=>()=>{f.current!==null&&clearTimeout(f.current)},[]),{copy:x.useCallback(async y=>{if(!navigator.clipboard)return console.warn("useCopyToClipboard: Clipboard API not available in this browser."),s==null||s(),!1;try{return await navigator.clipboard.writeText(y),d(!0),f.current!==null&&clearTimeout(f.current),f.current=setTimeout(()=>{f.current=null,d(!1)},wb),n==null||n(),!0}catch(m){return console.error("useCopyToClipboard: Failed to write to clipboard.",m),s==null||s(),!1}},[n,s]),copied:u}},Nb="_consoleRoot_trpoh_2",Ab="_consoleHeader_trpoh_10",Cb="_consoleTitle_trpoh_20",jb="_consoleControls_trpoh_25",Mb="_controlButton_trpoh_30",Db="_console_trpoh_2",Ob="_emptyConsole_trpoh_67",zb="_consoleMessage_trpoh_80",kb="_consoleMessageActivatable_trpoh_94",Rb="_consoleMessageGraphLink_trpoh_104",Bb="_consoleMessageLargePayload_trpoh_115",Ub="_consoleMessageMockUpload_trpoh_122",Gb="_uploadMockButton_trpoh_131",Hb="_copyButton_trpoh_172",Lb="_copyButtonCopied_trpoh_225",qb="_sendToJsonPathButton_trpoh_234",Yb="_messageIcon_trpoh_268",Xb="_messageContent_trpoh_272",Jb="_messageText_trpoh_278",Zb="_messageTime_trpoh_283",Qb="_jsonViewWrapper_trpoh_295",Vb="_jsonContainer_trpoh_301",Ib="_jsonLabel_trpoh_302",Kb="_jsonString_trpoh_303",$b="_jsonNumber_trpoh_304",Wb="_jsonBoolean_trpoh_305",Pb="_jsonNull_trpoh_306",Le={consoleRoot:Nb,consoleHeader:Ab,consoleTitle:Cb,consoleControls:jb,controlButton:Mb,console:Db,emptyConsole:Ob,consoleMessage:zb,consoleMessageActivatable:kb,consoleMessageGraphLink:Rb,consoleMessageLargePayload:Bb,consoleMessageMockUpload:Ub,uploadMockButton:Gb,copyButton:Hb,copyButtonCopied:Lb,sendToJsonPathButton:qb,messageIcon:Yb,messageContent:Xb,messageText:Jb,messageTime:Zb,"messageType-error":"_messageType-error_trpoh_290","messageType-info":"_messageType-info_trpoh_291","messageType-welcome":"_messageType-welcome_trpoh_292",jsonViewWrapper:Qb,jsonContainer:Vb,jsonLabel:Ib,jsonString:Kb,jsonNumber:$b,jsonBoolean:Wb,jsonNull:Pb};function Fb(r){var sa;const n=Se.c(77),{message:s,msgId:u,classificationMap:d,onGraphLink:f,onCopyMessage:b,onSendToJsonPath:y,onUploadMockData:m,successfulUploadPaths:g}=r;let N,v,T;n[0]!==s?(v=vg(s),N=bg(v.type),T=po(v.message),n[0]=s,n[1]=N,n[2]=v,n[3]=T):(N=n[1],v=n[2],T=n[3]);const A=T;let E,C,S,O,R,G;if(n[4]!==d||n[5]!==u||n[6]!==m||n[7]!==g){const ge=(u!==void 0?d==null?void 0:d.get(u):void 0)??[];C=ge.some(l2),S=ge.some(a2),O=ge.some(n2),R=((sa=ge.find(t2))==null?void 0:sa.uploadPath)??null,E=!!m&&O&&R!==null,G=E&&!!(g!=null&&g.has(R)),n[4]=d,n[5]=u,n[6]=m,n[7]=g,n[8]=E,n[9]=C,n[10]=S,n[11]=O,n[12]=R,n[13]=G}else E=n[8],C=n[9],S=n[10],O=n[11],R=n[12],G=n[13];const L=G,Y=!!f&&C&&!O&&!S,q=!!y&&A.isJSON;let B;n[14]!==b?(B={onSuccess:b},n[14]=b,n[15]=B):B=n[15];const{copy:J,copied:I}=Eb(B);let K;n[16]!==J||n[17]!==s?(K=ge=>{ge.stopPropagation(),J(s)},n[16]=J,n[17]=s,n[18]=K):K=n[18];const $=K;let ne;n[19]!==J||n[20]!==s?(ne=ge=>{(ge.key==="Enter"||ge.key===" ")&&(ge.preventDefault(),ge.stopPropagation(),J(s))},n[19]=J,n[20]=s,n[21]=ne):ne=n[21];const ue=ne;let ce;n[22]!==A.data||n[23]!==A.isJSON||n[24]!==y?(ce=ge=>{if(ge.stopPropagation(),!y||!A.isJSON)return;const pn=JSON.stringify(A.data,null,2);y(pn)},n[22]=A.data,n[23]=A.isJSON,n[24]=y,n[25]=ce):ce=n[25];const ie=ce;let M;n[26]!==R||n[27]!==m?(M=ge=>{ge.stopPropagation(),!(!m||!R)&&m(R)},n[26]=R,n[27]=m,n[28]=M):M=n[28];const D=M,V=Le[`messageType-${v.type}`],P=Y?Le.consoleMessageActivatable:"",ee=C?Le.consoleMessageGraphLink:"",re=S?Le.consoleMessageLargePayload:"",te=O?Le.consoleMessageMockUpload:"";let F;n[29]!==ee||n[30]!==re||n[31]!==te||n[32]!==V||n[33]!==P?(F=[Le.consoleMessage,V,P,ee,re,te].filter(Boolean),n[29]=ee,n[30]=re,n[31]=te,n[32]=V,n[33]=P,n[34]=F):F=n[34];const me=F.join(" ");let _e;n[35]!==Y||n[36]!==f?(_e=Y?()=>f():void 0,n[35]=Y,n[36]=f,n[37]=_e):_e=n[37];const xe=Y?"Click to load graph in Graph View":void 0,Be=Y?"button":void 0,qe=Y?0:void 0;let Me;n[38]!==Y||n[39]!==f?(Me=Y?ge=>{(ge.key==="Enter"||ge.key===" ")&&(ge.preventDefault(),f())}:void 0,n[38]=Y,n[39]=f,n[40]=Me):Me=n[40];const le=Y?"Load graph in Graph View":void 0,de=O?"⬆️":S?"⬇️":C?"🕸️":N;let fe;n[41]!==de?(fe=h.jsx("span",{className:Le.messageIcon,children:de}),n[41]=de,n[42]=fe):fe=n[42];let ke;n[43]!==A.data||n[44]!==A.isJSON||n[45]!==v.message||n[46]!==L?(ke=h.jsx("div",{className:Le.messageContent,children:A.isJSON?h.jsx("div",{className:Le.jsonViewWrapper,children:h.jsx(vc,{data:A.data,shouldExpandNode:e2,style:{...uo,container:`${uo.container} ${Le.jsonContainer}`,label:Le.jsonLabel,stringValue:Le.jsonString,numberValue:Le.jsonNumber,booleanValue:Le.jsonBoolean,nullValue:Le.jsonNull}})}):h.jsxs("span",{className:Le.messageText,children:[v.message,L&&h.jsx("span",{title:"Upload succeeded",children:" ✅"})]})}),n[43]=A.data,n[44]=A.isJSON,n[45]=v.message,n[46]=L,n[47]=ke):ke=n[47];const W=`${Le.copyButton} ${I?Le.copyButtonCopied:""}`,ye=I?"Copied!":"Copy message",Te=I?"Copied to clipboard":"Copy message to clipboard",he=I?"✅":"📄";let De;n[48]!==$||n[49]!==ue||n[50]!==W||n[51]!==ye||n[52]!==Te||n[53]!==he?(De=h.jsx("button",{className:W,onClick:$,onKeyDown:ue,title:ye,"aria-label":Te,tabIndex:0,children:he}),n[48]=$,n[49]=ue,n[50]=W,n[51]=ye,n[52]=Te,n[53]=he,n[54]=De):De=n[54];let Ue;n[55]!==q||n[56]!==ie?(Ue=q&&h.jsx("button",{className:Le.sendToJsonPathButton,onClick:ie,onKeyDown:ge=>{(ge.key==="Enter"||ge.key===" ")&&ie(ge)},title:"Open in JSON-Path Playground","aria-label":"Open this JSON in the JSON-Path Playground",tabIndex:0,children:"➡️"}),n[55]=q,n[56]=ie,n[57]=Ue):Ue=n[57];let dt;n[58]!==E||n[59]!==D?(dt=E&&h.jsx("button",{className:Le.uploadMockButton,onClick:D,onKeyDown:ge=>{(ge.key==="Enter"||ge.key===" ")&&D(ge)},title:"Re-open upload dialog","aria-label":"Re-open mock data upload dialog",tabIndex:0,children:"⬆️ Upload JSON…"}),n[58]=E,n[59]=D,n[60]=dt):dt=n[60];let Fe;n[61]!==v.time?(Fe=v.time&&h.jsx("span",{className:Le.messageTime,children:v.time}),n[61]=v.time,n[62]=Fe):Fe=n[62];let ct;return n[63]!==me||n[64]!==_e||n[65]!==xe||n[66]!==Be||n[67]!==qe||n[68]!==Me||n[69]!==le||n[70]!==fe||n[71]!==ke||n[72]!==De||n[73]!==Ue||n[74]!==dt||n[75]!==Fe?(ct=h.jsxs("div",{className:me,onClick:_e,title:xe,role:Be,tabIndex:qe,onKeyDown:Me,"aria-label":le,children:[fe,ke,De,Ue,dt,Fe]}),n[63]=me,n[64]=_e,n[65]=xe,n[66]=Be,n[67]=qe,n[68]=Me,n[69]=le,n[70]=fe,n[71]=ke,n[72]=De,n[73]=Ue,n[74]=dt,n[75]=Fe,n[76]=ct):ct=n[76],ct}function e2(r){return r<1}function t2(r){return r.kind==="upload.invitation"}function n2(r){return r.kind==="upload.invitation"}function a2(r){return r.kind==="payload.large"}function l2(r){return r.kind==="graph.link"}function o2(r){const n=Se.c(32),{messages:s,classificationMap:u,onCopy:d,onClear:f,consoleRef:b,onGraphLinkMessage:y,onCopyMessage:m,onSendToJsonPath:g,onUploadMockData:N,successfulUploadPaths:v}=r;let T;n[0]===Symbol.for("react.memo_cache_sentinel")?(T=h.jsx("span",{className:Le.consoleTitle,children:"Console Output"}),n[0]=T):T=n[0];let A;n[1]!==d?(A=h.jsx("button",{className:Le.controlButton,onClick:d,title:"Copy console output","aria-label":"Copy console output to clipboard",children:"📑"}),n[1]=d,n[2]=A):A=n[2];let E;n[3]!==f?(E=h.jsx("button",{className:Le.controlButton,onClick:f,title:"Clear console","aria-label":"Clear console",children:"🗑️"}),n[3]=f,n[4]=E):E=n[4];let C;n[5]!==A||n[6]!==E?(C=h.jsxs("div",{className:Le.consoleHeader,children:[T,h.jsxs("div",{className:Le.consoleControls,children:[A,E]})]}),n[5]=A,n[6]=E,n[7]=C):C=n[7];let S;if(n[8]!==u||n[9]!==s||n[10]!==m||n[11]!==y||n[12]!==g||n[13]!==N||n[14]!==v){let L;n[16]!==u||n[17]!==m||n[18]!==y||n[19]!==g||n[20]!==N||n[21]!==v?(L=Y=>h.jsx(Tb,{fallback:Y.raw,children:h.jsx(Fb,{message:Y.raw,msgId:Y.id,classificationMap:u,onGraphLink:y?()=>y(Y):void 0,onCopyMessage:m,onSendToJsonPath:g,onUploadMockData:N,successfulUploadPaths:v})},Y.id),n[16]=u,n[17]=m,n[18]=y,n[19]=g,n[20]=N,n[21]=v,n[22]=L):L=n[22],S=s.map(L),n[8]=u,n[9]=s,n[10]=m,n[11]=y,n[12]=g,n[13]=N,n[14]=v,n[15]=S}else S=n[15];let O;n[23]!==s.length?(O=s.length===0&&h.jsxs("div",{className:Le.emptyConsole,children:["No messages yet. Use the ",h.jsx("strong",{children:"Start"})," button in the header to connect."]}),n[23]=s.length,n[24]=O):O=n[24];let R;n[25]!==b||n[26]!==S||n[27]!==O?(R=h.jsxs("div",{className:Le.console,ref:b,role:"log","aria-live":"polite",children:[S,O]}),n[25]=b,n[26]=S,n[27]=O,n[28]=R):R=n[28];let G;return n[29]!==C||n[30]!==R?(G=h.jsxs("div",{className:Le.consoleRoot,children:[C,R]}),n[29]=C,n[30]=R,n[31]=G):G=n[31],G}const i2="_commandInput_o73qt_2",s2="_labelRow_o73qt_8",r2="_labelGroup_o73qt_16",c2="_label_o73qt_8",u2="_infoWrapper_o73qt_28",d2="_paletteToggle_o73qt_34",p2="_paletteToggleActive_o73qt_60",f2="_popover_o73qt_67",h2="_popoverOpen_o73qt_89",m2="_popoverTitle_o73qt_115",y2="_popoverRow_o73qt_129",g2="_popoverKeyword_o73qt_146",v2="_popoverDesc_o73qt_158",b2="_popoverAlias_o73qt_164",_2="_inputRow_o73qt_171",S2="_inputWrapper_o73qt_177",x2="_textarea_o73qt_187",T2="_sendButton_o73qt_216",w2="_hint_o73qt_233",E2="_dropup_o73qt_241",N2="_dropupHeader_o73qt_256",A2="_dropupItem_o73qt_272",C2="_dropupItemText_o73qt_295",j2="_matchHighlight_o73qt_303",M2="_multilineIndicator_o73qt_309",Ke={commandInput:i2,labelRow:s2,labelGroup:r2,label:c2,infoWrapper:u2,paletteToggle:d2,paletteToggleActive:p2,popover:f2,popoverOpen:h2,popoverTitle:m2,popoverRow:y2,popoverKeyword:g2,popoverDesc:v2,popoverAlias:b2,inputRow:_2,inputWrapper:S2,textarea:x2,sendButton:T2,hint:w2,dropup:E2,dropupHeader:N2,dropupItem:A2,dropupItemText:C2,matchHighlight:j2,multilineIndicator:M2},D2=[{keyword:"help",description:"List all help topics, or get help for a specific command",template:"help"},{keyword:"create",description:"Create a new graph node",template:`create node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:"update",description:"Update an existing node",template:`update node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:"edit",description:"Print raw node data ready for editing and re-submitting",template:"edit node {name}"},{keyword:"delete node",description:"Delete a node by name",alias:"clear node",template:"delete node {name}"},{keyword:"delete connection",description:"Delete connection(s) between two nodes",alias:"clear connection",template:"delete connection {nodeA} and {nodeB}"},{keyword:"delete cache",description:"Clear cached API fetcher results",alias:"clear cache",template:"delete cache"},{keyword:"connect",description:"Connect two nodes with a named relation",template:"connect {node-A} to {node-B} with {relation}"},{keyword:"list nodes",description:"List all nodes in the current graph",template:"list nodes"},{keyword:"list connections",description:"List all connections in the current graph",template:"list connections"},{keyword:"describe graph",description:"Describe the current graph model",template:"describe graph"},{keyword:"describe node",description:"Describe a specific node and its connections",template:"describe node {name}"},{keyword:"describe connection",description:"Describe connection(s) between two nodes",template:"describe connection {nodeA} and {nodeB}"},{keyword:"describe skill",description:"Show documentation for a skill by route name",template:"describe skill {skill.route}"},{keyword:"export",description:"Export the graph model to a JSON file",template:"export graph as {name}"},{keyword:"import graph",description:"Import a graph model from a saved file",template:"import graph from {name}"},{keyword:"import node",description:"Import a single node from another saved graph",template:"import node {node-name} from {graph-name}"},{keyword:"instantiate",description:"Create a runnable graph instance with mock input",alias:"start",template:`instantiate graph
{constant} -> input.body.{key}`,multiline:!0},{keyword:"upload mock data",description:"Print the URL to POST a JSON payload as mock input.body",template:"upload mock data"},{keyword:"execute",description:"Execute a single node skill in isolation",template:"execute node {name}"},{keyword:"inspect",description:"Inspect a state-machine variable",template:"inspect {variable_name}"},{keyword:"run",description:"Run the graph instance from root to end",template:"run"}];function O2(r,n){const s=Se.c(22),[u,d]=x.useState(!1),[f,b]=x.useState(-1);let y;if(s[0]!==n||s[1]!==r){e:{const L=n.trimStart();if(L.length===0){let I;s[3]===Symbol.for("react.memo_cache_sentinel")?(I=[],s[3]=I):I=s[3],y=I;break e}const Y=L.toLowerCase(),q=r.filter(I=>I.toLowerCase().startsWith(Y)),B=new Set;y=q.filter(I=>B.has(I)?!1:(B.add(I),!0)).slice(0,hg)}s[0]=n,s[1]=r,s[2]=y}else y=s[2];const m=y;let g;s[4]===Symbol.for("react.memo_cache_sentinel")?(g=()=>{d(!0),b(-1)},s[4]=g):g=s[4];const N=g;let v;s[5]!==m?(v=L=>{const Y=m.length;Y!==0&&b(q=>L===1?q<0?0:(q+1)%Y:q<=0?Y-1:q-1)},s[5]=m,s[6]=v):v=s[6];const T=v;let A;s[7]!==m?(A=(L,Y)=>{L>=0&&L<m.length&&Y(m[L]),d(!1),b(-1)},s[7]=m,s[8]=A):A=s[8];const E=A;let C;s[9]!==E||s[10]!==f||s[11]!==u||s[12]!==m?(C=L=>{if(!u||m.length===0)return;const Y=f>=0?f:0;E(Y,L)},s[9]=E,s[10]=f,s[11]=u,s[12]=m,s[13]=C):C=s[13];const S=C;let O;s[14]===Symbol.for("react.memo_cache_sentinel")?(O=()=>{d(!1),b(-1)},s[14]=O):O=s[14];const R=O;let G;return s[15]!==E||s[16]!==f||s[17]!==u||s[18]!==T||s[19]!==S||s[20]!==m?(G={suggestions:m,isOpen:u,activeIndex:f,onCommandChange:N,navigate:T,accept:E,onTab:S,dismiss:R},s[15]=E,s[16]=f,s[17]=u,s[18]=T,s[19]=S,s[20]=m,s[21]=G):G=s[21],G}const z2=r=>x.createElement("svg",{xmlns:"http://www.w3.org/2000/svg",viewBox:"0 0 16 16",fill:"none",width:14,height:14,stroke:"currentColor",strokeWidth:1.5,strokeLinecap:"round",strokeLinejoin:"round",...r},x.createElement("polyline",{points:"2,4 6,8 2,12"}),x.createElement("line",{x1:7,y1:12,x2:14,y2:12}));function k2(r){const n=Se.c(70),{command:s,onChange:u,onKeyDown:d,onSend:f,sendDisabled:b,disabled:y,history:m}=r,g=x.useRef(null),N=x.useRef(null),v=x.useRef(null),[T,A]=x.useState(!1);let E,C;n[0]!==T?(E=()=>{if(!T)return;const W=ye=>{N.current&&!N.current.contains(ye.target)&&A(!1)};return document.addEventListener("mousedown",W),()=>document.removeEventListener("mousedown",W)},C=[T],n[0]=T,n[1]=E,n[2]=C):(E=n[1],C=n[2]),x.useEffect(E,C);const S=O2(m,s);let O;n[3]===Symbol.for("react.memo_cache_sentinel")?(O=()=>{const W=g.current;W&&(W.style.height="auto",W.style.height=`${W.scrollHeight}px`)},n[3]=O):O=n[3];let R;n[4]!==s?(R=[s],n[4]=s,n[5]=R):R=n[5],x.useEffect(O,R);const G=y?"Not connected":"Enter command (Enter to send · Shift+Enter for new line)",L=y?"Enter your test message once it is connected":"Enter to send · Shift+Enter for new line · ↑↓ for history";let Y;n[6]!==S||n[7]!==u||n[8]!==d||n[9]!==f?(Y=W=>{var ye,Te;if(W.key==="Tab"){W.preventDefault(),S.isOpen&&S.suggestions.length>0&&(S.onTab(he=>u(he)),requestAnimationFrame(()=>{const he=g.current;he&&(he.selectionStart=he.selectionEnd=he.value.length)}));return}if(W.key==="Enter"){if(W.shiftKey)return;if(W.preventDefault(),S.isOpen&&S.activeIndex>=0){S.accept(S.activeIndex,he=>u(he)),requestAnimationFrame(()=>{const he=g.current;he&&(he.selectionStart=he.selectionEnd=he.value.length)}),(ye=g.current)==null||ye.focus();return}f(),(Te=g.current)==null||Te.focus();return}if(W.key==="Escape"){if(S.isOpen){S.dismiss(),W.preventDefault();return}return}if(W.key==="ArrowUp"||W.key==="ArrowDown"){if(S.isOpen&&S.suggestions.length>0){W.preventDefault(),S.navigate(W.key==="ArrowDown"?1:-1);return}const he=g.current;if(he){const{selectionStart:De,value:Ue}=he,Fe=!Ue.slice(0,De).includes(`
`),ct=!Ue.slice(De).includes(`
`);if(!(W.key==="ArrowUp"&&Fe||W.key==="ArrowDown"&&ct))return}d(W),requestAnimationFrame(()=>{const De=g.current;De&&(De.selectionStart=De.selectionEnd=De.value.length)});return}d(W)},n[6]=S,n[7]=u,n[8]=d,n[9]=f,n[10]=Y):Y=n[10];const q=Y;let B;n[11]===Symbol.for("react.memo_cache_sentinel")?(B=h.jsx("label",{htmlFor:"command",className:Ke.label,children:"Command"}),n[11]=B):B=n[11];const J=`${Ke.paletteToggle}${T?` ${Ke.paletteToggleActive}`:""}`;let I;n[12]===Symbol.for("react.memo_cache_sentinel")?(I=()=>A(U2),n[12]=I):I=n[12];let K;n[13]!==T?(K=W=>{var ye;if(W.key==="ArrowDown"&&T){W.preventDefault();const Te=(ye=v.current)==null?void 0:ye.querySelector('[role="option"]');Te==null||Te.focus()}},n[13]=T,n[14]=K):K=n[14];let $;n[15]===Symbol.for("react.memo_cache_sentinel")?($=h.jsx(z2,{"aria-hidden":"true",focusable:"false"}),n[15]=$):$=n[15];let ne;n[16]!==T||n[17]!==J||n[18]!==K?(ne=h.jsx("button",{type:"button",className:J,"aria-label":"Toggle command palette","aria-expanded":T,"aria-controls":"command-palette",onClick:I,onKeyDown:K,title:"Command palette",children:$}),n[16]=T,n[17]=J,n[18]=K,n[19]=ne):ne=n[19];const ue=`${Ke.popover}${T?` ${Ke.popoverOpen}`:""}`;let ce,ie;n[20]===Symbol.for("react.memo_cache_sentinel")?(ce=W=>{var ye,Te;if(W.key==="ArrowDown"||W.key==="ArrowUp"){W.preventDefault();const he=(ye=v.current)==null?void 0:ye.querySelectorAll('[role="option"]');if(!he||he.length===0)return;const De=Array.from(he).indexOf(document.activeElement);W.key==="ArrowDown"?he[De<0?0:(De+1)%he.length].focus():he[De<=0?he.length-1:De-1].focus()}else W.key==="Escape"&&(W.preventDefault(),A(!1),(Te=g.current)==null||Te.focus())},ie=h.jsx("p",{className:Ke.popoverTitle,children:"Command palette — click to insert"}),n[20]=ce,n[21]=ie):(ce=n[20],ie=n[21]);let M;n[22]!==T||n[23]!==u?(M=D2.map(W=>{const{keyword:ye,alias:Te,description:he,template:De}=W;return h.jsxs("div",{className:Ke.popoverRow,role:"option","aria-selected":!1,tabIndex:T?0:-1,onMouseDown:B2,onClick:()=>{var Ue;u(De),A(!1),(Ue=g.current)==null||Ue.focus()},onKeyDown:Ue=>{var dt;(Ue.key==="Enter"||Ue.key===" ")&&(Ue.preventDefault(),u(De),A(!1),(dt=g.current)==null||dt.focus())},children:[h.jsx("span",{className:Ke.popoverKeyword,children:ye}),h.jsxs("span",{className:Ke.popoverDesc,children:[he,Te&&h.jsxs("span",{className:Ke.popoverAlias,children:[" · alias: ",Te]})]})]},ye)}),n[22]=T,n[23]=u,n[24]=M):M=n[24];let D;n[25]!==ue||n[26]!==M?(D=h.jsxs("div",{id:"command-palette",ref:v,className:ue,role:"listbox","aria-label":"Command palette",onKeyDown:ce,children:[ie,M]}),n[25]=ue,n[26]=M,n[27]=D):D=n[27];let V;n[28]!==ne||n[29]!==D?(V=h.jsx("div",{className:Ke.labelRow,children:h.jsxs("div",{className:Ke.labelGroup,children:[B,h.jsxs("span",{ref:N,className:Ke.infoWrapper,children:[ne,D]})]})}),n[28]=ne,n[29]=D,n[30]=V):V=n[30];const P=!(S.isOpen&&S.suggestions.length>0);let ee;n[31]===Symbol.for("react.memo_cache_sentinel")?(ee=h.jsx("div",{className:Ke.dropupHeader,"aria-hidden":"true",children:"Recent Commands"}),n[31]=ee):ee=n[31];let re;n[32]!==S||n[33]!==s||n[34]!==u?(re=S.isOpen&&S.suggestions.length>0&&S.suggestions.map((W,ye)=>{const Te=W.split(`
`)[0],he=W.includes(`
`),De=s.trimStart().split(`
`)[0],Ue=Math.min(De.length,Te.length),dt=Te.slice(0,Ue),Fe=Te.slice(Ue);return h.jsxs("div",{id:`history-option-${ye}`,role:"option","aria-selected":ye===S.activeIndex,className:Ke.dropupItem,onMouseDown:R2,onClick:()=>{S.accept(ye,ct=>u(ct)),requestAnimationFrame(()=>{const ct=g.current;ct&&(ct.selectionStart=ct.selectionEnd=ct.value.length)})},children:[h.jsxs("span",{className:Ke.dropupItemText,children:[Ue>0&&h.jsx("strong",{className:Ke.matchHighlight,children:dt}),Fe,he?"…":""]}),he&&h.jsx("span",{className:Ke.multilineIndicator,"aria-label":"multi-line command",children:"↵"})]},W)}),n[32]=S,n[33]=s,n[34]=u,n[35]=re):re=n[35];let te;n[36]!==P||n[37]!==re?(te=h.jsxs("div",{id:"history-dropup",role:"listbox","aria-label":"Command history suggestions",className:Ke.dropup,hidden:P,children:[ee,re]}),n[36]=P,n[37]=re,n[38]=te):te=n[38];const F=S.isOpen&&S.suggestions.length>0,me=S.isOpen&&S.suggestions.length>0&&S.activeIndex>=0?`history-option-${S.activeIndex}`:void 0;let _e;n[39]!==S||n[40]!==u?(_e=W=>{u(W.target.value),S.onCommandChange()},n[39]=S,n[40]=u,n[41]=_e):_e=n[41];let xe;n[42]!==S?(xe=()=>S.dismiss(),n[42]=S,n[43]=xe):xe=n[43];let Be;n[44]!==s||n[45]!==y||n[46]!==q||n[47]!==G||n[48]!==F||n[49]!==me||n[50]!==_e||n[51]!==xe?(Be=h.jsx("textarea",{ref:g,id:"command",role:"combobox","aria-expanded":F,"aria-haspopup":"listbox","aria-controls":"history-dropup","aria-activedescendant":me,"aria-autocomplete":"list",className:Ke.textarea,rows:1,placeholder:G,value:s,disabled:y,onChange:_e,onKeyDown:q,onBlur:xe,autoComplete:"off",autoCorrect:"off",spellCheck:!1}),n[44]=s,n[45]=y,n[46]=q,n[47]=G,n[48]=F,n[49]=me,n[50]=_e,n[51]=xe,n[52]=Be):Be=n[52];let qe;n[53]!==te||n[54]!==Be?(qe=h.jsxs("div",{className:Ke.inputWrapper,children:[te,Be]}),n[53]=te,n[54]=Be,n[55]=qe):qe=n[55];let Me;n[56]!==f?(Me=()=>{var W;f(),(W=g.current)==null||W.focus()},n[56]=f,n[57]=Me):Me=n[57];let le;n[58]!==b||n[59]!==Me?(le=h.jsx("button",{className:Ke.sendButton,onClick:Me,disabled:b,"aria-label":"Send command",children:"Send"}),n[58]=b,n[59]=Me,n[60]=le):le=n[60];let de;n[61]!==qe||n[62]!==le?(de=h.jsxs("div",{className:Ke.inputRow,children:[qe,le]}),n[61]=qe,n[62]=le,n[63]=de):de=n[63];let fe;n[64]!==L?(fe=h.jsx("p",{className:Ke.hint,children:L}),n[64]=L,n[65]=fe):fe=n[65];let ke;return n[66]!==V||n[67]!==de||n[68]!==fe?(ke=h.jsxs("div",{className:Ke.commandInput,children:[V,de,fe]}),n[66]=V,n[67]=de,n[68]=fe,n[69]=ke):ke=n[69],ke}function R2(r){return r.preventDefault()}function B2(r){return r.preventDefault()}function U2(r){return!r}const G2="_root_1ac49_1",H2={root:G2};function L2(r){const n=Se.c(22),{messages:s,classificationMap:u,onCopy:d,onClear:f,consoleRef:b,onGraphLinkMessage:y,onCopyMessage:m,onSendToJsonPath:g,onUploadMockData:N,successfulUploadPaths:v,command:T,onCommandChange:A,onCommandKeyDown:E,onSend:C,sendDisabled:S,inputDisabled:O,commandHistory:R}=r;let G;n[0]!==u||n[1]!==b||n[2]!==s||n[3]!==f||n[4]!==d||n[5]!==m||n[6]!==y||n[7]!==g||n[8]!==N||n[9]!==v?(G=h.jsx(o2,{messages:s,classificationMap:u,onCopy:d,onClear:f,consoleRef:b,onGraphLinkMessage:y,onCopyMessage:m,onSendToJsonPath:g,onUploadMockData:N,successfulUploadPaths:v}),n[0]=u,n[1]=b,n[2]=s,n[3]=f,n[4]=d,n[5]=m,n[6]=y,n[7]=g,n[8]=N,n[9]=v,n[10]=G):G=n[10];let L;n[11]!==T||n[12]!==R||n[13]!==O||n[14]!==A||n[15]!==E||n[16]!==C||n[17]!==S?(L=h.jsx(k2,{command:T,onChange:A,onKeyDown:E,onSend:C,disabled:O,sendDisabled:S,history:R}),n[11]=T,n[12]=R,n[13]=O,n[14]=A,n[15]=E,n[16]=C,n[17]=S,n[18]=L):L=n[18];let Y;return n[19]!==G||n[20]!==L?(Y=h.jsxs("div",{className:H2.root,children:[G,L]}),n[19]=G,n[20]=L,n[21]=Y):Y=n[21],Y}const q2="_dialog_1ih1o_4",Y2="_modalInner_1ih1o_26",X2="_modalHeader_1ih1o_34",J2="_modalTitleGroup_1ih1o_44",Z2="_modalTitle_1ih1o_44",Q2="_modalPath_1ih1o_57",V2="_closeButton_1ih1o_64",I2="_modalBody_1ih1o_95",K2="_dropZone_1ih1o_105",$2="_dropZoneActive_1ih1o_127",W2="_dropZoneIcon_1ih1o_133",P2="_dropZoneText_1ih1o_139",F2="_dropZoneOr_1ih1o_152",e_="_browseButton_1ih1o_159",t_="_fileInputHidden_1ih1o_188",n_="_fileError_1ih1o_193",a_="_textareaLabel_1ih1o_198",l_="_textarea_1ih1o_198",o_="_validationError_1ih1o_226",i_="_keyboardHint_1ih1o_231",s_="_errorBanner_1ih1o_236",r_="_modalFooter_1ih1o_247",c_="_footerActions_1ih1o_257",u_="_formatButton_1ih1o_263",d_="_cancelButton_1ih1o_264",p_="_uploadButton_1ih1o_265",f_="_spinner_1ih1o_332",Qe={dialog:q2,modalInner:Y2,modalHeader:X2,modalTitleGroup:J2,modalTitle:Z2,modalPath:Q2,closeButton:V2,modalBody:I2,dropZone:K2,dropZoneActive:$2,dropZoneIcon:W2,dropZoneText:P2,dropZoneOr:F2,browseButton:e_,fileInputHidden:t_,fileError:n_,textareaLabel:a_,textarea:l_,validationError:o_,keyboardHint:i_,errorBanner:s_,modalFooter:r_,footerActions:c_,formatButton:u_,cancelButton:d_,uploadButton:p_,spinner:f_};function h_(r){const n=Se.c(9),{uploadPath:s,json:u,onSuccess:d,onError:f}=r,[b,y]=x.useState(!1),m=x.useRef(null);let g;n[0]===Symbol.for("react.memo_cache_sentinel")?(g=()=>{var E;(E=m.current)==null||E.abort(),m.current=null,y(!1)},n[0]=g):g=n[0];const N=g;let v;n[1]!==u||n[2]!==f||n[3]!==d||n[4]!==s?(v=async()=>{var C;(C=m.current)==null||C.abort();const E=new AbortController;m.current=E,y(!0);try{const S=await fetch(s,{method:"POST",headers:{"Content-Type":"application/json"},body:u,signal:E.signal}),O=await S.text();if(!S.ok){y(!1),f(`HTTP ${S.status} — ${O}`);return}y(!1),d(O)}catch(S){const O=S;if(O.name==="AbortError"){y(!1);return}y(!1),f(O.message??"Network error")}},n[1]=u,n[2]=f,n[3]=d,n[4]=s,n[5]=v):v=n[5];const T=v;let A;return n[6]!==b||n[7]!==T?(A={isUploading:b,upload:T,cancel:N},n[6]=b,n[7]=T,n[8]=A):A=n[8],A}var Jf;const m_=(((Jf=navigator.userAgentData)==null?void 0:Jf.platform)??navigator.platform).toLowerCase().includes("mac");function y_(r){return new Promise((n,s)=>{const u=new FileReader;u.onload=()=>n(u.result),u.onerror=()=>s(new Error(`Could not read file "${r.name}"`)),u.readAsText(r,"utf-8")})}function g_(r){const n=r.name.toLowerCase().endsWith(".json"),s=r.type==="application/json"||r.type==="text/plain";return!n&&!s?`"${r.name}" does not appear to be a JSON file. Only .json files are accepted.`:null}function v_({uploadPath:r,onSuccess:n,onClose:s,onError:u}){const[d,f]=x.useState(""),[b,y]=x.useState(null),[m,g]=x.useState(null),[N,v]=x.useState(!1),T=x.useRef(null),A=x.useRef(null),E=x.useRef(null),S=po(d).isJSON,O=S&&d.trim()!=="",{isUploading:R,upload:G,cancel:L}=h_({uploadPath:r,json:d,onSuccess:n,onError:D=>{y(D),u(D)}});x.useEffect(()=>{var V;const D=T.current;if(D)return D.open||D.showModal(),(V=A.current)==null||V.focus(),()=>{D.open&&D.close()}},[]);const Y=x.useCallback(()=>{L(),s()},[L,s]),q=x.useCallback(D=>{D.target===T.current&&Y()},[Y]),B=x.useCallback(D=>{D.preventDefault(),Y()},[Y]),J=x.useCallback(()=>{y(null),G()},[G]),I=x.useCallback(D=>{D.key==="Enter"&&(D.ctrlKey||D.metaKey)&&(D.preventDefault(),O&&!R&&J())},[O,R,J]),K=x.useCallback(()=>{S&&f(rc(d))},[S,d]),$=x.useCallback(async D=>{var P;g(null),y(null);const V=g_(D);if(V){g(V);return}try{const ee=await y_(D);if(!po(ee).isJSON){g(`"${D.name}" contains invalid JSON.`);return}f(rc(ee)),(P=A.current)==null||P.focus()}catch(ee){g(ee.message)}},[]),ne=x.useCallback(D=>{D.preventDefault(),D.stopPropagation(),N||v(!0)},[N]),ue=x.useCallback(D=>{D.preventDefault(),D.stopPropagation(),(D.currentTarget===D.target||!D.currentTarget.contains(D.relatedTarget))&&v(!1)},[]),ce=x.useCallback(D=>{D.preventDefault(),D.stopPropagation(),v(!1);const V=D.dataTransfer.files[0];V&&$(V)},[$]),ie=x.useCallback(D=>{var P;const V=(P=D.target.files)==null?void 0:P[0];V&&($(V),D.target.value="")},[$]),M=!S&&d.trim()!=="";return h.jsx("dialog",{ref:T,className:Qe.dialog,"aria-modal":"true","aria-labelledby":"mock-upload-modal-title",onClick:q,onCancel:B,children:h.jsxs("div",{className:Qe.modalInner,onClick:D=>D.stopPropagation(),children:[h.jsxs("div",{className:Qe.modalHeader,children:[h.jsxs("div",{className:Qe.modalTitleGroup,children:[h.jsx("span",{id:"mock-upload-modal-title",className:Qe.modalTitle,children:"⬆️ Upload Mock Data"}),h.jsx("span",{className:Qe.modalPath,children:r})]}),h.jsx("button",{className:Qe.closeButton,onClick:Y,"aria-label":"Close upload modal",title:"Close",disabled:R,children:"✕"})]}),h.jsxs("div",{className:Qe.modalBody,children:[h.jsxs("div",{className:`${Qe.dropZone} ${N?Qe.dropZoneActive:""}`,onDragOver:ne,onDragLeave:ue,onDrop:ce,"aria-label":"Drop a JSON file here",children:[h.jsx("span",{className:Qe.dropZoneIcon,children:"📂"}),h.jsxs("span",{className:Qe.dropZoneText,children:["Drop a ",h.jsx("code",{children:".json"})," file here"]}),h.jsx("span",{className:Qe.dropZoneOr,children:"— or —"}),h.jsx("input",{ref:E,type:"file",accept:".json,application/json",className:Qe.fileInputHidden,"aria-hidden":"true",tabIndex:-1,onChange:ie}),h.jsx("button",{type:"button",className:Qe.browseButton,onClick:()=>{var D;return(D=E.current)==null?void 0:D.click()},disabled:R,"aria-label":"Browse for a JSON file",children:"Browse file…"})]}),m&&h.jsxs("span",{className:Qe.fileError,role:"alert",children:["⚠️ ",m]}),h.jsx("label",{htmlFor:"mock-upload-textarea",className:Qe.textareaLabel,children:"JSON Payload"}),h.jsx("textarea",{id:"mock-upload-textarea",ref:A,className:Qe.textarea,value:d,onChange:D=>{f(D.target.value),g(null)},onKeyDown:I,placeholder:"Paste JSON here, or drop / browse a .json file above",rows:10,spellCheck:!1,"aria-describedby":M?"mock-upload-validation":void 0}),M&&h.jsx("span",{id:"mock-upload-validation",className:Qe.validationError,role:"status",children:"⚠️ Invalid JSON — check syntax"}),h.jsx("span",{className:Qe.keyboardHint,children:m_?"⌘+Enter to upload":"Ctrl+Enter to upload"}),b&&h.jsxs("div",{className:Qe.errorBanner,role:"alert",children:["❌ Upload failed: ",b]})]}),h.jsxs("div",{className:Qe.modalFooter,children:[h.jsx("button",{className:Qe.formatButton,onClick:K,disabled:!S||R,title:"Format JSON","aria-label":"Format JSON",children:"Format"}),h.jsxs("div",{className:Qe.footerActions,children:[h.jsx("button",{className:Qe.cancelButton,onClick:Y,disabled:R,children:"Cancel"}),h.jsx("button",{className:Qe.uploadButton,onClick:J,disabled:!O||R,"aria-busy":R,children:R?h.jsxs(h.Fragment,{children:[h.jsx("span",{className:Qe.spinner,"aria-hidden":"true"})," Uploading…"]}):"Upload ▶"})]})]})]})})}const pc=(r,n)=>n.some(s=>r instanceof s);let Rf,Bf;function b_(){return Rf||(Rf=[IDBDatabase,IDBObjectStore,IDBIndex,IDBCursor,IDBTransaction])}function __(){return Bf||(Bf=[IDBCursor.prototype.advance,IDBCursor.prototype.continue,IDBCursor.prototype.continuePrimaryKey])}const fc=new WeakMap,oc=new WeakMap,zi=new WeakMap;function S_(r){const n=new Promise((s,u)=>{const d=()=>{r.removeEventListener("success",f),r.removeEventListener("error",b)},f=()=>{s(ia(r.result)),d()},b=()=>{u(r.error),d()};r.addEventListener("success",f),r.addEventListener("error",b)});return zi.set(n,r),n}function x_(r){if(fc.has(r))return;const n=new Promise((s,u)=>{const d=()=>{r.removeEventListener("complete",f),r.removeEventListener("error",b),r.removeEventListener("abort",b)},f=()=>{s(),d()},b=()=>{u(r.error||new DOMException("AbortError","AbortError")),d()};r.addEventListener("complete",f),r.addEventListener("error",b),r.addEventListener("abort",b)});fc.set(r,n)}let hc={get(r,n,s){if(r instanceof IDBTransaction){if(n==="done")return fc.get(r);if(n==="store")return s.objectStoreNames[1]?void 0:s.objectStore(s.objectStoreNames[0])}return ia(r[n])},set(r,n,s){return r[n]=s,!0},has(r,n){return r instanceof IDBTransaction&&(n==="done"||n==="store")?!0:n in r}};function oh(r){hc=r(hc)}function T_(r){return __().includes(r)?function(...n){return r.apply(mc(this),n),ia(this.request)}:function(...n){return ia(r.apply(mc(this),n))}}function w_(r){return typeof r=="function"?T_(r):(r instanceof IDBTransaction&&x_(r),pc(r,b_())?new Proxy(r,hc):r)}function ia(r){if(r instanceof IDBRequest)return S_(r);if(oc.has(r))return oc.get(r);const n=w_(r);return n!==r&&(oc.set(r,n),zi.set(n,r)),n}const mc=r=>zi.get(r);function E_(r,n,{blocked:s,upgrade:u,blocking:d,terminated:f}={}){const b=indexedDB.open(r,n),y=ia(b);return u&&b.addEventListener("upgradeneeded",m=>{u(ia(b.result),m.oldVersion,m.newVersion,ia(b.transaction),m)}),s&&b.addEventListener("blocked",m=>s(m.oldVersion,m.newVersion,m)),y.then(m=>{f&&m.addEventListener("close",()=>f()),d&&m.addEventListener("versionchange",g=>d(g.oldVersion,g.newVersion,g))}).catch(()=>{}),y}function N_(r,{blocked:n}={}){const s=indexedDB.deleteDatabase(r);return n&&s.addEventListener("blocked",u=>n(u.oldVersion,u)),ia(s).then(()=>{})}const A_=["get","getKey","getAll","getAllKeys","count"],C_=["put","add","delete","clear"],ic=new Map;function Uf(r,n){if(!(r instanceof IDBDatabase&&!(n in r)&&typeof n=="string"))return;if(ic.get(n))return ic.get(n);const s=n.replace(/FromIndex$/,""),u=n!==s,d=C_.includes(s);if(!(s in(u?IDBIndex:IDBObjectStore).prototype)||!(d||A_.includes(s)))return;const f=async function(b,...y){const m=this.transaction(b,d?"readwrite":"readonly");let g=m.store;return u&&(g=g.index(y.shift())),(await Promise.all([g[s](...y),d&&m.done]))[0]};return ic.set(n,f),f}oh(r=>({...r,get:(n,s,u)=>Uf(n,s)||r.get(n,s,u),has:(n,s)=>!!Uf(n,s)||r.has(n,s)}));const j_=["continue","continuePrimaryKey","advance"],Gf={},yc=new WeakMap,ih=new WeakMap,M_={get(r,n){if(!j_.includes(n))return r[n];let s=Gf[n];return s||(s=Gf[n]=function(...u){yc.set(this,ih.get(this)[n](...u))}),s}};async function*D_(...r){let n=this;if(n instanceof IDBCursor||(n=await n.openCursor(...r)),!n)return;n=n;const s=new Proxy(n,M_);for(ih.set(s,n),zi.set(s,mc(n));n;)yield s,n=await(yc.get(s)||n.continue()),yc.delete(s)}function Hf(r,n){return n===Symbol.asyncIterator&&pc(r,[IDBIndex,IDBObjectStore,IDBCursor])||n==="iterate"&&pc(r,[IDBIndex,IDBObjectStore])}oh(r=>({...r,get(n,s,u){return Hf(n,s)?D_:r.get(n,s,u)},has(n,s){return Hf(n,s)||r.has(n,s)}}));const sh="minigraph-clipboard",O_=1,zn="items";let Di=null;function Lf(){return E_(sh,O_,{upgrade(r){r.objectStoreNames.contains(zn)&&r.deleteObjectStore(zn);const n=r.createObjectStore(zn,{keyPath:"id"});n.createIndex("by-alias","node.alias",{unique:!0}),n.createIndex("by-clippedAt","clippedAt")}})}function ml(){return Di||(Di=Lf().catch(async r=>(console.warn("[clipboard/db] openDB failed, deleting and recreating:",r),Di=null,await N_(sh),Lf()))),Di}async function z_(){return(await(await ml()).getAllFromIndex(zn,"by-clippedAt")).reverse()}async function qf(r){return(await ml()).getFromIndex(zn,"by-alias",r)}async function k_(r){await(await ml()).add(zn,r)}async function R_(r,n){const u=(await ml()).transaction(zn,"readwrite");await u.store.delete(r),await u.store.add(n),await u.done}async function B_(r){await(await ml()).delete(zn,r)}async function U_(){await(await ml()).clear(zn)}const G_="minigraph-clipboard-sync";function H_(){return new BroadcastChannel(G_)}function L_(r,n){switch(n.type){case"HYDRATE":return{items:n.items,isLoading:!1};case"ITEM_ADDED":return{...r,items:[n.item,...r.items]};case"ITEM_REPLACED":{const s=r.items.filter(u=>u.id!==n.previousId);return{...r,items:[n.item,...s]}}case"ITEM_REMOVED":return{...r,items:r.items.filter(s=>s.id!==n.id)};case"ITEMS_CLEARED":return{...r,items:[]};default:return r}}const rh=x.createContext(null);function q_({children:r}){const[n,s]=x.useReducer(L_,{items:[],isLoading:!0}),u=x.useRef(null);x.useEffect(()=>{z_().then(g=>s({type:"HYDRATE",items:g}))},[]),x.useEffect(()=>{let g;try{g=H_()}catch{return}return u.current=g,g.onmessage=N=>{const v=N.data;switch(v.type){case"item-added":s({type:"ITEM_ADDED",item:v.item});break;case"item-replaced":s({type:"ITEM_REPLACED",item:v.item,previousId:v.previousId});break;case"item-removed":s({type:"ITEM_REMOVED",id:v.id});break;case"items-cleared":s({type:"ITEMS_CLEARED"});break}},()=>{g.close(),u.current=null}},[]);const d=x.useCallback(g=>{var N;(N=u.current)==null||N.postMessage(g)},[]),f=x.useCallback(async(g,N,v)=>{try{const T={id:crypto.randomUUID(),clippedAt:new Date().toISOString(),sourceWsPath:v.sourceWsPath,sourceLabel:v.sourceLabel,node:g,connections:N},A=await qf(g.alias);if(A)return{status:"duplicate",existingItem:A,pendingItem:T};try{await k_(T)}catch(E){if(E instanceof DOMException&&E.name==="ConstraintError"){const C=await qf(g.alias);if(C)return{status:"duplicate",existingItem:C,pendingItem:T}}throw E}return s({type:"ITEM_ADDED",item:T}),d({type:"item-added",item:T}),{status:"added"}}catch(T){return{status:"error",message:T instanceof Error?T.message:String(T)}}},[d]),b=x.useCallback(async(g,N)=>{await R_(N,g),s({type:"ITEM_REPLACED",item:g,previousId:N}),d({type:"item-replaced",item:g,previousId:N})},[d]),y=x.useCallback(async g=>{await B_(g),s({type:"ITEM_REMOVED",id:g}),d({type:"item-removed",id:g})},[d]),m=x.useCallback(async()=>{await U_(),s({type:"ITEMS_CLEARED"}),d({type:"items-cleared"})},[d]);return h.jsx(rh.Provider,{value:{items:n.items,isLoading:n.isLoading,clipNode:f,confirmReplace:b,removeItem:y,clearAll:m},children:r})}function ch(){const r=x.useContext(rh);if(!r)throw new Error("useClipboardContext must be used inside <ClipboardProvider>");return r}function uh(r){const n=Date.now(),s=new Date(r).getTime(),u=n-s;if(u<0)return"just now";const d=Math.floor(u/1e3);if(d<60)return"just now";const f=Math.floor(d/60);if(f<60)return`${f} min ago`;const b=Math.floor(f/60);if(b<24)return`${b} hour${b>1?"s":""} ago`;const y=Math.floor(b/24);return y===1?"yesterday":y<30?`${y} days ago`:new Date(r).toLocaleDateString()}const Y_="_card_474bo_2",X_="_alias_474bo_10",J_="_meta_474bo_18",Z_="_propsLine_474bo_24",Q_="_timestamp_474bo_32",V_="_actions_474bo_39",I_="_pasteBtn_474bo_45",K_="_inspectBtn_474bo_46",$_="_removeBtn_474bo_47",Ft={card:Y_,alias:X_,meta:J_,propsLine:Z_,timestamp:Q_,actions:V_,pasteBtn:I_,inspectBtn:K_,removeBtn:$_};function W_(r){const n=Se.c(55),{item:s,connected:u,onPaste:d,onRemove:f,onInspect:b}=r,{node:y,connections:m,clippedAt:g,sourceLabel:N}=s,v=y.types[0]??"—",T=y.properties.skill??"—";let A;n[0]!==y.properties?(A=Object.entries(y.properties).filter(F_).map(P_),n[0]=y.properties,n[1]=A):A=n[1];const E=A,C=E.length>0?E.join(", "):"—";let S;if(n[2]!==m||n[3]!==y.alias){let F;n[5]!==y.alias?(F=me=>me.source===y.alias,n[5]=y.alias,n[6]=F):F=n[6],S=m.filter(F),n[2]=m,n[3]=y.alias,n[4]=S}else S=n[4];const O=S.length;let R;if(n[7]!==m||n[8]!==y.alias){let F;n[10]!==y.alias?(F=me=>me.target===y.alias,n[10]=y.alias,n[11]=F):F=n[11],R=m.filter(F),n[7]=m,n[8]=y.alias,n[9]=R}else R=n[9];const G=R.length,L=`${m.length} (${O} out, ${G} in)`;let Y;n[12]!==y.alias?(Y=h.jsx("div",{className:Ft.alias,children:y.alias}),n[12]=y.alias,n[13]=Y):Y=n[13];let q;n[14]!==v?(q=h.jsxs("div",{className:Ft.meta,children:["Type: ",v]}),n[14]=v,n[15]=q):q=n[15];let B;n[16]!==T?(B=h.jsxs("div",{className:Ft.meta,children:["Skill: ",T]}),n[16]=T,n[17]=B):B=n[17];let J;n[18]!==C?(J=h.jsxs("span",{className:Ft.propsLine,children:["Props: ",C]}),n[18]=C,n[19]=J):J=n[19];let I;n[20]!==C||n[21]!==J?(I=h.jsx("div",{className:Ft.meta,title:C,children:J}),n[20]=C,n[21]=J,n[22]=I):I=n[22];let K;n[23]!==L?(K=h.jsxs("div",{className:Ft.meta,children:["Connections: ",L]}),n[23]=L,n[24]=K):K=n[24];let $;n[25]!==g?($=uh(g),n[25]=g,n[26]=$):$=n[26];let ne;n[27]!==N||n[28]!==$?(ne=h.jsxs("div",{className:Ft.timestamp,children:["Clipped ",$," from ",N]}),n[27]=N,n[28]=$,n[29]=ne):ne=n[29];let ue;n[30]!==s||n[31]!==d?(ue=()=>d(s),n[30]=s,n[31]=d,n[32]=ue):ue=n[32];const ce=!u,ie=`Paste node ${y.alias}`;let M;n[33]!==ue||n[34]!==ce||n[35]!==ie?(M=h.jsx("button",{className:Ft.pasteBtn,onClick:ue,disabled:ce,"aria-label":ie,children:"Paste"}),n[33]=ue,n[34]=ce,n[35]=ie,n[36]=M):M=n[36];const D=`Inspect node ${y.alias}`;let V;n[37]!==b||n[38]!==D?(V=h.jsx("button",{className:Ft.inspectBtn,onClick:b,"aria-label":D,children:"Describe"}),n[37]=b,n[38]=D,n[39]=V):V=n[39];const P=`Remove node ${y.alias} from clipboard`;let ee;n[40]!==f||n[41]!==P?(ee=h.jsx("button",{className:Ft.removeBtn,onClick:f,"aria-label":P,children:"Remove"}),n[40]=f,n[41]=P,n[42]=ee):ee=n[42];let re;n[43]!==M||n[44]!==V||n[45]!==ee?(re=h.jsxs("div",{className:Ft.actions,children:[M,V,ee]}),n[43]=M,n[44]=V,n[45]=ee,n[46]=re):re=n[46];let te;return n[47]!==ne||n[48]!==re||n[49]!==Y||n[50]!==q||n[51]!==B||n[52]!==I||n[53]!==K?(te=h.jsxs("div",{className:Ft.card,children:[Y,q,B,I,K,ne,re]}),n[47]=ne,n[48]=re,n[49]=Y,n[50]=q,n[51]=B,n[52]=I,n[53]=K,n[54]=te):te=n[54],te}function P_(r){const[n,s]=r,u=typeof s=="string"?s:JSON.stringify(s);return`${n}=${u&&u.length>30?u.slice(0,30)+"…":u}`}function F_(r){const[n]=r;return n!=="skill"}const e3="_sidebar_ol0sc_2",t3="_header_ol0sc_12",n3="_headerTitle_ol0sc_22",a3="_clearBtn_ol0sc_29",l3="_itemList_ol0sc_45",o3="_loading_ol0sc_55",i3="_emptyState_ol0sc_65",s3="_emptyIcon_ol0sc_78",r3="_emptyTitle_ol0sc_83",c3="_emptyHint_ol0sc_87",u3="_inspectPanel_ol0sc_93",d3="_inspectHeader_ol0sc_101",p3="_inspectClose_ol0sc_115",f3="_inspectBody_ol0sc_129",h3="_dialog_ol0sc_135",m3="_dialogTitle_ol0sc_150",y3="_dialogBody_ol0sc_157",g3="_dialogActions_ol0sc_164",v3="_cancelBtn_ol0sc_171",b3="_replaceBtn_ol0sc_185",lt={sidebar:e3,header:t3,headerTitle:n3,clearBtn:a3,itemList:l3,loading:o3,emptyState:i3,emptyIcon:s3,emptyTitle:r3,emptyHint:c3,inspectPanel:u3,inspectHeader:d3,inspectClose:p3,inspectBody:f3,dialog:h3,dialogTitle:m3,dialogBody:y3,dialogActions:g3,cancelBtn:v3,replaceBtn:b3};function _3(){const r=Se.c(1);let n;return r[0]===Symbol.for("react.memo_cache_sentinel")?(n=h.jsxs("div",{className:lt.emptyState,children:[h.jsx("span",{className:lt.emptyIcon,children:"📋"}),h.jsx("span",{className:lt.emptyTitle,children:"No items clipped yet."}),h.jsx("span",{className:lt.emptyHint,children:"Right-click a node in the Graph view to get started."})]}),r[0]=n):n=r[0],n}function S3(r){const n=Se.c(18),{connected:s,onPaste:u}=r,d=ch(),[f,b]=x.useState(null);let y;n[0]===Symbol.for("react.memo_cache_sentinel")?(y=h.jsx("span",{className:lt.headerTitle,children:"Clipboard"}),n[0]=y):y=n[0];let m;n[1]!==d?(m=d.items.length>0&&h.jsx("button",{className:lt.clearBtn,onClick:()=>d.clearAll(),"aria-label":"Clear all clipboard items",children:"Clear"}),n[1]=d,n[2]=m):m=n[2];let g;n[3]!==m?(g=h.jsxs("div",{className:lt.header,children:[y,m]}),n[3]=m,n[4]=g):g=n[4];let N;n[5]!==d||n[6]!==s||n[7]!==(f==null?void 0:f.id)||n[8]!==u?(N=d.isLoading?h.jsx("div",{className:lt.loading,children:"Loading…"}):d.items.length===0?h.jsx(_3,{}):d.items.map(E=>h.jsx(W_,{item:E,connected:s,onPaste:u,onRemove:()=>d.removeItem(E.id),onInspect:()=>b((f==null?void 0:f.id)===E.id?null:E)},E.id)),n[5]=d,n[6]=s,n[7]=f==null?void 0:f.id,n[8]=u,n[9]=N):N=n[9];let v;n[10]!==N?(v=h.jsx("div",{className:lt.itemList,children:N}),n[10]=N,n[11]=v):v=n[11];let T;n[12]!==f?(T=f&&h.jsxs("div",{className:lt.inspectPanel,children:[h.jsxs("div",{className:lt.inspectHeader,children:[h.jsxs("span",{children:["Describe node ",f.node.alias]}),h.jsx("button",{className:lt.inspectClose,onClick:()=>b(null),"aria-label":"Close inspect panel",children:"✕"})]}),h.jsx("div",{className:lt.inspectBody,children:h.jsx(vc,{data:{node:f.node,connections:f.connections},style:uo})})]}),n[12]=f,n[13]=T):T=n[13];let A;return n[14]!==g||n[15]!==v||n[16]!==T?(A=h.jsxs("div",{className:lt.sidebar,children:[g,v,T]}),n[14]=g,n[15]=v,n[16]=T,n[17]=A):A=n[17],A}const Yf=120,Xf=18,x3=180,T3=650;function w3(r){const{wheelTargetRef:n,scrollRef:s,contentWrapperRef:u,currentIndex:d,totalPages:f,onNavigatePrev:b,onNavigateNext:y}=r,m=x.useRef(0),g=x.useRef(null),N=x.useRef(!1),v=x.useRef(null),T=x.useRef(b),A=x.useRef(y),E=x.useRef(d),C=x.useRef(f);x.useEffect(()=>{T.current=b}),x.useEffect(()=>{A.current=y}),x.useEffect(()=>{E.current=d}),x.useEffect(()=>{C.current=f}),x.useEffect(()=>{v.current!==null&&(clearTimeout(v.current),v.current=null),u.current&&(u.current.style.transition="none",u.current.style.transform="translateY(0)"),m.current=0,g.current=null},[d]),x.useEffect(()=>{const S=n.current;if(!S)return;function O(){m.current=0,g.current=null,u.current&&(u.current.style.transition="transform 0.28s cubic-bezier(0.25, 0.46, 0.45, 0.94)",u.current.style.transform="translateY(0)")}function R(G){if(G.deltaY===0)return;const L=s.current;if(!L)return;const Y=L.scrollTop<=0,q=L.scrollTop+L.clientHeight>=L.scrollHeight-1,B=G.deltaY<0,J=G.deltaY>0,I=Y&&B,K=q&&J;if(!I&&!K){O();return}if(N.current)return;const $=E.current,ne=C.current;if(I&&$===0||K&&$===ne-1)return;const ue=I?"prev":"next";if(g.current!==null&&g.current!==ue&&O(),g.current=ue,m.current+=Math.abs(G.deltaY),u.current){const ce=ue==="prev"?-1:1,ie=m.current*(Xf/Yf),M=Math.min(ie,Xf)*ce;u.current.style.transition="none",u.current.style.transform=`translateY(${M}px)`}if(v.current!==null&&clearTimeout(v.current),v.current=setTimeout(O,x3),m.current>=Yf){v.current!==null&&clearTimeout(v.current);const ce=g.current;O(),N.current=!0,ce==="prev"?T.current():A.current(),setTimeout(()=>{N.current=!1},T3)}}return S.addEventListener("wheel",R,{passive:!0}),()=>{v.current!==null&&clearTimeout(v.current),S.removeEventListener("wheel",R)}},[])}const E3="_helpRoot_ss4ov_2",N3="_categoryNav_ss4ov_11",A3="_categoryTabScroller_ss4ov_21",C3="_categoryTab_ss4ov_21",j3="_categoryTabActive_ss4ov_71",M3="_maximizeButton_ss4ov_78",D3="_closeButton_ss4ov_100",O3="_helpBody_ss4ov_122",z3="_emptyFallback_ss4ov_130",k3="_helpContent_ss4ov_147",R3="_topicLink_ss4ov_226",B3="_helpBodyContent_ss4ov_271",U3="_chipStrip_ss4ov_276",G3="_topicChip_ss4ov_290",H3="_topicChipActive_ss4ov_318",wt={helpRoot:E3,categoryNav:N3,categoryTabScroller:A3,categoryTab:C3,categoryTabActive:j3,maximizeButton:M3,closeButton:D3,helpBody:O3,emptyFallback:z3,helpContent:k3,topicLink:R3,helpBodyContent:B3,chipStrip:U3,topicChip:G3,topicChipActive:H3};function gc(r){return typeof r=="string"?r:typeof r=="number"?String(r):Array.isArray(r)?r.map(gc).join(""):Vf.isValidElement(r)?gc(r.props.children):""}function L3(r){if(!r.trim().toLowerCase().startsWith("help "))return null;const u=r.trim().slice(5).replace(/\s*\(.*\)\s*$/,"").trim().toLowerCase();return u.length>0?u:null}function q3(r){const n=Se.c(49),{activeTopic:s,onNavigate:u,onClose:d,onToggleMaximize:f,isMaximized:b}=r,y=x.useRef(null),m=x.useRef(null),g=x.useRef(null),N=x.useRef(null);let v;n[0]===Symbol.for("react.memo_cache_sentinel")?(v=()=>{y.current&&(y.current.scrollTop=0)},n[0]=v):v=n[0];let T;n[1]!==s?(T=[s],n[1]=s,n[2]=T):T=n[2],x.useEffect(v,T);let A;n[3]===Symbol.for("react.memo_cache_sentinel")?(A=()=>{const te=N.current;if(!te)return;const F=te.querySelector('[aria-current="step"]');F&&F.scrollIntoView({block:"nearest",inline:"nearest",behavior:"smooth"})},n[3]=A):A=n[3];let E;n[4]!==s?(E=[s],n[4]=s,n[5]=E):E=n[5],x.useEffect(A,E);let C;n[6]!==s?(C=th(s),n[6]=s,n[7]=C):C=n[7];const S=C;let O;n[8]!==S?(O=cc(S),n[8]=S,n[9]=O):O=n[9];const R=O,G=R.length,L=hl.indexOf(s),Y=L<0?0:L,q=hl.length;let B,J;n[10]!==u||n[11]!==Y?(B=()=>u(hl[Y-1]??""),J=()=>u(hl[Y+1]??hl[hl.length-1]),n[10]=u,n[11]=Y,n[12]=B,n[13]=J):(B=n[12],J=n[13]);let I;n[14]!==Y||n[15]!==B||n[16]!==J?(I={wheelTargetRef:m,scrollRef:y,contentWrapperRef:g,currentIndex:Y,totalPages:q,onNavigatePrev:B,onNavigateNext:J},n[14]=Y,n[15]=B,n[16]=J,n[17]=I):I=n[17],w3(I);let K;n[18]!==s?(K=Oi(s),n[18]=s,n[19]=K):K=n[19];const $=K;let ne;n[20]!==u?(ne=te=>{const{children:F,...me}=te,_e=gc(F).trim(),xe=L3(_e);return xe!==null&&Oi(xe)!==null?h.jsx("li",{...me,children:h.jsx("button",{className:wt.topicLink,"aria-label":`Open help topic: ${xe}`,onClick:()=>u(xe),children:F})}):h.jsx("li",{...me,children:F})},n[20]=u,n[21]=ne):ne=n[21];const ue=ne;let ce;n[22]!==S||n[23]!==u?(ce=eh.map(te=>h.jsx("button",{className:[wt.categoryTab,te.id===S?wt.categoryTabActive:""].join(" ").trim(),"aria-current":te.id===S?"true":void 0,onClick:()=>{const F=cc(te.id);u(F[0]??"")},children:te.label},te.id)),n[22]=S,n[23]=u,n[24]=ce):ce=n[24];let ie;n[25]!==ce?(ie=h.jsx("div",{className:wt.categoryTabScroller,children:ce}),n[25]=ce,n[26]=ie):ie=n[26];let M;n[27]!==b||n[28]!==f?(M=f&&h.jsx("button",{className:wt.maximizeButton,onClick:f,"aria-label":b?"Restore help panel":"Maximize help panel",children:b?"⊞":"⛶"}),n[27]=b,n[28]=f,n[29]=M):M=n[29];let D;n[30]!==d?(D=d&&h.jsx("button",{className:wt.closeButton,onClick:d,"aria-label":"Close help panel",children:"×"}),n[30]=d,n[31]=D):D=n[31];let V;n[32]!==ie||n[33]!==M||n[34]!==D?(V=h.jsxs("nav",{className:wt.categoryNav,"aria-label":"Help categories",children:[ie,M,D]}),n[32]=ie,n[33]=M,n[34]=D,n[35]=V):V=n[35];let P;n[36]!==s||n[37]!==R||n[38]!==G||n[39]!==u?(P=G>1&&h.jsx("div",{className:wt.chipStrip,ref:N,children:R.map(te=>{const F=te===s,me=te===""?"Overview":te;return h.jsx("button",{className:[wt.topicChip,F?wt.topicChipActive:""].join(" ").trim(),"aria-current":F?"step":void 0,onClick:()=>u(te),children:me},te)})}),n[36]=s,n[37]=R,n[38]=G,n[39]=u,n[40]=P):P=n[40];let ee;n[41]!==s||n[42]!==$||n[43]!==ue?(ee=h.jsx("div",{className:wt.helpBody,ref:y,children:h.jsx("div",{className:wt.helpBodyContent,ref:g,children:$===null?h.jsxs("div",{className:wt.emptyFallback,children:[h.jsxs("code",{children:["help ",s||""]}),"  not found in the local bundle."]}):h.jsx("div",{className:wt.helpContent,children:h.jsx(qy,{remarkPlugins:[Yy],components:s===""?{li:ue}:void 0,children:$})})})}),n[41]=s,n[42]=$,n[43]=ue,n[44]=ee):ee=n[44];let re;return n[45]!==V||n[46]!==P||n[47]!==ee?(re=h.jsxs("div",{className:wt.helpRoot,role:"region","aria-label":"Help browser",ref:m,children:[V,P,ee]}),n[45]=V,n[46]=P,n[47]=ee,n[48]=re):re=n[48],re}function Y3(r){const n=Se.c(22),{existingItem:s,pendingItem:u,onReplace:d,onCancel:f}=r,b=x.useRef(null);let y,m;n[0]===Symbol.for("react.memo_cache_sentinel")?(y=()=>{const R=b.current;R&&!R.open&&R.showModal()},m=[],n[0]=y,n[1]=m):(y=n[0],m=n[1]),x.useEffect(y,m);let g;n[2]===Symbol.for("react.memo_cache_sentinel")?(g=h.jsx("h2",{id:"duplicate-dialog-title",className:lt.dialogTitle,children:"Duplicate Node"}),n[2]=g):g=n[2];let N;n[3]!==u.node.alias?(N=h.jsxs("strong",{children:['"',u.node.alias,'"']}),n[3]=u.node.alias,n[4]=N):N=n[4];let v;n[5]!==s.clippedAt?(v=uh(s.clippedAt),n[5]=s.clippedAt,n[6]=v):v=n[6];let T;n[7]!==N||n[8]!==v?(T=h.jsxs("p",{className:lt.dialogBody,children:["A clipboard item with alias ",N," already exists (clipped ",v,")."]}),n[7]=N,n[8]=v,n[9]=T):T=n[9];let A;n[10]===Symbol.for("react.memo_cache_sentinel")?(A=h.jsx("p",{className:lt.dialogBody,children:"Replace it with the new snapshot?"}),n[10]=A):A=n[10];let E;n[11]!==f?(E=h.jsx("button",{className:lt.cancelBtn,onClick:f,children:"Cancel"}),n[11]=f,n[12]=E):E=n[12];let C;n[13]!==d?(C=h.jsx("button",{className:lt.replaceBtn,onClick:d,children:"Replace"}),n[13]=d,n[14]=C):C=n[14];let S;n[15]!==E||n[16]!==C?(S=h.jsxs("div",{className:lt.dialogActions,children:[E,C]}),n[15]=E,n[16]=C,n[17]=S):S=n[17];let O;return n[18]!==f||n[19]!==S||n[20]!==T?(O=h.jsxs("dialog",{ref:b,className:lt.dialog,onClose:f,"aria-labelledby":"duplicate-dialog-title",children:[g,T,A,S]}),n[18]=f,n[19]=S,n[20]=T,n[21]=O):O=n[21],O}function X3(r,n){if(!n)return null;const s=r.trim().toLowerCase();if(s!=="help"&&!s.startsWith("help "))return null;const u=nh(r);return Oi(u)!==null?u:null}class J3{constructor(){this.listeners=new Map}on(n,s){const u=n;return this.listeners.has(u)||this.listeners.set(u,new Set),this.listeners.get(u).add(s),()=>{var d;(d=this.listeners.get(u))==null||d.delete(s)}}emit(n){const s=this.listeners.get(n.kind);s&&s.forEach(u=>{try{u(n)}catch(d){console.error(`[ProtocolBus] listener for '${n.kind}' threw:`,d)}})}clear(){this.listeners.clear()}}const Z3=new Set(["info","error","ping","welcome"]);function Q3(r,n){const s=[],u={msgId:r,raw:n};let d=!1,f=!1,b=!1,y=!1,m=!1;const g=po(n);if(g.isJSON){const S=g.data;if(typeof S.type=="string"){const O=S.type;return s.push({...u,kind:"lifecycle",type:O,knownType:Z3.has(O),message:typeof S.message=="string"?S.message:n,time:S.time??null}),s.length>0?s:[{...u,kind:"unclassified"}]}return s.push({...u,kind:"json.response",data:g.data}),s.length>0?s:[{...u,kind:"unclassified"}]}const N=xg(n);N&&(m=!0,s.push({...u,kind:"payload.large",apiPath:N.apiPath,byteSize:N.byteSize,filename:N.filename}));const v=Tg(n);v&&(b=!0,s.push({...u,kind:"upload.invitation",uploadPath:v}));const T=Pf(n);if(T&&(y=!0,s.push({...u,kind:"upload.contentPath",uploadPath:T})),Wf(n)){f=!0;const S=Sc(n);S&&s.push({...u,kind:"graph.link",apiPath:S})}if(f){const S=_g(n);S&&s.push({...u,kind:"graph.exported",graphName:S.graphName,apiPath:S.apiPath})}const A=Ng(n);A&&s.push({...u,kind:"graph.mutation",mutationType:A}),n.startsWith("> ")&&(d=!0,s.push({...u,kind:"command.echo",commandText:n.slice(2)})),wg(n)&&s.push({...u,kind:"command.helpOrDescribe",commandText:n.slice(2)});const E=Eg(n);E&&s.push({...u,kind:"command.importGraph",graphName:E});const C=Sg(n);return C&&s.push({...u,kind:"graph.export.failed",reason:C.reason}),!d&&!f&&!b&&!y&&!m&&_c(n)&&s.push({...u,kind:"docs.response",isMarkdown:!0}),s.length===0&&s.push({...u,kind:"unclassified"}),s}function V3(r){const n=Se.c(12),{messages:s,bus:u}=r,d=x.useRef(-1);let f;n[0]!==s?(f=()=>{s.length>0&&(d.current=s[s.length-1].id)},n[0]=s,n[1]=f):f=n[1];let b;n[2]===Symbol.for("react.memo_cache_sentinel")?(b=[],n[2]=b):b=n[2],x.useEffect(f,b);let y;if(n[3]!==s){y=new Map;for(const T of s)y.set(T.id,Q3(T.id,T.raw));n[3]=s,n[4]=y}else y=n[4];const m=y;let g,N;n[5]!==u||n[6]!==m||n[7]!==s?(g=()=>{if(s.length===0)return;const T=s.filter(A=>A.id>d.current);if(T.length!==0){d.current=s[s.length-1].id;for(const A of T){const E=m.get(A.id);if(E)for(const C of E)u.emit(C)}}},N=[s,u,m],n[5]=u,n[6]=m,n[7]=s,n[8]=g,n[9]=N):(g=n[8],N=n[9]),x.useEffect(g,N);let v;return n[10]!==m?(v={classificationMap:m},n[10]=m,n[11]=v):v=n[11],v}function I3({config:r}){const{title:n,wsPath:s,storageKeyPayload:u,storageKeyHistory:d,storageKeyTab:f,storageKeySavedGraphs:b,supportsUpload:y,supportsClipboard:m,supportsHelp:g,tabs:N}=r,v=Ay(),[T,A]=oa(u,""),E=bc(),[C,S]=x.useState(()=>E.peekPendingPayload(s)),{takePendingPayload:O}=E;x.useEffect(()=>{const je=O(s);je!==null&&S(je)},[O,s]);const R=C??T,G=x.useCallback(je=>{S(null),A(je)},[A]),L=x.useMemo(()=>R?dg(R):{valid:!0,error:null,type:null},[R]),{toasts:Y,addToast:q,removeToast:B}=pg(),I=x.useRef(new J3).current,K=x.useCallback(je=>X3(je,g===!0)!==null,[g]),$=jg({wsPath:s,storageKeyHistory:d,payload:R,addToast:q,bus:I,handleLocalCommand:K}),{classificationMap:ne}=V3({messages:$.messages,bus:I}),[ue,ce]=D1(s),{graphData:ie,setGraphData:M,rightTab:D,setRightTab:V,isRefreshing:P}=Og(ue,q,N[0],N,f),{modalUploadPath:ee,successfulUploadPaths:re,handleOpenUploadModal:te,handleCloseUploadModal:F,handleUploadSuccess:me,handleUploadError:_e,resetSuccessfulPaths:xe}=T1({bus:I,addToast:q});zg({bus:I,pinnedGraphPath:ue,setPinnedGraphPath:ce,connected:$.connected,sendRawText:$.sendRawText,addToast:q});const Be=x.useRef(!1);x.useEffect(()=>{Be.current&&!$.connected&&(ce(null),M(null)),Be.current=$.connected},[$.connected,ce,M]);const[qe,Me]=oa(r.storageKeyHelpTopic??"help-topic-fallback",""),[le,de]=oa("help-panel-open",!1),[fe,ke]=x.useState(()=>!!g&&!le),[W,ye]=x.useState(!1),Te=x.useRef(null),he=x.useCallback(()=>{fe&&(ye(!0),Te.current=setTimeout(()=>ke(!1),400))},[fe]);x.useEffect(()=>{if(!fe||W)return;const je=setTimeout(he,3e3);return()=>clearTimeout(je)},[fe,W,he]),x.useEffect(()=>{le&&fe&&he()},[le,fe,he]),x.useEffect(()=>()=>{Te.current&&clearTimeout(Te.current)},[]),x.useEffect(()=>{if(!g)return;const je=Et=>{Et.ctrlKey&&Et.key==="`"&&(Et.preventDefault(),de(et=>!et))};return window.addEventListener("keydown",je),()=>window.removeEventListener("keydown",je)},[g,de]),b1({bus:I,setHelpTopic:Me,onTabSwitch:g?()=>de(!0):()=>{}}),w1({bus:I,connected:$.connected,appendMessage:$.appendMessage,addToast:q});const De=ch(),[Ue,dt]=oa("clipboard-sidebar-open",!1),[Fe,ct]=x.useState(null),sa=x.useCallback(je=>{const et=(ie==null?void 0:ie.nodes.some(ja=>ja.alias===je.node.alias))??!1?"update":"create",kn=O1(et,je.node);$.setCommand(kn),q(`${et==="create"?"Create":"Update"} command for "${je.node.alias}" pasted to input`,"info")},[ie,$.setCommand,q]),ge=x.useCallback(async(je,Et)=>{try{const et=await De.clipNode(je,Et,{sourceWsPath:s,sourceLabel:r.label});switch(et.status){case"added":q(`Node "${je.alias}" clipped to clipboard`,"success");break;case"duplicate":ct({pendingItem:et.pendingItem,existingItem:et.existingItem});break;case"error":q(`Clip failed: ${et.message}`,"error");break}}catch(et){q(`Clip failed: ${et instanceof Error?et.message:String(et)}`,"error")}},[De,s,r.label,q]),pn=N1(b??""),{defaultName:fo,setLastSavedName:ho,resetName:ra}=C1(b?`${b}-untitled-counter`:"untitled-counter",I),{handleSaveGraph:ki,handleLoadGraph:mo}=M1({bus:I,connected:$.connected,sendRawText:$.sendRawText,saveGraph:pn.saveGraph,setLastSavedName:ho,addToast:q}),Ri=x.useCallback(je=>{const Et=ne.get(je.id),et=Et==null?void 0:Et.find(kn=>kn.kind==="graph.link");et&&ce(et.apiPath)},[ne]),{handleSendToJsonPath:Bi}=_1({ctx:E,navigate:v,addToast:q,wsPath:s}),ca=Dg("(max-width: 768px)"),{defaultLayout:St,onLayoutChanged:ln}=wy({id:r.path+"-panel-split",storage:localStorage}),xt=x.useCallback(()=>G(rc(R)),[R]),Ui=x.useCallback(()=>{$.clearMessages(),ce(null),M(null),xe(),ra()},[$.clearMessages,M,xe,ra]);return h.jsxs("div",{className:zt.wrapper,children:[h.jsx(q1,{toasts:Y,onRemove:B}),ee&&h.jsx(v_,{uploadPath:ee,onSuccess:me,onClose:F,onError:_e}),h.jsxs("header",{className:zt.header,children:[h.jsx("h1",{className:zt.title,children:n}),h.jsxs("div",{className:zt.headerActions,children:[b&&h.jsx(j0,{disabled:!ie,defaultName:fo,onSave:ki,nameExists:pn.hasGraph,connected:$.connected}),b&&pn.savedGraphs.length>0&&h.jsx(L0,{savedGraphs:pn.savedGraphs,onLoad:mo,onDelete:pn.deleteGraph,connected:$.connected}),m&&h.jsxs("button",{className:zt.clipboardToggle,onClick:()=>dt(je=>!je),"aria-label":Ue?"Close clipboard sidebar":"Open clipboard sidebar","aria-pressed":Ue,children:["Clipboard",De.items.length>0?` (${De.items.length})`:""]}),h.jsx(v0,{addToast:q}),g&&h.jsxs("div",{className:zt.helpButtonWrapper,children:[h.jsx("button",{className:`${zt.helpToggle}${fe&&!W?` ${zt.helpTogglePulsing}`:""}`,onClick:()=>de(je=>!je),"aria-label":le?"Close help panel":"Open help panel","aria-pressed":le,children:"?"}),fe&&h.jsxs("div",{className:`${zt.helpHint}${W?` ${zt.helpHintFading}`:""}`,onClick:he,role:"status",children:[h.jsx("kbd",{className:zt.helpHintKbd,children:"Ctrl + `"})," to toggle help"]})]})]})]}),Fe&&h.jsx(Y3,{existingItem:Fe.existingItem,pendingItem:Fe.pendingItem,onReplace:async()=>{try{await De.confirmReplace(Fe.pendingItem,Fe.existingItem.id),ct(null),q(`Clipboard item "${Fe.pendingItem.node.alias}" replaced`,"success")}catch(je){q(`Replace failed: ${je instanceof Error?je.message:String(je)}`,"error")}},onCancel:()=>{ct(null),q("Clip cancelled","info")}}),h.jsxs(Zf,{className:zt.panelGroup,orientation:ca?"vertical":"horizontal",defaultLayout:St,onLayoutChanged:ln,children:[h.jsx(co,{defaultSize:le||Ue?"50%":"60%",minSize:"25%",children:h.jsx(L2,{messages:$.messages,classificationMap:ne,onCopy:$.copyMessages,onClear:Ui,consoleRef:$.consoleRef,command:$.command,onCommandChange:$.setCommand,onCommandKeyDown:$.handleKeyDown,onSend:$.sendCommand,sendDisabled:!$.connected||!$.command.trim(),inputDisabled:!$.connected,commandHistory:$.history,onGraphLinkMessage:Ri,onCopyMessage:()=>q("Copied to clipboard","success"),onSendToJsonPath:Bi,onUploadMockData:te,successfulUploadPaths:re})}),h.jsx(sc,{className:zt.resizeHandle,"aria-label":"Resize panels"}),h.jsx(co,{defaultSize:le?"50%":Ue?"30%":"40%",minSize:"20%",children:h.jsx(xb,{tabs:N,payload:R,onChange:G,validation:L,onFormat:xt,onUpload:y?$.uploadPayload:void 0,graphData:ie,activeTab:D,onTabChange:V,onGraphRenderError:je=>q(je,"error"),onGraphDataCopySuccess:()=>q("Graph JSON copied to clipboard!","success"),onGraphDataCopyError:()=>q("Copy failed","error"),isGraphRefreshing:P,onClipNode:m?ge:void 0,helpPanel:g&&le?(je,Et)=>h.jsx(q3,{activeTopic:qe,onNavigate:Me,onClose:()=>de(!1),onToggleMaximize:je,isMaximized:Et}):void 0})}),m&&Ue&&h.jsxs(h.Fragment,{children:[h.jsx(sc,{className:zt.resizeHandle,"aria-label":"Resize clipboard"}),h.jsx(co,{defaultSize:"20%",minSize:"10%",maxSize:"40%",children:h.jsx(S3,{connected:$.connected,onPaste:sa})})]})]})]})}function K3(){const r=Se.c(2),n=On[0].path;let s;r[0]===Symbol.for("react.memo_cache_sentinel")?(s=On.map($3),r[0]=s):s=r[0];let u;return r[1]===Symbol.for("react.memo_cache_sentinel")?(u=h.jsx(gg,{children:h.jsx(q_,{children:h.jsx(Cy,{children:h.jsxs(jy,{children:[s,h.jsx(If,{path:"*",element:h.jsx(My,{to:n,replace:!0})})]})})})}),r[1]=u):u=r[1],u}function $3(r){return h.jsx(If,{path:r.path,element:h.jsx(I3,{config:r},r.path)},r.path)}Vy.createRoot(document.getElementById("root")).render(h.jsx(x.StrictMode,{children:h.jsx(K3,{})}));
//# sourceMappingURL=index-CVvR0phC.js.map
