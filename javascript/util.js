// Generated by CoffeeScript 1.6.3
(function() {
  var __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  this.add = function(name) {
    return document.body.classList.add(name);
  };

  this.all = function(selector) {
    return document.querySelectorAll(selector);
  };

  this.get = function(id) {
    return document.getElementById(id);
  };

  this.has = function(name) {
    return document.body.classList.contains(name);
  };

  this.listen = function(eventName, handler, capture) {
    return document.addEventListener(eventName, handler, capture || true);
  };

  this.listenOn = function(object, eventName, handler, capture) {
    return object.addEventListener(eventName, handler, capture || true);
  };

  this.log = function(msg) {
    get("log").innerHTML = msg + "<br>" + get("log").innerHTML;
    return console.log(msg);
  };

  this.remove = function(name) {
    return document.body.classList.remove(name);
  };

  this.toggle = function(name) {
    return (has(name) ? remove(name) : add(name));
  };

  this.touchable = function() {
    return (__indexOf.call(document.documentElement, 'ontouchmove') >= 0);
  };

  this.isMobile = function() {
    if (typeof device !== "undefined" && device !== null) {
      return true;
    } else {
      return false;
    }
  };

}).call(this);