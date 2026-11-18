package R5;

import A6.c;
import C4.f;
import E6.g;
import E6.h;
import E6.i;
import E6.n;
import E6.o;
import E6.p;
import E6.q;
import E6.r;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;
import java.util.Map;

/* JADX INFO: compiled from: r8-map-id-5db846206d50f768cea3f2990042f58a5d02f4aee0b59da179d90dbc2efb7c0a */
/* JADX INFO: loaded from: classes.dex */
public final class b implements c, i, p {

    /* JADX INFO: renamed from: A, reason: collision with root package name */
    public Display f4610A;

    /* JADX INFO: renamed from: B, reason: collision with root package name */
    public SensorManager f4611B;

    /* JADX INFO: renamed from: C, reason: collision with root package name */
    public Sensor f4612C;

    /* JADX INFO: renamed from: D, reason: collision with root package name */
    public Sensor f4613D;

    /* JADX INFO: renamed from: E, reason: collision with root package name */
    public Sensor f4614E;

    /* JADX INFO: renamed from: G, reason: collision with root package name */
    public int f4616G;
    public long H;

    /* JADX INFO: renamed from: I, reason: collision with root package name */
    public Double f4617I;

    /* JADX INFO: renamed from: L, reason: collision with root package name */
    public f f4620L;

    /* JADX INFO: renamed from: M, reason: collision with root package name */
    public r f4621M;

    /* JADX INFO: renamed from: z, reason: collision with root package name */
    public a f4625z;

    /* JADX INFO: renamed from: F, reason: collision with root package name */
    public final float[] f4615F = new float[4];

    /* JADX INFO: renamed from: J, reason: collision with root package name */
    public final float[] f4618J = new float[3];

    /* JADX INFO: renamed from: K, reason: collision with root package name */
    public final float[] f4619K = new float[3];

    /* JADX INFO: renamed from: N, reason: collision with root package name */
    public double f4622N = 0.1d;

    /* JADX INFO: renamed from: O, reason: collision with root package name */
    public long f4623O = 33;

    /* JADX INFO: renamed from: P, reason: collision with root package name */
    public int f4624P = 33000;

    @Override // A6.c
    public final void onAttachedToEngine(A6.b bVar) {
        p079j7.i.e("binding", bVar);
        E6.f fVar = bVar.f206c;
        this.f4620L = new f(fVar, "hemanthraj/flutter_compass");
        this.f4621M = new r(fVar, "hemanthraj/flutter_compass/methods");
        Context context = bVar.f204a;
        p079j7.i.d("getApplicationContext(...)", context);
        Object systemService = context.getSystemService("display");
        DisplayManager displayManager = systemService instanceof DisplayManager ? (DisplayManager) systemService : null;
        this.f4610A = displayManager != null ? displayManager.getDisplay(0) : null;
        Object systemService2 = context.getSystemService("sensor");
        SensorManager sensorManager = systemService2 instanceof SensorManager ? (SensorManager) systemService2 : null;
        this.f4611B = sensorManager;
        Sensor defaultSensor = sensorManager != null ? sensorManager.getDefaultSensor(11) : null;
        this.f4612C = defaultSensor;
        if (defaultSensor == null) {
            Log.d("FlutterCompass", "Rotation vector sensor not supported on device, falling back to accelerometer and magnetic field.");
            SensorManager sensorManager2 = this.f4611B;
            this.f4613D = sensorManager2 != null ? sensorManager2.getDefaultSensor(1) : null;
            SensorManager sensorManager3 = this.f4611B;
            this.f4614E = sensorManager3 != null ? sensorManager3.getDefaultSensor(2) : null;
        }
        f fVar2 = this.f4620L;
        if (fVar2 != null) {
            fVar2.C(this);
        }
        r rVar = this.f4621M;
        if (rVar != null) {
            rVar.b(this);
        }
    }

    @Override // E6.i
    public final void onCancel(Object obj) {
        a aVar;
        SensorManager sensorManager = this.f4611B;
        if (sensorManager != null && (aVar = this.f4625z) != null) {
            sensorManager.unregisterListener(aVar);
        }
        this.f4625z = null;
        this.f4617I = null;
        this.H = 0L;
    }

    @Override // A6.c
    public final void onDetachedFromEngine(A6.b bVar) {
        a aVar;
        p079j7.i.e("binding", bVar);
        SensorManager sensorManager = this.f4611B;
        if (sensorManager != null && (aVar = this.f4625z) != null) {
            sensorManager.unregisterListener(aVar);
        }
        this.f4611B = null;
        this.f4610A = null;
        this.f4612C = null;
        this.f4613D = null;
        this.f4614E = null;
        f fVar = this.f4620L;
        if (fVar != null) {
            fVar.C(null);
        }
        r rVar = this.f4621M;
        if (rVar != null) {
            rVar.b(null);
        }
        this.f4620L = null;
        this.f4621M = null;
    }

    @Override // E6.i
    public final void onListen(Object obj, g gVar) {
        a aVar;
        Map map = obj instanceof Map ? (Map) obj : null;
        if (map == null) {
            this.f4622N = 0.1d;
            this.f4623O = 33L;
            this.f4624P = 33000;
        } else {
            Object obj2 = map.get("headingFilter");
            Number number = obj2 instanceof Number ? (Number) obj2 : null;
            if (number != null) {
                double dDoubleValue = number.doubleValue();
                if (dDoubleValue >= 0.0d) {
                    this.f4622N = dDoubleValue;
                }
            }
            Object obj3 = map.get("updateIntervalMs");
            Number number2 = obj3 instanceof Number ? (Number) obj3 : null;
            if (number2 != null) {
                long jLongValue = number2.longValue();
                if (jLongValue > 0) {
                    this.f4623O = jLongValue;
                    int i8 = (int) (jLongValue * 1000);
                    if (i8 < 10000) {
                        i8 = 10000;
                    }
                    this.f4624P = i8;
                }
            }
        }
        SensorManager sensorManager = this.f4611B;
        if (sensorManager != null && (aVar = this.f4625z) != null) {
            sensorManager.unregisterListener(aVar);
        }
        a aVar2 = new a(this, (h) gVar);
        this.f4625z = aVar2;
        SensorManager sensorManager2 = this.f4611B;
        if (sensorManager2 == null) {
            return;
        }
        Sensor sensor = this.f4612C;
        if (sensor != null) {
            sensorManager2.registerListener(aVar2, sensor, this.f4624P);
        } else {
            sensorManager2.registerListener(aVar2, this.f4613D, this.f4624P);
            sensorManager2.registerListener(aVar2, this.f4614E, this.f4624P);
        }
    }

    @Override // E6.p
    public final void onMethodCall(n nVar, q qVar) {
        p079j7.i.e("call", nVar);
        if (p079j7.i.a(nVar.f905a, "hasSensors")) {
            ((o) qVar).success(Boolean.valueOf((this.f4612C == null && (this.f4613D == null || this.f4614E == null)) ? false : true));
        } else {
            ((o) qVar).notImplemented();
        }
    }
}
