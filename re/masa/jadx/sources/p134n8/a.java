package p134n8;

import A7.b;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;
import io.flutter.plugin.common.EventChannel;

/* JADX INFO: loaded from: classes3.dex */
public final class a implements SensorEventListener {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public final /* synthetic */ EventChannel.EventSink f19714a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public final /* synthetic */ b f19715b;

    public a(b bVar, EventChannel.EventSink eventSink) {
        this.f19715b = bVar;
        this.f19714a = eventSink;
    }

    public final float[] a(SensorEvent sensorEvent) {
        float[] fArr = sensorEvent.values;
        if (fArr.length <= 4) {
            return fArr;
        }
        b bVar = this.f19715b;
        System.arraycopy(fArr, 0, bVar.f19722g, 0, 4);
        return bVar.f19722g;
    }

    public final void b() {
        int i5;
        int i10;
        double d10;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        b bVar = this.f19715b;
        if (jElapsedRealtime < bVar.f19726k) {
            return;
        }
        float[] fArr = bVar.f19724i;
        if (fArr != null) {
            SensorManager.getRotationMatrixFromVector(bVar.f19723h, fArr);
        } else {
            SensorManager.getRotationMatrix(bVar.f19723h, null, bVar.l, bVar.m);
        }
        int rotation = bVar.f19717b.getRotation();
        int i11 = 130;
        int i12 = 129;
        if (rotation == 1) {
            i5 = 129;
            i10 = 2;
        } else if (rotation == 2) {
            i5 = 130;
            i10 = 129;
        } else if (rotation != 3) {
            i5 = 2;
            i10 = 1;
        } else {
            i10 = 130;
            i5 = 1;
        }
        float[] fArr2 = new float[9];
        SensorManager.remapCoordinateSystem(bVar.f19723h, i10, i5, fArr2);
        float[] fArr3 = new float[3];
        SensorManager.getOrientation(fArr2, fArr3);
        float f10 = fArr3[1];
        if (f10 < -0.7853981633974483d) {
            int rotation2 = bVar.f19717b.getRotation();
            if (rotation2 == 1) {
                i11 = 3;
            } else if (rotation2 == 2) {
                i11 = 129;
                i12 = 131;
            } else if (rotation2 != 3) {
                i12 = 3;
                i11 = 1;
            } else {
                i12 = 1;
                i11 = 131;
            }
        } else if (f10 > 0.7853981633974483d) {
            int rotation3 = bVar.f19717b.getRotation();
            if (rotation3 == 1) {
                i11 = 131;
            } else if (rotation3 == 2) {
                i11 = 129;
                i12 = 3;
            } else if (rotation3 != 3) {
                i11 = 1;
                i12 = 131;
            } else {
                i11 = 3;
                i12 = 1;
            }
        } else if (Math.abs(fArr3[2]) > 1.5707963267948966d) {
            int rotation4 = bVar.f19717b.getRotation();
            if (rotation4 != 1) {
                if (rotation4 == 2) {
                    i11 = 129;
                    i12 = 2;
                } else if (rotation4 != 3) {
                    i12 = 130;
                    i11 = 1;
                } else {
                    i11 = 2;
                    i12 = 1;
                }
            }
        } else {
            i11 = i10;
            i12 = i5;
        }
        SensorManager.remapCoordinateSystem(bVar.f19723h, i11, i12, fArr2);
        SensorManager.getOrientation(fArr2, fArr3);
        double[] dArr = new double[3];
        dArr[0] = Math.toDegrees(fArr3[0]);
        int i13 = bVar.f19725j;
        if (i13 == 3) {
            d10 = 15.0d;
        } else if (i13 == 2) {
            d10 = 30.0d;
        } else {
            d10 = i13 == 1 ? 45.0d : -1.0d;
        }
        dArr[2] = d10;
        this.f19714a.success(dArr);
        bVar.getClass();
        bVar.f19726k = jElapsedRealtime + 32;
    }

    @Override // android.hardware.SensorEventListener
    public final void onAccuracyChanged(Sensor sensor, int i5) {
        b bVar = this.f19715b;
        if (bVar.f19725j != i5) {
            bVar.f19725j = i5;
        }
    }

    @Override // android.hardware.SensorEventListener
    public final void onSensorChanged(SensorEvent sensorEvent) {
        b bVar = this.f19715b;
        if (bVar.f19725j == 0) {
            Log.d("FlutterCompass", "Compass sensor is unreliable, device calibration is needed.");
        }
        if (sensorEvent.sensor.getType() == 11) {
            bVar.f19724i = a(sensorEvent);
            b();
            return;
        }
        int i5 = 0;
        if (sensorEvent.sensor.getType() == 1 && bVar.f19719d == null) {
            float[] fArrA = a(sensorEvent);
            float[] fArr = bVar.l;
            if (fArr != null) {
                while (i5 < fArrA.length) {
                    float f10 = fArr[i5];
                    fArr[i5] = b.c(fArrA[i5], f10, 0.45f, f10);
                    i5++;
                }
                fArrA = fArr;
            }
            bVar.l = fArrA;
            b();
            return;
        }
        if (sensorEvent.sensor.getType() == 2 && bVar.f19719d == null) {
            float[] fArrA2 = a(sensorEvent);
            float[] fArr2 = bVar.m;
            if (fArr2 != null) {
                while (i5 < fArrA2.length) {
                    float f11 = fArr2[i5];
                    fArr2[i5] = b.c(fArrA2[i5], f11, 0.45f, f11);
                    i5++;
                }
                fArrA2 = fArr2;
            }
            bVar.m = fArrA2;
            b();
        }
    }
}
