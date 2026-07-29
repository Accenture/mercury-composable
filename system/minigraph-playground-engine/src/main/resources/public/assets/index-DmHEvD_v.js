import{r as e,t}from"./rolldown-runtime-QTnfLwEv.js";import{a as n,i as r,n as i,r as a,t as o}from"./vendor-json-view-XsbV5yWD.js";import{a as s,c,d as l,f as u,i as d,l as f,n as p,o as m,r as h,s as g,t as _,u as v}from"./vendor-xyflow-CdI6jjFy.js";import{n as y,r as b,t as x}from"./vendor-markdown-Di8s8vbi.js";import{i as S,n as C,r as ee,t as te}from"./vendor-panels-CXf4xYpQ.js";(function(){let e=document.createElement(`link`).relList;if(e&&e.supports&&e.supports(`modulepreload`))return;for(let e of document.querySelectorAll(`link[rel="modulepreload"]`))n(e);new MutationObserver(e=>{for(let t of e)if(t.type===`childList`)for(let e of t.addedNodes)e.tagName===`LINK`&&e.rel===`modulepreload`&&n(e)}).observe(document,{childList:!0,subtree:!0});function t(e){let t={};return e.integrity&&(t.integrity=e.integrity),e.referrerPolicy&&(t.referrerPolicy=e.referrerPolicy),e.crossOrigin===`use-credentials`?t.credentials=`include`:e.crossOrigin===`anonymous`?t.credentials=`omit`:t.credentials=`same-origin`,t}function n(e){if(e.ep)return;e.ep=!0;let n=t(e);fetch(e.href,n)}})();var w=t((e=>{function t(e,t){var n=e.length;e.push(t);a:for(;0<n;){var r=n-1>>>1,a=e[r];if(0<i(a,t))e[r]=t,e[n]=a,n=r;else break a}}function n(e){return e.length===0?null:e[0]}function r(e){if(e.length===0)return null;var t=e[0],n=e.pop();if(n!==t){e[0]=n;a:for(var r=0,a=e.length,o=a>>>1;r<o;){var s=2*(r+1)-1,c=e[s],l=s+1,u=e[l];if(0>i(c,n))l<a&&0>i(u,c)?(e[r]=u,e[l]=n,r=l):(e[r]=c,e[s]=n,r=s);else if(l<a&&0>i(u,n))e[r]=u,e[l]=n,r=l;else break a}}return t}function i(e,t){var n=e.sortIndex-t.sortIndex;return n===0?e.id-t.id:n}if(e.unstable_now=void 0,typeof performance==`object`&&typeof performance.now==`function`){var a=performance;e.unstable_now=function(){return a.now()}}else{var o=Date,s=o.now();e.unstable_now=function(){return o.now()-s}}var c=[],l=[],u=1,d=null,f=3,p=!1,m=!1,h=!1,g=!1,_=typeof setTimeout==`function`?setTimeout:null,v=typeof clearTimeout==`function`?clearTimeout:null,y=typeof setImmediate<`u`?setImmediate:null;function b(e){for(var i=n(l);i!==null;){if(i.callback===null)r(l);else if(i.startTime<=e)r(l),i.sortIndex=i.expirationTime,t(c,i);else break;i=n(l)}}function x(e){if(h=!1,b(e),!m)if(n(c)!==null)m=!0,S||(S=!0,T());else{var t=n(l);t!==null&&E(x,t.startTime-e)}}var S=!1,C=-1,ee=5,te=-1;function w(){return g?!0:!(e.unstable_now()-te<ee)}function ne(){if(g=!1,S){var t=e.unstable_now();te=t;var i=!0;try{a:{m=!1,h&&(h=!1,v(C),C=-1),p=!0;var a=f;try{b:{for(b(t),d=n(c);d!==null&&!(d.expirationTime>t&&w());){var o=d.callback;if(typeof o==`function`){d.callback=null,f=d.priorityLevel;var s=o(d.expirationTime<=t);if(t=e.unstable_now(),typeof s==`function`){d.callback=s,b(t),i=!0;break b}d===n(c)&&r(c),b(t)}else r(c);d=n(c)}if(d!==null)i=!0;else{var u=n(l);u!==null&&E(x,u.startTime-t),i=!1}}break a}finally{d=null,f=a,p=!1}i=void 0}}finally{i?T():S=!1}}}var T;if(typeof y==`function`)T=function(){y(ne)};else if(typeof MessageChannel<`u`){var re=new MessageChannel,ie=re.port2;re.port1.onmessage=ne,T=function(){ie.postMessage(null)}}else T=function(){_(ne,0)};function E(t,n){C=_(function(){t(e.unstable_now())},n)}e.unstable_IdlePriority=5,e.unstable_ImmediatePriority=1,e.unstable_LowPriority=4,e.unstable_NormalPriority=3,e.unstable_Profiling=null,e.unstable_UserBlockingPriority=2,e.unstable_cancelCallback=function(e){e.callback=null},e.unstable_forceFrameRate=function(e){0>e||125<e?console.error(`forceFrameRate takes a positive int between 0 and 125, forcing frame rates higher than 125 fps is not supported`):ee=0<e?Math.floor(1e3/e):5},e.unstable_getCurrentPriorityLevel=function(){return f},e.unstable_next=function(e){switch(f){case 1:case 2:case 3:var t=3;break;default:t=f}var n=f;f=t;try{return e()}finally{f=n}},e.unstable_requestPaint=function(){g=!0},e.unstable_runWithPriority=function(e,t){switch(e){case 1:case 2:case 3:case 4:case 5:break;default:e=3}var n=f;f=e;try{return t()}finally{f=n}},e.unstable_scheduleCallback=function(r,i,a){var o=e.unstable_now();switch(typeof a==`object`&&a?(a=a.delay,a=typeof a==`number`&&0<a?o+a:o):a=o,r){case 1:var s=-1;break;case 2:s=250;break;case 5:s=1073741823;break;case 4:s=1e4;break;default:s=5e3}return s=a+s,r={id:u++,callback:i,priorityLevel:r,startTime:a,expirationTime:s,sortIndex:-1},a>o?(r.sortIndex=a,t(l,r),n(c)===null&&r===n(l)&&(h?(v(C),C=-1):h=!0,E(x,a-o))):(r.sortIndex=s,t(c,r),m||p||(m=!0,S||(S=!0,T()))),r},e.unstable_shouldYield=w,e.unstable_wrapCallback=function(e){var t=f;return function(){var n=f;f=t;try{return e.apply(this,arguments)}finally{f=n}}}})),ne=t(((e,t)=>{t.exports=w()})),T=t((e=>{var t=ne(),r=n(),i=u();function a(e){var t=`https://react.dev/errors/`+e;if(1<arguments.length){t+=`?args[]=`+encodeURIComponent(arguments[1]);for(var n=2;n<arguments.length;n++)t+=`&args[]=`+encodeURIComponent(arguments[n])}return`Minified React error #`+e+`; visit `+t+` for the full message or use the non-minified dev environment for full errors and additional helpful warnings.`}function o(e){return!(!e||e.nodeType!==1&&e.nodeType!==9&&e.nodeType!==11)}function s(e){var t=e,n=e;if(e.alternate)for(;t.return;)t=t.return;else{e=t;do t=e,t.flags&4098&&(n=t.return),e=t.return;while(e)}return t.tag===3?n:null}function c(e){if(e.tag===13){var t=e.memoizedState;if(t===null&&(e=e.alternate,e!==null&&(t=e.memoizedState)),t!==null)return t.dehydrated}return null}function l(e){if(e.tag===31){var t=e.memoizedState;if(t===null&&(e=e.alternate,e!==null&&(t=e.memoizedState)),t!==null)return t.dehydrated}return null}function d(e){if(s(e)!==e)throw Error(a(188))}function f(e){var t=e.alternate;if(!t){if(t=s(e),t===null)throw Error(a(188));return t===e?e:null}for(var n=e,r=t;;){var i=n.return;if(i===null)break;var o=i.alternate;if(o===null){if(r=i.return,r!==null){n=r;continue}break}if(i.child===o.child){for(o=i.child;o;){if(o===n)return d(i),e;if(o===r)return d(i),t;o=o.sibling}throw Error(a(188))}if(n.return!==r.return)n=i,r=o;else{for(var c=!1,l=i.child;l;){if(l===n){c=!0,n=i,r=o;break}if(l===r){c=!0,r=i,n=o;break}l=l.sibling}if(!c){for(l=o.child;l;){if(l===n){c=!0,n=o,r=i;break}if(l===r){c=!0,r=o,n=i;break}l=l.sibling}if(!c)throw Error(a(189))}}if(n.alternate!==r)throw Error(a(190))}if(n.tag!==3)throw Error(a(188));return n.stateNode.current===n?e:t}function p(e){var t=e.tag;if(t===5||t===26||t===27||t===6)return e;for(e=e.child;e!==null;){if(t=p(e),t!==null)return t;e=e.sibling}return null}var m=Object.assign,h=Symbol.for(`react.element`),g=Symbol.for(`react.transitional.element`),_=Symbol.for(`react.portal`),v=Symbol.for(`react.fragment`),y=Symbol.for(`react.strict_mode`),b=Symbol.for(`react.profiler`),x=Symbol.for(`react.consumer`),S=Symbol.for(`react.context`),C=Symbol.for(`react.forward_ref`),ee=Symbol.for(`react.suspense`),te=Symbol.for(`react.suspense_list`),w=Symbol.for(`react.memo`),T=Symbol.for(`react.lazy`),re=Symbol.for(`react.activity`),ie=Symbol.for(`react.memo_cache_sentinel`),E=Symbol.iterator;function ae(e){return typeof e!=`object`||!e?null:(e=E&&e[E]||e[`@@iterator`],typeof e==`function`?e:null)}var oe=Symbol.for(`react.client.reference`);function se(e){if(e==null)return null;if(typeof e==`function`)return e.$$typeof===oe?null:e.displayName||e.name||null;if(typeof e==`string`)return e;switch(e){case v:return`Fragment`;case b:return`Profiler`;case y:return`StrictMode`;case ee:return`Suspense`;case te:return`SuspenseList`;case re:return`Activity`}if(typeof e==`object`)switch(e.$$typeof){case _:return`Portal`;case S:return e.displayName||`Context`;case x:return(e._context.displayName||`Context`)+`.Consumer`;case C:var t=e.render;return e=e.displayName,e||=(e=t.displayName||t.name||``,e===``?`ForwardRef`:`ForwardRef(`+e+`)`),e;case w:return t=e.displayName||null,t===null?se(e.type)||`Memo`:t;case T:t=e._payload,e=e._init;try{return se(e(t))}catch{}}return null}var ce=Array.isArray,D=r.__CLIENT_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,O=i.__DOM_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,le={pending:!1,data:null,method:null,action:null},ue=[],de=-1;function fe(e){return{current:e}}function k(e){0>de||(e.current=ue[de],ue[de]=null,de--)}function A(e,t){de++,ue[de]=e.current,e.current=t}var pe=fe(null),j=fe(null),me=fe(null),he=fe(null);function ge(e,t){switch(A(me,t),A(j,e),A(pe,null),t.nodeType){case 9:case 11:e=(e=t.documentElement)&&(e=e.namespaceURI)?Vd(e):0;break;default:if(e=t.tagName,t=t.namespaceURI)t=Vd(t),e=Hd(t,e);else switch(e){case`svg`:e=1;break;case`math`:e=2;break;default:e=0}}k(pe),A(pe,e)}function _e(){k(pe),k(j),k(me)}function ve(e){e.memoizedState!==null&&A(he,e);var t=pe.current,n=Hd(t,e.type);t!==n&&(A(j,e),A(pe,n))}function ye(e){j.current===e&&(k(pe),k(j)),he.current===e&&(k(he),Qf._currentValue=le)}var be,xe;function Se(e){if(be===void 0)try{throw Error()}catch(e){var t=e.stack.trim().match(/\n( *(at )?)/);be=t&&t[1]||``,xe=-1<e.stack.indexOf(`
    at`)?` (<anonymous>)`:-1<e.stack.indexOf(`@`)?`@unknown:0:0`:``}return`
`+be+e+xe}var Ce=!1;function we(e,t){if(!e||Ce)return``;Ce=!0;var n=Error.prepareStackTrace;Error.prepareStackTrace=void 0;try{var r={DetermineComponentFrameRoot:function(){try{if(t){var n=function(){throw Error()};if(Object.defineProperty(n.prototype,"props",{set:function(){throw Error()}}),typeof Reflect==`object`&&Reflect.construct){try{Reflect.construct(n,[])}catch(e){var r=e}Reflect.construct(e,[],n)}else{try{n.call()}catch(e){r=e}e.call(n.prototype)}}else{try{throw Error()}catch(e){r=e}(n=e())&&typeof n.catch==`function`&&n.catch(function(){})}}catch(e){if(e&&r&&typeof e.stack==`string`)return[e.stack,r.stack]}return[null,null]}};r.DetermineComponentFrameRoot.displayName=`DetermineComponentFrameRoot`;var i=Object.getOwnPropertyDescriptor(r.DetermineComponentFrameRoot,`name`);i&&i.configurable&&Object.defineProperty(r.DetermineComponentFrameRoot,"name",{value:`DetermineComponentFrameRoot`});var a=r.DetermineComponentFrameRoot(),o=a[0],s=a[1];if(o&&s){var c=o.split(`
`),l=s.split(`
`);for(i=r=0;r<c.length&&!c[r].includes(`DetermineComponentFrameRoot`);)r++;for(;i<l.length&&!l[i].includes(`DetermineComponentFrameRoot`);)i++;if(r===c.length||i===l.length)for(r=c.length-1,i=l.length-1;1<=r&&0<=i&&c[r]!==l[i];)i--;for(;1<=r&&0<=i;r--,i--)if(c[r]!==l[i]){if(r!==1||i!==1)do if(r--,i--,0>i||c[r]!==l[i]){var u=`
`+c[r].replace(` at new `,` at `);return e.displayName&&u.includes(`<anonymous>`)&&(u=u.replace(`<anonymous>`,e.displayName)),u}while(1<=r&&0<=i);break}}}finally{Ce=!1,Error.prepareStackTrace=n}return(n=e?e.displayName||e.name:``)?Se(n):``}function Te(e,t){switch(e.tag){case 26:case 27:case 5:return Se(e.type);case 16:return Se(`Lazy`);case 13:return e.child!==t&&t!==null?Se(`Suspense Fallback`):Se(`Suspense`);case 19:return Se(`SuspenseList`);case 0:case 15:return we(e.type,!1);case 11:return we(e.type.render,!1);case 1:return we(e.type,!0);case 31:return Se(`Activity`);default:return``}}function Ee(e){try{var t=``,n=null;do t+=Te(e,n),n=e,e=e.return;while(e);return t}catch(e){return`
Error generating stack: `+e.message+`
`+e.stack}}var De=Object.prototype.hasOwnProperty,Oe=t.unstable_scheduleCallback,ke=t.unstable_cancelCallback,Ae=t.unstable_shouldYield,je=t.unstable_requestPaint,Me=t.unstable_now,Ne=t.unstable_getCurrentPriorityLevel,Pe=t.unstable_ImmediatePriority,Fe=t.unstable_UserBlockingPriority,Ie=t.unstable_NormalPriority,Le=t.unstable_LowPriority,Re=t.unstable_IdlePriority,ze=t.log,Be=t.unstable_setDisableYieldValue,Ve=null,He=null;function Ue(e){if(typeof ze==`function`&&Be(e),He&&typeof He.setStrictMode==`function`)try{He.setStrictMode(Ve,e)}catch{}}var We=Math.clz32?Math.clz32:qe,Ge=Math.log,Ke=Math.LN2;function qe(e){return e>>>=0,e===0?32:31-(Ge(e)/Ke|0)|0}var Je=256,Ye=262144,Xe=4194304;function Ze(e){var t=e&42;if(t!==0)return t;switch(e&-e){case 1:return 1;case 2:return 2;case 4:return 4;case 8:return 8;case 16:return 16;case 32:return 32;case 64:return 64;case 128:return 128;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:return e&261888;case 262144:case 524288:case 1048576:case 2097152:return e&3932160;case 4194304:case 8388608:case 16777216:case 33554432:return e&62914560;case 67108864:return 67108864;case 134217728:return 134217728;case 268435456:return 268435456;case 536870912:return 536870912;case 1073741824:return 0;default:return e}}function Qe(e,t,n){var r=e.pendingLanes;if(r===0)return 0;var i=0,a=e.suspendedLanes,o=e.pingedLanes;e=e.warmLanes;var s=r&134217727;return s===0?(s=r&~a,s===0?o===0?n||(n=r&~e,n!==0&&(i=Ze(n))):i=Ze(o):i=Ze(s)):(r=s&~a,r===0?(o&=s,o===0?n||(n=s&~e,n!==0&&(i=Ze(n))):i=Ze(o)):i=Ze(r)),i===0?0:t!==0&&t!==i&&(t&a)===0&&(a=i&-i,n=t&-t,a>=n||a===32&&n&4194048)?t:i}function $e(e,t){return(e.pendingLanes&~(e.suspendedLanes&~e.pingedLanes)&t)===0}function et(e,t){switch(e){case 1:case 2:case 4:case 8:case 64:return t+250;case 16:case 32:case 128:case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:return t+5e3;case 4194304:case 8388608:case 16777216:case 33554432:return-1;case 67108864:case 134217728:case 268435456:case 536870912:case 1073741824:return-1;default:return-1}}function tt(){var e=Xe;return Xe<<=1,!(Xe&62914560)&&(Xe=4194304),e}function nt(e){for(var t=[],n=0;31>n;n++)t.push(e);return t}function rt(e,t){e.pendingLanes|=t,t!==268435456&&(e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0)}function it(e,t,n,r,i,a){var o=e.pendingLanes;e.pendingLanes=n,e.suspendedLanes=0,e.pingedLanes=0,e.warmLanes=0,e.expiredLanes&=n,e.entangledLanes&=n,e.errorRecoveryDisabledLanes&=n,e.shellSuspendCounter=0;var s=e.entanglements,c=e.expirationTimes,l=e.hiddenUpdates;for(n=o&~n;0<n;){var u=31-We(n),d=1<<u;s[u]=0,c[u]=-1;var f=l[u];if(f!==null)for(l[u]=null,u=0;u<f.length;u++){var p=f[u];p!==null&&(p.lane&=-536870913)}n&=~d}r!==0&&at(e,r,0),a!==0&&i===0&&e.tag!==0&&(e.suspendedLanes|=a&~(o&~t))}function at(e,t,n){e.pendingLanes|=t,e.suspendedLanes&=~t;var r=31-We(t);e.entangledLanes|=t,e.entanglements[r]=e.entanglements[r]|1073741824|n&261930}function ot(e,t){var n=e.entangledLanes|=t;for(e=e.entanglements;n;){var r=31-We(n),i=1<<r;i&t|e[r]&t&&(e[r]|=t),n&=~i}}function st(e,t){var n=t&-t;return n=n&42?1:ct(n),(n&(e.suspendedLanes|t))===0?n:0}function ct(e){switch(e){case 2:e=1;break;case 8:e=4;break;case 32:e=16;break;case 256:case 512:case 1024:case 2048:case 4096:case 8192:case 16384:case 32768:case 65536:case 131072:case 262144:case 524288:case 1048576:case 2097152:case 4194304:case 8388608:case 16777216:case 33554432:e=128;break;case 268435456:e=134217728;break;default:e=0}return e}function lt(e){return e&=-e,2<e?8<e?e&134217727?32:268435456:8:2}function ut(){var e=O.p;return e===0?(e=window.event,e===void 0?32:mp(e.type)):e}function dt(e,t){var n=O.p;try{return O.p=e,t()}finally{O.p=n}}var ft=Math.random().toString(36).slice(2),pt=`__reactFiber$`+ft,mt=`__reactProps$`+ft,ht=`__reactContainer$`+ft,gt=`__reactEvents$`+ft,_t=`__reactListeners$`+ft,vt=`__reactHandles$`+ft,yt=`__reactResources$`+ft,bt=`__reactMarker$`+ft;function xt(e){delete e[pt],delete e[mt],delete e[gt],delete e[_t],delete e[vt]}function St(e){var t=e[pt];if(t)return t;for(var n=e.parentNode;n;){if(t=n[ht]||n[pt]){if(n=t.alternate,t.child!==null||n!==null&&n.child!==null)for(e=df(e);e!==null;){if(n=e[pt])return n;e=df(e)}return t}e=n,n=e.parentNode}return null}function Ct(e){if(e=e[pt]||e[ht]){var t=e.tag;if(t===5||t===6||t===13||t===31||t===26||t===27||t===3)return e}return null}function wt(e){var t=e.tag;if(t===5||t===26||t===27||t===6)return e.stateNode;throw Error(a(33))}function Tt(e){var t=e[yt];return t||=e[yt]={hoistableStyles:new Map,hoistableScripts:new Map},t}function Et(e){e[bt]=!0}var Dt=new Set,Ot={};function kt(e,t){At(e,t),At(e+`Capture`,t)}function At(e,t){for(Ot[e]=t,e=0;e<t.length;e++)Dt.add(t[e])}var jt=RegExp(`^[:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD][:A-Z_a-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD\\-.0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040]*$`),Mt={},Nt={};function Pt(e){return De.call(Nt,e)?!0:De.call(Mt,e)?!1:jt.test(e)?Nt[e]=!0:(Mt[e]=!0,!1)}function Ft(e,t,n){if(Pt(t))if(n===null)e.removeAttribute(t);else{switch(typeof n){case`undefined`:case`function`:case`symbol`:e.removeAttribute(t);return;case`boolean`:var r=t.toLowerCase().slice(0,5);if(r!==`data-`&&r!==`aria-`){e.removeAttribute(t);return}}e.setAttribute(t,``+n)}}function It(e,t,n){if(n===null)e.removeAttribute(t);else{switch(typeof n){case`undefined`:case`function`:case`symbol`:case`boolean`:e.removeAttribute(t);return}e.setAttribute(t,``+n)}}function Lt(e,t,n,r){if(r===null)e.removeAttribute(n);else{switch(typeof r){case`undefined`:case`function`:case`symbol`:case`boolean`:e.removeAttribute(n);return}e.setAttributeNS(t,n,``+r)}}function Rt(e){switch(typeof e){case`bigint`:case`boolean`:case`number`:case`string`:case`undefined`:return e;case`object`:return e;default:return``}}function zt(e){var t=e.type;return(e=e.nodeName)&&e.toLowerCase()===`input`&&(t===`checkbox`||t===`radio`)}function Bt(e,t,n){var r=Object.getOwnPropertyDescriptor(e.constructor.prototype,t);if(!e.hasOwnProperty(t)&&r!==void 0&&typeof r.get==`function`&&typeof r.set==`function`){var i=r.get,a=r.set;return Object.defineProperty(e,t,{configurable:!0,get:function(){return i.call(this)},set:function(e){n=``+e,a.call(this,e)}}),Object.defineProperty(e,t,{enumerable:r.enumerable}),{getValue:function(){return n},setValue:function(e){n=``+e},stopTracking:function(){e._valueTracker=null,delete e[t]}}}}function Vt(e){if(!e._valueTracker){var t=zt(e)?`checked`:`value`;e._valueTracker=Bt(e,t,``+e[t])}}function Ht(e){if(!e)return!1;var t=e._valueTracker;if(!t)return!0;var n=t.getValue(),r=``;return e&&(r=zt(e)?e.checked?`true`:`false`:e.value),e=r,e===n?!1:(t.setValue(e),!0)}function Ut(e){if(e||=typeof document<`u`?document:void 0,e===void 0)return null;try{return e.activeElement||e.body}catch{return e.body}}var Wt=/[\n"\\]/g;function Gt(e){return e.replace(Wt,function(e){return`\\`+e.charCodeAt(0).toString(16)+` `})}function Kt(e,t,n,r,i,a,o,s){e.name=``,o!=null&&typeof o!=`function`&&typeof o!=`symbol`&&typeof o!=`boolean`?e.type=o:e.removeAttribute(`type`),t==null?o!==`submit`&&o!==`reset`||e.removeAttribute(`value`):o===`number`?(t===0&&e.value===``||e.value!=t)&&(e.value=``+Rt(t)):e.value!==``+Rt(t)&&(e.value=``+Rt(t)),t==null?n==null?r!=null&&e.removeAttribute(`value`):Jt(e,o,Rt(n)):Jt(e,o,Rt(t)),i==null&&a!=null&&(e.defaultChecked=!!a),i!=null&&(e.checked=i&&typeof i!=`function`&&typeof i!=`symbol`),s!=null&&typeof s!=`function`&&typeof s!=`symbol`&&typeof s!=`boolean`?e.name=``+Rt(s):e.removeAttribute(`name`)}function qt(e,t,n,r,i,a,o,s){if(a!=null&&typeof a!=`function`&&typeof a!=`symbol`&&typeof a!=`boolean`&&(e.type=a),t!=null||n!=null){if(!(a!==`submit`&&a!==`reset`||t!=null)){Vt(e);return}n=n==null?``:``+Rt(n),t=t==null?n:``+Rt(t),s||t===e.value||(e.value=t),e.defaultValue=t}r??=i,r=typeof r!=`function`&&typeof r!=`symbol`&&!!r,e.checked=s?e.checked:!!r,e.defaultChecked=!!r,o!=null&&typeof o!=`function`&&typeof o!=`symbol`&&typeof o!=`boolean`&&(e.name=o),Vt(e)}function Jt(e,t,n){t===`number`&&Ut(e.ownerDocument)===e||e.defaultValue===``+n||(e.defaultValue=``+n)}function Yt(e,t,n,r){if(e=e.options,t){t={};for(var i=0;i<n.length;i++)t[`$`+n[i]]=!0;for(n=0;n<e.length;n++)i=t.hasOwnProperty(`$`+e[n].value),e[n].selected!==i&&(e[n].selected=i),i&&r&&(e[n].defaultSelected=!0)}else{for(n=``+Rt(n),t=null,i=0;i<e.length;i++){if(e[i].value===n){e[i].selected=!0,r&&(e[i].defaultSelected=!0);return}t!==null||e[i].disabled||(t=e[i])}t!==null&&(t.selected=!0)}}function Xt(e,t,n){if(t!=null&&(t=``+Rt(t),t!==e.value&&(e.value=t),n==null)){e.defaultValue!==t&&(e.defaultValue=t);return}e.defaultValue=n==null?``:``+Rt(n)}function Zt(e,t,n,r){if(t==null){if(r!=null){if(n!=null)throw Error(a(92));if(ce(r)){if(1<r.length)throw Error(a(93));r=r[0]}n=r}n??=``,t=n}n=Rt(t),e.defaultValue=n,r=e.textContent,r===n&&r!==``&&r!==null&&(e.value=r),Vt(e)}function Qt(e,t){if(t){var n=e.firstChild;if(n&&n===e.lastChild&&n.nodeType===3){n.nodeValue=t;return}}e.textContent=t}var $t=new Set(`animationIterationCount aspectRatio borderImageOutset borderImageSlice borderImageWidth boxFlex boxFlexGroup boxOrdinalGroup columnCount columns flex flexGrow flexPositive flexShrink flexNegative flexOrder gridArea gridRow gridRowEnd gridRowSpan gridRowStart gridColumn gridColumnEnd gridColumnSpan gridColumnStart fontWeight lineClamp lineHeight opacity order orphans scale tabSize widows zIndex zoom fillOpacity floodOpacity stopOpacity strokeDasharray strokeDashoffset strokeMiterlimit strokeOpacity strokeWidth MozAnimationIterationCount MozBoxFlex MozBoxFlexGroup MozLineClamp msAnimationIterationCount msFlex msZoom msFlexGrow msFlexNegative msFlexOrder msFlexPositive msFlexShrink msGridColumn msGridColumnSpan msGridRow msGridRowSpan WebkitAnimationIterationCount WebkitBoxFlex WebKitBoxFlexGroup WebkitBoxOrdinalGroup WebkitColumnCount WebkitColumns WebkitFlex WebkitFlexGrow WebkitFlexPositive WebkitFlexShrink WebkitLineClamp`.split(` `));function en(e,t,n){var r=t.indexOf(`--`)===0;n==null||typeof n==`boolean`||n===``?r?e.setProperty(t,``):t===`float`?e.cssFloat=``:e[t]=``:r?e.setProperty(t,n):typeof n!=`number`||n===0||$t.has(t)?t===`float`?e.cssFloat=n:e[t]=(``+n).trim():e[t]=n+`px`}function tn(e,t,n){if(t!=null&&typeof t!=`object`)throw Error(a(62));if(e=e.style,n!=null){for(var r in n)!n.hasOwnProperty(r)||t!=null&&t.hasOwnProperty(r)||(r.indexOf(`--`)===0?e.setProperty(r,``):r===`float`?e.cssFloat=``:e[r]=``);for(var i in t)r=t[i],t.hasOwnProperty(i)&&n[i]!==r&&en(e,i,r)}else for(var o in t)t.hasOwnProperty(o)&&en(e,o,t[o])}function nn(e){if(e.indexOf(`-`)===-1)return!1;switch(e){case`annotation-xml`:case`color-profile`:case`font-face`:case`font-face-src`:case`font-face-uri`:case`font-face-format`:case`font-face-name`:case`missing-glyph`:return!1;default:return!0}}var rn=new Map([[`acceptCharset`,`accept-charset`],[`htmlFor`,`for`],[`httpEquiv`,`http-equiv`],[`crossOrigin`,`crossorigin`],[`accentHeight`,`accent-height`],[`alignmentBaseline`,`alignment-baseline`],[`arabicForm`,`arabic-form`],[`baselineShift`,`baseline-shift`],[`capHeight`,`cap-height`],[`clipPath`,`clip-path`],[`clipRule`,`clip-rule`],[`colorInterpolation`,`color-interpolation`],[`colorInterpolationFilters`,`color-interpolation-filters`],[`colorProfile`,`color-profile`],[`colorRendering`,`color-rendering`],[`dominantBaseline`,`dominant-baseline`],[`enableBackground`,`enable-background`],[`fillOpacity`,`fill-opacity`],[`fillRule`,`fill-rule`],[`floodColor`,`flood-color`],[`floodOpacity`,`flood-opacity`],[`fontFamily`,`font-family`],[`fontSize`,`font-size`],[`fontSizeAdjust`,`font-size-adjust`],[`fontStretch`,`font-stretch`],[`fontStyle`,`font-style`],[`fontVariant`,`font-variant`],[`fontWeight`,`font-weight`],[`glyphName`,`glyph-name`],[`glyphOrientationHorizontal`,`glyph-orientation-horizontal`],[`glyphOrientationVertical`,`glyph-orientation-vertical`],[`horizAdvX`,`horiz-adv-x`],[`horizOriginX`,`horiz-origin-x`],[`imageRendering`,`image-rendering`],[`letterSpacing`,`letter-spacing`],[`lightingColor`,`lighting-color`],[`markerEnd`,`marker-end`],[`markerMid`,`marker-mid`],[`markerStart`,`marker-start`],[`overlinePosition`,`overline-position`],[`overlineThickness`,`overline-thickness`],[`paintOrder`,`paint-order`],[`panose-1`,`panose-1`],[`pointerEvents`,`pointer-events`],[`renderingIntent`,`rendering-intent`],[`shapeRendering`,`shape-rendering`],[`stopColor`,`stop-color`],[`stopOpacity`,`stop-opacity`],[`strikethroughPosition`,`strikethrough-position`],[`strikethroughThickness`,`strikethrough-thickness`],[`strokeDasharray`,`stroke-dasharray`],[`strokeDashoffset`,`stroke-dashoffset`],[`strokeLinecap`,`stroke-linecap`],[`strokeLinejoin`,`stroke-linejoin`],[`strokeMiterlimit`,`stroke-miterlimit`],[`strokeOpacity`,`stroke-opacity`],[`strokeWidth`,`stroke-width`],[`textAnchor`,`text-anchor`],[`textDecoration`,`text-decoration`],[`textRendering`,`text-rendering`],[`transformOrigin`,`transform-origin`],[`underlinePosition`,`underline-position`],[`underlineThickness`,`underline-thickness`],[`unicodeBidi`,`unicode-bidi`],[`unicodeRange`,`unicode-range`],[`unitsPerEm`,`units-per-em`],[`vAlphabetic`,`v-alphabetic`],[`vHanging`,`v-hanging`],[`vIdeographic`,`v-ideographic`],[`vMathematical`,`v-mathematical`],[`vectorEffect`,`vector-effect`],[`vertAdvY`,`vert-adv-y`],[`vertOriginX`,`vert-origin-x`],[`vertOriginY`,`vert-origin-y`],[`wordSpacing`,`word-spacing`],[`writingMode`,`writing-mode`],[`xmlnsXlink`,`xmlns:xlink`],[`xHeight`,`x-height`]]),an=/^[\u0000-\u001F ]*j[\r\n\t]*a[\r\n\t]*v[\r\n\t]*a[\r\n\t]*s[\r\n\t]*c[\r\n\t]*r[\r\n\t]*i[\r\n\t]*p[\r\n\t]*t[\r\n\t]*:/i;function on(e){return an.test(``+e)?`javascript:throw new Error('React has blocked a javascript: URL as a security precaution.')`:e}function sn(){}var cn=null;function ln(e){return e=e.target||e.srcElement||window,e.correspondingUseElement&&(e=e.correspondingUseElement),e.nodeType===3?e.parentNode:e}var un=null,dn=null;function fn(e){var t=Ct(e);if(t&&(e=t.stateNode)){var n=e[mt]||null;a:switch(e=t.stateNode,t.type){case`input`:if(Kt(e,n.value,n.defaultValue,n.defaultValue,n.checked,n.defaultChecked,n.type,n.name),t=n.name,n.type===`radio`&&t!=null){for(n=e;n.parentNode;)n=n.parentNode;for(n=n.querySelectorAll(`input[name="`+Gt(``+t)+`"][type="radio"]`),t=0;t<n.length;t++){var r=n[t];if(r!==e&&r.form===e.form){var i=r[mt]||null;if(!i)throw Error(a(90));Kt(r,i.value,i.defaultValue,i.defaultValue,i.checked,i.defaultChecked,i.type,i.name)}}for(t=0;t<n.length;t++)r=n[t],r.form===e.form&&Ht(r)}break a;case`textarea`:Xt(e,n.value,n.defaultValue);break a;case`select`:t=n.value,t!=null&&Yt(e,!!n.multiple,t,!1)}}}var pn=!1;function mn(e,t,n){if(pn)return e(t,n);pn=!0;try{return e(t)}finally{if(pn=!1,(un!==null||dn!==null)&&(bu(),un&&(t=un,e=dn,dn=un=null,fn(t),e)))for(t=0;t<e.length;t++)fn(e[t])}}function hn(e,t){var n=e.stateNode;if(n===null)return null;var r=n[mt]||null;if(r===null)return null;n=r[t];a:switch(t){case`onClick`:case`onClickCapture`:case`onDoubleClick`:case`onDoubleClickCapture`:case`onMouseDown`:case`onMouseDownCapture`:case`onMouseMove`:case`onMouseMoveCapture`:case`onMouseUp`:case`onMouseUpCapture`:case`onMouseEnter`:(r=!r.disabled)||(e=e.type,r=!(e===`button`||e===`input`||e===`select`||e===`textarea`)),e=!r;break a;default:e=!1}if(e)return null;if(n&&typeof n!=`function`)throw Error(a(231,t,typeof n));return n}var gn=!(typeof window>`u`||window.document===void 0||window.document.createElement===void 0),_n=!1;if(gn)try{var vn={};Object.defineProperty(vn,"passive",{get:function(){_n=!0}}),window.addEventListener(`test`,vn,vn),window.removeEventListener(`test`,vn,vn)}catch{_n=!1}var yn=null,bn=null,xn=null;function Sn(){if(xn)return xn;var e,t=bn,n=t.length,r,i=`value`in yn?yn.value:yn.textContent,a=i.length;for(e=0;e<n&&t[e]===i[e];e++);var o=n-e;for(r=1;r<=o&&t[n-r]===i[a-r];r++);return xn=i.slice(e,1<r?1-r:void 0)}function Cn(e){var t=e.keyCode;return`charCode`in e?(e=e.charCode,e===0&&t===13&&(e=13)):e=t,e===10&&(e=13),32<=e||e===13?e:0}function wn(){return!0}function Tn(){return!1}function En(e){function t(t,n,r,i,a){for(var o in this._reactName=t,this._targetInst=r,this.type=n,this.nativeEvent=i,this.target=a,this.currentTarget=null,e)e.hasOwnProperty(o)&&(t=e[o],this[o]=t?t(i):i[o]);return this.isDefaultPrevented=(i.defaultPrevented==null?!1===i.returnValue:i.defaultPrevented)?wn:Tn,this.isPropagationStopped=Tn,this}return m(t.prototype,{preventDefault:function(){this.defaultPrevented=!0;var e=this.nativeEvent;e&&(e.preventDefault?e.preventDefault():typeof e.returnValue!=`unknown`&&(e.returnValue=!1),this.isDefaultPrevented=wn)},stopPropagation:function(){var e=this.nativeEvent;e&&(e.stopPropagation?e.stopPropagation():typeof e.cancelBubble!=`unknown`&&(e.cancelBubble=!0),this.isPropagationStopped=wn)},persist:function(){},isPersistent:wn}),t}var Dn={eventPhase:0,bubbles:0,cancelable:0,timeStamp:function(e){return e.timeStamp||Date.now()},defaultPrevented:0,isTrusted:0},On=En(Dn),kn=m({},Dn,{view:0,detail:0}),An=En(kn),jn,Mn,Nn,Pn=m({},kn,{screenX:0,screenY:0,clientX:0,clientY:0,pageX:0,pageY:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,getModifierState:Gn,button:0,buttons:0,relatedTarget:function(e){return e.relatedTarget===void 0?e.fromElement===e.srcElement?e.toElement:e.fromElement:e.relatedTarget},movementX:function(e){return`movementX`in e?e.movementX:(e!==Nn&&(Nn&&e.type===`mousemove`?(jn=e.screenX-Nn.screenX,Mn=e.screenY-Nn.screenY):Mn=jn=0,Nn=e),jn)},movementY:function(e){return`movementY`in e?e.movementY:Mn}}),Fn=En(Pn),In=En(m({},Pn,{dataTransfer:0})),Ln=En(m({},kn,{relatedTarget:0})),Rn=En(m({},Dn,{animationName:0,elapsedTime:0,pseudoElement:0})),zn=En(m({},Dn,{clipboardData:function(e){return`clipboardData`in e?e.clipboardData:window.clipboardData}})),Bn=En(m({},Dn,{data:0})),Vn={Esc:`Escape`,Spacebar:` `,Left:`ArrowLeft`,Up:`ArrowUp`,Right:`ArrowRight`,Down:`ArrowDown`,Del:`Delete`,Win:`OS`,Menu:`ContextMenu`,Apps:`ContextMenu`,Scroll:`ScrollLock`,MozPrintableKey:`Unidentified`},Hn={8:`Backspace`,9:`Tab`,12:`Clear`,13:`Enter`,16:`Shift`,17:`Control`,18:`Alt`,19:`Pause`,20:`CapsLock`,27:`Escape`,32:` `,33:`PageUp`,34:`PageDown`,35:`End`,36:`Home`,37:`ArrowLeft`,38:`ArrowUp`,39:`ArrowRight`,40:`ArrowDown`,45:`Insert`,46:`Delete`,112:`F1`,113:`F2`,114:`F3`,115:`F4`,116:`F5`,117:`F6`,118:`F7`,119:`F8`,120:`F9`,121:`F10`,122:`F11`,123:`F12`,144:`NumLock`,145:`ScrollLock`,224:`Meta`},Un={Alt:`altKey`,Control:`ctrlKey`,Meta:`metaKey`,Shift:`shiftKey`};function Wn(e){var t=this.nativeEvent;return t.getModifierState?t.getModifierState(e):(e=Un[e])?!!t[e]:!1}function Gn(){return Wn}var Kn=En(m({},kn,{key:function(e){if(e.key){var t=Vn[e.key]||e.key;if(t!==`Unidentified`)return t}return e.type===`keypress`?(e=Cn(e),e===13?`Enter`:String.fromCharCode(e)):e.type===`keydown`||e.type===`keyup`?Hn[e.keyCode]||`Unidentified`:``},code:0,location:0,ctrlKey:0,shiftKey:0,altKey:0,metaKey:0,repeat:0,locale:0,getModifierState:Gn,charCode:function(e){return e.type===`keypress`?Cn(e):0},keyCode:function(e){return e.type===`keydown`||e.type===`keyup`?e.keyCode:0},which:function(e){return e.type===`keypress`?Cn(e):e.type===`keydown`||e.type===`keyup`?e.keyCode:0}})),qn=En(m({},Pn,{pointerId:0,width:0,height:0,pressure:0,tangentialPressure:0,tiltX:0,tiltY:0,twist:0,pointerType:0,isPrimary:0})),Jn=En(m({},kn,{touches:0,targetTouches:0,changedTouches:0,altKey:0,metaKey:0,ctrlKey:0,shiftKey:0,getModifierState:Gn})),Yn=En(m({},Dn,{propertyName:0,elapsedTime:0,pseudoElement:0})),Xn=En(m({},Pn,{deltaX:function(e){return`deltaX`in e?e.deltaX:`wheelDeltaX`in e?-e.wheelDeltaX:0},deltaY:function(e){return`deltaY`in e?e.deltaY:`wheelDeltaY`in e?-e.wheelDeltaY:`wheelDelta`in e?-e.wheelDelta:0},deltaZ:0,deltaMode:0})),Zn=En(m({},Dn,{newState:0,oldState:0})),Qn=[9,13,27,32],$n=gn&&`CompositionEvent`in window,er=null;gn&&`documentMode`in document&&(er=document.documentMode);var tr=gn&&`TextEvent`in window&&!er,nr=gn&&(!$n||er&&8<er&&11>=er),rr=` `,ir=!1;function ar(e,t){switch(e){case`keyup`:return Qn.indexOf(t.keyCode)!==-1;case`keydown`:return t.keyCode!==229;case`keypress`:case`mousedown`:case`focusout`:return!0;default:return!1}}function or(e){return e=e.detail,typeof e==`object`&&`data`in e?e.data:null}var sr=!1;function cr(e,t){switch(e){case`compositionend`:return or(t);case`keypress`:return t.which===32?(ir=!0,rr):null;case`textInput`:return e=t.data,e===rr&&ir?null:e;default:return null}}function lr(e,t){if(sr)return e===`compositionend`||!$n&&ar(e,t)?(e=Sn(),xn=bn=yn=null,sr=!1,e):null;switch(e){case`paste`:return null;case`keypress`:if(!(t.ctrlKey||t.altKey||t.metaKey)||t.ctrlKey&&t.altKey){if(t.char&&1<t.char.length)return t.char;if(t.which)return String.fromCharCode(t.which)}return null;case`compositionend`:return nr&&t.locale!==`ko`?null:t.data;default:return null}}var ur={color:!0,date:!0,datetime:!0,"datetime-local":!0,email:!0,month:!0,number:!0,password:!0,range:!0,search:!0,tel:!0,text:!0,time:!0,url:!0,week:!0};function dr(e){var t=e&&e.nodeName&&e.nodeName.toLowerCase();return t===`input`?!!ur[e.type]:t===`textarea`}function fr(e,t,n,r){un?dn?dn.push(r):dn=[r]:un=r,t=Ed(t,`onChange`),0<t.length&&(n=new On(`onChange`,`change`,null,n,r),e.push({event:n,listeners:t}))}var pr=null,mr=null;function hr(e){yd(e,0)}function gr(e){if(Ht(wt(e)))return e}function _r(e,t){if(e===`change`)return t}var vr=!1;if(gn){var yr;if(gn){var br=`oninput`in document;if(!br){var xr=document.createElement(`div`);xr.setAttribute(`oninput`,`return;`),br=typeof xr.oninput==`function`}yr=br}else yr=!1;vr=yr&&(!document.documentMode||9<document.documentMode)}function M(){pr&&(pr.detachEvent(`onpropertychange`,Sr),mr=pr=null)}function Sr(e){if(e.propertyName===`value`&&gr(mr)){var t=[];fr(t,mr,e,ln(e)),mn(hr,t)}}function Cr(e,t,n){e===`focusin`?(M(),pr=t,mr=n,pr.attachEvent(`onpropertychange`,Sr)):e===`focusout`&&M()}function wr(e){if(e===`selectionchange`||e===`keyup`||e===`keydown`)return gr(mr)}function Tr(e,t){if(e===`click`)return gr(t)}function Er(e,t){if(e===`input`||e===`change`)return gr(t)}function Dr(e,t){return e===t&&(e!==0||1/e==1/t)||e!==e&&t!==t}var Or=typeof Object.is==`function`?Object.is:Dr;function kr(e,t){if(Or(e,t))return!0;if(typeof e!=`object`||!e||typeof t!=`object`||!t)return!1;var n=Object.keys(e),r=Object.keys(t);if(n.length!==r.length)return!1;for(r=0;r<n.length;r++){var i=n[r];if(!De.call(t,i)||!Or(e[i],t[i]))return!1}return!0}function Ar(e){for(;e&&e.firstChild;)e=e.firstChild;return e}function jr(e,t){var n=Ar(e);e=0;for(var r;n;){if(n.nodeType===3){if(r=e+n.textContent.length,e<=t&&r>=t)return{node:n,offset:t-e};e=r}a:{for(;n;){if(n.nextSibling){n=n.nextSibling;break a}n=n.parentNode}n=void 0}n=Ar(n)}}function Mr(e,t){return e&&t?e===t?!0:e&&e.nodeType===3?!1:t&&t.nodeType===3?Mr(e,t.parentNode):`contains`in e?e.contains(t):e.compareDocumentPosition?!!(e.compareDocumentPosition(t)&16):!1:!1}function Nr(e){e=e!=null&&e.ownerDocument!=null&&e.ownerDocument.defaultView!=null?e.ownerDocument.defaultView:window;for(var t=Ut(e.document);t instanceof e.HTMLIFrameElement;){try{var n=typeof t.contentWindow.location.href==`string`}catch{n=!1}if(n)e=t.contentWindow;else break;t=Ut(e.document)}return t}function Pr(e){var t=e&&e.nodeName&&e.nodeName.toLowerCase();return t&&(t===`input`&&(e.type===`text`||e.type===`search`||e.type===`tel`||e.type===`url`||e.type===`password`)||t===`textarea`||e.contentEditable===`true`)}var Fr=gn&&`documentMode`in document&&11>=document.documentMode,Ir=null,Lr=null,Rr=null,zr=!1;function Br(e,t,n){var r=n.window===n?n.document:n.nodeType===9?n:n.ownerDocument;zr||Ir==null||Ir!==Ut(r)||(r=Ir,`selectionStart`in r&&Pr(r)?r={start:r.selectionStart,end:r.selectionEnd}:(r=(r.ownerDocument&&r.ownerDocument.defaultView||window).getSelection(),r={anchorNode:r.anchorNode,anchorOffset:r.anchorOffset,focusNode:r.focusNode,focusOffset:r.focusOffset}),Rr&&kr(Rr,r)||(Rr=r,r=Ed(Lr,`onSelect`),0<r.length&&(t=new On(`onSelect`,`select`,null,t,n),e.push({event:t,listeners:r}),t.target=Ir)))}function Vr(e,t){var n={};return n[e.toLowerCase()]=t.toLowerCase(),n[`Webkit`+e]=`webkit`+t,n[`Moz`+e]=`moz`+t,n}var Hr={animationend:Vr(`Animation`,`AnimationEnd`),animationiteration:Vr(`Animation`,`AnimationIteration`),animationstart:Vr(`Animation`,`AnimationStart`),transitionrun:Vr(`Transition`,`TransitionRun`),transitionstart:Vr(`Transition`,`TransitionStart`),transitioncancel:Vr(`Transition`,`TransitionCancel`),transitionend:Vr(`Transition`,`TransitionEnd`)},Ur={},Wr={};gn&&(Wr=document.createElement(`div`).style,`AnimationEvent`in window||(delete Hr.animationend.animation,delete Hr.animationiteration.animation,delete Hr.animationstart.animation),`TransitionEvent`in window||delete Hr.transitionend.transition);function Gr(e){if(Ur[e])return Ur[e];if(!Hr[e])return e;var t=Hr[e],n;for(n in t)if(t.hasOwnProperty(n)&&n in Wr)return Ur[e]=t[n];return e}var Kr=Gr(`animationend`),qr=Gr(`animationiteration`),Jr=Gr(`animationstart`),Yr=Gr(`transitionrun`),Xr=Gr(`transitionstart`),Zr=Gr(`transitioncancel`),Qr=Gr(`transitionend`),$r=new Map,ei=`abort auxClick beforeToggle cancel canPlay canPlayThrough click close contextMenu copy cut drag dragEnd dragEnter dragExit dragLeave dragOver dragStart drop durationChange emptied encrypted ended error gotPointerCapture input invalid keyDown keyPress keyUp load loadedData loadedMetadata loadStart lostPointerCapture mouseDown mouseMove mouseOut mouseOver mouseUp paste pause play playing pointerCancel pointerDown pointerMove pointerOut pointerOver pointerUp progress rateChange reset resize seeked seeking stalled submit suspend timeUpdate touchCancel touchEnd touchStart volumeChange scroll toggle touchMove waiting wheel`.split(` `);ei.push(`scrollEnd`);function ti(e,t){$r.set(e,t),kt(t,[e])}var ni=typeof reportError==`function`?reportError:function(e){if(typeof window==`object`&&typeof window.ErrorEvent==`function`){var t=new window.ErrorEvent(`error`,{bubbles:!0,cancelable:!0,message:typeof e==`object`&&e&&typeof e.message==`string`?String(e.message):String(e),error:e});if(!window.dispatchEvent(t))return}else if(typeof process==`object`&&typeof process.emit==`function`){process.emit(`uncaughtException`,e);return}console.error(e)},ri=[],ii=0,ai=0;function oi(){for(var e=ii,t=ai=ii=0;t<e;){var n=ri[t];ri[t++]=null;var r=ri[t];ri[t++]=null;var i=ri[t];ri[t++]=null;var a=ri[t];if(ri[t++]=null,r!==null&&i!==null){var o=r.pending;o===null?i.next=i:(i.next=o.next,o.next=i),r.pending=i}a!==0&&ui(n,i,a)}}function si(e,t,n,r){ri[ii++]=e,ri[ii++]=t,ri[ii++]=n,ri[ii++]=r,ai|=r,e.lanes|=r,e=e.alternate,e!==null&&(e.lanes|=r)}function ci(e,t,n,r){return si(e,t,n,r),di(e)}function li(e,t){return si(e,null,null,t),di(e)}function ui(e,t,n){e.lanes|=n;var r=e.alternate;r!==null&&(r.lanes|=n);for(var i=!1,a=e.return;a!==null;)a.childLanes|=n,r=a.alternate,r!==null&&(r.childLanes|=n),a.tag===22&&(e=a.stateNode,e===null||e._visibility&1||(i=!0)),e=a,a=a.return;return e.tag===3?(a=e.stateNode,i&&t!==null&&(i=31-We(n),e=a.hiddenUpdates,r=e[i],r===null?e[i]=[t]:r.push(t),t.lane=n|536870912),a):null}function di(e){if(50<du)throw du=0,fu=null,Error(a(185));for(var t=e.return;t!==null;)e=t,t=e.return;return e.tag===3?e.stateNode:null}var fi={};function pi(e,t,n,r){this.tag=e,this.key=n,this.sibling=this.child=this.return=this.stateNode=this.type=this.elementType=null,this.index=0,this.refCleanup=this.ref=null,this.pendingProps=t,this.dependencies=this.memoizedState=this.updateQueue=this.memoizedProps=null,this.mode=r,this.subtreeFlags=this.flags=0,this.deletions=null,this.childLanes=this.lanes=0,this.alternate=null}function mi(e,t,n,r){return new pi(e,t,n,r)}function hi(e){return e=e.prototype,!(!e||!e.isReactComponent)}function gi(e,t){var n=e.alternate;return n===null?(n=mi(e.tag,t,e.key,e.mode),n.elementType=e.elementType,n.type=e.type,n.stateNode=e.stateNode,n.alternate=e,e.alternate=n):(n.pendingProps=t,n.type=e.type,n.flags=0,n.subtreeFlags=0,n.deletions=null),n.flags=e.flags&65011712,n.childLanes=e.childLanes,n.lanes=e.lanes,n.child=e.child,n.memoizedProps=e.memoizedProps,n.memoizedState=e.memoizedState,n.updateQueue=e.updateQueue,t=e.dependencies,n.dependencies=t===null?null:{lanes:t.lanes,firstContext:t.firstContext},n.sibling=e.sibling,n.index=e.index,n.ref=e.ref,n.refCleanup=e.refCleanup,n}function _i(e,t){e.flags&=65011714;var n=e.alternate;return n===null?(e.childLanes=0,e.lanes=t,e.child=null,e.subtreeFlags=0,e.memoizedProps=null,e.memoizedState=null,e.updateQueue=null,e.dependencies=null,e.stateNode=null):(e.childLanes=n.childLanes,e.lanes=n.lanes,e.child=n.child,e.subtreeFlags=0,e.deletions=null,e.memoizedProps=n.memoizedProps,e.memoizedState=n.memoizedState,e.updateQueue=n.updateQueue,e.type=n.type,t=n.dependencies,e.dependencies=t===null?null:{lanes:t.lanes,firstContext:t.firstContext}),e}function vi(e,t,n,r,i,o){var s=0;if(r=e,typeof e==`function`)hi(e)&&(s=1);else if(typeof e==`string`)s=Uf(e,n,pe.current)?26:e===`html`||e===`head`||e===`body`?27:5;else a:switch(e){case re:return e=mi(31,n,t,i),e.elementType=re,e.lanes=o,e;case v:return yi(n.children,i,o,t);case y:s=8,i|=24;break;case b:return e=mi(12,n,t,i|2),e.elementType=b,e.lanes=o,e;case ee:return e=mi(13,n,t,i),e.elementType=ee,e.lanes=o,e;case te:return e=mi(19,n,t,i),e.elementType=te,e.lanes=o,e;default:if(typeof e==`object`&&e)switch(e.$$typeof){case S:s=10;break a;case x:s=9;break a;case C:s=11;break a;case w:s=14;break a;case T:s=16,r=null;break a}s=29,n=Error(a(130,e===null?`null`:typeof e,``)),r=null}return t=mi(s,n,t,i),t.elementType=e,t.type=r,t.lanes=o,t}function yi(e,t,n,r){return e=mi(7,e,r,t),e.lanes=n,e}function bi(e,t,n){return e=mi(6,e,null,t),e.lanes=n,e}function xi(e){var t=mi(18,null,null,0);return t.stateNode=e,t}function Si(e,t,n){return t=mi(4,e.children===null?[]:e.children,e.key,t),t.lanes=n,t.stateNode={containerInfo:e.containerInfo,pendingChildren:null,implementation:e.implementation},t}var Ci=new WeakMap;function wi(e,t){if(typeof e==`object`&&e){var n=Ci.get(e);return n===void 0?(t={value:e,source:t,stack:Ee(t)},Ci.set(e,t),t):n}return{value:e,source:t,stack:Ee(t)}}var Ti=[],Ei=0,Di=null,Oi=0,ki=[],Ai=0,ji=null,Mi=1,Ni=``;function Pi(e,t){Ti[Ei++]=Oi,Ti[Ei++]=Di,Di=e,Oi=t}function Fi(e,t,n){ki[Ai++]=Mi,ki[Ai++]=Ni,ki[Ai++]=ji,ji=e;var r=Mi;e=Ni;var i=32-We(r)-1;r&=~(1<<i),n+=1;var a=32-We(t)+i;if(30<a){var o=i-i%5;a=(r&(1<<o)-1).toString(32),r>>=o,i-=o,Mi=1<<32-We(t)+i|n<<i|r,Ni=a+e}else Mi=1<<a|n<<i|r,Ni=e}function Ii(e){e.return!==null&&(Pi(e,1),Fi(e,1,0))}function Li(e){for(;e===Di;)Di=Ti[--Ei],Ti[Ei]=null,Oi=Ti[--Ei],Ti[Ei]=null;for(;e===ji;)ji=ki[--Ai],ki[Ai]=null,Ni=ki[--Ai],ki[Ai]=null,Mi=ki[--Ai],ki[Ai]=null}function N(e,t){ki[Ai++]=Mi,ki[Ai++]=Ni,ki[Ai++]=ji,Mi=t.id,Ni=t.overflow,ji=e}var Ri=null,P=null,F=!1,zi=null,I=!1,Bi=Error(a(519));function Vi(e){throw qi(wi(Error(a(418,1<arguments.length&&arguments[1]!==void 0&&arguments[1]?`text`:`HTML`,``)),e)),Bi}function Hi(e){var t=e.stateNode,n=e.type,r=e.memoizedProps;switch(t[pt]=e,t[mt]=r,n){case`dialog`:Q(`cancel`,t),Q(`close`,t);break;case`iframe`:case`object`:case`embed`:Q(`load`,t);break;case`video`:case`audio`:for(n=0;n<_d.length;n++)Q(_d[n],t);break;case`source`:Q(`error`,t);break;case`img`:case`image`:case`link`:Q(`error`,t),Q(`load`,t);break;case`details`:Q(`toggle`,t);break;case`input`:Q(`invalid`,t),qt(t,r.value,r.defaultValue,r.checked,r.defaultChecked,r.type,r.name,!0);break;case`select`:Q(`invalid`,t);break;case`textarea`:Q(`invalid`,t),Zt(t,r.value,r.defaultValue,r.children)}n=r.children,typeof n!=`string`&&typeof n!=`number`&&typeof n!=`bigint`||t.textContent===``+n||!0===r.suppressHydrationWarning||Md(t.textContent,n)?(r.popover!=null&&(Q(`beforetoggle`,t),Q(`toggle`,t)),r.onScroll!=null&&Q(`scroll`,t),r.onScrollEnd!=null&&Q(`scrollend`,t),r.onClick!=null&&(t.onclick=sn),t=!0):t=!1,t||Vi(e,!0)}function Ui(e){for(Ri=e.return;Ri;)switch(Ri.tag){case 5:case 31:case 13:I=!1;return;case 27:case 3:I=!0;return;default:Ri=Ri.return}}function Wi(e){if(e!==Ri)return!1;if(!F)return Ui(e),F=!0,!1;var t=e.tag,n;if((n=t!==3&&t!==27)&&((n=t===5)&&(n=e.type,n=!(n!==`form`&&n!==`button`)||Ud(e.type,e.memoizedProps)),n=!n),n&&P&&Vi(e),Ui(e),t===13){if(e=e.memoizedState,e=e===null?null:e.dehydrated,!e)throw Error(a(317));P=uf(e)}else if(t===31){if(e=e.memoizedState,e=e===null?null:e.dehydrated,!e)throw Error(a(317));P=uf(e)}else t===27?(t=P,Zd(e.type)?(e=lf,lf=null,P=e):P=t):P=Ri?cf(e.stateNode.nextSibling):null;return!0}function Gi(){P=Ri=null,F=!1}function Ki(){var e=zi;return e!==null&&(Zl===null?Zl=e:Zl.push.apply(Zl,e),zi=null),e}function qi(e){zi===null?zi=[e]:zi.push(e)}var Ji=fe(null),Yi=null,Xi=null;function Zi(e,t,n){A(Ji,t._currentValue),t._currentValue=n}function Qi(e){e._currentValue=Ji.current,k(Ji)}function $i(e,t,n){for(;e!==null;){var r=e.alternate;if((e.childLanes&t)===t?r!==null&&(r.childLanes&t)!==t&&(r.childLanes|=t):(e.childLanes|=t,r!==null&&(r.childLanes|=t)),e===n)break;e=e.return}}function ea(e,t,n,r){var i=e.child;for(i!==null&&(i.return=e);i!==null;){var o=i.dependencies;if(o!==null){var s=i.child;o=o.firstContext;a:for(;o!==null;){var c=o;o=i;for(var l=0;l<t.length;l++)if(c.context===t[l]){o.lanes|=n,c=o.alternate,c!==null&&(c.lanes|=n),$i(o.return,n,e),r||(s=null);break a}o=c.next}}else if(i.tag===18){if(s=i.return,s===null)throw Error(a(341));s.lanes|=n,o=s.alternate,o!==null&&(o.lanes|=n),$i(s,n,e),s=null}else s=i.child;if(s!==null)s.return=i;else for(s=i;s!==null;){if(s===e){s=null;break}if(i=s.sibling,i!==null){i.return=s.return,s=i;break}s=s.return}i=s}}function ta(e,t,n,r){e=null;for(var i=t,o=!1;i!==null;){if(!o){if(i.flags&524288)o=!0;else if(i.flags&262144)break}if(i.tag===10){var s=i.alternate;if(s===null)throw Error(a(387));if(s=s.memoizedProps,s!==null){var c=i.type;Or(i.pendingProps.value,s.value)||(e===null?e=[c]:e.push(c))}}else if(i===he.current){if(s=i.alternate,s===null)throw Error(a(387));s.memoizedState.memoizedState!==i.memoizedState.memoizedState&&(e===null?e=[Qf]:e.push(Qf))}i=i.return}e!==null&&ea(t,e,n,r),t.flags|=262144}function na(e){for(e=e.firstContext;e!==null;){if(!Or(e.context._currentValue,e.memoizedValue))return!0;e=e.next}return!1}function ra(e){Yi=e,Xi=null,e=e.dependencies,e!==null&&(e.firstContext=null)}function ia(e){return oa(Yi,e)}function aa(e,t){return Yi===null&&ra(e),oa(e,t)}function oa(e,t){var n=t._currentValue;if(t={context:t,memoizedValue:n,next:null},Xi===null){if(e===null)throw Error(a(308));Xi=t,e.dependencies={lanes:0,firstContext:t},e.flags|=524288}else Xi=Xi.next=t;return n}var sa=typeof AbortController<`u`?AbortController:function(){var e=[],t=this.signal={aborted:!1,addEventListener:function(t,n){e.push(n)}};this.abort=function(){t.aborted=!0,e.forEach(function(e){return e()})}},ca=t.unstable_scheduleCallback,la=t.unstable_NormalPriority,ua={$$typeof:S,Consumer:null,Provider:null,_currentValue:null,_currentValue2:null,_threadCount:0};function da(){return{controller:new sa,data:new Map,refCount:0}}function fa(e){e.refCount--,e.refCount===0&&ca(la,function(){e.controller.abort()})}var pa=null,ma=0,ha=0,ga=null;function _a(e,t){if(pa===null){var n=pa=[];ma=0,ha=dd(),ga={status:`pending`,value:void 0,then:function(e){n.push(e)}}}return ma++,t.then(va,va),t}function va(){if(--ma===0&&pa!==null){ga!==null&&(ga.status=`fulfilled`);var e=pa;pa=null,ha=0,ga=null;for(var t=0;t<e.length;t++)(0,e[t])()}}function ya(e,t){var n=[],r={status:`pending`,value:null,reason:null,then:function(e){n.push(e)}};return e.then(function(){r.status=`fulfilled`,r.value=t;for(var e=0;e<n.length;e++)(0,n[e])(t)},function(e){for(r.status=`rejected`,r.reason=e,e=0;e<n.length;e++)(0,n[e])(void 0)}),r}var ba=D.S;D.S=function(e,t){eu=Me(),typeof t==`object`&&t&&typeof t.then==`function`&&_a(e,t),ba!==null&&ba(e,t)};var xa=fe(null);function Sa(){var e=xa.current;return e===null?q.pooledCache:e}function Ca(e,t){t===null?A(xa,xa.current):A(xa,t.pool)}function wa(){var e=Sa();return e===null?null:{parent:ua._currentValue,pool:e}}var Ta=Error(a(460)),Ea=Error(a(474)),Da=Error(a(542)),Oa={then:function(){}};function ka(e){return e=e.status,e===`fulfilled`||e===`rejected`}function Aa(e,t,n){switch(n=e[n],n===void 0?e.push(t):n!==t&&(t.then(sn,sn),t=n),t.status){case`fulfilled`:return t.value;case`rejected`:throw e=t.reason,Pa(e),e;default:if(typeof t.status==`string`)t.then(sn,sn);else{if(e=q,e!==null&&100<e.shellSuspendCounter)throw Error(a(482));e=t,e.status=`pending`,e.then(function(e){if(t.status===`pending`){var n=t;n.status=`fulfilled`,n.value=e}},function(e){if(t.status===`pending`){var n=t;n.status=`rejected`,n.reason=e}})}switch(t.status){case`fulfilled`:return t.value;case`rejected`:throw e=t.reason,Pa(e),e}throw Ma=t,Ta}}function ja(e){try{var t=e._init;return t(e._payload)}catch(e){throw typeof e==`object`&&e&&typeof e.then==`function`?(Ma=e,Ta):e}}var Ma=null;function Na(){if(Ma===null)throw Error(a(459));var e=Ma;return Ma=null,e}function Pa(e){if(e===Ta||e===Da)throw Error(a(483))}var Fa=null,Ia=0;function La(e){var t=Ia;return Ia+=1,Fa===null&&(Fa=[]),Aa(Fa,e,t)}function Ra(e,t){t=t.props.ref,e.ref=t===void 0?null:t}function za(e,t){throw t.$$typeof===h?Error(a(525)):(e=Object.prototype.toString.call(t),Error(a(31,e===`[object Object]`?`object with keys {`+Object.keys(t).join(`, `)+`}`:e)))}function Ba(e){function t(t,n){if(e){var r=t.deletions;r===null?(t.deletions=[n],t.flags|=16):r.push(n)}}function n(n,r){if(!e)return null;for(;r!==null;)t(n,r),r=r.sibling;return null}function r(e){for(var t=new Map;e!==null;)e.key===null?t.set(e.index,e):t.set(e.key,e),e=e.sibling;return t}function i(e,t){return e=gi(e,t),e.index=0,e.sibling=null,e}function o(t,n,r){return t.index=r,e?(r=t.alternate,r===null?(t.flags|=67108866,n):(r=r.index,r<n?(t.flags|=67108866,n):r)):(t.flags|=1048576,n)}function s(t){return e&&t.alternate===null&&(t.flags|=67108866),t}function c(e,t,n,r){return t===null||t.tag!==6?(t=bi(n,e.mode,r),t.return=e,t):(t=i(t,n),t.return=e,t)}function l(e,t,n,r){var a=n.type;return a===v?d(e,t,n.props.children,r,n.key):t!==null&&(t.elementType===a||typeof a==`object`&&a&&a.$$typeof===T&&ja(a)===t.type)?(t=i(t,n.props),Ra(t,n),t.return=e,t):(t=vi(n.type,n.key,n.props,null,e.mode,r),Ra(t,n),t.return=e,t)}function u(e,t,n,r){return t===null||t.tag!==4||t.stateNode.containerInfo!==n.containerInfo||t.stateNode.implementation!==n.implementation?(t=Si(n,e.mode,r),t.return=e,t):(t=i(t,n.children||[]),t.return=e,t)}function d(e,t,n,r,a){return t===null||t.tag!==7?(t=yi(n,e.mode,r,a),t.return=e,t):(t=i(t,n),t.return=e,t)}function f(e,t,n){if(typeof t==`string`&&t!==``||typeof t==`number`||typeof t==`bigint`)return t=bi(``+t,e.mode,n),t.return=e,t;if(typeof t==`object`&&t){switch(t.$$typeof){case g:return n=vi(t.type,t.key,t.props,null,e.mode,n),Ra(n,t),n.return=e,n;case _:return t=Si(t,e.mode,n),t.return=e,t;case T:return t=ja(t),f(e,t,n)}if(ce(t)||ae(t))return t=yi(t,e.mode,n,null),t.return=e,t;if(typeof t.then==`function`)return f(e,La(t),n);if(t.$$typeof===S)return f(e,aa(e,t),n);za(e,t)}return null}function p(e,t,n,r){var i=t===null?null:t.key;if(typeof n==`string`&&n!==``||typeof n==`number`||typeof n==`bigint`)return i===null?c(e,t,``+n,r):null;if(typeof n==`object`&&n){switch(n.$$typeof){case g:return n.key===i?l(e,t,n,r):null;case _:return n.key===i?u(e,t,n,r):null;case T:return n=ja(n),p(e,t,n,r)}if(ce(n)||ae(n))return i===null?d(e,t,n,r,null):null;if(typeof n.then==`function`)return p(e,t,La(n),r);if(n.$$typeof===S)return p(e,t,aa(e,n),r);za(e,n)}return null}function m(e,t,n,r,i){if(typeof r==`string`&&r!==``||typeof r==`number`||typeof r==`bigint`)return e=e.get(n)||null,c(t,e,``+r,i);if(typeof r==`object`&&r){switch(r.$$typeof){case g:return e=e.get(r.key===null?n:r.key)||null,l(t,e,r,i);case _:return e=e.get(r.key===null?n:r.key)||null,u(t,e,r,i);case T:return r=ja(r),m(e,t,n,r,i)}if(ce(r)||ae(r))return e=e.get(n)||null,d(t,e,r,i,null);if(typeof r.then==`function`)return m(e,t,n,La(r),i);if(r.$$typeof===S)return m(e,t,n,aa(t,r),i);za(t,r)}return null}function h(i,a,s,c){for(var l=null,u=null,d=a,h=a=0,g=null;d!==null&&h<s.length;h++){d.index>h?(g=d,d=null):g=d.sibling;var _=p(i,d,s[h],c);if(_===null){d===null&&(d=g);break}e&&d&&_.alternate===null&&t(i,d),a=o(_,a,h),u===null?l=_:u.sibling=_,u=_,d=g}if(h===s.length)return n(i,d),F&&Pi(i,h),l;if(d===null){for(;h<s.length;h++)d=f(i,s[h],c),d!==null&&(a=o(d,a,h),u===null?l=d:u.sibling=d,u=d);return F&&Pi(i,h),l}for(d=r(d);h<s.length;h++)g=m(d,i,h,s[h],c),g!==null&&(e&&g.alternate!==null&&d.delete(g.key===null?h:g.key),a=o(g,a,h),u===null?l=g:u.sibling=g,u=g);return e&&d.forEach(function(e){return t(i,e)}),F&&Pi(i,h),l}function y(i,s,c,l){if(c==null)throw Error(a(151));for(var u=null,d=null,h=s,g=s=0,_=null,v=c.next();h!==null&&!v.done;g++,v=c.next()){h.index>g?(_=h,h=null):_=h.sibling;var y=p(i,h,v.value,l);if(y===null){h===null&&(h=_);break}e&&h&&y.alternate===null&&t(i,h),s=o(y,s,g),d===null?u=y:d.sibling=y,d=y,h=_}if(v.done)return n(i,h),F&&Pi(i,g),u;if(h===null){for(;!v.done;g++,v=c.next())v=f(i,v.value,l),v!==null&&(s=o(v,s,g),d===null?u=v:d.sibling=v,d=v);return F&&Pi(i,g),u}for(h=r(h);!v.done;g++,v=c.next())v=m(h,i,g,v.value,l),v!==null&&(e&&v.alternate!==null&&h.delete(v.key===null?g:v.key),s=o(v,s,g),d===null?u=v:d.sibling=v,d=v);return e&&h.forEach(function(e){return t(i,e)}),F&&Pi(i,g),u}function b(e,r,o,c){if(typeof o==`object`&&o&&o.type===v&&o.key===null&&(o=o.props.children),typeof o==`object`&&o){switch(o.$$typeof){case g:a:{for(var l=o.key;r!==null;){if(r.key===l){if(l=o.type,l===v){if(r.tag===7){n(e,r.sibling),c=i(r,o.props.children),c.return=e,e=c;break a}}else if(r.elementType===l||typeof l==`object`&&l&&l.$$typeof===T&&ja(l)===r.type){n(e,r.sibling),c=i(r,o.props),Ra(c,o),c.return=e,e=c;break a}n(e,r);break}else t(e,r);r=r.sibling}o.type===v?(c=yi(o.props.children,e.mode,c,o.key),c.return=e,e=c):(c=vi(o.type,o.key,o.props,null,e.mode,c),Ra(c,o),c.return=e,e=c)}return s(e);case _:a:{for(l=o.key;r!==null;){if(r.key===l)if(r.tag===4&&r.stateNode.containerInfo===o.containerInfo&&r.stateNode.implementation===o.implementation){n(e,r.sibling),c=i(r,o.children||[]),c.return=e,e=c;break a}else{n(e,r);break}else t(e,r);r=r.sibling}c=Si(o,e.mode,c),c.return=e,e=c}return s(e);case T:return o=ja(o),b(e,r,o,c)}if(ce(o))return h(e,r,o,c);if(ae(o)){if(l=ae(o),typeof l!=`function`)throw Error(a(150));return o=l.call(o),y(e,r,o,c)}if(typeof o.then==`function`)return b(e,r,La(o),c);if(o.$$typeof===S)return b(e,r,aa(e,o),c);za(e,o)}return typeof o==`string`&&o!==``||typeof o==`number`||typeof o==`bigint`?(o=``+o,r!==null&&r.tag===6?(n(e,r.sibling),c=i(r,o),c.return=e,e=c):(n(e,r),c=bi(o,e.mode,c),c.return=e,e=c),s(e)):n(e,r)}return function(e,t,n,r){try{Ia=0;var i=b(e,t,n,r);return Fa=null,i}catch(t){if(t===Ta||t===Da)throw t;var a=mi(29,t,null,e.mode);return a.lanes=r,a.return=e,a}}}var Va=Ba(!0),Ha=Ba(!1),Ua=!1;function Wa(e){e.updateQueue={baseState:e.memoizedState,firstBaseUpdate:null,lastBaseUpdate:null,shared:{pending:null,lanes:0,hiddenCallbacks:null},callbacks:null}}function Ga(e,t){e=e.updateQueue,t.updateQueue===e&&(t.updateQueue={baseState:e.baseState,firstBaseUpdate:e.firstBaseUpdate,lastBaseUpdate:e.lastBaseUpdate,shared:e.shared,callbacks:null})}function Ka(e){return{lane:e,tag:0,payload:null,callback:null,next:null}}function qa(e,t,n){var r=e.updateQueue;if(r===null)return null;if(r=r.shared,K&2){var i=r.pending;return i===null?t.next=t:(t.next=i.next,i.next=t),r.pending=t,t=di(e),ui(e,null,n),t}return si(e,r,t,n),di(e)}function Ja(e,t,n){if(t=t.updateQueue,t!==null&&(t=t.shared,n&4194048)){var r=t.lanes;r&=e.pendingLanes,n|=r,t.lanes=n,ot(e,n)}}function Ya(e,t){var n=e.updateQueue,r=e.alternate;if(r!==null&&(r=r.updateQueue,n===r)){var i=null,a=null;if(n=n.firstBaseUpdate,n!==null){do{var o={lane:n.lane,tag:n.tag,payload:n.payload,callback:null,next:null};a===null?i=a=o:a=a.next=o,n=n.next}while(n!==null);a===null?i=a=t:a=a.next=t}else i=a=t;n={baseState:r.baseState,firstBaseUpdate:i,lastBaseUpdate:a,shared:r.shared,callbacks:r.callbacks},e.updateQueue=n;return}e=n.lastBaseUpdate,e===null?n.firstBaseUpdate=t:e.next=t,n.lastBaseUpdate=t}var Xa=!1;function Za(){if(Xa){var e=ga;if(e!==null)throw e}}function Qa(e,t,n,r){Xa=!1;var i=e.updateQueue;Ua=!1;var a=i.firstBaseUpdate,o=i.lastBaseUpdate,s=i.shared.pending;if(s!==null){i.shared.pending=null;var c=s,l=c.next;c.next=null,o===null?a=l:o.next=l,o=c;var u=e.alternate;u!==null&&(u=u.updateQueue,s=u.lastBaseUpdate,s!==o&&(s===null?u.firstBaseUpdate=l:s.next=l,u.lastBaseUpdate=c))}if(a!==null){var d=i.baseState;o=0,u=l=c=null,s=a;do{var f=s.lane&-536870913,p=f!==s.lane;if(p?(Y&f)===f:(r&f)===f){f!==0&&f===ha&&(Xa=!0),u!==null&&(u=u.next={lane:0,tag:s.tag,payload:s.payload,callback:null,next:null});a:{var h=e,g=s;f=t;var _=n;switch(g.tag){case 1:if(h=g.payload,typeof h==`function`){d=h.call(_,d,f);break a}d=h;break a;case 3:h.flags=h.flags&-65537|128;case 0:if(h=g.payload,f=typeof h==`function`?h.call(_,d,f):h,f==null)break a;d=m({},d,f);break a;case 2:Ua=!0}}f=s.callback,f!==null&&(e.flags|=64,p&&(e.flags|=8192),p=i.callbacks,p===null?i.callbacks=[f]:p.push(f))}else p={lane:f,tag:s.tag,payload:s.payload,callback:s.callback,next:null},u===null?(l=u=p,c=d):u=u.next=p,o|=f;if(s=s.next,s===null){if(s=i.shared.pending,s===null)break;p=s,s=p.next,p.next=null,i.lastBaseUpdate=p,i.shared.pending=null}}while(1);u===null&&(c=d),i.baseState=c,i.firstBaseUpdate=l,i.lastBaseUpdate=u,a===null&&(i.shared.lanes=0),Gl|=o,e.lanes=o,e.memoizedState=d}}function $a(e,t){if(typeof e!=`function`)throw Error(a(191,e));e.call(t)}function eo(e,t){var n=e.callbacks;if(n!==null)for(e.callbacks=null,e=0;e<n.length;e++)$a(n[e],t)}var to=fe(null),no=fe(0);function ro(e,t){e=Ul,A(no,e),A(to,t),Ul=e|t.baseLanes}function io(){A(no,Ul),A(to,to.current)}function ao(){Ul=no.current,k(to),k(no)}var oo=fe(null),so=null;function co(e){var t=e.alternate;A(mo,mo.current&1),A(oo,e),so===null&&(t===null||to.current!==null||t.memoizedState!==null)&&(so=e)}function lo(e){A(mo,mo.current),A(oo,e),so===null&&(so=e)}function uo(e){e.tag===22?(A(mo,mo.current),A(oo,e),so===null&&(so=e)):fo(e)}function fo(){A(mo,mo.current),A(oo,oo.current)}function po(e){k(oo),so===e&&(so=null),k(mo)}var mo=fe(0);function ho(e){for(var t=e;t!==null;){if(t.tag===13){var n=t.memoizedState;if(n!==null&&(n=n.dehydrated,n===null||af(n)||of(n)))return t}else if(t.tag===19&&(t.memoizedProps.revealOrder===`forwards`||t.memoizedProps.revealOrder===`backwards`||t.memoizedProps.revealOrder===`unstable_legacy-backwards`||t.memoizedProps.revealOrder===`together`)){if(t.flags&128)return t}else if(t.child!==null){t.child.return=t,t=t.child;continue}if(t===e)break;for(;t.sibling===null;){if(t.return===null||t.return===e)return null;t=t.return}t.sibling.return=t.return,t=t.sibling}return null}var go=0,L=null,R=null,_o=null,vo=!1,yo=!1,bo=!1,xo=0,z=0,So=null,Co=0;function B(){throw Error(a(321))}function wo(e,t){if(t===null)return!1;for(var n=0;n<t.length&&n<e.length;n++)if(!Or(e[n],t[n]))return!1;return!0}function To(e,t,n,r,i,a){return go=a,L=t,t.memoizedState=null,t.updateQueue=null,t.lanes=0,D.H=e===null||e.memoizedState===null?Bs:Vs,bo=!1,a=n(r,i),bo=!1,yo&&(a=Do(t,n,r,i)),Eo(e),a}function Eo(e){D.H=zs;var t=R!==null&&R.next!==null;if(go=0,_o=R=L=null,vo=!1,z=0,So=null,t)throw Error(a(300));e===null||ic||(e=e.dependencies,e!==null&&na(e)&&(ic=!0))}function Do(e,t,n,r){L=e;var i=0;do{if(yo&&(So=null),z=0,yo=!1,25<=i)throw Error(a(301));if(i+=1,_o=R=null,e.updateQueue!=null){var o=e.updateQueue;o.lastEffect=null,o.events=null,o.stores=null,o.memoCache!=null&&(o.memoCache.index=0)}D.H=Hs,o=t(n,r)}while(yo);return o}function Oo(){var e=D.H,t=e.useState()[0];return t=typeof t.then==`function`?Po(t):t,e=e.useState()[0],(R===null?null:R.memoizedState)!==e&&(L.flags|=1024),t}function ko(){var e=xo!==0;return xo=0,e}function Ao(e,t,n){t.updateQueue=e.updateQueue,t.flags&=-2053,e.lanes&=~n}function V(e){if(vo){for(e=e.memoizedState;e!==null;){var t=e.queue;t!==null&&(t.pending=null),e=e.next}vo=!1}go=0,_o=R=L=null,yo=!1,z=xo=0,So=null}function jo(){var e={memoizedState:null,baseState:null,baseQueue:null,queue:null,next:null};return _o===null?L.memoizedState=_o=e:_o=_o.next=e,_o}function Mo(){if(R===null){var e=L.alternate;e=e===null?null:e.memoizedState}else e=R.next;var t=_o===null?L.memoizedState:_o.next;if(t!==null)_o=t,R=e;else{if(e===null)throw L.alternate===null?Error(a(467)):Error(a(310));R=e,e={memoizedState:R.memoizedState,baseState:R.baseState,baseQueue:R.baseQueue,queue:R.queue,next:null},_o===null?L.memoizedState=_o=e:_o=_o.next=e}return _o}function No(){return{lastEffect:null,events:null,stores:null,memoCache:null}}function Po(e){var t=z;return z+=1,So===null&&(So=[]),e=Aa(So,e,t),t=L,(_o===null?t.memoizedState:_o.next)===null&&(t=t.alternate,D.H=t===null||t.memoizedState===null?Bs:Vs),e}function Fo(e){if(typeof e==`object`&&e){if(typeof e.then==`function`)return Po(e);if(e.$$typeof===S)return ia(e)}throw Error(a(438,String(e)))}function Io(e){var t=null,n=L.updateQueue;if(n!==null&&(t=n.memoCache),t==null){var r=L.alternate;r!==null&&(r=r.updateQueue,r!==null&&(r=r.memoCache,r!=null&&(t={data:r.data.map(function(e){return e.slice()}),index:0})))}if(t??={data:[],index:0},n===null&&(n=No(),L.updateQueue=n),n.memoCache=t,n=t.data[t.index],n===void 0)for(n=t.data[t.index]=Array(e),r=0;r<e;r++)n[r]=ie;return t.index++,n}function Lo(e,t){return typeof t==`function`?t(e):t}function Ro(e){return zo(Mo(),R,e)}function zo(e,t,n){var r=e.queue;if(r===null)throw Error(a(311));r.lastRenderedReducer=n;var i=e.baseQueue,o=r.pending;if(o!==null){if(i!==null){var s=i.next;i.next=o.next,o.next=s}t.baseQueue=i=o,r.pending=null}if(o=e.baseState,i===null)e.memoizedState=o;else{t=i.next;var c=s=null,l=null,u=t,d=!1;do{var f=u.lane&-536870913;if(f===u.lane?(go&f)===f:(Y&f)===f){var p=u.revertLane;if(p===0)l!==null&&(l=l.next={lane:0,revertLane:0,gesture:null,action:u.action,hasEagerState:u.hasEagerState,eagerState:u.eagerState,next:null}),f===ha&&(d=!0);else if((go&p)===p){u=u.next,p===ha&&(d=!0);continue}else f={lane:0,revertLane:u.revertLane,gesture:null,action:u.action,hasEagerState:u.hasEagerState,eagerState:u.eagerState,next:null},l===null?(c=l=f,s=o):l=l.next=f,L.lanes|=p,Gl|=p;f=u.action,bo&&n(o,f),o=u.hasEagerState?u.eagerState:n(o,f)}else p={lane:f,revertLane:u.revertLane,gesture:u.gesture,action:u.action,hasEagerState:u.hasEagerState,eagerState:u.eagerState,next:null},l===null?(c=l=p,s=o):l=l.next=p,L.lanes|=f,Gl|=f;u=u.next}while(u!==null&&u!==t);if(l===null?s=o:l.next=c,!Or(o,e.memoizedState)&&(ic=!0,d&&(n=ga,n!==null)))throw n;e.memoizedState=o,e.baseState=s,e.baseQueue=l,r.lastRenderedState=o}return i===null&&(r.lanes=0),[e.memoizedState,r.dispatch]}function Bo(e){var t=Mo(),n=t.queue;if(n===null)throw Error(a(311));n.lastRenderedReducer=e;var r=n.dispatch,i=n.pending,o=t.memoizedState;if(i!==null){n.pending=null;var s=i=i.next;do o=e(o,s.action),s=s.next;while(s!==i);Or(o,t.memoizedState)||(ic=!0),t.memoizedState=o,t.baseQueue===null&&(t.baseState=o),n.lastRenderedState=o}return[o,r]}function Vo(e,t,n){var r=L,i=Mo(),o=F;if(o){if(n===void 0)throw Error(a(407));n=n()}else n=t();var s=!Or((R||i).memoizedState,n);if(s&&(i.memoizedState=n,ic=!0),i=i.queue,ds(Wo.bind(null,r,i,e),[e]),i.getSnapshot!==t||s||_o!==null&&_o.memoizedState.tag&1){if(r.flags|=2048,os(9,{destroy:void 0},Uo.bind(null,r,i,n,t),null),q===null)throw Error(a(349));o||go&127||Ho(r,t,n)}return n}function Ho(e,t,n){e.flags|=16384,e={getSnapshot:t,value:n},t=L.updateQueue,t===null?(t=No(),L.updateQueue=t,t.stores=[e]):(n=t.stores,n===null?t.stores=[e]:n.push(e))}function Uo(e,t,n,r){t.value=n,t.getSnapshot=r,Go(t)&&Ko(e)}function Wo(e,t,n){return n(function(){Go(t)&&Ko(e)})}function Go(e){var t=e.getSnapshot;e=e.value;try{var n=t();return!Or(e,n)}catch{return!0}}function Ko(e){var t=li(e,2);t!==null&&hu(t,e,2)}function qo(e){var t=jo();if(typeof e==`function`){var n=e;if(e=n(),bo){Ue(!0);try{n()}finally{Ue(!1)}}}return t.memoizedState=t.baseState=e,t.queue={pending:null,lanes:0,dispatch:null,lastRenderedReducer:Lo,lastRenderedState:e},t}function Jo(e,t,n,r){return e.baseState=n,zo(e,R,typeof r==`function`?r:Lo)}function Yo(e,t,n,r,i){if(Is(e))throw Error(a(485));if(e=t.action,e!==null){var o={payload:i,action:e,next:null,isTransition:!0,status:`pending`,value:null,reason:null,listeners:[],then:function(e){o.listeners.push(e)}};D.T===null?o.isTransition=!1:n(!0),r(o),n=t.pending,n===null?(o.next=t.pending=o,Xo(t,o)):(o.next=n.next,t.pending=n.next=o)}}function Xo(e,t){var n=t.action,r=t.payload,i=e.state;if(t.isTransition){var a=D.T,o={};D.T=o;try{var s=n(i,r),c=D.S;c!==null&&c(o,s),Zo(e,t,s)}catch(n){$o(e,t,n)}finally{a!==null&&o.types!==null&&(a.types=o.types),D.T=a}}else try{a=n(i,r),Zo(e,t,a)}catch(n){$o(e,t,n)}}function Zo(e,t,n){typeof n==`object`&&n&&typeof n.then==`function`?n.then(function(n){Qo(e,t,n)},function(n){return $o(e,t,n)}):Qo(e,t,n)}function Qo(e,t,n){t.status=`fulfilled`,t.value=n,H(t),e.state=n,t=e.pending,t!==null&&(n=t.next,n===t?e.pending=null:(n=n.next,t.next=n,Xo(e,n)))}function $o(e,t,n){var r=e.pending;if(e.pending=null,r!==null){r=r.next;do t.status=`rejected`,t.reason=n,H(t),t=t.next;while(t!==r)}e.action=null}function H(e){e=e.listeners;for(var t=0;t<e.length;t++)(0,e[t])()}function es(e,t){return t}function ts(e,t){if(F){var n=q.formState;if(n!==null){a:{var r=L;if(F){if(P){b:{for(var i=P,a=I;i.nodeType!==8;){if(!a){i=null;break b}if(i=cf(i.nextSibling),i===null){i=null;break b}}a=i.data,i=a===`F!`||a===`F`?i:null}if(i){P=cf(i.nextSibling),r=i.data===`F!`;break a}}Vi(r)}r=!1}r&&(t=n[0])}}return n=jo(),n.memoizedState=n.baseState=t,r={pending:null,lanes:0,dispatch:null,lastRenderedReducer:es,lastRenderedState:t},n.queue=r,n=Ns.bind(null,L,r),r.dispatch=n,r=qo(!1),a=Fs.bind(null,L,!1,r.queue),r=jo(),i={state:t,dispatch:null,action:e,pending:null},r.queue=i,n=Yo.bind(null,L,i,a,n),i.dispatch=n,r.memoizedState=e,[t,n,!1]}function ns(e){return rs(Mo(),R,e)}function rs(e,t,n){if(t=zo(e,t,es)[0],e=Ro(Lo)[0],typeof t==`object`&&t&&typeof t.then==`function`)try{var r=Po(t)}catch(e){throw e===Ta?Da:e}else r=t;t=Mo();var i=t.queue,a=i.dispatch;return n!==t.memoizedState&&(L.flags|=2048,os(9,{destroy:void 0},is.bind(null,i,n),null)),[r,a,e]}function is(e,t){e.action=t}function as(e){var t=Mo(),n=R;if(n!==null)return rs(t,n,e);Mo(),t=t.memoizedState,n=Mo();var r=n.queue.dispatch;return n.memoizedState=e,[t,r,!1]}function os(e,t,n,r){return e={tag:e,create:n,deps:r,inst:t,next:null},t=L.updateQueue,t===null&&(t=No(),L.updateQueue=t),n=t.lastEffect,n===null?t.lastEffect=e.next=e:(r=n.next,n.next=e,e.next=r,t.lastEffect=e),e}function ss(){return Mo().memoizedState}function cs(e,t,n,r){var i=jo();L.flags|=e,i.memoizedState=os(1|t,{destroy:void 0},n,r===void 0?null:r)}function ls(e,t,n,r){var i=Mo();r=r===void 0?null:r;var a=i.memoizedState.inst;R!==null&&r!==null&&wo(r,R.memoizedState.deps)?i.memoizedState=os(t,a,n,r):(L.flags|=e,i.memoizedState=os(1|t,a,n,r))}function us(e,t){cs(8390656,8,e,t)}function ds(e,t){ls(2048,8,e,t)}function fs(e){L.flags|=4;var t=L.updateQueue;if(t===null)t=No(),L.updateQueue=t,t.events=[e];else{var n=t.events;n===null?t.events=[e]:n.push(e)}}function ps(e){var t=Mo().memoizedState;return fs({ref:t,nextImpl:e}),function(){if(K&2)throw Error(a(440));return t.impl.apply(void 0,arguments)}}function ms(e,t){return ls(4,2,e,t)}function hs(e,t){return ls(4,4,e,t)}function gs(e,t){if(typeof t==`function`){e=e();var n=t(e);return function(){typeof n==`function`?n():t(null)}}if(t!=null)return e=e(),t.current=e,function(){t.current=null}}function _s(e,t,n){n=n==null?null:n.concat([e]),ls(4,4,gs.bind(null,t,e),n)}function vs(){}function ys(e,t){var n=Mo();t=t===void 0?null:t;var r=n.memoizedState;return t!==null&&wo(t,r[1])?r[0]:(n.memoizedState=[e,t],e)}function bs(e,t){var n=Mo();t=t===void 0?null:t;var r=n.memoizedState;if(t!==null&&wo(t,r[1]))return r[0];if(r=e(),bo){Ue(!0);try{e()}finally{Ue(!1)}}return n.memoizedState=[r,t],r}function xs(e,t,n){return n===void 0||go&1073741824&&!(Y&261930)?e.memoizedState=t:(e.memoizedState=n,e=mu(),L.lanes|=e,Gl|=e,n)}function Ss(e,t,n,r){return Or(n,t)?n:to.current===null?!(go&42)||go&1073741824&&!(Y&261930)?(ic=!0,e.memoizedState=n):(e=mu(),L.lanes|=e,Gl|=e,t):(e=xs(e,n,r),Or(e,t)||(ic=!0),e)}function Cs(e,t,n,r,i){var a=O.p;O.p=a!==0&&8>a?a:8;var o=D.T,s={};D.T=s,Fs(e,!1,t,n);try{var c=i(),l=D.S;l!==null&&l(s,c),typeof c==`object`&&c&&typeof c.then==`function`?Ps(e,t,ya(c,r),pu(e)):Ps(e,t,r,pu(e))}catch(n){Ps(e,t,{then:function(){},status:`rejected`,reason:n},pu())}finally{O.p=a,o!==null&&s.types!==null&&(o.types=s.types),D.T=o}}function ws(){}function Ts(e,t,n,r){if(e.tag!==5)throw Error(a(476));var i=Es(e).queue;Cs(e,i,t,le,n===null?ws:function(){return Ds(e),n(r)})}function Es(e){var t=e.memoizedState;if(t!==null)return t;t={memoizedState:le,baseState:le,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:Lo,lastRenderedState:le},next:null};var n={};return t.next={memoizedState:n,baseState:n,baseQueue:null,queue:{pending:null,lanes:0,dispatch:null,lastRenderedReducer:Lo,lastRenderedState:n},next:null},e.memoizedState=t,e=e.alternate,e!==null&&(e.memoizedState=t),t}function Ds(e){var t=Es(e);t.next===null&&(t=e.alternate.memoizedState),Ps(e,t.next.queue,{},pu())}function Os(){return ia(Qf)}function ks(){return Mo().memoizedState}function As(){return Mo().memoizedState}function js(e){for(var t=e.return;t!==null;){switch(t.tag){case 24:case 3:var n=pu();e=Ka(n);var r=qa(t,e,n);r!==null&&(hu(r,t,n),Ja(r,t,n)),t={cache:da()},e.payload=t;return}t=t.return}}function Ms(e,t,n){var r=pu();n={lane:r,revertLane:0,gesture:null,action:n,hasEagerState:!1,eagerState:null,next:null},Is(e)?Ls(t,n):(n=ci(e,t,n,r),n!==null&&(hu(n,e,r),Rs(n,t,r)))}function Ns(e,t,n){Ps(e,t,n,pu())}function Ps(e,t,n,r){var i={lane:r,revertLane:0,gesture:null,action:n,hasEagerState:!1,eagerState:null,next:null};if(Is(e))Ls(t,i);else{var a=e.alternate;if(e.lanes===0&&(a===null||a.lanes===0)&&(a=t.lastRenderedReducer,a!==null))try{var o=t.lastRenderedState,s=a(o,n);if(i.hasEagerState=!0,i.eagerState=s,Or(s,o))return si(e,t,i,0),q===null&&oi(),!1}catch{}if(n=ci(e,t,i,r),n!==null)return hu(n,e,r),Rs(n,t,r),!0}return!1}function Fs(e,t,n,r){if(r={lane:2,revertLane:dd(),gesture:null,action:r,hasEagerState:!1,eagerState:null,next:null},Is(e)){if(t)throw Error(a(479))}else t=ci(e,n,r,2),t!==null&&hu(t,e,2)}function Is(e){var t=e.alternate;return e===L||t!==null&&t===L}function Ls(e,t){yo=vo=!0;var n=e.pending;n===null?t.next=t:(t.next=n.next,n.next=t),e.pending=t}function Rs(e,t,n){if(n&4194048){var r=t.lanes;r&=e.pendingLanes,n|=r,t.lanes=n,ot(e,n)}}var zs={readContext:ia,use:Fo,useCallback:B,useContext:B,useEffect:B,useImperativeHandle:B,useLayoutEffect:B,useInsertionEffect:B,useMemo:B,useReducer:B,useRef:B,useState:B,useDebugValue:B,useDeferredValue:B,useTransition:B,useSyncExternalStore:B,useId:B,useHostTransitionStatus:B,useFormState:B,useActionState:B,useOptimistic:B,useMemoCache:B,useCacheRefresh:B};zs.useEffectEvent=B;var Bs={readContext:ia,use:Fo,useCallback:function(e,t){return jo().memoizedState=[e,t===void 0?null:t],e},useContext:ia,useEffect:us,useImperativeHandle:function(e,t,n){n=n==null?null:n.concat([e]),cs(4194308,4,gs.bind(null,t,e),n)},useLayoutEffect:function(e,t){return cs(4194308,4,e,t)},useInsertionEffect:function(e,t){cs(4,2,e,t)},useMemo:function(e,t){var n=jo();t=t===void 0?null:t;var r=e();if(bo){Ue(!0);try{e()}finally{Ue(!1)}}return n.memoizedState=[r,t],r},useReducer:function(e,t,n){var r=jo();if(n!==void 0){var i=n(t);if(bo){Ue(!0);try{n(t)}finally{Ue(!1)}}}else i=t;return r.memoizedState=r.baseState=i,e={pending:null,lanes:0,dispatch:null,lastRenderedReducer:e,lastRenderedState:i},r.queue=e,e=e.dispatch=Ms.bind(null,L,e),[r.memoizedState,e]},useRef:function(e){var t=jo();return e={current:e},t.memoizedState=e},useState:function(e){e=qo(e);var t=e.queue,n=Ns.bind(null,L,t);return t.dispatch=n,[e.memoizedState,n]},useDebugValue:vs,useDeferredValue:function(e,t){return xs(jo(),e,t)},useTransition:function(){var e=qo(!1);return e=Cs.bind(null,L,e.queue,!0,!1),jo().memoizedState=e,[!1,e]},useSyncExternalStore:function(e,t,n){var r=L,i=jo();if(F){if(n===void 0)throw Error(a(407));n=n()}else{if(n=t(),q===null)throw Error(a(349));Y&127||Ho(r,t,n)}i.memoizedState=n;var o={value:n,getSnapshot:t};return i.queue=o,us(Wo.bind(null,r,o,e),[e]),r.flags|=2048,os(9,{destroy:void 0},Uo.bind(null,r,o,n,t),null),n},useId:function(){var e=jo(),t=q.identifierPrefix;if(F){var n=Ni,r=Mi;n=(r&~(1<<32-We(r)-1)).toString(32)+n,t=`_`+t+`R_`+n,n=xo++,0<n&&(t+=`H`+n.toString(32)),t+=`_`}else n=Co++,t=`_`+t+`r_`+n.toString(32)+`_`;return e.memoizedState=t},useHostTransitionStatus:Os,useFormState:ts,useActionState:ts,useOptimistic:function(e){var t=jo();t.memoizedState=t.baseState=e;var n={pending:null,lanes:0,dispatch:null,lastRenderedReducer:null,lastRenderedState:null};return t.queue=n,t=Fs.bind(null,L,!0,n),n.dispatch=t,[e,t]},useMemoCache:Io,useCacheRefresh:function(){return jo().memoizedState=js.bind(null,L)},useEffectEvent:function(e){var t=jo(),n={impl:e};return t.memoizedState=n,function(){if(K&2)throw Error(a(440));return n.impl.apply(void 0,arguments)}}},Vs={readContext:ia,use:Fo,useCallback:ys,useContext:ia,useEffect:ds,useImperativeHandle:_s,useInsertionEffect:ms,useLayoutEffect:hs,useMemo:bs,useReducer:Ro,useRef:ss,useState:function(){return Ro(Lo)},useDebugValue:vs,useDeferredValue:function(e,t){return Ss(Mo(),R.memoizedState,e,t)},useTransition:function(){var e=Ro(Lo)[0],t=Mo().memoizedState;return[typeof e==`boolean`?e:Po(e),t]},useSyncExternalStore:Vo,useId:ks,useHostTransitionStatus:Os,useFormState:ns,useActionState:ns,useOptimistic:function(e,t){return Jo(Mo(),R,e,t)},useMemoCache:Io,useCacheRefresh:As};Vs.useEffectEvent=ps;var Hs={readContext:ia,use:Fo,useCallback:ys,useContext:ia,useEffect:ds,useImperativeHandle:_s,useInsertionEffect:ms,useLayoutEffect:hs,useMemo:bs,useReducer:Bo,useRef:ss,useState:function(){return Bo(Lo)},useDebugValue:vs,useDeferredValue:function(e,t){var n=Mo();return R===null?xs(n,e,t):Ss(n,R.memoizedState,e,t)},useTransition:function(){var e=Bo(Lo)[0],t=Mo().memoizedState;return[typeof e==`boolean`?e:Po(e),t]},useSyncExternalStore:Vo,useId:ks,useHostTransitionStatus:Os,useFormState:as,useActionState:as,useOptimistic:function(e,t){var n=Mo();return R===null?(n.baseState=e,[e,n.queue.dispatch]):Jo(n,R,e,t)},useMemoCache:Io,useCacheRefresh:As};Hs.useEffectEvent=ps;function Us(e,t,n,r){t=e.memoizedState,n=n(r,t),n=n==null?t:m({},t,n),e.memoizedState=n,e.lanes===0&&(e.updateQueue.baseState=n)}var Ws={enqueueSetState:function(e,t,n){e=e._reactInternals;var r=pu(),i=Ka(r);i.payload=t,n!=null&&(i.callback=n),t=qa(e,i,r),t!==null&&(hu(t,e,r),Ja(t,e,r))},enqueueReplaceState:function(e,t,n){e=e._reactInternals;var r=pu(),i=Ka(r);i.tag=1,i.payload=t,n!=null&&(i.callback=n),t=qa(e,i,r),t!==null&&(hu(t,e,r),Ja(t,e,r))},enqueueForceUpdate:function(e,t){e=e._reactInternals;var n=pu(),r=Ka(n);r.tag=2,t!=null&&(r.callback=t),t=qa(e,r,n),t!==null&&(hu(t,e,n),Ja(t,e,n))}};function Gs(e,t,n,r,i,a,o){return e=e.stateNode,typeof e.shouldComponentUpdate==`function`?e.shouldComponentUpdate(r,a,o):t.prototype&&t.prototype.isPureReactComponent?!kr(n,r)||!kr(i,a):!0}function Ks(e,t,n,r){e=t.state,typeof t.componentWillReceiveProps==`function`&&t.componentWillReceiveProps(n,r),typeof t.UNSAFE_componentWillReceiveProps==`function`&&t.UNSAFE_componentWillReceiveProps(n,r),t.state!==e&&Ws.enqueueReplaceState(t,t.state,null)}function qs(e,t){var n=t;if(`ref`in t)for(var r in n={},t)r!==`ref`&&(n[r]=t[r]);if(e=e.defaultProps)for(var i in n===t&&(n=m({},n)),e)n[i]===void 0&&(n[i]=e[i]);return n}function Js(e){ni(e)}function Ys(e){console.error(e)}function Xs(e){ni(e)}function Zs(e,t){try{var n=e.onUncaughtError;n(t.value,{componentStack:t.stack})}catch(e){setTimeout(function(){throw e})}}function Qs(e,t,n){try{var r=e.onCaughtError;r(n.value,{componentStack:n.stack,errorBoundary:t.tag===1?t.stateNode:null})}catch(e){setTimeout(function(){throw e})}}function $s(e,t,n){return n=Ka(n),n.tag=3,n.payload={element:null},n.callback=function(){Zs(e,t)},n}function ec(e){return e=Ka(e),e.tag=3,e}function tc(e,t,n,r){var i=n.type.getDerivedStateFromError;if(typeof i==`function`){var a=r.value;e.payload=function(){return i(a)},e.callback=function(){Qs(t,n,r)}}var o=n.stateNode;o!==null&&typeof o.componentDidCatch==`function`&&(e.callback=function(){Qs(t,n,r),typeof i!=`function`&&(ru===null?ru=new Set([this]):ru.add(this));var e=r.stack;this.componentDidCatch(r.value,{componentStack:e===null?``:e})})}function nc(e,t,n,r,i){if(n.flags|=32768,typeof r==`object`&&r&&typeof r.then==`function`){if(t=n.alternate,t!==null&&ta(t,n,i,!0),n=oo.current,n!==null){switch(n.tag){case 31:case 13:return so===null?Du():n.alternate===null&&Wl===0&&(Wl=3),n.flags&=-257,n.flags|=65536,n.lanes=i,r===Oa?n.flags|=16384:(t=n.updateQueue,t===null?n.updateQueue=new Set([r]):t.add(r),Gu(e,r,i)),!1;case 22:return n.flags|=65536,r===Oa?n.flags|=16384:(t=n.updateQueue,t===null?(t={transitions:null,markerInstances:null,retryQueue:new Set([r])},n.updateQueue=t):(n=t.retryQueue,n===null?t.retryQueue=new Set([r]):n.add(r)),Gu(e,r,i)),!1}throw Error(a(435,n.tag))}return Gu(e,r,i),Du(),!1}if(F)return t=oo.current,t===null?(r!==Bi&&(t=Error(a(423),{cause:r}),qi(wi(t,n))),e=e.current.alternate,e.flags|=65536,i&=-i,e.lanes|=i,r=wi(r,n),i=$s(e.stateNode,r,i),Ya(e,i),Wl!==4&&(Wl=2)):(!(t.flags&65536)&&(t.flags|=256),t.flags|=65536,t.lanes=i,r!==Bi&&(e=Error(a(422),{cause:r}),qi(wi(e,n)))),!1;var o=Error(a(520),{cause:r});if(o=wi(o,n),Xl===null?Xl=[o]:Xl.push(o),Wl!==4&&(Wl=2),t===null)return!0;r=wi(r,n),n=t;do{switch(n.tag){case 3:return n.flags|=65536,e=i&-i,n.lanes|=e,e=$s(n.stateNode,r,e),Ya(n,e),!1;case 1:if(t=n.type,o=n.stateNode,!(n.flags&128)&&(typeof t.getDerivedStateFromError==`function`||o!==null&&typeof o.componentDidCatch==`function`&&(ru===null||!ru.has(o))))return n.flags|=65536,i&=-i,n.lanes|=i,i=ec(i),tc(i,e,n,r),Ya(n,i),!1}n=n.return}while(n!==null);return!1}var rc=Error(a(461)),ic=!1;function ac(e,t,n,r){t.child=e===null?Ha(t,null,n,r):Va(t,e.child,n,r)}function oc(e,t,n,r,i){n=n.render;var a=t.ref;if(`ref`in r){var o={};for(var s in r)s!==`ref`&&(o[s]=r[s])}else o=r;return ra(t),r=To(e,t,n,o,a,i),s=ko(),e!==null&&!ic?(Ao(e,t,i),Ac(e,t,i)):(F&&s&&Ii(t),t.flags|=1,ac(e,t,r,i),t.child)}function sc(e,t,n,r,i){if(e===null){var a=n.type;return typeof a==`function`&&!hi(a)&&a.defaultProps===void 0&&n.compare===null?(t.tag=15,t.type=a,cc(e,t,a,r,i)):(e=vi(n.type,null,r,t,t.mode,i),e.ref=t.ref,e.return=t,t.child=e)}if(a=e.child,!jc(e,i)){var o=a.memoizedProps;if(n=n.compare,n=n===null?kr:n,n(o,r)&&e.ref===t.ref)return Ac(e,t,i)}return t.flags|=1,e=gi(a,r),e.ref=t.ref,e.return=t,t.child=e}function cc(e,t,n,r,i){if(e!==null){var a=e.memoizedProps;if(kr(a,r)&&e.ref===t.ref)if(ic=!1,t.pendingProps=r=a,jc(e,i))e.flags&131072&&(ic=!0);else return t.lanes=e.lanes,Ac(e,t,i)}return gc(e,t,n,r,i)}function lc(e,t,n,r){var i=r.children,a=e===null?null:e.memoizedState;if(e===null&&t.stateNode===null&&(t.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),r.mode===`hidden`){if(t.flags&128){if(a=a===null?n:a.baseLanes|n,e!==null){for(r=t.child=e.child,i=0;r!==null;)i=i|r.lanes|r.childLanes,r=r.sibling;r=i&~a}else r=0,t.child=null;return dc(e,t,a,n,r)}if(n&536870912)t.memoizedState={baseLanes:0,cachePool:null},e!==null&&Ca(t,a===null?null:a.cachePool),a===null?io():ro(t,a),uo(t);else return r=t.lanes=536870912,dc(e,t,a===null?n:a.baseLanes|n,n,r)}else a===null?(e!==null&&Ca(t,null),io(),fo(t)):(Ca(t,a.cachePool),ro(t,a),fo(t),t.memoizedState=null);return ac(e,t,i,n),t.child}function uc(e,t){return e!==null&&e.tag===22||t.stateNode!==null||(t.stateNode={_visibility:1,_pendingMarkers:null,_retryCache:null,_transitions:null}),t.sibling}function dc(e,t,n,r,i){var a=Sa();return a=a===null?null:{parent:ua._currentValue,pool:a},t.memoizedState={baseLanes:n,cachePool:a},e!==null&&Ca(t,null),io(),uo(t),e!==null&&ta(e,t,r,!0),t.childLanes=i,null}function fc(e,t){return t=Tc({mode:t.mode,children:t.children},e.mode),t.ref=e.ref,e.child=t,t.return=e,t}function pc(e,t,n){return Va(t,e.child,null,n),e=fc(t,t.pendingProps),e.flags|=2,po(t),t.memoizedState=null,e}function mc(e,t,n){var r=t.pendingProps,i=(t.flags&128)!=0;if(t.flags&=-129,e===null){if(F){if(r.mode===`hidden`)return e=fc(t,r),t.lanes=536870912,uc(null,e);if(lo(t),(e=P)?(e=rf(e,I),e=e!==null&&e.data===`&`?e:null,e!==null&&(t.memoizedState={dehydrated:e,treeContext:ji===null?null:{id:Mi,overflow:Ni},retryLane:536870912,hydrationErrors:null},n=xi(e),n.return=t,t.child=n,Ri=t,P=null)):e=null,e===null)throw Vi(t);return t.lanes=536870912,null}return fc(t,r)}var o=e.memoizedState;if(o!==null){var s=o.dehydrated;if(lo(t),i)if(t.flags&256)t.flags&=-257,t=pc(e,t,n);else if(t.memoizedState!==null)t.child=e.child,t.flags|=128,t=null;else throw Error(a(558));else if(ic||ta(e,t,n,!1),i=(n&e.childLanes)!==0,ic||i){if(r=q,r!==null&&(s=st(r,n),s!==0&&s!==o.retryLane))throw o.retryLane=s,li(e,s),hu(r,e,s),rc;Du(),t=pc(e,t,n)}else e=o.treeContext,P=cf(s.nextSibling),Ri=t,F=!0,zi=null,I=!1,e!==null&&N(t,e),t=fc(t,r),t.flags|=4096;return t}return e=gi(e.child,{mode:r.mode,children:r.children}),e.ref=t.ref,t.child=e,e.return=t,e}function hc(e,t){var n=t.ref;if(n===null)e!==null&&e.ref!==null&&(t.flags|=4194816);else{if(typeof n!=`function`&&typeof n!=`object`)throw Error(a(284));(e===null||e.ref!==n)&&(t.flags|=4194816)}}function gc(e,t,n,r,i){return ra(t),n=To(e,t,n,r,void 0,i),r=ko(),e!==null&&!ic?(Ao(e,t,i),Ac(e,t,i)):(F&&r&&Ii(t),t.flags|=1,ac(e,t,n,i),t.child)}function _c(e,t,n,r,i,a){return ra(t),t.updateQueue=null,n=Do(t,r,n,i),Eo(e),r=ko(),e!==null&&!ic?(Ao(e,t,a),Ac(e,t,a)):(F&&r&&Ii(t),t.flags|=1,ac(e,t,n,a),t.child)}function vc(e,t,n,r,i){if(ra(t),t.stateNode===null){var a=fi,o=n.contextType;typeof o==`object`&&o&&(a=ia(o)),a=new n(r,a),t.memoizedState=a.state!==null&&a.state!==void 0?a.state:null,a.updater=Ws,t.stateNode=a,a._reactInternals=t,a=t.stateNode,a.props=r,a.state=t.memoizedState,a.refs={},Wa(t),o=n.contextType,a.context=typeof o==`object`&&o?ia(o):fi,a.state=t.memoizedState,o=n.getDerivedStateFromProps,typeof o==`function`&&(Us(t,n,o,r),a.state=t.memoizedState),typeof n.getDerivedStateFromProps==`function`||typeof a.getSnapshotBeforeUpdate==`function`||typeof a.UNSAFE_componentWillMount!=`function`&&typeof a.componentWillMount!=`function`||(o=a.state,typeof a.componentWillMount==`function`&&a.componentWillMount(),typeof a.UNSAFE_componentWillMount==`function`&&a.UNSAFE_componentWillMount(),o!==a.state&&Ws.enqueueReplaceState(a,a.state,null),Qa(t,r,a,i),Za(),a.state=t.memoizedState),typeof a.componentDidMount==`function`&&(t.flags|=4194308),r=!0}else if(e===null){a=t.stateNode;var s=t.memoizedProps,c=qs(n,s);a.props=c;var l=a.context,u=n.contextType;o=fi,typeof u==`object`&&u&&(o=ia(u));var d=n.getDerivedStateFromProps;u=typeof d==`function`||typeof a.getSnapshotBeforeUpdate==`function`,s=t.pendingProps!==s,u||typeof a.UNSAFE_componentWillReceiveProps!=`function`&&typeof a.componentWillReceiveProps!=`function`||(s||l!==o)&&Ks(t,a,r,o),Ua=!1;var f=t.memoizedState;a.state=f,Qa(t,r,a,i),Za(),l=t.memoizedState,s||f!==l||Ua?(typeof d==`function`&&(Us(t,n,d,r),l=t.memoizedState),(c=Ua||Gs(t,n,c,r,f,l,o))?(u||typeof a.UNSAFE_componentWillMount!=`function`&&typeof a.componentWillMount!=`function`||(typeof a.componentWillMount==`function`&&a.componentWillMount(),typeof a.UNSAFE_componentWillMount==`function`&&a.UNSAFE_componentWillMount()),typeof a.componentDidMount==`function`&&(t.flags|=4194308)):(typeof a.componentDidMount==`function`&&(t.flags|=4194308),t.memoizedProps=r,t.memoizedState=l),a.props=r,a.state=l,a.context=o,r=c):(typeof a.componentDidMount==`function`&&(t.flags|=4194308),r=!1)}else{a=t.stateNode,Ga(e,t),o=t.memoizedProps,u=qs(n,o),a.props=u,d=t.pendingProps,f=a.context,l=n.contextType,c=fi,typeof l==`object`&&l&&(c=ia(l)),s=n.getDerivedStateFromProps,(l=typeof s==`function`||typeof a.getSnapshotBeforeUpdate==`function`)||typeof a.UNSAFE_componentWillReceiveProps!=`function`&&typeof a.componentWillReceiveProps!=`function`||(o!==d||f!==c)&&Ks(t,a,r,c),Ua=!1,f=t.memoizedState,a.state=f,Qa(t,r,a,i),Za();var p=t.memoizedState;o!==d||f!==p||Ua||e!==null&&e.dependencies!==null&&na(e.dependencies)?(typeof s==`function`&&(Us(t,n,s,r),p=t.memoizedState),(u=Ua||Gs(t,n,u,r,f,p,c)||e!==null&&e.dependencies!==null&&na(e.dependencies))?(l||typeof a.UNSAFE_componentWillUpdate!=`function`&&typeof a.componentWillUpdate!=`function`||(typeof a.componentWillUpdate==`function`&&a.componentWillUpdate(r,p,c),typeof a.UNSAFE_componentWillUpdate==`function`&&a.UNSAFE_componentWillUpdate(r,p,c)),typeof a.componentDidUpdate==`function`&&(t.flags|=4),typeof a.getSnapshotBeforeUpdate==`function`&&(t.flags|=1024)):(typeof a.componentDidUpdate!=`function`||o===e.memoizedProps&&f===e.memoizedState||(t.flags|=4),typeof a.getSnapshotBeforeUpdate!=`function`||o===e.memoizedProps&&f===e.memoizedState||(t.flags|=1024),t.memoizedProps=r,t.memoizedState=p),a.props=r,a.state=p,a.context=c,r=u):(typeof a.componentDidUpdate!=`function`||o===e.memoizedProps&&f===e.memoizedState||(t.flags|=4),typeof a.getSnapshotBeforeUpdate!=`function`||o===e.memoizedProps&&f===e.memoizedState||(t.flags|=1024),r=!1)}return a=r,hc(e,t),r=(t.flags&128)!=0,a||r?(a=t.stateNode,n=r&&typeof n.getDerivedStateFromError!=`function`?null:a.render(),t.flags|=1,e!==null&&r?(t.child=Va(t,e.child,null,i),t.child=Va(t,null,n,i)):ac(e,t,n,i),t.memoizedState=a.state,e=t.child):e=Ac(e,t,i),e}function yc(e,t,n,r){return Gi(),t.flags|=256,ac(e,t,n,r),t.child}var bc={dehydrated:null,treeContext:null,retryLane:0,hydrationErrors:null};function xc(e){return{baseLanes:e,cachePool:wa()}}function Sc(e,t,n){return e=e===null?0:e.childLanes&~n,t&&(e|=Jl),e}function Cc(e,t,n){var r=t.pendingProps,i=!1,o=(t.flags&128)!=0,s;if((s=o)||(s=e!==null&&e.memoizedState===null?!1:(mo.current&2)!=0),s&&(i=!0,t.flags&=-129),s=(t.flags&32)!=0,t.flags&=-33,e===null){if(F){if(i?co(t):fo(t),(e=P)?(e=rf(e,I),e=e!==null&&e.data!==`&`?e:null,e!==null&&(t.memoizedState={dehydrated:e,treeContext:ji===null?null:{id:Mi,overflow:Ni},retryLane:536870912,hydrationErrors:null},n=xi(e),n.return=t,t.child=n,Ri=t,P=null)):e=null,e===null)throw Vi(t);return of(e)?t.lanes=32:t.lanes=536870912,null}var c=r.children;return r=r.fallback,i?(fo(t),i=t.mode,c=Tc({mode:`hidden`,children:c},i),r=yi(r,i,n,null),c.return=t,r.return=t,c.sibling=r,t.child=c,r=t.child,r.memoizedState=xc(n),r.childLanes=Sc(e,s,n),t.memoizedState=bc,uc(null,r)):(co(t),wc(t,c))}var l=e.memoizedState;if(l!==null&&(c=l.dehydrated,c!==null)){if(o)t.flags&256?(co(t),t.flags&=-257,t=Ec(e,t,n)):t.memoizedState===null?(fo(t),c=r.fallback,i=t.mode,r=Tc({mode:`visible`,children:r.children},i),c=yi(c,i,n,null),c.flags|=2,r.return=t,c.return=t,r.sibling=c,t.child=r,Va(t,e.child,null,n),r=t.child,r.memoizedState=xc(n),r.childLanes=Sc(e,s,n),t.memoizedState=bc,t=uc(null,r)):(fo(t),t.child=e.child,t.flags|=128,t=null);else if(co(t),of(c)){if(s=c.nextSibling&&c.nextSibling.dataset,s)var u=s.dgst;s=u,r=Error(a(419)),r.stack=``,r.digest=s,qi({value:r,source:null,stack:null}),t=Ec(e,t,n)}else if(ic||ta(e,t,n,!1),s=(n&e.childLanes)!==0,ic||s){if(s=q,s!==null&&(r=st(s,n),r!==0&&r!==l.retryLane))throw l.retryLane=r,li(e,r),hu(s,e,r),rc;af(c)||Du(),t=Ec(e,t,n)}else af(c)?(t.flags|=192,t.child=e.child,t=null):(e=l.treeContext,P=cf(c.nextSibling),Ri=t,F=!0,zi=null,I=!1,e!==null&&N(t,e),t=wc(t,r.children),t.flags|=4096);return t}return i?(fo(t),c=r.fallback,i=t.mode,l=e.child,u=l.sibling,r=gi(l,{mode:`hidden`,children:r.children}),r.subtreeFlags=l.subtreeFlags&65011712,u===null?(c=yi(c,i,n,null),c.flags|=2):c=gi(u,c),c.return=t,r.return=t,r.sibling=c,t.child=r,uc(null,r),r=t.child,c=e.child.memoizedState,c===null?c=xc(n):(i=c.cachePool,i===null?i=wa():(l=ua._currentValue,i=i.parent===l?i:{parent:l,pool:l}),c={baseLanes:c.baseLanes|n,cachePool:i}),r.memoizedState=c,r.childLanes=Sc(e,s,n),t.memoizedState=bc,uc(e.child,r)):(co(t),n=e.child,e=n.sibling,n=gi(n,{mode:`visible`,children:r.children}),n.return=t,n.sibling=null,e!==null&&(s=t.deletions,s===null?(t.deletions=[e],t.flags|=16):s.push(e)),t.child=n,t.memoizedState=null,n)}function wc(e,t){return t=Tc({mode:`visible`,children:t},e.mode),t.return=e,e.child=t}function Tc(e,t){return e=mi(22,e,null,t),e.lanes=0,e}function Ec(e,t,n){return Va(t,e.child,null,n),e=wc(t,t.pendingProps.children),e.flags|=2,t.memoizedState=null,e}function Dc(e,t,n){e.lanes|=t;var r=e.alternate;r!==null&&(r.lanes|=t),$i(e.return,t,n)}function Oc(e,t,n,r,i,a){var o=e.memoizedState;o===null?e.memoizedState={isBackwards:t,rendering:null,renderingStartTime:0,last:r,tail:n,tailMode:i,treeForkCount:a}:(o.isBackwards=t,o.rendering=null,o.renderingStartTime=0,o.last=r,o.tail=n,o.tailMode=i,o.treeForkCount=a)}function kc(e,t,n){var r=t.pendingProps,i=r.revealOrder,a=r.tail;r=r.children;var o=mo.current,s=(o&2)!=0;if(s?(o=o&1|2,t.flags|=128):o&=1,A(mo,o),ac(e,t,r,n),r=F?Oi:0,!s&&e!==null&&e.flags&128)a:for(e=t.child;e!==null;){if(e.tag===13)e.memoizedState!==null&&Dc(e,n,t);else if(e.tag===19)Dc(e,n,t);else if(e.child!==null){e.child.return=e,e=e.child;continue}if(e===t)break a;for(;e.sibling===null;){if(e.return===null||e.return===t)break a;e=e.return}e.sibling.return=e.return,e=e.sibling}switch(i){case`forwards`:for(n=t.child,i=null;n!==null;)e=n.alternate,e!==null&&ho(e)===null&&(i=n),n=n.sibling;n=i,n===null?(i=t.child,t.child=null):(i=n.sibling,n.sibling=null),Oc(t,!1,i,n,a,r);break;case`backwards`:case`unstable_legacy-backwards`:for(n=null,i=t.child,t.child=null;i!==null;){if(e=i.alternate,e!==null&&ho(e)===null){t.child=i;break}e=i.sibling,i.sibling=n,n=i,i=e}Oc(t,!0,n,null,a,r);break;case`together`:Oc(t,!1,null,null,void 0,r);break;default:t.memoizedState=null}return t.child}function Ac(e,t,n){if(e!==null&&(t.dependencies=e.dependencies),Gl|=t.lanes,(n&t.childLanes)===0)if(e!==null){if(ta(e,t,n,!1),(n&t.childLanes)===0)return null}else return null;if(e!==null&&t.child!==e.child)throw Error(a(153));if(t.child!==null){for(e=t.child,n=gi(e,e.pendingProps),t.child=n,n.return=t;e.sibling!==null;)e=e.sibling,n=n.sibling=gi(e,e.pendingProps),n.return=t;n.sibling=null}return t.child}function jc(e,t){return(e.lanes&t)===0?(e=e.dependencies,!!(e!==null&&na(e))):!0}function Mc(e,t,n){switch(t.tag){case 3:ge(t,t.stateNode.containerInfo),Zi(t,ua,e.memoizedState.cache),Gi();break;case 27:case 5:ve(t);break;case 4:ge(t,t.stateNode.containerInfo);break;case 10:Zi(t,t.type,t.memoizedProps.value);break;case 31:if(t.memoizedState!==null)return t.flags|=128,lo(t),null;break;case 13:var r=t.memoizedState;if(r!==null)return r.dehydrated===null?(n&t.child.childLanes)===0?(co(t),e=Ac(e,t,n),e===null?null:e.sibling):Cc(e,t,n):(co(t),t.flags|=128,null);co(t);break;case 19:var i=(e.flags&128)!=0;if(r=(n&t.childLanes)!==0,r||=(ta(e,t,n,!1),(n&t.childLanes)!==0),i){if(r)return kc(e,t,n);t.flags|=128}if(i=t.memoizedState,i!==null&&(i.rendering=null,i.tail=null,i.lastEffect=null),A(mo,mo.current),r)break;return null;case 22:return t.lanes=0,lc(e,t,n,t.pendingProps);case 24:Zi(t,ua,e.memoizedState.cache)}return Ac(e,t,n)}function Nc(e,t,n){if(e!==null)if(e.memoizedProps!==t.pendingProps)ic=!0;else{if(!jc(e,n)&&!(t.flags&128))return ic=!1,Mc(e,t,n);ic=!!(e.flags&131072)}else ic=!1,F&&t.flags&1048576&&Fi(t,Oi,t.index);switch(t.lanes=0,t.tag){case 16:a:{var r=t.pendingProps;if(e=ja(t.elementType),t.type=e,typeof e==`function`)hi(e)?(r=qs(e,r),t.tag=1,t=vc(null,t,e,r,n)):(t.tag=0,t=gc(null,t,e,r,n));else{if(e!=null){var i=e.$$typeof;if(i===C){t.tag=11,t=oc(null,t,e,r,n);break a}else if(i===w){t.tag=14,t=sc(null,t,e,r,n);break a}}throw t=se(e)||e,Error(a(306,t,``))}}return t;case 0:return gc(e,t,t.type,t.pendingProps,n);case 1:return r=t.type,i=qs(r,t.pendingProps),vc(e,t,r,i,n);case 3:a:{if(ge(t,t.stateNode.containerInfo),e===null)throw Error(a(387));r=t.pendingProps;var o=t.memoizedState;i=o.element,Ga(e,t),Qa(t,r,null,n);var s=t.memoizedState;if(r=s.cache,Zi(t,ua,r),r!==o.cache&&ea(t,[ua],n,!0),Za(),r=s.element,o.isDehydrated)if(o={element:r,isDehydrated:!1,cache:s.cache},t.updateQueue.baseState=o,t.memoizedState=o,t.flags&256){t=yc(e,t,r,n);break a}else if(r!==i){i=wi(Error(a(424)),t),qi(i),t=yc(e,t,r,n);break a}else{switch(e=t.stateNode.containerInfo,e.nodeType){case 9:e=e.body;break;default:e=e.nodeName===`HTML`?e.ownerDocument.body:e}for(P=cf(e.firstChild),Ri=t,F=!0,zi=null,I=!0,n=Ha(t,null,r,n),t.child=n;n;)n.flags=n.flags&-3|4096,n=n.sibling}else{if(Gi(),r===i){t=Ac(e,t,n);break a}ac(e,t,r,n)}t=t.child}return t;case 26:return hc(e,t),e===null?(n=kf(t.type,null,t.pendingProps,null))?t.memoizedState=n:F||(n=t.type,e=t.pendingProps,r=Bd(me.current).createElement(n),r[pt]=t,r[mt]=e,Pd(r,n,e),Et(r),t.stateNode=r):t.memoizedState=kf(t.type,e.memoizedProps,t.pendingProps,e.memoizedState),null;case 27:return ve(t),e===null&&F&&(r=t.stateNode=ff(t.type,t.pendingProps,me.current),Ri=t,I=!0,i=P,Zd(t.type)?(lf=i,P=cf(r.firstChild)):P=i),ac(e,t,t.pendingProps.children,n),hc(e,t),e===null&&(t.flags|=4194304),t.child;case 5:return e===null&&F&&((i=r=P)&&(r=tf(r,t.type,t.pendingProps,I),r===null?i=!1:(t.stateNode=r,Ri=t,P=cf(r.firstChild),I=!1,i=!0)),i||Vi(t)),ve(t),i=t.type,o=t.pendingProps,s=e===null?null:e.memoizedProps,r=o.children,Ud(i,o)?r=null:s!==null&&Ud(i,s)&&(t.flags|=32),t.memoizedState!==null&&(i=To(e,t,Oo,null,null,n),Qf._currentValue=i),hc(e,t),ac(e,t,r,n),t.child;case 6:return e===null&&F&&((e=n=P)&&(n=nf(n,t.pendingProps,I),n===null?e=!1:(t.stateNode=n,Ri=t,P=null,e=!0)),e||Vi(t)),null;case 13:return Cc(e,t,n);case 4:return ge(t,t.stateNode.containerInfo),r=t.pendingProps,e===null?t.child=Va(t,null,r,n):ac(e,t,r,n),t.child;case 11:return oc(e,t,t.type,t.pendingProps,n);case 7:return ac(e,t,t.pendingProps,n),t.child;case 8:return ac(e,t,t.pendingProps.children,n),t.child;case 12:return ac(e,t,t.pendingProps.children,n),t.child;case 10:return r=t.pendingProps,Zi(t,t.type,r.value),ac(e,t,r.children,n),t.child;case 9:return i=t.type._context,r=t.pendingProps.children,ra(t),i=ia(i),r=r(i),t.flags|=1,ac(e,t,r,n),t.child;case 14:return sc(e,t,t.type,t.pendingProps,n);case 15:return cc(e,t,t.type,t.pendingProps,n);case 19:return kc(e,t,n);case 31:return mc(e,t,n);case 22:return lc(e,t,n,t.pendingProps);case 24:return ra(t),r=ia(ua),e===null?(i=Sa(),i===null&&(i=q,o=da(),i.pooledCache=o,o.refCount++,o!==null&&(i.pooledCacheLanes|=n),i=o),t.memoizedState={parent:r,cache:i},Wa(t),Zi(t,ua,i)):((e.lanes&n)!==0&&(Ga(e,t),Qa(t,null,null,n),Za()),i=e.memoizedState,o=t.memoizedState,i.parent===r?(r=o.cache,Zi(t,ua,r),r!==i.cache&&ea(t,[ua],n,!0)):(i={parent:r,cache:r},t.memoizedState=i,t.lanes===0&&(t.memoizedState=t.updateQueue.baseState=i),Zi(t,ua,r))),ac(e,t,t.pendingProps.children,n),t.child;case 29:throw t.pendingProps}throw Error(a(156,t.tag))}function Pc(e){e.flags|=4}function Fc(e,t,n,r,i){if((t=(e.mode&32)!=0)&&(t=!1),t){if(e.flags|=16777216,(i&335544128)===i)if(e.stateNode.complete)e.flags|=8192;else if(wu())e.flags|=8192;else throw Ma=Oa,Ea}else e.flags&=-16777217}function Ic(e,t){if(t.type!==`stylesheet`||t.state.loading&4)e.flags&=-16777217;else if(e.flags|=16777216,!Wf(t))if(wu())e.flags|=8192;else throw Ma=Oa,Ea}function Lc(e,t){t!==null&&(e.flags|=4),e.flags&16384&&(t=e.tag===22?536870912:tt(),e.lanes|=t,Yl|=t)}function Rc(e,t){if(!F)switch(e.tailMode){case`hidden`:t=e.tail;for(var n=null;t!==null;)t.alternate!==null&&(n=t),t=t.sibling;n===null?e.tail=null:n.sibling=null;break;case`collapsed`:n=e.tail;for(var r=null;n!==null;)n.alternate!==null&&(r=n),n=n.sibling;r===null?t||e.tail===null?e.tail=null:e.tail.sibling=null:r.sibling=null}}function U(e){var t=e.alternate!==null&&e.alternate.child===e.child,n=0,r=0;if(t)for(var i=e.child;i!==null;)n|=i.lanes|i.childLanes,r|=i.subtreeFlags&65011712,r|=i.flags&65011712,i.return=e,i=i.sibling;else for(i=e.child;i!==null;)n|=i.lanes|i.childLanes,r|=i.subtreeFlags,r|=i.flags,i.return=e,i=i.sibling;return e.subtreeFlags|=r,e.childLanes=n,t}function zc(e,t,n){var r=t.pendingProps;switch(Li(t),t.tag){case 16:case 15:case 0:case 11:case 7:case 8:case 12:case 9:case 14:return U(t),null;case 1:return U(t),null;case 3:return n=t.stateNode,r=null,e!==null&&(r=e.memoizedState.cache),t.memoizedState.cache!==r&&(t.flags|=2048),Qi(ua),_e(),n.pendingContext&&(n.context=n.pendingContext,n.pendingContext=null),(e===null||e.child===null)&&(Wi(t)?Pc(t):e===null||e.memoizedState.isDehydrated&&!(t.flags&256)||(t.flags|=1024,Ki())),U(t),null;case 26:var i=t.type,o=t.memoizedState;return e===null?(Pc(t),o===null?(U(t),Fc(t,i,null,r,n)):(U(t),Ic(t,o))):o?o===e.memoizedState?(U(t),t.flags&=-16777217):(Pc(t),U(t),Ic(t,o)):(e=e.memoizedProps,e!==r&&Pc(t),U(t),Fc(t,i,e,r,n)),null;case 27:if(ye(t),n=me.current,i=t.type,e!==null&&t.stateNode!=null)e.memoizedProps!==r&&Pc(t);else{if(!r){if(t.stateNode===null)throw Error(a(166));return U(t),null}e=pe.current,Wi(t)?Hi(t,e):(e=ff(i,r,n),t.stateNode=e,Pc(t))}return U(t),null;case 5:if(ye(t),i=t.type,e!==null&&t.stateNode!=null)e.memoizedProps!==r&&Pc(t);else{if(!r){if(t.stateNode===null)throw Error(a(166));return U(t),null}if(o=pe.current,Wi(t))Hi(t,o);else{var s=Bd(me.current);switch(o){case 1:o=s.createElementNS(`http://www.w3.org/2000/svg`,i);break;case 2:o=s.createElementNS(`http://www.w3.org/1998/Math/MathML`,i);break;default:switch(i){case`svg`:o=s.createElementNS(`http://www.w3.org/2000/svg`,i);break;case`math`:o=s.createElementNS(`http://www.w3.org/1998/Math/MathML`,i);break;case`script`:o=s.createElement(`div`),o.innerHTML=`<script><\/script>`,o=o.removeChild(o.firstChild);break;case`select`:o=typeof r.is==`string`?s.createElement(`select`,{is:r.is}):s.createElement(`select`),r.multiple?o.multiple=!0:r.size&&(o.size=r.size);break;default:o=typeof r.is==`string`?s.createElement(i,{is:r.is}):s.createElement(i)}}o[pt]=t,o[mt]=r;a:for(s=t.child;s!==null;){if(s.tag===5||s.tag===6)o.appendChild(s.stateNode);else if(s.tag!==4&&s.tag!==27&&s.child!==null){s.child.return=s,s=s.child;continue}if(s===t)break a;for(;s.sibling===null;){if(s.return===null||s.return===t)break a;s=s.return}s.sibling.return=s.return,s=s.sibling}t.stateNode=o;a:switch(Pd(o,i,r),i){case`button`:case`input`:case`select`:case`textarea`:r=!!r.autoFocus;break a;case`img`:r=!0;break a;default:r=!1}r&&Pc(t)}}return U(t),Fc(t,t.type,e===null?null:e.memoizedProps,t.pendingProps,n),null;case 6:if(e&&t.stateNode!=null)e.memoizedProps!==r&&Pc(t);else{if(typeof r!=`string`&&t.stateNode===null)throw Error(a(166));if(e=me.current,Wi(t)){if(e=t.stateNode,n=t.memoizedProps,r=null,i=Ri,i!==null)switch(i.tag){case 27:case 5:r=i.memoizedProps}e[pt]=t,e=!!(e.nodeValue===n||r!==null&&!0===r.suppressHydrationWarning||Md(e.nodeValue,n)),e||Vi(t,!0)}else e=Bd(e).createTextNode(r),e[pt]=t,t.stateNode=e}return U(t),null;case 31:if(n=t.memoizedState,e===null||e.memoizedState!==null){if(r=Wi(t),n!==null){if(e===null){if(!r)throw Error(a(318));if(e=t.memoizedState,e=e===null?null:e.dehydrated,!e)throw Error(a(557));e[pt]=t}else Gi(),!(t.flags&128)&&(t.memoizedState=null),t.flags|=4;U(t),e=!1}else n=Ki(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=n),e=!0;if(!e)return t.flags&256?(po(t),t):(po(t),null);if(t.flags&128)throw Error(a(558))}return U(t),null;case 13:if(r=t.memoizedState,e===null||e.memoizedState!==null&&e.memoizedState.dehydrated!==null){if(i=Wi(t),r!==null&&r.dehydrated!==null){if(e===null){if(!i)throw Error(a(318));if(i=t.memoizedState,i=i===null?null:i.dehydrated,!i)throw Error(a(317));i[pt]=t}else Gi(),!(t.flags&128)&&(t.memoizedState=null),t.flags|=4;U(t),i=!1}else i=Ki(),e!==null&&e.memoizedState!==null&&(e.memoizedState.hydrationErrors=i),i=!0;if(!i)return t.flags&256?(po(t),t):(po(t),null)}return po(t),t.flags&128?(t.lanes=n,t):(n=r!==null,e=e!==null&&e.memoizedState!==null,n&&(r=t.child,i=null,r.alternate!==null&&r.alternate.memoizedState!==null&&r.alternate.memoizedState.cachePool!==null&&(i=r.alternate.memoizedState.cachePool.pool),o=null,r.memoizedState!==null&&r.memoizedState.cachePool!==null&&(o=r.memoizedState.cachePool.pool),o!==i&&(r.flags|=2048)),n!==e&&n&&(t.child.flags|=8192),Lc(t,t.updateQueue),U(t),null);case 4:return _e(),e===null&&Sd(t.stateNode.containerInfo),U(t),null;case 10:return Qi(t.type),U(t),null;case 19:if(k(mo),r=t.memoizedState,r===null)return U(t),null;if(i=(t.flags&128)!=0,o=r.rendering,o===null)if(i)Rc(r,!1);else{if(Wl!==0||e!==null&&e.flags&128)for(e=t.child;e!==null;){if(o=ho(e),o!==null){for(t.flags|=128,Rc(r,!1),e=o.updateQueue,t.updateQueue=e,Lc(t,e),t.subtreeFlags=0,e=n,n=t.child;n!==null;)_i(n,e),n=n.sibling;return A(mo,mo.current&1|2),F&&Pi(t,r.treeForkCount),t.child}e=e.sibling}r.tail!==null&&Me()>tu&&(t.flags|=128,i=!0,Rc(r,!1),t.lanes=4194304)}else{if(!i)if(e=ho(o),e!==null){if(t.flags|=128,i=!0,e=e.updateQueue,t.updateQueue=e,Lc(t,e),Rc(r,!0),r.tail===null&&r.tailMode===`hidden`&&!o.alternate&&!F)return U(t),null}else 2*Me()-r.renderingStartTime>tu&&n!==536870912&&(t.flags|=128,i=!0,Rc(r,!1),t.lanes=4194304);r.isBackwards?(o.sibling=t.child,t.child=o):(e=r.last,e===null?t.child=o:e.sibling=o,r.last=o)}return r.tail===null?(U(t),null):(e=r.tail,r.rendering=e,r.tail=e.sibling,r.renderingStartTime=Me(),e.sibling=null,n=mo.current,A(mo,i?n&1|2:n&1),F&&Pi(t,r.treeForkCount),e);case 22:case 23:return po(t),ao(),r=t.memoizedState!==null,e===null?r&&(t.flags|=8192):e.memoizedState!==null!==r&&(t.flags|=8192),r?n&536870912&&!(t.flags&128)&&(U(t),t.subtreeFlags&6&&(t.flags|=8192)):U(t),n=t.updateQueue,n!==null&&Lc(t,n.retryQueue),n=null,e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(n=e.memoizedState.cachePool.pool),r=null,t.memoizedState!==null&&t.memoizedState.cachePool!==null&&(r=t.memoizedState.cachePool.pool),r!==n&&(t.flags|=2048),e!==null&&k(xa),null;case 24:return n=null,e!==null&&(n=e.memoizedState.cache),t.memoizedState.cache!==n&&(t.flags|=2048),Qi(ua),U(t),null;case 25:return null;case 30:return null}throw Error(a(156,t.tag))}function Bc(e,t){switch(Li(t),t.tag){case 1:return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 3:return Qi(ua),_e(),e=t.flags,e&65536&&!(e&128)?(t.flags=e&-65537|128,t):null;case 26:case 27:case 5:return ye(t),null;case 31:if(t.memoizedState!==null){if(po(t),t.alternate===null)throw Error(a(340));Gi()}return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 13:if(po(t),e=t.memoizedState,e!==null&&e.dehydrated!==null){if(t.alternate===null)throw Error(a(340));Gi()}return e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 19:return k(mo),null;case 4:return _e(),null;case 10:return Qi(t.type),null;case 22:case 23:return po(t),ao(),e!==null&&k(xa),e=t.flags,e&65536?(t.flags=e&-65537|128,t):null;case 24:return Qi(ua),null;case 25:return null;default:return null}}function Vc(e,t){switch(Li(t),t.tag){case 3:Qi(ua),_e();break;case 26:case 27:case 5:ye(t);break;case 4:_e();break;case 31:t.memoizedState!==null&&po(t);break;case 13:po(t);break;case 19:k(mo);break;case 10:Qi(t.type);break;case 22:case 23:po(t),ao(),e!==null&&k(xa);break;case 24:Qi(ua)}}function Hc(e,t){try{var n=t.updateQueue,r=n===null?null:n.lastEffect;if(r!==null){var i=r.next;n=i;do{if((n.tag&e)===e){r=void 0;var a=n.create,o=n.inst;r=a(),o.destroy=r}n=n.next}while(n!==i)}}catch(e){Z(t,t.return,e)}}function Uc(e,t,n){try{var r=t.updateQueue,i=r===null?null:r.lastEffect;if(i!==null){var a=i.next;r=a;do{if((r.tag&e)===e){var o=r.inst,s=o.destroy;if(s!==void 0){o.destroy=void 0,i=t;var c=n,l=s;try{l()}catch(e){Z(i,c,e)}}}r=r.next}while(r!==a)}}catch(e){Z(t,t.return,e)}}function Wc(e){var t=e.updateQueue;if(t!==null){var n=e.stateNode;try{eo(t,n)}catch(t){Z(e,e.return,t)}}}function Gc(e,t,n){n.props=qs(e.type,e.memoizedProps),n.state=e.memoizedState;try{n.componentWillUnmount()}catch(n){Z(e,t,n)}}function Kc(e,t){try{var n=e.ref;if(n!==null){switch(e.tag){case 26:case 27:case 5:var r=e.stateNode;break;case 30:r=e.stateNode;break;default:r=e.stateNode}typeof n==`function`?e.refCleanup=n(r):n.current=r}}catch(n){Z(e,t,n)}}function qc(e,t){var n=e.ref,r=e.refCleanup;if(n!==null)if(typeof r==`function`)try{r()}catch(n){Z(e,t,n)}finally{e.refCleanup=null,e=e.alternate,e!=null&&(e.refCleanup=null)}else if(typeof n==`function`)try{n(null)}catch(n){Z(e,t,n)}else n.current=null}function W(e){var t=e.type,n=e.memoizedProps,r=e.stateNode;try{a:switch(t){case`button`:case`input`:case`select`:case`textarea`:n.autoFocus&&r.focus();break a;case`img`:n.src?r.src=n.src:n.srcSet&&(r.srcset=n.srcSet)}}catch(t){Z(e,e.return,t)}}function Jc(e,t,n){try{var r=e.stateNode;Fd(r,e.type,n,t),r[mt]=t}catch(t){Z(e,e.return,t)}}function Yc(e){return e.tag===5||e.tag===3||e.tag===26||e.tag===27&&Zd(e.type)||e.tag===4}function Xc(e){a:for(;;){for(;e.sibling===null;){if(e.return===null||Yc(e.return))return null;e=e.return}for(e.sibling.return=e.return,e=e.sibling;e.tag!==5&&e.tag!==6&&e.tag!==18;){if(e.tag===27&&Zd(e.type)||e.flags&2||e.child===null||e.tag===4)continue a;e.child.return=e,e=e.child}if(!(e.flags&2))return e.stateNode}}function Zc(e,t,n){var r=e.tag;if(r===5||r===6)e=e.stateNode,t?(n.nodeType===9?n.body:n.nodeName===`HTML`?n.ownerDocument.body:n).insertBefore(e,t):(t=n.nodeType===9?n.body:n.nodeName===`HTML`?n.ownerDocument.body:n,t.appendChild(e),n=n._reactRootContainer,n!=null||t.onclick!==null||(t.onclick=sn));else if(r!==4&&(r===27&&Zd(e.type)&&(n=e.stateNode,t=null),e=e.child,e!==null))for(Zc(e,t,n),e=e.sibling;e!==null;)Zc(e,t,n),e=e.sibling}function Qc(e,t,n){var r=e.tag;if(r===5||r===6)e=e.stateNode,t?n.insertBefore(e,t):n.appendChild(e);else if(r!==4&&(r===27&&Zd(e.type)&&(n=e.stateNode),e=e.child,e!==null))for(Qc(e,t,n),e=e.sibling;e!==null;)Qc(e,t,n),e=e.sibling}function $c(e){var t=e.stateNode,n=e.memoizedProps;try{for(var r=e.type,i=t.attributes;i.length;)t.removeAttributeNode(i[0]);Pd(t,r,n),t[pt]=e,t[mt]=n}catch(t){Z(e,e.return,t)}}var G=!1,el=!1,tl=!1,nl=typeof WeakSet==`function`?WeakSet:Set,rl=null;function il(e,t){if(e=e.containerInfo,Rd=sp,e=Nr(e),Pr(e)){if(`selectionStart`in e)var n={start:e.selectionStart,end:e.selectionEnd};else a:{n=(n=e.ownerDocument)&&n.defaultView||window;var r=n.getSelection&&n.getSelection();if(r&&r.rangeCount!==0){n=r.anchorNode;var i=r.anchorOffset,o=r.focusNode;r=r.focusOffset;try{n.nodeType,o.nodeType}catch{n=null;break a}var s=0,c=-1,l=-1,u=0,d=0,f=e,p=null;b:for(;;){for(var m;f!==n||i!==0&&f.nodeType!==3||(c=s+i),f!==o||r!==0&&f.nodeType!==3||(l=s+r),f.nodeType===3&&(s+=f.nodeValue.length),(m=f.firstChild)!==null;)p=f,f=m;for(;;){if(f===e)break b;if(p===n&&++u===i&&(c=s),p===o&&++d===r&&(l=s),(m=f.nextSibling)!==null)break;f=p,p=f.parentNode}f=m}n=c===-1||l===-1?null:{start:c,end:l}}else n=null}n||={start:0,end:0}}else n=null;for(zd={focusedElem:e,selectionRange:n},sp=!1,rl=t;rl!==null;)if(t=rl,e=t.child,t.subtreeFlags&1028&&e!==null)e.return=t,rl=e;else for(;rl!==null;){switch(t=rl,o=t.alternate,e=t.flags,t.tag){case 0:if(e&4&&(e=t.updateQueue,e=e===null?null:e.events,e!==null))for(n=0;n<e.length;n++)i=e[n],i.ref.impl=i.nextImpl;break;case 11:case 15:break;case 1:if(e&1024&&o!==null){e=void 0,n=t,i=o.memoizedProps,o=o.memoizedState,r=n.stateNode;try{var h=qs(n.type,i);e=r.getSnapshotBeforeUpdate(h,o),r.__reactInternalSnapshotBeforeUpdate=e}catch(e){Z(n,n.return,e)}}break;case 3:if(e&1024){if(e=t.stateNode.containerInfo,n=e.nodeType,n===9)ef(e);else if(n===1)switch(e.nodeName){case`HEAD`:case`HTML`:case`BODY`:ef(e);break;default:e.textContent=``}}break;case 5:case 26:case 27:case 6:case 4:case 17:break;default:if(e&1024)throw Error(a(163))}if(e=t.sibling,e!==null){e.return=t.return,rl=e;break}rl=t.return}}function al(e,t,n){var r=n.flags;switch(n.tag){case 0:case 11:case 15:bl(e,n),r&4&&Hc(5,n);break;case 1:if(bl(e,n),r&4)if(e=n.stateNode,t===null)try{e.componentDidMount()}catch(e){Z(n,n.return,e)}else{var i=qs(n.type,t.memoizedProps);t=t.memoizedState;try{e.componentDidUpdate(i,t,e.__reactInternalSnapshotBeforeUpdate)}catch(e){Z(n,n.return,e)}}r&64&&Wc(n),r&512&&Kc(n,n.return);break;case 3:if(bl(e,n),r&64&&(e=n.updateQueue,e!==null)){if(t=null,n.child!==null)switch(n.child.tag){case 27:case 5:t=n.child.stateNode;break;case 1:t=n.child.stateNode}try{eo(e,t)}catch(e){Z(n,n.return,e)}}break;case 27:t===null&&r&4&&$c(n);case 26:case 5:bl(e,n),t===null&&r&4&&W(n),r&512&&Kc(n,n.return);break;case 12:bl(e,n);break;case 31:bl(e,n),r&4&&dl(e,n);break;case 13:bl(e,n),r&4&&fl(e,n),r&64&&(e=n.memoizedState,e!==null&&(e=e.dehydrated,e!==null&&(n=Ju.bind(null,n),sf(e,n))));break;case 22:if(r=n.memoizedState!==null||G,!r){t=t!==null&&t.memoizedState!==null||el,i=G;var a=el;G=r,(el=t)&&!a?Sl(e,n,(n.subtreeFlags&8772)!=0):bl(e,n),G=i,el=a}break;case 30:break;default:bl(e,n)}}function ol(e){var t=e.alternate;t!==null&&(e.alternate=null,ol(t)),e.child=null,e.deletions=null,e.sibling=null,e.tag===5&&(t=e.stateNode,t!==null&&xt(t)),e.stateNode=null,e.return=null,e.dependencies=null,e.memoizedProps=null,e.memoizedState=null,e.pendingProps=null,e.stateNode=null,e.updateQueue=null}var sl=null,cl=!1;function ll(e,t,n){for(n=n.child;n!==null;)ul(e,t,n),n=n.sibling}function ul(e,t,n){if(He&&typeof He.onCommitFiberUnmount==`function`)try{He.onCommitFiberUnmount(Ve,n)}catch{}switch(n.tag){case 26:el||qc(n,t),ll(e,t,n),n.memoizedState?n.memoizedState.count--:n.stateNode&&(n=n.stateNode,n.parentNode.removeChild(n));break;case 27:el||qc(n,t);var r=sl,i=cl;Zd(n.type)&&(sl=n.stateNode,cl=!1),ll(e,t,n),pf(n.stateNode),sl=r,cl=i;break;case 5:el||qc(n,t);case 6:if(r=sl,i=cl,sl=null,ll(e,t,n),sl=r,cl=i,sl!==null)if(cl)try{(sl.nodeType===9?sl.body:sl.nodeName===`HTML`?sl.ownerDocument.body:sl).removeChild(n.stateNode)}catch(e){Z(n,t,e)}else try{sl.removeChild(n.stateNode)}catch(e){Z(n,t,e)}break;case 18:sl!==null&&(cl?(e=sl,Qd(e.nodeType===9?e.body:e.nodeName===`HTML`?e.ownerDocument.body:e,n.stateNode),Np(e)):Qd(sl,n.stateNode));break;case 4:r=sl,i=cl,sl=n.stateNode.containerInfo,cl=!0,ll(e,t,n),sl=r,cl=i;break;case 0:case 11:case 14:case 15:Uc(2,n,t),el||Uc(4,n,t),ll(e,t,n);break;case 1:el||(qc(n,t),r=n.stateNode,typeof r.componentWillUnmount==`function`&&Gc(n,t,r)),ll(e,t,n);break;case 21:ll(e,t,n);break;case 22:el=(r=el)||n.memoizedState!==null,ll(e,t,n),el=r;break;default:ll(e,t,n)}}function dl(e,t){if(t.memoizedState===null&&(e=t.alternate,e!==null&&(e=e.memoizedState,e!==null))){e=e.dehydrated;try{Np(e)}catch(e){Z(t,t.return,e)}}}function fl(e,t){if(t.memoizedState===null&&(e=t.alternate,e!==null&&(e=e.memoizedState,e!==null&&(e=e.dehydrated,e!==null))))try{Np(e)}catch(e){Z(t,t.return,e)}}function pl(e){switch(e.tag){case 31:case 13:case 19:var t=e.stateNode;return t===null&&(t=e.stateNode=new nl),t;case 22:return e=e.stateNode,t=e._retryCache,t===null&&(t=e._retryCache=new nl),t;default:throw Error(a(435,e.tag))}}function ml(e,t){var n=pl(e);t.forEach(function(t){if(!n.has(t)){n.add(t);var r=Yu.bind(null,e,t);t.then(r,r)}})}function hl(e,t){var n=t.deletions;if(n!==null)for(var r=0;r<n.length;r++){var i=n[r],o=e,s=t,c=s;a:for(;c!==null;){switch(c.tag){case 27:if(Zd(c.type)){sl=c.stateNode,cl=!1;break a}break;case 5:sl=c.stateNode,cl=!1;break a;case 3:case 4:sl=c.stateNode.containerInfo,cl=!0;break a}c=c.return}if(sl===null)throw Error(a(160));ul(o,s,i),sl=null,cl=!1,o=i.alternate,o!==null&&(o.return=null),i.return=null}if(t.subtreeFlags&13886)for(t=t.child;t!==null;)_l(t,e),t=t.sibling}var gl=null;function _l(e,t){var n=e.alternate,r=e.flags;switch(e.tag){case 0:case 11:case 14:case 15:hl(t,e),vl(e),r&4&&(Uc(3,e,e.return),Hc(3,e),Uc(5,e,e.return));break;case 1:hl(t,e),vl(e),r&512&&(el||n===null||qc(n,n.return)),r&64&&G&&(e=e.updateQueue,e!==null&&(r=e.callbacks,r!==null&&(n=e.shared.hiddenCallbacks,e.shared.hiddenCallbacks=n===null?r:n.concat(r))));break;case 26:var i=gl;if(hl(t,e),vl(e),r&512&&(el||n===null||qc(n,n.return)),r&4){var o=n===null?null:n.memoizedState;if(r=e.memoizedState,n===null)if(r===null)if(e.stateNode===null){a:{r=e.type,n=e.memoizedProps,i=i.ownerDocument||i;b:switch(r){case`title`:o=i.getElementsByTagName(`title`)[0],(!o||o[bt]||o[pt]||o.namespaceURI===`http://www.w3.org/2000/svg`||o.hasAttribute(`itemprop`))&&(o=i.createElement(r),i.head.insertBefore(o,i.querySelector(`head > title`))),Pd(o,r,n),o[pt]=e,Et(o),r=o;break a;case`link`:var s=Vf(`link`,`href`,i).get(r+(n.href||``));if(s){for(var c=0;c<s.length;c++)if(o=s[c],o.getAttribute(`href`)===(n.href==null||n.href===``?null:n.href)&&o.getAttribute(`rel`)===(n.rel==null?null:n.rel)&&o.getAttribute(`title`)===(n.title==null?null:n.title)&&o.getAttribute(`crossorigin`)===(n.crossOrigin==null?null:n.crossOrigin)){s.splice(c,1);break b}}o=i.createElement(r),Pd(o,r,n),i.head.appendChild(o);break;case`meta`:if(s=Vf(`meta`,`content`,i).get(r+(n.content||``))){for(c=0;c<s.length;c++)if(o=s[c],o.getAttribute(`content`)===(n.content==null?null:``+n.content)&&o.getAttribute(`name`)===(n.name==null?null:n.name)&&o.getAttribute(`property`)===(n.property==null?null:n.property)&&o.getAttribute(`http-equiv`)===(n.httpEquiv==null?null:n.httpEquiv)&&o.getAttribute(`charset`)===(n.charSet==null?null:n.charSet)){s.splice(c,1);break b}}o=i.createElement(r),Pd(o,r,n),i.head.appendChild(o);break;default:throw Error(a(468,r))}o[pt]=e,Et(o),r=o}e.stateNode=r}else Hf(i,e.type,e.stateNode);else e.stateNode=If(i,r,e.memoizedProps);else o===r?r===null&&e.stateNode!==null&&Jc(e,e.memoizedProps,n.memoizedProps):(o===null?n.stateNode!==null&&(n=n.stateNode,n.parentNode.removeChild(n)):o.count--,r===null?Hf(i,e.type,e.stateNode):If(i,r,e.memoizedProps))}break;case 27:hl(t,e),vl(e),r&512&&(el||n===null||qc(n,n.return)),n!==null&&r&4&&Jc(e,e.memoizedProps,n.memoizedProps);break;case 5:if(hl(t,e),vl(e),r&512&&(el||n===null||qc(n,n.return)),e.flags&32){i=e.stateNode;try{Qt(i,``)}catch(t){Z(e,e.return,t)}}r&4&&e.stateNode!=null&&(i=e.memoizedProps,Jc(e,i,n===null?i:n.memoizedProps)),r&1024&&(tl=!0);break;case 6:if(hl(t,e),vl(e),r&4){if(e.stateNode===null)throw Error(a(162));r=e.memoizedProps,n=e.stateNode;try{n.nodeValue=r}catch(t){Z(e,e.return,t)}}break;case 3:if(Bf=null,i=gl,gl=gf(t.containerInfo),hl(t,e),gl=i,vl(e),r&4&&n!==null&&n.memoizedState.isDehydrated)try{Np(t.containerInfo)}catch(t){Z(e,e.return,t)}tl&&(tl=!1,yl(e));break;case 4:r=gl,gl=gf(e.stateNode.containerInfo),hl(t,e),vl(e),gl=r;break;case 12:hl(t,e),vl(e);break;case 31:hl(t,e),vl(e),r&4&&(r=e.updateQueue,r!==null&&(e.updateQueue=null,ml(e,r)));break;case 13:hl(t,e),vl(e),e.child.flags&8192&&e.memoizedState!==null!=(n!==null&&n.memoizedState!==null)&&($l=Me()),r&4&&(r=e.updateQueue,r!==null&&(e.updateQueue=null,ml(e,r)));break;case 22:i=e.memoizedState!==null;var l=n!==null&&n.memoizedState!==null,u=G,d=el;if(G=u||i,el=d||l,hl(t,e),el=d,G=u,vl(e),r&8192)a:for(t=e.stateNode,t._visibility=i?t._visibility&-2:t._visibility|1,i&&(n===null||l||G||el||xl(e)),n=null,t=e;;){if(t.tag===5||t.tag===26){if(n===null){l=n=t;try{if(o=l.stateNode,i)s=o.style,typeof s.setProperty==`function`?s.setProperty(`display`,`none`,`important`):s.display=`none`;else{c=l.stateNode;var f=l.memoizedProps.style,p=f!=null&&f.hasOwnProperty(`display`)?f.display:null;c.style.display=p==null||typeof p==`boolean`?``:(``+p).trim()}}catch(e){Z(l,l.return,e)}}}else if(t.tag===6){if(n===null){l=t;try{l.stateNode.nodeValue=i?``:l.memoizedProps}catch(e){Z(l,l.return,e)}}}else if(t.tag===18){if(n===null){l=t;try{var m=l.stateNode;i?$d(m,!0):$d(l.stateNode,!1)}catch(e){Z(l,l.return,e)}}}else if((t.tag!==22&&t.tag!==23||t.memoizedState===null||t===e)&&t.child!==null){t.child.return=t,t=t.child;continue}if(t===e)break a;for(;t.sibling===null;){if(t.return===null||t.return===e)break a;n===t&&(n=null),t=t.return}n===t&&(n=null),t.sibling.return=t.return,t=t.sibling}r&4&&(r=e.updateQueue,r!==null&&(n=r.retryQueue,n!==null&&(r.retryQueue=null,ml(e,n))));break;case 19:hl(t,e),vl(e),r&4&&(r=e.updateQueue,r!==null&&(e.updateQueue=null,ml(e,r)));break;case 30:break;case 21:break;default:hl(t,e),vl(e)}}function vl(e){var t=e.flags;if(t&2){try{for(var n,r=e.return;r!==null;){if(Yc(r)){n=r;break}r=r.return}if(n==null)throw Error(a(160));switch(n.tag){case 27:var i=n.stateNode;Qc(e,Xc(e),i);break;case 5:var o=n.stateNode;n.flags&32&&(Qt(o,``),n.flags&=-33),Qc(e,Xc(e),o);break;case 3:case 4:var s=n.stateNode.containerInfo;Zc(e,Xc(e),s);break;default:throw Error(a(161))}}catch(t){Z(e,e.return,t)}e.flags&=-3}t&4096&&(e.flags&=-4097)}function yl(e){if(e.subtreeFlags&1024)for(e=e.child;e!==null;){var t=e;yl(t),t.tag===5&&t.flags&1024&&t.stateNode.reset(),e=e.sibling}}function bl(e,t){if(t.subtreeFlags&8772)for(t=t.child;t!==null;)al(e,t.alternate,t),t=t.sibling}function xl(e){for(e=e.child;e!==null;){var t=e;switch(t.tag){case 0:case 11:case 14:case 15:Uc(4,t,t.return),xl(t);break;case 1:qc(t,t.return);var n=t.stateNode;typeof n.componentWillUnmount==`function`&&Gc(t,t.return,n),xl(t);break;case 27:pf(t.stateNode);case 26:case 5:qc(t,t.return),xl(t);break;case 22:t.memoizedState===null&&xl(t);break;case 30:xl(t);break;default:xl(t)}e=e.sibling}}function Sl(e,t,n){for(n&&=(t.subtreeFlags&8772)!=0,t=t.child;t!==null;){var r=t.alternate,i=e,a=t,o=a.flags;switch(a.tag){case 0:case 11:case 15:Sl(i,a,n),Hc(4,a);break;case 1:if(Sl(i,a,n),r=a,i=r.stateNode,typeof i.componentDidMount==`function`)try{i.componentDidMount()}catch(e){Z(r,r.return,e)}if(r=a,i=r.updateQueue,i!==null){var s=r.stateNode;try{var c=i.shared.hiddenCallbacks;if(c!==null)for(i.shared.hiddenCallbacks=null,i=0;i<c.length;i++)$a(c[i],s)}catch(e){Z(r,r.return,e)}}n&&o&64&&Wc(a),Kc(a,a.return);break;case 27:$c(a);case 26:case 5:Sl(i,a,n),n&&r===null&&o&4&&W(a),Kc(a,a.return);break;case 12:Sl(i,a,n);break;case 31:Sl(i,a,n),n&&o&4&&dl(i,a);break;case 13:Sl(i,a,n),n&&o&4&&fl(i,a);break;case 22:a.memoizedState===null&&Sl(i,a,n),Kc(a,a.return);break;case 30:break;default:Sl(i,a,n)}t=t.sibling}}function Cl(e,t){var n=null;e!==null&&e.memoizedState!==null&&e.memoizedState.cachePool!==null&&(n=e.memoizedState.cachePool.pool),e=null,t.memoizedState!==null&&t.memoizedState.cachePool!==null&&(e=t.memoizedState.cachePool.pool),e!==n&&(e!=null&&e.refCount++,n!=null&&fa(n))}function wl(e,t){e=null,t.alternate!==null&&(e=t.alternate.memoizedState.cache),t=t.memoizedState.cache,t!==e&&(t.refCount++,e!=null&&fa(e))}function Tl(e,t,n,r){if(t.subtreeFlags&10256)for(t=t.child;t!==null;)El(e,t,n,r),t=t.sibling}function El(e,t,n,r){var i=t.flags;switch(t.tag){case 0:case 11:case 15:Tl(e,t,n,r),i&2048&&Hc(9,t);break;case 1:Tl(e,t,n,r);break;case 3:Tl(e,t,n,r),i&2048&&(e=null,t.alternate!==null&&(e=t.alternate.memoizedState.cache),t=t.memoizedState.cache,t!==e&&(t.refCount++,e!=null&&fa(e)));break;case 12:if(i&2048){Tl(e,t,n,r),e=t.stateNode;try{var a=t.memoizedProps,o=a.id,s=a.onPostCommit;typeof s==`function`&&s(o,t.alternate===null?`mount`:`update`,e.passiveEffectDuration,-0)}catch(e){Z(t,t.return,e)}}else Tl(e,t,n,r);break;case 31:Tl(e,t,n,r);break;case 13:Tl(e,t,n,r);break;case 23:break;case 22:a=t.stateNode,o=t.alternate,t.memoizedState===null?a._visibility&2?Tl(e,t,n,r):(a._visibility|=2,Dl(e,t,n,r,(t.subtreeFlags&10256)!=0||!1)):a._visibility&2?Tl(e,t,n,r):Ol(e,t),i&2048&&Cl(o,t);break;case 24:Tl(e,t,n,r),i&2048&&wl(t.alternate,t);break;default:Tl(e,t,n,r)}}function Dl(e,t,n,r,i){for(i&&=(t.subtreeFlags&10256)!=0||!1,t=t.child;t!==null;){var a=e,o=t,s=n,c=r,l=o.flags;switch(o.tag){case 0:case 11:case 15:Dl(a,o,s,c,i),Hc(8,o);break;case 23:break;case 22:var u=o.stateNode;o.memoizedState===null?(u._visibility|=2,Dl(a,o,s,c,i)):u._visibility&2?Dl(a,o,s,c,i):Ol(a,o),i&&l&2048&&Cl(o.alternate,o);break;case 24:Dl(a,o,s,c,i),i&&l&2048&&wl(o.alternate,o);break;default:Dl(a,o,s,c,i)}t=t.sibling}}function Ol(e,t){if(t.subtreeFlags&10256)for(t=t.child;t!==null;){var n=e,r=t,i=r.flags;switch(r.tag){case 22:Ol(n,r),i&2048&&Cl(r.alternate,r);break;case 24:Ol(n,r),i&2048&&wl(r.alternate,r);break;default:Ol(n,r)}t=t.sibling}}var kl=8192;function Al(e,t,n){if(e.subtreeFlags&kl)for(e=e.child;e!==null;)jl(e,t,n),e=e.sibling}function jl(e,t,n){switch(e.tag){case 26:Al(e,t,n),e.flags&kl&&e.memoizedState!==null&&Gf(n,gl,e.memoizedState,e.memoizedProps);break;case 5:Al(e,t,n);break;case 3:case 4:var r=gl;gl=gf(e.stateNode.containerInfo),Al(e,t,n),gl=r;break;case 22:e.memoizedState===null&&(r=e.alternate,r!==null&&r.memoizedState!==null?(r=kl,kl=16777216,Al(e,t,n),kl=r):Al(e,t,n));break;default:Al(e,t,n)}}function Ml(e){var t=e.alternate;if(t!==null&&(e=t.child,e!==null)){t.child=null;do t=e.sibling,e.sibling=null,e=t;while(e!==null)}}function Nl(e){var t=e.deletions;if(e.flags&16){if(t!==null)for(var n=0;n<t.length;n++){var r=t[n];rl=r,Il(r,e)}Ml(e)}if(e.subtreeFlags&10256)for(e=e.child;e!==null;)Pl(e),e=e.sibling}function Pl(e){switch(e.tag){case 0:case 11:case 15:Nl(e),e.flags&2048&&Uc(9,e,e.return);break;case 3:Nl(e);break;case 12:Nl(e);break;case 22:var t=e.stateNode;e.memoizedState!==null&&t._visibility&2&&(e.return===null||e.return.tag!==13)?(t._visibility&=-3,Fl(e)):Nl(e);break;default:Nl(e)}}function Fl(e){var t=e.deletions;if(e.flags&16){if(t!==null)for(var n=0;n<t.length;n++){var r=t[n];rl=r,Il(r,e)}Ml(e)}for(e=e.child;e!==null;){switch(t=e,t.tag){case 0:case 11:case 15:Uc(8,t,t.return),Fl(t);break;case 22:n=t.stateNode,n._visibility&2&&(n._visibility&=-3,Fl(t));break;default:Fl(t)}e=e.sibling}}function Il(e,t){for(;rl!==null;){var n=rl;switch(n.tag){case 0:case 11:case 15:Uc(8,n,t);break;case 23:case 22:if(n.memoizedState!==null&&n.memoizedState.cachePool!==null){var r=n.memoizedState.cachePool.pool;r!=null&&r.refCount++}break;case 24:fa(n.memoizedState.cache)}if(r=n.child,r!==null)r.return=n,rl=r;else a:for(n=e;rl!==null;){r=rl;var i=r.sibling,a=r.return;if(ol(r),r===n){rl=null;break a}if(i!==null){i.return=a,rl=i;break a}rl=a}}}var Ll={getCacheForType:function(e){var t=ia(ua),n=t.data.get(e);return n===void 0&&(n=e(),t.data.set(e,n)),n},cacheSignal:function(){return ia(ua).controller.signal}},Rl=typeof WeakMap==`function`?WeakMap:Map,K=0,q=null,J=null,Y=0,X=0,zl=null,Bl=!1,Vl=!1,Hl=!1,Ul=0,Wl=0,Gl=0,Kl=0,ql=0,Jl=0,Yl=0,Xl=null,Zl=null,Ql=!1,$l=0,eu=0,tu=1/0,nu=null,ru=null,iu=0,au=null,ou=null,su=0,cu=0,lu=null,uu=null,du=0,fu=null;function pu(){return K&2&&Y!==0?Y&-Y:D.T===null?ut():dd()}function mu(){if(Jl===0)if(!(Y&536870912)||F){var e=Ye;Ye<<=1,!(Ye&3932160)&&(Ye=262144),Jl=e}else Jl=536870912;return e=oo.current,e!==null&&(e.flags|=32),Jl}function hu(e,t,n){(e===q&&(X===2||X===9)||e.cancelPendingCommit!==null)&&(Su(e,0),yu(e,Y,Jl,!1)),rt(e,n),(!(K&2)||e!==q)&&(e===q&&(!(K&2)&&(Kl|=n),Wl===4&&yu(e,Y,Jl,!1)),rd(e))}function gu(e,t,n){if(K&6)throw Error(a(327));var r=!n&&(t&127)==0&&(t&e.expiredLanes)===0||$e(e,t),i=r?Au(e,t):Ou(e,t,!0),o=r;do{if(i===0){Vl&&!r&&yu(e,t,0,!1);break}else{if(n=e.current.alternate,o&&!vu(n)){i=Ou(e,t,!1),o=!1;continue}if(i===2){if(o=t,e.errorRecoveryDisabledLanes&o)var s=0;else s=e.pendingLanes&-536870913,s=s===0?s&536870912?536870912:0:s;if(s!==0){t=s;a:{var c=e;i=Xl;var l=c.current.memoizedState.isDehydrated;if(l&&(Su(c,s).flags|=256),s=Ou(c,s,!1),s!==2){if(Hl&&!l){c.errorRecoveryDisabledLanes|=o,Kl|=o,i=4;break a}o=Zl,Zl=i,o!==null&&(Zl===null?Zl=o:Zl.push.apply(Zl,o))}i=s}if(o=!1,i!==2)continue}}if(i===1){Su(e,0),yu(e,t,0,!0);break}a:{switch(r=e,o=i,o){case 0:case 1:throw Error(a(345));case 4:if((t&4194048)!==t)break;case 6:yu(r,t,Jl,!Bl);break a;case 2:Zl=null;break;case 3:case 5:break;default:throw Error(a(329))}if((t&62914560)===t&&(i=$l+300-Me(),10<i)){if(yu(r,t,Jl,!Bl),Qe(r,0,!0)!==0)break a;su=t,r.timeoutHandle=Kd(_u.bind(null,r,n,Zl,nu,Ql,t,Jl,Kl,Yl,Bl,o,`Throttled`,-0,0),i);break a}_u(r,n,Zl,nu,Ql,t,Jl,Kl,Yl,Bl,o,null,-0,0)}}break}while(1);rd(e)}function _u(e,t,n,r,i,a,o,s,c,l,u,d,f,p){if(e.timeoutHandle=-1,d=t.subtreeFlags,d&8192||(d&16785408)==16785408){d={stylesheets:null,count:0,imgCount:0,imgBytes:0,suspenseyImages:[],waitingForImages:!0,waitingForViewTransition:!1,unsuspend:sn},jl(t,a,d);var m=(a&62914560)===a?$l-Me():(a&4194048)===a?eu-Me():0;if(m=qf(d,m),m!==null){su=a,e.cancelPendingCommit=m(Lu.bind(null,e,t,a,n,r,i,o,s,c,u,d,null,f,p)),yu(e,a,o,!l);return}}Lu(e,t,a,n,r,i,o,s,c)}function vu(e){for(var t=e;;){var n=t.tag;if((n===0||n===11||n===15)&&t.flags&16384&&(n=t.updateQueue,n!==null&&(n=n.stores,n!==null)))for(var r=0;r<n.length;r++){var i=n[r],a=i.getSnapshot;i=i.value;try{if(!Or(a(),i))return!1}catch{return!1}}if(n=t.child,t.subtreeFlags&16384&&n!==null)n.return=t,t=n;else{if(t===e)break;for(;t.sibling===null;){if(t.return===null||t.return===e)return!0;t=t.return}t.sibling.return=t.return,t=t.sibling}}return!0}function yu(e,t,n,r){t&=~ql,t&=~Kl,e.suspendedLanes|=t,e.pingedLanes&=~t,r&&(e.warmLanes|=t),r=e.expirationTimes;for(var i=t;0<i;){var a=31-We(i),o=1<<a;r[a]=-1,i&=~o}n!==0&&at(e,n,t)}function bu(){return K&6?!0:(id(0,!1),!1)}function xu(){if(J!==null){if(X===0)var e=J.return;else e=J,Xi=Yi=null,V(e),Fa=null,Ia=0,e=J;for(;e!==null;)Vc(e.alternate,e),e=e.return;J=null}}function Su(e,t){var n=e.timeoutHandle;n!==-1&&(e.timeoutHandle=-1,qd(n)),n=e.cancelPendingCommit,n!==null&&(e.cancelPendingCommit=null,n()),su=0,xu(),q=e,J=n=gi(e.current,null),Y=t,X=0,zl=null,Bl=!1,Vl=$e(e,t),Hl=!1,Yl=Jl=ql=Kl=Gl=Wl=0,Zl=Xl=null,Ql=!1,t&8&&(t|=t&32);var r=e.entangledLanes;if(r!==0)for(e=e.entanglements,r&=t;0<r;){var i=31-We(r),a=1<<i;t|=e[i],r&=~a}return Ul=t,oi(),n}function Cu(e,t){L=null,D.H=zs,t===Ta||t===Da?(t=Na(),X=3):t===Ea?(t=Na(),X=4):X=t===rc?8:typeof t==`object`&&t&&typeof t.then==`function`?6:1,zl=t,J===null&&(Wl=1,Zs(e,wi(t,e.current)))}function wu(){var e=oo.current;return e===null?!0:(Y&4194048)===Y?so===null:(Y&62914560)===Y||Y&536870912?e===so:!1}function Tu(){var e=D.H;return D.H=zs,e===null?zs:e}function Eu(){var e=D.A;return D.A=Ll,e}function Du(){Wl=4,Bl||(Y&4194048)!==Y&&oo.current!==null||(Vl=!0),!(Gl&134217727)&&!(Kl&134217727)||q===null||yu(q,Y,Jl,!1)}function Ou(e,t,n){var r=K;K|=2;var i=Tu(),a=Eu();(q!==e||Y!==t)&&(nu=null,Su(e,t)),t=!1;var o=Wl;a:do try{if(X!==0&&J!==null){var s=J,c=zl;switch(X){case 8:xu(),o=6;break a;case 3:case 2:case 9:case 6:oo.current===null&&(t=!0);var l=X;if(X=0,zl=null,Pu(e,s,c,l),n&&Vl){o=0;break a}break;default:l=X,X=0,zl=null,Pu(e,s,c,l)}}ku(),o=Wl;break}catch(t){Cu(e,t)}while(1);return t&&e.shellSuspendCounter++,Xi=Yi=null,K=r,D.H=i,D.A=a,J===null&&(q=null,Y=0,oi()),o}function ku(){for(;J!==null;)Mu(J)}function Au(e,t){var n=K;K|=2;var r=Tu(),i=Eu();q!==e||Y!==t?(nu=null,tu=Me()+500,Su(e,t)):Vl=$e(e,t);a:do try{if(X!==0&&J!==null){t=J;var o=zl;b:switch(X){case 1:X=0,zl=null,Pu(e,t,o,1);break;case 2:case 9:if(ka(o)){X=0,zl=null,Nu(t);break}t=function(){X!==2&&X!==9||q!==e||(X=7),rd(e)},o.then(t,t);break a;case 3:X=7;break a;case 4:X=5;break a;case 7:ka(o)?(X=0,zl=null,Nu(t)):(X=0,zl=null,Pu(e,t,o,7));break;case 5:var s=null;switch(J.tag){case 26:s=J.memoizedState;case 5:case 27:var c=J;if(s?Wf(s):c.stateNode.complete){X=0,zl=null;var l=c.sibling;if(l!==null)J=l;else{var u=c.return;u===null?J=null:(J=u,Fu(u))}break b}}X=0,zl=null,Pu(e,t,o,5);break;case 6:X=0,zl=null,Pu(e,t,o,6);break;case 8:xu(),Wl=6;break a;default:throw Error(a(462))}}ju();break}catch(t){Cu(e,t)}while(1);return Xi=Yi=null,D.H=r,D.A=i,K=n,J===null?(q=null,Y=0,oi(),Wl):0}function ju(){for(;J!==null&&!Ae();)Mu(J)}function Mu(e){var t=Nc(e.alternate,e,Ul);e.memoizedProps=e.pendingProps,t===null?Fu(e):J=t}function Nu(e){var t=e,n=t.alternate;switch(t.tag){case 15:case 0:t=_c(n,t,t.pendingProps,t.type,void 0,Y);break;case 11:t=_c(n,t,t.pendingProps,t.type.render,t.ref,Y);break;case 5:V(t);default:Vc(n,t),t=J=_i(t,Ul),t=Nc(n,t,Ul)}e.memoizedProps=e.pendingProps,t===null?Fu(e):J=t}function Pu(e,t,n,r){Xi=Yi=null,V(t),Fa=null,Ia=0;var i=t.return;try{if(nc(e,i,t,n,Y)){Wl=1,Zs(e,wi(n,e.current)),J=null;return}}catch(t){if(i!==null)throw J=i,t;Wl=1,Zs(e,wi(n,e.current)),J=null;return}t.flags&32768?(F||r===1?e=!0:Vl||Y&536870912?e=!1:(Bl=e=!0,(r===2||r===9||r===3||r===6)&&(r=oo.current,r!==null&&r.tag===13&&(r.flags|=16384))),Iu(t,e)):Fu(t)}function Fu(e){var t=e;do{if(t.flags&32768){Iu(t,Bl);return}e=t.return;var n=zc(t.alternate,t,Ul);if(n!==null){J=n;return}if(t=t.sibling,t!==null){J=t;return}J=t=e}while(t!==null);Wl===0&&(Wl=5)}function Iu(e,t){do{var n=Bc(e.alternate,e);if(n!==null){n.flags&=32767,J=n;return}if(n=e.return,n!==null&&(n.flags|=32768,n.subtreeFlags=0,n.deletions=null),!t&&(e=e.sibling,e!==null)){J=e;return}J=e=n}while(e!==null);Wl=6,J=null}function Lu(e,t,n,r,i,o,s,c,l){e.cancelPendingCommit=null;do Hu();while(iu!==0);if(K&6)throw Error(a(327));if(t!==null){if(t===e.current)throw Error(a(177));if(o=t.lanes|t.childLanes,o|=ai,it(e,n,o,s,c,l),e===q&&(J=q=null,Y=0),ou=t,au=e,su=n,cu=o,lu=i,uu=r,t.subtreeFlags&10256||t.flags&10256?(e.callbackNode=null,e.callbackPriority=0,Xu(Ie,function(){return Uu(),null})):(e.callbackNode=null,e.callbackPriority=0),r=(t.flags&13878)!=0,t.subtreeFlags&13878||r){r=D.T,D.T=null,i=O.p,O.p=2,s=K,K|=4;try{il(e,t,n)}finally{K=s,O.p=i,D.T=r}}iu=1,Ru(),zu(),Bu()}}function Ru(){if(iu===1){iu=0;var e=au,t=ou,n=(t.flags&13878)!=0;if(t.subtreeFlags&13878||n){n=D.T,D.T=null;var r=O.p;O.p=2;var i=K;K|=4;try{_l(t,e);var a=zd,o=Nr(e.containerInfo),s=a.focusedElem,c=a.selectionRange;if(o!==s&&s&&s.ownerDocument&&Mr(s.ownerDocument.documentElement,s)){if(c!==null&&Pr(s)){var l=c.start,u=c.end;if(u===void 0&&(u=l),`selectionStart`in s)s.selectionStart=l,s.selectionEnd=Math.min(u,s.value.length);else{var d=s.ownerDocument||document,f=d&&d.defaultView||window;if(f.getSelection){var p=f.getSelection(),m=s.textContent.length,h=Math.min(c.start,m),g=c.end===void 0?h:Math.min(c.end,m);!p.extend&&h>g&&(o=g,g=h,h=o);var _=jr(s,h),v=jr(s,g);if(_&&v&&(p.rangeCount!==1||p.anchorNode!==_.node||p.anchorOffset!==_.offset||p.focusNode!==v.node||p.focusOffset!==v.offset)){var y=d.createRange();y.setStart(_.node,_.offset),p.removeAllRanges(),h>g?(p.addRange(y),p.extend(v.node,v.offset)):(y.setEnd(v.node,v.offset),p.addRange(y))}}}}for(d=[],p=s;p=p.parentNode;)p.nodeType===1&&d.push({element:p,left:p.scrollLeft,top:p.scrollTop});for(typeof s.focus==`function`&&s.focus(),s=0;s<d.length;s++){var b=d[s];b.element.scrollLeft=b.left,b.element.scrollTop=b.top}}sp=!!Rd,zd=Rd=null}finally{K=i,O.p=r,D.T=n}}e.current=t,iu=2}}function zu(){if(iu===2){iu=0;var e=au,t=ou,n=(t.flags&8772)!=0;if(t.subtreeFlags&8772||n){n=D.T,D.T=null;var r=O.p;O.p=2;var i=K;K|=4;try{al(e,t.alternate,t)}finally{K=i,O.p=r,D.T=n}}iu=3}}function Bu(){if(iu===4||iu===3){iu=0,je();var e=au,t=ou,n=su,r=uu;t.subtreeFlags&10256||t.flags&10256?iu=5:(iu=0,ou=au=null,Vu(e,e.pendingLanes));var i=e.pendingLanes;if(i===0&&(ru=null),lt(n),t=t.stateNode,He&&typeof He.onCommitFiberRoot==`function`)try{He.onCommitFiberRoot(Ve,t,void 0,(t.current.flags&128)==128)}catch{}if(r!==null){t=D.T,i=O.p,O.p=2,D.T=null;try{for(var a=e.onRecoverableError,o=0;o<r.length;o++){var s=r[o];a(s.value,{componentStack:s.stack})}}finally{D.T=t,O.p=i}}su&3&&Hu(),rd(e),i=e.pendingLanes,n&261930&&i&42?e===fu?du++:(du=0,fu=e):du=0,id(0,!1)}}function Vu(e,t){(e.pooledCacheLanes&=t)===0&&(t=e.pooledCache,t!=null&&(e.pooledCache=null,fa(t)))}function Hu(){return Ru(),zu(),Bu(),Uu()}function Uu(){if(iu!==5)return!1;var e=au,t=cu;cu=0;var n=lt(su),r=D.T,i=O.p;try{O.p=32>n?32:n,D.T=null,n=lu,lu=null;var o=au,s=su;if(iu=0,ou=au=null,su=0,K&6)throw Error(a(331));var c=K;if(K|=4,Pl(o.current),El(o,o.current,s,n),K=c,id(0,!1),He&&typeof He.onPostCommitFiberRoot==`function`)try{He.onPostCommitFiberRoot(Ve,o)}catch{}return!0}finally{O.p=i,D.T=r,Vu(e,t)}}function Wu(e,t,n){t=wi(n,t),t=$s(e.stateNode,t,2),e=qa(e,t,2),e!==null&&(rt(e,2),rd(e))}function Z(e,t,n){if(e.tag===3)Wu(e,e,n);else for(;t!==null;){if(t.tag===3){Wu(t,e,n);break}else if(t.tag===1){var r=t.stateNode;if(typeof t.type.getDerivedStateFromError==`function`||typeof r.componentDidCatch==`function`&&(ru===null||!ru.has(r))){e=wi(n,e),n=ec(2),r=qa(t,n,2),r!==null&&(tc(n,r,t,e),rt(r,2),rd(r));break}}t=t.return}}function Gu(e,t,n){var r=e.pingCache;if(r===null){r=e.pingCache=new Rl;var i=new Set;r.set(t,i)}else i=r.get(t),i===void 0&&(i=new Set,r.set(t,i));i.has(n)||(Hl=!0,i.add(n),e=Ku.bind(null,e,t,n),t.then(e,e))}function Ku(e,t,n){var r=e.pingCache;r!==null&&r.delete(t),e.pingedLanes|=e.suspendedLanes&n,e.warmLanes&=~n,q===e&&(Y&n)===n&&(Wl===4||Wl===3&&(Y&62914560)===Y&&300>Me()-$l?!(K&2)&&Su(e,0):ql|=n,Yl===Y&&(Yl=0)),rd(e)}function qu(e,t){t===0&&(t=tt()),e=li(e,t),e!==null&&(rt(e,t),rd(e))}function Ju(e){var t=e.memoizedState,n=0;t!==null&&(n=t.retryLane),qu(e,n)}function Yu(e,t){var n=0;switch(e.tag){case 31:case 13:var r=e.stateNode,i=e.memoizedState;i!==null&&(n=i.retryLane);break;case 19:r=e.stateNode;break;case 22:r=e.stateNode._retryCache;break;default:throw Error(a(314))}r!==null&&r.delete(t),qu(e,n)}function Xu(e,t){return Oe(e,t)}var Zu=null,Qu=null,$u=!1,ed=!1,td=!1,nd=0;function rd(e){e!==Qu&&e.next===null&&(Qu===null?Zu=Qu=e:Qu=Qu.next=e),ed=!0,$u||($u=!0,ud())}function id(e,t){if(!td&&ed){td=!0;do for(var n=!1,r=Zu;r!==null;){if(!t)if(e!==0){var i=r.pendingLanes;if(i===0)var a=0;else{var o=r.suspendedLanes,s=r.pingedLanes;a=(1<<31-We(42|e)+1)-1,a&=i&~(o&~s),a=a&201326741?a&201326741|1:a?a|2:0}a!==0&&(n=!0,ld(r,a))}else a=Y,a=Qe(r,r===q?a:0,r.cancelPendingCommit!==null||r.timeoutHandle!==-1),!(a&3)||$e(r,a)||(n=!0,ld(r,a));r=r.next}while(n);td=!1}}function ad(){od()}function od(){ed=$u=!1;var e=0;nd!==0&&Gd()&&(e=nd);for(var t=Me(),n=null,r=Zu;r!==null;){var i=r.next,a=sd(r,t);a===0?(r.next=null,n===null?Zu=i:n.next=i,i===null&&(Qu=n)):(n=r,(e!==0||a&3)&&(ed=!0)),r=i}iu!==0&&iu!==5||id(e,!1),nd!==0&&(nd=0)}function sd(e,t){for(var n=e.suspendedLanes,r=e.pingedLanes,i=e.expirationTimes,a=e.pendingLanes&-62914561;0<a;){var o=31-We(a),s=1<<o,c=i[o];c===-1?((s&n)===0||(s&r)!==0)&&(i[o]=et(s,t)):c<=t&&(e.expiredLanes|=s),a&=~s}if(t=q,n=Y,n=Qe(e,e===t?n:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),r=e.callbackNode,n===0||e===t&&(X===2||X===9)||e.cancelPendingCommit!==null)return r!==null&&r!==null&&ke(r),e.callbackNode=null,e.callbackPriority=0;if(!(n&3)||$e(e,n)){if(t=n&-n,t===e.callbackPriority)return t;switch(r!==null&&ke(r),lt(n)){case 2:case 8:n=Fe;break;case 32:n=Ie;break;case 268435456:n=Re;break;default:n=Ie}return r=cd.bind(null,e),n=Oe(n,r),e.callbackPriority=t,e.callbackNode=n,t}return r!==null&&r!==null&&ke(r),e.callbackPriority=2,e.callbackNode=null,2}function cd(e,t){if(iu!==0&&iu!==5)return e.callbackNode=null,e.callbackPriority=0,null;var n=e.callbackNode;if(Hu()&&e.callbackNode!==n)return null;var r=Y;return r=Qe(e,e===q?r:0,e.cancelPendingCommit!==null||e.timeoutHandle!==-1),r===0?null:(gu(e,r,t),sd(e,Me()),e.callbackNode!=null&&e.callbackNode===n?cd.bind(null,e):null)}function ld(e,t){if(Hu())return null;gu(e,t,!0)}function ud(){Yd(function(){K&6?Oe(Pe,ad):od()})}function dd(){if(nd===0){var e=ha;e===0&&(e=Je,Je<<=1,!(Je&261888)&&(Je=256)),nd=e}return nd}function fd(e){return e==null||typeof e==`symbol`||typeof e==`boolean`?null:typeof e==`function`?e:on(``+e)}function pd(e,t){var n=t.ownerDocument.createElement(`input`);return n.name=t.name,n.value=t.value,e.id&&n.setAttribute(`form`,e.id),t.parentNode.insertBefore(n,t),e=new FormData(e),n.parentNode.removeChild(n),e}function md(e,t,n,r,i){if(t===`submit`&&n&&n.stateNode===i){var a=fd((i[mt]||null).action),o=r.submitter;o&&(t=(t=o[mt]||null)?fd(t.formAction):o.getAttribute(`formAction`),t!==null&&(a=t,o=null));var s=new On(`action`,`action`,null,r,i);e.push({event:s,listeners:[{instance:null,listener:function(){if(r.defaultPrevented){if(nd!==0){var e=o?pd(i,o):new FormData(i);Ts(n,{pending:!0,data:e,method:i.method,action:a},null,e)}}else typeof a==`function`&&(s.preventDefault(),e=o?pd(i,o):new FormData(i),Ts(n,{pending:!0,data:e,method:i.method,action:a},a,e))},currentTarget:i}]})}}for(var hd=0;hd<ei.length;hd++){var gd=ei[hd];ti(gd.toLowerCase(),`on`+(gd[0].toUpperCase()+gd.slice(1)))}ti(Kr,`onAnimationEnd`),ti(qr,`onAnimationIteration`),ti(Jr,`onAnimationStart`),ti(`dblclick`,`onDoubleClick`),ti(`focusin`,`onFocus`),ti(`focusout`,`onBlur`),ti(Yr,`onTransitionRun`),ti(Xr,`onTransitionStart`),ti(Zr,`onTransitionCancel`),ti(Qr,`onTransitionEnd`),At(`onMouseEnter`,[`mouseout`,`mouseover`]),At(`onMouseLeave`,[`mouseout`,`mouseover`]),At(`onPointerEnter`,[`pointerout`,`pointerover`]),At(`onPointerLeave`,[`pointerout`,`pointerover`]),kt(`onChange`,`change click focusin focusout input keydown keyup selectionchange`.split(` `)),kt(`onSelect`,`focusout contextmenu dragend focusin keydown keyup mousedown mouseup selectionchange`.split(` `)),kt(`onBeforeInput`,[`compositionend`,`keypress`,`textInput`,`paste`]),kt(`onCompositionEnd`,`compositionend focusout keydown keypress keyup mousedown`.split(` `)),kt(`onCompositionStart`,`compositionstart focusout keydown keypress keyup mousedown`.split(` `)),kt(`onCompositionUpdate`,`compositionupdate focusout keydown keypress keyup mousedown`.split(` `));var _d=`abort canplay canplaythrough durationchange emptied encrypted ended error loadeddata loadedmetadata loadstart pause play playing progress ratechange resize seeked seeking stalled suspend timeupdate volumechange waiting`.split(` `),vd=new Set(`beforetoggle cancel close invalid load scroll scrollend toggle`.split(` `).concat(_d));function yd(e,t){t=(t&4)!=0;for(var n=0;n<e.length;n++){var r=e[n],i=r.event;r=r.listeners;a:{var a=void 0;if(t)for(var o=r.length-1;0<=o;o--){var s=r[o],c=s.instance,l=s.currentTarget;if(s=s.listener,c!==a&&i.isPropagationStopped())break a;a=s,i.currentTarget=l;try{a(i)}catch(e){ni(e)}i.currentTarget=null,a=c}else for(o=0;o<r.length;o++){if(s=r[o],c=s.instance,l=s.currentTarget,s=s.listener,c!==a&&i.isPropagationStopped())break a;a=s,i.currentTarget=l;try{a(i)}catch(e){ni(e)}i.currentTarget=null,a=c}}}}function Q(e,t){var n=t[gt];n===void 0&&(n=t[gt]=new Set);var r=e+`__bubble`;n.has(r)||(Cd(t,e,2,!1),n.add(r))}function bd(e,t,n){var r=0;t&&(r|=4),Cd(n,e,r,t)}var xd=`_reactListening`+Math.random().toString(36).slice(2);function Sd(e){if(!e[xd]){e[xd]=!0,Dt.forEach(function(t){t!==`selectionchange`&&(vd.has(t)||bd(t,!1,e),bd(t,!0,e))});var t=e.nodeType===9?e:e.ownerDocument;t===null||t[xd]||(t[xd]=!0,bd(`selectionchange`,!1,t))}}function Cd(e,t,n,r){switch(mp(t)){case 2:var i=cp;break;case 8:i=lp;break;default:i=up}n=i.bind(null,t,n,e),i=void 0,!_n||t!==`touchstart`&&t!==`touchmove`&&t!==`wheel`||(i=!0),r?i===void 0?e.addEventListener(t,n,!0):e.addEventListener(t,n,{capture:!0,passive:i}):i===void 0?e.addEventListener(t,n,!1):e.addEventListener(t,n,{passive:i})}function wd(e,t,n,r,i){var a=r;if(!(t&1)&&!(t&2)&&r!==null)a:for(;;){if(r===null)return;var o=r.tag;if(o===3||o===4){var c=r.stateNode.containerInfo;if(c===i)break;if(o===4)for(o=r.return;o!==null;){var l=o.tag;if((l===3||l===4)&&o.stateNode.containerInfo===i)return;o=o.return}for(;c!==null;){if(o=St(c),o===null)return;if(l=o.tag,l===5||l===6||l===26||l===27){r=a=o;continue a}c=c.parentNode}}r=r.return}mn(function(){var r=a,i=ln(n),o=[];a:{var c=$r.get(e);if(c!==void 0){var l=On,u=e;switch(e){case`keypress`:if(Cn(n)===0)break a;case`keydown`:case`keyup`:l=Kn;break;case`focusin`:u=`focus`,l=Ln;break;case`focusout`:u=`blur`,l=Ln;break;case`beforeblur`:case`afterblur`:l=Ln;break;case`click`:if(n.button===2)break a;case`auxclick`:case`dblclick`:case`mousedown`:case`mousemove`:case`mouseup`:case`mouseout`:case`mouseover`:case`contextmenu`:l=Fn;break;case`drag`:case`dragend`:case`dragenter`:case`dragexit`:case`dragleave`:case`dragover`:case`dragstart`:case`drop`:l=In;break;case`touchcancel`:case`touchend`:case`touchmove`:case`touchstart`:l=Jn;break;case Kr:case qr:case Jr:l=Rn;break;case Qr:l=Yn;break;case`scroll`:case`scrollend`:l=An;break;case`wheel`:l=Xn;break;case`copy`:case`cut`:case`paste`:l=zn;break;case`gotpointercapture`:case`lostpointercapture`:case`pointercancel`:case`pointerdown`:case`pointermove`:case`pointerout`:case`pointerover`:case`pointerup`:l=qn;break;case`toggle`:case`beforetoggle`:l=Zn}var d=(t&4)!=0,f=!d&&(e===`scroll`||e===`scrollend`),p=d?c===null?null:c+`Capture`:c;d=[];for(var m=r,h;m!==null;){var g=m;if(h=g.stateNode,g=g.tag,g!==5&&g!==26&&g!==27||h===null||p===null||(g=hn(m,p),g!=null&&d.push(Td(m,g,h))),f)break;m=m.return}0<d.length&&(c=new l(c,u,null,n,i),o.push({event:c,listeners:d}))}}if(!(t&7)){a:{if(c=e===`mouseover`||e===`pointerover`,l=e===`mouseout`||e===`pointerout`,c&&n!==cn&&(u=n.relatedTarget||n.fromElement)&&(St(u)||u[ht]))break a;if((l||c)&&(c=i.window===i?i:(c=i.ownerDocument)?c.defaultView||c.parentWindow:window,l?(u=n.relatedTarget||n.toElement,l=r,u=u?St(u):null,u!==null&&(f=s(u),d=u.tag,u!==f||d!==5&&d!==27&&d!==6)&&(u=null)):(l=null,u=r),l!==u)){if(d=Fn,g=`onMouseLeave`,p=`onMouseEnter`,m=`mouse`,(e===`pointerout`||e===`pointerover`)&&(d=qn,g=`onPointerLeave`,p=`onPointerEnter`,m=`pointer`),f=l==null?c:wt(l),h=u==null?c:wt(u),c=new d(g,m+`leave`,l,n,i),c.target=f,c.relatedTarget=h,g=null,St(i)===r&&(d=new d(p,m+`enter`,u,n,i),d.target=h,d.relatedTarget=f,g=d),f=g,l&&u)b:{for(d=Dd,p=l,m=u,h=0,g=p;g;g=d(g))h++;g=0;for(var _=m;_;_=d(_))g++;for(;0<h-g;)p=d(p),h--;for(;0<g-h;)m=d(m),g--;for(;h--;){if(p===m||m!==null&&p===m.alternate){d=p;break b}p=d(p),m=d(m)}d=null}else d=null;l!==null&&Od(o,c,l,d,!1),u!==null&&f!==null&&Od(o,f,u,d,!0)}}a:{if(c=r?wt(r):window,l=c.nodeName&&c.nodeName.toLowerCase(),l===`select`||l===`input`&&c.type===`file`)var v=_r;else if(dr(c))if(vr)v=Er;else{v=wr;var y=Cr}else l=c.nodeName,!l||l.toLowerCase()!==`input`||c.type!==`checkbox`&&c.type!==`radio`?r&&nn(r.elementType)&&(v=_r):v=Tr;if(v&&=v(e,r)){fr(o,v,n,i);break a}y&&y(e,c,r),e===`focusout`&&r&&c.type===`number`&&r.memoizedProps.value!=null&&Jt(c,`number`,c.value)}switch(y=r?wt(r):window,e){case`focusin`:(dr(y)||y.contentEditable===`true`)&&(Ir=y,Lr=r,Rr=null);break;case`focusout`:Rr=Lr=Ir=null;break;case`mousedown`:zr=!0;break;case`contextmenu`:case`mouseup`:case`dragend`:zr=!1,Br(o,n,i);break;case`selectionchange`:if(Fr)break;case`keydown`:case`keyup`:Br(o,n,i)}var b;if($n)b:{switch(e){case`compositionstart`:var x=`onCompositionStart`;break b;case`compositionend`:x=`onCompositionEnd`;break b;case`compositionupdate`:x=`onCompositionUpdate`;break b}x=void 0}else sr?ar(e,n)&&(x=`onCompositionEnd`):e===`keydown`&&n.keyCode===229&&(x=`onCompositionStart`);x&&(nr&&n.locale!==`ko`&&(sr||x!==`onCompositionStart`?x===`onCompositionEnd`&&sr&&(b=Sn()):(yn=i,bn=`value`in yn?yn.value:yn.textContent,sr=!0)),y=Ed(r,x),0<y.length&&(x=new Bn(x,e,null,n,i),o.push({event:x,listeners:y}),b?x.data=b:(b=or(n),b!==null&&(x.data=b)))),(b=tr?cr(e,n):lr(e,n))&&(x=Ed(r,`onBeforeInput`),0<x.length&&(y=new Bn(`onBeforeInput`,`beforeinput`,null,n,i),o.push({event:y,listeners:x}),y.data=b)),md(o,e,r,n,i)}yd(o,t)})}function Td(e,t,n){return{instance:e,listener:t,currentTarget:n}}function Ed(e,t){for(var n=t+`Capture`,r=[];e!==null;){var i=e,a=i.stateNode;if(i=i.tag,i!==5&&i!==26&&i!==27||a===null||(i=hn(e,n),i!=null&&r.unshift(Td(e,i,a)),i=hn(e,t),i!=null&&r.push(Td(e,i,a))),e.tag===3)return r;e=e.return}return[]}function Dd(e){if(e===null)return null;do e=e.return;while(e&&e.tag!==5&&e.tag!==27);return e||null}function Od(e,t,n,r,i){for(var a=t._reactName,o=[];n!==null&&n!==r;){var s=n,c=s.alternate,l=s.stateNode;if(s=s.tag,c!==null&&c===r)break;s!==5&&s!==26&&s!==27||l===null||(c=l,i?(l=hn(n,a),l!=null&&o.unshift(Td(n,l,c))):i||(l=hn(n,a),l!=null&&o.push(Td(n,l,c)))),n=n.return}o.length!==0&&e.push({event:t,listeners:o})}var kd=/\r\n?/g,Ad=/\u0000|\uFFFD/g;function jd(e){return(typeof e==`string`?e:``+e).replace(kd,`
`).replace(Ad,``)}function Md(e,t){return t=jd(t),jd(e)===t}function $(e,t,n,r,i,o){switch(n){case`children`:typeof r==`string`?t===`body`||t===`textarea`&&r===``||Qt(e,r):(typeof r==`number`||typeof r==`bigint`)&&t!==`body`&&Qt(e,``+r);break;case`className`:It(e,`class`,r);break;case`tabIndex`:It(e,`tabindex`,r);break;case`dir`:case`role`:case`viewBox`:case`width`:case`height`:It(e,n,r);break;case`style`:tn(e,r,o);break;case`data`:if(t!==`object`){It(e,`data`,r);break}case`src`:case`href`:if(r===``&&(t!==`a`||n!==`href`)){e.removeAttribute(n);break}if(r==null||typeof r==`function`||typeof r==`symbol`||typeof r==`boolean`){e.removeAttribute(n);break}r=on(``+r),e.setAttribute(n,r);break;case`action`:case`formAction`:if(typeof r==`function`){e.setAttribute(n,`javascript:throw new Error('A React form was unexpectedly submitted. If you called form.submit() manually, consider using form.requestSubmit() instead. If you\\'re trying to use event.stopPropagation() in a submit event handler, consider also calling event.preventDefault().')`);break}else typeof o==`function`&&(n===`formAction`?(t!==`input`&&$(e,t,`name`,i.name,i,null),$(e,t,`formEncType`,i.formEncType,i,null),$(e,t,`formMethod`,i.formMethod,i,null),$(e,t,`formTarget`,i.formTarget,i,null)):($(e,t,`encType`,i.encType,i,null),$(e,t,`method`,i.method,i,null),$(e,t,`target`,i.target,i,null)));if(r==null||typeof r==`symbol`||typeof r==`boolean`){e.removeAttribute(n);break}r=on(``+r),e.setAttribute(n,r);break;case`onClick`:r!=null&&(e.onclick=sn);break;case`onScroll`:r!=null&&Q(`scroll`,e);break;case`onScrollEnd`:r!=null&&Q(`scrollend`,e);break;case`dangerouslySetInnerHTML`:if(r!=null){if(typeof r!=`object`||!(`__html`in r))throw Error(a(61));if(n=r.__html,n!=null){if(i.children!=null)throw Error(a(60));e.innerHTML=n}}break;case`multiple`:e.multiple=r&&typeof r!=`function`&&typeof r!=`symbol`;break;case`muted`:e.muted=r&&typeof r!=`function`&&typeof r!=`symbol`;break;case`suppressContentEditableWarning`:case`suppressHydrationWarning`:case`defaultValue`:case`defaultChecked`:case`innerHTML`:case`ref`:break;case`autoFocus`:break;case`xlinkHref`:if(r==null||typeof r==`function`||typeof r==`boolean`||typeof r==`symbol`){e.removeAttribute(`xlink:href`);break}n=on(``+r),e.setAttributeNS(`http://www.w3.org/1999/xlink`,`xlink:href`,n);break;case`contentEditable`:case`spellCheck`:case`draggable`:case`value`:case`autoReverse`:case`externalResourcesRequired`:case`focusable`:case`preserveAlpha`:r!=null&&typeof r!=`function`&&typeof r!=`symbol`?e.setAttribute(n,``+r):e.removeAttribute(n);break;case`inert`:case`allowFullScreen`:case`async`:case`autoPlay`:case`controls`:case`default`:case`defer`:case`disabled`:case`disablePictureInPicture`:case`disableRemotePlayback`:case`formNoValidate`:case`hidden`:case`loop`:case`noModule`:case`noValidate`:case`open`:case`playsInline`:case`readOnly`:case`required`:case`reversed`:case`scoped`:case`seamless`:case`itemScope`:r&&typeof r!=`function`&&typeof r!=`symbol`?e.setAttribute(n,``):e.removeAttribute(n);break;case`capture`:case`download`:!0===r?e.setAttribute(n,``):!1!==r&&r!=null&&typeof r!=`function`&&typeof r!=`symbol`?e.setAttribute(n,r):e.removeAttribute(n);break;case`cols`:case`rows`:case`size`:case`span`:r!=null&&typeof r!=`function`&&typeof r!=`symbol`&&!isNaN(r)&&1<=r?e.setAttribute(n,r):e.removeAttribute(n);break;case`rowSpan`:case`start`:r==null||typeof r==`function`||typeof r==`symbol`||isNaN(r)?e.removeAttribute(n):e.setAttribute(n,r);break;case`popover`:Q(`beforetoggle`,e),Q(`toggle`,e),Ft(e,`popover`,r);break;case`xlinkActuate`:Lt(e,`http://www.w3.org/1999/xlink`,`xlink:actuate`,r);break;case`xlinkArcrole`:Lt(e,`http://www.w3.org/1999/xlink`,`xlink:arcrole`,r);break;case`xlinkRole`:Lt(e,`http://www.w3.org/1999/xlink`,`xlink:role`,r);break;case`xlinkShow`:Lt(e,`http://www.w3.org/1999/xlink`,`xlink:show`,r);break;case`xlinkTitle`:Lt(e,`http://www.w3.org/1999/xlink`,`xlink:title`,r);break;case`xlinkType`:Lt(e,`http://www.w3.org/1999/xlink`,`xlink:type`,r);break;case`xmlBase`:Lt(e,`http://www.w3.org/XML/1998/namespace`,`xml:base`,r);break;case`xmlLang`:Lt(e,`http://www.w3.org/XML/1998/namespace`,`xml:lang`,r);break;case`xmlSpace`:Lt(e,`http://www.w3.org/XML/1998/namespace`,`xml:space`,r);break;case`is`:Ft(e,`is`,r);break;case`innerText`:case`textContent`:break;default:(!(2<n.length)||n[0]!==`o`&&n[0]!==`O`||n[1]!==`n`&&n[1]!==`N`)&&(n=rn.get(n)||n,Ft(e,n,r))}}function Nd(e,t,n,r,i,o){switch(n){case`style`:tn(e,r,o);break;case`dangerouslySetInnerHTML`:if(r!=null){if(typeof r!=`object`||!(`__html`in r))throw Error(a(61));if(n=r.__html,n!=null){if(i.children!=null)throw Error(a(60));e.innerHTML=n}}break;case`children`:typeof r==`string`?Qt(e,r):(typeof r==`number`||typeof r==`bigint`)&&Qt(e,``+r);break;case`onScroll`:r!=null&&Q(`scroll`,e);break;case`onScrollEnd`:r!=null&&Q(`scrollend`,e);break;case`onClick`:r!=null&&(e.onclick=sn);break;case`suppressContentEditableWarning`:case`suppressHydrationWarning`:case`innerHTML`:case`ref`:break;case`innerText`:case`textContent`:break;default:if(!Ot.hasOwnProperty(n))a:{if(n[0]===`o`&&n[1]===`n`&&(i=n.endsWith(`Capture`),t=n.slice(2,i?n.length-7:void 0),o=e[mt]||null,o=o==null?null:o[n],typeof o==`function`&&e.removeEventListener(t,o,i),typeof r==`function`)){typeof o!=`function`&&o!==null&&(n in e?e[n]=null:e.hasAttribute(n)&&e.removeAttribute(n)),e.addEventListener(t,r,i);break a}n in e?e[n]=r:!0===r?e.setAttribute(n,``):Ft(e,n,r)}}}function Pd(e,t,n){switch(t){case`div`:case`span`:case`svg`:case`path`:case`a`:case`g`:case`p`:case`li`:break;case`img`:Q(`error`,e),Q(`load`,e);var r=!1,i=!1,o;for(o in n)if(n.hasOwnProperty(o)){var s=n[o];if(s!=null)switch(o){case`src`:r=!0;break;case`srcSet`:i=!0;break;case`children`:case`dangerouslySetInnerHTML`:throw Error(a(137,t));default:$(e,t,o,s,n,null)}}i&&$(e,t,`srcSet`,n.srcSet,n,null),r&&$(e,t,`src`,n.src,n,null);return;case`input`:Q(`invalid`,e);var c=o=s=i=null,l=null,u=null;for(r in n)if(n.hasOwnProperty(r)){var d=n[r];if(d!=null)switch(r){case`name`:i=d;break;case`type`:s=d;break;case`checked`:l=d;break;case`defaultChecked`:u=d;break;case`value`:o=d;break;case`defaultValue`:c=d;break;case`children`:case`dangerouslySetInnerHTML`:if(d!=null)throw Error(a(137,t));break;default:$(e,t,r,d,n,null)}}qt(e,o,c,l,u,s,i,!1);return;case`select`:for(i in Q(`invalid`,e),r=s=o=null,n)if(n.hasOwnProperty(i)&&(c=n[i],c!=null))switch(i){case`value`:o=c;break;case`defaultValue`:s=c;break;case`multiple`:r=c;default:$(e,t,i,c,n,null)}t=o,n=s,e.multiple=!!r,t==null?n!=null&&Yt(e,!!r,n,!0):Yt(e,!!r,t,!1);return;case`textarea`:for(s in Q(`invalid`,e),o=i=r=null,n)if(n.hasOwnProperty(s)&&(c=n[s],c!=null))switch(s){case`value`:r=c;break;case`defaultValue`:i=c;break;case`children`:o=c;break;case`dangerouslySetInnerHTML`:if(c!=null)throw Error(a(91));break;default:$(e,t,s,c,n,null)}Zt(e,r,i,o);return;case`option`:for(l in n)if(n.hasOwnProperty(l)&&(r=n[l],r!=null))switch(l){case`selected`:e.selected=r&&typeof r!=`function`&&typeof r!=`symbol`;break;default:$(e,t,l,r,n,null)}return;case`dialog`:Q(`beforetoggle`,e),Q(`toggle`,e),Q(`cancel`,e),Q(`close`,e);break;case`iframe`:case`object`:Q(`load`,e);break;case`video`:case`audio`:for(r=0;r<_d.length;r++)Q(_d[r],e);break;case`image`:Q(`error`,e),Q(`load`,e);break;case`details`:Q(`toggle`,e);break;case`embed`:case`source`:case`link`:Q(`error`,e),Q(`load`,e);case`area`:case`base`:case`br`:case`col`:case`hr`:case`keygen`:case`meta`:case`param`:case`track`:case`wbr`:case`menuitem`:for(u in n)if(n.hasOwnProperty(u)&&(r=n[u],r!=null))switch(u){case`children`:case`dangerouslySetInnerHTML`:throw Error(a(137,t));default:$(e,t,u,r,n,null)}return;default:if(nn(t)){for(d in n)n.hasOwnProperty(d)&&(r=n[d],r!==void 0&&Nd(e,t,d,r,n,void 0));return}}for(c in n)n.hasOwnProperty(c)&&(r=n[c],r!=null&&$(e,t,c,r,n,null))}function Fd(e,t,n,r){switch(t){case`div`:case`span`:case`svg`:case`path`:case`a`:case`g`:case`p`:case`li`:break;case`input`:var i=null,o=null,s=null,c=null,l=null,u=null,d=null;for(m in n){var f=n[m];if(n.hasOwnProperty(m)&&f!=null)switch(m){case`checked`:break;case`value`:break;case`defaultValue`:l=f;default:r.hasOwnProperty(m)||$(e,t,m,null,r,f)}}for(var p in r){var m=r[p];if(f=n[p],r.hasOwnProperty(p)&&(m!=null||f!=null))switch(p){case`type`:o=m;break;case`name`:i=m;break;case`checked`:u=m;break;case`defaultChecked`:d=m;break;case`value`:s=m;break;case`defaultValue`:c=m;break;case`children`:case`dangerouslySetInnerHTML`:if(m!=null)throw Error(a(137,t));break;default:m!==f&&$(e,t,p,m,r,f)}}Kt(e,s,c,l,u,d,o,i);return;case`select`:for(o in m=s=c=p=null,n)if(l=n[o],n.hasOwnProperty(o)&&l!=null)switch(o){case`value`:break;case`multiple`:m=l;default:r.hasOwnProperty(o)||$(e,t,o,null,r,l)}for(i in r)if(o=r[i],l=n[i],r.hasOwnProperty(i)&&(o!=null||l!=null))switch(i){case`value`:p=o;break;case`defaultValue`:c=o;break;case`multiple`:s=o;default:o!==l&&$(e,t,i,o,r,l)}t=c,n=s,r=m,p==null?!!r!=!!n&&(t==null?Yt(e,!!n,n?[]:``,!1):Yt(e,!!n,t,!0)):Yt(e,!!n,p,!1);return;case`textarea`:for(c in m=p=null,n)if(i=n[c],n.hasOwnProperty(c)&&i!=null&&!r.hasOwnProperty(c))switch(c){case`value`:break;case`children`:break;default:$(e,t,c,null,r,i)}for(s in r)if(i=r[s],o=n[s],r.hasOwnProperty(s)&&(i!=null||o!=null))switch(s){case`value`:p=i;break;case`defaultValue`:m=i;break;case`children`:break;case`dangerouslySetInnerHTML`:if(i!=null)throw Error(a(91));break;default:i!==o&&$(e,t,s,i,r,o)}Xt(e,p,m);return;case`option`:for(var h in n)if(p=n[h],n.hasOwnProperty(h)&&p!=null&&!r.hasOwnProperty(h))switch(h){case`selected`:e.selected=!1;break;default:$(e,t,h,null,r,p)}for(l in r)if(p=r[l],m=n[l],r.hasOwnProperty(l)&&p!==m&&(p!=null||m!=null))switch(l){case`selected`:e.selected=p&&typeof p!=`function`&&typeof p!=`symbol`;break;default:$(e,t,l,p,r,m)}return;case`img`:case`link`:case`area`:case`base`:case`br`:case`col`:case`embed`:case`hr`:case`keygen`:case`meta`:case`param`:case`source`:case`track`:case`wbr`:case`menuitem`:for(var g in n)p=n[g],n.hasOwnProperty(g)&&p!=null&&!r.hasOwnProperty(g)&&$(e,t,g,null,r,p);for(u in r)if(p=r[u],m=n[u],r.hasOwnProperty(u)&&p!==m&&(p!=null||m!=null))switch(u){case`children`:case`dangerouslySetInnerHTML`:if(p!=null)throw Error(a(137,t));break;default:$(e,t,u,p,r,m)}return;default:if(nn(t)){for(var _ in n)p=n[_],n.hasOwnProperty(_)&&p!==void 0&&!r.hasOwnProperty(_)&&Nd(e,t,_,void 0,r,p);for(d in r)p=r[d],m=n[d],!r.hasOwnProperty(d)||p===m||p===void 0&&m===void 0||Nd(e,t,d,p,r,m);return}}for(var v in n)p=n[v],n.hasOwnProperty(v)&&p!=null&&!r.hasOwnProperty(v)&&$(e,t,v,null,r,p);for(f in r)p=r[f],m=n[f],!r.hasOwnProperty(f)||p===m||p==null&&m==null||$(e,t,f,p,r,m)}function Id(e){switch(e){case`css`:case`script`:case`font`:case`img`:case`image`:case`input`:case`link`:return!0;default:return!1}}function Ld(){if(typeof performance.getEntriesByType==`function`){for(var e=0,t=0,n=performance.getEntriesByType(`resource`),r=0;r<n.length;r++){var i=n[r],a=i.transferSize,o=i.initiatorType,s=i.duration;if(a&&s&&Id(o)){for(o=0,s=i.responseEnd,r+=1;r<n.length;r++){var c=n[r],l=c.startTime;if(l>s)break;var u=c.transferSize,d=c.initiatorType;u&&Id(d)&&(c=c.responseEnd,o+=u*(c<s?1:(s-l)/(c-l)))}if(--r,t+=8*(a+o)/(i.duration/1e3),e++,10<e)break}}if(0<e)return t/e/1e6}return navigator.connection&&(e=navigator.connection.downlink,typeof e==`number`)?e:5}var Rd=null,zd=null;function Bd(e){return e.nodeType===9?e:e.ownerDocument}function Vd(e){switch(e){case`http://www.w3.org/2000/svg`:return 1;case`http://www.w3.org/1998/Math/MathML`:return 2;default:return 0}}function Hd(e,t){if(e===0)switch(t){case`svg`:return 1;case`math`:return 2;default:return 0}return e===1&&t===`foreignObject`?0:e}function Ud(e,t){return e===`textarea`||e===`noscript`||typeof t.children==`string`||typeof t.children==`number`||typeof t.children==`bigint`||typeof t.dangerouslySetInnerHTML==`object`&&t.dangerouslySetInnerHTML!==null&&t.dangerouslySetInnerHTML.__html!=null}var Wd=null;function Gd(){var e=window.event;return e&&e.type===`popstate`?e===Wd?!1:(Wd=e,!0):(Wd=null,!1)}var Kd=typeof setTimeout==`function`?setTimeout:void 0,qd=typeof clearTimeout==`function`?clearTimeout:void 0,Jd=typeof Promise==`function`?Promise:void 0,Yd=typeof queueMicrotask==`function`?queueMicrotask:Jd===void 0?Kd:function(e){return Jd.resolve(null).then(e).catch(Xd)};function Xd(e){setTimeout(function(){throw e})}function Zd(e){return e===`head`}function Qd(e,t){var n=t,r=0;do{var i=n.nextSibling;if(e.removeChild(n),i&&i.nodeType===8)if(n=i.data,n===`/$`||n===`/&`){if(r===0){e.removeChild(i),Np(t);return}r--}else if(n===`$`||n===`$?`||n===`$~`||n===`$!`||n===`&`)r++;else if(n===`html`)pf(e.ownerDocument.documentElement);else if(n===`head`){n=e.ownerDocument.head,pf(n);for(var a=n.firstChild;a;){var o=a.nextSibling,s=a.nodeName;a[bt]||s===`SCRIPT`||s===`STYLE`||s===`LINK`&&a.rel.toLowerCase()===`stylesheet`||n.removeChild(a),a=o}}else n===`body`&&pf(e.ownerDocument.body);n=i}while(n);Np(t)}function $d(e,t){var n=e;e=0;do{var r=n.nextSibling;if(n.nodeType===1?t?(n._stashedDisplay=n.style.display,n.style.display=`none`):(n.style.display=n._stashedDisplay||``,n.getAttribute(`style`)===``&&n.removeAttribute(`style`)):n.nodeType===3&&(t?(n._stashedText=n.nodeValue,n.nodeValue=``):n.nodeValue=n._stashedText||``),r&&r.nodeType===8)if(n=r.data,n===`/$`){if(e===0)break;e--}else n!==`$`&&n!==`$?`&&n!==`$~`&&n!==`$!`||e++;n=r}while(n)}function ef(e){var t=e.firstChild;for(t&&t.nodeType===10&&(t=t.nextSibling);t;){var n=t;switch(t=t.nextSibling,n.nodeName){case`HTML`:case`HEAD`:case`BODY`:ef(n),xt(n);continue;case`SCRIPT`:case`STYLE`:continue;case`LINK`:if(n.rel.toLowerCase()===`stylesheet`)continue}e.removeChild(n)}}function tf(e,t,n,r){for(;e.nodeType===1;){var i=n;if(e.nodeName.toLowerCase()!==t.toLowerCase()){if(!r&&(e.nodeName!==`INPUT`||e.type!==`hidden`))break}else if(!r)if(t===`input`&&e.type===`hidden`){var a=i.name==null?null:``+i.name;if(i.type===`hidden`&&e.getAttribute(`name`)===a)return e}else return e;else if(!e[bt])switch(t){case`meta`:if(!e.hasAttribute(`itemprop`))break;return e;case`link`:if(a=e.getAttribute(`rel`),a===`stylesheet`&&e.hasAttribute(`data-precedence`)||a!==i.rel||e.getAttribute(`href`)!==(i.href==null||i.href===``?null:i.href)||e.getAttribute(`crossorigin`)!==(i.crossOrigin==null?null:i.crossOrigin)||e.getAttribute(`title`)!==(i.title==null?null:i.title))break;return e;case`style`:if(e.hasAttribute(`data-precedence`))break;return e;case`script`:if(a=e.getAttribute(`src`),(a!==(i.src==null?null:i.src)||e.getAttribute(`type`)!==(i.type==null?null:i.type)||e.getAttribute(`crossorigin`)!==(i.crossOrigin==null?null:i.crossOrigin))&&a&&e.hasAttribute(`async`)&&!e.hasAttribute(`itemprop`))break;return e;default:return e}if(e=cf(e.nextSibling),e===null)break}return null}function nf(e,t,n){if(t===``)return null;for(;e.nodeType!==3;)if((e.nodeType!==1||e.nodeName!==`INPUT`||e.type!==`hidden`)&&!n||(e=cf(e.nextSibling),e===null))return null;return e}function rf(e,t){for(;e.nodeType!==8;)if((e.nodeType!==1||e.nodeName!==`INPUT`||e.type!==`hidden`)&&!t||(e=cf(e.nextSibling),e===null))return null;return e}function af(e){return e.data===`$?`||e.data===`$~`}function of(e){return e.data===`$!`||e.data===`$?`&&e.ownerDocument.readyState!==`loading`}function sf(e,t){var n=e.ownerDocument;if(e.data===`$~`)e._reactRetry=t;else if(e.data!==`$?`||n.readyState!==`loading`)t();else{var r=function(){t(),n.removeEventListener(`DOMContentLoaded`,r)};n.addEventListener(`DOMContentLoaded`,r),e._reactRetry=r}}function cf(e){for(;e!=null;e=e.nextSibling){var t=e.nodeType;if(t===1||t===3)break;if(t===8){if(t=e.data,t===`$`||t===`$!`||t===`$?`||t===`$~`||t===`&`||t===`F!`||t===`F`)break;if(t===`/$`||t===`/&`)return null}}return e}var lf=null;function uf(e){e=e.nextSibling;for(var t=0;e;){if(e.nodeType===8){var n=e.data;if(n===`/$`||n===`/&`){if(t===0)return cf(e.nextSibling);t--}else n!==`$`&&n!==`$!`&&n!==`$?`&&n!==`$~`&&n!==`&`||t++}e=e.nextSibling}return null}function df(e){e=e.previousSibling;for(var t=0;e;){if(e.nodeType===8){var n=e.data;if(n===`$`||n===`$!`||n===`$?`||n===`$~`||n===`&`){if(t===0)return e;t--}else n!==`/$`&&n!==`/&`||t++}e=e.previousSibling}return null}function ff(e,t,n){switch(t=Bd(n),e){case`html`:if(e=t.documentElement,!e)throw Error(a(452));return e;case`head`:if(e=t.head,!e)throw Error(a(453));return e;case`body`:if(e=t.body,!e)throw Error(a(454));return e;default:throw Error(a(451))}}function pf(e){for(var t=e.attributes;t.length;)e.removeAttributeNode(t[0]);xt(e)}var mf=new Map,hf=new Set;function gf(e){return typeof e.getRootNode==`function`?e.getRootNode():e.nodeType===9?e:e.ownerDocument}var _f=O.d;O.d={f:vf,r:yf,D:Sf,C:Cf,L:wf,m:Tf,X:Df,S:Ef,M:Of};function vf(){var e=_f.f(),t=bu();return e||t}function yf(e){var t=Ct(e);t!==null&&t.tag===5&&t.type===`form`?Ds(t):_f.r(e)}var bf=typeof document>`u`?null:document;function xf(e,t,n){var r=bf;if(r&&typeof t==`string`&&t){var i=Gt(t);i=`link[rel="`+e+`"][href="`+i+`"]`,typeof n==`string`&&(i+=`[crossorigin="`+n+`"]`),hf.has(i)||(hf.add(i),e={rel:e,crossOrigin:n,href:t},r.querySelector(i)===null&&(t=r.createElement(`link`),Pd(t,`link`,e),Et(t),r.head.appendChild(t)))}}function Sf(e){_f.D(e),xf(`dns-prefetch`,e,null)}function Cf(e,t){_f.C(e,t),xf(`preconnect`,e,t)}function wf(e,t,n){_f.L(e,t,n);var r=bf;if(r&&e&&t){var i=`link[rel="preload"][as="`+Gt(t)+`"]`;t===`image`&&n&&n.imageSrcSet?(i+=`[imagesrcset="`+Gt(n.imageSrcSet)+`"]`,typeof n.imageSizes==`string`&&(i+=`[imagesizes="`+Gt(n.imageSizes)+`"]`)):i+=`[href="`+Gt(e)+`"]`;var a=i;switch(t){case`style`:a=Af(e);break;case`script`:a=Pf(e)}mf.has(a)||(e=m({rel:`preload`,href:t===`image`&&n&&n.imageSrcSet?void 0:e,as:t},n),mf.set(a,e),r.querySelector(i)!==null||t===`style`&&r.querySelector(jf(a))||t===`script`&&r.querySelector(Ff(a))||(t=r.createElement(`link`),Pd(t,`link`,e),Et(t),r.head.appendChild(t)))}}function Tf(e,t){_f.m(e,t);var n=bf;if(n&&e){var r=t&&typeof t.as==`string`?t.as:`script`,i=`link[rel="modulepreload"][as="`+Gt(r)+`"][href="`+Gt(e)+`"]`,a=i;switch(r){case`audioworklet`:case`paintworklet`:case`serviceworker`:case`sharedworker`:case`worker`:case`script`:a=Pf(e)}if(!mf.has(a)&&(e=m({rel:`modulepreload`,href:e},t),mf.set(a,e),n.querySelector(i)===null)){switch(r){case`audioworklet`:case`paintworklet`:case`serviceworker`:case`sharedworker`:case`worker`:case`script`:if(n.querySelector(Ff(a)))return}r=n.createElement(`link`),Pd(r,`link`,e),Et(r),n.head.appendChild(r)}}}function Ef(e,t,n){_f.S(e,t,n);var r=bf;if(r&&e){var i=Tt(r).hoistableStyles,a=Af(e);t||=`default`;var o=i.get(a);if(!o){var s={loading:0,preload:null};if(o=r.querySelector(jf(a)))s.loading=5;else{e=m({rel:`stylesheet`,href:e,"data-precedence":t},n),(n=mf.get(a))&&Rf(e,n);var c=o=r.createElement(`link`);Et(c),Pd(c,`link`,e),c._p=new Promise(function(e,t){c.onload=e,c.onerror=t}),c.addEventListener(`load`,function(){s.loading|=1}),c.addEventListener(`error`,function(){s.loading|=2}),s.loading|=4,Lf(o,t,r)}o={type:`stylesheet`,instance:o,count:1,state:s},i.set(a,o)}}}function Df(e,t){_f.X(e,t);var n=bf;if(n&&e){var r=Tt(n).hoistableScripts,i=Pf(e),a=r.get(i);a||(a=n.querySelector(Ff(i)),a||(e=m({src:e,async:!0},t),(t=mf.get(i))&&zf(e,t),a=n.createElement(`script`),Et(a),Pd(a,`link`,e),n.head.appendChild(a)),a={type:`script`,instance:a,count:1,state:null},r.set(i,a))}}function Of(e,t){_f.M(e,t);var n=bf;if(n&&e){var r=Tt(n).hoistableScripts,i=Pf(e),a=r.get(i);a||(a=n.querySelector(Ff(i)),a||(e=m({src:e,async:!0,type:`module`},t),(t=mf.get(i))&&zf(e,t),a=n.createElement(`script`),Et(a),Pd(a,`link`,e),n.head.appendChild(a)),a={type:`script`,instance:a,count:1,state:null},r.set(i,a))}}function kf(e,t,n,r){var i=(i=me.current)?gf(i):null;if(!i)throw Error(a(446));switch(e){case`meta`:case`title`:return null;case`style`:return typeof n.precedence==`string`&&typeof n.href==`string`?(t=Af(n.href),n=Tt(i).hoistableStyles,r=n.get(t),r||(r={type:`style`,instance:null,count:0,state:null},n.set(t,r)),r):{type:`void`,instance:null,count:0,state:null};case`link`:if(n.rel===`stylesheet`&&typeof n.href==`string`&&typeof n.precedence==`string`){e=Af(n.href);var o=Tt(i).hoistableStyles,s=o.get(e);if(s||(i=i.ownerDocument||i,s={type:`stylesheet`,instance:null,count:0,state:{loading:0,preload:null}},o.set(e,s),(o=i.querySelector(jf(e)))&&!o._p&&(s.instance=o,s.state.loading=5),mf.has(e)||(n={rel:`preload`,as:`style`,href:n.href,crossOrigin:n.crossOrigin,integrity:n.integrity,media:n.media,hrefLang:n.hrefLang,referrerPolicy:n.referrerPolicy},mf.set(e,n),o||Nf(i,e,n,s.state))),t&&r===null)throw Error(a(528,``));return s}if(t&&r!==null)throw Error(a(529,``));return null;case`script`:return t=n.async,n=n.src,typeof n==`string`&&t&&typeof t!=`function`&&typeof t!=`symbol`?(t=Pf(n),n=Tt(i).hoistableScripts,r=n.get(t),r||(r={type:`script`,instance:null,count:0,state:null},n.set(t,r)),r):{type:`void`,instance:null,count:0,state:null};default:throw Error(a(444,e))}}function Af(e){return`href="`+Gt(e)+`"`}function jf(e){return`link[rel="stylesheet"][`+e+`]`}function Mf(e){return m({},e,{"data-precedence":e.precedence,precedence:null})}function Nf(e,t,n,r){e.querySelector(`link[rel="preload"][as="style"][`+t+`]`)?r.loading=1:(t=e.createElement(`link`),r.preload=t,t.addEventListener(`load`,function(){return r.loading|=1}),t.addEventListener(`error`,function(){return r.loading|=2}),Pd(t,`link`,n),Et(t),e.head.appendChild(t))}function Pf(e){return`[src="`+Gt(e)+`"]`}function Ff(e){return`script[async]`+e}function If(e,t,n){if(t.count++,t.instance===null)switch(t.type){case`style`:var r=e.querySelector(`style[data-href~="`+Gt(n.href)+`"]`);if(r)return t.instance=r,Et(r),r;var i=m({},n,{"data-href":n.href,"data-precedence":n.precedence,href:null,precedence:null});return r=(e.ownerDocument||e).createElement(`style`),Et(r),Pd(r,`style`,i),Lf(r,n.precedence,e),t.instance=r;case`stylesheet`:i=Af(n.href);var o=e.querySelector(jf(i));if(o)return t.state.loading|=4,t.instance=o,Et(o),o;r=Mf(n),(i=mf.get(i))&&Rf(r,i),o=(e.ownerDocument||e).createElement(`link`),Et(o);var s=o;return s._p=new Promise(function(e,t){s.onload=e,s.onerror=t}),Pd(o,`link`,r),t.state.loading|=4,Lf(o,n.precedence,e),t.instance=o;case`script`:return o=Pf(n.src),(i=e.querySelector(Ff(o)))?(t.instance=i,Et(i),i):(r=n,(i=mf.get(o))&&(r=m({},n),zf(r,i)),e=e.ownerDocument||e,i=e.createElement(`script`),Et(i),Pd(i,`link`,r),e.head.appendChild(i),t.instance=i);case`void`:return null;default:throw Error(a(443,t.type))}else t.type===`stylesheet`&&!(t.state.loading&4)&&(r=t.instance,t.state.loading|=4,Lf(r,n.precedence,e));return t.instance}function Lf(e,t,n){for(var r=n.querySelectorAll(`link[rel="stylesheet"][data-precedence],style[data-precedence]`),i=r.length?r[r.length-1]:null,a=i,o=0;o<r.length;o++){var s=r[o];if(s.dataset.precedence===t)a=s;else if(a!==i)break}a?a.parentNode.insertBefore(e,a.nextSibling):(t=n.nodeType===9?n.head:n,t.insertBefore(e,t.firstChild))}function Rf(e,t){e.crossOrigin??=t.crossOrigin,e.referrerPolicy??=t.referrerPolicy,e.title??=t.title}function zf(e,t){e.crossOrigin??=t.crossOrigin,e.referrerPolicy??=t.referrerPolicy,e.integrity??=t.integrity}var Bf=null;function Vf(e,t,n){if(Bf===null){var r=new Map,i=Bf=new Map;i.set(n,r)}else i=Bf,r=i.get(n),r||(r=new Map,i.set(n,r));if(r.has(e))return r;for(r.set(e,null),n=n.getElementsByTagName(e),i=0;i<n.length;i++){var a=n[i];if(!(a[bt]||a[pt]||e===`link`&&a.getAttribute(`rel`)===`stylesheet`)&&a.namespaceURI!==`http://www.w3.org/2000/svg`){var o=a.getAttribute(t)||``;o=e+o;var s=r.get(o);s?s.push(a):r.set(o,[a])}}return r}function Hf(e,t,n){e=e.ownerDocument||e,e.head.insertBefore(n,t===`title`?e.querySelector(`head > title`):null)}function Uf(e,t,n){if(n===1||t.itemProp!=null)return!1;switch(e){case`meta`:case`title`:return!0;case`style`:if(typeof t.precedence!=`string`||typeof t.href!=`string`||t.href===``)break;return!0;case`link`:if(typeof t.rel!=`string`||typeof t.href!=`string`||t.href===``||t.onLoad||t.onError)break;switch(t.rel){case`stylesheet`:return e=t.disabled,typeof t.precedence==`string`&&e==null;default:return!0}case`script`:if(t.async&&typeof t.async!=`function`&&typeof t.async!=`symbol`&&!t.onLoad&&!t.onError&&t.src&&typeof t.src==`string`)return!0}return!1}function Wf(e){return!(e.type===`stylesheet`&&!(e.state.loading&3))}function Gf(e,t,n,r){if(n.type===`stylesheet`&&(typeof r.media!=`string`||!1!==matchMedia(r.media).matches)&&!(n.state.loading&4)){if(n.instance===null){var i=Af(r.href),a=t.querySelector(jf(i));if(a){t=a._p,typeof t==`object`&&t&&typeof t.then==`function`&&(e.count++,e=Jf.bind(e),t.then(e,e)),n.state.loading|=4,n.instance=a,Et(a);return}a=t.ownerDocument||t,r=Mf(r),(i=mf.get(i))&&Rf(r,i),a=a.createElement(`link`),Et(a);var o=a;o._p=new Promise(function(e,t){o.onload=e,o.onerror=t}),Pd(a,`link`,r),n.instance=a}e.stylesheets===null&&(e.stylesheets=new Map),e.stylesheets.set(n,t),(t=n.state.preload)&&!(n.state.loading&3)&&(e.count++,n=Jf.bind(e),t.addEventListener(`load`,n),t.addEventListener(`error`,n))}}var Kf=0;function qf(e,t){return e.stylesheets&&e.count===0&&Xf(e,e.stylesheets),0<e.count||0<e.imgCount?function(n){var r=setTimeout(function(){if(e.stylesheets&&Xf(e,e.stylesheets),e.unsuspend){var t=e.unsuspend;e.unsuspend=null,t()}},6e4+t);0<e.imgBytes&&Kf===0&&(Kf=62500*Ld());var i=setTimeout(function(){if(e.waitingForImages=!1,e.count===0&&(e.stylesheets&&Xf(e,e.stylesheets),e.unsuspend)){var t=e.unsuspend;e.unsuspend=null,t()}},(e.imgBytes>Kf?50:800)+t);return e.unsuspend=n,function(){e.unsuspend=null,clearTimeout(r),clearTimeout(i)}}:null}function Jf(){if(this.count--,this.count===0&&(this.imgCount===0||!this.waitingForImages)){if(this.stylesheets)Xf(this,this.stylesheets);else if(this.unsuspend){var e=this.unsuspend;this.unsuspend=null,e()}}}var Yf=null;function Xf(e,t){e.stylesheets=null,e.unsuspend!==null&&(e.count++,Yf=new Map,t.forEach(Zf,e),Yf=null,Jf.call(e))}function Zf(e,t){if(!(t.state.loading&4)){var n=Yf.get(e);if(n)var r=n.get(null);else{n=new Map,Yf.set(e,n);for(var i=e.querySelectorAll(`link[data-precedence],style[data-precedence]`),a=0;a<i.length;a++){var o=i[a];(o.nodeName===`LINK`||o.getAttribute(`media`)!==`not all`)&&(n.set(o.dataset.precedence,o),r=o)}r&&n.set(null,r)}i=t.instance,o=i.getAttribute(`data-precedence`),a=n.get(o)||r,a===r&&n.set(null,i),n.set(o,i),this.count++,r=Jf.bind(this),i.addEventListener(`load`,r),i.addEventListener(`error`,r),a?a.parentNode.insertBefore(i,a.nextSibling):(e=e.nodeType===9?e.head:e,e.insertBefore(i,e.firstChild)),t.state.loading|=4}}var Qf={$$typeof:S,Provider:null,Consumer:null,_currentValue:le,_currentValue2:le,_threadCount:0};function $f(e,t,n,r,i,a,o,s,c){this.tag=1,this.containerInfo=e,this.pingCache=this.current=this.pendingChildren=null,this.timeoutHandle=-1,this.callbackNode=this.next=this.pendingContext=this.context=this.cancelPendingCommit=null,this.callbackPriority=0,this.expirationTimes=nt(-1),this.entangledLanes=this.shellSuspendCounter=this.errorRecoveryDisabledLanes=this.expiredLanes=this.warmLanes=this.pingedLanes=this.suspendedLanes=this.pendingLanes=0,this.entanglements=nt(0),this.hiddenUpdates=nt(null),this.identifierPrefix=r,this.onUncaughtError=i,this.onCaughtError=a,this.onRecoverableError=o,this.pooledCache=null,this.pooledCacheLanes=0,this.formState=c,this.incompleteTransitions=new Map}function ep(e,t,n,r,i,a,o,s,c,l,u,d){return e=new $f(e,t,n,o,c,l,u,d,s),t=1,!0===a&&(t|=24),a=mi(3,null,null,t),e.current=a,a.stateNode=e,t=da(),t.refCount++,e.pooledCache=t,t.refCount++,a.memoizedState={element:r,isDehydrated:n,cache:t},Wa(a),e}function tp(e){return e?(e=fi,e):fi}function np(e,t,n,r,i,a){i=tp(i),r.context===null?r.context=i:r.pendingContext=i,r=Ka(t),r.payload={element:n},a=a===void 0?null:a,a!==null&&(r.callback=a),n=qa(e,r,t),n!==null&&(hu(n,e,t),Ja(n,e,t))}function rp(e,t){if(e=e.memoizedState,e!==null&&e.dehydrated!==null){var n=e.retryLane;e.retryLane=n!==0&&n<t?n:t}}function ip(e,t){rp(e,t),(e=e.alternate)&&rp(e,t)}function ap(e){if(e.tag===13||e.tag===31){var t=li(e,67108864);t!==null&&hu(t,e,67108864),ip(e,67108864)}}function op(e){if(e.tag===13||e.tag===31){var t=pu();t=ct(t);var n=li(e,t);n!==null&&hu(n,e,t),ip(e,t)}}var sp=!0;function cp(e,t,n,r){var i=D.T;D.T=null;var a=O.p;try{O.p=2,up(e,t,n,r)}finally{O.p=a,D.T=i}}function lp(e,t,n,r){var i=D.T;D.T=null;var a=O.p;try{O.p=8,up(e,t,n,r)}finally{O.p=a,D.T=i}}function up(e,t,n,r){if(sp){var i=dp(r);if(i===null)wd(e,t,r,fp,n),Cp(e,r);else if(Tp(i,e,t,n,r))r.stopPropagation();else if(Cp(e,r),t&4&&-1<Sp.indexOf(e)){for(;i!==null;){var a=Ct(i);if(a!==null)switch(a.tag){case 3:if(a=a.stateNode,a.current.memoizedState.isDehydrated){var o=Ze(a.pendingLanes);if(o!==0){var s=a;for(s.pendingLanes|=2,s.entangledLanes|=2;o;){var c=1<<31-We(o);s.entanglements[1]|=c,o&=~c}rd(a),!(K&6)&&(tu=Me()+500,id(0,!1))}}break;case 31:case 13:s=li(a,2),s!==null&&hu(s,a,2),bu(),ip(a,2)}if(a=dp(r),a===null&&wd(e,t,r,fp,n),a===i)break;i=a}i!==null&&r.stopPropagation()}else wd(e,t,r,null,n)}}function dp(e){return e=ln(e),pp(e)}var fp=null;function pp(e){if(fp=null,e=St(e),e!==null){var t=s(e);if(t===null)e=null;else{var n=t.tag;if(n===13){if(e=c(t),e!==null)return e;e=null}else if(n===31){if(e=l(t),e!==null)return e;e=null}else if(n===3){if(t.stateNode.current.memoizedState.isDehydrated)return t.tag===3?t.stateNode.containerInfo:null;e=null}else t!==e&&(e=null)}}return fp=e,null}function mp(e){switch(e){case`beforetoggle`:case`cancel`:case`click`:case`close`:case`contextmenu`:case`copy`:case`cut`:case`auxclick`:case`dblclick`:case`dragend`:case`dragstart`:case`drop`:case`focusin`:case`focusout`:case`input`:case`invalid`:case`keydown`:case`keypress`:case`keyup`:case`mousedown`:case`mouseup`:case`paste`:case`pause`:case`play`:case`pointercancel`:case`pointerdown`:case`pointerup`:case`ratechange`:case`reset`:case`resize`:case`seeked`:case`submit`:case`toggle`:case`touchcancel`:case`touchend`:case`touchstart`:case`volumechange`:case`change`:case`selectionchange`:case`textInput`:case`compositionstart`:case`compositionend`:case`compositionupdate`:case`beforeblur`:case`afterblur`:case`beforeinput`:case`blur`:case`fullscreenchange`:case`focus`:case`hashchange`:case`popstate`:case`select`:case`selectstart`:return 2;case`drag`:case`dragenter`:case`dragexit`:case`dragleave`:case`dragover`:case`mousemove`:case`mouseout`:case`mouseover`:case`pointermove`:case`pointerout`:case`pointerover`:case`scroll`:case`touchmove`:case`wheel`:case`mouseenter`:case`mouseleave`:case`pointerenter`:case`pointerleave`:return 8;case`message`:switch(Ne()){case Pe:return 2;case Fe:return 8;case Ie:case Le:return 32;case Re:return 268435456;default:return 32}default:return 32}}var hp=!1,gp=null,_p=null,vp=null,yp=new Map,bp=new Map,xp=[],Sp=`mousedown mouseup touchcancel touchend touchstart auxclick dblclick pointercancel pointerdown pointerup dragend dragstart drop compositionend compositionstart keydown keypress keyup input textInput copy cut paste click change contextmenu reset`.split(` `);function Cp(e,t){switch(e){case`focusin`:case`focusout`:gp=null;break;case`dragenter`:case`dragleave`:_p=null;break;case`mouseover`:case`mouseout`:vp=null;break;case`pointerover`:case`pointerout`:yp.delete(t.pointerId);break;case`gotpointercapture`:case`lostpointercapture`:bp.delete(t.pointerId)}}function wp(e,t,n,r,i,a){return e===null||e.nativeEvent!==a?(e={blockedOn:t,domEventName:n,eventSystemFlags:r,nativeEvent:a,targetContainers:[i]},t!==null&&(t=Ct(t),t!==null&&ap(t)),e):(e.eventSystemFlags|=r,t=e.targetContainers,i!==null&&t.indexOf(i)===-1&&t.push(i),e)}function Tp(e,t,n,r,i){switch(t){case`focusin`:return gp=wp(gp,e,t,n,r,i),!0;case`dragenter`:return _p=wp(_p,e,t,n,r,i),!0;case`mouseover`:return vp=wp(vp,e,t,n,r,i),!0;case`pointerover`:var a=i.pointerId;return yp.set(a,wp(yp.get(a)||null,e,t,n,r,i)),!0;case`gotpointercapture`:return a=i.pointerId,bp.set(a,wp(bp.get(a)||null,e,t,n,r,i)),!0}return!1}function Ep(e){var t=St(e.target);if(t!==null){var n=s(t);if(n!==null){if(t=n.tag,t===13){if(t=c(n),t!==null){e.blockedOn=t,dt(e.priority,function(){op(n)});return}}else if(t===31){if(t=l(n),t!==null){e.blockedOn=t,dt(e.priority,function(){op(n)});return}}else if(t===3&&n.stateNode.current.memoizedState.isDehydrated){e.blockedOn=n.tag===3?n.stateNode.containerInfo:null;return}}}e.blockedOn=null}function Dp(e){if(e.blockedOn!==null)return!1;for(var t=e.targetContainers;0<t.length;){var n=dp(e.nativeEvent);if(n===null){n=e.nativeEvent;var r=new n.constructor(n.type,n);cn=r,n.target.dispatchEvent(r),cn=null}else return t=Ct(n),t!==null&&ap(t),e.blockedOn=n,!1;t.shift()}return!0}function Op(e,t,n){Dp(e)&&n.delete(t)}function kp(){hp=!1,gp!==null&&Dp(gp)&&(gp=null),_p!==null&&Dp(_p)&&(_p=null),vp!==null&&Dp(vp)&&(vp=null),yp.forEach(Op),bp.forEach(Op)}function Ap(e,n){e.blockedOn===n&&(e.blockedOn=null,hp||(hp=!0,t.unstable_scheduleCallback(t.unstable_NormalPriority,kp)))}var jp=null;function Mp(e){jp!==e&&(jp=e,t.unstable_scheduleCallback(t.unstable_NormalPriority,function(){jp===e&&(jp=null);for(var t=0;t<e.length;t+=3){var n=e[t],r=e[t+1],i=e[t+2];if(typeof r!=`function`){if(pp(r||n)===null)continue;break}var a=Ct(n);a!==null&&(e.splice(t,3),t-=3,Ts(a,{pending:!0,data:i,method:n.method,action:r},r,i))}}))}function Np(e){function t(t){return Ap(t,e)}gp!==null&&Ap(gp,e),_p!==null&&Ap(_p,e),vp!==null&&Ap(vp,e),yp.forEach(t),bp.forEach(t);for(var n=0;n<xp.length;n++){var r=xp[n];r.blockedOn===e&&(r.blockedOn=null)}for(;0<xp.length&&(n=xp[0],n.blockedOn===null);)Ep(n),n.blockedOn===null&&xp.shift();if(n=(e.ownerDocument||e).$$reactFormReplay,n!=null)for(r=0;r<n.length;r+=3){var i=n[r],a=n[r+1],o=i[mt]||null;if(typeof a==`function`)o||Mp(n);else if(o){var s=null;if(a&&a.hasAttribute(`formAction`)){if(i=a,o=a[mt]||null)s=o.formAction;else if(pp(i)!==null)continue}else s=o.action;typeof s==`function`?n[r+1]=s:(n.splice(r,3),r-=3),Mp(n)}}}function Pp(){function e(e){e.canIntercept&&e.info===`react-transition`&&e.intercept({handler:function(){return new Promise(function(e){return i=e})},focusReset:`manual`,scroll:`manual`})}function t(){i!==null&&(i(),i=null),r||setTimeout(n,20)}function n(){if(!r&&!navigation.transition){var e=navigation.currentEntry;e&&e.url!=null&&navigation.navigate(e.url,{state:e.getState(),info:`react-transition`,history:`replace`})}}if(typeof navigation==`object`){var r=!1,i=null;return navigation.addEventListener(`navigate`,e),navigation.addEventListener(`navigatesuccess`,t),navigation.addEventListener(`navigateerror`,t),setTimeout(n,100),function(){r=!0,navigation.removeEventListener(`navigate`,e),navigation.removeEventListener(`navigatesuccess`,t),navigation.removeEventListener(`navigateerror`,t),i!==null&&(i(),i=null)}}}function Fp(e){this._internalRoot=e}Ip.prototype.render=Fp.prototype.render=function(e){var t=this._internalRoot;if(t===null)throw Error(a(409));var n=t.current;np(n,pu(),e,t,null,null)},Ip.prototype.unmount=Fp.prototype.unmount=function(){var e=this._internalRoot;if(e!==null){this._internalRoot=null;var t=e.containerInfo;np(e.current,2,null,e,null,null),bu(),t[ht]=null}};function Ip(e){this._internalRoot=e}Ip.prototype.unstable_scheduleHydration=function(e){if(e){var t=ut();e={blockedOn:null,target:e,priority:t};for(var n=0;n<xp.length&&t!==0&&t<xp[n].priority;n++);xp.splice(n,0,e),n===0&&Ep(e)}};var Lp=r.version;if(Lp!==`19.2.8`)throw Error(a(527,Lp,`19.2.8`));O.findDOMNode=function(e){var t=e._reactInternals;if(t===void 0)throw typeof e.render==`function`?Error(a(188)):(e=Object.keys(e).join(`,`),Error(a(268,e)));return e=f(t),e=e===null?null:p(e),e=e===null?null:e.stateNode,e};var Rp={bundleType:0,version:`19.2.8`,rendererPackageName:`react-dom`,currentDispatcherRef:D,reconcilerVersion:`19.2.8`};if(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__<`u`){var zp=__REACT_DEVTOOLS_GLOBAL_HOOK__;if(!zp.isDisabled&&zp.supportsFiber)try{Ve=zp.inject(Rp),He=zp}catch{}}e.createRoot=function(e,t){if(!o(e))throw Error(a(299));var n=!1,r=``,i=Js,s=Ys,c=Xs;return t!=null&&(!0===t.unstable_strictMode&&(n=!0),t.identifierPrefix!==void 0&&(r=t.identifierPrefix),t.onUncaughtError!==void 0&&(i=t.onUncaughtError),t.onCaughtError!==void 0&&(s=t.onCaughtError),t.onRecoverableError!==void 0&&(c=t.onRecoverableError)),t=ep(e,1,!1,null,null,n,r,null,i,s,c,Pp),e[ht]=t.current,Sd(e),new Fp(t)}})),re=t(((e,t)=>{function n(){if(!(typeof __REACT_DEVTOOLS_GLOBAL_HOOK__>`u`||typeof __REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE!=`function`))try{__REACT_DEVTOOLS_GLOBAL_HOOK__.checkDCE(n)}catch(e){console.error(e)}}n(),t.exports=T()})),ie=/^(?:[a-z][a-z0-9+.-]*:|[\\/]{2})/i,E=/^[\\/]{2}/;function ae(e,t){return t+e.replace(/\\/g,`/`)}var oe=`popstate`;function se(e){return typeof e==`object`&&!!e&&`pathname`in e&&`search`in e&&`hash`in e&&`state`in e&&`key`in e}function ce(e={}){function t(e,t){let n=t.state?.masked,{pathname:r,search:i,hash:a}=n||e.location;return de(``,{pathname:r,search:i,hash:a},t.state&&t.state.usr||null,t.state&&t.state.key||`default`,n?{pathname:e.location.pathname,search:e.location.search,hash:e.location.hash}:void 0)}function n(e,t){return typeof t==`string`?t:fe(t)}return A(t,n,null,e)}function D(e,t){if(e===!1||e==null)throw Error(t)}function O(e,t){if(!e){typeof console<`u`&&console.warn(t);try{throw Error(t)}catch{}}}function le(){return Math.random().toString(36).substring(2,10)}function ue(e,t){return{usr:e.state,key:e.key,idx:t,masked:e.mask?{pathname:e.pathname,search:e.search,hash:e.hash}:void 0}}function de(e,t,n=null,r,i){return{pathname:typeof e==`string`?e:e.pathname,search:``,hash:``,...typeof t==`string`?k(t):t,state:n,key:t&&t.key||r||le(),mask:i}}function fe({pathname:e=`/`,search:t=``,hash:n=``}){return t&&t!==`?`&&(e+=t.charAt(0)===`?`?t:`?`+t),n&&n!==`#`&&(e+=n.charAt(0)===`#`?n:`#`+n),e}function k(e){let t={};if(e){let n=e.indexOf(`#`);n>=0&&(t.hash=e.substring(n),e=e.substring(0,n));let r=e.indexOf(`?`);r>=0&&(t.search=e.substring(r),e=e.substring(0,r)),e&&(t.pathname=e)}return t}function A(e,t,n,r={}){let{window:i=document.defaultView,v5Compat:a=!1}=r,o=i.history,s=`POP`,c=null,l=u();l??(l=0,o.replaceState({...o.state,idx:l},``));function u(){return(o.state||{idx:null}).idx}function d(){s=`POP`;let e=u(),t=e==null?null:e-l;l=e,c&&c({action:s,location:h.location,delta:t})}function f(e,t){s=`PUSH`;let r=se(e)?e:de(h.location,e,t);n&&n(r,e),l=u()+1;let d=ue(r,l),f=h.createHref(r.mask||r);try{o.pushState(d,``,f)}catch(e){if(e instanceof DOMException&&e.name===`DataCloneError`)throw e;i.location.assign(f)}a&&c&&c({action:s,location:h.location,delta:1})}function p(e,t){s=`REPLACE`;let r=se(e)?e:de(h.location,e,t);n&&n(r,e),l=u();let i=ue(r,l),d=h.createHref(r.mask||r);o.replaceState(i,``,d),a&&c&&c({action:s,location:h.location,delta:0})}function m(e){return pe(i,e)}let h={get action(){return s},get location(){return e(i,o)},listen(e){if(c)throw Error(`A history only accepts one active listener`);return i.addEventListener(oe,d),c=e,()=>{i.removeEventListener(oe,d),c=null}},createHref(e){return t(i,e)},createURL:m,encodeLocation(e){let t=m(e);return{pathname:t.pathname,search:t.search,hash:t.hash}},push:f,replace:p,go(e){return o.go(e)}};return h}function pe(e,t,n=!1){let r=`http://localhost`;e&&(r=e.location.origin===`null`?e.location.href:e.location.origin),D(r,`No window.location.(origin|href) available to create URL`);let i=typeof t==`string`?t:fe(t);return i=i.replace(/ $/,`%20`),!n&&E.test(i)&&(i=r+i),new URL(i,r)}var j=e(n(),1);function me(e,t,n=`/`){return he(e,t,n,!1)}function he(e,t,n,r,i){let a=Le((typeof t==`string`?k(t):t).pathname||`/`,n);if(a==null)return null;let o=i??_e(e),s=null,c=Ie(a);for(let e=0;s==null&&e<o.length;++e)s=Me(o[e],c,r);return s}function ge(e,t){let{route:n,pathname:r,params:i}=e;return{id:n.id,pathname:r,params:i,loaderData:t[n.id],handle:n.handle}}function _e(e){let t=ve(e);return be(t),t}function ve(e,t=[],n=[],r=``,i=!1){let a=(e,a,o=i,s)=>{let c={relativePath:s===void 0?e.path||``:s,caseSensitive:e.caseSensitive===!0,childrenIndex:a,route:e};if(c.relativePath.startsWith(`/`)){if(!c.relativePath.startsWith(r)&&o)return;D(c.relativePath.startsWith(r),`Absolute route path "${c.relativePath}" nested under path "${r}" is not valid. An absolute child route path must start with the combined path of all its parent routes.`),c.relativePath=c.relativePath.slice(r.length)}let l=Ge([r,c.relativePath]),u=n.concat(c);e.children&&e.children.length>0&&(D(e.index!==!0,`Index routes must not have child routes. Please remove all child routes from route path "${l}".`),ve(e.children,t,u,l,o)),!(e.path==null&&!e.index)&&t.push({path:l,score:Ae(l,e.index),routesMeta:u.map((e,t)=>{let[n,r]=Fe(e.relativePath,e.caseSensitive,t===u.length-1);return{...e,matcher:n,compiledParams:r}})})};return e.forEach((e,t)=>{if(e.path===``||!e.path?.includes(`?`))a(e,t);else for(let n of ye(e.path))a(e,t,!0,n)}),t}function ye(e){let t=e.split(`/`);if(t.length===0)return[];let[n,...r]=t,i=n.endsWith(`?`),a=n.replace(/\?$/,``);if(r.length===0)return i?[a,``]:[a];let o=ye(r.join(`/`)),s=[];return s.push(...o.map(e=>e===``?a:[a,e].join(`/`))),i&&s.push(...o),s.map(t=>e.startsWith(`/`)&&t===``?`/`:t)}function be(e){e.sort((e,t)=>e.score===t.score?je(e.routesMeta.map(e=>e.childrenIndex),t.routesMeta.map(e=>e.childrenIndex)):t.score-e.score)}var xe=/^:[\w-]+$/,Se=/^:[\w-]+/,Ce=3.5,we=3,Te=2,Ee=1,De=10,Oe=-2,ke=e=>e===`*`;function Ae(e,t){let n=e.split(`/`),r=n.length;return n.some(ke)&&(r+=Oe),t&&(r+=Te),n.filter(e=>!ke(e)).reduce((e,t)=>e+(xe.test(t)?we:Se.test(t)?Ce:t===``?Ee:De),r)}function je(e,t){return e.length===t.length&&e.slice(0,-1).every((e,n)=>e===t[n])?e[e.length-1]-t[t.length-1]:0}function Me(e,t,n=!1){let{routesMeta:r}=e,i={},a=`/`,o=[];for(let e=0;e<r.length;++e){let s=r[e],c=e===r.length-1,l=a===`/`?t:t.slice(a.length)||`/`,u={path:s.relativePath,caseSensitive:s.caseSensitive,end:c},d=s.matcher&&s.compiledParams?Pe(u,l,s.matcher,s.compiledParams):Ne(u,l),f=s.route;if(!d&&c&&n&&!r[r.length-1].route.index&&(d=Ne({path:s.relativePath,caseSensitive:s.caseSensitive,end:!1},l)),!d)return null;Object.assign(i,d.params),o.push({params:i,pathname:Ge([a,d.pathname]),pathnameBase:qe(Ge([a,d.pathnameBase])),route:f}),d.pathnameBase!==`/`&&(a=Ge([a,d.pathnameBase]))}return o}function Ne(e,t){typeof e==`string`&&(e={path:e,caseSensitive:!1,end:!0});let[n,r]=Fe(e.path,e.caseSensitive,e.end);return Pe(e,t,n,r)}function Pe(e,t,n,r){let i=t.match(n);if(!i)return null;let a=i[0],o=a.replace(/(.)\/+$/,`$1`),s=i.slice(1);return{params:r.reduce((e,{paramName:t,isOptional:n},r)=>{if(t===`*`){let e=s[r]||``;o=a.slice(0,a.length-e.length).replace(/(.)\/+$/,`$1`)}let i=s[r];return n&&!i?e[t]=void 0:e[t]=(i||``).replace(/%2F/g,`/`),e},{}),pathname:a,pathnameBase:o,pattern:e}}function Fe(e,t=!1,n=!0){O(e===`*`||!e.endsWith(`*`)||e.endsWith(`/*`),`Route path "${e}" will be treated as if it were "${e.replace(/\*$/,`/*`)}" because the \`*\` character must always follow a \`/\` in the pattern. To get rid of this warning, please change the route path to "${e.replace(/\*$/,`/*`)}".`);let r=[],i=`^`+e.replace(/\/*\*?$/,``).replace(/^\/*/,`/`).replace(/[\\.*+^${}|()[\]]/g,`\\$&`).replace(/\/:([\w-]+)(\?)?/g,(e,t,n,i,a)=>{if(r.push({paramName:t,isOptional:n!=null}),n){let t=a.charAt(i+e.length);return t&&t!==`/`?`/([^\\/]*)`:`(?:/([^\\/]*))?`}return`/([^\\/]+)`}).replace(/\/([\w-]+)\?(?=\/|$|\()/g,`(?:/$1)?`);return e.endsWith(`*`)?(r.push({paramName:`*`}),i+=e===`*`||e===`/*`?`(.*)$`:`(?:\\/(.+)|\\/*)$`):n?i+=`\\/*$`:e!==``&&e!==`/`&&(i+=`(?:(?=\\/|$))`),[new RegExp(i,t?void 0:`i`),r]}function Ie(e){try{return e.split(`/`).map(e=>decodeURIComponent(e).replace(/\//g,`%2F`)).join(`/`)}catch(t){return O(!1,`The URL path "${e}" could not be decoded because it is a malformed URL segment. This is probably due to a bad percent encoding (${t}).`),e}}function Le(e,t){if(t===`/`)return e;if(!e.toLowerCase().startsWith(t.toLowerCase()))return null;let n=t.endsWith(`/`)?t.length-1:t.length,r=e.charAt(n);return r&&r!==`/`?null:e.slice(n)||`/`}function Re(e,t=`/`){let{pathname:n,search:r=``,hash:i=``}=typeof e==`string`?k(e):e,a;return n?(n=We(n),a=n.startsWith(`/`)?ze(n.substring(1),`/`):ze(n,t)):a=t,{pathname:a,search:Je(r),hash:Ye(i)}}function ze(e,t){let n=Ke(t).split(`/`);return e.split(`/`).forEach(e=>{e===`..`?n.length>1&&n.pop():e!==`.`&&n.push(e)}),n.length>1?n.join(`/`):`/`}function Be(e,t,n,r){return`Cannot include a '${e}' character in a manually specified \`to.${t}\` field [${JSON.stringify(r)}].  Please separate it out to the \`to.${n}\` field. Alternatively you may provide the full path as a string in <Link to="..."> and the router will parse it for you.`}function Ve(e){return e.filter((e,t)=>t===0||e.route.path&&e.route.path.length>0)}function He(e){let t=Ve(e);return t.map((e,n)=>n===t.length-1?e.pathname:e.pathnameBase)}function Ue(e,t,n,r=!1){let i;typeof e==`string`?i=k(e):(i={...e},D(!i.pathname||!i.pathname.includes(`?`),Be(`?`,`pathname`,`search`,i)),D(!i.pathname||!i.pathname.includes(`#`),Be(`#`,`pathname`,`hash`,i)),D(!i.search||!i.search.includes(`#`),Be(`#`,`search`,`hash`,i)));let a=e===``||i.pathname===``,o=a?`/`:i.pathname,s;if(o==null)s=n;else{let e=t.length-1;if(!r&&o.startsWith(`..`)){let t=o.split(`/`);for(;t[0]===`..`;)t.shift(),--e;i.pathname=t.join(`/`)}s=e>=0?t[e]:`/`}let c=Re(i,s),l=o&&o!==`/`&&o.endsWith(`/`),u=(a||o===`.`)&&n.endsWith(`/`);return!c.pathname.endsWith(`/`)&&(l||u)&&(c.pathname+=`/`),c}var We=e=>e.replace(/[\\/]{2,}/g,`/`),Ge=e=>We(e.join(`/`)),Ke=e=>e.replace(/\/+$/,``),qe=e=>Ke(e).replace(/^\/*/,`/`),Je=e=>!e||e===`?`?``:e.startsWith(`?`)?e:`?`+e,Ye=e=>!e||e===`#`?``:e.startsWith(`#`)?e:`#`+e,Xe=class{status;statusText;data;error;internal;constructor(e,t,n,r=!1){this.status=e,this.statusText=t||``,this.internal=r,n instanceof Error?(this.data=n.toString(),this.error=n):this.data=n}};function Ze(e){return e!=null&&typeof e.status==`number`&&typeof e.statusText==`string`&&typeof e.internal==`boolean`&&`data`in e}function Qe(e){return Ge(e.map(e=>e.route.path).filter(Boolean))||`/`}var $e=typeof window<`u`&&window.document!==void 0&&window.document.createElement!==void 0;function et(e,t){let n=e;if(typeof n!=`string`||!ie.test(n))return{absoluteURL:void 0,isExternal:!1,to:n};let r=n,i=!1;if($e)try{let e=new URL(window.location.href),r=E.test(n)?new URL(ae(n,e.protocol)):new URL(n),a=Le(r.pathname,t);r.origin===e.origin&&a!=null?n=a+r.search+r.hash:i=!0}catch{O(!1,`<Link to="${n}"> contains an invalid URL which will probably break when clicked - please update to a valid URL path.`)}return{absoluteURL:r,isExternal:i,to:n}}var tt=[`POST`,`PUT`,`PATCH`,`DELETE`];new Set(tt);var nt=[`GET`,...tt];new Set(nt);var rt=[`about:`,`blob:`,`chrome:`,`chrome-untrusted:`,`content:`,`data:`,`devtools:`,`file:`,`filesystem:`,`javascript:`];function it(e){try{return rt.includes(new URL(e).protocol)}catch{return!1}}var at=j.createContext(null);at.displayName=`DataRouter`;var ot=j.createContext(null);ot.displayName=`DataRouterState`;var st=j.createContext(!1);function ct(){return j.useContext(st)}var lt=j.createContext({isTransitioning:!1});lt.displayName=`ViewTransition`;var ut=j.createContext(new Map);ut.displayName=`Fetchers`;var dt=j.createContext(null);dt.displayName=`Await`;var ft=j.createContext(null);ft.displayName=`Navigation`;var pt=j.createContext(null);pt.displayName=`Location`;var mt=j.createContext({outlet:null,matches:[],isDataRoute:!1});mt.displayName=`Route`;var ht=j.createContext(null);ht.displayName=`RouteError`;var gt=`REACT_ROUTER_ERROR`,_t=`REDIRECT`,vt=`ROUTE_ERROR_RESPONSE`;function yt(e){if(e.startsWith(`${gt}:${_t}:{`))try{let t=JSON.parse(e.slice(28));if(typeof t==`object`&&t&&typeof t.status==`number`&&typeof t.statusText==`string`&&typeof t.location==`string`&&typeof t.reloadDocument==`boolean`&&typeof t.replace==`boolean`)return t}catch{}}function bt(e){if(e.startsWith(`${gt}:${vt}:{`))try{let t=JSON.parse(e.slice(40));if(typeof t==`object`&&t&&typeof t.status==`number`&&typeof t.statusText==`string`)return new Xe(t.status,t.statusText,t.data)}catch{}}function xt(e,{relative:t}={}){D(St(),`useHref() may be used only in the context of a <Router> component.`);let{basename:n,navigator:r}=j.useContext(ft),{hash:i,pathname:a,search:o}=Dt(e,{relative:t}),s=a;return n!==`/`&&(s=a===`/`?n:Ge([n,a])),r.createHref({pathname:s,search:o,hash:i})}function St(){return j.useContext(pt)!=null}function Ct(){return D(St(),`useLocation() may be used only in the context of a <Router> component.`),j.useContext(pt).location}var wt=`You should call navigate() in a React.useEffect(), not when your component is first rendered.`;function Tt(){let{isDataRoute:e}=j.useContext(mt);return e?Kt():Et()}function Et(){D(St(),`useNavigate() may be used only in the context of a <Router> component.`);let e=j.useContext(at),{basename:t,navigator:n}=j.useContext(ft),{matches:r}=j.useContext(mt),{pathname:i}=Ct(),a=JSON.stringify(He(r)),o=j.useRef(!1);return j.useLayoutEffect(()=>{o.current=!0}),j.useCallback((r,s={})=>{if(O(o.current,wt),!o.current)return;if(typeof r==`number`){n.go(r);return}let c=Ue(r,JSON.parse(a),i,s.relative===`path`);e==null&&t!==`/`&&(c.pathname=c.pathname===`/`?t:Ge([t,c.pathname])),(s.replace?n.replace:n.push)(c,s.state,s)},[t,n,a,i,e])}j.createContext(null);function Dt(e,{relative:t}={}){let{matches:n}=j.useContext(mt),{pathname:r}=Ct(),i=JSON.stringify(He(n));return j.useMemo(()=>Ue(e,JSON.parse(i),r,t===`path`),[e,i,r,t])}function Ot(e,t){return kt(e,t)}function kt(e,t,n){D(St(),`useRoutes() may be used only in the context of a <Router> component.`);let{navigator:r}=j.useContext(ft),{matches:i}=j.useContext(mt),a=i[i.length-1],o=a?a.params:{};a&&a.pathname;let s=a?a.pathnameBase:`/`;a&&a.route;let c=Ct(),l;if(t){let e=typeof t==`string`?k(t):t;D(s===`/`||e.pathname?.startsWith(s),`When overriding the location using \`<Routes location>\` or \`useRoutes(routes, location)\`, the location pathname must begin with the portion of the URL pathname that was matched by all parent routes. The current pathname base is "${s}" but pathname "${e.pathname}" was given in the \`location\` prop.`),l=e}else l=c;let u=l.pathname||`/`,d=u;if(s!==`/`){let e=s.replace(/^\//,``).split(`/`);d=`/`+u.replace(/^\//,``).split(`/`).slice(e.length).join(`/`)}let f=n&&n.state.matches.length?n.state.matches.map(e=>Object.assign(e,{route:n.manifest[e.route.id]||e.route})):me(e,{pathname:d}),p=It(f&&f.map(e=>Object.assign({},e,{params:Object.assign({},o,e.params),pathname:Ge([s,r.encodeLocation?r.encodeLocation(e.pathname.replace(/%/g,`%25`).replace(/\?/g,`%3F`).replace(/#/g,`%23`)).pathname:e.pathname]),pathnameBase:e.pathnameBase===`/`?s:Ge([s,r.encodeLocation?r.encodeLocation(e.pathnameBase.replace(/%/g,`%25`).replace(/\?/g,`%3F`).replace(/#/g,`%23`)).pathname:e.pathnameBase])})),i,n);return t&&p?j.createElement(pt.Provider,{value:{location:{pathname:`/`,search:``,hash:``,state:null,key:`default`,mask:void 0,...l},navigationType:`POP`}},p):p}function At(){let e=Gt(),t=Ze(e)?`${e.status} ${e.statusText}`:e instanceof Error?e.message:JSON.stringify(e),n=e instanceof Error?e.stack:null;return j.createElement(j.Fragment,null,j.createElement(`h2`,null,`Unexpected Application Error!`),j.createElement(`h3`,{style:{fontStyle:`italic`}},t),n?j.createElement(`pre`,{style:{padding:`0.5rem`,backgroundColor:`rgba(200,200,200, 0.5)`}},n):null,null)}var jt=j.createElement(At,null),Mt=class extends j.Component{constructor(e){super(e),this.state={location:e.location,revalidation:e.revalidation,error:e.error}}static contextType=st;static getDerivedStateFromError(e){return{error:e}}static getDerivedStateFromProps(e,t){return t.location!==e.location||t.revalidation!==`idle`&&e.revalidation===`idle`?{error:e.error,location:e.location,revalidation:e.revalidation}:{error:e.error===void 0?t.error:e.error,location:t.location,revalidation:e.revalidation||t.revalidation}}componentDidCatch(e,t){this.props.onError?this.props.onError(e,t):console.error(`React Router caught the following error during render`,e)}render(){let e=this.state.error;if(this.context&&typeof e==`object`&&e&&`digest`in e&&typeof e.digest==`string`){let t=bt(e.digest);t&&(e=t)}let t=e===void 0?this.props.children:j.createElement(mt.Provider,{value:this.props.routeContext},j.createElement(ht.Provider,{value:e,children:this.props.component}));return this.context?j.createElement(Pt,{error:e},t):t}},Nt=new WeakMap;function Pt({children:e,error:t}){let{basename:n}=j.useContext(ft);if(typeof t==`object`&&t&&`digest`in t&&typeof t.digest==`string`){let e=yt(t.digest);if(e){let r=Nt.get(t);if(r)throw r;let i=et(e.location,n),a=i.absoluteURL||i.to;if(it(a))throw Error(`Invalid redirect location`);if($e&&!Nt.get(t))if(i.isExternal||e.reloadDocument)window.location.href=a;else{let n=Promise.resolve().then(()=>window.__reactRouterDataRouter.navigate(i.to,{replace:e.replace}));throw Nt.set(t,n),n}return j.createElement(`meta`,{httpEquiv:`refresh`,content:`0;url=${a}`})}}return e}function Ft({routeContext:e,match:t,children:n}){let r=j.useContext(at);return r&&r.static&&r.staticContext&&(t.route.errorElement||t.route.ErrorBoundary)&&(r.staticContext._deepestRenderedBoundaryId=t.route.id),j.createElement(mt.Provider,{value:e},n)}function It(e,t=[],n){let r=n?.state;if(e==null){if(!r)return null;if(r.errors)e=r.matches;else if(t.length===0&&!r.initialized&&r.matches.length>0)e=r.matches;else return null}let i=e,a=r?.errors;if(a!=null){let e=i.findIndex(e=>e.route.id&&a?.[e.route.id]!==void 0);D(e>=0,`Could not find a matching route for errors on route IDs: ${Object.keys(a).join(`,`)}`),i=i.slice(0,Math.min(i.length,e+1))}let o=!1,s=-1;if(n&&r){o=r.renderFallback;for(let e=0;e<i.length;e++){let t=i[e];if((t.route.HydrateFallback||t.route.hydrateFallbackElement)&&(s=e),t.route.id){let{loaderData:e,errors:a}=r,c=t.route.loader&&!e.hasOwnProperty(t.route.id)&&(!a||a[t.route.id]===void 0);if(t.route.lazy||c){n.isStatic&&(o=!0),i=s>=0?i.slice(0,s+1):[i[0]];break}}}}let c=n?.onError,l=r&&c?(e,t)=>{c(e,{location:r.location,params:r.matches?.[0]?.params??{},pattern:Qe(r.matches),errorInfo:t})}:void 0;return i.reduceRight((e,n,c)=>{let u,d=!1,f=null,p=null;r&&(u=a&&n.route.id?a[n.route.id]:void 0,f=n.route.errorElement||jt,o&&(s<0&&c===0?(Jt(`route-fallback`,!1,"No `HydrateFallback` element provided to render during initial hydration"),d=!0,p=null):s===c&&(d=!0,p=n.route.hydrateFallbackElement||null)));let m=t.concat(i.slice(0,c+1)),h=()=>{let t;return t=u?f:d?p:n.route.Component?j.createElement(n.route.Component,null):n.route.element?n.route.element:e,j.createElement(Ft,{match:n,routeContext:{outlet:e,matches:m,isDataRoute:r!=null},children:t})};return r&&(n.route.ErrorBoundary||n.route.errorElement||c===0)?j.createElement(Mt,{location:r.location,revalidation:r.revalidation,component:f,error:u,children:h(),routeContext:{outlet:null,matches:m,isDataRoute:!0},onError:l}):h()},null)}function Lt(e){return`${e} must be used within a data router.  See https://reactrouter.com/en/main/routers/picking-a-router.`}function Rt(e){let t=j.useContext(at);return D(t,Lt(e)),t}function zt(e){let t=j.useContext(ot);return D(t,Lt(e)),t}function Bt(e){let t=j.useContext(mt);return D(t,Lt(e)),t}function Vt(e){let t=Bt(e),n=t.matches[t.matches.length-1];return D(n.route.id,`${e} can only be used on routes that contain a unique "id"`),n.route.id}function Ht(){return Vt(`useRouteId`)}function Ut(){let e=zt(`useNavigation`);return j.useMemo(()=>{let{matches:t,historyAction:n,...r}=e.navigation;return r},[e.navigation])}function Wt(){let{matches:e,loaderData:t}=zt(`useMatches`);return j.useMemo(()=>e.map(e=>ge(e,t)),[e,t])}function Gt(){let e=j.useContext(ht),t=zt(`useRouteError`),n=Vt(`useRouteError`);return e===void 0?t.errors?.[n]:e}function Kt(){let{router:e}=Rt(`useNavigate`),t=Vt(`useNavigate`),n=j.useRef(!1);return j.useLayoutEffect(()=>{n.current=!0}),j.useCallback(async(r,i={})=>{O(n.current,wt),n.current&&(typeof r==`number`?await e.navigate(r):await e.navigate(r,{fromRouteId:t,...i}))},[e,t])}var qt={};function Jt(e,t,n){!t&&!qt[e]&&(qt[e]=!0,O(!1,n))}j.memo(Yt);function Yt({routes:e,manifest:t,future:n,state:r,isStatic:i,onError:a}){return kt(e,void 0,{manifest:t,state:r,isStatic:i,onError:a,future:n})}function Xt({to:e,replace:t,state:n,relative:r}){D(St(),`<Navigate> may be used only in the context of a <Router> component.`);let{static:i}=j.useContext(ft);O(!i,`<Navigate> must not be used on the initial render in a <StaticRouter>. This is a no-op, but you should modify your code so the <Navigate> is only ever rendered in response to some user interaction or state change.`);let{matches:a}=j.useContext(mt),{pathname:o}=Ct(),s=Tt(),c=Ue(e,He(a),o,r===`path`),l=JSON.stringify(c);return j.useEffect(()=>{s(JSON.parse(l),{replace:t,state:n,relative:r})},[s,l,r,t,n]),null}function Zt(e){D(!1,`A <Route> is only ever to be used as the child of <Routes> element, never rendered directly. Please wrap your <Route> in a <Routes>.`)}function Qt({basename:e=`/`,children:t=null,location:n,navigationType:r=`POP`,navigator:i,static:a=!1,useTransitions:o}){D(!St(),`You cannot render a <Router> inside another <Router>. You should never have more than one in your app.`);let s=e.replace(/^\/*/,`/`),c=j.useMemo(()=>({basename:s,navigator:i,static:a,useTransitions:o,future:{}}),[s,i,a,o]);typeof n==`string`&&(n=k(n));let{pathname:l=`/`,search:u=``,hash:d=``,state:f=null,key:p=`default`,mask:m}=n,h=j.useMemo(()=>{let e=Le(l,s);return e==null?null:{location:{pathname:e,search:u,hash:d,state:f,key:p,mask:m},navigationType:r}},[s,l,u,d,f,p,r,m]);return O(h!=null,`<Router basename="${s}"> is not able to match the URL "${l}${u}${d}" because it does not start with the basename, so the <Router> won't render anything.`),h==null?null:j.createElement(ft.Provider,{value:c},j.createElement(pt.Provider,{children:t,value:h}))}function $t({children:e,location:t}){return Ot(en(e),t)}j.Component;function en(e,t=[]){let n=[];return j.Children.forEach(e,(e,r)=>{if(!j.isValidElement(e))return;let i=[...t,r];if(e.type===j.Fragment){n.push.apply(n,en(e.props.children,i));return}D(e.type===Zt,`[${typeof e.type==`string`?e.type:e.type.name}] is not a <Route> component. All component children of <Routes> must be a <Route> or <React.Fragment>`);let a=e.props;D(!a.index||!a.children,`An index route cannot have child routes.`);let o={id:a.id||i.join(`-`),caseSensitive:a.caseSensitive,element:a.element,Component:a.Component,index:a.index,path:a.path,middleware:a.middleware,loader:a.loader,action:a.action,hydrateFallbackElement:a.hydrateFallbackElement,HydrateFallback:a.HydrateFallback,errorElement:a.errorElement,ErrorBoundary:a.ErrorBoundary,shouldRevalidate:a.shouldRevalidate,handle:a.handle,lazy:a.lazy};a.children&&(o.children=en(a.children,i)),n.push(o)}),n}var tn=`application/x-www-form-urlencoded`;function nn(e){return typeof HTMLElement<`u`&&e instanceof HTMLElement}function rn(e){return nn(e)&&e.tagName.toLowerCase()===`button`}function an(e){return nn(e)&&e.tagName.toLowerCase()===`form`}function on(e){return nn(e)&&e.tagName.toLowerCase()===`input`}function sn(e){return!!(e.metaKey||e.altKey||e.ctrlKey||e.shiftKey)}function cn(e,t){return e.button===0&&(!t||t===`_self`)&&!sn(e)}var ln=null;function un(){if(ln===null)try{new FormData(document.createElement(`form`),0),ln=!1}catch{ln=!0}return ln}var dn=new Set([`application/x-www-form-urlencoded`,`multipart/form-data`,`text/plain`]);function fn(e){return e!=null&&!dn.has(e)?(O(!1,`"${e}" is not a valid \`encType\` for \`<Form>\`/\`<fetcher.Form>\` and will default to "${tn}"`),null):e}function pn(e,t){let n,r,i,a,o;if(an(e)){let o=e.getAttribute(`action`);r=o?Le(o,t):null,n=e.getAttribute(`method`)||`get`,i=fn(e.getAttribute(`enctype`))||tn,a=new FormData(e)}else if(rn(e)||on(e)&&(e.type===`submit`||e.type===`image`)){let o=e.form;if(o==null)throw Error(`Cannot submit a <button> or <input type="submit"> without a <form>`);let s=e.getAttribute(`formaction`)||o.getAttribute(`action`);if(r=s?Le(s,t):null,n=e.getAttribute(`formmethod`)||o.getAttribute(`method`)||`get`,i=fn(e.getAttribute(`formenctype`))||fn(o.getAttribute(`enctype`))||tn,a=new FormData(o,e),!un()){let{name:t,type:n,value:r}=e;if(n===`image`){let e=t?`${t}.`:``;a.append(`${e}x`,`0`),a.append(`${e}y`,`0`)}else t&&a.append(t,r)}}else if(nn(e))throw Error(`Cannot submit element that is not <form>, <button>, or <input type="submit|image">`);else n=`get`,r=null,i=tn,o=e;return a&&i===`text/plain`&&(o=a,a=void 0),{action:r,method:n.toLowerCase(),encType:i,formData:a,body:o}}function mn(e,t){if(e===!1||e==null)throw Error(t)}var hn={"&":`\\u0026`,">":`\\u003e`,"<":`\\u003c`,"\u2028":`\\u2028`,"\u2029":`\\u2029`},gn=/[&><\u2028\u2029]/g;function _n(e){return e.replace(gn,e=>hn[e])}function vn(e,t){let n=typeof e==`string`?new URL(e,typeof window>`u`?`server://singlefetch/`:window.location.origin):e;return n.pathname.endsWith(`/`)?n.pathname=`${n.pathname}_.${t}`:n.pathname=`${n.pathname}.${t}`,n}var yn=`modulepreload`,bn=function(e){return`/`+e},xn={},Sn=function(e,t,n){let r=Promise.resolve();if(t&&t.length>0){let e=document.getElementsByTagName(`link`),i=document.querySelector(`meta[property=csp-nonce]`),a=i?.nonce||i?.getAttribute(`nonce`);function o(e){return Promise.all(e.map(e=>Promise.resolve(e).then(e=>({status:`fulfilled`,value:e}),e=>({status:`rejected`,reason:e}))))}function s(e){return import.meta.resolve?import.meta.resolve(e):new URL(e,import.meta.url).href}r=o(t.map(t=>{if(t=bn(t,n),t=s(t),t in xn)return;xn[t]=!0;let r=t.endsWith(`.css`);for(let n=e.length-1;n>=0;n--){let i=e[n];if(i.href===t&&(!r||i.rel===`stylesheet`))return}let i=document.createElement(`link`);if(i.rel=r?`stylesheet`:yn,r||(i.as=`script`),i.crossOrigin=``,i.href=t,a&&i.setAttribute(`nonce`,a),document.head.appendChild(i),r)return new Promise((e,n)=>{i.addEventListener(`load`,e),i.addEventListener(`error`,()=>n(Error(`Unable to preload CSS for ${t}`)))})}))}function i(e){let t=new Event(`vite:preloadError`,{cancelable:!0});if(t.payload=e,window.dispatchEvent(t),!t.defaultPrevented)throw e}return r.then(t=>{for(let e of t||[])e.status===`rejected`&&i(e.reason);return e().catch(i)})};async function Cn(e,t){if(e.id in t)return t[e.id];try{let n=await Sn(()=>import(e.module),[]);return t[e.id]=n,n}catch(t){return console.error(`Error loading route module \`${e.module}\`, reloading page...`),console.error(t),window.__reactRouterContext&&window.__reactRouterContext.isSpaMode,window.location.reload(),new Promise(()=>{})}}function wn(e){return e!=null&&typeof e.page==`string`}function Tn(e){return e==null?!1:e.href==null?e.rel===`preload`&&typeof e.imageSrcSet==`string`&&typeof e.imageSizes==`string`:typeof e.rel==`string`&&typeof e.href==`string`}async function En(e,t,n){return jn((await Promise.all(e.map(async e=>{let r=t.routes[e.route.id];if(r){let e=await Cn(r,n);return e.links?e.links():[]}return[]}))).flat(1).filter(Tn).filter(e=>e.rel===`stylesheet`||e.rel===`preload`).map(e=>e.rel===`stylesheet`?{...e,rel:`prefetch`,as:`style`}:{...e,rel:`prefetch`}))}function Dn(e,t,n,r,i,a){let o=(e,t)=>!n[t]||e.route.id!==n[t].route.id,s=(e,t)=>n[t].pathname!==e.pathname||n[t].route.path?.endsWith(`*`)&&n[t].params[`*`]!==e.params[`*`];return a===`assets`?t.filter((e,t)=>o(e,t)||s(e,t)):a===`data`?t.filter((t,a)=>{let c=r.routes[t.route.id];if(!c||!c.hasLoader)return!1;if(o(t,a)||s(t,a))return!0;if(t.route.shouldRevalidate){let r=t.route.shouldRevalidate({currentUrl:new URL(i.pathname+i.search+i.hash,window.origin),currentParams:n[0]?.params||{},nextUrl:new URL(e,window.origin),nextParams:t.params,defaultShouldRevalidate:!0});if(typeof r==`boolean`)return r}return!0}):[]}function On(e,t,{includeHydrateFallback:n}={}){return kn(e.map(e=>{let r=t.routes[e.route.id];if(!r)return[];let i=[r.module];return r.clientActionModule&&(i=i.concat(r.clientActionModule)),r.clientLoaderModule&&(i=i.concat(r.clientLoaderModule)),n&&r.hydrateFallbackModule&&(i=i.concat(r.hydrateFallbackModule)),r.imports&&(i=i.concat(r.imports)),i}).flat(1))}function kn(e){return[...new Set(e)]}function An(e){let t={},n=Object.keys(e).sort();for(let r of n)t[r]=e[r];return t}function jn(e,t){let n=new Set,r=new Set(t);return e.reduce((e,i)=>{if(t&&!wn(i)&&i.as===`script`&&i.href&&r.has(i.href))return e;let a=JSON.stringify(An(i));return n.has(a)||(n.add(a),e.push({key:a,link:i})),e},[])}function Mn(){let e=j.useContext(at);return mn(e,`You must render this element inside a <DataRouterContext.Provider> element`),e}function Nn(){let e=j.useContext(ot);return mn(e,`You must render this element inside a <DataRouterStateContext.Provider> element`),e}var Pn=j.createContext(void 0);Pn.displayName=`FrameworkContext`;function Fn(){let e=j.useContext(Pn);return mn(e,`You must render this element inside a <HydratedRouter> element`),e}function In(e,t){let n=j.useContext(Pn),[r,i]=j.useState(!1),[a,o]=j.useState(!1),{onFocus:s,onBlur:c,onMouseEnter:l,onMouseLeave:u,onTouchStart:d}=t,f=j.useRef(null);j.useEffect(()=>{if(e===`render`&&o(!0),e===`viewport`){let e=new IntersectionObserver(e=>{e.forEach(e=>{o(e.isIntersecting)})},{threshold:.5});return f.current&&e.observe(f.current),()=>{e.disconnect()}}},[e]),j.useEffect(()=>{if(r){let e=setTimeout(()=>{o(!0)},100);return()=>{clearTimeout(e)}}},[r]);let p=()=>{i(!0)},m=()=>{i(!1),o(!1)};return n?e===`intent`?[a,f,{onFocus:Ln(s,p),onBlur:Ln(c,m),onMouseEnter:Ln(l,p),onMouseLeave:Ln(u,m),onTouchStart:Ln(d,p)}]:[a,f,{}]:[!1,f,{}]}function Ln(e,t){return n=>{e&&e(n),n.defaultPrevented||t(n)}}function Rn({page:e,...t}){let n=ct(),{nonce:r}=Fn(),{router:i}=Mn(),a=j.useMemo(()=>me(i.routes,e,i.basename),[i.routes,e,i.basename]);return a?(t.nonce==null&&r&&(t={...t,nonce:r}),n?j.createElement(Bn,{page:e,matches:a,...t}):j.createElement(Vn,{page:e,matches:a,...t})):null}function zn(e){let{manifest:t,routeModules:n}=Fn(),[r,i]=j.useState([]);return j.useEffect(()=>{let r=!1;return En(e,t,n).then(e=>{r||i(e)}),()=>{r=!0}},[e,t,n]),r}function Bn({page:e,matches:t,...n}){let r=Ct(),i=j.useMemo(()=>{if(e===r.pathname+r.search+r.hash)return[];let n=vn(e,`rsc`),i=!1,a=[];for(let e of t)typeof e.route.shouldRevalidate==`function`?i=!0:a.push(e.route.id);return i&&a.length>0&&n.searchParams.set(`_routes`,a.join(`,`)),[n.pathname+n.search]},[e,r,t]);return j.createElement(j.Fragment,null,i.map(e=>j.createElement(`link`,{key:e,rel:`prefetch`,as:`fetch`,href:e,...n})))}function Vn({page:e,matches:t,...n}){let r=Ct(),{manifest:i,routeModules:a}=Fn(),{loaderData:o,matches:s}=Nn(),c=j.useMemo(()=>Dn(e,t,s,i,r,`data`),[e,t,s,i,r]),l=j.useMemo(()=>Dn(e,t,s,i,r,`assets`),[e,t,s,i,r]),u=j.useMemo(()=>{if(e===r.pathname+r.search+r.hash)return[];let n=new Set,s=!1;if(t.forEach(e=>{let t=i.routes[e.route.id];!t||!t.hasLoader||(!c.some(t=>t.route.id===e.route.id)&&e.route.id in o&&a[e.route.id]?.shouldRevalidate||t.hasClientLoader?s=!0:n.add(e.route.id))}),n.size===0)return[];let l=vn(e,`data`);return s&&n.size>0&&l.searchParams.set(`_routes`,t.filter(e=>n.has(e.route.id)).map(e=>e.route.id).join(`,`)),[l.pathname+l.search]},[o,r,i,c,t,e,a]),d=j.useMemo(()=>On(l,i),[l,i]),f=zn(l);return j.createElement(j.Fragment,null,u.map(e=>j.createElement(`link`,{key:e,rel:`prefetch`,as:`fetch`,href:e,...n})),d.map(e=>j.createElement(`link`,{key:e,rel:`modulepreload`,href:e,...n})),f.map(({key:e,link:t})=>j.createElement(`link`,{key:e,nonce:n.nonce,...t,crossOrigin:t.crossOrigin??n.crossOrigin})))}function Hn(...e){return t=>{e.forEach(e=>{typeof e==`function`?e(t):e!=null&&(e.current=t)})}}var Un=typeof window<`u`&&window.document!==void 0&&window.document.createElement!==void 0;try{Un&&(window.__reactRouterVersion=`8.3.0`)}catch{}function Wn({basename:e,children:t,useTransitions:n,window:r}){let i=j.useRef(null);i.current??=ce({window:r,v5Compat:!0});let a=i.current,[o,s]=j.useState({action:a.action,location:a.location}),c=j.useCallback(e=>{n===!1?s(e):j.startTransition(()=>s(e))},[n]);return j.useLayoutEffect(()=>a.listen(c),[a,c]),j.createElement(Qt,{basename:e,children:t,location:o.location,navigationType:o.action,navigator:a,useTransitions:n})}function Gn({basename:e,children:t,history:n,useTransitions:r}){let[i,a]=j.useState({action:n.action,location:n.location}),o=j.useCallback(e=>{r===!1?a(e):j.startTransition(()=>a(e))},[r]);return j.useLayoutEffect(()=>n.listen(o),[n,o]),j.createElement(Qt,{basename:e,children:t,location:i.location,navigationType:i.action,navigator:n,useTransitions:r})}Gn.displayName=`unstable_HistoryRouter`;var Kn=j.forwardRef(function({onClick:e,discover:t=`render`,prefetch:n=`none`,relative:r,reloadDocument:i,replace:a,mask:o,state:s,target:c,to:l,preventScrollReset:u,viewTransition:d,defaultShouldRevalidate:f,...p},m){let{basename:h,navigator:g,useTransitions:_}=j.useContext(ft),v=typeof l==`string`&&ie.test(l),y=et(l,h);l=y.to;let b=xt(l,{relative:r}),x=Ct(),S=null;if(o){let e=Ue(o,[],x.mask?x.mask.pathname:`/`,!0);h!==`/`&&(e.pathname=e.pathname===`/`?h:Ge([h,e.pathname])),S=g.createHref(e)}let[C,ee,te]=In(n,p),w=$n(l,{replace:a,mask:o,state:s,target:c,preventScrollReset:u,relative:r,viewTransition:d,defaultShouldRevalidate:f,useTransitions:_});function ne(t){e&&e(t),t.defaultPrevented||w(t)}let T=!(y.isExternal||i),re=j.createElement(`a`,{...p,...te,href:(T?S:void 0)||y.absoluteURL||b,onClick:T?ne:e,ref:Hn(m,ee),target:c,"data-discover":!v&&t===`render`?`true`:void 0});return C&&!v?j.createElement(j.Fragment,null,re,j.createElement(Rn,{page:b})):re});Kn.displayName=`Link`;var qn=j.forwardRef(function({"aria-current":e=`page`,caseSensitive:t=!1,className:n=``,end:r=!1,style:i,to:a,viewTransition:o,children:s,...c},l){let u=Dt(a,{relative:c.relative}),d=Ct(),f=j.useContext(ot),{navigator:p,basename:m}=j.useContext(ft),h=f!=null&&lr(u)&&o===!0,g=p.encodeLocation?p.encodeLocation(u).pathname:u.pathname,_=d.pathname,v=f&&f.navigation&&f.navigation.location?f.navigation.location.pathname:null;t||(_=_.toLowerCase(),v=v?v.toLowerCase():null,g=g.toLowerCase()),v&&m&&(v=Le(v,m)||v);let y=g!==`/`&&g.endsWith(`/`)?g.length-1:g.length,b=_===g||!r&&_.startsWith(g)&&_.charAt(y)===`/`,x=v!=null&&(v===g||!r&&v.startsWith(g)&&v.charAt(y)===`/`),S={isActive:b,isPending:x,isTransitioning:h},C=b?e:void 0,ee;ee=typeof n==`function`?n(S):[n,b?`active`:null,x?`pending`:null,h?`transitioning`:null].filter(Boolean).join(` `);let te=typeof i==`function`?i(S):i;return j.createElement(Kn,{...c,"aria-current":C,className:ee,ref:l,style:te,to:a,viewTransition:o},typeof s==`function`?s(S):s)});qn.displayName=`NavLink`;var Jn=j.forwardRef(({discover:e=`render`,fetcherKey:t,navigate:n,reloadDocument:r,replace:i,state:a,method:o=`get`,action:s,onSubmit:c,relative:l,preventScrollReset:u,viewTransition:d,defaultShouldRevalidate:f,...p},m)=>{let{useTransitions:h}=j.useContext(ft),g=nr(),_=rr(s,{relative:l}),v=o.toLowerCase()===`get`?`get`:`post`,y=typeof s==`string`&&ie.test(s);return j.createElement(`form`,{ref:m,method:v,action:_,onSubmit:r?c:e=>{if(c&&c(e),e.defaultPrevented)return;e.preventDefault();let r=e.nativeEvent.submitter,s=r?.getAttribute(`formmethod`)||o,p=()=>g(r||e.currentTarget,{fetcherKey:t,method:s,navigate:n,replace:i,state:a,relative:l,preventScrollReset:u,viewTransition:d,defaultShouldRevalidate:f});h&&n!==!1?j.startTransition(()=>p()):p()},...p,"data-discover":!y&&e===`render`?`true`:void 0})});Jn.displayName=`Form`;function Yn({getKey:e,storageKey:t,...n}){let r=j.useContext(Pn),{basename:i}=j.useContext(ft),a=Ct(),o=Wt();sr({getKey:e,storageKey:t});let s=j.useMemo(()=>{if(!r||!e)return null;let t=or(a,o,i,e);return t===a.key?null:t},[]);if(!r||r.isSpaMode)return null;let c=((e,t)=>{if(!window.history.state||!window.history.state.key){let e=Math.random().toString(32).slice(2);window.history.replaceState({key:e},``)}try{let n=JSON.parse(sessionStorage.getItem(e)||`{}`)[t||window.history.state.key];typeof n==`number`&&window.scrollTo(0,n)}catch(t){console.error(t),sessionStorage.removeItem(e)}}).toString();return n.nonce==null&&r?.nonce&&(n.nonce=r.nonce),j.createElement(`script`,{...n,suppressHydrationWarning:!0,dangerouslySetInnerHTML:{__html:`(${c})(${_n(JSON.stringify(t||ir))}, ${_n(JSON.stringify(s))})`}})}Yn.displayName=`ScrollRestoration`;function Xn(e){return`${e} must be used within a data router.  See https://reactrouter.com/en/main/routers/picking-a-router.`}function Zn(e){let t=j.useContext(at);return D(t,Xn(e)),t}function Qn(e){let t=j.useContext(ot);return D(t,Xn(e)),t}function $n(e,{target:t,replace:n,mask:r,state:i,preventScrollReset:a,relative:o,viewTransition:s,defaultShouldRevalidate:c,useTransitions:l}={}){let u=Tt(),d=Ct(),f=Dt(e,{relative:o});return j.useCallback(p=>{if(cn(p,t)){p.preventDefault();let t=n===void 0?fe(d)===fe(f):n,m=()=>u(e,{replace:t,mask:r,state:i,preventScrollReset:a,relative:o,viewTransition:s,defaultShouldRevalidate:c});l?j.startTransition(()=>m()):m()}},[d,u,f,n,r,i,t,e,a,o,s,c,l])}var er=0,tr=()=>`__${String(++er)}__`;function nr(){let{router:e}=Zn(`useSubmit`),{basename:t}=j.useContext(ft),n=Ht(),r=e.fetch,i=e.navigate;return j.useCallback(async(e,a={})=>{let{action:o,method:s,encType:c,formData:l,body:u}=pn(e,t);a.navigate===!1?await r(a.fetcherKey||tr(),n,a.action||o,{defaultShouldRevalidate:a.defaultShouldRevalidate,preventScrollReset:a.preventScrollReset,formData:l,body:u,formMethod:a.method||s,formEncType:a.encType||c,flushSync:a.flushSync}):await i(a.action||o,{defaultShouldRevalidate:a.defaultShouldRevalidate,preventScrollReset:a.preventScrollReset,formData:l,body:u,formMethod:a.method||s,formEncType:a.encType||c,replace:a.replace,state:a.state,fromRouteId:n,flushSync:a.flushSync,viewTransition:a.viewTransition})},[r,i,t,n])}function rr(e,{relative:t}={}){let{basename:n}=j.useContext(ft),r=j.useContext(mt);D(r,`useFormAction must be used inside a RouteContext`);let[i]=r.matches.slice(-1),a={...Dt(e||`.`,{relative:t})},o=Ct();if(e==null){a.search=o.search;let e=new URLSearchParams(a.search),t=e.getAll(`index`);if(t.some(e=>e===``)){e.delete(`index`),t.filter(e=>e).forEach(t=>e.append(`index`,t));let n=e.toString();a.search=n?`?${n}`:``}}return(!e||e===`.`)&&i.route.index&&(a.search=a.search?a.search.replace(/^\?/,`?index&`):`?index`),n!==`/`&&(a.pathname=a.pathname===`/`?n:Ge([n,a.pathname])),fe(a)}var ir=`react-router-scroll-positions`,ar={};function or(e,t,n,r){let i=null;return r&&(i=r(n===`/`?e:{...e,pathname:Le(e.pathname,n)||e.pathname},t)),i??=e.key,i}function sr({getKey:e,storageKey:t}={}){let{router:n}=Zn(`useScrollRestoration`),{restoreScrollPosition:r,preventScrollReset:i}=Qn(`useScrollRestoration`),{basename:a}=j.useContext(ft),o=Ct(),s=Wt(),c=Ut();j.useEffect(()=>(window.history.scrollRestoration=`manual`,()=>{window.history.scrollRestoration=`auto`}),[]),cr(j.useCallback(()=>{if(c.state===`idle`){let t=or(o,s,a,e);ar[t]=window.scrollY}try{sessionStorage.setItem(t||ir,JSON.stringify(ar))}catch(e){O(!1,`Failed to save scroll positions in sessionStorage, <ScrollRestoration /> will not work properly (${e}).`)}window.history.scrollRestoration=`auto`},[c.state,e,a,o,s,t])),typeof document<`u`&&(j.useLayoutEffect(()=>{try{let e=sessionStorage.getItem(t||ir);e&&(ar=JSON.parse(e))}catch{}},[t]),j.useLayoutEffect(()=>{let t=n?.enableScrollRestoration(ar,()=>window.scrollY,e?(t,n)=>or(t,n,a,e):void 0);return()=>t&&t()},[n,a,e]),j.useLayoutEffect(()=>{if(r!==!1){if(typeof r==`number`){window.scrollTo(0,r);return}try{if(o.hash){let e=document.getElementById(decodeURIComponent(o.hash.slice(1)));if(e){e.scrollIntoView();return}}}catch{O(!1,`"${o.hash.slice(1)}" is not a decodable element ID. The view will not scroll to it.`)}i!==!0&&window.scrollTo(0,0)}},[o,r,i]))}function cr(e,t){let{capture:n}=t||{};j.useEffect(()=>{let t=n==null?void 0:{capture:n};return window.addEventListener(`pagehide`,e,t),()=>{window.removeEventListener(`pagehide`,e,t)}},[e,n])}function lr(e,{relative:t}={}){let n=j.useContext(lt);D(n!=null,"`useViewTransitionState` must be used within `react-router/dom`'s `RouterProvider`.  Did you accidentally import `RouterProvider` from `react-router`?");let{basename:r}=Zn(`useViewTransitionState`),i=Dt(e,{relative:t});if(!n.isTransitioning)return!1;let a=Le(n.currentLocation.pathname,r)||n.currentLocation.pathname,o=Le(n.nextLocation.pathname,r)||n.nextLocation.pathname;return Ne(i.pathname,o)!=null||Ne(i.pathname,a)!=null}var ur=re(),dr={wrapper:`_wrapper_bt1w8_2`,header:`_header_bt1w8_10`,headerActions:`_headerActions_bt1w8_21`,title:`_title_bt1w8_27`,panelGroup:`_panelGroup_bt1w8_36`,clipboardToggle:`_clipboardToggle_bt1w8_43`,helpToggle:`_helpToggle_bt1w8_66`,helpButtonWrapper:`_helpButtonWrapper_bt1w8_93`,helpTogglePulsing:`_helpTogglePulsing_bt1w8_97`,helpPulse:`_helpPulse_bt1w8_1`,helpHint:`_helpHint_bt1w8_112`,helpHintFading:`_helpHintFading_bt1w8_139`,helpHintKbd:`_helpHintKbd_bt1w8_144`,resizeHandle:`_resizeHandle_bt1w8_153`},fr=e=>{try{return!new DOMParser().parseFromString(e.trim(),`text/xml`).querySelector(`parsererror`)}catch{return!1}},pr=e=>{try{return JSON.parse(e),!0}catch{return!1}},mr=e=>e.trim()?pr(e)?{valid:!0,error:null,type:`json`}:fr(e)?{valid:!0,error:null,type:`xml`}:{valid:!1,error:`Invalid JSON/XML format`,type:null}:{valid:!0,error:null,type:null},hr=e=>{try{let t=JSON.parse(e);return JSON.stringify(t,null,2)}catch{return e}},gr=()=>{let[e,t]=(0,j.useState)([]),n=(0,j.useRef)(0),r=(0,j.useRef)(new Set);return(0,j.useEffect)(()=>()=>{r.current.forEach(clearTimeout)},[]),{toasts:e,addToast:(0,j.useCallback)((e,i=`info`)=>{let a=++n.current;t(t=>[...t,{id:a,message:e,type:i}]);let o=setTimeout(()=>{r.current.delete(o),t(e=>e.filter(e=>e.id!==a))},3e3);r.current.add(o)},[]),removeToast:(0,j.useCallback)(e=>{t(t=>t.filter(t=>t.id!==e))},[])}},_r=(e,t)=>{let n=(0,j.useCallback)(()=>{try{let n=window.localStorage.getItem(e);return n?JSON.parse(n):t}catch{return t}},[e]),[r,i]=(0,j.useState)(n);return(0,j.useEffect)(()=>{i(n())},[e]),(0,j.useEffect)(()=>{try{window.localStorage.setItem(e,JSON.stringify(r))}catch(t){console.error(`Error setting localStorage key "${e}":`,t)}},[e,r]),(0,j.useEffect)(()=>{let t=t=>{(t.key===e||t.key===null)&&i(n())};return window.addEventListener(`storage`,t),()=>window.removeEventListener(`storage`,t)},[e,n]),(0,j.useEffect)(()=>{let e=()=>i(n());return window.addEventListener(`focus`,e),document.addEventListener(`visibilitychange`,e),()=>{window.removeEventListener(`focus`,e),document.removeEventListener(`visibilitychange`,e)}},[n]),[r,i]},vr=2e4,yr=[{path:`/json-path`,label:`JSON-Path`,title:`JSON-Path Playground`,wsPath:`/ws/json/path`,storageKeyPayload:`jsonpath-last-payload`,storageKeyHistory:`jsonpath-command-history`,storageKeyTab:`jsonpath-right-tab`,supportsUpload:!0,tabs:[`payload`,`graph`,`graph-data`]},{path:`/`,label:`Minigraph`,title:`Minigraph Playground`,wsPath:`/ws/graph/playground`,storageKeyPayload:`minigraph-last-payload`,storageKeyHistory:`minigraph-command-history`,storageKeyTab:`minigraph-right-tab`,storageKeySavedGraphs:`minigraph-saved-graphs`,storageKeyHelpTopic:`minigraph-help-topic`,supportsClipboard:!0,supportsHelp:!0,supportsAuthoring:!0,supportsSessionCollaboration:!0,tabs:[`graph`,`graph-data`]}],br={json_simple:JSON.stringify({name:`John Doe`,age:30,city:`New York`},null,2),json_nested:JSON.stringify({user:{name:`Jane Smith`,profile:{email:`jane@example.com`,address:{city:`San Francisco`,country:`USA`}}}},null,2),json_array:JSON.stringify([{id:1,name:`Item 1`,status:`active`},{id:2,name:`Item 2`,status:`pending`},{id:3,name:`Item 3`,status:`inactive`}],null,2),xml_simple:`<?xml version="1.0" encoding="UTF-8"?>
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
</items>`};function xr(e){return`ws://${window.location.host}${e}`}var M=b();function Sr(e,t,n,r){let i=e[t]??{phase:`idle`,messages:[]},a=[...i.messages,{id:n,raw:r}];return a.length>200&&a.shift(),{...e,[t]:{...i,messages:a}}}function Cr(e,t){let n=e[t.path]??{phase:`idle`,messages:[]};switch(t.type){case`CONNECTING`:return{...e,[t.path]:{...n,phase:`connecting`}};case`CONNECTED`:return Sr({...e,[t.path]:{...n,phase:`connected`}},t.path,t.id,t.msg);case`MESSAGE_RECEIVED`:return Sr(e,t.path,t.id,t.msg);case`DISCONNECTED`:return Sr({...e,[t.path]:{...n,phase:`idle`}},t.path,t.id,t.msg);case`CONNECT_ERROR`:return{...e,[t.path]:{...n,phase:`idle`}};case`CLEAR_MESSAGES`:return{...e,[t.path]:{...n,messages:[]}};default:return e}}var wr=(0,j.createContext)(null);function Tr({children:e}){let[t,n]=(0,j.useReducer)(Cr,{}),r=(0,j.useRef)({}),i=(0,j.useRef)({}),a=(0,j.useRef)({});(0,j.useEffect)(()=>()=>{Object.entries(r.current).forEach(([e,t])=>{t?.close();let n=i.current[e];n&&clearInterval(n)})},[]);let o=e=>xr(e),s=e=>(a.current[e]=(a.current[e]??0)+1,a.current[e]),c=()=>{let e=new Date().toString(),t=e.indexOf(`GMT`);return t>0?e.substring(0,t).trim():e},l=(e,t)=>JSON.stringify({type:e,message:t,time:c()}),u=e=>{try{let t=JSON.parse(e);if(typeof t==`object`&&t){let e=t.type;return e===`ping`||e===`pong`}}catch{}return!1},d=(0,j.useCallback)((e,t)=>{if(!window.WebSocket){t?.(`WebSocket not supported by your browser`,`error`);return}let a=r.current[e];if(a&&(a.readyState===WebSocket.OPEN||a.readyState===WebSocket.CONNECTING)){t?.(`Already connected`,`error`);return}n({type:`CONNECTING`,path:e});let c=new WebSocket(o(e));r.current[e]=c,c.onopen=()=>{n({type:`CONNECTED`,path:e,id:s(e),msg:l(`info`,`connected`)}),t?.(`Connected to WebSocket`,`success`),c.send(JSON.stringify({type:`welcome`})),i.current[e]=setInterval(()=>{c.readyState===WebSocket.OPEN&&c.send(l(`ping`,`keep alive`))},vr)},c.onmessage=t=>{u(t.data)||n({type:`MESSAGE_RECEIVED`,path:e,id:s(e),msg:t.data})},c.onerror=()=>{n({type:`CONNECT_ERROR`,path:e})},c.onclose=a=>{let o=i.current[e];o&&(clearInterval(o),i.current[e]=null),n({type:`DISCONNECTED`,path:e,id:s(e),msg:l(`info`,`disconnected - (${a.code}) ${a.reason}`)}),t?.(`Disconnected from WebSocket`,`info`),r.current[e]===c&&(r.current[e]=null)}},[]),f=(0,j.useCallback)(e=>{let t=r.current[e];t?t.close():n({type:`MESSAGE_RECEIVED`,path:e,id:s(e),msg:l(`error`,`already disconnected`)})},[]);(0,j.useEffect)(()=>(yr.forEach(e=>{d(e.wsPath)}),()=>{yr.forEach(e=>{let t=r.current[e.wsPath];t&&t.close()})}),[]);let p=(0,j.useCallback)((e,t)=>{let n=r.current[e];return n&&n.readyState===WebSocket.OPEN?(n.send(t),!0):!1},[]),m=(0,j.useCallback)((e,t)=>{n({type:`MESSAGE_RECEIVED`,path:e,id:s(e),msg:t})},[]),h=(0,j.useCallback)(e=>{n({type:`CLEAR_MESSAGES`,path:e})},[]),[g,_]=(0,j.useState)({}),v=(0,j.useCallback)((e,t)=>{_(n=>{if(t===null){let t={...n};return delete t[e],t}return{...n,[e]:t}})},[]),y=(0,j.useCallback)(e=>g[e]??null,[g]),b=(0,j.useCallback)(e=>{let t=g[e]??null;return t!==null&&_(t=>{let n={...t};return delete n[e],n}),t},[g]),x=(0,j.useCallback)(e=>t[e]??{phase:`idle`,messages:[]},[t]),S=(0,j.useMemo)(()=>({getSlot:x,connect:d,disconnect:f,send:p,appendMessage:m,clearMessages:h,setPendingPayload:v,peekPendingPayload:y,takePendingPayload:b}),[x,d,f,p,m,h,v,y,b]);return(0,M.jsx)(wr.Provider,{value:S,children:e})}function Er(){let e=(0,j.useContext)(wr);if(!e)throw Error(`useWebSocketContext must be used inside <WebSocketProvider>`);return e}var Dr=e=>{try{let t=JSON.parse(e);return{type:t.type||`info`,message:t.message||e,time:t.time,raw:e}}catch{return{type:`raw`,message:e,time:null,raw:e}}},Or=e=>({info:`ℹ️`,error:`❌`,ping:`🔄`,welcome:`👋`,raw:``})[e]??`•`,kr=e=>{try{let t=JSON.parse(e);if(typeof t==`object`&&t)return{isJSON:!0,data:t}}catch{}return{isJSON:!1,data:null}};function Ar(e){if(!e.includes(`Graph exported to `))return null;let t=Nr(e);if(!t)return null;let n=t.split(`/`)[4];return n?{graphName:n,apiPath:t}:null}function jr(e){return e.includes(`Invalid filename`)?{reason:`invalid-name`}:e.includes(`Expect root node name`)?{reason:`root-name-conflict`}:null}function Mr(e){let t=kr(e);return t.isJSON?(t.data.type,!1):!0}function Nr(e){let t=e.match(/\/api\/graph\/model\/([^\s'"]+)/);return t?t[0]:null}function Pr(e){return Mr(e)?Nr(e)!==null:!1}function Fr(e){let t=e.match(/\/api\/json\/content\/([\w-]+)/);return t?t[0]:null}function Ir(e){let t=e.match(/Large payload \((\d+)\)\s*->\s*GET\s+(\/api\/inspect\/[^\s]+)/i);if(!t)return null;let n=parseInt(t[1],10),r=t[2];return{apiPath:r,byteSize:n,filename:`${r.split(`/`).filter(Boolean).pop()??`payload`}.json`}}function Lr(e){let t=e.match(/You may upload .*?->\s*POST\s+(\/api\/mock\/[\w-]+)/i);return t?t[1]:null}function Rr(e){if(!e.startsWith(`> `))return!1;let t=e.slice(2).trim().toLowerCase();return t===`help`||t.startsWith(`help `)?!0:t.startsWith(`describe `)?!t.slice(9).trim().startsWith(`graph`):!1}function zr(e){if(!e.startsWith(`> `)||!e.slice(2).trimStart().toLowerCase().startsWith(`import graph from `))return null;let t=e.slice(2).trimStart().slice(18).trim();return t.length>0?t:null}var Br=/^node ([A-Za-z0-9_-]+) created$/i,Vr=/^node ([A-Za-z0-9_-]+) already exists$/i,Hr=/^node ([A-Za-z0-9_-]+) updated$/i,Ur=/^node ([A-Za-z0-9_-]+) deleted$/i,Wr=/^node ([A-Za-z0-9_-]+) not found$/i,Gr=/^ERROR: (.+)$/;function Kr(e){let t=e.trim();if(t.startsWith(`> `))return null;let n=t.match(Br);if(n)return{status:`accepted`,action:`create-node`,alias:n[1],message:t};let r=t.match(Vr);if(r)return{status:`rejected`,action:`create-node`,alias:r[1],message:t};let i=t.match(Hr);if(i)return{status:`accepted`,action:`edit-node`,alias:i[1],message:t};let a=t.match(Ur);if(a)return{status:`accepted`,action:`delete-node`,alias:a[1],message:t};let o=t.match(Wr);return o?{status:`rejected`,action:null,alias:o[1],message:t}:t.match(Gr)?{status:`error`,action:null,alias:null,message:t}:null}function qr(e){if(!Mr(e)||e.startsWith(`> `)||Pr(e))return null;let t=e.toLowerCase();return t.includes(`graph model imported as draft`)?`import-graph`:t.includes(` -> `)&&t.includes(`removed`)||t.startsWith(`node `)&&(t.includes(` created`)||t.includes(` updated`)||t.includes(` deleted`)||t.includes(` connected to `)||t.includes(` imported from `)||t.includes(` overwritten by node from `))?`node-mutation`:null}var Jr={command:``,historyIndex:-1,draftCommand:``};function Yr(e,t){switch(t.type){case`SET_COMMAND`:return{...e,command:t.value,historyIndex:-1,draftCommand:``};case`CLEAR_COMMAND`:return{...e,command:``,historyIndex:-1,draftCommand:``};case`SET_HISTORY_INDEX`:return{...e,historyIndex:t.index,command:t.command};case`ENTER_HISTORY`:return{...e,historyIndex:0,command:t.command,draftCommand:e.command};case`EXIT_HISTORY`:return{...e,historyIndex:-1,command:e.draftCommand,draftCommand:``};default:return e}}function Xr({wsPath:e,storageKeyHistory:t,payload:n,addToast:r,bus:i,handleLocalCommand:a}){let o=Er(),{phase:s,messages:c}=o.getSlot(e),l=s===`connected`,u=s===`connecting`,[d,f]=(0,j.useReducer)(Yr,Jr),{command:p,historyIndex:m}=d,[h,g]=_r(t,[]),_=(0,j.useRef)(null),v=(0,j.useRef)(!1);(0,j.useEffect)(()=>{_.current&&(_.current.scrollTop=_.current.scrollHeight)},[c]);let y=(0,j.useCallback)(()=>{o.connect(e,r)},[o,e,r]),b=(0,j.useCallback)(()=>{o.disconnect(e)},[o,e]),x=(0,j.useCallback)(()=>{if(s!==`connected`)return;let t=p.trim();if(t.length!==0){if(a?.(t)===!0){h[0]!==t&&g(e=>[t,...e].slice(0,50)),o.appendMessage(e,`> `+t),f({type:`CLEAR_COMMAND`});return}o.send(e,t),h[0]!==t&&g(e=>[t,...e].slice(0,50)),t===`load`&&(n.length===0?o.appendMessage(e,`ERROR: please paste JSON/XML payload in input text area`):o.send(e,n)),f({type:`CLEAR_COMMAND`})}},[o,e,s,p,n,h,g,a]),S=(0,j.useCallback)(e=>{if(e.key===`ArrowUp`){if(e.preventDefault(),h.length===0)return;if(m===-1)f({type:`ENTER_HISTORY`,command:h[0]});else if(m<h.length-1){let e=m+1;f({type:`SET_HISTORY_INDEX`,index:e,command:h[e]})}}else if(e.key===`ArrowDown`)if(e.preventDefault(),m<=0)m===0&&f({type:`EXIT_HISTORY`});else{let e=m-1;f({type:`SET_HISTORY_INDEX`,index:e,command:h[e]})}},[h,m]);(0,j.useEffect)(()=>{if(i)return i.on(`upload.contentPath`,t=>{if(!v.current)return;if(v.current=!1,n.length===0){o.appendMessage(e,`ERROR: please paste JSON/XML payload in the input text area`);return}let i;try{i=JSON.stringify(JSON.parse(n))}catch{o.appendMessage(e,`ERROR: payload is not valid JSON — cannot upload`);return}fetch(t.uploadPath,{method:`POST`,headers:{"Content-Type":`application/json`},body:i}).then(e=>{if(!e.ok)throw Error(`HTTP ${e.status}`);r(`Payload uploaded successfully`,`success`)}).catch(t=>{o.appendMessage(e,`ERROR: upload failed — ${t.message}`),r(`Upload failed: ${t.message}`,`error`)})})},[i,n,e,o,r]),(0,j.useEffect)(()=>{if(i||!v.current||c.length===0)return;let t=c[c.length-1].raw,a=Fr(t);if(!a)return;if(v.current=!1,n.length===0){o.appendMessage(e,`ERROR: please paste JSON/XML payload in the input text area`);return}let s;try{s=JSON.stringify(JSON.parse(n))}catch{o.appendMessage(e,`ERROR: payload is not valid JSON — cannot upload`);return}fetch(a,{method:`POST`,headers:{"Content-Type":`application/json`},body:s}).then(e=>{if(!e.ok)throw Error(`HTTP ${e.status}`);r(`Payload uploaded successfully`,`success`)}).catch(t=>{o.appendMessage(e,`ERROR: upload failed — ${t.message}`),r(`Upload failed: ${t.message}`,`error`)})},[i,c,n,e,o,r]);let C=(0,j.useCallback)(()=>{if(s===`connected`){if(n.length===0){r(`Nothing to upload — paste a JSON payload first`,`error`);return}v.current=!0,o.send(e,`upload`)}},[o,e,s,n,r]),ee=(0,j.useCallback)(t=>s===`connected`&&o.send(e,t),[o,e,s]),te=(0,j.useCallback)(()=>{navigator.clipboard.writeText(c.map(e=>e.raw).join(`
`)),r(`Console copied to clipboard!`,`success`)},[c,r]),w=(0,j.useCallback)(()=>{o.clearMessages(e),r(`Console cleared`,`info`)},[o,e,r]),ne=(0,j.useCallback)(t=>{o.appendMessage(e,t)},[o,e]);return{connected:l,connecting:u,messages:c,command:p,setCommand:(0,j.useCallback)(e=>f({type:`SET_COMMAND`,value:e}),[]),connect:y,disconnect:b,sendCommand:x,handleKeyDown:S,consoleRef:_,copyMessages:te,clearMessages:w,uploadPayload:C,sendRawText:ee,appendMessage:ne,history:h}}function Zr(e){let[t,n]=(0,j.useState)(()=>window.matchMedia(e).matches);return(0,j.useEffect)(()=>{let t=window.matchMedia(e),r=e=>n(e.matches);return t.addEventListener(`change`,r),()=>t.removeEventListener(`change`,r)},[e]),t}function Qr(e){if(typeof e!=`object`||!e)return!1;let t=e;return Array.isArray(t.nodes)}function $r(e,t,n){let r=t.includes(n)?n:t[0]??`graph`;return typeof e==`string`&&t.includes(e)?e:r}function ei(e,t,n,r,i){let[a,o]=(0,j.useState)(null),[s,c]=_r(i,n),l=$r(s,r,n),[u,d]=(0,j.useState)(!1),f=(0,j.useCallback)(e=>{c(t=>{let i=$r(t,r,n);return $r(typeof e==`function`?e(i):e,r,n)})},[c,r,n]);(0,j.useEffect)(()=>{s!==l&&c(l)},[s,l,c]);let p=(0,j.useRef)(e);(0,j.useEffect)(()=>{p.current=e},[e]);let m=(0,j.useRef)(null);(0,j.useEffect)(()=>{if(!e){o(null);return}let n=new AbortController;return o(null),fetch(e,{signal:n.signal}).then(e=>{if(!e.ok)throw Error(`HTTP ${e.status}`);return e.json()}).then(e=>{Qr(e)&&(o(e),f(`graph`))}).catch(e=>{e.name!==`AbortError`&&t(`Graph fetch failed: ${e.message}`,`error`)}),()=>{n.abort()}},[e,t]);let h=(0,j.useCallback)(()=>{let e=p.current;if(!e)return;m.current?.abort();let n=new AbortController;m.current=n,d(!0),fetch(e,{signal:n.signal}).then(e=>{if(!e.ok)throw Error(`HTTP ${e.status}`);return e.json()}).then(e=>{Qr(e)&&o(e),d(!1)}).catch(e=>{e.name!==`AbortError`&&(t(`Graph refresh failed: ${e.message}`,`error`),d(!1))})},[]);return(0,j.useEffect)(()=>()=>{m.current?.abort()},[]),{graphData:a,setGraphData:o,rightTab:l,setRightTab:f,isRefreshing:u,refetchGraph:h}}function ti({bus:e,pinnedGraphPath:t,setPinnedGraphPath:n,connected:r,sendRawText:i,addToast:a}){let o=(0,j.useRef)(null),s=(0,j.useRef)(!1),c=(0,j.useRef)(t),l=(0,j.useRef)(r),u=(0,j.useRef)(i);(0,j.useEffect)(()=>{c.current=t},[t]),(0,j.useEffect)(()=>{l.current=r},[r]),(0,j.useEffect)(()=>{u.current=i},[i]),(0,j.useEffect)(()=>{r||(s.current=!1,o.current!==null&&(clearTimeout(o.current),o.current=null))},[r]),(0,j.useEffect)(()=>e.on(`graph.link`,e=>{s.current&&(s.current=!1,n(e.apiPath))}),[e,n]),(0,j.useEffect)(()=>e.on(`graph.mutation`,e=>{if(l.current){if(e.mutationType===`import-graph`){o.current!==null&&(clearTimeout(o.current),o.current=null),s.current=!0,u.current(`describe graph`),a(`Graph imported — refreshing view…`,`info`);return}s.current=!0,o.current!==null&&clearTimeout(o.current),o.current=setTimeout(()=>{o.current=null,l.current&&(s.current=!0,u.current(`describe graph`),a(c.current===null?`Graph updated — opening Graph tab…`:`Graph updated — refreshing…`,`info`))},300)}}),[e,a]),(0,j.useEffect)(()=>e.on(`session.reset`,()=>{o.current!==null&&(clearTimeout(o.current),o.current=null),s.current=!1,n(null)}),[e,n]),(0,j.useEffect)(()=>()=>{o.current!==null&&clearTimeout(o.current)},[])}var ni=Object.assign({"../../../src/main/resources/help/help connect.md":`Connect two nodes together
--------------------------
1. Each connection is directional. Connect A to B is different from B to A.
2. A node must connect to one or more nodes. When a graph has orphan nodes, you cannot export the graph for deployment.

Syntax
------
\`\`\`
connect {node-A} to {node-B} with {relation}
\`\`\`
`,"../../../src/main/resources/help/help create.md":`Create a new node
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
`,"../../../src/main/resources/help/help data-dictionary.md":`Data Dictionary
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
`,"../../../src/main/resources/help/help delete.md":`Delete a node, a connection or clear cache
------------------------------------------

Syntax
------
Delete a node
-------------
\`\`\`
delete node {name}
\`\`\`

Delete the connections between two nodes if any
-----------------------------------------------
\`\`\`
delete connection {nodeA} and {nodeB}
\`\`\`

Clear cache for API fetchers
----------------------------
\`\`\`
clear cache
\`\`\`

Alias
-----
\`clear\` is an alias of \`delete\`
`,"../../../src/main/resources/help/help describe.md":`Describe graph, node, connection or skill
-----------------------------------------

Syntax
------
Show the structure of the current graph model
---------------------------------------------
\`\`\`
describe graph
\`\`\`

Print the structure of a node
-----------------------------
\`\`\`
describe node {name}
\`\`\`

Confirm if there is a connection between node-A and node-B
----------------------------------------------------------
\`\`\`
describe connection {node-A} and {node-B}
\`\`\`

Skill description of a specific composable function
---------------------------------------------------
\`\`\`
describe skill {skill.route.name}
\`\`\`
`,"../../../src/main/resources/help/help edit.md":`Edit a node
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
`,"../../../src/main/resources/help/help execute.md":`Execute a node with a skill
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
`,"../../../src/main/resources/help/help export.md":`Export a graph model
--------------------
1. This command exports a graph as a model in JSON format for deployment
2. The name does not require the ".json" extension

Syntax
------
\`\`\`
export graph as {name}
\`\`\`
`,"../../../src/main/resources/help/help graph-api-fetcher.md":`Skill: Graph API Fetcher
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

Deprecated syntax
-----------------
Event Script's "simple type matching" syntax (e.g. \`model.someKey:text\`) is deprecated. Use "simple plugin"
syntax instead (e.g. \`f:text(model.someKey)\`). If you (or an AI agent) submit a "create node" or "update node"
command that still uses the deprecated colon-type syntax, the system will automatically convert it to the
simple plugin syntax and return a deprecation notice - it will not silently fail, but please switch to the
new syntax going forward.

Custom error handling
---------------------
By default, when an API request fails, the system will abort the graph execution and return the error code
and message to the caller.

If you want to handle the exception in your graph model, you can set the node-name of the error-handler in
the "exception" property to tell the system to traverse to the error-handler node.

On a failed call (HTTP status >= 400):

- {node}.status and {node}.error are set (the engine's error record)
- the output[] mappings are SKIPPED
- with exception={handler-node}, traversal JUMPS to the handler; without
  it, the run ABORTS and the error is returned to the caller.

To handle an exception, the error-handler node should be a decision-making node using the graph.math or graph.js skill.
It can evaluate the status code and error in the API fetcher node to determine the next step. The
canonical bounded-retry handler:

\`\`\`
create node error-handler
with type Decision
with properties
skill=graph.math
statement[]=RESET: fetcher, error-handler
statement[]=MAPPING: f:defaultValue(model.attempts, int(0)) -> model.attempts
statement[]=MAPPING: f:add(model.attempts, int(1)) -> model.attempts
statement[]='''
IF: {model.attempts} >= 3
THEN: recovery-node
ELSE: next
'''
statement[]=NEXT: fetcher
statement[]=DELAY: 50
\`\`\`

RESET comes first among the action statements so it runs on every path (a taken IF jump ends the
statement list) - the attempt counters live in the "model" namespace, which RESET never touches.
If the handler also carries a defensive check on the failed node's status, that check must come
BEFORE the RESET (it reads state the reset wipes). Wire the handler back explicitly (connect
error-handler to fetcher with retry) - no node left unconnected.

HTTP semantics
--------------
- One data-provider call is exactly one HTTP request - redirects are never followed. A 3xx answer
  is a non-failure: its status and body are captured and traversal proceeds (only >= 400 triggers
  the exception route). Point the provider url at the redirect target to land on it.
- {node}.status always carries the HTTP status of the fetch, success included (a 200 or a 301 is
  readable there, not just failures). The response.* namespace in a dictionary output[] addresses
  the BODY only; the bare root (response -> result.page) captures a whole non-JSON body such as
  an HTML page.
- Deduplication: identical requests (same provider + same input values) within one graph instance
  are deduplicated into a single HTTP call. Only SUCCESSFUL responses are cached - a failed call
  is never cached, so a retry after RESET makes a real call, while an identical successful call
  reuses the cached response.

Caution
-------
API fetchers can be chained together to make multiple API calls. 
However, you should design the API chain to be minimalist.

An overly complex chain of API requests would mean slow performance. Just take the minimal set of data that are
required by your application. Don't abuse the flexibility of the API fetcher.
`,"../../../src/main/resources/help/help graph-data-mapper.md":`Skill: Graph Data Mapper
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

Deprecated syntax
-----------------
Event Script's "simple type matching" syntax (e.g. \`model.someKey:text\`) is deprecated. Use "simple plugin"
syntax instead (e.g. \`f:text(model.someKey)\`). If you (or an AI agent) submit a "create node" or "update node"
command that still uses the deprecated colon-type syntax, the system will automatically convert it to the
simple plugin syntax and return a deprecation notice - it will not silently fail, but please switch to the
new syntax going forward.
`,"../../../src/main/resources/help/help graph-extension.md":`Skill: Graph Extension
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
`,"../../../src/main/resources/help/help graph-island.md":`Skill: Graph Island
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
`,"../../../src/main/resources/help/help graph-join.md":`Skill: Graph Join
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
`,"../../../src/main/resources/help/help graph-js.md":`Skill: Graph JS
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
`,"../../../src/main/resources/help/help graph-math.md":`Skill: Graph Math
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
`,"../../../src/main/resources/help/help graph-resume.md":`Skill: Graph Resume
-------------------
When a graph run starts with the same business correlation ID as a previously suspended
transaction, the node with this skill restores the persisted workflow state and continues
traversal from the recorded suspension point - without re-executing it.

This skill is a superset of "graph.task": the "task" property names the pluggable store
function, but restoration is encapsulated by the skill, so the node needs no input or
output data mapping.

Place the resume node early in the traversal - conventionally named "resume" and connected
right after "root", or after nodes that perform setup and initialization. When the store
has a record for the business correlation ID (model.cid), the skill merges the persisted
model key-values into the state machine (the current run's reserved keys such as model.cid
and model.instance always win), restores the traversal bookkeeping so downstream join
barriers still see branches completed before suspension, and jumps past the suspension
point onto its normal forward path.

When there is no record - a fresh transaction, which is the normal first-run case, or an
expired record - traversal simply continues along the resume node's own forward path.

The default store behavior consumes the record on retrieval, so a duplicate resume request
cannot execute the continuation twice.

Route name
----------
"graph.resume"

Setup
-----
To enable this skill, set "skill=graph.resume" as a property in a node.

The following parameter is required in the properties of the node:

1. task - the route name of the state-store function (e.g. "v1.redis.retrieve.model")

Optional parameter:

2. missing - the alias of a node to jump to when no record exists, for workflows where an
   absent record needs distinct handling (e.g. an expired-approval response)

The store function receives headers "type=get" and a body of {"cid": "..."} and returns
the persisted record, or nothing (null or an empty map) when absent or expired.

Example
-------
create node resume
with type Resume
with properties
purpose=Restore workflow state from the external state store
skill=graph.resume
task=v1.redis.retrieve.model
`,"../../../src/main/resources/help/help graph-suspend.md":`Skill: Graph Suspend
--------------------
When a graph reaches the node with this skill, the workflow state of the graph instance is
persisted to an external state store and the graph run completes normally - the transaction
can resume later through the "graph.resume" skill using the same business correlation ID.

This skill is a superset of "graph.task": the "task" property names the pluggable store
function, but the persistence envelope is assembled by the skill itself, so the node needs
no input or output data mapping.

The node carrying this skill MUST be named "suspend" - a reserved alias like "root" and
"end" - because graph traversal jumps to it by name: when a node with the "suspend=true"
property completes normally, the walker routes to the "suspend" node instead of the node's
normal forward path. A plain connection into the "suspend" node is an unconditional
suspension point. There is exactly one suspend node per graph.

A suspension point must be the sole active branch - do not suspend between a fan-out and
its join; suspend after the join instead. Anything a later step needs must be mapped into
the "model" namespace before the suspension point, because a node's transient "result"
properties do not survive suspension - the model is the workflow's durable memory.

Unless the graph staged its own output before suspension, the skill stages a default
response body so the caller of the suspended run receives a meaningful reply:

{
  "type": "suspended",
  "cid": "<business correlation ID>"
}

Route name
----------
"graph.suspend"

Setup
-----
To enable this skill, create a node named "suspend" with "skill=graph.suspend".

The following parameters are required in the properties of the node:

1. task - the route name of the state-store function (e.g. "v1.redis.persist.model")
2. ttl - the record's time-to-live using duration syntax, e.g. 20s, 5m, 2h, 2d

The store function receives headers "type=put" and a body of:

{
  "cid":   "<business correlation ID - the retrieval key>",
  "node":  "<the suspension point - the node that routed here>",
  "ttl":   <seconds>,
  "model": { the model namespace minus the per-run reserved keys },
  "seen":  { traversal bookkeeping },
  "run":   { traversal bookkeeping }
}

The store must acknowledge with a 2xx reply before the graph completes - a failed store
call fails the node (the optional "exception" property routes it to a handler node).

Example
-------
create node suspend
with type Suspend
with properties
purpose=Persist workflow state to the external state store
skill=graph.suspend
task=v1.redis.persist.model
ttl=2d
`,"../../../src/main/resources/help/help graph-task.md":`Skill: Graph Task
-----------------
When a node is configured with this skill of "graph task", it will invoke a composable function
through its route name and collect the function's response into the "result" property of the node.
In case of exception, the "status" and "error" fields will be set to the node's properties and the
graph execution will stop unless an exception handler node is configured.

A composable function is a TypedLambdaFunction registered using the PreLoad annotation. This provides
a lightweight method to extend a knowledge graph's capability with a small piece of business logic,
without writing a new skill - more complex business logic should be delegated to a flow extension
or a subgraph using the "graph.extension" skill.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.task"

Setup
-----
To enable this skill for a node, set "skill=graph.task" as a property in a node.

The following parameters are required in the properties of the node:

1. task - the route name of the composable function to invoke
2. input - one or more data mapping entries as input to the composable function

The system uses the same syntax of Event Script for data mapping.

Properties
----------
\`\`\`
skill=graph.task
task=route.name.of.composable.function
input[]={mapping of key-values from input, model or another node to the function's request}
output[]={optional mapping of result set to one or more variables in the 'model.' or 'output.' namespace}
\`\`\`

Optional properties
-------------------
\`\`\`
for_each[]={map an array parameter for iterative function execution}
concurrency={controls parallel function calls for an "iterative task request". Default 3, max 30}
exception={error-handler-node-name}
\`\`\`

Input data mapping
------------------
source.composite.key -> target

The source (LHS) can use a key-value from the \`input.\` namespace, the \`model.\` namespace, another
node or a constant such as text(hello). The target (RHS) addresses the function's request:

1. \`*\` - the LHS value becomes the whole request body (same as Event Script). Data mapping entries
   are processed in order, so later entries can merge additional key-values into a request body
   that was seeded with \`*\`.
2. \`header.{name}\` - sets a request header of the function call
3. any other composite key - a key-value in the request body

Example:
\`\`\`
input[]=input.body -> *
input[]=input.header.hello -> header.hello
input[]=input.body.amount -> amount
\`\`\`

If the function is declared as a TypedLambdaFunction with a PoJo input class, the request body map
is automatically converted to the PoJo at the function boundary.

Result set
----------
Upon successful execution, the function's response body is stored in the "result" parameter, the
response status in "status" and the response headers in "header" in the properties of the node.
The optional output data mapping can copy them to the 'model.' or 'output.' namespace.

Example:
\`\`\`
output[]=result -> model.soap_request_payload
\`\`\`

Timeout
-------
The function call uses the graph instance's time-to-live from "model.ttl" (default 30000 ms).

Exception handling
------------------
If the function throws an exception (e.g. AppException with a status code) or the call times out,
the "error" and "status" parameters of the node are set. When the node has an "exception" property,
the graph jumps to that error handler node. Otherwise, the error is returned as the graph output.

Example
-------
\`\`\`
create node prepare-soap-request
with type Task
with properties
task=v1.prepare.soap.request
input[]=input.body -> *
input[]=input.header.hello -> header.hello
output[]=result -> model.soap_request_payload
skill=graph.task
\`\`\`
`,"../../../src/main/resources/help/help import.md":`Import a graph model
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
`,"../../../src/main/resources/help/help inspect.md":`Inspect state machine
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
\`{variable_name}\` is a placeholder — substitute your key and do **not** type the
braces (see the examples). A whole namespace (\`input\` | \`output\` | \`model\`) is
also valid, e.g. \`inspect output\`.

Examples
--------
\`\`\`
inspect input.body.user_id
inspect book.price
inspect model.some_variable
inspect output.body.some_key
\`\`\`
`,"../../../src/main/resources/help/help instantiate.md":`Instantiate from a Graph Model
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
`,"../../../src/main/resources/help/help list.md":`List nodes, connections, graphs or flows
----------------------------------------
The "list nodes" and "list connections" commands list all the nodes and connections of the current graph model
respectively. The "list graphs" and "list flows" commands are read-only DISCOVERY commands: they enumerate the
deployable graph models (each with its root node's "purpose" - living documentation) and the Event Script flows -
the valid extension={graph-id} and extension=flow://{flow-id} delegation targets.

Syntax
------
List all nodes
--------------
\`\`\`
list nodes
\`\`\`

List all connections
--------------------
\`\`\`
list connections
\`\`\`

List deployable graph models (discovery)
----------------------------------------
\`\`\`
list graphs
\`\`\`

List Event Script flows (discovery)
-----------------------------------
\`\`\`
list flows
\`\`\`
`,"../../../src/main/resources/help/help run.md":`Run a graph instance
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
`,"../../../src/main/resources/help/help seen.md":`Display nodes that have been 'seen'
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
`,"../../../src/main/resources/help/help session.md":`Session commands
----------------
The session commands are used for user collaboration.

1. Display current session 
2. Subscribe to a session for collaboration with another user
3. Reset the current session
4. Unsubscribe from another session

Syntax
------

Display current session
-----------------------
\`\`\`
session
\`\`\`

For example, when your session is subscribed by another user.
\`\`\`
> session
Session ws-178443-2 started since 2026-06-02 10:20:32.054
subscribed by [ws-485844-4]
\`\`\`

Subscribe to another session
----------------------------
\`\`\`
session subscribe {session-id}
\`\`\`

e.g.
\`\`\`
> session subscribe ws-178443-2
Subscribed to ws-178443-2
\`\`\`

When you subscribe to a session, input commands from you and the other user
will be executed in both the sessions, thus syncing the action and content
of the graph sessions.

If the target session is not a primary session, you will see this error.

\`\`\`
> session subscribe ws-485844-4
ws-485844-4 is not a primary session
\`\`\`

The system will also reject your subscription request if you try to subscribe
to yourself.

Reset as a new session
----------------------
\`\`\`
session reset
\`\`\`

e.g.
\`\`\`
> session reset
Session restarted
\`\`\`

When you reset a session and you are the primary session, all subscribers will be disconnected.
Your session will be cleared but the previous subscribers would retain their own graphs so they can
continue updating them.

Unsubscribe
-----------
\`\`\`
session unsubscribe
\`\`\`

e.g.
\`\`\`
> session unsubscribe
Session unsubscribed from ws-287159-4
\`\`\`

If you have subscribed to another session, the "unsubscribe" command decouples your session from it.
The graph in your session is retained so that you can continue editing.

If you are the primary session, the system will reject your "unsubscribe" command with an error message
"Nothing to unsubscribe".
`,"../../../src/main/resources/help/help tutorial 1.md":`Tutorial 1
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
`,"../../../src/main/resources/help/help tutorial 10.md":`Tutorial 10
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
`,"../../../src/main/resources/help/help tutorial 11.md":`Tutorial 11
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
`,"../../../src/main/resources/help/help tutorial 12.md":`Tutorial 10
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
`,"../../../src/main/resources/help/help tutorial 13.md":`Tutorial 13
-----------
In this session, you will create a graph model that invokes a composable function using the
"graph.task" skill.

Pre-requisite
-------------
You would need some working knowledge of composable functions. A composable function is a
TypedLambdaFunction registered with the PreLoad annotation. For more details, please refer to the
[Developer Guide](https://accenture.github.io/mercury-composable/).

What is a task?
---------------
A task is a node that invokes a composable function through its route name. MiniGraph is designed to be
zero-code with built-in skills for data mapping, decision-making and API fetching. More complex business
logic is delegated to a flow extension or a subgraph (tutorials 10 and 11). A task node sits in between -
it provides a lightweight method to extend a knowledge graph's capability with a small piece of business
logic, without writing a new skill.

Create the graph model
----------------------
Create the root node:

\`\`\`
create node root
with type Root
with properties
name=tutorial-13
purpose=Demonstrate the graph.task skill - invoking a composable function through its route name
\`\`\`

Create the task node. The "task" property is the route name of the composable function:

\`\`\`
create node hello-task
with type Task
with properties
task=v1.hello.task
input[]=input.body -> *
input[]=text(minigraph) -> header.x-app
output[]=result -> output.body
skill=graph.task
\`\`\`

Create the end node and connect the three nodes:

\`\`\`
create node end
with type End
\`\`\`

\`\`\`
connect root to hello-task with run
connect hello-task to end with finish
\`\`\`

For your convenience, this graph model is also preloaded. You can import it with
'import graph from tutorial-13' instead of creating the nodes manually.

About the input data mapping
----------------------------
The input data mapping follows the Event Script syntax and is applied in declaration order:

1. \`input.body -> *\` maps the whole request body as the request body of the composable function.
   Since data mapping entries are processed in order, later entries can merge additional key-values
   into a request body that was seeded with \`*\`.
2. \`text(minigraph) -> header.x-app\` sets a request header of the function call. You can also map
   individual fields, e.g. \`input.body.amount -> amount\` would set one key-value in the request body.

If the composable function is declared with a PoJo input class, the request body map is automatically
converted to the PoJo at the function boundary.

About v1.hello.task
-------------------
For your convenience, the composable function "v1.hello.task" is preloaded in dev mode. It composes a
greeting from the "name" field, doubles the "amount" field and echoes the "x-app" request header.
Below is an extract of the function:

\`\`\`java
@PreLoad(route = "v1.hello.task", instances = 50)
public class HelloTask implements TypedLambdaFunction<Map<String, Object>, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var result = new HashMap<String, Object>();
        result.put("greeting", "Hello, " + input.getOrDefault("name", "stranger"));
        if (input.get("amount") instanceof Number n) {
            result.put("doubled", n.doubleValue() * 2);
        }
        if (headers.containsKey("x-app")) {
            result.put("app", headers.get("x-app"));
        }
        return result;
    }
}
\`\`\`

Perform a dry-run
-----------------
To test the graph model, you can instantiate the graph with mock input as follows:

\`\`\`
instantiate graph
text(world) -> input.body.name
int(21) -> input.body.amount
\`\`\`

Then enter 'run' to execute the graph.

\`\`\`
> start graph...
Graph instance created. Loaded 2 mock entries, model.ttl = 30000 ms
> run
Walk to root
Walk to hello-task
Executed hello-task with skill graph.task in 4.12 ms
Walk to end
{
  "output": {
    "body": {
      "greeting": "Hello, world",
      "doubled": 42.0,
      "app": "minigraph"
    }
  }
}
Graph traversal completed in 6 ms
\`\`\`

You can also check the application log. Telemetry and tracing information are shown, proving that the
composable function was executed by the graph instance with full trace propagation.

\`\`\`
GraphTask:144 - Call task v1.hello.task, ttl=30000
Telemetry:81 - {trace={path=/graph/playground, service=graph.task...
Telemetry:81 - {trace={path=/graph/playground, service=v1.hello.task...
\`\`\`

Error handling
--------------
If the composable function throws an exception (e.g. AppException with a status code) or the call times
out, the "error" and "status" parameters of the node are set. You can add an "exception" property to the
task node to route the error to a handler node, e.g. \`exception=on-error\`.

Iterative execution
-------------------
Like the API fetcher and the flow extension, a task node supports iterative fork-join execution with the
"for_each" and "concurrency" properties. Please enter 'describe skill graph.task' for details.

Export the graph model
----------------------
Now you may save the graph model by exporting it.

\`\`\`
> export graph as tutorial-13
Graph exported to /tmp/graph/tutorial-13.json
Described in /api/graph/model/tutorial-13/431-3
\`\`\`

Deploy the graph model
----------------------
To deploy the graph model, copy "/tmp/graph/tutorial-13.json" to your application's \`main/resources/graph\`
folder. You can then test the deployed model with a curl command.

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-13 \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "world",
    "amount": 21
}'
\`\`\`

Summary
-------
In this session, we have discussed the use of the "graph.task" skill to invoke a composable function
through its route name, with Event Script style input and output data mapping.

Why invoke a composable function from a graph?
----------------------------------------------
The built-in skills cover data mapping, decision-making, computation and API fetching without writing
any code, and flow extensions or subgraphs handle complex orchestration. A task node completes the
picture - any custom business logic can now be packaged as a composable function and plugged into a
graph as if it were a custom skill. This means you can extend a knowledge graph's capability with the
full power of the Mercury Composable programming model, one small function at a time.
`,"../../../src/main/resources/help/help tutorial 14.md":`Tutorial 14
-----------
In this session, you will build a purchase workflow with THREE human checkpoints - a customer
orders, the store manager approves, the delivery department releases the shipment, and the
parcel ships to the customer. One graph model, four short runs, one correlation ID.

Pre-requisite
-------------
Workflow suspension persists state to an external store through two composable functions. This
tutorial uses the Redis store from the "minigraph-state-redis" extension - the playground
application already includes it, so "v1.redis.persist.model" and "v1.redis.retrieve.model" are
registered automatically. Start a Redis before you run the graph (the "redis-standalone" helper
application works out of the box).

What is workflow suspension?
----------------------------
An approval may take minutes or days. Instead of parking a live graph instance, the graph
persists its workflow state - the "model" namespace - under the business correlation ID and the
run completes normally. A later request with the same correlation ID restores that state and
continues past the checkpoint without re-executing it. Three vocabulary pieces make this work:

1. the "suspend" node - a reserved node name (like root and end) with the "graph.suspend" skill;
   traversal jumps to it by name. ONE suspend node serves every checkpoint in the graph.
2. a suspensible node - any skilled node with the "suspend=true" property; it routes to the
   suspend node after its skill completes
3. the resume node - the "graph.resume" skill placed right after root; it restores a persisted
   record and jumps past the LAST checkpoint, or lets a fresh transaction flow through (the
   optional "missing" property names a node to handle the no-record case)

The graph navigation is:

\`\`\`
root -> resume -> order (suspend=true) -> approval (suspend=true) -> delivery (suspend=true) -> ship -> end
\`\`\`

Each suspensible node captures its actor's input into the model and suspends; each following
run resumes one checkpoint further. The model is the workflow's durable memory - anything a
later step needs must be mapped into "model.*" before the checkpoint.

Create the graph model
----------------------
Create the root node:

\`\`\`
create node root
with properties
purpose=Purchase workflow with three human checkpoints
name=tutorial-14
\`\`\`

Create the resume node. A fresh transaction (no suspended record) jumps to the "check-fresh"
validation gate:

\`\`\`
create node resume
with type Resume
with properties
purpose=Restore workflow state if this transaction was suspended earlier
skill=graph.resume
task=v1.redis.retrieve.model
missing=check-fresh
\`\`\`

Create the input validation gate. The variable substitution inside the text() constant is
null-safe: when the request has no "item" field it is not an order submission, so a later-stage
request without a suspended record is rejected:

\`\`\`
create node check-fresh
with type Decision
with properties
purpose=A fresh transaction must be an order submission
skill=graph.math
statement[]=MAPPING: text(={input.body.item}) -> model.order_probe
statement[]=IF: {model.order_probe} == '=null'
THEN: reject
ELSE: order
\`\`\`

Create the three checkpoint nodes. Each captures its actor's input into the model, stages a
stage-specific reply for the caller (overriding the default suspended response), and carries
"suspend=true" so traversal routes to the suspend node when it completes:

\`\`\`
create node order
with type Suspensible
with properties
purpose=Capture the customer order, then suspend for the store manager
skill=graph.data.mapper
suspend=true
mapping[]=input.body -> model.order
mapping[]=text(order-submitted; waiting for store manager approval) -> output.body.stage
mapping[]=model.cid -> output.body.cid
\`\`\`

\`\`\`
create node approval
with type Suspensible
with properties
purpose=Capture the store manager approval, then suspend for the delivery department
skill=graph.data.mapper
suspend=true
mapping[]=input.body -> model.approval
mapping[]=text(approved; waiting for the delivery department to release the shipment) -> output.body.stage
mapping[]=model.cid -> output.body.cid
\`\`\`

\`\`\`
create node delivery
with type Suspensible
with properties
purpose=Capture the shipment release, then suspend for shipment confirmation
skill=graph.data.mapper
suspend=true
mapping[]=input.body -> model.delivery
mapping[]=text(released; waiting for shipment confirmation) -> output.body.stage
mapping[]=model.cid -> output.body.cid
\`\`\`

Create the completion, rejection, suspend and end nodes:

\`\`\`
create node ship
with type mapper
with properties
purpose=Ship to the customer with the full order history
skill=graph.data.mapper
mapping[]=text(shipped) -> output.body.stage
mapping[]=model.order -> output.body.order
mapping[]=model.approval -> output.body.approval
mapping[]=model.delivery -> output.body.delivery
mapping[]=input.body -> output.body.shipment
mapping[]=model.cid -> output.body.cid
\`\`\`

\`\`\`
create node reject
with type mapper
with properties
purpose=Reject a request that has no suspended transaction and is not an order
skill=graph.data.mapper
mapping[]=int(404) -> output.status
mapping[]=text(rejected) -> output.body.type
mapping[]=text(Transaction not found. Submit the order first) -> output.body.message
\`\`\`

\`\`\`
create node suspend
with type Suspend
with properties
purpose=Persist workflow state to Redis and wait for the next actor
skill=graph.suspend
task=v1.redis.persist.model
ttl=1h
\`\`\`

\`\`\`
create node end
\`\`\`

Connect the nodes. Every suspensible node draws BOTH edges - the checkpoint edge to "suspend"
and the continuation edge to the next step - so the diagram tells the whole story:

\`\`\`
connect root to resume with then
connect resume to check-fresh with fresh
connect check-fresh to order with submission
connect check-fresh to reject with no-transaction
connect order to suspend with checkpoint
connect order to approval with next
connect approval to suspend with checkpoint
connect approval to delivery with next
connect delivery to suspend with checkpoint
connect delivery to ship with next
connect ship to end with then
connect reject to end with then
connect suspend to end with then
\`\`\`

For your convenience, this graph model is preloaded as "tutorial-14".

Test the workflow
-----------------
Run 1 - the customer orders a laptop:

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-14 \\
  -H "Content-Type: application/json" \\
  -H "X-Correlation-Id: order-1001" \\
  -d '{"item": "laptop", "amount": 2000}'
\`\`\`

The reply is {"stage": "order-submitted; waiting for store manager approval", "cid": "order-1001"}
and the run is over - nothing stays in memory. Run 2 - the store manager approves:

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-14 \\
  -H "Content-Type: application/json" \\
  -H "X-Correlation-Id: order-1001" \\
  -d '{"decision": "approved", "manager": "store-88"}'
\`\`\`

Run 3 - the delivery department releases the shipment:

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-14 \\
  -H "Content-Type: application/json" \\
  -H "X-Correlation-Id: order-1001" \\
  -d '{"release": true, "courier": "express"}'
\`\`\`

Run 4 - shipment confirmation completes the workflow:

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-14 \\
  -H "Content-Type: application/json" \\
  -H "X-Correlation-Id: order-1001" \\
  -d '{"tracking": "TRK-12345"}'
\`\`\`

The final reply carries the whole history - the order from run 1, the approval from run 2, the
release from run 3 and the shipment from run 4 - proof that the workflow state crossed every
suspension. Now try a decision with a correlation ID that never ordered:

\`\`\`
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-14 \\
  -H "Content-Type: application/json" \\
  -H "X-Correlation-Id: order-9999" \\
  -d '{"decision": "approved"}'
\`\`\`

The workflow rejects it with HTTP-404 - the order must come first. Each record is consumed on
resume, so a duplicated request at any stage behaves like a fresh transaction instead of
executing that stage twice.

Summary
-------
In this session, we expressed a purchase workflow with three human checkpoints as four short
graph runs keyed by one business correlation ID: one reserved "suspend" node served every
checkpoint, each suspensible node captured its actor's input into the model and staged its own
stage response, and input validation enforced the order-before-decision sequence.

Why suspend and resume?
-----------------------
Real business processes wait on people - repeatedly. Suspension turns each wait into a durable
record instead of a parked runtime: any application instance sharing the state store can resume
the workflow, restarts lose nothing, and each run stays short and observable. The state store is
pluggable - Redis is the packaged implementation, and any composable function honoring the
documented store contract can replace it.
`,"../../../src/main/resources/help/help tutorial 2.md":`Tutorial 2
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
`,"../../../src/main/resources/help/help tutorial 3.md":`Tutorial 3
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
purpose=Demonstrate data sourcing using the Data Dictionary method - fetch one person profile (name and address) by person_id
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
`,"../../../src/main/resources/help/help tutorial 4.md":`Tutorial 4
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
`,"../../../src/main/resources/help/help tutorial 5.md":`Tutorial 5
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
`,"../../../src/main/resources/help/help tutorial 6.md":`Tutorial 6
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
`,"../../../src/main/resources/help/help tutorial 7.md":`Tutorial 7
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
`,"../../../src/main/resources/help/help tutorial 8.md":`Tutorial 8
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
`,"../../../src/main/resources/help/help tutorial 9.md":`Tutorial 9
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
`,"../../../src/main/resources/help/help update.md":`Update a node
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
`,"../../../src/main/resources/help/help upload.md":`Upload mock data to current graph instance
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
`,"../../../src/main/resources/help/help.md":`MiniGraph
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
- help list (nodes, connections, graphs, flows)
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
8. graph.task - invoke a composable function through its route name
9. graph.suspend - persist workflow state at a suspension point (the reserved 'suspend' node)
10. graph.resume - restore workflow state and continue past the suspension point

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
- help tutorial 13 (invoking a composable function with the task skill)
- help tutorial 14 (workflow suspension - a purchase workflow with three human checkpoints)
`});function ri(e){let t=e.split(`/`);return(t[t.length-1]??e).replace(/\.md$/,``)}var ii=Object.fromEntries(Object.entries(ni).map(([e,t])=>[ri(e),t]));function ai(e){return ii[e===``?`help`:`help ${e}`]??null}var oi=Object.keys(ii).filter(e=>e!==`help`).map(e=>e.replace(/^help\s+/,``)).sort(),si=[{id:`overview`,label:`Overview`},{id:`graph-model`,label:`Graph Model`},{id:`graph-skills`,label:`Graph Skills`},{id:`instance-model`,label:`Instance Model`},{id:`tutorials`,label:`Tutorials`,chipStripLabel:`Chapters`}],ci=new Set([`execute`,`inspect`,`instantiate`,`run`,`seen`,`upload`]);function li(e){return e===``?`overview`:e.startsWith(`tutorial `)?`tutorials`:e.startsWith(`graph-`)?`graph-skills`:ci.has(e)?`instance-model`:`graph-model`}function ui(e){if(e===`overview`)return[``];let t=oi.filter(t=>li(t)===e);return e===`tutorials`?[...t].sort((e,t)=>parseInt(e.replace(/^tutorial\s+/,``),10)-parseInt(t.replace(/^tutorial\s+/,``),10)):t}function di(e,t){return e===``?`Overview`:t===`tutorials`?e.replace(/^tutorial\s+/,``):e}var fi=si.flatMap(e=>ui(e.id));function pi(e){return e.replace(/^help\s*/i,``).trim().toLowerCase()}function mi({bus:e,setHelpTopic:t,onTabSwitch:n}){let r=(0,j.useRef)(n);(0,j.useEffect)(()=>{r.current=n}),(0,j.useEffect)(()=>e.on(`command.helpOrDescribe`,e=>{if(!e.commandText.trim().toLowerCase().startsWith(`help`))return;let n=pi(e.commandText);ai(n)!==null&&(t(n),r.current())}),[e,t])}function hi({ctx:e,navigate:t,addToast:n,wsPath:r}){let i=yr.find(e=>e.tabs.includes(`payload`)&&e.supportsUpload),a=(0,j.useRef)(null),o=i?.wsPath;(0,j.useEffect)(()=>{if(!(!o||!a.current)&&e.getSlot(o).phase===`connected`){let{wsPath:r,json:o}=a.current;a.current=null,e.setPendingPayload(r,o),t(i.path),n(`JSON loaded into JSON-Path editor ✓`,`success`)}},[o,e,t,n,i]);let s=(0,j.useCallback)(r=>{if(!i)return;let o=e.getSlot(i.wsPath);o.phase===`connected`?(e.setPendingPayload(i.wsPath,r),t(i.path),n(`JSON loaded into JSON-Path editor ✓`,`success`)):o.phase===`connecting`?(a.current={wsPath:i.wsPath,json:r},n(`Updated pending JSON transfer — latest payload will open when connected`,`info`)):(a.current={wsPath:i.wsPath,json:r},e.connect(i.wsPath,n),n(`Connecting to JSON-Path Playground…`,`info`))},[e,t,n,i]);return{handleSendToJsonPath:i&&r!==i.wsPath?s:void 0}}function gi({bus:e,onOpenModal:t,modalOpen:n}){let r=(0,j.useRef)(!1);(0,j.useEffect)(()=>{n||(r.current=!1)},[n]),(0,j.useEffect)(()=>e.on(`upload.invitation`,e=>{r.current||(r.current=!0,t(e.uploadPath))}),[e,t])}function _i({bus:e,addToast:t}){let[n,r]=(0,j.useState)(null),i=(0,j.useRef)(null),[a,o]=(0,j.useState)(new Set),s=(0,j.useCallback)(e=>{i.current=document.activeElement,r(e)},[]),c=(0,j.useCallback)(()=>{r(null),setTimeout(()=>i.current?.focus(),0)},[]),l=(0,j.useCallback)(e=>{o(e=>new Set([...e,n])),r(null),setTimeout(()=>i.current?.focus(),0),t(`Mock data uploaded successfully ✓`,`success`)},[n,t]),u=(0,j.useCallback)(e=>{t(`Upload failed: ${e}`,`error`)},[t]),d=(0,j.useCallback)(()=>{o(new Set)},[]);return gi({bus:e,onOpenModal:s,modalOpen:n!==null}),{modalUploadPath:n,successfulUploadPaths:a,handleOpenUploadModal:s,handleCloseUploadModal:c,handleUploadSuccess:l,handleUploadError:u,resetSuccessfulPaths:d}}function vi({bus:e,connected:t,appendMessage:n,addToast:r}){let i=(0,j.useRef)(null),a=(0,j.useRef)(!1),o=(0,j.useRef)(n);(0,j.useEffect)(()=>{o.current=n},[n]);let s=(0,j.useRef)(r);(0,j.useEffect)(()=>{s.current=r},[r]),(0,j.useEffect)(()=>{t||(i.current?.abort(),i.current=null,a.current=!1)},[t]),(0,j.useEffect)(()=>()=>{i.current?.abort()},[]),(0,j.useEffect)(()=>e.on(`payload.large`,e=>{if(a.current)return;let{apiPath:t,byteSize:n}=e;i.current?.abort();let r=new AbortController;i.current=r;let c=(n/(1024*1024)).toFixed(2);s.current(`Fetching large payload (${c} MB)…`,`info`),a.current=!0,fetch(t,{signal:r.signal}).then(e=>{if(!e.ok)throw Error(`HTTP ${e.status}`);return e.text()}).then(e=>{if(!e.trim())throw Error(`empty response body`);let t=e;try{t=JSON.stringify(JSON.parse(e),null,2)}catch{}o.current(t),a.current=!1,i.current=null}).catch(e=>{e.name!==`AbortError`&&(a.current=!1,i.current=null,o.current(`ERROR: payload fetch failed — ${e.message}`),s.current(`Payload fetch failed: ${e.message}`,`error`))})}),[e])}function yi(e){let[t,n]=_r(e,{}),r=(0,j.useCallback)(e=>{n(t=>({...t,[e]:{name:e,savedAt:new Date().toISOString()}}))},[n]),i=(0,j.useCallback)(e=>{n(t=>{let n={...t};return delete n[e],n})},[n]),a=(0,j.useCallback)(e=>Object.prototype.hasOwnProperty.call(t,e),[t]);return{savedGraphs:(0,j.useMemo)(()=>Object.values(t).sort((e,t)=>new Date(t.savedAt).getTime()-new Date(e.savedAt).getTime()),[t]),saveGraph:r,deleteGraph:i,hasGraph:a}}function bi(e,t){let[n,r]=_r(e,1),i=(0,j.useRef)(!1),[a,o]=(0,j.useState)(null),[s,c]=(0,j.useState)(null);(0,j.useEffect)(()=>t.on(`command.importGraph`,e=>{o(e.graphName),c(null)}),[t]);let l=(0,j.useCallback)(e=>{c(e),e===`untitled-${n}`&&(i.current=!0)},[n]),u=(0,j.useCallback)(()=>{o(null),c(null),i.current&&r(e=>e+1),i.current=!1},[r]);return{defaultName:s??a??`untitled-${n}`,setLastSavedName:l,resetName:u}}function xi({bus:e,connected:t,sendRawText:n,saveGraph:r,setLastSavedName:i,addToast:a}){let o=(0,j.useRef)(null),s=(0,j.useCallback)(e=>{if(!t){a(`Save failed: connection required to export graph`,`error`);return}let r=setTimeout(()=>{o.current!==null&&(o.current=null,a(`Save failed: export confirmation timed out`,`error`))},1e4);o.current={graphName:e,timeoutId:r},n(`export graph as ${e}`)},[t,n,a]);return(0,j.useEffect)(()=>e.on(`graph.exported`,e=>{if(o.current===null||e.graphName!==o.current.graphName)return;clearTimeout(o.current.timeoutId);let t=o.current.graphName;o.current=null,r(t),i(t),a(`Graph saved as "${t}"`,`success`)}),[e,r,i,a]),(0,j.useEffect)(()=>e.on(`graph.export.failed`,e=>{o.current!==null&&(clearTimeout(o.current.timeoutId),o.current=null,e.reason===`invalid-name`?a(`Save failed: invalid filename (a–z, A–Z, 0–9, hyphen only)`,`error`):a(`Save failed: root node name does not match existing graph`,`error`))}),[e,a]),(0,j.useEffect)(()=>{!t&&o.current!==null&&(clearTimeout(o.current.timeoutId),o.current=null,a(`Save failed: connection closed before export confirmation`,`error`))},[t,a]),(0,j.useEffect)(()=>()=>{o.current!==null&&clearTimeout(o.current.timeoutId)},[]),{handleSaveGraph:s,handleLoadGraph:(0,j.useCallback)(e=>{t&&(n(`import graph from ${e}`),a(`Importing graph "${e}"…`,`info`))},[t,n,a])}}var Si=new Map;function Ci(e){let[t,n]=(0,j.useState)(()=>Si.get(e)??null);return[t,(0,j.useCallback)(t=>{n(t),t===null?Si.delete(e):Si.set(e,t)},[e])]}function wi(e){if(e==null)return``;let t=typeof e==`string`?e:JSON.stringify(e);return t.includes(`'''`)&&console.warn(`[commandBuilder] Property value contains "'''" which cannot be escaped in the backend grammar. The value may be truncated on paste.`),t.includes(`
`)?`'''\n${t}\n'''`:t}function Ti(e,t){let n=[`${e} node ${t.alias}`];t.types.length>0&&n.push(`with type ${t.types[0]}`);let r=Object.entries(t.properties).filter(([,e])=>e!=null);if(r.length>0){n.push(`with properties`);for(let[e,t]of r)if(Array.isArray(t))for(let r of t)n.push(`${e}[]=${wi(r)}`);else n.push(`${e}[]=${wi(t)}`)}return n.join(`
`)}function Ei(e,t){let n=t?.nodes.some(t=>t.alias===e.node.alias)?`update`:`create`;return{verb:n,command:Ti(n,e.node)}}function Di(e){return`${e} ${e===1?`node`:`nodes`} clipped to workspace`}function Oi(e){let{added:t,duplicates:n,failed:r}=e;if(t===0&&n===0&&r===0)return{message:`No selected nodes are available to clip.`,type:`info`};if(t===0&&n>0&&r===0)return{message:`All selected nodes already exist in workspace.`,type:`info`};if(t===0&&n===0&&r>0)return{message:`Failed to clip selected nodes to workspace.`,type:`error`};let i=Di(t);return n>0&&(i+=`. ${n} already existed.`),r>0&&(i+=`. ${r} failed.`),{message:i,type:r>0?`error`:`success`}}function ki(e){return{execute(t){return e(t)}}}function Ai(e){return e.trim().toLowerCase()}function ji(e,t){if(!t.alias||t.action!==null&&t.action!==`delete-node`)return null;let n=e.aliases.find(n=>Ai(n)===Ai(t.alias)&&e.results[n]===void 0);return n?{...e,results:{...e.results,[n]:t.status===`accepted`?`success`:`error`}}:null}function Mi(e){return e.aliases.every(t=>e.results[t]!==void 0)}function Ni(e){let t=Object.values(e.results).filter(e=>e===`success`).length,n=Object.values(e.results).filter(e=>e===`error`).length,r=`${t} selected ${t===1?`node`:`nodes`} deleted.`;return t===0&&n>0?{message:`Failed to delete selected nodes.`,type:`error`}:n>0?{message:`${r} ${n} failed.`,type:`error`}:{message:r,type:`success`}}var Pi={toastContainer:`_toastContainer_hhy5k_1`,toast:`_toast_hhy5k_1`,slideIn:`_slideIn_hhy5k_1`,success:`_success_hhy5k_36`,error:`_error_hhy5k_40`,info:`_info_hhy5k_44`,toastIcon:`_toastIcon_hhy5k_48`,toastMessage:`_toastMessage_hhy5k_53`},Fi=({toasts:e,onRemove:t})=>e.length===0?null:(0,M.jsx)(`div`,{className:Pi.toastContainer,children:e.map(e=>(0,M.jsxs)(`div`,{className:`${Pi.toast} ${Pi[e.type]}`,onClick:()=>t(e.id),children:[(0,M.jsxs)(`span`,{className:Pi.toastIcon,children:[e.type===`success`&&`✅`,e.type===`error`&&`❌`,e.type===`info`&&`ℹ️`]}),(0,M.jsx)(`span`,{className:Pi.toastMessage,children:e.message})]},e.id))}),Ii={container:`_container_9dbh2_3`,trigger:`_trigger_9dbh2_7`,chevron:`_chevron_9dbh2_37`,chevronOpen:`_chevronOpen_9dbh2_43`,dot:`_dot_9dbh2_49`,dotIdle:`_dotIdle_9dbh2_56`,dotConnecting:`_dotConnecting_9dbh2_57`,pulse:`_pulse_9dbh2_1`,dotConnected:`_dotConnected_9dbh2_58`,dotPartial:`_dotPartial_9dbh2_59`,dropdown:`_dropdown_9dbh2_65`,fadeIn:`_fadeIn_9dbh2_1`};function Li({label:e,dotStatus:t,children:n}){let[r,i]=(0,j.useState)(!1),a=(0,j.useRef)(null);(0,j.useEffect)(()=>{if(!r)return;let e=e=>{a.current&&!a.current.contains(e.target)&&i(!1)};return document.addEventListener(`mousedown`,e),()=>document.removeEventListener(`mousedown`,e)},[r]);let o=e=>{e.key===`Escape`&&(i(!1),a.current?.querySelector(`button[aria-haspopup]`)?.focus())},s=t===`connected`?Ii.dotConnected:t===`connecting`?Ii.dotConnecting:t===`partial`?Ii.dotPartial:t===`idle`?Ii.dotIdle:void 0;return(0,M.jsxs)(`div`,{className:Ii.container,ref:a,onKeyDown:o,children:[(0,M.jsxs)(`button`,{className:Ii.trigger,onClick:()=>i(e=>!e),"aria-haspopup":`true`,"aria-expanded":r,children:[t!==void 0&&(0,M.jsx)(`span`,{className:`${Ii.dot} ${s??``}`,"aria-hidden":`true`}),(0,M.jsx)(`span`,{children:e}),(0,M.jsx)(`span`,{className:`${Ii.chevron} ${r?Ii.chevronOpen:``}`,"aria-hidden":`true`,children:`▾`})]}),r&&(0,M.jsx)(`div`,{className:Ii.dropdown,role:`menu`,children:n})]})}var N={panel:`_panel_1ws0d_1`,section:`_section_1ws0d_7`,sectionHeader:`_sectionHeader_1ws0d_20`,sessionHeaderRow:`_sessionHeaderRow_1ws0d_29`,iconButton:`_iconButton_1ws0d_36`,copyButton:`_copyButton_1ws0d_63`,copyButtonCopied:`_copyButtonCopied_1ws0d_95`,relationshipRow:`_relationshipRow_1ws0d_102`,subscriberRow:`_subscriberRow_1ws0d_103`,sessionId:`_sessionId_1ws0d_114`,statusDot:`_statusDot_1ws0d_127`,metaText:`_metaText_1ws0d_136`,emptyMessage:`_emptyMessage_1ws0d_137`,subscriberList:`_subscriberList_1ws0d_148`,subscribeForm:`_subscribeForm_1ws0d_157`,subscribeInput:`_subscribeInput_1ws0d_164`,subscribeButton:`_subscribeButton_1ws0d_189`,resetButton:`_resetButton_1ws0d_190`,unsubscribeButton:`_unsubscribeButton_1ws0d_218`,actionsRow:`_actionsRow_1ws0d_241`,errorMessage:`_errorMessage_1ws0d_261`,infoMessage:`_infoMessage_1ws0d_262`};function Ri({copied:e}){return e?(0,M.jsx)(`svg`,{viewBox:`0 0 16 16`,"aria-hidden":`true`,focusable:`false`,children:(0,M.jsx)(`path`,{d:`M6.2 11.4 2.9 8.1l1.1-1.1 2.2 2.2 5.8-5.8 1.1 1.1-6.9 6.9Z`})}):(0,M.jsxs)(`svg`,{viewBox:`0 0 16 16`,"aria-hidden":`true`,focusable:`false`,children:[(0,M.jsx)(`path`,{d:`M5 2.5A1.5 1.5 0 0 1 6.5 1h6A1.5 1.5 0 0 1 14 2.5v6A1.5 1.5 0 0 1 12.5 10H11V8.7h1.5a.2.2 0 0 0 .2-.2v-6a.2.2 0 0 0-.2-.2h-6a.2.2 0 0 0-.2.2V4H5V2.5Z`}),(0,M.jsx)(`path`,{d:`M2 6.5A1.5 1.5 0 0 1 3.5 5h6A1.5 1.5 0 0 1 11 6.5v7A1.5 1.5 0 0 1 9.5 15h-6A1.5 1.5 0 0 1 2 13.5v-7Zm1.5-.2a.2.2 0 0 0-.2.2v7a.2.2 0 0 0 .2.2h6a.2.2 0 0 0 .2-.2v-7a.2.2 0 0 0-.2-.2h-6Z`})]})}function P(e){return e.connected?e.state.pendingCommand===`refresh`||e.state.loading?`connecting`:e.state.sessionId?`connected`:`partial`:`idle`}function F({controller:e}){let{state:t}=e,[n,r]=(0,j.useState)(!1),[i,a]=(0,j.useState)(``),[o,s]=(0,j.useState)(null),[c,l]=(0,j.useState)(null),u=(0,j.useRef)(null),d=e.canReset;(0,j.useEffect)(()=>()=>{u.current!==null&&window.clearTimeout(u.current)},[]),(0,j.useEffect)(()=>{t.subscribedTo&&t.pendingCommand!==`subscribe`&&(r(!1),a(``))},[t.pendingCommand,t.subscribedTo]);let f=t=>{t.preventDefault(),e.subscribeToSession(i)},p=async e=>{try{await navigator.clipboard.writeText(e),s(null),l(e),u.current!==null&&window.clearTimeout(u.current),u.current=window.setTimeout(()=>{l(null),u.current=null},1400)}catch{l(null),s(`Could not copy the session ID. Please copy it manually.`)}};return e.connected?(0,M.jsxs)(`div`,{className:N.panel,children:[(0,M.jsxs)(`section`,{className:N.section,children:[(0,M.jsx)(`div`,{className:N.sectionHeader,children:`This session`}),(0,M.jsxs)(`div`,{className:N.sessionHeaderRow,children:[t.sessionId?(0,M.jsx)(`code`,{className:N.sessionId,title:t.sessionId,children:t.sessionId}):(0,M.jsx)(`p`,{className:N.emptyMessage,children:`Session details are not loaded yet.`}),t.sessionId&&(0,M.jsx)(`button`,{type:`button`,className:`${N.copyButton} ${c===t.sessionId?N.copyButtonCopied:``}`,onClick:()=>void p(t.sessionId),"aria-label":`Copy session ID`,title:c===t.sessionId?`Copied`:`Copy session ID`,children:(0,M.jsx)(Ri,{copied:c===t.sessionId})}),e.canSubscribe&&(0,M.jsx)(`button`,{type:`button`,className:N.iconButton,onClick:()=>{e.clearMessage(),r(e=>!e)},"aria-expanded":n,"aria-label":n?`Close subscribe form`:`Subscribe to another session`,title:n?`Close subscribe form`:`Subscribe to another session`,children:n?`×`:`+`})]}),t.startedSince&&(0,M.jsxs)(`div`,{className:N.metaText,children:[`Started since `,t.startedSince]}),n&&(0,M.jsxs)(`form`,{className:N.subscribeForm,onSubmit:f,children:[(0,M.jsx)(`input`,{className:N.subscribeInput,value:i,onChange:t=>{a(t.target.value),e.clearMessage()},placeholder:`ws-123456-1`,autoComplete:`off`,disabled:!e.canSubscribe}),(0,M.jsx)(`button`,{type:`submit`,className:N.subscribeButton,disabled:!e.canSubscribe||i.trim().length===0,children:t.pendingCommand===`subscribe`?`...`:`Subscribe`})]})]}),t.subscribedTo&&(0,M.jsxs)(`section`,{className:N.section,children:[(0,M.jsx)(`div`,{className:N.sectionHeader,children:`Subscribed to`}),(0,M.jsxs)(`div`,{className:N.relationshipRow,children:[(0,M.jsx)(`span`,{className:N.statusDot,"aria-hidden":`true`}),(0,M.jsx)(`code`,{className:N.sessionId,title:t.subscribedTo,children:t.subscribedTo}),(0,M.jsx)(`button`,{type:`button`,className:N.unsubscribeButton,onClick:e.unsubscribe,disabled:!e.canUnsubscribe,children:t.pendingCommand===`unsubscribe`?`...`:`Unsubscribe`})]})]}),t.subscribers.length>0&&(0,M.jsxs)(`section`,{className:N.section,children:[(0,M.jsx)(`div`,{className:N.sectionHeader,children:`Subscribers`}),(0,M.jsx)(`ul`,{className:N.subscriberList,children:t.subscribers.map(e=>(0,M.jsxs)(`li`,{className:N.subscriberRow,children:[(0,M.jsx)(`span`,{className:N.statusDot,"aria-hidden":`true`}),(0,M.jsx)(`code`,{className:N.sessionId,title:e,children:e})]},e))})]}),d&&(0,M.jsx)(`div`,{className:N.actionsRow,children:(0,M.jsx)(`button`,{type:`button`,className:N.resetButton,onClick:e.resetSession,disabled:!e.canReset,children:t.pendingCommand===`reset`?`Resetting...`:`Reset Session`})}),t.error&&(0,M.jsx)(`div`,{className:N.errorMessage,role:`alert`,children:t.error}),o&&(0,M.jsx)(`div`,{className:N.errorMessage,role:`alert`,children:o})]}):(0,M.jsx)(`div`,{className:N.panel,children:(0,M.jsx)(`p`,{className:N.emptyMessage,children:`Connect Minigraph to view session details.`})})}function zi({controller:e}){return(0,M.jsx)(Li,{label:`Session`,dotStatus:P(e),children:(0,M.jsx)(F,{controller:e})})}var I={nav:`_nav_1hfby_3`,menuList:`_menuList_1hfby_11`,menuItem:`_menuItem_1hfby_19`,toolRow:`_toolRow_1hfby_56`,toolLink:`_toolLink_1hfby_67`,toolLinkActive:`_toolLinkActive_1hfby_92`,toolDot:`_toolDot_1hfby_99`,toolDotIdle:`_toolDotIdle_1hfby_106`,toolDotConnecting:`_toolDotConnecting_1hfby_107`,pulse:`_pulse_1hfby_1`,toolDotConnected:`_toolDotConnected_1hfby_108`,connectAllRow:`_connectAllRow_1hfby_112`,connectAllBtn:`_connectAllBtn_1hfby_118`,connectAllBtnStop:`_connectAllBtnStop_1hfby_142`,toolConnectBtn:`_toolConnectBtn_1hfby_154`,toolConnectBtnStop:`_toolConnectBtnStop_1hfby_180`,externalIcon:`_externalIcon_1hfby_192`};function Bi(e){return e.every(e=>e===`connected`)?`connected`:e.every(e=>e===`idle`)?`idle`:e.some(e=>e===`connecting`)?`connecting`:`partial`}function Vi(e){return e===`connected`?`connected`:e===`connecting`?`connecting`:`idle`}var Hi=[{href:`/info`,label:`Info`},{href:`/info/lib`,label:`Libraries`},{href:`/info/routes`,label:`Services`},{href:`/health`,label:`Health`},{href:`/env`,label:`Environment`},{href:`http://localhost:8085/api/ws/json`,label:`Legacy JSON`},{href:`http://localhost:8085/api/ws/graph`,label:`Legacy Graph`}];function Ui({addToast:e,sessionCollaboration:t}){let n=Er(),r=yr.map(e=>n.getSlot(e.wsPath).phase),i=Bi(r),a=r.every(e=>e===`connected`),o=r.some(e=>e===`connecting`);function s(){yr.forEach(t=>{n.getSlot(t.wsPath).phase===`idle`&&n.connect(t.wsPath,e)})}function c(){yr.forEach(e=>{let{phase:t}=n.getSlot(e.wsPath);(t===`connected`||t===`connecting`)&&n.disconnect(e.wsPath)})}return(0,M.jsxs)(`nav`,{className:I.nav,"aria-label":`Main navigation`,children:[t&&(0,M.jsx)(zi,{controller:t}),(0,M.jsxs)(Li,{label:`Tools`,dotStatus:i,children:[(0,M.jsx)(`div`,{className:I.connectAllRow,children:(0,M.jsx)(`button`,{className:`${I.connectAllBtn} ${a?I.connectAllBtnStop:``}`,onClick:a?c:s,disabled:o,"aria-label":o?`Connecting…`:a?`Disconnect all WebSockets`:`Connect all WebSockets`,children:o?`Connecting…`:a?`Disconnect All`:`Connect All`})}),(0,M.jsx)(`ul`,{className:I.menuList,role:`none`,children:yr.map(t=>{let{phase:r}=n.getSlot(t.wsPath),i=Vi(r),a=r===`connected`,o=r===`connecting`,s=i===`connected`?I.toolDotConnected:i===`connecting`?I.toolDotConnecting:I.toolDotIdle;return(0,M.jsxs)(`li`,{role:`none`,className:I.toolRow,children:[(0,M.jsxs)(qn,{to:t.path,role:`menuitem`,className:({isActive:e})=>`${I.toolLink} ${e?I.toolLinkActive:``}`,children:[(0,M.jsx)(`span`,{className:`${I.toolDot} ${s}`,"aria-hidden":`true`}),(0,M.jsx)(`span`,{className:I.toolLabel,children:t.label})]}),(0,M.jsx)(`button`,{className:`${I.toolConnectBtn} ${a?I.toolConnectBtnStop:``}`,onClick:()=>a||o?n.disconnect(t.wsPath):n.connect(t.wsPath,e),disabled:o,"aria-label":o?`Connecting…`:a?`Disconnect ${t.label}`:`Connect ${t.label}`,title:o?`Connecting…`:xr(t.wsPath),children:o?`…`:a?`Stop`:`Start`})]},t.path)})})]}),(0,M.jsx)(Li,{label:`Quick Links`,children:(0,M.jsx)(`ul`,{className:I.menuList,role:`none`,children:Hi.map(e=>(0,M.jsx)(`li`,{role:`none`,children:(0,M.jsxs)(`a`,{href:e.href,role:`menuitem`,className:I.menuItem,target:`_blank`,rel:`noopener noreferrer`,children:[e.label,(0,M.jsx)(`span`,{className:I.externalIcon,"aria-hidden":`true`,children:`↗`})]})},e.href))})})]})}var Wi={saveBtn:`_saveBtn_1xd2l_3`,saveForm:`_saveForm_1xd2l_33`,saveInput:`_saveInput_1xd2l_39`,saveInputWarn:`_saveInputWarn_1xd2l_55`,saveWarnLabel:`_saveWarnLabel_1xd2l_59`,saveActionBtn:`_saveActionBtn_1xd2l_65`};function Gi({disabled:e,defaultName:t,onSave:n,nameExists:r,connected:i=!1}){let[a,o]=(0,j.useState)(!1),[s,c]=(0,j.useState)(``),l=(0,j.useRef)(null),u=(0,j.useCallback)(()=>{c(t),o(!0)},[t]),d=(0,j.useCallback)(()=>{o(!1),c(``)},[]),f=(0,j.useCallback)(()=>{let e=s.trim();e&&(n(e),o(!1),c(``))},[s,n]),p=(0,j.useCallback)(e=>{e.key===`Enter`&&(e.preventDefault(),f()),e.key===`Escape`&&(e.preventDefault(),d())},[f,d]);return(0,j.useEffect)(()=>{a&&l.current?.focus()},[a]),a?(0,M.jsxs)(`div`,{className:Wi.saveForm,children:[(0,M.jsx)(`input`,{ref:l,className:`${Wi.saveInput}${r?.(s.trim())?` ${Wi.saveInputWarn}`:``}`,type:`text`,value:s,onChange:e=>c(e.target.value),onKeyDown:p,placeholder:`Enter a name…`,"aria-label":`Graph save name`,maxLength:80}),r?.(s.trim())&&(0,M.jsx)(`span`,{className:Wi.saveWarnLabel,role:`status`,children:`Overwrite?`}),(0,M.jsx)(`button`,{className:Wi.saveActionBtn,onClick:f,disabled:!s.trim(),"aria-label":`Confirm save`,children:`✅`}),(0,M.jsx)(`button`,{className:Wi.saveActionBtn,onClick:d,"aria-label":`Cancel save`,children:`❌`})]}):(0,M.jsx)(`button`,{className:Wi.saveBtn,onClick:u,disabled:e||!i,title:e?`No graph loaded`:i?`Export graph snapshot to server and save bookmark`:`Connect first to save`,"aria-label":`Save graph snapshot`,children:`💾 Save Graph`})}var Ki={empty:`_empty_tpeii_3`,hint:`_hint_tpeii_12`,list:`_list_tpeii_21`,row:`_row_tpeii_31`,rowInfo:`_rowInfo_tpeii_50`,rowName:`_rowName_tpeii_58`,rowMeta:`_rowMeta_tpeii_67`,rowActions:`_rowActions_tpeii_78`,loadBtn:`_loadBtn_tpeii_84`,deleteBtn:`_deleteBtn_tpeii_85`};function qi({savedGraphs:e,onLoad:t,onDelete:n,connected:r}){return(0,M.jsx)(Li,{label:e.length>0?`Load Graph (${e.length})`:`Load Graph`,children:e.length===0?(0,M.jsx)(`p`,{className:Ki.empty,children:`No saved graphs yet.`}):(0,M.jsxs)(M.Fragment,{children:[!r&&(0,M.jsx)(`p`,{className:Ki.hint,children:`Connect to load a graph`}),(0,M.jsx)(`ul`,{className:Ki.list,role:`list`,children:e.map(e=>(0,M.jsxs)(`li`,{className:Ki.row,children:[(0,M.jsxs)(`div`,{className:Ki.rowInfo,children:[(0,M.jsx)(`span`,{className:Ki.rowName,title:e.name,children:e.name}),(0,M.jsx)(`span`,{className:Ki.rowMeta,children:new Date(e.savedAt).toLocaleString()})]}),(0,M.jsxs)(`div`,{className:Ki.rowActions,children:[(0,M.jsx)(`button`,{className:Ki.loadBtn,onClick:()=>t(e.name),disabled:!r,title:r?`Run: import graph from ${e.name}`:`Connect to the playground first`,"aria-label":`Load graph ${e.name}`,children:`Load`}),(0,M.jsx)(`button`,{className:Ki.deleteBtn,onClick:()=>n(e.name),title:`Remove "${e.name}" from local storage`,"aria-label":`Delete saved graph ${e.name}`,children:`Delete`})]})]},e.name))})]})})}var Ji={payloadRoot:`_payloadRoot_6u47x_2`,labelRow:`_labelRow_6u47x_10`,label:`_label_6u47x_10`,payloadControls:`_payloadControls_6u47x_26`,charCounter:`_charCounter_6u47x_32`,typeIndicator:`_typeIndicator_6u47x_38`,validationIcon:`_validationIcon_6u47x_49`,formatButton:`_formatButton_6u47x_53`,uploadButton:`_uploadButton_6u47x_67`,textarea:`_textarea_6u47x_82`,textareaError:`_textareaError_6u47x_107`,errorMessage:`_errorMessage_6u47x_109`,sampleButtonsRow:`_sampleButtonsRow_6u47x_117`,sampleButtons:`_sampleButtons_6u47x_117`,sampleLabel:`_sampleLabel_6u47x_130`,sampleGroup:`_sampleGroup_6u47x_136`,sampleGroupLabel:`_sampleGroupLabel_6u47x_143`,sampleButton:`_sampleButton_6u47x_117`};function Yi({onLoad:e}){let t=Object.keys(br).filter(e=>e.startsWith(`json_`)),n=Object.keys(br).filter(e=>e.startsWith(`xml_`)),r=e=>e.replace(/^(json|xml)_/,``).replace(/_/g,` `);return(0,M.jsxs)(`div`,{className:Ji.sampleButtons,children:[(0,M.jsx)(`span`,{className:Ji.sampleLabel,children:`Quick load:`}),(0,M.jsxs)(`div`,{className:Ji.sampleGroup,children:[(0,M.jsx)(`span`,{className:Ji.sampleGroupLabel,children:`JSON:`}),t.map(t=>(0,M.jsx)(`button`,{className:Ji.sampleButton,onClick:()=>e(br[t]),children:r(t)},t))]}),(0,M.jsxs)(`div`,{className:Ji.sampleGroup,children:[(0,M.jsx)(`span`,{className:Ji.sampleGroupLabel,children:`XML:`}),n.map(t=>(0,M.jsx)(`button`,{className:Ji.sampleButton,onClick:()=>e(br[t]),children:r(t)},t))]})]})}function Xi({payload:e,onChange:t,validation:n,onFormat:r,onUpload:i}){return(0,M.jsxs)(`div`,{className:Ji.payloadRoot,children:[(0,M.jsxs)(`div`,{className:Ji.labelRow,children:[(0,M.jsx)(`label`,{htmlFor:`payload`,className:Ji.label,children:`JSON/XML Payload`}),(0,M.jsxs)(`div`,{className:Ji.payloadControls,children:[(0,M.jsxs)(`span`,{className:Ji.charCounter,children:[`size: `,e.length]}),e&&n.type&&(0,M.jsx)(`span`,{className:Ji.typeIndicator,children:n.type.toUpperCase()}),e&&(0,M.jsx)(`span`,{className:Ji.validationIcon,children:n.valid?`✅`:`❌`}),(0,M.jsx)(`button`,{className:Ji.formatButton,onClick:r,disabled:!e||n.type!==`json`,title:n.type===`xml`?`Format only available for JSON`:`Format JSON`,children:`Format`}),i!==void 0&&(0,M.jsx)(`button`,{className:Ji.uploadButton,onClick:i,disabled:!e||!n.valid||n.type!==`json`,title:`Upload JSON payload to current session via REST`,children:`Upload`})]})]}),(0,M.jsx)(`textarea`,{id:`payload`,className:`${Ji.textarea} ${n.valid?``:Ji.textareaError}`,placeholder:`Paste your JSON/XML payload here`,value:e,onChange:e=>t(e.target.value)}),!n.valid&&(0,M.jsx)(`div`,{className:Ji.errorMessage,children:n.error}),(0,M.jsx)(`div`,{className:Ji.sampleButtonsRow,children:(0,M.jsx)(Yi,{onLoad:t})})]})}var Zi={Root:{icon:`🚀`,label:`Root`},End:{icon:`🏁`,label:`End`},Fetcher:{icon:`🌐`,label:`Fetcher`},mapper:{icon:`🗺️`,label:`Mapper`},Math:{icon:`🔢`,label:`Math`},JavaScript:{icon:`📜`,label:`JavaScript`},Provider:{icon:`🔌`,label:`Provider`},Dictionary:{icon:`📖`,label:`Dictionary`},Join:{icon:`🔀`,label:`Join`},Extension:{icon:`🧩`,label:`Extension`},Island:{icon:`🏝️`,label:`Island`},Decision:{icon:`❓`,label:`Decision`},Suspend:{icon:`⏸️`,label:`Suspend`},Resume:{icon:`▶️`,label:`Resume`},Suspensible:{icon:`⏯️`,label:`Suspensible`}},Qi={boxSizing:`border-box`,borderRadius:`8px`,borderWidth:`1.5px`,borderStyle:`solid`,background:`var(--bg-secondary, #1e1e2e)`,color:`var(--text-primary, #cdd6f4)`,fontSize:`0.75rem`,boxShadow:`0 2px 8px rgba(0,0,0,0.45)`,overflow:`visible`,padding:0},$i={Root:`#15803d`,End:`#dc2626`,Fetcher:`#2563eb`,mapper:`#ea580c`,Math:`#a16207`,JavaScript:`#7e22ce`,Provider:`#be185d`,Dictionary:`#0e7490`,Join:`#65a30d`,Extension:`#4338ca`,Island:`#475569`,Decision:`#b45309`,Suspend:`#0d9488`,Resume:`#0284c7`,Suspensible:`#c026d3`},ea=`#6c7086`;function ta(e){return Zi[e]??{icon:`📦`,label:e}}function na(e){let t=$i[e]??ea;return{...Qi,borderColor:t,"--node-accent":t}}var ra={content:`_content_138ap_8`,header:`_header_138ap_22`,icon:`_icon_138ap_42`,alias:`_alias_138ap_47`,badge:`_badge_138ap_53`,body:`_body_138ap_65`,row:`_row_138ap_70`,label:`_label_138ap_83`,value:`_value_138ap_89`,edgeHandle:`_edgeHandle_138ap_103`};function ia({label:e,value:t}){return(0,M.jsxs)(`div`,{className:ra.row,children:[(0,M.jsx)(`span`,{className:ra.label,children:e}),(0,M.jsx)(`span`,{className:ra.value,title:t,children:t})]})}function aa({properties:e}){let t=Object.entries(e).filter(([,e])=>e!=null);return t.length===0?null:(0,M.jsx)(M.Fragment,{children:t.map(([e,t])=>Array.isArray(t)?t.map((t,n)=>{let r=typeof t==`string`?t:JSON.stringify(t);return(0,M.jsx)(ia,{label:n===0?e:``,value:r},`${e}-${n}`)}):(0,M.jsx)(ia,{label:e,value:typeof t==`string`?t:JSON.stringify(t)},e))})}function oa({alias:e,nodeType:t,properties:n}){let r=ta(t);return(0,M.jsx)(j.Fragment,{children:(0,M.jsxs)(`div`,{className:ra.content,children:[(0,M.jsxs)(`div`,{className:ra.header,children:[(0,M.jsx)(`span`,{className:ra.icon,children:r.icon}),(0,M.jsx)(`span`,{className:ra.alias,children:e}),(0,M.jsx)(`span`,{className:ra.badge,children:r.label})]}),(0,M.jsx)(`div`,{className:ra.body,children:(0,M.jsx)(aa,{properties:n})})]})})}function sa({data:e,isConnectable:t,selected:n}){return(0,M.jsxs)(M.Fragment,{children:[(0,M.jsx)(s,{minWidth:180,minHeight:e.minHeight,isVisible:n}),e.targetHandles.map(({id:e,offset:n})=>(0,M.jsx)(d,{id:e,type:`target`,position:v.Left,isConnectable:t,className:ra.edgeHandle,style:{top:`calc(50% + ${n}px)`}},e)),e.backSourceHandles.map(({id:e,offset:n})=>(0,M.jsx)(d,{id:e,type:`source`,position:v.Left,isConnectable:t,className:ra.edgeHandle,style:{top:`calc(50% + ${n}px)`}},e)),(0,M.jsx)(oa,{alias:e.alias,nodeType:e.nodeType,properties:e.properties}),e.sourceHandles.map(({id:e,offset:n})=>(0,M.jsx)(d,{id:e,type:`source`,position:v.Right,isConnectable:t,className:ra.edgeHandle,style:{top:`calc(50% + ${n}px)`}},e)),e.backTargetHandles.map(({id:e,offset:n})=>(0,M.jsx)(d,{id:e,type:`target`,position:v.Right,isConnectable:t,className:ra.edgeHandle,style:{top:`calc(50% + ${n}px)`}},e))]})}var ca={Root:sa,End:sa,Fetcher:sa,mapper:sa,Math:sa,JavaScript:sa,Provider:sa,Dictionary:sa,Join:sa,Extension:sa,Island:sa,Decision:sa,default:sa},la={graphWrapper:`_graphWrapper_b38vy_15`,graphSurface:`_graphSurface_b38vy_24`,empty:`_empty_b38vy_30`,emptyIcon:`_emptyIcon_b38vy_43`,emptyCreateButton:`_emptyCreateButton_b38vy_48`,emptyHint:`_emptyHint_b38vy_70`,refreshingOverlay:`_refreshingOverlay_b38vy_139`,clipboardDropOverlay:`_clipboardDropOverlay_b38vy_151`,clipboardDropMessage:`_clipboardDropMessage_b38vy_164`,refreshingSpinner:`_refreshingSpinner_b38vy_179`,graphRefreshSpin:`_graphRefreshSpin_b38vy_1`},ua=class extends j.Component{constructor(...e){super(...e),this.state={caughtError:null}}static getDerivedStateFromError(e){return{caughtError:e instanceof Error?e.message:String(e)}}componentDidCatch(e,t){let n=e instanceof Error?e.message:String(e);console.error(`[GraphView] Render error:`,n,t.componentStack),this.props.onRenderError?.(`Graph render failed: ${n}`)}render(){return this.state.caughtError?(0,M.jsxs)(`div`,{className:la.empty,children:[(0,M.jsx)(`span`,{className:la.emptyIcon,children:`⚠️`}),(0,M.jsx)(`span`,{children:`Graph could not be rendered.`}),(0,M.jsx)(`span`,{children:this.state.caughtError})]}):this.props.children}},da=240,fa=100,pa=60,ma=360,ha=120,ga=80,_a=`rgba(148, 163, 184, 0.42)`,va=`var(--bg-secondary)`,ya=24,ba=32,xa=[`#0369a1`,`#15803d`,`#b45309`,`#7e22ce`,`#b91c1c`,`#0f766e`,`#c2410c`,`#a16207`],Sa={fetch:`#0369a1`,details:`#0369a1`,"ext-call":`#0369a1`,mapping:`#b45309`,compute:`#b45309`,calculate:`#b45309`,evaluate:`#b45309`,fork:`#7e22ce`,join:`#7e22ce`,one:`#7e22ce`,two:`#6d28d9`,three:`#5b21b6`,more:`#4c1d95`,done:`#15803d`,complete:`#15803d`,finish:`#15803d`,positive:`#15803d`,negative:`#b91c1c`};function Ca(e){let t=0;for(let n=0;n<e.length;n++)t=(t<<5)-t+e.charCodeAt(n),t|=0;return Math.abs(t)}function wa(e){if(e.length===0)return _a;let t=e[0].trim().toLowerCase();return Sa[t]||xa[Ca(t)%xa.length]}function Ta(e){return`source-${e}`}function Ea(e){return`target-${e}`}function Da(e){return`back-source-${e}`}function Oa(e){return`back-target-${e}`}function ka(e,t){return t<=1?0:t===2?e===0?-24:ya:(e-(t-1)/2)*ya}function Aa(e){return e<=1?fa:Math.max(fa,(e-1)*ya+ba*2)}var ja=new Set([`graph.math`,`graph.js`]),Ma=[`Dictionary`,`Provider`,`Module`,`Entity`],Na={ROOT_TREE:0,DEFAULT_TREE:1,END_TREE:2};function Pa(e){return e.alias.toLowerCase()===`root`||e.types.includes(`Root`)||e.types.includes(`entry_point`)}function Fa(e){return e.alias.toLowerCase()===`end`||e.types.includes(`End`)}function Ia(e){return e.hasRoot?Na.ROOT_TREE:e.hasEnd?Na.END_TREE:Na.DEFAULT_TREE}function La(e,t){let n=Ia(e)-Ia(t);return n===0?e.sortKey.localeCompare(t.sortKey):n}function Ra(e,t){if(t.has(e.alias))return`flow`;let n=e.types[0]??``,r=typeof e.properties.skill==`string`?e.properties.skill:void 0;return n===`Dictionary`?`Dictionary`:n===`Provider`?`Provider`:r&&ja.has(r)?`Module`:r?`__unknown__`:`Entity`}function za(e,t,n){let r=new Set;for(let e of t??[])r.add(e.source),r.add(e.target);let i=[],a=[],o=new Map;for(let t of e){let e=Ra(t,r);o.set(t.alias,e),e===`flow`?i.push(t):a.push(t)}let s=new Set(i.map(e=>e.alias)),c=new Map(i.map(e=>[e.alias,e])),l=new Map,u=new Map,d=new Map;for(let e of i)l.set(e.alias,[]),u.set(e.alias,new Set),d.set(e.alias,0);for(let e of t??[])!s.has(e.source)||!s.has(e.target)||(l.get(e.source)?.push(e.target),u.get(e.source)?.add(e.target),u.get(e.target)?.add(e.source),d.set(e.target,(d.get(e.target)??0)+1));let f=i.filter(e=>d.get(e.alias)===0||e.types.includes(`entry_point`)||Pa(e)).map(e=>e.alias),p=new Set;{let e=new Map;for(let t of i)e.set(t.alias,0);function t(t){if(e.get(t)!==0)return;e.set(t,1);let n=[{node:t,childIdx:0}];for(;n.length>0;){let t=n[n.length-1],r=l.get(t.node)??[];if(t.childIdx>=r.length){e.set(t.node,2),n.pop();continue}let i=r[t.childIdx++],a=e.get(i);a===1?p.add(`${t.node}\t${i}`):a===0&&(e.set(i,1),n.push({node:i,childIdx:0}))}}for(let e of f)t(e);for(let e of i)t(e.alias)}let m=[],h=new Set;for(let e of Array.from(s).sort()){if(h.has(e))continue;let t=[],n=[e];for(h.add(e);n.length>0;){let e=n.pop();t.push(e);for(let t of u.get(e)??[])h.has(t)||(h.add(t),n.push(t))}t.sort();let r=t.map(e=>c.get(e)).filter(e=>!!e);m.push({aliases:t,nodes:r,hasRoot:r.some(Pa),hasEnd:r.some(Fa),sortKey:t[0]??``})}m.sort(La);let g=new Map,_=new Map,v=0,y=0;for(let e of m){let t=new Set(e.aliases),r=e.nodes.filter(e=>d.get(e.alias)===0||e.types.includes(`entry_point`)||Pa(e)).map(e=>e.alias).sort();r.length===0&&e.aliases.length>0&&r.push(e.aliases[0]);let i=new Map,a=[...r];for(r.forEach(e=>i.set(e,0));a.length>0;){let e=a.shift(),n=i.get(e)??0;for(let r of l.get(e)??[])t.has(r)&&(p.has(`${e}\t${r}`)||(!i.has(r)||i.get(r)<=n)&&(i.set(r,n+1),a.push(r)))}let o=i.size>0?Math.max(...i.values()):0;for(let t of e.aliases)i.has(t)||i.set(t,o+1);let s=new Map;for(let[e,t]of i)s.has(t)||s.set(t,[]),s.get(t).push(e);let c=y;for(let[e,t]of[...s].sort(([e],[t])=>e-t)){let r=t.slice().sort(),i=-(r.reduce((e,t)=>e+(n.get(t)??fa),0)+Math.max(0,r.length-1)*pa)/2,a=v+e,o=y+e*360;c=Math.max(c,o),r.forEach(e=>{let t=n.get(e)??fa;g.set(e,a),_.set(e,{x:o,y:i}),i+=t+pa})}let u=i.size>0?Math.max(...i.values()):0;v+=u+1,y=c+da+ma}let b=0;for(let[e,t]of _)b=Math.max(b,t.y+(n.get(e)??fa));let x=b+(_.size>0?ha:0),S=new Map;for(let e of Ma)S.set(e,[]);S.set(`__unknown__`,[]);for(let e of a){let t=o.get(e.alias);S.get(t).push(e.alias)}for(let e of[...Ma,`__unknown__`]){let t=(S.get(e)??[]).slice().sort();if(t.length===0)continue;let r=t.reduce((e,t)=>Math.max(e,n.get(t)??fa),0);t.forEach((e,t)=>{_.set(e,{x:0+t*360,y:x})}),x+=r+ga}return{positions:_,levelOf:g}}function Ba(e){let t=e.connections??[],n=new Map,r=new Map;for(let e of t)n.set(e.source,(n.get(e.source)??0)+1),r.set(e.target,(r.get(e.target)??0)+1);let i=new Map(e.nodes.map(e=>[e.alias,Aa(Math.max(n.get(e.alias)??0,r.get(e.alias)??0))])),{positions:a,levelOf:o}=za(e.nodes,t,i),s=new Set;for(let[e,n]of t.entries()){let t=o.get(n.source),r=o.get(n.target);t!==void 0&&r!==void 0&&t>=r&&s.add(e)}let c=new Map,l=new Map;for(let t of e.nodes)c.set(t.alias,[]),l.set(t.alias,[]);for(let[e,n]of t.entries())s.has(e)?(l.get(n.source).push({connIndex:e,peerAlias:n.target,isBack:!0}),c.get(n.target).push({connIndex:e,peerAlias:n.source,isBack:!0})):(c.get(n.source).push({connIndex:e,peerAlias:n.target,isBack:!1}),l.get(n.target).push({connIndex:e,peerAlias:n.source,isBack:!1}));let u=e=>a.get(e)?.y??0;for(let e of c.values())e.sort((e,t)=>u(e.peerAlias)-u(t.peerAlias));for(let e of l.values())e.sort((e,t)=>u(e.peerAlias)-u(t.peerAlias));let d=new Map,p=new Map,m=e.nodes.map(e=>{let t=c.get(e.alias)??[],n=l.get(e.alias)??[],r=Aa(Math.max(t.length,n.length)),i=[],o=[],s=0,u=0;for(let e=0;e<t.length;e++){let n=t[e],r=ka(e,t.length);if(n.isBack){let e=Oa(u++);o.push({id:e,offset:r}),p.set(n.connIndex,e)}else{let e=Ta(s++);i.push({id:e,offset:r}),d.set(n.connIndex,e)}}let f=[],m=[],h=0,g=0;for(let e=0;e<n.length;e++){let t=n[e],r=ka(e,n.length);if(t.isBack){let e=Da(g++);m.push({id:e,offset:r}),d.set(t.connIndex,e)}else{let e=Ea(h++);f.push({id:e,offset:r}),p.set(t.connIndex,e)}}return{id:e.alias,type:e.types[0]??`default`,className:`nokey`,position:a.get(e.alias)??{x:0,y:0},width:da,height:r,style:na(e.types[0]??`unknown`),data:{alias:e.alias,nodeType:e.types[0]??`unknown`,properties:e.properties,sourceHandles:i,targetHandles:f,backSourceHandles:m,backTargetHandles:o,minHeight:r}}}),h=[];for(let[e,n]of t.entries()){let t=n.relations.map(e=>e.type),r=`${n.source}__${n.target}__${e}`,i=wa(t);h.push({id:r,source:n.source,target:n.target,sourceHandle:d.get(e),targetHandle:p.get(e),label:t.join(`, `),type:`bezier`,markerEnd:{type:f.ArrowClosed,width:16,height:16,color:_a},style:{stroke:_a,strokeWidth:2},labelStyle:{fill:i,fontSize:10,fontWeight:700},labelBgStyle:{fill:va,fillOpacity:.94,stroke:`rgba(15, 23, 42, 0.16)`,strokeWidth:1},labelBgPadding:[5,2],labelBgBorderRadius:6,data:{relationTypes:t}})}return{nodes:m,edges:h}}var Va=`application/x-minigraph-clipboard-item`;function Ha(e){return e.includes(Va)}function Ua(e,t){e.effectAllowed=`copy`,e.setData(Va,t)}function Wa(e){let t=e?.getData(`application/x-minigraph-clipboard-item`)??``;return t.trim()?t:null}function Ga(e,t){return e.nodes.find(e=>e.alias===t)}function Ka(e,t){return(e.connections??[]).filter(e=>e.source!==e.target&&(e.source===t||e.target===t))}var qa={toolbar:`_toolbar_117v8_2`,nameGroup:`_nameGroup_117v8_13`,graphName:`_graphName_117v8_20`,stats:`_stats_117v8_29`,toolbarActions:`_toolbarActions_117v8_49`,toolbarButton:`_toolbarButton_117v8_55`};function Ja({graphData:e,graphName:t,onCopySuccess:n,onCopyError:r,extraActions:i}){let a=(0,j.useCallback)(()=>{e&&navigator.clipboard.writeText(JSON.stringify(e,null,2)).then(()=>n?.()).catch(()=>r?.())},[e,n,r]),o=e?.nodes.length??0,s=(e?.connections??[]).length;return(0,M.jsxs)(`div`,{className:qa.toolbar,children:[(0,M.jsxs)(`div`,{className:qa.nameGroup,children:[(0,M.jsx)(`span`,{className:qa.graphName,children:t??`Untitled`}),(0,M.jsxs)(`span`,{className:qa.stats,children:[o,` node`,o===1?``:`s`,` · `,s,` connection`,s===1?``:`s`]})]}),(0,M.jsxs)(`div`,{className:qa.toolbarActions,children:[i,(0,M.jsx)(`button`,{className:qa.toolbarButton,onClick:a,title:`Copy raw graph JSON to clipboard`,"aria-label":`Copy raw graph JSON to clipboard`,children:`📑`})]})]})}var Ya={menu:`_menu_13qxg_1`,menuItem:`_menuItem_13qxg_12`};function Xa({open:e,x:t,y:n,canCreateNode:r,onCreateNode:i,onClose:a}){let o=(0,j.useRef)(null),s=(0,j.useRef)(null);return(0,j.useEffect)(()=>{if(!e)return;s.current?.focus();let t=e=>{o.current&&!o.current.contains(e.target)&&a()},n=e=>{e.key===`Escape`&&(e.preventDefault(),a())};return document.addEventListener(`pointerdown`,t),document.addEventListener(`keydown`,n),()=>{document.removeEventListener(`pointerdown`,t),document.removeEventListener(`keydown`,n)}},[e,a]),e?(0,M.jsx)(`div`,{ref:o,className:Ya.menu,style:{left:t,top:n},role:`menu`,"aria-label":`Graph actions`,children:(0,M.jsx)(`button`,{ref:s,role:`menuitem`,type:`button`,className:Ya.menuItem,disabled:!r,onClick:()=>{r&&(i(),a())},children:`Create Node`})}):null}var Za={menu:`_menu_1trgd_1`,menuItem:`_menuItem_1trgd_12`,dangerItem:`_dangerItem_1trgd_38`,confirmation:`_confirmation_1trgd_51`,confirmationText:`_confirmationText_1trgd_57`,confirmationActions:`_confirmationActions_1trgd_65`},Qa=8;function $a(e){let{open:t,x:n,y:r,onClose:i}=e,[a,o]=(0,j.useState)(!1),[s,c]=(0,j.useState)({left:n,top:r}),l=(0,j.useRef)(null),u=(0,j.useRef)(null),d=(0,j.useRef)(null),f=e.mode===`multi-node`?e.selectedCount:null,p=f!==null&&f>1,m=e.mode===`multi-node`?p&&e.canClipSelectedNodes:e.canClipNode,h=e.mode===`single-node`&&e.canEditNode,g=e.mode===`multi-node`?p&&e.canDeleteSelectedNodes:e.canDeleteNode,_=m||h||g,v=p?`${f} selected nodes`:e.mode===`single-node`?e.nodeAlias:``;return(0,j.useLayoutEffect)(()=>{t&&o(!1)},[t,v,n,r]),(0,j.useLayoutEffect)(()=>{if(!t)return;let e=l.current;if(!e){c({left:n,top:r});return}let i=e.getBoundingClientRect(),a=Math.max(Qa,window.innerWidth-i.width-Qa),o=Math.max(Qa,window.innerHeight-i.height-Qa);c({left:Math.min(Math.max(n,Qa),a),top:Math.min(Math.max(r,Qa),o)})},[m,g,h,a,t,v,n,r]),(0,j.useEffect)(()=>{if(!t){o(!1);return}a?d.current?.focus():u.current?.focus()},[a,t]),(0,j.useEffect)(()=>{if(!t)return;let e=e=>{l.current&&!l.current.contains(e.target)&&i()},n=e=>{e.key===`Escape`&&(e.preventDefault(),i())},r=()=>i();return document.addEventListener(`pointerdown`,e),document.addEventListener(`keydown`,n),window.addEventListener(`scroll`,r,!0),window.addEventListener(`resize`,r),()=>{document.removeEventListener(`pointerdown`,e),document.removeEventListener(`keydown`,n),window.removeEventListener(`scroll`,r,!0),window.removeEventListener(`resize`,r)}},[i,t]),!t||!_?null:(0,M.jsx)(`div`,{ref:l,className:Za.menu,style:{left:s.left,top:s.top},role:`menu`,"aria-label":p?`Actions for ${f} selected nodes`:`Node actions for ${v}`,children:a?(0,M.jsxs)(`div`,{className:Za.confirmation,role:`group`,"aria-label":`Confirm delete ${v}`,children:[(0,M.jsx)(`div`,{className:Za.confirmationText,children:p?`Delete ${f} selected nodes?`:`Delete "${v}"?`}),(0,M.jsxs)(`div`,{className:Za.confirmationActions,children:[(0,M.jsx)(`button`,{ref:d,type:`button`,className:`${Za.menuItem} ${Za.dangerItem}`,onClick:()=>{e.mode===`multi-node`?e.onDeleteSelectedNodes():e.onDeleteNode(),i()},children:`Delete`}),(0,M.jsx)(`button`,{type:`button`,className:Za.menuItem,onClick:()=>o(!1),children:`Cancel`})]})]}):(0,M.jsxs)(M.Fragment,{children:[m&&(0,M.jsx)(`button`,{ref:u,role:`menuitem`,type:`button`,className:Za.menuItem,onClick:()=>{e.mode===`multi-node`?e.onClipSelectedNodes():e.onClipNode(),i()},children:p?`Clip ${f} selected nodes to Workspace`:`Clip to Workspace`}),h&&e.mode===`single-node`&&(0,M.jsx)(`button`,{ref:m?void 0:u,role:`menuitem`,type:`button`,className:Za.menuItem,onClick:()=>{e.onEditNode(),i()},children:`Edit Node`}),g&&(0,M.jsx)(`button`,{ref:!m&&!h?u:void 0,role:`menuitem`,type:`button`,className:`${Za.menuItem} ${Za.dangerItem}`,onClick:()=>o(!0),children:p?`Delete ${f} selected nodes`:`Delete Node`})]})})}var eo={tip:`_tip_a9o1b_1`,fading:`_fading_a9o1b_11`,tipButton:`_tipButton_a9o1b_15`};function to({visible:e,fading:t,onDismiss:n}){return e?(0,M.jsx)(`div`,{className:`${eo.tip}${t?` ${eo.fading}`:``}`,role:`status`,children:(0,M.jsx)(`button`,{type:`button`,className:eo.tipButton,onClick:n,children:`Multi-select: Shift/Ctrl/Cmd + click nodes, or Shift + drag on the canvas.`})}):null}function no(e){return e.trim().toLowerCase()}function ro(e){let t=new Set;return e.filter(e=>{let n=no(e);return t.has(n)?!1:(t.add(n),!0)})}function io(e,t){let n=ro(t),r=no(e);return n.length>1&&n.some(e=>no(e)===r)?{kind:`multi-node`,aliases:n}:{kind:`single-node`,alias:e}}function ao(e,t){let n=new Map(t.nodes.map(e=>[no(e.alias),e]));return ro(e).map(e=>n.get(no(e))).filter(e=>e!==void 0)}var oo=[],so=[],co=[`Shift`,`Control`,`Meta`];function lo(e,t){return e.length===t.length&&e.every((e,n)=>e===t[n])}function uo({graphData:e,graphName:t,onCopySuccess:n,onCopyError:r,onRenderError:i,isRefreshing:a=!1,onClipNode:o,onClipNodes:s,onClipboardDrop:u,isConnected:d,supportsAuthoring:f=!1,onCreateNode:v,onEditNode:y,onDeleteNode:b,onDeleteNodes:x}){let[S,C]=(0,j.useState)(null),[ee,te]=(0,j.useState)(null),[w,ne]=(0,j.useState)([]),[T,re]=(0,j.useState)(!1),[ie,E]=(0,j.useState)(!1),[ae,oe]=(0,j.useState)(!1),se=(0,j.useRef)(0),ce=(0,j.useRef)(!1),D=(0,j.useRef)(null),O=!!(f&&v&&d),le=!!o,ue=!!s,de=!!(f&&y&&d),fe=!!(f&&b&&d),k=!!(f&&x&&d),A=le||de||fe,pe=ue||k,me=A||pe,he=!!(u&&d),ge=(0,j.useCallback)(()=>{se.current=0,re(!1)},[]);(0,j.useEffect)(()=>{if(!ee)return;let e=e=>{e.key===`Escape`&&te(null)},t=()=>te(null);return document.addEventListener(`keydown`,e),window.addEventListener(`scroll`,t,!0),window.addEventListener(`resize`,t),()=>{document.removeEventListener(`keydown`,e),window.removeEventListener(`scroll`,t,!0),window.removeEventListener(`resize`,t)}},[ee]),(0,j.useEffect)(()=>{let e=()=>ge();return window.addEventListener(`dragend`,e),window.addEventListener(`drop`,e),()=>{window.removeEventListener(`dragend`,e),window.removeEventListener(`drop`,e),ge()}},[ge]);let _e=(0,j.useRef)(i);(0,j.useEffect)(()=>{_e.current=i},[i]);let{nodes:ve,edges:ye,transformError:be}=(0,j.useMemo)(()=>{if(!e)return{nodes:oo,edges:so,transformError:null};try{return{...Ba(e),transformError:null}}catch(e){return{nodes:oo,edges:so,transformError:e instanceof Error?e.message:String(e)}}},[e]);(0,j.useEffect)(()=>{be&&_e.current?.(`Graph render failed: ${be}`)},[be]);let xe=(0,j.useMemo)(()=>e?JSON.stringify(e.nodes.map(e=>e.alias)):`empty`,[e]),[Se,Ce,we]=c(ve),[Te,Ee,De]=g(ye),Oe=!!(e&&e.nodes.length>0),ke=(0,j.useCallback)(({nodes:e})=>{let t=e.map(e=>e.data.alias);ne(e=>lo(e,t)?e:t)},[]);(0,j.useEffect)(()=>{Ce(ve),Ee(ye),ne([]),C(null)},[ve,ye,Ce,Ee]);let Ae=(0,j.useCallback)(()=>{!ie||ae||(oe(!0),D.current!==null&&clearTimeout(D.current),D.current=setTimeout(()=>{E(!1),D.current=null},400))},[ae,ie]);(0,j.useEffect)(()=>{!Oe||be||ce.current||(ce.current=!0,oe(!1),E(!0))},[Oe,be]),(0,j.useEffect)(()=>{if(!ie||ae)return;let e=setTimeout(Ae,5e3);return()=>clearTimeout(e)},[Ae,ae,ie]),(0,j.useEffect)(()=>()=>{D.current!==null&&clearTimeout(D.current)},[]);let je=e=>{he&&Ha(Array.from(e.dataTransfer.types))&&(e.preventDefault(),se.current+=1,re(!0))},Me=e=>{he&&Ha(Array.from(e.dataTransfer.types))&&(e.preventDefault(),e.dataTransfer.dropEffect=`copy`,re(!0))},Ne=e=>{Ha(Array.from(e.dataTransfer.types))&&(se.current=Math.max(0,se.current-1),se.current===0&&re(!1))},Pe=e=>{if(!he||!Ha(Array.from(e.dataTransfer.types)))return;e.preventDefault();let t=Wa(e.dataTransfer);ge(),t&&u?.(t)},Fe=S?.target.kind===`single-node`&&e?Ga(e,S.target.alias):null,Ie=S?.target.kind===`multi-node`?S.target.aliases:[],Le=e?ao(Ie,e):[];return be?(0,M.jsxs)(`div`,{className:la.empty,children:[(0,M.jsx)(`span`,{className:la.emptyIcon,children:`⚠️`}),(0,M.jsx)(`span`,{children:`Graph could not be rendered.`}),(0,M.jsx)(`span`,{children:be})]}):(0,M.jsx)(ua,{onRenderError:i,children:(0,M.jsxs)(`div`,{className:la.graphWrapper,"aria-busy":a,children:[Oe&&e&&(0,M.jsx)(Ja,{graphData:e,graphName:t,onCopySuccess:n,onCopyError:r}),(0,M.jsxs)(`div`,{className:la.graphSurface,onDragEnter:je,onDragOver:Me,onDragLeave:Ne,onDrop:Pe,onWheelCapture:Ae,children:[Oe?(0,M.jsxs)(m,{nodes:Se,edges:Te,onNodesChange:we,onEdgesChange:De,nodeTypes:ca,fitView:!0,fitViewOptions:{padding:.25},minZoom:.2,maxZoom:2.5,selectionKeyCode:`Shift`,multiSelectionKeyCode:co,selectionOnDrag:!1,selectionMode:l.Partial,proOptions:{hideAttribution:!1},onSelectionChange:ke,onNodeContextMenu:(e,t)=>{if(e.preventDefault(),e.stopPropagation(),Ae(),te(null),!me)return;let n=io(t.data.alias,w);n.kind===`single-node`&&w.length>1&&(Ce(e=>e.map(e=>({...e,selected:e.data.alias===t.data.alias}))),ne([t.data.alias])),C({x:e.clientX,y:e.clientY,target:n})},onPaneContextMenu:e=>{e.preventDefault(),Ae(),C(null),O&&te({x:e.clientX,y:e.clientY})},onPaneClick:()=>{Ae(),C(null),te(null)},onNodeClick:()=>Ae(),onNodeDragStart:()=>Ae(),onSelectionStart:()=>Ae(),onMoveStart:e=>{e&&Ae()},children:[(0,M.jsx)(_,{variant:p.Dots,gap:18,size:1,color:`rgba(255,255,255,0.07)`}),(0,M.jsx)(h,{showInteractive:!1})]}):(0,M.jsxs)(`div`,{className:la.empty,children:[(0,M.jsx)(`span`,{className:la.emptyIcon,children:`🕸️`}),(0,M.jsx)(`span`,{children:`No graph data yet.`}),(0,M.jsxs)(`span`,{children:[`Run `,(0,M.jsx)(`strong`,{children:`describe graph`}),` or `,(0,M.jsx)(`strong`,{children:`export graph`}),` in the playground.`]}),f&&v&&(0,M.jsxs)(M.Fragment,{children:[(0,M.jsx)(`button`,{type:`button`,className:la.emptyCreateButton,disabled:!d,onClick:()=>v(`empty-graph`),children:`Create Node`}),!d&&(0,M.jsx)(`span`,{className:la.emptyHint,children:`Connect WebSocket to create a node.`})]})]}),(0,M.jsx)(to,{visible:ie,fading:ae,onDismiss:Ae}),a&&(0,M.jsx)(`div`,{className:la.refreshingOverlay,children:(0,M.jsx)(`div`,{className:la.refreshingSpinner,role:`status`,"aria-label":`Graph refreshing`})}),T&&(0,M.jsx)(`div`,{className:la.clipboardDropOverlay,children:(0,M.jsx)(`div`,{className:la.clipboardDropMessage,children:`Drop to paste workspace node`})}),(0,M.jsx)(Xa,{open:ee!==null,x:ee?.x??0,y:ee?.y??0,canCreateNode:O,onCreateNode:()=>v?.(`pane-context-menu`),onClose:()=>te(null)}),S?.target.kind===`multi-node`?(0,M.jsx)($a,{mode:`multi-node`,open:Le.length>1&&pe,x:S.x,y:S.y,selectedCount:Ie.length,canClipSelectedNodes:ue,canDeleteSelectedNodes:k,onClipSelectedNodes:()=>{if(!e){s?.([]);return}let t=Le.map(t=>({node:t,connections:Ka(e,t.alias)}));s?.(t)},onDeleteSelectedNodes:()=>{let e=Le.length===Ie.length;x?.(e?Le:[])},onClose:()=>C(null)}):(0,M.jsx)($a,{mode:`single-node`,open:S!==null&&Fe!==null&&A,x:S?.x??0,y:S?.y??0,nodeAlias:S?.target.kind===`single-node`?S.target.alias:``,canClipNode:le&&Fe!==null,canEditNode:de&&Fe!==null,canDeleteNode:fe&&Fe!==null,onClipNode:()=>{if(!Fe||!e)return;let t=Ka(e,Fe.alias);o?.(Fe,t)},onEditNode:()=>{Fe&&y?.(Fe)},onDeleteNode:()=>{Fe&&b?.(Fe)},onClose:()=>C(null)})]})]})},xe)}var fo={root:`_root_1yhjs_2`,empty:`_empty_1yhjs_10`,emptyIcon:`_emptyIcon_1yhjs_23`,toolbarButton:`_toolbarButton_1yhjs_29 _toolbarButton_117v8_55`,scrollBody:`_scrollBody_1yhjs_34`,jsonContainer:`_jsonContainer_1yhjs_45`,jsonLabel:`_jsonLabel_1yhjs_46`,jsonString:`_jsonString_1yhjs_47`,jsonNumber:`_jsonNumber_1yhjs_48`,jsonBoolean:`_jsonBoolean_1yhjs_49`,jsonNull:`_jsonNull_1yhjs_50`},po={default:e=>e<3,all:i,none:a};function mo({graphData:e,graphName:t,onCopySuccess:n,onCopyError:i}){let[a,s]=(0,j.useState)(`all`);return e?(0,M.jsxs)(`div`,{className:fo.root,children:[(0,M.jsx)(Ja,{graphData:e,graphName:t,onCopySuccess:n,onCopyError:i,extraActions:(0,M.jsxs)(M.Fragment,{children:[(0,M.jsx)(`button`,{className:fo.toolbarButton,onClick:()=>s(`all`),title:`Expand all nodes`,"aria-label":`Expand all JSON nodes`,"aria-pressed":a===`all`,children:`➖`}),(0,M.jsx)(`button`,{className:fo.toolbarButton,onClick:()=>s(`none`),title:`Collapse all nodes`,"aria-label":`Collapse all JSON nodes`,"aria-pressed":a===`none`,children:`➕`})]})}),(0,M.jsx)(`div`,{className:fo.scrollBody,children:(0,M.jsx)(o,{data:e,shouldExpandNode:po[a],style:{...r,container:`${r.container} ${fo.jsonContainer}`,label:fo.jsonLabel,stringValue:fo.jsonString,numberValue:fo.jsonNumber,booleanValue:fo.jsonBoolean,nullValue:fo.jsonNull}})})]}):(0,M.jsx)(`div`,{className:fo.root,children:(0,M.jsxs)(`div`,{className:fo.empty,children:[(0,M.jsx)(`span`,{className:fo.emptyIcon,children:`🕸️`}),(0,M.jsx)(`span`,{children:`No graph data yet.`}),(0,M.jsx)(`span`,{children:`Pin a graph-link message in the Console to load the raw data here.`})]})})}var ho={rightPanel:`_rightPanel_1l0x1_2`,tabStrip:`_tabStrip_1l0x1_10`,tab:`_tab_1l0x1_10`,tabActive:`_tabActive_1l0x1_38`,tabBadge:`_tabBadge_1l0x1_42`,tabBody:`_tabBody_1l0x1_48`,tabBodyHidden:`_tabBodyHidden_1l0x1_61`,graphContent:`_graphContent_1l0x1_65`,rightPanelGroup:`_rightPanelGroup_1l0x1_72`,verticalResizeHandle:`_verticalResizeHandle_1l0x1_80`},go=`help-split-percent`,L=`help-split-maximized`,R=45,_o=98;function vo({tabs:e,payload:t,onChange:n,validation:r,onFormat:i,onUpload:a,graphData:o,graphName:s,activeTab:c,onTabChange:l,onGraphRenderError:u,onGraphDataCopySuccess:d,onGraphDataCopyError:f,isGraphRefreshing:p,onClipNode:m,onClipNodes:h,onClipboardDrop:g,isConnected:_,supportsAuthoring:v,onCreateNode:y,onEditNode:b,onDeleteNode:x,onDeleteNodes:ee,helpPanel:w}){let ne=(0,j.useId)(),T=`${ne}-tab-payload`,re=`${ne}-tab-graph`,ie=`${ne}-tab-graph-data`,E=(0,M.jsxs)(`div`,{className:ho.rightPanel,children:[(0,M.jsxs)(`div`,{className:ho.tabStrip,role:`tablist`,"aria-label":`Right panel tabs`,children:[e.includes(`payload`)&&(0,M.jsx)(`button`,{role:`tab`,"aria-selected":c===`payload`,"aria-controls":T,className:`${ho.tab}${c===`payload`?` ${ho.tabActive}`:``}`,onClick:()=>l(`payload`),children:`Payload Editor`}),e.includes(`graph`)&&(0,M.jsxs)(`button`,{role:`tab`,"aria-selected":c===`graph`,"aria-controls":re,className:`${ho.tab}${c===`graph`?` ${ho.tabActive}`:``}`,onClick:()=>l(`graph`),children:[`Graph`,o!==null&&(0,M.jsx)(`span`,{className:ho.tabBadge,"aria-label":`Graph data available`,children:`🕸️`})]}),e.includes(`graph-data`)&&(0,M.jsx)(`button`,{role:`tab`,"aria-selected":c===`graph-data`,"aria-controls":ie,className:`${ho.tab}${c===`graph-data`?` ${ho.tabActive}`:``}`,onClick:()=>l(`graph-data`),children:`Graph Data (Raw)`})]}),e.includes(`payload`)&&(0,M.jsx)(`div`,{role:`tabpanel`,id:T,tabIndex:c===`payload`?0:-1,className:`${ho.tabBody}${c===`payload`?``:` ${ho.tabBodyHidden}`}`,children:(0,M.jsx)(Xi,{payload:t,onChange:n,validation:r,onFormat:i,onUpload:a})}),e.includes(`graph`)&&(0,M.jsx)(`div`,{role:`tabpanel`,id:re,tabIndex:c===`graph`?0:-1,className:`${ho.tabBody}${c===`graph`?``:` ${ho.tabBodyHidden}`}`,children:(0,M.jsx)(`div`,{className:ho.graphContent,children:(0,M.jsx)(uo,{graphData:o,graphName:s,onRenderError:u,isRefreshing:p,onCopySuccess:d,onCopyError:f,onClipNode:m,onClipNodes:h,onClipboardDrop:g,isConnected:_,supportsAuthoring:v,onCreateNode:y,onEditNode:b,onDeleteNode:x,onDeleteNodes:ee})})}),e.includes(`graph-data`)&&(0,M.jsx)(`div`,{role:`tabpanel`,id:ie,tabIndex:c===`graph-data`?0:-1,className:`${ho.tabBody}${c===`graph-data`?``:` ${ho.tabBodyHidden}`}`,children:(0,M.jsx)(mo,{graphData:o,graphName:s,onCopySuccess:d,onCopyError:f})})]}),ae=(0,j.useRef)(Number(sessionStorage.getItem(go))||R),oe=(0,j.useRef)(null),se=(0,j.useRef)(null),[ce,D]=(0,j.useState)(()=>sessionStorage.getItem(L)===`1`),O=(0,j.useRef)(ce),le=(0,j.useCallback)(e=>{let t=e[`help-split-help`];if(t===void 0)return;let n=t>=_o;n!==O.current&&(O.current=n,D(n),sessionStorage.setItem(L,n?`1`:`0`)),n||(ae.current=t,sessionStorage.setItem(go,String(t)))},[]),ue=(0,j.useCallback)(()=>{let e=!O.current;if(O.current=e,D(e),sessionStorage.setItem(L,e?`1`:`0`),e)se.current?.resize(`0%`),oe.current?.resize(`100%`);else{let e=ae.current;oe.current?.resize(`${e}%`),se.current?.resize(`${100-e}%`)}},[]),de=!!w;if((0,j.useEffect)(()=>{de&&O.current&&requestAnimationFrame(()=>{se.current?.resize(`0%`),oe.current?.resize(`100%`)})},[de]),!w)return E;let fe=typeof w==`function`?w(ue,ce):w,k=O.current?100:ae.current,A=100-k;return(0,M.jsxs)(te,{orientation:`vertical`,className:ho.rightPanelGroup,onLayoutChanged:le,children:[(0,M.jsx)(C,{panelRef:se,defaultSize:`${A}%`,minSize:`0%`,children:E}),(0,M.jsx)(S,{className:ho.verticalResizeHandle,"aria-label":`Resize help panel`}),(0,M.jsx)(C,{id:`help-split-help`,panelRef:oe,defaultSize:`${k}%`,minSize:`15%`,children:fe})]})}var yo=class extends j.Component{constructor(...e){super(...e),this.state={hasError:!1}}static getDerivedStateFromError(){return{hasError:!0}}componentDidCatch(e,t){console.error(`[ConsoleErrorBoundary] Failed to render message:`,e,t.componentStack)}render(){return this.state.hasError?(0,M.jsx)(`span`,{children:this.props.fallback}):this.props.children}},bo=2e3,xo=(e={})=>{let{onSuccess:t,onError:n}=e,[r,i]=(0,j.useState)(!1),a=(0,j.useRef)(null);return(0,j.useEffect)(()=>()=>{a.current!==null&&clearTimeout(a.current)},[]),{copy:(0,j.useCallback)(async e=>{if(!navigator.clipboard)return console.warn(`useCopyToClipboard: Clipboard API not available in this browser.`),n?.(),!1;try{return await navigator.clipboard.writeText(e),i(!0),a.current!==null&&clearTimeout(a.current),a.current=setTimeout(()=>{a.current=null,i(!1)},bo),t?.(),!0}catch(e){return console.error(`useCopyToClipboard: Failed to write to clipboard.`,e),n?.(),!1}},[t,n]),copied:r}},z={consoleRoot:`_consoleRoot_1lgp1_2`,consoleHeader:`_consoleHeader_1lgp1_10`,consoleTitle:`_consoleTitle_1lgp1_20`,consoleControls:`_consoleControls_1lgp1_25`,controlButton:`_controlButton_1lgp1_30`,console:`_console_1lgp1_2`,emptyConsole:`_emptyConsole_1lgp1_67`,consoleMessage:`_consoleMessage_1lgp1_80`,consoleMessageActivatable:`_consoleMessageActivatable_1lgp1_94`,consoleMessageGraphLink:`_consoleMessageGraphLink_1lgp1_104`,consoleMessageLargePayload:`_consoleMessageLargePayload_1lgp1_115`,consoleMessageMockUpload:`_consoleMessageMockUpload_1lgp1_122`,uploadMockButton:`_uploadMockButton_1lgp1_131`,copyButton:`_copyButton_1lgp1_172`,copyButtonCopied:`_copyButtonCopied_1lgp1_225`,sendToJsonPathButton:`_sendToJsonPathButton_1lgp1_234`,messageIcon:`_messageIcon_1lgp1_268`,messageContent:`_messageContent_1lgp1_272`,messageText:`_messageText_1lgp1_278`,messageTime:`_messageTime_1lgp1_283`,"messageType-error":`_messageType-error_1lgp1_290`,"messageType-info":`_messageType-info_1lgp1_291`,"messageType-welcome":`_messageType-welcome_1lgp1_292`,jsonViewWrapper:`_jsonViewWrapper_1lgp1_295`,jsonContainer:`_jsonContainer_1lgp1_301`,jsonLabel:`_jsonLabel_1lgp1_302`,jsonString:`_jsonString_1lgp1_303`,jsonNumber:`_jsonNumber_1lgp1_304`,jsonBoolean:`_jsonBoolean_1lgp1_305`,jsonNull:`_jsonNull_1lgp1_306`};function So({message:e,msgId:t,classificationMap:n,onGraphLink:i,onCopyMessage:a,onSendToJsonPath:s,onUploadMockData:c,successfulUploadPaths:l}){let u=Dr(e),d=Or(u.type),f=kr(u.message),p=(t===void 0?void 0:n?.get(t))??[],m=p.some(e=>e.kind===`graph.link`),h=p.some(e=>e.kind===`payload.large`),g=p.some(e=>e.kind===`upload.invitation`),_=p.find(e=>e.kind===`upload.invitation`)?.uploadPath??null,v=!!c&&g&&_!==null,y=v&&!!l?.has(_),b=!!i&&m&&!g&&!h,x=!!s&&f.isJSON,{copy:S,copied:C}=xo({onSuccess:a}),ee=t=>{t.stopPropagation(),S(e)},te=t=>{(t.key===`Enter`||t.key===` `)&&(t.preventDefault(),t.stopPropagation(),S(e))},w=e=>{e.stopPropagation(),!(!s||!f.isJSON)&&s(JSON.stringify(f.data,null,2))},ne=e=>{e.stopPropagation(),!(!c||!_)&&c(_)};return(0,M.jsxs)(`div`,{className:[z.consoleMessage,z[`messageType-${u.type}`],b?z.consoleMessageActivatable:``,m?z.consoleMessageGraphLink:``,h?z.consoleMessageLargePayload:``,g?z.consoleMessageMockUpload:``].filter(Boolean).join(` `),onClick:b?()=>i():void 0,title:b?`Click to load graph in Graph View`:void 0,role:b?`button`:void 0,tabIndex:b?0:void 0,onKeyDown:b?e=>{(e.key===`Enter`||e.key===` `)&&(e.preventDefault(),i())}:void 0,"aria-label":b?`Load graph in Graph View`:void 0,children:[(0,M.jsx)(`span`,{className:z.messageIcon,children:g?`⬆️`:h?`⬇️`:m?`🕸️`:d}),(0,M.jsx)(`div`,{className:z.messageContent,children:f.isJSON?(0,M.jsx)(`div`,{className:z.jsonViewWrapper,children:(0,M.jsx)(o,{data:f.data,shouldExpandNode:e=>e<1,style:{...r,container:`${r.container} ${z.jsonContainer}`,label:z.jsonLabel,stringValue:z.jsonString,numberValue:z.jsonNumber,booleanValue:z.jsonBoolean,nullValue:z.jsonNull}})}):(0,M.jsxs)(`span`,{className:z.messageText,children:[u.message,y&&(0,M.jsx)(`span`,{title:`Upload succeeded`,children:` ✅`})]})}),(0,M.jsx)(`button`,{className:`${z.copyButton} ${C?z.copyButtonCopied:``}`,onClick:ee,onKeyDown:te,title:C?`Copied!`:`Copy message`,"aria-label":C?`Copied to clipboard`:`Copy message to clipboard`,tabIndex:0,children:C?`✅`:`📄`}),x&&(0,M.jsx)(`button`,{className:z.sendToJsonPathButton,onClick:w,onKeyDown:e=>{(e.key===`Enter`||e.key===` `)&&w(e)},title:`Open in JSON-Path Playground`,"aria-label":`Open this JSON in the JSON-Path Playground`,tabIndex:0,children:`➡️`}),v&&(0,M.jsx)(`button`,{className:z.uploadMockButton,onClick:ne,onKeyDown:e=>{(e.key===`Enter`||e.key===` `)&&ne(e)},title:`Re-open upload dialog`,"aria-label":`Re-open mock data upload dialog`,tabIndex:0,children:`⬆️ Upload JSON…`}),u.time&&(0,M.jsx)(`span`,{className:z.messageTime,children:u.time})]})}function Co({messages:e,classificationMap:t,onCopy:n,onClear:r,consoleRef:i,onGraphLinkMessage:a,onCopyMessage:o,onSendToJsonPath:s,onUploadMockData:c,successfulUploadPaths:l}){return(0,M.jsxs)(`div`,{className:z.consoleRoot,children:[(0,M.jsxs)(`div`,{className:z.consoleHeader,children:[(0,M.jsx)(`span`,{className:z.consoleTitle,children:`Console Output`}),(0,M.jsxs)(`div`,{className:z.consoleControls,children:[(0,M.jsx)(`button`,{className:z.controlButton,onClick:n,title:`Copy console output`,"aria-label":`Copy console output to clipboard`,children:`📑`}),(0,M.jsx)(`button`,{className:z.controlButton,onClick:r,title:`Clear console`,"aria-label":`Clear console`,children:`🗑️`})]})]}),(0,M.jsxs)(`div`,{className:z.console,ref:i,role:`log`,"aria-live":`polite`,children:[e.map(e=>(0,M.jsx)(yo,{fallback:e.raw,children:(0,M.jsx)(So,{message:e.raw,msgId:e.id,classificationMap:t,onGraphLink:a?()=>a(e):void 0,onCopyMessage:o,onSendToJsonPath:s,onUploadMockData:c,successfulUploadPaths:l})},e.id)),e.length===0&&(0,M.jsxs)(`div`,{className:z.emptyConsole,children:[`No messages yet. Use the `,(0,M.jsx)(`strong`,{children:`Start`}),` button in the header to connect.`]})]})]})}var B={commandInput:`_commandInput_j85f1_2`,labelRow:`_labelRow_j85f1_8`,labelGroup:`_labelGroup_j85f1_16`,label:`_label_j85f1_8`,infoWrapper:`_infoWrapper_j85f1_28`,paletteToggle:`_paletteToggle_j85f1_34`,paletteToggleActive:`_paletteToggleActive_j85f1_66`,popover:`_popover_j85f1_73`,popoverOpen:`_popoverOpen_j85f1_95`,popoverTitle:`_popoverTitle_j85f1_121`,popoverRow:`_popoverRow_j85f1_135`,popoverKeyword:`_popoverKeyword_j85f1_156`,popoverDesc:`_popoverDesc_j85f1_168`,popoverAlias:`_popoverAlias_j85f1_174`,inputRow:`_inputRow_j85f1_181`,inputWrapper:`_inputWrapper_j85f1_187`,textarea:`_textarea_j85f1_197`,sendButton:`_sendButton_j85f1_226`,hint:`_hint_j85f1_243`,dropup:`_dropup_j85f1_251`,dropupHeader:`_dropupHeader_j85f1_266`,dropupItem:`_dropupItem_j85f1_282`,dropupItemText:`_dropupItemText_j85f1_305`,matchHighlight:`_matchHighlight_j85f1_313`,multilineIndicator:`_multilineIndicator_j85f1_319`},wo=[`graph.data.mapper`,`graph.math`,`graph.js`,`graph.api.fetcher`,`graph.extension`,`graph.island`,`graph.join`],To=[{keyword:`help`,description:`List all help topics, or get help for a specific command`,template:`help`},{keyword:`create`,description:`Create a new graph node`,template:`create node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:`update`,description:`Update an existing node`,template:`update node {name}
with type {type}
with properties
{key}={value}`,multiline:!0},{keyword:`edit`,description:`Print raw node data ready for editing and re-submitting`,template:`edit node {name}`},{keyword:`delete node`,description:`Delete a node by name`,alias:`clear node`,template:`delete node {name}`},{keyword:`delete connection`,description:`Delete connection(s) between two nodes`,alias:`clear connection`,template:`delete connection {nodeA} and {nodeB}`},{keyword:`delete cache`,description:`Clear cached API fetcher results`,alias:`clear cache`,template:`delete cache`},{keyword:`connect`,description:`Connect two nodes with a named relation`,template:`connect {node-A} to {node-B} with {relation}`},{keyword:`list nodes`,description:`List all nodes in the current graph`,template:`list nodes`},{keyword:`list connections`,description:`List all connections in the current graph`,template:`list connections`},{keyword:`describe graph`,description:`Describe the current graph model`,template:`describe graph`},{keyword:`describe node`,description:`Describe a specific node and its connections`,template:`describe node {name}`},{keyword:`describe connection`,description:`Describe connection(s) between two nodes`,template:`describe connection {nodeA} and {nodeB}`},{keyword:`describe skill`,description:`Show documentation for a skill by route name`,template:`describe skill {skill.route}`},{keyword:`export`,description:`Export the graph model to a JSON file`,template:`export graph as {name}`},{keyword:`import graph`,description:`Import a graph model from a saved file`,template:`import graph from {name}`},{keyword:`import node`,description:`Import a single node from another saved graph`,template:`import node {node-name} from {graph-name}`},{keyword:`instantiate`,description:`Create a runnable graph instance with mock input`,alias:`start`,template:`instantiate graph
{constant} -> input.body.{key}`,multiline:!0},{keyword:`upload mock data`,description:`Print the URL to POST a JSON payload as mock input.body`,template:`upload mock data`},{keyword:`execute`,description:`Execute a single node skill in isolation`,template:`execute node {name}`},{keyword:`inspect`,description:`Inspect a state-machine variable`,template:`inspect {variable_name}`},{keyword:`run`,description:`Run the graph instance from root to end`,template:`run`}];[...wo.map(e=>({tokens:[`describe`,`skill`,e],template:`describe skill ${e}`,hint:`Describe built-in skill: ${e}`}))];function Eo(e,t){let[n,r]=(0,j.useState)(!1),[i,a]=(0,j.useState)(-1),o=(0,j.useMemo)(()=>{let n=t.trimStart();if(n.length===0)return[];let r=n.toLowerCase(),i=e.filter(e=>e.toLowerCase().startsWith(r)),a=new Set;return i.filter(e=>a.has(e)?!1:(a.add(e),!0)).slice(0,8)},[e,t]),s=()=>{r(!0),a(-1)},c=e=>{let t=o.length;t!==0&&a(n=>e===1?n<0?0:(n+1)%t:n<=0?t-1:n-1)},l=(e,t)=>{e>=0&&e<o.length&&t(o[e]),r(!1),a(-1)};return{suggestions:o,isOpen:n,activeIndex:i,onCommandChange:s,navigate:c,accept:l,onTab:e=>{!n||o.length===0||l(i>=0?i:0,e)},dismiss:()=>{r(!1),a(-1)}}}var Do=e=>(0,M.jsxs)(`svg`,{xmlns:`http://www.w3.org/2000/svg`,viewBox:`0 0 16 16`,fill:`none`,width:14,height:14,stroke:`currentColor`,strokeWidth:1.5,strokeLinecap:`round`,strokeLinejoin:`round`,...e,children:[(0,M.jsx)(`polyline`,{points:`2,4 6,8 2,12`}),(0,M.jsx)(`line`,{x1:7,y1:12,x2:14,y2:12})]});function Oo({command:e,onChange:t,onKeyDown:n,onSend:r,sendDisabled:i,disabled:a,history:o}){let s=(0,j.useRef)(null),c=(0,j.useRef)(null),l=(0,j.useRef)(null),[u,d]=(0,j.useState)(!1);(0,j.useEffect)(()=>{if(!u)return;let e=e=>{c.current&&!c.current.contains(e.target)&&d(!1)};return document.addEventListener(`mousedown`,e),()=>document.removeEventListener(`mousedown`,e)},[u]);let f=Eo(o,e);(0,j.useEffect)(()=>{let e=s.current;e&&(e.style.height=`auto`,e.style.height=`${e.scrollHeight}px`)},[e]);let p=a?`Not connected`:`Enter command (Enter to send · Shift+Enter for new line)`,m=a?`Enter your test message once it is connected`:`Enter to send · Shift+Enter for new line · ↑↓ for history`;return(0,M.jsxs)(`div`,{className:B.commandInput,children:[(0,M.jsx)(`div`,{className:B.labelRow,children:(0,M.jsxs)(`div`,{className:B.labelGroup,children:[(0,M.jsx)(`label`,{htmlFor:`command`,className:B.label,children:`Command`}),(0,M.jsxs)(`span`,{ref:c,className:B.infoWrapper,children:[(0,M.jsx)(`button`,{type:`button`,className:`${B.paletteToggle}${u?` ${B.paletteToggleActive}`:``}`,"aria-label":`Toggle command palette`,"aria-expanded":u,"aria-controls":`command-palette`,onClick:()=>d(e=>!e),onKeyDown:e=>{e.key===`ArrowDown`&&u&&(e.preventDefault(),(l.current?.querySelector(`[role="option"]`))?.focus())},title:`Command palette`,children:(0,M.jsx)(Do,{"aria-hidden":`true`,focusable:`false`})}),(0,M.jsxs)(`div`,{id:`command-palette`,ref:l,className:`${B.popover}${u?` ${B.popoverOpen}`:``}`,role:`listbox`,"aria-label":`Command palette`,onKeyDown:e=>{if(e.key===`ArrowDown`||e.key===`ArrowUp`){e.preventDefault();let t=l.current?.querySelectorAll(`[role="option"]`);if(!t||t.length===0)return;let n=Array.from(t).indexOf(document.activeElement);e.key===`ArrowDown`?t[n<0?0:(n+1)%t.length].focus():t[n<=0?t.length-1:n-1].focus()}else e.key===`Escape`&&(e.preventDefault(),d(!1),s.current?.focus())},children:[(0,M.jsx)(`p`,{className:B.popoverTitle,children:`Command palette — click to insert`}),To.map(({keyword:e,alias:n,description:r,template:i})=>(0,M.jsxs)(`div`,{className:B.popoverRow,role:`option`,"aria-selected":!1,tabIndex:u?0:-1,onMouseDown:e=>e.preventDefault(),onClick:()=>{t(i),d(!1),s.current?.focus()},onKeyDown:e=>{(e.key===`Enter`||e.key===` `)&&(e.preventDefault(),t(i),d(!1),s.current?.focus())},children:[(0,M.jsx)(`span`,{className:B.popoverKeyword,children:e}),(0,M.jsxs)(`span`,{className:B.popoverDesc,children:[r,n&&(0,M.jsxs)(`span`,{className:B.popoverAlias,children:[` · alias: `,n]})]})]},e))]})]})]})}),(0,M.jsxs)(`div`,{className:B.inputRow,children:[(0,M.jsxs)(`div`,{className:B.inputWrapper,children:[(0,M.jsxs)(`div`,{id:`history-dropup`,role:`listbox`,"aria-label":`Command history suggestions`,className:B.dropup,hidden:!(f.isOpen&&f.suggestions.length>0),children:[(0,M.jsx)(`div`,{className:B.dropupHeader,"aria-hidden":`true`,children:`Recent Commands`}),f.isOpen&&f.suggestions.length>0&&f.suggestions.map((n,r)=>{let i=n.split(`
`)[0],a=n.includes(`
`),o=e.trimStart().split(`
`)[0],c=Math.min(o.length,i.length),l=i.slice(0,c),u=i.slice(c);return(0,M.jsxs)(`div`,{id:`history-option-${r}`,role:`option`,"aria-selected":r===f.activeIndex,className:B.dropupItem,onMouseDown:e=>e.preventDefault(),onClick:()=>{f.accept(r,e=>t(e)),requestAnimationFrame(()=>{let e=s.current;e&&(e.selectionStart=e.selectionEnd=e.value.length)})},children:[(0,M.jsxs)(`span`,{className:B.dropupItemText,children:[c>0&&(0,M.jsx)(`strong`,{className:B.matchHighlight,children:l}),u,a?`…`:``]}),a&&(0,M.jsx)(`span`,{className:B.multilineIndicator,"aria-label":`multi-line command`,children:`↵`})]},n)})]}),(0,M.jsx)(`textarea`,{ref:s,id:`command`,role:`combobox`,"aria-expanded":f.isOpen&&f.suggestions.length>0,"aria-haspopup":`listbox`,"aria-controls":`history-dropup`,"aria-activedescendant":f.isOpen&&f.suggestions.length>0&&f.activeIndex>=0?`history-option-${f.activeIndex}`:void 0,"aria-autocomplete":`list`,className:B.textarea,rows:1,placeholder:p,value:e,disabled:a,onChange:e=>{t(e.target.value),f.onCommandChange()},onKeyDown:e=>{if(e.key===`Tab`){e.preventDefault(),f.isOpen&&f.suggestions.length>0&&(f.onTab(e=>t(e)),requestAnimationFrame(()=>{let e=s.current;e&&(e.selectionStart=e.selectionEnd=e.value.length)}));return}if(e.key===`Enter`){if(e.shiftKey)return;if(e.preventDefault(),f.isOpen&&f.activeIndex>=0){f.accept(f.activeIndex,e=>t(e)),requestAnimationFrame(()=>{let e=s.current;e&&(e.selectionStart=e.selectionEnd=e.value.length)}),s.current?.focus();return}r(),s.current?.focus();return}if(e.key===`Escape`){if(f.isOpen){f.dismiss(),e.preventDefault();return}return}if(e.key===`ArrowUp`||e.key===`ArrowDown`){if(f.isOpen&&f.suggestions.length>0){e.preventDefault(),f.navigate(e.key===`ArrowDown`?1:-1);return}let t=s.current;if(t){let{selectionStart:n,value:r}=t,i=!r.slice(0,n).includes(`
`),a=!r.slice(n).includes(`
`);if(!(e.key===`ArrowUp`&&i||e.key===`ArrowDown`&&a))return}n(e),requestAnimationFrame(()=>{let e=s.current;e&&(e.selectionStart=e.selectionEnd=e.value.length)});return}n(e)},onBlur:()=>f.dismiss(),autoComplete:`off`,autoCorrect:`off`,spellCheck:!1})]}),(0,M.jsx)(`button`,{className:B.sendButton,onClick:()=>{r(),s.current?.focus()},disabled:i,"aria-label":`Send command`,children:`Send`})]}),m&&(0,M.jsx)(`p`,{className:B.hint,children:m})]})}var ko={root:`_root_1ac49_1`};function Ao({messages:e,classificationMap:t,onCopy:n,onClear:r,consoleRef:i,onGraphLinkMessage:a,onCopyMessage:o,onSendToJsonPath:s,onUploadMockData:c,successfulUploadPaths:l,command:u,onCommandChange:d,onCommandKeyDown:f,onSend:p,sendDisabled:m,inputDisabled:h,commandHistory:g}){return(0,M.jsxs)(`div`,{className:ko.root,children:[(0,M.jsx)(Co,{messages:e,classificationMap:t,onCopy:n,onClear:r,consoleRef:i,onGraphLinkMessage:a,onCopyMessage:o,onSendToJsonPath:s,onUploadMockData:c,successfulUploadPaths:l}),(0,M.jsx)(Oo,{command:u,onChange:d,onKeyDown:f,onSend:p,disabled:h,sendDisabled:m,history:g})]})}var V={dialog:`_dialog_g80bk_4`,modalInner:`_modalInner_g80bk_26`,modalHeader:`_modalHeader_g80bk_34`,modalTitleGroup:`_modalTitleGroup_g80bk_44`,modalTitle:`_modalTitle_g80bk_44`,modalPath:`_modalPath_g80bk_57`,closeButton:`_closeButton_g80bk_64`,modalBody:`_modalBody_g80bk_95`,dropZone:`_dropZone_g80bk_105`,dropZoneActive:`_dropZoneActive_g80bk_127`,dropZoneIcon:`_dropZoneIcon_g80bk_133`,dropZoneText:`_dropZoneText_g80bk_139`,dropZoneOr:`_dropZoneOr_g80bk_152`,browseButton:`_browseButton_g80bk_159`,fileInputHidden:`_fileInputHidden_g80bk_188`,fileError:`_fileError_g80bk_193`,textareaLabel:`_textareaLabel_g80bk_198`,textarea:`_textarea_g80bk_198`,validationError:`_validationError_g80bk_226`,keyboardHint:`_keyboardHint_g80bk_231`,errorBanner:`_errorBanner_g80bk_236`,modalFooter:`_modalFooter_g80bk_247`,footerActions:`_footerActions_g80bk_257`,formatButton:`_formatButton_g80bk_263`,cancelButton:`_cancelButton_g80bk_264`,uploadButton:`_uploadButton_g80bk_265`,spinner:`_spinner_g80bk_332`,spin:`_spin_g80bk_332`};function jo({uploadPath:e,json:t,onSuccess:n,onError:r}){let[i,a]=(0,j.useState)(!1),o=(0,j.useRef)(null),s=(0,j.useCallback)(()=>{o.current?.abort(),o.current=null,a(!1)},[]);return{isUploading:i,upload:(0,j.useCallback)(async()=>{o.current?.abort();let i=new AbortController;o.current=i,a(!0);try{let o=await fetch(e,{method:`POST`,headers:{"Content-Type":`application/json`},body:t,signal:i.signal}),s=await o.text();if(!o.ok){a(!1),r(`HTTP ${o.status} — ${s}`);return}a(!1),n(s)}catch(e){if(e.name===`AbortError`){a(!1);return}a(!1),r(e.message??`Network error`)}},[e,t,n,r]),cancel:s}}var Mo=(navigator.userAgentData?.platform??navigator.platform).toLowerCase().includes(`mac`);function No(e){return new Promise((t,n)=>{let r=new FileReader;r.onload=()=>t(r.result),r.onerror=()=>n(Error(`Could not read file "${e.name}"`)),r.readAsText(e,`utf-8`)})}function Po(e){let t=e.name.toLowerCase().endsWith(`.json`),n=e.type===`application/json`||e.type===`text/plain`;return!t&&!n?`"${e.name}" does not appear to be a JSON file. Only .json files are accepted.`:null}function Fo({uploadPath:e,onSuccess:t,onClose:n,onError:r}){let[i,a]=(0,j.useState)(``),[o,s]=(0,j.useState)(null),[c,l]=(0,j.useState)(null),[u,d]=(0,j.useState)(!1),f=(0,j.useRef)(null),p=(0,j.useRef)(null),m=(0,j.useRef)(null),h=kr(i).isJSON,g=h&&i.trim()!==``,{isUploading:_,upload:v,cancel:y}=jo({uploadPath:e,json:i,onSuccess:t,onError:e=>{s(e),r(e)}});(0,j.useEffect)(()=>{let e=f.current;if(e)return e.open||e.showModal(),p.current?.focus(),()=>{e.open&&e.close()}},[]);let b=(0,j.useCallback)(()=>{y(),n()},[y,n]),x=(0,j.useCallback)(e=>{e.target===f.current&&b()},[b]),S=(0,j.useCallback)(e=>{e.preventDefault(),b()},[b]),C=(0,j.useCallback)(()=>{s(null),v()},[v]),ee=(0,j.useCallback)(e=>{e.key===`Enter`&&(e.ctrlKey||e.metaKey)&&(e.preventDefault(),g&&!_&&C())},[g,_,C]),te=(0,j.useCallback)(()=>{h&&a(hr(i))},[h,i]),w=(0,j.useCallback)(async e=>{l(null),s(null);let t=Po(e);if(t){l(t);return}try{let t=await No(e);if(!kr(t).isJSON){l(`"${e.name}" contains invalid JSON.`);return}a(hr(t)),p.current?.focus()}catch(e){l(e.message)}},[]),ne=(0,j.useCallback)(e=>{e.preventDefault(),e.stopPropagation(),u||d(!0)},[u]),T=(0,j.useCallback)(e=>{e.preventDefault(),e.stopPropagation(),(e.currentTarget===e.target||!e.currentTarget.contains(e.relatedTarget))&&d(!1)},[]),re=(0,j.useCallback)(e=>{e.preventDefault(),e.stopPropagation(),d(!1);let t=e.dataTransfer.files[0];t&&w(t)},[w]),ie=(0,j.useCallback)(e=>{let t=e.target.files?.[0];t&&(w(t),e.target.value=``)},[w]),E=!h&&i.trim()!==``;return(0,M.jsx)(`dialog`,{ref:f,className:V.dialog,"aria-modal":`true`,"aria-labelledby":`mock-upload-modal-title`,onClick:x,onCancel:S,children:(0,M.jsxs)(`div`,{className:V.modalInner,onClick:e=>e.stopPropagation(),children:[(0,M.jsxs)(`div`,{className:V.modalHeader,children:[(0,M.jsxs)(`div`,{className:V.modalTitleGroup,children:[(0,M.jsx)(`span`,{id:`mock-upload-modal-title`,className:V.modalTitle,children:`⬆️ Upload Mock Data`}),(0,M.jsx)(`span`,{className:V.modalPath,children:e})]}),(0,M.jsx)(`button`,{className:V.closeButton,onClick:b,"aria-label":`Close upload modal`,title:`Close`,disabled:_,children:`✕`})]}),(0,M.jsxs)(`div`,{className:V.modalBody,children:[(0,M.jsxs)(`div`,{className:`${V.dropZone} ${u?V.dropZoneActive:``}`,onDragOver:ne,onDragLeave:T,onDrop:re,"aria-label":`Drop a JSON file here`,children:[(0,M.jsx)(`span`,{className:V.dropZoneIcon,children:`📂`}),(0,M.jsxs)(`span`,{className:V.dropZoneText,children:[`Drop a `,(0,M.jsx)(`code`,{children:`.json`}),` file here`]}),(0,M.jsx)(`span`,{className:V.dropZoneOr,children:`— or —`}),(0,M.jsx)(`input`,{ref:m,type:`file`,accept:`.json,application/json`,className:V.fileInputHidden,"aria-hidden":`true`,tabIndex:-1,onChange:ie}),(0,M.jsx)(`button`,{type:`button`,className:V.browseButton,onClick:()=>m.current?.click(),disabled:_,"aria-label":`Browse for a JSON file`,children:`Browse file…`})]}),c&&(0,M.jsxs)(`span`,{className:V.fileError,role:`alert`,children:[`⚠️ `,c]}),(0,M.jsx)(`label`,{htmlFor:`mock-upload-textarea`,className:V.textareaLabel,children:`JSON Payload`}),(0,M.jsx)(`textarea`,{id:`mock-upload-textarea`,ref:p,className:V.textarea,value:i,onChange:e=>{a(e.target.value),l(null)},onKeyDown:ee,placeholder:`Paste JSON here, or drop / browse a .json file above`,rows:10,spellCheck:!1,"aria-describedby":E?`mock-upload-validation`:void 0}),E&&(0,M.jsx)(`span`,{id:`mock-upload-validation`,className:V.validationError,role:`status`,children:`⚠️ Invalid JSON — check syntax`}),(0,M.jsx)(`span`,{className:V.keyboardHint,children:Mo?`⌘+Enter to upload`:`Ctrl+Enter to upload`}),o&&(0,M.jsxs)(`div`,{className:V.errorBanner,role:`alert`,children:[`❌ Upload failed: `,o]})]}),(0,M.jsxs)(`div`,{className:V.modalFooter,children:[(0,M.jsx)(`button`,{className:V.formatButton,onClick:te,disabled:!h||_,title:`Format JSON`,"aria-label":`Format JSON`,children:`Format`}),(0,M.jsxs)(`div`,{className:V.footerActions,children:[(0,M.jsx)(`button`,{className:V.cancelButton,onClick:b,disabled:_,children:`Cancel`}),(0,M.jsx)(`button`,{className:V.uploadButton,onClick:C,disabled:!g||_,"aria-busy":_,children:_?(0,M.jsxs)(M.Fragment,{children:[(0,M.jsx)(`span`,{className:V.spinner,"aria-hidden":`true`}),` Uploading…`]}):`Upload ▶`})]})]})]})})}var Io=/^[A-Za-z0-9_-]+$/,Lo=/^[A-Za-z0-9_-]+(?:\[(?:0|[1-9]\d*)\])*$/,Ro=new Set([`input`,`output`,`model`,`response`,`result`,`parameter`,`none`,`next`,`api`,`error`]);function zo(e,t){return`properties.${e}.${t}`}function Bo(e){return e.split(`.`).every(e=>Lo.test(e))}function Vo(e,t){return t===`edit`?Bo(e):Io.test(e)}function Ho(e,t={}){let n={},r=t.mode??`create`,i=e.alias.trim(),a=t.originalAlias?.trim()??``,o=e.nodeType.trim();r===`edit`?a?Io.test(a)||(n.alias=`Use only letters, numbers, underscore, and hyphen.`):n.alias=`Original alias is required.`:i?Io.test(i)?Ro.has(i.toLowerCase())?n.alias=`"${i}" is reserved.`:t.graphData?.nodes.some(e=>e.alias.toLowerCase()===i.toLowerCase())&&(n.alias=`Node "${i}" already exists in the current graph.`):n.alias=`Use only letters, numbers, underscore, and hyphen.`:n.alias=`Alias is required.`,o&&!Io.test(o)&&(n.nodeType=`Use only letters, numbers, underscore, and hyphen.`);for(let t of e.properties){let e=t.key.trim(),i=t.value.trim();!e&&!i||(!e&&i?n[zo(t.id,`key`)]=`Property key is required when value is present.`:Vo(e,r)||(n[zo(t.id,`key`)]=r===`edit`?`Use a property name or dot/bracket path, for example mapping[0] or config.value.`:`Use only letters, numbers, underscore, and hyphen.`),r===`create`&&(i.includes(`\r`)||i.includes(`
`))?n[zo(t.id,`value`)]=`Property value must be a single line.`:i.includes(`'''`)&&(n[zo(t.id,`value`)]=`Property value cannot contain '''.`))}return{valid:Object.keys(n).length===0,errors:n}}function Uo(e,t={}){let n={},r=e.trim();return r?Io.test(r)?t.graphData&&!t.graphData.nodes.some(e=>e.alias.toLowerCase()===r.toLowerCase())&&(n.alias=`Node "${r}" is no longer available in the current graph.`):n.alias=`Use only letters, numbers, underscore, and hyphen.`:n.alias=`Alias is required.`,{valid:Object.keys(n).length===0,errors:n}}function Wo(e){return e.length<=63488?{valid:!0,errors:{}}:{valid:!1,errors:{command:`The node command is too large. Shorten property values before submitting.`}}}var Go=0;function Ko(e=``,t=``){return Go+=1,{id:`property-row-${Go}`,key:e,value:t}}function qo(e){return{alias:e===`empty-graph`?`root`:``,nodeType:e===`empty-graph`?`Root`:``,properties:[Ko()],source:e}}var Jo=`This node contains data that cannot be safely represented in the edit form. Use the console edit command for this node.`;function Yo(e){return typeof e==`object`&&!!e&&!Array.isArray(e)}function Xo(e){return e===null?`null`:String(e)}function Zo(e,t,n){if(!Bo(e))return!1;if(Array.isArray(t))return t.length!==0&&t.every((t,r)=>Zo(`${e}[${r}]`,t,n));if(Yo(t)){let r=Object.entries(t);return r.length!==0&&r.every(([t,r])=>Zo(`${e}.${t}`,r,n))}let r=Xo(t);return r.includes(`'''`)?!1:(n.push(Ko(e,r)),!0)}function Qo(e){if(!Io.test(e.alias)||e.types.length>1)return{valid:!1,formState:null,message:Jo};let t=Object.entries(e.properties),n=[];for(let[e,r]of t)if(!Zo(e,r,n))return{valid:!1,formState:null,message:Jo};return{valid:!0,formState:{alias:e.alias,nodeType:e.types[0]??``,properties:n.length>0?n:[Ko()],source:`edit-node`},message:null}}var $o=e=>(0,M.jsxs)(`svg`,{xmlns:`http://www.w3.org/2000/svg`,viewBox:`0 0 16 16`,fill:`none`,width:16,height:16,stroke:`currentColor`,strokeWidth:1.8,strokeLinecap:`round`,strokeLinejoin:`round`,...e,children:[(0,M.jsx)(`line`,{x1:4.75,y1:4.75,x2:11.25,y2:11.25}),(0,M.jsx)(`line`,{x1:11.25,y1:4.75,x2:4.75,y2:11.25})]}),H={overlay:`_overlay_37wtf_1`,panel:`_panel_37wtf_21`,form:`_form_37wtf_34`,header:`_header_37wtf_41`,title:`_title_37wtf_50`,iconButton:`_iconButton_37wtf_57`,removeButton:`_removeButton_37wtf_58`,buttonIcon:`_buttonIcon_37wtf_93`,body:`_body_37wtf_151`,field:`_field_37wtf_161`,propertyField:`_propertyField_37wtf_162`,label:`_label_37wtf_169`,input:`_input_37wtf_176`,textarea:`_textarea_37wtf_189`,properties:`_properties_37wtf_213`,propertiesHeader:`_propertiesHeader_37wtf_229`,sectionTitle:`_sectionTitle_37wtf_236`,propertyRows:`_propertyRows_37wtf_242`,propertyActions:`_propertyActions_37wtf_248`,propertyRow:`_propertyRow_37wtf_242`,message:`_message_37wtf_266`,warningMessage:`_warningMessage_37wtf_267`,errorMessage:`_errorMessage_37wtf_268`,errorText:`_errorText_37wtf_293`,footer:`_footer_37wtf_298`,primaryButton:`_primaryButton_37wtf_308`,secondaryButton:`_secondaryButton_37wtf_309`,addPropertyButton:`_addPropertyButton_37wtf_341`},es=2,ts=8,ns=42;function rs(e){let t=e.split(`
`).reduce((e,t)=>e+Math.max(1,Math.ceil(t.length/ns)),0);return Math.min(Math.max(t,es),ts)}function is({open:e,mode:t,aliasReadOnly:n,formState:r,phase:i,lockReason:a,serverMessage:o,validationErrors:s,onFormStateChange:c,onSubmit:l,onClose:u}){let d=(0,j.useRef)(null),f=(0,j.useRef)(null),p=(0,j.useRef)(new Map),m=(0,j.useRef)(null),h=t===`edit`,g=i===`sending`,_=a===`disconnected`,v=g||_,y=h?`Edit Node`:`Create Node`,b=h?`Close edit node dialog`:`Close create node dialog`,x=h?`Save Changes`:`Create Node`,S=h?`Saving...`:`Creating...`,C=h?`Connection disconnected. Refresh the page and edit the node again after the app reconnects.`:`Connection disconnected. Refresh the page and create the node again after the app reconnects.`;(0,j.useEffect)(()=>{if(!e)return;h?f.current?.focus():d.current?.focus();let t=e=>{e.key===`Escape`&&(e.preventDefault(),g||u())};return document.addEventListener(`keydown`,t),()=>{document.removeEventListener(`keydown`,t)}},[h,u,e,g]),(0,j.useEffect)(()=>{let e=m.current;if(!e)return;let t=p.current.get(e);t&&(t.focus(),m.current=null)},[r.properties]);let ee=(0,j.useCallback)(e=>{e.preventDefault(),e.stopPropagation()},[]),te=(0,j.useCallback)(e=>{e.preventDefault(),e.stopPropagation(),g||u()},[u,g]),w=(0,j.useCallback)(e=>{e.stopPropagation()},[]),ne=(0,j.useCallback)(e=>{e.preventDefault(),!v&&l()},[v,l]),T=(0,j.useCallback)(e=>{c({...r,...e})},[r,c]),re=(0,j.useCallback)((e,t)=>{c({...r,properties:r.properties.map(n=>n.id===e?{...n,...t}:n)})},[r,c]),ie=(0,j.useCallback)(()=>{let e=Ko();m.current=e.id,c({...r,properties:[...r.properties,e]})},[r,c]),E=(0,j.useCallback)(e=>{let t=r.properties.filter(t=>t.id!==e);c({...r,properties:t.length>0?t:[Ko()]})},[r,c]);return e?(0,M.jsx)(`div`,{className:H.overlay,onPointerDown:ee,onClick:te,children:(0,M.jsxs)(`div`,{className:H.panel,role:`dialog`,"aria-modal":`true`,"aria-labelledby":`node-dialog-title`,onPointerDown:w,onClick:e=>e.stopPropagation(),children:[(0,M.jsxs)(`header`,{className:H.header,children:[(0,M.jsx)(`div`,{children:(0,M.jsx)(`h2`,{id:`node-dialog-title`,className:H.title,children:y})}),(0,M.jsx)(`button`,{type:`button`,className:H.iconButton,"aria-label":b,onClick:u,disabled:g,children:(0,M.jsx)($o,{className:H.buttonIcon,"aria-hidden":`true`,focusable:`false`})})]}),(0,M.jsxs)(`form`,{className:H.form,onSubmit:ne,children:[(0,M.jsxs)(`div`,{className:H.body,children:[o&&!_&&(0,M.jsx)(`div`,{className:H.message,role:`status`,children:o}),s.command&&(0,M.jsx)(`div`,{className:H.errorMessage,role:`alert`,children:s.command}),_&&(0,M.jsx)(`div`,{className:H.warningMessage,role:`status`,children:o??C}),(0,M.jsxs)(`label`,{className:H.field,children:[(0,M.jsx)(`span`,{className:H.label,children:`Alias`}),(0,M.jsx)(`input`,{ref:d,className:H.input,value:r.alias,disabled:v,readOnly:n,"aria-invalid":!!s.alias,"aria-describedby":s.alias?`node-alias-error`:void 0,onChange:e=>T({alias:e.target.value})}),s.alias&&(0,M.jsx)(`span`,{id:`node-alias-error`,className:H.errorText,children:s.alias})]}),(0,M.jsxs)(`label`,{className:H.field,children:[(0,M.jsx)(`span`,{className:H.label,children:`Node Type`}),(0,M.jsx)(`input`,{ref:f,className:H.input,value:r.nodeType,disabled:v,"aria-invalid":!!s.nodeType,"aria-describedby":s.nodeType?`node-type-error`:void 0,onChange:e=>T({nodeType:e.target.value})}),s.nodeType&&(0,M.jsx)(`span`,{id:`node-type-error`,className:H.errorText,children:s.nodeType})]}),(0,M.jsxs)(`section`,{className:H.properties,"aria-labelledby":`node-properties-title`,children:[(0,M.jsx)(`div`,{className:H.propertiesHeader,children:(0,M.jsx)(`h3`,{id:`node-properties-title`,className:H.sectionTitle,children:`Properties`})}),(0,M.jsx)(`div`,{className:H.propertyRows,children:r.properties.map(e=>{let t=s[zo(e.id,`key`)],n=s[zo(e.id,`value`)],r=rs(e.value);return(0,M.jsxs)(`div`,{className:H.propertyRow,children:[(0,M.jsxs)(`label`,{className:H.propertyField,children:[(0,M.jsx)(`span`,{className:H.label,children:`Key`}),(0,M.jsx)(`input`,{ref:t=>{t?p.current.set(e.id,t):p.current.delete(e.id)},className:H.input,value:e.key,disabled:v,"aria-invalid":!!t,onChange:t=>re(e.id,{key:t.target.value})}),t&&(0,M.jsx)(`span`,{className:H.errorText,children:t})]}),(0,M.jsxs)(`label`,{className:H.propertyField,children:[(0,M.jsx)(`span`,{className:H.label,children:`Value`}),h?(0,M.jsx)(`textarea`,{className:`${H.input} ${H.textarea}`,value:e.value,disabled:v,rows:r,"aria-invalid":!!n,onChange:t=>re(e.id,{value:t.target.value})}):(0,M.jsx)(`input`,{className:H.input,value:e.value,disabled:v,"aria-invalid":!!n,onChange:t=>re(e.id,{value:t.target.value})}),n&&(0,M.jsx)(`span`,{className:H.errorText,children:n})]}),(0,M.jsx)(`button`,{type:`button`,className:H.removeButton,"aria-label":`Remove property`,disabled:v,onClick:()=>E(e.id),children:(0,M.jsx)($o,{className:H.buttonIcon,"aria-hidden":`true`,focusable:`false`})})]},e.id)})}),(0,M.jsx)(`div`,{className:H.propertyActions,children:(0,M.jsxs)(`button`,{type:`button`,className:`${H.secondaryButton} ${H.addPropertyButton}`,disabled:v,onClick:ie,children:[(0,M.jsx)(`span`,{"aria-hidden":`true`,children:`+`}),(0,M.jsx)(`span`,{children:`Add Property`})]})})]})]}),(0,M.jsxs)(`footer`,{className:H.footer,children:[(0,M.jsx)(`button`,{type:`button`,className:H.secondaryButton,onClick:u,disabled:g,children:`Cancel`}),(0,M.jsx)(`button`,{type:`submit`,className:H.primaryButton,disabled:v,children:g?S:x})]})]})]})}):null}function as({state:e,validationErrors:t,onFormStateChange:n,onSubmit:r,onClose:i}){if(e.status===`closed`)return null;let a=e.phase===`sending`?`sending`:e.connectionLost?`disconnected`:null;return(0,M.jsx)(is,{open:!0,mode:e.action===`edit-node`?`edit`:`create`,aliasReadOnly:e.action===`edit-node`,formState:e.formState,phase:e.phase,lockReason:a,serverMessage:e.serverMessage,validationErrors:t,onFormStateChange:n,onSubmit:r,onClose:i})}function os(e,t=!1){return e.properties.map(e=>({key:e.key.trim(),value:t?e.value.replace(/\r\n/g,`
`).replace(/\r/g,`
`):e.value.trim()})).filter(e=>e.key||e.value.trim())}function ss(e){let t=Wo(e);if(!t.valid)throw Error(t.errors.command)}function cs(e,t,n){if(n.includes(`
`)){e.push(`${t}='''`),e.push(n),e.push(`'''`);return}e.push(`${t}=${n}`)}function ls(e){let t=Ho(e);if(!t.valid)throw Error(Object.values(t.errors)[0]??`Invalid node form state.`);let n=e.alias.trim(),r=e.nodeType.trim(),i=os(e),a=[`create node ${n}`];if(r&&a.push(`with type ${r}`),i.length>0){a.push(`with properties`);for(let e of i)cs(a,e.key,e.value)}let o=a.join(`
`);return ss(o),o}function us(e,t){let n=t.trim(),r=Ho(e,{mode:`edit`,originalAlias:n});if(!r.valid)throw Error(Object.values(r.errors)[0]??`Invalid node form state.`);let i=e.nodeType.trim(),a=os(e,!0),o=[`update node ${n}`];if(i&&o.push(`with type ${i}`),a.length>0){o.push(`with properties`);for(let e of a)cs(o,e.key,e.value)}let s=o.join(`
`);return ss(s),s}function ds(e,t={}){let n=e.trim(),r=Uo(n,t);if(!r.valid)throw Error(Object.values(r.errors)[0]??`Invalid node alias.`);let i=`delete node ${n}`;return ss(i),i}var fs=1e4,ps=`A node action is already pending. Wait for it to finish before starting another.`,ms=`Could not send the create-node command because the WebSocket is not open. The form values remain in this dialog.`,hs=`Could not send the edit-node command because the WebSocket is not open. Your changes remain in this dialog.`,gs=`Could not send the delete-node command because the WebSocket is not open.`,_s=`Could not send delete-node commands because the WebSocket is not open.`,vs=`No selected nodes are available to delete.`,ys=`Select 100 or fewer nodes to delete at once.`,bs=`Some delete-node commands were sent, but not all backend results were observed yet. Refresh the graph before trying again.`,xs=`This node is no longer available in the current graph.`,Ss=`Connection disconnected. Refresh the page and create the node again after the app reconnects.`,Cs=`Connection disconnected. Refresh the page and edit the node again after the app reconnects.`,ws=`Connection disconnected while the node action was pending. The outcome is unknown. Refresh the page and check the graph before trying again.`,Ts={status:`closed`,pendingSubmit:null,serverMessage:null};function Es(e){return e.pendingSubmit}function Ds(e){return e.action===`delete-nodes`}function Os(e){return e===`edit-node`?hs:e===`delete-node`?gs:ms}function ks(e){return Ds(e)?bs:`The ${e.action} command was sent, but no backend result was observed yet. The outcome is unknown.`}function As(e){return e===`edit-node`?Cs:Ss}function js(e,t){return e?.trim().toLowerCase()===t.trim().toLowerCase()}function Ms(e,t){return e?.nodes.find(e=>e.alias.toLowerCase()===t.toLowerCase())??null}function Ns(e,t){return e.status===`error`?!0:js(e.alias,t.alias)?e.action===null||e.action===t.action:!1}function Ps({bus:e,connected:t,graphData:n,executor:r,timeoutMs:i=fs,onAccepted:a,onUserMessage:o}){let[s,c]=(0,j.useState)(Ts),[l,u]=(0,j.useState)({}),d=(0,j.useRef)(s),f=(0,j.useRef)(null),p=(0,j.useRef)(t),m=(0,j.useRef)(n),h=(0,j.useRef)(a),g=(0,j.useRef)(o);(0,j.useEffect)(()=>{d.current=s},[s]),(0,j.useEffect)(()=>{m.current=n},[n]),(0,j.useEffect)(()=>{h.current=a},[a]),(0,j.useEffect)(()=>{g.current=o},[o]);let _=(0,j.useCallback)((e,t=`error`)=>{g.current?.(e,t)},[]),v=(0,j.useCallback)(e=>{d.current=e,c(e)},[]),y=(0,j.useCallback)(()=>{f.current!==null&&(clearTimeout(f.current),f.current=null)},[]),b=(0,j.useCallback)(()=>{y(),f.current=setTimeout(()=>{let e=d.current,t=Es(e);t&&(Ds(t)?(v(Ts),_(ks(t),`error`)):e.status===`open`?v({...e,phase:`editing`,pendingSubmit:null,serverMessage:ks(t)}):(v(Ts),_(ks(t),`error`)),f.current=null)},i)},[y,_,v,i]),x=(0,j.useCallback)(e=>{if(!t)return;if(Es(d.current)){_(ps,`error`);return}let n=qo(e);u({}),v({status:`open`,action:`create-node`,phase:`editing`,formState:n,originalAlias:null,pendingSubmit:null,serverMessage:null,connectionLost:!1})},[t,_,v]),S=(0,j.useCallback)(e=>{if(!t){_(Cs,`error`);return}if(Es(d.current)){_(ps,`error`);return}let n=Ms(m.current,e.alias);if(!n){_(xs,`error`);return}let r=Qo(n);if(!r.valid||!r.formState){_(r.message??`This node cannot be edited in the UI.`,`error`);return}u({}),v({status:`open`,action:`edit-node`,phase:`editing`,formState:r.formState,originalAlias:n.alias,pendingSubmit:null,serverMessage:null,connectionLost:!1})},[t,_,v]),C=(0,j.useCallback)(e=>{if(!t){_(gs,`error`);return}if(Es(d.current)){_(ps,`error`);return}let n=Uo(e.alias,{graphData:m.current});if(!n.valid){_(Object.values(n.errors)[0]??`Invalid node alias.`,`error`);return}let i;try{i=ds(e.alias,{graphData:m.current})}catch(e){_(e instanceof Error?e.message:String(e),`error`);return}if(!r.execute(i)){_(gs,`error`);return}let a={action:`delete-node`,alias:e.alias.trim(),command:i,sentAt:new Date().toISOString()};u({}),v({status:`closed`,pendingSubmit:a,serverMessage:null}),b()},[t,r,_,v,b]),ee=(0,j.useCallback)(e=>{if(!t){_(_s,`error`);return}if(Es(d.current)){_(ps,`error`);return}if(e.length===0){_(vs,`info`);return}if(e.length>100){_(ys,`error`);return}let n=new Set,i=e.filter(e=>{let t=e.alias.trim().toLowerCase();return n.has(t)?!1:(n.add(t),!0)}),a=[],o=[];for(let e of i){let t=Uo(e.alias,{graphData:m.current});if(!t.valid){_(Object.values(t.errors)[0]??vs,`error`);return}try{o.push(e.alias.trim()),a.push(ds(e.alias,{graphData:m.current}))}catch(e){_(e instanceof Error?e.message:String(e),`error`);return}}for(let[e,t]of a.entries())if(!r.execute(t)){_(e===0?_s:ws,`error`);return}let s={action:`delete-nodes`,aliases:o,commands:a,sentAt:new Date().toISOString(),results:{}};u({}),v({status:`closed`,pendingSubmit:s,serverMessage:null}),_(`${o.length} delete-node commands sent. Waiting for backend response.`,`info`),b()},[t,r,_,v,b]),te=(0,j.useCallback)(e=>{let t=d.current;t.status===`open`&&(t.phase===`sending`||t.connectionLost||(u({}),v({...t,formState:e,pendingSubmit:null,serverMessage:null,connectionLost:!1})))},[v]),w=(0,j.useCallback)(()=>{let e=d.current;if(e.status!==`open`||e.phase===`sending`||e.connectionLost)return;let n=e.action;if(!t){v({...e,serverMessage:Os(n)});return}let i=Ho(e.formState,n===`edit-node`?{mode:`edit`,originalAlias:e.originalAlias}:{graphData:m.current});if(!i.valid){u(i.errors);return}let a,o;try{n===`edit-node`?(o=e.originalAlias?.trim()??``,a=us(e.formState,o)):(o=e.formState.alias.trim(),a=ls(e.formState))}catch(e){u({command:e instanceof Error?e.message:String(e)});return}if(!r.execute(a)){v({...e,phase:`editing`,pendingSubmit:null,serverMessage:Os(n)});return}let s={action:n,alias:o,command:a,sentAt:new Date().toISOString()};u({}),v({...e,phase:`sending`,pendingSubmit:s,serverMessage:null,connectionLost:!1}),b()},[t,r,v,b]),ne=(0,j.useCallback)(()=>{let e=d.current;e.status===`open`&&e.phase!==`sending`&&(y(),u({}),v(Ts))},[y,v]);return(0,j.useEffect)(()=>e.on(`minigraph.nodeAction.textResult`,e=>{let t=d.current,n=Es(t);if(n){if(Ds(n)){let r=ji(n,e);if(!r)return;if(e.status===`accepted`&&h.current?.({status:e.status,action:e.action,alias:e.alias,message:e.message}),!Mi(r)){v({...t,pendingSubmit:r});return}y(),v(Ts);let i=Ni(r);_(i.message,i.type);return}if(Ns(e,n)){if(y(),e.status===`accepted`){u({}),v(Ts),h.current?.({status:e.status,action:e.action,alias:e.alias,message:e.message});return}t.status===`open`?v({...t,phase:`editing`,pendingSubmit:null,serverMessage:e.status===`error`?`Backend returned an error while this submit was pending: ${e.message}`:e.message}):(v(Ts),_(e.message,`error`))}}}),[e,y,_,v]),(0,j.useEffect)(()=>{if(p.current&&!t){let e=d.current,t=Es(e);if(e.status===`open`){y();let n=t?ws:As(e.action);v({...e,phase:`editing`,pendingSubmit:null,serverMessage:n,connectionLost:!0})}else t&&(y(),v(Ts),_(ws,`error`))}p.current=t},[y,t,_,v]),(0,j.useEffect)(()=>()=>{y()},[y]),{state:s,validationErrors:l,openCreateNode:x,openEditNode:S,deleteNode:C,deleteNodes:ee,updateFormState:te,submit:w,close:ne}}var Fs=/^ws-\d+-\d+$/;function Is(e){return Fs.test(e.trim())}var Ls={sessionId:null,startedSince:null,subscribedTo:null,subscribers:[],loading:!1,pendingCommand:null,error:null,lastInfo:null};function Rs(e){return Array.from(new Set(e)).sort()}function zs({enabled:e,connected:t,bus:n,classificationMap:r,sendRawText:i,addToast:a}){let[o,s]=(0,j.useState)(Ls),c=(0,j.useRef)(new Set),l=(0,j.useRef)(0),u=(0,j.useRef)(i),d=(0,j.useRef)(a);(0,j.useEffect)(()=>{u.current=i},[i]),(0,j.useEffect)(()=>{d.current=a},[a]);let f=(0,j.useCallback)(()=>{if(!e||!t)return!1;s(e=>({...e,loading:!0,pendingCommand:`refresh`,error:null,lastInfo:null}));let n=u.current(`session`);if(!n){let e=`Could not load session details because the WebSocket is not open.`;s(t=>({...t,loading:!1,pendingCommand:null,error:e})),d.current(e,`error`)}return n},[t,e]),p=(0,j.useCallback)(e=>{let t=`${e.kind}:${e.msgId}`;if(!c.current.has(t)){if(c.current.add(t),e.kind===`minigraph.session.started`){s({...Ls,sessionId:e.sessionId});return}if(e.kind===`minigraph.session.status`){s(t=>({...t,sessionId:e.sessionId,startedSince:e.startedSince,subscribedTo:e.subscribedTo,subscribers:Rs(e.subscribers),loading:!1,pendingCommand:null,error:null,lastInfo:null}));return}if(e.kind===`minigraph.session.commandResult`){if(e.status===`accepted`){s(t=>({...t,subscribedTo:e.command===`subscribe`?e.sessionId:e.command===`unsubscribe`?null:t.subscribedTo,pendingCommand:null,error:null,lastInfo:null}));return}s(t=>({...t,pendingCommand:null,error:e.message,lastInfo:null}));return}if(e.kind===`minigraph.session.notification`){e.type===`host-closed`?s(t=>({...t,subscribedTo:t.subscribedTo===e.sessionId?null:t.subscribedTo,subscribers:t.subscribers.filter(t=>t!==e.sessionId),error:null,lastInfo:null})):e.type===`subscriber-joined`?s(t=>({...t,subscribers:Rs([...t.subscribers,e.sessionId]),error:null,lastInfo:null})):s(t=>({...t,subscribers:t.subscribers.filter(t=>t!==e.sessionId),error:null,lastInfo:null}));return}e.kind===`session.reset`&&(s(e=>({...e,startedSince:null,subscribedTo:null,subscribers:[],loading:!1,pendingCommand:null,error:null,lastInfo:null})),f())}},[f]),m=(0,j.useCallback)(()=>{s(e=>({...e,error:null,lastInfo:null}))},[]),h=(0,j.useCallback)(n=>{let r=n.trim();if(!e||!t||o.pendingCommand!==null||o.subscribedTo!==null)return!1;if(!Is(r))return s(e=>({...e,error:`Enter a valid session ID like ws-123456-1.`,lastInfo:null})),!1;s(e=>({...e,pendingCommand:`subscribe`,error:null,lastInfo:null}));let c=i(`session subscribe ${r}`);if(!c){let e=`Could not subscribe because the WebSocket is not open.`;s(t=>({...t,pendingCommand:null,error:e})),a(e,`error`)}return c},[a,t,e,i,o.pendingCommand,o.subscribedTo]),g=(0,j.useCallback)(()=>{if(!e||!t||o.pendingCommand!==null||o.subscribedTo===null)return!1;s(e=>({...e,pendingCommand:`unsubscribe`,error:null,lastInfo:null}));let n=i(`session unsubscribe`);if(!n){let e=`Could not unsubscribe because the WebSocket is not open.`;s(t=>({...t,pendingCommand:null,error:e})),a(e,`error`)}return n},[a,t,e,i,o.pendingCommand,o.subscribedTo]),_=(0,j.useCallback)(()=>{if(!e||!t||o.pendingCommand!==null||o.subscribedTo!==null||o.subscribers.length===0)return!1;s(e=>({...e,pendingCommand:`reset`,error:null,lastInfo:null}));let n=i(`session reset`);if(!n){let e=`Could not reset because the WebSocket is not open.`;s(t=>({...t,pendingCommand:null,error:e})),a(e,`error`)}return n},[a,t,e,i,o.pendingCommand,o.subscribedTo,o.subscribers.length]);(0,j.useEffect)(()=>{e&&t||(c.current.clear(),l.current=0,s(Ls))},[t,e]),(0,j.useEffect)(()=>{if(!e)return;let t=n.on(`minigraph.session.started`,e=>{p(e)}),r=n.on(`minigraph.session.status`,e=>{p(e)}),i=n.on(`minigraph.session.commandResult`,e=>{p(e)}),a=n.on(`minigraph.session.notification`,e=>{p(e)}),o=n.on(`session.reset`,e=>{p(e)});return()=>{t(),r(),i(),a(),o()}},[n,e,p]),(0,j.useEffect)(()=>{!e||!t||f()},[t,e,f]),(0,j.useEffect)(()=>{if(!e||!r)return;let t=l.current;for(let[e,n]of r)if(!(e<=l.current)){for(let e of n)(e.kind===`minigraph.session.started`||e.kind===`minigraph.session.status`||e.kind===`minigraph.session.commandResult`||e.kind===`minigraph.session.notification`||e.kind===`session.reset`)&&p(e);t=Math.max(t,e)}l.current=t,c.current.clear()},[r,e,p]);let v=o.subscribedTo===null,y=o.subscribers.length>0;return(0,j.useMemo)(()=>({state:o,connected:t,isPrimary:v,hasSubscribers:y,canSubscribe:e&&t&&o.pendingCommand===null&&o.subscribedTo===null,canUnsubscribe:e&&t&&o.subscribedTo!==null&&o.pendingCommand===null,canReset:e&&t&&o.pendingCommand===null&&o.subscribedTo===null&&o.subscribers.length>0,subscribeToSession:h,unsubscribe:g,resetSession:_,clearMessage:m}),[m,t,e,y,v,_,o,h,g])}var Bs=(e,t)=>t.some(t=>e instanceof t),Vs,Hs;function Us(){return Vs||=[IDBDatabase,IDBObjectStore,IDBIndex,IDBCursor,IDBTransaction]}function Ws(){return Hs||=[IDBCursor.prototype.advance,IDBCursor.prototype.continue,IDBCursor.prototype.continuePrimaryKey]}var Gs=new WeakMap,Ks=new WeakMap,qs=new WeakMap;function Js(e){let t=new Promise((t,n)=>{let r=()=>{e.removeEventListener(`success`,i),e.removeEventListener(`error`,a)},i=()=>{t(ec(e.result)),r()},a=()=>{n(e.error),r()};e.addEventListener(`success`,i),e.addEventListener(`error`,a)});return qs.set(t,e),t}function Ys(e){if(Gs.has(e))return;let t=new Promise((t,n)=>{let r=()=>{e.removeEventListener(`complete`,i),e.removeEventListener(`error`,a),e.removeEventListener(`abort`,a)},i=()=>{t(),r()},a=()=>{n(e.error||new DOMException(`AbortError`,`AbortError`)),r()};e.addEventListener(`complete`,i),e.addEventListener(`error`,a),e.addEventListener(`abort`,a)});Gs.set(e,t)}var Xs={get(e,t,n){if(e instanceof IDBTransaction){if(t===`done`)return Gs.get(e);if(t===`store`)return n.objectStoreNames[1]?void 0:n.objectStore(n.objectStoreNames[0])}return ec(e[t])},set(e,t,n){return e[t]=n,!0},has(e,t){return e instanceof IDBTransaction&&(t===`done`||t===`store`)||t in e}};function Zs(e){Xs=e(Xs)}function Qs(e){return Ws().includes(e)?function(...t){return e.apply(tc(this),t),ec(this.request)}:function(...t){return ec(e.apply(tc(this),t))}}function $s(e){return typeof e==`function`?Qs(e):(e instanceof IDBTransaction&&Ys(e),Bs(e,Us())?new Proxy(e,Xs):e)}function ec(e){if(e instanceof IDBRequest)return Js(e);if(Ks.has(e))return Ks.get(e);let t=$s(e);return t!==e&&(Ks.set(e,t),qs.set(t,e)),t}var tc=e=>qs.get(e);function nc(e,t,{blocked:n,upgrade:r,blocking:i,terminated:a}={}){let o=indexedDB.open(e,t),s=ec(o);return r&&o.addEventListener(`upgradeneeded`,e=>{r(ec(o.result),e.oldVersion,e.newVersion,ec(o.transaction),e)}),n&&o.addEventListener(`blocked`,e=>n(e.oldVersion,e.newVersion,e)),s.then(e=>{a&&e.addEventListener(`close`,()=>a()),i&&e.addEventListener(`versionchange`,e=>i(e.oldVersion,e.newVersion,e))}).catch(()=>{}),s}function rc(e,{blocked:t}={}){let n=indexedDB.deleteDatabase(e);return t&&n.addEventListener(`blocked`,e=>t(e.oldVersion,e)),ec(n).then(()=>void 0)}var ic=[`get`,`getKey`,`getAll`,`getAllKeys`,`count`],ac=[`put`,`add`,`delete`,`clear`],oc=new Map;function sc(e,t){if(!(e instanceof IDBDatabase&&!(t in e)&&typeof t==`string`))return;if(oc.get(t))return oc.get(t);let n=t.replace(/FromIndex$/,``),r=t!==n,i=ac.includes(n);if(!(n in(r?IDBIndex:IDBObjectStore).prototype)||!(i||ic.includes(n)))return;let a=async function(e,...t){let a=this.transaction(e,i?`readwrite`:`readonly`),o=a.store;return r&&(o=o.index(t.shift())),(await Promise.all([o[n](...t),i&&a.done]))[0]};return oc.set(t,a),a}Zs(e=>({...e,get:(t,n,r)=>sc(t,n)||e.get(t,n,r),has:(t,n)=>!!sc(t,n)||e.has(t,n)}));var cc=[`continue`,`continuePrimaryKey`,`advance`],lc={},uc=new WeakMap,dc=new WeakMap,fc={get(e,t){if(!cc.includes(t))return e[t];let n=lc[t];return n||=lc[t]=function(...e){uc.set(this,dc.get(this)[t](...e))},n}};async function*pc(...e){let t=this;if(t instanceof IDBCursor||(t=await t.openCursor(...e)),!t)return;t=t;let n=new Proxy(t,fc);for(dc.set(n,t),qs.set(n,tc(t));t;)yield n,t=await(uc.get(n)||t.continue()),uc.delete(n)}function mc(e,t){return t===Symbol.asyncIterator&&Bs(e,[IDBIndex,IDBObjectStore,IDBCursor])||t===`iterate`&&Bs(e,[IDBIndex,IDBObjectStore])}Zs(e=>({...e,get(t,n,r){return mc(t,n)?pc:e.get(t,n,r)},has(t,n){return mc(t,n)||e.has(t,n)}}));var hc=`minigraph-clipboard`,gc=1,_c=`items`,vc=null;function yc(){return nc(hc,gc,{upgrade(e){e.objectStoreNames.contains(_c)&&e.deleteObjectStore(_c);let t=e.createObjectStore(_c,{keyPath:`id`});t.createIndex(`by-alias`,`node.alias`,{unique:!0}),t.createIndex(`by-clippedAt`,`clippedAt`)}})}function bc(){return vc||=yc().catch(async e=>(console.warn(`[clipboard/db] openDB failed, deleting and recreating:`,e),vc=null,await rc(hc),yc())),vc}async function xc(){return(await(await bc()).getAllFromIndex(_c,`by-clippedAt`)).reverse()}async function Sc(e){return(await bc()).getFromIndex(_c,`by-alias`,e)}async function Cc(e){await(await bc()).add(_c,e)}async function wc(e,t){let n=(await bc()).transaction(_c,`readwrite`);await n.store.delete(e),await n.store.add(t),await n.done}async function Tc(e){await(await bc()).delete(_c,e)}async function Ec(){await(await bc()).clear(_c)}var Dc=`minigraph-clipboard-sync`;function Oc(){return new BroadcastChannel(Dc)}function kc(e,t){switch(t.type){case`HYDRATE`:return{items:t.items,isLoading:!1};case`ITEM_ADDED`:return{...e,items:[t.item,...e.items]};case`ITEM_REPLACED`:{let n=e.items.filter(e=>e.id!==t.previousId);return{...e,items:[t.item,...n]}}case`ITEM_REMOVED`:return{...e,items:e.items.filter(e=>e.id!==t.id)};case`ITEMS_CLEARED`:return{...e,items:[]};default:return e}}var Ac=(0,j.createContext)(null);function jc({children:e}){let[t,n]=(0,j.useReducer)(kc,{items:[],isLoading:!0}),r=(0,j.useRef)(null);(0,j.useEffect)(()=>{xc().then(e=>n({type:`HYDRATE`,items:e}))},[]),(0,j.useEffect)(()=>{let e;try{e=Oc()}catch{return}return r.current=e,e.onmessage=e=>{let t=e.data;switch(t.type){case`item-added`:n({type:`ITEM_ADDED`,item:t.item});break;case`item-replaced`:n({type:`ITEM_REPLACED`,item:t.item,previousId:t.previousId});break;case`item-removed`:n({type:`ITEM_REMOVED`,id:t.id});break;case`items-cleared`:n({type:`ITEMS_CLEARED`});break}},()=>{e.close(),r.current=null}},[]);let i=(0,j.useCallback)(e=>{r.current?.postMessage(e)},[]),a=(0,j.useCallback)(async(e,t,r)=>{try{let a={id:crypto.randomUUID(),clippedAt:new Date().toISOString(),sourceWsPath:r.sourceWsPath,sourceLabel:r.sourceLabel,node:e,connections:t},o=await Sc(e.alias);if(o)return{status:`duplicate`,existingItem:o,pendingItem:a};try{await Cc(a)}catch(t){if(t instanceof DOMException&&t.name===`ConstraintError`){let t=await Sc(e.alias);if(t)return{status:`duplicate`,existingItem:t,pendingItem:a}}throw t}return n({type:`ITEM_ADDED`,item:a}),i({type:`item-added`,item:a}),{status:`added`}}catch(e){return{status:`error`,message:e instanceof Error?e.message:String(e)}}},[i]),o=(0,j.useCallback)(async(e,t)=>{await wc(t,e),n({type:`ITEM_REPLACED`,item:e,previousId:t}),i({type:`item-replaced`,item:e,previousId:t})},[i]),s=(0,j.useCallback)(async e=>{await Tc(e),n({type:`ITEM_REMOVED`,id:e}),i({type:`item-removed`,id:e})},[i]),c=(0,j.useCallback)(async()=>{await Ec(),n({type:`ITEMS_CLEARED`}),i({type:`items-cleared`})},[i]);return(0,M.jsx)(Ac.Provider,{value:{items:t.items,isLoading:t.isLoading,clipNode:a,confirmReplace:o,removeItem:s,clearAll:c},children:e})}function Mc(){let e=(0,j.useContext)(Ac);if(!e)throw Error(`useClipboardContext must be used inside <ClipboardProvider>`);return e}var Nc=new Intl.Collator(void 0,{sensitivity:`base`,numeric:!0});function Pc(e){return e.node.types[0]?.trim()||`unknown`}function Fc(e,t){return Nc.compare(e,t)}function Ic(e,t){return e-t}function Lc(e){return e===`recent`||e===`connections`?`descending`:`ascending`}function Rc(e,t){return t===`descending`?-e:e}function U(e,t){let n=t.trim();if(!n)return{missing:!0,value:``};let r=e.node.properties[n];return r==null?{missing:!0,value:``}:typeof r==`string`?{missing:!1,value:r}:typeof r==`number`||typeof r==`boolean`?{missing:!1,value:String(r)}:{missing:!1,value:JSON.stringify(r)}}function zc(e,t,n,r){let i=U(e,n),a=U(t,n);return i.missing&&!a.missing?1:!i.missing&&a.missing?-1:Rc(Fc(i.value,a.value),r)}function Bc(e,t){let n=t.direction??Lc(t.field);return e.map((e,t)=>({item:e,originalIndex:t})).sort((e,r)=>{let i=0;switch(t.field){case`type`:i=Fc(Pc(e.item),Pc(r.item));break;case`alias`:i=Fc(e.item.node.alias,r.item.node.alias);break;case`source`:i=Fc(e.item.sourceLabel,r.item.sourceLabel);break;case`connections`:i=e.item.connections.length-r.item.connections.length;break;case`property`:i=zc(e.item,r.item,t.propertyKey??``,n);break;default:i=Date.parse(e.item.clippedAt)-Date.parse(r.item.clippedAt);break}return t.field!==`property`&&(i=Rc(i,n)),i===0?Ic(e.originalIndex,r.originalIndex):i}).map(({item:e})=>e)}function Vc(e){let t=Date.now()-new Date(e).getTime();if(t<0)return`just now`;let n=Math.floor(t/1e3);if(n<60)return`just now`;let r=Math.floor(n/60);if(r<60)return`${r} min ago`;let i=Math.floor(r/60);if(i<24)return`${i} hour${i>1?`s`:``} ago`;let a=Math.floor(i/24);return a===1?`yesterday`:a<30?`${a} days ago`:new Date(e).toLocaleDateString()}var Hc={item:`_item_1rbm8_1`,previewFrame:`_previewFrame_1rbm8_13`,preview:`_preview_1rbm8_13`,previewShell:`_previewShell_1rbm8_25`,metaBlock:`_metaBlock_1rbm8_29`,timestamp:`_timestamp_1rbm8_35`,removeChrome:`_removeChrome_1rbm8_40`,removeIcon:`_removeIcon_1rbm8_68`};function Uc({item:e,onRemove:t,onOpenMenu:n,onCloseMenu:r}){let{node:i,clippedAt:a,sourceLabel:o}=e;return(0,M.jsxs)(`div`,{className:Hc.item,children:[(0,M.jsxs)(`div`,{className:Hc.previewFrame,children:[(0,M.jsx)(`button`,{type:`button`,className:Hc.removeChrome,draggable:!1,"aria-label":`Remove node ${i.alias} from clipboard`,onClick:n=>{n.stopPropagation(),r(),t(e.id)},children:(0,M.jsx)($o,{className:Hc.removeIcon,"aria-hidden":`true`,focusable:`false`})}),(0,M.jsx)(`div`,{className:Hc.preview,role:`group`,draggable:!0,onDragStart:t=>{r(),Ua(t.dataTransfer,e.id)},onContextMenu:t=>{t.preventDefault(),n(e.id,t.clientX,t.clientY)},onKeyDown:t=>{if(t.key===`ContextMenu`||t.key===`F10`&&t.shiftKey){t.preventDefault();let r=t.currentTarget.getBoundingClientRect();n(e.id,Math.round(r.left+8),Math.round(r.top+8))}},tabIndex:0,"aria-label":`Drag node ${i.alias} into the graph to paste`,children:(0,M.jsx)(`div`,{className:Hc.previewShell,style:na(i.types[0]??`unknown`),children:(0,M.jsx)(oa,{alias:i.alias,nodeType:i.types[0]??`unknown`,properties:i.properties})})})]}),(0,M.jsx)(`div`,{className:Hc.metaBlock,children:(0,M.jsxs)(`div`,{className:Hc.timestamp,children:[`Clipped `,Vc(a),` from `,o]})})]})}var Wc={menu:`_menu_164vh_1`,menuItem:`_menuItem_164vh_12`},Gc=16;function Kc(e,t,n){let r=Gc,i=Math.max(Gc,n-t-Gc);return Math.min(Math.max(e,r),i)}function qc({open:e,x:t,y:n,canPasteToInput:r,onPasteToInput:i,onInspect:a,onClose:o}){let s=(0,j.useRef)(null),c=(0,j.useRef)(null),l=(0,j.useRef)(null),[u,d]=(0,j.useState)({left:t,top:n});return(0,j.useLayoutEffect)(()=>{if(!e||!s.current)return;let r=s.current.getBoundingClientRect();d({left:Kc(t,r.width,window.innerWidth),top:Kc(n,r.height,window.innerHeight)})},[e,t,n]),(0,j.useEffect)(()=>{if(!e)return;r?c.current?.focus():l.current?.focus();let t=e=>{s.current&&!s.current.contains(e.target)&&o()},n=e=>{e.key===`Escape`&&(e.preventDefault(),o())},i=()=>o();return document.addEventListener(`pointerdown`,t),document.addEventListener(`keydown`,n),window.addEventListener(`scroll`,i,!0),window.addEventListener(`resize`,i),()=>{document.removeEventListener(`pointerdown`,t),document.removeEventListener(`keydown`,n),window.removeEventListener(`scroll`,i,!0),window.removeEventListener(`resize`,i)}},[e,r,o]),e?(0,M.jsxs)(`div`,{ref:s,className:Wc.menu,style:{left:u.left,top:u.top},role:`menu`,"aria-label":`Clipboard item actions`,children:[(0,M.jsx)(`button`,{ref:c,role:`menuitem`,type:`button`,className:Wc.menuItem,disabled:!r,onClick:()=>{r&&i()},children:`Paste to Input`}),(0,M.jsx)(`button`,{ref:l,role:`menuitem`,type:`button`,className:Wc.menuItem,onClick:a,children:`Inspect`})]}):null}var W={sidebar:`_sidebar_6257m_2`,header:`_header_6257m_12`,headerTitle:`_headerTitle_6257m_22`,clearBtn:`_clearBtn_6257m_29`,sortBar:`_sortBar_6257m_45`,sortMenuWrapper:`_sortMenuWrapper_6257m_56`,sortMenuButton:`_sortMenuButton_6257m_61`,sortButtonLabel:`_sortButtonLabel_6257m_83`,sortButtonValue:`_sortButtonValue_6257m_88`,sortButtonDirection:`_sortButtonDirection_6257m_96`,sortButtonCaret:`_sortButtonCaret_6257m_102`,sortPopover:`_sortPopover_6257m_107`,sortGroup:`_sortGroup_6257m_121`,propertySortRow:`_propertySortRow_6257m_132`,sortGroupTitle:`_sortGroupTitle_6257m_136`,sortOption:`_sortOption_6257m_146`,propertyLabel:`_propertyLabel_6257m_179`,propertyInput:`_propertyInput_6257m_187`,itemList:`_itemList_6257m_205`,loading:`_loading_6257m_215`,emptyState:`_emptyState_6257m_225`,emptyIcon:`_emptyIcon_6257m_238`,emptyTitle:`_emptyTitle_6257m_243`,emptyHint:`_emptyHint_6257m_247`,inspectPanel:`_inspectPanel_6257m_253`,inspectHeader:`_inspectHeader_6257m_261`,inspectClose:`_inspectClose_6257m_275`,inspectBody:`_inspectBody_6257m_289`,dialog:`_dialog_6257m_295`,dialogTitle:`_dialogTitle_6257m_310`,dialogBody:`_dialogBody_6257m_317`,dialogActions:`_dialogActions_6257m_324`,cancelBtn:`_cancelBtn_6257m_331`,replaceBtn:`_replaceBtn_6257m_345`};function Jc(){return(0,M.jsxs)(`div`,{className:W.emptyState,children:[(0,M.jsx)(`span`,{className:W.emptyIcon,children:`📋`}),(0,M.jsx)(`span`,{className:W.emptyTitle,children:`No items clipped yet.`}),(0,M.jsx)(`span`,{className:W.emptyHint,children:`Right-click a node in the Graph view to get started.`})]})}var Yc=[{value:`recent`,label:`Recent`},{value:`type`,label:`Type`},{value:`alias`,label:`Alias`},{value:`source`,label:`Source`},{value:`connections`,label:`Connections`},{value:`property`,label:`Property`}],Xc=[{value:`ascending`,label:`Ascending`},{value:`descending`,label:`Descending`}],Zc=Yc.reduce((e,t)=>({...e,[t.value]:t.label}),{});function Qc({connected:e,onPasteToInput:t}){let n=(0,j.useId)(),i=(0,j.useId)(),a=(0,j.useRef)(null),s=Mc(),[c,l]=(0,j.useState)(null),[u,d]=(0,j.useState)(null),[f,p]=(0,j.useState)(`recent`),[m,h]=(0,j.useState)(Lc(`recent`)),[g,_]=(0,j.useState)(``),[v,y]=(0,j.useState)(!1),b=(e,t,n)=>{d({itemId:e,x:t,y:n})},x=()=>{d(null)},S=e=>{x(),t(e)},C=e=>{x(),l(t=>t?.id===e.id?null:e)},ee=e=>{x(),l(t=>t?.id===e?null:t),s.removeItem(e)},te=()=>{x(),l(null),s.clearAll()},w=e=>{p(e),h(Lc(e))};(0,j.useEffect)(()=>{let e=new Set(s.items.map(e=>e.id));u&&!e.has(u.itemId)&&d(null),c&&!e.has(c.id)&&l(null)},[s.items,u,c]),(0,j.useEffect)(()=>{if(!v)return;let e=e=>{a.current?.contains(e.target)||y(!1)},t=e=>{e.key===`Escape`&&y(!1)};return document.addEventListener(`pointerdown`,e),document.addEventListener(`keydown`,t),()=>{document.removeEventListener(`pointerdown`,e),document.removeEventListener(`keydown`,t)}},[v]);let ne=(0,j.useMemo)(()=>u?s.items.find(e=>e.id===u.itemId)??null:null,[u,s.items]),T=(0,j.useMemo)(()=>Bc(s.items,{field:f,direction:m,propertyKey:g}),[s.items,m,f,g]);return(0,M.jsxs)(`div`,{className:W.sidebar,children:[(0,M.jsxs)(`div`,{className:W.header,children:[(0,M.jsx)(`span`,{className:W.headerTitle,children:`Workspace`}),s.items.length>0&&(0,M.jsx)(`button`,{className:W.clearBtn,onClick:te,"aria-label":`Clear all workspace items`,children:`Clear`})]}),s.items.length>0&&(0,M.jsx)(`div`,{className:W.sortBar,children:(0,M.jsxs)(`div`,{className:W.sortMenuWrapper,ref:a,children:[(0,M.jsxs)(`button`,{type:`button`,className:W.sortMenuButton,onClick:()=>y(e=>!e),"aria-expanded":v,"aria-controls":n,children:[(0,M.jsx)(`span`,{className:W.sortButtonLabel,children:`Sort`}),(0,M.jsx)(`span`,{className:W.sortButtonValue,children:Zc[f]}),(0,M.jsx)(`span`,{className:W.sortButtonDirection,children:m===`ascending`?`Asc`:`Desc`}),(0,M.jsx)(`span`,{className:W.sortButtonCaret,"aria-hidden":`true`,children:`v`})]}),v&&(0,M.jsxs)(`div`,{id:n,className:W.sortPopover,children:[(0,M.jsxs)(`div`,{className:W.sortGroup,role:`group`,"aria-labelledby":`${n}-field-title`,children:[(0,M.jsx)(`div`,{id:`${n}-field-title`,className:W.sortGroupTitle,children:`Sort By`}),Yc.map(e=>(0,M.jsxs)(`label`,{className:W.sortOption,children:[(0,M.jsx)(`input`,{type:`radio`,name:`${n}-field`,value:e.value,checked:f===e.value,onChange:()=>w(e.value)}),(0,M.jsx)(`span`,{children:e.label})]},e.value))]}),f===`property`&&(0,M.jsxs)(`div`,{className:W.propertySortRow,children:[(0,M.jsx)(`label`,{className:W.propertyLabel,htmlFor:i,children:`Property Key`}),(0,M.jsx)(`input`,{id:i,className:W.propertyInput,value:g,onChange:e=>_(e.target.value),placeholder:`skill`,"aria-label":`Property key to sort by`})]}),(0,M.jsxs)(`div`,{className:W.sortGroup,role:`group`,"aria-labelledby":`${n}-direction-title`,children:[(0,M.jsx)(`div`,{id:`${n}-direction-title`,className:W.sortGroupTitle,children:`Sort Direction`}),Xc.map(e=>(0,M.jsxs)(`label`,{className:W.sortOption,children:[(0,M.jsx)(`input`,{type:`radio`,name:`${n}-direction`,value:e.value,checked:m===e.value,onChange:()=>h(e.value)}),(0,M.jsx)(`span`,{children:e.label})]},e.value))]})]})]})}),(0,M.jsx)(`div`,{className:W.itemList,children:s.isLoading?(0,M.jsx)(`div`,{className:W.loading,children:`Loading…`}):s.items.length===0?(0,M.jsx)(Jc,{}):T.map(e=>(0,M.jsx)(Uc,{item:e,onRemove:ee,onOpenMenu:b,onCloseMenu:x},e.id))}),c&&(0,M.jsxs)(`div`,{className:W.inspectPanel,children:[(0,M.jsxs)(`div`,{className:W.inspectHeader,children:[(0,M.jsxs)(`span`,{children:[`Inspect node `,c.node.alias]}),(0,M.jsx)(`button`,{className:W.inspectClose,onClick:()=>l(null),"aria-label":`Close inspect panel`,children:`✕`})]}),(0,M.jsx)(`div`,{className:W.inspectBody,children:(0,M.jsx)(o,{data:{node:c.node,connections:c.connections},style:r})})]}),u&&ne&&(0,M.jsx)(qc,{open:!0,x:u.x,y:u.y,canPasteToInput:e,onPasteToInput:()=>S(ne),onInspect:()=>C(ne),onClose:x})]})}function $c(e){let{wheelTargetRef:t,scrollRef:n,contentWrapperRef:r,currentIndex:i,totalPages:a,onNavigatePrev:o,onNavigateNext:s}=e,c=(0,j.useRef)(0),l=(0,j.useRef)(null),u=(0,j.useRef)(!1),d=(0,j.useRef)(null),f=(0,j.useRef)(o),p=(0,j.useRef)(s),m=(0,j.useRef)(i),h=(0,j.useRef)(a);(0,j.useEffect)(()=>{f.current=o}),(0,j.useEffect)(()=>{p.current=s}),(0,j.useEffect)(()=>{m.current=i}),(0,j.useEffect)(()=>{h.current=a}),(0,j.useEffect)(()=>{d.current!==null&&(clearTimeout(d.current),d.current=null),r.current&&(r.current.style.transition=`none`,r.current.style.transform=`translateY(0)`),c.current=0,l.current=null},[i]),(0,j.useEffect)(()=>{let e=t.current;if(!e)return;function i(){c.current=0,l.current=null,r.current&&(r.current.style.transition=`transform 0.28s cubic-bezier(0.25, 0.46, 0.45, 0.94)`,r.current.style.transform=`translateY(0)`)}function a(e){if(e.deltaY===0)return;let t=n.current;if(!t)return;let a=t.scrollTop<=0,o=t.scrollTop+t.clientHeight>=t.scrollHeight-1,s=e.deltaY<0,g=e.deltaY>0,_=a&&s,v=o&&g;if(!_&&!v){i();return}if(u.current)return;let y=m.current,b=h.current;if(_&&y===0||v&&y===b-1)return;let x=_?`prev`:`next`;if(l.current!==null&&l.current!==x&&i(),l.current=x,c.current+=Math.abs(e.deltaY),r.current){let e=x===`prev`?-1:1,t=c.current*(18/120),n=Math.min(t,18)*e;r.current.style.transition=`none`,r.current.style.transform=`translateY(${n}px)`}if(d.current!==null&&clearTimeout(d.current),d.current=setTimeout(i,180),c.current>=120){d.current!==null&&clearTimeout(d.current);let e=l.current;i(),u.current=!0,e===`prev`?f.current():p.current(),setTimeout(()=>{u.current=!1},650)}}return e.addEventListener(`wheel`,a,{passive:!0}),()=>{d.current!==null&&clearTimeout(d.current),e.removeEventListener(`wheel`,a)}},[])}var G={helpRoot:`_helpRoot_18tja_2`,categoryNav:`_categoryNav_18tja_11`,categoryTabScroller:`_categoryTabScroller_18tja_21`,categoryTab:`_categoryTab_18tja_21`,categoryTabActive:`_categoryTabActive_18tja_71`,maximizeButton:`_maximizeButton_18tja_78`,closeButton:`_closeButton_18tja_100`,helpBody:`_helpBody_18tja_122`,emptyFallback:`_emptyFallback_18tja_130`,helpContent:`_helpContent_18tja_147`,topicLink:`_topicLink_18tja_226`,helpBodyContent:`_helpBodyContent_18tja_271`,chipStrip:`_chipStrip_18tja_276`,chipStripLabel:`_chipStripLabel_18tja_294`,topicChip:`_topicChip_18tja_310`,topicChipActive:`_topicChipActive_18tja_338`};function el(e){return typeof e==`string`?e:typeof e==`number`?String(e):Array.isArray(e)?e.map(el).join(``):j.isValidElement(e)?el(e.props.children):``}function tl(e){if(!e.trim().toLowerCase().startsWith(`help `))return null;let t=e.trim().slice(5).replace(/\s*\(.*\)\s*$/,``).trim().toLowerCase();return t.length>0?t:null}function nl({activeTopic:e,onNavigate:t,onClose:n,onToggleMaximize:r,isMaximized:i}){let a=(0,j.useRef)(null),o=(0,j.useRef)(null),s=(0,j.useRef)(null),c=(0,j.useRef)(null);(0,j.useEffect)(()=>{a.current&&(a.current.scrollTop=0)},[e]),(0,j.useEffect)(()=>{let e=c.current;if(!e)return;let t=e.querySelector(`[aria-current="step"]`);t&&t.scrollIntoView({block:`nearest`,inline:`nearest`,behavior:`smooth`})},[e]);let l=li(e),u=(0,j.useMemo)(()=>ui(l),[l]),d=u.length,f=(0,j.useMemo)(()=>si.find(e=>e.id===l)?.chipStripLabel??null,[l]),p=fi.indexOf(e),m=p<0?0:p,h=fi.length;$c({wheelTargetRef:o,scrollRef:a,contentWrapperRef:s,currentIndex:m,totalPages:h,onNavigatePrev:()=>t(fi[m-1]??``),onNavigateNext:()=>t(fi[m+1]??fi[fi.length-1])});let g=ai(e);return(0,M.jsxs)(`div`,{className:G.helpRoot,role:`region`,"aria-label":`Help browser`,ref:o,children:[(0,M.jsxs)(`nav`,{className:G.categoryNav,"aria-label":`Help categories`,children:[(0,M.jsx)(`div`,{className:G.categoryTabScroller,children:si.map(e=>(0,M.jsx)(`button`,{className:[G.categoryTab,e.id===l?G.categoryTabActive:``].join(` `).trim(),"aria-current":e.id===l?`true`:void 0,onClick:()=>{t(ui(e.id)[0]??``)},children:e.label},e.id))}),r&&(0,M.jsx)(`button`,{className:G.maximizeButton,onClick:r,"aria-label":i?`Restore help panel`:`Maximize help panel`,children:i?`⊞`:`⛶`}),n&&(0,M.jsx)(`button`,{className:G.closeButton,onClick:n,"aria-label":`Close help panel`,children:`×`})]}),d>1&&(0,M.jsxs)(`div`,{className:G.chipStrip,ref:c,children:[f!==null&&(0,M.jsx)(`span`,{className:G.chipStripLabel,children:f}),u.map(n=>{let r=n===e,i=di(n,l);return(0,M.jsx)(`button`,{className:[G.topicChip,r?G.topicChipActive:``].join(` `).trim(),"aria-current":r?`step`:void 0,onClick:()=>t(n),children:i},n)})]}),(0,M.jsx)(`div`,{className:G.helpBody,ref:a,children:(0,M.jsx)(`div`,{className:G.helpBodyContent,ref:s,children:g===null?(0,M.jsxs)(`div`,{className:G.emptyFallback,children:[(0,M.jsxs)(`code`,{children:[`help `,e||``]}),`\xA0 not found in the local bundle.`]}):(0,M.jsx)(`div`,{className:G.helpContent,children:(0,M.jsx)(y,{remarkPlugins:[x],components:e===``?{li:({children:e,...n})=>{let r=tl(el(e).trim());return r!==null&&ai(r)!==null?(0,M.jsx)(`li`,{...n,children:(0,M.jsx)(`button`,{className:G.topicLink,"aria-label":`Open help topic: ${r}`,onClick:()=>t(r),children:e})}):(0,M.jsx)(`li`,{...n,children:e})}}:void 0,children:g})})})})]})}function rl({existingItem:e,pendingItem:t,onReplace:n,onCancel:r}){let i=(0,j.useRef)(null);return(0,j.useEffect)(()=>{let e=i.current;e&&!e.open&&e.showModal()},[]),(0,M.jsxs)(`dialog`,{ref:i,className:W.dialog,onClose:r,"aria-labelledby":`duplicate-dialog-title`,children:[(0,M.jsx)(`h2`,{id:`duplicate-dialog-title`,className:W.dialogTitle,children:`Duplicate Node`}),(0,M.jsxs)(`p`,{className:W.dialogBody,children:[`A clipboard item with alias `,(0,M.jsxs)(`strong`,{children:[`"`,t.node.alias,`"`]}),` already exists (clipped `,Vc(e.clippedAt),`).`]}),(0,M.jsx)(`p`,{className:W.dialogBody,children:`Replace it with the new snapshot?`}),(0,M.jsxs)(`div`,{className:W.dialogActions,children:[(0,M.jsx)(`button`,{className:W.cancelBtn,onClick:r,children:`Cancel`}),(0,M.jsx)(`button`,{className:W.replaceBtn,onClick:n,children:`Replace`})]})]})}function il(e,t){if(!t)return null;let n=e.trim().toLowerCase();if(n!==`help`&&!n.startsWith(`help `))return null;let r=pi(e);return ai(r)===null?null:r}var al=class{constructor(){this.listeners=new Map}on(e,t){let n=e;return this.listeners.has(n)||this.listeners.set(n,new Set),this.listeners.get(n).add(t),()=>{this.listeners.get(n)?.delete(t)}}emit(e){let t=this.listeners.get(e.kind);t&&t.forEach(t=>{try{t(e)}catch(t){console.error(`[ProtocolBus] listener for '${e.kind}' threw:`,t)}})}clear(){this.listeners.clear()}},ol=`(ws-\\d+-\\d+)`,sl=RegExp(`^session ${ol} started(?:\\nCompanion endpoint: (\\/api\\/companion\\/${ol}))?$`,`i`),cl=RegExp(`^Session ${ol} started since (.+)$`),ll=RegExp(`^subscribed to ${ol}$`),ul=/^subscribed by \[(.*)]$/,dl=RegExp(`^Subscribed to ${ol}$`),fl=RegExp(`^Session unsubscribed from ${ol}$`),pl=RegExp(`^Session ${ol} not found$`),ml=RegExp(`^${ol} is not a primary session$`),hl=RegExp(`^You have already subscribed to ${ol}(?:\\nPlease do 'session reset' before subscribing to another session)?$`),gl=RegExp(`^${ol} subscribed to your session$`),_l=RegExp(`^${ol} unsubscribed from your session$`),vl=RegExp(`^Session ${ol} has closed$`);function yl(e){let t=e.trim();return t.length===0||t.startsWith(`> `)?null:t}function bl(e){return e.split(`,`).map(e=>e.trim()).filter(e=>e.length>0&&Is(e))}function xl(e){let t=yl(e);if(!t)return null;let n=t.match(sl);return n?{sessionId:n[1],companionEndpoint:n[2]??null}:null}function Sl(e){let t=yl(e);if(!t)return null;let n=t.split(`
`).map(e=>e.trim()).filter(Boolean),r=n[0];if(!r)return null;let i=r.match(cl);if(!i)return null;let a=null,o=[];for(let e of n.slice(1)){let t=e.match(ll);if(t){a=t[1];continue}let n=e.match(ul);n&&(o=bl(n[1]))}return{sessionId:i[1],startedSince:i[2],subscribedTo:a,subscribers:o}}function Cl(e){let t=yl(e);if(!t)return null;let n=t.match(dl);if(n)return{command:`subscribe`,status:`accepted`,sessionId:n[1],message:t};let r=t.match(fl);if(r)return{command:`unsubscribe`,status:`accepted`,sessionId:r[1],message:t};let i=t.match(pl);if(i)return{command:`subscribe`,status:`rejected`,sessionId:i[1],message:t};let a=t.match(ml);if(a)return{command:`subscribe`,status:`rejected`,sessionId:a[1],message:t};let o=t.match(hl);return o?{command:`subscribe`,status:`rejected`,sessionId:o[1],message:t}:t===`You cannot subscribe to yourself`?{command:`subscribe`,status:`rejected`,sessionId:null,message:t}:t===`Nothing to unsubscribe`?{command:`unsubscribe`,status:`rejected`,sessionId:null,message:t}:t===`Invalid session command`?{command:`unknown`,status:`rejected`,sessionId:null,message:t}:null}function wl(e){let t=yl(e);if(!t)return null;let n=t.match(gl);if(n)return{type:`subscriber-joined`,sessionId:n[1],message:t};let r=t.match(_l);if(r)return{type:`subscriber-left`,sessionId:r[1],message:t};let i=t.match(vl);return i?{type:`host-closed`,sessionId:i[1],message:t}:null}var Tl=new Set([`info`,`error`,`ping`,`welcome`]);function El(e,t){let n=[],r={msgId:e,raw:t},i=!1,a=!1,o=!1,s=!1,c=!1,l=!1,u=kr(t);if(u.isJSON){let e=u.data;if(typeof e.type==`string`){let i=e.type;return n.push({...r,kind:`lifecycle`,type:i,knownType:Tl.has(i),message:typeof e.message==`string`?e.message:t,time:e.time??null}),n.length>0?n:[{...r,kind:`unclassified`}]}return n.push({...r,kind:`json.response`,data:u.data}),n.length>0?n:[{...r,kind:`unclassified`}]}let d=Ir(t);d&&(c=!0,n.push({...r,kind:`payload.large`,apiPath:d.apiPath,byteSize:d.byteSize,filename:d.filename}));let f=Lr(t);f&&(o=!0,n.push({...r,kind:`upload.invitation`,uploadPath:f}));let p=Fr(t);if(p&&(s=!0,n.push({...r,kind:`upload.contentPath`,uploadPath:p})),Pr(t)){a=!0;let e=Nr(t);e&&n.push({...r,kind:`graph.link`,apiPath:e})}if(a){let e=Ar(t);e&&n.push({...r,kind:`graph.exported`,graphName:e.graphName,apiPath:e.apiPath})}let m=qr(t);m&&n.push({...r,kind:`graph.mutation`,mutationType:m});let h=Kr(t);h&&n.push({...r,kind:`minigraph.nodeAction.textResult`,status:h.status,action:h.action,alias:h.alias,message:h.message}),h&&(h.action===`create-node`||h.status===`error`)&&n.push({...r,kind:`minigraph.createNode.textResult`,status:h.status,alias:h.alias,message:h.message}),t===`Session restarted`&&(l=!0,n.push({...r,kind:`session.reset`}));let g=xl(t);g&&(l=!0,n.push({...r,kind:`minigraph.session.started`,sessionId:g.sessionId,companionEndpoint:g.companionEndpoint}));let _=Sl(t);_&&(l=!0,n.push({...r,kind:`minigraph.session.status`,sessionId:_.sessionId,startedSince:_.startedSince,subscribedTo:_.subscribedTo,subscribers:_.subscribers}));let v=Cl(t);v&&(l=!0,n.push({...r,kind:`minigraph.session.commandResult`,command:v.command,status:v.status,sessionId:v.sessionId,message:v.message}));let y=wl(t);y&&(l=!0,n.push({...r,kind:`minigraph.session.notification`,type:y.type,sessionId:y.sessionId,message:y.message})),t.startsWith(`> `)&&(i=!0,n.push({...r,kind:`command.echo`,commandText:t.slice(2)})),Rr(t)&&n.push({...r,kind:`command.helpOrDescribe`,commandText:t.slice(2)});let b=zr(t);b&&n.push({...r,kind:`command.importGraph`,graphName:b});let x=jr(t);return x&&n.push({...r,kind:`graph.export.failed`,reason:x.reason}),!i&&!a&&!o&&!s&&!c&&!l&&Mr(t)&&n.push({...r,kind:`docs.response`,isMarkdown:!0}),n.length===0&&n.push({...r,kind:`unclassified`}),n}function Dl({messages:e,bus:t}){let n=(0,j.useRef)(-1);(0,j.useEffect)(()=>{e.length>0&&(n.current=e[e.length-1].id)},[]);let r=(0,j.useMemo)(()=>{let t=new Map;for(let n of e)t.set(n.id,El(n.id,n.raw));return t},[e]);return(0,j.useEffect)(()=>{if(e.length===0)return;let i=e.filter(e=>e.id>n.current);if(i.length!==0){n.current=e[e.length-1].id;for(let e of i){let n=r.get(e.id);if(n)for(let e of n)t.emit(e)}}},[e,t,r]),{classificationMap:r}}function Ol({config:e}){let{title:t,wsPath:n,storageKeyPayload:r,storageKeyHistory:i,storageKeyTab:a,storageKeySavedGraphs:o,supportsUpload:s,supportsClipboard:c,supportsHelp:l,supportsAuthoring:u,supportsSessionCollaboration:d,tabs:f}=e,p=Tt(),[m,h]=_r(r,``),g=Er(),[_,v]=(0,j.useState)(()=>g.peekPendingPayload(n)),{takePendingPayload:y}=g;(0,j.useEffect)(()=>{let e=y(n);e!==null&&v(e)},[y,n]);let b=_??m,x=(0,j.useCallback)(e=>{v(null),h(e)},[h]),w=(0,j.useMemo)(()=>b?mr(b):{valid:!0,error:null,type:null},[b]),{toasts:ne,addToast:T,removeToast:re}=gr(),ie=(0,j.useRef)(new al).current,E=Xr({wsPath:n,storageKeyHistory:i,payload:b,addToast:T,bus:ie,handleLocalCommand:(0,j.useCallback)(e=>il(e,l===!0)!==null,[l])}),{classificationMap:ae}=Dl({messages:E.messages,bus:ie}),[oe,se]=Ci(n),{graphData:ce,setGraphData:D,rightTab:O,setRightTab:le,isRefreshing:ue}=ei(oe,T,f[0],f,a),{modalUploadPath:de,successfulUploadPaths:fe,handleOpenUploadModal:k,handleCloseUploadModal:A,handleUploadSuccess:pe,handleUploadError:me,resetSuccessfulPaths:he}=_i({bus:ie,addToast:T});ti({bus:ie,pinnedGraphPath:oe,setPinnedGraphPath:se,connected:E.connected,sendRawText:E.sendRawText,addToast:T});let ge=(0,j.useRef)(!1);(0,j.useEffect)(()=>{ge.current&&!E.connected&&(se(null),D(null)),ge.current=E.connected},[E.connected,se,D]);let[_e,ve]=_r(e.storageKeyHelpTopic??`help-topic-fallback`,``),[ye,be]=_r(`help-panel-open`,!1),[xe,Se]=(0,j.useState)(()=>!!l&&!ye),[Ce,we]=(0,j.useState)(!1),Te=(0,j.useRef)(null),Ee=(0,j.useCallback)(()=>{xe&&(we(!0),Te.current=setTimeout(()=>Se(!1),400))},[xe]);(0,j.useEffect)(()=>{if(!xe||Ce)return;let e=setTimeout(Ee,3e3);return()=>clearTimeout(e)},[xe,Ce,Ee]),(0,j.useEffect)(()=>{ye&&xe&&Ee()},[ye,xe,Ee]),(0,j.useEffect)(()=>()=>{Te.current&&clearTimeout(Te.current)},[]),(0,j.useEffect)(()=>{if(!l)return;let e=e=>{e.ctrlKey&&e.key==="`"&&(e.preventDefault(),be(e=>!e))};return window.addEventListener(`keydown`,e),()=>window.removeEventListener(`keydown`,e)},[l,be]),mi({bus:ie,setHelpTopic:ve,onTabSwitch:l?()=>be(!0):()=>{}}),vi({bus:ie,connected:E.connected,appendMessage:E.appendMessage,addToast:T});let De=Mc(),[Oe,ke]=_r(`clipboard-sidebar-open`,!1),[Ae,je]=(0,j.useState)(null),Me=(0,j.useCallback)(e=>{let t=Ei(e,ce);E.setCommand(t.command),T(`${t.verb===`create`?`Create`:`Update`} command for "${e.node.alias}" pasted to input`,`info`)},[ce,E.setCommand,T]),Ne=(0,j.useCallback)(e=>{let t=De.items.find(t=>t.id===e);if(!t){T(`Clipboard item is no longer available. It may have been removed in another tab.`,`error`);return}let n=Ei(t,ce);if(!E.sendRawText(n.command)){T(`Could not send clipboard paste command because the WebSocket is not open.`,`error`);return}T(`Clipboard node "${t.node.alias}" sent as ${n.verb}. Waiting for backend response.`,`info`)},[De.items,ce,E.sendRawText,T]),Pe=(0,j.useCallback)(async(t,r)=>{try{let i=await De.clipNode(t,r,{sourceWsPath:n,sourceLabel:e.label});switch(i.status){case`added`:T(`Node "${t.alias}" clipped to workspace`,`success`);break;case`duplicate`:je({pendingItem:i.pendingItem,existingItem:i.existingItem});break;case`error`:T(`Clip failed: ${i.message}`,`error`);break}}catch(e){T(`Clip failed: ${e instanceof Error?e.message:String(e)}`,`error`)}},[De,n,e.label,T]),Fe=(0,j.useCallback)(async t=>{if(t.length===0){T(`No selected nodes are available to clip.`,`info`);return}if(t.length>100){T(`Select 100 or fewer nodes to clip at once.`,`error`);return}let r={added:0,duplicates:0,failed:0};for(let i of t){if(!i.node?.alias?.trim()){r.failed+=1;continue}try{let t=await De.clipNode(i.node,i.connections,{sourceWsPath:n,sourceLabel:e.label});t.status===`added`&&(r.added+=1),t.status===`duplicate`&&(r.duplicates+=1),t.status===`error`&&(r.failed+=1)}catch{r.failed+=1}}let i=Oi(r);T(i.message,i.type)},[T,De,e.label,n]),Ie=yi(o??``),{defaultName:Le,setLastSavedName:Re,resetName:ze}=bi(o?`${o}-untitled-counter`:`untitled-counter`,ie),Be=(0,j.useMemo)(()=>{let e=ce?.nodes.find(e=>e.types.includes(`Root`)),t=typeof e?.properties?.name==`string`?e.properties.name:void 0;return t?.trim()?t:null},[ce])??Le,Ve=(0,j.useMemo)(()=>ki(E.sendRawText),[E.sendRawText]),He=Ps({bus:ie,connected:E.connected,graphData:ce,executor:Ve,onUserMessage:T}),Ue=zs({enabled:d===!0,bus:ie,classificationMap:ae,connected:E.connected,sendRawText:E.sendRawText,addToast:T}),{handleSaveGraph:We,handleLoadGraph:Ge}=xi({bus:ie,connected:E.connected,sendRawText:E.sendRawText,saveGraph:Ie.saveGraph,setLastSavedName:Re,addToast:T}),Ke=(0,j.useCallback)(e=>{let t=ae.get(e.id)?.find(e=>e.kind===`graph.link`);t&&se(t.apiPath)},[ae]),{handleSendToJsonPath:qe}=hi({ctx:g,navigate:p,addToast:T,wsPath:n}),Je=Zr(`(max-width: 768px)`),{defaultLayout:Ye,onLayoutChanged:Xe}=ee({id:e.path+`-panel-split`,storage:localStorage}),Ze=(0,j.useCallback)(()=>x(hr(b)),[b]),Qe=(0,j.useCallback)(()=>{E.clearMessages(),se(null),D(null),he(),ze()},[E.clearMessages,D,he,ze]);return(0,M.jsxs)(`div`,{className:dr.wrapper,children:[(0,M.jsx)(Fi,{toasts:ne,onRemove:re}),de&&(0,M.jsx)(Fo,{uploadPath:de,onSuccess:pe,onClose:A,onError:me}),u&&(0,M.jsx)(as,{state:He.state,validationErrors:He.validationErrors,onFormStateChange:He.updateFormState,onSubmit:He.submit,onClose:He.close}),(0,M.jsxs)(`header`,{className:dr.header,children:[(0,M.jsx)(`h1`,{className:dr.title,children:t}),(0,M.jsxs)(`div`,{className:dr.headerActions,children:[o&&(0,M.jsx)(Gi,{disabled:!ce,defaultName:Le,onSave:We,nameExists:Ie.hasGraph,connected:E.connected}),o&&Ie.savedGraphs.length>0&&(0,M.jsx)(qi,{savedGraphs:Ie.savedGraphs,onLoad:Ge,onDelete:Ie.deleteGraph,connected:E.connected}),c&&(0,M.jsxs)(`button`,{className:dr.clipboardToggle,onClick:()=>ke(e=>!e),"aria-label":Oe?`Close workspace sidebar`:`Open workspace sidebar`,"aria-pressed":Oe,children:[`Workspace`,De.items.length>0?` (${De.items.length})`:``]}),(0,M.jsx)(Ui,{addToast:T,sessionCollaboration:d?Ue:null}),l&&(0,M.jsxs)(`div`,{className:dr.helpButtonWrapper,children:[(0,M.jsx)(`button`,{className:`${dr.helpToggle}${xe&&!Ce?` ${dr.helpTogglePulsing}`:``}`,onClick:()=>be(e=>!e),"aria-label":ye?`Close help panel`:`Open help panel`,"aria-pressed":ye,children:`?`}),xe&&(0,M.jsxs)(`div`,{className:`${dr.helpHint}${Ce?` ${dr.helpHintFading}`:``}`,onClick:Ee,role:`status`,children:[(0,M.jsx)(`kbd`,{className:dr.helpHintKbd,children:"Ctrl + `"}),` to toggle help`]})]})]})]}),Ae&&(0,M.jsx)(rl,{existingItem:Ae.existingItem,pendingItem:Ae.pendingItem,onReplace:async()=>{try{await De.confirmReplace(Ae.pendingItem,Ae.existingItem.id),je(null),T(`Clipboard item "${Ae.pendingItem.node.alias}" replaced`,`success`)}catch(e){T(`Replace failed: ${e instanceof Error?e.message:String(e)}`,`error`)}},onCancel:()=>{je(null),T(`Clip cancelled`,`info`)}}),(0,M.jsxs)(te,{className:dr.panelGroup,orientation:Je?`vertical`:`horizontal`,defaultLayout:Ye,onLayoutChanged:Xe,children:[(0,M.jsx)(C,{defaultSize:ye||Oe?`50%`:`60%`,minSize:`25%`,children:(0,M.jsx)(Ao,{messages:E.messages,classificationMap:ae,onCopy:E.copyMessages,onClear:Qe,consoleRef:E.consoleRef,command:E.command,onCommandChange:E.setCommand,onCommandKeyDown:E.handleKeyDown,onSend:E.sendCommand,sendDisabled:!E.connected||!E.command.trim(),inputDisabled:!E.connected,commandHistory:E.history,onGraphLinkMessage:Ke,onCopyMessage:()=>T(`Copied to clipboard`,`success`),onSendToJsonPath:qe,onUploadMockData:k,successfulUploadPaths:fe})}),(0,M.jsx)(S,{className:dr.resizeHandle,"aria-label":`Resize panels`}),(0,M.jsx)(C,{defaultSize:ye?`50%`:Oe?`30%`:`40%`,minSize:`20%`,children:(0,M.jsx)(vo,{tabs:f,payload:b,onChange:x,validation:w,onFormat:Ze,onUpload:s?E.uploadPayload:void 0,graphData:ce,graphName:Be,activeTab:O,onTabChange:le,onGraphRenderError:e=>T(e,`error`),onGraphDataCopySuccess:()=>T(`Graph JSON copied to clipboard!`,`success`),onGraphDataCopyError:()=>T(`Copy failed`,`error`),isGraphRefreshing:ue,onClipNode:c?Pe:void 0,onClipNodes:c?Fe:void 0,onClipboardDrop:c?Ne:void 0,isConnected:E.connected,supportsAuthoring:u,onCreateNode:u?He.openCreateNode:void 0,onEditNode:u?He.openEditNode:void 0,onDeleteNode:u?He.deleteNode:void 0,onDeleteNodes:u?He.deleteNodes:void 0,helpPanel:l&&ye?((e,t)=>(0,M.jsx)(nl,{activeTopic:_e,onNavigate:ve,onClose:()=>be(!1),onToggleMaximize:e,isMaximized:t})):void 0})}),c&&Oe&&(0,M.jsxs)(M.Fragment,{children:[(0,M.jsx)(S,{className:dr.resizeHandle,"aria-label":`Resize clipboard`}),(0,M.jsx)(C,{defaultSize:`20%`,minSize:`10%`,maxSize:`40%`,children:(0,M.jsx)(Qc,{connected:E.connected,onPasteToInput:Me})})]})]})]})}function kl(){let e=yr[0].path;return(0,M.jsx)(Tr,{children:(0,M.jsx)(jc,{children:(0,M.jsx)(Wn,{children:(0,M.jsxs)($t,{children:[yr.map(e=>(0,M.jsx)(Zt,{path:e.path,element:(0,M.jsx)(Ol,{config:e},e.path)},e.path)),(0,M.jsx)(Zt,{path:`*`,element:(0,M.jsx)(Xt,{to:e,replace:!0})})]})})})})}(0,ur.createRoot)(document.getElementById(`root`)).render((0,M.jsx)(j.StrictMode,{children:(0,M.jsx)(kl,{})}));
//# sourceMappingURL=index-DmHEvD_v.js.map