var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'LinphonePlugin', 'coolMethod', [arg0]);
};

exports.initLinphoneCore = function(success, fail) {
    exec(success, fail, "LinphonePlugin", "initLinphoneCore", []);
};

exports.registerSIP = function(username,domain,password,transport,success, fail) {
    exec(success, fail, "LinphonePlugin", "registerSIP", [username,domain,password,transport]);
};

exports.acceptCall = function(success, fail) {
    exec(success, fail, "LinphonePlugin", "acceptCall", []);
};