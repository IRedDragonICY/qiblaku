package p134n8;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;
import com.google.firebase.messaging.Constants;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;

/* JADX INFO: loaded from: classes3.dex */
public final class b implements FlutterPlugin, EventChannel.StreamHandler {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public a f19716a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public Display f19717b;

    /* JADX INFO: renamed from: c, reason: collision with root package name */
    public SensorManager f19718c;

    /* JADX INFO: renamed from: d, reason: collision with root package name */
    public Sensor f19719d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    public Sensor f19720e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    public Sensor f19721f;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    public float[] f19724i;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    public int f19725j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    public long f19726k;

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    public EventChannel f19727n;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    public final float[] f19722g = new float[4];

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    public final float[] f19723h = new float[9];
    public float[] l = new float[3];
    public float[] m = new float[3];

    public final void a() {
        SensorManager sensorManager = this.f19718c;
        if (sensorManager == null) {
            return;
        }
        Sensor sensor = this.f19719d;
        if (sensor != null) {
            sensorManager.unregisterListener(this.f19716a, sensor);
        }
        this.f19718c.unregisterListener(this.f19716a, this.f19720e);
        this.f19718c.unregisterListener(this.f19716a, this.f19721f);
    }

    @Override // io.flutter.embedding.engine.plugins.FlutterPlugin
    public final void onAttachedToEngine(FlutterPlugin.FlutterPluginBinding flutterPluginBinding) {
        this.f19727n = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "hemanthraj/flutter_compass");
        Context applicationContext = flutterPluginBinding.getApplicationContext();
        this.f19717b = ((DisplayManager) applicationContext.getSystemService(Constants.ScionAnalytics.MessageType.DISPLAY_NOTIFICATION)).getDisplay(0);
        SensorManager sensorManager = (SensorManager) applicationContext.getSystemService("sensor");
        this.f19718c = sensorManager;
        Sensor defaultSensor = sensorManager.getDefaultSensor(11);
        this.f19719d = defaultSensor;
        if (defaultSensor == null) {
            Log.d("FlutterCompass", "Rotation vector sensor not supported on device, falling back to accelerometer and magnetic field.");
        }
        this.f19720e = this.f19718c.getDefaultSensor(1);
        this.f19721f = this.f19718c.getDefaultSensor(2);
        this.f19727n.setStreamHandler(this);
    }

    @Override // io.flutter.plugin.common.EventChannel.StreamHandler
    public final void onCancel(Object obj) {
        a();
    }

    @Override // io.flutter.embedding.engine.plugins.FlutterPlugin
    public final void onDetachedFromEngine(FlutterPlugin.FlutterPluginBinding flutterPluginBinding) {
        a();
        this.f19718c = null;
        this.f19717b = null;
        this.f19719d = null;
        this.f19720e = null;
        this.f19721f = null;
        EventChannel eventChannel = this.f19727n;
        if (eventChannel != null) {
            eventChannel.setStreamHandler(null);
        }
    }

    @Override // io.flutter.plugin.common.EventChannel.StreamHandler
    public final void onListen(Object obj, EventChannel.EventSink eventSink) {
        a aVar = new a(this, eventSink);
        this.f19716a = aVar;
        SensorManager sensorManager = this.f19718c;
        if (sensorManager == null) {
            return;
        }
        Sensor sensor = this.f19719d;
        if (sensor != null) {
            sensorManager.registerListener(aVar, sensor, 30000);
        }
        this.f19718c.registerListener(this.f19716a, this.f19720e, 30000);
        this.f19718c.registerListener(this.f19716a, this.f19721f, 30000);
    }
}
