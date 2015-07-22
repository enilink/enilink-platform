define([],function(){"use strict";function n(n,t){var e,i,s,o=t.length;return"function"==typeof t[o-1]&&(o-=1,s=t[o]),"object"==typeof t[o-1]&&(o-=1),2==o?(e=t[0],i=t[1]):(e=n.node,i=t[0]),{element:e,type:i,callback:s}}function t(n,t){return n.element==t.element&&n.type==t.type&&(null==t.callback||n.callback==t.callback)}function e(){function e(n){this.component=n,this.attachedTo=[],this.instances={},this.addInstance=function(n){var t=new i(n);return this.instances[n.identity]=t,this.attachedTo.push(n.node),t},this.removeInstance=function(n){delete this.instances[n.identity];var t=this.attachedTo.indexOf(n.node);t>-1&&this.attachedTo.splice(t,1),Object.keys(this.instances).length||s.removeComponentInfo(this)},this.isAttachedTo=function(n){return this.attachedTo.indexOf(n)>-1}}function i(n){this.instance=n,this.events=[],this.addBind=function(n){this.events.push(n),s.events.push(n)},this.removeBind=function(n){for(var e,i=0;e=this.events[i];i++)t(e,n)&&this.events.splice(i,1)}}var s=this;(this.reset=function(){this.components=[],this.allInstances={},this.events=[]}).call(this),this.addInstance=function(n){var t=this.findComponentInfo(n);t||(t=new e(n.constructor),this.components.push(t));var i=t.addInstance(n);return this.allInstances[n.identity]=i,t},this.removeInstance=function(n){var t=(this.findInstanceInfo(n),this.findComponentInfo(n));t&&t.removeInstance(n),delete this.allInstances[n.identity]},this.removeComponentInfo=function(n){var t=this.components.indexOf(n);t>-1&&this.components.splice(t,1)},this.findComponentInfo=function(n){for(var t,e=n.attachTo?n:n.constructor,i=0;t=this.components[i];i++)if(t.component===e)return t;return null},this.findInstanceInfo=function(n){return this.allInstances[n.identity]||null},this.getBoundEventNames=function(n){return this.findInstanceInfo(n).events.map(function(n){return n.type})},this.findInstanceInfoByNode=function(n){var t=[];return Object.keys(this.allInstances).forEach(function(e){var i=this.allInstances[e];i.instance.node===n&&t.push(i)},this),t},this.on=function(t){for(var e,i=s.findInstanceInfo(this),o=arguments.length,a=1,c=new Array(o-1);o>a;a++)c[a-1]=arguments[a];if(i){e=t.apply(null,c),e&&(c[c.length-1]=e);var h=n(this,c);i.addBind(h)}},this.off=function(){var e=n(this,arguments),i=s.findInstanceInfo(this);i&&i.removeBind(e);for(var o,a=0;o=s.events[a];a++)t(o,e)&&s.events.splice(a,1)},s.trigger=function(){},this.teardown=function(){s.removeInstance(this)},this.withRegistration=function(){this.after("initialize",function(){s.addInstance(this)}),this.around("on",s.on),this.after("off",s.off),window.DEBUG&&DEBUG.enabled&&this.after("trigger",s.trigger),this.after("teardown",{obj:s,fnName:"teardown"})}}return new e});
