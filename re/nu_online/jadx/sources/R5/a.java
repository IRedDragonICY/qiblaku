package R5;

import E6.h;
import H2.D;
import S5.c;
import S5.d;
import S5.e;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import p079j7.i;

/* JADX INFO: compiled from: r8-map-id-5db846206d50f768cea3f2990042f58a5d02f4aee0b59da179d90dbc2efb7c0a */
/* JADX INFO: loaded from: classes.dex */
public final class a implements SensorEventListener {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public final h f4608a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public final /* synthetic */ b f4609b;

    public a(b bVar, h hVar) {
        this.f4609b = bVar;
        this.f4608a = hVar;
    }

    public final L7.b a() {
        Display display = this.f4609b.f4610A;
        int rotation = display != null ? display.getRotation() : 0;
        if (rotation == 1) {
            return e.f5422a;
        }
        if (rotation != 2) {
            return rotation != 3 ? S5.b.f5419a : d.f5421a;
        }
        return c.f5420a;
    }

    public final void b(double d8, double d9) {
        int i8;
        if (Double.isNaN(d8) || Double.isNaN(d9)) {
            return;
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        b bVar = this.f4609b;
        if (jElapsedRealtime < bVar.H) {
            return;
        }
        Double d10 = bVar.f4617I;
        if (d10 != null && bVar.f4622N > 0.0d) {
            double dAbs = Math.abs(d8 - d10.doubleValue()) % 360.0d;
            if (dAbs > 180.0d) {
                dAbs = 360.0d - dAbs;
            }
            if (dAbs < bVar.f4622N) {
                return;
            }
        }
        bVar.H = jElapsedRealtime + bVar.f4623O;
        bVar.f4617I = Double.valueOf(d8);
        int i9 = bVar.f4616G;
        if (i9 == 1) {
            i8 = 45;
        } else if (i9 != 2) {
            i8 = i9 != 3 ? -1 : 15;
        } else {
            i8 = 30;
        }
        this.f4608a.success(new double[]{d8, d9, i8});
    }

    public final void c() {
        L7.b bVarA = a();
        b bVar = this.f4609b;
        float[] fArr = bVar.f4618J;
        float[] fArr2 = bVar.f4619K;
        float[] fArr3 = new float[9];
        S5.a aVarI = !SensorManager.getRotationMatrix(fArr3, new float[9], fArr, fArr2) ? null : D.i(fArr3, bVarA);
        if (aVarI != null) {
            b(aVarI.f5417a, aVarI.f5418b);
            return;
        }
        float f8 = fArr[0];
        float f9 = fArr[1];
        float f10 = fArr[2];
        float f11 = fArr2[0];
        float f12 = fArr2[1];
        float f13 = fArr2[2];
        float f14 = (f12 * f10) - (f13 * f9);
        float f15 = (f13 * f8) - (f11 * f10);
        float f16 = (f11 * f9) - (f12 * f8);
        float fSqrt = 1.0f / ((float) Math.sqrt((f16 * f16) + ((f15 * f15) + (f14 * f14))));
        float fSqrt2 = 1.0f / ((float) Math.sqrt((f10 * f10) + ((f9 * f9) + (f8 * f8))));
        double dAtan2 = ((((float) (((double) ((float) Math.atan2(f15 * fSqrt, ((f10 * fSqrt2) * (f14 * fSqrt)) - ((f8 * fSqrt2) * (f16 * fSqrt))))) / 3.141592653589793d)) * 180.0f) + 360.0f) % 360.0f;
        b(dAtan2, dAtan2);
    }

    @Override // android.hardware.SensorEventListener
    public final void onAccuracyChanged(Sensor sensor, int i8) {
        i.e("sensor", sensor);
        int type = sensor.getType();
        b bVar = this.f4609b;
        if (type == 2) {
            if (bVar.f4616G != i8) {
                bVar.f4616G = i8;
            }
        } else {
            if (type != 11) {
                Log.w("FlutterCompass", "Unexpected accuracy changed event of type " + sensor.getType());
                return;
            }
            if (bVar.f4616G != i8) {
                bVar.f4616G = i8;
            }
            Log.v("FlutterCompass", "Received rotation vector sensor accuracy " + i8);
        }
    }

    @Override // android.hardware.SensorEventListener
    public final void onSensorChanged(SensorEvent sensorEvent) {
        i.e("event", sensorEvent);
        b bVar = this.f4609b;
        float[] fArr = bVar.f4615F;
        if (bVar.f4616G == 0) {
            Log.d("FlutterCompass", "Compass sensor is unreliable, device calibration is needed.");
        }
        int i8 = 0;
        if (sensorEvent.sensor.getType() != 11) {
            if (sensorEvent.sensor.getType() == 1 && bVar.f4612C == null) {
                float[] fArr2 = (float[]) sensorEvent.values.clone();
                float[] fArr3 = bVar.f4618J;
                i.e("input", fArr2);
                int length = fArr2.length;
                while (i8 < length) {
                    float f8 = fArr3[i8];
                    fArr3[i8] = ((fArr2[i8] - f8) * 0.15f) + f8;
                    i8++;
                }
                c();
                return;
            }
            if (sensorEvent.sensor.getType() == 2 && bVar.f4612C == null) {
                float[] fArr4 = (float[]) sensorEvent.values.clone();
                float[] fArr5 = bVar.f4619K;
                i.e("input", fArr4);
                int length2 = fArr4.length;
                while (i8 < length2) {
                    float f9 = fArr5[i8];
                    fArr5[i8] = ((fArr4[i8] - f9) * 0.15f) + f9;
                    i8++;
                }
                c();
                return;
            }
            return;
        }
        float[] fArr6 = sensorEvent.values;
        if (fArr6.length > 4) {
            System.arraycopy(fArr6, 0, fArr, 0, 4);
        } else {
            fArr = fArr6;
        }
        if (Math.abs(fArr[0]) > Float.MAX_VALUE || Math.abs(fArr[1]) > Float.MAX_VALUE || Math.abs(fArr[2]) > Float.MAX_VALUE) {
            return;
        }
        float f10 = fArr[0];
        float f11 = fArr[1];
        float f12 = fArr[2];
        if (Math.abs(f10) <= Float.MAX_VALUE && Math.abs(f11) <= Float.MAX_VALUE && Math.abs(f12) <= Float.MAX_VALUE) {
            L7.b bVarA = a();
            float[] fArr7 = new float[9];
            SensorManager.getRotationMatrixFromVector(fArr7, new float[]{f10, f11, f12});
            S5.a aVarI = D.i(fArr7, bVarA);
            b(aVarI.f5417a, aVarI.f5418b);
            return;
        }
        throw new IllegalArgumentException(("SensorAttitude components must be finite (got x=" + f10 + " y=" + f11 + " z=" + f12 + ")").toString());
    }
}
