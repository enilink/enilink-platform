define([],function(){"use strict";function n(e,t,o){o=o||{};var i=o.obj||window,a=o.path||(i==window?"window":""),l=Object.keys(i);l.forEach(function(o){(v[e]||e)(t,i,o)&&console.log([a,".",o].join(""),"->",["(",typeof i[o],")"].join(""),i[o]),"[object Object]"==Object.prototype.toString.call(i[o])&&i[o]!=i&&-1==a.split(".").indexOf(o)&&n(e,t,{obj:i[o],path:[a,o].join(".")})})}function e(e,t,o,i){t&&typeof o!=t?console.error([o,"must be",t].join(" ")):n(e,o,i)}function t(n,t){e("name","string",n,t)}function o(n,t){e("nameContains","string",n,t)}function i(n,t){e("type","function",n,t)}function a(n,t){e("value",null,n,t)}function l(n,t){e("valueCoerced",null,n,t)}function c(e,t){n(e,null,t)}function r(){var n=[].slice.call(arguments);d.eventNames.length||(d.eventNames=y),d.actions=n.length?n:y,f()}function s(){var n=[].slice.call(arguments);d.actions.length||(d.actions=y),d.eventNames=n.length?n:y,f()}function u(){d.actions=[],d.eventNames=[],f()}function g(){d.actions=y,d.eventNames=y,f()}function f(){try{window.localStorage&&(localStorage.setItem("logFilter_eventNames",d.eventNames),localStorage.setItem("logFilter_actions",d.actions))}catch(n){}}function m(){var n,e;try{n=window.localStorage&&localStorage.getItem("logFilter_eventNames"),e=window.localStorage&&localStorage.getItem("logFilter_actions")}catch(t){return}n&&(d.eventNames=n),e&&(d.actions=e),Object.keys(d).forEach(function(n){var e=d[n];"string"==typeof e&&e!==y&&(d[n]=e?e.split(","):[])})}var v={name:function(n,e,t){return n==t},nameContains:function(n,e,t){return t.indexOf(n)>-1},type:function(n,e,t){return e[t]instanceof n},value:function(n,e,t){return e[t]===n},valueCoerced:function(n,e,t){return e[t]==n}},y="all",d={eventNames:[],actions:[]};return{enable:function(n){this.enabled=!!n,n&&window.console&&(console.info("Booting in DEBUG mode"),console.info("You can configure event logging with DEBUG.events.logAll()/logNone()/logByName()/logByAction()")),m(),window.DEBUG=this},find:{byName:t,byNameContains:o,byType:i,byValue:a,byValueCoerced:l,custom:c},events:{logFilter:d,logByAction:r,logByName:s,logAll:g,logNone:u}}});
