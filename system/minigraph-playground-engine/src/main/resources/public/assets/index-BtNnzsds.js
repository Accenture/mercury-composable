import{j as p,T as mf,_ as Tl,H as vc,W as Qg}from"./vendor-panels-Cixz1HBJ.js";import{a as gf,b as Kg,r as w,N as $g,R as yf,u as Wg,B as Pg,c as Fg,d as bf,e as ey}from"./vendor-router-DUFbnzxw.js";import{N as ty,H as Ui,P as Li,M as ny,u as ay,a as oy,i as ly,B as iy,b as sy,C as ry,c as cy}from"./vendor-xyflow-k-RwjR-l.js";import{c as uy,a as dy,d as El,J as Rc}from"./vendor-json-view-Djmwb-hd.js";import{M as py,r as hy}from"./vendor-markdown-Cp1IxVgw.js";(function(){const n=document.createElement("link").relList;if(n&&n.supports&&n.supports("modulepreload"))return;for(const d of document.querySelectorAll('link[rel="modulepreload"]'))c(d);new MutationObserver(d=>{for(const h of d)if(h.type==="childList")for(const y of h.addedNodes)y.tagName==="LINK"&&y.rel==="modulepreload"&&c(y)}).observe(document,{childList:!0,subtree:!0});function s(d){const h={};return d.integrity&&(h.integrity=d.integrity),d.referrerPolicy&&(h.referrerPolicy=d.referrerPolicy),d.crossOrigin==="use-credentials"?h.credentials="include":d.crossOrigin==="anonymous"?h.credentials="omit":h.credentials="same-origin",h}function c(d){if(d.ep)return;d.ep=!0;const h=s(d);fetch(d.href,h)}})();var lc={exports:{}},Sl={},ic={exports:{}},sc={};/**
 * @license React
 * scheduler.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Rh;function fy(){return Rh||(Rh=1,(function(r){function n(R,z){var $=R.length;R.push(z);e:for(;0<$;){var ie=$-1>>>1,se=R[ie];if(0<d(se,z))R[ie]=z,R[$]=se,$=ie;else break e}}function s(R){return R.length===0?null:R[0]}function c(R){if(R.length===0)return null;var z=R[0],$=R.pop();if($!==z){R[0]=$;e:for(var ie=0,se=R.length,pe=se>>>1;ie<pe;){var ae=2*(ie+1)-1,te=R[ae],ue=ae+1,re=R[ue];if(0>d(te,$))ue<se&&0>d(re,te)?(R[ie]=re,R[ue]=$,ie=ue):(R[ie]=te,R[ae]=$,ie=ae);else if(ue<se&&0>d(re,$))R[ie]=re,R[ue]=$,ie=ue;else break e}}return z}function d(R,z){var $=R.sortIndex-z.sortIndex;return $!==0?$:R.id-z.id}if(r.unstable_now=void 0,typeof performance=="object"&&typeof performance.now=="function"){var h=performance;r.unstable_now=function(){return h.now()}}else{var y=Date,g=y.now();r.unstable_now=function(){return y.now()-g}}var f=[],b=[],E=1,x=null,S=3,j=!1,A=!1,_=!1,v=!1,T=typeof setTimeout=="function"?setTimeout:null,O=typeof clearTimeout=="function"?clearTimeout:null,k=typeof setImmediate<"u"?setImmediate:null;function H(R){for(var z=s(b);z!==null;){if(z.callback===null)c(b);else if(z.startTime<=R)c(b),z.sortIndex=z.expirationTime,n(f,z);else break;z=s(b)}}function D(R){if(_=!1,H(R),!A)if(s(f)!==null)A=!0,L||(L=!0,Z());else{var z=s(b);z!==null&&le(D,z.startTime-R)}}var L=!1,C=-1,Y=5,J=-1;function I(){return v?!0:!(r.unstable_now()-J<Y)}function K(){if(v=!1,L){var R=r.unstable_now();J=R;var z=!0;try{e:{A=!1,_&&(_=!1,O(C),C=-1),j=!0;var $=S;try{t:{for(H(R),x=s(f);x!==null&&!(x.expirationTime>R&&I());){var ie=x.callback;if(typeof ie=="function"){x.callback=null,S=x.priorityLevel;var se=ie(x.expirationTime<=R);if(R=r.unstable_now(),typeof se=="function"){x.callback=se,H(R),z=!0;break t}x===s(f)&&c(f),H(R)}else c(f);x=s(f)}if(x!==null)z=!0;else{var pe=s(b);pe!==null&&le(D,pe.startTime-R),z=!1}}break e}finally{x=null,S=$,j=!1}z=void 0}}finally{z?Z():L=!1}}}var Z;if(typeof k=="function")Z=function(){k(K)};else if(typeof MessageChannel<"u"){var ee=new MessageChannel,P=ee.port2;ee.port1.onmessage=K,Z=function(){P.postMessage(null)}}else Z=function(){T(K,0)};function le(R,z){C=T(function(){R(r.unstable_now())},z)}r.unstable_IdlePriority=5,r.unstable_ImmediatePriority=1,r.unstable_LowPriority=4,r.unstable_NormalPriority=3,r.unstable_Profiling=null,r.unstable_UserBlockingPriority=2,r.unstable_cancelCallback=function(R){R.callback=null},r.unstable_forceFrameRate=function(R){0>R||125<R?console.error("forceFrameRate takes a positive int between 0 and 125, forcing frame rates higher than 125 fps is not supported"):Y=0<R?Math.floor(1e3/R):5},r.unstable_getCurrentPriorityLevel=function(){return S},r.unstable_next=function(R){switch(S){case 1:case 2:case 3:var z=3;break;default:z=S}var $=S;S=z;try{return R()}finally{S=$}},r.unstable_requestPaint=function(){v=!0},r.unstable_runWithPriority=function(R,z){switch(R){case 1:case 2:case 3:case 4:case 5:break;default:R=3}var $=S;S=R;try{return z()}finally{S=$}},r.unstable_scheduleCallback=function(R,z,$){var ie=r.unstable_now();switch(typeof $=="object"&&$!==null?($=$.delay,$=typeof $=="number"&&0<$?ie+$:ie):$=ie,R){case 1:var se=-1;break;case 2:se=250;break;case 5:se=1073741823;break;case 4:se=1e4;break;default:se=5e3}return se=$+se,R={id:E++,callback:z,priorityLevel:R,startTime:$,expirationTime:se,sortIndex:-1},$>ie?(R.sortIndex=$,n(b,R),s(f)===null&&R===s(b)&&(_?(O(C),C=-1):_=!0,le(D,$-ie))):(R.sortIndex=se,n(f,R),A||j||(A=!0,L||(L=!0,Z()))),R},r.unstable_shouldYield=I,r.unstable_wrapCallback=function(R){var z=S;return function(){var $=S;S=z;try{return R.apply(this,arguments)}finally{S=$}}}})(sc)),sc}var zh;function my(){return zh||(zh=1,ic.exports=fy()),ic.exports}/**
 * @license React
 * react-dom-client.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Bh;function gy(){if(Bh)return Sl;Bh=1;var r=my(),n=gf(),s=Kg();function c(e){var t="https://react.dev/errors/"+e;if(1<arguments.length){t+="?args[]="+encodeURIComponent(arguments[1]);for(var a=2;a<arguments.length;a++)t+="&args[]="+encodeURIComponent(arguments[a])}return"Minified React error #"+e+"; visit "+t+" for the full message or use the non-minified dev environment for full errors and additional helpful warnings."}function d(e){return!(!e||e.nodeType!==1&&e.nodeType!==9&&e.nodeType!==11)}function h(e){var t=e,a=e;if(e.alternate)for(;t.return;)t=t.return;else{e=t;do t=e,(t.flags&4098)!==0&&(a=t.return),e=t.return;while(e)}return t.tag===3?a:null}function y(e){if(e.tag===13){var t=e.memoizedState;if(t===null&&(e=e.alternate,e!==null&&(t=e.memoizedState)),t!==null)return t.dehydrated}return null}function g(e){if(e.tag===31){var t=e.memoizedState;if(t===null&&(e=e.alternate,e!==null&&(t=e.memoizedState)),t!==null)return t.dehydrated}return null}function f(e){if(h(e)!==e)throw Error(c(188))}function b(e){var t=e.alternate;if(!t){if(t=h(e),t===null)throw Error(c(188));return t!==e?null:e}for(var a=e,o=t;;){var l=a.return;if(l===null)break;var i=l.alternate;if(i===null){if(o=l.return,o!==null){a=o;continue}break}if(l.child===i.child){for(i=l.child;i;){if(i===a)return f(l),e;if(i===o)return f(l),t;i=i.sibling}throw Error(c(188))}if(a.return!==o.return)a=l,o=i;else{for(var u=!1,m=l.child;m;){if(m===a){u=!0,a=l,o=i;break}if(m===o){u=!0,o=l,a=i;break}m=m.sibling}if(!u){for(m=i.child;m;){if(m===a){u=!0,a=i,o=l;break}if(m===o){u=!0,o=i,a=l;break}m=m.sibling}if(!u)throw Error(c(189))}}if(a.alternate!==o)throw Error(c(190))}if(a.tag!==3)throw Error(c(188));return a.stateNode.current===a?e:t}function E(e){var t=e.tag;if(t===5||t===26||t===27||t===6)return e;for(e=e.child;e!==null;){if(t=E(e),t!==null)return t;e=e.sibling}return null}var x=Object.assign,S=Symbol.for("react.element"),j=Symbol.for("react.transitional.element"),A=Symbol.for("react.portal"),_=Symbol.for("react.fragment"),v=Symbol.for("react.strict_mode"),T=Symbol.for("react.profiler"),O=Symbol.for("react.consumer"),k=Symbol.for("react.context"),H=Symbol.for("react.forward_ref"),D=Symbol.for("react.suspense"),L=Symbol.for("react.suspense_list"),C=Symbol.for("react.memo"),Y=Symbol.for("react.lazy"),J=Symbol.for("react.activity"),I=Symbol.for("react.memo_cache_sentinel"),K=Symbol.iterator;function Z(e){return e===null||typeof e!="object"?null:(e=K&&e[K]||e["@@iterator"],typeof e=="function"?e:null)}var ee=Symbol.for("react.client.reference");function P(e){if(e==null)return null;if(typeof e=="function")return e.$$typeof===ee?null:e.displayName||e.name||null;if(typeof e=="string")return e;switch(e){case _:return"Fragment";case T:return"Profiler";case v:return"StrictMode";case D:return"Suspense";case L:return"SuspenseList";case J:return"Activity"}if(typeof e=="object")switch(e.$$typeof){case A:return"Portal";case k:return e.displayName||"Context";case O:return(e._context.displayName||"Context")+".Consumer";case H:var t=e.render;return e=e.displayName,e||(e=t.displayName||t.name||"",e=e!==""?"ForwardRef("+e+")":"ForwardRef"),e;case C:return t=e.displayName||null,t!==null?t:P(e.type)||"Memo";case Y:t=e._payload,e=e._init;try{return P(e(t))}catch{}}return null}var le=Array.isArray,R=n.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,z=s.__DOM_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,$={pending:!1,data:null,method:null,action:null},ie=[],se=-1;function pe(e){return{current:e}}function ae(e){0>se||(e.current=ie[se],ie[se]=null,se--)}function te(e,t){se++,ie[se]=e.current,e.current=t}var ue=pe(null),re=pe(null),me=pe(null),Ce=pe(null);function Oe(e,t){switch(te(me,t),te(re,e),te(ue,null),t.nodeType){case 9:case 11:e=(e=t.documentElement)&&(e=e.namespaceURI)?ah(e):0;break;default:if(e=t.tagName,t=t.namespaceURI)t=ah(t),e=oh(t,e);else switch(e){case"svg":e=1;break;case"math":e=2;break;default:e=0}}ae(ue),te(ue,e)}function _e(){ae(ue),ae(re),ae(me)}function Re(e){e.memoizedState!==null&&te(Ce,e);var t=ue.current,a=oh(t,e.type);t!==a&&(te(re,e),te(ue,a))}function ne(e){re.current===e&&(ae(ue),ae(re)),Ce.current===e&&(ae(Ce),bl._currentValue=$)}var ge,xe;function F(e){if(ge===void 0)try{throw Error()}catch(a){var t=a.stack.trim().match(/\n( *(at )?)/);ge=t&&t[1]||"",xe=-1<a.stack.indexOf(`
    at`)?" (<anonymous>)":-1<a.stack.indexOf("@")?"@unknown:0:0":""}return`
`+ge+e+xe}var ye=!1;function Se(e,t){if(!e||ye)return"";ye=!0;var a=Error.prepareStackTrace;Error.prepareStackTrace=void 0;try{var o={DetermineComponentFrameRoot:function(){try{if(t){var W=function(){throw Error()};if(Object.defineProperty(W.prototype,"props",{set:function(){throw Error()}}),typeof Reflect=="object"&&Reflect.construct){try{Reflect.construct(W,[])}catch(X){var q=X}Reflect.construct(e,[],W)}else{try{W.call()}catch(X){q=X}e.call(W.prototype)}}else{try{throw Error()}catch(X){q=X}(W=e())&&typeof W.catch=="function"&&W.catch(function(){})}}catch(X){if(X&&q&&typeof X.stack=="string")return[X.stack,q.stack]}return[null,null]}};o.DetermineComponentFrameRoot.displayName="DetermineComponentFrameRoot";var l=Object.getOwnPropertyDescriptor(o.DetermineComponentFrameRoot,"name");l&&l.configurable&&Object.defineProperty(o.DetermineComponentFrameRoot,"name",{value:"DetermineComponentFrameRoot"});var i=o.DetermineComponentFrameRoot(),u=i[0],m=i[1];if(u&&m){var N=u.split(`
`),U=m.split(`
`);for(l=o=0;o<N.length&&!N[o].includes("DetermineComponentFrameRoot");)o++;for(;l<U.length&&!U[l].includes("DetermineComponentFrameRoot");)l++;if(o===N.length||l===U.length)for(o=N.length-1,l=U.length-1;1<=o&&0<=l&&N[o]!==U[l];)l--;for(;1<=o&&0<=l;o--,l--)if(N[o]!==U[l]){if(o!==1||l!==1)do if(o--,l--,0>l||N[o]!==U[l]){var V=`
`+N[o].replace(" at new "," at ");return e.displayName&&V.includes("<anonymous>")&&(V=V.replace("<anonymous>",e.displayName)),V}while(1<=o&&0<=l);break}}}finally{ye=!1,Error.prepareStackTrace=a}return(a=e?e.displayName||e.name:"")?F(a):""}function he(e,t){switch(e.tag){case 26:case 27:case 5:return F(e.type);case 16:return F("Lazy");case 13:return e.child!==t&&t!==null?F("Suspense Fallback"):F("Suspense");case 19:return F("SuspenseList");case 0:case 15:return Se(e.type,!1);case 11:return Se(e.type.render,!1);case 1:return Se(e.type,!0);case 31:return F("Activity");default:return""}}function we(e){try{var t="",a=null;do t+=he(e,a),a=e,e=e.return;while(e);return t}catch(o){return`
Error generating stack: `+o.message+`
`+o.stack}}var Ne=Object.prototype.hasOwnProperty,Le=r.unstable_scheduleCallback,nt=r.unstable_cancelCallback,qe=r.unstable_shouldYield,vt=r.unstable_requestPaint,fe=r.unstable_now,Mt=r.unstable_getCurrentPriorityLevel,Ht=r.unstable_ImmediatePriority,ut=r.unstable_UserBlockingPriority,ht=r.unstable_NormalPriority,qt=r.unstable_LowPriority,Gt=r.unstable_IdlePriority,Sn=r.log,Dt=r.unstable_setDisableYieldValue,Tt=null,Ye=null;function it(e){if(typeof Sn=="function"&&Dt(e),Ye&&typeof Ye.setStrictMode=="function")try{Ye.setStrictMode(Tt,e)}catch{}}var Fe=Math.clz32?Math.clz32:oe,Te=Math.log,ct=Math.LN2;function oe(e){return e>>>=0,e===0?32:31-(Te(e)/ct|0)|0}var Me=256,st=262144,bn=4194304;function jn(e){var t=e&42;if(t!==0)return t;switch(e&-e){case 1:return 1;case 2:return 2;case 4:return 4;case 8:return 8;case 16:return 16;case 32:return 32;case 64:return 64;case 128:return 128;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:return e&261888;case 262144:case 524288:case 1048576:case 2097152:return e&3932160;case 4194304:case 8388608:case 16777216:case 33554432:return e&62914560;case 67108864:return 67108864;case 134217728:return 134217728;case 268435456:return 268435456;case 536870912:return 536870912;case 1073741824:return 0;default:return e}}function Ae(e,t,a){var o=e.pendingLanes;if(o===0)return 0;var l=0,i=e.suspendedLanes,u=e.pingedLanes;e=e.warmLanes;var m=o&134217727;return m!==0?(o=m&~i,o!==0?l=jn(o):(u&=m,u!==0?l=jn(u):a||(a=m&~e,a!==0&&(l=jn(a))))):(m=o&~i,m!==0?l=jn(m):u!==0?l=jn(u):a||(a=o&~e,a!==0&&(l=jn(a)))),l===0?0:t!==0&&t!==l&&(t&i)===0&&(i=l&-l,a=t&-t,i>=a||i===32&&(a&4194048)!==0)?t:l}function Ze(e,t){return(e.pendingLanes&~(e.suspendedLanes&~e.pingedLanes)&t)===0}function ft(e,t){switch(e){case 1:case 2:case 4:case 8:case 64:return t+250;case 16:case 32:case 128:case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:return t+5e3;case 4194304:case 8388608:case 16777216:case 33554432:return-1;case 67108864:case 134217728:case 268435456:case 536870912:case 1073741824:return-1;default:return-1}}function $n(){var e=bn;return bn<<=1,(bn&62914560)===0&&(bn=4194304),e}function Vi(e){for(var t=[],a=0;31>a;a++)t.push(e);return t}function ko(e,t){e.pendingLanes|=t,t!==268435456&&(e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0)}function Bf(e,t,a,o,l,i){var u=e.pendingLanes;e.pendingLanes=a,e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0,e.expiredLanes&=a,e.entangledLanes&=a,e.errorRecoveryDisabledLanes&=a,e.shellSuspendCounter=0;var m=e.entanglements,N=e.expirationTimes,U=e.hiddenUpdates;for(a=u&~a;0<a;){var V=31-Fe(a),W=1<<V;m[V]=0,N[V]=-1;var q=U[V];if(q!==null)for(U[V]=null,V=0;V<q.length;V++){var X=q[V];X!==null&&(X.lane&=-536870913)}a&=~W}o!==0&&Uc(e,o,0),i!==0&&l===0&&e.tag!==0&&(e.suspendedLanes|=i&~(u&~t))}function Uc(e,t,a){e.pendingLanes|=t,e.suspendedLanes&=~t;var o=31-Fe(t);e.entangledLanes|=t,e.entanglements[o]=e.entanglements[o]|1073741824|a&261930}function Lc(e,t){var a=e.entangledLanes|=t;for(e=e.entanglements;a;){var o=31-Fe(a),l=1<<o;l&t|e[o]&t&&(e[o]|=t),a&=~l}}function Yc(e,t){var a=t&-t;return a=(a&42)!==0?1:Qi(a),(a&(e.suspendedLanes|t))!==0?0:a}function Qi(e){switch(e){case 2:e=1;break;case 8:e=4;break;case 32:e=16;break;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:case 4194304:case 8388608:case 16777216:case 33554432:e=128;break;case 268435456:e=134217728;break;default:e=0}return e}function Ki(e){return e&=-e,2<e?8<e?(e&134217727)!==0?32:268435456:8:2}function qc(){var e=z.p;return e!==0?e:(e=window.event,e===void 0?32:Ah(e.type))}function Ic(e,t){var a=z.p;try{return z.p=e,t()}finally{z.p=a}}var Wn=Math.random().toString(36).slice(2),kt="__reactFiber$"+Wn,It="__reactProps$"+Wn,qa="__reactContainer$"+Wn,$i="__reactEvents$"+Wn,Hf="__reactListeners$"+Wn,Gf="__reactHandles$"+Wn,Xc="__reactResources$"+Wn,Oo="__reactMarker$"+Wn;function Wi(e){delete e[kt],delete e[It],delete e[$i],delete e[Hf],delete e[Gf]}function Ia(e){var t=e[kt];if(t)return t;for(var a=e.parentNode;a;){if(t=a[qa]||a[kt]){if(a=t.alternate,t.child!==null||a!==null&&a.child!==null)for(e=dh(e);e!==null;){if(a=e[kt])return a;e=dh(e)}return t}e=a,a=e.parentNode}return null}function Xa(e){if(e=e[kt]||e[qa]){var t=e.tag;if(t===5||t===6||t===13||t===31||t===26||t===27||t===3)return e}return null}function Ro(e){var t=e.tag;if(t===5||t===26||t===27||t===6)return e.stateNode;throw Error(c(33))}function Ja(e){var t=e[Xc];return t||(t=e[Xc]={hoistableStyles:new Map,hoistableScripts:new Map}),t}function Nt(e){e[Oo]=!0}var Jc=new Set,Zc={};function Ta(e,t){Za(e,t),Za(e+"Capture",t)}function Za(e,t){for(Zc[e]=t,e=0;e<t.length;e++)Jc.add(t[e])}var Uf=RegExp("^[:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD][:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD\\-.0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040]*$"),Vc={},Qc={};function Lf(e){return Ne.call(Qc,e)?!0:Ne.call(Vc,e)?!1:Uf.test(e)?Qc[e]=!0:(Vc[e]=!0,!1)}function Al(e,t,a){if(Lf(t))if(a===null)e.removeAttribute(t);else{switch(typeof a){case"undefined":case"function":case"symbol":e.removeAttribute(t);return;case"boolean":var o=t.toLowerCase().slice(0,5);if(o!=="data-"&&o!=="aria-"){e.removeAttribute(t);return}}e.setAttribute(t,""+a)}}function Cl(e,t,a){if(a===null)e.removeAttribute(t);else{switch(typeof a){case"undefined":case"function":case"symbol":case"boolean":e.removeAttribute(t);return}e.setAttribute(t,""+a)}}function Mn(e,t,a,o){if(o===null)e.removeAttribute(a);else{switch(typeof o){case"undefined":case"function":case"symbol":case"boolean":e.removeAttribute(a);return}e.setAttributeNS(t,a,""+o)}}function rn(e){switch(typeof e){case"bigint":case"boolean":case"number":case"string":case"undefined":return e;case"object":return e;default:return""}}function Kc(e){var t=e.type;return(e=e.nodeName)&&e.toLowerCase()==="input"&&(t==="checkbox"||t==="radio")}function Yf(e,t,a){var o=Object.getOwnPropertyDescriptor(e.constructor.prototype,t);if(!e.hasOwnProperty(t)&&typeof o<"u"&&typeof o.get=="function"&&typeof o.set=="function"){var l=o.get,i=o.set;return Object.defineProperty(e,t,{configurable:!0,get:function(){return l.call(this)},set:function(u){a=""+u,i.call(this,u)}}),Object.defineProperty(e,t,{enumerable:o.enumerable}),{getValue:function(){return a},setValue:function(u){a=""+u},stopTracking:function(){e._valueTracker=null,delete e[t]}}}}function Pi(e){if(!e._valueTracker){var t=Kc(e)?"checked":"value";e._valueTracker=Yf(e,t,""+e[t])}}function $c(e){if(!e)return!1;var t=e._valueTracker;if(!t)return!0;var a=t.getValue(),o="";return e&&(o=Kc(e)?e.checked?"true":"false":e.value),e=o,e!==a?(t.setValue(e),!0):!1}function jl(e){if(e=e||(typeof document<"u"?document:void 0),typeof e>"u")return null;try{return e.activeElement||e.body}catch{return e.body}}var qf=/[\n"\\]/g;function cn(e){return e.replace(qf,function(t){return"\\"+t.charCodeAt(0).toString(16)+" "})}function Fi(e,t,a,o,l,i,u,m){e.name="",u!=null&&typeof u!="function"&&typeof u!="symbol"&&typeof u!="boolean"?e.type=u:e.removeAttribute("type"),t!=null?u==="number"?(t===0&&e.value===""||e.value!=t)&&(e.value=""+rn(t)):e.value!==""+rn(t)&&(e.value=""+rn(t)):u!=="submit"&&u!=="reset"||e.removeAttribute("value"),t!=null?es(e,u,rn(t)):a!=null?es(e,u,rn(a)):o!=null&&e.removeAttribute("value"),l==null&&i!=null&&(e.defaultChecked=!!i),l!=null&&(e.checked=l&&typeof l!="function"&&typeof l!="symbol"),m!=null&&typeof m!="function"&&typeof m!="symbol"&&typeof m!="boolean"?e.name=""+rn(m):e.removeAttribute("name")}function Wc(e,t,a,o,l,i,u,m){if(i!=null&&typeof i!="function"&&typeof i!="symbol"&&typeof i!="boolean"&&(e.type=i),t!=null||a!=null){if(!(i!=="submit"&&i!=="reset"||t!=null)){Pi(e);return}a=a!=null?""+rn(a):"",t=t!=null?""+rn(t):a,m||t===e.value||(e.value=t),e.defaultValue=t}o=o??l,o=typeof o!="function"&&typeof o!="symbol"&&!!o,e.checked=m?e.checked:!!o,e.defaultChecked=!!o,u!=null&&typeof u!="function"&&typeof u!="symbol"&&typeof u!="boolean"&&(e.name=u),Pi(e)}function es(e,t,a){t==="number"&&jl(e.ownerDocument)===e||e.defaultValue===""+a||(e.defaultValue=""+a)}function Va(e,t,a,o){if(e=e.options,t){t={};for(var l=0;l<a.length;l++)t["$"+a[l]]=!0;for(a=0;a<e.length;a++)l=t.hasOwnProperty("$"+e[a].value),e[a].selected!==l&&(e[a].selected=l),l&&o&&(e[a].defaultSelected=!0)}else{for(a=""+rn(a),t=null,l=0;l<e.length;l++){if(e[l].value===a){e[l].selected=!0,o&&(e[l].defaultSelected=!0);return}t!==null||e[l].disabled||(t=e[l])}t!==null&&(t.selected=!0)}}function Pc(e,t,a){if(t!=null&&(t=""+rn(t),t!==e.value&&(e.value=t),a==null)){e.defaultValue!==t&&(e.defaultValue=t);return}e.defaultValue=a!=null?""+rn(a):""}function Fc(e,t,a,o){if(t==null){if(o!=null){if(a!=null)throw Error(c(92));if(le(o)){if(1<o.length)throw Error(c(93));o=o[0]}a=o}a==null&&(a=""),t=a}a=rn(t),e.defaultValue=a,o=e.textContent,o===a&&o!==""&&o!==null&&(e.value=o),Pi(e)}function Qa(e,t){if(t){var a=e.firstChild;if(a&&a===e.lastChild&&a.nodeType===3){a.nodeValue=t;return}}e.textContent=t}var If=new Set("animationIterationCount aspectRatio borderImageOutset borderImageSlice borderImageWidth boxFlex boxFlexGroup boxOrdinalGroup columnCount columns flex flexGrow flexPositive flexShrink flexNegative flexOrder gridArea gridRow gridRowEnd gridRowSpan gridRowStart gridColumn gridColumnEnd gridColumnSpan gridColumnStart fontWeight lineClamp lineHeight opacity order orphans scale tabSize widows zIndex zoom fillOpacity floodOpacity stopOpacity strokeDasharray strokeDashoffset strokeMiterlimit strokeOpacity strokeWidth MozAnimationIterationCount MozBoxFlex MozBoxFlexGroup MozLineClamp msAnimationIterationCount msFlex msZoom msFlexGrow msFlexNegative msFlexOrder msFlexPositive msFlexShrink msGridColumn msGridColumnSpan msGridRow msGridRowSpan WebkitAnimationIterationCount WebkitBoxFlex WebKitBoxFlexGroup WebkitBoxOrdinalGroup WebkitColumnCount WebkitColumns WebkitFlex WebkitFlexGrow WebkitFlexPositive WebkitFlexShrink WebkitLineClamp".split(" "));function eu(e,t,a){var o=t.indexOf("--")===0;a==null||typeof a=="boolean"||a===""?o?e.setProperty(t,""):t==="float"?e.cssFloat="":e[t]="":o?e.setProperty(t,a):typeof a!="number"||a===0||If.has(t)?t==="float"?e.cssFloat=a:e[t]=(""+a).trim():e[t]=a+"px"}function tu(e,t,a){if(t!=null&&typeof t!="object")throw Error(c(62));if(e=e.style,a!=null){for(var o in a)!a.hasOwnProperty(o)||t!=null&&t.hasOwnProperty(o)||(o.indexOf("--")===0?e.setProperty(o,""):o==="float"?e.cssFloat="":e[o]="");for(var l in t)o=t[l],t.hasOwnProperty(l)&&a[l]!==o&&eu(e,l,o)}else for(var i in t)t.hasOwnProperty(i)&&eu(e,i,t[i])}function ts(e){if(e.indexOf("-")===-1)return!1;switch(e){case"annotation-xml":case"color-profile":case"font-face":case"font-face-src":case"font-face-uri":case"font-face-format":case"font-face-name":case"missing-glyph":return!1;default:return!0}}var Xf=new Map([["acceptCharset","accept-charset"],["htmlFor","for"],["httpEquiv","http-equiv"],["crossOrigin","crossorigin"],["accentHeight","accent-height"],["alignmentBaseline","alignment-baseline"],["arabicForm","arabic-form"],["baselineShift","baseline-shift"],["capHeight","cap-height"],["clipPath","clip-path"],["clipRule","clip-rule"],["colorInterpolation","color-interpolation"],["colorInterpolationFilters","color-interpolation-filters"],["colorProfile","color-profile"],["colorRendering","color-rendering"],["dominantBaseline","dominant-baseline"],["enableBackground","enable-background"],["fillOpacity","fill-opacity"],["fillRule","fill-rule"],["floodColor","flood-color"],["floodOpacity","flood-opacity"],["fontFamily","font-family"],["fontSize","font-size"],["fontSizeAdjust","font-size-adjust"],["fontStretch","font-stretch"],["fontStyle","font-style"],["fontVariant","font-variant"],["fontWeight","font-weight"],["glyphName","glyph-name"],["glyphOrientationHorizontal","glyph-orientation-horizontal"],["glyphOrientationVertical","glyph-orientation-vertical"],["horizAdvX","horiz-adv-x"],["horizOriginX","horiz-origin-x"],["imageRendering","image-rendering"],["letterSpacing","letter-spacing"],["lightingColor","lighting-color"],["markerEnd","marker-end"],["markerMid","marker-mid"],["markerStart","marker-start"],["overlinePosition","overline-position"],["overlineThickness","overline-thickness"],["paintOrder","paint-order"],["panose-1","panose-1"],["pointerEvents","pointer-events"],["renderingIntent","rendering-intent"],["shapeRendering","shape-rendering"],["stopColor","stop-color"],["stopOpacity","stop-opacity"],["strikethroughPosition","strikethrough-position"],["strikethroughThickness","strikethrough-thickness"],["strokeDasharray","stroke-dasharray"],["strokeDashoffset","stroke-dashoffset"],["strokeLinecap","stroke-linecap"],["strokeLinejoin","stroke-linejoin"],["strokeMiterlimit","stroke-miterlimit"],["strokeOpacity","stroke-opacity"],["strokeWidth","stroke-width"],["textAnchor","text-anchor"],["textDecoration","text-decoration"],["textRendering","text-rendering"],["transformOrigin","transform-origin"],["underlinePosition","underline-position"],["underlineThickness","underline-thickness"],["unicodeBidi","unicode-bidi"],["unicodeRange","unicode-range"],["unitsPerEm","units-per-em"],["vAlphabetic","v-alphabetic"],["vHanging","v-hanging"],["vIdeographic","v-ideographic"],["vMathematical","v-mathematical"],["vectorEffect","vector-effect"],["vertAdvY","vert-adv-y"],["vertOriginX","vert-origin-x"],["vertOriginY","vert-origin-y"],["wordSpacing","word-spacing"],["writingMode","writing-mode"],["xmlnsXlink","xmlns:xlink"],["xHeight","x-height"]]),Jf=/^[\u0000-\u001F ]*j[\r\n\t]*a[\r\n\t]*v[\r\n\t]*a[\r\n\t]*s[\r\n\t]*c[\r\n\t]*r[\r\n\t]*i[\r\n\t]*p[\r\n\t]*t[\r\n\t]*:/i;function Ml(e){return Jf.test(""+e)?"javascript:throw new Error('React has blocked a javascript: URL as a security precaution.')":e}function Dn(){}var ns=null;function as(e){return e=e.target||e.srcElement||window,e.correspondingUseElement&&(e=e.correspondingUseElement),e.nodeType===3?e.parentNode:e}var Ka=null,$a=null;function nu(e){var t=Xa(e);if(t&&(e=t.stateNode)){var a=e[It]||null;e:switch(e=t.stateNode,t.type){case"input":if(Fi(e,a.value,a.defaultValue,a.defaultValue,a.checked,a.defaultChecked,a.type,a.name),t=a.name,a.type==="radio"&&t!=null){for(a=e;a.parentNode;)a=a.parentNode;for(a=a.querySelectorAll('input[name="'+cn(""+t)+'"][type="radio"]'),t=0;t<a.length;t++){var o=a[t];if(o!==e&&o.form===e.form){var l=o[It]||null;if(!l)throw Error(c(90));Fi(o,l.value,l.defaultValue,l.defaultValue,l.checked,l.defaultChecked,l.type,l.name)}}for(t=0;t<a.length;t++)o=a[t],o.form===e.form&&$c(o)}break e;case"textarea":Pc(e,a.value,a.defaultValue);break e;case"select":t=a.value,t!=null&&Va(e,!!a.multiple,t,!1)}}}var os=!1;function au(e,t,a){if(os)return e(t,a);os=!0;try{var o=e(t);return o}finally{if(os=!1,(Ka!==null||$a!==null)&&(yi(),Ka&&(t=Ka,e=$a,$a=Ka=null,nu(t),e)))for(t=0;t<e.length;t++)nu(e[t])}}function zo(e,t){var a=e.stateNode;if(a===null)return null;var o=a[It]||null;if(o===null)return null;a=o[t];e:switch(t){case"onClick":case"onClickCapture":case"onDoubleClick":case"onDoubleClickCapture":case"onMouseDown":case"onMouseDownCapture":case"onMouseMove":case"onMouseMoveCapture":case"onMouseUp":case"onMouseUpCapture":case"onMouseEnter":(o=!o.disabled)||(e=e.type,o=!(e==="button"||e==="input"||e==="select"||e==="textarea")),e=!o;break e;default:e=!1}if(e)return null;if(a&&typeof a!="function")throw Error(c(231,t,typeof a));return a}var kn=!(typeof window>"u"||typeof window.document>"u"||typeof window.document.createElement>"u"),ls=!1;if(kn)try{var Bo={};Object.defineProperty(Bo,"passive",{get:function(){ls=!0}}),window.addEventListener("test",Bo,Bo),window.removeEventListener("test",Bo,Bo)}catch{ls=!1}var Pn=null,is=null,Dl=null;function ou(){if(Dl)return Dl;var e,t=is,a=t.length,o,l="value"in Pn?Pn.value:Pn.textContent,i=l.length;for(e=0;e<a&&t[e]===l[e];e++);var u=a-e;for(o=1;o<=u&&t[a-o]===l[i-o];o++);return Dl=l.slice(e,1<o?1-o:void 0)}function kl(e){var t=e.keyCode;return"charCode"in e?(e=e.charCode,e===0&&t===13&&(e=13)):e=t,e===10&&(e=13),32<=e||e===13?e:0}function Ol(){return!0}function lu(){return!1}function Xt(e){function t(a,o,l,i,u){this._reactName=a,this._targetInst=l,this.type=o,this.nativeEvent=i,this.target=u,this.currentTarget=null;for(var m in e)e.hasOwnProperty(m)&&(a=e[m],this[m]=a?a(i):i[m]);return this.isDefaultPrevented=(i.defaultPrevented!=null?i.defaultPrevented:i.returnValue===!1)?Ol:lu,this.isPropagationStopped=lu,this}return x(t.prototype,{preventDefault:function(){this.defaultPrevented=!0;var a=this.nativeEvent;a&&(a.preventDefault?a.preventDefault():typeof a.returnValue!="unknown"&&(a.returnValue=!1),this.isDefaultPrevented=Ol)},stopPropagation:function(){var a=this.nativeEvent;a&&(a.stopPropagation?a.stopPropagation():typeof a.cancelBubble!="unknown"&&(a.cancelBubble=!0),this.isPropagationStopped=Ol)},persist:function(){},isPersistent:Ol}),t}var Ea={eventPhase:0,bubbles:0,cancelable:0,timeStamp:function(e){return e.timeStamp||Date.now()},defaultPrevented:0,isTrusted:0},Rl=Xt(Ea),Ho=x({},Ea,{view:0,detail:0}),Zf=Xt(Ho),ss,rs,Go,zl=x({},Ho,{screenX:0,screenY:0,clientX:0,clientY:0,pageX:0,pageY:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,getModifierState:us,button:0,buttons:0,relatedTarget:function(e){return e.relatedTarget===void 0?e.fromElement===e.srcElement?e.toElement:e.fromElement:e.relatedTarget},movementX:function(e){return"movementX"in e?e.movementX:(e!==Go&&(Go&&e.type==="mousemove"?(ss=e.screenX-Go.screenX,rs=e.screenY-Go.screenY):rs=ss=0,Go=e),ss)},movementY:function(e){return"movementY"in e?e.movementY:rs}}),iu=Xt(zl),Vf=x({},zl,{dataTransfer:0}),Qf=Xt(Vf),Kf=x({},Ho,{relatedTarget:0}),cs=Xt(Kf),$f=x({},Ea,{animationName:0,elapsedTime:0,pseudoElement:0}),Wf=Xt($f),Pf=x({},Ea,{clipboardData:function(e){return"clipboardData"in e?e.clipboardData:window.clipboardData}}),Ff=Xt(Pf),em=x({},Ea,{data:0}),su=Xt(em),tm={Esc:"Escape",Spacebar:" ",Left:"ArrowLeft",Up:"ArrowUp",Right:"ArrowRight",Down:"ArrowDown",Del:"Delete",Win:"OS",Menu:"ContextMenu",Apps:"ContextMenu",Scroll:"ScrollLock",MozPrintableKey:"Unidentified"},nm={8:"Backspace",9:"Tab",12:"Clear",13:"Enter",16:"Shift",17:"Control",18:"Alt",19:"Pause",20:"CapsLock",27:"Escape",32:" ",33:"PageUp",34:"PageDown",35:"End",36:"Home",37:"ArrowLeft",38:"ArrowUp",39:"ArrowRight",40:"ArrowDown",45:"Insert",46:"Delete",112:"F1",113:"F2",114:"F3",115:"F4",116:"F5",117:"F6",118:"F7",119:"F8",120:"F9",121:"F10",122:"F11",123:"F12",144:"NumLock",145:"ScrollLock",224:"Meta"},am={Alt:"altKey",Control:"ctrlKey",Meta:"metaKey",Shift:"shiftKey"};function om(e){var t=this.nativeEvent;return t.getModifierState?t.getModifierState(e):(e=am[e])?!!t[e]:!1}function us(){return om}var lm=x({},Ho,{key:function(e){if(e.key){var t=tm[e.key]||e.key;if(t!=="Unidentified")return t}return e.type==="keypress"?(e=kl(e),e===13?"Enter":String.fromCharCode(e)):e.type==="keydown"||e.type==="keyup"?nm[e.keyCode]||"Unidentified":""},code:0,location:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,repeat:0,locale:0,getModifierState:us,charCode:function(e){return e.type==="keypress"?kl(e):0},keyCode:function(e){return e.type==="keydown"||e.type==="keyup"?e.keyCode:0},which:function(e){return e.type==="keypress"?kl(e):e.type==="keydown"||e.type==="keyup"?e.keyCode:0}}),im=Xt(lm),sm=x({},zl,{pointerId:0,width:0,height:0,pressure:0,tangentialPressure:0,tiltX:0,tiltY:0,twist:0,pointerType:0,isPrimary:0}),ru=Xt(sm),rm=x({},Ho,{touches:0,targetTouches:0,changedTouches:0,altKey:0,metaKey:0,ctrlKey:0,shiftKey:0,getModifierState:us}),cm=Xt(rm),um=x({},Ea,{propertyName:0,elapsedTime:0,pseudoElement:0}),dm=Xt(um),pm=x({},zl,{deltaX:function(e){return"deltaX"in e?e.deltaX:"wheelDeltaX"in e?-e.wheelDeltaX:0},deltaY:function(e){return"deltaY"in e?e.deltaY:"wheelDeltaY"in e?-e.wheelDeltaY:"wheelDelta"in e?-e.wheelDelta:0},deltaZ:0,deltaMode:0}),hm=Xt(pm),fm=x({},Ea,{newState:0,oldState:0}),mm=Xt(fm),gm=[9,13,27,32],ds=kn&&"CompositionEvent"in window,Uo=null;kn&&"documentMode"in document&&(Uo=document.documentMode);var ym=kn&&"TextEvent"in window&&!Uo,cu=kn&&(!ds||Uo&&8<Uo&&11>=Uo),uu=" ",du=!1;function pu(e,t){switch(e){case"keyup":return gm.indexOf(t.keyCode)!==-1;case"keydown":return t.keyCode!==229;case"keypress":case"mousedown":case"focusout":return!0;default:return!1}}function hu(e){return e=e.detail,typeof e=="object"&&"data"in e?e.data:null}var Wa=!1;function bm(e,t){switch(e){case"compositionend":return hu(t);case"keypress":return t.which!==32?null:(du=!0,uu);case"textInput":return e=t.data,e===uu&&du?null:e;default:return null}}function vm(e,t){if(Wa)return e==="compositionend"||!ds&&pu(e,t)?(e=ou(),Dl=is=Pn=null,Wa=!1,e):null;switch(e){case"paste":return null;case"keypress":if(!(t.ctrlKey||t.altKey||t.metaKey)||t.ctrlKey&&t.altKey){if(t.char&&1<t.char.length)return t.char;if(t.which)return String.fromCharCode(t.which)}return null;case"compositionend":return cu&&t.locale!=="ko"?null:t.data;default:return null}}var _m={color:!0,date:!0,datetime:!0,"datetime-local":!0,email:!0,month:!0,number:!0,password:!0,range:!0,search:!0,tel:!0,text:!0,time:!0,url:!0,week:!0};function fu(e){var t=e&&e.nodeName&&e.nodeName.toLowerCase();return t==="input"?!!_m[e.type]:t==="textarea"}function mu(e,t,a,o){Ka?$a?$a.push(o):$a=[o]:Ka=o,t=Ti(t,"onChange"),0<t.length&&(a=new Rl("onChange","change",null,a,o),e.push({event:a,listeners:t}))}var Lo=null,Yo=null;function xm(e){Wp(e,0)}function Bl(e){var t=Ro(e);if($c(t))return e}function gu(e,t){if(e==="change")return t}var yu=!1;if(kn){var ps;if(kn){var hs="oninput"in document;if(!hs){var bu=document.createElement("div");bu.setAttribute("oninput","return;"),hs=typeof bu.oninput=="function"}ps=hs}else ps=!1;yu=ps&&(!document.documentMode||9<document.documentMode)}function vu(){Lo&&(Lo.detachEvent("onpropertychange",_u),Yo=Lo=null)}function _u(e){if(e.propertyName==="value"&&Bl(Yo)){var t=[];mu(t,Yo,e,as(e)),au(xm,t)}}function Sm(e,t,a){e==="focusin"?(vu(),Lo=t,Yo=a,Lo.attachEvent("onpropertychange",_u)):e==="focusout"&&vu()}function wm(e){if(e==="selectionchange"||e==="keyup"||e==="keydown")return Bl(Yo)}function Tm(e,t){if(e==="click")return Bl(t)}function Em(e,t){if(e==="input"||e==="change")return Bl(t)}function Nm(e,t){return e===t&&(e!==0||1/e===1/t)||e!==e&&t!==t}var Wt=typeof Object.is=="function"?Object.is:Nm;function qo(e,t){if(Wt(e,t))return!0;if(typeof e!="object"||e===null||typeof t!="object"||t===null)return!1;var a=Object.keys(e),o=Object.keys(t);if(a.length!==o.length)return!1;for(o=0;o<a.length;o++){var l=a[o];if(!Ne.call(t,l)||!Wt(e[l],t[l]))return!1}return!0}function xu(e){for(;e&&e.firstChild;)e=e.firstChild;return e}function Su(e,t){var a=xu(e);e=0;for(var o;a;){if(a.nodeType===3){if(o=e+a.textContent.length,e<=t&&o>=t)return{node:a,offset:t-e};e=o}e:{for(;a;){if(a.nextSibling){a=a.nextSibling;break e}a=a.parentNode}a=void 0}a=xu(a)}}function wu(e,t){return e&&t?e===t?!0:e&&e.nodeType===3?!1:t&&t.nodeType===3?wu(e,t.parentNode):"contains"in e?e.contains(t):e.compareDocumentPosition?!!(e.compareDocumentPosition(t)&16):!1:!1}function Tu(e){e=e!=null&&e.ownerDocument!=null&&e.ownerDocument.defaultView!=null?e.ownerDocument.defaultView:window;for(var t=jl(e.document);t instanceof e.HTMLIFrameElement;){try{var a=typeof t.contentWindow.location.href=="string"}catch{a=!1}if(a)e=t.contentWindow;else break;t=jl(e.document)}return t}function fs(e){var t=e&&e.nodeName&&e.nodeName.toLowerCase();return t&&(t==="input"&&(e.type==="text"||e.type==="search"||e.type==="tel"||e.type==="url"||e.type==="password")||t==="textarea"||e.contentEditable==="true")}var Am=kn&&"documentMode"in document&&11>=document.documentMode,Pa=null,ms=null,Io=null,gs=!1;function Eu(e,t,a){var o=a.window===a?a.document:a.nodeType===9?a:a.ownerDocument;gs||Pa==null||Pa!==jl(o)||(o=Pa,"selectionStart"in o&&fs(o)?o={start:o.selectionStart,end:o.selectionEnd}:(o=(o.ownerDocument&&o.ownerDocument.defaultView||window).getSelection(),o={anchorNode:o.anchorNode,anchorOffset:o.anchorOffset,focusNode:o.focusNode,focusOffset:o.focusOffset}),Io&&qo(Io,o)||(Io=o,o=Ti(ms,"onSelect"),0<o.length&&(t=new Rl("onSelect","select",null,t,a),e.push({event:t,listeners:o}),t.target=Pa)))}function Na(e,t){var a={};return a[e.toLowerCase()]=t.toLowerCase(),a["Webkit"+e]="webkit"+t,a["Moz"+e]="moz"+t,a}var Fa={animationend:Na("Animation","AnimationEnd"),animationiteration:Na("Animation","AnimationIteration"),animationstart:Na("Animation","AnimationStart"),transitionrun:Na("Transition","TransitionRun"),transitionstart:Na("Transition","TransitionStart"),transitioncancel:Na("Transition","TransitionCancel"),transitionend:Na("Transition","TransitionEnd")},ys={},Nu={};kn&&(Nu=document.createElement("div").style,"AnimationEvent"in window||(delete Fa.animationend.animation,delete Fa.animationiteration.animation,delete Fa.animationstart.animation),"TransitionEvent"in window||delete Fa.transitionend.transition);function Aa(e){if(ys[e])return ys[e];if(!Fa[e])return e;var t=Fa[e],a;for(a in t)if(t.hasOwnProperty(a)&&a in Nu)return ys[e]=t[a];return e}var Au=Aa("animationend"),Cu=Aa("animationiteration"),ju=Aa("animationstart"),Cm=Aa("transitionrun"),jm=Aa("transitionstart"),Mm=Aa("transitioncancel"),Mu=Aa("transitionend"),Du=new Map,bs="abort auxClick beforeToggle cancel canPlay canPlayThrough click close contextMenu copy cut drag dragEnd dragEnter dragExit dragLeave dragOver dragStart drop durationChange emptied encrypted ended error gotPointerCapture input invalid keyDown keyPress keyUp load loadedData loadedMetadata loadStart lostPointerCapture mouseDown mouseMove mouseOut mouseOver mouseUp paste pause play playing pointerCancel pointerDown pointerMove pointerOut pointerOver pointerUp progress rateChange reset resize seeked seeking stalled submit suspend timeUpdate touchCancel touchEnd touchStart volumeChange scroll toggle touchMove waiting wheel".split(" ");bs.push("scrollEnd");function vn(e,t){Du.set(e,t),Ta(t,[e])}var Hl=typeof reportError=="function"?reportError:function(e){if(typeof window=="object"&&typeof window.ErrorEvent=="function"){var t=new window.ErrorEvent("error",{bubbles:!0,cancelable:!0,message:typeof e=="object"&&e!==null&&typeof e.message=="string"?String(e.message):String(e),error:e});if(!window.dispatchEvent(t))return}else if(typeof process=="object"&&typeof process.emit=="function"){process.emit("uncaughtException",e);return}console.error(e)},un=[],eo=0,vs=0;function Gl(){for(var e=eo,t=vs=eo=0;t<e;){var a=un[t];un[t++]=null;var o=un[t];un[t++]=null;var l=un[t];un[t++]=null;var i=un[t];if(un[t++]=null,o!==null&&l!==null){var u=o.pending;u===null?l.next=l:(l.next=u.next,u.next=l),o.pending=l}i!==0&&ku(a,l,i)}}function Ul(e,t,a,o){un[eo++]=e,un[eo++]=t,un[eo++]=a,un[eo++]=o,vs|=o,e.lanes|=o,e=e.alternate,e!==null&&(e.lanes|=o)}function _s(e,t,a,o){return Ul(e,t,a,o),Ll(e)}function Ca(e,t){return Ul(e,null,null,t),Ll(e)}function ku(e,t,a){e.lanes|=a;var o=e.alternate;o!==null&&(o.lanes|=a);for(var l=!1,i=e.return;i!==null;)i.childLanes|=a,o=i.alternate,o!==null&&(o.childLanes|=a),i.tag===22&&(e=i.stateNode,e===null||e._visibility&1||(l=!0)),e=i,i=i.return;return e.tag===3?(i=e.stateNode,l&&t!==null&&(l=31-Fe(a),e=i.hiddenUpdates,o=e[l],o===null?e[l]=[t]:o.push(t),t.lane=a|536870912),i):null}function Ll(e){if(50<dl)throw dl=0,jr=null,Error(c(185));for(var t=e.return;t!==null;)e=t,t=e.return;return e.tag===3?e.stateNode:null}var to={};function Dm(e,t,a,o){this.tag=e,this.key=a,this.sibling=this.child=this.return=this.stateNode=this.type=this.elementType=null,this.index=0,this.refCleanup=this.ref=null,this.pendingProps=t,this.dependencies=this.memoizedState=this.updateQueue=this.memoizedProps=null,this.mode=o,this.subtreeFlags=this.flags=0,this.deletions=null,this.childLanes=this.lanes=0,this.alternate=null}function Pt(e,t,a,o){return new Dm(e,t,a,o)}function xs(e){return e=e.prototype,!(!e||!e.isReactComponent)}function On(e,t){var a=e.alternate;return a===null?(a=Pt(e.tag,t,e.key,e.mode),a.elementType=e.elementType,a.type=e.type,a.stateNode=e.stateNode,a.alternate=e,e.alternate=a):(a.pendingProps=t,a.type=e.type,a.flags=0,a.subtreeFlags=0,a.deletions=null),a.flags=e.flags&65011712,a.childLanes=e.childLanes,a.lanes=e.lanes,a.child=e.child,a.memoizedProps=e.memoizedProps,a.memoizedState=e.memoizedState,a.updateQueue=e.updateQueue,t=e.dependencies,a.dependencies=t===null?null:{lanes:t.lanes,firstContext:t.firstContext},a.sibling=e.sibling,a.index=e.index,a.ref=e.ref,a.refCleanup=e.refCleanup,a}function Ou(e,t){e.flags&=65011714;var a=e.alternate;return a===null?(e.childLanes=0,e.lanes=t,e.child=null,e.subtreeFlags=0,e.memoizedProps=null,e.memoizedState=null,e.updateQueue=null,e.dependencies=null,e.stateNode=null):(e.childLanes=a.childLanes,e.lanes=a.lanes,e.child=a.child,e.subtreeFlags=0,e.deletions=null,e.memoizedProps=a.memoizedProps,e.memoizedState=a.memoizedState,e.updateQueue=a.updateQueue,e.type=a.type,t=a.dependencies,e.dependencies=t===null?null:{lanes:t.lanes,firstContext:t.firstContext}),e}function Yl(e,t,a,o,l,i){var u=0;if(o=e,typeof e=="function")xs(e)&&(u=1);else if(typeof e=="string")u=Bg(e,a,ue.current)?26:e==="html"||e==="head"||e==="body"?27:5;else e:switch(e){case J:return e=Pt(31,a,t,l),e.elementType=J,e.lanes=i,e;case _:return ja(a.children,l,i,t);case v:u=8,l|=24;break;case T:return e=Pt(12,a,t,l|2),e.elementType=T,e.lanes=i,e;case D:return e=Pt(13,a,t,l),e.elementType=D,e.lanes=i,e;case L:return e=Pt(19,a,t,l),e.elementType=L,e.lanes=i,e;default:if(typeof e=="object"&&e!==null)switch(e.$$typeof){case k:u=10;break e;case O:u=9;break e;case H:u=11;break e;case C:u=14;break e;case Y:u=16,o=null;break e}u=29,a=Error(c(130,e===null?"null":typeof e,"")),o=null}return t=Pt(u,a,t,l),t.elementType=e,t.type=o,t.lanes=i,t}function ja(e,t,a,o){return e=Pt(7,e,o,t),e.lanes=a,e}function Ss(e,t,a){return e=Pt(6,e,null,t),e.lanes=a,e}function Ru(e){var t=Pt(18,null,null,0);return t.stateNode=e,t}function ws(e,t,a){return t=Pt(4,e.children!==null?e.children:[],e.key,t),t.lanes=a,t.stateNode={containerInfo:e.containerInfo,pendingChildren:null,implementation:e.implementation},t}var zu=new WeakMap;function dn(e,t){if(typeof e=="object"&&e!==null){var a=zu.get(e);return a!==void 0?a:(t={value:e,source:t,stack:we(t)},zu.set(e,t),t)}return{value:e,source:t,stack:we(t)}}var no=[],ao=0,ql=null,Xo=0,pn=[],hn=0,Fn=null,wn=1,Tn="";function Rn(e,t){no[ao++]=Xo,no[ao++]=ql,ql=e,Xo=t}function Bu(e,t,a){pn[hn++]=wn,pn[hn++]=Tn,pn[hn++]=Fn,Fn=e;var o=wn;e=Tn;var l=32-Fe(o)-1;o&=~(1<<l),a+=1;var i=32-Fe(t)+l;if(30<i){var u=l-l%5;i=(o&(1<<u)-1).toString(32),o>>=u,l-=u,wn=1<<32-Fe(t)+l|a<<l|o,Tn=i+e}else wn=1<<i|a<<l|o,Tn=e}function Ts(e){e.return!==null&&(Rn(e,1),Bu(e,1,0))}function Es(e){for(;e===ql;)ql=no[--ao],no[ao]=null,Xo=no[--ao],no[ao]=null;for(;e===Fn;)Fn=pn[--hn],pn[hn]=null,Tn=pn[--hn],pn[hn]=null,wn=pn[--hn],pn[hn]=null}function Hu(e,t){pn[hn++]=wn,pn[hn++]=Tn,pn[hn++]=Fn,wn=t.id,Tn=t.overflow,Fn=e}var Ot=null,at=null,Ue=!1,ea=null,fn=!1,Ns=Error(c(519));function ta(e){var t=Error(c(418,1<arguments.length&&arguments[1]!==void 0&&arguments[1]?"text":"HTML",""));throw Jo(dn(t,e)),Ns}function Gu(e){var t=e.stateNode,a=e.type,o=e.memoizedProps;switch(t[kt]=e,t[It]=o,a){case"dialog":Be("cancel",t),Be("close",t);break;case"iframe":case"object":case"embed":Be("load",t);break;case"video":case"audio":for(a=0;a<hl.length;a++)Be(hl[a],t);break;case"source":Be("error",t);break;case"img":case"image":case"link":Be("error",t),Be("load",t);break;case"details":Be("toggle",t);break;case"input":Be("invalid",t),Wc(t,o.value,o.defaultValue,o.checked,o.defaultChecked,o.type,o.name,!0);break;case"select":Be("invalid",t);break;case"textarea":Be("invalid",t),Fc(t,o.value,o.defaultValue,o.children)}a=o.children,typeof a!="string"&&typeof a!="number"&&typeof a!="bigint"||t.textContent===""+a||o.suppressHydrationWarning===!0||th(t.textContent,a)?(o.popover!=null&&(Be("beforetoggle",t),Be("toggle",t)),o.onScroll!=null&&Be("scroll",t),o.onScrollEnd!=null&&Be("scrollend",t),o.onClick!=null&&(t.onclick=Dn),t=!0):t=!1,t||ta(e,!0)}function Uu(e){for(Ot=e.return;Ot;)switch(Ot.tag){case 5:case 31:case 13:fn=!1;return;case 27:case 3:fn=!0;return;default:Ot=Ot.return}}function oo(e){if(e!==Ot)return!1;if(!Ue)return Uu(e),Ue=!0,!1;var t=e.tag,a;if((a=t!==3&&t!==27)&&((a=t===5)&&(a=e.type,a=!(a!=="form"&&a!=="button")||Xr(e.type,e.memoizedProps)),a=!a),a&&at&&ta(e),Uu(e),t===13){if(e=e.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(c(317));at=uh(e)}else if(t===31){if(e=e.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(c(317));at=uh(e)}else t===27?(t=at,ma(e.type)?(e=Kr,Kr=null,at=e):at=t):at=Ot?gn(e.stateNode.nextSibling):null;return!0}function Ma(){at=Ot=null,Ue=!1}function As(){var e=ea;return e!==null&&(Qt===null?Qt=e:Qt.push.apply(Qt,e),ea=null),e}function Jo(e){ea===null?ea=[e]:ea.push(e)}var Cs=pe(null),Da=null,zn=null;function na(e,t,a){te(Cs,t._currentValue),t._currentValue=a}function Bn(e){e._currentValue=Cs.current,ae(Cs)}function js(e,t,a){for(;e!==null;){var o=e.alternate;if((e.childLanes&t)!==t?(e.childLanes|=t,o!==null&&(o.childLanes|=t)):o!==null&&(o.childLanes&t)!==t&&(o.childLanes|=t),e===a)break;e=e.return}}function Ms(e,t,a,o){var l=e.child;for(l!==null&&(l.return=e);l!==null;){var i=l.dependencies;if(i!==null){var u=l.child;i=i.firstContext;e:for(;i!==null;){var m=i;i=l;for(var N=0;N<t.length;N++)if(m.context===t[N]){i.lanes|=a,m=i.alternate,m!==null&&(m.lanes|=a),js(i.return,a,e),o||(u=null);break e}i=m.next}}else if(l.tag===18){if(u=l.return,u===null)throw Error(c(341));u.lanes|=a,i=u.alternate,i!==null&&(i.lanes|=a),js(u,a,e),u=null}else u=l.child;if(u!==null)u.return=l;else for(u=l;u!==null;){if(u===e){u=null;break}if(l=u.sibling,l!==null){l.return=u.return,u=l;break}u=u.return}l=u}}function lo(e,t,a,o){e=null;for(var l=t,i=!1;l!==null;){if(!i){if((l.flags&524288)!==0)i=!0;else if((l.flags&262144)!==0)break}if(l.tag===10){var u=l.alternate;if(u===null)throw Error(c(387));if(u=u.memoizedProps,u!==null){var m=l.type;Wt(l.pendingProps.value,u.value)||(e!==null?e.push(m):e=[m])}}else if(l===Ce.current){if(u=l.alternate,u===null)throw Error(c(387));u.memoizedState.memoizedState!==l.memoizedState.memoizedState&&(e!==null?e.push(bl):e=[bl])}l=l.return}e!==null&&Ms(t,e,a,o),t.flags|=262144}function Il(e){for(e=e.firstContext;e!==null;){if(!Wt(e.context._currentValue,e.memoizedValue))return!0;e=e.next}return!1}function ka(e){Da=e,zn=null,e=e.dependencies,e!==null&&(e.firstContext=null)}function Rt(e){return Lu(Da,e)}function Xl(e,t){return Da===null&&ka(e),Lu(e,t)}function Lu(e,t){var a=t._currentValue;if(t={context:t,memoizedValue:a,next:null},zn===null){if(e===null)throw Error(c(308));zn=t,e.dependencies={lanes:0,firstContext:t},e.flags|=524288}else zn=zn.next=t;return a}var km=typeof AbortController<"u"?AbortController:function(){var e=[],t=this.signal={aborted:!1,addEventListener:function(a,o){e.push(o)}};this.abort=function(){t.aborted=!0,e.forEach(function(a){return a()})}},Om=r.unstable_scheduleCallback,Rm=r.unstable_NormalPriority,_t={$$typeof:k,Consumer:null,Provider:null,_currentValue:null,_currentValue2:null,_threadCount:0};function Ds(){return{controller:new km,data:new Map,refCount:0}}function Zo(e){e.refCount--,e.refCount===0&&Om(Rm,function(){e.controller.abort()})}var Vo=null,ks=0,io=0,so=null;function zm(e,t){if(Vo===null){var a=Vo=[];ks=0,io=zr(),so={status:"pending",value:void 0,then:function(o){a.push(o)}}}return ks++,t.then(Yu,Yu),t}function Yu(){if(--ks===0&&Vo!==null){so!==null&&(so.status="fulfilled");var e=Vo;Vo=null,io=0,so=null;for(var t=0;t<e.length;t++)(0,e[t])()}}function Bm(e,t){var a=[],o={status:"pending",value:null,reason:null,then:function(l){a.push(l)}};return e.then(function(){o.status="fulfilled",o.value=t;for(var l=0;l<a.length;l++)(0,a[l])(t)},function(l){for(o.status="rejected",o.reason=l,l=0;l<a.length;l++)(0,a[l])(void 0)}),o}var qu=R.S;R.S=function(e,t){Ep=fe(),typeof t=="object"&&t!==null&&typeof t.then=="function"&&zm(e,t),qu!==null&&qu(e,t)};var Oa=pe(null);function Os(){var e=Oa.current;return e!==null?e:et.pooledCache}function Jl(e,t){t===null?te(Oa,Oa.current):te(Oa,t.pool)}function Iu(){var e=Os();return e===null?null:{parent:_t._currentValue,pool:e}}var ro=Error(c(460)),Rs=Error(c(474)),Zl=Error(c(542)),Vl={then:function(){}};function Xu(e){return e=e.status,e==="fulfilled"||e==="rejected"}function Ju(e,t,a){switch(a=e[a],a===void 0?e.push(t):a!==t&&(t.then(Dn,Dn),t=a),t.status){case"fulfilled":return t.value;case"rejected":throw e=t.reason,Vu(e),e;default:if(typeof t.status=="string")t.then(Dn,Dn);else{if(e=et,e!==null&&100<e.shellSuspendCounter)throw Error(c(482));e=t,e.status="pending",e.then(function(o){if(t.status==="pending"){var l=t;l.status="fulfilled",l.value=o}},function(o){if(t.status==="pending"){var l=t;l.status="rejected",l.reason=o}})}switch(t.status){case"fulfilled":return t.value;case"rejected":throw e=t.reason,Vu(e),e}throw za=t,ro}}function Ra(e){try{var t=e._init;return t(e._payload)}catch(a){throw a!==null&&typeof a=="object"&&typeof a.then=="function"?(za=a,ro):a}}var za=null;function Zu(){if(za===null)throw Error(c(459));var e=za;return za=null,e}function Vu(e){if(e===ro||e===Zl)throw Error(c(483))}var co=null,Qo=0;function Ql(e){var t=Qo;return Qo+=1,co===null&&(co=[]),Ju(co,e,t)}function Ko(e,t){t=t.props.ref,e.ref=t!==void 0?t:null}function Kl(e,t){throw t.$$typeof===S?Error(c(525)):(e=Object.prototype.toString.call(t),Error(c(31,e==="[object Object]"?"object with keys {"+Object.keys(t).join(", ")+"}":e)))}function Qu(e){function t(B,M){if(e){var G=B.deletions;G===null?(B.deletions=[M],B.flags|=16):G.push(M)}}function a(B,M){if(!e)return null;for(;M!==null;)t(B,M),M=M.sibling;return null}function o(B){for(var M=new Map;B!==null;)B.key!==null?M.set(B.key,B):M.set(B.index,B),B=B.sibling;return M}function l(B,M){return B=On(B,M),B.index=0,B.sibling=null,B}function i(B,M,G){return B.index=G,e?(G=B.alternate,G!==null?(G=G.index,G<M?(B.flags|=67108866,M):G):(B.flags|=67108866,M)):(B.flags|=1048576,M)}function u(B){return e&&B.alternate===null&&(B.flags|=67108866),B}function m(B,M,G,Q){return M===null||M.tag!==6?(M=Ss(G,B.mode,Q),M.return=B,M):(M=l(M,G),M.return=B,M)}function N(B,M,G,Q){var be=G.type;return be===_?V(B,M,G.props.children,Q,G.key):M!==null&&(M.elementType===be||typeof be=="object"&&be!==null&&be.$$typeof===Y&&Ra(be)===M.type)?(M=l(M,G.props),Ko(M,G),M.return=B,M):(M=Yl(G.type,G.key,G.props,null,B.mode,Q),Ko(M,G),M.return=B,M)}function U(B,M,G,Q){return M===null||M.tag!==4||M.stateNode.containerInfo!==G.containerInfo||M.stateNode.implementation!==G.implementation?(M=ws(G,B.mode,Q),M.return=B,M):(M=l(M,G.children||[]),M.return=B,M)}function V(B,M,G,Q,be){return M===null||M.tag!==7?(M=ja(G,B.mode,Q,be),M.return=B,M):(M=l(M,G),M.return=B,M)}function W(B,M,G){if(typeof M=="string"&&M!==""||typeof M=="number"||typeof M=="bigint")return M=Ss(""+M,B.mode,G),M.return=B,M;if(typeof M=="object"&&M!==null){switch(M.$$typeof){case j:return G=Yl(M.type,M.key,M.props,null,B.mode,G),Ko(G,M),G.return=B,G;case A:return M=ws(M,B.mode,G),M.return=B,M;case Y:return M=Ra(M),W(B,M,G)}if(le(M)||Z(M))return M=ja(M,B.mode,G,null),M.return=B,M;if(typeof M.then=="function")return W(B,Ql(M),G);if(M.$$typeof===k)return W(B,Xl(B,M),G);Kl(B,M)}return null}function q(B,M,G,Q){var be=M!==null?M.key:null;if(typeof G=="string"&&G!==""||typeof G=="number"||typeof G=="bigint")return be!==null?null:m(B,M,""+G,Q);if(typeof G=="object"&&G!==null){switch(G.$$typeof){case j:return G.key===be?N(B,M,G,Q):null;case A:return G.key===be?U(B,M,G,Q):null;case Y:return G=Ra(G),q(B,M,G,Q)}if(le(G)||Z(G))return be!==null?null:V(B,M,G,Q,null);if(typeof G.then=="function")return q(B,M,Ql(G),Q);if(G.$$typeof===k)return q(B,M,Xl(B,G),Q);Kl(B,G)}return null}function X(B,M,G,Q,be){if(typeof Q=="string"&&Q!==""||typeof Q=="number"||typeof Q=="bigint")return B=B.get(G)||null,m(M,B,""+Q,be);if(typeof Q=="object"&&Q!==null){switch(Q.$$typeof){case j:return B=B.get(Q.key===null?G:Q.key)||null,N(M,B,Q,be);case A:return B=B.get(Q.key===null?G:Q.key)||null,U(M,B,Q,be);case Y:return Q=Ra(Q),X(B,M,G,Q,be)}if(le(Q)||Z(Q))return B=B.get(G)||null,V(M,B,Q,be,null);if(typeof Q.then=="function")return X(B,M,G,Ql(Q),be);if(Q.$$typeof===k)return X(B,M,G,Xl(M,Q),be);Kl(M,Q)}return null}function ce(B,M,G,Q){for(var be=null,Ie=null,de=M,De=M=0,Ge=null;de!==null&&De<G.length;De++){de.index>De?(Ge=de,de=null):Ge=de.sibling;var Xe=q(B,de,G[De],Q);if(Xe===null){de===null&&(de=Ge);break}e&&de&&Xe.alternate===null&&t(B,de),M=i(Xe,M,De),Ie===null?be=Xe:Ie.sibling=Xe,Ie=Xe,de=Ge}if(De===G.length)return a(B,de),Ue&&Rn(B,De),be;if(de===null){for(;De<G.length;De++)de=W(B,G[De],Q),de!==null&&(M=i(de,M,De),Ie===null?be=de:Ie.sibling=de,Ie=de);return Ue&&Rn(B,De),be}for(de=o(de);De<G.length;De++)Ge=X(de,B,De,G[De],Q),Ge!==null&&(e&&Ge.alternate!==null&&de.delete(Ge.key===null?De:Ge.key),M=i(Ge,M,De),Ie===null?be=Ge:Ie.sibling=Ge,Ie=Ge);return e&&de.forEach(function(_a){return t(B,_a)}),Ue&&Rn(B,De),be}function ve(B,M,G,Q){if(G==null)throw Error(c(151));for(var be=null,Ie=null,de=M,De=M=0,Ge=null,Xe=G.next();de!==null&&!Xe.done;De++,Xe=G.next()){de.index>De?(Ge=de,de=null):Ge=de.sibling;var _a=q(B,de,Xe.value,Q);if(_a===null){de===null&&(de=Ge);break}e&&de&&_a.alternate===null&&t(B,de),M=i(_a,M,De),Ie===null?be=_a:Ie.sibling=_a,Ie=_a,de=Ge}if(Xe.done)return a(B,de),Ue&&Rn(B,De),be;if(de===null){for(;!Xe.done;De++,Xe=G.next())Xe=W(B,Xe.value,Q),Xe!==null&&(M=i(Xe,M,De),Ie===null?be=Xe:Ie.sibling=Xe,Ie=Xe);return Ue&&Rn(B,De),be}for(de=o(de);!Xe.done;De++,Xe=G.next())Xe=X(de,B,De,Xe.value,Q),Xe!==null&&(e&&Xe.alternate!==null&&de.delete(Xe.key===null?De:Xe.key),M=i(Xe,M,De),Ie===null?be=Xe:Ie.sibling=Xe,Ie=Xe);return e&&de.forEach(function(Vg){return t(B,Vg)}),Ue&&Rn(B,De),be}function Pe(B,M,G,Q){if(typeof G=="object"&&G!==null&&G.type===_&&G.key===null&&(G=G.props.children),typeof G=="object"&&G!==null){switch(G.$$typeof){case j:e:{for(var be=G.key;M!==null;){if(M.key===be){if(be=G.type,be===_){if(M.tag===7){a(B,M.sibling),Q=l(M,G.props.children),Q.return=B,B=Q;break e}}else if(M.elementType===be||typeof be=="object"&&be!==null&&be.$$typeof===Y&&Ra(be)===M.type){a(B,M.sibling),Q=l(M,G.props),Ko(Q,G),Q.return=B,B=Q;break e}a(B,M);break}else t(B,M);M=M.sibling}G.type===_?(Q=ja(G.props.children,B.mode,Q,G.key),Q.return=B,B=Q):(Q=Yl(G.type,G.key,G.props,null,B.mode,Q),Ko(Q,G),Q.return=B,B=Q)}return u(B);case A:e:{for(be=G.key;M!==null;){if(M.key===be)if(M.tag===4&&M.stateNode.containerInfo===G.containerInfo&&M.stateNode.implementation===G.implementation){a(B,M.sibling),Q=l(M,G.children||[]),Q.return=B,B=Q;break e}else{a(B,M);break}else t(B,M);M=M.sibling}Q=ws(G,B.mode,Q),Q.return=B,B=Q}return u(B);case Y:return G=Ra(G),Pe(B,M,G,Q)}if(le(G))return ce(B,M,G,Q);if(Z(G)){if(be=Z(G),typeof be!="function")throw Error(c(150));return G=be.call(G),ve(B,M,G,Q)}if(typeof G.then=="function")return Pe(B,M,Ql(G),Q);if(G.$$typeof===k)return Pe(B,M,Xl(B,G),Q);Kl(B,G)}return typeof G=="string"&&G!==""||typeof G=="number"||typeof G=="bigint"?(G=""+G,M!==null&&M.tag===6?(a(B,M.sibling),Q=l(M,G),Q.return=B,B=Q):(a(B,M),Q=Ss(G,B.mode,Q),Q.return=B,B=Q),u(B)):a(B,M)}return function(B,M,G,Q){try{Qo=0;var be=Pe(B,M,G,Q);return co=null,be}catch(de){if(de===ro||de===Zl)throw de;var Ie=Pt(29,de,null,B.mode);return Ie.lanes=Q,Ie.return=B,Ie}finally{}}}var Ba=Qu(!0),Ku=Qu(!1),aa=!1;function zs(e){e.updateQueue={baseState:e.memoizedState,firstBaseUpdate:null,lastBaseUpdate:null,shared:{pending:null,lanes:0,hiddenCallbacks:null},callbacks:null}}function Bs(e,t){e=e.updateQueue,t.updateQueue===e&&(t.updateQueue={baseState:e.baseState,firstBaseUpdate:e.firstBaseUpdate,lastBaseUpdate:e.lastBaseUpdate,shared:e.shared,callbacks:null})}function oa(e){return{lane:e,tag:0,payload:null,callback:null,next:null}}function la(e,t,a){var o=e.updateQueue;if(o===null)return null;if(o=o.shared,(Je&2)!==0){var l=o.pending;return l===null?t.next=t:(t.next=l.next,l.next=t),o.pending=t,t=Ll(e),ku(e,null,a),t}return Ul(e,o,t,a),Ll(e)}function $o(e,t,a){if(t=t.updateQueue,t!==null&&(t=t.shared,(a&4194048)!==0)){var o=t.lanes;o&=e.pendingLanes,a|=o,t.lanes=a,Lc(e,a)}}function Hs(e,t){var a=e.updateQueue,o=e.alternate;if(o!==null&&(o=o.updateQueue,a===o)){var l=null,i=null;if(a=a.firstBaseUpdate,a!==null){do{var u={lane:a.lane,tag:a.tag,payload:a.payload,callback:null,next:null};i===null?l=i=u:i=i.next=u,a=a.next}while(a!==null);i===null?l=i=t:i=i.next=t}else l=i=t;a={baseState:o.baseState,firstBaseUpdate:l,lastBaseUpdate:i,shared:o.shared,callbacks:o.callbacks},e.updateQueue=a;return}e=a.lastBaseUpdate,e===null?a.firstBaseUpdate=t:e.next=t,a.lastBaseUpdate=t}var Gs=!1;function Wo(){if(Gs){var e=so;if(e!==null)throw e}}function Po(e,t,a,o){Gs=!1;var l=e.updateQueue;aa=!1;var i=l.firstBaseUpdate,u=l.lastBaseUpdate,m=l.shared.pending;if(m!==null){l.shared.pending=null;var N=m,U=N.next;N.next=null,u===null?i=U:u.next=U,u=N;var V=e.alternate;V!==null&&(V=V.updateQueue,m=V.lastBaseUpdate,m!==u&&(m===null?V.firstBaseUpdate=U:m.next=U,V.lastBaseUpdate=N))}if(i!==null){var W=l.baseState;u=0,V=U=N=null,m=i;do{var q=m.lane&-536870913,X=q!==m.lane;if(X?(He&q)===q:(o&q)===q){q!==0&&q===io&&(Gs=!0),V!==null&&(V=V.next={lane:0,tag:m.tag,payload:m.payload,callback:null,next:null});e:{var ce=e,ve=m;q=t;var Pe=a;switch(ve.tag){case 1:if(ce=ve.payload,typeof ce=="function"){W=ce.call(Pe,W,q);break e}W=ce;break e;case 3:ce.flags=ce.flags&-65537|128;case 0:if(ce=ve.payload,q=typeof ce=="function"?ce.call(Pe,W,q):ce,q==null)break e;W=x({},W,q);break e;case 2:aa=!0}}q=m.callback,q!==null&&(e.flags|=64,X&&(e.flags|=8192),X=l.callbacks,X===null?l.callbacks=[q]:X.push(q))}else X={lane:q,tag:m.tag,payload:m.payload,callback:m.callback,next:null},V===null?(U=V=X,N=W):V=V.next=X,u|=q;if(m=m.next,m===null){if(m=l.shared.pending,m===null)break;X=m,m=X.next,X.next=null,l.lastBaseUpdate=X,l.shared.pending=null}}while(!0);V===null&&(N=W),l.baseState=N,l.firstBaseUpdate=U,l.lastBaseUpdate=V,i===null&&(l.shared.lanes=0),ua|=u,e.lanes=u,e.memoizedState=W}}function $u(e,t){if(typeof e!="function")throw Error(c(191,e));e.call(t)}function Wu(e,t){var a=e.callbacks;if(a!==null)for(e.callbacks=null,e=0;e<a.length;e++)$u(a[e],t)}var uo=pe(null),$l=pe(0);function Pu(e,t){e=Jn,te($l,e),te(uo,t),Jn=e|t.baseLanes}function Us(){te($l,Jn),te(uo,uo.current)}function Ls(){Jn=$l.current,ae(uo),ae($l)}var Ft=pe(null),mn=null;function ia(e){var t=e.alternate;te(mt,mt.current&1),te(Ft,e),mn===null&&(t===null||uo.current!==null||t.memoizedState!==null)&&(mn=e)}function Ys(e){te(mt,mt.current),te(Ft,e),mn===null&&(mn=e)}function Fu(e){e.tag===22?(te(mt,mt.current),te(Ft,e),mn===null&&(mn=e)):sa()}function sa(){te(mt,mt.current),te(Ft,Ft.current)}function en(e){ae(Ft),mn===e&&(mn=null),ae(mt)}var mt=pe(0);function Wl(e){for(var t=e;t!==null;){if(t.tag===13){var a=t.memoizedState;if(a!==null&&(a=a.dehydrated,a===null||Vr(a)||Qr(a)))return t}else if(t.tag===19&&(t.memoizedProps.revealOrder==="forwards"||t.memoizedProps.revealOrder==="backwards"||t.memoizedProps.revealOrder==="unstable_legacy-backwards"||t.memoizedProps.revealOrder==="together")){if((t.flags&128)!==0)return t}else if(t.child!==null){t.child.return=t,t=t.child;continue}if(t===e)break;for(;t.sibling===null;){if(t.return===null||t.return===e)return null;t=t.return}t.sibling.return=t.return,t=t.sibling}return null}var Hn=0,je=null,$e=null,xt=null,Pl=!1,po=!1,Ha=!1,Fl=0,Fo=0,ho=null,Hm=0;function dt(){throw Error(c(321))}function qs(e,t){if(t===null)return!1;for(var a=0;a<t.length&&a<e.length;a++)if(!Wt(e[a],t[a]))return!1;return!0}function Is(e,t,a,o,l,i){return Hn=i,je=t,t.memoizedState=null,t.updateQueue=null,t.lanes=0,R.H=e===null||e.memoizedState===null?Bd:or,Ha=!1,i=a(o,l),Ha=!1,po&&(i=td(t,a,o,l)),ed(e),i}function ed(e){R.H=nl;var t=$e!==null&&$e.next!==null;if(Hn=0,xt=$e=je=null,Pl=!1,Fo=0,ho=null,t)throw Error(c(300));e===null||St||(e=e.dependencies,e!==null&&Il(e)&&(St=!0))}function td(e,t,a,o){je=e;var l=0;do{if(po&&(ho=null),Fo=0,po=!1,25<=l)throw Error(c(301));if(l+=1,xt=$e=null,e.updateQueue!=null){var i=e.updateQueue;i.lastEffect=null,i.events=null,i.stores=null,i.memoCache!=null&&(i.memoCache.index=0)}R.H=Hd,i=t(a,o)}while(po);return i}function Gm(){var e=R.H,t=e.useState()[0];return t=typeof t.then=="function"?el(t):t,e=e.useState()[0],($e!==null?$e.memoizedState:null)!==e&&(je.flags|=1024),t}function Xs(){var e=Fl!==0;return Fl=0,e}function Js(e,t,a){t.updateQueue=e.updateQueue,t.flags&=-2053,e.lanes&=~a}function Zs(e){if(Pl){for(e=e.memoizedState;e!==null;){var t=e.queue;t!==null&&(t.pending=null),e=e.next}Pl=!1}Hn=0,xt=$e=je=null,po=!1,Fo=Fl=0,ho=null}function Yt(){var e={memoizedState:null,baseState:null,baseQueue:null,queue:null,next:null};return xt===null?je.memoizedState=xt=e:xt=xt.next=e,xt}function gt(){if($e===null){var e=je.alternate;e=e!==null?e.memoizedState:null}else e=$e.next;var t=xt===null?je.memoizedState:xt.next;if(t!==null)xt=t,$e=e;else{if(e===null)throw je.alternate===null?Error(c(467)):Error(c(310));$e=e,e={memoizedState:$e.memoizedState,baseState:$e.baseState,baseQueue:$e.baseQueue,queue:$e.queue,next:null},xt===null?je.memoizedState=xt=e:xt=xt.next=e}return xt}function ei(){return{lastEffect:null,events:null,stores:null,memoCache:null}}function el(e){var t=Fo;return Fo+=1,ho===null&&(ho=[]),e=Ju(ho,e,t),t=je,(xt===null?t.memoizedState:xt.next)===null&&(t=t.alternate,R.H=t===null||t.memoizedState===null?Bd:or),e}function ti(e){if(e!==null&&typeof e=="object"){if(typeof e.then=="function")return el(e);if(e.$$typeof===k)return Rt(e)}throw Error(c(438,String(e)))}function Vs(e){var t=null,a=je.updateQueue;if(a!==null&&(t=a.memoCache),t==null){var o=je.alternate;o!==null&&(o=o.updateQueue,o!==null&&(o=o.memoCache,o!=null&&(t={data:o.data.map(function(l){return l.slice()}),index:0})))}if(t==null&&(t={data:[],index:0}),a===null&&(a=ei(),je.updateQueue=a),a.memoCache=t,a=t.data[t.index],a===void 0)for(a=t.data[t.index]=Array(e),o=0;o<e;o++)a[o]=I;return t.index++,a}function Gn(e,t){return typeof t=="function"?t(e):t}function ni(e){var t=gt();return Qs(t,$e,e)}function Qs(e,t,a){var o=e.queue;if(o===null)throw Error(c(311));o.lastRenderedReducer=a;var l=e.baseQueue,i=o.pending;if(i!==null){if(l!==null){var u=l.next;l.next=i.next,i.next=u}t.baseQueue=l=i,o.pending=null}if(i=e.baseState,l===null)e.memoizedState=i;else{t=l.next;var m=u=null,N=null,U=t,V=!1;do{var W=U.lane&-536870913;if(W!==U.lane?(He&W)===W:(Hn&W)===W){var q=U.revertLane;if(q===0)N!==null&&(N=N.next={lane:0,revertLane:0,gesture:null,action:U.action,hasEagerState:U.hasEagerState,eagerState:U.eagerState,next:null}),W===io&&(V=!0);else if((Hn&q)===q){U=U.next,q===io&&(V=!0);continue}else W={lane:0,revertLane:U.revertLane,gesture:null,action:U.action,hasEagerState:U.hasEagerState,eagerState:U.eagerState,next:null},N===null?(m=N=W,u=i):N=N.next=W,je.lanes|=q,ua|=q;W=U.action,Ha&&a(i,W),i=U.hasEagerState?U.eagerState:a(i,W)}else q={lane:W,revertLane:U.revertLane,gesture:U.gesture,action:U.action,hasEagerState:U.hasEagerState,eagerState:U.eagerState,next:null},N===null?(m=N=q,u=i):N=N.next=q,je.lanes|=W,ua|=W;U=U.next}while(U!==null&&U!==t);if(N===null?u=i:N.next=m,!Wt(i,e.memoizedState)&&(St=!0,V&&(a=so,a!==null)))throw a;e.memoizedState=i,e.baseState=u,e.baseQueue=N,o.lastRenderedState=i}return l===null&&(o.lanes=0),[e.memoizedState,o.dispatch]}function Ks(e){var t=gt(),a=t.queue;if(a===null)throw Error(c(311));a.lastRenderedReducer=e;var o=a.dispatch,l=a.pending,i=t.memoizedState;if(l!==null){a.pending=null;var u=l=l.next;do i=e(i,u.action),u=u.next;while(u!==l);Wt(i,t.memoizedState)||(St=!0),t.memoizedState=i,t.baseQueue===null&&(t.baseState=i),a.lastRenderedState=i}return[i,o]}function nd(e,t,a){var o=je,l=gt(),i=Ue;if(i){if(a===void 0)throw Error(c(407));a=a()}else a=t();var u=!Wt(($e||l).memoizedState,a);if(u&&(l.memoizedState=a,St=!0),l=l.queue,Ps(ld.bind(null,o,l,e),[e]),l.getSnapshot!==t||u||xt!==null&&xt.memoizedState.tag&1){if(o.flags|=2048,fo(9,{destroy:void 0},od.bind(null,o,l,a,t),null),et===null)throw Error(c(349));i||(Hn&127)!==0||ad(o,t,a)}return a}function ad(e,t,a){e.flags|=16384,e={getSnapshot:t,value:a},t=je.updateQueue,t===null?(t=ei(),je.updateQueue=t,t.stores=[e]):(a=t.stores,a===null?t.stores=[e]:a.push(e))}function od(e,t,a,o){t.value=a,t.getSnapshot=o,id(t)&&sd(e)}function ld(e,t,a){return a(function(){id(t)&&sd(e)})}function id(e){var t=e.getSnapshot;e=e.value;try{var a=t();return!Wt(e,a)}catch{return!0}}function sd(e){var t=Ca(e,2);t!==null&&Kt(t,e,2)}function $s(e){var t=Yt();if(typeof e=="function"){var a=e;if(e=a(),Ha){it(!0);try{a()}finally{it(!1)}}}return t.memoizedState=t.baseState=e,t.queue={pending:null,lanes:0,dispatch:null,lastRenderedReducer:Gn,lastRenderedState:e},t}function rd(e,t,a,o){return e.baseState=a,Qs(e,$e,typeof o=="function"?o:Gn)}function Um(e,t,a,o,l){if(li(e))throw Error(c(485));if(e=t.action,e!==null){var i={payload:l,action:e,next:null,isTransition:!0,status:"pending",value:null,reason:null,listeners:[],then:function(u){i.listeners.push(u)}};R.T!==null?a(!0):i.isTransition=!1,o(i),a=t.pending,a===null?(i.next=t.pending=i,cd(t,i)):(i.next=a.next,t.pending=a.next=i)}}function cd(e,t){var a=t.action,o=t.payload,l=e.state;if(t.isTransition){var i=R.T,u={};R.T=u;try{var m=a(l,o),N=R.S;N!==null&&N(u,m),ud(e,t,m)}catch(U){Ws(e,t,U)}finally{i!==null&&u.types!==null&&(i.types=u.types),R.T=i}}else try{i=a(l,o),ud(e,t,i)}catch(U){Ws(e,t,U)}}function ud(e,t,a){a!==null&&typeof a=="object"&&typeof a.then=="function"?a.then(function(o){dd(e,t,o)},function(o){return Ws(e,t,o)}):dd(e,t,a)}function dd(e,t,a){t.status="fulfilled",t.value=a,pd(t),e.state=a,t=e.pending,t!==null&&(a=t.next,a===t?e.pending=null:(a=a.next,t.next=a,cd(e,a)))}function Ws(e,t,a){var o=e.pending;if(e.pending=null,o!==null){o=o.next;do t.status="rejected",t.reason=a,pd(t),t=t.next;while(t!==o)}e.action=null}function pd(e){e=e.listeners;for(var t=0;t<e.length;t++)(0,e[t])()}function hd(e,t){return t}function fd(e,t){if(Ue){var a=et.formState;if(a!==null){e:{var o=je;if(Ue){if(at){t:{for(var l=at,i=fn;l.nodeType!==8;){if(!i){l=null;break t}if(l=gn(l.nextSibling),l===null){l=null;break t}}i=l.data,l=i==="F!"||i==="F"?l:null}if(l){at=gn(l.nextSibling),o=l.data==="F!";break e}}ta(o)}o=!1}o&&(t=a[0])}}return a=Yt(),a.memoizedState=a.baseState=t,o={pending:null,lanes:0,dispatch:null,lastRenderedReducer:hd,lastRenderedState:t},a.queue=o,a=Od.bind(null,je,o),o.dispatch=a,o=$s(!1),i=ar.bind(null,je,!1,o.queue),o=Yt(),l={state:t,dispatch:null,action:e,pending:null},o.queue=l,a=Um.bind(null,je,l,i,a),l.dispatch=a,o.memoizedState=e,[t,a,!1]}function md(e){var t=gt();return gd(t,$e,e)}function gd(e,t,a){if(t=Qs(e,t,hd)[0],e=ni(Gn)[0],typeof t=="object"&&t!==null&&typeof t.then=="function")try{var o=el(t)}catch(u){throw u===ro?Zl:u}else o=t;t=gt();var l=t.queue,i=l.dispatch;return a!==t.memoizedState&&(je.flags|=2048,fo(9,{destroy:void 0},Lm.bind(null,l,a),null)),[o,i,e]}function Lm(e,t){e.action=t}function yd(e){var t=gt(),a=$e;if(a!==null)return gd(t,a,e);gt(),t=t.memoizedState,a=gt();var o=a.queue.dispatch;return a.memoizedState=e,[t,o,!1]}function fo(e,t,a,o){return e={tag:e,create:a,deps:o,inst:t,next:null},t=je.updateQueue,t===null&&(t=ei(),je.updateQueue=t),a=t.lastEffect,a===null?t.lastEffect=e.next=e:(o=a.next,a.next=e,e.next=o,t.lastEffect=e),e}function bd(){return gt().memoizedState}function ai(e,t,a,o){var l=Yt();je.flags|=e,l.memoizedState=fo(1|t,{destroy:void 0},a,o===void 0?null:o)}function oi(e,t,a,o){var l=gt();o=o===void 0?null:o;var i=l.memoizedState.inst;$e!==null&&o!==null&&qs(o,$e.memoizedState.deps)?l.memoizedState=fo(t,i,a,o):(je.flags|=e,l.memoizedState=fo(1|t,i,a,o))}function vd(e,t){ai(8390656,8,e,t)}function Ps(e,t){oi(2048,8,e,t)}function Ym(e){je.flags|=4;var t=je.updateQueue;if(t===null)t=ei(),je.updateQueue=t,t.events=[e];else{var a=t.events;a===null?t.events=[e]:a.push(e)}}function _d(e){var t=gt().memoizedState;return Ym({ref:t,nextImpl:e}),function(){if((Je&2)!==0)throw Error(c(440));return t.impl.apply(void 0,arguments)}}function xd(e,t){return oi(4,2,e,t)}function Sd(e,t){return oi(4,4,e,t)}function wd(e,t){if(typeof t=="function"){e=e();var a=t(e);return function(){typeof a=="function"?a():t(null)}}if(t!=null)return e=e(),t.current=e,function(){t.current=null}}function Td(e,t,a){a=a!=null?a.concat([e]):null,oi(4,4,wd.bind(null,t,e),a)}function Fs(){}function Ed(e,t){var a=gt();t=t===void 0?null:t;var o=a.memoizedState;return t!==null&&qs(t,o[1])?o[0]:(a.memoizedState=[e,t],e)}function Nd(e,t){var a=gt();t=t===void 0?null:t;var o=a.memoizedState;if(t!==null&&qs(t,o[1]))return o[0];if(o=e(),Ha){it(!0);try{e()}finally{it(!1)}}return a.memoizedState=[o,t],o}function er(e,t,a){return a===void 0||(Hn&1073741824)!==0&&(He&261930)===0?e.memoizedState=t:(e.memoizedState=a,e=Ap(),je.lanes|=e,ua|=e,a)}function Ad(e,t,a,o){return Wt(a,t)?a:uo.current!==null?(e=er(e,a,o),Wt(e,t)||(St=!0),e):(Hn&42)===0||(Hn&1073741824)!==0&&(He&261930)===0?(St=!0,e.memoizedState=a):(e=Ap(),je.lanes|=e,ua|=e,t)}function Cd(e,t,a,o,l){var i=z.p;z.p=i!==0&&8>i?i:8;var u=R.T,m={};R.T=m,ar(e,!1,t,a);try{var N=l(),U=R.S;if(U!==null&&U(m,N),N!==null&&typeof N=="object"&&typeof N.then=="function"){var V=Bm(N,o);tl(e,t,V,an(e))}else tl(e,t,o,an(e))}catch(W){tl(e,t,{then:function(){},status:"rejected",reason:W},an())}finally{z.p=i,u!==null&&m.types!==null&&(u.types=m.types),R.T=u}}function qm(){}function tr(e,t,a,o){if(e.tag!==5)throw Error(c(476));var l=jd(e).queue;Cd(e,l,t,$,a===null?qm:function(){return Md(e),a(o)})}function jd(e){var t=e.memoizedState;if(t!==null)return t;t={memoizedState:$,baseState:$,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:Gn,lastRenderedState:$},next:null};var a={};return t.next={memoizedState:a,baseState:a,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:Gn,lastRenderedState:a},next:null},e.memoizedState=t,e=e.alternate,e!==null&&(e.memoizedState=t),t}function Md(e){var t=jd(e);t.next===null&&(t=e.alternate.memoizedState),tl(e,t.next.queue,{},an())}function nr(){return Rt(bl)}function Dd(){return gt().memoizedState}function kd(){return gt().memoizedState}function Im(e){for(var t=e.return;t!==null;){switch(t.tag){case 24:case 3:var a=an();e=oa(a);var o=la(t,e,a);o!==null&&(Kt(o,t,a),$o(o,t,a)),t={cache:Ds()},e.payload=t;return}t=t.return}}function Xm(e,t,a){var o=an();a={lane:o,revertLane:0,gesture:null,action:a,hasEagerState:!1,eagerState:null,next:null},li(e)?Rd(t,a):(a=_s(e,t,a,o),a!==null&&(Kt(a,e,o),zd(a,t,o)))}function Od(e,t,a){var o=an();tl(e,t,a,o)}function tl(e,t,a,o){var l={lane:o,revertLane:0,gesture:null,action:a,hasEagerState:!1,eagerState:null,next:null};if(li(e))Rd(t,l);else{var i=e.alternate;if(e.lanes===0&&(i===null||i.lanes===0)&&(i=t.lastRenderedReducer,i!==null))try{var u=t.lastRenderedState,m=i(u,a);if(l.hasEagerState=!0,l.eagerState=m,Wt(m,u))return Ul(e,t,l,0),et===null&&Gl(),!1}catch{}finally{}if(a=_s(e,t,l,o),a!==null)return Kt(a,e,o),zd(a,t,o),!0}return!1}function ar(e,t,a,o){if(o={lane:2,revertLane:zr(),gesture:null,action:o,hasEagerState:!1,eagerState:null,next:null},li(e)){if(t)throw Error(c(479))}else t=_s(e,a,o,2),t!==null&&Kt(t,e,2)}function li(e){var t=e.alternate;return e===je||t!==null&&t===je}function Rd(e,t){po=Pl=!0;var a=e.pending;a===null?t.next=t:(t.next=a.next,a.next=t),e.pending=t}function zd(e,t,a){if((a&4194048)!==0){var o=t.lanes;o&=e.pendingLanes,a|=o,t.lanes=a,Lc(e,a)}}var nl={readContext:Rt,use:ti,useCallback:dt,useContext:dt,useEffect:dt,useImperativeHandle:dt,useLayoutEffect:dt,useInsertionEffect:dt,useMemo:dt,useReducer:dt,useRef:dt,useState:dt,useDebugValue:dt,useDeferredValue:dt,useTransition:dt,useSyncExternalStore:dt,useId:dt,useHostTransitionStatus:dt,useFormState:dt,useActionState:dt,useOptimistic:dt,useMemoCache:dt,useCacheRefresh:dt};nl.useEffectEvent=dt;var Bd={readContext:Rt,use:ti,useCallback:function(e,t){return Yt().memoizedState=[e,t===void 0?null:t],e},useContext:Rt,useEffect:vd,useImperativeHandle:function(e,t,a){a=a!=null?a.concat([e]):null,ai(4194308,4,wd.bind(null,t,e),a)},useLayoutEffect:function(e,t){return ai(4194308,4,e,t)},useInsertionEffect:function(e,t){ai(4,2,e,t)},useMemo:function(e,t){var a=Yt();t=t===void 0?null:t;var o=e();if(Ha){it(!0);try{e()}finally{it(!1)}}return a.memoizedState=[o,t],o},useReducer:function(e,t,a){var o=Yt();if(a!==void 0){var l=a(t);if(Ha){it(!0);try{a(t)}finally{it(!1)}}}else l=t;return o.memoizedState=o.baseState=l,e={pending:null,lanes:0,dispatch:null,lastRenderedReducer:e,lastRenderedState:l},o.queue=e,e=e.dispatch=Xm.bind(null,je,e),[o.memoizedState,e]},useRef:function(e){var t=Yt();return e={current:e},t.memoizedState=e},useState:function(e){e=$s(e);var t=e.queue,a=Od.bind(null,je,t);return t.dispatch=a,[e.memoizedState,a]},useDebugValue:Fs,useDeferredValue:function(e,t){var a=Yt();return er(a,e,t)},useTransition:function(){var e=$s(!1);return e=Cd.bind(null,je,e.queue,!0,!1),Yt().memoizedState=e,[!1,e]},useSyncExternalStore:function(e,t,a){var o=je,l=Yt();if(Ue){if(a===void 0)throw Error(c(407));a=a()}else{if(a=t(),et===null)throw Error(c(349));(He&127)!==0||ad(o,t,a)}l.memoizedState=a;var i={value:a,getSnapshot:t};return l.queue=i,vd(ld.bind(null,o,i,e),[e]),o.flags|=2048,fo(9,{destroy:void 0},od.bind(null,o,i,a,t),null),a},useId:function(){var e=Yt(),t=et.identifierPrefix;if(Ue){var a=Tn,o=wn;a=(o&~(1<<32-Fe(o)-1)).toString(32)+a,t="_"+t+"R_"+a,a=Fl++,0<a&&(t+="H"+a.toString(32)),t+="_"}else a=Hm++,t="_"+t+"r_"+a.toString(32)+"_";return e.memoizedState=t},useHostTransitionStatus:nr,useFormState:fd,useActionState:fd,useOptimistic:function(e){var t=Yt();t.memoizedState=t.baseState=e;var a={pending:null,lanes:0,dispatch:null,lastRenderedReducer:null,lastRenderedState:null};return t.queue=a,t=ar.bind(null,je,!0,a),a.dispatch=t,[e,t]},useMemoCache:Vs,useCacheRefresh:function(){return Yt().memoizedState=Im.bind(null,je)},useEffectEvent:function(e){var t=Yt(),a={impl:e};return t.memoizedState=a,function(){if((Je&2)!==0)throw Error(c(440));return a.impl.apply(void 0,arguments)}}},or={readContext:Rt,use:ti,useCallback:Ed,useContext:Rt,useEffect:Ps,useImperativeHandle:Td,useInsertionEffect:xd,useLayoutEffect:Sd,useMemo:Nd,useReducer:ni,useRef:bd,useState:function(){return ni(Gn)},useDebugValue:Fs,useDeferredValue:function(e,t){var a=gt();return Ad(a,$e.memoizedState,e,t)},useTransition:function(){var e=ni(Gn)[0],t=gt().memoizedState;return[typeof e=="boolean"?e:el(e),t]},useSyncExternalStore:nd,useId:Dd,useHostTransitionStatus:nr,useFormState:md,useActionState:md,useOptimistic:function(e,t){var a=gt();return rd(a,$e,e,t)},useMemoCache:Vs,useCacheRefresh:kd};or.useEffectEvent=_d;var Hd={readContext:Rt,use:ti,useCallback:Ed,useContext:Rt,useEffect:Ps,useImperativeHandle:Td,useInsertionEffect:xd,useLayoutEffect:Sd,useMemo:Nd,useReducer:Ks,useRef:bd,useState:function(){return Ks(Gn)},useDebugValue:Fs,useDeferredValue:function(e,t){var a=gt();return $e===null?er(a,e,t):Ad(a,$e.memoizedState,e,t)},useTransition:function(){var e=Ks(Gn)[0],t=gt().memoizedState;return[typeof e=="boolean"?e:el(e),t]},useSyncExternalStore:nd,useId:Dd,useHostTransitionStatus:nr,useFormState:yd,useActionState:yd,useOptimistic:function(e,t){var a=gt();return $e!==null?rd(a,$e,e,t):(a.baseState=e,[e,a.queue.dispatch])},useMemoCache:Vs,useCacheRefresh:kd};Hd.useEffectEvent=_d;function lr(e,t,a,o){t=e.memoizedState,a=a(o,t),a=a==null?t:x({},t,a),e.memoizedState=a,e.lanes===0&&(e.updateQueue.baseState=a)}var ir={enqueueSetState:function(e,t,a){e=e._reactInternals;var o=an(),l=oa(o);l.payload=t,a!=null&&(l.callback=a),t=la(e,l,o),t!==null&&(Kt(t,e,o),$o(t,e,o))},enqueueReplaceState:function(e,t,a){e=e._reactInternals;var o=an(),l=oa(o);l.tag=1,l.payload=t,a!=null&&(l.callback=a),t=la(e,l,o),t!==null&&(Kt(t,e,o),$o(t,e,o))},enqueueForceUpdate:function(e,t){e=e._reactInternals;var a=an(),o=oa(a);o.tag=2,t!=null&&(o.callback=t),t=la(e,o,a),t!==null&&(Kt(t,e,a),$o(t,e,a))}};function Gd(e,t,a,o,l,i,u){return e=e.stateNode,typeof e.shouldComponentUpdate=="function"?e.shouldComponentUpdate(o,i,u):t.prototype&&t.prototype.isPureReactComponent?!qo(a,o)||!qo(l,i):!0}function Ud(e,t,a,o){e=t.state,typeof t.componentWillReceiveProps=="function"&&t.componentWillReceiveProps(a,o),typeof t.UNSAFE_componentWillReceiveProps=="function"&&t.UNSAFE_componentWillReceiveProps(a,o),t.state!==e&&ir.enqueueReplaceState(t,t.state,null)}function Ga(e,t){var a=t;if("ref"in t){a={};for(var o in t)o!=="ref"&&(a[o]=t[o])}if(e=e.defaultProps){a===t&&(a=x({},a));for(var l in e)a[l]===void 0&&(a[l]=e[l])}return a}function Ld(e){Hl(e)}function Yd(e){console.error(e)}function qd(e){Hl(e)}function ii(e,t){try{var a=e.onUncaughtError;a(t.value,{componentStack:t.stack})}catch(o){setTimeout(function(){throw o})}}function Id(e,t,a){try{var o=e.onCaughtError;o(a.value,{componentStack:a.stack,errorBoundary:t.tag===1?t.stateNode:null})}catch(l){setTimeout(function(){throw l})}}function sr(e,t,a){return a=oa(a),a.tag=3,a.payload={element:null},a.callback=function(){ii(e,t)},a}function Xd(e){return e=oa(e),e.tag=3,e}function Jd(e,t,a,o){var l=a.type.getDerivedStateFromError;if(typeof l=="function"){var i=o.value;e.payload=function(){return l(i)},e.callback=function(){Id(t,a,o)}}var u=a.stateNode;u!==null&&typeof u.componentDidCatch=="function"&&(e.callback=function(){Id(t,a,o),typeof l!="function"&&(da===null?da=new Set([this]):da.add(this));var m=o.stack;this.componentDidCatch(o.value,{componentStack:m!==null?m:""})})}function Jm(e,t,a,o,l){if(a.flags|=32768,o!==null&&typeof o=="object"&&typeof o.then=="function"){if(t=a.alternate,t!==null&&lo(t,a,l,!0),a=Ft.current,a!==null){switch(a.tag){case 31:case 13:return mn===null?bi():a.alternate===null&&pt===0&&(pt=3),a.flags&=-257,a.flags|=65536,a.lanes=l,o===Vl?a.flags|=16384:(t=a.updateQueue,t===null?a.updateQueue=new Set([o]):t.add(o),kr(e,o,l)),!1;case 22:return a.flags|=65536,o===Vl?a.flags|=16384:(t=a.updateQueue,t===null?(t={transitions:null,markerInstances:null,retryQueue:new Set([o])},a.updateQueue=t):(a=t.retryQueue,a===null?t.retryQueue=new Set([o]):a.add(o)),kr(e,o,l)),!1}throw Error(c(435,a.tag))}return kr(e,o,l),bi(),!1}if(Ue)return t=Ft.current,t!==null?((t.flags&65536)===0&&(t.flags|=256),t.flags|=65536,t.lanes=l,o!==Ns&&(e=Error(c(422),{cause:o}),Jo(dn(e,a)))):(o!==Ns&&(t=Error(c(423),{cause:o}),Jo(dn(t,a))),e=e.current.alternate,e.flags|=65536,l&=-l,e.lanes|=l,o=dn(o,a),l=sr(e.stateNode,o,l),Hs(e,l),pt!==4&&(pt=2)),!1;var i=Error(c(520),{cause:o});if(i=dn(i,a),ul===null?ul=[i]:ul.push(i),pt!==4&&(pt=2),t===null)return!0;o=dn(o,a),a=t;do{switch(a.tag){case 3:return a.flags|=65536,e=l&-l,a.lanes|=e,e=sr(a.stateNode,o,e),Hs(a,e),!1;case 1:if(t=a.type,i=a.stateNode,(a.flags&128)===0&&(typeof t.getDerivedStateFromError=="function"||i!==null&&typeof i.componentDidCatch=="function"&&(da===null||!da.has(i))))return a.flags|=65536,l&=-l,a.lanes|=l,l=Xd(l),Jd(l,e,a,o),Hs(a,l),!1}a=a.return}while(a!==null);return!1}var rr=Error(c(461)),St=!1;function zt(e,t,a,o){t.child=e===null?Ku(t,null,a,o):Ba(t,e.child,a,o)}function Zd(e,t,a,o,l){a=a.render;var i=t.ref;if("ref"in o){var u={};for(var m in o)m!=="ref"&&(u[m]=o[m])}else u=o;return ka(t),o=Is(e,t,a,u,i,l),m=Xs(),e!==null&&!St?(Js(e,t,l),Un(e,t,l)):(Ue&&m&&Ts(t),t.flags|=1,zt(e,t,o,l),t.child)}function Vd(e,t,a,o,l){if(e===null){var i=a.type;return typeof i=="function"&&!xs(i)&&i.defaultProps===void 0&&a.compare===null?(t.tag=15,t.type=i,Qd(e,t,i,o,l)):(e=Yl(a.type,null,o,t,t.mode,l),e.ref=t.ref,e.return=t,t.child=e)}if(i=e.child,!gr(e,l)){var u=i.memoizedProps;if(a=a.compare,a=a!==null?a:qo,a(u,o)&&e.ref===t.ref)return Un(e,t,l)}return t.flags|=1,e=On(i,o),e.ref=t.ref,e.return=t,t.child=e}function Qd(e,t,a,o,l){if(e!==null){var i=e.memoizedProps;if(qo(i,o)&&e.ref===t.ref)if(St=!1,t.pendingProps=o=i,gr(e,l))(e.flags&131072)!==0&&(St=!0);else return t.lanes=e.lanes,Un(e,t,l)}return cr(e,t,a,o,l)}function Kd(e,t,a,o){var l=o.children,i=e!==null?e.memoizedState:null;if(e===null&&t.stateNode===null&&(t.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),o.mode==="hidden"){if((t.flags&128)!==0){if(i=i!==null?i.baseLanes|a:a,e!==null){for(o=t.child=e.child,l=0;o!==null;)l=l|o.lanes|o.childLanes,o=o.sibling;o=l&~i}else o=0,t.child=null;return $d(e,t,i,a,o)}if((a&536870912)!==0)t.memoizedState={baseLanes:0,cachePool:null},e!==null&&Jl(t,i!==null?i.cachePool:null),i!==null?Pu(t,i):Us(),Fu(t);else return o=t.lanes=536870912,$d(e,t,i!==null?i.baseLanes|a:a,a,o)}else i!==null?(Jl(t,i.cachePool),Pu(t,i),sa(),t.memoizedState=null):(e!==null&&Jl(t,null),Us(),sa());return zt(e,t,l,a),t.child}function al(e,t){return e!==null&&e.tag===22||t.stateNode!==null||(t.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),t.sibling}function $d(e,t,a,o,l){var i=Os();return i=i===null?null:{parent:_t._currentValue,pool:i},t.memoizedState={baseLanes:a,cachePool:i},e!==null&&Jl(t,null),Us(),Fu(t),e!==null&&lo(e,t,o,!0),t.childLanes=l,null}function si(e,t){return t=ci({mode:t.mode,children:t.children},e.mode),t.ref=e.ref,e.child=t,t.return=e,t}function Wd(e,t,a){return Ba(t,e.child,null,a),e=si(t,t.pendingProps),e.flags|=2,en(t),t.memoizedState=null,e}function Zm(e,t,a){var o=t.pendingProps,l=(t.flags&128)!==0;if(t.flags&=-129,e===null){if(Ue){if(o.mode==="hidden")return e=si(t,o),t.lanes=536870912,al(null,e);if(Ys(t),(e=at)?(e=ch(e,fn),e=e!==null&&e.data==="&"?e:null,e!==null&&(t.memoizedState={dehydrated:e,treeContext:Fn!==null?{id:wn,overflow:Tn}:null,retryLane:536870912,hydrationErrors:null},a=Ru(e),a.return=t,t.child=a,Ot=t,at=null)):e=null,e===null)throw ta(t);return t.lanes=536870912,null}return si(t,o)}var i=e.memoizedState;if(i!==null){var u=i.dehydrated;if(Ys(t),l)if(t.flags&256)t.flags&=-257,t=Wd(e,t,a);else if(t.memoizedState!==null)t.child=e.child,t.flags|=128,t=null;else throw Error(c(558));else if(St||lo(e,t,a,!1),l=(a&e.childLanes)!==0,St||l){if(o=et,o!==null&&(u=Yc(o,a),u!==0&&u!==i.retryLane))throw i.retryLane=u,Ca(e,u),Kt(o,e,u),rr;bi(),t=Wd(e,t,a)}else e=i.treeContext,at=gn(u.nextSibling),Ot=t,Ue=!0,ea=null,fn=!1,e!==null&&Hu(t,e),t=si(t,o),t.flags|=4096;return t}return e=On(e.child,{mode:o.mode,children:o.children}),e.ref=t.ref,t.child=e,e.return=t,e}function ri(e,t){var a=t.ref;if(a===null)e!==null&&e.ref!==null&&(t.flags|=4194816);else{if(typeof a!="function"&&typeof a!="object")throw Error(c(284));(e===null||e.ref!==a)&&(t.flags|=4194816)}}function cr(e,t,a,o,l){return ka(t),a=Is(e,t,a,o,void 0,l),o=Xs(),e!==null&&!St?(Js(e,t,l),Un(e,t,l)):(Ue&&o&&Ts(t),t.flags|=1,zt(e,t,a,l),t.child)}function Pd(e,t,a,o,l,i){return ka(t),t.updateQueue=null,a=td(t,o,a,l),ed(e),o=Xs(),e!==null&&!St?(Js(e,t,i),Un(e,t,i)):(Ue&&o&&Ts(t),t.flags|=1,zt(e,t,a,i),t.child)}function Fd(e,t,a,o,l){if(ka(t),t.stateNode===null){var i=to,u=a.contextType;typeof u=="object"&&u!==null&&(i=Rt(u)),i=new a(o,i),t.memoizedState=i.state!==null&&i.state!==void 0?i.state:null,i.updater=ir,t.stateNode=i,i._reactInternals=t,i=t.stateNode,i.props=o,i.state=t.memoizedState,i.refs={},zs(t),u=a.contextType,i.context=typeof u=="object"&&u!==null?Rt(u):to,i.state=t.memoizedState,u=a.getDerivedStateFromProps,typeof u=="function"&&(lr(t,a,u,o),i.state=t.memoizedState),typeof a.getDerivedStateFromProps=="function"||typeof i.getSnapshotBeforeUpdate=="function"||typeof i.UNSAFE_componentWillMount!="function"&&typeof i.componentWillMount!="function"||(u=i.state,typeof i.componentWillMount=="function"&&i.componentWillMount(),typeof i.UNSAFE_componentWillMount=="function"&&i.UNSAFE_componentWillMount(),u!==i.state&&ir.enqueueReplaceState(i,i.state,null),Po(t,o,i,l),Wo(),i.state=t.memoizedState),typeof i.componentDidMount=="function"&&(t.flags|=4194308),o=!0}else if(e===null){i=t.stateNode;var m=t.memoizedProps,N=Ga(a,m);i.props=N;var U=i.context,V=a.contextType;u=to,typeof V=="object"&&V!==null&&(u=Rt(V));var W=a.getDerivedStateFromProps;V=typeof W=="function"||typeof i.getSnapshotBeforeUpdate=="function",m=t.pendingProps!==m,V||typeof i.UNSAFE_componentWillReceiveProps!="function"&&typeof i.componentWillReceiveProps!="function"||(m||U!==u)&&Ud(t,i,o,u),aa=!1;var q=t.memoizedState;i.state=q,Po(t,o,i,l),Wo(),U=t.memoizedState,m||q!==U||aa?(typeof W=="function"&&(lr(t,a,W,o),U=t.memoizedState),(N=aa||Gd(t,a,N,o,q,U,u))?(V||typeof i.UNSAFE_componentWillMount!="function"&&typeof i.componentWillMount!="function"||(typeof i.componentWillMount=="function"&&i.componentWillMount(),typeof i.UNSAFE_componentWillMount=="function"&&i.UNSAFE_componentWillMount()),typeof i.componentDidMount=="function"&&(t.flags|=4194308)):(typeof i.componentDidMount=="function"&&(t.flags|=4194308),t.memoizedProps=o,t.memoizedState=U),i.props=o,i.state=U,i.context=u,o=N):(typeof i.componentDidMount=="function"&&(t.flags|=4194308),o=!1)}else{i=t.stateNode,Bs(e,t),u=t.memoizedProps,V=Ga(a,u),i.props=V,W=t.pendingProps,q=i.context,U=a.contextType,N=to,typeof U=="object"&&U!==null&&(N=Rt(U)),m=a.getDerivedStateFromProps,(U=typeof m=="function"||typeof i.getSnapshotBeforeUpdate=="function")||typeof i.UNSAFE_componentWillReceiveProps!="function"&&typeof i.componentWillReceiveProps!="function"||(u!==W||q!==N)&&Ud(t,i,o,N),aa=!1,q=t.memoizedState,i.state=q,Po(t,o,i,l),Wo();var X=t.memoizedState;u!==W||q!==X||aa||e!==null&&e.dependencies!==null&&Il(e.dependencies)?(typeof m=="function"&&(lr(t,a,m,o),X=t.memoizedState),(V=aa||Gd(t,a,V,o,q,X,N)||e!==null&&e.dependencies!==null&&Il(e.dependencies))?(U||typeof i.UNSAFE_componentWillUpdate!="function"&&typeof i.componentWillUpdate!="function"||(typeof i.componentWillUpdate=="function"&&i.componentWillUpdate(o,X,N),typeof i.UNSAFE_componentWillUpdate=="function"&&i.UNSAFE_componentWillUpdate(o,X,N)),typeof i.componentDidUpdate=="function"&&(t.flags|=4),typeof i.getSnapshotBeforeUpdate=="function"&&(t.flags|=1024)):(typeof i.componentDidUpdate!="function"||u===e.memoizedProps&&q===e.memoizedState||(t.flags|=4),typeof i.getSnapshotBeforeUpdate!="function"||u===e.memoizedProps&&q===e.memoizedState||(t.flags|=1024),t.memoizedProps=o,t.memoizedState=X),i.props=o,i.state=X,i.context=N,o=V):(typeof i.componentDidUpdate!="function"||u===e.memoizedProps&&q===e.memoizedState||(t.flags|=4),typeof i.getSnapshotBeforeUpdate!="function"||u===e.memoizedProps&&q===e.memoizedState||(t.flags|=1024),o=!1)}return i=o,ri(e,t),o=(t.flags&128)!==0,i||o?(i=t.stateNode,a=o&&typeof a.getDerivedStateFromError!="function"?null:i.render(),t.flags|=1,e!==null&&o?(t.child=Ba(t,e.child,null,l),t.child=Ba(t,null,a,l)):zt(e,t,a,l),t.memoizedState=i.state,e=t.child):e=Un(e,t,l),e}function ep(e,t,a,o){return Ma(),t.flags|=256,zt(e,t,a,o),t.child}var ur={dehydrated:null,treeContext:null,retryLane:0,hydrationErrors:null};function dr(e){return{baseLanes:e,cachePool:Iu()}}function pr(e,t,a){return e=e!==null?e.childLanes&~a:0,t&&(e|=nn),e}function tp(e,t,a){var o=t.pendingProps,l=!1,i=(t.flags&128)!==0,u;if((u=i)||(u=e!==null&&e.memoizedState===null?!1:(mt.current&2)!==0),u&&(l=!0,t.flags&=-129),u=(t.flags&32)!==0,t.flags&=-33,e===null){if(Ue){if(l?ia(t):sa(),(e=at)?(e=ch(e,fn),e=e!==null&&e.data!=="&"?e:null,e!==null&&(t.memoizedState={dehydrated:e,treeContext:Fn!==null?{id:wn,overflow:Tn}:null,retryLane:536870912,hydrationErrors:null},a=Ru(e),a.return=t,t.child=a,Ot=t,at=null)):e=null,e===null)throw ta(t);return Qr(e)?t.lanes=32:t.lanes=536870912,null}var m=o.children;return o=o.fallback,l?(sa(),l=t.mode,m=ci({mode:"hidden",children:m},l),o=ja(o,l,a,null),m.return=t,o.return=t,m.sibling=o,t.child=m,o=t.child,o.memoizedState=dr(a),o.childLanes=pr(e,u,a),t.memoizedState=ur,al(null,o)):(ia(t),hr(t,m))}var N=e.memoizedState;if(N!==null&&(m=N.dehydrated,m!==null)){if(i)t.flags&256?(ia(t),t.flags&=-257,t=fr(e,t,a)):t.memoizedState!==null?(sa(),t.child=e.child,t.flags|=128,t=null):(sa(),m=o.fallback,l=t.mode,o=ci({mode:"visible",children:o.children},l),m=ja(m,l,a,null),m.flags|=2,o.return=t,m.return=t,o.sibling=m,t.child=o,Ba(t,e.child,null,a),o=t.child,o.memoizedState=dr(a),o.childLanes=pr(e,u,a),t.memoizedState=ur,t=al(null,o));else if(ia(t),Qr(m)){if(u=m.nextSibling&&m.nextSibling.dataset,u)var U=u.dgst;u=U,o=Error(c(419)),o.stack="",o.digest=u,Jo({value:o,source:null,stack:null}),t=fr(e,t,a)}else if(St||lo(e,t,a,!1),u=(a&e.childLanes)!==0,St||u){if(u=et,u!==null&&(o=Yc(u,a),o!==0&&o!==N.retryLane))throw N.retryLane=o,Ca(e,o),Kt(u,e,o),rr;Vr(m)||bi(),t=fr(e,t,a)}else Vr(m)?(t.flags|=192,t.child=e.child,t=null):(e=N.treeContext,at=gn(m.nextSibling),Ot=t,Ue=!0,ea=null,fn=!1,e!==null&&Hu(t,e),t=hr(t,o.children),t.flags|=4096);return t}return l?(sa(),m=o.fallback,l=t.mode,N=e.child,U=N.sibling,o=On(N,{mode:"hidden",children:o.children}),o.subtreeFlags=N.subtreeFlags&65011712,U!==null?m=On(U,m):(m=ja(m,l,a,null),m.flags|=2),m.return=t,o.return=t,o.sibling=m,t.child=o,al(null,o),o=t.child,m=e.child.memoizedState,m===null?m=dr(a):(l=m.cachePool,l!==null?(N=_t._currentValue,l=l.parent!==N?{parent:N,pool:N}:l):l=Iu(),m={baseLanes:m.baseLanes|a,cachePool:l}),o.memoizedState=m,o.childLanes=pr(e,u,a),t.memoizedState=ur,al(e.child,o)):(ia(t),a=e.child,e=a.sibling,a=On(a,{mode:"visible",children:o.children}),a.return=t,a.sibling=null,e!==null&&(u=t.deletions,u===null?(t.deletions=[e],t.flags|=16):u.push(e)),t.child=a,t.memoizedState=null,a)}function hr(e,t){return t=ci({mode:"visible",children:t},e.mode),t.return=e,e.child=t}function ci(e,t){return e=Pt(22,e,null,t),e.lanes=0,e}function fr(e,t,a){return Ba(t,e.child,null,a),e=hr(t,t.pendingProps.children),e.flags|=2,t.memoizedState=null,e}function np(e,t,a){e.lanes|=t;var o=e.alternate;o!==null&&(o.lanes|=t),js(e.return,t,a)}function mr(e,t,a,o,l,i){var u=e.memoizedState;u===null?e.memoizedState={isBackwards:t,rendering:null,renderingStartTime:0,last:o,tail:a,tailMode:l,treeForkCount:i}:(u.isBackwards=t,u.rendering=null,u.renderingStartTime=0,u.last=o,u.tail=a,u.tailMode=l,u.treeForkCount=i)}function ap(e,t,a){var o=t.pendingProps,l=o.revealOrder,i=o.tail;o=o.children;var u=mt.current,m=(u&2)!==0;if(m?(u=u&1|2,t.flags|=128):u&=1,te(mt,u),zt(e,t,o,a),o=Ue?Xo:0,!m&&e!==null&&(e.flags&128)!==0)e:for(e=t.child;e!==null;){if(e.tag===13)e.memoizedState!==null&&np(e,a,t);else if(e.tag===19)np(e,a,t);else if(e.child!==null){e.child.return=e,e=e.child;continue}if(e===t)break e;for(;e.sibling===null;){if(e.return===null||e.return===t)break e;e=e.return}e.sibling.return=e.return,e=e.sibling}switch(l){case"forwards":for(a=t.child,l=null;a!==null;)e=a.alternate,e!==null&&Wl(e)===null&&(l=a),a=a.sibling;a=l,a===null?(l=t.child,t.child=null):(l=a.sibling,a.sibling=null),mr(t,!1,l,a,i,o);break;case"backwards":case"unstable_legacy-backwards":for(a=null,l=t.child,t.child=null;l!==null;){if(e=l.alternate,e!==null&&Wl(e)===null){t.child=l;break}e=l.sibling,l.sibling=a,a=l,l=e}mr(t,!0,a,null,i,o);break;case"together":mr(t,!1,null,null,void 0,o);break;default:t.memoizedState=null}return t.child}function Un(e,t,a){if(e!==null&&(t.dependencies=e.dependencies),ua|=t.lanes,(a&t.childLanes)===0)if(e!==null){if(lo(e,t,a,!1),(a&t.childLanes)===0)return null}else return null;if(e!==null&&t.child!==e.child)throw Error(c(153));if(t.child!==null){for(e=t.child,a=On(e,e.pendingProps),t.child=a,a.return=t;e.sibling!==null;)e=e.sibling,a=a.sibling=On(e,e.pendingProps),a.return=t;a.sibling=null}return t.child}function gr(e,t){return(e.lanes&t)!==0?!0:(e=e.dependencies,!!(e!==null&&Il(e)))}function Vm(e,t,a){switch(t.tag){case 3:Oe(t,t.stateNode.containerInfo),na(t,_t,e.memoizedState.cache),Ma();break;case 27:case 5:Re(t);break;case 4:Oe(t,t.stateNode.containerInfo);break;case 10:na(t,t.type,t.memoizedProps.value);break;case 31:if(t.memoizedState!==null)return t.flags|=128,Ys(t),null;break;case 13:var o=t.memoizedState;if(o!==null)return o.dehydrated!==null?(ia(t),t.flags|=128,null):(a&t.child.childLanes)!==0?tp(e,t,a):(ia(t),e=Un(e,t,a),e!==null?e.sibling:null);ia(t);break;case 19:var l=(e.flags&128)!==0;if(o=(a&t.childLanes)!==0,o||(lo(e,t,a,!1),o=(a&t.childLanes)!==0),l){if(o)return ap(e,t,a);t.flags|=128}if(l=t.memoizedState,l!==null&&(l.rendering=null,l.tail=null,l.lastEffect=null),te(mt,mt.current),o)break;return null;case 22:return t.lanes=0,Kd(e,t,a,t.pendingProps);case 24:na(t,_t,e.memoizedState.cache)}return Un(e,t,a)}function op(e,t,a){if(e!==null)if(e.memoizedProps!==t.pendingProps)St=!0;else{if(!gr(e,a)&&(t.flags&128)===0)return St=!1,Vm(e,t,a);St=(e.flags&131072)!==0}else St=!1,Ue&&(t.flags&1048576)!==0&&Bu(t,Xo,t.index);switch(t.lanes=0,t.tag){case 16:e:{var o=t.pendingProps;if(e=Ra(t.elementType),t.type=e,typeof e=="function")xs(e)?(o=Ga(e,o),t.tag=1,t=Fd(null,t,e,o,a)):(t.tag=0,t=cr(null,t,e,o,a));else{if(e!=null){var l=e.$$typeof;if(l===H){t.tag=11,t=Zd(null,t,e,o,a);break e}else if(l===C){t.tag=14,t=Vd(null,t,e,o,a);break e}}throw t=P(e)||e,Error(c(306,t,""))}}return t;case 0:return cr(e,t,t.type,t.pendingProps,a);case 1:return o=t.type,l=Ga(o,t.pendingProps),Fd(e,t,o,l,a);case 3:e:{if(Oe(t,t.stateNode.containerInfo),e===null)throw Error(c(387));o=t.pendingProps;var i=t.memoizedState;l=i.element,Bs(e,t),Po(t,o,null,a);var u=t.memoizedState;if(o=u.cache,na(t,_t,o),o!==i.cache&&Ms(t,[_t],a,!0),Wo(),o=u.element,i.isDehydrated)if(i={element:o,isDehydrated:!1,cache:u.cache},t.updateQueue.baseState=i,t.memoizedState=i,t.flags&256){t=ep(e,t,o,a);break e}else if(o!==l){l=dn(Error(c(424)),t),Jo(l),t=ep(e,t,o,a);break e}else{switch(e=t.stateNode.containerInfo,e.nodeType){case 9:e=e.body;break;default:e=e.nodeName==="HTML"?e.ownerDocument.body:e}for(at=gn(e.firstChild),Ot=t,Ue=!0,ea=null,fn=!0,a=Ku(t,null,o,a),t.child=a;a;)a.flags=a.flags&-3|4096,a=a.sibling}else{if(Ma(),o===l){t=Un(e,t,a);break e}zt(e,t,o,a)}t=t.child}return t;case 26:return ri(e,t),e===null?(a=mh(t.type,null,t.pendingProps,null))?t.memoizedState=a:Ue||(a=t.type,e=t.pendingProps,o=Ei(me.current).createElement(a),o[kt]=t,o[It]=e,Bt(o,a,e),Nt(o),t.stateNode=o):t.memoizedState=mh(t.type,e.memoizedProps,t.pendingProps,e.memoizedState),null;case 27:return Re(t),e===null&&Ue&&(o=t.stateNode=ph(t.type,t.pendingProps,me.current),Ot=t,fn=!0,l=at,ma(t.type)?(Kr=l,at=gn(o.firstChild)):at=l),zt(e,t,t.pendingProps.children,a),ri(e,t),e===null&&(t.flags|=4194304),t.child;case 5:return e===null&&Ue&&((l=o=at)&&(o=wg(o,t.type,t.pendingProps,fn),o!==null?(t.stateNode=o,Ot=t,at=gn(o.firstChild),fn=!1,l=!0):l=!1),l||ta(t)),Re(t),l=t.type,i=t.pendingProps,u=e!==null?e.memoizedProps:null,o=i.children,Xr(l,i)?o=null:u!==null&&Xr(l,u)&&(t.flags|=32),t.memoizedState!==null&&(l=Is(e,t,Gm,null,null,a),bl._currentValue=l),ri(e,t),zt(e,t,o,a),t.child;case 6:return e===null&&Ue&&((e=a=at)&&(a=Tg(a,t.pendingProps,fn),a!==null?(t.stateNode=a,Ot=t,at=null,e=!0):e=!1),e||ta(t)),null;case 13:return tp(e,t,a);case 4:return Oe(t,t.stateNode.containerInfo),o=t.pendingProps,e===null?t.child=Ba(t,null,o,a):zt(e,t,o,a),t.child;case 11:return Zd(e,t,t.type,t.pendingProps,a);case 7:return zt(e,t,t.pendingProps,a),t.child;case 8:return zt(e,t,t.pendingProps.children,a),t.child;case 12:return zt(e,t,t.pendingProps.children,a),t.child;case 10:return o=t.pendingProps,na(t,t.type,o.value),zt(e,t,o.children,a),t.child;case 9:return l=t.type._context,o=t.pendingProps.children,ka(t),l=Rt(l),o=o(l),t.flags|=1,zt(e,t,o,a),t.child;case 14:return Vd(e,t,t.type,t.pendingProps,a);case 15:return Qd(e,t,t.type,t.pendingProps,a);case 19:return ap(e,t,a);case 31:return Zm(e,t,a);case 22:return Kd(e,t,a,t.pendingProps);case 24:return ka(t),o=Rt(_t),e===null?(l=Os(),l===null&&(l=et,i=Ds(),l.pooledCache=i,i.refCount++,i!==null&&(l.pooledCacheLanes|=a),l=i),t.memoizedState={parent:o,cache:l},zs(t),na(t,_t,l)):((e.lanes&a)!==0&&(Bs(e,t),Po(t,null,null,a),Wo()),l=e.memoizedState,i=t.memoizedState,l.parent!==o?(l={parent:o,cache:o},t.memoizedState=l,t.lanes===0&&(t.memoizedState=t.updateQueue.baseState=l),na(t,_t,o)):(o=i.cache,na(t,_t,o),o!==l.cache&&Ms(t,[_t],a,!0))),zt(e,t,t.pendingProps.children,a),t.child;case 29:throw t.pendingProps}throw Error(c(156,t.tag))}function Ln(e){e.flags|=4}function yr(e,t,a,o,l){if((t=(e.mode&32)!==0)&&(t=!1),t){if(e.flags|=16777216,(l&335544128)===l)if(e.stateNode.complete)e.flags|=8192;else if(Dp())e.flags|=8192;else throw za=Vl,Rs}else e.flags&=-16777217}function lp(e,t){if(t.type!=="stylesheet"||(t.state.loading&4)!==0)e.flags&=-16777217;else if(e.flags|=16777216,!_h(t))if(Dp())e.flags|=8192;else throw za=Vl,Rs}function ui(e,t){t!==null&&(e.flags|=4),e.flags&16384&&(t=e.tag!==22?$n():536870912,e.lanes|=t,bo|=t)}function ol(e,t){if(!Ue)switch(e.tailMode){case"hidden":t=e.tail;for(var a=null;t!==null;)t.alternate!==null&&(a=t),t=t.sibling;a===null?e.tail=null:a.sibling=null;break;case"collapsed":a=e.tail;for(var o=null;a!==null;)a.alternate!==null&&(o=a),a=a.sibling;o===null?t||e.tail===null?e.tail=null:e.tail.sibling=null:o.sibling=null}}function ot(e){var t=e.alternate!==null&&e.alternate.child===e.child,a=0,o=0;if(t)for(var l=e.child;l!==null;)a|=l.lanes|l.childLanes,o|=l.subtreeFlags&65011712,o|=l.flags&65011712,l.return=e,l=l.sibling;else for(l=e.child;l!==null;)a|=l.lanes|l.childLanes,o|=l.subtreeFlags,o|=l.flags,l.return=e,l=l.sibling;return e.subtreeFlags|=o,e.childLanes=a,t}function Qm(e,t,a){var o=t.pendingProps;switch(Es(t),t.tag){case 16:case 15:case 0:case 11:case 7:case 8:case 12:case 9:case 14:return ot(t),null;case 1:return ot(t),null;case 3:return a=t.stateNode,o=null,e!==null&&(o=e.memoizedState.cache),t.memoizedState.cache!==o&&(t.flags|=2048),Bn(_t),_e(),a.pendingContext&&(a.context=a.pendingContext,a.pendingContext=null),(e===null||e.child===null)&&(oo(t)?Ln(t):e===null||e.memoizedState.isDehydrated&&(t.flags&256)===0||(t.flags|=1024,As())),ot(t),null;case 26:var l=t.type,i=t.memoizedState;return e===null?(Ln(t),i!==null?(ot(t),lp(t,i)):(ot(t),yr(t,l,null,o,a))):i?i!==e.memoizedState?(Ln(t),ot(t),lp(t,i)):(ot(t),t.flags&=-16777217):(e=e.memoizedProps,e!==o&&Ln(t),ot(t),yr(t,l,e,o,a)),null;case 27:if(ne(t),a=me.current,l=t.type,e!==null&&t.stateNode!=null)e.memoizedProps!==o&&Ln(t);else{if(!o){if(t.stateNode===null)throw Error(c(166));return ot(t),null}e=ue.current,oo(t)?Gu(t):(e=ph(l,o,a),t.stateNode=e,Ln(t))}return ot(t),null;case 5:if(ne(t),l=t.type,e!==null&&t.stateNode!=null)e.memoizedProps!==o&&Ln(t);else{if(!o){if(t.stateNode===null)throw Error(c(166));return ot(t),null}if(i=ue.current,oo(t))Gu(t);else{var u=Ei(me.current);switch(i){case 1:i=u.createElementNS("http://www.w3.org/2000/svg",l);break;case 2:i=u.createElementNS("http://www.w3.org/1998/Math/MathML",l);break;default:switch(l){case"svg":i=u.createElementNS("http://www.w3.org/2000/svg",l);break;case"math":i=u.createElementNS("http://www.w3.org/1998/Math/MathML",l);break;case"script":i=u.createElement("div"),i.innerHTML="<script><\/script>",i=i.removeChild(i.firstChild);break;case"select":i=typeof o.is=="string"?u.createElement("select",{is:o.is}):u.createElement("select"),o.multiple?i.multiple=!0:o.size&&(i.size=o.size);break;default:i=typeof o.is=="string"?u.createElement(l,{is:o.is}):u.createElement(l)}}i[kt]=t,i[It]=o;e:for(u=t.child;u!==null;){if(u.tag===5||u.tag===6)i.appendChild(u.stateNode);else if(u.tag!==4&&u.tag!==27&&u.child!==null){u.child.return=u,u=u.child;continue}if(u===t)break e;for(;u.sibling===null;){if(u.return===null||u.return===t)break e;u=u.return}u.sibling.return=u.return,u=u.sibling}t.stateNode=i;e:switch(Bt(i,l,o),l){case"button":case"input":case"select":case"textarea":o=!!o.autoFocus;break e;case"img":o=!0;break e;default:o=!1}o&&Ln(t)}}return ot(t),yr(t,t.type,e===null?null:e.memoizedProps,t.pendingProps,a),null;case 6:if(e&&t.stateNode!=null)e.memoizedProps!==o&&Ln(t);else{if(typeof o!="string"&&t.stateNode===null)throw Error(c(166));if(e=me.current,oo(t)){if(e=t.stateNode,a=t.memoizedProps,o=null,l=Ot,l!==null)switch(l.tag){case 27:case 5:o=l.memoizedProps}e[kt]=t,e=!!(e.nodeValue===a||o!==null&&o.suppressHydrationWarning===!0||th(e.nodeValue,a)),e||ta(t,!0)}else e=Ei(e).createTextNode(o),e[kt]=t,t.stateNode=e}return ot(t),null;case 31:if(a=t.memoizedState,e===null||e.memoizedState!==null){if(o=oo(t),a!==null){if(e===null){if(!o)throw Error(c(318));if(e=t.memoizedState,e=e!==null?e.dehydrated:null,!e)throw Error(c(557));e[kt]=t}else Ma(),(t.flags&128)===0&&(t.memoizedState=null),t.flags|=4;ot(t),e=!1}else a=As(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=a),e=!0;if(!e)return t.flags&256?(en(t),t):(en(t),null);if((t.flags&128)!==0)throw Error(c(558))}return ot(t),null;case 13:if(o=t.memoizedState,e===null||e.memoizedState!==null&&e.memoizedState.dehydrated!==null){if(l=oo(t),o!==null&&o.dehydrated!==null){if(e===null){if(!l)throw Error(c(318));if(l=t.memoizedState,l=l!==null?l.dehydrated:null,!l)throw Error(c(317));l[kt]=t}else Ma(),(t.flags&128)===0&&(t.memoizedState=null),t.flags|=4;ot(t),l=!1}else l=As(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=l),l=!0;if(!l)return t.flags&256?(en(t),t):(en(t),null)}return en(t),(t.flags&128)!==0?(t.lanes=a,t):(a=o!==null,e=e!==null&&e.memoizedState!==null,a&&(o=t.child,l=null,o.alternate!==null&&o.alternate.memoizedState!==null&&o.alternate.memoizedState.cachePool!==null&&(l=o.alternate.memoizedState.cachePool.pool),i=null,o.memoizedState!==null&&o.memoizedState.cachePool!==null&&(i=o.memoizedState.cachePool.pool),i!==l&&(o.flags|=2048)),a!==e&&a&&(t.child.flags|=8192),ui(t,t.updateQueue),ot(t),null);case 4:return _e(),e===null&&Ur(t.stateNode.containerInfo),ot(t),null;case 10:return Bn(t.type),ot(t),null;case 19:if(ae(mt),o=t.memoizedState,o===null)return ot(t),null;if(l=(t.flags&128)!==0,i=o.rendering,i===null)if(l)ol(o,!1);else{if(pt!==0||e!==null&&(e.flags&128)!==0)for(e=t.child;e!==null;){if(i=Wl(e),i!==null){for(t.flags|=128,ol(o,!1),e=i.updateQueue,t.updateQueue=e,ui(t,e),t.subtreeFlags=0,e=a,a=t.child;a!==null;)Ou(a,e),a=a.sibling;return te(mt,mt.current&1|2),Ue&&Rn(t,o.treeForkCount),t.child}e=e.sibling}o.tail!==null&&fe()>mi&&(t.flags|=128,l=!0,ol(o,!1),t.lanes=4194304)}else{if(!l)if(e=Wl(i),e!==null){if(t.flags|=128,l=!0,e=e.updateQueue,t.updateQueue=e,ui(t,e),ol(o,!0),o.tail===null&&o.tailMode==="hidden"&&!i.alternate&&!Ue)return ot(t),null}else 2*fe()-o.renderingStartTime>mi&&a!==536870912&&(t.flags|=128,l=!0,ol(o,!1),t.lanes=4194304);o.isBackwards?(i.sibling=t.child,t.child=i):(e=o.last,e!==null?e.sibling=i:t.child=i,o.last=i)}return o.tail!==null?(e=o.tail,o.rendering=e,o.tail=e.sibling,o.renderingStartTime=fe(),e.sibling=null,a=mt.current,te(mt,l?a&1|2:a&1),Ue&&Rn(t,o.treeForkCount),e):(ot(t),null);case 22:case 23:return en(t),Ls(),o=t.memoizedState!==null,e!==null?e.memoizedState!==null!==o&&(t.flags|=8192):o&&(t.flags|=8192),o?(a&536870912)!==0&&(t.flags&128)===0&&(ot(t),t.subtreeFlags&6&&(t.flags|=8192)):ot(t),a=t.updateQueue,a!==null&&ui(t,a.retryQueue),a=null,e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(a=e.memoizedState.cachePool.pool),o=null,t.memoizedState!==null&&t.memoizedState.cachePool!==null&&(o=t.memoizedState.cachePool.pool),o!==a&&(t.flags|=2048),e!==null&&ae(Oa),null;case 24:return a=null,e!==null&&(a=e.memoizedState.cache),t.memoizedState.cache!==a&&(t.flags|=2048),Bn(_t),ot(t),null;case 25:return null;case 30:return null}throw Error(c(156,t.tag))}function Km(e,t){switch(Es(t),t.tag){case 1:return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 3:return Bn(_t),_e(),e=t.flags,(e&65536)!==0&&(e&128)===0?(t.flags=e&-65537|128,t):null;case 26:case 27:case 5:return ne(t),null;case 31:if(t.memoizedState!==null){if(en(t),t.alternate===null)throw Error(c(340));Ma()}return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 13:if(en(t),e=t.memoizedState,e!==null&&e.dehydrated!==null){if(t.alternate===null)throw Error(c(340));Ma()}return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 19:return ae(mt),null;case 4:return _e(),null;case 10:return Bn(t.type),null;case 22:case 23:return en(t),Ls(),e!==null&&ae(Oa),e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 24:return Bn(_t),null;case 25:return null;default:return null}}function ip(e,t){switch(Es(t),t.tag){case 3:Bn(_t),_e();break;case 26:case 27:case 5:ne(t);break;case 4:_e();break;case 31:t.memoizedState!==null&&en(t);break;case 13:en(t);break;case 19:ae(mt);break;case 10:Bn(t.type);break;case 22:case 23:en(t),Ls(),e!==null&&ae(Oa);break;case 24:Bn(_t)}}function ll(e,t){try{var a=t.updateQueue,o=a!==null?a.lastEffect:null;if(o!==null){var l=o.next;a=l;do{if((a.tag&e)===e){o=void 0;var i=a.create,u=a.inst;o=i(),u.destroy=o}a=a.next}while(a!==l)}}catch(m){Qe(t,t.return,m)}}function ra(e,t,a){try{var o=t.updateQueue,l=o!==null?o.lastEffect:null;if(l!==null){var i=l.next;o=i;do{if((o.tag&e)===e){var u=o.inst,m=u.destroy;if(m!==void 0){u.destroy=void 0,l=t;var N=a,U=m;try{U()}catch(V){Qe(l,N,V)}}}o=o.next}while(o!==i)}}catch(V){Qe(t,t.return,V)}}function sp(e){var t=e.updateQueue;if(t!==null){var a=e.stateNode;try{Wu(t,a)}catch(o){Qe(e,e.return,o)}}}function rp(e,t,a){a.props=Ga(e.type,e.memoizedProps),a.state=e.memoizedState;try{a.componentWillUnmount()}catch(o){Qe(e,t,o)}}function il(e,t){try{var a=e.ref;if(a!==null){switch(e.tag){case 26:case 27:case 5:var o=e.stateNode;break;case 30:o=e.stateNode;break;default:o=e.stateNode}typeof a=="function"?e.refCleanup=a(o):a.current=o}}catch(l){Qe(e,t,l)}}function En(e,t){var a=e.ref,o=e.refCleanup;if(a!==null)if(typeof o=="function")try{o()}catch(l){Qe(e,t,l)}finally{e.refCleanup=null,e=e.alternate,e!=null&&(e.refCleanup=null)}else if(typeof a=="function")try{a(null)}catch(l){Qe(e,t,l)}else a.current=null}function cp(e){var t=e.type,a=e.memoizedProps,o=e.stateNode;try{e:switch(t){case"button":case"input":case"select":case"textarea":a.autoFocus&&o.focus();break e;case"img":a.src?o.src=a.src:a.srcSet&&(o.srcset=a.srcSet)}}catch(l){Qe(e,e.return,l)}}function br(e,t,a){try{var o=e.stateNode;yg(o,e.type,a,t),o[It]=t}catch(l){Qe(e,e.return,l)}}function up(e){return e.tag===5||e.tag===3||e.tag===26||e.tag===27&&ma(e.type)||e.tag===4}function vr(e){e:for(;;){for(;e.sibling===null;){if(e.return===null||up(e.return))return null;e=e.return}for(e.sibling.return=e.return,e=e.sibling;e.tag!==5&&e.tag!==6&&e.tag!==18;){if(e.tag===27&&ma(e.type)||e.flags&2||e.child===null||e.tag===4)continue e;e.child.return=e,e=e.child}if(!(e.flags&2))return e.stateNode}}function _r(e,t,a){var o=e.tag;if(o===5||o===6)e=e.stateNode,t?(a.nodeType===9?a.body:a.nodeName==="HTML"?a.ownerDocument.body:a).insertBefore(e,t):(t=a.nodeType===9?a.body:a.nodeName==="HTML"?a.ownerDocument.body:a,t.appendChild(e),a=a._reactRootContainer,a!=null||t.onclick!==null||(t.onclick=Dn));else if(o!==4&&(o===27&&ma(e.type)&&(a=e.stateNode,t=null),e=e.child,e!==null))for(_r(e,t,a),e=e.sibling;e!==null;)_r(e,t,a),e=e.sibling}function di(e,t,a){var o=e.tag;if(o===5||o===6)e=e.stateNode,t?a.insertBefore(e,t):a.appendChild(e);else if(o!==4&&(o===27&&ma(e.type)&&(a=e.stateNode),e=e.child,e!==null))for(di(e,t,a),e=e.sibling;e!==null;)di(e,t,a),e=e.sibling}function dp(e){var t=e.stateNode,a=e.memoizedProps;try{for(var o=e.type,l=t.attributes;l.length;)t.removeAttributeNode(l[0]);Bt(t,o,a),t[kt]=e,t[It]=a}catch(i){Qe(e,e.return,i)}}var Yn=!1,wt=!1,xr=!1,pp=typeof WeakSet=="function"?WeakSet:Set,At=null;function $m(e,t){if(e=e.containerInfo,qr=ki,e=Tu(e),fs(e)){if("selectionStart"in e)var a={start:e.selectionStart,end:e.selectionEnd};else e:{a=(a=e.ownerDocument)&&a.defaultView||window;var o=a.getSelection&&a.getSelection();if(o&&o.rangeCount!==0){a=o.anchorNode;var l=o.anchorOffset,i=o.focusNode;o=o.focusOffset;try{a.nodeType,i.nodeType}catch{a=null;break e}var u=0,m=-1,N=-1,U=0,V=0,W=e,q=null;t:for(;;){for(var X;W!==a||l!==0&&W.nodeType!==3||(m=u+l),W!==i||o!==0&&W.nodeType!==3||(N=u+o),W.nodeType===3&&(u+=W.nodeValue.length),(X=W.firstChild)!==null;)q=W,W=X;for(;;){if(W===e)break t;if(q===a&&++U===l&&(m=u),q===i&&++V===o&&(N=u),(X=W.nextSibling)!==null)break;W=q,q=W.parentNode}W=X}a=m===-1||N===-1?null:{start:m,end:N}}else a=null}a=a||{start:0,end:0}}else a=null;for(Ir={focusedElem:e,selectionRange:a},ki=!1,At=t;At!==null;)if(t=At,e=t.child,(t.subtreeFlags&1028)!==0&&e!==null)e.return=t,At=e;else for(;At!==null;){switch(t=At,i=t.alternate,e=t.flags,t.tag){case 0:if((e&4)!==0&&(e=t.updateQueue,e=e!==null?e.events:null,e!==null))for(a=0;a<e.length;a++)l=e[a],l.ref.impl=l.nextImpl;break;case 11:case 15:break;case 1:if((e&1024)!==0&&i!==null){e=void 0,a=t,l=i.memoizedProps,i=i.memoizedState,o=a.stateNode;try{var ce=Ga(a.type,l);e=o.getSnapshotBeforeUpdate(ce,i),o.__reactInternalSnapshotBeforeUpdate=e}catch(ve){Qe(a,a.return,ve)}}break;case 3:if((e&1024)!==0){if(e=t.stateNode.containerInfo,a=e.nodeType,a===9)Zr(e);else if(a===1)switch(e.nodeName){case"HEAD":case"HTML":case"BODY":Zr(e);break;default:e.textContent=""}}break;case 5:case 26:case 27:case 6:case 4:case 17:break;default:if((e&1024)!==0)throw Error(c(163))}if(e=t.sibling,e!==null){e.return=t.return,At=e;break}At=t.return}}function hp(e,t,a){var o=a.flags;switch(a.tag){case 0:case 11:case 15:In(e,a),o&4&&ll(5,a);break;case 1:if(In(e,a),o&4)if(e=a.stateNode,t===null)try{e.componentDidMount()}catch(u){Qe(a,a.return,u)}else{var l=Ga(a.type,t.memoizedProps);t=t.memoizedState;try{e.componentDidUpdate(l,t,e.__reactInternalSnapshotBeforeUpdate)}catch(u){Qe(a,a.return,u)}}o&64&&sp(a),o&512&&il(a,a.return);break;case 3:if(In(e,a),o&64&&(e=a.updateQueue,e!==null)){if(t=null,a.child!==null)switch(a.child.tag){case 27:case 5:t=a.child.stateNode;break;case 1:t=a.child.stateNode}try{Wu(e,t)}catch(u){Qe(a,a.return,u)}}break;case 27:t===null&&o&4&&dp(a);case 26:case 5:In(e,a),t===null&&o&4&&cp(a),o&512&&il(a,a.return);break;case 12:In(e,a);break;case 31:In(e,a),o&4&&gp(e,a);break;case 13:In(e,a),o&4&&yp(e,a),o&64&&(e=a.memoizedState,e!==null&&(e=e.dehydrated,e!==null&&(a=lg.bind(null,a),Eg(e,a))));break;case 22:if(o=a.memoizedState!==null||Yn,!o){t=t!==null&&t.memoizedState!==null||wt,l=Yn;var i=wt;Yn=o,(wt=t)&&!i?Xn(e,a,(a.subtreeFlags&8772)!==0):In(e,a),Yn=l,wt=i}break;case 30:break;default:In(e,a)}}function fp(e){var t=e.alternate;t!==null&&(e.alternate=null,fp(t)),e.child=null,e.deletions=null,e.sibling=null,e.tag===5&&(t=e.stateNode,t!==null&&Wi(t)),e.stateNode=null,e.return=null,e.dependencies=null,e.memoizedProps=null,e.memoizedState=null,e.pendingProps=null,e.stateNode=null,e.updateQueue=null}var rt=null,Jt=!1;function qn(e,t,a){for(a=a.child;a!==null;)mp(e,t,a),a=a.sibling}function mp(e,t,a){if(Ye&&typeof Ye.onCommitFiberUnmount=="function")try{Ye.onCommitFiberUnmount(Tt,a)}catch{}switch(a.tag){case 26:wt||En(a,t),qn(e,t,a),a.memoizedState?a.memoizedState.count--:a.stateNode&&(a=a.stateNode,a.parentNode.removeChild(a));break;case 27:wt||En(a,t);var o=rt,l=Jt;ma(a.type)&&(rt=a.stateNode,Jt=!1),qn(e,t,a),ml(a.stateNode),rt=o,Jt=l;break;case 5:wt||En(a,t);case 6:if(o=rt,l=Jt,rt=null,qn(e,t,a),rt=o,Jt=l,rt!==null)if(Jt)try{(rt.nodeType===9?rt.body:rt.nodeName==="HTML"?rt.ownerDocument.body:rt).removeChild(a.stateNode)}catch(i){Qe(a,t,i)}else try{rt.removeChild(a.stateNode)}catch(i){Qe(a,t,i)}break;case 18:rt!==null&&(Jt?(e=rt,sh(e.nodeType===9?e.body:e.nodeName==="HTML"?e.ownerDocument.body:e,a.stateNode),No(e)):sh(rt,a.stateNode));break;case 4:o=rt,l=Jt,rt=a.stateNode.containerInfo,Jt=!0,qn(e,t,a),rt=o,Jt=l;break;case 0:case 11:case 14:case 15:ra(2,a,t),wt||ra(4,a,t),qn(e,t,a);break;case 1:wt||(En(a,t),o=a.stateNode,typeof o.componentWillUnmount=="function"&&rp(a,t,o)),qn(e,t,a);break;case 21:qn(e,t,a);break;case 22:wt=(o=wt)||a.memoizedState!==null,qn(e,t,a),wt=o;break;default:qn(e,t,a)}}function gp(e,t){if(t.memoizedState===null&&(e=t.alternate,e!==null&&(e=e.memoizedState,e!==null))){e=e.dehydrated;try{No(e)}catch(a){Qe(t,t.return,a)}}}function yp(e,t){if(t.memoizedState===null&&(e=t.alternate,e!==null&&(e=e.memoizedState,e!==null&&(e=e.dehydrated,e!==null))))try{No(e)}catch(a){Qe(t,t.return,a)}}function Wm(e){switch(e.tag){case 31:case 13:case 19:var t=e.stateNode;return t===null&&(t=e.stateNode=new pp),t;case 22:return e=e.stateNode,t=e._retryCache,t===null&&(t=e._retryCache=new pp),t;default:throw Error(c(435,e.tag))}}function pi(e,t){var a=Wm(e);t.forEach(function(o){if(!a.has(o)){a.add(o);var l=ig.bind(null,e,o);o.then(l,l)}})}function Zt(e,t){var a=t.deletions;if(a!==null)for(var o=0;o<a.length;o++){var l=a[o],i=e,u=t,m=u;e:for(;m!==null;){switch(m.tag){case 27:if(ma(m.type)){rt=m.stateNode,Jt=!1;break e}break;case 5:rt=m.stateNode,Jt=!1;break e;case 3:case 4:rt=m.stateNode.containerInfo,Jt=!0;break e}m=m.return}if(rt===null)throw Error(c(160));mp(i,u,l),rt=null,Jt=!1,i=l.alternate,i!==null&&(i.return=null),l.return=null}if(t.subtreeFlags&13886)for(t=t.child;t!==null;)bp(t,e),t=t.sibling}var _n=null;function bp(e,t){var a=e.alternate,o=e.flags;switch(e.tag){case 0:case 11:case 14:case 15:Zt(t,e),Vt(e),o&4&&(ra(3,e,e.return),ll(3,e),ra(5,e,e.return));break;case 1:Zt(t,e),Vt(e),o&512&&(wt||a===null||En(a,a.return)),o&64&&Yn&&(e=e.updateQueue,e!==null&&(o=e.callbacks,o!==null&&(a=e.shared.hiddenCallbacks,e.shared.hiddenCallbacks=a===null?o:a.concat(o))));break;case 26:var l=_n;if(Zt(t,e),Vt(e),o&512&&(wt||a===null||En(a,a.return)),o&4){var i=a!==null?a.memoizedState:null;if(o=e.memoizedState,a===null)if(o===null)if(e.stateNode===null){e:{o=e.type,a=e.memoizedProps,l=l.ownerDocument||l;t:switch(o){case"title":i=l.getElementsByTagName("title")[0],(!i||i[Oo]||i[kt]||i.namespaceURI==="http://www.w3.org/2000/svg"||i.hasAttribute("itemprop"))&&(i=l.createElement(o),l.head.insertBefore(i,l.querySelector("head > title"))),Bt(i,o,a),i[kt]=e,Nt(i),o=i;break e;case"link":var u=bh("link","href",l).get(o+(a.href||""));if(u){for(var m=0;m<u.length;m++)if(i=u[m],i.getAttribute("href")===(a.href==null||a.href===""?null:a.href)&&i.getAttribute("rel")===(a.rel==null?null:a.rel)&&i.getAttribute("title")===(a.title==null?null:a.title)&&i.getAttribute("crossorigin")===(a.crossOrigin==null?null:a.crossOrigin)){u.splice(m,1);break t}}i=l.createElement(o),Bt(i,o,a),l.head.appendChild(i);break;case"meta":if(u=bh("meta","content",l).get(o+(a.content||""))){for(m=0;m<u.length;m++)if(i=u[m],i.getAttribute("content")===(a.content==null?null:""+a.content)&&i.getAttribute("name")===(a.name==null?null:a.name)&&i.getAttribute("property")===(a.property==null?null:a.property)&&i.getAttribute("http-equiv")===(a.httpEquiv==null?null:a.httpEquiv)&&i.getAttribute("charset")===(a.charSet==null?null:a.charSet)){u.splice(m,1);break t}}i=l.createElement(o),Bt(i,o,a),l.head.appendChild(i);break;default:throw Error(c(468,o))}i[kt]=e,Nt(i),o=i}e.stateNode=o}else vh(l,e.type,e.stateNode);else e.stateNode=yh(l,o,e.memoizedProps);else i!==o?(i===null?a.stateNode!==null&&(a=a.stateNode,a.parentNode.removeChild(a)):i.count--,o===null?vh(l,e.type,e.stateNode):yh(l,o,e.memoizedProps)):o===null&&e.stateNode!==null&&br(e,e.memoizedProps,a.memoizedProps)}break;case 27:Zt(t,e),Vt(e),o&512&&(wt||a===null||En(a,a.return)),a!==null&&o&4&&br(e,e.memoizedProps,a.memoizedProps);break;case 5:if(Zt(t,e),Vt(e),o&512&&(wt||a===null||En(a,a.return)),e.flags&32){l=e.stateNode;try{Qa(l,"")}catch(ce){Qe(e,e.return,ce)}}o&4&&e.stateNode!=null&&(l=e.memoizedProps,br(e,l,a!==null?a.memoizedProps:l)),o&1024&&(xr=!0);break;case 6:if(Zt(t,e),Vt(e),o&4){if(e.stateNode===null)throw Error(c(162));o=e.memoizedProps,a=e.stateNode;try{a.nodeValue=o}catch(ce){Qe(e,e.return,ce)}}break;case 3:if(Ci=null,l=_n,_n=Ni(t.containerInfo),Zt(t,e),_n=l,Vt(e),o&4&&a!==null&&a.memoizedState.isDehydrated)try{No(t.containerInfo)}catch(ce){Qe(e,e.return,ce)}xr&&(xr=!1,vp(e));break;case 4:o=_n,_n=Ni(e.stateNode.containerInfo),Zt(t,e),Vt(e),_n=o;break;case 12:Zt(t,e),Vt(e);break;case 31:Zt(t,e),Vt(e),o&4&&(o=e.updateQueue,o!==null&&(e.updateQueue=null,pi(e,o)));break;case 13:Zt(t,e),Vt(e),e.child.flags&8192&&e.memoizedState!==null!=(a!==null&&a.memoizedState!==null)&&(fi=fe()),o&4&&(o=e.updateQueue,o!==null&&(e.updateQueue=null,pi(e,o)));break;case 22:l=e.memoizedState!==null;var N=a!==null&&a.memoizedState!==null,U=Yn,V=wt;if(Yn=U||l,wt=V||N,Zt(t,e),wt=V,Yn=U,Vt(e),o&8192)e:for(t=e.stateNode,t._visibility=l?t._visibility&-2:t._visibility|1,l&&(a===null||N||Yn||wt||Ua(e)),a=null,t=e;;){if(t.tag===5||t.tag===26){if(a===null){N=a=t;try{if(i=N.stateNode,l)u=i.style,typeof u.setProperty=="function"?u.setProperty("display","none","important"):u.display="none";else{m=N.stateNode;var W=N.memoizedProps.style,q=W!=null&&W.hasOwnProperty("display")?W.display:null;m.style.display=q==null||typeof q=="boolean"?"":(""+q).trim()}}catch(ce){Qe(N,N.return,ce)}}}else if(t.tag===6){if(a===null){N=t;try{N.stateNode.nodeValue=l?"":N.memoizedProps}catch(ce){Qe(N,N.return,ce)}}}else if(t.tag===18){if(a===null){N=t;try{var X=N.stateNode;l?rh(X,!0):rh(N.stateNode,!1)}catch(ce){Qe(N,N.return,ce)}}}else if((t.tag!==22&&t.tag!==23||t.memoizedState===null||t===e)&&t.child!==null){t.child.return=t,t=t.child;continue}if(t===e)break e;for(;t.sibling===null;){if(t.return===null||t.return===e)break e;a===t&&(a=null),t=t.return}a===t&&(a=null),t.sibling.return=t.return,t=t.sibling}o&4&&(o=e.updateQueue,o!==null&&(a=o.retryQueue,a!==null&&(o.retryQueue=null,pi(e,a))));break;case 19:Zt(t,e),Vt(e),o&4&&(o=e.updateQueue,o!==null&&(e.updateQueue=null,pi(e,o)));break;case 30:break;case 21:break;default:Zt(t,e),Vt(e)}}function Vt(e){var t=e.flags;if(t&2){try{for(var a,o=e.return;o!==null;){if(up(o)){a=o;break}o=o.return}if(a==null)throw Error(c(160));switch(a.tag){case 27:var l=a.stateNode,i=vr(e);di(e,i,l);break;case 5:var u=a.stateNode;a.flags&32&&(Qa(u,""),a.flags&=-33);var m=vr(e);di(e,m,u);break;case 3:case 4:var N=a.stateNode.containerInfo,U=vr(e);_r(e,U,N);break;default:throw Error(c(161))}}catch(V){Qe(e,e.return,V)}e.flags&=-3}t&4096&&(e.flags&=-4097)}function vp(e){if(e.subtreeFlags&1024)for(e=e.child;e!==null;){var t=e;vp(t),t.tag===5&&t.flags&1024&&t.stateNode.reset(),e=e.sibling}}function In(e,t){if(t.subtreeFlags&8772)for(t=t.child;t!==null;)hp(e,t.alternate,t),t=t.sibling}function Ua(e){for(e=e.child;e!==null;){var t=e;switch(t.tag){case 0:case 11:case 14:case 15:ra(4,t,t.return),Ua(t);break;case 1:En(t,t.return);var a=t.stateNode;typeof a.componentWillUnmount=="function"&&rp(t,t.return,a),Ua(t);break;case 27:ml(t.stateNode);case 26:case 5:En(t,t.return),Ua(t);break;case 22:t.memoizedState===null&&Ua(t);break;case 30:Ua(t);break;default:Ua(t)}e=e.sibling}}function Xn(e,t,a){for(a=a&&(t.subtreeFlags&8772)!==0,t=t.child;t!==null;){var o=t.alternate,l=e,i=t,u=i.flags;switch(i.tag){case 0:case 11:case 15:Xn(l,i,a),ll(4,i);break;case 1:if(Xn(l,i,a),o=i,l=o.stateNode,typeof l.componentDidMount=="function")try{l.componentDidMount()}catch(U){Qe(o,o.return,U)}if(o=i,l=o.updateQueue,l!==null){var m=o.stateNode;try{var N=l.shared.hiddenCallbacks;if(N!==null)for(l.shared.hiddenCallbacks=null,l=0;l<N.length;l++)$u(N[l],m)}catch(U){Qe(o,o.return,U)}}a&&u&64&&sp(i),il(i,i.return);break;case 27:dp(i);case 26:case 5:Xn(l,i,a),a&&o===null&&u&4&&cp(i),il(i,i.return);break;case 12:Xn(l,i,a);break;case 31:Xn(l,i,a),a&&u&4&&gp(l,i);break;case 13:Xn(l,i,a),a&&u&4&&yp(l,i);break;case 22:i.memoizedState===null&&Xn(l,i,a),il(i,i.return);break;case 30:break;default:Xn(l,i,a)}t=t.sibling}}function Sr(e,t){var a=null;e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(a=e.memoizedState.cachePool.pool),e=null,t.memoizedState!==null&&t.memoizedState.cachePool!==null&&(e=t.memoizedState.cachePool.pool),e!==a&&(e!=null&&e.refCount++,a!=null&&Zo(a))}function wr(e,t){e=null,t.alternate!==null&&(e=t.alternate.memoizedState.cache),t=t.memoizedState.cache,t!==e&&(t.refCount++,e!=null&&Zo(e))}function xn(e,t,a,o){if(t.subtreeFlags&10256)for(t=t.child;t!==null;)_p(e,t,a,o),t=t.sibling}function _p(e,t,a,o){var l=t.flags;switch(t.tag){case 0:case 11:case 15:xn(e,t,a,o),l&2048&&ll(9,t);break;case 1:xn(e,t,a,o);break;case 3:xn(e,t,a,o),l&2048&&(e=null,t.alternate!==null&&(e=t.alternate.memoizedState.cache),t=t.memoizedState.cache,t!==e&&(t.refCount++,e!=null&&Zo(e)));break;case 12:if(l&2048){xn(e,t,a,o),e=t.stateNode;try{var i=t.memoizedProps,u=i.id,m=i.onPostCommit;typeof m=="function"&&m(u,t.alternate===null?"mount":"update",e.passiveEffectDuration,-0)}catch(N){Qe(t,t.return,N)}}else xn(e,t,a,o);break;case 31:xn(e,t,a,o);break;case 13:xn(e,t,a,o);break;case 23:break;case 22:i=t.stateNode,u=t.alternate,t.memoizedState!==null?i._visibility&2?xn(e,t,a,o):sl(e,t):i._visibility&2?xn(e,t,a,o):(i._visibility|=2,mo(e,t,a,o,(t.subtreeFlags&10256)!==0||!1)),l&2048&&Sr(u,t);break;case 24:xn(e,t,a,o),l&2048&&wr(t.alternate,t);break;default:xn(e,t,a,o)}}function mo(e,t,a,o,l){for(l=l&&((t.subtreeFlags&10256)!==0||!1),t=t.child;t!==null;){var i=e,u=t,m=a,N=o,U=u.flags;switch(u.tag){case 0:case 11:case 15:mo(i,u,m,N,l),ll(8,u);break;case 23:break;case 22:var V=u.stateNode;u.memoizedState!==null?V._visibility&2?mo(i,u,m,N,l):sl(i,u):(V._visibility|=2,mo(i,u,m,N,l)),l&&U&2048&&Sr(u.alternate,u);break;case 24:mo(i,u,m,N,l),l&&U&2048&&wr(u.alternate,u);break;default:mo(i,u,m,N,l)}t=t.sibling}}function sl(e,t){if(t.subtreeFlags&10256)for(t=t.child;t!==null;){var a=e,o=t,l=o.flags;switch(o.tag){case 22:sl(a,o),l&2048&&Sr(o.alternate,o);break;case 24:sl(a,o),l&2048&&wr(o.alternate,o);break;default:sl(a,o)}t=t.sibling}}var rl=8192;function go(e,t,a){if(e.subtreeFlags&rl)for(e=e.child;e!==null;)xp(e,t,a),e=e.sibling}function xp(e,t,a){switch(e.tag){case 26:go(e,t,a),e.flags&rl&&e.memoizedState!==null&&Hg(a,_n,e.memoizedState,e.memoizedProps);break;case 5:go(e,t,a);break;case 3:case 4:var o=_n;_n=Ni(e.stateNode.containerInfo),go(e,t,a),_n=o;break;case 22:e.memoizedState===null&&(o=e.alternate,o!==null&&o.memoizedState!==null?(o=rl,rl=16777216,go(e,t,a),rl=o):go(e,t,a));break;default:go(e,t,a)}}function Sp(e){var t=e.alternate;if(t!==null&&(e=t.child,e!==null)){t.child=null;do t=e.sibling,e.sibling=null,e=t;while(e!==null)}}function cl(e){var t=e.deletions;if((e.flags&16)!==0){if(t!==null)for(var a=0;a<t.length;a++){var o=t[a];At=o,Tp(o,e)}Sp(e)}if(e.subtreeFlags&10256)for(e=e.child;e!==null;)wp(e),e=e.sibling}function wp(e){switch(e.tag){case 0:case 11:case 15:cl(e),e.flags&2048&&ra(9,e,e.return);break;case 3:cl(e);break;case 12:cl(e);break;case 22:var t=e.stateNode;e.memoizedState!==null&&t._visibility&2&&(e.return===null||e.return.tag!==13)?(t._visibility&=-3,hi(e)):cl(e);break;default:cl(e)}}function hi(e){var t=e.deletions;if((e.flags&16)!==0){if(t!==null)for(var a=0;a<t.length;a++){var o=t[a];At=o,Tp(o,e)}Sp(e)}for(e=e.child;e!==null;){switch(t=e,t.tag){case 0:case 11:case 15:ra(8,t,t.return),hi(t);break;case 22:a=t.stateNode,a._visibility&2&&(a._visibility&=-3,hi(t));break;default:hi(t)}e=e.sibling}}function Tp(e,t){for(;At!==null;){var a=At;switch(a.tag){case 0:case 11:case 15:ra(8,a,t);break;case 23:case 22:if(a.memoizedState!==null&&a.memoizedState.cachePool!==null){var o=a.memoizedState.cachePool.pool;o!=null&&o.refCount++}break;case 24:Zo(a.memoizedState.cache)}if(o=a.child,o!==null)o.return=a,At=o;else e:for(a=e;At!==null;){o=At;var l=o.sibling,i=o.return;if(fp(o),o===a){At=null;break e}if(l!==null){l.return=i,At=l;break e}At=i}}}var Pm={getCacheForType:function(e){var t=Rt(_t),a=t.data.get(e);return a===void 0&&(a=e(),t.data.set(e,a)),a},cacheSignal:function(){return Rt(_t).controller.signal}},Fm=typeof WeakMap=="function"?WeakMap:Map,Je=0,et=null,ze=null,He=0,Ve=0,tn=null,ca=!1,yo=!1,Tr=!1,Jn=0,pt=0,ua=0,La=0,Er=0,nn=0,bo=0,ul=null,Qt=null,Nr=!1,fi=0,Ep=0,mi=1/0,gi=null,da=null,Et=0,pa=null,vo=null,Zn=0,Ar=0,Cr=null,Np=null,dl=0,jr=null;function an(){return(Je&2)!==0&&He!==0?He&-He:R.T!==null?zr():qc()}function Ap(){if(nn===0)if((He&536870912)===0||Ue){var e=st;st<<=1,(st&3932160)===0&&(st=262144),nn=e}else nn=536870912;return e=Ft.current,e!==null&&(e.flags|=32),nn}function Kt(e,t,a){(e===et&&(Ve===2||Ve===9)||e.cancelPendingCommit!==null)&&(_o(e,0),ha(e,He,nn,!1)),ko(e,a),((Je&2)===0||e!==et)&&(e===et&&((Je&2)===0&&(La|=a),pt===4&&ha(e,He,nn,!1)),Nn(e))}function Cp(e,t,a){if((Je&6)!==0)throw Error(c(327));var o=!a&&(t&127)===0&&(t&e.expiredLanes)===0||Ze(e,t),l=o?ng(e,t):Dr(e,t,!0),i=o;do{if(l===0){yo&&!o&&ha(e,t,0,!1);break}else{if(a=e.current.alternate,i&&!eg(a)){l=Dr(e,t,!1),i=!1;continue}if(l===2){if(i=t,e.errorRecoveryDisabledLanes&i)var u=0;else u=e.pendingLanes&-536870913,u=u!==0?u:u&536870912?536870912:0;if(u!==0){t=u;e:{var m=e;l=ul;var N=m.current.memoizedState.isDehydrated;if(N&&(_o(m,u).flags|=256),u=Dr(m,u,!1),u!==2){if(Tr&&!N){m.errorRecoveryDisabledLanes|=i,La|=i,l=4;break e}i=Qt,Qt=l,i!==null&&(Qt===null?Qt=i:Qt.push.apply(Qt,i))}l=u}if(i=!1,l!==2)continue}}if(l===1){_o(e,0),ha(e,t,0,!0);break}e:{switch(o=e,i=l,i){case 0:case 1:throw Error(c(345));case 4:if((t&4194048)!==t)break;case 6:ha(o,t,nn,!ca);break e;case 2:Qt=null;break;case 3:case 5:break;default:throw Error(c(329))}if((t&62914560)===t&&(l=fi+300-fe(),10<l)){if(ha(o,t,nn,!ca),Ae(o,0,!0)!==0)break e;Zn=t,o.timeoutHandle=lh(jp.bind(null,o,a,Qt,gi,Nr,t,nn,La,bo,ca,i,"Throttled",-0,0),l);break e}jp(o,a,Qt,gi,Nr,t,nn,La,bo,ca,i,null,-0,0)}}break}while(!0);Nn(e)}function jp(e,t,a,o,l,i,u,m,N,U,V,W,q,X){if(e.timeoutHandle=-1,W=t.subtreeFlags,W&8192||(W&16785408)===16785408){W={stylesheets:null,count:0,imgCount:0,imgBytes:0,suspenseyImages:[],waitingForImages:!0,waitingForViewTransition:!1,unsuspend:Dn},xp(t,i,W);var ce=(i&62914560)===i?fi-fe():(i&4194048)===i?Ep-fe():0;if(ce=Gg(W,ce),ce!==null){Zn=i,e.cancelPendingCommit=ce(Hp.bind(null,e,t,i,a,o,l,u,m,N,V,W,null,q,X)),ha(e,i,u,!U);return}}Hp(e,t,i,a,o,l,u,m,N)}function eg(e){for(var t=e;;){var a=t.tag;if((a===0||a===11||a===15)&&t.flags&16384&&(a=t.updateQueue,a!==null&&(a=a.stores,a!==null)))for(var o=0;o<a.length;o++){var l=a[o],i=l.getSnapshot;l=l.value;try{if(!Wt(i(),l))return!1}catch{return!1}}if(a=t.child,t.subtreeFlags&16384&&a!==null)a.return=t,t=a;else{if(t===e)break;for(;t.sibling===null;){if(t.return===null||t.return===e)return!0;t=t.return}t.sibling.return=t.return,t=t.sibling}}return!0}function ha(e,t,a,o){t&=~Er,t&=~La,e.suspendedLanes|=t,e.pingedLanes&=~t,o&&(e.warmLanes|=t),o=e.expirationTimes;for(var l=t;0<l;){var i=31-Fe(l),u=1<<i;o[i]=-1,l&=~u}a!==0&&Uc(e,a,t)}function yi(){return(Je&6)===0?(pl(0),!1):!0}function Mr(){if(ze!==null){if(Ve===0)var e=ze.return;else e=ze,zn=Da=null,Zs(e),co=null,Qo=0,e=ze;for(;e!==null;)ip(e.alternate,e),e=e.return;ze=null}}function _o(e,t){var a=e.timeoutHandle;a!==-1&&(e.timeoutHandle=-1,_g(a)),a=e.cancelPendingCommit,a!==null&&(e.cancelPendingCommit=null,a()),Zn=0,Mr(),et=e,ze=a=On(e.current,null),He=t,Ve=0,tn=null,ca=!1,yo=Ze(e,t),Tr=!1,bo=nn=Er=La=ua=pt=0,Qt=ul=null,Nr=!1,(t&8)!==0&&(t|=t&32);var o=e.entangledLanes;if(o!==0)for(e=e.entanglements,o&=t;0<o;){var l=31-Fe(o),i=1<<l;t|=e[l],o&=~i}return Jn=t,Gl(),a}function Mp(e,t){je=null,R.H=nl,t===ro||t===Zl?(t=Zu(),Ve=3):t===Rs?(t=Zu(),Ve=4):Ve=t===rr?8:t!==null&&typeof t=="object"&&typeof t.then=="function"?6:1,tn=t,ze===null&&(pt=1,ii(e,dn(t,e.current)))}function Dp(){var e=Ft.current;return e===null?!0:(He&4194048)===He?mn===null:(He&62914560)===He||(He&536870912)!==0?e===mn:!1}function kp(){var e=R.H;return R.H=nl,e===null?nl:e}function Op(){var e=R.A;return R.A=Pm,e}function bi(){pt=4,ca||(He&4194048)!==He&&Ft.current!==null||(yo=!0),(ua&134217727)===0&&(La&134217727)===0||et===null||ha(et,He,nn,!1)}function Dr(e,t,a){var o=Je;Je|=2;var l=kp(),i=Op();(et!==e||He!==t)&&(gi=null,_o(e,t)),t=!1;var u=pt;e:do try{if(Ve!==0&&ze!==null){var m=ze,N=tn;switch(Ve){case 8:Mr(),u=6;break e;case 3:case 2:case 9:case 6:Ft.current===null&&(t=!0);var U=Ve;if(Ve=0,tn=null,xo(e,m,N,U),a&&yo){u=0;break e}break;default:U=Ve,Ve=0,tn=null,xo(e,m,N,U)}}tg(),u=pt;break}catch(V){Mp(e,V)}while(!0);return t&&e.shellSuspendCounter++,zn=Da=null,Je=o,R.H=l,R.A=i,ze===null&&(et=null,He=0,Gl()),u}function tg(){for(;ze!==null;)Rp(ze)}function ng(e,t){var a=Je;Je|=2;var o=kp(),l=Op();et!==e||He!==t?(gi=null,mi=fe()+500,_o(e,t)):yo=Ze(e,t);e:do try{if(Ve!==0&&ze!==null){t=ze;var i=tn;t:switch(Ve){case 1:Ve=0,tn=null,xo(e,t,i,1);break;case 2:case 9:if(Xu(i)){Ve=0,tn=null,zp(t);break}t=function(){Ve!==2&&Ve!==9||et!==e||(Ve=7),Nn(e)},i.then(t,t);break e;case 3:Ve=7;break e;case 4:Ve=5;break e;case 7:Xu(i)?(Ve=0,tn=null,zp(t)):(Ve=0,tn=null,xo(e,t,i,7));break;case 5:var u=null;switch(ze.tag){case 26:u=ze.memoizedState;case 5:case 27:var m=ze;if(u?_h(u):m.stateNode.complete){Ve=0,tn=null;var N=m.sibling;if(N!==null)ze=N;else{var U=m.return;U!==null?(ze=U,vi(U)):ze=null}break t}}Ve=0,tn=null,xo(e,t,i,5);break;case 6:Ve=0,tn=null,xo(e,t,i,6);break;case 8:Mr(),pt=6;break e;default:throw Error(c(462))}}ag();break}catch(V){Mp(e,V)}while(!0);return zn=Da=null,R.H=o,R.A=l,Je=a,ze!==null?0:(et=null,He=0,Gl(),pt)}function ag(){for(;ze!==null&&!qe();)Rp(ze)}function Rp(e){var t=op(e.alternate,e,Jn);e.memoizedProps=e.pendingProps,t===null?vi(e):ze=t}function zp(e){var t=e,a=t.alternate;switch(t.tag){case 15:case 0:t=Pd(a,t,t.pendingProps,t.type,void 0,He);break;case 11:t=Pd(a,t,t.pendingProps,t.type.render,t.ref,He);break;case 5:Zs(t);default:ip(a,t),t=ze=Ou(t,Jn),t=op(a,t,Jn)}e.memoizedProps=e.pendingProps,t===null?vi(e):ze=t}function xo(e,t,a,o){zn=Da=null,Zs(t),co=null,Qo=0;var l=t.return;try{if(Jm(e,l,t,a,He)){pt=1,ii(e,dn(a,e.current)),ze=null;return}}catch(i){if(l!==null)throw ze=l,i;pt=1,ii(e,dn(a,e.current)),ze=null;return}t.flags&32768?(Ue||o===1?e=!0:yo||(He&536870912)!==0?e=!1:(ca=e=!0,(o===2||o===9||o===3||o===6)&&(o=Ft.current,o!==null&&o.tag===13&&(o.flags|=16384))),Bp(t,e)):vi(t)}function vi(e){var t=e;do{if((t.flags&32768)!==0){Bp(t,ca);return}e=t.return;var a=Qm(t.alternate,t,Jn);if(a!==null){ze=a;return}if(t=t.sibling,t!==null){ze=t;return}ze=t=e}while(t!==null);pt===0&&(pt=5)}function Bp(e,t){do{var a=Km(e.alternate,e);if(a!==null){a.flags&=32767,ze=a;return}if(a=e.return,a!==null&&(a.flags|=32768,a.subtreeFlags=0,a.deletions=null),!t&&(e=e.sibling,e!==null)){ze=e;return}ze=e=a}while(e!==null);pt=6,ze=null}function Hp(e,t,a,o,l,i,u,m,N){e.cancelPendingCommit=null;do _i();while(Et!==0);if((Je&6)!==0)throw Error(c(327));if(t!==null){if(t===e.current)throw Error(c(177));if(i=t.lanes|t.childLanes,i|=vs,Bf(e,a,i,u,m,N),e===et&&(ze=et=null,He=0),vo=t,pa=e,Zn=a,Ar=i,Cr=l,Np=o,(t.subtreeFlags&10256)!==0||(t.flags&10256)!==0?(e.callbackNode=null,e.callbackPriority=0,sg(ht,function(){return qp(),null})):(e.callbackNode=null,e.callbackPriority=0),o=(t.flags&13878)!==0,(t.subtreeFlags&13878)!==0||o){o=R.T,R.T=null,l=z.p,z.p=2,u=Je,Je|=4;try{$m(e,t,a)}finally{Je=u,z.p=l,R.T=o}}Et=1,Gp(),Up(),Lp()}}function Gp(){if(Et===1){Et=0;var e=pa,t=vo,a=(t.flags&13878)!==0;if((t.subtreeFlags&13878)!==0||a){a=R.T,R.T=null;var o=z.p;z.p=2;var l=Je;Je|=4;try{bp(t,e);var i=Ir,u=Tu(e.containerInfo),m=i.focusedElem,N=i.selectionRange;if(u!==m&&m&&m.ownerDocument&&wu(m.ownerDocument.documentElement,m)){if(N!==null&&fs(m)){var U=N.start,V=N.end;if(V===void 0&&(V=U),"selectionStart"in m)m.selectionStart=U,m.selectionEnd=Math.min(V,m.value.length);else{var W=m.ownerDocument||document,q=W&&W.defaultView||window;if(q.getSelection){var X=q.getSelection(),ce=m.textContent.length,ve=Math.min(N.start,ce),Pe=N.end===void 0?ve:Math.min(N.end,ce);!X.extend&&ve>Pe&&(u=Pe,Pe=ve,ve=u);var B=Su(m,ve),M=Su(m,Pe);if(B&&M&&(X.rangeCount!==1||X.anchorNode!==B.node||X.anchorOffset!==B.offset||X.focusNode!==M.node||X.focusOffset!==M.offset)){var G=W.createRange();G.setStart(B.node,B.offset),X.removeAllRanges(),ve>Pe?(X.addRange(G),X.extend(M.node,M.offset)):(G.setEnd(M.node,M.offset),X.addRange(G))}}}}for(W=[],X=m;X=X.parentNode;)X.nodeType===1&&W.push({element:X,left:X.scrollLeft,top:X.scrollTop});for(typeof m.focus=="function"&&m.focus(),m=0;m<W.length;m++){var Q=W[m];Q.element.scrollLeft=Q.left,Q.element.scrollTop=Q.top}}ki=!!qr,Ir=qr=null}finally{Je=l,z.p=o,R.T=a}}e.current=t,Et=2}}function Up(){if(Et===2){Et=0;var e=pa,t=vo,a=(t.flags&8772)!==0;if((t.subtreeFlags&8772)!==0||a){a=R.T,R.T=null;var o=z.p;z.p=2;var l=Je;Je|=4;try{hp(e,t.alternate,t)}finally{Je=l,z.p=o,R.T=a}}Et=3}}function Lp(){if(Et===4||Et===3){Et=0,vt();var e=pa,t=vo,a=Zn,o=Np;(t.subtreeFlags&10256)!==0||(t.flags&10256)!==0?Et=5:(Et=0,vo=pa=null,Yp(e,e.pendingLanes));var l=e.pendingLanes;if(l===0&&(da=null),Ki(a),t=t.stateNode,Ye&&typeof Ye.onCommitFiberRoot=="function")try{Ye.onCommitFiberRoot(Tt,t,void 0,(t.current.flags&128)===128)}catch{}if(o!==null){t=R.T,l=z.p,z.p=2,R.T=null;try{for(var i=e.onRecoverableError,u=0;u<o.length;u++){var m=o[u];i(m.value,{componentStack:m.stack})}}finally{R.T=t,z.p=l}}(Zn&3)!==0&&_i(),Nn(e),l=e.pendingLanes,(a&261930)!==0&&(l&42)!==0?e===jr?dl++:(dl=0,jr=e):dl=0,pl(0)}}function Yp(e,t){(e.pooledCacheLanes&=t)===0&&(t=e.pooledCache,t!=null&&(e.pooledCache=null,Zo(t)))}function _i(){return Gp(),Up(),Lp(),qp()}function qp(){if(Et!==5)return!1;var e=pa,t=Ar;Ar=0;var a=Ki(Zn),o=R.T,l=z.p;try{z.p=32>a?32:a,R.T=null,a=Cr,Cr=null;var i=pa,u=Zn;if(Et=0,vo=pa=null,Zn=0,(Je&6)!==0)throw Error(c(331));var m=Je;if(Je|=4,wp(i.current),_p(i,i.current,u,a),Je=m,pl(0,!1),Ye&&typeof Ye.onPostCommitFiberRoot=="function")try{Ye.onPostCommitFiberRoot(Tt,i)}catch{}return!0}finally{z.p=l,R.T=o,Yp(e,t)}}function Ip(e,t,a){t=dn(a,t),t=sr(e.stateNode,t,2),e=la(e,t,2),e!==null&&(ko(e,2),Nn(e))}function Qe(e,t,a){if(e.tag===3)Ip(e,e,a);else for(;t!==null;){if(t.tag===3){Ip(t,e,a);break}else if(t.tag===1){var o=t.stateNode;if(typeof t.type.getDerivedStateFromError=="function"||typeof o.componentDidCatch=="function"&&(da===null||!da.has(o))){e=dn(a,e),a=Xd(2),o=la(t,a,2),o!==null&&(Jd(a,o,t,e),ko(o,2),Nn(o));break}}t=t.return}}function kr(e,t,a){var o=e.pingCache;if(o===null){o=e.pingCache=new Fm;var l=new Set;o.set(t,l)}else l=o.get(t),l===void 0&&(l=new Set,o.set(t,l));l.has(a)||(Tr=!0,l.add(a),e=og.bind(null,e,t,a),t.then(e,e))}function og(e,t,a){var o=e.pingCache;o!==null&&o.delete(t),e.pingedLanes|=e.suspendedLanes&a,e.warmLanes&=~a,et===e&&(He&a)===a&&(pt===4||pt===3&&(He&62914560)===He&&300>fe()-fi?(Je&2)===0&&_o(e,0):Er|=a,bo===He&&(bo=0)),Nn(e)}function Xp(e,t){t===0&&(t=$n()),e=Ca(e,t),e!==null&&(ko(e,t),Nn(e))}function lg(e){var t=e.memoizedState,a=0;t!==null&&(a=t.retryLane),Xp(e,a)}function ig(e,t){var a=0;switch(e.tag){case 31:case 13:var o=e.stateNode,l=e.memoizedState;l!==null&&(a=l.retryLane);break;case 19:o=e.stateNode;break;case 22:o=e.stateNode._retryCache;break;default:throw Error(c(314))}o!==null&&o.delete(t),Xp(e,a)}function sg(e,t){return Le(e,t)}var xi=null,So=null,Or=!1,Si=!1,Rr=!1,fa=0;function Nn(e){e!==So&&e.next===null&&(So===null?xi=So=e:So=So.next=e),Si=!0,Or||(Or=!0,cg())}function pl(e,t){if(!Rr&&Si){Rr=!0;do for(var a=!1,o=xi;o!==null;){if(e!==0){var l=o.pendingLanes;if(l===0)var i=0;else{var u=o.suspendedLanes,m=o.pingedLanes;i=(1<<31-Fe(42|e)+1)-1,i&=l&~(u&~m),i=i&201326741?i&201326741|1:i?i|2:0}i!==0&&(a=!0,Qp(o,i))}else i=He,i=Ae(o,o===et?i:0,o.cancelPendingCommit!==null||o.timeoutHandle!==-1),(i&3)===0||Ze(o,i)||(a=!0,Qp(o,i));o=o.next}while(a);Rr=!1}}function rg(){Jp()}function Jp(){Si=Or=!1;var e=0;fa!==0&&vg()&&(e=fa);for(var t=fe(),a=null,o=xi;o!==null;){var l=o.next,i=Zp(o,t);i===0?(o.next=null,a===null?xi=l:a.next=l,l===null&&(So=a)):(a=o,(e!==0||(i&3)!==0)&&(Si=!0)),o=l}Et!==0&&Et!==5||pl(e),fa!==0&&(fa=0)}function Zp(e,t){for(var a=e.suspendedLanes,o=e.pingedLanes,l=e.expirationTimes,i=e.pendingLanes&-62914561;0<i;){var u=31-Fe(i),m=1<<u,N=l[u];N===-1?((m&a)===0||(m&o)!==0)&&(l[u]=ft(m,t)):N<=t&&(e.expiredLanes|=m),i&=~m}if(t=et,a=He,a=Ae(e,e===t?a:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),o=e.callbackNode,a===0||e===t&&(Ve===2||Ve===9)||e.cancelPendingCommit!==null)return o!==null&&o!==null&&nt(o),e.callbackNode=null,e.callbackPriority=0;if((a&3)===0||Ze(e,a)){if(t=a&-a,t===e.callbackPriority)return t;switch(o!==null&&nt(o),Ki(a)){case 2:case 8:a=ut;break;case 32:a=ht;break;case 268435456:a=Gt;break;default:a=ht}return o=Vp.bind(null,e),a=Le(a,o),e.callbackPriority=t,e.callbackNode=a,t}return o!==null&&o!==null&&nt(o),e.callbackPriority=2,e.callbackNode=null,2}function Vp(e,t){if(Et!==0&&Et!==5)return e.callbackNode=null,e.callbackPriority=0,null;var a=e.callbackNode;if(_i()&&e.callbackNode!==a)return null;var o=He;return o=Ae(e,e===et?o:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),o===0?null:(Cp(e,o,t),Zp(e,fe()),e.callbackNode!=null&&e.callbackNode===a?Vp.bind(null,e):null)}function Qp(e,t){if(_i())return null;Cp(e,t,!0)}function cg(){xg(function(){(Je&6)!==0?Le(Ht,rg):Jp()})}function zr(){if(fa===0){var e=io;e===0&&(e=Me,Me<<=1,(Me&261888)===0&&(Me=256)),fa=e}return fa}function Kp(e){return e==null||typeof e=="symbol"||typeof e=="boolean"?null:typeof e=="function"?e:Ml(""+e)}function $p(e,t){var a=t.ownerDocument.createElement("input");return a.name=t.name,a.value=t.value,e.id&&a.setAttribute("form",e.id),t.parentNode.insertBefore(a,t),e=new FormData(e),a.parentNode.removeChild(a),e}function ug(e,t,a,o,l){if(t==="submit"&&a&&a.stateNode===l){var i=Kp((l[It]||null).action),u=o.submitter;u&&(t=(t=u[It]||null)?Kp(t.formAction):u.getAttribute("formAction"),t!==null&&(i=t,u=null));var m=new Rl("action","action",null,o,l);e.push({event:m,listeners:[{instance:null,listener:function(){if(o.defaultPrevented){if(fa!==0){var N=u?$p(l,u):new FormData(l);tr(a,{pending:!0,data:N,method:l.method,action:i},null,N)}}else typeof i=="function"&&(m.preventDefault(),N=u?$p(l,u):new FormData(l),tr(a,{pending:!0,data:N,method:l.method,action:i},i,N))},currentTarget:l}]})}}for(var Br=0;Br<bs.length;Br++){var Hr=bs[Br],dg=Hr.toLowerCase(),pg=Hr[0].toUpperCase()+Hr.slice(1);vn(dg,"on"+pg)}vn(Au,"onAnimationEnd"),vn(Cu,"onAnimationIteration"),vn(ju,"onAnimationStart"),vn("dblclick","onDoubleClick"),vn("focusin","onFocus"),vn("focusout","onBlur"),vn(Cm,"onTransitionRun"),vn(jm,"onTransitionStart"),vn(Mm,"onTransitionCancel"),vn(Mu,"onTransitionEnd"),Za("onMouseEnter",["mouseout","mouseover"]),Za("onMouseLeave",["mouseout","mouseover"]),Za("onPointerEnter",["pointerout","pointerover"]),Za("onPointerLeave",["pointerout","pointerover"]),Ta("onChange","change click focusin focusout input keydown keyup selectionchange".split(" ")),Ta("onSelect","focusout contextmenu dragend focusin keydown keyup mousedown mouseup selectionchange".split(" ")),Ta("onBeforeInput",["compositionend","keypress","textInput","paste"]),Ta("onCompositionEnd","compositionend focusout keydown keypress keyup mousedown".split(" ")),Ta("onCompositionStart","compositionstart focusout keydown keypress keyup mousedown".split(" ")),Ta("onCompositionUpdate","compositionupdate focusout keydown keypress keyup mousedown".split(" "));var hl="abort canplay canplaythrough durationchange emptied encrypted ended error loadeddata loadedmetadata loadstart pause play playing progress ratechange resize seeked seeking stalled suspend timeupdate volumechange waiting".split(" "),hg=new Set("beforetoggle cancel close invalid load scroll scrollend toggle".split(" ").concat(hl));function Wp(e,t){t=(t&4)!==0;for(var a=0;a<e.length;a++){var o=e[a],l=o.event;o=o.listeners;e:{var i=void 0;if(t)for(var u=o.length-1;0<=u;u--){var m=o[u],N=m.instance,U=m.currentTarget;if(m=m.listener,N!==i&&l.isPropagationStopped())break e;i=m,l.currentTarget=U;try{i(l)}catch(V){Hl(V)}l.currentTarget=null,i=N}else for(u=0;u<o.length;u++){if(m=o[u],N=m.instance,U=m.currentTarget,m=m.listener,N!==i&&l.isPropagationStopped())break e;i=m,l.currentTarget=U;try{i(l)}catch(V){Hl(V)}l.currentTarget=null,i=N}}}}function Be(e,t){var a=t[$i];a===void 0&&(a=t[$i]=new Set);var o=e+"__bubble";a.has(o)||(Pp(t,e,2,!1),a.add(o))}function Gr(e,t,a){var o=0;t&&(o|=4),Pp(a,e,o,t)}var wi="_reactListening"+Math.random().toString(36).slice(2);function Ur(e){if(!e[wi]){e[wi]=!0,Jc.forEach(function(a){a!=="selectionchange"&&(hg.has(a)||Gr(a,!1,e),Gr(a,!0,e))});var t=e.nodeType===9?e:e.ownerDocument;t===null||t[wi]||(t[wi]=!0,Gr("selectionchange",!1,t))}}function Pp(e,t,a,o){switch(Ah(t)){case 2:var l=Yg;break;case 8:l=qg;break;default:l=ec}a=l.bind(null,t,a,e),l=void 0,!ls||t!=="touchstart"&&t!=="touchmove"&&t!=="wheel"||(l=!0),o?l!==void 0?e.addEventListener(t,a,{capture:!0,passive:l}):e.addEventListener(t,a,!0):l!==void 0?e.addEventListener(t,a,{passive:l}):e.addEventListener(t,a,!1)}function Lr(e,t,a,o,l){var i=o;if((t&1)===0&&(t&2)===0&&o!==null)e:for(;;){if(o===null)return;var u=o.tag;if(u===3||u===4){var m=o.stateNode.containerInfo;if(m===l)break;if(u===4)for(u=o.return;u!==null;){var N=u.tag;if((N===3||N===4)&&u.stateNode.containerInfo===l)return;u=u.return}for(;m!==null;){if(u=Ia(m),u===null)return;if(N=u.tag,N===5||N===6||N===26||N===27){o=i=u;continue e}m=m.parentNode}}o=o.return}au(function(){var U=i,V=as(a),W=[];e:{var q=Du.get(e);if(q!==void 0){var X=Rl,ce=e;switch(e){case"keypress":if(kl(a)===0)break e;case"keydown":case"keyup":X=im;break;case"focusin":ce="focus",X=cs;break;case"focusout":ce="blur",X=cs;break;case"beforeblur":case"afterblur":X=cs;break;case"click":if(a.button===2)break e;case"auxclick":case"dblclick":case"mousedown":case"mousemove":case"mouseup":case"mouseout":case"mouseover":case"contextmenu":X=iu;break;case"drag":case"dragend":case"dragenter":case"dragexit":case"dragleave":case"dragover":case"dragstart":case"drop":X=Qf;break;case"touchcancel":case"touchend":case"touchmove":case"touchstart":X=cm;break;case Au:case Cu:case ju:X=Wf;break;case Mu:X=dm;break;case"scroll":case"scrollend":X=Zf;break;case"wheel":X=hm;break;case"copy":case"cut":case"paste":X=Ff;break;case"gotpointercapture":case"lostpointercapture":case"pointercancel":case"pointerdown":case"pointermove":case"pointerout":case"pointerover":case"pointerup":X=ru;break;case"toggle":case"beforetoggle":X=mm}var ve=(t&4)!==0,Pe=!ve&&(e==="scroll"||e==="scrollend"),B=ve?q!==null?q+"Capture":null:q;ve=[];for(var M=U,G;M!==null;){var Q=M;if(G=Q.stateNode,Q=Q.tag,Q!==5&&Q!==26&&Q!==27||G===null||B===null||(Q=zo(M,B),Q!=null&&ve.push(fl(M,Q,G))),Pe)break;M=M.return}0<ve.length&&(q=new X(q,ce,null,a,V),W.push({event:q,listeners:ve}))}}if((t&7)===0){e:{if(q=e==="mouseover"||e==="pointerover",X=e==="mouseout"||e==="pointerout",q&&a!==ns&&(ce=a.relatedTarget||a.fromElement)&&(Ia(ce)||ce[qa]))break e;if((X||q)&&(q=V.window===V?V:(q=V.ownerDocument)?q.defaultView||q.parentWindow:window,X?(ce=a.relatedTarget||a.toElement,X=U,ce=ce?Ia(ce):null,ce!==null&&(Pe=h(ce),ve=ce.tag,ce!==Pe||ve!==5&&ve!==27&&ve!==6)&&(ce=null)):(X=null,ce=U),X!==ce)){if(ve=iu,Q="onMouseLeave",B="onMouseEnter",M="mouse",(e==="pointerout"||e==="pointerover")&&(ve=ru,Q="onPointerLeave",B="onPointerEnter",M="pointer"),Pe=X==null?q:Ro(X),G=ce==null?q:Ro(ce),q=new ve(Q,M+"leave",X,a,V),q.target=Pe,q.relatedTarget=G,Q=null,Ia(V)===U&&(ve=new ve(B,M+"enter",ce,a,V),ve.target=G,ve.relatedTarget=Pe,Q=ve),Pe=Q,X&&ce)t:{for(ve=fg,B=X,M=ce,G=0,Q=B;Q;Q=ve(Q))G++;Q=0;for(var be=M;be;be=ve(be))Q++;for(;0<G-Q;)B=ve(B),G--;for(;0<Q-G;)M=ve(M),Q--;for(;G--;){if(B===M||M!==null&&B===M.alternate){ve=B;break t}B=ve(B),M=ve(M)}ve=null}else ve=null;X!==null&&Fp(W,q,X,ve,!1),ce!==null&&Pe!==null&&Fp(W,Pe,ce,ve,!0)}}e:{if(q=U?Ro(U):window,X=q.nodeName&&q.nodeName.toLowerCase(),X==="select"||X==="input"&&q.type==="file")var Ie=gu;else if(fu(q))if(yu)Ie=Em;else{Ie=wm;var de=Sm}else X=q.nodeName,!X||X.toLowerCase()!=="input"||q.type!=="checkbox"&&q.type!=="radio"?U&&ts(U.elementType)&&(Ie=gu):Ie=Tm;if(Ie&&(Ie=Ie(e,U))){mu(W,Ie,a,V);break e}de&&de(e,q,U),e==="focusout"&&U&&q.type==="number"&&U.memoizedProps.value!=null&&es(q,"number",q.value)}switch(de=U?Ro(U):window,e){case"focusin":(fu(de)||de.contentEditable==="true")&&(Pa=de,ms=U,Io=null);break;case"focusout":Io=ms=Pa=null;break;case"mousedown":gs=!0;break;case"contextmenu":case"mouseup":case"dragend":gs=!1,Eu(W,a,V);break;case"selectionchange":if(Am)break;case"keydown":case"keyup":Eu(W,a,V)}var De;if(ds)e:{switch(e){case"compositionstart":var Ge="onCompositionStart";break e;case"compositionend":Ge="onCompositionEnd";break e;case"compositionupdate":Ge="onCompositionUpdate";break e}Ge=void 0}else Wa?pu(e,a)&&(Ge="onCompositionEnd"):e==="keydown"&&a.keyCode===229&&(Ge="onCompositionStart");Ge&&(cu&&a.locale!=="ko"&&(Wa||Ge!=="onCompositionStart"?Ge==="onCompositionEnd"&&Wa&&(De=ou()):(Pn=V,is="value"in Pn?Pn.value:Pn.textContent,Wa=!0)),de=Ti(U,Ge),0<de.length&&(Ge=new su(Ge,e,null,a,V),W.push({event:Ge,listeners:de}),De?Ge.data=De:(De=hu(a),De!==null&&(Ge.data=De)))),(De=ym?bm(e,a):vm(e,a))&&(Ge=Ti(U,"onBeforeInput"),0<Ge.length&&(de=new su("onBeforeInput","beforeinput",null,a,V),W.push({event:de,listeners:Ge}),de.data=De)),ug(W,e,U,a,V)}Wp(W,t)})}function fl(e,t,a){return{instance:e,listener:t,currentTarget:a}}function Ti(e,t){for(var a=t+"Capture",o=[];e!==null;){var l=e,i=l.stateNode;if(l=l.tag,l!==5&&l!==26&&l!==27||i===null||(l=zo(e,a),l!=null&&o.unshift(fl(e,l,i)),l=zo(e,t),l!=null&&o.push(fl(e,l,i))),e.tag===3)return o;e=e.return}return[]}function fg(e){if(e===null)return null;do e=e.return;while(e&&e.tag!==5&&e.tag!==27);return e||null}function Fp(e,t,a,o,l){for(var i=t._reactName,u=[];a!==null&&a!==o;){var m=a,N=m.alternate,U=m.stateNode;if(m=m.tag,N!==null&&N===o)break;m!==5&&m!==26&&m!==27||U===null||(N=U,l?(U=zo(a,i),U!=null&&u.unshift(fl(a,U,N))):l||(U=zo(a,i),U!=null&&u.push(fl(a,U,N)))),a=a.return}u.length!==0&&e.push({event:t,listeners:u})}var mg=/\r\n?/g,gg=/\u0000|\uFFFD/g;function eh(e){return(typeof e=="string"?e:""+e).replace(mg,`
`).replace(gg,"")}function th(e,t){return t=eh(t),eh(e)===t}function We(e,t,a,o,l,i){switch(a){case"children":typeof o=="string"?t==="body"||t==="textarea"&&o===""||Qa(e,o):(typeof o=="number"||typeof o=="bigint")&&t!=="body"&&Qa(e,""+o);break;case"className":Cl(e,"class",o);break;case"tabIndex":Cl(e,"tabindex",o);break;case"dir":case"role":case"viewBox":case"width":case"height":Cl(e,a,o);break;case"style":tu(e,o,i);break;case"data":if(t!=="object"){Cl(e,"data",o);break}case"src":case"href":if(o===""&&(t!=="a"||a!=="href")){e.removeAttribute(a);break}if(o==null||typeof o=="function"||typeof o=="symbol"||typeof o=="boolean"){e.removeAttribute(a);break}o=Ml(""+o),e.setAttribute(a,o);break;case"action":case"formAction":if(typeof o=="function"){e.setAttribute(a,"javascript:throw new Error('A React form was unexpectedly submitted. If you called form.submit() manually, consider using form.requestSubmit() instead. If you\\'re trying to use event.stopPropagation() in a submit event handler, consider also calling event.preventDefault().')");break}else typeof i=="function"&&(a==="formAction"?(t!=="input"&&We(e,t,"name",l.name,l,null),We(e,t,"formEncType",l.formEncType,l,null),We(e,t,"formMethod",l.formMethod,l,null),We(e,t,"formTarget",l.formTarget,l,null)):(We(e,t,"encType",l.encType,l,null),We(e,t,"method",l.method,l,null),We(e,t,"target",l.target,l,null)));if(o==null||typeof o=="symbol"||typeof o=="boolean"){e.removeAttribute(a);break}o=Ml(""+o),e.setAttribute(a,o);break;case"onClick":o!=null&&(e.onclick=Dn);break;case"onScroll":o!=null&&Be("scroll",e);break;case"onScrollEnd":o!=null&&Be("scrollend",e);break;case"dangerouslySetInnerHTML":if(o!=null){if(typeof o!="object"||!("__html"in o))throw Error(c(61));if(a=o.__html,a!=null){if(l.children!=null)throw Error(c(60));e.innerHTML=a}}break;case"multiple":e.multiple=o&&typeof o!="function"&&typeof o!="symbol";break;case"muted":e.muted=o&&typeof o!="function"&&typeof o!="symbol";break;case"suppressContentEditableWarning":case"suppressHydrationWarning":case"defaultValue":case"defaultChecked":case"innerHTML":case"ref":break;case"autoFocus":break;case"xlinkHref":if(o==null||typeof o=="function"||typeof o=="boolean"||typeof o=="symbol"){e.removeAttribute("xlink:href");break}a=Ml(""+o),e.setAttributeNS("http://www.w3.org/1999/xlink","xlink:href",a);break;case"contentEditable":case"spellCheck":case"draggable":case"value":case"autoReverse":case"externalResourcesRequired":case"focusable":case"preserveAlpha":o!=null&&typeof o!="function"&&typeof o!="symbol"?e.setAttribute(a,""+o):e.removeAttribute(a);break;case"inert":case"allowFullScreen":case"async":case"autoPlay":case"controls":case"default":case"defer":case"disabled":case"disablePictureInPicture":case"disableRemotePlayback":case"formNoValidate":case"hidden":case"loop":case"noModule":case"noValidate":case"open":case"playsInline":case"readOnly":case"required":case"reversed":case"scoped":case"seamless":case"itemScope":o&&typeof o!="function"&&typeof o!="symbol"?e.setAttribute(a,""):e.removeAttribute(a);break;case"capture":case"download":o===!0?e.setAttribute(a,""):o!==!1&&o!=null&&typeof o!="function"&&typeof o!="symbol"?e.setAttribute(a,o):e.removeAttribute(a);break;case"cols":case"rows":case"size":case"span":o!=null&&typeof o!="function"&&typeof o!="symbol"&&!isNaN(o)&&1<=o?e.setAttribute(a,o):e.removeAttribute(a);break;case"rowSpan":case"start":o==null||typeof o=="function"||typeof o=="symbol"||isNaN(o)?e.removeAttribute(a):e.setAttribute(a,o);break;case"popover":Be("beforetoggle",e),Be("toggle",e),Al(e,"popover",o);break;case"xlinkActuate":Mn(e,"http://www.w3.org/1999/xlink","xlink:actuate",o);break;case"xlinkArcrole":Mn(e,"http://www.w3.org/1999/xlink","xlink:arcrole",o);break;case"xlinkRole":Mn(e,"http://www.w3.org/1999/xlink","xlink:role",o);break;case"xlinkShow":Mn(e,"http://www.w3.org/1999/xlink","xlink:show",o);break;case"xlinkTitle":Mn(e,"http://www.w3.org/1999/xlink","xlink:title",o);break;case"xlinkType":Mn(e,"http://www.w3.org/1999/xlink","xlink:type",o);break;case"xmlBase":Mn(e,"http://www.w3.org/XML/1998/namespace","xml:base",o);break;case"xmlLang":Mn(e,"http://www.w3.org/XML/1998/namespace","xml:lang",o);break;case"xmlSpace":Mn(e,"http://www.w3.org/XML/1998/namespace","xml:space",o);break;case"is":Al(e,"is",o);break;case"innerText":case"textContent":break;default:(!(2<a.length)||a[0]!=="o"&&a[0]!=="O"||a[1]!=="n"&&a[1]!=="N")&&(a=Xf.get(a)||a,Al(e,a,o))}}function Yr(e,t,a,o,l,i){switch(a){case"style":tu(e,o,i);break;case"dangerouslySetInnerHTML":if(o!=null){if(typeof o!="object"||!("__html"in o))throw Error(c(61));if(a=o.__html,a!=null){if(l.children!=null)throw Error(c(60));e.innerHTML=a}}break;case"children":typeof o=="string"?Qa(e,o):(typeof o=="number"||typeof o=="bigint")&&Qa(e,""+o);break;case"onScroll":o!=null&&Be("scroll",e);break;case"onScrollEnd":o!=null&&Be("scrollend",e);break;case"onClick":o!=null&&(e.onclick=Dn);break;case"suppressContentEditableWarning":case"suppressHydrationWarning":case"innerHTML":case"ref":break;case"innerText":case"textContent":break;default:if(!Zc.hasOwnProperty(a))e:{if(a[0]==="o"&&a[1]==="n"&&(l=a.endsWith("Capture"),t=a.slice(2,l?a.length-7:void 0),i=e[It]||null,i=i!=null?i[a]:null,typeof i=="function"&&e.removeEventListener(t,i,l),typeof o=="function")){typeof i!="function"&&i!==null&&(a in e?e[a]=null:e.hasAttribute(a)&&e.removeAttribute(a)),e.addEventListener(t,o,l);break e}a in e?e[a]=o:o===!0?e.setAttribute(a,""):Al(e,a,o)}}}function Bt(e,t,a){switch(t){case"div":case"span":case"svg":case"path":case"a":case"g":case"p":case"li":break;case"img":Be("error",e),Be("load",e);var o=!1,l=!1,i;for(i in a)if(a.hasOwnProperty(i)){var u=a[i];if(u!=null)switch(i){case"src":o=!0;break;case"srcSet":l=!0;break;case"children":case"dangerouslySetInnerHTML":throw Error(c(137,t));default:We(e,t,i,u,a,null)}}l&&We(e,t,"srcSet",a.srcSet,a,null),o&&We(e,t,"src",a.src,a,null);return;case"input":Be("invalid",e);var m=i=u=l=null,N=null,U=null;for(o in a)if(a.hasOwnProperty(o)){var V=a[o];if(V!=null)switch(o){case"name":l=V;break;case"type":u=V;break;case"checked":N=V;break;case"defaultChecked":U=V;break;case"value":i=V;break;case"defaultValue":m=V;break;case"children":case"dangerouslySetInnerHTML":if(V!=null)throw Error(c(137,t));break;default:We(e,t,o,V,a,null)}}Wc(e,i,m,N,U,u,l,!1);return;case"select":Be("invalid",e),o=u=i=null;for(l in a)if(a.hasOwnProperty(l)&&(m=a[l],m!=null))switch(l){case"value":i=m;break;case"defaultValue":u=m;break;case"multiple":o=m;default:We(e,t,l,m,a,null)}t=i,a=u,e.multiple=!!o,t!=null?Va(e,!!o,t,!1):a!=null&&Va(e,!!o,a,!0);return;case"textarea":Be("invalid",e),i=l=o=null;for(u in a)if(a.hasOwnProperty(u)&&(m=a[u],m!=null))switch(u){case"value":o=m;break;case"defaultValue":l=m;break;case"children":i=m;break;case"dangerouslySetInnerHTML":if(m!=null)throw Error(c(91));break;default:We(e,t,u,m,a,null)}Fc(e,o,l,i);return;case"option":for(N in a)if(a.hasOwnProperty(N)&&(o=a[N],o!=null))switch(N){case"selected":e.selected=o&&typeof o!="function"&&typeof o!="symbol";break;default:We(e,t,N,o,a,null)}return;case"dialog":Be("beforetoggle",e),Be("toggle",e),Be("cancel",e),Be("close",e);break;case"iframe":case"object":Be("load",e);break;case"video":case"audio":for(o=0;o<hl.length;o++)Be(hl[o],e);break;case"image":Be("error",e),Be("load",e);break;case"details":Be("toggle",e);break;case"embed":case"source":case"link":Be("error",e),Be("load",e);case"area":case"base":case"br":case"col":case"hr":case"keygen":case"meta":case"param":case"track":case"wbr":case"menuitem":for(U in a)if(a.hasOwnProperty(U)&&(o=a[U],o!=null))switch(U){case"children":case"dangerouslySetInnerHTML":throw Error(c(137,t));default:We(e,t,U,o,a,null)}return;default:if(ts(t)){for(V in a)a.hasOwnProperty(V)&&(o=a[V],o!==void 0&&Yr(e,t,V,o,a,void 0));return}}for(m in a)a.hasOwnProperty(m)&&(o=a[m],o!=null&&We(e,t,m,o,a,null))}function yg(e,t,a,o){switch(t){case"div":case"span":case"svg":case"path":case"a":case"g":case"p":case"li":break;case"input":var l=null,i=null,u=null,m=null,N=null,U=null,V=null;for(X in a){var W=a[X];if(a.hasOwnProperty(X)&&W!=null)switch(X){case"checked":break;case"value":break;case"defaultValue":N=W;default:o.hasOwnProperty(X)||We(e,t,X,null,o,W)}}for(var q in o){var X=o[q];if(W=a[q],o.hasOwnProperty(q)&&(X!=null||W!=null))switch(q){case"type":i=X;break;case"name":l=X;break;case"checked":U=X;break;case"defaultChecked":V=X;break;case"value":u=X;break;case"defaultValue":m=X;break;case"children":case"dangerouslySetInnerHTML":if(X!=null)throw Error(c(137,t));break;default:X!==W&&We(e,t,q,X,o,W)}}Fi(e,u,m,N,U,V,i,l);return;case"select":X=u=m=q=null;for(i in a)if(N=a[i],a.hasOwnProperty(i)&&N!=null)switch(i){case"value":break;case"multiple":X=N;default:o.hasOwnProperty(i)||We(e,t,i,null,o,N)}for(l in o)if(i=o[l],N=a[l],o.hasOwnProperty(l)&&(i!=null||N!=null))switch(l){case"value":q=i;break;case"defaultValue":m=i;break;case"multiple":u=i;default:i!==N&&We(e,t,l,i,o,N)}t=m,a=u,o=X,q!=null?Va(e,!!a,q,!1):!!o!=!!a&&(t!=null?Va(e,!!a,t,!0):Va(e,!!a,a?[]:"",!1));return;case"textarea":X=q=null;for(m in a)if(l=a[m],a.hasOwnProperty(m)&&l!=null&&!o.hasOwnProperty(m))switch(m){case"value":break;case"children":break;default:We(e,t,m,null,o,l)}for(u in o)if(l=o[u],i=a[u],o.hasOwnProperty(u)&&(l!=null||i!=null))switch(u){case"value":q=l;break;case"defaultValue":X=l;break;case"children":break;case"dangerouslySetInnerHTML":if(l!=null)throw Error(c(91));break;default:l!==i&&We(e,t,u,l,o,i)}Pc(e,q,X);return;case"option":for(var ce in a)if(q=a[ce],a.hasOwnProperty(ce)&&q!=null&&!o.hasOwnProperty(ce))switch(ce){case"selected":e.selected=!1;break;default:We(e,t,ce,null,o,q)}for(N in o)if(q=o[N],X=a[N],o.hasOwnProperty(N)&&q!==X&&(q!=null||X!=null))switch(N){case"selected":e.selected=q&&typeof q!="function"&&typeof q!="symbol";break;default:We(e,t,N,q,o,X)}return;case"img":case"link":case"area":case"base":case"br":case"col":case"embed":case"hr":case"keygen":case"meta":case"param":case"source":case"track":case"wbr":case"menuitem":for(var ve in a)q=a[ve],a.hasOwnProperty(ve)&&q!=null&&!o.hasOwnProperty(ve)&&We(e,t,ve,null,o,q);for(U in o)if(q=o[U],X=a[U],o.hasOwnProperty(U)&&q!==X&&(q!=null||X!=null))switch(U){case"children":case"dangerouslySetInnerHTML":if(q!=null)throw Error(c(137,t));break;default:We(e,t,U,q,o,X)}return;default:if(ts(t)){for(var Pe in a)q=a[Pe],a.hasOwnProperty(Pe)&&q!==void 0&&!o.hasOwnProperty(Pe)&&Yr(e,t,Pe,void 0,o,q);for(V in o)q=o[V],X=a[V],!o.hasOwnProperty(V)||q===X||q===void 0&&X===void 0||Yr(e,t,V,q,o,X);return}}for(var B in a)q=a[B],a.hasOwnProperty(B)&&q!=null&&!o.hasOwnProperty(B)&&We(e,t,B,null,o,q);for(W in o)q=o[W],X=a[W],!o.hasOwnProperty(W)||q===X||q==null&&X==null||We(e,t,W,q,o,X)}function nh(e){switch(e){case"css":case"script":case"font":case"img":case"image":case"input":case"link":return!0;default:return!1}}function bg(){if(typeof performance.getEntriesByType=="function"){for(var e=0,t=0,a=performance.getEntriesByType("resource"),o=0;o<a.length;o++){var l=a[o],i=l.transferSize,u=l.initiatorType,m=l.duration;if(i&&m&&nh(u)){for(u=0,m=l.responseEnd,o+=1;o<a.length;o++){var N=a[o],U=N.startTime;if(U>m)break;var V=N.transferSize,W=N.initiatorType;V&&nh(W)&&(N=N.responseEnd,u+=V*(N<m?1:(m-U)/(N-U)))}if(--o,t+=8*(i+u)/(l.duration/1e3),e++,10<e)break}}if(0<e)return t/e/1e6}return navigator.connection&&(e=navigator.connection.downlink,typeof e=="number")?e:5}var qr=null,Ir=null;function Ei(e){return e.nodeType===9?e:e.ownerDocument}function ah(e){switch(e){case"http://www.w3.org/2000/svg":return 1;case"http://www.w3.org/1998/Math/MathML":return 2;default:return 0}}function oh(e,t){if(e===0)switch(t){case"svg":return 1;case"math":return 2;default:return 0}return e===1&&t==="foreignObject"?0:e}function Xr(e,t){return e==="textarea"||e==="noscript"||typeof t.children=="string"||typeof t.children=="number"||typeof t.children=="bigint"||typeof t.dangerouslySetInnerHTML=="object"&&t.dangerouslySetInnerHTML!==null&&t.dangerouslySetInnerHTML.__html!=null}var Jr=null;function vg(){var e=window.event;return e&&e.type==="popstate"?e===Jr?!1:(Jr=e,!0):(Jr=null,!1)}var lh=typeof setTimeout=="function"?setTimeout:void 0,_g=typeof clearTimeout=="function"?clearTimeout:void 0,ih=typeof Promise=="function"?Promise:void 0,xg=typeof queueMicrotask=="function"?queueMicrotask:typeof ih<"u"?function(e){return ih.resolve(null).then(e).catch(Sg)}:lh;function Sg(e){setTimeout(function(){throw e})}function ma(e){return e==="head"}function sh(e,t){var a=t,o=0;do{var l=a.nextSibling;if(e.removeChild(a),l&&l.nodeType===8)if(a=l.data,a==="/$"||a==="/&"){if(o===0){e.removeChild(l),No(t);return}o--}else if(a==="$"||a==="$?"||a==="$~"||a==="$!"||a==="&")o++;else if(a==="html")ml(e.ownerDocument.documentElement);else if(a==="head"){a=e.ownerDocument.head,ml(a);for(var i=a.firstChild;i;){var u=i.nextSibling,m=i.nodeName;i[Oo]||m==="SCRIPT"||m==="STYLE"||m==="LINK"&&i.rel.toLowerCase()==="stylesheet"||a.removeChild(i),i=u}}else a==="body"&&ml(e.ownerDocument.body);a=l}while(a);No(t)}function rh(e,t){var a=e;e=0;do{var o=a.nextSibling;if(a.nodeType===1?t?(a._stashedDisplay=a.style.display,a.style.display="none"):(a.style.display=a._stashedDisplay||"",a.getAttribute("style")===""&&a.removeAttribute("style")):a.nodeType===3&&(t?(a._stashedText=a.nodeValue,a.nodeValue=""):a.nodeValue=a._stashedText||""),o&&o.nodeType===8)if(a=o.data,a==="/$"){if(e===0)break;e--}else a!=="$"&&a!=="$?"&&a!=="$~"&&a!=="$!"||e++;a=o}while(a)}function Zr(e){var t=e.firstChild;for(t&&t.nodeType===10&&(t=t.nextSibling);t;){var a=t;switch(t=t.nextSibling,a.nodeName){case"HTML":case"HEAD":case"BODY":Zr(a),Wi(a);continue;case"SCRIPT":case"STYLE":continue;case"LINK":if(a.rel.toLowerCase()==="stylesheet")continue}e.removeChild(a)}}function wg(e,t,a,o){for(;e.nodeType===1;){var l=a;if(e.nodeName.toLowerCase()!==t.toLowerCase()){if(!o&&(e.nodeName!=="INPUT"||e.type!=="hidden"))break}else if(o){if(!e[Oo])switch(t){case"meta":if(!e.hasAttribute("itemprop"))break;return e;case"link":if(i=e.getAttribute("rel"),i==="stylesheet"&&e.hasAttribute("data-precedence"))break;if(i!==l.rel||e.getAttribute("href")!==(l.href==null||l.href===""?null:l.href)||e.getAttribute("crossorigin")!==(l.crossOrigin==null?null:l.crossOrigin)||e.getAttribute("title")!==(l.title==null?null:l.title))break;return e;case"style":if(e.hasAttribute("data-precedence"))break;return e;case"script":if(i=e.getAttribute("src"),(i!==(l.src==null?null:l.src)||e.getAttribute("type")!==(l.type==null?null:l.type)||e.getAttribute("crossorigin")!==(l.crossOrigin==null?null:l.crossOrigin))&&i&&e.hasAttribute("async")&&!e.hasAttribute("itemprop"))break;return e;default:return e}}else if(t==="input"&&e.type==="hidden"){var i=l.name==null?null:""+l.name;if(l.type==="hidden"&&e.getAttribute("name")===i)return e}else return e;if(e=gn(e.nextSibling),e===null)break}return null}function Tg(e,t,a){if(t==="")return null;for(;e.nodeType!==3;)if((e.nodeType!==1||e.nodeName!=="INPUT"||e.type!=="hidden")&&!a||(e=gn(e.nextSibling),e===null))return null;return e}function ch(e,t){for(;e.nodeType!==8;)if((e.nodeType!==1||e.nodeName!=="INPUT"||e.type!=="hidden")&&!t||(e=gn(e.nextSibling),e===null))return null;return e}function Vr(e){return e.data==="$?"||e.data==="$~"}function Qr(e){return e.data==="$!"||e.data==="$?"&&e.ownerDocument.readyState!=="loading"}function Eg(e,t){var a=e.ownerDocument;if(e.data==="$~")e._reactRetry=t;else if(e.data!=="$?"||a.readyState!=="loading")t();else{var o=function(){t(),a.removeEventListener("DOMContentLoaded",o)};a.addEventListener("DOMContentLoaded",o),e._reactRetry=o}}function gn(e){for(;e!=null;e=e.nextSibling){var t=e.nodeType;if(t===1||t===3)break;if(t===8){if(t=e.data,t==="$"||t==="$!"||t==="$?"||t==="$~"||t==="&"||t==="F!"||t==="F")break;if(t==="/$"||t==="/&")return null}}return e}var Kr=null;function uh(e){e=e.nextSibling;for(var t=0;e;){if(e.nodeType===8){var a=e.data;if(a==="/$"||a==="/&"){if(t===0)return gn(e.nextSibling);t--}else a!=="$"&&a!=="$!"&&a!=="$?"&&a!=="$~"&&a!=="&"||t++}e=e.nextSibling}return null}function dh(e){e=e.previousSibling;for(var t=0;e;){if(e.nodeType===8){var a=e.data;if(a==="$"||a==="$!"||a==="$?"||a==="$~"||a==="&"){if(t===0)return e;t--}else a!=="/$"&&a!=="/&"||t++}e=e.previousSibling}return null}function ph(e,t,a){switch(t=Ei(a),e){case"html":if(e=t.documentElement,!e)throw Error(c(452));return e;case"head":if(e=t.head,!e)throw Error(c(453));return e;case"body":if(e=t.body,!e)throw Error(c(454));return e;default:throw Error(c(451))}}function ml(e){for(var t=e.attributes;t.length;)e.removeAttributeNode(t[0]);Wi(e)}var yn=new Map,hh=new Set;function Ni(e){return typeof e.getRootNode=="function"?e.getRootNode():e.nodeType===9?e:e.ownerDocument}var Vn=z.d;z.d={f:Ng,r:Ag,D:Cg,C:jg,L:Mg,m:Dg,X:Og,S:kg,M:Rg};function Ng(){var e=Vn.f(),t=yi();return e||t}function Ag(e){var t=Xa(e);t!==null&&t.tag===5&&t.type==="form"?Md(t):Vn.r(e)}var wo=typeof document>"u"?null:document;function fh(e,t,a){var o=wo;if(o&&typeof t=="string"&&t){var l=cn(t);l='link[rel="'+e+'"][href="'+l+'"]',typeof a=="string"&&(l+='[crossorigin="'+a+'"]'),hh.has(l)||(hh.add(l),e={rel:e,crossOrigin:a,href:t},o.querySelector(l)===null&&(t=o.createElement("link"),Bt(t,"link",e),Nt(t),o.head.appendChild(t)))}}function Cg(e){Vn.D(e),fh("dns-prefetch",e,null)}function jg(e,t){Vn.C(e,t),fh("preconnect",e,t)}function Mg(e,t,a){Vn.L(e,t,a);var o=wo;if(o&&e&&t){var l='link[rel="preload"][as="'+cn(t)+'"]';t==="image"&&a&&a.imageSrcSet?(l+='[imagesrcset="'+cn(a.imageSrcSet)+'"]',typeof a.imageSizes=="string"&&(l+='[imagesizes="'+cn(a.imageSizes)+'"]')):l+='[href="'+cn(e)+'"]';var i=l;switch(t){case"style":i=To(e);break;case"script":i=Eo(e)}yn.has(i)||(e=x({rel:"preload",href:t==="image"&&a&&a.imageSrcSet?void 0:e,as:t},a),yn.set(i,e),o.querySelector(l)!==null||t==="style"&&o.querySelector(gl(i))||t==="script"&&o.querySelector(yl(i))||(t=o.createElement("link"),Bt(t,"link",e),Nt(t),o.head.appendChild(t)))}}function Dg(e,t){Vn.m(e,t);var a=wo;if(a&&e){var o=t&&typeof t.as=="string"?t.as:"script",l='link[rel="modulepreload"][as="'+cn(o)+'"][href="'+cn(e)+'"]',i=l;switch(o){case"audioworklet":case"paintworklet":case"serviceworker":case"sharedworker":case"worker":case"script":i=Eo(e)}if(!yn.has(i)&&(e=x({rel:"modulepreload",href:e},t),yn.set(i,e),a.querySelector(l)===null)){switch(o){case"audioworklet":case"paintworklet":case"serviceworker":case"sharedworker":case"worker":case"script":if(a.querySelector(yl(i)))return}o=a.createElement("link"),Bt(o,"link",e),Nt(o),a.head.appendChild(o)}}}function kg(e,t,a){Vn.S(e,t,a);var o=wo;if(o&&e){var l=Ja(o).hoistableStyles,i=To(e);t=t||"default";var u=l.get(i);if(!u){var m={loading:0,preload:null};if(u=o.querySelector(gl(i)))m.loading=5;else{e=x({rel:"stylesheet",href:e,"data-precedence":t},a),(a=yn.get(i))&&$r(e,a);var N=u=o.createElement("link");Nt(N),Bt(N,"link",e),N._p=new Promise(function(U,V){N.onload=U,N.onerror=V}),N.addEventListener("load",function(){m.loading|=1}),N.addEventListener("error",function(){m.loading|=2}),m.loading|=4,Ai(u,t,o)}u={type:"stylesheet",instance:u,count:1,state:m},l.set(i,u)}}}function Og(e,t){Vn.X(e,t);var a=wo;if(a&&e){var o=Ja(a).hoistableScripts,l=Eo(e),i=o.get(l);i||(i=a.querySelector(yl(l)),i||(e=x({src:e,async:!0},t),(t=yn.get(l))&&Wr(e,t),i=a.createElement("script"),Nt(i),Bt(i,"link",e),a.head.appendChild(i)),i={type:"script",instance:i,count:1,state:null},o.set(l,i))}}function Rg(e,t){Vn.M(e,t);var a=wo;if(a&&e){var o=Ja(a).hoistableScripts,l=Eo(e),i=o.get(l);i||(i=a.querySelector(yl(l)),i||(e=x({src:e,async:!0,type:"module"},t),(t=yn.get(l))&&Wr(e,t),i=a.createElement("script"),Nt(i),Bt(i,"link",e),a.head.appendChild(i)),i={type:"script",instance:i,count:1,state:null},o.set(l,i))}}function mh(e,t,a,o){var l=(l=me.current)?Ni(l):null;if(!l)throw Error(c(446));switch(e){case"meta":case"title":return null;case"style":return typeof a.precedence=="string"&&typeof a.href=="string"?(t=To(a.href),a=Ja(l).hoistableStyles,o=a.get(t),o||(o={type:"style",instance:null,count:0,state:null},a.set(t,o)),o):{type:"void",instance:null,count:0,state:null};case"link":if(a.rel==="stylesheet"&&typeof a.href=="string"&&typeof a.precedence=="string"){e=To(a.href);var i=Ja(l).hoistableStyles,u=i.get(e);if(u||(l=l.ownerDocument||l,u={type:"stylesheet",instance:null,count:0,state:{loading:0,preload:null}},i.set(e,u),(i=l.querySelector(gl(e)))&&!i._p&&(u.instance=i,u.state.loading=5),yn.has(e)||(a={rel:"preload",as:"style",href:a.href,crossOrigin:a.crossOrigin,integrity:a.integrity,media:a.media,hrefLang:a.hrefLang,referrerPolicy:a.referrerPolicy},yn.set(e,a),i||zg(l,e,a,u.state))),t&&o===null)throw Error(c(528,""));return u}if(t&&o!==null)throw Error(c(529,""));return null;case"script":return t=a.async,a=a.src,typeof a=="string"&&t&&typeof t!="function"&&typeof t!="symbol"?(t=Eo(a),a=Ja(l).hoistableScripts,o=a.get(t),o||(o={type:"script",instance:null,count:0,state:null},a.set(t,o)),o):{type:"void",instance:null,count:0,state:null};default:throw Error(c(444,e))}}function To(e){return'href="'+cn(e)+'"'}function gl(e){return'link[rel="stylesheet"]['+e+"]"}function gh(e){return x({},e,{"data-precedence":e.precedence,precedence:null})}function zg(e,t,a,o){e.querySelector('link[rel="preload"][as="style"]['+t+"]")?o.loading=1:(t=e.createElement("link"),o.preload=t,t.addEventListener("load",function(){return o.loading|=1}),t.addEventListener("error",function(){return o.loading|=2}),Bt(t,"link",a),Nt(t),e.head.appendChild(t))}function Eo(e){return'[src="'+cn(e)+'"]'}function yl(e){return"script[async]"+e}function yh(e,t,a){if(t.count++,t.instance===null)switch(t.type){case"style":var o=e.querySelector('style[data-href~="'+cn(a.href)+'"]');if(o)return t.instance=o,Nt(o),o;var l=x({},a,{"data-href":a.href,"data-precedence":a.precedence,href:null,precedence:null});return o=(e.ownerDocument||e).createElement("style"),Nt(o),Bt(o,"style",l),Ai(o,a.precedence,e),t.instance=o;case"stylesheet":l=To(a.href);var i=e.querySelector(gl(l));if(i)return t.state.loading|=4,t.instance=i,Nt(i),i;o=gh(a),(l=yn.get(l))&&$r(o,l),i=(e.ownerDocument||e).createElement("link"),Nt(i);var u=i;return u._p=new Promise(function(m,N){u.onload=m,u.onerror=N}),Bt(i,"link",o),t.state.loading|=4,Ai(i,a.precedence,e),t.instance=i;case"script":return i=Eo(a.src),(l=e.querySelector(yl(i)))?(t.instance=l,Nt(l),l):(o=a,(l=yn.get(i))&&(o=x({},a),Wr(o,l)),e=e.ownerDocument||e,l=e.createElement("script"),Nt(l),Bt(l,"link",o),e.head.appendChild(l),t.instance=l);case"void":return null;default:throw Error(c(443,t.type))}else t.type==="stylesheet"&&(t.state.loading&4)===0&&(o=t.instance,t.state.loading|=4,Ai(o,a.precedence,e));return t.instance}function Ai(e,t,a){for(var o=a.querySelectorAll('link[rel="stylesheet"][data-precedence],style[data-precedence]'),l=o.length?o[o.length-1]:null,i=l,u=0;u<o.length;u++){var m=o[u];if(m.dataset.precedence===t)i=m;else if(i!==l)break}i?i.parentNode.insertBefore(e,i.nextSibling):(t=a.nodeType===9?a.head:a,t.insertBefore(e,t.firstChild))}function $r(e,t){e.crossOrigin==null&&(e.crossOrigin=t.crossOrigin),e.referrerPolicy==null&&(e.referrerPolicy=t.referrerPolicy),e.title==null&&(e.title=t.title)}function Wr(e,t){e.crossOrigin==null&&(e.crossOrigin=t.crossOrigin),e.referrerPolicy==null&&(e.referrerPolicy=t.referrerPolicy),e.integrity==null&&(e.integrity=t.integrity)}var Ci=null;function bh(e,t,a){if(Ci===null){var o=new Map,l=Ci=new Map;l.set(a,o)}else l=Ci,o=l.get(a),o||(o=new Map,l.set(a,o));if(o.has(e))return o;for(o.set(e,null),a=a.getElementsByTagName(e),l=0;l<a.length;l++){var i=a[l];if(!(i[Oo]||i[kt]||e==="link"&&i.getAttribute("rel")==="stylesheet")&&i.namespaceURI!=="http://www.w3.org/2000/svg"){var u=i.getAttribute(t)||"";u=e+u;var m=o.get(u);m?m.push(i):o.set(u,[i])}}return o}function vh(e,t,a){e=e.ownerDocument||e,e.head.insertBefore(a,t==="title"?e.querySelector("head > title"):null)}function Bg(e,t,a){if(a===1||t.itemProp!=null)return!1;switch(e){case"meta":case"title":return!0;case"style":if(typeof t.precedence!="string"||typeof t.href!="string"||t.href==="")break;return!0;case"link":if(typeof t.rel!="string"||typeof t.href!="string"||t.href===""||t.onLoad||t.onError)break;switch(t.rel){case"stylesheet":return e=t.disabled,typeof t.precedence=="string"&&e==null;default:return!0}case"script":if(t.async&&typeof t.async!="function"&&typeof t.async!="symbol"&&!t.onLoad&&!t.onError&&t.src&&typeof t.src=="string")return!0}return!1}function _h(e){return!(e.type==="stylesheet"&&(e.state.loading&3)===0)}function Hg(e,t,a,o){if(a.type==="stylesheet"&&(typeof o.media!="string"||matchMedia(o.media).matches!==!1)&&(a.state.loading&4)===0){if(a.instance===null){var l=To(o.href),i=t.querySelector(gl(l));if(i){t=i._p,t!==null&&typeof t=="object"&&typeof t.then=="function"&&(e.count++,e=ji.bind(e),t.then(e,e)),a.state.loading|=4,a.instance=i,Nt(i);return}i=t.ownerDocument||t,o=gh(o),(l=yn.get(l))&&$r(o,l),i=i.createElement("link"),Nt(i);var u=i;u._p=new Promise(function(m,N){u.onload=m,u.onerror=N}),Bt(i,"link",o),a.instance=i}e.stylesheets===null&&(e.stylesheets=new Map),e.stylesheets.set(a,t),(t=a.state.preload)&&(a.state.loading&3)===0&&(e.count++,a=ji.bind(e),t.addEventListener("load",a),t.addEventListener("error",a))}}var Pr=0;function Gg(e,t){return e.stylesheets&&e.count===0&&Di(e,e.stylesheets),0<e.count||0<e.imgCount?function(a){var o=setTimeout(function(){if(e.stylesheets&&Di(e,e.stylesheets),e.unsuspend){var i=e.unsuspend;e.unsuspend=null,i()}},6e4+t);0<e.imgBytes&&Pr===0&&(Pr=62500*bg());var l=setTimeout(function(){if(e.waitingForImages=!1,e.count===0&&(e.stylesheets&&Di(e,e.stylesheets),e.unsuspend)){var i=e.unsuspend;e.unsuspend=null,i()}},(e.imgBytes>Pr?50:800)+t);return e.unsuspend=a,function(){e.unsuspend=null,clearTimeout(o),clearTimeout(l)}}:null}function ji(){if(this.count--,this.count===0&&(this.imgCount===0||!this.waitingForImages)){if(this.stylesheets)Di(this,this.stylesheets);else if(this.unsuspend){var e=this.unsuspend;this.unsuspend=null,e()}}}var Mi=null;function Di(e,t){e.stylesheets=null,e.unsuspend!==null&&(e.count++,Mi=new Map,t.forEach(Ug,e),Mi=null,ji.call(e))}function Ug(e,t){if(!(t.state.loading&4)){var a=Mi.get(e);if(a)var o=a.get(null);else{a=new Map,Mi.set(e,a);for(var l=e.querySelectorAll("link[data-precedence],style[data-precedence]"),i=0;i<l.length;i++){var u=l[i];(u.nodeName==="LINK"||u.getAttribute("media")!=="not all")&&(a.set(u.dataset.precedence,u),o=u)}o&&a.set(null,o)}l=t.instance,u=l.getAttribute("data-precedence"),i=a.get(u)||o,i===o&&a.set(null,l),a.set(u,l),this.count++,o=ji.bind(this),l.addEventListener("load",o),l.addEventListener("error",o),i?i.parentNode.insertBefore(l,i.nextSibling):(e=e.nodeType===9?e.head:e,e.insertBefore(l,e.firstChild)),t.state.loading|=4}}var bl={$$typeof:k,Provider:null,Consumer:null,_currentValue:$,_currentValue2:$,_threadCount:0};function Lg(e,t,a,o,l,i,u,m,N){this.tag=1,this.containerInfo=e,this.pingCache=this.current=this.pendingChildren=null,this.timeoutHandle=-1,this.callbackNode=this.next=this.pendingContext=this.context=this.cancelPendingCommit=null,this.callbackPriority=0,this.expirationTimes=Vi(-1),this.entangledLanes=this.shellSuspendCounter=this.errorRecoveryDisabledLanes=this.expiredLanes=this.warmLanes=this.pingedLanes=this.suspendedLanes=this.pendingLanes=0,this.entanglements=Vi(0),this.hiddenUpdates=Vi(null),this.identifierPrefix=o,this.onUncaughtError=l,this.onCaughtError=i,this.onRecoverableError=u,this.pooledCache=null,this.pooledCacheLanes=0,this.formState=N,this.incompleteTransitions=new Map}function xh(e,t,a,o,l,i,u,m,N,U,V,W){return e=new Lg(e,t,a,u,N,U,V,W,m),t=1,i===!0&&(t|=24),i=Pt(3,null,null,t),e.current=i,i.stateNode=e,t=Ds(),t.refCount++,e.pooledCache=t,t.refCount++,i.memoizedState={element:o,isDehydrated:a,cache:t},zs(i),e}function Sh(e){return e?(e=to,e):to}function wh(e,t,a,o,l,i){l=Sh(l),o.context===null?o.context=l:o.pendingContext=l,o=oa(t),o.payload={element:a},i=i===void 0?null:i,i!==null&&(o.callback=i),a=la(e,o,t),a!==null&&(Kt(a,e,t),$o(a,e,t))}function Th(e,t){if(e=e.memoizedState,e!==null&&e.dehydrated!==null){var a=e.retryLane;e.retryLane=a!==0&&a<t?a:t}}function Fr(e,t){Th(e,t),(e=e.alternate)&&Th(e,t)}function Eh(e){if(e.tag===13||e.tag===31){var t=Ca(e,67108864);t!==null&&Kt(t,e,67108864),Fr(e,67108864)}}function Nh(e){if(e.tag===13||e.tag===31){var t=an();t=Qi(t);var a=Ca(e,t);a!==null&&Kt(a,e,t),Fr(e,t)}}var ki=!0;function Yg(e,t,a,o){var l=R.T;R.T=null;var i=z.p;try{z.p=2,ec(e,t,a,o)}finally{z.p=i,R.T=l}}function qg(e,t,a,o){var l=R.T;R.T=null;var i=z.p;try{z.p=8,ec(e,t,a,o)}finally{z.p=i,R.T=l}}function ec(e,t,a,o){if(ki){var l=tc(o);if(l===null)Lr(e,t,o,Oi,a),Ch(e,o);else if(Xg(l,e,t,a,o))o.stopPropagation();else if(Ch(e,o),t&4&&-1<Ig.indexOf(e)){for(;l!==null;){var i=Xa(l);if(i!==null)switch(i.tag){case 3:if(i=i.stateNode,i.current.memoizedState.isDehydrated){var u=jn(i.pendingLanes);if(u!==0){var m=i;for(m.pendingLanes|=2,m.entangledLanes|=2;u;){var N=1<<31-Fe(u);m.entanglements[1]|=N,u&=~N}Nn(i),(Je&6)===0&&(mi=fe()+500,pl(0))}}break;case 31:case 13:m=Ca(i,2),m!==null&&Kt(m,i,2),yi(),Fr(i,2)}if(i=tc(o),i===null&&Lr(e,t,o,Oi,a),i===l)break;l=i}l!==null&&o.stopPropagation()}else Lr(e,t,o,null,a)}}function tc(e){return e=as(e),nc(e)}var Oi=null;function nc(e){if(Oi=null,e=Ia(e),e!==null){var t=h(e);if(t===null)e=null;else{var a=t.tag;if(a===13){if(e=y(t),e!==null)return e;e=null}else if(a===31){if(e=g(t),e!==null)return e;e=null}else if(a===3){if(t.stateNode.current.memoizedState.isDehydrated)return t.tag===3?t.stateNode.containerInfo:null;e=null}else t!==e&&(e=null)}}return Oi=e,null}function Ah(e){switch(e){case"beforetoggle":case"cancel":case"click":case"close":case"contextmenu":case"copy":case"cut":case"auxclick":case"dblclick":case"dragend":case"dragstart":case"drop":case"focusin":case"focusout":case"input":case"invalid":case"keydown":case"keypress":case"keyup":case"mousedown":case"mouseup":case"paste":case"pause":case"play":case"pointercancel":case"pointerdown":case"pointerup":case"ratechange":case"reset":case"resize":case"seeked":case"submit":case"toggle":case"touchcancel":case"touchend":case"touchstart":case"volumechange":case"change":case"selectionchange":case"textInput":case"compositionstart":case"compositionend":case"compositionupdate":case"beforeblur":case"afterblur":case"beforeinput":case"blur":case"fullscreenchange":case"focus":case"hashchange":case"popstate":case"select":case"selectstart":return 2;case"drag":case"dragenter":case"dragexit":case"dragleave":case"dragover":case"mousemove":case"mouseout":case"mouseover":case"pointermove":case"pointerout":case"pointerover":case"scroll":case"touchmove":case"wheel":case"mouseenter":case"mouseleave":case"pointerenter":case"pointerleave":return 8;case"message":switch(Mt()){case Ht:return 2;case ut:return 8;case ht:case qt:return 32;case Gt:return 268435456;default:return 32}default:return 32}}var ac=!1,ga=null,ya=null,ba=null,vl=new Map,_l=new Map,va=[],Ig="mousedown mouseup touchcancel touchend touchstart auxclick dblclick pointercancel pointerdown pointerup dragend dragstart drop compositionend compositionstart keydown keypress keyup input textInput copy cut paste click change contextmenu reset".split(" ");function Ch(e,t){switch(e){case"focusin":case"focusout":ga=null;break;case"dragenter":case"dragleave":ya=null;break;case"mouseover":case"mouseout":ba=null;break;case"pointerover":case"pointerout":vl.delete(t.pointerId);break;case"gotpointercapture":case"lostpointercapture":_l.delete(t.pointerId)}}function xl(e,t,a,o,l,i){return e===null||e.nativeEvent!==i?(e={blockedOn:t,domEventName:a,eventSystemFlags:o,nativeEvent:i,targetContainers:[l]},t!==null&&(t=Xa(t),t!==null&&Eh(t)),e):(e.eventSystemFlags|=o,t=e.targetContainers,l!==null&&t.indexOf(l)===-1&&t.push(l),e)}function Xg(e,t,a,o,l){switch(t){case"focusin":return ga=xl(ga,e,t,a,o,l),!0;case"dragenter":return ya=xl(ya,e,t,a,o,l),!0;case"mouseover":return ba=xl(ba,e,t,a,o,l),!0;case"pointerover":var i=l.pointerId;return vl.set(i,xl(vl.get(i)||null,e,t,a,o,l)),!0;case"gotpointercapture":return i=l.pointerId,_l.set(i,xl(_l.get(i)||null,e,t,a,o,l)),!0}return!1}function jh(e){var t=Ia(e.target);if(t!==null){var a=h(t);if(a!==null){if(t=a.tag,t===13){if(t=y(a),t!==null){e.blockedOn=t,Ic(e.priority,function(){Nh(a)});return}}else if(t===31){if(t=g(a),t!==null){e.blockedOn=t,Ic(e.priority,function(){Nh(a)});return}}else if(t===3&&a.stateNode.current.memoizedState.isDehydrated){e.blockedOn=a.tag===3?a.stateNode.containerInfo:null;return}}}e.blockedOn=null}function Ri(e){if(e.blockedOn!==null)return!1;for(var t=e.targetContainers;0<t.length;){var a=tc(e.nativeEvent);if(a===null){a=e.nativeEvent;var o=new a.constructor(a.type,a);ns=o,a.target.dispatchEvent(o),ns=null}else return t=Xa(a),t!==null&&Eh(t),e.blockedOn=a,!1;t.shift()}return!0}function Mh(e,t,a){Ri(e)&&a.delete(t)}function Jg(){ac=!1,ga!==null&&Ri(ga)&&(ga=null),ya!==null&&Ri(ya)&&(ya=null),ba!==null&&Ri(ba)&&(ba=null),vl.forEach(Mh),_l.forEach(Mh)}function zi(e,t){e.blockedOn===t&&(e.blockedOn=null,ac||(ac=!0,r.unstable_scheduleCallback(r.unstable_NormalPriority,Jg)))}var Bi=null;function Dh(e){Bi!==e&&(Bi=e,r.unstable_scheduleCallback(r.unstable_NormalPriority,function(){Bi===e&&(Bi=null);for(var t=0;t<e.length;t+=3){var a=e[t],o=e[t+1],l=e[t+2];if(typeof o!="function"){if(nc(o||a)===null)continue;break}var i=Xa(a);i!==null&&(e.splice(t,3),t-=3,tr(i,{pending:!0,data:l,method:a.method,action:o},o,l))}}))}function No(e){function t(N){return zi(N,e)}ga!==null&&zi(ga,e),ya!==null&&zi(ya,e),ba!==null&&zi(ba,e),vl.forEach(t),_l.forEach(t);for(var a=0;a<va.length;a++){var o=va[a];o.blockedOn===e&&(o.blockedOn=null)}for(;0<va.length&&(a=va[0],a.blockedOn===null);)jh(a),a.blockedOn===null&&va.shift();if(a=(e.ownerDocument||e).$$reactFormReplay,a!=null)for(o=0;o<a.length;o+=3){var l=a[o],i=a[o+1],u=l[It]||null;if(typeof i=="function")u||Dh(a);else if(u){var m=null;if(i&&i.hasAttribute("formAction")){if(l=i,u=i[It]||null)m=u.formAction;else if(nc(l)!==null)continue}else m=u.action;typeof m=="function"?a[o+1]=m:(a.splice(o,3),o-=3),Dh(a)}}}function kh(){function e(i){i.canIntercept&&i.info==="react-transition"&&i.intercept({handler:function(){return new Promise(function(u){return l=u})},focusReset:"manual",scroll:"manual"})}function t(){l!==null&&(l(),l=null),o||setTimeout(a,20)}function a(){if(!o&&!navigation.transition){var i=navigation.currentEntry;i&&i.url!=null&&navigation.navigate(i.url,{state:i.getState(),info:"react-transition",history:"replace"})}}if(typeof navigation=="object"){var o=!1,l=null;return navigation.addEventListener("navigate",e),navigation.addEventListener("navigatesuccess",t),navigation.addEventListener("navigateerror",t),setTimeout(a,100),function(){o=!0,navigation.removeEventListener("navigate",e),navigation.removeEventListener("navigatesuccess",t),navigation.removeEventListener("navigateerror",t),l!==null&&(l(),l=null)}}}function oc(e){this._internalRoot=e}Hi.prototype.render=oc.prototype.render=function(e){var t=this._internalRoot;if(t===null)throw Error(c(409));var a=t.current,o=an();wh(a,o,e,t,null,null)},Hi.prototype.unmount=oc.prototype.unmount=function(){var e=this._internalRoot;if(e!==null){this._internalRoot=null;var t=e.containerInfo;wh(e.current,2,null,e,null,null),yi(),t[qa]=null}};function Hi(e){this._internalRoot=e}Hi.prototype.unstable_scheduleHydration=function(e){if(e){var t=qc();e={blockedOn:null,target:e,priority:t};for(var a=0;a<va.length&&t!==0&&t<va[a].priority;a++);va.splice(a,0,e),a===0&&jh(e)}};var Oh=n.version;if(Oh!=="19.2.4")throw Error(c(527,Oh,"19.2.4"));z.findDOMNode=function(e){var t=e._reactInternals;if(t===void 0)throw typeof e.render=="function"?Error(c(188)):(e=Object.keys(e).join(","),Error(c(268,e)));return e=b(t),e=e!==null?E(e):null,e=e===null?null:e.stateNode,e};var Zg={bundleType:0,version:"19.2.4",rendererPackageName:"react-dom",currentDispatcherRef:R,reconcilerVersion:"19.2.4"};if(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__<"u"){var Gi=__REACT_DEVTOOLS_GLOBAL_HOOK__;if(!Gi.isDisabled&&Gi.supportsFiber)try{Tt=Gi.inject(Zg),Ye=Gi}catch{}}return Sl.createRoot=function(e,t){if(!d(e))throw Error(c(299));var a=!1,o="",l=Ld,i=Yd,u=qd;return t!=null&&(t.unstable_strictMode===!0&&(a=!0),t.identifierPrefix!==void 0&&(o=t.identifierPrefix),t.onUncaughtError!==void 0&&(l=t.onUncaughtError),t.onCaughtError!==void 0&&(i=t.onCaughtError),t.onRecoverableError!==void 0&&(u=t.onRecoverableError)),t=xh(e,1,!1,null,null,a,o,null,l,i,u,kh),e[qa]=t.current,Ur(e),new oc(t)},Sl.hydrateRoot=function(e,t,a){if(!d(e))throw Error(c(299));var o=!1,l="",i=Ld,u=Yd,m=qd,N=null;return a!=null&&(a.unstable_strictMode===!0&&(o=!0),a.identifierPrefix!==void 0&&(l=a.identifierPrefix),a.onUncaughtError!==void 0&&(i=a.onUncaughtError),a.onCaughtError!==void 0&&(u=a.onCaughtError),a.onRecoverableError!==void 0&&(m=a.onRecoverableError),a.formState!==void 0&&(N=a.formState)),t=xh(e,1,!0,t,a??null,o,l,N,i,u,m,kh),t.context=Sh(null),a=t.current,o=an(),o=Qi(o),l=oa(o),l.callback=null,la(a,l,o),a=o,t.current.lanes=a,ko(t,a),Nn(t),e[qa]=t.current,Ur(e),new Hi(t)},Sl.version="19.2.4",Sl}var Hh;function yy(){if(Hh)return lc.exports;Hh=1;function r(){if(!(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__>"u"||typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE!="function"))try{__REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE(r)}catch(n){console.error(n)}}return r(),lc.exports=gy(),lc.exports}var by=yy(),rc={exports:{}},cc={};/**
 * @license React
 * react-compiler-runtime.production.js
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */var Gh;function vy(){if(Gh)return cc;Gh=1;var r=gf().__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE;return cc.c=function(n){return r.H.useMemoCache(n)},cc}var Uh;function _y(){return Uh||(Uh=1,rc.exports=vy()),rc.exports}var Ee=_y();const xy="_wrapper_bt1w8_2",Sy="_header_bt1w8_10",wy="_headerActions_bt1w8_21",Ty="_title_bt1w8_27",Ey="_panelGroup_bt1w8_36",Ny="_clipboardToggle_bt1w8_43",Ay="_helpToggle_bt1w8_66",Cy="_helpButtonWrapper_bt1w8_93",jy="_helpTogglePulsing_bt1w8_97",My="_helpHint_bt1w8_112",Dy="_helpHintFading_bt1w8_139",ky="_helpHintKbd_bt1w8_144",Oy="_resizeHandle_bt1w8_153",$t={wrapper:xy,header:Sy,headerActions:wy,title:Ty,panelGroup:Ey,clipboardToggle:Ny,helpToggle:Ay,helpButtonWrapper:Cy,helpTogglePulsing:jy,helpHint:My,helpHintFading:Dy,helpHintKbd:ky,resizeHandle:Oy},Ry=r=>{try{return!new DOMParser().parseFromString(r.trim(),"text/xml").querySelector("parsererror")}catch{return!1}},zy=r=>{try{return JSON.parse(r),!0}catch{return!1}},By=r=>r.trim()?zy(r)?{valid:!0,error:null,type:"json"}:Ry(r)?{valid:!0,error:null,type:"xml"}:{valid:!1,error:"Invalid JSON/XML format",type:null}:{valid:!0,error:null,type:null},_c=r=>{try{const n=JSON.parse(r);return JSON.stringify(n,null,2)}catch{return r}},Hy=()=>{const r=Ee.c(8);let n;r[0]===Symbol.for("react.memo_cache_sentinel")?(n=[],r[0]=n):n=r[0];const[s,c]=w.useState(n),d=w.useRef(0);let h;r[1]===Symbol.for("react.memo_cache_sentinel")?(h=new Set,r[1]=h):h=r[1];const y=w.useRef(h);let g,f;r[2]===Symbol.for("react.memo_cache_sentinel")?(g=()=>()=>{y.current.forEach(clearTimeout)},f=[],r[2]=g,r[3]=f):(g=r[2],f=r[3]),w.useEffect(g,f);let b;r[4]===Symbol.for("react.memo_cache_sentinel")?(b=(A,_)=>{const v=_===void 0?"info":_,T=d.current=d.current+1;c(k=>[...k,{id:T,message:A,type:v}]);const O=setTimeout(()=>{y.current.delete(O),c(k=>k.filter(H=>H.id!==T))},3e3);y.current.add(O)},r[4]=b):b=r[4];const E=b;let x;r[5]===Symbol.for("react.memo_cache_sentinel")?(x=A=>{c(_=>_.filter(v=>v.id!==A))},r[5]=x):x=r[5];const S=x;let j;return r[6]!==s?(j={toasts:s,addToast:E,removeToast:S},r[6]=s,r[7]=j):j=r[7],j},Sa=(r,n)=>{const s=w.useCallback(()=>{try{const h=window.localStorage.getItem(r);return h?JSON.parse(h):n}catch{return n}},[r]),[c,d]=w.useState(s);return w.useEffect(()=>{d(s())},[r]),w.useEffect(()=>{try{window.localStorage.setItem(r,JSON.stringify(c))}catch(h){console.error(`Error setting localStorage key "${r}":`,h)}},[r,c]),w.useEffect(()=>{const h=y=>{(y.key===r||y.key===null)&&d(s())};return window.addEventListener("storage",h),()=>window.removeEventListener("storage",h)},[r,s]),w.useEffect(()=>{const h=()=>d(s());return window.addEventListener("focus",h),document.addEventListener("visibilitychange",h),()=>{window.removeEventListener("focus",h),document.removeEventListener("visibilitychange",h)}},[s]),[c,d]},Gy=200,Lh=50,Uy=8,Ly=63488,Yy=2e4,Qn=[{path:"/json-path",label:"JSON-Path",title:"JSON-Path Playground",wsPath:"/ws/json/path",storageKeyPayload:"jsonpath-last-payload",storageKeyHistory:"jsonpath-command-history",storageKeyTab:"jsonpath-right-tab",supportsUpload:!0,tabs:["payload","graph","graph-data"]},{path:"/",label:"Minigraph",title:"Minigraph Playground",wsPath:"/ws/graph/playground",storageKeyPayload:"minigraph-last-payload",storageKeyHistory:"minigraph-command-history",storageKeyTab:"minigraph-right-tab",storageKeySavedGraphs:"minigraph-saved-graphs",storageKeyHelpTopic:"minigraph-help-topic",supportsClipboard:!0,supportsHelp:!0,supportsAuthoring:!0,tabs:["graph","graph-data"]}],Yi={json_simple:JSON.stringify({name:"John Doe",age:30,city:"New York"},null,2),json_nested:JSON.stringify({user:{name:"Jane Smith",profile:{email:"jane@example.com",address:{city:"San Francisco",country:"USA"}}}},null,2),json_array:JSON.stringify([{id:1,name:"Item 1",status:"active"},{id:2,name:"Item 2",status:"pending"},{id:3,name:"Item 3",status:"inactive"}],null,2),xml_simple:`<?xml version="1.0" encoding="UTF-8"?>
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
</items>`};function vf(r){return`ws://${window.location.host}${r}`}function uc(r,n,s,c){const d=r[n]??{phase:"idle",messages:[]},h=[...d.messages,{id:s,raw:c}];return h.length>Gy&&h.shift(),{...r,[n]:{...d,messages:h}}}function qy(r,n){const s=r[n.path]??{phase:"idle",messages:[]};switch(n.type){case"CONNECTING":return{...r,[n.path]:{...s,phase:"connecting"}};case"CONNECTED":return uc({...r,[n.path]:{...s,phase:"connected"}},n.path,n.id,n.msg);case"MESSAGE_RECEIVED":return uc(r,n.path,n.id,n.msg);case"DISCONNECTED":return uc({...r,[n.path]:{...s,phase:"idle"}},n.path,n.id,n.msg);case"CONNECT_ERROR":return{...r,[n.path]:{...s,phase:"idle"}};case"CLEAR_MESSAGES":return{...r,[n.path]:{...s,messages:[]}};default:return r}}const _f=w.createContext(null);function Iy({children:r}){const[n,s]=w.useReducer(qy,{}),c=w.useRef({}),d=w.useRef({}),h=w.useRef({});w.useEffect(()=>()=>{Object.entries(c.current).forEach(([C,Y])=>{Y==null||Y.close();const J=d.current[C];J&&clearInterval(J)})},[]);const y=C=>vf(C),g=C=>(h.current[C]=(h.current[C]??0)+1,h.current[C]),f=()=>{const C=new Date().toString(),Y=C.indexOf("GMT");return Y>0?C.substring(0,Y).trim():C},b=(C,Y)=>JSON.stringify({type:C,message:Y,time:f()}),E=C=>{try{const Y=JSON.parse(C);if(Y!==null&&typeof Y=="object"){const J=Y.type;return J==="ping"||J==="pong"}}catch{}return!1},x=w.useCallback((C,Y)=>{if(!window.WebSocket){Y==null||Y("WebSocket not supported by your browser","error");return}const J=c.current[C];if(J&&(J.readyState===WebSocket.OPEN||J.readyState===WebSocket.CONNECTING)){Y==null||Y("Already connected","error");return}s({type:"CONNECTING",path:C});const I=new WebSocket(y(C));c.current[C]=I,I.onopen=()=>{s({type:"CONNECTED",path:C,id:g(C),msg:b("info","connected")}),Y==null||Y("Connected to WebSocket","success"),I.send(JSON.stringify({type:"welcome"})),d.current[C]=setInterval(()=>{I.readyState===WebSocket.OPEN&&I.send(b("ping","keep alive"))},Yy)},I.onmessage=K=>{E(K.data)||s({type:"MESSAGE_RECEIVED",path:C,id:g(C),msg:K.data})},I.onerror=()=>{s({type:"CONNECT_ERROR",path:C})},I.onclose=K=>{const Z=d.current[C];Z&&(clearInterval(Z),d.current[C]=null),s({type:"DISCONNECTED",path:C,id:g(C),msg:b("info",`disconnected - (${K.code}) ${K.reason}`)}),Y==null||Y("Disconnected from WebSocket","info"),c.current[C]===I&&(c.current[C]=null)}},[]),S=w.useCallback(C=>{const Y=c.current[C];Y?Y.close():s({type:"MESSAGE_RECEIVED",path:C,id:g(C),msg:b("error","already disconnected")})},[]);w.useEffect(()=>(Qn.forEach(C=>{x(C.wsPath)}),()=>{Qn.forEach(C=>{const Y=c.current[C.wsPath];Y&&Y.close()})}),[]);const j=w.useCallback((C,Y)=>{const J=c.current[C];return J&&J.readyState===WebSocket.OPEN?(J.send(Y),!0):!1},[]),A=w.useCallback((C,Y)=>{s({type:"MESSAGE_RECEIVED",path:C,id:g(C),msg:Y})},[]),_=w.useCallback(C=>{s({type:"CLEAR_MESSAGES",path:C})},[]),[v,T]=w.useState({}),O=w.useCallback((C,Y)=>{T(J=>{if(Y===null){const I={...J};return delete I[C],I}return{...J,[C]:Y}})},[]),k=w.useCallback(C=>v[C]??null,[v]),H=w.useCallback(C=>{const Y=v[C]??null;return Y!==null&&T(J=>{const I={...J};return delete I[C],I}),Y},[v]),D=w.useCallback(C=>n[C]??{phase:"idle",messages:[]},[n]),L=w.useMemo(()=>({getSlot:D,connect:x,disconnect:S,send:j,appendMessage:A,clearMessages:_,setPendingPayload:O,peekPendingPayload:k,takePendingPayload:H}),[D,x,S,j,A,_,O,k,H]);return p.jsx(_f.Provider,{value:L,children:r})}function zc(){const r=w.useContext(_f);if(!r)throw new Error("useWebSocketContext must be used inside <WebSocketProvider>");return r}const Xy=r=>{try{const n=JSON.parse(r);return{type:n.type||"info",message:n.message||r,time:n.time,raw:r}}catch{return{type:"raw",message:r,time:null,raw:r}}},Jy=r=>({info:"ℹ️",error:"❌",ping:"🔄",welcome:"👋",raw:""})[r]??"•",Nl=r=>{try{const n=JSON.parse(r);if(typeof n=="object"&&n!==null)return{isJSON:!0,data:n}}catch{}return{isJSON:!1,data:null}};function Zy(r){if(!r.includes("Graph exported to "))return null;const n=Hc(r);if(!n)return null;const s=n.split("/")[4];return s?{graphName:s,apiPath:n}:null}function Vy(r){return r.includes("Invalid filename")?{reason:"invalid-name"}:r.includes("Expect root node name")?{reason:"root-name-conflict"}:null}function Bc(r){const n=Nl(r);return n.isJSON?(typeof n.data.type=="string",!1):!0}function Hc(r){const n=r.match(/\/api\/graph\/model\/([^\s'"]+)/);return n?n[0]:null}function xf(r){return Bc(r)?Hc(r)!==null:!1}function Sf(r){const n=r.match(/\/api\/json\/content\/([\w-]+)/);return n?n[0]:null}function Qy(r){const n=r.match(/Large payload \((\d+)\)\s*->\s*GET\s+(\/api\/inspect\/[^\s]+)/i);if(!n)return null;const s=parseInt(n[1],10),c=n[2],h=`${c.split("/").filter(Boolean).pop()??"payload"}.json`;return{apiPath:c,byteSize:s,filename:h}}function Ky(r){const n=r.match(/You may upload .*?->\s*POST\s+(\/api\/mock\/[\w-]+)/i);return n?n[1]:null}function $y(r){if(!r.startsWith("> "))return!1;const n=r.slice(2).trim().toLowerCase();return n==="help"||n.startsWith("help ")?!0:n.startsWith("describe ")?!n.slice(9).trim().startsWith("graph"):!1}function Wy(r){if(!r.startsWith("> ")||!r.slice(2).trimStart().toLowerCase().startsWith("import graph from "))return null;const s=r.slice(2).trimStart().slice(18).trim();return s.length>0?s:null}const Py=/^node ([A-Za-z0-9_-]+) created$/,Fy=/^node ([A-Za-z0-9_-]+) already exists$/,e1=/^ERROR: (.+)$/;function t1(r){const n=r.trim();if(n.startsWith("> "))return null;const s=n.match(Py);if(s)return{status:"accepted",alias:s[1],message:n};const c=n.match(Fy);return c?{status:"rejected",alias:c[1],message:n}:n.match(e1)?{status:"error",alias:null,message:n}:null}function n1(r){if(!Bc(r)||r.startsWith("> ")||xf(r))return null;const n=r.toLowerCase();return n.includes("graph model imported as draft")?"import-graph":n.includes(" -> ")&&n.includes("removed")||n.startsWith("node ")&&(n.includes(" created")||n.includes(" updated")||n.includes(" deleted")||n.includes(" connected to ")||n.includes(" imported from ")||n.includes(" overwritten by node from "))?"node-mutation":null}const a1={command:"",historyIndex:-1,draftCommand:""};function o1(r,n){switch(n.type){case"SET_COMMAND":return{...r,command:n.value,historyIndex:-1,draftCommand:""};case"CLEAR_COMMAND":return{...r,command:"",historyIndex:-1,draftCommand:""};case"SET_HISTORY_INDEX":return{...r,historyIndex:n.index,command:n.command};case"ENTER_HISTORY":return{...r,historyIndex:0,command:n.command,draftCommand:r.command};case"EXIT_HISTORY":return{...r,historyIndex:-1,command:r.draftCommand,draftCommand:""};default:return r}}function l1(r){const n=Ee.c(77),{wsPath:s,storageKeyHistory:c,payload:d,addToast:h,bus:y,handleLocalCommand:g}=r,f=zc();let b;n[0]!==f||n[1]!==s?(b=f.getSlot(s),n[0]=f,n[1]=s,n[2]=b):b=n[2];const{phase:E,messages:x}=b,S=E==="connected",j=E==="connecting",[A,_]=w.useReducer(o1,a1),{command:v,historyIndex:T}=A;let O;n[3]===Symbol.for("react.memo_cache_sentinel")?(O=[],n[3]=O):O=n[3];const[k,H]=Sa(c,O),D=w.useRef(null),L=w.useRef(!1);let C;n[4]===Symbol.for("react.memo_cache_sentinel")?(C=()=>{D.current&&(D.current.scrollTop=D.current.scrollHeight)},n[4]=C):C=n[4];let Y;n[5]!==x?(Y=[x],n[5]=x,n[6]=Y):Y=n[6],w.useEffect(C,Y);let J;n[7]!==h||n[8]!==f||n[9]!==s?(J=()=>{f.connect(s,h)},n[7]=h,n[8]=f,n[9]=s,n[10]=J):J=n[10];const I=J;let K;n[11]!==f||n[12]!==s?(K=()=>{f.disconnect(s)},n[11]=f,n[12]=s,n[13]=K):K=n[13];const Z=K;let ee;n[14]!==v||n[15]!==f||n[16]!==g||n[17]!==k||n[18]!==d||n[19]!==E||n[20]!==H||n[21]!==s?(ee=()=>{if(E!=="connected")return;const F=v.trim();if(F.length!==0){if((g==null?void 0:g(F))===!0){k[0]!==F&&H(ye=>[F,...ye].slice(0,Lh)),f.appendMessage(s,"> "+F),_({type:"CLEAR_COMMAND"});return}f.send(s,F),k[0]!==F&&H(ye=>[F,...ye].slice(0,Lh)),F==="load"&&(d.length===0?f.appendMessage(s,"ERROR: please paste JSON/XML payload in input text area"):f.send(s,d)),_({type:"CLEAR_COMMAND"})}},n[14]=v,n[15]=f,n[16]=g,n[17]=k,n[18]=d,n[19]=E,n[20]=H,n[21]=s,n[22]=ee):ee=n[22];const P=ee;let le;n[23]!==k||n[24]!==T?(le=F=>{if(F.key==="ArrowUp"){if(F.preventDefault(),k.length===0)return;if(T===-1)_({type:"ENTER_HISTORY",command:k[0]});else if(T<k.length-1){const ye=T+1;_({type:"SET_HISTORY_INDEX",index:ye,command:k[ye]})}}else if(F.key==="ArrowDown")if(F.preventDefault(),T<=0)T===0&&_({type:"EXIT_HISTORY"});else{const ye=T-1;_({type:"SET_HISTORY_INDEX",index:ye,command:k[ye]})}},n[23]=k,n[24]=T,n[25]=le):le=n[25];const R=le;let z,$;n[26]!==h||n[27]!==y||n[28]!==f||n[29]!==d||n[30]!==s?($=()=>{if(y)return y.on("upload.contentPath",F=>{if(!L.current)return;if(L.current=!1,d.length===0){f.appendMessage(s,"ERROR: please paste JSON/XML payload in the input text area");return}let ye;try{ye=JSON.stringify(JSON.parse(d))}catch{f.appendMessage(s,"ERROR: payload is not valid JSON — cannot upload");return}fetch(F.uploadPath,{method:"POST",headers:{"Content-Type":"application/json"},body:ye}).then(Se=>{if(!Se.ok)throw new Error(`HTTP ${Se.status}`);h("Payload uploaded successfully","success")}).catch(Se=>{f.appendMessage(s,`ERROR: upload failed — ${Se.message}`),h(`Upload failed: ${Se.message}`,"error")})})},z=[y,d,s,f,h],n[26]=h,n[27]=y,n[28]=f,n[29]=d,n[30]=s,n[31]=z,n[32]=$):(z=n[31],$=n[32]),w.useEffect($,z);let ie,se;n[33]!==h||n[34]!==y||n[35]!==f||n[36]!==x||n[37]!==d||n[38]!==s?(ie=()=>{if(y||!L.current||x.length===0)return;const F=x[x.length-1].raw,ye=Sf(F);if(!ye)return;if(L.current=!1,d.length===0){f.appendMessage(s,"ERROR: please paste JSON/XML payload in the input text area");return}let Se;try{Se=JSON.stringify(JSON.parse(d))}catch{f.appendMessage(s,"ERROR: payload is not valid JSON — cannot upload");return}fetch(ye,{method:"POST",headers:{"Content-Type":"application/json"},body:Se}).then(he=>{if(!he.ok)throw new Error(`HTTP ${he.status}`);h("Payload uploaded successfully","success")}).catch(he=>{f.appendMessage(s,`ERROR: upload failed — ${he.message}`),h(`Upload failed: ${he.message}`,"error")})},se=[y,x,d,s,f,h],n[33]=h,n[34]=y,n[35]=f,n[36]=x,n[37]=d,n[38]=s,n[39]=ie,n[40]=se):(ie=n[39],se=n[40]),w.useEffect(ie,se);let pe;n[41]!==h||n[42]!==f||n[43]!==d||n[44]!==E||n[45]!==s?(pe=()=>{if(E==="connected"){if(d.length===0){h("Nothing to upload — paste a JSON payload first","error");return}L.current=!0,f.send(s,"upload")}},n[41]=h,n[42]=f,n[43]=d,n[44]=E,n[45]=s,n[46]=pe):pe=n[46];const ae=pe;let te;n[47]!==f||n[48]!==E||n[49]!==s?(te=F=>E!=="connected"?!1:f.send(s,F),n[47]=f,n[48]=E,n[49]=s,n[50]=te):te=n[50];const ue=te;let re;n[51]!==h||n[52]!==x?(re=()=>{navigator.clipboard.writeText(x.map(i1).join(`
`)),h("Console copied to clipboard!","success")},n[51]=h,n[52]=x,n[53]=re):re=n[53];const me=re;let Ce;n[54]!==h||n[55]!==f||n[56]!==s?(Ce=()=>{f.clearMessages(s),h("Console cleared","info")},n[54]=h,n[55]=f,n[56]=s,n[57]=Ce):Ce=n[57];const Oe=Ce;let _e;n[58]!==f||n[59]!==s?(_e=F=>{f.appendMessage(s,F)},n[58]=f,n[59]=s,n[60]=_e):_e=n[60];const Re=_e;let ne;n[61]===Symbol.for("react.memo_cache_sentinel")?(ne=F=>_({type:"SET_COMMAND",value:F}),n[61]=ne):ne=n[61];const ge=ne;let xe;return n[62]!==Re||n[63]!==Oe||n[64]!==v||n[65]!==I||n[66]!==S||n[67]!==j||n[68]!==me||n[69]!==Z||n[70]!==R||n[71]!==k||n[72]!==x||n[73]!==P||n[74]!==ue||n[75]!==ae?(xe={connected:S,connecting:j,messages:x,command:v,setCommand:ge,connect:I,disconnect:Z,sendCommand:P,handleKeyDown:R,consoleRef:D,copyMessages:me,clearMessages:Oe,uploadPayload:ae,sendRawText:ue,appendMessage:Re,history:k},n[62]=Re,n[63]=Oe,n[64]=v,n[65]=I,n[66]=S,n[67]=j,n[68]=me,n[69]=Z,n[70]=R,n[71]=k,n[72]=x,n[73]=P,n[74]=ue,n[75]=ae,n[76]=xe):xe=n[76],xe}function i1(r){return r.raw}function s1(r){const n=Ee.c(5);let s;n[0]!==r?(s=()=>window.matchMedia(r).matches,n[0]=r,n[1]=s):s=n[1];const[c,d]=w.useState(s);let h,y;return n[2]!==r?(h=()=>{const g=window.matchMedia(r),f=b=>d(b.matches);return g.addEventListener("change",f),()=>g.removeEventListener("change",f)},y=[r],n[2]=r,n[3]=h,n[4]=y):(h=n[3],y=n[4]),w.useEffect(h,y),c}function Yh(r){return typeof r!="object"||r===null?!1:Array.isArray(r.nodes)}function dc(r,n,s){const c=n.includes(s)?s:n[0]??"graph";return typeof r=="string"&&n.includes(r)?r:c}function r1(r,n,s,c,d){const[h,y]=w.useState(null),[g,f]=Sa(d,s),b=dc(g,c,s),[E,x]=w.useState(!1),S=w.useCallback(v=>{f(T=>{const O=dc(T,c,s),k=typeof v=="function"?v(O):v;return dc(k,c,s)})},[f,c,s]);w.useEffect(()=>{g!==b&&f(b)},[g,b,f]);const j=w.useRef(r);w.useEffect(()=>{j.current=r},[r]);const A=w.useRef(null);w.useEffect(()=>{if(!r)return;const v=new AbortController;return y(null),fetch(r,{signal:v.signal}).then(T=>{if(!T.ok)throw new Error(`HTTP ${T.status}`);return T.json()}).then(T=>{Yh(T)&&(y(T),S("graph"))}).catch(T=>{T.name!=="AbortError"&&n(`Graph fetch failed: ${T.message}`,"error")}),()=>{v.abort()}},[r,n]);const _=w.useCallback(()=>{var O;const v=j.current;if(!v)return;(O=A.current)==null||O.abort();const T=new AbortController;A.current=T,x(!0),fetch(v,{signal:T.signal}).then(k=>{if(!k.ok)throw new Error(`HTTP ${k.status}`);return k.json()}).then(k=>{Yh(k)&&y(k),x(!1)}).catch(k=>{k.name!=="AbortError"&&(n(`Graph refresh failed: ${k.message}`,"error"),x(!1))})},[]);return w.useEffect(()=>()=>{var v;(v=A.current)==null||v.abort()},[]),{graphData:h,setGraphData:y,rightTab:b,setRightTab:S,isRefreshing:E,refetchGraph:_}}function c1(r){const n=Ee.c(22),{bus:s,pinnedGraphPath:c,setPinnedGraphPath:d,connected:h,sendRawText:y,addToast:g}=r,f=w.useRef(null),b=w.useRef(!1),E=w.useRef(c),x=w.useRef(h),S=w.useRef(y);let j,A;n[0]!==c?(j=()=>{E.current=c},A=[c],n[0]=c,n[1]=j,n[2]=A):(j=n[1],A=n[2]),w.useEffect(j,A);let _,v;n[3]!==h?(_=()=>{x.current=h},v=[h],n[3]=h,n[4]=_,n[5]=v):(_=n[4],v=n[5]),w.useEffect(_,v);let T,O;n[6]!==y?(T=()=>{S.current=y},O=[y],n[6]=y,n[7]=T,n[8]=O):(T=n[7],O=n[8]),w.useEffect(T,O);let k,H;n[9]!==h?(k=()=>{h||(b.current=!1,f.current!==null&&(clearTimeout(f.current),f.current=null))},H=[h],n[9]=h,n[10]=k,n[11]=H):(k=n[10],H=n[11]),w.useEffect(k,H);let D,L;n[12]!==s||n[13]!==d?(L=()=>s.on("graph.link",K=>{b.current&&(b.current=!1,d(K.apiPath))}),D=[s,d],n[12]=s,n[13]=d,n[14]=D,n[15]=L):(D=n[14],L=n[15]),w.useEffect(L,D);let C,Y;n[16]!==g||n[17]!==s?(C=()=>s.on("graph.mutation",K=>{if(x.current){if(K.mutationType==="import-graph"){f.current!==null&&(clearTimeout(f.current),f.current=null),b.current=!0,S.current("describe graph"),g("Graph imported — refreshing view…","info");return}f.current!==null&&clearTimeout(f.current),f.current=setTimeout(()=>{f.current=null,x.current&&(b.current=!0,S.current("describe graph"),g(E.current!==null?"Graph updated — refreshing…":"Graph updated — opening Graph tab…","info"))},300)}}),Y=[s,g],n[16]=g,n[17]=s,n[18]=C,n[19]=Y):(C=n[18],Y=n[19]),w.useEffect(C,Y);let J,I;n[20]===Symbol.for("react.memo_cache_sentinel")?(J=()=>()=>{f.current!==null&&clearTimeout(f.current)},I=[],n[20]=J,n[21]=I):(J=n[20],I=n[21]),w.useEffect(J,I)}const u1=`Connect two nodes together
--------------------------
1. Each connection is directional. Connect A to B is different from B to A.
2. A node must connect to one or more nodes. When a graph has orphan nodes, you cannot export the graph for deployment.

Syntax
------
\`\`\`
connect {node-A} to {node-B} with {relation}
\`\`\`
`,d1=`Create a new node
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
`,p1=`Data Dictionary
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
`,h1=`Delete a node, a connection or clear cache
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
`,f1="Describe graph, node, connection or skill\n-----------------------------------------\n\nSyntax\n------\nShow the structure of the current graph model\n```\ndescribe graph\n```\n\nPrint the structure of a node\n```\ndescribe node {name}\n```\n\nConfirm if there is a connection between node-A and node-B\n```\ndescribe connection {node-A} and {node-B}\n```\n\nSkill description of a specific composable function serving the skill\n```\ndescribe skill {skill.route.name}\n```\n",m1=`Edit a node
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
`,g1=`Execute a node with a skill
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
`,y1=`Export a graph model
--------------------
1. This command exports a graph as a model in JSON format for deployment
2. The name does not require the ".json" extension

Syntax
------
\`\`\`
export graph as {name}
\`\`\`
`,b1=`Skill: Graph API Fetcher
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
`,v1=`Skill: Graph Data Mapper
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
`,_1=`Skill: Graph Extension
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
`,x1=`Skill: Graph Island
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
`,S1=`Skill: Graph Join
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
`,w1=`Skill: Graph JS
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
`,T1=`Skill: Graph Math
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
`,E1=`Import a graph model
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
`,N1=`Inspect state machine
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
`,A1=`Instantiate from a Graph Model
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
`,C1=`List nodes or connections
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
`,j1=`Run a graph instance
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
`,M1=`Display nodes that have been 'seen'
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
`,D1=`Tutorial 1
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
`,k1=`Tutorial 10
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
`,O1=`Tutorial 11
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
`,R1=`Tutorial 10
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
`,z1=`Tutorial 2
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
`,B1=`Tutorial 3
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
`,H1=`Tutorial 4
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
`,G1=`Tutorial 5
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
`,U1=`Tutorial 6
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
`,L1=`Tutorial 7
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
`,Y1=`Tutorial 8
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
`,q1=`Tutorial 9
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
`,I1=`Update a node
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
`,X1=`Upload mock data to current graph instance
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
`,J1=`MiniGraph
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
- help tutorial 12 (custom error handling)
`,Z1=Object.assign({"../../../src/main/resources/help/help connect.md":u1,"../../../src/main/resources/help/help create.md":d1,"../../../src/main/resources/help/help data-dictionary.md":p1,"../../../src/main/resources/help/help delete.md":h1,"../../../src/main/resources/help/help describe.md":f1,"../../../src/main/resources/help/help edit.md":m1,"../../../src/main/resources/help/help execute.md":g1,"../../../src/main/resources/help/help export.md":y1,"../../../src/main/resources/help/help graph-api-fetcher.md":b1,"../../../src/main/resources/help/help graph-data-mapper.md":v1,"../../../src/main/resources/help/help graph-extension.md":_1,"../../../src/main/resources/help/help graph-island.md":x1,"../../../src/main/resources/help/help graph-join.md":S1,"../../../src/main/resources/help/help graph-js.md":w1,"../../../src/main/resources/help/help graph-math.md":T1,"../../../src/main/resources/help/help import.md":E1,"../../../src/main/resources/help/help inspect.md":N1,"../../../src/main/resources/help/help instantiate.md":A1,"../../../src/main/resources/help/help list.md":C1,"../../../src/main/resources/help/help run.md":j1,"../../../src/main/resources/help/help seen.md":M1,"../../../src/main/resources/help/help tutorial 1.md":D1,"../../../src/main/resources/help/help tutorial 10.md":k1,"../../../src/main/resources/help/help tutorial 11.md":O1,"../../../src/main/resources/help/help tutorial 12.md":R1,"../../../src/main/resources/help/help tutorial 2.md":z1,"../../../src/main/resources/help/help tutorial 3.md":B1,"../../../src/main/resources/help/help tutorial 4.md":H1,"../../../src/main/resources/help/help tutorial 5.md":G1,"../../../src/main/resources/help/help tutorial 6.md":U1,"../../../src/main/resources/help/help tutorial 7.md":L1,"../../../src/main/resources/help/help tutorial 8.md":Y1,"../../../src/main/resources/help/help tutorial 9.md":q1,"../../../src/main/resources/help/help update.md":I1,"../../../src/main/resources/help/help upload.md":X1,"../../../src/main/resources/help/help.md":J1});function V1(r){const n=r.split("/");return(n[n.length-1]??r).replace(/\.md$/,"")}const wf=Object.fromEntries(Object.entries(Z1).map(([r,n])=>[V1(r),n]));function Ji(r){const n=r===""?"help":`help ${r}`;return wf[n]??null}const Q1=Object.keys(wf).filter(r=>r!=="help").map(r=>r.replace(/^help\s+/,"")).sort(),xc=[{id:"overview",label:"Overview"},{id:"graph-model",label:"Graph Model"},{id:"graph-skills",label:"Graph Skills"},{id:"instance-model",label:"Instance Model"},{id:"tutorials",label:"Tutorials",chipStripLabel:"Chapters"}],K1=new Set(["execute","inspect","instantiate","run","seen","upload"]);function Tf(r){return r===""?"overview":r.startsWith("tutorial ")?"tutorials":r.startsWith("graph-")?"graph-skills":K1.has(r)?"instance-model":"graph-model"}function Sc(r){if(r==="overview")return[""];const n=Q1.filter(s=>Tf(s)===r);return r==="tutorials"?[...n].sort((s,c)=>{const d=parseInt(s.replace(/^tutorial\s+/,""),10),h=parseInt(c.replace(/^tutorial\s+/,""),10);return d-h}):n}function $1(r,n){return r===""?"Overview":n==="tutorials"?r.replace(/^tutorial\s+/,""):r}const Ao=xc.flatMap(r=>Sc(r.id));function Ef(r){return r.replace(/^help\s*/i,"").trim().toLowerCase()}function W1(r){const n=Ee.c(6),{bus:s,setHelpTopic:c,onTabSwitch:d}=r,h=w.useRef(d);let y;n[0]!==d?(y=()=>{h.current=d},n[0]=d,n[1]=y):y=n[1],w.useEffect(y);let g,f;n[2]!==s||n[3]!==c?(g=()=>s.on("command.helpOrDescribe",b=>{if(!b.commandText.trim().toLowerCase().startsWith("help"))return;const x=Ef(b.commandText);Ji(x)!==null&&(c(x),h.current())}),f=[s,c],n[2]=s,n[3]=c,n[4]=g,n[5]=f):(g=n[4],f=n[5]),w.useEffect(g,f)}function P1(r){const n=Ee.c(12),{ctx:s,navigate:c,addToast:d,wsPath:h}=r;let y;n[0]===Symbol.for("react.memo_cache_sentinel")?(y=Qn.find(F1),n[0]=y):y=n[0];const g=y,f=w.useRef(null),b=g==null?void 0:g.wsPath;let E,x;n[1]!==d||n[2]!==s||n[3]!==c?(E=()=>{if(!b||!f.current)return;if(s.getSlot(b).phase==="connected"){const{wsPath:T,json:O}=f.current;f.current=null,s.setPendingPayload(T,O),c(g.path),d("JSON loaded into JSON-Path editor ✓","success")}},x=[b,s,c,d,g],n[1]=d,n[2]=s,n[3]=c,n[4]=E,n[5]=x):(E=n[4],x=n[5]),w.useEffect(E,x);let S;n[6]!==d||n[7]!==s||n[8]!==c?(S=v=>{if(!g)return;const T=s.getSlot(g.wsPath);T.phase==="connected"?(s.setPendingPayload(g.wsPath,v),c(g.path),d("JSON loaded into JSON-Path editor ✓","success")):T.phase==="connecting"?(f.current={wsPath:g.wsPath,json:v},d("Updated pending JSON transfer — latest payload will open when connected","info")):(f.current={wsPath:g.wsPath,json:v},s.connect(g.wsPath,d),d("Connecting to JSON-Path Playground…","info"))},n[6]=d,n[7]=s,n[8]=c,n[9]=S):S=n[9];const j=S,A=g&&h!==g.wsPath?j:void 0;let _;return n[10]!==A?(_={handleSendToJsonPath:A},n[10]=A,n[11]=_):_=n[11],_}function F1(r){return r.tabs.includes("payload")&&r.supportsUpload}function eb(r){const n=Ee.c(7),{bus:s,onOpenModal:c,modalOpen:d}=r,h=w.useRef(!1);let y,g;n[0]!==d?(y=()=>{d||(h.current=!1)},g=[d],n[0]=d,n[1]=y,n[2]=g):(y=n[1],g=n[2]),w.useEffect(y,g);let f,b;n[3]!==s||n[4]!==c?(f=()=>s.on("upload.invitation",E=>{h.current||(h.current=!0,c(E.uploadPath))}),b=[s,c],n[3]=s,n[4]=c,n[5]=f,n[6]=b):(f=n[5],b=n[6]),w.useEffect(f,b)}function tb(r){const n=Ee.c(17),{bus:s,addToast:c}=r,[d,h]=w.useState(null),y=w.useRef(null);let g;n[0]===Symbol.for("react.memo_cache_sentinel")?(g=new Set,n[0]=g):g=n[0];const[f,b]=w.useState(g);let E;n[1]===Symbol.for("react.memo_cache_sentinel")?(E=C=>{y.current=document.activeElement,h(C)},n[1]=E):E=n[1];const x=E;let S;n[2]===Symbol.for("react.memo_cache_sentinel")?(S=()=>{h(null),setTimeout(()=>{var C;return(C=y.current)==null?void 0:C.focus()},0)},n[2]=S):S=n[2];const j=S;let A;n[3]!==c||n[4]!==d?(A=C=>{b(Y=>new Set([...Y,d])),h(null),setTimeout(()=>{var Y;return(Y=y.current)==null?void 0:Y.focus()},0),c("Mock data uploaded successfully ✓","success")},n[3]=c,n[4]=d,n[5]=A):A=n[5];const _=A;let v;n[6]!==c?(v=C=>{c(`Upload failed: ${C}`,"error")},n[6]=c,n[7]=v):v=n[7];const T=v;let O;n[8]===Symbol.for("react.memo_cache_sentinel")?(O=()=>{b(new Set)},n[8]=O):O=n[8];const k=O,H=d!==null;let D;n[9]!==s||n[10]!==H?(D={bus:s,onOpenModal:x,modalOpen:H},n[9]=s,n[10]=H,n[11]=D):D=n[11],eb(D);let L;return n[12]!==T||n[13]!==_||n[14]!==d||n[15]!==f?(L={modalUploadPath:d,successfulUploadPaths:f,handleOpenUploadModal:x,handleCloseUploadModal:j,handleUploadSuccess:_,handleUploadError:T,resetSuccessfulPaths:k},n[12]=T,n[13]=_,n[14]=d,n[15]=f,n[16]=L):L=n[16],L}function nb(r){const n=Ee.c(14),{bus:s,connected:c,appendMessage:d,addToast:h}=r,y=w.useRef(null),g=w.useRef(!1),f=w.useRef(d);let b,E;n[0]!==d?(b=()=>{f.current=d},E=[d],n[0]=d,n[1]=b,n[2]=E):(b=n[1],E=n[2]),w.useEffect(b,E);const x=w.useRef(h);let S,j;n[3]!==h?(S=()=>{x.current=h},j=[h],n[3]=h,n[4]=S,n[5]=j):(S=n[4],j=n[5]),w.useEffect(S,j);let A,_;n[6]!==c?(A=()=>{var H;c||((H=y.current)==null||H.abort(),y.current=null,g.current=!1)},_=[c],n[6]=c,n[7]=A,n[8]=_):(A=n[7],_=n[8]),w.useEffect(A,_);let v,T;n[9]===Symbol.for("react.memo_cache_sentinel")?(v=()=>()=>{var H;(H=y.current)==null||H.abort()},T=[],n[9]=v,n[10]=T):(v=n[9],T=n[10]),w.useEffect(v,T);let O,k;n[11]!==s?(k=()=>s.on("payload.large",H=>{var J;if(g.current)return;const{apiPath:D,byteSize:L}=H;(J=y.current)==null||J.abort();const C=new AbortController;y.current=C;const Y=(L/1048576).toFixed(2);x.current(`Fetching large payload (${Y} MB)…`,"info"),g.current=!0,fetch(D,{signal:C.signal}).then(ab).then(I=>{if(!I.trim())throw new Error("empty response body");let K=I;try{K=JSON.stringify(JSON.parse(I),null,2)}catch{}f.current(K),g.current=!1,y.current=null}).catch(I=>{I.name!=="AbortError"&&(g.current=!1,y.current=null,f.current(`ERROR: payload fetch failed — ${I.message}`),x.current(`Payload fetch failed: ${I.message}`,"error"))})}),O=[s],n[11]=s,n[12]=O,n[13]=k):(O=n[12],k=n[13]),w.useEffect(k,O)}function ab(r){if(!r.ok)throw new Error(`HTTP ${r.status}`);return r.text()}function ob(r){const n=Ee.c(14);let s;n[0]===Symbol.for("react.memo_cache_sentinel")?(s={},n[0]=s):s=n[0];const[c,d]=Sa(r,s);let h;n[1]!==d?(h=A=>{d(_=>({..._,[A]:{name:A,savedAt:new Date().toISOString()}}))},n[1]=d,n[2]=h):h=n[2];const y=h;let g;n[3]!==d?(g=A=>{d(_=>{const v={..._};return delete v[A],v})},n[3]=d,n[4]=g):g=n[4];const f=g;let b;n[5]!==c?(b=A=>Object.prototype.hasOwnProperty.call(c,A),n[5]=c,n[6]=b):b=n[6];const E=b;let x;n[7]!==c?(x=Object.values(c).sort(lb),n[7]=c,n[8]=x):x=n[8];const S=x;let j;return n[9]!==f||n[10]!==E||n[11]!==y||n[12]!==S?(j={savedGraphs:S,saveGraph:y,deleteGraph:f,hasGraph:E},n[9]=f,n[10]=E,n[11]=y,n[12]=S,n[13]=j):j=n[13],j}function lb(r,n){return new Date(n.savedAt).getTime()-new Date(r.savedAt).getTime()}function ib(r,n){const s=Ee.c(11),[c,d]=Sa(r,1),h=w.useRef(!1),[y,g]=w.useState(null),[f,b]=w.useState(null);let E,x;s[0]!==n?(E=()=>n.on("command.importGraph",O=>{g(O.graphName),b(null)}),x=[n],s[0]=n,s[1]=E,s[2]=x):(E=s[1],x=s[2]),w.useEffect(E,x);let S;s[3]!==c?(S=O=>{b(O),O===`untitled-${c}`&&(h.current=!0)},s[3]=c,s[4]=S):S=s[4];const j=S;let A;s[5]!==d?(A=()=>{g(null),b(null),h.current&&d(sb),h.current=!1},s[5]=d,s[6]=A):A=s[6];const _=A,v=f??y??`untitled-${c}`;let T;return s[7]!==v||s[8]!==_||s[9]!==j?(T={defaultName:v,setLastSavedName:j,resetName:_},s[7]=v,s[8]=_,s[9]=j,s[10]=T):T=s[10],T}function sb(r){return r+1}function rb(r){const n=Ee.c(27),{bus:s,connected:c,sendRawText:d,saveGraph:h,setLastSavedName:y,addToast:g}=r,f=w.useRef(null);let b;n[0]!==g||n[1]!==c||n[2]!==d?(b=L=>{if(!c){g("Save failed: connection required to export graph","error");return}const C=setTimeout(()=>{f.current!==null&&(f.current=null,g("Save failed: export confirmation timed out","error"))},1e4);f.current={graphName:L,timeoutId:C},d(`export graph as ${L}`)},n[0]=g,n[1]=c,n[2]=d,n[3]=b):b=n[3];const E=b;let x,S;n[4]!==g||n[5]!==s||n[6]!==h||n[7]!==y?(x=()=>s.on("graph.exported",L=>{if(f.current===null||L.graphName!==f.current.graphName)return;clearTimeout(f.current.timeoutId);const C=f.current.graphName;f.current=null,h(C),y(C),g(`Graph saved as "${C}"`,"success")}),S=[s,h,y,g],n[4]=g,n[5]=s,n[6]=h,n[7]=y,n[8]=x,n[9]=S):(x=n[8],S=n[9]),w.useEffect(x,S);let j,A;n[10]!==g||n[11]!==s?(j=()=>s.on("graph.export.failed",L=>{f.current!==null&&(clearTimeout(f.current.timeoutId),f.current=null,L.reason==="invalid-name"?g("Save failed: invalid filename (a–z, A–Z, 0–9, hyphen only)","error"):g("Save failed: root node name does not match existing graph","error"))}),A=[s,g],n[10]=g,n[11]=s,n[12]=j,n[13]=A):(j=n[12],A=n[13]),w.useEffect(j,A);let _,v;n[14]!==g||n[15]!==c?(_=()=>{!c&&f.current!==null&&(clearTimeout(f.current.timeoutId),f.current=null,g("Save failed: connection closed before export confirmation","error"))},v=[c,g],n[14]=g,n[15]=c,n[16]=_,n[17]=v):(_=n[16],v=n[17]),w.useEffect(_,v);let T,O;n[18]===Symbol.for("react.memo_cache_sentinel")?(T=()=>()=>{f.current!==null&&clearTimeout(f.current.timeoutId)},O=[],n[18]=T,n[19]=O):(T=n[18],O=n[19]),w.useEffect(T,O);let k;n[20]!==g||n[21]!==c||n[22]!==d?(k=L=>{c&&(d(`import graph from ${L}`),g(`Importing graph "${L}"…`,"info"))},n[20]=g,n[21]=c,n[22]=d,n[23]=k):k=n[23];const H=k;let D;return n[24]!==H||n[25]!==E?(D={handleSaveGraph:E,handleLoadGraph:H},n[24]=H,n[25]=E,n[26]=D):D=n[26],D}const pc=new Map;function cb(r){const n=Ee.c(7);let s;n[0]!==r?(s=()=>pc.get(r)??null,n[0]=r,n[1]=s):s=n[1];const[c,d]=w.useState(s);let h;n[2]!==r?(h=f=>{d(f),f===null?pc.delete(r):pc.set(r,f)},n[2]=r,n[3]=h):h=n[3];const y=h;let g;return n[4]!==c||n[5]!==y?(g=[c,y],n[4]=c,n[5]=y,n[6]=g):g=n[6],g}function qh(r){if(r==null)return"";const n=typeof r=="string"?r:JSON.stringify(r);return n.includes("'''")&&console.warn(`[commandBuilder] Property value contains "'''" which cannot be escaped in the backend grammar. The value may be truncated on paste.`),n.includes(`
`)?`'''
${n}
'''`:n}function ub(r,n){const s=[`${r} node ${n.alias}`];n.types.length>0&&s.push(`with type ${n.types[0]}`);const c=Object.entries(n.properties).filter(([,d])=>d!=null);if(c.length>0){s.push("with properties");for(const[d,h]of c)if(Array.isArray(h))for(const y of h)s.push(`${d}[]=${qh(y)}`);else s.push(`${d}[]=${qh(h)}`)}return s.join(`
`)}function Ih(r,n){const s=n!=null&&n.nodes.some(c=>c.alias===r.node.alias)?"update":"create";return{verb:s,command:ub(s,r.node)}}function db(r){return{execute(n){return r(n)}}}const pb="_toastContainer_hhy5k_1",hb="_toast_hhy5k_1",fb="_slideIn_hhy5k_1",mb="_success_hhy5k_36",gb="_error_hhy5k_40",yb="_info_hhy5k_44",bb="_toastIcon_hhy5k_48",vb="_toastMessage_hhy5k_53",wl={toastContainer:pb,toast:hb,slideIn:fb,success:mb,error:gb,info:yb,toastIcon:bb,toastMessage:vb},_b=r=>{const n=Ee.c(7),{toasts:s,onRemove:c}=r;if(s.length===0)return null;let d;if(n[0]!==c||n[1]!==s){let y;n[3]!==c?(y=g=>p.jsxs("div",{className:`${wl.toast} ${wl[g.type]}`,onClick:()=>c(g.id),children:[p.jsxs("span",{className:wl.toastIcon,children:[g.type==="success"&&"✅",g.type==="error"&&"❌",g.type==="info"&&"ℹ️"]}),p.jsx("span",{className:wl.toastMessage,children:g.message})]},g.id),n[3]=c,n[4]=y):y=n[4],d=s.map(y),n[0]=c,n[1]=s,n[2]=d}else d=n[2];let h;return n[5]!==d?(h=p.jsx("div",{className:wl.toastContainer,children:d}),n[5]=d,n[6]=h):h=n[6],h},xb="_container_9dbh2_3",Sb="_trigger_9dbh2_7",wb="_chevron_9dbh2_37",Tb="_chevronOpen_9dbh2_43",Eb="_dot_9dbh2_49",Nb="_dotIdle_9dbh2_56",Ab="_dotConnecting_9dbh2_57",Cb="_dotConnected_9dbh2_58",jb="_dotPartial_9dbh2_59",Mb="_dropdown_9dbh2_65",An={container:xb,trigger:Sb,chevron:wb,chevronOpen:Tb,dot:Eb,dotIdle:Nb,dotConnecting:Ab,dotConnected:Cb,dotPartial:jb,dropdown:Mb};function wc(r){const n=Ee.c(23),{label:s,dotStatus:c,children:d}=r,[h,y]=w.useState(!1),g=w.useRef(null);let f,b;n[0]!==h?(f=()=>{if(!h)return;const D=L=>{g.current&&!g.current.contains(L.target)&&y(!1)};return document.addEventListener("mousedown",D),()=>document.removeEventListener("mousedown",D)},b=[h],n[0]=h,n[1]=f,n[2]=b):(f=n[1],b=n[2]),w.useEffect(f,b);let E;n[3]===Symbol.for("react.memo_cache_sentinel")?(E=D=>{var L,C;D.key==="Escape"&&(y(!1),(C=(L=g.current)==null?void 0:L.querySelector("button[aria-haspopup]"))==null||C.focus())},n[3]=E):E=n[3];const x=E,S=c==="connected"?An.dotConnected:c==="connecting"?An.dotConnecting:c==="partial"?An.dotPartial:c==="idle"?An.dotIdle:void 0;let j;n[4]===Symbol.for("react.memo_cache_sentinel")?(j=()=>y(Db),n[4]=j):j=n[4];let A;n[5]!==S||n[6]!==c?(A=c!==void 0&&p.jsx("span",{className:`${An.dot} ${S??""}`,"aria-hidden":"true"}),n[5]=S,n[6]=c,n[7]=A):A=n[7];let _;n[8]!==s?(_=p.jsx("span",{children:s}),n[8]=s,n[9]=_):_=n[9];const v=`${An.chevron} ${h?An.chevronOpen:""}`;let T;n[10]!==v?(T=p.jsx("span",{className:v,"aria-hidden":"true",children:"▾"}),n[10]=v,n[11]=T):T=n[11];let O;n[12]!==h||n[13]!==A||n[14]!==_||n[15]!==T?(O=p.jsxs("button",{className:An.trigger,onClick:j,"aria-haspopup":"true","aria-expanded":h,children:[A,_,T]}),n[12]=h,n[13]=A,n[14]=_,n[15]=T,n[16]=O):O=n[16];let k;n[17]!==d||n[18]!==h?(k=h&&p.jsx("div",{className:An.dropdown,role:"menu",children:d}),n[17]=d,n[18]=h,n[19]=k):k=n[19];let H;return n[20]!==k||n[21]!==O?(H=p.jsxs("div",{className:An.container,ref:g,onKeyDown:x,children:[O,k]}),n[20]=k,n[21]=O,n[22]=H):H=n[22],H}function Db(r){return!r}const kb="_nav_1hfby_3",Ob="_menuList_1hfby_11",Rb="_menuItem_1hfby_19",zb="_toolRow_1hfby_56",Bb="_toolLink_1hfby_67",Hb="_toolLinkActive_1hfby_92",Gb="_toolDot_1hfby_99",Ub="_toolDotIdle_1hfby_106",Lb="_toolDotConnecting_1hfby_107",Yb="_toolDotConnected_1hfby_108",qb="_connectAllRow_1hfby_112",Ib="_connectAllBtn_1hfby_118",Xb="_connectAllBtnStop_1hfby_142",Jb="_toolConnectBtn_1hfby_154",Zb="_toolConnectBtnStop_1hfby_180",Vb="_externalIcon_1hfby_192",jt={nav:kb,menuList:Ob,menuItem:Rb,toolRow:zb,toolLink:Bb,toolLinkActive:Hb,toolDot:Gb,toolDotIdle:Ub,toolDotConnecting:Lb,toolDotConnected:Yb,connectAllRow:qb,connectAllBtn:Ib,connectAllBtnStop:Xb,toolConnectBtn:Jb,toolConnectBtnStop:Zb,externalIcon:Vb};function Qb(r){return r.every(n=>n==="connected")?"connected":r.every(n=>n==="idle")?"idle":r.some(n=>n==="connecting")?"connecting":"partial"}function Kb(r){return r==="connected"?"connected":r==="connecting"?"connecting":"idle"}const $b=[{href:"/info",label:"Info"},{href:"/info/lib",label:"Libraries"},{href:"/info/routes",label:"Services"},{href:"/health",label:"Health"},{href:"/env",label:"Environment"},{href:"http://localhost:8085/api/ws/json",label:"Legacy JSON"},{href:"http://localhost:8085/api/ws/graph",label:"Legacy Graph"}];function Wb(r){const n=Ee.c(27),{addToast:s}=r,c=zc();let d,h,y;if(n[0]!==c){const L=Qn.map(C=>c.getSlot(C.wsPath).phase);y=Qb(L),d=L.every(tv),h=L.some(ev),n[0]=c,n[1]=d,n[2]=h,n[3]=y}else d=n[1],h=n[2],y=n[3];const g=h;let f;n[4]!==s||n[5]!==c?(f=function(){Qn.forEach(C=>{c.getSlot(C.wsPath).phase==="idle"&&c.connect(C.wsPath,s)})},n[4]=s,n[5]=c,n[6]=f):f=n[6];const b=f;let E;n[7]!==c?(E=function(){Qn.forEach(C=>{const{phase:Y}=c.getSlot(C.wsPath);(Y==="connected"||Y==="connecting")&&c.disconnect(C.wsPath)})},n[7]=c,n[8]=E):E=n[8];const x=E,S=`${jt.connectAllBtn} ${d?jt.connectAllBtnStop:""}`,j=d?x:b,A=g?"Connecting…":d?"Disconnect all WebSockets":"Connect all WebSockets",_=g?"Connecting…":d?"Disconnect All":"Connect All";let v;n[9]!==g||n[10]!==S||n[11]!==j||n[12]!==A||n[13]!==_?(v=p.jsx("div",{className:jt.connectAllRow,children:p.jsx("button",{className:S,onClick:j,disabled:g,"aria-label":A,children:_})}),n[9]=g,n[10]=S,n[11]=j,n[12]=A,n[13]=_,n[14]=v):v=n[14];let T;n[15]!==s||n[16]!==c?(T=Qn.map(L=>{const{phase:C}=c.getSlot(L.wsPath),Y=Kb(C),J=C==="connected",I=C==="connecting",K=Y==="connected"?jt.toolDotConnected:Y==="connecting"?jt.toolDotConnecting:jt.toolDotIdle;return p.jsxs("li",{role:"none",className:jt.toolRow,children:[p.jsxs($g,{to:L.path,role:"menuitem",className:Fb,children:[p.jsx("span",{className:`${jt.toolDot} ${K}`,"aria-hidden":"true"}),p.jsx("span",{className:jt.toolLabel,children:L.label})]}),p.jsx("button",{className:`${jt.toolConnectBtn} ${J?jt.toolConnectBtnStop:""}`,onClick:()=>J||I?c.disconnect(L.wsPath):c.connect(L.wsPath,s),disabled:I,"aria-label":I?"Connecting…":J?`Disconnect ${L.label}`:`Connect ${L.label}`,title:I?"Connecting…":vf(L.wsPath),children:I?"…":J?"Stop":"Start"})]},L.path)}),n[15]=s,n[16]=c,n[17]=T):T=n[17];let O;n[18]!==T?(O=p.jsx("ul",{className:jt.menuList,role:"none",children:T}),n[18]=T,n[19]=O):O=n[19];let k;n[20]!==O||n[21]!==v||n[22]!==y?(k=p.jsxs(wc,{label:"Tools",dotStatus:y,children:[v,O]}),n[20]=O,n[21]=v,n[22]=y,n[23]=k):k=n[23];let H;n[24]===Symbol.for("react.memo_cache_sentinel")?(H=p.jsx(wc,{label:"Quick Links",children:p.jsx("ul",{className:jt.menuList,role:"none",children:$b.map(Pb)})}),n[24]=H):H=n[24];let D;return n[25]!==k?(D=p.jsxs("nav",{className:jt.nav,"aria-label":"Main navigation",children:[k,H]}),n[25]=k,n[26]=D):D=n[26],D}function Pb(r){return p.jsx("li",{role:"none",children:p.jsxs("a",{href:r.href,role:"menuitem",className:jt.menuItem,target:"_blank",rel:"noopener noreferrer",children:[r.label,p.jsx("span",{className:jt.externalIcon,"aria-hidden":"true",children:"↗"})]})},r.href)}function Fb(r){const{isActive:n}=r;return`${jt.toolLink} ${n?jt.toolLinkActive:""}`}function ev(r){return r==="connecting"}function tv(r){return r==="connected"}const nv="_saveBtn_1xd2l_3",av="_saveForm_1xd2l_33",ov="_saveInput_1xd2l_39",lv="_saveInputWarn_1xd2l_55",iv="_saveWarnLabel_1xd2l_59",sv="_saveActionBtn_1xd2l_65",Ya={saveBtn:nv,saveForm:av,saveInput:ov,saveInputWarn:lv,saveWarnLabel:iv,saveActionBtn:sv};function rv(r){const n=Ee.c(33),{disabled:s,defaultName:c,onSave:d,nameExists:h,connected:y}=r,g=y===void 0?!1:y,[f,b]=w.useState(!1),[E,x]=w.useState(""),S=w.useRef(null);let j;n[0]!==c?(j=()=>{x(c),b(!0)},n[0]=c,n[1]=j):j=n[1];const A=j;let _;n[2]===Symbol.for("react.memo_cache_sentinel")?(_=()=>{b(!1),x("")},n[2]=_):_=n[2];const v=_;let T;n[3]!==d||n[4]!==E?(T=()=>{const I=E.trim();I&&(d(I),b(!1),x(""))},n[3]=d,n[4]=E,n[5]=T):T=n[5];const O=T;let k;n[6]!==O?(k=I=>{I.key==="Enter"&&(I.preventDefault(),O()),I.key==="Escape"&&(I.preventDefault(),v())},n[6]=O,n[7]=k):k=n[7];const H=k;let D,L;if(n[8]!==f?(D=()=>{var I;f&&((I=S.current)==null||I.focus())},L=[f],n[8]=f,n[9]=D,n[10]=L):(D=n[9],L=n[10]),w.useEffect(D,L),f){const I=`${Ya.saveInput}${h!=null&&h(E.trim())?` ${Ya.saveInputWarn}`:""}`;let K;n[11]===Symbol.for("react.memo_cache_sentinel")?(K=ie=>x(ie.target.value),n[11]=K):K=n[11];let Z;n[12]!==H||n[13]!==E||n[14]!==I?(Z=p.jsx("input",{ref:S,className:I,type:"text",value:E,onChange:K,onKeyDown:H,placeholder:"Enter a name…","aria-label":"Graph save name",maxLength:80}),n[12]=H,n[13]=E,n[14]=I,n[15]=Z):Z=n[15];let ee;n[16]!==h||n[17]!==E?(ee=(h==null?void 0:h(E.trim()))&&p.jsx("span",{className:Ya.saveWarnLabel,role:"status",children:"Overwrite?"}),n[16]=h,n[17]=E,n[18]=ee):ee=n[18];let P;n[19]!==E?(P=E.trim(),n[19]=E,n[20]=P):P=n[20];const le=!P;let R;n[21]!==O||n[22]!==le?(R=p.jsx("button",{className:Ya.saveActionBtn,onClick:O,disabled:le,"aria-label":"Confirm save",children:"✅"}),n[21]=O,n[22]=le,n[23]=R):R=n[23];let z;n[24]===Symbol.for("react.memo_cache_sentinel")?(z=p.jsx("button",{className:Ya.saveActionBtn,onClick:v,"aria-label":"Cancel save",children:"❌"}),n[24]=z):z=n[24];let $;return n[25]!==Z||n[26]!==ee||n[27]!==R?($=p.jsxs("div",{className:Ya.saveForm,children:[Z,ee,R,z]}),n[25]=Z,n[26]=ee,n[27]=R,n[28]=$):$=n[28],$}const C=s||!g,Y=s?"No graph loaded":g?"Export graph snapshot to server and save bookmark":"Connect first to save";let J;return n[29]!==A||n[30]!==C||n[31]!==Y?(J=p.jsx("button",{className:Ya.saveBtn,onClick:A,disabled:C,title:Y,"aria-label":"Save graph snapshot",children:"💾 Save Graph"}),n[29]=A,n[30]=C,n[31]=Y,n[32]=J):J=n[32],J}const cv="_empty_tpeii_3",uv="_hint_tpeii_12",dv="_list_tpeii_21",pv="_row_tpeii_31",hv="_rowInfo_tpeii_50",fv="_rowName_tpeii_58",mv="_rowMeta_tpeii_67",gv="_rowActions_tpeii_78",yv="_loadBtn_tpeii_84",bv="_deleteBtn_tpeii_85",Cn={empty:cv,hint:uv,list:dv,row:pv,rowInfo:hv,rowName:fv,rowMeta:mv,rowActions:gv,loadBtn:yv,deleteBtn:bv};function vv(r){const n=Ee.c(8),{savedGraphs:s,onLoad:c,onDelete:d,connected:h}=r,y=s.length>0?`Load Graph (${s.length})`:"Load Graph";let g;n[0]!==h||n[1]!==d||n[2]!==c||n[3]!==s?(g=s.length===0?p.jsx("p",{className:Cn.empty,children:"No saved graphs yet."}):p.jsxs(p.Fragment,{children:[!h&&p.jsx("p",{className:Cn.hint,children:"Connect to load a graph"}),p.jsx("ul",{className:Cn.list,role:"list",children:s.map(b=>p.jsxs("li",{className:Cn.row,children:[p.jsxs("div",{className:Cn.rowInfo,children:[p.jsx("span",{className:Cn.rowName,title:b.name,children:b.name}),p.jsx("span",{className:Cn.rowMeta,children:new Date(b.savedAt).toLocaleString()})]}),p.jsxs("div",{className:Cn.rowActions,children:[p.jsx("button",{className:Cn.loadBtn,onClick:()=>c(b.name),disabled:!h,title:h?`Run: import graph from ${b.name}`:"Connect to the playground first","aria-label":`Load graph ${b.name}`,children:"Load"}),p.jsx("button",{className:Cn.deleteBtn,onClick:()=>d(b.name),title:`Remove "${b.name}" from local storage`,"aria-label":`Delete saved graph ${b.name}`,children:"Delete"})]})]},b.name))})]}),n[0]=h,n[1]=d,n[2]=c,n[3]=s,n[4]=g):g=n[4];let f;return n[5]!==y||n[6]!==g?(f=p.jsx(wc,{label:y,children:g}),n[5]=y,n[6]=g,n[7]=f):f=n[7],f}const _v="_payloadRoot_6u47x_2",xv="_labelRow_6u47x_10",Sv="_label_6u47x_10",wv="_payloadControls_6u47x_26",Tv="_charCounter_6u47x_32",Ev="_typeIndicator_6u47x_38",Nv="_validationIcon_6u47x_49",Av="_formatButton_6u47x_53",Cv="_uploadButton_6u47x_67",jv="_textarea_6u47x_82",Mv="_textareaError_6u47x_107",Dv="_errorMessage_6u47x_109",kv="_sampleButtonsRow_6u47x_117",Ov="_sampleButtons_6u47x_117",Rv="_sampleLabel_6u47x_130",zv="_sampleGroup_6u47x_136",Bv="_sampleGroupLabel_6u47x_143",Hv="_sampleButton_6u47x_117",yt={payloadRoot:_v,labelRow:xv,label:Sv,payloadControls:wv,charCounter:Tv,typeIndicator:Ev,validationIcon:Nv,formatButton:Av,uploadButton:Cv,textarea:jv,textareaError:Mv,errorMessage:Dv,sampleButtonsRow:kv,sampleButtons:Ov,sampleLabel:Rv,sampleGroup:zv,sampleGroupLabel:Bv,sampleButton:Hv};function Gv(r){const n=Ee.c(21),{onLoad:s}=r;let c,d,h,y,g,f;if(n[0]!==s){const x=Object.keys(Yi).filter(Yv),S=Object.keys(Yi).filter(Lv),j=Uv;y=yt.sampleButtons,n[7]===Symbol.for("react.memo_cache_sentinel")?(g=p.jsx("span",{className:yt.sampleLabel,children:"Quick load:"}),n[7]=g):g=n[7];let A;n[8]===Symbol.for("react.memo_cache_sentinel")?(A=p.jsx("span",{className:yt.sampleGroupLabel,children:"JSON:"}),n[8]=A):A=n[8];const _=x.map(v=>p.jsx("button",{className:yt.sampleButton,onClick:()=>s(Yi[v]),children:j(v)},v));n[9]!==_?(f=p.jsxs("div",{className:yt.sampleGroup,children:[A,_]}),n[9]=_,n[10]=f):f=n[10],c=yt.sampleGroup,n[11]===Symbol.for("react.memo_cache_sentinel")?(d=p.jsx("span",{className:yt.sampleGroupLabel,children:"XML:"}),n[11]=d):d=n[11],h=S.map(v=>p.jsx("button",{className:yt.sampleButton,onClick:()=>s(Yi[v]),children:j(v)},v)),n[0]=s,n[1]=c,n[2]=d,n[3]=h,n[4]=y,n[5]=g,n[6]=f}else c=n[1],d=n[2],h=n[3],y=n[4],g=n[5],f=n[6];let b;n[12]!==c||n[13]!==d||n[14]!==h?(b=p.jsxs("div",{className:c,children:[d,h]}),n[12]=c,n[13]=d,n[14]=h,n[15]=b):b=n[15];let E;return n[16]!==y||n[17]!==g||n[18]!==f||n[19]!==b?(E=p.jsxs("div",{className:y,children:[g,f,b]}),n[16]=y,n[17]=g,n[18]=f,n[19]=b,n[20]=E):E=n[20],E}function Uv(r){return r.replace(/^(json|xml)_/,"").replace(/_/g," ")}function Lv(r){return r.startsWith("xml_")}function Yv(r){return r.startsWith("json_")}function qv(r){const n=Ee.c(40),{payload:s,onChange:c,validation:d,onFormat:h,onUpload:y}=r;let g;n[0]===Symbol.for("react.memo_cache_sentinel")?(g=p.jsx("label",{htmlFor:"payload",className:yt.label,children:"JSON/XML Payload"}),n[0]=g):g=n[0];let f;n[1]!==s.length?(f=p.jsxs("span",{className:yt.charCounter,children:["size: ",s.length]}),n[1]=s.length,n[2]=f):f=n[2];let b;n[3]!==s||n[4]!==d.type?(b=s&&d.type&&p.jsx("span",{className:yt.typeIndicator,children:d.type.toUpperCase()}),n[3]=s,n[4]=d.type,n[5]=b):b=n[5];let E;n[6]!==s||n[7]!==d.valid?(E=s&&p.jsx("span",{className:yt.validationIcon,children:d.valid?"✅":"❌"}),n[6]=s,n[7]=d.valid,n[8]=E):E=n[8];const x=!s||d.type!=="json",S=d.type==="xml"?"Format only available for JSON":"Format JSON";let j;n[9]!==h||n[10]!==x||n[11]!==S?(j=p.jsx("button",{className:yt.formatButton,onClick:h,disabled:x,title:S,children:"Format"}),n[9]=h,n[10]=x,n[11]=S,n[12]=j):j=n[12];let A;n[13]!==y||n[14]!==s||n[15]!==d.type||n[16]!==d.valid?(A=y!==void 0&&p.jsx("button",{className:yt.uploadButton,onClick:y,disabled:!s||!d.valid||d.type!=="json",title:"Upload JSON payload to current session via REST",children:"Upload"}),n[13]=y,n[14]=s,n[15]=d.type,n[16]=d.valid,n[17]=A):A=n[17];let _;n[18]!==f||n[19]!==b||n[20]!==E||n[21]!==j||n[22]!==A?(_=p.jsxs("div",{className:yt.labelRow,children:[g,p.jsxs("div",{className:yt.payloadControls,children:[f,b,E,j,A]})]}),n[18]=f,n[19]=b,n[20]=E,n[21]=j,n[22]=A,n[23]=_):_=n[23];const v=`${yt.textarea} ${d.valid?"":yt.textareaError}`;let T;n[24]!==c?(T=L=>c(L.target.value),n[24]=c,n[25]=T):T=n[25];let O;n[26]!==s||n[27]!==v||n[28]!==T?(O=p.jsx("textarea",{id:"payload",className:v,placeholder:"Paste your JSON/XML payload here",value:s,onChange:T}),n[26]=s,n[27]=v,n[28]=T,n[29]=O):O=n[29];let k;n[30]!==d.error||n[31]!==d.valid?(k=!d.valid&&p.jsx("div",{className:yt.errorMessage,children:d.error}),n[30]=d.error,n[31]=d.valid,n[32]=k):k=n[32];let H;n[33]!==c?(H=p.jsx("div",{className:yt.sampleButtonsRow,children:p.jsx(Gv,{onLoad:c})}),n[33]=c,n[34]=H):H=n[34];let D;return n[35]!==O||n[36]!==k||n[37]!==H||n[38]!==_?(D=p.jsxs("div",{className:yt.payloadRoot,children:[_,O,k,H]}),n[35]=O,n[36]=k,n[37]=H,n[38]=_,n[39]=D):D=n[39],D}const Iv={Root:{icon:"🚀",label:"Root"},End:{icon:"🏁",label:"End"},Fetcher:{icon:"🌐",label:"Fetcher"},mapper:{icon:"🗺️",label:"Mapper"},Math:{icon:"🔢",label:"Math"},JavaScript:{icon:"📜",label:"JavaScript"},Provider:{icon:"🔌",label:"Provider"},Dictionary:{icon:"📖",label:"Dictionary"},Join:{icon:"🔀",label:"Join"},Extension:{icon:"🧩",label:"Extension"},Island:{icon:"🏝️",label:"Island"},Decision:{icon:"❓",label:"Decision"}},Xv={boxSizing:"border-box",borderRadius:"8px",borderWidth:"1.5px",borderStyle:"solid",background:"var(--bg-secondary, #1e1e2e)",color:"var(--text-primary, #cdd6f4)",fontSize:"0.75rem",boxShadow:"0 2px 8px rgba(0,0,0,0.45)",overflow:"visible",padding:0},Jv={Root:"#15803d",End:"#dc2626",Fetcher:"#2563eb",mapper:"#ea580c",Math:"#a16207",JavaScript:"#7e22ce",Provider:"#be185d",Dictionary:"#0e7490",Join:"#65a30d",Extension:"#4338ca",Island:"#475569",Decision:"#b45309"},Zv="#6c7086";function Vv(r){return Iv[r]??{icon:"📦",label:r}}function Nf(r){const n=Jv[r]??Zv;return{...Xv,borderColor:n,"--node-accent":n}}const Qv="_content_138ap_8",Kv="_header_138ap_22",$v="_icon_138ap_42",Wv="_alias_138ap_47",Pv="_badge_138ap_53",Fv="_body_138ap_65",e0="_row_138ap_70",t0="_label_138ap_83",n0="_value_138ap_89",a0="_edgeHandle_138ap_103",sn={content:Qv,header:Kv,icon:$v,alias:Wv,badge:Pv,body:Fv,row:e0,label:t0,value:n0,edgeHandle:a0};function Xh(r){const n=Ee.c(7),{label:s,value:c}=r;let d;n[0]!==s?(d=p.jsx("span",{className:sn.label,children:s}),n[0]=s,n[1]=d):d=n[1];let h;n[2]!==c?(h=p.jsx("span",{className:sn.value,title:c,children:c}),n[2]=c,n[3]=h):h=n[3];let y;return n[4]!==d||n[5]!==h?(y=p.jsxs("div",{className:sn.row,children:[d,h]}),n[4]=d,n[5]=h,n[6]=y):y=n[6],y}function o0(r){const n=Ee.c(3),{properties:s}=r;let c,d;if(n[0]!==s){d=Symbol.for("react.early_return_sentinel");e:{const h=Object.entries(s).filter(i0);if(h.length===0){d=null;break e}c=p.jsx(p.Fragment,{children:h.map(l0)})}n[0]=s,n[1]=c,n[2]=d}else c=n[1],d=n[2];return d!==Symbol.for("react.early_return_sentinel")?d:c}function l0(r){const[n,s]=r;if(Array.isArray(s))return s.map((d,h)=>{const y=typeof d=="string"?d:JSON.stringify(d);return p.jsx(Xh,{label:h===0?n:"",value:y},`${n}-${h}`)});const c=typeof s=="string"?s:JSON.stringify(s);return p.jsx(Xh,{label:n,value:c},n)}function i0(r){const[,n]=r;return n!=null}function Af(r){const n=Ee.c(17),{alias:s,nodeType:c,properties:d}=r;let h;n[0]!==c?(h=Vv(c),n[0]=c,n[1]=h):h=n[1];const y=h;let g;n[2]!==y.icon?(g=p.jsx("span",{className:sn.icon,children:y.icon}),n[2]=y.icon,n[3]=g):g=n[3];let f;n[4]!==s?(f=p.jsx("span",{className:sn.alias,children:s}),n[4]=s,n[5]=f):f=n[5];let b;n[6]!==y.label?(b=p.jsx("span",{className:sn.badge,children:y.label}),n[6]=y.label,n[7]=b):b=n[7];let E;n[8]!==g||n[9]!==f||n[10]!==b?(E=p.jsxs("div",{className:sn.header,children:[g,f,b]}),n[8]=g,n[9]=f,n[10]=b,n[11]=E):E=n[11];let x;n[12]!==d?(x=p.jsx("div",{className:sn.body,children:p.jsx(o0,{properties:d})}),n[12]=d,n[13]=x):x=n[13];let S;return n[14]!==E||n[15]!==x?(S=p.jsx(w.Fragment,{children:p.jsxs("div",{className:sn.content,children:[E,x]})}),n[14]=E,n[15]=x,n[16]=S):S=n[16],S}function on(r){const n=Ee.c(34),{data:s,isConnectable:c,selected:d}=r;let h;n[0]!==s.minHeight||n[1]!==d?(h=p.jsx(ty,{minWidth:180,minHeight:s.minHeight,isVisible:d}),n[0]=s.minHeight,n[1]=d,n[2]=h):h=n[2];let y;if(n[3]!==s.targetHandles||n[4]!==c){let S;n[6]!==c?(S=j=>{const{id:A,offset:_}=j;return p.jsx(Ui,{id:A,type:"target",position:Li.Left,isConnectable:c,className:sn.edgeHandle,style:{top:`calc(50% + ${_}px)`}},A)},n[6]=c,n[7]=S):S=n[7],y=s.targetHandles.map(S),n[3]=s.targetHandles,n[4]=c,n[5]=y}else y=n[5];let g;if(n[8]!==s.backSourceHandles||n[9]!==c){let S;n[11]!==c?(S=j=>{const{id:A,offset:_}=j;return p.jsx(Ui,{id:A,type:"source",position:Li.Left,isConnectable:c,className:sn.edgeHandle,style:{top:`calc(50% + ${_}px)`}},A)},n[11]=c,n[12]=S):S=n[12],g=s.backSourceHandles.map(S),n[8]=s.backSourceHandles,n[9]=c,n[10]=g}else g=n[10];let f;n[13]!==s.alias||n[14]!==s.nodeType||n[15]!==s.properties?(f=p.jsx(Af,{alias:s.alias,nodeType:s.nodeType,properties:s.properties}),n[13]=s.alias,n[14]=s.nodeType,n[15]=s.properties,n[16]=f):f=n[16];let b;if(n[17]!==s.sourceHandles||n[18]!==c){let S;n[20]!==c?(S=j=>{const{id:A,offset:_}=j;return p.jsx(Ui,{id:A,type:"source",position:Li.Right,isConnectable:c,className:sn.edgeHandle,style:{top:`calc(50% + ${_}px)`}},A)},n[20]=c,n[21]=S):S=n[21],b=s.sourceHandles.map(S),n[17]=s.sourceHandles,n[18]=c,n[19]=b}else b=n[19];let E;if(n[22]!==s.backTargetHandles||n[23]!==c){let S;n[25]!==c?(S=j=>{const{id:A,offset:_}=j;return p.jsx(Ui,{id:A,type:"target",position:Li.Right,isConnectable:c,className:sn.edgeHandle,style:{top:`calc(50% + ${_}px)`}},A)},n[25]=c,n[26]=S):S=n[26],E=s.backTargetHandles.map(S),n[22]=s.backTargetHandles,n[23]=c,n[24]=E}else E=n[24];let x;return n[27]!==h||n[28]!==y||n[29]!==g||n[30]!==f||n[31]!==b||n[32]!==E?(x=p.jsxs(p.Fragment,{children:[h,y,g,f,b,E]}),n[27]=h,n[28]=y,n[29]=g,n[30]=f,n[31]=b,n[32]=E,n[33]=x):x=n[33],x}const s0={Root:on,End:on,Fetcher:on,mapper:on,Math:on,JavaScript:on,Provider:on,Dictionary:on,Join:on,Extension:on,Island:on,Decision:on,default:on},r0="_graphWrapper_l4gww_15",c0="_graphSurface_l4gww_24",u0="_empty_l4gww_30",d0="_emptyIcon_l4gww_43",p0="_emptyCreateButton_l4gww_48",h0="_emptyHint_l4gww_70",f0="_refreshingOverlay_l4gww_104",m0="_clipboardDropOverlay_l4gww_116",g0="_clipboardDropMessage_l4gww_129",y0="_refreshingSpinner_l4gww_144",b0="_contextMenu_l4gww_159",v0="_contextMenuItem_l4gww_169",Lt={graphWrapper:r0,graphSurface:c0,empty:u0,emptyIcon:d0,emptyCreateButton:p0,emptyHint:h0,refreshingOverlay:f0,clipboardDropOverlay:m0,clipboardDropMessage:g0,refreshingSpinner:y0,contextMenu:b0,contextMenuItem:v0};class _0 extends w.Component{constructor(){super(...arguments),this.state={caughtError:null}}static getDerivedStateFromError(n){return{caughtError:n instanceof Error?n.message:String(n)}}componentDidCatch(n,s){var d,h;const c=n instanceof Error?n.message:String(n);console.error("[GraphView] Render error:",c,s.componentStack),(h=(d=this.props).onRenderError)==null||h.call(d,`Graph render failed: ${c}`)}render(){return this.state.caughtError?p.jsxs("div",{className:Lt.empty,children:[p.jsx("span",{className:Lt.emptyIcon,children:"⚠️"}),p.jsx("span",{children:"Graph could not be rendered."}),p.jsx("span",{children:this.state.caughtError})]}):this.props.children}}const Tc=240,jo=100,Jh=60,Zh=120,x0=120,S0=80,Ec="rgba(148, 163, 184, 0.42)",w0="var(--bg-secondary)",Xi=24,T0=32,Vh=["#0369a1","#15803d","#b45309","#7e22ce","#b91c1c","#0f766e","#c2410c","#a16207"],E0={fetch:"#0369a1",details:"#0369a1","ext-call":"#0369a1",mapping:"#b45309",compute:"#b45309",calculate:"#b45309",evaluate:"#b45309",fork:"#7e22ce",join:"#7e22ce",one:"#7e22ce",two:"#6d28d9",three:"#5b21b6",more:"#4c1d95",done:"#15803d",complete:"#15803d",finish:"#15803d",positive:"#15803d",negative:"#b91c1c"};function N0(r){let n=0;for(let s=0;s<r.length;s++)n=(n<<5)-n+r.charCodeAt(s),n|=0;return Math.abs(n)}function A0(r){if(r.length===0)return Ec;const n=r[0].trim().toLowerCase(),s=E0[n];return s||Vh[N0(n)%Vh.length]}function C0(r){return`source-${r}`}function j0(r){return`target-${r}`}function M0(r){return`back-source-${r}`}function D0(r){return`back-target-${r}`}function Qh(r,n){return n<=1?0:n===2?r===0?-Xi:Xi:(r-(n-1)/2)*Xi}function Kh(r){return r<=1?jo:Math.max(jo,(r-1)*Xi+T0*2)}const k0=new Set(["graph.math","graph.js"]),$h=["Dictionary","Provider","Module","Entity"];function O0(r,n){if(n.has(r.alias))return"flow";const c=r.types[0]??"",d=typeof r.properties.skill=="string"?r.properties.skill:void 0;return c==="Dictionary"?"Dictionary":c==="Provider"?"Provider":d&&k0.has(d)?"Module":d?"__unknown__":"Entity"}function R0(r,n,s){var H;const c=new Set;for(const D of n??[])c.add(D.source),c.add(D.target);const d=[],h=[],y=new Map;for(const D of r){const L=O0(D,c);y.set(D.alias,L),L==="flow"?d.push(D):h.push(D)}const g=new Set(d.map(D=>D.alias)),f=new Map,b=new Map;for(const D of d)f.set(D.alias,[]),b.set(D.alias,0);for(const D of n??[])!g.has(D.source)||!g.has(D.target)||((H=f.get(D.source))==null||H.push(D.target),b.set(D.target,(b.get(D.target)??0)+1));const E=d.filter(D=>b.get(D.alias)===0||D.types.includes("entry_point")).map(D=>D.alias),x=new Set;{let D=function(I){if(J.get(I)!==L)return;J.set(I,C);const K=[{node:I,childIdx:0}];for(;K.length>0;){const Z=K[K.length-1],ee=f.get(Z.node)??[];if(Z.childIdx>=ee.length){J.set(Z.node,Y),K.pop();continue}const P=ee[Z.childIdx++],le=J.get(P);le===C?x.add(`${Z.node}	${P}`):le===L&&(J.set(P,C),K.push({node:P,childIdx:0}))}};const L=0,C=1,Y=2,J=new Map;for(const I of d)J.set(I.alias,0);for(const I of E)D(I);for(const I of d)D(I.alias)}const S=new Map,j=[...E];for(E.forEach(D=>S.set(D,0));j.length>0;){const D=j.shift(),L=S.get(D)??0;for(const C of f.get(D)??[])x.has(`${D}	${C}`)||(!S.has(C)||S.get(C)<=L)&&(S.set(C,L+1),j.push(C))}const A=S.size>0?Math.max(...S.values()):0;for(const D of d)S.has(D.alias)||S.set(D.alias,A+1);const _=new Map;for(const[D,L]of S)_.has(L)||_.set(L,[]),_.get(L).push(D);const v=new Map;for(const[D,L]of _){let Y=-(L.reduce((J,I)=>J+(s.get(I)??jo),0)+Math.max(0,L.length-1)*Jh)/2;L.forEach(J=>{const I=s.get(J)??jo;v.set(J,{x:D*(Tc+Zh),y:Y}),Y+=I+Jh})}let T=0;for(const[D,L]of v)T=Math.max(T,L.y+(s.get(D)??jo));let O=T+(v.size>0?x0:0);const k=new Map;for(const D of $h)k.set(D,[]);k.set("__unknown__",[]);for(const D of h){const L=y.get(D.alias);k.get(L).push(D.alias)}for(const D of[...$h,"__unknown__"]){const L=(k.get(D)??[]).slice().sort();if(L.length===0)continue;const C=0,Y=L.reduce((J,I)=>Math.max(J,s.get(I)??jo),0);L.forEach((J,I)=>{v.set(J,{x:C+I*(Tc+Zh),y:O})}),O+=Y+S0}return{positions:v,levelOf:S}}function z0(r){const n=r.connections??[],s=new Map,c=new Map;for(const _ of n)s.set(_.source,(s.get(_.source)??0)+1),c.set(_.target,(c.get(_.target)??0)+1);const d=new Map(r.nodes.map(_=>[_.alias,Kh(Math.max(s.get(_.alias)??0,c.get(_.alias)??0))])),{positions:h,levelOf:y}=R0(r.nodes,n,d),g=new Set;for(const[_,v]of n.entries()){const T=y.get(v.source),O=y.get(v.target);T!==void 0&&O!==void 0&&T>=O&&g.add(_)}const f=new Map,b=new Map;for(const _ of r.nodes)f.set(_.alias,[]),b.set(_.alias,[]);for(const[_,v]of n.entries())g.has(_)?(b.get(v.source).push({connIndex:_,peerAlias:v.target,isBack:!0}),f.get(v.target).push({connIndex:_,peerAlias:v.source,isBack:!0})):(f.get(v.source).push({connIndex:_,peerAlias:v.target,isBack:!1}),b.get(v.target).push({connIndex:_,peerAlias:v.source,isBack:!1}));const E=_=>{var v;return((v=h.get(_))==null?void 0:v.y)??0};for(const _ of f.values())_.sort((v,T)=>E(v.peerAlias)-E(T.peerAlias));for(const _ of b.values())_.sort((v,T)=>E(v.peerAlias)-E(T.peerAlias));const x=new Map,S=new Map,j=r.nodes.map(_=>{const v=f.get(_.alias)??[],T=b.get(_.alias)??[],O=Kh(Math.max(v.length,T.length)),k=[],H=[];let D=0,L=0;for(let K=0;K<v.length;K++){const Z=v[K],ee=Qh(K,v.length);if(Z.isBack){const P=D0(L++);H.push({id:P,offset:ee}),S.set(Z.connIndex,P)}else{const P=C0(D++);k.push({id:P,offset:ee}),x.set(Z.connIndex,P)}}const C=[],Y=[];let J=0,I=0;for(let K=0;K<T.length;K++){const Z=T[K],ee=Qh(K,T.length);if(Z.isBack){const P=M0(I++);Y.push({id:P,offset:ee}),x.set(Z.connIndex,P)}else{const P=j0(J++);C.push({id:P,offset:ee}),S.set(Z.connIndex,P)}}return{id:_.alias,type:_.types[0]??"default",position:h.get(_.alias)??{x:0,y:0},width:Tc,height:O,style:Nf(_.types[0]??"unknown"),data:{alias:_.alias,nodeType:_.types[0]??"unknown",properties:_.properties,sourceHandles:k,targetHandles:C,backSourceHandles:Y,backTargetHandles:H,minHeight:O}}}),A=[];for(const[_,v]of n.entries()){const T=v.relations.map(H=>H.type),O=`${v.source}__${v.target}__${_}`,k=A0(T);A.push({id:O,source:v.source,target:v.target,sourceHandle:x.get(_),targetHandle:S.get(_),label:T.join(", "),type:"bezier",markerEnd:{type:ny.ArrowClosed,width:16,height:16,color:Ec},style:{stroke:Ec,strokeWidth:2},labelStyle:{fill:k,fontSize:10,fontWeight:700},labelBgStyle:{fill:w0,fillOpacity:.94,stroke:"rgba(15, 23, 42, 0.16)",strokeWidth:1},labelBgPadding:[5,2],labelBgBorderRadius:6,data:{relationTypes:T}})}return{nodes:j,edges:A}}const Gc="application/x-minigraph-clipboard-item";function qi(r){return r.includes(Gc)}function B0(r,n){r.effectAllowed="copy",r.setData(Gc,n)}function H0(r){const n=(r==null?void 0:r.getData(Gc))??"";return n.trim()?n:null}function G0(r,n){return r.nodes.find(s=>s.alias===n)}function U0(r,n){return(r.connections??[]).filter(s=>s.source!==s.target&&(s.source===n||s.target===n))}const L0="_toolbar_117v8_2",Y0="_nameGroup_117v8_13",q0="_graphName_117v8_20",I0="_stats_117v8_29",X0="_toolbarActions_117v8_49",J0="_toolbarButton_117v8_55",Co={toolbar:L0,nameGroup:Y0,graphName:q0,stats:I0,toolbarActions:X0,toolbarButton:J0};function Cf(r){const n=Ee.c(24),{graphData:s,graphName:c,onCopySuccess:d,onCopyError:h,extraActions:y}=r;let g;n[0]!==s||n[1]!==h||n[2]!==d?(g=()=>{s&&navigator.clipboard.writeText(JSON.stringify(s,null,2)).then(()=>d==null?void 0:d()).catch(()=>h==null?void 0:h())},n[0]=s,n[1]=h,n[2]=d,n[3]=g):g=n[3];const f=g,b=(s==null?void 0:s.nodes.length)??0;let E;n[4]!==(s==null?void 0:s.connections)?(E=(s==null?void 0:s.connections)??[],n[4]=s==null?void 0:s.connections,n[5]=E):E=n[5];const x=E.length,S=c??"Untitled";let j;n[6]!==S?(j=p.jsx("span",{className:Co.graphName,children:S}),n[6]=S,n[7]=j):j=n[7];const A=b!==1?"s":"",_=x!==1?"s":"";let v;n[8]!==x||n[9]!==b||n[10]!==A||n[11]!==_?(v=p.jsxs("span",{className:Co.stats,children:[b," node",A," · ",x," connection",_]}),n[8]=x,n[9]=b,n[10]=A,n[11]=_,n[12]=v):v=n[12];let T;n[13]!==j||n[14]!==v?(T=p.jsxs("div",{className:Co.nameGroup,children:[j,v]}),n[13]=j,n[14]=v,n[15]=T):T=n[15];let O;n[16]!==f?(O=p.jsx("button",{className:Co.toolbarButton,onClick:f,title:"Copy raw graph JSON to clipboard","aria-label":"Copy raw graph JSON to clipboard",children:"📑"}),n[16]=f,n[17]=O):O=n[17];let k;n[18]!==y||n[19]!==O?(k=p.jsxs("div",{className:Co.toolbarActions,children:[y,O]}),n[18]=y,n[19]=O,n[20]=k):k=n[20];let H;return n[21]!==k||n[22]!==T?(H=p.jsxs("div",{className:Co.toolbar,children:[T,k]}),n[21]=k,n[22]=T,n[23]=H):H=n[23],H}const Z0="_menu_13qxg_1",V0="_menuItem_13qxg_12",Wh={menu:Z0,menuItem:V0};function Q0(r){const n=Ee.c(17),{open:s,x:c,y:d,canCreateNode:h,onCreateNode:y,onClose:g}=r,f=w.useRef(null),b=w.useRef(null);let E,x;if(n[0]!==g||n[1]!==s?(E=()=>{var k;if(!s)return;(k=b.current)==null||k.focus();const T=H=>{f.current&&!f.current.contains(H.target)&&g()},O=H=>{H.key==="Escape"&&(H.preventDefault(),g())};return document.addEventListener("pointerdown",T),document.addEventListener("keydown",O),()=>{document.removeEventListener("pointerdown",T),document.removeEventListener("keydown",O)}},x=[s,g],n[0]=g,n[1]=s,n[2]=E,n[3]=x):(E=n[2],x=n[3]),w.useEffect(E,x),!s)return null;let S;n[4]!==c||n[5]!==d?(S={left:c,top:d},n[4]=c,n[5]=d,n[6]=S):S=n[6];const j=!h;let A;n[7]!==h||n[8]!==g||n[9]!==y?(A=()=>{h&&(y(),g())},n[7]=h,n[8]=g,n[9]=y,n[10]=A):A=n[10];let _;n[11]!==j||n[12]!==A?(_=p.jsx("button",{ref:b,role:"menuitem",type:"button",className:Wh.menuItem,disabled:j,onClick:A,children:"Create Node"}),n[11]=j,n[12]=A,n[13]=_):_=n[13];let v;return n[14]!==S||n[15]!==_?(v=p.jsx("div",{ref:f,className:Wh.menu,style:S,role:"menu","aria-label":"Graph actions",children:_}),n[14]=S,n[15]=_,n[16]=v):v=n[16],v}const Ph=[],Fh=[];function K0(r){const n=Ee.c(92),{graphData:s,graphName:c,onCopySuccess:d,onCopyError:h,onRenderError:y,isRefreshing:g,onClipNode:f,onClipboardDrop:b,isConnected:E,supportsAuthoring:x,onCreateNode:S}=r,j=g===void 0?!1:g,A=x===void 0?!1:x,[_,v]=w.useState(null),[T,O]=w.useState(null),[k,H]=w.useState(!1),D=w.useRef(null),L=w.useRef(0),C=!!(A&&S&&E),Y=!!(b&&E);let J;n[0]===Symbol.for("react.memo_cache_sentinel")?(J=()=>{L.current=0,H(!1)},n[0]=J):J=n[0];const I=J;let K,Z;n[1]!==_?(K=()=>{if(!_)return;const oe=st=>{D.current&&!D.current.contains(st.target)&&v(null)},Me=st=>{st.key==="Escape"&&v(null)};return document.addEventListener("mousedown",oe),document.addEventListener("keydown",Me),()=>{document.removeEventListener("mousedown",oe),document.removeEventListener("keydown",Me)}},Z=[_],n[1]=_,n[2]=K,n[3]=Z):(K=n[2],Z=n[3]),w.useEffect(K,Z);let ee,P;n[4]!==T?(ee=()=>{if(!T)return;const oe=st=>{st.key==="Escape"&&O(null)},Me=()=>O(null);return document.addEventListener("keydown",oe),window.addEventListener("scroll",Me,!0),window.addEventListener("resize",Me),()=>{document.removeEventListener("keydown",oe),window.removeEventListener("scroll",Me,!0),window.removeEventListener("resize",Me)}},P=[T],n[4]=T,n[5]=ee,n[6]=P):(ee=n[5],P=n[6]),w.useEffect(ee,P);let le,R;n[7]===Symbol.for("react.memo_cache_sentinel")?(le=()=>{const oe=()=>I();return window.addEventListener("dragend",oe),window.addEventListener("drop",oe),()=>{window.removeEventListener("dragend",oe),window.removeEventListener("drop",oe),I()}},R=[I],n[7]=le,n[8]=R):(le=n[7],R=n[8]),w.useEffect(le,R);const z=w.useRef(y);let $,ie;n[9]!==y?($=()=>{z.current=y},ie=[y],n[9]=y,n[10]=$,n[11]=ie):($=n[10],ie=n[11]),w.useEffect($,ie);let se;e:{if(!s){let oe;n[12]===Symbol.for("react.memo_cache_sentinel")?(oe={nodes:Ph,edges:Fh,transformError:null},n[12]=oe):oe=n[12],se=oe;break e}try{let oe;n[13]!==s?(oe=z0(s),n[13]=s,n[14]=oe):oe=n[14];const Me=oe;let st;n[15]!==Me?(st={...Me,transformError:null},n[15]=Me,n[16]=st):st=n[16],se=st}catch(oe){const Me=oe,st=Me instanceof Error?Me.message:String(Me);let bn;n[17]===Symbol.for("react.memo_cache_sentinel")?(bn={nodes:Ph,edges:Fh,transformError:st},n[17]=bn):bn=n[17],se=bn}}const{nodes:pe,edges:ae,transformError:te}=se;let ue,re;n[18]!==te?(ue=()=>{var oe;te&&((oe=z.current)==null||oe.call(z,`Graph render failed: ${te}`))},re=[te],n[18]=te,n[19]=ue,n[20]=re):(ue=n[19],re=n[20]),w.useEffect(ue,re);let me;n[21]!==s?(me=s?JSON.stringify(s.nodes.map(W0)):"empty",n[21]=s,n[22]=me):me=n[22];const Ce=me,[Oe,_e,Re]=ay(pe),[ne,ge,xe]=oy(ae);let F,ye;n[23]!==ae||n[24]!==pe||n[25]!==ge||n[26]!==_e?(F=()=>{_e(pe),ge(ae)},ye=[pe,ae,_e,ge],n[23]=ae,n[24]=pe,n[25]=ge,n[26]=_e,n[27]=F,n[28]=ye):(F=n[27],ye=n[28]),w.useEffect(F,ye);let Se;n[29]!==Y?(Se=oe=>{Y&&qi(Array.from(oe.dataTransfer.types))&&(oe.preventDefault(),L.current=L.current+1,H(!0))},n[29]=Y,n[30]=Se):Se=n[30];const he=Se;let we;n[31]!==Y?(we=oe=>{Y&&qi(Array.from(oe.dataTransfer.types))&&(oe.preventDefault(),oe.dataTransfer.dropEffect="copy",H(!0))},n[31]=Y,n[32]=we):we=n[32];const Ne=we;let Le;n[33]===Symbol.for("react.memo_cache_sentinel")?(Le=oe=>{qi(Array.from(oe.dataTransfer.types))&&(L.current=Math.max(0,L.current-1),L.current===0&&H(!1))},n[33]=Le):Le=n[33];const nt=Le;let qe;n[34]!==Y||n[35]!==b?(qe=oe=>{if(!Y||!qi(Array.from(oe.dataTransfer.types)))return;oe.preventDefault();const Me=H0(oe.dataTransfer);I(),Me&&(b==null||b(Me))},n[34]=Y,n[35]=b,n[36]=qe):qe=n[36];const vt=qe,fe=!!(s&&s.nodes.length>0);if(te){let oe,Me;n[37]===Symbol.for("react.memo_cache_sentinel")?(oe=p.jsx("span",{className:Lt.emptyIcon,children:"⚠️"}),Me=p.jsx("span",{children:"Graph could not be rendered."}),n[37]=oe,n[38]=Me):(oe=n[37],Me=n[38]);let st;return n[39]!==te?(st=p.jsxs("div",{className:Lt.empty,children:[oe,Me,p.jsx("span",{children:te})]}),n[39]=te,n[40]=st):st=n[40],st}let Mt;n[41]!==s||n[42]!==c||n[43]!==fe||n[44]!==h||n[45]!==d?(Mt=fe&&s&&p.jsx(Cf,{graphData:s,graphName:c,onCopySuccess:d,onCopyError:h}),n[41]=s,n[42]=c,n[43]=fe,n[44]=h,n[45]=d,n[46]=Mt):Mt=n[46];let Ht;n[47]!==C||n[48]!==ne||n[49]!==fe||n[50]!==E||n[51]!==Oe||n[52]!==f||n[53]!==S||n[54]!==xe||n[55]!==Re||n[56]!==A?(Ht=fe?p.jsxs(ly,{nodes:Oe,edges:ne,onNodesChange:Re,onEdgesChange:xe,nodeTypes:s0,fitView:!0,fitViewOptions:{padding:.25},minZoom:.2,maxZoom:2.5,proOptions:{hideAttribution:!1},onNodeContextMenu:(oe,Me)=>{oe.preventDefault(),oe.stopPropagation(),O(null),f&&v({x:oe.clientX,y:oe.clientY,nodeAlias:Me.data.alias})},onPaneContextMenu:oe=>{oe.preventDefault(),C&&(v(null),O({x:oe.clientX,y:oe.clientY}))},onPaneClick:()=>{v(null),O(null)},children:[p.jsx(iy,{variant:sy.Dots,gap:18,size:1,color:"rgba(255,255,255,0.07)"}),p.jsx(ry,{showInteractive:!1}),p.jsx(cy,{nodeColor:$0,maskColor:"rgba(0,0,0,0.3)",style:{background:"#fff"}})]}):p.jsxs("div",{className:Lt.empty,children:[p.jsx("span",{className:Lt.emptyIcon,children:"🕸️"}),p.jsx("span",{children:"No graph data yet."}),p.jsxs("span",{children:["Run ",p.jsx("strong",{children:"describe graph"})," or ",p.jsx("strong",{children:"export graph"})," in the playground."]}),A&&S&&p.jsxs(p.Fragment,{children:[p.jsx("button",{type:"button",className:Lt.emptyCreateButton,disabled:!E,onClick:()=>S("empty-graph"),children:"Create Node"}),!E&&p.jsx("span",{className:Lt.emptyHint,children:"Connect WebSocket to create a node."})]})]}),n[47]=C,n[48]=ne,n[49]=fe,n[50]=E,n[51]=Oe,n[52]=f,n[53]=S,n[54]=xe,n[55]=Re,n[56]=A,n[57]=Ht):Ht=n[57];let ut;n[58]!==j?(ut=j&&p.jsx("div",{className:Lt.refreshingOverlay,children:p.jsx("div",{className:Lt.refreshingSpinner,role:"status","aria-label":"Graph refreshing"})}),n[58]=j,n[59]=ut):ut=n[59];let ht;n[60]!==k?(ht=k&&p.jsx("div",{className:Lt.clipboardDropOverlay,children:p.jsx("div",{className:Lt.clipboardDropMessage,children:"Drop to paste workspace node"})}),n[60]=k,n[61]=ht):ht=n[61];const qt=T!==null,Gt=(T==null?void 0:T.x)??0,Sn=(T==null?void 0:T.y)??0;let Dt;n[62]!==S?(Dt=()=>S==null?void 0:S("pane-context-menu"),n[62]=S,n[63]=Dt):Dt=n[63];let Tt;n[64]===Symbol.for("react.memo_cache_sentinel")?(Tt=()=>O(null),n[64]=Tt):Tt=n[64];let Ye;n[65]!==C||n[66]!==qt||n[67]!==Gt||n[68]!==Sn||n[69]!==Dt?(Ye=p.jsx(Q0,{open:qt,x:Gt,y:Sn,canCreateNode:C,onCreateNode:Dt,onClose:Tt}),n[65]=C,n[66]=qt,n[67]=Gt,n[68]=Sn,n[69]=Dt,n[70]=Ye):Ye=n[70];let it;n[71]!==_||n[72]!==s||n[73]!==f?(it=_&&f&&s&&p.jsx("div",{ref:D,className:Lt.contextMenu,style:{position:"fixed",top:_.y,left:_.x},role:"menu",children:p.jsx("button",{role:"menuitem",autoFocus:!0,className:Lt.contextMenuItem,onClick:()=>{const oe=G0(s,_.nodeAlias);if(oe){const Me=U0(s,_.nodeAlias);f(oe,Me)}v(null)},children:"Clip to Clipboard"})}),n[71]=_,n[72]=s,n[73]=f,n[74]=it):it=n[74];let Fe;n[75]!==he||n[76]!==Ne||n[77]!==vt||n[78]!==Ht||n[79]!==ut||n[80]!==ht||n[81]!==Ye||n[82]!==it?(Fe=p.jsxs("div",{className:Lt.graphSurface,onDragEnter:he,onDragOver:Ne,onDragLeave:nt,onDrop:vt,children:[Ht,ut,ht,Ye,it]}),n[75]=he,n[76]=Ne,n[77]=vt,n[78]=Ht,n[79]=ut,n[80]=ht,n[81]=Ye,n[82]=it,n[83]=Fe):Fe=n[83];let Te;n[84]!==j||n[85]!==Mt||n[86]!==Fe?(Te=p.jsxs("div",{className:Lt.graphWrapper,"aria-busy":j,children:[Mt,Fe]}),n[84]=j,n[85]=Mt,n[86]=Fe,n[87]=Te):Te=n[87];let ct;return n[88]!==Ce||n[89]!==y||n[90]!==Te?(ct=p.jsx(_0,{onRenderError:y,children:Te},Ce),n[88]=Ce,n[89]=y,n[90]=Te,n[91]=ct):ct=n[91],ct}function $0(r){return{Root:"#15803d",End:"#dc2626",Fetcher:"#2563eb",mapper:"#ea580c",Math:"#a16207",JavaScript:"#7e22ce",Provider:"#be185d",Dictionary:"#0e7490",Join:"#65a30d",Extension:"#4338ca",Island:"#475569",Decision:"#b45309"}[r.type??""]??"#6c7086"}function W0(r){return r.alias}const P0="_root_1yhjs_2",F0="_empty_1yhjs_10",e2="_emptyIcon_1yhjs_23",t2="_toolbarButton_1yhjs_29 _toolbarButton_117v8_55",n2="_scrollBody_1yhjs_34",a2="_jsonContainer_1yhjs_45",o2="_jsonLabel_1yhjs_46",l2="_jsonString_1yhjs_47",i2="_jsonNumber_1yhjs_48",s2="_jsonBoolean_1yhjs_49",r2="_jsonNull_1yhjs_50",ln={root:P0,empty:F0,emptyIcon:e2,toolbarButton:t2,scrollBody:n2,jsonContainer:a2,jsonLabel:o2,jsonString:l2,jsonNumber:i2,jsonBoolean:s2,jsonNull:r2},c2=dy,u2=uy,d2=r=>r<3,p2={default:d2,all:c2,none:u2};function h2(r){const n=Ee.c(23),{graphData:s,graphName:c,onCopySuccess:d,onCopyError:h}=r,[y,g]=w.useState("all");if(!s){let D;return n[0]===Symbol.for("react.memo_cache_sentinel")?(D=p.jsx("div",{className:ln.root,children:p.jsxs("div",{className:ln.empty,children:[p.jsx("span",{className:ln.emptyIcon,children:"🕸️"}),p.jsx("span",{children:"No graph data yet."}),p.jsx("span",{children:"Pin a graph-link message in the Console to load the raw data here."})]})}),n[0]=D):D=n[0],D}let f;n[1]===Symbol.for("react.memo_cache_sentinel")?(f=()=>g("all"),n[1]=f):f=n[1];const b=y==="all";let E;n[2]!==b?(E=p.jsx("button",{className:ln.toolbarButton,onClick:f,title:"Expand all nodes","aria-label":"Expand all JSON nodes","aria-pressed":b,children:"➖"}),n[2]=b,n[3]=E):E=n[3];let x;n[4]===Symbol.for("react.memo_cache_sentinel")?(x=()=>g("none"),n[4]=x):x=n[4];const S=y==="none";let j;n[5]!==S?(j=p.jsx("button",{className:ln.toolbarButton,onClick:x,title:"Collapse all nodes","aria-label":"Collapse all JSON nodes","aria-pressed":S,children:"➕"}),n[5]=S,n[6]=j):j=n[6];let A;n[7]!==E||n[8]!==j?(A=p.jsxs(p.Fragment,{children:[E,j]}),n[7]=E,n[8]=j,n[9]=A):A=n[9];let _;n[10]!==s||n[11]!==c||n[12]!==h||n[13]!==d||n[14]!==A?(_=p.jsx(Cf,{graphData:s,graphName:c,onCopySuccess:d,onCopyError:h,extraActions:A}),n[10]=s,n[11]=c,n[12]=h,n[13]=d,n[14]=A,n[15]=_):_=n[15];const v=s,T=p2[y];let O;n[16]===Symbol.for("react.memo_cache_sentinel")?(O={...El,container:`${El.container} ${ln.jsonContainer}`,label:ln.jsonLabel,stringValue:ln.jsonString,numberValue:ln.jsonNumber,booleanValue:ln.jsonBoolean,nullValue:ln.jsonNull},n[16]=O):O=n[16];let k;n[17]!==T||n[18]!==v?(k=p.jsx("div",{className:ln.scrollBody,children:p.jsx(Rc,{data:v,shouldExpandNode:T,style:O})}),n[17]=T,n[18]=v,n[19]=k):k=n[19];let H;return n[20]!==k||n[21]!==_?(H=p.jsxs("div",{className:ln.root,children:[_,k]}),n[20]=k,n[21]=_,n[22]=H):H=n[22],H}const f2="_rightPanel_1xiht_2",m2="_tabStrip_1xiht_10",g2="_tab_1xiht_10",y2="_tabActive_1xiht_38",b2="_tabBadge_1xiht_42",v2="_tabBody_1xiht_48",_2="_tabBodyHidden_1xiht_57",x2="_graphContent_1xiht_61",S2="_rightPanelGroup_1xiht_68",w2="_verticalResizeHandle_1xiht_76",Ct={rightPanel:f2,tabStrip:m2,tab:g2,tabActive:y2,tabBadge:b2,tabBody:v2,tabBodyHidden:_2,graphContent:x2,rightPanelGroup:S2,verticalResizeHandle:w2},ef="help-split-percent",hc="help-split-maximized",T2=45,E2=98;function N2({tabs:r,payload:n,onChange:s,validation:c,onFormat:d,onUpload:h,graphData:y,graphName:g,activeTab:f,onTabChange:b,onGraphRenderError:E,onGraphDataCopySuccess:x,onGraphDataCopyError:S,isGraphRefreshing:j,onClipNode:A,onClipboardDrop:_,isConnected:v,supportsAuthoring:T,onCreateNode:O,helpPanel:k}){const H=w.useId(),D=`${H}-tab-payload`,L=`${H}-tab-graph`,C=`${H}-tab-graph-data`,Y=p.jsxs("div",{className:Ct.rightPanel,children:[p.jsxs("div",{className:Ct.tabStrip,role:"tablist","aria-label":"Right panel tabs",children:[r.includes("payload")&&p.jsx("button",{role:"tab","aria-selected":f==="payload","aria-controls":D,className:`${Ct.tab}${f==="payload"?` ${Ct.tabActive}`:""}`,onClick:()=>b("payload"),children:"Payload Editor"}),r.includes("graph")&&p.jsxs("button",{role:"tab","aria-selected":f==="graph","aria-controls":L,className:`${Ct.tab}${f==="graph"?` ${Ct.tabActive}`:""}`,onClick:()=>b("graph"),children:["Graph",y!==null&&p.jsx("span",{className:Ct.tabBadge,"aria-label":"Graph data available",children:"🕸️"})]}),r.includes("graph-data")&&p.jsx("button",{role:"tab","aria-selected":f==="graph-data","aria-controls":C,className:`${Ct.tab}${f==="graph-data"?` ${Ct.tabActive}`:""}`,onClick:()=>b("graph-data"),children:"Graph Data (Raw)"})]}),r.includes("payload")&&p.jsx("div",{role:"tabpanel",id:D,tabIndex:f==="payload"?0:-1,className:`${Ct.tabBody}${f!=="payload"?` ${Ct.tabBodyHidden}`:""}`,children:p.jsx(qv,{payload:n,onChange:s,validation:c,onFormat:d,onUpload:h})}),r.includes("graph")&&p.jsx("div",{role:"tabpanel",id:L,tabIndex:f==="graph"?0:-1,className:`${Ct.tabBody}${f!=="graph"?` ${Ct.tabBodyHidden}`:""}`,children:p.jsx("div",{className:Ct.graphContent,children:p.jsx(K0,{graphData:y,graphName:g,onRenderError:E,isRefreshing:j,onCopySuccess:x,onCopyError:S,onClipNode:A,onClipboardDrop:_,isConnected:v,supportsAuthoring:T,onCreateNode:O})})}),r.includes("graph-data")&&p.jsx("div",{role:"tabpanel",id:C,tabIndex:f==="graph-data"?0:-1,className:`${Ct.tabBody}${f!=="graph-data"?` ${Ct.tabBodyHidden}`:""}`,children:p.jsx(h2,{graphData:y,graphName:g,onCopySuccess:x,onCopyError:S})})]}),J=w.useRef(Number(sessionStorage.getItem(ef))||T2),I=w.useRef(null),K=w.useRef(null),[Z,ee]=w.useState(()=>sessionStorage.getItem(hc)==="1"),P=w.useRef(Z),le=w.useCallback(ae=>{const te=ae["help-split-help"];if(te===void 0)return;const ue=te>=E2;ue!==P.current&&(P.current=ue,ee(ue),sessionStorage.setItem(hc,ue?"1":"0")),ue||(J.current=te,sessionStorage.setItem(ef,String(te)))},[]),R=w.useCallback(()=>{var te,ue,re,me;const ae=!P.current;if(P.current=ae,ee(ae),sessionStorage.setItem(hc,ae?"1":"0"),ae)(te=K.current)==null||te.resize("0%"),(ue=I.current)==null||ue.resize("100%");else{const Ce=J.current;(re=I.current)==null||re.resize(`${Ce}%`),(me=K.current)==null||me.resize(`${100-Ce}%`)}},[]),z=!!k;if(w.useEffect(()=>{z&&P.current&&requestAnimationFrame(()=>{var ae,te;(ae=K.current)==null||ae.resize("0%"),(te=I.current)==null||te.resize("100%")})},[z]),!k)return Y;const $=typeof k=="function"?k(R,Z):k,se=P.current?100:J.current,pe=100-se;return p.jsxs(mf,{orientation:"vertical",className:Ct.rightPanelGroup,onLayoutChanged:le,children:[p.jsx(Tl,{panelRef:K,defaultSize:`${pe}%`,minSize:"0%",children:Y}),p.jsx(vc,{className:Ct.verticalResizeHandle,"aria-label":"Resize help panel"}),p.jsx(Tl,{id:"help-split-help",panelRef:I,defaultSize:`${se}%`,minSize:"15%",children:$})]})}class A2 extends yf.Component{constructor(){super(...arguments),this.state={hasError:!1}}static getDerivedStateFromError(){return{hasError:!0}}componentDidCatch(n,s){console.error("[ConsoleErrorBoundary] Failed to render message:",n,s.componentStack)}render(){return this.state.hasError?p.jsx("span",{children:this.props.fallback}):this.props.children}}const C2=2e3,j2=(r={})=>{const{onSuccess:n,onError:s}=r,[c,d]=w.useState(!1),h=w.useRef(null);return w.useEffect(()=>()=>{h.current!==null&&clearTimeout(h.current)},[]),{copy:w.useCallback(async g=>{if(!navigator.clipboard)return console.warn("useCopyToClipboard: Clipboard API not available in this browser."),s==null||s(),!1;try{return await navigator.clipboard.writeText(g),d(!0),h.current!==null&&clearTimeout(h.current),h.current=setTimeout(()=>{h.current=null,d(!1)},C2),n==null||n(),!0}catch(f){return console.error("useCopyToClipboard: Failed to write to clipboard.",f),s==null||s(),!1}},[n,s]),copied:c}},M2="_consoleRoot_1lgp1_2",D2="_consoleHeader_1lgp1_10",k2="_consoleTitle_1lgp1_20",O2="_consoleControls_1lgp1_25",R2="_controlButton_1lgp1_30",z2="_console_1lgp1_2",B2="_emptyConsole_1lgp1_67",H2="_consoleMessage_1lgp1_80",G2="_consoleMessageActivatable_1lgp1_94",U2="_consoleMessageGraphLink_1lgp1_104",L2="_consoleMessageLargePayload_1lgp1_115",Y2="_consoleMessageMockUpload_1lgp1_122",q2="_uploadMockButton_1lgp1_131",I2="_copyButton_1lgp1_172",X2="_copyButtonCopied_1lgp1_225",J2="_sendToJsonPathButton_1lgp1_234",Z2="_messageIcon_1lgp1_268",V2="_messageContent_1lgp1_272",Q2="_messageText_1lgp1_278",K2="_messageTime_1lgp1_283",$2="_jsonViewWrapper_1lgp1_295",W2="_jsonContainer_1lgp1_301",P2="_jsonLabel_1lgp1_302",F2="_jsonString_1lgp1_303",e_="_jsonNumber_1lgp1_304",t_="_jsonBoolean_1lgp1_305",n_="_jsonNull_1lgp1_306",Ke={consoleRoot:M2,consoleHeader:D2,consoleTitle:k2,consoleControls:O2,controlButton:R2,console:z2,emptyConsole:B2,consoleMessage:H2,consoleMessageActivatable:G2,consoleMessageGraphLink:U2,consoleMessageLargePayload:L2,consoleMessageMockUpload:Y2,uploadMockButton:q2,copyButton:I2,copyButtonCopied:X2,sendToJsonPathButton:J2,messageIcon:Z2,messageContent:V2,messageText:Q2,messageTime:K2,"messageType-error":"_messageType-error_1lgp1_290","messageType-info":"_messageType-info_1lgp1_291","messageType-welcome":"_messageType-welcome_1lgp1_292",jsonViewWrapper:$2,jsonContainer:W2,jsonLabel:P2,jsonString:F2,jsonNumber:e_,jsonBoolean:t_,jsonNull:n_};function a_(r){var vt;const n=Ee.c(77),{message:s,msgId:c,classificationMap:d,onGraphLink:h,onCopyMessage:y,onSendToJsonPath:g,onUploadMockData:f,successfulUploadPaths:b}=r;let E,x,S;n[0]!==s?(x=Xy(s),E=Jy(x.type),S=Nl(x.message),n[0]=s,n[1]=E,n[2]=x,n[3]=S):(E=n[1],x=n[2],S=n[3]);const j=S;let A,_,v,T,O,k;if(n[4]!==d||n[5]!==c||n[6]!==f||n[7]!==b){const fe=(c!==void 0?d==null?void 0:d.get(c):void 0)??[];_=fe.some(r_),v=fe.some(s_),T=fe.some(i_),O=((vt=fe.find(l_))==null?void 0:vt.uploadPath)??null,A=!!f&&T&&O!==null,k=A&&!!(b!=null&&b.has(O)),n[4]=d,n[5]=c,n[6]=f,n[7]=b,n[8]=A,n[9]=_,n[10]=v,n[11]=T,n[12]=O,n[13]=k}else A=n[8],_=n[9],v=n[10],T=n[11],O=n[12],k=n[13];const H=k,D=!!h&&_&&!T&&!v,L=!!g&&j.isJSON;let C;n[14]!==y?(C={onSuccess:y},n[14]=y,n[15]=C):C=n[15];const{copy:Y,copied:J}=j2(C);let I;n[16]!==Y||n[17]!==s?(I=fe=>{fe.stopPropagation(),Y(s)},n[16]=Y,n[17]=s,n[18]=I):I=n[18];const K=I;let Z;n[19]!==Y||n[20]!==s?(Z=fe=>{(fe.key==="Enter"||fe.key===" ")&&(fe.preventDefault(),fe.stopPropagation(),Y(s))},n[19]=Y,n[20]=s,n[21]=Z):Z=n[21];const ee=Z;let P;n[22]!==j.data||n[23]!==j.isJSON||n[24]!==g?(P=fe=>{if(fe.stopPropagation(),!g||!j.isJSON)return;const Mt=JSON.stringify(j.data,null,2);g(Mt)},n[22]=j.data,n[23]=j.isJSON,n[24]=g,n[25]=P):P=n[25];const le=P;let R;n[26]!==O||n[27]!==f?(R=fe=>{fe.stopPropagation(),!(!f||!O)&&f(O)},n[26]=O,n[27]=f,n[28]=R):R=n[28];const z=R,$=Ke[`messageType-${x.type}`],ie=D?Ke.consoleMessageActivatable:"",se=_?Ke.consoleMessageGraphLink:"",pe=v?Ke.consoleMessageLargePayload:"",ae=T?Ke.consoleMessageMockUpload:"";let te;n[29]!==se||n[30]!==pe||n[31]!==ae||n[32]!==$||n[33]!==ie?(te=[Ke.consoleMessage,$,ie,se,pe,ae].filter(Boolean),n[29]=se,n[30]=pe,n[31]=ae,n[32]=$,n[33]=ie,n[34]=te):te=n[34];const ue=te.join(" ");let re;n[35]!==D||n[36]!==h?(re=D?()=>h():void 0,n[35]=D,n[36]=h,n[37]=re):re=n[37];const me=D?"Click to load graph in Graph View":void 0,Ce=D?"button":void 0,Oe=D?0:void 0;let _e;n[38]!==D||n[39]!==h?(_e=D?fe=>{(fe.key==="Enter"||fe.key===" ")&&(fe.preventDefault(),h())}:void 0,n[38]=D,n[39]=h,n[40]=_e):_e=n[40];const Re=D?"Load graph in Graph View":void 0,ne=T?"⬆️":v?"⬇️":_?"🕸️":E;let ge;n[41]!==ne?(ge=p.jsx("span",{className:Ke.messageIcon,children:ne}),n[41]=ne,n[42]=ge):ge=n[42];let xe;n[43]!==j.data||n[44]!==j.isJSON||n[45]!==x.message||n[46]!==H?(xe=p.jsx("div",{className:Ke.messageContent,children:j.isJSON?p.jsx("div",{className:Ke.jsonViewWrapper,children:p.jsx(Rc,{data:j.data,shouldExpandNode:o_,style:{...El,container:`${El.container} ${Ke.jsonContainer}`,label:Ke.jsonLabel,stringValue:Ke.jsonString,numberValue:Ke.jsonNumber,booleanValue:Ke.jsonBoolean,nullValue:Ke.jsonNull}})}):p.jsxs("span",{className:Ke.messageText,children:[x.message,H&&p.jsx("span",{title:"Upload succeeded",children:" ✅"})]})}),n[43]=j.data,n[44]=j.isJSON,n[45]=x.message,n[46]=H,n[47]=xe):xe=n[47];const F=`${Ke.copyButton} ${J?Ke.copyButtonCopied:""}`,ye=J?"Copied!":"Copy message",Se=J?"Copied to clipboard":"Copy message to clipboard",he=J?"✅":"📄";let we;n[48]!==K||n[49]!==ee||n[50]!==F||n[51]!==ye||n[52]!==Se||n[53]!==he?(we=p.jsx("button",{className:F,onClick:K,onKeyDown:ee,title:ye,"aria-label":Se,tabIndex:0,children:he}),n[48]=K,n[49]=ee,n[50]=F,n[51]=ye,n[52]=Se,n[53]=he,n[54]=we):we=n[54];let Ne;n[55]!==L||n[56]!==le?(Ne=L&&p.jsx("button",{className:Ke.sendToJsonPathButton,onClick:le,onKeyDown:fe=>{(fe.key==="Enter"||fe.key===" ")&&le(fe)},title:"Open in JSON-Path Playground","aria-label":"Open this JSON in the JSON-Path Playground",tabIndex:0,children:"➡️"}),n[55]=L,n[56]=le,n[57]=Ne):Ne=n[57];let Le;n[58]!==A||n[59]!==z?(Le=A&&p.jsx("button",{className:Ke.uploadMockButton,onClick:z,onKeyDown:fe=>{(fe.key==="Enter"||fe.key===" ")&&z(fe)},title:"Re-open upload dialog","aria-label":"Re-open mock data upload dialog",tabIndex:0,children:"⬆️ Upload JSON…"}),n[58]=A,n[59]=z,n[60]=Le):Le=n[60];let nt;n[61]!==x.time?(nt=x.time&&p.jsx("span",{className:Ke.messageTime,children:x.time}),n[61]=x.time,n[62]=nt):nt=n[62];let qe;return n[63]!==ue||n[64]!==re||n[65]!==me||n[66]!==Ce||n[67]!==Oe||n[68]!==_e||n[69]!==Re||n[70]!==ge||n[71]!==xe||n[72]!==we||n[73]!==Ne||n[74]!==Le||n[75]!==nt?(qe=p.jsxs("div",{className:ue,onClick:re,title:me,role:Ce,tabIndex:Oe,onKeyDown:_e,"aria-label":Re,children:[ge,xe,we,Ne,Le,nt]}),n[63]=ue,n[64]=re,n[65]=me,n[66]=Ce,n[67]=Oe,n[68]=_e,n[69]=Re,n[70]=ge,n[71]=xe,n[72]=we,n[73]=Ne,n[74]=Le,n[75]=nt,n[76]=qe):qe=n[76],qe}function o_(r){return r<1}function l_(r){return r.kind==="upload.invitation"}function i_(r){return r.kind==="upload.invitation"}function s_(r){return r.kind==="payload.large"}function r_(r){return r.kind==="graph.link"}function c_(r){const n=Ee.c(32),{messages:s,classificationMap:c,onCopy:d,onClear:h,consoleRef:y,onGraphLinkMessage:g,onCopyMessage:f,onSendToJsonPath:b,onUploadMockData:E,successfulUploadPaths:x}=r;let S;n[0]===Symbol.for("react.memo_cache_sentinel")?(S=p.jsx("span",{className:Ke.consoleTitle,children:"Console Output"}),n[0]=S):S=n[0];let j;n[1]!==d?(j=p.jsx("button",{className:Ke.controlButton,onClick:d,title:"Copy console output","aria-label":"Copy console output to clipboard",children:"📑"}),n[1]=d,n[2]=j):j=n[2];let A;n[3]!==h?(A=p.jsx("button",{className:Ke.controlButton,onClick:h,title:"Clear console","aria-label":"Clear console",children:"🗑️"}),n[3]=h,n[4]=A):A=n[4];let _;n[5]!==j||n[6]!==A?(_=p.jsxs("div",{className:Ke.consoleHeader,children:[S,p.jsxs("div",{className:Ke.consoleControls,children:[j,A]})]}),n[5]=j,n[6]=A,n[7]=_):_=n[7];let v;if(n[8]!==c||n[9]!==s||n[10]!==f||n[11]!==g||n[12]!==b||n[13]!==E||n[14]!==x){let H;n[16]!==c||n[17]!==f||n[18]!==g||n[19]!==b||n[20]!==E||n[21]!==x?(H=D=>p.jsx(A2,{fallback:D.raw,children:p.jsx(a_,{message:D.raw,msgId:D.id,classificationMap:c,onGraphLink:g?()=>g(D):void 0,onCopyMessage:f,onSendToJsonPath:b,onUploadMockData:E,successfulUploadPaths:x})},D.id),n[16]=c,n[17]=f,n[18]=g,n[19]=b,n[20]=E,n[21]=x,n[22]=H):H=n[22],v=s.map(H),n[8]=c,n[9]=s,n[10]=f,n[11]=g,n[12]=b,n[13]=E,n[14]=x,n[15]=v}else v=n[15];let T;n[23]!==s.length?(T=s.length===0&&p.jsxs("div",{className:Ke.emptyConsole,children:["No messages yet. Use the ",p.jsx("strong",{children:"Start"})," button in the header to connect."]}),n[23]=s.length,n[24]=T):T=n[24];let O;n[25]!==y||n[26]!==v||n[27]!==T?(O=p.jsxs("div",{className:Ke.console,ref:y,role:"log","aria-live":"polite",children:[v,T]}),n[25]=y,n[26]=v,n[27]=T,n[28]=O):O=n[28];let k;return n[29]!==_||n[30]!==O?(k=p.jsxs("div",{className:Ke.consoleRoot,children:[_,O]}),n[29]=_,n[30]=O,n[31]=k):k=n[31],k}const u_="_commandInput_j85f1_2",d_="_labelRow_j85f1_8",p_="_labelGroup_j85f1_16",h_="_label_j85f1_8",f_="_infoWrapper_j85f1_28",m_="_paletteToggle_j85f1_34",g_="_paletteToggleActive_j85f1_66",y_="_popover_j85f1_73",b_="_popoverOpen_j85f1_95",v_="_popoverTitle_j85f1_121",__="_popoverRow_j85f1_135",x_="_popoverKeyword_j85f1_156",S_="_popoverDesc_j85f1_168",w_="_popoverAlias_j85f1_174",T_="_inputRow_j85f1_181",E_="_inputWrapper_j85f1_187",N_="_textarea_j85f1_197",A_="_sendButton_j85f1_226",C_="_hint_j85f1_243",j_="_dropup_j85f1_251",M_="_dropupHeader_j85f1_266",D_="_dropupItem_j85f1_282",k_="_dropupItemText_j85f1_305",O_="_matchHighlight_j85f1_313",R_="_multilineIndicator_j85f1_319",lt={commandInput:u_,labelRow:d_,labelGroup:p_,label:h_,infoWrapper:f_,paletteToggle:m_,paletteToggleActive:g_,popover:y_,popoverOpen:b_,popoverTitle:v_,popoverRow:__,popoverKeyword:x_,popoverDesc:S_,popoverAlias:w_,inputRow:T_,inputWrapper:E_,textarea:N_,sendButton:A_,hint:C_,dropup:j_,dropupHeader:M_,dropupItem:D_,dropupItemText:k_,matchHighlight:O_,multilineIndicator:R_},z_=[{keyword:"help",description:"List all help topics, or get help for a specific command",template:"help"},{keyword:"create",description:"Create a new graph node",template:`create node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:"update",description:"Update an existing node",template:`update node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:"edit",description:"Print raw node data ready for editing and re-submitting",template:"edit node {name}"},{keyword:"delete node",description:"Delete a node by name",alias:"clear node",template:"delete node {name}"},{keyword:"delete connection",description:"Delete connection(s) between two nodes",alias:"clear connection",template:"delete connection {nodeA} and {nodeB}"},{keyword:"delete cache",description:"Clear cached API fetcher results",alias:"clear cache",template:"delete cache"},{keyword:"connect",description:"Connect two nodes with a named relation",template:"connect {node-A} to {node-B} with {relation}"},{keyword:"list nodes",description:"List all nodes in the current graph",template:"list nodes"},{keyword:"list connections",description:"List all connections in the current graph",template:"list connections"},{keyword:"describe graph",description:"Describe the current graph model",template:"describe graph"},{keyword:"describe node",description:"Describe a specific node and its connections",template:"describe node {name}"},{keyword:"describe connection",description:"Describe connection(s) between two nodes",template:"describe connection {nodeA} and {nodeB}"},{keyword:"describe skill",description:"Show documentation for a skill by route name",template:"describe skill {skill.route}"},{keyword:"export",description:"Export the graph model to a JSON file",template:"export graph as {name}"},{keyword:"import graph",description:"Import a graph model from a saved file",template:"import graph from {name}"},{keyword:"import node",description:"Import a single node from another saved graph",template:"import node {node-name} from {graph-name}"},{keyword:"instantiate",description:"Create a runnable graph instance with mock input",alias:"start",template:`instantiate graph
{constant} -> input.body.{key}`,multiline:!0},{keyword:"upload mock data",description:"Print the URL to POST a JSON payload as mock input.body",template:"upload mock data"},{keyword:"execute",description:"Execute a single node skill in isolation",template:"execute node {name}"},{keyword:"inspect",description:"Inspect a state-machine variable",template:"inspect {variable_name}"},{keyword:"run",description:"Run the graph instance from root to end",template:"run"}];function B_(r,n){const s=Ee.c(22),[c,d]=w.useState(!1),[h,y]=w.useState(-1);let g;if(s[0]!==n||s[1]!==r){e:{const H=n.trimStart();if(H.length===0){let J;s[3]===Symbol.for("react.memo_cache_sentinel")?(J=[],s[3]=J):J=s[3],g=J;break e}const D=H.toLowerCase(),L=r.filter(J=>J.toLowerCase().startsWith(D)),C=new Set;g=L.filter(J=>C.has(J)?!1:(C.add(J),!0)).slice(0,Uy)}s[0]=n,s[1]=r,s[2]=g}else g=s[2];const f=g;let b;s[4]===Symbol.for("react.memo_cache_sentinel")?(b=()=>{d(!0),y(-1)},s[4]=b):b=s[4];const E=b;let x;s[5]!==f?(x=H=>{const D=f.length;D!==0&&y(L=>H===1?L<0?0:(L+1)%D:L<=0?D-1:L-1)},s[5]=f,s[6]=x):x=s[6];const S=x;let j;s[7]!==f?(j=(H,D)=>{H>=0&&H<f.length&&D(f[H]),d(!1),y(-1)},s[7]=f,s[8]=j):j=s[8];const A=j;let _;s[9]!==A||s[10]!==h||s[11]!==c||s[12]!==f?(_=H=>{if(!c||f.length===0)return;const D=h>=0?h:0;A(D,H)},s[9]=A,s[10]=h,s[11]=c,s[12]=f,s[13]=_):_=s[13];const v=_;let T;s[14]===Symbol.for("react.memo_cache_sentinel")?(T=()=>{d(!1),y(-1)},s[14]=T):T=s[14];const O=T;let k;return s[15]!==A||s[16]!==h||s[17]!==c||s[18]!==S||s[19]!==v||s[20]!==f?(k={suggestions:f,isOpen:c,activeIndex:h,onCommandChange:E,navigate:S,accept:A,onTab:v,dismiss:O},s[15]=A,s[16]=h,s[17]=c,s[18]=S,s[19]=v,s[20]=f,s[21]=k):k=s[21],k}const H_=r=>w.createElement("svg",{xmlns:"http://www.w3.org/2000/svg",viewBox:"0 0 16 16",fill:"none",width:14,height:14,stroke:"currentColor",strokeWidth:1.5,strokeLinecap:"round",strokeLinejoin:"round",...r},w.createElement("polyline",{points:"2,4 6,8 2,12"}),w.createElement("line",{x1:7,y1:12,x2:14,y2:12}));function G_(r){const n=Ee.c(70),{command:s,onChange:c,onKeyDown:d,onSend:h,sendDisabled:y,disabled:g,history:f}=r,b=w.useRef(null),E=w.useRef(null),x=w.useRef(null),[S,j]=w.useState(!1);let A,_;n[0]!==S?(A=()=>{if(!S)return;const F=ye=>{E.current&&!E.current.contains(ye.target)&&j(!1)};return document.addEventListener("mousedown",F),()=>document.removeEventListener("mousedown",F)},_=[S],n[0]=S,n[1]=A,n[2]=_):(A=n[1],_=n[2]),w.useEffect(A,_);const v=B_(f,s);let T;n[3]===Symbol.for("react.memo_cache_sentinel")?(T=()=>{const F=b.current;F&&(F.style.height="auto",F.style.height=`${F.scrollHeight}px`)},n[3]=T):T=n[3];let O;n[4]!==s?(O=[s],n[4]=s,n[5]=O):O=n[5],w.useEffect(T,O);const k=g?"Not connected":"Enter command (Enter to send · Shift+Enter for new line)",H=g?"Enter your test message once it is connected":"Enter to send · Shift+Enter for new line · ↑↓ for history";let D;n[6]!==v||n[7]!==c||n[8]!==d||n[9]!==h?(D=F=>{var ye,Se;if(F.key==="Tab"){F.preventDefault(),v.isOpen&&v.suggestions.length>0&&(v.onTab(he=>c(he)),requestAnimationFrame(()=>{const he=b.current;he&&(he.selectionStart=he.selectionEnd=he.value.length)}));return}if(F.key==="Enter"){if(F.shiftKey)return;if(F.preventDefault(),v.isOpen&&v.activeIndex>=0){v.accept(v.activeIndex,he=>c(he)),requestAnimationFrame(()=>{const he=b.current;he&&(he.selectionStart=he.selectionEnd=he.value.length)}),(ye=b.current)==null||ye.focus();return}h(),(Se=b.current)==null||Se.focus();return}if(F.key==="Escape"){if(v.isOpen){v.dismiss(),F.preventDefault();return}return}if(F.key==="ArrowUp"||F.key==="ArrowDown"){if(v.isOpen&&v.suggestions.length>0){F.preventDefault(),v.navigate(F.key==="ArrowDown"?1:-1);return}const he=b.current;if(he){const{selectionStart:we,value:Ne}=he,nt=!Ne.slice(0,we).includes(`
`),qe=!Ne.slice(we).includes(`
`);if(!(F.key==="ArrowUp"&&nt||F.key==="ArrowDown"&&qe))return}d(F),requestAnimationFrame(()=>{const we=b.current;we&&(we.selectionStart=we.selectionEnd=we.value.length)});return}d(F)},n[6]=v,n[7]=c,n[8]=d,n[9]=h,n[10]=D):D=n[10];const L=D;let C;n[11]===Symbol.for("react.memo_cache_sentinel")?(C=p.jsx("label",{htmlFor:"command",className:lt.label,children:"Command"}),n[11]=C):C=n[11];const Y=`${lt.paletteToggle}${S?` ${lt.paletteToggleActive}`:""}`;let J;n[12]===Symbol.for("react.memo_cache_sentinel")?(J=()=>j(Y_),n[12]=J):J=n[12];let I;n[13]!==S?(I=F=>{var ye;if(F.key==="ArrowDown"&&S){F.preventDefault();const Se=(ye=x.current)==null?void 0:ye.querySelector('[role="option"]');Se==null||Se.focus()}},n[13]=S,n[14]=I):I=n[14];let K;n[15]===Symbol.for("react.memo_cache_sentinel")?(K=p.jsx(H_,{"aria-hidden":"true",focusable:"false"}),n[15]=K):K=n[15];let Z;n[16]!==S||n[17]!==Y||n[18]!==I?(Z=p.jsx("button",{type:"button",className:Y,"aria-label":"Toggle command palette","aria-expanded":S,"aria-controls":"command-palette",onClick:J,onKeyDown:I,title:"Command palette",children:K}),n[16]=S,n[17]=Y,n[18]=I,n[19]=Z):Z=n[19];const ee=`${lt.popover}${S?` ${lt.popoverOpen}`:""}`;let P,le;n[20]===Symbol.for("react.memo_cache_sentinel")?(P=F=>{var ye,Se;if(F.key==="ArrowDown"||F.key==="ArrowUp"){F.preventDefault();const he=(ye=x.current)==null?void 0:ye.querySelectorAll('[role="option"]');if(!he||he.length===0)return;const we=Array.from(he).indexOf(document.activeElement);F.key==="ArrowDown"?he[we<0?0:(we+1)%he.length].focus():he[we<=0?he.length-1:we-1].focus()}else F.key==="Escape"&&(F.preventDefault(),j(!1),(Se=b.current)==null||Se.focus())},le=p.jsx("p",{className:lt.popoverTitle,children:"Command palette — click to insert"}),n[20]=P,n[21]=le):(P=n[20],le=n[21]);let R;n[22]!==S||n[23]!==c?(R=z_.map(F=>{const{keyword:ye,alias:Se,description:he,template:we}=F;return p.jsxs("div",{className:lt.popoverRow,role:"option","aria-selected":!1,tabIndex:S?0:-1,onMouseDown:L_,onClick:()=>{var Ne;c(we),j(!1),(Ne=b.current)==null||Ne.focus()},onKeyDown:Ne=>{var Le;(Ne.key==="Enter"||Ne.key===" ")&&(Ne.preventDefault(),c(we),j(!1),(Le=b.current)==null||Le.focus())},children:[p.jsx("span",{className:lt.popoverKeyword,children:ye}),p.jsxs("span",{className:lt.popoverDesc,children:[he,Se&&p.jsxs("span",{className:lt.popoverAlias,children:[" · alias: ",Se]})]})]},ye)}),n[22]=S,n[23]=c,n[24]=R):R=n[24];let z;n[25]!==ee||n[26]!==R?(z=p.jsxs("div",{id:"command-palette",ref:x,className:ee,role:"listbox","aria-label":"Command palette",onKeyDown:P,children:[le,R]}),n[25]=ee,n[26]=R,n[27]=z):z=n[27];let $;n[28]!==Z||n[29]!==z?($=p.jsx("div",{className:lt.labelRow,children:p.jsxs("div",{className:lt.labelGroup,children:[C,p.jsxs("span",{ref:E,className:lt.infoWrapper,children:[Z,z]})]})}),n[28]=Z,n[29]=z,n[30]=$):$=n[30];const ie=!(v.isOpen&&v.suggestions.length>0);let se;n[31]===Symbol.for("react.memo_cache_sentinel")?(se=p.jsx("div",{className:lt.dropupHeader,"aria-hidden":"true",children:"Recent Commands"}),n[31]=se):se=n[31];let pe;n[32]!==v||n[33]!==s||n[34]!==c?(pe=v.isOpen&&v.suggestions.length>0&&v.suggestions.map((F,ye)=>{const Se=F.split(`
`)[0],he=F.includes(`
`),we=s.trimStart().split(`
`)[0],Ne=Math.min(we.length,Se.length),Le=Se.slice(0,Ne),nt=Se.slice(Ne);return p.jsxs("div",{id:`history-option-${ye}`,role:"option","aria-selected":ye===v.activeIndex,className:lt.dropupItem,onMouseDown:U_,onClick:()=>{v.accept(ye,qe=>c(qe)),requestAnimationFrame(()=>{const qe=b.current;qe&&(qe.selectionStart=qe.selectionEnd=qe.value.length)})},children:[p.jsxs("span",{className:lt.dropupItemText,children:[Ne>0&&p.jsx("strong",{className:lt.matchHighlight,children:Le}),nt,he?"…":""]}),he&&p.jsx("span",{className:lt.multilineIndicator,"aria-label":"multi-line command",children:"↵"})]},F)}),n[32]=v,n[33]=s,n[34]=c,n[35]=pe):pe=n[35];let ae;n[36]!==ie||n[37]!==pe?(ae=p.jsxs("div",{id:"history-dropup",role:"listbox","aria-label":"Command history suggestions",className:lt.dropup,hidden:ie,children:[se,pe]}),n[36]=ie,n[37]=pe,n[38]=ae):ae=n[38];const te=v.isOpen&&v.suggestions.length>0,ue=v.isOpen&&v.suggestions.length>0&&v.activeIndex>=0?`history-option-${v.activeIndex}`:void 0;let re;n[39]!==v||n[40]!==c?(re=F=>{c(F.target.value),v.onCommandChange()},n[39]=v,n[40]=c,n[41]=re):re=n[41];let me;n[42]!==v?(me=()=>v.dismiss(),n[42]=v,n[43]=me):me=n[43];let Ce;n[44]!==s||n[45]!==g||n[46]!==L||n[47]!==k||n[48]!==te||n[49]!==ue||n[50]!==re||n[51]!==me?(Ce=p.jsx("textarea",{ref:b,id:"command",role:"combobox","aria-expanded":te,"aria-haspopup":"listbox","aria-controls":"history-dropup","aria-activedescendant":ue,"aria-autocomplete":"list",className:lt.textarea,rows:1,placeholder:k,value:s,disabled:g,onChange:re,onKeyDown:L,onBlur:me,autoComplete:"off",autoCorrect:"off",spellCheck:!1}),n[44]=s,n[45]=g,n[46]=L,n[47]=k,n[48]=te,n[49]=ue,n[50]=re,n[51]=me,n[52]=Ce):Ce=n[52];let Oe;n[53]!==ae||n[54]!==Ce?(Oe=p.jsxs("div",{className:lt.inputWrapper,children:[ae,Ce]}),n[53]=ae,n[54]=Ce,n[55]=Oe):Oe=n[55];let _e;n[56]!==h?(_e=()=>{var F;h(),(F=b.current)==null||F.focus()},n[56]=h,n[57]=_e):_e=n[57];let Re;n[58]!==y||n[59]!==_e?(Re=p.jsx("button",{className:lt.sendButton,onClick:_e,disabled:y,"aria-label":"Send command",children:"Send"}),n[58]=y,n[59]=_e,n[60]=Re):Re=n[60];let ne;n[61]!==Oe||n[62]!==Re?(ne=p.jsxs("div",{className:lt.inputRow,children:[Oe,Re]}),n[61]=Oe,n[62]=Re,n[63]=ne):ne=n[63];let ge;n[64]!==H?(ge=p.jsx("p",{className:lt.hint,children:H}),n[64]=H,n[65]=ge):ge=n[65];let xe;return n[66]!==$||n[67]!==ne||n[68]!==ge?(xe=p.jsxs("div",{className:lt.commandInput,children:[$,ne,ge]}),n[66]=$,n[67]=ne,n[68]=ge,n[69]=xe):xe=n[69],xe}function U_(r){return r.preventDefault()}function L_(r){return r.preventDefault()}function Y_(r){return!r}const q_="_root_1ac49_1",I_={root:q_};function X_(r){const n=Ee.c(22),{messages:s,classificationMap:c,onCopy:d,onClear:h,consoleRef:y,onGraphLinkMessage:g,onCopyMessage:f,onSendToJsonPath:b,onUploadMockData:E,successfulUploadPaths:x,command:S,onCommandChange:j,onCommandKeyDown:A,onSend:_,sendDisabled:v,inputDisabled:T,commandHistory:O}=r;let k;n[0]!==c||n[1]!==y||n[2]!==s||n[3]!==h||n[4]!==d||n[5]!==f||n[6]!==g||n[7]!==b||n[8]!==E||n[9]!==x?(k=p.jsx(c_,{messages:s,classificationMap:c,onCopy:d,onClear:h,consoleRef:y,onGraphLinkMessage:g,onCopyMessage:f,onSendToJsonPath:b,onUploadMockData:E,successfulUploadPaths:x}),n[0]=c,n[1]=y,n[2]=s,n[3]=h,n[4]=d,n[5]=f,n[6]=g,n[7]=b,n[8]=E,n[9]=x,n[10]=k):k=n[10];let H;n[11]!==S||n[12]!==O||n[13]!==T||n[14]!==j||n[15]!==A||n[16]!==_||n[17]!==v?(H=p.jsx(G_,{command:S,onChange:j,onKeyDown:A,onSend:_,disabled:T,sendDisabled:v,history:O}),n[11]=S,n[12]=O,n[13]=T,n[14]=j,n[15]=A,n[16]=_,n[17]=v,n[18]=H):H=n[18];let D;return n[19]!==k||n[20]!==H?(D=p.jsxs("div",{className:I_.root,children:[k,H]}),n[19]=k,n[20]=H,n[21]=D):D=n[21],D}const J_="_dialog_g80bk_4",Z_="_modalInner_g80bk_26",V_="_modalHeader_g80bk_34",Q_="_modalTitleGroup_g80bk_44",K_="_modalTitle_g80bk_44",$_="_modalPath_g80bk_57",W_="_closeButton_g80bk_64",P_="_modalBody_g80bk_95",F_="_dropZone_g80bk_105",ex="_dropZoneActive_g80bk_127",tx="_dropZoneIcon_g80bk_133",nx="_dropZoneText_g80bk_139",ax="_dropZoneOr_g80bk_152",ox="_browseButton_g80bk_159",lx="_fileInputHidden_g80bk_188",ix="_fileError_g80bk_193",sx="_textareaLabel_g80bk_198",rx="_textarea_g80bk_198",cx="_validationError_g80bk_226",ux="_keyboardHint_g80bk_231",dx="_errorBanner_g80bk_236",px="_modalFooter_g80bk_247",hx="_footerActions_g80bk_257",fx="_formatButton_g80bk_263",mx="_cancelButton_g80bk_264",gx="_uploadButton_g80bk_265",yx="_spinner_g80bk_332",tt={dialog:J_,modalInner:Z_,modalHeader:V_,modalTitleGroup:Q_,modalTitle:K_,modalPath:$_,closeButton:W_,modalBody:P_,dropZone:F_,dropZoneActive:ex,dropZoneIcon:tx,dropZoneText:nx,dropZoneOr:ax,browseButton:ox,fileInputHidden:lx,fileError:ix,textareaLabel:sx,textarea:rx,validationError:cx,keyboardHint:ux,errorBanner:dx,modalFooter:px,footerActions:hx,formatButton:fx,cancelButton:mx,uploadButton:gx,spinner:yx};function bx(r){const n=Ee.c(9),{uploadPath:s,json:c,onSuccess:d,onError:h}=r,[y,g]=w.useState(!1),f=w.useRef(null);let b;n[0]===Symbol.for("react.memo_cache_sentinel")?(b=()=>{var A;(A=f.current)==null||A.abort(),f.current=null,g(!1)},n[0]=b):b=n[0];const E=b;let x;n[1]!==c||n[2]!==h||n[3]!==d||n[4]!==s?(x=async()=>{var _;(_=f.current)==null||_.abort();const A=new AbortController;f.current=A,g(!0);try{const v=await fetch(s,{method:"POST",headers:{"Content-Type":"application/json"},body:c,signal:A.signal}),T=await v.text();if(!v.ok){g(!1),h(`HTTP ${v.status} — ${T}`);return}g(!1),d(T)}catch(v){const T=v;if(T.name==="AbortError"){g(!1);return}g(!1),h(T.message??"Network error")}},n[1]=c,n[2]=h,n[3]=d,n[4]=s,n[5]=x):x=n[5];const S=x;let j;return n[6]!==y||n[7]!==S?(j={isUploading:y,upload:S,cancel:E},n[6]=y,n[7]=S,n[8]=j):j=n[8],j}var ff;const vx=(((ff=navigator.userAgentData)==null?void 0:ff.platform)??navigator.platform).toLowerCase().includes("mac");function _x(r){return new Promise((n,s)=>{const c=new FileReader;c.onload=()=>n(c.result),c.onerror=()=>s(new Error(`Could not read file "${r.name}"`)),c.readAsText(r,"utf-8")})}function xx(r){const n=r.name.toLowerCase().endsWith(".json"),s=r.type==="application/json"||r.type==="text/plain";return!n&&!s?`"${r.name}" does not appear to be a JSON file. Only .json files are accepted.`:null}function Sx({uploadPath:r,onSuccess:n,onClose:s,onError:c}){const[d,h]=w.useState(""),[y,g]=w.useState(null),[f,b]=w.useState(null),[E,x]=w.useState(!1),S=w.useRef(null),j=w.useRef(null),A=w.useRef(null),v=Nl(d).isJSON,T=v&&d.trim()!=="",{isUploading:O,upload:k,cancel:H}=bx({uploadPath:r,json:d,onSuccess:n,onError:z=>{g(z),c(z)}});w.useEffect(()=>{var $;const z=S.current;if(z)return z.open||z.showModal(),($=j.current)==null||$.focus(),()=>{z.open&&z.close()}},[]);const D=w.useCallback(()=>{H(),s()},[H,s]),L=w.useCallback(z=>{z.target===S.current&&D()},[D]),C=w.useCallback(z=>{z.preventDefault(),D()},[D]),Y=w.useCallback(()=>{g(null),k()},[k]),J=w.useCallback(z=>{z.key==="Enter"&&(z.ctrlKey||z.metaKey)&&(z.preventDefault(),T&&!O&&Y())},[T,O,Y]),I=w.useCallback(()=>{v&&h(_c(d))},[v,d]),K=w.useCallback(async z=>{var ie;b(null),g(null);const $=xx(z);if($){b($);return}try{const se=await _x(z);if(!Nl(se).isJSON){b(`"${z.name}" contains invalid JSON.`);return}h(_c(se)),(ie=j.current)==null||ie.focus()}catch(se){b(se.message)}},[]),Z=w.useCallback(z=>{z.preventDefault(),z.stopPropagation(),E||x(!0)},[E]),ee=w.useCallback(z=>{z.preventDefault(),z.stopPropagation(),(z.currentTarget===z.target||!z.currentTarget.contains(z.relatedTarget))&&x(!1)},[]),P=w.useCallback(z=>{z.preventDefault(),z.stopPropagation(),x(!1);const $=z.dataTransfer.files[0];$&&K($)},[K]),le=w.useCallback(z=>{var ie;const $=(ie=z.target.files)==null?void 0:ie[0];$&&(K($),z.target.value="")},[K]),R=!v&&d.trim()!=="";return p.jsx("dialog",{ref:S,className:tt.dialog,"aria-modal":"true","aria-labelledby":"mock-upload-modal-title",onClick:L,onCancel:C,children:p.jsxs("div",{className:tt.modalInner,onClick:z=>z.stopPropagation(),children:[p.jsxs("div",{className:tt.modalHeader,children:[p.jsxs("div",{className:tt.modalTitleGroup,children:[p.jsx("span",{id:"mock-upload-modal-title",className:tt.modalTitle,children:"⬆️ Upload Mock Data"}),p.jsx("span",{className:tt.modalPath,children:r})]}),p.jsx("button",{className:tt.closeButton,onClick:D,"aria-label":"Close upload modal",title:"Close",disabled:O,children:"✕"})]}),p.jsxs("div",{className:tt.modalBody,children:[p.jsxs("div",{className:`${tt.dropZone} ${E?tt.dropZoneActive:""}`,onDragOver:Z,onDragLeave:ee,onDrop:P,"aria-label":"Drop a JSON file here",children:[p.jsx("span",{className:tt.dropZoneIcon,children:"📂"}),p.jsxs("span",{className:tt.dropZoneText,children:["Drop a ",p.jsx("code",{children:".json"})," file here"]}),p.jsx("span",{className:tt.dropZoneOr,children:"— or —"}),p.jsx("input",{ref:A,type:"file",accept:".json,application/json",className:tt.fileInputHidden,"aria-hidden":"true",tabIndex:-1,onChange:le}),p.jsx("button",{type:"button",className:tt.browseButton,onClick:()=>{var z;return(z=A.current)==null?void 0:z.click()},disabled:O,"aria-label":"Browse for a JSON file",children:"Browse file…"})]}),f&&p.jsxs("span",{className:tt.fileError,role:"alert",children:["⚠️ ",f]}),p.jsx("label",{htmlFor:"mock-upload-textarea",className:tt.textareaLabel,children:"JSON Payload"}),p.jsx("textarea",{id:"mock-upload-textarea",ref:j,className:tt.textarea,value:d,onChange:z=>{h(z.target.value),b(null)},onKeyDown:J,placeholder:"Paste JSON here, or drop / browse a .json file above",rows:10,spellCheck:!1,"aria-describedby":R?"mock-upload-validation":void 0}),R&&p.jsx("span",{id:"mock-upload-validation",className:tt.validationError,role:"status",children:"⚠️ Invalid JSON — check syntax"}),p.jsx("span",{className:tt.keyboardHint,children:vx?"⌘+Enter to upload":"Ctrl+Enter to upload"}),y&&p.jsxs("div",{className:tt.errorBanner,role:"alert",children:["❌ Upload failed: ",y]})]}),p.jsxs("div",{className:tt.modalFooter,children:[p.jsx("button",{className:tt.formatButton,onClick:I,disabled:!v||O,title:"Format JSON","aria-label":"Format JSON",children:"Format"}),p.jsxs("div",{className:tt.footerActions,children:[p.jsx("button",{className:tt.cancelButton,onClick:D,disabled:O,children:"Cancel"}),p.jsx("button",{className:tt.uploadButton,onClick:Y,disabled:!T||O,"aria-busy":O,children:O?p.jsxs(p.Fragment,{children:[p.jsx("span",{className:tt.spinner,"aria-hidden":"true"})," Uploading…"]}):"Upload ▶"})]})]})]})})}let tf=0;function Nc(r="",n=""){return tf+=1,{id:`property-row-${tf}`,key:r,value:n}}function wx(r){return{alias:r==="empty-graph"?"root":"",nodeType:r==="empty-graph"?"Root":"",properties:[Nc()],source:r}}const fc=/^[A-Za-z0-9_-]+$/,Tx=new Set(["input","output","model","response","result","parameter","none","next","api","error"]);function Mo(r,n){return`properties.${r}.${n}`}function jf(r,n={}){var h;const s={},c=r.alias.trim(),d=r.nodeType.trim();c?fc.test(c)?Tx.has(c.toLowerCase())?s.alias=`"${c}" is reserved.`:(h=n.graphData)!=null&&h.nodes.some(y=>y.alias.toLowerCase()===c.toLowerCase())&&(s.alias=`Node "${c}" already exists in the current graph.`):s.alias="Use only letters, numbers, underscore, and hyphen.":s.alias="Alias is required.",d&&!fc.test(d)&&(s.nodeType="Use only letters, numbers, underscore, and hyphen.");for(const y of r.properties){const g=y.key.trim(),f=y.value.trim();!g&&!f||(!g&&f?s[Mo(y.id,"key")]="Property key is required when value is present.":fc.test(g)||(s[Mo(y.id,"key")]="Use only letters, numbers, underscore, and hyphen."),f.includes("\r")||f.includes(`
`)?s[Mo(y.id,"value")]="Property value must be a single line.":f.includes("'''")&&(s[Mo(y.id,"value")]="Property value cannot contain '''."))}return{valid:Object.keys(s).length===0,errors:s}}function Ex(r){return r.length<=Ly?{valid:!0,errors:{}}:{valid:!1,errors:{command:"The node command is too large. Shorten property values before submitting."}}}const Ac=r=>w.createElement("svg",{xmlns:"http://www.w3.org/2000/svg",viewBox:"0 0 16 16",fill:"none",width:16,height:16,stroke:"currentColor",strokeWidth:1.8,strokeLinecap:"round",strokeLinejoin:"round",...r},w.createElement("line",{x1:4.75,y1:4.75,x2:11.25,y2:11.25}),w.createElement("line",{x1:11.25,y1:4.75,x2:4.75,y2:11.25})),Nx="_overlay_1n8dp_1",Ax="_panel_1n8dp_21",Cx="_form_1n8dp_34",jx="_header_1n8dp_41",Mx="_title_1n8dp_50",Dx="_iconButton_1n8dp_57",kx="_removeButton_1n8dp_58",Ox="_buttonIcon_1n8dp_93",Rx="_body_1n8dp_151",zx="_field_1n8dp_161",Bx="_propertyField_1n8dp_162",Hx="_label_1n8dp_169",Gx="_input_1n8dp_176",Ux="_properties_1n8dp_205",Lx="_propertiesHeader_1n8dp_221",Yx="_sectionTitle_1n8dp_228",qx="_propertyRows_1n8dp_234",Ix="_propertyActions_1n8dp_240",Xx="_propertyRow_1n8dp_234",Jx="_message_1n8dp_258",Zx="_warningMessage_1n8dp_259",Vx="_errorMessage_1n8dp_260",Qx="_errorText_1n8dp_285",Kx="_footer_1n8dp_290",$x="_primaryButton_1n8dp_300",Wx="_secondaryButton_1n8dp_301",Px="_addPropertyButton_1n8dp_333",ke={overlay:Nx,panel:Ax,form:Cx,header:jx,title:Mx,iconButton:Dx,removeButton:kx,buttonIcon:Ox,body:Rx,field:zx,propertyField:Bx,label:Hx,input:Gx,properties:Ux,propertiesHeader:Lx,sectionTitle:Yx,propertyRows:qx,propertyActions:Ix,propertyRow:Xx,message:Jx,warningMessage:Zx,errorMessage:Vx,errorText:Qx,footer:Kx,primaryButton:$x,secondaryButton:Wx,addPropertyButton:Px};function Fx(r){const n=Ee.c(111),{open:s,draft:c,phase:d,lockReason:h,serverMessage:y,validationErrors:g,onDraftChange:f,onSubmit:b,onClose:E}=r,x=w.useRef(null);let S;n[0]===Symbol.for("react.memo_cache_sentinel")?(S=new Map,n[0]=S):S=n[0];const j=w.useRef(S),A=w.useRef(null),_=d==="sending",v=h==="disconnected",T=_||v;let O,k;n[1]!==E||n[2]!==s||n[3]!==_?(O=()=>{var ct;if(!s)return;(ct=x.current)==null||ct.focus();const Te=oe=>{oe.key==="Escape"&&(oe.preventDefault(),_||E())};return document.addEventListener("keydown",Te),()=>{document.removeEventListener("keydown",Te)}},k=[E,s,_],n[1]=E,n[2]=s,n[3]=_,n[4]=O,n[5]=k):(O=n[4],k=n[5]),w.useEffect(O,k);let H;n[6]===Symbol.for("react.memo_cache_sentinel")?(H=()=>{const Te=A.current;if(!Te)return;const ct=j.current.get(Te);ct&&(ct.focus(),A.current=null)},n[6]=H):H=n[6];let D;n[7]!==c.properties?(D=[c.properties],n[7]=c.properties,n[8]=D):D=n[8],w.useEffect(H,D);const L=n3;let C;n[9]!==E||n[10]!==_?(C=Te=>{Te.preventDefault(),Te.stopPropagation(),_||E()},n[9]=E,n[10]=_,n[11]=C):C=n[11];const Y=C,J=t3;let I;n[12]!==T||n[13]!==b?(I=Te=>{Te.preventDefault(),!T&&b()},n[12]=T,n[13]=b,n[14]=I):I=n[14];const K=I;let Z;n[15]!==c||n[16]!==f?(Z=Te=>{f({...c,...Te})},n[15]=c,n[16]=f,n[17]=Z):Z=n[17];const ee=Z;let P;n[18]!==c||n[19]!==f?(P=(Te,ct)=>{f({...c,properties:c.properties.map(oe=>oe.id===Te?{...oe,...ct}:oe)})},n[18]=c,n[19]=f,n[20]=P):P=n[20];const le=P;let R;n[21]!==c||n[22]!==f?(R=()=>{const Te=Nc();A.current=Te.id,f({...c,properties:[...c.properties,Te]})},n[21]=c,n[22]=f,n[23]=R):R=n[23];const z=R;let $;n[24]!==c||n[25]!==f?($=Te=>{const ct=c.properties.filter(oe=>oe.id!==Te);f({...c,properties:ct.length>0?ct:[Nc()]})},n[24]=c,n[25]=f,n[26]=$):$=n[26];const ie=$;if(!s)return null;let se;n[27]===Symbol.for("react.memo_cache_sentinel")?(se=p.jsx("div",{children:p.jsx("h2",{id:"node-dialog-title",className:ke.title,children:"Create Node"})}),n[27]=se):se=n[27];let pe;n[28]===Symbol.for("react.memo_cache_sentinel")?(pe=p.jsx(Ac,{className:ke.buttonIcon,"aria-hidden":"true",focusable:"false"}),n[28]=pe):pe=n[28];let ae;n[29]!==E||n[30]!==_?(ae=p.jsxs("header",{className:ke.header,children:[se,p.jsx("button",{type:"button",className:ke.iconButton,"aria-label":"Close create node dialog",onClick:E,disabled:_,children:pe})]}),n[29]=E,n[30]=_,n[31]=ae):ae=n[31];let te;n[32]!==v||n[33]!==y?(te=y&&!v&&p.jsx("div",{className:ke.message,role:"status",children:y}),n[32]=v,n[33]=y,n[34]=te):te=n[34];let ue;n[35]!==g.command?(ue=g.command&&p.jsx("div",{className:ke.errorMessage,role:"alert",children:g.command}),n[35]=g.command,n[36]=ue):ue=n[36];let re;n[37]!==v||n[38]!==y?(re=v&&p.jsx("div",{className:ke.warningMessage,role:"status",children:y??"Connection disconnected. Refresh the page and create the node again after the app reconnects."}),n[37]=v,n[38]=y,n[39]=re):re=n[39];let me;n[40]===Symbol.for("react.memo_cache_sentinel")?(me=p.jsx("span",{className:ke.label,children:"Alias"}),n[40]=me):me=n[40];const Ce=c.alias,Oe=!!g.alias,_e=g.alias?"node-alias-error":void 0;let Re;n[41]!==ee?(Re=Te=>ee({alias:Te.target.value}),n[41]=ee,n[42]=Re):Re=n[42];let ne;n[43]!==T||n[44]!==c.alias||n[45]!==Oe||n[46]!==_e||n[47]!==Re?(ne=p.jsx("input",{ref:x,className:ke.input,value:Ce,disabled:T,"aria-invalid":Oe,"aria-describedby":_e,onChange:Re}),n[43]=T,n[44]=c.alias,n[45]=Oe,n[46]=_e,n[47]=Re,n[48]=ne):ne=n[48];let ge;n[49]!==g.alias?(ge=g.alias&&p.jsx("span",{id:"node-alias-error",className:ke.errorText,children:g.alias}),n[49]=g.alias,n[50]=ge):ge=n[50];let xe;n[51]!==ne||n[52]!==ge?(xe=p.jsxs("label",{className:ke.field,children:[me,ne,ge]}),n[51]=ne,n[52]=ge,n[53]=xe):xe=n[53];let F;n[54]===Symbol.for("react.memo_cache_sentinel")?(F=p.jsx("span",{className:ke.label,children:"Node Type"}),n[54]=F):F=n[54];const ye=c.nodeType,Se=!!g.nodeType,he=g.nodeType?"node-type-error":void 0;let we;n[55]!==ee?(we=Te=>ee({nodeType:Te.target.value}),n[55]=ee,n[56]=we):we=n[56];let Ne;n[57]!==T||n[58]!==c.nodeType||n[59]!==Se||n[60]!==he||n[61]!==we?(Ne=p.jsx("input",{className:ke.input,value:ye,disabled:T,"aria-invalid":Se,"aria-describedby":he,onChange:we}),n[57]=T,n[58]=c.nodeType,n[59]=Se,n[60]=he,n[61]=we,n[62]=Ne):Ne=n[62];let Le;n[63]!==g.nodeType?(Le=g.nodeType&&p.jsx("span",{id:"node-type-error",className:ke.errorText,children:g.nodeType}),n[63]=g.nodeType,n[64]=Le):Le=n[64];let nt;n[65]!==Ne||n[66]!==Le?(nt=p.jsxs("label",{className:ke.field,children:[F,Ne,Le]}),n[65]=Ne,n[66]=Le,n[67]=nt):nt=n[67];let qe;n[68]===Symbol.for("react.memo_cache_sentinel")?(qe=p.jsx("div",{className:ke.propertiesHeader,children:p.jsx("h3",{id:"node-properties-title",className:ke.sectionTitle,children:"Properties"})}),n[68]=qe):qe=n[68];let vt;n[69]!==T||n[70]!==c.properties||n[71]!==ie||n[72]!==le||n[73]!==g?(vt=c.properties.map(Te=>{const ct=g[Mo(Te.id,"key")],oe=g[Mo(Te.id,"value")];return p.jsxs("div",{className:ke.propertyRow,children:[p.jsxs("label",{className:ke.propertyField,children:[p.jsx("span",{className:ke.label,children:"Key"}),p.jsx("input",{ref:Me=>{Me?j.current.set(Te.id,Me):j.current.delete(Te.id)},className:ke.input,value:Te.key,disabled:T,"aria-invalid":!!ct,onChange:Me=>le(Te.id,{key:Me.target.value})}),ct&&p.jsx("span",{className:ke.errorText,children:ct})]}),p.jsxs("label",{className:ke.propertyField,children:[p.jsx("span",{className:ke.label,children:"Value"}),p.jsx("input",{className:ke.input,value:Te.value,disabled:T,"aria-invalid":!!oe,onChange:Me=>le(Te.id,{value:Me.target.value})}),oe&&p.jsx("span",{className:ke.errorText,children:oe})]}),p.jsx("button",{type:"button",className:ke.removeButton,"aria-label":"Remove property",disabled:T,onClick:()=>ie(Te.id),children:p.jsx(Ac,{className:ke.buttonIcon,"aria-hidden":"true",focusable:"false"})})]},Te.id)}),n[69]=T,n[70]=c.properties,n[71]=ie,n[72]=le,n[73]=g,n[74]=vt):vt=n[74];let fe;n[75]!==vt?(fe=p.jsx("div",{className:ke.propertyRows,children:vt}),n[75]=vt,n[76]=fe):fe=n[76];let Mt,Ht;n[77]===Symbol.for("react.memo_cache_sentinel")?(Mt=p.jsx("span",{"aria-hidden":"true",children:"+"}),Ht=p.jsx("span",{children:"Add Property"}),n[77]=Mt,n[78]=Ht):(Mt=n[77],Ht=n[78]);let ut;n[79]!==z||n[80]!==T?(ut=p.jsx("div",{className:ke.propertyActions,children:p.jsxs("button",{type:"button",className:`${ke.secondaryButton} ${ke.addPropertyButton}`,disabled:T,onClick:z,children:[Mt,Ht]})}),n[79]=z,n[80]=T,n[81]=ut):ut=n[81];let ht;n[82]!==fe||n[83]!==ut?(ht=p.jsxs("section",{className:ke.properties,"aria-labelledby":"node-properties-title",children:[qe,fe,ut]}),n[82]=fe,n[83]=ut,n[84]=ht):ht=n[84];let qt;n[85]!==te||n[86]!==ue||n[87]!==re||n[88]!==xe||n[89]!==nt||n[90]!==ht?(qt=p.jsxs("div",{className:ke.body,children:[te,ue,re,xe,nt,ht]}),n[85]=te,n[86]=ue,n[87]=re,n[88]=xe,n[89]=nt,n[90]=ht,n[91]=qt):qt=n[91];let Gt;n[92]!==E||n[93]!==_?(Gt=p.jsx("button",{type:"button",className:ke.secondaryButton,onClick:E,disabled:_,children:"Cancel"}),n[92]=E,n[93]=_,n[94]=Gt):Gt=n[94];const Sn=_?"Creating...":"Create Node";let Dt;n[95]!==T||n[96]!==Sn?(Dt=p.jsx("button",{type:"submit",className:ke.primaryButton,disabled:T,children:Sn}),n[95]=T,n[96]=Sn,n[97]=Dt):Dt=n[97];let Tt;n[98]!==Gt||n[99]!==Dt?(Tt=p.jsxs("footer",{className:ke.footer,children:[Gt,Dt]}),n[98]=Gt,n[99]=Dt,n[100]=Tt):Tt=n[100];let Ye;n[101]!==K||n[102]!==qt||n[103]!==Tt?(Ye=p.jsxs("form",{className:ke.form,onSubmit:K,children:[qt,Tt]}),n[101]=K,n[102]=qt,n[103]=Tt,n[104]=Ye):Ye=n[104];let it;n[105]!==ae||n[106]!==Ye?(it=p.jsxs("div",{className:ke.panel,role:"dialog","aria-modal":"true","aria-labelledby":"node-dialog-title",onPointerDown:J,onClick:e3,children:[ae,Ye]}),n[105]=ae,n[106]=Ye,n[107]=it):it=n[107];let Fe;return n[108]!==Y||n[109]!==it?(Fe=p.jsx("div",{className:ke.overlay,onPointerDown:L,onClick:Y,children:it}),n[108]=Y,n[109]=it,n[110]=Fe):Fe=n[110],Fe}function e3(r){return r.stopPropagation()}function t3(r){r.stopPropagation()}function n3(r){r.preventDefault(),r.stopPropagation()}function a3(r){const n=Ee.c(9),{state:s,validationErrors:c,onDraftChange:d,onSubmit:h,onClose:y}=r;if(s.status==="closed")return null;const g=s.phase==="sending"?"sending":s.connectionLost?"disconnected":null;let f;return n[0]!==g||n[1]!==y||n[2]!==d||n[3]!==h||n[4]!==s.draft||n[5]!==s.phase||n[6]!==s.serverMessage||n[7]!==c?(f=p.jsx(Fx,{open:!0,draft:s.draft,phase:s.phase,lockReason:g,serverMessage:s.serverMessage,validationErrors:c,onDraftChange:d,onSubmit:h,onClose:y}),n[0]=g,n[1]=y,n[2]=d,n[3]=h,n[4]=s.draft,n[5]=s.phase,n[6]=s.serverMessage,n[7]=c,n[8]=f):f=n[8],f}function o3(r){const n=jf(r);if(!n.valid)throw new Error(Object.values(n.errors)[0]??"Invalid node draft.");const s=r.alias.trim(),c=r.nodeType.trim(),d=r.properties.map(f=>({key:f.key.trim(),value:f.value.trim()})).filter(f=>f.key||f.value),h=[`create node ${s}`];if(c&&h.push(`with type ${c}`),d.length>0){h.push("with properties");for(const f of d)h.push(`${f.key}=${f.value}`)}const y=h.join(`
`),g=Ex(y);if(!g.valid)throw new Error(g.errors.command);return y}const l3=1e4,nf="Could not send the create-node command because the WebSocket is not open. The draft was preserved in this dialog.",i3="The create-node command was sent, but no backend result was observed yet. The outcome is unknown.",s3="Connection disconnected. This graph session may no longer be valid. Refresh the page and create the node again after the app reconnects.",r3="Connection disconnected while the create-node command was pending. The outcome is unknown. Refresh the page and check the graph before trying again.";function c3(r){const n=Ee.c(36),{bus:s,connected:c,graphData:d,executor:h,timeoutMs:y,onAccepted:g}=r,f=y===void 0?l3:y;let b;n[0]===Symbol.for("react.memo_cache_sentinel")?(b={status:"closed"},n[0]=b):b=n[0];const[E,x]=w.useState(b);let S;n[1]===Symbol.for("react.memo_cache_sentinel")?(S={},n[1]=S):S=n[1];const[j,A]=w.useState(S),_=w.useRef(E),v=w.useRef(null),T=w.useRef(c),O=w.useRef(d),k=w.useRef(g);let H,D;n[2]!==E?(H=()=>{_.current=E},D=[E],n[2]=E,n[3]=H,n[4]=D):(H=n[3],D=n[4]),w.useEffect(H,D);let L,C;n[5]!==d?(L=()=>{O.current=d},C=[d],n[5]=d,n[6]=L,n[7]=C):(L=n[6],C=n[7]),w.useEffect(L,C);let Y,J;n[8]!==g?(Y=()=>{k.current=g},J=[g],n[8]=g,n[9]=Y,n[10]=J):(Y=n[9],J=n[10]),w.useEffect(Y,J);let I;n[11]===Symbol.for("react.memo_cache_sentinel")?(I=ne=>{_.current=ne,x(ne)},n[11]=I):I=n[11];const K=I;let Z;n[12]===Symbol.for("react.memo_cache_sentinel")?(Z=()=>{v.current!==null&&(clearTimeout(v.current),v.current=null)},n[12]=Z):Z=n[12];const ee=Z;let P;n[13]!==c?(P=ne=>{if(!c)return;const ge=wx(ne);A({}),K({status:"open",action:"create-node",phase:"editing",draft:ge,pendingSubmit:null,serverMessage:null,connectionLost:!1})},n[13]=c,n[14]=P):P=n[14];const le=P;let R;n[15]===Symbol.for("react.memo_cache_sentinel")?(R=ne=>{const ge=_.current;ge.status==="open"&&(ge.phase==="sending"||ge.connectionLost||(A({}),K({...ge,draft:ne,pendingSubmit:null,serverMessage:null,connectionLost:!1})))},n[15]=R):R=n[15];const z=R;let $;n[16]!==f?($=()=>{ee(),v.current=setTimeout(()=>{const ne=_.current;ne.status!=="open"||ne.phase!=="sending"||!ne.pendingSubmit||(K({...ne,phase:"editing",serverMessage:i3}),v.current=null)},f)},n[16]=f,n[17]=$):$=n[17];const ie=$;let se;n[18]!==c||n[19]!==h||n[20]!==ie?(se=()=>{const ne=_.current;if(ne.status!=="open"||ne.phase==="sending"||ne.connectionLost)return;if(!c){K({...ne,serverMessage:nf});return}const ge=jf(ne.draft,{graphData:O.current});if(!ge.valid){A(ge.errors);return}let xe;try{xe=o3(ne.draft)}catch(Se){const he=Se;A({command:he instanceof Error?he.message:String(he)});return}if(!h.execute(xe)){K({...ne,phase:"editing",pendingSubmit:null,serverMessage:nf});return}const ye={alias:ne.draft.alias.trim(),command:xe,sentAt:new Date().toISOString()};A({}),K({...ne,phase:"sending",pendingSubmit:ye,serverMessage:null,connectionLost:!1}),ie()},n[18]=c,n[19]=h,n[20]=ie,n[21]=se):se=n[21];const pe=se;let ae;n[22]===Symbol.for("react.memo_cache_sentinel")?(ae=()=>{const ne=_.current;ne.status==="open"&&ne.phase!=="sending"&&(ee(),A({}),K({status:"closed"}))},n[22]=ae):ae=n[22];const te=ae;let ue,re;n[23]!==s?(ue=()=>s.on("minigraph.createNode.textResult",ne=>{var F;const ge=_.current;if(ge.status!=="open"||!ge.pendingSubmit)return;const xe=ge.pendingSubmit;if(ne.status==="accepted"&&ne.alias===xe.alias){ee(),A({}),K({status:"closed"}),(F=k.current)==null||F.call(k,{status:ne.status,alias:ne.alias,message:ne.message});return}if(ne.status==="rejected"&&ne.alias===xe.alias){ee(),K({...ge,phase:"editing",pendingSubmit:null,serverMessage:ne.message});return}ne.status==="error"&&(ee(),K({...ge,phase:"editing",pendingSubmit:null,serverMessage:`Backend returned an error while this submit was pending: ${ne.message}`}))}),re=[s,ee,K],n[23]=s,n[24]=ue,n[25]=re):(ue=n[24],re=n[25]),w.useEffect(ue,re);let me,Ce;n[26]!==c?(me=()=>{if(T.current&&!c){const ne=_.current;if(ne.status==="open"){ee();const ge=ne.pendingSubmit?r3:s3;K({...ne,phase:"editing",pendingSubmit:null,serverMessage:ge,connectionLost:!0})}}T.current=c},Ce=[ee,c,K],n[26]=c,n[27]=me,n[28]=Ce):(me=n[27],Ce=n[28]),w.useEffect(me,Ce);let Oe,_e;n[29]===Symbol.for("react.memo_cache_sentinel")?(Oe=()=>()=>{ee()},_e=[ee],n[29]=Oe,n[30]=_e):(Oe=n[29],_e=n[30]),w.useEffect(Oe,_e);let Re;return n[31]!==le||n[32]!==E||n[33]!==pe||n[34]!==j?(Re={state:E,validationErrors:j,openCreateNode:le,updateDraft:z,submit:pe,close:te},n[31]=le,n[32]=E,n[33]=pe,n[34]=j,n[35]=Re):Re=n[35],Re}const Cc=(r,n)=>n.some(s=>r instanceof s);let af,of;function u3(){return af||(af=[IDBDatabase,IDBObjectStore,IDBIndex,IDBCursor,IDBTransaction])}function d3(){return of||(of=[IDBCursor.prototype.advance,IDBCursor.prototype.continue,IDBCursor.prototype.continuePrimaryKey])}const jc=new WeakMap,mc=new WeakMap,Zi=new WeakMap;function p3(r){const n=new Promise((s,c)=>{const d=()=>{r.removeEventListener("success",h),r.removeEventListener("error",y)},h=()=>{s(wa(r.result)),d()},y=()=>{c(r.error),d()};r.addEventListener("success",h),r.addEventListener("error",y)});return Zi.set(n,r),n}function h3(r){if(jc.has(r))return;const n=new Promise((s,c)=>{const d=()=>{r.removeEventListener("complete",h),r.removeEventListener("error",y),r.removeEventListener("abort",y)},h=()=>{s(),d()},y=()=>{c(r.error||new DOMException("AbortError","AbortError")),d()};r.addEventListener("complete",h),r.addEventListener("error",y),r.addEventListener("abort",y)});jc.set(r,n)}let Mc={get(r,n,s){if(r instanceof IDBTransaction){if(n==="done")return jc.get(r);if(n==="store")return s.objectStoreNames[1]?void 0:s.objectStore(s.objectStoreNames[0])}return wa(r[n])},set(r,n,s){return r[n]=s,!0},has(r,n){return r instanceof IDBTransaction&&(n==="done"||n==="store")?!0:n in r}};function Mf(r){Mc=r(Mc)}function f3(r){return d3().includes(r)?function(...n){return r.apply(Dc(this),n),wa(this.request)}:function(...n){return wa(r.apply(Dc(this),n))}}function m3(r){return typeof r=="function"?f3(r):(r instanceof IDBTransaction&&h3(r),Cc(r,u3())?new Proxy(r,Mc):r)}function wa(r){if(r instanceof IDBRequest)return p3(r);if(mc.has(r))return mc.get(r);const n=m3(r);return n!==r&&(mc.set(r,n),Zi.set(n,r)),n}const Dc=r=>Zi.get(r);function g3(r,n,{blocked:s,upgrade:c,blocking:d,terminated:h}={}){const y=indexedDB.open(r,n),g=wa(y);return c&&y.addEventListener("upgradeneeded",f=>{c(wa(y.result),f.oldVersion,f.newVersion,wa(y.transaction),f)}),s&&y.addEventListener("blocked",f=>s(f.oldVersion,f.newVersion,f)),g.then(f=>{h&&f.addEventListener("close",()=>h()),d&&f.addEventListener("versionchange",b=>d(b.oldVersion,b.newVersion,b))}).catch(()=>{}),g}function y3(r,{blocked:n}={}){const s=indexedDB.deleteDatabase(r);return n&&s.addEventListener("blocked",c=>n(c.oldVersion,c)),wa(s).then(()=>{})}const b3=["get","getKey","getAll","getAllKeys","count"],v3=["put","add","delete","clear"],gc=new Map;function lf(r,n){if(!(r instanceof IDBDatabase&&!(n in r)&&typeof n=="string"))return;if(gc.get(n))return gc.get(n);const s=n.replace(/FromIndex$/,""),c=n!==s,d=v3.includes(s);if(!(s in(c?IDBIndex:IDBObjectStore).prototype)||!(d||b3.includes(s)))return;const h=async function(y,...g){const f=this.transaction(y,d?"readwrite":"readonly");let b=f.store;return c&&(b=b.index(g.shift())),(await Promise.all([b[s](...g),d&&f.done]))[0]};return gc.set(n,h),h}Mf(r=>({...r,get:(n,s,c)=>lf(n,s)||r.get(n,s,c),has:(n,s)=>!!lf(n,s)||r.has(n,s)}));const _3=["continue","continuePrimaryKey","advance"],sf={},kc=new WeakMap,Df=new WeakMap,x3={get(r,n){if(!_3.includes(n))return r[n];let s=sf[n];return s||(s=sf[n]=function(...c){kc.set(this,Df.get(this)[n](...c))}),s}};async function*S3(...r){let n=this;if(n instanceof IDBCursor||(n=await n.openCursor(...r)),!n)return;n=n;const s=new Proxy(n,x3);for(Df.set(s,n),Zi.set(s,Dc(n));n;)yield s,n=await(kc.get(s)||n.continue()),kc.delete(s)}function rf(r,n){return n===Symbol.asyncIterator&&Cc(r,[IDBIndex,IDBObjectStore,IDBCursor])||n==="iterate"&&Cc(r,[IDBIndex,IDBObjectStore])}Mf(r=>({...r,get(n,s,c){return rf(n,s)?S3:r.get(n,s,c)},has(n,s){return rf(n,s)||r.has(n,s)}}));const kf="minigraph-clipboard",w3=1,Kn="items";let Ii=null;function cf(){return g3(kf,w3,{upgrade(r){r.objectStoreNames.contains(Kn)&&r.deleteObjectStore(Kn);const n=r.createObjectStore(Kn,{keyPath:"id"});n.createIndex("by-alias","node.alias",{unique:!0}),n.createIndex("by-clippedAt","clippedAt")}})}function Do(){return Ii||(Ii=cf().catch(async r=>(console.warn("[clipboard/db] openDB failed, deleting and recreating:",r),Ii=null,await y3(kf),cf()))),Ii}async function T3(){return(await(await Do()).getAllFromIndex(Kn,"by-clippedAt")).reverse()}async function uf(r){return(await Do()).getFromIndex(Kn,"by-alias",r)}async function E3(r){await(await Do()).add(Kn,r)}async function N3(r,n){const c=(await Do()).transaction(Kn,"readwrite");await c.store.delete(r),await c.store.add(n),await c.done}async function A3(r){await(await Do()).delete(Kn,r)}async function C3(){await(await Do()).clear(Kn)}const j3="minigraph-clipboard-sync";function M3(){return new BroadcastChannel(j3)}function D3(r,n){switch(n.type){case"HYDRATE":return{items:n.items,isLoading:!1};case"ITEM_ADDED":return{...r,items:[n.item,...r.items]};case"ITEM_REPLACED":{const s=r.items.filter(c=>c.id!==n.previousId);return{...r,items:[n.item,...s]}}case"ITEM_REMOVED":return{...r,items:r.items.filter(s=>s.id!==n.id)};case"ITEMS_CLEARED":return{...r,items:[]};default:return r}}const Of=w.createContext(null);function k3({children:r}){const[n,s]=w.useReducer(D3,{items:[],isLoading:!0}),c=w.useRef(null);w.useEffect(()=>{T3().then(b=>s({type:"HYDRATE",items:b}))},[]),w.useEffect(()=>{let b;try{b=M3()}catch{return}return c.current=b,b.onmessage=E=>{const x=E.data;switch(x.type){case"item-added":s({type:"ITEM_ADDED",item:x.item});break;case"item-replaced":s({type:"ITEM_REPLACED",item:x.item,previousId:x.previousId});break;case"item-removed":s({type:"ITEM_REMOVED",id:x.id});break;case"items-cleared":s({type:"ITEMS_CLEARED"});break}},()=>{b.close(),c.current=null}},[]);const d=w.useCallback(b=>{var E;(E=c.current)==null||E.postMessage(b)},[]),h=w.useCallback(async(b,E,x)=>{try{const S={id:crypto.randomUUID(),clippedAt:new Date().toISOString(),sourceWsPath:x.sourceWsPath,sourceLabel:x.sourceLabel,node:b,connections:E},j=await uf(b.alias);if(j)return{status:"duplicate",existingItem:j,pendingItem:S};try{await E3(S)}catch(A){if(A instanceof DOMException&&A.name==="ConstraintError"){const _=await uf(b.alias);if(_)return{status:"duplicate",existingItem:_,pendingItem:S}}throw A}return s({type:"ITEM_ADDED",item:S}),d({type:"item-added",item:S}),{status:"added"}}catch(S){return{status:"error",message:S instanceof Error?S.message:String(S)}}},[d]),y=w.useCallback(async(b,E)=>{await N3(E,b),s({type:"ITEM_REPLACED",item:b,previousId:E}),d({type:"item-replaced",item:b,previousId:E})},[d]),g=w.useCallback(async b=>{await A3(b),s({type:"ITEM_REMOVED",id:b}),d({type:"item-removed",id:b})},[d]),f=w.useCallback(async()=>{await C3(),s({type:"ITEMS_CLEARED"}),d({type:"items-cleared"})},[d]);return p.jsx(Of.Provider,{value:{items:n.items,isLoading:n.isLoading,clipNode:h,confirmReplace:y,removeItem:g,clearAll:f},children:r})}function Rf(){const r=w.useContext(Of);if(!r)throw new Error("useClipboardContext must be used inside <ClipboardProvider>");return r}function zf(r){const n=Date.now(),s=new Date(r).getTime(),c=n-s;if(c<0)return"just now";const d=Math.floor(c/1e3);if(d<60)return"just now";const h=Math.floor(d/60);if(h<60)return`${h} min ago`;const y=Math.floor(h/60);if(y<24)return`${y} hour${y>1?"s":""} ago`;const g=Math.floor(y/24);return g===1?"yesterday":g<30?`${g} days ago`:new Date(r).toLocaleDateString()}const O3="_item_1rbm8_1",R3="_previewFrame_1rbm8_13",z3="_preview_1rbm8_13",B3="_previewShell_1rbm8_25",H3="_metaBlock_1rbm8_29",G3="_timestamp_1rbm8_35",U3="_removeChrome_1rbm8_40",L3="_removeIcon_1rbm8_68",xa={item:O3,previewFrame:R3,preview:z3,previewShell:B3,metaBlock:H3,timestamp:G3,removeChrome:U3,removeIcon:L3};function Y3(r){const n=Ee.c(43),{item:s,onRemove:c,onOpenMenu:d,onCloseMenu:h}=r,{node:y,clippedAt:g,sourceLabel:f}=s;let b;n[0]!==s.id||n[1]!==h?(b=P=>{h(),B0(P.dataTransfer,s.id)},n[0]=s.id,n[1]=h,n[2]=b):b=n[2];const E=b;let x;n[3]!==s.id||n[4]!==d?(x=P=>{P.preventDefault(),d(s.id,P.clientX,P.clientY)},n[3]=s.id,n[4]=d,n[5]=x):x=n[5];const S=x;let j;n[6]!==s.id||n[7]!==d?(j=P=>{if(P.key==="ContextMenu"||P.key==="F10"&&P.shiftKey){P.preventDefault();const le=P.currentTarget.getBoundingClientRect();d(s.id,Math.round(le.left+8),Math.round(le.top+8))}},n[6]=s.id,n[7]=d,n[8]=j):j=n[8];const A=j,_=`Remove node ${y.alias} from clipboard`;let v;n[9]!==s.id||n[10]!==h||n[11]!==c?(v=P=>{P.stopPropagation(),h(),c(s.id)},n[9]=s.id,n[10]=h,n[11]=c,n[12]=v):v=n[12];let T;n[13]===Symbol.for("react.memo_cache_sentinel")?(T=p.jsx(Ac,{className:xa.removeIcon,"aria-hidden":"true",focusable:"false"}),n[13]=T):T=n[13];let O;n[14]!==_||n[15]!==v?(O=p.jsx("button",{type:"button",className:xa.removeChrome,draggable:!1,"aria-label":_,onClick:v,children:T}),n[14]=_,n[15]=v,n[16]=O):O=n[16];const k=`Drag node ${y.alias} into the graph to paste`,H=y.types[0]??"unknown";let D;n[17]!==H?(D=Nf(H),n[17]=H,n[18]=D):D=n[18];const L=y.types[0]??"unknown";let C;n[19]!==y.alias||n[20]!==y.properties||n[21]!==L?(C=p.jsx(Af,{alias:y.alias,nodeType:L,properties:y.properties}),n[19]=y.alias,n[20]=y.properties,n[21]=L,n[22]=C):C=n[22];let Y;n[23]!==D||n[24]!==C?(Y=p.jsx("div",{className:xa.previewShell,style:D,children:C}),n[23]=D,n[24]=C,n[25]=Y):Y=n[25];let J;n[26]!==S||n[27]!==E||n[28]!==A||n[29]!==Y||n[30]!==k?(J=p.jsx("div",{className:xa.preview,role:"group",draggable:!0,onDragStart:E,onContextMenu:S,onKeyDown:A,tabIndex:0,"aria-label":k,children:Y}),n[26]=S,n[27]=E,n[28]=A,n[29]=Y,n[30]=k,n[31]=J):J=n[31];let I;n[32]!==J||n[33]!==O?(I=p.jsxs("div",{className:xa.previewFrame,children:[O,J]}),n[32]=J,n[33]=O,n[34]=I):I=n[34];let K;n[35]!==g?(K=zf(g),n[35]=g,n[36]=K):K=n[36];let Z;n[37]!==f||n[38]!==K?(Z=p.jsx("div",{className:xa.metaBlock,children:p.jsxs("div",{className:xa.timestamp,children:["Clipped ",K," from ",f]})}),n[37]=f,n[38]=K,n[39]=Z):Z=n[39];let ee;return n[40]!==I||n[41]!==Z?(ee=p.jsxs("div",{className:xa.item,children:[I,Z]}),n[40]=I,n[41]=Z,n[42]=ee):ee=n[42],ee}const q3="_menu_164vh_1",I3="_menuItem_164vh_12",yc={menu:q3,menuItem:I3},bc=16;function df(r,n,s){const c=bc,d=Math.max(bc,s-n-bc);return Math.min(Math.max(r,c),d)}function X3(r){const n=Ee.c(28),{open:s,x:c,y:d,canPasteToInput:h,onPasteToInput:y,onInspect:g,onClose:f}=r,b=w.useRef(null),E=w.useRef(null),x=w.useRef(null);let S;n[0]!==c||n[1]!==d?(S={left:c,top:d},n[0]=c,n[1]=d,n[2]=S):S=n[2];const[j,A]=w.useState(S);let _,v;n[3]!==s||n[4]!==c||n[5]!==d?(_=()=>{if(!s||!b.current)return;const J=b.current.getBoundingClientRect();A({left:df(c,J.width,window.innerWidth),top:df(d,J.height,window.innerHeight)})},v=[s,c,d],n[3]=s,n[4]=c,n[5]=d,n[6]=_,n[7]=v):(_=n[6],v=n[7]),w.useLayoutEffect(_,v);let T,O;if(n[8]!==h||n[9]!==f||n[10]!==s?(T=()=>{var Z,ee;if(!s)return;h?(Z=E.current)==null||Z.focus():(ee=x.current)==null||ee.focus();const J=P=>{b.current&&!b.current.contains(P.target)&&f()},I=P=>{P.key==="Escape"&&(P.preventDefault(),f())},K=()=>f();return document.addEventListener("pointerdown",J),document.addEventListener("keydown",I),window.addEventListener("scroll",K,!0),window.addEventListener("resize",K),()=>{document.removeEventListener("pointerdown",J),document.removeEventListener("keydown",I),window.removeEventListener("scroll",K,!0),window.removeEventListener("resize",K)}},O=[s,h,f],n[8]=h,n[9]=f,n[10]=s,n[11]=T,n[12]=O):(T=n[11],O=n[12]),w.useEffect(T,O),!s)return null;let k;n[13]!==j.left||n[14]!==j.top?(k={left:j.left,top:j.top},n[13]=j.left,n[14]=j.top,n[15]=k):k=n[15];const H=!h;let D;n[16]!==h||n[17]!==y?(D=()=>{h&&y()},n[16]=h,n[17]=y,n[18]=D):D=n[18];let L;n[19]!==H||n[20]!==D?(L=p.jsx("button",{ref:E,role:"menuitem",type:"button",className:yc.menuItem,disabled:H,onClick:D,children:"Paste to Input"}),n[19]=H,n[20]=D,n[21]=L):L=n[21];let C;n[22]!==g?(C=p.jsx("button",{ref:x,role:"menuitem",type:"button",className:yc.menuItem,onClick:g,children:"Inspect"}),n[22]=g,n[23]=C):C=n[23];let Y;return n[24]!==C||n[25]!==k||n[26]!==L?(Y=p.jsxs("div",{ref:b,className:yc.menu,style:k,role:"menu","aria-label":"Clipboard item actions",children:[L,C]}),n[24]=C,n[25]=k,n[26]=L,n[27]=Y):Y=n[27],Y}const J3="_sidebar_nf394_2",Z3="_header_nf394_12",V3="_headerTitle_nf394_22",Q3="_clearBtn_nf394_29",K3="_itemList_nf394_45",$3="_loading_nf394_55",W3="_emptyState_nf394_65",P3="_emptyIcon_nf394_78",F3="_emptyTitle_nf394_83",eS="_emptyHint_nf394_87",tS="_inspectPanel_nf394_93",nS="_inspectHeader_nf394_101",aS="_inspectClose_nf394_115",oS="_inspectBody_nf394_129",lS="_dialog_nf394_135",iS="_dialogTitle_nf394_150",sS="_dialogBody_nf394_157",rS="_dialogActions_nf394_164",cS="_cancelBtn_nf394_171",uS="_replaceBtn_nf394_185",bt={sidebar:J3,header:Z3,headerTitle:V3,clearBtn:Q3,itemList:K3,loading:$3,emptyState:W3,emptyIcon:P3,emptyTitle:F3,emptyHint:eS,inspectPanel:tS,inspectHeader:nS,inspectClose:aS,inspectBody:oS,dialog:lS,dialogTitle:iS,dialogBody:sS,dialogActions:rS,cancelBtn:cS,replaceBtn:uS};function dS(){const r=Ee.c(1);let n;return r[0]===Symbol.for("react.memo_cache_sentinel")?(n=p.jsxs("div",{className:bt.emptyState,children:[p.jsx("span",{className:bt.emptyIcon,children:"📋"}),p.jsx("span",{className:bt.emptyTitle,children:"No items clipped yet."}),p.jsx("span",{className:bt.emptyHint,children:"Right-click a node in the Graph view to get started."})]}),r[0]=n):n=r[0],n}function pS(r){const n=Ee.c(41),{connected:s,onPasteToInput:c}=r,d=Rf(),[h,y]=w.useState(null),[g,f]=w.useState(null);let b;n[0]===Symbol.for("react.memo_cache_sentinel")?(b=(z,$,ie)=>{f({itemId:z,x:$,y:ie})},n[0]=b):b=n[0];const E=b;let x;n[1]===Symbol.for("react.memo_cache_sentinel")?(x=()=>{f(null)},n[1]=x):x=n[1];const S=x;let j;n[2]!==c?(j=z=>{S(),c(z)},n[2]=c,n[3]=j):j=n[3];const A=j;let _;n[4]===Symbol.for("react.memo_cache_sentinel")?(_=z=>{S(),y($=>($==null?void 0:$.id)===z.id?null:z)},n[4]=_):_=n[4];const v=_;let T;n[5]!==d?(T=z=>{S(),y($=>($==null?void 0:$.id)===z?null:$),d.removeItem(z)},n[5]=d,n[6]=T):T=n[6];const O=T;let k;n[7]!==d?(k=()=>{S(),y(null),d.clearAll()},n[7]=d,n[8]=k):k=n[8];const H=k;let D,L;n[9]!==g||n[10]!==d.items||n[11]!==h?(D=()=>{const z=new Set(d.items.map(hS));g&&!z.has(g.itemId)&&f(null),h&&!z.has(h.id)&&y(null)},L=[d.items,g,h],n[9]=g,n[10]=d.items,n[11]=h,n[12]=D,n[13]=L):(D=n[12],L=n[13]),w.useEffect(D,L);let C;n[14]!==g||n[15]!==d.items?(C=g?d.items.find(z=>z.id===g.itemId)??null:null,n[14]=g,n[15]=d.items,n[16]=C):C=n[16];const Y=C;let J;n[17]===Symbol.for("react.memo_cache_sentinel")?(J=p.jsx("span",{className:bt.headerTitle,children:"Workspace"}),n[17]=J):J=n[17];let I;n[18]!==d.items.length||n[19]!==H?(I=d.items.length>0&&p.jsx("button",{className:bt.clearBtn,onClick:H,"aria-label":"Clear all workspace items",children:"Clear"}),n[18]=d.items.length,n[19]=H,n[20]=I):I=n[20];let K;n[21]!==I?(K=p.jsxs("div",{className:bt.header,children:[J,I]}),n[21]=I,n[22]=K):K=n[22];let Z;n[23]!==d.isLoading||n[24]!==d.items||n[25]!==O?(Z=d.isLoading?p.jsx("div",{className:bt.loading,children:"Loading…"}):d.items.length===0?p.jsx(dS,{}):d.items.map(z=>p.jsx(Y3,{item:z,onRemove:O,onOpenMenu:E,onCloseMenu:S},z.id)),n[23]=d.isLoading,n[24]=d.items,n[25]=O,n[26]=Z):Z=n[26];let ee;n[27]!==Z?(ee=p.jsx("div",{className:bt.itemList,children:Z}),n[27]=Z,n[28]=ee):ee=n[28];let P;n[29]!==h?(P=h&&p.jsxs("div",{className:bt.inspectPanel,children:[p.jsxs("div",{className:bt.inspectHeader,children:[p.jsxs("span",{children:["Inspect node ",h.node.alias]}),p.jsx("button",{className:bt.inspectClose,onClick:()=>y(null),"aria-label":"Close inspect panel",children:"✕"})]}),p.jsx("div",{className:bt.inspectBody,children:p.jsx(Rc,{data:{node:h.node,connections:h.connections},style:El})})]}),n[29]=h,n[30]=P):P=n[30];let le;n[31]!==g||n[32]!==Y||n[33]!==s||n[34]!==A?(le=g&&Y&&p.jsx(X3,{open:!0,x:g.x,y:g.y,canPasteToInput:s,onPasteToInput:()=>A(Y),onInspect:()=>v(Y),onClose:S}),n[31]=g,n[32]=Y,n[33]=s,n[34]=A,n[35]=le):le=n[35];let R;return n[36]!==K||n[37]!==ee||n[38]!==P||n[39]!==le?(R=p.jsxs("div",{className:bt.sidebar,children:[K,ee,P,le]}),n[36]=K,n[37]=ee,n[38]=P,n[39]=le,n[40]=R):R=n[40],R}function hS(r){return r.id}const pf=120,hf=18,fS=180,mS=650;function gS(r){const{wheelTargetRef:n,scrollRef:s,contentWrapperRef:c,currentIndex:d,totalPages:h,onNavigatePrev:y,onNavigateNext:g}=r,f=w.useRef(0),b=w.useRef(null),E=w.useRef(!1),x=w.useRef(null),S=w.useRef(y),j=w.useRef(g),A=w.useRef(d),_=w.useRef(h);w.useEffect(()=>{S.current=y}),w.useEffect(()=>{j.current=g}),w.useEffect(()=>{A.current=d}),w.useEffect(()=>{_.current=h}),w.useEffect(()=>{x.current!==null&&(clearTimeout(x.current),x.current=null),c.current&&(c.current.style.transition="none",c.current.style.transform="translateY(0)"),f.current=0,b.current=null},[d]),w.useEffect(()=>{const v=n.current;if(!v)return;function T(){f.current=0,b.current=null,c.current&&(c.current.style.transition="transform 0.28s cubic-bezier(0.25, 0.46, 0.45, 0.94)",c.current.style.transform="translateY(0)")}function O(k){if(k.deltaY===0)return;const H=s.current;if(!H)return;const D=H.scrollTop<=0,L=H.scrollTop+H.clientHeight>=H.scrollHeight-1,C=k.deltaY<0,Y=k.deltaY>0,J=D&&C,I=L&&Y;if(!J&&!I){T();return}if(E.current)return;const K=A.current,Z=_.current;if(J&&K===0||I&&K===Z-1)return;const ee=J?"prev":"next";if(b.current!==null&&b.current!==ee&&T(),b.current=ee,f.current+=Math.abs(k.deltaY),c.current){const P=ee==="prev"?-1:1,le=f.current*(hf/pf),R=Math.min(le,hf)*P;c.current.style.transition="none",c.current.style.transform=`translateY(${R}px)`}if(x.current!==null&&clearTimeout(x.current),x.current=setTimeout(T,fS),f.current>=pf){x.current!==null&&clearTimeout(x.current);const P=b.current;T(),E.current=!0,P==="prev"?S.current():j.current(),setTimeout(()=>{E.current=!1},mS)}}return v.addEventListener("wheel",O,{passive:!0}),()=>{x.current!==null&&clearTimeout(x.current),v.removeEventListener("wheel",O)}},[])}const yS="_helpRoot_18tja_2",bS="_categoryNav_18tja_11",vS="_categoryTabScroller_18tja_21",_S="_categoryTab_18tja_21",xS="_categoryTabActive_18tja_71",SS="_maximizeButton_18tja_78",wS="_closeButton_18tja_100",TS="_helpBody_18tja_122",ES="_emptyFallback_18tja_130",NS="_helpContent_18tja_147",AS="_topicLink_18tja_226",CS="_helpBodyContent_18tja_271",jS="_chipStrip_18tja_276",MS="_chipStripLabel_18tja_294",DS="_topicChip_18tja_310",kS="_topicChipActive_18tja_338",Ut={helpRoot:yS,categoryNav:bS,categoryTabScroller:vS,categoryTab:_S,categoryTabActive:xS,maximizeButton:SS,closeButton:wS,helpBody:TS,emptyFallback:ES,helpContent:NS,topicLink:AS,helpBodyContent:CS,chipStrip:jS,chipStripLabel:MS,topicChip:DS,topicChipActive:kS};function Oc(r){return typeof r=="string"?r:typeof r=="number"?String(r):Array.isArray(r)?r.map(Oc).join(""):yf.isValidElement(r)?Oc(r.props.children):""}function OS(r){if(!r.trim().toLowerCase().startsWith("help "))return null;const c=r.trim().slice(5).replace(/\s*\(.*\)\s*$/,"").trim().toLowerCase();return c.length>0?c:null}function RS(r){var ue;const n=Ee.c(53),{activeTopic:s,onNavigate:c,onClose:d,onToggleMaximize:h,isMaximized:y}=r,g=w.useRef(null),f=w.useRef(null),b=w.useRef(null),E=w.useRef(null);let x;n[0]===Symbol.for("react.memo_cache_sentinel")?(x=()=>{g.current&&(g.current.scrollTop=0)},n[0]=x):x=n[0];let S;n[1]!==s?(S=[s],n[1]=s,n[2]=S):S=n[2],w.useEffect(x,S);let j;n[3]===Symbol.for("react.memo_cache_sentinel")?(j=()=>{const re=E.current;if(!re)return;const me=re.querySelector('[aria-current="step"]');me&&me.scrollIntoView({block:"nearest",inline:"nearest",behavior:"smooth"})},n[3]=j):j=n[3];let A;n[4]!==s?(A=[s],n[4]=s,n[5]=A):A=n[5],w.useEffect(j,A);let _;n[6]!==s?(_=Tf(s),n[6]=s,n[7]=_):_=n[7];const v=_;let T;n[8]!==v?(T=Sc(v),n[8]=v,n[9]=T):T=n[9];const O=T,k=O.length;let H;n[10]!==v?(H=((ue=xc.find(re=>re.id===v))==null?void 0:ue.chipStripLabel)??null,n[10]=v,n[11]=H):H=n[11];const D=H,L=Ao.indexOf(s),C=L<0?0:L,Y=Ao.length;let J,I;n[12]!==c||n[13]!==C?(J=()=>c(Ao[C-1]??""),I=()=>c(Ao[C+1]??Ao[Ao.length-1]),n[12]=c,n[13]=C,n[14]=J,n[15]=I):(J=n[14],I=n[15]);let K;n[16]!==C||n[17]!==J||n[18]!==I?(K={wheelTargetRef:f,scrollRef:g,contentWrapperRef:b,currentIndex:C,totalPages:Y,onNavigatePrev:J,onNavigateNext:I},n[16]=C,n[17]=J,n[18]=I,n[19]=K):K=n[19],gS(K);let Z;n[20]!==s?(Z=Ji(s),n[20]=s,n[21]=Z):Z=n[21];const ee=Z;let P;n[22]!==c?(P=re=>{const{children:me,...Ce}=re,Oe=Oc(me).trim(),_e=OS(Oe);return _e!==null&&Ji(_e)!==null?p.jsx("li",{...Ce,children:p.jsx("button",{className:Ut.topicLink,"aria-label":`Open help topic: ${_e}`,onClick:()=>c(_e),children:me})}):p.jsx("li",{...Ce,children:me})},n[22]=c,n[23]=P):P=n[23];const le=P;let R;n[24]!==v||n[25]!==c?(R=xc.map(re=>p.jsx("button",{className:[Ut.categoryTab,re.id===v?Ut.categoryTabActive:""].join(" ").trim(),"aria-current":re.id===v?"true":void 0,onClick:()=>{const me=Sc(re.id);c(me[0]??"")},children:re.label},re.id)),n[24]=v,n[25]=c,n[26]=R):R=n[26];let z;n[27]!==R?(z=p.jsx("div",{className:Ut.categoryTabScroller,children:R}),n[27]=R,n[28]=z):z=n[28];let $;n[29]!==y||n[30]!==h?($=h&&p.jsx("button",{className:Ut.maximizeButton,onClick:h,"aria-label":y?"Restore help panel":"Maximize help panel",children:y?"⊞":"⛶"}),n[29]=y,n[30]=h,n[31]=$):$=n[31];let ie;n[32]!==d?(ie=d&&p.jsx("button",{className:Ut.closeButton,onClick:d,"aria-label":"Close help panel",children:"×"}),n[32]=d,n[33]=ie):ie=n[33];let se;n[34]!==z||n[35]!==$||n[36]!==ie?(se=p.jsxs("nav",{className:Ut.categoryNav,"aria-label":"Help categories",children:[z,$,ie]}),n[34]=z,n[35]=$,n[36]=ie,n[37]=se):se=n[37];let pe;n[38]!==v||n[39]!==s||n[40]!==O||n[41]!==k||n[42]!==D||n[43]!==c?(pe=k>1&&p.jsxs("div",{className:Ut.chipStrip,ref:E,children:[D!==null&&p.jsx("span",{className:Ut.chipStripLabel,children:D}),O.map(re=>{const me=re===s,Ce=$1(re,v);return p.jsx("button",{className:[Ut.topicChip,me?Ut.topicChipActive:""].join(" ").trim(),"aria-current":me?"step":void 0,onClick:()=>c(re),children:Ce},re)})]}),n[38]=v,n[39]=s,n[40]=O,n[41]=k,n[42]=D,n[43]=c,n[44]=pe):pe=n[44];let ae;n[45]!==s||n[46]!==ee||n[47]!==le?(ae=p.jsx("div",{className:Ut.helpBody,ref:g,children:p.jsx("div",{className:Ut.helpBodyContent,ref:b,children:ee===null?p.jsxs("div",{className:Ut.emptyFallback,children:[p.jsxs("code",{children:["help ",s||""]}),"  not found in the local bundle."]}):p.jsx("div",{className:Ut.helpContent,children:p.jsx(py,{remarkPlugins:[hy],components:s===""?{li:le}:void 0,children:ee})})})}),n[45]=s,n[46]=ee,n[47]=le,n[48]=ae):ae=n[48];let te;return n[49]!==se||n[50]!==pe||n[51]!==ae?(te=p.jsxs("div",{className:Ut.helpRoot,role:"region","aria-label":"Help browser",ref:f,children:[se,pe,ae]}),n[49]=se,n[50]=pe,n[51]=ae,n[52]=te):te=n[52],te}function zS(r){const n=Ee.c(22),{existingItem:s,pendingItem:c,onReplace:d,onCancel:h}=r,y=w.useRef(null);let g,f;n[0]===Symbol.for("react.memo_cache_sentinel")?(g=()=>{const O=y.current;O&&!O.open&&O.showModal()},f=[],n[0]=g,n[1]=f):(g=n[0],f=n[1]),w.useEffect(g,f);let b;n[2]===Symbol.for("react.memo_cache_sentinel")?(b=p.jsx("h2",{id:"duplicate-dialog-title",className:bt.dialogTitle,children:"Duplicate Node"}),n[2]=b):b=n[2];let E;n[3]!==c.node.alias?(E=p.jsxs("strong",{children:['"',c.node.alias,'"']}),n[3]=c.node.alias,n[4]=E):E=n[4];let x;n[5]!==s.clippedAt?(x=zf(s.clippedAt),n[5]=s.clippedAt,n[6]=x):x=n[6];let S;n[7]!==E||n[8]!==x?(S=p.jsxs("p",{className:bt.dialogBody,children:["A clipboard item with alias ",E," already exists (clipped ",x,")."]}),n[7]=E,n[8]=x,n[9]=S):S=n[9];let j;n[10]===Symbol.for("react.memo_cache_sentinel")?(j=p.jsx("p",{className:bt.dialogBody,children:"Replace it with the new snapshot?"}),n[10]=j):j=n[10];let A;n[11]!==h?(A=p.jsx("button",{className:bt.cancelBtn,onClick:h,children:"Cancel"}),n[11]=h,n[12]=A):A=n[12];let _;n[13]!==d?(_=p.jsx("button",{className:bt.replaceBtn,onClick:d,children:"Replace"}),n[13]=d,n[14]=_):_=n[14];let v;n[15]!==A||n[16]!==_?(v=p.jsxs("div",{className:bt.dialogActions,children:[A,_]}),n[15]=A,n[16]=_,n[17]=v):v=n[17];let T;return n[18]!==h||n[19]!==v||n[20]!==S?(T=p.jsxs("dialog",{ref:y,className:bt.dialog,onClose:h,"aria-labelledby":"duplicate-dialog-title",children:[b,S,j,v]}),n[18]=h,n[19]=v,n[20]=S,n[21]=T):T=n[21],T}function BS(r,n){if(!n)return null;const s=r.trim().toLowerCase();if(s!=="help"&&!s.startsWith("help "))return null;const c=Ef(r);return Ji(c)!==null?c:null}class HS{constructor(){this.listeners=new Map}on(n,s){const c=n;return this.listeners.has(c)||this.listeners.set(c,new Set),this.listeners.get(c).add(s),()=>{var d;(d=this.listeners.get(c))==null||d.delete(s)}}emit(n){const s=this.listeners.get(n.kind);s&&s.forEach(c=>{try{c(n)}catch(d){console.error(`[ProtocolBus] listener for '${n.kind}' threw:`,d)}})}clear(){this.listeners.clear()}}const GS=new Set(["info","error","ping","welcome"]);function US(r,n){const s=[],c={msgId:r,raw:n};let d=!1,h=!1,y=!1,g=!1,f=!1;const b=Nl(n);if(b.isJSON){const T=b.data;if(typeof T.type=="string"){const O=T.type;return s.push({...c,kind:"lifecycle",type:O,knownType:GS.has(O),message:typeof T.message=="string"?T.message:n,time:T.time??null}),s.length>0?s:[{...c,kind:"unclassified"}]}return s.push({...c,kind:"json.response",data:b.data}),s.length>0?s:[{...c,kind:"unclassified"}]}const E=Qy(n);E&&(f=!0,s.push({...c,kind:"payload.large",apiPath:E.apiPath,byteSize:E.byteSize,filename:E.filename}));const x=Ky(n);x&&(y=!0,s.push({...c,kind:"upload.invitation",uploadPath:x}));const S=Sf(n);if(S&&(g=!0,s.push({...c,kind:"upload.contentPath",uploadPath:S})),xf(n)){h=!0;const T=Hc(n);T&&s.push({...c,kind:"graph.link",apiPath:T})}if(h){const T=Zy(n);T&&s.push({...c,kind:"graph.exported",graphName:T.graphName,apiPath:T.apiPath})}const j=n1(n);j&&s.push({...c,kind:"graph.mutation",mutationType:j});const A=t1(n);A&&s.push({...c,kind:"minigraph.createNode.textResult",status:A.status,alias:A.alias,message:A.message}),n.startsWith("> ")&&(d=!0,s.push({...c,kind:"command.echo",commandText:n.slice(2)})),$y(n)&&s.push({...c,kind:"command.helpOrDescribe",commandText:n.slice(2)});const _=Wy(n);_&&s.push({...c,kind:"command.importGraph",graphName:_});const v=Vy(n);return v&&s.push({...c,kind:"graph.export.failed",reason:v.reason}),!d&&!h&&!y&&!g&&!f&&Bc(n)&&s.push({...c,kind:"docs.response",isMarkdown:!0}),s.length===0&&s.push({...c,kind:"unclassified"}),s}function LS(r){const n=Ee.c(12),{messages:s,bus:c}=r,d=w.useRef(-1);let h;n[0]!==s?(h=()=>{s.length>0&&(d.current=s[s.length-1].id)},n[0]=s,n[1]=h):h=n[1];let y;n[2]===Symbol.for("react.memo_cache_sentinel")?(y=[],n[2]=y):y=n[2],w.useEffect(h,y);let g;if(n[3]!==s){g=new Map;for(const S of s)g.set(S.id,US(S.id,S.raw));n[3]=s,n[4]=g}else g=n[4];const f=g;let b,E;n[5]!==c||n[6]!==f||n[7]!==s?(b=()=>{if(s.length===0)return;const S=s.filter(j=>j.id>d.current);if(S.length!==0){d.current=s[s.length-1].id;for(const j of S){const A=f.get(j.id);if(A)for(const _ of A)c.emit(_)}}},E=[s,c,f],n[5]=c,n[6]=f,n[7]=s,n[8]=b,n[9]=E):(b=n[8],E=n[9]),w.useEffect(b,E);let x;return n[10]!==f?(x={classificationMap:f},n[10]=f,n[11]=x):x=n[11],x}function YS({config:r}){const{title:n,wsPath:s,storageKeyPayload:c,storageKeyHistory:d,storageKeyTab:h,storageKeySavedGraphs:y,supportsUpload:g,supportsClipboard:f,supportsHelp:b,supportsAuthoring:E,tabs:x}=r,S=Wg(),[j,A]=Sa(c,""),_=zc(),[v,T]=w.useState(()=>_.peekPendingPayload(s)),{takePendingPayload:O}=_;w.useEffect(()=>{const Ae=O(s);Ae!==null&&T(Ae)},[O,s]);const k=v??j,H=w.useCallback(Ae=>{T(null),A(Ae)},[A]),D=w.useMemo(()=>k?By(k):{valid:!0,error:null,type:null},[k]),{toasts:L,addToast:C,removeToast:Y}=Hy(),I=w.useRef(new HS).current,K=w.useCallback(Ae=>BS(Ae,b===!0)!==null,[b]),Z=l1({wsPath:s,storageKeyHistory:d,payload:k,addToast:C,bus:I,handleLocalCommand:K}),{classificationMap:ee}=LS({messages:Z.messages,bus:I}),[P,le]=cb(s),{graphData:R,setGraphData:z,rightTab:$,setRightTab:ie,isRefreshing:se}=r1(P,C,x[0],x,h),{modalUploadPath:pe,successfulUploadPaths:ae,handleOpenUploadModal:te,handleCloseUploadModal:ue,handleUploadSuccess:re,handleUploadError:me,resetSuccessfulPaths:Ce}=tb({bus:I,addToast:C});c1({bus:I,pinnedGraphPath:P,setPinnedGraphPath:le,connected:Z.connected,sendRawText:Z.sendRawText,addToast:C});const Oe=w.useRef(!1);w.useEffect(()=>{Oe.current&&!Z.connected&&(le(null),z(null)),Oe.current=Z.connected},[Z.connected,le,z]);const[_e,Re]=Sa(r.storageKeyHelpTopic??"help-topic-fallback",""),[ne,ge]=Sa("help-panel-open",!1),[xe,F]=w.useState(()=>!!b&&!ne),[ye,Se]=w.useState(!1),he=w.useRef(null),we=w.useCallback(()=>{xe&&(Se(!0),he.current=setTimeout(()=>F(!1),400))},[xe]);w.useEffect(()=>{if(!xe||ye)return;const Ae=setTimeout(we,3e3);return()=>clearTimeout(Ae)},[xe,ye,we]),w.useEffect(()=>{ne&&xe&&we()},[ne,xe,we]),w.useEffect(()=>()=>{he.current&&clearTimeout(he.current)},[]),w.useEffect(()=>{if(!b)return;const Ae=Ze=>{Ze.ctrlKey&&Ze.key==="`"&&(Ze.preventDefault(),ge(ft=>!ft))};return window.addEventListener("keydown",Ae),()=>window.removeEventListener("keydown",Ae)},[b,ge]),W1({bus:I,setHelpTopic:Re,onTabSwitch:b?()=>ge(!0):()=>{}}),nb({bus:I,connected:Z.connected,appendMessage:Z.appendMessage,addToast:C});const Ne=Rf(),[Le,nt]=Sa("clipboard-sidebar-open",!1),[qe,vt]=w.useState(null),fe=w.useCallback(Ae=>{const Ze=Ih(Ae,R);Z.setCommand(Ze.command),C(`${Ze.verb==="create"?"Create":"Update"} command for "${Ae.node.alias}" pasted to input`,"info")},[R,Z.setCommand,C]),Mt=w.useCallback(Ae=>{const Ze=Ne.items.find($n=>$n.id===Ae);if(!Ze){C("Clipboard item is no longer available. It may have been removed in another tab.","error");return}const ft=Ih(Ze,R);if(!Z.sendRawText(ft.command)){C("Could not send clipboard paste command because the WebSocket is not open.","error");return}C(`Clipboard node "${Ze.node.alias}" sent as ${ft.verb}. Waiting for backend response.`,"info")},[Ne.items,R,Z.sendRawText,C]),Ht=w.useCallback(async(Ae,Ze)=>{try{const ft=await Ne.clipNode(Ae,Ze,{sourceWsPath:s,sourceLabel:r.label});switch(ft.status){case"added":C(`Node "${Ae.alias}" clipped to clipboard`,"success");break;case"duplicate":vt({pendingItem:ft.pendingItem,existingItem:ft.existingItem});break;case"error":C(`Clip failed: ${ft.message}`,"error");break}}catch(ft){C(`Clip failed: ${ft instanceof Error?ft.message:String(ft)}`,"error")}},[Ne,s,r.label,C]),ut=ob(y??""),{defaultName:ht,setLastSavedName:qt,resetName:Gt}=ib(y?`${y}-untitled-counter`:"untitled-counter",I),Dt=w.useMemo(()=>{var ft;const Ae=R==null?void 0:R.nodes.find($n=>$n.types.includes("Root")),Ze=typeof((ft=Ae==null?void 0:Ae.properties)==null?void 0:ft.name)=="string"?Ae.properties.name:void 0;return Ze!=null&&Ze.trim()?Ze:null},[R])??ht,Tt=w.useMemo(()=>db(Z.sendRawText),[Z.sendRawText]),Ye=c3({bus:I,connected:Z.connected,graphData:R,executor:Tt}),{handleSaveGraph:it,handleLoadGraph:Fe}=rb({bus:I,connected:Z.connected,sendRawText:Z.sendRawText,saveGraph:ut.saveGraph,setLastSavedName:qt,addToast:C}),Te=w.useCallback(Ae=>{const Ze=ee.get(Ae.id),ft=Ze==null?void 0:Ze.find($n=>$n.kind==="graph.link");ft&&le(ft.apiPath)},[ee]),{handleSendToJsonPath:ct}=P1({ctx:_,navigate:S,addToast:C,wsPath:s}),oe=s1("(max-width: 768px)"),{defaultLayout:Me,onLayoutChanged:st}=Qg({id:r.path+"-panel-split",storage:localStorage}),bn=w.useCallback(()=>H(_c(k)),[k]),jn=w.useCallback(()=>{Z.clearMessages(),le(null),z(null),Ce(),Gt()},[Z.clearMessages,z,Ce,Gt]);return p.jsxs("div",{className:$t.wrapper,children:[p.jsx(_b,{toasts:L,onRemove:Y}),pe&&p.jsx(Sx,{uploadPath:pe,onSuccess:re,onClose:ue,onError:me}),E&&p.jsx(a3,{state:Ye.state,validationErrors:Ye.validationErrors,onDraftChange:Ye.updateDraft,onSubmit:Ye.submit,onClose:Ye.close}),p.jsxs("header",{className:$t.header,children:[p.jsx("h1",{className:$t.title,children:n}),p.jsxs("div",{className:$t.headerActions,children:[y&&p.jsx(rv,{disabled:!R,defaultName:ht,onSave:it,nameExists:ut.hasGraph,connected:Z.connected}),y&&ut.savedGraphs.length>0&&p.jsx(vv,{savedGraphs:ut.savedGraphs,onLoad:Fe,onDelete:ut.deleteGraph,connected:Z.connected}),f&&p.jsxs("button",{className:$t.clipboardToggle,onClick:()=>nt(Ae=>!Ae),"aria-label":Le?"Close workspace sidebar":"Open workspace sidebar","aria-pressed":Le,children:["Workspace",Ne.items.length>0?` (${Ne.items.length})`:""]}),p.jsx(Wb,{addToast:C}),b&&p.jsxs("div",{className:$t.helpButtonWrapper,children:[p.jsx("button",{className:`${$t.helpToggle}${xe&&!ye?` ${$t.helpTogglePulsing}`:""}`,onClick:()=>ge(Ae=>!Ae),"aria-label":ne?"Close help panel":"Open help panel","aria-pressed":ne,children:"?"}),xe&&p.jsxs("div",{className:`${$t.helpHint}${ye?` ${$t.helpHintFading}`:""}`,onClick:we,role:"status",children:[p.jsx("kbd",{className:$t.helpHintKbd,children:"Ctrl + `"})," to toggle help"]})]})]})]}),qe&&p.jsx(zS,{existingItem:qe.existingItem,pendingItem:qe.pendingItem,onReplace:async()=>{try{await Ne.confirmReplace(qe.pendingItem,qe.existingItem.id),vt(null),C(`Clipboard item "${qe.pendingItem.node.alias}" replaced`,"success")}catch(Ae){C(`Replace failed: ${Ae instanceof Error?Ae.message:String(Ae)}`,"error")}},onCancel:()=>{vt(null),C("Clip cancelled","info")}}),p.jsxs(mf,{className:$t.panelGroup,orientation:oe?"vertical":"horizontal",defaultLayout:Me,onLayoutChanged:st,children:[p.jsx(Tl,{defaultSize:ne||Le?"50%":"60%",minSize:"25%",children:p.jsx(X_,{messages:Z.messages,classificationMap:ee,onCopy:Z.copyMessages,onClear:jn,consoleRef:Z.consoleRef,command:Z.command,onCommandChange:Z.setCommand,onCommandKeyDown:Z.handleKeyDown,onSend:Z.sendCommand,sendDisabled:!Z.connected||!Z.command.trim(),inputDisabled:!Z.connected,commandHistory:Z.history,onGraphLinkMessage:Te,onCopyMessage:()=>C("Copied to clipboard","success"),onSendToJsonPath:ct,onUploadMockData:te,successfulUploadPaths:ae})}),p.jsx(vc,{className:$t.resizeHandle,"aria-label":"Resize panels"}),p.jsx(Tl,{defaultSize:ne?"50%":Le?"30%":"40%",minSize:"20%",children:p.jsx(N2,{tabs:x,payload:k,onChange:H,validation:D,onFormat:bn,onUpload:g?Z.uploadPayload:void 0,graphData:R,graphName:Dt,activeTab:$,onTabChange:ie,onGraphRenderError:Ae=>C(Ae,"error"),onGraphDataCopySuccess:()=>C("Graph JSON copied to clipboard!","success"),onGraphDataCopyError:()=>C("Copy failed","error"),isGraphRefreshing:se,onClipNode:f?Ht:void 0,onClipboardDrop:f?Mt:void 0,isConnected:Z.connected,supportsAuthoring:E,onCreateNode:E?Ye.openCreateNode:void 0,helpPanel:b&&ne?(Ae,Ze)=>p.jsx(RS,{activeTopic:_e,onNavigate:Re,onClose:()=>ge(!1),onToggleMaximize:Ae,isMaximized:Ze}):void 0})}),f&&Le&&p.jsxs(p.Fragment,{children:[p.jsx(vc,{className:$t.resizeHandle,"aria-label":"Resize clipboard"}),p.jsx(Tl,{defaultSize:"20%",minSize:"10%",maxSize:"40%",children:p.jsx(pS,{connected:Z.connected,onPasteToInput:fe})})]})]})]})}function qS(){const r=Ee.c(2),n=Qn[0].path;let s;r[0]===Symbol.for("react.memo_cache_sentinel")?(s=Qn.map(IS),r[0]=s):s=r[0];let c;return r[1]===Symbol.for("react.memo_cache_sentinel")?(c=p.jsx(Iy,{children:p.jsx(k3,{children:p.jsx(Pg,{children:p.jsxs(Fg,{children:[s,p.jsx(bf,{path:"*",element:p.jsx(ey,{to:n,replace:!0})})]})})})}),r[1]=c):c=r[1],c}function IS(r){return p.jsx(bf,{path:r.path,element:p.jsx(YS,{config:r},r.path)},r.path)}by.createRoot(document.getElementById("root")).render(p.jsx(w.StrictMode,{children:p.jsx(qS,{})}));
//# sourceMappingURL=index-BtNnzsds.js.map
