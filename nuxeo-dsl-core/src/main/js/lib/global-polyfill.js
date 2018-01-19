var global = this;
var self = this;
var window = this;
var process = {env: {}};
var console = {};

Object.assign = function (t) {
  for (var s, i = 1, n = arguments.length; i < n; i++) {
    s = arguments[i];
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
      t[p] = s[p];
  }
  return t;
};

(function consoleInit(context) {
  var logger = context.__NASHORN_LOGGER__ || {
      debug: print,
      warn: print,
      info: print,
      error: print,
      trace: print
    };

  console.debug = function(args){ logger.debug(args); };
  console.warn = function(args){ logger.warn(args);};
  console.log = function(args){ logger.info(args);};
  console.error = function(args){ logger.error(args);};
  console.trace = function(args){ logger.trace(args);};
})(this);
