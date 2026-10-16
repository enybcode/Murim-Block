package com.colleuhu95.bloodsect.ninja;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Shoulder-led diagonal cleave; quaternion wrist interpolation avoids Euler spins. */
final class DaoM1Motion {
    static final int DURATION=19;
    static final DaoAttackMotion.Trail TRAIL=new DaoAttackMotion.Trail(5,9.5F,4,11);
    private static final float[] TIMES={0F,3F,5F,6.5F,8F,9.5F,11.5F,14.5F,19F};
    private static final float[][] POSES={
        {0.000000F,0.000000F,5.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,2.000000F,0.000000F,2.000000F,-5.000000F,0.000000F,0.000000F,0.000000F},
        {-100.000000F,-12.000000F,32.000000F,-68.000000F,22.000000F,-28.000000F,-60.000000F,-15.000000F,-4.000000F,0.350000F,-10.000000F,20.000000F,-8.000000F,16.000000F,-12.000000F,-110.833039F,6.121245F,-123.623201F},
        {-108.000000F,-10.000000F,30.000000F,-65.000000F,28.000000F,-35.000000F,-65.000000F,-22.000000F,-5.000000F,0.500000F,-12.000000F,24.000000F,-10.000000F,20.000000F,-15.000000F,-111.047527F,-1.755637F,-120.527129F},
        {-85.000000F,0.000000F,15.000000F,-30.000000F,12.000000F,-40.000000F,-60.000000F,30.000000F,8.000000F,0.800000F,-20.000000F,40.000000F,-12.000000F,24.000000F,-18.000000F,-137.821558F,-53.437329F,-131.610608F},
        {-65.000000F,10.000000F,-12.000000F,-12.000000F,-8.000000F,-35.000000F,-60.000000F,52.000000F,14.000000F,1.000000F,-25.000000F,50.000000F,-14.000000F,28.000000F,-20.000000F,163.562071F,-32.740346F,-106.140362F},
        {-44.000000F,15.000000F,-45.000000F,-16.000000F,-32.000000F,-28.000000F,-72.000000F,38.000000F,12.000000F,0.800000F,-22.000000F,44.000000F,-12.000000F,24.000000F,-15.000000F,175.720177F,-3.586152F,-87.315228F},
        {-42.000000F,15.000000F,-44.000000F,-20.000000F,-30.000000F,-28.000000F,-72.000000F,-18.000000F,10.000000F,0.800000F,-22.000000F,44.000000F,-12.000000F,24.000000F,-15.000000F,175.810551F,-3.495007F,-82.453811F},
        {-28.000000F,4.000000F,-8.000000F,-30.000000F,-14.000000F,-18.000000F,-38.000000F,-9.000000F,4.000000F,0.300000F,-10.000000F,20.000000F,-6.000000F,12.000000F,-10.000000F,-175.658227F,-25.543606F,-73.202027F},
        {0.000000F,0.000000F,5.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,2.000000F,0.000000F,2.000000F,-5.000000F,0.000000F,0.000000F,0.000000F}
    };
    private static final float RAD=(float)(Math.PI/180);
    private static final Quaternionf[] WRISTS=new Quaternionf[POSES.length];
    static {for(int i=0;i<POSES.length;i++) WRISTS[i]=new Quaternionf().rotationZYX(POSES[i][17]*RAD,POSES[i][16]*RAD,POSES[i][15]*RAD);}
    static DaoAttackMotion.Pose sample(float age) {
        if(!Float.isFinite(age)) age=0;
        age=Math.max(0,Math.min(DURATION,age));
        int i=0;while(i<TIMES.length-2 && age>TIMES[i+1]) i++;
        float u=(age-TIMES[i])/(TIMES[i+1]-TIMES[i]);u=u*u*(3-2*u);
        var a=POSES[i];var b=POSES[i+1];
        var q=new Quaternionf(WRISTS[i]).slerp(WRISTS[i+1],u).normalize();
        // The bundled JOML 1.10.5 ZYX extractor has an incorrect Y-squared sign in its X denominator.
        // Derive the three angles directly, keeping the pinned runtime unchanged.
        var wrist=new Vector3f(
            (float)Math.atan2(2*(q.w*q.x+q.y*q.z),1-2*(q.x*q.x+q.y*q.y)),
            (float)Math.asin(Math.max(-1,Math.min(1,2*(q.w*q.y-q.x*q.z)))),
            (float)Math.atan2(2*(q.w*q.z+q.x*q.y),1-2*(q.y*q.y+q.z*q.z))).div(RAD);
        if(age==0 || age==DURATION) wrist.set(0,0,0);
        return new DaoAttackMotion.Pose(lerp(a,b,0,u),lerp(a,b,1,u),lerp(a,b,2,u),lerp(a,b,3,u),lerp(a,b,4,u),lerp(a,b,5,u),lerp(a,b,6,u),lerp(a,b,7,u),lerp(a,b,8,u),lerp(a,b,9,u),lerp(a,b,10,u),lerp(a,b,11,u),lerp(a,b,12,u),lerp(a,b,13,u),lerp(a,b,14,u),wrist.x,wrist.y,wrist.z);
    }
    private static float lerp(float[] a,float[] b,int i,float u) {return a[i]+(b[i]-a[i])*u;}
    private DaoM1Motion() {}
}
