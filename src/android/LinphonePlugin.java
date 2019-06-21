package cordova.plugin.linphone;

import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.core.AccountCreator;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.ProxyConfig;
import org.linphone.core.RegistrationState;
import org.linphone.core.TransportType;

import io.ionic.starter.R;

/**
 * This class echoes a string called from JavaScript.
 */
public class LinphonePlugin extends CordovaPlugin {
    private Handler mHandler;
    PluginResult pluginResult;
    private CallbackContext callbackContext;
    private CoreListenerStub mCoreListener;
    private AccountCreator mAccountCreator;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        } else if (action.equals("initLinphoneCore")) {

            mCoreListener = new CoreListenerStub() {
                @Override
                public void onRegistrationStateChanged(Core core, ProxyConfig cfg, RegistrationState state, String message) {
                    if (state == RegistrationState.Ok) {
                        Toast.makeText(cordova.getContext(), "success: " + message, Toast.LENGTH_LONG).show();
                    } else if (state == RegistrationState.Failed) {
                        Toast.makeText(cordova.getContext(), "Failure: " + message, Toast.LENGTH_LONG).show();
                    }
                }
            };
            this.configureAccount();

            return true;
        } else if (action.equals("registerSIP")) {
            String message = args.getString(0);
            this.registerSIP(args, callbackContext);
            return true;
        }
        return false;
    }



    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    public void registerSIP( JSONArray args, CallbackContext callbackContext){
        try {
            String transport = args.get(3).toString();

            // At least the 3 below values are required
            mAccountCreator.setUsername(args.get(0).toString());
            mAccountCreator.setDomain(args.get(1).toString());
            mAccountCreator.setPassword(args.get(2).toString());

            // By default it will be UDP if not set, but TLS is strongly recommended
            switch (transport) {
                case "UDP":
                    mAccountCreator.setTransport(TransportType.Udp);
                    break;
                case "TCP":
                    mAccountCreator.setTransport(TransportType.Tcp);
                    break;
                case "TLS":
                    mAccountCreator.setTransport(TransportType.Tls);
                    break;
            }

            // This will automatically create the proxy config and auth info and add them to the Core
            ProxyConfig cfg = mAccountCreator.createProxyConfig();
            // Make sure the newly created one is the default
            LinphoneService.getCore().setDefaultProxyConfig(cfg);

            pluginResult = new PluginResult(PluginResult.Status.OK, "Registeration start!");
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);

        } catch (JSONException e) {
            pluginResult = new PluginResult(PluginResult.Status.OK, "Error:"+e);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    }

    public void configureAccount(){
        mHandler = new Handler();
        LinphoneService.callbackContext=callbackContext;
        if (LinphoneService.isReady()) {
            mAccountCreator = LinphoneService.getCore().createAccountCreator(null);
            LinphoneService.getCore().addListener(mCoreListener);
            Toast.makeText(cordova.getContext(), "service already ready", Toast.LENGTH_LONG).show();
            pluginResult = new PluginResult(PluginResult.Status.OK, "Service Already Ready!");
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        } else {
            Toast.makeText(cordova.getContext(), "service not ready", Toast.LENGTH_LONG).show();
            // If it's not, let's start it
            cordova.getContext().startService(
                    new Intent().setClass(cordova.getContext(), LinphoneService.class));
            // And wait for it to be ready, so we can safely use it afterwards
            new ServiceWaitThread().start();
        }

    }

    // This thread will periodically check if the Service is ready, and then call onServiceReady
    private class ServiceWaitThread extends Thread {
        public void run() {
            while (!LinphoneService.isReady()) {
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException("waiting thread sleep() has been interrupted");
                }
            }
            // As we're in a thread, we can't do UI stuff in it, must post a runnable in UI thread
            mHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            mAccountCreator = LinphoneService.getCore().createAccountCreator(null);
                            LinphoneService.getCore().addListener(mCoreListener);
                            Toast.makeText(cordova.getContext(), "service ready!", Toast.LENGTH_LONG).show();
                            pluginResult = new PluginResult(PluginResult.Status.OK, "Service Ready!");
                            pluginResult.setKeepCallback(true);
                            callbackContext.sendPluginResult(pluginResult);
                        }
                    });
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        LinphoneService.getCore().addListener(mCoreListener);
    }

    @Override
    public void onPause(boolean multitasking) {
        LinphoneService.getCore().removeListener(mCoreListener);
        super.onPause(multitasking);
    }
}
